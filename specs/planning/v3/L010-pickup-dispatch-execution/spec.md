# L010 — pickup-dispatch-execution

PROPOSED / BLOCKED_FOR_IMPLEMENTATION. Primary accountable domain: dispatch; wave F2; actors R04,R06,R07,R18,R24,R27.

## Outcome và scope

Dispatcher giao việc, Courier nhận kiện. Không cấp quyền từ role label; không ghi dữ liệu owner khác;5 DEFER chỉ denial. CORE wave sau vẫn trong target.

## Requirements theo source

[371 BC crosswalk](../../../../docs/reconciliation/v3/capability-crosswalk.csv) giữ từng guard/owner/status. [Legacy crosswalk](../../../../docs/reconciliation/v3/legacy-crosswalk.csv) bảo toàn FR/REQ/AC/task, không tự coi mapping là equivalence.

| Requirement | Source | Hành vi | Guard bắt buộc | Acceptance kind |
|---|---|---|---|---|
| REQ-V3-BC-037 | BC-037/R04/M07 | Phê duyệt ngoại lệ điều phối theo thẩm quyền | Không tự tạo bằng chứng pickup; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-047 | BC-047/R06/M07 | Xem pickup pool và phân công pickup | Theo vùng, tải và trạng thái; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-049 | BC-049/R06/M07 | Tái phân công pickup có lý do | Audit, không tự xác nhận pickup; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-056 | BC-056/R07/M07 | Xem pickup assignment cá nhân | Chỉ task được phân công; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-057 | BC-057/R07/M07 | Check-in/scan và ghi nhận PICKED_UP | Có bằng chứng, đúng trạng thái; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-058 | BC-058/R07/M07 | Ghi pickup thất bại và lý do | Attempt có timestamp; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-062 | BC-062/R07/M07 | Tối ưu thứ tự tuyến pickup | Không bắt buộc GPS/routing chuyên sâu; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-141 | BC-141/R18/M07 | Đặt yêu cầu Pickup tại nhà | Dịch vụ/địa chỉ đủ điều kiện; phân công do Operations; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-207 | BC-207/R06/M07 | Phân công Pickup Courier theo khu vực, năng lực và tải hiện tại | Station/region scope; chỉ assignment đủ điều kiện; chống phân công trùng; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-210 | BC-210/R07/M11 | Xem tuyến, thứ tự điểm lấy và địa chỉ Merchant/Retail Sender | Chỉ nhiệm vụ đang được giao; PII tối thiểu; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-211 | BC-211/R07/M07 | Liên hệ Merchant/Retail Sender phục vụ pickup | Chỉ trong phạm vi nhiệm vụ; log/quy tắc bảo vệ liên hệ; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-214 | BC-214/R07/M07 | Xác nhận thời gian và bằng chứng bàn giao nhận kiện | Assignment hợp lệ; timestamp, actor, custody; không button-only PICKED_UP; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-244 | BC-244/R24/M07 | Xem nhiệm vụ Pickup Courier toàn đội | Chỉ đội/ca được giao; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-247 | BC-247/R24/M07 | Đề xuất tái phân công pickup | Approval/authorization tách biệt với hành động reassignment; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-277 | BC-277/R27/M07 | Giám sát năng lực pickup giữa các khu vực | Không tự tạo chứng cứ PICKED_UP; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |

## Workflow và acceptance

Assign→scan/count→nhận→handover; stale/reassign/double scan deny.

Với từng requirement: Given actor/resource đúng scope và preconditions, When thực hiện operation nguồn, Then chỉ outcome đúng guard được commit với provenance. Với NEGATIVE/DENY: When gọi trực tiếp API, Then từ chối và không ghi effect. Given expired/revoked/wrong resource/state, When submit/replay/recovery, Then không leak data/receipt hay mutation. Given mất response sau commit, When recovery đúng contract, Then không lặp effect. Required test IDs tại CSV là planned, chưa có runtime receipt.

## Dependencies và readiness

Hard: L007,L008,L009. Soft/consumer: none; soft không cho phép fake missing facts. [Roadmap](../../../../docs/implementation/roadmap-v3.md) phân biệt minimum contract slice và full acceptance.

Blockers: DEC-14; D-V3-05. Owner: Project Owner + Tech Lead; Security cho IAM/privacy; Finance cho money; Operations cho custody. Owner duyệt mapping/decisions, API/event/state/data owner và fixtures; không BLOCKING trong slice; positive/deny/concurrency/recovery đã thiết kế; canonical docs/codex/definition-of-ready.md.

## Delivery gate

BE+HTTP+DB+FE thật, PostgreSQL invariants/guards và browser/native evidence phù hợp, source+PR CI pass; BC/test/evidence cập nhật; Owner merge/acceptance riêng. [Design](design.md), [tasks](tasks.md), [shared convention](../../../../docs/reconciliation/v3/delivery-plan.md). Không promote IMPLEMENTED/VERIFIED do có file.
