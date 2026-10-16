# D1-07 / C-03 — Actual HTTP wire conformance on isolated PostgreSQL

**C-06 cross-reference - 2026-10-17 /1f26854:** this is the original dated
checkpoint evidence, not a claim all gates below ran again at its original date.
[C-06 final combined evidence](d1-07-c06-final-evidence.md) freshly verifies
C-01-06 local gates, actual PG48/no skips and HTTP/browser repeatability;
C-05 accepted/committed as1f26854. D1-07 PARTIAL / READY_FOR_PR; remote CI/integration
UNVERIFIED. Current audits web0/0, SDK4-high/0, mobile58/35; prior snapshot59 is
retained historically, not silently rewritten as remediation. C-06 uncommitted.

Execution2026-10-15. **C-03 DONE for the approved local HTTP/PG scope**:
two final independent runs,27/27 tests each, no skips,108 actual HTTP responses
each (34 success,74 negative), all11 operations, zero contract mismatches.
C-04–06 NOT STARTED. D1-07, Feature001 and Day1 remain PARTIAL.

## Authorized Git checkpoint and C-01/C-02 assessment

User task “C-01/C-02 Checkpoint & C-03 Actual HTTP Wire Conformance” authorizes
one checkpoint commit after safety review, then C-03, with no push/PR/merge.
Branch remains feature/d1-07-demo-contract-client. Before checkpoint HEAD was
20072b65cc183153bd26a614103ab9e8a442a713. Exact43-file inventory matched the
[C-02 evidence](d1-07-c02-sdk-evidence.md#final-file-inventory); staged exactly
those43 after manifest/hash, whitespace, bounded secret/artifact review and85
passing tests. .demo/authoring script, node_modules, dist and logs were excluded.

Checkpoint commit: **b3852440e0454c549ddee4fd72fd6ac302b6b381**,
`feat(api): establish OpenAPI contract and deterministic SDK`,43 files,
6189 insertions/24 deletions. Working tree was clean immediately afterwards.
C-03 changes remain uncommitted; no second commit, push, PR or merge performed.

C-01 final review remains DONE and C-02 local DONE. Parser enrichment only adds
redundant string types to pattern-only allOf branches already enclosed by string
constraints; equivalence/compiler tests remain PASS. No contract/generated-SDK/generator-config edits
were made for C-03. Ignored .demo/d1-07-c01/author-contract.py remains temporary,
unexecuted/untracked; it is not a pipeline dependency. Four HIGH dev advisories
remain OPEN; no new zero-vulnerability or implicit acceptance claim.

Re-read AGENTS/DoR, accepted DEC-29/ADR-0003 and subordinate spec/design/tasks;
reused source-backed controllers/DTO/security/service/repository/migration and
existing backend regression fixtures. No parent REQ/AC text or business behavior
was changed to obtain conformance.

## Isolation and actual backend

New code lives only in the private client package scripts/tests; no backend
test/POM/CI allowlist changes. Python3.14, Node24.13.0, npm11.6.2, existing Java21,
Maven Wrapper3.10.0 and Boot4.1.1 are retained. No dependency added/pin changed.
Docker29.6.2 Engine PASS; installed CLI fallback is discovered from LOCALAPPDATA
when absent on PATH. Sandbox denied direct CLI access; approved scoped execution
was used, not installation or system configuration changes.

Image matches approved foundation:
`postgres:18.6-bookworm@sha256:afc7e2d441324c0388fa80c3d24f733b4194a4eb7f47dd8ee2b08eb1a24a647c`.
Actual server reports18.6 (Debian18.6-1.pgdg12+2). --pull=never uses the existing
image. Every run creates its own elogistics-c03-postgres-<random> container,
com.elogistic.c03.run label and captured container ID. PG18 data lives in tmpfs
/var/lib/postgresql; no named/anonymous data volume or developer DB mount.
Only that run's random bootstrap password file is mounted read-only.

Container port5432 is published to a random host port bound only to127.0.0.1;
neither native5432 nor developer5433 is targeted. Fresh elogistics_wire_<suffix>
database and owner NOLOGIN/migrator/runtime roles are generated in this container.
PUBLIC database/schema access revoked, platform prerequisite owned by owner.
Application runs as runtime, Flyway as migrator SET ROLE owner, unchanged V1/V2,
no Hibernate DDL/Flyway clean/repair. Five existing demo synthetic accounts are
seeded with newly generated passwords; no developer credential/database reused.

Before Java starts, inherited PG_/PG_TEST_/DEMO_/SPRING_ values are removed from
the child's environment; only owned settings are supplied. Backend binds another
random loopback port and runs the actual newly Maven-verified JAR with
profiles demo,postgres. Readiness requires live HTTP health plus five seeded
accounts and Flyway successful history1,2. Constant demo health is not relabeled
as general dependency readiness. JAR SHA256:
`d3b8ffca5fa16c35e394ca677e3aa96a0ab22a6f44ae801e448165710c56d7c9`.

Test SQL uses psql inside the captured, label/ID-verified disposable container,
never arbitrary host credentials. Fixture helper allows bounded SELECT/account
UPDATE statements, rejecting destructive/admin statements outside literals.
It is not a general SQL parser/security gateway. Operation='create' is correctly
treated as a literal. An executable control accepted that read and rejected four
unsafe statements without executing them. Live flag/role/merchant fixture updates
are restored in finally; all test data disappears with this owned tmpfs container.

## Actual wire validation and verification matrix

The generated SDK uses real global Fetch to the owned backend; there is no mocked
Fetch/server. One malformed-JSON fixture uses raw Fetch to send invalid syntax.
JSON bodies/tokens are validated in memory and are not saved or printed.
Validation uses the frozen OpenAPI with strict Ajv2020/formats, operation-specific
status/schema/error-code definitions and Location. Positive request body/path/key
schemas and actual serialized method/path/body are checked. Negative input is
explicitly classified; it is not passed as a supposedly schema-valid request.

Prefix /api/demo/v1. Numbers below are per final run; both runs match exactly.

| Actual operation | Success responses | Actual negative statuses/counts | Result |
|---|---|---|---|
| GET /health |200×1 |401×1 | PASS |
| POST /auth/login |200×4 |401×8,429×1 | PASS |
| GET /auth/me |200×4 |401×6 | PASS |
| GET /merchant/me |200×2 |401×2,403×3 | PASS |
| GET /shipments |200×4 |401×2,403×4 | PASS |
| POST /shipments |201×10 |400×13,401×2,403×2,409×2 | PASS |
| GET /shipments/{id} |200×2 |400×1,401×2,403×2,404×2 | PASS |
| POST /shipments/{id}/cancel |200×4 |400×1,401×2,403×2,404×1,409×2 | PASS |
| GET /admin/users |200×1 |401×2,403×3 | PASS |
| GET /admin/overview |200×1 |401×2,403×2 | PASS |
| GET /admin/shipments |200×1 |401×2,403×2 | PASS |

Twenty-seven scenarios include setup/SQL assertions and mixed positive/negative
HTTP groups;34/74 are response counts, not34/74 separate Node tests. The108 count
excludes harness startup readiness probes. All observed statuses/JSON bodies and
error codes conform to their specific operation; no generic default fallback is
used to excuse an unexpected status. Deliberate DB-outage500, arbitrary framework
errors and web nonJSON fallback are not forced/claimed as actual C-03 evidence;
the latter remains adapter scope. No promise of exhaustive HTTP input coverage.

The27 scenarios verify:

- Real PG18 cold V1/V2, fresh zero-effect DB and unprivileged runtime role.
- Existing health body, four actual logins/JWT issuer/subject/lifetime900,
  current user and nullable admin merchantId, merchant profiles and empty arrays.
- Real draft creation/Location/trusted actor/owner, detail/nullable event reason,
  owner list, sanitized admin users/summaries and actual overview counts.
- Same create key/body replay201/current resource; changed body409
  IDEMPOTENCY_CONFLICT; fresh key/duplicate reference409 REFERENCE_EXISTS.
- Cross-owner read/cancel404 identical to absent resource; no denied writes.
- Merchant B can independently use A's key/reference and only replay/list B's row.
- Missing credentials on all nine protected operations, explicit invalid bearer
  on all11 including public APIs, wrong roles on all eight role-restricted APIs.
- Expired/tampered/wrong-issuer tokens denied; signed role=ADMIN claim cannot
  elevate a merchant. DB enabled/role/scope changes are reloaded for existing JWT.
- Disabled/wrong login, actual sixth-failure429 limiter and safe flat errors.
- Ten invalid body cases (range/precision/blank/phone/missing/null/injected fields),
  missing/invalid key, bad UUID/malformed JSON/Unicode blank cancel reason; zero
  shipment/event/receipt changes after rejected writes.
- Padded draft values normalize on server and canonical body replays the same key.
- Cancel/trimming/replay/conflicts; fresh-key invalid transition; one appended
  cancel event, create replay still201 returning its now-CANCELLED representation.
- Two concurrent matching create requests and two concurrent matching cancel
  requests, with actual aggregate and per-resource event/receipt SQL counts.

OAS directly verifies shapes, required/nullability, types/formats, statuses,
headers and auth metadata. Authorization/merchant scope, live account trust,
hash normalization, no extra writes and concurrency/transaction effects require
the additional HTTP/SQL assertions above; a JSON schema cannot prove those alone.

Canonical request caveat is preserved: DraftInput trims before Bean Validation;
the padded-valid fixture validates its explicitly canonical value while sending
the raw body unchanged to the backend and checking trimmed output/replay.
CancelInput validates raw reason before service trim. This matches the already
documented source semantics; no new client normalization/validator or silent
schema loosening. Java UTF-16/UTF-8/Jackson coercion limitations in the contract
notes remain explicit, not a claim the OAS models every raw framework input.

## Commands, repeatability, cleanup and findings

Backend build from repository root used Python subprocess with output captured
to ignored .demo/d1-07-c03/backend-build-private.log:

```text
app/backend/mvnw.cmd -o -B -ntp -Dmaven.repo.local=<repo>/.demo/maven-repository -Ddebug=false -Dtrace=false verify
python scripts/ci/verify-reports.py --source app/backend/target/surefire-reports --output <ignored reports> --mode h2 --maven-exit 0
```

PASS:48 discovered,40 executed/pass including23 architecture tests,8 named
PostgreSQL-only skips, zero failures/errors; the existing report guard PASS.
No new Surefire test, assertion, report allowlist or architecture rule changed.
This is not a new48/48 backend PostgreSQL-regression claim. C-03's real PG/HTTP
suite is the separate27-test evidence above.

From packages/api-client:

| Actual command | Result |
|---|---|
| `npm.cmd run verify:c02` before checkpoint and after C-03 | PASS: C-0141 + SDK44 tests/no skips; strict lint/inventory/generation/typecheck/build |
| `python scripts/wire-harness.py` / `npm.cmd run test:wire` | Final two runs PASS27/27,0 skips;108 HTTP responses each |
| `npm.cmd audit --json` refreshed | FAIL:4 high,0 critical; graph unchanged, no fix/override |
| `npm.cmd audit --omit=dev --json` refreshed | PASS:0 findings |
| Git diff/check, bounded changed-file/secret/artifact review | PASS; C-03 files uncommitted, protected paths unchanged |

Two final runs of the same code/fixtures:

| Run suffix | Owned database | PG/backend host ports | Cases / HTTP | Cleanup/privacy |
|---|---|---|---|---|
|627e0104cbc2 |elogistics_wire_627e0104cbc2 |58671 /58684 |27 PASS;34+74 | PASS |
|1544d7bd1d60 |elogistics_wire_1544d7bd1d60 |56456 /56457 |27 PASS;34+74 | PASS |

Both safe-wire-results.json files have SHA256
`076527a15993c1396b1510819d2a4af48ce4c124819495adcc6f63e597be7b7a`:
same allowlisted case/status/operation/schema outcomes, no token/body/secret data.
These are local observations, not a performance benchmark or cross-machine claim.

Every attempt finally terminates/reaps only its launched backend process, checks
container ID/name/label/tmpfs ownership, removes only that captured disposable ID
without -v/prune/down, and compares all pre-existing container IDs/volume names
before/after. All four cleanup flags PASS for all six attempts. No developer DB,
existing named volume/container or emulator was changed. Reports/context/logs
remain ignored; only sanitized evidence is copied into documentation. Password/
signing-secret/JWT-pattern scans of backend logs PASS for all six attempts.

Earlier attempts are retained, not overwritten or reported green:

1. 019c202be23d: FAIL before HTTP tests. Redocly bundle wrote its output but its
   Node24 Windows process exited with libuv UV_HANDLE_CLOSING assertion; exit code
   was honored. Root cause not proven. Not a contract drift; no C-01/pin/assertion
   change. It did not recur in the final two runs. Child stdin/timeouts are bounded;
   this does not prove an upstream bug is fixed. Monitor if it recurs.
2. 031df527cadc: initial26 scenarios PASS, before final scope/extra SQL assertions.
3. f0886c0994c6:25/26 PASS; a new per-create receipt query was incorrectly denied
   by the harness SQL guard matching the literal 'create'. HTTP conformance still
   had zero mismatches, but this run FAIL remained recorded. Fixed literal handling;
   four unsafe controls still rejected, no SQL assertion was removed.
4. 6c5ef2eb4b7f:26/26 PASS with per-resource SQL assertions.
5–6. Final27-scenario runs above add independent merchant key scope and untrusted
   role-claim verification; both PASS.

No contract drift or backend business defect found in the approved fixtures.
No endpoint/DTO/security/idempotency/schema/migration change was made to pass tests.

## Remaining risk, traceability and C-04 entry

Four HIGH dev findings through generator/ref-parser/shared/js-yaml4.2.0 remain
OPEN, bounded loader mitigation intact; no remediation/acceptance is inferred.
No dependency/lock or approved pin changed. Runtime audit0 is not a whole-web/
mobile security audit; prior advisories there remain open. Transient Windows
process failure above remains a disclosed diagnostic, not an application failure.

TEST-001-CONTRACT-05/06/07 PASS actual HTTP+PostgreSQL. SC-01/04 wire/security/
replay/concurrency subsets and REQ-001-04/05, BR-01/02, NFR-S01/S02/C02 subsets
have concrete evidence. SC-AC-02 still requires real generated-client web adoption/
browser tests; SC-AC-04 UI/network fallback and SC-AC-05/06 consumer/final gates
remain C-04–06. C-01/C-02 DONE scopes retained; no full parent REQ/AC was marked
DONE or edited, and this manual disposable harness does not implement Testcontainers.

**Next C-04 prerequisites:** C-01/C-02/C-03 now pass locally; build the private SDK
before consumer install. Read existing web API/UI/unit/browser fixtures; replace
only handwritten types/operation calls with generated SDK and a minimal ApiError
adapter. Preserve memory-only auth, same-origin/proxy base, inputs/operation keys,
RBAC/owner and error/network/nonJSON behavior. Run package gates plus approved
web clean install/lint/typecheck/unit/browser/build gates. Mobile adoption remains
C-05; final combined backend/consumer regression and publication conditions C-06.
Do not start those tasks without their implementation instruction.

## C-03 file inventory

Fourteen uncommitted files (10 modified,4 new) after checkpoint b385244:

```text
contracts/openapi/README.md
docs/implementation/d1-07-c03-wire-evidence.md
docs/implementation/day-1-plan.md
docs/quality/traceability.md
docs/runbooks/local-development.md
packages/api-client/README.md
packages/api-client/package.json
packages/api-client/scripts/wire-harness.py
packages/api-client/tests/wire-lib.mjs
packages/api-client/tests/wire.test.mjs
specs/001-platform-foundation/slices/demo-contract-client/design.md
specs/001-platform-foundation/slices/demo-contract-client/spec.md
specs/001-platform-foundation/slices/demo-contract-client/tasks.md
specs/001-platform-foundation/tasks.md
```

No generated SDK/source contract, lockfile, backend/web/mobile source, migration,
workflow or architecture test file changed. New harness files are test-only and
not included in package runtime exports. Checkpoint43-file inventory is retained
in the C-02 evidence and commit; C-03 was not staged/committed/pushed.
