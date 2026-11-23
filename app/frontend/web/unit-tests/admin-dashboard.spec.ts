import { test, expect } from '@playwright/test';
import { parseContext, parseDashboard, parseShipments } from '../src/admin/data';
import type { Query } from '../src/admin/data';
import { adminAccount, adminContext, adminReport, adminRows } from '../ui-unit-tests/support/admin-data';

const query: Query = { from: '2026-11-01', to: '2026-11-21', grain: 'DAY', shipmentStatus: null, regionId: null };
test('Admin projection preserves exact values and never turns unavailable sources into zero KPIs', () => {
  const report = adminReport(query); expect(parseDashboard(report, adminContext(), query)).toEqual(report);
  expect(report.sections.find(s => s.key === 'MERCHANT')?.metrics).toEqual([]);
  const finance = report.sections.find(s => s.key === 'FINANCE')!;
  for (const m of finance.metrics) { m.value = '9007199254740993'; finance.series.filter(p => p.metric === m.key).at(-1)!.value = m.value; }
  expect(parseDashboard(report, adminContext(), query).sections.find(s => s.key === 'FINANCE')?.metrics[0].value).toBe('9007199254740993');
});
test('foreign context unknown permissions and Production cannot enter Admin state', () => {
  for (const v of [{ ...adminContext(), accountId: '20000000-0000-4000-8000-000000000001' }, { ...adminContext(), environment: 'PRODUCTION' }, { ...adminContext(), permissions: ['ADMIN_DASHBOARD_READ', 'ADMIN_EVERYTHING'] }, { ...adminContext(), recipient: 'private' }]) expect(() => parseContext(v, adminAccount)).toThrow('RESPONSE_INVALID');
});
test('unauthorized financial sections are rejected rather than merely hidden', () => {
  const limited = adminContext(['ADMIN_DASHBOARD_READ', 'ADMIN_SHIPMENT_READ']);
  expect(() => parseDashboard(adminReport(query), limited, query)).toThrow('RESPONSE_INVALID');
  expect(parseDashboard(adminReport(query, limited), limited, query).sections.some(s => s.key === 'FINANCE')).toBe(false);
});
test('inconsistent bucket totals duplicate metrics and foreign query fail closed', () => {
  const mismatch = adminReport(query); mismatch.sections[0].series[0].value = '1';
  const duplicate = adminReport(query); duplicate.sections[0].metrics[1] = duplicate.sections[0].metrics[0];
  const wrongQuery = adminReport({ ...query, grain: 'MONTH' });
  for (const v of [mismatch, duplicate, wrongQuery]) expect(() => parseDashboard(v, adminContext(), query)).toThrow('RESPONSE_INVALID');
});
test('drill-through rejects PII fields wrong snapshot and wrong state before display', () => {
  const report = adminReport(query), valid = adminRows(report, 'ACCEPTED');
  expect(parseShipments(valid, report, 'ACCEPTED', 0, 20)).toEqual(valid);
  for (const v of [{ ...valid, snapshotId: 'other' }, { ...valid, items: [{ ...valid.items[0], recipientPhone: 'private' }] }, { ...valid, items: [{ ...valid.items[0], status: 'BOOKED' }] }, { ...valid, items: [valid.items[0], valid.items[0]] }]) expect(() => parseShipments(v, report, 'ACCEPTED', 0, 20)).toThrow('RESPONSE_INVALID');
});
test('empty available cohort is a truthful zero with complete date buckets', () => {
  const report = parseDashboard(adminReport(query, adminContext(), true), adminContext(), query);
  expect(report.sections.filter(s => s.availability === 'AVAILABLE').flatMap(s => s.metrics).every(m => m.value === '0')).toBe(true);
  expect(report.sections[0].series.filter(p => p.metric === 'BOOKINGS')).toHaveLength(21);
});
