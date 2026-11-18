# Organization Support — technical manifest / checkpoint

**Subsequent Owner decision:** exact manifest SHA-256
`9484c415356e601ee8192e84a09af2e39f2f448d07244457de1ef79a09c3a940`
is technically approved for source/test execution. The JSON remains frozen;
historical pending statements below describe the pre-approval checkpoint.
Current implementation, V35 allocation, compatible refinements and actual test
evidence are in [organization-support-execution.md](organization-support-execution.md).
No Support localhost deployment/grant or Production authorization is implied.

Date2026-11-17. Current branch `feature/argopost-business-shipping-tools`, baseline
c8cdfa3. No new worktree, commit/push or merge in this checkpoint.

## Authority split — Owner GO WITH CONDITIONS

V1 BUSINESS scope is approved: Organization-owned Ticket, scoped Membership/RBAC,
Merchant–CSKH conversation, internal responsibility/history, filters, actual
response timestamps and committed-event in-app notices. Mandatory separation of
Merchant and Carrier authority remains binding.

Owner explicitly said business approval does **not** approve all proposed exact
API/DTO/schema/permission items. Therefore OSC-03a is DONE, OSC-03b is pending.
This checkpoint supplies the requested concrete manifest, not another business
scope approval request. [Contract](organization-support-contract.proposed.md) ·
[Exact JSON manifest](organization-support-technical-manifest.json).

| Manifest IDs | Exact proposed inventory | Current disposition |
| --- | --- | --- |
| existing_reuse | Current Identity/TLS/CSRF, BusinessWorkspaces admission, immutable Organization Shipment read, OSM reads, canonical Support rules/key ring | REUSE EXISTING ACCEPTED CONTRACT; no authority expansion |
| OSC-API-01–23 |23 new HTTP operations, each with method/path/request/response/current authority/version/idempotency definition | PENDING_EXPLICIT_TECHNICAL_CONFIRMATION |
| OSC-DTO-01–28 |28 exact request/response/filter/report/notice shapes, enums/limits and server-derived fields | PENDING_EXPLICIT_TECHNICAL_CONFIRMATION |
| OSC-PERM-01–05 | support.read, support.write, support.assign, support.reports.read, support.notifications.manage | PENDING_EXPLICIT_TECHNICAL_CONFIRMATION; no grants |
| OSC-REUSE-STAFF-01–02 | Existing SUPPORT_TRIAGE/HANDLE codes; additional explicit Organization binding to the actual canonical grant | Existing codes reused; NEW binding still pending; no automatic authority |
| OSC-DB-01–12 |12 Orders-owned Ticket/message/internal+CSKH assignment/binding/receipt/audit/quota/preferences/notice/operator-audit relations | PENDING_EXPLICIT_TECHNICAL_CONFIRMATION; no DDL created/applied |
| OSC-AMEND-01–04 | Exact Merchant permission/scope/operator allowlists; additive canonical Staff-grant composite key for scope FK only | PENDING_EXPLICIT_TECHNICAL_CONFIRMATION; existing grant codes/rows/uniqueness and Personal semantics preserved |
| OSC-OPERATOR-01 | Offline bind/unbind function for an explicit existing canonical Support Staff grant + Organization; no runtime execution or global grant creation | PENDING_EXPLICIT_TECHNICAL_CONFIRMATION |
| OSC-CONFIG-01 | Default-false organization-enabled flag AND existing Support Local/Test/SANDBOX/non-Production guards | PENDING_EXPLICIT_TECHNICAL_CONFIRMATION; not activated |
| OSC-MODULE-01–03 | Scoped member discovery, typed Organization Support API, shared pure lifecycle policy | PENDING_EXPLICIT_TECHNICAL_CONFIRMATION; same canonical Support/Identity authority |

The JSON lists each field/column and resource binding, not just these counts.
Read-only search is a POST with literal search text in the body to avoid putting
subjects/PII into URLs; it requires Origin/CSRF but has no command side effect,
receipt, quota write or session activity extension. Staff receipt operations and
their routes are purpose-separated from Merchant internal assignment receipts.

Staff scope is a restriction on the existing canonical grant, never a second
grant engine. Existing Personal Support grants alone cannot open Organization
cases. Merchant assignment never changes that grant or the CSKH assignment.

## Implemented independent slice — OSC-02 LOCAL DONE

- New Business root menu and `/merchant/{org}/support` hub, searchable7 factual
  guides and separate Ticket / change / Claim explanations. No fake Ticket counts,
  form submission, resolution, SLA or compensation success.
- Shipment Detail support tab links the exact admitted Organization/Shipment to
  this hub. Existing BSC-06 GET checks ownership/current resource scope; the hub
  shows only code/time/Parcel count/route/current source state, not contacts.
- Safe same-Organization return preserves the selected support view and Shipment.
  Duplicate/malformed/foreign selectors do not become Personal fallback queries.
- Orange Business styling, keyboard/native details and390/1440px visual checks.

Changed source: `OrganizationSupport.tsx`, `organization-support.ts/.css`,
`BusinessWorkspace.tsx`, `WorkspaceShell.tsx`, `BusinessShipmentTabs.tsx`,
`BusinessShipments.tsx`, `organization-management.ts`; focused unit/UI fixtures,
real Browser journey and test-host SPA forward. No Backend production API,
Identity, SQL migration, permission or existing grant implementation changed.
Source manifest/traceability updated; prior19 source paths remain SHA-256
preserved in the ignored checkpoint even where shared navigation was extended.

## Observed tests / environment

- `npm.cmd --prefix app/frontend/web test -- organization-support.spec.ts organization-management.spec.ts`:
  **12/12 PASS** (two new support query/return units + existing scoped Management).
- `npm.cmd --prefix app/frontend/web run test:customer-ui -- organization-support.spec.ts business-ui.spec.ts`:
  **15/15 PASS** (six Support hub + nine existing Business cases). Three initial
  failures were incorrect test assumptions that the existing menu made no read
  requests; corrected to assert no Support writes/resource queries on invalid
  input and only same-Org GETs. No production security assertion was weakened.
- Earlier affected Shipping Tools UI regression **9/9 PASS** in the combined run;
  no fake one-run full-UI acceptance claim.
- Typecheck, lint and default + Customer build PASS. Existing Customer bundle-size
  advisory remains; it is not a failed gate or a new Support authority blocker.
- Focused PostgreSQL parent
  `IdentityGrantApplicationTests#customerEntryBrowserUsesActualHttpsPostgresAndSmtp`:
  **1/1 PASS,0 skipped**; **18/18 actual HTTPS/PG18/test-SMTP Browser journeys PASS**.
  The Business journey now opens Shipment Support → exact Organization hub →
  Claim guidance → actual Shipment tracking → safe support return, then continues
  existing labels/preparation/handover. Real API/owned PG sources; no fulfilled
  HTTP fixture, real-account provisioning or live Gmail claim.
- Credential privacy PASS. Independent recorded-suite observed/verified/stopped
  identity bindings and read-only container/volume/reaper absence PASS; no cleanup
  operation against developer data. Previous19-file backup SHA-256 PASS.
- Official launcher: LOCAL_FULL_STACK_HTTP_READY. No restart/init, migrate, grant,
  certificate/secret change, reset/clean or data overwrite performed.
- Current source stream has32 migrations. **V33 not created and not executed**;
  therefore new V33 integrity/rollback/authorization/Personal-regression gates are
  NOT_RUN_AWAITING_TECHNICAL_CONFIRMATION, not PASS. Backend production/migration
  Git diff is empty. No new hosted CI/PR is claimed.

Private raw tests and synthetic visuals are ignored under
`.demo/organization-support/`. No real PII, tokens or credentials enter Git.

## Remaining gate / next implementation

Only OSC-03b blocks the new V1 runtime: exact technical item confirmation.
After explicit confirmation, implement OSC-04/05 on disposable PostgreSQL18 and
prove OSC-06, without another V1 BUSINESS approval. V1–V32 remain immutable;
localhost migration/grants still need the separate deployment disposition.

Claims/Compensation, Shipment mutation, Evidence Upload and Finance/Carrier
shipping operations are outside this V1 approval. Their absence is explicit and
must not be hidden by the hub or presented as a completed Organization Ticket.

Manifest SHA-256: `9484c415356e601ee8192e84a09af2e39f2f448d07244457de1ef79a09c3a940`. All new technical items remain pending; this digest
binds the concrete review artifact, not an approval or deployment receipt.
