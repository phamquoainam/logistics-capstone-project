# P0-CTX-01 — exact Identity callback ownership verification

Status: **PROPOSED — SECURITY GATE FAIL; OWNER TECHNICAL APPROVAL REQUIRED**.
Parent manifest SHA remains unchanged:
`90c795bd7e773b6a58d0d220056cfbc5e9a5ee95b38ae2493d5b9f75a372291f`.
This does not reopen V42 scope, P0-BC-01/02 or P0-OP-01.

## Observed failure, not a hypothetical HTTP exploit

Focused PG18 negative gate:
`IdentityGrantApplicationTests#merchantGovernanceRejectsConstructedIdentityContexts`.
Artifact: `.demo/cskh-focused-4263911b3b4c47aaa5fc161ec93aa9c5/`.
Result: **1 FAIL**, containing two failed denial assertions;
`SOURCE_UNCHANGED=True`.

The test creates two genuine dedicated synthetic Admins in the owned test DB.
Only Admin B has a persisted Merchant policy assignment. During a genuine Admin A
callback, a Java interface wrapper substitutes accountId=B while forwarding
requireCurrent to A's genuine callback. JdbcMerchantGovernance accepts B's scope
for both READ and COMMAND wrappers. Expected AdminFailure was not thrown.

This is an in-process public-port ownership gap. No claim of unauthenticated
remote exploitation or real-target breach is made. The test reads a synthetic
policy only, commits no forged business decision, and touches no demo/UAT target.
Existing Admin step-up already rejects substituted contexts via a private Identity
issued-context registry; Merchant cannot call that private implementation.

## Why a bounded interface delta is needed

Calling the supplied interface's requireCurrent cannot prove that the supplied
object is the one Identity issued. Merchant must not import Identity internals,
trust implementation class names, reject only java.lang.reflect.Proxy, inspect
thread-locals reflectively, or repeat private Identity SQL. Reentering execute
would create/reject a nested transaction rather than validate the existing one.

Keep both existing execute signatures and both context interfaces unchanged.
Add exactly:

```java
// identity.api.admin.AdminAdmission
default void requireCurrent(AdminReadContext context);  // default denies

// identity.api.admin.AdminCommandAdmission (P0-PORT-01)
default void requireCurrent(AdminCommandContext context); // default denies
```

Both default implementations throw the existing safe AdminFailure ACCESS_DENIED.
No new HTTP path, DTO, permission, role, table, migration, SQL signature or ACL.
No grant, provisioning, principal linking or realm activation.

## Identity-owned implementation [PROPOSED]

1. Command implementation reuses JdbcAdminCommandAdmission's existing private
   issued-context/source registry already used by step-up. It requires exact
   object identity, same current DataSource resource and current callback lifetime,
   then invokes the genuine context's own current session/authority checks.
2. Read implementation tracks the exact issued context and physical DataSource
   resource for the existing callback. Verify those, current callback lifetime,
   read-only REPEATABLE_READ and existing session/permission/deadline checks.
   Also bind the genuine read context's own requireCurrent to its original physical
   resource, closing a suspended REQUIRES_NEW context with identical isolation.
3. A wrapper/constructed/captured/cross-source/suspended context never authorizes,
   even when it forwards a genuine object's requireCurrent or copies identifiers.
4. No caller-accessible registry issuance or mutable context is introduced.
   Cleanup happens in finally, including exceptions/rollback.

## Merchant consumption [PROPOSED]

Inject the existing Identity read/command admission ports into the Merchant scope
adapter/review composition. At every withScope boundary call the appropriate
owner's requireCurrent(context) **before reading any actor, realm or scope fields**.
Keep current persisted policy/scope/expiry checks and callback/physical-resource
validation. Do not treat the new check as a permission or an alternate admission.

There is no permissive compatibility fallback. Default-off legacy Dashboard APIs
do not use this new Merchant scope path. A custom admission implementation that
does not support the owner check fails closed rather than manufacturing authority.

## Required verification

- Keep the existing negative test unchanged and make both denial assertions PASS.
- Real contexts and all currently passing8 focused V42/legacy gates remain PASS.
- Exact forwarding wrapper, substituted actor/realm/version, constructed instance,
  escaped callback, cross-DataSource and suspended read/command transaction denial.
- Revocation/expiry and genuine current admission still enforced; no context from
  Customer, Staff or Identity Root becomes dedicated Admin governance authority.
- Relevant HTTP/HTTPS Browser, architecture and legacy six-READ regressions.
- No change to V1–V41, approved manifest bytes, WIP archives or real databases.

## Boundary and disposition

Source/test on disposable PostgreSQL18 only after exact approval. The negative
gate remains FAIL until then; no exception/assertion is weakened or skipped.
New Governance HTTP/decision exposure is held. Safe Customer evidence/contact UI
and ordinary verification continue independently.

AGENTS.md requires approved proposed contracts, and the current Owner directive
requires surfacing an exact public API/security-boundary delta outside the approved
manifest. These two new admission methods are therefore proposed here rather than
self-approved or hidden in an internal import/class-name workaround.
