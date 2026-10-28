import { X509Certificate, createHash } from 'node:crypto';
import { readFile } from 'node:fs/promises';
import { connect } from 'node:tls';

export default async function preflight() {
  const url = new URL(process.env.S003_BROWSER_ORIGIN ?? '');
  const pem = await readFile(process.env.S003_BROWSER_CERT ?? ''), certificate = new X509Certificate(pem);
  const pin = createHash('sha256').update(certificate.publicKey.export({ type: 'spki', format: 'der' })).digest('base64');
  if (pin !== process.env.S003_BROWSER_SPKI || !certificate.checkIP('127.0.0.1') || !certificate.checkHost('localhost') || Date.parse(certificate.validTo) <= Date.now()) throw new Error('OWNED_CERTIFICATE_BINDING_REJECTED');
  async function handshake(wrongHost: boolean) {
    return new Promise<void>((done, reject) => {
      const socket = connect({ host: '127.0.0.1', port: Number(url.port), ca: pem, minVersion: 'TLSv1.2', rejectUnauthorized: true, ...(wrongHost ? { servername: 'not-approved.example.invalid' } : {}) });
      socket.setTimeout(3000, () => { socket.destroy(); reject(new Error('OWNED_TLS_PREFLIGHT_TIMEOUT')); });
      socket.once('secureConnect', () => { const valid = socket.authorized && !wrongHost; socket.destroy(); if (valid) done(); else reject(new Error('OWNED_TLS_PREFLIGHT_REJECTED')); });
      socket.once('error', error => { socket.destroy(); if (wrongHost && 'code' in error && error.code === 'ERR_TLS_CERT_ALTNAME_INVALID') done(); else reject(new Error('OWNED_TLS_PREFLIGHT_REJECTED')); });
    });
  }
  await handshake(false); await handshake(true);
}
