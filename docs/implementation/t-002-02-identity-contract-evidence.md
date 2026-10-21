# Feature 002 — reconstructed Production Identity contract evidence

> Current checkpoint2026-10-21: **T_002_02_DONE** under OWNER_T_002_02_FINAL_VERIFICATION_APPROVAL;512 fresh checks and one strict Linux Redocly2.64.0 attempt PASS. [Final review, exact outputs and T-002-03 handoff](#t-002-02-final-review-and-acceptance-2026-10-21). Recovery results below are historical. Runtime ACs remain unaccepted.

Checkpoint closed for this iteration: 2026-10-21 (Asia/Saigon).
Authoring/owner decision provenance: 2026-10-20.
Verdict: **FEATURE_002_BASELINE_REBUILT_REVIEW_PENDING**.
T-002-01 DONE for browser decision scope. T-002-02 CONTRACT_REVIEW_PENDING.
Production AC-002-01–05 NOT ACCEPTED; T-002-03 NOT STARTED.
Feature001 / D1-08 / Day1 remain PARTIAL.

## Authorization, loss and reconstruction
Owner task: “FEATURE-002 — RECOVER SDD BASELINE & REBUILD PRODUCTION IDENTITY CONTRACT”;
OWNER_FEATURE_002_BASELINE_RECOVERY_APPROVAL: GO. Work directly in .,
without another worktree or branch switch. Earlier portal worktree and its
uncommitted Identity artifacts were explicitly deleted without backup.
These are newly authored files; no historical byte identity, 187-test recovery,
old artifact hash or past Redocly success is claimed.

Initial branch feature/ac-001-04-s3a-runtime-fail-fast, HEAD
e8dd52c6baff25ad5c75b80a85e7c81a9f85587f, 11 pre-existing working paths
(4 tracked modifications +7 untracked), empty index. A read-only pre-edit
441-public-file hash inventory was captured in tool orchestration. This was not
a backup of private evidence or a committed snapshot.
No recovery write targeted any of those11 paths. Final current fingerprints are
recorded below; the four owner-supplied checkpoints are explicitly rechecked.
The full pre-edit in-memory map did not survive the continuation boundary:
an automated end-to-end comparison of all11 against that map cannot be asserted.
No attempt was made to read private logs to reconstruct it.

## Authority read
Root AGENTS; docs/README-SDD.md, prd.md, principles.md, codex/definition-of-ready.md,
implementation/dependency-order.md, specs/INDEX.md; Feature002 spec/design/tasks;
Demo001 spec/design/tasks; decision register; security, module catalog,
data ownership, transactions/events and API conventions; ADR0001/0002/0003/0004/0008;
quality/test-strategy.md and relevant FR-A01/FR-A02/FR-AD01, NFR-S01/S02/S04;
OpenAPI README, existing demo tooling/config and demo-local runbook.
Existing LoginService/AuthController/AccountRepository were inspected read-only:
demo username matching is exact, and demo passwords/production trust are not
silently migrated. No DOCX source was present in the selected public source
inventory; original report was not directly re-verified.

## T-002-01 decision recovery
[ADR-0009](../architecture/decisions/ADR-0009-production-identity-lifecycle.md)
and [decision register](../decisions/open-questions.md) freshly record:
DEC-03 JWT_ACCESS_15M_WITH_ROTATING_REFRESH_AND_BACKEND_REVOCATION;
DEC-04 ADMIN_CONTROLLED_PROVISIONING_WITH_VERIFIED_EMAIL_INVITATION_AND_RECOVERY.
The owner's reaffirmed policy closes session architecture, lifetimes,
provisioning/recovery and browser CSRF design choices for this bounded slice.
This meets T-002-01's decision-review DoD; it does not imply an RBAC runtime smoke.
Global DEC-25 and unrelated accepted ADRs remain unchanged.

## New contract
[OpenAPI3.1](../../contracts/openapi/identity-access.yaml),
[semantics/error inventory](../../contracts/identity-access/README.md),
[fixtures](../../contracts/identity-access/fixtures.json),
[validator](../../contracts/identity-access/validate.mjs),
[strict lint configuration](../../contracts/identity-access/redocly.yaml).

17 operations are justified individually by session, CSRF, provisioning,
invitations/recovery, target lifecycle and catalogs; the old count was not a
required target.86 schemas comprise22 reusable wire schemas and64
operation/status-bound error schemas. This is not86 business endpoints.
The OpenAPI file uses JSON syntax, a YAML1.2 subset; no parser install required.
All examples are SYNTHETIC_CONTRACT_ONLY.21 behavioral declarations are explicitly
DECLARATIVE_NOT_RUNTIME_EXECUTED. No proposed event wire schema is added without
a subscriber/use case. No generated SDK or runtime endpoint is created.

## O1–O7 and R1 disposition
| Finding | Reconstructed decision/control | Current disposition |
|---|---|---|
| R1-01 / O5 | Whole-target read/lifecycle, union current+requested grants; resource-owner public authority, default deny; transaction-wide session revocation/audit | SPECIFIED; structured policy deletion/any-match mutation rejected; real authorization/concurrency UNVERIFIED |
| R1-02 / O7 | If-Match400/428/412 versus business/idempotency409; current authority and receipt replay precedence | SPECIFIED; exact headers/status/error bindings and weakening mutants covered |
| R1-03 / O1,O7 | Identity-only objects/flat errors, safe validation fields, distinct operation-specific error codes, no Demo/global envelope change | SPECIFIED; operation error examples and wrong-code negatives validated |
| R1-04 | Source-independent fixed method/path/opId/security/header/request/response/error/claim/policy/DTO expectations; mutations assert rejection category, positive control accepts nonpolicy text change | Implemented;36 mutation controls and1 nonpolicy positive control PASS |
| R1-05 | Every operation has positioned success/header fixture; request negatives where applicable, secret-shaped response negatives, every modeled error code, JWT required claims and password byte/codepoint boundaries | Contract checks PASS;21 future effect scenarios remain declarations, not runtime tests |
| R1-06 / O2,O3,O4,O6 | Server-bound CSRF AND cookies/header/Origin, rotation/rebinding; strict refresh/no polling idle extension; new15-codepoint/72-byte policy with legacy login; no self-edit/disable, last-admin and safe reactivation | SPECIFIED; structured-policy mutations PASS; HTTP/DB/cryptographic behavior UNVERIFIED |
| R1-07 | Pinned Redocly2.64.0 recommended-strict; existing info-license exception only | UNVERIFIED: no available Linux Node in bounded PATH preflight; no actual lint invocation |

Cookie/DTO/username/receipt-storage choices remain newly reconstructed candidate
details for focused review. The username proposal is exact sequence/no
trim/casefold/Unicode normalization, nonblank1..80 code points, consistently
validated at provisioning/login/recovery; existing Demo identities are untouched.
JSON Schema cannot enforce UTF-8 bytes. The test-only byte-policy predicate
does not certify the eventual Java BCrypt preflight.

## Actual fresh verification and failures
All counts below refer to this rebuilt source, not deleted artifacts.
No test selection/assertion suppression or dependency installation was used.

~~~powershell
$env:NODE_DISABLE_COMPILE_CACHE='1'
Remove-Item Env:NODE_OPTIONS -ErrorAction SilentlyContinue
node --version
node contracts/identity-access/validate.mjs
~~~

Node v24.13.0. The validator explicitly verifies existing Ajv8.20.0 and
ajv-formats3.0.1. It reads local JSON and Ajv code; no subprocess, network,
file publication, credentials or service access.

| Execution | Actual outcome |
|---|---|
| Initial rebuilt validator | exit1,460 PASS/1 FAIL/0 skip: self-disable declaration failed the fixture completeness length assertion; not a production failure |
| Fixture clarification + required-header/JWT guard revision | exit0,463 PASS/0 FAIL/0 skip;36 document mutation controls |
| Final source revision with missing-header negatives, JWT claim negatives and operation lifecycle fixture shapes | **exit0,500 PASS/0 FAIL/0 skip**;36 mutation controls;1 nonpolicy positive control;17 operations,86 schema compilers,21 declaration checks included |
| OpenAPI document JSON syntax/local refs, operation/policy/DTO bindings, component schema compilation and fixtures | PASS within custom-validator coverage above |
| Complete standards-based strict OpenAPI lint | UNVERIFIED; custom checks do not substitute for Redocly |
| Java/Maven/H2/PostgreSQL, browser auth runtime, email, Docker, native security experiments | NOT RUN, per exclusions |

The intermediate lifecycle fixture revision corrected provisioning to pending,
disable to disabled and mutation version/ETag to v2; fixtures are descriptions,
not database captures. Initial source-writing/patch command errors occurred
during authoring and were corrected; they are not test PASS evidence.

### Bounded Linux readiness, no Redocly attempt consumed
1. wsl.exe --list --verbose first returned E_ACCESSDENIED in the sandbox.
   Read-only metadata enumeration with authorized tool escalation exited0:
   Ubuntu WSL2 existed (stopped); docker-desktop was listed only, never invoked.
2. Existing Ubuntu command:
~~~text
wsl.exe -d Ubuntu --exec /usr/bin/env -i PATH=/usr/local/bin:/usr/bin:/bin /usr/bin/timeout 15s /bin/sh -c 'command -v node; command -v timeout; if command -v node >/dev/null 2>&1; then node --version; node -p process.platform; fi'
~~~
Actual stdout: /usr/bin/timeout. stderr empty, command exit0.
No Node was found in this bounded PATH. This does not prove Node is absent
everywhere on Linux; no personal-directory search, download/install or PATH
modification was attempted. No Linux Node/toolchain identity could be verified,
so the mandatory lint precondition failed and **Redocly was not launched**.
The locally installed Windows package metadata reports2.64.0 only.
Historical Windows zero-findings/native-assertion exit1 remains historical FAIL;
no Windows retry or new pass is claimed.

## File scope and traceability
Created:
- contracts/openapi/identity-access.yaml
- contracts/identity-access/README.md
- contracts/identity-access/fixtures.json
- contracts/identity-access/validate.mjs
- contracts/identity-access/redocly.yaml
- docs/architecture/decisions/ADR-0009-production-identity-lifecycle.md
- docs/implementation/t-002-02-identity-contract-evidence.md
- docs/quality/feature-002-recovery-traceability.md

Modified only:
- docs/decisions/open-questions.md: DEC-03/04 rows.
- specs/002-identity-access/design.md: appended recovery supplement.
- specs/002-identity-access/tasks.md: T-002-01 checkbox + appended bounded status.

[Supplemental REQ trace](../quality/feature-002-recovery-traceability.md) is
authorized instead of the protected main traceability file. Feature-level DoD's
main traceability integration remains a separate approved checkpoint; this task
does not edit it or mark the full feature accepted.

## Remaining gates and canonical handoff
- T-002-02 remains CONTRACT_REVIEW_PENDING: strict Redocly and focused review of
  reconstructed exact routes/DTOs/username/error/CSRF and replay details incomplete.
- Production mail provider, mobile transport, bootstrap deployment, signing/secret
  operations and retention/throttle configs remain scoped implementation decisions,
  not permissions to silently pick providers or defaults.
- No runtime AC002 acceptance, no Foundation001 completion, no historical finding erased.
- Next canonical task after contract acceptance: T-002-03 Migration. DoR: accepted
  contract, reviewed Identity data model/constraints/role ownership and safe isolated
  PostgreSQL verification plan. Scope is a NEW Flyway migration for accounts,
  credentials/sessions/families, grants/scopes, one-use proofs, receipts/audit and
  delivery intents only as justified; V1/V2 unchanged. Verify cold-start/upgrade,
  uniqueness/FKs/permissions, audit/receipt atomicity, session revocation and
  no break of Demo-001. No migration or execution is authorized by this report.

## Protected current fingerprints
The following final manifest is recorded without reading private credentials.
Only the four hashes supplied in the approval are asserted as an automated
pre-existing checkpoint match; no full11 before/after claim is invented across
the continuation-memory loss.

| Path | SHA-256 |
|---|---|
| infra/verification/maven_h2.py | 42198a9be515196b3e3acce57967335ccb8414a98859b633c744345cb0e349eb |
| infra/verification/test_maven_h2.py | 85a5bc2c0189f9b7b11a1fed98b80bd11561ef5f34f9149748a7005766c03dd3 |
| docs/implementation/ac-001-04-s3a-r1-evidence.md | 668733d05374d4216eb5cbce16b6f16d11aaa1069a1c7a7a8f89863daa410658 |
| docs/quality/traceability.md | 0e992a189ff22e0876d3f7616890b15cf23188abf0e6723022696e6b3bc89eb9 |
| app/backend/src/main/java/com/elogistic/backend/identity/internal/infrastructure/DemoAccountSeed.java | 38f698f420521fbc6c8af562039cd09de8d0f9f8400fff56842fb82fa2cd2ba5 |
| app/backend/src/main/java/com/elogistic/backend/identity/internal/infrastructure/DemoSeedPreflight.java | d5a7fe07321fd68502e6942761bcb9be82f13416aa86763b10d3c3f1e4d4198e |
| app/backend/src/test/java/com/elogistic/backend/identity/internal/infrastructure/DemoSeedPreflightTests.java | 276bd82d81bf2fc75be2072456ef41e95fbd20df565fb38ac5b82f1a0c53f995 |
| infra/verification/fail_fast.py | ec349558c7aa197663bc5a6ea88e139069a5d4359f164faa2d065ba24c095524 |
| infra/verification/test_fail_fast.py | c80d775d8f72351744e30cf49f0ce7660bf875f5fe7c71eee278b1965b80d0dc |
| scripts/ci/verify-reports.py | e47150d1949681998de8dc77d93269369d9fbe94bee78b42a96675da58b0974d |
| scripts/ci/test_verify_reports.py | 2ce80192430e055be6e6e5d8c75dc4602f7cce6555a709a2643d5b86075a1ad6 |

No branch switch, stage, reset, clean, stash, rebase, commit, push, PR or merge.
No worktree recreation. Main Foundation traceability, sources/tests and private
evidence were not targeted by a recovery write. Final documentation/scope checks
are appended after execution.

## Final publication-scope checks ? 2026-10-21

- Selected-file UTF-8/NUL/replacement-character, relative links, fixed-pattern disclosure, decision/task status and current protected-manifest checks:100 PASS/0 FAIL;42 relative links,11 recovery files. This bounded scanner is not a full-history/private-credential audit.
- Initial git diff --check exit2 identified two trailing Markdown spaces on the newly ticked T-002-01 line. Removed only those spaces; final recheck exit0. Existing Foundation line-ending notices are not source edits.
- Actual final inventory:22 working files (7 tracked modifications/15 untracked), comprising original11 Foundation paths plus11 recovery files; empty index; branch/HEAD unchanged. git worktree list shows only .. No runtime/source/dependency/CI modifications introduced by recovery.

Current contract-source fingerprints (no circular hash of this evidence file):

| Path | Contract SHA-256 |
|---|---|
| contracts/openapi/identity-access.yaml | e293d134d5965cb2a94d35a89a6378ab7afbd3ba7f15b8af6f690b90e8cb2df1 |
| contracts/identity-access/README.md | 7f3f53356e6f4ea2465f8dba7683147efbc675d3e21d639120b40b4879e06da8 |
| contracts/identity-access/fixtures.json | b026aff969c54fac54a014cb7c52d3f0b48699f05d2e73fd39ef047114f45fb8 |
| contracts/identity-access/redocly.yaml | 7d396584d897068fc713b6dfe973a20f965fda649f8e23f2947731717b155226 |
| contracts/identity-access/validate.mjs | ae328b5b39549f1108cfe5d149b3b111b8f0b22defa7a80f1f702a306a662499 |

## T-002-02-FINAL review and acceptance 2026-10-21

**Current verdict: T_002_02_DONE** for the technical contract task under
OWNER_T_002_02_FINAL_VERIFICATION_APPROVAL: GO. This supersedes recovery
REVIEW_PENDING above for this exact source revision; historical failures and
prior results remain intact. No full Feature002/runtime acceptance is granted.

### Fresh baseline and review authority
Same branch/HEAD,22 pre-existing working files and empty index. Current task
captured449 nonignored public-file hashes and persisted the11 protected hashes
in a task-owned temporary preflight manifest before source correction. Four
owner-established H2-12B hashes match. The prior recovery's missing-memory
comparison limitation is not retroactively removed; this task's new preflight
supports a fresh before/after comparison.

Canonical T-002-02 DoD requires schema validation, request/response/error
examples and defined contract error cases. The owner explicitly delegates
focused final contract/security review and acceptance here. No separate
nondelegable human sign-off is specified for this task; future publication,
migration/model approval and full feature CI/runtime acceptance remain separate.
User authorization permits the supplemental trace without changing protected
main traceability. ADR0009 and decision register are read-only in this task.

### Actual findings and minimal corrections
| ID | Exact location | Defect / risk | Correction and fresh controls |
|---|---|---|---|
| FINAL-01 | OpenAPI /paths/~1auth~1logout/post/security and parameters; README Session/CSRF | Mandatory refresh cookie contradicted204 repeat behavior after the server cleared that cookie | Require CSRF state/header/exact Origin always; refresh cookie optional with family binding when supplied; valid preauth binding supports repeat204.43-mutation suite includes refresh-required/optional-param deletion/CSRF-disabled mutants;4 logout case declarations are checked only as contract expectations |
| FINAL-02 | OpenAPI /paths/~1auth~1recovery/post/parameters and /x-identity-policy/recovery; README Credentials | Unauthenticated actor/subject receipt scope and delivery authority were underspecified; account-existence-dependent receipts or request-chosen delivery could violate non-enumeration/recovery policy | Preauth-binding/operation/key scope, same receipt behavior for eligible/ineligible inputs, consistent username/email fingerprint; delivery only to stored verified email matching submitted matcher. Mutants reject account-derived scope, arbitrary email, existing-only receipt and plaintext receipt storage |
| DOC-01 | Previous evidence final-publication heading | Nonsemantic question-mark substitution in the old heading was observed | Preserved as historical text; current explicit final checkpoint is authoritative. No acceptance inference from encoding or formatting |

No route/method/opId, reusable DTO shape, accepted O1–O7, password limits,
role/resource boundary, JWT claims, If-Match mapping, refresh replay or idle
policy was weakened.17 operations and86 schema inventory remain unchanged.
All operations were reviewed against independent inventory/DTO/security/header/
status/error expectations; whole-target authority, new-password codepoints/UTF8,
proof lifetime, self-management, last-admin and production/demo separation are
represented by explicit contract rules. No backend implementation was reviewed
as runtime evidence.

21 original behavioral expectations are future runtime requirements, classified
in the supplemental trace.4 additional logout case vectors are explicitly
DECLARATIVE_NOT_RUNTIME_EXECUTED. The test-only expected-status check is not an
HTTP authentication simulator or proof of session/CSRF enforcement.

### Fresh contract verification
Command from repository root, with invocation-local compile cache disabled and
NODE_OPTIONS removed:
~~~powershell
node contracts/identity-access/validate.mjs
~~~
Actual exit0: **512 PASS /0 FAIL /0 skips**, including43 document-mutation
controls and1 nonpolicy positive control. Expected rejected categories are
asserted, not merely arbitrary thrown exceptions. The original500 result is
historical, not reused. No project dependencies were installed/upgraded.

### One bounded Linux toolchain setup
Known /usr/bin,/usr/local/bin,/opt Node locations, the exact pinned nvm location,
and two exact /tmp cache paths had no usable Node24.13.0. Ubuntu is x86_64 WSL2.
One portable setup used official TLS source:
https://nodejs.org/dist/v24.13.0/node-v24.13.0-linux-x64.tar.xz
and official SHASUMS256.txt, with TLS verification and redirects restricted to
nodejs.org; no proxy credentials, npm, apt/snap/sudo or global install.
Archive bytes30768936; published and actual SHA-256:
e798599612f4bb71333a3397ab0d095fd62214e115aea45aa858a145fc72d67e.
All6046 archive paths/link targets were checked for containment before extraction.

The bounded full-archive tar wrapper returned exit1 without output and was not
retried. The required bin/node payload was complete: SHA-256
53fb205ae78805130177e24bcb459a69a1518c8d98f8965f31d85aae7ea840fc
matched the payload inside the checksum-verified official archive exactly.
The runtime probe exited0 with version v24.13.0/platform linux/arch x64.
No partially extracted npm/header asset was used; only that verified standalone
Node executable ran. No global PATH/shell startup, ACL, Windows environment,
developer service or project dependency mutation.

Task-owned directory: [REDACTED_PRIVATE_SESSION_PATH]
Owner nonce: 88b28c4a0d5947c2b756fd13fa9fc89e. It held public tooling, preflight hashes and contract
outputs only, not credentials. Cleanup status is recorded after ownership checks.

### Strict Redocly: exactly one actual invocation
Installed package metadata pin2.64.0 verified. recommended-strict and the original
info-license publishing-metadata exception were unchanged; config SHA-256
7d396584d897068fc713b6dfe973a20f965fda649f8e23f2947731717b155226.

CWD was the new owned directory, not repository root: source inspection showed
CLI automatically reads CWD/.env. Empty owned HOME/XDG_CONFIG_HOME prevented
developer auth/config discovery. CI=1, REDOCLY_SUPPRESS_UPDATE_NOTICE=true,
REDOCLY_TELEMETRY=off, REDOCLY_SDK_TELEMETRY=off, DO_NOT_TRACK=1 and
NODE_DISABLE_COMPILE_CACHE=1 were child-process-only. The inspected pinned code
gates npm update requests and telemetry with those settings. No project .env,
credential store or developer service was read. Only local refs/config presets;
no other network dependency was requested.

Exact command (all Linux paths absolute):
~~~text
wsl.exe -d Ubuntu --cd [REDACTED_PRIVATE_LOCAL_PATH] --exec /usr/bin/env -i PATH=/usr/bin:/bin HOME=[REDACTED_PRIVATE_LOCAL_PATH]/home XDG_CONFIG_HOME=[REDACTED_PRIVATE_LOCAL_PATH]/home CI=1 REDOCLY_TELEMETRY=off REDOCLY_SDK_TELEMETRY=off REDOCLY_SUPPRESS_UPDATE_NOTICE=true DO_NOT_TRACK=1 NODE_DISABLE_COMPILE_CACHE=1 NO_COLOR=1 /usr/bin/timeout --signal=TERM --kill-after=5s 60s [REDACTED_PRIVATE_LOCAL_PATH]/node-v24.13.0-linux-x64/bin/node packages/api-client/node_modules/@redocly/cli/bin/cli.js lint contracts/openapi/identity-access.yaml --config contracts/identity-access/redocly.yaml --lint-config error --format json
~~~
Only one exclusive lint-attempt marker existed. Timeout60s plus5s kill grace,
outer capture80s; actual3.943s, **CLI exit0**.
Validated contract SHA-256:
acf92380c325698a1370a2ade6accff82bb338c4524f6328a9613d3e4b7b6128.
Contract was unchanged during and after lint.

Exact stdout,115 bytes:
~~~json
{
  "totals": {
    "errors": 0,
    "warnings": 0,
    "ignored": 0
  },
  "version": "2.64.0",
  "problems": []
}
~~~
Exact stderr,209 bytes (informational progress, no warning):
~~~text
validating ../../contracts/openapi/identity-access.yaml using lint rules for api 'identity@v1'...
../../contracts/openapi/identity-access.yaml: validated in 153ms

Woohoo! Your API description is valid. 🎉
~~~
stdout SHA-256 acf1f3d59378bee1a60e56a3fd39ee3536ef7c699a81df502cfdc772dc792b73;
stderr SHA-2562537b6ae597bec036fedeb13bc3cb46d5800c5f217a63576d2314c5a29da4cba.
The Windows Python capture wrapper exited1 while rendering stderr's celebration
character through cp1258 AFTER writing stdout/stderr/result. UTF8-safe readback
of the already saved files exited0 and confirmed actual CLI0 and zero totals.
**No second Redocly invocation occurred.** Historical Windows native assertion
exit1 remains historical FAIL; it is not relabeled or used as Linux evidence.

### T-002-02 acceptance and unresolved feature gates
| Gate | Current status |
|---|---|
| Focused full contract/security/SDD review | PASS after FINAL-01/02 correction |
| Schema/request/response/header/error/local-ref/inventory and negative controls | PASS512, exit0 |
| Strict pinned Redocly on exact current contract | PASS one attempt, CLI0;0 errors/0 warnings/0 ignored |
| Task traceability/evidence | Approved supplemental record; main traceability unchanged |
| Contract technical DoD | DONE, no remaining blocking contract ambiguity identified |
| JWT cryptography/CSRF HTTP, grants/revocation/proof/concurrency/DB/email runtime | UNVERIFIED / NOT RUN |
| Full Feature002 / AC-002-01..05 / feature-level CI/main trace integration | NOT ACCEPTED / still open |

The approved task-level supplement is not a waiver of later feature-level main
traceability/CI requirements. No Java/Maven/H2, Docker/PostgreSQL/Redis/Rabbit,
native Windows security or real email execution occurred.

### T-002-03 migration handoff — no SQL created
Canonical task: identify Identity data model and Flyway migration for accounts,
credentials/session, roles/permissions/scopes and audit, public IDs for external
references; migrate empty DB and upgrade with ownership/index/constraint evidence.

**DoR:** accepted exact contract above; explicit subsequent owner authorization
for migration/model files and isolated execution; review concrete logical-to-
physical names, key/retention/config choices and resource-owner authority
integration before adding DDL. Current approval grants none of that execution.
Existing Foundation process observer H2-12B remains PATCH_READY_FOR_REVIEW;
historical Direct H2 SECURITY_BLOCKED and Wrapper/S3-B/S4 limitations are unchanged.
A separate contained runtime plan (existing isolated PostgreSQL/Testcontainers
harness or approved clean runner) must be agreed before Java/Maven/DB verification;
never developer credentials/volumes, no implicit observer bypass.

Logical model needs: distinct production user_account identity/activation
eligibility/version; BCrypt credential; backend session/authVersion and revoked
family/rotating-token hashes; single-use invitation/recovery hashes and deadlines;
roles/grants/resource public IDs; canonical request-bound receipts; append-only
audit; durable sandbox delivery intent only when justified. No plaintext
password/refresh/proof in persistence receipts/logs. Existing demo_account and
Demo-001 trust/data remain separate; no automatic import, username rewrite or
credential transfer. Delivery occurs outside DB transaction.

**Conventions / proposed files:** one global stream currentlyV1/V2 under
app/backend/src/main/resources/db/migration; proposed next file
V3__production_identity_foundation.sql only after version/model review.
History platform.flyway_schema_history, NOLOGIN owner, migrator SET ROLE owner,
runtime DML-only, clean disabled; no edit of V1/V2, schema auto-update, repair,
baseline, cross-module DML or developer database. Proposed isolated migration
tests under app/backend/src/test/java/com/elogistic/backend/identity/ and existing
testsupport only as needed; design/tasks/evidence/supplement updated in that
later approved task. No speculative cross-domain tables/FKs or framework upgrades.

**Positive/negative verification:** cold empty databaseV1–V3; isolated upgrade
fromV2 with existing Demo rows preserved; validate/rerun0 new migrations;
independent upgrade data preservation; exact case-sensitive username uniqueness,
required/check/FK/index invariants, correct proof/session/receipt hash uniqueness;
runtime DDL/history denial, owner/migrator separation, no new writes to other
domain tables. Isolated PostgreSQL18 approved image/random ports/synthetic roles,
fresh owned lifecycle, checked cleanup/report privacy. No H2 substitution.
Last-admin and grants/session/audit transaction semantics must be supported by
reviewed schema/locking design; their application/concurrency enforcement belongs
to later T-002-04..09, not falsely proved by table existence.

**DoD:** migration applies from empty and upgrades without losing existing data,
index/constraint/ownership tests and relevant regression PASS on real isolated
PostgreSQL; immutable applied scripts, exact reports and SDD trace. If execution
authorization/environment gates are missing, implementation/verification stays
partial; no fake runtime result. T-002-03 remains NOT STARTED in this checkpoint.

### Exact changed source/doc fingerprints (excluding this self-referential evidence)
| Path | Final SHA-256 |
|---|---|
| contracts/openapi/identity-access.yaml | acf92380c325698a1370a2ade6accff82bb338c4524f6328a9613d3e4b7b6128 |
| contracts/identity-access/README.md | 813bcb5f3d1ef2d5f39f32d36c0d5ab358f8bf199e37fdf28c62d45ce3a036d3 |
| contracts/identity-access/fixtures.json | 74a8abfa8316d9c1a36169332bde827698e72d9575118bfe0bba1541eca18a34 |
| contracts/identity-access/validate.mjs | 82e387350a84de7b1851b218457ed087faa66baf34bd24fbd4d006e750cb6cad |
| specs/002-identity-access/design.md | f987b8323d2434fb689a00d55f7b77c42c7f563eae314c7ae4f5d964acf7172f |
| specs/002-identity-access/tasks.md | 527189e647abed2964155849846db4c7cb5774c5e38978e182f4baac72262b83 |
| docs/quality/feature-002-recovery-traceability.md | 195f03fbf913269be3596ef8b83757c7b7eced10d68e54133467f3860ac6819d |

Eight existing files were changed this task: the seven above plus this evidence. No new tracked/untracked source path, accepted ADR/register change, manifest/CI/SDK/frontend/Java/migration change. Final scope/privacy/link and all11 preflight hash comparison results follow after execution.

### Final integrity, documentation and cleanup results
Fresh post-correction checks:113 PASS/0 FAIL,34 relative links/anchors checked,
8 changed files,11 original Foundation hashes compared against this task's
persisted preflight and unchanged. Seven published contract/doc source hashes
also match. git diff --check exit0; existing LF/CRLF notices only.
The full public inventory stayed449 files; delta is exactly8 allowed files,
zero new paths. Accepted ADR0009, decision register, original REQ/AC, Demo001,
Java/SQL/SDK/dependency/CI and the protected main traceability are unchanged
relative to this task's preflight.

Owned cleanup: owner nonce and exact canonical .demo child path revalidated,
root not a reparse point, all root entries matched the owned allowlist and
nested link/junction targets stayed inside the owned directory. Actual
Remove-Item -LiteralPath on that single owned directory completed exit0 and
absence verified. Portable archive/binary/temporary HOME/preflight/output files
are removed; sanitized CLI contents and all source/report hashes are preserved
above. No foreign session, container, volume, credential or developer evidence
was targeted; no global cleanup or WSL/daemon shutdown.

Final branch feature/ac-001-04-s3a-runtime-fail-fast, HEAD
e8dd52c6baff25ad5c75b80a85e7c81a9f85587f, index empty and original22 working
paths preserved. No commit/push/PR/merge or worktree creation.
**T_002_02_DONE**; T-002-03 remains NOT STARTED until separately authorized.
