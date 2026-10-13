# Quy chuẩn tổ chức mã (Package by Domain + Selective Clean Architecture)

**[REPORT]** Chương 4 §4.2.4. Dưới đây là mẫu định hướng; skeleton được tạo sau khi Codex kiểm tra repo thực tế.

```text
apps/backend/src/main/java/com/example/elogistics/
├── ELogisticsApplication.java
├── identity/       # api/, internal/
├── merchant/
├── order/
│   ├── package-info.java
│   ├── api/                     # Public command, query, DTO, event
│   └── internal/
│       ├── web/                 # Controller + HTTP DTO
│       ├── application/         # Feature use cases + ports
│       │   ├── createshipment/
│       │   ├── confirmshipment/
│       │   ├── cancelshipment/
│       │   └── port/
│       ├── domain/              # Aggregate, entity, value object
│       └── infrastructure/      # DB adapters, external adapters
│           ├── persistence/
│           ├── messaging/
│           └── configuration/
├── pricing/
├── warehouse/
├── finance/
├── dispatch/
├── routing/
├── notification/
├── workflow/       # Application orchestration, only other api/*
└── platform/       # Shared technical kernel, NOT shared domain entities
```

## Hướng dependency hợp lệ

- `web → application → domain`; infrastructure implements ports declared by application/domain.
- Domain không phụ thuộc Spring Web, persistence entity, HTTP DTO, Redis hoặc RabbitMQ.
- Application dùng public ports hoặc domain abstractions, không phụ thuộc controller.
- Infrastructure phụ thuộc vào lõi, không ngược lại. Spring dependency injection được wiring ngoài business logic.
- `api` công khai không được expose `internal.*` hoặc entity persistence.
- `workflow` điều phối multi-module commands với transaction boundary rõ, **không** cập nhật trực tiếp các entity nội bộ module.

## Feature-first trong module
Khi số use case tăng, tổ chức `application/confirmshipment`, `application/cancelshipment`… Mỗi feature có handler/usecase, command/result, unit test; tránh áp đặt class `XxxServiceImpl` không có lý do.

## SOLID thực hành
- SRP: controller chỉ HTTP, application điều phối, domain bảo vệ invariant, adapter IO.
- OCP/DIP: pricing strategy và provider adapter qua port khi có từ hai implement hoặc lý do testability.
- LSP/ISP: public interface nhỏ theo capability; không buộc use case phụ thuộc giant `*Service`.
- Không thiết kế abstraction cho mọi CRUD, tránh over-engineering.

## Quy tắc review
Không vượt ownership bằng JPA `@ManyToOne` sang entity module khác; chỉ lưu FK ID/refs rõ. Không để `@Transactional` ở controller. Event handler phải gọi use case đúng owner. Domain exception được map nhất quán tại API boundary.
