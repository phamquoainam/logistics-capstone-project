import { resolve } from 'node:path';
import { createHmac } from 'node:crypto';
import { loadTestSettings } from './settings-source';

const source = loadTestSettings(process.env);
const context = source.context;
export const settings = source.settings;
export const evidencePath = (file: string) => resolve(process.env.C04_RESULT_DIR ?? '../../../.demo/evidence', file);

export function syntheticExpiredToken() {
  if (!context) throw new Error('New C-04 runtime tests require the isolated web harness');
  const now = Math.floor(Date.now() / 1000);
  const header = Buffer.from(JSON.stringify({ alg: 'HS256' })).toString('base64url');
  const payload = Buffer.from(JSON.stringify({ sub: '00000000-0000-0000-0000-000000000001', iss: 'elogistics-demo', iat: now - 600, exp: now - 120 })).toString('base64url');
  return `${header}.${payload}.${createHmac('sha256', context.tokenSecret).update(`${header}.${payload}`).digest('base64url')}`;
}
