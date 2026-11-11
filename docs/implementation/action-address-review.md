# Personal Action Center / Address Book review —2026-11-09

Owner-directed feature delivery; preserve all53 existing working paths before
edits. Reuse approved Personal Management/Address/Shipping/Analytics contracts.
No new schema/API/provider/Carrier or Finance command is authorized by this review.

- [x] AA-01 Inventory existing contracts, implementation and preserve source hashes.
- [x] AA-02 Address UX: Management navigation, explicit normalization, advisory
  exact-duplicate check using existing self reads, default shortcut, empty-page
  recovery and owned contact copy into a new Shipment Draft.
- [x] AA-03 Action Center boundary UX: show requested issue taxonomy and the actual unavailable
  source boundary; no false empty-success/counts/dead action buttons/fake tickets.
- [x] AA-04 Fees audit: preserve exact snapshot/UNPAID; validate filter/page and
  source error handling, leave unsupported payment/COD/adjustment commands closed.
- [x] AA-05 Focused/unit/UI/build/browser/PG validation, responsive evidence,
  traceability and final fingerprint preservation.

## Mandatory dependency disposition

Accepted Address Book policy explicitly forbids editing/restoring archived
contacts; archive keeps immutable history and clears defaults atomically. Owner
asks restoration "according to policy", so this review does not create a restore
endpoint or change that accepted rule. Display policy before archive. Labels are
the already accepted optional80-character label, not new group entities.

Administrative area/locality remain customer-entered UNVERIFIED VN contacts, not
an official administrative dictionary or coverage decision. Existing Quote uses
only approved SIM routes; never infer serviceability from raw text. A recipient
contact is not an Identity account or verified credential.

Action Center has no accepted exception/request/ticket source, Customer response
commands, priority/deadline policy or resolution state machine today. All requested
operational categories require those sources before becoming live cases. Keep
the capability unavailable, not zero cases or a list copied from ordinary waiting
shipments. No Customer Tracking/Delivered/compensation/payment mutation is added.

Duplicate checks are advisory exact matches among active self-owned contacts,
compared across the entire existing phone/kind query before paging ends. They are
not a uniqueness invariant or permission grant. No contacts in logs/localStorage/
export; no silent merging. Normalization is an explicit user action and never
invents a different geographic address. Existing Backend still validates writes.

## Implemented bounded behavior

- Sổ địa chỉ appears once under Management; route-based disclosure opens for
  direct/SPA navigation without overriding same-page manual collapse/keyboard use.
- Explicit normalization retains accents and complete location. Phone formatting
  follows existing VN rules. Contact label remains optional; no group schema added.
- Duplicate check excludes the edited row, retains kind/name/phone/full-location
  identity, searches successive own phone/kind pages and does not merge/block a
  legitimate write based solely on an advisory result. Error/limit/repeated rows
  give an unknown outcome, not a no-duplicate success. Stale form result is hidden.
- Default shortcut reuses versioned PUT and idempotency/receipt recovery. Failure
  or unknown outcome never paints a default; alternate operations remain locked
  until receipt confirmation. Backend default uniqueness/serialization is unchanged.
- Owned current contact copy into a new draft accepts only UUID selection context
  in the frontend URL; no PII or credential. Re-read/copy validates kind/current
  ownership. Foreign/wrong-kind/archived/invalid selections fail; existing draft
  plus new selection is rejected before reads. Source edit/archive never changes
  already-booked Shipment snapshots. No draft is created by merely opening a link.
- Empty later Address/Fees pages recover to page0 while retaining actual filters;
  Address has explicit filter reset. No invented totals beyond the existing page.
- Action Center has distinct pending/sent views, category/response guidance and
  a clear unavailable-source state. No fake case/status/count/priority/deadline,
  dead submit action, customer evidence upload or Carrier/Finance mutation.
- Fees audit retains full-cohort confirmed/UNPAID metrics, source-bound snapshots,
  page-only CSV and separate unavailable Payment/COD/refund/inquiry panels. No
  positive paid/overdue/collection/legal-document assertion without a real source.

## Evidence collected

`npm.cmd test`:94 PASS, including2 new normalization/comparison cases.
Typecheck/lint/default+Customer build PASS. Final full
`npm.cmd run test:customer-ui`:141/141 PASS, including12 new Address/Action cases
and Fees empty-page recovery. Synthetic desktop1440/mobile390 visuals reviewed.
The first full UI pass had139 PASS/2 failures because the old navigation test
looked for the now-nested Address link while Management was closed. Updated the
font comparison to another top-level link; retained keyboard/layout assertions.

Focused PG command uses `-Ptestcontainers-postgres` and test selector
`IdentityGrantApplicationTests#address*+customerEntryBrowserUsesActualHttpsPostgresAndSmtp`.
Ten Address methods PASS (CRUD/foreign isolation/validation/default and archive
races/replay/audit rollback/revocation/admission). Browser13/13 PASS using real
HTTPS/PG18/test SMTP. Initial parent postcondition still expected old address v3/
four history+receipts; two new explicit default updates require archived v5/six
history+receipts. Updated exact counts and added zero active defaults after archive;
final parent revalidation recorded below. No assertions relaxed or disabled.

First PG attempt failed before container startup because Docker Engine was off;
started existing Desktop and reran only the affected tests. No Reset/Purge,
application database reset or Flyway repair. Test resources have credential privacy
and owned cleanup checks; raw diagnostics remain ACL-protected and ignored.

No production Backend/API/schema/migration changes in this task; only test host
SPA route and Browser parent evidence expectations changed on Backend side. Existing
PW-03/PW-04/Booking/Station/UNPAID isolation remains. Earlier broader suites are
not claimed as rerun; hosted CI/publication remains pending, no commit/push/merge.

Final revalidation: Browser parent1/1 PASS, no failures/errors/skips; actual
Browser13/13 PASS, safe receipt PASS/CLEANUP/NONE/LOCAL_UNPUBLISHED. Exact v5/six
history+receipts/zero-active-default postconditions PASS. Final and10-method Address
runs have privacy/owned-resource cleanup PASS. Source validator errors[] and
semantic ownership CONSISTENT; whitespace PASS. Initial failed logs retained.
AA-01–05 bounded implementation/checkpoint work is LOCAL DONE. Live Action Center
cases/responses and archived-contact restoration are explicitly NOT implemented
or accepted by this completion marker; dependencies above remain BLOCKED.

Local launcher reused the existing PostgreSQL18.6 container/volume, existing
secrets and trusted certificates. Flyway check PASS and LOCAL_FULL_STACK_HTTP_READY.
HTTPS frontend Action Center/Addresses/Billing routes return200; unauthenticated
Address API returns401. No init/credential rotation/live Gmail test claimed.
