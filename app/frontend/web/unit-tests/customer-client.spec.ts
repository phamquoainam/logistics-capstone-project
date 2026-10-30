import { test, expect } from '@playwright/test';
import { createCustomerClient } from '../src/customer/client';
import { captureEntryProof, getVerificationLocator, hasEntryProof, withEntryProof, clearEntryProof } from '../src/customer/entry-proof';

const account='10000000-0000-4000-8000-000000000001', other='10000000-0000-4000-8000-000000000002', key='10000000-0000-4000-8000-000000000010';
const subject={accountId:account,displayName:'Synthetic Customer',verifiedEmail:'synthetic@example.invalid',securityVersion:1};
const initialTime=Date.parse('2026-10-28T00:00:00Z');
const registration={email:'synthetic@example.invalid',password:'synthetic-enrollment-password',displayName:'Synthetic Customer',consentVersion:'S003-2026-10-27'};
type Call={path:string;init:RequestInit};
function fixture(handler?:(call:Call)=>Promise<Response>|Response){
  const calls:Call[]=[];let now=initialTime;let tail:Promise<unknown>=Promise.resolve();
  const transport:typeof fetch=async(input,init)=>{
    const call={path:String(input),init:init??{}};calls.push(call);if(handler)return handler(call);
    if(call.path.endsWith('/csrf'))return Response.json({challenge:'synthetic-live-challenge'});
    if(call.path.endsWith('/me'))return Response.json(subject);
    if(call.path.endsWith('/login')||call.path.endsWith('/refresh'))return Response.json({accessToken:'synthetic-customer-access',expiresAt:new Date(now+900000).toISOString(),accountId:account});
    if(call.path.endsWith('/logout'))return new Response(null,{status:204});
    return Response.json({id:account,accountId:account,displayName:'Business display',preferredLanguage:'vi',version:2},{headers:{ETag:'"v2"'}});
  };
  // Isolated transport tests only; browser Web Locks and actual backend are separate runtime gates.
  const lock=<T>(work:()=>Promise<T>)=>{const result=tail.then(work);tail=result.then(()=>{},()=>{});return result;};
  const client=createCustomerClient({fetch:transport,lock,now:()=>now});
  return {client,calls,advance:(millis:number)=>{now+=millis;}};
}
test.afterEach(()=>clearEntryProof());

test('concurrent initialization has one strict refresh and never exposes access in session snapshot',async()=>{
  const {client,calls}=fixture();await Promise.all([client.initialize(),client.initialize(),client.initialize()]);
  expect(calls.filter(c=>c.path.endsWith('/refresh'))).toHaveLength(1);expect(client.getSnapshot()).toEqual({status:'authenticated',subject,error:null});expect(JSON.stringify(client.getSnapshot())).not.toContain('synthetic-customer-access');client.dispose();
});
test('lost refresh response stays unknown and cannot be blindly retried by another mount',async()=>{
  const {client,calls}=fixture(call=>{if(call.path.endsWith('/csrf'))return Response.json({challenge:'challenge'});throw new Error('synthetic private transport detail');});
  await client.initialize();await client.initialize();expect(client.getSnapshot()).toEqual({status:'guest',subject:null,error:'OUTCOME_UNKNOWN'});expect(calls.filter(c=>c.path.endsWith('/refresh'))).toHaveLength(1);await expect(client.readProfile()).rejects.toMatchObject({code:'SESSION_INVALID'});expect(calls).toHaveLength(2);client.dispose();
});
test('known replay explicitly logs out cookie binding and never retries refresh',async()=>{
  const {client,calls}=fixture(call=>call.path.endsWith('/csrf')?Response.json({challenge:'challenge'}):call.path.endsWith('/refresh')?Response.json({code:'REFRESH_REPLAY'},{status:401}):new Response(null,{status:204}));
  await client.initialize();expect(client.getSnapshot().status).toBe('guest');expect(calls.map(c=>c.path.split('/').pop())).toEqual(['csrf','refresh','csrf','logout']);client.dispose();
});
test('login preserves exact input and sends credentials only in same-origin protected body',async()=>{
  const {client,calls}=fixture();await client.login('Exact.Case@example.invalid','  synthetic exact password  ');
  const call=calls.find(c=>c.path.endsWith('/login'))!;expect(JSON.parse(String(call.init.body))).toEqual({email:'Exact.Case@example.invalid',password:'  synthetic exact password  '});expect(call.init).toMatchObject({method:'POST',credentials:'same-origin',mode:'same-origin',cache:'no-store',redirect:'error',referrerPolicy:'no-referrer'});expect(call.init.headers).toMatchObject({'X-CSRF-Token':'synthetic-live-challenge'});expect(calls.every(c=>!c.path.includes('password')&&!c.path.includes('Exact.Case'))).toBe(true);client.dispose();
});
test('unknown enrollment retries only on explicit call with original key body and live challenge',async()=>{
  let posts=0;const {client,calls}=fixture(call=>{if(call.path.endsWith('/csrf'))return Response.json({challenge:'original-challenge'});if(++posts===1)throw new Error('synthetic lost response');return Response.json({accepted:true,requestId:key},{status:202});});
  await expect(client.enroll(registration,key)).rejects.toMatchObject({code:'OUTCOME_UNKNOWN'});await expect(client.enroll({...registration,displayName:'Different'},key)).rejects.toMatchObject({code:'IDEMPOTENCY_CONFLICT'});expect(calls).toHaveLength(2);await client.enroll(registration,key);expect(calls).toHaveLength(3);expect(calls[1].init.body).toEqual(calls[2].init.body);expect(calls[2].init.headers).toMatchObject({'Idempotency-Key':key,'X-CSRF-Token':'original-challenge'});client.dispose();
});
test('expired access refresh is serialized across simultaneous protected reads',async()=>{
  const {client,calls,advance}=fixture();await client.login(registration.email,registration.password);advance(880000);await Promise.all([client.readProfile(),client.readProfile()]);expect(calls.filter(c=>c.path.endsWith('/refresh'))).toHaveLength(1);expect(calls.filter(c=>c.path.endsWith('/profile'))).toHaveLength(2);client.dispose();
});
test('profile rejects foreign account and mismatched ETag before presenting data',async()=>{
  for(const variant of ['foreign','etag']){
    const {client}=fixture(call=>call.path.endsWith('/csrf')?Response.json({challenge:'challenge'}):call.path.endsWith('/login')?Response.json({accessToken:'synthetic',accountId:account,expiresAt:new Date(initialTime+900000).toISOString()}):call.path.endsWith('/me')?Response.json(subject):Response.json({id:account,accountId:variant==='foreign'?other:account,displayName:'Private',preferredLanguage:'vi',version:2},{headers:{ETag:variant==='etag'?'"v3"':'"v2"'}}));
    await client.login(registration.email,registration.password);await expect(client.readProfile()).rejects.toMatchObject({code:'RESPONSE_INVALID'});client.dispose();
  }
});
test('profile unknown outcome uses scoped receipt GET without another mutation',async()=>{
  const {client,calls}=fixture(call=>{
    if(call.path.endsWith('/csrf'))return Response.json({challenge:'challenge'});
    if(call.path.endsWith('/login'))return Response.json({accessToken:'synthetic',accountId:account,expiresAt:new Date(initialTime+900000).toISOString()});
    if(call.path.endsWith('/me'))return Response.json(subject);
    if(call.init.method==='PUT')throw new Error('synthetic lost response');
    return Response.json({operationId:'updateCustomerProfile',targetId:account,outcome:'COMMITTED',version:2});
  });
  await client.login(registration.email,registration.password);await expect(client.updateProfile({displayName:'Business',preferredLanguage:'vi'},'"v1"',key)).rejects.toMatchObject({code:'OUTCOME_UNKNOWN'});expect(await client.profileReceipt(key)).toEqual({targetId:account,version:2});expect(calls.filter(c=>c.init.method==='PUT')).toHaveLength(1);expect(calls.at(-1)?.path).toBe('/api/v1/onboarding-receipts/updateCustomerProfile/'+key);expect(calls.at(-1)?.init.method).toBe('GET');client.dispose();
});
test('failed or revoked protected requests clear the in-memory account',async()=>{
  const {client}=fixture(call=>call.path.endsWith('/csrf')?Response.json({challenge:'challenge'}):call.path.endsWith('/login')?Response.json({accessToken:'synthetic',accountId:account,expiresAt:new Date(initialTime+900000).toISOString()}):call.path.endsWith('/me')?Response.json(subject):Response.json({code:'SESSION_INVALID'},{status:401}));
  await client.login(registration.email,registration.password);await expect(client.readProfile()).rejects.toMatchObject({status:401});expect(client.getSnapshot().status).toBe('guest');expect(client.getSnapshot().subject).toBeNull();client.dispose();
});
test('logout transport loss drops access without claiming successful cookie revocation',async()=>{
  const {client}=fixture(call=>call.path.endsWith('/csrf')?Response.json({challenge:'challenge'}):call.path.endsWith('/login')?Response.json({accessToken:'synthetic',accountId:account,expiresAt:new Date(initialTime+900000).toISOString()}):call.path.endsWith('/me')?Response.json(subject):Promise.reject(new Error('synthetic transport detail')));
  await client.login(registration.email,registration.password);await expect(client.logout()).rejects.toMatchObject({code:'OUTCOME_UNKNOWN'});expect(client.getSnapshot()).toEqual({status:'guest',subject:null,error:'OUTCOME_UNKNOWN'});client.dispose();
});
test('proof fragment is removed synchronously and only explicit POST can consume it',async()=>{
  const removed:string[]=[];captureEntryProof({href:'https://portal.example.invalid/verify-email#proofId='+key+'&proof='+'A'.repeat(43)},{replaceState:(_state,_unused,url)=>{removed.push(String(url));}});
  expect(removed).toEqual(['/verify-email']);expect(hasEntryProof('verification')).toBe(true);expect(hasEntryProof('recovery')).toBe(false);
  const {client,calls}=fixture(call=>call.path.endsWith('/csrf')?Response.json({challenge:'challenge'}):new Response(null,{status:204}));expect(calls).toHaveLength(0);await client.verify(account);expect(calls[1].path).toBe('/api/v1/customer-auth/verifications');expect(calls[1].init.method).toBe('POST');expect(JSON.parse(String(calls[1].init.body))).toEqual({proofId:key,proof:'A'.repeat(43)});expect(hasEntryProof('verification')).toBe(false);client.dispose();
});

test('OTP locator strips the fragment and submits only six typed digits',async()=>{
  const expiry='2026-10-30T10:02:00Z',removed:string[]=[];
  captureEntryProof({href:`https://portal.example.invalid/verify-email#proofId=${key}&expiresAt=${expiry}`},{replaceState:(_state,_unused,url)=>{removed.push(String(url));}});
  expect(removed).toEqual(['/verify-email']);expect(getVerificationLocator()).toEqual({proofId:key,expiresAt:Date.parse(expiry)});
  const {client,calls}=fixture(call=>call.path.endsWith('/csrf')?Response.json({challenge:'challenge'}):new Response(null,{status:204}));
  await expect(client.verify(account,'not-otp')).rejects.toMatchObject({code:'VALIDATION_FAILED'});
  await client.verify(account,'123456');expect(JSON.parse(String(calls[1].init.body))).toEqual({proofId:key,proof:'123456'});expect(getVerificationLocator()).toBeUndefined();client.dispose();
});
test('query duplicate wrong-route and oversized proofs never become possession authority',()=>{
  for(const href of ['https://portal.example.invalid/verify-email?proof=bad#proofId='+key+'&proof='+'A'.repeat(43),'https://portal.example.invalid/verify-email#proofId='+key+'&proof='+'A'.repeat(43)+'&proof=bad','https://portal.example.invalid/login#proofId='+key+'&proof='+'A'.repeat(43),'https://portal.example.invalid/recover/confirm#proofId='+key+'&proof='+'A'.repeat(500)]){
    const removed:string[]=[];captureEntryProof({href},{replaceState:(_state,_unused,url)=>{removed.push(String(url));}});expect(removed).toHaveLength(1);expect(hasEntryProof('verification')).toBe(false);expect(hasEntryProof('recovery')).toBe(false);expect(()=>withEntryProof('verification',()=>null)).toThrow('ENTRY_PROOF_UNAVAILABLE');
  }
});
