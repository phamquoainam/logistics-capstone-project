# Tasks 016 — returns-reverse-logistics

NOT_STARTED; thứ tự là contract/fixture → DB → BE/HTTP → UI/channel → verification → full acceptance. Mỗi BC là gói end-to-end nội bộ, không thay phương pháp Spec-by-Spec. Không task nào DONE trong rebuild docs.

## REQ-V3-BC-052 / BC-052: Điều phối hoàn hàng theo workflow

- [ ] **T-BC-052-CONTRACT** — Chốt operation «Điều phối hoàn hàng theo workflow»: allowed actor R06, guard «Không xóa lịch sử delivery; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource return authorization/events, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-052-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-052-DB** — Review forward schema/index/constraint của orders cho return authorization/events nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-052-BE** — Implement «Điều phối hoàn hàng theo workflow» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-052-UI** — Tích hợp Internal scoped channel — Điều phối hoàn hàng theo workflow; action «Điều phối hoàn hàng theo workflow», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-052-VERIFY** — Chạy testcase riêng TEST-V3-BC-052-FLOW và AC-BC-052-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-052 trong traceability.

## REQ-V3-BC-067 / BC-067: Nhận kiện giao thất bại/đang hoàn

- [ ] **T-BC-067-CONTRACT** — Chốt operation «Nhận kiện giao thất bại/đang hoàn»: allowed actor R08, guard «Đúng trạng thái custody; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource return authorization/events, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-067-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-067-DB** — Review forward schema/index/constraint của orders cho return authorization/events nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-067-BE** — Implement «Nhận kiện giao thất bại/đang hoàn» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-067-UI** — Tích hợp Internal scoped channel — Nhận kiện giao thất bại/đang hoàn; action «Nhận kiện giao thất bại/đang hoàn», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-067-VERIFY** — Chạy testcase riêng TEST-V3-BC-067-FLOW và AC-BC-067-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-067 trong traceability.

## REQ-V3-BC-077 / BC-077: Trả kiện giao thất bại về Station

- [ ] **T-BC-077-CONTRACT** — Chốt operation «Trả kiện giao thất bại về Station»: allowed actor R09, guard «Có custody handover; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource return authorization/events, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-077-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-077-DB** — Review forward schema/index/constraint của orders cho return authorization/events nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-077-BE** — Implement «Trả kiện giao thất bại về Station» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-077-UI** — Tích hợp Internal scoped channel — Trả kiện giao thất bại về Station; action «Trả kiện giao thất bại về Station», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-077-VERIFY** — Chạy testcase riêng TEST-V3-BC-077-FLOW và AC-BC-077-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-077 trong traceability.

## REQ-V3-BC-111 / BC-111: Theo dõi ticket liên quan hoàn hàng

- [ ] **T-BC-111-CONTRACT** — Chốt operation «Theo dõi ticket liên quan hoàn hàng»: allowed actor R14, guard «Không tự sửa custody; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource return authorization/events, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-111-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-111-DB** — Review forward schema/index/constraint của orders cho return authorization/events nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-111-BE** — Implement «Theo dõi ticket liên quan hoàn hàng» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-111-UI** — Tích hợp Internal scoped channel — Theo dõi ticket liên quan hoàn hàng; action «Theo dõi ticket liên quan hoàn hàng», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-111-VERIFY** — Chạy testcase riêng TEST-V3-BC-111-FLOW và AC-BC-111-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-111 trong traceability.

## REQ-V3-BC-146 / BC-146: Theo dõi và quản lý hoàn trả kiện gửi lẻ

- [ ] **T-BC-146-CONTRACT** — Chốt operation «Theo dõi và quản lý hoàn trả kiện gửi lẻ»: allowed actor R18, guard «Return events/custody rõ ràng; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource return authorization/events, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-146-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-146-DB** — Review forward schema/index/constraint của orders cho return authorization/events nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-146-BE** — Implement «Theo dõi và quản lý hoàn trả kiện gửi lẻ» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-146-UI** — Tích hợp Personal — Theo dõi và quản lý hoàn trả kiện gửi lẻ; action «Theo dõi và quản lý hoàn trả kiện gửi lẻ», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-146-VERIFY** — Chạy testcase riêng TEST-V3-BC-146-FLOW và AC-BC-146-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-146 trong traceability.

## REQ-V3-BC-178 / BC-178: Yêu cầu hoàn/thu hồi Shipment theo quyền và trạng thái

- [ ] **T-BC-178-CONTRACT** — Chốt operation «Yêu cầu hoàn/thu hồi Shipment theo quyền và trạng thái»: allowed actor R03, guard «Hàng đã pickup phải đi reverse logistics; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource return authorization/events, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-178-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-178-DB** — Review forward schema/index/constraint của orders cho return authorization/events nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-178-BE** — Implement «Yêu cầu hoàn/thu hồi Shipment theo quyền và trạng thái» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-178-UI** — Tích hợp Merchant current context — Yêu cầu hoàn/thu hồi Shipment theo quyền và trạng thái; action «Yêu cầu hoàn/thu hồi Shipment theo quyền và trạng thái», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-178-VERIFY** — Chạy testcase riêng TEST-V3-BC-178-FLOW và AC-BC-178-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-178 trong traceability.

## REQ-V3-BC-198 / BC-198: Theo dõi hoàn hàng và trao đổi kết quả với Recipient

- [ ] **T-BC-198-CONTRACT** — Chốt operation «Theo dõi hoàn hàng và trao đổi kết quả với Recipient»: allowed actor R21, guard «Không thao tác custody của hãng; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource return authorization/events, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-198-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-198-DB** — Review forward schema/index/constraint của orders cho return authorization/events nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-198-BE** — Implement «Theo dõi hoàn hàng và trao đổi kết quả với Recipient» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-198-UI** — Tích hợp Merchant current context — Theo dõi hoàn hàng và trao đổi kết quả với Recipient; action «Theo dõi hoàn hàng và trao đổi kết quả với Recipient», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-198-VERIFY** — Chạy testcase riêng TEST-V3-BC-198-FLOW và AC-BC-198-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-198 trong traceability.


- [ ] T-LEGACY-L016: map từng obligation legacy ở spec sang implementation/negative/compatibility testcase, bao gồm phần consumer; không đóng full spec khi requirement legacy còn chưa được chứng minh hoặc disposition rõ.

## Closure toàn Spec

- [ ] T-L016-FULL: từng REQ có AC/design/task/code/API/DB/UI/test/receipt; lint/typecheck/build/test theo app liên quan, required Architecture/H2/PG18 CI exact revision, report/privacy/owned cleanup và backward compatibility giữ nguyên; no unresolved blocking findings.
- [ ] T-L016-ACCEPT: scoped PR integration PASS → Owner merge → regression trên merge SHA → Owner full-spec acceptance riêng. Chưa tick vì docs promotion.


- [ ] **T-AB-RETURNS** — REQ-AB-RETURNS: close approved operation/state/data/UX/config contract + fixtures; implement owner API/DB/HTTP and integrated Personal/Merchant return visibility and authorized request; unit/PG/security/browser or verified channel assert «Failure/return/retry request cannot silently set custody or refund; historical delivery attempts preserved; rejected request reason safe»; TEST-AB-RETURNS with source+integration receipts. Dependencies L011;L015;L018; do not mark full spec when missing.
