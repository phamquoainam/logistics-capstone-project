import {
  cancelDemoShipment, createDemoShipment, getDemoCurrentMerchant,
  getDemoCurrentUser, getDemoHealth, getDemoOverview, getDemoShipment,
  listDemoShipments, listDemoShipmentSummaries, listDemoUsers, loginDemo,
} from '@elogistics/api-client';
import type { CancelDemoShipmentData, CreateDemoShipmentData, LoginDemoData } from '@elogistics/api-client';
import { createClient } from '@elogistics/api-client/client';

// Presentation names only; wire definitions belong to the generated package.
export type {
  AccountView as User, MerchantView as Merchant, ShipmentView as Shipment,
  ShipmentEvent, ShipmentDetail as Detail, Overview, ShipmentSummary as Summary,
} from '@elogistics/api-client';

export class ApiError extends Error {
  constructor(public status: number, public code: string, message: string) { super(message); }
}
const fallback = 'Thao tác chưa hoàn tất. Kiểm tra backend và thử lại.';
const networkMessage = 'Không thể kết nối backend. Dữ liệu nhập vẫn được giữ; thử lại với cùng mã thao tác.';

async function unwrap<T>(pending: Promise<{ data?: T; error?: unknown; response?: Response }>): Promise<T> {
  const { data, error, response } = await pending;
  if (!response) throw new ApiError(0, 'NETWORK_ERROR', networkMessage);
  if (!response.ok) {
    const payload = typeof error === 'object' && error !== null ? error : {};
    const code = 'code' in payload && typeof payload.code === 'string' ? payload.code : 'API_ERROR';
    const message = 'message' in payload && typeof payload.message === 'string' ? payload.message : fallback;
    throw new ApiError(response.status, code, message);
  }
  // A parser exception is not a backend error payload; don't expose its snippet.
  if (data === undefined) throw new ApiError(response.status, 'API_ERROR', fallback);
  return data;
}

// No token is stored in client config or a singleton interceptor. Each protected
// call receives the current in-memory token; public calls remain anonymous.
export function createDemoApi(baseUrl = '/api/demo/v1') {
  const client = createClient({ baseUrl, parseAs: 'json', responseStyle: 'fields', throwOnError: false });
  return {
    health: () => unwrap(getDemoHealth({ client })),
    login: (body: LoginDemoData['body']) => unwrap(loginDemo({ client, body })),
    currentUser: (token?: string) => unwrap(getDemoCurrentUser({ client, auth: token })),
    merchant: (token?: string) => unwrap(getDemoCurrentMerchant({ client, auth: token })),
    shipments: (token?: string) => unwrap(listDemoShipments({ client, auth: token })),
    createShipment: (token: string, body: CreateDemoShipmentData['body'], key: string) =>
      unwrap(createDemoShipment({ client, auth: token, body, headers: { 'Idempotency-Key': key } })),
    shipment: (token: string, id: string) => unwrap(getDemoShipment({ client, auth: token, path: { id } })),
    cancelShipment: (token: string, id: string, body: CancelDemoShipmentData['body'], key: string) =>
      unwrap(cancelDemoShipment({ client, auth: token, path: { id }, body, headers: { 'Idempotency-Key': key } })),
    users: (token?: string) => unwrap(listDemoUsers({ client, auth: token })),
    overview: (token?: string) => unwrap(getDemoOverview({ client, auth: token })),
    summaries: (token?: string) => unwrap(listDemoShipmentSummaries({ client, auth: token })),
  };
}
export const api = createDemoApi();
export const formatDate = (value: string) => new Intl.DateTimeFormat('vi-VN', { dateStyle: 'short', timeStyle: 'short' }).format(new Date(value));
