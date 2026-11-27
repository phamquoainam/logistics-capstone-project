# Menu 2 — consolidated technical review / Owner decisions

2026-11-25 · **MENU_2_CONTRACT_REVIEW_PREPARED — NOT IMPLEMENTATION APPROVAL**.
Menu 2 vẫn PARTIAL/BLOCKED_CONTRACT và có integration dependencies; chưa FULL READY.

## Gói review chính xác

- Contract: `admin-menu2-completion-contract.proposed.md`.
- Manifest: `admin-menu2-completion-technical-manifest.proposed.json`.
- SHA-256 manifest: `6d072d9b649ab144c2fb9b8b780c8f077757387bcef58536a31618f9154251a2`.
- Inventory: **33 API mới + 6 existing API amendments, 58 DTO/public models,
  13 capability, 13 public ports, 12 bảng, 7 SQL signatures, 10 amendments**.
- Mọi mục mới là **[PROPOSED]**. Đây là inventory mục tiêu qua nhiều dependency
  gates, không phải đề nghị một migration khổng lồ hay blanket privilege approval.
- Parent P0 SHA giữ nguyên:
  `90c795bd7e773b6a58d0d220056cfbc5e9a5ee95b38ae2493d5b9f75a372291f`.

## 1. Những phần đã đủ authority và giữ nguyên

Merchant Approval/Organization Activation V41–V43, existing scoped reads,
assignment/RFI/checklist/independent decisions, receipt/audit/committed notice
binding, ACTIVE↔SUSPENDED và Menu2 UI completion không cần xin duyệt lại.
Latest source/evidence review không phát hiện thêm gap có thể sửa ngay trong
những API/DTO/authority hiện hữu mà không mở contract mới. Vì vậy checkpoint này
chỉ thay tài liệu, không triển khai API/schema/permission hoặc chạy lại suite.

Menu12 report giữ lịch sử TDD/locator failure và PASS; không gán CI cũ cho WIP mới.
NWS-V1 và 44 migration files V1–V45 (không V33), staged Owner files và mọi WIP được
fingerprint trước tác vụ. Không thay application source, migrations hay shared
Menu6–9/IAM/Notifications. Không truy vấn hoặc mutate database demo/UAT/Production.

## 2. Source findings và controlled deltas

| Finding có evidence source | Tác động / giải pháp tối thiểu |
|---|---|
| `MerchantGovernance.ApplicationQuery` chỉ có ngày/state/reviewer/policy/page/size | Advanced search là contract mới. M2R-API-01/02 là POST purpose-bound; giữ GET cũ |
| `application_revision.submitted_input` có dữ liệu revision immutable và thuộc Merchant | Có thể so sánh/tìm trực tiếp ở owning module; không cần duplicate search source hoặc private cross-domain query |
| ReviewData hiện chỉ cho current revision; revisions DTO chỉ metadata | Comparison phải audited disclosure hai revisions cụ thể; không mở lịch sử bằng trả raw table |
| V43 notice có attempts/accepted_at nhưng không có delivered timestamp/provider delivery receipt | Reader tái sử dụng actual state; acceptedByProvider ≠ delivered, không invent field |
| `OrganizationSummary` chỉ bounded summary; membership counts đã có | Expanded profile và member metadata cần exact DTO/grants; không dùng Customer Team API dưới danh Admin |
| V23 organization CHECK chỉ ACTIVE/SUSPENDED, V43 guard chỉ suspend/reinstate | M2-L01 là business-state/guard amendment, không phải UI enum patch |
| V23 `check_workspace_invariants`, V34 `team_permission`, V43 `application_origin_admitted`, Java workspace/Team/organization readers dựa Initial Owner | Current Owner bridge + mọi writer/offline path phải coordinated; thay một pointer/role sẽ phá admission hoặc để lại quyền cũ |
| V41 reauth operation allowlist chỉ APPROVE/REJECT/SUSPEND/REINSTATE | `ORG_CASE_DECIDE/EXECUTE` cần Identity amendment + changed existing HTTP enum; không giả operation khác |
| `CustomerOwnerRecoveryConsents` chỉ org-bound; proof engine có real REAUTHENTICATION | Cần exact case/revision/party/intent binding. Không dùng recovery consent như một transfer approval |
| `identity.lock_local_business_subject` schema-owner/offline-only | Không dùng nó làm runtime eligibility workaround hoặc đổi current_user guard |
| Finance `AdminObligationFacts` không cấp closure clearance; Orders/Support/Claims có nguồn riêng | Cần co-signed read/serialization ports; nguồn chưa đủ = UNAVAILABLE, CLOSE vẫn bị chặn |

Primary source files: `merchant/api/MerchantGovernance.java`,
`merchant/internal/infrastructure/JdbcMerchantApplicationReviews.java`,
`JdbcMerchantOrganizationGovernance.java`, `JdbcBusinessWorkspaces.java`,
`JdbcOrganizationTeam.java`; `identity/api/admin/AdminCommandAdmission.java`,
`AdminGovernanceReauthentication.java`, `identity/api/customer/CustomerAdmission.java`,
`CustomerReauthentication.java`, `CustomerOwnerRecoveryConsents.java`;
`notifications/api/MerchantApplicationNotices.java`,
`platform/api/MerchantNoticeMail.java`; migrations V3/V8/V12/V23/V34/V41/V42/V43.
Paths trong mục này relative to `app/backend/src/main/java/com/elogistic/backend`
trừ migrations thuộc `app/backend/src/main/resources/db/migration`.

## 3. Owner quyết định nghiệp vụ/authority — một consolidated checkpoint

Các phương án dưới đây là **đề xuất để chọn**, chưa có hiệu lực. Owner có thể duyệt
M2-R01 trước mà không mặc nhiên duyệt lifecycle/Owner Transfer.

| Decision ID | Nội dung cần chốt | Candidate đề xuất, chưa approved |
|---|---|---|
| DEC-R1 | Read scope, matching semantics, disclosure/grants và review timing | Exact normalized name/tax/ID; search trước paging; purpose/audit; bốn R capabilities cấp riêng. Chọn deadline start event và số giây nếu mở SLA filter; thiếu policy = UNAVAILABLE. Không email recipient/Finance amounts |
| DEC-L1 | RESTRICTED/CLOSED còn quyền gì; được reopen không | RESTRICTED chặn Quote/Booking/delegation/contract change; giữ scoped history/support. CLOSED terminal, chỉ retained read/support; không tự reopen |
| DEC-L2 | Maker/reviewer/decider/executor cho case mới | Ba người maker/reviewer/decider khác nhau, không thuộc Org đích; executor có thể là decider chỉ khi có execute grant + proof riêng. Không đổi hai-person Merchant application |
| DEC-L3 | Điều kiện CLOSE và tác động risk holds | Chỉ CLEAR thật từ Orders/Finance; UNKNOWN chặn. Hai hold codes BOOKING_HOLD/CONTRACT_CHANGE_HOLD; không sửa Shipment/custody/ledger |
| DEC-L4 | Case/consent TTL, due/effective limits, reverification evidence và retention | Owner chọn giá trị/policy reference cụ thể; không seed mặc định, không copy TTL invitation. Legal registry mutation ngoài scope |
| DEC-O1 | Transferor/transferee và consent | Current verified Owner có explicit request grant; transferee đã ACTIVE verified cùng Org; cả hai consent bằng proof thật cùng case/revision/digest |
| DEC-O2 | Historical Initial Owner vs current Owner | Giữ initial pointer/activation; thêm current-owner và history, exactly one current ACTIVE Owner; controlled Team/admission amendments |
| DEC-O3 | Role/grants/sessions/invitations sau transfer | Outgoing Org Membership SUSPENDED, grants và pending invitations bị thu hồi trong transaction; incoming minimal dashboard/request grant, không kế thừa Finance/Team write; Personal/Org khác không ảnh hưởng |
| DEC-O4 | Open obligations/dispute/legacy origins | ACTIVE approved-origin và không open ownership dispute; UNPAID giữ ở Org, không tự PAID/chuyển nợ; legacy operator origin cần authoritative onboarding trước |

DEC-L4 chưa có các con số thật thì policy không thể PUBLISHED/ACTIVE. Việc duyệt
schema/API không mặc nhiên duyệt các số, quyền hoặc exception thương mại còn trống.
Không có field nào trong manifest tự chọn nhân viên/Account/Organization để cấp grant.

## 4. Coordination cần thiết, không thuộc quyền tự sửa của nhóm Menu2

| ID / owner | Deliverable cần phối hợp | Có thể tiếp tục độc lập |
|---|---|---|
| COORD-NOTICE — Notifications/SMTP | R redacted notice reader; L/O target-kind/recipient/template/unique/guard + same-worker integration; exact signatures trong manifest | Merchant-only R search/diff/comments/membership metadata |
| COORD-PROFILE — Pricing/Platform | Contract-reference read port và canonical evidence binding; không thêm provider | Profile legal/facility hiện do Merchant sở hữu; thiếu contract source ghi UNAVAILABLE |
| COORD-ADMISSION — Merchant consumers/Team | Operation-aware RESTRICTED/CLOSED/hold gate; v1/v2 compatibility; coordinated lock order | R không đổi state |
| COORD-EXIT — Orders/Finance | CLEAR/BLOCKED/UNAVAILABLE assertions, current versions, serialization với Booking/obligation mutations | Case review có thể xây sau approval; CLOSE execution thiếu source giữ BLOCKED |
| COORD-IAM — Identity/Menu6–9 | Proof operation enum, case intent binding/guard, genuine context/current Customer eligibility | Không đổi authentication/session engine; R không cần port Identity mới |
| COORD-TEAM — Team/Menu6–9 | Current-owner references, exact role/grant/invite receipt guards, offline operator paths | Không rewrite Team implementation hoặc cấp role templates |

Không gửi message cho nhóm khác hoặc tự merge branch của họ trong checkpoint này.
Không giả định một interface chưa merge đã tồn tại. Coordination phải có acknowledgment
và exact source/manifest revision trước khi code phần liên quan.

## 5. Triển khai tối ưu theo dependency sau approval

**R trước:** M2R-API-01/02/03/05/07/08/09 + legal/facility core của 04; một comment
table, bốn explicit capabilities, các projection còn lại không tạo storage mới.
Giữ source chưa tích hợp UNAVAILABLE, không tính projection thiếu vào full acceptance.
M2R-API-06 và contract block 04 hoàn thành sau COORD-NOTICE/PROFILE.

**L tiếp:** Sau DEC-L1..L4, thiết kế policy/case/review/event/receipt/holds và
current-owner read bridge, operation admission + clearance integration. Thứ tự
candidate V46(R)→V47(L)→V48(O); số chưa reserve, phải kiểm tra branch nhóm khác.
Identity/Notifications forward migrations do owner phối hợp chốt số, không ẩn vào
một migration Merchant. V1–V45 và approved artifact bytes giữ nguyên.

**O sau bridge/coordination:** Genuine two-party consent, case-bound proof,
independent review, approved atomic owner/member/grant/history/notice execution.
Không mở before guard/last-owner/replay/direct-DML gates. No real grants/deploy.

**Integration cuối:** consume Work Inbox/Audit/Notifications interfaces đã chốt;
một HTTPS+PG18 scenario thực, dữ liệu tổng hợp, không seed fake success. Map M2-T-01..08
theo thay đổi; regression Team/Business/NWS chỉ khi dependencies bị tác động. Không
chạy lại full Backend suite hoặc thêm nhiều dataset dư thừa.

## 6. Verification ở checkpoint tài liệu này

Chỉ kiểm JSON/ID/routes/DTO/grant references, schema/security inventory, coverage
mapping, ownership validator, diff và preservation fingerprints. Đây **không** là
API/DB/security behavior PASS. Tất cả M2-T-01..08 là **NOT RUN — pending approval**.
Không chạy TypeScript/build/Browser/PostgreSQL vì application source/dependencies
không đổi. Evidence đã PASS của Menu12/P0/NWS được giữ nguyên, không chạy lặp lại.

Kết quả cụ thể của document gates và preservation được ghi sau validation bên dưới.
Definition of Ready của L/O còn BLOCKED ở business policy và coordination; không
đề nghị đánh dấu READY_FOR_IMPLEMENTATION cho toàn gói khi các điều kiện này chưa đạt.

### Kết quả thực thi document gates

- `python .demo/validate-menu2-contracts.py`: **13/13 PASS**. JSON parse, stable
  IDs, unique method/path, API→DTO/grant references, requirement links, proposed
  status, source evidence hashes và review checksum/counts khớp.
- `python docs/reconciliation/v3/validate.py --implementation`: **PASS**, exit0.
- `git diff --check`: **PASS**, exit0, chỉ unstaged delta; staged Owner files giữ
  nguyên, không tự format các hard breaks đã ghi nhận ở checkpoint trước.
- `.demo/admin-menu2-contract-validation.json`: source/migration/WIP preservation
  PASS; chỉ bốn tài liệu đã có thay đổi có chủ đích (CSV/readiness/brief/traceability).
  Ba artifact completion mới được thêm; không application/backend/frontend/DB delta.
- HEAD vẫn `d5a0c240841bdb35d9ef6064cddba696a5b2fa62`; 44 migration files và approved
  parent P0 manifest unchanged. Không tạo SQL migration candidate file, không
  kết nối/mutate DB, không thêm grant/deploy/commit/push/merge.

Đây là **document checks PASS**, không phải backend security/integration PASS.
Checkpoint: **MENU_2_CONTRACT_REVIEW_PREPARED — OWNER POLICY / TECHNICAL APPROVAL
AND COORDINATION REQUIRED**. Không chuyển Menu3 và không đánh dấu Menu2 FULL READY.
