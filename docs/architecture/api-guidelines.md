# HTTP API & Contract Policy — [PROPOSED]

**Lưu ý:** Báo cáo chưa cung cấp bộ OpenAPI chuẩn đã chốt. Mọi path dưới `specs/*/design.md` là **đề xuất** cần rà soát rồi ghi thành `contracts/openapi/elogistics.yaml` trước khi code.

## API consistency
- Base `/api/v1`; URI số nhiều; request/response JSON `camelCase` (DB `snake_case`).
- Xác thực JWT/OIDC/session là [BLOCKING] cần chốt ADR; token lấy từ header/secure session, không gửi trong URL.
- Mọi mutation không idempotent: `Idempotency-Key` + correlation ID. Dữ liệu owner/merchant từ trusted auth context hoặc server-side ownership validation, **không tin request body**.
- Output money VND bằng decimal string, timestamp ISO 8601 UTC + timezone business display; external user ID là opaque string.
- Pagination cursor ổn định cho tracking/ledger/list; filter/sort whitelist; safe rate-limit public endpoints.
- OpenAPI là source of truth; sinh client TS trong `packages/api-client`, không viết lại request type thủ công ở web/mobile.

## Success contract ví dụ
```json
{"success":true,"data":{"shipmentId":"uuid","status":"DRAFT"},"message":"Shipment created","requestId":"trace-id"}
```

## Error contract ví dụ
```json
{"success":false,"data":null,"message":"Invalid state transition","error":{"code":"INVALID_SHIPMENT_STATE","details":[]},"requestId":"trace-id"}
```

## HTTP semantics
- 200 query/update success; 201 created resource; 202 accepted asynchronous/unknown; 400 format; 401 not authenticated; 403 not authorized; 404 not found (hoặc 404 khi che giấu đối tượng ngoài quyền); 409 state/duplicate conflict; 422 semantic invalid; 429 throttled; 503 dependency unavailable. Không trả 500 cho lỗi validation thông thường.
- Các status là quy ước đề xuất, không mặc định báo cáo quy định chính xác mã 409/422.
- Phân biệt `UNKNOWN` và `FAILED` khi thanh toán/mạng chưa xác định; frontend giữ form/dữ liệu và hiển thị action tra cứu.

## Security and testing
- Kiểm tra object authorization khi đọc và khi ghi; negative tests cross-merchant, warehouse/hub scope, expired assignment.
- Test OpenAPI validation, generated client, request replay/different payload, duplicate business key, API downgrade/compatibility.
