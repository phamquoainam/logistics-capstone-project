# Feature Dependency Order / Release Slices

Dependency ở từng feature đã liệt kê tại `spec.md`. Hướng ưu tiên:

1. Foundation `001` → Auth `002` → Merchant/address `003`.
2. Pricing `004` → Wallet `006` → WMS `007` → Shipment confirm `005` (Shipment draft có thể phát triển sớm hơn dưới contract đã chốt).
3. Dispatch/Hub `008` → Shipper POD/COD collection `009` → Routing/GPS `010` → COD reconciliation `011`.
4. Notification/dashboard `012`, Admin `013`, Bulk import `014`, Offline sync `015` có thể phát triển song song sau core contracts.
5. RQ harness `016` chuẩn bị fixture ngay từ đầu; kiểm chứng sau khi luồng P0 hoạt động.

**Tránh vòng tròn module:** `005` phụ thuộc khả năng Finance/WMS thông qua `workflow` chứ `finance`/`warehouse` không được phụ thuộc ngược `order`. Khi implement, có thể tạo các API contract được chốt sớm, không gắn implementation circular imports.

## MVP demo slice 1
Auth → merchant point → versioned pricing → shipment DRAFT + show tracking, nhưng **chưa** được gọi đó là P0 end-to-end.

## MVP demo slice 2
Shipment confirm atomic ↔ fee debit ↔ reservation; tests cạnh tranh/xung đột; hoàn thành các bất biến RQ1 cơ bản.

## MVP demo slice 3
WMS outbound → hub manifest/custody → assign → route confirm → shipper POD → COD collection/remittance/closing → notification.
