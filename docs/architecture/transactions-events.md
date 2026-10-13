# Transaction, Event Delivery, Idempotency & Recovery

**Nguồn:** [REPORT] BR-01–BR-21; Chương 4 §4.2.2–4.2.3 và §4.3.1. Contract wire format là [PROPOSED].

## 1. Transaction boundary

| Use case | Đồng bộ trong một DB transaction | Bất đồng bộ sau commit |
| --- | --- | --- |
| Confirm shipment | validate quote, reserve WMS nếu áp dụng, charge wallet, confirm shipment, idempotency record | notification, reporting projection |
| Cancel shipment | verify cancellation threshold, release remaining reservation, refund rule, cancel shipment | notification |
| Shipment delivery with COD | assignment valid, delivery result/POD metadata, COD actual collected, shipment tracking | notification |
| Hub handover | validate parcel/manifest/custody, register source record idempotently | alerts, dashboard |
| VNPay credit | verified provider result, one credit + ledger + payment intent outcome | notification |
| Close COD period | re-check eligibility, unique closed membership, totals, status CLOSED | notification/report export |

Không kéo mạng bên ngoài vào transaction, không giả định `@Transactional` trên phương thức self-invocation hoạt động; đảm bảo coordinator/port đồng bộ dùng cùng transaction manager.

## 2. Idempotency contract [PROPOSED]
`Idempotency-Key` bắt buộc cho non-idempotent HTTP command. Scope = authenticated actor/merchant + operation name + key; persist canonical request fingerprint, status (`IN_PROGRESS`, `COMPLETED`, `FAILED_FINAL`), response/body ref, createdAt/expiry. Hai request cùng key/payload: một tác động, trả kết quả cũ hoặc trạng thái pending; cùng key/payload khác: `409 IDEMPOTENCY_CONFLICT`. Request key không thay unique business references. Retry có thể diễn ra trên instance khác.

## 3. Reliable messages [PROPOSED]
- Outbox trong transaction của producer, persistent event ID, schema version, aggregate ID, occurredAt, trace ID.
- Worker publish durable với bounded retries; worker restart/publish lặp không mất dữ liệu.
- Inbox theo consumer+event ID, unique constraint; transaction gồm apply effect + mark processed.
- RabbitMQ at-least-once: duplicate/out-of-order delivery là bình thường; không quảng bá exactly-once transport.
- Dead-letter/failed job có lỗi, attempt, lastAttemptAt và operator recovery có audit.
- Event chỉ chứa dữ liệu tối thiểu; consumer phải dùng public query khi cần thêm dữ kiện trong phạm vi quyền.

## 4. Failure matrix

| Tình huống | Hành vi bắt buộc |
| --- | --- |
| Mất kết nối sau server commit | Client retry cùng key và nhận lại result; không commit lại |
| Deadlock / conflict DB | bounded retry toàn transaction ở application boundary khi an toàn; không retry vô hạn |
| Pricing cache mất | đọc PostgreSQL bảng giá versioned; không dùng dữ liệu sai |
| VNPay callback trùng | verify, return already processed; không ghi có lại |
| Callback không rõ / timeout | `UNKNOWN/PENDING_VERIFICATION`, tra soát sau; không ghi thành công |
| Message đến trễ | kiểm tra state, version và idempotency tại domain |
| RabbitMQ down | outbox còn trong DB; nghiệp vụ chính vẫn hoàn tất nếu không cần message synchronous |
| Mobile offline | queue bền + server revalidate assignment; conflict rõ ràng |
| Cross-module transaction fail | rollback tất cả write nguyên tử, trả lỗi nghiệp vụ phù hợp |

## 5. Observability
Gắn request/trace ID với nghiệp vụ, không log token, credentials, GPS chi tiết không cần thiết, POD URL và thông tin thanh toán. Instrument outbox lag, pending callback, retry, conflict, failed jobs.
