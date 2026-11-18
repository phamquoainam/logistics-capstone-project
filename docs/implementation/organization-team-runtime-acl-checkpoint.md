# Organization Team V1 — bounded runtime ACL checkpoint

Current status: **SOURCE PATCH APPLIED AFTER OWNER RISK GO**. Owner explicitly
answered “Cho phép patch ACL giới hạn và kiểm thử DB riêng”; normal automatic
review accepted the exact patch below. It is applied only to isolated test
databases during verification. The earlier rejection remains historical
evidence. Localhost deployment/grants and full acceptance remain unauthorized
and unclaimed; this is no longer an outstanding approval request.

2026-11-17. Technical approval remains effective for frozen manifest
`f6e21ee3ec3f742035d37f2c10d624031cc450a9b590939df878e9397952f426`.
The separately approved V23 guard replacement is present in source V34.
This document records an automatic approval-review rejection, not a new design
or business review. No new API/DTO/table/role or deployment scope is proposed.

## Blocked action and reason

Automatic approval review rejected the following **source V34** ACL patch:

> Patch cấp quyền INSERT/UPDATE/DELETE cho runtime trên các bảng Membership/Grant/Invitation/Audit/Receipt khi transcript còn ghi rõ nhiều guard và lifecycle/invitation test chưa hoàn tất; đây là thay đổi quyền bảo mật persistent với blast radius đáng kể và chưa được Owner ủy quyền ở checkpoint này.

The patch was not applied. Current runtime has SELECT only on the Team tables
and cannot write canonical Membership/Grants. Do not run equivalent grants in
test setup, temporary SQL, a different migration or another tool to bypass review.

## Exact proposed patch

Append after the current SELECT grants in V34, retaining every existing guard:

```sql
GRANT INSERT ON merchant.membership,merchant.member_grant,
 merchant.team_invitation,merchant.team_invitation_grant,
 merchant.team_audit,merchant.team_command_receipt TO "${runtimeRole}";
GRANT UPDATE(role,state,authority_version,version,updated_at)
 ON merchant.membership TO "${runtimeRole}";
GRANT UPDATE(state,proof_digest,proof_version,expires_at,
 accepted_membership_id,version,updated_at)
 ON merchant.team_invitation TO "${runtimeRole}";
GRANT DELETE ON merchant.member_grant TO "${runtimeRole}";
```

No UPDATE/DELETE on audit, receipts or invitation grants; no runtime writes on
Team Policy or Organization; no execution of offline operator configuration.
Column grants cannot change member identity/Organization/creation timestamp.
Initial Owner, version checks and terminal revoke remain enforced.

## Installed guards and current evidence

- Runtime Team permission grants denied by the independent-operator trigger.
- Member role/lifecycle changes require current-transaction receipt/audit,
  actual active Owner, explicit permission and consumed session-bound reauth.
- Member insertion requires accepted invitation, recipient-bound consumed
  Identity proof and receipt. Grant insertion must match exact invitation intent
  or current Owner access command; direct grant changes require membership
  authority/version mutation in the same transaction.
- Invitation issue/resend requires existing protected Identity proof/outbox;
  accept/decline requires consumed proof with actual recipient session/version.
- Immutable intent/history, exact versions, same-Organization FKs, deferred
  resource/audit/receipt/grant checks and offline-policy invalidation are in source.
- **Observed PG18 PASS: 4/4** migration/preservation/FK/CHECK, independent
  operator grant denial/audit, directory HTTP/current permission/privacy tests.
- **Observed non-PG Backend PASS: 33/33** Team policy, real JCE protected token
  and encrypted contact, SMTP loopback/TLS. No live Gmail test.
- Lifecycle previously failed HTTP503/SQLSTATE42501 under intentionally read-only
  ACL. Invitation/lifecycle/negative direct-DML coverage after the proposed grants
  remains unverified. This is not full OTA-04/05 completion or security acceptance.

## Required risk disposition

Allow **only this source ACL patch** through normal automatic review, then run
isolated PG18 tests of lifecycle, invitation, privilege escalation, replay,
concurrency, rollback and direct DML before declaring the lane accepted. A failed
affected control must be fixed or the lane kept disabled; never skip a guard.

No localhost migration/grant/data change, Production, credentials/certificates,
Owner transfer, extra role templates, commit/push or merge is authorized here.
