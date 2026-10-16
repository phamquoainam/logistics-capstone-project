/**
 * C-05 compiler/Metro fixture only. Not imported by the starter entry or App.
 * Creating this module does not call an operation or supply account/config data.
 */
import {
  cancelDemoShipment, createDemoShipment, getDemoCurrentMerchant,
  getDemoCurrentUser, getDemoHealth, getDemoOverview, getDemoShipment,
  listDemoShipments, listDemoShipmentSummaries, listDemoUsers, loginDemo,
} from '@elogistics/api-client';
import type {
  AccountView, CancelDemoShipmentData, CreateDemoShipmentData,
  LoginDemoData, ShipmentDetail, ShipmentView,
} from '@elogistics/api-client';
import { createClient } from '@elogistics/api-client/client';

// Value exports deliberately make this an actual Metro dependency fixture,
// rather than a types-only import erased before the bundler sees the package.
export function createMobileContractConsumer(config: { baseUrl: string; fetch: typeof globalThis.fetch }) {
  const client = createClient({ baseUrl: config.baseUrl, fetch: config.fetch });
  return {
    health: () => getDemoHealth({ client }),
    login: (body: LoginDemoData['body']) => loginDemo({ client, body }),
    currentUser: (token: string) => getDemoCurrentUser({ client, auth: token }),
    merchant: (token: string) => getDemoCurrentMerchant({ client, auth: token }),
    shipments: (token: string) => listDemoShipments({ client, auth: token }),
    createShipment: (token: string, body: CreateDemoShipmentData['body'], headers: CreateDemoShipmentData['headers']) =>
      createDemoShipment({ client, auth: token, body, headers }),
    shipment: (token: string, id: string) => getDemoShipment({ client, auth: token, path: { id } }),
    cancelShipment: (token: string, path: CancelDemoShipmentData['path'], body: CancelDemoShipmentData['body'], headers: CancelDemoShipmentData['headers']) =>
      cancelDemoShipment({ client, auth: token, path, body, headers }),
    users: (token: string) => listDemoUsers({ client, auth: token }),
    overview: (token: string) => getDemoOverview({ client, auth: token }),
    summaries: (token: string) => listDemoShipmentSummaries({ client, auth: token }),
  };
}

type Equal<A, B> = (<T>() => T extends A ? 1 : 2) extends (<T>() => T extends B ? 1 : 2) ? true : false;
type Assert<T extends true> = T;

// These are checked by the existing Expo/RN tsconfig, not a stand-in DOM config.
export type MobileContractCompileChecks = [
  Assert<Equal<LoginDemoData['body']['password'], string>>,
  Assert<Equal<CreateDemoShipmentData['body']['weightKg'], number>>,
  Assert<Equal<CreateDemoShipmentData['headers']['Idempotency-Key'], string>>,
  Assert<Equal<CancelDemoShipmentData['path']['id'], string>>,
  Assert<Equal<CancelDemoShipmentData['body']['reason'], string>>,
  Assert<Equal<AccountView['merchantId'], string | null>>,
  Assert<Equal<ShipmentDetail['events'][number]['reason'], string | null>>,
  Assert<Equal<ShipmentView['status'], 'DRAFT' | 'CANCELLED'>>,
];

export type MobileCreateResult = Awaited<ReturnType<ReturnType<typeof createMobileContractConsumer>['createShipment']>>;
export type MobileCreateResponseCheck = Assert<Equal<NonNullable<MobileCreateResult['data']>, ShipmentView>>;
