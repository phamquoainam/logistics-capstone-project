# P0-NOTICE-COMPOSE-01 - owner-bound command event handoff

Status: **OWNER APPROVED - isolated SOURCE/TEST only; implementation IN_PROGRESS**.
Owner confirmed P0-NOTICE-COMPOSE-01 on2026-11-24; no deployment or real mail.
Parent manifest remains90c795bd7e773b6a58d0d220056cfbc5e9a5ee95b38ae2493d5b9f75a372291f.
P0-NOTICE-BIND-01 and P0-NOTICE-DELIVERY-01 remain approved unchanged.

## Exact architectural gap

P0-PORT-07 requires a Merchant public event port but does not list its exact Java
signature. MerchantGovernance responses expose event UUID only for APPROVE activation;
REQUEST_INFORMATION/REJECT/WITHDRAW receipts do not. Notifications cannot select
Merchant tables; Merchant cannot import Notifications API under ADR domain dependencies.
Direct domain-to-Notifications coupling was detected during source integration and
must be removed before acceptance. No architecture rule is relaxed.

## Exact additions [PROPOSED]

One Merchant-owned public reader, no HTTP route:

```java
// merchant.api.MerchantApplicationNoticeEvents
Optional<Event> commandEvent(AdminCommandContext context, UUID applicationId,
                             String operation, UUID operationKey);
Optional<Event> commandEvent(CustomerAdmission.Context context, UUID applicationId,
                             String operation, UUID operationKey);
record Event(UUID eventId, UUID applicationId, UUID applicantId, String template) {}
```

- The existing owning Merchant implementation reads only its own event/receipt/app.
- Genuine current Identity context verification before any actor/scope access,
  same physical DataSource callback transaction. Default implementations deny.
- Customer: only SELF WITHDRAW; Admin: own actor/realm receipt with current required
  domain capability for REQUEST_INFORMATION, REJECT or APPROVE, current eligible
  applicant and exact scope. No constructed/captured context or arbitrary event ID.
- Exact receipt operation/key/actor/target/result and event/app/applicant/template
  binding. Only an event/receipt inserted in the current transaction is returned.
  Authorized historical committed replay returns Optional.empty; it never creates a
  second outbox intent or authorizes new business activity.
- The workflow module alone composes existing approved Merchant commands, this read
  port and Notifications.enqueue inside the same Identity transaction. It translates
  the four metadata fields into the existing Notifications ApplicationEvent and uses
  the exact locked Identity Subject. Failed enqueue rolls back the business command.
- SMTP remains outside transaction through the existing approved Platform port.
- No new DTO field in existing responses, database table/column, SQL signature,
  permission, grant, feature flag, authority or provider. No private imports.

## Mandatory tests

Architecture dependency unchanged; four correct event mappings; own exact fresh
receipt only; constructed/escaped context; foreign actor/realm/application/key;
current admission and scope revocation; historical replay produces no new notice;
rollback leaves no activation/event/receipt/outbox; direct DML denied by approved
boolean guard; concurrent exact-key command creates one notice only.

No deployment, real email, real grants, shared DB or Git publication.
