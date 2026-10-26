# L025 — network-ops-routing-optimization

PROPOSED / BLOCKED_FOR_IMPLEMENTATION. Primary accountable domain: routing; wave F4; actors R06,R07,R09,R24,R26,R27.

## Outcome và scope

Network capacity và routing có phiên bản. Không cấp quyền từ role label; không ghi dữ liệu owner khác;5 DEFER chỉ denial. CORE wave sau vẫn trong target.

## Requirements theo source

[371 BC crosswalk](../../../../docs/reconciliation/v3/capability-crosswalk.csv) giữ từng guard/owner/status. [Legacy crosswalk](../../../../docs/reconciliation/v3/legacy-crosswalk.csv) bảo toàn FR/REQ/AC/task, không tự coi mapping là equivalence.

| Requirement | Source | Hành vi | Guard bắt buộc | Acceptance kind |
|---|---|---|---|---|
| REQ-V3-BC-061 | BC-061/R07/M16 | Xem KPI cá nhân | Không có quyền giám sát toàn đội; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-209 | BC-209/R06/M16 | Giám sát tỷ lệ pickup/delivery thành công và tải công việc | KPI có định nghĩa, dữ liệu trong vùng được phân quyền; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-215 | BC-215/R07/M16 | Báo cáo số điểm đã ghé, số kiện lấy và nhiệm vụ thất bại | Chỉ hiệu suất cá nhân; dựa trên event và attempt; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-229 | BC-229/R09/M16 | Xem KPI số kiện giao, tỷ lệ thành công và COD còn phải nộp | Chỉ dữ liệu cá nhân; COD chưa nộp không được coi là remitted; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-246 | BC-246/R24/M16 | Theo dõi hiệu suất và năng suất đội giao nhận | Dữ liệu đúng team/scope và nguồn sự kiện; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-268 | BC-268/R26/M16 | Theo dõi KPI, throughput và thời gian tồn Hub | Chỉ số định nghĩa và provenance; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-275 | BC-275/R27/M16 | Theo dõi SLA, cảnh báo và năng suất mạng lưới | Định nghĩa thống nhất, phân quyền dữ liệu; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |

## Workflow và acceptance

Propose→verify constraints→approve; timeout/infeasible giữ plan cũ có nhãn.

Với từng requirement: Given actor/resource đúng scope và preconditions, When thực hiện operation nguồn, Then chỉ outcome đúng guard được commit với provenance. Với NEGATIVE/DENY: When gọi trực tiếp API, Then từ chối và không ghi effect. Given expired/revoked/wrong resource/state, When submit/replay/recovery, Then không leak data/receipt hay mutation. Given mất response sau commit, When recovery đúng contract, Then không lặp effect. Required test IDs tại CSV là planned, chưa có runtime receipt.

## Dependencies và readiness

Hard: L012,L013,L014. Soft/consumer: none; soft không cho phép fake missing facts. [Roadmap](../../../../docs/implementation/roadmap-v3.md) phân biệt minimum contract slice và full acceptance.

Blockers: DEC-06; DEC-17; DEC-18. Owner: Project Owner + Tech Lead; Security cho IAM/privacy; Finance cho money; Operations cho custody. Owner duyệt mapping/decisions, API/event/state/data owner và fixtures; không BLOCKING trong slice; positive/deny/concurrency/recovery đã thiết kế; canonical docs/codex/definition-of-ready.md.

## Delivery gate

BE+HTTP+DB+FE thật, PostgreSQL invariants/guards và browser/native evidence phù hợp, source+PR CI pass; BC/test/evidence cập nhật; Owner merge/acceptance riêng. [Design](design.md), [tasks](tasks.md), [shared convention](../../../../docs/reconciliation/v3/delivery-plan.md). Không promote IMPLEMENTED/VERIFIED do có file.
