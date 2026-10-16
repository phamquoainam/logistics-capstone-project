import { readFileSync } from 'node:fs';
import { resolve } from 'node:path';
import { createHmac } from 'node:crypto';

const contextPath = process.env.C04_CONTEXT;
const context = contextPath ? JSON.parse(readFileSync(contextPath, 'utf8')) : null;
if (context && (!context.database?.startsWith('elogistics_wire_') || !/^http:\/\/127\.0\.0\.1:\d+\/api\/demo\/v1$/.test(context.baseUrl))) {
  throw new Error('Invalid isolated C-04 test context');
}
export const settings = context ? { merchantAPassword: context.password, merchantBPassword: context.password, adminPassword: context.password }
  : JSON.parse(readFileSync(resolve(process.cwd(), '../../../.demo/settings.json'), 'utf8').replace(/^\uFEFF/, ''));
export const evidencePath = (file: string) => resolve(process.env.C04_RESULT_DIR ?? '../../../.demo/evidence', file);

export function syntheticExpiredToken() {
  if (!context) throw new Error('New C-04 runtime tests require the isolated web harness');
  const now = Math.floor(Date.now() / 1000);
  const header = Buffer.from(JSON.stringify({ alg: 'HS256' })).toString('base64url');
  const payload = Buffer.from(JSON.stringify({ sub: '00000000-0000-0000-0000-000000000001', iss: 'elogistics-demo', iat: now - 600, exp: now - 120 })).toString('base64url');
  return `${header}.${payload}.${createHmac('sha256', context.tokenSecret).update(`${header}.${payload}`).digest('base64url')}`;
}
