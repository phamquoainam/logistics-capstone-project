import { useRef, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { acceptAdminInvitation, AdminInvitationError, clearAdminInvitation, hasAdminInvitation } from './invitation';

export default function AdminActivation() {
  const navigate = useNavigate(), running = useRef(false), key = useRef(crypto.randomUUID());
  const [password, setPassword] = useState(''), [confirmation, setConfirmation] = useState(''), [busy, setBusy] = useState(false), [error, setError] = useState<string | null>(null);
  const available = hasAdminInvitation() && location.protocol === 'https:';
  const messages: Record<string, string> = {
    PASSWORD_INVALID: 'Mật khẩu cần từ 15 đến 72 ký tự, tối đa 72 byte UTF-8.',
    PASSWORD_MISMATCH: 'Hai mật khẩu chưa trùng khớp.',
    TOKEN_INVALID_OR_EXPIRED: 'Lời mời không còn hợp lệ. Hãy liên hệ người phụ trách để được kiểm tra hoặc gửi lại.',
    RATE_LIMITED: 'Có quá nhiều yêu cầu. Vui lòng chờ trước khi thử lại.',
    OUTCOME_UNKNOWN: 'Kết nối gián đoạn. Chưa xác định được kết quả; bạn có thể thử lại với cùng mật khẩu.',
    IDEMPOTENCY_CONFLICT: 'Lần gửi lại không khớp yêu cầu trước. Hãy liên hệ người phụ trách để kiểm tra.',
    ACCESS_DENIED: 'Yêu cầu bị từ chối. Hãy mở lời mời bằng phiên trình duyệt riêng cho quản trị.',
  };
  async function submit() {
    if (running.current || !available) return;
    if (password !== confirmation) { setError('PASSWORD_MISMATCH'); return; }
    running.current = true; setBusy(true); setError(null);
    try { await acceptAdminInvitation(password, key.current); navigate('/admin/login', { replace: true, state: { activated: true } }); }
    catch (failure) { setError(failure instanceof AdminInvitationError ? failure.code : 'SERVICE_UNAVAILABLE'); }
    finally { setPassword(''); setConfirmation(''); setBusy(false); running.current = false; }
  }
  return <main className="ap-root ap-login">
    <header><Link className="ap-brand" to="/admin/login" onClick={clearAdminInvitation}><img src="/media/argopost-logo-primary.png" alt="ArgoPost" /><span>ADMIN PORTAL</span></Link></header>
    <section className="ap-login-card ap-activation" aria-labelledby="admin-activation-title">
      <h1 id="admin-activation-title">Kích hoạt tài khoản quản trị</h1>
      {available ? <><p>Xác minh lời mời và đặt mật khẩu của bạn. Kích hoạt tài khoản không tự tạo phiên đăng nhập hoặc mở thêm quyền.</p>
        <form onSubmit={event => { event.preventDefault(); void submit(); }}>
          <fieldset disabled={busy}>
            <label htmlFor="admin-new-password">Mật khẩu mới</label><input id="admin-new-password" type="password" autoComplete="new-password" required value={password} onChange={event => setPassword(event.target.value)} />
            <label htmlFor="admin-confirm-password">Nhập lại mật khẩu</label><input id="admin-confirm-password" type="password" autoComplete="new-password" required value={confirmation} onChange={event => setConfirmation(event.target.value)} />
          </fieldset>
          <p className="ap-access-help">Từ 15 đến 72 ký tự, tối đa 72 byte UTF-8. Không dùng mật khẩu đã chia sẻ.</p>
          {error && <p role="alert" className="ap-error">{messages[error] ?? 'Chưa thể hoàn tất xác minh. Vui lòng thử lại sau.'}</p>}
          <button className="ap-primary" disabled={busy} type="submit">{busy ? 'Đang xác minh…' : 'Xác minh và đặt mật khẩu'}</button>
        </form></> : <p role="alert">Chưa có lời mời hợp lệ. Hãy mở liên kết HTTPS trong email mới nhất hoặc liên hệ người phụ trách.</p>}
      <Link className="ap-public-link" to="/admin/login" onClick={clearAdminInvitation}>Về đăng nhập</Link>
    </section>
  </main>;
}
