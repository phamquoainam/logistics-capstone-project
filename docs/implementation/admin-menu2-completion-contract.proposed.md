# Menu 2 — ADM-02/03 completion contract [PROPOSED]

Ngày 2026-11-25. **Thiết kế để review; chưa cho phép triển khai authority mới.**
Nhiệm vụ hiện hành chỉ hoàn thiện Menu 2, **không chuyển Menu 3**. Tài liệu này
không thay approved P0 manifest, không cấp quyền từ Full Specification hoặc từ
việc Owner yêu cầu chuẩn bị contract.

Manifest kỹ thuật: `admin-menu2-completion-technical-manifest.proposed.json`.
Review/quyết định: `admin-menu2-completion-technical-review.md`.
Mọi API/DTO/schema/permission/SQL function mới trong manifest đều **[PROPOSED]**.

## 1. Baseline và source ownership

Đã đọc AGENTS, toàn bộ Project North Star, coverage CSV, Menu12 readiness,
remaining-contracts proposal và Definition of Ready. Branch `develop`; không
switch/reset/stash WIP. Baseline source/test hiện có được giữ nguyên.

| Source thực tế | Điều được tái sử dụng | Không suy diễn |
|---|---|---|
| `MerchantGovernance`, `JdbcMerchantApplicationReviews`, V42/V43 | Submission/revision, review/assignment/checklist/RFI, độc lập reviewer–decider, receipt/audit, activation nguyên tử | Không xây Merchant workflow thứ hai |
| `JdbcMerchantOrganizationGovernance` | Scoped directory, activation provenance, membership counts, ACTIVE↔SUSPENDED với step-up | Không có Owner Transfer/RESTRICTED/CLOSED |
| `AdminAdmission` / `AdminCommandAdmission` | Genuine Identity-owned context, physical transaction/lifetime, current scope và referenced subjects | Không constructed context, không ghép SYSTEM/Customer/STAFF |
| `CustomerReauthentication` / `CustomerAdmission` | Xác thực lại thật, verified Customer và version hiện hành | OwnerRecoveryConsent hiện hữu không phải consent cho một transfer case cụ thể |
| V23/V34, `JdbcBusinessWorkspaces` / `JdbcOrganizationTeam` | Identity Membership, cấp quyền theo scope, protection và invitation receipts | Initial Owner hiện là admission anchor; không đơn giản đổi role/pointer |
| V43 `notification.merchant_application_notice` | Bốn template, trạng thái/attempt/accepted_at thực, unique event/template, worker/SMTP hiện hữu | Không có DELIVERED receipt; không dùng application ID giả cho case Organization |
| `MerchantFacilities` / `BusinessQuotes` | Facility canonical; hợp đồng Merchant có nguồn Pricing thật | API Customer không phải Admin admission; không gọi giả Customer để lấy profile |
| `AdminObligationFacts` | Cước/UNPAID đã xác nhận | Không đủ để cấp clearance đóng Organization, không phải ledger/COD/settlement |

Domain `merchant` sở hữu application, Organization, ownership, restrictions và
bounded Organization governance cases. `workflow` phối hợp qua public ports trong
cùng local transaction. Identity sở hữu authentication/proofs/eligibility;
Notifications sở hữu outbox/delivery; Orders sở hữu Shipment/custody/Support/Claim;
Finance sở hữu nghĩa vụ và clearance tài chính. Không import private implementation
hoặc SELECT chéo private table từ consumer.

## 2. Đối chiếu Full Specification: bắt buộc và tùy chọn

| ID | Năng lực | Loại / hiện trạng | Phương án |
|---|---|---|---|
| M2-REQ-01 | Inbox chính thức, filter ngày/trạng thái/reviewer/scope, mở mã hồ sơ | Bắt buộc, VERIFIED | Giữ API/UI/evidence hiện có |
| M2-REQ-02 | Tìm applicant/legal name/tax/Organization/Owner, global pagination | Bắt buộc, BLOCKED_CONTRACT | M2R-API-01/02; match toàn cohort được cấp trước paging |
| M2-REQ-03 | Dữ liệu pháp lý/contact/consent/documents và kiểm tra trùng/completeness | Bắt buộc, phần core VERIFIED | M2R-API-09 chỉ chiếu kết quả guard thật; không fake fraud/expiry/provider result |
| M2-REQ-04 | Diff revision đã nộp, bình luận nội bộ | Bắt buộc, BLOCKED_CONTRACT | M2R-API-03/07/08; không dùng draft làm lịch sử |
| M2-REQ-05 | Assignment/RFI/resubmit/review/approve/reject, maker-checker và activation | Bắt buộc, VERIFIED | Không triển khai lại V41–V43; policy hai người hiện hành không đổi |
| M2-REQ-06 | Theo dõi kết quả notice tách approval | Bắt buộc, engine có / Admin reader thiếu | M2R-API-06 + Notifications-owned reader cần coordination |
| M2-REQ-07 | Directory/profile/legal/contract/service/warehouse references | Bắt buộc, summary có / expanded thiếu | M2R-API-04; từng block ghi source/permission/availability riêng |
| M2-REQ-08 | Membership/invitation oversight | Bắt buộc, counts có / metadata thiếu | M2R-API-05; chỉ UUID/role/state/versions, không sửa Team hoặc lộ email invitation |
| M2-REQ-09 | ACTIVE↔SUSPENDED, bảo toàn lịch sử | Bắt buộc, VERIFIED | Giữ nguyên command/receipt/proof đã PASS |
| M2-REQ-10 | RESTRICTED/CLOSED, restriction/risk holds/effectivity | Bắt buộc, BLOCKED_CONTRACT | M2-L01, policy DEC-L1..L4, current-domain locks/clearances |
| M2-REQ-11 | Ownership dispute và reverification | Bắt buộc, BLOCKED_CONTRACT | Case có authority/evidence riêng; không biến Support Ticket thành quyết định pháp lý |
| M2-REQ-12 | Owner Transfer, hai chủ thể verified, last Owner, atomicity | Bắt buộc, BLOCKED_CONTRACT | M2-O01 + DEC-O1..O4 + coordinated IAM/Team amendment |
| M2-REQ-13 | Audit/notification/Work Inbox integration | Bắt buộc, core VERIFIED / case integration phụ thuộc | Consume approved interfaces; shared implementation do nhóm khác sở hữu |
| M2-REQ-14 | Review SLA/due/risk filters | Bắt buộc khi policy áp dụng; BLOCKED_CONTRACT / INTEGRATION_DEPENDENT | M2R-API-10 + explicit review-timing policy; chỉ actual guard facts, không giả deadline/fraud flag |
| M2-OPT-01 | Fuzzy search, saved views, bulk export nâng cao | Tùy chọn, PROPOSED | Không thêm vào execution bắt buộc của gói này |
| M2-OPT-02 | Fraud scoring, external registry tự động, SLA/risk engine | Optional automation / dependency | Chỉ hiển thị UNAVAILABLE khi thiếu canonical source; không giả engine |
| M2-OPT-03 | Owner recovery thiếu consent, transferee ngoài Organization, reopen CLOSED | Mở rộng chưa duyệt | Không triển khai trong candidate policy này |

Membership oversight là **bắt buộc** theo CSV; từ “optional” trong brief cũ không
loại yêu cầu này. Một block có nguồn phụ thuộc chỉ được ghi INTEGRATION_DEPENDENT,
không được tính thành FULL READY nếu chức năng bắt buộc chưa nghiệm thu.

## 3. M2-R01 — read projections và review bổ sung

### API và admission

M2R-API-01..10 trong JSON là danh sách exact method/path/input/output/grants.
Không sửa shape GET cũ. Search pháp lý dùng POST body để tránh PII trong URL,
chỉ NFC/canonical exact-match theo semantics đã ghi, không “tìm toàn bộ” bằng lọc
một trang đang hiển thị. Legal name/tax chỉ match sau current Merchant scope và
quyền disclosure; không cho READ Dashboard dò hồ sơ bằng điều kiện bí mật.

Merchant có thể query `application_revision.submitted_input` trong module của
mình: không cần tạo search source/index table thứ hai. Chỉ revision đã submit
hiện hành; Organization pháp lý lấy activation application/revision và canonical
registry, không lấy nội dung draft mới hơn. Legacy operator Organization không có
legal provenance thì block pháp lý UNAVAILABLE. Dùng parameters SQL, escape
wildcards nếu sau này duyệt match khác; không dynamic SQL từ tên field client.

Phân trang mới dùng opaque HMAC cursor ràng actor/realm/current scope/filter/source
fingerprint/expiry. Source hoặc authority thay đổi phải báo hết snapshot, không
âm thầm đổi cohort/đếm trùng. Không lưu raw search body vào log/receipt/cursor.
Quy mô trả tối đa 50, mặc định 20; query thời gian tối đa 366 ngày.

Comparison là audited disclosure của đúng hai immutable revisions thuộc một
application: trường typed allowlist, evidence added/removed, contact verified fact
và digest của cả hai. Không expose proof/token/private document path. Reviewer
hiện hành không được xem hồ sơ của policy khác hoặc biến diff thành legal mutation.

Comment là append-only có encrypted text, exact application/revision, actor,
timestamp, operation key/HMAC. Row comment đồng thời giữ durable append receipt;
same-key/same-input replay trả row cũ sau current authorization, khác input reject.
Không tăng application business version hay làm mất READY chỉ vì một comment.
Audit dùng READ_DISCLOSURE với protected purpose COMMENT đã ràng row, không chế
thêm state machine application hoặc quyền sửa/xóa comment.

Profile gồm legal/contact/facility/contract references theo từng availability.
Không có contract prices/Finance amount trong `org.profile.read`. Dữ liệu contact
nhạy cảm chỉ disclose khi có thêm application evidence scope của hồ sơ nguồn.
Membership reader dùng Merchant-owned metadata, không cần thêm Identity account
directory hoặc sửa `CustomerMemberNames` của nhóm khác.

Notice reader do Notifications sở hữu: QUEUED/SENDING/RETRY_WAIT/
ACCEPTED_BY_PROVIDER/FAILED/BLOCKED; mapping accepted_at thành
acceptedByProviderAt. `deliveryConfirmed=null`, không tạo DELIVERED giả, không
expose destination/provider error body. Callback boolean do caller tự dựng không
được xem là authority; Identity và Merchant owner phải xác minh lại context/scope.

### Quyền và schema R

Reuse `application.read/evidence.read`, `org.read`, `audit.read` theo operation.
Bốn capability mới: `application.comment.write`, `application.notice.read`,
`org.profile.read`, `org.members.read`; cấp riêng bằng independent operator sau
approval, không tự theo role. Chỉ một bảng R mới: encrypted append-only comment.
R không cần thay authentication, session, Shipment/Finance hoặc trạng thái Org.

Review timing là M2R-API-10/M2-AMEND-10: nullable `application_policy.review_timing`
chỉ có start_event FIRST_SUBMISSION hoặc CURRENT_REVISION_SUBMISSION, duration_seconds
do Owner chọn và calendar_mode ELAPSED_SECONDS. Published version/digest cũ không
đổi, không backfill deadline. Policy thiếu thì dueAt/isOverdue là null và SLA
filter không được trả kết quả giả. Check filters chỉ dùng actual duplicate/contact/
evidence/reviewer-conflict facts. Shared Work Inbox chỉ nhận dueAt qua reader được
nhóm ADM-17 phối hợp, không sửa implementation của nhóm đó trong lượt này.

## 4. M2-L01 — lifecycle/restrictions/dispute/reverification

### State và policy là đề xuất, không mặc định có hiệu lực

Current Organization chỉ ACTIVE/SUSPENDED. Candidate RESTRICTED/CLOSED cần Owner
chọn DEC-L1..L4. Giữ hai chiều ACTIVE↔SUSPENDED cũ và lịch sử đã PASS. Candidate
chuyển trạng thái mới: ACTIVE→RESTRICTED; RESTRICTED→ACTIVE sau release case hợp lệ;
ACTIVE/RESTRICTED/SUSPENDED→CLOSED khi đủ clearance. CLOSED terminal theo candidate,
không tự reopen. SUSPENDED→RESTRICTED hoặc quay ACTIVE phải đi qua điều kiện
reinstatement và current-owner eligibility, không “release hold” để vượt suspend.

Restriction/hold chỉ chặn capability cụ thể được policy duyệt, không sửa Booking
snapshot, Shipment/custody, Finance, Support hoặc event lịch sử. Candidate hold
chỉ BOOKING_HOLD và CONTRACT_CHANGE_HOLD, không arbitrary string permissions.
Expiry được đọc theo Backend clock, không cần fake scheduler event. Release là
một case độc lập có decision/evidence, không xóa row hold cũ.

Case domain trong Merchant chỉ dành bảy loại lifecycle/dispute/reverification và
Owner Transfer. Đây không phải generic task/approval engine của Menu 9/18.
Case state machine và exact event/reason enums nằm trong JSON. Material resubmission
tăng revision, invalidates readiness/impact snapshot/consents, bắt buộc review lại.
Đổi assignment có reason/version; không xóa assignment/audit lịch sử.

GET không mutate case khi quá hạn: `isExpired/asOf` là metadata tính từ expiresAt
thật và Backend clock, execution deny ngay. Một mutating request cùng Org đã được
authorize có thể terminalize case hết hạn dưới lock trước tạo case mới, với event/
receipt EXPIRE_CASE và child key deterministic của request gốc. Không giả actor
SYSTEM, không tạo background job engine, không để unique open-case giữ chỗ vô hạn.

Maker/reviewer/decider/executor và scope/type/expiry được kiểm tra khi đọc, quyết
định và execute, kể cả khi replay receipt. Candidate tách ba người cho case mới
chờ DEC-L2; không thay policy hai người của Merchant application V42. Approval
không tự execute hoặc cấp grant. Sensitive decision/execution cần fresh purpose-
bound proof, không dùng Root/IAM/Staff như governance permission.

### Clearance và concurrency

Đóng Organization không được suy UNPAID=0 từ Finance reader không có nguồn.
Orders/Finance cung cấp clearance read port độc lập: CLEAR/BLOCKED/UNAVAILABLE,
fixed reason codes và version, không PII hoặc tiền nếu caller không có Finance
READ. Missing lifecycle/reconciliation source là UNAVAILABLE và chặn CLOSE,
trừ một khả năng đã xác định rõ không áp dụng trong service policy như NO_COD.

Thứ tự chung: Identity administration/current subject locks → Merchant Org gate
→ current-owner/version → case/review → Orders work gate → Finance obligation gate
→ evidence/consent/receipt/event, trước commit revalidate. Thứ tự cụ thể phải được
các owner cùng chốt để tránh deadlock; tất cả Booking/Team/contract mutations liên
quan dùng cùng Org serialization gate. Không dùng Redis/front-end check để thay DB.

Preflight với nguồn đọc không lock không đủ cho execution: concurrent Booking
hoặc obligation mới sau “CLEAR” phải bị ngăn hoặc làm stamp stale dưới cùng gate.
Không dùng token client tự gửi làm clearance thực. Guard deferred kiểm tra toàn
bộ case/decision/event/receipt/state/history cùng transaction; rollback tất cả.

Dispute không phải phán quyết pháp lý/bồi thường. Reverification nhận evidence
và phản hồi đúng named party, dùng scanner/store đã có. Chưa duyệt cơ chế đổi tax/
legal name thì kết quả reverification không được ghi đè business-key registry hay
contract snapshot; legal mutation là một delta riêng. Retention/legal-hold và
thời hạn phản hồi chưa có policy không được tự đặt số ngày.

### Compatibility

Old OrganizationSummary/Business context parser không hỗ trợ state mới. Dùng
opt-in state reader/DTO v2 và nâng operation-aware admission dưới coordination;
không đổi RESTRICTED/CLOSED thành ACTIVE cho client cũ. Full acceptance yêu cầu
consumer chính đọc/làm đúng history/support theo policy; fail-closed parser đơn
thuần chưa đủ UX/functional acceptance. No Production activation.

## 5. M2-O01 — Owner Transfer

### Hai bên và scope

Candidate DEC-O1 chỉ chuyển cho ACTIVE verified non-Owner Membership cùng Org;
không mời người ngoài hoặc tra email Customer toàn hệ thống. Current Owner phải
có thêm explicit ORGANIZATION grant `ownership.transfer.request`; Owner role
không tự cấp grant này. Mọi Admin thao tác cần `org.ownership.*` độc lập với
`org.lifecycle.*`, không dùng OR-union giữa hai bộ quyền.

Hai bên tự consent bằng Customer session hiện hành và reauthentication thật với
cùng case/revision/intentDigest. Admin không nhập hộ mật khẩu/proof, không đổi
Customer thành SYSTEM. Identity-owned binding/receipt giữ proof/security-version
thật; source proof TTL hoặc natural session expiry sau consent không tự xóa một
historical fact. Binding TTL, security-version change, explicit revoke/eligibility
hoặc material revision vẫn chặn use tại decision/execution. Giá trị TTL cần Owner
chọn trong published policy, không copy 72 giờ invitation Team sang consent.

Existing OwnerRecoveryConsent chỉ bind Org, chưa bind transferee/case/revision/
intent, do đó không đủ và không được âm thầm tái sử dụng. New port/binding/predicate
trong manifest là amendment **cần Identity owner phối hợp trước code**.

### Historical Initial Owner và current ownership

Candidate DEC-O2 giữ nguyên `initial_owner_membership_id` và `application_activation`
như provenance; thêm `organization_current_owner` và append-only ownership history.
Không đổi account/org/member UUID. Exactly one current ACTIVE Owner cho mỗi Org;
forward backfill chỉ từ Initial Owner hợp lệ, gặp inconsistent row phải dừng,
không tự chọn account thay thế. Đây là data migration design, chưa được phép chạy.

Guard và mọi offline/Team/admission path dùng “Initial Owner hiện hành” phải được
đổi có kiểm soát sang current-owner predicate. Không chỉ sửa một React form hay
CHECK role. Không để offline configure_local_workspace tiếp tục cấp quyền theo
Owner cũ sau transfer. Original Owner chỉ được mất vai trò current qua exact
EXECUTE_TRANSFER transaction đã duyệt, không qua Team role edit/suspend tùy ý.

### Transaction execution

Recheck scope/eligibility/proof/consents/current owner/case revision/decision/
policy/impact và Organization version dưới locks. Update case EXECUTED, current-
owner pointer/version, membership role/state/authority versions, grant delta,
pending invitations và immutable audit/receipt/history trong cùng transaction.
Notifications enqueue được ràng event/receipt/recipients cùng transaction;
SMTP dispatch sau commit. Không dùng approved record stale để mutate.

Candidate DEC-O3: outgoing Membership SUSPENDED và gỡ grants của đúng Org;
recipient giữ ID, lên OWNER với tối thiểu dashboard.read và explicit request grant;
không kế thừa Finance/Carrier/Team administration. Cần Owner chốt chính sách này.
Customer authentication không đổi: Membership authority version/state khiến mọi
Business request/session snapshot cũ bị deny; Personal và Org khác vẫn hợp lệ.
Không gọi logout-all hay tăng global auth_version chỉ để thu hồi Business scope.

Replay cùng actor/org/operation/key/input phải trả kết quả cũ, không execute lại,
kể cả proof đã consumed và If-Match cũ. Current admission luôn kiểm tra trước
receipt; payload khác hoặc revoke không cho khôi phục grant. Two concurrent cases
không tạo hai current Owners/history cùng version. Partial failure rollback toàn
bộ actor/grant/owner/audit/notice delta, không fake successful transfer.

## 6. Schema/ACL/migration boundary

JSON liệt kê exact API/DTO, 13 proposed capabilities, 12 proposed tables, 13 public
ports, 7 SQL signatures và 10 amendments. Sáu existing API amendments gồm operation
enum reauthentication và opt-in Business contexts/Organization list/detail,
Work Inbox/Audit v2 (hai reader sau chỉ khi được nhóm sở hữu phối hợp);
không được mở rộng âm thầm shape v1. Các nguyên tắc ACL: không role/login mới;
existing NOLOGIN writer và trusted offline operator giữ độc lập runtime; không
runtime SET ROLE/INHERIT/GRANT OPTION; private predicate chỉ BOOLEAN; helper/trigger
không PUBLIC EXECUTE; protected execute function chỉ action-bound runtime entry,
không arbitrary membership DML. Không SELECT private Identity/Notifications từ
Merchant code. Không sửa schema-owner guards hoặc SYSTEM realm.

V46/V47/V48 là **candidate chưa reserve**, do hiện tại stream có V1–V45, không V33.
Team khác có thể thêm migration: phải kiểm tra cả branch tích hợp trước chốt số.
Identity/Notifications migrations phải có tên/số/chữ ký được owner co-sign riêng,
không nhét unapproved privilege vào V48. Không sửa historical V1–V45, không clean/
repair/reset. Baseline manifest và WIP bytes giữ nguyên.

## 7. Gates và thứ tự implementation sau approval

1. Duyệt M2-R01 + DEC-R1. Implement Merchant-only search/comparison/profile core/
   membership metadata/comment/check projections trước; Notifications/Pricing
   blocks đợi port coordination, ghi UNAVAILABLE đúng sự thật.
2. Chốt DEC-L1..L4 + coordination admission/Orders/Finance; implement policy/case/
   restricted state/evidence/clearance và concurrency. CLOSE chưa có CLEAR thì chặn.
3. Chốt DEC-O1..O4 + Identity/Team; thực hiện historical/current-owner bridge,
   proof binding, named-party consent và atomic transfer. Không đi tắt qua Team.
4. Tích hợp existing Work Inbox/Audit/Notifications bằng interface do nhóm kia
   phối hợp; không viết lại Menu 6–9 hay tạo task engine thứ hai.
5. Chạy targeted M2-T-01..08 và một HTTPS+PG18 scenario tổng hợp; tái sử dụng
   P0/NWS evidence không bị tác động. Không lặp full Backend suite.

**Current: CONTRACT REVIEW / OWNER DECISIONS REQUIRED.** Không source coding mới,
không migrate/test DB mutation, grant, deploy hoặc Git mutation ở checkpoint này.
Menu 2 full acceptance vẫn BLOCKED_CONTRACT/INTEGRATION_DEPENDENT; proposal không
phải evidence backend PASS hay integration acceptance.
