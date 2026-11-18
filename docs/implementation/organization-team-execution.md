# Organization Team V1 — approved execution

Owner technical GO WITH CONDITIONS received2026-11-17 for immutable manifest
SHA-256 `f6e21ee3ec3f742035d37f2c10d624031cc450a9b590939df878e9397952f426`.
The approved JSON is retained byte-for-byte as the approval reference; its
historical pending fields are not rewritten to suggest execution evidence.

OTA-03 DONE: APIs01–15, DTOs01–20, permissions01–05, tables01–05,
amendments01–04 and clarifications01–06 explicitly accepted by Owner.

Plan for OTA-04/05:

1. Preserve current37-path WIP checkpoint; add V34 forward-only Team schema and
   direct-DML invariants, pure delegation/lifecycle tests. V1–V32 immutable.
2. Implement canonical Merchant Team API/repository, existing Identity
   admission/reauthentication and protected-delivery integration, bounded HTTP.
3. Connect Organization Team UI and verified recipient decision page; test
   backend/PG18/HTTP/Browser/security/privacy with synthetic fixtures only.

V34 is the actual source allocation: no V33 file exists; the unapproved Support
proposal reserved that number. No V33 placeholder or Support implementation is
created. Any future Support migration must use the then-current ordered stream;
do not introduce a lower migration into an already-upgraded database.

Boundary: isolated PostgreSQL18 test databases only. No localhost migration or
grant, credentials/certificates/global environment changes, demo data mutation,
Production activation, Owner transfer, additional roles, commit/push or merge.

- [x] OTA-03 exact technical approval recorded.
- [x] OTA-04 approved core source and working vertical slices.
- [x] OTA-05 local source/test checkpoint, including the explicitly recorded
  fixture-only failure and successful targeted rerun below. This is not a claim
  that the original full Maven invocation or hosted CI passed.
- [x] Separate Owner Local/Test GO received for V34 and exact NamShop Demo Initial Owner grants.
  Deployment/read smoke evidence and pending write UAT are in the
  [Local/Test checkpoint](organization-team-local-checkpoint.md).

## Delivered core

- All15 approved HTTP operations and20 DTOs: current Organization/Membership/RBAC,
  directory/search/pagination, role ceilings, invitations, recipient decisions,
  lifecycle, access replacement, audit and actor-bound receipts.
- Five explicit ORGANIZATION Team rights, independent operator grants only.
  Initial Owner/identity/version and terminal revocation remain protected.
  Manager can invite only Operator within the actual delegated scope; no Finance,
  Platform or Carrier escalation. Existing grants outside V1 ceilings are not
  silently stripped.
- Invitation72h, verified exact recipient, single-use, resend revocation, offline
  inviter/policy invalidation, protected contact/outbox and existing SMTP worker.
  No network send inside the Membership transaction. Same-key replay survives
  consumed proofs/old ETags while rechecking current admission and input digest.
- Team UI: directory, member rights, bounded Warehouse invitation grants,
  sensitive reauthentication, suspend/resume/revoke, invitation management and
  administration history. Recipient fragment is scrubbed before render/network;
  uncertain writes reuse the same operation/input/key rather than duplicate work.
- V34 adds exactly five Team tables and bounded guard/ACL amendments, preserving
  all32 historical SQL migrations. The operator script accepts the five explicit
  rights without changing its default `dashboard.read`; its body was not run.

## Observed verification, 2026-11-17

| Gate | Actual result |
| --- | --- |
| Backend H2 `verify`, report-policy mode h2 |698 PASS,17 expected mode-specific skips;715 discovered |
| Full PostgreSQL18 `verify`, CI-style disposable DB + Testcontainers |1172 PASS,1 fixture assertion FAIL,5 expected H2-only skips;1178 discovered |
| Corrected focused PostgreSQL run: Browser parent + nine Team methods |10/10 PASS; no skips/errors |
| Actual HTTPS/PostgreSQL/test-SMTP Browser journeys |18/18 PASS, including invite→accept→Workspace→suspend→resume→revoke and preserved Personal |
| PostgreSQL migration suite |64/64 PASS, FK/CHECK/unique/direct-DML/transaction rollback evidence |
| Frontend unit suite / focused Team UI fixtures |182/182 and21/21 PASS; UI fixtures are not full-stack evidence |
| Frontend lint/typecheck/default+Customer builds |PASS; typecheck is also executed by both builds |
| Report-policy fail-closed regression |132/132 PASS |
| Source manifest/traceability validator / diff whitespace |PASS, no errors |
| Generated credentials, UTF8/UTF16 diagnostics |PASS; no credential values published |
| Independent recorded Testcontainers and CI-container absence audit |PASS; only owned test resources cleaned |
| Operator parameter binding only |PASS: five approved rights accepted, default unchanged, Platform right rejected; no script body/DB command executed |

Two defects were corrected during this checkpoint. The real-clock Browser exposed
an action timestamp captured before Identity proof consumption; the action now
captures time after successful consumption, retaining all deadline/session/DB
guards. The expanded journey also required purpose-specific proof/delivery counts.
Its subsequent full run was compiled before the synthetic Organization UUID
assertion was corrected to `id(901)`, producing the single failure above.

That full run is retained as **FAIL**, including its original XML and sanitized
summary. The corrected test and all nine Team methods passed in a separate real
PostgreSQL run. Production `target/classes` trees are byte-for-byte identical
between those runs; the sole changed test class is
`IdentityGrantApplicationTests.class`. Across the recorded full run and focused
rerun, all1173 executed test identities have a passing observed outcome; five
mode-specific skips remain unchanged. No raw report was edited or synthesized,
and no single-run full PASS is asserted. Unaffected suites were not repeated for
this fixture-only correction.

Ignored/private evidence pointers:
`.demo/organization-team/full-h2-path.txt`, `full-postgres-path.txt`,
`focused-final-path.txt`; resolution metadata is
`.demo/organization-team/verification-resolution.json`. Raw logs, generated
credentials, mail bodies and browser artifacts stay private and outside Git.

Source/test preservation checks: all37 checkpoint copies retain their SHA-256;
the approved technical manifest retains its exact hash; V1–V32 content is
unchanged. Original demo PostgreSQL remains `exited`, as it was before these
test runs; it was not started, stopped, migrated or queried for this checkpoint.

## Source/test boundary before the later Local/Test GO

No hosted GitHub CI, commit/push/PR/merge, localhost migration or real-account
Team grant was performed. Owner transfer and Warehouse/Finance/Support role
templates remain outside the approved core. Local deployment is a separate
Owner decision: V34 and the five explicit Team permissions for the intended
verified Customer/Organization must be reviewed together before execution.

The subsequent Owner Local/Test approval and actual bounded deployment are
recorded separately in the [Local/Test checkpoint](organization-team-local-checkpoint.md).
It preserves the source/test failure history above and does not imply hosted CI,
Production or expanded role/ownership authority.
