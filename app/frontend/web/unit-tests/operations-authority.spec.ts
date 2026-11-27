import { test, expect } from '@playwright/test';
import { createStaffClient } from '../src/customer/staff-client';
import { captureOperationsInvitation, clearOperationsInvitation, hasOperationsInvitation } from '../src/customer/operations-invitation-proof';
import { hasStaffInvitation } from '../src/customer/staff-invitation-proof';

const account = '10000000-0000-4000-8000-000000000001';
test.afterEach(clearOperationsInvitation);
test('Operations proof is memory-only and cannot populate SUPPORT proof context', () => {
  const history: unknown[][] = [];
  captureOperationsInvitation({ pathname: '/ops/activate', search: '', hash: '#proofId=' + account + '&token=' + 'a'.repeat(43) }, { replaceState: (...v: unknown[]) => { history.push(v); } });
  expect(history).toEqual([[null, '', '/ops/activate']]); expect(hasOperationsInvitation()).toBe(true); expect(hasStaffInvitation()).toBe(false);
  captureOperationsInvitation({ pathname: '/ops/activate', search: '?token=forged', hash: '#proofId=' + account + '&token=' + 'a'.repeat(43) }, { replaceState: () => {} }); expect(hasOperationsInvitation()).toBe(false);
});
test('OPS session requires OPERATIONS purpose and never sends SUPPORT or Customer routes', async () => {
  const calls: string[] = [], now = Date.parse('2026-11-25T00:00:00Z'); let purpose = 'OPERATIONS';
  const transport: typeof fetch = async (path) => { const url = String(path); calls.push(url);
    if (url.endsWith('/csrf')) return Response.json({ csrfToken: 'synthetic-operations-challenge-123456' });
    if (url.endsWith('/refresh') || url.endsWith('/login')) return Response.json({ accessToken: 'o'.repeat(128), expiresAt: new Date(now + 900000).toISOString(), accountId: account });
    if (url.endsWith('/me')) return Response.json({ accountId: account, displayName: 'Synthetic Ops', principalType: 'STAFF', purpose, environment: 'TEST' });
    return new Response(null, { status: 204 });
  };
  const client = createStaffClient({ fetch: transport, lock: work => work(), now: () => now }, 'OPERATIONS');
  await client.initialize(); expect(client.getSnapshot().subject?.purpose).toBe('OPERATIONS'); await client.logout(); purpose = 'SUPPORT'; await client.initialize(); expect(client.getSnapshot().status).toBe('guest');
  expect(calls.every(p => p.startsWith('/api/v1/ops-auth/'))).toBe(true);
});
