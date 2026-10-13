# Definition of Ready (DoR) — Gate trước triển khai feature

Một feature chỉ chuyển từ `DRAFT_SPEC` sang `READY_FOR_IMPLEMENTATION` khi:
- [ ] `spec.md` có actor, scope, REQ IDs, journeys, edge cases và AC Given/When/Then kiểm chứng được.
- [ ] Map được mã FR/BR/NFR/UC gốc; không có hành vi lạ phạm vi P0.
- [ ] `design.md` ghi code owner, transaction, API, data schema, event, error contract và auth object.
- [ ] Tất cả `BLOCKING` trong phạm vi feature đã có quyết định (ADR hoặc accepted configuration) hoặc split task chỉ làm phần độc lập.
- [ ] `tasks.md` có thứ tự, dependency, deliverable, test và verify gate, không còn task mơ hồ “làm backend”.
- [ ] Dependencies có public contract rõ; các đường đi qua workflow/transaction được review.
- [ ] Xác định test fixtures cho success, invalid/expired, duplicate, concurrent và failure.

**Nếu chưa đủ:** Codex chỉ được lập câu hỏi, sửa spec và thực hiện spike tách biệt, không triển khai business code vượt assumptions.
