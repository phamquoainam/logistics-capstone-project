# Open Decisions / Assumptions Register

**Quy tắc:** mọi `[BLOCKING]` phải có owner và ADR/decision trước khi Codex triển khai feature liên quan. `PROPOSED` có thể dùng làm phương án để review nhưng không được gắn nhãn [REPORT]. `CONFIG` phải có default được phê duyệt trước demo.

| ID | Mức | Quyết định còn mở | Feature | Cách chốt / evidence |
| --- | --- | --- | --- | --- |
| DEC-01 | BLOCKING | Phiên bản Spring Boot, Spring Modulith và Java distribution/patch | 001 | Maven wrapper build smoke + compatibility verified |
| DEC-02 | BLOCKING | Node/package manager/React/Vite/Expo SDK, Android emulator & dev build | 001,009,015 | lockfile + native scan/location/SQLite spikes |
| DEC-03 | BLOCKING | Session cookie vs JWT access/refresh; logout/revocation/expiration policy | 002 | ADR + RBAC smoke |
| DEC-04 | BLOCKING | Ai được tạo account, reset password bằng kênh nào | 002 | flows/UC confirmation |
| DEC-05 | BLOCKING | Danh mục địa chỉ/vùng tính cước áp dụng ở VN | 003,004 | sample data, catalog version and business approval |
| DEC-06 | BLOCKING | Map tiles, geocoding, matrix provider, coverage, pricing/quota/license | 003,010 | provider integration + fallback spike |
| DEC-07 | BLOCKING | Khối lượng quy đổi, đơn vị, bậc giá, VAT/fees, discount order và rounding | 004 | golden tariff cases signed off |
| DEC-08 | CONFIG | Quote TTL, tariff effective time zone và cache invalidation TTL | 004 | choose config values + tests |
| DEC-09 | BLOCKING | Shipment identity/external reference, draft required fields, cancel fee refund | 005 | business policy + transition approval |
| DEC-10 | BLOCKING | Trình tự lock khi confirm transaction order/wallet/reservation; retry on deadlock | 005,006,007 | concurrent real DB tests |
| DEC-11 | BLOCKING | Wallet representation VND, account mapping cho double entry, fee funding | 006,011 | accounting sample reconciled |
| DEC-12 | BLOCKING | VNPay Sandbox keys/signing/version/callback/IPN validation | 006 | signature verified in sandbox tests |
| DEC-13 | BLOCKING | Inventory SKU units, QC quarantine, reservation source ownership | 007 | warehouse sample cases |
| DEC-14 | CONFIG | Hub backlog threshold; manifest/leg SLA and custody signoff method | 008 | demo use cases + edge tests |
| DEC-15 | BLOCKING | POD object provider, private bucket, max size/type, retention and signed read | 009 | secure upload/read/delete smoke |
| DEC-16 | BLOCKING | POD evidence mandatory fields, delivery retry/return cutoff | 009 | UC/BR-05 validation |
| DEC-17 | BLOCKING | Route solver OR-Tools compatibility; matrix origin, fallback, constraints | 010 | native/Java solver spike and dataset |
| DEC-18 | CONFIG | GPS accuracy and stale thresholds, background permission, transport polling/socket | 010,015 | device tests + latency measurement |
| DEC-19 | BLOCKING | COD partial remittance, cutoff timezone, dispute and payout simulation formula | 011 | Finance review + complete sample |
| DEC-20 | CONFIG | Which P0 notifications and dashboard KPI definitions | 012 | recipient scope and event catalog |
| DEC-21 | CONFIG | Audit retention, manual retry permission and config hot reload | 013 | security review |
| DEC-22 | BLOCKING | Excel format, preview errors, partial import vs atomicity, limits | 014 | sample workbook and contract |
| DEC-23 | BLOCKING | Offline mutation allowlist, conflict resolution UX, queue/media retention | 015 | Android airplane mode/crash tests |
| DEC-24 | BLOCKING | RQ1–RQ4 fixed datasets, load machine, comparable mode and metric export | 016 | experiment design approved before runs |
| DEC-25 | PROPOSED | REST `/api/v1`, JSON envelope, error taxonomy, `Idempotency-Key` name | all | OpenAPI review and ADR |
| DEC-26 | ACCEPTED (D1-03, 2026-10-13) | PostgreSQL 18.6 pinned digest; one DB/nine schemas; global V1/V2 stream; platform history; owner/migrator/runtime; local/test migrations and shared job | 001+ | Explicit user approval; ADR-0002; real PostgreSQL cold-start/upgrade/rerun/permissions PASS; P0 contracts outside demo remain pending |
| DEC-27 | PROPOSED | RabbitMQ event envelope/versioning, outbox retry backoff and DLQ tooling | 001,012 | fault-injection and schema review |
| DEC-28 | PROPOSED | `workflow` owner for multi-module orchestration; single transaction manager | 005,009 | integration test + Modulith verification |

## Chốt từng quyết định
1. Mở issue ghi owner, problem, constraints, alternatives, tradeoffs, evidence, chosen/decisionDate.
2. Nếu thay đổi kiến trúc, thêm ADR ở `docs/architecture/decisions/` rồi sửa affected design/spec/tests.
3. Đánh dấu `ACCEPTED` hoặc `DEFERRED` có lý do; không xóa dòng còn mở. Nếu chưa chốt nhưng không ảnh hưởng code ngay, Codex chỉ scaffold phần độc lập.
4. Không dùng số liệu demo hoặc source code đã viết để suy ngược thành quyết định đúng mà không đánh giá.
