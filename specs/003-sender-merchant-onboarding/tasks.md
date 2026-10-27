# Tasks 003 — sender-merchant-onboarding

NOT_STARTED; thứ tự là contract/fixture → DB → BE/HTTP → UI/channel → verification → full acceptance. Mỗi BC là gói end-to-end nội bộ, không thay phương pháp Spec-by-Spec. Không task nào DONE trong rebuild docs.

## REQ-V3-BC-007 / BC-007: Dùng tài khoản với Merchant membership ACTIVE

- [ ] **T-BC-007-CONTRACT** — Chốt operation «Dùng tài khoản với Merchant membership ACTIVE»: allowed actor R02, guard «Đúng org và trạng thái; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource current organization membership/context, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-007-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-007-DB** — Review forward schema/index/constraint của merchant cho current organization membership/context nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-007-BE** — Implement «Dùng tài khoản với Merchant membership ACTIVE» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-007-UI** — Tích hợp Merchant current context — Dùng tài khoản với Merchant membership ACTIVE; action «Dùng tài khoản với Merchant membership ACTIVE», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-007-VERIFY** — Chạy testcase riêng TEST-V3-BC-007-FLOW và AC-BC-007-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-007 trong traceability.

## REQ-V3-BC-008 / BC-008: Quản lý hồ sơ và nhân viên Merchant

- [ ] **T-BC-008-CONTRACT** — Chốt operation «Quản lý hồ sơ và nhân viên Merchant»: allowed actor R02, guard «Đúng quyền quản trị tổ chức; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource organization business profile and scoped staff directory, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-008-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-008-DB** — Review forward schema/index/constraint của merchant cho organization business profile and scoped staff directory nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-008-BE** — Implement «Quản lý hồ sơ và nhân viên Merchant» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-008-UI** — Tích hợp Merchant current context — Quản lý hồ sơ và nhân viên Merchant; action «Quản lý hồ sơ và nhân viên Merchant», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-008-VERIFY** — Chạy testcase riêng TEST-V3-BC-008-FLOW và AC-BC-008-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-008 trong traceability.


## REQ-V3-BC-020 / BC-020: Truy cập qua membership Merchant

- [ ] **T-BC-020-CONTRACT** — Chốt operation «Truy cập qua membership Merchant»: allowed actor R03, guard «Không tự nâng quyền; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource current delegated business membership/context, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-020-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-020-DB** — Review forward schema/index/constraint của merchant cho current delegated business membership/context nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-020-BE** — Implement «Truy cập qua membership Merchant» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-020-UI** — Tích hợp Merchant current context — Truy cập qua membership Merchant; action «Truy cập qua membership Merchant», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-020-VERIFY** — Chạy testcase riêng TEST-V3-BC-020-FLOW và AC-BC-020-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-020 trong traceability.


## REQ-V3-BC-032 / BC-032: Duyệt/giám sát Merchant theo policy

- [ ] **T-BC-032-CONTRACT** — Chốt operation «Duyệt/giám sát Merchant theo policy»: allowed actor R04, guard «Phạm vi org/region; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource application review decision/org lifecycle, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-032-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-032-DB** — Review forward schema/index/constraint của merchant cho application review decision/org lifecycle nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-032-BE** — Implement «Duyệt/giám sát Merchant theo policy» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-032-UI** — Tích hợp Internal scoped channel — Duyệt/giám sát Merchant theo policy; action «Duyệt/giám sát Merchant theo policy», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-032-VERIFY** — Chạy testcase riêng TEST-V3-BC-032-FLOW và AC-BC-032-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-032 trong traceability.

## REQ-V3-BC-045 / BC-045: Sửa nội dung nghiệp vụ Merchant

- [ ] **T-BC-045-CONTRACT** — Chốt operation «Sửa nội dung nghiệp vụ Merchant»: allowed actor R05, guard «Không tự có quyền Logistics; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource Merchant business records denied to Identity-only authority, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-045-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-045-DB** — Chứng minh không ghi effect bị cấm; không thêm positive schema/seed.
- [ ] **T-BC-045-BE** — Implement «Sửa nội dung nghiệp vụ Merchant» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, deny all unauthorized states, không positive branch.
- [ ] **T-BC-045-UI** — Tích hợp Internal scoped channel — Sửa nội dung nghiệp vụ Merchant; không nút cấp hành vi cấm; direct API denial vẫn bắt buộc.
- [ ] **T-BC-045-VERIFY** — Chạy testcase riêng TEST-V3-BC-045-DENY và AC-BC-045-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-045 trong traceability.

## REQ-V3-BC-132 / BC-132: Đi đến đăng ký một tài khoản khách hàng cá nhân

- [ ] **T-BC-132-CONTRACT** — Chốt operation «Đi đến đăng ký một tài khoản khách hàng cá nhân»: allowed actor R01, guard «Public entry; xác minh Identity; không chọn loại tài khoản, không tự cấp role/Owner; permission code DECISION_REQUIRED», resource public personal-registration entry; no business record from navigation, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-132-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-132-DB** — Review forward schema/index/constraint của merchant cho public personal-registration entry; no business record from navigation nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-132-BE** — Implement «Đi đến đăng ký một tài khoản khách hàng cá nhân» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-132-UI** — Tích hợp Public — personal registration/login entry only; action «Đi đến đăng ký một tài khoản khách hàng cá nhân», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-132-VERIFY** — Chạy testcase riêng TEST-V3-BC-132-FLOW và AC-BC-132-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-132 trong traceability.

## REQ-V3-BC-133 / BC-133: Nộp và theo dõi đơn trở thành Merchant từ workspace cá nhân sau đăng nhập

- [ ] **T-BC-133-CONTRACT** — Chốt operation «Nộp và theo dõi đơn trở thành Merchant từ workspace cá nhân sau đăng nhập»: allowed actor R18, guard «Account verified và eligible hiện hành; không Guest submit, không Owner trước approval; permission code DECISION_REQUIRED», resource owned application/evidence revisions/review status, no Owner before approval, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-133-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-133-DB** — Review forward schema/index/constraint của merchant cho owned application/evidence revisions/review status, no Owner before approval nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-133-BE** — Implement «Nộp và theo dõi đơn trở thành Merchant từ workspace cá nhân sau đăng nhập» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-133-UI** — Tích hợp Personal authenticated — Merchant application and application status; action «Nộp và theo dõi đơn trở thành Merchant từ workspace cá nhân sau đăng nhập», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-133-VERIFY** — Chạy testcase riêng TEST-V3-BC-133-FLOW và AC-BC-133-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-133 trong traceability.

## REQ-V3-BC-134 / BC-134: Đăng ký/đăng nhập tài khoản cá nhân

- [ ] **T-BC-134-CONTRACT** — Chốt operation «Đăng ký/đăng nhập tài khoản cá nhân»: allowed actor R18, guard «Không buộc Merchant membership; xác minh/chống giả mạo; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource Identity customer verification/account/session; Merchant orchestrates personal onboarding, no credential copy, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-134-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-134-DB** — Review forward schema/index/constraint của merchant cho Identity customer verification/account/session; Merchant orchestrates personal onboarding, no credential copy nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-134-BE** — Implement «Đăng ký/đăng nhập tài khoản cá nhân» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-134-UI** — Tích hợp Public registration/verification/login → Personal default; action «Đăng ký/đăng nhập tài khoản cá nhân», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-134-VERIFY** — Chạy testcase riêng TEST-V3-BC-134-FLOW và AC-BC-134-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-134 trong traceability.

## REQ-V3-BC-135 / BC-135: Quản lý hồ sơ, tên, liên hệ, trạng thái tài khoản

- [ ] **T-BC-135-CONTRACT** — Chốt operation «Quản lý hồ sơ, tên, liên hệ, trạng thái tài khoản»: allowed actor R18, guard «Chỉ dữ liệu bản thân; thay đổi nhạy cảm cần xác minh; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource self business profile/contact preferences; Identity security contacts authoritative, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-135-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-135-DB** — Review forward schema/index/constraint của merchant cho self business profile/contact preferences; Identity security contacts authoritative nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-135-BE** — Implement «Quản lý hồ sơ, tên, liên hệ, trạng thái tài khoản» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-135-UI** — Tích hợp Personal — Quản lý hồ sơ, tên, liên hệ, trạng thái tài khoản; action «Quản lý hồ sơ, tên, liên hệ, trạng thái tài khoản», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-135-VERIFY** — Chạy testcase riêng TEST-V3-BC-135-FLOW và AC-BC-135-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-135 trong traceability.

## REQ-V3-BC-157 / BC-157: Cập nhật thông tin pháp nhân, tên cửa hàng, mã số thuế và cấu hình Merchant

- [ ] **T-BC-157-CONTRACT** — Chốt operation «Cập nhật thông tin pháp nhân, tên cửa hàng, mã số thuế và cấu hình Merchant»: allowed actor R02, guard «Chỉ Merchant sở hữu; kiểm soát thay đổi nhạy cảm; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource organization legal/business profile, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-157-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-157-DB** — Review forward schema/index/constraint của merchant cho organization legal/business profile nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-157-BE** — Implement «Cập nhật thông tin pháp nhân, tên cửa hàng, mã số thuế và cấu hình Merchant» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-157-UI** — Tích hợp Merchant current context — Cập nhật thông tin pháp nhân, tên cửa hàng, mã số thuế và cấu hình Merchant; action «Cập nhật thông tin pháp nhân, tên cửa hàng, mã số thuế và cấu hình Merchant», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-157-VERIFY** — Chạy testcase riêng TEST-V3-BC-157-FLOW và AC-BC-157-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-157 trong traceability.

## REQ-V3-BC-158 / BC-158: Mời/thêm/khóa nhân viên, quản lý membership và quyền tổ chức

- [ ] **T-BC-158-CONTRACT** — Chốt operation «Mời/thêm/khóa nhân viên, quản lý membership và quyền tổ chức»: allowed actor R02, guard «Không tự cấp quyền nền tảng; phân quyền trong giới hạn; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource membership invitation/current member status, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-158-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-158-DB** — Review forward schema/index/constraint của merchant cho membership invitation/current member status nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-158-BE** — Implement «Mời/thêm/khóa nhân viên, quản lý membership và quyền tổ chức» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-158-UI** — Tích hợp Merchant current context — Mời/thêm/khóa nhân viên, quản lý membership và quyền tổ chức; action «Mời/thêm/khóa nhân viên, quản lý membership và quyền tổ chức», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-158-VERIFY** — Chạy testcase riêng TEST-V3-BC-158-FLOW và AC-BC-158-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-158 trong traceability.

## REQ-V3-BC-159 / BC-159: Quản lý nhiều cửa hàng/kho, cấu hình dịch vụ và quy tắc mặc định

- [ ] **T-BC-159-CONTRACT** — Chốt operation «Quản lý nhiều cửa hàng/kho, cấu hình dịch vụ và quy tắc mặc định»: allowed actor R02, guard «Merchant scope; chính sách phiên bản; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource branch/facility metadata and versioned defaults referencing supported catalog, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-159-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-159-DB** — Review forward schema/index/constraint của merchant cho branch/facility metadata and versioned defaults referencing supported catalog nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-159-BE** — Implement «Quản lý nhiều cửa hàng/kho, cấu hình dịch vụ và quy tắc mặc định» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-159-UI** — Tích hợp Merchant current context — Quản lý nhiều cửa hàng/kho, cấu hình dịch vụ và quy tắc mặc định; action «Quản lý nhiều cửa hàng/kho, cấu hình dịch vụ và quy tắc mặc định», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-159-VERIFY** — Chạy testcase riêng TEST-V3-BC-159-FLOW và AC-BC-159-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-159 trong traceability.

## REQ-V3-BC-166 / BC-166: Quản lý nhân sự và quyền hạn được Owner ủy quyền

- [ ] **T-BC-166-CONTRACT** — Chốt operation «Quản lý nhân sự và quyền hạn được Owner ủy quyền»: allowed actor R19, guard «Không vượt delegated scope; audit thay đổi; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource current delegated staff/permission assignment, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-166-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-166-DB** — Review forward schema/index/constraint của merchant cho current delegated staff/permission assignment nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-166-BE** — Implement «Quản lý nhân sự và quyền hạn được Owner ủy quyền» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-166-UI** — Tích hợp Merchant current context — Quản lý nhân sự và quyền hạn được Owner ủy quyền; action «Quản lý nhân sự và quyền hạn được Owner ủy quyền», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-166-VERIFY** — Chạy testcase riêng TEST-V3-BC-166-FLOW và AC-BC-166-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-166 trong traceability.

## REQ-V3-BC-167 / BC-167: Điều hành cửa hàng/kho và cấu hình vận hành được giao

- [ ] **T-BC-167-CONTRACT** — Chốt operation «Điều hành cửa hàng/kho và cấu hình vận hành được giao»: allowed actor R19, guard «Không đổi Merchant owner hoặc ngân hàng COD; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource delegated branch/facility/defaults, no Owner/bank edits, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-167-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-167-DB** — Review forward schema/index/constraint của merchant cho delegated branch/facility/defaults, no Owner/bank edits nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-167-BE** — Implement «Điều hành cửa hàng/kho và cấu hình vận hành được giao» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-167-UI** — Tích hợp Merchant current context — Điều hành cửa hàng/kho và cấu hình vận hành được giao; action «Điều hành cửa hàng/kho và cấu hình vận hành được giao», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-167-VERIFY** — Chạy testcase riêng TEST-V3-BC-167-FLOW và AC-BC-167-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-167 trong traceability.


## Closure toàn Spec

- [ ] T-L003-FULL: từng REQ có AC/design/task/code/API/DB/UI/test/receipt; lint/typecheck/build/test theo app liên quan, required Architecture/H2/PG18 CI exact revision, report/privacy/owned cleanup và backward compatibility giữ nguyên; no unresolved blocking findings.
- [ ] T-L003-ACCEPT: scoped PR integration PASS → Owner merge → regression trên merge SHA → Owner full-spec acceptance riêng. Chưa tick vì docs promotion.


## Gói bổ sung cụ thể — chốt trước khi code, không tick DONE trong docs rebuild

- [ ] **T003-01-IAM-CONTRACT** — close IAM-CUSTOMER-01/CONTEXT-02/INVITE-03: before/after accepted Identity contracts, distinct customer admission eligibility, trusted current-subject bridge, session-owned TX/revocation protocol; fixtures không customer SYSTEM grant, không normalize legacy username/password. Trace S003-ACCOUNT/MEMBERSHIP.
- [ ] **T003-02-POLICY** — review entity-type/evidence matrix, business duplicate keys, initial/last Owner, multi-org policy, closure obligations, invitation expiry và delegation bounds; wire-independent state matrix; manual sandbox label. Trace S003-APPLICATION/ACTIVATION/MEMBERSHIP/FACILITIES.
- [ ] **T003-03-API** — define exact operations/DTO/errors/conditional versions/keys/retention/query paging for all design operations; input allowlist blocks owner/role/status injection; generate client only after accepted contract. Map từng BC + S003 requirement vào fixtures.
- [ ] **T003-04-PG** — review forward migrations sau V6 cho approved Merchant/Identity scope; separate owner DML, scoped FK/unique initial Owner/business key/receipt, evidence revisions, locks; PG18 cold/upgrade/privileges/constraint/race/old-build compatibility tests. No schema implementation authorized by current docs task.
- [ ] **T003-05-SELF** — implement verified personal onboarding consumption, self business profile and contact/security separation, current contexts; HTTP production guard deny unverified/disabled/wrong-person. Trace BC132/134/135, S003-ACCOUNT/PROFILE.
- [ ] **T003-06-APPLICATION** — implement own draft/save/submit/resubmit/status/withdraw, entity validation/duplicate review/private evidence lifecycle; PG rollback and no Owner from submission. Trace BC133/157, S003-APPLICATION.
- [ ] **T003-07-REVIEW** — implement scoped review queue/request-information/reject/approve, independent reviewer check and atomic org+initial Owner+audit/receipt; fault/race/replay/orphan tests. Trace BC032/007/008, S003-ACTIVATION.
- [ ] **T003-08-STAFF** — implement invite/accept/reject/expire/revoke, membership suspend/reinstate/leave and delegated replacement; current inviter rights and last-Owner lock, unrelated personal/B org survives. Trace BC020/158/166, S003-MEMBERSHIP.
- [ ] **T003-09-ORG** — profile/facilities/versioned defaults with published catalog minimum contract, org suspend/reinstate/closure obligation checks; Manager no Owner/bank changes; no pickup duplicate. Trace BC157/159/167, S003-FACILITIES.
- [ ] **T003-10-WEB** — real registration/verification/login/profile/application/status/context/org/staff/review screens; typed client and safe cache switch/in-flight cancellation; sensitive confirmation/unsaved input/keyboard/responsive/error/re-auth. Trace all user-facing003 requirements.
- [ ] **T003-11-E2E** — real browser+Backend+PG journeys personal→application→needs-info→approval→org→staff→revoke; malicious Guest submit, cross-tenant, forged role, rejected/suspended org personal access, expiry/replay/unknown recover, concurrent activation/last Owner and audit rollback. Evidence must identify each AC and tested revision.
- [ ] **T003-12-FULL-ACCEPTANCE** — verify all assigned BC+S003+applicable legacy obligations, downstream minimum catalog contract, unchanged Foundation/Identity regressions, no fake email/verification; lint/typecheck/build/contract/unit/architecture/H2/PG/browser+required CI exact PR+merge SHA; traceability/runbooks/findings; Owner full003 acceptance separate. Portal milestone remains NOT_STARTED until cross-spec gates.


- [ ] **T-AB-ENTRY** — REQ-AB-ENTRY: close approved operation/state/data/UX/config contract + fixtures; implement owner API/DB/HTTP and integrated Public/personal entry; unit/PG/security/browser or verified channel assert «Guest cannot submit application; verified personal login default; deep links rechecked; no role grants»; TEST-AB-ENTRY with source+integration receipts. Dependencies L002; do not mark full spec when missing.


- [ ] **T-AB-CONTEXT** — REQ-AB-CONTEXT: close approved operation/state/data/UX/config contract + fixtures; implement owner API/DB/HTTP and integrated Context switch/profile/staff; unit/PG/security/browser or verified channel assert «A→B switch cannot show A response in B; revoked org cleared and current request denied; personal still valid»; TEST-AB-CONTEXT with source+integration receipts. Dependencies L002; do not mark full spec when missing.
