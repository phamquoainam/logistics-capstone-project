# Organization Action Center & Contact Directory V1 [ACCEPTED]

Date: 2026-11-15. Owner requests Business Management3.4/3.5. This packet covers
the accepted API/data/security scope, preserving OSM/MRF/BSC behavior.
Owner accepted both V1 scopes in chat on 2026-11-15; the original filename is
retained for stable links. OAA-03 was source/test authority only. Owner separately
approved OAA-08 for the confirmed NamShop Demo Customer/Organization on2026-11-16;
additive V29/V30 and exactly six new Organization permissions, Local/Test only.
Current branch: `feature/argopost-organization-management`, HEAD5172dee; all57
preceding working source paths copied and SHA-256 preserved before this task.

## Tasks and implementation order

- [x] OAA-01 Inventory sources/authority and preserve pending source.
- [x] OAA-02 Existing-contract Action Center: paged current Draft work and scoped
  unassigned Bookings; genuine continuation/assignment links, truthful source gaps.
- [x] OAA-03 Owner accepted both V1 scopes in chat, 2026-11-15. API/data/permission
  implementation authorized; disposable test DB first, no live V29/V30 activation.
- [x] OAA-04 Organization contacts CRUD/archive/restore/defaults, current scopes,
  validation/duplicates, real PostgreSQL and create-entry snapshot selection.
- [x] OAA-05 Bounded directory CSV/XLSX import preview/explicit commit and export;
  contact permissions, actor receipts, audit and literal spreadsheet safety.
- [x] OAA-06 Durable Merchant internal work: assignment, replies/review/completion,
  priority/deadline/history and assignee in-app notices; no Carrier event writes.
- [x] OAA-07 Upgrade/isolation/concurrency/privacy/unknown-write/FE/HTTP/PG/Browser
  gates, source inventory/report guards and traceability; DONE only with evidence.
- [x] OAA-08 Owner separately approved Local deployment2026-11-16; verified JAR,
  forward V29/V30, six exact-target grants with audit, preserved existing data,
  HTTPS readiness/unauthenticated denial and Owner-confirmed authenticated screens.
  OAA-03 itself did not authorize automatic deployment or real-account grants.

LOCAL/TEST + Customer SANDBOX only, default disabled and deny prod/production,
current verified Identity + ACTIVE Organization/Membership + actual scoped grants.
No new worktree, source discard, special account seed or modification of V1-V28.
Forward V29 contact and V30 task migrations only after confirming stream position.
Personal APIs/tables/contact restore policy and all booked snapshots stay intact.

## Existing-source slice (already authorized wire)

Use existing `/shipments` category ACTION UI, GET shipment-preparations pages20
and GET shipments management with assignee NONE. Draft work is not an exception
Shipment; unassigned is an internal organization responsibility, not delivery
failure or payment delinquency. Show server totals only where available; a Draft
page count is not a global count. No broad client pagination scan or fake incident.
Existing per-row allowedActions controls continuation and Shipment assignment.
Carrier/Support/Finance issues remain unavailable until their actual sources exist.

## Contact semantics and current permissions [ACCEPTED]

Owner module `merchant`; Org is derived from admitted context, never trusted from
a contact body. Contacts are UNVERIFIED sender/recipient records, not Identity
accounts, confirmed administrative locations, serviceable routes or Carrier sites.
Reuse Personal contact normalization/limits and validation helpers without changing
Personal ownership/persistence. No new map/storage/email provider or geocoding.

Explicit permissions `contacts.read`, `contacts.write`, `contacts.import`,
`contacts.export`, each ORGANIZATION or same-Org FACILITY. write/import/export
require matching read scope; import additionally requires write scope. Extend the
exact FACILITY allowlist, not an unrestricted permission-prefix rule. Existing
shipment permissions are unchanged; directory is independently permissioned.
Null facility is Organization-shared data requiring Org-level permission; finite
facility grants cannot read it or another warehouse through search/default/export.
No automatic role grants. Explicit operator permissions remain audited/revocable.

Contact input `{kind:SENDER|RECIPIENT,facilityId:null|UUID,label:null|string,
reference:null|string,group:null|string,contactName,contactPhone,address}`.
label/group1..80, reference1..80 when present; name1..160; address reuses accepted
`{country:VN,administrativeArea:1..160,locality:1..160,addressLine:1..500}`.
Phone accepts domestic0/84/+84 and persists canonical +84, never verified credential.
NFC/trim/control rejection, no unknown/duplicate JSON fields or hidden owner/status.
kind/facility binding immutable; moving requires an explicit new contact record.
group/reference are Merchant-entered labels/codes, not a fictitious Store master.

Output Contact = input + `{id,organizationId,state:ACTIVE|ARCHIVED,
verificationStatus:UNVERIFIED,version,createdAt,updatedAt,archivedAt,allowedActions}`.
List `{items,page,size:20,hasNext}`; sorted updatedAt DESC/id ASC. GET archived
requires current same scope. archive retains history and atomically removes all
default bindings; restore creates a new active version without stealing defaults.
Same non-null reference is unique per Org/kind/facility including archived entries;
conflict must identify an accessible record or give a non-disclosing error.
Duplicate identity compares kind/canonical phone/normalized full name/full address;
advisory only, never silently merge or reject distinct contacts sharing a phone.

Defaults are separate versioned bindings per `(organization,facility|null,kind)`.
The contact must be active and have exactly that same facility/kind. Selecting a
default requires read/write at that scope. One active default per binding enforced
in PostgreSQL with atomic ETag/idempotent replacement; no front-end-only invariant.
Archive clears a matching binding in the same transaction. Restore leaves it empty.
Facility is Merchant configuration, not Pickup capability or a verified location.

### Exact contact HTTP [ACCEPTED]

Prefix `/api/v1/merchant-organizations/{organizationId}`. TLS/Bearer/no-store;
mutations require existing Origin/CSRF, UUID Idempotency-Key and If-Match `vN`
except CREATE and new virtual default version0. Query/body shapes are strict.

| Operation / route | Exact behavior |
| --- | --- |
| GET `/contacts` | page>=0,size20; optional kind,state ACTIVE/ARCHIVED,facilityId. Current scope before paging. No contact text in URL. |
| POST `/contacts/search` | Read-only body `{page,size:20,search:1..160,kind?,state?,facilityId?,group?}`. Literal name/phone/reference/address search; current Origin/CSRF but no mutation/idempotency/audit side effect. Search not in URL/browser storage/logs. |
| POST `/contacts`; GET/PUT `/contacts/{id}` | Create/replace accepted input;201+Location/ETag or200; version-bound replacement. |
| POST `/contacts/{id}/archive`, `/restore` | Empty body, current version; inactive edit denied; archive/restore idempotent replay exact original result. |
| POST `/contact-duplicates` | Read-only `{input}`; current writable/readable target scope, page20 bounded candidates, no all-directory scan or cross-scope disclosure. |
| GET/PUT `/contact-defaults` | Query kind,facilityId optional. Virtual `{kind,facilityId,contactId:null,version:0,updatedAt:null}`; PUT `{contactId:null|UUID}`, If-Match `dN`, scoped durable receipt. |
| GET `/contact-receipts/{operation}/{key}` | `{operationId,targetId,outcome:COMMITTED,version}`; current scope/permission, actor/purpose bound; no contact PII. |
| POST `/contact-imports` | `{templateVersion:ORG_CONTACTS_CSV_1,facilityId:null|UUID,csv}`. Max100 rows/256KiB; exact RFC4180 headers kind,label,reference,group,contact_name,contact_phone,country,administrative_area,locality,address_line. Formula/control/duplicate header rejection. XLSX parsed using existing literal-cell frontend worker into this same wire. Preview persists normalized rows/errors, no contacts created. |
| GET `/contact-imports`, `/contact-imports/{id}` | Current creator + current Org/facility/import/read/write rights, paged job list/rows20; real source totals/version/result IDs. |
| POST `/contact-imports/{id}/commit` | `{rows:[1..100]}` <=20 unique selected VALID rows, If-Match; transaction all-or-nothing for chunk, deterministic job/row receipts, unique-reference race safety. Preview duplicates need explicit selection, never auto-merge. |
| POST `/contact-exports` | Read-only export request with search filters body, <=200 current matching contacts; reject overflow, no truncation. Requires export+read on every row, audit before output. Literal JSON rows for existing CSV/XLSX writer, no hidden Identity/email or credentials; source labels local/test on saved file. |

Applying contact/default to manual Business creation re-reads current active
same-Org/scope/kind data and copies a snapshot; it cannot overwrite unsaved edits
without confirmation or mutate existing Bookings. Contact IDs only as UI context,
no PII in URL. Bulk UI can explicitly fill selected unlocked source cells with a
current contact snapshot before preview; no new hidden import or Booking effect.

V29 tables (Merchant-owned): organization_contact (input/state/version/timestamps),
organization_contact_version (immutable snapshots), organization_contact_default
(binding/version), organization_contact_command_receipt (Org/actor/operation/key,
target/expected/result version, HMAC/key ID/scope), organization_contact_audit
(metadata only), organization_contact_import_job/row (bounded creator/job/scope
fingerprints/outcomes), organization_contact_export_audit (IDs/scope/count/digest).
Composite Org/facility FKs; current/version/default consistency, append-only
history/audit/receipts; source+audit+receipt/default changes atomic. Retention/purge
is not implemented. Runtime cannot grant permissions, DDL, delete history or read
archived contacts without scope. No Personal or Identity table writes.

## Merchant internal Action workflow [ACCEPTED]

New durable work is **internal Merchant work**, separate from Carrier exceptions,
Support Tickets, Finance Disputes, Claims and Tracking. Sources: existing same-Org
active DRAFT or confirmed BUSINESS Shipment, immutable exactly-one target binding.
Types PREPARATION_REVIEW or SHIPMENT_FOLLOW_UP; not a Carrier failure classification.
Existing-source Draft/unassigned tabs remain independent from durable task totals.

Read uses current shipment.draft.read scope. Add `shipment.actions.manage` for
creation/assignment/priority/deadline/review/close/reopen and
`shipment.actions.respond` for the current assigned member's START/comment/review
request. Both require same scope read; no role-name bypass. Candidate assignee
must be active verified same-Org member with read+respond at the target facility.
Reuse Identity subject locking and Merchant admission ordering before row locks.
Historical assignees remain audit facts, not ongoing permission grants.

States OPEN, IN_PROGRESS, AWAITING_REVIEW, COMPLETED, CANCELLED. START is assigned
responder only OPEN->IN_PROGRESS. REQUEST_REVIEW requires a substantive response,
IN_PROGRESS->AWAITING_REVIEW. Manager COMPLETE only AWAITING_REVIEW->COMPLETED
with conclusion; REOPEN completed/review->OPEN with reason; CANCEL unresolved work
requires manage and reason. Internal completion never changes Shipment/Draft,
Tracking, fee, Carrier acceptance/delivery or compensation.

Priority LOW/NORMAL/HIGH is manager-set, default NORMAL; dueAt nullable UTC future
instant set explicitly by authorized Merchant manager, never inferred Carrier SLA.
Overdue derives from nonterminal current task with dueAt<serverTime. Preserve
initial dueAt and append deadline changes; completion latency is internal work
time, not shipping/delivery SLA. Comments1..2000, title1..160, details1..2000;
no credentials or files. Evidence upload needs its own private/scanning authority
amendment; do not reuse Merchant Application blobs as task evidence without it.

### Exact Action HTTP [ACCEPTED]

Same prefix/auth/version/idempotency/error/no-store conventions as contacts.

- GET/POST `/action-tasks`; list page,size20 and optional targetKind,targetId,
  facilityId,assignee NONE/UUID,state,priority,overdue=true; counts for the same
  scoped filtered cohort, no false Carrier/Support counts.
- Create body `{targetKind:PREPARATION|SHIPMENT,targetId,type,title,details,
  priority:NORMAL|LOW|HIGH,dueAt:null|instant}`; initial OPEN/unassigned.
- GET `/action-tasks/{id}` returns summary plus ordered public internal events and
  server allowedActions. Summary `{id,organizationId,facilityId,targetKind,targetId,
  type,title,state,priority,assigneeAccountId,dueAt,initialDueAt,version,createdAt,
  updatedAt,completedAt,overdue,allowedActions}`; details only on authorized detail.
- GET `/action-tasks/{id}/assignment-candidates`, PUT `/{id}/assignment`
  `{assigneeAccountId:null|UUID,reason}`; ETag/idempotency/current target authority.
- POST `/{id}/comments` `{message}`; POST `/{id}/transitions` `{action,message}`;
  PUT `/{id}/policy` `{priority,dueAt,reason}`; versioned append-only events.
- GET `/action-task-receipts/{operation}/{key}`; actor+scope+purpose bound.
- No general bulk mutation in V1: select <=20 tasks, execute explicit individual
  permitted versioned commands with real per-task outcomes; no all-success claim
  if a row fails or becomes revoked. No implicit mass Carrier workflow.

V30 Orders-owned organization_action_task/event/command_receipt with composite
Org/target/facility/member FKs, constrained state/version edges, deferred mandatory
event/receipt invariants, no direct runtime arbitrary status or audit mutation.
Workflow composes Orders public APIs and Notifications public API in one physical
transaction. Optional validated contactId link references the exact active scoped
Organization contact without changing shipping snapshots; no Personal contact link.
Unavailable Ticket/Finance Dispute/Carrier request links show no invented IDs.

Notifications-owned organization_action_notice stores Org/task/recipient/event
IDs/type/timestamps only, not contact/details; same transaction as assignment or
relevant follow-up. GET `/action-notices` returns own currently authorized task
notices20, POST `/{id}/read` marks own notice read idempotently, never completes
the task. No external email/SMS/push or fabricated Carrier exception notification.

## Gates and boundaries

Real PG18 populated upgrades preserve prior data/checksums/grants. Isolation across
Org/facilities/search/defaults/restore/import/export/receipts and revoked subjects;
concurrent default/archive/restore and same-key writes; stale versions and all
mandatory-evidence rollback; permission/CSV injection/PII/Production denial tests.
Action actor/assignment/revocation/transition/deadline/notice isolation and atomicity.
Frontend/HTTP/Browser must prove real CRUD, explicit selection, unknown receipt,
preparation snapshot copy and assigned-worker->manager review flow. No fake Carrier
request, Claim award, payment, COD, delivery, Pickup or quoted coverage inference.
