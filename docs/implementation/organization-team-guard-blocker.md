# OTA-AMEND-01 — automatic review blocker

Current status: **SOURCE PATCH APPLIED AFTER OWNER RISK GO**. Owner separately
confirmed the bounded guard replacement and isolated test execution. Automatic
review then accepted the patch in V34. The historical rejection/proposal below
is retained as evidence, not an outstanding approval request. No localhost
migration/grant was performed. Runtime ACL has its own separately confirmed
checkpoint in [runtime ACL disposition](organization-team-runtime-acl-checkpoint.md).

2026-11-17. The Owner's exact technical approval of manifest SHA-256
`f6e21ee3ec3f742035d37f2c10d624031cc450a9b590939df878e9397952f426`
is effective and is not being reopened as a business/technical scope review.

## Action blocked by automatic approval review

A forward-only definition of `merchant.guard_workspace_identity()` in V34,
preserving V23 immutable membership identity/provenance, exact version increments,
terminal revoke and initial/last Owner protections, while permitting only the
accepted MANAGER/OPERATOR edits under current-transaction Team evidence.

Automatic review rejected the latest patch with the reason:

> thay thế guard V23 và thêm kiểm tra quyền/reauth trong migration, tạo thay đổi authority bảo mật persistent với blast radius rộng

It also said preservation had not yet been demonstrated and rejected another
retry. The patch is **not applied**. Do not execute it through a temporary SQL
file, test-only alternate implementation, shell write or another tool.

## Concrete proposed change, not executed

| Existing behavior | Proposed constrained amendment |
| --- | --- |
| V23 rejects any role change | Only MANAGER↔OPERATOR, never OWNER; require the evidence below |
| Immutable id/org/account/createdAt; exact version+authorityVersion increment | Keep unchanged |
| Organization initial_owner_membership_id/origin/environment immutable | Keep unchanged |
| ACTIVE Organization requires original ACTIVE OWNER; REVOKED terminal | Keep unchanged |
| Operator runs existing schema-owner/Local-Test path | Keep its authority independent; no runtime operator EXECUTE |

The proposed new predicate would require, **before** a runtime member mutation:

1. Same-Organization non-self, non-Owner target; exact prior/new version and legal state transition.
2. A Team receipt created in the same PostgreSQL transaction, matching target,
   operation, result version and action timestamp.
3. Matching same-Org Team audit, actual ACTIVE Owner actor and explicit current
   team.access.manage or team.membership.manage grant.
4. Existing Identity reauthentication consumed by that same actor in the current
   transaction, with current security version and valid session/deadline. Identity
   would expose a boolean check; Merchant would not read credential tables.
5. All old grants/scopes checked against actor rights and the approved old-role
   ceiling. New grant writes require their own complete guard; this change alone
   does not authorize them.

Source service now stages receipt/audit before the row update in the same joined
transaction, so a BEFORE guard can inspect actual evidence. Both are rolled back
on failure. Same-key replay is checked before ETag/consumed reauthentication and
does not stage new writes. Runtime write privileges remain REVOKED until the
complete invitation/grant/delivery guard set is implemented and verified.

## Evidence already observed

- Four pure Team policy tests PASS.
- Initial V34 migration/FK/CHECK/owner/data-preservation tests2/2 PASS before
  subsequent additive guard work; not a claim of final V34 acceptance.
- Operator TDD failed at WORKSPACE_COMMAND_INVALID; static operator amendment
  subsequently accepted by automatic review. Operator grant test then PASS.
- Directory HTTP test PASS: permission/self-vs-Org isolation, current revoke,
  role templates and no account/email/phone fields in the member projection.
- Lifecycle/replay test remains FAIL at SQLSTATE42501 / HTTP503 because write
  privileges have intentionally not been granted. No fake PASS.

No localhost migration/grant, credentials/certificates/demo changes, historical
migration edits, Owner transfer or merge. Full OTA-04/05 remain incomplete.

## Required disposition

If automatic review cannot accept the bounded amendment from the existing
approval/evidence, an explicit Owner risk disposition is required for **this exact
guard replacement and its current-transaction evidence checks**, test database
only. This is an automatic approval-review blocker, not a request to approve the
same15 APIs or expand the core manifest. Further checks must not bypass review.
