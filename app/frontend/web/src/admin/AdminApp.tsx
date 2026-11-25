import { useEffect, useRef, useState, useSyncExternalStore } from 'react';
import { Link, Navigate, Route, Routes, useLocation } from 'react-router-dom';
import AdminActivation from './Activation';
import { clearAdminInvitation } from './invitation';
import { adminSession, AdminSessionError } from './session';
import AdminDashboard from './Dashboard';
import './admin.css';

function errorText(code: string | null) {
  switch (code) {
    case 'INVALID_CREDENTIALS': return 'Tên đăng nhập hoặc mật khẩu không đúng, hoặc tài khoản chưa được phép truy cập.';
    case 'VALIDATION_FAILED': return 'Tên đăng nhập tối đa 80 ký tự; mật khẩu tối đa 72 byte. Vui lòng kiểm tra thông tin đã nhập.';
    case 'RATE_LIMITED': return 'Có quá nhiều yêu cầu. Vui lòng chờ trước khi thử lại.';
    case 'HTTPS_REQUIRED': return 'Đăng nhập quản trị yêu cầu kết nối HTTPS.';
    case 'UNSUPPORTED_BROWSER': return 'Trình duyệt chưa hỗ trợ quản lý phiên an toàn. Hãy dùng phiên bản Chrome hoặc Edge mới.';
    case 'LOGOUT_UNCONFIRMED': return 'Đã xóa phiên khỏi màn hình này nhưng chưa xác nhận được đăng xuất trên máy chủ. Hãy thử đăng xuất lại.';
    case 'OUTCOME_UNKNOWN': return 'Kết nối gián đoạn, chưa xác định được kết quả. Không có yêu cầu nào được tự động gửi lại.';
    case 'SESSION_INVALID': return 'Phiên không còn hiệu lực. Vui lòng đăng nhập lại.';
    case 'ACCESS_DENIED': return 'Yêu cầu bị từ chối. Kiểm tra cấu hình truy cập quản trị với người phụ trách hệ thống.';
    case 'NETWORK_UNAVAILABLE': return 'Không kết nối được máy chủ. Kiểm tra kết nối rồi thử lại.';
    default: return 'Chưa thể xác minh phiên quản trị. Vui lòng thử lại hoặc liên hệ người phụ trách hệ thống.';
  }
}
function Brand() {
  return <Link className="ap-brand" to="/admin/login" aria-label="ArgoPost Administration Console"><img src="/media/argopost-logo-primary.png" alt="ArgoPost" /><span>INTERNAL · ADMINISTRATION</span></Link>;
}
function Login() {
  const location = useLocation();
  const session = useSyncExternalStore(adminSession.subscribe, adminSession.getSnapshot, adminSession.getSnapshot);
  const [username, setUsername] = useState(''), [password, setPassword] = useState(''), [visible, setVisible] = useState(false), [busy, setBusy] = useState(false), [error, setError] = useState<string | null>(null);
  const running = useRef(false);
  if (session.status === 'authenticated') return <Navigate to="/admin" replace />;
  const blocked = session.error === 'HTTPS_REQUIRED' || session.error === 'UNSUPPORTED_BROWSER';
  const message = error ?? (session.error && session.error !== 'SESSION_INVALID' ? session.error : null);
  async function submit() {
    if (running.current || blocked) return;
    running.current = true; setBusy(true); setError(null);
    try { await adminSession.login(username, password); }
    catch (failure) { setError(failure instanceof AdminSessionError ? failure.code : 'SERVICE_UNAVAILABLE'); }
    finally { setPassword(''); setVisible(false); setBusy(false); running.current = false; }
  }
  return <main className="ap-root ap-login">
    <header><Brand /><a className="ap-public-link" href="/">Website ArgoPost</a></header>
    <div className="ap-login-layout">
      <aside className="ap-introduction">
        <span className="ap-eyebrow">KHÔNG GIAN ĐIỀU HÀNH</span>
        <h1>Kết nối dữ liệu.<br /><em>Quyết định rõ ràng.</em></h1>
        <p>Cổng quản trị dành cho nhân sự được ArgoPost cấp quyền.</p>
        <div className="ap-grid-art" aria-hidden="true"><span /><span /><span /><span /></div>
      </aside>
      <section className="ap-login-card" aria-labelledby="admin-login-title">
        <span className="ap-auth-icon" aria-hidden="true"><svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.6"><path d="m12 3 8 3v6c0 5-8 9-8 9s-8-4-8-9V6l8-3Z" /><path d="m8.5 12 2.5 2.5 4.5-5" /></svg></span>
        <h2 id="admin-login-title">Đăng nhập quản trị</h2>
        {location.state?.activated === true && <p role="status">Đã xác minh và đặt mật khẩu thành công. Hãy đăng nhập bằng tên đăng nhập trong email.</p>}
        <p>Sử dụng tài khoản quản trị đã được kích hoạt.</p>
        {session.status === 'loading' ? <p role="status">Đang kiểm tra phiên…</p> : <form onSubmit={event => { event.preventDefault(); void submit(); }}>
          <fieldset disabled={busy || blocked}>
            <label htmlFor="admin-username">Tên đăng nhập</label>
            <input id="admin-username" autoComplete="username" autoCapitalize="none" spellCheck={false} required maxLength={160} value={username} onChange={event => setUsername(event.target.value)} />
            <label htmlFor="admin-password">Mật khẩu</label>
            <div className="ap-password">
              <input id="admin-password" type={visible ? 'text' : 'password'} autoComplete="current-password" required maxLength={256} value={password} onChange={event => setPassword(event.target.value)} />
              <button type="button" aria-label={visible ? 'Ẩn mật khẩu' : 'Hiện mật khẩu'} aria-pressed={visible} onClick={() => setVisible(!visible)}>{visible ? 'Ẩn' : 'Hiện'}</button>
            </div>
          </fieldset>
          {message && <p className="ap-error" role="alert">{errorText(message)}</p>}
          <button className="ap-primary" type="submit" disabled={busy || blocked}>{busy ? 'Đang xác thực…' : 'Đăng nhập'}</button>
          {session.error === 'LOGOUT_UNCONFIRMED' && <button className="ap-secondary" type="button" disabled={busy} onClick={() => { if (running.current) return; running.current = true; setBusy(true); void adminSession.logout().catch(() => {}).finally(() => { setBusy(false); running.current = false; }); }}>Thử đăng xuất lại</button>}
        </form>}
        <p className="ap-access-help">Chưa có quyền truy cập? Liên hệ người phụ trách hệ thống để được hướng dẫn.</p>
      </section>
    </div>
    <footer>ArgoPost · Cổng quản trị</footer>
  </main>;
}
function AccessBoundary() {
  const session = useSyncExternalStore(adminSession.subscribe, adminSession.getSnapshot, adminSession.getSnapshot);
  const [busy, setBusy] = useState(false);
  if (session.status === 'loading') return <main className="ap-root ap-checking"><p role="status">Đang kiểm tra phiên quản trị…</p></main>;
  if (session.status !== 'authenticated' || !session.subject) return <Navigate to="/admin/login" replace />;
  return <AdminDashboard key={`${session.subject.id}:${session.subject.authorizationVersion}`} subject={session.subject} busy={busy} logout={() => { if (busy) return; setBusy(true); void adminSession.logout().catch(() => {}).finally(() => setBusy(false)); }} />;
}
export default function AdminApp() {
  const activation = useLocation().pathname === '/admin/activate';
  useEffect(() => {
    const previousTitle = document.title;
    document.title = 'ArgoPost | Administration Console';
    if (activation) return () => { document.title = previousTitle; };
    clearAdminInvitation();
    void adminSession.initialize();
    const check = () => { if (document.visibilityState === 'visible') void adminSession.checkSession(); };
    const timer = window.setInterval(check, 60000);
    const channel = typeof BroadcastChannel === 'undefined' ? null : new BroadcastChannel('argopost.system.session.events');
    let previous = adminSession.getSnapshot().status;
    const unsubscribe = adminSession.subscribe(() => { const next = adminSession.getSnapshot().status; if (previous === 'authenticated' && next === 'guest') channel?.postMessage('SESSION_CLEARED'); previous = next; });
    if (channel) channel.onmessage = event => { if (event.data === 'SESSION_CLEARED') adminSession.discard(); };
    window.addEventListener('focus', check);
    return () => { document.title = previousTitle; window.clearInterval(timer); window.removeEventListener('focus', check); unsubscribe(); channel?.close(); };
  }, [activation]);
  return <Routes><Route path="/admin/activate" element={<AdminActivation />} /><Route path="/admin/login" element={<Login />} /><Route path="/admin/*" element={<AccessBoundary />} /><Route path="*" element={<Navigate to="/admin/login" replace />} /></Routes>;
}
