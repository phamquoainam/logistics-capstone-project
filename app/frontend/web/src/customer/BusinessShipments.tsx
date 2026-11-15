import { useEffect, useRef, useState } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { customerClient, CustomerApiError } from './client';
import type { BusinessPreparation, BusinessShipmentView } from './client';
import type { BusinessContext } from './business-data';
import type { OrganizationManagementItem } from './business-management-data';
import { SavedShippingSnapshot } from './ShipmentEntryFields';
import { pilotMoney } from './PilotShipping';
import { BusinessShipmentTabs } from './BusinessShipmentTabs';
import { OrganizationAssignment, OrganizationInternalHistory } from './OrganizationAssignment';
import { detailTabs, shipmentDate } from './shipment-management';
import { organizationReturn } from './organization-management';
import './organization-management.css';
export { OrganizationShipmentList as BusinessShipmentList } from './OrganizationManagement';

function message(error: unknown) { return error instanceof CustomerApiError && error.status === 404 ? 'Vận đơn không tồn tại hoặc quyền truy cập đã thay đổi.' : 'Chưa tải được dữ liệu. Hãy kiểm tra kết nối và thử lại.'; }
function SourceFacts({ data, item }: { data: BusinessPreparation; item: OrganizationManagementItem }) {
  return <section className="om-info"><h2>Nguồn đơn doanh nghiệp</h2><dl className="se-facts"><div><dt>Nguồn đơn</dt><dd>{data.sourceChannel ?? 'Không khai báo'}</dd></div><div><dt>Mã đơn nguồn</dt><dd>{data.sourceOrderReference ?? 'Không khai báo'}</dd></div><div><dt>Kho / điểm gửi</dt><dd>{data.facilityId ? <code className="bp-parcel-code">{data.facilityId}</code> : 'Không gắn kho'}</dd></div><div><dt>Người tạo</dt><dd><code className="bp-parcel-code">{item.record.shipment.creatorAccountId}</code></dd></div><div><dt>Đợt nhập file</dt><dd>{item.source.importJobIds.length ? item.source.importJobIds.map(id => <code className="bp-parcel-code" key={id}>{id}</code>) : 'Không có'}</dd></div></dl><p className="se-hint">Dữ liệu nguồn được gắn tại Booking. Kho Merchant và đợt nhập không phải Station hoặc đợt Carrier nhận hàng.</p></section>;
}
export function BusinessShipmentDetail({ context, id }: { context: BusinessContext; id: string }) {
  const [params, setParams] = useSearchParams(), tabValue = params.get('tab') ?? 'overview', tab = (detailTabs as readonly string[]).includes(tabValue) ? tabValue : 'overview';
  const [result, setResult] = useState<{ key: string; data?: BusinessShipmentView; preparation?: BusinessPreparation; item?: OrganizationManagementItem; error?: string }>();
  const [revision, setRevision] = useState(0), [historyRevision, setHistoryRevision] = useState(0), [busy, setBusy] = useState(false), [downloadError, setDownloadError] = useState('');
  const lifecycle = useRef({ active: true, running: false, generation: 0 });
  const key = context.organizationId + ':' + context.authorityVersion + ':' + id + ':' + revision;
  const back = organizationReturn(context.organizationId, params.get('returnTo'));
  const actionReturn = back.startsWith('/merchant/' + context.organizationId + '/shipments?') && new URL(back, 'https://local.invalid').searchParams.get('category') === 'ACTION';
  useEffect(() => { const state = lifecycle.current; state.active = true; state.generation++; return () => { state.active = false; state.generation++; }; }, [context, id]);
  useEffect(() => { let active = true;
    void customerClient.readBusinessShipment(context, id).then(async data => {
      const preparation = await customerClient.readBusinessPreparation(context, data.shipment.preparationId);
      const page = await customerClient.readOrganizationManagement(context, new URLSearchParams({ search: data.shipment.code }));
      const item = page.items.find(row => row.record.shipment.id === id);
      if (!item || item.record.shipment.quote.id !== data.shipment.quote.id || item.record.shipment.totalVnd !== data.shipment.totalVnd || preparation.state !== 'BOOKED' || preparation.version !== data.shipment.preparationVersion + 1 || preparation.facilityId !== data.shipment.facilityId) throw new CustomerApiError('RESPONSE_INVALID');
      if (active) setResult({ key, data, preparation, item });
    }).catch(error => { if (active) setResult({ key, error: message(error) }); });
    return () => { active = false; };
  }, [context, id, key]);
  const current = result?.key === key ? result : undefined, data = current?.data, item = current?.item;
  function change(value: string) { const next = new URLSearchParams(params); if (value === 'overview') next.delete('tab'); else next.set('tab', value); setParams(next, { replace: true }); }
  async function label() {
    const state = lifecycle.current, generation = state.generation, identity = customerClient.getSnapshot();
    if (state.running || !item?.allowedActions.includes('READ_LABEL')) return;
    state.running = true; setBusy(true); setDownloadError('');
    try { const blob = await customerClient.downloadBusinessLabel(context, id);
      if (!state.active || state.generation !== generation || customerClient.getSnapshot() !== identity) return;
      const url = URL.createObjectURL(blob), anchor = document.createElement('a'); anchor.href = url; anchor.download = 'argopost-' + id + '.pdf'; anchor.click(); window.setTimeout(() => URL.revokeObjectURL(url), 1000);
    } catch (error) { if (state.active && state.generation === generation) setDownloadError(message(error)); }
    finally { state.running = false; if (state.active && state.generation === generation) setBusy(false); }
  }
  return <section className="sd-page se-entry om-detail"><div className="pw-page-heading"><div><span className="pw-eyebrow">{context.displayName}</span><h1 tabIndex={-1}>Chi tiết vận đơn doanh nghiệp</h1></div><Link className="pw-secondary" to={back}>{actionReturn ? 'Quay lại đơn cần xử lý' : back.startsWith('/merchant/' + context.organizationId + '/reports') ? 'Quay lại báo cáo' : back.startsWith('/merchant/' + context.organizationId + '/finance') ? 'Quay lại cước & đối soát' : 'Danh sách vận đơn'}</Link></div>
    {!current ? <p role="status">Đang tải vận đơn…</p> : current.error ? <div className="cp-notice cp-error"><p role="alert">{current.error}</p><button className="sd-link" onClick={() => setRevision(v => v + 1)}>Tải lại</button></div> : data && item && <>
      <div className="pb-shipment-heading"><h2 className="pb-code">{data.shipment.code}</h2><span className="pb-unpaid">UNPAID</span><span>Chờ bàn giao</span></div>
      <BusinessShipmentTabs data={data} tab={tab} change={change} download={() => { void label(); }} busy={busy} canLabel={item.allowedActions.includes('READ_LABEL')} history={<OrganizationInternalHistory key={context.organizationId + ':' + id + ':' + context.authorityVersion} context={context} id={id} revision={historyRevision} />} />
      {tab === 'overview' && <div id="business-panel-overview" role="tabpanel" tabIndex={0} aria-labelledby="business-tab-overview" className="se-review-grid"><SavedShippingSnapshot input={data.shipment.input} /><div className="se-column">
        {current.preparation && <SourceFacts data={current.preparation} item={item} />}
        <OrganizationAssignment key={context.organizationId + ':' + id + ':' + context.authorityVersion} context={context} item={item} changed={() => setHistoryRevision(v => v + 1)} />
        <section className="om-info"><h2>Cước & bàn giao</h2><dl className="se-facts"><div><dt>Ngày Booking</dt><dd>{shipmentDate(data.shipment.createdAt)}</dd></div><div><dt>Tuyến</dt><dd>{data.shipment.quote.route.originCode} → {data.shipment.quote.route.destinationCode}</dd></div><div><dt>Điểm bàn giao dự kiến</dt><dd>{data.shipment.quote.route.dropOffCode}</dd></div></dl><div className="pb-total"><span>Cước đã xác nhận</span><strong>{pilotMoney(data.obligation.amountVnd)}</strong></div><p className="se-hint">Carrier chưa tiếp nhận. Booking không xác nhận đã thu tiền.</p></section>
        <section className="om-info"><h2>Kiện hàng & nhãn</h2><p>{data.shipment.parcels.length} kiện đã có định danh riêng.</p>{item.allowedActions.includes('READ_LABEL') ? <button type="button" className="pw-primary" disabled={busy} onClick={() => { void label(); }}>{busy ? 'Đang tải…' : 'Tải nhãn tất cả kiện'}</button> : <p>Cần quyền đọc nhãn tại kho của vận đơn.</p>}<p className="se-hint">PDF 100 × 150 mm, Code128 và đầy đủ trang tiếp nối.</p></section>
      </div></div>}
    </>}{downloadError && <p role="alert">{downloadError}</p>}
  </section>;
}
