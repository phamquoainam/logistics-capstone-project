# P0-NOTICE-DELIVERY-01 — purpose-bound public bridge to the existing SMTP engine

Status: **OWNER APPROVED — isolated SOURCE/TEST only; implementation IN_PROGRESS**.
Owner approved P0-NOTICE-DELIVERY-01 on 2026-11-24. No real mail, runtime enablement or deployment.
Parent manifest SHA-256 remains
`90c795bd7e773b6a58d0d220056cfbc5e9a5ee95b38ae2493d5b9f75a372291f`.
P0-NOTICE-BIND-01 is already approved and is not reopened.

## Observed dependency, not a new provider decision

P0-PORT-07 authorizes dispatch through the existing configured transport. Source
has only `identity.internal.application.port.CustomerDeliveryTransport`, implemented
by `identity.internal.infrastructure.SmtpCustomerDelivery`. Its Message requires a
real Identity possession proof/token; Staff/Admin methods likewise require their
specific invitations. Its shared `sendPrepared` already enforces JavaMailSender,
TLS/STARTTLS, timeout, authentication, diagnostic redaction and bounded outcomes.

Notifications cannot import that private port/class, turn a business decision into
an Identity proof, use reflection, or copy the SMTP implementation. No such
workaround is implemented. A public transport bridge is absent from the exact
manifest; dispatch remains fail-closed pending this bounded interface approval.
V42 and V43 registry/activation/outbox work independent of dispatch may continue.

## Exact public port [PROPOSED]

```java
// platform.api.MerchantNoticeMail
Outcome send(Message message);
enum Template { REQUEST_INFORMATION, REJECTED, APPROVED, WITHDRAWN }
enum Kind { ACCEPTED_BY_PROVIDER, TRANSIENT_FAILURE, PERMANENT_FAILURE, BLOCKED }
record Message(UUID id, UUID applicationId, String destination, Template template) {}
record Outcome(Kind kind, String reference, boolean sandbox) {}
```

- Transport-only Java types; no HTTP route, database table/column, permission,
  SQL privilege, role, account, proof, grant or provider addition.
- Message UUIDs canonical/nonzero; destination uses the existing Identity email
  validation; diagnostics/toString redact recipient/body. No caller-provided HTML,
  subject, free text, proof URL, arbitrary link, credentials or attachment.
- Outcome reference exists only for ACCEPTED_BY_PROVIDER and retains the current
  `TEST:<uuid>` / `SMTP:<uuid>` form. Acceptance does not mean mailbox delivery.
- The fixed Vietnamese status templates link only to configured HTTPS Customer
  origin `/personal/applications/{applicationId}`. No legal fields or proofs in mail.
- Worker destination comes only from the protected, event-bound outbox and is
  revalidated by the approved P0-PORT-07 current-recipient protocol at enqueue.
  A constructed Message never creates business authority or validates an event.

## Exact reuse / compatibility [PROPOSED]

`SmtpCustomerDelivery` additionally implements the public Platform port. Its new
purpose-bound overload delegates to the existing private `sendPrepared`, with
unchanged Settings, provider, TLS, credential loading, test-domain restrictions,
rate/timeout/error policy and redaction. Existing Customer/Staff/Admin proof methods
and their signatures/semantics remain unchanged. No second SMTP sender/policy.

Identity composition exposes the same configured transport instance as the narrow
Platform interface; Notifications depends only on Platform API. No Identity import
of Merchant/Notifications, and no Notifications import of Identity internals.
Default notice enablement remains false; this source/test approval would not
enable a worker, load real local secrets, send real mail or change runtime config.

## Required tests after approval

Controlled loopback SMTP real send/receive and exact fixed template/link/Message-ID;
no proof token; malformed IDs/recipient/template rejection; test-domain/network
restrictions; TLS/authentication/timeout/failure outcome regressions; log/toString
privacy; unchanged Customer enrollment/recovery and Staff/Admin invitation behavior;
architecture boundaries; queue provider acceptance distinct from delivery. No
Internet SMTP, production mutation or real Customer/Staff/Admin UAT.
