import { useRef, useState } from 'react';
import type { BusinessContext } from './business-data';
import type { TeamMemberDetail, TeamGrant } from './organization-team-data';
import { customerClient, CustomerApiError } from './client';
import { teamCapabilities } from './organization-team';
import { missingTeamGrantReads } from './organization-team-data';
type Operation = 'access' | 'suspend' | 'resume' | 'revoke';
const title: Record<Operation, string> = { access: 'Cập nhật quyền', suspend: 'Tạm khóa Membership', resume: 'Mở lại Membership', revoke: 'Thu hồi Membership' };
type Command = { operation: Operation; role: 'MANAGER' | 'OPERATOR'; grants: TeamGrant[]; reason: string; password: string; proofId?: string; reauthKey: string; key: string };
const grantKey = (g: TeamGrant) => JSON.stringify([g.permission, g.scope, g.facilityId]);
export function OrganizationTeamMemberActions({ context, original, saved }: { context: BusinessContext; original: TeamMemberDetail; saved: () => void }) {
  const [operation, setOperation] = useState<Operation>(), [role, setRole] = useState<'MANAGER' | 'OPERATOR'>(original.role === 'MANAGER' ? 'MANAGER' : 'OPERATOR');
  const [grants, setGrants] = useState(original.grants), [reason, setReason] = useState(''), [password, setPassword] = useState(''), [busy, setBusy] = useState(false), [uncertain, setUncertain] = useState(false), [error, setError] = useState('');
  const pending = useRef<Command | undefined>(undefined), running = useRef(false);
  const options = [...new Map([...original.grants, ...context.permissions.filter(p => !p.startsWith('team.') && !p.startsWith('support.')).map(permission => ({ permission, scope: 'ORGANIZATION' as const, facilityId: null }))].map(g => [grantKey(g), g])).values()];
  const permitted = context.role === 'OWNER' && context.membershipId !== original.membershipId && original.role !== 'OWNER' && original.state !== 'REVOKED';
  if (!permitted) return null;
  const manage = context.permissions.includes('team.membership.manage'), edit = context.permissions.includes('team.access.manage') && original.state === 'ACTIVE' && !original.grants.some(g => (g.permission.startsWith('team.') || g.permission.startsWith('support.')));
  if (!manage && !edit) return null;
  async function submit() {
    if (!operation || running.current) return;
    const command = pending.current ?? { operation, role, grants, reason, password, reauthKey: crypto.randomUUID(), key: crypto.randomUUID() };
    if (command.operation === 'access') { const missing = missingTeamGrantReads(command.grants); if (missing.length) { setError('Cần chọn quyền đọc cùng phạm vi: ' + missing.map(p => teamCapabilities[p].label).join(', ')); return; } }
    pending.current = command; running.current = true; setBusy(true); setError('');
    try {
      if (!command.proofId) { const proof = await customerClient.reauthenticateTeam(command.password, command.reauthKey); command.proofId = proof.proofId; command.password = ''; setPassword(''); }
      await customerClient.changeTeamMember(context, original, { operation: command.operation, role: command.role, grants: command.grants, reason: command.reason, reauthenticationProofId: command.proofId }, command.key);
      pending.current = undefined; setPassword(''); setUncertain(false); saved();
    } catch (failure) {
      const unknown = failure instanceof CustomerApiError && ['OUTCOME_UNKNOWN', 'RESPONSE_INVALID', 'SERVICE_UNAVAILABLE', 'NETWORK_UNAVAILABLE'].includes(failure.code);
      setUncertain(unknown); if (!unknown) { command.password = ''; pending.current = undefined; setPassword(''); }
      const code = failure instanceof CustomerApiError ? failure.code : '';
      setError(unknown ? 'Kết quả chưa xác định. Giữ nguyên yêu cầu và kiểm tra lại; proof đã dùng không làm mất khả năng nhận receipt cũ.' : code === 'VERSION_MISMATCH' ? 'Thành viên đã thay đổi. Đóng thao tác, tải lại chi tiết rồi xác nhận lại.' : code === 'TOKEN_INVALID_OR_EXPIRED' ? 'Xác thực lại đã hết hiệu lực hoặc được dùng. Hãy nhập lại mật khẩu để tiếp tục.' : code === 'TEAM_STATE_CONFLICT' ? 'Quyền cũ nằm ngoài giới hạn chỉnh sửa hiện hành. Cần operator xử lý đúng phạm vi; không tự bỏ quyền cũ.' : 'Không thể thực hiện. Kiểm tra mật khẩu, quyền hiện hành và phạm vi quyền của thành viên.');
    } finally { running.current = false; setBusy(false); }
  }
  return <section className="otm-member-command" aria-label="Quản trị Membership">{!operation ? <div className="otm-pagination">{edit && <button type="button" className="pw-secondary" onClick={() => setOperation('access')}>Cập nhật quyền</button>}{manage && <><button type="button" className="pw-secondary" onClick={() => setOperation(original.state === 'SUSPENDED' ? 'resume' : 'suspend')}>{original.state === 'SUSPENDED' ? 'Mở lại Membership' : 'Tạm khóa Membership'}</button><button type="button" className="pw-secondary" onClick={() => setOperation('revoke')}>Thu hồi Membership</button></>}</div> : <form className="otm-invitation-form" onSubmit={event => { event.preventDefault(); void submit(); }}><h4>{title[operation]}</h4><p>{operation === 'revoke' ? 'Thu hồi là trạng thái cuối, không khôi phục bằng lời mời khác. Tài khoản cá nhân và Organization khác không bị thu hồi.' : 'Thay đổi chỉ áp dụng Membership tại doanh nghiệp này; Backend kiểm tra lại quyền và phạm vi.'}</p><fieldset disabled={busy || uncertain}>
      {operation === 'access' && <><label>Vai trò mới<select value={role} onChange={event => setRole(event.target.value as typeof role)}><option value="OPERATOR">Nhân viên vận hành</option><option value="MANAGER">Quản lý</option></select></label><div className="otm-invitation-grants">{options.map(g => <label key={grantKey(g)}><input type="checkbox" checked={grants.some(p => grantKey(p) === grantKey(g))} disabled={g.permission === 'dashboard.read' && g.scope === 'ORGANIZATION'} onChange={event => setGrants(old => event.target.checked ? [...old, g] : old.filter(p => grantKey(p) !== grantKey(g)))} /><span>{teamCapabilities[g.permission].label}<small>{g.scope === 'ORGANIZATION' ? ' · Toàn doanh nghiệp' : ' · Kho ' + g.facilityId}</small></span></label>)}</div></>}
      <label>Lý do thay đổi<textarea required maxLength={300} rows={2} value={reason} onChange={event => setReason(event.target.value)} /></label><label>Mật khẩu hiện tại<input required type="password" maxLength={72} autoComplete="current-password" value={password} onChange={event => setPassword(event.target.value)} /></label>
    </fieldset>{error && <p role="alert">{error}</p>}<div className="otm-pagination"><button type="submit" className="pw-primary" disabled={busy}>{busy ? 'Đang kiểm tra…' : uncertain ? 'Kiểm tra lại đúng yêu cầu' : 'Xác nhận thay đổi'}</button>{!uncertain && <button className="pw-secondary" type="button" disabled={busy} onClick={() => { setOperation(undefined); setPassword(''); setError(''); }}>Đóng thao tác</button>}</div></form>}</section>;
}
