import { test, expect } from '@playwright/test';
import { mkdtempSync, writeFileSync, unlinkSync, rmdirSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { join } from 'node:path';
import { loadTestSettings } from '../tests/support/settings-source';

function fixture(value: unknown, run: (path: string) => void) {
  const directory = mkdtempSync(join(tmpdir(), 'elogistics-web-source-'));
  const path = join(directory, 'synthetic.json');
  try { writeFileSync(path, JSON.stringify(value)); run(path); }
  finally { unlinkSync(path); rmdirSync(directory); }
}
test('no source and ambiguous sources fail without developer discovery', () => {
  expect(() => loadTestSettings({})).toThrow('no developer fallback');
  expect(() => loadTestSettings({ C04_CONTEXT: 'not-read', ELOGISTICS_TEST_SETTINGS_FILE: 'not-read' })).toThrow('exactly one');
});
test('existing isolated C04 context preserves password mapping', () => {
  fixture({ database: 'elogistics_wire_synthetic', baseUrl: 'http://127.0.0.1:18080/api/demo/v1',
    password: 'synthetic-test-only', tokenSecret: 'synthetic-signing-only' }, path => {
    const result = loadTestSettings({ C04_CONTEXT: path });
    expect(result.settings).toEqual({ merchantAPassword: 'synthetic-test-only', merchantBPassword: 'synthetic-test-only', adminPassword: 'synthetic-test-only' });
    expect(result.context?.database).toBe('elogistics_wire_synthetic');
  });
});
test('explicit synthetic fixture accepted; raw developer settings rejected', () => {
  const settings = { merchantAPassword: 'synthetic-a', merchantBPassword: 'synthetic-b', adminPassword: 'synthetic-admin' };
  fixture({ purpose: 'synthetic-web-test', settings }, path => {
    expect(loadTestSettings({ ELOGISTICS_TEST_SETTINGS_FILE: path })).toEqual({ settings, context: null });
  });
  fixture(settings, path => expect(() => loadTestSettings({ ELOGISTICS_TEST_SETTINGS_FILE: path })).toThrow('synthetic web fixture'));
});
test('invalid isolation/shape and file errors are sanitized', () => {
  fixture({ database: 'elogistics_dev', password: 'synthetic-sensitive-marker' }, path => {
    expect(() => loadTestSettings({ C04_CONTEXT: path })).toThrow('Invalid isolated');
    expect(() => loadTestSettings({ C04_CONTEXT: path })).not.toThrow('synthetic-sensitive-marker');
  });
  expect(() => loadTestSettings({ C04_CONTEXT: 'synthetic-missing-path' })).toThrow('diagnostics suppressed');
});
