import { useEffect, useRef, useState } from 'react';
import { adminSession, AdminSessionError } from './session';
import { governanceId, parseApplication } from './governance';
import type { ApplicationSummary } from './governance';

type Intent = { id: string; version: number; revision: number; policy: string; reason: string; digest: string; stepKey: string; decisionKey: string; proof?: string; operation: 'REJECT' | 'APPROVE' };
export async function rejectionDigest(value: { id: string; version: number; revision: number; policy: string; reason: string; operation?: 'REJECT' | 'APPROVE' }) {
  // Matches the approved Merchant TreeMap canonical command, excluding credentials/proof.
  const body = JSON.stringify({ operation: value.operation ?? 'REJECT', originalVersion: value.version, policy: value.policy, reason: value.reason, revision: value.revision, target: value.id });
  return [...new Uint8Array(await crypto.subtle.digest('SHA-256', new TextEncoder().encode(body)))].map(b => b.toString(16).padStart(2, '0')).join('');
}
function stepResult(value: unknown) {
  if (!value || typeof value !== 'object' || Array.isArray(value)) throw new AdminSessionError('RESPONSE_INVALID');
  const v = value as Record<string, unknown>;
  if (Object.keys(v).length !== 2 || !governanceId(v.proofId) || typeof v.expiresAt !== 'string' || !Number.isFinite(Date.parse(v.expiresAt)) || Date.parse(v.expiresAt) <= Date.now() || Date.parse(v.expiresAt) > Date.now() + 901000) throw new AdminSessionError('RESPONSE_INVALID');
  return v.proofId as string;
}
/** Dedicated independent decision; pending retry never stores the credential. */
export function AdminRejection({ application, policyVersion, committed, lock }: { application: ApplicationSummary; policyVersion: string; committed: (value: ApplicationSummary) => void; lock: (busy: boolean) => void }) {
  const [operation, setOperation] = useState<'REJECT' | 'APPROVE'>('REJECT');
  const [reason, setReason] = useState(''), [password, setPassword] = useState(''), [busy, setBusy] = useState(false), [uncertain, setUncertain] = useState(false), [error, setError] = useState('');
  const intent = useRef<Intent | null>(null), running = useRef(false), active = useRef(false);
  useEffect(() => { active.current = true; return () => { active.current = false; intent.current = null; }; }, []);
  useEffect(() => { if (!uncertain) return; const warn = (e: BeforeUnloadEvent) => e.preventDefault(); window.addEventListener('beforeunload', warn); return () => window.removeEventListener('beforeunload', warn); }, [uncertain]);
  async function submit() {
    if (running.current) return; running.current = true; setBusy(true); setError(''); lock(true);
    const credential = password; setPassword('');
    try {
      if (!intent.current) {
        const input = { id: application.id, version: application.version, revision: application.revision, policy: policyVersion, operation, reason: reason.normalize('NFC') };
        if (!input.reason.trim() || [...input.reason].length > 300 || /[\p{Cc}\p{Cs}\u2028\u2029]/u.test(input.reason)) throw new AdminSessionError('VALIDATION_FAILED');
        intent.current = { ...input, digest: await rejectionDigest(input), stepKey: crypto.randomUUID(), decisionKey: crypto.randomUUID() };
      }
      const i = intent.current;
      if (!i.proof) {
        if (!credential) throw new AdminSessionError('VALIDATION_FAILED');
        i.proof = await adminSession.governanceWrite('stepup', i.id, i.version, i.stepKey, { operation: i.operation, targetId: i.id, expectedVersion: i.version, commandDigest: i.digest, password: credential }, stepResult);
      }
      await adminSession.governanceWrite(i.operation === 'APPROVE' ? 'approval' : 'rejection', i.id, i.version, i.decisionKey, { revision: i.revision, policyVersion: i.policy, reauthenticationProofId: i.proof, reason: i.reason }, v => parseApplication(v, i.id));
      const current = await adminSession.read('application-detail', new URLSearchParams(), v => parseApplication(v, i.id), i.id);
      intent.current = null; if (active.current) { setUncertain(false); lock(false); committed(current); }
    } catch (failure) {
      const code = failure instanceof AdminSessionError ? failure.code : 'SERVICE_UNAVAILABLE';
      const unknown = ['OUTCOME_UNKNOWN', 'NETWORK_UNAVAILABLE', 'RESPONSE_INVALID', 'SERVICE_UNAVAILABLE'].includes(code);
      if (!unknown) intent.current = null;
      if (active.current) { setUncertain(unknown); if (!unknown) lock(false); setError(unknown ? 'Chưa xác định kết quả. Thử lại đúng quyết định đã gửi; nếu xác thực lại chưa trả kết quả, nhập lại mật khẩu trực tiếp tại đây.' : code === 'ACCESS_DENIED' ? 'Bạn không còn quyền quyết định hồ sơ này.' : code === 'SESSION_INVALID' ? 'Phiên xác thực không còn hợp lệ.' : 'Hồ sơ hoặc điều kiện quyết định đã thay đổi. Đọc lại hồ sơ trước khi tiếp tục.'); }
    } finally { running.current = false; if (active.current) setBusy(false); }
  }
  return <section className="ap-review-decision"><h3>Quyết định độc lập</h3><p>{operation === 'APPROVE' ? 'Phê duyệt tạo doanh nghiệp và Owner từ hồ sơ đã thẩm định. Chỉ cấp quyền Tổng quan ban đầu.' : 'Từ chối hồ sơ cần lý do và xác thực lại. Không tạo Organization hoặc quyền Business.'}</p>
    {application.allowedActions.includes('APPROVE') && <label>Quyết định<select aria-label="Quyết định" disabled={busy || uncertain} value={operation} onChange={e => setOperation(e.target.value as 'REJECT' | 'APPROVE')}><option value="REJECT">Từ chối hồ sơ</option><option value="APPROVE">Phê duyệt & kích hoạt doanh nghiệp</option></select></label>}
    <label>{operation === 'APPROVE' ? 'Lý do phê duyệt' : 'Lý do từ chối'}<textarea maxLength={300} disabled={busy || uncertain} value={reason} onChange={e => setReason(e.target.value)} /></label>
    {(!uncertain || !intent.current?.proof) && <label>Mật khẩu xác thực lại<input type="password" autoComplete="current-password" value={password} disabled={busy} onChange={e => setPassword(e.target.value)} /></label>}
    {error && <p role="alert" className="ap-error">{error}</p>}
    <button className="ap-primary" type="button" disabled={busy || !uncertain && (!reason.trim() || !password) || uncertain && !intent.current?.proof && !password} onClick={() => void submit()}>{busy ? 'Đang xác nhận…' : uncertain ? 'Thử lại đúng quyết định' : operation === 'APPROVE' ? 'Xác thực & phê duyệt hồ sơ' : 'Xác thực & từ chối hồ sơ'}</button>
  </section>;
}
