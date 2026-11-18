import { useEffect, useRef, useState } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { customerClient } from './client';
import type { BusinessContext } from './business-data';
import type { OrganizationManagementItem, OrganizationManagementPage } from './business-management-data';
import { businessLookupInputs, lookupBusinessShipment } from './business-shipping-tools';
import type { LookupKind } from './business-shipping-tools';
import { OrganizationFilters } from './OrganizationManagement';
import { OrganizationDownloads } from './OrganizationDownloads';
import { organizationQuery } from './organization-management';
import { BusinessQuotation, BusinessServiceTools } from './BusinessQuotation';
import { csvCell } from './shipment-management';
import './business-shipping-tools.css';

function useLive() {
  const state = useRef({ active: true, generation: 0 });
  useEffect(() => { const s = state.current; s.active = true; return () => { s.active = false; s.generation++; }; }, []);
  return state;
}
function save(blob: Blob, name: string) { const url = URL.createObjectURL(blob), a = document.createElement('a'); a.href = url; a.download = name; a.click(); setTimeout(() => URL.revokeObjectURL(url), 1000); }

export function BusinessShippingTools({ context, labels = false }: { context: BusinessContext; labels?: boolean }) {
  const [params, setParams] = useSearchParams();
  const views = labels ? { labels: 'Nhãn vận chuyển', packing: 'Chuẩn bị & Bàn giao', calculator: 'Trọng lượng & Đóng gói' } : { tracking: 'Tra cứu vận đơn', quote: 'Báo giá doanh nghiệp', services: 'Dịch vụ & Vùng phục vụ' };
  const view = params.get('view') ?? Object.keys(views)[0], valid = Object.hasOwn(views, view) && params.getAll('view').length <= 1;
  return <section className="bst-page"><header className="bw-heading"><div><span className="pw-eyebrow">{context.displayName}</span><h1 tabIndex={-1}>{labels ? 'In nhãn & Tiện ích' : 'Tra cứu & Báo giá'}</h1><p>{labels ? 'Nhãn và công cụ chuẩn bị từ vận đơn đã xác nhận.' : 'Vận đơn và báo giá thuộc doanh nghiệp của bạn.'}</p></div></header>
    <nav className="st-tabs" aria-label="Công cụ doanh nghiệp">{Object.entries(views).map(([id, title]) => <button type="button" key={id} aria-current={view === id ? 'page' : undefined} onClick={() => setParams(id === Object.keys(views)[0] ? {} : { view: id })}>{title}</button>)}</nav>
    {!valid ? <p role="alert">Mục công cụ không hợp lệ. Chọn lại nội dung phía trên.</p> : view === 'tracking' ? <BusinessTracking key={context.authorityVersion} context={context} /> : view === 'quote' ? <BusinessQuotation context={context} /> : view === 'labels' ? <BusinessLabelLibrary context={context} /> : view === 'packing' ? <BusinessPacking context={context} /> : <BusinessServiceTools context={context} calculator={view === 'calculator'} />}
  </section>;
}
function BusinessTracking({ context }: { context: BusinessContext }) {
  const [kind, setKind] = useState<LookupKind>('SHIPMENT'), [text, setText] = useState(''), [busy, setBusy] = useState(false), [error, setError] = useState('');
  const [rows, setRows] = useState<{ value: string; items?: OrganizationManagementItem[]; unknown?: boolean }[]>();
  const [exporting, setExporting] = useState(false), live = useLive();
  async function lookup() {
    if (busy || exporting) return;
    setError(''); setRows(undefined); let inputs: string[];
    try { inputs = businessLookupInputs(text, kind); } catch (e) { setError((e as Error).message); return; }
    const generation = ++live.current.generation, identity = customerClient.getSnapshot(), current = () => live.current.active && live.current.generation === generation && customerClient.getSnapshot() === identity;
    setBusy(true); const result: NonNullable<typeof rows> = [];
    for (const value of inputs) { if (!current()) break; try { result.push({ value, items: await lookupBusinessShipment(value, kind, q => customerClient.readOrganizationManagement(context, q), current) }); } catch { result.push({ value, unknown: true }); } }
    if (current()) { setRows(result); setBusy(false); } else if (live.current.active) { setRows(undefined); setBusy(false); }
  }
  const items = [...new Map(rows?.flatMap(r => r.items ?? []).map(i => [i.record.shipment.id, i]) ?? []).values()];
  const canExport = !!rows?.length && rows.every(r => !r.unknown) && items.length > 0 && items.length <= 20 && items.every(i => i.allowedActions.includes('EXPORT'));
  async function exportResults() {
    if (!canExport || exporting || busy) return;
    const generation = live.current.generation, identity = customerClient.getSnapshot(); setExporting(true); setError('');
    try { const result = await customerClient.exportOrganizationShipments(context, new URLSearchParams(), false, items.map(i => i.record.shipment.id));
      if (live.current.active && live.current.generation === generation && customerClient.getSnapshot() === identity) save(new Blob(['\uFEFF' + [['ARGOPOST LOCAL/TEST SIMULATION'], ['Doanh nghiệp', context.displayName], ['Thời điểm', result.generatedAt], result.columns, ...result.rows].map(r => r.map(csvCell).join(',')).join('\r\n')], { type: 'text/csv;charset=utf-8' }), 'argopost-tracking-' + result.exportId + '.csv');
    } catch { if (live.current.active) setError('Chưa thể xuất kết quả. Cần quyền xuất cho mọi vận đơn và audit thành công.'); }
    finally { if (live.current.active) setExporting(false); }
  }
  return <><form className="bw-panel bst-search" onSubmit={e => { e.preventDefault(); void lookup(); }}><label>Tra cứu theo<select aria-label="Tra cứu theo" value={kind} disabled={busy || exporting} onChange={e => { setKind(e.target.value as LookupKind); setRows(undefined); setError(''); }}><option value="SHIPMENT">Mã vận đơn</option><option value="PARCEL">Mã kiện hàng</option><option value="REFERENCE">Mã tham chiếu / đơn nguồn</option></select></label><label>Danh sách mã<textarea value={text} maxLength={2048} rows={3} onChange={e => { setText(e.target.value); setRows(undefined); }} disabled={busy || exporting} placeholder="Tối đa 10 mã. Mỗi tham chiếu đặt trên một dòng." required /></label><button className="pw-primary" disabled={busy || exporting}>{busy ? 'Đang tra cứu…' : 'Tra cứu'}</button></form>
    {error && <p role="alert">{error}</p>}{rows && <div className="bst-results"><header><h2>Kết quả tra cứu</h2><button type="button" className="pw-secondary" disabled={!canExport || exporting} onClick={() => { void exportResults(); }}>Xuất kết quả CSV</button></header>{items.length > 20 && <p>Hơn 20 vận đơn phù hợp. Thu hẹp tham chiếu hoặc xuất trong Quản lý vận đơn.</p>}{rows.map(r => <section className="bw-panel bst-result" key={r.value}><h3><code>{r.value}</code></h3>{r.unknown ? <p role="alert">Chưa xác định kết quả: quyền, dữ liệu, kết nối hoặc giới hạn tra cứu đã thay đổi. Thử lại với mã cụ thể.</p> : !r.items?.length ? <p>Không có kết quả khớp chính xác trong phạm vi quyền hiện hành.</p> : r.items.map(item => { const s = item.record.shipment; return <div className="bst-match" key={s.id}><div><strong>{s.code}</strong><span>Chờ bàn giao · Carrier chưa tiếp nhận</span><small>{s.parcels.length} kiện · {s.quote.route.originCode} → {s.quote.route.destinationCode}</small></div><Link className="pw-secondary" to={'/merchant/' + context.organizationId + '/shipments/' + s.id + '?tab=tracking&returnTo=' + encodeURIComponent('/merchant/' + context.organizationId + '/lookup')}>Xem hành trình</Link></div>; })}</section>)}</div>}</>;
}

function BusinessLabelPreview({ context, id, close }: { context: BusinessContext; id: string; close: () => void }) {
  const [url, setUrl] = useState(''), [error, setError] = useState(false);
  useEffect(() => { let active = true, owned = ''; const identity = customerClient.getSnapshot();
    void customerClient.downloadBusinessLabel(context, id).then(blob => { if (active && customerClient.getSnapshot() === identity) { owned = URL.createObjectURL(blob); setUrl(owned); } }).catch(() => { if (active) setError(true); });
    const unsubscribe = customerClient.subscribe(() => { if (customerClient.getSnapshot() !== identity) { active = false; if (owned) URL.revokeObjectURL(owned); setUrl(''); setError(true); } });
    return () => { active = false; unsubscribe(); if (owned) URL.revokeObjectURL(owned); };
  }, [context, id]);
  return <section className="bw-panel bst-preview" aria-label="Bản nhãn riêng tư"><header><h2>Xem trước nhãn · 100 × 150 mm</h2><button type="button" className="pw-secondary" onClick={close}>Đóng bản nhãn</button></header>{error ? <p role="alert">Không tải được nhãn hoặc phiên đã thay đổi. Chưa có bản in.</p> : !url ? <p role="status">Đang kiểm tra quyền và tải PDF…</p> : <><div className="bst-actions"><a className="pw-primary" href={url} download={'argopost-' + id + '.pdf'}>Lưu PDF</a><a className="pw-secondary" href={url} target="_blank" rel="noopener noreferrer">Mở PDF để in</a></div><object data={url} type="application/pdf" aria-label="PDF nhãn đã được cấp quyền"><p>Mở PDF để xem và in bằng trình đọc của trình duyệt.</p></object></>}<p className="se-hint">Chọn 100% / kích thước thực, không cắt barcode. Một kiện có thể có trang tiếp nối; in đủ các trang của kiện. Chưa hỗ trợ khổ khác, QR hoặc kết nối máy in trực tiếp.</p></section>;
}
function BusinessLabelLibrary({ context }: { context: BusinessContext }) {
  const [raw, setRaw] = useSearchParams(), copy = new URLSearchParams(raw); copy.delete('view'); const q = organizationQuery(copy);
  const [revision, setRevision] = useState(0), [result, setResult] = useState<{ key: string; data?: OrganizationManagementPage; error?: boolean }>();
  const [selection, setSelection] = useState<{ key: string; ids: string[] }>({ key: '', ids: [] }), [preview, setPreview] = useState<{ key: string; id: string }>();
  const key = context.organizationId + ':' + context.authorityVersion + ':' + q.api + ':' + revision, apiQuery = q.api.toString();
  useEffect(() => { if (!q.valid || q.category !== 'ALL') return; let active = true; void customerClient.readOrganizationManagement(context, new URLSearchParams(apiQuery)).then(data => { if (active) setResult({ key, data }); }).catch(() => { if (active) setResult({ key, error: true }); }); return () => { active = false; }; }, [context, key, apiQuery, q.valid, q.category]);
  const current = result?.key === key ? result : undefined, data = current?.data, selected = selection.key === key ? selection.ids : [];
  function apply(p: URLSearchParams) { p.delete('page'); setRaw(p); }
  return <section className="om-page"><div className="bst-section-heading"><h2>Chọn vận đơn để in nhãn</h2><button type="button" className="pw-secondary" onClick={() => setRevision(v => v + 1)}>Làm mới</button></div><section className="om-list-panel"><OrganizationFilters key={q.query.toString()} query={q.api} data={data} apply={apply} />{!q.valid || q.category !== 'ALL' ? <p role="alert">Bộ lọc không hợp lệ. Đặt lại để chọn nhãn.</p> : !current ? <p role="status">Đang tải danh sách nhãn…</p> : current.error ? <p role="alert">Chưa tải được vận đơn hoặc quyền đã thay đổi. Không dùng lại danh sách cũ.</p> : data && <>
      <p>{data.total} vận đơn phù hợp · Chọn tối đa 20 vận đơn trên trang. Chỉ nhãn có quyền hiện hành mới được tải.</p><OrganizationDownloads key={key + data.generatedAt} context={context} data={data} query={q.api} selected={selected} />
      {data.items.length ? <div className="om-table-scroll"><table className="om-table"><thead><tr><th><input type="checkbox" aria-label="Chọn nhãn trên trang" checked={selected.length > 0 && selected.length === data.items.filter(i => i.allowedActions.includes('READ_LABEL')).length} onChange={e => setSelection({ key, ids: e.target.checked ? data.items.filter(i => i.allowedActions.includes('READ_LABEL')).map(i => i.record.shipment.id) : [] })} /></th><th>Vận đơn</th><th>Kho / đợt nhập</th><th>Kiện</th><th>Nhãn</th></tr></thead><tbody>{data.items.map(item => { const s = item.record.shipment, allowed = item.allowedActions.includes('READ_LABEL'); return <tr key={s.id}><td><input type="checkbox" aria-label={'Chọn nhãn ' + s.code} disabled={!allowed} checked={selected.includes(s.id)} onChange={e => setSelection({ key, ids: e.target.checked ? [...selected, s.id] : selected.filter(id => id !== s.id) })} /></td><td><strong>{s.code}</strong><small>Chờ bàn giao</small></td><td>{s.facilityId?.slice(0, 8) ?? 'Không gắn kho'}<small>{item.source.importJobIds.length ? item.source.importJobIds.map(id => id.slice(0, 8)).join(', ') : 'Không có đợt nhập'}</small></td><td>{s.parcels.length}</td><td><button type="button" className="pw-secondary" disabled={!allowed} onClick={() => setPreview({ key, id: s.id })}>Xem / In PDF</button><Link to={'/merchant/' + context.organizationId + '/shipments/' + s.id + '?tab=parcels'}>Kiện & mã barcode</Link></td></tr>; })}</tbody></table></div> : <p>Chưa có vận đơn phù hợp. Nhãn chỉ có sau khi Booking thành công.</p>}
      <nav className="ab-pagination" aria-label="Phân trang nhãn"><button className="pw-secondary" disabled={q.page === 0} onClick={() => { const p = new URLSearchParams(q.api); p.set('page', String(q.page - 1)); setRaw(p); }}>Trang trước</button><span>Trang {q.page + 1}</span><button className="pw-secondary" disabled={!data.hasNext} onClick={() => { const p = new URLSearchParams(q.api); p.set('page', String(q.page + 1)); setRaw(p); }}>Trang sau</button></nav></>}</section>{preview?.key === key && <BusinessLabelPreview key={key + preview.id} context={context} id={preview.id} close={() => setPreview(undefined)} />}</section>;
}
function BusinessPacking({ context }: { context: BusinessContext }) {
  return <div className="bst-catalog-grid"><section className="bw-panel bst-card"><h2>Đóng gói & Đối chiếu kiện</h2><ol><li>Đối chiếu tên hàng, số lượng, trọng lượng và kích thước từng Parcel đã Booking.</li><li>Dùng bao bì phù hợp, chèn chống va đập; không che hoặc gấp barcode.</li><li>Dán đúng nhãn cho từng mã kiện; không dùng nhãn của bản nháp hoặc vận đơn khác.</li><li>Kiểm tra đủ kiện trước khi chuyển sẵn sàng bàn giao.</li></ol><Link className="pw-primary" to={'/merchant/' + context.organizationId + '/facilities'}>Mở công việc tại kho</Link></section><section className="bw-panel bst-card"><h2>Danh sách bàn giao nội bộ</h2><p>Tạo Packing List và đối chiếu Parcel thật trong Kho & Điểm lấy hàng, theo quyền chuẩn bị / bàn giao tại địa điểm.</p><p>Danh sách nội bộ không phải biên nhận Carrier. Chưa có nguồn Carrier Acceptance Business, POD hoặc chứng từ giao thành công để tải.</p><Link className="pw-secondary" to={'/merchant/' + context.organizationId + '/shipments'}>Chọn vận đơn đã xác nhận</Link></section></div>;
}
