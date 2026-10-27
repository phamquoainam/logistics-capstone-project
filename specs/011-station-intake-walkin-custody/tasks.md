# Tasks 011 — station-intake-walkin-custody

NOT_STARTED; thứ tự là contract/fixture → DB → BE/HTTP → UI/channel → verification → full acceptance. Mỗi BC là gói end-to-end nội bộ, không thay phương pháp Spec-by-Spec. Không task nào DONE trong rebuild docs.

## REQ-V3-BC-055 / BC-055: Điều phối qua nhiều Station

- [ ] **T-BC-055-CONTRACT** — Chốt operation «Điều phối qua nhiều Station»: allowed actor R06, guard «Policy phạm vi bưu cục; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource Station/shift/staff/custody/manifest, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-055-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-055-DB** — Review forward schema/index/constraint của dispatch cho Station/shift/staff/custody/manifest nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-055-BE** — Implement «Điều phối qua nhiều Station» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-055-UI** — Tích hợp Internal scoped channel — Điều phối qua nhiều Station; action «Điều phối qua nhiều Station», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-055-VERIFY** — Chạy testcase riêng TEST-V3-BC-055-FLOW và AC-BC-055-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-055 trong traceability.

## REQ-V3-BC-059 / BC-059: Bàn giao kiện cho Station

- [ ] **T-BC-059-CONTRACT** — Chốt operation «Bàn giao kiện cho Station»: allowed actor R07, guard «Bên nhận xác nhận custody; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource Station/shift/staff/custody/manifest, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-059-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-059-DB** — Review forward schema/index/constraint của dispatch cho Station/shift/staff/custody/manifest nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-059-BE** — Implement «Bàn giao kiện cho Station» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-059-UI** — Tích hợp Internal scoped channel — Bàn giao kiện cho Station; action «Bàn giao kiện cho Station», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-059-VERIFY** — Chạy testcase riêng TEST-V3-BC-059-FLOW và AC-BC-059-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-059 trong traceability.

## REQ-V3-BC-063 / BC-063: Nhận kiện và xác nhận custody tại Station

- [ ] **T-BC-063-CONTRACT** — Chốt operation «Nhận kiện và xác nhận custody tại Station»: allowed actor R08, guard «Scan, người nhận, timestamp; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource Station/shift/staff/custody/manifest, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-063-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-063-DB** — Review forward schema/index/constraint của dispatch cho Station/shift/staff/custody/manifest nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-063-BE** — Implement «Nhận kiện và xác nhận custody tại Station» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-063-UI** — Tích hợp Internal scoped channel — Nhận kiện và xác nhận custody tại Station; action «Nhận kiện và xác nhận custody tại Station», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-063-VERIFY** — Chạy testcase riêng TEST-V3-BC-063-FLOW và AC-BC-063-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-063 trong traceability.

## REQ-V3-BC-065 / BC-065: Nhập/xuất kiện và lập manifest

- [ ] **T-BC-065-CONTRACT** — Chốt operation «Nhập/xuất kiện và lập manifest»: allowed actor R08, guard «Không ghi đè lịch sử; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource Station/shift/staff/custody/manifest, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-065-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-065-DB** — Review forward schema/index/constraint của dispatch cho Station/shift/staff/custody/manifest nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-065-BE** — Implement «Nhập/xuất kiện và lập manifest» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-065-UI** — Tích hợp Internal scoped channel — Nhập/xuất kiện và lập manifest; action «Nhập/xuất kiện và lập manifest», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-065-VERIFY** — Chạy testcase riêng TEST-V3-BC-065-FLOW và AC-BC-065-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-065 trong traceability.

## REQ-V3-BC-066 / BC-066: Bàn giao kiện cho courier/tuyến

- [ ] **T-BC-066-CONTRACT** — Chốt operation «Bàn giao kiện cho courier/tuyến»: allowed actor R08, guard «Hai phía xác nhận khi cần; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource Station/shift/staff/custody/manifest, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-066-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-066-DB** — Review forward schema/index/constraint của dispatch cho Station/shift/staff/custody/manifest nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-066-BE** — Implement «Bàn giao kiện cho courier/tuyến» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-066-UI** — Tích hợp Internal scoped channel — Bàn giao kiện cho courier/tuyến; action «Bàn giao kiện cho courier/tuyến», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-066-VERIFY** — Chạy testcase riêng TEST-V3-BC-066-FLOW và AC-BC-066-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-066 trong traceability.

## REQ-V3-BC-069 / BC-069: Kiểm kê tồn chi tiết, phân loại tự động

- [ ] **T-BC-069-CONTRACT** — Chốt operation «Kiểm kê tồn chi tiết, phân loại tự động»: allowed actor R08, guard «Có quy trình chênh lệch; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource Station/shift/staff/custody/manifest, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-069-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-069-DB** — Review forward schema/index/constraint của dispatch cho Station/shift/staff/custody/manifest nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-069-BE** — Implement «Kiểm kê tồn chi tiết, phân loại tự động» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-069-UI** — Tích hợp Internal scoped channel — Kiểm kê tồn chi tiết, phân loại tự động; action «Kiểm kê tồn chi tiết, phân loại tự động», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-069-VERIFY** — Chạy testcase riêng TEST-V3-BC-069-FLOW và AC-BC-069-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-069 trong traceability.

## REQ-V3-BC-118 / BC-118: Giám sát tồn kiện, nhập/xuất, manifest

- [ ] **T-BC-118-CONTRACT** — Chốt operation «Giám sát tồn kiện, nhập/xuất, manifest»: allowed actor R16, guard «Đúng Station scope; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource Station/shift/staff/custody/manifest, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-118-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-118-DB** — Review forward schema/index/constraint của dispatch cho Station/shift/staff/custody/manifest nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-118-BE** — Implement «Giám sát tồn kiện, nhập/xuất, manifest» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-118-UI** — Tích hợp Internal scoped channel — Giám sát tồn kiện, nhập/xuất, manifest; action «Giám sát tồn kiện, nhập/xuất, manifest», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-118-VERIFY** — Chạy testcase riêng TEST-V3-BC-118-FLOW và AC-BC-118-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-118 trong traceability.

## REQ-V3-BC-123 / BC-123: Duyệt sai lệch kho có bằng chứng

- [ ] **T-BC-123-CONTRACT** — Chốt operation «Duyệt sai lệch kho có bằng chứng»: allowed actor R16, guard «Có audit/approval; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource Station/shift/staff/custody/manifest, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-123-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-123-DB** — Review forward schema/index/constraint của dispatch cho Station/shift/staff/custody/manifest nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-123-BE** — Implement «Duyệt sai lệch kho có bằng chứng» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-123-UI** — Tích hợp Internal scoped channel — Duyệt sai lệch kho có bằng chứng; action «Duyệt sai lệch kho có bằng chứng», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-123-VERIFY** — Chạy testcase riêng TEST-V3-BC-123-FLOW và AC-BC-123-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-123 trong traceability.

## REQ-V3-BC-149 / BC-149: Tiếp nhận tại quầy và lập Shipment cho Walk-in Sender

- [ ] **T-BC-149-CONTRACT** — Chốt operation «Tiếp nhận tại quầy và lập Shipment cho Walk-in Sender»: allowed actor R08, guard «Chỉ nhân viên được phân công, xác minh người gửi; KHÔNG phải Guest API; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource Station/shift/staff/custody/manifest, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-149-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-149-DB** — Review forward schema/index/constraint của dispatch cho Station/shift/staff/custody/manifest nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-149-BE** — Implement «Tiếp nhận tại quầy và lập Shipment cho Walk-in Sender» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-149-UI** — Tích hợp Internal scoped channel — Tiếp nhận tại quầy và lập Shipment cho Walk-in Sender; action «Tiếp nhận tại quầy và lập Shipment cho Walk-in Sender», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-149-VERIFY** — Chạy testcase riêng TEST-V3-BC-149-FLOW và AC-BC-149-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-149 trong traceability.

## REQ-V3-BC-152 / BC-152: Nhập custody kiện walk-in tại Station/Hub phục vụ giao dịch

- [ ] **T-BC-152-CONTRACT** — Chốt operation «Nhập custody kiện walk-in tại Station/Hub phục vụ giao dịch»: allowed actor R08, guard «Đúng Station, nguồn walk-in, manifest/handover; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource Station/shift/staff/custody/manifest, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-152-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-152-DB** — Review forward schema/index/constraint của dispatch cho Station/shift/staff/custody/manifest nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-152-BE** — Implement «Nhập custody kiện walk-in tại Station/Hub phục vụ giao dịch» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-152-UI** — Tích hợp Internal scoped channel — Nhập custody kiện walk-in tại Station/Hub phục vụ giao dịch; action «Nhập custody kiện walk-in tại Station/Hub phục vụ giao dịch», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-152-VERIFY** — Chạy testcase riêng TEST-V3-BC-152-FLOW và AC-BC-152-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-152 trong traceability.

## REQ-V3-BC-216 / BC-216: Nhận kiện trực tiếp từ khách walk-in hoặc Courier

- [ ] **T-BC-216-CONTRACT** — Chốt operation «Nhận kiện trực tiếp từ khách walk-in hoặc Courier»: allowed actor R08, guard «Phân biệt walk-in/assigned handover; actor, thời điểm, số lượng, chứng cứ; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource Station/shift/staff/custody/manifest, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-216-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-216-DB** — Review forward schema/index/constraint của dispatch cho Station/shift/staff/custody/manifest nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-216-BE** — Implement «Nhận kiện trực tiếp từ khách walk-in hoặc Courier» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-216-UI** — Tích hợp Internal scoped channel — Nhận kiện trực tiếp từ khách walk-in hoặc Courier; action «Nhận kiện trực tiếp từ khách walk-in hoặc Courier», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-216-VERIFY** — Chạy testcase riêng TEST-V3-BC-216-FLOW và AC-BC-216-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-216 trong traceability.

## REQ-V3-BC-217 / BC-217: Xác nhận kiện thực nhận theo manifest và kiểm đếm

- [ ] **T-BC-217-CONTRACT** — Chốt operation «Xác nhận kiện thực nhận theo manifest và kiểm đếm»: allowed actor R08, guard «Lệch manifest mở exception, không mặc định khớp; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource Station/shift/staff/custody/manifest, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-217-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-217-DB** — Review forward schema/index/constraint của dispatch cho Station/shift/staff/custody/manifest nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-217-BE** — Implement «Xác nhận kiện thực nhận theo manifest và kiểm đếm» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-217-UI** — Tích hợp Internal scoped channel — Xác nhận kiện thực nhận theo manifest và kiểm đếm; action «Xác nhận kiện thực nhận theo manifest và kiểm đếm», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-217-VERIFY** — Chạy testcase riêng TEST-V3-BC-217-FLOW và AC-BC-217-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-217 trong traceability.

## REQ-V3-BC-219 / BC-219: Ghi nhận tồn kho và vị trí vật lý của kiện

- [ ] **T-BC-219-CONTRACT** — Chốt operation «Ghi nhận tồn kho và vị trí vật lý của kiện»: allowed actor R08, guard «Station custody và location event bất biến; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource Station/shift/staff/custody/manifest, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-219-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-219-DB** — Review forward schema/index/constraint của dispatch cho Station/shift/staff/custody/manifest nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-219-BE** — Implement «Ghi nhận tồn kho và vị trí vật lý của kiện» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-219-UI** — Tích hợp Internal scoped channel — Ghi nhận tồn kho và vị trí vật lý của kiện; action «Ghi nhận tồn kho và vị trí vật lý của kiện», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-219-VERIFY** — Chạy testcase riêng TEST-V3-BC-219-FLOW và AC-BC-219-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-219 trong traceability.

## REQ-V3-BC-220 / BC-220: Xử lý sai lệch, tồn lâu và kiện không xác định

- [ ] **T-BC-220-CONTRACT** — Chốt operation «Xử lý sai lệch, tồn lâu và kiện không xác định»: allowed actor R08, guard «Lý do, evidence và escalation; không âm thầm sửa kho; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource Station/shift/staff/custody/manifest, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-220-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-220-DB** — Review forward schema/index/constraint của dispatch cho Station/shift/staff/custody/manifest nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-220-BE** — Implement «Xử lý sai lệch, tồn lâu và kiện không xác định» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-220-UI** — Tích hợp Internal scoped channel — Xử lý sai lệch, tồn lâu và kiện không xác định; action «Xử lý sai lệch, tồn lâu và kiện không xác định», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-220-VERIFY** — Chạy testcase riêng TEST-V3-BC-220-FLOW và AC-BC-220-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-220 trong traceability.

## REQ-V3-BC-228 / BC-228: Bàn giao kiện không giao được về Station/Hub

- [ ] **T-BC-228-CONTRACT** — Chốt operation «Bàn giao kiện không giao được về Station/Hub»: allowed actor R09, guard «Custody giao hai phía, xác nhận thực nhận; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource Station/shift/staff/custody/manifest, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-228-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-228-DB** — Review forward schema/index/constraint của dispatch cho Station/shift/staff/custody/manifest nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-228-BE** — Implement «Bàn giao kiện không giao được về Station/Hub» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-228-UI** — Tích hợp Internal scoped channel — Bàn giao kiện không giao được về Station/Hub; action «Bàn giao kiện không giao được về Station/Hub», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-228-VERIFY** — Chạy testcase riêng TEST-V3-BC-228-FLOW và AC-BC-228-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-228 trong traceability.

## REQ-V3-BC-230 / BC-230: Quản lý ca làm và nhân sự Station trong quyền được ủy quyền

- [ ] **T-BC-230-CONTRACT** — Chốt operation «Quản lý ca làm và nhân sự Station trong quyền được ủy quyền»: allowed actor R16, guard «Không tự cấp global/system admin hoặc quyền ngoài Station; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource Station/shift/staff/custody/manifest, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-230-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-230-DB** — Review forward schema/index/constraint của dispatch cho Station/shift/staff/custody/manifest nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-230-BE** — Implement «Quản lý ca làm và nhân sự Station trong quyền được ủy quyền» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-230-UI** — Tích hợp Internal scoped channel — Quản lý ca làm và nhân sự Station trong quyền được ủy quyền; action «Quản lý ca làm và nhân sự Station trong quyền được ủy quyền», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-230-VERIFY** — Chạy testcase riêng TEST-V3-BC-230-FLOW và AC-BC-230-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-230 trong traceability.

## REQ-V3-BC-232 / BC-232: Xử lý kiện tồn lâu, sai tuyến, mất/hỏng qua workflow

- [ ] **T-BC-232-CONTRACT** — Chốt operation «Xử lý kiện tồn lâu, sai tuyến, mất/hỏng qua workflow»: allowed actor R16, guard «Case, bằng chứng, phê duyệt khi cần; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource Station/shift/staff/custody/manifest, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-232-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-232-DB** — Review forward schema/index/constraint của dispatch cho Station/shift/staff/custody/manifest nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-232-BE** — Implement «Xử lý kiện tồn lâu, sai tuyến, mất/hỏng qua workflow» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-232-UI** — Tích hợp Internal scoped channel — Xử lý kiện tồn lâu, sai tuyến, mất/hỏng qua workflow; action «Xử lý kiện tồn lâu, sai tuyến, mất/hỏng qua workflow», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-232-VERIFY** — Chạy testcase riêng TEST-V3-BC-232-FLOW và AC-BC-232-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-232 trong traceability.

## REQ-V3-BC-250 / BC-250: Kiểm tra kiện tồn và bàn giao bất thường

- [ ] **T-BC-250-CONTRACT** — Chốt operation «Kiểm tra kiện tồn và bàn giao bất thường»: allowed actor R24, guard «Chỉ vị trí/đội được phân công, có đối chiếu custody; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource Station/shift/staff/custody/manifest, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-250-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-250-DB** — Review forward schema/index/constraint của dispatch cho Station/shift/staff/custody/manifest nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-250-BE** — Implement «Kiểm tra kiện tồn và bàn giao bất thường» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-250-UI** — Tích hợp Internal scoped channel — Kiểm tra kiện tồn và bàn giao bất thường; action «Kiểm tra kiện tồn và bàn giao bất thường», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-250-VERIFY** — Chạy testcase riêng TEST-V3-BC-250-FLOW và AC-BC-250-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-250 trong traceability.

## REQ-V3-BC-251 / BC-251: Xác nhận chênh lệch bàn giao theo quyền

- [ ] **T-BC-251-CONTRACT** — Chốt operation «Xác nhận chênh lệch bàn giao theo quyền»: allowed actor R24, guard «Evidence, audit, phê duyệt riêng với mất mát; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource Station/shift/staff/custody/manifest, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-251-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-251-DB** — Review forward schema/index/constraint của dispatch cho Station/shift/staff/custody/manifest nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-251-BE** — Implement «Xác nhận chênh lệch bàn giao theo quyền» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-251-UI** — Tích hợp Internal scoped channel — Xác nhận chênh lệch bàn giao theo quyền; action «Xác nhận chênh lệch bàn giao theo quyền», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-251-VERIFY** — Chạy testcase riêng TEST-V3-BC-251-FLOW và AC-BC-251-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-251 trong traceability.

## REQ-V3-BC-305 / BC-305: Quản trị cơ cấu Station/Hub/khu vực theo phạm vi

- [ ] **T-BC-305-CONTRACT** — Chốt operation «Quản trị cơ cấu Station/Hub/khu vực theo phạm vi»: allowed actor R04, guard «Không sửa custody đã xảy ra; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource Station/shift/staff/custody/manifest, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-305-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-305-DB** — Review forward schema/index/constraint của dispatch cho Station/shift/staff/custody/manifest nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-305-BE** — Implement «Quản trị cơ cấu Station/Hub/khu vực theo phạm vi» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-305-UI** — Tích hợp Internal scoped channel — Quản trị cơ cấu Station/Hub/khu vực theo phạm vi; action «Quản trị cơ cấu Station/Hub/khu vực theo phạm vi», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-305-VERIFY** — Chạy testcase riêng TEST-V3-BC-305-FLOW và AC-BC-305-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-305 trong traceability.


- [ ] T-LEGACY-L011: map từng obligation legacy ở spec sang implementation/negative/compatibility testcase, bao gồm phần consumer; không đóng full spec khi requirement legacy còn chưa được chứng minh hoặc disposition rõ.

## Closure toàn Spec

- [ ] T-L011-FULL: từng REQ có AC/design/task/code/API/DB/UI/test/receipt; lint/typecheck/build/test theo app liên quan, required Architecture/H2/PG18 CI exact revision, report/privacy/owned cleanup và backward compatibility giữ nguyên; no unresolved blocking findings.
- [ ] T-L011-ACCEPT: scoped PR integration PASS → Owner merge → regression trên merge SHA → Owner full-spec acceptance riêng. Chưa tick vì docs promotion.


- [ ] **T-AB-STATION-STAFF** — REQ-AB-STATION-STAFF: close approved operation/state/data/UX/config contract + fixtures; implement owner API/DB/HTTP and integrated Station admin/staff/shift and published service points; unit/PG/security/browser or verified channel assert «Merchant cannot DML Station by Owner role; wrong Station/delegation/revoke deny; shift changes audit/version and do not change historical custody»; TEST-AB-STATION-STAFF with source+integration receipts. Dependencies L002;L004;L010; do not mark full spec when missing.
