import { organizationSupportQuery } from './organization-support';

export const organizationCategories = [
  ['ALL', 'Tất cả vận đơn'], ['DRAFT', 'Đơn nháp'], ['WAITING', 'Chờ bàn giao'],
  ['TRANSIT', 'Đang vận chuyển'], ['ACTION', 'Đơn cần xử lý'], ['COMPLETED', 'Đã hoàn tất'],
  ['RETURNED', 'Hoàn hàng'], ['CANCELLED', 'Đã hủy'], ['SUPPORT', 'Hỗ trợ liên quan'],
] as const;
export type OrganizationCategory = typeof organizationCategories[number][0];
export const organizationFilterKeys = ['page', 'search', 'from', 'until', 'status', 'service', 'origin', 'destination', 'obligation', 'handover', 'facilityId', 'creatorId', 'assignee', 'sourceChannel', 'batchId', 'sort'] as const;
export const organizationDependencies: Partial<Record<OrganizationCategory, [string, string]>> = {
  TRANSIT: ['Chưa mở dữ liệu vận chuyển Business', 'Cần sự kiện Carrier xử lý chặng. Booking hoặc chuẩn bị hàng nội bộ không đồng nghĩa Carrier đã nhận hàng.'],
  ACTION: ['Chưa kết nối yêu cầu xử lý Business', 'Các sự cố, lý do từ chối nhận và yêu cầu Customer phản hồi cần nguồn nghiệp vụ có authority. Không suy đoán số đơn cần xử lý.'],
  COMPLETED: ['Chưa mở kết quả giao hàng Business', 'Chỉ hiển thị khi Carrier có kết quả giao hàng và bằng chứng kết thúc hợp lệ. Merchant không được tự đánh dấu giao thành công.'],
  RETURNED: ['Chưa mở quy trình hoàn hàng Business', 'Cần yêu cầu, chặng và bằng chứng hoàn hàng hợp lệ từ Carrier.'],
  CANCELLED: ['Chưa mở hủy vận đơn đã Booking', 'Lưu trữ bản nháp không phải hủy Shipment. Yêu cầu hủy phải qua workflow được phép.'],
  SUPPORT: ['Chưa kết nối Ticket theo Organization', 'Ticket Personal không được dùng thay cho yêu cầu của doanh nghiệp. Claim và bồi thường có authority riêng.'],
};
export function organizationQuery(raw: URLSearchParams) {
  const category = raw.get('category') ?? (raw.get('status') === 'WAITING_FOR_HANDOVER' ? 'WAITING' : 'ALL'), search = (raw.get('search') ?? '').trim(), pageText = raw.get('page') ?? '0', page = Number(pageText);
  const valid = organizationCategories.some(([key]) => key === category) && /^(0|[1-9][0-9]*)$/.test(pageText) && Number.isSafeInteger(page) && page >= 0 && page <= 2147483647 && search.length <= 160 && !/[\u0000-\u001f\u007f]/.test(search) && [...raw.keys()].every(key => (['category', ...organizationFilterKeys] as string[]).includes(key) && raw.getAll(key).length === 1 && (raw.get(key)?.length ?? 0) <= 160);
  const query = new URLSearchParams(raw);
  query.delete('page'); query.delete('category'); query.delete('search');
  if (category !== 'ALL') query.set('category', category);
  if (search) query.set('search', search);
  if (page) query.set('page', String(page));
  const api = new URLSearchParams(query); api.delete('category'); if (category === 'WAITING') api.set('status', 'WAITING_FOR_HANDOVER');
  return { category: category as OrganizationCategory, search, page, query, api, valid };
}
export function organizationReturn(organization: string, value: string | null) {
  const path = '/merchant/' + organization + '/shipments';
  const reportPrefix = '/merchant/' + organization + '/';
  if (value && value.length <= 4096 && (value.startsWith(reportPrefix + 'reports') || value.startsWith(reportPrefix + 'finance'))) {
    const url = new URL(value, 'https://local.invalid'); const keys = ['from','until','interval','service','origin','destination','facilityId','creatorId','assignee','sourceChannel','batchId','status','obligation','handover','page','size','search','sort','panel'];
    if ([reportPrefix + 'reports', reportPrefix + 'finance'].includes(url.pathname) && !url.hash && [...url.searchParams.keys()].every(k => keys.includes(k) && url.searchParams.getAll(k).length === 1 && (url.searchParams.get(k)?.length ?? 0) <= 160 && !/[\u0000-\u001f\u007f]/.test(url.searchParams.get(k) ?? ''))) return value;
    return path;
  }
  if (value && value.length <= 4096 && (value === reportPrefix + 'support' || value.startsWith(reportPrefix + 'support?'))) {
    const url = new URL(value, 'https://local.invalid');
    return url.pathname === reportPrefix + 'support' && !url.hash && organizationSupportQuery(url.searchParams).valid ? value : path;
  }
  if (value === reportPrefix + 'lookup' || value === reportPrefix + 'labels') return value;
  if (!value || value.length > 4096 || value !== path && !value.startsWith(path + '?')) return path;
  const url = new URL(value, 'https://local.invalid');
  if (url.pathname !== path || url.hash || !organizationQuery(url.searchParams).valid) return path;
  return value;
}
export function organizationDetailLink(organization: string, shipment: string, query: URLSearchParams) {
  const path = '/merchant/' + organization + '/shipments', returnTo = path + (query.size ? '?' + query : '');
  return path + '/' + shipment + '?' + new URLSearchParams({ returnTo });
}
