import https from 'node:https';

const url = process.argv[2];
if (!url || !['https://127.0.0.1:5173/', 'https://127.0.0.1:8443/api/v1/public-media/hero-network.svg'].includes(url)) process.exit(2);
const request = https.get(url, { timeout: 3000 }, response => {
  response.resume();
  process.exit(response.statusCode === 200 ? 0 : 1);
});
request.on('timeout', () => request.destroy());
request.on('error', () => process.exit(1));
