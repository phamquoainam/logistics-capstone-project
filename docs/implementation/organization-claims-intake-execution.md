# Organization Claims Intake - source/test execution

Date: 2026-11-20. Boundary: Owner-authorized intake/evidence first; compensation
BLOCKED. No existing demo or Production database, account, grant, binding, feature
flag, runtime, credential or certificate was changed. No commit/push/merge.

## Delivered scope

- Organization/Shipment/Ticket-bound document intake, allegation categories,
  ETag/idempotent supplement and assigned CSKH document review. Eligibility remains
  UNASSESSED; closing an intake never decides entitlement or Carrier facts.
- V38 adds five tables with composite foreign keys, immutable history/receipts,
  legal transitions and deferred same-transaction evidence. No historical migration
  edit or V33 placeholder. V36/V37 are the separate Staff prerequisite.
- Durable content-bound reservation before storage I/O; bounded PNG/JPEG/passive
  PDF screening, immutable private object, reauthorization after I/O, atomic CLEAN
  metadata/event/receipt. Revocation or a late database failure leaves private
  RESERVED bytes recoverable by an exact authorized retry, never publicly served.
- Reused private filesystem contract moved into Platform with compatibility alias;
  S3 adapter uses existing SDK/default credentials, SSE-S3/checksum/conditional put,
  fixed server-generated key, no ACL/public URL/Delete operation. No new AWS service.
- Full scoped-cohort reports, actual response/closure duration samples, event-based
  notices using existing Ticket preferences, no duplicate notices on replay.
- Merchant and dedicated Staff UI: real linked Ticket intake, evidence upload and
  private download, history, information request, supplement, closure, reports and
  notification deep links. No compensation or payment controls.

## Verification evidence

Private ignored evidence lives under `.demo/cskh-focused-<suffix>`; raw mail,
HTTP diagnostics, PDFs and XML are not publication artifacts. Synthetic fixtures
only; no real Customer PII was added.

| Evidence suffix | Actual result |
| --- | --- |
| `b8dcf3383dde4788ac626ccbb77fec15` | V38 upgrade/integrity: 1/1 PASS |
| `6af10c74327b46d0b89eb5089b11f898` | Intake HTTP persistence/security: 1/1 PASS |
| `6f4c719161b44d3792e875c0cd22ace0` | Real HTTP/storage/PG failure, retry and revoke-after-I/O: 1/1 PASS |
| `ca95369a708f40498635dbbd09fee521` | Browser, evidence HTTP, V38/config and architecture: 17/17 PASS |
| `9b72471f538b45b7aabc2e5fe0e3ee8a` | Outbox, evidence HTTP, guard/crypto and S3 HTTP transport: 9/9 PASS |
| `b82792e3bb3940169b13a36a37cd469d` | Late DDL rollback plus scoped reports/notices/late-failure/preferences: 2/2 PASS |
| `5c66fad7892d42699e7d67086446b4b0` | Affected final regression: 382 PASS, 1 FAIL / 383, no skips; all 72 migration cases PASS. Staff and Claims Browsers PASS. Existing Customer Browser had obsolete Claims navigation assertion; correction rerun below PASS. |

| `0c3e80f833d6428e8cfefcc86a7be1fd` | Corrected existing Customer Browser: all 19 cases PASS, JUnit 1/1 PASS. |
| `89d066b6d88d4c75b6692642f870025e` | Final Claims Browser: 1/1 PASS, source unchanged. Mobile 390px image visually verified after evidence loaded and navigation overlay closed. |

Tests include cross-Organization and Facility denial, current membership/Staff
binding/assignment, direct DML denial, concurrent duplicate requests, stale versions,
late event/receipt/notification failure, malicious and oversized content, scanner
failure, byte integrity and private unauthorized read. Controlled ClamAV INSTREAM
daemon verifies protocol behavior; it is not real antivirus classification. S3 SDK
mock and controlled HTTP verify requests/integrity/retries, not live AWS storage.

Frontend: `npm.cmd run typecheck`, `npm.cmd run lint`, `npm.cmd run build:customer`
PASS; build retains its large-bundle warning. Focused Staff/Claims/evidence/Support
unit suites: 13/13 PASS. Python report-policy/bootstrap/validator tests: 187/187 PASS.
Actual HTTP Browser uses owned HTTPS, isolated PG18 and SMTP inbox; no API fulfilment,
trust-all TLS, persistent tokens or real Gmail delivery.

## Failures retained, not hidden

Earlier failed runs remain private. Fixed causes include incorrect schema-column
names in test/SQL, wrong rowtype field evaluation, immutable-event transaction
checks, a fixture attempting to revoke mandatory base permission, a corrupted
Vietnamese locator, stale detail submission during reload, and an old Claims-tab
assertion. None was resolved by skipping tests, weakening access or Flyway repair.

## Remaining boundary

Compensation eligibility/windows/award/appeal/SoD and financial execution remain
BLOCKED by missing approved commercial policy. Automatic escalation or SLA promises
are not invented. No live AWS/ClamAV/demo activation or full PostgreSQL/GitHub CI
PASS is claimed. Final current gate status is recorded with Staff evidence in
[cskh-staff-authority-execution.md](cskh-staff-authority-execution.md).
