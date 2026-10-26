# Tasks L003 — sender-merchant-onboarding

NOT_STARTED / mapping PROPOSED. Một authorization theo coherent BE+FE capability, không Owner checkpoints routine theo từng layer.

- [ ] T-V3-L003-01: Duyệt requirement/guard trong [spec](spec.md), chốt D-V3-01; D-V3-02; D-V3-03; D-V3-05; accepted API/event/state/data contract trước code.
- [ ] T-V3-L003-02: Viết fixture/test cho Guest chọn loại→xác minh→đúng profile/org; revoke giữa form và submit phải deny; positive/deny/rollback/race/replay, expected DB changes review trước migration.
- [ ] T-V3-L003-03: Implement Sender profile, org/membership/delegation/lifecycle; credential chỉ Identity; data scope Personal business profile/membership dưới merchant là proposal; owner API, V1–V6 bất biến, PostgreSQL empty/upgrade/rollback proof.
- [ ] T-V3-L003-04: Tích hợp Public chọn đăng ký, Sender profile, Merchant staff/context qua accepted typed client; error/loading/empty/unknown/conflict/revoked-session/accessibility.
- [ ] T-V3-L003-05: Unit guard/state; ArchUnit boundaries; contract conformance; PostgreSQL empty/upgrade/permissions/constraints/rollback; HTTP/filter-chain tenant/assignment/expiry; concurrency/replay; browser happy/deny/recovery; native device nếu applicable; cross-module regression; CI SHA/report/skips/artifact provenance. Đối chiếu required_test từng BC; không H2/mock thành PG/runtime evidence.
- [ ] T-V3-L003-06: Sửa defect trong scope, cập nhật BC/receipts/SHA, scoped commit/push/source CI, incremental PR/actual integration checks, dừng Owner merge.

DoR: Owner duyệt mapping/decisions, API/event/state/data owner và fixtures; không BLOCKING trong slice; positive/deny/concurrency/recovery đã thiết kế; canonical docs/codex/definition-of-ready.md.

DoD: BE+HTTP+DB+FE thật, PostgreSQL invariants/guards và browser/native evidence phù hợp, source+PR CI pass; BC/test/evidence cập nhật; Owner merge/acceptance riêng.
