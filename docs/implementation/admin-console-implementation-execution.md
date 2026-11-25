# Internal Administration Console implementation evidence

Owner directive2026-11-23. Branch feature/argopost-admin-dashboard, starting
HEAD248d651. This checkpoint does not authorize commit/push/merge or deployment.

## Tasks and current evidence

- [x] IW-A01: fully extract/read Document A/B and preserve exact originals, HEAD
  source and migration fingerprints. See architecture input hashes.
- [x] IW-A02: reconcile23 target modules, original17 groups,9 parents and role
  collisions; see coverage CSV and role mapping. Do not confuse old ADM-02–08
  implementation task IDs with Document B business module IDs.
- [x] IW-B01: common InternalApp/InternalShell, actual Admin and Staff consumers;
  route/permission/source-state, keyboard and responsive gates PASS below.
- [x] IW-C01: existing-contract slice only: six Dashboard tabs, current read-backed
  oversight/fees/platform/account and preserved filters/snapshots/auth. Full
  ADM-01 business-source coverage remains PARTIAL, not DONE.
- [x] IW-D01: frontend/build, real HTTPS/PG18 Browser and current Backend negative
  authorization regressions PASS; migration/source/WIP integrity verified.
- [x] IW-E01: one consolidated unapproved contract amendment, no mutation outside
  existing contracts; no separate repeated approval for already authorized work.

## Actual source changes

Frontend only: InternalApp, InternalShell, internal navigation/design CSS,
OperationsBoundary, Admin ConsoleViews, AdminApp/Dashboard, StaffApp composition
and root route dispatch. Existing session.ts, staff-client.ts, Backend contracts,
security engines and SQL stay unchanged. Unknown region/scope URL parameters are
not silently accepted; denied modules do not trigger extra privileged sources.

Real read slices use the existing context/dashboard/shipments/platform APIs.
Unsupported Merchant/Network/Decisions show exact dependencies, no fake records.
Own Account displays existing context/grants and logout, no admin grant control.
CSKH retains real Personal/Organization Ticket and Claim intake workflows under
the finance shell, with independent Staff Backend authorization. This is not
Finance ledger execution or Admin Claims Governance.

## Test execution log

New navigation tests were written first; initial command could not import the
not-yet-created navigation module (RED discovery, zero cases executed). After
implementation, five unit cases PASS. Initial typecheck and lint PASS.

First scoped intercepted Browser run:18 PASS,2 FAIL. One new deep-link test
incorrectly required a query suffix even with no explicit filter; corrected to
assert preservation of actual from/to filters. Existing title assertion still
expected old branding; updated to Administration Console. No security assertion
was removed or relaxed. That affected rerun passed20/20.

The next final-composition UI run passed19/20: duplicate-login fixture ended before
its new shell assets finished loading, causing `Fetch response has been disposed`
in route.fulfill during teardown. Added afterEach network-idle synchronization,
retaining the one-login-request assertion. The isolated PG18 run passed7/8: the
mobile drawer backdrop's default click point was behind the sidebar. Its visible
click area is now restricted to the region outside the sidebar, with a regression
test. Two focused HTTPS/PG18 cases then passed2/2, including all5 Admin Browser
cases and1 Staff Browser case. No retries or skips were configured to hide failure.

Visual review found offscreen skip-link painting during full-page capture and
selects losing their old ap-root styling. Scoped clipping/focus and form styling
were corrected; a keyboard-to-main regression was added. Final22 UI cases and a
final isolated Admin HTTPS/PG18 rerun passed. Desktop1280px and mobile390px PNGs
were inspected: no horizontal overflow, clipped business content or stray skip
link; the drawer opens/closes and keyboard skip navigation reaches main.

Browser fixture tests are not live Backend evidence. Existing real Local UAT23/23
and V39/V40 source/test acceptance are retained historical evidence; this source
checkpoint must separately validate its changed UI with isolated runtime fixtures.
Do not overwrite the old UAT readiness record with an unexecuted new deployment.

## Coverage disposition

All23 modules have source/dependency/status rows. Unavailable screens and disabled
routes are navigation/dependency handling only. Full ADM-01 requires missing
Merchant/network/SLA/Finance/decision sources and metadata/scope amendments.
Cross-portal switching, proposed extensions and Production are not READY.

## Final executed commands and evidence

Commands from `app/frontend/web`, unless stated otherwise:

| Command / gate | Observed result |
|---|---|
| `npm.cmd run typecheck` | PASS |
| `npm.cmd run lint` | PASS, zero warnings |
| `npm.cmd run build` | PASS |
| `npm.cmd run build:customer` | PASS; existing large-bundle advisory remains, no suppression |
| `npm.cmd run test -- admin-dashboard.spec.ts admin-session.spec.ts admin-invitation.spec.ts internal-navigation.spec.ts staff-authority.spec.ts` |30/30 PASS |
| `npm.cmd run test:customer-ui -- admin-dashboard.spec.ts admin-login.spec.ts admin-activation.spec.ts` | Final22/22 PASS; fixtures, not Backend acceptance |
| `scripts/ci/run-cskh-focused.ps1 -Tests 'ArchitectureTests,ArchitectureNegativeFixtureTests,AdminDashboardTests' -Offline` from repo root |28/28 PASS; Maven0 |
| Initial eight-case PG18 command below |7 PASS,1 FAIL; failure preserved above |
| Focused Admin+Staff HTTPS PG18 after backdrop fix |2/2 PASS; Maven0;5+1 Browser cases PASS |
| Final `IdentityGrantApplicationTests#adminDashboardRealHttpsBrowserAndPostgres` after visual corrections |1/1 PASS;5 Browser cases PASS; Maven0 |
| `python docs/reconciliation/v3/validate.py --implementation` | PASS; ownership CONSISTENT;10 preexisting broken links retained |
| `git diff --check` and preservation checks | PASS; no staged paths; HEAD unchanged |

Initial PG18 selector, using existing owned Testcontainers lifecycle and runtime
roles, was `IdentityGrantApplicationTests#adminDashboardSixApisAndSystemAdmission+adminDashboardGranularPermissionIsolation+adminDashboardDefaultOffAndRevocation+adminDashboardSnapshotTamperReplayAndDrift+adminDashboardPartialRevocationAndAudience+adminDashboardCanonicalCohortAndPii+adminDashboardRealHttpsBrowserAndPostgres+staffAuthorityBrowserUsesActualHttpsPostgresAndSmtp`.
All eight distinct cases have a passing relevant run after corrections. This is
not a claim of full Backend H2/full PostgreSQL regression or GitHub CI PASS. No
Backend production or SQL changes required another broad suite. Staff's Browser
host has no Admin controller, so its404 check is only missing-route isolation;
actual foreign Staff-audience401 enforcement is covered by the separate Admin
HTTP/PostgreSQL partial-revocation/audience test.

Private evidence under `.demo/` (excluded from Git):

- `cskh-focused-2484775a4f6245d8b02ba4fcd77e4f1e/summary.json`: initial8 and first
  failure; Admin Browser3 PASS, mobile timeout, following serial logout skipped.
- `cskh-focused-b9c75cdb3d8c4f4580d63a165bb6ad96/summary.json`: corrected2/2,
  Admin5/5 and Staff1/1 Browser results. Both use actual HTTPS, real Spring
  endpoints/PostgreSQL and owned SMTP test transport, without intercepted APIs.
- `cskh-focused-31cf084f9c754b30a3738623b0a730aa/summary.json`: architecture/unit28.
- `cskh-focused-f2370693fe6c43bfb7383d9fcbe8a6ff/summary.json`: final Admin1/1 and
  Browser5/5. Final app/test source fingerprint equals architecture run fingerprint
  `1e633a70e8f7ac24a9647093b3cfde268b915eb0bc3a1e0769e5c95043080b69`;
  both runners report `SOURCE_UNCHANGED=True`.
- `admin-console-preflight-59c74427a4154ade8f0157f0e2797cd2/`: preserved HEAD/input
  ZIPs, document extraction, CSV authoring/inspection, validator, migration
  fingerprints, final integrity and final desktop/mobile PNGs.

Private fixtures use synthetic accounts/data only. No real Gmail, private keys,
tokens, invitation proofs, CSRF values, account passwords or DB credentials are
included in these published documents or the CSV.

## Integrity and scope

Both original DOCX hashes and all39 current migration-file hashes match preflight,
including all37 historical migrations. V33 remains absent. The original51-entry
WIP ZIP remains byte-identical: SHA256
`01aceab2ee1927b3a429e12c2c573cd1ff66eeaf9e1b76f273d8d626c83c8626`, CRC PASS.
Approved Dashboard/AON/UAT manifest hashes match the architecture references.
HEAD remains248d651951a1d3f70a5d15e73ccfa76b9877993d. No database demo/shared/UAT
mutation, process restart, grants, SQL, production Backend source, auth client,
historical evidence or approved immutable artifact was changed. Only two Backend
test hosts gained static SPA deep-link paths to exercise real routing.

Source/test/traceability changes are uncommitted. There is no new commit, push,
merge or current-revision GitHub CI result in this checkpoint.

## Readiness and next dependency

| Acceptance target | Verdict |
|---|---|
| Shared Internal Shell | **READY — SOURCE/TEST**, consumed by actual Admin and STAFF/SUPPORT UI |
| ADM-01 existing approved read slice | **READY — SOURCE/TEST**; original real UAT evidence retained, new source not deployed |
| Full ADM-01 in Document B | **PARTIAL**: Merchant/network/SLA/decision/advanced Finance/telemetry and exact metadata/scope missing |
| Administration Console all23 modules | **PARTIAL**:8 PARTIAL,5 BLOCKED,3 UNAVAILABLE,1 MISSING,6 PROPOSED;0 complete modules against full target |
| Cross-Portal Integration | **BLOCKED**: no approved discovery/target-admission contract; selector never unions grants |
| Production | **NOT READY**, no activation or deployment authorized |

Single Owner checkpoint: review the [consolidated amendment](admin-console-contract-amendment.proposed.md).
P0 implementation should next establish approved scope/context and Merchant
application submission/review/activation, together with scoped oversight/IAM/audit;
P1–P3 dependencies follow Document B's priority mapping. No unavailable module is
marked complete simply because its sidebar item or dependency view exists.
