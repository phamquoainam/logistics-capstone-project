# Tasks 021 — claims-investigation-compensation

NOT_STARTED; thứ tự là contract/fixture → DB → BE/HTTP → UI/channel → verification → full acceptance. Mỗi BC là gói end-to-end nội bộ, không thay phương pháp Spec-by-Spec. Không task nào DONE trong rebuild docs.

## REQ-V3-BC-112 / BC-112: Mở claim case gắn ticket/Shipment

- [ ] **T-BC-112-CONTRACT** — Chốt operation «Mở claim case gắn ticket/Shipment»: allowed actor R15, guard «Không trùng vụ việc; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource claim/evidence/proposal/approval, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-112-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-112-DB** — Review forward schema/index/constraint của orders cho claim/evidence/proposal/approval nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-112-BE** — Implement «Mở claim case gắn ticket/Shipment» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-112-UI** — Tích hợp Internal scoped channel — Mở claim case gắn ticket/Shipment; action «Mở claim case gắn ticket/Shipment», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-112-VERIFY** — Chạy testcase riêng TEST-V3-BC-112-FLOW và AC-BC-112-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-112 trong traceability.

## REQ-V3-BC-113 / BC-113: Thu thập POD/tracking/ảnh/biên bản

- [ ] **T-BC-113-CONTRACT** — Chốt operation «Thu thập POD/tracking/ảnh/biên bản»: allowed actor R15, guard «Bằng chứng có nguồn gốc; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource claim/evidence/proposal/approval, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-113-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-113-DB** — Review forward schema/index/constraint của orders cho claim/evidence/proposal/approval nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-113-BE** — Implement «Thu thập POD/tracking/ảnh/biên bản» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-113-UI** — Tích hợp Internal scoped channel — Thu thập POD/tracking/ảnh/biên bản; action «Thu thập POD/tracking/ảnh/biên bản», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-113-VERIFY** — Chạy testcase riêng TEST-V3-BC-113-FLOW và AC-BC-113-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-113 trong traceability.

## REQ-V3-BC-114 / BC-114: Xác định trách nhiệm và đề xuất phương án

- [ ] **T-BC-114-CONTRACT** — Chốt operation «Xác định trách nhiệm và đề xuất phương án»: allowed actor R15, guard «Người đề xuất khác người duyệt nếu cần; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource claim/evidence/proposal/approval, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-114-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-114-DB** — Review forward schema/index/constraint của orders cho claim/evidence/proposal/approval nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-114-BE** — Implement «Xác định trách nhiệm và đề xuất phương án» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-114-UI** — Tích hợp Internal scoped channel — Xác định trách nhiệm và đề xuất phương án; action «Xác định trách nhiệm và đề xuất phương án», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-114-VERIFY** — Chạy testcase riêng TEST-V3-BC-114-FLOW và AC-BC-114-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-114 trong traceability.

## REQ-V3-BC-115 / BC-115: Ghi nhận outcome và thông báo CS/Merchant

- [ ] **T-BC-115-CONTRACT** — Chốt operation «Ghi nhận outcome và thông báo CS/Merchant»: allowed actor R15, guard «Audit đầy đủ; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource claim/evidence/proposal/approval, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-115-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-115-DB** — Review forward schema/index/constraint của orders cho claim/evidence/proposal/approval nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-115-BE** — Implement «Ghi nhận outcome và thông báo CS/Merchant» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-115-UI** — Tích hợp Internal scoped channel — Ghi nhận outcome và thông báo CS/Merchant; action «Ghi nhận outcome và thông báo CS/Merchant», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-115-VERIFY** — Chạy testcase riêng TEST-V3-BC-115-FLOW và AC-BC-115-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-115 trong traceability.

## REQ-V3-BC-117 / BC-117: Truy vết lịch sử xử lý claim

- [ ] **T-BC-117-CONTRACT** — Chốt operation «Truy vết lịch sử xử lý claim»: allowed actor R15, guard «Không xóa evidence; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource claim/evidence/proposal/approval, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-117-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-117-DB** — Review forward schema/index/constraint của orders cho claim/evidence/proposal/approval nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-117-BE** — Implement «Truy vết lịch sử xử lý claim» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-117-UI** — Tích hợp Internal scoped channel — Truy vết lịch sử xử lý claim; action «Truy vết lịch sử xử lý claim», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-117-VERIFY** — Chạy testcase riêng TEST-V3-BC-117-FLOW và AC-BC-117-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-117 trong traceability.

## REQ-V3-BC-129 / BC-129: Đọc bằng chứng claim và phê duyệt

- [ ] **T-BC-129-CONTRACT** — Chốt operation «Đọc bằng chứng claim và phê duyệt»: allowed actor R17, guard «Đúng phạm vi kiểm toán; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource claim/evidence/proposal/approval, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-129-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-129-DB** — Review forward schema/index/constraint của orders cho claim/evidence/proposal/approval nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-129-BE** — Implement «Đọc bằng chứng claim và phê duyệt» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-129-UI** — Tích hợp Internal scoped channel — Đọc bằng chứng claim và phê duyệt; action «Đọc bằng chứng claim và phê duyệt», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-129-VERIFY** — Chạy testcase riêng TEST-V3-BC-129-FLOW và AC-BC-129-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-129 trong traceability.

## REQ-V3-BC-163 / BC-163: Theo dõi claim, yêu cầu bồi thường và đơn bất thường

- [ ] **T-BC-163-CONTRACT** — Chốt operation «Theo dõi claim, yêu cầu bồi thường và đơn bất thường»: allowed actor R02, guard «Không tự phê duyệt bồi thường của hãng; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource claim/evidence/proposal/approval, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-163-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-163-DB** — Review forward schema/index/constraint của orders cho claim/evidence/proposal/approval nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-163-BE** — Implement «Theo dõi claim, yêu cầu bồi thường và đơn bất thường» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-163-UI** — Tích hợp Merchant current context — Theo dõi claim, yêu cầu bồi thường và đơn bất thường; action «Theo dõi claim, yêu cầu bồi thường và đơn bất thường», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-163-VERIFY** — Chạy testcase riêng TEST-V3-BC-163-FLOW và AC-BC-163-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-163 trong traceability.

## REQ-V3-BC-297 / BC-297: Điều tra mất/hỏng/thiếu/giao nhầm và tranh chấp POD

- [ ] **T-BC-297-CONTRACT** — Chốt operation «Điều tra mất/hỏng/thiếu/giao nhầm và tranh chấp POD»: allowed actor R15, guard «Đối chiếu evidence, tracking, custody và trách nhiệm; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource claim/evidence/proposal/approval, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-297-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-297-DB** — Review forward schema/index/constraint của orders cho claim/evidence/proposal/approval nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-297-BE** — Implement «Điều tra mất/hỏng/thiếu/giao nhầm và tranh chấp POD» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-297-UI** — Tích hợp Internal scoped channel — Điều tra mất/hỏng/thiếu/giao nhầm và tranh chấp POD; action «Điều tra mất/hỏng/thiếu/giao nhầm và tranh chấp POD», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-297-VERIFY** — Chạy testcase riêng TEST-V3-BC-297-FLOW và AC-BC-297-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-297 trong traceability.

## REQ-V3-BC-298 / BC-298: Truy xuất custody và biên bản bàn giao phục vụ claim

- [ ] **T-BC-298-CONTRACT** — Chốt operation «Truy xuất custody và biên bản bàn giao phục vụ claim»: allowed actor R15, guard «Chỉ case scope; không sửa sự kiện; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource claim/evidence/proposal/approval, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-298-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-298-DB** — Review forward schema/index/constraint của orders cho claim/evidence/proposal/approval nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-298-BE** — Implement «Truy xuất custody và biên bản bàn giao phục vụ claim» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-298-UI** — Tích hợp Internal scoped channel — Truy xuất custody và biên bản bàn giao phục vụ claim; action «Truy xuất custody và biên bản bàn giao phục vụ claim», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-298-VERIFY** — Chạy testcase riêng TEST-V3-BC-298-FLOW và AC-BC-298-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-298 trong traceability.

## REQ-V3-BC-300 / BC-300: Điều tra thu sai COD liên quan claim

- [ ] **T-BC-300-CONTRACT** — Chốt operation «Điều tra thu sai COD liên quan claim»: allowed actor R15, guard «Không tự tạo điều chỉnh tài chính; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource claim/evidence/proposal/approval, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-300-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-300-DB** — Review forward schema/index/constraint của orders cho claim/evidence/proposal/approval nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-300-BE** — Implement «Điều tra thu sai COD liên quan claim» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-300-UI** — Tích hợp Internal scoped channel — Điều tra thu sai COD liên quan claim; action «Điều tra thu sai COD liên quan claim», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-300-VERIFY** — Chạy testcase riêng TEST-V3-BC-300-FLOW và AC-BC-300-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-300 trong traceability.

## REQ-V3-BC-301 / BC-301: Đánh giá chính sách, hạn mức và đề xuất số bồi thường

- [ ] **T-BC-301-CONTRACT** — Chốt operation «Đánh giá chính sách, hạn mức và đề xuất số bồi thường»: allowed actor R15, guard «Separation of duties và reason; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource claim/evidence/proposal/approval, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-301-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-301-DB** — Review forward schema/index/constraint của orders cho claim/evidence/proposal/approval nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-301-BE** — Implement «Đánh giá chính sách, hạn mức và đề xuất số bồi thường» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-301-UI** — Tích hợp Internal scoped channel — Đánh giá chính sách, hạn mức và đề xuất số bồi thường; action «Đánh giá chính sách, hạn mức và đề xuất số bồi thường», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-301-VERIFY** — Chạy testcase riêng TEST-V3-BC-301-FLOW và AC-BC-301-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-301 trong traceability.

## REQ-V3-BC-302 / BC-302: Chuyển hồ sơ đề xuất sang người phê duyệt

- [ ] **T-BC-302-CONTRACT** — Chốt operation «Chuyển hồ sơ đề xuất sang người phê duyệt»: allowed actor R15, guard «Không tự phê duyệt chi trả khi không có quyền; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource claim/evidence/proposal/approval, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-302-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-302-DB** — Review forward schema/index/constraint của orders cho claim/evidence/proposal/approval nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-302-BE** — Implement «Chuyển hồ sơ đề xuất sang người phê duyệt» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-302-UI** — Tích hợp Internal scoped channel — Chuyển hồ sơ đề xuất sang người phê duyệt; action «Chuyển hồ sơ đề xuất sang người phê duyệt», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-302-VERIFY** — Chạy testcase riêng TEST-V3-BC-302-FLOW và AC-BC-302-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-302 trong traceability.

## REQ-V3-BC-318 / BC-318: Đọc bằng chứng claim và phê duyệt

- [ ] **T-BC-318-CONTRACT** — Chốt operation «Đọc bằng chứng claim và phê duyệt»: allowed actor R17, guard «Không thay đổi outcome; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource claim/evidence/proposal/approval, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-318-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-318-DB** — Review forward schema/index/constraint của orders cho claim/evidence/proposal/approval nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-318-BE** — Implement «Đọc bằng chứng claim và phê duyệt» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-318-UI** — Tích hợp Internal scoped channel — Đọc bằng chứng claim và phê duyệt; action «Đọc bằng chứng claim và phê duyệt», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-318-VERIFY** — Chạy testcase riêng TEST-V3-BC-318-FLOW và AC-BC-318-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-318 trong traceability.


## Closure toàn Spec

- [ ] T-L021-FULL: từng REQ có AC/design/task/code/API/DB/UI/test/receipt; lint/typecheck/build/test theo app liên quan, required Architecture/H2/PG18 CI exact revision, report/privacy/owned cleanup và backward compatibility giữ nguyên; no unresolved blocking findings.
- [ ] T-L021-ACCEPT: scoped PR integration PASS → Owner merge → regression trên merge SHA → Owner full-spec acceptance riêng. Chưa tick vì docs promotion.


- [ ] **T-AB-CLAIMS** — REQ-AB-CLAIMS: close approved operation/state/data/UX/config contract + fixtures; implement owner API/DB/HTTP and integrated Personal/Merchant claim case/evidence/outcome; unit/PG/security/browser or verified channel assert «Personal owner verified without Merchant; wrong case/evidence denied; no self-approved compensation or fake paid status; duplicate case policy»; TEST-AB-CLAIMS with source+integration receipts. Dependencies L020;L016;L018; do not mark full spec when missing.
