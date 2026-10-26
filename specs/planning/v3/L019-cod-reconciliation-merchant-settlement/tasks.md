# Tasks L019 — cod-reconciliation-merchant-settlement

NOT_STARTED / mapping PROPOSED. Một authorization theo coherent BE+FE capability, không Owner checkpoints routine theo từng layer.

- [ ] T-V3-L019-01: Duyệt requirement/guard trong [spec](spec.md), chốt DEC-19; D-V3-04; accepted API/event/state/data contract trước code.
- [ ] T-V3-L019-02: Viết fixture/test cho Delivered+collected+confirmed remitted→reconcile→simulated payout; late data kỳ sau; positive/deny/rollback/race/replay, expected DB changes review trước migration.
- [ ] T-V3-L019-03: Implement Eligibility recheck/unique closed membership/cutoff/dispute/payout evidence; data scope Closed snapshot bất biến; payout/adjustment mới tham chiếu gốc; owner API, V1–V6 bất biến, PostgreSQL empty/upgrade/rollback proof.
- [ ] T-V3-L019-04: Tích hợp Finance preview/close/approve; Merchant đối chiếu/chứng từ/dispute qua accepted typed client; error/loading/empty/unknown/conflict/revoked-session/accessibility.
- [ ] T-V3-L019-05: Unit guard/state; ArchUnit boundaries; contract conformance; PostgreSQL empty/upgrade/permissions/constraints/rollback; HTTP/filter-chain tenant/assignment/expiry; concurrency/replay; browser happy/deny/recovery; native device nếu applicable; cross-module regression; CI SHA/report/skips/artifact provenance. Đối chiếu required_test từng BC; không H2/mock thành PG/runtime evidence.
- [ ] T-V3-L019-06: Sửa defect trong scope, cập nhật BC/receipts/SHA, scoped commit/push/source CI, incremental PR/actual integration checks, dừng Owner merge.

DoR: Owner duyệt mapping/decisions, API/event/state/data owner và fixtures; không BLOCKING trong slice; positive/deny/concurrency/recovery đã thiết kế; canonical docs/codex/definition-of-ready.md.

DoD: BE+HTTP+DB+FE thật, PostgreSQL invariants/guards và browser/native evidence phù hợp, source+PR CI pass; BC/test/evidence cập nhật; Owner merge/acceptance riêng.
