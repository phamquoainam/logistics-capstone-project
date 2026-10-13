export interface User { id: string; username: string; displayName: string; role: string; enabled: boolean; merchantId: string | null }
export interface Merchant { id: string; displayName: string; reference: string }
export interface Shipment { id: string; merchantId: string; reference: string; pickupAddress: string; recipientName: string; recipientPhone: string; deliveryStreet: string; deliveryWard: string; deliveryProvince: string; parcelDescription: string; weightKg: number; status: 'DRAFT' | 'CANCELLED'; createdBy: string; createdAt: string; updatedAt: string }
export interface ShipmentEvent { id: string; eventType: string; actorId: string; reason: string | null; occurredAt: string }
export interface Detail { shipment: Shipment; events: ShipmentEvent[]; addressVerification: string }
export interface Overview { total: number; draft: number; cancelled: number }
export interface Summary { id: string; merchantId: string; reference: string; status: 'DRAFT' | 'CANCELLED'; createdAt: string }
export class ApiError extends Error { constructor(public status: number, public code: string, message: string) { super(message) } }
export async function request<T>(path: string, token?: string, options: RequestInit = {}): Promise<T> {
  let response: Response;
  try { response = await fetch(`/api/demo/v1${path}`, { ...options, headers: { ...(options.body ? { 'Content-Type': 'application/json' } : {}), ...(token ? { Authorization: `Bearer ${token}` } : {}), ...options.headers } }); }
  catch { throw new ApiError(0, 'NETWORK_ERROR', 'Không thể kết nối backend. Dữ liệu nhập vẫn được giữ; thử lại với cùng mã thao tác.'); }
  const body = await response.json().catch(() => null);
  if (!response.ok) throw new ApiError(response.status, body?.code ?? 'API_ERROR', body?.message ?? 'Thao tác chưa hoàn tất. Kiểm tra backend và thử lại.');
  return body as T;
}
export const formatDate = (value: string) => new Intl.DateTimeFormat('vi-VN', { dateStyle: 'short', timeStyle: 'short' }).format(new Date(value));
