type Purpose = 'verification' | 'recovery';
type Proof = { proofId: string; proof: string };
let captured: { purpose: Purpose; value: Proof } | undefined;
const uuid = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/;
/** Synchronous before render/network; no proof in React, DOM, storage or query strings. */
export function captureEntryProof(location: Pick<Location, 'href'>, history: Pick<History, 'replaceState'>): void {
  captured = undefined;
  const url = new URL(location.href), fragment = url.hash;
  if (fragment || url.searchParams.has('proof') || url.searchParams.has('proofId')) history.replaceState(null, '', url.pathname);
  const purpose = url.pathname === '/verify-email' ? 'verification' : url.pathname === '/recover/confirm' ? 'recovery' : undefined;
  if (!purpose || url.searchParams.has('proof') || url.searchParams.has('proofId') || fragment.length > 160) return;
  const fields = new URLSearchParams(fragment.slice(1));
  if ([...fields].length !== 2 || fields.getAll('proofId').length !== 1 || fields.getAll('proof').length !== 1) return;
  const proofId = fields.get('proofId') ?? '', proof = fields.get('proof') ?? '';
  if (uuid.test(proofId) && /^[A-Za-z0-9_-]{43}$/.test(proof)) captured = { purpose, value: { proofId, proof } };
}
export function hasEntryProof(purpose: Purpose): boolean { return captured?.purpose === purpose; }
export function withEntryProof<T>(purpose: Purpose, work: (value: Proof) => T): T {
  if (!captured || captured.purpose !== purpose) throw new Error('ENTRY_PROOF_UNAVAILABLE');
  return work({ ...captured.value });
}
export function clearEntryProof(): void { captured = undefined; }
