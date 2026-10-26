# Tasks L023 — bulk-shipment-import

NOT_STARTED / mapping PROPOSED. Một authorization theo coherent BE+FE capability, không Owner checkpoints routine theo từng layer.

- [ ] T-V3-L023-01: Duyệt requirement/guard trong [spec](spec.md), chốt DEC-22; accepted API/event/state/data contract trước code.
- [ ] T-V3-L023-02: Viết fixture/test cho Mixed valid/invalid→preview→commit theo policy→sửa/retry; không auto-confirm/charge; positive/deny/rollback/race/replay, expected DB changes review trước migration.
- [ ] T-V3-L023-03: Implement Validate file/preview, row dedup, commit draft qua canonical booking; data scope Orders import job/row refs; private temp file retention; owner API, V1–V6 bất biến, PostgreSQL empty/upgrade/rollback proof.
- [ ] T-V3-L023-04: Tích hợp Merchant upload/preview/correct/retry/status/error download qua accepted typed client; error/loading/empty/unknown/conflict/revoked-session/accessibility.
- [ ] T-V3-L023-05: Unit guard/state; ArchUnit boundaries; contract conformance; PostgreSQL empty/upgrade/permissions/constraints/rollback; HTTP/filter-chain tenant/assignment/expiry; concurrency/replay; browser happy/deny/recovery; native device nếu applicable; cross-module regression; CI SHA/report/skips/artifact provenance. Đối chiếu required_test từng BC; không H2/mock thành PG/runtime evidence.
- [ ] T-V3-L023-06: Sửa defect trong scope, cập nhật BC/receipts/SHA, scoped commit/push/source CI, incremental PR/actual integration checks, dừng Owner merge.

DoR: Owner duyệt mapping/decisions, API/event/state/data owner và fixtures; không BLOCKING trong slice; positive/deny/concurrency/recovery đã thiết kế; canonical docs/codex/definition-of-ready.md.

DoD: BE+HTTP+DB+FE thật, PostgreSQL invariants/guards và browser/native evidence phù hợp, source+PR CI pass; BC/test/evidence cập nhật; Owner merge/acceptance riêng.
