# First slice — design và contract decision packet

DESIGN_PROPOSED, không endpoint/field/table implicit approval. Owners identity credential/enrollment/session, merchant personal business profile/org/membership/address/pickup/catalog (D03/05), routing adapter nếu approved. Giữ9 modules, không âm thầm đổi AuthenticatedActor.

| Principal | Candidate allowed | Guard/deny |
|---|---|---|
| Guest R01 | Public catalog/coverage/point/enrollment entry | Redaction/abuse, no Shipment/private address/role allocation |
| Sender R18 | Enrol/profile/personal address create/read/update/archive theo contract | Current self owner, không Merchant privilege/membership bắt buộc |
| Owner R02 | Org/profile/invite/delegate/pickup | Active member+explicit delegation, no security-admin assignability |
| Manager R19 | Delegated staff/branch/point | Current bounds, no default bank/owner/global change |
| Operator R03 | Delegated point/address | Active member+capability, no default staff management |
| Identity Admin R05 | Existing approved Identity only | SYSTEM Realm unchanged, no Merchant point DML mặc định |
| Recipient R10 | Public lookup khi L009 | No IAM/portal/private writes |

API decision packet: D01 enrollment verification/purpose/lifetime/transport/anti-enumeration/abuse/error/replay khác current admin provisioning. D02 context selection/trusted principal/owner membership API/delegation/current revocation/version/transaction; không body actor/owner. D03 versioned catalog/coverage/public Station dataset/raw-structured-confirmed provenance/create-read-update-archive/geocode timeout/manual selection. Exact routes/DTO/state/error/ETag/idempotency/permission phải accepted OpenAPI trước code, không tự chốt ở đây.

DB decision: merchant profile/membership/points/catalog proposal; Identity proof/authority delta qua reviewed forward migration sau V6, empty+upgrade/backfill/runtime privileges; không edit checksum/demo conversion. Scoped references/uniques/optimistic revision policy cần review.

Transaction: merchant mutation+audit+receipt/outbox owner atomic; Identity owns enrollment/session transaction. Initial org/enrollment nếu cần atomic cross-owner phải reviewed workflow/public APIs/same DB/lock protocol, không commit Identity-owned transaction bằng ambient wrapper. Nếu chọn onboarding nhiều bước phải explicit pending/recovery/compensation và no partial active authority (D02 unresolved). External verification/geocode ngoài DB transaction; cache không authoritative membership. Replay sau current authorization; refresh single-flight/lost rotation response re-auth.

FE: current app/frontend/web/src/App.tsx React Router, public catalog/enrol choice; Sender profile/address; Merchant context/profile/staff/pickup list/form; accepted typed client trong packages/api-client, không dùng Demo token cho production. URLs/layout mới chờ contract/UX review, không repo riêng. Keyboard/focus/error labels/text statuses/responsive/PII mask; loading/empty/error/ambiguous/pending/conflict/revoked-session; không localStorage secrets. Native không first packet vì không Courier use case, later RN requirements giữ target.

Fixtures synthetic S-A/S-B, A/B merchants, delegated/revoked staff, Identity-only admin, versioned catalog/point, approved verification sandbox. PG cold/upgrade/privileges/scoped FK/unique/replay/concurrent update/revoke race/audit rollback; HTTP production filter chain; browser AC-FIRST01..07; không mock authority/PII thật.
