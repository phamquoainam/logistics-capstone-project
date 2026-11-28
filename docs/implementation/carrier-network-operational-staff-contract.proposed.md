# ADM-05 + ADM-12 — Carrier Network & Operational Staff V1 [PROPOSED]

Date: 2026-11-25. Status: **BLOCKED_CONTRACT / OWNER_TECHNICAL_REVIEW_REQUIRED**.
This document is design, not implementation/deployment/grant authority.
The latest Owner task and [North Star](ARGOPOST_PROJECT_NORTH_STAR.md) govern scope.
Exact IDs/signatures and proposed impacts are indexed in the
[manifest](carrier-network-operational-staff-technical-manifest.proposed.json).
[Readiness/evidence](carrier-network-operational-staff-readiness.md) separates
existing verified pilot behavior from the new, unimplemented admission boundary.

## 1. First incomplete task and owning specs

First work is the contract gate in spec011 (Station/site/Staff authority),
coordinated with spec027 (Identity/IAM), spec004 (coverage) and spec012 (next hop).
Do not mark the canonical specs fully accepted from Milestone B evidence.
BC-063 intake/custody and BC-065/066 onward retain their separate acceptance.

The current V21 intake is accepted under ADR-0014 and BR13-LOCAL-INTAKE-01:
verified Customer admission plus explicit Dispatch assignment, only PERSONAL
Local/Test simulated drop-off. It is not dedicated operational Staff admission.
V36 purpose is fixed to SUPPORT; existing Staff login/admission explicitly calls
`identity.support_staff_eligible`. Neither SYSTEM Admin nor CSKH can acquire Ops
authority through a portal switch. V21 CHECKs permit only three SIM-DROP stations.

Propose only a Local/Test foundational path: dedicated OPERATIONS Staff -> current
site assignment -> approved origin drop-off -> the same all-Parcels intake and
custody transaction. Directory/topology may describe STATION/HUB, but HUB handling,
sorting/manifest/transfer/dispatch commands remain unimplemented and denied.
Do not reinterpret Merchant facilities or SIM codes as official geographic data.

## 2. Reuse and bounded amendments

- Reuse Identity account/credential/password/proof/outbox/session/rate-limit/TLS
  engines. One new adapter/audience, not a second authentication engine.
- Keep SUPPORT-only functions/admission semantics and SYSTEM/Customer isolation.
  Provision a new purpose; never convert a Customer, CSKH or Admin account.
- Reuse PilotCoverage's persisted four-route dataset and Pricing/Booking snapshots.
  Its current Customer-context-only reader needs one approved Ops-context overload;
  never construct a Customer context for Staff.
- Reuse StationIntake's scan/set/version/idempotency core, Orders PilotHandling,
  Finance unpaid binding and existing custody/tracking transaction. No duplicate
  acceptance, tracking, ledger, barcode or Quote engine.
- Do not change V1–V43 files. Candidate V44/V45 are free at this preflight only;
  recheck the stream before implementation. No SQL/migration is created here.

## 3. Identity boundary [PROPOSED: NWS-ID-01 / NWS-ARCH-01]

New OPERATIONS purpose on `identity.local_staff_principal`; widen VARCHAR(7) to
VARCHAR(10), CHECK exactly SUPPORT/OPERATIONS. Purpose immutable, account purpose
exclusive, no implicit grant. Existing verified ACTIVE and environment binding
requirements remain. Invitation purpose stays STAFF_INV, with the authoritative
principal purpose deciding the acceptance adapter/template; no caller override.

Add OPS audience (VARCHAR(6) unchanged) to `auth_session` and `csrf_binding` CHECKs.
Refresh/browser/session storage filters must bind OPS, never STAFF/SYSTEM. New
cookies: `__Host-argopost.ops.refresh` and `__Host-argopost.ops.csrf`; Secure,
HttpOnly where appropriate, SameSite, Path=/ and existing CSRF/origin safeguards.
Reuse the existing key ring; no new secrets/configuration store or global changes.
The refresh cookie must not work at staff/customer/system endpoints and vice versa.

Exact new public Identity interface:

```java
interface OperationalStaffAdmission {
  record Subject(UUID accountId, String displayName, long securityVersion) {}
  record Context(UUID requestId, UUID sessionId, Subject actor) {}
  <T> T execute(String encodedAccess, UUID requestId, Function<Context,T> work);
  void requireCurrent(Context context);
  void meaningfulAction(Context context);
}
```

Owner validation must reject constructed/copied/forwarded/escaped contexts,
different physical DataSource resources, suspended/replaced transactions and stale
authorization. Recheck genuine issued object, callback lifetime, account state,
purpose, auth version, audience/session expiry/revocation before meaningful work
and at commit. No cross-realm grant union. Provisioning issues no Dispatch grants.

Dispatch currently may import Orders API only. NWS-ARCH-01 explicitly proposes
one narrow collaboration exception: Dispatch and nested types may use only
`identity.api.staff.OperationalStaffAdmission` for owner `requireCurrent` checks.
Do not permit general Identity dependencies, provisioning access or private imports;
retain architecture negative fixtures for every other Identity type. This exception
requires approval; do not silently expand the collaborator allowlist.

## 4. Network/assignment authority [PROPOSED: NWS-NET-01]

Dispatch owns immutable site identity and versioned configuration: code, name,
STATION/HUB type, location text, area code, optional latitude/longitude (no map
provider), IANA timezone, operating-hours declaration, state ACTIVE/INACTIVE,
capabilities, effectiveFrom/effectiveTo. No deletion/reuse of site codes.

Capabilities initially allow SITE_READ and STATION_INTAKE only. HUB may be listed
but cannot enable STATION_INTAKE. Future sorting/handover/dispatch capabilities
are not permission codes/grants in this package. Topology is directed site links
and service/coverage/drop-off bindings; it does not mean a transport route is live.
Require distinct same-environment endpoints, current versions, effective windows
and existing dataset/service references. A link alone cannot create Pickup/ETA.
The exact private Merchant-owned boolean predicate NWS-SQL-11 validates dataset/
service/area/drop-off references; Dispatch does not SELECT Merchant tables. Site
and logical-link versions have increasing effectiveFrom, no backdating; the newest
effective revision governs, with end=min(declared end,next revision start). Old
open-ended snapshots are never updated merely to add a revision.

Dedicated Staff site assignments have immutable ID/account/site/permission,
ACTIVE/REVOKED state, effective interval, version and immutable audit/receipt.
Permission is SITE_READ or STATION_INTAKE; numeric role IDs are untouched.
STATION_INTAKE requires a valid current site read/handling capability and matching
booked origin binding. No wildcard/global/region inheritance. Runtime cannot write
registry, assignments or operator receipts. Revocation is terminal for that grant;
a later explicit new grant has a different ID, and cannot reuse an old receipt.

All commands take current Identity admission locks, then Dispatch authority/site
locks, then Shipment/intake locks in a fixed order. Site state/capability, binding
and assignment changes use the same Dispatch locks even through operator paths.
Define revocation linearization: a command serialized before revoke can commit;
a command obtaining authority after revoke must deny, including receipt replay.
Locking must protect decisions through commit; no client lock/version is authority.

## 5. Integration and compatibility [PROPOSED: NWS-INTAKE-01]

New Staff intake HTTP adapter uses genuine Ops context and Dispatch's new
OperationalStationIntake port; it delegates to the existing intake implementation.
Workflow composes Dispatch/Orders/Merchant/Finance through public APIs in one
physical transaction. Public port methods/DTO reuse are in the manifest.

To preserve immutable old evidence, V45 adds `operational_assignment_id` and
`site_version_id` to existing Dispatch scan/acceptance evidence. Historical
`assignment_id` becomes nullable only when the new operational FK and site-version
FK are present; exactly one legacy/operational authority branch is required. All
legacy rows retain original values/FKs, no backfill/rewrite. New operational FKs
bind assignment ID + account + site, site code + version, environment and time;
replay cannot substitute a different actor, site or authorization branch.

Acceptance for this package remains PERSONAL Local/Test on four existing routes,
matching SIM-DROP origin, all Parcels together, MATCHED inspection and UNPAID.
Network registry does not widen the existing BR13 payment/acceptance exception.
Business intake, arbitrary new coverage, Pickup, partial intake, collection,
In Transit and Delivered stay blocked. Do not change booked Shipment/Parcel IDs,
snapshot, route or fee. Scan is not custody. Printing is not Acceptance.

Orders/Finance read/command ports stay unchanged. Keep current deferred completeness
and FK/unique invariants: one acceptance, each required Parcel custody exactly once,
one Shipment event, every Parcel event, audit and receipt, or rollback everything.
Custody still has one source; network configuration is never a custody fact.

## 6. HTTP/UI contract [PROPOSED: NWS-HTTP-01]

Exact endpoints and reused response schemas are in the manifest. Two new Admin
directory GETs use existing ADMIN_DASHBOARD_READ + ADMIN_NETWORK_READ, not Staff
credential/permission data. ADM-12 write/Staff-directory editor is out of scope;
trusted offline provisioning/assignment is the bounded first-use channel.
No new Admin permission is proposed. Admin READ never implies Ops execution.

Ops login/activation and `/ops/station` use the existing Internal Shell and visual
patterns with audience-isolated state. Actual assigned-site selector, scan/set
inspection, pending/unknown outcome receipt recovery, current version, empty/error/
denied states; purge loaded cargo/private PDF when site/account/assignment changes.
Do not send PII to analytics/logs or expose Staff email in network GETs.
Legacy `/operations/station` and `/api/v1/station-operations/...` retain exact V21
behavior; Customer does not gain `/ops` access. No frontend authority switch.

Auth JSON reuses existing Staff-auth exact DTO shapes, errors and proof acceptance
semantics, at an OPS-specific prefix. New reads are private/no-store; mutations
require existing CSRF + exact origin + Idempotency-Key, acceptance If-Match.
Errors: 400 invalid input; 401 invalid/wrong-audience/expired Identity; 403 current
permission/site denial; 404 absent or foreign cargo; 409 key/state/policy conflict;
412 stale version; 428 missing version; 503 unavailable dependency. Preserve V21
legacy status semantics (including its existing 409 stale result) unchanged.

## 7. Operator, schema and security gates

The manifest enumerates proposed new SQL signatures, rows, constraints and ACL.
Offline operator has EXECUTE on exact provisioning/disposition and Dispatch
configuration functions only; no table DML/SELECT, writer SET/INHERIT, SUPERUSER,
CREATEROLE, CREATEDB or BYPASSRLS. Provisioner seals verifier/encrypted outbox using
existing Identity protection before commit. Password/proof never in command logs.
Runtime receives no operator role/EXECUTE. NOLOGIN writer roles are bootstrapped
by a separate approved isolated-test prerequisite, not Flyway/runtime CREATEROLE.
SECURITY DEFINER functions pin safe search_path and revalidate session_user,
physical environment and authorized operator membership; caller-set flags alone
are not sufficient authority. No new function with default PUBLIC EXECUTE.
The manifest also isolates the Merchant coverage predicate in a NOLOGIN read-only
owner and the Identity subject-lock helper in the existing private writer. These
are explicit proposed SQL/ACL changes, not permission implied by a Java port.

Request-bound durable receipt must bind operator identity, physical DB identity,
namespace, operation, request ID, full canonical input digest and expected version.
Same input returns original outcome under current authority; changed payload rejects;
revoke/expiry cannot be undone by replay. Receipt/audit/registry or assignment write
commit together. SQL operator digests use canonical SHA-256; proof acceptance keeps
the existing protected Identity fingerprint. No proof/credential in generic receipt.

## 8. Minimal verification after technical approval

1. Migration/FK/CHECK/ACL/direct-DML negatives, unchanged old rows/checksums.
2. Dedicated Ops invitation/real engine verification using isolated test SMTP;
   no Customer/CSKH/Admin conversion, no default assignment, wrong-audience denial.
3. Current context object/resource/lifetime/suspension/expiry/revocation negatives.
4. Exact site capability/binding/effective dates, wrong-site/no-permission denial;
   concurrent revoke/site-disable vs intake; same-key replay/changed payload denial.
5. One focused HTTPS + PostgreSQL18 scenario: Personal Quote/Booking with two
   Parcels -> Ops login/assigned origin -> scan both -> confirm -> custody/tracking/
   audit/receipt all match; obligation remains UNPAID; Customer refresh sees result.
   Deny unassigned Staff and revoked assignment, and preserve Personal/CSKH sessions.
6. Reuse existing acceptance fault-injection/race tests; add assertions only for
   the new operational branch/FKs. Do not run the whole Backend suite by default.

No gate above is claimed run for proposed code. Source/test authorization must
precede implementation, not inferred from this design or earlier pilot approval.

## 9. Next dependency after foundation acceptance

Spec012: Parcel/site inventory + sort destination -> immutable manifest membership,
with scoped creator, seal/reopen/cancel policy, exact Parcel set/version, custody
holder, independent send/receive confirmation and discrepancy handling. Spec013
then owns transfer/trip/receiver confirmation, followed by spec014 dispatch. A
topology link, packing list or sender declaration cannot itself move custody or
set In Transit. These commands need a separate exact domain contract; do not add
them as unused permissions to the foundation.
