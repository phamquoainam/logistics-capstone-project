# Organization Support V1 — exact amendment [PROPOSED]

**Current authority:** Owner subsequently approved Technical Core V1 against
manifest SHA-256 `9484c415356e601ee8192e84a09af2e39f2f448d07244457de1ef79a09c3a940`.
The historical proposal text below is retained for provenance; its pending
technical-approval boundary is superseded. V35 replaces the unavailable V33
placeholder because V34 Team is already applied. Source/test execution and the
separately approved compatibility refinements are recorded in
[organization-support-execution.md](organization-support-execution.md).
Localhost migration/grants and Production remain unauthorized.

Owner directive2026-11-16: Business Support & Claims. Exact technical contract
approval is pending under root AGENTS.md; the business purpose is authorized.
Owner GO WITH CONDITIONS2026-11-17 explicitly approved V1 BUSINESS scope and
required an exact technical manifest; this does not approve the proposed wire,
DTO/schema/permission details. [Machine-readable manifest](organization-support-technical-manifest.json)
and [review/checkpoint](organization-support-technical-review.md) record that split.
Canonical020/BC-016/030; Claims021 remains separate. Existing branch/worktree and
all19 preceding source paths are copied with SHA-256 under ignored
`.demo/organization-support/`; no discard, new worktree or publication.

## Tasks / delivery gate

- [x] OSC-01 Preserve current source; inventory Personal Ticket, Organization
  admission, OSM/WFP and existing internal work/notifications.
- [x] OSC-02 Existing-contract Business support hub, actual scoped Shipment
  context, searchable guidance and honest separate workflow availability.
- [x] OSC-03a Owner accepts the bounded V1 business scope, with mandatory guards.
- [ ] OSC-03b Explicit confirmation of exact technical manifest items remains pending.
- [ ] OSC-04 Organization Ticket + conversation + separate CSKH case authority;
  ETag/idempotency/atomic evidence, PostgreSQL test persistence and UI.
- [ ] OSC-05 Merchant internal assignment, filters, actual response-time reports
  and opt-in in-app notices; current-scope/revocation/concurrency tests.
- [ ] OSC-06 Personal regression, migration upgrade, architecture/security,
  Frontend, HTTPS/PG18 Browser and source/report/privacy gates; local checkpoint.
- [ ] OSC-07 Separate explicit localhost migration/grant disposition, if required
  after source/test gates. Never infer new real-account grants from OSC-03a/03b.

V1 is verified Customer SANDBOX + explicitly enabled LOCAL/TEST only, disabled by
default; prod/production denied. Preserve V1–V32, Personal Ticket API/data and all
existing Shipments/Quotes/obligations. Forward migration V33 only after verifying
the actual stream. First implement and migrate disposable test databases only.

## What V1 actually does / does not imply

Orders remains the owner of Support. Reuse the approved Personal lifecycle,
validation, receipt/audit patterns and current Identity/transaction boundaries;
extract shared pure policy helpers rather than relaxing Personal ownership or
copying its authorization assumptions. Organization data has its own owner/FKs;
an Organization UUID is never stored as a Personal owner/account ID.

Ticket types GENERAL/SHIPMENT. SHIPMENT binds one existing confirmed BUSINESS
Shipment of this Organization and its immutable facility; it cannot bind a
Personal Shipment or another warehouse to bypass scope. GENERAL optionally binds
an active same-Organization facility; null facility requires organization-wide
authority. Changing Shipment/facility/kind after creation is forbidden.

Categories ACCOUNT_HELP, SHIPMENT_TRACE, DAMAGE_REPORT, LOSS_REPORT, FEE_REVIEW,
OTHER. Damage/loss are Merchant reports requesting investigation, not established
Carrier events or approved Claims. COD is unavailable while Business COD policy
is disabled. Ticket messages do not create a cancellation, address edit, delivery
retry, Pickup, return, ledger correction, Claim award or payout.

States NEW, IN_PROGRESS, NEEDS_INFORMATION, RESOLVED, CLOSED, using the accepted
Personal transition rules. Merchant can reply in NEW/IN_PROGRESS/NEEDS_INFORMATION
(last -> IN_PROGRESS), request one review for the current RESOLVED version or
acknowledge that resolution (-> CLOSED). It cannot set state, CSKH assignment,
handler, Carrier priority, SLA deadline or compensation. requestedPriority is
LOW/NORMAL/HIGH and explicitly labelled Merchant-requested priority; actual
priority remains NORMAL and responseDueAt null until an accepted CSKH SLA exists.

Customer-facing handler is only team label `ArgoPost Support`, not a Staff email,
account identifier or permission. Public conversation and Merchant internal
assignment history are distinct; no Staff internal notes/audit leakage.

## Current authority [PROPOSED]

Explicit Merchant permissions: `support.read`, `support.write`, `support.assign`,
`support.reports.read`, `support.notifications.manage`. ORGANIZATION or exact
same-Organization FACILITY scopes; write/assign/reports require matching read.
Notifications preference is self-owned; its permission never allows setting
another member's preferences. No role-name grants, backfill or automatic grants.
Extend only the exact admitted permission/FACILITY lists and context projection.

Every list/count/detail/message/receipt/report/notice rechecks current verified
Identity + ACTIVE Organization/Membership + exact resource scope. Shipment binding
also requires existing `shipment.draft.read` on that Shipment/facility. Permissions
to see Ticket subjects/messages are not inferred from Shipment read alone.

Internal assignee is a current eligible same-Organization member already holding
support.read/write at the Ticket facility. Assignment confers no permission and
does not change the CSKH handler. Candidates disclose only accountId,
membershipId,displayName; no email/phone/security fields. Recheck target Identity
and membership under locks; revocation-first must deny. Null assignment is legal.

Carrier triage/handling reuses the canonical existing SUPPORT_TRIAGE/HANDLE
Staff grants, with an ADDITIONAL explicit Organization binding to the actual
existing grant ID. A binding cannot confer authority without the current canonical
grant; a Personal Staff grant without that binding cannot authorize Business tickets.
Triage may see bounded NEW metadata and assign an eligible scoped handler;
handler needs current grant AND current case assignment to read/reply/transition.
Merchant support.assign never grants Carrier authority. No runtime grant API.

Audited offline Local/Test operator commands can grant/revoke these explicit
Merchant permissions or bind/unbind existing canonical Staff grants for supplied
verified UUIDs. No second Staff grant authority or automatic global Staff grant.
Require operator role, explicit Organization/account/permission/reason/request UUID
and idempotent non-PII audit. Never select real accounts or grant all Organizations.

## Wire [PROPOSED]

Merchant prefix `/api/v1/merchant-organizations/{organizationId}`. Existing
TLS/Bearer/current admission, Origin/CSRF on writes and no-store. Strict unknown/
duplicate JSON/query rejection. UUID Idempotency-Key; If-Match `"vN"` on Ticket
writes except create. Internal assignment/preferences have `"aN"` / `"nN"`.
Absent/foreign/scoped-out404, invalid400, stale/state/key409, missing version428,
quota429, disabled503. No blind POST retry after an unknown write.

| Route | Input / exact outcome |
| --- | --- |
| GET /support-tickets | page>=0,size20; optional state,category,shipmentId,facilityId,creatorId,assignee(NONE/UUID),requestedPriority; optional from/until Vietnam creation dates max366 days; sort UPDATED default or NEWEST, deterministic ID tie-break. No caller totals/owner. |
| POST /support-tickets/search | Read-only body with the above filters plus literal search1..160 for code/subject/source reference; Origin/CSRF required, no Idempotency-Key, receipt, quota write or meaningfulAction. Search content never goes in URLs/logs. |
| POST /support-tickets | {kind,shipmentId:null\|UUID,facilityId:null\|UUID,category,subject:1..160,message:1..4000,requestedPriority};201/Location/ETag. GENERAL requires shipmentId null; SHIPMENT facility must match authoritative Shipment or be server-derived when omitted/null (a non-null mismatch is rejected, never ignored). |
| GET /support-tickets/{id} | Summary + publicMessages + internalAssignment + allowedActions; ETag. |
| POST /support-tickets/{id}/replies | {message}; current permitted Merchant state/version. |
| POST /support-tickets/{id}/review-requests | {message}; one pending review against current resolution version. |
| POST /support-tickets/{id}/acknowledgements | empty JSON object; resolution acknowledgement only. |
| GET /support-receipts/{operation}/{key} | {operationId,targetId,outcome:COMMITTED,version}; actor/Org/facility/purpose bound. Operations CREATE,REPLY,REQUEST_REVIEW,ACKNOWLEDGE,ASSIGN,PREFERENCES,READ_NOTICE. |
| GET /support-tickets/{id}/assignment-candidates | page,size20; eligible current same-scope member metadata only. |
| GET /support-tickets/{id}/assignment | {ticketId,organizationId,assigneeAccountId:null\|UUID,version,updatedAt}; virtual a0, read has no write. |
| PUT /support-tickets/{id}/assignment | {assigneeAccountId:null\|UUID}; current support.assign + aN; no-op409. Append internal audit/event; no Carrier state change. |
| GET /support-tickets/{id}/internal-history | page,size20; {items:[{id,operation,actorAccountId,priorAssigneeAccountId,assigneeAccountId,version,occurredAt}],page,size:20,hasNext}; assignment-only metadata, not Tracking. |
| GET /support-reports | required from/until creation dates max366 days; same supported Ticket filters except page/sort; summary described below over all authorized matching Tickets. |
| GET/PUT /support-notification-preferences | self-owned {enabled,eventTypes,version,updatedAt}; virtual n0/default disabled. PUT {enabled,eventTypes}; types ASSIGNMENT,REPLY,NEEDS_INFORMATION,RESOLUTION,REVIEW. |
| GET /support-notices | self-only page,size20; {items:[{id,organizationId,ticketId,eventId,type,createdAt,readAt,version}],page,size:20,hasNext}. Current Ticket scope before paging/output. Unread version1; read version2. |
| POST /support-notices/{id}/read | empty JSON; self-only, If-Match current notice version + receipt; no other member's notice. |

Summary: {id,code,organizationId,facilityId,creatorAccountId,kind,shipmentId,
category,subject,state,version,createdAt,updatedAt,requestedPriority,priority:NORMAL,
responseDueAt:null,teamLabel:null\|ArgoPost Support,internalAssigneeAccountId}.
List: {items,page,size:20,hasNext}; counts must not be inferred from one page.
PublicMessage: {id,authorKind:MERCHANT\|SUPPORT,message,createdAt,eventType};
detail adds {publicMessages,internalAssignment,allowedActions}. No raw credentials,
handler account/contact, compensation or arbitrary private Staff fields.

Carrier prefix `/api/v1/support-operations`: GET /organization-queue with page,
size20 and organizationId; POST /organization-cases/{id}/assignments
{assigneeAccountId}; GET /organization-cases/{id}; POST /organization-cases/{id}/
replies {message}; POST /organization-cases/{id}/transitions {action,message}.
START NEW->IN_PROGRESS; REQUEST_INFORMATION IN_PROGRESS->NEEDS_INFORMATION;
RESOLVE IN_PROGRESS->RESOLVED; REOPEN only RESOLVED + pending Merchant review.
Plain Staff reply allowed IN_PROGRESS only; no generic arbitrary state patch.
Same version/idempotency/current scoped authority rules; no Merchant self-approval.
GET `/api/v1/support-operations/organization-receipts/{operation}/{key}` is the
Staff-only receipt route, bound to original actor/Organization/current canonical
grant + Organization binding and assignment when applicable. Operations are
CARRIER_ASSIGN,CARRIER_REPLY,CARRIER_TRANSITION, distinct from Merchant ASSIGN.

Reports: {schemaVersion:ORGANIZATION_SUPPORT_REPORT_1,organizationId,generatedAt,
timeZone:Asia/Ho_Chi_Minh,period:{from,until},totalTickets,byState,byCategory,
byRequestedPriority,firstResponse:{sampleCount,totalMs,minMs,maxMs},
firstResolution:{sampleCount,totalMs,minMs,maxMs},slaState:UNPUBLISHED}.
Counts/durations are decimal integer strings; min/max null if sampleCount zero.
Source cohort = Tickets created in the selected Vietnam date window, using CURRENT
state and authorized scope. First response uses first substantive SUPPORT public
START/REPLY/REQUEST_INFORMATION/RESOLVE message after creation; assignment/system
notices excluded. First resolution explicitly uses first RESOLVE event; reopening
never rewrites that history. No made-up SLA success/overdue or staff productivity.

## Data / atomicity [PROPOSED]

Forward V33 Orders-owned tables: organization_support_ticket,
organization_support_message, organization_support_assignment (Merchant internal),
organization_support_assignment_event, organization_support_case_assignment
(Carrier handler), organization_support_staff_scope, organization_support_receipt,
organization_support_audit, organization_support_window,
organization_support_notification_preference, organization_support_notice.
Scoped operator audit/receipt uses a separate append-only support_operator_audit
relation; runtime cannot execute operator procedures. Preserve V22 Personal tables,
NOT NULLs, grants, receipts and APIs unchanged.
The existing `support_staff_grant` codes/active uniqueness/rows remain unchanged;
only an additive same-grant/account/permission unique key may be needed to back
the new Organization binding FK. This restriction does not create a grant engine.

Composite same-Org/facility/Ticket/Shipment/member FKs, immutable resource bindings,
legal state/version transitions, one current internal and one current CSKH
assignment, actor/purpose/key HMAC receipts, append-only public messages/audit and
mandatory deferred evidence checks. No direct edits to Shipping/Finance/Claim data.
Receipt target variants are explicit: TICKET for lifecycle/internal assignment,
PREFERENCES for actor-owned Organization preference and NOTICE for self notice
read; variant-specific FKs/CHECKs forbid substituting an arbitrary target UUID.
Reading a notice is a version1->2 acknowledgement, never a Ticket state mutation;
same-key replay returns its original receipt without another version/event.
General Ticket source facility state rechecked on create, history retained on
archive; current Ticket scope remains mandatory for reads of historic resources.

Current Identity subjects -> current Merchant membership/scope -> Support Staff
scope if applicable -> Ticket/assignment -> messages/audit/receipt/quota/notices,
one local physical transaction. Shared pure lifecycle/normalization policy with
Personal remains regression-tested; persistence does not substitute accountId for
organizationId. Audit/receipt/notice failure rolls back all effects. Same-key race
one effect; stale concurrent reply/resolve or assignee revoke cannot overwrite.
Quota:5 creates/actor/Organization/15min;20 Merchant conversation commands/actor/
Organization/15min; matching replays do not consume quota or extend idle.

In-app notices only for current eligible subscribed creator/internal assignee,
each still permitted to read the Ticket. Unique(event,recipient); no duplicate on
replay, no broadcast, no outbound email/SMS/Push or cross-Organization disclosure.
GET/list/read-notice do not claim a meaningful account activity or Carrier event.

## Excluded pending their own accepted contracts / actual policy

Private attachments require scoped metadata, quarantine/scanning, download ACL,
integrity/size/type validation and storage-failure recovery; existing S3/media
or Personal evidence approval is not an Organization Ticket attachment contract.
No unscanned upload, public evidence URL or document-body substitute in V1.

Shipment changes/cancel/return/retry and COD investigation require their actual
service/state/authority contracts. Claims require case/evidence/investigator,
separate proposal/approval, responsibility/liability/limits and authorized Finance
payment. V1 Tickets may ask for investigation; no Claim award/settlement promise.
Full Support acceptance requires those later capabilities, not a mock contact form.

## Acceptance before source delivery / later explicit deployment

Disposable PG18 populated upgrade preserves V1–V32 checksums/data/authority;
Personal Ticket regression; Organization/facility/membership/Staff-assignment/
permission isolation on list/detail/receipt/report/notice; wrong Shipment and
overposting; state/rate-limit/same-key/concurrency and target revocation; audit/
receipt/notice failure atomicity; no Carrier/Finance/Claim writes; Production deny.
Frontend -> actual HTTPS -> PG: Merchant create, Carrier triage/assignment/start,
information request, Merchant reply, resolve/review/reopen/close, internal
assignment/report/notice read, refresh and lost-write recovery. Synthetic fixtures,
private evidence, source/report guards, architecture, H2/PG18 and affected UI gates.
No automatic PR merge, localhost migration, real account selection or new grants.
