import { useEffect, useRef, useState } from 'react';
import { Link } from 'react-router-dom';
import { customerClient, CustomerApiError } from './client';
import type { BusinessCatalog, BusinessPreparation, BusinessPreparationPage, BusinessQuoteView } from './client';
import type { BusinessContext } from './business-data';
import { parcelServiceIssues } from './shipping-entry';
import { dimensionalGrams } from './shipping-tools-data';
import { pilotMoney } from './PilotShipping';
import { csvCell } from './shipment-management';
import { quoteMatchesDraft, remainingBusinessQuoteSeconds } from './business-shipping-tools';

type QuoteRow = { draft: BusinessPreparation; state: 'IDLE' | 'RUNNING' | 'QUOTED' | 'FAILED' | 'UNKNOWN'; key?: string; quote?: BusinessQuoteView; deadline?: number; error?: string };
function errorText(e: unknown) { const code = e instanceof CustomerApiError ? e.code : ''; return ({ RESOURCE_NOT_FOUND: 'Không còn quyền tại doanh nghiệp / kho.', PRECONDITION_STALE: 'Bản nháp đã thay đổi. Tải lại và chọn phiên bản mới.', PARCEL_NOT_ELIGIBLE: 'Kiện vượt giới hạn của dịch vụ.', UNSUPPORTED_ROUTE: 'Tuyến chưa được hỗ trợ.', PILOT_DISABLED: 'Không có hợp đồng giá hiện hành hoặc khả năng báo giá.', VALIDATION_FAILED: 'Thông tin báo giá chưa hợp lệ.' } as Record<string, string>)[code] ?? 'Chưa hoàn tất báo giá. Kiểm tra dữ liệu và quyền hiện hành.'; }
export function BusinessQuotation({ context }: { context: BusinessContext }) {
  const [catalog, setCatalog] = useState<BusinessCatalog>(), [catalogError, setCatalogError] = useState(false), [page, setPage] = useState(0), [revision, setRevision] = useState(0);
  const [source, setSource] = useState<{ key: string; data?: BusinessPreparationPage; error?: boolean }>(), [rows, setRows] = useState<QuoteRow[]>([]);
  const [route, setRoute] = useState(''), [ordinary, setOrdinary] = useState(false), [busy, setBusy] = useState(false), [error, setError] = useState(''), [now, setNow] = useState(() => performance.now());
  const live = useRef({ active: true, running: false }), key = context.organizationId + ':' + context.authorityVersion + ':' + page + ':' + revision;
  useEffect(() => { const s = live.current; s.active = true; return () => { s.active = false; }; }, []);
  useEffect(() => { let active = true; void customerClient.readBusinessCatalog(context).then(c => { if (active) { setCatalog(c); setRoute(c.coverage.routes[0] ? c.coverage.routes[0].originCode + '/' + c.coverage.routes[0].destinationCode : ''); } }).catch(() => { if (active) setCatalogError(true); }); return () => { active = false; }; }, [context, revision]);
  useEffect(() => { let active = true; void customerClient.listBusinessPreparations(context, page).then(data => { if (active) setSource({ key, data }); }).catch(() => { if (active) setSource({ key, error: true }); }); return () => { active = false; }; }, [context, page, key]);
  useEffect(() => { const timer = setInterval(() => setNow(performance.now()), 1000); return () => clearInterval(timer); }, []);
  const unknown = rows.some(r => r.state === 'UNKNOWN');
  useEffect(() => { if (!unknown && !busy) return; const warn = (e: BeforeUnloadEvent) => { e.preventDefault(); }; window.addEventListener('beforeunload', warn); return () => window.removeEventListener('beforeunload', warn); }, [unknown, busy]);
  const current = source?.key === key ? source : undefined;
  function update(id: string, patch: Partial<QuoteRow>) { if (live.current.active) setRows(rs => rs.map(r => r.draft.id === id ? { ...r, ...patch } : r)); }
  function accept(row: QuoteRow, quote: BusinessQuoteView) {
    if (!quoteMatchesDraft(quote, row.draft)) throw new CustomerApiError('RESPONSE_INVALID');
    const received = performance.now(); setNow(received); update(row.draft.id, { state: 'QUOTED', quote, deadline: received + Date.parse(quote.quote.expiresAt) - Date.parse(quote.serverTime), key: undefined, error: undefined });
  }
  async function price() {
    if (live.current.running || unknown || !rows.length || !catalog?.quoteEnabled || !catalog.tariff || !ordinary || !catalog.coverage.routes.some(r => r.originCode + '/' + r.destinationCode === route)) return;
    const identity = customerClient.getSnapshot(), pending = rows.filter(r => r.state === 'IDLE' || r.state === 'FAILED'); if (!pending.length) return;
    live.current.running = true; setBusy(true); setError('');
    try { for (const row of pending) {
      if (!live.current.active || customerClient.getSnapshot() !== identity) break;
      let sent = false; const requestKey = crypto.randomUUID(); update(row.draft.id, { state: 'RUNNING', error: undefined, key: requestKey });
      try {
        const fresh = await customerClient.readBusinessPreparation(context, row.draft.id);
        if (fresh.version !== row.draft.version || fresh.state !== 'DRAFT' || fresh.facilityId !== row.draft.facilityId) throw new CustomerApiError('PRECONDITION_STALE');
        if (parcelServiceIssues(fresh.input.parcels, catalog.tariff).length) throw new CustomerApiError('PARCEL_NOT_ELIGIBLE');
        if (!live.current.active || customerClient.getSnapshot() !== identity) break;
        const [origin, destination] = route.split('/'); sent = true;
        const quote = await customerClient.createBusinessQuote(context, fresh, origin, destination, true, requestKey);
        if (!live.current.active || customerClient.getSnapshot() !== identity) break;
        accept(row, quote);
      } catch (e) {
        if (customerClient.getSnapshot() !== identity) { if (live.current.active) setRows([]); break; }
        const uncertain = sent && (!(e instanceof CustomerApiError) || e.code === 'OUTCOME_UNKNOWN' || e.code === 'RESPONSE_INVALID' || e.status >= 500);
        update(row.draft.id, { state: uncertain ? 'UNKNOWN' : 'FAILED', error: uncertain ? 'Chưa rõ kết quả ghi. Kiểm tra receipt trước khi tiếp tục.' : errorText(e), key: uncertain ? requestKey : undefined });
        if (uncertain) break;
      }
    } } finally { live.current.running = false; if (live.current.active) setBusy(false); }
  }
  async function resolve(row: QuoteRow) {
    if (live.current.running || row.state !== 'UNKNOWN' || !row.key) return;
    const identity = customerClient.getSnapshot(); live.current.running = true; setBusy(true);
    try { const receipt = await customerClient.businessShippingReceipt(context, 'quote', row.key), quote = await customerClient.readBusinessQuote(context, receipt.targetId); if (live.current.active && customerClient.getSnapshot() === identity) accept(row, quote); }
    catch { if (live.current.active) setError('Chưa xác định được báo giá đã ghi. Giữ mã thao tác hiện tại; chưa gửi lại POST.'); }
    finally { live.current.running = false; if (live.current.active) setBusy(false); }
  }
  const quotations = rows.filter(r => r.state === 'QUOTED' && r.quote), canExport = quotations.length > 0 && !unknown && !busy && context.permissions.includes('shipment.export');
  async function exportQuotes() {
    if (!canExport) return;
    if (live.current.running) return;
    const identity = customerClient.getSnapshot(); live.current.running = true; setBusy(true); setError('');
    try {
      const contexts = await customerClient.businessContexts(), current = contexts.find(c => c.organizationId === context.organizationId);
      if (!current?.permissions.includes('shipment.export')) throw new Error('EXPORT_DENIED');
      // Re-read every Quote through current Backend scope; export no contact/reference fields.
      const verified: BusinessQuoteView[] = [];
      for (const row of quotations) {
        if (!live.current.active || customerClient.getSnapshot() !== identity) return;
        const value = await customerClient.readBusinessQuote(current, row.quote!.quote.id);
        if (!quoteMatchesDraft(value, row.draft)) throw new Error('QUOTE_CHANGED');
        verified.push(value);
      }
      if (!live.current.active || customerClient.getSnapshot() !== identity) return;
      const lines = [['ARGOPOST LOCAL/TEST SIMULATION — Báo giá, không phải chứng từ thanh toán'], ['Doanh nghiệp', context.displayName], ['Mã báo giá', 'Mã bản nháp', 'Hợp đồng v', 'Biểu phí', 'Tuyến', 'Tổng VND', 'Hết hạn', 'Trạng thái tại lúc đọc'], ...verified.map(v => { const q = v.quote; return [q.id, q.preparationId, String(q.contractVersion), q.tariffVersion, q.route.originCode + '/' + q.route.destinationCode, String(q.totalVnd), q.expiresAt, v.usable ? 'Cần kiểm tra lại khi Booking' : 'Không sử dụng được']; })];
      const url = URL.createObjectURL(new Blob(['\uFEFF' + lines.map(r => r.map(csvCell).join(',')).join('\r\n')], { type: 'text/csv;charset=utf-8' })), a = document.createElement('a'); a.href = url; a.download = 'argopost-business-quotes.csv'; a.click(); setTimeout(() => URL.revokeObjectURL(url), 1000);
    } catch { if (live.current.active) setError('Không thể xác minh đầy đủ quyền xuất và các báo giá hiện hành. Chưa tạo file.'); }
    finally { live.current.running = false; if (live.current.active) setBusy(false); }
  }
  return <div className="bst-quotation">{catalogError ? <p role="alert">Chưa tải được hợp đồng doanh nghiệp. Không dùng giá Personal thay thế.</p> : !catalog ? <p role="status">Đang kiểm tra hợp đồng giá…</p> : <div className="bw-panel bst-contract"><div><h2>Giá theo hợp đồng Merchant</h2><p>{catalog.contract && catalog.quoteEnabled ? `Hợp đồng v${catalog.contract.version} · ${catalog.contract.tariffVersion}` : 'Chưa có khả năng báo giá theo hợp đồng hiện hành.'}</p><small>Báo giá từ bản nháp đã lưu, hiệu lực {catalog.tariff ? catalog.tariff.ttlSeconds / 60 : '—'} phút. Không tự xác nhận Booking.</small></div><button type="button" className="pw-secondary" disabled={busy || unknown} onClick={() => { setRows([]); setCatalog(undefined); setCatalogError(false); setRevision(v => v + 1); }}>Tải lại nguồn</button></div>}
    <section className="bw-panel bst-card"><h2>1. Chọn bản nháp cần báo giá</h2><p>Chọn tối đa 10 bản nháp trong cùng doanh nghiệp; kho và kiện được lấy từ từng bản nháp đã lưu.</p>{!current ? <p role="status">Đang tải bản nháp…</p> : current.error ? <p role="alert">Không tải được bản nháp theo quyền hiện hành.</p> : current.data && <><div className="bst-drafts">{current.data.items.map(d => <label key={d.id}><input type="checkbox" disabled={busy || unknown || !catalog?.quoteEnabled || rows.length >= 10 && !rows.some(r => r.draft.id === d.id)} checked={rows.some(r => r.draft.id === d.id)} onChange={e => setRows(rs => e.target.checked ? [...rs, { draft: d, state: 'IDLE' }] : rs.filter(r => r.draft.id !== d.id))} /><span><strong>{d.input.reference ?? d.sourceOrderReference ?? 'Bản nháp ' + d.id.slice(0, 8)}</strong><small>{d.input.parcels.length} kiện · Kho {d.facilityId?.slice(0, 8) ?? 'không gắn'} · v{d.version}</small></span></label>)}</div>{!current.data.items.length && <p>Chưa có bản nháp. Tạo và lưu thông tin gửi hàng trước khi báo giá.</p>}<nav className="ab-pagination"><button type="button" className="pw-secondary" disabled={busy || unknown || !page} onClick={() => setPage(p => p - 1)}>Trang trước</button><span>Trang {page + 1}</span><button type="button" className="pw-secondary" disabled={busy || unknown || !current.data.hasNext} onClick={() => setPage(p => p + 1)}>Trang sau</button></nav></>}<Link to={'/merchant/' + context.organizationId + '/create'} onClick={e => { if (busy || unknown) e.preventDefault(); }}>Tạo bản nháp mới</Link></section>
    <section className="bw-panel bst-card"><h2>2. Tuyến và điều kiện vận chuyển</h2><label>Tuyến áp dụng cho các bản nháp đã chọn<select value={route} disabled={busy || unknown || rows.some(r => r.state === 'QUOTED')} onChange={e => setRoute(e.target.value)}><option value="">Chọn tuyến</option>{catalog?.coverage.routes.map(r => <option key={r.originCode + '/' + r.destinationCode} value={r.originCode + '/' + r.destinationCode}>{r.originCode} → {r.destinationCode} · {r.dropOffCode}</option>)}</select></label><p>ROAD_STANDARD · Gửi tại điểm bàn giao · Người gửi trả cước · Không COD / Pickup.</p><label className="bst-check"><input type="checkbox" checked={ordinary} disabled={busy || unknown} onChange={e => setOrdinary(e.target.checked)} />Tất cả kiện là hàng thông thường, được đóng gói phù hợp; không có hàng cấm, nguy hiểm hoặc yêu cầu bảo quản đặc biệt.</label><div className="bst-actions"><button type="button" className="pw-primary" disabled={busy || unknown || !ordinary || !rows.some(r => r.state === 'IDLE' || r.state === 'FAILED') || !catalog?.quoteEnabled} onClick={() => { void price(); }}>{busy ? 'Đang xử lý…' : 'Lấy báo giá đã chọn'}</button><button type="button" className="pw-secondary" disabled={!canExport} onClick={() => { void exportQuotes(); }}>Xuất báo giá CSV</button></div>{error && <p role="alert">{error}</p>}</section>
    {rows.length > 0 && <section className="bw-panel bst-card"><h2>3. Kết quả từng bản nháp</h2><p>Mỗi dòng là một báo giá độc lập. Lỗi hoặc chưa rõ kết quả không được tính là thành công.</p>{rows.map(row => { const q = row.quote?.quote, seconds = row.quote ? remainingBusinessQuoteSeconds(row.quote, row.deadline ?? 0, now) : 0, usable = row.quote?.usable && seconds > 0; return <article className="bst-quote-row" key={row.draft.id}><header><h3>{row.draft.input.reference ?? row.draft.sourceOrderReference ?? row.draft.id.slice(0, 8)}</h3><span>{q ? pilotMoney(q.totalVnd) : ({ IDLE: 'Chưa báo giá', RUNNING: 'Đang báo giá', FAILED: 'Không thành công', UNKNOWN: 'Chưa rõ kết quả', QUOTED: 'Đã báo giá' })[row.state]}</span></header>{row.error && <p role={row.state === 'UNKNOWN' ? 'alert' : 'status'}>{row.error}</p>}{row.state === 'UNKNOWN' && <button type="button" className="pw-secondary" disabled={busy} onClick={() => { void resolve(row); }}>Kiểm tra kết quả báo giá</button>}{q && <><p>{q.route.originCode} → {q.route.destinationCode} · {q.feeLines.length} kiện · {usable ? `Còn ${Math.floor(seconds / 60)}:${String(seconds % 60).padStart(2, '0')}` : 'Không còn hiệu lực / đã dùng'}</p><details><summary>Chi tiết tính cước</summary>{q.feeLines.map(l => <p key={l.parcelNumber}>Kiện {l.parcelNumber}: thực {l.actualGrams} g · tính cước {l.chargeableGrams} g · {pilotMoney(l.feeVnd)}</p>)}<p>Hợp đồng v{q.contractVersion} · {q.tariffVersion} · Thuế mô phỏng, không phải hóa đơn thuế.</p></details>{usable && <Link className="pw-primary" to={'/merchant/' + context.organizationId + '/create?' + new URLSearchParams({ draft: row.draft.id, step: 'review', quote: q.id })}>Kiểm tra & Xác nhận Booking</Link>}</>}</article>; })}</section>}
  </div>;
}

export function BusinessServiceTools({ context, calculator }: { context: BusinessContext; calculator?: boolean }) {
  const [catalog, setCatalog] = useState<BusinessCatalog>(), [error, setError] = useState(false), [revision, setRevision] = useState(0);
  const [route, setRoute] = useState(''), [measures, setMeasures] = useState({ weightGrams: '500', lengthMm: '100', widthMm: '100', heightMm: '100' });
  useEffect(() => { let active = true; void customerClient.readBusinessCatalog(context).then(c => { if (active) setCatalog(c); }).catch(() => { if (active) setError(true); }); return () => { active = false; }; }, [context, revision]);
  const selected = catalog?.coverage.routes.find(r => r.originCode + '/' + r.destinationCode === route), t = catalog?.tariff;
  const valid = Object.values(measures).every(v => /^[1-9][0-9]{0,6}$/.test(v));
  const parcel = { description: 'Kiểm tra kích thước', quantity: 1, declaredValueVnd: 0, weightGrams: Number(measures.weightGrams), lengthMm: Number(measures.lengthMm), widthMm: Number(measures.widthMm), heightMm: Number(measures.heightMm) };
  const issues = valid && t ? parcelServiceIssues([parcel], t) : [];
  const volume = valid ? BigInt(measures.lengthMm) * BigInt(measures.widthMm) * BigInt(measures.heightMm) : 0n;
  const rounded = valid && t ? (volume > BigInt(parcel.weightGrams) * BigInt(t.volumetricDivisor) ? volume : BigInt(parcel.weightGrams) * BigInt(t.volumetricDivisor)) : 0n;
  return error ? <section className="bw-panel bst-card"><p role="alert">Chưa kiểm tra được danh mục Merchant. Không dùng danh mục Personal thay thế.</p><button className="pw-secondary" onClick={() => { setError(false); setRevision(v => v + 1); }}>Thử lại</button></section> : !catalog ? <p role="status">Đang tải dịch vụ doanh nghiệp…</p> : <div className="bst-catalog-grid"><section className="bw-panel bst-card"><h2>{calculator ? 'Kiểm tra trọng lượng & Kích thước' : 'Đường bộ tiêu chuẩn'}</h2>{calculator ? <><div className="bst-measures">{(Object.keys(measures) as (keyof typeof measures)[]).map(k => <label key={k}>{{ weightGrams: 'Trọng lượng (g)', lengthMm: 'Dài (mm)', widthMm: 'Rộng (mm)', heightMm: 'Cao (mm)' }[k]}<input inputMode="numeric" maxLength={7} value={measures[k]} onChange={e => setMeasures(m => ({ ...m, [k]: e.target.value }))} /></label>)}</div>{!valid ? <p role="alert">Nhập số nguyên dương đúng đơn vị.</p> : !t ? <p>Chưa có thông số hợp đồng hiện hành để kiểm tra.</p> : <><p>Quy đổi: <strong>{dimensionalGrams(String(volume), String(t.volumetricDivisor))} g</strong></p>{issues.length ? <p role="alert">{issues.join(' ')}</p> : <p>Tính cước: <strong>{((rounded + BigInt(t.volumetricDivisor * t.stepGrams) - 1n) / BigInt(t.volumetricDivisor * t.stepGrams) * BigInt(t.stepGrams)).toLocaleString('vi-VN')} g</strong> · Trong giới hạn kiện.</p>}<p className="se-hint">Tiện ích kiểm tra, không phải báo giá đã phát hành. Backend kiểm tra lại dữ liệu bản nháp khi lấy Quote.</p></>}</> : <><p>ROAD_STANDARD · Bàn giao tại điểm gửi được chỉ định.</p>{t && <dl className="se-facts"><div><dt>Số kiện tối đa</dt><dd>{t.maxParcels}</dd></div><div><dt>Trọng lượng thực / kiện</dt><dd>≤ {t.maxActualGrams / 1000} kg</dd></div><div><dt>Mỗi chiều / chu vi quy ước</dt><dd>≤ {t.maxDimensionMm / 10} cm / {t.maxGirthMm / 10} cm</dd></div><div><dt>Quy đổi</dt><dd>Dài × Rộng × Cao (cm) / {t.volumetricDivisor} kg; làm tròn {t.stepGrams} g / kiện</dd></div></dl>}<p>Không có dịch vụ thứ hai để so sánh; không hiển thị giá hoặc ETA giả.</p></>}</section><section className="bw-panel bst-card"><h2>Vùng phục vụ & Điểm gửi</h2><label>Kiểm tra tuyến<select value={route} onChange={e => setRoute(e.target.value)}><option value="">Chọn tuyến đã công bố</option>{catalog.coverage.routes.map(r => <option key={r.originCode + '/' + r.destinationCode} value={r.originCode + '/' + r.destinationCode}>{r.originCode} → {r.destinationCode}</option>)}</select></label>{selected && <p><strong>{selected.dropOffCode}</strong> · Chỉ drop-off. Tuyến {selected.classification === 'INTRA' ? 'nội vùng' : 'liên vùng'} thuộc {selected.datasetVersion}.</p>}<p>Mã SIM không phải địa giới hoặc địa chỉ Station thực. Chưa có tọa độ, giờ Carrier nhận hàng, ETA hay năng lực Pickup để cam kết.</p><h3>Hàng hóa & dịch vụ bổ sung</h3><p>Chỉ nhận hàng thông thường đã đóng gói phù hợp. Không hàng cấm, nguy hiểm, cần điều kiện bảo quản đặc biệt. Chưa hỗ trợ COD, bảo hiểm, thu tiền hoặc dịch vụ cộng thêm.</p><Link to={'/merchant/' + context.organizationId + '/lookup?view=quote'}>Báo giá theo bản nháp doanh nghiệp</Link></section></div>;
}
