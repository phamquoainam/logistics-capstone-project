# Administration Console role and capability reconciliation

Owner direction2026-11-23 adopts A/B as product targets, while approved technical
contracts and security invariants remain authoritative for execution. No role or
grant was changed in this checkpoint.

## Role vocabulary conflict

| Documentary ID | Existing business matrix | Document A/B target | Disposition |
|---|---|---|---|
| R28 | Pricing / Billing Administrator | Business Admin | Different capability sets; no automatic widening |
| R29 | Sales / Key Account Manager | Platform Super Admin | Must not convert Sales into Platform authority |
| R30 | Risk / Fraud / Compliance Officer | Identity / Security Administrator | Risk role does not become IAM grant issuer |
| R31 | Platform Super Admin | IT / Integration Operations | Preserve current identity; no implicit Finance/PII |
| R32 | IT / Integration Operations | Auditor read-only | Existing operational permissions must not become an Auditor template |

Refer to [existing matrix](../business-references/E_Logistics_Role_Module_Business_Capability_Matrix_GroupEF_Full.md),
role table, and A sections I/V plus B sections I/IV. Treat ADMIN_SPEC.R28–R32 and
BUSINESS_MATRIX.R28–R32 as documentary namespaces in this mapping, not runtime IDs.
No code uses either R label to grant or decide access. Naming migration, any new
canonical role identifiers/templates and legacy compatibility need one explicit
controlled amendment. R33 Partner remains outside Admin by default.

## Current executable capability mapping

| Capability | Current permission | Resource scope/admission | UI and Backend enforcement | Delegated authority |
|---|---|---|---|---|
| Enter/read own Admin context | ADMIN_DASHBOARD_READ | Dedicated verified SYSTEM Admin; bound LOCAL/TEST realm | Admin client plus JdbcAdminAdmission/context | None |
| Application aggregate/source read | ADMIN_MERCHANT_READ | Same current Admin context | Merchant tab/dependency view; owner read port; no submitted source yet | No approve/reject |
| Official network source read | ADMIN_NETWORK_READ | Same current Admin context | Network tab/owner port; no official source yet | No site changes |
| Booking/Parcel/intake oversight | ADMIN_SHIPMENT_READ | Existing admitted Local/Test cohort | Snapshot-bound list API, no recipient PII | No custody/POD mutations |
| Confirmed fees/UNPAID | ADMIN_FINANCE_READ | Same context; Shipment grant additionally required for list drill | Backend checks before Finance port; UI hides unauthorized data | No collect/ledger/settle |
| Bounded API availability | ADMIN_TECHNICAL_READ | Same context | Technical API/widget; no arbitrary actuator/secret access | No job replay/deploy |
| Root IAM | IDENTITY_MANAGEMENT | Existing Initial Root/IAM admission | Existing canonical IAM guards; Root denied Admin business admission | Existing IAM policy only |
| CSKH execution | SUPPORT_TRIAGE / SUPPORT_HANDLE | Separate STAFF/SUPPORT admission and case/org binding | Existing Support APIs and Staff client under finance shell | No Admin/Finance authority |

An available menu, technical grant or verified Staff identity never substitutes
for an object-level permission. Current Admin grants have IDENTITY/SYSTEM scope;
there is no implemented regional Admin assignment. Do not treat region filter,
Merchant UUID or SIM code as a grant. A region-scoped deployment remains blocked
until an authoritative scope contract exists.

## Target role principles requiring granular contracts

- Business Admin: governance decisions only for explicit domain/delegation/scope;
  applicant and conflicted reviewer cannot approve. Approval does not create a
  physical Carrier event or Finance transaction outside the owning workflow.
- Platform and IT: no implicit business or PII access. Technical investigation
  cannot impersonate a Customer or edit Shipment/ledger through SQL.
- Identity/Security: existing Root protections, genuine verification, no default
  Merchant approval, no self-promotion or last-admin removal.
- Auditor: read-only grants and current resource scope. Export is a separately
  authorized audited disclosure; it is not automatically harmless or permitted.
- Partner: assigned Partner scope only, no central Admin or unrelated sites.

No MFA/SoD/approval-limit mechanism is claimed from read-only screens. New
delegation, masked/unmasked PII, audit export and approval requests remain
PROPOSED in the [single amendment](admin-console-contract-amendment.proposed.md).

## Target role to enforcement crosswalk (documentary, not runtime grants)

| Target role in A/B | Business capability | Current permission reuse if independently granted | Resource scope | Delegated authority | Enforcement / missing contract |
|---|---|---|---|---|---|
| ADMIN_SPEC.R28 Business Admin | Merchant/network/Shipment oversight, business decisions | ADMIN_DASHBOARD_READ plus exact MERCHANT/NETWORK/SHIPMENT READ as required | Current SYSTEM Local/Test only; regional/Organization limits not yet contracted | None from READ; approve/reject requires exact independent mandate/SoD | Existing read Backend admission; decision/directory workflows ADM-GAP-01–04 remain blocked |
| ADMIN_SPEC.R29 Platform Super Admin | Platform/configuration governance | ADMIN_DASHBOARD_READ + ADMIN_TECHNICAL_READ only if explicitly granted | Explicit environment and current realm; no inferred Customer/Finance scope | No implicit IAM/Finance/PII/Carrier write | Platform read enforces granular grant; config/change approvals require ADM-GAP-06 |
| ADMIN_SPEC.R30 Identity/Security Admin | Identity security, privileged-access review, privacy | No automatic READ or IDENTITY_MANAGEMENT assignment | Existing separate IAM Root/management contracts; proposed scoped governance | Existing IAM workflows only for actual IAM grantee; never implicit Root Dashboard access | V4/V39/V40 guards preserved; incident/audit/PII readers require ADM-GAP-05 |
| ADMIN_SPEC.R31 IT/Integration | API/job/integration diagnostics | ADMIN_DASHBOARD_READ + ADMIN_TECHNICAL_READ if independently granted | Current bound environment; no implicit financial or recipient PII scope | No replay/release/secrets/config authority from technical READ | PlatformStatus reader only; telemetry/action contracts absent |
| ADMIN_SPEC.R32 Auditor | Scoped evidence/report review | No blanket permission set; exact READ must be approved independently | Current persisted permission context; regional/export scope absent | Read-only, no mutation/approval/self-grant | No Admin audit/export reader yet; new audit/disclosure contract required |

These conditional reuse rows do not recommend automatically granting every named
permission to a role. They expose exactly which parts already have an enforceable
read contract and which need a separately approved policy and Backend authority.

## P0 resolution proposal — no role data migration

See ROLE-P0-01 in the [bounded Identity/context contract](admin-p0-identity-context-contract.proposed.md).
ADMIN_BUSINESS_GOVERNANCE, ADMIN_PLATFORM_GOVERNANCE, ADMIN_IDENTITY_SECURITY,
ADMIN_TECHNICAL_OPERATIONS and ADMIN_SCOPED_AUDITOR are documentary semantic keys,
not new runtime templates or grant codes. Historical matrix labels keep their
original vocabulary. No role-name/numeric-ID conversion is proposed.

P0 Merchant review requires both the actual dedicated SYSTEM Admin entry/READ
admission and separately persisted Merchant POLICY scope. Organization scope
cannot grant application review/decision/task capabilities. Assignment is issued
only by an independent authorized offline operator under the proposed manifest;
no default title grants, self-promotion, IAM, Carrier or Finance expansion.

The [consolidated manifest](admin-p0-implementation-manifest.proposed.json) awaits
Owner approval; the above current executable mapping is unchanged. P0 does not
implement principal linking or claim that current SYSTEM realm is a region scope.
