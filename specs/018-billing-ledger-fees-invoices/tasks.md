# Tasks 018 — billing-ledger-fees-invoices

NOT_STARTED; thứ tự là contract/fixture → DB → BE/HTTP → UI/channel → verification → full acceptance. Mỗi BC là gói end-to-end nội bộ, không thay phương pháp Spec-by-Spec. Không task nào DONE trong rebuild docs.

## REQ-V3-BC-088 / BC-088: Tra cứu phí và phụ phí theo Shipment

- [ ] **T-BC-088-CONTRACT** — Chốt operation «Tra cứu phí và phụ phí theo Shipment»: allowed actor R11, guard «Tôn trọng snapshot giá; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource ledger/payment/charge/invoice/receipt, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-088-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-088-DB** — Review forward schema/index/constraint của finance cho ledger/payment/charge/invoice/receipt nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-088-BE** — Implement «Tra cứu phí và phụ phí theo Shipment» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-088-UI** — Tích hợp Merchant current context — Tra cứu phí và phụ phí theo Shipment; action «Tra cứu phí và phụ phí theo Shipment», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-088-VERIFY** — Chạy testcase riêng TEST-V3-BC-088-FLOW và AC-BC-088-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-088 trong traceability.

## REQ-V3-BC-101 / BC-101: Tính phí và công nợ phải thu/phải trả

- [ ] **T-BC-101-CONTRACT** — Chốt operation «Tính phí và công nợ phải thu/phải trả»: allowed actor R13, guard «Snapshot giá + audit; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource ledger/payment/charge/invoice/receipt, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-101-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-101-DB** — Review forward schema/index/constraint của finance cho ledger/payment/charge/invoice/receipt nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-101-BE** — Implement «Tính phí và công nợ phải thu/phải trả» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-101-UI** — Tích hợp Internal scoped channel — Tính phí và công nợ phải thu/phải trả; action «Tính phí và công nợ phải thu/phải trả», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-101-VERIFY** — Chạy testcase riêng TEST-V3-BC-101-FLOW và AC-BC-101-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-101 trong traceability.

## REQ-V3-BC-102 / BC-102: Audit điều chỉnh tài chính

- [ ] **T-BC-102-CONTRACT** — Chốt operation «Audit điều chỉnh tài chính»: allowed actor R13, guard «Không sửa lịch sử; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource ledger/payment/charge/invoice/receipt, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-102-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-102-DB** — Review forward schema/index/constraint của finance cho ledger/payment/charge/invoice/receipt nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-102-BE** — Implement «Audit điều chỉnh tài chính» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-102-UI** — Tích hợp Internal scoped channel — Audit điều chỉnh tài chính; action «Audit điều chỉnh tài chính», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-102-VERIFY** — Chạy testcase riêng TEST-V3-BC-102-FLOW và AC-BC-102-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-102 trong traceability.

## REQ-V3-BC-103 / BC-103: Refund và xử lý khiếu nại tài chính

- [ ] **T-BC-103-CONTRACT** — Chốt operation «Refund và xử lý khiếu nại tài chính»: allowed actor R13, guard «Phải được phê duyệt; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource ledger/payment/charge/invoice/receipt, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-103-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-103-DB** — Review forward schema/index/constraint của finance cho ledger/payment/charge/invoice/receipt nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-103-BE** — Implement «Refund và xử lý khiếu nại tài chính» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-103-UI** — Tích hợp Internal scoped channel — Refund và xử lý khiếu nại tài chính; action «Refund và xử lý khiếu nại tài chính», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-103-VERIFY** — Chạy testcase riêng TEST-V3-BC-103-FLOW và AC-BC-103-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-103 trong traceability.

## REQ-V3-BC-104 / BC-104: Tích hợp cổng thanh toán/ngân hàng

- [ ] **T-BC-104-CONTRACT** — Chốt operation «Tích hợp cổng thanh toán/ngân hàng»: allowed actor R13, guard «Không yêu cầu thanh toán thật trong demo; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource ledger/payment/charge/invoice/receipt, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-104-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-104-DB** — Review forward schema/index/constraint của finance cho ledger/payment/charge/invoice/receipt nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-104-BE** — Implement «Tích hợp cổng thanh toán/ngân hàng» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-104-UI** — Tích hợp Internal scoped channel — Tích hợp cổng thanh toán/ngân hàng; action «Tích hợp cổng thanh toán/ngân hàng», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-104-VERIFY** — Chạy testcase riêng TEST-V3-BC-104-FLOW và AC-BC-104-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-104 trong traceability.

## REQ-V3-BC-116 / BC-116: Đề xuất bồi thường/hoàn tiền

- [ ] **T-BC-116-CONTRACT** — Chốt operation «Đề xuất bồi thường/hoàn tiền»: allowed actor R15, guard «Không tự thực hiện settlement; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource ledger/payment/charge/invoice/receipt, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-116-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-116-DB** — Review forward schema/index/constraint của finance cho ledger/payment/charge/invoice/receipt nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-116-BE** — Implement «Đề xuất bồi thường/hoàn tiền» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-116-UI** — Tích hợp Internal scoped channel — Đề xuất bồi thường/hoàn tiền; action «Đề xuất bồi thường/hoàn tiền», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-116-VERIFY** — Chạy testcase riêng TEST-V3-BC-116-FLOW và AC-BC-116-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-116 trong traceability.

## REQ-V3-BC-142 / BC-142: Thanh toán phí vận chuyển theo phương thức hỗ trợ

- [ ] **T-BC-142-CONTRACT** — Chốt operation «Thanh toán phí vận chuyển theo phương thức hỗ trợ»: allowed actor R18, guard «Receipt, price snapshot và chống thu trùng; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource ledger/payment/charge/invoice/receipt, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-142-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-142-DB** — Review forward schema/index/constraint của finance cho ledger/payment/charge/invoice/receipt nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-142-BE** — Implement «Thanh toán phí vận chuyển theo phương thức hỗ trợ» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-142-UI** — Tích hợp Personal — Thanh toán phí vận chuyển theo phương thức hỗ trợ; action «Thanh toán phí vận chuyển theo phương thức hỗ trợ», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-142-VERIFY** — Chạy testcase riêng TEST-V3-BC-142-FLOW và AC-BC-142-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-142 trong traceability.

## REQ-V3-BC-148 / BC-148: Xem/tải hóa đơn và biên nhận

- [ ] **T-BC-148-CONTRACT** — Chốt operation «Xem/tải hóa đơn và biên nhận»: allowed actor R18, guard «Hợp lệ với hình thức thanh toán, không rò dữ liệu; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource ledger/payment/charge/invoice/receipt, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-148-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-148-DB** — Review forward schema/index/constraint của finance cho ledger/payment/charge/invoice/receipt nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-148-BE** — Implement «Xem/tải hóa đơn và biên nhận» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-148-UI** — Tích hợp Personal — Xem/tải hóa đơn và biên nhận; action «Xem/tải hóa đơn và biên nhận», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-148-VERIFY** — Chạy testcase riêng TEST-V3-BC-148-FLOW và AC-BC-148-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-148 trong traceability.

## REQ-V3-BC-151 / BC-151: Ghi nhận phí/thanh toán ký gửi tại quầy theo policy

- [ ] **T-BC-151-CONTRACT** — Chốt operation «Ghi nhận phí/thanh toán ký gửi tại quầy theo policy»: allowed actor R08, guard «Không tự xác nhận thanh toán thiếu bằng chứng; sổ thu/receipt; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource ledger/payment/charge/invoice/receipt, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-151-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-151-DB** — Review forward schema/index/constraint của finance cho ledger/payment/charge/invoice/receipt nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-151-BE** — Implement «Ghi nhận phí/thanh toán ký gửi tại quầy theo policy» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-151-UI** — Tích hợp Internal scoped channel — Ghi nhận phí/thanh toán ký gửi tại quầy theo policy; action «Ghi nhận phí/thanh toán ký gửi tại quầy theo policy», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-151-VERIFY** — Chạy testcase riêng TEST-V3-BC-151-FLOW và AC-BC-151-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-151 trong traceability.

## REQ-V3-BC-286 / BC-286: Quản lý công nợ phải thu và phải trả

- [ ] **T-BC-286-CONTRACT** — Chốt operation «Quản lý công nợ phải thu và phải trả»: allowed actor R13, guard «Ledger entries append-only; tính toán có căn cứ; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource ledger/payment/charge/invoice/receipt, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-286-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-286-DB** — Review forward schema/index/constraint của finance cho ledger/payment/charge/invoice/receipt nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-286-BE** — Implement «Quản lý công nợ phải thu và phải trả» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-286-UI** — Tích hợp Internal scoped channel — Quản lý công nợ phải thu và phải trả; action «Quản lý công nợ phải thu và phải trả», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-286-VERIFY** — Chạy testcase riêng TEST-V3-BC-286-FLOW và AC-BC-286-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-286 trong traceability.

## REQ-V3-BC-288 / BC-288: Thực hiện hoàn tiền đã được phê duyệt

- [ ] **T-BC-288-CONTRACT** — Chốt operation «Thực hiện hoàn tiền đã được phê duyệt»: allowed actor R13, guard «Idempotency, kiểm soát hai người khi cần; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource ledger/payment/charge/invoice/receipt, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-288-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-288-DB** — Review forward schema/index/constraint của finance cho ledger/payment/charge/invoice/receipt nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-288-BE** — Implement «Thực hiện hoàn tiền đã được phê duyệt» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-288-UI** — Tích hợp Internal scoped channel — Thực hiện hoàn tiền đã được phê duyệt; action «Thực hiện hoàn tiền đã được phê duyệt», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-288-VERIFY** — Chạy testcase riêng TEST-V3-BC-288-FLOW và AC-BC-288-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-288 trong traceability.

## REQ-V3-BC-289 / BC-289: Phát hiện và từ chối thanh toán hoặc refund trùng

- [ ] **T-BC-289-CONTRACT** — Chốt operation «Phát hiện và từ chối thanh toán hoặc refund trùng»: allowed actor R13, guard «Idempotency key, unique reference và reconciliation; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource ledger/payment/charge/invoice/receipt, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-289-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-289-DB** — Review forward schema/index/constraint của finance cho ledger/payment/charge/invoice/receipt nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-289-BE** — Implement «Phát hiện và từ chối thanh toán hoặc refund trùng» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-289-UI** — Tích hợp Internal scoped channel — Phát hiện và từ chối thanh toán hoặc refund trùng; action «Phát hiện và từ chối thanh toán hoặc refund trùng», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-289-VERIFY** — Chạy testcase riêng TEST-V3-BC-289-FLOW và AC-BC-289-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-289 trong traceability.

## REQ-V3-BC-303 / BC-303: Theo dõi thanh toán bồi thường/refund đã phê duyệt

- [ ] **T-BC-303-CONTRACT** — Chốt operation «Theo dõi thanh toán bồi thường/refund đã phê duyệt»: allowed actor R15, guard «Không giả lập payment confirmation; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource ledger/payment/charge/invoice/receipt, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-303-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-303-DB** — Review forward schema/index/constraint của finance cho ledger/payment/charge/invoice/receipt nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-303-BE** — Implement «Theo dõi thanh toán bồi thường/refund đã phê duyệt» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-303-UI** — Tích hợp Internal scoped channel — Theo dõi thanh toán bồi thường/refund đã phê duyệt; action «Theo dõi thanh toán bồi thường/refund đã phê duyệt», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-303-VERIFY** — Chạy testcase riêng TEST-V3-BC-303-FLOW và AC-BC-303-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-303 trong traceability.

## REQ-V3-BC-316 / BC-316: Đọc ledger và lịch sử điều chỉnh tài chính

- [ ] **T-BC-316-CONTRACT** — Chốt operation «Đọc ledger và lịch sử điều chỉnh tài chính»: allowed actor R17, guard «PII redaction và scope; read-only; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource ledger/payment/charge/invoice/receipt, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-316-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-316-DB** — Review forward schema/index/constraint của finance cho ledger/payment/charge/invoice/receipt nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-316-BE** — Implement «Đọc ledger và lịch sử điều chỉnh tài chính» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-316-UI** — Tích hợp Internal scoped channel — Đọc ledger và lịch sử điều chỉnh tài chính; action «Đọc ledger và lịch sử điều chỉnh tài chính», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-316-VERIFY** — Chạy testcase riêng TEST-V3-BC-316-FLOW và AC-BC-316-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-316 trong traceability.

## REQ-V3-BC-327 / BC-327: Cung cấp dữ liệu cước cho hóa đơn và đối soát

- [ ] **T-BC-327-CONTRACT** — Chốt operation «Cung cấp dữ liệu cước cho hóa đơn và đối soát»: allowed actor R28, guard «Không trực tiếp sửa ledger/số dư; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource ledger/payment/charge/invoice/receipt, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-327-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-327-DB** — Review forward schema/index/constraint của finance cho ledger/payment/charge/invoice/receipt nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-327-BE** — Implement «Cung cấp dữ liệu cước cho hóa đơn và đối soát» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-327-UI** — Tích hợp Internal scoped channel — Cung cấp dữ liệu cước cho hóa đơn và đối soát; action «Cung cấp dữ liệu cước cho hóa đơn và đối soát», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-327-VERIFY** — Chạy testcase riêng TEST-V3-BC-327-FLOW và AC-BC-327-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-327 trong traceability.


- [ ] T-LEGACY-L018: map từng obligation legacy ở spec sang implementation/negative/compatibility testcase, bao gồm phần consumer; không đóng full spec khi requirement legacy còn chưa được chứng minh hoặc disposition rõ.

## Closure toàn Spec

- [ ] T-L018-FULL: từng REQ có AC/design/task/code/API/DB/UI/test/receipt; lint/typecheck/build/test theo app liên quan, required Architecture/H2/PG18 CI exact revision, report/privacy/owned cleanup và backward compatibility giữ nguyên; no unresolved blocking findings.
- [ ] T-L018-ACCEPT: scoped PR integration PASS → Owner merge → regression trên merge SHA → Owner full-spec acceptance riêng. Chưa tick vì docs promotion.


- [ ] **T-AB-PAYMENTS-RECEIPTS** — REQ-AB-PAYMENTS-RECEIPTS: close approved operation/state/data/UX/config contract + fixtures; implement owner API/DB/HTTP and integrated Payments/receipts/invoices/adjustment/refund; unit/PG/security/browser or verified channel assert «No real bank/payment execution implied; callback verified/unknown handling, duplicate refund/charge denied; personal/tenant invoice access scoped; invoice policy before issue»; TEST-AB-PAYMENTS-RECEIPTS with source+integration receipts. Dependencies L005;L006;L007; do not mark full spec when missing.
