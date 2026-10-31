import { useEffect, useRef, useState, useSyncExternalStore } from 'react';
import type { FormEvent, ReactNode } from 'react';
import { Link, NavLink, Navigate, Outlet, Route, Routes, useLocation, useNavigate, useParams } from 'react-router-dom';
import { customerClient, CustomerApiError } from './client';
import type { Enrollment, ProfileInput, VersionedProfile, ApplicationAddress, ApplicationDraft, ApplicationPage, VersionedApplication } from './client';
import { clearEntryProof, getOtpChallenge, getVerificationLocator, hasEntryProof } from './entry-proof';
import PublicWebsite, { PublicBrand, PublicFooter, PublicHeader } from './PublicWebsite';
import './customer.css';

function message(error: unknown): string {
  if (!(error instanceof CustomerApiError)) return 'Không thể hoàn tất yêu cầu. Vui lòng thử lại sau.';
  const messages: Record<string, string> = {
    OUTCOME_UNKNOWN: 'Chưa xác định kết quả do kết nối bị gián đoạn. Yêu cầu có thể đã được xử lý. Vui lòng kiểm tra hướng dẫn trong email trước khi tiếp tục.',
    NETWORK_UNAVAILABLE: 'Không kết nối được máy chủ. Vui lòng kiểm tra kết nối và thử lại.',
    SERVICE_UNAVAILABLE: 'Dịch vụ tạm thời không khả dụng. Vui lòng thử lại sau.',
    INVALID_CREDENTIALS: 'Email hoặc mật khẩu chưa đúng.', SESSION_INVALID: 'Phiên đã kết thúc. Vui lòng đăng nhập lại.',
    REFRESH_REPLAY: 'Phiên đã kết thúc. Vui lòng đăng nhập lại.', SESSION_CHANGED: 'Phiên đăng nhập đã thay đổi ở một cửa sổ khác. Vui lòng đăng nhập lại.',
    CSRF_REJECTED: 'Yêu cầu bảo vệ đã hết hiệu lực. Vui lòng mở lại trang trước khi gửi một yêu cầu mới.',
    TOKEN_INVALID_OR_EXPIRED: 'Mã hoặc phiên xác minh không còn hiệu lực. Vui lòng yêu cầu mã mới.',
    VALIDATION_FAILED: 'Thông tin chưa hợp lệ. Vui lòng kiểm tra các trường đã nhập.',
    PRECONDITION_STALE: 'Hồ sơ đã thay đổi. Hãy tải bản mới nhất trước khi chỉnh sửa tiếp.',
    RESOURCE_NOT_FOUND: 'Không tìm thấy hồ sơ trong tài khoản hiện tại.',
    BUSINESS_CONFLICT: 'Thông tin pháp nhân trùng với hồ sơ đang xử lý. Vui lòng kiểm tra các hồ sơ hiện có hoặc liên hệ hỗ trợ.',
    IDEMPOTENCY_CONFLICT: 'Nội dung khác với yêu cầu trước đó. Vui lòng kiểm tra kết quả của yêu cầu cũ.',
    RESPONSE_INVALID: 'Không xác thực được phản hồi. Vui lòng thử lại sau.',
    UNSUPPORTED_BROWSER: 'Vui lòng dùng trình duyệt hỗ trợ phiên đăng nhập an toàn, như phiên bản Chrome hoặc Edge hiện hành.',
    ENTRY_PROOF_UNAVAILABLE: 'Liên kết xác nhận chưa hợp lệ. Vui lòng mở lại liên kết trong email.',
  };
  if (error.status === 429) return `Bạn đã gửi quá nhiều yêu cầu. ${error.retryAfter ? `Thử lại sau ${error.retryAfter} giây.` : 'Vui lòng chờ rồi thử lại.'}`;
  return messages[error.code] ?? 'Không thể hoàn tất yêu cầu. Vui lòng kiểm tra thông tin và thử lại sau.';
}
function Notice({ children, error = false }: { children: ReactNode; error?: boolean }) { return <div className={`cp-notice ${error ? 'cp-error' : ''}`} role={error ? 'alert' : 'status'}>{children}</div>; }
type AuthSuccess = 'registered' | 'password-reset';
function SuccessPopup({ kind, dismiss }: { kind: AuthSuccess; dismiss: () => void }) {
  return <div className="cp-success-popup" role="status" aria-live="polite" aria-atomic="true">
    <span className="cp-success-popup-icon" aria-hidden="true"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><path d="m5 12 4 4L19 6" /></svg></span>
    <div><strong>{kind === 'registered' ? 'Bạn đã đăng ký thành công!' : 'Bạn đã đổi mật khẩu thành công!'}</strong><p>{kind === 'registered' ? 'Chào mừng bạn đến với movapost. Đăng nhập để bắt đầu hành trình của bạn.' : 'Hãy đăng nhập bằng mật khẩu mới của bạn.'}</p></div>
    <button type="button" aria-label="Đóng thông báo thành công" onClick={dismiss}><svg aria-hidden="true" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8"><path d="m6 6 12 12M18 6 6 18" /></svg></button>
  </div>;
}
function Loading() { return <p className="cp-loading" role="status">Đang kiểm tra phiên và tải dữ liệu…</p>; }
function useSession() { return useSyncExternalStore(customerClient.subscribe, customerClient.getSnapshot, customerClient.getSnapshot); }
function useCommand<T>(work: (input: T, key: string) => Promise<void>) {
  const running = useRef(false), command = useRef<{ input: T; key: string } | undefined>(undefined);
  const [busy, setBusy] = useState(false), [error, setError] = useState(''), [uncertain, setUncertain] = useState(false), [done, setDone] = useState(false);
  useEffect(() => () => { customerClient.discardPending(); }, []);
  async function submit(input: T) {
    if (running.current) return;
    running.current = true; setBusy(true); setError('');
    const current = command.current ?? { input, key: crypto.randomUUID() }; command.current = current;
    try { await work(current.input, current.key); command.current = undefined; setUncertain(false); setDone(true); }
    catch (failure) {
      setError(message(failure));
      const unknown = failure instanceof CustomerApiError && (failure.code === 'OUTCOME_UNKNOWN' || failure.status === 503);
      setUncertain(unknown); if (!unknown) command.current = undefined;
    } finally { running.current = false; setBusy(false); }
  }
  async function resolve(lookup: (key: string) => Promise<void>) {
    if (running.current || !command.current) return;
    running.current = true; setBusy(true); setError('');
    try { await lookup(command.current.key); command.current = undefined; setUncertain(false); setDone(true); }
    catch { setError('Chưa xác nhận được kết quả lưu. Yêu cầu vẫn đang ở trạng thái chưa xác định; vui lòng kiểm tra lại sau.'); }
    finally { running.current = false; setBusy(false); }
  }
  return { submit, resolve, busy, error, uncertain, done, clearError: () => setError('') };
}
function passwordValid(value: string) { return [...value].length >= 15 && new TextEncoder().encode(value).length <= 72; }
function canonicalPhone(value: string) {
  const compact = value.replace(/[\s()-]/g, '');
  if (/^0[35789]\d{8}$/.test(compact)) return '+84' + compact.slice(1);
  if (/^84[35789]\d{8}$/.test(compact)) return '+' + compact;
  return compact;
}
const emailProps = { type: 'text', inputMode: 'email', autoCapitalize: 'none', autoCorrect: 'off', spellCheck: false, maxLength: 254, pattern: '[^\\s@]+@[^\\s@]+', required: true } as const;
function Page({ title, description, children, otp = false }: { title: string; description?: string; children: ReactNode; otp?: boolean }) { return <section className={`cp-page ${otp ? 'cp-otp-page' : ''}`}><p className="cp-eyebrow">MOVAPOST. · TÀI KHOẢN CÁ NHÂN</p><h1 tabIndex={-1}>{title}</h1>{description && <p className="cp-description">{description}</p>}{children}</section>; }
function PasswordField({ id, label, value, change, complete }: { id: string; label: string; value: string; change: (value: string) => void; complete: 'current-password' | 'new-password' }) {
  const [visible, setVisible] = useState(false);
  return <><label htmlFor={id}>{label}</label><div className="cp-password-field"><input id={id} type={visible ? 'text' : 'password'} autoComplete={complete} placeholder={id.includes('confirmation') ? 'Nhập lại mật khẩu' : 'Nhập mật khẩu'} required maxLength={complete === 'new-password' ? 72 : 256} value={value} onChange={event => change(event.target.value)} /><button type="button" aria-label={`${visible ? 'Ẩn' : 'Hiện'} nội dung trường ${id === 'register-confirmation' ? 'xác nhận' : 'mật khẩu'}`} aria-pressed={visible} onClick={() => setVisible(current => !current)}><svg aria-hidden="true" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.6"><path d="M2 12s3.5-6 10-6 10 6 10 6-3.5 6-10 6S2 12 2 12Z" /><circle cx="12" cy="12" r="3" />{visible && <path d="m3 3 18 18" />}</svg></button></div></>;
}
function Guest({ children }: { children: ReactNode }) { const session = useSession(); return session.status === 'authenticated' ? <Navigate to="/personal" replace /> : children; }
function Protected() { const session = useSession(); return session.status === 'authenticated' ? <Outlet key={`${session.subject?.accountId}:${session.subject?.securityVersion}`} /> : <Navigate to="/login" replace />; }
function Login() {
  const session = useSession(), [email, setEmail] = useState(''), [password, setPassword] = useState('');
  const action = useCommand<{ email: string; password: string }>(async input => { try { await customerClient.login(input.email, input.password); } finally { setPassword(''); } });
  if (session.status === 'authenticated') return <Navigate to="/personal" replace />;
  return <Page title="Đăng nhập bằng mật khẩu">
    {session.error && !action.error && session.error !== 'SESSION_INVALID' && <Notice error>{message(new CustomerApiError(session.error))}</Notice>}
    <form onSubmit={event => { event.preventDefault(); void action.submit({ email, password }); }}>
      <fieldset disabled={action.busy || action.uncertain}><label htmlFor="login-email">Email đã xác minh</label><input id="login-email" {...emailProps} autoComplete="username" placeholder="Nhập email đã xác minh" value={email} onChange={e => setEmail(e.target.value)} />
        <PasswordField id="login-password" label="Mật khẩu" complete="current-password" value={password} change={setPassword} /></fieldset>
      {action.error && <Notice error>{action.error}</Notice>}
      <button className="cp-primary" type="submit" disabled={action.busy}>{action.busy ? 'Đang đăng nhập…' : action.uncertain ? 'Đăng nhập lại với thông tin vừa gửi' : 'Đăng nhập'}</button>
    </form><div className="cp-form-links"><Link to="/recover">Quên mật khẩu?</Link></div><p className="cp-auth-switch">Bạn chưa có tài khoản? <Link to="/register">Đăng ký ngay</Link></p>
  </Page>;
}
function Register() {
  const navigate = useNavigate();
  const [email, setEmail] = useState(''), [name, setName] = useState(''), [phone, setPhone] = useState(''), [password, setPassword] = useState(''), [confirmation, setConfirmation] = useState(''), [validation, setValidation] = useState('');
  const action = useCommand<Enrollment>(async (input, key) => { await customerClient.enroll(input, key); setPassword(''); setConfirmation(''); navigate('/verify-email'); });
  function submit(event: FormEvent<HTMLFormElement>) { event.preventDefault(); const normalized = canonicalPhone(phone); if (!action.uncertain && !/^\+[1-9][0-9]{7,14}$/.test(normalized)) { setValidation('Nhập số di động Việt Nam bắt đầu bằng 0 hoặc số quốc tế có mã quốc gia.'); return; } if (!action.uncertain && !passwordValid(password)) { setValidation('Mật khẩu cần ít nhất 15 ký tự và không quá 72 byte UTF-8.'); return; } if (!action.uncertain && password !== confirmation) { setValidation('Mật khẩu nhập lại chưa khớp.'); return; } setValidation(''); void action.submit({ email, displayName: name, phone: normalized, password, consentVersion: 'S003-2026-10-27' }); }
  return <Guest><Page title="Tạo tài khoản mới">
    <p className="cp-register-intro">Bạn đã có tài khoản? <Link to="/login">Đăng nhập ngay</Link></p>
    <form onSubmit={submit}>
      <fieldset disabled={action.busy || action.uncertain}><label htmlFor="register-name">Họ và tên</label><input id="register-name" autoComplete="name" required maxLength={160} placeholder="Nhập họ và tên" value={name} onChange={e => setName(e.target.value)} />
        <label htmlFor="register-email">Email</label><input id="register-email" {...emailProps} autoComplete="email" placeholder="Nhập email" value={email} onChange={e => setEmail(e.target.value)} />
        <label htmlFor="register-phone">Số điện thoại liên hệ</label><input id="register-phone" type="tel" inputMode="tel" autoComplete="tel" required maxLength={32} placeholder="0901234567" value={phone} onChange={e => setPhone(e.target.value)} /><p className="cp-hint">Nhập số di động bắt đầu bằng 0, không cần thêm +84.</p>
        <PasswordField id="register-password" label="Mật khẩu" complete="new-password" value={password} change={setPassword} /><p id="password-rule" className="cp-hint">Ít nhất 15 ký tự, tối đa 72 byte UTF-8.</p>
        <PasswordField id="register-confirmation" label="Nhập lại mật khẩu" complete="new-password" value={confirmation} change={setConfirmation} />
        <label className="cp-checkbox"><input type="checkbox" required />Tôi xác nhận thông tin và đồng ý tạo tài khoản cá nhân.</label></fieldset>
      {(validation || action.error) && <Notice error>{validation || action.error}</Notice>}<button className="cp-primary" disabled={action.busy} type="submit">{action.busy ? 'Đang tiếp nhận…' : action.uncertain ? 'Gửi lại đúng yêu cầu vừa rồi' : 'Đăng ký ngay'}</button>
    </form>
  </Page></Guest>;
}
function EmailRequest({ kind, complete }: { kind: 'resend' | 'recover'; complete?: () => void }) {
  const navigate = useNavigate();
  const [email, setEmail] = useState(''); const action = useCommand<string>(async (value, key) => { await (kind === 'resend' ? customerClient.resend(value, key) : customerClient.recover(value, key)); complete?.(); navigate(kind === 'resend' ? '/verify-email' : '/recover/otp'); });
  return action.done ? <Notice>Đã tiếp nhận yêu cầu. Nếu email đủ điều kiện, bạn sẽ nhận được hướng dẫn tiếp theo. Vui lòng kiểm tra cả thư rác.</Notice> : <form onSubmit={event => { event.preventDefault(); void action.submit(email); }}>
    <fieldset disabled={action.busy || action.uncertain}><label htmlFor={`${kind}-email`}>Email của tài khoản</label><input id={`${kind}-email`} {...emailProps} autoComplete="email" value={email} onChange={e => setEmail(e.target.value)} /></fieldset>
    {action.error && <Notice error>{action.error}</Notice>}<button className="cp-primary" type="submit" disabled={action.busy}>{action.busy ? 'Đang tiếp nhận…' : action.uncertain ? 'Gửi lại đúng yêu cầu vừa rồi' : kind === 'resend' ? 'Yêu cầu hướng dẫn xác minh' : 'Yêu cầu khôi phục mật khẩu'}</button>
  </form>;
}
function EndSessionBeforeProof() {
  const [error, setError] = useState(''), [busy, setBusy] = useState(false);
  return <><Notice>Bạn đang đăng nhập. Hãy kết thúc phiên hiện tại trước khi xác nhận liên kết này.</Notice>{error && <Notice error>{error}</Notice>}<button className="cp-primary" disabled={busy} onClick={() => { setBusy(true); void customerClient.logout().catch(e => setError(message(e))).finally(() => setBusy(false)); }}>Đăng xuất và tiếp tục</button></>;
}
function Verify({ recovery = false, onRegistered }: { recovery?: boolean; onRegistered: () => void }) {
  const session = useSession(), navigate = useNavigate();
  const purpose = recovery ? 'recovery' : 'verification';
  const [context, setContext] = useState(() => getOtpChallenge(purpose));
  const [legacyLocator] = useState(getVerificationLocator);
  const locator = context ?? (!recovery ? legacyLocator : undefined);
  const [code, setCode] = useState(''), [now, setNow] = useState(Date.now());
  const action = useCommand<string>(async (value, key) => {
    if (recovery) { await customerClient.verifyRecovery(value, key); setCode(''); navigate('/recover/confirm'); }
    else { await customerClient.verify(key, value); setCode(''); onRegistered(); }
  });
  const resend = useCommand<string>(async (email, key) => {
    await (recovery ? customerClient.recover(email, key) : customerClient.resend(email, key));
    setContext(getOtpChallenge(purpose)); setCode(''); setNow(Date.now()); action.clearError();
  });
  useEffect(() => { if (!locator) return; const timer = window.setInterval(() => setNow(Date.now()), 1000); return () => window.clearInterval(timer); }, [locator]);
  const remaining = locator ? Math.max(0, Math.ceil((locator.expiresAt - now) / 1000)) : 0;
  const cooldown = locator ? Math.max(0, Math.ceil((locator.expiresAt - 90000 - now) / 1000)) : 0;
  const time = (value: number) => `${String(Math.floor(value / 60)).padStart(2, '0')}:${String(value % 60).padStart(2, '0')}`;
  return <Page otp title={recovery ? 'Xác minh khôi phục mật khẩu' : 'Xác minh email'}>
    {session.status === 'authenticated' ? <EndSessionBeforeProof /> : locator ? <>
      <div className="cp-otp-icon" aria-hidden="true"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.6"><rect x="3" y="5" width="18" height="14" rx="3" /><path d="m4 7 8 6 8-6" /></svg></div>
      <p className="cp-otp-intro">Nhập mã 6 số từ email{context ? <> <strong>{context.email}</strong></> : ''}<span>Nếu email đủ điều kiện, mã có hiệu lực trong 2 phút. Kiểm tra cả thư rác.</span></p>
      <form onSubmit={event => { event.preventDefault(); void action.submit(code); }}>
        <div className="cp-otp-label"><label htmlFor="verification-code">Mã xác minh 6 chữ số</label><span role="timer" aria-live="off">{remaining > 0 ? time(remaining) : 'Mã đã hết hạn'}</span></div>
        <fieldset disabled={action.busy || resend.busy || action.uncertain || remaining === 0}><input id="verification-code" className="cp-otp-input" type="text" inputMode="numeric" autoComplete="one-time-code" pattern="[0-9]{6}" maxLength={6} required placeholder="000000" value={code} onChange={event => setCode(event.target.value.replace(/\D/g, '').slice(0, 6))} /></fieldset>
        {action.error && <Notice error>{action.error}</Notice>}
        {remaining === 0 && <Notice error>Mã không còn hiệu lực. {recovery ? 'Vui lòng gửi lại mã để tiếp tục.' : 'Tài khoản chưa được tạo. Bạn có thể gửi lại mã.'}</Notice>}
        <button className="cp-primary" type="submit" disabled={action.busy || resend.busy || code.length !== 6 || remaining === 0}>{action.busy ? 'Đang xác minh…' : recovery ? 'Tiếp tục' : 'Xác minh tài khoản'}</button>
      </form>
      {context ? <><div className="cp-otp-resend"><span>Chưa nhận được mã?</span><button type="button" disabled={resend.busy || action.busy || cooldown > 0 || action.uncertain} onClick={() => { void resend.submit(context.email); }}>{resend.busy ? 'Đang tiếp nhận…' : cooldown > 0 ? `Gửi lại sau ${time(cooldown)}` : 'Gửi lại mã'}</button></div>{resend.error && <Notice error>{resend.error}</Notice>}<Link className="cp-change-email" to={recovery ? '/recover' : '/register'}>← Đổi email</Link></> : <><p className="cp-otp-resend-title">Chưa nhận được mã? Yêu cầu mã mới</p><EmailRequest kind={recovery ? 'recover' : 'resend'} complete={() => setContext(getOtpChallenge(purpose))} /></>}
    </> : !recovery && hasEntryProof('verification') ? <>{action.error && <Notice error>{action.error}</Notice>}<button className="cp-primary" disabled={action.busy} onClick={() => { void action.submit(''); }}>{action.busy ? 'Đang xác minh…' : 'Xác nhận email của tôi'}</button></> : <><Notice>Nhập email để nhận mã xác minh mới.</Notice><EmailRequest kind={recovery ? 'recover' : 'resend'} complete={() => setContext(getOtpChallenge(purpose))} /></>}
    <p className="cp-footer-link"><Link to="/login">Quay lại đăng nhập</Link></p>
  </Page>;
}
function Recover() { return <Guest><Page title="Quên mật khẩu?" description="Nhập email đã xác minh để nhận mã khôi phục mật khẩu."><EmailRequest kind="recover" /><p className="cp-footer-link"><Link to="/login">Quay lại đăng nhập</Link></p></Page></Guest>; }
function ConfirmRecovery({ onRecovered }: { onRecovered: () => void }) {
  const session = useSession(), [password, setPassword] = useState(''), [confirmation, setConfirmation] = useState(''), [validation, setValidation] = useState('');
  const action = useCommand<string>(async (value, key) => { await customerClient.confirmRecovery(value, key); setPassword(''); setConfirmation(''); onRecovered(); });
  function submit(event: FormEvent<HTMLFormElement>) { event.preventDefault(); if (!action.uncertain && !passwordValid(password)) { setValidation('Mật khẩu cần ít nhất 15 ký tự và không quá 72 byte UTF-8.'); return; } if (!action.uncertain && password !== confirmation) { setValidation('Mật khẩu nhập lại chưa khớp.'); return; } setValidation(''); void action.submit(password); }
  return <Page title="Đặt mật khẩu mới">
    {session.status === 'authenticated' ? <EndSessionBeforeProof /> : !hasEntryProof('recovery') ? <><Notice>Phiên khôi phục chưa hợp lệ hoặc không còn trên trang này.</Notice><Link to="/recover">Yêu cầu mã mới</Link></> : <form onSubmit={submit}>
      <fieldset disabled={action.busy || action.uncertain}><label htmlFor="recovery-password">Mật khẩu mới</label><input id="recovery-password" type="password" autoComplete="new-password" required maxLength={72} aria-describedby="recovery-rule" value={password} onChange={e => setPassword(e.target.value)} /><p id="recovery-rule" className="cp-hint">Ít nhất 15 ký tự, tối đa 72 byte UTF-8.</p><label htmlFor="recovery-confirmation">Nhập lại mật khẩu mới</label><input id="recovery-confirmation" type="password" autoComplete="new-password" required maxLength={72} value={confirmation} onChange={e => setConfirmation(e.target.value)} /></fieldset>
      {(validation || action.error) && <Notice error>{validation || action.error}</Notice>}<button className="cp-primary" type="submit" disabled={action.busy}>{action.busy ? 'Đang cập nhật…' : 'Xác nhận mật khẩu mới'}</button>
    </form>}
  </Page>;
}
function Personal() {
  const session = useSession(); return <Page title={`Xin chào, ${session.subject?.displayName ?? 'bạn'}`} description="Không gian gửi hàng cá nhân của bạn.">
    <div className="cp-summary"><span className="cp-avatar" aria-hidden="true">CN</span><div><strong>Tài khoản cá nhân</strong><p>Email đăng nhập: {session.subject?.verifiedEmail}</p><span className="cp-verified">Email đã xác minh</span></div></div>
    <Link className="cp-feature" to="/personal/profile"><span><strong>Hồ sơ cá nhân</strong><small>Cập nhật tên hiển thị và thông tin liên hệ dùng cho công việc.</small></span><span aria-hidden="true">→</span></Link>
    <Link className="cp-feature" to="/personal/applications"><span><strong>Đăng ký trở thành Merchant</strong><small>Chuẩn bị hồ sơ doanh nghiệp và theo dõi tiến trình xét duyệt.</small></span><span aria-hidden="true">→</span></Link>
  </Page>;
}
function MerchantEntry() {
  return <Page title="Không gian Merchant chưa khả dụng" description="Quyền truy cập tổ chức cần ACTIVE membership được backend xác nhận cho từng yêu cầu.">
    <Notice error>Hệ thống chưa cung cấp API membership và ngữ cảnh tổ chức đã được nghiệm thu. Trang này không hiển thị dữ liệu Merchant hoặc cấp quyền từ đường dẫn.</Notice>
    <Link className="cp-secondary" to="/personal/applications">Xem hồ sơ đăng ký Merchant</Link>
  </Page>;
}
function ProfileEditor({ original, saved, reload }: { original: VersionedProfile; saved: (value: VersionedProfile) => void; reload: () => void }) {
  const [name, setName] = useState(original.profile.displayName), [language, setLanguage] = useState(original.profile.preferredLanguage), [email, setEmail] = useState(original.profile.businessContactEmail ?? ''), [phone, setPhone] = useState(original.profile.businessContactPhone ?? '');
  const action = useCommand<ProfileInput>(async (input, key) => { saved(await customerClient.updateProfile(input, original.etag, key)); });
  function submit(event: FormEvent<HTMLFormElement>) { event.preventDefault(); void action.submit({ displayName: name, preferredLanguage: language, ...(email ? { businessContactEmail: email } : {}), ...(phone ? { businessContactPhone: phone } : {}) }); }
  return <form onSubmit={submit}><fieldset disabled={action.busy || action.uncertain}>
    <label htmlFor="profile-name">Tên hiển thị trong hồ sơ</label><input id="profile-name" required maxLength={160} autoComplete="name" value={name} onChange={e => setName(e.target.value)} />
    <label htmlFor="profile-language">Ngôn ngữ ưu tiên</label><select id="profile-language" value={language} onChange={e => setLanguage(e.target.value as 'vi' | 'en')}><option value="vi">Tiếng Việt</option><option value="en">Tiếng Anh</option></select>
    <label htmlFor="profile-email">Email liên hệ công việc <span className="cp-optional">(không bắt buộc)</span></label><input id="profile-email" {...emailProps} required={false} autoComplete="email" value={email} onChange={e => setEmail(e.target.value)} />
    <label htmlFor="profile-phone">Số điện thoại liên hệ <span className="cp-optional">(không bắt buộc)</span></label><input id="profile-phone" type="tel" maxLength={32} autoComplete="tel" value={phone} onChange={e => setPhone(e.target.value)} />
    <p className="cp-hint">Thông tin liên hệ công việc không thay đổi email hoặc mật khẩu đăng nhập.</p></fieldset>
    {action.error && <Notice error>{action.error}</Notice>}
    {action.uncertain ? <><Notice>Kết quả lưu chưa được xác định. Hãy kiểm tra kết quả trước khi gửi thay đổi mới.</Notice><button className="cp-primary" type="button" disabled={action.busy} onClick={() => { void action.resolve(async key => { await customerClient.profileReceipt(key); saved(await customerClient.readProfile()); }); }}>{action.busy ? 'Đang kiểm tra…' : 'Kiểm tra kết quả lưu'}</button></> : <button className="cp-primary" type="submit" disabled={action.busy}>{action.busy ? 'Đang lưu…' : 'Lưu hồ sơ'}</button>}
    {action.error && !action.uncertain && <button className="cp-secondary" type="button" disabled={action.busy} onClick={reload}>Xem dữ liệu hiện tại</button>}
  </form>;
}
function ProfilePage() {
  const [data, setData] = useState<VersionedProfile>(), [error, setError] = useState(''), [loading, setLoading] = useState(true), [revision, setRevision] = useState(0), [success, setSuccess] = useState(false);
  useEffect(() => { let active = true; setLoading(true); setError(''); setSuccess(false); customerClient.readProfile().then(value => { if (active) setData(value); }).catch(e => { if (active) setError(message(e)); }).finally(() => { if (active) setLoading(false); }); return () => { active = false; }; }, [revision]);
  return <Page title="Hồ sơ cá nhân" description="Quản lý tên và thông tin liên hệ dùng cho công việc.">{loading ? <Loading /> : error ? <><Notice error>{error}</Notice><button className="cp-secondary" onClick={() => setRevision(v => v + 1)}>Tải lại hồ sơ</button></> : data && <>{success && <Notice>Đã lưu hồ sơ.</Notice>}<ProfileEditor key={`${data.profile.id}:${data.profile.version}:${revision}`} original={data} saved={value => { setData(value); setSuccess(true); }} reload={() => setRevision(v => v + 1)} /></>}</Page>;
}
const applicationStates = { DRAFT: 'Bản nháp', SUBMITTED: 'Đã nộp', UNDER_REVIEW: 'Đang xét duyệt', NEEDS_INFORMATION: 'Cần bổ sung', APPROVED: 'Đã duyệt', REJECTED: 'Đã từ chối', WITHDRAWN: 'Đã rút' } as const;
function ApplicationList() {
  const navigate = useNavigate(), [data, setData] = useState<ApplicationPage>(), [error, setError] = useState(''), [loading, setLoading] = useState(true), [page, setPage] = useState(0), [query, setQuery] = useState({ search: '', state: '' }), [search, setSearch] = useState(''), [state, setState] = useState(''), [revision, setRevision] = useState(0);
  const action = useCommand<null>(async (_, key) => { const result = await customerClient.createApplication({}, key); navigate('/personal/applications/' + result.application.id); });
  useEffect(() => { let active = true; setLoading(true); setError(''); customerClient.listApplications(page, query.search, query.state).then(value => { if (active) setData(value); }).catch(e => { if (active) setError(message(e)); }).finally(() => { if (active) setLoading(false); }); return () => { active = false; }; }, [page, query, revision]);
  return <Page title="Hồ sơ đăng ký Merchant" description="Chuẩn bị thông tin pháp nhân trong bản nháp. Tài khoản cá nhân vẫn sử dụng bình thường trong quá trình xét duyệt.">
    {action.error && <Notice error>{action.error}</Notice>}
    {action.uncertain ? <><Notice>Chưa xác nhận được kết quả tạo hồ sơ. Hãy kiểm tra trước khi tạo yêu cầu mới.</Notice><button className="cp-primary" disabled={action.busy} onClick={() => { void action.resolve(async key => { const result = await customerClient.applicationReceipt('createApplicationDraft', key); navigate('/personal/applications/' + result.targetId); }); }}>Kiểm tra kết quả tạo hồ sơ</button></> : <button className="cp-primary" disabled={action.busy} onClick={() => { void action.submit(null); }}>{action.busy ? 'Đang tạo…' : 'Tạo bản nháp Merchant'}</button>}
    <form className="cp-search" onSubmit={event => { event.preventDefault(); setPage(0); setQuery({ search, state }); }}><label htmlFor="application-search">Tìm tên pháp nhân hoặc mã đăng ký</label><input id="application-search" maxLength={160} value={search} onChange={e => setSearch(e.target.value)} /><label htmlFor="application-state">Trạng thái</label><select id="application-state" value={state} onChange={e => setState(e.target.value)}><option value="">Tất cả trạng thái</option>{Object.entries(applicationStates).map(([code, label]) => <option key={code} value={code}>{label}</option>)}</select><button className="cp-secondary" type="submit">Tìm hồ sơ</button></form>
    {loading ? <Loading /> : error ? <><Notice error>{error}</Notice><button className="cp-secondary" onClick={() => setRevision(v => v + 1)}>Tải lại danh sách</button></> : data && <>{data.items.length === 0 ? <Notice>Chưa có hồ sơ phù hợp. Bạn có thể tạo bản nháp mới.</Notice> : <ul className="cp-applications">{data.items.map(item => <li key={item.id}><Link className="cp-feature" to={'/personal/applications/' + item.id}><span><strong>{item.draft?.registeredName ?? item.submission?.registeredName ?? 'Hồ sơ chưa đặt tên'}</strong><small>{applicationStates[item.state]} · phiên bản {item.version}</small></span><span aria-hidden="true">→</span></Link></li>)}</ul>}
      <nav aria-label="Phân trang hồ sơ"><button className="cp-secondary" disabled={page === 0} onClick={() => setPage(v => v - 1)}>Trang trước</button><span aria-live="polite"> Trang {page + 1} </span><button className="cp-secondary" disabled={!data.hasNext} onClick={() => setPage(v => v + 1)}>Trang sau</button></nav></>}
  </Page>;
}
function DraftAddress({ name, title, value, change }: { name: string; title: string; value?: ApplicationAddress; change: (address: ApplicationAddress | undefined) => void }) {
  return <div className="cp-address"><label className="cp-checkbox"><input type="checkbox" checked={!!value} onChange={e => change(e.target.checked ? { country: 'VN', administrativeArea: '', locality: '', addressLine: '' } : undefined)} />Bổ sung {title.toLowerCase()}</label>{value && <fieldset><legend>{title}</legend>{([['country', 'Mã quốc gia', 2], ['administrativeArea', 'Tỉnh / thành phố', 160], ['locality', 'Quận / huyện / địa phương', 160], ['addressLine', 'Địa chỉ chi tiết', 500]] as const).map(([key, label, max]) => <div key={key}><label htmlFor={`${name}-${key}`}>{label}</label><input id={`${name}-${key}`} required maxLength={max} pattern={key === 'country' ? '[A-Z]{2}' : undefined} value={value[key]} onChange={e => change({ ...value, [key]: e.target.value })} /></div>)}</fieldset>}</div>;
}
function ApplicationEditor({ original, saved, reload }: { original: VersionedApplication; saved: (value: VersionedApplication) => void; reload: () => void }) {
  const [input, setInput] = useState<ApplicationDraft>(original.application.draft ?? {});
  const action = useCommand<ApplicationDraft>(async (value, key) => { saved(await customerClient.updateApplication(original.application.id, value, original.etag, key)); });
  const dirty = JSON.stringify(input) !== JSON.stringify(original.application.draft ?? {});
  useEffect(() => { if (!dirty) return; const warn = (event: BeforeUnloadEvent) => { event.preventDefault(); }; window.addEventListener('beforeunload', warn); return () => window.removeEventListener('beforeunload', warn); }, [dirty]);
  function field<K extends keyof ApplicationDraft>(key: K, value: ApplicationDraft[K]) { setInput(previous => { const next = { ...previous }; if (value === undefined || value === '') delete next[key]; else next[key] = value; return next; }); }
  return <form onSubmit={event => { event.preventDefault(); void action.submit(input); }}><fieldset disabled={action.busy || action.uncertain}>
    <label htmlFor="application-entity">Loại hình pháp nhân</label><select id="application-entity" value={input.entityType ?? ''} onChange={e => field('entityType', e.target.value ? e.target.value as ApplicationDraft['entityType'] : undefined)}><option value="">Chọn khi đã có thông tin</option><option value="COMPANY">Doanh nghiệp</option><option value="HOUSEHOLD_BUSINESS">Hộ kinh doanh</option></select>
    {([['registeredName', 'Tên pháp nhân đăng ký', 240], ['jurisdiction', 'Nơi cấp đăng ký', 64], ['legalIdentifier', 'Mã đăng ký / mã số thuế', 80], ['representativeName', 'Họ tên người đại diện', 160], ['representativeEmail', 'Email người đại diện', 254], ['representativeRelationship', 'Quan hệ / thẩm quyền đại diện', 160]] as const).map(([key, label, max]) => <div key={key}><label htmlFor={'application-' + key}>{label}</label><input id={'application-' + key} type={key === 'representativeEmail' ? 'email' : 'text'} maxLength={max} value={input[key] ?? ''} onChange={e => field(key, e.target.value)} /></div>)}
    <DraftAddress name="registered" title="Địa chỉ đăng ký" value={input.registeredAddress} change={value => field('registeredAddress', value)} /><DraftAddress name="operating" title="Địa chỉ hoạt động" value={input.operatingAddress} change={value => field('operatingAddress', value)} />
    <p className="cp-hint">Bạn có thể lưu khi chưa nhập đủ thông tin. Bản nháp chưa được nộp xét duyệt.</p></fieldset>
    {action.error && <Notice error>{action.error}</Notice>}
    {action.uncertain ? <><Notice>Kết quả lưu chưa xác định. Nội dung được giữ trên trang này để bạn kiểm tra.</Notice><button className="cp-primary" type="button" disabled={action.busy} onClick={() => { void action.resolve(async key => { const receipt = await customerClient.applicationReceipt('updateApplicationDraft', key); if (receipt.targetId !== original.application.id) throw new CustomerApiError('RESPONSE_INVALID'); saved(await customerClient.readApplication(receipt.targetId)); }); }}>Kiểm tra kết quả lưu bản nháp</button></> : <button className="cp-primary" type="submit" disabled={action.busy}>{action.busy ? 'Đang lưu…' : 'Lưu bản nháp'}</button>}
    {action.error && !action.uncertain && <button className="cp-secondary" type="button" onClick={() => { if (!dirty || window.confirm('Tải lại sẽ thay nội dung chưa lưu bằng dữ liệu hiện tại. Tiếp tục?')) reload(); }}>Xem bản hiện tại</button>}
  </form>;
}
function ApplicationDetail() {
  const { id = '' } = useParams(), [data, setData] = useState<VersionedApplication>(), [error, setError] = useState(''), [loading, setLoading] = useState(true), [revision, setRevision] = useState(0), [success, setSuccess] = useState(false);
  useEffect(() => { let active = true; setLoading(true); setError(''); setSuccess(false); customerClient.readApplication(id).then(value => { if (active) setData(value); }).catch(e => { if (active) setError(message(e)); }).finally(() => { if (active) setLoading(false); }); return () => { active = false; }; }, [id, revision]);
  return <Page title="Chi tiết hồ sơ Merchant"><Link to="/personal/applications">← Danh sách hồ sơ</Link>{loading ? <Loading /> : error ? <><Notice error>{error}</Notice><button className="cp-secondary" onClick={() => setRevision(v => v + 1)}>Tải lại hồ sơ Merchant</button></> : data && <><p>Trạng thái: <strong>{applicationStates[data.application.state]}</strong> · phiên bản {data.application.version}</p>{success && <Notice>Đã lưu bản nháp.</Notice>}{data.application.informationRequest && <Notice>{data.application.informationRequest}</Notice>}{data.application.state === 'DRAFT' ? <ApplicationEditor key={`${id}:${data.application.version}:${revision}`} original={data} saved={value => { setData(value); setSuccess(true); }} reload={() => setRevision(v => v + 1)} /> : <Notice>Hồ sơ đã được ghi nhận. Thông tin đã nộp được lưu theo từng lần xét duyệt.</Notice>}</>}</Page>;
}
export default function CustomerApp() {
  const session = useSession(), location = useLocation(), navigate = useNavigate();
  const [success, setSuccess] = useState<{ kind: AuthSuccess; id: string }>();
  useEffect(() => { if (!success) return; const timer = window.setTimeout(() => setSuccess(undefined), 10000); return () => window.clearTimeout(timer); }, [success]);
  function complete(kind: AuthSuccess) { setSuccess({ kind, id: crypto.randomUUID() }); navigate('/login', { replace: true }); }
  useEffect(() => { if (!['/verify-email', '/recover/otp', '/recover/confirm'].includes(location.pathname)) clearEntryProof(); document.title = 'movapost. | Gửi niềm tin, nối hành trình'; window.scrollTo(0, 0); document.querySelector<HTMLElement>('main h1')?.focus(); }, [location.pathname]);
  const accountRoute = /^\/(login|register|verify-email|recover|personal|merchant)(\/|$)/.test(location.pathname);
  const authRoute = /^\/(login|register|verify-email|recover)(\/|$)/.test(location.pathname);
  if (!accountRoute) return <PublicWebsite />;
  return <div className={`customer-portal ${authRoute ? 'cp-auth-portal' : 'cp-workspace-portal'}`}><a className="cp-skip" href="#main-content">Đến nội dung chính</a>{success && <SuccessPopup key={success.id} kind={success.kind} dismiss={() => setSuccess(undefined)} />}{authRoute ? <PublicHeader compact /> : <header className="cp-header"><PublicBrand /><nav aria-label="Điều hướng tài khoản">
    {session.status === 'authenticated' ? <><NavLink to="/personal" end>Cá nhân</NavLink><NavLink to="/personal/profile">Hồ sơ</NavLink><NavLink to="/personal/applications">Merchant</NavLink><button className="cp-logout" onClick={() => { void customerClient.logout().catch(() => {}); }}>Đăng xuất</button></> : <><NavLink to="/login">Đăng nhập</NavLink><NavLink to="/register">Đăng ký</NavLink></>}
  </nav></header>}<main id="main-content" key={location.pathname}>{session.status === 'loading' ? <Loading /> : <Routes>
    <Route path="/login" element={<Login />} /><Route path="/register" element={<Register />} /><Route path="/verify-email" element={<Verify onRegistered={() => complete('registered')} />} /><Route path="/recover" element={<Recover />} /><Route path="/recover/otp" element={<Verify recovery onRegistered={() => complete('registered')} />} /><Route path="/recover/confirm" element={<ConfirmRecovery onRecovered={() => complete('password-reset')} />} />
    <Route element={<Protected />}><Route path="/personal" element={<Personal />} /><Route path="/personal/profile" element={<ProfilePage />} /><Route path="/personal/applications" element={<ApplicationList />} /><Route path="/personal/applications/:id" element={<ApplicationDetail />} /><Route path="/merchant" element={<MerchantEntry />} /></Route>
    <Route path="*" element={<Page title="Không tìm thấy trang"><Link to="/">Về trang chủ</Link></Page>} />
  </Routes>}</main>{authRoute ? <PublicFooter /> : <footer className="cp-footer">movapost. · Gửi niềm tin, nối hành trình.</footer>}</div>;
}
