# Codex Project Instructions (must read before changing code)

Bạn đang làm việc trên nền tảng E-Logistics theo SDD. Ngôn ngữ trao đổi với nhóm: **tiếng Việt**, tên identifier trong code/API: tiếng Anh rõ nghĩa.

## Thứ tự tài liệu có thẩm quyền
1. Báo cáo DOCX do người dùng cung cấp, đặc biệt BR-01–21, FR và NFR tại Chương 2; state machine và domain model Chương 3.
2. `docs/prd.md`, `docs/principles.md`, `docs/domain/*`, `docs/architecture/*`, quyết định `ACCEPTED`.
3. `specs/<feature>/spec.md` → `design.md` → `tasks.md`.
4. Hợp đồng chuẩn OpenAPI/event sau khi được review; không tự phát minh field.

Nếu mâu thuẫn, **dừng và nêu xung đột**, không sửa nghĩa báo cáo im lặng. Các path/DTO/bảng ghi `[PROPOSED]` phải được duyệt trước khi làm production feature.

## Workflow bắt buộc
1. `git status --short`, `git branch --show-current`; không xóa/reset/force push code đang có, không commit thẳng main.
2. Đọc tài liệu feature, kiểm tra `docs/codex/definition-of-ready.md`.
3. Tạo plan ngắn cho **task đầu tiên chưa tick**; giải thích những file dự kiến đổi.
4. Viết test tương ứng trước hoặc cùng feature logic; implement ít nhất một vertical slice hoạt động thật.
5. Chạy lint/typecheck/build/test/migrate; báo chính xác lệnh, trạng thái pass/fail, lý do không chạy nếu vướng môi trường.
6. Chỉ đánh dấu task DONE sau khi chứng minh gate; cập nhật `docs/quality/traceability.md`, ADR nếu có design change.
7. Kết thúc trả summary: changed files, tests run, failing tests, dependencies, open questions, next task.

## Hard constraints
- Một Spring Boot backend artifact với nine domain modules, DDD ownership; không tự đổi thành microservices.
- Không import `othermodule.internal.*`; `workflow` phối hợp qua `api` cùng local DB transaction.
- Không bypass DB invariant bằng kiểm tra frontend, Redis lock, queue hoặc mock.
- Không tính số dư từ user body; không trừ tiền/hold tồn nhiều lần; không tự sửa closed reconciliation.
- Không chạy Flyway `repair` bừa bãi hoặc sửa migration đã triển khai; schema không cho Hibernate tự update.
- Không thêm JWT secrets/payment credentials hay sample PII thực; `.env.example` chỉ placeholders.
- Không tự chọn map/payment/storage providers ngoài decision log.
- Không tự ghi `DONE` nếu chưa chạy test thật, không bịa benchmark.
- Kích thước thay đổi phải nhỏ và kiểm chứng được. Nếu còn `BLOCKING`, ưu tiên foundation và spike.
