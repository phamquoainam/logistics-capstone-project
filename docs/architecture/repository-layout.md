# Enterprise-style Monorepo Layout (development blueprint)

**Tham chiếu:** [REPORT] Chương 4 §4.2.4, bảng mã nguồn `Logistic/`, Java package, React Native/web features. **[PROPOSED]** phần bổ sung SDD, contracts, architecture test và quality folders.

```text
elogistics/
├── AGENTS.md                        # Instructions Codex bắt buộc
├── README.md
├── .editorconfig                     # Codex tạo ở task nền tảng
├── .gitignore                        # Codex tạo ở task nền tảng
├── .env.example                      # Không có secrets thật
├── docs/
│   ├── prd.md
│   ├── principles.md
│   ├── domain/
│   │   ├── glossary.md
│   │   ├── workflows.md
│   │   ├── business-rules.md
│   │   ├── requirements-catalog.md
│   │   └── state-machines.md
│   ├── architecture/
│   │   ├── overview.md
│   │   ├── module-catalog.md
│   │   ├── code-architecture.md
│   │   ├── repository-layout.md
│   │   ├── data-ownership.md
│   │   ├── transactions-events.md
│   │   ├── api-guidelines.md
│   │   ├── frontend-mobile.md
│   │   ├── security.md
│   │   └── decisions/ADR-*.md
│   ├── quality/
│   │   ├── non-functional-requirements.md
│   │   ├── acceptance.md
│   │   ├── test-strategy.md
│   │   └── traceability.md
│   ├── decisions/open-questions.md
│   ├── implementation/{day-1-plan.md,dependency-order.md}
│   ├── codex/{prompts.md,definition-of-ready.md}
│   └── runbooks/{local-development.md,failure-recovery.md}
├── specs/
│   ├── INDEX.md
│   ├── 001-platform-foundation/{spec.md,design.md,tasks.md}
│   ├── 002-identity-access/{spec.md,design.md,tasks.md}
│   └── ... 016-research-experiments/{spec.md,design.md,tasks.md}
├── contracts/
│   ├── openapi/README.md             # Sau review: elogistics.yaml + schemas/
│   └── events/README.md              # Sau review: versioned event schemas
├── apps/
│   ├── backend/                      # Java 21 + Spring Boot + Maven Wrapper
│   │   ├── pom.xml                   # Tạo bởi Codex sau khi chốt baseline
│   │   └── src/
│   │       ├── main/java/com/example/elogistics/
│   │       │   ├── ELogisticsApplication.java
│   │       │   ├── identity/{api,internal}
│   │       │   ├── merchant/{api,internal}
│   │       │   ├── order/{api,internal}
│   │       │   ├── pricing/{api,internal}
│   │       │   ├── warehouse/{api,internal}
│   │       │   ├── finance/{api,internal}
│   │       │   ├── dispatch/{api,internal}
│   │       │   ├── routing/{api,internal}
│   │       │   ├── notification/{api,internal}
│   │       │   ├── workflow/
│   │       │   └── platform/
│   │       ├── main/resources/{application.yml,db/migration/}
│   │       └── test/java/com/example/elogistics/{architecture,modules,workflow}
│   ├── web/
│   │   ├── package.json
│   │   └── src/{features,components,navigation,api,platform}
│   └── shipper-mobile/
│       ├── package.json
│       └── src/{features,components,navigation,api,platform}
├── packages/
│   ├── api-client/                   # Generated TS client từ OpenAPI
│   └── tooling-config/               # Config lint/format nếu thực sự dùng chung
├── infra/
│   ├── compose.yaml                  # PostgreSQL, Redis, RabbitMQ + app/profile
│   ├── postgres/
│   └── monitoring/
├── quality/
│   ├── e2e/                           # Web E2E
│   ├── mobile/                        # Native/device test notes
│   ├── benchmarks/                    # RQ1–RQ4 harness
│   ├── datasets/                      # Non-sensitive fixtures
│   ├── fault-injection/
│   └── evidence/                       # Real test results, git policy
├── scripts/
│   ├── bootstrap/
│   ├── seed/
│   ├── verify/
│   └── recovery/
└── .github/workflows/                  # CI matrix
```

## Điều gì KHÔNG nên tạo sẵn
- Không thêm Docker Compose services giả, `pom.xml` có versions tự đoán, mock interfaces giả làm nghiệp vụ production.
- Không tạo 9 Spring Boot applications; chỉ có **một backend**.
- Không có `apps/backend/src/main/java/.../<module>/controller,service,repository` ngang hàng thiếu domain ownership; controller nằm trong module internal.web.
- Không tạo `contracts` kép với `specs/openapi` lẫn `contracts/openapi`; nguồn hợp đồng duy nhất là `contracts`.
- Không trộn `specs/00X-feature/` với `contracts` hoặc mã executable.

## Nguyên tắc đồng bộ structure
1. docs = WHAT/WHY/policy; specs = feature-specific WHAT/HOW/TODO; contracts = external/internal published schema; apps = executable; quality = independent test/evidence; infra = reproducible runtime.
2. Java module package là business boundary, không nhất thiết Maven module riêng cho từng bounded context.
3. Mỗi feature folder có đúng bộ 3 md cơ bản, đủ yêu cầu; thêm diagrams/fixtures nếu phát sinh nhu cầu thật, không thêm file rỗng.
4. Không commit build artifacts (`target`, `dist`, `.expo`, binaries`, `.env`, secret) hoặc file upload từ khách hàng.
