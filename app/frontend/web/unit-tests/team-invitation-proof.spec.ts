import { test, expect } from '@playwright/test';
import { captureTeamInvitation, clearTeamInvitation, hasTeamInvitation, teamInvitationReturnPath, withTeamInvitation } from '../src/customer/team-invitation-proof';
const id = '10000000-0000-4000-8000-000000000901', token = 'A'.repeat(43), path = '/organization-invitations/' + id;
test.afterEach(() => clearTeamInvitation());
test('captures invitation possession only from bounded fragment and scrubs before request/render', () => {
  const replaced: unknown[][] = [];
  captureTeamInvitation({ href: 'https://portal.example.invalid' + path + '#proof=' + token }, { replaceState: (...args) => { replaced.push(args); } });
  expect(replaced).toEqual([[null, '', path]]); expect(hasTeamInvitation(id)).toBe(true); expect(teamInvitationReturnPath()).toBe(path);
  expect(withTeamInvitation(id, proof => proof === token)).toBe(true);
  expect(() => withTeamInvitation('10000000-0000-4000-8000-000000000902', () => true)).toThrow('TEAM_INVITATION_UNAVAILABLE');
  clearTeamInvitation(); expect(teamInvitationReturnPath()).toBeUndefined(); expect(hasTeamInvitation(id)).toBe(false);
});
test('rejects query credentials, repeated fields, extra claims, invalid locators and stale capture', () => {
  for (const suffix of ['?proof=' + token, '#proof=' + token + '&proof=' + token, '#proof=' + token + '&role=OWNER', '#proof=short', '#proof=' + 'A'.repeat(44)]) {
    captureTeamInvitation({ href: 'https://portal.example.invalid' + path + '#proof=' + token }, { replaceState: () => {} });
    captureTeamInvitation({ href: 'https://portal.example.invalid' + path + suffix }, { replaceState: () => {} }); expect(hasTeamInvitation(id)).toBe(false);
  }
  for (const bad of ['bad', '00000000-0000-0000-0000-000000000000']) {
    captureTeamInvitation({ href: 'https://portal.example.invalid/organization-invitations/' + bad + '#proof=' + token }, { replaceState: () => {} });
    expect(teamInvitationReturnPath()).toBeUndefined();
  }
});
