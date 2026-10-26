# L022 — enterprise-api-webhooks

PROPOSED / BLOCKED_FOR_IMPLEMENTATION. Primary accountable domain: merchant; wave F4; actors R02,R03,R22,R32.

## Outcome và scope

Enterprise API principal và webhook. Không cấp quyền từ role label; không ghi dữ liệu owner khác;5 DEFER chỉ denial. CORE wave sau vẫn trong target.

## Requirements theo source

[371 BC crosswalk](../../../../docs/reconciliation/v3/capability-crosswalk.csv) giữ từng guard/owner/status. [Legacy crosswalk](../../../../docs/reconciliation/v3/legacy-crosswalk.csv) bảo toàn FR/REQ/AC/task, không tự coi mapping là equivalence.

| Requirement | Source | Hành vi | Guard bắt buộc | Acceptance kind |
|---|---|---|---|---|
| REQ-V3-BC-018 | BC-018/R02/M17 | Quản lý API integration/webhook Merchant | Key và scope tách riêng; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-164 | BC-164/R02/M17 | Quản lý cấu hình tích hợp và webhook trong scope Merchant | Credential secret bảo vệ; không tự cấp scope cao hơn; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-175 | BC-175/R03/M17 | Đồng bộ đơn từ hệ thống bán hàng trong phạm vi được cấp | Credential dùng integration principal, không chia sẻ human secret; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-200 | BC-200/R22/M01 | Xác thực integration principal và quản lý scope API gắn Merchant | Credential tách human account; rotate, revoke, audit; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-201 | BC-201/R22/M17 | Thiết lập endpoint và subscription webhook trạng thái | Tenant bound, signature, retry, duplicate handling; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-202 | BC-202/R22/M05 | Tạo và đồng bộ Shipment từ ERP/OMS/WMS/website | API idempotency key; đúng Merchant; validation; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-203 | BC-203/R22/M04 | Lấy báo giá và danh sách dịch vụ được phép qua API | Áp dụng đúng hợp đồng Merchant; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-204 | BC-204/R22/M06 | Lấy nhãn và barcode của Shipment/Parcel qua API | Chỉ tài nguyên thuộc Merchant; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-205 | BC-205/R22/M11 | Tra cứu Shipment và nhận tracking events | Hạn chế PII; filtering theo Merchant; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-206 | BC-206/R22/M17 | Đối chiếu lỗi đồng bộ, retry và failed webhook deliveries | Quan sát idempotency; không tạo bút toán/đơn trùng; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-357 | BC-357/R32/M17 | Điều tra lỗi webhook và lịch sử retry | Idempotent retries; không tạo duplicate state; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-358 | BC-358/R32/M17 | Quản lý cấu hình tích hợp theo quyền kỹ thuật | Tách secret và business action; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-360 | BC-360/R32/M05 | Đối chiếu lỗi đồng bộ Shipment | Không sửa nghiệp vụ vượt quyền; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |

## Workflow và acceptance

Scoped credential→quote/create/label/tracking→signed webhook/retry; revoke/cross-tenant deny.

Với từng requirement: Given actor/resource đúng scope và preconditions, When thực hiện operation nguồn, Then chỉ outcome đúng guard được commit với provenance. Với NEGATIVE/DENY: When gọi trực tiếp API, Then từ chối và không ghi effect. Given expired/revoked/wrong resource/state, When submit/replay/recovery, Then không leak data/receipt hay mutation. Given mất response sau commit, When recovery đúng contract, Then không lặp effect. Required test IDs tại CSV là planned, chưa có runtime receipt.

## Dependencies và readiness

Hard: L002,L003,L005,L007,L008,L009. Soft/consumer: none; soft không cho phép fake missing facts. [Roadmap](../../../../docs/implementation/roadmap-v3.md) phân biệt minimum contract slice và full acceptance.

Blockers: D-V3-02; D-V3-06; D-V3-08. Owner: Project Owner + Tech Lead; Security cho IAM/privacy; Finance cho money; Operations cho custody. Owner duyệt mapping/decisions, API/event/state/data owner và fixtures; không BLOCKING trong slice; positive/deny/concurrency/recovery đã thiết kế; canonical docs/codex/definition-of-ready.md.

## Delivery gate

BE+HTTP+DB+FE thật, PostgreSQL invariants/guards và browser/native evidence phù hợp, source+PR CI pass; BC/test/evidence cập nhật; Owner merge/acceptance riêng. [Design](design.md), [tasks](tasks.md), [shared convention](../../../../docs/reconciliation/v3/delivery-plan.md). Không promote IMPLEMENTED/VERIFIED do có file.
