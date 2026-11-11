import { useId, useState } from 'react';
import type { ComponentType, ComponentProps } from 'react';
import { Link } from 'react-router-dom';
import type { AnalyticsMetric, AnalyticsReport } from './analytics-data';
import { axisLabel, chartAxis, chartGeometry, chartPercent } from './chart-math';

type Drill = (dimension: string, extra?: Record<string, string>) => string;
type ChartLink = ComponentType<ComponentProps<typeof Link>>;
const exact = (value: string) => BigInt(value).toLocaleString('vi-VN');
const date = (value: string) => new Date(value).toLocaleDateString('vi-VN', { day: '2-digit', month: '2-digit', year: 'numeric', timeZone: 'Asia/Ho_Chi_Minh' });

export function AnalyticsTimeChart({ data, drill, chartLink: ChartLink, fees = false, parcels = false, bucket = 'day' }: {
  data: AnalyticsReport; drill: Drill; chartLink: ChartLink; fees?: boolean; parcels?: boolean; bucket?: string;
}) {
  const [active, setActive] = useState<number>();
  const id = useId().replaceAll(':', ''), title = fees ? 'Cước đã xác nhận' : parcels ? 'Kiện thuộc vận đơn trong kỳ' : 'Vận đơn đã đặt';
  const metric = data.metrics.find(m => m.id === (fees ? 'CONFIRMED_FEES' : parcels ? 'CREATED_PARCELS' : 'BOOKED_SHIPMENTS'))!;
  const series = parcels ? metric.points.map(p => ({ date: p.date, value: p.value, unpaidVnd: '0' })) : data.volume.map(p => ({ date: p.date, value: fees ? p.confirmedFeeVnd : p.shipments, unpaidVnd: p.unpaidVnd }));
  const axis = chartAxis(series.map(p => p.value)), geometry = chartGeometry(series.map(p => p.value), axis.maximum), selected = active === undefined ? undefined : series[active];
  const unit = fees ? '₫' : parcels ? 'kiện' : 'vận đơn', group = ({ day: 'ngày', week: 'tuần', month: 'tháng' } as Record<string, string>)[bucket] ?? 'ngày';
  const ticks = [...new Set([0, Math.floor((series.length - 1) / 2), series.length - 1])].filter(index => index >= 0);
  return <div className={'ac-time' + (fees ? ' ac-time-fees' : parcels ? ' ac-time-parcels' : '')}>
    <div className="ac-chart-summary"><strong>{exact(metric.value)} <small>{unit}</small></strong><span>{metric.previousValue === null ? 'Không đối chiếu kỳ trước' : `Kỳ trước: ${exact(metric.previousValue)} ${unit}`}</span></div>
    <div className="ac-chart-legend"><span><i />{title}</span><small>Theo {group} · Giờ Việt Nam</small></div>
    <div className="an-chart-context an-sr-only" role="status" aria-live="polite">{selected ? `${date(selected.date)} · ${exact(selected.value)} ${unit}${fees ? ' cước xác nhận · ' + exact(selected.unpaidVnd) + ' ₫ UNPAID' : ' Booking'}` : `Theo thời điểm tạo vận đơn, nhóm ${group}`}</div>
    <div className="ac-chart-frame">
      <div className="ac-y-axis" aria-hidden="true">{axis.ticks.map(tick => <span key={tick.toString()}>{axisLabel(tick)}</span>)}</div>
      <div className="an-time-chart ac-plot" aria-label={fees ? 'Cước đã xác nhận theo thời gian' : parcels ? 'Kiện thuộc vận đơn theo thời gian' : 'Số vận đơn Booking theo thời gian'} onMouseLeave={() => setActive(undefined)} onBlur={event => { if (!event.currentTarget.contains(event.relatedTarget)) setActive(undefined); }}>
        <svg className="ac-plot-svg" viewBox="0 0 1000 180" preserveAspectRatio="none" aria-hidden="true">
          <defs><linearGradient id={'area-' + id} x1="0" y1="0" x2="0" y2="1"><stop offset="0%" stopColor={parcels ? '#00858A' : '#2563EB'} stopOpacity=".26" /><stop offset="100%" stopColor={parcels ? '#00858A' : '#2563EB'} stopOpacity=".02" /></linearGradient></defs>
          {[0, 45, 90, 135, 180].map(y => <line key={y} x1="0" x2="1000" y1={y} y2={y} className="ac-grid-line" vectorEffect="non-scaling-stroke" />)}
          {!fees && <><path className="ac-area" d={geometry.area} fill={`url(#area-${id})`} /><path className="ac-line" d={geometry.line} vectorEffect="non-scaling-stroke" /></>}
          {active !== undefined && geometry.points[active] && <line className="ac-crosshair" x1={geometry.points[active].x} x2={geometry.points[active].x} y1="0" y2="180" vectorEffect="non-scaling-stroke" />}
        </svg>
        <div className="ac-hit-grid" style={{ gridTemplateColumns: `repeat(${series.length}, minmax(1px,1fr))` }}>
          {series.map((p, index) => <ChartLink key={p.date} className={'an-chart-bar ac-point' + (active === index ? ' is-active' : '')} to={drill(fees ? 'fees' : parcels ? 'parcels' : 'shipments', { day: p.date })} aria-label={`${date(p.date)}: ${exact(p.value)} ${unit}. Xem vận đơn đóng góp`} onMouseEnter={() => setActive(index)} onFocus={() => setActive(index)} onTouchStart={() => setActive(index)}>
            {fees ? <span className="ac-column" style={{ height: chartPercent(p.value, axis.maximum) + '%' }} /> : <span className="ac-marker" style={{ bottom: chartPercent(p.value, axis.maximum) + '%' }} />}
          </ChartLink>)}
        </div>
        {selected && <div className={'ac-tooltip' + ((active ?? 0) < series.length / 2 ? ' ac-tooltip-right' : '')} role="presentation"><span>{date(selected.date)}</span><strong>{exact(selected.value)} {unit}</strong>{fees && <small>Chưa thanh toán: {exact(selected.unpaidVnd)} ₫</small>}<small>Chọn để xem vận đơn đóng góp</small></div>}
      </div>
    </div>
    <div className="ac-x-axis" aria-hidden="true">{ticks.map(index => <span key={index} style={{ left: (index + .5) * 100 / series.length + '%' }}>{date(series[index].date).slice(0, 5)}</span>)}</div>
  </div>;
}

export function AnalyticsStatusDonut({ data }: { data: AnalyticsReport }) {
  const total = BigInt(data.shipments.total), accepted = data.status.find(s => s.key === 'AT_STATION')!.shipments;
  const share = chartPercent(accepted, total);
  return <div className="an-donut ac-status-donut" role="img" aria-label={`${exact(accepted)} trên ${exact(data.shipments.total)} vận đơn đã được tiếp nhận`}>
    <svg viewBox="0 0 120 120" aria-hidden="true"><circle className="ac-ring-track" cx="60" cy="60" r="48" /><circle className="ac-ring-accepted" cx="60" cy="60" r="48" pathLength="100" strokeDasharray={`${share} ${100 - share}`} transform="rotate(-90 60 60)" /></svg>
    <span><strong>{exact(data.shipments.total)}</strong><small>vận đơn</small></span>
  </div>;
}

export function AnalyticsParcelChart({ data, drill, chartLink: ChartLink }: { data: AnalyticsReport; drill: Drill; chartLink: ChartLink }) {
  const names: Record<string, string> = { UP_TO_500G: 'Đến 0,5 kg', UP_TO_5KG: 'Trên 0,5 đến 5 kg', OVER_5KG: 'Trên 5 kg' };
  return <div className="ac-parcel-bars">{data.parcels.weight.map(group => <ChartLink key={group.key} to={drill('parcels', { weight: group.key })} aria-label={`${names[group.key]}: ${exact(group.parcels)} kiện. Xem vận đơn có kiện phù hợp`}>
    <span>{names[group.key]}</span><strong>{exact(group.parcels)} <small>kiện</small></strong><span className="ac-parcel-track" aria-hidden="true"><span style={{ width: chartPercent(group.parcels, BigInt(data.parcels.count)) + '%' }} /></span>
  </ChartLink>)}</div>;
}


export function AnalyticsSparkline({ metric, label }: { metric: AnalyticsMetric; label: string }) {
  const id = useId().replaceAll(':', ''), max = metric.points.reduce((n, point) => BigInt(point.value) > n ? BigInt(point.value) : n, 0n);
  const geometry = chartGeometry(metric.points.map(p => p.value), max || 1n);
  if (!metric.points.length) return <span className="an-spark-missing">Chưa có chuỗi dữ liệu</span>;
  return <svg className="an-kpi-spark" viewBox="-10 -15 1020 210" preserveAspectRatio="none" role="img" aria-label={`Xu hướng ${label.toLowerCase()}: ${metric.points.length} mốc theo ngày tạo vận đơn`} data-values={metric.points.map(p => p.value).join(',')}>
    <defs><linearGradient id={'spark-' + id} x1="0" y1="0" x2="0" y2="1"><stop offset="0%" stopColor="currentColor" stopOpacity=".22" /><stop offset="100%" stopColor="currentColor" stopOpacity="0" /></linearGradient></defs>
    <path d={geometry.area} fill={`url(#spark-${id})`} /><path className="an-spark-stroke" d={geometry.line} fill="none" stroke="currentColor" strokeWidth="1.8" vectorEffect="non-scaling-stroke" />
    {geometry.points.length === 1 && <circle cx={geometry.points[0].x} cy={geometry.points[0].y} r="13" fill="currentColor" />}
  </svg>;
}

export function AnalyticsRouteChart({ data, drill, chartLink: ChartLink, fees = false }: { data: AnalyticsReport; drill: Drill; chartLink: ChartLink; fees?: boolean }) {
  const field = fees ? 'confirmedFeeVnd' : 'shipments', total = fees ? data.metrics.find(m => m.id === 'CONFIRMED_FEES')!.value : data.shipments.total;
  const routes = [...data.routes].sort((a, b) => BigInt(a[field]) === BigInt(b[field]) ? (a.origin + a.destination).localeCompare(b.origin + b.destination) : BigInt(a[field]) > BigInt(b[field]) ? -1 : 1);
  return <div className={'ac-route-bars' + (fees ? ' ac-route-fees' : '')}>{routes.map(route => <ChartLink key={route.origin + route.destination} to={drill('routes', { origin: route.origin, destination: route.destination })}>
    <span className="ac-route-name">{route.origin}<span aria-hidden="true"> → </span>{route.destination}</span><strong>{exact(route[field])} <small>{fees ? '₫' : 'vận đơn'}</small></strong>
    <span className="ac-route-track" aria-hidden="true"><span style={{ width: chartPercent(route[field], BigInt(total)) + '%' }} /></span><small>{(Math.round(chartPercent(route[field], BigInt(total)) * 10) / 10).toLocaleString('vi-VN')}% {fees ? 'tổng cước xác nhận' : 'vận đơn trong kỳ'}</small>
  </ChartLink>)}</div>;
}
