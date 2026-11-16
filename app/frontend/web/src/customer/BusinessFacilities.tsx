import { useEffect, useState } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { customerClient } from './client';
import type { BusinessContext } from './business-data';
import type { OperationsReport } from './business-report-data';
import { dateOffset, vietnamToday } from './business-report-navigation';
import './business-facilities.css';

const count = (value: string) => BigInt(value).toLocaleString('vi-VN');
/** Existing accepted reporting source only. This is not a Facility master listing. */
export function BusinessFacilities({ context }: { context: BusinessContext }) {
  const [raw, setRaw] = useSearchParams();
  const range = raw.get('days') ?? '30';
  const valid = [...raw.keys()].every(k => k === 'days' && raw.getAll(k).length === 1) && ['7', '30', '90'].includes(range);
  const today = vietnamToday(), from = dateOffset(today, 1 - Number(valid ? range : '30')), until = dateOffset(today, 1);
  const [revision, setRevision] = useState(0);
  const [result, setResult] = useState<{ key: string; report?: OperationsReport; error?: string }>();
  const key = [context.organizationId, context.authorityVersion, range, from, until, revision].join(':');
  useEffect(() => {
    if (!valid) return;
    let current = true;
    void customerClient.readBusinessOperations(context, new URLSearchParams({ from, until, interval: 'DAY' }))
      .then(report => { if (current) setResult({ key, report }); })
      .catch(error => { if (current) setResult({ key, error: /RESOURCE_NOT_FOUND|FORBIDDEN|CONTEXT_UNAVAILABLE/.test(error instanceof Error ? error.message : '') ? 'Chưa có quyền đọc báo cáo tại các địa điểm này hoặc quyền truy cập đã thay đổi.' : 'Chưa tải được dữ liệu địa điểm. Hãy thử lại.' }); });
    return () => { current = false; };
  }, [context, valid, from, until, key]);
  const current = result?.key === key ? result : undefined, report = current?.report;
  const sites = report?.breakdowns.facility.filter(row => row.key !== 'UNBOUND') ?? [];
  const unbound = report?.breakdowns.facility.find(row => row.key === 'UNBOUND');
  const total = report ? BigInt(report.summary.shipments) : 0n;
  const base = '/merchant/' + context.organizationId;
  const shipments = (id: string) => base + '/shipments?' + new URLSearchParams({ from, until, facilityId: id });
  const reports = base + '/reports?' + new URLSearchParams({ from, until, panel: 'dimensions' });
  return <section className="wf-page">
    <header className="wf-heading"><div><span className="pw-eyebrow">ARGOPOST BUSINESS</span><h1 tabIndex={-1}>Kho & Điểm lấy hàng</h1><p>Hoạt động gửi hàng theo địa điểm của {context.displayName}.</p></div><button className="pw-secondary" type="button" onClick={() => setRevision(v => v + 1)}>Làm mới</button></header>
    {!valid ? <div className="pw-panel" role="alert"><h2>Bộ lọc không hợp lệ</h2><Link to={base + '/facilities?days=30'}>Đặt lại bộ lọc</Link></div> : <>
      <div className="wf-toolbar"><div className="bw-range" aria-label="Khoảng thời gian sản lượng địa điểm">{['7', '30', '90'].map(days => <button type="button" key={days} aria-pressed={range === days} onClick={() => setRaw({ days })}>{days} ngày</button>)}</div><span>{from.split('-').reverse().join('/')} – {today.split('-').reverse().join('/')} · Giờ Việt Nam</span></div>
      {!current ? <div className="pw-panel" role="status">Đang tải sản lượng theo địa điểm…</div> : current.error ? <div className="pw-panel" role="alert"><h2>Không thể hiển thị dữ liệu</h2><p>{current.error}</p></div> : report && <>
        <div className="wf-kpis"><article><span>Địa điểm có Booking trong kỳ</span><strong>{sites.length.toLocaleString('vi-VN')}</strong></article><article><span>Vận đơn đã gắn địa điểm</span><strong>{count((total - BigInt(unbound?.shipments ?? '0')).toString())}</strong></article><article><span>Vận đơn chưa gắn địa điểm</span><strong>{count(unbound?.shipments ?? '0')}</strong></article></div>
        <section className="wf-card"><header><div><h2>Sản lượng theo địa điểm</h2><p>Toàn bộ vận đơn trong kỳ và phạm vi được cấp quyền.</p></div><Link to={reports}>Phân tích sản lượng</Link></header>
          <div className="wf-table-scroll"><table><thead><tr><th>Địa điểm</th><th>Vận đơn</th><th>Kiện hàng</th><th>Trọng lượng thực</th>{report.financialState === 'AVAILABLE' && <th>Cước xác nhận</th>}<th>Danh sách</th></tr></thead><tbody>{sites.map(row => <tr key={row.key}><td><strong>Địa điểm Merchant</strong><small>{row.key}</small><span className="wf-volume" aria-hidden="true"><span style={{ width: (total ? Number(BigInt(row.shipments) * 10000n / total) / 100 : 0) + '%' }} /></span></td><td>{count(row.shipments)}</td><td>{count(row.parcels)}</td><td>{count(row.actualGrams)} g</td>{report.financialState === 'AVAILABLE' && <td>{count(row.confirmedFeesVnd!)} ₫</td>}<td><Link to={shipments(row.key)}>Xem vận đơn<span className="wf-sr-only"> tại {row.key}</span></Link></td></tr>)}</tbody></table></div>
          {!sites.length && <div className="wf-empty"><h3>Chưa có Booking gắn địa điểm trong kỳ</h3><p>Điều này không xác định doanh nghiệp đã có hay chưa có kho.</p></div>}
          {unbound && BigInt(unbound.shipments) > 0n && <div className="wf-unbound"><strong>{count(unbound.shipments)} vận đơn chưa gắn địa điểm</strong><p>Được thống kê riêng, không quy thành một kho hoặc tự gán lại sau Booking.</p><Link to={reports}>Xem nhóm chưa gắn kho trong báo cáo</Link></div>}
        </section>
        <p className="wf-scope">Nguồn hiện tại là Booking, không phải danh mục kho hoặc bằng chứng đã bàn giao. Pickup chưa được dịch vụ hiện hành hỗ trợ.</p>
      </>}
    </>}
  </section>;
}
