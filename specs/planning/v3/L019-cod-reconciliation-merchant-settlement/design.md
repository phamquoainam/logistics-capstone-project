# Design L019 — cod-reconciliation-merchant-settlement

DESIGN_PROPOSED. [Structured card](../../../../docs/reconciliation/v3/spec-register.json), [scope](spec.md).

## Backend và ownership

Eligibility recheck/unique closed membership/cutoff/dispute/payout evidence. Source thật app/backend/src/main/java/com/elogistic/backend; một artifact/chín domains. Primary spec accountability không cấp DML trên consumer data: CSV ghi primary domain từng BC; Parcel/label ở orders, SKU ở warehouse, custody/assignment ở dispatch, ledger ở finance. Consumer chỉ public api; workflow dùng local DB transaction. Owner mới catalog/profile/case/risk cần D-V3-03/05.

## Frontend

Finance preview/close/approve; Merchant đối chiếu/chứng từ/dispute. Reuse app/frontend/web/src/App.tsx và React Router, không repo theo role. Loading/empty/validation/error/ambiguous/pending/conflict/unknown/revoked-session phải rõ; giữ input thích hợp, không success trước server. Keyboard/focus/text statuses/responsive/PII mask có browser evidence. Tác nghiệp native dùng app/frontend/shipper-mobile theo AGENTS; web không chứng minh camera/GPS/SQLite native.

## Data/API/event

Closed snapshot bất biến; payout/adjustment mới tham chiếu gốc. Eligibility recheck/unique closed membership/cutoff/dispute/payout evidence; exact API/event/DTO/permission/schema DECISION_REQUIRED trước code; contracts/ là nguồn duy nhất. Không endpoint/field/table mới được tự duyệt tại đây. [XC contracts](../../../../docs/reconciliation/v3/cross-module-contracts.md) giữ source of truth; wire schemas chỉ contracts/ sau review.

## Transaction và security

Mutation: stable key+scope/canonical payload; replay một effect, khác payload conflict; DB uniqueness/locking và rollback tests. Read-only không mutation. Identity Refresh strict single-flight/no blind retry. Current account/session AND explicit permission AND authoritative membership/resource/assignment scope AND current state/evidence; actors: R02,R11,R13. Actor/source/reason/correlation, append-only audit/evidence; PII tối thiểu, không secrets/raw payment trong log; retention D-V3-06. External I/O ngoài transaction; source+audit/outbox cùng commit; inbox/dedup trước consumer effect. Payment/callback chưa xác minh phải unknown/pending, không tự settled. Delivered+collected+confirmed remitted→reconcile→simulated payout; late data kỳ sau; giữ BR-01–21 theo applicability; xem business-baseline.md.

## Validation trước acceptance

Unit guard/state; ArchUnit boundaries; contract conformance; PostgreSQL empty/upgrade/permissions/constraints/rollback; HTTP/filter-chain tenant/assignment/expiry; concurrency/replay; browser happy/deny/recovery; native device nếu applicable; cross-module regression; CI SHA/report/skips/artifact provenance. Blockers DEC-19; D-V3-04, deadline trước first use. Không runtime/migration/benchmark mới trong task docs.
