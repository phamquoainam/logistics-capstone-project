# D1-04 — Architecture mapping and verification

2026-10-13 (+07). Baseline: clean `develop`, HEAD and remote `origin/develop`
`ccafeb4c339ffdfeb23181c245440b61176c2c31` (PR #3 merges D1-03). Remote HEAD checked
with `git ls-remote`; required Compose/bootstrap/V1/V2/config/integration/evidence
files exist in HEAD. No local or remote D1-04 branch existed. Created
`feature/d1-04-architecture-verification` from that HEAD; no commit/push/PR/merge.

## Mapping before implementation

Root is `com.elogistic.backend`, not the illustrative `com.example.elogistics`.
One backend artifact; `BackendApplication` is its composition root. Entries below
describe production Java, not `.gitkeep` directories. Sources: module catalog,
code-architecture, ADR-0001, accepted Demo-001 and D1-03 design, feature 001.

| Module | Actual base (under root) | Implemented public API | Internal layers with Java | Allowed collaborators from catalog | Status |
|---|---|---|---|---|---|
| identity | `identity` | `api.AccountAccess`, `api.AccountView` | application, infrastructure, web; no domain classes | No business module; technical platform API | PARTIALLY IMPLEMENTED: demo auth/accounts only |
| merchant | `merchant` | `api.MerchantAccess`, `api.MerchantView` | application (seed), infrastructure, web; no domain classes | identity public API; technical platform API | PARTIALLY IMPLEMENTED: demo profile query |
| order | `orders` (historical plural) | None: `api` is placeholder | application, domain, infrastructure, web | merchant, pricing public API; technical platform API | PARTIALLY IMPLEMENTED: owned DRAFT/idempotency/cancel |
| pricing | None | None | None | No business module | NOT YET IMPLEMENTED; boundary NOT APPLICABLE |
| warehouse | `wms` placeholder only | None | None | No reverse order/finance | NOT YET IMPLEMENTED; boundary NOT APPLICABLE |
| dispatch | `dispatch` placeholder only | None | None | order public API | NOT YET IMPLEMENTED; boundary NOT APPLICABLE |
| finance | `finance` placeholder only | None | None | No reverse order/warehouse | NOT YET IMPLEMENTED; boundary NOT APPLICABLE |
| routing | None | None | None | dispatch public API | NOT YET IMPLEMENTED; boundary NOT APPLICABLE |
| notification | `notifications` placeholder only | None | None | Public events of order/warehouse/finance/dispatch/routing; identity API | NOT YET IMPLEMENTED; boundary NOT APPLICABLE |
| platform (technical) | `platform` | `api.DemoException` | internal migration job, internal.web; other directories placeholders | No business module | PARTIALLY IMPLEMENTED: migration/error/health helpers |
| workflow (coordinator) | None | None | None | Participating modules' public APIs only | NOT YET IMPLEMENTED; boundary NOT APPLICABLE |

`shipping` is an empty historical folder, not an approved tenth business module.
No module renaming or nine-module skeleton generation is authorized here.
PostgreSQL schema ownership already exists for all nine contexts, but a schema
does not prove a Java module boundary. DB ownership/transaction gates belong to
D1-03; ArchUnit cannot prove SQL ownership or runtime atomicity.

## Review findings before rules

- No observed cross-module internal import. Real graph: orders → merchant,
  identity, platform; merchant → identity, platform; identity → platform.
- **Awaiting approval:** orders imports `identity.api.AccountView` for the trusted
  actor. This is present in Demo-001 but absent from the catalog's compile-time
  allowlist. Do not silently expand the allowlist or suppress the violation.
- Identity LoginService/DemoAccountSeed depend directly on AccountRepository;
  AuthController calls that adapter directly. Orders ShipmentService depends on
  ShipmentRepository and its nested records; ShipmentController exposes two of
  those records. These are same-module dependencies, but violate the documented
  inward adapter direction. Minimal possible repair: internal application ports,
  adapter implements port, response record fields unchanged, seed adapters under
  infrastructure. No cross-module contract or business behavior change.
- Merchant seed performs JDBC in application. It is a technical seed adapter and
  can move inside the same module's infrastructure without changing SQL/order.
- Domain's Jakarta validation annotations are supported by the selective model;
  Spring Web/JDBC/JPA and adapter/application dependencies are forbidden there.

## Approved solution and current mapping

The user approved minimal trusted-actor collaboration and small refactoring after
reviewing the catalog conflict. Orders uses exactly `id/enabled/role/merchantId`;
it does not use username/displayName. Added `identity.api.AuthenticatedActor` and
made the existing AccountView implement it. Orders controller/application now use
that interface, never AccountView. `@AuthenticationPrincipal` receives the existing
DB-validated principal; token decoder/account lookup/role checks are unchanged.
No actor binds from request and no Orders domain imports Identity. Exact type and
boundary locations are enforced, not an allowlist for the whole Identity API.
See [ADR-0004](../architecture/decisions/ADR-0004-architecture-verification.md)
and the updated module catalog.

Identity AccountQueries and Orders ShipmentStore are internal application ports.
Repository adapters implement them; nested response/receipt records moved to the
ports with identical fields. Controllers/services do not import adapters. Seed
runners moved to their own infrastructure packages, same SQL/profile/order and
transaction annotations. Merchant consequently has no implemented application
layer; identity/order application now include `port`. Existing `api/internal`
boundaries and missing-module classifications in the baseline mapping remain.
No HTTP contract, migration, SQL, transaction caller, RBAC or ownership change.

## Rules and non-vacuous production coverage

Final imports: **37 production classes** (27 Java files), excluding test output.
Six API classes, 12 application classes, three domain classes, eight web classes,
six infrastructure classes, root application and platform migration job. Four
actual controllers. ArchitectureTests asserts exact implemented-module inventory,
actual ShipmentService/controller presence and nonempty layer predicates used by
the rules. Missing modules are not counted as verified.

| Rule / production test | Real scope | Violation checked / fixture evidence |
|---|---|---|
| PACKAGE_SHAPE / packagesMatchApprovedModuleStructure | 37 classes; only BackendApplication at root | Unknown module/layer fails; exact module inventory also asserted |
| MODULE_BOUNDARIES / modulesOnlyUsePublicApis | Four implemented packages | Finance → Orders internal ShipmentRepository detected |
| API_ENCAPSULATION / publicApisDoNotLeakInternals | Six API classes | Public Finance API exposes Orders internal ShipmentView detected |
| MODULE_DIRECTION / modulesFollowApprovedDependencyDirection | Four implemented packages | Identity → merchant API; Orders → broad AccountView; Orders domain → AuthenticatedActor detected; application → minimal actor accepted |
| MODULE_CYCLES / moduleGraphHasNoCycles | identity/merchant/orders/platform slices | Finance API ↔ pricing API fixture cycle detected |
| DOMAIN_INDEPENDENCE / domainIsIndependentOfOuterLayers | Three Orders domain records | Domain → JdbcTemplate detected; also bans Spring, SQL/JPA and outer module layers |
| APPLICATION_DIRECTION / applicationDependsInward | 12 application classes, including ports | Application → own adapter detected; also bans web/JDBC/SQL/JPA |
| WEB_DIRECTION / webDoesNotUsePersistenceAdapters | Eight HTTP/advice classes | Web → own adapter detected |
| INFRASTRUCTURE_DIRECTION / infrastructureDoesNotDependOnWeb | Six adapter/seed classes | Infrastructure → own HTTP adapter detected |
| CONTROLLER_TRANSACTIONS / controllersDoNotOwnTransactions | Four controllers | Controller method @Transactional detected |

Selective architecture still permits Spring transaction/security/error facilities
in application and Jakarta validation in domain input values. Infrastructure may
wire application services/implement ports. Platform technical error handling may
map Spring DAO failures. No ignore/suppression/freeze/allowEmptyShould setting.

ArchitectureNegativeFixtureTests: **12 PASS** = 11 deliberate violations detected
and one permitted actor dependency. Every violating fixture runs the exact shared
production ArchRule via evaluate and asserts both hasViolation/message and that
check throws AssertionError. No production violation is injected. Fixture classes
are test-only; the annotated controller fixture uses an inactive dedicated profile
so it is not registered in demo regression contexts. Examples in Surefire output:

```text
Forbidden cross-module implementation dependency:
com.elogistic.backend.finance.internal.application.NegativeInternalDependency
-> com.elogistic.backend.orders.internal.infrastructure.ShipmentRepository
Cycle detected: Slice finance -> Slice pricing -> Slice finance
```

## Execution evidence — 2026-10-13/14 (+07)

Java 21.0.10 / Boot 4.1.1 / wrapper Maven 3.10.0 / PostgreSQL 18.6 unchanged.
ArchUnit core **1.5.1** is test-scope only; existing Jupiter 6.0.3/Surefire 3.5.6
run ordinary @Test methods, no additional test engine. Cache is the existing
ignored `.demo/maven-repository`; no global settings/cache deletion/SSL bypass.

Commands below run from repo root unless a working directory is specified.

| Actual command / gate | Result | Evidence / limit |
|---|---|---|
| Git branch/status/log -5/rev-parse HEAD/origin/develop; ls-remote and merged-file checks; switch -c | PASS | Baseline ccafeb4; no existing branch or dirty files |
| backend: `mvnw.cmd -B -ntp -Dmaven.repo.local=<repo>/.demo/maven-repository -DskipTests test-compile` | PASS | New test dependency resolved; existing remote metadata warnings 401/timeout did not block |
| backend: `mvnw.cmd -o -B -ntp -Dmaven.repo.local=<cache> -Dtest=ArchitectureTests,ArchitectureNegativeFixtureTests test` before repair | FAIL, expected discovery | Eight fixture tests PASS; three production tests FAIL on real direction/adapter violations; no bypass |
| backend: offline `clean test` | BLOCKED at plugin resolution | Clean plugin 3.5.0 was not cached; resolved online next, no source/config problem |
| backend: online `mvnw.cmd -B -ntp -Dmaven.repo.local=<cache> -Ddebug=false -Dtrace=false clean verify` | PASS | Fresh compile removes obsolete seed bytecode; initial full baseline green |
| backend: final `mvnw.cmd -o -B -ntp -Dmaven.repo.local=<cache> -Ddebug=false -Dtrace=false verify` | PASS | 00:05:30 14/10: 48 discovered, 40 executed/pass, eight PG-only skipped; zero failures/errors |
| `powershell.exe -NoProfile -ExecutionPolicy Bypass -File app/backend/verify-postgres.ps1 -Offline` final | PASS | 00:06:23 14/10: **48 tests, zero failure/error/skip**; 23 architecture, one context, seven auth, two security, seven shipment, eight PG foundation |
| Production architecture | PASS | 11 tests: ten shared rules plus explicit scope guards; real module graph acyclic |
| Trusted principal regression | PASS on H2 and PG | actorMerchantOwnershipIsRecheckedFromDatabase: existing token follows changed DB merchant scope, old-owner access 404; finally restores isolated test account |
| Existing business/security regression | PASS | Auth tamper/expiry/disabled/live role; forged owner/status refused; A/B isolation, idempotency/conflicts, concurrent create/cancel, admin responses, DB permissions/FKs/checksums/upgrade |
| DemoSecurityEvidenceTests | PASS on H2 and PG | Captured application output excludes password/signing secret/bearer/rejected password; missing/weak config fails active demo context, valid config instantiates TokenService |
| Installed Docker Desktop start; docker version/ps/inspect; docker start existing elogistics-d1-postgres18 | PASS | Initially Engine stopped; Engine 29.6.2, PG healthy afterwards. Same container/config/volume, no prune/reset/native changes |
| web: `npm.cmd run typecheck`; `npm.cmd run build` | PASS | Existing React/Vite app, 25 bundled modules; no web/lock changes |
| `run-postgres-demo.ps1 -RuntimeOnly`; web `npm.cmd run dev` | PASS | Existing dev DB, migration/seed disabled, health UP; backend 8080/Vite 5173 |
| web: `npm.cmd run test:e2e` | PASS | Three installed Edge tests, 8.8s; real PostgreSQL APIs/render/A-B isolation/admin/same-operation retry. Existing Node color-env warning nonfatal |

Initial added test was accidentally duplicated (compilation FAIL, corrected).
First fail-fast fixture omitted demo profile and therefore did not create its
service (test FAIL, corrected and positive activation assertion added). Neither
failure was fixed by loosening production behavior; final reports above are green.
Test DBs from failed runs remain; no deletion. Final test DB
`elogistics_test_fad8435bd7d6`, upgrade `elogistics_test_upgrade_fad8435bd7d6`.

Ignored artifacts: `.demo/d1-04-initial-architecture.log`,
`d1-04-h2-complete.log`, `d1-04-postgres-complete.log`,
`d1-04-backend-runtime.log`, `d1-04-web-smoke.log`;
`.demo/d1-04/surefire-h2-complete/reports` and
`surefire-postgres-complete/reports`. Current target Surefire reports are final PG.
No raw auth traces shared. Final security/artifact audit appended below.

### Final bounded audit and operational state

`python .demo/d1-04/audit-local.py` PASS: 303 Git-visible files checked against
24 private generated values and selected private-key/GitHub-token/AWS patterns;
47 artifact class/resource entries checked; three application/build logs and two
credential templates checked. No private value/pattern match; no ArchUnit/test
fixtures in the runtime JAR. Templates retain placeholders. Audit script and
count-only result `.demo/d1-04/secret-audit.log` are ignored local evidence.
Initial template probe incorrectly treated a secrets-directory path as a credential
and compared placeholder prefixes case-sensitively; corrected the probe, not the
protected template. This is not an exhaustive Git-history/provider/PII audit.

Final `git diff --check` PASS; protected migrations/resources, Compose/bootstrap,
wrapper and frontend/lock paths have no diff. 44 changed paths (counting each seed
move's old/new paths separately); full inventory `.demo/d1-04/changed-files.txt`.
Changes are POM, minimal actor, two application ports and their consumers/adapters,
two seed moves, architecture/negative/security/regression tests, catalog/ADR,
plan/evidence/runbook/traceability and feature metadata. No commit/push/PR/merge;
HEAD remains baseline ccafeb4 on the new feature branch, with this work uncommitted.
PostgreSQL healthy on 127.0.0.1:5433; runtime-only backend 8080 and Vite 5173 remain
running for review. Native PostgreSQL and previous containers/volumes untouched.
Browser tests only add synthetic demo fixtures; no pre-existing data reset.

## Violations and remediation

| Initial production finding | Dependency path | Severity | Final resolution |
|---|---|---|---|
| Unapproved Identity coupling | ShipmentController/ShipmentService → AccountView | High: undeclared collaboration | User-approved minimal AuthenticatedActor only at boundary; broad/domain use has negative tests |
| Core depends on persistence | LoginService/DemoAccountSeed → AccountRepository; ShipmentService → ShipmentRepository and nested records | High: inward direction broken | Application ports implemented by same adapters; technical seeds moved inward-independent infrastructure |
| Application performs JDBC | DemoMerchantSeed → JdbcTemplate | Medium: seed misplaced | Same module infrastructure, unchanged SQL/runner behavior |
| HTTP uses persistence | AuthController → AccountRepository; ShipmentController → repository Summary/Overview | High: adapter leakage | Internal application ports/identical response records |

No unresolved violation of the implemented mandatory rules. No large refactor,
module renaming, new business module, migration or public HTTP change.

## Closure reconciliation

| Scope | Assessment | Remaining conditions |
|---|---|---|
| D1-03 approved PostgreSQL local milestone | PASS / completed milestone | D1-03 historical cold-start/rerun/restart evidence retained; current PG regression also PASS |
| D1-03 build/test/migration deliverables | PASS locally | Does not independently prove clean-machine reproduction or all environment prerequisites |
| D1-03 overall Day 1 task | PARTIAL, unchanged | Listed D1-02 dependency and clean-machine reproduction remain unresolved; merge/architecture PASS does not waive them |
| D1-04 approved local architecture verification | PASS; propose DONE | All execution requirements F met; reviewer closure remains a proposal, not feature 001 acceptance |
| REQ-001-03 / AC-001-03 | PARTIAL | Local forbidden-import detection/clean pass proven; GitHub CI workflow and CI execution absent |
| Cross-cutting Day 1 / feature 001 | PARTIAL | Native development build, mobile lint/test, accepted pins/log disposition, clean clone by another developer, CI, OpenAPI/client/health infrastructure scope remain open |

Six unimplemented business modules and absent workflow have no production-boundary
PASS claim. No AC edits or implicit DEFERRED status. ArchUnit-only is approved for
this slice; full DEC-01/02 and unrelated architecture/provider choices remain open.
Next independent task: **D1-05 CI foundation slice for REQ-001-03/AC-001-03**, running
the Maven architecture gate on a clean runner and recording actual CI evidence;
prepare its exact scope/DoR before implementation. No CI deployment/push performed.
