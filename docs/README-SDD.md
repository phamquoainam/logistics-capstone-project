# E-Logistics — Bộ tài liệu phát triển theo đặc tả (SDD)

**Trạng thái:** Bộ tài liệu lập kế hoạch triển khai P0, chưa phải phần mềm đã hoạt động.  
**Căn cứ:** `CLV04_BaoCao_TLCN_04.docx`, đặc biệt phần A §5, Chương 2 (§2.1–2.7), Chương 3 (§3.1–3.7), Chương 4 (§4.1–4.3.1).  
**Ngày biên soạn:** 2026-10-13.

## Mục tiêu

Xây dựng prototype E-Logistics hỗ trợ giao nhận chặng cuối với web, ứng dụng shipper và backend Modular Monolith. Bộ tài liệu được tổ chức để Codex thực thi **theo từng vertical slice**, giữ được mối liên kết `Nguồn báo cáo → BR/FR/NFR → SPEC/REQ → AC → thiết kế → TASK → TEST → bằng chứng`.

## Bắt đầu trong ngày đầu

1. Đọc `docs/prd.md`, `docs/principles.md`, `docs/decisions/open-questions.md`, `docs/architecture/overview.md`, `docs/architecture/module-catalog.md` và `AGENTS.md`.
2. Chốt các quyết định có nhãn `BLOCKING` theo `docs/decisions/open-questions.md`. **Không được xem `[PROPOSED]` là quy định của báo cáo.**
3. Chạy chuỗi công việc tại `docs/implementation/day-1-plan.md` và `specs/001-platform-foundation/`.
4. Sau khi nền tảng build/test chạy, triển khai `002-identity-access`, `003-merchant-address`, `004-pricing`, `005-shipment-lifecycle`, rồi các vertical slice tiếp theo. Các tính năng có thứ tự ưu tiên, không có nghĩa phải làm toàn bộ trong một ngày.
5. Trước khi code mỗi feature, kiểm tra ba file `spec.md`, `design.md`, `tasks.md`. Khi thay đổi hành vi, cập nhật chúng trước rồi mới code.

## Ranh giới nguồn và mức độ quyết định

- `[REPORT]`: Nội dung được mô tả trong báo cáo, giữ nguyên mã FR/BR/NFR và ngữ nghĩa.
- `[PROPOSED]`: Lựa chọn thiết kế do bộ SDD đề xuất để tăng tính thi hành, chưa phải nội dung đã được báo cáo chốt.
- `[BLOCKING]`: Quyết định cần được xác nhận trước khi triển khai nhánh liên quan.
- `[CONFIG]`: Giá trị cố ý để ở cấu hình, không hardcode khi báo cáo chưa đặt ngưỡng.
- `[OUT-OF-SCOPE]`: Không thuộc P0 hoặc đề tài.

**Phạm vi tài liệu nguồn:** Bản DOCX cung cấp dừng ở phần **4.3.1**, chưa có mô tả hoàn thiện của các mục 4.3.2 trở đi hoặc kết quả Chương 5–6. Không ghi giả những phần này đã được chứng minh.

## Nội dung

- `docs/`: PRD, nguyên tắc, domain, kiến trúc, quy ước, quyết định, bảo mật, giao dịch và kiểm thử.
- `specs/00X-*/`: spec, design, tasks cho từng luồng nghiệp vụ.
- `contracts/`: quy ước OpenAPI/event/schema được đề xuất, chưa có hợp đồng được duyệt.
- `apps/`, `packages/`, `infra/`, `quality/`, `scripts/`: hướng dẫn nơi Codex sẽ sinh source/test/config theo công nghệ đã chốt, **không chứa mã ứng dụng giả**.
- `AGENTS.md` và `docs/codex/prompts.md`: chỉ dẫn thực thi và prompt chia giai đoạn.

## Nguyên tắc kết luận

Không xem việc tạo file, compile thành công hoặc demo đẹp là bằng chứng đã đạt P0; cần thêm migration, kiểm thử quyền, bất biến, end-to-end và thực nghiệm RQ1–RQ4.
