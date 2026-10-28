import { useEffect, useRef, useState, useSyncExternalStore } from 'react';
import type { FormEvent, ReactNode } from 'react';
import { Link, NavLink, Navigate, Outlet, Route, Routes, useLocation } from 'react-router-dom';
import { customerClient, CustomerApiError } from './client';
import type { Enrollment, ProfileInput, VersionedProfile } from './client';
import { clearEntryProof, hasEntryProof } from './entry-proof';
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
    TOKEN_INVALID_OR_EXPIRED: 'Liên kết không còn hiệu lực hoặc đã được sử dụng. Vui lòng yêu cầu hướng dẫn mới.',
    VALIDATION_FAILED: 'Thông tin chưa hợp lệ. Vui lòng kiểm tra các trường đã nhập.',
    PRECONDITION_STALE: 'Hồ sơ đã thay đổi. Hãy tải bản mới nhất trước khi chỉnh sửa tiếp.',
    IDEMPOTENCY_CONFLICT: 'Nội dung khác với yêu cầu trước đó. Vui lòng kiểm tra kết quả của yêu cầu cũ.',
    RESPONSE_INVALID: 'Không xác thực được phản hồi. Vui lòng thử lại sau.',
    UNSUPPORTED_BROWSER: 'Vui lòng dùng trình duyệt hỗ trợ phiên đăng nhập an toàn, như phiên bản Chrome hoặc Edge hiện hành.',
    ENTRY_PROOF_UNAVAILABLE: 'Liên kết xác nhận chưa hợp lệ. Vui lòng mở lại liên kết trong email.',
  };
  if (error.status === 429) return `Bạn đã gửi quá nhiều yêu cầu. ${error.retryAfter ? `Thử lại sau ${error.retryAfter} giây.` : 'Vui lòng chờ rồi thử lại.'}`;
  return messages[error.code] ?? 'Không thể hoàn tất yêu cầu. Vui lòng kiểm tra thông tin và thử lại sau.';
}
function Notice({ children, error = false }: { children: ReactNode; error?: boolean }) { return <div className={`cp-notice ${error ? 'cp-error' : ''}`} role={error ? 'alert' : 'status'}>{children}</div>; }
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
  return { submit, resolve, busy, error, uncertain, done };
}
function passwordValid(value: string) { return [...value].length >= 15 && new TextEncoder().encode(value).length <= 72; }
const emailProps = { type: 'text', inputMode: 'email', autoCapitalize: 'none', autoCorrect: 'off', spellCheck: false, maxLength: 254, pattern: '[^\\s@]+@[^\\s@]+', required: true } as const;
function Page({ title, description, children }: { title: string; description?: string; children: ReactNode }) { return <section className="cp-page"><p className="cp-eyebrow">E-LOGISTICS · TÀI KHOẢN CÁ NHÂN</p><h1 tabIndex={-1}>{title}</h1>{description && <p className="cp-description">{description}</p>}{children}</section>; }
function Guest({ children }: { children: ReactNode }) { const session = useSession(); return session.status === 'authenticated' ? <Navigate to="/personal" replace /> : children; }
function Protected() { const session = useSession(); return session.status === 'authenticated' ? <Outlet /> : <Navigate to="/login" replace />; }
function Login() {
  const session = useSession(), [email, setEmail] = useState(''), [password, setPassword] = useState('');
  const action = useCommand<{ email: string; password: string }>(async input => { try { await customerClient.login(input.email, input.password); } finally { setPassword(''); } });
  if (session.status === 'authenticated') return <Navigate to="/personal" replace />;
  return <Page title="Chào mừng trở lại" description="Đăng nhập để sử dụng không gian gửi hàng cá nhân của bạn.">
    {session.error && !action.error && session.error !== 'SESSION_INVALID' && <Notice error>{message(new CustomerApiError(session.error))}</Notice>}
    <form onSubmit={event => { event.preventDefault(); void action.submit({ email, password }); }}>
      <fieldset disabled={action.busy || action.uncertain}><label htmlFor="login-email">Email đã xác minh</label><input id="login-email" {...emailProps} autoComplete="username" value={email} onChange={e => setEmail(e.target.value)} />
        <label htmlFor="login-password">Mật khẩu</label><input id="login-password" type="password" autoComplete="current-password" required maxLength={256} value={password} onChange={e => setPassword(e.target.value)} /></fieldset>
      {action.error && <Notice error>{action.error}</Notice>}
      <button className="cp-primary" type="submit" disabled={action.busy}>{action.busy ? 'Đang đăng nhập…' : action.uncertain ? 'Đăng nhập lại với thông tin vừa gửi' : 'Đăng nhập'}</button>
    </form><div className="cp-form-links"><Link to="/recover">Quên mật khẩu?</Link><Link to="/register">Tạo tài khoản cá nhân</Link></div>
  </Page>;
}
function Register() {
  const [email, setEmail] = useState(''), [name, setName] = useState(''), [password, setPassword] = useState(''), [validation, setValidation] = useState('');
  const action = useCommand<Enrollment>(async (input, key) => { await customerClient.enroll(input, key); setPassword(''); });
  function submit(event: FormEvent<HTMLFormElement>) { event.preventDefault(); if (!passwordValid(password) && !action.uncertain) { setValidation('Mật khẩu cần ít nhất 15 ký tự và không quá 72 byte UTF-8.'); return; } setValidation(''); void action.submit({ email, displayName: name, password, consentVersion: 'S003-2026-10-27' }); }
  return <Guest><Page title="Tạo tài khoản cá nhân" description="Bắt đầu với tài khoản riêng của bạn. Bạn có thể đăng ký trở thành Merchant sau khi đăng nhập.">
    {action.done ? <><Notice>Đã tiếp nhận yêu cầu. Nếu email đủ điều kiện, bạn sẽ nhận được hướng dẫn xác minh. Vui lòng kiểm tra cả thư rác.</Notice><Link className="cp-primary cp-button" to="/verify-email">Tôi cần xác minh email</Link></> : <form onSubmit={submit}>
      <fieldset disabled={action.busy || action.uncertain}><label htmlFor="register-name">Tên hiển thị</label><input id="register-name" autoComplete="name" required maxLength={160} value={name} onChange={e => setName(e.target.value)} />
        <label htmlFor="register-email">Email</label><input id="register-email" {...emailProps} autoComplete="email" value={email} onChange={e => setEmail(e.target.value)} />
        <label htmlFor="register-password">Mật khẩu</label><input id="register-password" type="password" autoComplete="new-password" required maxLength={72} aria-describedby="password-rule" value={password} onChange={e => setPassword(e.target.value)} /><p id="password-rule" className="cp-hint">Ít nhất 15 ký tự, tối đa 72 byte UTF-8. Giữ nguyên dấu cách và chữ hoa khi đăng nhập.</p>
        <label className="cp-checkbox"><input type="checkbox" required />Tôi xác nhận thông tin và đồng ý tạo tài khoản cá nhân.</label></fieldset>
      {(validation || action.error) && <Notice error>{validation || action.error}</Notice>}<button className="cp-primary" disabled={action.busy} type="submit">{action.busy ? 'Đang tiếp nhận…' : action.uncertain ? 'Gửi lại đúng yêu cầu vừa rồi' : 'Tạo tài khoản'}</button>
    </form>}<p className="cp-footer-link">Đã có tài khoản? <Link to="/login">Đăng nhập</Link></p>
  </Page></Guest>;
}
function EmailRequest({ kind }: { kind: 'resend' | 'recover' }) {
  const [email, setEmail] = useState(''); const action = useCommand<string>(async (value, key) => { await (kind === 'resend' ? customerClient.resend(value, key) : customerClient.recover(value, key)); });
  return action.done ? <Notice>Đã tiếp nhận yêu cầu. Nếu email đủ điều kiện, bạn sẽ nhận được hướng dẫn tiếp theo. Vui lòng kiểm tra cả thư rác.</Notice> : <form onSubmit={event => { event.preventDefault(); void action.submit(email); }}>
    <fieldset disabled={action.busy || action.uncertain}><label htmlFor={`${kind}-email`}>Email của tài khoản</label><input id={`${kind}-email`} {...emailProps} autoComplete="email" value={email} onChange={e => setEmail(e.target.value)} /></fieldset>
    {action.error && <Notice error>{action.error}</Notice>}<button className="cp-primary" type="submit" disabled={action.busy}>{action.busy ? 'Đang tiếp nhận…' : action.uncertain ? 'Gửi lại đúng yêu cầu vừa rồi' : kind === 'resend' ? 'Yêu cầu hướng dẫn xác minh' : 'Yêu cầu khôi phục mật khẩu'}</button>
  </form>;
}
function EndSessionBeforeProof() {
  const [error, setError] = useState(''), [busy, setBusy] = useState(false);
  return <><Notice>Bạn đang đăng nhập. Hãy kết thúc phiên hiện tại trước khi xác nhận liên kết này.</Notice>{error && <Notice error>{error}</Notice>}<button className="cp-primary" disabled={busy} onClick={() => { setBusy(true); void customerClient.logout().catch(e => setError(message(e))).finally(() => setBusy(false)); }}>Đăng xuất và tiếp tục</button></>;
}
function Verify() {
  const session = useSession(); const action = useCommand<null>(async (_input, key) => { await customerClient.verify(key); });
  return <Page title="Xác minh email" description="Chỉ xác nhận khi bạn đã yêu cầu tạo tài khoản này.">
    {session.status === 'authenticated' ? <EndSessionBeforeProof /> : action.done ? <><Notice>Email đã được xác minh. Bạn có thể đăng nhập bằng thông tin đã đăng ký.</Notice><Link className="cp-primary cp-button" to="/login">Đăng nhập</Link></> : hasEntryProof('verification') ? <>
      {action.error && <Notice error>{action.error}</Notice>}<button className="cp-primary" disabled={action.busy} onClick={() => { void action.submit(null); }}>{action.busy ? 'Đang xác minh…' : 'Xác nhận email của tôi'}</button>
    </> : <><Notice>Mở liên kết xác minh trong email để tiếp tục. Nếu chưa nhận được hướng dẫn hoặc liên kết đã hết hạn, bạn có thể yêu cầu lại.</Notice><EmailRequest kind="resend" /></>}
    <p className="cp-footer-link"><Link to="/login">Quay lại đăng nhập</Link></p>
  </Page>;
}
function Recover() { return <Guest><Page title="Khôi phục mật khẩu" description="Nhập email đã xác minh của bạn. Hướng dẫn sẽ được chuyển tới địa chỉ đủ điều kiện."><EmailRequest kind="recover" /><p className="cp-footer-link"><Link to="/login">Quay lại đăng nhập</Link></p></Page></Guest>; }
function ConfirmRecovery() {
  const session = useSession(), [password, setPassword] = useState(''), [validation, setValidation] = useState('');
  const action = useCommand<string>(async (value, key) => { await customerClient.confirmRecovery(value, key); setPassword(''); });
  function submit(event: FormEvent<HTMLFormElement>) { event.preventDefault(); if (!action.uncertain && !passwordValid(password)) { setValidation('Mật khẩu cần ít nhất 15 ký tự và không quá 72 byte UTF-8.'); return; } setValidation(''); void action.submit(password); }
  return <Page title="Đặt mật khẩu mới">
    {session.status === 'authenticated' ? <EndSessionBeforeProof /> : action.done ? <><Notice>Mật khẩu đã được cập nhật. Vui lòng đăng nhập lại.</Notice><Link className="cp-primary cp-button" to="/login">Đăng nhập</Link></> : !hasEntryProof('recovery') ? <><Notice>Liên kết khôi phục chưa hợp lệ hoặc không còn trên trang này.</Notice><Link to="/recover">Yêu cầu hướng dẫn mới</Link></> : <form onSubmit={submit}>
      <fieldset disabled={action.busy || action.uncertain}><label htmlFor="recovery-password">Mật khẩu mới</label><input id="recovery-password" type="password" autoComplete="new-password" required maxLength={72} aria-describedby="recovery-rule" value={password} onChange={e => setPassword(e.target.value)} /><p id="recovery-rule" className="cp-hint">Ít nhất 15 ký tự, tối đa 72 byte UTF-8.</p></fieldset>
      {(validation || action.error) && <Notice error>{validation || action.error}</Notice>}<button className="cp-primary" type="submit" disabled={action.busy}>{action.busy ? 'Đang cập nhật…' : 'Xác nhận mật khẩu mới'}</button>
    </form>}
  </Page>;
}
function Personal() {
  const session = useSession(); return <Page title={`Xin chào, ${session.subject?.displayName ?? 'bạn'}`} description="Không gian gửi hàng cá nhân của bạn.">
    <div className="cp-summary"><span className="cp-avatar" aria-hidden="true">CN</span><div><strong>Tài khoản cá nhân</strong><p>Email đăng nhập: {session.subject?.verifiedEmail}</p><span className="cp-verified">Email đã xác minh</span></div></div>
    <Link className="cp-feature" to="/personal/profile"><span><strong>Hồ sơ cá nhân</strong><small>Cập nhật tên hiển thị và thông tin liên hệ dùng cho công việc.</small></span><span aria-hidden="true">→</span></Link>
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
export default function CustomerApp() {
  const session = useSession(), location = useLocation();
  useEffect(() => { if (!['/verify-email', '/recover/confirm'].includes(location.pathname)) clearEntryProof(); document.querySelector<HTMLElement>('main h1')?.focus(); }, [location.pathname]);
  return <div className="customer-portal"><a className="cp-skip" href="#main-content">Đến nội dung chính</a><header className="cp-header"><Link className="cp-brand" to="/">E<span>↗</span> Logistics</Link><nav aria-label="Điều hướng tài khoản">
    {session.status === 'authenticated' ? <><NavLink to="/personal" end>Cá nhân</NavLink><NavLink to="/personal/profile">Hồ sơ</NavLink><button className="cp-logout" onClick={() => { void customerClient.logout().catch(() => {}); }}>Đăng xuất</button></> : <><NavLink to="/login">Đăng nhập</NavLink><NavLink to="/register">Đăng ký</NavLink></>}
  </nav></header><main id="main-content">{session.status === 'loading' ? <Loading /> : <Routes>
    <Route path="/" element={<Navigate to={session.status === 'authenticated' ? '/personal' : '/login'} replace />} /><Route path="/login" element={<Login />} /><Route path="/register" element={<Register />} /><Route path="/verify-email" element={<Verify />} /><Route path="/recover" element={<Recover />} /><Route path="/recover/confirm" element={<ConfirmRecovery />} />
    <Route element={<Protected />}><Route path="/personal" element={<Personal />} /><Route path="/personal/profile" element={<ProfilePage />} /></Route>
    <Route path="*" element={<Page title="Không tìm thấy trang"><Link to="/">Về tài khoản cá nhân</Link></Page>} />
  </Routes>}</main><footer className="cp-footer">E-Logistics · Kết nối hành trình của bạn</footer></div>;
}
