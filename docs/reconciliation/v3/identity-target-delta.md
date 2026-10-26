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
