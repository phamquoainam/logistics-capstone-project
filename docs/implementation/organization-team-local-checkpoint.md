# Organization Team V1 — controlled Local/Test deployment

Date:2026-11-17. Verdict: **LOCAL_TEAM_V1_UAT_READY**.
Owner Local/Test GO WITH CONDITIONS explicitly authorized V34 and exactly five
Team permissions for the verified Initial Owner of NamShop Demo. This extends
the earlier isolated source/test authorization only for this deployment.

## Completed deployment gates

- [x] Preserve87 WIP source paths with SHA-256 in a private checkpoint.
- [x] Identify the original container, volume, database and environment uniquely.
- [x] Verify historical checksums, schema/operator compatibility and exact Owner.
- [x] Create backup and prove recovery on an isolated restore copy.
- [x] Apply V34 through the official one-shot Flyway profile, target34 only.
- [x] Grant the exact five rights through the independent operator procedure.
- [x] Verify permission delta, immutable bindings, audit/receipt and same-key replay.
- [x] Start Backend/Frontend with HTTPS and the existing protected configuration.
- [x] HTTP/guest Browser smoke and Owner-confirmed authenticated read UI.
- [x] Live invitation sending/receipt, exact72h metadata and recipient acceptance.
- [x] Live SUSPEND with reauthentication, audit/receipt and Personal preservation.
- [x] Live RESUME with audit/receipt: ACTIVE/version3/authority3.
- [ ] Remaining resend/decline/revoke-invitation and REVOKE_MEMBER UAT.
- [ ] Warehouse-scoped live delegation UAT, using suitable authorized test data.

## Preflight identity and preservation

| Item | Observed value |
| --- | --- |
| Container | `elogistics-d1-postgres18` |
| Container ID | `965042a98c43043378b8757a13cb4364a063a289c59e35f43ee7c9eeac761a63` |
| Image | `postgres:18.6-bookworm@sha256:afc7e2d441324c0388fa80c3d24f733b4194a4eb7f47dd8ee2b08eb1a24a647c` |
| Volume | `elogistics_d1_pg18_dev_data` |
| Database / port | `elogistics_dev`, `127.0.0.1:5433` |
| PostgreSQL / system identifier |18.6 / `7696150062350938155` |
| Environment | Organization `LOCAL`, admission `LOCAL_OPERATOR_PROVISIONED`; application `SANDBOX` |
| Organization | `782c4766-8bdd-435b-9961-512c831922d6` — NamShop Demo |
| Account | `5d206ef3-350e-42b7-af8a-4c54b8d43fe2` |
| Initial Owner Membership | `d34e1afe-4d3d-4e6e-bd63-449ddca5f92f` |

NamShop Demo matched exactly one Organization. Account ACTIVE/email verified,
credential present and exact active Customer identifier consistent. Organization
ACTIVE; its immutable initial-owner pointer matched the ACTIVE/OWNER Membership
and the exact Account above. No replacement target was selected.

The container initially was `exited`; PostgreSQL control metadata indicated an
unclean earlier shutdown. No migration/grant preceded recovery verification.
A stopped-cluster archive included the complete data/WAL, from a read-only
volume mount. An isolated container with **no network**, no published ports and
tmpfs restore storage recovered that archive and read all114 application tables.
Its restored system ID, Flyway history and target bindings matched the original
after starting that exact original container ID. Compose recreation was not used.

Backup:206069760 bytes, SHA-256
`9db0d2882d3cd371e3b117173659be956dd7a2fc6169792ead5c8a0277b1d4a4`.
The archive and private evidence stay outside Git under the path recorded in
`.demo/organization-team/local-deploy-path.txt`. The restored helper was stopped
cleanly and removed after verification; the original volume and archive remain.

## Flyway and grant results

All32 historical source checksums and script names matched successful history.
Neither source/JAR nor database history contained V33; the Support proposal was
not deployed. Only V34 was pending. The artifact matched all33 source scripts.
V34 succeeded with checksum **667729765**; history is exactly1–32,34. No repair,
reset, clean, out-of-order execution or historical migration edit occurred.
Future Support work must allocate a forward migration above34, not insert V33.

Migration-only readback showed113 pre-existing data tables unchanged; only
`platform.flyway_schema_history` gained its V34 row. Exactly five Team tables
were added, with policy initialization for the existing Organization.

Independent operator request: `add29367-c085-45ee-b3c7-3d0e24dad0db`.
Session principal: `elogistics_migrator`, `SET LOCAL ROLE elogistics_owner`,
Local operator mode, canonical `merchant.configure_local_workspace` procedure.
Runtime cannot execute that procedure.

Exact added grant delta, all `ORGANIZATION` scope and null Facility:

```text
team.read
team.invite
team.access.manage
team.membership.manage
team.audit.read
```

All20 previous grant rows, including their IDs/scopes, remained unchanged.
The existing `dashboard.read` was passed as the procedure's required base grant;
it was not duplicated or newly added. Only the five Team rows were inserted.
Membership identity, Organization, Account, Role and ACTIVE state remained
unchanged. Its required version/authority counters advanced6→7 exactly once.
One existing workspace operator audit and one receipt were added. Replaying the
same request/input returned the same outcome without additional grants, audit,
receipts or counter increments. No Membership or other-account grant was created.
The later Owner-initiated invitation UAT created one OPERATOR Membership through
the canonical recipient-acceptance transaction; this is separate from the
operator activation and does not grant any Team or platform permission.

Across the full pre-runtime comparison,109 old tables were unchanged. The five
expected changed tables were Flyway history, Membership, grants, operator audit
and operator receipt. No unexpected old-table delta was found. Read-only checks
confirmed Initial Owner change denial, foreign-Organization permission denial
and denial of runtime delegation of Team rights to Operator.
Post-deployment catalog readback passed23 ACL assertions: runtime cannot write
Organization/Team Policy or change Membership identity columns, cannot update or
delete Team audit/receipts, and cannot execute the offline operator procedure.

## Local runtime and UAT evidence

Launcher changes: enable Team only in the existing Local/Test launch process;
reuse the verified existing PostgreSQL instance rather than Compose recreation;
clear the Team flag before the frontend child starts. Four focused dependency-
mocked launcher tests PASS: healthy reuse, stopped exact-ID start, foreign-project
denial and initial-launcher fallback. These mocks are not live UAT evidence.

Observed live results:

- Backend/Frontend ownership and HTTPS readiness: `LOCAL_FULL_STACK_HTTP_READY`.
- HTTP smoke6/6 PASS: public endpoints200, self/Team/foreign-Org guest reads401,
  no Owner PII in denied responses; TLS verification remained enabled.
- Browser software-test context, without any Owner profile/cookies/credentials:
 5/5 PASS for login rendering, Team/Personal guest denial,390px layout and no
  unhandled page error. Initial locator-encoding failure was corrected in the
  private test fixture; it was not an application defect.
- Owner confirmed authenticated Team page, directory, role templates, audit and
  the five Team rights display correctly for NamShop Demo.
- The first Owner response concerned read UI/permissions only; readback then
  found0 write records. That empty snapshot was retained and not accepted as
  evidence of successful write UAT.
- After a concrete controlled-mailbox test, Owner confirmed a real invitation
  email arrived. Readback found one OPERATOR invitation, already ACCEPTED;
  its proof duration is exactly72h and its proof was consumed once. Gmail SMTP
  accepted one attempt at2026-11-17T12:34:29Z; recipient consumption at12:35:35Z
  created delivery evidence and cleared the protected delivery payload. Exactly
  one INVITE and one ACCEPT audit/receipt were present. Sending and acceptance
  are Local PASS; remaining write journeys are not inferred from that result.
- Lifecycle was likewise not accepted from a broad success response alone:
  readback initially still showed ACTIVE/version1. A subsequent concrete
  Tạm khóa step produced SUSPENDED/version2/authority2 with exactly one SUSPEND
  audit/receipt. Subsequent readback confirmed RESUME with ACTIVE/version3/
  authority3 and exactly one RESUME audit/receipt. Personal Account remained
  ACTIVE/verified. Terminal revoke is not accepted from a general success reply:
  latest readback still showed ACTIVE/version3, with no REVOKE_MEMBER evidence.
- Application is configured `LOCAL_REAL_SMTP` / `smtp.gmail.com`. Real delivery
  evidence combines the provider/consumption record with Owner inbox confirmation,
  rather than configuration alone. The email was initiated by Owner on the UI;
  the agent did not select another account or send an unsolicited message.
- Public/Personal access protection is intact. Shipping/Finance data were
  unchanged before runtime; no fake booking, payment or Carrier event was made.
  Authenticated end-to-end Shipping was not repeated or claimed from this smoke.
  A later read-only preservation check matched all55 Shipping/Pricing/Finance/
  Carrier tables and global Identity grants against their pre-deployment hashes.
- Five protected runtime/PostgreSQL/certificate configuration files retained
  their hashes; no credentials, certificate or global setting was changed.

Browser Use and native Computer Use initialization both failed with
`failed to write kernel assets ... (os error 3)`. Authenticated Owner UI evidence
comes from the Owner response, not an agent-controlled session. Guest Browser
software tests are reported separately.

URLs: `https://127.0.0.1:5173/` and
`https://127.0.0.1:5173/merchant/782c4766-8bdd-435b-9961-512c831922d6/team`.
Normal launcher: `.\scripts\dev\local.cmd start`; inspect existing runtime with
`.\scripts\dev\local.cmd status` before attempting another start.

## Limits, rollback and Git

No Production, Support V33, Claim, Owner Transfer, role-template amendment,
Finance/Carrier authority expansion, commit/push/merge was performed. Branch
remains `feature/argopost-business-shipping-tools`, HEAD
`c8cdfa335e31596116cf0d7626b81ef0d40dee78`; WIP and checkpoints are preserved.

Rollback was **not executed**: deployment checks succeeded and the verified
backup remains available. An in-place restore would require a separate bounded
decision preserving any subsequent demo/UAT data; no destructive rollback or
reverse Flyway migration is performed automatically.

Previous source/test evidence remains unchanged: original full PostgreSQL run
1172 PASS/1 fixture FAIL/5 expected skips; corrected focused rerun10/10 PASS.
This deployment is not a full-suite PASS or GitHub CI PASS claim.
