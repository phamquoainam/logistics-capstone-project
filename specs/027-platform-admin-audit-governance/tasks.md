# Tasks 027 — platform-admin-audit-governance

NOT_STARTED; thứ tự là contract/fixture → DB → BE/HTTP → UI/channel → verification → full acceptance. Mỗi BC là gói end-to-end nội bộ, không thay phương pháp Spec-by-Spec. Không task nào DONE trong rebuild docs.

## REQ-V3-BC-031 / BC-031: Truy cập admin bằng quyền nghiệp vụ

- [ ] **T-BC-031-CONTRACT** — Chốt operation «Truy cập admin bằng quyền nghiệp vụ»: allowed actor R04, guard «Không mặc nhiên có Identity quyền; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource policy/audit/read projection, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-031-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-031-DB** — Review forward schema/index/constraint của identity cho policy/audit/read projection nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-031-BE** — Implement «Truy cập admin bằng quyền nghiệp vụ» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-031-UI** — Tích hợp Internal scoped channel — Truy cập admin bằng quyền nghiệp vụ; action «Truy cập admin bằng quyền nghiệp vụ», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-031-VERIFY** — Chạy testcase riêng TEST-V3-BC-031-FLOW và AC-BC-031-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-031 trong traceability.

## REQ-V3-BC-036 / BC-036: Tra cứu audit nghiệp vụ liên quan

- [ ] **T-BC-036-CONTRACT** — Chốt operation «Tra cứu audit nghiệp vụ liên quan»: allowed actor R04, guard «Không xóa/sửa audit; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource policy/audit/read projection, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-036-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-036-DB** — Review forward schema/index/constraint của identity cho policy/audit/read projection nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-036-BE** — Implement «Tra cứu audit nghiệp vụ liên quan» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-036-UI** — Tích hợp Internal scoped channel — Tra cứu audit nghiệp vụ liên quan; action «Tra cứu audit nghiệp vụ liên quan», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-036-VERIFY** — Chạy testcase riêng TEST-V3-BC-036-FLOW và AC-BC-036-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-036 trong traceability.

## REQ-V3-BC-043 / BC-043: Xem sự kiện bảo mật Identity

- [ ] **T-BC-043-CONTRACT** — Chốt operation «Xem sự kiện bảo mật Identity»: allowed actor R05, guard «Least privilege; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource policy/audit/read projection, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-043-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-043-DB** — Review forward schema/index/constraint của identity cho policy/audit/read projection nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-043-BE** — Implement «Xem sự kiện bảo mật Identity» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-043-UI** — Tích hợp Internal scoped channel — Xem sự kiện bảo mật Identity; action «Xem sự kiện bảo mật Identity», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-043-VERIFY** — Chạy testcase riêng TEST-V3-BC-043-FLOW và AC-BC-043-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-043 trong traceability.

## REQ-V3-BC-124 / BC-124: Xem audit vận hành của Station

- [ ] **T-BC-124-CONTRACT** — Chốt operation «Xem audit vận hành của Station»: allowed actor R16, guard «Chỉ đọc; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource policy/audit/read projection, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-124-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-124-DB** — Review forward schema/index/constraint của identity cho policy/audit/read projection nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-124-BE** — Implement «Xem audit vận hành của Station» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-124-UI** — Tích hợp Internal scoped channel — Xem audit vận hành của Station; action «Xem audit vận hành của Station», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-124-VERIFY** — Chạy testcase riêng TEST-V3-BC-124-FLOW và AC-BC-124-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-124 trong traceability.

## REQ-V3-BC-125 / BC-125: Đọc audit events đúng phạm vi

- [ ] **T-BC-125-CONTRACT** — Chốt operation «Đọc audit events đúng phạm vi»: allowed actor R17, guard «Read only, không sửa/xóa; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource policy/audit/read projection, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-125-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-125-DB** — Review forward schema/index/constraint của identity cho policy/audit/read projection nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-125-BE** — Implement «Đọc audit events đúng phạm vi» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-125-UI** — Tích hợp Internal scoped channel — Đọc audit events đúng phạm vi; action «Đọc audit events đúng phạm vi», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-125-VERIFY** — Chạy testcase riêng TEST-V3-BC-125-FLOW và AC-BC-125-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-125 trong traceability.

## REQ-V3-BC-128 / BC-128: Xem lịch sử phân quyền và thu hồi quyền

- [ ] **T-BC-128-CONTRACT** — Chốt operation «Xem lịch sử phân quyền và thu hồi quyền»: allowed actor R17, guard «Không quản lý tài khoản; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource policy/audit/read projection, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-128-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-128-DB** — Review forward schema/index/constraint của identity cho policy/audit/read projection nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-128-BE** — Implement «Xem lịch sử phân quyền và thu hồi quyền» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-128-UI** — Tích hợp Internal scoped channel — Xem lịch sử phân quyền và thu hồi quyền; action «Xem lịch sử phân quyền và thu hồi quyền», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-128-VERIFY** — Chạy testcase riêng TEST-V3-BC-128-FLOW và AC-BC-128-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-128 trong traceability.

## REQ-V3-BC-252 / BC-252: Thao tác thay đổi assignment/chấp nhận tổn thất cần quyền riêng

- [ ] **T-BC-252-CONTRACT** — Chốt operation «Thao tác thay đổi assignment/chấp nhận tổn thất cần quyền riêng»: allowed actor R24, guard «Không mặc định có privilege đặc biệt; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource policy/audit/read projection, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-252-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-252-DB** — Chứng minh không ghi effect bị cấm; không thêm positive schema/seed.
- [ ] **T-BC-252-BE** — Implement «Thao tác thay đổi assignment/chấp nhận tổn thất cần quyền riêng» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, deny all unauthorized states, không positive branch.
- [ ] **T-BC-252-UI** — Tích hợp Internal scoped channel — Thao tác thay đổi assignment/chấp nhận tổn thất cần quyền riêng; không nút cấp hành vi cấm; direct API denial vẫn bắt buộc.
- [ ] **T-BC-252-VERIFY** — Chạy testcase riêng TEST-V3-BC-252-DENY và AC-BC-252-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-252 trong traceability.

## REQ-V3-BC-279 / BC-279: Audit quyết định chuyển tuyến và ngoại lệ

- [ ] **T-BC-279-CONTRACT** — Chốt operation «Audit quyết định chuyển tuyến và ngoại lệ»: allowed actor R27, guard «Chỉ quyền được ủy quyền, không xóa log; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource policy/audit/read projection, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-279-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-279-DB** — Review forward schema/index/constraint của identity cho policy/audit/read projection nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-279-BE** — Implement «Audit quyết định chuyển tuyến và ngoại lệ» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-279-UI** — Tích hợp Internal scoped channel — Audit quyết định chuyển tuyến và ngoại lệ; action «Audit quyết định chuyển tuyến và ngoại lệ», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-279-VERIFY** — Chạy testcase riêng TEST-V3-BC-279-FLOW và AC-BC-279-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-279 trong traceability.

## REQ-V3-BC-306 / BC-306: Phân công vai trò nghiệp vụ nhân sự theo ủy quyền

- [ ] **T-BC-306-CONTRACT** — Chốt operation «Phân công vai trò nghiệp vụ nhân sự theo ủy quyền»: allowed actor R04, guard «Không tự cấp admin bảo mật/Identity; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource policy/audit/read projection, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-306-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-306-DB** — Review forward schema/index/constraint của identity cho policy/audit/read projection nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-306-BE** — Implement «Phân công vai trò nghiệp vụ nhân sự theo ủy quyền» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-306-UI** — Tích hợp Internal scoped channel — Phân công vai trò nghiệp vụ nhân sự theo ủy quyền; action «Phân công vai trò nghiệp vụ nhân sự theo ủy quyền», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-306-VERIFY** — Chạy testcase riêng TEST-V3-BC-306-FLOW và AC-BC-306-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-306 trong traceability.

## REQ-V3-BC-308 / BC-308: Xem báo cáo doanh thu, sản lượng, chất lượng dịch vụ

- [ ] **T-BC-308-CONTRACT** — Chốt operation «Xem báo cáo doanh thu, sản lượng, chất lượng dịch vụ»: allowed actor R04, guard «Theo tổ chức/vùng được cấp phép; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource policy/audit/read projection, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-308-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-308-DB** — Review forward schema/index/constraint của identity cho policy/audit/read projection nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-308-BE** — Implement «Xem báo cáo doanh thu, sản lượng, chất lượng dịch vụ» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-308-UI** — Tích hợp Internal scoped channel — Xem báo cáo doanh thu, sản lượng, chất lượng dịch vụ; action «Xem báo cáo doanh thu, sản lượng, chất lượng dịch vụ», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-308-VERIFY** — Chạy testcase riêng TEST-V3-BC-308-FLOW và AC-BC-308-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-308 trong traceability.

## REQ-V3-BC-313 / BC-313: Giám sát thay đổi quyền và sự kiện Identity

- [ ] **T-BC-313-CONTRACT** — Chốt operation «Giám sát thay đổi quyền và sự kiện Identity»: allowed actor R05, guard «Chỉ phạm vi bảo mật, append-only audit; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource policy/audit/read projection, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-313-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-313-DB** — Review forward schema/index/constraint của identity cho policy/audit/read projection nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-313-BE** — Implement «Giám sát thay đổi quyền và sự kiện Identity» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-313-UI** — Tích hợp Internal scoped channel — Giám sát thay đổi quyền và sự kiện Identity; action «Giám sát thay đổi quyền và sự kiện Identity», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-313-VERIFY** — Chạy testcase riêng TEST-V3-BC-313-FLOW và AC-BC-313-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-313 trong traceability.

## REQ-V3-BC-314 / BC-314: Xem lịch sử thay đổi quyền và quản trị

- [ ] **T-BC-314-CONTRACT** — Chốt operation «Xem lịch sử thay đổi quyền và quản trị»: allowed actor R17, guard «Read-only, scoped, immutable source; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource policy/audit/read projection, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-314-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-314-DB** — Review forward schema/index/constraint của identity cho policy/audit/read projection nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-314-BE** — Implement «Xem lịch sử thay đổi quyền và quản trị» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-314-UI** — Tích hợp Internal scoped channel — Xem lịch sử thay đổi quyền và quản trị; action «Xem lịch sử thay đổi quyền và quản trị», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-314-VERIFY** — Chạy testcase riêng TEST-V3-BC-314-FLOW và AC-BC-314-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-314 trong traceability.

## REQ-V3-BC-348 / BC-348: Quản lý tenant và quyền quản trị nền tảng cấp cao

- [ ] **T-BC-348-CONTRACT** — Chốt operation «Quản lý tenant và quyền quản trị nền tảng cấp cao»: allowed actor R31, guard «Privilege boundary, approval và session assurance; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource policy/audit/read projection, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-348-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-348-DB** — Review forward schema/index/constraint của identity cho policy/audit/read projection nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-348-BE** — Implement «Quản lý tenant và quyền quản trị nền tảng cấp cao» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-348-UI** — Tích hợp Internal scoped channel — Quản lý tenant và quyền quản trị nền tảng cấp cao; action «Quản lý tenant và quyền quản trị nền tảng cấp cao», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-348-VERIFY** — Chạy testcase riêng TEST-V3-BC-348-FLOW và AC-BC-348-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-348 trong traceability.

## REQ-V3-BC-349 / BC-349: Cấu hình phân vùng tổ chức ở cấp nền tảng

- [ ] **T-BC-349-CONTRACT** — Chốt operation «Cấu hình phân vùng tổ chức ở cấp nền tảng»: allowed actor R31, guard «Không tự có quyền sở hữu Merchant; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource policy/audit/read projection, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-349-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-349-DB** — Review forward schema/index/constraint của identity cho policy/audit/read projection nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-349-BE** — Implement «Cấu hình phân vùng tổ chức ở cấp nền tảng» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-349-UI** — Tích hợp Internal scoped channel — Cấu hình phân vùng tổ chức ở cấp nền tảng; action «Cấu hình phân vùng tổ chức ở cấp nền tảng», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-349-VERIFY** — Chạy testcase riêng TEST-V3-BC-349-FLOW và AC-BC-349-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-349 trong traceability.

## REQ-V3-BC-350 / BC-350: Quản lý feature flags theo kiểm soát thay đổi

- [ ] **T-BC-350-CONTRACT** — Chốt operation «Quản lý feature flags theo kiểm soát thay đổi»: allowed actor R31, guard «Audit, rollback và rollout scope; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource policy/audit/read projection, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-350-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-350-DB** — Review forward schema/index/constraint của identity cho policy/audit/read projection nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-350-BE** — Implement «Quản lý feature flags theo kiểm soát thay đổi» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-350-UI** — Tích hợp Internal scoped channel — Quản lý feature flags theo kiểm soát thay đổi; action «Quản lý feature flags theo kiểm soát thay đổi», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-350-VERIFY** — Chạy testcase riêng TEST-V3-BC-350-FLOW và AC-BC-350-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-350 trong traceability.

## REQ-V3-BC-351 / BC-351: Quản lý chính sách nền tảng được phân quyền

- [ ] **T-BC-351-CONTRACT** — Chốt operation «Quản lý chính sách nền tảng được phân quyền»: allowed actor R31, guard «Policy versioning và phê duyệt; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource policy/audit/read projection, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-351-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-351-DB** — Review forward schema/index/constraint của identity cho policy/audit/read projection nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-351-BE** — Implement «Quản lý chính sách nền tảng được phân quyền» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-351-UI** — Tích hợp Internal scoped channel — Quản lý chính sách nền tảng được phân quyền; action «Quản lý chính sách nền tảng được phân quyền», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-351-VERIFY** — Chạy testcase riêng TEST-V3-BC-351-FLOW và AC-BC-351-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-351 trong traceability.

## REQ-V3-BC-352 / BC-352: Phê duyệt tác vụ quản trị đặc biệt theo quy trình

- [ ] **T-BC-352-CONTRACT** — Chốt operation «Phê duyệt tác vụ quản trị đặc biệt theo quy trình»: allowed actor R31, guard «Dual approval khi policy yêu cầu; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource policy/audit/read projection, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-352-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-352-DB** — Review forward schema/index/constraint của identity cho policy/audit/read projection nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-352-BE** — Implement «Phê duyệt tác vụ quản trị đặc biệt theo quy trình» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-352-UI** — Tích hợp Internal scoped channel — Phê duyệt tác vụ quản trị đặc biệt theo quy trình; action «Phê duyệt tác vụ quản trị đặc biệt theo quy trình», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-352-VERIFY** — Chạy testcase riêng TEST-V3-BC-352-FLOW và AC-BC-352-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-352 trong traceability.

## REQ-V3-BC-353 / BC-353: Giám sát thao tác đặc quyền và break-glass

- [ ] **T-BC-353-CONTRACT** — Chốt operation «Giám sát thao tác đặc quyền và break-glass»: allowed actor R31, guard «Reason, expiry, immutable audit; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource policy/audit/read projection, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-353-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-353-DB** — Review forward schema/index/constraint của identity cho policy/audit/read projection nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-353-BE** — Implement «Giám sát thao tác đặc quyền và break-glass» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-353-UI** — Tích hợp Internal scoped channel — Giám sát thao tác đặc quyền và break-glass; action «Giám sát thao tác đặc quyền và break-glass», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-353-VERIFY** — Chạy testcase riêng TEST-V3-BC-353-FLOW và AC-BC-353-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-353 trong traceability.

## REQ-V3-BC-361 / BC-361: Vận hành production có kiểm soát

- [ ] **T-BC-361-CONTRACT** — Chốt operation «Vận hành production có kiểm soát»: allowed actor R32, guard «Approval, least privilege, audit; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource policy/audit/read projection, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-361-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-361-DB** — Review forward schema/index/constraint của identity cho policy/audit/read projection nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-361-BE** — Implement «Vận hành production có kiểm soát» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-361-UI** — Tích hợp Internal scoped channel — Vận hành production có kiểm soát; action «Vận hành production có kiểm soát», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-361-VERIFY** — Chạy testcase riêng TEST-V3-BC-361-FLOW và AC-BC-361-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-361 trong traceability.

## REQ-V3-BC-362 / BC-362: Không impersonate Merchant tùy ý

- [ ] **T-BC-362-CONTRACT** — Chốt operation «Không impersonate Merchant tùy ý»: allowed actor R32, guard «Session/account controls và evidence; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource policy/audit/read projection, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-362-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-362-DB** — Chứng minh không ghi effect bị cấm; không thêm positive schema/seed.
- [ ] **T-BC-362-BE** — Implement «Không impersonate Merchant tùy ý» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, deny all unauthorized states, không positive branch.
- [ ] **T-BC-362-UI** — Tích hợp Internal scoped channel — Không impersonate Merchant tùy ý; không nút cấp hành vi cấm; direct API denial vẫn bắt buộc.
- [ ] **T-BC-362-VERIFY** — Chạy testcase riêng TEST-V3-BC-362-DENY và AC-BC-362-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-362 trong traceability.


- [ ] T-LEGACY-L027: map từng obligation legacy ở spec sang implementation/negative/compatibility testcase, bao gồm phần consumer; không đóng full spec khi requirement legacy còn chưa được chứng minh hoặc disposition rõ.

## Closure toàn Spec

- [ ] T-L027-FULL: từng REQ có AC/design/task/code/API/DB/UI/test/receipt; lint/typecheck/build/test theo app liên quan, required Architecture/H2/PG18 CI exact revision, report/privacy/owned cleanup và backward compatibility giữ nguyên; no unresolved blocking findings.
- [ ] T-L027-ACCEPT: scoped PR integration PASS → Owner merge → regression trên merge SHA → Owner full-spec acceptance riêng. Chưa tick vì docs promotion.
