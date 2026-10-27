# Tasks 009 — tracking-event-core-public-lookup

NOT_STARTED; thứ tự là contract/fixture → DB → BE/HTTP → UI/channel → verification → full acceptance. Mỗi BC là gói end-to-end nội bộ, không thay phương pháp Spec-by-Spec. Không task nào DONE trong rebuild docs.

## REQ-V3-BC-003 / BC-003: Tra cứu tracking giới hạn

- [ ] **T-BC-003-CONTRACT** — Chốt operation «Tra cứu tracking giới hạn»: allowed actor R01, guard «Không lộ PII, COD nhạy cảm; tăng xác minh khi cần; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource source tracking event/scoped projection, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-003-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-003-DB** — Review forward schema/index/constraint của orders cho source tracking event/scoped projection nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-003-BE** — Implement «Tra cứu tracking giới hạn» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-003-UI** — Tích hợp Public — Tra cứu tracking giới hạn; action «Tra cứu tracking giới hạn», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-003-VERIFY** — Chạy testcase riêng TEST-V3-BC-003-FLOW và AC-BC-003-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-003 trong traceability.

## REQ-V3-BC-013 / BC-013: Theo dõi mọi Shipment thuộc Merchant

- [ ] **T-BC-013-CONTRACT** — Chốt operation «Theo dõi mọi Shipment thuộc Merchant»: allowed actor R02, guard «Không cross-tenant; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource source tracking event/scoped projection, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-013-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-013-DB** — Review forward schema/index/constraint của orders cho source tracking event/scoped projection nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-013-BE** — Implement «Theo dõi mọi Shipment thuộc Merchant» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-013-UI** — Tích hợp Merchant current context — Theo dõi mọi Shipment thuộc Merchant; action «Theo dõi mọi Shipment thuộc Merchant», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-013-VERIFY** — Chạy testcase riêng TEST-V3-BC-013-FLOW và AC-BC-013-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-013 trong traceability.

## REQ-V3-BC-027 / BC-027: Tra cứu trạng thái Shipment thuộc scope

- [ ] **T-BC-027-CONTRACT** — Chốt operation «Tra cứu trạng thái Shipment thuộc scope»: allowed actor R03, guard «Merchant isolation; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource source tracking event/scoped projection, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-027-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-027-DB** — Review forward schema/index/constraint của orders cho source tracking event/scoped projection nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-027-BE** — Implement «Tra cứu trạng thái Shipment thuộc scope» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-027-UI** — Tích hợp Merchant current context — Tra cứu trạng thái Shipment thuộc scope; action «Tra cứu trạng thái Shipment thuộc scope», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-027-VERIFY** — Chạy testcase riêng TEST-V3-BC-027-FLOW và AC-BC-027-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-027 trong traceability.

## REQ-V3-BC-051 / BC-051: Theo dõi sự kiện và nhiệm vụ vận chuyển

- [ ] **T-BC-051-CONTRACT** — Chốt operation «Theo dõi sự kiện và nhiệm vụ vận chuyển»: allowed actor R06, guard «Không truy cập ngoài region; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource source tracking event/scoped projection, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-051-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-051-DB** — Review forward schema/index/constraint của orders cho source tracking event/scoped projection nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-051-BE** — Implement «Theo dõi sự kiện và nhiệm vụ vận chuyển» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-051-UI** — Tích hợp Internal scoped channel — Theo dõi sự kiện và nhiệm vụ vận chuyển; action «Theo dõi sự kiện và nhiệm vụ vận chuyển», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-051-VERIFY** — Chạy testcase riêng TEST-V3-BC-051-FLOW và AC-BC-051-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-051 trong traceability.

## REQ-V3-BC-060 / BC-060: Xem lịch sử nhiệm vụ cá nhân

- [ ] **T-BC-060-CONTRACT** — Chốt operation «Xem lịch sử nhiệm vụ cá nhân»: allowed actor R07, guard «PII tối thiểu; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource source tracking event/scoped projection, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-060-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-060-DB** — Review forward schema/index/constraint của orders cho source tracking event/scoped projection nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-060-BE** — Implement «Xem lịch sử nhiệm vụ cá nhân» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-060-UI** — Tích hợp Internal scoped channel — Xem lịch sử nhiệm vụ cá nhân; action «Xem lịch sử nhiệm vụ cá nhân», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-060-VERIFY** — Chạy testcase riêng TEST-V3-BC-060-FLOW và AC-BC-060-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-060 trong traceability.

## REQ-V3-BC-068 / BC-068: Xem trạng thái kiện tại Station

- [ ] **T-BC-068-CONTRACT** — Chốt operation «Xem trạng thái kiện tại Station»: allowed actor R08, guard «Đúng station scope; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource source tracking event/scoped projection, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-068-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-068-DB** — Review forward schema/index/constraint của orders cho source tracking event/scoped projection nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-068-BE** — Implement «Xem trạng thái kiện tại Station» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-068-UI** — Tích hợp Internal scoped channel — Xem trạng thái kiện tại Station; action «Xem trạng thái kiện tại Station», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-068-VERIFY** — Chạy testcase riêng TEST-V3-BC-068-FLOW và AC-BC-068-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-068 trong traceability.

## REQ-V3-BC-078 / BC-078: Xem thông tin liên hệ người nhận tối thiểu

- [ ] **T-BC-078-CONTRACT** — Chốt operation «Xem thông tin liên hệ người nhận tối thiểu»: allowed actor R09, guard «Chỉ trong assignment; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource source tracking event/scoped projection, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-078-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-078-DB** — Review forward schema/index/constraint của orders cho source tracking event/scoped projection nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-078-BE** — Implement «Xem thông tin liên hệ người nhận tối thiểu» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-078-UI** — Tích hợp Internal scoped channel — Xem thông tin liên hệ người nhận tối thiểu; action «Xem thông tin liên hệ người nhận tối thiểu», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-078-VERIFY** — Chạy testcase riêng TEST-V3-BC-078-FLOW và AC-BC-078-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-078 trong traceability.

## REQ-V3-BC-080 / BC-080: Nhận/tra cứu thông tin trạng thái giới hạn

- [ ] **T-BC-080-CONTRACT** — Chốt operation «Nhận/tra cứu thông tin trạng thái giới hạn»: allowed actor R10, guard «Không login; xác minh phù hợp; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource source tracking event/scoped projection, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-080-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-080-DB** — Review forward schema/index/constraint của orders cho source tracking event/scoped projection nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-080-BE** — Implement «Nhận/tra cứu thông tin trạng thái giới hạn» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-080-UI** — Tích hợp Verified Recipient channel — Nhận/tra cứu thông tin trạng thái giới hạn;Courier/CS; public tracking; không Portal/IAM với accepted client/channel; action «Nhận/tra cứu thông tin trạng thái giới hạn», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-080-VERIFY** — Chạy testcase riêng TEST-V3-BC-080-FLOW và AC-BC-080-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-080 trong traceability.

## REQ-V3-BC-106 / BC-106: Tra tracking, POD và điểm quét cuối

- [ ] **T-BC-106-CONTRACT** — Chốt operation «Tra tracking, POD và điểm quét cuối»: allowed actor R14, guard «Chỉ ticket/scope được phân công; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource source tracking event/scoped projection, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-106-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-106-DB** — Review forward schema/index/constraint của orders cho source tracking event/scoped projection nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-106-BE** — Implement «Tra tracking, POD và điểm quét cuối» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-106-UI** — Tích hợp Internal scoped channel — Tra tracking, POD và điểm quét cuối; action «Tra tracking, POD và điểm quét cuối», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-106-VERIFY** — Chạy testcase riêng TEST-V3-BC-106-FLOW và AC-BC-106-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-106 trong traceability.

## REQ-V3-BC-126 / BC-126: Xem lịch sử tracking/custody có chứng cứ

- [ ] **T-BC-126-CONTRACT** — Chốt operation «Xem lịch sử tracking/custody có chứng cứ»: allowed actor R17, guard «Chỉ dữ liệu được cấp; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource source tracking event/scoped projection, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-126-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-126-DB** — Review forward schema/index/constraint của orders cho source tracking event/scoped projection nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-126-BE** — Implement «Xem lịch sử tracking/custody có chứng cứ» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-126-UI** — Tích hợp Internal scoped channel — Xem lịch sử tracking/custody có chứng cứ; action «Xem lịch sử tracking/custody có chứng cứ», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-126-VERIFY** — Chạy testcase riêng TEST-V3-BC-126-FLOW và AC-BC-126-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-126 trong traceability.

## REQ-V3-BC-143 / BC-143: Tra cứu tất cả Shipment do mình tạo

- [ ] **T-BC-143-CONTRACT** — Chốt operation «Tra cứu tất cả Shipment do mình tạo»: allowed actor R18, guard «Resource owner isolation; không cross-account; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource source tracking event/scoped projection, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-143-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-143-DB** — Review forward schema/index/constraint của orders cho source tracking event/scoped projection nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-143-BE** — Implement «Tra cứu tất cả Shipment do mình tạo» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-143-UI** — Tích hợp Personal — Tra cứu tất cả Shipment do mình tạo; action «Tra cứu tất cả Shipment do mình tạo», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-143-VERIFY** — Chạy testcase riêng TEST-V3-BC-143-FLOW và AC-BC-143-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-143 trong traceability.

## REQ-V3-BC-170 / BC-170: Theo dõi giao chậm, vận đơn bất thường và tiến độ

- [ ] **T-BC-170-CONTRACT** — Chốt operation «Theo dõi giao chậm, vận đơn bất thường và tiến độ»: allowed actor R19, guard «Đúng scope Merchant/branch; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource source tracking event/scoped projection, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-170-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-170-DB** — Review forward schema/index/constraint của orders cho source tracking event/scoped projection nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-170-BE** — Implement «Theo dõi giao chậm, vận đơn bất thường và tiến độ» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-170-UI** — Tích hợp Merchant current context — Theo dõi giao chậm, vận đơn bất thường và tiến độ; action «Theo dõi giao chậm, vận đơn bất thường và tiến độ», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-170-VERIFY** — Chạy testcase riêng TEST-V3-BC-170-FLOW và AC-BC-170-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-170 trong traceability.

## REQ-V3-BC-186 / BC-186: Theo dõi trạng thái kiện đã bàn giao cho hãng

- [ ] **T-BC-186-CONTRACT** — Chốt operation «Theo dõi trạng thái kiện đã bàn giao cho hãng»: allowed actor R20, guard «Chỉ kho/đơn thuộc scope; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource source tracking event/scoped projection, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-186-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-186-DB** — Review forward schema/index/constraint của orders cho source tracking event/scoped projection nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-186-BE** — Implement «Theo dõi trạng thái kiện đã bàn giao cho hãng» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-186-UI** — Tích hợp Merchant current context — Theo dõi trạng thái kiện đã bàn giao cho hãng; action «Theo dõi trạng thái kiện đã bàn giao cho hãng», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-186-VERIFY** — Chạy testcase riêng TEST-V3-BC-186-FLOW và AC-BC-186-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-186 trong traceability.

## REQ-V3-BC-194 / BC-194: Theo dõi giao chậm, giao thất bại và thông báo bất thường

- [ ] **T-BC-194-CONTRACT** — Chốt operation «Theo dõi giao chậm, giao thất bại và thông báo bất thường»: allowed actor R21, guard «Chỉ dữ liệu cần cho CSKH; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource source tracking event/scoped projection, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-194-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-194-DB** — Review forward schema/index/constraint của orders cho source tracking event/scoped projection nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-194-BE** — Implement «Theo dõi giao chậm, giao thất bại và thông báo bất thường» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-194-UI** — Tích hợp Merchant current context — Theo dõi giao chậm, giao thất bại và thông báo bất thường; action «Theo dõi giao chậm, giao thất bại và thông báo bất thường», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-194-VERIFY** — Chạy testcase riêng TEST-V3-BC-194-FLOW và AC-BC-194-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-194 trong traceability.

## REQ-V3-BC-293 / BC-293: Xử lý yêu cầu giao chậm qua điểm quét cuối

- [ ] **T-BC-293-CONTRACT** — Chốt operation «Xử lý yêu cầu giao chậm qua điểm quét cuối»: allowed actor R14, guard «Không giả lập tracking event; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource source tracking event/scoped projection, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-293-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-293-DB** — Review forward schema/index/constraint của orders cho source tracking event/scoped projection nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-293-BE** — Implement «Xử lý yêu cầu giao chậm qua điểm quét cuối» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-293-UI** — Tích hợp Internal scoped channel — Xử lý yêu cầu giao chậm qua điểm quét cuối; action «Xử lý yêu cầu giao chậm qua điểm quét cuối», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-293-VERIFY** — Chạy testcase riêng TEST-V3-BC-293-FLOW và AC-BC-293-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-293 trong traceability.

## REQ-V3-BC-315 / BC-315: Đọc tracking events để kiểm toán vận chuyển

- [ ] **T-BC-315-CONTRACT** — Chốt operation «Đọc tracking events để kiểm toán vận chuyển»: allowed actor R17, guard «Không sửa trạng thái/attempt; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource source tracking event/scoped projection, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-315-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-315-DB** — Review forward schema/index/constraint của orders cho source tracking event/scoped projection nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-315-BE** — Implement «Đọc tracking events để kiểm toán vận chuyển» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-315-UI** — Tích hợp Internal scoped channel — Đọc tracking events để kiểm toán vận chuyển; action «Đọc tracking events để kiểm toán vận chuyển», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-315-VERIFY** — Chạy testcase riêng TEST-V3-BC-315-FLOW và AC-BC-315-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-315 trong traceability.


- [ ] T-LEGACY-L009: map từng obligation legacy ở spec sang implementation/negative/compatibility testcase, bao gồm phần consumer; không đóng full spec khi requirement legacy còn chưa được chứng minh hoặc disposition rõ.

## Closure toàn Spec

- [ ] T-L009-FULL: từng REQ có AC/design/task/code/API/DB/UI/test/receipt; lint/typecheck/build/test theo app liên quan, required Architecture/H2/PG18 CI exact revision, report/privacy/owned cleanup và backward compatibility giữ nguyên; no unresolved blocking findings.
- [ ] T-L009-ACCEPT: scoped PR integration PASS → Owner merge → regression trên merge SHA → Owner full-spec acceptance riêng. Chưa tick vì docs promotion.


- [ ] **T-AB-TRACKING-PRIVACY** — REQ-AB-TRACKING-PRIVACY: close approved operation/state/data/UX/config contract + fixtures; implement owner API/DB/HTTP and integrated Public limited lookup; private detail/timeline; unit/PG/security/browser or verified channel assert «Direct code/UUID enumeration, token misuse, delayed/reordered duplicate events and revoked scope tests; trace sources; no event retroactively fakes POD»; TEST-AB-TRACKING-PRIVACY with source+integration receipts. Dependencies L006; do not mark full spec when missing.
