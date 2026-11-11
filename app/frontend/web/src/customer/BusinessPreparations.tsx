import { useEffect, useRef, useState } from 'react';
import type { FormEvent } from 'react';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import { customerClient, CustomerApiError } from './client';
import type { BusinessPreparation, BusinessPreparationInput, BusinessPreparationOperation, BusinessPreparationPage, DraftContact } from './client';
import type { BusinessContext } from './business-data';
import { centimetresToMillimetres, emptyParcel } from './shipping-entry';
import './business-preparations.css';

const contact = (): DraftContact => ({ name: '', phone: '', address: { country: 'VN', administrativeArea: '', locality: '', addressLine: '' } });
const blank = (): BusinessPreparationInput => ({ sender: contact(), recipient: contact(), parcels: [emptyParcel()] });
const inputOf = (d: BusinessPreparation): BusinessPreparationInput => ({ ...structuredClone(d.input), facilityId: d.facilityId, sourceChannel: d.sourceChannel, sourceOrderReference: d.sourceOrderReference });
function message(error: unknown) {
  const code = error instanceof CustomerApiError ? error.code : '';
  return ({ RESOURCE_NOT_FOUND: 'Không có quyền truy cập bản nháp hoặc kho này.', BUSINESS_CONTEXT_UNAVAILABLE: 'Quyền doanh nghiệp đã thay đổi. Hãy tải lại không gian làm việc.', PRECONDITION_STALE: 'Bản nháp đã thay đổi. Nội dung đang nhập được giữ lại; tải bản mới trước khi lưu tiếp.', IDEMPOTENCY_CONFLICT: 'Mã thao tác đã được dùng cho nội dung khác.', SOURCE_REFERENCE_CONFLICT: 'Nguồn và mã đơn này đã có bản nháp trong doanh nghiệp.', SOURCE_REFERENCE_IMMUTABLE: 'Nguồn đơn đã được gắn cố định khi tạo bản nháp.', VALIDATION_FAILED: 'Kiểm tra liên hệ, các kiện và cặp nguồn / mã đơn.', OUTCOME_UNKNOWN: 'Chưa xác định kết quả. Kiểm tra kết quả trước khi thực hiện thao tác khác.' } as Record<string, string>)[code] ?? 'Chưa hoàn tất yêu cầu. Hãy kiểm tra kết nối và thử lại.';
}
function ContactFields({ kind, value, change }: { kind: 'sender' | 'recipient'; value: DraftContact; change: (v: DraftContact) => void }) {
  const title = kind === 'sender' ? 'Người gửi' : 'Người nhận';
  return <section className="bw-panel"><header><h2>{title}</h2></header><div className="bp-fields">
    <label>Họ tên {title.toLowerCase()}<input required maxLength={160} autoComplete="off" value={value.name} onChange={e => change({ ...value, name: e.target.value })} /></label>
    <label>Số điện thoại {title.toLowerCase()}<input required type="tel" maxLength={32} placeholder="0… hoặc +84…" pattern="(0|84|\+84)[35789][0-9]{8}" value={value.phone} onChange={e => change({ ...value, phone: e.target.value })} /></label>
    {(['addressLine', 'administrativeArea', 'locality'] as const).map((field, i) => <label className={i === 0 ? 'bp-full' : ''} key={field}>{['Địa chỉ chi tiết', 'Tỉnh / thành phố', 'Phường / xã'][i]} ({title.toLowerCase()})<input required maxLength={i === 0 ? 500 : 160} value={value.address[field]} onChange={e => change({ ...value, address: { ...value.address, [field]: e.target.value } })} /></label>)}
  </div></section>;
}
export function BusinessPreparationList({ context }: { context: BusinessContext }) {
  const [page, setPage] = useState(0), [revision, setRevision] = useState(0), [result, setResult] = useState<{ key: string; data?: BusinessPreparationPage; error?: string }>();
  const key = context.organizationId + ':' + context.authorityVersion + ':' + page + ':' + revision, base = '/merchant/' + context.organizationId;
  useEffect(() => { let active = true; void customerClient.listBusinessPreparations(context, page).then(data => { if (active) setResult({ key, data }); }).catch(e => { if (active) setResult({ key, error: message(e) }); }); return () => { active = false; }; }, [context, key, page]);
  const current = result?.key === key ? result : undefined;
  return <section className="bp-workspace"><div className="bw-heading"><div><span className="pw-eyebrow">{context.displayName}</span><h1>Đơn nháp doanh nghiệp</h1><p>Thông tin chuẩn bị trước khi xác nhận vận đơn.</p></div>{context.permissions.includes('shipment.draft.write') && <Link className="pw-primary" to={base + '/create'}>Tạo bản nháp</Link>}</div>
    {!current ? <p role="status">Đang kiểm tra quyền và tải bản nháp…</p> : current.error ? <div className="bw-panel bp-empty"><p role="alert">{current.error}</p><button className="pw-secondary" onClick={() => setRevision(v => v + 1)}>Thử lại</button></div> : current.data && <>
      {current.data.items.length ? <div className="bw-panel bp-list"><table><thead><tr><th>Mã tham chiếu</th><th>Người gửi / người nhận</th><th>Kiện</th><th>Cập nhật</th><th>Thao tác</th></tr></thead><tbody>{current.data.items.map(d => <tr key={d.id}><td>{d.input.reference ?? d.sourceOrderReference ?? 'Bản nháp'}<small>Chưa Booking</small></td><td>{d.input.sender.name}<small>{d.input.recipient.name}</small></td><td>{d.input.parcels.length}</td><td>{new Date(d.updatedAt).toLocaleString('vi-VN')}</td><td><Link to={base + '/create?draft=' + d.id}>Mở bản nháp</Link></td></tr>)}</tbody></table></div> : <div className="bw-panel bp-empty"><h2>Chưa có bản nháp</h2><p>Bản nháp của doanh nghiệp sẽ xuất hiện tại đây.</p></div>}
      <nav className="ab-pagination" aria-label="Phân trang bản nháp doanh nghiệp"><button className="pw-secondary" disabled={page === 0} onClick={() => setPage(v => v - 1)}>Trang trước</button><span>Trang {page + 1}</span><button className="pw-secondary" disabled={!current.data.hasNext} onClick={() => setPage(v => v + 1)}>Trang sau</button></nav>
    </>}
  </section>;
}
export function BusinessPreparationEditor({ context }: { context: BusinessContext }) {
  const [params] = useSearchParams(), id = params.get('draft'), navigate = useNavigate();
  const [input, setInput] = useState<BusinessPreparationInput>(blank), [original, setOriginal] = useState<BusinessPreparation>(), [loaded, setLoaded] = useState(!id), [loadError, setLoadError] = useState(''), [error, setError] = useState(''), [notice, setNotice] = useState(''), [busy, setBusy] = useState(false), [uncertain, setUncertain] = useState(false), [revision, setRevision] = useState(0);
  const pending = useRef<{ operation: BusinessPreparationOperation; key: string; target?: string } | undefined>(undefined), running = useRef(false), live = useRef(true);
  const base = '/merchant/' + context.organizationId, dirty = JSON.stringify(input) !== JSON.stringify(original ? inputOf(original) : blank());
  useEffect(() => { live.current = true; return () => { live.current = false; }; }, []);
  useEffect(() => { if (!dirty && !uncertain) return; const warn = (e: BeforeUnloadEvent) => { e.preventDefault(); }; window.addEventListener('beforeunload', warn); return () => window.removeEventListener('beforeunload', warn); }, [dirty, uncertain]);
  useEffect(() => { let active = true; if (!id) return;
    void customerClient.readBusinessPreparation(context, id).then(d => { if (active) { setOriginal(d); setInput(inputOf(d)); setLoaded(true); setLoadError(''); } }).catch(e => { if (active) { setLoadError(message(e)); setLoaded(true); } }); return () => { active = false; };
  }, [context, id, revision]);
  function accept(d: BusinessPreparation) {
    pending.current = undefined; setUncertain(false); setOriginal(d); setInput(inputOf(d)); setError('');
    if (d.state === 'ARCHIVED') navigate(base + '/preparations', { replace: true });
    else { setNotice('Đã lưu bản nháp doanh nghiệp. Chưa tạo Shipment hoặc nghĩa vụ cước.'); if (id !== d.id) navigate(base + '/create?draft=' + d.id, { replace: true }); }
  }
  async function save(event: FormEvent) {
    event.preventDefault(); if (running.current || uncertain) return; running.current = true; setBusy(true); setError(''); setNotice('');
    pending.current = { operation: original ? 'UPDATE' : 'CREATE', key: crypto.randomUUID(), target: original?.id };
    try { const d = await customerClient.saveBusinessPreparation(context, input, pending.current.key, original); if (live.current) accept(d); }
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
    try { const request = pending.current, r = await customerClient.businessPreparationReceipt(context, request.operation, request.key); if (request.target && r.targetId !== request.target) throw new Error(); const d = await customerClient.readBusinessPreparation(context, r.targetId); if (live.current) accept(d); }
    catch { if (live.current) setError('Chưa xác nhận được kết quả. Giữ trang này và kiểm tra lại; không gửi thao tác mới.'); }
    finally { running.current = false; if (live.current) setBusy(false); }
  }
  const editable = original ? original.allowedActions.includes('UPDATE') : context.permissions.includes('shipment.draft.write');
  return <section className="bp-workspace"><div className="bw-heading"><div><span className="pw-eyebrow">{context.displayName}</span><h1>{original ? 'Chi tiết bản nháp' : 'Tạo đơn doanh nghiệp'}</h1><p>Lưu thông tin người gửi, người nhận và từng kiện hàng.</p></div><Link className="pw-secondary" to={base + '/preparations'} onClick={e => { if ((dirty || uncertain) && !window.confirm('Rời trang sẽ bỏ nội dung chưa lưu. Tiếp tục?')) e.preventDefault(); }}>Danh sách bản nháp</Link></div>
    {!loaded ? <p role="status">Đang tải bản nháp…</p> : loadError ? <p className="cp-notice cp-error" role="alert">{loadError}</p> : <form onSubmit={event => { void save(event); }}>
      {error && <p role="alert" className="cp-notice cp-error">{error}</p>}{notice && <p role="status" className="cp-notice">{notice}</p>}{uncertain && <button type="button" className="pw-secondary" disabled={busy} onClick={() => { void resolve(); }}>Kiểm tra kết quả lưu</button>}{!editable && !original && <p role="status">Tài khoản chưa có quyền tạo bản nháp doanh nghiệp.</p>}
      <fieldset disabled={busy || uncertain || !editable} className="bp-editor"><div className="bp-two-columns"><ContactFields kind="sender" value={input.sender} change={sender => setInput(v => ({ ...v, sender }))} /><ContactFields kind="recipient" value={input.recipient} change={recipient => setInput(v => ({ ...v, recipient }))} /></div>
        <section className="bw-panel"><header><h2>Kiện hàng</h2><span>{input.parcels.length} / 5 kiện</span></header><div className="bp-parcels">{input.parcels.map((p, i) => <div className="bp-parcel" key={i}><div className="bp-parcel-title"><h3>Kiện {i + 1}</h3><button type="button" disabled={input.parcels.length === 1} onClick={() => setInput(v => ({ ...v, parcels: v.parcels.filter((_, n) => n !== i) }))}>Bỏ kiện {i + 1}</button></div><label>Mô tả kiện {i + 1}<input required maxLength={160} value={p.description} onChange={e => setInput(v => ({ ...v, parcels: v.parcels.map((item, n) => n === i ? { ...item, description: e.target.value } : item) }))} /></label><div className="bp-measurements">{(['quantity', 'weightGrams', 'declaredValueVnd', 'lengthMm', 'widthMm', 'heightMm'] as const).map((field, index) => {
          const cm = index >= 3, value = p[field]; return <label key={field}>{['Số lượng hàng', 'Trọng lượng (g)', 'Giá trị khai báo (₫)', 'Dài (cm)', 'Rộng (cm)', 'Cao (cm)'][index]} — kiện {i + 1}<input type="number" required min={cm ? .1 : field === 'declaredValueVnd' ? 0 : 1} max={cm ? 100000 : field === 'declaredValueVnd' ? 1e12 : field === 'weightGrams' ? 1e9 : 1e6} step={cm ? .1 : 1} value={Number.isNaN(value) ? '' : cm ? value / 10 : value} onChange={e => { const next = cm ? centimetresToMillimetres(e.target.value) : e.target.value === '' ? NaN : Number(e.target.value); setInput(v => ({ ...v, parcels: v.parcels.map((item, n) => n === i ? { ...item, [field]: next } : item) })); }} /></label>;
        })}</div></div>)}<button type="button" className="pw-secondary" disabled={input.parcels.length >= 5} onClick={() => setInput(v => ({ ...v, parcels: [...v.parcels, emptyParcel()] }))}>Thêm kiện</button><p className="se-hint">Tuyến, giới hạn dịch vụ và cước được kiểm tra riêng khi báo giá.</p></div></section>
        <section className="bw-panel"><header><h2>Nguồn đơn & ghi chú</h2></header><div className="bp-fields"><label>Nguồn đơn<input maxLength={40} disabled={!!original} placeholder="Điền cùng mã đơn nguồn hoặc để trống cả hai" value={input.sourceChannel ?? ''} onChange={e => setInput(v => ({ ...v, sourceChannel: e.target.value || null }))} /></label><label>Mã đơn nguồn<input maxLength={80} disabled={!!original} value={input.sourceOrderReference ?? ''} onChange={e => setInput(v => ({ ...v, sourceOrderReference: e.target.value || null }))} /></label><label className="bp-full">Mã tham chiếu<input maxLength={80} value={input.reference ?? ''} onChange={e => setInput(v => ({ ...v, reference: e.target.value || null }))} /></label><label className="bp-full">Ghi chú<textarea maxLength={1000} rows={3} value={input.instructions ?? ''} onChange={e => setInput(v => ({ ...v, instructions: e.target.value || null }))} /></label></div></section>
      </fieldset><div className="bp-actions">{original && <button className="pw-secondary" type="button" disabled={busy || uncertain} onClick={() => { if (!dirty || window.confirm('Tải bản mới sẽ thay nội dung chưa lưu. Tiếp tục?')) { setRevision(v => v + 1); setError(''); setNotice(''); } }}>Tải bản mới nhất</button>}{original?.allowedActions.includes('ARCHIVE') && <button className="pw-secondary" type="button" disabled={busy || uncertain} onClick={() => { void archive(); }}>Lưu trữ bản nháp</button>}{editable && <button className="pw-primary" disabled={busy || uncertain}>{busy ? 'Đang lưu…' : 'Lưu bản nháp doanh nghiệp'}</button>}</div>
    </form>}
  </section>;
}
