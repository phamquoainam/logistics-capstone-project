# ADR-0016 — Bounded Carrier Admin read admission

Status: ACCEPTED for Owner-approved ADM-02–08 source/test scope, 2026-11-21.
Authority: [Owner decision](../../implementation/argopost-admin-dashboard-owner-approval.md),
[immutable contract](../../implementation/admin-dashboard-contract.proposed.md), SHA-256
`69d1ac81606199a2db644d8038553e19fd78bfddb2608e92713b2fa1396e2289`.
Implementation verification is tracked separately; acceptance of this decision is
not a claim that runtime tests or deployment have passed.

Reuse SYSTEM credentials, verification, session/token/CSRF and registered realm.
Dedicated Carrier read principals must be ACTIVE/verified, have no Customer
identifier or SUPPORT Staff registration, and hold only a nonempty subset of the
six approved read codes in one ACTIVE SYSTEM realm, including
`ADMIN_DASHBOARD_READ`. Initial Root and mixed Identity-management sets cannot be
Carrier read principals. Identity management admission and assignability stay
unchanged; no read grant authorizes account administration or grant issuance.

Default off. Enabled composition additionally requires LOCAL/TEST configuration
and the existing physically bound database/environment predicate. Reusing that
boolean predicate does not confer SUPPORT authority. Production and restored or
mismatched database admission deny. No schema, SQL privilege or operator change.
Real onboarding, grants, realm activation and deployment remain separate decisions.

Domain adapters accept owner-local typed capability views delegated by Workflow
to the callback-scoped Identity `AdminReadContext`. This keeps the existing module
collaborator graph and architecture tests intact; no cross-module internal imports
or new shared grant engine. Check granular permission before invoking a source.

Identity admission acquires the existing population lock on a dedicated bounded
connection **before** starting the callback's single read-only repeatable-read
transaction. A busy lock denies availability rather than reading a snapshot taken
before a waited-for revocation. The regular DataSource supplies the transaction
connection for all owner queries; the context cannot escape the callback or thread.
The enabled test/runtime pool therefore needs at least two available connections;
failure to acquire them fails closed. No read heartbeat extends session activity.

Snapshot selectors are stateless, HMAC-authenticated and domain-separated from the
existing protected limiter secret; no new default secret/provider or cache table.
Bind actor, realm, current grant fingerprint, normalized cohort, cutoff, source
fingerprint and 15-minute validity. Current authority is always rechecked. Use
immutable Booking/tracking event timestamps to reproduce supported states at the
cutoff; late committed facts that change the justified cohort yield 409. A chart
series action uses the same KPI cohort; it does not silently refresh into a new
date subset or create an unapproved query parameter.

Only confirmed Shipment/Parcel and exact UNPAID obligation facts are available.
Drafts, SIM network points, absent delivery/SLA/financial settlement sources and
unsourced telemetry must not be represented as approved applications, official
network, successful deliveries, revenue or zero-valued implemented capabilities.
