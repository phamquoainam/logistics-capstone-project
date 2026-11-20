# Organization Claims Intake V1 — source/test contract

Date: 2026-11-20. Authority: Owner Organization Support & Claims standing
source/test directive, followed by explicit **intake/evidence first;
compensation BLOCKED**. This is a new bounded implementation amendment, not a
statement that canonical Spec 021 is fully accepted.

## Scope and invariants

- `orders` owns intake, attachment metadata, events and receipts. `workflow`
  composes public Identity/Business/Support/Claims APIs in the existing transaction.
- A case references one real confirmed Organization Shipment and one
  SHIPMENT-kind Organization Support Ticket for that same Shipment/Organization.
  Facility is copied from the locked Ticket; neither owner nor facility comes
  from the request body. No Personal-to-Merchant reassignment.
- Categories: LOSS, DAMAGE, DELAY, SERVICE_DISPUTE. These are allegations, not
  verified Carrier events. Recording LOSS does not mark a Shipment lost.
- States: RECEIVED, UNDER_REVIEW, NEEDS_INFORMATION, CLOSED_WITHOUT_DECISION.
  Merchant may supplement an open case. An assigned, currently authorized CSKH
  Handler may begin document review, request information or close the intake
  without a compensation decision. No APPROVE, AWARD, REJECT_ENTITLEMENT, PAY,
  REFUND, PAID, custody or tracking command exists in this slice.
- Eligibility remains UNASSESSED; policy/award/appeal windows and compensation
  remain BLOCKED until the separate business policy/SoD decision. Display this
  status on the Claim detail, not as a repeated global portal banner.
- One open intake per Organization/Shipment/category. Same-key replay is actor,
  Organization, operation, target and HMAC-payload bound; recheck current access
  before returning a receipt. ETag conflicts never overwrite another response.
- Reuse scoped `support.read`/`support.write` for this document-intake-only slice,
  together with access to the linked Ticket. No new Merchant grant, role
  auto-grant or Team delegation expansion is introduced.
  Existing SUPPORT_HANDLE plus current Organization binding and Ticket assignment
  permits document processing only; it never grants compensation authority.
- Source/test only, disabled by default, Local/Test SANDBOX and physical Staff
  environment gates. No automatic demo migration, grants, binding or activation.

## Storage amendment OCI-STORAGE-01

Reuse the existing `PrivateEvidenceBlobs` contract/implementation rather than the
public website media API. Promote its byte-only contract to
`platform.api.PrivateEvidenceStore`; retain the Merchant port as a compatibility
alias. Move the currently unwired filesystem implementation into platform
infrastructure and retain its existing hardening tests. No domain imports
another domain's internal adapter. Authorization remains in the calling domain.

Add server-reserved UUID writes for recoverable external I/O. Key is generated
and persisted before upload, never a caller path, filename or public URL. Writes
are create-only; an exact retry must compare stored bytes/digest and never
overwrite different content. Existing random-key writes remain supported.

S3 uses the current AWS SDK and Default Credentials Provider Chain, private
`private/claims/quarantine/<server UUID>` keys, SSE-S3, no ACL/public URL or Delete
operation. No CloudFront or new AWS resource. Backend streams only after current
Claim access and CLEAN screening; attachment response is no-store/download with
nosniff and a sanitized filename. No unscanned evidence is exposed.

Technical limits: 5 MiB/file (existing storage ceiling), at most ten attachments
and 25 MiB total per case; JPEG/PNG/PDF only, measured content/signature limits,
no SVG/scripts/embedded PDF actions. ClamAV screening is fail-closed. These are
technical intake safety limits, not a compensation or commercial service policy.
Tests use synthetic files and controlled scanner/S3 transports; report simulated
transport separately from live AWS/antivirus verification.

Reservation -> upload/screen outside DB transaction -> authorized finalize.
Durable reservation binds actor/case/content digest and preallocated object UUID.
Final failure leaves private recoverable evidence; do not pretend a failed DB
commit deleted the object or require unavailable business DeleteObject rights.
No orphan is served and no automatic business-object deletion is introduced.

## Implemented source/test wire/schema slice OCI-01

New routes are source/test-authorized under the standing directive; no runtime
feature grant is inferred:

| Operation | Route | Authority |
| --- | --- | --- |
| List/create | GET/POST `/api/v1/merchant-organizations/{org}/claim-intakes` | Current member + scoped Support read/write + Ticket access |
| Detail | GET `.../claim-intakes/{id}` | Same Organization/Facility read |
| Supplement | POST `.../{id}/supplements` | Intake write, open state, ETag/key |
| Evidence reservation/upload/read | POST `.../{id}/evidence-reservations`, PUT/GET `.../{id}/evidence/{evidenceId}` | Same current domain authority; immutable reservation binding |
| Receipt | GET `.../claim-intake-receipts/{operation}/{key}` | Current original actor and domain authority |
| CSKH detail/command | GET `.../support-operations/organization-claim-intakes/{id}`, POST `.../{id}/commands` | Fixed Staff admission + assigned Ticket SUPPORT_HANDLE |

V38 was verified free before creation; it has run only on owned isolated test databases.
Implemented tables: claim intake, immutable event, evidence reservation/metadata, receipt and
notifications sourced from committed events. Composite FKs bind Organization,
Ticket, Shipment, Facility and actor; guards enforce append-only history,
correlated audit/receipt and legal transitions. V1–V37 are not rewritten after
application. Exact implemented DTO/schema manifest accompanies the slice.

## Required verification

TDD for lifecycle, duplicate open case, cross-tenant/facility/role/assignment
denial, receipt replay/conflict/race, revoke during upload, mandatory-audit late
failure, external-storage uncertainty, malicious/oversize/polyglot files,
scanner failure, private authorization and no financial/Carrier effects.
PG18 empty/upgrade and direct-DML negatives, real HTTP/Browser, storage SDK test
transport, source manifest, architecture/report/privacy regressions are required.
No DONE or SOURCE_TEST_READY while those gates are pending.

## Exact implementation inventory and reporting

The [technical manifest](organization-claims-intake-technical-manifest.json) records
16 exact HTTP operations, public DTO records, V38 fingerprint and five tables.
Read-only reports use `support.reports.read` intersected with current Ticket/Facility
access over the whole permitted cohort, not the current page. First Staff response
and intake closure report actual persisted sample counts and total/min/max duration.
Closure is document-intake closure without an award; no commercial SLA is inferred.
The current-recipient event feed reuses Ticket notification preferences. A unique
(event, recipient) key and transactional guards prevent replay or rollback notices;
it does not pretend to implement notification read/unread tracking.

Filesystem activation requires explicit `support.claim-evidence.public-roots`
listing every effective static/dev-server root; private storage must be outside
all listed roots. Missing configuration fails closed. No local runtime is activated.
PNG/JPEG safety includes a ten-million-pixel ceiling; passive PDF limit is 20 pages.
Production/environment, physical database and current Staff/Member guards remain
mandatory. Real AWS object access and actual ClamAV engine classification were not
run: controlled SDK HTTP and loopback INSTREAM test transports are separate evidence.

Commercial eligibility, filing/appeal windows, liability, compensation approval,
payout/refund, financial ledger writes and retention deletion remain BLOCKED.
Evidence is private and recoverable after external-I/O/DB disagreement; no object
in `public/*` or existing business `private/*` was deleted.

See [execution results](organization-claims-intake-execution.md) for real gates and
historical failures. This contract does not claim full Spec 021 or live acceptance.
