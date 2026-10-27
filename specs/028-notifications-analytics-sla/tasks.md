# Tasks 028 — notifications-analytics-sla

NOT_STARTED; thứ tự là contract/fixture → DB → BE/HTTP → UI/channel → verification → full acceptance. Mỗi BC là gói end-to-end nội bộ, không thay phương pháp Spec-by-Spec. Không task nào DONE trong rebuild docs.

## REQ-V3-BC-017 / BC-017: Xem KPI, báo cáo chi phí, tỷ lệ giao

- [ ] **T-BC-017-CONTRACT** — Chốt operation «Xem KPI, báo cáo chi phí, tỷ lệ giao»: allowed actor R02, guard «Chỉ dữ liệu Merchant; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource notification/inbox/scoped KPI projection, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-017-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-017-DB** — Review forward schema/index/constraint của notification cho notification/inbox/scoped KPI projection nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-017-BE** — Implement «Xem KPI, báo cáo chi phí, tỷ lệ giao» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-017-UI** — Tích hợp Merchant current context — Xem KPI, báo cáo chi phí, tỷ lệ giao; action «Xem KPI, báo cáo chi phí, tỷ lệ giao», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-017-VERIFY** — Chạy testcase riêng TEST-V3-BC-017-FLOW và AC-BC-017-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-017 trong traceability.

## REQ-V3-BC-035 / BC-035: Theo dõi SLA và báo cáo vận hành

- [ ] **T-BC-035-CONTRACT** — Chốt operation «Theo dõi SLA và báo cáo vận hành»: allowed actor R04, guard «Chỉ phạm vi được phép; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource notification/inbox/scoped KPI projection, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-035-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-035-DB** — Review forward schema/index/constraint của notification cho notification/inbox/scoped KPI projection nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-035-BE** — Implement «Theo dõi SLA và báo cáo vận hành» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-035-UI** — Tích hợp Internal scoped channel — Theo dõi SLA và báo cáo vận hành; action «Theo dõi SLA và báo cáo vận hành», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-035-VERIFY** — Chạy testcase riêng TEST-V3-BC-035-FLOW và AC-BC-035-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-035 trong traceability.

## REQ-V3-BC-053 / BC-053: Theo dõi SLA, cảnh báo quá hạn

- [ ] **T-BC-053-CONTRACT** — Chốt operation «Theo dõi SLA, cảnh báo quá hạn»: allowed actor R06, guard «Không tự sửa tracking; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource notification/inbox/scoped KPI projection, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-053-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-053-DB** — Review forward schema/index/constraint của notification cho notification/inbox/scoped KPI projection nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-053-BE** — Implement «Theo dõi SLA, cảnh báo quá hạn» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-053-UI** — Tích hợp Internal scoped channel — Theo dõi SLA, cảnh báo quá hạn; action «Theo dõi SLA, cảnh báo quá hạn», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-053-VERIFY** — Chạy testcase riêng TEST-V3-BC-053-FLOW và AC-BC-053-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-053 trong traceability.

## REQ-V3-BC-061 / BC-061: Xem KPI cá nhân

- [ ] **T-BC-061-CONTRACT** — Chốt operation «Xem KPI cá nhân»: allowed actor R07, guard «Không có quyền giám sát toàn đội; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource notification/inbox/scoped KPI projection, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-061-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-061-DB** — Review forward schema/index/constraint của notification cho notification/inbox/scoped KPI projection nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-061-BE** — Implement «Xem KPI cá nhân» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-061-UI** — Tích hợp Internal scoped channel — Xem KPI cá nhân; action «Xem KPI cá nhân», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-061-VERIFY** — Chạy testcase riêng TEST-V3-BC-061-FLOW và AC-BC-061-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-061 trong traceability.

## REQ-V3-BC-090 / BC-090: Xuất báo cáo COD, phí

- [ ] **T-BC-090-CONTRACT** — Chốt operation «Xuất báo cáo COD, phí»: allowed actor R11, guard «Đúng phạm vi Merchant; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource notification/inbox/scoped KPI projection, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-090-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-090-DB** — Review forward schema/index/constraint của notification cho notification/inbox/scoped KPI projection nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-090-BE** — Implement «Xuất báo cáo COD, phí» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-090-UI** — Tích hợp Merchant current context — Xuất báo cáo COD, phí; action «Xuất báo cáo COD, phí», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-090-VERIFY** — Chạy testcase riêng TEST-V3-BC-090-FLOW và AC-BC-090-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-090 trong traceability.

## REQ-V3-BC-122 / BC-122: Theo dõi KPI/SLA và năng lực ca

- [ ] **T-BC-122-CONTRACT** — Chốt operation «Theo dõi KPI/SLA và năng lực ca»: allowed actor R16, guard «Đúng trạm; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource notification/inbox/scoped KPI projection, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-122-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-122-DB** — Review forward schema/index/constraint của notification cho notification/inbox/scoped KPI projection nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-122-BE** — Implement «Theo dõi KPI/SLA và năng lực ca» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-122-UI** — Tích hợp Internal scoped channel — Theo dõi KPI/SLA và năng lực ca; action «Theo dõi KPI/SLA và năng lực ca», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-122-VERIFY** — Chạy testcase riêng TEST-V3-BC-122-FLOW và AC-BC-122-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-122 trong traceability.

## REQ-V3-BC-130 / BC-130: Xuất báo cáo audit

- [ ] **T-BC-130-CONTRACT** — Chốt operation «Xuất báo cáo audit»: allowed actor R17, guard «Bảo vệ PII; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource notification/inbox/scoped KPI projection, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-130-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-130-DB** — Review forward schema/index/constraint của notification cho notification/inbox/scoped KPI projection nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-130-BE** — Implement «Xuất báo cáo audit» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-130-UI** — Tích hợp Internal scoped channel — Xuất báo cáo audit; action «Xuất báo cáo audit», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-130-VERIFY** — Chạy testcase riêng TEST-V3-BC-130-FLOW và AC-BC-130-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-130 trong traceability.

## REQ-V3-BC-165 / BC-165: Xem báo cáo COD, giao thành công, đơn hoàn, chi phí

- [ ] **T-BC-165-CONTRACT** — Chốt operation «Xem báo cáo COD, giao thành công, đơn hoàn, chi phí»: allowed actor R02, guard «Dữ liệu Merchant; không lộ tenant khác; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource notification/inbox/scoped KPI projection, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-165-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-165-DB** — Review forward schema/index/constraint của notification cho notification/inbox/scoped KPI projection nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-165-BE** — Implement «Xem báo cáo COD, giao thành công, đơn hoàn, chi phí» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-165-UI** — Tích hợp Merchant current context — Xem báo cáo COD, giao thành công, đơn hoàn, chi phí; action «Xem báo cáo COD, giao thành công, đơn hoàn, chi phí», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-165-VERIFY** — Chạy testcase riêng TEST-V3-BC-165-FLOW và AC-BC-165-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-165 trong traceability.

## REQ-V3-BC-172 / BC-172: Theo dõi hiệu suất giao hàng theo Merchant/đơn vị được giao

- [ ] **T-BC-172-CONTRACT** — Chốt operation «Theo dõi hiệu suất giao hàng theo Merchant/đơn vị được giao»: allowed actor R19, guard «Không xem KPI Merchant khác; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource notification/inbox/scoped KPI projection, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-172-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-172-DB** — Review forward schema/index/constraint của notification cho notification/inbox/scoped KPI projection nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-172-BE** — Implement «Theo dõi hiệu suất giao hàng theo Merchant/đơn vị được giao» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-172-UI** — Tích hợp Merchant current context — Theo dõi hiệu suất giao hàng theo Merchant/đơn vị được giao; action «Theo dõi hiệu suất giao hàng theo Merchant/đơn vị được giao», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-172-VERIFY** — Chạy testcase riêng TEST-V3-BC-172-FLOW và AC-BC-172-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-172 trong traceability.

## REQ-V3-BC-192 / BC-192: Xuất báo cáo tài chính theo thời gian, kho và đơn vị kinh doanh

- [ ] **T-BC-192-CONTRACT** — Chốt operation «Xuất báo cáo tài chính theo thời gian, kho và đơn vị kinh doanh»: allowed actor R11, guard «Giới hạn Merchant/branch scope; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource notification/inbox/scoped KPI projection, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-192-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-192-DB** — Review forward schema/index/constraint của notification cho notification/inbox/scoped KPI projection nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-192-BE** — Implement «Xuất báo cáo tài chính theo thời gian, kho và đơn vị kinh doanh» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-192-UI** — Tích hợp Merchant current context — Xuất báo cáo tài chính theo thời gian, kho và đơn vị kinh doanh; action «Xuất báo cáo tài chính theo thời gian, kho và đơn vị kinh doanh», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-192-VERIFY** — Chạy testcase riêng TEST-V3-BC-192-FLOW và AC-BC-192-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-192 trong traceability.

## REQ-V3-BC-209 / BC-209: Giám sát tỷ lệ pickup/delivery thành công và tải công việc

- [ ] **T-BC-209-CONTRACT** — Chốt operation «Giám sát tỷ lệ pickup/delivery thành công và tải công việc»: allowed actor R06, guard «KPI có định nghĩa, dữ liệu trong vùng được phân quyền; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource notification/inbox/scoped KPI projection, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-209-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-209-DB** — Review forward schema/index/constraint của notification cho notification/inbox/scoped KPI projection nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-209-BE** — Implement «Giám sát tỷ lệ pickup/delivery thành công và tải công việc» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-209-UI** — Tích hợp Internal scoped channel — Giám sát tỷ lệ pickup/delivery thành công và tải công việc; action «Giám sát tỷ lệ pickup/delivery thành công và tải công việc», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-209-VERIFY** — Chạy testcase riêng TEST-V3-BC-209-FLOW và AC-BC-209-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-209 trong traceability.

## REQ-V3-BC-215 / BC-215: Báo cáo số điểm đã ghé, số kiện lấy và nhiệm vụ thất bại

- [ ] **T-BC-215-CONTRACT** — Chốt operation «Báo cáo số điểm đã ghé, số kiện lấy và nhiệm vụ thất bại»: allowed actor R07, guard «Chỉ hiệu suất cá nhân; dựa trên event và attempt; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource notification/inbox/scoped KPI projection, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-215-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-215-DB** — Review forward schema/index/constraint của notification cho notification/inbox/scoped KPI projection nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-215-BE** — Implement «Báo cáo số điểm đã ghé, số kiện lấy và nhiệm vụ thất bại» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-215-UI** — Tích hợp Internal scoped channel — Báo cáo số điểm đã ghé, số kiện lấy và nhiệm vụ thất bại; action «Báo cáo số điểm đã ghé, số kiện lấy và nhiệm vụ thất bại», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-215-VERIFY** — Chạy testcase riêng TEST-V3-BC-215-FLOW và AC-BC-215-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-215 trong traceability.

## REQ-V3-BC-229 / BC-229: Xem KPI số kiện giao, tỷ lệ thành công và COD còn phải nộp

- [ ] **T-BC-229-CONTRACT** — Chốt operation «Xem KPI số kiện giao, tỷ lệ thành công và COD còn phải nộp»: allowed actor R09, guard «Chỉ dữ liệu cá nhân; COD chưa nộp không được coi là remitted; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource notification/inbox/scoped KPI projection, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-229-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-229-DB** — Review forward schema/index/constraint của notification cho notification/inbox/scoped KPI projection nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-229-BE** — Implement «Xem KPI số kiện giao, tỷ lệ thành công và COD còn phải nộp» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-229-UI** — Tích hợp Internal scoped channel — Xem KPI số kiện giao, tỷ lệ thành công và COD còn phải nộp; action «Xem KPI số kiện giao, tỷ lệ thành công và COD còn phải nộp», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-229-VERIFY** — Chạy testcase riêng TEST-V3-BC-229-FLOW và AC-BC-229-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-229 trong traceability.

## REQ-V3-BC-246 / BC-246: Theo dõi hiệu suất và năng suất đội giao nhận

- [ ] **T-BC-246-CONTRACT** — Chốt operation «Theo dõi hiệu suất và năng suất đội giao nhận»: allowed actor R24, guard «Dữ liệu đúng team/scope và nguồn sự kiện; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource notification/inbox/scoped KPI projection, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-246-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-246-DB** — Review forward schema/index/constraint của notification cho notification/inbox/scoped KPI projection nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-246-BE** — Implement «Theo dõi hiệu suất và năng suất đội giao nhận» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-246-UI** — Tích hợp Internal scoped channel — Theo dõi hiệu suất và năng suất đội giao nhận; action «Theo dõi hiệu suất và năng suất đội giao nhận», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-246-VERIFY** — Chạy testcase riêng TEST-V3-BC-246-FLOW và AC-BC-246-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-246 trong traceability.

## REQ-V3-BC-268 / BC-268: Theo dõi KPI, throughput và thời gian tồn Hub

- [ ] **T-BC-268-CONTRACT** — Chốt operation «Theo dõi KPI, throughput và thời gian tồn Hub»: allowed actor R26, guard «Chỉ số định nghĩa và provenance; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource notification/inbox/scoped KPI projection, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-268-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-268-DB** — Review forward schema/index/constraint của notification cho notification/inbox/scoped KPI projection nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-268-BE** — Implement «Theo dõi KPI, throughput và thời gian tồn Hub» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-268-UI** — Tích hợp Internal scoped channel — Theo dõi KPI, throughput và thời gian tồn Hub; action «Theo dõi KPI, throughput và thời gian tồn Hub», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-268-VERIFY** — Chạy testcase riêng TEST-V3-BC-268-FLOW và AC-BC-268-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-268 trong traceability.

## REQ-V3-BC-275 / BC-275: Theo dõi SLA, cảnh báo và năng suất mạng lưới

- [ ] **T-BC-275-CONTRACT** — Chốt operation «Theo dõi SLA, cảnh báo và năng suất mạng lưới»: allowed actor R27, guard «Định nghĩa thống nhất, phân quyền dữ liệu; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource notification/inbox/scoped KPI projection, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-275-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-275-DB** — Review forward schema/index/constraint của notification cho notification/inbox/scoped KPI projection nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-275-BE** — Implement «Theo dõi SLA, cảnh báo và năng suất mạng lưới» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-275-UI** — Tích hợp Internal scoped channel — Theo dõi SLA, cảnh báo và năng suất mạng lưới; action «Theo dõi SLA, cảnh báo và năng suất mạng lưới», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-275-VERIFY** — Chạy testcase riêng TEST-V3-BC-275-FLOW và AC-BC-275-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-275 trong traceability.

## REQ-V3-BC-291 / BC-291: Xuất báo cáo công nợ và COD tồn chưa đối soát

- [ ] **T-BC-291-CONTRACT** — Chốt operation «Xuất báo cáo công nợ và COD tồn chưa đối soát»: allowed actor R13, guard «Theo kỳ, có nguồn bút toán; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource notification/inbox/scoped KPI projection, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-291-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-291-DB** — Review forward schema/index/constraint của notification cho notification/inbox/scoped KPI projection nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-291-BE** — Implement «Xuất báo cáo công nợ và COD tồn chưa đối soát» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-291-UI** — Tích hợp Internal scoped channel — Xuất báo cáo công nợ và COD tồn chưa đối soát; action «Xuất báo cáo công nợ và COD tồn chưa đối soát», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-291-VERIFY** — Chạy testcase riêng TEST-V3-BC-291-FLOW và AC-BC-291-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-291 trong traceability.

## REQ-V3-BC-333 / BC-333: Theo dõi KPI và chỉ tiêu kinh doanh

- [ ] **T-BC-333-CONTRACT** — Chốt operation «Theo dõi KPI và chỉ tiêu kinh doanh»: allowed actor R29, guard «Dữ liệu theo account/region scope; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource notification/inbox/scoped KPI projection, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-333-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-333-DB** — Review forward schema/index/constraint của notification cho notification/inbox/scoped KPI projection nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-333-BE** — Implement «Theo dõi KPI và chỉ tiêu kinh doanh» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-333-UI** — Tích hợp Internal scoped channel — Theo dõi KPI và chỉ tiêu kinh doanh; action «Theo dõi KPI và chỉ tiêu kinh doanh», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-333-VERIFY** — Chạy testcase riêng TEST-V3-BC-333-FLOW và AC-BC-333-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-333 trong traceability.

## REQ-V3-BC-356 / BC-356: Kiểm tra sức khỏe hệ thống và tín hiệu vận hành

- [ ] **T-BC-356-CONTRACT** — Chốt operation «Kiểm tra sức khỏe hệ thống và tín hiệu vận hành»: allowed actor R32, guard «Chỉ telemetry cần thiết; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource notification/inbox/scoped KPI projection, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-356-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-356-DB** — Review forward schema/index/constraint của notification cho notification/inbox/scoped KPI projection nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-356-BE** — Implement «Kiểm tra sức khỏe hệ thống và tín hiệu vận hành» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-356-UI** — Tích hợp Internal scoped channel — Kiểm tra sức khỏe hệ thống và tín hiệu vận hành; action «Kiểm tra sức khỏe hệ thống và tín hiệu vận hành», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-356-VERIFY** — Chạy testcase riêng TEST-V3-BC-356-FLOW và AC-BC-356-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-356 trong traceability.

## REQ-V3-BC-359 / BC-359: Giám sát hàng đợi, dead-letter và backlog

- [ ] **T-BC-359-CONTRACT** — Chốt operation «Giám sát hàng đợi, dead-letter và backlog»: allowed actor R32, guard «Có hạn mức retry và cảnh báo; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource notification/inbox/scoped KPI projection, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-359-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-359-DB** — Review forward schema/index/constraint của notification cho notification/inbox/scoped KPI projection nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-359-BE** — Implement «Giám sát hàng đợi, dead-letter và backlog» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-359-UI** — Tích hợp Internal scoped channel — Giám sát hàng đợi, dead-letter và backlog; action «Giám sát hàng đợi, dead-letter và backlog», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-359-VERIFY** — Chạy testcase riêng TEST-V3-BC-359-FLOW và AC-BC-359-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-359 trong traceability.

## REQ-V3-BC-363 / BC-363: Khắc phục lỗi phát thông báo/tracking event

- [ ] **T-BC-363-CONTRACT** — Chốt operation «Khắc phục lỗi phát thông báo/tracking event»: allowed actor R32, guard «Không giả mạo sự kiện giao hàng; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource notification/inbox/scoped KPI projection, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-363-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-363-DB** — Review forward schema/index/constraint của notification cho notification/inbox/scoped KPI projection nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-363-BE** — Implement «Khắc phục lỗi phát thông báo/tracking event» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-363-UI** — Tích hợp Internal scoped channel — Khắc phục lỗi phát thông báo/tracking event; action «Khắc phục lỗi phát thông báo/tracking event», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-363-VERIFY** — Chạy testcase riêng TEST-V3-BC-363-FLOW và AC-BC-363-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-363 trong traceability.


- [ ] T-LEGACY-L028: map từng obligation legacy ở spec sang implementation/negative/compatibility testcase, bao gồm phần consumer; không đóng full spec khi requirement legacy còn chưa được chứng minh hoặc disposition rõ.

## Closure toàn Spec

- [ ] T-L028-FULL: từng REQ có AC/design/task/code/API/DB/UI/test/receipt; lint/typecheck/build/test theo app liên quan, required Architecture/H2/PG18 CI exact revision, report/privacy/owned cleanup và backward compatibility giữ nguyên; no unresolved blocking findings.
- [ ] T-L028-ACCEPT: scoped PR integration PASS → Owner merge → regression trên merge SHA → Owner full-spec acceptance riêng. Chưa tick vì docs promotion.


- [ ] **T-AB-PERSONAL-DASH** — REQ-AB-PERSONAL-DASH: close approved operation/state/data/UX/config contract + fixtures; implement owner API/DB/HTTP and integrated Personal dashboard; unit/PG/security/browser or verified channel assert «Counters and chart tie to eligible owned source rows and timestamps; empty data zero/empty not fabricated; foreign shipment search denied»; TEST-AB-PERSONAL-DASH with source+integration receipts. Dependencies L009;L007; do not mark full spec when missing.


- [ ] **T-AB-NOTIFICATIONS** — REQ-AB-NOTIFICATIONS: close approved operation/state/data/UX/config contract + fixtures; implement owner API/DB/HTTP and integrated Personal/Merchant inbox; verified Recipient channel; unit/PG/security/browser or verified channel assert «Duplicate/reorder/revoked recipient no leak/double effect; mark-read only owned; failure retained with bounded retry»; TEST-AB-NOTIFICATIONS with source+integration receipts. Dependencies L009;L015;L020; do not mark full spec when missing.


- [ ] **T-AB-PORTAL-SHELL** — REQ-AB-PORTAL-SHELL: close approved operation/state/data/UX/config contract + fixtures; implement owner API/DB/HTTP and integrated Shared customer/Merchant shell; unit/PG/security/browser or verified channel assert «Each visible interactive action calls supported API or deliberate disabled availability explanation; no mock-only dashboards; no privileged menu by role label alone»; TEST-AB-PORTAL-SHELL with source+integration receipts. Dependencies L003;L009; do not mark full spec when missing.
