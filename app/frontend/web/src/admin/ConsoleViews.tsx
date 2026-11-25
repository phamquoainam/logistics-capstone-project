import type { AdminContext, Dashboard } from './data';
import { adminModules, sectionState, snapshotState } from '../internal/navigation';
import type { TabId } from '../internal/navigation';
import { SourceState } from '../internal/InternalShell';

export function DependencyView({ tab, report }: { tab: TabId; report: Dashboard }) {
  const key = tab === 'merchant' ? 'MERCHANT' : tab === 'network' ? 'NETWORK' : 'PRIORITIES';
  const content = tab === 'merchant'
    ? { title: 'Merchant Queue', reason: 'Chưa có nguồn hồ sơ SUBMITTED/UNDER_REVIEW/NEEDS_INFORMATION/APPROVED/REJECTED. Hồ sơ Draft và Organization được operator tạo không được tính thành hồ sơ xét duyệt.' }
    : tab === 'network'
      ? { title: 'Network & SLA', reason: 'Chưa có registry Station/Hub/Partner chính thức, service availability hoặc SLA policy có hiệu lực. SIM-DROP không được tính là mạng lưới vận hành thương mại.' }
      : { title: 'Decisions', reason: 'Chưa có ApprovalTask, risk hold hoặc delegated approval queue canonical. Không có hành động tạo review task hay phê duyệt được mở từ quyền Dashboard READ.' };
  return <SourceState state={sectionState(true, report.sections.find(s => s.key === key))} title={content.title}>{content.reason}</SourceState>;
}
export function ModuleDependency({ id, allowed }: { id?: string; allowed: boolean }) {
  const m = adminModules.find(item => item.id === id);
  return <SourceState state={allowed ? 'UNAVAILABLE' : 'FORBIDDEN'} title={m?.title ?? 'Không tìm thấy trang được phép'}>{allowed ? m?.dependency : 'Trang này chưa có capability được cấp trong context hiện tại. Không có dữ liệu hoặc thao tác đặc quyền nào được mở bằng đường dẫn.'}</SourceState>;
}
export function AccountContext({ context }: { context: AdminContext }) {
  return <section className="ap-panel iw-account"><h2>Tài khoản & Quyền hiện hành</h2><dl><dt>Tên hiển thị</dt><dd>{context.displayName}</dd><dt>Context</dt><dd>SYSTEM · Administration Console</dd><dt>Môi trường</dt><dd>{context.environment}</dd><dt>Chế độ</dt><dd>Admin · từng thao tác được Backend kiểm tra độc lập</dd><dt>Múi giờ</dt><dd>{context.timeZone}</dd></dl><h3>Capabilities hiện hành</h3><ul>{context.permissions.map(p => <li key={p}>{p}</li>)}</ul><p>Không cộng dồn quyền Customer, Staff Operations hoặc Finance. Role không tự cấp quyền; chỉ capability và scope được Backend xác nhận mới có hiệu lực.</p></section>;
}
export function MetricMetadata({ report, now }: { report: Dashboard; now: number }) {
  return <details className="iw-metadata"><summary>Định nghĩa chỉ số & Tính cập nhật</summary><dl>
    <dt>Timezone</dt><dd>Asia/Ho_Chi_Minh · UTC+07:00</dd>
    <dt>Cohort</dt><dd>Ngày Booking trong bộ lọc; trạng thái chốt tại thời điểm đọc. Không phải báo cáo theo ngày phát sinh mọi event.</dd>
    <dt>Filter snapshot</dt><dd>{report.query.from} – {report.query.to} · {report.query.grain} · {report.query.shipmentStatus ?? 'Tất cả trạng thái hỗ trợ'}</dd>
    <dt>Freshness của snapshot</dt><dd>{snapshotState(report.generatedAt, now) === 'CURRENT' ? 'Trong thời hạn drill-through 15 phút' : 'Đã hết thời hạn; cần làm mới'}. Đây không phải xác nhận độ trễ pipeline.</dd>
    <dt>Metric version / Last source event</dt><dd>Chưa được API canonical cung cấp. Không dùng thời điểm refresh làm thời điểm event nguồn.</dd>
    <dt>Numerator / Denominator</dt><dd>Các KPI hiện tại là count/sum. Tỷ trọng trạng thái dùng số vận đơn cùng trạng thái / tổng Booking trong cùng snapshot; chưa có tỷ lệ giao thành công hoặc SLA.</dd>
    <dt>Phạm vi</dt><dd>Local/Test · SYSTEM realm đã được cấp; chưa có phân quyền vùng vận hành. Không suy scope vùng từ realm hoặc SIM area.</dd>
    <dt>Money types</dt><dd>Cước xác nhận ≠ tiền đã thu ≠ doanh thu ≠ COD. Nghĩa vụ hiện có là UNPAID; không suy ra overdue hoặc settled.</dd>
  </dl></details>;
}
