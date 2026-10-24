# Implementation Tasks 002: Xác thực và phân quyền theo phạm vi

**Tình trạng hiện tại:** T-002-01/02/03/04 DONE theo scope được duyệt; T-002-05 Application PARTIAL: first slice replaceIdentityGrants CI_VERIFIED on1698940/run39641670408. Production Feature002 và AC-002-01..05 chưa được nghiệm thu. **Rule:** Không tick DONE trước khi có code+test+evidence.
**Ordering:** T-002-01 → T-002-02…; các phần web/mobile có thể song song sau khi contract+API ổn định, không merge thiếu tests.

- [x] **T-002-01 — Review**
  - Thực hiện: Đọc source trace FR/BR/NFR và xác nhận [BLOCKING] Session cookie hay access+refresh token, vòng đời token, reset password qua mail sandbox, đăng ký account công khai hay chỉ admin cấp?.  
  - Phụ thuộc: specs/001.  
  - Hoàn thành khi: Owner và reviewer đánh dấu PROPOSED/ACCEPTED; không còn BLOCKING của vertical slice.  
  - Trace: REQ-002-01..05; AC-002-01..05.
- [x] **T-002-02 — Contract**
  - Thực hiện: Tạo hoặc cập nhật OpenAPI/event DTO cho `POST /api/v1/auth/login; POST /api/v1/auth/logout; POST /api/v1/auth/refresh [nếu chọn token]; GET /api/v1/auth/me; Admin /api/v1/admin/users và /scopes.`.  
  - Phụ thuộc: T-002-01.  
  - Hoàn thành khi: Schema build/validate được, mẫu request/response và error cases xác định.  
  - Trace: REQ-002-01..05; AC-002-01..05.
- [x] **T-002-03 — Migration**
  - Thực hiện: Xác định data model + Flyway migration cho `identity.user_account, credential/session, roles, permissions, scopes, audit trail; FK chỉ qua public IDs.`.  
  - Phụ thuộc: T-002-02.  
  - Hoàn thành khi: Migration apply từ DB trống + nâng cấp, kiểm tra indexes/constraints ownership và không phá schema hiện có.  
  - Trace: REQ-002-01..05; AC-002-01..05.
- [x] **T-002-04 — Domain**
  - Thực hiện: Viết domain types/guards bảo vệ module/ownership rules.  
  - Phụ thuộc: T-002-03.  
  - Hoàn thành khi: Unit test happy path, negative, bound và transition xanh.  
  - Trace: REQ-002-01..05; AC-002-01..05.
- [ ] **T-002-05 — Application**  
  - Thực hiện: Implement use cases, port, public module API và workflow `Role update/assignment and audit trong một transaction của identity; không update bảng order/finance.`.  
  - Phụ thuộc: T-002-04.  
  - Hoàn thành khi: Transaction consistent; không import internals module khác; integration test pass.  
  - Trace: REQ-002-01..05; AC-002-01..05.
- [ ] **T-002-06 — HTTP/security**  
  - Thực hiện: Lắp controller, validation, role+object authZ, error mapping, idempotency.  
  - Phụ thuộc: T-002-05.  
  - Hoàn thành khi: Contract tests và cross-tenant/expired-scope tests pass.  
  - Trace: REQ-002-01..05; AC-002-01..05.
- [ ] **T-002-07 — Frontend**  
  - Thực hiện: Lắp web/mobile feature UI cần thiết theo user journey, gồm loading/pending/error/conflict.  
  - Phụ thuộc: T-002-06.  
  - Hoàn thành khi: Hành vi form/state có test; không báo success trước server.  
  - Trace: REQ-002-01..05; AC-002-01..05.
- [ ] **T-002-08 — Async/integrations**  
  - Thực hiện: Kết nối provider/outbox/inbox/events nếu feature cần: AccountDisabled/PermissionChanged công khai tối thiểu nếu có subscriber; không chứa token hoặc password..  
  - Phụ thuộc: T-002-07.  
  - Hoàn thành khi: Duplicate/retry/crash không gây tác động trùng, failure có trạng thái rõ.  
  - Trace: REQ-002-01..05; AC-002-01..05.
- [ ] **T-002-09 — Quality**  
  - Thực hiện: Chạy: Domain role matrix, API 401/403/404, cross-tenant object integration, revoked token, session expiry, auth rate-limit and logs.  
  - Phụ thuộc: T-002-08.  
  - Hoàn thành khi: Unit/integration/concurrency/E2E/fault test và audit/metrics đạt, có run log.  
  - Trace: REQ-002-01..05; AC-002-01..05.
- [ ] **T-002-10 — Review & trace**  
  - Thực hiện: Cập nhật traceability + runbook + README/decision status, review PR an toàn.  
  - Phụ thuộc: T-002-09.  
  - Hoàn thành khi: Tất cả REQ/AC có test IDs và evidence; DoD trong docs/quality/test-strategy.md đầy đủ.  
  - Trace: REQ-002-01..05; AC-002-01..05.

## Verification gate (trước khi tích hợp)
- [ ] `mvnw verify` (backend) và architecture/integration tests.
- [ ] `typecheck`, lint, build web; mobile smoke/development build nếu liên quan.
- [ ] Flyway upgrade/test trên PostgreSQL sạch; không phát sinh file secrets.
- [ ] Thử replay key giống/khác payload, unauthorized object và concurrency nếu module có stateful changes.
- [ ] Chạy E2E happy và negative scenario; cập nhật evidence thật, traceability và ADR.

## Prompt cho Codex sau khi duyệt thiết kế
Đọc `AGENTS.md`, `docs/principles.md`, `docs/architecture/*`, các quyết định ACCEPTED và ba file feature này. Chỉ làm task đầu tiên chưa tick; không đổi tài liệu ngoài phạm vi nếu không có lý do và diff; chạy test, trình bày file đã chỉnh, kết quả, giả định và blocker. Không tự đánh dấu đã test khi chưa chạy.

## Fresh recovery checkpoint — 2026-10-20

Historical checkpoints below preserve their original results. The final
T-002-03 acceptance checkpoint at the end supersedes earlier migration pending
states only for its verified scope; no original REQ/AC or canonical DoD changes.

T-002-01: DONE for owner-approved browser design decisions; DEC-03/04 plus O1–O7 recorded in [ADR-0009](../../docs/architecture/decisions/ADR-0009-production-identity-lifecycle.md). This is fresh decision evidence after deletion. Existing “Chưa triển khai” refers to production feature implementation.
T-002-02: CONTRACT_REVIEW_PENDING; rebuilt schema/fixtures/validator require fresh results and strict lint/review. T-002-03..10 NOT STARTED; production AC-002-01..05 NOT ACCEPTED.
[Fresh evidence](../../docs/implementation/t-002-02-identity-contract-evidence.md) and [supplemental trace](../../docs/quality/feature-002-recovery-traceability.md). Main traceability.md is protected by concurrent Foundation work and unchanged; final integration requires separately authorized reconciliation.

## T-002-02 final checkpoint — 2026-10-21

T-002-02 DONE under owner-delegated final technical review scope. Fresh512 contract checks PASS, including43 mutation controls; strict Linux Redocly2.64.0 single attempt PASS with0 errors/0 warnings/0 ignored on exact accepted revision. [Findings, source hashes, execution and T-002-03 DoR/DoD](../../docs/implementation/t-002-02-identity-contract-evidence.md#t-002-02-final-review-and-acceptance-2026-10-21). This supersedes the recovery REVIEW_PENDING checkpoint above.
No distinct nondelegable human sign-off gate is specified for this contract task in its canonical DoD or owner authorization. Owner review of future publication remains separate; no commit or merge approval is inferred. The approved [supplemental trace](../../docs/quality/feature-002-recovery-traceability.md) covers task-level traceability; protected main traceability reconciliation and CI/runtime belong to later feature integration.
T-002-03 Migration is next, NOT STARTED. DoR: accepted contract, explicit migration/model scope approval and isolated runtime verification plan. This checkpoint prepares that plan; it creates no SQL, does not approve concrete tables beyond canonical logical scope, and runs no Java/Maven/PostgreSQL. AC-002-01..05 and the complete Feature002 remain NOT ACCEPTED.

## T-002-03 implementation checkpoint — 2026-10-21

**T-002-03: IMPLEMENTED_VERIFICATION_PENDING**, under explicit
OWNER_T_002_03_IMPLEMENTATION_APPROVAL: GO. This supersedes the NOT STARTED
checkpoint above, without ticking its completion checkbox or changing its DoD.
[Migration/evidence and bounded runtime authorization packet](../../docs/implementation/t-002-03-identity-migration-evidence.md).

- [x] Inventory global V1/V2; append V3__production_identity.sql, preserving prior migrations and Demo data paths.
- [x] Implement Identity-local account/grant/session-family/hash-proof/CSRF/receipt/audit schema; no production endpoints or automatic Demo import.
- [x] Write14 isolated PostgreSQL migration test methods; retain existing regression method identities and adapt exact V3 inventory assertions.
- [x] Run15 process-safe lexical/source controls, including15 mutation subcontrols and1 comment positive control (included, not additional DB tests).
- [ ] Compile and execute new PostgreSQL tests on this exact revision after bounded runtime authorization.
- [ ] Prove empty migrate, V2 upgrade/Demo preservation, FK/check/unique/index/ownership/permission negatives with actual PostgreSQL reports.
- [ ] Run relevant backend regressions and reconcile strict CI inventory in a separately authorized integration scope; protected report-policy files remain unchanged.

Source controls are NOT a PostgreSQL SQL parser or database enforcement evidence.
No Java/Maven/H2/Docker/PostgreSQL was executed in this checkpoint. T-002-04 Domain
remains NOT STARTED, dependent on T-002-03 acceptance. Feature002/AC-002-01..05
remain NOT ACCEPTED; Feature001/D1-08/Day1 remain PARTIAL.

## T-002-03-PG conditional verification checkpoint — 2026-10-21

OWNER_T_002_03_ISOLATED_POSTGRES_VERIFICATION: CONDITIONAL_GO was evaluated.
Verdict **T_002_03_ENVIRONMENT_BLOCKED**: source fingerprints and process-safe
checks PASS, but the proposed Linux Java21/Maven3.10.0 execution/containment path
has not been established. No Windows H2/Wrapper policy was reused for PostgreSQL.
Strict report inventory also lacks IdentityMigrationTests and its14 methods;
protected policy changes need a separately scoped authorization.

Fresh static suite15/15 PASS; runtime batch0, JVM/Maven launches0, actual database
migrations0, PostgreSQL assertions0. Compilation/cold-start/upgrade/constraints/
regression remain UNVERIFIED; no SQL/build failure is inferred.
[Current preflight and exact blockers](../../docs/implementation/t-002-03-identity-migration-evidence.md#t-002-03-pg-conditional-verification-preflight--2026-10-21).
T-002-03 stays IMPLEMENTED_VERIFICATION_PENDING with its checkbox open;
T-002-04 Domain NOT STARTED. Feature002/AC-002-01..05 remain NOT ACCEPTED;
Feature001/D1-08/Day1 remain PARTIAL. No REQ/AC or implementation source changed.

## T-002-03-RI readiness integration checkpoint — 2026-10-21

OWNER_T_002_03_READINESS_INTEGRATION_APPROVAL permits inventory-only changes to
the two protected report files. **T_002_03_CI_READY_RUNTIME_PENDING**:

- [x] Exact14 IdentityMigrationTests methods added to REQUIRED/TESTCONTAINERS_ONLY; no old identity removed, no expected skip broadened.
- [x]67 process-safe report tests PASS (56 retained +11 new);15 migration static tests PASS.
- [x] Existing ubuntu-24.04 / Backend PostgreSQL18 job selected; active workflow and historical baseline run39211616700 verified through read-only API.
- [ ] Resolve RI-LIFECYCLE-01: two suites currently overwrite the same single-container cleanup proof; current approval does not permit changing helper/audit behavior.
- [ ] Freeze corrected source packet and satisfy live hosted-runner/containment/resource/privacy gates under separate runtime/publication authorization.
- [ ] Actual Java compilation, PostgreSQL migration/upgrade/constraints/regressions and remote CI.

[Exact packet, hashes and lifecycle finding](../../docs/implementation/t-002-03-identity-migration-evidence.md#t-002-03-ri-readiness-integration--2026-10-21).
Nine other Foundation protected paths remain unchanged; two authorized policy
hash deltas are recorded. Main traceability is not edited. T-002-03 checkbox stays
open/IMPLEMENTED_VERIFICATION_PENDING; T-002-04 NOT STARTED; production ACs NOT
ACCEPTED, Feature001/D1-08/Day1 PARTIAL. No runtime execution or publication.

## RI-LIFECYCLE-01 correction checkpoint — 2026-10-21

OWNER_RI_LIFECYCLE_01_CORRECTION: GO; **RI_LIFECYCLE_01_CORRECTED_CI_READY**
(source/validation/wiring only).

- [x] Two known suites have dedicated create-only lifecycle records; expected suite/run/job/session identities fixed and audited.
- [x] Exact observations/created/verified/stopped resources retained; startup/ownership/stop uncertainty blocks PASS; teardown rechecks labels/mounts/consumers.
- [x] Combined read-only audit requires both suites and absence of both PG containers, both volumes and the explicitly shared Ryuk; no unknown-resource deletion.
- [x]88 current process-free report/lifecycle tests PASS; all67 original controls retained;15 current migration static tests PASS.
- [x] Existing workflow entrypoint consumes corrected manifest/records; no YAML/launcher change required.
- [ ] Separately authorize source dependency-closure review/publication and one exact-commit hosted verification batch.
- [ ] Actual Java compilation, PostgreSQL/Flyway/constraints/regression, runtime privacy and complete cleanup evidence.

[Current source hashes/protocol/runtime handoff](../../docs/implementation/t-002-03-identity-migration-evidence.md#ri-lifecycle-01-source-correction--2026-10-21).
Prior RI-LIFECYCLE-01 source gap is superseded by this correction; no historical
runtime result is rewritten. Nine other Foundation protected paths unchanged;
two cleanup-policy deltas are authorized and recorded. T-002-03 stays open /
IMPLEMENTED_VERIFICATION_PENDING; T-002-04 NOT STARTED; production ACs NOT ACCEPTED;
Feature001/D1-08/Day1 PARTIAL. No JVM/container/service/publication execution.

## T-002-03 final migration acceptance — 2026-10-21

**T-002-03 DONE** under OWNER_T_002_03_FINAL_ACCEPTANCE: GO and
OWNER_T_002_03_FINAL_EVIDENCE_REVIEW: GO; canonical checkbox is now checked.
[Verified run/artifacts, exact14 method outcomes, DoD and Domain handoff](../../docs/implementation/t-002-03-identity-migration-evidence.md#final-migration-acceptance--2026-10-21)
supersede earlier migration pending/not-run checkpoints without rewriting history.

- [x] Run39575568268 attempt1 tests PR head bf80d84a459e2b6c8f8431c47dcbbe9f0e84c817; actual checkout f426491f9b02c3045333f8296f6b1e320e4b7339 verified from all three job consoles.
- [x] Architecture23 PASS/0 skip; H2231 PASS/17 expected skips; PostgreSQL270 PASS/5 expected skips;0 failures/errors, exact inventories and all three artifact SHA-256 digests verified.
- [x] IdentityMigrationTests14/14 PASS; corrected PostgresFoundationTests.validateAndRerunPreserveDataAndChecksums PASS; cold V1/V2/V3, upgrades, constraints/indexes/ownership and Demo compatibility proven for migration scope.
- [x]88 report-policy controls per job; actual credential/privacy guards and PostgreSQL owned cleanup PASS.
- [x] Evidence and authorized supplemental traceability updated; protected main traceability and six Foundation changes preserved.

**T-002-04 Domain READY / NOT STARTED** for the bounded pure-domain scope in
the handoff. Canonical dependency, DoD and REQ/AC text are unchanged. Unit tests
must cover happy/negative/boundary/transition cases; concurrency/HTTP/application
acceptance remains with subsequent tasks. No Domain code or new runtime runs here.
PR20 review/merge and this three-file documentation publication remain separate,
unperformed operations. Production AC-002-01..05 NOT ACCEPTED; Feature002 remains
incomplete; Feature001/D1-08/Day1 PARTIAL.

## T-002-04 Domain implementation checkpoint — 2026-10-21

OWNER_T_002_04_DOMAIN_IMPLEMENTATION: GO. **IMPLEMENTED_VERIFICATION_PENDING**,
superseding Domain READY / NOT STARTED above without ticking its checkbox.
[Source/test fingerprints, invariant mapping and bounded CI handoff](../../docs/implementation/t-002-04-identity-domain-evidence.md).

- [x]13 pure-Java Domain files: account/lifecycle/version, role/scope/authority, username/password, session/proof/refresh policies; no framework/SQL/HTTP/developer-service dependency.
- [x]48 meaningful Jupiter test methods across5 suites plus synthetic fixture; happy/negative/boundary/transition assertions written, NOT executed.
- [x] Narrow exact inventory integration in the two permitted report-policy files;20 previous required class lists and collector functions unchanged; expected skips unchanged.
- [x]93 process-safe Python report controls PASS, including5 new Domain controls; final focused inventory controls5/5 PASS. Synthetic/source checks are not Java execution evidence.
- [ ] Compile and execute48 Domain JUnit cases on this exact revision through an approved contained/hosted path.
- [ ] Actual Identity/ArchUnit/backend regressions and strict inventories/privacy/cleanup on the same published source; no previous migration run substituted.
- [ ] Evaluate canonical Domain DoD against actual Java results before marking DONE.

T-002-05 Application is the next canonical task and is not started; its dependency
T-002-04 is not DONE. T-002-03 acceptance evidence remains byte-identical.
Production AC-002-01..05 NOT ACCEPTED; Feature002 incomplete;
Feature001/D1-08/Day1 PARTIAL. No stage/commit/push/PR/merge or next-task code.

## T-002-04 actual CI acceptance — 2026-10-21

**T-002-04 DONE**, superseding its historical pending checkpoint above.
[Run39626192090 attempt1, exact48 outcomes, artifact digests and Application handoff](../../docs/implementation/t-002-04-identity-domain-evidence.md#t-002-04-actual-ci-acceptance--2026-10-21).
Source commit/actual checkout bbb10987faa8dbcf7a85ccffe72c7c299e8e38b0 on
feature/002-identity-domain-ci, parent bf80d84; exactly25 scoped paths published,
one normal push and one native workflow_dispatch; no PR20 change/new PR/merge.

- [x] Real Java compile/backend verify and48/48 Domain JUnit cases PASS in BOTH H2 and PostgreSQL, including happy/negative/boundary/transitions.
- [x] Architecture23 PASS/0 skips; H2279 PASS/17 expected skips; PostgreSQL318 PASS/5 expected skips;0 failures/errors, exact test inventories.
- [x] PostgreSQL IdentityMigrationTests14/14 and corrected Foundation rerun regression PASS; no SQL changes or prohibited skips.
- [x]93 report-policy controls per job, SHA-256-verified sanitized artifacts, actual credential/privacy and PostgreSQL/Testcontainers owned cleanup PASS.
- [x] Evidence and supplemental trace updated; six Foundation changes and T-002-03 acceptance record preserved.

T-002-05 Application READY / NOT STARTED: bounded grant-management transaction
slice in the handoff; canonical DoD unchanged. Closure documentation after CI is
not included in tested source commit and needs a separate documentation commit;
no second automatic commit/push here. PR integration/review remains separate.
AC-002-01..05 NOT ACCEPTED; Feature002 incomplete; Feature001/D1-08/Day1 PARTIAL.

## T-002-05 first Application slice — 2026-10-21

OWNER_T_002_05_APPLICATION_IMPLEMENTATION: GO. Supersedes Application NOT STARTED
only for the bounded replaceIdentityGrants slice. **T-002-05 PARTIAL /
IMPLEMENTED_VERIFICATION_PENDING**, canonical checkbox remains open; canonical
task body/DoD and REQ/AC unchanged.
[Application source, persistence, tests and verification handoff](../../docs/implementation/t-002-05-identity-application-evidence.md).

- [x] Source authored: public backend command/result, Application orchestration/inward ports and V3 PostgreSQL JDBC adapter.
- [x] Reuse Domain full-target/union/default-deny/self/last-admin/version rules; current session/account checks; atomic grants/version/revocation/audit/receipt writes implemented.
- [x]33 PostgreSQL integration methods authored: positive/negative/replay, real-DB fault rollback and two-transaction concurrency; no dummy success assertions.
- [x] Strict finite report inventory and three-suite lifecycle registration;97 Python policy controls PASS (not Java tests).
- [x] Actual Java compilation, ArchUnit and all33 PostgreSQL transaction/fault/concurrency tests PASS on1698940/run39641670408 (2026-10-22), superseding the initial pending checkpoint.
- [x] Actual reports, credentials/privacy and complete three-suite owned Testcontainers cleanup PASS on1698940/run39641670408.
- [ ] Remaining canonical Application use cases and real owner-authority integrations implemented and verified within approved slices.

No HTTP/controller, production authentication/provider fallback, Demo behavior,
SQL migration, workflow, dependency, protected Foundation change or publication.
T-002-01..04 remain DONE; Feature002/AC-002-01..05 NOT ACCEPTED;
Feature001/D1-08/Day1 PARTIAL. Prior Domain CI verifies its historical source only.

## T-002-05 first slice CI acceptance — 2026-10-22

**T_002_05_FIRST_SLICE_CI_VERIFIED** under OWNER_T_002_05_FIRST_SLICE_CI_APPROVAL.
[Actual run,33 outcomes, exact publication manifest and artifact digests](../../docs/implementation/t-002-05-identity-application-evidence.md#t-002-05-first-slice-actual-ci-verification--2026-10-22).
Source1698940e4d4d888993daa2e38e995014c4f3282b, parent1498879,
feature/002-identity-application-ci; one normal push and one workflow_dispatch;
run39641670408 attempt1 SUCCESS, exact checkout SHA in all three consoles.
Architecture23 PASS/0 skips; H2279 PASS/17 expected skips;
PostgreSQL351 PASS/5 expected skips; all33 grant integration,48 Domain per
backend job and14 PostgreSQL migration methods PASS.97 policy controls/job,
sanitized ZIP SHA-256/inventory, privacy and complete owned cleanup PASS.

T-002-05 remains PARTIAL and unchecked: other Application operations and real
owner-authority composition remain pending/default-deny. No HTTP/API or full
AC-002-01..05 acceptance. Three post-CI SDD closure edits are uncommitted and
not tested by this source run. Six Foundation paths and T-002-03 evidence
preserved; no PR/merge, source retry or next-slice implementation.

## T-002-05-AUTH authority composition checkpoint — 2026-10-22

OWNER_T_002_05_AUTHORITY_COMPOSITION: GO.
**T_002_05_AUTHORITY_DEPENDENCY_BLOCKED** for complete production owner authority;
independent composition/availability boundary implemented, NEW runtime UNVERIFIED.
[Exact boundaries, missing contracts, source and19 authored tests](../../docs/implementation/t-002-05-identity-application-evidence.md#t-002-05-auth--bounded-production-composition--2026-10-22).

- [x] Add explicit production JDBC/use-case composition and fail-closed decorator of existing GrantAuthority; no permission catalog or permissive source.
- [x] Reuse authoritative current account/session/grant admission, full-target Domain guard, existing transaction and24h receipt protection unchanged.
- [x] Author8 unit +11 PostgreSQL composition cases; preserve all33 existing grant method bodies,48 Domain and14 migration requirements.
- [x] Exact strict inventory and101 Python report-policy controls PASS; existing lifecycle/resource ownership logic unchanged.
- [ ] Approved Identity scope-management/owner authority semantics and architecture-compatible public-contract bridge available.
- [ ] Real approved resource-owner authority provider connected and verified; unsupported scopes currently DENY.
- [ ] New Java compilation/unit/PostgreSQL/concurrent authority-state tests and privacy/cleanup PASS on this revision.

Original run39641670408 still verifies1698940 first slice only. Full T-002-05
PARTIAL/unchecked; T-002-01..04 DONE; Feature002/AC-002-01..05 NOT ACCEPTED;
Feature001/D1-08/Day1 PARTIAL. No branch change/publication/runtime or automatic
next-slice implementation. Existing three CI closure records preserved; current
AUTH checkpoint is appended rather than rewriting accepted historical evidence.

## T-002-05-AUTH-CONTRACT decision checkpoint — 2026-10-22

**T_002_05_AUTHORITY_CONTRACT_READY_FOR_OWNER_APPROVAL**.
[Exact policy, proposed public signatures, dependency directions, consistency and tests](../../docs/implementation/t-002-05-identity-application-evidence.md#t-002-05-auth-contract--owner-decision-packet--2026-10-22).

- [x] Distinguish accepted management admission/Domain guards from missing scope/delegation policy; no global authority inferred from IDENTITY_MANAGEMENT.
- [x] Inspect actual public owner APIs: Merchant existence is not actor-bound authority; other owner queries absent.
- [x] Specify PROPOSED actor/target/operation/exact-pair SPI, owner queries, explicit AUTHORIZED/DENIED/UNKNOWN results and fail-closed combination.
- [x] Specify architecture-compatible workflow/public-API bridge, including the two additional Warehouse/Dispatch-to-Identity directions that require approval if common DTOs are chosen.
- [x] Prepare exact Identity-only policy option, transaction/owner-lock preconditions and AUTHC-01..11 verification plan.
- [ ] Owner approves AUTHC-D1..D4; proposal is not an ACCEPTED contract/ADR or an authority grant.
- [ ] Implement genuine Identity provider only after scope/delegation policy is unambiguous; keep unsupported external scopes denied.
- [ ] Separately authorize and execute final-source Java/PostgreSQL/ArchUnit/privacy/cleanup verification.

Existing8 unit +11 PostgreSQL AUTH cases remain authored/UNVERIFIED. No source,
test, SQL, approved contract/ADR, workflow or report policy changes in this
checkpoint; previous evidence stays historical. T-002-05 PARTIAL/unchecked;
canonical task/DoD/REQ/AC unchanged. T-002-01..04 DONE; Feature002 and
AC-002-01..05 NOT ACCEPTED; Feature001/D1-08/Day1 PARTIAL.

## T-002-05-AUTHC-RESOLUTION restricted decision — 2026-10-22

OWNER_AUTHC_DECISION: CONDITIONAL_APPROVAL_WITH_RESTRICTIONS.
**T_002_05_IDENTITY_SCOPE_MAPPING_BLOCKED**.
[Exact D1–D4 disposition, mapping evidence, source/tests and remaining decision](../../docs/implementation/t-002-05-identity-application-evidence.md#t-002-05-authc-resolution--restricted-owner-decision--2026-10-22).

- [x] Record D1 restrictions, D2 approval in principle only, D3 external default-deny and D4 proven local consistency only.
- [x] Establish that accepted scopeId is a UUID reference with no approved Identity resource/realm/delegation mapping; previous realm registry/actor-held delegation proposal is NOT accepted.
- [x] Implement approved production empty-target denial before delegate invocation; preserve generic Domain/initial provisioning boundaries.
- [x] Author two focused negative Java cases and exact report identities;102 process-safe policy controls PASS, not Java results.
- [ ] Resolve one owner decision IDENTITY_SCOPE_RESOURCE_AND_ASSIGNABILITY; no allowing provider until authoritative mapping exists.
- [ ] Implement and verify an allowing Identity-local provider only within that resolved policy.
- [ ] Current-source Java/PostgreSQL/ArchUnit/privacy/cleanup verified under separate runtime/publication authorization.

Preserve8 earlier unit +11 AUTH PostgreSQL methods,33 original grant methods,
Domain48 and migration14. Now9 authority unit /45 grant PostgreSQL methods authored;
new21 authority cases overall remain UNVERIFIED. No external owner authority,
SPI implementation/dependency direction, migration or lifecycle change.
T-002-05 PARTIAL/unchecked; T-002-01..04 DONE; Feature002/AC-002-01..05 NOT ACCEPTED;
Feature001/D1-08/Day1 PARTIAL. Canonical task/DoD/REQ/AC unchanged.

## T-002-05-AUTHC-REALM architecture checkpoint — 2026-10-22

OWNER_IDENTITY_REALM_ARCHITECTURE: CONDITIONALLY_APPROVED.
**T_002_05_REALM_MIGRATION_APPROVAL_REQUIRED**.
[Realm decision, exact proposed V4, delegation policy and implementation gates](../../docs/implementation/t-002-05-identity-application-evidence.md#t-002-05-authc-realm--registered-identity-realm-decision--2026-10-22).

- [x] Record approved IDENTITY.scopeId = authoritative registered Realm UUID; prior resource-meaning ambiguity resolved at architecture level.
- [x] Bound proposed MVP to one system security Realm; no canonical platform-tenant/root organization mapping exists and Merchant is not a Realm.
- [x] Inspect V3 and propose V4__identity_system_realm.sql with registry/status, explicit registration, generated IDENTITY-only FK and safe legacy-data preflight.
- [x] Distinguish ordinary grant possession, management admission and explicit delegation; prepare exact same-Realm management-pair policy for approval.
- [x] Preserve all existing source/tests/empty-target guards;102 process-safe policy controls PASS, no Java/PG/SQL execution.
- [ ] Owner approves REALM-P1/P2 persistence and bootstrap before creating/applying V4 or a backed registry adapter.
- [ ] Owner settles REALM-P3 delegation before enabling an allowing provider.
- [ ] Implement approved bounded Realm model and execute REALM-01..09, current-source regressions/privacy/cleanup under runtime authorization.

No Realm source/value/port stub or SQL file added in this blocked checkpoint.
Existing9 authority unit /45 grant PG methods remain unchanged;21 authority
additions remain runtime UNVERIFIED. Original33 grant/48 Domain/14 migration
requirements retained. Canonical task/DoD/REQ/AC unchanged; T-002-05 PARTIAL and
unchecked; Feature002/AC-002-01..05 NOT ACCEPTED; Feature001/D1-08/Day1 PARTIAL.

## T-002-05-REALM-IMPLEMENTATION checkpoint — 2026-10-22

**T_002_05_REALM_PROVIDER_IMPLEMENTED_VERIFICATION_PENDING**.
[Approved scope, V4/bootstrap/provider, exact counts/hashes and runtime handoff](../../docs/implementation/t-002-05-identity-application-evidence.md#t-002-05-realm-implementation--approved-local-realm-provider--2026-10-22).
[ADR-0010](../../docs/architecture/decisions/ADR-0010-identity-system-realm.md)
records OWNER_REALM_P1/P2/P3 approval; prior approval blockers above are historical.

- [x] Implement additive V4 registry/IDENTITY-only FK, explicit UUID, INACTIVE registration and fail-closed unmatched legacy data.
- [x] Implement owner-only insert-new pending administrator bootstrap and prerequisite-gated one-time activation with atomic lock/audit; no production bootstrap executed.
- [x] Implement real same-transaction JDBC registry authority and exact same-Realm management pair policy, including authoritative nonempty full target context and external denial.
- [x] Wire default production composition; retain Domain/session/last-admin/version/idempotency/24h guards and empty-target restriction.
- [x] Author23 new Realm methods; retain original33 grant/48 Domain/14 migration identities plus21 prior AUTH cases; current grant52/migration22/new Realm unit8/authority unit9.
- [x] Maintain exact report/skip/privacy/cleanup inventory;104 policy +15 migration source checks PASS after correcting stale fixture counts.
- [ ] Compile and execute current-source Java/H2/PostgreSQL/ArchUnit, V4/privileges/bootstrap/rollback/concurrency and privacy/owned cleanup on an authorized runner.
- [ ] Complete and verify accepted invitation/provisioning and other remaining canonical Application/owner integrations.

Real initial invitation is a separate unfinished use case; synthetic fixture
activation is not a production bypass. Full T-002-05 PARTIAL/unchecked; canonical
task/DoD/REQ/AC unchanged. Feature002/AC-002-01..05 NOT ACCEPTED;
Feature001/D1-08/Day1 PARTIAL. No Git publication, dispatch, credentials, developer
services or privileged production bootstrap. Main Foundation trace protected.

## T-002-05-PROVISION bounded implementation checkpoint — 2026-10-22

[Source, authority approval, transaction, tests and delivery dependency](../../docs/implementation/t-002-05-identity-application-evidence.md#t-002-05-provision--bounded-invitation-provisioning-implementation--2026-10-22).
[Accepted narrow provisioning extension](../../docs/architecture/decisions/ADR-0010-identity-system-realm.md#bounded-ordinary-provisioning-extension--owner-approved-2026-10-22).

- [x] Reconcile prior Realm runtime evidence: exact4a90baf/run39795074146 attempt1
  SUCCESS, Architecture23/H2296+17 skips/PostgreSQL395+5 skips. Historical
  Realm runtime-pending statements above are superseded for that source only.
- [x] Record OWNER_PROVISION_INITIAL_IDENTITY_PAIR restrictions: explicit exact
  same-Realm management initial pair for a NEW pending account only; no external/
  mixed/automatic grants, activation, bootstrap or empty-target replacement change.
- [x] Author command, payload validation/fingerprint, dedicated authority,
  existing-transaction JDBC account/grant/proof/audit/receipt orchestration.
- [x] Require same-transaction invitation delivery port; default missing adapter
  fails before account writes. No new migration, crypto or provider invented.
- [x] Author12 unit and18 PostgreSQL methods; strict report inventory/privacy
  controls107 PASS and migration static controls15 PASS.
- [ ] Approve/implement durable protected invitation intent schema/key/issuer
  adapter. Synthetic test intent is NOT a production outbox/delivery mechanism.
- [ ] Execute current-source Java, ArchUnit/H2/PostgreSQL, transaction/concurrency,
  privacy and complete owned cleanup under separate runtime authorization.
- [ ] Complete other canonical T-002-05 use cases and later HTTP/security tasks.

T_002_05_PROVISIONING_IMPLEMENTED_VERIFICATION_PENDING, with production delivery
dependency BLOCKED. Future inventories23/0,308/17,425/5 are not actual test results.
T-002-05 remains PARTIAL and unchecked. T-002-01..04 DONE; Feature002/AC-002-01..05
NOT ACCEPTED. Original REQ/AC/DoD and six protected Foundation changes preserved.


## T-002-05 invitation/outbox checkpoint — 2026-10-23

T-002-05 remains PARTIAL, checkbox unchecked. Owner-approved source slice:
T_002_05_INVITATION_OUTBOX_IMPLEMENTED_VERIFICATION_PENDING.
[Evidence, exact inventory and runtime handoff](../../docs/implementation/t-002-05-identity-application-evidence.md#t-002-05-invitation-outbox-checkpoint--2026-10-23),
[ADR-0011](../../docs/architecture/decisions/ADR-0011-identity-invitation-outbox.md).

- [x] Append V5; preserve V1-V4 bytes and legacy proof representation.
- [x] Implement separate externally keyed HMAC verifier / AEAD delivery protection.
- [x] Integrate durable intent in the existing provisioning transaction and replay boundary.
- [x] Author 10 crypto/unit, 15 real-outbox PG and 3 V5 migration methods.
- [x] Python report-policy 110/110 and static source 25/25 PASS; finite inventory extended.
- [x] Compile and execute this revision's Java/PostgreSQL/ArchUnit/Demo regressions (run39896561084, tested0098461f).
- [x] Verify V1-V5 cold start, V4 upgrade, runtime roles, crypto/rollback/races/leases/purge (actual PostgreSQL CI PASS).
- [x] Verify current CI report/privacy and owned Testcontainers cleanup evidence (110 controls/job and all guards PASS).
- [ ] Separately integrate approved worker/provider and periodic retention maintenance.
- [ ] Implement remaining canonical Application use cases including invitation activation.

Do not reuse run39795074146 for this source. Expected future jobs: Architecture23/0skip,
H2318/17skip, PG453/5skip, all with110 report-policy controls. These are expectations,
not observed runtime results. GRANT_MUTATION_FUNCTIONALITY_LIMITED remains;
Feature002 and AC-002-01..05 NOT ACCEPTED. No REQ/AC/DoD modification.


## Provisioning and Secure Outbox CI acceptance - 2026-10-23

T_002_05_PROVISION_OUTBOX_CI_VERIFIED for tested source
0098461f2c4b242956c401430e5d5ca304521eb8, [run39896561084 attempt1](https://github.com/phamquoainam/logistics-capstone-project/actions/runs/39896561084).
[Exact counts, inventories, privacy/cleanup and artifact digests](../../docs/implementation/t-002-05-identity-application-evidence.md#provisioning-and-secure-outbox-ci-closure---2026-10-23).
Architecture23 PASS/0 skips; H2318 PASS/17 expected skips; PG453 PASS/5 expected
skips; zero FAIL/ERROR. Provisioning12 unit +18 PG, crypto10, secure outbox15,
migration25 (including V5), Domain48 and all85 Grant/Application methods PASS.
Previously failing concurrency method PASS. Three ZIP digests match GitHub metadata;
110 report-policy controls per job, credential/privacy and owned cleanup PASS.
This actual CI supersedes the earlier future/pending statements only for this
source checkpoint; subsequent documentation edits are not CI-tested.

T-002-05 remains PARTIAL/unchecked; Feature002 and AC-002-01..05 NOT ACCEPTED.
Email sender/worker, maintenance/scheduled purge, invitation consumption, password
setup/activation and remaining Application use cases stay pending.
GRANT_MUTATION_FUNCTIONALITY_LIMITED remains. No production delivery/bootstrap,
HTTP/API completion, full feature acceptance or new publication is implied.


## Invitation consumption / initial activation ? 2026-10-23

OWNER_T_002_05_INVITATION_ACTIVATION: GO; bounded Application source
IMPLEMENTED_VERIFICATION_PENDING. [Scope, exact tests/files and limitations](../../docs/implementation/t-002-05-invitation-activation-evidence.md).

- [x] Author inward application/store/crypto ports and explicit runtime JDBC composition, reusing Domain password/account/proof policy and V3-V5.
- [x] Author atomic proof authentication/consumption, BCrypt password setup, ACTIVE account/version/authVersion, revocation/audit/proof-bound receipt and obsolete payload clearing; no Realm activation, HTTP or login.
- [x] Author12 unit +20 PostgreSQL activation cases; retain all85 existing grant/provision/outbox PG test bodies and three-suite cleanup protocol.
- [x] Execute113 Python report-policy and15 migration-source controls; exact inventory/expected skips preserved.
- [ ] Resolve/carry Windows raw-byte source-guard limitation:9/10 Outbox source methods PASS, V3/V4 CRLF checkout differs from pinned LF blobs despite no SQL diff.
- [ ] Execute current-source Java compile/ArchUnit/H2/PostgreSQL/crypto/transaction/races, exact reports, runtime privacy and owned cleanup on an approved runner.
- [ ] Complete other canonical Application use cases; HTTP proof locator/admission stays T-002-06, actual sender/maintenance T-002-08.

T-002-05 stays PARTIAL and canonical checkbox unchecked; Feature002 and
AC-002-01..05 NOT ACCEPTED. GRANT_MUTATION_FUNCTIONALITY_LIMITED remains.
Future business-feature scope requires separate owner review when dependencies
are actually ready; no Merchant/Business Admin/Shipper implementation here.


## T-002-05 bounded Session Application - 2026-10-23

OWNER_IDENTITY_SESSION_APPLICATION: CONDITIONAL_GO.
[Current source/evidence and PostgreSQL handoff](../../docs/implementation/t-002-05-identity-session-evidence.md).
T_002_05_SESSION_APPLICATION_IMPLEMENTED_VERIFICATION_PENDING.

- [x] Implement backend login/refresh/logout, mandatory trusted admission port and real BCrypt/opaque-refresh crypto; no production HTTP/JWT registration.
- [x] Implement same-DataSource session/family/audit atomicity, strict replay commit-before-denial, current account/authVersion/Realm checks and scoped logout.
- [x] Author20 unit +18 PostgreSQL cases; preserve105 previous integration methods, immutable V1-V5 and three-suite cleanup registry.
- [x] Compile/run focused55 Java tests;116 policy and15 migration static controls PASS.
- [ ] Verify current-source PostgreSQL transaction/rollback/races, exact reports and credential/privacy/owned cleanup on an authorized Docker runtime/CI.
- [ ] Complete remaining current-session/activity, recovery/resend/lifecycle/query and authoritative security use cases; later HTTP integration stays T-002-06.

Canonical T-002-05 checkbox stays open/PARTIAL; original DoD/REQ/AC unchanged.
Feature002/AC-002-01..05 NOT ACCEPTED; GRANT_MUTATION_FUNCTIONALITY_LIMITED retained.
No automatic publication, dispatch, new business feature or production deployment.

Session local full default/H2 clean verify:350 PASS/0 FAIL/0 ERROR/17 expected
skips; actual strict report guard exit0. This does not prove PostgreSQL or new
GitHub CI. Current-source Session PostgreSQL18 cases remain UNVERIFIED.


## Current-session/activity Application checkpoint - 2026-10-24

Owner: OWNER_IDENTITY_CURRENT_SESSION_ACTIVITY: CONDITIONAL_GO.
[Exact scope/test inventory/evidence](../../docs/implementation/t-002-05-current-session-activity-evidence.md).
T_002_05_CURRENT_SESSION_ACTIVITY_IMPLEMENTED_VERIFICATION_PENDING.

- [x] Implement approved Account current-session lookup and Domain meaningful idle accounting behind required trusted admission; no HTTP/JWT/default principal.
- [x] Preserve owned-transaction guard, account/authVersion/Realm/deadline checks, strict replay and all existing128 PG methods.
- [x] Run21 new unit cases; full local Java/H2375 PASS/17 expected skips; Architecture23 and119 report-policy controls PASS.
- [x] Author/compile14 new PostgreSQL cases with independent-state, rollback and controlled concurrency assertions; extend exact finite inventory.
- [ ] Execute current-source PG cases and142 combined methods with strict reports/privacy/owned cleanup on an authorized runtime/CI.
- [ ] Supply real T-002-06 production access and meaningful-action admission, HTTP/JWT/cookie/CSRF/throttling adapters.

Activity feed/audit-query pagination is not a contract operation in this slice.
Recovery/resend/remaining lifecycle/query/authority and T-002-08 delivery remain
pending. Canonical T-002-05 stays unchecked/PARTIAL; Feature002/AC-002-01..05 NOT
ACCEPTED; GRANT_MUTATION_FUNCTIONALITY_LIMITED retained. No source CI, publication,
next business feature or new endpoint is accepted by local unit evidence.


## Remaining Application checkpoint - 2026-10-24

Owner: OWNER_T_002_05_REMAINING_APPLICATION: CONDITIONAL_GO.
[Canonical operation/dependency matrix and evidence](../../docs/implementation/t-002-05-remaining-application-evidence.md).

- [x] Revalidate actual PR25 develop push CI40069216714 on899cf8f211c1a8b52441e01849bcfefbdbee6162, including exact reports/privacy/cleanup.
- [x] Verify seven Application operations: list/get account, role/scope catalogs, disable/reactivate, resend invitation.
- [x] Verify36 new unit and34 real PostgreSQL cases; all previous142 PG methods stay mandatory, combined176.
- [x] Verify unchanged Domain/Migration/Session/Activation/Provisioning/Outbox/Realm/Grant regressions and exact finite report policy.
- [ ] Publish scoped source, verify real PostgreSQL CI, separately close evidence, then create one PR and verify actual integration checks.
- [ ] Resolve affected Recovery storage/crypto dependency via reviewed forward migration/owner decision; V1-V5 remain immutable.

Predicted counts411 H2/17 expected skips,637 PG/5 expected skips are NOT actual
runtime results. Original T-002-05 remains unchecked/PARTIAL. Feature002 and all
AC-002-01..05 NOT ACCEPTED; GRANT_MUTATION_FUNCTIONALITY_LIMITED retained.
No automatic PR merge or business-module implementation is authorized.


Remaining Application source CI checkpoint: 0f49ff8d0b5329437cf783b8f632d4a844ece58b,
run40072363351 attempt1 PASS:23 Architecture/411 H2/637 PostgreSQL,0 FAIL/ERROR,
17/5 exact skips,122 report controls/job, privacy/owned cleanup PASS. Previous
prediction lines are historical. Seven-operation verification gates above are
complete; combined publication/PR-integration gate remains pending until the
actual PR checks pass. Recovery dependency remains open; original T-002-05 stays
unchecked/PARTIAL and original Feature002/AC acceptance is unchanged.


## T-002-06 bounded HTTP/security checkpoint ? 2026-10-24

Owner: OWNER_T_002_06_SECURITY_DESIGN: CONDITIONAL_GO. T-002-06 PARTIAL/unchecked.
[Scope and actual-versus-pending verification](../../docs/implementation/t-002-06-http-security-evidence.md),
[ADR-0012](../../docs/architecture/decisions/ADR-0012-production-identity-http-security.md)
and [runbook](../../docs/runbooks/production-identity-security.md). Eight accepted operations
implemented independently of Recovery: me/account list/object/role/scope reads and grants/disable/
reactivate. External RS256 keys, authoritative per-request admission, exact request/error mappings,
no polling/replay idle extension. 43 unit/18 PG methods authored; current PostgreSQL/CI pending.
Full T-002-06 DoD remains open for browser auth/cookie/CSRF/shared limiter, protected issuance/
invitation compatibility and Recovery dependencies. No task checkbox or original REQ/AC changed;
T-002-05 PARTIAL, Feature002/AC-002-01..05 NOT ACCEPTED, grant limitation unchanged.


### T-002-06 current-source CI checkpoint ? 2026-10-24

Source1962a839fce6e601d0f541e094a1dc9d2c663c05,
[run40103151184](https://github.com/phamquoainam/logistics-capstone-project/actions/runs/40103151184),
attempt1/workflow_dispatch: Architecture23 PASS/0 skips, H2454/17 expected skips,
PostgreSQL698/5 expected skips,0 failures/errors,125 policy controls/job. Exact inventory,
all three artifact SHA-256 digests, credential/privacy and owned cleanup PASS. New43 security
methods and18 PG/filter-chain cases verified; prior176 PG retained, combined194 PG PASS.
Eight-operation boundary CI_VERIFIED; earlier current-source CI pending statements are historical.
Full T-002-06 PARTIAL/unchecked; browser auth/CSRF/shared limiter/issuance/compatibility/Recovery
gates and original Feature002/AC-002-01..05 remain OPEN/NOT ACCEPTED; grant limitation unchanged.
This later documentation revision is NOT the source run's checkout; PR integration remains required.
