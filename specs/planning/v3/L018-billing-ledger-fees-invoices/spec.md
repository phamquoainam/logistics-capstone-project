# L018 — billing-ledger-fees-invoices

PROPOSED / BLOCKED_FOR_IMPLEMENTATION. Primary accountable domain: finance; wave F3; actors R08,R11,R13,R15,R17,R18,R28.

## Outcome và scope

Ledger, billing, phí và chứng từ. Không cấp quyền từ role label; không ghi dữ liệu owner khác;5 DEFER chỉ denial. CORE wave sau vẫn trong target.

## Requirements theo source

[371 BC crosswalk](../../../../docs/reconciliation/v3/capability-crosswalk.csv) giữ từng guard/owner/status. [Legacy crosswalk](../../../../docs/reconciliation/v3/legacy-crosswalk.csv) bảo toàn FR/REQ/AC/task, không tự coi mapping là equivalence.

| Requirement | Source | Hành vi | Guard bắt buộc | Acceptance kind |
|---|---|---|---|---|
| REQ-V3-BC-088 | BC-088/R11/M13 | Tra cứu phí và phụ phí theo Shipment | Tôn trọng snapshot giá; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-101 | BC-101/R13/M13 | Tính phí và công nợ phải thu/phải trả | Snapshot giá + audit; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-102 | BC-102/R13/M18 | Audit điều chỉnh tài chính | Không sửa lịch sử; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-103 | BC-103/R13/M13 | Refund và xử lý khiếu nại tài chính | Phải được phê duyệt; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-104 | BC-104/R13/M13 | Tích hợp cổng thanh toán/ngân hàng | Không yêu cầu thanh toán thật trong demo; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-116 | BC-116/R15/M13 | Đề xuất bồi thường/hoàn tiền | Không tự thực hiện settlement; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-142 | BC-142/R18/M13 | Thanh toán phí vận chuyển theo phương thức hỗ trợ | Receipt, price snapshot và chống thu trùng; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-148 | BC-148/R18/M13 | Xem/tải hóa đơn và biên nhận | Hợp lệ với hình thức thanh toán, không rò dữ liệu; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-151 | BC-151/R08/M13 | Ghi nhận phí/thanh toán ký gửi tại quầy theo policy | Không tự xác nhận thanh toán thiếu bằng chứng; sổ thu/receipt; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-286 | BC-286/R13/M13 | Quản lý công nợ phải thu và phải trả | Ledger entries append-only; tính toán có căn cứ; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-288 | BC-288/R13/M13 | Thực hiện hoàn tiền đã được phê duyệt | Idempotency, kiểm soát hai người khi cần; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-289 | BC-289/R13/M13 | Phát hiện và từ chối thanh toán hoặc refund trùng | Idempotency key, unique reference và reconciliation; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-303 | BC-303/R15/M13 | Theo dõi thanh toán bồi thường/refund đã phê duyệt | Không giả lập payment confirmation; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-316 | BC-316/R17/M13 | Đọc ledger và lịch sử điều chỉnh tài chính | PII redaction và scope; read-only; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-327 | BC-327/R28/M13 | Cung cấp dữ liệu cước cho hóa đơn và đối soát | Không trực tiếp sửa ledger/số dư; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |

## Workflow và acceptance

Verified charge→balanced posting; duplicate callback/refund không thêm tiền; correction bằng entry mới.

Với từng requirement: Given actor/resource đúng scope và preconditions, When thực hiện operation nguồn, Then chỉ outcome đúng guard được commit với provenance. Với NEGATIVE/DENY: When gọi trực tiếp API, Then từ chối và không ghi effect. Given expired/revoked/wrong resource/state, When submit/replay/recovery, Then không leak data/receipt hay mutation. Given mất response sau commit, When recovery đúng contract, Then không lặp effect. Required test IDs tại CSV là planned, chưa có runtime receipt.

## Dependencies và readiness

Hard: L005,L006. Soft/consumer: L007,L017; soft không cho phép fake missing facts. [Roadmap](../../../../docs/implementation/roadmap-v3.md) phân biệt minimum contract slice và full acceptance.

Blockers: DEC-11; DEC-12; D-V3-04. Owner: Project Owner + Tech Lead; Security cho IAM/privacy; Finance cho money; Operations cho custody. Owner duyệt mapping/decisions, API/event/state/data owner và fixtures; không BLOCKING trong slice; positive/deny/concurrency/recovery đã thiết kế; canonical docs/codex/definition-of-ready.md.

## Delivery gate

BE+HTTP+DB+FE thật, PostgreSQL invariants/guards và browser/native evidence phù hợp, source+PR CI pass; BC/test/evidence cập nhật; Owner merge/acceptance riêng. [Design](design.md), [tasks](tasks.md), [shared convention](../../../../docs/reconciliation/v3/delivery-plan.md). Không promote IMPLEMENTED/VERIFIED do có file.
