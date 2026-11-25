# ArgoPost Administration P0 — approved implementation execution

> Latest2026-11-24: **V42 IN_PROGRESS**. Owner-approved P0-CTX-01/P0-BC-03 implemented; former failing ownership gate now PASS. Historical SECURITY_BLOCKED evidence remains below. Final V42/P0 gates are not yet complete.


Owner approval received2026-11-23: P0 TECHNICAL APPROVAL & CONTINUOUS ENGINEERING
EXECUTION. Exact approved manifest remains byte-identical:
`90c795bd7e773b6a58d0d220056cfbc5e9a5ee95b38ae2493d5b9f75a372291f`.
This record supersedes the approval-pending execution status, not its history.

## Boundary and preservation

SOURCE/TEST only; disposable isolated PostgreSQL18 and synthetic fixtures. No
demo/shared/existing Admin UAT/Production mutation, real grants/operator membership,
mail to customers, commit/push/merge, stash/reset/clean or historical migration edit.
No Identity linking or automatic role grants. New P0 authority is limited to the
approved manifest and its bounded designs; P1–P3 remain a roadmap, not new authority.

Preflight PASS: branch feature/argopost-admin-dashboard, HEAD248d651951a1d3f70a5d15e73ccfa76b9877993d;
manifest SHA exact;39 original SQL files, V33 absent, V41–V43 unoccupied.
36 current WIP files archived with SHA256 and ZIP CRC in
`.demo/admin-p0-implementation-preflight-40f4686747e94f34b5d2dc7e4dfc51f1/`.
Prior archives and immutable approved artifacts retained.

## Implementation dependency tasks

- [x] P0-IMPL-00: approval integrity, DoR/source inventory and WIP preservation.
- [x] P0-IMPL-01 DONE: Identity command admission + current referenced subjects; focused PG18 denial/rollback/session/concurrency tests below.
- [x] P0-IMPL-02 DONE (core): V41 purpose-bound step-up and BUSINESS_CONTACT attestation/receipt; genuine loopback SMTP. Public HTTP/Customer UI integration remains P0-IMPL-05, not accepted by this core result.
- [ ] P0-IMPL-03: V42 scoped governance/operator guards, evidence and immutable submission/review.
  - [x] P0-IMPL-03.1 DONE (bounded schema/operator gate): exact offline signatures, SQL_SHA256_1 replay, ACL and forward V42 validation on disposable PG18. This excludes the failing runtime callback-ownership gate.
  - [x] P0-IMPL-03.2 DONE (tested core paths): Customer upload/read/archive, real contact consume, immutable Submit and Draft withdrawal/receipts. Full metadata/resubmission/decision acceptance remains pending.
  - [x] P0-IMPL-03.3 DONE (core): assignment/BEGIN/Request Information, resubmission and HTTP current-authority gates PASS; constructed READ/COMMAND context denial now PASS. The historical failure evidence is retained below.
  - [x] P0-IMPL-03.4 DONE (core): approved P0-BC-03 real submitted contact fact/current security-version/revocation and READY/independent rejection prerequisites PASS. This excludes V43 approval/activation.
  - [x] P0-IMPL-03.5 DONE (targeted): approved applicant requirements reader, structured policy publication and Customer submission UI transport/negative/replay gates. Full genuine Customer-to-activation browser acceptance remains pending.
  - [ ] P0-IMPL-03.6: remaining scoped event/receipt readers, evidence download UI, concurrency and full V42 integration gates.

- [ ] P0-IMPL-04: V43 atomic activation/lifecycle and Business/Team compatibility.
  - [x] P0-IMPL-04.1 DONE (targeted): atomic approval/initial Owner/minimal grant/key registry, consumed-proof receipt replay and rollback on isolated PG18; schema/ACL and original context gates PASS.
  - [x] P0-IMPL-04.2 DONE (bounded implemented paths): approved event-reader/Workflow/outbox, exact current recipient, HTTP approval/RFI and Customer withdrawal/replay, concurrent workers/bounded retries, AEAD and same SMTP transport with real loopback post-commit acceptance. Full rejection-notice/lifecycle/P0 acceptance remains separate.
  - [x] P0-IMPL-04.3 DONE (targeted): scoped Organization directory/lifecycle, canonical step-up/replay and impacted Business/Team HTTP regressions plus actual HTTPS Browser1aae0c7b20624598a24d89760c9ed7d2 PASS. Full P0 acceptance remains pending.

- [ ] P0-IMPL-05: canonical Customer/Admin HTTP workflows, review UI/directory/history.
  - [x] P0-IMPL-05.1 DONE: P0-CUST-04/05 Business Contact HTTP with actual Customer login/CSRF and owned Draft checks; Customer proof UI and submission/review remain incomplete.
- [ ] P0-IMPL-06: metadata v2/scope snapshots, Merchant Queue, Audit/Work.
  - [x] P0-IMPL-06.1 DONE: masked Shipment Oversight GET + source-backed UI; HTTP/privacy and genuine HTTPS Browser gates below.
  - [x] P0-IMPL-06.2 DONE (bounded): scoped application Audit metadata/HTTP/UI; positive Organization-domain and full E2E remain separate.
  - [x] P0-IMPL-06.3 DONE (bounded): canonical first-submission facts, approved Identity context owner verification and snapshot/scope mismatch denial.
  - [x] P0-IMPL-06.4 DONE (core only): pure metric definitions/denominators and Vietnam buckets; v2 wire integration remains pending.
- [ ] P0-IMPL-07: complete P0-T-01–15, positive-data HTTPS Browser, impacted regressions/coverage/rollback.

First task changes Identity public admin ports, Identity-owned transaction adapter
and focused tests. Reuse current SYSTEM session admission and six-code Admin policy;
do not widen READ codes or write through existing read-only callbacks. No migration
is required merely to prove command transaction admission against current V40.

## Actual evidence ledger

P0-T-01–15: NOT RUN until their required acceptance coverage is executed. No final
ARG OPOST_ADMIN_P0_SOURCE_TEST_READY or new Local UAT readiness claim at this point.

## Additional exact approval — P0-BC-01/02

Owner2026-11-23 explicitly approved both deltas in
[business-contact binding correction](admin-p0-business-contact-binding.delta.md),
SOURCE/TEST isolated only. Parent manifest bytes unchanged. The new V41 receipt
may retain one protected recipient envelope; consumeAttestation adds expectedEmail
to compare the locked submitted contact. No HTTP DTO/path/permission/table added.
Do not treat the preapproval blocker history as an existing security bypass.

## Source/test checkpoint - Identity bridge and Shipment Oversight

This is component completion, **not full P0 acceptance**. P0-T-01..15 remain
incomplete as a combined acceptance inventory. No new real-target UAT or CI PASS.
V42/V43 and the canonical Merchant submission/review/activation journey are not
implemented. Approval is retained; no repeat approval is requested for those
already approved components.

### Implemented inventory

- Identity public ports P0-PORT-01/02/03, AdminCommandContext and the two approved
  P0-BC corrections; genuine current Customer/SYSTEM contexts, purpose separation,
  deadline checks and existing configured proof/credential/rate-limit engines.
- New V41 only: three Identity tables, exact-family receipts, narrow runtime ACLs,
  immutable bindings/audit, deferred receipt checks and P0-SQL-05 boolean bridge.
  One purpose proof has one request receipt, preventing a second request key from
  rebinding a proof to another application via direct DML.
- BUSINESS_CONTACT sends through the existing protected Customer delivery worker,
  opt-in only. The encrypted pending recipient survives clearing of SMTP payload.
  Verification never creates an Identity credential; consumption compares the
  exact submitted email and joins the caller transaction. Old constructor paths
  retain disabled Business Contact behavior.
- New application binding passes through internal delivery Lease/Message overloads;
  old callers remain compatible. ArgoPost Business Contact mail links carry the
  real application/proof in a fragment, expire after15 minutes and are single-use.
  SMTP acceptance is not represented as actual inbox delivery.
- Fixed a demonstrated Customer admission defect: REQUIRES_NEW can suspend an
  admitted transaction while leaving its Java callback live. requireCurrent now
  compares physical resource holders; suspended/captured contexts deny. No new
  authentication engine, HTTP authority or Customer grant.
- P0-READ-01 GET `/api/v1/admin/shipments/{shipmentId}` and P0-PORT-06 read canonical
  Shipment/Parcel/acceptance/tracking facts under the original metric selector.
  Shared AdminMetricCohorts prevents a second contributor definition. No private
  labels, contact fields, finance writes or invented lifecycle states.
- Frontend strict detail parser, fixed-path authenticated transport and masked
  detail view. Row - detail - existing list preserves the query and selector;
  forged Parcel codes, duplicate rows, extra PII fields and unsupported events
  deny. Existing six default-v1 Dashboard projections remain unchanged.

### Commands and actual results

PostgreSQL command form (each artifact owns disposable PG18, synthetic accounts,
protected test evidence and source fingerprints):

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File scripts/ci/run-cskh-focused.ps1 -Tests 'IdentityGrantApplicationTests#<exact selected methods>' -Offline
```

Passing evidence, all with `SOURCE_UNCHANGED=True`:

| Private artifact suffix | Result | Executed coverage |
|---|---|---|
| `f44d3e60b7964fe2a24de0a86ad4fbee` |4/4 PASS|Contact real loopback SMTP/clearing/atomic consumption, request replay/limits, Admin step-up replay, command rollback|
| `af0aa06803bd4bf9baa7e807b6eef002` |7/7 PASS|Wrong app/email/proof, expiry/tamper, suspended transaction/revoked session, Contact SMTP, constructed Admin context denial, direct-DML audit denial, two existing Customer transaction regressions|
| `4c2f9481b22041aaa5a303bf9f630d7e` |8/8 PASS|Concurrent confirmation, receipt rebinding denial, V41 migration/ACL/history/idempotent migrate, step-up replay and Admin purpose/lifetime/expiry/revoke race|
| `48430f9dffff44ed85af5b8e3fd7ebc4` |3/3 PASS|Cross-account and cross-session Contact denial; legacy V40 read-only/scope lifetime; masked Oversight HTTP snapshot/privacy|
| `7a6bfa147b294ee692c0f3d460521b0a` |1 PG test PASS;6/6 Browser PASS|Actual Spring HTTPS, isolated PG18, controlled SMTP invitation, verified SYSTEM login, positive Booking/Parcel/UNPAID facts, drill-through, refresh/logout and masked Parcel/intake detail|

The last Browser safe report is under
`admin-browser-a6d4753e-3752-4c88-879f-7951c83ed64f/safe-browser-results.json`
inside that private artifact. Never publish fixture-login.json, private mail,
proofs, signing keys or TLS private material. Desktop/mobile synthetic Oversight
screenshots were inspected: content is readable, table scrolling stays inside its
container and there is no page horizontal overflow at390px.

Other actual gates:

- Maven offline `-DskipTests test-compile`: PASS.
- Maven offline `-Dtest=ArchitectureTests,ArchitectureNegativeFixtureTests,CustomerAdmissionTests,SmtpCustomerDeliveryTests,CustomerProofProtectionTests test`: **68/68 PASS** (23 architecture/negative,16 admission,18 protection,11 SMTP).
  Preserved safe reports: `.demo/admin-p0-core-regression-13c15927d010479383449466286d43f0/`.
- `npm.cmd run typecheck`, `npm.cmd run lint`, `npm.cmd run build`: PASS.
- `npm.cmd run test -- admin-dashboard.spec.ts admin-session.spec.ts internal-navigation.spec.ts`:26/26 PASS.
- `npm.cmd run test:customer-ui -- admin-dashboard.spec.ts admin-login.spec.ts`:23/23 PASS; intercepted synthetic API fixtures, not full-stack acceptance. Its required `build:customer` PASS.
- `python -m unittest scripts/ci/test_admin_role_prerequisites.py scripts/ci/test_staff_role_prerequisites.py`:5/5 PASS.
- `python docs/reconciliation/v3/validate.py --implementation`: final PASS,
  ownership CONSISTENT,0 errors after registering the new Oversight component.
  The10 old mobile-path broken links remain pre-existing and reported.

### Retained failed attempts and corrections

- `88746e03e6c945079fd1320604bf3f4a`: compile failure, duplicated test wrapper
  insertion; removed the exact duplicate, no logic/assertion weakened.
- `29314ffa658d4580962405cc335e3951`:1/2 PASS,1 FAIL; genuine SMTP exposed the old
  Business Contact branding/link missing application binding. Fixed worker message
  binding/template; subsequent real-loopback check PASS.
- `3762b86583bc4d77b46f3fa46108af45`:1 FAIL/1 ERROR; REQUIRES_NEW admission defect
  plus synthetic proof timestamp not truncated to canonical microseconds. Fixed
  the actual admission check and test fixture precision; later negative gate PASS.
- `161ceecfeb5149889a4e5dfc920d8886`: concurrent confirmation succeeded once, but
  direct-DML alias receipt was accepted. Added purpose-specific unique indexes in
  unreleased V41; later SQLSTATE23505 denial PASS.
- Initial frontend TDD failed because parseShipmentOversight was absent; parser
  implementation then passed positive/negative cases. `npm.ps1` was blocked by
  Windows execution policy; used normal npm.cmd, without global policy changes.
- Initial source validator found the undeclared new UI file. Registered it in the
  implementation scope and reran to CONSISTENT/0 errors.

### Preservation and remaining dependency

39 historical migrations retain their preflight SHA256. Approved manifest SHA
remains exact. Pre-implementation WIP ZIP SHA and CRC rechecked PASS. V41 SHA256:
`aa78854d594c8ed228b88570bfae62f7c3c590dbdac663d96beb59d53359fed6`.
No shared/demo/UAT migration, actual account/grant, SYSTEM activation, Internet
mail, credentials, worktree, commit/push/merge or destructive cleanup.

P0-IMPL-03 is next. The exact conflict for offline operator receipt hashing is
isolated in [P0-OP-01](admin-p0-offline-receipt-digest.delta.md); Owner has now
approved that exact delta for isolated source/test. No substitute key, secret GUC or unapproved SQL authority
was introduced. Other P0 dependencies remain implementation work, not additional
business approval requests. Full P0 HTTPS Customer - approval - Organization E2E
and metadata-v2 acceptance remain NOT RUN.


## Additional exact approval - P0-OP-01

Owner explicitly replied: "Duyet P0-OP-01, chi source/test co lap" (approval received
in the active session2026-11-23). The exact proposed
[offline digest correction](admin-p0-offline-receipt-digest.delta.md) is now
approved for implementation. Preserve its proposal history and the parent
manifest. Only OPERATOR receipts use SQL_SHA256_1 and SQL-computed canonical
SHA256; CUSTOMER/SYSTEM receipts retain configured-key-ring HMAC. No signature,
table, role, privilege, demo target or deployment scope expansion. The V42
operator dependency is no longer waiting for approval; it remains implementation
work. Do not request this same approval again.


## Business Contact HTTP checkpoint

P0-CUST-04/05 are implemented in CustomerBusinessContactHttp, using only public
Identity/Customer and Merchant APIs. MerchantApplications.get checks applicant
ownership before proof issuance/confirmation; only editable DRAFT or
NEEDS_INFORMATION is admitted. Existing Customer guard enforces genuine bearer,
HTTPS, exact Origin and session-bound CSRF. Exact JSON/Idempotency-Key validation
rejects duplicate/extra fields, query proofs, malformed/noncanonical UUIDs and
unknown content. No raw proof in response/receipt. Rate-limit failures cross the
module boundary as a finite public Limited exception and return429/Retry-After.

ProductionCustomerComposition preserves its old disabled overloads. The opt-in
flag remains false by default; enabling requires customer+identity+postgres,
SANDBOX and LOCAL/TEST, rejects production/prod/demo/migration, then verifies the
physical environment and V41 catalog. No flag or target configuration was enabled.
This is not Customer Browser proof-page completion or application submission.

TDD compile was RED before controller/composition existed. Actual source/test:

- `.demo/cskh-focused-c730f0259e12470298f93651b6c3a667/`:2/2 PG18 PASS,
  genuine Customer HTTP/login/owned Draft/contact mail verification/replay and
  durable quota. SOURCE_UNCHANGED=True.
- `.demo/cskh-focused-cc4ce079d78546a3a4b4f4e342ad0492/`:2/2 PG18 PASS after
  adding duplicate JSON, CSRF denial, cross-owner404, quota429/Retry-After and
  physical V41 startup check. SOURCE_UNCHANGED=True.
- Maven offline `-Dtest=ArchitectureTests,ArchitectureNegativeFixtureTests,BusinessContactConfigurationTests test`:26/26 PASS (23 architecture plus3 configuration-isolation cases).
- Source validator after registration of both new Java files: CONSISTENT,0 errors.

These are incremental gates, not26 additional distinct architecture tests and
not a full PostgreSQL/CI acceptance claim. V42/V43, evidence upload and immutable
submitted revisions, review/decision/activation and metadata-v2 remain pending.
P0-OP-01 approval is already recorded above and does not need to be repeated.


Final impacted legacy Customer regression:
`.demo/cskh-focused-4b5997607b40417ba538944a00a7b6b2/`2/2 PASS,
SOURCE_UNCHANGED=True. Exact selected methods:
`customerHttpEnrollmentMailVerificationLoginRefreshLogoutUseActualChains` and
`profileHttpUpdatePreservesIdentityAndReturnsOriginalVersionOnReceiptReplay`.
Existing disabled composition keeps enrollment/auth/profile behavior; no broad
manual suite repeated. Coverage CSV remains23 rows/28 columns and unchanged
classifications; updated ADM-01/02/03/08 component evidence and approval status,
with no module promoted to fully complete.


## V42 continuation - scoped authority and workflow implementation

Owner explicitly authorized continued V42 followed by V43, retaining P0-BC-01/02
and P0-OP-01. No repeat approval requested. New preflight archive:
`.demo/admin-p0-v42-preflight-3d8401d449414bf3bd3592df0019b453/` preserves77 WIP
paths and40 existing migration files, including accepted V41. Manifest and HEAD
remain unchanged. V42 was unoccupied and is source/test only.

Current implementation: exact two offline operator signatures; typed published
policy/consent and scoped authority, SQL_SHA256_1 operator receipts, current
policy/scope callback and privilege ceilings; nine V42 tables; forward application
state/revision fields, versioned guard, immutable revisions and retained evidence.
Draft projections now read actual state/revision and keep historical Draft receipts.
No APPROVED/ACTIVE creation is enabled by V42; V43 is not implemented yet.

Observed gates (incremental, not full P0 acceptance):

- `.demo/cskh-focused-33a30e6c03a14d2ca0e3090425e59663/`: expected TDD RED,
  missing migration target42; retained, not PASS.
- `.demo/cskh-focused-5acb644e0ddb455ba32c20a5a33c18f3/`:1/1 PASS,
  initial V42 schema/ACL/operator receipts; SOURCE_UNCHANGED=True.
- `.demo/cskh-focused-443f56afd24f40a6937d803ff3f9c8dc/`:2/2 PASS,
  current scope/revocation/expiry, changed authority fingerprint, wrong target,
  callback lifetime and suspended physical transaction denial plus operator gates.
- `.demo/cskh-focused-e71d0fa7dbbf4cb1a647c52fafefa94f/`:2 ERROR,
  first cause PL/pgSQL CASE expression syntax at V42 line114. Parenthesized CASE;
  no assertion relaxed.
- `.demo/cskh-focused-e1de9c0fe8034359befe288863907f55/`:3/3 PASS,
  above gates and actual Customer Draft create/update/historical replay;
  incomplete submission and direct version/history write without receipt rejected.
- First genuine private-evidence/contact/submission test attempt in
  `.demo/cskh-focused-f3c2c60edd61429e87ef27c5bba496f3/`:1 ERROR,
  SELECT FOR SHARE required UPDATE on runtime-read-only policy. Removed that row
  lock, retaining the existing Identity population and Merchant control locks
  shared with both operator functions. No runtime policy UPDATE granted.

Automatic review rejected an intermediate proposal for broad writer SELECT and
trigger ownership. It was NOT applied. Accepted safer implementation keeps the
workflow commit trigger SECURITY INVOKER with the caller's existing Merchant
reads, and a separate private SECURITY DEFINER trigger invokes only the already
approved Identity boolean eligibility predicate. No new writer data privileges,
PUBLIC/runtime EXECUTE or operator membership were introduced by that correction.
Operator record guards remain SECURITY INVOKER and preserve current_user checks.

Private upload/submission integration and remaining V42 review/HTTP/UI gates are
still being implemented/tested. The ClamAV fixture is controlled INSTREAM test
transport with real MIME/content validation; it is not live antivirus certification.
Full V42, V43, all P0-T-01..15 and P0 HTTPS E2E remain incomplete. No full-suite,
GitHub CI, demo activation or real-target acceptance is claimed.


## Latest V42 source/test checkpoint -2026-11-24

**SECURITY_BLOCKED for V42 completion; no P0 readiness claim.** New Governance
HTTP/decision exposure and V43 wait for the failed security gate. Two exact deltas
are proposed; neither is implemented or assumed approved:

- [P0-CTX-01](admin-p0-context-ownership.delta.md): two owner-verification methods
  on existing Identity admission ports. Merchant currently trusts a forwarding
  requireCurrent while reading a substituted accountId. The private Identity
  issued-context registry must not be imported/reflected into Merchant.
- [P0-BC-03](admin-p0-submitted-contact-fact.delta.md): read-only submitted contact
  fact through the existing Identity port. Preserve void consume and V41 bytes;
  obtain real verifiedAt and check revocation/security version after submission,
  without inferring timestamps or reading private Identity tables from Merchant.

Asynchronous Owner questions reference the exact bounded documents. Existing P0,
P0-BC-01/02 and P0-OP-01 approvals remain valid and are not requested again.

### Implemented source and bounded positive evidence

V42 includes nine approved tables, forward state/revision fields and versioned
guard, immutable submitted revisions/retained evidence, exact working history for
every application version, scoped operator publication/grants, typed receipt/audit
binding and reviewer assignment guards. Runtime has no policy/scope DML or operator
EXECUTE. Narrow private Identity eligibility triggers use the existing boolean;
no additional writer SELECT of applicant PII was granted.

JdbcMerchantApplicationWorkflow uses actual CustomerAdmission and existing contact
consume. Upload reserves quota in one admission, performs private file I/O/inspection
outside it, verifies read-back integrity and re-admits before finalization. Same-key
input/target is HMAC-bound. Archive retains bytes; missing/foreign/expired evidence
never becomes CLEAN. Submit preserves full legal name and a separate display name.
Never-submitted Draft withdrawal is the sole typed CUSTOMER/WITHDRAW null-policy
event branch. No Organization/Owner activation is introduced.

JdbcMerchantApplicationReviews implements current scoped assignment, BEGIN and
Request Information, eligible locked applicant/assignee, known conflict/independence,
ETag and grant revocation checks. Review body is protected with retained configured
AES-GCM keys, bound to event/application/revision/op. Replay returns the original
committed state/version after later state changes. READY remains unavailable;
there is no fake contact fact. Positive-path evidence is not security acceptance
because P0-CTX-01 failed.

Approved applicant routes now cover submit/resubmit/withdrawal, evidence upload,
metadata/private content/archive and additive finite receipt lookup. Draft wire and
historical outcomes are retained. The default legacy composition keeps new writes
disabled. Workflow implementations are source/test compositions; production wiring,
full Customer/Admin workflows and genuine full P0 HTTPS Browser remain pending.

Frontend captures the Business Contact fragment before the general proof scrubber,
strips it before render/network and requires explicit confirmation. Lost Draft-save
response retries exact key/body/ETag, without another fresh write. No proof in DOM,
router/localStorage/sessionStorage. NEEDS_INFORMATION edits its working Draft.
The approved six-field Application shape does not imply Business Membership.
Registration/recovery and Membership-driven switching semantics remain unchanged.

### Exact verification and retained failures

PG command: `scripts/ci/run-cskh-focused.ps1 -Tests <selected methods> -Offline`.
All databases/roles/mailboxes/private files below are disposable synthetic fixtures.

- `.demo/cskh-focused-c0bfba37c7cc4eab8d4a712d879804d9/`:1 ERROR,
  audit INSERT had one excess placeholder; corrected, no weakened assertion.
- `.demo/cskh-focused-8f9367d53942404ebf9614bc746fa3d9/`:1/1 PASS,
  actual private bytes/inspection, genuine loopback SMTP, contact consume, immutable
  Submit/replay and full legal name; no Organization/Membership created.
- `.demo/cskh-focused-49e849ba633741d5bd77baae94338c1e/`:5/5 PASS,
  schema/operator, current scope, Draft compatibility, Submit, Customer HTTP/CSRF/
  ownership/private download/strict payload denial.
- `.demo/cskh-focused-656fad3bdfa543bc90596bd47587d88a/`:1/1 PASS,
  independent assignment/BEGIN/Request Information, encrypted audit, historical
  result replay and revoked scope denial.
- `.demo/cskh-focused-09a39ba4f9ca4c7b83ddd14d4a4cf5fe/`:8/8 PASS,
  six V42 gates plus legacy Application HTTP paging/replay and foreign-target
  denial; extended archive/withdraw/receipt HTTP gates PASS. SOURCE_UNCHANGED=True.
  Foreign Draft test setup now uses real Customer HTTP creation, not raw DML
  lacking the new required receipt. Both successful creates' audits are asserted.
- `.demo/cskh-focused-4263911b3b4c47aaa5fc161ec93aa9c5/`:**1 FAIL**,
  merchantGovernanceRejectsConstructedIdentityContexts. Two failed assertions:
  substituted READ and COMMAND wrappers each expected AdminFailure but neither
  threw. SOURCE_UNCHANGED=True. Synthetic policy reads only; no forged write.
  This failure is retained and blocks readiness; it is not rerun to hide a defect.
- Offline Maven architecture/negative fixtures/payload protection:24/24 PASS;
  final bounded validation cleanup rerun is recorded below when complete.
- Frontend typecheck/lint/build:customer PASS. Initial JSX/unused import errors
  corrected; existing Vite large-chunk advisory remains and was not suppressed.
- Frontend targeted unit tests24/24 PASS; UI Browser units38/38 PASS including
  lost-save retry, URL/storage privacy and existing Personal/auth/responsive flows.
  These intercept API fixtures and are NOT real HTTPS/PG E2E evidence.

ClamAV is a controlled INSTREAM test daemon plus real MIME/content validation,
not live antivirus certification. No full PostgreSQL suite, GitHub CI, live Gmail/
S3, full P0 Browser or real-target acceptance PASS is claimed. V42 resubmission/
concurrency/full decisions, V43 activation/lifecycle, metadata v2/queue/audit/work
and all combined P0-T-01..15 remain incomplete. All runtime flags are unchanged/off.

### Inventory and preservation

New Merchant contracts/adapters: MerchantGovernance, GovernanceFailure,
JdbcMerchantGovernance, JdbcMerchantApplicationWorkflow, JdbcMerchantApplicationReviews,
ProtectedGovernancePayloads; applicant HTTP and V42 SQL. Modified Draft projection/
store/service, finite receipt/error mapping and focused fixtures/tests. Added
BusinessContact UI/client/proof capture and privacy/retry tests. Scope/traceability
and module evidence are updated without promoting any module to full completion.
No V43 file exists. No demo/shared/Admin-UAT/Production, historical SQL, credentials,
real grant/operator or Git publication was changed. Final fingerprints follow.

### Final continuation verification2026-11-24

- Final offline Maven reports confirm ArchitectureTests11/11,
  ArchitectureNegativeFixtureTests12/12 and ProtectedGovernancePayloadsTests1/1:
  **24 tests,0 failures,0 errors,0 skipped** after the bounded null/error cleanup.
  The earlier execution process handle expired; the persisted Surefire XML is the
  evidence, not an inferred process exit. No PostgreSQL security failure is hidden
  by these separate unit/architecture results.
- `git diff --check` PASS. `python docs/reconciliation/v3/validate.py --implementation`
  completed with errors=[] and semantic ownership status recorded in
  `.demo/admin-p0-v42-validator-latest.json`; existing link debt remains separately
  reported by that validator and is not reclassified as fixed.
- All40 SQL files present at the V42 preflight remain SHA-256 identical, including
  V41 `aa78854d594c8ed228b88570bfae62f7c3c590dbdac663d96beb59d53359fed6`.
  V42 is the only additional migration; V43 remains absent.
- Parent approved manifest still hashes to
  `90c795bd7e773b6a58d0d220056cfbc5e9a5ee95b38ae2493d5b9f75a372291f`.
  Preflight77-file archive retains its recorded SHA-256 and passes ZIP CRC.
- Branch/HEAD remain feature/argopost-admin-dashboard /
  `248d651951a1d3f70a5d15e73ccfa76b9877993d`; no staged changes or publication.
  Source fingerprints and safe test counts are captured in
  `.demo/admin-p0-v42-continuation-checkpoint.json` without credentials/proofs.

Disposition remains **SECURITY_BLOCKED**, not V42/P0 SOURCE_TEST_READY.
The single next decision is Owner approval of the two exact bounded deltas
P0-CTX-01 and P0-BC-03. No repeated approval of V42, BC-01/02 or OP-01 is sought.
Their proposed public-port additions are not implemented. No V43 activation or new
Governance HTTP exposure proceeds while the current negative security gate FAILS.


## Owner approval2026-11-24: P0-CTX-01 + P0-BC-03

Both exact bounded deltas approved for isolated SOURCE/TEST. Parent manifest remains unchanged. Historical SECURITY_BLOCKED and its failing evidence remain above. Remediation order: exact Identity context ownership, authoritative submitted-contact fact, then V42 review/decision dependencies and V43. No deployment, real grants/mail, historical migration edits or Git publication. New preservation archive: `.demo/admin-p0-approved-remediation-5456bd89d8884eff865983bc42162147/`.


### Approved remediation evidence2026-11-24

P0-CTX-01 implemented: default-deny owner verification on existing AdminAdmission
and AdminCommandAdmission; exact Identity issued object and DataSource validation,
read callback physical resource binding, finally cleanup, Merchant verification
before actor/scope reads. No private cross-module imports/reflection/class-name
workaround. Original constructed-context denial assertions remain unchanged; only
its infrastructure constructor wiring now supplies the required owner ports.
A separate real-context positive gate prevents a default-deny/vacuous result.

- `.demo/cskh-focused-3f332505a6b94fc5b4e7c2b33e9ca4f7/`:4/4 PASS,
  sourceUnchanged=true: original READ+COMMAND wrapper denial, cross-DataSource,
  escaped/reused callback, identical-isolation REQUIRES_NEW suspension, legitimate
  context paths, current scope revoke/expiry and existing review flow. The filter
  name for the separate command-expiry gate did not match its wrapper in this run;
  that test is counted only in the next five-test artifact, not here.
- `.demo/cskh-focused-e38eaa9d58fd4d9ea30e7b8d88004e39/`:5/5 PASS,
  sourceUnchanged=true: actual authoritative consumed fact, rollback, exact bindings,
  wrong revision/application/email, unconsumed/revoked denial, Customer/Admin
  suspension/forwarded contexts and unknown referenced applicant denial, retained
  historical fact after proof TTL/original-session logout, security-version change,
  no new mail/receipt/audit; command revocation/expiry and existing contact regressions.
- `.demo/cskh-focused-b2a71ae865934a43b65f591f03462d06/`:3 PASS/1 ERROR,
  sourceUnchanged=true. JSONB from queryForMap is PGobject, not String; fixed the
  projection via its textual value without changing assertions or business policy.
- `.demo/cskh-focused-f8cb9faf344d488f8d04a7474e2325a9/`:affected test1/1 PASS,
  sourceUnchanged=true. Real Request Information -> fresh contact -> Draft update ->
  resubmit revision2 -> BEGIN -> complete READY checklist, immutable revision1,
  protected actual verifiedAt fact, digest and original-key replay; incomplete
  checklist denied. No Organization, Membership or activation is created.

P0-BC-03 keeps void consumeAttestation and V41 bytes. The two approved read overloads
are Identity-only, check eligible current subject/security version and exact used
binding, and have no read side effect. Merchant seals verifiedAt/id/application/
revision in the existing protected submission event and includes the fact in its
content digest. Human command receipt HMAC remains bound to the client command;
immutable event details are not smuggled into its input digest or replay input.

V42 is still IN_PROGRESS: these passes resolve the observed context defect and
contact reader gap, not the full V42/P0 gate set. Full decisions, V43 atomic
activation, notice outbox, complete UI, v2 metrics/queue/audit/work, concurrency and
real full HTTPS Browser remain incomplete. New bounded disclosure/HTTP tests are
running; no result is claimed in this paragraph. Historical failure evidence stays.


### V42 continuation: disclosure and independent rejection

- `.demo/cskh-focused-121f5312a03e490bb2cd17680f69571a/`:2/2 PASS,
  sourceUnchanged=true. Separate application.evidence.read controls full immutable
  review-data; purpose is finite, each successful disclosure writes an append-only
  event at unchanged application version. Actual HTTPS-marked MockMvc requests use
  genuine bearer/Identity request admission and PG18: masked summary/no-store,
  authorized review-data,403 without evidence authority,428 missing If-Match,
  412 stale ETag,401 POST missing Origin,400 unexpected query. This is HTTP integration,
  not actual TLS Browser acceptance. Controller is explicit opt-in/default-off.
- `.demo/cskh-focused-41e824b228c6440b845ead0e91501111/`:3/3 PASS,
  sourceUnchanged=true. Independent dedicated decider with current domain scope,
  current eligible reviewer, exact latest revision-bound READY event, revalidated
  contact/evidence/content digest and canonical credential step-up rejects once.
  Reviewer cannot self-decide; rollback leaves state and proof unconsumed; committed
  same-key replay returns original REJECTED version despite stale If-Match/used proof.
  V42 schema/operator/ACL and original wrapper denial gates also PASS.

The private existing event eligibility trigger validates rejection against the
existing Identity-owned admin_governance_command_bound boolean. A transaction-local
proof descriptor supplies the opaque proof ID/authority/digest; it is not authority
and cannot pass without a genuinely consumed proof bound to the same actor/request/
action/target/version/digest. It is reset after the event insert, not persisted as
new schema. No new SQL signature, grants, Identity SELECT or writer membership.
The deferred Merchant invariant requires the independent current READY event at
exact prior version. Runtime has no execution of the private trigger/Identity bool.
Source V42 has not been applied to any real database; V1-V41 remain untouched.

Current queue/context/revision projection tests are pending in their own fresh
artifact. No final V42/P0 readiness or V43 execution is claimed yet.


### Continuation after automatic-review availability interruption

The first attempt to read the assertion-only XML was not executed because automatic
approval review reported a usage-limit availability error. No alternate tool was
used to bypass that gate. The identical protected read was retried successfully
through automatic review after the Owner's continue instruction.

`.demo/cskh-focused-3bf16b11c9984464bc79b18420d0bd40/` retains **34 PASS/1 FAIL**,
sourceUnchanged=true. First causal assertion: candidate API must return403 for a
reviewer lacking assignment permission, but returned503. JdbcAdminAdmission's
outer generic catch was wrapping the Merchant callback failure. In-scope fix uses
an Identity-private callback exception carrier to preserve domain-owned failure;
DataAccess/SQL/unavailable failures remain finite503. No domain-internal import,
permission, admission or SQL privilege changed. Assertion403 is unchanged.
Affected HTTP/read-only/permission regressions and genuine TLS reviewer flow will
be recorded only after executing the next test run.

Earlier candidate fixture failure `.demo/cskh-focused-e18d06db67ee4108babfe1b6c5342c7d/`
was an incorrect new expected count: the existing synthetic assigner also had an
explicit application.review grant, so two eligible candidates are correct. The
assertion now requires the exact two UUIDs and both one-item pages/hasNext without
duplicates; no eligible candidate was hidden and no backend rule was weakened.

Frontend latest:17/17 transport/parser units PASS; typecheck/lint PASS before TLS
fixture additions.18/18 Dashboard UI units PASS before final scoped form styling.
The latest25-test UI run's process handle expired before its final output was read;
its total is NOT claimed as PASS here. Screenshot visual QA identified and fixed
checkbox/form layout and redundant global refresh for the new module. No intercepted
UI test is counted as Backend/TLS acceptance. Current build/lint and actual TLS
fixture gates remain pending until their results are observed.

### 2026-11-24 — approved applicant policy reader continuation

Owner approved P0-POL-UX-01: “Duyệt delta source/test cô lập”. Parent manifest
remains SHA-256 90c795bd7e773b6a58d0d220056cfbc5e9a5ee95b38ae2493d5b9f75a372291f.
The old publication `consentText` already exists in protected PUBLISH_POLICY event
payloads and remains preserved. The approved delta adds two structured statements
in the unapplied V42 policy plus the self-owned requirements reader. It does not
publish real policy data or approve Terms/Privacy.

Previously pending results now observed:
- `.demo/cskh-focused-b9c4745e7fc4420fb0f2843a735f00d2/`:5 PASS/1 FAIL.
  Candidate403 and five affected backend authorization/regression gates PASS.
  The HTTPS test's subsequent foreign-resource request raced pending UI refresh
  admissions and received intentional503; expected404 was retained.
- `.demo/cskh-focused-14aeae57e3ae48039c7dfb1ca553458f/`:1/1 PASS,
  sourceUnchanged=true. Genuine HTTPS reviewer browser flow PASS after explicitly
  awaiting UI network idle before the separate direct API negative request. No
  interception, auth bypass or assertion reduction. This does not establish full
  Customer-to-Organization activation E2E.

Current isolated focused run for requirements/schema/HTTP/submission/review:
`.demo/cskh-focused-25ec1f57867c4c2fbc9d0ce6495cdf28/` — IN_PROGRESS.
No pending run is counted PASS. Historical failure artifacts remain intact.

### 2026-11-24 — Docker restored; applicant requirements source/test evidence

Docker Engine read-only version29.6.2 responded after Owner restart. The interrupted
25ec1f57867c4c2fbc9d0ce6495cdf28 run ended Maven1, zero fresh completed test cases,
sourceUnchanged=true. It is infrastructure-interrupted, NOT a test PASS. No Docker
volume/database was removed and no live application process was stopped.

Fresh isolated PG18 artifact `.demo/cskh-focused-c148f677979a4b66aba768676126abce/`:
**4/4 PASS, Maven0, sourceUnchanged=true**. Exact cases:
- merchantGovernanceV42SchemaAndOperatorReceipts: V42 migration, malformed/null/
  extra/empty/oversized/control/non-NFC attestation statements, no partial receipt,
  canonical publication replay and same-key changed wording conflict.
- merchantV42ScreenedEvidenceAndImmutableSubmission: real scanner/private-store
  flow, policy requirements with exact statements/version and immutable submission.
- merchantV42HttpEvidenceAndSubmissionNegatives: Customer admission/ownership,
  requirements exact DTO/no-store, foreign404, unexpected query400, incomplete
  selection422, evidence/CSRF/strict body and existing withdrawal regressions.
- merchantV42IndependentAssignmentReviewAndInformationRequest: legitimate scoped
  review/resubmit/fact binding/rejection/receipt and current-authority regressions.

Task checkpoints (only this bounded increment):
- [x] Approved P0-POL-UX-01 Backend/schema/HTTP targeted gates DONE.
- [x] Customer transport/parser focused gates DONE:9/9 PASS, including old contact
  regressions. Four new tests first observed RED before implementation.
- [x] Customer UI integration targeted gates DONE:3/3 intercepted UI units PASS;
  explicit version7 statements/two unchecked consents, unknown submission same-key
  retry, canonical reread, no-policy fail-closed and contact-proof regression.
- [x] Frontend typecheck/lint/customer build PASS on the applicant UI implementation.
  Existing >500kB chunk advisory remains visible, not suppressed.
- [ ] Genuine Customer browser submission-to-activation full P0 acceptance NOT_DONE.

UI uses private multipart through the existing Customer token/CSRF/session engine;
no filename, proof, token or legal data goes into browser persistent storage.
Draft preparation, evidence association and submit/resubmit are explicit versioned
commands. A committed response alone never grants Business access. The full legal
name remains intact and activation display name is independently bounded160.

P0-ID-API-01 / P0-GOV-09 HTTP continuation is implemented for REJECT only so far,
using existing canonical proof engine and Merchant current scoped authority. Origin
is mandatory, server-generated request identity is used, inputs are finite, errors
contain no password or protected body. APPROVE/Organization lifecycle remain
unavailable pending V43. No privilege/SQL signature or historical migration changed.
Focused artifact ebce9e1574554dc2a983598bcf8b73ca is pending; not counted PASS.

Preservation check: original103-entry WIP archive SHA and ZIP CRC both PASS.
Parent manifest remains byte-identical. No demo/UAT/Production mutation, real mail,
operator/grant change or commit/push/merge occurred in this continuation.

### Independent decision HTTP/UI and genuine HTTPS acceptance

- ebce9e1574554dc2a983598bcf8b73ca:1 PASS/1 FAIL. The new rollback fixture used
  a different decider session from the HTTP step-up issuer. Identity correctly
  rejected the proof. Fixed fixture uses the same genuine HTTP session and adds an
  explicit same-account/different-session negative assertion; no proof guard change.
- 6d8bbc0586984fb084976a65b2929ce0:1/1 PASS, sourceUnchanged=true: step-up Origin,
  capability denial, exact receipt replay, session binding, rollback, independent
  rejection HTTP, stale ETag/consumed-proof replay and no Organization activation.
- Frontend18/18 session/parser units PASS and3/3 targeted Merchant UI units PASS:
  real policy metadata, explicit review checklist, independent reauthentication,
  lost response same-key retry without retaining a credential and canonical reread.
- 8dff83dd5ef14a41bf25c152ec35452e:**26/26 PASS**, sourceUnchanged=true.
  Two genuine HTTPS Browser+Spring+PG18 fixtures: reviewer BEGIN/checklist and
  independent Admin step-up/rejection, plus23 architecture positive/negative tests
  and one protected-payload regression. Test fixtures use synthetic principals,
  pinned ephemeral TLS, private memory-only credentials and test mail transport.
  No API interception or Owner authentication is used in those runtime fixtures.

Scoped audit/receipt readers then added from approved P0-GOV-11/P0-GOV-17:
- 23bd3df2c0bd4806ad1783d2b0184953:compiler FAIL, zero tests. A duplicate newly
  declared ActivationOutcome caused cascading symbols. Existing approved DTO was
  reused and the duplicate addition removed; no contract/schema change.
- ca16829ae5a24d6d8284614574a8ca8d:**2/2 PASS**, sourceUnchanged=true.
  Scoped audit pagination, metadata-only privacy, own actor receipt/version,
  foreign actor404, revoked authority denial and original constructed-context
  READ/COMMAND assertions all PASS. Receipt metadata does not authenticate a new
  command or expose protected cached payload.
- Audit frontend parser5/5 PASS; targeted decider+audit UI1/1 PASS.

All40 historical V1-V41 files match preserved hashes (V33 absent); parent manifest
SHA remains exact. Source manifest validator PASS: errors=[], semantic ownership
CONSISTENT;10 pre-existing shipper-mobile links remain listed, not silently fixed.

Private Admin evidence download P0-GOV-12 implementation/testing is IN_PROGRESS
in isolated artifact e29fee5402dd4c9399cb3e342a0f9b1e. It uses the same private
store and scans/snapshot facts, no provider change. Current authority/facts are
checked before and after external byte I/O, with bounded SHA/size verification;
no DB transaction is held during storage reads. No pending test is counted PASS.

### Scoped history UI and private-byte regression continuation

Audit parser5/5 unit tests PASS; decider+audit UI1/1 PASS, including an explicit
metadata-only history request and exact-key rejection retry. History is cleared
when the selected application/committed decision changes, rather than presenting
old metadata as current. Source paths are inventoried in implementation-scope.

Private evidence artifact e29fee5402dd4c9399cb3e342a0f9b1e:1 PASS/1 FAIL,
sourceUnchanged=true. Expected reviewer HTTP200 returned403 because the new
byte-I/O coordinator test instance was wired to the assigner's fixed test admission.
It correctly denied the assigner, who lacks evidence permission. Fixture now uses
the actual HTTP request admission/command port; existing static owner verification
and all authorization assertions are unchanged. Focused rerun
`.demo/cskh-focused-d5e2138a079544a28a32a0714c6dce58/` is pending.
No failure is removed or relabeled PASS. V42 remains IN_PROGRESS.


### V42 final related security-gate continuation before V43

- d5e2138a079544a28a32a0714c6dce58:2/2 PASS, sourceUnchanged=true;
  actual HTTP admission now drives private byte I/O; reviewer200, assigner403,
  purpose400, unknown evidence404, privacy/attachment/MIME and integrity validated.
- 572f6f9b48b04a43a68ddb229987ceb8:2/2 PASS, sourceUnchanged=true;
  an actual canonical operator revokes the reviewer while the private store reads
  outside any DB transaction. The second admission denies404 and returns JSON,
  never private bytes. Original constructed READ/COMMAND contexts still denied.
- Frontend21/21 focused session/governance parser units PASS, including private
  bounded attachment transport, HTML/header/size denial. Typecheck/lint PASS.
- e7c635fe5be8425bbc0223869a20555c:27/27 PASS, sourceUnchanged=true;
  opt-in composition security/key-retention tests plus23 architecture and one
  protected-payload test. New composition reuses actual current request admissions,
  configured retained key rings and the canonical private store/scanner. Applicant
  workflow does not require or activate Admin authentication. No local flag changed.
- 12e89e0efc624e25a752fda556759805:2 PASS/1 FAIL. A new concurrent HTTP assertion
  expected both requests200, but existing read admission deliberately try-locks
  and returns503 under a concurrent population writer. No lock or timeout changed.
- 51f73103871341f79bdbd04343a4fde0:3/3 PASS, sourceUnchanged=true. Concurrent
  same-key review returns one outcome; concurrent decision admits200 or bounded503,
  requires at least one200, then requires an explicit same-key/body/ETag retry200
  equal to the committed result. One decision/receipt/proof only; rollback and all
  previous negative assertions remain. Two canonical private-store configuration
  tests prove Merchant storage does not enable Claims or Staff, retains Claim
  prerequisites and rejects Production/default-disabled operation.

Owner approved P0-NOTICE-BIND-01 for isolated source/test. Approval recorded in its
bounded delta, parent manifest unchanged. V43 activation/outbox can now proceed
behind the passing related V42 security gates. Full P0 acceptance is not claimed.


### Owner-approved notice SMTP bridge and V43 TDD continuation (2026-11-24)

P0-NOTICE-DELIVERY-01 is Owner-approved, isolated source/test only. The exact
Platform port is implemented by the existing configured SmtpCustomerDelivery;
Customer composition exposes the same instance, not a second sender/configuration.
No runtime flags or real SMTP settings were changed. Dispatch/outbox integration
and its mandatory negative/rollback gates are still IN_PROGRESS.

- b0659c57de2d4cbebec342aad19d4c58: TDD RED, compiler missing new public port.
- 8eca9b506ba84ec6baccfd04d5865f22:12 PASS/1 FAIL/1 ERROR. Explicit type qualification
  accidentally changed the literal Message-ID header; restored exact standard header.
  Four-message test waited for inbox completion after its first message; moved reads
  after all four sends. Assertions were retained.
- 89347602069143c9a926095719391d16: compiler FAIL, moved test assertion referenced a
  previous loop variable; assertion moved back into the correct scope.
- 251f80bb318c41a9809629502025e588:14/14 PASS, sourceUnchanged=true. Four fixed status
  templates, no proof impersonation, stable Message-ID, synthetic recipient privacy,
  SMTP450/550/retry, TLS and existing Identity email regressions. Provider acceptance
  is not mailbox delivery; no real email sent.
- 071a4ec8f6204cbd949b41ef5c106238: V43 TDD RED, migration43 did not yet exist.
- 5e71cba6350f443ba4085a9f694dc7b2: V43 migration FAIL, function already belonged to
  NOLOGIN writer; corrected CREATE OR REPLACE ownership ordering using the existing
  migration-owner SET delegation, with temporary CREATE revoked afterward.
- aba51940b8a842a7836c73175f1e45c5: fixture assertion FAIL before test body, historical
  expected41 migration files versus actual42 after adding V43 (V33 absent). Updated
  only that exact count; no historical migration was changed.
- 8b339413bc5b4452a9a9729c5a23d3b1: focused V43 schema/ACL rerun pending.

V43 source includes guarded atomic activation and approved origin compatibility.
It is not SOURCE/TEST READY: positive activation, current recipient/outbox,
transaction rollback/concurrency and full notification/UI gates remain unverified.
Parent manifest bytes and all historical migration files remain preserved.


### V43 approval/outbox test cycle (in progress)

8b339413bc5b4452a9a9729c5a23d3b1:1/1 PASS, sourceUnchanged=true (schema, exact
SQL owners/ACL, runtime no writer membership, direct activation/DML negatives).
7ff6f16be0ab4e9f963cdfd5d6a4ba31:1 PASS/1 ERROR; shared decision fixture explicitly
selected42 while new approval projection expected43. Fixed fixture selection only.
fc990bbcae4541538477704cee7706a9:1 FAIL, activation rollback reached an underlying
SQL error redacted by canonical command admission as SERVICE_UNAVAILABLE. Test-only
causal assertion added; no production error response/log or security assertion
weakened. New diagnostic rerun3250b2588db14d98bf579b25296c8af1 pending.

The notification port checks genuine issued context ownership before reading a
subject, exact locked Subject object identity, eligible verified recipient and same
physical DataSource transaction. Encryption binds notice/event/application/applicant/
template. No Notifications SQL reads Merchant tables. Worker uses short claims and
post-commit SMTP with bounded retries; source is not yet accepted as tested.


### V43 causal fixes / architecture reconciliation

3250b2588db14d98bf579b25296c8af1:1 FAIL; causal SQL permission denied for unchanged
member_grant CHECK helper team_role_ceiling(TEXT). V43 grants this pure immutable
helper to the existing NOLOGIN activation writer only; no extra role membership.
0a944c5b44b045c28d0068f7e6e821d7:1 FAIL; new approval HTTP path was deliberately
outside the existing finite origin gate. No origin bypass was added. Production
HTTP approval remains closed pending correct public command-event composition.
1583078194fc4c2ea7f9abadf52485b9:1 ERROR at deferred commit. Test now checks all
constraints immediately before returning so the causal failure remains visible.
ef152c12fe144329ab50cf63ceaccebc:1 FAIL; deferred Team trigger requires EXECUTE on
existing team_offline_operator(). It remains false for the non-schema-owner writer;
only helper EXECUTE added, no operator identity/membership or changed predicate.
Rerunab67fd5b6de84365851ff5f6c5b22c13 pending.

P0-PORT-07 references a Merchant public event port but supplies no Java signature.
Exact new reader proposed once in admin-p0-notice-composition.delta.md (approval
pending); no new reader is implemented. Direct Merchant-to-Notifications API coupling
was detected and removed, preserving ADR dependency rules. Current isolated test
observer discovers event IDs only as test evidence and composes accepted public
ports in the same transaction; it is not production implementation or E2E acceptance.
No fake event, proof, recipient or business record substitutes for the canonical flow.


### P0-NOTICE-COMPOSE-01 approval / canonical Workflow integration

Owner approved exact bounded composition reader2026-11-24. Delta status updated;
parent manifest unchanged. Reader now verifies owning issued Identity context,
current resource/subject/scope, own actor/realm/key/application/operation, exact
Merchant event+receipt binding and same-transaction freshness. Authorized historical
replay returns empty, not a second enqueue. Workflow composes accepted public ports;
Merchant has no Notifications dependency. No new HTTP route/schema/permission/role.

ab67fd5b6de84365851ff5f6c5b22c13:1/1 PASS, sourceUnchanged=true: genuine canonical
review/stepup/domain activation, one Org/Owner/key/grant, same-key consumed-proof
replay, rollback preserves only prior RFI notice, no transport before commit,
post-commit bounded worker accepts two notices (not mailbox-delivered evidence).
This earlier case used a test-only event observer; not claimed as production E2E.
New58639f07104c4f0389da402ffb818576 rerun exercises actual approved event reader,
Workflow composition and canonical HTTP approval alongside V42 regression; pending.
No task is marked full READY while tests are pending.


58639f07104c4f0389da402ffb818576:2/2 PASS, sourceUnchanged=true. Approved public
reader and Workflow now drive actual HTTP approval/RFI, with genuine proof/session,
independent reviewers, concurrent exact-key retry, rollback and old V42 rejection.
Only initial dashboard.read is created; fresh event is enqueued once; old receipt
replay is empty. Constructed recipient denied and worker refuses business TX.
57df004961ca4874ba8eb31712e2686e:13/13 PASS, sourceUnchanged=true: architecture,
recipient AEAD tamper/context/key retention and Local/Test-only configuration.
Frontend22/22 session/governance units PASS; typecheck, lint and customer build PASS.
Existing >500kB build chunk advisory remains; no bundle warning was suppressed.
Validator initially found two unregistered new test paths; scope inventory corrected.
Real HTTPS Browser approval and post-commit loopback SMTP acceptance pending artifact
7e5b2f082e6e4723bc9c28a6f14baba6; not counted PASS before completion.


7e5b2f082e6e4723bc9c28a6f14baba6:2 PASS/1 FAIL. Original constructed-context
READ/COMMAND gate and existing genuine rejection Browser PASS. Approval Browser
selectOption timed out on implicit label association; the element had the right
visible/role label but Playwright label-text matching included option content.
Added explicit aria-label, no authority/actionability workaround. Frontend focused
UI tests2/2 PASS for reject and approve including exact lost-response retry.
Only affected genuine approval Browser rerun205d9171034c4614b36e1b478912645c pending.
Preservation archive hash/CRC PASS. Its migration inventory also includes an old
unapplied V42 draft: that file differs from archive due to previously approved
V42 amendments;40 historical V1-V41 files remain unchanged. Parent hash exact.


205d9171034c4614b36e1b478912645c:1/1 PASS, sourceUnchanged=true. Actual pinned HTTPS
Admin login, independent approval UI/stepup, persisted activation, foreign404,
anonymous401, mobile overflow, logout; committed outbox delivered via the existing
SmtpCustomerDelivery to controlled loopback SMTP and marked ACCEPTED_BY_PROVIDER.
No real mailbox-delivery or demo deployment claim.
b2b87ebdc4b949e1bfb0d6ccbbf15640: compiler FAIL (JUnit generic/BooleanSupplier overload).
Assertion changed to exact Boolean.TRUE comparison, no weakened condition.
e4d00162086848c89e7eaf63dd61ab85: interrupted execution, no summary/fresh report;
not counted PASS. No surviving owned Java test process when resuming.
e27b164838884a52a82d2335f0e09d23:1/1 PASS, sourceUnchanged=true. Extended current
Customer/Admin event reader, constructed/captured contexts, foreign/missing/key/
template negatives, direct-DML denial, Customer withdrawal/replay, no mail in TX,
concurrent workers, transient recovery retaining Message-ID and max5-attempt stop.
9a99308d4fe4495b9b67f0e8576a0329: new Organization directory/lifecycle TDD RED pending;
these default-deny methods are not implemented or marked DONE yet.


### Organization directory and lifecycle TDD continuation

9a99308d4fe4495b9b67f0e8576a0329: RED as intended, new Organization reader default
POLICY_UNAVAILABLE. No fixture/environment error or disabled assertion.
dacdee56e2094385ab59949708850fb8:1/1 PASS, sourceUnchanged=true. Scoped directory,
immutable activation-policy inheritance, membership counts, SUSPEND/REINSTATE with
canonical proof/digest, old-version consumed-proof replay and current Business
admission denial/restoration. No Customer Identity or grant changes.
Implementation adds only approved P0-GOV13..16/DTO25..27,39. Legacy origin retains
explicit Org scopes only; new lifecycle guard refuses runtime direct state DML and
requires exact current System event/receipt/proof/scope, independent actor, legal
state/version and current initial Owner for reinstatement. No Carrier/Finance/IAM
rights. PostgreSQL role setting (not a custom GUC) preserves the original authorized
offline operator branch for legacy workspaces; approved-origin changes require the
canonical lifecycle. Existing Team/Owner/initial policy branches remain unchanged.
29d11352ba0b4d42974f83efd6b4f164: focused HTTP lifecycle + affected Business/Team
regressions pending. No source/test-ready claim while these gates are unverified.


29d11352ba0b4d42974f83efd6b4f164: compiler FAIL in test only, duplicate stepBody
local variable. Renamed lifecycle test variable; assertions and authority unchanged.
aa0113a664d140cb8f63c9a6ad4bf7a5 rerun pending. Organization frontend added with exact
finite routes/DTOs, explicit state/origin, role/state counts and step-up/replay-safe
lifecycle; default deny and API authorization remain Backend-owned. No frontend
success or source/test completion claimed before its focused tests and Browser.


aa0113a664d140cb8f63c9a6ad4bf7a5:2 PASS/1 FAIL/1 ERROR. New scope assertion expected
403 after reviewer had already been revoked; exact404 is correct resource concealment.
Legacy Business HTTP context exposed a genuine default-off dependency regression:
CustomerProfileConfiguration eagerly required the new Business Contact port even
when workflow was disabled. Changed to ObjectProvider resolved only on the enabled
workflow branch; no fake or permissive contact authority fallback.
520438676fc946a48d390198eaad5eb9:3/3 PASS, sourceUnchanged=true. Actual Organization
HTTP stepup/suspension/reinstatement, Origin/If-Match/direct-DML negatives, current
Business denial/restoration, legacy Business current authority and real Team
Membership lifecycle all PASS. Prior operator/Team direct-DML tests had PASS in
aa0113 and were not redundantly rerun without a further affected change.
Frontend typecheck/lint24 unit tests PASS after correcting one effect-ref lint
finding; no lint rule suppressed. Focused UI3/3 PASS: rejection, approval and
Organization lifecycle retry retain the original body/key/ETag and one stepup;
password is cleared and never placed in browser storage. Current-state refresh is
separate from immutable activation outcome.
Actual HTTPS approval->directory->suspend/reinstate extension pending in
1aae0c7b20624598a24d89760c9ed7d2. Full P0 acceptance still pending.


### Resume after Owner local-preview pause (2026-11-24)

Owner resumed source/test implementation; demo V35 and isolated Admin UAT V40
remain separate from P0 disposable tests. No runtime artifact/configuration, real
grant, mailbox, demo/UAT schema or historical migration changes in this resume.
Parent manifest hash remains exact. Branch/HEAD unchanged;148 WIP paths at resume.

Reconciled prior pending evidence:1aae0c7b20624598a24d89760c9ed7d2 completed1/1 PASS,
sourceUnchanged=true: actual HTTPS approval, scoped Organization directory,
suspension/reinstatement, logout and committed loopback notification. No real mail.
307bcf274c8049079ad92497ef19c1fb: compiler FAIL,0 tests, public-port return type
assigned to concrete test record field. Changed only ReadyMerchant.commands to
AdminCommandAdmission; no assertion removed.

aa9f78cddbe3430da098c4d30065ba10: startup ERROR before test methods. Docker Linux
Engine pipe absent on read-only diagnostic; no source failure/PG PASS inferred.
Owner subsequently confirmed Docker Engine ready; affected tests rerun separately.

6e6771a6d6964ac197071bf2fa5160f0: TDD RED,0 executed tests, missing
activationFailure method. Synthetic SQLState/message tests require finite exact
canonical mappings and reject wrong states/unrelated constraints. Implementation
translates only canonical legacy23514 and registry duplicate23505 conflicts; all
other errors propagate fail closed. HTTP mappings now follow approved P0-ERROR-01
409/422 contract, with generic finite response bodies and no SQL/PII diagnostics.

e399b5164d764ca096ee3d5fa7477b55: focused unit/HTTP and isolated PG18 legacy,
concurrent opposite-decision and duplicate legal-key tests RUNNING; not PASS until
fresh reports complete. No full-suite/CI/P0 READY claim.


e399b5164d764ca096ee3d5fa7477b55:5 PASS/1 FAIL, sourceUnchanged=true. Unit canonical
activation error mapping2/2, safe finite HTTP problems1/1, actual PG18 legacy-key
clearance and concurrent APPROVE/REJECT each PASS. Duplicate-key test failed before
activation because the reused submission fixture asserted a globally empty
Organization/Membership database. Original fixture retains its zero precondition;
a second variant asserts exact original Organization/Membership ID sets are
unchanged, allowing the deliberate existing-Organization collision case.

cd9cdedca28f414aadcadfe554524e88: compiler FAIL, duplicate identical new rejection
test method. Removed only the duplicate definition, retained all assertions.
96a23f27ab65467f972969a826a28ac3:1 PASS/1 FAIL. Real duplicate legal-key conflict
PASS with one original Organization/registry and second application unchanged.
The new rejection assertion used PENDING, which is not an approved outbox state;
corrected to exact QUEUED from V43 contract. No source state-machine change.
1760fb0f4d0648c38fa6d23372d3139b: affected rejection-only rerun pending.


### V43 conflict completion and scoped Audit reader

1760fb0f4d0648c38fa6d23372d3139b: rejection-only1/1 PASS, sourceUnchanged=true.
Canonical REJECTED event/receipt creates one QUEUED outbox intent, consumed-proof
replay creates none, and only post-commit dispatch reaches the test transport.
Organization and activation counts remain0. No real email.

P0-READ-02 Audit source TDD: dfeeefa876e94e639e170f4a1d19e322 compiler FAIL from
identical duplicated patch hunks, removed without assertion changes.
84713d168edb4b8f870a78b5cba333fa RED with actual POLICY_UNAVAILABLE default method.
9b7dace0e2084eb4a7e72ae300567da5:1/1 PASS, sourceUnchanged=true, isolated PG18 and
canonical authenticated HTTP filter. A dedicated ENTRY-only Admin with exact
audit.read policy scope reads finite event metadata, pagination partitions rows,
private Merchant detail remains denied, foreign target conceals404, escaped
callback denies and operator scope revoke denies next read. No new grants outside
DB tests. Source/HTTP support application/Organization domains; Organization
positive-event and expanded isolation acceptance remain pending.

Frontend Audit: initial command used wrong working directory and found no test;
not a test PASS. Corrected TDD RED cannot resolve audit-data before implementation.
Typecheck/lint PASS;31/31 unit tests PASS (Audit privacy/navigation, existing
Governance/session and Internal navigation). Customer build and6/6 intercepted UI
tests PASS: review/lost response/approval/lifecycle/KPI and new scoped Audit.
These are UI units, not new real Browser/backend E2E evidence. Preexisting bundle
chunk advisory remains unchanged. Audit rows clear on permission denial; no local
credential storage, new mutation or export introduced.

Owner approved P0-FACTS-CTX-01 on2026-11-24. Delta adds exactly the default-deny
Access.identityContext getter; Merchant invokes existing Identity owner verifier
before actor/scope fields, physical source checks remain required. Parent manifest
bytes unchanged. P0-PORT-05 first-submission capture/page implementation pending
832b6846c2414fe0ad6772940f2c6fc1; no Dashboard v2 completion claim.
d29c1c6dd7024fb3b957047910938b97 was TDD compiler RED (missing source constructor),
0 executed tests. No schema, runtime flags, real target or authority changes.


832b6846c2414fe0ad6772940f2c6fc1: P0-PORT-05 canonical application capture/page1/1
PASS on isolated PG18. firstSubmittedAt/terminal time are persisted Merchant facts,
no Draft counted, default v1 availability unchanged. Source scope/page fingerprint
mismatch denies. 8b03f519aac54f10b51b2bfd07fdc3c8:13/13 PASS, sourceUnchanged=true,
Architecture and expanded source context tests plus original constructed READ/COMMAND
assertions unchanged. New source denies no-op Access, forwarding/substituted context,
escaped callback, different physical DataSource and suspended transaction. An
otherwise eligible MERCHANT READ Admin without domain scope receives unavailable
POLICY_NOT_CONFIGURED/empty payload, never all records or a fabricated count0.

Preservation reconfirmed: parent manifest exact SHA,103-entry original WIP archive
SHA/CRC PASS and40/40 historical V1-V41 hashes match. Initial inventory command used
array-shaped fields against an object map and reported0 examined; that incomplete
check is not evidence. Corrected property-map comparison examined40 and passed.
No historical SQL, shared DB, credentials, runtime flags, grant or Git changes.

Bounded tasks: P0-IMPL-06.2 Audit application-domain source/HTTP/UI tested;
P0-IMPL-06.3 canonical application facts and context delta tested. Full metric-v2
negotiation/metadata/scope snapshots, Work reader, source-backed positive Browser
and combined P0-T-01..15 remain pending. New metric-registry TDD5dddd423192f4a3baf69eba5516ada12
running; not counted PASS before actual report.


### Metric definition registry continuation

5dddd423192f4a3baf69eba5516ada12: TDD compiler RED,0 tests (class absent).
19da974f92c144d5aeca1ed473fcdad9:4/4 metric tests PASS, sourceUnchanged=true.
Definitions use exact integer count/sum and terminal-decision sample denominator;
AVG floors total milliseconds/sample count, is null at0 samples, MIN/MAX omitted
without observed samples. Unavailable sources yield no metrics. Vietnam calendar
series bucket first submission, not decision time; duplicate IDs, Draft and
unbound terminal facts fail closed. No commercial SLA/revenue inference.

2df25f6e996542db9524195aef4d0e4f: architecture10 PASS/1 FAIL, prohibited
Notifications-to-Merchant dependency in the new pure helper. Moved that new
cross-domain composition and its test to Workflow, which already owns this
public-port composition. No rule, graph, SQL, API or authority was widened.
d4f1b8002ba04ef9bfce4af93198bc91: affected Architecture+metric rerun pending.

Validator after Audit/facts additions: PASS, source_mode OWNER_AUTHORIZED_SPEC003_IMPLEMENTATION,
semantic ownership CONSISTENT, errors0 and10 unchanged preexisting broken links.
Registry path move is registered in mutable implementation-scope inventory;
final validator rerun remains required. Full P0-T inventory, v2 HTTP negotiation
and UI/snapshot integration and Work Inbox remain incomplete.


d4f1b8002ba04ef9bfce4af93198bc91:15/15 PASS, sourceUnchanged=true: unchanged
Architecture rules11/11 and moved Workflow metric definitions4/4. Original
prohibited-dependency failure remains above. No private imports, new authority or
runtime feature activation. Frontend gates were not repeated after Backend-only
helper movement because their source/dependencies did not change.

Current checkpoint: **P0 SOURCE/TEST IN_PROGRESS**, not V43/P0 READY. Closed this
continuation's actual compiler/fixture/conflict/architecture failures with targeted
evidence. Remaining work: Dashboard v2 wire/negotiation/metadata/scope snapshots,
Merchant Queue metrics integration, Work Inbox, expanded audit Organization/tenant
and full Customer-to-independent-activation Browser acceptance and P0-T-01..15.
No user approval pending for P0-FACTS-CTX-01. No commit/push/merge or real-target
P0 deployment has been performed.

## 2026-11-25 ? Overview group completion review

Owner's current priority is ADM-01/17/18 under Overview. Existing implemented
capabilities are reused; no new authority, schema, production source or real grant
has been introduced in this UI refinement. Parent manifest remains
90c795bd7e773b6a58d0d220056cfbc5e9a5ee95b38ae2493d5b9f75a372291f.

| Module | Implemented / connected | Remaining boundary |
|---|---|---|
| ADM-01 PARTIAL | Exact v1 compatibility; opt-in v2; Merchant first-submission counts and terminal-duration samples; real Shipment/Parcel/confirmed-fee/UNPAID facts; source metadata; scope/snapshot drill-through; Merchant Queue | Official Network, advanced Carrier lifecycle, SLA/risks, COD/reconciliation and platform telemetry have no approved authoritative source; unavailable never becomes zero |
| ADM-17 PARTIAL | Genuine assignment-derived Work Inbox and independent decisions, eligibility before pagination, current scopes; canonical review/handoff navigation; own context/profile and logout; truthful security/preferences states | Admin notifications, MFA/device/session inventory, other-session logout, persisted preferences, escalation/deadline policies require additional Identity/Notifications contracts |
| ADM-18 PROPOSED extension | Own active scope/capability matrix via existing API; Merchant independent-review invariant and actual approval workflow remain enforced | General policy/type registry, delegated limits, publication/revocation workflows and multi-domain approval authority remain unapproved; no new write endpoint, role or grant |

Navigation now groups Executive Dashboard, My Approvals, Operational Risk and
Service Health. ADM-17 is no longer duplicated in a separate parent group.
ADM-18 own-scope reader is reachable from Work; it is not a policy editor or an
Admin-wide grant directory. Documentary R28-R32 aliases never issue authority.

### Executed evidence retained for unchanged dependencies

- 6e85dc80c40c4d6a84707e351497ad61:5/5 PASS (metric registry4 + actual PG18
  Dashboard v2 HTTP1). Exact legacy fields, negotiation/406, first-submitted
  counts, processing samples, safe application/Shipment drill, filter/scope drift
  and revocation checked. sourceUnchanged=true.
- dfbd45edf3b0448da8c15700025e8517: Architecture11 PASS; Work/Audit fixture
  ERROR on duplicate open business key. Fixed fixture uses a distinct synthetic
  registration for the second business, without changing domain uniqueness.
- 0bf210e34ee5478aa13bf221338df43e: affected Work/Organization Audit1/1 PASS.
  Stable identity, independent decision work, filtering/pagination, HTTP privacy,
  wrong scope/anonymous denial, revoked reviewer assignment and regrant denial,
  canonical Organization suspend/reinstate events, policy and exact-Organization
  audit access, audit-only denial of organization detail. sourceUnchanged=true.
- Frontend scoped parser/transport regressions31/31 PASS. Latest targeted
  navigation/v2/work parsers9/9 PASS. Overview/work/own-authority UI, grouped
  navigation and direct-route denial3/3 PASS. Existing profile assertion matched
  a disabled menu's permission explanation; scoped it to actual Profile actions;
  affected profile test1/1 PASS. This does not authorize any MFA or grant action.
- Typecheck, lint and customer production build passed before the final fixture
  correction. Existing >500kB bundle advisory retained, not silenced.

### Consolidated Customer/Admin acceptance and targeted correction

The integrated Browser uses actual Customer and SYSTEM engines, independent
reviewer/decider, an owned HTTPS Spring host, PostgreSQL18, loopback SMTP and
scanner protocol. No HTTP interception or direct-SQL KPI seeds. Scanner transport
integration is not real antivirus certification; provider acceptance is not inbox
delivery. This remains source/test, not real Admin-UAT deployment.

Retained failures: missing test-host BusinessDashboard configuration; invalid PNG
fixture (422); cleanup masking the primary exception; ambiguous tbody selector;
real Audit mobile overflow caused by unused CSS class names; incorrect test-only
membership column reference. Fixtures, cleanup and scoped selectors corrected;
Audit now uses the existing scroll/filter classes. No business assertion relaxed.
32493fbe9f99440fa4a6007db032cf41 also observed an intermittent503 at review; no
unsupported root-cause claim is made for that historical occurrence.

b6a2c7dff2484e438da1e2c07edc27c2: integrated HTTPS Browser + PostgreSQL assertions
1/1 PASS, sourceUnchanged=true. Customer draft/contact verification/screened
evidence/consent/submission -> assigned review/checklist -> independent reauth
approval -> one Organization/Initial Owner -> Dashboard count/drill, empty Work,
scoped Audit, two loopback messages, mobile and logout all exercised.

Rather than silently retry the intermittent failure, the same case was extended
with deterministic contention on the existing Identity administration advisory
lock. 2cf54d8632044f04b83a1afc60f09772 failed: expected503, got401; redacted SQL
states55P03 prove lock timeout. Confirmed source defect: ProductionReadGuard's
generic RuntimeException handler converted session-storage DataAccessException
to SESSION_INVALID, causing the UI to discard a valid session/pending request.
Narrow fix: DataAccessException returns503 SERVICE_UNAVAILABLE. Invalid sessions
still return401; no route/authentication proceeds on failure, no authority widened.

Final affected command:
`powershell.exe -NoProfile -ExecutionPolicy Bypass -File scripts/ci/run-cskh-focused.ps1 -Tests 'ProductionIdentitySecurityTests,IdentityGrantApplicationTests#adminCoreRealHttpsCustomerToIndependentActivation' -Offline`

Run e341ff862d8b40c09d87fdca83db0892: **45/45 PASS**, zero failed/error/skipped,
sourceUnchanged=true. Security44 + integrated HTTPS/PG18 Browser1. Synthetic lock
produced the expected single55P03 and503; explicit same-request UI retry succeeded
with the original Idempotency-Key, body and If-Match, creating exactly one
READY_FOR_DECISION event. Genuine SESSION_INVALID remains401; private storage
marker is redacted, admission/context cleared, no Set-Cookie or admin execution.

Frontend final affected command (npm.cmd avoids host PowerShell npm.ps1 policy):
`npm.cmd run lint` and
`npx.cmd playwright test --config playwright-customer-ui.config.ts admin-dashboard.spec.ts --grep 'overview work center'`.
Both PASS; the latter builds Customer production assets with typecheck and tests
one UI case. Own Authority view now links My Approvals only when task.read and
application.read share a POLICY assignment; audit-only refresh hides the link.
Mobile screenshot inspected; page does not overflow and the data table scrolls
inside its panel. The existing bundle-size advisory remains visible. This UI
case uses intercepted fixtures and is not substituted for the real HTTPS case.

### Preservation, inventory and checkpoint

`python docs/reconciliation/v3/validate.py --implementation`: PASS, errors0,
semantic ownership CONSISTENT; ten preexisting shipper-mobile links unchanged.
New approved implementation sources registered in mutable implementation-scope.
`git diff --check`: PASS (only existing CRLF normalization advisories).
Parent manifest SHA256 remains90c795bd7e773b6a58d0d220056cfbc5e9a5ee95b38ae2493d5b9f75a372291f.
Historical V1..V41:40 files, hashes unchanged; V33 remains absent. The preservation
inventory also contains an old un-applied V42 draft; it is not a historical applied
migration and changed only under the approved V42 source/test work. Archive103
entries, CRC PASS, SHA256
1ed104f3c7bf09914473ac03f32b9f94a4d6a4112a6a9fdd9ea0b6bd8aa9bcf3.

Approved Overview/Work/Merchant workflow acceptance above is verified. Full
ADM-01/17/18 and the broader P0-T-01..15 inventory are **not** declared DONE.
ADM-01 PARTIAL / ADM-17 PARTIAL / ADM-18 PROPOSED remain the honest classifications:
missing authoritative sources or unapproved policy/security write contracts are
listed in the coverage matrix. No live deployment, real email/grant or database
mutation outside isolated fixtures; no commit/push/merge. No full PostgreSQL suite
or GitHub CI PASS claim. Historical failures stay in this ledger.


### 2026-11-25 integration into PR51

Owner separately authorized commit/push/merge. Source checkpoint c369590 pushed
to existing PR51. First exact-head workflow45538723368 failed before Maven at
Test fail-closed report guard: three finite-source inventory assertions had not
registered42 new P0 PostgreSQL methods and the new storage-failure security test.
The new P0 unit suites and three SMTP methods also required registration before
strict report sanitization could accept their XML. No business test was skipped.

Correction extends the explicit allowlist by42 PG cases and22 unit cases across
nine new suites plus two existing suites. Old420 PG inventory grows to462 and
HTTP security43 to44. Existing methods and source ordering stay required; unknown
methods remain rejected. Added two focused tests for exact P0 source inventory
and missing/renamed/skipped/failure/error report rejection. Initial TDD failure and
three original CI assertions retained; six targeted inventory gates PASS after
correction (one mistyped test selector corrected without source change). Full
report-guard and integration outcomes will be checked on the replacement CI SHA;
this is not a full-suite PASS claim. Local preview launcher focused checks PASS.
Exact manifest CRLF bytes and V41 bytes preserved through Git attributes; coverage
CSV newline normalization changes no field text. No deployment or DB mutation.


Second integration workflow45539198883 at5a412ef rejected two stale expected job
totals: H2 expected731 but explicit inventory753; PostgreSQL expected1263 but1327.
These totals grow by22 unit and42 PG cases; architecture23 and skip counts17/5
stay unchanged. The totals test plus P0/HTTP inventory gates4/4 PASS after correction.

Focused cold-start reproduction91f1979846454e818ba8547e0f7d10bd failed exactly
expected39 migrations/actual42. Legacy current-stream migration tests and the
exact table-owner inventory still described V40. Updated only fixed expected
counts/version list for V41-V43 and15 approved new tables (143 ->158), preserving
all prior checksum/data/ownership/privilege assertions and zero-repeat checks.
No migration SQL or runtime authority changed. Focused rerun tracked separately.

Focused PG18 rerun a61d50dc086c43219ead296b5bc75b0a:4/4 PASS, sourceUnchanged=true:
current-stream cold start, exact table/owner inventory, Identity history/rerun
preservation, populated Business V23 upgrade/data/authority preservation. These
checks do not replace required exact-revision CI or a full migration-suite result.
