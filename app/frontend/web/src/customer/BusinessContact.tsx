import { useRef, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { customerClient, CustomerApiError } from './client';
import type { ApplicationDraft, VersionedApplication } from './client';
import { clearBusinessContactProof, hasBusinessContactProof, withBusinessContactProof } from './business-contact-proof';

function errorText(error: unknown) {
  if (error instanceof CustomerApiError) {
    if (error.code === 'TOKEN_INVALID_OR_EXPIRED' || error.code === 'CONTACT_PROOF_REQUIRED') return 'Liên kết đã hết hạn hoặc không còn hiệu lực. Mở hồ sơ và gửi lại email xác minh.';
    if (error.code === 'PRECONDITION_STALE') return 'Hồ sơ đã thay đổi. Tải lại hồ sơ trước khi tiếp tục.';
    if (error.code === 'RESOURCE_NOT_FOUND') return 'Hồ sơ không thuộc tài khoản đang đăng nhập.';
    if (error.code === 'SESSION_INVALID' || error.code === 'SESSION_CHANGED') return 'Phiên đăng nhập đã thay đổi. Đăng nhập đúng tài khoản đã yêu cầu xác minh.';
    if (error.status === 429) return 'Bạn đã gửi nhiều yêu cầu. Vui lòng chờ trước khi gửi lại.';
  }
  return 'Chưa thể xác nhận kết quả. Bạn có thể thử lại đúng yêu cầu vừa gửi.';
}
export function BusinessContactRequest({ application }: { application: VersionedApplication }) {
  const [busy, setBusy] = useState(false), [sent, setSent] = useState(false), [error, setError] = useState('');
  const pending = useRef<string | undefined>(undefined), running = useRef(false);
  const email = application.application.draft?.representativeEmail;
  return <section className="cp-notice"><h3>Email liên hệ doanh nghiệp</h3><p>{email ? <>Email đã lưu trong hồ sơ: <strong>{email}</strong>. Mở liên kết xác minh trong cùng phiên trình duyệt. Liên kết có hiệu lực 15 phút.</> : 'Lưu email người đại diện trong bản nháp trước khi yêu cầu xác minh.'}</p>
    {sent && <p role="status">Đã tiếp nhận yêu cầu gửi email. Xác minh email không đồng nghĩa hồ sơ đã được nộp hoặc phê duyệt.</p>}{error && <p role="alert">{error}</p>}
    <button className="cp-secondary" type="button" disabled={!email || busy} onClick={() => {
      if (running.current || !email) return; running.current = true; setBusy(true); setError(''); pending.current ??= crypto.randomUUID();
      void customerClient.requestBusinessContact(application.application.id, email, pending.current).then(() => { pending.current = undefined; setSent(true); }).catch(e => { if (!(e instanceof CustomerApiError) || (e.code !== 'OUTCOME_UNKNOWN' && e.status !== 503)) pending.current = undefined; setError(errorText(e)); }).finally(() => { running.current = false; setBusy(false); });
    }}>{busy ? 'Đang gửi…' : sent ? 'Gửi lại email xác minh' : 'Gửi email xác minh liên hệ'}</button>
  </section>;
}
export function BusinessContactConfirmation() {
  const [available, setAvailable] = useState(hasBusinessContactProof), [busy, setBusy] = useState(false), [error, setError] = useState('');
  const pending = useRef<{ key: string; update?: { id: string; etag: string; input: ApplicationDraft } } | undefined>(undefined), running = useRef(false), navigate = useNavigate();
  if (!available) return null;
  return <section className="cp-notice"><h2>Xác minh email liên hệ doanh nghiệp</h2><p>Chỉ xác nhận nếu bạn đã yêu cầu xác minh email cho hồ sơ Merchant này.</p>{error && <p role="alert">{error}</p>}
    <button className="cp-primary" disabled={busy} onClick={() => {
      if (running.current) return; running.current = true; setBusy(true); setError(''); pending.current ??= { key: crypto.randomUUID() };
      void withBusinessContactProof(async proof => {
        const fact = await customerClient.confirmBusinessContact(proof.applicationId, proof.proofId, proof.proof, pending.current!.key);
        if (!pending.current!.update) {
          const current = await customerClient.readApplication(proof.applicationId);
          if (!['DRAFT', 'NEEDS_INFORMATION'].includes(current.application.state)) throw new CustomerApiError('STATE_CONFLICT');
          pending.current!.update = { id: proof.applicationId, input: { ...current.application.draft, businessContactAttestationId: fact.id }, etag: current.etag };
        }
        const update = pending.current!.update!;
        await customerClient.updateApplication(update.id, update.input, update.etag, pending.current!.key);
        clearBusinessContactProof(); setAvailable(false); navigate('/personal/applications/' + proof.applicationId, { replace: true });
      }).catch(e => { if (!(e instanceof CustomerApiError) || (e.code !== 'OUTCOME_UNKNOWN' && e.status !== 503)) pending.current = undefined; setError(errorText(e)); }).finally(() => { running.current = false; setBusy(false); });
    }}>{busy ? 'Đang xác minh…' : 'Xác nhận email liên hệ'}</button>{' '}
    <button className="cp-secondary" disabled={busy} onClick={() => { clearBusinessContactProof(); pending.current = undefined; setAvailable(false); }}>Bỏ qua liên kết</button>
  </section>;
}
