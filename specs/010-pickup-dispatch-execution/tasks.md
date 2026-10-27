# Tasks 010 — pickup-dispatch-execution

NOT_STARTED; thứ tự là contract/fixture → DB → BE/HTTP → UI/channel → verification → full acceptance. Mỗi BC là gói end-to-end nội bộ, không thay phương pháp Spec-by-Spec. Không task nào DONE trong rebuild docs.

## REQ-V3-BC-037 / BC-037: Phê duyệt ngoại lệ điều phối theo thẩm quyền

- [ ] **T-BC-037-CONTRACT** — Chốt operation «Phê duyệt ngoại lệ điều phối theo thẩm quyền»: allowed actor R04, guard «Không tự tạo bằng chứng pickup; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource pickup request/assignment/evidence, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-037-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-037-DB** — Review forward schema/index/constraint của dispatch cho pickup request/assignment/evidence nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-037-BE** — Implement «Phê duyệt ngoại lệ điều phối theo thẩm quyền» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-037-UI** — Tích hợp Internal scoped channel — Phê duyệt ngoại lệ điều phối theo thẩm quyền; action «Phê duyệt ngoại lệ điều phối theo thẩm quyền», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-037-VERIFY** — Chạy testcase riêng TEST-V3-BC-037-FLOW và AC-BC-037-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-037 trong traceability.

## REQ-V3-BC-047 / BC-047: Xem pickup pool và phân công pickup

- [ ] **T-BC-047-CONTRACT** — Chốt operation «Xem pickup pool và phân công pickup»: allowed actor R06, guard «Theo vùng, tải và trạng thái; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource pickup request/assignment/evidence, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-047-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-047-DB** — Review forward schema/index/constraint của dispatch cho pickup request/assignment/evidence nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-047-BE** — Implement «Xem pickup pool và phân công pickup» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-047-UI** — Tích hợp Internal scoped channel — Xem pickup pool và phân công pickup; action «Xem pickup pool và phân công pickup», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-047-VERIFY** — Chạy testcase riêng TEST-V3-BC-047-FLOW và AC-BC-047-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-047 trong traceability.

## REQ-V3-BC-049 / BC-049: Tái phân công pickup có lý do

- [ ] **T-BC-049-CONTRACT** — Chốt operation «Tái phân công pickup có lý do»: allowed actor R06, guard «Audit, không tự xác nhận pickup; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource pickup request/assignment/evidence, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-049-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-049-DB** — Review forward schema/index/constraint của dispatch cho pickup request/assignment/evidence nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-049-BE** — Implement «Tái phân công pickup có lý do» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-049-UI** — Tích hợp Internal scoped channel — Tái phân công pickup có lý do; action «Tái phân công pickup có lý do», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-049-VERIFY** — Chạy testcase riêng TEST-V3-BC-049-FLOW và AC-BC-049-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-049 trong traceability.

## REQ-V3-BC-056 / BC-056: Xem pickup assignment cá nhân

- [ ] **T-BC-056-CONTRACT** — Chốt operation «Xem pickup assignment cá nhân»: allowed actor R07, guard «Chỉ task được phân công; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource pickup request/assignment/evidence, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-056-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-056-DB** — Review forward schema/index/constraint của dispatch cho pickup request/assignment/evidence nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-056-BE** — Implement «Xem pickup assignment cá nhân» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-056-UI** — Tích hợp Internal scoped channel — Xem pickup assignment cá nhân; action «Xem pickup assignment cá nhân», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-056-VERIFY** — Chạy testcase riêng TEST-V3-BC-056-FLOW và AC-BC-056-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-056 trong traceability.

## REQ-V3-BC-057 / BC-057: Check-in/scan và ghi nhận PICKED_UP

- [ ] **T-BC-057-CONTRACT** — Chốt operation «Check-in/scan và ghi nhận PICKED_UP»: allowed actor R07, guard «Có bằng chứng, đúng trạng thái; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource pickup request/assignment/evidence, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-057-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-057-DB** — Review forward schema/index/constraint của dispatch cho pickup request/assignment/evidence nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-057-BE** — Implement «Check-in/scan và ghi nhận PICKED_UP» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-057-UI** — Tích hợp Internal scoped channel — Check-in/scan và ghi nhận PICKED_UP; action «Check-in/scan và ghi nhận PICKED_UP», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-057-VERIFY** — Chạy testcase riêng TEST-V3-BC-057-FLOW và AC-BC-057-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-057 trong traceability.

## REQ-V3-BC-058 / BC-058: Ghi pickup thất bại và lý do

- [ ] **T-BC-058-CONTRACT** — Chốt operation «Ghi pickup thất bại và lý do»: allowed actor R07, guard «Attempt có timestamp; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource pickup request/assignment/evidence, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-058-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-058-DB** — Review forward schema/index/constraint của dispatch cho pickup request/assignment/evidence nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-058-BE** — Implement «Ghi pickup thất bại và lý do» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-058-UI** — Tích hợp Internal scoped channel — Ghi pickup thất bại và lý do; action «Ghi pickup thất bại và lý do», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-058-VERIFY** — Chạy testcase riêng TEST-V3-BC-058-FLOW và AC-BC-058-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-058 trong traceability.

## REQ-V3-BC-062 / BC-062: Tối ưu thứ tự tuyến pickup

- [ ] **T-BC-062-CONTRACT** — Chốt operation «Tối ưu thứ tự tuyến pickup»: allowed actor R07, guard «Không bắt buộc GPS/routing chuyên sâu; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource pickup request/assignment/evidence, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-062-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-062-DB** — Review forward schema/index/constraint của dispatch cho pickup request/assignment/evidence nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-062-BE** — Implement «Tối ưu thứ tự tuyến pickup» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-062-UI** — Tích hợp Internal scoped channel — Tối ưu thứ tự tuyến pickup; action «Tối ưu thứ tự tuyến pickup», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-062-VERIFY** — Chạy testcase riêng TEST-V3-BC-062-FLOW và AC-BC-062-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-062 trong traceability.

## REQ-V3-BC-141 / BC-141: Đặt yêu cầu Pickup tại nhà

- [ ] **T-BC-141-CONTRACT** — Chốt operation «Đặt yêu cầu Pickup tại nhà»: allowed actor R18, guard «Dịch vụ/địa chỉ đủ điều kiện; phân công do Operations; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource pickup request/assignment/evidence, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-141-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-141-DB** — Review forward schema/index/constraint của dispatch cho pickup request/assignment/evidence nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-141-BE** — Implement «Đặt yêu cầu Pickup tại nhà» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-141-UI** — Tích hợp Personal — Đặt yêu cầu Pickup tại nhà; action «Đặt yêu cầu Pickup tại nhà», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-141-VERIFY** — Chạy testcase riêng TEST-V3-BC-141-FLOW và AC-BC-141-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-141 trong traceability.

## REQ-V3-BC-207 / BC-207: Phân công Pickup Courier theo khu vực, năng lực và tải hiện tại

- [ ] **T-BC-207-CONTRACT** — Chốt operation «Phân công Pickup Courier theo khu vực, năng lực và tải hiện tại»: allowed actor R06, guard «Station/region scope; chỉ assignment đủ điều kiện; chống phân công trùng; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource pickup request/assignment/evidence, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-207-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-207-DB** — Review forward schema/index/constraint của dispatch cho pickup request/assignment/evidence nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-207-BE** — Implement «Phân công Pickup Courier theo khu vực, năng lực và tải hiện tại» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-207-UI** — Tích hợp Internal scoped channel — Phân công Pickup Courier theo khu vực, năng lực và tải hiện tại; action «Phân công Pickup Courier theo khu vực, năng lực và tải hiện tại», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-207-VERIFY** — Chạy testcase riêng TEST-V3-BC-207-FLOW và AC-BC-207-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-207 trong traceability.

## REQ-V3-BC-210 / BC-210: Xem tuyến, thứ tự điểm lấy và địa chỉ Merchant/Retail Sender

- [ ] **T-BC-210-CONTRACT** — Chốt operation «Xem tuyến, thứ tự điểm lấy và địa chỉ Merchant/Retail Sender»: allowed actor R07, guard «Chỉ nhiệm vụ đang được giao; PII tối thiểu; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource pickup request/assignment/evidence, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-210-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-210-DB** — Review forward schema/index/constraint của dispatch cho pickup request/assignment/evidence nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-210-BE** — Implement «Xem tuyến, thứ tự điểm lấy và địa chỉ Merchant/Retail Sender» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-210-UI** — Tích hợp Internal scoped channel — Xem tuyến, thứ tự điểm lấy và địa chỉ Merchant/Retail Sender; action «Xem tuyến, thứ tự điểm lấy và địa chỉ Merchant/Retail Sender», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-210-VERIFY** — Chạy testcase riêng TEST-V3-BC-210-FLOW và AC-BC-210-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-210 trong traceability.

## REQ-V3-BC-211 / BC-211: Liên hệ Merchant/Retail Sender phục vụ pickup

- [ ] **T-BC-211-CONTRACT** — Chốt operation «Liên hệ Merchant/Retail Sender phục vụ pickup»: allowed actor R07, guard «Chỉ trong phạm vi nhiệm vụ; log/quy tắc bảo vệ liên hệ; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource pickup request/assignment/evidence, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-211-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-211-DB** — Review forward schema/index/constraint của dispatch cho pickup request/assignment/evidence nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-211-BE** — Implement «Liên hệ Merchant/Retail Sender phục vụ pickup» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-211-UI** — Tích hợp Internal scoped channel — Liên hệ Merchant/Retail Sender phục vụ pickup; action «Liên hệ Merchant/Retail Sender phục vụ pickup», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-211-VERIFY** — Chạy testcase riêng TEST-V3-BC-211-FLOW và AC-BC-211-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-211 trong traceability.

## REQ-V3-BC-214 / BC-214: Xác nhận thời gian và bằng chứng bàn giao nhận kiện

- [ ] **T-BC-214-CONTRACT** — Chốt operation «Xác nhận thời gian và bằng chứng bàn giao nhận kiện»: allowed actor R07, guard «Assignment hợp lệ; timestamp, actor, custody; không button-only PICKED_UP; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource pickup request/assignment/evidence, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-214-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-214-DB** — Review forward schema/index/constraint của dispatch cho pickup request/assignment/evidence nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-214-BE** — Implement «Xác nhận thời gian và bằng chứng bàn giao nhận kiện» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-214-UI** — Tích hợp Internal scoped channel — Xác nhận thời gian và bằng chứng bàn giao nhận kiện; action «Xác nhận thời gian và bằng chứng bàn giao nhận kiện», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-214-VERIFY** — Chạy testcase riêng TEST-V3-BC-214-FLOW và AC-BC-214-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-214 trong traceability.

## REQ-V3-BC-244 / BC-244: Xem nhiệm vụ Pickup Courier toàn đội

- [ ] **T-BC-244-CONTRACT** — Chốt operation «Xem nhiệm vụ Pickup Courier toàn đội»: allowed actor R24, guard «Chỉ đội/ca được giao; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource pickup request/assignment/evidence, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-244-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-244-DB** — Review forward schema/index/constraint của dispatch cho pickup request/assignment/evidence nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-244-BE** — Implement «Xem nhiệm vụ Pickup Courier toàn đội» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-244-UI** — Tích hợp Internal scoped channel — Xem nhiệm vụ Pickup Courier toàn đội; action «Xem nhiệm vụ Pickup Courier toàn đội», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-244-VERIFY** — Chạy testcase riêng TEST-V3-BC-244-FLOW và AC-BC-244-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-244 trong traceability.

## REQ-V3-BC-247 / BC-247: Đề xuất tái phân công pickup

- [ ] **T-BC-247-CONTRACT** — Chốt operation «Đề xuất tái phân công pickup»: allowed actor R24, guard «Approval/authorization tách biệt với hành động reassignment; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource pickup request/assignment/evidence, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-247-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-247-DB** — Review forward schema/index/constraint của dispatch cho pickup request/assignment/evidence nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-247-BE** — Implement «Đề xuất tái phân công pickup» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-247-UI** — Tích hợp Internal scoped channel — Đề xuất tái phân công pickup; action «Đề xuất tái phân công pickup», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-247-VERIFY** — Chạy testcase riêng TEST-V3-BC-247-FLOW và AC-BC-247-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-247 trong traceability.

## REQ-V3-BC-277 / BC-277: Giám sát năng lực pickup giữa các khu vực

- [ ] **T-BC-277-CONTRACT** — Chốt operation «Giám sát năng lực pickup giữa các khu vực»: allowed actor R27, guard «Không tự tạo chứng cứ PICKED_UP; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource pickup request/assignment/evidence, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-277-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-277-DB** — Review forward schema/index/constraint của dispatch cho pickup request/assignment/evidence nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-277-BE** — Implement «Giám sát năng lực pickup giữa các khu vực» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-277-UI** — Tích hợp Internal scoped channel — Giám sát năng lực pickup giữa các khu vực; action «Giám sát năng lực pickup giữa các khu vực», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-277-VERIFY** — Chạy testcase riêng TEST-V3-BC-277-FLOW và AC-BC-277-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-277 trong traceability.


- [ ] T-LEGACY-L010: map từng obligation legacy ở spec sang implementation/negative/compatibility testcase, bao gồm phần consumer; không đóng full spec khi requirement legacy còn chưa được chứng minh hoặc disposition rõ.

## Closure toàn Spec

- [ ] T-L010-FULL: từng REQ có AC/design/task/code/API/DB/UI/test/receipt; lint/typecheck/build/test theo app liên quan, required Architecture/H2/PG18 CI exact revision, report/privacy/owned cleanup và backward compatibility giữ nguyên; no unresolved blocking findings.
- [ ] T-L010-ACCEPT: scoped PR integration PASS → Owner merge → regression trên merge SHA → Owner full-spec acceptance riêng. Chưa tick vì docs promotion.
