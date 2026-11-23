# Admin isolated Local/Test deployment execution

Owner execution approval: exact manifest SHA256
`9e0773d7d18bb281e82c06d7efe28cd03d457018561f708dcf1971210ddf916e`;
deployment `e9e6c0e4-8266-4c54-88b8-1bfc7ed3f3d0`.
Scope: only127.0.0.1:5543/argopost_admin_uat_15f3f2a22479 and the named recovery DB.
Manifest bytes remain unchanged; original pending-approval fields are historical.

Current checkpoint: **ADMIN_DASHBOARD_LOCAL_UAT_READY**.
Acceptance scope: approved isolated Local/Test target, genuine Owner onboarding
and authentication, real empty canonical cohort; no Production/full-suite/CI claim.
Automatic review is available again; the prior failure/blocker sections are
retained history and superseded by this resume record. Deployment approval remains
valid; no repeat Owner approval of manifest/V39/V40/operator scope is requested.

## Resume — actual results after restoring automatic review

- Revalidated10 integrity checks and owned PID/start-time state; no source/JAR or
  historical migration drift. Bootstrap/provisioning was not rerun.
- Corrected only target-private Hikari env keys to CONNECTIONTIMEOUT and
  MAXIMUMPOOLSIZE. Normal HTTPS launch then PASS. Pool4/2000ms and JDBC PG18
  were subsequently read from the live Hikari bean by a private ApplicationReady
  listener attached to the same full BackendApplication from the approved JAR.
  The operational wrapper adds no bean/API/authority or fixture data.
- Effective runtime: database argopost_admin_uat_15f3f2a22479, role
  argopost_admin_runtime_15f3f2a22479, JDBC valid, identity/customer/postgres;
  no migrator/operator credential injection. Evidence `effective-runtime.json`.
- Explicit Flyway validate39/39 PASS;12 actual callable privilege denials PASS
  for runtime/Admin/Realm roles. All20 AON function signatures match caller matrix
  and have no PUBLIC EXECUTE. Evidence `postmigration-validate-security.json`,
  `function-acl-audit.json`.
- Root invitation SENT on first Gmail attempt, payload erased; Owner confirmed
  verification and DB shows ACTIVE/verified/credential set, one consumed proof
  and one acceptance receipt. Only IDENTITY_MANAGEMENT grant. Canonical V40/V4
  activation changed realm to ACTIVE. First-bootstrap runtime flag is now false.
- Dedicated Admin UUID `c84c9391-a433-4f8d-a09f-9d684a5ff4d4` provisioned once,
  exactly six approved READ grants. Its invitation SENT on first attempt and
  payload erased. Owner performed actual verification/password entry; persisted
  ACTIVE/verified state and consumed proof confirmed. Real SYSTEM login audit
  exists for both accounts. Root Admin-read eligibility is false.
- Browser helper initialization failed with kernel-assets path error; no automated
  GUI-helper PASS. A separate owned Playwright/Chrome profile with TLS validation
  and manual Owner login runs real software UAT, without reading password/proof/
  cookie/token. Initial harness Unicode selectors were corrupted by PowerShell
  ASCII piping; fixed as Unicode escapes, no app change. Existing profile restored
  normally through the canonical refresh cookie, without credential extraction.
- Browser baseline14/14 PASS: six APIs, same-snapshot empty drill, filters, real
  refresh, desktop/mobile overflow, no browser token storage and auth/validation/
  tamper denials. Positive APIs200; Merchant/Network422 SOURCE_UNAVAILABLE,
  tampered snapshot409, invalid date400, unauthenticated401. Actual metrics all0
  match source tables:0 Shipments/Parcels/obligations. No seeded KPI or demo data.
- Canonical temporary ADMIN_FINANCE_READ reduction committed version3. Old session
  invalidation PASS; Owner genuinely re-authenticated with five permissions and
  Finance returned403. Canonical restoration committed version4; Owner genuinely
  re-authenticated with all six READ permissions. No additional authority issued.
- Reversible rollback PASS: all four Admin flags disabled on this exact target,
  only owned Backend restarted, access denied; exact saved configuration restored,
  owned restart and canonical refresh recovered Dashboard. First-bootstrap remains
  false. Evidence `rollback-flag-off-evidence.json` and Browser results.
- 23/23 live checks PASS. Natural access-expiry probe took915039ms, returned actual
  401/SESSION_INVALID and guest state; canonical refresh then recovered Dashboard.
  Actual UI logout redirected protected route to login and API returned401.
  Completed2026-11-22T17:30:31.948Z, without fake clock, database deadline edit or
  credential extraction. An earlier probe omitted required client Ports; corrected
  test initialization, no product change. Failure evidence retained separately.

All private filenames below are relative to `.demo/admin-uat-15f3f2a22479/`.
Current finite Browser outcomes are in `live-browser-result.json`; snapshots in
`live-admin-desktop.png` and `live-admin-mobile.png` contain no auth form/password.
No source/test fixture PASS is used as real target evidence.

## End-state integrity and authority audit

- `final-authority-audit.json`: exact target realm/environment ACTIVE; two verified
  ACTIVE accounts only. Root retains one IAM grant and fails Admin admission.
  Dedicated Admin version4 has exactly the six approved READ grants. Six operator
  receipts have real audit references. Both invitation outboxes SENT with erased
  encrypted payloads.39 successful Flyway records; no extra migration.
- `final-preservation-check.json`:575/575 source fingerprints match; deployment
  manifest SHA9e0773d7... and JAR SHA7415fb29... match approved bytes. Original51
  WIP archive entries pass ZIP CRC and archive SHA01aceab2... remains unchanged.
- Repository implementation validator: PASS, ownership CONSISTENT, zero errors;
  ten preexisting broken links retained. No validator exemption added.
- Public evidence privacy scan PASS: no mailbox addresses, private keys or JWTs
  in the deployment report/runbook/coverage/approval documents. Private operational
  evidence remains ACL-protected and ignored by Git.
- Owned Backend and Frontend status both HTTPS ready. No runtime restart during
  the natural access-expiry probe. No source/JAR changes in this deployment step.
- After actual logout, read-only DB audit found four Admin sessions, all four
  revoked and zero unrevoked. Evidence `final-logout-audit.json`. Backend/Frontend
  remain HTTPS ready for the next genuine Owner login.

The dataset is empty: real empty-cohort KPI and drill-through acceptance is
verified, while positive-data aggregation remains NOT VERIFIED on this target.
Absolute8h/idle30min lifetimes retain source/test evidence; this UAT does not
shorten those deadlines or claim to have waited their full durations.

## Operational handoff and residual coverage

- Admin login: `https://localhost:5273/admin/login`; Backend HTTPS:
  `https://localhost:9443`. Services remain owned and ready; test Browser is logged
  out intentionally. Owner can sign in with the already verified dedicated Admin.
- Private launcher: `.demo/admin-uat-15f3f2a22479/launch-admin-uat.ps1` with
  `-Action status`, `-Action stop`, or `-Action start`. Stop affects only matching
  owned PID/start-time records and preserves database/volume/configuration.
- Reversible target-only flag-off/process restart rollback was actually tested
  and restored. No terminal realm/Root disable or destructive rollback on target.
- Merchant decision lifecycle, official network, advanced Carrier/SLA, collections,
  COD/settlement and queue/job telemetry remain UNAVAILABLE by canonical source.
  No fabricated zero for these sources and no UNPAID-as-revenue inference.
- Positive-data KPI aggregation remains NOT VERIFIED on this empty live target;
  relevant source/test evidence is separate. No business data was seeded for UAT.
- No outstanding observed blocker for this bounded Local UAT checkpoint. Git HEAD
  remains d7329a84822f88a1c01e70a0e23b065123860410; no staged changes, commit,
  push or merge. Existing WIP and demo/shared/Production data preserved.

## First gate and plan

- [x] Read AGENTS, accepted execution evidence, exact manifest and current runbook.
- [x] Reconcile12 integrity checks: manifest, private SQL, mailbox selection,
  source fingerprints, JAR, backup, exact container/image/volume/port, current
  physical identity and free application ports. PASS, no source drift.
- [x] SQL bytes match approved JSON (`bb9792c0...`); evidence Markdown was stale
  (`28890fbf...`) and corrected. Do not normalize/change executable SQL or manifest.
- [x] Recovery restore rehearsal before privileged bootstrap.
- [x] Exact privileged prerequisite SQL, roles and custom memberships.
- [x] Forward-only Flyway applied39 files through V40; V33 absent; all success.
- [x] Separate post-migration validate/complete callable ACL negative audit.
- [x] Physical environment/realm binding through canonical operator.
- [x] Canonical pending Initial Root provision and exact-request replay.
- [x] Source-built HTTPS deployment and actual protected SMTP delivery.
- [x] Owner genuine Initial Root verification; canonical SYSTEM activation.
- [x] Owner genuine dedicated Admin verification and login.
- [x] Real API/Browser/permissions/revocation/restoration/rollback evidence.
- [x] Natural access expiry, refresh recovery and logout evidence:23/23 PASS.
- [x] ADMIN_DASHBOARD_LOCAL_UAT_READY within the explicit acceptance scope above.

Ordinary deployment glue may compose existing engines; no new account/grant
authority or DML bypass. Credentials/proofs/keys and raw diagnostics remain in
ACL-protected private files, never Git/report/chat. No shared/demo/Production,
historical migration change, commit/push/merge. Source/test acceptance is retained;
only affected checks and real target gates are executed here.

## Integrity discrepancy resolution

Actual executable SQL SHA256 is
`bb9792c0c0250e23772e040b4b276daa2f0f39cbe91e7b0d79ca3e18a26f9d9d`,
matching the approved JSON. Its34 CRLF lines, normalized to LF for diagnosis only,
produce `28890fbf1f5951564cd80626a62b0c9d5de9fa91f03c59e6ad7550cc66ef8a65`.
Only stale Markdown evidence was corrected. SQL and manifest bytes were not edited.

## Initial deployment evidence (historical pre-verification state)

Private evidence directory: `.demo/admin-uat-15f3f2a22479/`.

| Operation | Observed result | Evidence |
|---|---|---|
| Integrity revalidation |12/12 PASS; source/JAR/mailbox/dump/physical identity match | `execution-preflight.json` |
| Recovery restore | PASS into exact recovery DB; schemas agree,0 app tables/history; original target unchanged | `recovery-result.json` |
| Privileged SQL | Approved exact bytes committed transactionally; no demo/shared mutation | `bootstrap-result.json` |
| Role audit |11 new roles,6 exact custom memberships; runtime has no owner/writer/operator membership; no custom superuser/CREATEROLE/CREATEDB/BYPASSRLS | `catalog-verification.json`, `post-migration-catalog.json` |
| Flyway | Migration job from approved JAR exit0;39 successful versions1–40 excluding33 | `migration-result.json`, `post-migration-catalog.json` |
| V39/V40 | Applied checksums -350159411 /1057813618 | `post-migration-catalog.json` |
| Physical binding | Canonical bind request equals approved deployment UUID; environment binding ACTIVE, realm remains INACTIVE | `receipt-BIND.json` |
| Initial Root | Generated Account UUID `b7d9f111-db7e-4374-a412-3b84dbe74eec`; REALM_PENDING/PENDING | `receipt-ROOT_PROVISION.json` |
| Same-key replay | Same Account UUID and committed request, replayed=true; no second provision request | repeated canonical adapter result |
| Offline composition | Private Java driver compiled against classes/libs from exact approved JAR; existing canonical adapters only; no Owner password/proof entry | `AdminUatOperator.java`, private compiled classes |

The first role-audit count assertion included three built-in pg_monitor memberships.
Catalog reconciliation identified exactly pg_read_all_settings/read_all_stats/
stat_scan_tables → pg_monitor. Corrected audit verifies that exact built-in set
separately from six custom memberships; no grant or security assertion was relaxed.

At this initial checkpoint, dedicated Admin provisioning and Root/SYSTEM activation
were pending. The resume record above supersedes that state with actual results.
No raw verification DML or fixture credentials were used. All37 historical
migration files are intact.

## Historical HTTPS startup failure (resolved in resume record)

The private launcher reuses Process-Record/Owned-Process/Stop-OwnedProcess from the
existing approved launcher, validates manifest/JAR, keeps operator/migrator secrets
out of Backend, and strips DB/SMTP/JWK configuration before Frontend creation.
The observed Backend launch terminated before readiness; Frontend startup was not
reached. Do not treat configured flags or process creation as ready.

Observed first causal failure:

`spring.datasource.hikari.data-source-properties.connecttimeout` could not bind
because the Hikari pool was already sealed. Spring reported
`The configuration of the pool is sealed once started`.
Private diagnostics: `backend-live.out.log` / `backend-live.err.log`.

The prepared env keys use extra separators within compound property names:
`SPRING_DATASOURCE_HIKARI_CONNECTION_TIMEOUT` and
`SPRING_DATASOURCE_HIKARI_MAXIMUM_POOL_SIZE`. The bounded candidate fix is to use
`SPRING_DATASOURCE_HIKARI_CONNECTIONTIMEOUT` and
`SPRING_DATASOURCE_HIKARI_MAXIMUMPOOLSIZE`, preserving the approved2000ms/4 settings.
This avoids mapping connection.timeout as a nested property. Runtime confirmation
of the diagnosis remains pending; no successful fix/rerun is claimed.

The same pending command would exclude Boot's default generated-user auto-config,
as the existing HTTPS source fixture does, retaining canonical Identity security.
It would preserve failed startup logs before retry. No app source/JAR, historical
migration, authority or security guard rewrite is proposed.

## Historical execution-control blocker (review subsequently restored)

Automatic approval review could not complete the escalated config patch/restart:
usage limit reached. The tool explicitly states that **the action was not
executed**, that this is a review failure rather than a finding that it is unsafe,
and that the review must not be bypassed. Consequently the pending config changes,
log-copy step and runtime retry in that command have not run.

No equivalent private-file edit, ACL change, alternate tool or user-executed command
is used to evade the review. Independent workspace evidence/document updates are
completed. Execution can resume when automatic review is available again; the
existing Owner deployment approval does not need repeating.

## Historical resume sequence (remaining gate tracked in current checklist)

1. Revalidate manifest/source/JAR/target physical identity, current process state,
   receipts and migration history; do not rerun bootstrap or provisioning blindly.
2. Apply the bounded runtime config correction through functioning approval review,
   retain original failure evidence and retry only owned HTTPS processes.
3. Verify effective pool, post-migration validation/callable ACLs, genuine SMTP
   delivery and safe public transport negative tests.
4. Ask Owner to open the real Root invitation and set password directly on HTTPS
   UI. If its24h proof expired, use canonical resend with a new audited request,
   preserving the original Initial Root binding; never fabricate acceptance or
   provision a replacement Root.
5. Only after genuine verification activate SYSTEM through V40/V4, then provision
   the separate Admin with exactly the six READ permissions and obtain genuine
   Owner mailbox verification/login.
6. Complete live six-API/Browser, session/permission reduction-restoration, logout
   and reversible target-only rollback acceptance. Empty canonical data does not
   prove positive-data KPI/drill-through acceptance.

The original blocked checkpoint made no live SMTP/login/UAT claim. Current real
results are recorded above. No full PostgreSQL suite or GitHub CI claim;
branch/HEAD unchanged, no Git publication.
