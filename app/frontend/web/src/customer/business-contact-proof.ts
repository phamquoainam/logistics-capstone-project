type Proof = { applicationId: string; proofId: string; proof: string };
let pending: Proof | undefined;
const uuid = /^[a-f0-9]{8}(?:-[a-f0-9]{4}){3}-[a-f0-9]{12}$/;
const validId = (value: string | null): value is string => !!value && uuid.test(value) && value !== '00000000-0000-0000-0000-000000000000';
/** Capture before entry-proof removes fragments. Never put the secret in router state, DOM or storage. */
export function captureBusinessContactProof(location: Pick<Location, 'pathname' | 'hash' | 'search'>, history: Pick<History, 'replaceState'>) {
  if (location.pathname !== '/personal/profile') return;
  pending = undefined;
  const fragment = location.hash, query = location.search;
  if (fragment || query) history.replaceState(null, '', '/personal/profile');
  if (query || fragment.length > 240) return;
  const fields = new URLSearchParams(fragment.replace(/^#/, ''));
  const app = fields.get('applicationId'), id = fields.get('proofId'), proof = fields.get('proof');
  if ([...fields.keys()].length !== 3 || fields.getAll('applicationId').length !== 1 || fields.getAll('proofId').length !== 1 || fields.getAll('proof').length !== 1 || !validId(app) || !validId(id) || !proof || !/^[A-Za-z0-9_-]{43}$/.test(proof)) return;
  pending = { applicationId: app, proofId: id, proof };
}
export function hasBusinessContactProof() { return pending !== undefined; }
export function withBusinessContactProof<T>(work: (value: Readonly<Proof>) => T): T { if (!pending) throw new Error('BUSINESS_CONTACT_PROOF_UNAVAILABLE'); return work(pending); }
export function clearBusinessContactProof() { pending = undefined; }
