# Controlled system Identity Realm registration and activation

Scope: [ADR-0010](../architecture/decisions/ADR-0010-identity-system-realm.md). Operator procedure only; none of these privileged operations were executed by the implementation task. No public registration/bootstrap endpoint, default account, password or Realm UUID exists.

## Before migration

1. Use the existing approved owner/migrator connection to the explicitly intended PostgreSQL database. Verify database identity, owner, V1/V2/V3 history/checksums and a rollback-capable backup under the owner's procedure. Do not target developer/foreign data during verification.
2. Select one stable Realm UUID explicitly and record it in the operator's private deployment record. Do not derive it from account, Merchant or the first grant. Review distinct existing IDENTITY scope IDs read-only. If any differ from the chosen ID, STOP for mapping review; do not rewrite/delete them.
3. Set IDENTITY_SYSTEM_REALM_ID in the private migration process configuration. Existing Spring migration/PostgreSQL profiles pass it as identitySystemRealmId; no default exists. It is a UUID identifier, not a secret. Never copy connection credentials or account PII into Git/public evidence.
4. Quiesce incompatible writers and run the existing controlled migration job. V4 registers one INACTIVE SYSTEM Realm, validates existing references and adds the generated FK. Validate/rerun must preserve checksums and data. Keep raw migration diagnostics private; publish only sanitized outcomes. Never run clean/repair to hide a failure.

## Initial administrator registration

Use an owner-authorized psql session with credentials supplied through the approved private connection mechanism, never a password in shell arguments or SQL examples. Keep SQL logs private. Supply operator-selected psql variables owner_role, realm_id, NEW account_id, username, display_name, email and request_id. There are deliberately no real/default values below. psql's quoted variable form must be retained; UUID casts reject invalid input.

```sql
\set ON_ERROR_STOP on
BEGIN;
SET LOCAL ROLE :"owner_role";
SELECT identity.bootstrap_system_realm_admin(
  :'realm_id'::uuid, :'account_id'::uuid, :'username', :'display_name',
  :'email', :'request_id'::uuid
);
COMMIT;
```

This INSERT-only function creates a NEW pending account and exact Realm grant, binds it as the initial administrator, and appends provenance audit in one transaction. It never updates or takes over an existing account, sets a password or marks email verified. Duplicate account/username/request/audit or second bootstrap fails without partial effects. Do not replace failure with an upsert or change the supplied ID until provenance is reviewed.

## Activation prerequisite

The bound pending account must complete the separately implemented accepted verified-email invitation workflow: atomic proof consumption, credential activation, verified email and audit/receipt behavior under ADR-0009. That end-to-end Application workflow is not completed merely by this bootstrap task. Until it exists and is verified, STOP here; the Realm remains INACTIVE. Never manually fake email_verified, password_hash or consumed proof to obtain production activation. Synthetic test helpers are forbidden in production.

After that workflow is proven, use a NEW operator correlation request_id and the same Realm/account IDs:

```sql
\set ON_ERROR_STOP on
BEGIN;
SET LOCAL ROLE :"owner_role";
SELECT identity.activate_system_realm(
  :'realm_id'::uuid, :'account_id'::uuid, :'request_id'::uuid
);
COMMIT;
```

The owner-only function checks the exact pending-bootstrap binding, current account state, activated credential/email, consumed nonrevoked invitation and exact grant set. It takes the Identity administration lock, updates Realm state and appends audit atomically. It cannot be used on a never-bootstrapped/arbitrary account, before prerequisites, a second time, or after deactivation of an already activated Realm.

## State safety and runtime

Runtime may read Realm, not insert/update/delete/truncate it or execute bootstrap functions. Registry validity alone authorizes nobody. The production provider additionally checks admitted actor/session/authVersion, actual complete target scope context and approved exact pair policy, followed by existing Domain guards.

Any separately authorized owner maintenance of Realm state must acquire the same transaction advisory lock (5279420865908921433) BEFORE mutation and retain it through commit with provenance audit. Do not update status from another connection outside that protocol. No automatic reactivation is provided; recovery after deliberate deactivation needs a reviewed owner action. Keep owner credentials outside the application.

## Verification handoff

- Existing three required CI job names stay unchanged; no automatic dispatch/publication authorized here.
- Temporary CI and Testcontainers fixtures explicitly supply synthetic UUIDs; they never register/activate a production Realm. No developer services or credentials are used.
- Run actual V1..V4 cold/upgrade/validate/rerun, FK and permissions/bootstrap tests, provider positives/negatives and concurrency, then original regressions with strict report/privacy/cleanup audits on one exact source revision.
- The older full clean-clone checks currently contain V1/V2 history/table-count assumptions; those are a separate Foundation reconciliation gap. Do not claim that onboarding/runtime gate PASS from the backend tests or rewrite its existing acceptance record here.
