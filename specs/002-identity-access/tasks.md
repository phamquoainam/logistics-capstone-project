# Implementation Tasks 002: Xác thực và phân quyền theo phạm vi

**Tình trạng hiện tại:** T-002-01/02/03/04 DONE theo scope được duyệt; T-002-05 Application READY / NOT STARTED. Production Feature002 và AC-002-01..05 chưa được nghiệm thu. **Rule:** Không tick DONE trước khi có code+test+evidence.
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
