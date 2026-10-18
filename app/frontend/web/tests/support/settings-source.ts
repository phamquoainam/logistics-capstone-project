import { lstatSync, readFileSync, realpathSync } from 'node:fs';
import { resolve } from 'node:path';

type Settings = { merchantAPassword: string; merchantBPassword: string; adminPassword: string };
export type WireContext = { database: string; baseUrl: string; password: string; tokenSecret: string };

function record(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null && !Array.isArray(value);
}
function text(value: unknown): value is string {
  return typeof value === 'string' && value.length > 0 && value.length <= 4096;
}
function json(path: string): unknown {
  try {
    const absolute = resolve(path);
    const info = lstatSync(absolute);
    if (!info.isFile() || info.isSymbolicLink() || info.size > 1024 * 1024
      || realpathSync(absolute).toLowerCase() !== absolute.toLowerCase()) throw new Error();
    return JSON.parse(readFileSync(absolute, 'utf8').replace(/^\uFEFF/, ''));
  } catch {
    throw new Error('Invalid explicit web test settings; diagnostics suppressed');
  }
}

/** Test-only sources. Never discover/read .demo or the developer store. */
export function loadTestSettings(env: Record<string, string | undefined>): { settings: Settings; context: WireContext | null } {
  const wire = env.C04_CONTEXT;
  const fixture = env.ELOGISTICS_TEST_SETTINGS_FILE;
  if ((!wire && !fixture) || (wire && fixture)) {
    throw new Error('Select exactly one explicit web test source; no developer fallback');
  }
  const value = json((wire ?? fixture)!);
  if (wire) {
    if (!record(value) || !text(value.database) || !value.database.startsWith('elogistics_wire_')
      || !text(value.baseUrl) || !/^http:\/\/127\.0\.0\.1:[1-9]\d{0,4}\/api\/demo\/v1$/.test(value.baseUrl)
      || Number(value.baseUrl.split(':')[2]?.split('/')[0]) > 65535 || !text(value.password) || !text(value.tokenSecret)) {
      throw new Error('Invalid isolated C-04 test context');
    }
    const context = value as WireContext;
    return { context, settings: { merchantAPassword: context.password, merchantBPassword: context.password, adminPassword: context.password } };
  }
  if (!record(value) || value.purpose !== 'synthetic-web-test' || !record(value.settings)
    || !['merchantAPassword', 'merchantBPassword', 'adminPassword'].every(k => text(value.settings && (value.settings as Record<string, unknown>)[k]))) {
    throw new Error('Explicit synthetic web fixture required');
  }
  return { context: null, settings: value.settings as Settings };
}
