let captured: { id: string; proof: string } | undefined;
const uuid = /^[0-9a-f]{8}(?:-[0-9a-f]{4}){3}-[0-9a-f]{12}$/;
/** Capture before React/network; the bearer proof never enters storage or DOM. */
export function captureTeamInvitation(location: Pick<Location, 'href'>, history: Pick<History, 'replaceState'>) {
  captured = undefined;
  const url = new URL(location.href), match = /^\/organization-invitations\/([^/]+)$/.exec(url.pathname);
  if (!match) return;
  if (url.hash || url.search) history.replaceState(null, '', url.pathname);
  if (!uuid.test(match[1]) || match[1] === '00000000-0000-0000-0000-000000000000' || url.search || url.hash.length > 60) return;
  const fields = new URLSearchParams(url.hash.slice(1));
  const proof = fields.get('proof');
  if ([...fields].length === 1 && proof && /^[A-Za-z0-9_-]{43}$/.test(proof)) captured = { id: match[1], proof };
}
export function hasTeamInvitation(id: string) { return captured?.id === id; }
export function teamInvitationReturnPath() { return captured ? '/organization-invitations/' + captured.id : undefined; }
export function withTeamInvitation<T>(id: string, work: (proof: string) => T): T {
  if (!captured || captured.id !== id) throw new Error('TEAM_INVITATION_UNAVAILABLE');
  return work(captured.proof);
}
export function clearTeamInvitation() { captured = undefined; }
