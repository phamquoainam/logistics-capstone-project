import { OrganizationTaskBoard } from './OrganizationTaskBoard';
import { useEffect, useState } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import type { BusinessContext } from './business-data';
import { customerClient } from './client';
import type { BusinessPreparationPage } from './client';
import type { OrganizationManagementPage } from './business-management-data';
import { actionSourceQuery, unassignedActionQuery } from './organization-action-center';
import { organizationDetailLink } from './organization-management';
import { shipmentDate } from './shipment-management';
import './organization-action-center.css';

type View = 'drafts' | 'unassigned' | 'carrier' | 'tasks';
type Result = { key: string; drafts?: BusinessPreparationPage; shipments?: OrganizationManagementPage; error?: string };
const views: [View, string][] = [['drafts', 'Hoàn tất bản nháp'], ['unassigned', 'Chưa phân công'], ['carrier', 'Yêu cầu từ Carrier'], ['tasks', 'Công việc nội bộ']];
export function OrganizationActionCenter({ context }: { context: BusinessContext }) {
  const [raw, setRaw] = useSearchParams(), source = actionSourceQuery(raw), [view, setView] = useState<View>(source.draftComparable ? 'drafts' : 'unassigned');
  const [paging, setPaging] = useState({ key: '', page: 0 }), [revision, setRevision] = useState(0), [result, setResult] = useState<Result>();
  const [taskBlocked, setTaskBlocked] = useState(false);
  const base = '/merchant/' + context.organizationId, sourceKey = context.organizationId + ':' + context.authorityVersion + ':' + source.filters + ':' + view;
  const page = paging.key === sourceKey ? paging.page : 0, key = sourceKey + ':' + page + ':' + revision, encoded = source.filters.toString();
  const blocked = !source.valid || (view === 'drafts' || view === 'tasks') && !source.draftComparable || view === 'unassigned' && source.conflictingAssignee;
  useEffect(() => {
    if (view === 'carrier' || view === 'tasks' || blocked) return;
    let active = true;
    void (view === 'drafts' ? customerClient.listBusinessPreparations(context, page).then(drafts => ({ key, drafts })) : customerClient.readOrganizationManagement(context, unassignedActionQuery(new URLSearchParams(encoded), page)).then(shipments => { if (shipments.items.some(item => item.assignment.assigneeAccountId !== null)) throw new Error('ACTION_SOURCE_RESPONSE_MISMATCH'); return { key, shipments }; }))
      .then(value => { if (active) setResult(value); }).catch(() => { if (active) setResult({ key, error: 'Chưa tải được công việc hoặc quyền theo kho đã thay đổi. Không dùng số 0 thay cho lỗi nguồn.' }); });
    return () => { active = false; };
  }, [context, page, view, blocked, key, encoded]);
  const current = result?.key === key ? result : undefined, drafts = current?.drafts, shipments = current?.shipments, hasNext = drafts?.hasNext ?? shipments?.hasNext ?? false;
  const detailQuery = new URLSearchParams(raw); detailQuery.set('assignee', 'NONE');
  function changeView(next: View) { if ((next === 'drafts' || next === 'tasks') && raw.get('assignee') === 'NONE') { const p = new URLSearchParams(raw); p.delete('assignee'); setRaw(p); } setView(next); }
  function clearFilters() { setRaw(new URLSearchParams({ category: 'ACTION' })); }
  function useUnassigned() { const p = new URLSearchParams(raw); p.set('assignee', 'NONE'); p.delete('page'); setRaw(p); }
  return <section className="oa-workspace"><header className="oa-heading"><div><span className="pw-eyebrow">{context.displayName}</span><h1>Đơn cần xử lý</h1><p>Chuẩn bị đơn và phân công nội bộ theo phạm vi được cấp.</p></div><button className="pw-secondary" disabled={taskBlocked} onClick={() => setRevision(v => v + 1)}>Làm mới</button></header>
    <nav className="oa-tabs" aria-label="Nguồn công việc doanh nghiệp">{views.map(([id, label]) => <button type="button" key={id} disabled={taskBlocked} aria-current={view === id ? 'page' : undefined} onClick={() => changeView(id)}>{label}</button>)}</nav>
    {view === 'tasks' && !blocked ? <OrganizationTaskBoard context={context} revision={revision} onBlocked={setTaskBlocked} /> : view === 'carrier' ? <section className="oa-panel oa-empty"><span className="oa-source-label">Nguồn chưa kết nối</span><h2>Chưa có yêu cầu phản hồi từ Carrier</h2><p>Pickup thất bại, giao thất bại, vấn đề liên hệ, phụ phí hoặc hoàn hàng cần có yêu cầu nghiệp vụ hợp lệ trước khi Merchant phản hồi.</p><p>Chưa thể xác định số yêu cầu, mức ưu tiên hay thời hạn. Booking, UNPAID và chưa phân công không tự trở thành sự cố giao hàng.</p><Link className="pw-secondary" to={base + '/shipments'}>Xem vận đơn doanh nghiệp</Link></section>
      : blocked ? <section className="oa-panel oa-empty"><h2>{view === 'drafts' || view === 'tasks' ? 'Nguồn có bộ lọc riêng' : 'Bộ lọc người phụ trách đang xung đột'}</h2><p>{view === 'drafts' ? 'Bộ lọc vận đơn đang được giữ trong đường dẫn. Nguồn bản nháp hiện chưa hỗ trợ các bộ lọc đó; hệ thống không âm thầm bỏ bộ lọc để hiển thị dữ liệu khác.' : 'Bạn đang lọc một người phụ trách cụ thể. Nhóm này chỉ gồm các vận đơn chưa được phân công.'}</p><button className="pw-secondary" onClick={view === 'drafts' || view === 'tasks' ? clearFilters : useUnassigned}>{view === 'drafts' ? 'Xóa bộ lọc vận đơn để xem bản nháp' : 'Chuyển sang chưa phân công'}</button></section>
      : !current ? <p className="oa-loading" role="status">Đang kiểm tra phạm vi và tải công việc…</p> : current.error ? <section className="oa-panel oa-empty"><p role="alert">{current.error}</p><button className="pw-secondary" disabled={taskBlocked} onClick={() => setRevision(v => v + 1)}>Thử lại</button></section> : <section className="oa-panel">
        <header className="oa-section-heading"><div><h2>{view === 'drafts' ? 'Bản nháp cần hoàn tất' : 'Vận đơn chưa phân công nội bộ'}</h2><p>{view === 'drafts' ? 'Chưa xác nhận Booking; chưa phải vận đơn phát sinh ngoại lệ.' : 'Phân công người theo dõi đơn, tách biệt với Courier và Carrier Tracking.'}</p></div><span className="oa-count">{drafts ? drafts.items.length + ' bản trên trang' : shipments ? BigInt(shipments.total).toLocaleString('vi-VN') + ' vận đơn phù hợp' : ''}</span></header>
        {source.filters.size > 0 && <div className="oa-filter-context"><span>Đang giữ bộ lọc từ Quản lý vận đơn.</span><button className="sd-link" onClick={clearFilters}>Xóa bộ lọc</button></div>}
        <div className="oa-table-scroll"><table><thead><tr><th>{view === 'drafts' ? 'Bản nháp / tham chiếu' : 'Vận đơn'}</th><th>Người nhận</th><th>Kho / nguồn</th><th>Kiện</th><th>{view === 'drafts' ? 'Cập nhật' : 'Ngày Booking'}</th><th>Thao tác</th></tr></thead><tbody>
          {drafts?.items.map(row => <tr key={row.id}><td><strong>{row.input.reference ?? row.sourceOrderReference ?? 'Bản nháp · ' + row.id.slice(0, 8)}</strong><small>Chưa Booking</small></td><td>{row.input.recipient.name}</td><td>{row.facilityId ? <code title={row.facilityId}>{row.facilityId.slice(0, 8)}</code> : 'Chưa gắn kho'}<small>{row.sourceChannel ?? 'Chưa khai báo nguồn'}</small></td><td>{row.input.parcels.length}</td><td>{shipmentDate(row.updatedAt)}</td><td><Link to={base + '/create?draft=' + row.id}>{row.allowedActions.includes('UPDATE') ? 'Tiếp tục bản nháp' : 'Xem bản nháp'}</Link></td></tr>)}
          {shipments?.items.map(row => { const s = row.record.shipment; return <tr key={s.id}><td><strong>{s.code}</strong><small>Chờ bàn giao</small></td><td>{s.input.recipient.name}</td><td>{row.source.facilityId ? <code title={row.source.facilityId}>{row.source.facilityId.slice(0, 8)}</code> : 'Chưa gắn kho'}<small>{row.source.sourceChannel ?? 'Chưa khai báo nguồn'}</small></td><td>{s.parcels.length}</td><td>{shipmentDate(s.createdAt)}</td><td><Link to={organizationDetailLink(context.organizationId, s.id, detailQuery)}>{row.allowedActions.includes('ASSIGN') ? 'Xem & phân công' : 'Xem chi tiết'}</Link></td></tr>; })}
        </tbody></table></div>
        {(drafts?.items.length ?? shipments?.items.length) === 0 && <div className="oa-empty"><h3>{view === 'drafts' ? 'Không có bản nháp trên trang này' : 'Không có vận đơn chưa phân công phù hợp'}</h3><p>Kết quả thuộc nguồn và phạm vi đang chọn; không có nghĩa mọi yêu cầu Carrier đã được xử lý.</p></div>}
        <nav className="ab-pagination" aria-label="Phân trang công việc nội bộ"><button className="pw-secondary" disabled={page === 0} onClick={() => setPaging({ key: sourceKey, page: page - 1 })}>Trang trước</button><span>Trang {page + 1}</span><button className="pw-secondary" disabled={!hasNext} onClick={() => setPaging({ key: sourceKey, page: page + 1 })}>Trang sau</button></nav>
      </section>}
    <details className="oa-boundary"><summary>Phạm vi xử lý hiện tại</summary><p>Hoàn tất bản nháp, phân công và phản hồi công việc nội bộ theo quyền. Ưu tiên, hạn xử lý và lịch sử công việc được quản lý riêng; hoàn tất công việc không thay đổi trạng thái vận chuyển. Các yêu cầu từ Carrier chỉ xuất hiện khi có nguồn nghiệp vụ hợp lệ.</p></details>
  </section>;
}
