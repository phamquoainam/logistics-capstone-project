# L015 — lastmile-attempt-pod

PROPOSED / BLOCKED_FOR_IMPLEMENTATION. Primary accountable domain: orders; wave F2; actors R09,R10,R15,R31.

## Outcome và scope

Delivery attempt/POD và COD thực thu. Không cấp quyền từ role label; không ghi dữ liệu owner khác;5 DEFER chỉ denial. CORE wave sau vẫn trong target.

## Requirements theo source

[371 BC crosswalk](../../../../docs/reconciliation/v3/capability-crosswalk.csv) giữ từng guard/owner/status. [Legacy crosswalk](../../../../docs/reconciliation/v3/legacy-crosswalk.csv) bảo toàn FR/REQ/AC/task, không tự coi mapping là equivalence.

| Requirement | Source | Hành vi | Guard bắt buộc | Acceptance kind |
|---|---|---|---|---|
| REQ-V3-BC-073 | BC-073/R09/M10 | Ghi DELIVERED kèm POD | Bằng chứng hợp lệ; chống trùng; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-074 | BC-074/R09/M10 | Ghi DELIVERY_FAILED kèm lý do/attempt | Không xóa thất bại; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-081 | BC-081/R10/M10 | Nhận hoặc từ chối kiện; xác nhận POD/OTP khi có | Courier ghi nhận; không cấp role IAM; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-084 | BC-084/R10/M10 | Thông tin giao lại/chỉ dẫn qua Merchant hoặc Courier | Nhân sự được quyền xử lý theo state; Recipient không thao tác backend; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-227 | BC-227/R09/M10 | Ghi nhận bằng chứng và tăng số lần giao thất bại | Lý do, timestamp, evidence, attempt ID độc lập; không xóa history; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-299 | BC-299/R15/M10 | Đối chiếu delivery attempts và bằng chứng POD | Chứng cứ nguồn gốc; xử lý tranh chấp; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-355 | BC-355/R31/M10 | Không được giả POD/DELIVERED | Không bypass chứng cứ dù là Super Admin; permission code DECISION_REQUIRED (no runtime allowlist expansion) | NEGATIVE/DENY |

## Workflow và acceptance

OFD→POD hoặc failed reason; COD theo giá trị chốt; orphan upload có cleanup.

Với từng requirement: Given actor/resource đúng scope và preconditions, When thực hiện operation nguồn, Then chỉ outcome đúng guard được commit với provenance. Với NEGATIVE/DENY: When gọi trực tiếp API, Then từ chối và không ghi effect. Given expired/revoked/wrong resource/state, When submit/replay/recovery, Then không leak data/receipt hay mutation. Given mất response sau commit, When recovery đúng contract, Then không lặp effect. Required test IDs tại CSV là planned, chưa có runtime receipt.

## Dependencies và readiness

Hard: L009,L014,L017. Soft/consumer: none; soft không cho phép fake missing facts. [Roadmap](../../../../docs/implementation/roadmap-v3.md) phân biệt minimum contract slice và full acceptance.

Blockers: DEC-15; DEC-16; D-V3-04. Owner: Project Owner + Tech Lead; Security cho IAM/privacy; Finance cho money; Operations cho custody. Owner duyệt mapping/decisions, API/event/state/data owner và fixtures; không BLOCKING trong slice; positive/deny/concurrency/recovery đã thiết kế; canonical docs/codex/definition-of-ready.md.

## Delivery gate

BE+HTTP+DB+FE thật, PostgreSQL invariants/guards và browser/native evidence phù hợp, source+PR CI pass; BC/test/evidence cập nhật; Owner merge/acceptance riêng. [Design](design.md), [tasks](tasks.md), [shared convention](../../../../docs/reconciliation/v3/delivery-plan.md). Không promote IMPLEMENTED/VERIFIED do có file.
