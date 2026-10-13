# Frontend & Mobile Architecture — [PROPOSED] dựa trên §4.1.3, §4.2.4

## Web: React + TypeScript
`apps/web/src/`: `features/<feature>/{api,components,pages,hooks,types}`, `components/` chung, `navigation/`, `api/`, `platform/`, `styles/`. Role-gated routes chỉ tăng UX, server authZ mới quyết định. API client typed generate từ OpenAPI; error/loading/empty/conflict states bắt buộc. Các form quan trọng giữ dữ liệu khi lỗi mạng hoặc `409`, không gửi hai lần tùy tiện.

## React Native Shipper + TypeScript
`apps/shipper-mobile/src/`: `features/{tasks,scan,delivery,pod,cod,routing,location,sync}`, `navigation/`, `api/`, `platform/{camera,storage,location,secure-storage}`, `components/`.

- **[REPORT]** Native scan/POD/location và queue offline là P0; Expo development build chỉ là đề xuất, Expo Go không đủ để xác nhận background GPS.
- Session location có bật/tắt và quyền riêng; GPS sample có `recordedAt`, `receivedAt`, `accuracy`, `source`, `sessionId` và kiểm tra staleness.
- Queue SQLite durable cho **các hành động offline được chấp thuận**; payload có operationId bất biến, `assignmentVersion`/`shipmentVersion` và trạng thái `PENDING`, `SENDING`, `ACKNOWLEDGED`, `CONFLICT`, `FAILED_ACTION_REQUIRED`.
- Crash/restart không mất pending item; server reject task đã gán lại, không tự ghi nhận giao thành công giả; hiển thị hành động cần xử lý.
- Credentials lưu kho bảo mật thiết bị, không vào SQLite queue/log; dữ liệu POD cục bộ có retention và chính sách bảo vệ.
- Provider bản đồ hiển thị ≠ routing matrix; license, coverage VN và API quota cần xác nhận qua `open-questions.md`.

## UI accessibility / vận hành
- Trạng thái nghiệp vụ có text nhãn rõ, không chỉ dùng màu.
- Bảng dữ liệu có loading, empty, retry, pagination, ownership-aware actions.
- Audit và exception UI chỉ mở theo quyền; không thêm chức năng sửa ledger trực tiếp.
