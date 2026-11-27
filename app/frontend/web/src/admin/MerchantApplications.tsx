import { useEffect, useRef, useState } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import MerchantMenu from './MerchantMenu';
import { MerchantAdvancedSearch, MerchantRevisionTools } from './MerchantReviewViews';
import { adminSession, AdminSessionError } from './session';
import { AdminRejection } from './AdminRejection';
import { applicationStates, currentPolicyVersion, governanceId, parseApplicationRevisions, parseApplication, parseApplicationPage, parseGovernanceContext, parseReviewData, parseReviewers, parseApplicationEvents, stateLabels } from './governance';
import type { ApplicationPage, ApplicationSummary, GovernanceContext, ReviewData, ReviewerPage, ApplicationAuditPage, ApplicationRevisionPage } from './governance';

const day = () => new Date(Date.now() + 7 * 3600000).toISOString().slice(0, 10);
const labels: Record<string, string> = { registrationValid: 'Hồ sơ đăng ký hợp lệ', representationValid: 'Thẩm quyền đại diện hợp lệ', contactValid: 'Liên hệ đã xác minh', duplicatesClear: 'Đã đối chiếu trùng lặp', conflictsClear: 'Không có xung đột đã biết' };
const fields: Record<string, string> = { registeredAddress: 'Địa chỉ đăng ký', operatingAddress: 'Địa chỉ hoạt động', representativeName: 'Người đại diện', representativeEmail: 'Email liên hệ', businessContactAttestationId: 'Xác minh liên hệ', REGISTRATION: 'Tài liệu đăng ký', REPRESENTATIVE_AUTHORIZATION: 'Tài liệu ủy quyền' };
const message = (failure: unknown) => {
  const code = failure instanceof AdminSessionError ? failure.code : 'SERVICE_UNAVAILABLE';
  return code === 'ACCESS_DENIED' ? 'Bạn chưa có quyền thực hiện thao tác này.' : code === 'PRECONDITION_STALE' || code === 'STATE_CONFLICT' ? 'Hồ sơ đã thay đổi hoặc chưa đủ điều kiện. Hãy tải lại trước khi thao tác.' : code === 'OUTCOME_UNKNOWN' ? 'Chưa xác định được kết quả. Thử lại đúng yêu cầu để kiểm tra kết quả đã lưu.' : code === 'RESOURCE_NOT_FOUND' || code === 'SERVICE_UNAVAILABLE' ? 'Workflow xét duyệt chưa khả dụng trong môi trường hoặc phạm vi hiện tại.' : code === 'SESSION_INVALID' ? 'Phiên đã hết hiệu lực. Vui lòng đăng nhập lại.' : 'Không thể hoàn tất yêu cầu. Kiểm tra dữ liệu và thử lại.';
};
type Pending = { operation: 'assignment' | 'review' | 'information'; id: string; version: number; key: string; body: unknown };
function address(value: unknown) {
  if (!value || typeof value !== 'object' || Array.isArray(value)) return '—';
  const fields = value as Record<string, unknown>;
  return ['addressLine', 'locality', 'administrativeArea', 'country'].map(key => fields[key]).filter((value): value is string => typeof value === 'string' && !!value.trim()).join(', ') || '—';
}
function declaration(value: unknown, key: string) {
  if (!value || typeof value !== 'object' || Array.isArray(value)) return '—';
  const field = (value as Record<string, unknown>)[key];
  return typeof field === 'boolean' ? field ? 'Đã đồng ý' : 'Chưa đồng ý' : typeof field === 'string' ? field : '—';
}
export default function MerchantApplications({ accountId }: { accountId: string }) {
  const [params] = useSearchParams();
  const applicationId = params.get('applicationId');
  const deepLink = useRef<string | null>(null);
  const [context, setContext] = useState<GovernanceContext | null>(null), [rows, setRows] = useState<ApplicationPage | null>(null);
  const [from, setFrom] = useState(() => new Date(Date.now() + 7 * 3600000 - 29 * 86400000).toISOString().slice(0, 10)), [to, setTo] = useState(day), [state, setState] = useState(''), [page, setPage] = useState(0), [refresh, setRefresh] = useState(0);
  const [draftFrom, setDraftFrom] = useState(from), [draftTo, setDraftTo] = useState(to), [draftState, setDraftState] = useState(state);
  const [policy, setPolicy] = useState(''), [draftPolicy, setDraftPolicy] = useState(''), [mine, setMine] = useState(false), [draftMine, setDraftMine] = useState(false), [lookup, setLookup] = useState('');
  const [selected, setSelected] = useState<ApplicationSummary | null>(null), [review, setReview] = useState<ReviewData | null>(null), [candidates, setCandidates] = useState<ReviewerPage | null>(null), [candidatePage, setCandidatePage] = useState(0);
  const [error, setError] = useState(''), [busy, setBusy] = useState(false), [loading, setLoading] = useState(true), [reason, setReason] = useState(''), [assignee, setAssignee] = useState(''), [requested, setRequested] = useState('registeredAddress');
  const [checks, setChecks] = useState<Record<string, boolean>>({}), [unknown, setUnknown] = useState(false);
  const [policyVersion, setPolicyVersion] = useState<string | null>(null);
  const [history, setHistory] = useState<ApplicationAuditPage | null>(null);
  const [revisions, setRevisions] = useState<ApplicationRevisionPage | null>(null);
  const pending = useRef<Pending | null>(null), mounted = useRef(false), running = useRef(false);
  useEffect(() => { mounted.current = true;return () => { mounted.current = false;pending.current = null; }; }, []);
  useEffect(() => {
    let active = true;setLoading(true);setError('');setRows(null);setContext(null);
    const parameters = new URLSearchParams({ from, to, page: String(page), size: '20' });if (state) parameters.set('state', state);
    if (policy) parameters.set('policyId', policy);if (mine) parameters.set('reviewerId', accountId);
    void Promise.all([adminSession.read('governance-context', new URLSearchParams(), v => parseGovernanceContext(v, accountId)), adminSession.read('application-list', parameters, v => parseApplicationPage(v, page))]).then(([c, list]) => { if (active) { setContext(c);setRows(list); } }).catch(e => { if (active) setError(message(e)); }).finally(() => { if (active) setLoading(false); });
    return () => { active = false; };
  }, [accountId, from, to, state, page, refresh, policy, mine]);
  const permissions = context?.assignments.filter(a => a.scopeType === 'POLICY' && a.policyId === selected?.policyId).flatMap(a => a.permissions) ?? [];
  const assigned = selected?.reviewerId === accountId, canReview = assigned && permissions.includes('application.review');
  const canAssign = permissions.includes('application.assign');
  useEffect(() => {
    let active = true;setCandidates(null);setAssignee('');
    if (!selected || !canAssign) return;
    void adminSession.read('reviewer-candidates', new URLSearchParams({ policyId: selected.policyId, page: String(candidatePage), size: '20' }), v => parseReviewers(v, candidatePage)).then(value => { if (active) setCandidates(value); }).catch(e => { if (active) setError(message(e)); });
    return () => { active = false; };
  }, [selected, canAssign, context, candidatePage]);
  async function open(item: Pick<ApplicationSummary, 'id'>) {
    setRevisions(null);
    setHistory(null);
    if (running.current || unknown) return;setError('');setReview(null);setPolicyVersion(null);setChecks({});setReason('');setCandidatePage(0);setBusy(true);running.current = true;
    try { const value = await adminSession.read('application-detail', new URLSearchParams(), v => parseApplication(v, item.id), item.id);const version = await adminSession.read('application-revisions', new URLSearchParams(), v => currentPolicyVersion(v, item.id, value.revision), item.id);if (mounted.current) { setSelected(value);setPolicyVersion(version); } }
    catch (e) { if (mounted.current) setError(message(e)); }finally { running.current = false;if (mounted.current) setBusy(false); }
  }
  useEffect(() => {
    if (!context || !applicationId || deepLink.current === applicationId) return;
    deepLink.current = applicationId;
    if (!/^[a-f0-9]{8}(?:-[a-f0-9]{4}){3}-[a-f0-9]{12}$/.test(applicationId)) { setError('M\u00e3 h\u1ed3 s\u01a1 ch\u01b0a h\u1ee3p l\u1ec7.');return; }
    let active = true;setSelected(null);setReview(null);setRevisions(null);setPolicyVersion(null);setBusy(true);
    void adminSession.read('application-detail', new URLSearchParams(), v => parseApplication(v, applicationId), applicationId).then(async item => {
      const version = await adminSession.read('application-revisions', new URLSearchParams(), v => currentPolicyVersion(v, applicationId, item.revision), applicationId);
      if (active) { setSelected(item);setPolicyVersion(version); }
    }).catch(e => { if (active) setError(message(e)); }).finally(() => { if (active) setBusy(false); });
    return () => { active = false;deepLink.current = null; };
  }, [applicationId, context]);
  async function disclose() {
    if (!selected || running.current) return;const id = selected.id;running.current = true;setBusy(true);setError('');
    try { const value = await adminSession.read('application-review-data', new URLSearchParams({ purpose: 'MERCHANT_REVIEW' }), v => parseReviewData(v, id), id);if (mounted.current) { setReview(value);setSelected(value.application); } }
    catch (e) { if (mounted.current) setError(message(e)); }finally { running.current = false;if (mounted.current) setBusy(false); }
  }
  async function send(operation?: Pending['operation'], body?: unknown) {
    setRevisions(null);
    setHistory(null);
    if (!selected || running.current || !operation && !pending.current) return;
    const request = pending.current ?? { operation: operation!, id: selected.id, version: selected.version, key: crypto.randomUUID(), body };
    pending.current = request;running.current = true;setBusy(true);setError('');
    try {
      await adminSession.governanceWrite(request.operation, request.id, request.version, request.key, request.body, v => parseApplication(v, request.id));
      pending.current = null;
      if (mounted.current) { setUnknown(false);setReview(null);setReason('');setChecks({});setRefresh(n => n + 1); }
      try {
        const current = await adminSession.read('application-detail', new URLSearchParams(), v => parseApplication(v, request.id), request.id);
        const version = await adminSession.read('application-revisions', new URLSearchParams(), v => currentPolicyVersion(v, request.id, current.revision), request.id);
        if (mounted.current) { setSelected(current);setPolicyVersion(version); }
      } catch { if (mounted.current) { setSelected(null);setPolicyVersion(null);setError('Thao tác đã được lưu. Hãy tải lại danh sách để xem trạng thái hiện tại.'); } }
    } catch (e) {
      if (mounted.current) { const lost = e instanceof AdminSessionError && ['OUTCOME_UNKNOWN', 'SERVICE_UNAVAILABLE', 'RESPONSE_INVALID'].includes(e.code);setUnknown(lost);if (!lost) pending.current = null;setError(message(e)); }
    } finally { running.current = false;if (mounted.current) setBusy(false); }
  }
  async function downloadEvidence(evidence: string) {
    if (!selected || running.current || unknown) return;const id = selected.id;running.current = true;setBusy(true);setError('');
    try { const blob = await adminSession.governanceEvidence(id, evidence);if (!mounted.current) return;const url = URL.createObjectURL(blob), link = document.createElement('a');link.href = url;link.download = 'merchant-evidence.' + (blob.type === 'application/pdf' ? 'pdf' : blob.type === 'image/png' ? 'png' : 'jpg');document.body.appendChild(link);link.click();link.remove();setTimeout(() => URL.revokeObjectURL(url), 1000); }
    catch (failure) { if (mounted.current) setError(message(failure)); } finally { running.current = false;if (mounted.current) setBusy(false); }
  }
  async function events(page: number) {
    if (!selected || running.current || unknown) return; const id = selected.id; running.current = true;setBusy(true);setError('');
    try { const result = await adminSession.read('application-events', new URLSearchParams({ page: String(page), size: '20' }), v => parseApplicationEvents(v, id, page), id);if (mounted.current) setHistory(result); }
    catch (failure) { if (mounted.current) setError(message(failure)); } finally { running.current = false;if (mounted.current) setBusy(false); }
  }
  async function revisionHistory(page: number) {
    if (!selected || running.current || unknown) return;
    const id = selected.id;running.current = true;setBusy(true);setError('');setRevisions(null);
    try { const result = await adminSession.read('application-revisions', new URLSearchParams({ page: String(page), size: '20' }), value => parseApplicationRevisions(value, id, page), id);if (mounted.current) setRevisions(result); }
    catch (failure) { if (mounted.current) setError(message(failure)); }finally { running.current = false;if (mounted.current) setBusy(false); }
  }
  const reviewView = params.get('view') === 'review';
  const activation = selected?.activation as { organizationId: string; activatedAt: string; ownerMembershipId: string } | null;
  return <section className="ap-merchant-review" aria-label="Hồ sơ Merchant">
    <MerchantMenu current={reviewView ? 'review' : 'applications'} locked={busy || unknown} />
    <header className="ap-page-heading"><div><h1>{reviewView ? 'Review Workspace' : 'Merchant Applications'}</h1><p>{reviewView ? 'Mở hồ sơ thuộc phạm vi để thẩm định hoặc quyết định độc lập trên revision hiện hành.' : 'Hồ sơ đã nộp theo workflow Merchant; bản nháp không nằm trong hàng đợi.'}</p></div></header>
    <form className="ap-filters" onSubmit={e => { e.preventDefault();if ((Date.parse(draftTo) - Date.parse(draftFrom)) / 86400000 >= 366) { setError('Khoảng thời gian tối đa 366 ngày.');return; }setFrom(draftFrom);setTo(draftTo);setState(draftState);setPolicy(draftPolicy);setMine(draftMine);setPage(0);setSelected(null);setReview(null);setRevisions(null);setRefresh(n => n + 1); }}>
      <label>Từ ngày<input type="date" required value={draftFrom} max={draftTo} onChange={e => setDraftFrom(e.target.value)} /></label>
      <label>Đến ngày<input type="date" required value={draftTo} min={draftFrom} max={day()} onChange={e => setDraftTo(e.target.value)} /></label>
      <label>Trạng thái<select value={draftState} onChange={e => setDraftState(e.target.value)}><option value="">Tất cả hồ sơ đã nộp</option>{applicationStates.map(s => <option key={s} value={s}>{stateLabels[s]}</option>)}</select></label>
      <label>Policy scope<select value={draftPolicy} disabled={busy || unknown} onChange={e => setDraftPolicy(e.target.value)}><option value="">Tất cả scope được cấp</option>{[...new Set(context?.assignments.filter(a => a.scopeType === 'POLICY' && a.permissions.includes('application.read')).map(a => a.policyId) ?? [])].map(id => <option key={id} value={id ?? ''}>{id}</option>)}</select></label>
      <label><input type="checkbox" checked={draftMine} disabled={busy || unknown} onChange={e => setDraftMine(e.target.checked)} />Được phân công cho tôi</label>
      <button className="ap-secondary" disabled={busy || loading || unknown}>Làm mới</button>
    </form>
    <form className="ap-filters" onSubmit={e => { e.preventDefault();if (!governanceId(lookup)) { setError('Application ID phải là UUID hợp lệ.');return; }void open({ id: lookup }); }}><label>Application ID<input value={lookup} maxLength={36} spellCheck={false} disabled={busy || unknown} onChange={e => setLookup(e.target.value)} /></label><button className="ap-secondary" disabled={busy || unknown || !context}>Mở theo mã hồ sơ</button></form>
    {!selected && <MerchantAdvancedSearch key={from + to + state + policy + mine} kind="application" from={from} to={to} state={state} policy={policy} reviewer={mine ? accountId : undefined} locked={busy || unknown} open={item => void open(item)} />}
    {error && <p className="ap-error" role="alert">{error}</p>}
    {loading && <p role="status">Đang tải hồ sơ theo phạm vi được cấp…</p>}
    {unknown && <button className="ap-primary" disabled={busy} onClick={() => void send()}>Thử lại đúng yêu cầu</button>}
    {!selected && rows && <section className="ap-panel"><div className="ap-panel-heading"><h2>Hồ sơ đã nộp</h2><span>Giờ Việt Nam · theo ngày nộp lần đầu</span></div>
      <div className="ap-table-scroll"><table><thead><tr><th>Hồ sơ</th><th>Mã đăng ký</th><th>Trạng thái</th><th>Revision</th><th>Nộp lần đầu</th><th>Thao tác</th></tr></thead><tbody>{rows.items.map(item => <tr key={item.id}><td>{item.legalNameMasked}</td><td>{item.legalIdentifierMasked}</td><td>{stateLabels[item.state]}</td><td>{item.revision}</td><td>{item.firstSubmittedAt ? new Date(item.firstSubmittedAt).toLocaleDateString('vi-VN', { timeZone: 'Asia/Ho_Chi_Minh' }) : '—'}</td><td><button className="ap-secondary" disabled={busy || unknown} onClick={() => void open(item)}>Mở hồ sơ</button></td></tr>)}</tbody></table></div>
      {!rows.items.length && <p className="ap-empty">Không có hồ sơ đã nộp trong bộ lọc và phạm vi này.</p>}
      <div className="ap-pagination"><button disabled={page === 0 || busy} onClick={() => setPage(p => p - 1)}>Trang trước</button><span>Trang {page + 1}</span><button disabled={!rows.hasNext || busy} onClick={() => setPage(p => p + 1)}>Trang sau</button></div>
    </section>}
    {selected && <section className="ap-panel"><div className="ap-panel-heading"><div><h2>{selected.legalNameMasked}</h2><p>{stateLabels[selected.state]} · Revision {selected.revision} · Phiên bản {selected.version}</p></div><button className="ap-secondary" disabled={busy || unknown} onClick={() => { setSelected(null);setReview(null); }}>Về danh sách</button></div>
      {permissions.includes('application.evidence.read') && <button className="ap-secondary" disabled={busy} onClick={() => void disclose()}>Mở dữ liệu thẩm định</button>}
      {review && <div className="ap-review-facts"><h3>{review.organizationDisplayName}</h3><dl>{[['Tên pháp lý', 'registeredName'], ['Mã đăng ký', 'legalIdentifier'], ['Loại pháp nhân', 'entityType'], ['Phạm vi đăng ký', 'jurisdiction'], ['Người đại diện', 'representativeName'], ['Tư cách đại diện', 'representativeRelationship'], ['Liên hệ', 'representativeEmail']].map(([label, key]) => <div key={key}><dt>{label}</dt><dd>{typeof review.submission[key] === 'string' ? String(review.submission[key]) : '—'}</dd></div>)}<div><dt>Địa chỉ đăng ký</dt><dd>{address(review.submission.registeredAddress)}</dd></div><div><dt>Địa chỉ hoạt động</dt><dd>{address(review.submission.operatingAddress)}</dd></div><div><dt>Xác minh liên hệ</dt><dd>{new Date(review.contactVerifiedAt).toLocaleString('vi-VN', { timeZone: 'Asia/Ho_Chi_Minh' })}</dd></div></dl><h4>Tài liệu đã sàng lọc</h4><p>{review.evidence.length} tài liệu đã được kiểm tra · Chính sách {review.policyVersion}</p><div className="ap-review-documents">{review.evidence.map((e, index) => <div key={e.id}><span>{e.purpose === 'REGISTRATION' ? 'Đăng ký pháp nhân' : 'Ủy quyền đại diện'} · CLEAN</span><button className="ap-secondary" type="button" disabled={busy || unknown} onClick={() => void downloadEvidence(e.id)}>Tải tài liệu {index + 1}</button></div>)}</div><p>Đối chiếu trùng lặp và xung đột được kiểm tra lại tại Backend trước quyết định; checklist không phải quyền bỏ qua các kiểm soát này.</p></div>}
      {review && <section aria-label="Consent đã nộp"><h3>Đồng ý điều khoản tại revision hiện hành</h3><dl><dt>Tính chính xác</dt><dd>{declaration(review.submission.declarations, 'accuracy')}</dd><dt>Thẩm quyền đại diện</dt><dd>{declaration(review.submission.declarations, 'representation')}</dd><dt>Phiên bản consent</dt><dd>{declaration(review.submission.declarations, 'consentVersion')}</dd></dl></section>}
      <section aria-label="Revision history"><h3>Revision đã nộp</h3><button className="ap-secondary" type="button" disabled={busy || unknown} onClick={() => void revisionHistory(0)}>Xem lịch sử revision</button>
        {revisions && <><p>Metadata bất biến của từng lần nộp. API hiện chưa cung cấp nội dung lịch sử để so sánh từng trường.</p><div className="ap-table-scroll"><table><thead><tr><th>Revision</th><th>Nộp lúc</th><th>Tên hiển thị</th><th>Policy</th><th>Tài liệu</th><th>Contact verified</th></tr></thead><tbody>{revisions.items.map(row => <tr key={row.revision}><td>{row.revision}</td><td>{new Date(row.submittedAt).toLocaleString('vi-VN', { timeZone: 'Asia/Ho_Chi_Minh' })}</td><td>{row.organizationDisplayName}</td><td>{row.policyVersion}</td><td>{row.evidenceIds.length}</td><td>{new Date(row.contactVerifiedAt).toLocaleString('vi-VN', { timeZone: 'Asia/Ho_Chi_Minh' })}</td></tr>)}</tbody></table></div><div className="ap-pagination"><button disabled={busy || unknown || revisions.page === 0} onClick={() => void revisionHistory(revisions.page - 1)}>Revision trước</button><span>Trang {revisions.page + 1}</span><button disabled={busy || unknown || !revisions.hasNext} onClick={() => void revisionHistory(revisions.page + 1)}>Revision sau</button></div></>}
      </section>
      <MerchantRevisionTools key={selected.id + ":" + selected.version} application={selected} permissions={permissions} locked={busy || unknown} />
      {activation && <section aria-label="Kết quả kích hoạt"><h3>Đã kích hoạt Organization</h3><dl><dt>Organization</dt><dd>{activation.organizationId}</dd><dt>Initial Owner Membership</dt><dd>{activation.ownerMembershipId}</dd><dt>Kích hoạt lúc</dt><dd>{new Date(activation.activatedAt).toLocaleString('vi-VN', { timeZone: 'Asia/Ho_Chi_Minh' })}</dd></dl><Link className="ap-secondary" to={'/admin/merchants/organizations?organizationId=' + activation.organizationId}>Mở Organization đã kích hoạt</Link><p>Quyền truy cập Organization được Backend kiểm tra riêng. Kết quả kích hoạt không xác nhận email thông báo đã được giao.</p></section>}
      {!['APPROVED', 'REJECTED', 'WITHDRAWN'].includes(selected.state) && <fieldset disabled={busy || unknown}><legend>Thao tác xét duyệt</legend><label>Lý do<textarea maxLength={300} value={reason} onChange={e => setReason(e.target.value)} /></label>
        {permissions.includes('application.assign') && <div><label>Reviewer<select value={assignee} onChange={e => setAssignee(e.target.value)}><option value="">Chọn reviewer đủ điều kiện</option>{candidates?.items.map(c => <option key={c.accountId} value={c.accountId}>{c.displayName}</option>)}</select></label><button className="ap-secondary" disabled={!reason.trim() || !assignee} onClick={() => void send('assignment', { assigneeAccountId: assignee, reason })}>Phân công</button><button disabled={candidatePage === 0} onClick={() => setCandidatePage(p => p - 1)}>Reviewer trước</button><button disabled={!candidates?.hasNext} onClick={() => setCandidatePage(p => p + 1)}>Reviewer sau</button></div>}
        {canReview && selected.allowedActions.includes('BEGIN') && <button className="ap-primary" disabled={!reason.trim() || !policyVersion} onClick={() => void send('review', { action: 'BEGIN', revision: selected.revision, policyVersion, checklist: null, reason })}>Bắt đầu thẩm định</button>}
        {canReview && selected.allowedActions.includes('REQUEST_INFORMATION') && <><div className="ap-review-checklist">{Object.entries(labels).map(([key, label]) => <label key={key}><input type="checkbox" checked={checks[key] === true} onChange={e => setChecks(c => ({ ...c, [key]: e.target.checked }))} />{label}</label>)}</div><button className="ap-primary" disabled={!selected.allowedActions.includes('READY_FOR_DECISION') || !review || !reason.trim() || Object.keys(labels).some(k => checks[k] !== true)} onClick={() => void send('review', { action: 'READY_FOR_DECISION', revision: selected.revision, policyVersion: review!.policyVersion, checklist: checks, reason })}>Hoàn tất thẩm định</button><label>Nội dung cần bổ sung<select value={requested} onChange={e => setRequested(e.target.value)}>{Object.entries(fields).map(([key, label]) => <option key={key} value={key}>{label}</option>)}</select></label><button className="ap-secondary" disabled={!reason.trim()} onClick={() => void send('information', { revision: selected.revision, fieldCodes: [requested], reason })}>Yêu cầu bổ sung</button></>}
      </fieldset>}
      {permissions.includes('application.decide') && selected.allowedActions.includes('REJECT') && policyVersion && <AdminRejection key={selected.id + ':' + selected.version} application={selected} policyVersion={policyVersion} lock={setBusy} committed={current => { setSelected(current);setHistory(null);setReview(null);setChecks({});setRefresh(n => n + 1); }} />}
      {permissions.includes('audit.read') && <section><h3>Lịch sử nghiệp vụ</h3><button className="ap-secondary" type="button" disabled={busy || unknown} onClick={() => void events(0)}>Đọc lịch sử hồ sơ</button>
        {history && <><div className="ap-table-scroll"><table><thead><tr><th>Thời gian</th><th>Sự kiện</th><th>Phiên bản</th><th>Revision</th><th>Mã lý do</th></tr></thead><tbody>{history.items.map(e => <tr key={e.id}><td>{new Date(e.occurredAt).toLocaleString('vi-VN', { timeZone: 'Asia/Ho_Chi_Minh' })}</td><td>{e.operation}</td><td>{e.priorVersion} → {e.newVersion}</td><td>{e.revision ?? '—'}</td><td>{e.reasonCode}</td></tr>)}</tbody></table></div>{!history.items.length && <p>Chưa có sự kiện trong phạm vi này.</p>}<div className="ap-pagination"><button disabled={busy || unknown || history.page === 0} onClick={() => void events(history.page - 1)}>Lịch sử trước</button><span>Trang {history.page + 1}</span><button disabled={busy || unknown || !history.hasNext} onClick={() => void events(history.page + 1)}>Lịch sử sau</button></div></>}
      </section>}
    </section>}
  </section>;
}
