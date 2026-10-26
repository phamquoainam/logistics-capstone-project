# L013 — linehaul-trip-hub-transfer

PROPOSED / BLOCKED_FOR_IMPLEMENTATION. Primary accountable domain: dispatch; wave F2; actors R08,R16,R23,R26,R27.

## Outcome và scope

Line-haul với đối chiếu hai đầu. Không cấp quyền từ role label; không ghi dữ liệu owner khác;5 DEFER chỉ denial. CORE wave sau vẫn trong target.

## Requirements theo source

[371 BC crosswalk](../../../../docs/reconciliation/v3/capability-crosswalk.csv) giữ từng guard/owner/status. [Legacy crosswalk](../../../../docs/reconciliation/v3/legacy-crosswalk.csv) bảo toàn FR/REQ/AC/task, không tự coi mapping là equivalence.

| Requirement | Source | Hành vi | Guard bắt buộc | Acceptance kind |
|---|---|---|---|---|
| REQ-V3-BC-070 | BC-070/R08/M09 | Thực hiện phần nghiệp vụ tại Station trong bàn giao chuyến xe liên Hub | Station xác nhận nhận/xuất manifest, không vận hành lái xe thay Line-haul Driver; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-221 | BC-221/R08/M09 | Thực hiện scan bàn giao nhận/xuất kiện với chuyến xe liên Hub | Vai trò Station chỉ xác nhận custody đầu/cuối; Driver quản lý hành trình; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-231 | BC-231/R16/M09 | Giám sát các chuyến xe đi/đến và bàn giao kiện | Không làm Driver giả, giám sát manifest/custody; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-234 | BC-234/R23/M09 | Nhận chuyến xe được phân công | Chỉ trip hợp lệ theo tuyến/driver, thời hạn; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-235 | BC-235/R23/M09 | Kiểm tra seal, tải trọng và trạng thái phương tiện | Ghi checklist/evidence; bất thường phải xử lý trước departure; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-236 | BC-236/R23/M08 | Nhận manifest kiện, bao tải và thông tin số lượng | Manifest phiên bản, cargo/trip relation rõ ràng; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-237 | BC-237/R23/M06 | Quét barcode và đối chiếu kiện/bao khi nhận tải | Chặn kiện trùng/sai manifest; ghi discrepancy; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-238 | BC-238/R23/M08 | Xác nhận bàn giao kiện tại điểm xuất phát | Hai phía custody; timestamp, station/hub, actor; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-239 | BC-239/R23/M09 | Ghi nhận hành trình và các mốc vận tải | Trip events độc lập với shipment delivered; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-240 | BC-240/R23/M09 | Báo trễ, sự cố hoặc thay đổi hành trình | Lý do/evidence, escalation, SLA; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-241 | BC-241/R23/M09 | Xác nhận đến Hub/Station đích | Đúng trip/destination, thời gian và trạng thái; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-242 | BC-242/R23/M08 | Bàn giao và đối chiếu số kiện thực nhận ở đích | Đối chiếu manifest và bên nhận, tạo discrepancy khi lệch; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-264 | BC-264/R26/M09 | Theo dõi chuyến xe đến/đi Hub | Trip + manifest + ETA/delay consistency; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-267 | BC-267/R26/M09 | Phối hợp điều chuyển hàng và ngoại lệ vận tải | Theo policy/coverage/capacity, audit; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-270 | BC-270/R27/M08 | Quản lý mạng lưới Station và Hub theo vùng/toàn mạng | Đúng scope, không tự vượt quyền identity; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-272 | BC-272/R27/M09 | Giám sát năng lực tuyến và vận tải | Dữ liệu theo network region; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-273 | BC-273/R27/M09 | Điều chuyển hàng liên vùng | Phê duyệt theo policy, manifest/custody liên tục; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-276 | BC-276/R27/M09 | Điều chỉnh tuyến ngoại lệ theo chính sách | Reason/evidence/approval/audit; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |

## Workflow và acceptance

Nhận trip→scan/seal→departure→delay/arrival→đích kiểm đếm; không fake DELIVERED.

Với từng requirement: Given actor/resource đúng scope và preconditions, When thực hiện operation nguồn, Then chỉ outcome đúng guard được commit với provenance. Với NEGATIVE/DENY: When gọi trực tiếp API, Then từ chối và không ghi effect. Given expired/revoked/wrong resource/state, When submit/replay/recovery, Then không leak data/receipt hay mutation. Given mất response sau commit, When recovery đúng contract, Then không lặp effect. Required test IDs tại CSV là planned, chưa có runtime receipt.

## Dependencies và readiness

Hard: L011,L012. Soft/consumer: none; soft không cho phép fake missing facts. [Roadmap](../../../../docs/implementation/roadmap-v3.md) phân biệt minimum contract slice và full acceptance.

Blockers: D-V3-05; DEC-14. Owner: Project Owner + Tech Lead; Security cho IAM/privacy; Finance cho money; Operations cho custody. Owner duyệt mapping/decisions, API/event/state/data owner và fixtures; không BLOCKING trong slice; positive/deny/concurrency/recovery đã thiết kế; canonical docs/codex/definition-of-ready.md.

## Delivery gate

BE+HTTP+DB+FE thật, PostgreSQL invariants/guards và browser/native evidence phù hợp, source+PR CI pass; BC/test/evidence cập nhật; Owner merge/acceptance riêng. [Design](design.md), [tasks](tasks.md), [shared convention](../../../../docs/reconciliation/v3/delivery-plan.md). Không promote IMPLEMENTED/VERIFIED do có file.
