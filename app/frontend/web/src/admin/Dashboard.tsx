import { useEffect, useMemo, useState } from 'react';
import { useLocation, useNavigate, useSearchParams } from 'react-router-dom';
import { adminSession, AdminSessionError } from './session';
import ShipmentOversight from './ShipmentOversight';
import { date, daysBefore, displayDate, displayNumber, metricLabels, parseContext, parseDashboard, parsePlatform, parseShipments, queryParameters, todayVietnam } from './data';
import type { AdminContext, Dashboard as Report, Metric, MetricKey, PlatformStatus, Point, Query, Shipment, ShipmentPage } from './data';
import { axisLabel, chartAxis, chartGeometry, chartPercent } from '../customer/chart-math';
import InternalShell, { SourceState } from '../internal/InternalShell';
import { adminTabs, canOpenModule, resolveAdminModule, sectionState, snapshotState } from '../internal/navigation';
import { DependencyView, MetricMetadata, ModuleDependency } from './ConsoleViews';
import MerchantApplications from './MerchantApplications';
import MerchantOrganizations from './MerchantOrganizations';
import MerchantAudit from './MerchantAudit';
import AdminWorkCenter from './AdminWorkCenter';
import AuthorityView from './AuthorityView';
import { OperationalRisk } from './OverviewSources';
import DashboardV2View from './DashboardV2';
import { parseGovernanceContext } from './governance';

const text = (error: unknown) => error instanceof AdminSessionError ? error.code : 'SERVICE_UNAVAILABLE';
function problem(code: string) {
  return code === 'SNAPSHOT_EXPIRED' ? 'Số liệu đã thay đổi hoặc phiên phân tích đã hết hạn. Làm mới để xem danh sách khớp với KPI.' : code === 'ACCESS_DENIED' ? 'Bạn chưa có quyền xem dữ liệu này.' : code === 'VALIDATION_FAILED' ? 'Bộ lọc chưa hợp lệ. Kiểm tra khoảng thời gian và trạng thái.' : 'Chưa tải được dữ liệu. Vui lòng thử lại.';
}
function Trend({ points, metric, title, color = '#0B2D5B' }: { points: Point[]; metric: MetricKey; title: string; color?: string }) {
  const series = points.filter(p => p.metric === metric), values = series.map(p => p.value), axis = chartAxis(values);
  return <div className="ap-chart"><svg viewBox="0 0 640 210" role="img" aria-label={title}>
    {axis.ticks.map((tick, index) => <g key={index}><line x1="55" x2="625" y1={15 + index * 39} y2={15 + index * 39} stroke="#e4eaf1" strokeDasharray="3 5" /><text x="45" y={19 + index * 39} textAnchor="end">{axisLabel(tick)}</text></g>)}
    {series.map((p, index) => { const width = 570 / series.length, height = chartPercent(p.value, axis.maximum) * 1.56; return <rect key={p.from} x={55 + index * width + width * .18} y={171 - height} width={Math.max(1, width * .64)} height={height} rx="3" fill={color}><title>{displayDate(p.from)}: {displayNumber(p.value)} {metric.endsWith('FEES') ? 'VND' : metric === 'PARCELS' ? 'kiện' : 'vận đơn'}</title></rect>; })}
    {[0, Math.floor((series.length - 1) / 2), series.length - 1].filter((n, index, all) => n >= 0 && all.indexOf(n) === index).map(index => <text key={index} x={55 + (index + .5) * 570 / series.length} y="198" textAnchor="middle">{displayDate(series[index].from).slice(0, 5)}</text>)}
  </svg></div>;
}
function Spark({ points, metric, color, grain }: { points: Point[]; metric: MetricKey; color: string; grain: Query['grain'] }) {
  const values = points.filter(p => p.metric === metric).map(p => p.value), axis = chartAxis(values), geometry = chartGeometry(values, axis.maximum);
  const unit = { DAY: 'ngày', WEEK: 'tuần', MONTH: 'tháng' }[grain];
  return <svg viewBox="0 0 1000 180" className="ap-spark" role="img" aria-label={`Xu hướng ${metricLabels[metric].toLowerCase()} theo ${unit} Booking`}><path d={geometry.area} fill={color} opacity=".10" /><path d={geometry.line} fill="none" stroke={color} strokeWidth="2" strokeLinejoin="round" vectorEffect="non-scaling-stroke" /></svg>;
}
function Sources({ report }: { report: Report }) {
  const missing = report.sections.filter(s => s.availability !== 'AVAILABLE');
  const labels: Record<string, string> = { MERCHANT: 'Thẩm định Merchant', NETWORK: 'Mạng lưới Station / Hub / Partner chính thức', SLA: 'SLA và các chặng vận chuyển tiếp theo', PRIORITIES: 'Hàng đợi quyết định và escalation' };
  return <details className="ap-sources"><summary>Nguồn dữ liệu & phạm vi thống kê</summary><p>Thống kê theo thời gian Booking, giờ Việt Nam. Trạng thái được chốt tại thời điểm tính số liệu; Parcel và vận đơn được đếm riêng.</p>{missing.length > 0 && <ul>{missing.map(s => <li key={s.key}><strong>{labels[s.key]}</strong><span>{s.availability === 'POLICY_NOT_CONFIGURED' ? 'Chưa có chính sách và nguồn dữ liệu phù hợp' : 'Nguồn nghiệp vụ chưa được triển khai'}</span></li>)}</ul>}</details>;
}
export default function AdminDashboard({ subject, logout, busy }: { subject: { id: string; displayName: string }; logout: () => void; busy: boolean }) {
  const navigate = useNavigate(), location = useLocation(), activeModule = resolveAdminModule(location.pathname);
  const supportedSurface = !!activeModule && ['ADM-01', 'ADM-08', 'ADM-11', 'ADM-14'].includes(activeModule.id);
  const [params, setParams] = useSearchParams(), today = todayVietnam();
  const from = params.get('from') ?? daysBefore(today, 29), to = params.get('to') ?? today, rawGrain = params.get('grain') ?? 'DAY', status = params.get('shipmentStatus');
  const query = useMemo<Query>(() => ({ from, to, grain: rawGrain as Query['grain'], shipmentStatus: status as Query['shipmentStatus'], regionId: null }), [from, to, rawGrain, status]);
  const valid = date(from) && date(to) && from <= to && to <= today && (Date.parse(to) - Date.parse(from)) / 86400000 < 366 && ['DAY', 'WEEK', 'MONTH'].includes(rawGrain) && (status === null || ['BOOKED', 'AT_STATION'].includes(status)) && [...params.keys()].every(k => ['from', 'to', 'grain', 'shipmentStatus', 'metric', 'page', 'tab'].includes(k)) && ['from', 'to', 'grain', 'shipmentStatus', 'metric', 'page', 'tab'].every(k => params.getAll(k).length <= 1);
  const [context, setContext] = useState<AdminContext | null>(null), [contextError, setContextError] = useState('');
  const [domainCapabilities, setDomainCapabilities] = useState<string[]>([]);
  const [sourceMode, setSourceMode] = useState<'v1' | 'v2' | null>(null);
  const advanced = activeModule?.id === 'ADM-01' && sourceMode === 'v2';
  const moduleAllowed = !!context && canOpenModule(activeModule?.id ?? '', context.permissions, domainCapabilities);
  useEffect(() => {
    let active = true;setDomainCapabilities([]);setSourceMode(null);
    if (context) void adminSession.read('governance-context', new URLSearchParams(), value => parseGovernanceContext(value, subject.id)).then(value => { if (active) { setDomainCapabilities([...new Set(value.assignments.flatMap(assignment => assignment.permissions))]);setSourceMode('v2'); } }).catch(() => { if (active) { setDomainCapabilities([]);setSourceMode('v1'); } });
    return () => { active = false; };
  }, [context, subject.id]);
  const tab = activeModule?.id === 'ADM-11' ? 'finance' : activeModule?.id === 'ADM-14' ? 'platform' : activeModule?.id === 'ADM-08' ? 'overview' : params.get('tab') ?? 'overview';
  const tabs = context ? adminTabs(context.permissions) : [], tabAllowed = tabs.some(t => t.id === tab);
  const [clock, setClock] = useState(Date.now);
  useEffect(() => { const timer = window.setInterval(() => setClock(Date.now()), 30000); return () => window.clearInterval(timer); }, []);
  const [report, setReport] = useState<Report | null>(null), [error, setError] = useState(''), [refresh, setRefresh] = useState(0);
  const [rows, setRows] = useState<ShipmentPage | null>(null), [rowError, setRowError] = useState('');
  const [selectedShipment, setSelectedShipment] = useState<Shipment | null>(null);
  const [platform, setPlatform] = useState<PlatformStatus | null>(null), [platformError, setPlatformError] = useState('');
  const [seriesMetric, setSeriesMetric] = useState<MetricKey>('BOOKINGS');
  const reportKey = queryParameters(query).toString(), currentReport = report && queryParameters(report.query).toString() === reportKey ? report : null;
  const stale = !!currentReport && snapshotState(currentReport.generatedAt, clock) === 'STALE';
  const metrics = currentReport?.sections.flatMap(s => s.metrics) ?? [], metric = metrics.find(m => m.key === (params.get('metric') ?? (activeModule?.id === 'ADM-08' ? 'BOOKINGS' : null)));
  const visibleMetrics = metrics.filter(m => tab === 'overview' || tab === 'finance' && m.unit === 'VND');
  const page = /^(0|[1-9][0-9]{0,4})$/.test(params.get('page') ?? '0') ? Number(params.get('page') ?? '0') : -1;
  useEffect(() => { let active = true; void adminSession.read('context', new URLSearchParams(), v => parseContext(v, subject.id)).then(value => { if (active) setContext(value); }).catch(e => { if (active) setContextError(text(e)); }); return () => { active = false; }; }, [subject.id]);
  useEffect(() => {
    if (!context || !valid || !moduleAllowed || !supportedSurface || sourceMode === null || advanced) return;
    let active = true; setReport(null); setError(''); setRows(null);
    void adminSession.read('dashboard', queryParameters(query), v => parseDashboard(v, context, query)).then(value => { if (active) { setReport(value); setClock(Date.now()); } }).catch(e => { if (active) setError(text(e)); });
    return () => { active = false; };
  }, [context, query, valid, refresh, moduleAllowed, supportedSurface, sourceMode, advanced]);
  useEffect(() => {
    if (!moduleAllowed || !supportedSurface || advanced || sourceMode === null || !context?.permissions.includes('ADMIN_TECHNICAL_READ')) return;
    let active = true; setPlatform(null); setPlatformError('');
    void adminSession.read('platform', new URLSearchParams(), parsePlatform).then(value => { if (active) setPlatform(value); }).catch(e => { if (active) setPlatformError(text(e)); });
    return () => { active = false; };
  }, [context, refresh, moduleAllowed, supportedSurface, sourceMode, advanced]);
  useEffect(() => {
    setRows(null); setRowError(stale ? 'SNAPSHOT_EXPIRED' : ''); setSelectedShipment(null);
    if (advanced || !moduleAllowed || !supportedSurface || !tabAllowed || stale || !currentReport || !metric?.drilldown || page < 0 || page > 10000) return;
    let active = true; const parameters = queryParameters(currentReport.query); parameters.set('snapshotId', currentReport.snapshotId); parameters.set('metric', metric.key); parameters.set('page', String(page)); parameters.set('size', '20');
    void adminSession.read('shipments', parameters, v => parseShipments(v, currentReport, metric.key, page, 20)).then(value => { if (active) setRows(value); }).catch(e => { if (active) setRowError(text(e)); });
    return () => { active = false; };
  }, [currentReport, metric, page, moduleAllowed, supportedSurface, tabAllowed, stale, advanced]);
  function filters(changes: Record<string, string | null>) { const next = new URLSearchParams(params); next.delete('metric'); next.delete('page'); for (const [key, value] of Object.entries(changes)) { if (value) next.set(key, value); else next.delete(key); } setParams(next); }
  function drill(metric: Metric) { if (!metric.drilldown || stale) return; const next = queryParameters(query); if (tab !== 'overview') next.set('tab', tab); next.set('metric', metric.key); setParams(next); }
  function back() { const next = new URLSearchParams(params); next.delete('metric'); next.delete('page'); if (activeModule?.id === 'ADM-08') navigate('/admin?' + next.toString()); else setParams(next); }
  const shipping = currentReport?.sections.find(s => s.key === 'SHIPMENTS' && s.availability === 'AVAILABLE'), finance = currentReport?.sections.find(s => s.key === 'FINANCE' && s.availability === 'AVAILABLE');
  const count = (key: MetricKey) => metrics.find(m => m.key === key)?.value ?? '0';
  const selectedSeries = shipping?.metrics.some(m => m.key === seriesMetric) ? seriesMetric : 'BOOKINGS';
  const colors: Record<MetricKey, string> = { BOOKINGS: '#0B2D5B', WAITING_HANDOVER: '#d96200', ACCEPTED: '#118477', PARCELS: '#2460a1', CONFIRMED_FEES: '#0B2D5B', UNPAID_FEES: '#c65a00' };
  const allPoints = currentReport?.sections.flatMap(s => s.series) ?? [];
  return <InternalShell portal="admin" name={subject.displayName} environment={context?.environment} permissions={context?.permissions} domainCapabilities={domainCapabilities} path={location.pathname} logout={logout} busy={busy}>
      <section className="ap-dashboard"><div className="ap-dashboard-heading"><div><h1>{activeModule?.id === 'ADM-01' ? 'Tổng quan điều hành' : activeModule?.title ?? 'Tổng quan điều hành'}</h1><p>Dữ liệu nghiệp vụ theo quyền truy cập hiện hành.</p></div>{supportedSurface && !advanced && <button className="ap-secondary" disabled={!context || !currentReport && !error} onClick={() => setRefresh(n => n + 1)}>Làm mới</button>}</div>
        {!context && !contextError && <p className="ap-loading" role="status">Đang kiểm tra quyền truy cập…</p>}
        {contextError && <section className="ap-panel ap-access-boundary" role="alert"><h2>Chưa có quyền xem dữ liệu điều hành</h2><p>{contextError === 'ACCESS_DENIED' ? 'Quyền quản trị Identity hiện tại chưa bao gồm quyền đọc dữ liệu điều hành Carrier.' : problem(contextError)}</p><p>Không có dữ liệu Merchant, vận đơn hoặc tài chính nào được hiển thị. Liên hệ người phụ trách cấp quyền để được hướng dẫn.</p></section>}
        {context && !moduleAllowed && <ModuleDependency id={activeModule?.id} allowed={false} />}
        {context && moduleAllowed && activeModule?.id === 'ADM-17' && <AdminWorkCenter context={context} capabilities={domainCapabilities} />}
        {context && moduleAllowed && activeModule?.id === 'ADM-02' && <MerchantApplications accountId={subject.id} />}
        {context && moduleAllowed && activeModule?.id === 'ADM-03' && <MerchantOrganizations />}
        {context && moduleAllowed && activeModule?.id === 'ADM-16' && <MerchantAudit />}
        {context && moduleAllowed && activeModule?.id === 'ADM-18' && <AuthorityView accountId={subject.id} />}
        {context && moduleAllowed && !supportedSurface && activeModule?.id !== 'ADM-17' && activeModule?.id !== 'ADM-02' && activeModule?.id !== 'ADM-03' && activeModule?.id !== 'ADM-16' && activeModule?.id !== 'ADM-18' && <ModuleDependency id={activeModule?.id} allowed />}
        {context && moduleAllowed && advanced && <DashboardV2View context={context} />}
        {context && moduleAllowed && supportedSurface && !advanced && sourceMode !== null && <>
          {activeModule?.id === 'ADM-01' && <div className="iw-tabs" role="tablist" aria-label="Dashboard điều hành">{tabs.map((item, index) => <button key={item.id} id={'admin-tab-' + item.id} type="button" role="tab" aria-controls="admin-tab-panel" tabIndex={tab === item.id ? 0 : -1} aria-selected={tab === item.id} onClick={() => filters({ tab: item.id === 'overview' ? null : item.id })} onKeyDown={event => {
            const next = event.key === 'ArrowRight' ? (index + 1) % tabs.length : event.key === 'ArrowLeft' ? (index - 1 + tabs.length) % tabs.length : event.key === 'Home' ? 0 : event.key === 'End' ? tabs.length - 1 : -1;
            if (next < 0) return; event.preventDefault();
            event.currentTarget.parentElement?.querySelectorAll<HTMLButtonElement>('[role="tab"]')[next]?.focus();
            filters({ tab: tabs[next].id === 'overview' ? null : tabs[next].id });
          }}>{item.title}</button>)}</div>}
          <div id="admin-tab-panel" role={activeModule?.id === 'ADM-01' && tabAllowed ? 'tabpanel' : undefined} aria-labelledby={activeModule?.id === 'ADM-01' && tabAllowed ? 'admin-tab-' + tab : undefined} tabIndex={0}>
          {!tabAllowed && <SourceState state="FORBIDDEN" title="Tab chưa được cấp quyền">Chọn tab được phép trong context hiện hành. Thay đổi URL không mở quyền Finance hoặc Technical.</SourceState>}
          <form className="ap-filters" aria-label="Bộ lọc điều hành" onSubmit={e => e.preventDefault()}><div className="ap-presets">{[[1, 'Hôm nay'], [7, '7 ngày'], [30, '30 ngày'], [90, '90 ngày']].map(([days, label]) => <button type="button" key={days} className={to === today && from === daysBefore(today, Number(days) - 1) ? 'active' : ''} onClick={() => filters({ from: daysBefore(today, Number(days) - 1), to: today })}>{label}</button>)}</div><label>Từ ngày<input aria-label="Từ ngày" type="date" value={date(from) ? from : ''} max={today} onChange={e => filters({ from: e.target.value })} /></label><label>Đến ngày<input aria-label="Đến ngày" type="date" value={date(to) ? to : ''} max={today} onChange={e => filters({ to: e.target.value })} /></label><label>Trạng thái<select aria-label="Trạng thái" value={status ?? ''} onChange={e => filters({ shipmentStatus: e.target.value || null })}><option value="">Tất cả trạng thái hỗ trợ</option><option value="BOOKED">Chờ bàn giao</option><option value="AT_STATION">Đã tiếp nhận</option></select></label><label>Thống kê theo<select aria-label="Thống kê theo" value={rawGrain} onChange={e => filters({ grain: e.target.value })}><option value="DAY">Ngày</option><option value="WEEK">Tuần</option><option value="MONTH">Tháng</option></select></label></form>
          {!valid && <p className="ap-error" role="alert">Chọn khoảng thời gian hợp lệ, tối đa 366 ngày và không vượt quá hôm nay.</p>}
          {valid && !currentReport && !error && <p className="ap-loading" role="status">Đang tải dữ liệu điều hành…</p>}
          {valid && error && <SourceState state={error === 'ACCESS_DENIED' ? 'FORBIDDEN' : 'DEGRADED'} title="Chưa thể đọc số liệu điều hành">{problem(error)}</SourceState>}
          {valid && currentReport && tabAllowed && <>
            <p className="iw-scope-note">Phạm vi Local/Test theo quyền SYSTEM hiện hành. Bộ lọc vùng vận hành chưa khả dụng; không suy vùng từ mã SIM.</p>
            {stale && <div className="iw-stale" role="alert"><span>DEGRADED · Snapshot đã hết thời hạn drill-through. Làm mới trước khi mở danh sách.</span><button className="ap-secondary" onClick={() => setRefresh(n => n + 1)}>Làm mới snapshot</button></div>}
            <div className="ap-cutoff"><span>{displayDate(query.from)} – {displayDate(query.to)} · Giờ Việt Nam</span><span>Chốt lúc {new Date(currentReport.generatedAt).toLocaleString('vi-VN', { timeZone: 'Asia/Ho_Chi_Minh', hour: '2-digit', minute: '2-digit', second: '2-digit' })}</span></div>
            {(params.has('metric') || activeModule?.id === 'ADM-08') ? <section className="ap-panel ap-drill"><div className="ap-panel-heading"><div><h2>{metric ? metricLabels[metric.key] : 'Danh sách đóng góp'}</h2><p>Vận đơn đóng góp vào chỉ số · Giữ nguyên bộ lọc và thời điểm chốt</p></div><button className="ap-secondary" onClick={back}>Về tổng quan</button></div>
              {!metric?.drilldown || page < 0 || page > 10000 ? <p className="ap-error" role="alert">Chỉ số này không có danh sách được phép xem.</p> : rowError ? <div className="ap-error" role="alert"><p>{problem(rowError)}</p><button className="ap-secondary" onClick={() => setRefresh(n => n + 1)}>Làm mới số liệu</button></div> : !rows ? <p className="ap-loading" role="status">Đang đối chiếu danh sách với KPI…</p> : selectedShipment ? <ShipmentOversight key={currentReport.snapshotId + selectedShipment.id} row={selectedShipment} report={currentReport} metric={metric.key} close={() => setSelectedShipment(null)} /> : <><div className="ap-table-scroll"><table><thead><tr><th>Mã vận đơn</th><th>Phạm vi sở hữu</th><th>Ngày Booking</th><th>Trạng thái</th><th>Số kiện</th></tr></thead><tbody>{rows.items.map(row => <tr key={row.id}><td><button type="button" className="ap-shipment-link" onClick={() => setSelectedShipment(row)} aria-label={`Xem chi tiết ${row.code}`}>{row.code}</button></td><td>{row.ownerKind === 'BUSINESS' ? 'Doanh nghiệp' : 'Cá nhân'}</td><td>{new Date(row.bookedAt).toLocaleString('vi-VN', { timeZone: 'Asia/Ho_Chi_Minh' })}</td><td><span className={'ap-status ' + (row.status === 'AT_STATION' ? 'accepted' : '')}>{row.status === 'AT_STATION' ? 'Đã tiếp nhận' : 'Chờ bàn giao'}</span></td><td>{row.parcelCount}</td></tr>)}{!rows.items.length && <tr><td colSpan={5} className="ap-empty">Không có vận đơn đóng góp trong phạm vi đã chọn.</td></tr>}</tbody></table></div><div className="ap-pagination"><span>Trang {page + 1}</span><button className="ap-secondary" disabled={page === 0} onClick={() => { const next = new URLSearchParams(params); next.set('page', String(page - 1)); setParams(next); }}>Trang trước</button><button className="ap-secondary" disabled={!rows.hasNext} onClick={() => { const next = new URLSearchParams(params); next.set('page', String(page + 1)); setParams(next); }}>Trang sau</button></div></>}
            </section> : <>
              {(tab === 'merchant' || tab === 'network' || tab === 'decisions') && <DependencyView tab={tab} report={currentReport} />}
              <div className="ap-kpis" data-state={sectionState(true, currentReport.sections.find(s => s.key === (tab === 'finance' ? 'FINANCE' : 'SHIPMENTS')), stale)}>{visibleMetrics.map(m => <button type="button" key={m.key} className={'ap-kpi ' + (m.drilldown ? 'interactive' : '')} disabled={!m.drilldown || stale} onClick={() => drill(m)} aria-label={`${metricLabels[m.key]}: ${displayNumber(m.value)}${m.unit === 'VND' ? ' VND' : m.unit === 'PARCEL' ? ' kiện' : ' vận đơn'}`} style={{ borderTopColor: colors[m.key] }}><span className="ap-kpi-label">{metricLabels[m.key]}</span><div><strong style={{ color: colors[m.key] }}>{displayNumber(m.value)}</strong><small>{m.unit === 'VND' ? 'VND' : m.unit === 'PARCEL' ? 'kiện' : 'vận đơn'}</small></div><Spark points={allPoints} metric={m.key} color={colors[m.key]} grain={query.grain} /></button>)}</div>
              {tab === 'overview' && shipping && <div className="ap-chart-grid"><section className="ap-panel ap-volume"><div className="ap-panel-heading"><div><h2>Sản lượng Booking</h2><p>Theo thời gian tạo · Trạng thái tại thời điểm chốt</p></div><select aria-label="Chỉ số biểu đồ sản lượng" value={selectedSeries} onChange={e => setSeriesMetric(e.target.value as MetricKey)}>{shipping.metrics.map(m => <option key={m.key} value={m.key}>{metricLabels[m.key]}</option>)}</select></div><Trend points={shipping.series} metric={selectedSeries} title={`${metricLabels[selectedSeries]} theo thời gian Booking`} color={colors[selectedSeries]} /><button className="ap-chart-action" onClick={() => drill(metrics.find(m => m.key === selectedSeries)!)}>Xem vận đơn đóng góp</button></section><section className="ap-panel"><div className="ap-panel-heading"><div><h2>Phân bố trạng thái</h2><p>Trong bộ lọc đang chọn</p></div></div><div className="ap-donut" role="img" aria-label={`Chờ bàn giao ${count('WAITING_HANDOVER')}, đã tiếp nhận ${count('ACCEPTED')}`} style={{ background: BigInt(count('BOOKINGS')) === 0n ? '#e4eaf1' : `conic-gradient(#ff7a00 0 ${chartPercent(count('WAITING_HANDOVER'), BigInt(count('BOOKINGS')))}%, #118477 0 100%)` }}><div><strong>{displayNumber(count('BOOKINGS'))}</strong><span>vận đơn</span></div></div><div className="ap-state-legend">{(['WAITING_HANDOVER', 'ACCEPTED'] as const).map(key => <button key={key} onClick={() => drill(metrics.find(m => m.key === key)!)}><span><i style={{ background: colors[key] }} />{metricLabels[key]}</span><strong>{displayNumber(count(key))}</strong></button>)}</div></section></div>}
              {tab === 'finance' && finance && <section className="ap-panel"><div className="ap-panel-heading"><div><h2>Phân bổ cước đã xác nhận</h2><p>Theo ngày Booking · VND</p></div></div><Trend points={finance.series} metric="CONFIRMED_FEES" title="Cước đã xác nhận theo thời gian Booking" color="#ff7a00" /><p className="ap-finance-note">Cước đã xác nhận và nghĩa vụ chưa thanh toán; không phải doanh thu hoặc tiền đã thu. Chưa có nguồn COD, đối soát và thanh toán.</p></section>}
              {['overview', 'finance'].includes(tab) && !visibleMetrics.length && <div className="ap-panel ap-empty">Chưa có nguồn chỉ số khả dụng trong các quyền đọc được cấp.</div>}
            </>}
            {tab === 'risk' && <OperationalRisk merchant={context.permissions.includes('ADMIN_MERCHANT_READ')} work={domainCapabilities.includes('task.read') && domainCapabilities.includes('application.read')} />}
            {['overview', 'platform'].includes(tab) && context.permissions.includes('ADMIN_TECHNICAL_READ') && <section className="ap-panel ap-technical"><h2>Tình trạng API quản trị</h2>{platform ? <><span className={platform.apiAvailability === 'UP' ? 'ap-up' : ''}>{platform.apiAvailability === 'UP' ? 'Đang phản hồi' : 'Chưa khả dụng'}</span><p>Kiểm tra tại {new Date(platform.checkedAt).toLocaleTimeString('vi-VN', { timeZone: 'Asia/Ho_Chi_Minh' })}. Chưa có telemetry cho queues, scheduled jobs và integration failures.</p></> : <p role={platformError ? 'alert' : 'status'}>{platformError ? problem(platformError) : 'Đang kiểm tra…'}</p>}</section>}
            <MetricMetadata report={currentReport} now={clock} /><Sources report={currentReport} />
          </>}
          </div>
        </>}
      </section>
  </InternalShell>;
}
