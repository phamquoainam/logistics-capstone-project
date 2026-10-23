# ADR-0010: Registered system Identity Realm and bounded delegation

- Status: ACCEPTED for the exact REALM-P1/P2/P3 scope approved by owner on 2026-10-22; implementation runtime verification PENDING.
- Authority: OWNER_REALM_P1 APPROVED_WITH_SAFETY_CONDITIONS; OWNER_REALM_P2 APPROVED_WITH_CONTROLLED_BOOTSTRAP; OWNER_REALM_P3 APPROVED_FOR_EXACT_SAME_REALM_IDENTITY_MANAGEMENT_DELEGATION.
- Scope: one SYSTEM Identity security Realm. No organization/tenant hierarchy, external owner permission, HTTP endpoint or production bootstrap execution.
- Preserves [ADR-0009](ADR-0009-production-identity-lifecycle.md), original Feature002 REQ/AC and accepted wire formats.

## Decision

IDENTITY.scopeId refers to identity.realm.id, a registered authoritative UUID supplied explicitly by the operator to migration. No fallback UUID, registration from existing grants or Merchant/account-to-Realm inference. Migration V4 adds the registry, initially INACTIVE, and a stored generated IDENTITY-only reference/FK; external scope IDs remain under their own owners. Unmatched legacy Identity references abort the entire migration; no V1/V2/V3 edits, grant rewriting or repair.

Runtime has SELECT-only access to Realm registration. Owner-only SECURITY INVOKER functions create a brand-new PENDING_ACTIVATION initial administrator and later activate Realm. A unique bootstrap-account binding prevents takeover/repeated bootstrap; immutable first-activation timestamp prevents replay or uncontrolled reactivation. Both functions use the existing Identity advisory lock and append operator-principal/correlation audit atomically. Neither function creates a credential, verifies email, consumes invitation proof or exposes a public signup endpoint.

Realm activation requires the bound new account to be ACTIVE, have verified email/activated credential, a consumed nonrevoked invitation proof created after registration, and only the exact Identity Management grant in the Realm. Completing the accepted invitation flow remains a separate Application responsibility; test fixtures simulating it are never production bootstrap logic. No active authority exists during partial bootstrap.

## Target eligibility and exact policy

The existing account_grant relation is authoritative account-to-scope context for this bounded replacement operation: rows are FK-bound to account_id, loaded as the complete current target grants after taking the existing Identity administration/account/session locks. Eligibility requires a different existing target, a nonempty current set, and ALL current and requested pairs to be exactly IDENTITY_MANAGEMENT/IDENTITY/the same registered ACTIVE Realm. Mere resource/FK existence is insufficient. No general account membership or tenant invariant is introduced.

Actor account/session/authVersion are loaded and admitted by ReplaceIdentityGrants. The actor must hold the exact current Identity Management pair for this Realm. REALM-P3 explicitly gives this dedicated administrative capability management AND assignability of that same pair; ordinary grant possession conveys neither right. All external/other-role/mixed/empty target contexts deny. Existing Domain guards still own self-management, last-active-admin, version/replay ordering and mutation invariants; no role wildcard or Admin-to-Finance implication.

The default production composition uses a real same-DataSource JDBC registry provider, inside an active writable transaction holding the existing Identity administration lock. It performs no network/cached lookup and returns no authority without the lock/registry/activation/policy conditions. Bootstrap/state administration must participate in the same local lock protocol. No independent module transaction atomicity is claimed.

## Consequences and verification

V4 requires an explicit IDENTITY_SYSTEM_REALM_ID for migration-enabled PostgreSQL profiles; runtime does not read this variable as authority. Runtime-only/no-migration operation needs pre-applied V4. Older database with unmatched scope IDs needs an owner-reviewed mapping plan before upgrade. Demo/H2 schemas and accepted JSON fields/statuses remain unchanged.

Actual SQL migration/bootstrap/provider and current-source regression evidence is mandatory before acceptance. Required tests include cold/upgrade/FK/runtime privileges, duplicate/takeover/prerequisite/rollback bootstrap, exact same-Realm management, inactive/unavailable/external/hidden-target denial, current session/self/stale/audit rollback and real Realm deactivation concurrency. Existing tests and strict privacy/cleanup gates stay active.

T-002-05 remains PARTIAL; this ADR does not accept Feature002 or AC-002-01..05. [Operator procedure](../../runbooks/identity-realm-bootstrap.md) and [implementation evidence](../../implementation/t-002-05-identity-application-evidence.md) contain the bounded runtime handoff.

## Bounded ordinary provisioning extension — owner approved 2026-10-22

OWNER_PROVISION_INITIAL_IDENTITY_PAIR: APPROVED_WITH_RESTRICTIONS explicitly
extends assignability ONLY for provisionIdentityAccount. This does not expand
the existing replacement policy above. The actor must be admitted from current
persisted account/session/authVersion and hold the dedicated management pair
in the registered ACTIVE Realm R, under the existing administration transaction
lock. Ordinary grants, registry/FK existence or a Demo principal never delegate.

An explicit request may assign exactly IDENTITY_MANAGEMENT/IDENTITY/R to a NEW
server-identified PENDING_ACTIVATION/UNVERIFIED account at version/authVersion1.
No default grant is added: empty, external, mixed, other-role and cross-Realm
requests deny. No client-supplied account identity, existing-account takeover,
login/session issue, password setup or Realm activation is part of this action.
For this bounded operation the authoritative membership is the new account's
exact requested pair, persisted atomically under the registered Realm's V4 FK;
no separate tenant/membership entity or inference from Merchant/account IDs exists.
If that binding or current delegation cannot be established, deny.

The distinct provisioning policy authorizes new pending targets only. Replacement
still denies an empty target. Replay rechecks current actor/session and the full
stored target grant set, then returns an authorized account view without repeating
account/proof/audit/receipt/delivery effects. Minimum24h receipt retention remains.

Prior Realm source4a90baf/run39795074146 attempt1 is CI_VERIFIED: Architecture23,
H2296/17 expected skips, PostgreSQL395/5,104 policy controls/job and owned cleanup.
That evidence does NOT verify the new provisioning extension.

The application now has a trusted InvitationIssuer port. A production implementation
must generate opaque proof material, retain only the approved hash in action_proof,
and append durable protected delivery intent in the SAME runtime transaction with
no external I/O. V3/V4 have no delivery-intent storage. Schema, payload protection/
key ownership and the real issuer adapter remain unimplemented dependencies needing
a separately reviewed decision/forward migration; no migration is authorized here.
The default composition without that adapter fails BEFORE any account write.
Synthetic test intent metadata is not a deployable outbox or cryptographic proof.
T-002-05 remains PARTIAL; this extension's Java/PostgreSQL runtime is UNVERIFIED.
