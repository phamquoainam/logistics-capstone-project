import { useSearchParams, Link } from 'react-router-dom';
import type { AdminContext } from './data';
import { AccountContext } from './ConsoleViews';
import MerchantWork from './MerchantWork';
import { SourceState } from '../internal/InternalShell';

export default function AdminWorkCenter({ context, capabilities }: { context: AdminContext; capabilities: readonly string[] }) {
  const [params, setParams] = useSearchParams(), tab = params.get('tab') ?? 'queue';
  const work = capabilities.includes('task.read') && capabilities.includes('application.read');
  const tabs = [{ id: 'queue', label: 'My Queue' }, { id: 'approvals', label: 'Approvals' }, { id: 'notifications', label: 'Notifications' }, { id: 'profile', label: 'Profile' }, { id: 'security', label: 'MFA & Sessions' }, { id: 'preferences', label: 'Preferences' }];
  return <section className="ap-work-center" aria-label="Trung tâm công việc Admin">
    <div className="iw-tabs" role="tablist" aria-label="Công việc và tài khoản">{tabs.map(item => <button key={item.id} type="button" role="tab" aria-selected={tab === item.id} onClick={() => setParams({ tab: item.id })}>{item.label}</button>)}</div>
    {!tabs.some(item => item.id === tab) && <p role="alert">Mục tài khoản không hợp lệ.</p>}
    {['queue', 'approvals'].includes(tab) && (work ? <MerchantWork key={tab} initialKind={tab === 'approvals' ? 'MERCHANT_DECISION' : ''} /> : <SourceState state="FORBIDDEN" title="Chưa có quyền công việc">Cần cả task.read và application.read trong cùng phạm vi được Backend cấp. Không có công việc Operations hoặc Finance được kế thừa vào đây.</SourceState>)}
    {['queue', 'approvals'].includes(tab) && capabilities.length > 0 && <p className="ap-work-links"><Link to="/admin/governance/authority">Quyền xét duyệt & nguyên tắc độc lập</Link>{capabilities.includes('audit.read') && <Link to="/admin/audit">Lịch sử quản trị</Link>}</p>}
    {tab === 'notifications' && <SourceState state="UNAVAILABLE" title="Thông báo Admin chưa có nguồn">Outbox hiện tại gửi kết quả hồ sơ cho Customer. Đây không phải hộp thông báo của Admin. Incident, thay đổi mạng lưới, Finance và tùy chọn nhận thông báo cần nguồn và contract riêng.</SourceState>}
    {tab === 'profile' && <AccountContext context={context} />}
    {tab === 'security' && <section className="ap-panel"><h2>Bảo mật phiên quản trị</h2><dl><dt>Context đang dùng</dt><dd>SYSTEM · Administration Console</dd><dt>Phiên hiện tại</dt><dd>Đã được Backend xác thực; kiểm tra lại quyền khi gọi API.</dd></dl><p>Đăng xuất ở header thu hồi phiên hiện tại. Thay đổi quyền làm mất hiệu lực phiên theo Identity policy.</p><SourceState state="UNAVAILABLE" title="MFA & danh sách phiên">Chưa có API Admin cho đăng ký MFA, liệt kê thiết bị, đổi mật khẩu hoặc đăng xuất phiên khác. Không sử dụng luồng Customer để thay đổi credential SYSTEM.</SourceState></section>}
    {tab === 'preferences' && <section className="ap-panel"><h2>Tùy chọn hiển thị</h2><dl><dt>Ngôn ngữ</dt><dd>Tiếng Việt</dd><dt>Múi giờ thống kê</dt><dd>{context.timeZone}</dd><dt>Chuyển động</dt><dd>Theo tùy chọn giảm chuyển động của hệ điều hành.</dd></dl><p>Chưa lưu tùy chọn thông báo hoặc dashboard vào tài khoản khi chưa có contract AccountPreference.</p></section>}
  </section>;
}
