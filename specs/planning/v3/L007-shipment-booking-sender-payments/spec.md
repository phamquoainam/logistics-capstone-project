# L007 — shipment-booking-sender-payments

PROPOSED / BLOCKED_FOR_IMPLEMENTATION. Primary accountable domain: orders; wave F1; actors R01,R02,R03,R18,R19,R21,R23.

## Outcome và scope

Booking Sender/Merchant và thu cước đúng. Không cấp quyền từ role label; không ghi dữ liệu owner khác;5 DEFER chỉ denial. CORE wave sau vẫn trong target.

## Requirements theo source

[371 BC crosswalk](../../../../docs/reconciliation/v3/capability-crosswalk.csv) giữ từng guard/owner/status. [Legacy crosswalk](../../../../docs/reconciliation/v3/legacy-crosswalk.csv) bảo toàn FR/REQ/AC/task, không tự coi mapping là equivalence.

| Requirement | Source | Hành vi | Guard bắt buộc | Acceptance kind |
|---|---|---|---|---|
| REQ-V3-BC-006 | BC-006/R01/M05 | Tự tạo Shipment online khi không có tài khoản — BỊ CẤM theo phạm vi Capstone | Không cấp API public tạo Shipment; ký gửi tại quầy do Station Operator thực hiện; permission code DECISION_REQUIRED (no runtime allowlist expansion) | NEGATIVE/DENY |
| REQ-V3-BC-011 | BC-011/R02/M05 | Tạo, xem, xác nhận Shipment | Membership, valid state, idempotency; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-012 | BC-012/R02/M05 | Yêu cầu hủy/sửa Shipment | Chỉ trạng thái cho phép; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-024 | BC-024/R03/M05 | Tạo/xác nhận Shipment | Idempotency, đúng org; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-025 | BC-025/R03/M05 | Xem và yêu cầu hủy/giao lại | Hủy/sửa phải đúng quyền và state; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-139 | BC-139/R18/M05 | Tạo và xác nhận Shipment gửi hàng cá nhân | Shipment thuộc account; valid state, chống trùng/idempotency; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-144 | BC-144/R18/M05 | Hủy yêu cầu gửi khi trạng thái cho phép | Đã pickup phải chuyển workflow thu hồi/hoàn; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-147 | BC-147/R18/M05 | Xem lịch sử vận đơn đã gửi | Giới hạn theo account và quyền riêng tư; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-160 | BC-160/R02/M05 | Tạo Shipment thủ công và hàng loạt, quản lý toàn bộ Shipment Merchant | Có kiểm tra dữ liệu và idempotency, không sửa lịch sử; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-169 | BC-169/R19/M05 | Kiểm tra Shipment cần xử lý và duyệt sửa/hủy theo chính sách | State guard; không ghi đè Shipment đã chốt trái quyền; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-193 | BC-193/R21/M05 | Tra cứu Order/Shipment của Merchant để hỗ trợ người mua | Không xem Shipment Merchant khác; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-243 | BC-243/R23/M18 | Không sửa đơn Merchant hoặc ghi DELIVERED thay Courier | Negative authorization cho commercial state/POD; permission code DECISION_REQUIRED (no runtime allowlist expansion) | NEGATIVE/DENY |

## Workflow và acceptance

Confirm→một charge/hold/snapshot; thiếu tiền/tồn rollback; timeout tra cứu cùng key.

Với từng requirement: Given actor/resource đúng scope và preconditions, When thực hiện operation nguồn, Then chỉ outcome đúng guard được commit với provenance. Với NEGATIVE/DENY: When gọi trực tiếp API, Then từ chối và không ghi effect. Given expired/revoked/wrong resource/state, When submit/replay/recovery, Then không leak data/receipt hay mutation. Given mất response sau commit, When recovery đúng contract, Then không lặp effect. Required test IDs tại CSV là planned, chưa có runtime receipt.

## Dependencies và readiness

Hard: L005,L006,L018. Soft/consumer: none; soft không cho phép fake missing facts. [Roadmap](../../../../docs/implementation/roadmap-v3.md) phân biệt minimum contract slice và full acceptance.

Blockers: D-V3-04; DEC-09; DEC-10; DEC-11; DEC-12. Owner: Project Owner + Tech Lead; Security cho IAM/privacy; Finance cho money; Operations cho custody. Owner duyệt mapping/decisions, API/event/state/data owner và fixtures; không BLOCKING trong slice; positive/deny/concurrency/recovery đã thiết kế; canonical docs/codex/definition-of-ready.md.

## Delivery gate

BE+HTTP+DB+FE thật, PostgreSQL invariants/guards và browser/native evidence phù hợp, source+PR CI pass; BC/test/evidence cập nhật; Owner merge/acceptance riêng. [Design](design.md), [tasks](tasks.md), [shared convention](../../../../docs/reconciliation/v3/delivery-plan.md). Không promote IMPLEMENTED/VERIFIED do có file.
