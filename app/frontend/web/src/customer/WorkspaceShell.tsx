import { useEffect, useRef, useState } from 'react';
import type { ReactNode } from 'react';
import { Link, NavLink, useLocation, useNavigate } from 'react-router-dom';
import { customerClient } from './client';
import type { Subject } from './client';
import { PublicBrand } from './PublicWebsite';
import './workspace.css';
import { PersonalAnalyticsDashboard } from './PersonalAnalytics';

function Icon({ name }: { name: string }) {
  const paths: Record<string, string> = {
    home: 'm3 10 9-7 9 7v10H3Zm6 10v-7h6v7',
    plus: 'M12 5v14M5 12h14', box: 'm3 7 9-5 9 5v10l-9 5-9-5Zm0 0 9 5 9-5M12 12v10',
    address: 'M5 3h14v18H5ZM3 7h4M3 12h4M3 17h4M10 8h5M10 12h5',
    search: 'M21 21l-5-5M18 10a8 8 0 1 0-16 0 8 8 0 0 0 16 0',
    label: 'M6 8V3h12v5M6 17H3V8h18v9h-3M6 13h12v8H6Z',
    receipt: 'M5 3h14v19l-3-2-4 2-4-2-3 2ZM9 8h6M9 12h6',
    help: 'M9 9a3 3 0 0 1 6 0c0 2-3 2-3 5m0 3h.01M22 12a10 10 0 1 0-20 0 10 10 0 0 0 20 0',
    user: 'M5 21v-2a7 7 0 0 1 14 0v2M16 7a4 4 0 1 0-8 0 4 4 0 0 0 8 0',
    business: 'M3 21V7h7v14M10 3h10v18M1 21h22M6 11h1m-1 4h1M14 7h2m-2 4h2m-2 4h2',
    check: 'm5 12 4 4L19 6', arrow: 'M5 12h14m-5-5 5 5-5 5',
    chevron: 'm6 9 6 6 6-6', menu: 'M4 6h16M4 12h16M4 18h16',
    exit: 'M10 4H4v16h6M13 7l5 5-5 5M8 12h10', shield: 'M12 2 3 6v6c0 5 9 10 9 10s9-5 9-10V6ZM8 12l3 3 5-6',
  };
  return <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.7" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true"><path d={paths[name] ?? paths.box} /></svg>;
}

export const workspaceModules = [
  { path: 'create', icon: 'plus', title: 'Tạo vận đơn', description: 'Một hành trình gửi hàng rõ ràng, từ thông tin đến xác nhận.', reason: 'Chưa mở tạo vận đơn. Danh mục dịch vụ, vùng phục vụ, bảng giá và phương thức thu cước chưa được thiết lập. Không có vận đơn hoặc yêu cầu lấy hàng nào được tạo tại đây.' },
  { path: 'shipments', icon: 'box', title: 'Quản lý vận đơn', description: 'Danh sách và hành trình các vận đơn thuộc tài khoản của bạn.', reason: 'Chưa có nguồn dữ liệu vận đơn Personal. Danh sách, bộ lọc, kiện hàng và tracking sẽ được kết nối cùng nghiệp vụ gửi hàng; vận đơn demo Merchant không được sử dụng trong không gian này.' },
  { path: 'addresses', icon: 'address', title: 'Sổ địa chỉ', description: 'Thông tin người gửi và người nhận của riêng bạn.', reason: 'Sổ địa chỉ cá nhân chưa được triển khai. Địa chỉ trong hồ sơ Merchant chỉ phục vụ hồ sơ đăng ký, không tự trở thành điểm lấy hàng hoặc địa chỉ đã được xác nhận.' },
  { path: 'lookup', icon: 'search', title: 'Tra cứu & Báo giá', description: 'Tra cứu hành trình, dịch vụ và cước phù hợp.', reason: 'Tra cứu và báo giá chưa khả dụng vì chưa có dữ liệu hành trình, vùng phục vụ và bảng giá được duyệt. Không hiển thị phí, thời gian giao hoặc địa điểm minh họa.' },
  { path: 'labels', icon: 'label', title: 'In nhãn & Tiện ích', description: 'Chứng từ gửi hàng gắn với vận đơn của bạn.', reason: 'Mở chi tiết vận đơn để tải nhãn gửi hàng và mã vạch. Công cụ in hàng loạt chưa khả dụng.' },
  { path: 'billing', icon: 'receipt', title: 'Cước phí & Thống kê', description: 'Cước xác nhận và nghĩa vụ cước theo vận đơn.', reason: 'Chứng từ hợp lệ được truy cập từ từng vận đơn.' },
  { path: 'support', icon: 'help', title: 'Hỗ trợ & Khiếu nại', description: 'Hướng dẫn tài khoản và yêu cầu hỗ trợ liên quan đến vận đơn.', reason: 'Tạo và theo dõi ticket vận đơn chưa được triển khai. Bạn vẫn có thể xem hướng dẫn sử dụng tài khoản trong trung tâm trợ giúp.' },
] as const;

function ManagementNavigation() {
  const location = useLocation(), trigger = useRef<HTMLButtonElement>(null);
  const active = ['/personal/shipments', '/personal/drafts', '/personal/billing', '/personal/action-required'].some(path => location.pathname.startsWith(path));
  const [open, setOpen] = useState(active);
  return <div className={'pw-management-links ' + (active ? 'is-active' : '')} onKeyDown={event => { if (event.key === 'Escape' && open) { event.stopPropagation(); setOpen(false); trigger.current?.focus(); } }}>
    <button className="pw-management-toggle" ref={trigger} type="button" aria-expanded={open} aria-controls="personal-management-links" onClick={() => setOpen(value => !value)}><Icon name="box" /><span>Quản lý</span><Icon name="chevron" /></button>
    <div className="pw-management-items" id="personal-management-links" hidden={!open}>
    <NavLink to="/personal/shipments" className={({ isActive }) => isActive || location.pathname.startsWith('/personal/drafts/') ? 'active' : undefined}>Quản lý vận đơn</NavLink>
    <NavLink to="/personal/action-required">Đơn hàng cần xử lý</NavLink>
    <NavLink to="/personal/billing">Cước phí & Thống kê</NavLink>
    </div>
  </div>;
}

function WorkspaceSwitcher() {
  const [open, setOpen] = useState(false), container = useRef<HTMLDivElement>(null), trigger = useRef<HTMLButtonElement>(null);
  useEffect(() => {
    if (!open) return;
    const outside = (event: PointerEvent) => { if (!container.current?.contains(event.target as Node)) setOpen(false); };
    const escape = (event: KeyboardEvent) => { if (event.key === 'Escape') { setOpen(false); trigger.current?.focus(); } };
    const close = () => setOpen(false);
    document.addEventListener('pointerdown', outside); document.addEventListener('keydown', escape);
    window.addEventListener('resize', close); window.addEventListener('scroll', close, true);
    return () => { document.removeEventListener('pointerdown', outside); document.removeEventListener('keydown', escape); window.removeEventListener('resize', close); window.removeEventListener('scroll', close, true); };
  }, [open]);
  return <div className="pw-switcher" ref={container} onBlur={event => { if (!event.currentTarget.contains(event.relatedTarget as Node | null)) setOpen(false); }}>
    <button className="pw-workspace-trigger" ref={trigger} type="button" aria-expanded={open} aria-controls="workspace-choices" onClick={() => setOpen(value => !value)}><Icon name="user" /><span>ArgoPost Personal</span><Icon name="chevron" /></button>
    {open && <div id="workspace-choices" className="pw-workspace-choices" role="group" aria-label="Chọn không gian làm việc" onPointerDown={event => { if ((event.target as Element).closest('button:disabled')) event.preventDefault(); }}>
      <Link className="pw-workspace-choice" to="/personal" aria-current="page" onClick={() => setOpen(false)}><span className="pw-choice-icon"><Icon name="user" /></span><span><strong>ArgoPost Personal</strong><small>Tài khoản cá nhân</small></span><Icon name="check" /></Link>
      <button type="button" className="pw-workspace-choice pw-workspace-business" disabled><span className="pw-choice-icon"><Icon name="business" /></span><span><strong>ArgoPost Business</strong><small>Tài khoản doanh nghiệp</small></span><span className="pw-workspace-status">Chưa kích hoạt</span></button>
    </div>}
  </div>;
}

export function WorkspaceShell({ subject, children }: { subject: Subject; children: ReactNode }) {
  const [sidebar, setSidebar] = useState(false), [loggingOut, setLoggingOut] = useState(false);
  const menu = useRef<HTMLButtonElement>(null);
  const location = useLocation();
  useEffect(() => {
    if (!sidebar) return;
    const escape = (event: KeyboardEvent) => { if (event.key === 'Escape') { setSidebar(false); menu.current?.focus(); } };
    document.addEventListener('keydown', escape); return () => document.removeEventListener('keydown', escape);
  }, [sidebar]);
  const avatar = subject.displayName.trim().split(/\s+/u).slice(-2).map(part => [...part][0]).join('').toLocaleUpperCase('vi');
  async function logout() { if (loggingOut) return; setLoggingOut(true); try { await customerClient.logout(); } catch { /* Client clears local authority and exposes unknown outcome on login. */ } finally { setLoggingOut(false); } }
  return <div className="pw-shell"><a className="cp-skip" href="#main-content">Đến nội dung chính</a>
    <header className="pw-header"><button ref={menu} className="pw-icon-button pw-menu-toggle" type="button" aria-label={sidebar ? 'Đóng menu cá nhân' : 'Mở menu cá nhân'} aria-expanded={sidebar} aria-controls="personal-sidebar" onClick={() => setSidebar(value => !value)}><Icon name="menu" /></button><WorkspaceSwitcher key={location.pathname} />
      <div className="pw-user"><span className="pw-avatar" aria-hidden="true">{avatar}</span><details key={location.pathname} onKeyDown={event => { if (event.key === 'Escape') { event.currentTarget.open = false; event.currentTarget.querySelector('summary')?.focus(); } }}><summary aria-label={`Menu tài khoản ${subject.displayName}`}><span>{subject.displayName}</span><Icon name="chevron" /></summary><div className="pw-user-dropdown" onClick={event => { if ((event.target as HTMLElement).closest('a')) setSidebar(false); }}><strong>Tài khoản cá nhân</strong><p>Email đã xác minh</p><Link to="/personal/profile">Hồ sơ cá nhân</Link><Link to="/personal/security">Bảo mật tài khoản</Link><Link to="/personal/applications">Hồ sơ Merchant</Link></div></details></div>
      <button type="button" className="pw-logout" aria-label={loggingOut ? 'Đang đăng xuất…' : 'Đăng xuất'} disabled={loggingOut} onClick={() => { void logout(); }}><Icon name="exit" /><span>{loggingOut ? 'Đang đăng xuất…' : 'Đăng xuất'}</span></button>
    </header>
    {sidebar && <button className="pw-sidebar-overlay" type="button" aria-label="Đóng điều hướng cá nhân" onClick={() => setSidebar(false)} />}
    <aside id="personal-sidebar" className={`pw-sidebar ${sidebar ? 'is-open' : ''}`} aria-label="Thanh điều hướng Personal"><div className="pw-sidebar-brand"><PublicBrand /></div><span className="pw-nav-label">KHÔNG GIAN CÁ NHÂN</span>
      <nav aria-label="Điều hướng Personal" onClick={event => { if ((event.target as HTMLElement).closest('a')) setSidebar(false); }}>
        <Link to="/personal/overview" aria-current={location.pathname === '/personal' || location.pathname.startsWith('/personal/overview') ? 'page' : undefined} className={location.pathname === '/personal' || location.pathname.startsWith('/personal/overview') ? 'active' : undefined}><Icon name="home" /><span>Tổng quan</span></Link>
        {workspaceModules.filter(item => item.path !== 'billing').map(item => item.path === 'shipments' ? <ManagementNavigation key="management" /> : <NavLink key={item.path} to={'/personal/' + item.path}><Icon name={item.icon} /><span>{item.title}</span>{!['addresses', 'create', 'lookup'].includes(item.path) && <span className="pw-nav-dot" aria-label="Chưa khả dụng" />}</NavLink>)}
      </nav><Link className="pw-sidebar-foot" to="/support"><Icon name="help" /><span>Trung tâm trợ giúp</span><Icon name="arrow" /></Link>
    </aside>
    <div className="pw-content"><main id="main-content" key={`${subject.accountId}:${subject.securityVersion}:${location.pathname}`}>{children}</main><footer className="pw-footer"><span>© {new Date().getFullYear()} ArgoPost</span><Link to="/support">Trung tâm trợ giúp <Icon name="arrow" /></Link></footer></div>
  </div>;
}

export function WorkspaceDashboard({ subject }: { subject: Subject }) {
  return <PersonalAnalyticsDashboard subject={subject} />;
}

export function WorkspaceActionRequired() {
  return <section className="pm-action-page"><div className="pw-page-heading"><div><span className="pw-eyebrow">QUẢN LÝ</span><h1 tabIndex={-1}>Đơn hàng cần xử lý</h1><p>Các vấn đề và yêu cầu gắn với vận đơn của bạn.</p></div><Link className="pw-secondary" to="/personal/shipments">Quản lý vận đơn</Link></div><div className="pw-panel pm-dependency"><span className="pw-empty-icon"><Icon name="help" /></span><h2>Chưa có nguồn yêu cầu xử lý</h2><p>Ngoại lệ vận chuyển và ticket hỗ trợ chưa được kết nối. Màn hình này chưa xác nhận số lượng đơn cần xử lý.</p><div className="pm-dependency-grid"><div><strong>Sự cố vận chuyển</strong><span>Chờ nghiệp vụ Carrier Exception</span></div><div><strong>Yêu cầu hỗ trợ</strong><span>Chờ nghiệp vụ Support / Ticket</span></div><div><strong>Hành động xử lý</strong><span>Hiển thị khi có quyền và workflow hợp lệ</span></div></div><Link className="pw-primary" to="/personal/shipments">Xem vận đơn của bạn <Icon name="arrow" /></Link></div></section>;
}

export function WorkspaceModule({ name }: { name: string }) {
  const item = workspaceModules.find(module => module.path === name)!;
  return <section className="pw-module-page"><span className="pw-eyebrow">ARGOPOST PERSONAL</span><h1 tabIndex={-1}>{item.title}</h1><p className="pw-module-description">{item.description}</p><div className="pw-panel pw-module-state"><span className="pw-empty-icon"><Icon name={item.icon} /></span><span className="pw-tag">Chưa khả dụng</span><h2>Chức năng đang được chuẩn bị</h2><p>{item.reason}</p><div className="pw-module-actions"><Link className="pw-primary" to="/personal">Về tổng quan <Icon name="arrow" /></Link>{name === 'support' && <Link className="pw-secondary" to="/faq">Hướng dẫn tài khoản</Link>}</div></div></section>;
}

export function WorkspaceSecurity({ subject }: { subject: Subject }) {
  const [busy, setBusy] = useState(false), navigate = useNavigate();
  async function startRecovery() {
    if (busy) return; setBusy(true);
    try { await customerClient.logout(); navigate('/recover', { replace: true }); } catch { /* Unknown logout must not be presented as successful. */ } finally { setBusy(false); }
  }
  return <section className="pw-module-page"><span className="pw-eyebrow">TÀI KHOẢN CÁ NHÂN</span><h1 tabIndex={-1}>Bảo mật tài khoản</h1><p className="pw-module-description">Giữ quyền truy cập trong tay bạn.</p><div className="pw-panel pw-security-panel"><span className="pw-action-icon"><Icon name="shield" /></span><h2>Email đã xác minh</h2><p>{subject.verifiedEmail}</p><p className="pw-small">Thông tin liên hệ trong hồ sơ không thay đổi email đăng nhập hoặc quyền truy cập.</p></div><div className="pw-panel pw-security-panel"><h2>Đặt lại mật khẩu</h2><p>Đăng xuất rồi xác minh bằng mã gửi đến email của bạn để đặt mật khẩu mới. Luồng này kết thúc các phiên cũ sau khi đặt lại thành công.</p><button className="pw-secondary" type="button" disabled={busy} onClick={() => { void startRecovery(); }}>{busy ? 'Đang đăng xuất…' : 'Đăng xuất để đặt lại mật khẩu'}</button></div></section>;
}
