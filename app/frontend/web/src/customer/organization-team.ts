import type { BusinessContext, BusinessPermission } from './business-data';

export const teamRoleLabels: Record<BusinessContext['role'], string> = { OWNER: 'Chủ doanh nghiệp', MANAGER: 'Quản lý', OPERATOR: 'Nhân viên vận hành' };
type Capability = { group: string; label: string };
export const teamCapabilities: Record<BusinessPermission, Capability> = {
  'support.read': { group: 'Hỗ trợ doanh nghiệp', label: 'Xem Ticket trong phạm vi được cấp' },
  'support.write': { group: 'Hỗ trợ doanh nghiệp', label: 'Tạo và phản hồi Ticket' },
  'support.assign': { group: 'Hỗ trợ doanh nghiệp', label: 'Phân công phối hợp nội bộ' },
  'support.reports.read': { group: 'Hỗ trợ doanh nghiệp', label: 'Xem báo cáo hỗ trợ' },
  'support.notifications.manage': { group: 'Hỗ trợ doanh nghiệp', label: 'Cài đặt thông báo Ticket của mình' },
  'team.read': { group: 'Nhân sự & Phân quyền', label: 'Xem thành viên doanh nghiệp' },
  'team.invite': { group: 'Nhân sự & Phân quyền', label: 'Mời thành viên trong phạm vi được phép' },
  'team.access.manage': { group: 'Nhân sự & Phân quyền', label: 'Quản lý quyền thành viên được phép' },
  'team.membership.manage': { group: 'Nhân sự & Phân quyền', label: 'Tạm khóa và thu hồi Membership được phép' },
  'team.audit.read': { group: 'Nhân sự & Phân quyền', label: 'Xem lịch sử quản trị nhân sự' },
  'dashboard.read': { group: 'Báo cáo', label: 'Xem tổng quan doanh nghiệp' },
  'dashboard.finance.read': { group: 'Tài chính', label: 'Xem chỉ số tài chính trên tổng quan' },
  'dashboard.staff.read': { group: 'Báo cáo', label: 'Xem chỉ số hoạt động nhân viên' },
  'shipment.draft.read': { group: 'Vận đơn', label: 'Xem bản nháp và vận đơn' },
  'shipment.draft.write': { group: 'Vận đơn', label: 'Tạo và sửa bản nháp' },
  'shipment.import': { group: 'Vận đơn', label: 'Nhập danh sách vận đơn' },
  'shipment.template.manage': { group: 'Vận đơn', label: 'Quản lý mẫu vận đơn' },
  'shipment.prepare': { group: 'Kho & Bàn giao', label: 'Chuẩn bị và kiểm tra kiện hàng' },
  'shipment.quote': { group: 'Vận đơn', label: 'Yêu cầu báo giá Merchant' },
  'shipment.book': { group: 'Vận đơn', label: 'Xác nhận Booking' },
  'shipment.label.read': { group: 'Vận đơn', label: 'Xem và tải nhãn' },
  'shipment.handover.read': { group: 'Kho & Bàn giao', label: 'Xem danh sách bàn giao nội bộ' },
  'shipment.handover.manage': { group: 'Kho & Bàn giao', label: 'Quản lý danh sách bàn giao nội bộ' },
  'facility.read': { group: 'Kho & Bàn giao', label: 'Xem địa điểm Merchant' },
  'facility.manage': { group: 'Kho & Bàn giao', label: 'Quản lý địa điểm Merchant' },
  'defaults.manage': { group: 'Kho & Bàn giao', label: 'Cấu hình địa điểm mặc định' },
  'shipment.export': { group: 'Báo cáo', label: 'Xuất danh sách vận đơn' },
  'shipment.export.pii': { group: 'Danh bạ & Dữ liệu', label: 'Xuất thông tin liên hệ trong vận đơn' },
  'shipment.assign': { group: 'Công việc nội bộ', label: 'Phân công xử lý vận đơn' },
  'shipment.reports.read': { group: 'Báo cáo', label: 'Xem báo cáo vận hành Merchant' },
  'shipment.finance.read': { group: 'Tài chính', label: 'Xem cước và nghĩa vụ tài chính' },
  'contacts.read': { group: 'Danh bạ & Dữ liệu', label: 'Xem danh bạ doanh nghiệp' },
  'contacts.write': { group: 'Danh bạ & Dữ liệu', label: 'Quản lý liên hệ doanh nghiệp' },
  'contacts.import': { group: 'Danh bạ & Dữ liệu', label: 'Nhập danh bạ' },
  'contacts.export': { group: 'Danh bạ & Dữ liệu', label: 'Xuất danh bạ' },
  'shipment.actions.manage': { group: 'Công việc nội bộ', label: 'Quản lý công việc cần xử lý' },
  'shipment.actions.respond': { group: 'Công việc nội bộ', label: 'Phản hồi công việc được phép' },
};

/** The existing contexts API projects ORGANIZATION grants only. Never infer facility grants or admin rights. */
export function currentTeamContext(contexts: BusinessContext[], expected: BusinessContext): BusinessContext {
  const matches = contexts.filter(c => c.organizationId === expected.organizationId);
  if (matches.length !== 1 || matches[0].membershipId !== expected.membershipId || matches[0].authorityVersion < expected.authorityVersion) throw new Error('BUSINESS_CONTEXT_UNAVAILABLE');
  return matches[0];
}

export function teamCapabilityGroups(context: BusinessContext, search = '') {
  const needle = search.trim().toLocaleLowerCase('vi');
  const groups = new Map<string, { permission: BusinessPermission; label: string }[]>();
  for (const permission of context.permissions) {
    const item = teamCapabilities[permission];
    if (!item || !(item.label + ' ' + item.group + ' ' + permission).toLocaleLowerCase('vi').includes(needle)) continue;
    const items = groups.get(item.group) ?? []; items.push({ permission, label: item.label }); groups.set(item.group, items);
  }
  return [...groups].map(([group, items]) => ({ group, items }));
}
