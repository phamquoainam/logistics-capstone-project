# Business file import worksheet

Owner direction: 2026-11-10, CSV + Excel with an editable worksheet layout inspired
by the supplied ViettelPost screenshots. Status: LOCAL DONE (file-to-Draft scope).

- [x] IG-01 Preserve 122 existing source paths; inspect current contract and DoR.
- [x] IG-02 Bounded CSV/XLSX adapters, editable cells and full multi-Parcel fidelity.
- [x] IG-03 Search, row/cell selection, paste, undo/redo, row actions, templates,
  column options, full-screen and real current-Organization import history.
- [x] IG-04 Backend revalidation after edits; explicit selected-row Draft commit,
  safe chunking/unknown-write recovery, refresh/context isolation.
- [x] IG-05 Unit/UI/real HTTPS-PG Browser, dependency/source/privacy gates and visuals.

First slice: replace the current raw-text upload form with a worksheet backed by
the existing accepted BusinessImports API. Source/config files: BusinessImports.tsx,
new grid/file-format helpers and styles, client.ts (existing history GET wrapper),
package/lock file for a pinned local Excel parser, focused tests and traceability.
No new server path/DTO/table/permission or migration is needed. Server CSV V1
remains the authoritative validation and idempotent Draft command boundary.

Workbook adapter: XLSX only (no legacy XLS/macros). DonHang has one row per
Shipment with sender/recipient/source and first Parcel facts. Optional KienHang
contains Parcel2–5 keyed by a file-only import_key and sequence. The ROW key is never a Shipment/Parcel ID and is not sent to Backend. Both normalize
losslessly to the already accepted CSV V1 parcels_json; canonical CSV V1 remains
importable. Flat CSV templates support the first Parcel; subsequent Parcels can
be edited in the worksheet and exported via canonical CSV or XLSX. Phone and
source identifiers are text. Weight is grams, dimensions are centimetres in the
worksheet and converted exactly to integer millimetres for the accepted API.

XLSX handling stays in a bounded same-origin Worker. Reject encrypted/ZIP64,
oversized/decompression bomb, formula, external relationship and embedded active
content inputs. Never execute formulas, import caller-owned fees or take ownership
from file cells. No workbook content goes to an external service. Session/Org
changes cancel or ignore stale results; no PII in browser persistent storage.

Editing invalidates the current server check. Original import jobs remain immutable
history; a changed worksheet creates a new preview. Created rows stay read-only and
are excluded from new previews. History's invalid rows do not retain raw rejected
content under the existing contract; reopening the source file is required to
recover those rejected cell values. Do not fabricate them.

Fees stay unavailable until an actual approved Business quote exists. COD, payment,
Pickup, international/express service, Booking and Carrier actions remain closed.
No pretend "push orders" success. This task neither commits nor pushes or merges.

## Observed verification

- `npm.cmd run test -- unit-tests/business-import-grid.spec.ts
  unit-tests/business-data.spec.ts unit-tests/business-preparations.spec.ts`:
  18/18 PASS. Unicode/quoted CSV, leading zero identifiers and phones, explicit
  zero, exact cm-to-mm conversion, five Parcels, corrupt JSON preservation,
  atomic bounded paste/locks, XLSX reorder/key fidelity, formulas/cached formulas,
  hyperlinks/hidden/merged/type rejection, corrupt/oversized ZIP and history
  Organization/counter validation.
- `npm.cmd run test:customer-ui -- ui-unit-tests/business-grid-ui.spec.ts
  ui-unit-tests/business-import-ui.spec.ts`: final focused cases 6/6 PASS (run
  separately when fixing responsive layout). Real browser Worker XLSX read/write,
  editable check invalidation/undo, explicit commit, history, selection/search,
  column options, multi-Parcel editor; 23 selected rows sent as20+3 commands with
  distinct idempotency keys/current ETags; sticky-column geometry and no document
  overflow at360/1440px. Additional existing Business dashboard/switcher9 cases
  PASS during this checkpoint. These intercepted API UI tests are not live DB
  evidence.
- `npm.cmd run lint`, `npm.cmd run build`, `npm.cmd run build:customer`: PASS;
  TypeScript is included in both builds. Existing Customer application bundle
  remains above Vite's500kB warning threshold; warning was not disabled. Excel is
  a separate same-origin Worker bundle, not a remote script.
- Real integration command: `.demo/business-shipping-review/run-focused.ps1
  -Tests IdentityGrantApplicationTests#customerEntryBrowserUsesActualHttpsPostgresAndSmtp`.
  JUnit1/1, Browser16/16 PASS. The Business case opens CSV, exports/reopens XLSX
  through the actual browser Worker, validates through actual HTTPS/Orders/PG18,
  explicitly creates one real Organization Draft from the valid row, resolves
  lost preview/commit responses without repeat writes, reloads persisted outcomes
  and keeps Personal facts isolated. Existing Identity/Personal/Address Book/
  Support/Label/Station/UNPAID scenarios also PASS. SMTP is the test transport;
  no Gmail/AWS live verification is claimed by this UI change.
- Private synthetic integration evidence:
  `.demo/business-shipping-review/pg-e696775e-4088-4b72-80cf-bf749258de39`.
  Reviewed synthetic desktop/mobile/real-HTTP images under
  `.demo/business-import-grid`. No actual Customer PII is used in fixtures.
- `python -B docs/reconciliation/v3/validate.py --implementation`: errors[],
  semantic ownership CONSISTENT. Source manifest includes new helpers/tests and
  dependency files. `git diff --check`: PASS.
- Preserved122 original WIP paths, missing0; all existing Backend source and
  applied migration files unchanged from this checkpoint's SHA-256 inventory.
  Testcontainers used a disposable test database, not the Owner's database.
  `scripts/dev/local.cmd status`: LOCAL_FULL_STACK_HTTP_READY.
- Official pinned SheetJS0.20.3 tarball used for local XLSX parsing. Npm audit
  reported7 pre-existing moderate development-chain findings, none for xlsx;
  no unrelated dependency auto-upgrade was performed. No hosted CI, commit, push
  or PR is claimed for this uncommitted checkpoint.

## Owner usage

Open the existing localhost Customer site, switch to NamShop Demo and choose
**Nhập file**. Download an Excel or CSV template; keep header names unchanged.
Enter source reference, sender/recipient and measured Parcel facts. Open the
file, correct cells (or paste a rectangle from Excel), manage up to5 Parcels per
order and choose **Kiểm tra dữ liệu**. Select valid rows, then explicitly choose
**Lưu … dòng thành nháp**. Open saved Drafts to continue. A change requires a new
Backend preview; undo does not restore authority of an old check.

Import history is authorized by the current Organization/facility scope. An
invalid row reloaded from history has empty cells because rejected raw content
was intentionally not retained. Reopen the original file to recover those cells.
No filename or original-file archive is falsely represented as server storage.

Remaining Business dependencies are unchanged: facilities/defaults/templates/
packing and the exact confirmed Merchant pricing/Booking amendment. The worksheet
does not activate COD, payment, Pickup, Booking or Carrier acceptance. Legacy XLS,
macros, formulas, hidden/merged cells and arbitrary ViettelPost templates are not
accepted as ArgoPost import contracts.
