# ArgoPost Personal — Owner-directed delivery

Authority: Owner attachment 896af06c-cd95-4779-9f5b-61f420206216, 2026-11-01.
Baseline: develop `cefd3189701ba493dba1430a9fda8b1e50ee2a03`.
Owner subsequently confirmed service/coverage/tariff data is unavailable and
shipment creation must remain closed. This supersedes the initial delivery
sequence only for those blocked dependencies; it does not authorize fake rates.

## Actual inventory and bounded design

| Area | Existing source / contract | Delivery status before this change |
| --- | --- | --- |
| Customer authentication | ProductionCustomerGuard, CustomerAdmission, customer client; V7–V10/V12/V15–V17 | IMPLEMENTED; reuse current HTTPS/session/OTP controls |
| Independent personal profile | CustomerProfileHttp / CustomerProfiles, V11; current GET/PUT profile + ETag/scoped receipt | IMPLEMENTED; integrate unchanged |
| Merchant applications | MerchantApplicationHttp, V14; own-account drafts/search/paging | PARTIAL; application is not membership or active workspace |
| Organization/membership contexts | Spec003 context contract exists; no deployed production org/member provider | BLOCKED for Business switch; Personal is the only current workspace |
| Shipments | orders.demo_shipment and ShipmentController only under demo profile, merchant-owned DRAFT/CANCELLED | NOT a Personal implementation; never call /api/demo from Customer |
| Pricing / service coverage | Empty owner schemas, target specs004/005; no production tariff/catalog API | BLOCKED: DEC05/06/07/08; Owner says leave booking closed |
| Personal drafts / parcels | Target specs006/007; no production API/table | NOT_STARTED; approved exact schema/wire and shipping policy needed |
| Tracking / pickup / labels | Target specs008–010/015, no production data source | NOT_STARTED; source-owner dependency |
| Finance / COD / documents | Target specs017–019/021; no production financial source | NOT_STARTED; never assume payment or zero obligations |
| Address book | Target spec004; application addresses are application snapshots, not an address book | NOT_STARTED; reviewed manual/unverified address contract and catalog integration needed |

Shared Customer Application: current authentication and proof client, existing
profile/application editors, one responsive WorkspaceShell and typed module
navigation. Header dropdown is a disclosure of available workspaces, not a grant
selector. It exposes Personal only; explains missing Business authority and links
to the actual application list. No organization is inferred from APPROVED,
organizationId, URL, email or cached selection. Any /merchant path remains denied.
No new API, table, permission, provider or migration is introduced by slice1.

Dashboard reads the current subject and personal profile from the existing
protected API. A failed read is an error with explicit retry, never an empty or
zero-count success. Shipment statistics/lookup/notifications are unavailable
until their source exists. Deferred sidebar modules explain their actual
dependency; they do not simulate forms, payments, tracking or tickets.

## Delivery checklist

- [x] **PW-01 SHELL — DONE** — Responsive header/sidebar, Personal workspace disclosure,
  real user menu/logout, protected deep links, no Business grant. Verify keyboard,
  session revocation, mobile, unavailable modules and no demo/API fallback.
- [x] **PW-02 ACCOUNT — DONE within this bounded scope** — Dashboard real profile read, loading/error/retry and
  existing profile/application navigation. Verify save/refresh/unknown receipt
  behavior through actual HTTPS + PostgreSQL; no fake shipment empty state.
- [x] **PW-03 ADDRESS — DONE** under Owner GO WITH CONDITIONS, 2026-11-01;
  [approved exact contract](personal-address-book-contract.md): manual UNVERIFIED
  records, self authority/default/version/receipt/audit; no provider or booking.
- [ ] **PW-04 BOOKING** — Draft/multiple parcels/quote/confirmation. BLOCKED by
  absent approved service/coverage/tariff/funding inputs; Owner leaves creation closed.
- [ ] **PW-05 SHIPMENTS** — Scoped list/detail/timeline/cancel transitions.
  NOT_STARTED; depends on PW-04 and authoritative tracking/custody contracts.
- [ ] **PW-06 OPERATIONS** — Pickup/labels/support/documents/COD. NOT_STARTED;
  integrate respective owners only when sources/policies exist.

No Full Personal or Full Spec003 acceptance follows from a shell checkpoint.
Backend architecture/H2/PG/browser gates remain mandatory for later integration.
Local UI fixtures and actual HTTP/PG evidence must be reported separately.

## Observed verification — 2026-11-01

- Frontend typecheck/lint, default and customer builds PASS. Frontend units41/41.
  Initial full isolated UI26/26 PASS; final security-entry and responsive changes
  then passed focused9/9, including confirmed/unknown logout behavior. Final full
  isolated UI suite27/27 PASS on the completed source; no remaining UI failure.
- Backend `mvnw.cmd -o -B -ntp -Ddebug=false -Dtrace=false clean verify` PASS:
  H2658 discovered,641 executed/PASS,17 policy skips,0 failure/error; includes
  Architecture23/23. Sanitized H2 report guard PASS with zero problems.
- Existing `IdentityGrantApplicationTests#customerEntryBrowserUsesActualHttpsPostgresAndSmtp`
  via explicit testcontainers-postgres profile PASS on the final customer build.
  One Maven PG integration test executes9/9 actual HTTPS/PG18/test-SMTP browser
  cases; runtime result PASS/CLEANUP/NONE, revision LOCAL_UNPUBLISHED. Added real
  dashboard reads, Personal-only switcher and saved-profile refresh assertions.
  This is a focused PG browser gate, not the full993-test PG regression or live Gmail.
- Report-policy/privacy Python suite130/130 PASS. Source-manifest validator and
  whitespace check PASS. Desktop1440 and mobile390 screenshots reviewed; UI checks
  widths360/390/768/1024/1440, guest denial, revoked session, missing Business source,
  loading/error/retry and no fake shipment data.
- Existing launcher stop/start used only owned processes. Existing PostgreSQL18.6
  preserved; Flyway migration check/startup PASS, LOCAL_FULL_STACK_HTTP_READY.
  Visit https://127.0.0.1:5173/login then /personal. No new account was inserted
  into the development database for verification; browser fixture data is disposable.
- Actual Windows localhost guest check5/5 protected routes denied to login with
  TLS validation enabled, fresh browser context and no account/email mutation.
  Initial sandbox-only launch could not access local runtime; the same check
  passed with host access. Existing development Flyway history remains V17 and
  only the original PostgreSQL container remains after test-owned cleanup.
- Earlier attempts retained accurately: the .demo Maven cache lacked mail in
  offline mode; switched to the existing default cache without version changes.
  Initial H2 tests passed but packaging failed because the running local backend
  locked its JAR. Report guard also rejected mixed stale PG/H2 reports. Owned stop
  and fresh clean verify resolved both; no test/report guard was bypassed.

Evidence stays ignored under `.demo/argopost-personal/`, including private raw
diagnostics. Safe final focused PG result:
`pg-final-0718fd0f-4169-4e21-931b-4190d075322e/customer-browser-*/customer-browser-results.json`.
Source is uncommitted on feature/argopost-personal-workspace; no new PR, hosted CI
or merge is claimed. Full Dashboard statistics and authorized Business switching
remain PARTIAL/BLOCKED as the inventory states, despite PW-01/02's bounded DONE.

## Personal Address Book — completed vertical slice, 2026-11-01

PW-03 is IMPLEMENTED and VERIFIED_LOCAL. React form/list/filter/paging/edit/archive
uses the approved six production operations in `contracts/openapi/personal-address-book.yaml`.
CustomerAdmission derives self scope and owns the physical transaction; the
merchant module owns address/current-version/history, audit and receipt DML.
Raw sender/recipient contacts remain UNVERIFIED; domestic0/+84 normalize only as
contact metadata. Search also accepts a full domestic phone, scoped before paging.
Default displacement is atomic with a version/audit on the displaced address.
Archived contacts disappear from active reads; self receipt metadata remains
available to resolve an unknown archive without returning archived PII.

Observed checks:

- Full frontend unit44/44, UI30/30 PASS; final screenshot-only refinement reran
  address UI3/3 PASS. Desktop/mobile screenshots viewed. Typecheck/lint/default
  and customer builds PASS; no additional framework/provider.
- Backend clean H2 verify646 PASS/17 policy skips; Architecture23 included.
  H2 sanitized report guard PASS. Existing report-policy/privacy suite130/130 PASS
  with finite new inventories and negative missing/skip controls preserved.
- Full CI-style local PG18 clean verify1011 PASS/5 policy skips,0 failures/errors;
  sanitized report guard, generated-secret privacy and complete Testcontainers
  cleanup audits PASS. Real HTTPS/PG18/test-SMTP browser10/10 PASS/CLEANUP, including
  actual CRUD/refresh and one forwarded-then-lost create resolved via its real receipt.
- Subsequent domestic-phone search refinement reran all affected address,
  migration, HMAC, architecture and real-browser cases:46/46 Maven tests and
  browser10/10 PASS. Full1011-case evidence belongs to the preceding regression
  checkpoint; no claim that hosted CI or the full suite was rerun after this
  narrowly scoped SQL search adjustment.
- Local official launcher migration V18 and HTTP readiness PASS. Aggregate
  fingerprints of existing accounts/identifiers/profiles/applications unchanged;
  historical migration version/checksum prefix unchanged. No raw Customer data
  exported for this check; no test customer/contact inserted into the dev DB.
- Final live guest smoke: six protected routes redirect to login and the address
  API returns401; TLS validation enabled, no account mutation. The Node HTTP probe
  needed the existing public localhost certificate via NODE_EXTRA_CA_CERTS;
  no certificate replacement or TLS bypass. Launcher status is LOCAL_FULL_STACK_HTTP_READY.

Existing local container was stopped temporarily only to let the owned, labeled
CI fixture use its policy-required5433 socket-fault test. CI fixture was removed
and the exact original container ID restarted healthy, preserving its named volume.
Earlier separate fresh test DBs/roles were removed only after name/owner checks.
No reset, old migration edit, worktree creation, live Gmail or S3 test occurred.

Initial failures retained in ignored diagnostics: missing zero-version HMAC
createPersonalAddress allowance (fixed specifically, guarded for update/archive);
OpenAPI explicit4xx/tag metadata; finite test counts after V18; old36-table/17-
migration expectations; full PG setup requiring the actual owned CI container;
PowerShell quoting in the temporary runner. None was bypassed or called PASS.

Safe full-PG evidence: `.demo/argopost-personal/address-ci-pg-af263a4d-fb99-498c-9c1e-c99ee7af7181/sanitized/`.
Final focused evidence: `address-final-d6fb7c80-d1e5-4012-84b3-16c797ea0e7a/` under
the same ignored evidence root. Private raw diagnostics and local fingerprint
records remain ignored; they are not Git deliverables.

Local use: sign in at https://127.0.0.1:5173/login, open **Sổ địa chỉ** or
https://127.0.0.1:5173/personal/addresses. Shipping/quotes/geocoding/Business remain
closed or unavailable under the previously recorded Owner decisions. No further
approval is needed for this delivered address slice; future dependencies do not
block its manual/unverified contact-management scope.
