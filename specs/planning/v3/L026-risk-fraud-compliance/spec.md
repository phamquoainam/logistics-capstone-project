# L026 — risk-fraud-compliance

PROPOSED / BLOCKED_FOR_IMPLEMENTATION. Primary accountable domain: orders; wave F4; actors R30.

## Outcome và scope

Risk case/hold/release đúng quyền. Không cấp quyền từ role label; không ghi dữ liệu owner khác;5 DEFER chỉ denial. CORE wave sau vẫn trong target.

## Requirements theo source

[371 BC crosswalk](../../../../docs/reconciliation/v3/capability-crosswalk.csv) giữ từng guard/owner/status. [Legacy crosswalk](../../../../docs/reconciliation/v3/legacy-crosswalk.csv) bảo toàn FR/REQ/AC/task, không tự coi mapping là equivalence.

| Requirement | Source | Hành vi | Guard bắt buộc | Acceptance kind |
|---|---|---|---|---|
| REQ-V3-BC-337 | BC-337/R30/M12 | Điều tra dấu hiệu gian lận COD | Read theo case và dấu vết tiền; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-338 | BC-338/R30/M01 | Phát hiện và điều tra tài khoản sử dụng trái phép | Không tự cấp quyền mới; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-339 | BC-339/R30/M05 | Phát hiện spam hoặc tạo Shipment hàng loạt bất thường | Rate, fingerprint, scope, false positives; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-340 | BC-340/R30/M05 | Kiểm tra thông tin Recipient có dấu hiệu giả | PII minimization, authorization; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-341 | BC-341/R30/M08 | Đánh dấu kiện có dấu hiệu rủi ro | Case, evidence và custody không bị sửa; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-342 | BC-342/R30/M18 | Điều tra truy cập dữ liệu trái phép | Audit bất biến, nguyên tắc tối thiểu; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-343 | BC-343/R30/M05 | Đề xuất tạm giữ Shipment theo chính sách | Không thay đổi silent status; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-344 | BC-344/R30/M15 | Chuyển hồ sơ rủi ro sang thẩm định/phê duyệt | Separation of duties; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-345 | BC-345/R30/M05 | Phê duyệt giải phóng Shipment theo thẩm quyền | Dual control/hạn mức khi cần; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-346 | BC-346/R30/M18 | Xem audit và giao dịch bất thường theo case | Mọi lần truy cập nhạy cảm có log; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-347 | BC-347/R30/M13 | Điều tra giao dịch tài chính bất thường | Không tự sửa ledger; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |

## Workflow và acceptance

Flag→investigate→propose→approve/release; replay/self-approval deny.

Với từng requirement: Given actor/resource đúng scope và preconditions, When thực hiện operation nguồn, Then chỉ outcome đúng guard được commit với provenance. Với NEGATIVE/DENY: When gọi trực tiếp API, Then từ chối và không ghi effect. Given expired/revoked/wrong resource/state, When submit/replay/recovery, Then không leak data/receipt hay mutation. Given mất response sau commit, When recovery đúng contract, Then không lặp effect. Required test IDs tại CSV là planned, chưa có runtime receipt.

## Dependencies và readiness

Hard: L006,L009. Soft/consumer: L017,L019; soft không cho phép fake missing facts. [Roadmap](../../../../docs/implementation/roadmap-v3.md) phân biệt minimum contract slice và full acceptance.

Blockers: D-V3-05; D-V3-07; D-V3-06. Owner: Project Owner + Tech Lead; Security cho IAM/privacy; Finance cho money; Operations cho custody. Owner duyệt mapping/decisions, API/event/state/data owner và fixtures; không BLOCKING trong slice; positive/deny/concurrency/recovery đã thiết kế; canonical docs/codex/definition-of-ready.md.

## Delivery gate

BE+HTTP+DB+FE thật, PostgreSQL invariants/guards và browser/native evidence phù hợp, source+PR CI pass; BC/test/evidence cập nhật; Owner merge/acceptance riêng. [Design](design.md), [tasks](tasks.md), [shared convention](../../../../docs/reconciliation/v3/delivery-plan.md). Không promote IMPLEMENTED/VERIFIED do có file.
