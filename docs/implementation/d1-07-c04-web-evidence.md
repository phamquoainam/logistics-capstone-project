# D1-07 / C-04 — Existing web adoption of the generated Fetch SDK

Execution2026-10-16. **C-04 DONE for the approved local web scope**. C-01–03
remain DONE locally; C-05/mobile consumer and C-06/final combined gates NOT STARTED.
D1-07, Feature001 and Day1 remain PARTIAL. No parent or slice REQ/AC text changed.

## C-03 acceptance and authorized checkpoint

Branch feature/d1-07-demo-contract-client, initial HEAD
b3852440e0454c549ddee4fd72fd6ac302b6b381. Reviewed AGENTS/DoR, accepted DEC-29,
bounded ADR-0003, slice spec/design/tasks and actual web source/tests.
Exactly14 C-03 files matched the [evidence inventory](d1-07-c03-wire-evidence.md#c-03-file-inventory):
10 modified/4 new, index initially empty. Reviewed diff/harness ownership,
UTF-8/relative files/whitespace and bounded secret/artifact checks. OAS, generated
SDK, backend, migrations, workflow and lockfiles unchanged. Final two safe wire
reports retain identical SHA256,27 PASS/108 responses each; cleanup flags all true.
The earlier failed attempts and4 HIGH dev findings remain disclosed, not waived.

User's current task explicitly authorized stage/commit after review PASS.
Staged only the14-file hash-checked allowlist and committed:
**a2609f995d871608e4ae35c831976413bd360bf9**,
`test(api): verify Demo-001 HTTP contract conformance`.
Working tree clean immediately after this checkpoint. No push/PR/merge.
C-04 changes below remain unstaged/uncommitted at that same HEAD.
Earlier C-03 documents' uncommitted statements describe their previous checkpoint.

## Existing call → generated operation → adapter / regression

Prefix remains /api/demo/v1. All imports use the package's public root or /client
exports; no generated-internal consumer import or handwritten response DTO copy.

| Existing call / consumer | Generated operation | Web adapter / verification |
|---|---|---|
| GET /health; existing API test, no health UI | getDemoHealth | health; anonymous-header unit and actual browser SDK read |
| POST /auth/login; Login | loginDemo | login; request body/JWT result, invalid login/input retention/admin browser |
| GET /auth/me; Login and60-second polling | getDemoCurrentUser | currentUser; per-call bearer, initial load/polling/logout/expired-token browser |
| GET /merchant/me; Dashboard | getDemoCurrentMerchant | merchant; real A/B dashboards |
| GET /shipments; Dashboard/List | listDemoShipments | shipments; real owner list and retry regression |
| POST /shipments; CreateShipment | createDemoShipment | createShipment; unchanged numeric weight/raw strings/key, actual201/replay409/201 |
| GET /shipments/{id}; ShipmentDetail | getDemoShipment | shipment; memoized callback/id, actual detail/owner404/history |
| POST /shipments/{id}/cancel; ShipmentDetail | cancelDemoShipment | cancelShipment; raw reason/key, actual200/replayed200/one cancel event |
| GET /admin/users; Users | listDemoUsers | users; real sanitized users/disabled row |
| GET /admin/overview; AdminDashboard | getDemoOverview | overview; real admin UI, actual merchant403 |
| GET /admin/shipments; AdminDashboard | listDemoShipmentSummaries | summaries; real admin UI |

Web manifest/lock add file:../../../packages/api-client, no workspace manager,
version upgrade or runtime validation dependency. Lock comparison confirms every
existing version/resolved/integrity unchanged. Only two package entries added:
local package metadata and node_modules/@elogistics/api-client link. npm also
marks the already locked TypeScript as peer; compiler version stays6.0.3.
SDK build precedes consumer install. Public declarations and ESM exports resolve
in the existing web compiler and production Vite bundle. SDK generator/Ajv/Redocly
remain dev tooling outside the shipped web module graph.

api.ts reexports schema-derived aliases for presentation names; useData accepts
typed operation callbacks and infers responses. No generic manual HTTP request,
successful `as T`, new DTO definition, disabled assertion or type suppression.
Each client is configured with the existing relative browser base, JSON parsing
and field-style results; generated operations construct method/path/body/headers.
Normal vite.config.ts/proxy/dev/preview ports remain unchanged. A separate
vite.c04.config.ts only accepts loopback test-backend origin/random test port.

## Compatibility and actual browser evidence

ApiError retains status/code/message and the existing Vietnamese network/nonJSON
failure fallbacks. Generated result errors are unwrapped without exposing raw
proxy HTML. A missing HTTP response becomes status0/NETWORK_ERROR; actual safe
backend errors retain their status/code/message. Valid success DTOs are inferred
from the generated operation. The adapter does not add runtime success-schema
validation; arbitrary malformed2xx responses are outside the frozen contract.
A parser failure on malformed2xx is surfaced with safe API_ERROR fallback,
never the SDK parser's body snippet; a targeted unit fixture verifies this.

JWT is supplied per protected request, never client-global config/interceptor or
storage. Public health/login remain anonymous. React memory-only session, initial
current-user load,60-second polling and existing401-only logout condition remain.
403/404/409/429 do not become login failures; mutation error handling remains at
the same UI call sites. No refresh/session-restoration API or persistence added.
Reload deliberately returns to login, matching existing implementation.

Form values, explicit key rotation button and create/cancel key lifetimes remain
in React. No trim/coercion validator, implicit retry, generated merchant/actor
authority or new status success branch. Weight conversion stays at the UI boundary.

Final browser run **31898e5d9991**, seven tests, zero failures/skips, Edge headless:

1. Existing merchant create/detail/cancel/list and second merchant hidden404.
2. Existing invalid-login/input retention, admin overview/users and logout.
3. Existing network-failure/input retention/retry, strengthened to compare all
   three identical keys across abort, explicit nonJSON503 proxy fixture, success.
4. Actual browser-imported web adapter: health/login/me, merchant403, missing/
   invalid/expired bearer401, second-owner read/cancel404, duplicate reference409
   and sixth bad login429. Readback retains one DRAFT creation event after denials.
5. Current-user polling: explicitly simulated403 retains session; subsequent
   real backend401 from invalid bearer logs out. /auth/me has no normal role403,
   so that branch fixture is not misrepresented as an actual backend403 there.
6. Logout A/login B sends no old bearer on login, displays B, local/sessionStorage
   empty; reload returns to login. Token values never leave in report output.
7. Lost committed create response: server201 then aborted delivery, retained form,
   changed input/same key409, restored input/same key replay201. Lost cancel200
   then retry200 retains reason/key and renders exactly one creation/cancel event.

UI assertions run in the real React app consuming generated SDK. Backend calls
use the actual JAR and fresh PostgreSQL18.6; no mocked SDK substitutes. Only the
explicit network/proxy/403 branch fixtures above are intercepted. Four added
tests assert no pageerror; original merchant regression retains its own error
check. Existing UI/UX and business behavior were not redesigned.

## Isolation, commands and verification matrix

web-harness.py reuses wire-harness.run via an optional source-controlled consumer
callback. Default wire command is unchanged and separately regressed. Each run
owns a new labeled container ID, tmpfs database/roles/passwords, random127.0.0.1
DB/backend/Vite ports. Same approved PG18.6 digest, unchanged V1/V2 and current JAR.
No developer DB, native5432, developer5433, existing mount/volume/credentials.
Vite and Java are captured direct child processes, stopped/reaped in finally;
only the verified new container ID is removed, no -v/prune/down/clean/repair.

Final run DB elogistics_wire_31898e5d9991, PG53937, backend53940, Vite53963.
safe-environment: PG18.6, history1,2, test exit0/log privacyPASS.
safe-web-environment: realBackend=true/viteStopped=true. Four cleanup flags
PASS: backend stopped, owned container removed, all prior container IDs preserved,
volume names unchanged. Previous successful7-test run08c826fc7973 predates the
extra nonJSON/pageerror assertions; d873b35027a1 includes those fixtures.
31898e5d9991 reruns the complete scope after the malformed2xx parser-safety guard.

From packages/api-client unless stated otherwise:

| Actual command / gate | Result |
|---|---|
| Web: `npm.cmd install --package-lock-only --ignore-scripts --cache ../../../.demo/npm-cache` | PASS, minimal local-link lock update |
| Web: `npm.cmd ci --ignore-scripts --no-audit --cache ../../../.demo/npm-cache` | PASS176 packages; no version change |
| Web: `npm.cmd run lint`, `npm.cmd run typecheck` | PASS after final fixture changes |
| Web: `npm.cmd test` | PASS24/24/no skips; original5 +19 targeted/mapping tests |
| Web: `npm.cmd run build` | PASS TypeScript/Vite8.3.4;39 modules,264.62kB JS/83.62kB gzip |
| `npm.cmd run verify:c02` | PASS complete rerun: strict lint/inventory,41 contract +44 SDK tests/no skips, deterministic16 files, typecheck/build |
| `npm.cmd run test:web` | Final PASS7/7/no skips, original3 +4 additional browser tests |
| `npm.cmd run test:wire` after harness reuse | PASS27/27/no skips,108 responses34+/74-,11 operations/zero mismatches; cleanup/privacyPASS |
| Backend Wrapper offline verify + existing report guard | PASS48 discovered/40 executed incl23 architecture,8 named PG-only skips,0 failures/errors |
| Web `npm.cmd audit --json` and `--omit=dev --json` | Both exit0/0 findings in current registry result |
| Client same audits | Full exit1/4 high; runtime exit0/0; OPEN, not remediation |
| Diff/UTF-8/relative files/bounded secrets/generated/protected-path review | PASS; index empty, no C-04 commit |
| Mobile import/consumer, final combined C-06 and remote CI on C-04 | NOT RUN / UNVERIFIED, not this task |

Backend command from app/backend:
`mvnw.cmd -o -B -ntp -Dmaven.repo.local=<repo>/.demo/maven-repository -Ddebug=false -Dtrace=false verify`.
Output captured privately; `python scripts/ci/verify-reports.py --source app/backend/target/surefire-reports --output <ignored safe reports> --mode h2 --maven-exit 0` PASS.
JAR SHA256 unchanged d3b8ffca5fa16c35e394ca677e3aa96a0ab22a6f44ae801e448165710c56d7c9.
This is not a new48/48 PostgreSQL backend-suite claim; actual PG wire tests are
the separate27-test run37f58be476fc with the same108-response result as C-03.

Earlier attempts retained as failures: sandbox npm install fetches returned
EACCES; scoped authorized execution completed clean install. First verify:c02
attempt linted with0 findings but Node/Redocly exited with UV_HANDLE_CLOSING
assertion, so that attempt FAIL. The unchanged full rerun PASS; root cause remains
unproven/intermittent Windows tooling risk, no pin/assertion workaround. First
web runeb04dc2dcd4d failed before browser because test Vite omitted the existing
dev script's --host127.0.0.1; corrected helper, all process/container cleanup PASS.
Neither failed attempt is counted as a passing test run.

## Security, traceability, rollback and next task

Web audit0 does not erase SDK dev graph4 HIGH through js-yaml4.2.0/ref-parser/
shared/openapi-ts. Existing checked-object generation/trap mitigation preserved;
no audit fix/override/pin change or implicit risk acceptance. Maintenance owner:
project/platform. No supported runtime imports those tooling modules. Deprecated
ESLint9 warning is recorded as maintenance, no version change. Redocly announced
newer2.64.1; approved2.64.0 remains installed.

Context/password/signing secret/private backend/Vite logs and browser diagnostics
stay ignored under .demo. Safe reporter only emits test title/status/error count;
no errors/snippets/request values/trace in public output. Explicit screenshots
contain synthetic merchant/admin UI only and use the unique run directory.
Bounded scans of changed source, production JS, safe reports/Vite logs against
fresh secrets and credential/JWT patterns PASS. No claim of exhaustive security
review. Sensitive error objects, if a test fails, remain private/ignored.

TEST-001-CONTRACT-04/08 web portions PASS, TEST03/09 web compiler/quality subset
PASS, SC-03 and SC-AC-02 web journey PASS; SC-AC-03/04 adapter/browser security/
retry subsets PASS alongside C-03. TEST10 H2/architecture/privacy subset refreshed,
full combined/mobile/PG backend regression remains C-06. No parent full requirement
is declared complete by these subset results.

Rollback remains application-only: reverse the reviewed web adapter/call-site,
manifest/lock and test-helper integration patch together; build/validate web.
Do not revert OAS/SDK as a second contract source, backend behavior, data/native
state or migrations. No rollback/reset operation was executed.

Next task is C-05 only after review/instruction: build SDK, add approved local
mobile file dependency and compile/import signature fixture; run mobile clean
install/lint/typecheck/Jest. Keep App.tsx/native/permissions/UI/network unchanged.
C-06 final combined verification and separately authorized publish remain open.

## Exact C-04 file inventory

Twenty-three unstaged files (17 modified/6 new) relative to checkpoint a2609f9:

```text
app/frontend/web/package.json
app/frontend/web/package-lock.json
app/frontend/web/playwright.config.ts
app/frontend/web/src/api.ts
app/frontend/web/src/App.tsx
app/frontend/web/unit-tests/api.spec.ts
app/frontend/web/tests/demo.spec.ts
app/frontend/web/tests/sdk-adoption.spec.ts
app/frontend/web/tests/support/settings.ts
app/frontend/web/tests/support/safe-reporter.ts
app/frontend/web/vite.c04.config.ts
packages/api-client/package.json
packages/api-client/scripts/wire-harness.py
packages/api-client/scripts/web-harness.py
packages/api-client/README.md
docs/implementation/d1-07-c04-web-evidence.md
docs/implementation/day-1-plan.md
docs/quality/traceability.md
docs/runbooks/local-development.md
specs/001-platform-foundation/slices/demo-contract-client/spec.md
specs/001-platform-foundation/slices/demo-contract-client/design.md
specs/001-platform-foundation/slices/demo-contract-client/tasks.md
specs/001-platform-foundation/tasks.md
```

No backend, OAS, generated source/config/client lock, mobile, migration, workflow,
architecture rule or original AC changed. Dist/node_modules/private artifacts
remain ignored. No C-04 stage/commit/push/PR/merge.
