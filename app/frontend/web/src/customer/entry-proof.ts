type Purpose = 'verification' | 'recovery';
type Proof = { proofId: string; proof: string };
type Locator = { proofId: string; expiresAt: number };
type OtpChallenge = Locator & { purpose: Purpose; email: string };
let captured: { purpose: Purpose; value: Proof } | undefined;
let verificationLocator: Locator | undefined;
let otpChallenge: OtpChallenge | undefined;
const uuid = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/;
/** Synchronous before render/network; no proof in React, DOM, storage or query strings. */
export function captureEntryProof(location: Pick<Location, 'href'>, history: Pick<History, 'replaceState'>): void {
  captured = undefined;
  verificationLocator = undefined;
  otpChallenge = undefined;
  const url = new URL(location.href), fragment = url.hash;
  if (fragment || url.searchParams.has('proof') || url.searchParams.has('proofId')) history.replaceState(null, '', url.pathname);
  const purpose = url.pathname === '/verify-email' ? 'verification' : url.pathname === '/recover/confirm' ? 'recovery' : undefined;
  if (!purpose || url.searchParams.has('proof') || url.searchParams.has('proofId') || fragment.length > 160) return;
  const fields = new URLSearchParams(fragment.slice(1));
  if (purpose === 'verification' && [...fields].length === 2 && fields.getAll('proofId').length === 1 && fields.getAll('expiresAt').length === 1) {
    const proofId = fields.get('proofId') ?? '', expiry = fields.get('expiresAt') ?? '';
    const expiresAt = Date.parse(expiry);
    if (uuid.test(proofId) && /^\d{4}-\d\d-\d\dT\d\d:\d\d:\d\d(?:\.\d+)?Z$/.test(expiry) && Number.isFinite(expiresAt)) verificationLocator = { proofId, expiresAt };
    return;
  }
  if ([...fields].length !== 2 || fields.getAll('proofId').length !== 1 || fields.getAll('proof').length !== 1) return;
  const proofId = fields.get('proofId') ?? '', proof = fields.get('proof') ?? '';
  if (uuid.test(proofId) && /^[A-Za-z0-9_-]{43}$/.test(proof)) captured = { purpose, value: { proofId, proof } };
}
export function hasEntryProof(purpose: Purpose): boolean { return captured?.purpose === purpose; }
export function getVerificationLocator(): Locator | undefined { return verificationLocator && { ...verificationLocator }; }
export function beginOtp(purpose: Purpose, proofId: string, email: string, expiresAt: number): void {
  captured = undefined;
  otpChallenge = { purpose, proofId, email, expiresAt };
  verificationLocator = purpose === 'verification' ? { proofId, expiresAt } : undefined;
}
export function getOtpChallenge(purpose: Purpose): OtpChallenge | undefined { return otpChallenge?.purpose === purpose ? { ...otpChallenge } : undefined; }
export function acceptRecoveryGrant(value: Proof): void { captured = { purpose: 'recovery', value: { ...value } }; }
export function withEntryProof<T>(purpose: Purpose, work: (value: Proof) => T): T {
  if (!captured || captured.purpose !== purpose) throw new Error('ENTRY_PROOF_UNAVAILABLE');
  return work({ ...captured.value });
}
export function clearEntryProof(): void { captured = undefined; verificationLocator = undefined; otpChallenge = undefined; }
