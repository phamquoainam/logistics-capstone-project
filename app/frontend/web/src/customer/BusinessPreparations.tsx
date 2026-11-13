import { useEffect, useRef, useState } from 'react';
import type { FormEvent } from 'react';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import { customerClient, CustomerApiError } from './client';
import type { BusinessPreparation, BusinessPreparationInput, BusinessPreparationOperation, BusinessPreparationPage, DraftContact } from './client';
import type { BusinessContext } from './business-data';
import { emptyParcel, parcelTotals } from './shipping-entry';
import { PilotBookingPanel } from './PilotShipping';
import { BusinessEntryMode, ContactFields, EntryHeading, EntryToolbar, NotesFields, ParcelFields, PaymentFacts, SavedShippingSnapshot, ServiceFacts, entryNumber } from './ShipmentEntryFields';
import './business-preparations.css';

const contact = (): DraftContact => ({ name: '', phone: '', address: { country: 'VN', administrativeArea: '', locality: '', addressLine: '' } });
const blank = (): BusinessPreparationInput => ({ sender: contact(), recipient: contact(), parcels: [emptyParcel()] });
const inputOf = (d: BusinessPreparation): BusinessPreparationInput => ({ ...structuredClone(d.input), facilityId: d.facilityId, sourceChannel: d.sourceChannel, sourceOrderReference: d.sourceOrderReference });
function message(error: unknown) {
  const code = error instanceof CustomerApiError ? error.code : '';
  return ({ RESOURCE_NOT_FOUND: 'Không có quyền truy cập bản nháp hoặc kho này.', BUSINESS_CONTEXT_UNAVAILABLE: 'Quyền doanh nghiệp đã thay đổi. Hãy tải lại không gian làm việc.', PRECONDITION_STALE: 'Bản nháp đã thay đổi. Nội dung đang nhập được giữ lại; tải bản mới trước khi lưu tiếp.', IDEMPOTENCY_CONFLICT: 'Mã thao tác đã được dùng cho nội dung khác.', SOURCE_REFERENCE_CONFLICT: 'Nguồn và mã đơn này đã có bản nháp trong doanh nghiệp.', SOURCE_REFERENCE_IMMUTABLE: 'Nguồn đơn đã được gắn cố định khi tạo bản nháp.', VALIDATION_FAILED: 'Kiểm tra liên hệ, các kiện và cặp nguồn / mã đơn.', OUTCOME_UNKNOWN: 'Chưa xác định kết quả. Kiểm tra kết quả trước khi thực hiện thao tác khác.' } as Record<string, string>)[code] ?? 'Chưa hoàn tất yêu cầu. Hãy kiểm tra kết nối và thử lại.';
}
export function BusinessPreparationList({ context }: { context: BusinessContext }) {
  const [page, setPage] = useState(0), [revision, setRevision] = useState(0), [result, setResult] = useState<{ key: string; data?: BusinessPreparationPage; error?: string }>();
  const key = context.organizationId + ':' + context.authorityVersion + ':' + page + ':' + revision, base = '/merchant/' + context.organizationId;
  useEffect(() => { let active = true; void customerClient.listBusinessPreparations(context, page).then(data => { if (active) setResult({ key, data }); }).catch(e => { if (active) setResult({ key, error: message(e) }); }); return () => { active = false; }; }, [context, key, page]);
  const current = result?.key === key ? result : undefined;
  return <section className="bp-workspace"><div className="bw-heading"><div><span className="pw-eyebrow">{context.displayName}</span><h1>Đơn nháp doanh nghiệp</h1><p>Thông tin chuẩn bị trước khi xác nhận vận đơn.</p></div><div className="se-heading-actions"><Link className="pw-secondary" to={base + '/shipments'}>Vận đơn đã tạo</Link>{context.permissions.includes('shipment.draft.write') && <Link className="pw-primary" to={base + '/create'}>Tạo bản nháp</Link>}</div></div>
    {!current ? <p role="status">Đang kiểm tra quyền và tải bản nháp…</p> : current.error ? <div className="bw-panel bp-empty"><p role="alert">{current.error}</p><button className="pw-secondary" onClick={() => setRevision(v => v + 1)}>Thử lại</button></div> : current.data && <>
      {current.data.items.length ? <div className="bw-panel bp-list"><table><thead><tr><th>Mã tham chiếu</th><th>Người gửi / người nhận</th><th>Kiện</th><th>Cập nhật</th><th>Thao tác</th></tr></thead><tbody>{current.data.items.map(d => <tr key={d.id}><td>{d.input.reference ?? d.sourceOrderReference ?? 'Bản nháp'}<small>Chưa Booking</small></td><td>{d.input.sender.name}<small>{d.input.recipient.name}</small></td><td>{d.input.parcels.length}</td><td>{new Date(d.updatedAt).toLocaleString('vi-VN')}</td><td><Link to={base + '/create?draft=' + d.id}>Mở bản nháp</Link></td></tr>)}</tbody></table></div> : <div className="bw-panel bp-empty"><h2>Chưa có bản nháp</h2><p>Bản nháp của doanh nghiệp sẽ xuất hiện tại đây.</p></div>}
      <nav className="ab-pagination" aria-label="Phân trang bản nháp doanh nghiệp"><button className="pw-secondary" disabled={page === 0} onClick={() => setPage(v => v - 1)}>Trang trước</button><span>Trang {page + 1}</span><button className="pw-secondary" disabled={!current.data.hasNext} onClick={() => setPage(v => v + 1)}>Trang sau</button></nav>
    </>}
  </section>;
}
export function BusinessPreparationEditor({ context }: { context: BusinessContext }) {
  const [params] = useSearchParams(), id = params.get('draft'), review = params.get('step') === 'review', navigate = useNavigate();
  const [input, setInput] = useState<BusinessPreparationInput>(blank), [original, setOriginal] = useState<BusinessPreparation>(), [loaded, setLoaded] = useState(!id), [loadError, setLoadError] = useState(''), [error, setError] = useState(''), [notice, setNotice] = useState(''), [busy, setBusy] = useState(false), [uncertain, setUncertain] = useState(false), [revision, setRevision] = useState(0);
  const pending = useRef<{ operation: BusinessPreparationOperation; key: string; target?: string; review?: boolean } | undefined>(undefined), running = useRef(false), live = useRef(true);
  const base = '/merchant/' + context.organizationId, dirty = JSON.stringify(input) !== JSON.stringify(original ? inputOf(original) : blank());
  useEffect(() => { live.current = true; return () => { live.current = false; }; }, []);
  useEffect(() => { if (!dirty && !uncertain) return; const warn = (e: BeforeUnloadEvent) => { e.preventDefault(); }; window.addEventListener('beforeunload', warn); return () => window.removeEventListener('beforeunload', warn); }, [dirty, uncertain]);
  useEffect(() => { let active = true; if (!id) return;
    void customerClient.readBusinessPreparation(context, id).then(d => { if (active) { setOriginal(d); setInput(inputOf(d)); setLoaded(true); setLoadError(''); } }).catch(e => { if (active) { setLoadError(message(e)); setLoaded(true); } }); return () => { active = false; };
  }, [context, id, revision]);
  function accept(d: BusinessPreparation, showReview = false) {
    pending.current = undefined; setUncertain(false); setOriginal(d); setInput(inputOf(d)); setError('');
    if (d.state === 'ARCHIVED') navigate(base + '/preparations', { replace: true });
    else { setNotice('Đã lưu bản nháp doanh nghiệp. Chưa tạo Shipment hoặc nghĩa vụ cước.'); if (id !== d.id || showReview !== review) navigate(base + '/create?draft=' + d.id + (showReview ? '&step=review' : ''), { replace: true }); }
  }
  async function save(event: FormEvent<HTMLFormElement>) {
    event.preventDefault(); if (running.current || uncertain) return; const showReview = (event.nativeEvent as SubmitEvent).submitter?.getAttribute('value') === 'review'; if (showReview && original && !dirty) { accept(original, true); return; } running.current = true; setBusy(true); setError(''); setNotice('');
    pending.current = { operation: original ? 'UPDATE' : 'CREATE', key: crypto.randomUUID(), target: original?.id, review: showReview };
    try { const d = await customerClient.saveBusinessPreparation(context, input, pending.current.key, original); if (live.current) accept(d, showReview); }
    catch (e) { if (live.current) { setError(message(e)); if (e instanceof CustomerApiError && e.code === 'OUTCOME_UNKNOWN') setUncertain(true); else pending.current = undefined; } }
    finally { running.current = false; if (live.current) setBusy(false); }
  }
  async function archive() {
    if (!original || running.current || uncertain || !window.confirm('Lưu trữ bản nháp này?')) return;
    running.current = true; setBusy(true); setError(''); pending.current = { operation: 'ARCHIVE', key: crypto.randomUUID(), target: original.id };
    try { const d = await customerClient.archiveBusinessPreparation(context, original, pending.current.key); if (live.current) accept(d); }
    catch (e) { if (live.current) { setError(message(e)); if (e instanceof CustomerApiError && e.code === 'OUTCOME_UNKNOWN') setUncertain(true); else pending.current = undefined; } }
    finally { running.current = false; if (live.current) setBusy(false); }
  }
  async function resolve() {
    if (!pending.current || running.current) return; running.current = true; setBusy(true);
    try { const request = pending.current, r = await customerClient.businessPreparationReceipt(context, request.operation, request.key); if (request.target && r.targetId !== request.target) throw new Error(); const d = await customerClient.readBusinessPreparation(context, r.targetId); if (live.current) accept(d, request.review); }
    catch { if (live.current) setError('Chưa xác nhận được kết quả. Giữ trang này và kiểm tra lại; không gửi thao tác mới.'); }
    finally { running.current = false; if (live.current) setBusy(false); }
  }
  const editable = original ? original.allowedActions.includes('UPDATE') : context.permissions.includes('shipment.draft.write');
  const totals = parcelTotals(input.parcels), disabled = busy || uncertain || !editable;
  const merchantFields = <section className="pw-panel se-merchant"><EntryHeading icon="note" title="Dữ liệu doanh nghiệp" /><div className="se-card-body"><dl className="se-facts"><div><dt>Doanh nghiệp sở hữu</dt><dd>{context.displayName}</dd></div><div><dt>Kho / điểm gửi</dt><dd>{input.facilityId ? 'Đã gắn kho' : 'Chưa gắn kho'}</dd></div>{original && <div><dt>Phiên bản bản nháp</dt><dd>v{original.version}</dd></div>}</dl>{input.facilityId && <details className="se-policy-details"><summary>Mã kho đã gắn</summary><code>{input.facilityId}</code></details>}<div className="se-fields">
    <div><label htmlFor="business-source-channel">Nguồn đơn</label><input id="business-source-channel" maxLength={40} disabled={!!original || disabled} required={!!input.sourceOrderReference} placeholder="Ví dụ: WEB, CUA_HANG" value={input.sourceChannel ?? ''} onChange={e => setInput(v => ({ ...v, sourceChannel: e.target.value || null }))} /></div>
    <div><label htmlFor="business-source-reference">Mã đơn nguồn</label><input id="business-source-reference" maxLength={80} disabled={!!original || disabled} required={!!input.sourceChannel} placeholder="Mã đơn trong hệ thống bán hàng" value={input.sourceOrderReference ?? ''} onChange={e => setInput(v => ({ ...v, sourceOrderReference: e.target.value || null }))} /></div>
  </div><p className="se-hint">Điền cả nguồn và mã đơn, hoặc để trống cả hai. Cặp mã được giữ nguyên sau lần lưu đầu để chống tạo trùng trong doanh nghiệp.</p></div></section>;
  if (review && original && loaded && !loadError) return <section className="sd-page se-entry se-business-entry"><div className="pw-page-heading"><div><span className="pw-eyebrow">{context.displayName}</span><h1 tabIndex={-1}>Xác nhận thông tin doanh nghiệp</h1><p>Thông tin đã được lưu tại Backend theo quyền doanh nghiệp hiện hành.</p></div><Link className="pw-secondary" to={base + '/preparations'}>Danh sách bản nháp</Link></div><div className="se-review-grid"><SavedShippingSnapshot input={original.input} /><div className="se-column"><section className="pw-panel"><EntryHeading icon="note" title="Dữ liệu doanh nghiệp" /><div className="se-card-body"><dl className="se-facts"><div><dt>Doanh nghiệp sở hữu</dt><dd>{context.displayName}</dd></div><div><dt>Nguồn đơn</dt><dd>{original.sourceChannel ?? 'Không khai báo'}</dd></div><div><dt>Mã đơn nguồn</dt><dd>{original.sourceOrderReference ?? 'Không khai báo'}</dd></div><div><dt>Trạng thái</dt><dd>{({ DRAFT: 'Bản nháp', ARCHIVED: 'Đã lưu trữ', BOOKED: 'Đã Booking' })[original.state]}</dd></div><div><dt>Phiên bản</dt><dd>v{original.version}</dd></div></dl></div></section><ServiceFacts business /><PaymentFacts business />{original.state === 'DRAFT' ? <PilotBookingPanel draft={original} business={context} onEdit={editable ? () => navigate(base + '/create?draft=' + original.id) : undefined} /> : <p role="status">{original.state === 'BOOKED' ? 'Đã xác nhận Booking. Xem danh sách vận đơn doanh nghiệp.' : 'Bản nháp đã lưu trữ.'}</p>}</div></div></section>;
  return <section className="sd-page se-entry se-business-entry"><div className="pw-page-heading"><div><span className="pw-eyebrow">{context.displayName}</span><h1>{original ? 'Tiếp tục bản nháp' : 'Tạo đơn doanh nghiệp'}</h1><p>Điền thông tin người gửi, người nhận và các kiện của doanh nghiệp.</p></div><div className="se-heading-actions">{context.permissions.includes('shipment.draft.read') && <Link className="pw-secondary" to={base + '/shipments'} onClick={e => { if (busy || uncertain || dirty && !window.confirm('Rời trang sẽ bỏ nội dung chưa lưu. Tiếp tục?')) e.preventDefault(); }}>Vận đơn đã tạo</Link>}<Link className="pw-secondary" to={base + '/preparations'} onClick={e => { if ((dirty || uncertain) && !window.confirm('Rời trang sẽ bỏ nội dung chưa lưu. Tiếp tục?')) e.preventDefault(); }}>Danh sách bản nháp</Link></div></div>
    <BusinessEntryMode base={base} mode="manual" canImport={context.permissions.includes('shipment.import')} blocked={busy || uncertain} confirmLeave={() => !dirty || window.confirm('Chuyển sang nhập file sẽ bỏ nội dung chưa lưu. Tiếp tục?')} />
    <EntryToolbar />
    {!loaded ? <p role="status">Đang tải bản nháp…</p> : loadError ? <div className="cp-notice cp-error"><p role="alert">{loadError}</p><button type="button" className="pw-secondary" onClick={() => setRevision(v => v + 1)}>Tải lại bản nháp</button></div> : <form onSubmit={event => { void save(event); }}>
      {error && <p role="alert" className="cp-notice cp-error">{error}</p>}{notice && <p role="status" className="cp-notice">{notice}</p>}{uncertain && <button type="button" className="pw-secondary" disabled={busy} onClick={() => { void resolve(); }}>Kiểm tra kết quả lưu</button>}{!editable && <p role="status">Bạn chỉ có quyền xem bản nháp này.</p>}
      <div className="se-columns"><div className="se-column">{(['sender', 'recipient'] as const).map(kind => <section className="pw-panel se-contact" key={kind}><EntryHeading icon="user" title={kind === 'sender' ? 'Người gửi' : 'Người nhận'} /><fieldset disabled={disabled} className="se-card-body"><ContactFields kind={kind} value={input[kind]} change={value => setInput(v => ({ ...v, [kind]: value }))} /><p className="se-hint">Liên hệ do doanh nghiệp khai báo; địa chỉ chưa xác nhận vùng phục vụ.</p></fieldset></section>)}<ServiceFacts business />{merchantFields}</div>
      <div className="se-column"><ParcelFields parcels={input.parcels} disabled={disabled} change={parcels => setInput(v => ({ ...v, parcels }))} /><PaymentFacts business /><NotesFields input={input} disabled={disabled} change={patch => setInput(v => ({ ...v, ...patch }))} /></div></div>
      <div className="se-action-bar"><dl aria-label="Tổng kết khai báo"><div><dt>Số kiện / trọng lượng</dt><dd>{totals.parcels} kiện · {entryNumber(totals.weightGrams)} g</dd></div><div><dt>Tổng cước</dt><dd className="se-fee-pending">Chưa có báo giá</dd></div></dl><div className="se-actions"><button className="se-reset" type="button" disabled={disabled} onClick={() => { if (!dirty || window.confirm('Làm mới sẽ bỏ nội dung chưa lưu. Tiếp tục?')) { setInput(original ? inputOf(original) : blank()); setError(''); setNotice(''); } }}>Làm mới</button><button className="pw-secondary" name="intent" value="draft" type="submit" disabled={disabled}>Lưu bản nháp doanh nghiệp</button><button className="pw-primary" name="intent" value="review" type="submit" disabled={disabled}>{busy ? 'Đang lưu…' : 'Xem lại thông tin'}</button></div></div>
      {original && <div className="se-secondary-actions"><button className="sd-link" type="button" disabled={busy || uncertain} onClick={() => { if (!dirty || window.confirm('Tải bản mới sẽ thay nội dung chưa lưu. Tiếp tục?')) { setRevision(v => v + 1); setError(''); setNotice(''); } }}>Tải bản mới nhất</button>{original.allowedActions.includes('ARCHIVE') && <button className="sd-link" type="button" disabled={busy || uncertain} onClick={() => { void archive(); }}>Lưu trữ bản nháp</button>}</div>}
    </form>}
  </section>;
}
