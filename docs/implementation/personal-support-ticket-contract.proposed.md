# Personal Support Ticket V1 — exact bounded candidate

Status: OWNER ACCEPTED,2026-11-10; implementation/gates pending.
Owner explicitly answered: “Duyệt Ticket V1 Local/Test”. Scope and exclusions below
are binding. Approval is not implementation acceptance or permission to publish/merge.
Owner: Orders (canonical020/BC153); workflow composes existing Identity current
admission and Orders public APIs. No new domain/microservice. Separate from
Shipment change requests and Claims021/Finance compensation. Approval of this
candidate does NOT authorize those absent workflows.

## Bounded outcome and policy candidate

Only verified current PERSONAL Customer in explicitly enabled LOCAL/TEST may
create/read/reply to own Ticket. Runtime default DISABLED; Production/Merchant/
Guest denied. Ticket may be GENERAL or SHIPMENT; Shipment binding is immutable
and must be an existing self-owned confirmed Shipment before any side effect.
Categories: ACCOUNT_HELP, SHIPMENT_TRACE, FEE_REVIEW, OTHER. No carrier lifecycle
or payment status is changed by opening a Ticket. No promise of commercial SLA.

States: NEW, IN_PROGRESS, NEEDS_INFORMATION, RESOLVED, CLOSED. Orders stores
append-only public conversation/events plus separate private audit. No customer
editable state, handler, priority, deadline, decision or compensation amount.
Priority remains NORMAL unless a later separately reviewed policy exists;
deadline is null (unpublished SLA), never calculated from a made-up duration.
No file upload/storage reuse or outbound Ticket email/SMS/Push in this first slice.
Evidence messages must not contain passwords, tokens, full bank/card/ID documents.

## Exact proposed HTTP under existing TLS/Bearer/Origin/CSRF admission

All writes: UUID Idempotency-Key, strict duplicate/unknown field rejection,
positive If-Match version except initial creation; same binding replays only after
current authorization. Lost writes resolve through scoped receipt, not blind POST.

| Operation | Route | Input / output |
|---|---|---|
| listOwnSupportTickets | GET `/api/v1/customer/support-tickets` | page>=0, size20, optional state/category/shipmentId; owner/filter before paging; updatedAt DESC/id ASC |
| createOwnSupportTicket | POST same collection | {kind:GENERAL/SHIPMENT,shipmentId?:UUID,category,subject:1..160,message:1..4000};201 + Location/ETag |
| getOwnSupportTicket | GET same collection plus `/{id}` | own Ticket/detail/public messages/current allowed actions;404 for foreign/missing |
| replyToOwnSupportTicket | POST same resource plus `/replies` | {message:1..4000}; NEW/IN_PROGRESS stay, NEEDS_INFORMATION -> IN_PROGRESS; otherwise409 |
| requestOwnSupportReview | POST same resource plus `/review-requests` | {message:1..4000}; RESOLVED only, one pending review tied to that resolution version; no automatic reopening |
| acknowledgeOwnSupportResolution | POST same resource plus `/acknowledgements` | empty body; RESOLVED -> CLOSED, ticket-resolution acknowledgement only |
| ownSupportReceipt | GET `/api/v1/customer/support-receipts/{operation}/{key}` | {operationId,targetId,outcome:COMMITTED,version};no messages/PII |
| listSupportTriage | GET `/api/v1/support-operations/queue` | current SUPPORT_TRIAGE grant; PERSONAL NEW metadata only, fixed paging20 |
| assignSupportCase | POST `/api/v1/support-operations/cases/{id}/assignments` | {assigneeAccountId}; current triage authority, assignee has active SUPPORT_HANDLE grant; no permission grants |
| getAssignedSupportCase | GET `/api/v1/support-operations/cases/{id}` | current case assignment AND active SUPPORT_HANDLE; public conversation only |
| transitionSupportCase | POST same resource plus `/transitions` | {action:START/REQUEST_INFORMATION/RESOLVE/REOPEN,message:1..4000}; current case authority/version required |

Case transitions: START NEW -> IN_PROGRESS; REQUEST_INFORMATION IN_PROGRESS ->
NEEDS_INFORMATION; RESOLVE IN_PROGRESS -> RESOLVED; REOPEN RESOLVED -> IN_PROGRESS
only against a pending Customer review of the current resolution. No generic
arbitrary state PATCH, auto-close deadline or Customer staff impersonation.
Public handler is a configured team label, not staff contact/account/permission.
No internal notes/audit/assignment identity is exposed to Customer.

Ticket summary exact fields: id,code,kind,shipmentId(null|UUID),category,subject,
state,version,createdAt,updatedAt,priority:NORMAL,responseDueAt:null,teamLabel:null|string.
List: {items,page,size:20,hasNext}. Detail adds publicMessages and allowedActions.
Public message: id,authorKind:CUSTOMER/SUPPORT,message,createdAt,eventType.
allowedActions is derived by server from current state/version/authority only.
No hidden contacts, auth metadata or compensation/payment/Carrier decision DTO.
Response denials:400 validation,401 invalid/revoked session,403 operation scope,
404 foreign/missing,409 stale/state/key conflict,428 missing version,429 throttled,
503 disabled/unavailable. Private responses no-store; errors contain code/message/
requestId only. No PII/body in logs, URLs, receipts or CI artifacts.

## Authority, data and consistency candidate

New forward V22 ONLY if still the next migration when implemented: Orders-owned
personal_support_ticket, personal_support_message, personal_support_assignment,
support_staff_grant, support_command_receipt, support_business_audit,
support_request_window. No deployed migration edit, destructive cleanup or initial
Staff/customer seed. Only owner-controlled audited local procedure/script grants
SUPPORT_TRIAGE or SUPPORT_HANDLE to a specified existing Identity account; runtime
cannot self-grant. Eligibility is rechecked by current Identity admission at use.

Ticket columns correspond exactly to summary plus owner_id, original public
message relation and immutable Shipment binding. Messages/audit/history append
only; receipt unique(actor,operation,key) binds original target/version/canonical
payload. Case assignment/grants are versioned and revocable. One active case
assignment at a time. DB checks constrain kind/shipment binding/states/versions;
FKs bind message/receipt/assignment to the same Ticket/owner scope. No cross-module
internal imports, mock owner assertions or customer-body amounts/actor claims.

Current Identity admission -> Support authority/assignment locks -> Ticket lock ->
Orders Shipment validation; source/history/audit/receipt/counters in one local
transaction. Revocation-first denies, prior committed valid action remains. Writes
serialize same Ticket; same-key race one effect, stale update conflicts. Proposed
throttle:5 new Tickets/account per15min and20 customer replies/reviews per15min,
using transactional DB windows, not frontend/Redis-only enforcement. Idempotent
replays do not consume another quota or extend idle. No auto-purge/retention claim.

## Required gates before enabling

PG18 upgrade/preserved data, runtime privileges and rollback; A/B ownership,
foreign Shipment, query/body overposting, same-key/concurrent/stale/revoked Staff,
assignment isolation, invalid transitions, public/private message separation,
rate-limit concurrency, audit/receipt failure and unknown-write receipt recovery.
Real Customer -> assigned Support -> information request -> Customer reply ->
resolution/review/acknowledgement browser journey with controlled test actors.
No fake Claim award, refund, cancellation, uploaded evidence or delivery event.
