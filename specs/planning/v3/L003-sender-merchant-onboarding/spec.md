# L003 — sender-merchant-onboarding

PROPOSED / BLOCKED_FOR_IMPLEMENTATION. Primary accountable domain: merchant; wave F1; actors R01,R02,R03,R04,R05,R16,R18,R19,R29.

## Outcome và scope

Khách gửi cá nhân và Merchant tổ chức. Không cấp quyền từ role label; không ghi dữ liệu owner khác;5 DEFER chỉ denial. CORE wave sau vẫn trong target.

## Requirements theo source

[371 BC crosswalk](../../../../docs/reconciliation/v3/capability-crosswalk.csv) giữ từng guard/owner/status. [Legacy crosswalk](../../../../docs/reconciliation/v3/legacy-crosswalk.csv) bảo toàn FR/REQ/AC/task, không tự coi mapping là equivalence.

| Requirement | Source | Hành vi | Guard bắt buộc | Acceptance kind |
|---|---|---|---|---|
| REQ-V3-BC-007 | BC-007/R02/M01 | Dùng tài khoản với Merchant membership ACTIVE | Đúng org và trạng thái; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-008 | BC-008/R02/M02 | Quản lý hồ sơ và nhân viên Merchant | Đúng quyền quản trị tổ chức; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-009 | BC-009/R02/M02 | Quản lý pickup point | Tenant isolation; archive/sửa có điều kiện; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-020 | BC-020/R03/M01 | Truy cập qua membership Merchant | Không tự nâng quyền; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-021 | BC-021/R03/M02 | Xem và sửa pickup point được ủy quyền | Không quản lý nhân viên mặc định; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-032 | BC-032/R04/M02 | Duyệt/giám sát Merchant theo policy | Phạm vi org/region; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-045 | BC-045/R05/M02 | Sửa nội dung nghiệp vụ Merchant | Không tự có quyền Logistics; permission code DECISION_REQUIRED (no runtime allowlist expansion) | NEGATIVE/DENY |
| REQ-V3-BC-132 | BC-132/R01/M01 | Đi đến đăng ký tài khoản Retail Sender | Account tự đăng ký; xác minh theo Identity contract; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-133 | BC-133/R01/M01 | Đi đến onboarding Merchant | Tạo/đăng ký tổ chức theo workflow đã chốt; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-134 | BC-134/R18/M01 | Đăng ký/đăng nhập tài khoản cá nhân | Không buộc Merchant membership; xác minh/chống giả mạo; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-135 | BC-135/R18/M01 | Quản lý hồ sơ, tên, liên hệ, trạng thái tài khoản | Chỉ dữ liệu bản thân; thay đổi nhạy cảm cần xác minh; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-157 | BC-157/R02/M02 | Cập nhật thông tin pháp nhân, tên cửa hàng, mã số thuế và cấu hình Merchant | Chỉ Merchant sở hữu; kiểm soát thay đổi nhạy cảm; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-158 | BC-158/R02/M01 | Mời/thêm/khóa nhân viên, quản lý membership và quyền tổ chức | Không tự cấp quyền nền tảng; phân quyền trong giới hạn; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-159 | BC-159/R02/M02 | Quản lý nhiều cửa hàng/kho, cấu hình dịch vụ và quy tắc mặc định | Merchant scope; chính sách phiên bản; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-166 | BC-166/R19/M01 | Quản lý nhân sự và quyền hạn được Owner ủy quyền | Không vượt delegated scope; audit thay đổi; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-167 | BC-167/R19/M02 | Điều hành cửa hàng/kho và cấu hình vận hành được giao | Không đổi Merchant owner hoặc ngân hàng COD; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-230 | BC-230/R16/M01 | Quản lý ca làm và nhân sự Station trong quyền được ủy quyền | Không tự cấp global/system admin hoặc quyền ngoài Station; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-329 | BC-329/R29/M02 | Quản lý Merchant/account được giao | Không truy cập Merchant ngoài danh sách; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-330 | BC-330/R29/M02 | Tư vấn dịch vụ và ghi nhận tương tác khách hàng | Không đọc PII Shipment không cần thiết; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-331 | BC-331/R29/M02 | Theo dõi hợp đồng Merchant và gia hạn | Quyền xem theo scope, tài liệu versioned; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-335 | BC-335/R29/M17 | Phối hợp onboarding và integration Merchant | Không tự xem/cấp mọi API secrets; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |

## Workflow và acceptance

Guest chọn loại→xác minh→đúng profile/org; revoke giữa form và submit phải deny.

Với từng requirement: Given actor/resource đúng scope và preconditions, When thực hiện operation nguồn, Then chỉ outcome đúng guard được commit với provenance. Với NEGATIVE/DENY: When gọi trực tiếp API, Then từ chối và không ghi effect. Given expired/revoked/wrong resource/state, When submit/replay/recovery, Then không leak data/receipt hay mutation. Given mất response sau commit, When recovery đúng contract, Then không lặp effect. Required test IDs tại CSV là planned, chưa có runtime receipt.

## Dependencies và readiness

Hard: L002. Soft/consumer: L004; soft không cho phép fake missing facts. [Roadmap](../../../../docs/implementation/roadmap-v3.md) phân biệt minimum contract slice và full acceptance.

Blockers: D-V3-01; D-V3-02; D-V3-03; D-V3-05. Owner: Project Owner + Tech Lead; Security cho IAM/privacy; Finance cho money; Operations cho custody. Owner duyệt mapping/decisions, API/event/state/data owner và fixtures; không BLOCKING trong slice; positive/deny/concurrency/recovery đã thiết kế; canonical docs/codex/definition-of-ready.md.

## Delivery gate

BE+HTTP+DB+FE thật, PostgreSQL invariants/guards và browser/native evidence phù hợp, source+PR CI pass; BC/test/evidence cập nhật; Owner merge/acceptance riêng. [Design](design.md), [tasks](tasks.md), [shared convention](../../../../docs/reconciliation/v3/delivery-plan.md). Không promote IMPLEMENTED/VERIFIED do có file.
