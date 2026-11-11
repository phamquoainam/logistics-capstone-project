# Business Dashboard — implementation checkpoint, 2026-11-10

Owner accepted [BUSINESS-LOCAL-ACCESS-01](business-dashboard-local-contract.proposed.md).
BD-01–05 implementation DONE after the observed gates below. Owner subsequently
confirmed the exact target Account UUID and approved NamShop Demo Local/Test
provisioning. Local migration/provisioning and HTTPS readiness are now verified;
Owner confirmed the actual authenticated switcher/Dashboard review. No account was
selected or inferred from DB. Exact target/org/request identifiers remain only
in the ignored local operator evidence, not in committed source or fixtures.

## Delivered

- V23 additive organization/membership/explicit grants, operator audit/receipt and
  DB initial-owner/permission/provenance/immutability constraints. No seed or edits
  to V1–V22. Runtime gets SELECT only, no schema/grant/operator-function authority.
- Identity-owned offline eligibility/locking port validates an existing verified
  Customer identifier; Merchant performs no cross-schema Identity reads/writes.
  Same Identity administration/account → Merchant control protocol as current
  CustomerAdmission. Current initial Owner eligibility also gates organization reads.
- Two-stage private owner-reference discovery then current locked admission, not
  a user-supplied actor/context. Requests serialize with grant/revoke/suspend. Both
  caller and initial Owner must remain eligible. No runtime role-label/self grants.
- GET customer/business-contexts and merchant-organizations/{id}/dashboard under
  existing Customer TLS/Bearer/Origin guard, no-store, strict query/dates, org scope.
  Disabled/production profiles fail closed; an enabled missing V23 fails startup.
- Notifications owns dashboard definitions; workflow adapts public Merchant scope
  to public Analytics access. No new notification→merchant dependency or weakened
  architecture rule. No direct Orders/Finance query using Personal membership IDs.
- Backend-connected Business shell, actual context chooser, Personal return,
  permission-redacted finance/team regions, date/day/week/month filters, responsive
  ArgoPost navy/orange design. Missing sources are null/unavailable, not fake zeros,
  trends, revenue, debt, success rates or clickable fake drill-throughs.
- Old org/session results hidden during loading and ignored after context change;
  stale authority, foreign target, cacheable payload and logged-out results rejected.
- Explicit operator script `scripts/dev/business-workspace.ps1`: required request,
  organization/account UUIDs and reason; safe SQL literal quoting, original container
  identity/health, existing local secret load with no password in output/arguments.
  Same RequestId plus exact same input resolves an unknown result idempotently.
  PROVISION/GRANT/REVOKE/SUSPEND only, no Admin approval impersonation or restore.

## Observed gates

- Backend offline compilation PASS using existing Windows Maven cache. Sandboxed
  Maven initially could not read the cached parent; same offline invocation with
  cache access succeeded. No dependency/version workaround.
- Final focused PG18:5 Business HTTP/security/operator/ownership/concurrency cases,
  V22→V23 upgrade/schema guard1 case, architecture23 cases:29/29 PASS.
- Separate earlier PG regression:5 Support cases PASS; migration56-case run55 PASS
  and one exact cold-start version inventory failed because it still ended at22.
  Added23 to that exact inventory and reran the cold-start case:1/1 PASS. No migration
  edit/repair, test skip or assertion relaxation. V1–V22 data/checksums preserved.
- HTTPS + real PG18 + test-SMTP Browser16/16 PASS and parent1/1 PASS. Includes real
  two-organization Membership switching and read-only grant redaction, no Personal
  fact reuse, unsupported action denial, Personal/Ticket/address/Booking/intake/
  UNPAID/label/profile/OTP recovery regression. Fixtures are isolated synthetic DBs.
- Frontend110/110 unit tests PASS; typecheck/lint and both build modes PASS.
  Customer bundle size warning remains; no unrelated bundling refactor.
- Business UI8/8 PASS at360/768/1440px, source failure, date/interval, org/permission
  switching. Latest focused UI13/13 PASS; existing Customer/Account regressions also
  passed in earlier affected runs. These intercepted fixtures are UI unit evidence,
  never substituted for the separate real16-case Browser evidence.
- Report-policy130/130 PASS. Exact reviewed inventory updated to336 Identity cases,
  56 migration cases and1083 expected PostgreSQL cases; no required case removed.
- Visual desktop/mobile synthetic screenshots inspected. Unavailable secondary
  analysis panels compacted; no chart invented from disconnected sources.
- Operator PowerShell AST parse PASS. After explicit target confirmation, the
  accepted operator command provisioned exactly one development Organization and
  one ACTIVE OWNER Membership; exact postconditions are recorded below.
  No Gmail/AWS live verification claimed here.

## Local activation — 2026-11-10

- [x] Owner-confirmed UUID read-only eligibility: existing ACTIVE verified Customer
  with current exact Identity security-email identifier. No other account chosen.
- [x] Before/after SHA-256 fingerprints captured without exporting raw PII.
- [x] Official launcher stopped only its2 owned processes, reused original healthy
  PostgreSQL18.6 and applied V23 through Flyway. No init/reset/repair/volume removal.
  V23 is now deployed and immutable, as are V1–V22.
- [x] NamShop Demo ACTIVE, LOCAL_OPERATOR_PROVISIONED, environment LOCAL; exactly1
  ACTIVE initial OWNER Membership for the approved account,0 other-account members.
  Only dashboard.read/dashboard.finance.read/dashboard.staff.read at ORG scope.
- [x] One PROVISION audit (operator principal, reason, request) and one receipt.
  Runtime cannot invoke provisioning or write grants/membership authority.
- [x] Before/after Identity, identifiers, profiles, addresses, applications,
  Shipments, Parcels and UNPAID obligations have identical SHA-256 fingerprints.
  Existing22 Flyway checksums and target Personal Shipment count unchanged.
- [x] Official start returned LOCAL_FULL_STACK_HTTP_READY. Trusted HTTPS website
  and Business SPA route200; anonymous contexts/dashboard APIs401. No token or
  password extracted and no fabricated authenticated session used for verification.
- [x] The88 non-doc source fingerprints match the tested checkpoint. Reuse observed
  PG18/HTTP Production denial and16-case real Browser results; no redundant suites.
- [x] Owner authenticated browser: Personal ↔ NamShop Demo switcher and source states.
  Owner answered "Switcher và Dashboard hoạt động đúng" to the explicit local
  verification covering both workspaces and no Personal Shipment in Business.
  This is Owner-observed manual acceptance, not an agent browser-automation result.
  Computer-use/browser helpers failed initialization with "failed to write kernel
  assets"; anonymous route200 or DB rows were not substituted for this confirmation.

Private metadata: `.demo/business-dashboard-review/local-before.json`,
`local-after.json`, `local-data-preservation.json`, `local-provisioning.json`,
`local-provision-verified.json`, `local-https.json`, deployed V23 source fingerprint.
No new real-data evidence is added to Git/source archives.

## Deliberately not activated

The real account/organization were provisioned only after exact Owner confirmation;
there is no automatic account seed, public self-grant, legal approval or grant for
other Customers. Existing DB/volumes/certificates/local secrets remain preserved.

Merchant Shipment ownership, service/tariff/payment authority, import/labels/pickup,
Carrier outcome sources, warehouse/task/channel lineage and Merchant Finance/COD
sources remain absent. No existing Personal exception extended to Merchant.
Dashboard source states cannot be presented as live commercial analytics.
Facility-scoped totals are not implemented and cannot broaden to ORG totals.

## Owner current-account UUID, read-only

Preferred: DevTools → Network → existing GET `/api/v1/customer-auth/me` → Response
→ copy only `accountId`. Do not copy cURL/headers/cookies/accessToken.
Do not trigger login/refresh or query all Customers merely to infer the target.

Local Vite alternative, reading the existing in-memory public Subject only:

```javascript
(await import('/src/customer/client.ts')).customerClient.getSnapshot().subject?.accountId
```

No initialize/fetch/refresh/write command, token access or secret output. If it is
undefined (including a different HMR module instance), use the recorded GET response
instead; undefined is never a provisioning target. The required UUID/name confirmation
was subsequently received before the local activation documented above.

## Repository/evidence

Branch `feature/argopost-overview-completion`, HEAD
`59648b4bf4ba02ca069bd0fbf9529889134f3d59`, no commit/push/merge/new worktree.
Original87 working source paths backed up with SHA-256 under
`.demo/business-dashboard-review/before-source`. Private test resources and generated
credentials are not source artifacts. Final source fingerprints/patch and focused
test/privacy evidence are retained in the same ignored checkpoint directory.
Hosted integration CI/full H2 suite were not dispatched in this local checkpoint.
GitHub read-only query found no PR for the current branch. No new commit/push/PR
or merge was performed; local WIP is not represented by a hosted CI PASS.
