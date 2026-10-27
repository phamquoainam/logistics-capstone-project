# Spec003 focused technical review and implementation authorization package

Authority: **OWNER-S003-TECHNICAL-2026-10-27**, Owner request “OWNER SPEC003 TECHNICAL DESIGN APPROVAL & FINAL IMPLEMENTATION GATE”, supplied attachment `05b7c003-ed3c-4bab-85b1-3ace002e9c12/Pasted text.txt`. It approves the six directions, authorizes focused review/minimal correction and conditional normal PR32 merge. It does **not** authorize production code, migrations or feature acceptance.

Planning checkpoint: **SPEC003_READY_FOR_OWNER_IMPLEMENTATION_AUTHORIZATION**. Contract status: OWNER_REVIEWED_PLANNING_BASELINE. Runtime: NOT_IMPLEMENTED / NOT_ACCEPTED; implementation NOT_AUTHORIZED. The existing `openapi.candidate.json` filename is retained for link continuity; its explicit status and this controlled amendment govern the reviewed planning baseline. Accepted SYSTEM contracts/ADRs/Flyway V1–V6 remain unchanged and effective. Conditional merge still requires exact current integration CI, clean mergeability and resolved review threads.

## Reviewed inventory and genuine corrections

All original75 operations,54 HTTP schemas and3 backend schemas were read, including required/optional fields, media types, headers and error responses. [Per-operation review](operation-review.csv) records canonical requirement/use-case, trusted subject, auth/authz, scope, preconditions/transitions, wire behavior, owner, concurrency/idempotency and planned tests/AC for every operation. [Coverage](coverage.csv) retains all13 CORE and mandatory DENY BC045; all61 behavioral scenarios remain PLANNED_NOT_EXECUTED.

| Finding | Bounded correction / future verification |
|---|---|
| Refresh401 omitted the established replay distinction | Dedicated401 SESSION_INVALID/REFRESH_REPLAY; strict family revocation, no retry grace, fresh login after lost rotation response. S003-REFRESH strengthened; static mutation rejects losing the replay code. |
| Context schema allowed incomplete Merchant or mixed Personal context; list truncated at100 | Conditional required/forbidden org/member references; paged current-context list with page/size/hasNext. Schema success/negative checks; S003-CONTEXT covers later pages and stale responses. |
| Business Admin lacks scoped organization read/ETag without Merchant membership | Add `getReviewOrganization` with current regional application.review. APPROVED application exposes server-bound organizationId; organization read exposes existing approved legal fields for editing/review. No body organization authority. |
| Second recovery reviewer lacks independent read/version/target/evidence | Add `getReviewOwnerRecovery` with current regional ownership.recover and OwnerRecoveryReview; Ownership exposes server-resolved requestorAccountId/targetAccountId. No raw consent/proof or blanket organization access. |
| Fresh representative evidence cannot be submitted when all Owners are unusable | Existing org legal-evidence upload also permits current scoped ownership.recover reviewer for recovery-only quarantine intake; it cannot alter the approved profile, grant Owner, bypass screening or approve its own recovery. Separate approver and current target consent still mandatory. |
| Legal review has two concurrency versions | Review If-Match binds pending legal revision; stored baseOrganizationVersion is rechecked under org/key locks; stale base409. Current approved profile stays intact on conflict. |

Final inventory: **77 operations,55 HTTP schemas,3 backend dependency schemas**. **69 REQUIRED_FOR_SPEC003,8 SUPPORTING_DEPENDENCY,0 OUT_OF_SCOPE**. The eight supporting operations are CSRF challenge, recent reauthentication, target recovery consent, business-contact proof request/confirmation, receipt lookup and authenticated legacy link request/confirmation. They remain necessary secure prerequisites, not new standalone business features. No original operation was removed: own/reviewer routes have different scope; evidence metadata/content and invitation preview/accept have different disclosure/effect semantics. No unrelated logistics, payment, Station, Recipient IAM or master CRUD retained merely to maintain a count.

The three supporting backend contracts remain only CatalogEligibility004, ServicePolicyEligibility005 and ClosureClearance from authoritative owners. No Address/Coverage/Service/Pricing master copied into003. Read/validate publication locks+versions are the minimum004/005 subset before successful BC159 acceptance; real owner fixtures required. Missing/stale facts fail closed, never synthesize available service/empty obligations.

## Security and ownership disposition

No unresolved material design violation was found after these corrections. Identity owns the one account/password/verified contact master, customer proofs and purpose-separated session/refresh tables/trust. Customer-profile aggregate remains independent of org/member in the existing merchant module. Merchant owns application/org/member/Business Admin scope; workflow composes public APIs only. Current locked account/session/org/member/reviewer/target authority is checked at the final commit decision; revocation-first denies, earlier valid commits persist. No body actor/grant, cross-module SQL, self-review or unilateral recovery. Evidence references and extra subject IDs are untrusted until resolved and reread through owners. Existing Foundation/Identity findings remain scoped as previously recorded; this review claims no runtime closure.

Review-only evidence intake is org/recovery-purpose bound; retained/reserved file slots and pending recovery references obey the same quota/hold/privacy controls. The requestor who uploads/intakes evidence may propose recovery but cannot serve as its independent approver. No public or Merchant membership assignment of ownership.recover. Recovery read is scoped to the exact org+recovery, checks current reviewer region, and redacts proof/delivery destinations. Identity-only admin and cross-org/region access deny before resource/version disclosure. The final target is derived from current Identity consent, never requestor-supplied account ID.

## DoR and dependency staging

All seven repository DoR design/traceability/fixture criteria are satisfied at planning level; [readiness](../readiness.md) records the distinction from code authorization and runtime acceptance. No broad redesign or repeat candidate approval is pending. Routine DTO/client/persistence decisions following this baseline can proceed under one future implementation authorization; material changes to accepted security/ownership/provider policy still require controlled amendment.

| Gate | Timing and exact obligation |
|---|---|
| Production implementation authorization | The one remaining Owner action before code; all T003-01..12 together. |
| Email delivery | Secure configurable adapter/outbox, test inbox/delivery evidence or bound proof before successful verification/recovery E2E. Production SMTP vendor absence does not block independent account/profile coding; SMTP250 is not delivered. Sandbox evidence explicitly TEST. |
| Private storage and screening | Approved testable private adapter, quarantine/signature/parser/scanner with outage fail-closed before upload/clean-file review E2E. No fake CLEAN or public URLs. Deployment secrets stay outside Git. |
| Minimum004/005 | Real owner publication/eligibility API+fixtures before successful BC159 defaults and full003 acceptance. Independent account/application/member work proceeds in sequence. |
| Closure owners | Missing clearance retains CLOSURE_REQUESTED; full003 demonstrates safe pending/deny. Successful closure with live dependencies needs real current owner clearance, never missing=zero. |
| Full acceptance | All61 scenarios expanded into actual tests plus per-BC AC, real PG/HTTP/browser/migration/security/NFR evidence; required source/integration/merge CI; separate Owner full-Spec003 acceptance. Purge/public rollout excluded. |

## One bounded production authorization proposal

Authorize **all T003-01..12** in [tasks](../tasks.md): necessary Identity customer amendment; backend/public APIs; additive PostgreSQL18 migrations and runtime privileges; real secure delivery/storage/screen adapters; independent profile/application/review/activation; context/member/invite/delegation/revoke; org/legal/transfer/recovery; minimum004/005 consumer dependencies; integrated current frontend/client/browser E2E; negative/concurrency/privacy/migration/backward compatibility/architecture/contract/H2/PG18/CI evidence; full-spec traceability and final acceptance submission.

Use scoped implementation PRs in dependency order: (1) Identity/admission/contracts+migration groundwork, (2) profile/application/evidence/review/activation with UI journeys, (3) contexts/membership/delegation/org recovery/settings plus minimum owner dependencies, (4) final cross-feature hardening/E2E/full acceptance evidence. Split further only to keep changes reviewable; no extra worktree by default, no unrelated historical commits. Each implementation PR requires applicable gates and Owner merge, never automatic future merges. Routine execution within this authorization needs no repeated Owner permission. No Spec003 acceptance until the complete scope passes; no claim that003 alone accepts Group A/B portal or later Specs.
