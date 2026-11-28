import { randomBytes, randomUUID, generateKeyPairSync } from 'node:crypto';
import { mkdirSync, existsSync, readFileSync, writeFileSync, chmodSync, chownSync } from 'node:fs';
import { join, dirname, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';
import { execFileSync } from 'node:child_process';

const here = dirname(fileURLToPath(import.meta.url));
const secret = () => randomBytes(32).toString('base64url');
const key = () => randomBytes(32).toString('base64');
const jwk = kid => { const { privateKey } = generateKeyPairSync('rsa', { modulusLength: 3072 }); return JSON.stringify({ keys: [{ ...privateKey.export({ format: 'jwk' }), kid, use: 'sig', alg: 'RS256' }] }); };
const write = (path, value, uid = 0, mode = 0o600) => { writeFileSync(path, value, { flag: 'wx', mode });if (process.getuid?.() === 0) chownSync(path, uid, uid);chmodSync(path, mode); };
const properties = value => Object.entries(value).map(([k, v]) => `${k}=${String(v).replaceAll('\\', '\\\\').replaceAll('\n', '\\n')}\n`).join('');
const openssl = (...args) => execFileSync('openssl', args, { stdio: 'ignore' });

/** Generates no user/account/grant or business fact. Existing material is never rotated. */
export function generate(root = '/run', origin = 'https://localhost:5443') {
  const url = new URL(origin);
  if (url.protocol !== 'https:' || url.hostname !== 'localhost' || url.pathname !== '/' || url.username || url.password || url.search || url.hash) throw Error('EXACT_LOCAL_HTTPS_ORIGIN_REQUIRED');
  const db = join(root, 'database'), runtime = join(root, 'runtime'), web = join(root, 'web'), admin = join(root, 'admin-operator');
  for (const path of [db, runtime, web, admin]) mkdirSync(path, { recursive: true });
  const marker = join(db, 'material-ready.json');
  if (existsSync(marker)) {
    const previous = JSON.parse(readFileSync(marker, 'utf8'));
    if (previous.format !== 2 || previous.origin !== url.origin || ![join(runtime, 'application.properties'),join(runtime,'backend.p12'),join(web,'web.key'),join(web,'ca.crt'),join(db,'bootstrap.sql'),join(db,'migration.properties'),join(admin,'pgpass')].every(existsSync)) throw Error('EXISTING_MATERIAL_MISMATCH_NO_ROTATION');
    return 'EXISTING_LOCAL_MATERIAL_PRESERVED';
  }
  if (existsSync(join(db, 'installation.json'))) throw Error('PARTIAL_MATERIAL_REQUIRES_INSPECTION_NO_OVERWRITE');
  const suffix = randomBytes(6).toString('hex'), realm = randomUUID(), owner = 'elogistics_test_owner_' + suffix;
  const installation = { deploymentId: randomUUID(), realm, owner, database: 'argopost_admin_uat_' + suffix, staffBindingRequest: randomUUID(), origin: url.origin };
  const passwords = { bootstrap: secret(), migrator: secret(), runtime: secret() };
  write(join(db, 'installation.json'), JSON.stringify(installation));
  write(join(db, 'database-passwords.json'), JSON.stringify(passwords));
  write(join(db, 'pg-bootstrap'), passwords.bootstrap, 999);
  write(join(db, 'migration.properties'), properties({
    PG_DB_URL: `jdbc:postgresql://postgres:5432/${installation.database}`, PG_MIGRATOR_USER: 'argopost_migrator', PG_MIGRATOR_PASSWORD: passwords.migrator,
    PG_OWNER_ROLE: owner, PG_RUNTIME_USER: 'argopost_runtime', IDENTITY_SYSTEM_REALM_ID: realm,
    'spring.datasource.hikari.maximum-pool-size': 2, 'spring.datasource.hikari.connection-timeout': 5000,
  }), 10001);
  write(join(db, 'pgpass'), `postgres:5432:${installation.database}:argopost_migrator:${passwords.migrator}\n`);
  const adminPassword = secret();
  write(join(admin,'pgpass'),`postgres:5432:${installation.database}:argopost_local_admin_operator:${adminPassword}\n`);
  write(join(admin,'binding.json'),JSON.stringify({database:installation.database,realm,requestId:randomUUID(),deploymentId:installation.deploymentId}));
  // BRC-ACL-01: source/template only for NEW clusters. No role repair on existing volumes.
  execFileSync('python3', [join(here,'bootstrap.py'), db], { stdio: 'ignore',env:{...process.env,ARGOPOST_NEW_ADMIN_OPERATOR_PASSWORD:adminPassword} });
  if (process.getuid?.() === 0) chownSync(join(db,'bootstrap.sql'),999,999);
  // Private CA stays in the bootstrap volume; runtime/frontend get only their own keys.
  openssl('req','-x509','-newkey','rsa:3072','-nodes','-sha256','-days','365','-subj','/CN=ArgoPost Isolated Local CA','-keyout',join(db,'ca.key'),'-out',join(db,'ca.crt'));
  for (const [name, directory, uid] of [['backend',runtime,10001],['web',web,0]]) {
    const pkey=join(directory,name+'.key'), cert=join(directory,name+'.crt'), csr=join(db,name+'.csr'), ext=join(db,name+'.ext');
    write(ext, 'subjectAltName=DNS:localhost,DNS:backend,IP:127.0.0.1\nextendedKeyUsage=serverAuth\n');
    openssl('req','-new','-newkey','rsa:3072','-nodes','-subj','/CN=localhost','-keyout',pkey,'-out',csr);
    openssl('x509','-req','-in',csr,'-CA',join(db,'ca.crt'),'-CAkey',join(db,'ca.key'),'-set_serial','0x'+randomBytes(16).toString('hex'),'-days','90','-sha256','-extfile',ext,'-out',cert);
    for (const path of [pkey,cert]) { chmodSync(path,0o600);if(process.getuid?.()===0)chownSync(path,uid,uid); }
    write(join(directory,'ca.crt'),readFileSync(join(db,'ca.crt')),uid,0o644);
  }
  const p12password=secret();write(join(db,'p12-password'),p12password);
  openssl('pkcs12','-export','-name','argopost-local','-inkey',join(runtime,'backend.key'),'-in',join(runtime,'backend.crt'),'-certfile',join(db,'ca.crt'),'-out',join(runtime,'backend.p12'),'-passout','file:'+join(db,'p12-password'));
  chmodSync(join(runtime,'backend.p12'),0o600);if(process.getuid?.()===0)chownSync(join(runtime,'backend.p12'),10001,10001);
  const config = {
    PG_DB_URL: `jdbc:postgresql://postgres:5432/${installation.database}`, PG_RUNTIME_USER: 'argopost_runtime', PG_RUNTIME_PASSWORD: passwords.runtime,
    PG_MIGRATION_ENABLED: false, PG_OWNER_ROLE: owner, PG_MIGRATOR_USER: 'argopost_migrator', PG_MIGRATOR_PASSWORD: '', IDENTITY_SYSTEM_REALM_ID: realm,
    CUSTOMER_ROLLOUT:'SANDBOX', CUSTOMER_BIND_ADDRESS:'0.0.0.0', CUSTOMER_HTTPS_PORT:8443,
    CUSTOMER_HTTPS_KEY_STORE:'file:/run/runtime/backend.p12',CUSTOMER_HTTPS_KEY_STORE_PASSWORD:p12password,
    CUSTOMER_SECURITY_ORIGIN:url.origin,CUSTOMER_SECURITY_ISSUER:url.origin+'/customer',CUSTOMER_SECURITY_AUDIENCE:'argopost-local-customer',CUSTOMER_SECURITY_CURRENT_KID:'customer-local',CUSTOMER_SECURITY_JWK_SET:jwk('customer-local'),CUSTOMER_SECURITY_LIMITER_HMAC:key(),
    CUSTOMER_SECURITY_PROOF_VERIFIER_KEYS:JSON.stringify({v1:key()}),CUSTOMER_SECURITY_CURRENT_PROOF_KEY:'v1',CUSTOMER_SECURITY_DELIVERY_ENCRYPTION_KEYS:JSON.stringify({e1:key()}),CUSTOMER_SECURITY_CURRENT_ENCRYPTION_KEY:'e1',
    IDENTITY_SECURITY_ORIGIN:url.origin,IDENTITY_SECURITY_ISSUER:url.origin+'/system',IDENTITY_SECURITY_AUDIENCE:'argopost-local-system',IDENTITY_SECURITY_CURRENT_KID:'system-local',IDENTITY_SECURITY_JWK_SET:jwk('system-local'),IDENTITY_SECURITY_LIMITER_HMAC:key(),
    MERCHANT_CUSTOMER_PROFILE_RECEIPT_KEYS:JSON.stringify({m1:key()}),MERCHANT_CUSTOMER_PROFILE_CURRENT_RECEIPT_KEY:'m1',
    CUSTOMER_SMTP_MODE:'TEST_LOOPBACK',CUSTOMER_SMTP_HOST:'127.0.0.1',CUSTOMER_SMTP_PORT:1025,CUSTOMER_SMTP_SENDER:'no-reply@example.invalid',CUSTOMER_SMTP_APPROVED_SENDER_DOMAINS:'example.invalid',
    PUBLIC_MEDIA_MODE:'local',PUBLIC_MEDIA_LOCAL_ROOT:'/app/public-media',SHIPPING_PILOT_ENVIRONMENT:'LOCAL',BUSINESS_WORKSPACE_ENABLED:true,BUSINESS_WORKSPACE_ENVIRONMENT:'LOCAL',
    'identity.security.origin':url.origin,'identity.security.issuer':url.origin+'/system','identity.security.audience':'argopost-local-system','identity.security.current-kid':'system-local',
    'identity.admin-dashboard.enabled':true,'identity.admin-dashboard.environment':'LOCAL','identity.admin-onboarding.enabled':false,'identity.local-realm-bootstrap.enabled':false,
    'identity.operations.enabled':true,'identity.operations.environment':'LOCAL','identity.staff.enabled':false,
    'merchant.application-workflow.enabled':true,'merchant.admin-governance.enabled':true,'identity.business-contact.enabled':true,'identity.admin-governance-step-up.enabled':true,
    'shipping.pilot.business-intake-enabled':true,'support.claim-evidence.mode':'filesystem','support.claim-evidence.private-root':'/app/private-evidence',
    'support.claim-evidence.public-roots':'/app/public-media','support.claim-evidence.clamav-host':'127.0.0.1','support.claim-evidence.clamav-port':3310,
    'support.tickets.organization-enabled':false,'support.claim-evidence.enabled':false,
    'spring.datasource.hikari.maximum-pool-size':8,'spring.datasource.hikari.minimum-idle':1,'spring.datasource.hikari.connection-timeout':5000,
    'logging.level.root':'WARN','server.error.include-message':'never','server.error.include-stacktrace':'never',
  };
  config['identity.security.jwk-set']=config.IDENTITY_SECURITY_JWK_SET;
  config['identity.security.limiter-hmac']=config.IDENTITY_SECURITY_LIMITER_HMAC;
  config['business.workspace.enabled']=true;config['business.workspace.environment']='LOCAL';
  write(join(runtime,'application.properties'),properties(config),10001);
  write(join(runtime,'pgpass'),`postgres:5432:${installation.database}:argopost_runtime:${passwords.runtime}\n`,10001);
  write(join(runtime,'database-name'),installation.database,10001,0o644);
  write(marker,JSON.stringify({ format:2, origin:url.origin, deploymentId:installation.deploymentId,approvedOperatorDelta:'a15804e3a026e314345e326855bb2485efaa1aec3e01baf7c291903487c2d0da' }));
  return 'UNIQUE_LOCAL_MATERIAL_INITIALIZED_NO_ACCOUNTS';
}

export function bind(root='/run') {
  const db=join(root,'database'), meta=JSON.parse(readFileSync(join(db,'installation.json'),'utf8'));
  if(!/^argopost_admin_uat_[a-f0-9]{12}$/.test(meta.database)||!/^elogistics_test_owner_[a-f0-9]{12}$/.test(meta.owner))throw Error('EXACT_LOCAL_BINDING_REQUIRED');
  const sql=`BEGIN; SET LOCAL ROLE ${meta.owner}; SELECT * FROM identity.bind_local_staff_environment('${meta.staffBindingRequest}'::uuid,'LOCAL','Compose isolated local environment binding'); COMMIT;`;
  execFileSync('psql',['-X','-w','-h','postgres','-U','argopost_migrator','-d',meta.database,'-v','ON_ERROR_STOP=1','-v','VERBOSITY=sqlstate'],{input:sql,env:{...process.env,PGPASSFILE:join(db,'pgpass')},stdio:['pipe','ignore','ignore']});
  return 'LOCAL_STAFF_ENVIRONMENT_BOUND_NO_IDENTITIES_OR_ASSIGNMENTS';
}
export function bindAdmin(root='/run') {
  const admin=join(root,'admin-operator'), meta=JSON.parse(readFileSync(join(admin,'binding.json'),'utf8'));
  if(!/^argopost_admin_uat_[a-f0-9]{12}$/.test(meta.database)||![meta.realm,meta.requestId,meta.deploymentId].every(v=>/^[a-f0-9]{8}(?:-[a-f0-9]{4}){3}-[a-f0-9]{12}$/.test(v)))throw Error('EXACT_LOCAL_BINDING_REQUIRED');
  const sql=`SELECT * FROM identity.bind_local_admin_environment('${meta.requestId}'::uuid,'${meta.realm}'::uuid,'LOCAL','Portable Compose isolated environment binding');`;
  execFileSync('psql',['-X','-w','-h','postgres','-U','argopost_local_admin_operator','-d',meta.database,'-v','ON_ERROR_STOP=1','-v','VERBOSITY=sqlstate'],{input:sql,env:{...process.env,PGPASSFILE:join(admin,'pgpass')},stdio:['pipe','ignore','ignore']});
  return 'LOCAL_ADMIN_ENVIRONMENT_BOUND_SYSTEM_INACTIVE_NO_ACCOUNTS';
}
if (process.argv[1] && resolve(process.argv[1]) === fileURLToPath(import.meta.url)) {
  try { console.log(process.argv[2]==='bind-admin' ? bindAdmin() : process.argv[2]==='bind' ? bind() : generate('/run',process.env.ARGOPOST_ORIGIN ?? 'https://localhost:5443')); }
  catch { console.error('LOCAL_MATERIAL_OR_BINDING_FAILED_NO_SECRET_OUTPUT; existing material preserved');process.exitCode=1; }
}
