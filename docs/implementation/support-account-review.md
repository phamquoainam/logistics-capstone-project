# Personal Support / Account review —2026-11-10

Owner requests Support/Claims and avatar Account settings. Preserve current66
working paths; no worktree/reset/cleanup/publication. Reuse existing current-self
profile/ETag/receipt, logout/OTP recovery, Shipment/fees/label reads. Canonical
Support020 and Claims021 are Orders-owned. Owner accepted the bounded Ticket V1
Local/Test contract below; commercial SLA, Claims and Shipment changes remain blocked.

- [x] SA-01 Audit actual runtime/accepted contracts and preserve source fingerprints.
- [x] SA-02 Dedicated Support hub: searchable actual guidance, purposeful categories,
  contextual Shipment/fees links, distinct Ticket/change request/Claim boundaries.
- [x] SA-03 Avatar-triggered account disclosure, keyboard/outside/route dismissal,
  profile/security/settings access, no duplicate sidebar Account tree or grants.
- [x] SA-04 Existing profile/OTP-reset refinement; read-source settings/notification/
  privacy availability, no fictitious preference saves or security events.
- [x] SA-05 Tests/build/responsive/current-self/unknown-outcome/Browser/PG and source
  validation; update traceability and final archive only after observed gates.
- [x] SA-06 Owner-approved Ticket V1: forward V22, Orders service and workflow
  admission, current Staff grant/assignment, append-only conversation/audit and
  receipts/quota in the same transaction; no Claim/Shipment mutation.
- [x] SA-07 Customer Ticket UI and bounded Staff workbench, real PG18/HTTP/browser
  state transitions, ownership/concurrency/revocation/rollback/unknown outcomes.

Owner approved `personal-support-ticket-contract.proposed.md` on2026-11-10.
SA-06/07 are Local DONE after observed gates. Added V22, Orders SupportTickets
public API/JDBC/configuration, workflow SupportTicketHttp and client/UI/tests.
The new Staff route uses the existing Customer TLS/Bearer/Origin/CSRF chain plus
current grant/case checks inside the Identity transaction. No runtime self-grant.

## Not a blanket implementation grant for new contracts

Claims/Change Request APIs, evidence-purpose and commercial SLA policy are absent.
Ticket V1 is a real Local/Test workflow, separately approved. No
Merchant evidence uploader reused for claims, no compensation/payment/Carrier
mutation. Exact new contracts require Owner review under AGENTS.md. Do not mark
those live capabilities DONE from presentation work.

## Delivered behavior and scope

- Personal Support is a peer menu page. Default view is own Ticket list with
  server-side state/category filters and20-record paging; guidance is a separate
  searchable view, not a duplicated list or mock contact form.
- General or confirmed-own-Shipment intake; immutable binding, category, subject,
  paragraph-safe Vietnamese conversation. No arbitrary priority/SLA/client actor.
- Customer replies in NEW/IN_PROGRESS/NEEDS_INFORMATION; resolved review requests
  stay RESOLVED until assigned Staff accepts review. Resolution acknowledgement
  closes the Ticket only. CLOSED conversation remains readable and immutable.
- Every committed version has message, private audit and receipt in the same
  physical transaction. Same-key replay does not create another effect/quota or
  extend idle. Lost Customer writes use scoped GET receipt before a current read.
- DB throttles5 new Tickets and20 conversation actions per account/15min. Staff
  commands require a current grant AND active case assignment. Triage assignment
  returns metadata, not conversation of an unassigned case. No Customer state PATCH.
- Avatar itself opens the account disclosure. Outside click, focus departure,
  Escape and route/query changes close it. Distinct React keys prevent duplicate
  Workspace controls after navigation. No duplicate sidebar Account tree.
- Settings reads the actual saved preferredLanguage; this is not a translated UI.
  Profile contacts remain unverified/noncredential. Existing logout/OTP reset is
  reused; MFA/devices/security history/email change/closure are not fabricated.

## Observed verification

- `npm.cmd test`:104/104 PASS; typecheck and lint PASS.
- Customer UI command against customer-ui, management-ui, support-account-ui and
  support-tickets-ui:70/70 PASS, including360–1440px disclosure, source failure,
  wrong target/stale generation, unknown-write receipt and literal hostile markup.
- Default and Customer builds PASS. Customer bundle size warning remains; no
  unrelated bundling refactor or warning suppression.
- Core PG run: existing54 migration cases,5 Ticket HTTP/security/transaction cases
  and23 architecture cases PASS. Separate new V21→V22 upgrade1/1 PASS after fixing
  the synthetic pending-account fixture; V1–V21 checksums/data remain unchanged.
- Actual HTTPS/PostgreSQL18/test-SMTP Browser15/15 PASS and parent1/1 PASS. Includes
  Customer→Staff assignment→information request→reply→resolution→review→reopen→
  final resolution→close, lost create receipt, private labels/intake/UNPAID, RF-05,
  profile and recovery regression. No real Customer/Staff fixture seed in local DB.
- New Browser steps initially overused document reloads and hit existing refresh
  HTTP429 before RF-05. Replaced unnecessary reloads with actual conversation
  refresh buttons. Rate limits, assertions and security policies were not weakened.
- Report-policy run:129/130 PASS; only exact inventory count54 needed55 after the
  added migration case. Corrected count and reran that precise test:1/1 PASS.
  All130 distinct cases pass across these runs; not claimed one full130 green run.
- No remaining source test failure. An interrupted run and a later stopped Docker
  Engine prevented execution; after Owner restored Docker, the exact upgrade gate
  passed. No code change was used to mask that infrastructure failure.
- Final focused fail-closed regression1/1 PASS: even with TEST environment and
  enabled flags, an active `production` profile denies Ticket capability. Focused
  generated-credential and recorded-resource absence audit also PASS.

## Local runtime

Official launcher stop/start: no owned processes required stopping; original
`elogistics-d1-postgres18` reused, healthy, Flyway V22 successful and TLS trust
preserved. `LOCAL_FULL_STACK_HTTP_READY` observed. Metadata confirms V21/V22 success
and zero seeded Support Staff grants/Tickets. Local frontend paths support,
settings and operations/support return200; both protected Ticket/Staff APIs deny
anonymous access401. No Gmail live delivery or AWS live upload is claimed here.

Customer: HTTPS origin `https://127.0.0.1:5173`, path `/personal/support`.
Account: avatar → profile/security/settings. Existing verified login is required.

No actual Staff account was automatically created or granted rights. Explicit
Owner/operator setup for an existing verified Customer UUID, if needed:

```powershell
.\scripts\dev\support-staff.ps1 -AccountId '<verified-account-uuid>' -Permission SUPPORT_TRIAGE
.\scripts\dev\support-staff.ps1 -AccountId '<verified-account-uuid>' -Permission SUPPORT_HANDLE
```

The operator procedure is Local/Test-only, audited by DB principal and grant ID;
runtime cannot call it. Use `-Action REVOKE` to revoke the specified permission.
Staff opens `/operations/support`; an assigned handler may open its case URL.
This is a bounded workbench, not a full Staff directory/admin/support-SLA console.

SA-01–07 Local DONE. Source validator errors[]/CONSISTENT, diff whitespace check
PASS, zero staged changes and zero changes to tracked V1–V21 migration files.
Original66 source paths remain preserved with verified SHA-256. Final87 working
source paths are copied and hashed under `.demo/support-account-review/final-source`
with `final-manifest.json` and tracked `final.patch`. This archive has no Git repo,
runtime credentials, real PII or private diagnostic artifacts.

Working branch remains `feature/argopost-overview-completion`, unchanged HEAD
`59648b4bf4ba02ca069bd0fbf9529889134f3d59`. No commit, push, PR, merge, new worktree
or WIP discard. Required hosted integration CI remains a future publication gate.
Next Owner task is local review of the implemented Ticket V1/account surfaces;
Claims and other missing Account workflows need their own accepted contracts.

Focused evidence privacy audit PASS for core, Browser and final upgrade runs,
including UTF-8/UTF-16 generated credential scans. Reserved/observed/created/
verified/stopped identities, approved image digests, run/session labels and
current recorded container/volume absence verified read-only. This is explicitly
a focused local audit; the CI all-suite collector was not relaxed or fed invented
records for suites that did not execute. Original developer PostgreSQL was not
targeted by test cleanup. Evidence metadata: `.demo/support-account-review/`.

Identity has actual enrollment/recovery/reauthentication/owner-consent runtime,
but no current security-email-change transport, phone credential, MFA/device list,
public security history or account closure API. Candidate003 wire is not runtime.
Personal profile optional contacts stay unverified/noncredential; updating them
cannot change verified login email. Profile preferredLanguage is persisted by the
existing contract; it is not a promise of a translated interface.

Terms/Privacy legal text is still Owner-pending. No invented policy, request
receipt, support email/hotline or notification consent. Only requested transaction
emails are supported today; SMS/Push/marketing preferences are not simulated.
Business context/membership reads are not delivered; existing grey disabled
Business switcher and backend denial remain, without fake inactive memberships.
