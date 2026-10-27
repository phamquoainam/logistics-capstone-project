# Tasks 006 — order-shipment-parcel-contract

NOT_STARTED; thứ tự là contract/fixture → DB → BE/HTTP → UI/channel → verification → full acceptance. Mỗi BC là gói end-to-end nội bộ, không thay phương pháp Spec-by-Spec. Không task nào DONE trong rebuild docs.

## REQ-V3-BC-034 / BC-034: Giám sát Shipment và ngoại lệ

- [ ] **T-BC-034-CONTRACT** — Chốt operation «Giám sát Shipment và ngoại lệ»: allowed actor R04, guard «Không giả lập DELIVERED; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource Order/Shipment/Parcel/snapshots, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-034-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-034-DB** — Review forward schema/index/constraint của orders cho Order/Shipment/Parcel/snapshots nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-034-BE** — Implement «Giám sát Shipment và ngoại lệ» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-034-UI** — Tích hợp Internal scoped channel — Giám sát Shipment và ngoại lệ; action «Giám sát Shipment và ngoại lệ», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-034-VERIFY** — Chạy testcase riêng TEST-V3-BC-034-FLOW và AC-BC-034-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-034 trong traceability.


- [ ] T-LEGACY-L006: map từng obligation legacy ở spec sang implementation/negative/compatibility testcase, bao gồm phần consumer; không đóng full spec khi requirement legacy còn chưa được chứng minh hoặc disposition rõ.

## Closure toàn Spec

- [ ] T-L006-FULL: từng REQ có AC/design/task/code/API/DB/UI/test/receipt; lint/typecheck/build/test theo app liên quan, required Architecture/H2/PG18 CI exact revision, report/privacy/owned cleanup và backward compatibility giữ nguyên; no unresolved blocking findings.
- [ ] T-L006-ACCEPT: scoped PR integration PASS → Owner merge → regression trên merge SHA → Owner full-spec acceptance riêng. Chưa tick vì docs promotion.


- [ ] **T-AB-SHIPPING-FACTS** — REQ-AB-SHIPPING-FACTS: close approved operation/state/data/UX/config contract + fixtures; implement owner API/DB/HTTP and integrated Single-shipment form/detail facts; unit/PG/security/browser or verified channel assert «Invalid quantity/unit/measurement/unsupported handling/service fail safely; forbidden goods blocked; versioned snapshots survive profile/catalog edits; no direct status patch»; TEST-AB-SHIPPING-FACTS with source+integration receipts. Dependencies L003;L004;L005; do not mark full spec when missing.
