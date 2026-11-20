import { useEffect, useRef, useState, useSyncExternalStore } from 'react';
import type { ReactNode } from 'react';
import { Link, Navigate, Route, Routes, useNavigate, useSearchParams } from 'react-router-dom';
import { staffClient } from './staff-client';
import { clearStaffInvitation, hasStaffInvitation } from './staff-invitation-proof';
import { OrganizationSupportDesk } from './OrganizationSupportDesk';
import { PersonalSupportWorkbench } from './SupportTickets';
import { CustomerApiError } from './client';
import './customer.css';
import './staff.css';

function errorText(error: unknown) {
  if (error instanceof CustomerApiError) {
    if (error.code === 'INVALID_CREDENTIALS') return 'Email hoặc mật khẩu chưa đúng, hoặc tài khoản CSKH chưa được kích hoạt.';
    if (error.code === 'TOKEN_INVALID_OR_EXPIRED') return 'Lời mời đã hết hạn hoặc không còn hiệu lực. Liên hệ người quản trị lời mời.';
    if (error.code === 'OUTCOME_UNKNOWN') return 'Chưa xác định được kết quả. Chỉ thử lại đúng yêu cầu vừa gửi; không thay đổi mật khẩu.';
    if (error.status === 429) return 'Bạn đã gửi quá nhiều yêu cầu. Vui lòng chờ rồi thử lại.';
  }
  return 'Chưa thể hoàn tất. Kiểm tra cấu hình Local/Test hoặc liên hệ người quản trị.';
}
function Frame({ title, children }: { title: string; children: ReactNode }) {
  return <main className="cp-app staff-page"><header className="staff-header"><Link to="/staff/login"><img src="/media/argopost-logo-primary.png" alt="ArgoPost" /></Link><span>Không gian CSKH</span></header><section className="staff-auth-card"><h1>{title}</h1>{children}</section></main>;
}
function StaffLogin() {
  const session = useSyncExternalStore(staffClient.subscribe, staffClient.getSnapshot, staffClient.getSnapshot);
  const [email, setEmail] = useState(''), [password, setPassword] = useState(''), [error, setError] = useState(''), [busy, setBusy] = useState(false);
  const running = useRef(false);
  if (session.status === 'authenticated') return <Navigate to="/staff/cases" replace />;
  return <Frame title="Đăng nhập CSKH"><p>Dành cho tài khoản nhân viên đã được xác minh.</p><form onSubmit={event => { event.preventDefault(); if (running.current) return; running.current = true; setBusy(true); setError(''); void staffClient.login(email, password).catch(e => setError(errorText(e))).finally(() => { setPassword(''); setBusy(false); running.current = false; }); }}><fieldset disabled={busy}><label htmlFor="staff-email">Email nhân viên</label><input id="staff-email" type="email" required autoComplete="username" maxLength={254} value={email} onChange={e => setEmail(e.target.value)} /><label htmlFor="staff-password">Mật khẩu</label><input id="staff-password" type="password" required autoComplete="current-password" maxLength={256} value={password} onChange={e => setPassword(e.target.value)} /></fieldset>{error && <p role="alert">{error}</p>}<button className="cp-primary" disabled={busy} type="submit">{busy ? 'Đang đăng nhập…' : 'Đăng nhập'}</button></form></Frame>;
}
function StaffActivate() {
  const navigate = useNavigate(), [available] = useState(hasStaffInvitation);
  const [password, setPassword] = useState(''), [confirmation, setConfirmation] = useState(''), [error, setError] = useState(''), [busy, setBusy] = useState(false), [uncertain, setUncertain] = useState(false);
  const running = useRef(false), pending = useRef<{ password: string; key: string } | undefined>(undefined);
  useEffect(() => () => { pending.current = undefined; }, []);
  async function submit() {
    if (running.current) return;
    if (!pending.current) { if (password !== confirmation || [...password].length < 15 || new TextEncoder().encode(password).length > 72) { setError('Mật khẩu cần ít nhất 15 ký tự, tối đa 72 byte và nhập lại trùng khớp.'); return; } pending.current = { password, key: crypto.randomUUID() }; }
    running.current = true; setBusy(true); setError('');
    try { await staffClient.accept(pending.current.password, pending.current.key); pending.current = undefined; navigate('/staff/login', { replace: true }); }
    catch (e) { const unknown = e instanceof CustomerApiError && (e.code === 'OUTCOME_UNKNOWN' || e.status === 503); setUncertain(unknown); if (!unknown) pending.current = undefined; setError(errorText(e)); }
    finally { setPassword(''); setConfirmation(''); setBusy(false); running.current = false; }
  }
  return <Frame title="Kích hoạt tài khoản CSKH">{!available ? <p role="alert">Mở liên kết lời mời còn hiệu lực trong email để tiếp tục.</p> : <><p>Xác minh email và đặt mật khẩu cho tài khoản nhân viên. Lời mời có hiệu lực 24 giờ và chỉ dùng một lần.</p><form onSubmit={e => { e.preventDefault(); void submit(); }}><fieldset disabled={busy || uncertain}><label htmlFor="staff-new-password">Mật khẩu mới</label><input id="staff-new-password" type="password" autoComplete="new-password" required maxLength={72} value={password} onChange={e => setPassword(e.target.value)} /><label htmlFor="staff-confirm-password">Nhập lại mật khẩu</label><input id="staff-confirm-password" type="password" autoComplete="new-password" required maxLength={72} value={confirmation} onChange={e => setConfirmation(e.target.value)} /></fieldset>{error && <p role="alert">{error}</p>}<button className="cp-primary" disabled={busy} type="submit">{busy ? 'Đang xác minh…' : uncertain ? 'Thử lại đúng yêu cầu' : 'Kích hoạt tài khoản'}</button></form></>}<Link to="/staff/login" onClick={clearStaffInvitation}>Về đăng nhập CSKH</Link></Frame>;
}
function StaffCases() {
  const [params] = useSearchParams();
  const session = useSyncExternalStore(staffClient.subscribe, staffClient.getSnapshot, staffClient.getSnapshot), [error, setError] = useState('');
  if (session.status === 'loading') return <Frame title="Kiểm tra phiên"><p role="status">Đang tải…</p></Frame>;
  if (session.status !== 'authenticated' || !session.subject) return <Navigate to="/staff/login" replace />;
  return <main className="cp-app staff-page"><header className="staff-header"><img src="/media/argopost-logo-primary.png" alt="ArgoPost" /><strong>{session.subject.displayName}</strong><button className="pw-secondary" onClick={() => { void staffClient.logout().catch(e => setError(errorText(e))); }}>Đăng xuất CSKH</button></header>{error && <p role="alert">{error}</p>}<div className="staff-desk"><nav aria-label="Phạm vi hồ sơ CSKH"><Link to="/staff/cases">Doanh nghiệp</Link> · <Link to="/staff/cases?scope=personal">Cá nhân</Link></nav>{params.get('scope') === 'personal' ? <PersonalSupportWorkbench subject={session.subject} /> : <OrganizationSupportDesk subject={session.subject} />}</div></main>;
}
export default function StaffApp() {
  useEffect(() => { void staffClient.initialize(); }, []);
  return <Routes><Route path="/staff/login" element={<StaffLogin />} /><Route path="/staff/activate" element={<StaffActivate />} /><Route path="/staff/cases" element={<StaffCases />} /><Route path="*" element={<Navigate to="/staff/login" replace />} /></Routes>;
}
