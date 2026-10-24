# T-002-06 Authentication HTTP and shared limiter evidence

Owner: OWNER_AUTH_HTTP_503_CONTRACT_CHANGE APPROVED and
OWNER_T_002_06_AUTH_HTTP_CONTINUATION CONDITIONAL_GO, 2026-10-24.
State: SOURCE_CI_VERIFIED for the four-operation auth scope; PR integration remains pending.

Baseline46920c040b9cd2e78f99efee7718015dff69dc89; PR27 merge;
develop push40105487979 attempt1 verified23 Architecture,454 H2/17 skips,
698 PostgreSQL/5 skips;125 controls/job, artifact digest/privacy/owned cleanup PASS.
Reuse clean HTTP worktree on feature/002-identity-authentication-http; prior branch
and ignored evidence retained. Six original Foundation fingerprints and four prior
Activation/Session/Current Session/Remaining Application worktrees remain protected.

Scope: exactly CSRF/Login/Refresh/Logout, V6 limiter, dedicated external HMAC,
secure cookies, authoritative Session/replay/rollback and strict report inventory.
[Accepted design](../architecture/decisions/ADR-0013-identity-browser-authentication-limiter.md).
Four operations add503/SERVICE_UNAVAILABLE only; no original REQ/AC change.

Verification and actual source SHA/run/artifact references will be appended after
execution. Auth servlet/filter-chain tests use synthetic TLS metadata; no production
TLS/key rollout, browser E2E, invitation delivery, Recovery or business readiness.
T-002-05/T-002-06 PARTIAL; Feature002/AC-002-01..05 NOT ACCEPTED;
GRANT_MUTATION_FUNCTIONALITY_LIMITED retained. Main traceability remains protected.

## Actual source CI acceptance — 2026-10-24

Verified source/actual checkout: **f96df93c49f98bd098d02889de8943e6816b3509**.
[Run40110885806](https://github.com/phamquoainam/logistics-capstone-project/actions/runs/40110885806), attempt1, workflow_dispatch on
feature/002-identity-authentication-http: all three required jobs SUCCESS.

| Mode | Discovered | PASS | FAIL / ERROR | Expected skips | Report controls |
| --- | ---: | ---: | --- | ---: | ---: |
| Architecture |23|23|0 /0|0|128|
| H2 |500|483|0 /0|17|128|
| PostgreSQL18 |763|758|0 /0|5|128|

Exact class/method/outcome inventory PASS:29 new authentication unit methods and
31 new real PostgreSQL/filter-chain cases, all194 previous Application methods
retained, combined225/225 PASS. Existing Migration/Domain/Realm/Grant/Provision/
Outbox/Activation/Session/current-state/administration regressions remain mandatory.
V6 cold-start/upgrade/validate/rerun and earlier checksums,18 exact owned tables,
runtime DML/no-DDL, rolling thresholds/reservations/concurrent multi-instance
enforcement, DB-clock boundaries, abandoned leases/retention, cross-family CSRF,
real Login/rotation/replay/concurrency/Logout, stale authVersion, rollback,
fail-closed503 and no polling/refresh idle extension passed within owned fixtures.

Architecture job now executes the accepted Identity contract checks:530 PASS,
45 negative document mutation controls,0 failed/skipped; strict Redocly2.64.0 lint
PASS. Node24.13.0 via official full-SHA setup-node pin; existing approved npm lock
and dependency pins unchanged. Three required check names and ruleset unchanged.

Local final Java21.0.10/Maven3.10.0 verify and strict collector:483 PASS/17 expected
skips; Architecture23;128 report-policy controls and15 static migration guards PASS.
Workflow YAML parsing and git diff --check PASS. Local servlet cookie checks assert
exact parsed names/values/flags/path/no-Domain/lifetime, independent of attribute
serialization order. Initial weak contains-based test edit was rejected by automatic
review and was NOT executed; the applied replacement adds exact semantic assertions.

Initial source3daaad5b31f48811379747e4aa111a94e9402047/run40110035914 had Architecture/
H2 PASS but PostgreSQL532 PASS/226 FAIL/5 skips: stale five-migration Application
fixture and ownership set missing the two V6 tables. One authorized corrective
commit updated only two test files: exact six-migration/eighteen-table expectations
and statement-stable synthetic lease timestamps. Production code/SQL unchanged by
the correction; no test removed, skipped or weakened. Run40110885806 is the current
successful evidence; historical failed results are not substituted for it.

All job logs checked in RAM for exact checkout,Java21/Maven Wrapper3.10.0 fresh
bootstrap,128 controls and temporary credential audit PASS. PostgreSQL generated
credential privacy audit, three-suite owned Testcontainers cleanup and disposable
CI PostgreSQL cleanup PASS. No local Docker/developer credentials/services used.
Retained artifacts contain only sanitized summary.json/tests.xml, not raw logs.

| Artifact | GitHub ID | Verified ZIP SHA-256 |
| --- | --- | --- |
|backend-architecture-1|12566107653|79c69b665b7be53b948428e8ba5b2a03bd22e563f39fcb1bdad28d9e1a2102c4|
|backend-h2-1|12566089418|b50878ef34814a03fe55fbc0649198ba0ffc1874acd0b6d523aabb4da34d711b|
|backend-postgres-1|12566702376|25de0732260ad910cbc227a93bc7d62f67ba01af04121b3bbd36543095c78098|

All metadata SHA-256, ZIP CRC, entry allowlist and exact reports PASS. Private
downloads/projections remain ignored under .demo/identity-authentication/.

## Scope and verification limits

Four browser auth operations are SOURCE_CI_VERIFIED, in addition to the previously
merged eight Bearer account/admin operations. The tested source above precedes this
documentation closure commit. A new PR integration run must test the documentation
HEAD/synthetic merge; it is not covered by the source run merely by inheritance.

No production rollout, real TLS/proxy/key deployment or browser E2E is claimed.
Invitation provision/resend/activation HTTP retains ADR0011 issuance/maintenance
and legacy-verifier compatibility gates; Recovery needs purpose-separated storage/
crypto. No T-002-07 UI, T-002-08 sender or logistics implementation. Merchant003
remains DESIGN_PROPOSED; T-003-01 must review dataset/provider/coverage decisions
and its production authorization before implementation. Business Owner review is
mandatory; authentication scope verification is not approval of business modules.

T-002-05/T-002-06 remain PARTIAL/unchecked; Feature002 and AC-002-01..05 NOT ACCEPTED;
GRANT_MUTATION_FUNCTIONALITY_LIMITED unchanged. Original REQ/AC are unchanged.
