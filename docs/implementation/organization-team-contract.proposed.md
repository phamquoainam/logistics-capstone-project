# Organization Team & Access — OTA V1

Status: TECHNICAL CORE APPROVED by Owner on 2026-11-17, restricted to source
implementation and isolated PostgreSQL18 tests. Approval binds the frozen manifest
SHA-256 `f6e21ee3ec3f742035d37f2c10d624031cc450a9b590939df878e9397952f426`.
Existing IAM-INVITE-03 and accepted Owner policies are reused, not reopened.
Historical proposal/verification text below is retained for provenance; current
execution and gate evidence are recorded in [the execution log](organization-team-execution.md).
No localhost migration/grants, Owner transfer or extra role templates are authorized.

Subsequent Owner Local/Test GO authorizes V34 and only the exact five Team grants
for the Initial Owner of NamShop Demo. See the [deployment checkpoint](organization-team-local-checkpoint.md)
for actual execution and pending write UAT; there is no general self-grant,
Production, Owner-transfer or additional-role authorization.

## First delivery and authority

OTA-02 uses the accepted GET `/api/v1/customer/business-contexts`: the current
member's role, membership ID, authority version and ORGANIZATION-level grant
projection. It is not a member directory, complete Warehouse grant inventory or
an administration permission. No member count is inferred from this response.

The next runtime slice, OTA-04, is Local/Test only, operator-provisioned active
Organizations, verified current Customers, active Membership and explicit RBAC.
An Owner label confers neither platform Admin nor automatic grants. Keep one
canonical `merchant.membership` / `member_grant` authority and existing Identity
admission, CSRF, TLS, reauthentication and transaction/lock order.

## Precise core contract — accepted in the frozen technical manifest

Base `B=/api/v1/merchant-organizations/{organizationId}/team`.
Every read rechecks current Identity + Organization + Membership + permission.
Every mutation requires Origin/CSRF, UUID Idempotency-Key, current scope, and
If-Match for an existing admin-mutated resource. Recipient accept/decline has no
pre-Membership read/ETag: its bound proof version is consumed atomically instead.
Unknown fields, duplicate JSON keys, invalid
UUIDs/enums, excess lengths or scope escalation are rejected. Receipts are scoped
to actor, Organization and operation; payload digest mismatch is409. A stale ETag
is412. Forbidden/foreign resource existence is not disclosed. All responses are
no-store; invitation/PII fields are redacted from logs and evidence.

| ID | New method/path | Exact request | Response / authority |
| --- | --- | --- | --- |
| OTA-API-01 | POST B/members/search | MemberSearch | MemberPage; team.read; read-only body search, no receipt/write |
| OTA-API-02 | GET B/members/{membershipId} | none | MemberDetail + ETag; team.read |
| OTA-API-03 | GET B/role-templates | none | RoleTemplates; team.read; versioned platform ceilings, not grants |
| OTA-API-04 | POST B/invitations | InvitationInput | Invitation + ETag; team.invite |
| OTA-API-05 | POST B/invitations/search | InvitationSearch | InvitationPage; team.invite; no proof returned |
| OTA-API-06 | POST B/invitations/{id}/resend | Reason | Invitation + ETag; team.invite; replace old proof atomically |
| OTA-API-07 | POST B/invitations/{id}/revoke | Reason | Invitation + ETag; team.invite |
| OTA-API-08 | PUT B/members/{id}/access | AccessInput | MemberDetail + ETag; team.access.manage, fresh reauth |
| OTA-API-09 | POST B/members/{id}/suspend | SensitiveReason | MemberDetail + ETag; team.membership.manage, fresh reauth |
| OTA-API-10 | POST B/members/{id}/resume | SensitiveReason | MemberDetail + ETag; same authority, eligibility recheck |
| OTA-API-11 | POST B/members/{id}/revoke | SensitiveReason | MemberDetail + ETag; same authority; terminal revoke |
| OTA-API-12 | POST B/audit/search | AuditSearch | AuditPage; team.audit.read; no security secrets/IP/device inventory |
| OTA-API-13 | GET B/receipts/{operation}/{key} | none | Receipt; same current command authority; safe uncertain-outcome lookup |
| OTA-API-14 | POST /api/v1/customer/organization-invitations/{id}/accept | InvitationDecision | DecisionReceipt; verified self recipient; no preexisting Org membership required |
| OTA-API-15 | POST /api/v1/customer/organization-invitations/{id}/decline | InvitationDecision | DecisionReceipt; same recipient binding |

Exact DTO shapes (all UUIDs canonical/nonzero, timestamps server UTC; arrays unique):

- Grant = `{permission,scope,facilityId}`; permission one of the accepted27 or
  the five new rights below; scope ORGANIZATION/FACILITY; facilityId null iff
  ORGANIZATION. Maximum200 grants. Facility must be ACTIVE in the same Org and
  permission must already allow facility scope. All team rights are Org-only.
- MemberSearch = `{search,role,state,facilityId,page,size}`; literal search0–120
  Unicode characters on display name only, nullable role/state/facilityId,
  page0–1000, size1–50. No global email/account lookup. Facility filter intersects
  caller authority and does not permit directory discovery to a facility-only user.
- Member = `{membershipId,displayName,role,state,authorityVersion,version,createdAt,updatedAt}`.
  MemberPage = `{items,page,size,hasNext}`. MemberDetail = Member plus `{grants}`.
  No email, phone or Identity account UUID in directory projection. SUSPENDED is
  a proposed forward-only amendment, not an existing V23 state.
- RoleTemplate = `{role,version,allowedPermissions}`; RoleTemplates = `{items}`.
  Core roles remain OWNER/MANAGER/OPERATOR. WAREHOUSE/FINANCE/SUPPORT labels and
  ceilings are NOT activated in this core contract; their exact capability matrix
  requires a separate bounded amendment. Do not invent automatic role mappings.
- InvitationInput = `{email,role,grants,reason}`; email normalized through existing
  Identity rules, max254, role MANAGER/OPERATOR only, reason1–300. No Owner invite.
- Invitation = `{id,maskedEmail,role,grants,state,version,expiresAt,deliveryState,createdAt}`.
  State PENDING/ACCEPTED/DECLINED/REVOKED/EXPIRED/INVALIDATED. Delivery state is the
  existing protected-delivery state, not inferred DELIVERED from SMTP250.
  InvitationSearch = `{state,page,size}`; InvitationPage = `{items,page,size,hasNext}`.
- Reason = `{reason}` (1–300). SensitiveReason = `{reason,reauthenticationProofId}`.
  AccessInput = `{role,grants,reason,reauthenticationProofId}` (MANAGER/OPERATOR only).
- InvitationDecision = `{proof}`; opaque256-bit secret, request body only, no logs.
  Proof is bound to current verified Identity email + purpose + Org + invitation;
  it is not a password, Customer enrollment proof or privileged Identity invitation.
- AuditSearch = `{membershipId,operation,from,to,page,size}`; optional target and
  operation, UTC period max90 days, pages as above. Audit = `{id,actorMembershipId,
  targetMembershipId,operation,reason,occurredAt}`; AuditPage = `{items,page,size,hasNext}`.
- Receipt = `{operation,key,targetId,outcome,version}`; DecisionReceipt =
  `{invitationId,outcome,membershipId}` with membershipId null on DECLINED. Actor
  cannot query another actor's receipts. Successful replay never bypasses admission.

New explicit Org-level permissions: `team.read`, `team.invite`,
`team.access.manage`, `team.membership.manage`, `team.audit.read`.
No automatic grant/backfill, including existing Owner or NamShop Demo. Existing
dashboard.staff.read is analytics permission and must not become team.read.

## Delegation, invitations and lifecycle

- Platform ceilings, actual current delegable grants and every old/requested scope
  constrain access replacement. Full replacement checks the UNION of old/new
  scopes. Never promote self, alter Owner, add platform/Carrier authority, delegate
  team administration to Manager, or grant a permission the actor does not hold.
- Only Owner with explicit team.access.manage may change non-Owner grants. Manager
  with team.invite may invite OPERATOR using operational accepted grants only;
  finance, team administration and any unapproved Support rights are denied.
  Owner may invite MANAGER/OPERATOR within the same ceiling/subset checks.
- Accepted invitation lifetime72h, single-use, target-bound, resend invalidates
  old proof. Store inviter authorityVersion and organization policy version;
  revoke/suspend/rights/policy changes invalidate pending invitations. Lock and
  recheck them at acceptance; two accepts yield one Membership and one receipt.
- Existing Customer must authenticate and verify own email. New recipient uses
  regular Customer registration first. Accept never activates an Identity account,
  sets a password, merges Personal data or grants another Organization.
- Core lifecycle ACTIVE → SUSPENDED → ACTIVE or ACTIVE/SUSPENDED → REVOKED.
  REVOKED is terminal, existing UNIQUE(org,account) retained; rehire/rejoin after
  revoke is explicitly unavailable until an accepted separate policy. No deleting
  old membership/audit or silently reviving it through invitation.
- Cannot suspend/revoke Owner or last usable Owner through these commands.
  Revocation denies subsequent Org requests, assignments and pending invitation
  consumption immediately at Backend. It preserves Personal/other-Org sessions;
  it is not global Customer logout or a second session authority.
- Non-Owner staff offboarding needs reason, recent reauth and atomic membership,
  grant invalidation, versions, receipt, audit and invitation invalidation. Internal
  historical assignees remain traceable, never automatically reassigned/granted.

## Data / module amendments — explicitly pending

New Merchant-owned relations, exact columns (FKs always same Org):

1. `merchant.team_policy`: organization_id PK/FK, version, template_version,
   updated_at. No policy chosen in browser.
2. `merchant.team_invitation`: id PK, organization_id FK, inviter_membership_id FK,
   inviter_authority_version, policy_version, recipient_digest, protected_recipient,
   role, state, proof_digest, proof_version, expires_at, accepted_membership_id FK
   nullable, version, created_at, updated_at. Unique live recipient perOrg; do not
   store plaintext proof. Protected recipient uses existing configured key ring.
3. `merchant.team_invitation_grant`: id UUID PK, invitation_id FK, organization_id FK,
   permission, scope, facility_id nullable; same validation/uniqueness as member_grant.
4. `merchant.team_audit`: id PK, organization_id FK, actor_account_id (admitted
   Identity external reference), actor_membership_id nullable FK (only verified
   recipient DECLINE may have no Membership),
   target_membership_id nullable FK, invitation_id nullable FK, operation, reason,
   request_id, occurred_at. Append-only; no email, password or proof.
5. `merchant.team_command_receipt`: organization_id, actor_account_id, operation,
   key (compositePK), input_digest, membership_id nullable FK, invitation_id nullable
   FK (exactly one typed target), outcome, version, recorded_at.
   Immutable, no credential bodies; recipient decision receipts purpose-separated.

Existing `merchant.membership` amendment: allow SUSPENDED (widen state toVARCHAR9),
controlled non-Owner role change; immutable id/org/account/createdAt and terminal
REVOKED retained, exact version+authorityVersion increment retained. Preserve V23's
initial Owner binding and active Owner invariant in this core slice.

Extend existing grant allowlists/scope/operator guards for the five rights only;
runtime write access only through reviewed command boundaries. Runtime must not
gain execution of configure_local_workspace or Identity administration functions.
Deferred DB checks bind accepted invitation/member/grants, current templates,
active Owner and one audit+receipt per committed command. No raw DML bypass.

Typed `OrganizationTeam` API lives in Merchant. Workflow composes it with existing
Customer admission/reauthentication. Add an Identity-owned protected-delivery port
for MERCHANT_MEMBERSHIP purpose reusing the canonical outbox/crypto/worker, never
reading Identity internals or sending SMTP inside the transaction. Exact port:
`enqueueMembershipInvitation(context,invitationId,proofVersion,recipient,proof,expiresAt)`;
it returns a delivery reference, does not confer account/Merchant authority.
The stable outbox message identity includes invitationId + proofVersion + purpose,
so resend cannot accidentally reuse the old delivery intent.
Recipient eligibility comes from current Identity public admission, not Merchant
cross-schema reads. Transient sends bounded by proof expiry, permanent failure
visible without claiming Membership activated. Idempotent enqueue/message IDs.

New migration after immutable V1–V32; V33 is reserved by the pending Support
manifest. Final number is allocated before source implementation to keep one
ordered stream. Neither migration is created/executed by this proposal. Test DB
only after technical acceptance; no localhost migration, real-account grant,
Production rollout or destructive data changes authorized here.

## Owner transfer and additional roles — explicit next boundary

Accepted two-step15min target-bound transfer + existing fresh Customer reauth is
the intended policy, not a new decision. Current V23 requires initial Owner to
remain active OWNER, and JdbcBusinessWorkspaces rechecks that initial Owner.
Do NOT weaken that invariant as an incidental member edit. Ownership transfer
requires a separate exact current-owner pointer/backfill + deferred last-owner
invariant + all caller/Identity-discovery compatibility amendment, with target
consent and rollback/concurrency tests. Initial provenance must remain immutable.
Dual-Business-Admin recovery remains separate and is not a Merchant command.

Likewise WAREHOUSE/FINANCE/SUPPORT role names do not activate draft Support rights
or imply data access. Current facility grants already restrict capabilities by
Warehouse; role-template expansion must explicitly define each allowed permission.
No Store dimension invented without an accepted Store ownership source.

## Historical Owner verification checkpoint, before technical GO

Owner agrees with the BUSINESS direction but explicitly withholds technical
acceptance pending inventory verification. All15 new APIs,20 DTO definitions,
five permissions, five tables and four compatibility amendments remain PROPOSED.
The exact field/type/PK/FK/unique/check/authority/error inventory is
[the technical JSON manifest](organization-team-technical-manifest.json), explained
in [the verification packet](organization-team-technical-review.md). These are
one checkpoint, not additional business scope or separate approval rounds.

Technical clarifications are explicitly pending with the rest of that packet:
recipient proof consumption instead of nonexistent pre-admission ETag; concrete
invitation-grant PK; admitted recipient audit actor; typed receipt targets;
exact role ceilings/no runtime team-right delegation and legacy grant compatibility.
No implementation authority follows from this correction of proposal gaps.

Org receipt lookup covers the seven admin operations only. Recipient accept/decline
uncertain outcomes use authenticated same-key replay, never a new requirement to
hold Team admin rights. Proposed rolling-hour invite/resend budget:5 per
actor+recipient+Organization via existing limiter, no quota consumption on replay.
Role ceilings are exact in the JSON; labels do not confer rights. Existing
operator-granted rights outside new MANAGER/OPERATOR ceilings are preserved;
core access replacement for such a target returns TEAM_STATE_CONFLICT pending
explicit compatible template/operator disposition. Do not silently strip them.

### Task status

- [x] OTA-01 Inventory, accepted policy comparison,29-path SHA-256 checkpoint.
- [x] OTA-02 Current-self access page using existing API, menu and regression tests
  (2 unit,19 affected UI,1 PG parent/18 real HTTPS Browser journeys PASS;
  typecheck/lint/default+Customer build and source/privacy checks PASS).
- [x] OTA-03 Owner accepts exact core API/DTO/schema/permission/amendment above.
- [x] OTA-04 Implement canonical team/invitation/lifecycle vertical slices + V34.
- [x] OTA-05 Local source/test evidence: PostgreSQL18 direct-DML/rollback/Owner protection, cross-tenant,
  scope union, inviter invalidation, duplicate accept/resend, replay, reauth,
  offboarding and Personal/other-Org preservation; HTTP/Browser/test SMTP.
- [ ] OTA-06 Additional role templates and controlled ownership-transfer amendment.
- [x] OTA-07 Separate local deployment/grant decision received; bounded deployment
  and read smoke complete, live write UAT pending in the deployment checkpoint.

See the execution log for exact counts and the full-run fixture failure resolved
by a successful targeted rerun. No new hosted CI or expanded Team completion is claimed.
