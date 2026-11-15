import { useEffect, useRef, useState } from 'react';
import { customerClient, CustomerApiError } from './client';
import type { BusinessContext } from './business-data';
import type { OrganizationManagementPage } from './business-management-data';
import { csvCell, labelZip } from './shipment-management';
const names: Record<string, string> = { shipmentCode: 'Mã vận đơn', bookingTimeVietnam: 'Ngày Booking (giờ Việt Nam)', reference: 'Tham chiếu', sourceChannel: 'Kênh tạo đơn', sourceOrderReference: 'Mã đơn nguồn', service: 'Dịch vụ', origin: 'Vùng gửi', destination: 'Vùng nhận', parcelCount: 'Số kiện', totalVnd: 'Cước (VND)', paymentState: 'Nghĩa vụ cước', creatorAccountId: 'Người tạo', facilityId: 'Kho Merchant', importJobIds: 'Đợt nhập', assigneeAccountId: 'Người phụ trách nội bộ', recipientName: 'Người nhận', recipientPhone: 'Điện thoại người nhận', recipientAddress: 'Địa chỉ nhận' };
function download(blob: Blob, filename: string) { const url = URL.createObjectURL(blob), anchor = document.createElement('a'); anchor.href = url; anchor.download = filename; anchor.click(); setTimeout(() => URL.revokeObjectURL(url), 1000); }
export function OrganizationDownloads({ context, data, query, selected }: { context: BusinessContext; data: OrganizationManagementPage; query: URLSearchParams; selected: string[] }) {
  const [busy, setBusy] = useState(false), [pii, setPii] = useState(false), [scope, setScope] = useState('page'), [error, setError] = useState(''), [notice, setNotice] = useState('');
  const lifecycle = useRef({ active: true, running: false, generation: 0 });
  useEffect(() => { const run = lifecycle.current; run.active = true; return () => { run.active = false; run.generation++; }; }, []);
  const chosen = selected.length ? data.items.filter(i => selected.includes(i.record.shipment.id)) : data.items;
  const canExport = chosen.length > 0 && (!selected.length || chosen.length === selected.length) && chosen.every(i => i.allowedActions.includes('EXPORT'));
  const canPii = chosen.length > 0 && chosen.every(i => i.allowedActions.includes('EXPORT_PII'));
  const canFiltered = canExport;
  const canLabel = selected.length > 0 && chosen.length === selected.length && chosen.every(i => i.allowedActions.includes('READ_LABEL'));
  async function exportFile(format: 'csv' | 'xlsx') {
    const run = lifecycle.current, generation = run.generation, identity = customerClient.getSnapshot(); if (run.running || !canExport || pii && !canPii || scope === 'filtered' && !canFiltered) return;
    run.running = true; setBusy(true); setError(''); setNotice('');
    try {
      const result = await customerClient.exportOrganizationShipments(context, query, pii, scope === 'filtered' ? undefined : chosen.map(i => i.record.shipment.id));
      if (!run.active || run.generation !== generation || customerClient.getSnapshot() !== identity) return;
      const rows = [['ARGOPOST LOCAL/TEST SIMULATION — Không phải chứng từ thương mại'], ['Doanh nghiệp', context.displayName], ['Phạm vi', scope === 'filtered' ? 'Toàn bộ kết quả phù hợp (tối đa 200)' : 'Các vận đơn đã chọn / trang hiện tại'], ['Số vận đơn', String(result.rowCount)], ['Thời điểm', result.generatedAt], result.columns.map(k => names[k]), ...result.rows];
      let blob: Blob;
      if (format === 'csv') blob = new Blob(['\uFEFF' + rows.map(row => row.map(csvCell).join(',')).join('\r\n') + '\r\n'], { type: 'text/csv;charset=utf-8' });
      else { const xlsx = await import('xlsx'), sheet = xlsx.utils.aoa_to_sheet(rows.map(row => row.map(v => ({ t: 's', v })))); const book = xlsx.utils.book_new(); xlsx.utils.book_append_sheet(book, sheet, 'VanDon'); blob = new Blob([xlsx.write(book, { type: 'array', bookType: 'xlsx', compression: true })], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' }); }
      if (!run.active || run.generation !== generation || customerClient.getSnapshot() !== identity) return;
      download(blob, 'argopost-business-' + result.exportId + '.' + format); setNotice(`Đã xuất ${result.rowCount} vận đơn${result.piiIncluded ? ', có liên hệ người nhận theo quyền được cấp' : ', không gồm các cột liên hệ người nhận'}.`);
    } catch (e) { if (run.active && run.generation === generation) setError(e instanceof CustomerApiError && e.code === 'EXPORT_LIMIT_EXCEEDED' ? 'Có hơn 200 vận đơn. Thu hẹp bộ lọc hoặc chọn tối đa 20 đơn trên trang.' : 'Chưa thể xuất dữ liệu. Quyền hoặc dữ liệu đã thay đổi, hoặc chưa ghi được audit truy cập. Chưa tạo file.'); }
    finally { run.running = false; if (run.active && run.generation === generation) setBusy(false); }
  }
  async function labels() {
    const run = lifecycle.current, generation = run.generation, identity = customerClient.getSnapshot(); if (run.running || !canLabel) return;
    run.running = true; setBusy(true); setError(''); setNotice(''); const files: { name: string; bytes: Uint8Array<ArrayBuffer> }[] = []; let total = 0;
    try { for (const item of chosen) { const pdf = await customerClient.downloadBusinessLabel(context, item.record.shipment.id); if (!run.active || run.generation !== generation || customerClient.getSnapshot() !== identity) return; total += pdf.size; if (total > 32 * 1024 * 1024) throw new Error('LABEL_LIMIT'); files.push({ name: 'shipment-' + item.record.shipment.id + '.pdf', bytes: new Uint8Array(await pdf.arrayBuffer()) }); }
      if (!run.active || run.generation !== generation || customerClient.getSnapshot() !== identity) return; download(labelZip(files), 'argopost-business-labels.zip'); setNotice(`Đã tải nhãn của ${files.length} vận đơn. Chưa xác nhận Carrier nhận hàng.`);
    } catch { if (run.active && run.generation === generation) setError('Không tải được đầy đủ nhãn (quyền, kết nối hoặc giới hạn 32 MiB). Chưa tạo ZIP; không tải một phần.'); }
    finally { files.length = 0; run.running = false; if (run.active && run.generation === generation) setBusy(false); }
  }
  return <section className="om-downloads" aria-label="Thao tác vận đơn theo quyền"><div><span>{selected.length ? `${selected.length} vận đơn được chọn trên trang` : 'Tối đa 20 vận đơn mỗi trang'}</span><button type="button" className="pw-secondary" disabled={busy || !canLabel} onClick={() => { void labels(); }}>Tải nhãn đã chọn (.zip)</button><label>Phạm vi xuất<select disabled={busy} value={scope} onChange={e => setScope(e.target.value)}><option value="page">{selected.length ? 'Vận đơn đã chọn' : 'Trang hiện tại'}</option><option value="filtered" disabled={!canFiltered}>Toàn bộ kết quả (tối đa 200)</option></select></label><button type="button" className="pw-secondary" disabled={busy || !canExport} onClick={() => { void exportFile('csv'); }}>Xuất CSV</button><button type="button" className="pw-secondary" disabled={busy || !canExport} onClick={() => { void exportFile('xlsx'); }}>Xuất Excel</button></div><label className="om-pii"><input type="checkbox" checked={pii} disabled={busy || !canPii} onChange={e => setPii(e.target.checked)} />Gồm liên hệ người nhận (cần quyền riêng)</label>{busy && <p role="status">Đang kiểm tra quyền và chuẩn bị file…</p>}{notice && <p role="status">{notice}</p>}{error && <p role="alert">{error}</p>}</section>;
}
