import { test } from 'node:test';
import assert from 'node:assert/strict';
import { mkdtempSync, readFileSync, rmSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { join } from 'node:path';
import { generate } from './material.mjs';

test('unique material has isolated least-privilege configuration and cannot rotate on restart', () => {
  const root=mkdtempSync(join(tmpdir(),'argopost-material-test-'));
  try {
    assert.equal(generate(root),'UNIQUE_LOCAL_MATERIAL_INITIALIZED_NO_ACCOUNTS');
    const file=join(root,'runtime/application.properties'), before=readFileSync(file,'utf8'), sql=readFileSync(join(root,'database/bootstrap.sql'),'utf8');
    assert.match(before,/PUBLIC_MEDIA_MODE=local/);assert.match(before,/CUSTOMER_SMTP_HOST=127\.0\.0\.1/);assert.match(before,/PG_MIGRATION_ENABLED=false/);
    assert.doesNotMatch(before,/AWS_PROFILE|smtp\.gmail|D:\\|IDENTITY_MANAGEMENT|ADMIN_DASHBOARD_READ/);
    assert.match(sql,/FROM PUBLIC/);assert.match(sql,/dispatch_network_writer/);assert.doesNotMatch(sql,/INSERT INTO identity\.user_account|GRANT .* TO argopost_runtime WITH/);
    assert.match(sql,/GRANT elogistics_local_admin_operator TO argopost_local_admin_operator WITH INHERIT TRUE, SET FALSE/);
    assert.doesNotMatch(before,/argopost_local_admin_operator/);
    assert.equal(generate(root),'EXISTING_LOCAL_MATERIAL_PRESERVED');assert.equal(readFileSync(file,'utf8'),before);
    assert.throws(()=>generate(root,'https://localhost:9999'),/MISMATCH_NO_ROTATION/);
    assert.throws(()=>generate(root,'http://localhost:5443'),/HTTPS_ORIGIN_REQUIRED/);
  } finally { rmSync(root,{recursive:true}); }
});
