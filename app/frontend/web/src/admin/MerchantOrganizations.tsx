import { useEffect, useRef, useState } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import MerchantMenu from './MerchantMenu';
import OrganizationCases from './OrganizationCases';
import { MerchantAdvancedSearch, MerchantOrganizationViews } from './MerchantReviewViews';
import { adminSession, AdminSessionError } from './session';
import { governanceId, parseOrganization, parseOrganizationPage } from './governance';
import type { OrganizationSummary, OrganizationPage } from './governance';

type Intent = { id: string; version: number; operation: 'SUSPEND' | 'REINSTATE'; reason: string; digest: string; proof?: string; stepKey: string; commandKey: string };
export async function organizationCommandDigest(id: string, version: number, operation: Intent['operation'], reason: string) {
  const input = JSON.stringify({ operation, originalVersion: version, reason, target: id });
  return [...new Uint8Array(await crypto.subtle.digest('SHA-256', new TextEncoder().encode(input)))].map(b => b.toString(16).padStart(2, '0')).join('');
}
function proofResult(raw: unknown) {
  if (!raw || typeof raw !== 'object' || Array.isArray(raw)) throw new AdminSessionError('RESPONSE_INVALID');
  const value = raw as Record<string, unknown>;
  if (Object.keys(value).length !== 2 || !governanceId(value.proofId) || typeof value.expiresAt !== 'string' || !Number.isFinite(Date.parse(value.expiresAt)) || Date.parse(value.expiresAt) <= Date.now() || Date.parse(value.expiresAt) > Date.now() + 901000) throw new AdminSessionError('RESPONSE_INVALID');
  return value.proofId as string;
}
function errorMessage(failure: unknown) {
  const code = failure instanceof AdminSessionError ? failure.code : 'SERVICE_UNAVAILABLE';
  return code === 'ACCESS_DENIED' ? 'Bạn không có quyền quản trị tổ chức này.' : code === 'RESOURCE_NOT_FOUND' ? 'Tổ chức không thuộc phạm vi hiện tại.' : code === 'SESSION_INVALID' ? 'Phiên đăng nhập đã hết hiệu lực.' : 'Chưa thể hoàn thành yêu cầu. Kiểm tra kết nối hoặc tải lại dữ liệu hiện tại.';
}
function Lifecycle({ organization, updated, lock }: { organization: OrganizationSummary; updated: (value: OrganizationSummary) => void; lock: (locked: boolean) => void }) {
  const operation = organization.state === 'ACTIVE' ? 'SUSPEND' : 'REINSTATE';
  const [reason, setReason] = useState(''), [password, setPassword] = useState(''), [busy, setBusy] = useState(false), [uncertain, setUncertain] = useState(false), [error, setError] = useState('');
  const pending = useRef<Intent | null>(null), active = useRef(false), running = useRef(false);
  useEffect(() => { active.current = true;return () => { active.current = false;pending.current = null; }; }, []);
  useEffect(() => { if (!uncertain) return;const warn = (event: BeforeUnloadEvent) => event.preventDefault();window.addEventListener('beforeunload', warn);return () => window.removeEventListener('beforeunload', warn); }, [uncertain]);
  async function submit() {
    if (running.current) return;running.current = true;setBusy(true);setError('');lock(true);const credential = password;setPassword('');
    try {
      if (!pending.current) {
        const normalized = reason.normalize('NFC');if (!normalized.trim() || [...normalized].length > 300 || /[\p{Cc}\p{Cs}\u2028\u2029]/u.test(normalized)) throw new AdminSessionError('VALIDATION_FAILED');
        pending.current = { id: organization.id, version: organization.version, operation, reason: normalized, digest: await organizationCommandDigest(organization.id, organization.version, operation, normalized), stepKey: crypto.randomUUID(), commandKey: crypto.randomUUID() };
      }
      const i = pending.current;
      if (!i.proof) { if (!credential) throw new AdminSessionError('VALIDATION_FAILED');i.proof = await adminSession.governanceWrite('stepup', i.id, i.version, i.stepKey, { operation: i.operation, targetId: i.id, expectedVersion: i.version, commandDigest: i.digest, password: credential }, proofResult); }
      await adminSession.governanceWrite(i.operation === 'SUSPEND' ? 'suspension' : 'reinstatement', i.id, i.version, i.commandKey, { reason: i.reason, reauthenticationProofId: i.proof }, value => parseOrganization(value, i.id));
      const current = await adminSession.read('organization-detail', new URLSearchParams(), value => parseOrganization(value, i.id), i.id);
      pending.current = null;if (active.current) { setUncertain(false);lock(false);updated(current); }
    } catch (failure) {
      const code = failure instanceof AdminSessionError ? failure.code : 'SERVICE_UNAVAILABLE', unknown = ['OUTCOME_UNKNOWN', 'NETWORK_UNAVAILABLE', 'RESPONSE_INVALID', 'SERVICE_UNAVAILABLE'].includes(code);
      if (!unknown) pending.current = null;
      if (active.current) { setUncertain(unknown);if (!unknown) lock(false);setError(unknown ? 'Chưa xác định kết quả. Thử lại đúng yêu cầu đã gửi; nếu chưa có proof, nhập lại mật khẩu tại đây.' : errorMessage(failure)); }
    } finally { running.current = false;if (active.current) setBusy(false); }
  }
  if (!organization.allowedActions.includes(operation)) return null;
  return <section className="ap-review-decision"><h3>{operation === 'SUSPEND' ? 'Tạm ngừng tổ chức' : 'Khôi phục tổ chức'}</h3><p>Thay đổi quyền truy cập Business theo trạng thái tổ chức; không xóa tài khoản, vận đơn hoặc dữ liệu tài chính.</p>
    <label>Lý do thay đổi<textarea maxLength={300} value={reason} disabled={busy || uncertain} onChange={e => setReason(e.target.value)} /></label>
    {(!uncertain || !pending.current?.proof) && <label>Mật khẩu xác thực lại<input type="password" autoComplete="current-password" value={password} disabled={busy} onChange={e => setPassword(e.target.value)} /></label>}
    {error && <p role="alert" className="ap-error">{error}</p>}
    <button type="button" className="ap-primary" disabled={busy || !uncertain && (!reason.trim() || !password) || uncertain && !pending.current?.proof && !password} onClick={() => void submit()}>{busy ? 'Đang xác nhận…' : uncertain ? 'Thử lại đúng yêu cầu' : operation === 'SUSPEND' ? 'Xác thực & tạm ngừng' : 'Xác thực & khôi phục'}</button>
  </section>;
}
export default function MerchantOrganizations() {
  const [params] = useSearchParams();
  const view = params.get('view') === 'owner-transfer' ? 'owner-transfer' : params.get('view') === 'lifecycle' ? 'lifecycle' : 'directory';
  const organizationId = params.get('organizationId');
  const [rows, setRows] = useState<OrganizationPage | null>(null), [selected, setSelected] = useState<OrganizationSummary | null>(null), [state, setState] = useState(''), [page, setPage] = useState(0), [refresh, setRefresh] = useState(0), [loading, setLoading] = useState(true), [error, setError] = useState(''), [locked, setLocked] = useState(false);
  const mounted = useRef(false), generation = useRef(0);
  useEffect(() => { mounted.current = true;return () => { mounted.current = false; }; }, []);
  useEffect(() => {
    if (view === 'owner-transfer') { setLoading(false);setRows(null);return; }
    let active = true;setLoading(true);setError('');setRows(null);const parameters = new URLSearchParams({ page: String(page), size: '20' });if (state) parameters.set('state', state);
    void adminSession.read('organization-list', parameters, value => parseOrganizationPage(value, page)).then(value => { if (active) setRows(value); }).catch(failure => { if (active) setError(errorMessage(failure)); }).finally(() => { if (active) setLoading(false); });return () => { active = false; };
  }, [state, page, refresh, view]);
  useEffect(() => {
    if (!organizationId || view === 'owner-transfer') return;
    let active = true;const request = ++generation.current;setSelected(null);setError('');setLoading(true);
    if (!governanceId(organizationId)) { setError('Mã Organization chưa hợp lệ.');setLoading(false);return; }
    void adminSession.read('organization-detail', new URLSearchParams(), raw => parseOrganization(raw, organizationId), organizationId).then(value => { if (active && request === generation.current) setSelected(value); }).catch(failure => { if (active && request === generation.current) setError(errorMessage(failure)); }).finally(() => { if (active && request === generation.current) setLoading(false); });
    return () => { active = false; };
  }, [organizationId, view]);
  async function open(id: string) {
    if (locked) return;const request = ++generation.current;setLoading(true);setError('');setSelected(null);
    try { const value = await adminSession.read('organization-detail', new URLSearchParams(), raw => parseOrganization(raw, id), id);if (mounted.current && request === generation.current) setSelected(value); }
    catch (failure) { if (mounted.current && request === generation.current) setError(errorMessage(failure)); }finally { if (mounted.current && request === generation.current) setLoading(false); }
  }
  if (view === 'owner-transfer') return <section className="ap-governance"><MerchantMenu current={view} /><section className="ap-panel"><h1>Owner Transfer</h1><p>BLOCKED_COORDINATION · Policy nghiệp vụ đã được duyệt; chưa mở thực thi chuyển Owner.</p><p>Initial Owner và bảo vệ Owner cuối cùng vẫn được giữ nguyên. Sau chuyển, Owner cũ phải là ACTIVE OPERATOR với duy nhất dashboard.read; chờ các guards Team và Identity được phối hợp kiểm chứng.</p><p>Cần xác minh bên chuyển và bên nhận, quyết định độc lập, xử lý tranh chấp, thu hồi quyền và audit nguyên tử trước khi mở thao tác.</p></section></section>;
  return <section className="ap-governance"><MerchantMenu current={view} locked={locked} /><header className="ap-page-heading"><div><h1>{view === 'lifecycle' ? 'Organization Lifecycle' : 'Tổ chức Merchant'}</h1><p>Directory và trạng thái truy cập trong phạm vi quản trị được cấp.</p></div></header>
    {view === 'lifecycle' && <p>Chọn tổ chức để xử lý theo quyền hiện hành. Hồ sơ RESTRICTED/CLOSED và risk hold đang ở bước chuẩn bị; chưa mở thực thi các trạng thái này.</p>}
    <div className="ap-filter-row"><label>Trạng thái tổ chức<select value={state} disabled={locked} onChange={e => { setState(e.target.value);setPage(0);setSelected(null); }}><option value="">Tất cả trạng thái</option><option value="ACTIVE">Đang hoạt động</option><option value="SUSPENDED">Tạm ngừng</option></select></label><button className="ap-secondary" disabled={locked || loading} onClick={() => { setSelected(null);setRefresh(n => n + 1); }}>Làm mới</button></div>
    {!selected && <MerchantAdvancedSearch key={state} kind="organization" state={state} locked={locked} open={item => void open(item.id)} />}
    {error && <p className="ap-error" role="alert">{error}</p>}{loading && <p role="status">Đang đọc dữ liệu…</p>}
    {!selected && rows && <><div className="ap-table-scroll"><table><thead><tr><th>Doanh nghiệp</th><th>Trạng thái</th><th>Nguồn tiếp nhận</th><th>Thành viên</th><th>Thao tác</th></tr></thead><tbody>{rows.items.map(item => <tr key={item.id}><td>{item.displayName}</td><td>{item.state === 'ACTIVE' ? 'Đang hoạt động' : 'Tạm ngừng'}</td><td>{item.admissionOrigin === 'APPLICATION_APPROVED' ? 'Hồ sơ đã phê duyệt' : 'Operator Local/Test'}</td><td>{Object.values(item.membershipCounts).flatMap(states => Object.values(states)).reduce((sum, count) => sum + count, 0)}</td><td><button className="ap-text-button" disabled={locked || loading} onClick={() => void open(item.id)}>Mở tổ chức</button></td></tr>)}</tbody></table></div>{!rows.items.length && <p>Chưa có tổ chức thuộc bộ lọc và phạm vi hiện tại.</p>}<div className="ap-pagination"><button disabled={locked || loading || page === 0} onClick={() => setPage(n => n - 1)}>Trang trước</button><span>Trang {page + 1}</span><button disabled={locked || loading || !rows.hasNext} onClick={() => setPage(n => n + 1)}>Trang sau</button></div></>}
    {selected && <section className="ap-panel"><div className="ap-panel-heading"><div><h2>{selected.displayName}</h2><p>{selected.state === 'ACTIVE' ? 'Đang hoạt động' : 'Tạm ngừng'} · Phiên bản {selected.version}</p></div><button disabled={locked} onClick={() => { generation.current++;setSelected(null); }}>Về directory</button></div>
      <dl><dt>Nguồn tiếp nhận</dt><dd>{selected.admissionOrigin === 'APPLICATION_APPROVED' ? 'Hồ sơ được duyệt qua quy trình độc lập' : 'Operator Local/Test; không phải hồ sơ xét duyệt'}</dd><dt>Initial Owner Membership</dt><dd>{selected.initialOwnerMembershipId}</dd>{selected.activation && <><dt>Thời điểm kích hoạt</dt><dd>{new Date(selected.activation.activatedAt).toLocaleString('vi-VN', { timeZone: 'Asia/Ho_Chi_Minh' })}</dd><dt>Hồ sơ</dt><dd>{selected.activation.applicationId} · Revision {selected.activation.revision}</dd></>}</dl>
      {selected.activation && <Link className="ap-secondary" to={'/admin/merchants/applications?view=review&applicationId=' + selected.activation.applicationId}>Xem hồ sơ và lịch sử xét duyệt</Link>}
      <h3>Thành viên theo vai trò và trạng thái</h3><div className="ap-table-scroll"><table><thead><tr><th>Vai trò</th><th>Trạng thái</th><th>Số thành viên</th></tr></thead><tbody>{Object.entries(selected.membershipCounts).flatMap(([role, states]) => Object.entries(states).map(([state, count]) => <tr key={role + state}><td>{role}</td><td>{state}</td><td>{count}</td></tr>))}</tbody></table></div>
      <MerchantOrganizationViews key={selected.id + ":" + selected.version} organization={selected} locked={locked} />
      {view === 'lifecycle' && <OrganizationCases key={selected.id + ":" + selected.version} organization={selected} />}
      <Lifecycle key={selected.id + ':' + selected.version} organization={selected} lock={setLocked} updated={value => { setSelected(value);setRefresh(n => n + 1); }} />
    </section>}
  </section>;
}
