# ADR-0003: API, Idempotency and Events

- **Status:** ACCEPTED_FOR_DEC_29_SCOPE (2026-10-15, explicit user approval) for the bounded Demo-001 contract/client decision below. The broader `/api/v1`, envelope and event proposal remains PROPOSED.
- **Context:** Report requires request replay safety, transaction/Outbox/Inbox, role/object authz, but does not prescribe REST paths/headers/payload.
- **Proposal:** `/api/v1`, JSON envelope, `Idempotency-Key`, versioned OpenAPI/event schema in `contracts`, at-least-once message transport with durable outbox/inbox.
- **Tradeoff:** More persistence/operational work, clear repeatability and typed client generation.
- **Open points:** key retention, response-cache strategy, event schema versions, error taxonomy, batch semantics.
- **Validation:** identical replay one effect; different payload conflict; broker restart recovery.

## D1-06 bounded contract adoption — ACCEPTED, 2026-10-15

The user-approved Demo-001 is an existing scoped exception: /api/demo/v1,
resource/array success JSON and flat {code,message} errors, current bearer/RBAC
and persistent idempotency. It does not accept the broad /api/v1/envelope/event
proposal above. D1-06 selects codifying these existing eleven operations and
generating a Fetch SDK for the existing web journey; no new endpoint, body field,
status, transaction or event policy. DEC-29 scope is accepted by the user's
explicit “DEC-29 — APPROVED | D1-06 Definition of Ready Closure” message,
not inferred from existing source or registry metadata.

[Decision, alternatives and readiness](../../implementation/d1-06-slice-readiness.md),
[accepted bounded design](../../../specs/001-platform-foundation/slices/demo-contract-client/design.md).
Slice status is READY_FOR_IMPLEMENTATION; D1-06 documentation/DoR is DONE.
The broader P0 API/event proposal above remains PROPOSED.

Accepted pins: @hey-api/openapi-ts0.99.0, @redocly/cli2.64.0, TypeScript6.0.3,
Ajv8.20.0 and ajv-formats3.0.1. Generator uses bundled Fetch, no deprecated
standalone client package. Existing web/mobile compiler declarations stay intact.
OpenAPI3.1 at contracts/openapi/demo-001.yaml is managed in repository and checked
against the backend; packages/api-client is private. Web adopts the SDK without
behavior change; mobile only compiles/imports it. Ajv/formats are contract-test
harness dependencies, not unapproved frontend runtime dependencies.

Source/schema/wire drift, deterministic regeneration, fixtures and application-only
rollback are part of the accepted design. Metadata existence/constraints are
verified; actual install/build/generation/wire/regression compatibility is still
mandatory D1-07 evidence. Preserve JSON/status/errors/JWT/RBAC/merchant ownership/
idempotency and V1/V2; add no new API/envelope/business semantics. D1-07 remains
NOT STARTED; this approval turn only changes documentation, with no publish.
