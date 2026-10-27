# Tasks 019 — cod-reconciliation-merchant-settlement

NOT_STARTED; thứ tự là contract/fixture → DB → BE/HTTP → UI/channel → verification → full acceptance. Mỗi BC là gói end-to-end nội bộ, không thay phương pháp Spec-by-Spec. Không task nào DONE trong rebuild docs.

## REQ-V3-BC-015 / BC-015: Xem phí, bảng kê và settlement

- [ ] **T-BC-015-CONTRACT** — Chốt operation «Xem phí, bảng kê và settlement»: allowed actor R02, guard «Chỉ thuộc Merchant; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource statement/closed-period snapshot/payout evidence, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-015-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-015-DB** — Review forward schema/index/constraint của finance cho statement/closed-period snapshot/payout evidence nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-015-BE** — Implement «Xem phí, bảng kê và settlement» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-015-UI** — Tích hợp Merchant current context — Xem phí, bảng kê và settlement; action «Xem phí, bảng kê và settlement», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-015-VERIFY** — Chạy testcase riêng TEST-V3-BC-015-FLOW và AC-BC-015-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-015 trong traceability.

## REQ-V3-BC-087 / BC-087: Xem bảng kê đối soát và thanh toán

- [ ] **T-BC-087-CONTRACT** — Chốt operation «Xem bảng kê đối soát và thanh toán»: allowed actor R11, guard «Không sửa sổ cái; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource statement/closed-period snapshot/payout evidence, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-087-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-087-DB** — Review forward schema/index/constraint của finance cho statement/closed-period snapshot/payout evidence nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-087-BE** — Implement «Xem bảng kê đối soát và thanh toán» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-087-UI** — Tích hợp Merchant current context — Xem bảng kê đối soát và thanh toán; action «Xem bảng kê đối soát và thanh toán», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-087-VERIFY** — Chạy testcase riêng TEST-V3-BC-087-FLOW và AC-BC-087-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-087 trong traceability.

## REQ-V3-BC-091 / BC-091: Đối chiếu/ghi nhận xác nhận bảng kê

- [ ] **T-BC-091-CONTRACT** — Chốt operation «Đối chiếu/ghi nhận xác nhận bảng kê»: allowed actor R11, guard «Không tự quyết toán phía Logistics; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource statement/closed-period snapshot/payout evidence, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-091-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-091-DB** — Review forward schema/index/constraint của finance cho statement/closed-period snapshot/payout evidence nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-091-BE** — Implement «Đối chiếu/ghi nhận xác nhận bảng kê» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-091-UI** — Tích hợp Merchant current context — Đối chiếu/ghi nhận xác nhận bảng kê; action «Đối chiếu/ghi nhận xác nhận bảng kê», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-091-VERIFY** — Chạy testcase riêng TEST-V3-BC-091-FLOW và AC-BC-091-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-091 trong traceability.

## REQ-V3-BC-099 / BC-099: Tạo bảng kê và kỳ thanh toán Merchant

- [ ] **T-BC-099-CONTRACT** — Chốt operation «Tạo bảng kê và kỳ thanh toán Merchant»: allowed actor R13, guard «Có quyền phê duyệt phù hợp; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource statement/closed-period snapshot/payout evidence, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-099-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-099-DB** — Review forward schema/index/constraint của finance cho statement/closed-period snapshot/payout evidence nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-099-BE** — Implement «Tạo bảng kê và kỳ thanh toán Merchant» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-099-UI** — Tích hợp Internal scoped channel — Tạo bảng kê và kỳ thanh toán Merchant; action «Tạo bảng kê và kỳ thanh toán Merchant», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-099-VERIFY** — Chạy testcase riêng TEST-V3-BC-099-FLOW và AC-BC-099-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-099 trong traceability.

## REQ-V3-BC-100 / BC-100: Ghi nhận settlement đã thực hiện

- [ ] **T-BC-100-CONTRACT** — Chốt operation «Ghi nhận settlement đã thực hiện»: allowed actor R13, guard «Không giả định đã chuyển tiền khi chưa có bằng chứng; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource statement/closed-period snapshot/payout evidence, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-100-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-100-DB** — Review forward schema/index/constraint của finance cho statement/closed-period snapshot/payout evidence nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-100-BE** — Implement «Ghi nhận settlement đã thực hiện» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-100-UI** — Tích hợp Internal scoped channel — Ghi nhận settlement đã thực hiện; action «Ghi nhận settlement đã thực hiện», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-100-VERIFY** — Chạy testcase riêng TEST-V3-BC-100-FLOW và AC-BC-100-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-100 trong traceability.

## REQ-V3-BC-162 / BC-162: Xem lịch thanh toán, hóa đơn và chứng từ COD/cước

- [ ] **T-BC-162-CONTRACT** — Chốt operation «Xem lịch thanh toán, hóa đơn và chứng từ COD/cước»: allowed actor R02, guard «Chỉ Merchant được sở hữu; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource statement/closed-period snapshot/payout evidence, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-162-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-162-DB** — Review forward schema/index/constraint của finance cho statement/closed-period snapshot/payout evidence nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-162-BE** — Implement «Xem lịch thanh toán, hóa đơn và chứng từ COD/cước» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-162-UI** — Tích hợp Merchant current context — Xem lịch thanh toán, hóa đơn và chứng từ COD/cước; action «Xem lịch thanh toán, hóa đơn và chứng từ COD/cước», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-162-VERIFY** — Chạy testcase riêng TEST-V3-BC-162-FLOW và AC-BC-162-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-162 trong traceability.

## REQ-V3-BC-188 / BC-188: Đối soát Merchant với bảng kê hãng và công nợ phải thu/phải trả

- [ ] **T-BC-188-CONTRACT** — Chốt operation «Đối soát Merchant với bảng kê hãng và công nợ phải thu/phải trả»: allowed actor R11, guard «Ledger/statement không được sửa âm thầm; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource statement/closed-period snapshot/payout evidence, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-188-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-188-DB** — Review forward schema/index/constraint của finance cho statement/closed-period snapshot/payout evidence nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-188-BE** — Implement «Đối soát Merchant với bảng kê hãng và công nợ phải thu/phải trả» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-188-UI** — Tích hợp Merchant current context — Đối soát Merchant với bảng kê hãng và công nợ phải thu/phải trả; action «Đối soát Merchant với bảng kê hãng và công nợ phải thu/phải trả», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-188-VERIFY** — Chạy testcase riêng TEST-V3-BC-188-FLOW và AC-BC-188-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-188 trong traceability.

## REQ-V3-BC-190 / BC-190: Kiểm tra chuyển COD thực nhận, lịch thanh toán và tải chứng từ

- [ ] **T-BC-190-CONTRACT** — Chốt operation «Kiểm tra chuyển COD thực nhận, lịch thanh toán và tải chứng từ»: allowed actor R11, guard «Không tự xác nhận hãng đã trả; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource statement/closed-period snapshot/payout evidence, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-190-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-190-DB** — Review forward schema/index/constraint của finance cho statement/closed-period snapshot/payout evidence nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-190-BE** — Implement «Kiểm tra chuyển COD thực nhận, lịch thanh toán và tải chứng từ» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-190-UI** — Tích hợp Merchant current context — Kiểm tra chuyển COD thực nhận, lịch thanh toán và tải chứng từ; action «Kiểm tra chuyển COD thực nhận, lịch thanh toán và tải chứng từ», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-190-VERIFY** — Chạy testcase riêng TEST-V3-BC-190-FLOW và AC-BC-190-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-190 trong traceability.

## REQ-V3-BC-287 / BC-287: Xác định COD đủ điều kiện trả Merchant

- [ ] **T-BC-287-CONTRACT** — Chốt operation «Xác định COD đủ điều kiện trả Merchant»: allowed actor R13, guard «Theo hold, fees, disputes, policy, approvals; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource statement/closed-period snapshot/payout evidence, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-287-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-287-DB** — Review forward schema/index/constraint của finance cho statement/closed-period snapshot/payout evidence nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-287-BE** — Implement «Xác định COD đủ điều kiện trả Merchant» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-287-UI** — Tích hợp Internal scoped channel — Xác định COD đủ điều kiện trả Merchant; action «Xác định COD đủ điều kiện trả Merchant», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-287-VERIFY** — Chạy testcase riêng TEST-V3-BC-287-FLOW và AC-BC-287-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-287 trong traceability.


- [ ] T-LEGACY-L019: map từng obligation legacy ở spec sang implementation/negative/compatibility testcase, bao gồm phần consumer; không đóng full spec khi requirement legacy còn chưa được chứng minh hoặc disposition rõ.

## Closure toàn Spec

- [ ] T-L019-FULL: từng REQ có AC/design/task/code/API/DB/UI/test/receipt; lint/typecheck/build/test theo app liên quan, required Architecture/H2/PG18 CI exact revision, report/privacy/owned cleanup và backward compatibility giữ nguyên; no unresolved blocking findings.
- [ ] T-L019-ACCEPT: scoped PR integration PASS → Owner merge → regression trên merge SHA → Owner full-spec acceptance riêng. Chưa tick vì docs promotion.


- [ ] **T-AB-FINANCIAL-PORTAL** — REQ-AB-FINANCIAL-PORTAL: close approved operation/state/data/UX/config contract + fixtures; implement owner API/DB/HTTP and integrated COD statements/payout schedules/discrepancy; unit/PG/security/browser or verified channel assert «Statement totals tie to immutable ledger/allocation/eligible closed snapshots; Merchant acknowledgment not carrier settlement; late data new adjustment; sandbox payout accurately labeled»; TEST-AB-FINANCIAL-PORTAL with source+integration receipts. Dependencies L017;L018;L020; do not mark full spec when missing.
