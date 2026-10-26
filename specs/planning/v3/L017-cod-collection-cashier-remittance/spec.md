# L017 — cod-collection-cashier-remittance

PROPOSED / BLOCKED_FOR_IMPLEMENTATION. Primary accountable domain: finance; wave F3; actors R02,R03,R04,R05,R09,R10,R11,R12,R13,R14,R16,R17,R27,R31.

## Outcome và scope

COD collection và Cashier remittance. Không cấp quyền từ role label; không ghi dữ liệu owner khác;5 DEFER chỉ denial. CORE wave sau vẫn trong target.

## Requirements theo source

[371 BC crosswalk](../../../../docs/reconciliation/v3/capability-crosswalk.csv) giữ từng guard/owner/status. [Legacy crosswalk](../../../../docs/reconciliation/v3/legacy-crosswalk.csv) bảo toàn FR/REQ/AC/task, không tự coi mapping là equivalence.

| Requirement | Source | Hành vi | Guard bắt buộc | Acceptance kind |
|---|---|---|---|---|
| REQ-V3-BC-014 | BC-014/R02/M12 | Xem COD thuộc Merchant | Chỉ đọc theo quyền; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-028 | BC-028/R03/M12 | Xem COD của đơn được giao phụ trách | Không sửa COD đã chốt; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-040 | BC-040/R04/M12 | Thực hiện điều chỉnh COD/settlement | Không mặc định được phép; cần workflow tài chính riêng; permission code DECISION_REQUIRED (no runtime allowlist expansion) | NEGATIVE/DENY |
| REQ-V3-BC-046 | BC-046/R05/M12 | Sửa/duyệt COD | Ngoài quyền Identity; permission code DECISION_REQUIRED (no runtime allowlist expansion) | NEGATIVE/DENY |
| REQ-V3-BC-075 | BC-075/R09/M12 | Ghi nhận khoản COD đã thu | Đúng số tiền đã chốt; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-076 | BC-076/R09/M12 | Yêu cầu nộp COD cho quỹ | Không tự xác nhận quỹ đã nhận; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-082 | BC-082/R10/M12 | Thanh toán COD đã xác nhận | Không thay đổi cod_amount; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-086 | BC-086/R11/M12 | Xem COD dự kiến/đã thu/đang giữ/đã trả | Merchant isolation; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-092 | BC-092/R12/M12 | Tiếp nhận tiền mặt COD Courier nộp | Hai phía, amount và timestamp; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-093 | BC-093/R12/M12 | Xác nhận nộp và ghi thiếu/thừa | Không âm thầm sửa tiền; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-094 | BC-094/R12/M12 | Quản lý đối chiếu nộp COD theo ca | Không tự settle Merchant; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-095 | BC-095/R12/M18 | Ghi audit tiền nộp và điều chỉnh | Không xóa receipt; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-096 | BC-096/R12/M12 | Kiểm kê két quỹ/đóng ca nâng cao | Có đối chiếu kế toán; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-097 | BC-097/R13/M12 | Đối soát Courier thu và nộp COD | Phải khớp custody tiền và shipment; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-098 | BC-098/R13/M12 | Phát hiện COD thiếu/thừa/trùng | Ledger bất biến/idempotency; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-121 | BC-121/R16/M12 | Giám sát bàn giao COD tại Station | Không sửa sổ cái; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-127 | BC-127/R17/M12 | Đọc log đối soát COD | Không sửa tiền; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-161 | BC-161/R02/M12 | Xem COD dự kiến, đã thu, đã nộp, đã đối soát và công nợ | Chỉ đọc tài chính thuộc Merchant; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-187 | BC-187/R11/M12 | Xem COD dự kiến, đã thu, đang giữ và đã thanh toán | Không sửa dữ liệu thu COD của Courier; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-225 | BC-225/R09/M12 | Xác nhận số COD cần thu và phương thức thanh toán | Chỉ đọc giá trị COD đã chốt; không tự đổi tiền; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-233 | BC-233/R16/M12 | Giám sát Courier nộp COD tại Station | Cashier xác nhận tiền; manager không sửa ledger; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-280 | BC-280/R27/M12 | Không mặc nhiên được đổi COD, thanh toán hoặc cấu hình bảo mật | Negative permission tests, separation of duties; permission code DECISION_REQUIRED (no runtime allowlist expansion) | NEGATIVE/DENY |
| REQ-V3-BC-281 | BC-281/R12/M12 | Kiểm đếm COD thực nhận theo ca và từng Courier | Xác nhận song phương, denomination/count và số tiền đã chốt; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-282 | BC-282/R12/M12 | Đóng ca và đối chiếu quỹ COD thực tế | Biên bản đóng ca, thiếu/thừa và người chịu trách nhiệm; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-283 | BC-283/R12/M13 | Chuyển dữ liệu nộp tiền đã xác nhận sang đối soát | Không tự ghi RECONCILED hay SETTLED; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-284 | BC-284/R12/M08 | Ràng buộc điểm thu quỹ với Station/ca | Chỉ Station được giao, actor và thời gian có chứng cứ; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-285 | BC-285/R13/M12 | Đối chiếu Delivered Shipment với COD thu thực tế | Không suy luận COD từ DELIVERED; phát hiện ngoại lệ; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-295 | BC-295/R14/M12 | Tiếp nhận tra soát COD và chuyển Finance | Không tự sửa cod_amount; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-317 | BC-317/R17/M12 | Đọc chuỗi thu-nộp-đối soát COD | Không ghi remittance; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-354 | BC-354/R31/M12 | Không được giả xác nhận thu/đối soát/thanh toán COD | Negative permission tests; permission code DECISION_REQUIRED (no runtime allowlist expansion) | NEGATIVE/DENY |

## Workflow và acceptance

Collected→submitted→Cashier kiểm nhận; shortage giữ nghĩa vụ; courier không tự confirm.

Với từng requirement: Given actor/resource đúng scope và preconditions, When thực hiện operation nguồn, Then chỉ outcome đúng guard được commit với provenance. Với NEGATIVE/DENY: When gọi trực tiếp API, Then từ chối và không ghi effect. Given expired/revoked/wrong resource/state, When submit/replay/recovery, Then không leak data/receipt hay mutation. Given mất response sau commit, When recovery đúng contract, Then không lặp effect. Required test IDs tại CSV là planned, chưa có runtime receipt.

## Dependencies và readiness

Hard: L011,L018. Soft/consumer: L015; soft không cho phép fake missing facts. [Roadmap](../../../../docs/implementation/roadmap-v3.md) phân biệt minimum contract slice và full acceptance.

Blockers: DEC-19; D-V3-04. Owner: Project Owner + Tech Lead; Security cho IAM/privacy; Finance cho money; Operations cho custody. Owner duyệt mapping/decisions, API/event/state/data owner và fixtures; không BLOCKING trong slice; positive/deny/concurrency/recovery đã thiết kế; canonical docs/codex/definition-of-ready.md.

## Delivery gate

BE+HTTP+DB+FE thật, PostgreSQL invariants/guards và browser/native evidence phù hợp, source+PR CI pass; BC/test/evidence cập nhật; Owner merge/acceptance riêng. [Design](design.md), [tasks](tasks.md), [shared convention](../../../../docs/reconciliation/v3/delivery-plan.md). Không promote IMPLEMENTED/VERIFIED do có file.
