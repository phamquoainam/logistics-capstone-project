import type { Permission, Section } from '../admin/data';

export type Portal = 'ops' | 'finance' | 'admin';
export type DataState = 'AVAILABLE' | 'EMPTY' | 'UNAVAILABLE' | 'FORBIDDEN' | 'DEGRADED';
export const portalNames: Record<Portal, string> = { ops: 'Carrier Operations', finance: 'Carrier Business & Finance', admin: 'Administration Console' };
type Module = { id: string; title: string; path: string; permission: Permission | null; dependency: string; extension: boolean };
const module = (number: number, title: string, path: string, permission: Permission | null, dependency: string): Module => ({ id: `ADM-${String(number).padStart(2, '0')}`, title, path, permission, dependency, extension: number > 17 });
/** Document B sections II/III. This UI registry grants no Backend authority. */
export const adminModules: readonly Module[] = [
  module(1, 'Tổng quan điều hành', '/admin', 'ADMIN_DASHBOARD_READ', 'Merchant review, mạng lưới chính thức, SLA và quyết định cần nguồn riêng.'),
  module(2, 'Hồ sơ & Xét duyệt Merchant', '/admin/merchants/applications', 'ADMIN_MERCHANT_READ', 'Chưa có nguồn hồ sơ đã nộp và workflow xét duyệt; Draft không phải hồ sơ chờ duyệt.'),
  module(3, 'Tổ chức Merchant', '/admin/merchants/organizations', 'ADMIN_MERCHANT_READ', 'Cần contract đọc directory và lifecycle theo phạm vi Admin; không dùng API tự sở hữu của Merchant.'),
  module(4, 'Dịch vụ & Vùng phục vụ', '/admin/services', null, 'Cần authority quản trị policy và phiên bản phát hành. Dataset SIM không phải coverage Production.'),
  module(5, 'Station, Hub & Mạng lưới', '/admin/network', 'ADMIN_NETWORK_READ', 'Chưa có registry mạng lưới Carrier chính thức; SIM-DROP không phải Station thương mại.'),
  module(6, 'Partner & Franchise', '/admin/partners', 'ADMIN_NETWORK_READ', 'Chưa có registry Partner, hợp đồng và quyền quản trị điểm đại lý.'),
  module(7, 'Giá & Hợp đồng', '/admin/pricing', null, 'Cần permission Pricing và workflow maker-checker; Finance READ không cấp quyền công bố giá.'),
  module(8, 'Giám sát vận đơn', '/admin/shipments', 'ADMIN_SHIPMENT_READ', 'Hiện đọc Booking/Parcel/tiếp nhận; tracking chi tiết, PII disclosure và can thiệp cần contract riêng.'),
  module(9, 'SLA & Ngoại lệ', '/admin/sla', 'ADMIN_SHIPMENT_READ', 'Chưa có SLA policy, lịch vận hành và nguồn breach/escalation hợp lệ.'),
  module(10, 'Hỗ trợ & Claims Governance', '/admin/support', null, 'Support Staff và Admin governance là authority riêng; bồi thường vẫn bị chặn theo policy.'),
  module(11, 'Giám sát tài chính', '/admin/finance', 'ADMIN_FINANCE_READ', 'Hiện đọc cước xác nhận/UNPAID; chưa có collection, COD, ageing, settlement hoặc adjustment approval.'),
  module(12, 'Nhân sự & Truy cập', '/admin/security/staff', null, 'Cần Staff IAM governance contract; Admin READ không bao gồm IDENTITY_MANAGEMENT.'),
  module(13, 'Cấu hình & Feature Flags', '/admin/platform/configuration', null, 'Cần config registry, permission và approval policy; không dùng cờ UI để bật runtime.'),
  module(14, 'IT & Tích hợp', '/admin/platform/health', 'ADMIN_TECHNICAL_READ', 'Hiện có availability của API quản trị; chưa có telemetry jobs/queues/webhooks/security/release.'),
  module(15, 'Bảo mật & Tuân thủ', '/admin/security', null, 'Cần quyền Security, nguồn incident và policy truy cập PII; không suy từ chức danh.'),
  module(16, 'Kiểm toán & Báo cáo', '/admin/audit', null, 'Cần audit reader/export contract có scope và purpose; không mở SQL audit trực tiếp.'),
  module(17, 'Tài khoản & Công việc', '/admin/account', 'ADMIN_DASHBOARD_READ', 'Hiện xem context/quyền của chính mình và đăng xuất; chưa có task inbox, MFA/session inventory.'),
  module(18, 'Ủy quyền & Chính sách phê duyệt', '/admin/governance/authority', null, 'PROPOSED: cần delegated authority, giới hạn và SoD; approval không tự thực thi domain.'),
  module(19, 'Danh mục & Mã nghiệp vụ', '/admin/governance/reference-data', null, 'PROPOSED: cần taxonomy versioning và compatibility; không thay enum bằng label.'),
  module(20, 'Chất lượng & Nguồn dữ liệu', '/admin/data/quality', null, 'PROPOSED: cần lineage/anomaly source và correction workflow của domain owner.'),
  module(21, 'Liên tục kinh doanh & Khôi phục', '/admin/platform/continuity', null, 'PROPOSED: cần BCP/DR policy và evidence service; không kích hoạt restore qua UI.'),
  module(22, 'Release & Migration Governance', '/admin/platform/releases', null, 'PROPOSED: cần release evidence và quyền theo environment; không thực thi deploy bằng menu.'),
  module(23, 'Privacy, Retention & Legal Hold', '/admin/compliance/privacy', null, 'PROPOSED: cần privacy/retention policy; không tự đặt thời hạn lưu hoặc xóa audit.'),
];
export const adminGroups: readonly { title: string; modules: readonly string[] }[] = [
  { title: 'Tổng quan', modules: ['ADM-01', 'ADM-17', 'ADM-18'] },
  { title: 'Merchant & Phê duyệt', modules: ['ADM-02', 'ADM-03'] },
  { title: 'Dịch vụ & Mạng lưới', modules: ['ADM-04', 'ADM-05', 'ADM-06', 'ADM-07', 'ADM-19'] },
  { title: 'Giám sát vận hành', modules: ['ADM-08', 'ADM-09', 'ADM-20'] },
  { title: 'Hỗ trợ & Tài chính', modules: ['ADM-10', 'ADM-11'] },
  { title: 'Nhân sự & Bảo mật', modules: ['ADM-12', 'ADM-15', 'ADM-23'] },
  { title: 'Nền tảng & Tích hợp', modules: ['ADM-13', 'ADM-14', 'ADM-21', 'ADM-22'] },
  { title: 'Kiểm toán & Báo cáo', modules: ['ADM-16', 'ADM-20'] },
];
/** These are views of existing contracts, never a cross-portal grant. */
export const overviewEntries = [
  { id: 'executive', title: 'Executive Dashboard', path: '/admin' },
  { id: 'approvals', title: 'My Approvals', path: '/admin/account?tab=approvals' },
  { id: 'risk', title: 'Operational Risk', path: '/admin?tab=risk' },
  { id: 'health', title: 'Service Health', path: '/admin?tab=platform' },
] as const;
export function canOpenOverview(id: string, permissions: readonly Permission[], capabilities: readonly string[]) {
  return permissions.includes('ADMIN_DASHBOARD_READ') && (id === 'health' ? permissions.includes('ADMIN_TECHNICAL_READ') : id === 'approvals' ? capabilities.includes('task.read') && capabilities.includes('application.read') : ['executive', 'risk'].includes(id));
}
export function overviewSelection(path: string, search: string) {
  const tab = new URLSearchParams(search).get('tab');
  return path === '/admin/account' || path === '/admin/governance/authority' ? 'approvals' : ['/admin', '/admin/dashboard'].includes(path) ? tab === 'risk' ? 'risk' : tab === 'platform' ? 'health' : 'executive' : null;
}
export function resolveAdminModule(path: string) { return adminModules.find(m => m.path === path || m.id === 'ADM-01' && path === '/admin/dashboard'); }
export function canOpenModule(id: string, permissions: readonly Permission[], domainCapabilities: readonly string[] = []) {
  if (id === 'ADM-16') return permissions.includes('ADMIN_DASHBOARD_READ') && domainCapabilities.includes('audit.read');
  if (id === 'ADM-18') return permissions.includes('ADMIN_DASHBOARD_READ') && domainCapabilities.length > 0;
  const m = adminModules.find(m => m.id === id);
  return !!m?.permission && permissions.includes('ADMIN_DASHBOARD_READ') && permissions.includes(m.permission);
}
/** No approved cross-purpose discovery/activation port exists. Deny, never union grants. */
export function canSwitchPortal(current: Portal, target: Portal) { return current === target; }
export type TabId = 'overview' | 'merchant' | 'network' | 'finance' | 'platform' | 'decisions' | 'risk';
const tabs: readonly { id: TabId; title: string; permission: Permission }[] = [
  { id: 'overview', title: 'Tổng quan', permission: 'ADMIN_DASHBOARD_READ' },
  { id: 'merchant', title: 'Merchant Queue', permission: 'ADMIN_MERCHANT_READ' },
  { id: 'network', title: 'Network & SLA', permission: 'ADMIN_NETWORK_READ' },
  { id: 'finance', title: 'Finance Exposure', permission: 'ADMIN_FINANCE_READ' },
  { id: 'platform', title: 'Platform Health', permission: 'ADMIN_TECHNICAL_READ' },
  { id: 'risk', title: 'Operational Risk', permission: 'ADMIN_DASHBOARD_READ' },
  { id: 'decisions', title: 'Decisions', permission: 'ADMIN_DASHBOARD_READ' },
];
export function adminTabs(permissions: readonly Permission[]) { return permissions.includes('ADMIN_DASHBOARD_READ') ? tabs.filter(t => permissions.includes(t.permission)) : []; }
export function sectionState(permitted: boolean, section?: Section, degraded = false): DataState {
  if (!permitted) return 'FORBIDDEN';
  if (degraded) return 'DEGRADED';
  if (!section || section.availability !== 'AVAILABLE') return 'UNAVAILABLE';
  return section.metrics.length > 0 && section.metrics.every(m => m.value === '0') ? 'EMPTY' : 'AVAILABLE';
}
export function snapshotState(generatedAt: string, now: number): 'CURRENT' | 'STALE' {
  const cutoff = Date.parse(generatedAt);
  return Number.isFinite(cutoff) && Number.isFinite(now) && now >= cutoff && now - cutoff < 900000 ? 'CURRENT' : 'STALE';
}
