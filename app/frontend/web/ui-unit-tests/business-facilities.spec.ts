import { test, expect } from '@playwright/test';
import type { BrowserContext } from '@playwright/test';
import { organizationA, organizationB } from './support/business-fixture';
import { operationsFixture, reportingContext, reportSite } from './support/business-report-fixture';

/** Synthetic UI protocol tests. No database, account provisioning or Pickup simulation. */
async function fixture(context: BrowserContext, options: { financial?: boolean; unboundOnly?: boolean } = {}) {
  const queries: URL[] = [], state = { denied: false };
  await context.route('**/api/v1/**', async route => {
    const url = new URL(route.request().url()), path = url.pathname;
    const reply = (body: unknown, status = 200) => route.fulfill({ status, contentType: 'application/json', headers: { 'Cache-Control': 'no-store' }, body: JSON.stringify(body) });
    if (path.endsWith('/csrf')) return reply({ challenge: 'synthetic-csrf' });
    if (path.endsWith('/refresh')) return reply({ accountId: organizationA, accessToken: 'synthetic-access', expiresAt: new Date(Date.now() + 900000).toISOString() });
    if (path.endsWith('/me')) return reply({ accountId: organizationA, displayName: 'Synthetic Customer', verifiedEmail: 'synthetic@example.invalid', securityVersion: 1 });
    if (path.endsWith('/business-contexts')) return reply({ schemaVersion: 'BUSINESS_CONTEXTS_V1', items: [reportingContext, { ...reportingContext, organizationId: organizationB, membershipId: organizationB, displayName: 'Synthetic Store B' }] });
    if (path.endsWith('/operations-reports')) {
      queries.push(url);
      if (state.denied) return reply({ code: 'RESOURCE_NOT_FOUND' }, 404);
      const data = operationsFixture(url.searchParams, options.financial ?? true, path.includes(organizationB) ? organizationB : organizationA);
      if (!options.unboundOnly) data.breakdowns.facility = [{ ...data.breakdowns.facility[0], key: reportSite, label: reportSite }];
      return reply(data);
    }
    return reply({ code: 'RESOURCE_NOT_FOUND' }, 404);
  });
  return { queries, state };
}
const base = '/merchant/' + organizationA;

test('site activity is a root menu item and drill-through preserves the exact reporting cohort', async ({ page, context }) => {
  const api = await fixture(context);
  await page.goto(base + '/facilities?days=7');
  await expect(page.getByRole('heading', { name: 'Kho & Điểm lấy hàng', exact: true })).toBeVisible();
  const menu = page.getByRole('navigation', { name: 'Điều hướng Business', exact: true });
  const link = menu.getByRole('link', { name: 'Kho & Điểm lấy hàng', exact: true });
  await expect(link).toHaveAttribute('aria-current', 'page');
  expect(await link.evaluate(node => node.closest('.pw-management-items') === null)).toBe(true);
  await expect(page.locator('.wf-kpis > article')).toContainText(['Địa điểm có Booking trong kỳ1', 'Vận đơn đã gắn địa điểm2', 'Vận đơn chưa gắn địa điểm0']);
  const source = api.queries.at(-1)!;
  const href = await page.getByRole('link', { name: /^Xem vận đơn tại/ }).getAttribute('href');
  const q = new URL(href!, 'https://local.invalid').searchParams;
  expect(q.get('facilityId')).toBe(reportSite); expect(q.get('from')).toBe(source.searchParams.get('from')); expect(q.get('until')).toBe(source.searchParams.get('until'));
  expect(q.has('interval')).toBe(false);
  await page.getByRole('button', { name: '90 ngày', exact: true }).click();
  await expect(page.getByRole('button', { name: '90 ngày', exact: true })).toHaveAttribute('aria-pressed', 'true');
  await expect.poll(() => api.queries.at(-1)?.searchParams.get('from')).not.toBe(source.searchParams.get('from'));
  expect(api.queries.every(r => !r.pathname.includes('/customer/'))).toBe(true);
});

test('unbound shipments are not invented sites and missing financial permission never becomes zero money', async ({ page, context }) => {
  await fixture(context, { financial: false, unboundOnly: true }); await page.goto(base + '/facilities?days=30');
  await expect(page.locator('.wf-kpis')).toContainText('Địa điểm có Booking trong kỳ0');
  await expect(page.locator('.wf-unbound')).toContainText('2 vận đơn chưa gắn địa điểm');
  await expect(page.getByRole('columnheader', { name: 'Cước xác nhận' })).toHaveCount(0);
  await expect(page.getByRole('link', { name: /^Xem vận đơn tại/ })).toHaveCount(0);
  await expect(page.getByText('Điều này không xác định doanh nghiệp đã có hay chưa có kho.')).toBeVisible();
  await expect(page.getByRole('link', { name: 'Xem nhóm chưa gắn kho trong báo cáo' })).toHaveAttribute('href', /panel=dimensions/);
  await expect(page.getByRole('button', { name: /Pickup|Tiếp nhận|Đã bàn giao|Tạo kho/ })).toHaveCount(0);
});

test('invalid filters do not query reports and a revoked read removes old data', async ({ page, context }) => {
  const api = await fixture(context); await page.goto(base + '/facilities?days=7&days=90');
  await expect(page.getByRole('alert')).toContainText('Bộ lọc không hợp lệ'); expect(api.queries).toHaveLength(0);
  await page.getByRole('link', { name: 'Đặt lại bộ lọc' }).click(); await expect(page.locator('.wf-kpis')).toBeVisible();
  api.state.denied = true; await page.getByRole('button', { name: 'Làm mới', exact: true }).click();
  await expect(page.getByRole('alert')).toContainText('Chưa có quyền đọc báo cáo');
  await expect(page.locator('.wf-kpis')).toHaveCount(0); await expect(page.locator('.wf-card tbody tr')).toHaveCount(0);
});

test('organization switching clears scope and the site table stays inside mobile viewport', async ({ page, context }, info) => {
  const api = await fixture(context); await page.goto(base + '/facilities?days=30'); await expect(page.locator('.wf-kpis')).toBeVisible();
  await page.goto('/merchant/' + organizationB + '/facilities?days=30'); await expect(page.locator('.wf-heading')).toContainText('Synthetic Store B'); await expect(page.locator('.wf-kpis')).toBeVisible();
  expect(api.queries.at(-1)?.pathname).toContain(organizationB);
  await page.screenshot({ path: info.outputPath('facilities-desktop-synthetic.png'), fullPage: true, animations: 'disabled' });
  await page.setViewportSize({ width: 390, height: 844 }); await expect(page.locator('.wf-card')).toBeVisible();
  await page.screenshot({ path: info.outputPath('facilities-mobile-synthetic.png'), fullPage: true, animations: 'disabled' });
  const geometry = await page.evaluate(() => ({ width: document.documentElement.scrollWidth, viewport: innerWidth, outside: [...document.querySelectorAll('.wf-page, .wf-page > *, .pw-content, #main-content')].map(e => ({ name: e.className || e.id, width: e.getBoundingClientRect().width, right: e.getBoundingClientRect().right })) }));
  expect(geometry.width, JSON.stringify(geometry)).toBeLessThanOrEqual(geometry.viewport + 1);
});
