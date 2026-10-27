# Tasks 012 — sorting-hub-manifests

NOT_STARTED; thứ tự là contract/fixture → DB → BE/HTTP → UI/channel → verification → full acceptance. Mỗi BC là gói end-to-end nội bộ, không thay phương pháp Spec-by-Spec. Không task nào DONE trong rebuild docs.

## REQ-V3-BC-254 / BC-254: Phân tuyến theo đích và quy tắc vận hành

- [ ] **T-BC-254-CONTRACT** — Chốt operation «Phân tuyến theo đích và quy tắc vận hành»: allowed actor R25, guard «Đúng scope Hub, route plan hợp lệ; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource sort/bag/manifest/custody, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-254-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-254-DB** — Review forward schema/index/constraint của dispatch cho sort/bag/manifest/custody nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-254-BE** — Implement «Phân tuyến theo đích và quy tắc vận hành» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-254-UI** — Tích hợp Internal scoped channel — Phân tuyến theo đích và quy tắc vận hành; Hub capacity/variance approval với accepted client/channel; action «Phân tuyến theo đích và quy tắc vận hành», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-254-VERIFY** — Chạy testcase riêng TEST-V3-BC-254-FLOW và AC-BC-254-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-254 trong traceability.

## REQ-V3-BC-255 / BC-255: Gom kiện vào bao/túi/đơn vị xử lý

- [ ] **T-BC-255-CONTRACT** — Chốt operation «Gom kiện vào bao/túi/đơn vị xử lý»: allowed actor R25, guard «Parent-child manifest và số lượng kiểm soát; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource sort/bag/manifest/custody, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-255-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-255-DB** — Review forward schema/index/constraint của dispatch cho sort/bag/manifest/custody nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-255-BE** — Implement «Gom kiện vào bao/túi/đơn vị xử lý» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-255-UI** — Tích hợp Internal scoped channel — Gom kiện vào bao/túi/đơn vị xử lý; Hub capacity/variance approval với accepted client/channel; action «Gom kiện vào bao/túi/đơn vị xử lý», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-255-VERIFY** — Chạy testcase riêng TEST-V3-BC-255-FLOW và AC-BC-255-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-255 trong traceability.

## REQ-V3-BC-256 / BC-256: Xác nhận kiện đã qua dây chuyền phân loại

- [ ] **T-BC-256-CONTRACT** — Chốt operation «Xác nhận kiện đã qua dây chuyền phân loại»: allowed actor R25, guard «Không giả lập scan; có evidence; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource sort/bag/manifest/custody, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-256-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-256-DB** — Review forward schema/index/constraint của dispatch cho sort/bag/manifest/custody nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-256-BE** — Implement «Xác nhận kiện đã qua dây chuyền phân loại» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-256-UI** — Tích hợp Internal scoped channel — Xác nhận kiện đã qua dây chuyền phân loại; Hub capacity/variance approval với accepted client/channel; action «Xác nhận kiện đã qua dây chuyền phân loại», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-256-VERIFY** — Chạy testcase riêng TEST-V3-BC-256-FLOW và AC-BC-256-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-256 trong traceability.

## REQ-V3-BC-257 / BC-257: Ghi nhận kiện sai tuyến

- [ ] **T-BC-257-CONTRACT** — Chốt operation «Ghi nhận kiện sai tuyến»: allowed actor R25, guard «Exception, redirect plan, không xóa lịch sử; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource sort/bag/manifest/custody, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-257-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-257-DB** — Review forward schema/index/constraint của dispatch cho sort/bag/manifest/custody nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-257-BE** — Implement «Ghi nhận kiện sai tuyến» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-257-UI** — Tích hợp Internal scoped channel — Ghi nhận kiện sai tuyến; Hub capacity/variance approval với accepted client/channel; action «Ghi nhận kiện sai tuyến», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-257-VERIFY** — Chạy testcase riêng TEST-V3-BC-257-FLOW và AC-BC-257-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-257 trong traceability.

## REQ-V3-BC-259 / BC-259: Ghi nhận kiện hư hỏng khi phân loại

- [ ] **T-BC-259-CONTRACT** — Chốt operation «Ghi nhận kiện hư hỏng khi phân loại»: allowed actor R25, guard «Ảnh/biên bản, custody, escalation; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource sort/bag/manifest/custody, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-259-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-259-DB** — Review forward schema/index/constraint của dispatch cho sort/bag/manifest/custody nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-259-BE** — Implement «Ghi nhận kiện hư hỏng khi phân loại» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-259-UI** — Tích hợp Internal scoped channel — Ghi nhận kiện hư hỏng khi phân loại; Hub capacity/variance approval với accepted client/channel; action «Ghi nhận kiện hư hỏng khi phân loại», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-259-VERIFY** — Chạy testcase riêng TEST-V3-BC-259-FLOW và AC-BC-259-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-259 trong traceability.

## REQ-V3-BC-260 / BC-260: Chuyển kiện/bao vào tuyến vận tải phù hợp

- [ ] **T-BC-260-CONTRACT** — Chốt operation «Chuyển kiện/bao vào tuyến vận tải phù hợp»: allowed actor R25, guard «Manifest/tuyến/đích khớp, không tự tạo trip quyền driver; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource sort/bag/manifest/custody, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-260-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-260-DB** — Review forward schema/index/constraint của dispatch cho sort/bag/manifest/custody nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-260-BE** — Implement «Chuyển kiện/bao vào tuyến vận tải phù hợp» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-260-UI** — Tích hợp Internal scoped channel — Chuyển kiện/bao vào tuyến vận tải phù hợp; Hub capacity/variance approval với accepted client/channel; action «Chuyển kiện/bao vào tuyến vận tải phù hợp», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-260-VERIFY** — Chạy testcase riêng TEST-V3-BC-260-FLOW và AC-BC-260-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-260 trong traceability.

## REQ-V3-BC-261 / BC-261: Hạn chế PII và dữ liệu tài chính Merchant

- [ ] **T-BC-261-CONTRACT** — Chốt operation «Hạn chế PII và dữ liệu tài chính Merchant»: allowed actor R25, guard «Least privilege, test deny access; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource sort/bag/manifest/custody, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-261-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-261-DB** — Chứng minh không ghi effect bị cấm; không thêm positive schema/seed.
- [ ] **T-BC-261-BE** — Implement «Hạn chế PII và dữ liệu tài chính Merchant» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, deny all unauthorized states, không positive branch.
- [ ] **T-BC-261-UI** — Tích hợp Internal scoped channel — Hạn chế PII và dữ liệu tài chính Merchant; Hub capacity/variance approval với accepted client/channel; không nút cấp hành vi cấm; direct API denial vẫn bắt buộc.
- [ ] **T-BC-261-VERIFY** — Chạy testcase riêng TEST-V3-BC-261-DENY và AC-BC-261-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-261 trong traceability.

## REQ-V3-BC-262 / BC-262: Giám sát năng lực xử lý của Hub

- [ ] **T-BC-262-CONTRACT** — Chốt operation «Giám sát năng lực xử lý của Hub»: allowed actor R26, guard «Chỉ hub thuộc quyền; capacity measurable; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource sort/bag/manifest/custody, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-262-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-262-DB** — Review forward schema/index/constraint của dispatch cho sort/bag/manifest/custody nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-262-BE** — Implement «Giám sát năng lực xử lý của Hub» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-262-UI** — Tích hợp Internal scoped channel — Giám sát năng lực xử lý của Hub; Hub capacity/variance approval với accepted client/channel; action «Giám sát năng lực xử lý của Hub», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-262-VERIFY** — Chạy testcase riêng TEST-V3-BC-262-FLOW và AC-BC-262-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-262 trong traceability.

## REQ-V3-BC-263 / BC-263: Theo dõi tồn kiện theo vị trí/thời gian

- [ ] **T-BC-263-CONTRACT** — Chốt operation «Theo dõi tồn kiện theo vị trí/thời gian»: allowed actor R26, guard «Event custody và inventory reconcile; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource sort/bag/manifest/custody, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-263-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-263-DB** — Review forward schema/index/constraint của dispatch cho sort/bag/manifest/custody nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-263-BE** — Implement «Theo dõi tồn kiện theo vị trí/thời gian» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-263-UI** — Tích hợp Internal scoped channel — Theo dõi tồn kiện theo vị trí/thời gian; Hub capacity/variance approval với accepted client/channel; action «Theo dõi tồn kiện theo vị trí/thời gian», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-263-VERIFY** — Chạy testcase riêng TEST-V3-BC-263-FLOW và AC-BC-263-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-263 trong traceability.

## REQ-V3-BC-265 / BC-265: Đối chiếu manifest và xử lý chênh lệch

- [ ] **T-BC-265-CONTRACT** — Chốt operation «Đối chiếu manifest và xử lý chênh lệch»: allowed actor R26, guard «Không sửa âm thầm, chứng cứ/approval; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource sort/bag/manifest/custody, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-265-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-265-DB** — Review forward schema/index/constraint của dispatch cho sort/bag/manifest/custody nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-265-BE** — Implement «Đối chiếu manifest và xử lý chênh lệch» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-265-UI** — Tích hợp Internal scoped channel — Đối chiếu manifest và xử lý chênh lệch; Hub capacity/variance approval với accepted client/channel; action «Đối chiếu manifest và xử lý chênh lệch», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-265-VERIFY** — Chạy testcase riêng TEST-V3-BC-265-FLOW và AC-BC-265-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-265 trong traceability.

## REQ-V3-BC-269 / BC-269: Phê duyệt điều chỉnh tồn theo thẩm quyền

- [ ] **T-BC-269-CONTRACT** — Chốt operation «Phê duyệt điều chỉnh tồn theo thẩm quyền»: allowed actor R26, guard «Reason/evidence/separation of duties, không xóa custody; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource sort/bag/manifest/custody, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-269-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-269-DB** — Review forward schema/index/constraint của dispatch cho sort/bag/manifest/custody nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-269-BE** — Implement «Phê duyệt điều chỉnh tồn theo thẩm quyền» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-269-UI** — Tích hợp Internal scoped channel — Phê duyệt điều chỉnh tồn theo thẩm quyền; Hub capacity/variance approval với accepted client/channel; action «Phê duyệt điều chỉnh tồn theo thẩm quyền», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-269-VERIFY** — Chạy testcase riêng TEST-V3-BC-269-FLOW và AC-BC-269-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-269 trong traceability.


- [ ] T-LEGACY-L012: map từng obligation legacy ở spec sang implementation/negative/compatibility testcase, bao gồm phần consumer; không đóng full spec khi requirement legacy còn chưa được chứng minh hoặc disposition rõ.

## Closure toàn Spec

- [ ] T-L012-FULL: từng REQ có AC/design/task/code/API/DB/UI/test/receipt; lint/typecheck/build/test theo app liên quan, required Architecture/H2/PG18 CI exact revision, report/privacy/owned cleanup và backward compatibility giữ nguyên; no unresolved blocking findings.
- [ ] T-L012-ACCEPT: scoped PR integration PASS → Owner merge → regression trên merge SHA → Owner full-spec acceptance riêng. Chưa tick vì docs promotion.
