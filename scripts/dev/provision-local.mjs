import { randomBytes, randomUUID, generateKeyPairSync } from 'node:crypto';
import { existsSync, writeFileSync } from 'node:fs';
import { join } from 'node:path';

const directory = process.argv[2];
if (!directory || existsSync(join(directory, 'runtime.json'))) throw new Error('LOCAL_RUNTIME_ALREADY_EXISTS_OR_DIRECTORY_MISSING');
const key = () => randomBytes(32).toString('base64');
function jwk(kid) {
  const { privateKey } = generateKeyPairSync('rsa', { modulusLength: 3072 });
  return JSON.stringify({ keys: [{ ...privateKey.export({ format: 'jwk' }), kid, use: 'sig', alg: 'RS256' }] });
}
const certificatePassword = randomBytes(32).toString('base64url');
const env = {
  CUSTOMER_ROLLOUT: 'SANDBOX', CUSTOMER_BIND_ADDRESS: '127.0.0.1', CUSTOMER_HTTPS_PORT: '8443',
  CUSTOMER_SECURITY_ORIGIN: 'https://127.0.0.1:5173', IDENTITY_SECURITY_ORIGIN: 'https://127.0.0.1:5173',
  IDENTITY_SECURITY_ISSUER: 'https://127.0.0.1:8443/system', IDENTITY_SECURITY_AUDIENCE: 'elogistics-system-local',
  IDENTITY_SECURITY_CURRENT_KID: 'system-local-1', IDENTITY_SECURITY_JWK_SET: jwk('system-local-1'),
  IDENTITY_SECURITY_LIMITER_HMAC: key(),
  CUSTOMER_SECURITY_ISSUER: 'https://127.0.0.1:8443/customer', CUSTOMER_SECURITY_AUDIENCE: 'elogistics-customer-local',
  CUSTOMER_SECURITY_CURRENT_KID: 'customer-local-1', CUSTOMER_SECURITY_JWK_SET: jwk('customer-local-1'),
  CUSTOMER_SECURITY_LIMITER_HMAC: key(),
  CUSTOMER_SECURITY_PROOF_VERIFIER_KEYS: JSON.stringify({ 'proof-local-1': key() }),
  CUSTOMER_SECURITY_CURRENT_PROOF_KEY: 'proof-local-1',
  CUSTOMER_SECURITY_DELIVERY_ENCRYPTION_KEYS: JSON.stringify({ 'delivery-local-1': key() }),
  CUSTOMER_SECURITY_CURRENT_ENCRYPTION_KEY: 'delivery-local-1',
  MERCHANT_CUSTOMER_PROFILE_RECEIPT_KEYS: JSON.stringify({ 'profile-local-1': key() }),
  MERCHANT_CUSTOMER_PROFILE_CURRENT_RECEIPT_KEY: 'profile-local-1',
  IDENTITY_SYSTEM_REALM_ID: randomUUID(),
  CUSTOMER_HTTPS_KEY_STORE: join(directory, 'localhost.p12'),
  CUSTOMER_HTTPS_KEY_STORE_PASSWORD: certificatePassword,
  CUSTOMER_SMTP_MODE: 'LOCAL_REAL_SMTP', CUSTOMER_SMTP_HOST: 'smtp.gmail.com', CUSTOMER_SMTP_PORT: '587',
  PUBLIC_MEDIA_MODE: 's3', PUBLIC_MEDIA_S3_REGION: 'ap-southeast-2',
  PUBLIC_MEDIA_S3_BUCKET: 'elogistics-dev-media-160905', AWS_PROFILE: 'elogistics-dev',
};
writeFileSync(join(directory, 'runtime.json'), JSON.stringify(env), { flag: 'wx', mode: 0o600 });
