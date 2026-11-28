import { useEffect, useRef, useState } from 'react';
import { adminSession, AdminSessionError } from './session';
import { stateLabels } from './governance';
import type { ApplicationSummary, OrganizationSummary } from './governance';
import { applicationSearchPage, organizationSearchPage, comment, commentPage, comparison, checks, profile, members } from './review-views';
import type { Comparison, ReviewComment, SearchPage, Member } from './review-views';

const errorText = (error: unknown) => error instanceof AdminSessionError && error.code === 'SNAPSHOT_EXPIRED' ? 'Dữ liệu hoặc quyền đã thay đổi. Tìm lại từ trang đầu.' : error instanceof AdminSessionError && error.code === 'ACCESS_DENIED' ? 'Bạn chưa có đủ quyền cho dữ liệu này.' : error instanceof AdminSessionError && error.code === 'OUTCOME_UNKNOWN' ? 'Chưa xác định kết quả lưu. Thử lại đúng yêu cầu đang chờ.' : 'Chưa thể đọc dữ liệu nâng cao trong phạm vi hoặc môi trường hiện tại.';
const printable = (v: unknown) => v === null || v === undefined ? '—' : typeof v === 'string' ? v : JSON.stringify(v);

export function MerchantAdvancedSearch({ kind, from, to, policy, reviewer, state, locked, open }: { kind: 'application' | 'organization'; from?: string; to?: string; policy?: string; reviewer?: string; state?: string; locked: boolean; open: (value: { id: string }) => void }) {
  const [name, setName] = useState(''), [legal, setLegal] = useState(''), [account, setAccount] = useState(''), [code, setCode] = useState('');
  const [rows, setRows] = useState<SearchPage<ApplicationSummary | OrganizationSummary> | null>(null), [error, setError] = useState(''), [busy, setBusy] = useState(false);
  const mounted = useRef(false), running = useRef(false), query = useRef<unknown>(null), scope = kind + ':' + from + ':' + to + ':' + policy + ':' + reviewer + ':' + state;
  useEffect(() => { mounted.current = true;return () => { mounted.current = false; }; }, []);
  // Remounting by the owning screen's filter key clears previous disclosed results.
  async function search(next = false) {
    if (running.current || locked) return;running.current = true;setBusy(true);setError('');
    if (!next) query.current = kind === 'application' ? { from, to, state: state || null, policyId: policy || null, reviewerId: reviewer || null, applicantAccountId: account || null, registeredName: name || null, legalIdentifier: legal || null, purpose: 'MERCHANT_REVIEW', checkCode: code || null } : { state: state || null, currentOwnerAccountId: account || null, registeredName: name || null, legalIdentifier: legal || null, purpose: 'ORGANIZATION_OVERSIGHT' };
    const payload = { ...(query.current as object), page: { size: 20, cursor: next ? rows?.nextCursor : null } };setRows(null);
    try { const result = kind === 'application' ? await adminSession.governanceView('application-search', payload, applicationSearchPage) : await adminSession.governanceView('organization-search', payload, organizationSearchPage);if (mounted.current) setRows(result); }
    catch (e) { if (mounted.current) setError(errorText(e)); }finally { running.current = false;if (mounted.current) setBusy(false); }
  }
  return <details className="ap-panel" key={scope}><summary>Tìm kiếm nâng cao theo dữ liệu đã xác minh</summary><p>Khớp chính xác tên pháp lý hoặc mã đăng ký trên toàn bộ phạm vi được cấp. Dữ liệu tìm kiếm không đưa vào URL.</p>
    <form className="ap-filters" onSubmit={e => { e.preventDefault();void search(); }}><label>Tên pháp lý<input value={name} maxLength={240} disabled={locked || busy} onChange={e => { setName(e.target.value);setRows(null); }} /></label><label>Mã đăng ký<input value={legal} maxLength={80} disabled={locked || busy} onChange={e => { setLegal(e.target.value);setRows(null); }} /></label><label>{kind === 'application' ? 'Applicant Account UUID' : 'Current Owner Account UUID'}<input value={account} maxLength={36} disabled={locked || busy} onChange={e => { setAccount(e.target.value);setRows(null); }} /></label>
      {kind === 'application' && <label>Kiểm tra nguồn<select value={code} disabled={locked || busy} onChange={e => { setCode(e.target.value);setRows(null); }}><option value="">Không lọc vấn đề</option><option value="BUSINESS_KEY_CONFLICT">Trùng đăng ký đã ghi nhận</option><option value="CONTACT_REVERIFY_REQUIRED">Cần xác minh lại liên hệ</option><option value="EVIDENCE_NOT_CLEAN">Tài liệu chưa đủ điều kiện</option><option value="REVIEWER_CONFLICT">Xung đột người thẩm định</option></select></label>}
      <button className="ap-secondary" disabled={locked || busy}>Tìm trong phạm vi</button></form>
    {busy && <p role="status">Đang tìm theo nguồn Merchant…</p>}{error && <p className="ap-error" role="alert">{error}</p>}
    {rows && <><p>{rows.matchedCount} kết quả phù hợp</p><div className="ap-table-scroll"><table><thead><tr><th>Hồ sơ / Tổ chức</th><th>Trạng thái</th><th>Chi tiết</th></tr></thead><tbody>{rows.items.map(row => <tr key={row.id}><td>{'legalNameMasked' in row ? row.legalNameMasked : row.displayName}</td><td>{stateLabels[row.state] ?? row.state}</td><td><button className="ap-secondary" disabled={locked || busy} onClick={() => open(row)}>Mở kết quả</button></td></tr>)}</tbody></table></div>{!rows.items.length && <p>Không có kết quả trong phạm vi và bộ lọc này.</p>}<button className="ap-secondary" disabled={locked || busy || !rows.nextCursor} onClick={() => void search(true)}>Kết quả tiếp theo</button></>}
  </details>;
}

export function MerchantRevisionTools({ application, permissions, locked }: { application: ApplicationSummary; permissions: string[]; locked: boolean }) {
  const [from, setFrom] = useState(1), [to, setTo] = useState(application.revision), [diff, setDiff] = useState<Comparison | null>(null);
  const [comments, setComments] = useState<ReviewComment[] | null>(null), [cursor, setCursor] = useState<string | null>(null), [facts, setFacts] = useState<{ code: string; result: string }[] | null>(null);
  const [text, setText] = useState(''), [busy, setBusy] = useState(false), [error, setError] = useState(''), [uncertain, setUncertain] = useState(false);
  const mounted = useRef(false), running = useRef(false), pending = useRef<{ text: string; key: string; version: number; revision: number } | null>(null);
  useEffect(() => { mounted.current = true;return () => { mounted.current = false;pending.current = null; }; }, []);
  useEffect(() => { if (!uncertain) return;const warn = (event: BeforeUnloadEvent) => event.preventDefault();window.addEventListener('beforeunload', warn);return () => window.removeEventListener('beforeunload', warn); }, [uncertain]);
  async function run(action: () => Promise<void>) { if (running.current || locked) return;running.current = true;setBusy(true);setError('');try { await action(); } catch (e) { if (mounted.current) setError(errorText(e)); }finally { running.current = false;if (mounted.current) setBusy(false); } }
  async function history(next: boolean) { const value = await adminSession.governanceView('comments', { size: 20, cursor: next ? cursor : null }, raw => commentPage(raw, application.id), application.id);if (mounted.current) { setComments(value.items);setCursor(value.nextCursor); } }
  async function append() {
    pending.current ??= { text: text.normalize('NFC'), key: crypto.randomUUID(), version: application.version, revision: application.revision };const request = pending.current;
    try { await adminSession.governanceWrite('comment', application.id, request.version, request.key, { revision: request.revision, text: request.text }, raw => comment(raw, application.id));pending.current = null;if (mounted.current) { setText('');setUncertain(false);setComments(null);setCursor(null); } }
    catch (failure) { if (mounted.current) { const lost = failure instanceof AdminSessionError && ['OUTCOME_UNKNOWN', 'SERVICE_UNAVAILABLE', 'RESPONSE_INVALID'].includes(failure.code);setUncertain(lost);if (!lost) pending.current = null; }throw failure; }
  }
  return <section aria-label="Đối chiếu và ghi chú thẩm định"><h3>Đối chiếu và ghi chú thẩm định</h3>
    {permissions.includes('application.evidence.read') && <><div className="ap-filters"><label>Từ revision<input type="number" min={1} max={application.revision - 1} value={from} disabled={busy || locked || uncertain} onChange={e => setFrom(Number(e.target.value))} /></label><label>Đến revision<input type="number" min={2} max={application.revision} value={to} disabled={busy || locked || uncertain} onChange={e => setTo(Number(e.target.value))} /></label><button className="ap-secondary" disabled={busy || locked || uncertain || from >= to || to > application.revision} onClick={() => void run(async () => { setDiff(null);const value = await adminSession.governanceView('comparison', { fromRevision: from, toRevision: to, purpose: 'MERCHANT_REVIEW' }, raw => comparison(raw, application.id), application.id);if (mounted.current) setDiff(value); })}>So sánh revision</button></div>
      {diff && <div className="ap-table-scroll"><table><caption>Đối chiếu nội dung đã nộp</caption><thead><tr><th>Trường</th><th>Trước</th><th>Sau</th></tr></thead><tbody>{diff.changes.map(row => <tr key={row.field}><td>{row.field}</td><td>{printable(row.before)}</td><td>{printable(row.after)}</td></tr>)}</tbody></table><p>Tài liệu thêm: {diff.evidenceAdded.length} · Tài liệu bỏ: {diff.evidenceRemoved.length}</p>{!diff.changes.length && <p>Không thay đổi các trường thông tin được đối chiếu.</p>}</div>}
      <button className="ap-secondary" disabled={busy || locked || uncertain} onClick={() => void run(async () => { setFacts(null);const result = await adminSession.governanceView('checks', {}, raw => checks(raw, application.id), application.id);if (mounted.current) setFacts(result); })}>Kiểm tra nguồn hiện hành</button>
      {facts && <dl>{facts.map(f => <div key={f.code}><dt>{f.code}</dt><dd>{f.result}</dd></div>)}</dl>}
      <button className="ap-secondary" disabled={busy || locked || uncertain} onClick={() => void run(() => history(false))}>Đọc ghi chú nội bộ</button>
      {comments && <><ul>{comments.map(row => <li key={row.id}><p>{row.text}</p><small>Revision {row.revision} · {new Date(row.createdAt).toLocaleString('vi-VN')} · {row.actorAccountId}</small></li>)}</ul>{!comments.length && <p>Chưa có ghi chú.</p>}<button disabled={busy || locked || uncertain || !cursor} onClick={() => void run(() => history(true))}>Ghi chú tiếp theo</button></>}
    </>}
    {permissions.includes('application.comment.write') && <form onSubmit={e => { e.preventDefault();void run(append); }}><label>Ghi chú nội bộ<textarea value={text} maxLength={2000} disabled={busy || locked || uncertain} onChange={e => setText(e.target.value)} /></label><p>Ghi chú được lưu bất biến, không thay đổi quyết định hoặc phiên bản nghiệp vụ.</p><button className="ap-secondary" disabled={busy || locked || !text.trim() && !uncertain}>{uncertain ? 'Thử lại ghi chú đang chờ' : 'Lưu ghi chú'}</button></form>}
    {busy && <p role="status">Đang xác minh quyền và nguồn…</p>}{error && <p className="ap-error" role="alert">{error}</p>}
  </section>;
}

export function MerchantOrganizationViews({ organization, locked }: { organization: OrganizationSummary; locked: boolean }) {
  const [blocks, setBlocks] = useState<Record<string, Record<string, unknown>> | null>(null), [rows, setRows] = useState<Member[] | null>(null), [cursor, setCursor] = useState<string | null>(null), [pendingCount, setPendingCount] = useState(0), [busy, setBusy] = useState(false), [error, setError] = useState('');
  const mounted = useRef(false), running = useRef(false);useEffect(() => { mounted.current = true;return () => { mounted.current = false; }; }, []);
  async function disclose(kind: 'profile' | 'members', next = false) {
    if (running.current || locked) return;running.current = true;setBusy(true);setError('');
    try { if (kind === 'profile') { setBlocks(null);const result = await adminSession.governanceView('profile', { purpose: 'ORGANIZATION_OVERSIGHT' }, raw => profile(raw, organization.id), organization.id);if (mounted.current) setBlocks(result); }
      else { setRows(null);const result = await adminSession.governanceView('members', { purpose: 'ORGANIZATION_OVERSIGHT', page: { size: 20, cursor: next ? cursor : null } }, raw => members(raw, organization.id), organization.id);if (mounted.current) { setRows(result.items);setCursor(result.nextCursor);setPendingCount(result.pendingInvitationCount); } }
    }catch(e) { if (mounted.current) setError(errorText(e)); }finally { running.current = false;if (mounted.current) setBusy(false); }
  }
  return <section aria-label="Hồ sơ và thành viên tổ chức"><h3>Hồ sơ và thành viên tổ chức</h3><button className="ap-secondary" disabled={locked || busy} onClick={() => void disclose('profile')}>Đọc hồ sơ tổ chức</button><button className="ap-secondary" disabled={locked || busy} onClick={() => void disclose('members')}>Đọc metadata thành viên</button>
    {blocks && Object.entries(blocks).map(([key, b]) => <section key={key}><h4>{{ legal: 'Pháp lý', contact: 'Liên hệ đã xác minh', facilities: 'Địa điểm Merchant', pricing: 'Hợp đồng giá', holds: 'Hạn chế hoạt động' }[key]}</h4>{b.availability === 'AVAILABLE' ? <dl>{Object.entries(b).filter(([k]) => !['availability', 'sourceVersion', 'asOf', 'reason'].includes(k)).map(([k, value]) => <div key={k}><dt>{k}</dt><dd>{printable(value)}</dd></div>)}</dl> : <p>UNAVAILABLE · Chưa có nguồn được phép tích hợp.</p>}</section>)}
    {rows && <><p>{pendingCount} lời mời đang chờ · Chỉ xem metadata, không cấp hoặc sửa quyền.</p><div className="ap-table-scroll"><table><thead><tr><th>Membership</th><th>Account</th><th>Vai trò</th><th>Trạng thái</th><th>Owner</th></tr></thead><tbody>{rows.map(row => <tr key={row.membershipId}><td>{row.membershipId}</td><td>{row.accountId}</td><td>{row.role}</td><td>{row.state}</td><td>{row.isCurrentOwner ? 'Current Owner' : '—'}</td></tr>)}</tbody></table></div><button disabled={locked || busy || !cursor} onClick={() => void disclose('members', true)}>Thành viên tiếp theo</button></>}
    {busy && <p role="status">Đang đọc theo scope tổ chức…</p>}{error && <p className="ap-error" role="alert">{error}</p>}
  </section>;
}
