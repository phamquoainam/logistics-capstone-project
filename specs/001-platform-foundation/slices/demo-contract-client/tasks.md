# Tasks 001-CONTRACT — D1-07 handoff

**Status:** slice READY_FOR_IMPLEMENTATION; DEC-29 ACCEPTED on2026-10-15 by the
explicit user approval message recorded in the assessment. D1-06 documentation/
DoR DONE; implementation tasks C-01..06 NOT STARTED and tests NOT RUN.
[Spec](spec.md), [design and test inventory](design.md),
[D1-06 assessment](../../../../docs/implementation/d1-06-slice-readiness.md).
This is one slice in001, not completion of its original full-feature task list.

## Definition of Ready

- [x] Source-backed scope/non-goals, actors and existing journey defined.
- [x] SC-01..05 mapped to parent REQ/NFR and approved Demo-001 FR/BR subsets.
- [x] SC-AC-01..06 and edge/error/replay/security cases documented.
- [x] All11 operations, DTO/property constraints, ownership and auth reviewed.
- [x] Zero DB/migration/event/business-policy impact; accepted boundaries retained.
- [x] Exact tool versions/release times/dependency constraints checked on official registry; bundled Fetch tarball integrity/source presence verified read-only. No install/runtime PASS claim; existing web/mobile TS6 declarations unchanged.
- [x] File scope, tasks, test fixtures, verification/DoD and dependencies explicit.
- [x] Contract source of truth and source/schema/wire/generated-output drift gates defined; existing transport mocks are not schema fixtures.
- [x] Two-directory deterministic regeneration, negative controls and application-only rollback defined; no DB/native/cache reset.
- [x] DEC-29 formal owner approval recorded,2026-10-15, for the revised scope and exact pins; no inferred acceptance.
- [x] Source-drift recheck PASS: HEAD3186f01 unchanged, saved11-operation source hashes match, executable/configuration paths unchanged; slice READY_FOR_IMPLEMENTATION.

All mandatory DoR planning/approval gates PASS. This closes D1-06 only: the user
explicitly requested no installation, SDK generation or D1-07 execution in this
turn. D1-07 runtime/build/wire/regression gates remain mandatory and unexecuted.
No need to approve the existing Git branch again. Publication is not performed.

## Ordered implementation tasks (all tests NOT RUN)

- [ ] **C-01 — Preconditions/contract freeze.** Verify current branch/tree/HEAD and
  DEC-29 acceptance. Re-read exact controllers/records/security/errors and check
  drift since3186f01. Create OpenAPI source/examples for11 approved operations;
  no DTO invention. Test inventory/negative contract fixtures before or with code.
  Deliverables contracts/openapi/demo-001.yaml + package/config/lock.
  Establish the single schema source only after source/fixture review. Gate
  TEST-001-CONTRACT-01; stop/report contract/source conflicts. Positive wire
  fixtures must match current DTOs; legacy transport mocks are not contract truth.
- [ ] **C-02 — Deterministic SDK.** Clean-install exact tooling, audit new graph
  and verify licenses/integrity/engines, generate types+SDK+bundled Fetch and build
  the private package. Tests02/03/04: real operation calls, drift failure and type
  assertions. No peer override/framework downgrade or generic manual HTTP stand-in.
  Dependency C-01. Stop if declared compatibility does not hold in actual build.
  Tools are devDependencies of the new private package; retain both consumers'
  existing TypeScript declarations/locks. Ajv/formats are test-side only. No
  standalone client-fetch dependency, framework upgrade or automatic fallback.
- [ ] **C-03 — Wire conformance.** Run actual isolated demo backend and validate
  all11 operations/safe errors against the OpenAPI with test-side Ajv; reuse
  existing auth/replay/concurrency fixtures. Tests05/06/07; distinguish actual
  SQL assertions from transport mocks. Dependency C-02. No developer DB writes.
- [ ] **C-04 — Existing web adoption.** Replace handwritten wire types/generic
  calls with generated types/operation functions and minimal ApiError adapter;
  retain auth/session/UI/input/key semantics. Tests04/08 + web lint/typecheck/
  existing five unit and three browser regressions + build. Dependencies C-02/03.
- [ ] **C-05 — Shared compiler consumer.** Add private package local dependency
  and compile-only client import/signature fixture to mobile; retain starter UI
  and native baseline. Tests03/09 and current mobile lint/typecheck/Jest. No actual
  mobile backend/device test claim. Dependency C-02.
- [ ] **C-06 — Final regression/evidence.** Run package regeneration/validation/
  test/build and both frontend lockfile install/quality gates. Run backend Maven
  verify, architecture/H2 and isolated PG regression when environment permits.
  Preserve guard criteria; report BLOCKED instead of omitting required execution.
  Record exact commands/counts/skips/versions and scoped traceability, no full001
  DONE. Dependency C-03/04/05. No publish without user request.
  Keep the design's rollback patch boundary ready: consumer code/manifests/locks
  move together; preserve unrelated work and never revert schema/data/native state.

## Expected implementation file scope

Bounded implementation allowlist under accepted DEC-29 (not edits performed by D1-06):

- contracts/openapi/demo-001.yaml and its README/config if necessary.
- packages/api-client/{package.json,package-lock.json,.npmrc,tsconfig.json,
  openapi-ts.config.ts,redocly.yaml,src/index.ts,src/generated/**,tests/**,scripts/**}.
  Dist/node_modules/reports/temp output remain ignored; existing root ignores
  already cover them. Generator-owned TS source may be tracked intentionally.
- app/frontend/web/{package.json,package-lock.json,src/api.ts,src/App.tsx,
  unit-tests/api.spec.ts,tests/demo.spec.ts}; only minimal API-call/type adaptation,
  no visual/behavior redesign. Add test helpers/config only as needed for the
  isolated API conformance harness; explicitly account for all new files.
- app/frontend/shipper-mobile/{package.json,package-lock.json,type-tests/api-client.ts};
  compile-only usage, no App.tsx/native/app.json/permissions change.
- D1-07 evidence, local runbook, traceability, Day1 and subordinate slice metadata.

Not allowed: backend business source/POM/migrations, architecture tests/rules,
workflow/report guards, Docker definitions/volumes, new domain modules/native
configuration/framework versions/AC text. If a truly necessary change falls
outside this allowlist, report the exact reason and seek scoped approval first.

## Definition of Done for this slice

All C-01..06 deliverables verified; all SC-AC-01..06 have actual test/evidence
IDs; contract/generation/drift checks reject negative controls; generated methods
are used by the real existing web journey; no handwritten wire DTO duplicates.
Both consumers compile and lockfile clean installs are reproducible. All required
local tests/builds/regressions pass; no unresolved mandatory contract/security/
compatibility error. Advisory findings remain disclosed with impact/owner.
Evidence records environment/commit/config/counts and sanitized output. Unavailable
required execution is BLOCKED/PARTIAL, not an implicit skip/DEFERRED.

Before merging a separately authorized PR, actual three backend required checks
must PASS on its HEAD and integration conditions hold. Local tests alone do not
claim new frontend/mobile CI. D1-07 local verification and publication/integration
are recorded separately. No native rebuild claim if unchanged APK is reused.

Full001 cold-start/readiness/Redis/Rabbit/Testcontainers/independent onboarding,
literal AC-001-03 red-then-fixed experiment and D1-08 final review remain separate
open gates. Next foundation prioritization occurs after this slice; do not start
a second health/Compose/native slice while implementing this one.

## Copyable D1-07 implementation prompt

> Implement ONLY slice001-CONTRACT after DEC-29 is explicitly accepted and the
> subordinate DoR is READY. Read AGENTS.md, parent001, this spec/design/tasks,
> docs/implementation/d1-06-slice-readiness.md and existing Demo-001 contract.
> Verify safe Git baseline; do not discard any changes. Start C-01 then C-02..06
> in dependency order, tests first/with implementation. Codify current eleven
> /api/demo/v1 operations and flat resource/error JSON, generate the exact-pinned
> local Fetch SDK, adopt it in the existing web journey and compile a mobile
> consumer only. Preserve all auth/RBAC/ownership/idempotency/UI behavior and
> V1/V2. Respect the file allowlist; do not implement readiness/Redis/broker/POD or
> upgrade frameworks. Run the planned validation/generation/type/HTTP/browser/
> backend gates with isolated synthetic data, report actual PASS/FAIL/BLOCKED and
> counts/skips, update evidence/traceability. Stop on contract drift or mandatory
> incompatibility; never weaken tests/report guard. No commit/push/PR/merge unless
> separately requested. Do not mark full Feature001 or Day1 DONE.
