import { acceptRecoveryGrant, beginOtp, clearEntryProof, getOtpChallenge, getVerificationLocator, withEntryProof } from './entry-proof';
import { parseAnalytics } from './analytics-data';
export type Subject = { accountId: string; displayName: string; verifiedEmail: string; securityVersion: number };
export type ProfileInput = { displayName: string; preferredLanguage: 'vi' | 'en'; businessContactEmail?: string; businessContactPhone?: string };
export type CustomerProfile = ProfileInput & { id: string; accountId: string; version: number };
export type VersionedProfile = { profile: CustomerProfile; etag: string };
export type Enrollment = { email: string; password: string; displayName: string; phone: string; consentVersion: string };
export type ApplicationAddress = { country: string; administrativeArea: string; locality: string; addressLine: string };
export type ApplicationDraft = { entityType?: 'COMPANY' | 'HOUSEHOLD_BUSINESS'; jurisdiction?: string; registeredName?: string; legalIdentifier?: string; registeredAddress?: ApplicationAddress; operatingAddress?: ApplicationAddress; representativeName?: string; representativeEmail?: string; representativeRelationship?: string; businessContactAttestationId?: string; evidenceIds?: string[]; declarations?: { accuracy: true; representation: true; consentVersion: string } };
export type MerchantApplication = { id: string; applicantAccountId: string; state: 'DRAFT' | 'SUBMITTED' | 'UNDER_REVIEW' | 'NEEDS_INFORMATION' | 'APPROVED' | 'REJECTED' | 'WITHDRAWN'; revision: number; version: number; draft?: ApplicationDraft; submission?: ApplicationDraft; reviewRegionId?: string; informationRequest?: string; organizationId?: string };
export type VersionedApplication = { application: MerchantApplication; etag: string };
export type ApplicationPage = { items: MerchantApplication[]; page: number; size: number; hasNext: boolean };
export type AddressInput = { kind: 'SENDER' | 'RECIPIENT'; label?: string; contactName: string; contactPhone: string; address: ApplicationAddress; isDefault: boolean };
export type PersonalAddress = AddressInput & { id: string; accountId: string; verificationStatus: 'UNVERIFIED'; version: number; createdAt: string; updatedAt: string };
export type AddressPage = { items: PersonalAddress[]; page: number; size: number; hasNext: boolean };
export type VersionedAddress = { address: PersonalAddress; etag: string };
export type AddressOperation = 'createPersonalAddress' | 'updatePersonalAddress' | 'archivePersonalAddress';
export type DraftContact = { name: string; phone: string; address: ApplicationAddress };
export type DraftParcel = { description: string; quantity: number; weightGrams: number; lengthMm: number; widthMm: number; heightMm: number; declaredValueVnd: number };
export type ShippingDraftInput = { reference?: string | null; instructions?: string | null; sender: DraftContact; recipient: DraftContact; parcels: DraftParcel[] };
export type ShippingDraft = { id: string; accountId: string; ownerType: 'PERSONAL'; state: 'DRAFT'; input: ShippingDraftInput; version: number; createdAt: string; updatedAt: string };
export type ShippingDraftPage = { items: ShippingDraft[]; page: number; size: number; hasNext: boolean };
export type DraftOperation = 'createShippingDraft' | 'updateShippingDraft' | 'archiveShippingDraft';
export type ManagementPage = { schemaVersion: 'PERSONAL_MANAGEMENT_1'; generatedAt: string; timeZone: 'Asia/Ho_Chi_Minh'; summary: { totalShipments: string; waitingForHandover: string; atStation: string }; total: string; page: number; size: 20; hasNext: boolean; items: { record: PilotShipmentView; status: 'WAITING_FOR_HANDOVER' | 'AT_STATION' }[] };
export type PilotRoute = { datasetVersion: string; originCode: string; destinationCode: string; classification: 'INTRA' | 'INTER'; dropOffCode: string };
export type PilotCatalog = { simulation: true; serviceCode: 'ROAD_STANDARD'; quoteEnabled: true; bookingEnabled: true; coverage: { datasetVersion: string; namespace: string; provenance: 'SIMULATION'; effectiveFrom: string; areas: { code: string; region: string; label: string; dropOffCode: string }[]; routes: PilotRoute[] }; tariff: { version: string; intraFirstVnd: number; intraStepVnd: number; interFirstVnd: number; interStepVnd: number; stepGrams: number; volumetricDivisor: number; maxParcels: number; maxActualGrams: number; maxDimensionMm: number; maxGirthMm: number; ttlSeconds: number; taxTreatment: 'SIMULATION_NOT_TAX_INVOICE' }; paymentMethod: 'CASH_ON_HANDOVER'; payer: 'SENDER'; paymentCollectionEnabled: false };
export type PilotQuote = { id: string; accountId: string; draftId: string; draftVersion: number; tariffVersion: string; route: PilotRoute; parcels: { weightGrams: number; lengthMm: number; widthMm: number; heightMm: number }[]; feeLines: { parcelNumber: number; actualGrams: number; chargeableGrams: number; feeVnd: number }[]; totalVnd: number; taxTreatment: 'SIMULATION_NOT_TAX_INVOICE'; createdAt: string; expiresAt: string; consumedBy: string | null };
export type PilotQuoteView = { quote: PilotQuote; usable: boolean; serverTime: string };
export type PilotShipment = { id: string; accountId: string; code: string; draftId: string; draftVersion: number; bookingState: 'CONFIRMED'; carrierState: 'NOT_ACCEPTED'; input: ShippingDraftInput; quote: PilotQuote; totalVnd: number; createdAt: string; parcels: { id: string; parcelNumber: number; facts: DraftParcel }[] };
export type PilotShipmentView = { simulation: true; shipment: PilotShipment; obligation: { id: string; accountId: string; shipmentId: string; quoteId: string; amountVnd: number; currency: 'VND'; state: 'UNPAID'; payer: 'SENDER'; method: 'CASH_ON_HANDOVER'; policyVersion: 'BR13-LOCAL-PILOT-01'; createdAt: string } };
export type PilotShipmentPage = { items: PilotShipmentView[]; page: number; size: number; hasNext: boolean };
export type PilotHandling = { shipmentId: string; state: 'WAITING_FOR_HANDOVER' | 'AT_STATION'; version: 0 | 1; parcels: { parcelId: string; parcelNumber: number; barcode: string; state: 'NOT_ACCEPTED' | 'AT_STATION' }[]; events: { id: string; type: 'CARRIER_ACCEPTED_AT_STATION'; stationCode: string; occurredAt: string; provenance: 'SIMULATION' }[]; labelAvailable: true; simulation: true };
export type Station = { code: string; datasetVersion: 'ARGO-SIM-COVERAGE-1'; assignmentVersion: number; permission: 'STATION_INTAKE'; acceptanceEnabled: true };
export type StationScan = { id: string; shipmentId: string; parcelId: string; barcode: string; stationCode: string; scannedAt: string };
export type StationView = { cargo: { shipment: PilotShipment; handling: PilotHandling }; scans: StationScan[]; paymentState: 'UNPAID'; simulation: true; policyVersion: 'BR13-LOCAL-INTAKE-01' };
export type SessionState = Readonly<{ status: 'loading' | 'guest' | 'authenticated'; subject: Subject | null; error: string | null }>;
type Access = { accessToken: string; expiresAt: string; accountId: string };
type Lock = <T>(work: () => Promise<T>) => Promise<T>;
type Ports = { fetch: typeof fetch; lock: Lock; now: () => number };
const uuid = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/;
const record = (v: unknown): v is Record<string, unknown> => typeof v === 'object' && v !== null && !Array.isArray(v);
const text = (v: unknown, maximum: number): v is string => typeof v === 'string' && v.length > 0 && v.length <= maximum;
const characters = (v: unknown, maximum: number): v is string => typeof v === 'string' && [...v].length > 0 && [...v].length <= maximum;
const version = (v: unknown): v is number => Number.isSafeInteger(v) && (v as number) >= 1;
function exact(v: Record<string, unknown>, required: string[], optional: string[] = []): boolean {
  return required.every(k => Object.hasOwn(v, k)) && Object.keys(v).every(k => required.includes(k) || optional.includes(k));
}
export class CustomerApiError extends Error {
  constructor(readonly code: string, readonly status = 0, readonly retryAfter?: number) { super(code); this.name = 'CustomerApiError'; }
}
const unknown = () => new CustomerApiError('OUTCOME_UNKNOWN');
function malformed(): never { throw new CustomerApiError('RESPONSE_INVALID'); }
const browserLock: Lock = work => {
  if (typeof navigator === 'undefined' || !navigator.locks) return Promise.reject(new CustomerApiError('UNSUPPORTED_BROWSER'));
  return navigator.locks.request('elogistics.customer.session', work);
};
/** Same-origin purpose-specific transport; no persistent token, demo fallback or blind POST retry. */
export function createCustomerClient(ports: Ports = { fetch: (...args) => fetch(...args), lock: browserLock, now: () => Date.now() }) {
  let access: Access | undefined, generation = 0, initialization: Promise<void> | undefined;
  let state: SessionState = Object.freeze({ status: 'loading', subject: null, error: null });
  let pending: { key: string; route: string; body: string; challenge: string } | undefined;
  const listeners = new Set<() => void>();
  const channel = typeof window !== 'undefined' && typeof BroadcastChannel !== 'undefined' ? new BroadcastChannel('elogistics.customer.session-state') : undefined;
  function publish(next: SessionState) { state = Object.freeze(next); listeners.forEach(f => f()); }
  function forget(error: string | null = null) { access = undefined; generation++; pending = undefined; publish({ status: 'guest', subject: null, error }); }
  if (channel) channel.onmessage = () => forget('SESSION_CHANGED');
  async function request(path: string, method = 'GET', body?: string, headers: Record<string, string> = {}, maximum = 65536): Promise<{ value: unknown; response: Response }> {
    let response: Response;
    try { response = await ports.fetch('/api/v1/' + path, { method, body, headers: body === undefined ? headers : { ...headers, 'Content-Type': 'application/json' }, credentials: 'same-origin', mode: 'same-origin', cache: 'no-store', redirect: 'error', referrerPolicy: 'no-referrer', signal: AbortSignal.timeout(30000) }); }
    catch { throw method === 'GET' ? new CustomerApiError('NETWORK_UNAVAILABLE') : unknown(); }
    let value: unknown;
    try {
      const raw = await response.text(); if (raw.length > maximum) malformed();
      if (response.status !== 204) { if (!response.headers.get('Content-Type')?.toLowerCase().startsWith('application/json')) malformed(); value = JSON.parse(raw); }
      else if (raw !== '') malformed();
    } catch { throw method === 'GET' ? new CustomerApiError('RESPONSE_INVALID') : unknown(); }
    if (!response.ok) {
      const code = record(value) && typeof value.code === 'string' && /^[A-Z][A-Z0-9_]{0,63}$/.test(value.code) ? value.code : 'REQUEST_REJECTED';
      const retry = Number(response.headers.get('Retry-After'));
      if (response.status === 401 && access) forget('SESSION_INVALID');
      throw new CustomerApiError(code, response.status, Number.isInteger(retry) && retry > 0 && retry <= 86400 ? retry : undefined);
    }
    return { value, response };
  }
  async function csrf(): Promise<string> {
    const { value } = await request('customer-auth/csrf');
    if (!record(value) || !exact(value, ['challenge']) || !text(value.challenge, 512)) malformed(); return value.challenge;
  }
  function readAccess(value: unknown): Access {
    if (!record(value) || !exact(value, ['accessToken', 'expiresAt', 'accountId']) || !text(value.accessToken, 8192) || typeof value.accountId !== 'string' || !uuid.test(value.accountId) || typeof value.expiresAt !== 'string' || !Number.isFinite(Date.parse(value.expiresAt)) || Date.parse(value.expiresAt) <= ports.now() || Date.parse(value.expiresAt) > ports.now() + 901000) malformed();
    return value as Access;
  }
  async function subject(): Promise<Subject> {
    if (!access) throw new CustomerApiError('SESSION_INVALID');
    const expected = access.accountId, current = generation;
    const { value } = await request('customer-auth/me', 'GET', undefined, { Authorization: 'Bearer ' + access.accessToken });
    if (current !== generation) throw new CustomerApiError('SESSION_CHANGED');
    if (!record(value) || !exact(value, ['accountId', 'displayName', 'verifiedEmail', 'securityVersion']) || value.accountId !== expected || !characters(value.displayName, 160) || !text(value.verifiedEmail, 254) || !version(value.securityVersion)) malformed(); return value as Subject;
  }
  async function refreshLocked(): Promise<void> {
    try {
      const challenge = await csrf(), result = await request('customer-auth/refresh', 'POST', undefined, { 'X-CSRF-Token': challenge });
      access = readAccess(result.value); generation++; const user = await subject(); publish({ status: 'authenticated', subject: user, error: null });
    } catch (error) { forget(error instanceof CustomerApiError ? error.code : 'SESSION_INVALID'); throw error; }
  }
  async function ensureAccess(): Promise<void> { if (!access) throw new CustomerApiError('SESSION_INVALID'); if (Date.parse(access.expiresAt) <= ports.now() + 30000) await refreshLocked(); }
  async function logoutLocked(): Promise<void> {
    const challenge = await csrf(); await request('customer-auth/logout', 'POST', undefined, { 'X-CSRF-Token': challenge }); forget(); channel?.postMessage('changed');
  }
  async function preauth(route: string, input: object, key: string, noContent: boolean): Promise<string | undefined> {
    if (!uuid.test(key)) throw new CustomerApiError('VALIDATION_FAILED');
    return ports.lock(async () => {
      if (access) throw new CustomerApiError('CUSTOMER_ALREADY_SIGNED_IN');
      const body = JSON.stringify(input);
      if (pending?.key === key && (pending.route !== route || pending.body !== body)) throw new CustomerApiError('IDEMPOTENCY_CONFLICT');
      const challenge = pending?.key === key ? pending.challenge : await csrf(); pending = { key, route, body, challenge };
      const { value, response } = await request('customer-auth/' + route, 'POST', body, { 'X-CSRF-Token': challenge, 'Idempotency-Key': key });
      if (noContent ? response.status !== 204 : response.status !== 202 || !record(value) || !exact(value, ['accepted', 'requestId']) || value.accepted !== true || typeof value.requestId !== 'string' || !uuid.test(value.requestId)) throw unknown();
      pending = undefined;
      return !noContent && record(value) ? value.requestId as string : undefined;
    });
  }
  function profile(value: unknown, response: Response): VersionedProfile {
    if (!record(value) || !exact(value, ['id', 'accountId', 'displayName', 'preferredLanguage', 'version'], ['businessContactEmail', 'businessContactPhone']) || typeof value.id !== 'string' || !uuid.test(value.id) || value.accountId !== access?.accountId || !version(value.version) || !characters(value.displayName, 160) || !['vi', 'en'].includes(value.preferredLanguage as string) || ('businessContactEmail' in value && !text(value.businessContactEmail, 254)) || ('businessContactPhone' in value && !characters(value.businessContactPhone, 32))) malformed();
    const etag = response.headers.get('ETag'); if (etag !== '"v' + value.version + '"') malformed(); return { profile: value as CustomerProfile, etag };
  }
  function draft(value: unknown): value is ApplicationDraft {
    if (!record(value) || !exact(value, [], ['entityType', 'jurisdiction', 'registeredName', 'legalIdentifier', 'registeredAddress', 'operatingAddress', 'representativeName', 'representativeEmail', 'representativeRelationship', 'businessContactAttestationId', 'evidenceIds', 'declarations'])) return false;
    if ('entityType' in value && !['COMPANY', 'HOUSEHOLD_BUSINESS'].includes(value.entityType as string)) return false;
    for (const [name, max] of Object.entries({ jurisdiction: 64, registeredName: 240, legalIdentifier: 80, representativeName: 160, representativeEmail: 254, representativeRelationship: 160 })) if (name in value && !characters(value[name], max)) return false;
    for (const name of ['registeredAddress', 'operatingAddress']) if (name in value) {
      const a = value[name]; if (!record(a) || !exact(a, ['country', 'administrativeArea', 'locality', 'addressLine']) || typeof a.country !== 'string' || !/^[A-Z]{2}$/.test(a.country) || !characters(a.administrativeArea, 160) || !characters(a.locality, 160) || !characters(a.addressLine, 500)) return false;
    }
    if ('businessContactAttestationId' in value && (typeof value.businessContactAttestationId !== 'string' || !uuid.test(value.businessContactAttestationId))) return false;
    if ('evidenceIds' in value && (!Array.isArray(value.evidenceIds) || value.evidenceIds.length > 5 || new Set(value.evidenceIds).size !== value.evidenceIds.length || value.evidenceIds.some(id => typeof id !== 'string' || !uuid.test(id)))) return false;
    if ('declarations' in value) { const d = value.declarations; if (!record(d) || !exact(d, ['accuracy', 'representation', 'consentVersion']) || d.accuracy !== true || d.representation !== true || !characters(d.consentVersion, 64)) return false; }
    return true;
  }
  function application(value: unknown): MerchantApplication {
    if (!record(value) || !exact(value, ['id', 'applicantAccountId', 'state', 'revision', 'version'], ['draft', 'submission', 'reviewRegionId', 'informationRequest', 'organizationId']) || typeof value.id !== 'string' || !uuid.test(value.id) || value.applicantAccountId !== access?.accountId || !version(value.revision) || !version(value.version) || !['DRAFT', 'SUBMITTED', 'UNDER_REVIEW', 'NEEDS_INFORMATION', 'APPROVED', 'REJECTED', 'WITHDRAWN'].includes(value.state as string)) malformed();
    for (const name of ['draft', 'submission']) if (name in value && !draft(value[name])) malformed();
    if ('reviewRegionId' in value && (typeof value.reviewRegionId !== 'string' || !uuid.test(value.reviewRegionId))) malformed();
    if ('informationRequest' in value && !characters(value.informationRequest, 1000)) malformed();
    if (value.state === 'APPROVED' ? typeof value.organizationId !== 'string' || !uuid.test(value.organizationId) : 'organizationId' in value) malformed();
    return value as MerchantApplication;
  }
  async function applicationWrite(path: string, method: 'POST' | 'PUT', input: ApplicationDraft, key: string, etag?: string): Promise<VersionedApplication> {
    if (!uuid.test(key) || !draft(input) || method === 'PUT' && !/^"v[1-9][0-9]*"$/.test(etag ?? '')) throw new CustomerApiError('VALIDATION_FAILED');
    await ensureAccess(); const current = generation, challenge = await csrf();
    const result = await request(path, method, JSON.stringify(input), { Authorization: 'Bearer ' + access!.accessToken, 'X-CSRF-Token': challenge, 'Idempotency-Key': key, ...(etag ? { 'If-Match': etag } : {}) });
    if (current !== generation) throw new CustomerApiError('SESSION_CHANGED');
    try { const value = application(result.value); if (result.response.status !== (method === 'POST' ? 201 : 200) || result.response.headers.get('ETag') !== '"v' + value.version + '"') malformed(); return { application: value, etag: '"v' + value.version + '"' }; } catch { throw unknown(); }
  }
  function personalAddress(value: unknown): PersonalAddress {
    if (!record(value) || !exact(value, ['id', 'accountId', 'kind', 'contactName', 'contactPhone', 'address', 'isDefault', 'verificationStatus', 'version', 'createdAt', 'updatedAt'], ['label']) || typeof value.id !== 'string' || !uuid.test(value.id) || value.accountId !== access?.accountId || !version(value.version) || !['SENDER', 'RECIPIENT'].includes(value.kind as string) || !characters(value.contactName, 160) || typeof value.contactPhone !== 'string' || !/^\+84[35789]\d{8}$/.test(value.contactPhone) || 'label' in value && !characters(value.label, 80) || typeof value.isDefault !== 'boolean' || value.verificationStatus !== 'UNVERIFIED') malformed();
    const a = value.address;
    if (!record(a) || !exact(a, ['country', 'administrativeArea', 'locality', 'addressLine']) || a.country !== 'VN' || !characters(a.administrativeArea, 160) || !characters(a.locality, 160) || !characters(a.addressLine, 500)) malformed();
    if (typeof value.createdAt !== 'string' || !Number.isFinite(Date.parse(value.createdAt)) || typeof value.updatedAt !== 'string' || !Number.isFinite(Date.parse(value.updatedAt)) || Date.parse(value.updatedAt) < Date.parse(value.createdAt)) malformed();
    return value as PersonalAddress;
  }
  function shippingInput(value: unknown): value is ShippingDraftInput {
    if (!record(value) || !exact(value, ['sender', 'recipient', 'parcels'], ['reference', 'instructions'])) return false;
    if (value.reference != null && !characters(value.reference, 80) || value.instructions != null && !characters(value.instructions, 1000)) return false;
    for (const k of ['sender', 'recipient']) { const c = value[k]; if (!record(c) || !exact(c, ['name', 'phone', 'address']) || !characters(c.name, 160) || typeof c.phone !== 'string' || !/^\+84[35789][0-9]{8}$/.test(c.phone) || !record(c.address) || !exact(c.address, ['country', 'administrativeArea', 'locality', 'addressLine']) || c.address.country !== 'VN' || !characters(c.address.administrativeArea, 160) || !characters(c.address.locality, 160) || !characters(c.address.addressLine, 500)) return false; }
    return Array.isArray(value.parcels) && value.parcels.length >= 1 && value.parcels.length <= 5 && value.parcels.every(p => record(p) && exact(p, ['description', 'quantity', 'weightGrams', 'lengthMm', 'widthMm', 'heightMm', 'declaredValueVnd']) && characters(p.description, 160) && ['quantity', 'weightGrams', 'lengthMm', 'widthMm', 'heightMm', 'declaredValueVnd'].every(k => Number.isSafeInteger(p[k]) && (p[k] as number) >= (k === 'declaredValueVnd' ? 0 : 1) && (p[k] as number) <= (k === 'declaredValueVnd' ? 1e12 : k === 'weightGrams' ? 1e9 : 1e6)));
  }
  function shippingDraft(value: unknown): ShippingDraft {
    if (!record(value) || !exact(value, ['id', 'accountId', 'ownerType', 'state', 'input', 'version', 'createdAt', 'updatedAt']) || typeof value.id !== 'string' || !uuid.test(value.id) || value.accountId !== state.subject?.accountId || value.ownerType !== 'PERSONAL' || value.state !== 'DRAFT' || !version(value.version) || !shippingInput(value.input) || typeof value.createdAt !== 'string' || typeof value.updatedAt !== 'string' || !Number.isFinite(Date.parse(value.createdAt)) || !Number.isFinite(Date.parse(value.updatedAt)) || Date.parse(value.updatedAt) < Date.parse(value.createdAt)) malformed();
    return value as ShippingDraft;
  }
  async function draftWrite(path: string, method: 'POST' | 'PUT', input: ShippingDraftInput | undefined, key: string, etag?: string, archive = false): Promise<ShippingDraft | undefined> {
    if (!uuid.test(key) || etag !== undefined && !/^"v[1-9][0-9]*"$/.test(etag)) throw new CustomerApiError('VALIDATION_FAILED');
    await ensureAccess(); const current = generation, challenge = await csrf();
    const { value, response } = await request('customer/shipment-drafts' + path, method, input === undefined ? undefined : JSON.stringify(input), { Authorization: 'Bearer ' + access!.accessToken, 'X-CSRF-Token': challenge, 'Idempotency-Key': key, ...(etag ? { 'If-Match': etag } : {}) });
    if (current !== generation) throw new CustomerApiError('SESSION_CHANGED');
    if (archive) { if (response.status !== 204) throw unknown(); return; }
    try { const result = shippingDraft(value); if (response.headers.get('ETag') !== '"v' + result.version + '"' || path && result.id !== path.slice(1)) malformed(); return result; } catch { throw unknown(); }
  }
  const date = (v: unknown): v is string => typeof v === 'string' && Number.isFinite(Date.parse(v));
  const money = (v: unknown): v is number => Number.isSafeInteger(v) && (v as number) > 0 && (v as number) <= 1e12;
  function pilotRoute(v: unknown): v is PilotRoute { return record(v) && exact(v, ['datasetVersion', 'originCode', 'destinationCode', 'classification', 'dropOffCode']) && text(v.datasetVersion, 64) && text(v.originCode, 32) && text(v.destinationCode, 32) && v.originCode !== v.destinationCode && ['INTRA', 'INTER'].includes(String(v.classification)) && text(v.dropOffCode, 32); }
  function pilotQuote(v: unknown, owner = state.subject?.accountId): PilotQuote {
    if (!record(v) || !exact(v, ['id', 'accountId', 'draftId', 'draftVersion', 'tariffVersion', 'route', 'parcels', 'feeLines', 'totalVnd', 'taxTreatment', 'createdAt', 'expiresAt', 'consumedBy']) || typeof v.id !== 'string' || !uuid.test(v.id) || v.accountId !== owner || typeof v.draftId !== 'string' || !uuid.test(v.draftId) || !version(v.draftVersion) || !text(v.tariffVersion, 64) || !pilotRoute(v.route) || !money(v.totalVnd) || v.taxTreatment !== 'SIMULATION_NOT_TAX_INVOICE' || !date(v.createdAt) || !date(v.expiresAt) || Date.parse(v.expiresAt) - Date.parse(v.createdAt) !== 900000 || v.consumedBy !== null && (typeof v.consumedBy !== 'string' || !uuid.test(v.consumedBy)) || !Array.isArray(v.parcels) || !v.parcels.length || v.parcels.length > 5 || !Array.isArray(v.feeLines) || v.feeLines.length !== v.parcels.length) malformed();
    if (!v.parcels.every(p => record(p) && exact(p, ['weightGrams', 'lengthMm', 'widthMm', 'heightMm']) && Object.values(p).every(version)) || !v.feeLines.every((line, i) => record(line) && exact(line, ['parcelNumber', 'actualGrams', 'chargeableGrams', 'feeVnd']) && line.parcelNumber === i + 1 && line.actualGrams === (v.parcels as PilotQuote['parcels'])[i].weightGrams && version(line.chargeableGrams) && line.chargeableGrams % 500 === 0 && money(line.feeVnd)) || v.feeLines.reduce((sum, line) => sum + (line as { feeVnd: number }).feeVnd, 0) !== v.totalVnd) malformed();
    return v as PilotQuote;
  }
  function quoteView(v: unknown): PilotQuoteView { if (!record(v) || !exact(v, ['quote', 'usable', 'serverTime']) || typeof v.usable !== 'boolean' || !date(v.serverTime)) malformed(); const q = pilotQuote(v.quote); if (v.usable && (q.consumedBy !== null || Date.parse(v.serverTime) >= Date.parse(q.expiresAt))) malformed(); return { quote: q, usable: v.usable, serverTime: v.serverTime }; }
  function shipmentSnapshot(d: unknown, owner = state.subject?.accountId): PilotShipment {
    if (!record(d) || typeof d.accountId !== "string" || !uuid.test(d.accountId)) malformed();
    if (!exact(d, ['id', 'accountId', 'code', 'draftId', 'draftVersion', 'bookingState', 'carrierState', 'input', 'quote', 'totalVnd', 'createdAt', 'parcels']) || typeof d.id !== 'string' || !uuid.test(d.id) || d.accountId !== owner || typeof d.code !== 'string' || !/^AP-SIM-[a-f0-9]{32}$/.test(d.code) || typeof d.draftId !== 'string' || !uuid.test(d.draftId) || !version(d.draftVersion) || d.bookingState !== 'CONFIRMED' || d.carrierState !== 'NOT_ACCEPTED' || !shippingInput(d.input) || !money(d.totalVnd) || !date(d.createdAt) || !Array.isArray(d.parcels) || d.parcels.length !== d.input.parcels.length) malformed();
    const q = pilotQuote(d.quote, owner); if (q.consumedBy !== d.id || q.draftId !== d.draftId || q.draftVersion !== d.draftVersion || q.totalVnd !== d.totalVnd || !d.parcels.every((p, i) => record(p) && exact(p, ['id', 'parcelNumber', 'facts']) && typeof p.id === 'string' && uuid.test(p.id) && p.parcelNumber === i + 1 && record(p.facts) && Object.entries((d.input as ShippingDraftInput).parcels[i]).every(([k, value]) => (p.facts as Record<string, unknown>)[k] === value))) malformed();
    return d as PilotShipment;
  }
  function shipmentView(v: unknown): PilotShipmentView {
    if (!record(v) || !exact(v, ['shipment', 'obligation', 'simulation']) || v.simulation !== true || !record(v.shipment) || !record(v.obligation)) malformed(); const d = shipmentSnapshot(v.shipment), o = v.obligation, q = d.quote;
    if (!exact(o, ['id', 'accountId', 'shipmentId', 'quoteId', 'amountVnd', 'currency', 'state', 'payer', 'method', 'policyVersion', 'createdAt']) || typeof o.id !== 'string' || !uuid.test(o.id) || o.accountId !== d.accountId || o.shipmentId !== d.id || o.quoteId !== q.id || o.amountVnd !== q.totalVnd || o.currency !== 'VND' || o.state !== 'UNPAID' || o.payer !== 'SENDER' || o.method !== 'CASH_ON_HANDOVER' || o.policyVersion !== 'BR13-LOCAL-PILOT-01' || !date(o.createdAt)) malformed();
    return v as PilotShipmentView;
  }
  const stationCode = (v: unknown): v is string => typeof v === 'string' && ['SIM-DROP-A1', 'SIM-DROP-A2', 'SIM-DROP-B1'].includes(v);
  function handlingValue(v: unknown, shipment: string): PilotHandling {
    if (!record(v) || !exact(v, ['shipmentId', 'state', 'version', 'parcels', 'events', 'labelAvailable', 'simulation']) || v.shipmentId !== shipment || v.simulation !== true || v.labelAvailable !== true || !['WAITING_FOR_HANDOVER', 'AT_STATION'].includes(String(v.state)) || v.version !== (v.state === 'AT_STATION' ? 1 : 0) || !Array.isArray(v.parcels) || !v.parcels.length || v.parcels.length > 5 || !Array.isArray(v.events) || v.events.length !== v.version) malformed();
    if (!v.parcels.every((p, i) => record(p) && exact(p, ['parcelId', 'parcelNumber', 'barcode', 'state']) && typeof p.parcelId === 'string' && uuid.test(p.parcelId) && p.parcelNumber === i + 1 && p.barcode === 'AP-P-' + p.parcelId.replaceAll('-', '') && p.state === (v.version === 1 ? 'AT_STATION' : 'NOT_ACCEPTED')) || new Set(v.parcels.map(p => (p as { parcelId: string }).parcelId)).size !== v.parcels.length || !v.events.every(e => record(e) && exact(e, ['id', 'type', 'stationCode', 'occurredAt', 'provenance']) && typeof e.id === 'string' && uuid.test(e.id) && e.type === 'CARRIER_ACCEPTED_AT_STATION' && stationCode(e.stationCode) && date(e.occurredAt) && e.provenance === 'SIMULATION')) malformed();
    return v as PilotHandling;
  }
  function scanValue(v: unknown, station: string): StationScan {
    if (!record(v) || !exact(v, ['id', 'shipmentId', 'parcelId', 'barcode', 'stationCode', 'scannedAt']) || !['id', 'shipmentId', 'parcelId'].every(k => typeof v[k] === 'string' && uuid.test(v[k] as string)) || v.barcode !== 'AP-P-' + (v.parcelId as string).replaceAll('-', '') || v.stationCode !== station || !date(v.scannedAt)) malformed(); return v as StationScan;
  }
  function stationView(v: unknown, station: string, shipment?: string): StationView {
    if (!record(v) || !exact(v, ['cargo', 'scans', 'paymentState', 'simulation', 'policyVersion']) || v.simulation !== true || v.paymentState !== 'UNPAID' || v.policyVersion !== 'BR13-LOCAL-INTAKE-01' || !record(v.cargo) || !exact(v.cargo, ['shipment', 'handling']) || !record(v.cargo.shipment) || typeof v.cargo.shipment.accountId !== 'string' || !uuid.test(v.cargo.shipment.accountId) || !Array.isArray(v.scans) || v.scans.length > 5) malformed();
    const d = shipmentSnapshot(v.cargo.shipment, v.cargo.shipment.accountId), h = handlingValue(v.cargo.handling, d.id), scans = v.scans.map(s => scanValue(s, station));
    if (shipment && d.id !== shipment || d.quote.route.dropOffCode !== station || d.parcels.length !== h.parcels.length || d.parcels.some((p, i) => p.id !== h.parcels[i].parcelId) || h.events.some(e => e.stationCode !== station) || scans.some(s => s.shipmentId !== d.id || !d.parcels.some(p => p.id === s.parcelId)) || new Set(scans.map(s => s.parcelId)).size !== scans.length) malformed();return v as StationView;
  }
  async function stationRequest(station: string | undefined, suffix: string, method = 'GET', input?: unknown, key?: string, etag?: string) {
    if (station !== undefined && !stationCode(station) || key !== undefined && !uuid.test(key) || etag !== undefined && !/^"i[01]"$/.test(etag)) throw new CustomerApiError('VALIDATION_FAILED');
    await ensureAccess(); const current = generation, challenge = method === 'GET' ? undefined : await csrf();
    const result = await request('station-operations/stations' + (station ? '/' + station : '') + suffix, method, input === undefined ? undefined : JSON.stringify(input), { Authorization: 'Bearer ' + access!.accessToken, ...(challenge ? { 'X-CSRF-Token': challenge } : {}), ...(key ? { 'Idempotency-Key': key } : {}), ...(etag ? { 'If-Match': etag } : {}) }, 131072);if (current !== generation) throw new CustomerApiError('SESSION_CHANGED');return result;
  }
  async function privateLabel(shipment: string, station?: string): Promise<Blob> {
    if (!uuid.test(shipment) || station !== undefined && !stationCode(station)) throw new CustomerApiError('VALIDATION_FAILED');await ensureAccess();const current = generation;let r: Response;
    try { r = await ports.fetch('/api/v1/' + (station ? 'station-operations/stations/' + station : 'customer') + '/shipments/' + shipment + '/labels.pdf', { method: 'GET', headers: { Authorization: 'Bearer ' + access!.accessToken }, credentials: 'same-origin', mode: 'same-origin', cache: 'no-store', redirect: 'error', referrerPolicy: 'no-referrer', signal: AbortSignal.timeout(30000) }); } catch { throw new CustomerApiError('NETWORK_UNAVAILABLE'); }
    if (!r.ok) { if (r.status === 401) forget('SESSION_INVALID');throw new CustomerApiError(r.status === 404 ? 'RESOURCE_NOT_FOUND' : 'REQUEST_REJECTED', r.status); }
    if (r.headers.get('Content-Type')?.split(';')[0] !== 'application/pdf' || !r.body) malformed();const reader = r.body.getReader(), chunks: Uint8Array<ArrayBuffer>[] = [];let length = 0;
    try { while (true) { const part = await reader.read();if (part.done) break;length += part.value.byteLength;if (length > 12 * 1024 * 1024) malformed();chunks.push(new Uint8Array(part.value)); } } finally { await reader.cancel(); }
    if (current !== generation) throw new CustomerApiError('SESSION_CHANGED');const result = new Blob(chunks, { type: 'application/pdf' });if (await result.slice(0, 5).text() !== '%PDF-') malformed();return result;
  }
  async function pilotWrite(path: string, body: unknown, etag: string, key: string) {
    if (!uuid.test(key) || !/^"v[1-9][0-9]*"$/.test(etag)) throw new CustomerApiError('VALIDATION_FAILED'); await ensureAccess(); const current = generation, challenge = await csrf();
    const result = await request('customer/' + path, 'POST', JSON.stringify(body), { Authorization: 'Bearer ' + access!.accessToken, 'X-CSRF-Token': challenge, 'If-Match': etag, 'Idempotency-Key': key }); if (current !== generation) throw new CustomerApiError('SESSION_CHANGED'); return result;
  }
  async function pilotRead(path: string, maximum = 65536) { await ensureAccess(); const current = generation; const r = await request('customer/' + path, 'GET', undefined, { Authorization: 'Bearer ' + access!.accessToken }, maximum); if (current !== generation) throw new CustomerApiError('SESSION_CHANGED'); return r; }
  function addressResponse(value: unknown, response: Response, id?: string): VersionedAddress {
    const address = personalAddress(value), etag = response.headers.get('ETag');
    if (etag !== '"v' + address.version + '"' || id !== undefined && address.id !== id) malformed();
    return { address, etag };
  }
  async function addressWrite(path: string, method: 'POST' | 'PUT', input: AddressInput | undefined, key: string, etag?: string, archive = false): Promise<VersionedAddress | undefined> {
    if (!uuid.test(key) || etag !== undefined && !/^"v[1-9][0-9]*"$/.test(etag)) throw new CustomerApiError('VALIDATION_FAILED');
    await ensureAccess(); const current = generation, challenge = await csrf();
    const { value, response } = await request(path, method, input === undefined ? undefined : JSON.stringify(input), { Authorization: 'Bearer ' + access!.accessToken, 'X-CSRF-Token': challenge, 'Idempotency-Key': key, ...(etag ? { 'If-Match': etag } : {}) });
    if (current !== generation) throw new CustomerApiError('SESSION_CHANGED');
    try {
      if (archive) { if (response.status !== 204) malformed(); return undefined; }
      if (response.status !== (method === 'POST' ? 201 : 200)) malformed();
      return addressResponse(value, response, method === 'PUT' ? path.split('/').at(-1) : undefined);
    } catch { throw unknown(); }
  }
  return {
    readAnalytics: (parameters: URLSearchParams) => ports.lock(async () => {
      const { value } = await pilotRead('personal-analytics?' + parameters.toString(), 1024 * 1024);
      try { return parseAnalytics(value); } catch { return malformed(); }
    }),
    getSnapshot: () => state,
    subscribe: (listener: () => void) => { listeners.add(listener); return () => { listeners.delete(listener); }; },
    initialize(): Promise<void> {
      initialization ??= ports.lock(async () => {
        try { await refreshLocked(); } catch (error) {
          if (error instanceof CustomerApiError && error.status === 401) {
            // Explicitly clear the known invalid/replayed family, without retrying refresh.
            try { await logoutLocked(); } catch { forget('SESSION_INVALID'); }
          }
        }
      }).catch(error => { forget(error instanceof CustomerApiError ? error.code : 'SESSION_INVALID'); }); return initialization;
    },
    async login(email: string, password: string): Promise<void> {
      await ports.lock(async () => {
        try {
          const challenge = await csrf(), { value } = await request('customer-auth/login', 'POST', JSON.stringify({ email, password }), { 'X-CSRF-Token': challenge });
          access = readAccess(value); generation++; pending = undefined; const user = await subject(); publish({ status: 'authenticated', subject: user, error: null }); channel?.postMessage('changed');
        } catch (error) { forget(error instanceof CustomerApiError ? error.code : 'SESSION_INVALID'); throw error; }
      });
    },
    logout: () => ports.lock(async () => { try { await logoutLocked(); } catch (error) { forget(error instanceof CustomerApiError ? error.code : 'OUTCOME_UNKNOWN'); throw error; } }),
    discardPending() { pending = undefined; },
    async enroll(input: Enrollment, key: string): Promise<void> { const id = await preauth('enrollments', input, key, false); beginOtp('verification', id!, input.email, ports.now() + 120000); },
    async resend(email: string, key: string): Promise<void> { const id = await preauth('verification-resend', { email }, key, false); beginOtp('verification', id!, email, ports.now() + 120000); },
    async recover(email: string, key: string): Promise<void> { const id = await preauth('recovery', { email }, key, false); beginOtp('recovery', id!, email, ports.now() + 120000); },
    async verifyRecovery(code: string, key: string): Promise<void> {
      const locator = getOtpChallenge('recovery');
      if (!locator || !/^\d{6}$/.test(code) || !uuid.test(key)) throw new CustomerApiError('VALIDATION_FAILED');
      await ports.lock(async () => {
        if (access) throw new CustomerApiError('CUSTOMER_ALREADY_SIGNED_IN');
        const body = JSON.stringify({ proofId: locator.proofId, proof: code });
        if (pending?.key === key && (pending.route !== 'recovery-verifications' || pending.body !== body)) throw new CustomerApiError('IDEMPOTENCY_CONFLICT');
        const challenge = pending?.key === key ? pending.challenge : await csrf(); pending = { key, route: 'recovery-verifications', body, challenge };
        const { value, response } = await request('customer-auth/recovery-verifications', 'POST', body, { 'X-CSRF-Token': challenge, 'Idempotency-Key': key });
        if (response.status !== 200 || !record(value) || !exact(value, ['proofId', 'proof', 'expiresAt']) || value.proofId !== locator.proofId || typeof value.proof !== 'string' || !/^[A-Za-z0-9_-]{43}$/.test(value.proof) || typeof value.expiresAt !== 'string' || !Number.isFinite(Date.parse(value.expiresAt)) || Date.parse(value.expiresAt) <= ports.now() || Date.parse(value.expiresAt) > ports.now() + 121000) throw unknown();
        acceptRecoveryGrant({ proofId: locator.proofId, proof: value.proof }); pending = undefined;
      });
    },
    async verify(key: string, code?: string): Promise<void> {
      const locator = getVerificationLocator();
      if (locator) {
        if (!code || !/^\d{6}$/.test(code)) throw new CustomerApiError('VALIDATION_FAILED');
        await preauth('verifications', { proofId: locator.proofId, proof: code }, key, true);
      } else await withEntryProof('verification', proof => preauth('verifications', proof, key, true));
      clearEntryProof();
    },
    async confirmRecovery(newPassword: string, key: string): Promise<void> { await withEntryProof('recovery', proof => preauth('recovery-confirm', { ...proof, newPassword }, key, true)); clearEntryProof(); },
    readProfile: () => ports.lock(async () => {
      await ensureAccess(); const current = generation, result = await request('customer/profile', 'GET', undefined, { Authorization: 'Bearer ' + access!.accessToken });
      if (current !== generation) throw new CustomerApiError('SESSION_CHANGED'); return profile(result.value, result.response);
    }),
    updateProfile: (input: ProfileInput, etag: string, key: string) => ports.lock(async () => {
      if (!/^"v[1-9][0-9]*"$/.test(etag) || !uuid.test(key)) throw new CustomerApiError('VALIDATION_FAILED');
      await ensureAccess(); const current = generation, challenge = await csrf();
      const result = await request('customer/profile', 'PUT', JSON.stringify(input), { Authorization: 'Bearer ' + access!.accessToken, 'X-CSRF-Token': challenge, 'If-Match': etag, 'Idempotency-Key': key });
      if (current !== generation) throw new CustomerApiError('SESSION_CHANGED'); try { return profile(result.value, result.response); } catch { throw unknown(); }
    }),
    profileReceipt: (key: string) => ports.lock(async () => {
      if (!uuid.test(key)) throw new CustomerApiError('VALIDATION_FAILED');
      await ensureAccess(); const current = generation;
      const { value } = await request('onboarding-receipts/updateCustomerProfile/' + key, 'GET', undefined, { Authorization: 'Bearer ' + access!.accessToken });
      if (current !== generation) throw new CustomerApiError('SESSION_CHANGED');
      if (!record(value) || !exact(value, ['operationId', 'targetId', 'outcome', 'version']) || value.operationId !== 'updateCustomerProfile' || typeof value.targetId !== 'string' || !uuid.test(value.targetId) || value.outcome !== 'COMMITTED' || !version(value.version)) malformed();
      return { targetId: value.targetId, version: value.version };
    }),
    listApplications: (page = 0, search = '', state = '') => ports.lock(async (): Promise<ApplicationPage> => {
      if (!Number.isSafeInteger(page) || page < 0 || search.length > 160) throw new CustomerApiError('VALIDATION_FAILED');
      await ensureAccess(); const current = generation, query = new URLSearchParams({ page: String(page), size: '20', ...(search ? { search } : {}), ...(state ? { state } : {}) });
      const { value } = await request('merchant-applications?' + query, 'GET', undefined, { Authorization: 'Bearer ' + access!.accessToken }, 4 * 1024 * 1024);
      if (current !== generation) throw new CustomerApiError('SESSION_CHANGED');
      if (!record(value) || !exact(value, ['items', 'page', 'size', 'hasNext']) || !Array.isArray(value.items) || value.items.length > 20 || value.page !== page || value.size !== 20 || typeof value.hasNext !== 'boolean') malformed();
      return { items: value.items.map(application), page, size: 20, hasNext: value.hasNext };
    }),
    readApplication: (id: string) => ports.lock(async (): Promise<VersionedApplication> => {
      if (!uuid.test(id)) throw new CustomerApiError('VALIDATION_FAILED'); await ensureAccess(); const current = generation;
      const result = await request('merchant-applications/' + id, 'GET', undefined, { Authorization: 'Bearer ' + access!.accessToken });
      if (current !== generation) throw new CustomerApiError('SESSION_CHANGED'); const value = application(result.value), etag = result.response.headers.get('ETag'); if (value.id !== id || etag !== '"v' + value.version + '"') malformed(); return { application: value, etag };
    }),
    createApplication: (input: ApplicationDraft, key: string) => ports.lock(() => applicationWrite('merchant-applications', 'POST', input, key)),
    updateApplication: (id: string, input: ApplicationDraft, etag: string, key: string) => ports.lock(() => {
      if (!uuid.test(id)) throw new CustomerApiError('VALIDATION_FAILED'); return applicationWrite('merchant-applications/' + id, 'PUT', input, key, etag);
    }),
    applicationReceipt: (operation: 'createApplicationDraft' | 'updateApplicationDraft', key: string) => ports.lock(async () => {
      if (!uuid.test(key) || !['createApplicationDraft', 'updateApplicationDraft'].includes(operation)) throw new CustomerApiError('VALIDATION_FAILED');
      await ensureAccess(); const current = generation;
      const { value } = await request('onboarding-receipts/' + operation + '/' + key, 'GET', undefined, { Authorization: 'Bearer ' + access!.accessToken });
      if (current !== generation) throw new CustomerApiError('SESSION_CHANGED');
      if (!record(value) || !exact(value, ['operationId', 'targetId', 'outcome', 'version']) || value.operationId !== operation || typeof value.targetId !== 'string' || !uuid.test(value.targetId) || value.outcome !== 'COMMITTED' || !version(value.version)) malformed(); return { targetId: value.targetId, version: value.version };
    }),
    readPilotCatalog: () => ports.lock(async (): Promise<PilotCatalog> => {
      const { value: v } = await pilotRead('shipping-catalog');
      if (!record(v) || !exact(v, ['simulation', 'serviceCode', 'quoteEnabled', 'bookingEnabled', 'coverage', 'tariff', 'paymentMethod', 'payer', 'paymentCollectionEnabled']) || v.simulation !== true || v.serviceCode !== 'ROAD_STANDARD' || v.quoteEnabled !== true || v.bookingEnabled !== true || v.paymentMethod !== 'CASH_ON_HANDOVER' || v.payer !== 'SENDER' || v.paymentCollectionEnabled !== false || !record(v.coverage) || !record(v.tariff)) malformed();
      const c = v.coverage, t = v.tariff; if (!exact(c, ['datasetVersion', 'namespace', 'provenance', 'effectiveFrom', 'areas', 'routes']) || !text(c.datasetVersion, 64) || !text(c.namespace, 64) || c.provenance !== 'SIMULATION' || !date(c.effectiveFrom) || !Array.isArray(c.areas) || c.areas.length > 100 || !c.areas.every(a => record(a) && exact(a, ['code', 'region', 'label', 'dropOffCode']) && Object.values(a).every(x => text(x, 160))) || !Array.isArray(c.routes) || c.routes.length > 100 || !c.routes.every(pilotRoute)) malformed();
      const fields = ['version', 'intraFirstVnd', 'intraStepVnd', 'interFirstVnd', 'interStepVnd', 'stepGrams', 'volumetricDivisor', 'maxParcels', 'maxActualGrams', 'maxDimensionMm', 'maxGirthMm', 'ttlSeconds', 'taxTreatment']; if (!exact(t, fields) || !text(t.version, 64) || t.taxTreatment !== 'SIMULATION_NOT_TAX_INVOICE' || !fields.filter(k => !['version', 'taxTreatment'].includes(k)).every(k => version(t[k]))) malformed(); return v as PilotCatalog;
    }),
    createPilotQuote: (draft: string, version: number, originCode: string, destinationCode: string, ordinaryGoodsConfirmed: boolean, key: string) => ports.lock(async () => {
      if (!uuid.test(draft) || !Number.isSafeInteger(version) || version < 1) throw new CustomerApiError('VALIDATION_FAILED'); const result = await pilotWrite('shipment-drafts/' + draft + '/quotes', { originCode, destinationCode, ordinaryGoodsConfirmed }, '"v' + version + '"', key);
      try { const value = quoteView(result.value); if (value.quote.draftId !== draft || value.quote.draftVersion !== version || value.quote.route.originCode !== originCode || value.quote.route.destinationCode !== destinationCode) malformed(); return value; } catch { throw unknown(); }
    }),
    readPilotQuote: (id: string) => ports.lock(async () => { if (!uuid.test(id)) throw new CustomerApiError('VALIDATION_FAILED'); const r = await pilotRead('shipping-quotes/' + id); const v = quoteView(r.value); if (v.quote.id !== id) malformed(); return v; }),
    pilotQuoteReceipt: (key: string) => ports.lock(async () => { if (!uuid.test(key)) throw new CustomerApiError('VALIDATION_FAILED'); const r = await pilotRead('quote-receipts/' + key); if (!record(r.value) || !exact(r.value, ['targetId', 'outcome']) || typeof r.value.targetId !== 'string' || !uuid.test(r.value.targetId) || r.value.outcome !== 'COMMITTED') malformed(); return { targetId: r.value.targetId }; }),
    confirmPilotBooking: (draft: string, version: number, quoteId: string, key: string) => ports.lock(async () => { if (!uuid.test(draft) || !uuid.test(quoteId) || !Number.isSafeInteger(version) || version < 1) throw new CustomerApiError('VALIDATION_FAILED'); const r = await pilotWrite('shipment-drafts/' + draft + '/bookings', { quoteId }, '"v' + version + '"', key); try { const v = shipmentView(r.value); if (v.shipment.draftId !== draft || v.shipment.draftVersion !== version || v.shipment.quote.id !== quoteId) malformed(); return v; } catch { throw unknown(); } }),
    pilotBookingReceipt: (key: string) => ports.lock(async () => { if (!uuid.test(key)) throw new CustomerApiError('VALIDATION_FAILED'); const r = await pilotRead('booking-receipts/' + key); if (!record(r.value) || !exact(r.value, ['targetId', 'outcome']) || typeof r.value.targetId !== 'string' || !uuid.test(r.value.targetId) || r.value.outcome !== 'COMMITTED') malformed(); return { targetId: r.value.targetId }; }),
    readPilotHandling: (id: string) => ports.lock(async () => { if (!uuid.test(id)) throw new CustomerApiError('VALIDATION_FAILED');const r = await pilotRead('shipments/' + id + '/handling');return handlingValue(r.value, id); }),
    downloadPilotLabel: (id: string, station?: string) => ports.lock(() => privateLabel(id, station)),
    listStations: () => ports.lock(async (): Promise<Station[]> => {const { value: v } = await stationRequest(undefined, '');if (!record(v) || !exact(v, ['stations', 'simulation']) || v.simulation !== true || !Array.isArray(v.stations) || !v.stations.length || v.stations.length > 3 || !v.stations.every(s => record(s) && exact(s, ['code', 'datasetVersion', 'assignmentVersion', 'permission', 'acceptanceEnabled']) && stationCode(s.code) && s.datasetVersion === 'ARGO-SIM-COVERAGE-1' && version(s.assignmentVersion) && s.permission === 'STATION_INTAKE' && s.acceptanceEnabled === true)) malformed();return v.stations as Station[]; }),
    lookupStationParcel: (station: string, barcode: string) => ports.lock(async () => {if (!/^AP-P-[a-f0-9]{32}$/.test(barcode)) throw new CustomerApiError('VALIDATION_FAILED');const r = await stationRequest(station, '/parcels/' + barcode);const v = stationView(r.value, station);if (!v.cargo.handling.parcels.some(p => p.barcode === barcode)) malformed();return v; }),
    readStationShipment: (station: string, shipment: string) => ports.lock(async () => {if (!uuid.test(shipment)) throw new CustomerApiError('VALIDATION_FAILED');const r = await stationRequest(station, '/shipments/' + shipment);return stationView(r.value, station, shipment); }),
    scanStationParcel: (station: string, barcode: string, key: string) => ports.lock(async () => {if (!/^AP-P-[a-f0-9]{32}$/.test(barcode)) throw new CustomerApiError('VALIDATION_FAILED');const r = await stationRequest(station, '/scans', 'POST', { barcode }, key);try {const scan = scanValue(r.value, station);if (scan.barcode !== barcode) malformed();return scan;} catch {throw unknown();} }),
    acceptStationShipment: (station: string, shipment: string, scanIds: string[], expectedVersion: number, key: string) => ports.lock(async () => {if (!uuid.test(shipment) || ![0, 1].includes(expectedVersion) || !scanIds.length || scanIds.length > 5 || scanIds.some(v => !uuid.test(v)) || new Set(scanIds).size !== scanIds.length) throw new CustomerApiError('VALIDATION_FAILED');const r = await stationRequest(station, '/shipments/' + shipment + '/acceptance', 'POST', { scanIds, inspection: 'MATCHED' }, key, '"i' + expectedVersion + '"');try {const v = stationView(r.value, station, shipment);if (v.cargo.handling.state !== 'AT_STATION' || r.response.headers.get('ETag') !== '"i1"') malformed();return v;} catch {throw unknown();} }),
    stationReceipt: (station: string, key: string) => ports.lock(async () => {if (!uuid.test(key)) throw new CustomerApiError('VALIDATION_FAILED');const { value: v } = await stationRequest(station, '/receipts/' + key);if (!record(v) || !exact(v, ['operation', 'targetId', 'shipmentId', 'outcome']) || !['SCAN', 'ACCEPT'].includes(String(v.operation)) || typeof v.targetId !== 'string' || !uuid.test(v.targetId) || typeof v.shipmentId !== 'string' || !uuid.test(v.shipmentId) || v.outcome !== 'COMMITTED') malformed();return v as { operation: 'SCAN' | 'ACCEPT'; targetId: string; shipmentId: string; outcome: 'COMMITTED' }; }),
    readPilotShipment: (id: string) => ports.lock(async () => { if (!uuid.test(id)) throw new CustomerApiError('VALIDATION_FAILED'); const r = await pilotRead('shipments/' + id); const v = shipmentView(r.value); if (v.shipment.id !== id || r.response.headers.get('ETag') !== '"v1"') malformed(); return v; }),
    listPilotShipments: (page = 0, search = '', status = '') => ports.lock(async (): Promise<PilotShipmentPage> => { if (!Number.isSafeInteger(page) || page < 0 || search.length > 160 || status && !['WAITING_FOR_HANDOVER', 'AT_STATION'].includes(status)) throw new CustomerApiError('VALIDATION_FAILED'); const query = new URLSearchParams({ page: String(page), size: '20', ...(search ? { search } : {}), ...(status ? { status } : {}) }); const { value: v } = await pilotRead('shipments?' + query, 4 * 1024 * 1024); if (!record(v) || !exact(v, ['items', 'page', 'size', 'hasNext']) || !Array.isArray(v.items) || v.items.length > 20 || v.page !== page || v.size !== 20 || typeof v.hasNext !== 'boolean') malformed(); return { items: v.items.map(shipmentView), page, size: 20, hasNext: v.hasNext }; }),
    listPersonalManagement: (parameters: URLSearchParams) => ports.lock(async (): Promise<ManagementPage> => {
      const keys = new Set(['page', 'size', 'search', 'recipient', 'from', 'until', 'status', 'service', 'origin', 'destination', 'obligation', 'sort']);
      for (const [key] of parameters) if (!keys.has(key) || parameters.getAll(key).length !== 1) throw new CustomerApiError('VALIDATION_FAILED');
      const query = new URLSearchParams(parameters); query.set('view', 'management');
      const { value: v } = await pilotRead('shipments?' + query, 4 * 1024 * 1024);
      const count = (n: unknown): n is string => typeof n === 'string' && /^(0|[1-9][0-9]{0,19})$/.test(n);
      if (!record(v) || !exact(v, ['schemaVersion', 'generatedAt', 'timeZone', 'summary', 'total', 'page', 'size', 'hasNext', 'items']) || v.schemaVersion !== 'PERSONAL_MANAGEMENT_1' || v.timeZone !== 'Asia/Ho_Chi_Minh' || !text(v.generatedAt, 40) || !Number.isFinite(Date.parse(v.generatedAt)) || !record(v.summary) || !exact(v.summary, ['totalShipments', 'waitingForHandover', 'atStation']) || !Object.values(v.summary).every(count) || !count(v.total) || !Number.isSafeInteger(v.page) || v.page !== Number(parameters.get('page') ?? 0) || v.size !== 20 || typeof v.hasNext !== 'boolean' || !Array.isArray(v.items) || v.items.length > 20) malformed();
      const summary = v.summary as ManagementPage['summary'];
      if (BigInt(summary.waitingForHandover) + BigInt(summary.atStation) !== BigInt(summary.totalShipments) || BigInt(v.total as string) > BigInt(summary.totalShipments)) malformed();
      const selectedStatus = parameters.get('status');
      if (selectedStatus === 'AT_STATION' && v.total !== summary.atStation || selectedStatus === 'WAITING_FOR_HANDOVER' && v.total !== summary.waitingForHandover) malformed();
      const items = (v.items as unknown[]).map(item => { if (!record(item) || !exact(item, ['record', 'status']) || !['WAITING_FOR_HANDOVER', 'AT_STATION'].includes(String(item.status))) malformed(); return { record: shipmentView(item.record), status: item.status as ManagementPage['items'][number]['status'] }; });
      if (new Set(items.map(i => i.record.shipment.id)).size !== items.length || BigInt(items.length) > BigInt(v.total as string) || v.hasNext && items.length !== 20 || items.some(i => parameters.has('status') && i.status !== parameters.get('status'))) malformed();
      const offset = BigInt(v.page as number) * 20n, total = BigInt(v.total as string);
      if (BigInt(items.length) !== (total <= offset ? 0n : total - offset > 20n ? 20n : total - offset) || v.hasNext !== (offset + BigInt(items.length) < total)) malformed();
      return { ...v, summary, items } as ManagementPage;
    }),
    listShippingDrafts: (page = 0, search = '') => ports.lock(async (): Promise<ShippingDraftPage> => {
      if (!Number.isSafeInteger(page) || page < 0 || search.length > 160) throw new CustomerApiError('VALIDATION_FAILED');
      await ensureAccess(); const current = generation, query = new URLSearchParams({ page: String(page), size: '20', ...(search ? { search } : {}) });
      const { value } = await request('customer/shipment-drafts?' + query, 'GET', undefined, { Authorization: 'Bearer ' + access!.accessToken }, 4 * 1024 * 1024);
      if (current !== generation) throw new CustomerApiError('SESSION_CHANGED');
      if (!record(value) || !exact(value, ['items', 'page', 'size', 'hasNext']) || !Array.isArray(value.items) || value.items.length > 20 || value.page !== page || value.size !== 20 || typeof value.hasNext !== 'boolean') malformed();
      return { items: value.items.map(shippingDraft), page, size: 20, hasNext: value.hasNext };
    }),
    readShippingDraft: (id: string) => ports.lock(async () => {
      if (!uuid.test(id)) throw new CustomerApiError('VALIDATION_FAILED'); await ensureAccess(); const current = generation;
      const { value, response } = await request('customer/shipment-drafts/' + id, 'GET', undefined, { Authorization: 'Bearer ' + access!.accessToken });
      if (current !== generation) throw new CustomerApiError('SESSION_CHANGED'); const d = shippingDraft(value); if (d.id !== id || response.headers.get('ETag') !== '"v' + d.version + '"') malformed(); return d;
    }),
    createShippingDraft: (input: ShippingDraftInput, key: string) => ports.lock(() => draftWrite('', 'POST', input, key)),
    updateShippingDraft: (id: string, input: ShippingDraftInput, etag: string, key: string) => ports.lock(() => { if (!uuid.test(id)) throw new CustomerApiError('VALIDATION_FAILED'); return draftWrite('/' + id, 'PUT', input, key, etag); }),
    archiveShippingDraft: (id: string, etag: string, key: string) => ports.lock(() => { if (!uuid.test(id)) throw new CustomerApiError('VALIDATION_FAILED'); return draftWrite('/' + id + '/archive', 'POST', undefined, key, etag, true); }),
    shippingDraftReceipt: (operation: DraftOperation, key: string) => ports.lock(async () => {
      if (!uuid.test(key) || !['createShippingDraft', 'updateShippingDraft', 'archiveShippingDraft'].includes(operation)) throw new CustomerApiError('VALIDATION_FAILED'); await ensureAccess(); const current = generation;
      const { value } = await request('customer/shipment-draft-receipts/' + operation + '/' + key, 'GET', undefined, { Authorization: 'Bearer ' + access!.accessToken });
      if (current !== generation) throw new CustomerApiError('SESSION_CHANGED'); if (!record(value) || !exact(value, ['operationId', 'targetId', 'outcome', 'version']) || value.operationId !== operation || typeof value.targetId !== 'string' || !uuid.test(value.targetId) || value.outcome !== 'COMMITTED' || !version(value.version)) malformed(); return { targetId: value.targetId, version: value.version };
    }),
    listAddresses: (page = 0, search = '', kind = '') => ports.lock(async (): Promise<AddressPage> => {
      if (!Number.isSafeInteger(page) || page < 0 || search.length > 160 || kind && !['SENDER', 'RECIPIENT'].includes(kind)) throw new CustomerApiError('VALIDATION_FAILED');
      await ensureAccess(); const current = generation, query = new URLSearchParams({ page: String(page), size: '20', ...(search ? { search } : {}), ...(kind ? { kind } : {}) });
      const { value } = await request('customer/addresses?' + query, 'GET', undefined, { Authorization: 'Bearer ' + access!.accessToken });
      if (current !== generation) throw new CustomerApiError('SESSION_CHANGED');
      if (!record(value) || !exact(value, ['items', 'page', 'size', 'hasNext']) || !Array.isArray(value.items) || value.items.length > 20 || value.page !== page || value.size !== 20 || typeof value.hasNext !== 'boolean') malformed();
      const items = value.items.map(personalAddress); if (new Set(items.map(item => item.id)).size !== items.length) malformed();
      return { items, page, size: 20, hasNext: value.hasNext };
    }),
    readAddress: (id: string) => ports.lock(async (): Promise<VersionedAddress> => {
      if (!uuid.test(id)) throw new CustomerApiError('VALIDATION_FAILED'); await ensureAccess(); const current = generation;
      const { value, response } = await request('customer/addresses/' + id, 'GET', undefined, { Authorization: 'Bearer ' + access!.accessToken });
      if (current !== generation) throw new CustomerApiError('SESSION_CHANGED'); return addressResponse(value, response, id);
    }),
    createAddress: (input: AddressInput, key: string) => ports.lock(async () => (await addressWrite('customer/addresses', 'POST', input, key))!),
    updateAddress: (id: string, input: AddressInput, etag: string, key: string) => ports.lock(async () => {
      if (!uuid.test(id)) throw new CustomerApiError('VALIDATION_FAILED'); return (await addressWrite('customer/addresses/' + id, 'PUT', input, key, etag))!;
    }),
    archiveAddress: (id: string, etag: string, key: string) => ports.lock(async () => {
      if (!uuid.test(id)) throw new CustomerApiError('VALIDATION_FAILED'); await addressWrite('customer/addresses/' + id + '/archive', 'POST', undefined, key, etag, true);
    }),
    addressReceipt: (operation: AddressOperation, key: string) => ports.lock(async () => {
      if (!uuid.test(key) || !['createPersonalAddress', 'updatePersonalAddress', 'archivePersonalAddress'].includes(operation)) throw new CustomerApiError('VALIDATION_FAILED');
      await ensureAccess(); const current = generation;
      const { value } = await request('customer/address-receipts/' + operation + '/' + key, 'GET', undefined, { Authorization: 'Bearer ' + access!.accessToken });
      if (current !== generation) throw new CustomerApiError('SESSION_CHANGED');
      if (!record(value) || !exact(value, ['operationId', 'targetId', 'outcome', 'version']) || value.operationId !== operation || typeof value.targetId !== 'string' || !uuid.test(value.targetId) || value.outcome !== 'COMMITTED' || !version(value.version)) malformed();
      return { targetId: value.targetId, version: value.version };
    }),
    dispose() { forget(); channel?.close(); listeners.clear(); },
  };
}
export const customerClient = createCustomerClient();
