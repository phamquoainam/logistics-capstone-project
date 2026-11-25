# Admin Local/Test authority - single technical review checkpoint

Date: 2026-11-22. Current status: **ADMIN_ONBOARDING_SOURCE_TEST_READY**.
This document records the source/test review checkpoint. Subsequent separately
approved real isolated deployment is recorded in
[deployment execution](admin-uat-deployment-execution.md).

Latest Owner decision approved exact V39 + V40 Option B for source/test only;
the pending decision text below is historical. Implementation evidence is in
[AON execution](admin-onboarding-execution.md). A newly confirmed, separately
bounded [Staff predicate caller gap](admin-onboarding-staff-predicate-acl.delta.md)
was approved as AON-ACL-01 and its focused catalog/Staff regression has PASS
evidence. The parent manifest is not being presented for repeat approval.

Review [contract](admin-onboarding-contract.proposed.md) and
[exact technical manifest](admin-onboarding-technical-manifest.proposed.json).
Verified manifest SHA-256:
`025ae708cb97c01cf966a695402f730d5073364d7b0352298b7a27a644c9f0bc`.
The immutable manifest retains its original PROPOSED labels. Exact Owner approval
of Option B and AON-ACL-01/02 authorizes source/test implementation; this document
does not authorize deployment. The remaining proposal text is historical design
context; actual evidence and current disposition are in the execution report.

## Verified existing state

- Branch `feature/argopost-admin-dashboard`, HEAD
  `d7329a84822f88a1c01e70a0e23b065123860410`.
- All46 starting WIP files archived and verified at
  `.demo/admin-local-readiness-preflight-e8cfbd636cc94517b4504894a085e8b2`.
  Fingerprints of37 historical migrations retained; V39/V40 not present.
- ADM-01-08 remains SOURCE_TEST_READY. No old suite was rerun for this design task.
- Docker Engine29.6.2 responds. Exact existing container
  `elogistics-d1-postgres18`, ID
  `965042a98c43043378b8757a13cb4364a063a289c59e35f43ee7c9eeac761a63`,
  was **exited**; its stale health metadata said starting, not HEALTHY.
  Pinned PG18.6 image and existing volume `elogistics_d1_pg18_dev_data` observed.
  A read-only psql attempt could not run because the container was stopped.
  No SQL executed, no credentials printed, no container start/recreate or mutation.
- Current live Flyway/realm/operator state is therefore NOT REVERIFIED. Historical
  reports are not promoted to current V38/ACTIVE/ready evidence.

## Exact delta inventory

| Group | Exact scope | Approval disposition |
|---|---|---|
| Permissions | Existing six Admin READ codes only; no new permission code | Prior read admission accepted; issuing/revoking them is NEW authority |
| Core roles | AON-ROLE-01 offline Admin operator; AON-ROLE-02 NOLOGIN writer | PROPOSED; no login membership automatically granted |
| Core schema | AON-DB-01 environment, DB-02 principal, DB-03 durable operator receipt | PROPOSED V39; no historical edits |
| Core SQL | AON-SQL-01-10 and18-20; exact signatures in manifest | PROPOSED; no direct DML operator path |
| HTTP | AON-API-01 POST `/api/v1/auth/admin-invitations/accept` | PROPOSED verification-only; no registration/grant/login API |
| DTO/interfaces | AON-DTO-01-11 / AON-IF-01-05 | PROPOSED; optional Realm types must remain disabled if not approved |
| Guards | AON-GUARD-01-09 and11; optional10 for new isolated Root | PROPOSED; strict marked-target evidence and no authority inferred from GUC |
| Optional Realm | Distinct AON-ROLE-03 and SQL11-17 in V40 | Requires affirmative separate sensitive choice in this same review |
| Compatibility | AON-AMEND-01-07; four default-off config entries | Exact scope only; existing six GET/auth wire and prior migrations retained |

## Security findings and minimum resolution

**B1 - No authorized Admin issuance.** Ordinary provisioning's exact management
pair ceiling is intentional. Do not widen that policy or borrow CSKH's operator.
Resolution: independently authorized offline Admin capability with request-bound
receipt/current operator checks, exact finite read set and a fresh target only.

**B2 - Invitation eligibility mismatch.** Existing delivery/activation checks the
management pair. Resolution: explicit marked-Admin adapter/eligibility under the
same Identity proof/credential/outbox engines, not fake verification or a parallel
auth system. Marked identity/grant writes have complete provenance/transaction guards.

**B3 - Genuine SYSTEM prerequisite.** No currently running eligible isolated
ACTIVE realm has been identified. Ordinary invitation also cannot verify a new
Root while its realm is INACTIVE. Existing V4 bootstrap correctly refuses
activation without genuine verified credentials and a consumed proof.
Resolution choice: provide an already legitimate ACTIVE isolated realm, OR approve
the optional V40 first-bootstrap verification wrapper for a NEW isolated realm.
The latter carries more authority and is intentionally distinct from Admin reads;
it cannot modify an existing Root or apply to the stopped shared demo database.

These are one consolidated authority dependency checkpoint. Starting the shared
database alone would not resolve B1/B2/B3 and is not performed to create progress.
No production implementation/security fix is silently added while awaiting approval.

## Review conditions retained in the proposal

- No default broad permission set, Root Dashboard access, membership conversion,
  public provisioning, role-label trust, owner login or writable legacy shortcut.
- session_user/current authorization before receipt access; exact replay before
  expected-version/consumed-proof checks; current revoked state remains revoked.
- One physical transaction for mandatory writes; no SMTP inside it; post-provider
  crash may redeliver the same invitation, never activate an account.
- Existing SQL invariants protect relational/provenance consistency; external
  HMAC key possession is checked in the canonical Identity application. Do not
  misrepresent a DB CHECK as cryptographic verification of a raw invitation.
- Root/management invariant changes are not part of V39. Optional V40 never
  relaxes ordinary INACTIVE-realm checks or restores a previously activated realm.
- Fixed loopback HTTPS, secure cookies/CSRF, bounded two-connection pool admission,
  actual artifact/version readiness and no secrets/PII in public evidence.

## Functional backlog outside this authority delta

| ID | Missing canonical source/policy | Work needed before availability |
|---|---|---|
| ADM-BL-01 | Submitted Merchant review | Submission lifecycle, reviewer authority, decision events and exact timing cohort; DRAFT is insufficient |
| ADM-BL-02 | Official network | Carrier-owned Station/Hub/Partner registry, official active states and operational scopes; SIM-DROP is insufficient |
| ADM-BL-03 | Advanced Carrier lifecycle | Authorized movement/delivery/failure/return events and customer-safe projections; no inferred outcomes from Booking/print |
| ADM-BL-04 | SLA/priority | Approved promises/calendars/thresholds and escalation decision queue with roles; no invented late/urgent counters |
| ADM-BL-05 | Collection/settlement/COD | Canonical Finance obligations, collection evidence, ledger/reconciliation/payout authority and controls; UNPAID is not revenue |
| ADM-BL-06 | Technical operations | Actual queue/job/integration monitoring sources and allowlisted aggregation for technical role; current API response is not global health |

These remain UNAVAILABLE, not blockers to the bounded existing read Dashboard.
They are not authorized for implementation by the onboarding directive.

## One decision requested

Approve the exact manifest for AON core source/test and select the SYSTEM
prerequisite option A or B from the contract. Optional V40 requires explicit
affirmative approval; silence or approval of V39 alone does not approve it.
Exact later isolated deployment identities/operator membership/controlled mailbox
and selected read set must be recorded before real provisioning. Do not send any
password, invitation token, private key or Gmail App Password in the response.
