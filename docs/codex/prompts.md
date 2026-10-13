# Prompt Collection for Codex — run each phase explicitly

## Prompt A — Spec review only (không code)
```text
Đọc AGENTS.md, docs/prd.md, docs/principles.md, docs/domain/{business-rules,requirements-catalog,state-machines}.md, docs/decisions/open-questions.md và specs/001-platform-foundation/{spec,design,tasks}.md.
Đối chiếu nguồn, tìm requirement/AC không kiểm chứng được, xung đột ownership, những BLOCKING. Chỉ chỉnh docs/specs nếu cần, giữ mã nguồn gốc. Trả bảng: vấn đề, mức độ, đề xuất, file cần sửa. Không viết code bước này.
```

## Prompt B — Repo foundation (bắt đầu code có kiểm soát)
```text
Hãy kiểm tra repo hiện hữu bằng git status, không reset/overwrite. Đọc AGENTS.md, architecture/module-catalog.md, architecture/repository-layout.md, quyết định ACCEPTED và specs/001-platform-foundation/{spec,design,tasks}.md.
Chỉ implement T-001-01 đến task đầu tiên có thể hoàn tất an toàn. Với những dependency version/provider chưa được xác nhận, lập một spike và ADR PROPOSED; không chọn tùy tiện. Tạo Maven/Spring Boot modular skeleton, Flyway/PostgreSQL Compose theo baseline đã chốt, architecture test và health. Chạy lệnh build và smoke, báo kết quả thật. Không triển khai các nghiệp vụ business ở task foundation.
```

## Prompt C — Implement one feature vertical slice
```text
Feature: <FEATURE_FOLDER>
Đọc AGENTS.md; docs/prd.md; docs/principles.md; docs/architecture/{overview,module-catalog,data-ownership,transactions-events,api-guidelines}.md; docs/decisions/open-questions.md và 3 file trong specs/<FEATURE_FOLDER>/.
1) Check DoR và state những unresolved BLOCKING. 2) Nếu ready, làm lần lượt task chưa hoàn tất, bắt đầu từ contract và DB. 3) Viết test qua invariant/role/concurrency/replay. 4) Update OpenAPI + generated api-client. 5) Test trên DB thật qua Testcontainers, không mock correctness. 6) Cập nhật task status/evidence chỉ khi pass. Kết thúc trình bày diff, test log, blockers. Không đổi module khác ngoài public contract đã review.
```

## Prompt D — Audit architecture và code ownership
```text
Đọc docs/architecture/code-architecture.md, module-catalog.md và AGENTS.md. Kiểm tra imports vào othermodule.internal, repository cross-owner, transaction boundary workflow, Flyway drift, JPA entity leak, idempotency và audit. Chạy architecture tests. Chỉ lập báo cáo và patch nhỏ an toàn nếu có test, không đại tu repo tùy tiện.
```

## Prompt E — End-to-end P0 check
```text
Đọc docs/quality/{acceptance,test-strategy,traceability}.md, specs/INDEX.md. Lập danh sách gap để demo end-to-end gồm account→merchant/address→quote→draft/confirm→stock→hub/dispatch→routing/GPS→POD→COD→reconciliation→notification. Chạy scripts hiện có, ghi evidence thật và cảnh báo chưa đạt. Không đánh dấu AC-01–08 PASS nếu chưa có số liệu và artifacts.
```
