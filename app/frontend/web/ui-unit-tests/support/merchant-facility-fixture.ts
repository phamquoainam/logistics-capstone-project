import type { BrowserContext } from '@playwright/test';
import { organizationA, organizationB } from './business-fixture';
import { reportingContext } from './business-report-fixture';
import type { FacilityInput, MerchantFacility } from '../../src/customer/merchant-facilities-data';
export const siteId = '10000000-0000-4000-8000-000000000021';
export const facilityContact = { name: 'Synthetic representative', phone: '+84901234567', address: { country: 'VN', administrativeArea: 'Synthetic area', locality: 'Synthetic locality', addressLine: 'Synthetic street' } };
export function facilityFixture(): MerchantFacility { return { id: siteId, organizationId: organizationA, name: 'Synthetic warehouse', code: 'WH-01', kind: 'WAREHOUSE', contact: facilityContact, senderContact: null, returnContact: null, operatingHours: null, state: 'ACTIVE', version: 1, createdAt: '2026-11-16T00:00:00Z', updatedAt: '2026-11-16T00:00:00Z', allowedActions: ['UPDATE', 'ARCHIVE', 'RESPONSIBILITY', 'SET_DEFAULT'] }; }
export async function merchantFacilityFixture(context: BrowserContext) {
  const state = { sites: [facilityFixture()], defaultVersion: 0, defaultSite: null as string | null, unknownNext: false, denied: false, teamVersion: 0, team: [] as { accountId: string; responsibility: string }[] };
  const writes: { path: string; body: unknown; expected: string | undefined }[] = [], searches: { path: string; body: unknown; method: string }[] = [], receipts = new Map<string, object>();
  const fullContext = { ...reportingContext, permissions: [...reportingContext.permissions, 'facility.read', 'facility.manage', 'defaults.manage'] };
  await context.route('**/api/v1/**', async route => {
    const req = route.request(), url = new URL(req.url()), path = url.pathname, method = req.method();
    const reply = (body: unknown, status = 200, etag?: string) => route.fulfill({ status, contentType: 'application/json', headers: { 'Cache-Control': 'no-store', ...(etag ? { ETag: etag } : {}) }, body: JSON.stringify(body) });
    if (path.endsWith('/csrf')) return reply({ challenge: 'synthetic-csrf' });
    if (path.endsWith('/refresh')) return reply({ accountId: organizationA, accessToken: 'synthetic-access', expiresAt: new Date(Date.now() + 900000).toISOString() });
    if (path.endsWith('/me')) return reply({ accountId: organizationA, displayName: 'Synthetic Customer', verifiedEmail: 'synthetic@example.invalid', securityVersion: 1 });
    if (path.endsWith('/business-contexts')) return reply({ schemaVersion: 'BUSINESS_CONTEXTS_V1', items: [fullContext] });
    const base = '/api/v1/merchant-organizations/' + organizationA;
    if (!path.startsWith(base)) return reply({ code: 'RESOURCE_NOT_FOUND' }, 404);
    if (path.includes('/facility-receipts/')) { const key = path.split('/').at(-1)!; return receipts.has(key) ? reply(receipts.get(key)) : reply({ code: 'RESOURCE_NOT_FOUND' }, 404); }
    if (state.denied) return reply({ code: 'RESOURCE_NOT_FOUND' }, 404);
    const site = state.sites.find(s => path.includes('/facilities/' + s.id));
    const body = method === 'GET' || !req.postData() ? undefined : req.postDataJSON();
    const write = async (operation: string, target: string, version: number, result: unknown, status = 200, prefix = 'v') => {
      writes.push({ path, body, expected: req.headers()['if-match'] }); receipts.set(req.headers()['idempotency-key'], { operationId: operation, targetId: target, version, outcome: 'COMMITTED' });
      if (state.unknownNext) { state.unknownNext = false; return route.abort('connectionfailed'); } return reply(result, status, '"' + prefix + version + '"');
    };
    if (path.endsWith('/facility-default')) {
      if (method === 'PUT') { state.defaultSite = body.facilityId; state.defaultVersion++; return write('DEFAULT', organizationA, state.defaultVersion, { facilityId: state.defaultSite, version: state.defaultVersion, updatedAt: '2026-11-16T00:00:00Z' }, 200, 'd'); }
      return reply({ facilityId: state.defaultSite, version: state.defaultVersion, updatedAt: state.defaultVersion ? '2026-11-16T00:00:00Z' : null }, 200, '"d' + state.defaultVersion + '"');
    }
    if (path.endsWith('/members')) return reply({ items: [{ accountId: organizationA, membershipId: organizationB, role: 'OWNER', displayName: 'Synthetic Customer' }], page: 0, size: 20, hasNext: false });
    if (site && path.endsWith('/responsibilities')) {
      if (method === 'PUT') { state.team = body.items; state.teamVersion++; return write('RESPONSIBILITY', site.id, state.teamVersion, { facilityId: site.id, version: state.teamVersion, updatedAt: '2026-11-16T00:00:00Z', items: state.team }); }
      return reply({ facilityId: site.id, version: state.teamVersion, updatedAt: state.teamVersion ? '2026-11-16T00:00:00Z' : null, items: state.team }, 200, '"v' + state.teamVersion + '"');
    }
    if (site && path.endsWith('/archive')) { if (state.defaultSite === site.id) return reply({ code: 'FACILITY_REFERENCED' }, 409); site.state = 'ARCHIVED'; site.version++; site.allowedActions = []; return write('ARCHIVE', site.id, site.version, site); }
    if (path === base + '/facilities' && method === 'POST') { const input = structuredClone(body) as FacilityInput; input.contact.phone = input.contact.phone.replace(/^0/, '+84'); const created: MerchantFacility = { ...facilityFixture(), ...input, id: '10000000-0000-4000-8000-000000000022', version: 1 }; state.sites.push(created); return write('CREATE', created.id, 1, created, 201); }
    if (site && method === 'PUT') { Object.assign(site, body, { version: site.version + 1 }); site.contact.phone = site.contact.phone.replace(/^0/, '+84'); return write('UPDATE', site.id, site.version, site); }
    if (site) return reply(site, 200, '"v' + site.version + '"');
    if (path.endsWith('/facilities') || path.endsWith('/facilities/search')) {
      searches.push({ path, body, method }); const query = method === 'POST' ? body : Object.fromEntries(url.searchParams); const text = String(query.search ?? '').toLowerCase(); const items = state.sites.filter(s => s.state === (query.state ?? 'ACTIVE') && (!query.kind || query.kind === s.kind) && (!text || (s.name + s.code + s.contact.name).toLowerCase().includes(text))); return reply({ items, page: Number(query.page ?? 0), size: 20, hasNext: false });
    }
    return reply({ code: 'RESOURCE_NOT_FOUND' }, 404);
  });
  return { state, writes, searches };
}
