# Administration Console — Menu 1–2 readiness

Date: 2026-11-25. Scope: SOURCE/TEST only; Owner owns Menus 1–5 and limits this
checkpoint to Menus 1–2. Menus 6–9 and shared ADM-17/20 implementations are not
changed. Latest Owner instruction selects
`admin-console-functional-coverage.csv` as the primary coverage checklist instead
of rereading the DOCX. This review preserves its requirements and historical
evidence; a coverage row is not permission to implement proposed APIs or policy.

## Preflight and preservation

- Read AGENTS, entire Project North Star, definition-of-ready, current coverage,
  approved P0 contracts/manifest and implementation evidence before changes.
- Branch `develop`, baseline `d5a0c240841bdb35d9ef6064cddba696a5b2fa62`.
- Approved P0 manifest SHA-256 remains
  `90c795bd7e773b6a58d0d220056cfbc5e9a5ee95b38ae2493d5b9f75a372291f`.
- `.demo/admin-menu12-preflight.json` records 124 existing file hashes, including
  all 44 migrations V1–V45 (no V33), current WIP and staged Owner documentation.
- No Backend, migration, API, permission, role, operator, Work Inbox, shared
  ADM-17/20, Internal Shell or navigation registry implementation change.
- Keep all nine parents and 23 module IDs. The five Menu 2 entries below are
  local navigation on the existing ADM-02/03 routes, not new authority/routes.
- No DB mutation, deployment, email, commit, push, merge or new worktree.

## Menu 1 — all four entries

| Entry / required capability | Classification | Source and remaining boundary |
|---|---|---|
| Executive Dashboard: Merchant state counts and processing samples | VERIFIED | Canonical submitted applications/revisions/events, first-submission cohort; no DRAFT counting |
| Executive Dashboard: Booking, Acceptance, Parcel/status | VERIFIED | Orders Shipment/Parcel/intake sources; count grain stays explicit |
| Executive Dashboard: confirmed fees/UNPAID | VERIFIED | Finance obligations/snapshots; confirmed fee is not collection or revenue |
| Time/status/current scope filters, KPI charts and drill-through | VERIFIED | Existing six GET APIs, v2 negotiation, HMAC source/scope/cohort snapshot; revalidation rejects stale data |
| Metric version/timezone/numerator/denominator/source freshness | VERIFIED | Existing v2 metadata; checkedAt does not substitute for source event time |
| Merchant Queue and Decisions navigation | VERIFIED | Existing Merchant readers/Work Inbox and canonical command links |
| My Approvals: assigned review/decision, status, profile/context/logout | VERIFIED | Consume existing ADM-17 Work Inbox; no second task store or grant union |
| My Approvals: general claim/handoff, notifications/preferences, MFA/session inventory | INTEGRATION_DEPENDENT | Shared ADM-17/IAM/Notifications owned by other team; requires their approved interfaces |
| My Approvals: deadline/escalation | BLOCKED_CONTRACT | `dueAt=null` is truthful until a deadline policy/source exists |
| Operational Risk: source availability and valid work links | VERIFIED | Existing read-only surface; no inference of overdue from UNPAID or SLA breach from waiting handover |
| Operational Risk: official network/SLA/risk/incident/escalation metrics | INTEGRATION_DEPENDENT | Carrier/network/policy sources absent; UNAVAILABLE, not zero. NWS simulation sites do not constitute an official commercial registry |
| Service Health: permission-aware Administration API availability | VERIFIED | Existing technical READ/API; responsive loading/error/unavailable states |
| Service Health: queues/jobs/integrations/security/releases | INTEGRATION_DEPENDENT | Canonical telemetry and relevant Menu 6–9 contracts; unavailable widgets do not assert platform health |
| ADM-18: own delegated scope, independent reviewer/decider, no self-approval, expiry/revocation, Backend recheck and immutable audit | VERIFIED | Existing Merchant Governance/Identity controls and read-only AuthorityView |
| ADM-18: general matrix editor, decision-type registry, dynamic thresholds, multi-level delegation/policy publication/review | PROPOSED | No approved multi-domain execution authority; see existing ADM-18 coverage row |

**MENU_1_ADMIN_OWNED_READY — SOURCE/TEST**, limited to the currently approved
Admin-owned functionality and dependency disclosure above. Historical
`MENU_1_ADMIN_CORE_ACCEPTED — SOURCE/TEST` remains valid. This does not mean
Full Specification, advanced ADM-17/18 implementation, Local UAT or Production.
No Menu 1 engine was reimplemented.

## Menu 2 — all five entries

| Entry / required capability | Classification | Actual implementation / gap |
|---|---|---|
| Merchant Applications: submitted inbox, date/state/policy/reviewer filters and direct Application ID lookup | VERIFIED | Existing P0-GOV APIs; added UI controls use existing query fields, reset pagination; no page-only pseudo-search |
| Merchant Applications: search legal/tax/applicant, SLA/risk flags, saved views/export | BLOCKED_CONTRACT | Admin ApplicationQuery has no legal/tax/applicant search; no approved SLA/risk/export source/authority |
| Review Workspace: assignment, BEGIN/checklist, request information/resubmission and independent decisions | VERIFIED | Existing V42/V43, current scope/reviewer/decider/reauth/version/idempotency; existing actions reused |
| Review Workspace: explicit private disclosure, legal/representative/contact/addresses/consent and screened documents | VERIFIED | Added presentation of existing ReviewData fields; no automatic private disclosure or raw payload persistence |
| Review Workspace: immutable revision metadata and pagination | VERIFIED | Added consumer of existing revisions API; full-page parser rejects foreign/duplicate/extra-field metadata |
| Review Workspace: full field-by-field revision comparison and free-standing internal comments | BLOCKED_CONTRACT | Current revision DTO exposes metadata only; command reasons are not a general comment thread |
| Review Workspace: duplicate/open-key/contact/representation/conflict checks | VERIFIED | Existing canonical guards and policy/checklist, not editable duplicate override or automatic merge |
| Review Workspace: expanded investigative duplicate/risk/expiry result panels | BLOCKED_CONTRACT | No richer approved projection or external verification provider/policy |
| Review Workspace: atomic approve/Organization/Initial Owner and safe receipt replay | VERIFIED | Existing V43; approval response exposes immutable activation outcome; added link to exact Organization detail |
| Review Workspace: notices and event/audit integration | VERIFIED | Existing four-template event/receipt-bound committed outbox, Work Inbox deep links and application history; no parallel engines |
| Review Workspace: Admin notice attempt/delivery-status reader | BLOCKED_CONTRACT | No approved Admin outbox status API; approval never claims email delivery |
| Merchant Directory: scoped list/detail, state, admission origin, activation, owner reference and membership role/state counts | VERIFIED | Existing P0-GOV-13/14; added exact Organization deep link and backlink to reviewed application |
| Merchant Directory: full legal/contact/contract/warehouse/risk profile and per-member invitation oversight | BLOCKED_CONTRACT | Current OrganizationSummary is deliberately bounded; no Admin use of Merchant self-service or private tables |
| Organization Lifecycle: ACTIVE↔SUSPENDED, step-up, reason, ETag/idempotency, current-state refresh | VERIFIED | Existing P0-GOV-15/16; no historical Shipment/Finance deletion and no Owner/role mutation |
| Organization Lifecycle: immutable audit | VERIFIED | Existing scoped Audit reader remains reusable; Organization detail links to source application history; no new audit engine |
| Organization Lifecycle: RESTRICTED/CLOSED, effective risk holds, dispute/reverification workflow | BLOCKED_CONTRACT | Canonical state/authority/policy amendment required; only ACTIVE/SUSPENDED may execute |
| Owner Transfer: explicit entry and truthful blocked state | VERIFIED | Added existing ADM-03 view; no transfer button, role rewrite or operator workaround |
| Owner Transfer: verified parties, maker-checker, last Owner protection and atomic authority transfer | BLOCKED_CONTRACT | V23/V34 keep Initial Owner immutable; requires approved Merchant/Identity amendment and owner coordination |

**MENU_2_APPROVED_CAPABILITIES_READY — SOURCE/TEST** after the targeted gates
below. **Menu 2 full Admin-owned acceptance remains BLOCKED_CONTRACT/PARTIAL.**
Do not rename this result `MENU_2_ADMIN_OWNED_READY` or Full Specification while
the Admin-owned gaps above remain unapproved. Bounded follow-up proposal:
`admin-menu2-remaining-contracts.proposed.md`.

## Source changes

- `MerchantMenu.tsx`: five local entries on existing ADM-02/03 paths. Navigation
  disabled while the current command is in flight or outcome is uncertain.
- `MerchantApplications.tsx`: explicit Review Workspace view; existing scoped
  query controls/UUID lookup, disclosed addresses/consent/evidence purposes,
  paginated immutable revision metadata, exact activation link. Existing reason,
  checklist, reauthentication and retry commands remain canonical.
- `governance.ts`: validate every revision item before selecting policy, including
  items after the current revision; reject duplicate/foreign/extra-field rows.
- `MerchantOrganizations.tsx`: directory/lifecycle/blocked Owner Transfer views,
  authorized detail lookup by Organization ID, stale response generation guard
  and backlink to canonical application review. Existing lifecycle logic reused.
- `admin.css`: selectors scoped to Merchant navigation/content; no shared shell
  redesign. Existing implementation inventory records the new consumer file.
- Coverage CSV updates ADM-01/02/03 current facts, retaining historical evidence
  and full-spec PARTIAL classification. No change to requirements/other modules.

## Verification ledger

Initial TDD: new revision-history test failed collection because the parser export
did not exist. After implementation: **7/7 parser tests PASS**.

First affected UI run: **8/8 PASS**, including existing review/checklist, lost
response, independent reject/approve, Organization lifecycle, Menu 1 and shell.
After adding the scope-filter assertion and exact Organization denial case, the
next run was **8 PASS / 1 FAIL**: `getByLabel('Policy scope', exact)` did not match
the select label although the accessibility tree correctly exposed the combobox.
Changed only that locator to `getByRole('combobox', {name:'Policy scope',exact:true})`;
the filter request/pagination assertions remain unchanged. Focused rerun of both
new cases: **2/2 PASS**. The nine affected UI scenarios have passing evidence
across these runs; do not relabel the intermediate 8/1 run as 9/9.

Final commands:

- `npx.cmd playwright test --config playwright-unit.config.ts admin-governance.spec.ts internal-navigation.spec.ts` — **13/13 PASS**.
- `npx.cmd playwright test --config playwright-customer-ui.config.ts admin-dashboard.spec.ts --grep 'Menu 2|Merchant review|independent decider|independent approval|organization lifecycle|Organization activation|internal shell groups overview|overview work center'` — **8 PASS / 1 locator FAIL** as recorded above.
- `npx.cmd playwright test --config playwright-customer-ui.config.ts admin-dashboard.spec.ts --grep 'Menu 2 exposes|Organization activation'` — **2/2 PASS**, targeted correction only.
- `npm.cmd run typecheck` — **PASS**; UI test servers also run
  `build:customer` (TypeScript + Vite) successfully. Existing large-bundle advisory
  remains; no threshold or assertion was weakened.
- `npx.cmd eslint src/admin/MerchantMenu.tsx src/admin/MerchantApplications.tsx src/admin/MerchantOrganizations.tsx src/admin/governance.ts unit-tests/admin-governance.spec.ts ui-unit-tests/admin-dashboard.spec.ts --max-warnings 0` — **PASS**.
- `python docs/reconciliation/v3/validate.py --implementation` — **PASS**.
- `git diff --check` — **PASS** for unstaged changes. Staged Owner documents
  remain unchanged, including their previously recorded Markdown hard breaks.

UI cases use explicitly intercepted synthetic APIs, **not** Backend/live UAT.
Mobile 390×844 overflow assertion passed; rendered
`.demo/spec003-implementation/admin-menu2-mobile.png` visually inspected: contained
content, horizontal local navigation and explicit blocked transfer state.

Reused evidence: post-merge `develop` CI45557771726, committed tree equivalent to
accepted PR51 HEAD `6e4e1c3a7d38c810e215ea4de96db1f2ddadd35a`, previously reported
2,103 Backend PASS. The source/test execution ledger retains earlier failures and
exact focused PostgreSQL/HTTP/HTTPS cases, including independent activation and
revocation/rollback. That prior CI is not a CI certificate for this WIP UI patch.
No Backend/DB changes here, so no redundant full Backend/PG run or migration.

## Handoff

Do not start Menu 3 in this turn. Proposed next task is Menu 3 — Dịch vụ &
Mạng lưới, while the bounded Menu 2 contracts receive review. Reuse existing
PilotCoverage/Pricing/NWS-V1; distinguish simulation sites from official network.
Coordinate ADM-17/20 and IAM/Notifications/Platform consumers with the other team;
do not overwrite or duplicate their implementation.

## Latest Owner direction — stay on Menu2 (2026-11-25)

The previous proposed Menu3 handoff is **superseded by Owner**. Do not start Menu3.
Existing Menu1/2 core/approved-capability source/test evidence above is retained;
Menu2 Full Specification is still PARTIAL/BLOCKED_CONTRACT, not FULL READY.

Current work is the bounded M2-R01/M2-L01/M2-O01 technical contract package:
`admin-menu2-completion-contract.proposed.md`,
`admin-menu2-completion-technical-manifest.proposed.json` and
`admin-menu2-completion-technical-review.md`. It identifies mandatory vs optional
capabilities, exact proposed paths/DTO/schema/grants, source ownership, decisions
and shared-team coordination. No implementation of new authority was authorized
or performed; no old P0 engine or PASS tests were rerun for a documentation task.

Next action is consolidated review of DEC-R1, DEC-L1..L4, DEC-O1..O4 and the exact
manifest, with R independently reviewable before L/O. Identity/Team/Notifications
changes remain coordination-dependent. Menu3 is not the next execution task.

## Owner Phase1 implementation — 2026-11-26

The attached **ADM-02/03 COMPLETION & ACCEPTANCE** directive supersedes the
documentation-only boundary above for the exact **Merchant-owned M2-R01 subset**.
Manifest SHA-256 verified unchanged:
`6d072d9b649ab144c2fb9b8b780c8f077757387bcef58536a31618f9154251a2`.
Original contract/manifest/review bytes and all historical evidence remain intact.

**Checkpoint: M2-R01 APPROVED SUBSET SOURCE/TEST READY.** This is neither Menu2
Full Specification readiness nor Local UAT/deployment approval. Menu1 remains
accepted; no Menu3 implementation or shared Menu6–9 engine changes were made.

### Implemented and verified

| Menu2 screen | Current coverage | Remaining mandatory gap |
|---|---|---|
| Merchant Applications | Existing submitted queue retained; server search matches immutable submitted legal data over authorized cohort before paging; normalized exact name/legal ID, applicant/reviewer/policy/date/state, canonical check filters; HMAC cursor/source binding | Notice status and policy-derived timing unavailable |
| Review Workspace | Existing assignment/BEGIN/RFI/resubmission/independent decision retained; two-revision typed comparison, protected append-only comments, current canonical checks | Notifications-owned delivery status; no invented external verification/SLA |
| Merchant Directory | New Organization search/profile and membership metadata; historical activation/legal facts, masked contact, own facility references; current Owner remains Initial Owner under unchanged invariants | Pricing contract-reference port; expanded lifecycle/ownership dependencies |
| Organization Lifecycle | Existing ACTIVE↔SUSPENDED and audit preserved | M2-L01/DEC-L1..4 not approved; RESTRICTED/CLOSED/holds/dispute/reverification/clearance blocked |
| Owner Transfer | Existing historical Initial Owner protected; no mutation enabled | M2-O01/DEC-O1..4 and coordinated Identity/Team/notification contracts blocked |

New `MerchantGovernanceViews` and `MerchantGovernanceViewsHttp` implement only
M2R-API-01/02/03/05/07/08/09 and sourced Merchant blocks of04. No06/10 route,
new lifecycle state or ownership mutation. Reads use genuine Identity-owned
command transactions because disclosure audit is persisted. The two existing
SYSTEM HTTP dispatch allowlists were extended only with the exact approved
routes; bearer, HTTPS, same-Origin, current admission and Merchant scope remain
mandatory. This is routing integration, not IAM provisioning or a new auth engine.

The consumer is default-off under `merchant.admin-governance.views-enabled`,
inside the existing Local/Test governance gates. Enabled composition requires
V46. No demo configuration, process, account, operator or real grant was changed.
New explicit capabilities are only `application.comment.write`,
`org.profile.read`, `org.members.read`; `application.notice.read` is not installed.
Scope/source changes invalidate cursors. Requests exceeding the existing bounded
Identity subject-lock capacity fail unavailable rather than returning a partial
check-filter cohort. No caller-controlled SQL, raw query in cursor/URL/log,
credentials, invitation emails, proof or document path are disclosed.

### Migration and preservation

- Branch remains `develop`, HEAD `d5a0c240841bdb35d9ef6064cddba696a5b2fa62`.
  Git index remains Owner's staged AGENTS/NorthStar only; no Git mutation.
- Read-only GitHub preflight: no open PRs; integration develop at same HEAD;
  relevant Admin/onboarding branch trees contained no V46. Local WIP already
  includes NWS V44/V45. **V46** selected on this evidence; no claim to reserve
  unobservable teammate WIP or future V47/V48.
- V46 adds the one approved encrypted append-only comment table, exact three
  capability allowlist deltas and current-event/transaction binding guard.
  Existing canonical scope operator signature/owner/receipt semantics retained.
  Temporary schema CREATE is revoked before migration commit; runtime gets no
  operator EXECUTE, role membership or comment UPDATE/DELETE privilege.
- All **44 historical migration files V1–V45 (no V33)** match the preflight byte
  hashes, including NWS WIP. PG18 cold validation/rerun and V43→V46 forward upgrade
  preserved existing Identity/profile/address data and applied checksums.
- Existing acceptance, Organization activation, Business, Finance UNPAID, custody,
  Tracking, Team and Notifications implementations were not rebuilt or mutated.
  Remaining latest-stream fixture counts were advanced mechanically for V46;
  this does not assert all historical test methods were rerun.

### Actual verification and failure ledger

Commands, all isolated and with no real mailbox/provider mutation:

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File scripts/ci/run-cskh-focused.ps1 -Tests 'IdentityGrantApplicationTests#merchantR01SearchCommentsAndScope+merchantR01ProfileAndMembers+merchantGovernanceRejectsConstructedIdentityContexts,IdentityMigrationTests#coldStartValidateAndRerunPreserveHistoryAndIdentityData,MerchantViewCursorTests,ArchitectureTests,ProductionAuthenticationHttpTests' -Offline
powershell.exe -NoProfile -ExecutionPolicy Bypass -File scripts/ci/run-cskh-focused.ps1 -Tests 'IdentityGrantApplicationTests#merchantR01ComparisonAndHttps,IdentityMigrationTests#nwsForwardUpgradePreservesV43RowsChecksumsAndPrivateGuardBoundary' -Offline
# app/frontend/web
npm.cmd run typecheck
npm.cmd run lint
npm.cmd test -- admin-governance.spec.ts admin-session.spec.ts admin-review-views.spec.ts
npm.cmd run build:customer
```

- Backend batch: **45 PASS, 0 FAIL/ERROR/SKIP**, private evidence
  `.demo/cskh-focused-9b89ad9b054446a38faba481c0020b14`.
- Final E2E/forward-upgrade batch: **2 PASS, 0 FAIL/ERROR/SKIP**, private evidence
  `.demo/cskh-focused-f6c861455e444b45a475a788ba80fa43`.
- Both batches have identical source snapshot SHA-256
  `0fdf2be01d10d91e7da136b79284de22d1544860af77166f9c5d3cad7a39f9cf`,
  and `SOURCE_UNCHANGED=True`. Private summaries, reports and browser diagnostics
  are ignored artifacts, not committed credentials or public fixtures.
- Two actual submissions prove match-before-page, exact matchedCount and
  distinct pages; source mutation rejects the old cursor. Same-key/different-input,
  two concurrent comment appends, stale If-Match replay, transaction rollback,
  direct DML/update/delete/prior-transaction-event denial, revoked scope,
  forwarding context, substituted DataSource and escaped callback are covered.
- Genuine pinned HTTPS browser uses production SYSTEM login, two canonical
  immutable revisions, real independent approval/Organization activation, PG18
  search/comparison/checks/comment/profile/member APIs, foreign-Origin rejection,
  literal text rendering and mobile viewport containment. No intercepted API
  fixture or direct-SQL fake activation substituted for this acceptance.
- Frontend: **30/30 unit PASS**, typecheck/lint/build PASS. Existing Vite large
  chunk warning retained; no bundle-performance acceptance claimed.
- Original failures preserved: initial cursor RED missing implementation;
  ambiguous JDBC callback and test Timestamp compile failures corrected; initial
  V46 function replacement lacked temporary schema CREATE; fixture revoke used
  a newly calculated expiry rather than exact stored expiry; real HTTPS initially
  returned401/403 because both exact dispatch allowlists required integration.
  Each affected test was rerun after its fix; no assertion was weakened.
- No full Backend suite, GitHub CI, shared/demo/Admin-UAT, Production or live-mail
  acceptance is claimed for this WIP.
- `python docs/reconciliation/v3/validate.py --implementation`: PASS/exit0,
  semantic ownership CONSISTENT; historical broken-link warnings are retained.
  `git diff --check`: PASS. Preservation inventory confirms all44 historical SQL
  files unchanged and both approved manifest hashes exact. V46 SQL SHA-256:
  `10d213932e3e58088e42c220f970ecf816ff9662dde68aae5d55722a38b88919`.
  Safe inventories: `.demo/menu2-r01-preservation-final.json` and
  `.demo/menu2-final-validator.log`; original WIP preflight remains available.

| Gate | Current result |
|---|---|
| M2-T-01 | Approved search/projection subset PASS, including real cohort pagination/source drift and legacy GET consumers |
| M2-T-02 | Approved comparison/comments/member subset PASS; notice portion NOT RUN/BLOCKED |
| M2-T-03–05 | NOT RUN — lifecycle policy/authority/clearance unapproved |
| M2-T-06–07 | NOT RUN — Owner Transfer policy/Identity/Team amendments unapproved |
| M2-T-08 | Approved R-only HTTPS+PG18 integration PASS; full lifecycle/transfer scenario BLOCKED |

### One consolidated Owner decision checkpoint

Recommendations below remain candidates, not active policy or execution approval.

| Decision | Recommendation | Data/authority impact and actual dependency | Required Owner selection |
|---|---|---|---|
| DEC-L1 | RESTRICTED blocks only approved operational capabilities; CLOSED terminal; retain scoped history/support | Merchant operation-aware admission and current consumers; no Shipment/ledger deletion | Exact blocked capabilities, reinstatement and whether reopen is forbidden |
| DEC-L2 | Independent maker/reviewer/decider; executor needs separate grant and fresh proof; application two-person flow unchanged | Bounded case permissions/SoD, current authorized actors; no general approval engine | Required distinct people; whether decider may also execute |
| DEC-L3 | CLOSE requires real CLEAR from Orders/Finance; UNKNOWN denies; only BOOKING_HOLD/CONTRACT_CHANGE_HOLD | Current-domain serialization/clearance with Booking/obligations, not Dashboard totals | Exact clearance/hold effects and exceptions; UNAVAILABLE never CLEAR |
| DEC-L4 | Published versioned expiry/effectivity/evidence policy, no inferred deadlines | Merchant case/evidence + Identity consent binding; legal registry changes excluded | Exact allowed_case_types; case_ttl_seconds >0; consent_ttl_seconds >0 and <=caseTTL; max_future_effect_seconds >=0; dispute/reverification evidence requirements and approved retention_policy_reference; explicit due-date policy if enabled |
| DEC-O1 | Verified current Owner and ACTIVE verified non-Owner member of same Org both explicitly consent with reauth | Exact case/revision/intent-bound Identity port and explicit request permission | Eligible transferee set, consent TTL and both-party consent rules |
| DEC-O2 | Preserve Initial Owner/activation; separate one-current-ACTIVE-Owner pointer and immutable history | Coordinated Team/admission/offline operator paths; never change IDs by relabeling | Approve historical/current-owner split and coordinated source boundaries |
| DEC-O3 | Suspend outgoing Org membership/revoke its grants and pending invitations atomically; incoming receives only explicit minimum | Team grant/invitation/authority-version transitions; Personal/other Org unaffected | Exact outgoing state and incoming grant set; no inherited Finance/Carrier authority |
| DEC-O4 | Transfer only ACTIVE approved-origin Org with no open ownership dispute; retain obligations at Org | Merchant/Orders/Finance current facts and event-bound Notifications integration | Allowed origins/states, dispute/open-obligation blockers and legacy admission rules |

AWS IAM/S3 infrastructure belongs to the project Owner. **No AWS change is needed
or proposed for this R slice.** ArgoPost Identity owns proof/admission; Organization
Team owns membership/grant mutation. O01 requires exact coordinated public proof
binding and current-owner/guard changes described in the existing manifest;
this is not an AWS/IAM storage blocker or authority to rewrite another team's code.
Notifications owns notice-status/transfer-event delivery readers; Pricing owns
contract-reference disclosure; Orders/Finance own closure clearance/serialization.

**Menu2 remains incomplete against mandatory Full Specification** while these
unapproved policies and integrations remain. Do not declare Menu2 FULL READY,
Production READY or move to Menu3. Next authorized continuation is Owner's one
consolidated L/O decision plus exact shared-interface coordination; do not ask
again for approved R capabilities or repeat old PASS suites.

## Menu2 next execution gate — Owner confirmation, 2026-11-26

Owner accepted **M2-R01 APPROVED SUBSET SOURCE/TEST READY**. It is closed for this
task: no R reimplementation, API/V46 edits or repeated runtime tests. Latest
directive prioritizes L01 then O01, while explicitly requiring exact approval
before any new policy/API/schema/permission. It does **not** approve the eight
candidate decisions above or the remaining technical manifest entries.

**Current: M2-L01 / M2-O01 BLOCKED_EXACT_OWNER_APPROVAL**, plus the bounded
integration prerequisites below. Menu2 FULL_SPEC_READY remains pending. No new
general contract, runtime feature, migration candidate file or placeholder was
created at this checkpoint.

### Implementation order after exact approval

1. **Merchant Lifecycle preparation first:** existing M2L-API-01–15 design and
   Merchant-owned policy/case/review/event/receipt/hold components. Start with
   source/test for policy validation, draft/submission, scoped read/assignment,
   review/RFI/resubmission/cancellation and immutable history. Approve exact
   selected APIs/DTOs/tables/capabilities in the unchanged manifest, rather than
   inferring authority from this order. No account or operator grants follow
   automatically from source approval.
2. **Current-owner bridge is an early dependency:** M2-MIG-L explicitly installs
   `M2-DB-07 merchant.organization_current_owner` before any Lifecycle state
   activation. M2-DB-03/StateStamp bind an ownership version. Therefore the
   historical/current-owner model in **DEC-O2 must be approved early**, even if
   transfer execution is deferred. Initialize only from a verified, consistent
   Initial Owner; no fabricated ownership version, role change or transfer.
3. **Lifecycle decision/execution:** M2-AMEND-05 / M2-API-EXT-01 require Identity
   coordination for only `ORG_CASE_DECIDE` / `ORG_CASE_EXECUTE`, using existing
   proof engines. RESTRICTED/holds need M2-PORT-04/13 operation-aware admission
   and coordinated Customer/Orders/Pricing/Team consumers. Schema or review-case
   completion alone does not activate those state changes.
4. **CLOSE stays fail-closed:** M2-PORT-05 `OrganizationOpenWorkFacts.withClearance`
   and06 `OrganizationFinancialClearance.withClearance` have no implementations
   in current source. Orders/Finance must provide real facts, versions and
   serialization under the agreed Org gate. Missing source is UNAVAILABLE, never
   CLEAR; confirmed fees/UNPAID Dashboard readers cannot substitute for clearance.
5. **Owner Transfer after DEC-O1..4 and coordination:** M2O-API-01–08, exact
   ownership capabilities and consent/history/guard amendments remain unapproved.
   Keep current application approvals and ACTIVE↔SUSPENDED unchanged. Workflow
   composes canonical source events, proofs and notifications; it does not create
   a second Team, Identity, approval or notification engine.

### Exact shared dependencies requiring coordination, not unilateral edits

| Owner / manifest entries | Current source and missing change | Boundary |
|---|---|---|
| ArgoPost Identity — M2-AMEND-05/06, M2-API-EXT-01, M2-PORT-07, M2-DB-12, M2-SQL-03/07 | `AdminGovernanceReauthentication`, `JdbcAdminGovernanceReauthentication`, V41 operation checks; `CustomerReauthentication` / `CustomerOwnerRecoveryConsents` | Add case/revision/party/intent-bound consumed proof + current security-version checks under genuine context. Existing recovery consent binds only Org and cannot authorize transfer. No new login realm, AWS permission, real Identity grant or global session revocation |
| Organization Team / Business admission — M2-AMEND-03/08, M2-PORT-13, M2-API-EXT-02 | `JdbcBusinessWorkspaces`, `JdbcOrganizationTeam`, existing `team_permission`, `guard_workspace_identity`, `check_workspace_invariants`, `application_origin_admitted`, `configure_local_workspace` and their forward replacements | Coordinate current-owner reads, exactly-one-current-Owner protection, role/grant/invitation transitions and targeted Business authority invalidation. Preserve immutable Initial Owner, Personal and other Orgs; no edit to historical V23/V34/V43 |
| Notifications / shared ADM-17/20 — M2-AMEND-07/09, M2-PORT-09/10/11, M2-API-EXT-05/06 | Existing event-bound outbox/SMTP/Work Inbox/Audit | Exact case target/recipient/template and read-source integration need co-sign; no second engine, application-ID spoofing or delivered-email inference |
| Orders/Finance — M2-PORT-05/06 | Current clearance ports absent | CLOSE only after current canonical clearance/serialization and negative concurrency gates |

The project Owner controls AWS IAM/S3 infrastructure; it is **not a dependency
blocking Owner Transfer here**. Reuse existing evidence store/scanner where its
approved contract applies. No AWS policy/access changes or messages to other
contributors were made.

### GitHub / migration / preservation checks for this gate

- Local branch/HEAD unchanged: `develop` at
  `d5a0c240841bdb35d9ef6064cddba696a5b2fa62`; WIP/index inventoried before edits.
- Read-only GitHub queries found **0 open PRs** and the same integration HEAD.
  Complete, non-truncated trees checked for `develop`,
  `feature/argopost-admin-dashboard`, `feature/spec003-full-onboarding` and
  `feature/argopost-organization-management`; none contained V47/V48.
- Local stream ends at V46. No dedicated migration-reservation registry was found
  in the searched repository docs/specs; M2-MIG-L/O remain **candidate proposals**.
  This is not a reservation or certification of teammates' unobservable WIP.
  Recheck and coordinate version allocation immediately before creating SQL.
- V46 SHA remains
  `10d213932e3e58088e42c220f970ecf816ff9662dde68aae5d55722a38b88919`;
  manifest SHA remains `6d072d9b649ab144c2fb9b8b780c8f077757387bcef58536a31618f9154251a2`.
  New safe metadata inventories: `.demo/menu2-lo-next-gate-preflight.json` and
  `.demo/menu2-lo-next-gate-github.json`.
- Only this readiness report and traceability are updated in this task. Runtime
  security/PG18/HTTPS tests are **NOT RUN**: there is no newly approved L/O
  implementation to test, and accepted R source/dependencies are unchanged.
  Existing 45/45 +2/2 +30/30 evidence remains historical accepted evidence, not
  new L/O PASS. No database connection/mutation, grants, deployment or Git mutation.

**One Owner checkpoint only:** decide DEC-L1..L4 and DEC-O1..O4 using the table
above, including actual policy values, and identify the exact L/O manifest subset
authorized for isolated source/test. Shared Identity/Team/Notifications changes
must have exact source/interface coordination before edits. Until then, do not
mark L/O READY or Menu2 FULL_SPEC_READY, create V47/V48, or start Menu3.

## Owner Policy V1 / Step A execution — 2026-11-26

This section supersedes only the latest status above; historical approvals,
candidates, FAIL logs and accepted R01 evidence are preserved.

Owner approved DEC-L1–L4/O1–O4 business Policy V1 and isolated **Step A**:
Merchant policy/case/review/receipt/audit and current-owner bridge (DB02–07).
DEC-O3 is explicitly **ACTIVE OPERATOR + only dashboard.read** for the outgoing
Owner, replacing the old SUSPENDED candidate. See
`admin-menu2-outgoing-owner-operator.delta.md` and its JSON; business approval does
not authorize shared Team/Identity rewrites or transfer execution.

### Implemented preparation source

- New **V47 candidate**, after read-only branch/PR/migration preflight recorded in
  `.demo/menu2-policy-v1-migrations.json`; no claim of a global reservation of
  teammates' unobservable WIP. Six Merchant tables DB02–07, existing operator
  signature extended only for propose/review, exact Policy V1 publication,
  immutable event/receipt binding and read-only Current Owner backfill.
- Current Owner is initialized from the genuine eligible Initial Owner, retaining
  membership/identity/provenance. No transfer or Organization state activation.
- Canonical `OrganizationGovernanceCases` preparation API and JDBC implementation:
  draft/submission, exact Org scope, assignment/reassignment, BEGIN/checklist,
  RFI/resubmission, cancellation, immutable event history, current auth and receipts.
- Exact Step A HTTP routes and default-off `cases-enabled` composition. Existing
  SYSTEM routing allowlists admit only these approved paths; they issue no grants.
  Decision/execution/Customer consent remain absent.
- Lifecycle UI within existing Menu2; state filters, current allowed actions,
  uncertain-outcome replay retaining original input/key, policy selection and
  independent reviewer ID. No second task/Identity/notification engine.
- **M2-L-POLICY-PORT-01 separately Owner approved:** exact `policies` reader is
  implemented with genuine context/current Org scope. M2L-API-14 registration
  remains gated on its positive/foreign/revoked/unavailable tests PASS.

### Actual evidence (do not generalize to current full readiness)

- First TDD RED: missing policy validator; then preserved migration failures:
  PL/pgSQL CASE parse ambiguity, owned predicate EXECUTE mismatch and offline
  Identity operator-environment/current-role mismatch. Fixes stay in new V47;
  unchanged V1–V46 guards/ACL are not relaxed. Migration restores exact original
  role rather than RESET ROLE to login, and restores transaction-local config.
- `.demo/cskh-focused-c5a809e152c84ff0b9240ceeeb66873c`: **2/2 PASS**, including
  actual populated PG18 migration/backfill, unchanged Organization/history,
  policy publication/replay/conflict, create→submit→assign→begin→RFI→revision2
  resubmission→begin→ready, encrypted receipts/audit and revoked-review denial.
  Snapshot `0b779fefd9c571e727695690bf3df7ecd5c1e80de934d16e424500ff904571a5`.
  This is earlier preparation evidence, **not** final HTTP/UI/policy-reader PASS.
- Next focused run `.demo/cskh-focused-f0b39359806b4668a52565b8544fbb70` ended
  before test execution (0 fresh tests). Missing test import was subsequently
  reproduced/fixed by ordinary local compilation. Additional helper-signature
  compile errors were fixed; final local compile + policy test **1/1 PASS**.
- `npm.cmd test -- admin-organization-cases.spec.ts`: **3/3 PASS** (parsing,
  tenant/authority malformed-response denial, exact transport and lost response).
- `npm.cmd run typecheck`, `npm.cmd run lint`, `npm.cmd run build:customer`:
  **PASS**; existing bundle-size warning remains, no threshold weakening.
- Additional source tests now cover policy binding and current scope, constructed/
  escaped contexts, concurrent same-key creation, rollback, direct-DML denial,
  strict HTTP/ETag validation and a pinned HTTPS Browser scenario. They are **not
  claimed PASS** until their focused runner completes on this revision.

### Remaining gates / domain dependencies

| Item | Exact gate |
|---|---|
| Current focused PG18/HTTPS evidence | Automatic approval review failed due usage limit on elevated private-log access; no alternative-tool bypass. Re-run authorized focused checks only after review is available. |
| M2L-API-14 | Owner requires positive, wrong scope, revoked context and unavailable-policy tests PASS before endpoint registration. |
| API12 applied holds / release | Step A DB02–07 excludes DB09 authoritative applied holds; no fake empty inventory or applied hold from a preparation case. |
| DISPUTE/REVERIFY | No approved Organization-case evidence retention reference found. Existing application retention is not this purpose; keep blocked. |
| Lifecycle decide/execute | Identity purpose-bound proof operations + coordinated consumer admission and actual Orders/Finance CLOSE clearance are still required. |
| Owner Transfer | Current-owner Team/offline/invitation guards and genuine case/revision/party intent proof need owner-domain coordination and Step C exact technical gate. No transfer implementation or real grants. |

**Current: M2-L01 STEP A IN PROGRESS — final integration gates pending.**
Not Menu2 FULL_SPEC_READY, not Local UAT/Production Ready. No shared/demo/UAT/Prod
database mutation, historical migration edits, real mail/grants, deployment or Git
commit/push/merge. Preserve M2-R01 accepted status; do not rerun its suite.

### Resumed review / policy endpoint gate

At the review service's stated recovery time, the same authorized focused runner
was retried; no alternative access path bypassed the failed review. Run
`.demo/cskh-focused-e4de5f4b31ea4d2283926939ce5abe4d` is **2/2 PASS**, source
unchanged, snapshot
`a91b9d181cf024f2d53d085380c3bba4be9ee44f7a8875505c85d14cda0da879`.
This includes actual policy-reader positive, absent-policy, wrong-Organization,
escaped context and revoked-scope denials; concurrent same-key create commits one
case, rollback leaves no case, and strict HTTP/If-Match checks pass. Therefore
M2L-API-14 was registered **after** the Owner's prerequisite gates PASS, using the
existing DTO and `org.read`, with no new API path or permission.

Final HTTPS/PG18 scenario for this integration is tracked separately; the above
PASS is not a substitute for its result. Build retains the existing chunk warning.

### Full review UX contract gap (not silently implemented)

`M2-Case.privatePayload` is explicitly excluded by the parent manifest, but no
Lifecycle payload/reason/RFI disclosure API/DTO/public reader is defined. Thus
the verified preparation/checklist transitions are **mechanical source/test
evidence**, not proof of a reviewer completing a substantive in-portal review.
`admin-menu2-case-disclosure.delta.md` records exact minimal API/input/output/port
and audited maker/assigned-reviewer scope checks; Owner confirmation requested.
Private payload is not appended to the existing metadata DTO or exposed in logs.
Do not mark Step A/full review UX READY until this gap is resolved or explicitly
accepted as deferred. It is independent of the approved policy reader.

HTTPS run `.demo/cskh-focused-7ab74610a1e14a77bae13c605598fa4f`:
**1/2 PASS, 1 FAIL**; actual login/list succeeded, then Browser timed out locating
the action selector after opening a risk case. Final readiness remains pending;
the diagnostic follow-up asserts the detail response and selected-case heading
before choosing an action. Assertions were not reduced or skipped.

### Disclosure approval and further integration (supersedes pending proposal)

Owner approved **M2-L-DISCLOSURE-01** for exactly one audited POST/DTO/port, current
maker or assigned reviewer, exact Organization/current authority, allowlisted
data and no state execution. Backend tests passed on isolated PostgreSQL18:
`7e2c53a6…` **1/1 PASS**, then `191afe7f…` **1/1 PASS** for the added SQL party
guard/direct-DML denial and genuine RFI field/reason disclosure. Each read creates
an encrypted `READ_CASE_DISCLOSURE` audit bound to source revision/event; it does
not change case version/state or create a command receipt. The endpoint was
registered only after those tests PASS. No private payload was added to M2-Case.

Browser diagnostic `602cdff6…` preserved its FAIL: HTTP detail=200, production
parser and selected-case heading PASS, then exact action label lookup timed out.
The control now has an explicit accessible label. The integrated UI uses only
published policy metadata and audited disclosure, clears private display after
commands/denials, and requires the current disclosure before checklist completion.
No browser storage/HTTP cache of case private content is introduced.

Frontend after disclosure: **4/4 targeted unit PASS**, typecheck/lint/customer
build PASS (existing chunk warning retained). A subsequent final runner stopped
at compile due a duplicate controller declaration (`4da69fab…`, 0 fresh tests);
the duplicate was removed without changing the endpoint contract or assertions.
Final HTTPS result remains an independent gate, to be recorded below.

### Final approved preparation subset acceptance — 2026-11-26

**M2-L01 APPROVED PREPARATION SUBSET SOURCE/TEST READY.** This is not full Step B,
Owner Transfer or `MENU_2_FULL_SPEC_READY`.

- Final runner `.demo/cskh-focused-1fd0bca391534af3bf936e1d9c55ead2`:
  **2/2 PASS, no skips, SOURCE_UNCHANGED=True**, source snapshot
  `259e08ba8ec95d5eea7d5322169e8666d695a6df72262aa6c066c5d7dee26efc`.
- Includes fresh PG18 V47 migration/validation on populated synthetic application-
  approved Organization, historical checksum/data preservation, exact policy
  publication/replay, bridge, state machine, RFI/revision, no self-review,
  reassignment denying former reviewer, current scope revocation, forged/escaped
  context denial, concurrent same-key creation, rollback, direct-DML denial,
  audited private disclosure/redaction, strict HTTP and unchanged Organization.
- Child pinned HTTPS Chromium: **1/1 PASS**. Genuine SYSTEM login → Organization
  Lifecycle → canonical list/detail → actual audited private content → submit
  through UI → persisted version2. Missing If-Match=428, foreign Origin=401,
  unscoped Organization=404. No API interception or mock business backend.
- Mobile 390px: no document horizontal overflow. Actual
  `admin-browser-5f8b5ebe-f81f-48bc-b7da-cf40960b4ae2/lifecycle-mobile.png`
  visually inspected; tables use bounded horizontal scrolling, forms remain in
  the viewport. Screenshot/private fixture stay in ignored evidence directory.
- Frontend **4/4 unit PASS**, typecheck/lint/customer build PASS. Existing bundle
  size warning retained. No full Backend suite or accepted R01 suite rerun.
- Automatic review outage resolved; subsequent required operations passed the
  normal review. Historical compile/migration/Browser FAIL evidence retained.

**Exact delivered envelope:** metadata/history, policy reader and preparation /
review / RFI / resubmission / cancellation for RESTRICT, CLOSE and RISK_HOLD;
published-policy selection and authorized disclosure in UI. CASE readiness never
changes Organization state, applies a risk hold, settles debt or transfers Owner.

**Still blocked:** API12 applied-hold source/release (DB09 not in Step A DB02–07),
DISPUTE/REVERIFY retention/evidence purpose, Identity decision/execute proof,
coordinated operation admission, real Orders/Finance close clearance, and current-
Owner Team/invitation/consent guards for Step C. No state execution or real grants.

Final read-only GitHub tree: develop still
`d5a0c240841bdb35d9ef6064cddba696a5b2fa62`, non-truncated tree has no V47/V48/V49;
evidence `.demo/menu2-policy-v1-final-github.json`. This is collision preflight,
not reservation of another contributor's unpublished WIP. No Menu3/deployment or
commit/push/merge. Next bounded work is coordinated Lifecycle execution proof /
admission, with CLOSE remaining fail-closed until canonical clearance exists.

Final housekeeping gates: four impacted ArchitectureTests (public API/module
boundary/direction/cycles) **4/4 PASS**; implementation ownership validator exit0;
`git diff --check` PASS. Preservation inventory confirms all **45 existing
migration files V1–V46 (no V33)** unchanged, approved manifest unchanged and Git
index identical to preflight (only the Owner-staged AGENTS/North Star files).
New V47 SHA-256:
`3389ec46fd7c89aa56d42b3d8c29588b482037704881560ee42ae7b9881fec13`.
Metadata evidence: `.demo/menu2-policy-v1-final-preservation.json` and
`.demo/menu2-policy-v1-final-ownership.json`. No CI PASS is claimed for this WIP.

### Step B technical authority reconciliation — 2026-11-26

Owner accepts R01 and preparation subsets; preserve both acceptance histories.
Current Step B status: **STEP_B_TECHNICAL_REVIEW_PREPARED —
BLOCKED_EXACT_TECHNICAL_APPROVAL**, with CLOSE additionally
**BLOCKED_CANONICAL_SOURCE**. Not Lifecycle execution READY or Menu2 FULL READY.

Exact bounded proposal and approval checkpoint:

- [Step B technical manifest](admin-menu2-step-b-technical-manifest.proposed.json).
- [Source findings, approval groups and dependency review](admin-menu2-step-b-technical-review.md).

Business DEC-L1–L4/DEC-O1–O4 are already approved and are not being resubmitted.
The technical gap is purpose-bound case proof, decider/executor disclosure,
SQL-readable protected effect/proof binding, applied holds, and coordinated
operation-aware admission. Existing Step A explicitly blocks these operations;
there is no independent unimplemented Step B runtime slice with exact approval.
Do not register new endpoints or change shared implementation before approval.

Only documents changed in this checkpoint. No application source, SQL migration,
permission grant, database or runtime mutation. All M2-B-T-01–07 behavior gates
remain NOT RUN pending technical approval; reuse accepted preparation evidence
without calling it Step B evidence. Orders/Finance clearance is not implemented
by an Admin fake reader; no CLEAR-positive result is claimed.

GitHub read-only evidence `.demo/menu2-step-b-github.json`: 51 branches,
0 open PRs; four inspected non-truncated relevant trees show no V48+.
V48/V49 are candidates only, not reservations; recheck registry and parallel WIP
before creating migration files. No change to Step C Owner Transfer, Menu3,
shared Menu6–9 implementation, AWS IAM/S3 or approved manifest bytes.

Step B manifest SHA-256:
`b88e6d9df08c60d78d45afde9883dc424ed5ccb9aa5b12033f271d39e9cd1cd4`.
Document checks **13/13 PASS**; ownership validator exit0/CONSISTENT;
`git diff --check` exit0. Preservation: 163 baseline fingerprints checked;
only this readiness and traceability intentionally changed, other 161 files
unchanged including all 46 V1–V47 migration files. HEAD and staged index unchanged.
Evidence `.demo/menu2-step-b-document-validation.json` and
`.demo/menu2-step-b-ownership.log`. These are document gates, not Lifecycle
authorization/transaction/HTTP/PG18 PASS. No full Backend rerun or CI claim.

### Owner priority supersession — V1 operational E2E first, 2026-11-26

**Step B disposition: DEFERRED_POST_V1 — NOT APPROVED FOR EXECUTION.**
Supersedes its technical-review priority and near-term Menu2 FULL_SPEC target.
Business Policy V1 remains approved; R01/preparation/Menu1/NWS acceptance and
all historical evidence remain unchanged. Do not execute Lifecycle/RESTRICTED/
CLOSED/holds/Owner Transfer or add their Identity purposes/permissions/guards.

Original Step B manifest SHA remains
`b88e6d9df08c60d78d45afde9883dc424ed5ccb9aa5b12033f271d39e9cd1cd4`.
Keep the artifact and its historical proposed content; this effective disposition
does not rewrite it or relabel it REJECTED/COMPLETED.

Current deliverable: [V1 E2E critical path and implementation plan](v1-operational-e2e-critical-path.md).
First blocker is BUSINESS Booking→Station intake and current tracking; NWS
foundation intake remains PERSONAL-only. Shipping grant/contract enablement after
Organization activation is explicit, not an automatic Owner grant. The bounded
[BIA-V1 proposal](v1-business-station-intake.delta.md) is the next consolidated
technical/business approval checkpoint. No new runtime source was implemented.

Do not resume Step B automatically. After Slice1, prioritize canonical Inventory,
Sorting, Manifest, Transfer/Receiving and Dispatch/Courier/POD; no Full Portal or
Production acceptance is implied by this change in priority.
