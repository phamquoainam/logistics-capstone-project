# Menu 2 — bounded remaining contracts [PROPOSED]

Status: **BLOCKED_CONTRACT — design only**. No new API/DTO/schema/permission is
approved by this document. Latest coverage source is
`admin-console-functional-coverage.csv` ADM-02/03. Existing V41–V43 and Initial
Owner invariants remain unchanged. Do not implement this proposal through the
existing READ grant, operator functions, Customer Team API or direct DML.

## Current Owner checkpoint — 2026-11-25

Owner now prioritizes completion of Menu2 and explicitly forbids moving to Menu3.
This earlier bounded brief is expanded, not approved, by:
- `admin-menu2-completion-contract.proposed.md`;
- `admin-menu2-completion-technical-manifest.proposed.json`;
- `admin-menu2-completion-technical-review.md`.

The expanded manifest distinguishes mandatory CSV requirements from optional
extensions and exact new APIs/DTOs/schema/capabilities/ports. Membership oversight
is mandatory per ADM-03, even though the older brief below described an optional
reader. No prior P0 approval is requested again; only these new deltas/policies
need review. All new technical/business items remain PROPOSED, and shared owners
must coordinate before their implementations are changed.

## M2-R01 — read projections [PROPOSED]

Owner: Merchant; consumers: Admin Menu 2. Identity/Notifications/contracts and
warehouse owners expose reviewed public ports; no private cross-domain SQL.

- Extend the bounded directory/detail projection with independently authorized
  legal profile, verified contact and approved service/contract references.
  Determine field sensitivity, purpose/audit and source availability explicitly.
- Add server-side exact application-ID/applicant/legal/tax search, with scope
  applied before pagination. Never label current-page filtering as global search.
- Historical revision comparison must read two exact immutable submitted
  revisions, not current draft. Proposed operation is an audited disclosure under
  current `application.read` plus `application.evidence.read`, not Dashboard READ.
  Return only reviewed fields and evidence metadata; no raw private object or
  unrestricted document URL. Confirm exact route/DTO before implementation.
- Optional Organization membership oversight uses a Merchant-owned bounded
  reader, never grants access to Merchant Team mutation or Identity credentials.
- Notice status requires a Notifications-owned reader exposing template/state/
  attempt timestamp/provider-acceptance metadata only, bound to the authorized
  application event. Never infer delivered from SMTP acceptance; no mailbox or
  proof disclosure. No second outbox or notification center.

Approval prerequisite: exact path/query/DTO/port manifest, permission and resource
scope matrix, disclosure policy and ownership coordination. Current contracts do
not approve these additions merely because storage already exists.

## M2-L01 — Organization lifecycle/restrictions [PROPOSED]

Owner: Merchant governance, with Finance and active work readers as dependencies.
Current execution remains ACTIVE↔SUSPENDED only.

- Define RESTRICTED/CLOSED semantics, allowed transitions, effective/expiry time,
  targeted capabilities, independent approver and executor-domain consequences.
- Define what happens with open Shipments, custody, unresolved Support/Claims and
  UNPAID obligations; unavailable Finance facts must not silently permit closure.
- A restriction changes future admission under current Backend authorization; it
  must not rewrite booked snapshots or delete Shipment/custody/ledger/audit.
- Persist versioned reason, actor/scope, immutable event and request receipt in
  one canonical transaction. Require reauthentication and replay-safe receipts.
- Ownership dispute/reverification is a case workflow with authority independent
  from support intake; an Admin comment cannot resolve ownership.

Approval prerequisite: business policy and exact state/schema/API/authority delta.
No migration version is reserved and no historical migration may be edited.

## M2-O01 — Owner Transfer [PROPOSED]

Owner: Merchant organization ownership; Identity owns eligibility, proof/session
and current admission. Coordinate with the team responsible for IAM/Team before
changing shared contracts. Existing Initial Owner remains immutable historical
provenance. This proposal does not authorize rewriting that field or V23/V34.

1. A transfer case must bind exact Organization, current ownership version,
   verified transferor/transferee and explicit intent/consent. Prove applicant and
   recipient eligibility using Identity-owned context/proof ports; never take a
   client actor or email claim as authority.
2. An independent authorized reviewer/decider checks both parties, conflict,
   disputes, last-Owner safety and any approved outstanding-obligation rules.
   Merchant Owner is not a platform Admin and cannot self-grant approval rights.
3. Reauthentication is bound to case/version/input digest and action. Pending,
   expired, declined, revoked, conflicted or stale cases cannot execute.
4. Approval is separate from execution. Execution rechecks current eligibility,
   Organization state, granted ceiling and ownership version under locks; record
   current ownership history without destroying original activation provenance.
5. Old/new roles, grants and Business sessions require an explicit policy. No
   implicit transfer of financial/operator powers, Personal data, another
   Organization's Membership or platform authority. Never leave zero valid Owners.
6. Case decision, ownership transition, old/new grant changes, targeted session
   invalidation, event/receipt/audit and committed notice must be atomic and
   idempotent. Same input replays return its receipt only to its rightful actor;
   different input/key reuse, concurrent transfers and replay-after-revoke deny.

Approval prerequisite: choose transfer/consent and independent approval policy,
settle historical Initial Owner vs current Owner semantics, then review one exact
Merchant/Identity technical manifest (routes/DTOs/storage/ACL/proof/receipt ports).
No permission names, SQL signatures, role templates or new tables are implicitly
approved here. No UI button executes transfer in the current implementation.

## Required future tests, only after approval

Focused isolated PG18/HTTP/HTTPS: scoped projection/PII denial, stale/double
decision, revoked actor/recipient, forged/expired proof, two concurrent transfers,
last Owner, receipt replay with consumed proof, rollback of every affected table,
outbox-after-commit, old session denial and unaffected Personal/other-Org access.
For read-only projections test exact cohorts/paging and redacted unavailable data.
These tests are **NOT RUN** for this design-only proposal.
