# Tasks 007 — shipment-booking-sender-payments

NOT_STARTED; thứ tự là contract/fixture → DB → BE/HTTP → UI/channel → verification → full acceptance. Mỗi BC là gói end-to-end nội bộ, không thay phương pháp Spec-by-Spec. Không task nào DONE trong rebuild docs.

## REQ-V3-BC-006 / BC-006: Tự tạo Shipment online khi không có tài khoản — BỊ CẤM theo phạm vi Capstone

- [ ] **T-BC-006-CONTRACT** — Chốt operation «Tự tạo Shipment online khi không có tài khoản — BỊ CẤM theo phạm vi Capstone»: allowed actor R01, guard «Không cấp API public tạo Shipment; ký gửi tại quầy do Station Operator thực hiện; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource owned Shipment/request; finance charge reference, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-006-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-006-DB** — Chứng minh không ghi effect bị cấm; không thêm positive schema/seed.
- [ ] **T-BC-006-BE** — Implement «Tự tạo Shipment online khi không có tài khoản — BỊ CẤM theo phạm vi Capstone» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, deny all unauthorized states, không positive branch.
- [ ] **T-BC-006-UI** — Tích hợp Public — Tự tạo Shipment online khi không có tài khoản — BỊ CẤM theo phạm vi Capstone; không nút cấp hành vi cấm; direct API denial vẫn bắt buộc.
- [ ] **T-BC-006-VERIFY** — Chạy testcase riêng TEST-V3-BC-006-DENY và AC-BC-006-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-006 trong traceability.

## REQ-V3-BC-011 / BC-011: Tạo, xem, xác nhận Shipment

- [ ] **T-BC-011-CONTRACT** — Chốt operation «Tạo, xem, xác nhận Shipment»: allowed actor R02, guard «Membership, valid state, idempotency; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource owned Shipment/request; finance charge reference, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-011-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-011-DB** — Review forward schema/index/constraint của orders cho owned Shipment/request; finance charge reference nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-011-BE** — Implement «Tạo, xem, xác nhận Shipment» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-011-UI** — Tích hợp Merchant current context — Tạo, xem, xác nhận Shipment; action «Tạo, xem, xác nhận Shipment», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-011-VERIFY** — Chạy testcase riêng TEST-V3-BC-011-FLOW và AC-BC-011-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-011 trong traceability.

## REQ-V3-BC-012 / BC-012: Yêu cầu hủy/sửa Shipment

- [ ] **T-BC-012-CONTRACT** — Chốt operation «Yêu cầu hủy/sửa Shipment»: allowed actor R02, guard «Chỉ trạng thái cho phép; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource owned Shipment/request; finance charge reference, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-012-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-012-DB** — Review forward schema/index/constraint của orders cho owned Shipment/request; finance charge reference nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-012-BE** — Implement «Yêu cầu hủy/sửa Shipment» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-012-UI** — Tích hợp Merchant current context — Yêu cầu hủy/sửa Shipment; action «Yêu cầu hủy/sửa Shipment», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-012-VERIFY** — Chạy testcase riêng TEST-V3-BC-012-FLOW và AC-BC-012-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-012 trong traceability.

## REQ-V3-BC-024 / BC-024: Tạo/xác nhận Shipment

- [ ] **T-BC-024-CONTRACT** — Chốt operation «Tạo/xác nhận Shipment»: allowed actor R03, guard «Idempotency, đúng org; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource owned Shipment/request; finance charge reference, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-024-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-024-DB** — Review forward schema/index/constraint của orders cho owned Shipment/request; finance charge reference nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-024-BE** — Implement «Tạo/xác nhận Shipment» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-024-UI** — Tích hợp Merchant current context — Tạo/xác nhận Shipment; action «Tạo/xác nhận Shipment», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-024-VERIFY** — Chạy testcase riêng TEST-V3-BC-024-FLOW và AC-BC-024-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-024 trong traceability.

## REQ-V3-BC-025 / BC-025: Xem và yêu cầu hủy/giao lại

- [ ] **T-BC-025-CONTRACT** — Chốt operation «Xem và yêu cầu hủy/giao lại»: allowed actor R03, guard «Hủy/sửa phải đúng quyền và state; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource owned Shipment/request; finance charge reference, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-025-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-025-DB** — Review forward schema/index/constraint của orders cho owned Shipment/request; finance charge reference nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-025-BE** — Implement «Xem và yêu cầu hủy/giao lại» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-025-UI** — Tích hợp Merchant current context — Xem và yêu cầu hủy/giao lại; action «Xem và yêu cầu hủy/giao lại», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-025-VERIFY** — Chạy testcase riêng TEST-V3-BC-025-FLOW và AC-BC-025-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-025 trong traceability.

## REQ-V3-BC-139 / BC-139: Tạo và xác nhận Shipment gửi hàng cá nhân

- [ ] **T-BC-139-CONTRACT** — Chốt operation «Tạo và xác nhận Shipment gửi hàng cá nhân»: allowed actor R18, guard «Shipment thuộc account; valid state, chống trùng/idempotency; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource owned Shipment/request; finance charge reference, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-139-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-139-DB** — Review forward schema/index/constraint của orders cho owned Shipment/request; finance charge reference nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-139-BE** — Implement «Tạo và xác nhận Shipment gửi hàng cá nhân» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-139-UI** — Tích hợp Personal — Tạo và xác nhận Shipment gửi hàng cá nhân; action «Tạo và xác nhận Shipment gửi hàng cá nhân», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-139-VERIFY** — Chạy testcase riêng TEST-V3-BC-139-FLOW và AC-BC-139-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-139 trong traceability.

## REQ-V3-BC-144 / BC-144: Hủy yêu cầu gửi khi trạng thái cho phép

- [ ] **T-BC-144-CONTRACT** — Chốt operation «Hủy yêu cầu gửi khi trạng thái cho phép»: allowed actor R18, guard «Đã pickup phải chuyển workflow thu hồi/hoàn; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource owned Shipment/request; finance charge reference, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-144-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-144-DB** — Review forward schema/index/constraint của orders cho owned Shipment/request; finance charge reference nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-144-BE** — Implement «Hủy yêu cầu gửi khi trạng thái cho phép» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-144-UI** — Tích hợp Personal — Hủy yêu cầu gửi khi trạng thái cho phép; action «Hủy yêu cầu gửi khi trạng thái cho phép», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-144-VERIFY** — Chạy testcase riêng TEST-V3-BC-144-FLOW và AC-BC-144-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-144 trong traceability.

## REQ-V3-BC-147 / BC-147: Xem lịch sử vận đơn đã gửi

- [ ] **T-BC-147-CONTRACT** — Chốt operation «Xem lịch sử vận đơn đã gửi»: allowed actor R18, guard «Giới hạn theo account và quyền riêng tư; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource owned Shipment/request; finance charge reference, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-147-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-147-DB** — Review forward schema/index/constraint của orders cho owned Shipment/request; finance charge reference nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-147-BE** — Implement «Xem lịch sử vận đơn đã gửi» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-147-UI** — Tích hợp Personal — Xem lịch sử vận đơn đã gửi; action «Xem lịch sử vận đơn đã gửi», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-147-VERIFY** — Chạy testcase riêng TEST-V3-BC-147-FLOW và AC-BC-147-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-147 trong traceability.

## REQ-V3-BC-160 / BC-160: Tạo Shipment thủ công và hàng loạt, quản lý toàn bộ Shipment Merchant

- [ ] **T-BC-160-CONTRACT** — Chốt operation «Tạo Shipment thủ công và hàng loạt, quản lý toàn bộ Shipment Merchant»: allowed actor R02, guard «Có kiểm tra dữ liệu và idempotency, không sửa lịch sử; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource owned Shipment/request; finance charge reference, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-160-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-160-DB** — Review forward schema/index/constraint của orders cho owned Shipment/request; finance charge reference nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-160-BE** — Implement «Tạo Shipment thủ công và hàng loạt, quản lý toàn bộ Shipment Merchant» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-160-UI** — Tích hợp Merchant current context — Tạo Shipment thủ công và hàng loạt, quản lý toàn bộ Shipment Merchant; action «Tạo Shipment thủ công và hàng loạt, quản lý toàn bộ Shipment Merchant», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-160-VERIFY** — Chạy testcase riêng TEST-V3-BC-160-FLOW và AC-BC-160-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-160 trong traceability.

## REQ-V3-BC-169 / BC-169: Kiểm tra Shipment cần xử lý và duyệt sửa/hủy theo chính sách

- [ ] **T-BC-169-CONTRACT** — Chốt operation «Kiểm tra Shipment cần xử lý và duyệt sửa/hủy theo chính sách»: allowed actor R19, guard «State guard; không ghi đè Shipment đã chốt trái quyền; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource owned Shipment/request; finance charge reference, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-169-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-169-DB** — Review forward schema/index/constraint của orders cho owned Shipment/request; finance charge reference nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-169-BE** — Implement «Kiểm tra Shipment cần xử lý và duyệt sửa/hủy theo chính sách» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-169-UI** — Tích hợp Merchant current context — Kiểm tra Shipment cần xử lý và duyệt sửa/hủy theo chính sách; action «Kiểm tra Shipment cần xử lý và duyệt sửa/hủy theo chính sách», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-169-VERIFY** — Chạy testcase riêng TEST-V3-BC-169-FLOW và AC-BC-169-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-169 trong traceability.

## REQ-V3-BC-193 / BC-193: Tra cứu Order/Shipment của Merchant để hỗ trợ người mua

- [ ] **T-BC-193-CONTRACT** — Chốt operation «Tra cứu Order/Shipment của Merchant để hỗ trợ người mua»: allowed actor R21, guard «Không xem Shipment Merchant khác; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource owned Shipment/request; finance charge reference, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-193-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-193-DB** — Review forward schema/index/constraint của orders cho owned Shipment/request; finance charge reference nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-193-BE** — Implement «Tra cứu Order/Shipment của Merchant để hỗ trợ người mua» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-193-UI** — Tích hợp Merchant current context — Tra cứu Order/Shipment của Merchant để hỗ trợ người mua; action «Tra cứu Order/Shipment của Merchant để hỗ trợ người mua», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-193-VERIFY** — Chạy testcase riêng TEST-V3-BC-193-FLOW và AC-BC-193-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-193 trong traceability.

## REQ-V3-BC-243 / BC-243: Không sửa đơn Merchant hoặc ghi DELIVERED thay Courier

- [ ] **T-BC-243-CONTRACT** — Chốt operation «Không sửa đơn Merchant hoặc ghi DELIVERED thay Courier»: allowed actor R23, guard «Negative authorization cho commercial state/POD; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource owned Shipment/request; finance charge reference, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-243-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-243-DB** — Chứng minh không ghi effect bị cấm; không thêm positive schema/seed.
- [ ] **T-BC-243-BE** — Implement «Không sửa đơn Merchant hoặc ghi DELIVERED thay Courier» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, deny all unauthorized states, không positive branch.
- [ ] **T-BC-243-UI** — Tích hợp Internal scoped channel — Không sửa đơn Merchant hoặc ghi DELIVERED thay Courier; không nút cấp hành vi cấm; direct API denial vẫn bắt buộc.
- [ ] **T-BC-243-VERIFY** — Chạy testcase riêng TEST-V3-BC-243-DENY và AC-BC-243-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-243 trong traceability.


- [ ] T-LEGACY-L007: map từng obligation legacy ở spec sang implementation/negative/compatibility testcase, bao gồm phần consumer; không đóng full spec khi requirement legacy còn chưa được chứng minh hoặc disposition rõ.

## Closure toàn Spec

- [ ] T-L007-FULL: từng REQ có AC/design/task/code/API/DB/UI/test/receipt; lint/typecheck/build/test theo app liên quan, required Architecture/H2/PG18 CI exact revision, report/privacy/owned cleanup và backward compatibility giữ nguyên; no unresolved blocking findings.
- [ ] T-L007-ACCEPT: scoped PR integration PASS → Owner merge → regression trên merge SHA → Owner full-spec acceptance riêng. Chưa tick vì docs promotion.


- [ ] **T-AB-BOOKING-FORM** — REQ-AB-BOOKING-FORM: close approved operation/state/data/UX/config contract + fixtures; implement owner API/DB/HTTP and integrated Single create/draft/confirm/reset/view; unit/PG/security/browser or verified channel assert «Real browser personal WITHOUT Merchant and Merchant branch; confirm snapshots+single charge/reservation+state atomic; invalid/missing funds rollback; lost response status recovery; cancel cutoff enforced»; TEST-AB-BOOKING-FORM with source+integration receipts. Dependencies L003;L004;L005;L006;L018; do not mark full spec when missing.


- [ ] **T-AB-SHIPMENT-MANAGEMENT** — REQ-AB-SHIPMENT-MANAGEMENT: close approved operation/state/data/UX/config contract + fixtures; implement owner API/DB/HTTP and integrated My/Merchant shipments list/detail; unit/PG/security/browser or verified channel assert «Filter-before-paging isolation including counts; locked fields immutable, stale updates conflict; unsupported actions unavailable, history preserved»; TEST-AB-SHIPMENT-MANAGEMENT with source+integration receipts. Dependencies L006;L009;L008;L023;L028; do not mark full spec when missing.
