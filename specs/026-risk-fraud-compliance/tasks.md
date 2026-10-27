# Tasks 026 — risk-fraud-compliance

NOT_STARTED; thứ tự là contract/fixture → DB → BE/HTTP → UI/channel → verification → full acceptance. Mỗi BC là gói end-to-end nội bộ, không thay phương pháp Spec-by-Spec. Không task nào DONE trong rebuild docs.

## REQ-V3-BC-337 / BC-337: Điều tra dấu hiệu gian lận COD

- [ ] **T-BC-337-CONTRACT** — Chốt operation «Điều tra dấu hiệu gian lận COD»: allowed actor R30, guard «Read theo case và dấu vết tiền; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource risk/hold case, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-337-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-337-DB** — Review forward schema/index/constraint của orders cho risk/hold case nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-337-BE** — Implement «Điều tra dấu hiệu gian lận COD» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-337-UI** — Tích hợp Internal scoped channel — Điều tra dấu hiệu gian lận COD; action «Điều tra dấu hiệu gian lận COD», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-337-VERIFY** — Chạy testcase riêng TEST-V3-BC-337-FLOW và AC-BC-337-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-337 trong traceability.

## REQ-V3-BC-338 / BC-338: Phát hiện và điều tra tài khoản sử dụng trái phép

- [ ] **T-BC-338-CONTRACT** — Chốt operation «Phát hiện và điều tra tài khoản sử dụng trái phép»: allowed actor R30, guard «Không tự cấp quyền mới; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource risk/hold case, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-338-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-338-DB** — Review forward schema/index/constraint của orders cho risk/hold case nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-338-BE** — Implement «Phát hiện và điều tra tài khoản sử dụng trái phép» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-338-UI** — Tích hợp Internal scoped channel — Phát hiện và điều tra tài khoản sử dụng trái phép; action «Phát hiện và điều tra tài khoản sử dụng trái phép», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-338-VERIFY** — Chạy testcase riêng TEST-V3-BC-338-FLOW và AC-BC-338-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-338 trong traceability.

## REQ-V3-BC-339 / BC-339: Phát hiện spam hoặc tạo Shipment hàng loạt bất thường

- [ ] **T-BC-339-CONTRACT** — Chốt operation «Phát hiện spam hoặc tạo Shipment hàng loạt bất thường»: allowed actor R30, guard «Rate, fingerprint, scope, false positives; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource risk/hold case, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-339-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-339-DB** — Review forward schema/index/constraint của orders cho risk/hold case nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-339-BE** — Implement «Phát hiện spam hoặc tạo Shipment hàng loạt bất thường» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-339-UI** — Tích hợp Internal scoped channel — Phát hiện spam hoặc tạo Shipment hàng loạt bất thường; action «Phát hiện spam hoặc tạo Shipment hàng loạt bất thường», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-339-VERIFY** — Chạy testcase riêng TEST-V3-BC-339-FLOW và AC-BC-339-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-339 trong traceability.

## REQ-V3-BC-340 / BC-340: Kiểm tra thông tin Recipient có dấu hiệu giả

- [ ] **T-BC-340-CONTRACT** — Chốt operation «Kiểm tra thông tin Recipient có dấu hiệu giả»: allowed actor R30, guard «PII minimization, authorization; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource risk/hold case, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-340-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-340-DB** — Review forward schema/index/constraint của orders cho risk/hold case nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-340-BE** — Implement «Kiểm tra thông tin Recipient có dấu hiệu giả» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-340-UI** — Tích hợp Internal scoped channel — Kiểm tra thông tin Recipient có dấu hiệu giả; action «Kiểm tra thông tin Recipient có dấu hiệu giả», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-340-VERIFY** — Chạy testcase riêng TEST-V3-BC-340-FLOW và AC-BC-340-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-340 trong traceability.

## REQ-V3-BC-341 / BC-341: Đánh dấu kiện có dấu hiệu rủi ro

- [ ] **T-BC-341-CONTRACT** — Chốt operation «Đánh dấu kiện có dấu hiệu rủi ro»: allowed actor R30, guard «Case, evidence và custody không bị sửa; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource risk/hold case, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-341-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-341-DB** — Review forward schema/index/constraint của orders cho risk/hold case nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-341-BE** — Implement «Đánh dấu kiện có dấu hiệu rủi ro» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-341-UI** — Tích hợp Internal scoped channel — Đánh dấu kiện có dấu hiệu rủi ro; action «Đánh dấu kiện có dấu hiệu rủi ro», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-341-VERIFY** — Chạy testcase riêng TEST-V3-BC-341-FLOW và AC-BC-341-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-341 trong traceability.

## REQ-V3-BC-342 / BC-342: Điều tra truy cập dữ liệu trái phép

- [ ] **T-BC-342-CONTRACT** — Chốt operation «Điều tra truy cập dữ liệu trái phép»: allowed actor R30, guard «Audit bất biến, nguyên tắc tối thiểu; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource risk/hold case, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-342-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-342-DB** — Review forward schema/index/constraint của orders cho risk/hold case nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-342-BE** — Implement «Điều tra truy cập dữ liệu trái phép» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-342-UI** — Tích hợp Internal scoped channel — Điều tra truy cập dữ liệu trái phép; action «Điều tra truy cập dữ liệu trái phép», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-342-VERIFY** — Chạy testcase riêng TEST-V3-BC-342-FLOW và AC-BC-342-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-342 trong traceability.

## REQ-V3-BC-343 / BC-343: Đề xuất tạm giữ Shipment theo chính sách

- [ ] **T-BC-343-CONTRACT** — Chốt operation «Đề xuất tạm giữ Shipment theo chính sách»: allowed actor R30, guard «Không thay đổi silent status; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource risk/hold case, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-343-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-343-DB** — Review forward schema/index/constraint của orders cho risk/hold case nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-343-BE** — Implement «Đề xuất tạm giữ Shipment theo chính sách» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-343-UI** — Tích hợp Internal scoped channel — Đề xuất tạm giữ Shipment theo chính sách; action «Đề xuất tạm giữ Shipment theo chính sách», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-343-VERIFY** — Chạy testcase riêng TEST-V3-BC-343-FLOW và AC-BC-343-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-343 trong traceability.

## REQ-V3-BC-344 / BC-344: Chuyển hồ sơ rủi ro sang thẩm định/phê duyệt

- [ ] **T-BC-344-CONTRACT** — Chốt operation «Chuyển hồ sơ rủi ro sang thẩm định/phê duyệt»: allowed actor R30, guard «Separation of duties; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource risk/hold case, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-344-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-344-DB** — Review forward schema/index/constraint của orders cho risk/hold case nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-344-BE** — Implement «Chuyển hồ sơ rủi ro sang thẩm định/phê duyệt» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-344-UI** — Tích hợp Internal scoped channel — Chuyển hồ sơ rủi ro sang thẩm định/phê duyệt; action «Chuyển hồ sơ rủi ro sang thẩm định/phê duyệt», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-344-VERIFY** — Chạy testcase riêng TEST-V3-BC-344-FLOW và AC-BC-344-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-344 trong traceability.

## REQ-V3-BC-345 / BC-345: Phê duyệt giải phóng Shipment theo thẩm quyền

- [ ] **T-BC-345-CONTRACT** — Chốt operation «Phê duyệt giải phóng Shipment theo thẩm quyền»: allowed actor R30, guard «Dual control/hạn mức khi cần; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource risk/hold case, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-345-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-345-DB** — Review forward schema/index/constraint của orders cho risk/hold case nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-345-BE** — Implement «Phê duyệt giải phóng Shipment theo thẩm quyền» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-345-UI** — Tích hợp Internal scoped channel — Phê duyệt giải phóng Shipment theo thẩm quyền; action «Phê duyệt giải phóng Shipment theo thẩm quyền», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-345-VERIFY** — Chạy testcase riêng TEST-V3-BC-345-FLOW và AC-BC-345-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-345 trong traceability.

## REQ-V3-BC-346 / BC-346: Xem audit và giao dịch bất thường theo case

- [ ] **T-BC-346-CONTRACT** — Chốt operation «Xem audit và giao dịch bất thường theo case»: allowed actor R30, guard «Mọi lần truy cập nhạy cảm có log; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource risk/hold case, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-346-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-346-DB** — Review forward schema/index/constraint của orders cho risk/hold case nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-346-BE** — Implement «Xem audit và giao dịch bất thường theo case» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-346-UI** — Tích hợp Internal scoped channel — Xem audit và giao dịch bất thường theo case; action «Xem audit và giao dịch bất thường theo case», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-346-VERIFY** — Chạy testcase riêng TEST-V3-BC-346-FLOW và AC-BC-346-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-346 trong traceability.

## REQ-V3-BC-347 / BC-347: Điều tra giao dịch tài chính bất thường

- [ ] **T-BC-347-CONTRACT** — Chốt operation «Điều tra giao dịch tài chính bất thường»: allowed actor R30, guard «Không tự sửa ledger; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource risk/hold case, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-347-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-347-DB** — Review forward schema/index/constraint của orders cho risk/hold case nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-347-BE** — Implement «Điều tra giao dịch tài chính bất thường» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-347-UI** — Tích hợp Internal scoped channel — Điều tra giao dịch tài chính bất thường; action «Điều tra giao dịch tài chính bất thường», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-347-VERIFY** — Chạy testcase riêng TEST-V3-BC-347-FLOW và AC-BC-347-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-347 trong traceability.


- [ ] T-LEGACY-L026: map từng obligation legacy ở spec sang implementation/negative/compatibility testcase, bao gồm phần consumer; không đóng full spec khi requirement legacy còn chưa được chứng minh hoặc disposition rõ.

## Closure toàn Spec

- [ ] T-L026-FULL: từng REQ có AC/design/task/code/API/DB/UI/test/receipt; lint/typecheck/build/test theo app liên quan, required Architecture/H2/PG18 CI exact revision, report/privacy/owned cleanup và backward compatibility giữ nguyên; no unresolved blocking findings.
- [ ] T-L026-ACCEPT: scoped PR integration PASS → Owner merge → regression trên merge SHA → Owner full-spec acceptance riêng. Chưa tick vì docs promotion.
