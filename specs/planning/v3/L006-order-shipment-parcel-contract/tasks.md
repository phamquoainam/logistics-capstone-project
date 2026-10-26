# Tasks L006 — order-shipment-parcel-contract

NOT_STARTED / mapping PROPOSED. Một authorization theo coherent BE+FE capability, không Owner checkpoints routine theo từng layer.

- [ ] T-V3-L006-01: Duyệt requirement/guard trong [spec](spec.md), chốt D-V3-04; DEC-09; DEC-10; accepted API/event/state/data contract trước code.
- [ ] T-V3-L006-02: Viết fixture/test cho Draft→kiểm điều kiện confirm; reject illegal transition/hold/foreign object; positive/deny/rollback/race/replay, expected DB changes review trước migration.
- [ ] T-V3-L006-03: Implement Aggregate/state ownership, direct/WMS cancel cutoff/hold; workflow qua API; data scope Orders giữ Shipment/Parcel/snapshot/tracking/attempt/POD; owner API, V1–V6 bất biến, PostgreSQL empty/upgrade/rollback proof.
- [ ] T-V3-L006-04: Tích hợp Trang Shipment trình bày state/errors, không UI riêng cho contract qua accepted typed client; error/loading/empty/unknown/conflict/revoked-session/accessibility.
- [ ] T-V3-L006-05: Unit guard/state; ArchUnit boundaries; contract conformance; PostgreSQL empty/upgrade/permissions/constraints/rollback; HTTP/filter-chain tenant/assignment/expiry; concurrency/replay; browser happy/deny/recovery; native device nếu applicable; cross-module regression; CI SHA/report/skips/artifact provenance. Đối chiếu required_test từng BC; không H2/mock thành PG/runtime evidence.
- [ ] T-V3-L006-06: Sửa defect trong scope, cập nhật BC/receipts/SHA, scoped commit/push/source CI, incremental PR/actual integration checks, dừng Owner merge.

DoR: Owner duyệt mapping/decisions, API/event/state/data owner và fixtures; không BLOCKING trong slice; positive/deny/concurrency/recovery đã thiết kế; canonical docs/codex/definition-of-ready.md.

DoD: BE+HTTP+DB+FE thật, PostgreSQL invariants/guards và browser/native evidence phù hợp, source+PR CI pass; BC/test/evidence cập nhật; Owner merge/acceptance riêng.
