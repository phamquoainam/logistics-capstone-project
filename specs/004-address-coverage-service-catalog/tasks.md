# Tasks 004 — address-coverage-service-catalog

NOT_STARTED; thứ tự là contract/fixture → DB → BE/HTTP → UI/channel → verification → full acceptance. Mỗi BC là gói end-to-end nội bộ, không thay phương pháp Spec-by-Spec. Không task nào DONE trong rebuild docs.

## REQ-V3-BC-001 / BC-001: Tra cứu vùng phục vụ công khai

- [ ] **T-BC-001-CONTRACT** — Chốt operation «Tra cứu vùng phục vụ công khai»: allowed actor R01, guard «Chỉ dữ liệu công khai; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource versioned address/catalog/coverage, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-001-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-001-DB** — Review forward schema/index/constraint của merchant cho versioned address/catalog/coverage nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-001-BE** — Implement «Tra cứu vùng phục vụ công khai» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-001-UI** — Tích hợp Public — Tra cứu vùng phục vụ công khai; action «Tra cứu vùng phục vụ công khai», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-001-VERIFY** — Chạy testcase riêng TEST-V3-BC-001-FLOW và AC-BC-001-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-001 trong traceability.

## REQ-V3-BC-005 / BC-005: Tra cứu bưu cục/Hub có phục vụ, địa chỉ, giờ hoạt động

- [ ] **T-BC-005-CONTRACT** — Chốt operation «Tra cứu bưu cục/Hub có phục vụ, địa chỉ, giờ hoạt động»: allowed actor R01, guard «Dữ liệu công khai; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource versioned address/catalog/coverage, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-005-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-005-DB** — Review forward schema/index/constraint của merchant cho versioned address/catalog/coverage nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-005-BE** — Implement «Tra cứu bưu cục/Hub có phục vụ, địa chỉ, giờ hoạt động» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-005-UI** — Tích hợp Public — Tra cứu bưu cục/Hub có phục vụ, địa chỉ, giờ hoạt động; action «Tra cứu bưu cục/Hub có phục vụ, địa chỉ, giờ hoạt động», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-005-VERIFY** — Chạy testcase riêng TEST-V3-BC-005-FLOW và AC-BC-005-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-005 trong traceability.

## REQ-V3-BC-010 / BC-010: Quản lý địa chỉ lấy hàng

- [ ] **T-BC-010-CONTRACT** — Chốt operation «Quản lý địa chỉ lấy hàng»: allowed actor R02, guard «Catalog phiên bản và kiểm tra dữ liệu; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource versioned address/catalog/coverage, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-010-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-010-DB** — Review forward schema/index/constraint của merchant cho versioned address/catalog/coverage nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-010-BE** — Implement «Quản lý địa chỉ lấy hàng» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-010-UI** — Tích hợp Merchant current context — Quản lý địa chỉ lấy hàng; action «Quản lý địa chỉ lấy hàng», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-010-VERIFY** — Chạy testcase riêng TEST-V3-BC-010-FLOW và AC-BC-010-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-010 trong traceability.

## REQ-V3-BC-022 / BC-022: Chuẩn hóa địa chỉ nhận/gửi

- [ ] **T-BC-022-CONTRACT** — Chốt operation «Chuẩn hóa địa chỉ nhận/gửi»: allowed actor R03, guard «Dữ liệu hợp lệ; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource versioned address/catalog/coverage, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-022-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-022-DB** — Review forward schema/index/constraint của merchant cho versioned address/catalog/coverage nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-022-BE** — Implement «Chuẩn hóa địa chỉ nhận/gửi» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-022-UI** — Tích hợp Merchant current context — Chuẩn hóa địa chỉ nhận/gửi; action «Chuẩn hóa địa chỉ nhận/gửi», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-022-VERIFY** — Chạy testcase riêng TEST-V3-BC-022-FLOW và AC-BC-022-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-022 trong traceability.

## REQ-V3-BC-033 / BC-033: Quản lý vùng phục vụ theo policy

- [ ] **T-BC-033-CONTRACT** — Chốt operation «Quản lý vùng phục vụ theo policy»: allowed actor R04, guard «Có phiên bản và audit; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource versioned address/catalog/coverage, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-033-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-033-DB** — Review forward schema/index/constraint của merchant cho versioned address/catalog/coverage nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-033-BE** — Implement «Quản lý vùng phục vụ theo policy» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-033-UI** — Tích hợp Internal scoped channel — Quản lý vùng phục vụ theo policy; action «Quản lý vùng phục vụ theo policy», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-033-VERIFY** — Chạy testcase riêng TEST-V3-BC-033-FLOW và AC-BC-033-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-033 trong traceability.

## REQ-V3-BC-136 / BC-136: Lưu và quản lý địa chỉ gửi/nhận thường dùng

- [ ] **T-BC-136-CONTRACT** — Chốt operation «Lưu và quản lý địa chỉ gửi/nhận thường dùng»: allowed actor R18, guard «Chỉ sở hữu cá nhân; chuẩn hóa địa chỉ; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource versioned address/catalog/coverage, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-136-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-136-DB** — Review forward schema/index/constraint của merchant cho versioned address/catalog/coverage nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-136-BE** — Implement «Lưu và quản lý địa chỉ gửi/nhận thường dùng» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-136-UI** — Tích hợp Personal — Lưu và quản lý địa chỉ gửi/nhận thường dùng; action «Lưu và quản lý địa chỉ gửi/nhận thường dùng», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-136-VERIFY** — Chạy testcase riêng TEST-V3-BC-136-FLOW và AC-BC-136-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-136 trong traceability.

## REQ-V3-BC-137 / BC-137: Kiểm tra vùng phục vụ theo tuyến gửi lẻ

- [ ] **T-BC-137-CONTRACT** — Chốt operation «Kiểm tra vùng phục vụ theo tuyến gửi lẻ»: allowed actor R18, guard «Coverage snapshot/quy tắc hợp lệ; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource versioned address/catalog/coverage, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-137-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-137-DB** — Review forward schema/index/constraint của merchant cho versioned address/catalog/coverage nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-137-BE** — Implement «Kiểm tra vùng phục vụ theo tuyến gửi lẻ» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-137-UI** — Tích hợp Personal — Kiểm tra vùng phục vụ theo tuyến gửi lẻ; action «Kiểm tra vùng phục vụ theo tuyến gửi lẻ», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-137-VERIFY** — Chạy testcase riêng TEST-V3-BC-137-FLOW và AC-BC-137-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-137 trong traceability.

## REQ-V3-BC-271 / BC-271: Quản lý vùng giao nhận và coverage

- [ ] **T-BC-271-CONTRACT** — Chốt operation «Quản lý vùng giao nhận và coverage»: allowed actor R27, guard «Versioning/policy, không hồi tố đơn đã chốt tùy ý; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource versioned address/catalog/coverage, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-271-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-271-DB** — Review forward schema/index/constraint của merchant cho versioned address/catalog/coverage nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-271-BE** — Implement «Quản lý vùng giao nhận và coverage» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-271-UI** — Tích hợp Internal scoped channel — Quản lý vùng giao nhận và coverage; action «Quản lý vùng giao nhận và coverage», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-271-VERIFY** — Chạy testcase riêng TEST-V3-BC-271-FLOW và AC-BC-271-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-271 trong traceability.


- [ ] T-LEGACY-L004: map từng obligation legacy ở spec sang implementation/negative/compatibility testcase, bao gồm phần consumer; không đóng full spec khi requirement legacy còn chưa được chứng minh hoặc disposition rõ.

## Closure toàn Spec

- [ ] T-L004-FULL: từng REQ có AC/design/task/code/API/DB/UI/test/receipt; lint/typecheck/build/test theo app liên quan, required Architecture/H2/PG18 CI exact revision, report/privacy/owned cleanup và backward compatibility giữ nguyên; no unresolved blocking findings.
- [ ] T-L004-ACCEPT: scoped PR integration PASS → Owner merge → regression trên merge SHA → Owner full-spec acceptance riêng. Chưa tick vì docs promotion.

## REQ-V3-BC-009 / BC-009: Quản lý pickup point

- [ ] **T-BC-009-CONTRACT** — Chốt operation «Quản lý pickup point»: allowed actor R02, guard «Tenant isolation; archive/sửa có điều kiện; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource Merchant pickup point/address validation reference, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-009-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-009-DB** — Review forward schema/index/constraint của merchant cho Merchant pickup point/address validation reference nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-009-BE** — Implement «Quản lý pickup point» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-009-UI** — Tích hợp Merchant current context — Quản lý pickup point; action «Quản lý pickup point», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-009-VERIFY** — Chạy testcase riêng TEST-V3-BC-009-FLOW và AC-BC-009-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-009 trong traceability.

## REQ-V3-BC-021 / BC-021: Xem và sửa pickup point được ủy quyền

- [ ] **T-BC-021-CONTRACT** — Chốt operation «Xem và sửa pickup point được ủy quyền»: allowed actor R03, guard «Không quản lý nhân viên mặc định; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource delegated Merchant pickup point/address reference, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-021-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-021-DB** — Review forward schema/index/constraint của merchant cho delegated Merchant pickup point/address reference nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-021-BE** — Implement «Xem và sửa pickup point được ủy quyền» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-021-UI** — Tích hợp Merchant current context — Xem và sửa pickup point được ủy quyền; action «Xem và sửa pickup point được ủy quyền», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-021-VERIFY** — Chạy testcase riêng TEST-V3-BC-021-FLOW và AC-BC-021-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-021 trong traceability.

## REQ-V3-BC-168 / BC-168: Quản lý pickup point trong phạm vi ủy quyền

- [ ] **T-BC-168-CONTRACT** — Chốt operation «Quản lý pickup point trong phạm vi ủy quyền»: allowed actor R19, guard «Merchant/branch scope; không truy cập tenant khác; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource delegated Manager pickup point/address reference, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-168-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-168-DB** — Review forward schema/index/constraint của merchant cho delegated Manager pickup point/address reference nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-168-BE** — Implement «Quản lý pickup point trong phạm vi ủy quyền» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-168-UI** — Tích hợp Merchant current context — Quản lý pickup point trong phạm vi ủy quyền; action «Quản lý pickup point trong phạm vi ủy quyền», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-168-VERIFY** — Chạy testcase riêng TEST-V3-BC-168-FLOW và AC-BC-168-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-168 trong traceability.


- [ ] T-LEGACY-L003: map từng obligation legacy ở spec sang implementation/negative/compatibility testcase, bao gồm phần consumer; không đóng full spec khi requirement legacy còn chưa được chứng minh hoặc disposition rõ.
