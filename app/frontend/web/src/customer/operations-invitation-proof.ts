type Proof = { proofId: string; token: string };
let proof: Proof | undefined;
/** Capture before rendering; neither router state nor persistent browser storage receives the proof. */
export function captureOperationsInvitation(location: Pick<Location, 'pathname' | 'hash' | 'search'>, history: Pick<History, 'replaceState'>) {
  if (location.pathname !== '/ops/activate') return;
  const fragment = location.hash;
  history.replaceState(null, '', '/ops/activate');
  if (location.search) { proof = undefined; return; }
  const params = new URLSearchParams(fragment.replace(/^#/, ''));
  const id = params.get('proofId'), token = params.get('token');
  proof = [...params.keys()].length === 2 && params.getAll('proofId').length === 1 && params.getAll('token').length === 1 && id && /^[a-f0-9]{8}(?:-[a-f0-9]{4}){3}-[a-f0-9]{12}$/.test(id) && id !== '00000000-0000-0000-0000-000000000000' && token && /^[A-Za-z0-9_-]{43}$/.test(token) ? { proofId: id, token } : undefined;
}
export function hasOperationsInvitation() { return proof !== undefined; }
export function withOperationsInvitation<T>(work: (proofId: string, token: string) => T): T { if (!proof) throw new Error('OPS_INVITATION_UNAVAILABLE'); return work(proof.proofId, proof.token); }
export function clearOperationsInvitation() { proof = undefined; }
