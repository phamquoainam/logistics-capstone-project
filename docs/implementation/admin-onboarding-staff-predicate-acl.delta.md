# AON-ACL-01 - bounded Staff predicate dependency

Status: **OWNER APPROVED, APPLIED AND FOCUSED TESTED**. Source/test only; 2026-11-22.
Parent manifest remains byte-identical, SHA-256
`025ae708cb97c01cf966a695402f730d5073364d7b0352298b7a27a644c9f0bc`.
V39/V40 Option B source/test approval is retained; it is not requested again.

## Exact proposed delta

Owner's AON-ACL-01 directive approved exactly this statement. It is applied before
ownership transfer. PG18 catalog/Staff writer regression PASS evidence is in
[the execution report](admin-onboarding-execution.md). Historical proposal and
first failure reasoning below are retained; no repeated approval is requested.

Add only this caller to the already approved AON-SQL-19 function:

```sql
GRANT EXECUTE ON FUNCTION identity.local_admin_registered(UUID)
TO identity_staff_writer;
```

This is a BOOLEAN-only existing-marker predicate, including revoked markers. It
does not return account/profile/permission rows, grant any Admin code, create any
membership or permit a write. Unlisted callers remain denied by ACL; marker
existence is not current admission and must still protect revoked identities.
No PUBLIC grant is restored.

Apply ACL while the migration role is still the function owner, before transferring
ownership to the NOLOGIN Admin writer. Do not repair an already-applied migration;
V39 is still uncommitted source and has only been exercised in disposable test DBs.

## Why it is needed

V39 adds global shared-Identity guards with SECURITY INVOKER provenance checks.
The existing canonical Staff provisioning/activation functions execute under the
NOLOGIN `identity_staff_writer` role and legitimately update unmarked Identity
rows. Those guards invoke the new marker predicate to distinguish an Admin target.
Parent AON-SQL-19 lists runtime/Admin writer/constrained Merchant writer callers,
but does not explicitly include the existing Staff writer.

Do not grant SELECT on the three private Admin tables, operator EXECUTE, writer
SET/INHERIT or any Admin/Identity Management/Support permission to a real account.
Do not replace this dependency with PUBLIC EXECUTE, GUC authority, disabled guards
or weaker current_user checks.

## Evidence and safer implementation boundary

The initial prototype applied REVOKE/GRANT after ownership transfer. With SET-only
NOINHERIT membership PostgreSQL produced SQLSTATE01006/01007 warnings and left
PUBLIC EXECUTE in place. The strengthened test caught this using catalog ACLs;
the earlier positive Staff update was not evidence of correct least privilege.
Ordering is now corrected within the already approved ACL scope; it is not a new
grant or a waiver of the failing Staff compatibility test.

An alternative patch changing Account/Grant/Delivery guards to SECURITY DEFINER
and replacing writer checks with transaction/xmin evidence was rejected by
automatic approval review. It was **not applied**. Its stated risk was a broader
persistent privilege-boundary change and an unproven bypass risk. This proposal
does not retry or indirectly implement that rejected design.

Retain the original SECURITY INVOKER/current_user writer provenance guards.
The narrow extra predicate EXECUTE is presented explicitly because it changes a
persistent ACL beyond the parent manifest's exact caller list.

## Required focused verification after approval

1. Catalog proves no PUBLIC EXECUTE; only the exact additional Staff writer
   caller can invoke this predicate. It receives no private table SELECT or
   operator/activation/permission-management function privilege.
2. Existing controlled Staff writer can update an unmarked synthetic Identity
   row; the regression must affect exactly one row under the actual Staff role.
3. A marked Admin target remains protected: Staff direct Account/Grant/Proof writes
   deny; Customer/Staff purpose conversion and Merchant membership deny.
4. Runtime/operator cannot SET Admin writer; no real login gets membership.
5. Relevant canonical Staff activation/session/Support regressions pass on owned
   PG18. Do not claim source/test ready from the narrow regression alone.

No new API, DTO, table, role or schema-owner authority is proposed. No deployment,
shared/demo mutation, real provisioning/grant, Root change or Git publication.
