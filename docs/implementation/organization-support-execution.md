# Organization Support V1 — source/test execution

Authority: Owner technical GO binds manifest SHA-256
`9484c415356e601ee8192e84a09af2e39f2f448d07244457de1ef79a09c3a940`.
The manifest remains byte-for-byte unchanged as the approved review artifact.
Its historical pending fields are superseded by this explicit Owner decision.

## Boundary and task status

- [x] OSC-03b exact technical approval verified against the manifest hash.
- [x] OSC-04 Backend/API/PostgreSQL isolated-test gates — DONE.
- [x] OSC-05 actual Ticket UI and HTTP/Browser integration — DONE.
- [x] OSC-06 authorization, concurrency, privacy and regression checkpoint — DONE (source/test only).

First slice: Ticket creation and Merchant/CSKH conversation. Source files:
`OrganizationSupportTickets`, `SupportCasePolicy`, `JdbcOrganizationSupportTickets`,
`OrganizationSupportConfiguration`, `OrganizationSupportHttp`, and V35. Tests are
added beside the existing actual Customer HTTP/PostgreSQL fixtures. Subsequent
slices cover internal assignment, whole-cohort reports and in-app notifications.
No DONE is claimed for an unchecked gate.

## Preservation and migration allocation

Root branch `feature/argopost-business-shipping-tools`, baseline
`c8cdfa335e31596116cf0d7626b81ef0d40dee78`. Before Support edits, all 89 WIP paths
were archived with per-file SHA-256 and archive read-back verification in a private
ignored `.demo/organization-support/checkpoint-*` directory. No reset/clean/stash,
worktree creation, source discard or local database mutation.

V34 Team is already applied to the demo. Therefore the new forward migration is
**V35**, not the historical V33 placeholder in the manifest. V1–V32 and V34 remain
unchanged. V35 runs only in disposable PostgreSQL 18 test databases. No localhost
Support migration, grant, restart or deployment is authorized by this checkpoint.

## Explicit compatible refinements

1. DB07 receipt uses exclusive typed TICKET/PREFERENCES/NOTICE variants with real
   same-Organization foreign keys, including self notice recipient binding.
2. DB08 audit gains typed preference/notice keys and a composite receipt FK binding
   Organization, actor, operation, resource and prior/result version. HTTP request
   ID remains distinct from the idempotency key.
3. DB11 notice uses exclusive typed message/assignment-event keys with composite
   Organization/Ticket FKs. Source event must be written in the same transaction;
   it becomes visible atomically with the event. No external delivery is claimed.
4. Owner explicitly approved correcting OSC-AMEND-02 to the actual existing
   `merchant.configure_local_workspace(...)`, retaining its signature, existing
   allowlist, audit and idempotency; no nonexistent alternate operator is created.
5. Owner explicitly approved widening `merchant.member_grant.permission` from
   VARCHAR(24) to VARCHAR(32) for the 28-character approved permission
   `support.notifications.manage`. Exact CHECK allowlists remain enforced.
6. Owner explicitly approved `CustomerMemberNames.eligibleAccounts(context, ids)`:
   read only, Merchant-discovered IDs, existing Identity admission/administration
   lock and canonical Customer eligibility. This removes the admission's 100
   subject-lock limit from candidate discovery. Eligibility is filtered before
   pagination; an actual assignment still locks and revalidates its target.
7. DB07 has an additional partial unique key for Carrier actor/operation/key:
   the approved Staff receipt URL does not carry Organization, so reuse across
   Organizations is rejected instead of choosing an arbitrary matching receipt.
   The receipt still binds the exact Organization/resource in its FK and HMAC.
   A second unique key permits only one receipt per actual target/version change,
   keeping Ticket lifecycle and internal assignment version streams separate.

No new relation, HTTP operation, permission, provider or Team delegation ceiling
is added beyond the approved scope. Support permissions are never auto-granted.

## Implemented source — 2026-11-18

Orders implements the approved typed API and twelve-table V35 stream. Workflow
joins current Customer admission, Merchant scope and existing canonical Support
Staff authority in one transaction. Ticket lifecycle, Merchant assignment and
Carrier case assignment are distinct. Every write has version/idempotency,
typed receipt/audit and mandatory committed event evidence. Direct-DML guards
reject forged targets, events, authority and incomplete transactions.

Merchant UI includes general/Shipment-linked Ticket creation, paged filters,
conversation, review/closure, eligible internal assignees/history, whole-cohort
reports and self notifications/preferences. CSKH uses the existing workbench
with an explicit Organization binding. Search text stays in a CSRF-protected
POST body; source-order reference is read from the immutable preparation version
bound at Booking. Permission labels recognize Support but Team templates and
delegation ceilings do not grant these new rights.

Frontend DTOs validate exact Organization/resource/page/version and projections.
Unknown write outcomes retain the original key and recover the committed receipt;
they do not blindly submit another command. Native select accessible names are
stable independently of option content. Reports/notices hide stale cohorts/pages.

## Executed evidence (isolated test databases only)

| Gate | Observed result |
| --- | --- |
| `IdentityGrantApplicationTests#organizationSupport*` | 9 cases verified PASS across focused runs; lifecycle, replay/quota/rollback, notices, tenancy/scope/revocation, direct-DML, strict HTTP/Production denial, immutable Shipment binding/source-reference search, >100-member eligibility before pagination, schema/default-off |
| `IdentityGrantApplicationTests#supportTicket*` | 5 existing Personal Support cases PASS |
| `IdentityGrantApplicationTests#teamMembershipLifecycleReplaysConsumedProofAndPreservesOtherAccess` | 1 PASS, including Support revocation with offboarding |
| `IdentityMigrationTests#organizationSupportV35PreservesV34DataChecksumsAndTeamCeilings` | 1 PASS; V34 data/checksums preserved, V35 once then no-op, no Team ceiling expansion |
| `PostgresTestcontainersTests` | 7 PASS; actual PG18, complete 34-migration/131-table inventory, privileges, validate/restart/no-op |
| `SupportCasePolicyTests`, `OrganizationTeamPolicyTests` | 4 + 4 PASS |
| Architecture positive/negative checks | 23 PASS |
| `customerEntryBrowserUsesActualHttpsPostgresAndSmtp` | Final Java gate 1 PASS; nested Browser 19/19 PASS, actual HTTPS + owned PG18 + test SMTP |
| Frontend unit tests: Support data/navigation and Team data/capabilities | 15 PASS |
| Frontend UI tests: Support Hub and Ticket | 11 PASS; filters, lost response, selectable assignment and 390/1440px |
| Frontend `typecheck`, `lint`, `build:customer` | PASS; build has existing chunk-size warning |
| Report-policy unittest | 132 PASS, native process exit 0; finite test inventory updated, no skips/allowances weakened |
| Canonical `validate.py --implementation` | PASS, exit 0, zero errors, semantic status CONSISTENT |

Private ignored evidence retains the original failed runs as well as corrections:

- `verify-ac37e742-3274-4bb9-8112-1a24deba0d63`: 22/23 PASS; new source-reference fixture omitted its required source-channel pair. Fixed fixture, not business validation.
- `verify-751dccc0-2038-49e2-9b0e-9825d4d4a83f`: metadata 7/7 and upgrade 1/1 PASS; corrected source-reference test PASS. Browser 19/19 passed, but Java final session assertion found two new test actors not logged out. Real UI logout was added; the zero-active-session assertion remains unchanged.
- `verify-5c1d6964-8d1c-484a-ae22-462cea8e9327`: an existing long Facility/Team journey hit its 25-second budget while other frontend work ran. No business assertion or timeout was relaxed; cause of that single timing failure is not claimed proven.
- `verify-89bc2a17-c0ce-4df3-8393-42e33150f90c`: final sequential Browser/Java gate PASS, 19/19 journeys and 1/1 Java postcondition test. One CLOSED/version10 Organization Ticket, ten messages, one internal assignment, thirteen receipts and audits, one read notice, and zero active test sessions are asserted in PostgreSQL.

Synthetic screenshots were inspected at desktop/mobile and on the actual
committed conversation. No horizontal overflow or overlapping controls observed.
The original 89-file archive is intact with 89/89 matching SHA-256 fingerprints;
the approved manifest hash and V34 bytes match, with no tracked historical
migration diff. Source manifest/traceability validation passed. V35 source SHA-256:
`33e7e8b532de598627c321a454fd293157fb6c3a365cede946a03c3267d4844b`.
Final working tree has 105 paths; all original 89 source paths still exist.

Original Team evidence remains unchanged: original full PostgreSQL run had one
FAIL; focused corrected rerun 10/10 PASS. No full-suite or hosted GitHub CI PASS is
claimed by this Support checkpoint.

Localhost deployment/grants/UAT remain a separate Owner checkpoint. No demo DB,
credentials/certificates, local processes or global configuration were changed.
No Git commit/push/merge was performed; HEAD remains `c8cdfa3` on the original
feature branch. Claim/Compensation, Evidence upload and Shipment/Finance mutations
remain outside this implementation.

## Subsequent Owner checkpoint — V35 Local/Test schema only, 2026-11-18

Verdict: **LOCAL_SUPPORT_V35_SCHEMA_READY**. Owner separately authorized read-only
preflight, verified backup/restore and forward-only V35 on the existing demo.
This supersedes the source/test-only deployment boundary above solely for schema.
No Merchant Support grant, Staff binding, feature enablement or runtime UAT is
authorized or performed by this schema checkpoint.

- [x] Exact container/volume/database/environment and current V34 identified.
- [x] Source/approved manifest and all 33 historical Flyway checksums verified.
- [x] Pre-migration backup created and actual isolated recovery verified.
- [x] Existing data/schema/operator/permission compatibility verified.
- [x] V35 applied through the official one-shot migration profile, target35 only.
- [x] Flyway post-validate, data/catalog/ACL/guard and disabled-feature checks PASS.
- [x] Private backup/evidence retained; owned restore helper resources cleaned.
- [ ] Separate exact grants/bindings/enablement/UAT checkpoint — NOT AUTHORIZED.

### Verified environment and recovery

| Item | Observed value |
| --- | --- |
| Database endpoint | `127.0.0.1:5433/elogistics_dev` |
| PostgreSQL | 18.6 |
| Container | `elogistics-d1-postgres18` |
| Exact container ID | `965042a98c43043378b8757a13cb4364a063a289c59e35f43ee7c9eeac761a63` |
| Existing volume | `elogistics_d1_pg18_dev_data` |
| Cluster system identifier | `7696150062350938155` |
| Image digest | `sha256:afc7e2d441324c0388fa80c3d24f733b4194a4eb7f47dd8ee2b08eb1a24a647c` |
| Organization | NamShop Demo `782c4766-8bdd-435b-9961-512c831922d6`, ACTIVE / LOCAL |
| Initial Owner Account / Membership | `5d206ef3-350e-42b7-af8a-4c54b8d43fe2` / `d34e1afe-4d3d-4e6e-bd63-449ddca5f92f` |

The original container was stopped, with the same ID/image/volume/loopback port.
Before starting it, the entire stopped volume was archived via a read-only mount.
A network-none, no-published-port restore copy recovered the backup into tmpfs;
all 119 current tables were readable. Its database/system ID/history and ACTIVE,
verified Customer/immutable ACTIVE OWNER binding matched the expected demo.
Only after this recovery/compatibility gate did the exact original instance start.
Live history, catalog and all119 table fingerprints matched the restored copy.
No container recreation, volume replacement or database reset occurred.

Backup: **206602240 bytes**, SHA-256
`f7583e07eaf718775f6aa5565321d98b9b7268bb87895e5609192a4eda638368`.
Private ACL evidence/backup directory is recorded in
`.demo/organization-support/local-schema-path.txt`; it is excluded from Git.
The source checkpoint separately preserves all105 current WIP paths with
archive read-back SHA-256 verification. No source was discarded.

### Migration and integrity evidence

Before migration, history was exactly1–32,34 (33 successful rows). All script
names/checksums matched current source; V34 checksum remained667729765. Neither
source/artifact nor history contained V33. A separately packaged migration JAR
matched all34 source scripts byte-for-byte, including the approved V35 SHA-256
`33e7e8b532de598627c321a454fd293157fb6c3a365cede946a03c3267d4844b`.
The runtime JAR was not replaced. Read-only Flyway validate at target34 PASS.

The official `migration` profile ran once with target35, migration principal
`elogistics_migrator` and `SET ROLE elogistics_owner`. It exited0 and the job
closed after validation. Post-migration validate-only at target35 PASS, with
**34 applied migrations; V35 appears exactly once, checksum -599636176**.
No clean/repair/reset, manual schema workaround or historical script edit.

Observed read-only integrity results:

- **118/118 pre-existing data tables unchanged**. The only changed old table is
  `platform.flyway_schema_history`, with exactly its one V35 row added. This
  includes Identity, Membership/Team grants/roles/ownership, Personal Support,
  Business Shipping, Pricing, Finance, Carrier and existing audit/receipts.
- Exactly **12 new Orders Support tables**, all empty; approved manifest columns
  present. No Ticket/message/quota/audit/notice/preference/operator evidence seeded.
- Added14 functions,23 enabled non-internal triggers, and226 catalog constraints
  (including PostgreSQL18 NOT NULL entries); all new constraints validated.
  Typed same-Organization FK/receipt/event guards and runtime append-only/operator
  ACL protections are present. Runtime/PUBLIC cannot execute the new offline
  Staff-binding operator; runtime cannot read/write its private operator audit.
- Exactly four existing function/constraint definitions changed: the two approved
  workspace/facility operator definitions and the two member-grant CHECKs.
  The permission column widened24→32. No other existing catalog definition changed;
  all119 existing table owners/ACLs were preserved.
- The exact five Support permission values are supported in the CHECK allowlists,
  with ORGANIZATION/FACILITY scopes as approved in OSC-PERM-01–05. Owner role
  ceilings still exclude Support; **Support grant delta0, Staff bindings0**.
- Four protected runtime/PostgreSQL/certificate file hashes unchanged; all105 WIP
  source fingerprints matched before these documentation updates. The migration
  child used existing credentials without printing them or changing global env.
- `support.tickets.organization-enabled` remains absent/defaultfalse; no launcher
  enablement or runtime restart. Existing Personal Support configuration is not
  modified. This is Organization Support schema readiness, not Ticket activation.

The helper's first read-only metadata query used an incorrect column spelling;
it was corrected to the existing `admission_origin` before original startup.
A post-check initially expected notifications permission to be Organization-only;
the frozen manifest explicitly approves FACILITY too. The checker was corrected
to use the manifest's exact five scope definitions. Neither correction altered
the database, guards, migration or approved business/security contract.

The restore helper was shut down and removed after verification. Its image-created
anonymous volume was separately verified empty and unrelated to the demo, then
removed; the original named volume and backup remain. Rollback was not executed.
Any future destructive in-place restore needs a separate bounded decision.

No Backend/Frontend startup, Ticket Browser UAT, grants/bindings, Production,
commit/push/merge or new worktree. The existing source/test evidence and original
full PostgreSQL one-FAIL/focused10-PASS provenance remain unchanged; no full-suite
or GitHub CI PASS is claimed by this deployment.

## Subsequent Owner activation checkpoint — Merchant grants only, 2026-11-18

Verdict: **STAFF_BINDING_APPROVAL_REQUIRED**. Owner approved the exact Merchant
permission delta independently of Staff confirmation. Merchant grants completed;
Staff binding, feature activation and Ticket UAT stopped at the explicit boundary.

- [x] Fresh exact DB/V35/checksum/ACTIVE OWNER preflight — PASS.
- [x] Snapshot and recoverable backup of the existing V35 state — PASS.
- [x] Exact five Merchant Support rights, audited independent operator — DONE.
- [x] Grant delta, idempotent replay and data/config/source preservation — PASS.
- [x] Read-only canonical Staff discovery — DONE, zero grant rows found.
- [ ] Staff bindings — awaiting exact current canonical Staff authority/Owner confirmation.
- [ ] Feature/runtime activation and HTTP/Browser Ticket UAT — not executed.

### Preflight and backup

The database/container/system ID and exact Organization/Account/Membership were
reverified against the schema checkpoint above. Organization ACTIVE/LOCAL,
initial Membership ACTIVE/OWNER, Account ACTIVE/email verified/credential and
exact Customer identifier present. All34 applied migration script names/checksums
match source, including V35=-599636176; no migration was rerun or changed.

All105 WIP paths were archived and read-back verified. A fresh custom-format
`pg_dump --create` of `elogistics_dev` plus a private `pg_dumpall --globals-only`
backup preserves the state containing V35 before any grant. Database dump:
959415 bytes, SHA-256
`2b0ea8c2ee650e727c164095819ede3b35effacf0d01da952c5039974979cc97`.
Globals and database backups contain sensitive recovery material; both remain in
the ACL-restricted ignored directory recorded by
`.demo/organization-support/local-activation-path.txt`, never Git or CI artifacts.

Actual restore ran in a network-none container, no published ports, with tmpfs
storage and a read-only backup mount. Two restore dependencies were handled
without disabling constraints or changing the original database:

1. PostgreSQL18 explicit role-grantor semantics require the restored cluster to
   use the same initial `elogistics_bootstrap` principal. The initial attempt with
   a different bootstrap principal failed. The successful rehearsal initializes
   with the original principal, omits only its already-existing CREATE ROLE line
   from the restore input, and retains its ALTER attributes/password/ACL content.
   The original globals backup is unchanged.
2. `pilot_quote_exact_breakdown` reads tariff data through a CHECK function.
   Ordinary table-data order attempted Quote before Tariff and failed. A
   `pg_restore --use-list` TOC restores Tariff data first; no CHECK was disabled,
   guard bypassed, source migration edited or row altered to obtain success.

The successful restore matches all131 table fingerprints, including V35 history.
Catalog comparison covers2770 functions/constraints/triggers/indexes/ACL entries.
Functions, triggers and ACLs match exactly. PostgreSQL deparse round-trip forms
were explicitly reconciled: elementwise varchar-array→text casts, AND association
in two exact named CHECKs, and five generated NOT NULL-name suffixes on the same
table/column with identical validation. No unexplained semantic difference remains.
The owned restore container was removed after verification; it used no original
volume or persistent helper volume. Backup files remain available.

### Exact Merchant grant result

Authorized target remains NamShop Demo
`782c4766-8bdd-435b-9961-512c831922d6`, Account
`5d206ef3-350e-42b7-af8a-4c54b8d43fe2`, initial Membership
`d34e1afe-4d3d-4e6e-bd63-449ddca5f92f`.

Independent operator `elogistics_migrator`, `SET LOCAL ROLE elogistics_owner`,
LOCAL mode, called the existing `merchant.configure_local_workspace` procedure.
No runtime/self-grant route or direct grant-table DML was used. Request:
`9400d8a1-9815-45a5-a513-5fa788797cf5`.

Exactly five new grants, all ORGANIZATION scope, null Facility, this Membership:

```text
support.read
support.write
support.assign
support.reports.read
support.notifications.manage
```

All26 existing grant rows (including other memberships) retain their exact IDs,
permissions and scopes. The procedure-required existing `dashboard.read` was
passed as a base permission and was not duplicated. No other account/Organization
received a grant. Membership identity/role/state and Organization Ownership are
unchanged; required version/authority counters advanced7→8 once. No pending Team
invitation required invalidation. One workspace operator audit and one receipt
were committed. Exact same-key/input replay returned the same Membership and
added no data, grant, audit, receipt or counter increment.

Of131 tables, only the four expected ones changed: `merchant.member_grant`,
`merchant.membership`, `merchant.workspace_audit`,
`merchant.workspace_command_receipt`. Other127 table fingerprints are unchanged,
including Flyway history, Identity, Team data/grants, Personal Support, Business
Shipping, Finance and all Organization Support resources. Other Membership rows
are unchanged; no Support Ticket/audit/notification was seeded.
Four protected runtime/PostgreSQL/certificate hashes and all105 WIP source
fingerprints matched before these documentation updates. No credentials/global
settings, runtime artifact, feature flag or process configuration changed.

### Staff discovery and single remaining blocker

| Discovery item | Actual result |
| --- | --- |
| Canonical `SUPPORT_TRIAGE` rows | 0, including ACTIVE/REVOKED |
| Canonical `SUPPORT_HANDLE` rows | 0, including ACTIVE/REVOKED |
| Staff Account / canonical Grant IDs | None discoverable from the canonical registry; no account selected |
| State / Identity eligibility | Not applicable without an existing canonical principal/grant |
| Organization Staff bindings | 0; no change |
| Intended UAT responsibilities | Confirmed TRIAGE principal assigns cases; confirmed HANDLE principal reads/replies/transitions only assigned cases |
| Conditional expected change | Only Owner-confirmed existing eligible canonical grant IDs bound to NamShop Demo; no new global grant or other-Organization scope |

**Single blocker:** no existing current canonical Support Staff authority is
available for an exact Owner confirmation. An Identity/Customer/Merchant Owner
account is not selected or promoted as a substitute. No Staff Account or
SUPPORT_TRIAGE/HANDLE grant was created. Binding/activation remains stopped;
Owner must separately establish/confirm the exact Staff principal and canonical
authority before the conditional steps can proceed.

`support.tickets.organization-enabled` remains absent/defaultfalse. No stale JAR
was launched, no runtime replacement/restart or feature enablement attempted.
HTTP/Browser Ticket UAT and live Staff/Membership revocation journeys are
**NOT RUN** at this blocked boundary, not PASS. Privacy evidence consists of
read-only aggregate/fingerprint checks and exact admin grant readback; sensitive
backup contents and Customer contact details are excluded from this report.
No repeat of OSC-04/05/06, migration V35, full PostgreSQL suite or GitHub CI.
No commit/push/merge, worktree creation, rollback or database reset occurred.

## Owner-directed read-only Staff candidate discovery — 2026-11-18

Verdict remains **SECURITY BLOCKED — STAFF AUTHORITY NOT YET APPROVED**.
The explicitly authorized single discovery pass is DONE; no provisioning follows.

At2026-11-18T09:06:36Z, a REPEATABLE READ / READ ONLY transaction inspected the
same healthy original container and `127.0.0.1:5433/elogistics_dev`, system
ID7696150062350938155. History still has34 successful migrations ending V35,
checksum -599636176. The five approved NamShop Merchant Support grants remain
ORGANIZATION scope with null Facility; they were not reapplied.

Discovery reads only UUIDs, status/eligibility booleans and role/scope metadata;
it does not export email, display name, phone, password hash, proof or session.
Existing Staff signals considered: Identity account roles/scopes, Station Staff
assignments and canonical Support grants. An eligible Customer or Merchant
employee is not silently classified as Carrier CSKH.

| Inventory | Actual result |
| --- | --- |
| Identity accounts |3 |
| Merchant Owner excluded |1: `5d206ef3-350e-42b7-af8a-4c54b8d43fe2` |
| Remaining eligible Customer-only accounts |2; no Staff role/assignment/grant, not nominated or repurposed |
| Existing Staff-signal candidates |0; no Staff Account UUID available to nominate |
| Canonical SUPPORT_TRIAGE / SUPPORT_HANDLE grants |0 /0, including revoked records; no Grant IDs/state/eligibility to enumerate |
| NamShop Organization Staff bindings |0 |
| SYSTEM realm |`3e0f9db7-fa1a-4d8d-a0ca-db50a10a5dc0`, INACTIVE; not activated or proposed as a shortcut |

No suitable existing Staff candidate was found. Proposed next authority decision
only: a dedicated, verified Local/Test CSKH identity, separate from Merchant Owner
and current Customer-only accounts, using the existing Support V1 admission flow.
Its UUID must come from a separately approved provisioning checkpoint; none is
invented or reserved here. No Merchant Owner/Manager, Team, Station/Courier,
Finance/COD, Platform Admin or Production role is needed.

Least-privilege responsibilities are explicit:

- `SUPPORT_TRIAGE`: queue/triage and case assignment; does not authorize replies
  or case-resolution transitions.
- `SUPPORT_HANDLE`: read/reply/transition only an actually assigned case, with
  current Identity eligibility and the exact Organization binding; not triage.
- The current Browser workbench's "Nhận xử lý doanh nghiệp" action assigns to
  the authenticated Staff itself. For that existing full UAT path, a dedicated
  Staff needs both explicit grants; one permission does not imply the other.
  An assigned handler alone needs HANDLE only. Separate principals are possible
  through the approved assignment API, but no UI/authority change is made here.

Expected changes, all **pending separate Owner approval**, are exact canonical
grant UUIDs/account/permissions/state/effectivity, followed only after ACTIVE
verification by NamShop-specific bindings to those actual grants. No global grant,
other-Organization scope, Merchant Membership or feature enablement is implied.
No Staff target selected; no identity, canonical grant or binding created.

Private sanitized evidence:
`staff-candidate-discovery-1dee1093-4527-452a-b021-4d99c8bcfd58.json` under the
existing private directory pointer `.demo/organization-support/local-activation-path.txt`.
No Merchant grant replay, backup/restore, migration, source/test, runtime restart,
Browser UAT, credential/certificate/global setting change or Git operation beyond
read-only status/HEAD inspection occurred in this discovery checkpoint.

## Dedicated CSKH identity/canonical authority — mechanism gate, 2026-11-18

Verdict: **SECURITY_BLOCKED — CANONICAL_STAFF_PROVISIONING_REQUIRED**.
Owner authorized one dedicated verified Local/Test CSKH identity and the two
existing canonical Support grants, conditional on an already-approved executable
provisioning/verification/operator mechanism. The mechanism preflight is DONE;
provisioning and grant execution are BLOCKED before any database mutation.

### Observed Local/Test preflight

Read-only transaction at2026-11-18T09:23:36Z reidentified the same healthy original
container/database/system ID. All34 history rows successful; latest V35 checksum
-599636176. SYSTEM realm remains INACTIVE. Eligible current Identity Management
actors0; Identity accounts3; canonical Support grants0; Organization Staff
bindings0. The five NamShop Merchant Support grants remain ORGANIZATION-scoped.
Private evidence is `canonical-staff-provisioning-preflight-3b58fe44-4208-4526-9a65-30b1ce031e42.json`
under `.demo/organization-support/local-activation-path.txt`. No credential,
proof, Customer contact or password hash is exported.

### Exact implemented mechanisms and unmet requirements

| Item | Current implementation / authority | Why this approval cannot yet be executed |
| --- | --- | --- |
| Account creation | `ProvisionIdentityAccount.java:27–43` requires an authenticated admitted management-admin session; target UUID is correctly server-generated | No eligible actor exists; no approved independent Local/Test Staff provisioning authority is implemented |
| Provisioning policy | `IdentityRealmGrantPolicy.java:10–20`, `JdbcIdentityRealmGrantAuthority.java:43`, accepted ADR-0010 ordinary-provisioning extension | Requires registered ACTIVE SYSTEM realm and the exact initial IDENTITY_MANAGEMENT pair; cannot create a Support-only Staff while preserving INACTIVE SYSTEM/no Platform Admin authority |
| Real invitation verification | `JdbcIdentityInvitationStore.java:31–34` restricts acceptance to that same ACTIVE SYSTEM/exact management pair and protected invitation outbox | Cryptographic invitation/password/receipt components exist, but no approved dedicated Local/Test non-admin Staff verification path is wired by the current policy |
| Staff runtime admission | `OrganizationSupportHttp.java:39` uses existing CustomerAdmission plus separate canonical Staff grants/Organization binding | SYSTEM invitation provisioning is not automatically eligible for this admission. Public Customer enrollment followed by a Staff label is not substituted for the Owner-required Staff workflow |
| Canonical grant issuer | Existing `orders.configure_local_support_staff(uuid,text,text)` / `scripts/dev/support-staff.ps1`, V22:72–89; operator-only, existing verified-account prerequisite, actual session_user/time/Grant ID audit | This function DOES exist and state-idempotently returns an already ACTIVE grant without another audit. It has no request ID/input-digest/command receipt binding; its operator audit variant explicitly stores request_id NULL. It cannot supply request-bound grant receipt/replay evidence across intervening state changes without a separately approved amendment |

The live catalog confirms argument names `target, permission_name, action`,
SECURITY INVOKER, owner `elogistics_owner`, and runtime EXECUTE denied for the
existing canonical grant operator. It was inspected, not invoked. A privileged
schema-owner INSERT/verification update, manufactured session, injected authority
adapter, synthetic SMTP inbox or public Customer-registration relabel would not
repair the missing approved mechanism and was not used.

### Bounded technical changes requiring separate review, not implementation

1. Extend the existing Identity provisioning/invitation policy and trusted
   Local/Test operator admission for a dedicated non-admin CSKH principal, with
   server-generated UUID, real controlled-mailbox verification/password lifecycle,
   explicit Staff purpose and compatible Support V1 admission. Preserve INACTIVE
   SYSTEM, no new authentication system, no Merchant Membership or other roles.
2. Extend the existing canonical Support grant authority with durable request/
   payload binding, receipt and audit correlation for exactly SUPPORT_TRIAGE and
   SUPPORT_HANDLE. Preserve existing grant storage/codes and least privilege;
   any API/DTO/operator/schema delta must have an exact approved manifest first.
   Use a new forward migration if required; never edit deployed V22–V35.

These are newly identified Identity/operator security amendments, not another
approval of Organization Support APIs/DTOs/tables or its accepted business scope.
No new API, DTO, SQL function, migration, authority policy or alternate grant
engine is implemented by this checkpoint.

### Delivery state

- Staff Account UUID: **not issued**, no pending account or activation proof created.
- SUPPORT_TRIAGE/HANDLE Grant UUID/state/scope: **not issued**; no existing grant changed.
- New provisioning/grant audit/receipt: none; no replay execution claimed.
- Database mutation delta: none; preflight uses an enforced READ ONLY transaction.
- Merchant grants unchanged; Organization Staff bindings0; feature remains default-off.
- No Staff selection, direct DML, SYSTEM activation, recovery/migration rerun,
  runtime artifact/restart/config/credential/certificate change or Ticket UAT.
- No source/full-suite/Browser tests were repeated; no Git commit/push/merge.

No new recovery point is created because the mandatory authority gate stops this
run before mutation. Previously verified backups remain preserved; a fresh
recovery-point suitability check belongs before any later authorized write.
