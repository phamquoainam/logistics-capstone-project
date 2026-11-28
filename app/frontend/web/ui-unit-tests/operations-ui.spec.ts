import { test, expect } from '@playwright/test';

/** Presentation fixtures only; real authorization/intake evidence is the HTTPS runtime test. */
test('verified OPS context with no current site shows an actionable empty state and no intake controls', async ({ page, context }) => {
  let reads = 0;
  const account = '10000000-0000-4000-8000-000000000001', origin = 'https://operations.example.invalid';
  await context.route(origin + '/**', async route => {
    const path = new URL(route.request().url()).pathname;
    if (!path.startsWith('/api/')) return route.fulfill({ response: await context.request.get('http://127.0.0.1:5181' + path) });
    const value = path.endsWith('/csrf') ? { csrfToken: 'synthetic-ops-csrf-token-123456' } : path.endsWith('/refresh') ? { accessToken: 'o'.repeat(128), expiresAt: new Date(Date.now() + 900000).toISOString(), accountId: account } : path.endsWith('/me') ? { accountId: account, displayName: 'Synthetic Ops', principalType: 'STAFF', purpose: 'OPERATIONS', environment: 'TEST' } : path === '/api/v1/ops/sites' ? (reads++, { sites: [] }) : { code: 'ISOLATED_UI_ROUTE_UNSUPPORTED' };
    return route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(value) });
  });
  await page.goto(origin + '/ops/station'); await expect(page.getByRole('status')).toContainText('Chưa có Station được phân công');
  await expect(page.locator('#intake-barcode')).toHaveCount(0); await expect(page.locator('.iw-footer')).toContainText('OPS context');
  await page.getByRole('button', { name: 'Kiểm tra lại phân công', exact: true }).click(); await expect.poll(() => reads).toBe(2);
  await page.locator('.iw-portals>summary').click(); await expect(page.getByRole('button', { name: /^Administration Console/ })).toBeDisabled(); await expect(page.getByRole('button', { name: /^Carrier Business & Finance/ })).toBeDisabled();
});
