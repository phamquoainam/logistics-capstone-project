# Tasks 023 — bulk-shipment-import

NOT_STARTED; thứ tự là contract/fixture → DB → BE/HTTP → UI/channel → verification → full acceptance. Mỗi BC là gói end-to-end nội bộ, không thay phương pháp Spec-by-Spec. Không task nào DONE trong rebuild docs.

## REQ-V3-BC-029 / BC-029: Nhập đơn hàng loạt CSV/Excel

- [ ] **T-BC-029-CONTRACT** — Chốt operation «Nhập đơn hàng loạt CSV/Excel»: allowed actor R03, guard «Kiểm tra bản ghi trùng và tính hợp lệ; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource import job/row/source-to-Shipment reference, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-029-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-029-DB** — Review forward schema/index/constraint của orders cho import job/row/source-to-Shipment reference nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-029-BE** — Implement «Nhập đơn hàng loạt CSV/Excel» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-029-UI** — Tích hợp Merchant current context — Nhập đơn hàng loạt CSV/Excel; action «Nhập đơn hàng loạt CSV/Excel», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-029-VERIFY** — Chạy testcase riêng TEST-V3-BC-029-FLOW và AC-BC-029-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-029 trong traceability.

## REQ-V3-BC-174 / BC-174: Nhập Shipment hàng loạt từ CSV/Excel và xử lý từng dòng lỗi

- [ ] **T-BC-174-CONTRACT** — Chốt operation «Nhập Shipment hàng loạt từ CSV/Excel và xử lý từng dòng lỗi»: allowed actor R03, guard «Idempotency, validation, partial failure report; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource import job/row/source-to-Shipment reference, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-174-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-174-DB** — Review forward schema/index/constraint của orders cho import job/row/source-to-Shipment reference nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-174-BE** — Implement «Nhập Shipment hàng loạt từ CSV/Excel và xử lý từng dòng lỗi» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-174-UI** — Tích hợp Merchant current context — Nhập Shipment hàng loạt từ CSV/Excel và xử lý từng dòng lỗi; action «Nhập Shipment hàng loạt từ CSV/Excel và xử lý từng dòng lỗi», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-174-VERIFY** — Chạy testcase riêng TEST-V3-BC-174-FLOW và AC-BC-174-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-174 trong traceability.


- [ ] T-LEGACY-L023: map từng obligation legacy ở spec sang implementation/negative/compatibility testcase, bao gồm phần consumer; không đóng full spec khi requirement legacy còn chưa được chứng minh hoặc disposition rõ.

## Closure toàn Spec

- [ ] T-L023-FULL: từng REQ có AC/design/task/code/API/DB/UI/test/receipt; lint/typecheck/build/test theo app liên quan, required Architecture/H2/PG18 CI exact revision, report/privacy/owned cleanup và backward compatibility giữ nguyên; no unresolved blocking findings.
- [ ] T-L023-ACCEPT: scoped PR integration PASS → Owner merge → regression trên merge SHA → Owner full-spec acceptance riêng. Chưa tick vì docs promotion.


- [ ] **T-AB-BULK** — REQ-AB-BULK: close approved operation/state/data/UX/config contract + fixtures; implement owner API/DB/HTTP and integrated Upload/template/tabular preview/correction/history/progress/results; unit/PG/security/browser or verified channel assert «Proposed per-row atomic DRAFT creation after explicit commit, visible partial outcomes (DEC22 review required); stable job+row revision/source fingerprint/business ref; replay/reorder/retry no duplicate; export formula injection protected; no auto confirm/charge»; TEST-AB-BULK with source+integration receipts. Dependencies L003;L004;L007; do not mark full spec when missing.
