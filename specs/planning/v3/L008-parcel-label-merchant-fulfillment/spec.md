# L008 — parcel-label-merchant-fulfillment

PROPOSED / BLOCKED_FOR_IMPLEMENTATION. Primary accountable domain: warehouse; wave F1; actors R03,R07,R08,R18,R20,R25.

## Outcome và scope

Chuẩn bị SKU/kiện và nhãn có kiểm đếm. Không cấp quyền từ role label; không ghi dữ liệu owner khác;5 DEFER chỉ denial. CORE wave sau vẫn trong target.

## Requirements theo source

[371 BC crosswalk](../../../../docs/reconciliation/v3/capability-crosswalk.csv) giữ từng guard/owner/status. [Legacy crosswalk](../../../../docs/reconciliation/v3/legacy-crosswalk.csv) bảo toàn FR/REQ/AC/task, không tự coi mapping là equivalence.

| Requirement | Source | Hành vi | Guard bắt buộc | Acceptance kind |
|---|---|---|---|---|
| REQ-V3-BC-026 | BC-026/R03/M06 | Tạo nhãn và danh sách bàn giao | Barcode liên kết kiện; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-064 | BC-064/R08/M06 | Kiểm tra barcode/cân đo kiện | Ghi nhận sai lệch có chứng cứ; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-140 | BC-140/R18/M06 | Khai báo kiện và nhận mã vận đơn/nhãn/biên nhận gửi | Parcel khác Shipment, thông tin hàng hóa hợp lệ; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-150 | BC-150/R08/M06 | Kiểm kiện, cân đo, in nhãn và cấp biên nhận walk-in | Tạo Parcel, timestamp, bằng chứng nhận kiện; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-176 | BC-176/R03/M07 | Chuẩn bị kiện và lập danh sách bàn giao cho Pickup Courier | Quét/đối chiếu kiện; không tự tạo pickup evidence; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-179 | BC-179/R20/M05 | Nhận danh sách Shipment/đơn cần lấy hàng và chuẩn bị kiện | Chỉ đơn thuộc kho/nhân sự được phân công; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-180 | BC-180/R20/M06 | Lấy hàng, kiểm đếm và ghi số lượng thực tế | Order, Shipment, Parcel tách biệt; có sai lệch; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-181 | BC-181/R20/M06 | Đóng gói và cân đo kiện, ghi kích thước thực tế | Snapshot measurement; không tự xác nhận phí chốt; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-182 | BC-182/R20/M06 | Dán nhãn, quét barcode và đối chiếu Parcel | Không tạo barcode trùng; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-183 | BC-183/R20/M07 | Lập manifest/danh sách kiện bàn giao Pickup Courier | Bằng chứng số kiện, thời gian và người bàn giao; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-184 | BC-184/R20/M06 | Ghi nhận thiếu/sai hàng hoặc bao bì bất thường | Evidence, lý do và escalation; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-185 | BC-185/R20/M07 | Xác nhận Courier tới lấy và phối hợp bàn giao | Không tự ghi PICKED_UP thay Courier; hai phía đối chiếu; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-212 | BC-212/R07/M06 | Quét barcode và đối chiếu mã, số lượng kiện | Đúng parcel/manifest; phát hiện thiếu, trùng, lệch; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-213 | BC-213/R07/M06 | Kiểm tra bao bì, kích thước và thông tin kiện lúc pickup | Ghi sai lệch và evidence; không tự chốt lại phí; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-218 | BC-218/R08/M06 | Kiểm tra hàng gửi và điều kiện chấp nhận | Theo chính sách hàng cấm/hạn chế; không truy cập dữ liệu không cần thiết; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-253 | BC-253/R25/M06 | Quét barcode kiện trong công đoạn phân loại | Scan event truy vết người/máy/thời điểm; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-258 | BC-258/R25/M06 | Ghi nhận barcode không đọc được | Case xử lý định danh thay thế có kiểm soát; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |

## Workflow và acceptance

Reserve→pick/pack/count→label→handover; không giả Courier pickup.

Với từng requirement: Given actor/resource đúng scope và preconditions, When thực hiện operation nguồn, Then chỉ outcome đúng guard được commit với provenance. Với NEGATIVE/DENY: When gọi trực tiếp API, Then từ chối và không ghi effect. Given expired/revoked/wrong resource/state, When submit/replay/recovery, Then không leak data/receipt hay mutation. Given mất response sau commit, When recovery đúng contract, Then không lặp effect. Required test IDs tại CSV là planned, chưa có runtime receipt.

## Dependencies và readiness

Hard: L006,L007. Soft/consumer: none; soft không cho phép fake missing facts. [Roadmap](../../../../docs/implementation/roadmap-v3.md) phân biệt minimum contract slice và full acceptance.

Blockers: D-V3-05; DEC-13. Owner: Project Owner + Tech Lead; Security cho IAM/privacy; Finance cho money; Operations cho custody. Owner duyệt mapping/decisions, API/event/state/data owner và fixtures; không BLOCKING trong slice; positive/deny/concurrency/recovery đã thiết kế; canonical docs/codex/definition-of-ready.md.

## Delivery gate

BE+HTTP+DB+FE thật, PostgreSQL invariants/guards và browser/native evidence phù hợp, source+PR CI pass; BC/test/evidence cập nhật; Owner merge/acceptance riêng. [Design](design.md), [tasks](tasks.md), [shared convention](../../../../docs/reconciliation/v3/delivery-plan.md). Không promote IMPLEMENTED/VERIFIED do có file.
