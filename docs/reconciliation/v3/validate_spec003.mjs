// Local, process-free candidate-schema checks and mutation controls. No runtime claims.
import assert from 'node:assert/strict';
import fs from 'node:fs';
import path from 'node:path';
import {fileURLToPath} from 'node:url';
import {createRequire} from 'node:module';
const root=path.resolve(path.dirname(fileURLToPath(import.meta.url)),'../../..');
const require=createRequire(path.join(root,'packages/api-client/package.json'));
for(const [name,version] of [['ajv','8.20.0'],['ajv-formats','3.0.1']])assert.equal(require(name+'/package.json').version,version);
const Ajv=require('ajv/dist/2020.js').default,formats=require('ajv-formats');
const dir=path.join(root,'specs/003-sender-merchant-onboarding/contracts');
const read=name=>JSON.parse(fs.readFileSync(path.join(dir,name),'utf8'));
const expected=new Set(`requestLegacyCustomerLink confirmLegacyCustomerLink getCustomerCsrf enrollCustomer resendCustomerVerification verifyCustomerEmail loginCustomer refreshCustomer logoutCustomer getCustomerSubject requestCustomerRecovery confirmCustomerRecovery requestSecurityEmailChange confirmSecurityEmailChange requestCustomerReauthentication createOwnerRecoveryConsent getCustomerProfile updateCustomerProfile listCustomerContexts selectCustomerContext requestBusinessContactProof confirmBusinessContactProof listOwnApplications createApplicationDraft getOwnApplication updateApplicationDraft submitApplication resubmitApplication withdrawApplication uploadApplicationEvidence getApplicationEvidence downloadApplicationEvidence removeDraftEvidence listReviewApplications getReviewApplication takeApplicationReview requestApplicationInformation approveApplication rejectApplication getMerchantOrganization updateMerchantLegalProfile getMerchantLegalProfileRevision uploadMerchantLegalEvidence getMerchantLegalEvidence downloadMerchantLegalEvidence reviewMerchantLegalProfile suspendMerchantOrganization reinstateMerchantOrganization closeMerchantOrganization requestMerchantClosure listMerchantMembers replaceMerchantMemberGrants suspendMerchantMember reinstateMerchantMember revokeMerchantMember leaveMerchantOrganization listMerchantInvitations createMerchantInvitation resendMerchantInvitation revokeMerchantInvitation previewMerchantInvitation acceptMerchantInvitation rejectMerchantInvitation listMerchantFacilities createMerchantFacility updateMerchantFacility getMerchantDefaults updateMerchantDefaults proposeMerchantOwnerTransfer acceptMerchantOwnerTransfer revokeMerchantOwnerTransfer getMerchantOwnerTransfer requestMerchantOwnerRecovery approveMerchantOwnerRecovery getOnboardingReceipt`.split(' '));
function gate(doc,index){
 assert.equal(doc.openapi,'3.1.0');assert.equal(doc['x-status'],'OWNER_REVIEWED_PLANNING_BASELINE');
 const resolve=x=>x?.$ref?x.$ref.split('/').slice(1).reduce((o,k)=>o[k],doc):x;
 const found=new Map();
 for(const [url,item] of Object.entries(doc.paths))for(const [method,op] of Object.entries(item)){
  assert(!found.has(op.operationId),'duplicate operation');found.set(op.operationId,op);
  const indexed=index.find(i=>i.operation_id===op.operationId);assert(indexed,'indexed operation');assert.equal(indexed.path,url);assert.equal(indexed.method,method);
  const params=op.parameters.map(resolve);
  for(const name of [...url.matchAll(/\{([^}]+)\}/g)].map(m=>m[1]))assert(params.some(p=>p.in==='path'&&p.name===name&&p.required),'path parameter');
  assert(op.responses[String(indexed.success)],'success response');
  if(op['x-authority']==='PREAUTH')assert.deepEqual(op.security,[]);else if(op['x-authority']!=='CSRF_SESSION_OR_PREAUTH')assert(op.security.length,'authenticated operation');
  if(indexed.versioned)assert(params.some(p=>p.name==='If-Match'&&p.required),'required version');
  if(method!=='get')for(const name of ['Origin','X-CSRF-Token'])assert(params.some(p=>p.name===name&&p.required),'mutation CSRF/Origin');
  if(op['x-idempotency'])assert(params.some(p=>p.name==='Idempotency-Key'&&p.required),'mutation receipt key');
  if(op.requestBody){const contents=Object.values(op.requestBody.content);assert.equal(contents.length,1);assert.equal(contents[0].schema.$ref,'#/components/schemas/'+indexed.request);}
  else assert.equal(indexed.request,null);
  for(const [status,raw] of Object.entries(op.responses)){
   const response=resolve(raw);assert(response,'resolved response');
   assert.equal(resolve(response.headers?.['Cache-Control'])?.schema.const,'no-store');
   assert(resolve(response.headers?.['X-Request-Id'])?.required);
   if(status==='204')assert(!response.content,'empty204');
   if(Number(status)>=400)assert(response.content?.['application/json']?.schema.properties.code.enum.length,'typed errors');
   if(status==='429')assert(resolve(response.headers['Retry-After'])?.required);
  }
 }
 assert.deepEqual(new Set(found.keys()),expected,'complete independent operation inventory');assert.equal(index.length,78);
 for(const id of ['approveApplication','requestMerchantOwnerRecovery','reviewMerchantLegalProfile'])assert.equal(found.get(id)['x-authority'],'BUSINESS_ADMIN');
 assert.equal(found.get('getCustomerProfile')['x-permission-candidate'],'SELF');
 assert(![...found.keys()].includes('acceptIdentityInvitation'));
 for(const id of ['getReviewOrganization','getReviewOwnerRecovery'])assert.equal(found.get(id)['x-authority'],'BUSINESS_ADMIN');
 assert.equal(found.get('getReviewOwnerRecovery')['x-permission-candidate'],'ownership.recover');
 assert.deepEqual(resolve(found.get('refreshCustomer').responses['401']).content['application/json'].schema.properties.code.enum,['SESSION_INVALID','REFRESH_REPLAY']);
 assert.equal(found.get('uploadMerchantLegalEvidence')['x-authority'],'OWNER_OR_RECOVERY_REVIEWER');
 assert.equal(found.get('uploadMerchantLegalEvidence')['x-permission-candidate'],'org.legal.write|ownership.recover');
 return found;
}
const doc=read('openapi.candidate.json'),index=read('operation-index.json'),fixtures=read('fixtures.json');
expected.add('verifyCustomerRecovery');expected.add('getReviewOrganization');expected.add('getReviewOwnerRecovery');
const operations=gate(doc,index);
const ajv=new Ajv({strict:true,allErrors:true});formats(ajv);ajv.addFormat('binary',true);
const dependencySchemas=read('dependency-schemas.json').$defs;
assert.deepEqual(new Set(Object.keys(dependencySchemas)),new Set(['CatalogEligibility','ServicePolicyEligibility','ClosureClearance']));
const schemas=JSON.parse(JSON.stringify({...doc.components.schemas,...dependencySchemas}).replaceAll('#/components/schemas/','#/$defs/'));
const validators=new Map();let sampleChecks=0,negativeChecks=0;
for(const name of Object.keys(schemas))validators.set(name,ajv.compile({$schema:'https://json-schema.org/draft/2020-12/schema',$defs:schemas,$ref:'#/$defs/'+name}));
for(const [name,value] of Object.entries(fixtures.samples)){
 const validate=validators.get(name);assert(validate,'known sample schema');assert(validate(value),name+': '+ajv.errorsText(validate.errors));sampleChecks++;
 if(schemas[name].type==='object'){
  assert(!validate({...value,owner:true}),name+' unexpected owner field');negativeChecks++;
  for(const required of schemas[name].required){const copy=structuredClone(value);delete copy[required];assert(!validate(copy),name+' required '+required);negativeChecks++;}
 }
}
assert.equal(Object.keys(fixtures.samples).length,Object.keys(schemas).length-1,'every JSON DTO sample, binary upload separate');
assert(validators.get('ApplicationDraftInput')({}),'empty draft before upload');
assert(!validators.get('ApplicationInput')({}),'submission cannot be empty draft');
const uuid=fixtures.samples.CustomerSubject.accountId;
for(const [name,value] of [
 ['ContextInput',{kind:'MERCHANT',organizationId:uuid}],
 ['Context',{...fixtures.samples.Context,kind:'MERCHANT',organizationId:uuid,membershipId:uuid}],
 ['Application',{...fixtures.samples.Application,state:'APPROVED',organizationId:uuid}],
 ['ContextList',{items:[],page:5,size:20,hasNext:true}]
]){assert(validators.get(name)(value),'valid scoped read '+name);sampleChecks++;}
for(const [name,value] of [
 ['ContextInput',{kind:'MERCHANT'}],['ContextInput',{kind:'PERSONAL',organizationId:uuid}],
 ['Context',{...fixtures.samples.Context,kind:'MERCHANT'}],['Context',{...fixtures.samples.Context,organizationId:uuid}],
 ['Context',{...fixtures.samples.Context,kind:'MERCHANT',organizationId:uuid}],
 ['Application',{...fixtures.samples.Application,state:'APPROVED'}],['Application',{...fixtures.samples.Application,organizationId:uuid}],
 ['ContextList',{items:[]}]
]){assert(!validators.get(name)(value),'invalid scope/navigation '+name);negativeChecks++;}
for(const [name,bad] of [['ProfileInput',{...fixtures.samples.ProfileInput,accountId:'client-actor'}],['EnrollmentInput',{...fixtures.samples.EnrollmentInput,accountType:'MERCHANT'}],['CustomerLogin',{phone:'0900000000',password:'synthetic'}],['MemberInput',{role:'OWNER',grants:[]}],['ApplicationInput',{...fixtures.samples.ApplicationInput,ownerAccountId:'client-owner'}]]){assert(!validators.get(name)(bad),'security injection '+name);negativeChecks++;}
for(const name of ['EnrollmentInput','RecoveryConfirm']){
 const field=name==='EnrollmentInput'?'password':'newPassword';
 const byteValid=value=>validators.get(name)({...fixtures.samples[name],[field]:value})&&Buffer.byteLength(value,'utf8')<=72;
 assert(byteValid('a'.repeat(15)));assert(!byteValid('a'.repeat(14)));assert(byteValid('a'.repeat(72)));assert(!byteValid('á'.repeat(37)));negativeChecks+=3;sampleChecks+=2;
}
let mutationChecks=0;
for(const mutate of [
 d=>delete d.paths['/customer/profile'].get,
 d=>{d.paths['/business-review/applications/{applicationId}/approval'].post['x-authority']='CUSTOMER';},
 d=>{d.paths['/merchant-organizations/{organizationId}/defaults'].put.parameters=[];},
 d=>{d.components.headers.CacheControl.schema.const='public';},
 d=>{d.components.responses.Error429.headers={};},
 d=>{d['x-status']='ACCEPTED';},
 d=>{d.paths['/customer/profile'].get.security=[];},
 d=>{d.paths['/customer-auth/verifications'].post.responses['204'].content={'application/json':{}};}
 ,d=>{d.paths['/business-review/organizations/{organizationId}/owner-recoveries/{transferId}'].get['x-authority']='CUSTOMER';}
 ,d=>{d.components.responses.RefreshError401.content['application/json'].schema.properties.code.enum=['SESSION_INVALID'];}
 ,d=>{d.paths['/merchant-organizations/{organizationId}/legal-profile-evidence'].post['x-permission-candidate']='org.legal.write|member.manage';}
]){const changed=structuredClone(doc);mutate(changed);assert.throws(()=>gate(changed,index),'contract mutation rejected');mutationChecks++;}
const harmless=structuredClone(doc);harmless.info.description+=' Reviewed candidate.';gate(harmless,index);
console.log(JSON.stringify({status:'PASS',operations:operations.size,wire_schemas:Object.keys(doc.components.schemas).length,dependency_schemas:Object.keys(dependencySchemas).length,sample_checks:sampleChecks,negative_schema_checks:negativeChecks,contract_mutation_checks:mutationChecks,runtime_evidence:'NOT_IMPLEMENTED'}));
