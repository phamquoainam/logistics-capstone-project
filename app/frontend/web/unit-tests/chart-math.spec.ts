import { test, expect } from '@playwright/test';
import { axisLabel, chartAxis, chartGeometry, chartPercent } from '../src/customer/chart-math';

test('count axes have integer ticks and headroom without placing a valid point beyond the plot', () => {
  for (const values of [['0'], ['1'], ['9', '10', '25'], ['999', '1001']]) {
    const axis = chartAxis(values), points = chartGeometry(values, axis.maximum).points;
    expect(axis.ticks.at(-1)).toBe(0n);
    expect(axis.maximum >= BigInt(values.at(-1)!)).toBe(true);
    for (const point of points) { expect(point.y).toBeGreaterThanOrEqual(0); expect(point.y).toBeLessThanOrEqual(180); }
  }
});

test('single bucket and empty series never fabricate extra observations or a filled trend', () => {
  expect(chartGeometry([], 4n)).toEqual({ points: [], line: '', area: '' });
  expect(chartGeometry(['1'], 4n)).toEqual({ points: [{ x: 500, y: 135 }], line: 'M500,135', area: '' });
});

test('huge VND geometry remains finite and ordered without coercing money to a floating point number', () => {
  const values = ['0', '18446744073709551614', '9999999999999999999999999999999999999999'];
  const axis = chartAxis(values), points = chartGeometry(values, axis.maximum).points;
  expect(points.every(p => Number.isFinite(p.x) && Number.isFinite(p.y))).toBe(true);
  expect(points[0].y).toBeGreaterThanOrEqual(points[1].y);
  expect(points[1].y).toBeGreaterThan(points[2].y);
  expect(axis.maximum >= BigInt(values[2])).toBe(true);
  expect(axisLabel(axis.maximum)).toContain('×10^');
});

test('axis abbreviations identify their unit and percentages retain zero/full shares', () => {
  expect(axisLabel(0n)).toBe('0'); expect(axisLabel(25000n)).toBe('25 k');
  expect(axisLabel(1500000n)).toBe('1,5 tr'); expect(axisLabel(2000000000n)).toBe('2 tỷ');
  expect(chartPercent('0', 0n)).toBe(0); expect(chartPercent('12', 12n)).toBe(100);
});
