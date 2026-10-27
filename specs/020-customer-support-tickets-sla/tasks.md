# Tasks 020 — customer-support-tickets-sla

NOT_STARTED; thứ tự là contract/fixture → DB → BE/HTTP → UI/channel → verification → full acceptance. Mỗi BC là gói end-to-end nội bộ, không thay phương pháp Spec-by-Spec. Không task nào DONE trong rebuild docs.

## REQ-V3-BC-004 / BC-004: Gửi yêu cầu tư vấn/hỗ trợ không đăng nhập

- [ ] **T-BC-004-CONTRACT** — Chốt operation «Gửi yêu cầu tư vấn/hỗ trợ không đăng nhập»: allowed actor R01, guard «Rate limit và kiểm soát spam; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource ticket/assigned customer relationship/interaction, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-004-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-004-DB** — Review forward schema/index/constraint của orders cho ticket/assigned customer relationship/interaction nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-004-BE** — Implement «Gửi yêu cầu tư vấn/hỗ trợ không đăng nhập» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-004-UI** — Tích hợp Public — Gửi yêu cầu tư vấn/hỗ trợ không đăng nhập; action «Gửi yêu cầu tư vấn/hỗ trợ không đăng nhập», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-004-VERIFY** — Chạy testcase riêng TEST-V3-BC-004-FLOW và AC-BC-004-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-004 trong traceability.

## REQ-V3-BC-016 / BC-016: Gửi và theo dõi support ticket

- [ ] **T-BC-016-CONTRACT** — Chốt operation «Gửi và theo dõi support ticket»: allowed actor R02, guard «Không tự quyết định bồi thường; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource ticket/assigned customer relationship/interaction, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-016-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-016-DB** — Review forward schema/index/constraint của orders cho ticket/assigned customer relationship/interaction nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-016-BE** — Implement «Gửi và theo dõi support ticket» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-016-UI** — Tích hợp Merchant current context — Gửi và theo dõi support ticket; action «Gửi và theo dõi support ticket», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-016-VERIFY** — Chạy testcase riêng TEST-V3-BC-016-FLOW và AC-BC-016-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-016 trong traceability.

## REQ-V3-BC-030 / BC-030: Tạo ticket hỗ trợ Logistics nếu được cấp quyền

- [ ] **T-BC-030-CONTRACT** — Chốt operation «Tạo ticket hỗ trợ Logistics nếu được cấp quyền»: allowed actor R03, guard «Không duyệt bồi thường; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource ticket/assigned customer relationship/interaction, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-030-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-030-DB** — Review forward schema/index/constraint của orders cho ticket/assigned customer relationship/interaction nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-030-BE** — Implement «Tạo ticket hỗ trợ Logistics nếu được cấp quyền» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-030-UI** — Tích hợp Merchant current context — Tạo ticket hỗ trợ Logistics nếu được cấp quyền; action «Tạo ticket hỗ trợ Logistics nếu được cấp quyền», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-030-VERIFY** — Chạy testcase riêng TEST-V3-BC-030-FLOW và AC-BC-030-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-030 trong traceability.

## REQ-V3-BC-038 / BC-038: Phê duyệt escalation theo quyền

- [ ] **T-BC-038-CONTRACT** — Chốt operation «Phê duyệt escalation theo quyền»: allowed actor R04, guard «Separation of duties; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource ticket/assigned customer relationship/interaction, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-038-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-038-DB** — Review forward schema/index/constraint của orders cho ticket/assigned customer relationship/interaction nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-038-BE** — Implement «Phê duyệt escalation theo quyền» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-038-UI** — Tích hợp Internal scoped channel — Phê duyệt escalation theo quyền; action «Phê duyệt escalation theo quyền», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-038-VERIFY** — Chạy testcase riêng TEST-V3-BC-038-FLOW và AC-BC-038-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-038 trong traceability.

## REQ-V3-BC-083 / BC-083: Phản ánh đơn Merchant qua Merchant Support

- [ ] **T-BC-083-CONTRACT** — Chốt operation «Phản ánh đơn Merchant qua Merchant Support»: allowed actor R10, guard «Merchant lập ticket với Logistics CSKH; không có Recipient Portal; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource ticket/assigned customer relationship/interaction, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-083-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-083-DB** — Review forward schema/index/constraint của orders cho ticket/assigned customer relationship/interaction nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-083-BE** — Implement «Phản ánh đơn Merchant qua Merchant Support» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-083-UI** — Tích hợp Verified Recipient channel — Phản ánh đơn Merchant qua Merchant Support;Courier/CS; public tracking; không Portal/IAM với accepted client/channel; action «Phản ánh đơn Merchant qua Merchant Support», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-083-VERIFY** — Chạy testcase riêng TEST-V3-BC-083-FLOW và AC-BC-083-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-083 trong traceability.

## REQ-V3-BC-089 / BC-089: Mở tra soát COD thiếu/phí sai

- [ ] **T-BC-089-CONTRACT** — Chốt operation «Mở tra soát COD thiếu/phí sai»: allowed actor R11, guard «Không tự duyệt hoàn tiền; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource ticket/assigned customer relationship/interaction, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-089-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-089-DB** — Review forward schema/index/constraint của orders cho ticket/assigned customer relationship/interaction nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-089-BE** — Implement «Mở tra soát COD thiếu/phí sai» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-089-UI** — Tích hợp Merchant current context — Mở tra soát COD thiếu/phí sai; action «Mở tra soát COD thiếu/phí sai», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-089-VERIFY** — Chạy testcase riêng TEST-V3-BC-089-FLOW và AC-BC-089-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-089 trong traceability.

## REQ-V3-BC-105 / BC-105: Nhận/tạo ticket từ Merchant hoặc người liên quan

- [ ] **T-BC-105-CONTRACT** — Chốt operation «Nhận/tạo ticket từ Merchant hoặc người liên quan»: allowed actor R14, guard «Phân loại nguồn; xác minh quyền thông tin; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource ticket/assigned customer relationship/interaction, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-105-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-105-DB** — Review forward schema/index/constraint của orders cho ticket/assigned customer relationship/interaction nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-105-BE** — Implement «Nhận/tạo ticket từ Merchant hoặc người liên quan» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-105-UI** — Tích hợp Internal scoped channel — Nhận/tạo ticket từ Merchant hoặc người liên quan; action «Nhận/tạo ticket từ Merchant hoặc người liên quan», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-105-VERIFY** — Chạy testcase riêng TEST-V3-BC-105-FLOW và AC-BC-105-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-105 trong traceability.

## REQ-V3-BC-107 / BC-107: Phân loại, phân công và cập nhật SLA ticket

- [ ] **T-BC-107-CONTRACT** — Chốt operation «Phân loại, phân công và cập nhật SLA ticket»: allowed actor R14, guard «Lưu lịch sử thay đổi; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource ticket/assigned customer relationship/interaction, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-107-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-107-DB** — Review forward schema/index/constraint của orders cho ticket/assigned customer relationship/interaction nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-107-BE** — Implement «Phân loại, phân công và cập nhật SLA ticket» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-107-UI** — Tích hợp Internal scoped channel — Phân loại, phân công và cập nhật SLA ticket; action «Phân loại, phân công và cập nhật SLA ticket», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-107-VERIFY** — Chạy testcase riêng TEST-V3-BC-107-FLOW và AC-BC-107-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-107 trong traceability.

## REQ-V3-BC-108 / BC-108: Gửi yêu cầu giao lại/đổi thông tin

- [ ] **T-BC-108-CONTRACT** — Chốt operation «Gửi yêu cầu giao lại/đổi thông tin»: allowed actor R14, guard «Workflow phê duyệt; không tự sửa Shipment đã chốt; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource ticket/assigned customer relationship/interaction, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-108-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-108-DB** — Review forward schema/index/constraint của orders cho ticket/assigned customer relationship/interaction nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-108-BE** — Implement «Gửi yêu cầu giao lại/đổi thông tin» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-108-UI** — Tích hợp Internal scoped channel — Gửi yêu cầu giao lại/đổi thông tin; action «Gửi yêu cầu giao lại/đổi thông tin», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-108-VERIFY** — Chạy testcase riêng TEST-V3-BC-108-FLOW và AC-BC-108-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-108 trong traceability.

## REQ-V3-BC-109 / BC-109: Escalate vụ nghiêm trọng

- [ ] **T-BC-109-CONTRACT** — Chốt operation «Escalate vụ nghiêm trọng»: allowed actor R14, guard «Không tự phê duyệt bồi thường; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource ticket/assigned customer relationship/interaction, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-109-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-109-DB** — Review forward schema/index/constraint của orders cho ticket/assigned customer relationship/interaction nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-109-BE** — Implement «Escalate vụ nghiêm trọng» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-109-UI** — Tích hợp Internal scoped channel — Escalate vụ nghiêm trọng; action «Escalate vụ nghiêm trọng», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-109-VERIFY** — Chạy testcase riêng TEST-V3-BC-109-FLOW và AC-BC-109-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-109 trong traceability.

## REQ-V3-BC-110 / BC-110: Đa kênh hotline/email/chat tích hợp

- [ ] **T-BC-110-CONTRACT** — Chốt operation «Đa kênh hotline/email/chat tích hợp»: allowed actor R14, guard «Không cần xây portal Recipient; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource ticket/assigned customer relationship/interaction, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-110-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-110-DB** — Review forward schema/index/constraint của orders cho ticket/assigned customer relationship/interaction nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-110-BE** — Implement «Đa kênh hotline/email/chat tích hợp» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-110-UI** — Tích hợp Internal scoped channel — Đa kênh hotline/email/chat tích hợp; action «Đa kênh hotline/email/chat tích hợp», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-110-VERIFY** — Chạy testcase riêng TEST-V3-BC-110-FLOW và AC-BC-110-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-110 trong traceability.

## REQ-V3-BC-145 / BC-145: Mở và theo dõi yêu cầu hỗ trợ/tra soát mất, hỏng

- [ ] **T-BC-145-CONTRACT** — Chốt operation «Mở và theo dõi yêu cầu hỗ trợ/tra soát mất, hỏng»: allowed actor R18, guard «Chỉ Shipment thuộc mình, có ticket/evidence; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource ticket/assigned customer relationship/interaction, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-145-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-145-DB** — Review forward schema/index/constraint của orders cho ticket/assigned customer relationship/interaction nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-145-BE** — Implement «Mở và theo dõi yêu cầu hỗ trợ/tra soát mất, hỏng» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-145-UI** — Tích hợp Personal — Mở và theo dõi yêu cầu hỗ trợ/tra soát mất, hỏng; action «Mở và theo dõi yêu cầu hỗ trợ/tra soát mất, hỏng», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-145-VERIFY** — Chạy testcase riêng TEST-V3-BC-145-FLOW và AC-BC-145-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-145 trong traceability.

## REQ-V3-BC-153 / BC-153: Nhận và xử lý ticket trực tiếp từ Retail Sender

- [ ] **T-BC-153-CONTRACT** — Chốt operation «Nhận và xử lý ticket trực tiếp từ Retail Sender»: allowed actor R14, guard «Xác minh Shipment thuộc Sender, tracking, timeline, escalation; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource ticket/assigned customer relationship/interaction, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-153-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-153-DB** — Review forward schema/index/constraint của orders cho ticket/assigned customer relationship/interaction nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-153-BE** — Implement «Nhận và xử lý ticket trực tiếp từ Retail Sender» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-153-UI** — Tích hợp Internal scoped channel — Nhận và xử lý ticket trực tiếp từ Retail Sender; action «Nhận và xử lý ticket trực tiếp từ Retail Sender», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-153-VERIFY** — Chạy testcase riêng TEST-V3-BC-153-FLOW và AC-BC-153-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-153 trong traceability.

## REQ-V3-BC-154 / BC-154: Tiếp nhận phản ánh Recipient cho đơn Retail Sender

- [ ] **T-BC-154-CONTRACT** — Chốt operation «Tiếp nhận phản ánh Recipient cho đơn Retail Sender»: allowed actor R14, guard «Không giả định Merchant Support; xác minh tối thiểu; bảo vệ PII; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource ticket/assigned customer relationship/interaction, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-154-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-154-DB** — Review forward schema/index/constraint của orders cho ticket/assigned customer relationship/interaction nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-154-BE** — Implement «Tiếp nhận phản ánh Recipient cho đơn Retail Sender» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-154-UI** — Tích hợp Internal scoped channel — Tiếp nhận phản ánh Recipient cho đơn Retail Sender; action «Tiếp nhận phản ánh Recipient cho đơn Retail Sender», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-154-VERIFY** — Chạy testcase riêng TEST-V3-BC-154-FLOW và AC-BC-154-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-154 trong traceability.

## REQ-V3-BC-155 / BC-155: Tạo và theo dõi ticket từ phản ánh Recipient đối với đơn Merchant

- [ ] **T-BC-155-CONTRACT** — Chốt operation «Tạo và theo dõi ticket từ phản ánh Recipient đối với đơn Merchant»: allowed actor R02, guard «Có thể ủy quyền Merchant Support khi Role được bổ sung; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource ticket/assigned customer relationship/interaction, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-155-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-155-DB** — Review forward schema/index/constraint của orders cho ticket/assigned customer relationship/interaction nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-155-BE** — Implement «Tạo và theo dõi ticket từ phản ánh Recipient đối với đơn Merchant» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-155-UI** — Tích hợp Merchant current context — Tạo và theo dõi ticket từ phản ánh Recipient đối với đơn Merchant; action «Tạo và theo dõi ticket từ phản ánh Recipient đối với đơn Merchant», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-155-VERIFY** — Chạy testcase riêng TEST-V3-BC-155-FLOW và AC-BC-155-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-155 trong traceability.

## REQ-V3-BC-156 / BC-156: Tiếp nhận/chuyển phản ánh Recipient và theo dõi ticket theo ủy quyền

- [ ] **T-BC-156-CONTRACT** — Chốt operation «Tiếp nhận/chuyển phản ánh Recipient và theo dõi ticket theo ủy quyền»: allowed actor R03, guard «Merchant scope, không tự quyết định bồi thường; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource ticket/assigned customer relationship/interaction, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-156-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-156-DB** — Review forward schema/index/constraint của orders cho ticket/assigned customer relationship/interaction nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-156-BE** — Implement «Tiếp nhận/chuyển phản ánh Recipient và theo dõi ticket theo ủy quyền» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-156-UI** — Tích hợp Merchant current context — Tiếp nhận/chuyển phản ánh Recipient và theo dõi ticket theo ủy quyền; action «Tiếp nhận/chuyển phản ánh Recipient và theo dõi ticket theo ủy quyền», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-156-VERIFY** — Chạy testcase riêng TEST-V3-BC-156-FLOW và AC-BC-156-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-156 trong traceability.

## REQ-V3-BC-173 / BC-173: Trao đổi với hãng chuyển phát, mở/escalate ticket

- [ ] **T-BC-173-CONTRACT** — Chốt operation «Trao đổi với hãng chuyển phát, mở/escalate ticket»: allowed actor R19, guard «Không tự duyệt bồi thường; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource ticket/assigned customer relationship/interaction, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-173-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-173-DB** — Review forward schema/index/constraint của orders cho ticket/assigned customer relationship/interaction nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-173-BE** — Implement «Trao đổi với hãng chuyển phát, mở/escalate ticket» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-173-UI** — Tích hợp Merchant current context — Trao đổi với hãng chuyển phát, mở/escalate ticket; action «Trao đổi với hãng chuyển phát, mở/escalate ticket», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-173-VERIFY** — Chạy testcase riêng TEST-V3-BC-173-FLOW và AC-BC-173-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-173 trong traceability.

## REQ-V3-BC-191 / BC-191: Mở tra soát COD thiếu, phí sai và theo dõi kết quả

- [ ] **T-BC-191-CONTRACT** — Chốt operation «Mở tra soát COD thiếu, phí sai và theo dõi kết quả»: allowed actor R11, guard «Không tự phê duyệt khoản hoàn/bồi thường; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource ticket/assigned customer relationship/interaction, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-191-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-191-DB** — Review forward schema/index/constraint của orders cho ticket/assigned customer relationship/interaction nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-191-BE** — Implement «Mở tra soát COD thiếu, phí sai và theo dõi kết quả» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-191-UI** — Tích hợp Merchant current context — Mở tra soát COD thiếu, phí sai và theo dõi kết quả; action «Mở tra soát COD thiếu, phí sai và theo dõi kết quả», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-191-VERIFY** — Chạy testcase riêng TEST-V3-BC-191-FLOW và AC-BC-191-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-191 trong traceability.

## REQ-V3-BC-195 / BC-195: Tiếp nhận khiếu nại Recipient và tạo ticket liên kết Shipment

- [ ] **T-BC-195-CONTRACT** — Chốt operation «Tiếp nhận khiếu nại Recipient và tạo ticket liên kết Shipment»: allowed actor R21, guard «Xác minh đơn/nguồn phản ánh; chống ticket trùng; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource ticket/assigned customer relationship/interaction, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-195-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-195-DB** — Review forward schema/index/constraint của orders cho ticket/assigned customer relationship/interaction nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-195-BE** — Implement «Tiếp nhận khiếu nại Recipient và tạo ticket liên kết Shipment» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-195-UI** — Tích hợp Merchant current context — Tiếp nhận khiếu nại Recipient và tạo ticket liên kết Shipment; action «Tiếp nhận khiếu nại Recipient và tạo ticket liên kết Shipment», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-195-VERIFY** — Chạy testcase riêng TEST-V3-BC-195-FLOW và AC-BC-195-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-195 trong traceability.

## REQ-V3-BC-196 / BC-196: Gửi ticket sang E-Logistics Customer Service và theo dõi timeline

- [ ] **T-BC-196-CONTRACT** — Chốt operation «Gửi ticket sang E-Logistics Customer Service và theo dõi timeline»: allowed actor R21, guard «Có phân công, SLA, audit và bằng chứng; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource ticket/assigned customer relationship/interaction, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-196-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-196-DB** — Review forward schema/index/constraint của orders cho ticket/assigned customer relationship/interaction nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-196-BE** — Implement «Gửi ticket sang E-Logistics Customer Service và theo dõi timeline» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-196-UI** — Tích hợp Merchant current context — Gửi ticket sang E-Logistics Customer Service và theo dõi timeline; action «Gửi ticket sang E-Logistics Customer Service và theo dõi timeline», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-196-VERIFY** — Chạy testcase riêng TEST-V3-BC-196-FLOW và AC-BC-196-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-196 trong traceability.

## REQ-V3-BC-199 / BC-199: Cập nhật phản hồi Recipient và xác nhận hoàn tất phối hợp ticket

- [ ] **T-BC-199-CONTRACT** — Chốt operation «Cập nhật phản hồi Recipient và xác nhận hoàn tất phối hợp ticket»: allowed actor R21, guard «Không tự đóng ticket Logistics trái quy trình; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource ticket/assigned customer relationship/interaction, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-199-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-199-DB** — Review forward schema/index/constraint của orders cho ticket/assigned customer relationship/interaction nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-199-BE** — Implement «Cập nhật phản hồi Recipient và xác nhận hoàn tất phối hợp ticket» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-199-UI** — Tích hợp Merchant current context — Cập nhật phản hồi Recipient và xác nhận hoàn tất phối hợp ticket; action «Cập nhật phản hồi Recipient và xác nhận hoàn tất phối hợp ticket», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-199-VERIFY** — Chạy testcase riêng TEST-V3-BC-199-FLOW và AC-BC-199-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-199 trong traceability.

## REQ-V3-BC-249 / BC-249: Hỗ trợ sự cố giao nhận và escalations

- [ ] **T-BC-249-CONTRACT** — Chốt operation «Hỗ trợ sự cố giao nhận và escalations»: allowed actor R24, guard «Ghi lý do, liên kết ticket/incident; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource ticket/assigned customer relationship/interaction, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-249-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-249-DB** — Review forward schema/index/constraint của orders cho ticket/assigned customer relationship/interaction nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-249-BE** — Implement «Hỗ trợ sự cố giao nhận và escalations» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-249-UI** — Tích hợp Internal scoped channel — Hỗ trợ sự cố giao nhận và escalations; action «Hỗ trợ sự cố giao nhận và escalations», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-249-VERIFY** — Chạy testcase riêng TEST-V3-BC-249-FLOW và AC-BC-249-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-249 trong traceability.

## REQ-V3-BC-266 / BC-266: Tạo/điều phối báo cáo mất hoặc hỏng kiện

- [ ] **T-BC-266-CONTRACT** — Chốt operation «Tạo/điều phối báo cáo mất hoặc hỏng kiện»: allowed actor R26, guard «Liên kết kiện và vị trí cuối, ticket/claim; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource ticket/assigned customer relationship/interaction, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-266-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-266-DB** — Review forward schema/index/constraint của orders cho ticket/assigned customer relationship/interaction nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-266-BE** — Implement «Tạo/điều phối báo cáo mất hoặc hỏng kiện» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-266-UI** — Tích hợp Internal scoped channel — Tạo/điều phối báo cáo mất hoặc hỏng kiện; action «Tạo/điều phối báo cáo mất hoặc hỏng kiện», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-266-VERIFY** — Chạy testcase riêng TEST-V3-BC-266-FLOW và AC-BC-266-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-266 trong traceability.

## REQ-V3-BC-292 / BC-292: Tra cứu Shipment theo ticket/phạm vi phân công

- [ ] **T-BC-292-CONTRACT** — Chốt operation «Tra cứu Shipment theo ticket/phạm vi phân công»: allowed actor R14, guard «PII tối thiểu, reason và access audit; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource ticket/assigned customer relationship/interaction, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-292-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-292-DB** — Review forward schema/index/constraint của orders cho ticket/assigned customer relationship/interaction nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-292-BE** — Implement «Tra cứu Shipment theo ticket/phạm vi phân công» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-292-UI** — Tích hợp Internal scoped channel — Tra cứu Shipment theo ticket/phạm vi phân công; action «Tra cứu Shipment theo ticket/phạm vi phân công», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-292-VERIFY** — Chạy testcase riêng TEST-V3-BC-292-FLOW và AC-BC-292-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-292 trong traceability.

## REQ-V3-BC-294 / BC-294: Tiếp nhận yêu cầu đổi địa chỉ hoặc số liên lạc

- [ ] **T-BC-294-CONTRACT** — Chốt operation «Tiếp nhận yêu cầu đổi địa chỉ hoặc số liên lạc»: allowed actor R14, guard «Xác minh sở hữu; phê duyệt thay đổi theo state; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource ticket/assigned customer relationship/interaction, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-294-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-294-DB** — Review forward schema/index/constraint của orders cho ticket/assigned customer relationship/interaction nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-294-BE** — Implement «Tiếp nhận yêu cầu đổi địa chỉ hoặc số liên lạc» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-294-UI** — Tích hợp Internal scoped channel — Tiếp nhận yêu cầu đổi địa chỉ hoặc số liên lạc; action «Tiếp nhận yêu cầu đổi địa chỉ hoặc số liên lạc», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-294-VERIFY** — Chạy testcase riêng TEST-V3-BC-294-FLOW và AC-BC-294-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-294 trong traceability.

## REQ-V3-BC-296 / BC-296: Giám sát deadline phản hồi và giải quyết ticket

- [ ] **T-BC-296-CONTRACT** — Chốt operation «Giám sát deadline phản hồi và giải quyết ticket»: allowed actor R14, guard «SLA clock, escalation, audit; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource ticket/assigned customer relationship/interaction, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-296-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-296-DB** — Review forward schema/index/constraint của orders cho ticket/assigned customer relationship/interaction nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-296-BE** — Implement «Giám sát deadline phản hồi và giải quyết ticket» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-296-UI** — Tích hợp Internal scoped channel — Giám sát deadline phản hồi và giải quyết ticket; action «Giám sát deadline phản hồi và giải quyết ticket», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-296-VERIFY** — Chạy testcase riêng TEST-V3-BC-296-FLOW và AC-BC-296-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-296 trong traceability.

## REQ-V3-BC-307 / BC-307: Phê duyệt escalation nghiệp vụ theo hạn mức

- [ ] **T-BC-307-CONTRACT** — Chốt operation «Phê duyệt escalation nghiệp vụ theo hạn mức»: allowed actor R04, guard «Separation of duties, lý do và audit; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource ticket/assigned customer relationship/interaction, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-307-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-307-DB** — Review forward schema/index/constraint của orders cho ticket/assigned customer relationship/interaction nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-307-BE** — Implement «Phê duyệt escalation nghiệp vụ theo hạn mức» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-307-UI** — Tích hợp Internal scoped channel — Phê duyệt escalation nghiệp vụ theo hạn mức; action «Phê duyệt escalation nghiệp vụ theo hạn mức», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-307-VERIFY** — Chạy testcase riêng TEST-V3-BC-307-FLOW và AC-BC-307-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-307 trong traceability.

## REQ-V3-BC-329 / BC-329: Quản lý Merchant/account được giao

- [ ] **T-BC-329-CONTRACT** — Chốt operation «Quản lý Merchant/account được giao»: allowed actor R29, guard «Không truy cập Merchant ngoài danh sách; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource ticket/assigned customer relationship/interaction, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-329-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-329-DB** — Review forward schema/index/constraint của merchant cho ticket/assigned customer relationship/interaction nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-329-BE** — Implement «Quản lý Merchant/account được giao» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-329-UI** — Tích hợp Internal scoped channel — Quản lý Merchant/account được giao; action «Quản lý Merchant/account được giao», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-329-VERIFY** — Chạy testcase riêng TEST-V3-BC-329-FLOW và AC-BC-329-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-329 trong traceability.

## REQ-V3-BC-330 / BC-330: Tư vấn dịch vụ và ghi nhận tương tác khách hàng

- [ ] **T-BC-330-CONTRACT** — Chốt operation «Tư vấn dịch vụ và ghi nhận tương tác khách hàng»: allowed actor R29, guard «Không đọc PII Shipment không cần thiết; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource ticket/assigned customer relationship/interaction, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-330-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-330-DB** — Review forward schema/index/constraint của merchant cho ticket/assigned customer relationship/interaction nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-330-BE** — Implement «Tư vấn dịch vụ và ghi nhận tương tác khách hàng» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-330-UI** — Tích hợp Internal scoped channel — Tư vấn dịch vụ và ghi nhận tương tác khách hàng; action «Tư vấn dịch vụ và ghi nhận tương tác khách hàng», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-330-VERIFY** — Chạy testcase riêng TEST-V3-BC-330-FLOW và AC-BC-330-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-330 trong traceability.

## REQ-V3-BC-334 / BC-334: Phối hợp hỗ trợ Merchant lớn

- [ ] **T-BC-334-CONTRACT** — Chốt operation «Phối hợp hỗ trợ Merchant lớn»: allowed actor R29, guard «Ticket ownership và escalation; permission code DECISION_REQUIRED (no runtime allowlist expansion)», resource ticket/assigned customer relationship/interaction, exact input/result/error/state/version/key/retention; viết fixtures AC-BC-334-SUCCESS/DENY/RECOVERY với expected DB changes trước code.
- [ ] **T-BC-334-DB** — Review forward schema/index/constraint của orders cho ticket/assigned customer relationship/interaction nếu persistence mới; migrate empty/upgrade V1–V6, runtime DML/no DDL, rollback compatibility. Query-only phải chứng minh không cần DDL, không tạo bảng thừa.
- [ ] **T-BC-334-BE** — Implement «Phối hợp hỗ trợ Merchant lớn» qua application/public API/HTTP hoặc machine/channel đúng contract; current guard/scope, transaction/audit/receipt, fail-closed dependency, conflict/unknown/recovery; no internals access.
- [ ] **T-BC-334-UI** — Tích hợp Internal scoped channel — Phối hợp hỗ trợ Merchant lớn; action «Phối hợp hỗ trợ Merchant lớn», loading/empty/error/conflict/pending/revoked, scope-safe navigation/cache và accessibility; không mock success.
- [ ] **T-BC-334-VERIFY** — Chạy testcase riêng TEST-V3-BC-334-FLOW và AC-BC-334-SUCCESS/DENY/RECOVERY: unit/contract/HTTP/PG/browser hoặc channel tương ứng; race/replay/fault nếu mutation; lưu exact source+integration SHA/artifacts, map REQ-V3-BC-334 trong traceability.


- [ ] T-LEGACY-L020: map từng obligation legacy ở spec sang implementation/negative/compatibility testcase, bao gồm phần consumer; không đóng full spec khi requirement legacy còn chưa được chứng minh hoặc disposition rõ.

## Closure toàn Spec

- [ ] T-L020-FULL: từng REQ có AC/design/task/code/API/DB/UI/test/receipt; lint/typecheck/build/test theo app liên quan, required Architecture/H2/PG18 CI exact revision, report/privacy/owned cleanup và backward compatibility giữ nguyên; no unresolved blocking findings.
- [ ] T-L020-ACCEPT: scoped PR integration PASS → Owner merge → regression trên merge SHA → Owner full-spec acceptance riêng. Chưa tick vì docs promotion.


- [ ] **T-AB-SUPPORT** — REQ-AB-SUPPORT: close approved operation/state/data/UX/config contract + fixtures; implement owner API/DB/HTTP and integrated Guest contact; personal/Merchant tickets; verified Recipient channel; unit/PG/security/browser or verified channel assert «Identity/Shipment ownership verified; cross-tenant/case denied; external support channel no mandatory Recipient account; no ticket auto refunds/POD mutation»; TEST-AB-SUPPORT with source+integration receipts. Dependencies L002;L009; do not mark full spec when missing.
