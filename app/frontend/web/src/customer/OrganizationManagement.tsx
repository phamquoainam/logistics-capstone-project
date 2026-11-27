import { useEffect, useState } from 'react';
import type { FormEvent } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { customerClient, CustomerApiError } from './client';
import type { BusinessContext } from './business-data';
import type { OrganizationManagementPage } from './business-management-data';
import { BusinessPreparationList } from './BusinessPreparations';
import { OrganizationActionCenter } from './OrganizationActionCenter';
import { OrganizationDownloads } from './OrganizationDownloads';
import { organizationCategories, organizationDependencies, organizationDetailLink, organizationQuery } from './organization-management';
import { pilotMoney } from './PilotShipping';
import { shipmentDate } from './shipment-management';
import './organization-management.css';

const offsetDate = (date: string, days: number) => /^\d{4}-\d\d-\d\d$/.test(date) && Number.isFinite(Date.parse(date)) ? new Date(Date.parse(date + 'T00:00:00Z') + days * 86400000).toISOString().slice(0, 10) : '';
const short = (id: string) => id.slice(0, 8);
const columns = { contacts: 'Người nhận', source: 'Nguồn đơn', parcels: 'Kiện', fees: 'Cước', facility: 'Kho Merchant', creator: 'Người tạo', batch: 'Đợt nhập' };
type Column = keyof typeof columns;
function IdSelect({ name, label, value, options, none }: { name: string; label: string; value: string; options: string[]; none?: string }) {
  const values = [...new Set([...options, ...(value && value !== 'NONE' ? [value] : [])])];
  return <label>{label}<select name={name} defaultValue={value}><option value="">Tất cả</option>{none && <option value="NONE">{none}</option>}{values.map(id => <option value={id} key={id}>{label} · {short(id)}</option>)}</select><small>Các mã có trên trang hoặc đang lọc</small></label>;
}
export function OrganizationFilters({ query, data, apply }: { query: URLSearchParams; data?: OrganizationManagementPage; apply: (query: URLSearchParams) => void }) {
  const [error, setError] = useState('');
  const items = data?.items ?? [], value = (key: string) => query.get(key) ?? '';
  function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault(); const form = new FormData(event.currentTarget), p = new URLSearchParams();
    for (const [key, raw] of form) { const text = String(raw).trim(); if (text) p.set(key, text); }
    const from = p.get('from'), to = p.get('to'); p.delete('to');
    if (!!from !== !!to) { setError('Chọn đủ ngày bắt đầu và ngày kết thúc.'); return; }
    if (to) { const until = offsetDate(to, 1); if (!until || !from || until <= from || (Date.parse(until) - Date.parse(from)) / 86400000 > 366) { setError('Khoảng ngày không hợp lệ hoặc vượt 366 ngày.'); return; } p.set('until', until); }
    if (p.has('origin') !== p.has('destination')) { setError('Chọn đủ vùng gửi và vùng nhận.'); return; }
    if (p.has('origin') && !['SIM-A1/SIM-A2', 'SIM-A2/SIM-A1', 'SIM-A1/SIM-B1', 'SIM-B1/SIM-A1'].includes(p.get('origin') + '/' + p.get('destination'))) { setError('Tuyến này chưa được hỗ trợ. Hãy chọn tuyến có trong danh mục.'); return; }
    setError(''); apply(p);
  }
  const advanced = ['origin', 'destination', 'facilityId', 'creatorId', 'assignee', 'sourceChannel', 'batchId', 'obligation', 'handover', 'service'].some(key => query.has(key));
  return <form className="om-filters" onSubmit={submit}>
    <div className="om-filter-main"><label className="om-search-field">Tìm vận đơn<input name="search" maxLength={160} placeholder="Mã vận đơn / kiện, đơn nguồn, người nhận" defaultValue={value('search')} /></label><label>Từ ngày<input name="from" type="date" defaultValue={value('from')} /></label><label>Đến ngày<input name="to" type="date" defaultValue={offsetDate(value('until'), -1)} /></label><label>Sắp xếp<select name="sort" defaultValue={value('sort') || 'NEWEST'}><option value="NEWEST">Mới nhất</option><option value="OLDEST">Cũ nhất</option><option value="FEE_DESC">Cước giảm dần</option><option value="FEE_ASC">Cước tăng dần</option></select></label></div>
    <details className="om-advanced" open={advanced}><summary>Bộ lọc nâng cao</summary><div className="om-filter-grid">
      {(['origin', 'destination'] as const).map(key => <label key={key}>{key === 'origin' ? 'Vùng gửi' : 'Vùng nhận'}<select name={key} defaultValue={value(key)}><option value="">Tất cả</option>{['SIM-A1', 'SIM-A2', 'SIM-B1'].map(area => <option key={area}>{area}</option>)}</select></label>)}
      <IdSelect name="facilityId" label="Kho" value={value('facilityId')} options={items.flatMap(i => i.source.facilityId ? [i.source.facilityId] : [])} />
      <IdSelect name="creatorId" label="Người tạo" value={value('creatorId')} options={items.map(i => i.record.shipment.creatorAccountId)} />
      <IdSelect name="assignee" label="Phụ trách nội bộ" value={value('assignee')} options={items.flatMap(i => i.assignment.assigneeAccountId ? [i.assignment.assigneeAccountId] : [])} none="Chưa phân công" />
      <label>Kênh tạo đơn<input name="sourceChannel" maxLength={40} defaultValue={value('sourceChannel')} placeholder="Ví dụ: WEB, CUA_HANG" /></label>
      <IdSelect name="batchId" label="Đợt nhập" value={value('batchId')} options={items.flatMap(i => i.source.importJobIds)} />
      <label>Dịch vụ<select name="service" defaultValue={value('service')}><option value="">Tất cả</option><option value="ROAD_STANDARD">Đường bộ tiêu chuẩn</option></select></label>
      <label>Phương thức bàn giao<select name="handover" defaultValue={value('handover')}><option value="">Tất cả</option><option value="DROP_OFF">Gửi tại điểm bàn giao</option></select></label>
      <label>Nghĩa vụ cước<select name="obligation" defaultValue={value('obligation')}><option value="">Tất cả</option><option value="UNPAID">Chưa thanh toán</option></select></label>
    </div><p className="se-hint">Kho và đợt nhập là dữ liệu Merchant; không phải đợt Carrier nhận hàng. COD, Pickup và danh mục cửa hàng/đối tác chưa được kết nối.</p></details>
    <div className="om-filter-actions"><button className="pw-primary">Áp dụng bộ lọc</button><button type="button" className="pw-secondary" onClick={() => { setError(''); apply(new URLSearchParams()); }}>Đặt lại</button></div>{error && <p role="alert">{error}</p>}
  </form>;
}
export function OrganizationShipmentList({ context }: { context: BusinessContext }) {
  const [raw, setRaw] = useSearchParams(), q = organizationQuery(raw), base = '/merchant/' + context.organizationId;
  const [revision, setRevision] = useState(0), [visible, setVisible] = useState<Column[]>(['contacts', 'source', 'fees']);
  const [result, setResult] = useState<{ key: string; data?: OrganizationManagementPage; error?: string }>();
  const [selection, setSelection] = useState<{ key: string; ids: string[] }>({ key: '', ids: [] });
  const list = q.category === 'ALL' || q.category === 'WAITING', key = context.organizationId + ':' + context.authorityVersion + ':' + q.query + ':' + revision;
  const apiQuery = q.api.toString();
  useEffect(() => { if (!q.valid || !list) return; let active = true; void customerClient.readOrganizationManagement(context, new URLSearchParams(apiQuery)).then(data => { if (active) setResult({ key, data }); }).catch(e => { if (active) setResult({ key, error: e instanceof CustomerApiError && e.status === 400 ? 'Bộ lọc không hợp lệ. Kiểm tra ngày, tuyến hoặc các giá trị đã chọn.' : 'Chưa tải được vận đơn. Quyền hoặc dữ liệu có thể đã thay đổi; hãy thử lại.' }); }); return () => { active = false; }; }, [context, key, list, q.valid, apiQuery]);
  const current = result?.key === key ? result : undefined, data = current?.data, selected = selection.key === key ? selection.ids : [];
  function select(ids: string[]) { setSelection({ key, ids }); }
  function filters(next: URLSearchParams) { if (q.category === 'WAITING') next.set('category', 'WAITING'); next.delete('page'); setRaw(next); }
  function page(value: number) { const next = new URLSearchParams(q.query); if (value) next.set('page', String(value)); else next.delete('page'); setRaw(next); }
  const dependency = organizationDependencies[q.category];
  if (q.valid && q.category === 'ACTION') return <OrganizationActionCenter context={context} />;
  return <section className="bp-workspace om-page"><div className="bw-heading"><div><span className="pw-eyebrow">{context.displayName}</span><h1>Quản lý vận đơn</h1><p>Dữ liệu thuộc doanh nghiệp, theo phạm vi quyền hiện hành.</p></div><div className="se-heading-actions"><button type="button" className="pw-secondary" onClick={() => setRevision(v => v + 1)}>Làm mới</button>{context.permissions.includes('shipment.draft.write') && <Link className="pw-primary" to={base + '/create'}>Tạo vận đơn</Link>}</div></div>
    {!q.valid ? <section className="om-dependency"><h2>Bộ lọc không hợp lệ</h2><Link to={base + '/shipments'}>Đặt lại bộ lọc</Link></section> : <>
      <nav className="om-categories" aria-label="Nhóm vận đơn doanh nghiệp">{organizationCategories.map(([category, label]) => { const p = q.category === 'DRAFT' || category === 'DRAFT' ? new URLSearchParams() : new URLSearchParams(q.query); p.delete('page'); p.delete('status'); if (category !== 'ALL') p.set('category', category); else p.delete('category'); return <Link key={category} to={base + '/shipments' + (p.size ? '?' + p : '')} aria-current={q.category === category ? 'page' : undefined}>{label}</Link>; })}</nav>
      {q.category === 'DRAFT' ? <BusinessPreparationList context={context} embedded /> : dependency ? <section className="om-dependency"><h2>{dependency[0]}</h2><p>{dependency[1]}</p><Link className="pw-secondary" to={base + '/shipments'}>Xem vận đơn hiện có</Link></section> : <>
        <section className="om-list-panel"><OrganizationFilters key={q.query.toString()} query={q.api} data={data} apply={filters} />
        {!current ? <p className="om-state" role="status">Đang tải vận đơn…</p> : current.error ? <div className="om-state"><p role="alert">{current.error}</p><button className="pw-secondary" onClick={() => setRevision(v => v + 1)}>Thử lại</button></div> : data && <>
          <div className="om-summary"><div><span>Vận đơn phù hợp</span><strong>{BigInt(data.total).toLocaleString('vi-VN')}</strong></div><button type="button" onClick={() => { const next = new URLSearchParams(q.query); next.set('assignee', 'NONE'); filters(next); }}><span>Chưa phân công</span><strong>{BigInt(data.summary.unassigned).toLocaleString('vi-VN')}</strong></button><div><span>Cước chưa trả</span><strong>{BigInt(data.summary.unpaidFeesVnd).toLocaleString('vi-VN')} ₫</strong></div></div>
          <div className="om-list-tools"><span>Thống kê theo cùng bộ lọc · Không gộp đơn nháp</span><details><summary>Cột hiển thị</summary><div className="om-columns">{(Object.entries(columns) as [Column, string][]).map(([id, label]) => <label key={id}><input type="checkbox" checked={visible.includes(id)} onChange={e => setVisible(v => e.target.checked ? [...v, id] : v.filter(c => c !== id))} />{label}</label>)}</div></details></div>
          <OrganizationDownloads key={key + ':' + data.generatedAt} context={context} data={data} query={q.api} selected={selected} />
          {data.items.length ? <div className="om-table-scroll"><table className="om-table"><thead><tr><th><input type="checkbox" aria-label="Chọn tất cả vận đơn trên trang" checked={data.items.length > 0 && selected.length === data.items.length} onChange={e => select(e.target.checked ? data.items.map(i => i.record.shipment.id) : [])} /></th><th>Vận đơn / ngày tạo</th>{visible.includes('contacts') && <th>Người nhận</th>}{visible.includes('source') && <th>Nguồn đơn</th>}<th>Trạng thái / phụ trách</th>{visible.includes('parcels') && <th>Kiện</th>}{visible.includes('fees') && <th>Cước</th>}{visible.includes('facility') && <th>Kho</th>}{visible.includes('creator') && <th>Người tạo</th>}{visible.includes('batch') && <th>Đợt nhập</th>}<th>Chi tiết</th></tr></thead><tbody>{data.items.map(item => { const s = item.record.shipment, url = organizationDetailLink(context.organizationId, s.id, q.query); return <tr key={s.id}><td><input type="checkbox" aria-label={'Chọn ' + s.code} checked={selected.includes(s.id)} onChange={e => select(e.target.checked ? [...selected, s.id] : selected.filter(id => id !== s.id))} /></td><td><Link to={url}>{s.code}</Link><small>{shipmentDate(s.createdAt)}</small>{s.input.reference && <small>{s.input.reference}</small>}</td>{visible.includes('contacts') && <td>{s.input.recipient.name}<small>{s.input.recipient.phone}</small></td>}{visible.includes('source') && <td>{item.source.sourceChannel ?? 'Không khai báo'}<small>{item.source.sourceOrderReference}</small></td>}<td><span className="om-status">{item.status === 'AT_STATION' ? 'Đã tiếp nhận tại Station' : 'Chờ bàn giao'}</span><small>{item.assignment.assigneeAccountId ? 'Thành viên · ' + short(item.assignment.assigneeAccountId) : 'Chưa phân công'}</small>{item.allowedActions.includes('ASSIGN') && <Link className="om-assignment-link" to={url}>Phân công nội bộ</Link>}</td>{visible.includes('parcels') && <td>{s.parcels.length}</td>}{visible.includes('fees') && <td>{pilotMoney(s.totalVnd)}<small>UNPAID</small></td>}{visible.includes('facility') && <td>{s.facilityId ? short(s.facilityId) : 'Không gắn kho'}</td>}{visible.includes('creator') && <td><code title={s.creatorAccountId}>{short(s.creatorAccountId)}</code></td>}{visible.includes('batch') && <td>{item.source.importJobIds.map(id => <small key={id} title={id}>{short(id)}</small>)}</td>}<td><Link to={url}>Xem chi tiết</Link></td></tr>; })}</tbody></table></div> : <div className="om-state"><strong>Không có vận đơn phù hợp</strong><p>Thử bộ lọc khác hoặc tạo vận đơn mới.</p></div>}
          <nav className="ab-pagination" aria-label="Phân trang vận đơn doanh nghiệp"><button className="pw-secondary" disabled={q.page === 0} onClick={() => page(q.page - 1)}>Trang trước</button><span>Trang {q.page + 1} · {data.items.length} vận đơn</span><button className="pw-secondary" disabled={!data.hasNext} onClick={() => page(q.page + 1)}>Trang sau</button></nav>
        </>}
        </section>
      </>}
    </>}
  </section>;
}
