# ADR-0003: API, Idempotency and Events

- **Status:** PROPOSED — requires team approval before public API implementation.
- **Context:** Report requires request replay safety, transaction/Outbox/Inbox, role/object authz, but does not prescribe REST paths/headers/payload.
- **Proposal:** `/api/v1`, JSON envelope, `Idempotency-Key`, versioned OpenAPI/event schema in `contracts`, at-least-once message transport with durable outbox/inbox.
- **Tradeoff:** More persistence/operational work, clear repeatability and typed client generation.
- **Open points:** key retention, response-cache strategy, event schema versions, error taxonomy, batch semantics.
- **Validation:** identical replay one effect; different payload conflict; broker restart recovery.
