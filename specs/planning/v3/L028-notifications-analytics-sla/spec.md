# L028 — notifications-analytics-sla

PROPOSED / BLOCKED_FOR_IMPLEMENTATION. Primary accountable domain: notification; wave F4; actors R02,R04,R06,R11,R13,R16,R17,R19,R29,R32.

## Outcome và scope

Notifications/KPI/SLA từ committed facts. Không cấp quyền từ role label; không ghi dữ liệu owner khác;5 DEFER chỉ denial. CORE wave sau vẫn trong target.

## Requirements theo source

[371 BC crosswalk](../../../../docs/reconciliation/v3/capability-crosswalk.csv) giữ từng guard/owner/status. [Legacy crosswalk](../../../../docs/reconciliation/v3/legacy-crosswalk.csv) bảo toàn FR/REQ/AC/task, không tự coi mapping là equivalence.

| Requirement | Source | Hành vi | Guard bắt buộc | Acceptance kind |
|---|---|---|---|---|
| REQ-V3-BC-017 | BC-017/R02/M16 | Xem KPI, báo cáo chi phí, tỷ lệ giao | Chỉ dữ liệu Merchant; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-035 | BC-035/R04/M16 | Theo dõi SLA và báo cáo vận hành | Chỉ phạm vi được phép; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-053 | BC-053/R06/M16 | Theo dõi SLA, cảnh báo quá hạn | Không tự sửa tracking; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-090 | BC-090/R11/M16 | Xuất báo cáo COD, phí | Đúng phạm vi Merchant; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-122 | BC-122/R16/M16 | Theo dõi KPI/SLA và năng lực ca | Đúng trạm; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-130 | BC-130/R17/M16 | Xuất báo cáo audit | Bảo vệ PII; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-165 | BC-165/R02/M16 | Xem báo cáo COD, giao thành công, đơn hoàn, chi phí | Dữ liệu Merchant; không lộ tenant khác; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-172 | BC-172/R19/M16 | Theo dõi hiệu suất giao hàng theo Merchant/đơn vị được giao | Không xem KPI Merchant khác; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-192 | BC-192/R11/M16 | Xuất báo cáo tài chính theo thời gian, kho và đơn vị kinh doanh | Giới hạn Merchant/branch scope; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-291 | BC-291/R13/M16 | Xuất báo cáo công nợ và COD tồn chưa đối soát | Theo kỳ, có nguồn bút toán; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-333 | BC-333/R29/M16 | Theo dõi KPI và chỉ tiêu kinh doanh | Dữ liệu theo account/region scope; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-356 | BC-356/R32/M16 | Kiểm tra sức khỏe hệ thống và tín hiệu vận hành | Chỉ telemetry cần thiết; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-359 | BC-359/R32/M16 | Giám sát hàng đợi, dead-letter và backlog | Có hạn mức retry và cảnh báo; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-363 | BC-363/R32/M11 | Khắc phục lỗi phát thông báo/tracking event | Không giả mạo sự kiện giao hàng; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |

## Workflow và acceptance

Commit→dedup notification→read; late/missing event đối chiếu; không success inflation.

Với từng requirement: Given actor/resource đúng scope và preconditions, When thực hiện operation nguồn, Then chỉ outcome đúng guard được commit với provenance. Với NEGATIVE/DENY: When gọi trực tiếp API, Then từ chối và không ghi effect. Given expired/revoked/wrong resource/state, When submit/replay/recovery, Then không leak data/receipt hay mutation. Given mất response sau commit, When recovery đúng contract, Then không lặp effect. Required test IDs tại CSV là planned, chưa có runtime receipt.

## Dependencies và readiness

Hard: L009,L019,L020. Soft/consumer: L025; soft không cho phép fake missing facts. [Roadmap](../../../../docs/implementation/roadmap-v3.md) phân biệt minimum contract slice và full acceptance.

Blockers: DEC-20; D-V3-06. Owner: Project Owner + Tech Lead; Security cho IAM/privacy; Finance cho money; Operations cho custody. Owner duyệt mapping/decisions, API/event/state/data owner và fixtures; không BLOCKING trong slice; positive/deny/concurrency/recovery đã thiết kế; canonical docs/codex/definition-of-ready.md.

## Delivery gate

BE+HTTP+DB+FE thật, PostgreSQL invariants/guards và browser/native evidence phù hợp, source+PR CI pass; BC/test/evidence cập nhật; Owner merge/acceptance riêng. [Design](design.md), [tasks](tasks.md), [shared convention](../../../../docs/reconciliation/v3/delivery-plan.md). Không promote IMPLEMENTED/VERIFIED do có file.
