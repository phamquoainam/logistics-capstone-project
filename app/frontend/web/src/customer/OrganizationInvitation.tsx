import { useRef, useState } from 'react';
import { Link, useLocation } from 'react-router-dom';
import { customerClient, CustomerApiError } from './client';
import type { Subject } from './client';
import type { TeamDecision } from './organization-team-data';
import { clearTeamInvitation, hasTeamInvitation } from './team-invitation-proof';

/** Recipient decisions do not require existing Organization membership. */
export function OrganizationInvitation({ subject, loading }: { subject: Subject | null; loading: boolean }) {
  const location = useLocation(), id = location.pathname.slice('/organization-invitations/'.length);
  const pending = useRef<{ key: string; accept: boolean; actor: string } | undefined>(undefined), running = useRef(false);
  const [busy, setBusy] = useState(false), [error, setError] = useState(''), [uncertain, setUncertain] = useState(false), [done, setDone] = useState<TeamDecision>();
  async function decide(accept: boolean) {
    if (!subject || running.current || done || !hasTeamInvitation(id)) return;
    const command = pending.current ?? { key: crypto.randomUUID(), accept, actor: subject.accountId };
    if (command.actor !== subject.accountId) { setError('Hãy đăng nhập lại đúng tài khoản đã gửi yêu cầu trước đó để kiểm tra kết quả.'); return; }
    pending.current = command; running.current = true; setBusy(true); setError('');
    try {
      const receipt = await customerClient.decideTeamInvitation(id, command.accept, command.key);
      clearTeamInvitation(); pending.current = undefined; setDone(receipt); setUncertain(false);
    } catch (failure) {
      const unknown = failure instanceof CustomerApiError && ['OUTCOME_UNKNOWN', 'RESPONSE_INVALID', 'SERVICE_UNAVAILABLE', 'NETWORK_UNAVAILABLE'].includes(failure.code);
      setUncertain(unknown);
      if (!unknown) pending.current = undefined;
      const code = failure instanceof CustomerApiError ? failure.code : '';
      setError(unknown ? 'Chưa xác định kết quả. Gửi lại đúng yêu cầu để nhận kết quả cũ; hệ thống không tạo Membership trùng.' : code === 'TEAM_INVITATION_INVALID_OR_EXPIRED' ? 'Lời mời không còn hiệu lực hoặc email tài khoản chưa đúng. Vui lòng kiểm tra tài khoản nhận thư hoặc liên hệ người mời.' : code === 'MEMBERSHIP_ALREADY_EXISTS' ? 'Tài khoản đã có Membership tại doanh nghiệp này. Không tạo Membership mới hoặc khôi phục quyền đã thu hồi.' : 'Không thể xử lý lời mời. Kiểm tra phiên đăng nhập hoặc liên hệ người mời.');
    } finally { running.current = false; setBusy(false); }
  }
  return <section className="cp-page" aria-label="Lời mời doanh nghiệp"><p className="cp-eyebrow">ARGOPOST BUSINESS</p><h1 tabIndex={-1}>Lời mời tham gia doanh nghiệp</h1>
    {loading ? <p role="status">Đang kiểm tra phiên đăng nhập…</p> : done ? <><div className="cp-notice" role="status">{done.outcome === 'ACCEPTED' ? 'Đã chấp nhận lời mời. Doanh nghiệp sẽ xuất hiện trong danh sách Workspace theo quyền được cấp.' : 'Đã từ chối lời mời. Tài khoản của bạn không được cấp thêm quyền.'}</div><Link className="cp-primary" to="/personal">Về không gian cá nhân</Link></> : !hasTeamInvitation(id) ? <><p role="alert">Chưa có liên kết lời mời hợp lệ trong phiên này. Hãy mở lại email mới nhất; không sao chép mã vào địa chỉ trang.</p><Link to="/">Về trang chủ</Link></> : !subject ? <><p>Đăng nhập bằng tài khoản đã xác minh chính địa chỉ email nhận lời mời. Nếu chưa có tài khoản, hãy đăng ký và xác minh email trước.</p><Link className="cp-primary" to="/login">Đăng nhập để tiếp tục</Link><p><Link to="/register">Tạo tài khoản cá nhân</Link></p></> : <>
      <p>Bạn đang đăng nhập bằng <strong>{subject.verifiedEmail}</strong>.</p><p>Chỉ chấp nhận khi bạn mong đợi lời mời này. Membership chỉ có quyền doanh nghiệp được cấp, không thay đổi tài khoản cá nhân.</p>
      {error && <div className="cp-notice cp-error" role="alert">{error}</div>}
      {uncertain ? <button type="button" className="cp-primary" disabled={busy} onClick={() => { void decide(pending.current?.accept ?? true); }}>{busy ? 'Đang kiểm tra…' : 'Kiểm tra lại đúng yêu cầu'}</button> : <div className="cp-form-links"><button className="cp-primary" type="button" disabled={busy} onClick={() => { void decide(true); }}>{busy ? 'Đang xử lý…' : 'Chấp nhận lời mời'}</button><button className="cp-secondary" type="button" disabled={busy} onClick={() => { void decide(false); }}>Từ chối</button></div>}
      <p className="cp-hint">Lời mời có hiệu lực 72 giờ, chỉ sử dụng một lần. Quyền được kiểm tra lại khi xác nhận.</p>
    </>}
  </section>;
}
