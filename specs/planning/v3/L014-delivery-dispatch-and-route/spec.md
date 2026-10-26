# L014 — delivery-dispatch-and-route

PROPOSED / BLOCKED_FOR_IMPLEMENTATION. Primary accountable domain: dispatch; wave F2; actors R03,R06,R09,R16,R19,R21,R24,R27.

## Outcome và scope

Phân công giao và route có custody. Không cấp quyền từ role label; không ghi dữ liệu owner khác;5 DEFER chỉ denial. CORE wave sau vẫn trong target.

## Requirements theo source

[371 BC crosswalk](../../../../docs/reconciliation/v3/capability-crosswalk.csv) giữ từng guard/owner/status. [Legacy crosswalk](../../../../docs/reconciliation/v3/legacy-crosswalk.csv) bảo toàn FR/REQ/AC/task, không tự coi mapping là equivalence.

| Requirement | Source | Hành vi | Guard bắt buộc | Acceptance kind |
|---|---|---|---|---|
| REQ-V3-BC-048 | BC-048/R06/M10 | Xem delivery pool và phân công giao | Kiện đủ điều kiện; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-050 | BC-050/R06/M10 | Điều phối giao lại | Delivery attempt và chính sách; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-054 | BC-054/R06/M10 | Gom điểm/tối ưu tuyến | Chưa yêu cầu routing optimization chuyên sâu; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-071 | BC-071/R09/M10 | Xem và nhận kiện giao theo assignment | Chỉ nhiệm vụ cá nhân; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-072 | BC-072/R09/M10 | Ghi OUT_FOR_DELIVERY | Chỉ khi kiện đã được bàn giao hợp lệ; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-079 | BC-079/R09/M10 | Tuyến giao hàng tối ưu/GPS real-time | Không bắt buộc với MVP; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-119 | BC-119/R16/M07 | Giám sát pickup/courier thuộc Station | Không xác nhận PICKED_UP thay courier; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-120 | BC-120/R16/M10 | Giám sát giao thất bại và tồn phát | Không tạo POD giả; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-171 | BC-171/R19/M10 | Yêu cầu xử lý giao thất bại và giao lại | Không tạo DELIVERED/POD thay Courier; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-177 | BC-177/R03/M10 | Gửi yêu cầu giao lại qua workflow theo quyền | Không tự gán Courier hoặc POD; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-197 | BC-197/R21/M10 | Yêu cầu giao lại và cung cấp chỉ dẫn giao có kiểm soát | Không trực tiếp ghi trạng thái giao; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-208 | BC-208/R06/M10 | Phân công Delivery Courier theo khu vực, tuyến và năng lực | Kiện sẵn sàng giao, custody/scan và assignment hợp lệ; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-222 | BC-222/R09/M06 | Quét barcode xác nhận nhận tải kiện từ bưu cục | Handover hai phía, đúng assignment, không nhận trùng; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-223 | BC-223/R09/M11 | Xem tuyến giao, bản đồ và điểm dừng | Chỉ nhiệm vụ được giao; địa chỉ và PII tối thiểu; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-224 | BC-224/R09/M10 | Liên hệ Recipient theo chính sách bảo vệ số điện thoại | Chỉ khi assignment hợp lệ; bảo vệ thông tin liên hệ; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-226 | BC-226/R09/M10 | Thực hiện giao lại theo assignment mới | Delivery attempt riêng; không tái sử dụng assignment hết hiệu lực; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-245 | BC-245/R24/M10 | Xem nhiệm vụ Delivery Courier toàn đội | Không tự có quyền giao thay Courier; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-248 | BC-248/R24/M10 | Đề xuất tái phân công delivery | Không tự cấp assignment rồi tự ghi POD; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-274 | BC-274/R27/M10 | Theo dõi tỷ lệ giao thành công toàn mạng | KPI từ attempts/tracking, không sửa POD; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-278 | BC-278/R27/M10 | Điều phối chuyển vùng phát trong ngoại lệ | Tôn trọng state, active assignment và custody; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |

## Workflow và acceptance

Kiện đích hợp lệ→assign/load→route; race/revoked không tạo hai active.

Với từng requirement: Given actor/resource đúng scope và preconditions, When thực hiện operation nguồn, Then chỉ outcome đúng guard được commit với provenance. Với NEGATIVE/DENY: When gọi trực tiếp API, Then từ chối và không ghi effect. Given expired/revoked/wrong resource/state, When submit/replay/recovery, Then không leak data/receipt hay mutation. Given mất response sau commit, When recovery đúng contract, Then không lặp effect. Required test IDs tại CSV là planned, chưa có runtime receipt.

## Dependencies và readiness

Hard: L011,L013. Soft/consumer: none; soft không cho phép fake missing facts. [Roadmap](../../../../docs/implementation/roadmap-v3.md) phân biệt minimum contract slice và full acceptance.

Blockers: DEC-14; DEC-17; DEC-18. Owner: Project Owner + Tech Lead; Security cho IAM/privacy; Finance cho money; Operations cho custody. Owner duyệt mapping/decisions, API/event/state/data owner và fixtures; không BLOCKING trong slice; positive/deny/concurrency/recovery đã thiết kế; canonical docs/codex/definition-of-ready.md.

## Delivery gate

BE+HTTP+DB+FE thật, PostgreSQL invariants/guards và browser/native evidence phù hợp, source+PR CI pass; BC/test/evidence cập nhật; Owner merge/acceptance riêng. [Design](design.md), [tasks](tasks.md), [shared convention](../../../../docs/reconciliation/v3/delivery-plan.md). Không promote IMPLEMENTED/VERIFIED do có file.
