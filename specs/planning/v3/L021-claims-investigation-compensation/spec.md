# L021 — claims-investigation-compensation

PROPOSED / BLOCKED_FOR_IMPLEMENTATION. Primary accountable domain: orders; wave F3; actors R02,R15,R17.

## Outcome và scope

Claims và compensation tách ticket. Không cấp quyền từ role label; không ghi dữ liệu owner khác;5 DEFER chỉ denial. CORE wave sau vẫn trong target.

## Requirements theo source

[371 BC crosswalk](../../../../docs/reconciliation/v3/capability-crosswalk.csv) giữ từng guard/owner/status. [Legacy crosswalk](../../../../docs/reconciliation/v3/legacy-crosswalk.csv) bảo toàn FR/REQ/AC/task, không tự coi mapping là equivalence.

| Requirement | Source | Hành vi | Guard bắt buộc | Acceptance kind |
|---|---|---|---|---|
| REQ-V3-BC-112 | BC-112/R15/M15 | Mở claim case gắn ticket/Shipment | Không trùng vụ việc; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-113 | BC-113/R15/M15 | Thu thập POD/tracking/ảnh/biên bản | Bằng chứng có nguồn gốc; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-114 | BC-114/R15/M15 | Xác định trách nhiệm và đề xuất phương án | Người đề xuất khác người duyệt nếu cần; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-115 | BC-115/R15/M15 | Ghi nhận outcome và thông báo CS/Merchant | Audit đầy đủ; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-117 | BC-117/R15/M18 | Truy vết lịch sử xử lý claim | Không xóa evidence; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-129 | BC-129/R17/M15 | Đọc bằng chứng claim và phê duyệt | Đúng phạm vi kiểm toán; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-163 | BC-163/R02/M15 | Theo dõi claim, yêu cầu bồi thường và đơn bất thường | Không tự phê duyệt bồi thường của hãng; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-297 | BC-297/R15/M15 | Điều tra mất/hỏng/thiếu/giao nhầm và tranh chấp POD | Đối chiếu evidence, tracking, custody và trách nhiệm; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-298 | BC-298/R15/M08 | Truy xuất custody và biên bản bàn giao phục vụ claim | Chỉ case scope; không sửa sự kiện; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-300 | BC-300/R15/M12 | Điều tra thu sai COD liên quan claim | Không tự tạo điều chỉnh tài chính; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-301 | BC-301/R15/M15 | Đánh giá chính sách, hạn mức và đề xuất số bồi thường | Separation of duties và reason; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-302 | BC-302/R15/M15 | Chuyển hồ sơ đề xuất sang người phê duyệt | Không tự phê duyệt chi trả khi không có quyền; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-318 | BC-318/R17/M15 | Đọc bằng chứng claim và phê duyệt | Không thay đổi outcome; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |

## Workflow và acceptance

Ticket→claim→evidence→đề xuất→người khác duyệt→Finance pay; chống self-approval/trùng.

Với từng requirement: Given actor/resource đúng scope và preconditions, When thực hiện operation nguồn, Then chỉ outcome đúng guard được commit với provenance. Với NEGATIVE/DENY: When gọi trực tiếp API, Then từ chối và không ghi effect. Given expired/revoked/wrong resource/state, When submit/replay/recovery, Then không leak data/receipt hay mutation. Given mất response sau commit, When recovery đúng contract, Then không lặp effect. Required test IDs tại CSV là planned, chưa có runtime receipt.

## Dependencies và readiness

Hard: L016,L018,L020. Soft/consumer: L019; soft không cho phép fake missing facts. [Roadmap](../../../../docs/implementation/roadmap-v3.md) phân biệt minimum contract slice và full acceptance.

Blockers: D-V3-05; D-V3-07; DEC-15. Owner: Project Owner + Tech Lead; Security cho IAM/privacy; Finance cho money; Operations cho custody. Owner duyệt mapping/decisions, API/event/state/data owner và fixtures; không BLOCKING trong slice; positive/deny/concurrency/recovery đã thiết kế; canonical docs/codex/definition-of-ready.md.

## Delivery gate

BE+HTTP+DB+FE thật, PostgreSQL invariants/guards và browser/native evidence phù hợp, source+PR CI pass; BC/test/evidence cập nhật; Owner merge/acceptance riêng. [Design](design.md), [tasks](tasks.md), [shared convention](../../../../docs/reconciliation/v3/delivery-plan.md). Không promote IMPLEMENTED/VERIFIED do có file.
