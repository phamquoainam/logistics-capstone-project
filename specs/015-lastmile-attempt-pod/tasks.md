# Tasks 015 — lastmile-attempt-pod

NOT_STARTED; thứ tự là contract/fixture → DB → BE/HTTP → UI/channel → verification → full acceptance. Mỗi BC là gói end-to-end nội bộ, không thay phương pháp Spec-by-Spec. Không task nào DONE trong rebuild docs.

## REQ-V3-BC-073 / BC-073: Ghi DELIVERED kèm POD

- [ ] **T-BC-073-CONTRACT** — Chốt operation «Ghi DELIVERED kèm POD»: allowed actor R09, guard «Bằng chứng hợp lệ; chống trùng; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource delivery attempt/POD/instructions, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-073-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-073-DB** — Review forward schema/index/constraint của orders cho delivery attempt/POD/instructions nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-073-BE** — Implement «Ghi DELIVERED kèm POD» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-073-UI** — Tích hợp Internal scoped channel — Ghi DELIVERED kèm POD; action «Ghi DELIVERED kèm POD», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-073-VERIFY** — Chạy testcase riêng TEST-V3-BC-073-FLOW và AC-BC-073-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-073 trong traceability.

## REQ-V3-BC-074 / BC-074: Ghi DELIVERY_FAILED kèm lý do/attempt

- [ ] **T-BC-074-CONTRACT** — Chốt operation «Ghi DELIVERY_FAILED kèm lý do/attempt»: allowed actor R09, guard «Không xóa thất bại; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource delivery attempt/POD/instructions, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-074-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-074-DB** — Review forward schema/index/constraint của orders cho delivery attempt/POD/instructions nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-074-BE** — Implement «Ghi DELIVERY_FAILED kèm lý do/attempt» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-074-UI** — Tích hợp Internal scoped channel — Ghi DELIVERY_FAILED kèm lý do/attempt; action «Ghi DELIVERY_FAILED kèm lý do/attempt», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-074-VERIFY** — Chạy testcase riêng TEST-V3-BC-074-FLOW và AC-BC-074-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-074 trong traceability.

## REQ-V3-BC-081 / BC-081: Nhận hoặc từ chối kiện; xác nhận POD/OTP khi có

- [ ] **T-BC-081-CONTRACT** — Chốt operation «Nhận hoặc từ chối kiện; xác nhận POD/OTP khi có»: allowed actor R10, guard «Courier ghi nhận; không cấp role IAM; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource delivery attempt/POD/instructions, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-081-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-081-DB** — Review forward schema/index/constraint của orders cho delivery attempt/POD/instructions nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-081-BE** — Implement «Nhận hoặc từ chối kiện; xác nhận POD/OTP khi có» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-081-UI** — Tích hợp Verified Recipient channel — Nhận hoặc từ chối kiện; xác nhận POD/OTP khi có;Courier/CS; public tracking; không Portal/IAM với accepted client/channel; action «Nhận hoặc từ chối kiện; xác nhận POD/OTP khi có», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-081-VERIFY** — Chạy testcase riêng TEST-V3-BC-081-FLOW và AC-BC-081-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-081 trong traceability.

## REQ-V3-BC-084 / BC-084: Thông tin giao lại/chỉ dẫn qua Merchant hoặc Courier

- [ ] **T-BC-084-CONTRACT** — Chốt operation «Thông tin giao lại/chỉ dẫn qua Merchant hoặc Courier»: allowed actor R10, guard «Nhân sự được quyền xử lý theo state; Recipient không thao tác backend; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource delivery attempt/POD/instructions, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-084-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-084-DB** — Review forward schema/index/constraint của orders cho delivery attempt/POD/instructions nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-084-BE** — Implement «Thông tin giao lại/chỉ dẫn qua Merchant hoặc Courier» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-084-UI** — Tích hợp Verified Recipient channel — Thông tin giao lại/chỉ dẫn qua Merchant hoặc Courier;Courier/CS; public tracking; không Portal/IAM với accepted client/channel; action «Thông tin giao lại/chỉ dẫn qua Merchant hoặc Courier», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-084-VERIFY** — Chạy testcase riêng TEST-V3-BC-084-FLOW và AC-BC-084-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-084 trong traceability.

## REQ-V3-BC-227 / BC-227: Ghi nhận bằng chứng và tăng số lần giao thất bại

- [ ] **T-BC-227-CONTRACT** — Chốt operation «Ghi nhận bằng chứng và tăng số lần giao thất bại»: allowed actor R09, guard «Lý do, timestamp, evidence, attempt ID độc lập; không xóa history; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource delivery attempt/POD/instructions, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-227-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-227-DB** — Review forward schema/index/constraint của orders cho delivery attempt/POD/instructions nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-227-BE** — Implement «Ghi nhận bằng chứng và tăng số lần giao thất bại» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-227-UI** — Tích hợp Internal scoped channel — Ghi nhận bằng chứng và tăng số lần giao thất bại; action «Ghi nhận bằng chứng và tăng số lần giao thất bại», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-227-VERIFY** — Chạy testcase riêng TEST-V3-BC-227-FLOW và AC-BC-227-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-227 trong traceability.

## REQ-V3-BC-299 / BC-299: Đối chiếu delivery attempts và bằng chứng POD

- [ ] **T-BC-299-CONTRACT** — Chốt operation «Đối chiếu delivery attempts và bằng chứng POD»: allowed actor R15, guard «Chứng cứ nguồn gốc; xử lý tranh chấp; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource delivery attempt/POD/instructions, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-299-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-299-DB** — Review forward schema/index/constraint của orders cho delivery attempt/POD/instructions nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-299-BE** — Implement «Đối chiếu delivery attempts và bằng chứng POD» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-299-UI** — Tích hợp Internal scoped channel — Đối chiếu delivery attempts và bằng chứng POD; action «Đối chiếu delivery attempts và bằng chứng POD», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-299-VERIFY** — Chạy testcase riêng TEST-V3-BC-299-FLOW và AC-BC-299-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-299 trong traceability.

## REQ-V3-BC-355 / BC-355: Không được giả POD/DELIVERED

- [ ] **T-BC-355-CONTRACT** — Chốt operation «Không được giả POD/DELIVERED»: allowed actor R31, guard «Không bypass chứng cứ dù là Super Admin; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource delivery attempt/POD/instructions, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-355-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-355-DB** — Chứng minh không ghi effect bị cấm; không thêm positive schema/seed.
- [ ] **T-BC-355-BE** — Implement «Không được giả POD/DELIVERED» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, deny all unauthorized states, không positive branch.
- [ ] **T-BC-355-UI** — Tích hợp Internal scoped channel — Không được giả POD/DELIVERED; không nút cấp hành vi cấm; direct API denial vẫn bắt buộc.
- [ ] **T-BC-355-VERIFY** — Chạy testcase riêng TEST-V3-BC-355-DENY và AC-BC-355-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-355 trong traceability.


- [ ] T-LEGACY-L015: map từng obligation legacy ở spec sang implementation/negative/compatibility testcase, bao gồm phần consumer; không đóng full spec khi requirement legacy còn chưa được chứng minh hoặc disposition rõ.

## Closure toàn Spec

- [ ] T-L015-FULL: từng REQ có AC/design/task/code/API/DB/UI/test/receipt; lint/typecheck/build/test theo app liên quan, required Architecture/H2/PG18 CI exact revision, report/privacy/owned cleanup và backward compatibility giữ nguyên; no unresolved blocking findings.
- [ ] T-L015-ACCEPT: scoped PR integration PASS → Owner merge → regression trên merge SHA → Owner full-spec acceptance riêng. Chưa tick vì docs promotion.


- [ ] **T-AB-RECIPIENT** — REQ-AB-RECIPIENT: close approved operation/state/data/UX/config contract + fixtures; implement owner API/DB/HTTP and integrated Verified scoped delivery response/Courier/CS; unit/PG/security/browser or verified channel assert «Token cannot cancel/alter COD/final shipment facts or grant Merchant authority; expired/replayed verification denied; approved request produces separate attempt/event with evidence»; TEST-AB-RECIPIENT with source+integration receipts. Dependencies L009;L014;L020; do not mark full spec when missing.
