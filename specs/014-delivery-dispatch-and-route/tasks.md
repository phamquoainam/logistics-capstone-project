# Tasks 014 — delivery-dispatch-and-route

NOT_STARTED; thứ tự là contract/fixture → DB → BE/HTTP → UI/channel → verification → full acceptance. Mỗi BC là gói end-to-end nội bộ, không thay phương pháp Spec-by-Spec. Không task nào DONE trong rebuild docs.

## REQ-V3-BC-048 / BC-048: Xem delivery pool và phân công giao

- [ ] **T-BC-048-CONTRACT** — Chốt operation «Xem delivery pool và phân công giao»: allowed actor R06, guard «Kiện đủ điều kiện; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource delivery request/assignment/route, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-048-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-048-DB** — Review forward schema/index/constraint của dispatch cho delivery request/assignment/route nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-048-BE** — Implement «Xem delivery pool và phân công giao» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-048-UI** — Tích hợp Internal scoped channel — Xem delivery pool và phân công giao; action «Xem delivery pool và phân công giao», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-048-VERIFY** — Chạy testcase riêng TEST-V3-BC-048-FLOW và AC-BC-048-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-048 trong traceability.

## REQ-V3-BC-050 / BC-050: Điều phối giao lại

- [ ] **T-BC-050-CONTRACT** — Chốt operation «Điều phối giao lại»: allowed actor R06, guard «Delivery attempt và chính sách; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource delivery request/assignment/route, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-050-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-050-DB** — Review forward schema/index/constraint của dispatch cho delivery request/assignment/route nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-050-BE** — Implement «Điều phối giao lại» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-050-UI** — Tích hợp Internal scoped channel — Điều phối giao lại; action «Điều phối giao lại», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-050-VERIFY** — Chạy testcase riêng TEST-V3-BC-050-FLOW và AC-BC-050-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-050 trong traceability.

## REQ-V3-BC-054 / BC-054: Gom điểm/tối ưu tuyến

- [ ] **T-BC-054-CONTRACT** — Chốt operation «Gom điểm/tối ưu tuyến»: allowed actor R06, guard «Chưa yêu cầu routing optimization chuyên sâu; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource delivery request/assignment/route, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-054-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-054-DB** — Review forward schema/index/constraint của dispatch cho delivery request/assignment/route nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-054-BE** — Implement «Gom điểm/tối ưu tuyến» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-054-UI** — Tích hợp Internal scoped channel — Gom điểm/tối ưu tuyến; action «Gom điểm/tối ưu tuyến», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-054-VERIFY** — Chạy testcase riêng TEST-V3-BC-054-FLOW và AC-BC-054-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-054 trong traceability.

## REQ-V3-BC-071 / BC-071: Xem và nhận kiện giao theo assignment

- [ ] **T-BC-071-CONTRACT** — Chốt operation «Xem và nhận kiện giao theo assignment»: allowed actor R09, guard «Chỉ nhiệm vụ cá nhân; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource delivery request/assignment/route, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-071-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-071-DB** — Review forward schema/index/constraint của dispatch cho delivery request/assignment/route nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-071-BE** — Implement «Xem và nhận kiện giao theo assignment» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-071-UI** — Tích hợp Internal scoped channel — Xem và nhận kiện giao theo assignment; action «Xem và nhận kiện giao theo assignment», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-071-VERIFY** — Chạy testcase riêng TEST-V3-BC-071-FLOW và AC-BC-071-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-071 trong traceability.

## REQ-V3-BC-072 / BC-072: Ghi OUT_FOR_DELIVERY

- [ ] **T-BC-072-CONTRACT** — Chốt operation «Ghi OUT_FOR_DELIVERY»: allowed actor R09, guard «Chỉ khi kiện đã được bàn giao hợp lệ; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource delivery request/assignment/route, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-072-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-072-DB** — Review forward schema/index/constraint của dispatch cho delivery request/assignment/route nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-072-BE** — Implement «Ghi OUT_FOR_DELIVERY» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-072-UI** — Tích hợp Internal scoped channel — Ghi OUT_FOR_DELIVERY; action «Ghi OUT_FOR_DELIVERY», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-072-VERIFY** — Chạy testcase riêng TEST-V3-BC-072-FLOW và AC-BC-072-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-072 trong traceability.

## REQ-V3-BC-079 / BC-079: Tuyến giao hàng tối ưu/GPS real-time

- [ ] **T-BC-079-CONTRACT** — Chốt operation «Tuyến giao hàng tối ưu/GPS real-time»: allowed actor R09, guard «Không bắt buộc với MVP; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource delivery request/assignment/route, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-079-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-079-DB** — Review forward schema/index/constraint của dispatch cho delivery request/assignment/route nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-079-BE** — Implement «Tuyến giao hàng tối ưu/GPS real-time» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-079-UI** — Tích hợp Internal scoped channel — Tuyến giao hàng tối ưu/GPS real-time; action «Tuyến giao hàng tối ưu/GPS real-time», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-079-VERIFY** — Chạy testcase riêng TEST-V3-BC-079-FLOW và AC-BC-079-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-079 trong traceability.

## REQ-V3-BC-119 / BC-119: Giám sát pickup/courier thuộc Station

- [ ] **T-BC-119-CONTRACT** — Chốt operation «Giám sát pickup/courier thuộc Station»: allowed actor R16, guard «Không xác nhận PICKED_UP thay courier; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource delivery request/assignment/route, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-119-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-119-DB** — Review forward schema/index/constraint của dispatch cho delivery request/assignment/route nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-119-BE** — Implement «Giám sát pickup/courier thuộc Station» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-119-UI** — Tích hợp Internal scoped channel — Giám sát pickup/courier thuộc Station; action «Giám sát pickup/courier thuộc Station», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-119-VERIFY** — Chạy testcase riêng TEST-V3-BC-119-FLOW và AC-BC-119-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-119 trong traceability.

## REQ-V3-BC-120 / BC-120: Giám sát giao thất bại và tồn phát

- [ ] **T-BC-120-CONTRACT** — Chốt operation «Giám sát giao thất bại và tồn phát»: allowed actor R16, guard «Không tạo POD giả; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource delivery request/assignment/route, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-120-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-120-DB** — Review forward schema/index/constraint của dispatch cho delivery request/assignment/route nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-120-BE** — Implement «Giám sát giao thất bại và tồn phát» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-120-UI** — Tích hợp Internal scoped channel — Giám sát giao thất bại và tồn phát; action «Giám sát giao thất bại và tồn phát», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-120-VERIFY** — Chạy testcase riêng TEST-V3-BC-120-FLOW và AC-BC-120-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-120 trong traceability.

## REQ-V3-BC-171 / BC-171: Yêu cầu xử lý giao thất bại và giao lại

- [ ] **T-BC-171-CONTRACT** — Chốt operation «Yêu cầu xử lý giao thất bại và giao lại»: allowed actor R19, guard «Không tạo DELIVERED/POD thay Courier; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource delivery request/assignment/route, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-171-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-171-DB** — Review forward schema/index/constraint của dispatch cho delivery request/assignment/route nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-171-BE** — Implement «Yêu cầu xử lý giao thất bại và giao lại» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-171-UI** — Tích hợp Merchant current context — Yêu cầu xử lý giao thất bại và giao lại; action «Yêu cầu xử lý giao thất bại và giao lại», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-171-VERIFY** — Chạy testcase riêng TEST-V3-BC-171-FLOW và AC-BC-171-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-171 trong traceability.

## REQ-V3-BC-177 / BC-177: Gửi yêu cầu giao lại qua workflow theo quyền

- [ ] **T-BC-177-CONTRACT** — Chốt operation «Gửi yêu cầu giao lại qua workflow theo quyền»: allowed actor R03, guard «Không tự gán Courier hoặc POD; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource delivery request/assignment/route, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-177-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-177-DB** — Review forward schema/index/constraint của dispatch cho delivery request/assignment/route nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-177-BE** — Implement «Gửi yêu cầu giao lại qua workflow theo quyền» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-177-UI** — Tích hợp Merchant current context — Gửi yêu cầu giao lại qua workflow theo quyền; action «Gửi yêu cầu giao lại qua workflow theo quyền», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-177-VERIFY** — Chạy testcase riêng TEST-V3-BC-177-FLOW và AC-BC-177-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-177 trong traceability.

## REQ-V3-BC-197 / BC-197: Yêu cầu giao lại và cung cấp chỉ dẫn giao có kiểm soát

- [ ] **T-BC-197-CONTRACT** — Chốt operation «Yêu cầu giao lại và cung cấp chỉ dẫn giao có kiểm soát»: allowed actor R21, guard «Không trực tiếp ghi trạng thái giao; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource delivery request/assignment/route, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-197-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-197-DB** — Review forward schema/index/constraint của dispatch cho delivery request/assignment/route nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-197-BE** — Implement «Yêu cầu giao lại và cung cấp chỉ dẫn giao có kiểm soát» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-197-UI** — Tích hợp Merchant current context — Yêu cầu giao lại và cung cấp chỉ dẫn giao có kiểm soát; action «Yêu cầu giao lại và cung cấp chỉ dẫn giao có kiểm soát», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-197-VERIFY** — Chạy testcase riêng TEST-V3-BC-197-FLOW và AC-BC-197-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-197 trong traceability.

## REQ-V3-BC-208 / BC-208: Phân công Delivery Courier theo khu vực, tuyến và năng lực

- [ ] **T-BC-208-CONTRACT** — Chốt operation «Phân công Delivery Courier theo khu vực, tuyến và năng lực»: allowed actor R06, guard «Kiện sẵn sàng giao, custody/scan và assignment hợp lệ; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource delivery request/assignment/route, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-208-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-208-DB** — Review forward schema/index/constraint của dispatch cho delivery request/assignment/route nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-208-BE** — Implement «Phân công Delivery Courier theo khu vực, tuyến và năng lực» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-208-UI** — Tích hợp Internal scoped channel — Phân công Delivery Courier theo khu vực, tuyến và năng lực; action «Phân công Delivery Courier theo khu vực, tuyến và năng lực», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-208-VERIFY** — Chạy testcase riêng TEST-V3-BC-208-FLOW và AC-BC-208-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-208 trong traceability.

## REQ-V3-BC-222 / BC-222: Quét barcode xác nhận nhận tải kiện từ bưu cục

- [ ] **T-BC-222-CONTRACT** — Chốt operation «Quét barcode xác nhận nhận tải kiện từ bưu cục»: allowed actor R09, guard «Handover hai phía, đúng assignment, không nhận trùng; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource delivery request/assignment/route, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-222-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-222-DB** — Review forward schema/index/constraint của dispatch cho delivery request/assignment/route nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-222-BE** — Implement «Quét barcode xác nhận nhận tải kiện từ bưu cục» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-222-UI** — Tích hợp Internal scoped channel — Quét barcode xác nhận nhận tải kiện từ bưu cục; action «Quét barcode xác nhận nhận tải kiện từ bưu cục», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-222-VERIFY** — Chạy testcase riêng TEST-V3-BC-222-FLOW và AC-BC-222-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-222 trong traceability.

## REQ-V3-BC-223 / BC-223: Xem tuyến giao, bản đồ và điểm dừng

- [ ] **T-BC-223-CONTRACT** — Chốt operation «Xem tuyến giao, bản đồ và điểm dừng»: allowed actor R09, guard «Chỉ nhiệm vụ được giao; địa chỉ và PII tối thiểu; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource delivery request/assignment/route, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-223-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-223-DB** — Review forward schema/index/constraint của dispatch cho delivery request/assignment/route nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-223-BE** — Implement «Xem tuyến giao, bản đồ và điểm dừng» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-223-UI** — Tích hợp Internal scoped channel — Xem tuyến giao, bản đồ và điểm dừng; action «Xem tuyến giao, bản đồ và điểm dừng», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-223-VERIFY** — Chạy testcase riêng TEST-V3-BC-223-FLOW và AC-BC-223-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-223 trong traceability.

## REQ-V3-BC-224 / BC-224: Liên hệ Recipient theo chính sách bảo vệ số điện thoại

- [ ] **T-BC-224-CONTRACT** — Chốt operation «Liên hệ Recipient theo chính sách bảo vệ số điện thoại»: allowed actor R09, guard «Chỉ khi assignment hợp lệ; bảo vệ thông tin liên hệ; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource delivery request/assignment/route, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-224-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-224-DB** — Review forward schema/index/constraint của dispatch cho delivery request/assignment/route nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-224-BE** — Implement «Liên hệ Recipient theo chính sách bảo vệ số điện thoại» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-224-UI** — Tích hợp Internal scoped channel — Liên hệ Recipient theo chính sách bảo vệ số điện thoại; action «Liên hệ Recipient theo chính sách bảo vệ số điện thoại», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-224-VERIFY** — Chạy testcase riêng TEST-V3-BC-224-FLOW và AC-BC-224-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-224 trong traceability.

## REQ-V3-BC-226 / BC-226: Thực hiện giao lại theo assignment mới

- [ ] **T-BC-226-CONTRACT** — Chốt operation «Thực hiện giao lại theo assignment mới»: allowed actor R09, guard «Delivery attempt riêng; không tái sử dụng assignment hết hiệu lực; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource delivery request/assignment/route, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-226-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-226-DB** — Review forward schema/index/constraint của dispatch cho delivery request/assignment/route nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-226-BE** — Implement «Thực hiện giao lại theo assignment mới» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-226-UI** — Tích hợp Internal scoped channel — Thực hiện giao lại theo assignment mới; action «Thực hiện giao lại theo assignment mới», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-226-VERIFY** — Chạy testcase riêng TEST-V3-BC-226-FLOW và AC-BC-226-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-226 trong traceability.

## REQ-V3-BC-245 / BC-245: Xem nhiệm vụ Delivery Courier toàn đội

- [ ] **T-BC-245-CONTRACT** — Chốt operation «Xem nhiệm vụ Delivery Courier toàn đội»: allowed actor R24, guard «Không tự có quyền giao thay Courier; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource delivery request/assignment/route, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-245-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-245-DB** — Review forward schema/index/constraint của dispatch cho delivery request/assignment/route nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-245-BE** — Implement «Xem nhiệm vụ Delivery Courier toàn đội» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-245-UI** — Tích hợp Internal scoped channel — Xem nhiệm vụ Delivery Courier toàn đội; action «Xem nhiệm vụ Delivery Courier toàn đội», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-245-VERIFY** — Chạy testcase riêng TEST-V3-BC-245-FLOW và AC-BC-245-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-245 trong traceability.

## REQ-V3-BC-248 / BC-248: Đề xuất tái phân công delivery

- [ ] **T-BC-248-CONTRACT** — Chốt operation «Đề xuất tái phân công delivery»: allowed actor R24, guard «Không tự cấp assignment rồi tự ghi POD; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource delivery request/assignment/route, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-248-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-248-DB** — Review forward schema/index/constraint của dispatch cho delivery request/assignment/route nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-248-BE** — Implement «Đề xuất tái phân công delivery» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-248-UI** — Tích hợp Internal scoped channel — Đề xuất tái phân công delivery; action «Đề xuất tái phân công delivery», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-248-VERIFY** — Chạy testcase riêng TEST-V3-BC-248-FLOW và AC-BC-248-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-248 trong traceability.

## REQ-V3-BC-274 / BC-274: Theo dõi tỷ lệ giao thành công toàn mạng

- [ ] **T-BC-274-CONTRACT** — Chốt operation «Theo dõi tỷ lệ giao thành công toàn mạng»: allowed actor R27, guard «KPI từ attempts/tracking, không sửa POD; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource delivery request/assignment/route, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-274-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-274-DB** — Review forward schema/index/constraint của dispatch cho delivery request/assignment/route nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-274-BE** — Implement «Theo dõi tỷ lệ giao thành công toàn mạng» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-274-UI** — Tích hợp Internal scoped channel — Theo dõi tỷ lệ giao thành công toàn mạng; action «Theo dõi tỷ lệ giao thành công toàn mạng», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-274-VERIFY** — Chạy testcase riêng TEST-V3-BC-274-FLOW và AC-BC-274-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-274 trong traceability.

## REQ-V3-BC-278 / BC-278: Điều phối chuyển vùng phát trong ngoại lệ

- [ ] **T-BC-278-CONTRACT** — Chốt operation «Điều phối chuyển vùng phát trong ngoại lệ»: allowed actor R27, guard «Tôn trọng state, active assignment và custody; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource delivery request/assignment/route, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-278-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-278-DB** — Review forward schema/index/constraint của dispatch cho delivery request/assignment/route nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-278-BE** — Implement «Điều phối chuyển vùng phát trong ngoại lệ» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-278-UI** — Tích hợp Internal scoped channel — Điều phối chuyển vùng phát trong ngoại lệ; action «Điều phối chuyển vùng phát trong ngoại lệ», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-278-VERIFY** — Chạy testcase riêng TEST-V3-BC-278-FLOW và AC-BC-278-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-278 trong traceability.


- [ ] T-LEGACY-L014: map từng obligation legacy ở spec sang implementation/negative/compatibility testcase, bao gồm phần consumer; không đóng full spec khi requirement legacy còn chưa được chứng minh hoặc disposition rõ.

## Closure toàn Spec

- [ ] T-L014-FULL: từng REQ có AC/design/task/code/API/DB/UI/test/receipt; lint/typecheck/build/test theo app liên quan, required Architecture/H2/PG18 CI exact revision, report/privacy/owned cleanup và backward compatibility giữ nguyên; no unresolved blocking findings.
- [ ] T-L014-ACCEPT: scoped PR integration PASS → Owner merge → regression trên merge SHA → Owner full-spec acceptance riêng. Chưa tick vì docs promotion.
