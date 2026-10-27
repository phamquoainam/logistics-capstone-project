# Group A/B portal — information architecture và authority

CANONICAL_TARGET_DESIGN / NOT_IMPLEMENTED. Screen IDs dưới đây là conceptual design IDs, không official URL/API/permission code. [112-BC coverage](group-ab-coverage.csv), [supplemental requirements](supplemental-requirements.csv), [acceptance](group-ab-acceptance.md).

Một human account, personal default; Merchant workspace là business context. Machine principal không human login; Recipient không IAM/Portal bắt buộc. Menu visibility dựa current capability + delivery availability; server check độc lập, không hidden-button authorization.

| Screen family / planned entries | Actor/context | BC hoặc supplemental requirement | Owning specs / required behavior API | Status |
|---|---|---|---|---|
| PUB landing, track, quote, coverage, Station, catalog, contact | Guest | BC001–005/131/132; REQ-AB-ENTRY | 003/004/005/009/020; public redacted query/contact, no Shipment create | PLANNED |
| ID register/verify/login/recovery/security | Human customer; privileged internal policy separate | BC132/134/135; REQ-AB-IDENTITY-UX | 002 authoritative verification/session;003 orchestration | BLOCKED IAM amendment |
| PER dashboard/search/notifications/profile/application/status | Personal | BC133–135/143/147; REQ-AB-PERSONAL-DASH/NOTIFICATIONS/CONTEXT | 003/007/009/028; owned queries and application commands | PLANNED |
| PER create/my shipments/detail/pickup/address/payment/receipt/return/support/claim | Personal | BC136–148; REQ-AB-BOOKING-FORM/CLAIMS | 004–010/016/018/020/021; real supported service workflow | PLANNED |
| MER dashboard/search/notifications/reports | Current org, delegated scope | BC017/165/172/192; REQ-AB-PORTAL-SHELL | 028 scoped projections, definitions/as-of/lag | PLANNED |
| MER create/import/list/detail/cancel/changes | Owner/Manager/Operator per permission; Support scoped read | BC011/012/024/025/029/160/169/174/193 | 007/023; row preview/commit no auto charge; mutation state guards | PLANNED |
| MER parcels/labels/pick-pack/handover | Operator/Warehouse; Owner permitted view/actions | BC026/176/179–186 | 008/010/011; label identity and custody separated | PLANNED |
| MER addresses/pickup/facilities/staff/settings | Owner; Manager/Operator delegated subsets | BC008–010/021/157–159/166–168 | 003 facilities/staff;004 sole pickup/address commands | PLANNED |
| MER exceptions/returns/support/claims | Owner/Manager/Operator/Support scoped | BC016/030/155/156/163/171/173/177/178/195–199 | 014/016/020/021; requests not carrier state mutation | PLANNED |
| MER COD/statements/invoices/contracts | Owner/Finance; Operator read only when authorized | BC014/015/019/028/086–091/161/162/187–190 | 005/017/018/019; no Courier/ledger authority from finance view | PLANNED |
| MER integrations/webhooks | Explicit integration administrator; API machine separate | BC018/164/175/200–206 | 022 machine credential lifecycle; Sales335 no secrets | PLANNED |
| REC limited interaction channels | Verified scoped external recipient | BC080–084, deny085; REQ-AB-RECIPIENT | 009/015/017/020; request only, no COD/final facts direct edits | PLANNED |

## UX và consistency gates

Shared original E-Logistics visual system: spacing/type/colors/layout tokens, sidebar/topbar/breadcrumb, clear Vietnamese labels, responsive desktop/tablet/mobile, keyboard/focus/labels/error association/text statuses/contrast. Forms have field-level validation, sensitive confirmation và unsaved-change protection. Search/filter/sort/paging and timezone/currency/units consistent. Chart/KPI require source definition/as-of/lag, no fake counters. No unsupported service button or interactive dead end.

Context switch: query accessible orgs from current authority, show active org, invalidate scoped cache/forms/notifications, cancel/ignore stale in-flight data using context generation, prevent old-context data flash. Backend rechecks resource ownership on every operation. Membership/org revoked → safe message, clear business context, personal workspace if identity still valid. Logout clears sensitive client state; no refresh token/secrets in localStorage/URL/log.

Every actionable menu traces Role→BC/REQ-AB→spec→screen family→required behavior API→data owner→current permission→test→delivery status. Exact path/code still DECISION_REQUIRED; matrix cell is planning, not route/API implementation. Native camera/GPS/SQLite belongs Courier specs, not evidence from responsive web.
