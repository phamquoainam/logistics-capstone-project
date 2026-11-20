import { useEffect, useRef, useState } from 'react';
import type { BusinessContext } from './business-data';
import { customerClient, CustomerApiError } from './client';
import { staffClient } from './staff-client';
import { evidenceInput, saveEvidence } from './claim-evidence';
import type { ClaimEvidence } from './claim-evidence';

type Props = { intake: string; version: number; mode: { kind: 'merchant'; context: BusinessContext } | { kind: 'staff'; organization: string }; writable?: boolean; disabled?: boolean; lock?: (locked: boolean) => void; changed?: () => void };
export function ClaimEvidencePanel({ intake, version, mode, writable = false, disabled = false, lock, changed }: Props) {
  const context = mode.kind === 'merchant' ? mode.context : undefined, organization = context?.organizationId ?? (mode.kind === 'staff' ? mode.organization : '');
  const [items, setItems] = useState<ClaimEvidence[]>(), [file, setFile] = useState<File>(), [busy, setBusy] = useState(false), [uncertain, setUncertain] = useState(false), [error, setError] = useState(''), [revision, setRevision] = useState(0);
  const pending = useRef<{ key: string; version: number; file: File; evidence?: ClaimEvidence } | null>(null);
  useEffect(() => { let live = true; const read = context ? customerClient.claimEvidence(context, intake) : staffClient.claimEvidence(organization, intake); void read.then(v => { if (live) { setItems(v); setError(''); } }).catch(() => { if (live) setError('Không thể đọc danh sách tệp trong phạm vi hiện tại.'); }); return () => { live = false; }; }, [context, organization, intake, revision]);
  useEffect(() => () => { pending.current = null; lock?.(false); }, [lock]);
  async function upload(retry = false) {
    if (!context || busy || disabled || !retry && (!file || uncertain)) return;
    const work: NonNullable<typeof pending.current> | null = retry ? pending.current : file ? { key: crypto.randomUUID(), version, file } : null; if (!work) return;
    pending.current = work; setBusy(true); lock?.(true); setError(''); let unresolved = false;
    try { const input = await evidenceInput(work.file); work.evidence ??= await customerClient.reserveClaimEvidence(context, intake, input, work.version, work.key); await customerClient.uploadClaimEvidence(context, work.evidence, work.file); pending.current = null; setFile(undefined); setUncertain(false); setRevision(v => v + 1); changed?.(); }
    catch (e) { unresolved = !(e instanceof CustomerApiError) || e.code === 'OUTCOME_UNKNOWN' || e.status >= 500 || e.status === 0; if (unresolved) { setUncertain(true); setError('Tệp chưa hoàn tất. Thử lại đúng tệp và yêu cầu đã lưu để xác minh kết quả.'); } else { pending.current = null; setUncertain(false); setError('Không thể nhận tệp. Kiểm tra định dạng, giới hạn, trạng thái hồ sơ và quyền truy cập.'); changed?.(); setRevision(v => v + 1); } }
    finally { setBusy(false); lock?.(unresolved); }
  }
  async function download(evidence: ClaimEvidence) { if (busy || disabled || uncertain) return; setBusy(true); try { const blob = context ? await customerClient.downloadClaimEvidence(context, evidence) : await staffClient.downloadClaimEvidence(organization, evidence); saveEvidence(blob, evidence); } catch { setError('Không thể tải tệp. Quyền truy cập hoặc tính toàn vẹn có thể đã thay đổi.'); } finally { setBusy(false); } }
  return <section className="os-claim-evidence" aria-label="Tệp bằng chứng"><h4>Tệp bằng chứng</h4><p>PNG, JPEG hoặc PDF không có nội dung chủ động; tối đa 5 MB/tệp, 10 tệp và 25 MB/hồ sơ. Chỉ tệp đã kiểm tra sạch mới được tải về.</p>{error && <p role="alert">{error}</p>}
    {!items ? <p role="status">Đang tải danh sách tệp…</p> : items.length ? <ul>{items.map((item, index) => <li key={item.id}><span>Tệp {index + 1} · {item.mime.split('/')[1].toUpperCase()} · {Math.ceil(item.bytes / 1024)} KB · {item.state === 'CLEAN' ? 'Đã kiểm tra' : 'Chờ hoàn tất tải lên'}</span>{item.state === 'CLEAN' && <button className="pw-secondary" disabled={busy || disabled || uncertain} onClick={() => { void download(item); }}>Tải bằng chứng {index + 1}</button>}</li>)}</ul> : <p>Chưa có tệp bằng chứng.</p>}
    {context && writable && items && <form onSubmit={e => { e.preventDefault(); void upload(); }}><label>Tệp bằng chứng cần gửi<input type="file" accept="image/png,image/jpeg,application/pdf" disabled={busy || disabled || uncertain} onChange={e => { setFile(e.target.files?.[0]); e.currentTarget.value = ''; }} /></label><button className="pw-primary" disabled={busy || disabled || uncertain || !file}>{busy ? 'Đang kiểm tra và tải tệp…' : 'Tải tệp bằng chứng'}</button></form>}
    {uncertain && <div className="os-context-actions"><button className="pw-secondary" disabled={busy || disabled} onClick={() => { void upload(true); }}>Thử lại đúng tệp bằng chứng</button><button className="pw-secondary" disabled={busy || disabled} onClick={() => { pending.current = null; setUncertain(false); setFile(undefined); lock?.(false); changed?.(); setRevision(v => v + 1); }}>Dừng thao tác trên trình duyệt</button></div>}
  </section>;
}
