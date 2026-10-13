# Business Invariant Test Catalog (P0)

**Nguồn:** [REPORT] BR-01–BR-21; case design [PROPOSED] triển khai bằng unit/real-DB/concurrency/fault. Không đánh dấu PASS trước khi chạy.

| Test ID | BR | Chủ đề | Tình huống xác nhận |
| --- | --- | --- | --- |
| INV-01 | BR-01 | Shipment state transitions | Gửi event OUT_OF_ORDER sau DELIVERED; xác minh state guard và tracking không ghi lùi |
| INV-02 | BR-02 | Repeated operation | Hai request cùng key & payload; key trùng payload khác; retry sau lost response, expect one business effect |
| INV-03 | BR-03 | Cancel cutoff | Direct after picked up hoặc WMS after picking: cancel reject; before cutoff release only remaining |
| INV-04 | BR-04 | Active assignment | Hai dispatcher gán 2 shipper cùng leg đồng thời; kiểm tra unique 1 active assignment |
| INV-05 | BR-05 | POD tied to attempt | Cố ghi DELIVERED với POD thiếu/wrong attempt; phải từ chối |
| INV-06 | BR-06 | Non-negative stock availability | Concurrent reserve exceed eligible stock; reject và không âm available |
| INV-07 | BR-07 | Reservation conservation | reserved=consumed+released+remaining; reject pick/release vượt remaining |
| INV-08 | BR-08 | Append-only inventory movements | Adjust không có chứng từ/lý do; kiểm kê chỉ ghi variance, không ghi đè stock |
| INV-09 | BR-09 | Hub custody and scan dedupe | Scan trùng/wrong hub/wrong manifest; WMS outbound chưa xác nhận hub received |
| INV-10 | BR-10 | Chargeable weight | real/volumetric, rounding at unit boundaries, zero/negative dimensions |
| INV-11 | BR-11 | Pricing rule order | Golden cases zone/tier/service/surcharge/discount/rounding; missing rule rejected |
| INV-12 | BR-12 | Immutable tariff and quote snapshot | Tariff publish new version, old confirmed quote unchanged; expired quote rejected |
| INV-13 | BR-13 | Wallet/confirm atomicity | Insufficient money+enough stock and enough money+insufficient stock: rollback full confirm |
| INV-14 | BR-14 | Verified payment single credit | Invalid signature/amount, callback replay, timeout UNKNOWN, exactly one ledger credit after valid callback |
| INV-15 | BR-15 | Ledger immutable and balanced | Posting sum debit==credit; refund via new linked operation; duplicate refund impossible |
| INV-16 | BR-16 | Separate COD milestones | Shipper collected/submitted not finance confirmed; shipper cannot approve own remittance |
| INV-17 | BR-17 | Eligibility recheck at close | Dispute or partial remittance, concurrent late update, cutoff: cannot sneak into CLOSED |
| INV-18 | BR-18 | Closed reconciliation immutable | Race periods for same COD, duplicate payout, attempted UPDATE closed period rejected |
| INV-19 | BR-19 | Confirmed address snapshot | Ambiguous geocoding needs explicit confirmation; admin catalog changes not mutate past shipment |
| INV-20 | BR-20 | GPS validity/freshness | Outside session, stale, future, low accuracy, out-of-order GPS not shown as real-time current |
| INV-21 | BR-21 | Route against current tasks | Route planned before reassign, confirm after reassign rejected; route does not auto change assignments |

## Cách triển khai
- Map mỗi `INV-XX` với feature `TEST-<id>-INT/CONC/FAULT` và row bằng chứng tại `docs/quality/traceability.md`.
- Kiểm tra dữ liệu hậu điều kiện trực tiếp trên PostgreSQL, không chỉ đọc HTTP response hoặc mock repository.
- Ít nhất một lần inject concurrent/replay test dưới multi-instance nếu nghiên cứu RQ1 yêu cầu.
- Test timeout/unknown provider phải chứng minh trạng thái chờ và không có tác động tiền sai.
- Evidence gồm commit, fixture, initial DB, threads/load, SQL invariant assertions, timestamp và log sanitized.
