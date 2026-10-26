# Tasks L014 — delivery-dispatch-and-route

NOT_STARTED / mapping PROPOSED. Một authorization theo coherent BE+FE capability, không Owner checkpoints routine theo từng layer.

- [ ] T-V3-L014-01: Duyệt requirement/guard trong [spec](spec.md), chốt DEC-14; DEC-17; DEC-18; accepted API/event/state/data contract trước code.
- [ ] T-V3-L014-02: Viết fixture/test cho Kiện đích hợp lệ→assign/load→route; race/revoked không tạo hai active; positive/deny/rollback/race/replay, expected DB changes review trước migration.
- [ ] T-V3-L014-03: Implement Delivery pool/assignment; routing proposal/confirm giữ completed stops; data scope Dispatch assignment duy nhất; routing route version riêng; owner API, V1–V6 bất biến, PostgreSQL empty/upgrade/rollback proof.
- [ ] T-V3-L014-04: Tích hợp Dispatcher pool/route/reassign; Team Leader đề xuất; Courier nhận tải qua accepted typed client; error/loading/empty/unknown/conflict/revoked-session/accessibility.
- [ ] T-V3-L014-05: Unit guard/state; ArchUnit boundaries; contract conformance; PostgreSQL empty/upgrade/permissions/constraints/rollback; HTTP/filter-chain tenant/assignment/expiry; concurrency/replay; browser happy/deny/recovery; native device nếu applicable; cross-module regression; CI SHA/report/skips/artifact provenance. Đối chiếu required_test từng BC; không H2/mock thành PG/runtime evidence.
- [ ] T-V3-L014-06: Sửa defect trong scope, cập nhật BC/receipts/SHA, scoped commit/push/source CI, incremental PR/actual integration checks, dừng Owner merge.

DoR: Owner duyệt mapping/decisions, API/event/state/data owner và fixtures; không BLOCKING trong slice; positive/deny/concurrency/recovery đã thiết kế; canonical docs/codex/definition-of-ready.md.

DoD: BE+HTTP+DB+FE thật, PostgreSQL invariants/guards và browser/native evidence phù hợp, source+PR CI pass; BC/test/evidence cập nhật; Owner merge/acceptance riêng.
