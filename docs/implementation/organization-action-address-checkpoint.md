# Organization Action Center & Address Book - implementation checkpoint

Date: 2026-11-15. Branch `feature/argopost-organization-management`, HEAD
`5172dee570df26c593c15662b1318166cf64323f` unchanged. Owner accepted both scopes in
[OAA V1](organization-action-address-contract.proposed.md). No commit, push, merge,
new worktree, reset or cleanup. All57 preceding source paths remain in the ignored
checkpoint with matching SHA-256; V1-V28 are preserved.

## Implemented and verified slices

**OAA-04 DONE - Organization contacts.** Merchant owns current/version records,
default bindings, metadata audit and actor/purpose-bound command receipts. Current
verified Identity, active Organization/Membership and independent read/write grants
are required. Shared contacts require Organization permission; facility rights
never widen to shared or other sites. Domestic phone normalization, bounded
names/addresses, private literal search, duplicate advice, unique references even
when archived, immutable scope and versioned defaults are implemented. Archive
clears defaults atomically; restore does not steal defaults. Contacts remain
UNVERIFIED, not Identity credentials or proof of coverage. UI copies fresh active
same-scope snapshots into manual creation and explicit unlocked import rows. A
late lookup cannot overwrite newer edits or modify an existing Booking.

**OAA-05 DONE - Contact transfers.** Actual preview/job history, explicit <=20-row
atomic commit, deterministic row receipts and <=200-row audited export; overflow
rejects rather than truncates. CSV max100rows/256KiB; XLSX max1MiB, bounded worker,
one literal worksheet, no formulas/macros/links/hidden content. Invalid rows are
explicit and do not store invalid input. Current creator/import rights are checked
again; a conflict rolls back the selected chunk. CSV/Excel export is permissioned,
spreadsheet-safe and labelled LOCAL_TEST. Bulk contact selection edits source cells
before the existing Shipment preview and creates no Shipment. StrictMode file
worker controllers are renewed after development effect cleanup.

**OAA-06 DONE - Durable internal work.** Orders owns tasks/events/receipts on an
existing scoped Draft or confirmed Business Shipment. Manage and assigned-worker
respond permissions are separate. OPEN -> IN_PROGRESS -> AWAITING_REVIEW ->
manager COMPLETED; assignment, substantive replies, priority/deadline changes,
reasoned cancellation/reopen, history and guarded individual bulk commands are
implemented. Deadlines are explicit Merchant work deadlines, not Carrier SLA.
Workflow composes Orders and Notifications APIs in one physical transaction.
Notifications stores metadata-only assignee notices with current task access and
own read markers. It does not depend on Merchant or query Merchant Scope. Notice
failure rolls back task/event/receipt; replay never duplicates notices.

Existing Draft/unassigned queues retain real paging, filters, permissions and
return context. Unavailable Carrier/Ticket/Finance request sources are not
fabricated. Internal completion never changes Shipment, Tracking, fee, payment,
Carrier acceptance/delivery or compensation. Personal APIs/data stay independent.

## Executed checks and evidence boundaries

- Nine new PostgreSQL18 HTTP/race tests PASS: contact lifecycle/default/stale/
  reference/evidence rollback, Org/facility isolation, concurrency, import/export,
  chunk conflict rollback; task assignment/review/notices, atomic rollback/races,
  revoked permissions and Production denial.
- V1-V30 cold start/validate/rerun/populated upgrades: IdentityMigration61 and
  PostgresTestcontainers7 PASS on disposable databases. No demo data migration.
- Architecture23 PASS after removing Notifications -> Merchant dependency; no gate
  relaxation. Full H2 clean verify702 discovered:685 PASS/17 expected PG-only skips;
  report policy PASS.
- Real HTTPS/PostgreSQL/test-SMTP Browser17 PASS and parent PASS, including database,
  financial/Carrier isolation and session-cleanup assertions. Earlier closed-menu
  and duplicate-logout test failures remain private evidence, not PASS claims.
- Focused frontend units7 PASS; affected UI13 PASS including literal Excel,
  formula rejection, late-default-copy safety and revoked/unknown writes. Synthetic
  desktop/mobile screenshots inspected. UI mocks do not replace real HTTP/PG tests.
- Typecheck/lint/Customer build PASS; existing chunk-size warning remains.
  Report-policy132 PASS. Accepted Identity/Spec003 contract validators PASS;
  implementation/source reconciliation CONSISTENT with no errors.

**OAA-07 DONE (source/local verification).** Full Identity PostgreSQL18 regression:
370/370 PASS. Separate PostgreSQL Foundation8/8 PASS (V30 inventory/roles/data
preservation), migration61 and Testcontainers7 PASS. Report privacy and exact owned
lifecycle/container/volume/reaper absence audit PASS; this single-suite audit is
not misrepresented as the full multi-suite CI guard. The separately prepared
Foundation database/container was also removed by the existing ownership-checked
helper, with test credential privacy PASS and original demo state preserved.
Frontend whole unit suite155 PASS; affected production UI13 and committed
StrictMode development UI8 PASS, typecheck/lint/Customer build PASS. CI now includes
the narrow development UI regression for the concrete aborted-worker defect; no
required gate, assertion or TLS configuration was relaxed.

No hosted CI, integration PR or full external PostgreSQL job is claimed for
uncommitted work. Full required PR checks must run on the eventual pushed revision;
current evidence is the explicitly named executed suites above.

## Changed source groups and reproducible checks

- Merchant: OrganizationContacts/Transfers APIs, input/CSV validation, JDBC
  version/default/import/export implementation, existing configuration/fingerprint
  adapters and additive V29.
- Orders/Notifications/Workflow: OrganizationActions, metadata notices, current
  authorization/HTTP orchestration and additive V30. Existing operator command
  accepts only the six explicitly approved new permissions; OAA-08 real grants are recorded below.
- Web: OrganizationContacts/Transfers/TaskBoard, source parsers, client, navigation,
  manual/import contact snapshot selection and responsive styles.
- Tests: IdentityGrantApplicationTests (nine new cases plus real Browser fixture),
  migration/table inventories, finite report inventory, frontend unit/UI/worker
  regressions and owned development UI test server/configuration.

Executed commands:

- `.demo/business-shipping-review/run-focused.ps1 -Tests IdentityGrantApplicationTests`
  (370 PASS, includes actual Browser17 and database/session assertions).
- Same focused helper with `IdentityMigrationTests,PostgresTestcontainersTests`
  (61+7 PASS); focused OAA cases and ArchitectureTests/negative fixtures also PASS.
- `.demo/organization-action-address/run-foundation.ps1` (8 PASS, disposable test
  database on otherwise free5433; no demo start/stop or data writes).
- `.demo/merchant-reports-finance/run-h2.ps1` (702 discovered/685 PASS/17 expected
  PostgreSQL-only skips; report policy PASS).
- Web `npm.cmd test`, `npm.cmd run test:customer-ui -- organization-contacts.spec.ts
  organization-actions.spec.ts organization-action-center.spec.ts`,
  `npm.cmd run test:organization-dev-ui`, typecheck, lint and build:customer.
- `python -B -m unittest test_verify_reports` from scripts/ci (132 PASS),
  accepted Identity/Spec003 validators, implementation validator and diff checks.

## OAA-08 DONE - Owner-authorized Local deployment, 2026-11-16

Owner separately approved starting the existing PostgreSQL container, deploying
V29/V30 and the verified Backend, and granting six Organization permissions only
to the previously confirmed Customer in NamShop Demo. This does not activate the
features for every Customer or grant Carrier/Finance/Production authority.

- Same existing container identity, healthy; no replacement container, volume
  reset, init, secret rotation or certificate regeneration. Runtime lifecycle and
  ownership/readiness checks reuse scripts/dev/start-local.ps1. Only previously
  owned app processes were considered for stop; zero live owned processes stopped.
- Verified JAR SHA-256:
  `bc9ec6d42dc0391dc730f782f5f1a5095106407e5c846f57a0b4068abb9f4747`.
  All656 packaged production classes matched the PostgreSQL-tested compiled
  classes; all30 packaged migration files matched source bytes. The deployed JAR
  matched this hash; the preceding local artifact was preserved privately.
- Flyway advanced exactly28 ->30 through additive V29/V30. Fingerprints of every
  pre-existing owner table and V1-V28 migration/checksum history matched before
  and after migration. No synthetic contacts or tasks were created.
- Exact target grants: `contacts.read`, `contacts.write`, `contacts.import`,
  `contacts.export`, `shipment.actions.manage`, `shipment.actions.respond`.
  Base dashboard/read grants were already present and preserved. Other grants,
  memberships and existing business data matched their pre-deployment fingerprints.
  Target membership authority/version changed through the accepted operator flow.
- Operator request `91723a24-835d-448c-84d9-db7ce3017842`: one GRANT audit and one
  durable receipt verified; six exact new Organization grants verified. Target
  identifiers and private data fingerprints stay in ignored restricted evidence.
- Launcher status: `LOCAL_FULL_STACK_HTTP_READY`. Frontend HTTPS5173 and Backend
  public media HTTPS8443 returned200 with trusted TLS validation. Contacts, tasks
  and notices APIs without credentials returned401/no-store. No TLS validation,
  CSRF or secure-cookie gate was relaxed.
- Owner confirmed authenticated NamShop Demo navigation into both Sổ địa chỉ &
  Người nhận and Công việc nội bộ without permission/data-loading errors. This is
  Owner-observed local UI evidence; computer-use itself failed to initialize its
  kernel and was not claimed as an automated local browser PASS. The prior17-case
  actual HTTPS/PG Browser gate remains the automated CRUD/workflow evidence.
- No new real Gmail delivery test, real Customer contact/task fixtures or Carrier
  event was generated during deployment. Claims, external Carrier requests,
  Finance disputes and evidence uploads still require their separate authority.

Local URL: https://127.0.0.1:5173/ . Existing commands remain
`.\scripts\dev\local.cmd status`, `start` and `stop`; no init is needed. Backend
and Frontend are left running for Owner. No new business feature was started.

Branch/HEAD unchanged; no commit, push, PR or merge in this checkpoint. GitHub CI
has not run on these uncommitted changes. All57 original archived source copies
remain SHA-256 intact. OAA-01 through OAA-08 DONE within the accepted V1 scope;
this is not full Carrier/Claims/Finance or full-platform acceptance.

## Integration preparation - Owner authorization2026-11-16

Owner requested push and merge into develop. The99 pending paths contain the
accepted OSM/MRF/OAA V1 implementation and their dependency migrations V27-V30,
reviewed together against origin/develop5172dee. Earlier no-commit/CI-pending lines
above describe the completed Local checkpoint. Integration proceeds through one
feature commit and PR, current exact-revision required CI, conversation resolution
and a normal merge commit. No branch protection bypass, source-branch deletion or
checkpoint/runtime cleanup is authorized or needed. Current CI/merge SHA is
recorded in the PR and delivery report; prior Local tests do not replace it.
