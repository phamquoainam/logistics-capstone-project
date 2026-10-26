# L009 — tracking-event-core-public-lookup

PROPOSED / BLOCKED_FOR_IMPLEMENTATION. Primary accountable domain: orders; wave F1; actors R01,R02,R03,R06,R07,R08,R09,R10,R14,R17,R18,R19,R20,R21.

## Outcome và scope

Tracking an toàn và có nguồn. Không cấp quyền từ role label; không ghi dữ liệu owner khác;5 DEFER chỉ denial. CORE wave sau vẫn trong target.

## Requirements theo source

[371 BC crosswalk](../../../../docs/reconciliation/v3/capability-crosswalk.csv) giữ từng guard/owner/status. [Legacy crosswalk](../../../../docs/reconciliation/v3/legacy-crosswalk.csv) bảo toàn FR/REQ/AC/task, không tự coi mapping là equivalence.

| Requirement | Source | Hành vi | Guard bắt buộc | Acceptance kind |
|---|---|---|---|---|
| REQ-V3-BC-003 | BC-003/R01/M11 | Tra cứu tracking giới hạn | Không lộ PII, COD nhạy cảm; tăng xác minh khi cần; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-013 | BC-013/R02/M11 | Theo dõi mọi Shipment thuộc Merchant | Không cross-tenant; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-027 | BC-027/R03/M11 | Tra cứu trạng thái Shipment thuộc scope | Merchant isolation; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-051 | BC-051/R06/M11 | Theo dõi sự kiện và nhiệm vụ vận chuyển | Không truy cập ngoài region; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-060 | BC-060/R07/M11 | Xem lịch sử nhiệm vụ cá nhân | PII tối thiểu; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-068 | BC-068/R08/M11 | Xem trạng thái kiện tại Station | Đúng station scope; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-078 | BC-078/R09/M11 | Xem thông tin liên hệ người nhận tối thiểu | Chỉ trong assignment; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-080 | BC-080/R10/M11 | Nhận/tra cứu thông tin trạng thái giới hạn | Không login; xác minh phù hợp; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-106 | BC-106/R14/M11 | Tra tracking, POD và điểm quét cuối | Chỉ ticket/scope được phân công; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-126 | BC-126/R17/M11 | Xem lịch sử tracking/custody có chứng cứ | Chỉ dữ liệu được cấp; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-143 | BC-143/R18/M11 | Tra cứu tất cả Shipment do mình tạo | Resource owner isolation; không cross-account; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-170 | BC-170/R19/M11 | Theo dõi giao chậm, vận đơn bất thường và tiến độ | Đúng scope Merchant/branch; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-186 | BC-186/R20/M11 | Theo dõi trạng thái kiện đã bàn giao cho hãng | Chỉ kho/đơn thuộc scope; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-194 | BC-194/R21/M11 | Theo dõi giao chậm, giao thất bại và thông báo bất thường | Chỉ dữ liệu cần cho CSKH; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-293 | BC-293/R14/M11 | Xử lý yêu cầu giao chậm qua điểm quét cuối | Không giả lập tracking event; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-315 | BC-315/R17/M11 | Đọc tracking events để kiểm toán vận chuyển | Không sửa trạng thái/attempt; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |

## Workflow và acceptance

Source commit→timeline; đoán code/UUID không lộ PII; late/duplicate không sửa lịch sử.

Với từng requirement: Given actor/resource đúng scope và preconditions, When thực hiện operation nguồn, Then chỉ outcome đúng guard được commit với provenance. Với NEGATIVE/DENY: When gọi trực tiếp API, Then từ chối và không ghi effect. Given expired/revoked/wrong resource/state, When submit/replay/recovery, Then không leak data/receipt hay mutation. Given mất response sau commit, When recovery đúng contract, Then không lặp effect. Required test IDs tại CSV là planned, chưa có runtime receipt.

## Dependencies và readiness

Hard: L006. Soft/consumer: L028; soft không cho phép fake missing facts. [Roadmap](../../../../docs/implementation/roadmap-v3.md) phân biệt minimum contract slice và full acceptance.

Blockers: D-V3-06; DEC-20. Owner: Project Owner + Tech Lead; Security cho IAM/privacy; Finance cho money; Operations cho custody. Owner duyệt mapping/decisions, API/event/state/data owner và fixtures; không BLOCKING trong slice; positive/deny/concurrency/recovery đã thiết kế; canonical docs/codex/definition-of-ready.md.

## Delivery gate

BE+HTTP+DB+FE thật, PostgreSQL invariants/guards và browser/native evidence phù hợp, source+PR CI pass; BC/test/evidence cập nhật; Owner merge/acceptance riêng. [Design](design.md), [tasks](tasks.md), [shared convention](../../../../docs/reconciliation/v3/delivery-plan.md). Không promote IMPLEMENTED/VERIFIED do có file.
