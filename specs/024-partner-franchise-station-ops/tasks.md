# Tasks 024 — partner-franchise-station-ops

NOT_STARTED; thứ tự là contract/fixture → DB → BE/HTTP → UI/channel → verification → full acceptance. Mỗi BC là gói end-to-end nội bộ, không thay phương pháp Spec-by-Spec. Không task nào DONE trong rebuild docs.

## REQ-V3-BC-364 / BC-364: Tiếp nhận/gửi Shipment tại quầy đối tác được chỉ định

- [ ] **T-BC-364-CONTRACT** — Chốt operation «Tiếp nhận/gửi Shipment tại quầy đối tác được chỉ định»: allowed actor R33, guard «Partner+Station scope, xác minh walk-in; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource Partner point/assignment/custody, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-364-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-364-DB** — Review forward schema/index/constraint của dispatch cho Partner point/assignment/custody nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-364-BE** — Implement «Tiếp nhận/gửi Shipment tại quầy đối tác được chỉ định» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-364-UI** — Tích hợp Internal scoped channel — Tiếp nhận/gửi Shipment tại quầy đối tác được chỉ định; action «Tiếp nhận/gửi Shipment tại quầy đối tác được chỉ định», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-364-VERIFY** — Chạy testcase riêng TEST-V3-BC-364-FLOW và AC-BC-364-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-364 trong traceability.

## REQ-V3-BC-365 / BC-365: Bàn giao kiện cho hãng vận chuyển

- [ ] **T-BC-365-CONTRACT** — Chốt operation «Bàn giao kiện cho hãng vận chuyển»: allowed actor R33, guard «Actor, count, manifest, timestamp, custody proof; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource Partner point/assignment/custody, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-365-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-365-DB** — Review forward schema/index/constraint của dispatch cho Partner point/assignment/custody nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-365-BE** — Implement «Bàn giao kiện cho hãng vận chuyển» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-365-UI** — Tích hợp Internal scoped channel — Bàn giao kiện cho hãng vận chuyển; action «Bàn giao kiện cho hãng vận chuyển», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-365-VERIFY** — Chạy testcase riêng TEST-V3-BC-365-FLOW và AC-BC-365-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-365 trong traceability.

## REQ-V3-BC-366 / BC-366: Xác nhận số lượng thực giao/nhận

- [ ] **T-BC-366-CONTRACT** — Chốt operation «Xác nhận số lượng thực giao/nhận»: allowed actor R33, guard «Sai lệch phải ghi event và investigation; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource Partner point/assignment/custody, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-366-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-366-DB** — Review forward schema/index/constraint của dispatch cho Partner point/assignment/custody nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-366-BE** — Implement «Xác nhận số lượng thực giao/nhận» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-366-UI** — Tích hợp Internal scoped channel — Xác nhận số lượng thực giao/nhận; action «Xác nhận số lượng thực giao/nhận», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-366-VERIFY** — Chạy testcase riêng TEST-V3-BC-366-FLOW và AC-BC-366-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-366 trong traceability.

## REQ-V3-BC-367 / BC-367: Quản lý tồn kiện ở điểm đại lý

- [ ] **T-BC-367-CONTRACT** — Chốt operation «Quản lý tồn kiện ở điểm đại lý»: allowed actor R33, guard «Không đọc tồn nơi khác; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource Partner point/assignment/custody, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-367-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-367-DB** — Review forward schema/index/constraint của dispatch cho Partner point/assignment/custody nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-367-BE** — Implement «Quản lý tồn kiện ở điểm đại lý» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-367-UI** — Tích hợp Internal scoped channel — Quản lý tồn kiện ở điểm đại lý; action «Quản lý tồn kiện ở điểm đại lý», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-367-VERIFY** — Chạy testcase riêng TEST-V3-BC-367-FLOW và AC-BC-367-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-367 trong traceability.

## REQ-V3-BC-368 / BC-368: Xem doanh thu dịch vụ thuộc điểm được giao

- [ ] **T-BC-368-CONTRACT** — Chốt operation «Xem doanh thu dịch vụ thuộc điểm được giao»: allowed actor R33, guard «Chỉ báo cáo được phép; không sửa settlement; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource Partner point/assignment/custody, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-368-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-368-DB** — Review forward schema/index/constraint của dispatch cho Partner point/assignment/custody nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-368-BE** — Implement «Xem doanh thu dịch vụ thuộc điểm được giao» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-368-UI** — Tích hợp Internal scoped channel — Xem doanh thu dịch vụ thuộc điểm được giao; action «Xem doanh thu dịch vụ thuộc điểm được giao», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-368-VERIFY** — Chạy testcase riêng TEST-V3-BC-368-FLOW và AC-BC-368-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-368 trong traceability.

## REQ-V3-BC-369 / BC-369: Ràng buộc Partner Organization với Station

- [ ] **T-BC-369-CONTRACT** — Chốt operation «Ràng buộc Partner Organization với Station»: allowed actor R33, guard «Explicit membership/scope; no network-wide access; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource Partner point/assignment/custody, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-369-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-369-DB** — Review forward schema/index/constraint của dispatch cho Partner point/assignment/custody nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-369-BE** — Implement «Ràng buộc Partner Organization với Station» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-369-UI** — Tích hợp Internal scoped channel — Ràng buộc Partner Organization với Station; action «Ràng buộc Partner Organization với Station», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-369-VERIFY** — Chạy testcase riêng TEST-V3-BC-369-FLOW và AC-BC-369-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-369 trong traceability.

## REQ-V3-BC-370 / BC-370: Quét/kiểm tra nhãn và tình trạng kiện

- [ ] **T-BC-370-CONTRACT** — Chốt operation «Quét/kiểm tra nhãn và tình trạng kiện»: allowed actor R33, guard «Không giả mạo scan của hãng; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource Partner point/assignment/custody, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-370-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-370-DB** — Review forward schema/index/constraint của dispatch cho Partner point/assignment/custody nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-370-BE** — Implement «Quét/kiểm tra nhãn và tình trạng kiện» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-370-UI** — Tích hợp Internal scoped channel — Quét/kiểm tra nhãn và tình trạng kiện; action «Quét/kiểm tra nhãn và tình trạng kiện», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-370-VERIFY** — Chạy testcase riêng TEST-V3-BC-370-FLOW và AC-BC-370-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-370 trong traceability.

## REQ-V3-BC-371 / BC-371: Truy vết nghiệp vụ điểm đối tác

- [ ] **T-BC-371-CONTRACT** — Chốt operation «Truy vết nghiệp vụ điểm đối tác»: allowed actor R33, guard «Không xóa handover history; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource Partner point/assignment/custody, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-371-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-371-DB** — Review forward schema/index/constraint của dispatch cho Partner point/assignment/custody nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-371-BE** — Implement «Truy vết nghiệp vụ điểm đối tác» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-371-UI** — Tích hợp Internal scoped channel — Truy vết nghiệp vụ điểm đối tác; action «Truy vết nghiệp vụ điểm đối tác», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-371-VERIFY** — Chạy testcase riêng TEST-V3-BC-371-FLOW và AC-BC-371-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-371 trong traceability.


## Closure toàn Spec

- [ ] T-L024-FULL: từng REQ có AC/design/task/code/API/DB/UI/test/receipt; lint/typecheck/build/test theo app liên quan, required Architecture/H2/PG18 CI exact revision, report/privacy/owned cleanup và backward compatibility giữ nguyên; no unresolved blocking findings.
- [ ] T-L024-ACCEPT: scoped PR integration PASS → Owner merge → regression trên merge SHA → Owner full-spec acceptance riêng. Chưa tick vì docs promotion.
