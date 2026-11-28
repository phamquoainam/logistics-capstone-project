import { useState } from 'react';
import type { ReactNode } from 'react';
import { Link, useLocation } from 'react-router-dom';
import type { AdminContext } from '../admin/data';
import { adminGroups, adminModules, canOpenModule, canOpenOverview, canSwitchPortal, overviewEntries, overviewSelection, portalNames, resolveAdminModule } from './navigation';
import type { DataState, Portal } from './navigation';
import './internal.css';

export function SourceState({ state, title, children }: { state: DataState; title: string; children: ReactNode }) {
  const names: Record<DataState, string> = { AVAILABLE: 'Có nguồn dữ liệu', EMPTY: 'Không có bản ghi', UNAVAILABLE: 'Chưa có nguồn khả dụng', FORBIDDEN: 'Không có quyền truy cập', DEGRADED: 'Nguồn chưa đáp ứng' };
  return <section className={'iw-state iw-state-' + state.toLowerCase()} data-state={state} role={state === 'DEGRADED' || state === 'FORBIDDEN' ? 'alert' : undefined}><span>{names[state]}</span><h2>{title}</h2><p>{children}</p></section>;
}
type Props = { portal: Portal; name: string; environment?: 'LOCAL' | 'TEST'; permissions?: AdminContext['permissions']; domainCapabilities?: readonly string[]; path: string; busy?: boolean; logout?: () => void; logoutLabel?: string; children: ReactNode };
export default function InternalShell({ portal, name, environment, permissions = [], domainCapabilities = [], path, busy, logout, logoutLabel = 'Đăng xuất', children }: Props) {
  const [mobileOpen, setMobileOpen] = useState(false);
  const active = resolveAdminModule(path);
  const location = useLocation(), overview = overviewSelection(path, location.search);
  return <div className={'iw-app iw-' + portal}>
    <a className="iw-skip" href="#internal-content">Đến nội dung chính</a>
    <aside className={'iw-sidebar ' + (mobileOpen ? 'open' : '')} aria-label="Điều hướng nội bộ">
      <Link to={portal === 'admin' ? '/admin' : '/' + portal} className="iw-logo" onClick={() => setMobileOpen(false)}><img src="/media/argopost-logo-primary.png" alt="ArgoPost" /><span>INTERNAL WEB APPLICATION</span></Link>
      <p className="iw-nav-caption">{portalNames[portal]}</p>
      <nav aria-label="Menu phân hệ">
        {portal === 'admin' ? adminGroups.map(group => <details key={group.title} className="iw-nav-group" open={group.modules.includes(active?.id ?? '') || undefined}>
          <summary>{group.title}</summary><div>{group.modules.includes('ADM-01') ? overviewEntries.map(item => canOpenOverview(item.id, permissions, domainCapabilities) ? <Link key={item.id} to={item.path} aria-current={overview === item.id ? 'page' : undefined} onClick={() => setMobileOpen(false)}>{item.title}</Link> : <button key={item.id} disabled title="Cần quyền Backend tương ứng trong context hiện tại.">{item.title}<small>Chưa được cấp quyền</small></button>) : group.modules.map(id => {
            const item = adminModules.find(m => m.id === id)!;
            return canOpenModule(id, permissions, domainCapabilities) ? <Link key={id} to={item.path} aria-current={active?.id === id ? 'page' : undefined} onClick={() => setMobileOpen(false)}>{item.title}</Link> : <button key={id} disabled title={item.dependency}>{item.title}<small>{item.extension ? 'PROPOSED' : 'Chưa mở capability'}</small></button>;
          })}</div>
        </details>) : portal === 'finance' ? <><Link to="/finance" aria-current="page">CSKH & Hồ sơ hỗ trợ</Link><p className="iw-nav-note">Thu tiền, ledger, COD và payout chưa được mở trong admission này.</p></> : <Link to="/ops/station" aria-current={path === '/ops/station' ? 'page' : undefined}>Station Intake</Link>}
      </nav>
    </aside>
    {mobileOpen && <button className="iw-overlay" aria-label="Đóng menu nội bộ" onClick={() => setMobileOpen(false)} />}
    <div className="iw-content">
      <header className="iw-header"><button className="iw-menu-toggle" aria-label="Mở menu nội bộ" aria-expanded={mobileOpen} onClick={() => setMobileOpen(!mobileOpen)}>☰</button>
        <details className="iw-portals"><summary>{portalNames[portal]}</summary><div aria-label="Phân hệ nội bộ">{(['ops', 'finance', 'admin'] as const).map(target => <button key={target} disabled={!canSwitchPortal(portal, target)} aria-current={target === portal ? 'true' : undefined} title={target === portal ? 'Context hiện tại' : 'Cần admission và quyền Backend riêng; không kế thừa quyền từ phân hệ hiện tại.'}>{portalNames[target]}<small>{target === portal ? 'Context hiện tại' : 'Chưa có quyền chuyển context'}</small></button>)}</div></details>
        <div className="iw-identity"><span className="iw-environment">{environment ?? 'Chưa xác minh môi trường'}</span><span className="iw-session">{environment ? 'Phiên đã xác thực' : 'Chưa xác thực'}</span><strong>{name}</strong>{logout && <button className="ap-secondary" onClick={logout} disabled={busy}>{busy ? 'Đang đăng xuất…' : logoutLabel}</button>}</div>
      </header>
      <main id="internal-content" className="iw-main" tabIndex={-1}><nav className="iw-breadcrumbs" aria-label="Đường dẫn"><span>Nội bộ</span><span>/</span><Link to={portal === 'admin' ? '/admin' : '/' + portal}>{portalNames[portal]}</Link>{active && <><span>/</span><span aria-current="page">{active.title}</span></>}</nav>{children}</main>
      <footer className="iw-footer"><span>{portal === 'admin' ? 'Governance & Control Plane · Thẩm quyền theo từng nghiệp vụ' : portal === 'finance' ? 'Business & Finance Execution Plane · SUPPORT context' : 'Operational Execution Plane · OPS context · Quyền theo địa điểm'}</span><span>Giờ Việt Nam · UTC+07:00</span></footer>
    </div>
  </div>;
}
