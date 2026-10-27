# Identity target delta — PROPOSED

D-V3-01/02 và CC-V3-IDENTITY-01 kiểm soát amendment. Accepted002/ADR0009–0013/OpenAPI/V1–V6 nguyên vẹn; T-002-05/06 PARTIAL, Feature002/AC NOT ACCEPTED, GRANT_MUTATION_FUNCTIONALITY_LIMITED. Bounded SYSTEM Realm evidence không chứng minh toàn target.

| Requirement | Source | Target operation | Guard/tests |
|---|---|---|---|
| REQ-V3-BC-041 | BC-041/R05 | Quản lý trạng thái tài khoản và cấp quyền Identity | Đúng giới hạn hợp đồng Identity; permission code DECISION_REQUIRED (no runtime allowlist expansion); TEST-V3-BC-041-FLOW; PG/HTTP authorization; browser where applicable; replay/concurrency for mutations |
| REQ-V3-BC-042 | BC-042/R05 | Session revocation và MFA policy được phép | Audit, tránh quyền vượt scope; permission code DECISION_REQUIRED (no runtime allowlist expansion); TEST-V3-BC-042-FLOW; PG/HTTP authorization; browser where applicable; replay/concurrency for mutations |
| REQ-V3-BC-044 | BC-044/R05 | Xử lý credential incident và provisioning nâng cao | Quy trình quản trị riêng; permission code DECISION_REQUIRED (no runtime allowlist expansion); TEST-V3-BC-044-FLOW; PG/HTTP authorization; browser where applicable; replay/concurrency for mutations |
| REQ-V3-BC-085 | BC-085/R10 | Recipient account/login/Portal riêng — KHÔNG THUỘC PHẠM VI | Không cấp Identity role; chỉ public tracking giới hạn; permission code DECISION_REQUIRED (no runtime allowlist expansion); TEST-V3-BC-085-DENY; PG/HTTP authorization; browser where applicable; replay/concurrency for mutations |
| REQ-V3-BC-309 | BC-309/R05 | Provisioning tài khoản theo workflow được duyệt | Không tự cấp quyền nghiệp vụ Merchant/COD; permission code DECISION_REQUIRED (no runtime allowlist expansion); TEST-V3-BC-309-FLOW; PG/HTTP authorization; browser where applicable; replay/concurrency for mutations |
| REQ-V3-BC-310 | BC-310/R05 | Quản trị Realm và policy Identity trong phạm vi | Hạn chế superuser và audit config; permission code DECISION_REQUIRED (no runtime allowlist expansion); TEST-V3-BC-310-FLOW; PG/HTTP authorization; browser where applicable; replay/concurrency for mutations |
| REQ-V3-BC-311 | BC-311/R05 | Xử lý credential incident và thu hồi credential | Incident reference, forced rotation/revocation; permission code DECISION_REQUIRED (no runtime allowlist expansion); TEST-V3-BC-311-FLOW; PG/HTTP authorization; browser where applicable; replay/concurrency for mutations |
| REQ-V3-BC-312 | BC-312/R05 | Quản lý MFA và session revocation theo policy | Không bypass các security gates; permission code DECISION_REQUIRED (no runtime allowlist expansion); TEST-V3-BC-312-FLOW; PG/HTTP authorization; browser where applicable; replay/concurrency for mutations |


## REQ-AB-IDENTITY-UX

Owner target: L002; source BC-132;BC-134;BC-135; latest Owner portal scope, no new BC ID. UI/channel: Registration/verification/login/recovery/security.

Required behavior: Full name/contact categories, password/confirmation, versioned consent, duplicate-safe recovery and limits; credential verified contacts owned Identity. Exact routes/DTO/permission/schema CONFIG/DECISION_REQUIRED before code.

**AC-IDENTITY-UX:** Given valid current actor/resource/state; When thực hiện workflow nguồn; Then No password confirmation persistence; verification/recovery abuse/CSRF/replay/expiry negative and real browser; no customer SYSTEM pair. Negative: wrong owner/revoked/expired/illegal state → no unauthorized effect/private disclosure. Evidence: real source-owner HTTP/PG + browser/channel; TEST-AB-IDENTITY-UX PLANNED_NOT_EXECUTED. Dependencies: L001.

## AC-BC-041-SUCCESS/DENY/RECOVERY và DES-BC-041

REQ-V3-BC-041: Quản lý trạng thái tài khoản và cấp quyền Identity. Guard Đúng giới hạn hợp đồng Identity; permission code DECISION_REQUIRED (no runtime allowlist expansion); scope ASSIGNMENT_STATION_HUB_REGION_CASE. Identity owns credential/session/security records; resource owners retain business data. Exact API/model/policy needs controlled review; current bounded SYSTEM implementation is not full target evidence. Given authorized current actor and full target scope; When operation; Then correct accepted result/audit/receipt or explicit forbidden deny, wrong/revoked scope no leak/effect. Mutation replay/race/rollback tests; reads no unauthorized disclosure. UI admin/security hoặc external denial đúng actor, no Recipient portal.

- [ ] T-BC-041-CONTRACT: close exact permission/input/result/error/state/authority fixtures before affected code.
- [ ] T-BC-041-DB: reviewed forward model/constraints if needed, cold/upgrade/runtime roles, no V1-V6 edit.
- [ ] T-BC-041-BE: implement only accepted current-authority operation; positive/deny/replay/rollback, no SYSTEM widening.
- [ ] T-BC-041-UI: approved human/machine/channel surface or prohibition, no mandatory Recipient IAM.
- [ ] T-BC-041-VERIFY: TEST-V3-BC-041-FLOW; PG/HTTP/browser/security regression and exact CI receipts; target NOT_VERIFIED.

## AC-BC-042-SUCCESS/DENY/RECOVERY và DES-BC-042

REQ-V3-BC-042: Session revocation và MFA policy được phép. Guard Audit, tránh quyền vượt scope; permission code DECISION_REQUIRED (no runtime allowlist expansion); scope ASSIGNMENT_STATION_HUB_REGION_CASE. Identity owns credential/session/security records; resource owners retain business data. Exact API/model/policy needs controlled review; current bounded SYSTEM implementation is not full target evidence. Given authorized current actor and full target scope; When operation; Then correct accepted result/audit/receipt or explicit forbidden deny, wrong/revoked scope no leak/effect. Mutation replay/race/rollback tests; reads no unauthorized disclosure. UI admin/security hoặc external denial đúng actor, no Recipient portal.

- [ ] T-BC-042-CONTRACT: close exact permission/input/result/error/state/authority fixtures before affected code.
- [ ] T-BC-042-DB: reviewed forward model/constraints if needed, cold/upgrade/runtime roles, no V1-V6 edit.
- [ ] T-BC-042-BE: implement only accepted current-authority operation; positive/deny/replay/rollback, no SYSTEM widening.
- [ ] T-BC-042-UI: approved human/machine/channel surface or prohibition, no mandatory Recipient IAM.
- [ ] T-BC-042-VERIFY: TEST-V3-BC-042-FLOW; PG/HTTP/browser/security regression and exact CI receipts; target NOT_VERIFIED.

## AC-BC-044-SUCCESS/DENY/RECOVERY và DES-BC-044

REQ-V3-BC-044: Xử lý credential incident và provisioning nâng cao. Guard Quy trình quản trị riêng; permission code DECISION_REQUIRED (no runtime allowlist expansion); scope ASSIGNMENT_STATION_HUB_REGION_CASE. Identity owns credential/session/security records; resource owners retain business data. Exact API/model/policy needs controlled review; current bounded SYSTEM implementation is not full target evidence. Given authorized current actor and full target scope; When operation; Then correct accepted result/audit/receipt or explicit forbidden deny, wrong/revoked scope no leak/effect. Mutation replay/race/rollback tests; reads no unauthorized disclosure. UI admin/security hoặc external denial đúng actor, no Recipient portal.

- [ ] T-BC-044-CONTRACT: close exact permission/input/result/error/state/authority fixtures before affected code.
- [ ] T-BC-044-DB: reviewed forward model/constraints if needed, cold/upgrade/runtime roles, no V1-V6 edit.
- [ ] T-BC-044-BE: implement only accepted current-authority operation; positive/deny/replay/rollback, no SYSTEM widening.
- [ ] T-BC-044-UI: approved human/machine/channel surface or prohibition, no mandatory Recipient IAM.
- [ ] T-BC-044-VERIFY: TEST-V3-BC-044-FLOW; PG/HTTP/browser/security regression and exact CI receipts; target NOT_VERIFIED.

## AC-BC-085-SUCCESS/DENY/RECOVERY và DES-BC-085

REQ-V3-BC-085: Recipient account/login/Portal riêng — KHÔNG THUỘC PHẠM VI. Guard Không cấp Identity role; chỉ public tracking giới hạn; permission code DECISION_REQUIRED (no runtime allowlist expansion); scope PUBLIC_REDACTED. Identity owns credential/session/security records; resource owners retain business data. Exact API/model/policy needs controlled review; current bounded SYSTEM implementation is not full target evidence. Given authorized current actor and full target scope; When operation; Then correct accepted result/audit/receipt or explicit forbidden deny, wrong/revoked scope no leak/effect. Mutation replay/race/rollback tests; reads no unauthorized disclosure. UI admin/security hoặc external denial đúng actor, no Recipient portal.

- [ ] T-BC-085-CONTRACT: close exact permission/input/result/error/state/authority fixtures before affected code.
- [ ] T-BC-085-DB: reviewed forward model/constraints if needed, cold/upgrade/runtime roles, no V1-V6 edit.
- [ ] T-BC-085-BE: implement only accepted current-authority operation; positive/deny/replay/rollback, no SYSTEM widening.
- [ ] T-BC-085-UI: approved human/machine/channel surface or prohibition, no mandatory Recipient IAM.
- [ ] T-BC-085-VERIFY: TEST-V3-BC-085-DENY; PG/HTTP/browser/security regression and exact CI receipts; target NOT_VERIFIED.

## AC-BC-309-SUCCESS/DENY/RECOVERY và DES-BC-309

REQ-V3-BC-309: Provisioning tài khoản theo workflow được duyệt. Guard Không tự cấp quyền nghiệp vụ Merchant/COD; permission code DECISION_REQUIRED (no runtime allowlist expansion); scope ASSIGNMENT_STATION_HUB_REGION_CASE. Identity owns credential/session/security records; resource owners retain business data. Exact API/model/policy needs controlled review; current bounded SYSTEM implementation is not full target evidence. Given authorized current actor and full target scope; When operation; Then correct accepted result/audit/receipt or explicit forbidden deny, wrong/revoked scope no leak/effect. Mutation replay/race/rollback tests; reads no unauthorized disclosure. UI admin/security hoặc external denial đúng actor, no Recipient portal.

- [ ] T-BC-309-CONTRACT: close exact permission/input/result/error/state/authority fixtures before affected code.
- [ ] T-BC-309-DB: reviewed forward model/constraints if needed, cold/upgrade/runtime roles, no V1-V6 edit.
- [ ] T-BC-309-BE: implement only accepted current-authority operation; positive/deny/replay/rollback, no SYSTEM widening.
- [ ] T-BC-309-UI: approved human/machine/channel surface or prohibition, no mandatory Recipient IAM.
- [ ] T-BC-309-VERIFY: TEST-V3-BC-309-FLOW; PG/HTTP/browser/security regression and exact CI receipts; target NOT_VERIFIED.

## AC-BC-310-SUCCESS/DENY/RECOVERY và DES-BC-310

REQ-V3-BC-310: Quản trị Realm và policy Identity trong phạm vi. Guard Hạn chế superuser và audit config; permission code DECISION_REQUIRED (no runtime allowlist expansion); scope ASSIGNMENT_STATION_HUB_REGION_CASE. Identity owns credential/session/security records; resource owners retain business data. Exact API/model/policy needs controlled review; current bounded SYSTEM implementation is not full target evidence. Given authorized current actor and full target scope; When operation; Then correct accepted result/audit/receipt or explicit forbidden deny, wrong/revoked scope no leak/effect. Mutation replay/race/rollback tests; reads no unauthorized disclosure. UI admin/security hoặc external denial đúng actor, no Recipient portal.

- [ ] T-BC-310-CONTRACT: close exact permission/input/result/error/state/authority fixtures before affected code.
- [ ] T-BC-310-DB: reviewed forward model/constraints if needed, cold/upgrade/runtime roles, no V1-V6 edit.
- [ ] T-BC-310-BE: implement only accepted current-authority operation; positive/deny/replay/rollback, no SYSTEM widening.
- [ ] T-BC-310-UI: approved human/machine/channel surface or prohibition, no mandatory Recipient IAM.
- [ ] T-BC-310-VERIFY: TEST-V3-BC-310-FLOW; PG/HTTP/browser/security regression and exact CI receipts; target NOT_VERIFIED.

## AC-BC-311-SUCCESS/DENY/RECOVERY và DES-BC-311

REQ-V3-BC-311: Xử lý credential incident và thu hồi credential. Guard Incident reference, forced rotation/revocation; permission code DECISION_REQUIRED (no runtime allowlist expansion); scope ASSIGNMENT_STATION_HUB_REGION_CASE. Identity owns credential/session/security records; resource owners retain business data. Exact API/model/policy needs controlled review; current bounded SYSTEM implementation is not full target evidence. Given authorized current actor and full target scope; When operation; Then correct accepted result/audit/receipt or explicit forbidden deny, wrong/revoked scope no leak/effect. Mutation replay/race/rollback tests; reads no unauthorized disclosure. UI admin/security hoặc external denial đúng actor, no Recipient portal.

- [ ] T-BC-311-CONTRACT: close exact permission/input/result/error/state/authority fixtures before affected code.
- [ ] T-BC-311-DB: reviewed forward model/constraints if needed, cold/upgrade/runtime roles, no V1-V6 edit.
- [ ] T-BC-311-BE: implement only accepted current-authority operation; positive/deny/replay/rollback, no SYSTEM widening.
- [ ] T-BC-311-UI: approved human/machine/channel surface or prohibition, no mandatory Recipient IAM.
- [ ] T-BC-311-VERIFY: TEST-V3-BC-311-FLOW; PG/HTTP/browser/security regression and exact CI receipts; target NOT_VERIFIED.

## AC-BC-312-SUCCESS/DENY/RECOVERY và DES-BC-312

REQ-V3-BC-312: Quản lý MFA và session revocation theo policy. Guard Không bypass các security gates; permission code DECISION_REQUIRED (no runtime allowlist expansion); scope ASSIGNMENT_STATION_HUB_REGION_CASE. Identity owns credential/session/security records; resource owners retain business data. Exact API/model/policy needs controlled review; current bounded SYSTEM implementation is not full target evidence. Given authorized current actor and full target scope; When operation; Then correct accepted result/audit/receipt or explicit forbidden deny, wrong/revoked scope no leak/effect. Mutation replay/race/rollback tests; reads no unauthorized disclosure. UI admin/security hoặc external denial đúng actor, no Recipient portal.

- [ ] T-BC-312-CONTRACT: close exact permission/input/result/error/state/authority fixtures before affected code.
- [ ] T-BC-312-DB: reviewed forward model/constraints if needed, cold/upgrade/runtime roles, no V1-V6 edit.
- [ ] T-BC-312-BE: implement only accepted current-authority operation; positive/deny/replay/rollback, no SYSTEM widening.
- [ ] T-BC-312-UI: approved human/machine/channel surface or prohibition, no mandatory Recipient IAM.
- [ ] T-BC-312-VERIFY: TEST-V3-BC-312-FLOW; PG/HTTP/browser/security regression and exact CI receipts; target NOT_VERIFIED.
