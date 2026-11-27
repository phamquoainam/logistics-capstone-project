# DEC-O3 clarification — bounded Owner Transfer delta

2026-11-26 · Business policy **OWNER APPROVED**; shared technical changes remain
**BLOCKED_COORDINATION**, not authorization to execute a transfer.

Parent technical manifest is preserved byte-for-byte:
`6d072d9b649ab144c2fb9b8b780c8f077757387bcef58536a31618f9154251a2`.
This delta supersedes only its former outgoing `SUSPENDED` candidate. The Owner's
latest explicit clarification selects existing `ACTIVE OPERATOR`, not a new MEMBER
role. Step A prepares the current-owner bridge; it never changes a membership.

| Stable ID | Exact required result / implementation boundary |
|---|---|
| M2-O3-01 | Outgoing membership retains account, membership and Organization IDs; role becomes OPERATOR, state ACTIVE. |
| M2-O3-02 | Effective authority in that Organization is exactly one explicit ORGANIZATION `dashboard.read`. Remove all other Organization and Facility grants, including Team/Finance/Support administration and former Owner privileges. No role defaults or implicit bypass. |
| M2-O3-03 | Incoming verified eligible membership becomes current OWNER with exactly `dashboard.read` and `ownership.transfer.request` per approved grant contract. Do not inherit Finance/Carrier/Platform or outgoing grants. |
| M2-O3-04 | Preserve immutable Initial Owner and activation provenance. Pointer/version, both memberships/authority versions, exact grant delta, affected pending invitations, ownership history, decision audit and request receipt commit atomically. |
| M2-O3-05 | Invalidate only Business authority dependent on the former Owner in this Organization. Preserve Personal authentication and memberships/authority in other Organizations. |
| M2-O3-06 | Require case/revision/party/intent-bound genuine two-party consent, independent current decision/execute authority, last/current-Owner protection, replay/concurrency and rollback gates before execution. |

## Existing role compatibility evidence

`merchant/internal/domain/OrganizationTeamPolicy.java` explicitly defines grant
ceilings, not implicit grants. `JdbcBusinessWorkspaces.rows()` loads actual
`member_grant` ORGANIZATION rows and requires explicit `dashboard.read`;
`Scope.permits()` accepts only those permissions or explicit Facility grants.
No automatic OPERATOR privilege was found in these admission paths. This source
review does **not** establish successful transfer or full effective-authority
acceptance; post-transfer HTTP/DB negative tests are still mandatory.

## Exact outstanding coordination

- Team/Business owners: V23 invariants and Initial Owner guards, V34
  `team_permission`/`configure_local_workspace`, current `JdbcOrganizationTeam`
  and `JdbcBusinessWorkspaces`, invitation and authority-version invalidation.
  Forward amendments must retain Initial Owner history while using Current Owner
  for live authority. No shared source is rewritten in Step A.
- Identity owner: approved-intent proof interfaces and operation binding still
  need exact owner confirmation. Existing Organization recovery consent is not a
  case/revision/party-bound transfer consent.
- Merchant: existing M2O endpoints/grant/ownership-history design remains subject
  to Step C technical authorization and the coordinated guards above.

No new role, API, SQL signature, Team migration or permission is introduced by
this clarification document. No real grant, transfer, deployment or Git mutation.
