# Tasks 017 — cod-collection-cashier-remittance

NOT_STARTED; thứ tự là contract/fixture → DB → BE/HTTP → UI/channel → verification → full acceptance. Mỗi BC là gói end-to-end nội bộ, không thay phương pháp Spec-by-Spec. Không task nào DONE trong rebuild docs.

## REQ-V3-BC-014 / BC-014: Xem COD thuộc Merchant

- [ ] **T-BC-014-CONTRACT** — Chốt operation «Xem COD thuộc Merchant»: allowed actor R02, guard «Chỉ đọc theo quyền; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource COD liability/collection/remittance, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-014-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-014-DB** — Review forward schema/index/constraint của finance cho COD liability/collection/remittance nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-014-BE** — Implement «Xem COD thuộc Merchant» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-014-UI** — Tích hợp Merchant current context — Xem COD thuộc Merchant; action «Xem COD thuộc Merchant», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-014-VERIFY** — Chạy testcase riêng TEST-V3-BC-014-FLOW và AC-BC-014-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-014 trong traceability.

## REQ-V3-BC-028 / BC-028: Xem COD của đơn được giao phụ trách

- [ ] **T-BC-028-CONTRACT** — Chốt operation «Xem COD của đơn được giao phụ trách»: allowed actor R03, guard «Không sửa COD đã chốt; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource COD liability/collection/remittance, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-028-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-028-DB** — Review forward schema/index/constraint của finance cho COD liability/collection/remittance nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-028-BE** — Implement «Xem COD của đơn được giao phụ trách» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-028-UI** — Tích hợp Merchant current context — Xem COD của đơn được giao phụ trách; action «Xem COD của đơn được giao phụ trách», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-028-VERIFY** — Chạy testcase riêng TEST-V3-BC-028-FLOW và AC-BC-028-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-028 trong traceability.

## REQ-V3-BC-040 / BC-040: Thực hiện điều chỉnh COD/settlement

- [ ] **T-BC-040-CONTRACT** — Chốt operation «Thực hiện điều chỉnh COD/settlement»: allowed actor R04, guard «Không mặc định được phép; cần workflow tài chính riêng; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource COD liability/collection/remittance, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-040-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-040-DB** — Chứng minh không ghi effect bị cấm; không thêm positive schema/seed.
- [ ] **T-BC-040-BE** — Implement «Thực hiện điều chỉnh COD/settlement» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, deny all unauthorized states, không positive branch.
- [ ] **T-BC-040-UI** — Tích hợp Internal scoped channel — Thực hiện điều chỉnh COD/settlement; không nút cấp hành vi cấm; direct API denial vẫn bắt buộc.
- [ ] **T-BC-040-VERIFY** — Chạy testcase riêng TEST-V3-BC-040-DENY và AC-BC-040-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-040 trong traceability.

## REQ-V3-BC-046 / BC-046: Sửa/duyệt COD

- [ ] **T-BC-046-CONTRACT** — Chốt operation «Sửa/duyệt COD»: allowed actor R05, guard «Ngoài quyền Identity; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource COD liability/collection/remittance, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-046-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-046-DB** — Chứng minh không ghi effect bị cấm; không thêm positive schema/seed.
- [ ] **T-BC-046-BE** — Implement «Sửa/duyệt COD» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, deny all unauthorized states, không positive branch.
- [ ] **T-BC-046-UI** — Tích hợp Internal scoped channel — Sửa/duyệt COD; không nút cấp hành vi cấm; direct API denial vẫn bắt buộc.
- [ ] **T-BC-046-VERIFY** — Chạy testcase riêng TEST-V3-BC-046-DENY và AC-BC-046-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-046 trong traceability.

## REQ-V3-BC-075 / BC-075: Ghi nhận khoản COD đã thu

- [ ] **T-BC-075-CONTRACT** — Chốt operation «Ghi nhận khoản COD đã thu»: allowed actor R09, guard «Đúng số tiền đã chốt; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource COD liability/collection/remittance, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-075-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-075-DB** — Review forward schema/index/constraint của finance cho COD liability/collection/remittance nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-075-BE** — Implement «Ghi nhận khoản COD đã thu» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-075-UI** — Tích hợp Internal scoped channel — Ghi nhận khoản COD đã thu; action «Ghi nhận khoản COD đã thu», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-075-VERIFY** — Chạy testcase riêng TEST-V3-BC-075-FLOW và AC-BC-075-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-075 trong traceability.

## REQ-V3-BC-076 / BC-076: Yêu cầu nộp COD cho quỹ

- [ ] **T-BC-076-CONTRACT** — Chốt operation «Yêu cầu nộp COD cho quỹ»: allowed actor R09, guard «Không tự xác nhận quỹ đã nhận; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource COD liability/collection/remittance, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-076-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-076-DB** — Review forward schema/index/constraint của finance cho COD liability/collection/remittance nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-076-BE** — Implement «Yêu cầu nộp COD cho quỹ» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-076-UI** — Tích hợp Internal scoped channel — Yêu cầu nộp COD cho quỹ; action «Yêu cầu nộp COD cho quỹ», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-076-VERIFY** — Chạy testcase riêng TEST-V3-BC-076-FLOW và AC-BC-076-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-076 trong traceability.

## REQ-V3-BC-082 / BC-082: Thanh toán COD đã xác nhận

- [ ] **T-BC-082-CONTRACT** — Chốt operation «Thanh toán COD đã xác nhận»: allowed actor R10, guard «Không thay đổi cod_amount; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource COD liability/collection/remittance, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-082-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-082-DB** — Review forward schema/index/constraint của finance cho COD liability/collection/remittance nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-082-BE** — Implement «Thanh toán COD đã xác nhận» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-082-UI** — Tích hợp Verified Recipient channel — Thanh toán COD đã xác nhận;Courier/CS; public tracking; không Portal/IAM với accepted client/channel; action «Thanh toán COD đã xác nhận», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-082-VERIFY** — Chạy testcase riêng TEST-V3-BC-082-FLOW và AC-BC-082-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-082 trong traceability.

## REQ-V3-BC-086 / BC-086: Xem COD dự kiến/đã thu/đang giữ/đã trả

- [ ] **T-BC-086-CONTRACT** — Chốt operation «Xem COD dự kiến/đã thu/đang giữ/đã trả»: allowed actor R11, guard «Merchant isolation; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource COD liability/collection/remittance, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-086-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-086-DB** — Review forward schema/index/constraint của finance cho COD liability/collection/remittance nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-086-BE** — Implement «Xem COD dự kiến/đã thu/đang giữ/đã trả» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-086-UI** — Tích hợp Merchant current context — Xem COD dự kiến/đã thu/đang giữ/đã trả; action «Xem COD dự kiến/đã thu/đang giữ/đã trả», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-086-VERIFY** — Chạy testcase riêng TEST-V3-BC-086-FLOW và AC-BC-086-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-086 trong traceability.

## REQ-V3-BC-092 / BC-092: Tiếp nhận tiền mặt COD Courier nộp

- [ ] **T-BC-092-CONTRACT** — Chốt operation «Tiếp nhận tiền mặt COD Courier nộp»: allowed actor R12, guard «Hai phía, amount và timestamp; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource COD liability/collection/remittance, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-092-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-092-DB** — Review forward schema/index/constraint của finance cho COD liability/collection/remittance nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-092-BE** — Implement «Tiếp nhận tiền mặt COD Courier nộp» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-092-UI** — Tích hợp Internal scoped channel — Tiếp nhận tiền mặt COD Courier nộp; action «Tiếp nhận tiền mặt COD Courier nộp», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-092-VERIFY** — Chạy testcase riêng TEST-V3-BC-092-FLOW và AC-BC-092-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-092 trong traceability.

## REQ-V3-BC-093 / BC-093: Xác nhận nộp và ghi thiếu/thừa

- [ ] **T-BC-093-CONTRACT** — Chốt operation «Xác nhận nộp và ghi thiếu/thừa»: allowed actor R12, guard «Không âm thầm sửa tiền; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource COD liability/collection/remittance, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-093-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-093-DB** — Review forward schema/index/constraint của finance cho COD liability/collection/remittance nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-093-BE** — Implement «Xác nhận nộp và ghi thiếu/thừa» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-093-UI** — Tích hợp Internal scoped channel — Xác nhận nộp và ghi thiếu/thừa; action «Xác nhận nộp và ghi thiếu/thừa», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-093-VERIFY** — Chạy testcase riêng TEST-V3-BC-093-FLOW và AC-BC-093-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-093 trong traceability.

## REQ-V3-BC-094 / BC-094: Quản lý đối chiếu nộp COD theo ca

- [ ] **T-BC-094-CONTRACT** — Chốt operation «Quản lý đối chiếu nộp COD theo ca»: allowed actor R12, guard «Không tự settle Merchant; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource COD liability/collection/remittance, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-094-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-094-DB** — Review forward schema/index/constraint của finance cho COD liability/collection/remittance nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-094-BE** — Implement «Quản lý đối chiếu nộp COD theo ca» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-094-UI** — Tích hợp Internal scoped channel — Quản lý đối chiếu nộp COD theo ca; action «Quản lý đối chiếu nộp COD theo ca», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-094-VERIFY** — Chạy testcase riêng TEST-V3-BC-094-FLOW và AC-BC-094-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-094 trong traceability.

## REQ-V3-BC-095 / BC-095: Ghi audit tiền nộp và điều chỉnh

- [ ] **T-BC-095-CONTRACT** — Chốt operation «Ghi audit tiền nộp và điều chỉnh»: allowed actor R12, guard «Không xóa receipt; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource COD liability/collection/remittance, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-095-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-095-DB** — Review forward schema/index/constraint của finance cho COD liability/collection/remittance nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-095-BE** — Implement «Ghi audit tiền nộp và điều chỉnh» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-095-UI** — Tích hợp Internal scoped channel — Ghi audit tiền nộp và điều chỉnh; action «Ghi audit tiền nộp và điều chỉnh», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-095-VERIFY** — Chạy testcase riêng TEST-V3-BC-095-FLOW và AC-BC-095-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-095 trong traceability.

## REQ-V3-BC-096 / BC-096: Kiểm kê két quỹ/đóng ca nâng cao

- [ ] **T-BC-096-CONTRACT** — Chốt operation «Kiểm kê két quỹ/đóng ca nâng cao»: allowed actor R12, guard «Có đối chiếu kế toán; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource COD liability/collection/remittance, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-096-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-096-DB** — Review forward schema/index/constraint của finance cho COD liability/collection/remittance nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-096-BE** — Implement «Kiểm kê két quỹ/đóng ca nâng cao» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-096-UI** — Tích hợp Internal scoped channel — Kiểm kê két quỹ/đóng ca nâng cao; action «Kiểm kê két quỹ/đóng ca nâng cao», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-096-VERIFY** — Chạy testcase riêng TEST-V3-BC-096-FLOW và AC-BC-096-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-096 trong traceability.

## REQ-V3-BC-097 / BC-097: Đối soát Courier thu và nộp COD

- [ ] **T-BC-097-CONTRACT** — Chốt operation «Đối soát Courier thu và nộp COD»: allowed actor R13, guard «Phải khớp custody tiền và shipment; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource COD liability/collection/remittance, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-097-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-097-DB** — Review forward schema/index/constraint của finance cho COD liability/collection/remittance nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-097-BE** — Implement «Đối soát Courier thu và nộp COD» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-097-UI** — Tích hợp Internal scoped channel — Đối soát Courier thu và nộp COD; action «Đối soát Courier thu và nộp COD», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-097-VERIFY** — Chạy testcase riêng TEST-V3-BC-097-FLOW và AC-BC-097-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-097 trong traceability.

## REQ-V3-BC-098 / BC-098: Phát hiện COD thiếu/thừa/trùng

- [ ] **T-BC-098-CONTRACT** — Chốt operation «Phát hiện COD thiếu/thừa/trùng»: allowed actor R13, guard «Ledger bất biến/idempotency; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource COD liability/collection/remittance, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-098-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-098-DB** — Review forward schema/index/constraint của finance cho COD liability/collection/remittance nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-098-BE** — Implement «Phát hiện COD thiếu/thừa/trùng» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-098-UI** — Tích hợp Internal scoped channel — Phát hiện COD thiếu/thừa/trùng; action «Phát hiện COD thiếu/thừa/trùng», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-098-VERIFY** — Chạy testcase riêng TEST-V3-BC-098-FLOW và AC-BC-098-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-098 trong traceability.

## REQ-V3-BC-121 / BC-121: Giám sát bàn giao COD tại Station

- [ ] **T-BC-121-CONTRACT** — Chốt operation «Giám sát bàn giao COD tại Station»: allowed actor R16, guard «Không sửa sổ cái; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource COD liability/collection/remittance, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-121-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-121-DB** — Review forward schema/index/constraint của finance cho COD liability/collection/remittance nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-121-BE** — Implement «Giám sát bàn giao COD tại Station» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-121-UI** — Tích hợp Internal scoped channel — Giám sát bàn giao COD tại Station; action «Giám sát bàn giao COD tại Station», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-121-VERIFY** — Chạy testcase riêng TEST-V3-BC-121-FLOW và AC-BC-121-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-121 trong traceability.

## REQ-V3-BC-127 / BC-127: Đọc log đối soát COD

- [ ] **T-BC-127-CONTRACT** — Chốt operation «Đọc log đối soát COD»: allowed actor R17, guard «Không sửa tiền; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource COD liability/collection/remittance, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-127-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-127-DB** — Review forward schema/index/constraint của finance cho COD liability/collection/remittance nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-127-BE** — Implement «Đọc log đối soát COD» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-127-UI** — Tích hợp Internal scoped channel — Đọc log đối soát COD; action «Đọc log đối soát COD», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-127-VERIFY** — Chạy testcase riêng TEST-V3-BC-127-FLOW và AC-BC-127-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-127 trong traceability.

## REQ-V3-BC-161 / BC-161: Xem COD dự kiến, đã thu, đã nộp, đã đối soát và công nợ

- [ ] **T-BC-161-CONTRACT** — Chốt operation «Xem COD dự kiến, đã thu, đã nộp, đã đối soát và công nợ»: allowed actor R02, guard «Chỉ đọc tài chính thuộc Merchant; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource COD liability/collection/remittance, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-161-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-161-DB** — Review forward schema/index/constraint của finance cho COD liability/collection/remittance nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-161-BE** — Implement «Xem COD dự kiến, đã thu, đã nộp, đã đối soát và công nợ» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-161-UI** — Tích hợp Merchant current context — Xem COD dự kiến, đã thu, đã nộp, đã đối soát và công nợ; action «Xem COD dự kiến, đã thu, đã nộp, đã đối soát và công nợ», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-161-VERIFY** — Chạy testcase riêng TEST-V3-BC-161-FLOW và AC-BC-161-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-161 trong traceability.

## REQ-V3-BC-187 / BC-187: Xem COD dự kiến, đã thu, đang giữ và đã thanh toán

- [ ] **T-BC-187-CONTRACT** — Chốt operation «Xem COD dự kiến, đã thu, đang giữ và đã thanh toán»: allowed actor R11, guard «Không sửa dữ liệu thu COD của Courier; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource COD liability/collection/remittance, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-187-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-187-DB** — Review forward schema/index/constraint của finance cho COD liability/collection/remittance nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-187-BE** — Implement «Xem COD dự kiến, đã thu, đang giữ và đã thanh toán» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-187-UI** — Tích hợp Merchant current context — Xem COD dự kiến, đã thu, đang giữ và đã thanh toán; action «Xem COD dự kiến, đã thu, đang giữ và đã thanh toán», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-187-VERIFY** — Chạy testcase riêng TEST-V3-BC-187-FLOW và AC-BC-187-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-187 trong traceability.

## REQ-V3-BC-225 / BC-225: Xác nhận số COD cần thu và phương thức thanh toán

- [ ] **T-BC-225-CONTRACT** — Chốt operation «Xác nhận số COD cần thu và phương thức thanh toán»: allowed actor R09, guard «Chỉ đọc giá trị COD đã chốt; không tự đổi tiền; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource COD liability/collection/remittance, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-225-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-225-DB** — Review forward schema/index/constraint của finance cho COD liability/collection/remittance nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-225-BE** — Implement «Xác nhận số COD cần thu và phương thức thanh toán» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-225-UI** — Tích hợp Internal scoped channel — Xác nhận số COD cần thu và phương thức thanh toán; action «Xác nhận số COD cần thu và phương thức thanh toán», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-225-VERIFY** — Chạy testcase riêng TEST-V3-BC-225-FLOW và AC-BC-225-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-225 trong traceability.

## REQ-V3-BC-233 / BC-233: Giám sát Courier nộp COD tại Station

- [ ] **T-BC-233-CONTRACT** — Chốt operation «Giám sát Courier nộp COD tại Station»: allowed actor R16, guard «Cashier xác nhận tiền; manager không sửa ledger; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource COD liability/collection/remittance, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-233-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-233-DB** — Review forward schema/index/constraint của finance cho COD liability/collection/remittance nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-233-BE** — Implement «Giám sát Courier nộp COD tại Station» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-233-UI** — Tích hợp Internal scoped channel — Giám sát Courier nộp COD tại Station; action «Giám sát Courier nộp COD tại Station», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-233-VERIFY** — Chạy testcase riêng TEST-V3-BC-233-FLOW và AC-BC-233-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-233 trong traceability.

## REQ-V3-BC-280 / BC-280: Không mặc nhiên được đổi COD, thanh toán hoặc cấu hình bảo mật

- [ ] **T-BC-280-CONTRACT** — Chốt operation «Không mặc nhiên được đổi COD, thanh toán hoặc cấu hình bảo mật»: allowed actor R27, guard «Negative permission tests, separation of duties; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource COD liability/collection/remittance, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-280-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-280-DB** — Chứng minh không ghi effect bị cấm; không thêm positive schema/seed.
- [ ] **T-BC-280-BE** — Implement «Không mặc nhiên được đổi COD, thanh toán hoặc cấu hình bảo mật» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, deny all unauthorized states, không positive branch.
- [ ] **T-BC-280-UI** — Tích hợp Internal scoped channel — Không mặc nhiên được đổi COD, thanh toán hoặc cấu hình bảo mật; không nút cấp hành vi cấm; direct API denial vẫn bắt buộc.
- [ ] **T-BC-280-VERIFY** — Chạy testcase riêng TEST-V3-BC-280-DENY và AC-BC-280-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-280 trong traceability.

## REQ-V3-BC-281 / BC-281: Kiểm đếm COD thực nhận theo ca và từng Courier

- [ ] **T-BC-281-CONTRACT** — Chốt operation «Kiểm đếm COD thực nhận theo ca và từng Courier»: allowed actor R12, guard «Xác nhận song phương, denomination/count và số tiền đã chốt; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource COD liability/collection/remittance, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-281-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-281-DB** — Review forward schema/index/constraint của finance cho COD liability/collection/remittance nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-281-BE** — Implement «Kiểm đếm COD thực nhận theo ca và từng Courier» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-281-UI** — Tích hợp Internal scoped channel — Kiểm đếm COD thực nhận theo ca và từng Courier; action «Kiểm đếm COD thực nhận theo ca và từng Courier», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-281-VERIFY** — Chạy testcase riêng TEST-V3-BC-281-FLOW và AC-BC-281-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-281 trong traceability.

## REQ-V3-BC-282 / BC-282: Đóng ca và đối chiếu quỹ COD thực tế

- [ ] **T-BC-282-CONTRACT** — Chốt operation «Đóng ca và đối chiếu quỹ COD thực tế»: allowed actor R12, guard «Biên bản đóng ca, thiếu/thừa và người chịu trách nhiệm; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource COD liability/collection/remittance, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-282-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-282-DB** — Review forward schema/index/constraint của finance cho COD liability/collection/remittance nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-282-BE** — Implement «Đóng ca và đối chiếu quỹ COD thực tế» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-282-UI** — Tích hợp Internal scoped channel — Đóng ca và đối chiếu quỹ COD thực tế; action «Đóng ca và đối chiếu quỹ COD thực tế», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-282-VERIFY** — Chạy testcase riêng TEST-V3-BC-282-FLOW và AC-BC-282-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-282 trong traceability.

## REQ-V3-BC-283 / BC-283: Chuyển dữ liệu nộp tiền đã xác nhận sang đối soát

- [ ] **T-BC-283-CONTRACT** — Chốt operation «Chuyển dữ liệu nộp tiền đã xác nhận sang đối soát»: allowed actor R12, guard «Không tự ghi RECONCILED hay SETTLED; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource COD liability/collection/remittance, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-283-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-283-DB** — Review forward schema/index/constraint của finance cho COD liability/collection/remittance nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-283-BE** — Implement «Chuyển dữ liệu nộp tiền đã xác nhận sang đối soát» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-283-UI** — Tích hợp Internal scoped channel — Chuyển dữ liệu nộp tiền đã xác nhận sang đối soát; action «Chuyển dữ liệu nộp tiền đã xác nhận sang đối soát», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-283-VERIFY** — Chạy testcase riêng TEST-V3-BC-283-FLOW và AC-BC-283-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-283 trong traceability.

## REQ-V3-BC-284 / BC-284: Ràng buộc điểm thu quỹ với Station/ca

- [ ] **T-BC-284-CONTRACT** — Chốt operation «Ràng buộc điểm thu quỹ với Station/ca»: allowed actor R12, guard «Chỉ Station được giao, actor và thời gian có chứng cứ; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource COD liability/collection/remittance, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-284-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-284-DB** — Review forward schema/index/constraint của finance cho COD liability/collection/remittance nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-284-BE** — Implement «Ràng buộc điểm thu quỹ với Station/ca» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-284-UI** — Tích hợp Internal scoped channel — Ràng buộc điểm thu quỹ với Station/ca; action «Ràng buộc điểm thu quỹ với Station/ca», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-284-VERIFY** — Chạy testcase riêng TEST-V3-BC-284-FLOW và AC-BC-284-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-284 trong traceability.

## REQ-V3-BC-285 / BC-285: Đối chiếu Delivered Shipment với COD thu thực tế

- [ ] **T-BC-285-CONTRACT** — Chốt operation «Đối chiếu Delivered Shipment với COD thu thực tế»: allowed actor R13, guard «Không suy luận COD từ DELIVERED; phát hiện ngoại lệ; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource COD liability/collection/remittance, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-285-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-285-DB** — Review forward schema/index/constraint của finance cho COD liability/collection/remittance nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-285-BE** — Implement «Đối chiếu Delivered Shipment với COD thu thực tế» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-285-UI** — Tích hợp Internal scoped channel — Đối chiếu Delivered Shipment với COD thu thực tế; action «Đối chiếu Delivered Shipment với COD thu thực tế», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-285-VERIFY** — Chạy testcase riêng TEST-V3-BC-285-FLOW và AC-BC-285-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-285 trong traceability.

## REQ-V3-BC-295 / BC-295: Tiếp nhận tra soát COD và chuyển Finance

- [ ] **T-BC-295-CONTRACT** — Chốt operation «Tiếp nhận tra soát COD và chuyển Finance»: allowed actor R14, guard «Không tự sửa cod_amount; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource COD liability/collection/remittance, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-295-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-295-DB** — Review forward schema/index/constraint của finance cho COD liability/collection/remittance nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-295-BE** — Implement «Tiếp nhận tra soát COD và chuyển Finance» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-295-UI** — Tích hợp Internal scoped channel — Tiếp nhận tra soát COD và chuyển Finance; action «Tiếp nhận tra soát COD và chuyển Finance», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-295-VERIFY** — Chạy testcase riêng TEST-V3-BC-295-FLOW và AC-BC-295-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-295 trong traceability.

## REQ-V3-BC-317 / BC-317: Đọc chuỗi thu-nộp-đối soát COD

- [ ] **T-BC-317-CONTRACT** — Chốt operation «Đọc chuỗi thu-nộp-đối soát COD»: allowed actor R17, guard «Không ghi remittance; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource COD liability/collection/remittance, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-317-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-317-DB** — Review forward schema/index/constraint của finance cho COD liability/collection/remittance nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-317-BE** — Implement «Đọc chuỗi thu-nộp-đối soát COD» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-317-UI** — Tích hợp Internal scoped channel — Đọc chuỗi thu-nộp-đối soát COD; action «Đọc chuỗi thu-nộp-đối soát COD», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-317-VERIFY** — Chạy testcase riêng TEST-V3-BC-317-FLOW và AC-BC-317-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-317 trong traceability.

## REQ-V3-BC-354 / BC-354: Không được giả xác nhận thu/đối soát/thanh toán COD

- [ ] **T-BC-354-CONTRACT** — Chốt operation «Không được giả xác nhận thu/đối soát/thanh toán COD»: allowed actor R31, guard «Negative permission tests; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource COD liability/collection/remittance, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-354-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-354-DB** — Chứng minh không ghi effect bị cấm; không thêm positive schema/seed.
- [ ] **T-BC-354-BE** — Implement «Không được giả xác nhận thu/đối soát/thanh toán COD» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, deny all unauthorized states, không positive branch.
- [ ] **T-BC-354-UI** — Tích hợp Internal scoped channel — Không được giả xác nhận thu/đối soát/thanh toán COD; không nút cấp hành vi cấm; direct API denial vẫn bắt buộc.
- [ ] **T-BC-354-VERIFY** — Chạy testcase riêng TEST-V3-BC-354-DENY và AC-BC-354-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-354 trong traceability.


- [ ] T-LEGACY-L017: map từng obligation legacy ở spec sang implementation/negative/compatibility testcase, bao gồm phần consumer; không đóng full spec khi requirement legacy còn chưa được chứng minh hoặc disposition rõ.

## Closure toàn Spec

- [ ] T-L017-FULL: từng REQ có AC/design/task/code/API/DB/UI/test/receipt; lint/typecheck/build/test theo app liên quan, required Architecture/H2/PG18 CI exact revision, report/privacy/owned cleanup và backward compatibility giữ nguyên; no unresolved blocking findings.
- [ ] T-L017-ACCEPT: scoped PR integration PASS → Owner merge → regression trên merge SHA → Owner full-spec acceptance riêng. Chưa tick vì docs promotion.


- [ ] **T-AB-RECIPIENT-COD** — REQ-AB-RECIPIENT-COD: close approved operation/state/data/UX/config contract + fixtures; implement owner API/DB/HTTP and integrated Verified collection receipt/channel; unit/PG/security/browser or verified channel assert «COD immutable to recipient; duplicate collection/payment callback once; no DELIVERED→COLLECTED inference, no real money claim»; TEST-AB-RECIPIENT-COD with source+integration receipts. Dependencies L015;L018; do not mark full spec when missing.
