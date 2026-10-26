# L019 — cod-reconciliation-merchant-settlement

PROPOSED / BLOCKED_FOR_IMPLEMENTATION. Primary accountable domain: finance; wave F3; actors R02,R11,R13.

## Outcome và scope

Reconciliation và settlement có điều kiện. Không cấp quyền từ role label; không ghi dữ liệu owner khác;5 DEFER chỉ denial. CORE wave sau vẫn trong target.

## Requirements theo source

[371 BC crosswalk](../../../../docs/reconciliation/v3/capability-crosswalk.csv) giữ từng guard/owner/status. [Legacy crosswalk](../../../../docs/reconciliation/v3/legacy-crosswalk.csv) bảo toàn FR/REQ/AC/task, không tự coi mapping là equivalence.

| Requirement | Source | Hành vi | Guard bắt buộc | Acceptance kind |
|---|---|---|---|---|
| REQ-V3-BC-015 | BC-015/R02/M13 | Xem phí, bảng kê và settlement | Chỉ thuộc Merchant; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-087 | BC-087/R11/M13 | Xem bảng kê đối soát và thanh toán | Không sửa sổ cái; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-091 | BC-091/R11/M13 | Đối chiếu/ghi nhận xác nhận bảng kê | Không tự quyết toán phía Logistics; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-099 | BC-099/R13/M13 | Tạo bảng kê và kỳ thanh toán Merchant | Có quyền phê duyệt phù hợp; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-100 | BC-100/R13/M13 | Ghi nhận settlement đã thực hiện | Không giả định đã chuyển tiền khi chưa có bằng chứng; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-162 | BC-162/R02/M13 | Xem lịch thanh toán, hóa đơn và chứng từ COD/cước | Chỉ Merchant được sở hữu; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-188 | BC-188/R11/M13 | Đối soát Merchant với bảng kê hãng và công nợ phải thu/phải trả | Ledger/statement không được sửa âm thầm; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-190 | BC-190/R11/M13 | Kiểm tra chuyển COD thực nhận, lịch thanh toán và tải chứng từ | Không tự xác nhận hãng đã trả; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-287 | BC-287/R13/M13 | Xác định COD đủ điều kiện trả Merchant | Theo hold, fees, disputes, policy, approvals; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |

## Workflow và acceptance

Delivered+collected+confirmed remitted→reconcile→simulated payout; late data kỳ sau.

Với từng requirement: Given actor/resource đúng scope và preconditions, When thực hiện operation nguồn, Then chỉ outcome đúng guard được commit với provenance. Với NEGATIVE/DENY: When gọi trực tiếp API, Then từ chối và không ghi effect. Given expired/revoked/wrong resource/state, When submit/replay/recovery, Then không leak data/receipt hay mutation. Given mất response sau commit, When recovery đúng contract, Then không lặp effect. Required test IDs tại CSV là planned, chưa có runtime receipt.

## Dependencies và readiness

Hard: L017,L018. Soft/consumer: none; soft không cho phép fake missing facts. [Roadmap](../../../../docs/implementation/roadmap-v3.md) phân biệt minimum contract slice và full acceptance.

Blockers: DEC-19; D-V3-04. Owner: Project Owner + Tech Lead; Security cho IAM/privacy; Finance cho money; Operations cho custody. Owner duyệt mapping/decisions, API/event/state/data owner và fixtures; không BLOCKING trong slice; positive/deny/concurrency/recovery đã thiết kế; canonical docs/codex/definition-of-ready.md.

## Delivery gate

BE+HTTP+DB+FE thật, PostgreSQL invariants/guards và browser/native evidence phù hợp, source+PR CI pass; BC/test/evidence cập nhật; Owner merge/acceptance riêng. [Design](design.md), [tasks](tasks.md), [shared convention](../../../../docs/reconciliation/v3/delivery-plan.md). Không promote IMPLEMENTED/VERIFIED do có file.
