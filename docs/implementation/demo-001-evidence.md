# Demo 001 — Evidence, 2026-10-13

**Subsequent D1-03 integration:** Demo-001 was merged into develop at 08c0c34. Approved PostgreSQL foundation now passes real PostgreSQL regression/migration/roles/restart/browser verification while H2 remains operational. See [D1-03 evidence](d1-03-postgresql-evidence.md); sections below preserve the initial H2-only execution history, not current PostgreSQL readiness. Full feature/Day 1 closure remains pending.

Branch `docs/sdd-baseline-and-runbooks`. No commit/push. Existing Wrapper/day-plan/local-runbook changes were preserved. User explicitly authorized a demo vertical slice and H2 file fallback; original P0 spec/AC are unchanged.

## Verified result
| Gate | Actual command / evidence | Result |
|---|---|---|
| Backend checkpoints | `mvnw.cmd -U -Dmaven.repo.local=D:\TLCN\e-logistic\.demo\maven-repository verify`; checkpoint 1 7 tests, checkpoint 2 13 tests | PASS |
| Final backend | `mvnw.cmd -o -B -ntp -Dmaven.repo.local=D:\TLCN\e-logistic\.demo\maven-repository verify` | PASS: 14 tests, 0 failures/errors/skipped; 20:00:19 +07 |
| Original baseline | BackendApplicationTests.contextLoads now also asserts no datasource outside demo | PASS |
| Auth/RBAC | DemoAuthenticationTests: BCrypt, wrong/disabled login, 401/403, Admin vs Finance, fresh status/role, tampering, injection, throttling, signed expired token | PASS: 7 tests |
| Shipment | DemoShipmentTests: ownership, create/read, replay/conflicts, input rejection, cancellation/state guard, concurrent duplicate creation, database constraints/admin summary | PASS: 6 tests |
| Web install | npm 11.6.2, Node 24.13.0; npm install using D cache | PASS after pinning compatible React DOM types; no force/legacy-peer-deps |
| Web typecheck/build | `npm.cmd run typecheck`; `npm.cmd run build` | PASS, Vite 8.3.4; 25 modules, JS ~253.67 kB / gzip ~79.87 kB |
| Browser render / APIs | Playwright on installed Edge against localhost:5173 + real backend/H2 file | PASS: 3 tests; final rerun 5.8 seconds |
| Browser negative/fault | A/B isolation 404, wrong login retains username, admin permissions, failed network preserves form and retry | PASS within those 3 tests |
| Persistence | `python app/backend/smoke-demo.py create`, stop/restart backend, `... verify` | PASS: same shipment ID, timestamp, one creation event and persistent operation receipt |
| Persistence record | ID `5e98258b-f5d3-44e0-9234-574605ca0034`; reference `PERSIST-51f90ead5948` | DRAFT retained after restart |
| Flyway demo rerun | Runtime log: validated 1 migration; public history version 1; no migration necessary on restart | PASS for H2 demo |
| Boundary inspection | Read-only source import scan | No cross-module internal imports found; automated P0 architecture gate remains UNVERIFIED |
| PostgreSQL/Flyway foundation | No native DB/catalog/data mutations; no PostgreSQL migration tests | UNVERIFIED, not substituted by H2 |
| Frontend lint / full P0 | No lint suite or full P0 qualification claimed | UNVERIFIED |

## Resolved failures and remaining warning
Initial Maven attempt in sandbox failed network `Permission denied: getsockopt`; authorized unsandboxed resolve succeeded. No global Maven settings/cache deletion/security bypass.
Initial npm install had a peer mismatch because a broad @types/react-dom range selected 19.3.0; pinned 19.2.3 compatible with @types/react 19.2.18. Initial TS2882 was fixed with Vite client type declarations.
A repackage attempt failed because the demo Java process held the JAR on Windows. Stopped only that demo process and reran successfully; no database reset.
Flyway warns about H2 2.4.240 being newer than its verified H2 baseline. No dependency versions were changed to hide this; scoped results and follow-up recorded in runbook.

## Review artifacts / operation
Screenshots `.demo/evidence/merchant-draft.png`, `admin-overview.png`; persistence proof `.demo/evidence/persistence-proof.json`; log `.demo/backend-verify.log`, `.demo/demo-backend.log`. All ignored; no credentials printed or committed. UI screenshots visually inspected.
Runbook: [demo-local.md](../runbooks/demo-local.md). Demo contracts/model and intentionally deferred requirements: [design](https://github.com/phamquoainam/logistics-capstone-project/blob/eeb7833ab678240581c407ccd167d7e61ea873c6/specs/demo-001-auth-merchant-shipment/design.md), [spec](https://github.com/phamquoainam/logistics-capstone-project/blob/eeb7833ab678240581c407ccd167d7e61ea873c6/specs/demo-001-auth-merchant-shipment/spec.md). Checkpoints are demo-only; original features 002/003/005 are not marked DONE.
