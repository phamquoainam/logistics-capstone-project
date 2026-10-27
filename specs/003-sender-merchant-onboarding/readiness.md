# Spec003 final DoR review packet

Checkpoint: **SPEC003_CONTROLLED_DECISIONS_AND_DOR_READY_FOR_OWNER_REVIEW**.

Business policy: OWNER_APPROVED_WITH_REFINEMENTS. Technical package: CANDIDATE_FOR_OWNER_REVIEW. Implementation: NOT_AUTHORIZED / NOT_STARTED. Spec003 remains **BLOCKED_FOR_IMPLEMENTATION**, not ACCEPTED. Policy approval, candidate review, implementation authorization and full-spec acceptance are separate facts.

## Definition of Ready assessment

| Repository DoR criterion | Documentation result | Remaining exact gate |
|---|---|---|
| Actors/scope/REQ/journeys/edge cases/Given–When–Then AC | Defined in spec,14-row coverage and synthetic scenarios | Review candidate fixtures; runtime tests intentionally not yet executed |
| Original FR/BR/UC and latest Owner direction | FR-A01/A02/M01/AD03 and BR02 applicability mapped; new onboarding policy explicitly Owner source, no fabricated report equivalence | No new general business-model approval requested |
| Owner/transaction/API/data/event/error/auth object | Exact candidate OpenAPI, resource policy, data-model, lock/recovery and dependency contracts | **Accept the bounded technical candidate package**, including new names and Identity delta, before code |
| Blocking decisions resolved or independently sequenced | Six policy directions recorded; dependent defaults/closure subset and deployment conditions explicit | Candidate contract acceptance + subsequent production authorization pending; first-use deployment settings must be approved before delivery/upload E2E |
| Executable ordered tasks/deliverables/verify gates | T003-01..12 and per-BC CONTRACT/DB/BE/UI/VERIFY mapped to exact operations/scenarios | Implementation tasks remain unchecked |
| Public dependencies/reviewed workflow TX | CAT-004/POL-005/closure typed consumer contracts; existing module direction and session-owned TX preserved | Owner technical review of this package; actual minimum owner implementations before dependent acceptance |
| Success/invalid/expired/duplicate/concurrent/failure fixtures | Schema samples plus named behavioral scenarios and mutation controls | Static validity is not execution of Java/PG/browser acceptance |

**DoR is ready for a concrete Owner review, not unconditionally READY_FOR_IMPLEMENTATION.** There is no missing architecture choice requiring a new broad cycle. The exact pending review is the candidate package in this PR: new customer transport/admission; independent customer-profile physical placement; states/permissions/schema/lock protocol; SMTP/storage/scanner interfaces and first-use configuration; minimum catalog/clearance contracts. Provider credentials never belong in the PR. Once that package is explicitly accepted, a single bounded production authorization can permit the implementation sequence below without per-routine-step approvals. First-use production configuration and safe failures remain mandatory, not silently fabricated decisions.

## Full-stack sequence after explicit production authorization

1. **T003-01** accept/pin candidate contracts/fixtures; add exact customer Identity amendment preserving accepted SYSTEM/username/password/session behavior and add independent public admission tests.
2. **T003-02** forward-only migration groups and owned adapters; PG18 empty/V6 upgrade/runtime privilege/old-build compatibility/constraints/races; no V1–V6 edits.
3. **T003-03** real customer enrollment/email verification/login/refresh/logout/recovery and bounded delivery worker; labels distinguish queued/provider-accepted/delivered. No public rollout.
4. **T003-04** independent customer profile, application draft/submission/revision/evidence screening, private access, dry-run retention and purpose contact attestations.
5. **T003-05** current personal/multi-org context, trusted authority bridge, scoped paging/cache invalidation and linearizable revoke tests.
6. **T003-06** independent Business Admin review and org+initial Owner atomic activation, rejection/info request/audit/activation-fault recovery.
7. **T003-07** business invitations, staff admission/grants/delegation/revoke/leave, pending-invite invalidation, last-Owner protection.
8. **T003-08** legal-profile revisions, org suspension/reinstatement/closure-safe pending, controlled transfer/two-person Owner recovery.
9. **T003-09** implement minimum004/005 owner subset before003 facility/defaults acceptance; preserve single pickup command owner004 and Dispatch Station boundary.
10. **T003-10** integrated production web pages/client, real browser journeys and accessibility/current-state/recovery behavior for every owned CORE and mandatory deny.
11. **T003-11** full negative/concurrency/privacy/migration/architecture/contract/H2/PG18 regression; exact source+PR integration SHA/reports/artifact digests and honest skips/findings.
12. **T003-12** all assigned BC, S003 and REQ-AB-ENTRY/CONTEXT/legacy-consumer obligations evidence complete; required PR CI, Owner merge, exact merge-SHA CI, **separate Owner full-Spec003 acceptance**. Group A–F cross-Spec acceptance remains open.

## Acceptance impact of remaining dependencies

CAT-004/POL-005 implementation is required for successful BC159 defaults and its complete acceptance; missing/stale/unavailable must safely reject saving unsupported references. It does not stop independent enrollment/profile/application/member development once authorized. Closure without authoritative Orders/Finance/contract/claims clearance stays pending; no invented empty obligations. Delivery/storage/screening deployment configuration is required before real email/clean-file E2E, not an excuse to pretend mock adapters are production acceptance. Verified phone remains future channel-dependent; initial email login/recovery does not claim phone authentication. Destructive purge/public Internet deployment remain explicitly out of scope.

Coverage and commands/evidence: [contract package](contracts/README.md), [coverage](contracts/coverage.csv), [static receipt](../../docs/reconciliation/v3/spec003-readiness-validation.md). No actual migrations, provider setup, services, browser or production BE/FE implementation are authorized by this docs task.
