# Tasks 005 — pricing-contracts-quotation

NOT_STARTED; thứ tự là contract/fixture → DB → BE/HTTP → UI/channel → verification → full acceptance. Mỗi BC là gói end-to-end nội bộ, không thay phương pháp Spec-by-Spec. Không task nào DONE trong rebuild docs.

## REQ-V3-BC-002 / BC-002: Ước tính cước vận chuyển

- [ ] **T-BC-002-CONTRACT** — Chốt operation «Ước tính cước vận chuyển»: allowed actor R01, guard «Không lộ giá hợp đồng Merchant; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource service policy/quote/contract version, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-002-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-002-DB** — Review forward schema/index/constraint của pricing cho service policy/quote/contract version nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-002-BE** — Implement «Ước tính cước vận chuyển» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-002-UI** — Tích hợp Public — Ước tính cước vận chuyển; action «Ước tính cước vận chuyển», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-002-VERIFY** — Chạy testcase riêng TEST-V3-BC-002-FLOW và AC-BC-002-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-002 trong traceability.

## REQ-V3-BC-019 / BC-019: Xem hợp đồng và biểu phí áp dụng

- [ ] **T-BC-019-CONTRACT** — Chốt operation «Xem hợp đồng và biểu phí áp dụng»: allowed actor R02, guard «Không tự sửa bảng giá; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource service policy/quote/contract version, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-019-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-019-DB** — Review forward schema/index/constraint của pricing cho service policy/quote/contract version nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-019-BE** — Implement «Xem hợp đồng và biểu phí áp dụng» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-019-UI** — Tích hợp Merchant current context — Xem hợp đồng và biểu phí áp dụng; action «Xem hợp đồng và biểu phí áp dụng», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-019-VERIFY** — Chạy testcase riêng TEST-V3-BC-019-FLOW và AC-BC-019-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-019 trong traceability.

## REQ-V3-BC-023 / BC-023: Nhận báo giá và chọn dịch vụ

- [ ] **T-BC-023-CONTRACT** — Chốt operation «Nhận báo giá và chọn dịch vụ»: allowed actor R03, guard «Snapshot giá khi chốt; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource service policy/quote/contract version, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-023-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-023-DB** — Review forward schema/index/constraint của pricing cho service policy/quote/contract version nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-023-BE** — Implement «Nhận báo giá và chọn dịch vụ» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-023-UI** — Tích hợp Merchant current context — Nhận báo giá và chọn dịch vụ; action «Nhận báo giá và chọn dịch vụ», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-023-VERIFY** — Chạy testcase riêng TEST-V3-BC-023-FLOW và AC-BC-023-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-023 trong traceability.

## REQ-V3-BC-039 / BC-039: Quản trị chính sách dịch vụ/bảng giá được ủy quyền

- [ ] **T-BC-039-CONTRACT** — Chốt operation «Quản trị chính sách dịch vụ/bảng giá được ủy quyền»: allowed actor R04, guard «Không tự động có quyền sửa COD; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource service policy/quote/contract version, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-039-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-039-DB** — Review forward schema/index/constraint của pricing cho service policy/quote/contract version nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-039-BE** — Implement «Quản trị chính sách dịch vụ/bảng giá được ủy quyền» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-039-UI** — Tích hợp Internal scoped channel — Quản trị chính sách dịch vụ/bảng giá được ủy quyền; action «Quản trị chính sách dịch vụ/bảng giá được ủy quyền», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-039-VERIFY** — Chạy testcase riêng TEST-V3-BC-039-FLOW và AC-BC-039-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-039 trong traceability.

## REQ-V3-BC-131 / BC-131: Tra cứu danh mục dịch vụ, điều kiện gửi hàng

- [ ] **T-BC-131-CONTRACT** — Chốt operation «Tra cứu danh mục dịch vụ, điều kiện gửi hàng»: allowed actor R01, guard «Catalog công khai, không lộ hợp đồng riêng; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource service policy/quote/contract version, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-131-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-131-DB** — Review forward schema/index/constraint của pricing cho service policy/quote/contract version nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-131-BE** — Implement «Tra cứu danh mục dịch vụ, điều kiện gửi hàng» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-131-UI** — Tích hợp Public — Tra cứu danh mục dịch vụ, điều kiện gửi hàng; action «Tra cứu danh mục dịch vụ, điều kiện gửi hàng», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-131-VERIFY** — Chạy testcase riêng TEST-V3-BC-131-FLOW và AC-BC-131-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-131 trong traceability.

## REQ-V3-BC-138 / BC-138: Báo giá và chọn dịch vụ gửi lẻ

- [ ] **T-BC-138-CONTRACT** — Chốt operation «Báo giá và chọn dịch vụ gửi lẻ»: allowed actor R18, guard «Không áp dụng giá hợp đồng Merchant trái quyền; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource service policy/quote/contract version, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-138-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-138-DB** — Review forward schema/index/constraint của pricing cho service policy/quote/contract version nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-138-BE** — Implement «Báo giá và chọn dịch vụ gửi lẻ» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-138-UI** — Tích hợp Personal — Báo giá và chọn dịch vụ gửi lẻ; action «Báo giá và chọn dịch vụ gửi lẻ», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-138-VERIFY** — Chạy testcase riêng TEST-V3-BC-138-FLOW và AC-BC-138-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-138 trong traceability.

## REQ-V3-BC-189 / BC-189: Xem phí gốc, phụ phí và điều chỉnh theo Shipment/hợp đồng

- [ ] **T-BC-189-CONTRACT** — Chốt operation «Xem phí gốc, phụ phí và điều chỉnh theo Shipment/hợp đồng»: allowed actor R11, guard «Không tự thay bảng giá; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource service policy/quote/contract version, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-189-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-189-DB** — Review forward schema/index/constraint của pricing cho service policy/quote/contract version nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-189-BE** — Implement «Xem phí gốc, phụ phí và điều chỉnh theo Shipment/hợp đồng» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-189-UI** — Tích hợp Merchant current context — Xem phí gốc, phụ phí và điều chỉnh theo Shipment/hợp đồng; action «Xem phí gốc, phụ phí và điều chỉnh theo Shipment/hợp đồng», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-189-VERIFY** — Chạy testcase riêng TEST-V3-BC-189-FLOW và AC-BC-189-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-189 trong traceability.

## REQ-V3-BC-290 / BC-290: Đối chiếu giá chốt, phí điều chỉnh sau cân đo

- [ ] **T-BC-290-CONTRACT** — Chốt operation «Đối chiếu giá chốt, phí điều chỉnh sau cân đo»: allowed actor R13, guard «Snapshot + phiên bản rate card; audit sửa phí; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource service policy/quote/contract version, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-290-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-290-DB** — Review forward schema/index/constraint của pricing cho service policy/quote/contract version nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-290-BE** — Implement «Đối chiếu giá chốt, phí điều chỉnh sau cân đo» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-290-UI** — Tích hợp Internal scoped channel — Đối chiếu giá chốt, phí điều chỉnh sau cân đo; action «Đối chiếu giá chốt, phí điều chỉnh sau cân đo», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-290-VERIFY** — Chạy testcase riêng TEST-V3-BC-290-FLOW và AC-BC-290-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-290 trong traceability.

## REQ-V3-BC-304 / BC-304: Quản lý dịch vụ và chính sách nghiệp vụ theo quyền

- [ ] **T-BC-304-CONTRACT** — Chốt operation «Quản lý dịch vụ và chính sách nghiệp vụ theo quyền»: allowed actor R04, guard «Giá đặc biệt cần approval riêng; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource service policy/quote/contract version, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-304-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-304-DB** — Review forward schema/index/constraint của pricing cho service policy/quote/contract version nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-304-BE** — Implement «Quản lý dịch vụ và chính sách nghiệp vụ theo quyền» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-304-UI** — Tích hợp Internal scoped channel — Quản lý dịch vụ và chính sách nghiệp vụ theo quyền; action «Quản lý dịch vụ và chính sách nghiệp vụ theo quyền», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-304-VERIFY** — Chạy testcase riêng TEST-V3-BC-304-FLOW và AC-BC-304-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-304 trong traceability.

## REQ-V3-BC-319 / BC-319: Cấu hình rate card theo vùng và tuyến

- [ ] **T-BC-319-CONTRACT** — Chốt operation «Cấu hình rate card theo vùng và tuyến»: allowed actor R28, guard «Version, effective date, kiểm soát hồi tố; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource service policy/quote/contract version, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-319-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-319-DB** — Review forward schema/index/constraint của pricing cho service policy/quote/contract version nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-319-BE** — Implement «Cấu hình rate card theo vùng và tuyến» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-319-UI** — Tích hợp Internal scoped channel — Cấu hình rate card theo vùng và tuyến; action «Cấu hình rate card theo vùng và tuyến», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-319-VERIFY** — Chạy testcase riêng TEST-V3-BC-319-FLOW và AC-BC-319-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-319 trong traceability.

## REQ-V3-BC-320 / BC-320: Quy tắc trọng lượng tính cước và đo kích thước

- [ ] **T-BC-320-CONTRACT** — Chốt operation «Quy tắc trọng lượng tính cước và đo kích thước»: allowed actor R28, guard «Khối lượng quy đổi/cân thực tế theo policy; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource service policy/quote/contract version, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-320-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-320-DB** — Review forward schema/index/constraint của pricing cho service policy/quote/contract version nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-320-BE** — Implement «Quy tắc trọng lượng tính cước và đo kích thước» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-320-UI** — Tích hợp Internal scoped channel — Quy tắc trọng lượng tính cước và đo kích thước; action «Quy tắc trọng lượng tính cước và đo kích thước», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-320-VERIFY** — Chạy testcase riêng TEST-V3-BC-320-FLOW và AC-BC-320-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-320 trong traceability.

## REQ-V3-BC-321 / BC-321: Quản trị loại dịch vụ vận chuyển

- [ ] **T-BC-321-CONTRACT** — Chốt operation «Quản trị loại dịch vụ vận chuyển»: allowed actor R28, guard «Service availability và vùng phục vụ; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource service policy/quote/contract version, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-321-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-321-DB** — Review forward schema/index/constraint của pricing cho service policy/quote/contract version nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-321-BE** — Implement «Quản trị loại dịch vụ vận chuyển» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-321-UI** — Tích hợp Internal scoped channel — Quản trị loại dịch vụ vận chuyển; action «Quản trị loại dịch vụ vận chuyển», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-321-VERIFY** — Chạy testcase riêng TEST-V3-BC-321-FLOW và AC-BC-321-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-321 trong traceability.

## REQ-V3-BC-322 / BC-322: Cấu hình phí COD và các phụ phí

- [ ] **T-BC-322-CONTRACT** — Chốt operation «Cấu hình phí COD và các phụ phí»: allowed actor R28, guard «Min/max/tiers rõ ràng, audit; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource service policy/quote/contract version, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-322-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-322-DB** — Review forward schema/index/constraint của pricing cho service policy/quote/contract version nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-322-BE** — Implement «Cấu hình phí COD và các phụ phí» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-322-UI** — Tích hợp Internal scoped channel — Cấu hình phí COD và các phụ phí; action «Cấu hình phí COD và các phụ phí», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-322-VERIFY** — Chạy testcase riêng TEST-V3-BC-322-FLOW và AC-BC-322-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-322 trong traceability.

## REQ-V3-BC-323 / BC-323: Áp dụng biểu phí riêng theo hợp đồng Merchant

- [ ] **T-BC-323-CONTRACT** — Chốt operation «Áp dụng biểu phí riêng theo hợp đồng Merchant»: allowed actor R28, guard «Chỉ hợp đồng được phê duyệt và còn hiệu lực; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource service policy/quote/contract version, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-323-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-323-DB** — Review forward schema/index/constraint của pricing cho service policy/quote/contract version nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-323-BE** — Implement «Áp dụng biểu phí riêng theo hợp đồng Merchant» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-323-UI** — Tích hợp Internal scoped channel — Áp dụng biểu phí riêng theo hợp đồng Merchant; action «Áp dụng biểu phí riêng theo hợp đồng Merchant», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-323-VERIFY** — Chạy testcase riêng TEST-V3-BC-323-FLOW và AC-BC-323-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-323 trong traceability.

## REQ-V3-BC-324 / BC-324: Quản lý chiết khấu và khuyến mãi

- [ ] **T-BC-324-CONTRACT** — Chốt operation «Quản lý chiết khấu và khuyến mãi»: allowed actor R28, guard «Điều kiện áp dụng, phê duyệt và hạn sử dụng; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource service policy/quote/contract version, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-324-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-324-DB** — Review forward schema/index/constraint của pricing cho service policy/quote/contract version nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-324-BE** — Implement «Quản lý chiết khấu và khuyến mãi» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-324-UI** — Tích hợp Internal scoped channel — Quản lý chiết khấu và khuyến mãi; action «Quản lý chiết khấu và khuyến mãi», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-324-VERIFY** — Chạy testcase riêng TEST-V3-BC-324-FLOW và AC-BC-324-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-324 trong traceability.

## REQ-V3-BC-325 / BC-325: Quản trị thời điểm hiệu lực bảng giá

- [ ] **T-BC-325-CONTRACT** — Chốt operation «Quản trị thời điểm hiệu lực bảng giá»: allowed actor R28, guard «Không tự hồi tố đơn đã chốt; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource service policy/quote/contract version, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-325-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-325-DB** — Review forward schema/index/constraint của pricing cho service policy/quote/contract version nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-325-BE** — Implement «Quản trị thời điểm hiệu lực bảng giá» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-325-UI** — Tích hợp Internal scoped channel — Quản trị thời điểm hiệu lực bảng giá; action «Quản trị thời điểm hiệu lực bảng giá», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-325-VERIFY** — Chạy testcase riêng TEST-V3-BC-325-FLOW và AC-BC-325-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-325 trong traceability.

## REQ-V3-BC-326 / BC-326: Phân biệt giá ước tính, giá chốt và phí điều chỉnh

- [ ] **T-BC-326-CONTRACT** — Chốt operation «Phân biệt giá ước tính, giá chốt và phí điều chỉnh»: allowed actor R28, guard «Lưu price snapshot + adjustment trail; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource service policy/quote/contract version, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-326-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-326-DB** — Review forward schema/index/constraint của pricing cho service policy/quote/contract version nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-326-BE** — Implement «Phân biệt giá ước tính, giá chốt và phí điều chỉnh» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-326-UI** — Tích hợp Internal scoped channel — Phân biệt giá ước tính, giá chốt và phí điều chỉnh; action «Phân biệt giá ước tính, giá chốt và phí điều chỉnh», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-326-VERIFY** — Chạy testcase riêng TEST-V3-BC-326-FLOW và AC-BC-326-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-326 trong traceability.

## REQ-V3-BC-328 / BC-328: Audit sửa bảng giá, phê duyệt và phiên bản

- [ ] **T-BC-328-CONTRACT** — Chốt operation «Audit sửa bảng giá, phê duyệt và phiên bản»: allowed actor R28, guard «Không xóa price history; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource service policy/quote/contract version, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-328-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-328-DB** — Review forward schema/index/constraint của pricing cho service policy/quote/contract version nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-328-BE** — Implement «Audit sửa bảng giá, phê duyệt và phiên bản» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-328-UI** — Tích hợp Internal scoped channel — Audit sửa bảng giá, phê duyệt và phiên bản; action «Audit sửa bảng giá, phê duyệt và phiên bản», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-328-VERIFY** — Chạy testcase riêng TEST-V3-BC-328-FLOW và AC-BC-328-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-328 trong traceability.

## REQ-V3-BC-331 / BC-331: Theo dõi hợp đồng Merchant và gia hạn

- [ ] **T-BC-331-CONTRACT** — Chốt operation «Theo dõi hợp đồng Merchant và gia hạn»: allowed actor R29, guard «Quyền xem theo scope, tài liệu versioned; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource service policy/quote/contract version, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-331-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-331-DB** — Review forward schema/index/constraint của pricing cho service policy/quote/contract version nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-331-BE** — Implement «Theo dõi hợp đồng Merchant và gia hạn» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-331-UI** — Tích hợp Internal scoped channel — Theo dõi hợp đồng Merchant và gia hạn; action «Theo dõi hợp đồng Merchant và gia hạn», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-331-VERIFY** — Chạy testcase riêng TEST-V3-BC-331-FLOW và AC-BC-331-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-331 trong traceability.

## REQ-V3-BC-332 / BC-332: Đề xuất bảng giá hoặc chiết khấu đặc biệt

- [ ] **T-BC-332-CONTRACT** — Chốt operation «Đề xuất bảng giá hoặc chiết khấu đặc biệt»: allowed actor R29, guard «Không tự phê duyệt hoặc áp dụng giá; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource service policy/quote/contract version, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-332-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-332-DB** — Review forward schema/index/constraint của pricing cho service policy/quote/contract version nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-332-BE** — Implement «Đề xuất bảng giá hoặc chiết khấu đặc biệt» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-332-UI** — Tích hợp Internal scoped channel — Đề xuất bảng giá hoặc chiết khấu đặc biệt; action «Đề xuất bảng giá hoặc chiết khấu đặc biệt», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-332-VERIFY** — Chạy testcase riêng TEST-V3-BC-332-FLOW và AC-BC-332-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-332 trong traceability.

## REQ-V3-BC-336 / BC-336: Audit đề xuất giá và thay đổi quan hệ hợp đồng

- [ ] **T-BC-336-CONTRACT** — Chốt operation «Audit đề xuất giá và thay đổi quan hệ hợp đồng»: allowed actor R29, guard «Approval trace; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource service policy/quote/contract version, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-336-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-336-DB** — Review forward schema/index/constraint của pricing cho service policy/quote/contract version nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-336-BE** — Implement «Audit đề xuất giá và thay đổi quan hệ hợp đồng» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-336-UI** — Tích hợp Internal scoped channel — Audit đề xuất giá và thay đổi quan hệ hợp đồng; action «Audit đề xuất giá và thay đổi quan hệ hợp đồng», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-336-VERIFY** — Chạy testcase riêng TEST-V3-BC-336-FLOW và AC-BC-336-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-336 trong traceability.


- [ ] T-LEGACY-L005: map từng obligation legacy ở spec sang implementation/negative/compatibility testcase, bao gồm phần consumer; không đóng full spec khi requirement legacy còn chưa được chứng minh hoặc disposition rõ.

## Closure toàn Spec

- [ ] T-L005-FULL: từng REQ có AC/design/task/code/API/DB/UI/test/receipt; lint/typecheck/build/test theo app liên quan, required Architecture/H2/PG18 CI exact revision, report/privacy/owned cleanup và backward compatibility giữ nguyên; no unresolved blocking findings.
- [ ] T-L005-ACCEPT: scoped PR integration PASS → Owner merge → regression trên merge SHA → Owner full-spec acceptance riêng. Chưa tick vì docs promotion.


- [ ] **T-AB-QUOTE-FORMULA** — REQ-AB-QUOTE-FORMULA: close approved operation/state/data/UX/config contract + fixtures; implement owner API/DB/HTTP and integrated Quote/service/promotion/price ETA summary; unit/PG/security/browser or verified channel assert «Golden fixtures boundaries/expired quote/publish races/private rate isolation; missing rule rejects rather than guesses; unsupported international/immediate not advertised active»; TEST-AB-QUOTE-FORMULA with source+integration receipts. Dependencies L004; do not mark full spec when missing.
