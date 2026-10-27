# Quyết định nghiệp vụ — một tài khoản cá nhân trước

ID: OWNER-ACCOUNT-FIRST-2026-10-27. Status: OWNER_DECIDED_BUSINESS_DIRECTION; documentation rebuild authorized. Không phải approval cho runtime, exact schema/wire, provider hoặc feature acceptance. Nguồn: prompt Owner “FULL CANONICAL SDD V3 REBUILD & GROUP A/B PORTAL COMPLETENESS AUDIT”, §§2–3/10–15; supersedes luồng chọn loại tài khoản của reference 8.9 và review Spec003 FIXES_REQUIRED.

## Quyết định có hiệu lực cho target

1. Guest đăng ký **một customer account cá nhân**, không chọn Retail/Merchant. Sau Identity verification/activation và login đủ điều kiện, default là workspace cá nhân.
2. Customer verified/eligible đăng nhập bằng credential của chính mình mới được nộp “Đăng ký trở thành Merchant”. BC132 giữ public personal entry; BC133 giữ ID nhưng actor hiện hành là R18, không Guest submit.
3. Application không cấp Owner. Chỉ review đúng scope + representative-authority verification + controlled atomic organization/initial-membership activation mới có Merchant access. Rejected/suspended application/org không tự disable unrelated Sender access.
4. Mọi Owner/staff dùng personal identity riêng; Merchant là tổ chức; active membership và delegation xét hiện hành mỗi request. Không default SYSTEM/Finance/Courier privilege, không impersonation.
5. Personal workspace luôn tách org. Multi-org chỉ được cấp qua policy explicit và từng membership; không suy quyền từ email, role label hoặc org ID. Khuyến nghị multi-org qua explicit approved admission/invitation, policy version và isolation tests; chi tiết POLICY-003-01 còn chờ review.
6. Guest không tạo anonymous online Shipment kể cả có OTP (BC006 deny). Walk-in do authorized Station staff xử lý với verification/payment/receipt/custody. Recipient external không mandatory IAM/Portal (BC085 deny); integration principal tách human secret.

## Precedence và amendment

Latest Owner decision → accepted architecture/security giữ runtime cho tới controlled amendment → canonical target requirements → historical/proposed references → UX examples. D01/D02 business direction đã chốt; IAM-CUSTOMER-01/IAM-CONTEXT-02/IAM-INVITE-03 vẫn BLOCKING trước production code. Không sửa ADR0009–0013/OpenAPI hay V1–V6 trong rebuild.

Full name, email và/hoặc phone, password/confirmation, consent/version, verification, duplicate handling và Recovery là yêu cầu UX/behavior. Exact transport fields, credential contacts, verification channel/TTL/limits/retention và consent policy cần review. Password confirmation là input UX, không persistence/response. Không tự cho mọi contact có cờ verified.

## Capstone verification

Đề xuất manual/sandbox review: entity-type policy quyết định bộ thông tin và evidence; reviewer có scope và reason; audit rõ MANUAL_SANDBOX, không tuyên bố đã xác minh qua cơ quan nhà nước hay pháp lý production. Không dùng dữ liệu thật trong fixture. Missing/ambiguous evidence → request information/reject; không approve mặc định.

[Identity amendment](../reconciliation/v3/identity-customer-amendment.md) · [Spec003](../../specs/003-sender-merchant-onboarding/spec.md) · [Portal acceptance](../portal/group-ab-acceptance.md).
