# P0-POL-UX-01 — applicant requirements and published attestation text

Status: **OWNER APPROVED — isolated SOURCE/TEST only; implementation IN_PROGRESS**.
Owner explicitly approved: “Duyệt delta source/test cô lập”. No live publication,
deployment, grants or production consent approval follows from this decision.
Parent manifest bytes and SHA remain unchanged:
`90c795bd7e773b6a58d0d220056cfbc5e9a5ee95b38ae2493d5b9f75a372291f`.
This does not reopen any approved P0/CTX/BC/OP amendment.

## Concrete contract gap

The approved ten Customer routes expose Draft CRUD/workflow, contact proofs and
private evidence; none exposes the current published application requirements or
attestation wording. The six-field Application DTO deliberately stays unchanged.
V42 application_policy previously stored consent_version without structured
attestation statements. The existing publisher already retains combined
`consentText` in its immutable PUBLISH_POLICY event; that field is preserved.
This addition stores the two separately displayed statements, not a replacement
for that combined text or approval of Terms/Privacy. The existing Customer editor saves only partial identity/address
fields. It cannot derive an authoritative consent version/text from that response.

MA-POL-01 requires exact published wording/version before submission; it explicitly
forbids a placeholder Terms/Privacy checkbox. Hardcoding version1, asking a Customer
to type a technical policy version, borrowing Identity recovery consent, or silently
assuming declarations=true does not meet that contract. Existing synthetic backend
tests establish version matching, not a product requirements-discovery API or
approved legal wording. The original proposal required approval before implementing
the reader/column below; that approval has now been received.

## Exact bounded additions [PROPOSED]

### P0-POL-UX-API-01

`GET /api/v1/merchant-applications/{applicationId}/requirements`

- No query/body/idempotency header or mutation; canonical UUID.
- Current genuine CustomerAdmission, SELF-owned application; eligible actor and
  current environment. Foreign/missing target404 without policy disclosure.
- Resolve exactly the same one published Local/Test policy as submit, using the
  saved entityType + normalized jurisdiction; no arbitrary policy-ID query.
- Incomplete saved selection or unavailable/ambiguous/retired policy returns the
  existing finite POLICY_UNAVAILABLE; never a made-up version/text or empty success.
-200 JSON, Cache-Control:no-store; no email, proof, document IDs or other applicant.
- No new permission, principal, public Guest route or authority.

### P0-POL-UX-DTO-01: ApplicationRequirements

Exact fields:

```text
applicationId: UUID
policyId: UUID
policyVersion: positive int64 decimal string
consentVersion: string1..64
consentStatements: {accuracy:string1..1000,representation:string1..1000}
requiredEvidence: unique [REGISTRATION,REPRESENTATIVE_AUTHORIZATION]
```

These statements concern accuracy and authority to represent the applicant. They
are not invented Terms/Privacy approval. The existing separate Terms/Privacy
readiness restriction remains; no Production-ready claim follows from this reader.

### P0-POL-UX-DB-01 / OP-01

- Add exactly `consent_statements JSONB NOT NULL` to the **unapplied V42** policy
  table. CHECK: object with exactly accuracy/representation string keys; each NFC,
  nonblank,1..1000 Unicode characters and no control characters.
- Existing `merchant.publish_local_application_policy(uuid,uuid,bigint,jsonb,text)`
  signature/ACL/operator protocol stays unchanged. Its existing policy JSON accepts
  the mandatory `consentStatements` object, validates and stores it; current
  canonical SQL SHA256 publication input includes it. Same-key different text
  conflicts. Published text immutable; retire preserves the original text.
- No new table/function signature/role/grant. Runtime still cannot publish/edit.
- No V1-V41 rewrite, repair/backfill or real database migration. Tests use clearly
  synthetic wording in the disposable DB; actual publication requires separately
  approved exact policy data and deployment authorization as already required.

## Use and verification after approval

Applicant UI reads requirements after saving entity/jurisdiction, displays the
published statements and requires explicit unchecked acknowledgements. Save the
existing declarations fields only after those actions; no new Draft/wire fields.
Submit/resubmit revalidate current policy/version/statement availability together
with the existing complete Draft, evidence and contact facts. Lost response still
uses the original key/body/ETag and canonical reread; GET never sends mail or writes.

Focused isolated PG18/HTTP/Browser tests: exact own response, incomplete/foreign/
retired/ambiguous policy denial, immutable text and operator replay, no runtime DML,
explicit checkbox interaction, no default consent/version, unchanged six-field
Application and submitted revision history. No activation, real grants, mail or
commit/push/merge. Independent Admin/V42 work continues while this delta is pending.
