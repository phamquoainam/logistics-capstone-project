# Feature 016: Thực nghiệm RQ1–RQ4 và tái lập bằng chứng

**Status:** DRAFT_SPEC / NOT_IMPLEMENTED  
**Owners / Bounded Context:** `quality, scripts, infrastructure`  
**Actors:** Developer, Researcher, Reviewer  
**Priority:** P0 – kiểm thử / research  
**Source:** [REPORT] Chương 2 BR/FR/NFR; Chương 3 Use Case/State Machine; Chương 4 §4.2–4.3.1. API/UI cụ thể: [PROPOSED].

## 1. Bối cảnh / Problem statement
Thực nghiệm RQ1–RQ4 và tái lập bằng chứng là một lát cắt sản phẩm E-Logistics, triển khai sau/đồng thời các contract phụ thuộc. Người dùng cần giải quyết đúng nghiệp vụ đồng thời bảo vệ ownership, consistency và truy vết. Không được xây một CRUD demo bỏ qua trạng thái và tác động liên module.

## 2. User story / business outcome
Là **Developer, Researcher, Reviewer**, tôi muốn thực hiện nghiệp vụ **Thực nghiệm RQ1–RQ4 và tái lập bằng chứng** để đạt: **Hai lần chạy cùng seed/config có thể tái lập hợp lý; invariant checks pass; có tập raw metrics và kết luận giới hạn tương ứng từng RQ.**.

## 3. Luồng người dùng và dữ liệu (happy path)
Researcher seed dataset cố định → chạy control/baseline → chạy candidate cùng thiết lập → lưu raw metrics+invariant queries+commit/env → so sánh → báo cáo cả đạt/chưa đạt.

## 4. Yêu cầu có mã định danh
| Feature REQ | Hành vi cần xây dựng | Trace FR |
| --- | --- | --- |
| REQ-016-01 | RQ1 test concurrent wallet/reservation/assignment với DB baseline vs optional Redis lock khi nhiều replica, đếm invariant violations. | NFR/RQ |
| REQ-016-02 | RQ2 no cache/cold/hot, tariff change, cache outage; pricing accuracy luôn được đối chiếu golden cases. | NFR/RQ |
| REQ-016-03 | RQ3 message retries/duplicate/reorder/restart so baseline vs outbox/inbox; đo completion/duplicate effects/recovery. | NFR/RQ |
| REQ-016-04 | RQ4 baseline route order/nearest neighbor so solver candidate với cùng matrix/constraints; đo distance, estimated time, compute and valid routes. | NFR/RQ |
| REQ-016-05 | Mỗi run lưu seed, source commit, image versions, parameter load, metrics raw, errors, limitations; không tự sinh thành tích giả. | NFR/RQ |


### Mapping FR gốc
Không có FR riêng: đây là enabler cho các NFR và RQ trong báo cáo.

### Business invariants / NFR cần giữ
- **BR:** BR-02, BR-06, BR-11, BR-13, BR-18, BR-20, BR-21.
- **NFR:** NFR-C01, NFR-P01, NFR-P02, NFR-P03, NFR-P04, NFR-O03.
- Tham chiếu chi tiết tại `docs/domain/business-rules.md`, `docs/quality/non-functional-requirements.md`; không thay đổi nghĩa BR tại feature.

## 5. Lỗi, trường hợp biên và điều kiện thiếu
- EC-016-01: Redis lock expired during slow transaction
- EC-016-02: RabbitMQ redelivery
- EC-016-03: High contention hot key
- EC-016-04: Cache stale after tariff publish
- EC-016-05: Solver timeout/infeasible
- EC-016-06: Uncontrolled provider variability

- Từ chối quyền hoặc object cross-tenant trước khi tiết lộ dữ liệu. Input sai: xác định lỗi ở trường nào; partial commit chỉ được phép khi chính sách đã chốt.
- Mất mạng/timeout sau commit phải có đường `get status`/retry cùng idempotency key; không reset form và báo thành công giả.

## 6. Tiêu chí nghiệm thu kiểm chứng được

**AC-016-01 — RQ1:** Given dataset và concurrency settings cố định, When chạy các kịch bản wallet/reservation/assignment cạnh tranh, Then invariant audit chỉ ra số vi phạm và so sánh kết quả baseline với phương án tùy chọn Redis lock bằng raw output.

**AC-016-02 — RQ2:** Given golden tariff dataset cố định, When so no-cache/cold-cache/hot-cache, version publish và Redis outage, Then lưu số liệu độ trễ, query counts, cache-hit và số quote sai, không chấp nhận sai giá dù latency tốt.

**AC-016-03 — RQ3:** Given replayable event fixtures, When inject duplicate, worker restart, broker timeout và out-of-order event, Then lưu unique business effects, completion, failed-job và recovery time.

**AC-016-04 — RQ4:** Given cùng tập điểm/matrix/time windows, When chạy baseline và solver candidate, Then ghi tổng quãng đường, ETA, compute time, route validity, cùng trạng thái infeasible nếu có.

**AC-016-05 — reproducibility:** Given commit hash, image tags, seeds và run config của một thử nghiệm, When người đánh giá chạy lại, Then có thể tái lập phép đo, kiểm tra bất biến và phân biệt kết quả thật với thông tin giả định; run không thực thi phải đánh NOT_RUN.

## 7. Phạm vi và phụ thuộc
- **Cần có trước / phải có hợp đồng mock được:** `001`, `004`, `005`, `006`, `007`, `008`, `010`, `011`, `012`.
- **Ngoài phạm vi:** Production benchmark/SLA guarantees, kết luận công nghệ luôn tốt hơn trong mọi môi trường.
- **Mốc P0:** chỉ complete khi code, migration, contract và kiểm thử thực hiện, không chỉ dựng giao diện.

## 8. Câu hỏi còn mở và giả định
[BLOCKING] Baseline load environment, datasets, target devices, fixed maps provider/matrix, optional multi-replica plan.

**Giả định:** bảng master/demo seeded, người dùng và phạm vi quyền hợp lệ, các phụ thuộc ngoài dùng Sandbox/mô phỏng, các ngưỡng được đặt tại cấu hình sau khi chốt. Mọi assumption phải được đánh dấu `PROPOSED` hoặc `BLOCKING` trong quyết định chung.

## V3 reconciliation overlay — PROPOSED, 2026-10-26

ID/nội dung/checkbox cũ giữ nguyên lịch sử; candidate không DONE/REPORT/accepted decision. Target: [L030 capstone-e2e-hardening-release](../../specs/planning/v3/L030-capstone-e2e-hardening-release/spec.md). [Crosswalk](../../docs/reconciliation/v3/legacy-crosswalk.csv) SPLIT/EXTEND/PRESERVE là scope relation, không automatic equivalence.

Actors/flow/guard bổ sung theo REQ-V3-BC tại linked specs; P1/exclusions cũ không loại CORE FULL.5 DEFER và CORE negative cần direct API denial, không xóa AC cũ. Outcomes: Cross-module E2E/release/RQ — Reviewed SHA→journeys/negative/fault/load→restore→evidence→Owner release.

[Delivery/DoR/DoD](../../docs/reconciliation/v3/delivery-plan.md). Research016 chỉ RQ/L030, không Returns. T-003-01 còn mở cho authority/address/enrollment; [first packet](../planning/v3/first-business-slice/spec.md).
