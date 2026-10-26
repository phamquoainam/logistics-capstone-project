# BE+FE capability delivery — V3 candidate

Mỗi [BC row](capability-crosswalk.csv) là source requirement riêng; [spec cards](spec-register.json) ghi BE/FE/DB/auth/failure/test/DoR/DoD. Một logical spec có thể nhiều coherent packages, không tách Owner prompts theo controller/UI/test. Một authorization hoàn chỉnh gồm approved spec→fixtures/tests→migration/domain→application/HTTP→frontend→PG/security/concurrency/browser/native→fix defects→scoped commit/push→source CI→SDD evidence→incremental PR→PR CI→Owner merge decision.

| Layer | Deliverable/evidence |
|---|---|
| Business | Approved BC subset/actor/scope/guard/state/denial/recovery/compensation/owner; mapping approval khác implementation status |
| Contract | Accepted API/event/DTO/error/auth/version/idempotency; schema/status/headers/negative tests; contracts/ nguồn duy nhất, SDK sau review |
| Database | Approved forward Flyway, empty+upgrade từ V1–V6/runtime permissions/constraints; no ORM update/repair; backfill/rollback compatibility |
| Backend | Invariants/current object auth/local transaction qua API/workflow, audit/outbox atomic; external I/O ngoài DB transaction; explicit unknown/recovery |
| Frontend | Public/Sender/Merchant/Operations/Admin trong React Router hiện hữu, RN cho Courier; forms/list/detail/action theo BC; loading/empty/validation/error/ambiguous/pending/conflict; re-auth/revocation; permission navigation và direct API denial; PII mask/keyboard/text/responsive |
| Tests | Unit guards/state; ArchUnit negative boundaries; contract conformance; real PG transaction/permissions/constraints/rollback; HTTP/filter-chain tenant/assignment/expired scope; races/replay; browser happy/deny/recovery; native camera/GPS/SQLite khi applicable; cross-module regression |
| CI/evidence | SHA/command/env/version/exit/pass/fail/skips exact, report/artifact links/digests, privacy/owned cleanup; source và actual PR CI riêng. H2/mock không PG/runtime, planned test ID không receipt |
| Closure | Mapping PROPOSED→REVIEWED→APPROVED do review; implementation NOT_STARTED/PARTIAL/IMPLEMENTED/VERIFIED/BLOCKED/NOT_VERIFIED theo evidence; DONE đúng task gate, không parent AC auto accepted |

Owner gates giữ cho business/permission/material accepted contract/security architecture/migration ngoài authorization/production infrastructure+credentials/high-critical security/PR merge. Routine implementation/tests/fixes/source CI/docs closure/PR readiness trong cùng authorization nếu đã cấp. Docs reconciliation không cấp production coding.

DoR theo [canonical checklist](../../codex/definition-of-ready.md): approved scope/data/state/contracts/owner, no BLOCKING trong slice, fixture success/deny/concurrency/recovery, actual dependencies hoặc minimum contract slices đã chứng minh. DoD đủ layers trên và regression/rollback, no fake source state, CI/evidence+documented findings; Owner acceptance riêng.

First-use: tenant guard trước Merchant CRUD; personal ownership trước Sender data; snapshot/minimum ledger trước fee; custody trước pickup; attempt/POD trước DELIVERED; Cashier/SoD trước remittance; closed recon trước payout; audit trước privileged mutation; redaction/abuse limits trước public lookup. Không đợi L027/028. L018/L017/L020 có core sớm/full wave sau; phải test core thật.

Lệnh cho implementation sau: existing scripts/ci/run-backend.sh/Maven Wrapper verify trong isolated approved runtime; giữ exact finite report guard. Web app/frontend/web scripts lint/typecheck/build/test/test:e2e, accepted client/secure fixture harness. Mobile theo AGENTS của app/frontend/shipper-mobile. PG cold/upgrade/rollback/races theo Testcontainers/CI conventions. Reconciliation không local server/migrate/business E2E; PR backend CI chạy regression theo policy nguyên vẹn.

M1 customer foundation thuộc F1; M2 booking F1 và staffed Station F2; M3 physical network F2; M4 money/exceptions F2/F3; full Enterprise/Governance F4, reliability F5. Không estimate/benchmark mới: D08 chốt nguồn lực/milestone trước commitment, không cắt CORE. Mỗi capability package có đủ BE+FE+tests+evidence, không separate repos cho role.
