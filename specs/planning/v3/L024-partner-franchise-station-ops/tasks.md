# Tasks L024 — partner-franchise-station-ops

NOT_STARTED / mapping PROPOSED. Một authorization theo coherent BE+FE capability, không Owner checkpoints routine theo từng layer.

- [ ] T-V3-L024-01: Duyệt requirement/guard trong [spec](spec.md), chốt D-V3-02; D-V3-05; D-V3-08; accepted API/event/state/data contract trước code.
- [ ] T-V3-L024-02: Viết fixture/test cho Assigned staff→counter/scan/count/handover; foreign Station deny; positive/deny/rollback/race/replay, expected DB changes review trước migration.
- [ ] T-V3-L024-03: Implement Partner binding qua merchant/identity API; dispatch custody/counter; data scope Partner org ở merchant, custody ở dispatch, revenue ở finance; owner API, V1–V6 bất biến, PostgreSQL empty/upgrade/rollback proof.
- [ ] T-V3-L024-04: Tích hợp Partner counter/stock/handover/revenue scoped qua accepted typed client; error/loading/empty/unknown/conflict/revoked-session/accessibility.
- [ ] T-V3-L024-05: Unit guard/state; ArchUnit boundaries; contract conformance; PostgreSQL empty/upgrade/permissions/constraints/rollback; HTTP/filter-chain tenant/assignment/expiry; concurrency/replay; browser happy/deny/recovery; native device nếu applicable; cross-module regression; CI SHA/report/skips/artifact provenance. Đối chiếu required_test từng BC; không H2/mock thành PG/runtime evidence.
- [ ] T-V3-L024-06: Sửa defect trong scope, cập nhật BC/receipts/SHA, scoped commit/push/source CI, incremental PR/actual integration checks, dừng Owner merge.

DoR: Owner duyệt mapping/decisions, API/event/state/data owner và fixtures; không BLOCKING trong slice; positive/deny/concurrency/recovery đã thiết kế; canonical docs/codex/definition-of-ready.md.

DoD: BE+HTTP+DB+FE thật, PostgreSQL invariants/guards và browser/native evidence phù hợp, source+PR CI pass; BC/test/evidence cập nhật; Owner merge/acceptance riêng.
