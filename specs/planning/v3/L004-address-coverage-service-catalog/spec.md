# L004 — address-coverage-service-catalog

PROPOSED / BLOCKED_FOR_IMPLEMENTATION. Primary accountable domain: merchant; wave F1; actors R01,R02,R03,R04,R18,R19,R27.

## Outcome và scope

Địa chỉ/điểm lấy và catalog public. Không cấp quyền từ role label; không ghi dữ liệu owner khác;5 DEFER chỉ denial. CORE wave sau vẫn trong target.

## Requirements theo source

[371 BC crosswalk](../../../../docs/reconciliation/v3/capability-crosswalk.csv) giữ từng guard/owner/status. [Legacy crosswalk](../../../../docs/reconciliation/v3/legacy-crosswalk.csv) bảo toàn FR/REQ/AC/task, không tự coi mapping là equivalence.

| Requirement | Source | Hành vi | Guard bắt buộc | Acceptance kind |
|---|---|---|---|---|
| REQ-V3-BC-001 | BC-001/R01/M03 | Tra cứu vùng phục vụ công khai | Chỉ dữ liệu công khai; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-005 | BC-005/R01/M03 | Tra cứu bưu cục/Hub có phục vụ, địa chỉ, giờ hoạt động | Dữ liệu công khai; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-010 | BC-010/R02/M03 | Quản lý địa chỉ lấy hàng | Catalog phiên bản và kiểm tra dữ liệu; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-022 | BC-022/R03/M03 | Chuẩn hóa địa chỉ nhận/gửi | Dữ liệu hợp lệ; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-033 | BC-033/R04/M03 | Quản lý vùng phục vụ theo policy | Có phiên bản và audit; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-136 | BC-136/R18/M03 | Lưu và quản lý địa chỉ gửi/nhận thường dùng | Chỉ sở hữu cá nhân; chuẩn hóa địa chỉ; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-137 | BC-137/R18/M03 | Kiểm tra vùng phục vụ theo tuyến gửi lẻ | Coverage snapshot/quy tắc hợp lệ; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-168 | BC-168/R19/M03 | Quản lý pickup point trong phạm vi ủy quyền | Merchant/branch scope; không truy cập tenant khác; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-271 | BC-271/R27/M03 | Quản lý vùng giao nhận và coverage | Versioning/policy, không hồi tố đơn đã chốt tùy ý; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-305 | BC-305/R04/M08 | Quản trị cơ cấu Station/Hub/khu vực theo phạm vi | Không sửa custody đã xảy ra; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |

## Workflow và acceptance

Validate→xác nhận tọa độ→lưu/reload; timeout/mơ hồ chưa confirmed.

Với từng requirement: Given actor/resource đúng scope và preconditions, When thực hiện operation nguồn, Then chỉ outcome đúng guard được commit với provenance. Với NEGATIVE/DENY: When gọi trực tiếp API, Then từ chối và không ghi effect. Given expired/revoked/wrong resource/state, When submit/replay/recovery, Then không leak data/receipt hay mutation. Given mất response sau commit, When recovery đúng contract, Then không lặp effect. Required test IDs tại CSV là planned, chưa có runtime receipt.

## Dependencies và readiness

Hard: L001. Soft/consumer: L003; soft không cho phép fake missing facts. [Roadmap](../../../../docs/implementation/roadmap-v3.md) phân biệt minimum contract slice và full acceptance.

Blockers: D-V3-03; DEC-05; DEC-06. Owner: Project Owner + Tech Lead; Security cho IAM/privacy; Finance cho money; Operations cho custody. Owner duyệt mapping/decisions, API/event/state/data owner và fixtures; không BLOCKING trong slice; positive/deny/concurrency/recovery đã thiết kế; canonical docs/codex/definition-of-ready.md.

## Delivery gate

BE+HTTP+DB+FE thật, PostgreSQL invariants/guards và browser/native evidence phù hợp, source+PR CI pass; BC/test/evidence cập nhật; Owner merge/acceptance riêng. [Design](design.md), [tasks](tasks.md), [shared convention](../../../../docs/reconciliation/v3/delivery-plan.md). Không promote IMPLEMENTED/VERIFIED do có file.
