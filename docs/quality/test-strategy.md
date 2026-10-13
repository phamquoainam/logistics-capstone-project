# Test Strategy & Research Evidence

**Nguồn:** [REPORT] §2.6–2.7; RQ1–RQ4 Phần A; Chương 3 §3.7. Công cụ JUnit/Mockito/Testcontainers/Playwright/k6 là [PROPOSED].

## Test pyramid / evidence
| Cấp | Mục đích | Công cụ ứng viên |
| --- | --- | --- |
| Domain unit | State transition, pricing math, quantity, allocation, COD guards | JUnit / Mockito |
| Repository integration | Real PostgreSQL constraints, transaction, lock, migration, RLS-equivalent auth check | Testcontainers + PostgreSQL |
| API/contract | OpenAPI validate, error/status/idempotency, authz | Spring integration + generated client |
| Web E2E | Merchant–Dispatcher–Finance journey, error recovery | Playwright |
| Mobile device | Camera/POD/GPS/offline/restart and native permission | Android emulator + physical-device smoke |
| Concurrency | Parallel confirm, reserve, debit, assignment, close period | Custom concurrent integration + k6 |
| Resilience | RabbitMQ/Redis/VNPay unavailable, replay, crash/restart | Compose fault tests |
| Research | RQ1–RQ4 comparable setups, raw metrics | k6 + benchmark harness + metrics |

## Research plan
- **RQ1**: transaction+DB constraint/idempotency baseline so với phương án Redis lock phụ trợ đa-instance, đo invariant breaches, throughput, p95/99, retries, conflicts; Redis lock không thay correctness.
- **RQ2**: same workload/rules with no cache, cold cache, warm cache, changed version, Redis outage; đo latency, SQL query count, cache hit và quote correctness.
- **RQ3**: outbox/inbox enabled vs naive asynchronous baseline trong môi trường an toàn; inject duplicate, failure, restart, reorder, measure completion/duplicate effects/recovery.
- **RQ4**: baseline order or nearest neighbor vs route proposal; same road matrix/stops/constraints; đo distance, estimated travel time, compute time, valid routes; tối đa 50 điểm/10s là NFR-P03.

## Definition of Done per feature
1. Spec/design/tasks cập nhật, mọi REQ có AC và test case ID.
2. No test bypass of authz, no fail invariant under concur/replay scenarios.
3. Flyway apply from empty DB, endpoint contract generated, log/metrics non-sensitive.
4. API/UI errors có cơ chế retry/chờ/xung đột; seed/demo repeatable.
5. CI passes; link evidence into `docs/quality/traceability.md`.

## Evidence format
`testId, requirementIds, environment, commitHash, dataSeed, runConfig, timestampUTC, result, metricFile, notes` (CSV/JSON); raw test results lưu `quality/evidence/` sau khi chạy, không tạo số liệu mô phỏng gắn nhãn kết quả đo.
