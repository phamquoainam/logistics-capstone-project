import { useEffect, useRef, useState } from 'react';
import { customerClient } from './client';
import type { ManagementPage } from './client';
import { labelZip, managementCsv } from './shipment-management';
import type { ManagementColumn } from './shipment-management';

export function ShipmentDownloads({ data, selected, columns, query }: { data: ManagementPage; selected: string[]; columns: ManagementColumn[]; query: string }) {
  const [busy, setBusy] = useState(false), [message, setMessage] = useState(''), [error, setError] = useState('');
  const lifecycle = useRef({ active: true, running: false }), urls = useRef(new Set<string>());
  useEffect(() => { const current = lifecycle.current; current.active = true; const objects = urls.current; return () => { current.active = false; for (const url of objects) URL.revokeObjectURL(url); objects.clear(); }; }, []);
  function download(blob: Blob, name: string) {
    const url = URL.createObjectURL(blob); urls.current.add(url);
    const anchor = document.createElement('a'); anchor.href = url; anchor.download = name; anchor.click();
    window.setTimeout(() => { URL.revokeObjectURL(url); urls.current.delete(url); }, 60000);
  }
  function csv() {
    const items = selected.length ? data.items.filter(i => selected.includes(i.record.shipment.id)) : data.items;
    const identity = customerClient.getSnapshot();
    if (!items.length || identity.status !== 'authenticated' || items.some(i => i.record.shipment.accountId !== identity.subject?.accountId)) return;
    download(new Blob([managementCsv(data, items, columns, query)], { type: 'text/csv;charset=utf-8' }), 'argopost-shipments-page-' + (data.page + 1) + '.csv');
    setError(''); setMessage(`Đã xuất ${items.length} vận đơn trên trang ${data.page + 1}, theo các cột đang hiển thị.`);
  }
  async function labels() {
    if (lifecycle.current.running || !selected.length) return;
    const items = data.items.filter(i => selected.includes(i.record.shipment.id)), identity = customerClient.getSnapshot();
    if (items.length !== selected.length || identity.status !== 'authenticated') return;
    lifecycle.current.running = true; setBusy(true); setError(''); setMessage('');
    const files: { name: string; bytes: Uint8Array<ArrayBuffer> }[] = []; let size = 0;
    try {
      for (const { record: { shipment } } of items) {
        if (!lifecycle.current.active || customerClient.getSnapshot() !== identity) return;
        const pdf = await customerClient.downloadPilotLabel(shipment.id);
        if (!lifecycle.current.active || customerClient.getSnapshot() !== identity) return;
        size += pdf.size; if (size > 32 * 1024 * 1024) throw new Error('BUNDLE_LIMIT');
        files.push({ name: 'shipment-' + shipment.id + '.pdf', bytes: new Uint8Array(await pdf.arrayBuffer()) });
      }
      if (!lifecycle.current.active || customerClient.getSnapshot() !== identity) return;
      download(labelZip(files), 'argopost-private-labels.zip'); setMessage(`Đã tải nhãn của ${files.length} vận đơn. Mỗi PDF chứa nhãn của toàn bộ kiện thuộc vận đơn đó.`);
    } catch { if (lifecycle.current.active) setError('Không thể tải đầy đủ nhãn đã chọn (quyền truy cập, kết nối hoặc giới hạn 32 MB). Chưa tạo ZIP; hãy kiểm tra rồi thử lại.'); }
    finally { files.length = 0; lifecycle.current.running = false; if (lifecycle.current.active) setBusy(false); }
  }
  return <div className="pm-downloads"><div><span>{selected.length ? `${selected.length} vận đơn được chọn trên trang này` : 'Chọn vận đơn để tải nhiều nhãn'}</span><button type="button" className="pw-secondary" disabled={busy || !data.items.length} onClick={csv}>{selected.length ? 'Xuất CSV đã chọn' : 'Xuất CSV trang này'}</button><button type="button" className="pw-secondary" disabled={busy || !selected.length} onClick={() => { void labels(); }}>{busy ? 'Đang tải nhãn…' : 'Tải nhãn đã chọn (.zip)'}</button></div>{message && <p role="status">{message}</p>}{error && <p role="alert">{error}</p>}</div>;
}
