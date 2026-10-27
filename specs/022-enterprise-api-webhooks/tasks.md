# Tasks 022 — enterprise-api-webhooks

NOT_STARTED; thứ tự là contract/fixture → DB → BE/HTTP → UI/channel → verification → full acceptance. Mỗi BC là gói end-to-end nội bộ, không thay phương pháp Spec-by-Spec. Không task nào DONE trong rebuild docs.

## REQ-V3-BC-018 / BC-018: Quản lý API integration/webhook Merchant

- [ ] **T-BC-018-CONTRACT** — Chốt operation «Quản lý API integration/webhook Merchant»: allowed actor R02, guard «Key và scope tách riêng; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource integration config/scoped credential reference/webhook delivery, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-018-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-018-DB** — Review forward schema/index/constraint của merchant cho integration config/scoped credential reference/webhook delivery nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-018-BE** — Implement «Quản lý API integration/webhook Merchant» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-018-UI** — Tích hợp Merchant current context — Quản lý API integration/webhook Merchant; action «Quản lý API integration/webhook Merchant», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-018-VERIFY** — Chạy testcase riêng TEST-V3-BC-018-FLOW và AC-BC-018-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-018 trong traceability.

## REQ-V3-BC-164 / BC-164: Quản lý cấu hình tích hợp và webhook trong scope Merchant

- [ ] **T-BC-164-CONTRACT** — Chốt operation «Quản lý cấu hình tích hợp và webhook trong scope Merchant»: allowed actor R02, guard «Credential secret bảo vệ; không tự cấp scope cao hơn; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource integration config/scoped credential reference/webhook delivery, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-164-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-164-DB** — Review forward schema/index/constraint của merchant cho integration config/scoped credential reference/webhook delivery nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-164-BE** — Implement «Quản lý cấu hình tích hợp và webhook trong scope Merchant» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-164-UI** — Tích hợp Merchant current context — Quản lý cấu hình tích hợp và webhook trong scope Merchant; action «Quản lý cấu hình tích hợp và webhook trong scope Merchant», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-164-VERIFY** — Chạy testcase riêng TEST-V3-BC-164-FLOW và AC-BC-164-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-164 trong traceability.

## REQ-V3-BC-175 / BC-175: Đồng bộ đơn từ hệ thống bán hàng trong phạm vi được cấp

- [ ] **T-BC-175-CONTRACT** — Chốt operation «Đồng bộ đơn từ hệ thống bán hàng trong phạm vi được cấp»: allowed actor R03, guard «Credential dùng integration principal, không chia sẻ human secret; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource integration config/scoped credential reference/webhook delivery, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-175-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-175-DB** — Review forward schema/index/constraint của merchant cho integration config/scoped credential reference/webhook delivery nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-175-BE** — Implement «Đồng bộ đơn từ hệ thống bán hàng trong phạm vi được cấp» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-175-UI** — Tích hợp Merchant current context — Đồng bộ đơn từ hệ thống bán hàng trong phạm vi được cấp; action «Đồng bộ đơn từ hệ thống bán hàng trong phạm vi được cấp», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-175-VERIFY** — Chạy testcase riêng TEST-V3-BC-175-FLOW và AC-BC-175-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-175 trong traceability.

## REQ-V3-BC-200 / BC-200: Xác thực integration principal và quản lý scope API gắn Merchant

- [ ] **T-BC-200-CONTRACT** — Chốt operation «Xác thực integration principal và quản lý scope API gắn Merchant»: allowed actor R22, guard «Credential tách human account; rotate, revoke, audit; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource integration config/scoped credential reference/webhook delivery, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-200-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-200-DB** — Review forward schema/index/constraint của identity cho integration config/scoped credential reference/webhook delivery nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-200-BE** — Implement «Xác thực integration principal và quản lý scope API gắn Merchant» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-200-UI** — Tích hợp Machine API/webhook — Xác thực integration principal và quản lý scope API gắn Merchant; owner console scoped, no human login với accepted client/channel; action «Xác thực integration principal và quản lý scope API gắn Merchant», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-200-VERIFY** — Chạy testcase riêng TEST-V3-BC-200-FLOW và AC-BC-200-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-200 trong traceability.

## REQ-V3-BC-201 / BC-201: Thiết lập endpoint và subscription webhook trạng thái

- [ ] **T-BC-201-CONTRACT** — Chốt operation «Thiết lập endpoint và subscription webhook trạng thái»: allowed actor R22, guard «Tenant bound, signature, retry, duplicate handling; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource integration config/scoped credential reference/webhook delivery, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-201-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-201-DB** — Review forward schema/index/constraint của merchant cho integration config/scoped credential reference/webhook delivery nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-201-BE** — Implement «Thiết lập endpoint và subscription webhook trạng thái» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-201-UI** — Tích hợp Machine API/webhook — Thiết lập endpoint và subscription webhook trạng thái; owner console scoped, no human login với accepted client/channel; action «Thiết lập endpoint và subscription webhook trạng thái», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-201-VERIFY** — Chạy testcase riêng TEST-V3-BC-201-FLOW và AC-BC-201-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-201 trong traceability.

## REQ-V3-BC-202 / BC-202: Tạo và đồng bộ Shipment từ ERP/OMS/WMS/website

- [ ] **T-BC-202-CONTRACT** — Chốt operation «Tạo và đồng bộ Shipment từ ERP/OMS/WMS/website»: allowed actor R22, guard «API idempotency key; đúng Merchant; validation; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource integration config/scoped credential reference/webhook delivery, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-202-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-202-DB** — Review forward schema/index/constraint của merchant cho integration config/scoped credential reference/webhook delivery nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-202-BE** — Implement «Tạo và đồng bộ Shipment từ ERP/OMS/WMS/website» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-202-UI** — Tích hợp Machine API/webhook — Tạo và đồng bộ Shipment từ ERP/OMS/WMS/website; owner console scoped, no human login với accepted client/channel; action «Tạo và đồng bộ Shipment từ ERP/OMS/WMS/website», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-202-VERIFY** — Chạy testcase riêng TEST-V3-BC-202-FLOW và AC-BC-202-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-202 trong traceability.

## REQ-V3-BC-203 / BC-203: Lấy báo giá và danh sách dịch vụ được phép qua API

- [ ] **T-BC-203-CONTRACT** — Chốt operation «Lấy báo giá và danh sách dịch vụ được phép qua API»: allowed actor R22, guard «Áp dụng đúng hợp đồng Merchant; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource integration config/scoped credential reference/webhook delivery, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-203-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-203-DB** — Review forward schema/index/constraint của merchant cho integration config/scoped credential reference/webhook delivery nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-203-BE** — Implement «Lấy báo giá và danh sách dịch vụ được phép qua API» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-203-UI** — Tích hợp Machine API/webhook — Lấy báo giá và danh sách dịch vụ được phép qua API; owner console scoped, no human login với accepted client/channel; action «Lấy báo giá và danh sách dịch vụ được phép qua API», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-203-VERIFY** — Chạy testcase riêng TEST-V3-BC-203-FLOW và AC-BC-203-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-203 trong traceability.

## REQ-V3-BC-204 / BC-204: Lấy nhãn và barcode của Shipment/Parcel qua API

- [ ] **T-BC-204-CONTRACT** — Chốt operation «Lấy nhãn và barcode của Shipment/Parcel qua API»: allowed actor R22, guard «Chỉ tài nguyên thuộc Merchant; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource integration config/scoped credential reference/webhook delivery, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-204-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-204-DB** — Review forward schema/index/constraint của merchant cho integration config/scoped credential reference/webhook delivery nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-204-BE** — Implement «Lấy nhãn và barcode của Shipment/Parcel qua API» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-204-UI** — Tích hợp Machine API/webhook — Lấy nhãn và barcode của Shipment/Parcel qua API; owner console scoped, no human login với accepted client/channel; action «Lấy nhãn và barcode của Shipment/Parcel qua API», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-204-VERIFY** — Chạy testcase riêng TEST-V3-BC-204-FLOW và AC-BC-204-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-204 trong traceability.

## REQ-V3-BC-205 / BC-205: Tra cứu Shipment và nhận tracking events

- [ ] **T-BC-205-CONTRACT** — Chốt operation «Tra cứu Shipment và nhận tracking events»: allowed actor R22, guard «Hạn chế PII; filtering theo Merchant; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource integration config/scoped credential reference/webhook delivery, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-205-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-205-DB** — Review forward schema/index/constraint của merchant cho integration config/scoped credential reference/webhook delivery nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-205-BE** — Implement «Tra cứu Shipment và nhận tracking events» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-205-UI** — Tích hợp Machine API/webhook — Tra cứu Shipment và nhận tracking events; owner console scoped, no human login với accepted client/channel; action «Tra cứu Shipment và nhận tracking events», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-205-VERIFY** — Chạy testcase riêng TEST-V3-BC-205-FLOW và AC-BC-205-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-205 trong traceability.

## REQ-V3-BC-206 / BC-206: Đối chiếu lỗi đồng bộ, retry và failed webhook deliveries

- [ ] **T-BC-206-CONTRACT** — Chốt operation «Đối chiếu lỗi đồng bộ, retry và failed webhook deliveries»: allowed actor R22, guard «Quan sát idempotency; không tạo bút toán/đơn trùng; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource integration config/scoped credential reference/webhook delivery, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-206-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-206-DB** — Review forward schema/index/constraint của merchant cho integration config/scoped credential reference/webhook delivery nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-206-BE** — Implement «Đối chiếu lỗi đồng bộ, retry và failed webhook deliveries» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-206-UI** — Tích hợp Machine API/webhook — Đối chiếu lỗi đồng bộ, retry và failed webhook deliveries; owner console scoped, no human login với accepted client/channel; action «Đối chiếu lỗi đồng bộ, retry và failed webhook deliveries», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-206-VERIFY** — Chạy testcase riêng TEST-V3-BC-206-FLOW và AC-BC-206-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-206 trong traceability.

## REQ-V3-BC-335 / BC-335: Phối hợp onboarding và integration Merchant

- [ ] **T-BC-335-CONTRACT** — Chốt operation «Phối hợp onboarding và integration Merchant»: allowed actor R29, guard «Không tự xem/cấp mọi API secrets; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource integration config/scoped credential reference/webhook delivery, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-335-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-335-DB** — Review forward schema/index/constraint của merchant cho integration config/scoped credential reference/webhook delivery nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-335-BE** — Implement «Phối hợp onboarding và integration Merchant» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-335-UI** — Tích hợp Internal scoped channel — Phối hợp onboarding và integration Merchant; action «Phối hợp onboarding và integration Merchant», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-335-VERIFY** — Chạy testcase riêng TEST-V3-BC-335-FLOW và AC-BC-335-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-335 trong traceability.

## REQ-V3-BC-357 / BC-357: Điều tra lỗi webhook và lịch sử retry

- [ ] **T-BC-357-CONTRACT** — Chốt operation «Điều tra lỗi webhook và lịch sử retry»: allowed actor R32, guard «Idempotent retries; không tạo duplicate state; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource integration config/scoped credential reference/webhook delivery, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-357-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-357-DB** — Review forward schema/index/constraint của merchant cho integration config/scoped credential reference/webhook delivery nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-357-BE** — Implement «Điều tra lỗi webhook và lịch sử retry» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-357-UI** — Tích hợp Internal scoped channel — Điều tra lỗi webhook và lịch sử retry; action «Điều tra lỗi webhook và lịch sử retry», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-357-VERIFY** — Chạy testcase riêng TEST-V3-BC-357-FLOW và AC-BC-357-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-357 trong traceability.

## REQ-V3-BC-358 / BC-358: Quản lý cấu hình tích hợp theo quyền kỹ thuật

- [ ] **T-BC-358-CONTRACT** — Chốt operation «Quản lý cấu hình tích hợp theo quyền kỹ thuật»: allowed actor R32, guard «Tách secret và business action; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource integration config/scoped credential reference/webhook delivery, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-358-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-358-DB** — Review forward schema/index/constraint của merchant cho integration config/scoped credential reference/webhook delivery nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-358-BE** — Implement «Quản lý cấu hình tích hợp theo quyền kỹ thuật» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-358-UI** — Tích hợp Internal scoped channel — Quản lý cấu hình tích hợp theo quyền kỹ thuật; action «Quản lý cấu hình tích hợp theo quyền kỹ thuật», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-358-VERIFY** — Chạy testcase riêng TEST-V3-BC-358-FLOW và AC-BC-358-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-358 trong traceability.

## REQ-V3-BC-360 / BC-360: Đối chiếu lỗi đồng bộ Shipment

- [ ] **T-BC-360-CONTRACT** — Chốt operation «Đối chiếu lỗi đồng bộ Shipment»: allowed actor R32, guard «Không sửa nghiệp vụ vượt quyền; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource integration config/scoped credential reference/webhook delivery, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-360-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-360-DB** — Review forward schema/index/constraint của merchant cho integration config/scoped credential reference/webhook delivery nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-360-BE** — Implement «Đối chiếu lỗi đồng bộ Shipment» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-360-UI** — Tích hợp Internal scoped channel — Đối chiếu lỗi đồng bộ Shipment; action «Đối chiếu lỗi đồng bộ Shipment», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-360-VERIFY** — Chạy testcase riêng TEST-V3-BC-360-FLOW và AC-BC-360-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-360 trong traceability.


## Closure toàn Spec

- [ ] T-L022-FULL: từng REQ có AC/design/task/code/API/DB/UI/test/receipt; lint/typecheck/build/test theo app liên quan, required Architecture/H2/PG18 CI exact revision, report/privacy/owned cleanup và backward compatibility giữ nguyên; no unresolved blocking findings.
- [ ] T-L022-ACCEPT: scoped PR integration PASS → Owner merge → regression trên merge SHA → Owner full-spec acceptance riêng. Chưa tick vì docs promotion.


- [ ] **T-AB-ENTERPRISE** — REQ-AB-ENTERPRISE: close approved operation/state/data/UX/config contract + fixtures; implement owner API/DB/HTTP and integrated Scoped integration settings and machine API/webhooks; unit/PG/security/browser or verified channel assert «Cross-tenant/credential replay/revoked machine deny; no human secret reuse; key disclosure once only per approved contract; duplicate webhook no business duplicate; sync errors reconcile source job»; TEST-AB-ENTERPRISE with source+integration receipts. Dependencies L002;L003;L005;L007;L008;L009; do not mark full spec when missing.
