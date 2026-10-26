# L011 — station-intake-walkin-custody

PROPOSED / BLOCKED_FOR_IMPLEMENTATION. Primary accountable domain: dispatch; wave F2; actors R06,R07,R08,R09,R16,R24.

## Outcome và scope

Station intake và staffed walk-in. Không cấp quyền từ role label; không ghi dữ liệu owner khác;5 DEFER chỉ denial. CORE wave sau vẫn trong target.

## Requirements theo source

[371 BC crosswalk](../../../../docs/reconciliation/v3/capability-crosswalk.csv) giữ từng guard/owner/status. [Legacy crosswalk](../../../../docs/reconciliation/v3/legacy-crosswalk.csv) bảo toàn FR/REQ/AC/task, không tự coi mapping là equivalence.

| Requirement | Source | Hành vi | Guard bắt buộc | Acceptance kind |
|---|---|---|---|---|
| REQ-V3-BC-055 | BC-055/R06/M08 | Điều phối qua nhiều Station | Policy phạm vi bưu cục; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-059 | BC-059/R07/M08 | Bàn giao kiện cho Station | Bên nhận xác nhận custody; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-063 | BC-063/R08/M08 | Nhận kiện và xác nhận custody tại Station | Scan, người nhận, timestamp; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-065 | BC-065/R08/M08 | Nhập/xuất kiện và lập manifest | Không ghi đè lịch sử; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-066 | BC-066/R08/M08 | Bàn giao kiện cho courier/tuyến | Hai phía xác nhận khi cần; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-069 | BC-069/R08/M08 | Kiểm kê tồn chi tiết, phân loại tự động | Có quy trình chênh lệch; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-118 | BC-118/R16/M08 | Giám sát tồn kiện, nhập/xuất, manifest | Đúng Station scope; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-123 | BC-123/R16/M08 | Duyệt sai lệch kho có bằng chứng | Có audit/approval; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-149 | BC-149/R08/M05 | Tiếp nhận tại quầy và lập Shipment cho Walk-in Sender | Chỉ nhân viên được phân công, xác minh người gửi; KHÔNG phải Guest API; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-152 | BC-152/R08/M08 | Nhập custody kiện walk-in tại Station/Hub phục vụ giao dịch | Đúng Station, nguồn walk-in, manifest/handover; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-216 | BC-216/R08/M08 | Nhận kiện trực tiếp từ khách walk-in hoặc Courier | Phân biệt walk-in/assigned handover; actor, thời điểm, số lượng, chứng cứ; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-217 | BC-217/R08/M08 | Xác nhận kiện thực nhận theo manifest và kiểm đếm | Lệch manifest mở exception, không mặc định khớp; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-219 | BC-219/R08/M08 | Ghi nhận tồn kho và vị trí vật lý của kiện | Station custody và location event bất biến; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-220 | BC-220/R08/M08 | Xử lý sai lệch, tồn lâu và kiện không xác định | Lý do, evidence và escalation; không âm thầm sửa kho; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-228 | BC-228/R09/M08 | Bàn giao kiện không giao được về Station/Hub | Custody giao hai phía, xác nhận thực nhận; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-232 | BC-232/R16/M08 | Xử lý kiện tồn lâu, sai tuyến, mất/hỏng qua workflow | Case, bằng chứng, phê duyệt khi cần; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-250 | BC-250/R24/M08 | Kiểm tra kiện tồn và bàn giao bất thường | Chỉ vị trí/đội được phân công, có đối chiếu custody; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-251 | BC-251/R24/M08 | Xác nhận chênh lệch bàn giao theo quyền | Evidence, audit, phê duyệt riêng với mất mát; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |

## Workflow và acceptance

Staff verify→kiểm kiện→quote/pay/receipt→intake; lệch manifest mở exception.

Với từng requirement: Given actor/resource đúng scope và preconditions, When thực hiện operation nguồn, Then chỉ outcome đúng guard được commit với provenance. Với NEGATIVE/DENY: When gọi trực tiếp API, Then từ chối và không ghi effect. Given expired/revoked/wrong resource/state, When submit/replay/recovery, Then không leak data/receipt hay mutation. Given mất response sau commit, When recovery đúng contract, Then không lặp effect. Required test IDs tại CSV là planned, chưa có runtime receipt.

## Dependencies và readiness

Hard: L007,L008,L009,L010,L018. Soft/consumer: none; soft không cho phép fake missing facts. [Roadmap](../../../../docs/implementation/roadmap-v3.md) phân biệt minimum contract slice và full acceptance.

Blockers: D-V3-05; D-V3-04; DEC-14. Owner: Project Owner + Tech Lead; Security cho IAM/privacy; Finance cho money; Operations cho custody. Owner duyệt mapping/decisions, API/event/state/data owner và fixtures; không BLOCKING trong slice; positive/deny/concurrency/recovery đã thiết kế; canonical docs/codex/definition-of-ready.md.

## Delivery gate

BE+HTTP+DB+FE thật, PostgreSQL invariants/guards và browser/native evidence phù hợp, source+PR CI pass; BC/test/evidence cập nhật; Owner merge/acceptance riêng. [Design](design.md), [tasks](tasks.md), [shared convention](../../../../docs/reconciliation/v3/delivery-plan.md). Không promote IMPLEMENTED/VERIFIED do có file.
