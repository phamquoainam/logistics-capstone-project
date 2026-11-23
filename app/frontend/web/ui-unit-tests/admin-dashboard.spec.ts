import { test, expect } from '@playwright/test';
import type { BrowserContext } from '@playwright/test';
import type { Dashboard, MetricKey, Query } from '../src/admin/data';
import { adminContext, adminPermissions, adminReport, adminRows, adminUser } from './support/admin-data';

const origin = 'https://admin.example.invalid';
test.afterEach(async ({ page }) => { if (!page.isClosed()) await page.waitForLoadState('networkidle'); });
async function fixture(context: BrowserContext, limited = false, expired = false) {
  const calls: string[] = [], ctx = adminContext(limited ? ['ADMIN_DASHBOARD_READ', 'ADMIN_SHIPMENT_READ'] : adminPermissions);
  let report: Dashboard;
  await context.route(origin + '/**', async route => {
    const url = new URL(route.request().url());
    if (!url.pathname.startsWith('/api/')) return route.fulfill({ response: await context.request.get('http://127.0.0.1:5181' + url.pathname) });
    calls.push(url.pathname + url.search);
    const reply = (value: unknown, status = 200) => route.fulfill({ status, contentType: 'application/json', body: JSON.stringify(value) });
    if (url.pathname.endsWith('/csrf')) return reply({ csrfToken: 'synthetic-admin-csrf-challenge' });
    if (url.pathname.endsWith('/refresh')) return reply({ accessToken: 'a'.repeat(128), tokenType: 'Bearer', expiresIn: 900, sessionExpiresAt: new Date(Date.now() + 28800000).toISOString(), user: adminUser });
    if (url.pathname.endsWith('/me')) return reply(adminUser);
    if (url.pathname.endsWith('/context')) return reply(ctx);
    if (url.pathname === '/api/v1/admin/dashboard') {
      const p = url.searchParams, q: Query = { from: p.get('from')!, to: p.get('to')!, grain: p.get('grain') as Query['grain'], shipmentStatus: p.get('shipmentStatus') as Query['shipmentStatus'], regionId: null };
      report = adminReport(q, ctx); return reply(report);
    }
    if (url.pathname.endsWith('/shipments')) return expired ? reply({ code: 'SNAPSHOT_EXPIRED' }, 409) : reply(adminRows(report, url.searchParams.get('metric') as MetricKey));
    if (url.pathname.endsWith('/platform')) return reply({ checkedAt: new Date().toISOString(), apiAvailability: 'UP', queues: { availability: 'SOURCE_NOT_IMPLEMENTED', metrics: [] }, scheduledJobs: { availability: 'SOURCE_NOT_IMPLEMENTED', metrics: [] }, integrationFailures: { availability: 'SOURCE_NOT_IMPLEMENTED', metrics: [] } });
    return reply({ code: 'ACCESS_DENIED' }, 403);
  });
  return calls;
}
test('KPI drill-through and back retain cohort without redundant dashboard requests', async ({ page, context }) => {
  const calls = await fixture(context); await page.goto(origin + '/admin');
  await expect(page.getByRole('button', { name: 'Vận đơn đã đặt: 3 vận đơn' })).toBeVisible();
  await page.getByRole('button', { name: 'Chờ bàn giao: 2 vận đơn' }).click();
  await expect(page.getByRole('table')).toBeVisible();
  const request = calls.find(v => v.startsWith('/api/v1/admin/dashboard/shipments'))!;
  expect(request).toContain('snapshotId='); expect(request).toContain('metric=WAITING_HANDOVER'); expect(request).toContain('grain=DAY');
  await page.getByRole('button', { name: 'Về tổng quan' }).click();
  await expect(page.getByRole('img', { name: 'Vận đơn đã đặt theo thời gian Booking' })).toBeVisible();
  expect(calls.filter(v => v.startsWith('/api/v1/admin/dashboard?'))).toHaveLength(1);
});
test('filters apply once and protected financial and technical widgets are absent for limited roles', async ({ page, context }) => {
  const calls = await fixture(context, true); await page.goto(origin + '/admin');
  await expect(page.getByRole('button', { name: 'Vận đơn đã đặt: 3 vận đơn' })).toBeVisible();
  await expect(page.getByText('Cước chưa thanh toán', { exact: true })).toHaveCount(0);
  await expect(page.getByText('Tình trạng API quản trị', { exact: true })).toHaveCount(0);
  expect(calls.some(v => v.includes('/platform'))).toBe(false);
  await page.getByRole('button', { name: '7 ngày', exact: true }).click();
  await expect(page.getByRole('button', { name: 'Vận đơn đã đặt: 3 vận đơn' })).toBeVisible();
  expect(calls.filter(v => v.startsWith('/api/v1/admin/dashboard?'))).toHaveLength(2);
});
test('expired selector blocks rows and requires explicit refresh instead of silently changing cohort', async ({ page, context }) => {
  const calls = await fixture(context, false, true); await page.goto(origin + '/admin');
  await page.getByRole('button', { name: 'Vận đơn đã đặt: 3 vận đơn' }).click();
  await expect(page.getByRole('alert')).toContainText('Số liệu đã thay đổi');
  await expect(page.getByRole('table')).toHaveCount(0);
  expect(calls.filter(v => v.startsWith('/api/v1/admin/dashboard/shipments'))).toHaveLength(1);
  expect(calls.filter(v => v.startsWith('/api/v1/admin/dashboard?'))).toHaveLength(1);
});
test('responsive Dashboard shows truthful source availability without unsupported KPI counters', async ({ page, context }) => {
  await fixture(context); await page.goto(origin + '/admin');
  await expect(page.getByRole('button', { name: 'Vận đơn đã đặt: 3 vận đơn' })).toBeVisible();
  await page.getByText('Nguồn dữ liệu & phạm vi thống kê', { exact: true }).click();
  await expect(page.getByText('Nguồn nghiệp vụ chưa được triển khai').first()).toBeVisible();
  await expect(page.getByText('Giao thành công', { exact: true })).toHaveCount(0);
  await page.screenshot({ path: '../../../.demo/spec003-implementation/admin-dashboard-desktop.png', fullPage: true, animations: 'disabled' });
  await page.setViewportSize({ width: 390, height: 844 });
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(true);
  await page.screenshot({ path: '../../../.demo/spec003-implementation/admin-dashboard-mobile.png', fullPage: true, animations: 'disabled' });
});
