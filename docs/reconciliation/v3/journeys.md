> Current canonical target follows [Owner account-first decision](../../decisions/customer-account-merchant-admission.md). Historical statements below are source context where superseded; accepted runtime/security unchanged.

# J1–J9 system-wide acceptance — PROPOSED

Planned scenarios, NOT_VERIFIED. Fixtures synthetic, không PII thật; UI-only state không proof custody/payment/POD.

## J1 — Retail Sender

Roles R01/R18/R07/R08/R09/R12/R13; specs L003–015/L018; milestone F1 core,F2 delivery,F3 closure.

- Prerequisite data: S-A/S-B verified personal accounts, catalog/coverage/rate và authorized Courier/Station, không membership.
- Happy: Guest đăng ký một tài khoản cá nhân→Identity verify/login→personal profile/address→quote/book/pay→pickup/custody→POD→owned tracking/receipt.
- Negative/security: S-B đọc UUID A deny; Guest online create deny; Sender không đọc Merchant rate.
- Recovery: Expired quote/requote, payment unknown/status, failed pickup/retry, delivery failure→return/support.
- Database/events: source rows/references/versions, immutable event/audit, financial/physical totals theo owner; before/after/rollback/replay, no foreign effects; projection không là proof source.
- Backend API: accepted request/response/status/error/headers/auth captures, direct-object denial; PG assertions/correlation chứng minh commit.
- Browser/frontend: portal đúng role từ đầu đến reload/persistence; loading/empty/error/unknown/conflict/revoked-session; direct URL/UUID deny. J9 thêm native airplane-mode/kill/restart, browser không thay device.
- CI/acceptance: TEST-V3-J1-FLOW/DENY/RECOVERY là IDs kế hoạch; source SHA/env/commands/exits/reports/skips/artifacts, cross-module regression và actual PR checks; Owner acceptance sau evidence, hiện NOT_VERIFIED.

## J2 — Merchant

Roles R02/R03/R19/R20/R11/R21/R07/R09; specs L003–010/L019/L020/L022/L023; milestone F1 core,F3 finance,F4 full.

- Prerequisite data: Merchants A/B, branch/warehouse delegation, tariff/stock nếu WMS, authoritative membership.
- Happy: Owner onboard/delegate/point→Operator import/book→Warehouse pick/pack→handover→tracking→statement.
- Negative/security: B UUID A deny; Manager no bank/owner change; Warehouse no Courier pickup; Support no POD write.
- Recovery: Revoke giữa preview/commit; mixed import errors; insufficient stock/fee rollback; financial dispute.
- Database/events: source rows/references/versions, immutable event/audit, financial/physical totals theo owner; before/after/rollback/replay, no foreign effects; projection không là proof source.
- Backend API: accepted request/response/status/error/headers/auth captures, direct-object denial; PG assertions/correlation chứng minh commit.
- Browser/frontend: portal đúng role từ đầu đến reload/persistence; loading/empty/error/unknown/conflict/revoked-session; direct URL/UUID deny. J9 thêm native airplane-mode/kill/restart, browser không thay device.
- CI/acceptance: TEST-V3-J2-FLOW/DENY/RECOVERY là IDs kế hoạch; source SHA/env/commands/exits/reports/skips/artifacts, cross-module regression và actual PR checks; Owner acceptance sau evidence, hiện NOT_VERIFIED.

## J3 — Walk-in

Roles R01/R08/R12/R33; specs L004/L005/L007/L008/L011/L018/L024; milestone F2 Station,F4 Partner.

- Prerequisite data: Assigned counter Station, walk-in verification policy/rate/cash receipt; Partner binding khi dùng.
- Happy: Staff verify→kiểm/cân→quote/book→payment receipt→intake/custody→label/biên nhận.
- Negative/security: Anonymous online create/foreign Station deny; no walk-in IAM; paid thiếu proof deny.
- Recovery: Ambiguous address/manual-approved flow; payment unknown; count mismatch→exception.
- Database/events: source rows/references/versions, immutable event/audit, financial/physical totals theo owner; before/after/rollback/replay, no foreign effects; projection không là proof source.
- Backend API: accepted request/response/status/error/headers/auth captures, direct-object denial; PG assertions/correlation chứng minh commit.
- Browser/frontend: portal đúng role từ đầu đến reload/persistence; loading/empty/error/unknown/conflict/revoked-session; direct URL/UUID deny. J9 thêm native airplane-mode/kill/restart, browser không thay device.
- CI/acceptance: TEST-V3-J3-FLOW/DENY/RECOVERY là IDs kế hoạch; source SHA/env/commands/exits/reports/skips/artifacts, cross-module regression và actual PR checks; Owner acceptance sau evidence, hiện NOT_VERIFIED.

## J4 — Last-mile exceptions

Roles R06/R09/R08/R10/R21/R14; specs L014–016/L020; milestone F2,F3 full support.

- Prerequisite data: Destination custody, valid assignment/version, accepted POD/attempt/retry/return policy.
- Happy: Failed attempt/reason→Dispatcher new assignment→retry POD hoặc return→sender handback.
- Negative/security: Stale courier deny; Dispatcher no fake POD; không xóa failed attempts.
- Recovery: Lost upload/foreign POD deny; orphan cleanup; return count mismatch giữ history/case.
- Database/events: source rows/references/versions, immutable event/audit, financial/physical totals theo owner; before/after/rollback/replay, no foreign effects; projection không là proof source.
- Backend API: accepted request/response/status/error/headers/auth captures, direct-object denial; PG assertions/correlation chứng minh commit.
- Browser/frontend: portal đúng role từ đầu đến reload/persistence; loading/empty/error/unknown/conflict/revoked-session; direct URL/UUID deny. J9 thêm native airplane-mode/kill/restart, browser không thay device.
- CI/acceptance: TEST-V3-J4-FLOW/DENY/RECOVERY là IDs kế hoạch; source SHA/env/commands/exits/reports/skips/artifacts, cross-module regression và actual PR checks; Owner acceptance sau evidence, hiện NOT_VERIFIED.

## J5 — COD/Finance

Roles R09/R12/R13/R11/R28; specs L015/L017–019; milestone F2 collection,F3 closure.

- Prerequisite data: Chốt COD/snapshot, collection proof, Cashier shift, ledger/cutoff/approval.
- Happy: Collected→submitted→Cashier count/receipt→eligibility/reconcile→closed statement→simulated payout.
- Negative/security: Courier no self-confirm; Admin no money bypass; duplicate refund/payout/close deny.
- Recovery: Short/over payment giữ liability; dispute/late kỳ sau; adjustment mới, closed original immutable.
- Database/events: source rows/references/versions, immutable event/audit, financial/physical totals theo owner; before/after/rollback/replay, no foreign effects; projection không là proof source.
- Backend API: accepted request/response/status/error/headers/auth captures, direct-object denial; PG assertions/correlation chứng minh commit.
- Browser/frontend: portal đúng role từ đầu đến reload/persistence; loading/empty/error/unknown/conflict/revoked-session; direct URL/UUID deny. J9 thêm native airplane-mode/kill/restart, browser không thay device.
- CI/acceptance: TEST-V3-J5-FLOW/DENY/RECOVERY là IDs kế hoạch; source SHA/env/commands/exits/reports/skips/artifacts, cross-module regression và actual PR checks; Owner acceptance sau evidence, hiện NOT_VERIFIED.

## J6 — Claims

Roles R10/R18/R21/R14/R15/R13; specs L020/L021/L018/L019; milestone F3.

- Prerequisite data: Verified ticket/Shipment/POD/custody, claimant/evidence/approval limits.
- Happy: Recipient Merchant→Merchant Support→Logistics CS→claim/evidence→proposal→independent approval→Finance payment.
- Negative/security: No Recipient IAM; proposer không tự approve/pay; foreign evidence deny.
- Recovery: Sender dùng Logistics support trực tiếp; missing/tampered POD điều tra; payment timeout/status/no duplicate.
- Database/events: source rows/references/versions, immutable event/audit, financial/physical totals theo owner; before/after/rollback/replay, no foreign effects; projection không là proof source.
- Backend API: accepted request/response/status/error/headers/auth captures, direct-object denial; PG assertions/correlation chứng minh commit.
- Browser/frontend: portal đúng role từ đầu đến reload/persistence; loading/empty/error/unknown/conflict/revoked-session; direct URL/UUID deny. J9 thêm native airplane-mode/kill/restart, browser không thay device.
- CI/acceptance: TEST-V3-J6-FLOW/DENY/RECOVERY là IDs kế hoạch; source SHA/env/commands/exits/reports/skips/artifacts, cross-module regression và actual PR checks; Owner acceptance sau evidence, hiện NOT_VERIFIED.

## J7 — Integration/Partner

Roles R22/R33/R32/R02; specs L022–024; milestone F4.

- Prerequisite data: Scoped/rotatable machine credential, signed subscription, Partner point binding.
- Happy: API quote/create/label/tracking→signed webhook/retry; Partner counter/count/custody.
- Negative/security: Cross-tenant API/foreign Station deny; no shared human secret; IT không overwrite business.
- Recovery: Outage/reorder/replay→dedup/DLQ; revoke dừng access; count mismatch incident.
- Database/events: source rows/references/versions, immutable event/audit, financial/physical totals theo owner; before/after/rollback/replay, no foreign effects; projection không là proof source.
- Backend API: accepted request/response/status/error/headers/auth captures, direct-object denial; PG assertions/correlation chứng minh commit.
- Browser/frontend: portal đúng role từ đầu đến reload/persistence; loading/empty/error/unknown/conflict/revoked-session; direct URL/UUID deny. J9 thêm native airplane-mode/kill/restart, browser không thay device.
- CI/acceptance: TEST-V3-J7-FLOW/DENY/RECOVERY là IDs kế hoạch; source SHA/env/commands/exits/reports/skips/artifacts, cross-module regression và actual PR checks; Owner acceptance sau evidence, hiện NOT_VERIFIED.

## J8 — Fraud/Governance

Roles R30/R04/R05/R31/R32/R17; specs L026/L027; milestone F4,guards first-use.

- Prerequisite data: Risk case/scope, immutable audit, approved privilege/SoD/expiry.
- Happy: Evidence→hold proposal→approval/release→scoped audit; controlled IT retry; Auditor read.
- Negative/security: Business/Identity/SuperAdmin riêng; fake POD/COD deny; Auditor mutate/IT impersonate deny.
- Recovery: Revoked approver/expired break-glass deny; audit failure rollback effect.
- Database/events: source rows/references/versions, immutable event/audit, financial/physical totals theo owner; before/after/rollback/replay, no foreign effects; projection không là proof source.
- Backend API: accepted request/response/status/error/headers/auth captures, direct-object denial; PG assertions/correlation chứng minh commit.
- Browser/frontend: portal đúng role từ đầu đến reload/persistence; loading/empty/error/unknown/conflict/revoked-session; direct URL/UUID deny. J9 thêm native airplane-mode/kill/restart, browser không thay device.
- CI/acceptance: TEST-V3-J8-FLOW/DENY/RECOVERY là IDs kế hoạch; source SHA/env/commands/exits/reports/skips/artifacts, cross-module regression và actual PR checks; Owner acceptance sau evidence, hiện NOT_VERIFIED.

## J9 — Offline Courier

Roles R07/R09/R06; specs L010/L015/L017/L029; milestone F5,online earlier.

- Prerequisite data: Approved RN action allowlist/secure storage/current assignment/operation ID/media.
- Happy: Offline queue→restart→network→server revalidation→ACK/conflict visible.
- Negative/security: Revoked/stale assignment deny; duplicate sync/POD/COD/custody no double effect.
- Recovery: Missing file/storage full/session expiry/partial batch action required, no silent queue loss.
- Database/events: source rows/references/versions, immutable event/audit, financial/physical totals theo owner; before/after/rollback/replay, no foreign effects; projection không là proof source.
- Backend API: accepted request/response/status/error/headers/auth captures, direct-object denial; PG assertions/correlation chứng minh commit.
- Browser/frontend: portal đúng role từ đầu đến reload/persistence; loading/empty/error/unknown/conflict/revoked-session; direct URL/UUID deny. J9 thêm native airplane-mode/kill/restart, browser không thay device.
- CI/acceptance: TEST-V3-J9-FLOW/DENY/RECOVERY là IDs kế hoạch; source SHA/env/commands/exits/reports/skips/artifacts, cross-module regression và actual PR checks; Owner acceptance sau evidence, hiện NOT_VERIFIED.
