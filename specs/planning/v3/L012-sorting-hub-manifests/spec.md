# L012 — sorting-hub-manifests

PROPOSED / BLOCKED_FOR_IMPLEMENTATION. Primary accountable domain: dispatch; wave F2; actors R25,R26.

## Outcome và scope

Sorting và Hub manifests. Không cấp quyền từ role label; không ghi dữ liệu owner khác;5 DEFER chỉ denial. CORE wave sau vẫn trong target.

## Requirements theo source

[371 BC crosswalk](../../../../docs/reconciliation/v3/capability-crosswalk.csv) giữ từng guard/owner/status. [Legacy crosswalk](../../../../docs/reconciliation/v3/legacy-crosswalk.csv) bảo toàn FR/REQ/AC/task, không tự coi mapping là equivalence.

| Requirement | Source | Hành vi | Guard bắt buộc | Acceptance kind |
|---|---|---|---|---|
| REQ-V3-BC-254 | BC-254/R25/M08 | Phân tuyến theo đích và quy tắc vận hành | Đúng scope Hub, route plan hợp lệ; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-255 | BC-255/R25/M08 | Gom kiện vào bao/túi/đơn vị xử lý | Parent-child manifest và số lượng kiểm soát; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-256 | BC-256/R25/M08 | Xác nhận kiện đã qua dây chuyền phân loại | Không giả lập scan; có evidence; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-257 | BC-257/R25/M08 | Ghi nhận kiện sai tuyến | Exception, redirect plan, không xóa lịch sử; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-259 | BC-259/R25/M08 | Ghi nhận kiện hư hỏng khi phân loại | Ảnh/biên bản, custody, escalation; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-260 | BC-260/R25/M09 | Chuyển kiện/bao vào tuyến vận tải phù hợp | Manifest/tuyến/đích khớp, không tự tạo trip quyền driver; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-261 | BC-261/R25/M18 | Hạn chế PII và dữ liệu tài chính Merchant | Least privilege, test deny access; permission code DECISION_REQUIRED (no runtime allowlist expansion) | NEGATIVE/DENY |
| REQ-V3-BC-262 | BC-262/R26/M08 | Giám sát năng lực xử lý của Hub | Chỉ hub thuộc quyền; capacity measurable; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-263 | BC-263/R26/M08 | Theo dõi tồn kiện theo vị trí/thời gian | Event custody và inventory reconcile; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-265 | BC-265/R26/M08 | Đối chiếu manifest và xử lý chênh lệch | Không sửa âm thầm, chứng cứ/approval; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-269 | BC-269/R26/M18 | Phê duyệt điều chỉnh tồn theo thẩm quyền | Reason/evidence/separation of duties, không xóa custody; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |

## Workflow và acceptance

Scan→sort/bag→đối chiếu; trùng/sai tuyến/hỏng giữ evidence/escalation.

Với từng requirement: Given actor/resource đúng scope và preconditions, When thực hiện operation nguồn, Then chỉ outcome đúng guard được commit với provenance. Với NEGATIVE/DENY: When gọi trực tiếp API, Then từ chối và không ghi effect. Given expired/revoked/wrong resource/state, When submit/replay/recovery, Then không leak data/receipt hay mutation. Given mất response sau commit, When recovery đúng contract, Then không lặp effect. Required test IDs tại CSV là planned, chưa có runtime receipt.

## Dependencies và readiness

Hard: L011. Soft/consumer: none; soft không cho phép fake missing facts. [Roadmap](../../../../docs/implementation/roadmap-v3.md) phân biệt minimum contract slice và full acceptance.

Blockers: D-V3-05; DEC-14. Owner: Project Owner + Tech Lead; Security cho IAM/privacy; Finance cho money; Operations cho custody. Owner duyệt mapping/decisions, API/event/state/data owner và fixtures; không BLOCKING trong slice; positive/deny/concurrency/recovery đã thiết kế; canonical docs/codex/definition-of-ready.md.

## Delivery gate

BE+HTTP+DB+FE thật, PostgreSQL invariants/guards và browser/native evidence phù hợp, source+PR CI pass; BC/test/evidence cập nhật; Owner merge/acceptance riêng. [Design](design.md), [tasks](tasks.md), [shared convention](../../../../docs/reconciliation/v3/delivery-plan.md). Không promote IMPLEMENTED/VERIFIED do có file.
