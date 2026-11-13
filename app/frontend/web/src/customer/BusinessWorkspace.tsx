import { useEffect, useState } from 'react';
import { Link, Navigate, useLocation } from 'react-router-dom';
import { customerClient } from './client';
import type { Subject } from './client';
import { WorkspaceShell } from './WorkspaceShell';
import { businessChartTitles, businessMetrics } from './business-data';
import type { BusinessContext, BusinessReport } from './business-data';
import './business.css';
import './business-theme.css';
import { BusinessPreparationEditor, BusinessPreparationList } from './BusinessPreparations';
import { BusinessImportWorkspace } from './BusinessImports';
import { BusinessShipmentList, BusinessShipmentDetail } from './BusinessShipments';

function message(error: unknown) {
  const code = error instanceof Error ? error.message : '';
  if (/BUSINESS_DISABLED/.test(code)) return 'Business chưa được bật trong môi trường hiện tại.';
  if (/BUSINESS_CONTEXT_UNAVAILABLE/.test(code)) return 'Quyền truy cập doanh nghiệp không còn khả dụng. Hãy tải lại danh sách không gian làm việc.';
  return 'Chưa tải được dữ liệu doanh nghiệp. Hãy thử lại; dữ liệu cũ không được hiển thị thay thế.';
}
function SourceEmpty({ kind = 'shipping' }: { kind?: 'shipping' | 'status' | 'finance' | 'facility' | 'staff' | 'channel' }) {
  const text = { shipping: 'Sản lượng sẽ hiển thị khi có nguồn vận đơn thuộc doanh nghiệp.', status: 'Phân bố trạng thái cần các sự kiện vận chuyển của doanh nghiệp.', finance: 'Chưa kết nối nghĩa vụ cước và giao dịch của doanh nghiệp.', facility: 'Chưa kết nối danh mục kho, điểm lấy và dữ liệu vận đơn theo địa điểm.', staff: 'Chưa có dữ liệu tác vụ và phân công nội bộ doanh nghiệp.', channel: 'Chưa kết nối cửa hàng hoặc nguồn đơn của doanh nghiệp.' };
  return <div className={'bw-source-empty bw-source-' + kind}><svg viewBox="0 0 48 48" fill="none" stroke="currentColor" strokeWidth="1.5" aria-hidden="true"><rect x="8" y="7" width="32" height="34" rx="6" /><path d="M16 30v-6m8 6V17m8 13V21M15 35h18" /></svg><strong>Chưa có nguồn dữ liệu</strong><p>{text[kind]}</p></div>;
}
const quickLabels: Record<string, string> = { CREATE: 'Tạo vận đơn', IMPORT: 'Import Excel', LABELS: 'In nhãn', MANIFEST: 'Bảng kê', PICKUP: 'Yêu cầu Pickup', ACTION_CENTER: 'Xử lý đơn' };
function Overview({ context }: { context: BusinessContext }) {
  const [range, setRange] = useState('30'), [interval, setInterval] = useState('DAY');
  const [custom, setCustom] = useState({ from: '', to: '' }), [appliedCustom, setAppliedCustom] = useState({ from: '', to: '' });
  const [revision, setRevision] = useState(0), [result, setResult] = useState<{ key: string; report?: BusinessReport; error?: string }>();
  const today = new Intl.DateTimeFormat('en-CA', { timeZone: 'Asia/Ho_Chi_Minh', year: 'numeric', month: '2-digit', day: '2-digit' }).format(new Date());
  const from = range === 'custom' ? appliedCustom.from : new Date(Date.parse(today + 'T00:00:00Z') - (Number(range) - 1) * 86400000).toISOString().slice(0, 10);
  const to = range === 'custom' ? appliedCustom.to : today;
  const query = new URLSearchParams({ from, to, interval }), key = context.organizationId + ':' + context.authorityVersion + ':' + query + ':' + revision;
  useEffect(() => {
    let active = true;
    void customerClient.businessDashboard(context, new URLSearchParams({ from, to, interval })).then(report => { if (active) setResult({ key, report }); }).catch(error => { if (active) setResult({ key, error: message(error) }); });
    return () => { active = false; };
  }, [context, from, to, interval, key]);
  const current = result?.key === key ? result : undefined, report = current?.report;
  const finance = context.permissions.includes('dashboard.finance.read');
  return <section className="bw-dashboard"><div className="bw-heading"><div><span className="pw-eyebrow">TỔNG QUAN DOANH NGHIỆP</span><h1 tabIndex={-1}>{context.displayName}</h1><p>Hoạt động gửi hàng trong phạm vi tổ chức của bạn.</p></div><button type="button" className="pw-secondary" onClick={() => setRevision(v => v + 1)}>Làm mới</button></div>
    <div className="bw-toolbar"><div className="bw-range" aria-label="Khoảng thời gian báo cáo">{[['1', 'Hôm nay'], ['7', '7 ngày'], ['30', '30 ngày'], ['90', '90 ngày']].map(([value, label]) => <button type="button" key={value} aria-pressed={range === value} onClick={() => setRange(value)}>{label}</button>)}<button type="button" aria-pressed={range === 'custom'} onClick={() => { setCustom({ from, to }); setAppliedCustom({ from, to }); setRange('custom'); }}>Tùy chọn</button></div><label>Thống kê theo<select value={interval} onChange={event => setInterval(event.target.value)}><option value="DAY">Ngày</option><option value="WEEK">Tuần</option><option value="MONTH">Tháng</option></select></label><span className="bw-period">{from} — {to}<small>Giờ Việt Nam</small></span></div>
    {range === 'custom' && <form className="bw-custom-range" onSubmit={event => { event.preventDefault(); setAppliedCustom(custom); }}><label>Từ ngày<input type="date" required min="2000-01-01" max={custom.to || today} value={custom.from} onChange={event => setCustom({ ...custom, from: event.target.value })} /></label><label>Đến ngày<input type="date" required min={custom.from} max={today} value={custom.to} onChange={event => setCustom({ ...custom, to: event.target.value })} /></label><button className="pw-secondary">Áp dụng</button><small>Tối đa 366 ngày.</small></form>}
    {!current ? <div className="pw-panel bw-loading" role="status">Đang kiểm tra quyền và tải tổng quan…</div> : current.error ? <div className="pw-panel bw-error" role="alert"><h2>Không thể hiển thị tổng quan</h2><p>{current.error}</p><button className="pw-secondary" type="button" onClick={() => window.location.reload()}>Tải lại không gian làm việc</button></div> : report && <>
      <p className="bw-source-note">Workspace đã kết nối Backend. Nguồn vận đơn{finance ? ' và tài chính' : ''} Business chưa được kết nối; số liệu không lấy từ tài khoản Personal.</p>
      <div className="bw-kpi-grid">{(['BOOKED_SHIPMENTS', 'AWAITING_HANDOVER', 'ACCEPTED_SHIPMENTS', ...(finance ? ['OUTSTANDING_FEES'] : ['PARCELS'])] as const).map(id => <article className="bw-kpi" key={id}><span>{businessMetrics[id as keyof typeof businessMetrics][0]}</span><strong aria-label="Chưa có số liệu">—</strong><small>Chưa có nguồn dữ liệu</small></article>)}</div>
      <div className="bw-chart-grid"><article className="bw-panel bw-main-chart"><header><h2>Sản lượng gửi hàng</h2><span>{interval === 'DAY' ? 'Theo ngày' : interval === 'WEEK' ? 'Theo tuần' : 'Theo tháng'}</span></header><SourceEmpty /></article><article className="bw-panel"><header><h2>Trạng thái vận đơn</h2></header><SourceEmpty kind="status" /></article></div>
      <div className="bw-chart-grid"><article className="bw-panel"><header><h2>Vận đơn gần đây</h2></header><div className="bw-compact-empty"><p>Chưa có nguồn vận đơn doanh nghiệp để hiển thị danh sách.</p></div></article><article className="bw-panel"><header><h2>Công việc cần chú ý</h2></header><div className="bw-compact-empty"><p>Chưa kết nối sự cố và tác vụ Merchant. Không thể xác định số công việc cần xử lý.</p></div></article></div>
      <div className="bw-secondary-grid">{report.charts.filter(c => !['VOLUME_TREND', 'STATUS_DISTRIBUTION', 'SERVICE_BREAKDOWN', 'ROUTE_BREAKDOWN'].includes(c.id)).map(chart => <article className="bw-panel" key={chart.id}><header><h2>{businessChartTitles[chart.id]}</h2></header><SourceEmpty kind={chart.id === 'FEE_TREND' ? 'finance' : chart.id === 'FACILITY_BREAKDOWN' ? 'facility' : chart.id === 'STAFF_BREAKDOWN' ? 'staff' : 'channel'} /></article>)}</div>
      <details className="bw-panel bw-metric-details"><summary>Phạm vi chỉ số & dịch vụ</summary><div className="bw-metric-catalog">{report.metrics.filter(m => !['BOOKED_SHIPMENTS', 'AWAITING_HANDOVER', 'ACCEPTED_SHIPMENTS', 'OUTSTANDING_FEES'].includes(m.id)).map(m => <div key={m.id}><span>{businessMetrics[m.id][0]}</span><span>Chưa có nguồn</span></div>)}</div><p>Dịch vụ và tuyến chỉ được phân tích khi có vận đơn Business hợp lệ. Chưa thể xác định SLA, tỷ lệ thành công hoặc COD từ nguồn dữ liệu hiện tại.</p></details>
      <section className="bw-panel bw-quick"><header><h2>Thao tác nhanh</h2><span>Chưa mở workflow Merchant</span></header><div>{report.quickActions.map(action => <button type="button" disabled key={action.id}>{quickLabels[action.id]}</button>)}</div></section>
    </>}
  </section>;
}
export function BusinessWorkspace({ subject }: { subject: Subject }) {
  const location = useLocation(), [retry, setRetry] = useState(0), [state, setState] = useState<{ contexts?: BusinessContext[]; error?: string }>();
  useEffect(() => { let active = true; void customerClient.businessContexts().then(contexts => { if (active) setState({ contexts }); }).catch(error => { if (active) setState({ error: message(error) }); }); return () => { active = false; }; }, [subject.accountId, subject.securityVersion, retry]);
  const match = /^\/merchant\/([a-f0-9-]+)\/(overview|create|preparations|imports|shipments)(?:\/([a-f0-9-]+))?\/?$/.exec(location.pathname);
  const context = match && state?.contexts?.find(c => c.organizationId === match[1]);
  if (state?.contexts?.length && ['/merchant', '/merchant/'].includes(location.pathname)) return <Navigate replace to={'/merchant/' + state.contexts[0].organizationId + '/overview'} />;
  return <div className="customer-portal cp-workspace-portal cp-business-portal"><WorkspaceShell subject={subject} business={context || undefined}>
    {context ? match?.[2] === 'shipments' ? match[3] ? <BusinessShipmentDetail key={context.organizationId + ':' + context.authorityVersion + ':' + match[3]} context={context} id={match[3]} /> : <BusinessShipmentList key={context.organizationId + ':' + context.authorityVersion} context={context} /> : match?.[2] === 'imports' ? <BusinessImportWorkspace key={context.organizationId + ':' + context.authorityVersion} context={context} /> : match?.[2] === 'overview' ? <Overview key={context.organizationId + ':' + context.authorityVersion} context={context} /> : match?.[2] === 'preparations' ? <BusinessPreparationList key={context.organizationId + ':' + context.authorityVersion} context={context} /> : <BusinessPreparationEditor key={context.organizationId + ':' + context.authorityVersion + ':' + location.search} context={context} /> : <section className="pw-panel bw-access-state"><span className="pw-eyebrow">ARGOPOST BUSINESS</span><h1 tabIndex={-1}>{!state ? 'Đang kiểm tra không gian làm việc…' : state.error ? 'Chưa tải được không gian làm việc' : match ? 'Không gian làm việc không khả dụng' : 'Chưa có quyền truy cập doanh nghiệp'}</h1><p role={state?.error ? 'alert' : 'status'}>{state?.error ?? (!state ? 'Đang xác nhận Organization, Membership và quyền hiện hành.' : 'Business chỉ khả dụng với Organization đang hoạt động và Membership được cấp quyền. Tài khoản cá nhân của bạn vẫn sử dụng bình thường.')}</p><div><button type="button" className="pw-secondary" onClick={() => { setState(undefined); setRetry(v => v + 1); }}>Kiểm tra lại</button><Link className="pw-primary" to="/personal">Về Personal</Link></div></section>}
  </WorkspaceShell></div>;
}
