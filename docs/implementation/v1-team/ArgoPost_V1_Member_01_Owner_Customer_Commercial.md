# ARGOPOST V1 — NAM — Customer & Commercial

> **V1.6 — Tri-Co-Owner governance, full-stack acceptance & rescheduled execution.** Work assignment độc lập cho Codex của Nam. Đọc toàn bộ file trước khi lập kế hoạch hoặc sửa source. Không dùng file của thành viên khác để tự nhận thêm quyền.

---

| Thuộc tính | Giá trị |
|---|---|
| `ROLE_ID` | **V1-A** |
| Thành viên | **Nam — Co-Owner (V1-A)** |
| Vai trò chính | **Customer & Commercial** |
| Accountable cho | Identity/Merchant/Orders customer journey; Finance/Claims; 3-Co-Owner integration |
| Các Slice chính | S1, S5-request, S7, S8-coordination |
| Phiên bản tài liệu | `TEAM-ASSIGNMENT-v1.6 — 29NOV–07DEC EXECUTION` — 2026-11-29 |
| Trạng thái | `CO-OWNER PLANNING / WORK ASSIGNMENT — NOT TECHNICAL SECURITY APPROVAL` |
| Mục tiêu release | `V1_OPERATIONAL_E2E_ACCEPTED — SOURCE/TEST` (cả ba Co-Owners cùng nghiệm thu) |
| Deadline mục tiêu (không phải bảo đảm) | **07/12/2026** — GO/NO-GO theo evidence |

**SELF-IDENTIFICATION:** Khi nhận file này, Codex phải tự nhận `ROLE_ID=V1-A`, **không được** trả lời như thành viên khác. Nếu prompt muốn sửa module thuộc người khác, chỉ lập impact/contract request rồi handoff, trừ khi có explicit Owner assignment cho đúng change-set. Người được phân công file này là **Nam — Co-Owner phụ trách Customer & Commercial**.



**TEAM ROSTER — áp dụng cho cả ba Codex:** `V1-A = Nam (Co-Owner/Customer & Commercial)`; `V1-B = Hoàng (Co-Owner/Carrier Network & Custody)`; `V1-C = Triết (Co-Owner/Dispatch & Courier Delivery)`. **Tên thành viên là người phụ trách công việc, không phải runtime application role, không cấp quyền Git/database.** Mỗi Codex chỉ dùng ROLE_ID của file mình.

> **TEAM REPLAN — `TEAM-REPLAN-29NOV-07DEC-2026`:** Nam yêu cầu chuyển single active calendar sang **29/11–07/12/2026 (9 ngày lịch)**, GO/NO-GO mục tiêu **07/12/2026**, không giờ cố định. Theo cơ chế ba Co-Owners, Nam/Hoàng/Triết cùng ACK thay đổi deadline/scope này trong kênh nhóm trước khi coi là release commitment. Lịch cũ 28/11–05/12 và tất cả D0–D8 milestone chỉ còn là lịch sử, **không phải deadline hiện hành**. Giữ S1 đã PASS SOURCE/TEST ngày 27/11; xác minh riêng việc merge `develop` và clean-clone Docker Compose, không giả định đã PASS. Spec012 vẫn PROPOSED cho đến khi independent review chấp thuận.

**FLEXIBLE DAILY COORDINATION:** Chỉ mục tiêu theo ngày, không ấn định giờ họp, handoff, smoke test, status hoặc GO/NO-GO. Bàn giao hợp lệ ngay khi sẵn sàng; thiếu input thì `DEPENDENCY_NOTICE` + `NEXT_INDEPENDENT_TASK`, không idle hoặc bịa evidence. Acceptance/security/approval không được nới lỏng bởi lịch mới.

**FULL-STACK RELEASE RULE (v1.6):** Backend-only, UI-only hoặc contract-only không được ghi `FEATURE_FULL_STACK_PASS`/`SLICE_E2E_PASS`; xem III.3–III.5, V.3 và VI.5. `INTEGRATION_READY` là gate bổ sung, không thay thế EVIDENCE_READY.

**CO-OWNER IMPLEMENTATION RULE:** Mọi task trong domain mình đã giao và đã có technical authority được Codex thực hiện trực tiếp theo prompt hằng ngày; **không tạo checkpoint xin Nam phê duyệt**. Với các vấn đề cross-domain, hỏi đúng Co-Owner liên quan hoặc reviewer độc lập theo bảng Mục 0; nếu chưa xong, chuyển task độc lập.

**PRE-TASK RULE:** Trước khi gửi yêu cầu implement một task phụ thuộc của Member khác, Codex **phải đối chiếu V.2 và tạo `DEPENDENCY_NOTICE` theo VI.0**. Nếu chưa nhận được approved contract/source và evidence ở đúng level, chỉ triển khai phần riêng độc lập; không edit phần phụ thuộc. Tạo notice không đồng nghĩa đã thông báo cho người khác — kiểm chứng kênh gửi.

## 0. TEAM CO-OWNER GOVERNANCE — bảo lưu quyền đồng Owner từ v1.5

> **CO-OWNER DECISION `TEAM-COOWNER-DELEGATION-2026-11-27`:** Nam, Hoàng và Triết là **ba Co-Owners ngang quyền trong quản trị kế hoạch ArgoPost V1**. Không có bước "gửi Nam xin Owner approval" mặc định đối với task, sửa lỗi, UI/backend/test hay điều phối trong domain đã giao và đã có technical authority. Quyết định được ghi trong tài liệu nhóm này như **chính sách điều hành dự án**; không giả định nó tự cấp quyền PostgreSQL, Git, AWS, production hoặc vượt các quy định AGENTS.md / review bắt buộc.

| Mức quyết định | Người quyết và cách thực hiện | Có phải chờ Nam duyệt? |
|---|---|---|
| **L0 — Tự chủ domain**: lựa chọn Task ID, tái sử dụng, sửa bug, UI/Backend/tests trong approved boundary, đổi thứ tự P0/P1/P2 không đổi deadline/scope | **Nam** tự quyết Customer/Commercial; **Hoàng** tự quyết Network/Custody; **Triết** tự quyết Dispatch/Courier. Codex báo scope/source/tests cho Co-Owner của phiên làm việc và thực hiện ngay khi gate hợp lệ. | **Không** |
| **L1 — Handoff tương thích**: dùng API/DTO/event đã approved, consumer ACK, contract tests, integration smoke, báo blocker | **Provider + consumer** xác nhận source/version/evidence; không cần Co-Owner thứ ba ký hộ. | **Không** |
| **L2 — Shared contract/source**: sửa interface chưa được duyệt, migration registry, workflow/common shell, file chạm hai domain | **Các Co-Owners của domain bị ảnh hưởng** thống nhất minimal change-set, gán **1 CODE_WRITER + 1 reviewer khác writer**; thông báo người thứ ba để biết tiến độ. Nếu thay đổi ảnh hưởng ba domain thì **cả ba** đồng thuận. | **Không phải xin riêng Nam nếu Nam không thuộc domain bị ảnh hưởng** |
| **L3 — Protected boundary**: mới/đổi ACL/role/identity privileges, SECURITY DEFINER, cross-private reads, DB migration đã áp dụng, payment/COD, legal/production/cloud | Co-Owners liên quan chuẩn bị exact delta; **giữ nguyên independent technical/security/automatic review và quyền hạn thực có**. Co-Owner không thể tự vượt review bị từ chối, không tự cấp thêm permission. | **Không có "Nam approval" bổ sung nếu review hiện hành không yêu cầu; nhưng vẫn phải qua review hợp lệ** |
| **Team scope/release/deadline**, S8 `TEAM-V1-GO-NOGO` | **Nam + Hoàng + Triết** cùng ký trên **cùng integrated source revision** và bằng chứng tương ứng. Chỉ ghi GO khi cả ba sign-off và tất cả hard gates PASS; thiếu người ký hoặc bất đồng thì `PARTIAL/NO-GO`, không tự suy diễn GO. | **Không có Owner duy nhất** |

**Không tự động phê duyệt các delta còn PROPOSED.** File assignment là kế hoạch; authority sửa nguồn phải được xác minh trong repo/review. Các phê duyệt trước đó (ví dụ `BIA-DB-GUARD-01`) vẫn giới hạn đúng phạm vi, hash, điều kiện và quyết định tự động hiện hành; không nới quyền chỉ do đổi cơ cấu Co-Owner.

**Quy trình quyết định không gây chờ:** (1) chủ domain tự xử lý L0; (2) khi thật sự cần L2, ghi `COOWNER_DECISION_RECORD` tối thiểu `DECISION_ID | TASK_ID | AFFECTED_DOMAINS | WRITER | REVIEWER | CONTRACT_VERSION | SECURITY_IMPACT | ACK/REJECT | TEST_GATES` để người liên quan xác nhận, *không chuyển vòng qua Nam*; (3) nếu một bên chưa thống nhất, đánh dấu `WAITING_CROSS_DOMAIN_DECISION`, làm `NEXT_INDEPENDENT_TASK`, không chiếm writer lock hoặc tự đổi contract; (4) L3 theo review bắt buộc, không sử dụng phê duyệt nhóm làm phương tiện né review.

**Mốc thời gian vẫn theo ngày, không giờ cố định:** 29/11–07/12/2026; giao ngay khi đủ output. Mỗi Co-Owner tự báo status và lưu evidence; người cập nhật dashboard là **integration steward luân phiên hoặc người cả ba chỉ định**, không có quyền đơn phương duyệt tất cả Slice.

## I. Vai trò của tài liệu và nguyên tắc ưu tiên

Tài liệu này là **work assignment + acceptance plan**, được cập nhật thành **kế hoạch T1–T9 (29/11–07/12), 9 ngày lịch**, không phải cam kết 8 Slice chắc chắn sẽ hoàn thành cho Codex của đúng thành viên được định danh tại phần đầu. Nó **không** là sự phê duyệt triển khai tự động, không thay thế `AGENTS.md`, Project North Star, approved technical contracts, domain ownership hay quyết định Co-Owner và technical review hiện hành. Khi có khác biệt, dừng phần xung đột, đối chiếu tài liệu có hiệu lực và xin quyết định rõ ràng; **không được tự sửa luật**.

**Mục tiêu V1 (bắt buộc):** một canonical logistics execution cho hai loại chủ vận đơn `PERSONAL | BUSINESS` với **cả `DROP_OFF` và `PICKUP`**. Bốn luồng thành công phải chạy từ đăng ký/đăng nhập hoặc onboarding hợp lệ → Quote → Booking/Shipment/Parcel/Label → hàng vào custody Carrier → Station Inventory/Sorting/Manifest → Transfer/Hub Receive → Dispatch/Courier → Delivery Attempt/POD → Delivered → Tracking/Finance/Support tương ứng. Đường Pickup bắt đầu bằng Courier đến lấy hàng và bàn giao Station; Drop-off do khách đưa hàng đến Station. Sau khi hội tụ, **không có hai bộ engine hoặc hai nguồn sự thật**.

**Mức nghiệm thu:** V1 tối thiểu là **SOURCE/TEST tích hợp cô lập** với Backend thật, PostgreSQL18, HTTPS UI/Mobile phù hợp, actor và command nghiệp vụ thật; dữ liệu tài khoản, Station, hợp đồng trong fixture phải khai báo là test-only. Không được suy diễn rằng test mô phỏng tương đương UAT/Production hoặc mạng lưới logistics thương mại. Phạm vi V1 **không yêu cầu** tối ưu lộ trình AI, full Admin menus, toàn bộ kế toán, bồi thường tự động hay COD không có contract.

### I.1. Bộ tài liệu cần đọc trước mỗi task

1. `AGENTS.md` ở root và mọi bản `AGENTS.md` áp dụng theo thư mục đang sửa.
2. `docs/implementation/ARGOPOST_PROJECT_NORTH_STAR.md` và `docs/implementation/ARGOPOST_E2E_NORTH_STAR.md` nếu hiện diện; không chép đè một bản lên bản khác.
3. `docs/implementation/v1-operational-e2e-critical-path.md`, `docs/implementation/v1-dual-audience-source-test-execution.md` và báo cáo mới nhất trong repo.
4. Các **approved** contract/ADR/spec và evidence hiện hữu cho slice đang làm, đặc biệt `v1-business-station-intake.delta.md`, `v1-intake-completeness-guards.delta.md` khi đụng Station; chỉ bản có chứng cứ technical/domain approval hợp lệ theo quyền thực tế mới cấp authority.
5. **Lịch duy nhất:** `docs/implementation/v1-team/ArgoPost_V1_Team_Execution_Calendar_2026-11-29_to_2026-12-07.md` (nếu chưa có ở repo phải bổ sung đúng nội dung, không tự tạo lịch khác).
6. Source/API/schema/migrations/test hiện tại và Git status; kiểm chứng file thực có mặt trước khi đưa ra đường dẫn/claim. Tài liệu cũ không thay thế trạng thái code mới nhất.

**Quy tắc nhận task:** Co-Owner phụ trách domain gửi `MỤC 5 — NHIỆM VỤ HIỆN TẠI` hoặc một nhiệm vụ rõ ràng trong prompt. File này định nghĩa *trách nhiệm dài hạn*, không tự cho phép Codex thực thi mọi slice cùng lúc. Nếu chưa có task rõ ràng hoặc chưa xác minh được technical authority, làm **read-only audit/impact/contract proposal**, rồi báo blocker; không tự chạy migration hay phát triển domain chưa duyệt.

### I.2. Trạng thái nguồn trước lịch mới — kiểm chứng ngày 29/11

- **S1 `V1_SLICE_1_DUAL_AUDIENCE_READY — SOURCE/TEST`: PASS đã ghi trong `v1-dual-audience-source-test-execution.md` (final continuation 27/11).** PERSONAL và BUSINESS đã có genuine HTTPS + PostgreSQL18 Booking hai Parcel → scan → atomic Station Acceptance → custody/tracking, business review/activation, security/race/replay/rollback. Reuse theo fingerprint; không rebuild.
- V48/V49 và historical migrations đã có kiểm chứng catalog/ACL/preservation trong báo cáo; **không tuyên bố máy khác hoặc `develop` đã có cùng source**. `DEVELOP_BASELINE_READY` (clean clone Docker Compose + migration/bootstrap + S1 regression) phải được xác minh tách biệt. Nếu Nam đang tích hợp release, Hoàng/Triết không chỉnh cùng shared bootstrap/migration path.
- **Spec012 — Station Inventory → Sorting → Manifest vẫn `PROPOSED/BLOCKED_CONTRACT`** theo file đề xuất 27/11: các policy, API, role/ACL, SQL và acceptance cần review trước runtime code. Spec013/014/015/029 và pickup/payment cần kiểm kê authority thực tế, không tự gọi approved.
- S2–S8 chưa có báo cáo integrated Full-Stack PASS trong nguồn đã xem. Dữ liệu UNPAID, Excel Draft và Claims có thể đã có component; audit/reuse chính xác trước khi mở runtime task.
- Menu2 Step B tiếp tục `DEFERRED_POST_V1`; không tự quay lại Admin lifecycle expansion.

**Quy tắc nguồn:** Nếu bản repo/báo cáo mới hơn chứng minh kết quả khác, cập nhật evidence register và thông báo affected Co-Owners; không suy đoán `develop` đã merge, AWS/S3 hoặc Docker đã READY.

### I.3. SOURCE-FIRST INVENTORY — bắt buộc trước mọi code (T1, lặp lại khi nhận task mới)

**Không được triển khai dựa trên danh sách tính năng trong tài liệu này mà bỏ qua code đã tồn tại.** Báo cáo tiến độ ngày 27/11 là **baseline tham khảo, không phải trạng thái mới nhất của repository**. Codex phải đọc thực tế rồi chỉ làm **GAP có chứng cứ**. Không được đổi tên, sao chép hoặc xây lại engine đang tồn tại chỉ để hoàn thành Task ID.

**Trình tự `AUDIT → REUSE → GAP → CONTRACT → IMPLEMENT → PROVE`:**

1. **Read-only preflight:** đọc `AGENTS.md` (kể cả nested), hai North Star nếu có, Owner task mới nhất, critical-path, execution report mới nhất, approved deltas/ADRs/specs, functional coverage và evidence; ghi `git status --short`, `git diff --name-only`, `git log -1`, branch/worktree, staged index, migration list/registry, remote branches/PRs nếu có quyền xem. Không reset, clean, stash, commit, push hay sửa trạng thái để phục vụ audit.
2. **Source discovery:** tra bằng code search và các source files thực tế: class, controllers, ports, UI routes, schema, migrations, flags, security policy và tests. Đọc implementation thật, không chỉ tên `*Spec*`, TODO, README hoặc test stub. Phát hiện code đã có nhưng chưa nối HTTP/UI/worker; **ưu tiên wiring và tests thay vì tạo module mới**.
3. **Historical evidence verification:** đối chiếu exact Git revision, test names/results/run IDs, schema/checksum/fingerprint và dependencies thay đổi từ lúc PASS. PASS cũ được giữ là historical; nếu code liên quan đã đổi hoặc chưa từng test tích hợp thì ghi `STALE_EVIDENCE`/`NOT_INTEGRATED`, không ghi `CURRENT_PASS`.
4. **Gap register theo task ID:** ghi mỗi capability dưới một trạng thái: `REUSE_CURRENT_PASS`, `REUSE_NEEDS_REGRESSION`, `EXISTS_NEEDS_INTEGRATION`, `PARTIAL_GAP`, `MISSING`, `BLOCKED_APPROVAL`, `NOT_V1`. Kèm canonical source paths, behavior hiện hữu, bằng chứng, change nhỏ nhất, domain/source writer, approval, E2E dependency, estimated effort và test bắt buộc.
5. **Giới hạn patch:** chỉ chọn `PARTIAL_GAP`/`MISSING` có approved authority hoặc `EXISTS_NEEDS_INTEGRATION` trong phạm vi được phép. `REUSE_CURRENT_PASS` thì **không viết lại**, chỉ bổ sung integration/negative tests cần thiết. Code path/DB object không có trong source phải đánh dấu `PROPOSED`, không khẳng định đang hiện hữu.
6. **Chống đụng source:** trước mọi lần edit, đối chiếu owner+worktree+path-lock với hai teammate; nếu trùng file/port/migration, yêu cầu các Co-Owners có domain liên quan thống nhất đúng **một CODE_WRITER**. Khi hết quyền, chỉ mở request/handoff, không sửa source người khác.
7. **T1 audit output:** `ROLE_ID`, source-and-evidence inventory, `TASK_ID × STATUS × REUSE/GAP × PATH × OWNER × APPROVAL × TEST × DEPENDENCY × TARGET_DAY`, top blockers, và kế hoạch 24 giờ đầu. Report ngắn gọn nhưng đầy đủ, không làm giả test.

**Lệnh rà soát gợi ý (read-only; điều chỉnh theo cấu trúc repo):**

```powershell
git status --short
git log -1 --format="%h %H %s"
git diff --name-only
git diff --cached --name-only
git ls-files "*AGENTS.md" "*v1*" "*migration*"
rg -n "PilotHandling|StationIntake|Manifest|DeliveryRun|Pickup|ProofOfDelivery|BusinessShipments|UNPAID" .
```

Không chạy toàn repository bằng truy vấn quá rộng nếu `rg` lộ logs/secret; thu hẹp vào `docs/implementation`, backend, frontend và tests. Không chép thông tin nhạy cảm vào report. Cần kiểm tra quyền đọc registry/remote bằng phương thức đã được dự án cho phép, không kết luận từ kết quả local duy nhất.


## II. Ma trận ownership chung — 3 thành viên, 1 code owner cho mỗi thay đổi

| Domain / deliverable | Nam — CO-OWNER / CUSTOMER & COMMERCIAL | Hoàng — CARRIER NETWORK & CUSTODY | Triết — DISPATCH & COURIER |
|---|---|---|---|
| Identity, Merchant onboarding, Admin Business Approval | **Functional lead**; dùng owner domain/approved identity interfaces | Chỉ consume Ops admission | Chỉ consume Courier admission |
| Personal/Business Quote, Booking, Parcel, Label, Business Excel/CSV | **Lead + Orders/Merchant/Customer consumer** | Chỉ read canonical shipment/parcel qua port | Chỉ read task eligibility/label cần thiết qua port |
| Pickup request và Customer UX | **Lead** (request, policy, pricing, cancellation) | Receive về Station | Execute/assign Courier; không ghi customer request thay Nam |
| Station Intake, Scan, custody, Inventory, Sorting, Manifest | Consume receipt/tracking theo owning port, không ghi custody | **Lead / source owner trong assigned boundary**, bao gồm Dispatch-owned Station Intake | Consume readiness, không sửa intake engine |
| Transfer, Hub Receiving, exception/return vật lý trong network | Consume tracking | **Lead** | Handover/reverse events theo contract |
| Last-mile Dispatch, DeliveryRun, Courier App, Attempt, POD | Consume kết quả | Cung cấp ready-to-dispatch và bàn giao từ Hub | **Lead** |
| Finance/payment/reconciliation (phương thức được duyệt), Claims | **Lead**; Finance/CSKH owner controls | Cung cấp operational evidence | Cung cấp Courier proof, **không tự ghi tiền** |
| Final E2E 4 luồng | Lead Customer/Finance/Business | Lead physical custody & network | Lead Courier/POD; **cùng ký nghiệm thu** |

**Lưu ý quan trọng:** `Orders`, `Merchant`, `Dispatch`, `Finance`, `Workflow`, `Identity` là **domain thực tế của hệ thống** chứ không tự động là tên người. Giao việc cho Nam/Hoàng/Triết **không được ghi đè quyền domain owner/ACL hiện hữu**. Một source file/port/SQL object phải có đúng **một người thực hiện chỉnh sửa tại một thời điểm**; nếu tính năng qua hai domain, viết contract và giao owner cụ thể trước khi code. Những thay đổi composite cần thống nhất giữa Co-Owners có domain bị ảnh hưởng; nếu cả ba domain thì cả ba cùng quyết.

### II.1. Ranh giới file/schema theo chức năng (logical scope, không phải glob tự động)

- **Nam chỉ sửa khi được cấp authority:** Customer Portal pages, Merchant application/shipping policy, Orders-owned Booking/Quote/Parcel/Tracking projections, Finance/Claims consumers, Admin Business Approval hẹp. Tránh tác động `Identity` private code, shared `_layout`, routing/bootstrap, ACL khi chưa có approval hợp lệ theo domain/technical authority.
- **Hoàng:** Carrier Operations Station/Hub/Inventory/Sorting/Manifest/Transfer, `OperationalStationIntake` và các table/guards/custody do Dispatch sở hữu **thuộc Station Intake**. Không sửa Orders-owned tracking guard/Shipment private reader: chuyển request cho Nam/domain owner.
- **Triết:** Courier Mobile và Dispatch **DeliveryRun/Courier assignment/Pickup/Delivery** subdomain riêng. Không sửa `OperationalStationIntake`, Station custody persistence hay Manifest tables của Hoàng; kết nối qua approved ports.
- File dùng chung (`AGENTS.md`, North Star, application bootstrap, shared workflow/config, global type definitions, common Gradle/POM, migration registry, unified HTTP router, global UI shell, generated clients) = **INTEGRATION LOCK**. Không hai Codex sửa song song; có change request và người giữ lock được Owner chỉ định.
- Migration **không phân vùng bằng số version cho từng người**; sử dụng registry/check toàn repo và tất cả branch/PR trước khi chọn số, một migration writer tại một thời điểm. Không sửa migration đã áp dụng; forward-only với preflight/checksum/owner/ACL tests.

### II.2. Cách phối hợp tránh conflict

1. Mỗi người dùng clone/worktree/branch **riêng**; không để hai Codex ghi cùng working tree hoặc Git index. Branch được **đề xuất**, chỉ được commit/push/merge khi quyền Git thực tế + AGENTS/CI/branch protection cho phép; quyền quyết định domain được ủy quyền cho Co-Owner tương ứng, không phải xin Nam riêng.
2. Trước khi sửa shared path: xuất `CHANGE_REQUEST` gồm `CR-ID`, module/file, domain owner, mục tiêu, interface signature, schema/data/permission effect, migration & compatibility, test, thời điểm handoff. Các Co-Owners của domain bị ảnh hưởng thống nhất duy nhất một `CODE_WRITER` và một `REVIEWER` độc lập; phần còn lại chờ contract hoặc làm việc độc lập.
3. Cấm Hoàng đọc Orders private SQL (hoặc Triết đọc Hoàng private SQL) từ application layer để né public ports. **Riêng DB trigger cross-domain read** chỉ được sửa theo exact approval có security review, phải giữ `SECURITY INVOKER`, signature, owner và ACL trừ khi có phê duyệt khác; không xem là pattern cho truy cập tùy ý.
4. API contract được chốt trước integration: authenticated actor, owner type/account/org/facility/site, command + idempotency key, eligibility/expected state, request/response, error, events, version, rollback và negative cases.
5. Một canonical Shipment + Parcel IDs; mỗi custody chuyển giao có một sự kiện và một current holder hợp lệ; không ghi duplicated tracking, không dùng từ `DELIVERED`, `PAID` hoặc `IN_TRANSIT` nếu chưa có evidence.
6. Mỗi PR/handoff cần sanitized evidence và fingerprint/source revision; không gửi mật khẩu/OTP/email thật/token, không đụng UAT/Production, AWS IAM/S3 hoặc cấp quyền người thật khi chưa được phép.
7. Một người có thể tiếp tục **đọc, viết draft contract, test unit độc lập** trong lúc chờ dependency, nhưng không viết mock data/state để “chứng minh” một nghiệp vụ thực đã chạy.

## III. 8 Slice và mốc hoàn thành Full-Stack — kế hoạch mới 29/11–07/12

**Single active schedule:** T1=29/11, T2=30/11, T3=01/12, T4=02/12, T5=03/12, T6=04/12, T7=05/12, T8=06/12, T9=07/12/2026 (giờ Việt Nam, **không đặt giờ cố định**). Lịch cũ 28/11–05/12 và các mốc D0–D8 cũ **được thay thế hoàn toàn**, không còn là deadline triển khai. Các mốc mục tiêu chỉ đạt khi có evidence; ba Co-Owners xác nhận thay đổi kế hoạch chung này trong repository/team channel trước khi coi là quyết định release có hiệu lực.

| Slice | Kết quả Full-Stack bắt buộc | Người điều phối / CODE_WRITER | Ngày dự kiến hoàn chỉnh | Điều kiện trước khi ghi `SLICE_FULL_STACK_PASS` |
|---|---|---|---|---|
| **S1 — Dual-audience Booking + Station** | Personal/Business đăng nhập, Merchant xét duyệt, Quote→Booking→Parcel/Label, Station Scan→Acceptance→Custody/Tracking UI | **Nam** Orders/Customer; **Hoàng** Station; cả hai xác nhận | **Đã PASS 27/11** ở SOURCE/TEST; **30/11** reverify trên develop/Compose sau khi baseline sẵn sàng | Evidence S1 hiện hữu được reuse nếu fingerprint không đổi; clean-clone/baseline E2E vẫn phải kiểm chứng riêng, không suy diễn đã merged |
| **S2 — Inventory/Sorting/Manifest** | Operations Web Inventory→Sort→Manifest Draft/Seal và scoped API/PG18/negative tests | **Hoàng** | **03/12** | Spec012 bounded policy/API/ACL approved, canonical custody, UI thao tác thật; không coi Seal là Transfer |
| **S3 — Transfer/Hub Receiving** | Sender Web handover → Hub transfer → receiver Web scan/reconcile/discrepancy | **Hoàng** | **04/12** | Spec013 approved, real sealed Manifest, 1 holder, E2E custody/negative/rollback |
| **S4 — Dispatch/Delivery/POD** | Dispatcher Web assign DeliveryRun, Courier Mobile accept/scan/attempt/POD, Delivered + tracking | **Triết** (Courier/Dispatch); **Hoàng** Hub release | **05/12** | Approved Spec014/015/029 & proof policy; genuine Hub→Courier custody, proof; no fabricated Delivered |
| **S5 — First-mile Pickup** | Customer Personal/Business Pickup UI→Courier Mobile collection→Station Web receive→shared S2–S4 | **Triết** Slice lead; **Nam** Request; **Hoàng** Station | **06/12** | Approved request+handover, genuine sender→Courier→Station custody, end-to-end both owner types |
| **S6 — Failure/Retry/Return** | Courier failed pickup/delivery + retry/return; Network reverse handover; Customer/CSKH states | **Triết** lead; **Hoàng** Network; **Nam** Claims | **06/12** | Authorized policy, operational UI/Mobile, negative/replay tests, reverse custody consistency |
| **S7 — Excel/Finance/Claims** | Business Excel/CSV Draft→Quote→Booking, supported verified settlement/reconciliation and CSKH/Claims Web UI | **Nam**, Hoàng/Triết provide actual facts | **06/12** | Explicit finance/payment authority, non-fake PAID/COD, actual provenance and UI/PG18 evidence |
| **S8 — 4 E2E journeys** | P/B × Drop-off/Pickup, auth/security/finance/exception and responsive UI/Backend/Mobile/PG18 | **Nam + Hoàng + Triết** ký chung | **07/12** GO/NO-GO | All required gates on **same integrated source revision**, signed evidence; any blocker → PARTIAL/NO-GO |

**Không có câu trả lời “chắc chắn sẽ xong” theo ngày:** Spec012 là proposal ở thời điểm lập lịch; Spec013/014/015/029, payment authority và Git/Compose baseline phải kiểm chứng thực trạng. Mốc trên là forecast để ưu tiên; nếu contract không được duyệt hoặc E2E FAIL, Codex phải ghi `AT_RISK`/`PARTIAL`, không tự động dời thành PASS. S1 historical acceptance vẫn giữ nguyên, không lặp lại implementation.

### III.1. V1 Final Acceptance (4 hành trình bắt buộc)

| Test ID | Owner | First mile | Chuỗi có bằng chứng |
|---|---|---|---|
| `V1-E2E-P-DROP` | PERSONAL | Drop-off tại Station | Book/label → Station scan → Manifest → Hub → Courier → POD/Delivered |
| `V1-E2E-P-PICK` | PERSONAL | Courier Pickup | Book → Pickup request/assignment/scan → Station handover → Hub → POD/Delivered |
| `V1-E2E-B-DROP` | BUSINESS | Drop-off tại Station | Independent Org Approval + shipping config → Business Booking → Station → Hub → POD/Delivered |
| `V1-E2E-B-PICK` | BUSINESS | Courier Pickup | Org permission + Booking → Pickup → Station → Hub → POD/Delivered |

**V1 cũng cần:** Business import valid XLSX/CSV row → real Draft → Quote → Booking; physical failed delivery → reattempt hoặc return với custody hợp lệ; eligible ticket/claim → CSKH review/response/closure; đúng fee/UNPAID/real supported payment + reconciliation; negative tests wrong audience/org/facility/site, revoked staff, duplicate, stale version, concurrent acceptance, partial failure rollback, permission/PII, no direct SQL event forgery. Bất cứ giá trị nào chưa có policy/contract được phê duyệt phải là `BLOCKED_CONTRACT`, không được hợp thức hóa bằng fixture.

### III.2. Evidence & Done chuẩn cho mỗi đầu việc

`DONE` chỉ khi có: (a) exact approved authority; (b) implementation từ canonical source; (c) migration preflight nếu DB thay đổi; (d) Backend PG18 tests + HTTPS integration tại boundary tương ứng; (e) actor-facing Web/Mobile UI bắt buộc với các hành động V1 có giao diện, chạy trên Backend+PG18 thật; (f) negative auth/tenant/site/duplicate/concurrency/rollback & business invariant; (g) evidence sanitized (run ID, revision/fingerprint, test command/result, security catalog khi cần); (h) handoff interface version và bất kỳ known limitations. Đã có PASS còn cùng revision/dependencies thì reuse, không chạy lại toàn bộ suite không cần thiết. `PASS` cho contract tests **không** tương đương `E2E_READY`.

Tình trạng báo cáo thống nhất: `NOT_STARTED`, `AUDITED`, `BLOCKED_CONTRACT`, `READY_TO_IMPLEMENT`, `IN_PROGRESS`, `PARTIAL`, `SOURCE_TEST_PASS`, `INTEGRATED_E2E_PASS`, `DEFERRED_POST_V1`. **Không đơn phương đặt `V1_OPERATIONAL_E2E_ACCEPTED`**; ba Co-Owners cùng nghiệm thu theo evidence trên cùng integrated revision.

### III.3. FULL-STACK DEFINITION OF DONE (DoD) — áp dụng cho S1–S8

**Quy tắc cứng:** `IMPLEMENTED_BACKEND` / API PASS / migration PASS / `UI_PROTOTYPE_READY` / screenshot / mocked fixture **không tương đương** `FEATURE_FULL_STACK_PASS`. Mọi thao tác nghiệp vụ V1 dành cho Customer, Admin reviewer, Operations/Dispatcher, Finance/CSKH hoặc Courier đều cần **đường thao tác Web/Mobile thực** nối vào **cùng Backend + PostgreSQL18** trong SOURCE/TEST. Không ép làm giao diện cho internal-only guard/port; nếu không cần UI, ghi `UI_GATE=NOT_APPLICABLE` kèm actor-facing screen/flow nào tiêu thụ kết quả và phải chứng minh ở cấp Slice.

| Cổng nghiệm thu | Điều kiện bắt buộc | Evidence tối thiểu |
|---|---|---|
| **G0 — Reuse & authority** | Audit source hiện tại, dùng lại module đã có; contract/policy/role/migration phù hợp và CODE_WRITER rõ; giữ lịch sử | Reuse/GAP register, approval ref, source paths, branch/diff fingerprint |
| **G1 — Canonical Backend** | Domain state transitions, validations, permissions theo actor/site/ownerType, idempotency/concurrency, no fake physical/finance facts | Targeted backend tests, denial + replay/race/rollback, exact API status/errors |
| **G2 — Database integrity** | PG18 persistence, migrations cold-start/forward upgrade khi liên quan, FK/ACL/checksum/guard, immutable evidence, transaction boundaries | PG18 test run IDs, schema/object-owner/catalog gate, migration preservation |
| **G3 — Actor-facing UX** | Đủ **màn hình và thao tác V1** do UI matrix xác định; login đúng actor, field validation, detail/list/action/confirmation/status; responsive phù hợp Web hoặc Emulator | Current actual UI implementation path, UI tests và ảnh/video kiểm tra khi cần, không chỉ fixture |
| **G4 — Real API wiring** | Giao diện gửi command tới Backend thật, đọc lại data persisted, xử lý pending/empty/loading/error/denied/stale/retry; không hard-code trạng thái thành công | Real HTTPS Browser/Mobile UI ↔ Backend ↔ PG18, request/event/receipt correlation |
| **G5 — Cross-role integration** | Upstream/downstream sử dụng đúng contract version và source revision **đã được consumer nhận**, test tối thiểu cùng environment; no cross-domain SQL bypass | `HANDOFF_PACKET` ở `INTEGRATION_READY`, consumer ACK, integration run IDs |
| **G6 — Slice E2E** | Actor thao tác từ UI, dữ liệu di chuyển qua canonical pipeline đến outcome bắt buộc của Slice, có happy/negative/unauthorized/failure tests | End-to-end trace IDs + run IDs + UI/API/PG evidence, no mocked physical pass |
| **G7 — Three Co-Owner sign-off** | Nêu rõ PASS/PARTIAL/BLOCKED với residual risk, cả PERSONAL/BUSINESS và DROP_OFF/PICKUP ở chặng áp dụng | Signed role-specific slice reports và đồng thuận GO/NO-GO của ba Co-Owners; không suy luận production readiness |

**Trạng thái tiêu chuẩn:** `AUDITED`, `CONTRACT_READY`, `BACKEND_READY`, `UI_PROTOTYPE_READY`, `UI_BACKEND_INTEGRATED`, `INTEGRATION_READY`, `FEATURE_FULL_STACK_PASS`, `SLICE_E2E_PASS`, `BLOCKED_APPROVAL`, `WAITING_FOR_HANDOFF`, `PARTIAL`. `CONTRACT_READY` chỉ xác nhận giao diện kỹ thuật; `INTEGRATION_READY` chỉ xác nhận tích hợp một dependency; `FEATURE_FULL_STACK_PASS` là một chức năng đủ G0–G5; `SLICE_E2E_PASS` phải qua G6 và có consumer acceptance. Không dùng trạng thái trung gian làm bằng chứng cả Slice/V1 PASS.

**Required UX acceptance checklist cho mọi actor-facing feature:** (1) route có thể mở từ navigation theo role; (2) danh sách/chi tiết/biểu mẫu/action chính; (3) validation trước submit **và** backend validation; (4) xác nhận thành công dựa response + persisted read-back; (5) loading, empty, error, unauthorized/forbidden, conflict/stale, retry; (6) prevent duplicate submits; (7) không lộ PII, thông tin cross-tenant, proof URL, giá không có quyền; (8) responsive/touch usability cơ bản và accessibility label/focus khi phù hợp; (9) hiện trạng thái UNKNOWN/UNAVAILABLE thay vì suy diễn; (10) có UI automation/Browser hoặc mobile-emulator proof trên runtime thật. Nếu một yêu cầu không áp dụng, ghi `N/A_REASON` và upstream/downstream evidence thay thế, Owner/reviewer xác nhận.

**Traceability record mỗi tính năng:** `FEATURE_ID | SLICE/TASK | ACTOR | UI_ROUTE/SCREEN | USER_ACTION | BACKEND_COMMAND/READ | DATA_OWNER | EVENT/RECEIPT | SECURITY_SCOPE | HAPPY_TEST | NEGATIVE_TEST | REAL_UI_TEST | REUSE/GAP | STATUS | APPROVAL | SOURCE_REVISION`. Không đổi Task ID cũ, **gắn subtask UI/API/test vào Task ID hiện hữu**, không tạo thêm cùng loại engine.

### III.4. V1 ACTOR-TO-UI ACCEPTANCE MATRIX — đủ đường thao tác, không chỉ API

| Slice | Actor → thao tác bắt buộc trên Web/Mobile | Primary UI owner và backend boundary | Điều kiện UI+Backend PASS |
|---|---|---|---|
| **S1** | Customer Personal tạo Quote/Booking, quản lý kiện/in nhãn; Business tạo/duyệt hồ sơ bởi Admin độc lập, quyền Shipping thực; OPS quét đủ Parcel và Accept; Customer đọc `AT_STATION` | **Nam:** Customer + Admin Business review; **Hoàng:** Operations Station; Orders/Merchant/Dispatch qua ports | Browser flows Personal+Business và OPS staff thật, PG18 custody/tracking, correct owner/UNPAID |
| **S2** | OPS mở inventory scoped theo site, lọc/xem Parcel; thực hiện sorting hợp lệ; tạo Manifest Draft, kiểm danh sách, Seal; lỗi kiện/duplicate/stale hiển thị | **Hoàng:** Operations Inventory/Sort/Manifest; **Nam:** customer read-only tracking consumer | UI command từ Ops thật; same Parcel set, version/route guards, seal không tự tạo custody transfer |
| **S3** | OPS tạo/duyệt handover theo quyền; theo dõi transfer; Hub đích quét nhận, đối chiếu đủ/thiếu/thừa/sai kiện, xử lý discrepancy; Customer xem tracking | **Hoàng:** Operations Transfer/Hub Receiving UI; **Nam:** Customer status | PG18 events theo Sender→Transport→Hub, explicit discrepancy, actor/site denial, rollback và read-back |
| **S4** | Dispatcher trên Web tạo DeliveryRun và assign Courier; Courier Mobile đăng nhập, xem job, nhận/scan, Attempt, POD; Customer xem Delivered/failed | **Triết:** Carrier Operations Dispatch Web + Courier Mobile; **Hoàng:** Hub release UI; **Nam:** Customer tracking | Không thể assign khi Hub chưa ready; POD hợp lệ trước Delivered; real mobile-emulator + Browser + PG18 |
| **S5** | Customer Personal/Business chọn Pickup, yêu cầu/hủy/theo dõi; Dispatcher phân công pickup; Courier Mobile nhận/scan; OPS Station nhận từ Courier | **Nam:** Customer Pickup Web; **Triết:** Dispatch Web + Mobile Pickup; **Hoàng:** Ops Pickup Receiving UI | Sender→Courier→Station custody thật, nhận đúng Parcel, cùng pipeline S2–S4, cancellation/failure có feedback UI |
| **S6** | Courier Mobile nhập lý do thất bại/retry; OPS Web xem discrepancy/hold/return handover; Customer xem trạng thái và tạo yêu cầu hỗ trợ | **Triết:** Courier failure/retry; **Hoàng:** Network exception/return UI; **Nam:** Customer/CSKH view | Failure/return có event/custody thật; không mark Returned/Delivered từ UI text; permission+replay denied |
| **S7** | Business tải XLSX/CSV, xem lỗi từng dòng, sửa/confirm → Quote/Book; Finance Web xem obligation/receipt/reconcile; Customer/CSKH gửi/duyệt/đóng Claim | **Nam:** Customer Bulk + Finance Business & Finance + CSKH UI; Hoàng/Triết chỉ provide facts | Valid rows thực sự Booked; supported payment/receipt provenance + recon; ticket/claim có lifecycle và audit |
| **S8** | Các actor thực hiện từ giao diện tương ứng cả 4 hành trình, gồm Admin approval, Operations, Dispatcher, Courier, Customer và Finance/CSKH khi cần | **Nam:** lead Customer/Commercial; **Hoàng:** Network; **Triết:** Courier, cùng ba Co-Owners nghiệm thu | Four real SOURCE/TEST UI→HTTPS→PG18 flows, negative/security/rollback, exact source revision + signoffs |

**UI gate không yêu cầu pixel-perfect hoặc full Portal menus**, chỉ yêu cầu **tất cả thao tác cốt lõi V1** có giao diện có thể sử dụng và nhất quán; UI nâng cao không thuộc critical path để sau V1. Audit nếu màn hình đã tồn tại và PASS thì reuse, không dựng lại. Nếu UI owner khác backend owner, dùng hợp đồng API đã duyệt và bảng CODE_WRITER; không tự sửa source người khác.

### III.5. Evidence mẫu cho một tác vụ Full-Stack

Ví dụ `S3 Hub Receiving`: Operations actor đăng nhập đúng Hub → mở Manifest chuyển đến → scan thực tế qua giao diện → xác nhận nhận/hàng thiếu → HTTP command → PG18 committed events/custody → reload UI thấy đúng accepted/discrepancy → Customer (Nam) chỉ đọc event đã công bố. Lưu `actor/site`, request/correlation ID, Shipment/Parcel/Manifest IDs (sanitized), run IDs, code revision và các negative tests. Một SQL INSERT/test fixture tự tạo received event **không** thay cho chuỗi này. Đối với S4/S5, Courier Mobile phải thực hiện bước scan/accept/proof, không chỉ API unit test.

## IV. Giao thức bắt đầu / kết thúc một phiên Codex

**Read-first acknowledgment bắt buộc (trong phản hồi Codex):** `ROLE_ID`, tên role, active Slice & Task ID, canonical source modules, các source paths sẽ sửa, owner của từng path, authority ID/file (approved/proposed), baseline Git/current WIP, block đang có, target acceptance.

**Trước code:** đọc nhiệm vụ Co-Owner phụ trách domain mới nhất → audit source/evidence và source ownership → kiểm tra approved delta → kiểm tra share-lock → đề xuất smallest executable bounded patch. Nếu thiếu approval, đưa exact minimal checkpoint, không tự code phần bị chặn.

**Sau code/test:** báo theo mẫu:

```text
ROLE_ID: V1-...
SLICE / TASK: Sx / <Task-ID>
STATE: <PARTIAL | SOURCE_TEST_PASS | BLOCKED_CONTRACT | ...>
AUTHORITY: <approved domain contract/Co-Owner decision/independent security review ref/version>
SOURCE: <files/modules actually changed>; BASELINE: <revision/fingerprint>
RESULT: <business behavior verified>; NOT_CLAIMED: <not yet integrated>
TESTS: <PG18/HTTPS/UI/Mobile/architecture/security runs + counts + evidence>
DATA/SECURITY: <owner, site, custody, ACL, migration/rollback evidence>
HANDOFF: <role, port/DTO/event version, required tests>
NEXT: <smallest critical-path task authorized>
BLOCKER: <exact delta + affected Co-Owners or mandatory reviewer + required decision>
PRESERVED: <prior migrations/HEAD/index/WIP/other worktrees>
```

**Bất biến:** Không deploy, commit, push, merge, reset/clean/stash destructive, thay migration lịch sử, cấp real grants, đổi AWS IAM/S3, SMTP thật, sửa/giả business events hoặc mở các module ngoài V1 khi Owner không cho phép.

## V. Phạm vi độc quyền của V1-A — Customer & Commercial

**Mission:** Nam bảo đảm Personal và Business có **admission, giá cước, Booking/Parcel, quyền sở hữu, Pickup Request, nghĩa vụ tài chính và hậu mãi đúng nghiệp vụ**. Hoàng/Triết vận chuyển hàng hóa; Nam **không** tự tạo Station acceptance, Inventory movement, Courier attempt, POD, hoặc payment đã thu. Nam là **Owner/coordinator** của kế hoạch nhóm, nhưng quyết định Owner không thay thế formal security approval theo `AGENTS.md`.

**Nhóm source ưu tiên (chỉ sau khi inspect repo):** Customer Portal (Personal/Business), Merchant application/review/activation, Orders Booking/Quote/Label/owner and customer-facing projections, Finance obligation/approved payment/reconciliation, CSKH/Claims, Admin Merchant Approval V1. Known integration points: `PilotHandling`, `JdbcPilotHandling`, `BusinessShippingHttp`, `OrganizationShipmentManagementHttp`, Business management V2, `BusinessObligations`/`JdbcBusinessObligations`. **`Workflow` và catalog schema có thể là shared paths: claim lock trước khi sửa.**

### V.1. Nhiệm vụ theo từng Slice — task IDs cố định

#### S1 — CUSTOMER BOOKING + MERCHANT ADMISSION (A-S1-01 → A-S1-06)

**Input:** current Identity session, Personal account, Merchant Organization canonical approval, supported route/price policies. **Handoff:** `BookedCargo`/immutable Booking metadata, Parcel exact-set/labels, verified owner and financial obligation cho Hoàng.

- [ ] **A-S1-01 / audit:** Đối chiếu Personal registration/login/verification, genuine quote, Booking, multi-Parcel label; đối chiếu evidence HTTPS hiện hữu; không xây lại thành phần PASS. Registration-public UI nếu chưa có end-to-end evidence phải ghi gap riêng, không tự công bố đã PASS.
- [ ] **A-S1-02 / Merchant admission:** Kiểm chứng Business Application → reviewer độc lập → approved → Org activation. Activation chỉ cấp `dashboard.read` theo contract hiện hành; shipping grants và tariff phải thiết lập bằng operator setup **được duyệt, test-only**. Deny pending/rejected/inactive Org, self-approval, wrong facility và không tự grant.
- [ ] **A-S1-03 / Quotes & Booking:** Xác minh đúng owner `PERSONAL: account`, `BUSINESS: Organization/facility`; đúng tariff, coverage, quote TTL, snapshot bất biến, booking idempotency, Parcel IDs/Code128/PDF, Personal/Business `UNPAID` policy riêng. Không coi creator account là Business owner.
- [ ] **A-S1-04 / Orders interface:** Với BIA-TIMESTAMP-01, sau approved exact delta, bổ sung/kiểm chứng immutable Booking `createdAt` trên Orders-owned `CarrierCargo`; cung cấp owner-neutral cargo/tracking reads qua approved port. Không cho Hoàng đọc private Orders SQL từ application code.
- [ ] **A-S1-05 / Security ownership:** Lỗi `merchant.application_review_comment` owner của LOGIN migrator thuộc trách nhiệm tổ chức remediation **Merchant/migration owner**; sau exact approval điều phối một forward-only migration đúng canonical owner, kiểm chứng catalog/ACL và data/checksum preservation. **Không tự sửa V46 lịch sử.** Nếu DB guard thuộc Orders cần sửa, Nam giữ source writer/DB reviewer theo exact BIA-DB-GUARD authority; phối hợp Hoàng với guard Dispatch.
- [ ] **A-S1-06 / Customer presentation & E2E:** Personal và Business nhìn `WAITING_FOR_HANDOVER`/`AT_STATION` từ events thực; không sửa immutable Booking snapshot như live status; hỗ trợ Hoàng chạy consolidated HTTPS Business: Admin approval → explicit shipping config → quote/booking hai kiện → Hoàng station acceptance → Customer/Business reads.

**PASS khi:** Personal 2-Parcel HTTPS+PG18 scenario fresh/unchanged evidence hợp lệ; Business independent approval→explicit setup→Book 2 parcels→Hoàng accept→AT_STATION; correct tariff/UNPAID/owner/Org/Facility; right audience denied; no cross-tenant leaks; DB ownership gate PASS. *Nam không tự nhận S1 PASS nếu Hoàng chưa có Station evidence.*

#### S2 — CANONICAL ORDER FACTS FOR INVENTORY (A-S2-01 → A-S2-02)

- [ ] **A-S2-01:** Công bố versioned Orders Parcel/Shipment read-only port cho Hoàng: exact Parcel IDs, ownerType, origin/destination/route/service snapshot, acceptance eligibility facts **đã có source**; trả lỗi rõ cho wrong-owner/not-booked.
- [ ] **A-S2-02:** Customer tracking/status projection chỉ đọc real network events được public qua approved owner port; không tạo inventory mirror hoặc tự gán `SORTED`/`MANIFESTED` từ Booking.

**PASS khi:** Hoàng test query exact booked Parcel set và unaccepted/mismatched org/site denial; Nam consumer hiển thị đúng event đã phát sinh và không tiết lộ private customer data cho Ops ngoài quyền.

#### S3 — TRACKING OF HUB TRANSFER (A-S3-01 → A-S3-02)

- [ ] **A-S3-01:** Tiếp nhận event/version contract từ Hoàng, trình bày Transfer/Hub arrival qua shipment/parcel tracking theo quyền Personal/Org/facility; không sửa code Hoàng hoặc tự ghi event.
- [ ] **A-S3-02:** Đối chiếu route/pricing Booking snapshot bất biến khi hàng di chuyển; việc sửa route policy mới không thay giá/nghĩa vụ của Booking cũ.

**PASS khi:** customer tracking hiển thị chain nguồn Hoàng, correct ordering/idempotency, không suy diễn `DELIVERED` hoặc cập nhật ngược dữ liệu Booking.

#### S4 — DELIVERY READERS + CUSTOMER NOTIFICATIONS (A-S4-01 → A-S4-02)

- [ ] **A-S4-01:** Consume DeliveryRun/Attempt/POD fact do Triết sở hữu bằng public approved contract. Bảo vệ recipient PII/ảnh bằng scope và signed/authorized access; không lưu blob URL lộ ra public.
- [ ] **A-S4-02:** Cập nhật Customer Business/Personal detail và Admin V1 oversight cho `OUT_FOR_DELIVERY`, `DELIVERED`, `FAILED_ATTEMPT` **chỉ khi có events/proof hợp lệ**; notifications chỉ ở phương thức được duyệt/kiểm thử.

**PASS khi:** live read sau Triết delivery phản ánh đúng Parcel và Shipment aggregate, wrong account/Org cannot see POD.

#### S5 — PICKUP REQUEST / CUSTOMER CONTRACT (A-S5-01 → A-S5-04)

- [ ] **A-S5-01:** Trong Quote/Booking, hỗ trợ chọn `DROP_OFF | PICKUP` cho cả Personal/Business; kiểm tra pickup coverage, địa chỉ, thông tin liên hệ, slot/window nếu policy cho phép, fee/version snapshot. Không giả định mọi tuyến hỗ trợ Pickup.
- [ ] **A-S5-02:** Định nghĩa Pickup Request với canonical Shipment/Parcel, sender authorization, desired window/address, cancel/reschedule admissibility, idempotency key. Trạng thái yêu cầu khách hàng **không** đồng nghĩa Courier đã lấy hàng.
- [ ] **A-S5-03:** Public owner-specific request/read/cancel contract để Triết phân công Courier. Nam **không** ghi courier assignment/pickup scan/custody; Hoàng nhận station handover từ Triết.
- [ ] **A-S5-04:** Test cả Personal/Business: confirmed pickup request, rejected out-of-coverage address, duplicate request, revoked access, conflicting cancel vs assignment, quote fee integrity.

**PASS khi:** 2 owner types gửi/cancel/read Pickup Request đúng scope, Triết có contract version đầy đủ, không có phantom pickup evidence.

#### S6 — CUSTOMER EXCEPTIONS / CLAIMS (A-S6-01 → A-S6-03)

- [ ] **A-S6-01:** Consume Hoàng/Triết thực tế `PICKUP_FAILED`, `DELIVERY_FAILED`, `REATTEMPT`, `RETURN` events; Customer view nêu lý do vừa đủ, có history và quyền riêng tư.
- [ ] **A-S6-02:** Link đúng Shipment/Parcel và actor tới Ticket/Claim; tái sử dụng CSKH intake, review, request supplement, reply, closure; không tự suy diễn mất hàng, bồi thường/approved payout.
- [ ] **A-S6-03:** Trạng thái đóng claim cần quyết định CSKH hợp lệ + audit, không phải bằng chứng đã thu/hoàn tiền.

**PASS khi:** một failed delivery thật từ Triết tạo được ticket và kết quả CSKH có audit, không tự thay physical state.

#### S7 — BULK BOOKING, FINANCE & CLAIMS (A-S7-01 → A-S7-06)

- [ ] **A-S7-01 / import:** XLSX/CSV Worker → server-side validation/row errors → Preview → idempotent commit thành Draft → từng Draft hợp lệ nhận Quote → explicit Booking → mã vận đơn/Parcel/Label thật; ghi tổng valid/invalid, retry không tạo double Booking. **Import thành Draft không phải Bulk Booking hoàn thành.**
- [ ] **A-S7-02 / pricing:** Verify per Booking immutable charge breakdown, tariff/contract/version, pickup surcharge nếu được duyệt, obligations Personal/Business tách biệt.
- [ ] **A-S7-03 / payment policy gate:** Trình Owner một phương thức thanh toán V1 có thể kiểm chứng (actor, external/internal evidence, receipt, idempotency, reversal, data/ACL) trước khi phát triển; trạng thái `UNPAID` hiện hữu không tự chuyển thành `PAID`.
- [ ] **A-S7-04 / reconciliation:** Chỉ triển khai và đối soát phương thức đã được approved; đảm bảo số tiền, Shipment/Org attribution, fees, duplicate/partial/cancel/refund handling; không giả COD expected/collected/remitted/settled. COD chỉ khi có contract/policy độc lập.
- [ ] **A-S7-05 / support:** Reuse Claims V35/V38 và evidence storage đã được phê duyệt, hạn chế PII, truy xuất delivery proof từ Triết; compensation execution là **ngoài V1** nếu chưa có quyết định mới.
- [ ] **A-S7-06 / finance UI:** Customer/Business/Finance portal hiển thị obligation/payment/reconciliation **theo nguồn Finance**, phân biệt khoản phải thu/đã thu/đối soát/doanh thu và các bất thường.

**PASS khi:** XLSX/CSV valid row → Booked; duplicate import/duplicate payment deny; genuine supported payment + reconciliation có full PG18+HTTPS evidence hoặc công khai `BLOCKED_CONTRACT` nếu phương thức chưa được phê duyệt (không tự tuyên bố S7 READY).

#### S8 — CUSTOMER + COMMERCIAL FINAL ACCEPTANCE (A-S8-01 → A-S8-05)

- [ ] **A-S8-01:** Nam lead Customer/Commercial, cả ba Co-Owners cùng lập matrix đủ 4 kịch bản, phối hợp Hoàng/Triết chuẩn bị PG18 test network, login Business/Personal và real-only simulation.
- [ ] **A-S8-02:** Chứng minh Account/Business lifecycle, correct tariff/quote/booking, drop-off/pickup policy cho từng owner; Business import Booking là kịch bản riêng.
- [ ] **A-S8-03:** Chứng minh finance obligation/payment theo approved method, no double charge; claim lifecycle; PII/privacy/tenant isolation.
- [ ] **A-S8-04:** Tổng hợp run evidence, exact source fingerprint, migration checksum, residual risks; **không tuyên bố Production Ready**.
- [ ] **A-S8-05:** Chỉ đề xuất nghiệm thu đồng thuận ba Co-Owners khi Hoàng physical custody + Triết POD evidence đã ký và toàn bộ blocking tests PASS.

**PASS khi:** 4 E2E do ba người ký + finance/claims/bulk/negative security gates PASS; báo cáo chính xác `V1_OPERATIONAL_E2E_ACCEPTED — SOURCE/TEST` nếu cả ba Co-Owners thống nhất nghiệm thu.

### V.2. PRE-TASK DEPENDENCY MATRIX — Nam (V1-A)

**QUY TẮC:** Trước khi yêu cầu Codex sửa code cho **phần tích hợp phụ thuộc** của bất kỳ hàng nào dưới đây, Nam/Codex V1-A phải lập `DEPENDENCY_NOTICE` gửi đúng người ở cột *Cần thông báo*, đồng thời gửi các Co-Owners của domain bị ảnh hưởng khi có rủi ro critical path. **Không đợi hết ngày mới hỏi**, không mặc định Code của người khác đã có. Nếu trong source đã có handoff đúng revision, đính kèm bằng chứng và kiểm chứng `ACCEPTED`; không tạo request hoặc xây lại vô ích. `CONTRACT` cho phép tích hợp interface đã được phê duyệt; `EVIDENCE` mới đủ để nghiệm thu sự kiện vật lý. Phần riêng của task vẫn được tiến hành khi đủ authority.

| Slice / task cần gate | Cần thông báo trước cho | Phải yêu cầu / nhận được gì | Gate trước code phụ thuộc hoặc E2E | Nếu chưa nhận: tiếp tục task độc lập |
|---|---|---|---|---|
| **S1 A-S1-04/05** (thay đổi shared Orders/guards/migration) | **Hoàng V1-B**; Nam và Hoàng thống nhất path lock cho change-set chạm hai domain | Báo exact Orders `CarrierCargo.createdAt`, tracking guards, V48/change-set, file writer, diff/approved authority; Hoàng xác nhận không có concurrent writer | **CODE_WRITER LOCK + approved contract** trước chỉnh shared source; không thay B-owned guard nếu chưa reassignment | A-S1-01/02/03 read-only; A-S7-01 audit/import |
| **S1 A-S1-06** (live Station Tracking/Business E2E) | **Hoàng V1-B** | `B2A-S1-ACCEPTED-EVENT`: real acceptance/custody/tracking/receipt/audit, Personal+Business, source fingerprint và PG18/HTTPS runs | **EVIDENCE + consumer ACK** để nghiệm thu; contract-only để viết adapter/unit tests | A-S7-01, A-S5-01/02 contract, A-S6-02 |
| **S2 A-S2-01** (công bố Orders facts cho Network) | **Hoàng V1-B** | Yêu cầu DTO/interface/eligibility consumer needs, Org/facility/site scope, expected tests; kiểm tra port cũ | **CONTRACT + single writer** trước thay API | A-S7-02 và A-S3-02 snapshot regressions |
| **S2 A-S2-02** (Inventory/Sorting/Manifest status) | **Hoàng V1-B** | `B2A-S2/S3-EVENTS` level CONTRACT rồi EVIDENCE: event ID, Parcel/Shipment, holder, version, site, approved reader | **CONTRACT** trước wiring; **EVIDENCE** trước live E2E PASS | A-S7-01, A-S6-02/03 |
| **S3 A-S3-01** (Transfer/Hub Tracking) | **Hoàng V1-B** | Manifest/Transfer/Hub Receiving events, ordered event timeline, approved scoped reader và tests | **CONTRACT** trước wiring; **EVIDENCE** trước live tracking PASS | A-S3-02 snapshot, A-S7-02 |
| **S4 A-S4-01/02** (Attempt/POD/Delivered) | **Triết V1-C** | `C2A-S4-POD-FACTS`: DeliveryRun/attempt/POD schema, actor/Parcel provenance, privacy permission, genuine deliveries | **CONTRACT** trước scoped consumer; **EVIDENCE** trước DELIVERED/UI E2E PASS | A-S5-01/02, A-S7-01 |
| **S5 A-S5-01/02/03/04** (Customer Pickup contract/API for Courier) | **Triết V1-C**; **Hoàng V1-B** nhận thông tin hội tụ vào Station | Gửi sớm `A2C-S5-PICKUP-REQUEST` proposed shape, owner/coverage/address/window/fees, cancel/versions; hỏi Triết về consumer constraints và Hoàng về Station receiving | **CONTRACT coordination/approval** trước thay public shared DTO; **không chờ Triết Pickup runtime** để triển khai Nam Request | A-S7-01, A-S6-02; test request scope độc lập |
| **S5 Pickup end-to-end / A-S8-02** | **Triết V1-C + Hoàng V1-B** | Triết actual Courier Pickup collection/custody, Hoàng Station ACK/acceptance cùng Parcel IDs | **EVIDENCE** trước gọi Pickup E2E PASS | A-S7-02/03, A-S8-01 matrix |
| **S6 A-S6-01** (failed/return tracking) | **Triết V1-C + Hoàng V1-B** | Triết actual failure/retry/return decision; Hoàng reverse custody/Hub/Station events, reason codes và versions | **CONTRACT** trước customer adapter; **EVIDENCE** trước exceptions E2E PASS | A-S6-02/03 Claims core |
| **S6 A-S6-02; S7 A-S7-05** (Claims linked to real POD) | **Triết V1-C** | Authorized Attempt/POD proof references, approved privacy/retention reader | **CONTRACT** trước proof viewer; **EVIDENCE** trước linked-POD test | Reuse existing claim intake/review/closure |
| **S7 A-S7-04/06** (reconciliation/final finance UI) | **Hoàng V1-B + Triết V1-C** | `B2A-S7-NETWORK-FACTS` and `C2A-S7-FINANCE-EVIDENCE`: immutable fee-relevant physical events; separate Finance receipt/payment authority | **CONTRACT + approval** trước finance event consumer; **EVIDENCE** và real Finance evidence trước reconcile PASS | A-S7-01/02/03, obligation UI only |
| **S8 A-S8-02/03/05** (four journeys / final GO) | **Hoàng V1-B + Triết V1-C** | Signed Network custody + Courier/Delivery/POD evidence on same accessible source revision, S1–S7 critical gates | **EVIDENCE + joint ACK** trước S8 signoff; cả ba Co-Owners cùng quyết định GO/NO-GO | A-S8-01/04 test matrix/evidence register |

**Outbound notices Nam phải chủ động gửi ngay T1 (29/11) khi thấy dependency sẽ cần trong 1–2 ngày tới:** Hoàng về `A2B-S1-BOOKED-CARGO`, S2 Parcel reader và lịch source-lock BIA/V48; Triết về `A2C-S5-PICKUP-REQUEST`, Contract Request và dữ liệu customer-facing POD/finance. Việc gửi notice **không tự chuyển thành ACK**.


### V.3. UI/API/TEST WORK PACKAGES — NAM (V1-A)

Bảng này **bổ sung cách nghiệm thu giao diện cho Task IDs V.1**, không giao Nam code Ops/Courier của Hoàng/Triết. Nếu source UI đang có, đọc và reuse; nếu chưa có, ghi gap + authorization. Chỉ được dùng fixture cho unit/component tests; `FULL_STACK_PASS` cần backend/PG18 và giao diện thật.

| Slice / Task | UI/actor và hành động Nam chịu trách nhiệm | Backend / integration gate | Điều kiện nghiệm thu V1-A |
|---|---|---|---|
| S1 `A-S1-01/03` | Personal đăng nhập, tạo Quote, xem phí và confirmation, Booking 1–5 Parcel theo pilot, xem/in label, details/status | Identity/Orders real quote/book/label; ownerType PERSONAL + exact price/UNPAID | HTTPS customer UI→PG18 Booking/Parcel; validation, retry/idempotency, wrong owner denied |
| S1 `A-S1-02` | Business application nộp/sửa hồ sơ; Admin độc lập xem queue, yêu cầu bổ sung, approve/reject; Merchant xem activation + permissions | Merchant/Identity approval, explicit operator shipping grant/tariff (không auto grant) | Real reviewer actor; ACTIVE Org và owner/facility rights; no self approval; UI truthfully shows pending |
| S1 `A-S1-04/06` | Business quote/book/label/detail/list; trạng thái `WAITING_FOR_HANDOVER`→`AT_STATION` từ Hoàng events | `CarrierCargo`, Management V2, Hoàng intake reader | Browser integration và read-back; không đổi immutable Booking snapshot |
| S2 `A-S2-01/02` | Customer Shipment/Parcel timeline cho sorting/manifest khi event thật đã đến | Orders owner public facts + Hoàng scoped event reader | Timeline event IDs/version, no fabricated stage/PII, empty/error states |
| S3 `A-S3-01/02` | Customer transfer/arrival timeline; giá gốc và obligations view bất biến | Hoàng Hub event integration | Hub real evidence → UI đúng thứ tự; snapshot unchanged |
| S4 `A-S4-01/02` | Customer delivery status, failure reason theo quyền, POD view/link được cấp phép; Admin V1 oversight hẹp | Triết actual attempt/POD/Delivered read + privacy policy | No leaked proof; real UI reload after attempt; wrong-org denied |
| S5 `A-S5-01/04` | Personal+Business chọn Drop-off/Pickup khi Booking, Pickup Request form, address/window, cancel/reschedule, history + feedback | Canonical Booking/Parcel + approved Customer Pickup API; Triết consumes | Both owner types UI→backend request; rejected coverage, duplicate/canceled request; không tự ghi Courier collection |
| S6 `A-S6-01/03` | Customer exception/return view và claim form; CSKH authorized ticket queue, detail, supplement/review/response/close | Reuse existing CSKH; Hoàng/Triết actual exception/proof readers | UI↔Backend ticket lifecycle with audit, real Shipment association, privacy |
| S7 `A-S7-01/02` | Excel/CSV upload, row-level validation/grid, preview, explicit Quote/Booking confirmation, downloadable labels; fee summary | Existing worker/import/Draft; Orders booking and Finance obligation | Multi-row valid→Booked via UI+PG18; invalid stays unbooked, replay no duplicates |
| S7 `A-S7-03/06` | Finance/Business & Finance portal ledger-like obligation/receipt/reconcile list/detail and authorized actions for **approved payment method only** | Finance-owned posting/evidence; Hoàng/Triết immutable events | No UI can select PAID without receipt; payment+reconciliation test; unsupported shows unavailable |
| S8 `A-S8-01/05` | Orchestrate Customer/Admin/Finance/CSKH browser scripts for all four E2E; sign-off on current integrated revision | Hoàng + Triết integration and full stack gates | Four journeys with tracked UI actions, run IDs and Owner GO/NO-GO |

**Nam gửi UI expectations sớm:** cho Hoàng: customer event fields/`AT_STATION` readers; cho Triết: required POD metadata/notification and Pickup Request UX→API. Không xây UI Operations/Courier để lấp handoff đang chờ.

## VI. Handoff bắt buộc của Nam

| Handoff ID | Đối tác | Contract/output | Gate |
|---|---|---|---|
| `A2B-S1-BOOKED-CARGO` | Nam → Hoàng | Genuine Orders Booking/CarrierCargo/createdAt/Parcel set/owner/UNPAID | Approved contract + PG18 source evidence |
| `B2A-S1-ACCEPTED-EVENT` | Hoàng → Nam | Station acceptance/custody/tracking/receipt/audit actual event IDs | S1 atomic completeness & HTTPS/PG18 PASS |
| `A2C-S5-PICKUP-REQUEST` | Nam → Triết | Scoped Pickup Request + booking mode/coverage/policy/cancel/idempotency | CONTRACT/EVIDENCE gate per VI.1 |
| `B2A-S2/S3-EVENTS` | Hoàng → Nam | Inventory/manifest/Hub event reader/version for Customer projections | Actual network event + approved reader |
| `B2A-S7-NETWORK-FACTS` | Hoàng → Nam | Immutable physical custody/transfer/return financial-evidence refs | No finance DML |
| `B2C-S4-DISPATCH-READY` | Hoàng → Triết | Hub holder, dispatch eligibility, exact Parcel set and version | CONTRACT then physical EVIDENCE per VI.1 |
| `C2A-S4-POD-FACTS` | Triết → Nam | Authorized attempt/POD/Delivered reader with privacy | Real proof/security PASS |
| `C2A-S7-FINANCE-EVIDENCE` | Triết → Nam | Immutable delivery/collection evidence refs | No automatic payment state |

**Nam không được:** chạm `OperationalStationIntake`/custody của Hoàng hoặc Triết DeliveryRun/mobile khi không có reassignment; tự cấp quyền production; tự sửa `Identity` ACL; mở Admin Menu2 Step B; dùng SQL seed thay Merchant approval, Booking/Payment thật; chỉnh migration V1–V47 hay xóa WIP.


### VI.0. Mandatory `DEPENDENCY_NOTICE` BEFORE dependent coding — proactive protocol

> **Hướng dẫn vận hành áp dụng ngay cho Nam (V1-A), Hoàng (V1-B), Triết (V1-C).** Đọc **V.2 PRE-TASK DEPENDENCY MATRIX** của chính mình **trước khi chọn Task ID và trước khi yêu cầu Codex triển khai code**. Bảng V.2 làm rõ *khi nào phải thông báo đối tác*, không thay thế `VI.1` (hạn bàn giao), `VI.2` (trạng thái) hay approved technical contract.

1. **Preflight task:** Từ source hiện tại, xem Task ID có dòng ở V.2 và kiểm tra handoff registry VI.1. Nếu chỉ audit/thiết kế/test mock contract **không chạm nguồn shared và không công bố E2E**, vẫn làm độc lập; không chờ để bắt đầu. Nếu task cần API/event/schema/security/physical custody của thành viên khác, chuyển sang bước 2 **trước** khi bắt đầu patch cho đoạn phụ thuộc.
2. **Notify proactively:** Tạo `DEPENDENCY_NOTICE` có `REQUESTING_MEMBER`, `PROVIDER_MEMBER`, `TASK_ID`, `HANDOFF_ID`, `NEEDED_LEVEL=CONTRACT|EVIDENCE|SOURCE_LOCK`, `EXACT_REQUEST`, `APPROVAL_AUTHORITY`, `TARGET_DEADLINE_ICT`, `NEXT_INDEPENDENT_TASK`. Trình cho thành viên cung cấp và Nam (Co-Owner) qua **kênh thực sự có thể chuyển giao**; cần bằng chứng đã gửi (`DELIVERED_TO` và thời điểm). Không chờ đến khi test fail hoặc sát deadline mới hỏi.
3. **Nếu Codex không có kênh nhắn trực tiếp tới người khác:** Codex **chỉ được tạo notice và thông báo người đang sử dụng Codex để chuyển tiếp** (qua team chat/Jira/issue/approved shared channel). **Không được nói 'đã thông báo' khi chưa thực sự gửi**, không giả định hai phiên Codex tự thấy nhau. Nếu chưa chuyển được, ghi `NOTICE_PREPARED_NOT_DELIVERED`; Owner/team member phải chuyển thủ công. Trong lúc đó làm `NEXT_INDEPENDENT_TASK`.
4. **Provider response:** Trong khung triage/không trì hoãn với critical path, người nhận trả một trong `EXISTING_REUSABLE_PACKET`, `CONTRACT_READY`, `EVIDENCE_READY`, `PENDING_APPROVAL`, `IN_PROGRESS_ETA`, `BLOCKED` và exact source revision/tests. Người nhận **không** tự nâng `CONTRACT_READY` thành `EVIDENCE_READY`; nếu cần thay đổi shared file, các Co-Owners có domain bị ảnh hưởng phải thống nhất một `CODE_WRITER` duy nhất.
5. **Consumer decision:** Chỉ bắt đầu viết đoạn integration phụ thuộc khi có approved interface/source-lock và actual accessible source; chỉ nghiệm thu physical E2E khi có source+tests+events thực tế và consumer ACK. Thiếu mức nào ghi `WAITING_FOR_HANDOFF` hoặc `CONTRACT_PENDING/BLOCKED_APPROVAL` đúng nguyên nhân, **không tự implement thay provider**. Task riêng trong cùng Slice có thể vẫn `IMPLEMENTING`.
6. **No idle:** Khi chưa đủ gate, cập nhật `WAITING_TASK_ID`, `MISSING_DEPENDENCY`, `NOTICE_STATUS`, `NEXT_INDEPENDENT_TASK`, và làm ngay việc độc lập có authority; nếu không có task hợp lệ, hoàn thiện read-only gap/test/contract audit và trình Co-Owners/domain reviewer liên quan quyết định phần nhỏ nhất. Báo rủi ro deadline đúng VI.2/VI.1.

**Mẫu thông báo trước khi code (Codex điền từ source thực tế):**

```text
DEPENDENCY_NOTICE
FROM: <Nam V1-A | Hoàng V1-B | Triết V1-C>
TO: <member provider, full name + ROLE_ID>
CC: Co-Owners của domain liên quan; integration steward (FYI)
SLICE_TASK: <S# / actual Task IDs>
HANDOFF_ID: <exact VI.1 ID or PROPOSED_NEEDS_OWNER_REGISTRY>
NEEDED_LEVEL: CONTRACT | EVIDENCE | SOURCE_LOCK
REQUIRED_DATA: <exact interface/event/table fields / proof and permission>
CURRENT_SOURCE: <module, branch/worktree HEAD, safe diff/fingerprint>
EXISTING_EVIDENCE: <test/run IDs or NOT_YET_AVAILABLE>
APPROVAL_STATUS: <approved reference or pending>
TARGET_DEADLINE_ICT: <from VI.1; do not invent a second schedule>
BLOCKS: <exact dependent integration / E2E gate>
NEXT_INDEPENDENT_TASK: <valid task ID, approved authority>
NOTICE_STATUS: PREPARED | DELIVERED | ACKNOWLEDGED | RESPONSE_BLOCKED
DELIVERY_PROOF: <channel/reference/time, or NOT_DELIVERED>
RESPONSE_REQUIRED: <contract/version, current evidence, exact blocker and ETA if possible>
```

**Nhận handoff:** kiểm tra `HANDOFF_PACKET` ở VI.3, exact revision accessible ở worktree, approval + regression evidence; trả `ACK_ACCEPTED` hoặc `ACK_REJECTED` có lý do. Không tự commit/push/merge để lấy code và không sửa chung một migration hay file với người khác. `ACK_ACCEPTED` chỉ có nghĩa dependency đó đã đủ điều kiện, **không phải Slice PASS**.

**Thông báo ngược cho consumer (provider-side):** Khi code của chính mình sẽ thay đổi public port/DTO/event mà người khác dùng, gửi `CHANGE_IMPACT_NOTICE` *trước* patch hoặc tối thiểu trước khi khóa contract: schema/version, backwards compatibility, affected consumer, expected tests, cần approval hay không. Điều này áp dụng kể cả task không cần upstream handoff.


### VI.1. Daily Cross-Member Handoff Targets — duy nhất 29/11–07/12

**Mốc theo ngày, không ép giờ.** `CONTRACT_READY` cho phép consumer code theo API/DTO đã approved, `INTEGRATION_READY` cần source cùng revision và smoke test, `EVIDENCE_READY` cần sự kiện thật + security tests. Không chờ cả Slice để giao contract. **29/11** rà soát/nhận lại S1 packet đã có, không yêu cầu tạo lại sự kiện hoặc sửa migration.

| Ngày mục tiêu | Handoff ID / level | Provider → Consumer | Bằng chứng/đầu ra tối thiểu |
|---|---|---|---|
| **29/11** | `TEAM-D0-BASELINE` → `TEAM-BASELINE-29NOV` (registry alias, không viết lại history) | Nam, Hoàng, Triết → shared register | Source lock, actual develop/Compose state, task audit; `DEVELOP_BASELINE_READY` chỉ khi clean clone thật PASS; S1 evidence fingerprint, Spec012 = PROPOSED nếu chưa review |
| **29/11** | `A2B-S1-BOOKED-CARGO` và `B2A-S1-ACCEPTED-EVENT` — verified reuse | Nam ↔ Hoàng | Existing canonical 2-Parcel Booking + Station events/receipt, V48/V49 facts, exact accessible source/ACK; không build lại S1 |
| **30/11** | `A2C-S5-PICKUP-REQUEST` — **CONTRACT** | Nam → Triết | Approved Pickup DTO, owner/Org, site/address/window, coverage, cancel/replay/error and policy |
| **30/11** | `B2C-S4-DISPATCH-READY` — **CONTRACT** | Hoàng → Triết | Approved read/handback DTO, holder/site/Parcel IDs, eligibility/version; no physical Hub E2E implied |
| **01/12** | `SPM-S2-CONTRACT` — review gate | Hoàng → affected Co-Owners/reviewer | Bounded Spec012 policy/permissions/API/DB/ACL review disposition; source/tests may start only if approval passed |
| **02/12** | `A2C-S5-PICKUP-REQUEST` — **INTEGRATION_READY** | Nam → Triết | Pickup Request source+UI/HTTPS/PG18 targeted test and owner-scoped response, or explicit blocker |
| **03/12** | `B2A-S2/S3-EVENTS` — **S2 contract/event subset** | Hoàng → Nam | Manifest/Sort read contract, real S2 events; S3 Transfer/Hub component remains pending |
| **04/12** | `B2A-S2/S3-EVENTS` — **S3 EVIDENCE**; `B2C-S4-DISPATCH-READY` — **EVIDENCE** | Hoàng → Nam/Triết | Real Manifest→Transfer→Hub Receiving, exactly one holder + eligible dispatch Parcel set; HTTPS/PG18/UI evidence |
| **05/12** | `C2A-S4-POD-FACTS`; `B2A-S7-NETWORK-FACTS`; `C2A-S7-FINANCE-EVIDENCE` | Triết/Hoàng → Nam | Genuine Attempt/POD/Delivery, immutable custody/fee-relevant facts; no fake payment/claims |
| **05–06/12** | `C2B-S5-PICKUP-HANDOVER` | Triết → Hoàng | Sender→Courier collection; Station receiving ACK + exact Parcel set; both Personal/Business |
| **06/12** | `C2B-S6-RETURN-HANDOVER`; `TEAM-S8-REHEARSAL` | Triết → Hoàng; all three | Failed/return policy, actual reverse custody; four-journey rehearsal + S6/S7 evidence on integrated revision |
| **07/12** | `TEAM-S8-REVALIDATION` → `TEAM-S8-SIGNOFF` → `TEAM-V1-GO-NOGO` | Nam + Hoàng + Triết | Same-revision final UI/Web/Mobile→HTTPS→PG18 tests, evidence and all three ACKs; GO only if mandatory gates PASS |

**Chậm input:** gửi `DEPENDENCY_NOTICE` ngay khi biết sẽ cần trong 1–2 ngày, chuyển `WAITING_FOR_HANDOFF` + `NEXT_INDEPENDENT_TASK` nếu input thiếu. Mốc bàn giao mới là target, không cấp authority, không cho phép một người tự sửa source của người kia. Tất cả handoff đã PASS sớm phải được ghi `REUSE_CURRENT_PASS` thay vì thực hiện lại.

### VI.2. Dependency/Handoff Status Protocol — bắt buộc cho mọi Codex

| Trạng thái | Khi nào dùng | Hành động bắt buộc |
|---|---|---|
| `NOT_STARTED` | Chưa audit hoặc chưa bắt đầu handoff | Audit existing source/evidence; không kết luận `MISSING` từ spec tên gọi |
| `DEPENDENCY_REQUESTED` | Đã tạo và **gửi thực tế** `DEPENDENCY_NOTICE` trước khi code phần phụ thuộc; chưa đủ gate | Ghi provider, required level, delivery proof, ngày dự kiến; tiếp tục independent work, không tự chỉnh provider source |
| `CONTRACT_PENDING` | Interface/authority cần thiết chưa được duyệt | Chỉ đọc, thiết kế, viết proposal hoặc unit test độc lập; không sửa unapproved API/schema/security |
| `IMPLEMENTING` | Writer có explicit authority và path lock | Code phần nhỏ nhất; test riêng; bảo toàn shared boundaries |
| `READY_FOR_HANDOFF` | Provider có exact approved contract + tests/evidence đủ với **mức** CONTRACT hoặc EVIDENCE được chỉ định | Gửi `HANDOFF_PACKET` gồm revision/fingerprint, interface/event IDs, run IDs và giới hạn; không tự chuyển consumer thành PASS |
| `CONSUMER_VERIFYING` | Consumer đã nhận packet, đang đối chiếu contract/revision/runtime | Verify permission, scope, idempotency, canonical IDs, status/evidence; ACK hoặc trả lỗi chính xác |
| `ACCEPTED` | Consumer xác nhận đúng source revision + contract + required evidence | Cho phép consumer **tích hợp phần được duyệt**; release/E2E PASS vẫn cần end-to-end proof |
| `WAITING_FOR_HANDOFF` | Task hiện tại không thể tích hợp vì thiếu một packet/gate từ thành viên khác | Không chạm private source của provider; đánh dấu `DEPENDENCY_ID`, tiếp tục `NEXT_INDEPENDENT_TASK` có authority; báo Co-Owners liên quan nếu ảnh hưởng critical path |
| `BLOCKED_APPROVAL` | Co-Owner có thẩm quyền hoặc reviewer bắt buộc từ chối/chưa duyệt scope mới | Dừng đúng change-set; lập minimal delta; không workaround bằng SQL, mock, privilege hay công cụ khác |
| `FAILED_VERIFICATION` | Consumer thấy contract mismatch, stale source, permission/test FAIL hoặc evidence không đầy đủ | Trả packet cho provider với failure evidence; reopen handoff; không ACK giả, không đánh PASS |

**Chuyển trạng thái đúng:** task độc lập `NOT_STARTED → IMPLEMENTING`; task phụ thuộc `NOT_STARTED → DEPENDENCY_REQUESTED → (CONTRACT_PENDING/WAITING_FOR_HANDOFF nếu cần) → IMPLEMENTING → READY_FOR_HANDOFF → CONSUMER_VERIFYING → ACCEPTED`. `WAITING_FOR_HANDOFF`, `BLOCKED_APPROVAL`, `FAILED_VERIFICATION` là nhánh điều kiện; chỉ thoát khi có approved contract, corrected fresh evidence và/hoặc ACK phù hợp. Task runtime vẫn có thể chạy độc lập ở `IMPLEMENTING` khi chỉ E2E handoff đang chờ.

**Phân biệt BLOCKED và WAITING:** `WAITING_FOR_HANDOFF` chỉ khi *phụ thuộc vào deliverable của thành viên khác*. Nếu thiếu quyền/contract của chính task → `BLOCKED_APPROVAL` hoặc `CONTRACT_PENDING`. Nếu tests thất bại → `FAILED_VERIFICATION` hoặc `PARTIAL`, không đổ cho thành viên khác.

**Không được chờ toàn bộ:** nếu bị chặn integration, Codex phải chọn theo thứ tự: (1) audit/reuse & existing approved tests, (2) triển khai module riêng có contract đã phê duyệt, (3) negative/unit/contract tests không mạo nhận physical events, (4) proposal minimal cho phần chưa được duyệt, (5) review handoff khác đã đến. Phải ghi `NEXT_INDEPENDENT_TASK` + lý do. Không đổi source owner hoặc cắt bỏ required V1 scope để tránh chờ.

**Thông báo blocker & escalation:** Ngay khi phát hiện S1/S2/S3/S4/S5 hard gate bị chặn, phát `BLOCKER_NOTICE` cho Co-Owners domain bị ảnh hưởng và provider/consumer liên quan: `ROLE_ID`, `TASK_ID`, `DEPENDENCY_ID`, trạng thái, ngày mục tiêu/mức độ ảnh hưởng, exact missing API/event/evidence, test/revision, expected next handoff, workaround **trong authority** và quyết định nhỏ nhất cần Owner. Nếu chưa giải quyết, nhắc ở triage **đầu ngày** và status **cuối ngày**; không trì hoãn thông báo tới cuối ngày. Owner chỉ định reviewer/writer, re-sequence task hoặc ra GO/NO-GO; Codex không tự ban authority mới.

**Cross-working-tree delivery:** mỗi thành viên giữ branch/worktree riêng; đóng gói handoff thành report/patch/contract refs trong namespace của người gửi và chuyển qua kênh phối hợp được Owner cho phép. Chỉ sau khi consumer thực sự có đúng revision (không chỉ một đường dẫn ở máy người khác) mới được ACK. Không tự push/merge/rebase/cherry-pick nếu chưa được phép; không đồng thời sửa shared file, migration registry, git index hoặc quyền DB.

### VI.3. `HANDOFF_PACKET` & tiêu chí ACK

```text
HANDOFF_ID: <exact registry ID in VI.1>
LEVEL: CONTRACT_READY | INTEGRATION_READY | EVIDENCE_READY
PROVIDER: V1-A | V1-B | V1-C
CONSUMER: V1-A | V1-B | V1-C
TASK_IDS: <slice + task IDs>
STATUS: READY_FOR_HANDOFF | CONSUMER_VERIFYING | ACCEPTED | WAITING_FOR_HANDOFF | FAILED_VERIFICATION
CONTRACT_AUTHORITY: <approved document/version/hash OR BLOCKED_APPROVAL>
CANONICAL_SOURCE: <module/port/table + owner + exact changed files>
SOURCE_REVISION: <worktree/branch/HEAD + diff hash or reproducible fingerprint>
COMPATIBILITY: <Personal/Business, Drop-off/Pickup, legacy behavior>
SCHEMA_SECURITY: <migration/ACL/roles/ownership, if applicable>
EVENT_BINDING: <Shipment/Parcel IDs, ownerType, Org/facility/site, actor, event/receipt refs; sanitized>
TEST_EVIDENCE: <PG18/HTTPS/Mobile/UI command + run IDs + positive/negative result>
DEPENDENCY: <upstream/downstream handoff IDs; any missing gate>
CONSUMER_CHECK: <matched contract/version + source accessible + tests, or exact rejection>
ACK_BY: <ROLE_ID; timestamp ICT; ACCEPTED/REJECTED; evidence ref>
NEXT_ACTION: <integration step or independent task>
```

Consumer **chỉ ACK `ACCEPTED`** khi đúng approved contract/version, có source revision có thể truy cập và bằng chứng **đúng level** (CONTRACT hoặc EVIDENCE); những dữ liệu thiếu, stale hay synthetic fixture phải trả `FAILED_VERIFICATION`. Một packet EVIDENCE_READY phải có test thực và source event refs, không chỉ contract document hoặc screenshot. Đối với `TEAM-S8-SIGNOFF`, mỗi người ký đúng domain mình và cả ba Co-Owners cùng quyết định GO/NO-GO.

### VI.4. Quy trình nhanh khi bị chặn — minh họa bắt buộc

- **Nam thiếu `B2A-S1-ACCEPTED-EVENT`:** chỉ chặn phần Customer real `AT_STATION` và E2E S1-B; Nam tiếp tục Bulk Booking, Pickup Request, approved Finance/Claims; không tự tạo acceptance.
- **Hoàng thiếu `A2B-S1-BOOKED-CARGO`:** không tích hợp Business Intake bằng fabricated owner/createdAt; Hoàng tiếp tục approved inventory/manifest core và site security tests; không tự sửa Orders private SQL.
- **Triết thiếu `B2C-S4-DISPATCH-READY` EVIDENCE:** tiếp tục approved Courier auth/task UI/DeliveryRun contract tests; không đánh `DELIVERED`, không ghi POD E2E cho hàng chưa Hub-received.
- **Triết thiếu `A2C-S5-PICKUP-REQUEST`:** tiếp tục phần Courier/Delivery độc lập; không dựng endpoint yêu cầu Pickup thứ hai.
- **Hoàng thiếu `C2B-S5-PICKUP-HANDOVER`:** tiếp tục hub/manifest work; không dùng giả lập Courier pickup để tuyên bố Station receiving PASS.
- **Nam thiếu `C2A-S4-POD-FACTS` hoặc S7 immutable facts:** tiếp tục Customer UI contract và Finance reconciliation tests hợp lệ; không tự đánh `PAID`, `DELIVERED` hoặc tính doanh thu từ UNPAID.

### VI.5. INTEGRATION READINESS GATE — packet sớm, không chờ full Slice

**Thêm cấp độ bắt buộc `INTEGRATION_READY` giữa CONTRACT và EVIDENCE:** `CONTRACT_READY → INTEGRATION_READY → EVIDENCE_READY`. Chỉ một contract đã phê duyệt **chưa đủ** nếu consumer chưa có chính source revision và chưa test được adapter. Handoff nào không có runtime vì upstream chưa PASS thì `CONTRACT_READY`/`UI_PROTOTYPE_READY`, **không** phải `INTEGRATION_READY` hoặc `EVIDENCE_READY`.

| Level | Provider phải cung cấp | Consumer phải xác nhận | Được phép sau gate |
|---|---|---|---|
| `CONTRACT_READY` | Approved signature/version, actor/owner/site, DTO, errors, idempotency, event, schema/security impact, sample fixture only for unit tests | Consumer review/contract tests và owner/writer lock | Code độc lập theo contract trong authority; không E2E claim |
| `INTEGRATION_READY` | Approved implementation, actual source patch/revision được chuyển **thực sự**, API running hoặc test harness phù hợp, forward compatibility + minimal integration run | Consumer đã nhận revision trong environment cho phép, xây/run adapter và smoke/contract tests PASS; ACK source hash | Tích hợp phần consumer và test boundary, chưa claim physical handover nếu chưa có evidence |
| `EVIDENCE_READY` | Actual business event/receipt, actor/site/owner/Parcel and PG18+HTTPS/UI/Mobile run IDs, negative tests/rollback, source fingerprint | Consumer đọc event từ approved port, tự chạy source-joined test, ACK tested revision | Đề xuất E2E acceptance cho đúng dependency, không tự tuyên bố cả Slice PASS |

**Pre-task short-circuit:** Kiểm tra provider output sớm **ngay khi xác định dùng trong 1–2 ngày tới**, dù chưa đến deadline VI.1. Gửi `DEPENDENCY_NOTICE` level `CONTRACT` trước khi thiết kế consumer-specific adapter; nếu task cần runtime integration thì yêu cầu `INTEGRATION_READY`; nếu cần claim physical/finance outcome thì yêu cầu `EVIDENCE_READY`. Hỏi đúng *smallest deliverable* (ví dụ reader/DTO + test) thay vì đợi cả S3/S4 hoàn thành. Provider trả reply có xác minh, không chỉ ETA.

**Handoff contract/UI cross-review:** Trước khi một actor-facing UI gửi command sang domain khác, provider và consumer phải đồng thuận: `UI_ACTION`, request/response/errors, permission/audience, source-of-truth, consistency/read-back, cancellation/stale/version, handler writer, optimistic lock, UI loading/error/empty/deny behavior, E2E test. UI provider không được gọi private SQL để lấy dữ liệu thay cho approved reader.

**Integration-ready checklist:** `[ ]` approved contract; `[ ]` changed paths/sole writer listed; `[ ]` provider revision accessible; `[ ]` no version/ACL break; `[ ]` nonmock API integration test PASS; `[ ]` negative owner/site/invalid state test PASS; `[ ]` consumer ACK; `[ ]` real event evidence status explicitly reported; `[ ]` frontend/mobile wiring task owner and screen route named. *Source-only packet từ máy khác không tự biến thành integrated source.*

**Packet mở rộng:** bổ sung vào `HANDOFF_PACKET` hiện tại: `LEVEL: CONTRACT_READY|INTEGRATION_READY|EVIDENCE_READY`, `ACTOR_UI_ROUTE`, `UI_ACTION`, `API_COMMAND_OR_QUERY`, `PROVIDER_IMPLEMENTATION_HASH`, `SOURCE_DELIVERY_CHANNEL/PROOF`, `CONSUMER_WORKTREE_HASH`, `CONSUMER_SMOKE_RUN`, `FULL_STACK_GATE`, `REAL_EVENT_REF_OR_NOT_READY`, `NEXT_INDEPENDENT_TASK`. ACK riêng theo level. Nếu chưa có git-publication approval, transfer bằng source bundle/patch được Owner cho phép và thẩm tra; **không** tự commit/push/merge/cherry-pick để đạt gate.

**BIA-V1/V48 temporary lock:** Cho đến khi Nam bàn giao xong active BIA-DB-GUARD-01 code writer assignment, Hoàng chỉ *audit/contract/test-plan* trên đúng V48/shared guards; Triết không sửa BIA/Orders Tracking. Sau đó hai Co-Owners domain liên quan bàn giao lock từng path, không chuyển quyền bằng lời hứa chung, và cả ba đối chiếu fingerprint trước khi sửa.

## VII. FAST-TRACK V1 — T1–T9 và quy trình GO/NO-GO

**Timebox mới duy nhất:** từ **Chủ Nhật 29/11/2026 (T1)** đến **Thứ Hai 07/12/2026 (T9)**, deadline mục tiêu GO/NO-GO **trong ngày 07/12** (không có giờ cứng). S1 đã có SOURCE/TEST PASS 27/11; task S1 trong kế hoạch mới chỉ là **handoff, reuse, baseline/regression**, không tái triển khai. Deadline mới là target có điều kiện và cần ACK của ba Co-Owners theo L2/L3 release governance; nếu chưa đủ acceptance thì báo PARTIAL/NO-GO.

### VII.1. Quy tắc tăng tốc không đánh đổi chất lượng

- **Không rebuild** Identity/Booking/labels/NWS intake, Business management, Excel Draft importer, Finance obligation/CSKH nếu source+evidence xác minh có thể reuse. Thứ tự ưu tiên: `reuse → integrate → smallest missing behavior → test`.
- **Ba worktree/branch riêng**, không song song sửa cùng file, SQL migration, shared HTTP bootstrap, auth/security/config hoặc file mô tả chung. Các Co-Owners domain liên quan khóa shared paths/migration cho **một writer một thời điểm**; người khác triển khai phần độc lập trên interface đã được chốt.
- **T1 (29/11) gap+contract review đồng thời**: Hoàng trình Spec012/013, Triết trình Spec014/015/029 và pickup/return, Nam trình Pickup booking/request và exact supported Finance method, chỉ cho phần thật sự thiếu sau audit. Gom quyết định cần đồng thuận Co-Owners theo đúng affected domains vào một checkpoint tổng hợp nhưng giữ từng domain đúng authority; không gộp tất cả thành giấy phép chung.
- **Hard dependencies:** Hoàng không Manifest movement trước valid acceptance; Triết không DeliveryRun từ manifest chưa Hub received/custody-handover; Pickup từ Triết chỉ vào Station khi Hoàng xác nhận; Nam không đánh `PAID` hoặc `DELIVERED` từ UI hay Booking. Real login/session, canonical Shipment/Parcel/custody/tracking, idempotency, auth/scope và rollback không được cắt bỏ.
- **Bằng chứng**: PG18/test backend thật, HTTPS Browser tích hợp và Courier app chạy với backend thật (thiết bị/emulator nếu có), catalog/ACL và negative tests liên quan. Unit/fixture/demo/UI screenshot **không** thay thế E2E. Đợt test dùng mạng tuyến mô phỏng SOURCE/TEST; không phải UAT/Production.
- **Triage hàng ngày:** đầu ngày trao đổi ngắn thống nhất blockers/lock/contracts, cuối ngày mỗi role gửi status + evidence + handoff; các Co-Owners của domain liên quan xử lý decision queue trong ngày. Đổi timeline nếu critical path bị chặn; không giả làm đã approved bằng cách sửa nhãn tài liệu.

### VII.2. Checkpoints toàn đội — lịch mới 29/11–07/12

| Ngày | Outcome ưu tiên và Slice liên quan | Nếu thiếu approval/evidence |
|---|---|---|
| **T1 — 29/11** | `DEVELOP_BASELINE_READY` audit/clean clone path; S1 existing acceptance verified and handed off; Hoàng Spec012 review, Triết Mobile audit, Nam Bulk/Pickup contract | Không claim Docker/develop READY khi chưa xác minh; làm independent tasks, giữ locks |
| **T2 — 30/11** | S1 baseline regression target; Pickup Request + Dispatch Eligibility contract handoffs; Spec012/013/014/015/029 review queue | Contract pending → proposal/tests/UI skeleton không viết runtime chưa approved |
| **T3 — 01/12** | S2 Inventory/Sorting/Manifest implementation và Operations UI; Nam Pickup/Excel; Triết DeliveryRun/Mobile Foundation | Integration smoke hợp lệ; không claim full S2 trước real scan/PG18 |
| **T4 — 02/12** | S2 full-stack hardening, A→Triết Pickup Request source; S3 Hub and S4 Courier development song song | Dùng P2 approved tasks nếu blocked; alert S2 target risk |
| **T5 — 03/12** | **S2 target FULL-STACK PASS**; S3 real Transfer/Hub underway; Triết Dispatch Web+Courier Mobile/Attempt, Nam tracking + Finance | Nếu Spec012/PG18/UI FAIL → S2 PARTIAL; S3/S4 E2E không tự nhận PASS |
| **T6 — 04/12** | **S3 target FULL-STACK PASS** + `B2C` actual eligibility; S4 integrated courier handover, S5 Station Pickup contract | Hub E2E mất gate → Triết tập trung UI/contract tests; no fake Delivered |
| **T7 — 05/12** | **S4 target FULL-STACK PASS** Personal/Business Drop-off Delivery/POD; Nam Finance/Claim consume events; Triết Pickup collection | No POD/privacy/proof → S4 PARTIAL; S5 still can complete preparatory work |
| **T8 — 06/12** | **S5/S6/S7 target FULL-STACK PASS**; 4 E2E rehearsal (both Pickup/Drop-off); return and payment/claims negative gates | Any missing event/approved settlement → PARTIAL; create minimal critical fixes |
| **T9 — 07/12** | **S8 target FULL-STACK PASS / joint GO-NO-GO**; same-revision final regression, all three signoff | If any mandatory gate FAIL → PARTIAL/NO-GO, no retroactive PASS |

**Ưu tiên trong ngày:** `P0 (unblock someone) → P1 (own full-stack task) → P2 (independent fallback)`. Không ấn định giờ; gửi packet khi đạt đúng level. Các target đã hoàn thành sớm phải đóng status bằng evidence, không phải chờ đến đúng ngày mới làm task tiếp.

### VII.3. Definition of Done cấp release (không giảm chuẩn vì gấp)

1. Cả `V1-E2E-P-DROP`, `V1-E2E-B-DROP`, `V1-E2E-P-PICK`, `V1-E2E-B-PICK` chạy **Booking → actual custody → Station → Inventory → Manifest → Transfer/Hub receive → Courier attempt/POD → DELIVERED** qua genuine scoped actors; Pickup có handover Courier→Station. Chỉ chọn tuyến được môi trường thử nghiệm hỗ trợ.
2. Business independent approval/activation + explicit shipping grants/contract; Personal không cần Business. Correct ownerType/Org/facility, tariff/quote/booking/Parcel IDs, tracking and UNPAID/approved payment evidence; no fake PAID/COD or customer-created operator event.
3. Mỗi slice có happy-path + negative security/site/owner/parcel/replay/race/stale/rollback/partial failure theo phạm vi. S6 proof of failure/reattempt/return; S7 Excel valid Draft→actual Booking, one supported approved payment+reconciliation model, linked CSKH/Claims as scoped; no blanket feature waiver.
4. Current revision PG18+HTTPS+Mobile/UI evidence, migration cold/upgrade checksum/owner/ACL, architecture interfaces, data isolation; bảo toàn WIP và lịch sử. Không deploy/AWS IAM/S3/real grants; Git commit/push/merge chỉ theo permissions + AGENTS/CI + quyết định Co-Owner tương ứng, không cần xin Nam riêng.
5. Integration steward tổng hợp `S1…S8` và 3 Co-Owner signoffs có run IDs, source fingerprint, exact contract authority, blocker/residual risk. Không một Co-Owner hoặc tài liệu nào đơn phương đặt trạng thái full V1 accepted.

### VII.4. Báo cáo cuối mỗi ngày (Codex phải trả lời đúng role)

```text
DATE (ICT): YYYY-MM-DD   ROLE_ID: V1-...
CURRENT_GIT: branch / HEAD / worktree / index state
SOURCE_AUDIT_DELTA: những phần tìm thấy đã có; reuse vs new, source paths
TASK_PROGRESS: task IDs [REUSE_CURRENT_PASS | IN_PROGRESS | BLOCKED_APPROVAL | PASS | ...]
CHANGED_FILES: path → 1 CODE_WRITER → exact authority
TEST_EVIDENCE: command/run ID/PG18/HTTPS/UI/Mobile/result/source fingerprint
CROSS_ROLE_HANDOFF: HANDOFF_ID → level (CONTRACT/EVIDENCE) → provider/consumer → status/ACK/timestamp
WAITING_FOR_HANDOFF: dependency ID, ngày dự kiến, exact missing proof, impact
NEXT_INDEPENDENT_TASK: task ID + approved authority (không idle khi có việc độc lập)
ESCALATION: thời điểm thông báo / affected Co-Owner decision / overdue flag
BLOCKERS: reason, affected Co-Owners/reviewer, smallest required decision, critical-path impact
NEXT_24H: prioritized actions within authority
DEADLINE_RISK: GREEN | AMBER | RED with explanation
DO_NOT_CLAIM: undone slices/untested flows/production status
```

**PROMPT khởi động mỗi Codex:** `Tôi giao ROLE_ID=V1-... đúng file này. Bắt đầu T1 bằng read-only source/evidence audit, lập reuse/gap register cho task IDs của mình và kế hoạch tới ngày mục tiêu 07/12/2026. KHÔNG build lại code đã có, không edit chưa đủ approval. Sau đó triển khai smallest approved critical-path task, bắt buộc tuân thủ V.2 + VI.0–VI.4 (DEPENDENCY_NOTICE trước code phụ thuộc, handoff deadline + ACK), report theo VII.4.`


### VII.5. TEAM EXECUTION STRATEGY — T1–T9, giảm chờ và conflict

**Contract-first → smallest unblocker → parallel domain work → daily vertical integration → 4-journey proof.** Giữ một writer cho mỗi change-set, 1 runtime WIP + 1 independent contract/audit WIP cho mỗi Codex. Không chờ hoàn thành hết S2 mới code Courier UI theo approved contract; không dùng mock event để claim E2E. Mọi CODE_WRITER/ACL/migration/shared DTO phải theo Co-Owner affected-domain agreement và technical/security review.

| Ngày | Ưu tiên bàn giao mở khóa | Full-Stack integration cần thử | Fallback nếu blocked |
|---|---|---|---|
| **29/11** | Nam S1 handoff và baseline; Hoàng Spec012 review; Nam/Triết Pickup/Delivery DTO proposal | Clean clone `develop`+Compose **chỉ khi thực sự published**; reuse S1 evidence | Audits, UI gap, migration/schema read-only, Mobile foundation |
| **30/11** | Nam→Triết Pickup CONTRACT, Hoàng→Triết Dispatch CONTRACT | S1 baseline regression, Courier mobile auth harness, S2 contract tests | Bulk import, Manifest UI contract, POD proof policy |
| **01/12** | Spec012 approval disposition/lock; shared UI/API wiring for S2 | Inventory→Sort→Draft real UI/HTTPS/PG18 smallest vertical | Hub contract, DeliveryRun, Customer Pickup UI tests |
| **02/12** | Nam→Triết Pickup SOURCE, Hoàng→Triết station/Hub readiness API | Manifest Seal/negatives and Pickup Request/DeliveryRun smoke | CSKH, mobile screens, Transfer test design |
| **03/12** | Hoàng S2 Event reader/manifest packet to Nam; Hoàng Spec013 handover | S2 complete UI+Backend+PG18; S3 Transfer vertical preview | Courier consumer contract tests, Finance method proposal |
| **04/12** | Hoàng actual Hub→Triết, Hub events→Nam | S3 Hub Receiving E2E; Courier accept from real holder | S5 Station Receiving, POD UI tests |
| **05/12** | Triết POD facts→Nam; Network/Courier fee facts→Nam | S4 Dispatch Web→Courier Mobile→POD→Delivered on real holder | Return/pickup tests, Claims/Finance integration |
| **06/12** | Triết Pickup handover→Hoàng; Return→Hoàng; S7 facts→Nam | S5 Pickup/Station→downstream, S6 failure/return, S7 payment/claims UI; 4 scenarios rehearsal | Hard blockers only + regression evidence |
| **07/12** | Same revision signed by Nam/Hoàng/Triết | Four journeys + negative/security/finance/UX final tests; team GO/NO-GO | No-GO with exact blockers, recovery ownership |

**Baseline limitation:** `S1 SOURCE/TEST PASS` trong báo cáo 27/11 **không tự chứng minh `develop` đã merge hay Docker Compose clean clone PASS**. Đây là prerequisite cho distributed development; không cản mọi task độc lập. Spec012 station preparation proposal chưa là approval. Shared files hoặc migration numbering chỉ có một CODE_WRITER per revision; một Co-Owner không thể tự bypass automatic review. Không tự push/merge/deploy nếu thiếu quyền Git/CI, dù release intent đã được bàn giao.

**Single source of truth:** dùng lịch mới `ArgoPost_V1_Team_Execution_Calendar_2026-11-29_to_2026-12-07.md`; lịch 28/11–05/12 không còn active. Báo cáo cuối ngày gồm `TASK_ID | REAL_STATUS | UI | API | PG18 | SECURITY | SOURCE_REVISION | HANDOFF_LEVEL/ACK | NEXT_INDEPENDENT_TASK | RISK_TO_TARGET`.

### VII.A — Kế hoạch 9 ngày của Nam (Customer & Commercial)

**Cách dùng:** đọc đúng hàng ngày khi bắt đầu; ưu tiên **P0 mở khóa thành viên khác**, sau đó **P1 Full-Stack own domain**; nếu bị chặn thì **P2 độc lập**. Task IDs là tập nhiệm vụ cần đối chiếu source, không bắt buộc viết lại mọi task. Giữ đơn một writer cho mỗi migration/shared path. Mọi target dưới đây là forecast, không phải số giờ cố định.

| Ngày | P0 mở khóa + P1 Full-Stack chính (`TASK_ID`) | P2 nếu thiếu handoff/approval | Output kiểm chứng |
|---|---|---|---|
| **29/11/2026** | `A-S1-04/05/06, A-S8-01` — Verify S1 27/11 evidence, V48/V49 and baseline Docker/develop state; handoff canonical Booking/Parcel/custody readers to Hoàng; send early Pickup DTO notice Triết | A-S7-01 Bulk Draft audit, A-S7-02 obligations, A-S6-02 Claims audit | S1 HANDOFF packet + baseline status (do not claim merge unless proven); task/contract gap register |
| **30/11/2026** | `A-S5-01/02/03, A-S7-01` — Approve/chốt Pickup Request contract with Triết; reconcile UI mode/coverage/cancel/idempotency; S1 clean clone regression when baseline ready | Bulk Import UI→Draft→Quote→Booking/Label code/tests; A-S7-03 finance policy proposal | A2C Pickup CONTRACT; S1 baseline regression or blocker |
| **01/12/2026** | `A-S5-01/02/03/04, A-S7-01` — Implement approved Personal/Business Pickup Request Web + Backend/PG18; complete valid Excel/CSV→actual Booking integration | A-S3-02 immutable pricing, A-S6-02/03 claims lifecycle | Pickup Request/Bulk feature tests and source version |
| **02/12/2026** | `A-S5-01/04, A-S2-01, A-S7-02` — Deliver A2C Pickup SOURCE/EVIDENCE level available; real scoped owner/Org admission and denial; ensure Hoàng sees Parcel reader contract | A-S7-03/04 payment/reconciliation approval + code only if authorized | Pickup request integration packet; no fake collection/PAID |
| **03/12/2026** | `A-S2-02, A-S7-01/05, A-S8-01` — Consume S2 sorting/manifest reader; Customer/Business Operations timeline Web; Bulk negative/duplicate; CSKH claims UI | A-S5-04 Pickup UX error states, A-S7-02 financial obligations | S2 read ACK when genuine; tracking/claim/fee test evidence |
| **04/12/2026** | `A-S3-01, A-S7-03/04/06, A-S8-02` — Consume actual Hub/Transfer events, complete customer tracking; Finance supported payment + reconciliation implementation only under approved contract | A-S6-01/02 Claims association, A-S8-01 E2E harness | Hub event ACK + source UI readback; finance verified method status |
| **05/12/2026** | `A-S4-01/02, A-S6-01/02/03, A-S7-04/05` — Receive genuine Courier POD; customer Delivered tracking, claim-linked attempt; Finance event binding | A-S7-01 Bulk retry/negative UI, A-S8-02/03 browser tests | C2A POD ACK; Claim/Finance UI/backend evidence or exact blocker |
| **06/12/2026** | `A-S5-01/04, A-S7-01/06, A-S8-01/03` — Personal/Business Pickup end-to-end customer UI; finish supported payment/reconciliation, import booking, CSKH/Claims evidence; S8 rehearsal | A-S6-02/03 targeted auth/security/regression | S7 target complete + S5 UI ACK + 4 scenario rehearsal |
| **07/12/2026** | `A-S8-01/02/03/04/05` — Joint 4 journey final Web/Backend/PG18 regression, compare all role evidence and sign decision | Only authorized critical repairs and targeted re-tests | Nam signoff; all 3 joint TEAM-V1-GO-NOGO or PARTIAL/NO-GO |

**Role scope:** Nam chỉ sửa source trong domain Customer & Commercial hoặc exact approved shared change-set có CODE_WRITER. Nam, Hoàng, Triết cùng quyết định S8 trên một source revision; không một người tự GO thay cả nhóm.
