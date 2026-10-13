# Demo 001 — Auth → Merchant → Shipment DRAFT

Scope authorized by the user's urgent demo request on 2026-10-13. This local-only slice is not completion of features 001/002/003/005. Their original requirements and AC remain unchanged.

## Stories and trace
| ID | Story / requirement | Trace |
|---|---|---|
| DEMO-01 | Enabled account logs in using a hashed password; server issues a signed 15-minute access token. Every protected request reloads account status, role and merchant scope from the database. | REQ-002-01/02/03/05; FR-A01/A02 |
| DEMO-02 | MERCHANT sees its own organization and creates/lists/reads/cancels its DRAFT shipments. Ownership comes from authentication, never request data. | REQ-003-01 (profile subset); REQ-005-01/03/04/05; FR-M01/M04/M05 |
| DEMO-03 | ADMIN reads account/role overview and shipment summaries; operational roles receive no merchant/admin business permissions. | REQ-002-04; FR-AD01 |
| DEMO-04 | Web portal uses real APIs, validation, loading/error/success states and retains failed input. | FR-M04/M05; NFR-U02 |
| DEMO-05 | Isolated local database persists committed records across backend restart. | REQ-001-02 (demo persistence only) |

## Role matrix
| API | Public | MERCHANT | ADMIN | DISPATCHER / WAREHOUSE_OPERATOR / SHIPPER / FINANCE |
|---|---|---|---|---|
| Login | yes | yes | yes | yes |
| Current user | no | yes | yes | yes |
| Current merchant; own shipments | no | yes | no | no |
| Admin account/role and shipment summaries | no | no | yes | no |
| Finance / confirmation / delivery APIs | no | no | no | no |

No registration or role editing endpoint. One role and at most one merchant organization per demo account; multi-role/multi-scope P0 is deferred.

## Invariants and edge cases
- BR-01: only DRAFT creation and DRAFT → CANCELLED; no generic state update, confirmation, shipping or delivery.
- BR-02: creation and cancellation require Idempotency-Key, scoped by merchant and operation. Same key+same payload replays one result; changed payload is 409. Database uniqueness and transaction protect concurrent effects.
- Reference is unique per merchant. Cross-owner reads/cancels return the same 404 as an absent ID.
- Creation, immutable event and operation receipt commit atomically. Cancellation retains records and source account; no financial/inventory side effects exist.
- Address strings are unverified draft input, not a validated administrative catalog or confirmed shipping snapshot (BR-19 follow-up).
- Reject unknown request fields (including ownerId/status/role), blank/oversized fields, invalid weight and malformed IDs/keys.
- Disabled account, invalid/expired/tampered token: 401. Wrong role: 403. Basic process-local login throttling: 429.

## Acceptance / tests
1. Valid and invalid login, hashed storage, disabled account, anonymous 401 and wrong-role 403 have backend integration tests.
2. A creates/reads a DRAFT; B cannot read/cancel it; owner injection and invalid input fail without rows.
3. Creation/cancellation replay and payload conflict are tested, including concurrent creation.
4. Cancellation writes one immutable event; non-DRAFT cancellation and unsupported status changes fail.
5. File-database API smoke creates a record, restarts backend and reads the same record.
6. Web typecheck/build and browser walkthrough: login, create/list/detail/cancel and admin overview use backend responses.

## Intentionally deferred
Refresh tokens, server-side logout/revocation, password reset, registration, role administration/audit, multi-role/scopes, production throttling, catalog/geocoding/coordinates, pickup CRUD, quote, CONFIRMED, Finance/WMS/COD, public tracking, full P0 audit/metrics/CI, native mobile features. Browser logout clears local access token; an issued token remains valid until expiry unless account is disabled.
H2 file fallback is explicitly authorized for tonight; it is not PostgreSQL/Flyway foundation verification and does not change the PostgreSQL architecture.
