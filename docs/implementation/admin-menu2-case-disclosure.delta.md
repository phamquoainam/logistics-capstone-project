# M2-L-DISCLOSURE-01 — missing lifecycle review disclosure contract

**OWNER APPROVED — 2026-11-26; registration gated on Backend tests PASS.**
Isolated source/test only; parent manifest bytes
remain `6d072d9b649ab144c2fb9b8b780c8f077757387bcef58536a31618f9154251a2`.

Exact finding: `M2-Case.privatePayload` explicitly says **NOT INCLUDED; use
authorized evidence/disclosure**. API01–15 supply case metadata and event codes;
none returns the protected preparation reason/payload or RFI instructions.
M2O-API-07 returns separately screened evidence bytes, not the preparation payload.
Therefore the prepared state machine is testable, but a reviewer cannot complete
a real in-portal review of RESTRICT/RISK_HOLD impact, or a maker see RFI details,
through the approved metadata DTO alone. Do not silently add private fields to
M2-Case, log plaintext, reuse application disclosure or fake a completed review.

## Exact minimal proposal

- **M2-L-DISCLOSURE-API-01**: `POST /api/v1/admin/merchant-organizations/{organizationId}/governance-cases/{caseId}/disclosures`.
- **M2-L-DISCLOSURE-DTO-01 input**: `{ expectedCaseVersion:int64, purpose:"LIFECYCLE_REVIEW" }`.
- **M2-L-DISCLOSURE-DTO-02 output**: `{ caseId:UUID, organizationId:UUID, version:int64, revision:int64, casePayload:M2-CasePayload, reason:NFC1..300, requestedFieldCodes:string[], sourceEventId:UUID, disclosedAt:instant }`.
- **M2-L-DISCLOSURE-PORT-01** on existing Merchant-owned port:
  `CaseDisclosure disclose(AdminCommandContext actor, UUID organizationId, UUID caseId, CaseDisclosureInput input)`.
- Reuse genuine SYSTEM admission, exact `org.read` plus either current assigned
  independent `org.lifecycle.review`, or same-case maker `org.lifecycle.propose`.
  No access from role name, Root, constructed context, other Org or former reviewer.
- Merchant decrypts only its own current committed case/event envelope after
  current scope and expected-version checks. Return only declared reason/payload
  and actual RFI field codes; never command proofs, keys, receipts or evidence bytes.
- Append an audited `READ_CASE_DISCLOSURE`/`CASE_DISCLOSURE` event using existing
  approved event vocabulary, current actor/request, exact case/revision/source
  binding, protected audit envelope. No case version/state mutation or new task.
  Reject stale/foreign/revoked requests before disclosure; no-store/no-referrer.
- **No new permission, table, SQL signature, domain authority or deployment**.
  Existing event guard already permits metadata-only read audit; its exact
  constraints still need focused compatibility verification before implementation.

Required focused tests: assigned reviewer/maker positive, former/foreign reviewer,
wrong Org/current actor, constructed/escaped context, stale version, audit-before-
disclosure/rollback/privacy, no raw proof/envelope and HTTPS UI review/RFI display.

The Owner approved only the exact endpoint/DTO/port and maker/current-assigned-
reviewer disclosure above, no new table/permission/role/grant or state execution.
Source/DB guard integration is permitted; register the endpoint only after actual
authorization/redaction/audit tests PASS. This amendment is separate from the
approved policy-reader binding; preserve all earlier security failure evidence.

## Backend registration gate evidence

- `.demo/cskh-focused-7e2c53a6bace43558260096a1be7fd25`: **1/1 PASS** for
  maker/current reviewer, foreign Org, unassigned actor, forged context, stale
  version, response allowlist, encrypted audit and rollback.
- `.demo/cskh-focused-191afe7f6e004be5b4e3cc152c457b3c`: **1/1 PASS**, including
  SQL current-party guard/direct-DML negative, RFI reason/field disclosure,
  unchanged case version and command-receipt counts. Snapshot
  `afd6d39571014a682621e76ac4ce2215127b8bab293cf400915f4be96f992aae`.
- Only after these gates, the one approved POST endpoint and exact existing
  authenticated-route allowlists were registered. No new authority/table/grant.
- Final actual HTTPS integration and assignment-revocation follow-up are tracked
  separately in `admin-menu12-readiness.md`; the registration gate does not by
  itself declare full Lifecycle or Menu2 READY.
