# Demo 001 — Execution checkpoints

- [x] DEMO-T01: Read source/specs; define isolated demo contract and explicitly deferred scope (user authorized).
- [x] DEMO-T02: Checkpoint 1 — Flyway demo model, local seed/launcher, signed-token auth/RBAC; Maven verify PASS 2026-10-13 (authentication integration suite + original contextLoads). Cache is isolated on D; sandbox network denial was resolved by the authorized unsandboxed build.
- [x] DEMO-T03: Checkpoint 2 — merchant API and DRAFT create/list/detail/cancel; Maven verify PASS, 13 tests, 0 failures/errors/skipped (includes concurrent create, cross-merchant 404, DB constraints and replay/conflict).
- [x] DEMO-T04: Checkpoint 3 — web typecheck/build PASS; 3 Edge browser tests PASS; H2 file persistence, immutable creation event and operation receipt survived restart.
- [x] DEMO-T05: Commands/results, walkthrough and P0 limitations recorded in docs/runbooks/demo-local.md and docs/implementation/demo-001-evidence.md; demo trace appended without changing original AC.

Do not mark any original feature AC DONE. The initial Demo-001 tests above ran on isolated H2. Subsequent D1-03 PostgreSQL integration is verified separately: 22 backend tests (21 DB tests plus original contextLoads), three browser tests, migration/role/persistence checks PASS; H2 regression preserved. [Evidence](../../docs/implementation/d1-03-postgresql-evidence.md). D1-03 remains PARTIAL for its original outstanding gates.
