import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { adminSession, AdminSessionError } from './session';
import { parseGovernanceContext } from './governance';
import type { GovernanceContext } from './governance';
import { SourceState } from '../internal/InternalShell';

/** Own current authority only, read through the approved governance-context API. */
export default function AuthorityView({ accountId }: { accountId: string }) {
  const [context, setContext] = useState<GovernanceContext | null>(null), [error, setError] = useState(''), [refresh, setRefresh] = useState(0);
  const canReadWork = context?.assignments.some(a => a.scopeType === 'POLICY' && a.permissions.includes('task.read') && a.permissions.includes('application.read')) ?? false;
  useEffect(() => { let active = true;setContext(null);setError('');void adminSession.read('governance-context', new URLSearchParams(), v => parseGovernanceContext(v, accountId)).then(v => { if (active) setContext(v); }).catch(e => { if (active) setError(e instanceof AdminSessionError && e.code === 'ACCESS_DENIED' ? 'Quyền hiện tại không cho phép đọc context.' : 'Chưa tải được thẩm quyền hiện hành.'); });return () => { active = false; }; }, [accountId, refresh]);
  return <section className="ap-authority" aria-label="Thẩm quyền xét duyệt hiện hành">
    <div className="ap-v2-title"><div><h2>Authority Matrix</h2><p>Phạm vi và capability của chính tài khoản đang đăng nhập.</p></div><button className="ap-secondary" onClick={() => setRefresh(n => n + 1)}>Đối chiếu quyền hiện hành</button></div>
    {error && <p role="alert">{error}</p>}{!context && !error && <p role="status">Đang kiểm tra thẩm quyền…</p>}
    {context && <section className="ap-panel"><div className="ap-table-scroll"><table><thead><tr><th>Phạm vi</th><th>Định danh</th><th>Capabilities</th><th>Version</th><th>Hiệu lực đến</th></tr></thead><tbody>{context.assignments.map(a => <tr key={a.id}><td>{a.scopeType === 'POLICY' ? 'Merchant Policy' : 'Organization'}</td><td><code>{a.policyId ?? a.organizationId}</code></td><td>{a.permissions.map(p => <span className="ap-capability" key={p}>{p}</span>)}</td><td>{a.version}</td><td>{new Date(a.expiresAt).toLocaleString('vi-VN', { timeZone: 'Asia/Ho_Chi_Minh' })}</td></tr>)}</tbody></table></div>{!context.assignments.length && <p>Không có assignment còn hiệu lực.</p>}</section>}
    <section className="ap-panel"><h2>Maker–Checker · Merchant Application</h2><ol className="ap-authority-steps"><li>Customer nộp revision với liên hệ và bằng chứng đã xác minh.</li><li>Reviewer được phân công thực hiện checklist trên đúng revision.</li><li>Decider độc lập, có scope hiện hành, xác thực lại trước quyết định.</li><li>Backend kiểm tra version, xung đột và authority; activation Organization/Owner phải nguyên tử.</li></ol><p>Không tự duyệt hồ sơ của mình; không dùng reviewer đã bị thu hồi. Approval không cấp quyền Carrier, Finance hoặc Identity Management.</p>{canReadWork && <Link className="ap-secondary" to="/admin/account?tab=approvals">Xem quyết định đang chờ của tôi</Link>}</section>
    <SourceState state="UNAVAILABLE" title="Delegated Authority & Approval Policy Management">Registry loại quyết định, hạn mức, ủy quyền có thời hạn và phát hành policy cho Pricing, Claims, Finance, IAM hoặc Release chưa có technical contract được duyệt. Trang này không cấp, đổi hoặc thu hồi quyền; không đại diện cho quản lý policy toàn nền tảng.</SourceState>
  </section>;
}
