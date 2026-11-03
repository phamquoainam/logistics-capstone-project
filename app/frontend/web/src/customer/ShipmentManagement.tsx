import { useEffect, useState } from 'react';
import type { FormEvent, ReactNode } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { customerClient, CustomerApiError } from './client';
import type { ManagementPage } from './client';
import './management.css';

const categories = [
  ['ALL', 'Tất cả vận đơn'], ['DRAFT', 'Đơn nháp'], ['WAITING', 'Chờ bàn giao'], ['TRANSIT', 'Đang vận chuyển'], ['ACTION', 'Đơn cần xử lý'], ['COMPLETED', 'Đã hoàn tất'], ['RETURNED', 'Hoàn hàng'], ['CANCELLED', 'Đã hủy'], ['SUPPORT', 'Hỗ trợ liên quan'],
] as const;
const dependencies: Record<string, string> = {
  TRANSIT: 'Cần sự kiện Carrier Transit được ghi nhận từ nghiệp vụ điều phối và bàn giao. Tiếp nhận tại Station chưa đồng nghĩa đang vận chuyển.',
  COMPLETED: 'Cần kết quả giao hàng và bằng chứng POD hợp lệ từ Carrier. Personal không được tự ghi nhận đã giao.',
  RETURNED: 'Cần quy trình và bằng chứng hoàn hàng từ Carrier. Chưa có nguồn dữ liệu hoàn hàng được kết nối.',
  CANCELLED: 'Chưa mở quy trình hủy vận đơn đã Booking. Lưu trữ bản nháp không phải hủy vận đơn.',
  SUPPORT: 'Ticket hỗ trợ theo vận đơn chưa được triển khai. Không có yêu cầu hoặc kết quả xử lý được giả lập.',
};
const dateOffset = (value: string, days: number) => Number.isFinite(Date.parse(value)) ? new Date(Date.parse(value) + days * 86400000).toISOString().slice(0, 10) : '';
const count = (value: string) => BigInt(value).toLocaleString('vi-VN');
const money = (value: number) => value.toLocaleString('vi-VN') + ' ₫';
const statusLabel = { WAITING_FOR_HANDOVER: 'Chờ bàn giao', AT_STATION: 'Tại Station' };

function Categories({ raw, active }: { raw: URLSearchParams; active: string }) {
  function url(key: string) {
    if (key === 'DRAFT') return '/personal/shipments?tab=drafts';
    if (key === 'ACTION') return '/personal/action-required';
    const p = new URLSearchParams(raw); for (const k of ['category', 'tab', 'page', 'status']) p.delete(k);
    if (key === 'WAITING') p.set('status', 'WAITING_FOR_HANDOVER'); else if (key !== 'ALL') p.set('category', key);
    return '/personal/shipments' + (p.size ? '?' + p : '');
  }
  return <nav className="pm-categories" aria-label="Nhóm vận đơn">{categories.map(([key, title]) => <Link key={key} to={url(key)} aria-current={active === key ? 'page' : undefined} title={dependencies[key]}>{title}{dependencies[key] && <span aria-label="Chờ nghiệp vụ" className="pm-category-lock">◦</span>}</Link>)}</nav>;
}

function Filters({ parameters, change }: { parameters: URLSearchParams; change: (p: URLSearchParams) => void }) {
  const [from, setFrom] = useState(parameters.get('from') ?? ''), [to, setTo] = useState(parameters.has('until') ? dateOffset(parameters.get('until')!, -1) : '');
  function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault(); const fields = new FormData(event.currentTarget), p = new URLSearchParams();
    for (const key of ['search', 'recipient', 'status', 'service', 'obligation', 'sort']) { const value = String(fields.get(key) ?? '').trim(); if (value) p.set(key, value); }
    if (from || to) { p.set('from', from); p.set('until', dateOffset(to, 1)); }
    const route = String(fields.get('route') ?? '').split('/'); if (route.length === 2) { p.set('origin', route[0]); p.set('destination', route[1]); }
    change(p);
  }
  return <form className="pw-panel pm-filters" aria-label="Bộ lọc quản lý vận đơn" onSubmit={submit}>
    <div className="pm-filter-grid"><label htmlFor="pilot-shipment-search">Mã vận đơn / tham chiếu / liên hệ<input id="pilot-shipment-search" name="search" defaultValue={parameters.get('search') ?? ''} maxLength={160} placeholder="Tìm vận đơn của bạn…" /></label><label>Người nhận<input name="recipient" defaultValue={parameters.get('recipient') ?? ''} maxLength={160} placeholder="Tên hoặc số điện thoại" /></label><label>Từ ngày<input type="date" value={from} onChange={e => setFrom(e.target.value)} required={!!to} /></label><label>Đến ngày<input type="date" value={to} min={from} onChange={e => setTo(e.target.value)} required={!!from} /></label>
      <label htmlFor="pilot-status">Trạng thái<select id="pilot-status" name="status" aria-label="Trạng thái" defaultValue={parameters.get('status') ?? ''}><option value="">Tất cả trạng thái</option><option value="WAITING_FOR_HANDOVER">Chờ bàn giao</option><option value="AT_STATION">Tại Station</option></select></label><label>Dịch vụ<select name="service" aria-label="Dịch vụ" defaultValue={parameters.get('service') ?? ''}><option value="">Tất cả dịch vụ</option><option value="ROAD_STANDARD">Đường bộ tiêu chuẩn</option></select></label><label>Tuyến gửi hàng<select name="route" aria-label="Tuyến gửi hàng" defaultValue={parameters.has('origin') ? parameters.get('origin') + '/' + parameters.get('destination') : ''}><option value="">Tất cả tuyến</option>{['SIM-A1/SIM-A2', 'SIM-A2/SIM-A1', 'SIM-A1/SIM-B1', 'SIM-B1/SIM-A1'].map(r => <option key={r} value={r}>{r.replace('/', ' → ')}</option>)}</select></label><label>Nghĩa vụ cước<select name="obligation" aria-label="Nghĩa vụ cước" defaultValue={parameters.get('obligation') ?? ''}><option value="">Tất cả nghĩa vụ</option><option value="UNPAID">UNPAID · Chưa thu tiền</option></select></label>
    </div><div className="pm-filter-bottom"><span>{from ? 'Theo ngày tạo vận đơn · Giờ Việt Nam' : 'Toàn bộ thời gian tạo vận đơn'}</span><label>Sắp xếp<select name="sort" aria-label="Sắp xếp" defaultValue={parameters.get('sort') ?? 'NEWEST'}><option value="NEWEST">Mới nhất</option><option value="OLDEST">Cũ nhất</option><option value="FEE_DESC">Cước giảm dần</option><option value="FEE_ASC">Cước tăng dần</option></select></label><button type="button" className="pw-secondary" onClick={() => change(new URLSearchParams())}>Xóa bộ lọc</button><button className="pw-primary">Tìm vận đơn</button></div>
  </form>;
}

export function ShipmentManagement({ draftView, lookup = false }: { draftView?: ReactNode; lookup?: boolean }) {
  const [raw, setParameters] = useSearchParams(), category = raw.get('tab') === 'drafts' ? 'DRAFT' : raw.get('category') ?? (raw.get('status') === 'WAITING_FOR_HANDOVER' ? 'WAITING' : 'ALL');
  const parameters = new URLSearchParams(raw); parameters.delete('tab'); parameters.delete('category');
  const query = parameters.toString(), supported = ['ALL', 'WAITING'].includes(category);
  const [result, setResult] = useState<{ query: string; data?: ManagementPage; error?: string }>(), [revision, setRevision] = useState(0);
  const data = result?.query === query ? result.data : undefined, error = result?.query === query ? result.error : undefined;
  useEffect(() => {
    if (!supported) return;
    let current = true;
    customerClient.listPersonalManagement(new URLSearchParams(query)).then(data => { if (current) setResult({ query, data }); }).catch(failure => {
      if (current) setResult({ query, error: failure instanceof CustomerApiError && failure.code === 'VALIDATION_FAILED' ? 'Bộ lọc không hợp lệ. Hãy xóa bộ lọc rồi thử lại.' : 'Không tải được vận đơn. Danh sách chưa được xác nhận; vui lòng thử lại.' });
    }); return () => { current = false; };
  }, [query, revision, supported]);
  function change(p: URLSearchParams) { setResult(undefined); if (p.toString() === query) setRevision(v => v + 1); setParameters(p); }
  function status(value: string) { const p = new URLSearchParams(parameters); p.delete('page'); if (value) p.set('status', value); else p.delete('status'); change(p); }
  function page(offset: number) { if (!data) return; const p = new URLSearchParams(parameters); p.set('page', String(data.page + offset)); change(p); }
  const filtered = [...parameters.keys()].some(k => !['page', 'size', 'sort'].includes(k));
  const head = <div className="pw-page-heading"><div><span className="pw-eyebrow">QUẢN LÝ</span><h1 tabIndex={-1}>{lookup ? 'Tra cứu vận đơn của bạn' : 'Quản lý vận đơn'}</h1><p>Tra cứu, theo dõi trạng thái và cước vận đơn trong tài khoản của bạn.</p></div><Link className="pw-primary" to="/personal/create">+ Tạo vận đơn</Link></div>;
  const tabs = <Categories raw={raw} active={category} />;
  if (category === 'DRAFT' && draftView) return <section className="pm-workspace">{tabs}{draftView}</section>;
  if (!supported) return <section className="pm-workspace">{head}{tabs}<div className="pw-panel pm-dependency"><span className="pw-tag">Chờ nghiệp vụ</span><h2>{categories.find(([key]) => key === category)?.[1] ?? 'Nhóm vận đơn không hợp lệ'}</h2><p>{dependencies[category] ?? 'Nhóm này chưa có nguồn dữ liệu được kết nối. Không thể xác nhận danh sách trống hay số lượng vận đơn.'}</p><Link className="pw-secondary" to="/personal/shipments">Về tất cả vận đơn</Link></div></section>;
  return <section className="pm-workspace">{head}<Filters key={query} parameters={parameters} change={change} />{tabs}
    {error ? <div className="pw-panel pm-error"><h2>Chưa thể hiển thị vận đơn</h2><p role="alert">{error}</p><button className="pw-secondary" onClick={() => { setResult(undefined); setRevision(v => v + 1); }}>Tải lại vận đơn</button><button className="pw-secondary" onClick={() => change(new URLSearchParams())}>Xóa bộ lọc</button></div> : !data ? <div className="pw-panel pm-loading" role="status">Đang tải vận đơn và số liệu…</div> : <>
      <div className="pm-kpi-caption"><span>Vận đơn theo bộ lọc · Bao gồm mọi trạng thái</span><span>Cập nhật {new Date(data.generatedAt).toLocaleTimeString('vi-VN', { timeZone: data.timeZone })}</span></div><section className="pm-kpis" aria-label="KPI quản lý vận đơn">{[['', 'Tổng vận đơn', data.summary.totalShipments], ['WAITING_FOR_HANDOVER', 'Chờ bàn giao', data.summary.waitingForHandover], ['AT_STATION', 'Tại Station', data.summary.atStation]].map(([key, title, amount]) => <button key={title} type="button" onClick={() => status(key)} aria-pressed={(parameters.get('status') ?? '') === key}><span className="pm-kpi-symbol" aria-hidden="true">{key === 'AT_STATION' ? '✓' : key ? '◷' : '▣'}</span><span><span>{title}</span><strong>{count(amount)}<small>vận đơn</small></strong></span></button>)}</section>
      <section className="pw-panel pm-results"><div className="pm-results-heading"><div><h2>Danh sách vận đơn</h2><p>{count(data.total)} vận đơn phù hợp</p></div><span className="pm-period">{parameters.has('from') ? 'Theo khoảng ngày đã chọn' : 'Toàn bộ thời gian'}</span></div>
      {data.items.length ? <div className="pm-table-scroll" role="region" aria-label="Bảng quản lý vận đơn" tabIndex={0}><table className="pm-table"><thead><tr><th>Mã / ngày tạo</th><th>Người gửi / người nhận</th><th>Tuyến / dịch vụ</th><th>Kiện hàng</th><th>Cước / nghĩa vụ</th><th>Trạng thái</th><th>Thao tác</th></tr></thead><tbody>{data.items.map(({ record: { shipment: s, obligation }, status }) => <tr key={s.id}><td><Link className="pm-code" to={'/personal/shipments/' + s.id} title={s.code}>{s.code}</Link><small>{new Date(s.createdAt).toLocaleString('vi-VN', { timeZone: data.timeZone, dateStyle: 'short', timeStyle: 'short' })}</small>{s.input.reference && <span className="pm-reference">{s.input.reference}</span>}</td><td><div className="pm-contact"><small>Gửi</small><span>{s.input.sender.name}</span></div><div className="pm-contact"><small>Nhận</small><span>{s.input.recipient.name}<small>{s.input.recipient.phone}</small></span></div></td><td>{s.quote.route.originCode} → {s.quote.route.destinationCode}<small>Đường bộ tiêu chuẩn</small></td><td>{s.parcels.length} kiện</td><td><strong>{money(s.totalVnd)}</strong><span className="pb-unpaid">{obligation.state} · Chưa thu tiền</span></td><td><span className={'pm-status pm-status-' + status}>{statusLabel[status]}</span></td><td><Link to={'/personal/shipments/' + s.id}>Xem vận đơn →</Link><Link to={'/personal/shipments/' + s.id + '?tab=documents'}>Nhãn & chứng từ</Link></td></tr>)}</tbody></table></div> : <div className="pm-empty"><span className="pm-empty-symbol" aria-hidden="true">▣</span><h2>{data.total !== '0' ? 'Trang này không có vận đơn' : filtered ? 'Không có vận đơn phù hợp' : 'Bạn chưa có vận đơn đã đặt'}</h2><p>{data.total !== '0' ? 'Quay về trang đầu để xem dữ liệu.' : filtered ? 'Thử thay đổi điều kiện tìm kiếm hoặc xóa bộ lọc.' : 'Bản nháp được lưu riêng. Vận đơn xuất hiện sau khi bạn xác nhận Booking thành công.'}</p><Link className="pw-secondary" to={data.total !== '0' ? '/personal/shipments' : filtered ? '/personal/shipments' : '/personal/shipments?tab=drafts'}>{data.total !== '0' ? 'Về trang đầu' : filtered ? 'Xóa bộ lọc' : 'Xem đơn nháp'}</Link></div>}
      <nav className="pm-pagination" aria-label="Phân trang vận đơn"><span>Trang {data.page + 1} · 20 vận đơn / trang</span><button className="pw-secondary" disabled={data.page === 0} onClick={() => page(-1)}>Trang trước</button><button className="pw-secondary" disabled={!data.hasNext} onClick={() => page(1)}>Trang sau</button></nav></section>
    </>}
  </section>;
}
