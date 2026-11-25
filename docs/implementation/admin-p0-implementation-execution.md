# Administration Console P0 — implementation and review evidence

Date: 2026-11-23. Owner directive: INTERNAL ADMIN CONSOLE P0 CONTRACT & FUNCTIONAL
COMPLETION. Branch `feature/argopost-admin-dashboard`, HEAD
`248d651951a1d3f70a5d15e73ccfa76b9877993d` unchanged. No commit/push/merge/deployment.

## Scope and outcome

Existing-contract READ refinement: **SOURCE/TEST PASS**. New Merchant governance,
step-up, workflow, activation and metadata v2: **PROPOSED / TECHNICAL APPROVAL
REQUIRED**, not implemented or functionally accepted. No new Backend production
source, SQL file, table, grant, operator membership or live data was changed.

See [P0 readiness](admin-p0-contract-readiness.md),
[exact manifest](admin-p0-implementation-manifest.proposed.json),
[Identity](admin-p0-identity-context-contract.proposed.md),
[Merchant](admin-p0-merchant-approval-contract.proposed.md),
[metrics/readers](admin-p0-dashboard-supporting-read-contract.proposed.md).
Parent Admin/AON/deployment approvals have not been edited or reinterpreted.

## Preserved baseline and actual incremental changes

Before edits:27 WIP files archived with per-file hashes and all39 SQL hashes in
`.demo/admin-p0-preflight-17cf057a322b41acbde1fcc962720fa3/preservation.json`.
Archive `wip.zip` SHA256:
`29235b10bc4cbc060ecc566718345c3e3008209911afdee8761587e7430baa75`.
This captures the previous shell/restructuring WIP without resetting/stashing it.
Original51-entry AON archive remains separate and retained.

Incremental source/tests in this checkpoint:

| File under app/frontend/web | Change and purpose |
|---|---|
| src/admin/Dashboard.tsx | Spark label uses actual DAY/WEEK/MONTH grain; permitted tabs have Arrow/Home/End navigation, roving tabIndex and named tabpanel |
| src/admin/data.ts | Require exact canonical calendar bucket boundaries, not merely contiguous points carrying a requested grain label |
| ui-unit-tests/support/admin-data.ts | Intercepted UI fixture generates actual clipped Monday-week/calendar-month buckets |
| unit-tests/admin-dashboard.spec.ts | Negative relabeled-day series; partial first/last week and month regression |
| ui-unit-tests/admin-dashboard.spec.ts | Actual grain labels and keyboard navigation only among permitted tabs, preserving filters and avoiding unrelated data requests |
| runtime-tests/admin-dashboard.spec.ts | Existing source-backed PG18/HTTPS Browser tests now exercise WEEK/MONTH and keyboard navigation; await actual return to overview before selecting grain |

No accepted wire field/query/permission is added by these source changes. The
parser rejects malformed buckets under the existing Backend calendar contract;
real PG-backed tests prove valid WEEK/MONTH output remains accepted.

Documentation: five new P0 design/readiness/execution Markdown files, one proposed JSON
manifest, links/status updates in the original amendment/role mapping,23-row
coverage, traceability and implementation-scope inventory. New designs do not
rewrite ADR0018 or the accepted Admin/AON contracts.

## TDD and verification actually executed

| Command / gate | Observed result |
|---|---|
| New2 UI regression cases before implementation | RED2/2: daily-only label and absent keyboard tab behavior |
| New unit calendar regression before fix |6 PASS /2 FAIL: relabeled daily buckets incorrectly accepted; old synthetic fixture still emitted daily intervals |
| `npm.cmd run test -- admin-dashboard.spec.ts admin-session.spec.ts admin-invitation.spec.ts internal-navigation.spec.ts staff-authority.spec.ts` |32/32 PASS after implementation |
| `test:customer-ui` Admin dashboard/login/activation suites (15+7+2 cases) |24/24 PASS; intercepted transport UI tests, not live Backend evidence |
| `npm.cmd run lint` |PASS, exit0 |
| `npm.cmd run build:customer` |PASS, includes TypeScript; existing bundle-size advisory retained |
| `npm.cmd run build` |PASS, includes TypeScript; default build produced165 modules without warning |
| PG18 focused selector below |Initial2 PASS /1 FAIL; failure and rerun recorded below |
| Corrected real Admin HTTPS/PG18 Browser gate |JUnit1/1 PASS with5/5 real Browser cases, no skips/failures |

Focused command:

```powershell
.\scripts\ci\run-cskh-focused.ps1 -Tests 'IdentityGrantApplicationTests#adminDashboardRealHttpsBrowserAndPostgres+adminDashboardGranularPermissionIsolation+adminDashboardSnapshotTamperReplayAndDrift' -Offline
```

Initial evidence `.demo/cskh-focused-e08a06a407144ec3a33856c657488116/`:
granular permission isolation and snapshot tamper/replay/drift PASS. The Browser
case failed when its next step changed the shared grain filter while React was
still on Shipment Oversight after clicking Back. Diagnostic snapshot showed a
correct list screen; it was a test synchronization error, not wrong KPI buckets.
The first2 Browser cases passed and later serial cases did not run in that attempt.

Correction waits for `/admin?` URL and the overview heading before changing grain.
No timeout inflation, assertion weakening, Backend fallback or data mutation fix.
Only the affected Browser case was rerun:

```powershell
.\scripts\ci\run-cskh-focused.ps1 -Tests 'IdentityGrantApplicationTests#adminDashboardRealHttpsBrowserAndPostgres' -Offline
```

Final evidence `.demo/cskh-focused-803fccc757ac48f5a7ee7b94cc3eb60f/summary.json`:
Maven exit0; tests1/passed1/failed0/errors0/skipped0; sourceUnchanged=true;
source snapshot SHA256
`cf14ba0ad7fbfd3ee969edb4211304d1d5c4168abbe8e37026fc322d1f573256`.
Sanitized `safe-browser-results.json` records5/5:

1. Invitation/verification through HTTPS and controlled test SMTP.
2. Actual SYSTEM HTTPS login and confirmed PG Shipment counts/UNPAID amounts.
3. Exact contributing Shipment snapshot/drill-through; WEEK/MONTH and keyboard tabs.
4. Cookie refresh and mobile viewport.
5. Logout and unauthenticated API denial.

This is an isolated test fixture and controlled SMTP transport, **not Gmail live
delivery or a new deployment/UAT on the Owner's approved real target**. No Customer,
Staff, Root or Admin credentials/proofs/private keys are copied into this report.
Private screenshots/logs remain outside Git. Existing architecture/aggregation
28/28 and earlier Staff Browser evidence remain accepted historical evidence;
they were not rerun without an affected Backend change. No full H2/PG18 suite,
current GitHub CI or new real-target UAT PASS is claimed.

## New authority: required gates have not run

P0-T-01–15 in the manifest are all NOT_RUN_NOT_IMPLEMENTED. They include migration/
ACL/direct-DML tests, scoped grants/revocation, actual Customer contact proof and
screened evidence, immutable resubmission, independent decision/conflicts,
replay/ETag/proof races, atomic activation rollback, unresolved legacy key denial,
suspension regression, v1/v2 snapshots/metadata and source-backed end-to-end flow.
No mock PASS or JSON inventory check substitutes for these gates.

V41/V42/V43 are proposed unoccupied candidates only. No files created, no migration
applied, no default policy publication, no role provisioning or real permission
grant. Consent text/jurisdiction policy and legacy-key clearance remain explicit
future activation prerequisites. Internal context linking and new runtime role
templates are excluded.

## Coverage disposition

All23 module rows and original product requirements retained. Counts remain
8 PARTIAL /5 BLOCKED /3 UNAVAILABLE /1 MISSING /6 PROPOSED. No new complete module.
ADM-01/02/03/08/12/16/17 now link the bounded P0 contract dependencies and actual
current READ tests. Remaining platform/network/SLA/claims/Finance/privacy sources
remain explicit backlog, not synthetic data or new execution authority.

The single next approval is the exact consolidated technical manifest and its
listed policy/amendment deltas. Existing READ/UI approvals need no repetition.
Implementation and separate exact-target deployment remain later checkpoints.

## Final inventory and preservation checks

Final proposed manifest SHA256:
`90c795bd7e773b6a58d0d220056cfbc5e9a5ee95b38ae2493d5b9f75a372291f`.
Inventory:32 API records (31 new +1 existing receipt extension),6 existing opt-in
metric paths,49 DTO projections,7 public ports,10 Merchant capabilities,15 tables,
9 bounded existing-object amendments,5 SQL functions and2 proposed NOLOGIN roles.
Existing CustomerAdmission.Subject already carries verifiedEmail/eligibility, so
the proposal reuses it instead of adding an Identity recipient-discovery port.

Document consistency checks PASS: unique IDs/routes, resolved API/DTO references,
proposed-only new surfaces, no linking/Identity codes/runtime role templates,
explicit scope matrix and unoccupied migration candidates. This checks design
inventory only, not PostgreSQL behavior or business acceptance.

`python docs/reconciliation/v3/validate.py --implementation`: final PASS, ownership
CONSISTENT,0 unresolved ownership and10 previously broken mobile-path links. Initial
documentation validation found the execution report not yet created, then an
ASCII pipe encoding replacement in the generated review index; both corrected,
no validator rule disabled. `git diff --check` PASS; Git emits existing LF/CRLF
normalization notices, no whitespace errors.

Final `.demo/admin-p0-preflight-17cf057a322b41acbde1fcc962720fa3/final-preservation.json`:
HEAD unchanged, staged empty, all39 SQL paths and SHA256 unchanged, all27 initial
WIP file bytes preserved in the preflight archive with valid CRC/hash; original
51-entry archive CRC/hash unchanged. Approved Admin/AON/UAT artifacts retain their
exact prior hashes. No Backend production source diff. Final Git inventory36
working paths includes prior work and this checkpoint; no deletion/reset/clean.

Current blockers are the unapproved new technical authority and later runtime
policy prerequisites, not a failing existing READ test. All proposed P0-T-01–15
remain NOT RUN. No assertion of full-console completion, full-suite/CI PASS or
new real-target acceptance follows from this report.
