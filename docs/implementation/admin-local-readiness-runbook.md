# Admin isolated Local/Test deployment and rollback runbook

Status: **ADMIN_DASHBOARD_LOCAL_UAT_READY — ISOLATED EMPTY-COHORT ACCEPTANCE**.
Ngày Owner: 2026-11-22, Asia/Saigon.

Owner approved exact deployment manifest9e0773d7... end-to-end. HTTPS startup has
been corrected and effective pool/JDBC, real Owner Root/Admin verification and
SYSTEM login are observed. Revocation/restoration and reversible target-only
rollback have passed. Natural access expiry after915039ms, refresh and logout
have passed;23/23 actual Browser gates. Evidence and residual source gaps are in
[actual execution](admin-uat-deployment-execution.md). Earlier startup/review-blocked
paragraphs below are historical and superseded by that resume section.
The manifest's pending fields are immutable historical evidence, not a request
for repeat approval. Current execution records recovery, exact role bootstrap,
39 migrations through V40, physical binding, real Gmail/Owner verification and
Admin login. Hikari startup and automatic-review blockers were resolved through
approved execution. No Production/full-suite/CI or positive live-data aggregation
claim. Services remain ready and the test Browser is intentionally logged out.

The preparation-only paragraphs below describe the earlier checkpoint and are
superseded by the current execution report/Owner approval.

Current [consolidated deployment record](admin-uat-deployment-approval.md) and
[exact target manifest](admin-uat-deployment-manifest.json) supersede the earlier
no-target state. Owner-authorized step3 created only a new empty PG18 container,
volume and database at127.0.0.1:5543; physical identity and baseline dump/list
verified. No privileged prerequisites, migrations, operator membership, Identity
provisioning, SYSTEM activation or Backend/Frontend deployment has run. Owner's
step5 explicitly requires approval of that exact record before those operations.

Source V39/V40 and adapters reached ADMIN_ONBOARDING_SOURCE_TEST_READY;
see [execution evidence](admin-onboarding-execution.md). This runbook remains
prepared only for application deployment: no real operator membership or mailbox
UAT has been executed. Empty infrastructure preparation now has explicit Owner
authorization; privileged/application execution still awaits the exact record.
[AON-ACL-02](admin-onboarding-control-function-acl.delta.md) is separately
Owner-approved for source/test and its formerly failing catalog gate now PASS.
Real target deployment remains unauthorized; the catalog ACL amendment must
not be inferred to authorize changing a real/shared database.
No command in this document grants deployment or technical authority by itself.
The parent manifest and Option B SOURCE/TEST are approved; real deployment still
requires an exact isolated target and independent operator authorization.
No automatic shared/demo use.

## 1. Freeze the execution record before mutation

Record privately: exact source fingerprints/build SHA, approved manifest SHA,
container ID and ownership labels, immutable image digest, dedicated volume,
loopback host/port/database/OID/system identifier, owner/runtime/operator DB roles,
realm UUID, independent operator login and role membership, environment LOCAL/TEST,
frontend/backend origins/ports, certificate thumbprint/SAN, selected read codes,
controlled mailbox and correlation/request IDs. No password/token/key in Git or
public evidence. Only nonsecret exact target metadata belongs in the acceptance record.

The target must be a dedicated `argopost_admin_uat_<12 lower hex>` database and
an isolated volume/container, never `elogistics_dev`,
`elogistics-d1-postgres18` or `elogistics_d1_pg18_dev_data`. Generate identifiers
through the approved workflow, not a fixture or copied Root binding. Check ports
are free; do not terminate unrelated processes to take a port. Do not create
another worktree/repository. A new temporary isolated service is not a repository.

Stop if target identity, operator authority, source/migration version, backup or
ROOT prerequisite cannot be established uniquely. Starting the demo is not a
substitute for satisfying these gates.

## 2. Database and recovery preflight

1. Read-only inspect actual Docker ID/image/labels/volume/ports/state. Confirm
   PostgreSQL18 and healthy status, not a stale health value on an exited container.
2. Verify database name/current user/server version and physical identity using
   protected connection injection; no password command-line argument or raw config
   dump. Runtime must not be superuser/schema owner/operator/writer.
3. On an existing isolated target, make a custom-format pg_dump recovery point,
   checksum it and inspect pg_restore --list. Restore to a SECOND disposable
   validation database only under exact owned-resource authorization. Compare
   row/schema constraints and Flyway history there. Never restore over the target
   or use developer/demo volumes as scratch. If only archive listing is verified,
   report restore rehearsal NOT VERIFIED and stop before required mutations.
4. Confirm applied versions/checksums against source and approved manifest. Current
   source has37 historical migrations through V38 plus V39/V40, no V33. On the
   chosen target verify V39/V40 are either unapplied or exactly match their source;
   collision or old checksum mismatch stops. No placeholder, clean, repair,
   baseline trick, down migration or historical SQL edit.
5. Existing V36/V37 privileged isolated role prerequisites are still required for
   a fresh full-schema target. Do not weaken their runtime guards or grant their
   operator memberships to solve Admin setup. New Admin/optional Realm role
   prerequisites follow the approved delta only; NOLOGIN does not mean a login
   has been authorized to assume it.
6. Apply only the forward migrations authorized for this target; validate and
   rerun no-op info. Record exact versions/checksums and database change inventory.
   Schema migration does not itself provision an Admin or activate a realm.

## 3. SYSTEM prerequisite and impact review

Option A: read-only verify an existing legitimately ACTIVE isolated SYSTEM realm,
bound Initial Root and its accepted provenance. No Root/grant change. A restored
test fixture or fake consumed proof is not a legitimate prerequisite.

Option B: only if optional V40 was affirmatively approved, use its distinct Realm
operator on a new dedicated environment. New Root ID/login comes from the wrapper;
V4 creates its exact management pair and immutable bootstrap binding. Use the real
protected24h invitation, controlled mailbox and credential activation before
calling bounded V4 activation wrapper. No direct email_verified/password/proof SQL.
The read-only Admin account is a separate later account and never gets this pair.

Before activation inventory all SYSTEM-scope grant rows and current verified
eligible accounts on THAT target. Explain that activating SYSTEM can admit exact
IAM accounts through the existing auth engine; disabling Dashboard alone does not
disable IAM. Verify no imported Customer/Staff/root grants or extra realm rows.
If optional Root bootstrap is cancelled, quarantine the isolated realm; do not
reassign its bootstrap account or silently create another Root.

## 4. Runtime and transport preparation

- Build the current source with existing Maven/npm verification and fingerprint
  the artifact; source hash alone does not prove a stale JAR is current. Keep
  artifact/config/process records in protected target-specific storage.
- Existing `scripts/dev/start-local.ps1` is hardwired to demo5433/elogistics_dev;
  **do not use it for this isolated deployment**. Its process ownership/readiness
  helpers can be extracted/reused after approval, but there is no implemented
  isolated Admin start command in this checkpoint.
- Use target-specific process configuration only; no global env/credential change.
  Do not run `local.cmd init`, regenerate keys/certs, reset DB or restart demo.
  Existing private TLS material may be reused read-only if its SAN/trust fit the
  new loopback origins. Do not install a Root CA without approval, disable TLS
  validation, set insecure cookies or bypass CSRF.
- Prefer the certificate's existing `localhost` SAN for the isolated origin and
  a dedicated browser profile, keeping demo `127.0.0.1` cookies separate. Cookies
  are host-scoped, not port-scoped; changing ports alone is not browser isolation.
  Pin exact frontend Origin/issuer/audience and target-specific signing/protection
  key material through existing protected configuration. Never rotate/overwrite
  demo keys or let isolated cookies/proofs become demo credentials.
- Explicit LOCAL/TEST SANDBOX, default-off gates. Enable onboarding/delivery and
  Dashboard only for the approved physically bound target. Keep unrelated Support,
  Finance/Carrier and public Staff features at their accepted default/target state.
- At least two pool connections are required by Admin lock-before-snapshot reads.
  Verify actual effective Hikari maximum, acquisition timeout, statement timeout
  and concurrent HTTP behavior; a configured number alone is insufficient.
- Use existing protected invitation key rings and Gmail configuration; no new
  provider or App Password request. Missing isolated-safe configuration stops.
  Unit/CI uses test transport. Live acceptance requires actual receipt in an
  Owner-controlled mailbox, not only JavaMailSender provider acceptance.
- Start only owned backend/frontend processes on free loopback ports, record PID
  plus start-time identity before ownership registration, and wait for actual
  Backend HTTPS/DB and Frontend HTTPS readiness. Report no READY from process launch.

## 5. Dedicated Admin provisioning

Run the approved offline Java adapter/operator procedure using its operator-only
connection; no runtime HTTP provisioning, direct DML or SET ROLE writer shortcut.
Exact SQL/interface signatures are implemented and source-tested. There is no
new general SQL executor or demo launcher shortcut.

The offline composition is:

```java
// Independently authorized, protected target-specific DataSources; never runtime beans.
var admins = new LocalAdminProvisioner(adminOperatorDataSource, invitationProtection);
var roots = new LocalRealmBootstrapProvisioner(realmOperatorDataSource, invitationProtection);
// Supply reviewed command records and stable request IDs; neither adapter accepts a password.
// Bind first with identity.bind_local_admin_environment using the Admin operator.
// roots.provision -> real protected email -> /admin/activate -> roots.activate.
// admins.provision -> real protected email -> /admin/activate -> /admin/login.
```

Use the existing protected configuration mechanism for key rings/SMTP; do not
copy test fixture keys or inbox extraction into the real procedure. The owned
source tests exercise these exact Java/SQL adapters; they do not authorize
instantiating operator DataSources against any real target.

Private isolated runtime properties (all three new flags default false):

```properties
customer.rollout=SANDBOX
identity.admin-dashboard.environment=LOCAL
identity.admin-dashboard.enabled=true
identity.admin-onboarding.enabled=true
identity.admin-invitations.delivery-enabled=true
identity.local-realm-bootstrap.enabled=true
server.forward-headers-strategy=NONE
```

These are a future target configuration example, not settings applied in this
checkpoint. Set bootstrap false after approved first-bootstrap acceptance;
it never activates a realm by itself. Supply the existing Identity signing/
limiter and protected invitation/SMTP settings through private config, not Git.
Frontend and Identity exact HTTPS origins must agree. Use a fresh Admin browser
profile: Customer/Staff cookies and bound SYSTEM sessions are not preauth proofs.

1. Explicitly select a new controlled Admin mailbox/display name and exact read
   set. Mandatory ADMIN_DASHBOARD_READ. Add only the selected capabilities; no
   implicit all-six, IDENTITY_MANAGEMENT or Role->permission auto grant.
2. Use one stable request UUID and canonical payload. Provision/seal/account/
   grants/proof/outbox/audit/receipt must commit together; email outside transaction.
3. Same-key exact retry must return a receipt without creating another account,
   grant, audit or protected delivery. Changed payload must conflict. Current
   operator revocation/environment mismatch must deny even a prior receipt lookup.
4. Admin receives real email, opens fragment link, sets a password directly in
   the UI and completes cryptographic verification. Then log in normally. Never
   send password/proof/token to Codex or record it in screenshots/network traces.
5. Record generated nonsecret account/realm IDs, current grant set/state, audit
   correlations and receipt metadata. No credentials in the evidence report.

## 6. Real acceptance checklist (all initially NOT RUN)

- [ ] Email inbox receipt and genuine single-use invitation/password verification.
- [ ] Before verification login denied; after verification real SYSTEM login,
  refresh/expiry/logout, current permissions and secure cookies/CSRF work.
- [ ] Wrong/expired/cancelled proof, resend-old proof, replay/input mismatch and
  concurrent acceptance behave correctly without duplicate mutation/delivery intent.
- [ ] Customer/Staff/Root cannot become Dashboard read authority; wrong origin,
  query tokens, unauthenticated and unselected capabilities deny at Backend.
- [ ] Six Admin APIs return correct authorized data or honest source-unavailable.
  Dedicated controlled test shipments, if needed, must be booked through the
  canonical approved Customer/Pilot flow; do not insert fake KPI rows or import
  demo PII. Empty canonical cohorts may correctly yield zero but do not prove
  positive aggregation/drill-through acceptance.
- [ ] Booking/Parcel/accepted and confirmed/UNPAID values reconcile to permitted
  read-only source facts; no joined double counts or inferred paid/revenue/COD.
- [ ] Same captured query/snapshot drill list, pagination, expiry/drift refresh,
  status/date/week/month filters and permission-specific UI work.
- [ ] Desktop/mobile, empty/error/401/403/409/503 states, no storage/PII leaks.
- [ ] Live permission reduction/revoke invalidates access; exact replay cannot
  restore authority. Offboarding does not mutate Customer/Staff/other principals.
- [ ] Target-specific rollback/disable rehearsal and post-rollback denial verified.
- [ ] No unexpected grants/schema/data/realm/runtime mutation outside inventory.

## 7. Rollback/disable - no destructive restoration shortcut

1. Immediately stop the target's invitation sender and disable onboarding and
   Dashboard flags in its private process config; restart only owned processes
   if testing config rollback. Confirm flags actually reject new/current reads.
2. If a specific Admin credential/authority is suspect, use approved terminal
   revoke with current expected version; verify sessions/proofs/grants and replay.
3. For global dedicated-target shutdown, stop owned Backend/Frontend and verify
   ports no longer serve requests. Preserve audit, database, volume and backup.
   Dashboard flag-off alone does not stop existing IAM/SYSTEM authentication.
4. If optional V40 is authorized, the Realm operator can terminally deactivate
   only that isolated realm with audit and session revocation. Preserve original
   activated_at and bootstrap binding; do not auto-reactivate later.
5. No SQL down migration, account deletion, grant restoration by raw SQL, Flyway
   repair or restore over the existing/shared database. A recovery restore needs
   its own verified owned target and cannot be claimed done from pg_restore --list.

Owned source fixtures verify terminal revoke/environment disable/realm disable
and V40 DDL rollback without repair. Real-target rollback rehearsal:
**NOT RUN - exact deployment target not approved**.

## 8. Evidence and final decision

Publish safe outcome counts/IDs/config flags/checksums and source snapshot only;
raw logs, mailbox details, tokens, private keys and dumps remain ACL-protected.
Separate source/test results from real onboarding, SMTP inbox receipt and UAT.
Only after all applicable real gates pass declare ADMIN_DASHBOARD_LOCAL_UAT_READY.
Otherwise retain precise BLOCKED dependencies from
[the coverage matrix](admin-local-functional-coverage.csv).
