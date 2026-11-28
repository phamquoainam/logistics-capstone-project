import { createStaffClient } from './staff-client';
import { customerClient, CustomerApiError } from './client';
import type { StationScan, CarrierStationView } from './client';
import type { StationOperationsClient } from './StationOperations';

const session = createStaffClient({ fetch: (...args) => fetch(...args), now: () => Date.now(), lock: work => navigator.locks ? navigator.locks.request('argopost.ops.session', work) : Promise.reject(new CustomerApiError('UNSUPPORTED_BROWSER')) }, 'OPERATIONS');
const sites = new Map<string, string>();
const object = (v: unknown): v is Record<string, unknown> => !!v && typeof v === 'object' && !Array.isArray(v);
const uuid = (v: unknown): v is string => typeof v === 'string' && /^[a-f0-9]{8}(?:-[a-f0-9]{4}){3}-[a-f0-9]{12}$/.test(v) && v !== '00000000-0000-0000-0000-000000000000';
const invalid = () => new CustomerApiError('RESPONSE_INVALID');
function path(code: string, suffix: string) { const id = sites.get(code); if (!id) throw new CustomerApiError('AUTHORITY_REQUIRED'); return 'ops/sites/' + id + suffix; }
function view(value: unknown, code: string, id?: string): CarrierStationView { return customerClient.parseCarrierStationView(value, code, id); }
const station: StationOperationsClient = {
  listStations: () => session.operationsRequest('ops/sites', value => {
    if (!object(value) || Object.keys(value).length !== 1 || !Array.isArray(value.sites) || value.sites.length > 1000) throw invalid();
    const next = new Map<string, string>(), result: { code: string }[] = [];
    for (const site of value.sites) {
      if (!object(site) || Object.keys(site).length !== 9 || !uuid(site.id) || typeof site.code !== 'string' || !/^[A-Z0-9-]{1,32}$/.test(site.code) || typeof site.name !== 'string' || !site.name || !['STATION', 'HUB'].includes(String(site.type)) || site.state !== 'ACTIVE' || !Number.isSafeInteger(site.version) || Number(site.version) < 1 || !Array.isArray(site.capabilities) || !site.capabilities.every(v => ['SITE_READ', 'STATION_INTAKE'].includes(String(v))) || typeof site.effectiveFrom !== 'string' || !Number.isFinite(Date.parse(site.effectiveFrom)) || site.effectiveTo !== null && (typeof site.effectiveTo !== 'string' || !Number.isFinite(Date.parse(site.effectiveTo))) || next.has(site.code)) throw invalid();
      next.set(site.code, site.id); if (site.type === 'STATION') result.push({ code: site.code });
    }
    sites.clear(); next.forEach((id, code) => sites.set(code, id)); return result;
  }),
  lookupStationParcel: (code, barcode) => { if (!/^AP-P-[a-f0-9]{32}$/.test(barcode)) return Promise.reject(new CustomerApiError('VALIDATION_FAILED')); return session.operationsRequest(path(code, '/parcels/' + barcode), v => view(v, code)); },
  readStationShipment: (code, id) => { if (!uuid(id)) return Promise.reject(new CustomerApiError('VALIDATION_FAILED')); return session.operationsRequest(path(code, '/shipments/' + id), v => view(v, code, id)); },
  scanStationParcel: (code, barcode, key) => session.operationsRequest(path(code, '/scans'), value => {
    if (!object(value) || Object.keys(value).length !== 6 || !uuid(value.id) || !uuid(value.shipmentId) || !uuid(value.parcelId) || value.stationCode !== code || value.barcode !== barcode || typeof value.scannedAt !== 'string' || !Number.isFinite(Date.parse(value.scannedAt))) throw invalid(); return value as StationScan;
  }, 'POST', { barcode }, key),
  acceptStationShipment: (code, id, scanIds, version, key) => {
    if (!uuid(id) || ![0, 1].includes(version) || !scanIds.length || scanIds.length > 5 || !scanIds.every(uuid) || new Set(scanIds).size !== scanIds.length) return Promise.reject(new CustomerApiError('VALIDATION_FAILED'));
    return session.operationsRequest(path(code, '/shipments/' + id + '/acceptance'), value => { const v = view(value, code, id); if (v.cargo.handling.state !== 'AT_STATION') throw invalid(); return v; }, 'POST', { scanIds, inspection: 'MATCHED' }, key, version);
  },
  stationReceipt: (code, key) => { if (!uuid(key)) return Promise.reject(new CustomerApiError('VALIDATION_FAILED')); return session.operationsRequest(path(code, '/receipts/' + key), value => {
    if (!object(value) || Object.keys(value).length !== 4 || !['SCAN', 'ACCEPT'].includes(String(value.operation)) || !uuid(value.targetId) || !uuid(value.shipmentId) || value.outcome !== 'COMMITTED') throw invalid(); return value as { operation: 'SCAN' | 'ACCEPT'; targetId: string; shipmentId: string; outcome: 'COMMITTED' };
  }); },
  downloadPilotLabel: (id, code) => { const site = code && sites.get(code); if (!site || !uuid(id)) return Promise.reject(new CustomerApiError('AUTHORITY_REQUIRED')); return session.operationsLabel(site, id); },
  logout: async () => { try { await session.logout(); } finally { sites.clear(); } },
};
session.subscribe(() => { if (session.getSnapshot().status !== 'authenticated') sites.clear(); });
export const operationsClient = { ...station, initialize: session.initialize, subscribe: session.subscribe, getSnapshot: session.getSnapshot, login: session.login, accept: session.accept };
