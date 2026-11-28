import { useEffect, useRef, useState, useSyncExternalStore } from 'react';
import type { ReactNode } from 'react';
import { Link, Navigate, Route, Routes, useNavigate } from 'react-router-dom';
import { operationsClient } from './operations-client';
import { clearOperationsInvitation, hasOperationsInvitation } from './operations-invitation-proof';
import StationOperations from './StationOperations';
import { CustomerApiError } from './client';
import InternalShell from '../internal/InternalShell';
import './customer.css';
import './staff.css';

function errorText(error: unknown) {
  if (error instanceof CustomerApiError) {
    if (error.code === 'INVALID_CREDENTIALS') return 'Email hoặc mật khẩu chưa đúng, hoặc tài khoản Operations chưa được kích hoạt.';
    if (error.code === 'TOKEN_INVALID_OR_EXPIRED') return 'Lời mời đã hết hạn hoặc không còn hiệu lực. Liên hệ người quản trị lời mời.';
    if (error.code === 'OUTCOME_UNKNOWN') return 'Chưa xác định được kết quả. Chỉ thử lại đúng yêu cầu vừa gửi; không thay đổi mật khẩu.';
    if (error.status === 429) return 'Bạn đã gửi quá nhiều yêu cầu. Vui lòng chờ rồi thử lại.';
  }
  return 'Chưa thể hoàn tất. Kiểm tra cấu hình Local/Test hoặc liên hệ người quản trị.';
}
function Frame({ title, children }: { title: string; children: ReactNode }) {
  return <main className="cp-app staff-page"><header className="staff-header"><Link to="/ops/login"><img src="/media/argopost-logo-primary.png" alt="ArgoPost" /></Link><span>Không gian Operations</span></header><section className="staff-auth-card"><h1>{title}</h1>{children}</section></main>;
}
function StaffLogin() {
  const session = useSyncExternalStore(operationsClient.subscribe, operationsClient.getSnapshot, operationsClient.getSnapshot);
  const [email, setEmail] = useState(''), [password, setPassword] = useState(''), [error, setError] = useState(''), [busy, setBusy] = useState(false);
  const running = useRef(false);
  if (session.status === 'authenticated') return <Navigate to="/ops/station" replace />;
  return <Frame title="Đăng nhập Operations"><p>Dành cho tài khoản nhân viên đã được xác minh.</p><form onSubmit={event => { event.preventDefault(); if (running.current) return; running.current = true; setBusy(true); setError(''); void operationsClient.login(email, password).catch(e => setError(errorText(e))).finally(() => { setPassword(''); setBusy(false); running.current = false; }); }}><fieldset disabled={busy}><label htmlFor="staff-email">Email nhân viên</label><input id="staff-email" type="email" required autoComplete="username" maxLength={254} value={email} onChange={e => setEmail(e.target.value)} /><label htmlFor="staff-password">Mật khẩu</label><input id="staff-password" type="password" required autoComplete="current-password" maxLength={256} value={password} onChange={e => setPassword(e.target.value)} /></fieldset>{error && <p role="alert">{error}</p>}<button className="cp-primary" disabled={busy} type="submit">{busy ? 'Đang đăng nhập…' : 'Đăng nhập'}</button></form></Frame>;
}
function StaffActivate() {
  const navigate = useNavigate(), [available] = useState(hasOperationsInvitation);
  const [password, setPassword] = useState(''), [confirmation, setConfirmation] = useState(''), [error, setError] = useState(''), [busy, setBusy] = useState(false), [uncertain, setUncertain] = useState(false);
  const running = useRef(false), pending = useRef<{ password: string; key: string } | undefined>(undefined);
  useEffect(() => () => { pending.current = undefined; }, []);
  async function submit() {
    if (running.current) return;
    if (!pending.current) { if (password !== confirmation || [...password].length < 15 || new TextEncoder().encode(password).length > 72) { setError('Mật khẩu cần ít nhất 15 ký tự, tối đa 72 byte và nhập lại trùng khớp.'); return; } pending.current = { password, key: crypto.randomUUID() }; }
    running.current = true; setBusy(true); setError('');
    try { await operationsClient.accept(pending.current.password, pending.current.key); pending.current = undefined; navigate('/ops/login', { replace: true }); }
    catch (e) { const unknown = e instanceof CustomerApiError && (e.code === 'OUTCOME_UNKNOWN' || e.status === 503); setUncertain(unknown); if (!unknown) pending.current = undefined; setError(errorText(e)); }
    finally { setPassword(''); setConfirmation(''); setBusy(false); running.current = false; }
  }
  return <Frame title="Kích hoạt tài khoản Operations">{!available ? <p role="alert">Mở liên kết lời mời còn hiệu lực trong email để tiếp tục.</p> : <><p>Xác minh email và đặt mật khẩu cho tài khoản nhân viên. Lời mời có hiệu lực 24 giờ và chỉ dùng một lần.</p><form onSubmit={e => { e.preventDefault(); void submit(); }}><fieldset disabled={busy || uncertain}><label htmlFor="staff-new-password">Mật khẩu mới</label><input id="staff-new-password" type="password" autoComplete="new-password" required maxLength={72} value={password} onChange={e => setPassword(e.target.value)} /><label htmlFor="staff-confirm-password">Nhập lại mật khẩu</label><input id="staff-confirm-password" type="password" autoComplete="new-password" required maxLength={72} value={confirmation} onChange={e => setConfirmation(e.target.value)} /></fieldset>{error && <p role="alert">{error}</p>}<button className="cp-primary" disabled={busy} type="submit">{busy ? 'Đang xác minh…' : uncertain ? 'Thử lại đúng yêu cầu' : 'Kích hoạt tài khoản'}</button></form></>}<Link to="/ops/login" onClick={clearOperationsInvitation}>Về đăng nhập Operations</Link></Frame>;
}
function OperationsStation() {
  const state = useSyncExternalStore(operationsClient.subscribe, operationsClient.getSnapshot, operationsClient.getSnapshot);
  const [error, setError] = useState('');
  if (state.status === 'loading') return <Frame title="Checking Operations session"><p role="status">Loading...</p></Frame>;
  if (state.status !== 'authenticated' || !state.subject) return <Navigate to="/ops/login" replace />;
  return <InternalShell portal="ops" path="/ops/station" name={state.subject.displayName} environment={state.subject.environment} logout={() => { void operationsClient.logout().catch(e => setError(errorText(e))); }}>{error && <p role="alert">{error}</p>}<StationOperations key={state.subject.accountId} subject={state.subject} client={operationsClient} embedded /></InternalShell>;
}
export default function OperationsApp() {
  useEffect(() => { void operationsClient.initialize(); }, []);
  return <Routes><Route path="/ops/login" element={<StaffLogin />} /><Route path="/ops/activate" element={<StaffActivate />} /><Route path="/ops/station" element={<OperationsStation />} /><Route path="*" element={<Navigate to="/ops/station" replace />} /></Routes>;
}
