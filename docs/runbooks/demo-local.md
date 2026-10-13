# Demo local — Auth / Merchant / Shipment DRAFT

Scope: [demo spec](../../specs/demo-001-auth-merchant-shipment/spec.md), [contract/design](../../specs/demo-001-auth-merchant-shipment/design.md), [evidence](../implementation/demo-001-evidence.md). This is not P0 or D1-03 closure.

## Run
Use two terminals. Stop the demo Java process before rebuilding its JAR on Windows; a running JAR can block Spring Boot repackage. Do not stop the native PostgreSQL service.

Backend (from repository root):
```powershell
cd app/backend
$demoMavenRepo = Join-Path (Resolve-Path '../..').Path '.demo/maven-repository'
.\mvnw.cmd -B -ntp "-Dmaven.repo.local=$demoMavenRepo" verify
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\run-demo.ps1
```
ExecutionPolicy Bypass above is for that process only; it does not change machine/user policy. The launcher disables inherited DEBUG/TRACE via command arguments, binds localhost:8080 and uses profile demo. No Docker required.

Frontend (from repository root):
```powershell
cd app/frontend/web
$demoNpmCache = Join-Path (Resolve-Path '../../..').Path '.demo/npm-cache'
npm.cmd ci --cache $demoNpmCache
npm.cmd run dev
```
Open **http://127.0.0.1:5173/**. Production bundle can be checked with `npm.cmd run build`, then `npm.cmd run preview` (localhost:4173; backend still required).

## Accounts and credentials
The launcher generates `.demo/settings.json` only when absent. It is ignored by Git and contains private local credentials; open it locally, do not copy it into source or screenshots. Run with `-InitializeOnly` to prepare credentials without starting backend.

| Username | Password property in local settings | Access |
|---|---|---|
| demo.admin | adminPassword | Account/role overview, shipment summaries |
| demo.merchant.a | merchantAPassword | Organization A profile and own drafts |
| demo.merchant.b | merchantBPassword | Organization B profile and own drafts |
| demo.operator | operatorPassword | WAREHOUSE_OPERATOR; current-user only |
| demo.disabled | disabledPassword | Disabled SHIPPER; login rejected |

Seed creates missing accounts/merchants, does not reset existing passwords or roles. Keep settings together with its demo DB; generating new settings does not reset persisted account passwords. There is no public registration or role-selection API. Other supported role values are DISPATCHER and FINANCE; this demo grants them no business endpoints.

## Database and configuration
No PostgreSQL credentials were configured during implementation, so the explicitly authorized H2 file fallback was selected. Data is in `.demo/data/elogistics.mv.db` on the repository drive (D on this machine). Migration: `app/backend/src/main/resources/db/demo-migration/V1__demo_identity_merchant_drafts.sql`; history in H2 public schema. Schemas identity/merchant/orders isolate module ownership; no cross-module table queries.
Default profile excludes datasource/Flyway autoconfiguration; demo profile alone enables them. Five tables: demo_account, demo_merchant, demo_shipment, demo_event, demo_operation. Demo events are append-only through application APIs. Runtime and migrations share H2 local owner credentials; production permission isolation is not implemented.
Do not point this demo profile/migration at an existing PostgreSQL database. Do not delete data, caches or settings to repair failures. PostgreSQL foundation requires separately reviewed migrations/roles/version and real PostgreSQL tests.

## Test
Backend: `mvnw.cmd verify` as above; after dependencies resolve, `-o` uses the D cache. Reports: `app/backend/target/surefire-reports/`.
Web: `npm.cmd run typecheck`, `npm.cmd run build`. With backend and Vite running and local settings initialized:
```powershell
$demoTemp = Join-Path (Resolve-Path '../../..').Path '.demo/tmp'
New-Item -ItemType Directory -Path $demoTemp -Force | Out-Null
$env:TEMP = $demoTemp
$env:TMP = $demoTemp
npm.cmd run test:e2e
```
Browser tests use installed Microsoft Edge; no browser/SDK download. Artifacts under `.demo/`. Browser automation reads local passwords without printing them; traces/videos are disabled.

Persistence proof from repository root:
```powershell
python app/backend/smoke-demo.py create
# Stop only the demo backend, then start run-demo.ps1 again with unchanged settings/data.
python app/backend/smoke-demo.py verify
```
The check confirms the same shipment ID/created timestamp/event count and persistent idempotency receipt, plus isolation from Merchant B. It intentionally leaves synthetic demo rows intact.

## Instructor walkthrough (5–7 minutes)
1. Log in as Merchant A. Show merchant scope and dashboard populated from APIs.
2. Create a unique reference such as `CLASS-DEMO-001`, synthetic contact/address/parcel, weight 1.25 kg. Save and open detail; show DRAFT and creation history.
3. Return to list and reopen detail. Explain that quote/confirmation/fee/reservation are not implemented.
4. Log out, log in as Merchant B. A's reference is absent. Navigate to the previous detail hash while logged in: 404; no object disclosure.
5. Log in again as A and cancel with a reason. Show CANCELLED plus preserved creation/cancellation events; no confirmation/delivery shortcut.
6. Log in as ADMIN. Show aggregated counts and user/role/enabled overview; no Finance mutation or merchant create permission.
7. Show backend/browser test summary and saved persistence proof. State the demo uses real H2 file persistence, not PostgreSQL acceptance.

## Limits
Access tokens expire after 15 minutes, with standard validator clock skew; account enabled/role/scope reloaded for every request. Browser token is memory-only (page reload requires login). Logout clears browser state, does not revoke issued tokens. Refresh/revocation/reset/multiple roles/scopes/production TLS are deferred.
Login throttle is process-local, five failures per IP+username in five minutes, capped memory; restart clears it and no distributed protection is claimed.
Addresses are unverified draft input; no administrative catalog/geocoding or confirmed snapshot. One parcel, no COD, pricing, WMS, Finance, tracking or shipping transitions. Recent lists cap at 200; no pagination yet.
Boot-managed H2 2.4.240 triggers Flyway's warning that its latest verified H2 is 2.3.232. Actual demo migrate/validate/rerun/tests pass; this does not certify full version compatibility or PostgreSQL behavior. Java/Boot/Maven versions were unchanged.
