import { test, expect } from '@playwright/test';
import { monthDates, parsePeriodInput, periodPresets, shiftDate } from '../src/customer/AnalyticsPeriodPicker';

test('preset periods use inclusive visible days and an exclusive API end without future dates', () => {
  const presets = periodPresets('2026-11-09');
  expect(presets.find(p => p.label === 'Hôm nay')).toMatchObject({ from: '2026-11-09', until: '2026-11-10' });
  expect(presets.find(p => p.label === 'Hôm qua')).toMatchObject({ from: '2026-11-08', until: '2026-11-09' });
  for (const days of [7, 14, 30, 90]) {
    const preset = presets.find(p => p.label === `${days} ngày`)!;
    expect((Date.parse(preset.until) - Date.parse(preset.from)) / 86400000).toBe(days);
    expect(preset.until).toBe('2026-11-10');
  }
});

test('month presets handle current partial month, completed month and year boundaries', () => {
  expect(periodPresets('2026-11-09').find(p => p.label === 'Tháng này')).toMatchObject({ from: '2026-11-01', until: '2026-11-10' });
  expect(periodPresets('2026-01-02').find(p => p.label === 'Tháng trước')).toMatchObject({ from: '2025-12-01', until: '2026-01-01' });
});

test('calendar cells preserve leap days and Monday offsets without rollover dates', () => {
  expect(monthDates('2024-02-01').days).toHaveLength(29);
  expect(monthDates('2026-02-01').days).toHaveLength(28);
  expect(monthDates('2026-11-01').offset).toBe(6);
  expect(shiftDate('2026-12-31', 1)).toBe('2027-01-01');
});

test('typed dates reject impossible calendar input before changing the query', () => {
  expect(parsePeriodInput('31/02/2026')).toBe(''); expect(parsePeriodInput('29/02/2026')).toBe('');
  expect(parsePeriodInput('29/02/2024')).toBe('2024-02-29'); expect(parsePeriodInput('not-a-date')).toBe('');
});
