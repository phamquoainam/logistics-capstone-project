# Tasks 008 — parcel-label-merchant-fulfillment

NOT_STARTED; thứ tự là contract/fixture → DB → BE/HTTP → UI/channel → verification → full acceptance. Mỗi BC là gói end-to-end nội bộ, không thay phương pháp Spec-by-Spec. Không task nào DONE trong rebuild docs.

## REQ-V3-BC-026 / BC-026: Tạo nhãn và danh sách bàn giao

- [ ] **T-BC-026-CONTRACT** — Chốt operation «Tạo nhãn và danh sách bàn giao»: allowed actor R03, guard «Barcode liên kết kiện; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource Parcel/label/measurement or SKU pick-pack facts, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-026-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-026-DB** — Review forward schema/index/constraint của orders cho Parcel/label/measurement or SKU pick-pack facts nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-026-BE** — Implement «Tạo nhãn và danh sách bàn giao» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-026-UI** — Tích hợp Merchant current context — Tạo nhãn và danh sách bàn giao; action «Tạo nhãn và danh sách bàn giao», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-026-VERIFY** — Chạy testcase riêng TEST-V3-BC-026-FLOW và AC-BC-026-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-026 trong traceability.

## REQ-V3-BC-064 / BC-064: Kiểm tra barcode/cân đo kiện

- [ ] **T-BC-064-CONTRACT** — Chốt operation «Kiểm tra barcode/cân đo kiện»: allowed actor R08, guard «Ghi nhận sai lệch có chứng cứ; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource Parcel/label/measurement or SKU pick-pack facts, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-064-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-064-DB** — Review forward schema/index/constraint của orders cho Parcel/label/measurement or SKU pick-pack facts nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-064-BE** — Implement «Kiểm tra barcode/cân đo kiện» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-064-UI** — Tích hợp Internal scoped channel — Kiểm tra barcode/cân đo kiện; action «Kiểm tra barcode/cân đo kiện», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-064-VERIFY** — Chạy testcase riêng TEST-V3-BC-064-FLOW và AC-BC-064-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-064 trong traceability.

## REQ-V3-BC-140 / BC-140: Khai báo kiện và nhận mã vận đơn/nhãn/biên nhận gửi

- [ ] **T-BC-140-CONTRACT** — Chốt operation «Khai báo kiện và nhận mã vận đơn/nhãn/biên nhận gửi»: allowed actor R18, guard «Parcel khác Shipment, thông tin hàng hóa hợp lệ; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource Parcel/label/measurement or SKU pick-pack facts, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-140-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-140-DB** — Review forward schema/index/constraint của orders cho Parcel/label/measurement or SKU pick-pack facts nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-140-BE** — Implement «Khai báo kiện và nhận mã vận đơn/nhãn/biên nhận gửi» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-140-UI** — Tích hợp Personal — Khai báo kiện và nhận mã vận đơn/nhãn/biên nhận gửi; action «Khai báo kiện và nhận mã vận đơn/nhãn/biên nhận gửi», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-140-VERIFY** — Chạy testcase riêng TEST-V3-BC-140-FLOW và AC-BC-140-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-140 trong traceability.

## REQ-V3-BC-150 / BC-150: Kiểm kiện, cân đo, in nhãn và cấp biên nhận walk-in

- [ ] **T-BC-150-CONTRACT** — Chốt operation «Kiểm kiện, cân đo, in nhãn và cấp biên nhận walk-in»: allowed actor R08, guard «Tạo Parcel, timestamp, bằng chứng nhận kiện; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource Parcel/label/measurement or SKU pick-pack facts, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-150-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-150-DB** — Review forward schema/index/constraint của orders cho Parcel/label/measurement or SKU pick-pack facts nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-150-BE** — Implement «Kiểm kiện, cân đo, in nhãn và cấp biên nhận walk-in» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-150-UI** — Tích hợp Internal scoped channel — Kiểm kiện, cân đo, in nhãn và cấp biên nhận walk-in; action «Kiểm kiện, cân đo, in nhãn và cấp biên nhận walk-in», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-150-VERIFY** — Chạy testcase riêng TEST-V3-BC-150-FLOW và AC-BC-150-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-150 trong traceability.

## REQ-V3-BC-176 / BC-176: Chuẩn bị kiện và lập danh sách bàn giao cho Pickup Courier

- [ ] **T-BC-176-CONTRACT** — Chốt operation «Chuẩn bị kiện và lập danh sách bàn giao cho Pickup Courier»: allowed actor R03, guard «Quét/đối chiếu kiện; không tự tạo pickup evidence; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource Parcel/label/measurement or SKU pick-pack facts, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-176-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-176-DB** — Review forward schema/index/constraint của orders cho Parcel/label/measurement or SKU pick-pack facts nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-176-BE** — Implement «Chuẩn bị kiện và lập danh sách bàn giao cho Pickup Courier» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-176-UI** — Tích hợp Merchant current context — Chuẩn bị kiện và lập danh sách bàn giao cho Pickup Courier; action «Chuẩn bị kiện và lập danh sách bàn giao cho Pickup Courier», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-176-VERIFY** — Chạy testcase riêng TEST-V3-BC-176-FLOW và AC-BC-176-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-176 trong traceability.

## REQ-V3-BC-179 / BC-179: Nhận danh sách Shipment/đơn cần lấy hàng và chuẩn bị kiện

- [ ] **T-BC-179-CONTRACT** — Chốt operation «Nhận danh sách Shipment/đơn cần lấy hàng và chuẩn bị kiện»: allowed actor R20, guard «Chỉ đơn thuộc kho/nhân sự được phân công; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource Parcel/label/measurement or SKU pick-pack facts, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-179-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-179-DB** — Review forward schema/index/constraint của warehouse cho Parcel/label/measurement or SKU pick-pack facts nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-179-BE** — Implement «Nhận danh sách Shipment/đơn cần lấy hàng và chuẩn bị kiện» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-179-UI** — Tích hợp Merchant current context — Nhận danh sách Shipment/đơn cần lấy hàng và chuẩn bị kiện; action «Nhận danh sách Shipment/đơn cần lấy hàng và chuẩn bị kiện», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-179-VERIFY** — Chạy testcase riêng TEST-V3-BC-179-FLOW và AC-BC-179-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-179 trong traceability.

## REQ-V3-BC-180 / BC-180: Lấy hàng, kiểm đếm và ghi số lượng thực tế

- [ ] **T-BC-180-CONTRACT** — Chốt operation «Lấy hàng, kiểm đếm và ghi số lượng thực tế»: allowed actor R20, guard «Order, Shipment, Parcel tách biệt; có sai lệch; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource Parcel/label/measurement or SKU pick-pack facts, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-180-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-180-DB** — Review forward schema/index/constraint của warehouse cho Parcel/label/measurement or SKU pick-pack facts nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-180-BE** — Implement «Lấy hàng, kiểm đếm và ghi số lượng thực tế» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-180-UI** — Tích hợp Merchant current context — Lấy hàng, kiểm đếm và ghi số lượng thực tế; action «Lấy hàng, kiểm đếm và ghi số lượng thực tế», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-180-VERIFY** — Chạy testcase riêng TEST-V3-BC-180-FLOW và AC-BC-180-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-180 trong traceability.

## REQ-V3-BC-181 / BC-181: Đóng gói và cân đo kiện, ghi kích thước thực tế

- [ ] **T-BC-181-CONTRACT** — Chốt operation «Đóng gói và cân đo kiện, ghi kích thước thực tế»: allowed actor R20, guard «Snapshot measurement; không tự xác nhận phí chốt; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource Parcel/label/measurement or SKU pick-pack facts, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-181-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-181-DB** — Review forward schema/index/constraint của orders cho Parcel/label/measurement or SKU pick-pack facts nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-181-BE** — Implement «Đóng gói và cân đo kiện, ghi kích thước thực tế» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-181-UI** — Tích hợp Merchant current context — Đóng gói và cân đo kiện, ghi kích thước thực tế; action «Đóng gói và cân đo kiện, ghi kích thước thực tế», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-181-VERIFY** — Chạy testcase riêng TEST-V3-BC-181-FLOW và AC-BC-181-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-181 trong traceability.

## REQ-V3-BC-182 / BC-182: Dán nhãn, quét barcode và đối chiếu Parcel

- [ ] **T-BC-182-CONTRACT** — Chốt operation «Dán nhãn, quét barcode và đối chiếu Parcel»: allowed actor R20, guard «Không tạo barcode trùng; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource Parcel/label/measurement or SKU pick-pack facts, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-182-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-182-DB** — Review forward schema/index/constraint của orders cho Parcel/label/measurement or SKU pick-pack facts nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-182-BE** — Implement «Dán nhãn, quét barcode và đối chiếu Parcel» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-182-UI** — Tích hợp Merchant current context — Dán nhãn, quét barcode và đối chiếu Parcel; action «Dán nhãn, quét barcode và đối chiếu Parcel», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-182-VERIFY** — Chạy testcase riêng TEST-V3-BC-182-FLOW và AC-BC-182-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-182 trong traceability.

## REQ-V3-BC-183 / BC-183: Lập manifest/danh sách kiện bàn giao Pickup Courier

- [ ] **T-BC-183-CONTRACT** — Chốt operation «Lập manifest/danh sách kiện bàn giao Pickup Courier»: allowed actor R20, guard «Bằng chứng số kiện, thời gian và người bàn giao; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource Parcel/label/measurement or SKU pick-pack facts, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-183-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-183-DB** — Review forward schema/index/constraint của warehouse cho Parcel/label/measurement or SKU pick-pack facts nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-183-BE** — Implement «Lập manifest/danh sách kiện bàn giao Pickup Courier» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-183-UI** — Tích hợp Merchant current context — Lập manifest/danh sách kiện bàn giao Pickup Courier; action «Lập manifest/danh sách kiện bàn giao Pickup Courier», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-183-VERIFY** — Chạy testcase riêng TEST-V3-BC-183-FLOW và AC-BC-183-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-183 trong traceability.

## REQ-V3-BC-184 / BC-184: Ghi nhận thiếu/sai hàng hoặc bao bì bất thường

- [ ] **T-BC-184-CONTRACT** — Chốt operation «Ghi nhận thiếu/sai hàng hoặc bao bì bất thường»: allowed actor R20, guard «Evidence, lý do và escalation; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource Parcel/label/measurement or SKU pick-pack facts, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-184-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-184-DB** — Review forward schema/index/constraint của warehouse cho Parcel/label/measurement or SKU pick-pack facts nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-184-BE** — Implement «Ghi nhận thiếu/sai hàng hoặc bao bì bất thường» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-184-UI** — Tích hợp Merchant current context — Ghi nhận thiếu/sai hàng hoặc bao bì bất thường; action «Ghi nhận thiếu/sai hàng hoặc bao bì bất thường», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-184-VERIFY** — Chạy testcase riêng TEST-V3-BC-184-FLOW và AC-BC-184-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-184 trong traceability.

## REQ-V3-BC-185 / BC-185: Xác nhận Courier tới lấy và phối hợp bàn giao

- [ ] **T-BC-185-CONTRACT** — Chốt operation «Xác nhận Courier tới lấy và phối hợp bàn giao»: allowed actor R20, guard «Không tự ghi PICKED_UP thay Courier; hai phía đối chiếu; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource Parcel/label/measurement or SKU pick-pack facts, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-185-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-185-DB** — Review forward schema/index/constraint của warehouse cho Parcel/label/measurement or SKU pick-pack facts nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-185-BE** — Implement «Xác nhận Courier tới lấy và phối hợp bàn giao» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-185-UI** — Tích hợp Merchant current context — Xác nhận Courier tới lấy và phối hợp bàn giao; action «Xác nhận Courier tới lấy và phối hợp bàn giao», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-185-VERIFY** — Chạy testcase riêng TEST-V3-BC-185-FLOW và AC-BC-185-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-185 trong traceability.

## REQ-V3-BC-212 / BC-212: Quét barcode và đối chiếu mã, số lượng kiện

- [ ] **T-BC-212-CONTRACT** — Chốt operation «Quét barcode và đối chiếu mã, số lượng kiện»: allowed actor R07, guard «Đúng parcel/manifest; phát hiện thiếu, trùng, lệch; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource Parcel/label/measurement or SKU pick-pack facts, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-212-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-212-DB** — Review forward schema/index/constraint của orders cho Parcel/label/measurement or SKU pick-pack facts nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-212-BE** — Implement «Quét barcode và đối chiếu mã, số lượng kiện» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-212-UI** — Tích hợp Internal scoped channel — Quét barcode và đối chiếu mã, số lượng kiện; action «Quét barcode và đối chiếu mã, số lượng kiện», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-212-VERIFY** — Chạy testcase riêng TEST-V3-BC-212-FLOW và AC-BC-212-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-212 trong traceability.

## REQ-V3-BC-213 / BC-213: Kiểm tra bao bì, kích thước và thông tin kiện lúc pickup

- [ ] **T-BC-213-CONTRACT** — Chốt operation «Kiểm tra bao bì, kích thước và thông tin kiện lúc pickup»: allowed actor R07, guard «Ghi sai lệch và evidence; không tự chốt lại phí; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource Parcel/label/measurement or SKU pick-pack facts, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-213-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-213-DB** — Review forward schema/index/constraint của orders cho Parcel/label/measurement or SKU pick-pack facts nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-213-BE** — Implement «Kiểm tra bao bì, kích thước và thông tin kiện lúc pickup» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-213-UI** — Tích hợp Internal scoped channel — Kiểm tra bao bì, kích thước và thông tin kiện lúc pickup; action «Kiểm tra bao bì, kích thước và thông tin kiện lúc pickup», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-213-VERIFY** — Chạy testcase riêng TEST-V3-BC-213-FLOW và AC-BC-213-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-213 trong traceability.

## REQ-V3-BC-218 / BC-218: Kiểm tra hàng gửi và điều kiện chấp nhận

- [ ] **T-BC-218-CONTRACT** — Chốt operation «Kiểm tra hàng gửi và điều kiện chấp nhận»: allowed actor R08, guard «Theo chính sách hàng cấm/hạn chế; không truy cập dữ liệu không cần thiết; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource Parcel/label/measurement or SKU pick-pack facts, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-218-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-218-DB** — Review forward schema/index/constraint của orders cho Parcel/label/measurement or SKU pick-pack facts nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-218-BE** — Implement «Kiểm tra hàng gửi và điều kiện chấp nhận» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-218-UI** — Tích hợp Internal scoped channel — Kiểm tra hàng gửi và điều kiện chấp nhận; action «Kiểm tra hàng gửi và điều kiện chấp nhận», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-218-VERIFY** — Chạy testcase riêng TEST-V3-BC-218-FLOW và AC-BC-218-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-218 trong traceability.

## REQ-V3-BC-253 / BC-253: Quét barcode kiện trong công đoạn phân loại

- [ ] **T-BC-253-CONTRACT** — Chốt operation «Quét barcode kiện trong công đoạn phân loại»: allowed actor R25, guard «Scan event truy vết người/máy/thời điểm; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource Parcel/label/measurement or SKU pick-pack facts, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-253-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-253-DB** — Review forward schema/index/constraint của orders cho Parcel/label/measurement or SKU pick-pack facts nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-253-BE** — Implement «Quét barcode kiện trong công đoạn phân loại» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-253-UI** — Tích hợp Internal scoped channel — Quét barcode kiện trong công đoạn phân loại; action «Quét barcode kiện trong công đoạn phân loại», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-253-VERIFY** — Chạy testcase riêng TEST-V3-BC-253-FLOW và AC-BC-253-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-253 trong traceability.

## REQ-V3-BC-258 / BC-258: Ghi nhận barcode không đọc được

- [ ] **T-BC-258-CONTRACT** — Chốt operation «Ghi nhận barcode không đọc được»: allowed actor R25, guard «Case xử lý định danh thay thế có kiểm soát; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource Parcel/label/measurement or SKU pick-pack facts, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-258-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-258-DB** — Review forward schema/index/constraint của orders cho Parcel/label/measurement or SKU pick-pack facts nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-258-BE** — Implement «Ghi nhận barcode không đọc được» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-258-UI** — Tích hợp Internal scoped channel — Ghi nhận barcode không đọc được; action «Ghi nhận barcode không đọc được», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-258-VERIFY** — Chạy testcase riêng TEST-V3-BC-258-FLOW và AC-BC-258-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-258 trong traceability.


- [ ] T-LEGACY-L008: map từng obligation legacy ở spec sang implementation/negative/compatibility testcase, bao gồm phần consumer; không đóng full spec khi requirement legacy còn chưa được chứng minh hoặc disposition rõ.

## Closure toàn Spec

- [ ] T-L008-FULL: từng REQ có AC/design/task/code/API/DB/UI/test/receipt; lint/typecheck/build/test theo app liên quan, required Architecture/H2/PG18 CI exact revision, report/privacy/owned cleanup và backward compatibility giữ nguyên; no unresolved blocking findings.
- [ ] T-L008-ACCEPT: scoped PR integration PASS → Owner merge → regression trên merge SHA → Owner full-spec acceptance riêng. Chưa tick vì docs promotion.


- [ ] **T-AB-PARCEL-HANDOVER** — REQ-AB-PARCEL-HANDOVER: close approved operation/state/data/UX/config contract + fixtures; implement owner API/DB/HTTP and integrated Pick/pack/measure/labels/handover list; unit/PG/security/browser or verified channel assert «Wrong/missing/duplicate Parcel barcode and unauthorized printing denied; measurement snapshot; pack completion no carrier PICKED_UP; two-party handover through010/011»; TEST-AB-PARCEL-HANDOVER with source+integration receipts. Dependencies L006;L007;L010; do not mark full spec when missing.
