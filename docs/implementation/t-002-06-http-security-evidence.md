# T-002-06 bounded HTTP/security delivery evidence

Owner: OWNER_T_002_06_SECURITY_DESIGN: CONDITIONAL_GO, 2026-10-24.
State: SOURCE_CI_VERIFIED for the eight-operation boundary; actual PR integration CI remains UNVERIFIED.
T-002-06 PARTIAL; T-002-05 PARTIAL; Feature002/AC-002-01..05 NOT ACCEPTED;
GRANT_MUTATION_FUNCTIONALITY_LIMITED unchanged. No original REQ/AC/contract/migration change.

## Baseline and protected worktrees

PR26 merged at3da5fe63a7933710364f05c65d84d90fb28588e5 with parents899cf8 and1b0a4d8.
Actual develop push run40096507091 attempt1 PASS: Architecture23/0 skips, H2411/17,
PostgreSQL637/5;122 report controls/job, exact inventories/digests/privacy/owned cleanup PASS.
New worktree D:/TLCN/e-logistic-identity-http-security, branch feature/002-identity-http-security
starts at that exact baseline. Original six dirty Foundation fingerprints and the four preceding
Activation/Session/Current Session/Remaining Application worktrees are preserved.

## Canonical operation coverage and exclusions

| Accepted operation | T-002-06 status / dependency |
| --- | --- |
| getIdentityMe | Implemented Bearer/current DB admission; polling read-only |
| listIdentityAccounts/getIdentityAccount | Implemented whole-target authorization, pagination, masked404, ETag |
| listIdentityRoles/listIdentityScopes | Implemented bounded authoritative catalog |
| replaceIdentityGrants | Implemented accepted PUT/DTO/key/If-Match/error mapping; existing REALM-P3 guard |
| disableIdentityAccount/reactivateIdentityAccount | Implemented accepted POST/DTO/key/If-Match; existing Domain/transaction/receipt invariants |
| getIdentityCsrf/loginIdentity/refreshIdentity/logoutIdentity | NOT IMPLEMENTED: shared limiter/admission and browser-cookie lifecycle remain required |
| provisionIdentityAccount/resendIdentityInvitation | NOT IMPLEMENTED HTTP: shared limiter where specified; ADR0011 issuance/maintenance operational prerequisite; no sender installed |
| acceptIdentityInvitation | BLOCKED affected capability: v1 envelope approved; safe old bare-verifier resolution/transition is not established |
| requestIdentityRecovery/confirmIdentityRecovery | BLOCKED: purpose-specific protected storage/forward migration remains unapproved |

See [ADR-0012](../architecture/decisions/ADR-0012-production-identity-http-security.md) and
[runbook](../runbooks/production-identity-security.md) for approved RS256/key lifecycle, thresholds,
compatibility qualification and exact remaining model/deployment decisions. An existing Redis
probe ACL is not mutable limiter authority; no proposed table, V6, ACL change or live secret is used.

## Source and test scope

Explicit RS256 signer/verifier and external protected JWK configuration; per-request inward
admission; read-only persisted timestamp lookup; eight accepted HTTP operations/strict error
mapping; opt-in isolated composition. No Demo fallback, extra endpoints, random production keys,
client principal/activity flag, dependency update, SQL migration or deployment/CI workflow change.
The same owned PostgreSQL test suite/lifecycle is extended, not replaced or bypassed.

43 JUnit security/HTTP methods authored plus18 real PostgreSQL/filter-chain methods appended.
176 existing Grant/Application method identities/bodies remain intact. Exact new inventory and
3 new missing/renamed/skipped/failure/error/privacy controls added to strict policy; collectors,
skip semantics and three required job names unchanged. Expected complete source totals:
Architecture23/0; H2454/17; PostgreSQL698/5;125 report-policy controls/job.

Local focused43/43 security tests PASS with offline Java21.0.10/Maven3.10.0, allowlisted child
environment and synthetic user/global settings. Initial full verify detected an unmapped
composition root package; moved it under identity.internal without altering ArchUnit rules.
Current clean verify PASS:471 discovered,454 PASS,0 FAIL/ERROR,17 expected skips; strict H2
collector PASS.125/125 report-policy controls and15/15 migration static controls PASS. Initial
policy run found two stale job-total assertions; totals increased exactly to454/698 while
retaining every prior method and skip. Python unchanged-line EOL restored from HEAD with entire
AST equality to the tested scripts. No PostgreSQL PASS is inferred from compilation/H2. No local Docker/developer database/credentials are used.

## Verification limits and next acceptance

Servlet/MockMvc and actual Spring Security filter-chain fixtures use synthetic TLS metadata;
these are not real TLS/proxy/browser deployment tests. Public/private JWKs are generated ONLY
inside synthetic JUnit fixtures, never production startup and never public artifacts. Authoritative
PostgreSQL, revocation/Realm/ownership/permission, concurrency, replay and rollback require actual
current-source CI before this scoped delivery is accepted. Full task and original AC remain open.

Supplemental [traceability](../quality/feature-002-recovery-traceability.md) is used; the main
Foundation traceability file is protected. Later documentation HEAD must be distinguished from
any previously tested source SHA. No future PR merge is authorized in this delivery.

## Actual source CI acceptance ? 2026-10-24

Owner authorization remains bounded to independent T-002-06 scope, not full task/Feature acceptance.
Source and actual checkout SHA: **1962a839fce6e601d0f541e094a1dc9d2c663c05**.
[Run40103151184](https://github.com/phamquoainam/logistics-capstone-project/actions/runs/40103151184), attempt1, workflow_dispatch,
branch feature/002-identity-http-security, Backend verification. All three jobs SUCCESS.

| Mode | Discovered | PASS | FAIL / ERROR | Expected skips | Report controls |
| --- | ---: | ---: | --- | ---: | ---: |
| Architecture |23|23|0 /0|0|125|
| H2 |471|454|0 /0|17|125|
| PostgreSQL18 |703|698|0 /0|5|125|

Exact class/method/outcome inventory PASS, including43/43 new security methods in H2 AND
PostgreSQL and18/18 new PostgreSQL/Spring Security filter-chain methods. Previous176 PG method
bodies/identities retained, combined194/194 Grant/Application cases PASS. This proves current
DB revocation/version/account/Realm/owner/deadline checks, eight HTTP mappings, atomic business
mutations, concurrent same-key effect, rollback, meaningful activity and no replay/polling extension
within the synthetic owned fixture. Existing Migration/Domain/Provision/Activation/Session/Outbox
regressions remain present/passing; no Java/SQL fix or corrective CI run was required.

All three aggregate job logs show this exact source checkout; Java21/Maven Wrapper3.10.0 clean
bootstrap,125 policy controls/job and temporary credential audit PASS. PostgreSQL generated-secret
privacy audit, three-suite Testcontainers owned cleanup and disposable CI PG cleanup PASS. No
local Docker/DB/secret access or developer resource mutation. Raw remote logs processed in memory
only; retained downloads contain only sanitized summary.json/tests.xml.

| Sanitized artifact | GitHub artifact ID | Verified ZIP SHA-256 |
| --- | --- | --- |
|backend-architecture-1|12563826233|b8fde3b7f3ca0e8482c4ba1456c86d6c4f956f68ca713a70471a5c89d5fd2442|
|backend-h2-1|12564015598|0b90c88e2a9591db11c051fc36f882d02dfd03b8b2895792001849582f4d2587|
|backend-postgres-1|12564150488|9dfe0faaef20d8d640683915f75d51b80debe8f28af2340532336709ac5adf22|

ZIP metadata digest, CRC and exact entry allowlist PASS. Private local evidence lives only under
ignored .demo/identity-http/. No private key, bearer, cookie or raw diagnostic is a public artifact.

This documentation closure is LATER than the tested source commit. Its future HEAD must not be
attributed to the source run above; actual PR integration checks remain mandatory. Eight-operation
source is CI_VERIFIED; T-002-06 stays PARTIAL/unchecked for the explicitly excluded browser auth,
CSRF/cookie, shared limiter, protected issuance/compatibility and Recovery gates. No original AC
accepted; T-002-05/Feature002 and GRANT_MUTATION_FUNCTIONALITY_LIMITED remain unchanged.
