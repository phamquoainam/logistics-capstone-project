/** Stop only this suite's loopback static server using the ephemeral unlogged ownership nonce. */
export default async function teardown() {
  const nonce = process.env.ELOGISTICS_UI_UNIT_SERVER_NONCE;
  if (!nonce) throw new Error('OWNED_UI_UNIT_SERVER_NONCE_REQUIRED');
  const response = await fetch('http://127.0.0.1:5181/__isolated-ui-unit-stop', { method: 'POST', headers: { 'X-Owned-UI-Unit-Nonce': nonce }, signal: AbortSignal.timeout(3000) });
  if (response.status !== 204) throw new Error('OWNED_UI_UNIT_SERVER_CLEANUP_FAILED');
}
