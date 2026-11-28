/** Opt-in synthetic account through real Compose HTTPS + loopback SMTP; no mocks or SQL. */
import { createRequire } from 'node:module';
import { readFileSync } from 'node:fs';
import { createHash, randomBytes, X509Certificate } from 'node:crypto';

const require = createRequire(new URL('../../app/frontend/web/package.json', import.meta.url));
const { chromium, expect } = require('@playwright/test');
const origin = new URL(process.env.ARGOPOST_SMOKE_ORIGIN ?? 'https://localhost:5443');
const inbox = new URL(process.env.ARGOPOST_SMOKE_MAIL ?? 'http://127.0.0.1:8025');
if (origin.protocol !== 'https:' || origin.hostname !== 'localhost' || origin.pathname !== '/' || origin.username || origin.password || origin.search || origin.hash || inbox.protocol !== 'http:' || inbox.hostname !== '127.0.0.1' || inbox.pathname !== '/' || inbox.username || inbox.password || inbox.search || inbox.hash) throw Error('EXACT_LOCAL_TARGET_REQUIRED');
const certificate = new X509Certificate(readFileSync(process.env.ARGOPOST_SMOKE_CERT ?? '.local/argopost-web.crt'));
if (!certificate.checkHost('localhost')) throw Error('LOCAL_CERTIFICATE_HOST_REQUIRED');
const spki = createHash('sha256').update(certificate.publicKey.export({ type: 'spki', format: 'der' })).digest('base64');
let browser, stage = 'launch'; const denials=[];
try {
  browser = await chromium.launch({ ...(process.env.ARGOPOST_BROWSER_CHANNEL ? {channel:process.env.ARGOPOST_BROWSER_CHANNEL} : {}), args: ['--ignore-certificate-errors-spki-list=' + spki] });
  const context = await browser.newContext({ baseURL: origin.origin, ignoreHTTPSErrors: false });
  const page = await context.newPage(); page.setDefaultTimeout(20000);
  page.on('response', response => {
    if (response.status()<400 || !new URL(response.url()).pathname.startsWith('/api/')) return;
    const operation=new URL(response.url()).pathname.replace(/[a-f0-9]{8}(?:-[a-f0-9]{4}){3}-[a-f0-9]{12}/g,':id');
    const safe={operation,status:response.status()};denials.push(safe);
    void response.json().then(value => {if(typeof value?.code==='string' && /^[A-Z0-9_]+$/.test(value.code))safe.code=value.code;}).catch(()=>{});
  });
  const email = 'compose-' + randomBytes(8).toString('hex') + '@example.invalid', password = randomBytes(24).toString('base64url');
  stage = 'registration'; await page.goto('/register');
  await page.locator('#register-name').fill('Synthetic Compose Sender'); await page.locator('#register-email').fill(email);
  await page.locator('#register-phone').fill('0901234567'); await page.locator('#register-password').fill(password); await page.locator('#register-confirmation').fill(password);
  await page.getByRole('checkbox').check(); await page.getByRole('button', {name:'Đăng ký ngay',exact:true}).click();
  await expect(page).toHaveURL(origin.origin + '/verify-email');
  stage = 'actual_smtp'; let code;
  await expect.poll(async () => {
    const response = await fetch(new URL('/api/v1/messages', inbox), {signal:AbortSignal.timeout(3000)});
    if (!response.ok) return false;
    const data = await response.json();
    const found = data.messages?.find(message => message.To?.some(recipient => recipient.Address === email));
    if (!found) return false;
    const message = await (await fetch(new URL('/api/v1/message/' + found.ID, inbox), {signal:AbortSignal.timeout(3000)})).json();
    code = message.Text?.match(/(?:^|\n)([0-9]{6})(?:\r?\n|$)/)?.[1]; return !!code;
  }, {timeout:30000, message:'Synthetic SMTP delivery required'}).toBe(true);
  stage = 'verification'; await page.locator('#verification-code').fill(code); code = undefined;
  await page.getByRole('button',{name:'Xác minh tài khoản',exact:true}).click(); await expect(page).toHaveURL(origin.origin + '/login');
  stage = 'login'; await page.locator('#login-email').fill(email); await page.locator('#login-password').fill(password);
  const loginResponse = page.waitForResponse(r => new URL(r.url()).pathname === '/api/v1/customer-auth/login' && r.request().method() === 'POST');
  await page.getByRole('button',{name:'Đăng nhập',exact:true}).click(); const response = await loginResponse; expect(response.status()).toBe(200);
  const access = (await response.json()).accessToken; expect(typeof access).toBe('string');
  await expect(page.getByRole('heading',{name:'Tổng quan',exact:true})).toBeVisible();
  stage = 'cookies_and_isolation'; const cookies = await context.cookies();
  expect(cookies.some(c => c.name === '__Host-elogistics.customer.refresh' && c.httpOnly && c.secure && c.sameSite === 'Strict' && c.path === '/')).toBe(true);
  expect(cookies.some(c => c.name === '__Host-elogistics.refresh')).toBe(false);
  const denial = await page.evaluate(async token => ({
    system: (await fetch('/api/v1/auth/me',{headers:{Authorization:'Bearer '+token}})).status,
    csrf: (await fetch('/api/v1/customer-auth/logout',{method:'POST'})).status,
  }), access);
  expect(denial.system).toBe(401); expect(denial.csrf).toBe(403);
  expect(await page.evaluate(() => [localStorage.length,sessionStorage.length])).toEqual([0,0]);
  stage = 'business_contact_request'; await page.goto('/personal/applications');
  await page.getByRole('button',{name:'Tạo bản nháp Merchant',exact:true}).click();
  await expect(page.locator('#application-representativeEmail')).toBeVisible();
  await page.locator('#application-representativeEmail').fill(email);
  await page.getByRole('button',{name:'Lưu bản nháp',exact:true}).click();
  await expect(page.getByRole('button',{name:'Gửi email xác minh liên hệ',exact:true})).toBeEnabled();
  await page.getByRole('button',{name:'Gửi email xác minh liên hệ',exact:true}).click();
  let contactLink;
  await expect.poll(async () => {
    const data = await (await fetch(new URL('/api/v1/messages',inbox))).json();
    for (const found of data.messages?.filter(message => message.To?.some(recipient => recipient.Address === email)) ?? []) {
      const message = await (await fetch(new URL('/api/v1/message/'+found.ID,inbox))).json();
      const candidate = message.Text?.match(/https:\/\/localhost:[0-9]+\/personal\/profile#[^\s<>]+/)?.[0];
      if (candidate && new URL(candidate).origin === origin.origin) {contactLink=candidate;return true;}
    }
    return false;
  }, {timeout:30000,message:'Actual bound Business Contact email required'}).toBe(true);
  stage = 'business_contact_open'; await page.goto(contactLink);contactLink=undefined;
  await expect(page).toHaveURL(origin.origin+'/personal/profile');
  stage = 'business_contact_confirm';
  await page.getByRole('button',{name:'Xác nhận email liên hệ',exact:true}).click();
  stage = 'business_contact_return';
  await expect(page).toHaveURL(/\/personal\/applications\/[a-f0-9-]+$/);
  // A verified contact is still a draft, never a submitted/approved Merchant.
  await expect(page.getByText('Trạng thái:',{exact:false})).toContainText('Bản nháp');
  await page.goto('/personal');
  stage = 'responsive'; await page.setViewportSize({width:390,height:844});
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth + 1)).toBe(true);
  await page.setViewportSize({width:1280,height:900});
  stage = 'logout'; await page.getByRole('button',{name:'Đăng xuất',exact:true}).click(); await expect(page.locator('#login-email')).toBeVisible();
  console.log(JSON.stringify({result:'PASS',target:origin.origin,registration:true,smtpOtp:true,verification:true,login:true,secureCookie:true,csrfDenial:true,systemIsolation:true,businessContactVerification:true,merchantStillDraft:true,responsive:true,logout:true,syntheticAccountsCreated:1,noPrivilegedProvisioning:true}));
} catch {
  console.error(JSON.stringify({result:'FAIL',stage,denials,noSecretsPublished:true}));process.exitCode=1;
} finally { await browser?.close(); }
