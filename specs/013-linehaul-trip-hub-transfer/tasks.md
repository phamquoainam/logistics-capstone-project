# Tasks 013 — linehaul-trip-hub-transfer

NOT_STARTED; thứ tự là contract/fixture → DB → BE/HTTP → UI/channel → verification → full acceptance. Mỗi BC là gói end-to-end nội bộ, không thay phương pháp Spec-by-Spec. Không task nào DONE trong rebuild docs.

## REQ-V3-BC-070 / BC-070: Thực hiện phần nghiệp vụ tại Station trong bàn giao chuyến xe liên Hub

- [ ] **T-BC-070-CONTRACT** — Chốt operation «Thực hiện phần nghiệp vụ tại Station trong bàn giao chuyến xe liên Hub»: allowed actor R08, guard «Station xác nhận nhận/xuất manifest, không vận hành lái xe thay Line-haul Driver; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource trip/seal/manifest/custody, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-070-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-070-DB** — Review forward schema/index/constraint của dispatch cho trip/seal/manifest/custody nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-070-BE** — Implement «Thực hiện phần nghiệp vụ tại Station trong bàn giao chuyến xe liên Hub» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-070-UI** — Tích hợp Internal scoped channel — Thực hiện phần nghiệp vụ tại Station trong bàn giao chuyến xe liên Hub; Station xuất/nhận; Network giám sát với accepted client/channel; action «Thực hiện phần nghiệp vụ tại Station trong bàn giao chuyến xe liên Hub», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-070-VERIFY** — Chạy testcase riêng TEST-V3-BC-070-FLOW và AC-BC-070-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-070 trong traceability.

## REQ-V3-BC-221 / BC-221: Thực hiện scan bàn giao nhận/xuất kiện với chuyến xe liên Hub

- [ ] **T-BC-221-CONTRACT** — Chốt operation «Thực hiện scan bàn giao nhận/xuất kiện với chuyến xe liên Hub»: allowed actor R08, guard «Vai trò Station chỉ xác nhận custody đầu/cuối; Driver quản lý hành trình; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource trip/seal/manifest/custody, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-221-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-221-DB** — Review forward schema/index/constraint của dispatch cho trip/seal/manifest/custody nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-221-BE** — Implement «Thực hiện scan bàn giao nhận/xuất kiện với chuyến xe liên Hub» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-221-UI** — Tích hợp Internal scoped channel — Thực hiện scan bàn giao nhận/xuất kiện với chuyến xe liên Hub; Station xuất/nhận; Network giám sát với accepted client/channel; action «Thực hiện scan bàn giao nhận/xuất kiện với chuyến xe liên Hub», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-221-VERIFY** — Chạy testcase riêng TEST-V3-BC-221-FLOW và AC-BC-221-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-221 trong traceability.

## REQ-V3-BC-231 / BC-231: Giám sát các chuyến xe đi/đến và bàn giao kiện

- [ ] **T-BC-231-CONTRACT** — Chốt operation «Giám sát các chuyến xe đi/đến và bàn giao kiện»: allowed actor R16, guard «Không làm Driver giả, giám sát manifest/custody; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource trip/seal/manifest/custody, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-231-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-231-DB** — Review forward schema/index/constraint của dispatch cho trip/seal/manifest/custody nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-231-BE** — Implement «Giám sát các chuyến xe đi/đến và bàn giao kiện» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-231-UI** — Tích hợp Internal scoped channel — Giám sát các chuyến xe đi/đến và bàn giao kiện; Station xuất/nhận; Network giám sát với accepted client/channel; action «Giám sát các chuyến xe đi/đến và bàn giao kiện», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-231-VERIFY** — Chạy testcase riêng TEST-V3-BC-231-FLOW và AC-BC-231-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-231 trong traceability.

## REQ-V3-BC-234 / BC-234: Nhận chuyến xe được phân công

- [ ] **T-BC-234-CONTRACT** — Chốt operation «Nhận chuyến xe được phân công»: allowed actor R23, guard «Chỉ trip hợp lệ theo tuyến/driver, thời hạn; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource trip/seal/manifest/custody, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-234-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-234-DB** — Review forward schema/index/constraint của dispatch cho trip/seal/manifest/custody nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-234-BE** — Implement «Nhận chuyến xe được phân công» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-234-UI** — Tích hợp Internal scoped channel — Nhận chuyến xe được phân công; Station xuất/nhận; Network giám sát với accepted client/channel; action «Nhận chuyến xe được phân công», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-234-VERIFY** — Chạy testcase riêng TEST-V3-BC-234-FLOW và AC-BC-234-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-234 trong traceability.

## REQ-V3-BC-235 / BC-235: Kiểm tra seal, tải trọng và trạng thái phương tiện

- [ ] **T-BC-235-CONTRACT** — Chốt operation «Kiểm tra seal, tải trọng và trạng thái phương tiện»: allowed actor R23, guard «Ghi checklist/evidence; bất thường phải xử lý trước departure; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource trip/seal/manifest/custody, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-235-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-235-DB** — Review forward schema/index/constraint của dispatch cho trip/seal/manifest/custody nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-235-BE** — Implement «Kiểm tra seal, tải trọng và trạng thái phương tiện» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-235-UI** — Tích hợp Internal scoped channel — Kiểm tra seal, tải trọng và trạng thái phương tiện; Station xuất/nhận; Network giám sát với accepted client/channel; action «Kiểm tra seal, tải trọng và trạng thái phương tiện», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-235-VERIFY** — Chạy testcase riêng TEST-V3-BC-235-FLOW và AC-BC-235-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-235 trong traceability.

## REQ-V3-BC-236 / BC-236: Nhận manifest kiện, bao tải và thông tin số lượng

- [ ] **T-BC-236-CONTRACT** — Chốt operation «Nhận manifest kiện, bao tải và thông tin số lượng»: allowed actor R23, guard «Manifest phiên bản, cargo/trip relation rõ ràng; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource trip/seal/manifest/custody, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-236-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-236-DB** — Review forward schema/index/constraint của dispatch cho trip/seal/manifest/custody nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-236-BE** — Implement «Nhận manifest kiện, bao tải và thông tin số lượng» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-236-UI** — Tích hợp Internal scoped channel — Nhận manifest kiện, bao tải và thông tin số lượng; Station xuất/nhận; Network giám sát với accepted client/channel; action «Nhận manifest kiện, bao tải và thông tin số lượng», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-236-VERIFY** — Chạy testcase riêng TEST-V3-BC-236-FLOW và AC-BC-236-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-236 trong traceability.

## REQ-V3-BC-237 / BC-237: Quét barcode và đối chiếu kiện/bao khi nhận tải

- [ ] **T-BC-237-CONTRACT** — Chốt operation «Quét barcode và đối chiếu kiện/bao khi nhận tải»: allowed actor R23, guard «Chặn kiện trùng/sai manifest; ghi discrepancy; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource trip/seal/manifest/custody, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-237-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-237-DB** — Review forward schema/index/constraint của dispatch cho trip/seal/manifest/custody nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-237-BE** — Implement «Quét barcode và đối chiếu kiện/bao khi nhận tải» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-237-UI** — Tích hợp Internal scoped channel — Quét barcode và đối chiếu kiện/bao khi nhận tải; Station xuất/nhận; Network giám sát với accepted client/channel; action «Quét barcode và đối chiếu kiện/bao khi nhận tải», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-237-VERIFY** — Chạy testcase riêng TEST-V3-BC-237-FLOW và AC-BC-237-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-237 trong traceability.

## REQ-V3-BC-238 / BC-238: Xác nhận bàn giao kiện tại điểm xuất phát

- [ ] **T-BC-238-CONTRACT** — Chốt operation «Xác nhận bàn giao kiện tại điểm xuất phát»: allowed actor R23, guard «Hai phía custody; timestamp, station/hub, actor; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource trip/seal/manifest/custody, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-238-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-238-DB** — Review forward schema/index/constraint của dispatch cho trip/seal/manifest/custody nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-238-BE** — Implement «Xác nhận bàn giao kiện tại điểm xuất phát» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-238-UI** — Tích hợp Internal scoped channel — Xác nhận bàn giao kiện tại điểm xuất phát; Station xuất/nhận; Network giám sát với accepted client/channel; action «Xác nhận bàn giao kiện tại điểm xuất phát», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-238-VERIFY** — Chạy testcase riêng TEST-V3-BC-238-FLOW và AC-BC-238-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-238 trong traceability.

## REQ-V3-BC-239 / BC-239: Ghi nhận hành trình và các mốc vận tải

- [ ] **T-BC-239-CONTRACT** — Chốt operation «Ghi nhận hành trình và các mốc vận tải»: allowed actor R23, guard «Trip events độc lập với shipment delivered; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource trip/seal/manifest/custody, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-239-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-239-DB** — Review forward schema/index/constraint của dispatch cho trip/seal/manifest/custody nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-239-BE** — Implement «Ghi nhận hành trình và các mốc vận tải» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-239-UI** — Tích hợp Internal scoped channel — Ghi nhận hành trình và các mốc vận tải; Station xuất/nhận; Network giám sát với accepted client/channel; action «Ghi nhận hành trình và các mốc vận tải», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-239-VERIFY** — Chạy testcase riêng TEST-V3-BC-239-FLOW và AC-BC-239-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-239 trong traceability.

## REQ-V3-BC-240 / BC-240: Báo trễ, sự cố hoặc thay đổi hành trình

- [ ] **T-BC-240-CONTRACT** — Chốt operation «Báo trễ, sự cố hoặc thay đổi hành trình»: allowed actor R23, guard «Lý do/evidence, escalation, SLA; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource trip/seal/manifest/custody, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-240-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-240-DB** — Review forward schema/index/constraint của dispatch cho trip/seal/manifest/custody nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-240-BE** — Implement «Báo trễ, sự cố hoặc thay đổi hành trình» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-240-UI** — Tích hợp Internal scoped channel — Báo trễ, sự cố hoặc thay đổi hành trình; Station xuất/nhận; Network giám sát với accepted client/channel; action «Báo trễ, sự cố hoặc thay đổi hành trình», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-240-VERIFY** — Chạy testcase riêng TEST-V3-BC-240-FLOW và AC-BC-240-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-240 trong traceability.

## REQ-V3-BC-241 / BC-241: Xác nhận đến Hub/Station đích

- [ ] **T-BC-241-CONTRACT** — Chốt operation «Xác nhận đến Hub/Station đích»: allowed actor R23, guard «Đúng trip/destination, thời gian và trạng thái; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource trip/seal/manifest/custody, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-241-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-241-DB** — Review forward schema/index/constraint của dispatch cho trip/seal/manifest/custody nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-241-BE** — Implement «Xác nhận đến Hub/Station đích» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-241-UI** — Tích hợp Internal scoped channel — Xác nhận đến Hub/Station đích; Station xuất/nhận; Network giám sát với accepted client/channel; action «Xác nhận đến Hub/Station đích», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-241-VERIFY** — Chạy testcase riêng TEST-V3-BC-241-FLOW và AC-BC-241-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-241 trong traceability.

## REQ-V3-BC-242 / BC-242: Bàn giao và đối chiếu số kiện thực nhận ở đích

- [ ] **T-BC-242-CONTRACT** — Chốt operation «Bàn giao và đối chiếu số kiện thực nhận ở đích»: allowed actor R23, guard «Đối chiếu manifest và bên nhận, tạo discrepancy khi lệch; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource trip/seal/manifest/custody, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-242-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-242-DB** — Review forward schema/index/constraint của dispatch cho trip/seal/manifest/custody nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-242-BE** — Implement «Bàn giao và đối chiếu số kiện thực nhận ở đích» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-242-UI** — Tích hợp Internal scoped channel — Bàn giao và đối chiếu số kiện thực nhận ở đích; Station xuất/nhận; Network giám sát với accepted client/channel; action «Bàn giao và đối chiếu số kiện thực nhận ở đích», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-242-VERIFY** — Chạy testcase riêng TEST-V3-BC-242-FLOW và AC-BC-242-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-242 trong traceability.

## REQ-V3-BC-264 / BC-264: Theo dõi chuyến xe đến/đi Hub

- [ ] **T-BC-264-CONTRACT** — Chốt operation «Theo dõi chuyến xe đến/đi Hub»: allowed actor R26, guard «Trip + manifest + ETA/delay consistency; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource trip/seal/manifest/custody, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-264-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-264-DB** — Review forward schema/index/constraint của dispatch cho trip/seal/manifest/custody nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-264-BE** — Implement «Theo dõi chuyến xe đến/đi Hub» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-264-UI** — Tích hợp Internal scoped channel — Theo dõi chuyến xe đến/đi Hub; Station xuất/nhận; Network giám sát với accepted client/channel; action «Theo dõi chuyến xe đến/đi Hub», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-264-VERIFY** — Chạy testcase riêng TEST-V3-BC-264-FLOW và AC-BC-264-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-264 trong traceability.

## REQ-V3-BC-267 / BC-267: Phối hợp điều chuyển hàng và ngoại lệ vận tải

- [ ] **T-BC-267-CONTRACT** — Chốt operation «Phối hợp điều chuyển hàng và ngoại lệ vận tải»: allowed actor R26, guard «Theo policy/coverage/capacity, audit; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource trip/seal/manifest/custody, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-267-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-267-DB** — Review forward schema/index/constraint của dispatch cho trip/seal/manifest/custody nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-267-BE** — Implement «Phối hợp điều chuyển hàng và ngoại lệ vận tải» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-267-UI** — Tích hợp Internal scoped channel — Phối hợp điều chuyển hàng và ngoại lệ vận tải; Station xuất/nhận; Network giám sát với accepted client/channel; action «Phối hợp điều chuyển hàng và ngoại lệ vận tải», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-267-VERIFY** — Chạy testcase riêng TEST-V3-BC-267-FLOW và AC-BC-267-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-267 trong traceability.

## REQ-V3-BC-270 / BC-270: Quản lý mạng lưới Station và Hub theo vùng/toàn mạng

- [ ] **T-BC-270-CONTRACT** — Chốt operation «Quản lý mạng lưới Station và Hub theo vùng/toàn mạng»: allowed actor R27, guard «Đúng scope, không tự vượt quyền identity; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource trip/seal/manifest/custody, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-270-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-270-DB** — Review forward schema/index/constraint của dispatch cho trip/seal/manifest/custody nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-270-BE** — Implement «Quản lý mạng lưới Station và Hub theo vùng/toàn mạng» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-270-UI** — Tích hợp Internal scoped channel — Quản lý mạng lưới Station và Hub theo vùng/toàn mạng; Station xuất/nhận; Network giám sát với accepted client/channel; action «Quản lý mạng lưới Station và Hub theo vùng/toàn mạng», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-270-VERIFY** — Chạy testcase riêng TEST-V3-BC-270-FLOW và AC-BC-270-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-270 trong traceability.

## REQ-V3-BC-272 / BC-272: Giám sát năng lực tuyến và vận tải

- [ ] **T-BC-272-CONTRACT** — Chốt operation «Giám sát năng lực tuyến và vận tải»: allowed actor R27, guard «Dữ liệu theo network region; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource trip/seal/manifest/custody, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-272-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-272-DB** — Review forward schema/index/constraint của dispatch cho trip/seal/manifest/custody nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-272-BE** — Implement «Giám sát năng lực tuyến và vận tải» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-272-UI** — Tích hợp Internal scoped channel — Giám sát năng lực tuyến và vận tải; Station xuất/nhận; Network giám sát với accepted client/channel; action «Giám sát năng lực tuyến và vận tải», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-272-VERIFY** — Chạy testcase riêng TEST-V3-BC-272-FLOW và AC-BC-272-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-272 trong traceability.

## REQ-V3-BC-273 / BC-273: Điều chuyển hàng liên vùng

- [ ] **T-BC-273-CONTRACT** — Chốt operation «Điều chuyển hàng liên vùng»: allowed actor R27, guard «Phê duyệt theo policy, manifest/custody liên tục; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource trip/seal/manifest/custody, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-273-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-273-DB** — Review forward schema/index/constraint của dispatch cho trip/seal/manifest/custody nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-273-BE** — Implement «Điều chuyển hàng liên vùng» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-273-UI** — Tích hợp Internal scoped channel — Điều chuyển hàng liên vùng; Station xuất/nhận; Network giám sát với accepted client/channel; action «Điều chuyển hàng liên vùng», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-273-VERIFY** — Chạy testcase riêng TEST-V3-BC-273-FLOW và AC-BC-273-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-273 trong traceability.

## REQ-V3-BC-276 / BC-276: Điều chỉnh tuyến ngoại lệ theo chính sách

- [ ] **T-BC-276-CONTRACT** — Chốt operation «Điều chỉnh tuyến ngoại lệ theo chính sách»: allowed actor R27, guard «Reason/evidence/approval/audit; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource trip/seal/manifest/custody, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-276-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-276-DB** — Review forward schema/index/constraint của dispatch cho trip/seal/manifest/custody nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-276-BE** — Implement «Điều chỉnh tuyến ngoại lệ theo chính sách» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-276-UI** — Tích hợp Internal scoped channel — Điều chỉnh tuyến ngoại lệ theo chính sách; Station xuất/nhận; Network giám sát với accepted client/channel; action «Điều chỉnh tuyến ngoại lệ theo chính sách», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-276-VERIFY** — Chạy testcase riêng TEST-V3-BC-276-FLOW và AC-BC-276-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-276 trong traceability.


- [ ] T-LEGACY-L013: map từng obligation legacy ở spec sang implementation/negative/compatibility testcase, bao gồm phần consumer; không đóng full spec khi requirement legacy còn chưa được chứng minh hoặc disposition rõ.

## Closure toàn Spec

- [ ] T-L013-FULL: từng REQ có AC/design/task/code/API/DB/UI/test/receipt; lint/typecheck/build/test theo app liên quan, required Architecture/H2/PG18 CI exact revision, report/privacy/owned cleanup và backward compatibility giữ nguyên; no unresolved blocking findings.
- [ ] T-L013-ACCEPT: scoped PR integration PASS → Owner merge → regression trên merge SHA → Owner full-spec acceptance riêng. Chưa tick vì docs promotion.
