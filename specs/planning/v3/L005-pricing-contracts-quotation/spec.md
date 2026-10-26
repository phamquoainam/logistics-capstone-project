# L005 — pricing-contracts-quotation

PROPOSED / BLOCKED_FOR_IMPLEMENTATION. Primary accountable domain: pricing; wave F1; actors R01,R02,R03,R04,R11,R13,R18,R28,R29.

## Outcome và scope

Báo giá theo khách gửi/hợp đồng/thời điểm. Không cấp quyền từ role label; không ghi dữ liệu owner khác;5 DEFER chỉ denial. CORE wave sau vẫn trong target.

## Requirements theo source

[371 BC crosswalk](../../../../docs/reconciliation/v3/capability-crosswalk.csv) giữ từng guard/owner/status. [Legacy crosswalk](../../../../docs/reconciliation/v3/legacy-crosswalk.csv) bảo toàn FR/REQ/AC/task, không tự coi mapping là equivalence.

| Requirement | Source | Hành vi | Guard bắt buộc | Acceptance kind |
|---|---|---|---|---|
| REQ-V3-BC-002 | BC-002/R01/M04 | Ước tính cước vận chuyển | Không lộ giá hợp đồng Merchant; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-019 | BC-019/R02/M04 | Xem hợp đồng và biểu phí áp dụng | Không tự sửa bảng giá; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-023 | BC-023/R03/M04 | Nhận báo giá và chọn dịch vụ | Snapshot giá khi chốt; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-039 | BC-039/R04/M04 | Quản trị chính sách dịch vụ/bảng giá được ủy quyền | Không tự động có quyền sửa COD; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-131 | BC-131/R01/M04 | Tra cứu danh mục dịch vụ, điều kiện gửi hàng | Catalog công khai, không lộ hợp đồng riêng; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-138 | BC-138/R18/M04 | Báo giá và chọn dịch vụ gửi lẻ | Không áp dụng giá hợp đồng Merchant trái quyền; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-189 | BC-189/R11/M04 | Xem phí gốc, phụ phí và điều chỉnh theo Shipment/hợp đồng | Không tự thay bảng giá; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-290 | BC-290/R13/M04 | Đối chiếu giá chốt, phí điều chỉnh sau cân đo | Snapshot + phiên bản rate card; audit sửa phí; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-304 | BC-304/R04/M04 | Quản lý dịch vụ và chính sách nghiệp vụ theo quyền | Giá đặc biệt cần approval riêng; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-319 | BC-319/R28/M04 | Cấu hình rate card theo vùng và tuyến | Version, effective date, kiểm soát hồi tố; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-320 | BC-320/R28/M04 | Quy tắc trọng lượng tính cước và đo kích thước | Khối lượng quy đổi/cân thực tế theo policy; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-321 | BC-321/R28/M04 | Quản trị loại dịch vụ vận chuyển | Service availability và vùng phục vụ; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-322 | BC-322/R28/M04 | Cấu hình phí COD và các phụ phí | Min/max/tiers rõ ràng, audit; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-323 | BC-323/R28/M04 | Áp dụng biểu phí riêng theo hợp đồng Merchant | Chỉ hợp đồng được phê duyệt và còn hiệu lực; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-324 | BC-324/R28/M04 | Quản lý chiết khấu và khuyến mãi | Điều kiện áp dụng, phê duyệt và hạn sử dụng; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-325 | BC-325/R28/M04 | Quản trị thời điểm hiệu lực bảng giá | Không tự hồi tố đơn đã chốt; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-326 | BC-326/R28/M04 | Phân biệt giá ước tính, giá chốt và phí điều chỉnh | Lưu price snapshot + adjustment trail; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-328 | BC-328/R28/M18 | Audit sửa bảng giá, phê duyệt và phiên bản | Không xóa price history; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-332 | BC-332/R29/M04 | Đề xuất bảng giá hoặc chiết khấu đặc biệt | Không tự phê duyệt hoặc áp dụng giá; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-336 | BC-336/R29/M18 | Audit đề xuất giá và thay đổi quan hệ hợp đồng | Approval trace; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |

## Workflow và acceptance

Draft→duyệt/publish→quote→expiry/requote; không lộ giá tenant khác.

Với từng requirement: Given actor/resource đúng scope và preconditions, When thực hiện operation nguồn, Then chỉ outcome đúng guard được commit với provenance. Với NEGATIVE/DENY: When gọi trực tiếp API, Then từ chối và không ghi effect. Given expired/revoked/wrong resource/state, When submit/replay/recovery, Then không leak data/receipt hay mutation. Given mất response sau commit, When recovery đúng contract, Then không lặp effect. Required test IDs tại CSV là planned, chưa có runtime receipt.

## Dependencies và readiness

Hard: L004. Soft/consumer: none; soft không cho phép fake missing facts. [Roadmap](../../../../docs/implementation/roadmap-v3.md) phân biệt minimum contract slice và full acceptance.

Blockers: DEC-07; DEC-08; D-V3-04. Owner: Project Owner + Tech Lead; Security cho IAM/privacy; Finance cho money; Operations cho custody. Owner duyệt mapping/decisions, API/event/state/data owner và fixtures; không BLOCKING trong slice; positive/deny/concurrency/recovery đã thiết kế; canonical docs/codex/definition-of-ready.md.

## Delivery gate

BE+HTTP+DB+FE thật, PostgreSQL invariants/guards và browser/native evidence phù hợp, source+PR CI pass; BC/test/evidence cập nhật; Owner merge/acceptance riêng. [Design](design.md), [tasks](tasks.md), [shared convention](../../../../docs/reconciliation/v3/delivery-plan.md). Không promote IMPLEMENTED/VERIFIED do có file.
