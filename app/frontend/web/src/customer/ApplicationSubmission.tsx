import { useEffect, useRef, useState } from 'react';
import { customerClient, CustomerApiError } from './client';
import type { ApplicationDraft, VersionedApplication } from './client';
import type { ApplicationRequirements, ApplicationEvidence, EvidencePurpose } from './application-workflow-data';

const purposes: Record<EvidencePurpose, string> = { REGISTRATION: 'Đăng ký pháp nhân', REPRESENTATIVE_AUTHORIZATION: 'Thẩm quyền đại diện' };
function failureText(error: unknown) {
  const code = error instanceof CustomerApiError ? error.code : '';
  return ({ POLICY_UNAVAILABLE: 'Chưa có chính sách phù hợp với loại hình và nơi cấp đã lưu. Chưa thể nộp hồ sơ.', CONTACT_PROOF_REQUIRED: 'Cần xác minh lại email liên hệ trước khi nộp hồ sơ.', EVIDENCE_NOT_READY: 'Tài liệu chưa vượt qua kiểm tra. Hãy chọn tài liệu hợp lệ.', PRECONDITION_STALE: 'Hồ sơ đã thay đổi. Tải lại bản hiện tại trước khi tiếp tục.', STATE_CONFLICT: 'Trạng thái hồ sơ không cho phép thao tác này.', VALIDATION_FAILED: 'Kiểm tra lại thông tin bắt buộc và tài liệu của hồ sơ.', SESSION_CHANGED: 'Phiên đăng nhập đã thay đổi.', RESOURCE_NOT_FOUND: 'Không tìm thấy hồ sơ hoặc tài liệu thuộc quyền truy cập.' } as Record<string, string>)[code] ?? 'Chưa thể hoàn tất yêu cầu. Không có kết quả thành công được xác nhận.';
}
function complete(input: ApplicationDraft) {
  return !!(input.entityType && input.jurisdiction && input.registeredName && input.legalIdentifier && input.registeredAddress && input.operatingAddress && input.representativeName && input.representativeEmail && input.representativeRelationship && input.businessContactAttestationId);
}
/** Applicant SELF workflow. Keys, files and pending commands stay in this page's memory only. */
export function ApplicationSubmission({ original, disabled, saved, lock }: { original: VersionedApplication; disabled: boolean; saved: (value: VersionedApplication) => void; lock: (value: boolean) => void }) {
  const [requirements, setRequirements] = useState<ApplicationRequirements>(), [evidence, setEvidence] = useState<ApplicationEvidence[]>([]), [loading, setLoading] = useState(true), [loadError, setLoadError] = useState(''), [error, setError] = useState(''), [busy, setBusy] = useState(false), [uncertain, setUncertain] = useState(false);
  const [accuracy, setAccuracy] = useState(false), [representation, setRepresentation] = useState(false), [displayName, setDisplayName] = useState(''), [purpose, setPurpose] = useState<EvidencePurpose>('REGISTRATION'), [file, setFile] = useState<File>(), [reload, setReload] = useState(0);
  const pending = useRef<(() => Promise<void>) | undefined>(undefined), running = useRef(false);
  const id = original.application.id, input = original.application.draft ?? {};
  useEffect(() => {
    let active = true;
    Promise.all([customerClient.applicationRequirements(id), Promise.all((original.application.draft?.evidenceIds ?? []).map(value => customerClient.readApplicationEvidence(id, value)))]).then(([policy, documents]) => { if (active) { setRequirements(policy); setEvidence(documents); setLoadError(''); } }).catch(e => { if (active) setLoadError(failureText(e)); }).finally(() => { if (active) setLoading(false); });
    return () => { active = false; };
  }, [id, original.application.draft?.evidenceIds, reload]);
  useEffect(() => { if (!uncertain) return; const warn = (event: BeforeUnloadEvent) => event.preventDefault(); window.addEventListener('beforeunload', warn); return () => window.removeEventListener('beforeunload', warn); }, [uncertain]);
  async function execute(command?: () => Promise<void>) {
    if (running.current || disabled) return; const work = pending.current ?? command; if (!work) return;
    pending.current = work; running.current = true; setBusy(true); setError(''); lock(true);
    try { await work(); pending.current = undefined; setUncertain(false); lock(false); }
    catch (e) { const unknown = e instanceof CustomerApiError && (e.code === 'OUTCOME_UNKNOWN' || e.code === 'NETWORK_UNAVAILABLE' || e.code === 'RESPONSE_INVALID' || e.status === 503); setUncertain(unknown); setError(failureText(e)); if (!unknown) { pending.current = undefined; lock(false); } }
    finally { running.current = false; setBusy(false); }
  }
  const readyEvidence = requirements?.requiredEvidence.every(required => evidence.some(e => e.purpose === required && e.state === 'CLEAN')) ?? false;
  const blocked = disabled || loading || !!loadError || !requirements || busy || uncertain;
  return <section className="cp-application-submission" aria-labelledby="application-submission-title">
    <h2 id="application-submission-title">Tài liệu & nộp hồ sơ</h2>
    {disabled && <p className="cp-hint">Lưu các thay đổi của bản nháp trước khi chuẩn bị nộp.</p>}
    {loading ? <p role="status">Đang đọc yêu cầu hồ sơ…</p> : loadError ? <><p role="alert">{loadError}</p><button className="cp-secondary" type="button" disabled={busy || uncertain} onClick={() => { setLoading(true); setReload(v => v + 1); }}>Đọc lại yêu cầu hồ sơ</button></> : requirements && <>
      <p className="cp-hint">Chính sách phiên bản {requirements.policyVersion} · Cam kết {requirements.consentVersion}</p>
      <ul>{evidence.map(e => <li key={e.id}>{purposes[e.purpose]} · {e.state === 'CLEAN' ? 'Đã kiểm tra' : 'Chưa đủ điều kiện'} · {Math.ceil(e.bytes / 1024)} KB</li>)}</ul>
      <fieldset disabled={blocked || (input.evidenceIds?.length ?? 0) >= 5}><legend>Bổ sung tài liệu</legend>
        <label htmlFor="application-evidence-purpose">Loại tài liệu</label><select id="application-evidence-purpose" value={purpose} onChange={e => setPurpose(e.target.value as EvidencePurpose)}>{requirements.requiredEvidence.map(p => <option key={p} value={p}>{purposes[p]}</option>)}</select>
        <label htmlFor="application-evidence-file">PDF, PNG hoặc JPEG · tối đa 5 MB/tài liệu</label><input id="application-evidence-file" type="file" accept="application/pdf,image/png,image/jpeg" onChange={e => { setFile(e.target.files?.[0]); setError(''); }} />
        <button type="button" className="cp-secondary" disabled={!file} onClick={() => {
          if (!file) return; const selected = file, selectedPurpose = purpose, uploadKey = crypto.randomUUID(), saveKey = crypto.randomUUID(); let uploaded: ApplicationEvidence | undefined;
          void execute(async () => { uploaded ??= await customerClient.uploadApplicationEvidence(id, selectedPurpose, selected, uploadKey); if (uploaded.state !== 'CLEAN') throw new CustomerApiError('EVIDENCE_NOT_READY'); await customerClient.updateApplication(id, { ...input, evidenceIds: [...(input.evidenceIds ?? []), uploaded.id] }, original.etag, saveKey); saved(await customerClient.readApplication(id)); });
        }}>Tải lên & lưu tài liệu</button>
      </fieldset>
      <fieldset disabled={blocked}><legend>Xác nhận trước khi nộp</legend>
        <label htmlFor="application-display-name">Tên hiển thị doanh nghiệp</label><input id="application-display-name" maxLength={160} value={displayName} onChange={e => setDisplayName(e.target.value)} />
        <p className="cp-hint">Tên hiển thị dùng khi được duyệt; tên pháp nhân đầy đủ vẫn được giữ trong hồ sơ.</p>
        <label className="cp-checkbox"><input type="checkbox" checked={accuracy} onChange={e => setAccuracy(e.target.checked)} />{requirements.consentStatements.accuracy}</label>
        <label className="cp-checkbox"><input type="checkbox" checked={representation} onChange={e => setRepresentation(e.target.checked)} />{requirements.consentStatements.representation}</label>
        {!complete(input) && <p className="cp-hint">Cần đủ thông tin pháp nhân, hai địa chỉ, người đại diện và email liên hệ đã xác minh.</p>}
        {!readyEvidence && <p className="cp-hint">Cần tài liệu đăng ký và tài liệu thẩm quyền đại diện đã kiểm tra.</p>}
        <button className="cp-primary" type="button" disabled={!accuracy || !representation || !displayName.trim() || !complete(input) || !readyEvidence} onClick={() => {
          const policy = requirements, name = displayName.normalize('NFC'), updateKey = crypto.randomUUID(), submitKey = crypto.randomUUID(); let prepared: VersionedApplication | undefined;
          const payload: ApplicationDraft = { ...input, declarations: { accuracy: true, representation: true, consentVersion: policy.consentVersion } };
          void execute(async () => { prepared ??= await customerClient.updateApplication(id, payload, original.etag, updateKey); await customerClient.applicationCommand(id, original.application.state === 'NEEDS_INFORMATION' ? 'resubmit' : 'submit', name, prepared.etag, submitKey); saved(await customerClient.readApplication(id)); });
        }}>{original.application.state === 'NEEDS_INFORMATION' ? 'Nộp lại hồ sơ' : 'Nộp hồ sơ xét duyệt'}</button>
      </fieldset>
      <p className="cp-hint">Nộp hồ sơ chưa tạo Organization hoặc cấp quyền Business. Hồ sơ cần được thẩm định độc lập.</p>
    </>}
    {error && <p role="alert">{error}</p>}{busy && <p role="status">Đang xử lý…</p>}
    {uncertain && <><p role="status">Chưa xác định kết quả. Dữ liệu và request key ban đầu được giữ trên trang này; không tạo yêu cầu mới.</p><button className="cp-secondary" type="button" disabled={busy} onClick={() => { void execute(); }}>Kiểm tra / thử lại đúng yêu cầu</button></>}
  </section>;
}
