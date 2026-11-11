import { useEffect, useId, useRef, useState } from 'react';
import type { ComponentProps, FormEvent, ReactNode } from 'react';
import { Link, Navigate, useLocation, useSearchParams } from 'react-router-dom';
import { customerClient, CustomerApiError } from './client';
import type { Subject } from './client';
import { metricDefinitions } from './analytics-data';
import type { AnalyticsMetric, AnalyticsReport, MetricId } from './analytics-data';
import { AnalyticsParcelChart, AnalyticsRouteChart, AnalyticsSparkline, AnalyticsStatusDonut, AnalyticsTimeChart } from './AnalyticsCharts';
import { AnalyticsPeriodPicker } from './AnalyticsPeriodPicker';
import './analytics.css';

const statusLabel = { WAITING_FOR_HANDOVER: 'Chờ bàn giao', AT_STATION: 'Đã nhận tại Station' };
const weightLabel: Record<string, string> = { UP_TO_500G: 'Đến 0,5 kg', UP_TO_5KG: 'Trên 0,5 đến 5 kg', OVER_5KG: 'Trên 5 kg' };
const rowFilters = ['page', 'day', 'weight', 'multi', 'search', 'sort', 'obligation'];
const filterNames: Record<string, string> = { status: 'trạng thái', service: 'dịch vụ', origin: 'tuyến', day: 'nhóm thời gian', weight: 'trọng lượng', multi: 'nhiều kiện', obligation: 'nghĩa vụ cước', search: 'mã vận đơn', sort: 'sắp xếp' };
const overviewMetrics: MetricId[] = ['BOOKED_SHIPMENTS', 'AWAITING_HANDOVER', 'ACCEPTED_SHIPMENTS', 'UNPAID_OBLIGATIONS'];
const metricTitles: Partial<Record<MetricId, string>> = { BOOKED_SHIPMENTS: 'Vận đơn', UNPAID_OBLIGATIONS: 'Cước chưa trả' };
export const analyticsNumber = (value: string) => BigInt(value).toLocaleString('vi-VN');
const money = (value: string) => analyticsNumber(value) + ' ₫';
const moveDate = (date: string, days: number) => Number.isFinite(Date.parse(date)) ? new Date(Date.parse(date) + days * 86400000).toISOString().slice(0, 10) : '';
function defaultDates() { const today = new Date(Date.now() + 7 * 3600000).toISOString().slice(0, 10); return { from: moveDate(today, -29), until: moveDate(today, 1) }; }
const displayDate = (date: string) => Number.isFinite(Date.parse(date)) ? new Date(date).toLocaleDateString('vi-VN', { timeZone: 'Asia/Ho_Chi_Minh', day: '2-digit', month: '2-digit', year: 'numeric' }) : '—';
function fraction(value: string, total: string) { return BigInt(total) === 0n ? 'N/A' : (Number(BigInt(value) * 1000n / BigInt(total)) / 10).toLocaleString('vi-VN', { maximumFractionDigits: 1 }) + '%'; }
function scale(value: string, maximum: bigint) { return maximum === 0n ? 0 : Number(BigInt(value) * 10000n / maximum) / 100; }
function Info({ text, title }: { text: string; title: string }) {
  const element = useRef<HTMLDetailsElement>(null), [position, setPosition] = useState<{ left: number; top?: number; bottom?: number }>();
  useEffect(() => { const close = () => { if (element.current) element.current.open = false; }; window.addEventListener('scroll', close, true); window.addEventListener('resize', close); return () => { window.removeEventListener('scroll', close, true); window.removeEventListener('resize', close); }; }, []);
  return <details className="an-info" ref={element} onToggle={event => { const node = event.currentTarget; if (!node.open) return; const rect = node.getBoundingClientRect(); setPosition({ left: Math.max(16, Math.min(rect.right - 250, window.innerWidth - 266)), ...(rect.bottom + 190 > window.innerHeight ? { bottom: window.innerHeight - rect.top + 8 } : { top: rect.bottom + 8 }) }); }} onBlur={event => { if (!event.currentTarget.contains(event.relatedTarget)) event.currentTarget.open = false; }} onKeyDown={event => { if (event.key === 'Escape') { event.stopPropagation(); event.currentTarget.open = false; event.currentTarget.querySelector('summary')?.focus(); } }}><summary aria-label={'Giải thích ' + title}>i</summary><p style={position}>{text}</p></details>;
}
type Drill = (dimension: string, extra?: Record<string, string>) => string;

// Navigation context never becomes an Analytics API query parameter.
function overviewReturn(pathname: string, search: string, state: unknown) {
  const p = new URLSearchParams(search);
  if (!p.has('from') && !p.has('until')) { const dates = defaultDates(); p.set('from', dates.from); p.set('until', dates.until); }
  if (pathname === '/personal' || pathname === '/personal/overview') return '/personal/overview?' + p;
  const candidate = (state as { analyticsOverview?: unknown } | null)?.analyticsOverview;
  if (typeof candidate === 'string' && /^\/personal\/(?:overview|reports|billing)(?:\?[^#]*)?$/.test(candidate) && candidate.length <= 4096) return candidate;
  for (const key of ['page', 'day', 'weight', 'multi', 'search', 'sort', 'obligation']) p.delete(key);
  return '/personal/overview?' + p;
}
function AnalyticsLink(props: ComponentProps<typeof Link>) {
  const location = useLocation();
  return <Link {...props} state={{ analyticsOverview: overviewReturn(location.pathname, location.search, location.state), analyticsMeasure: (location.state as { analyticsMeasure?: unknown } | null)?.analyticsMeasure }} />;
}

function MetricIcon({ id }: { id: MetricId }) {
  const paths: Record<MetricId, string> = {
    BOOKED_SHIPMENTS: 'm3 7 9-5 9 5v10l-9 5-9-5Zm0 0 9 5 9-5M12 12v10',
    AWAITING_HANDOVER: 'M12 8v4l3 2M22 12a10 10 0 1 0-20 0 10 10 0 0 0 20 0',
    ACCEPTED_SHIPMENTS: 'm7 12 3 3 7-7M4 3h16v18H4Z',
    CONFIRMED_FEES: 'M5 3h14v19l-3-2-4 2-4-2-3 2ZM9 8h6M9 12h6',
    UNPAID_OBLIGATIONS: 'M3 6h18v14H3ZM3 10h18M15 15h3M7 3h10',
    CREATED_PARCELS: 'm3 8 7-4 7 4v8l-7 4-7-4Zm0 0 7 4 7-4M10 12v8m5-16 6 4v8l-5 3',
  };
  return <span className="an-metric-icon" aria-hidden="true"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.6" strokeLinecap="round" strokeLinejoin="round"><path d={paths[id]} /></svg></span>;
}

function Filters({ parameters, change }: { parameters: URLSearchParams; change: (p: URLSearchParams) => void }) {
  function update(values: Record<string, string>) { const next = new URLSearchParams(parameters); for (const key of rowFilters) next.delete(key); for (const [key, value] of Object.entries(values)) { if (value) next.set(key, value); else next.delete(key); } change(next); }
  const defaults = defaultDates(), filtered = parameters.get('from') !== defaults.from || parameters.get('until') !== defaults.until || ['status', 'origin', 'service'].some(key => parameters.has(key));
  return <section className="an-filter-toolbar" aria-label="Bộ lọc Tổng quan"><div className="an-filter-main">
    <AnalyticsPeriodPicker from={parameters.get('from') ?? ''} until={parameters.get('until') ?? ''} apply={(from, until) => update({ from, until })} />
    <select aria-label="Trạng thái" value={parameters.get('status') ?? ''} onChange={event => update({ status: event.target.value })}><option value="">Tất cả trạng thái</option><option value="WAITING_FOR_HANDOVER">Chờ bàn giao</option><option value="AT_STATION">Đã tiếp nhận</option></select>
    <select aria-label="Tuyến" value={parameters.has('origin') ? parameters.get('origin') + '/' + parameters.get('destination') : ''} onChange={event => { const [origin = '', destination = ''] = event.target.value.split('/'); update({ origin, destination }); }}><option value="">Tất cả tuyến</option>{['SIM-A1/SIM-A2', 'SIM-A2/SIM-A1', 'SIM-A1/SIM-B1', 'SIM-B1/SIM-A1'].map(route => <option key={route} value={route}>{route.replace('/', ' → ')}</option>)}</select>
    {filtered && <button type="button" className="an-reset-filters" onClick={() => change(new URLSearchParams(defaultDates()))}>Xóa bộ lọc</button>}
  </div></section>;
}
function Kpi({ metric, drill }: { metric: AnalyticsMetric; drill: Drill }) {
  const definition = metricDefinitions[metric.id], description = useId(), extra: Record<string, string> = metric.id === 'AWAITING_HANDOVER' ? { status: 'WAITING_FOR_HANDOVER' } : metric.id === 'ACCEPTED_SHIPMENTS' ? { status: 'AT_STATION' } : metric.id === 'UNPAID_OBLIGATIONS' ? { obligation: 'UNPAID' } : {};
  const trend = metric.changePercent === null ? null : Number(metric.changePercent), title = metricTitles[metric.id] ?? definition.title;
  const unit = metric.unit === 'VND' ? 'VND' : metric.unit === 'PARCEL' ? 'kiện' : 'vận đơn';
  return <article className={'an-kpi an-kpi-' + metric.id.toLowerCase()} data-metric={metric.id}>
    <div className="an-kpi-top"><MetricIcon id={metric.id} /><div className="an-kpi-title">{title}</div></div>
    <span id={description} className="an-sr-only">{definition.description} Biểu đồ theo ngày tạo vận đơn; trạng thái tại thời điểm đọc.</span>
    <div className="an-kpi-reading">{metric.value === '0' ? <span className="an-kpi-value" aria-describedby={description}>0<small>{unit}</small></span> : <AnalyticsLink className="an-kpi-value" to={drill(definition.dimension, extra)} aria-describedby={description}>{analyticsNumber(metric.value)}<small>{unit}</small></AnalyticsLink>}<AnalyticsSparkline metric={metric} label={title} /></div>
    <div className="an-kpi-bottom"><span className="an-trend">{trend !== null ? `${trend > 0 ? '+' : ''}${metric.changePercent}% so với kỳ trước` : metric.previousValue === null ? 'Trong khoảng đã chọn' : `Kỳ trước: ${analyticsNumber(metric.previousValue)} ${unit}`}</span></div>
  </article>;
}
function VolumeChart({ data, drill, fees = false }: { data: AnalyticsReport; drill: Drill; fees?: boolean }) {
  const [parameters] = useSearchParams();
  return <AnalyticsTimeChart data={data} drill={drill} chartLink={AnalyticsLink} fees={fees} bucket={parameters.get('bucket') ?? 'day'} />;
}
function Breakdown({ data, drill, dimension }: { data: AnalyticsReport; drill: Drill; dimension: string }) {
  const [feeTrend, setFeeTrend] = useState(false);
  const total = data.shipments.total;
  const all = !dimension || dimension === 'shipments';
  const show = (name: string) => all || dimension === name || dimension === 'fees' && name === 'services';
  return <div className="an-breakdown">
    {show('status') && <section className="an-card an-status-card"><div className="an-card-heading"><div><span className="an-eyebrow">PHÂN BỔ</span><h2>Trạng thái vận đơn</h2></div><Info title="Trạng thái vận đơn" text="Trạng thái hiện tại của các vận đơn đặt trong kỳ. Đã nhận tại Station không phải đã giao hàng." /></div><div className="an-status-content"><AnalyticsStatusDonut data={data} /><div className="an-status-legend">{data.status.map(s => { const content = <><span className={'an-dot an-dot-' + s.key} /><span>{statusLabel[s.key]}<small>{fraction(s.shipments, total)}</small></span><strong>{analyticsNumber(s.shipments)}</strong></>; return s.shipments === '0' ? <div className="an-status-zero" key={s.key}>{content}</div> : <AnalyticsLink key={s.key} to={drill('status', { status: s.key })}>{content}</AnalyticsLink>; })}</div></div></section>}
    {(all || dimension === 'fees') && <section className="an-card an-volume-card"><div className="an-card-heading"><div><span className="an-eyebrow">XU HƯỚNG</span><h2>{dimension === 'fees' || feeTrend ? 'Cước đã xác nhận theo kỳ' : 'Lượng vận đơn theo thời gian'}</h2></div><AnalyticsLink to={drill(dimension === 'fees' || feeTrend ? 'fees' : 'shipments')}>Phân tích</AnalyticsLink></div>{dimension !== 'fees' && <select className="an-chart-selector" aria-label="Chỉ số biểu đồ" value={feeTrend ? 'fees' : 'shipments'} onChange={e => setFeeTrend(e.target.value === 'fees')}><option value="shipments">Số vận đơn Booking</option><option value="fees">Cước đã xác nhận</option></select>}<VolumeChart data={data} drill={drill} fees={dimension === 'fees' || feeTrend} /></section>}
    {show('routes') && <section className="an-card"><div className="an-card-heading"><div><span className="an-eyebrow">KẾT NỐI</span><h2>Tuyến gửi hàng</h2></div><AnalyticsLink to={drill('routes')}>Chi tiết</AnalyticsLink></div>{!data.routes.length ? <p className="an-empty-small">Chưa có vận đơn trên tuyến nào trong bộ lọc.</p> : <div className="an-route-list">{data.routes.map(r => <AnalyticsLink key={r.origin + r.destination} to={drill('routes', { origin: r.origin, destination: r.destination })}><div><strong>{r.origin} <span>→</span> {r.destination}</strong><span>{analyticsNumber(r.shipments)} vận đơn</span></div><span className="an-track"><span style={{ width: `${scale(r.shipments, BigInt(total))}%` }} /></span><small>{money(r.confirmedFeeVnd)} cước xác nhận · {fraction(r.shipments, total)}</small></AnalyticsLink>)}</div>}<p className="an-footnote">Mã vùng theo dataset đã duyệt; không biểu diễn tọa độ địa lý.</p></section>}
    {show('parcels') && <section className="an-card"><div className="an-card-heading"><div><span className="an-eyebrow">KIỆN HÀNG</span><h2>Trọng lượng thực khai báo</h2></div><AnalyticsLink to={drill('parcels')}>Chi tiết</AnalyticsLink></div><div className="an-weight-list">{data.parcels.weight.map(w => <AnalyticsLink key={w.key} to={drill('parcels', { weight: w.key })}><span>{weightLabel[w.key]}</span><span className="an-track"><span style={{ width: `${scale(w.parcels, BigInt(data.parcels.count))}%` }} /></span><strong>{analyticsNumber(w.parcels)} kiện</strong></AnalyticsLink>)}</div><AnalyticsLink className="an-multi" to={drill('parcels', { multi: 'true' })}><strong>{analyticsNumber(data.parcels.multiParcelShipments)}</strong> vận đơn có nhiều kiện</AnalyticsLink><p className="an-footnote">Chọn một nhóm để xem vận đơn có kiện thuộc nhóm đó.</p></section>}
    {show('services') && <section className="an-card"><div className="an-card-heading"><div><span className="an-eyebrow">DỊCH VỤ</span><h2>Dịch vụ đang sử dụng</h2></div><AnalyticsLink to={drill('services')}>Chi tiết</AnalyticsLink></div>{data.services.length ? data.services.map(s => <AnalyticsLink key={s.code} className="an-service" to={drill('services', { service: s.code })}><span className="an-service-icon" aria-hidden="true"><MetricIcon id="BOOKED_SHIPMENTS" /></span><span><strong>Đường bộ tiêu chuẩn</strong><small>{analyticsNumber(s.shipments)} vận đơn · {fraction(s.shipments, total)}</small><small>{money(s.confirmedFeeVnd)} cước đã xác nhận</small></span></AnalyticsLink>) : <p className="an-empty-small">Chưa có dịch vụ phát sinh trong kỳ.</p>}</section>}
    {dimension === 'fees' && <section className="an-card an-fee-card"><div className="an-card-heading"><div><span className="an-eyebrow">CHI PHÍ</span><h2>Cước & nghĩa vụ</h2></div><Info title="Cước và nghĩa vụ" text="Cước xác nhận và nghĩa vụ UNPAID là hai cách nhìn cùng nghĩa vụ; không cộng chúng lại và không coi là tiền đã thu." /></div>{(['CONFIRMED_FEES', 'UNPAID_OBLIGATIONS'] as MetricId[]).map(id => <AnalyticsLink key={id} to={drill('fees', id === 'UNPAID_OBLIGATIONS' ? { obligation: 'UNPAID' } : {})}><span>{metricDefinitions[id].title}</span><strong>{money(data.metrics.find(m => m.id === id)!.value)}</strong></AnalyticsLink>)}<p className="an-footnote">UNPAID · Chức năng thu tiền chưa mở. Không phải doanh thu đã thu.</p></section>}
  </div>;
}
function Overview({ data, drill, recent, parameters, change, fees, setFees }: { data: AnalyticsReport; drill: Drill; recent: ReactNode; parameters: URLSearchParams; change: (p: URLSearchParams) => void; fees: boolean; setFees: (fees: boolean) => void }) {
  return <div className="an-overview-charts an-focused-layout">
    <section className="an-card an-volume-card"><div className="an-card-heading"><div><h2>Hoạt động gửi hàng</h2><p>Theo ngày tạo vận đơn</p></div><div className="an-chart-controls"><div role="group" aria-label="Chỉ số biểu đồ" className="an-measure-switch"><button type="button" aria-pressed={!fees} onClick={() => setFees(false)}>Vận đơn</button><button type="button" aria-pressed={fees} onClick={() => setFees(true)}>Cước</button></div><select aria-label="Nhóm thời gian" value={parameters.get('bucket') ?? 'day'} onChange={event => { const next = new URLSearchParams(parameters); next.set('bucket', event.target.value); next.delete('page'); change(next); }}><option value="day">Theo ngày</option><option value="week">Theo tuần</option><option value="month">Theo tháng</option></select></div></div><VolumeChart key={String(fees)} data={data} drill={drill} fees={fees} /></section>
    <Breakdown data={data} drill={drill} dimension="status" />
    <section className="an-card an-route-volume"><div className="an-card-heading"><div><h2>Sản lượng theo tuyến</h2><p>Tỷ trọng vận đơn đã đặt</p></div></div><AnalyticsRouteChart data={data} drill={drill} chartLink={AnalyticsLink} /></section>
    <section className="an-card an-route-cost"><div className="an-card-heading"><div><h2>Cước theo tuyến</h2><p>Cước xác nhận, không phải tiền đã thu</p></div></div><AnalyticsRouteChart data={data} drill={drill} chartLink={AnalyticsLink} fees /></section>
    <section className="an-card an-parcel-chart"><div className="an-card-heading"><div><h2>Kiện hàng</h2><p>Trọng lượng thực khai báo</p></div><strong className="an-parcel-total">{analyticsNumber(data.parcels.count)} <small>kiện</small></strong></div><AnalyticsParcelChart data={data} drill={drill} chartLink={AnalyticsLink} /><p className="an-footnote">{analyticsNumber(data.parcels.multiParcelShipments)} vận đơn có nhiều kiện. Chọn nhóm để xem vận đơn có kiện phù hợp.</p></section>
    {recent}
    <section className="an-card an-parcel-timeline"><div className="an-card-heading"><div><h2>Sản lượng kiện hàng</h2><p>Kiện thuộc vận đơn tạo trong kỳ</p></div></div><AnalyticsTimeChart data={data} drill={drill} chartLink={AnalyticsLink} parcels bucket={parameters.get('bucket') ?? 'day'} /></section>
  </div>;
}
function AnalyticsCapabilities() {
  return <details className="an-capabilities"><summary>Phạm vi số liệu & thông tin cần chú ý</summary><div>
      <section><h2>Vận chuyển & giao hàng</h2><p>Đang vận chuyển, giao thành công, giao thất bại, hoàn hàng và ngoại lệ kiện: chưa có nguồn dữ liệu được hỗ trợ. Biểu đồ hiện thống kê vận đơn đã đặt, không phải hiệu suất giao hàng.</p></section>
      <section><h2>Thanh toán & COD</h2><p>Cước đã thanh toán và COD dự kiến/đã thu/đã chuyển trả: chưa hỗ trợ. UNPAID không phải xác nhận đã thu tiền; chưa có thao tác thanh toán.</p></section>
      <section><h2>Đơn cần chú ý & thông báo</h2><p>Chưa kết nối nguồn yêu cầu xử lý, thông báo trạng thái, phụ phí, kết quả hỗ trợ và nhắc thanh toán. Chưa thể xác nhận số lượng hoặc khẳng định không có vấn đề.</p><Link to="/personal/action-required">Xem phạm vi xử lý</Link></section>
    </div></details>;
}
function ShipmentTable({ data, parameters, change, compact = false, drill }: { data: AnalyticsReport; parameters: URLSearchParams; change: (p: URLSearchParams) => void; compact?: boolean; drill: Drill }) {
  const location = useLocation(), returnTo = (location.pathname === '/personal' ? '/personal/overview' : location.pathname) + '?' + parameters;
  const returnState = { analyticsReturn: returnTo, analyticsOverview: overviewReturn(location.pathname, location.search, location.state), analyticsMeasure: (location.state as { analyticsMeasure?: unknown } | null)?.analyticsMeasure };
  const [search, setSearch] = useState(parameters.get('search') ?? '');
  function submit(e: FormEvent<HTMLFormElement>) { e.preventDefault(); const p = new URLSearchParams(parameters);  if (search.trim()) p.set('search', search.trim()); else p.delete('search'); p.delete('page'); change(p); }
  function page(offset: number) { const p = new URLSearchParams(parameters); p.set('page', String(data.shipments.page + offset)); change(p); }
  const recent = compact && data.shipments.page === 0 && !parameters.has('search') && (!parameters.has('sort') || parameters.get('sort') === 'NEWEST');
  return <section className={'an-card an-source' + (compact ? ' an-recent' : '')}>
    <div className="an-card-heading"><h2>{compact ? recent ? 'Vận đơn gần đây' : 'Vận đơn trong bộ lọc' : 'Vận đơn phù hợp'}</h2>{compact ? <AnalyticsLink to={drill('shipments')}>Xem tất cả</AnalyticsLink> : <span className="an-row-count">{analyticsNumber(data.shipments.total)} vận đơn</span>}</div>
    {!compact && <form className="an-table-filters" onSubmit={submit}><label>Tìm mã vận đơn<input value={search} maxLength={64} onChange={event => setSearch(event.target.value)} placeholder="AP-SIM-…" /></label><label>Sắp xếp<select aria-label="Sắp xếp" value={parameters.get('sort') ?? 'NEWEST'} onChange={event => { const next = new URLSearchParams(parameters); next.set('sort', event.target.value); next.delete('page'); change(next); }}><option value="NEWEST">Mới nhất</option><option value="OLDEST">Cũ nhất</option><option value="FEE_DESC">Cước giảm dần</option><option value="FEE_ASC">Cước tăng dần</option></select></label><button className="pw-secondary">Tìm vận đơn</button></form>}
    <div className="an-table-scroll" role="region" aria-label="Bảng chi tiết vận đơn" tabIndex={0}><table><thead><tr><th>{compact ? 'Mã / Ngày tạo' : 'Mã vận đơn'}</th>{!compact && <th>Ngày tạo</th>}<th>Trạng thái</th>{!compact && <><th>Tuyến</th><th>Kiện / Trọng lượng</th></>}<th>Cước xác nhận</th>{!compact && <th>Nghĩa vụ UNPAID</th>}<th>Nhãn</th></tr></thead><tbody>
      {(compact ? data.shipments.items.slice(0, 5) : data.shipments.items).map(shipment => <tr key={shipment.id}>
        <td><Link to={'/personal/shipments/' + shipment.id} state={returnState} title={shipment.code} aria-label={shipment.code}>{compact && shipment.code.length > 24 ? shipment.code.slice(0, 7) + '…' + shipment.code.slice(-8) : shipment.code}</Link>{compact && <small className="an-booked-time">{displayDate(shipment.bookedAt)}</small>}</td>
        {!compact && <td>{displayDate(shipment.bookedAt)} <small className="an-booked-time">{new Date(shipment.bookedAt).toLocaleTimeString('vi-VN', { timeZone: data.timeZone, hour: '2-digit', minute: '2-digit' })}</small></td>}
        <td><span className={'an-state an-state-' + shipment.status}>{statusLabel[shipment.status]}</span></td>
        {!compact && <><td>{shipment.origin} → {shipment.destination}</td><td>{analyticsNumber(shipment.parcels)} kiện / {analyticsNumber(shipment.weightGrams)} g</td></>}
        <td><span className="an-mobile-label">Cước: </span>{money(shipment.confirmedFeeVnd)}</td>{!compact && <td>{money(shipment.unpaidVnd)}</td>}
        <td><Link to={'/personal/shipments/' + shipment.id + '?tab=documents'} state={returnState} aria-label={'Xem nhãn ' + shipment.code}>{compact ? 'Nhãn' : 'Xem nhãn'}</Link></td>
      </tr>)}
    </tbody></table></div>
    {!data.shipments.items.length && <p className="an-empty-small">Không có vận đơn phù hợp với trang và bộ lọc này.</p>}
    {!compact && !data.shipments.items.length && data.shipments.page > 0 && <button className="pw-secondary" onClick={() => { const next = new URLSearchParams(parameters); next.delete('page'); change(next); }}>Về trang đầu</button>}
    {!compact && <nav className="an-pagination" aria-label="Phân trang phân tích"><span>Trang {data.shipments.page + 1} · 20 vận đơn / trang</span><button className="pw-secondary" disabled={data.shipments.page === 0} onClick={() => page(-1)}>Trang trước</button><button className="pw-secondary" disabled={!data.shipments.hasNext} onClick={() => page(1)}>Trang sau</button></nav>}
  </section>;
}

export function PersonalAnalyticsDashboard({ subject, view = 'overview' }: { subject: Subject; view?: 'overview' | 'fees' }) {
  const [raw, setParameters] = useSearchParams(), location = useLocation();
  const parameters = new URLSearchParams(raw); if (!parameters.has('from') && !parameters.has('until')) { const dates = defaultDates(); parameters.set('from', dates.from); parameters.set('until', dates.until); }
  const billing = view === 'fees', overview = !billing && !location.pathname.startsWith('/personal/overview/');
  const redirect = overview && rowFilters.some(key => parameters.has(key));
  const query = parameters.toString(), [result, setResult] = useState<{ query: string; report?: AnalyticsReport; error?: string }>(), [retry, setRetry] = useState(0);
  const data = result?.query === query ? result.report : undefined, error = result?.query === query ? result.error ?? '' : '';
  useEffect(() => { if (redirect) return; let current = true; customerClient.readAnalytics(new URLSearchParams(query)).then(value => { if (current) setResult({ query, report: value }); }).catch(failure => { if (current) { const code = failure instanceof CustomerApiError ? failure.code : ''; setResult({ query, error: code === 'ANALYTICS_RANGE_TOO_LARGE' ? 'Khoảng phân tích có quá nhiều vận đơn. Hãy chọn khoảng thời gian ngắn hơn.' : code === 'VALIDATION_FAILED' ? 'Bộ lọc không hợp lệ. Hãy đặt lại khoảng thời gian và điều kiện phân tích.' : code === 'PILOT_DISABLED' ? 'Nguồn dữ liệu Analytics chưa được mở trong môi trường này.' : 'Không tải được dữ liệu phân tích. Số liệu chưa được xác nhận; vui lòng thử lại.' }); } }); return () => { current = false; }; }, [query, retry, redirect]);
  function change(p: URLSearchParams) { if (p.toString() === query) return; setResult(undefined); setParameters(p, { state: location.state }); }
  const drill: Drill = (_name, extra = {}) => { const p = new URLSearchParams(parameters); p.delete('page'); for (const [key, value] of Object.entries(extra)) p.set(key, value); return '/personal/overview/shipments?' + p; };
  const back = overviewReturn(location.pathname, '?' + parameters, location.state);
  const filtered = ['status', 'service', 'origin', 'day', 'weight', 'multi', 'obligation', 'search', 'sort'].filter(key => parameters.has(key));
  const empty = data?.shipments.total === '0', fees = (location.state as { analyticsMeasure?: unknown } | null)?.analyticsMeasure === 'fees';
  const title = billing ? 'Cước phí & Thống kê' : overview ? 'Tổng quan' : parameters.has('weight') || parameters.has('multi') ? 'Vận đơn theo kiện hàng' : parameters.get('status') === 'AT_STATION' ? 'Vận đơn đã tiếp nhận' : parameters.get('status') === 'WAITING_FOR_HANDOVER' ? 'Vận đơn chờ bàn giao' : parameters.has('day') ? 'Vận đơn theo thời gian' : parameters.has('obligation') ? 'Vận đơn chưa thanh toán' : 'Vận đơn trong kỳ';
  if (redirect) return <Navigate to={'/personal/overview/shipments?' + parameters} replace state={{ analyticsOverview: overviewReturn('/personal/overview/shipments', '?' + parameters, null), analyticsMeasure: (location.state as { analyticsMeasure?: unknown } | null)?.analyticsMeasure }} />;
  return <div className={'an-dashboard' + (overview ? ' an-compact-overview' : billing ? '' : ' an-drill-list')}><header className={'an-header' + (overview ? ' an-overview-header' : '')}><div>{!overview && !billing && <nav aria-label="Đường dẫn phân tích"><Link to={back} state={location.state}>{back.startsWith('/personal/reports') ? 'Báo cáo gửi hàng' : back.startsWith('/personal/billing') ? 'Cước & Thanh toán' : 'Tổng quan'}</Link><span aria-hidden="true"> / </span><span>Vận đơn phù hợp</span></nav>}<h1 tabIndex={-1}>{title}</h1>{overview ? <p>Xin chào {subject.displayName}.</p> : !billing && <p>{displayDate(parameters.get('from') ?? '')} – {displayDate(moveDate(parameters.get('until') ?? '', -1))} · Theo ngày tạo vận đơn</p>}</div>{overview && <Filters parameters={parameters} change={change} />}{(overview && !empty || billing) && <Link className="pw-primary" to="/personal/create">+ Tạo vận đơn</Link>}</header>
    {billing && <Filters parameters={parameters} change={change} />}
    {!overview && filtered.length > 0 && <div className="an-selection" aria-label="Điều kiện đang chọn">{filtered.map(key => <button type="button" key={key} aria-label={'Bỏ lọc ' + filterNames[key]} onClick={() => { const p = new URLSearchParams(parameters); p.delete(key); p.delete('page'); if (key === 'origin') p.delete('destination'); change(p); }}>{key === 'status' ? statusLabel[parameters.get(key) as keyof typeof statusLabel] ?? parameters.get(key) : key === 'service' ? 'Đường bộ tiêu chuẩn' : key === 'search' ? 'Mã: ' + parameters.get(key) : key === 'sort' ? ({ NEWEST: 'Mới nhất', OLDEST: 'Cũ nhất', FEE_DESC: 'Cước giảm dần', FEE_ASC: 'Cước tăng dần' }[parameters.get(key)!] ?? parameters.get(key)) : key === 'origin' ? parameters.get(key) + ' → ' + parameters.get('destination') : key === 'day' ? 'Nhóm thời gian: ' + displayDate(parameters.get(key)!) : key === 'weight' ? 'Có kiện: ' + weightLabel[parameters.get(key)!] : key === 'multi' ? 'Vận đơn nhiều kiện' : 'Chưa thanh toán'} <span aria-hidden="true">×</span></button>)}</div>}
    {error ? <div className="an-card an-error"><h2>Chưa thể hiển thị dữ liệu</h2><p role="alert">{error}</p><button className="pw-secondary" onClick={() => { setResult(undefined); setRetry(r => r + 1); }}>Thử lại Analytics</button>{!overview && <button className="pw-secondary" onClick={() => change(new URLSearchParams(defaultDates()))}>Xóa bộ lọc</button>}</div> : !data ? <section className="an-loading" role="status" aria-label="Đang tải Analytics"><p>Đang tải dữ liệu…</p>{(overview || billing) && <div>{Array.from({ length: overview ? 4 : 3 }, (_, i) => <span key={i} />)}</div>}</section> : <>
      <div className="an-freshness"><span>Cập nhật {new Date(data.generatedAt).toLocaleTimeString('vi-VN', { timeZone: data.timeZone, hour: '2-digit', minute: '2-digit' })}</span>{overview && <button className="an-refresh" onClick={() => { setResult(undefined); setRetry(r => r + 1); }}>Làm mới</button>}</div>
      {(overview || billing) && <section className={'an-kpi-strip' + (overview ? ' an-overview-kpis' : ' an-fees-kpis')} aria-label="Chỉ số tổng quan">{data.metrics.filter(metric => overview ? overviewMetrics.includes(metric.id) : ['BOOKED_SHIPMENTS', 'CONFIRMED_FEES', 'UNPAID_OBLIGATIONS'].includes(metric.id)).map(metric => <Kpi key={metric.id} metric={metric} drill={drill} />)}</section>}
      {overview ? empty ? <section className="an-empty"><span className="an-empty-icon" aria-hidden="true"><MetricIcon id="BOOKED_SHIPMENTS" /></span><div><h2>Chưa có vận đơn trong khoảng đã chọn</h2><p>Thử khoảng thời gian khác hoặc bắt đầu với vận đơn đầu tiên.</p><Link to="/personal/create" className="pw-primary">+ Tạo vận đơn</Link></div></section> : <Overview key={query} data={data} drill={drill} parameters={parameters} change={change} fees={fees} setFees={value => setParameters(parameters, { replace: true, state: { ...location.state, analyticsMeasure: value ? 'fees' : 'shipments' } })} recent={<ShipmentTable key={query} data={data} parameters={parameters} change={change} compact drill={drill} />} /> : <>{billing && !empty && <Breakdown key={query} data={data} drill={drill} dimension="fees" />}<ShipmentTable key={query} data={data} parameters={parameters} change={change} drill={drill} /></>}
      {overview && <AnalyticsCapabilities />}
    </>}
  </div>;
}
