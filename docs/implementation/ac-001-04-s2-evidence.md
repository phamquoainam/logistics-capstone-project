# AC-001-04 S2-R1: nonblank validation and CI inventory

## Scope and acceptance boundary

Owner-approved S1 assessment is complete: 96 configuration rows, 49 fail-fast
assessment rows, and a bounded 429-source-file plus six-public-report audit.
Those are assessment/audit counts, not executed backend test counts.

S2 originally exposed two configuration defects. Independent focused review
resolved S2-R1-01 (token whitespace, HIGH original severity) and S2-R1-02
(demo seed whitespace, MEDIUM original severity) for the approved contract.
This checkpoint integrates their tests into the existing backend report policy.
AC-001-04 and AC-001-05 remain **NOT ACCEPTED**; Feature 001, D1-08 and Day 1
remain **PARTIAL**. Original REQ/AC are unchanged.

Baseline: `2c6a77e6541d7c97fd8c126cf7451ff537d53c77`, actual remote develop
verified before editing. Branch: `feature/ac-001-04-s2-fail-fast-evidence`.

## Minimal production fixes and retained RED/GREEN

- [TokenService](../../app/backend/src/main/java/com/elogistic/backend/identity/internal/application/TokenService.java):
  add `secret.isBlank()` to the existing UTF-8 byte-length guard. Preserve the
  minimum 32 bytes, raw padded keys, null rejection, HS256, claims and 900-second
  lifetime. No normalization or new entropy policy. Java blank semantics apply;
  this does not reject every visually whitespace-like Unicode character.
- [DemoAccountSeed](../../app/backend/src/main/java/com/elogistic/backend/identity/internal/infrastructure/DemoAccountSeed.java):
  add `password.isBlank()` to the existing 12..72 Java String length guard.
  Keep seed opt-in, existing-account bypass, raw passwords, BCrypt, identities,
  roles and transaction ordering unchanged. Validate before that account's
  encoding/insert. Fake earlier calls do not establish committed partial writes.

Private original RED reports are retained: three long-blank token cases and
five absent-account blank seed cases failed (Maven exit 1). The exact eight
assertions are unchanged and all pass after the two condition replacements
(exit 0). All original 98 cases then passed, followed by 11 additive controls
for intended causes, null behavior, padded JWT keys and seed side effects.
An initial added issuer test used a URL accessor for the existing string
issuer; only that new test accessor was corrected. Its failed log is retained.
Raw logs/XML, synthetic settings and diagnostic caches are private/ignored,
not documentation attachments or public artifacts.

## Explicit source-derived CI inventory

[Report policy](../../scripts/ci/verify-reports.py) enumerates four exact S2
classes and finite Surefire method/signature/invocation identities. Parameter
counts are fixed from reviewed MethodSource fixtures, not learned from reports.
No general display-name regex, arbitrary class allowance or count-only check.

| Suite | Required cases in H2 and PostgreSQL jobs |
| --- | ---: |
| TokenConfigurationEvidenceTests | 20 |
| DemoSeedConfigurationEvidenceTests | 38 |
| FoundationContextEvidenceTests | 17 |
| PostgresConfigurationEvidenceTests | 34 |
| Total S2 | 109 |

Maven's existing default discovery finds these `*Tests` classes in both database
jobs. Their MockEnvironment/context/fake-repository/property tests do not use
developer credentials, create datasources, invoke broker probes or connect to
external services. Architecture job explicitly selects only its existing two
classes. Task-C tags/profile and existing conditional skips are unchanged.
No workflow, Maven configuration, dependency, check name or ruleset change.

| Required check | Expected PASS | Expected SKIP | Discovered |
| --- | ---: | ---: | ---: |
| Architecture gate | 23 | 0 | 23 |
| Backend default / H2 | 157 | 17 | 174 |
| Backend PostgreSQL 18 | 182 | 5 | 187 |

H2 skips remain exactly eight PostgreSQL foundation, four PostgreSQL safety and
five real Redis/RabbitMQ tests. PostgreSQL skips remain the five real
Redis/RabbitMQ tests. No S2 skips are allowed in either job. Foundation-only
policy remains five required infrastructure tests and excludes S2.

The guard still rejects missing/extra/duplicate classes or methods, wrong
classname/job, skipped required tests, failure/error, inconsistent counts,
malformed XML and nonzero Maven exit. Only allowlisted metadata is published;
raw exception/output/properties and unknown dynamic identities stay excluded.
Existing credential and owned-resource cleanup guards remain enabled.

## Local publication verification

Execution date: 2026-10-19. Java 21 / Boot 4.1.1 / Maven Wrapper 3.10.0 unchanged.
Offline Maven cache; child environment excludes inherited developer connection
and tool-option variables. No local PostgreSQL/Docker integration execution.

| Command/scope | Actual result |
| --- | --- |
| `python -B -m unittest discover -s scripts/ci -p 'test_*.py' -v` | 37 PASS, no skips: 25 existing + 12 S2 policy controls |
| Maven `-Dtest=TokenConfigurationEvidenceTests,FoundationContextEvidenceTests,PostgresConfigurationEvidenceTests,DemoSeedConfigurationEvidenceTests test` | 109 PASS, no skips |
| Maven `-Dtest=ArchitectureTests,ArchitectureNegativeFixtureTests test` | 23 PASS, no skips |
| Maven `-Ddemo.test.database=h2 clean verify` | 157 PASS / 17 expected skips, exit 0 |
| Real guard over that clean H2 Surefire directory | PASS: exact 174 identities, 157 executed, 17 skips |

Maven runs use `-o -B -ntp -Dstyle.color=never` and an explicit existing local
repository cache. Synthetic fixture controls cover missing S2 suites/cases,
duplicate suites/invocations, unexpected signatures/indexes/payloads, wrong job
or classname, skips/failure/error, incomplete XML and privacy. Original
architecture/foundation inventories still pass; old database inventory missing
S2 now correctly fails. No production fix or assertion weakened for CI.

Hosted verification is a separate publication gate: require actual PR HEAD,
checkout/merge SHA, three job conclusions, exact sanitized artifact inventories
and hashes, privacy/cleanup checks and enforced develop ruleset. These local
results do not claim hosted PASS. PR/run evidence belongs to the publication
review; no automatic merge is authorized.

## Remaining independent gates and security findings

- Full-process exit, HTTP admission, remaining profile/binding/precedence paths
  and production PostgreSQL configuration/authentication matrix: UNVERIFIED.
  Property resolution and H2 are not substitutes for those runtime checks.
- Protected credential store Phase 1 remains integrated; real migration and
  developer PostgreSQL credential-mount cutover remain BLOCKED/unapproved.
- Legacy overbroad ACL/root-owner findings remain OPEN; no credential read,
  rotation, migration, live ACL/owner change or developer resource mutation.
- Historical raw compile-cache scanner FAIL and bounded npm/documentation-marker
  attribution remain preserved. A header marker alone does not establish a
  complete exposed key. Raw artifacts remain private; retention review
  2026-10-25 is an owner decision. Power-loss/full TOCTOU limits are unchanged.
- Bounded diff/publication scanning cannot establish absence of every unknown
  secret format, encoded material or private historical finding.

Trace: REQ-001-05 / AC-001-04 subset; TEST-001-CONFIG-01..04 and
TEST-001-CI-CONFIG-01. This evidence closes neither full security acceptance nor
independent developer onboarding.
