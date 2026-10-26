# L024 — partner-franchise-station-ops

PROPOSED / BLOCKED_FOR_IMPLEMENTATION. Primary accountable domain: dispatch; wave F4; actors R33.

## Outcome và scope

Partner chỉ vận hành điểm được giao. Không cấp quyền từ role label; không ghi dữ liệu owner khác;5 DEFER chỉ denial. CORE wave sau vẫn trong target.

## Requirements theo source

[371 BC crosswalk](../../../../docs/reconciliation/v3/capability-crosswalk.csv) giữ từng guard/owner/status. [Legacy crosswalk](../../../../docs/reconciliation/v3/legacy-crosswalk.csv) bảo toàn FR/REQ/AC/task, không tự coi mapping là equivalence.

| Requirement | Source | Hành vi | Guard bắt buộc | Acceptance kind |
|---|---|---|---|---|
| REQ-V3-BC-364 | BC-364/R33/M05 | Tiếp nhận/gửi Shipment tại quầy đối tác được chỉ định | Partner+Station scope, xác minh walk-in; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-365 | BC-365/R33/M08 | Bàn giao kiện cho hãng vận chuyển | Actor, count, manifest, timestamp, custody proof; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-366 | BC-366/R33/M08 | Xác nhận số lượng thực giao/nhận | Sai lệch phải ghi event và investigation; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-367 | BC-367/R33/M08 | Quản lý tồn kiện ở điểm đại lý | Không đọc tồn nơi khác; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-368 | BC-368/R33/M13 | Xem doanh thu dịch vụ thuộc điểm được giao | Chỉ báo cáo được phép; không sửa settlement; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-369 | BC-369/R33/M17 | Ràng buộc Partner Organization với Station | Explicit membership/scope; no network-wide access; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-370 | BC-370/R33/M06 | Quét/kiểm tra nhãn và tình trạng kiện | Không giả mạo scan của hãng; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-371 | BC-371/R33/M18 | Truy vết nghiệp vụ điểm đối tác | Không xóa handover history; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |

## Workflow và acceptance

Assigned staff→counter/scan/count/handover; foreign Station deny.

Với từng requirement: Given actor/resource đúng scope và preconditions, When thực hiện operation nguồn, Then chỉ outcome đúng guard được commit với provenance. Với NEGATIVE/DENY: When gọi trực tiếp API, Then từ chối và không ghi effect. Given expired/revoked/wrong resource/state, When submit/replay/recovery, Then không leak data/receipt hay mutation. Given mất response sau commit, When recovery đúng contract, Then không lặp effect. Required test IDs tại CSV là planned, chưa có runtime receipt.

## Dependencies và readiness

Hard: L003,L011. Soft/consumer: none; soft không cho phép fake missing facts. [Roadmap](../../../../docs/implementation/roadmap-v3.md) phân biệt minimum contract slice và full acceptance.

Blockers: D-V3-02; D-V3-05; D-V3-08. Owner: Project Owner + Tech Lead; Security cho IAM/privacy; Finance cho money; Operations cho custody. Owner duyệt mapping/decisions, API/event/state/data owner và fixtures; không BLOCKING trong slice; positive/deny/concurrency/recovery đã thiết kế; canonical docs/codex/definition-of-ready.md.

## Delivery gate

BE+HTTP+DB+FE thật, PostgreSQL invariants/guards và browser/native evidence phù hợp, source+PR CI pass; BC/test/evidence cập nhật; Owner merge/acceptance riêng. [Design](design.md), [tasks](tasks.md), [shared convention](../../../../docs/reconciliation/v3/delivery-plan.md). Không promote IMPLEMENTED/VERIFIED do có file.
