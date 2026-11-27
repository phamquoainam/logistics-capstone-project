# ARGOPOST V1 — HOÀNG — Carrier Network & Custody

> **V1.5 — Tri-Co-Owner governance, full-stack acceptance & integration readiness.** Work assignment độc lập cho Codex của Hoàng. Đọc toàn bộ file trước khi lập kế hoạch hoặc sửa source. Không dùng file của thành viên khác để tự nhận thêm quyền.

---

| Thuộc tính | Giá trị |
|---|---|
| `ROLE_ID` | **V1-B** |
| Thành viên | **Hoàng — Co-Owner (V1-B)** |
| Vai trò chính | **Carrier Network & Custody** |
| Accountable cho | Carrier Operations Station→Inventory→Sorting→Manifest→Transfer→Hub; network exceptions |
| Các Slice chính | S1-intake, S2, S3, S5-station, S6-network, S8-network |
| Phiên bản tài liệu | `TEAM-ASSIGNMENT-v1.5 — TRI-CO-OWNER AUTHORITY` — 2026-11-27 |
| Trạng thái | `CO-OWNER PLANNING / WORK ASSIGNMENT — NOT TECHNICAL SECURITY APPROVAL` |
| Mục tiêu release | `V1_OPERATIONAL_E2E_ACCEPTED — SOURCE/TEST` (cả ba Co-Owners cùng nghiệm thu) |
| Deadline mục tiêu (không phải bảo đảm) | **05/12/2026** — GO/NO-GO theo evidence |

**SELF-IDENTIFICATION:** Khi nhận file này, Codex phải tự nhận `ROLE_ID=V1-B`, **không được** trả lời như thành viên khác. Nếu prompt muốn sửa module thuộc người khác, chỉ lập impact/contract request rồi handoff, trừ khi có explicit Owner assignment cho đúng change-set. Người được phân công file này là **Hoàng — Co-Owner phụ trách Carrier Network & Custody**.



**TEAM ROSTER — áp dụng cho cả ba Codex:** `V1-A = Nam (Co-Owner/Customer & Commercial)`; `V1-B = Hoàng (Co-Owner/Carrier Network & Custody)`; `V1-C = Triết (Co-Owner/Dispatch & Courier Delivery)`. **Tên thành viên là người phụ trách công việc, không phải runtime application role, không cấp quyền Git/database.** Mỗi Codex chỉ dùng ROLE_ID của file mình.

> **OWNER DECISION — `OWNER-V1-DEADLINE-2026-11-27`:** Nam (V1-A/Co-Owner) chấp thuận chuyển **deadline GO/NO-GO từ 04/12/2026 sang 05/12/2026**. Giữ nguyên yêu cầu S1–S8, bốn hành trình Personal/Business × Drop-off/Pickup, security, full-stack gates và các handoff đường găng 28/11–03/12. **D7 04/12** được dùng cho sửa lỗi/integration regression và `TEAM-S8-REVALIDATION`; **D8 05/12** là ngày ký nhận và ba Co-Owners cùng quyết định. Đây chỉ là thay đổi timeline; **không** là approval kỹ thuật mới hoặc xác nhận đã PASS.


**OWNER DECISION — FLEXIBLE DAILY COORDINATION:** Kế hoạch này chỉ ấn định **ngày mục tiêu, không ấn định giờ cụ thể** cho handoff, daily sync, smoke test, status report hoặc quyết định GO/NO-GO. Nam/Hoàng/Triết chủ động bàn giao ngay khi có approved contract/source/evidence. Nếu thiếu input, báo rõ ngay khi nhận ra rủi ro, chuyển sang `NEXT_INDEPENDENT_TASK`; ngày 05/12 cả ba xác nhận domain evidence trên một integrated revision trước quyết định GO/NO-GO chung. Quy tắc này thay thế các khung giờ cố định của bản trước, **không nới lỏng acceptance/security/approval**.

**FULL-STACK RELEASE RULE (v1.5):** Backend-only, UI-only hoặc contract-only không được ghi `FEATURE_FULL_STACK_PASS`/`SLICE_E2E_PASS`; xem III.3–III.5, V.3 và VI.5. `INTEGRATION_READY` là gate bổ sung, không thay thế EVIDENCE_READY.

**CO-OWNER IMPLEMENTATION RULE:** Mọi task trong domain mình đã giao và đã có technical authority được Codex thực hiện trực tiếp theo prompt hằng ngày; **không tạo checkpoint xin Nam phê duyệt**. Với các vấn đề cross-domain, hỏi đúng Co-Owner liên quan hoặc reviewer độc lập theo bảng Mục 0; nếu chưa xong, chuyển task độc lập.

**PRE-TASK RULE:** Trước khi gửi yêu cầu implement một task phụ thuộc của Member khác, Codex **phải đối chiếu V.2 và tạo `DEPENDENCY_NOTICE` theo VI.0**. Nếu chưa nhận được approved contract/source và evidence ở đúng level, chỉ triển khai phần riêng độc lập; không edit phần phụ thuộc. Tạo notice không đồng nghĩa đã thông báo cho người khác — kiểm chứng kênh gửi.

## 0. TEAM CO-OWNER GOVERNANCE — Quyết định điều hành chung v1.5

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

**Mốc thời gian vẫn theo ngày, không giờ cố định:** 28/11–05/12/2026; giao ngay khi đủ output. Mỗi Co-Owner tự báo status và lưu evidence; người cập nhật dashboard là **integration steward luân phiên hoặc người cả ba chỉ định**, không có quyền đơn phương duyệt tất cả Slice.

## I. Vai trò của tài liệu và nguyên tắc ưu tiên

Tài liệu này là **work assignment + acceptance plan**, được cập nhật thành **kế hoạch D0–D8 (27/11–05/12), 8 ngày thực thi từ 28/11**, không phải cam kết 8 Slice chắc chắn sẽ hoàn thành cho Codex của đúng thành viên được định danh tại phần đầu. Nó **không** là sự phê duyệt triển khai tự động, không thay thế `AGENTS.md`, Project North Star, approved technical contracts, domain ownership hay quyết định Co-Owner và technical review hiện hành. Khi có khác biệt, dừng phần xung đột, đối chiếu tài liệu có hiệu lực và xin quyết định rõ ràng; **không được tự sửa luật**.

**Mục tiêu V1 (bắt buộc):** một canonical logistics execution cho hai loại chủ vận đơn `PERSONAL | BUSINESS` với **cả `DROP_OFF` và `PICKUP`**. Bốn luồng thành công phải chạy từ đăng ký/đăng nhập hoặc onboarding hợp lệ → Quote → Booking/Shipment/Parcel/Label → hàng vào custody Carrier → Station Inventory/Sorting/Manifest → Transfer/Hub Receive → Dispatch/Courier → Delivery Attempt/POD → Delivered → Tracking/Finance/Support tương ứng. Đường Pickup bắt đầu bằng Courier đến lấy hàng và bàn giao Station; Drop-off do khách đưa hàng đến Station. Sau khi hội tụ, **không có hai bộ engine hoặc hai nguồn sự thật**.

**Mức nghiệm thu:** V1 tối thiểu là **SOURCE/TEST tích hợp cô lập** với Backend thật, PostgreSQL18, HTTPS UI/Mobile phù hợp, actor và command nghiệp vụ thật; dữ liệu tài khoản, Station, hợp đồng trong fixture phải khai báo là test-only. Không được suy diễn rằng test mô phỏng tương đương UAT/Production hoặc mạng lưới logistics thương mại. Phạm vi V1 **không yêu cầu** tối ưu lộ trình AI, full Admin menus, toàn bộ kế toán, bồi thường tự động hay COD không có contract.

### I.1. Bộ tài liệu cần đọc trước mỗi task

1. `AGENTS.md` ở root và mọi bản `AGENTS.md` áp dụng theo thư mục đang sửa.
2. `docs/implementation/ARGOPOST_PROJECT_NORTH_STAR.md` và `docs/implementation/ARGOPOST_E2E_NORTH_STAR.md` nếu hiện diện; không chép đè một bản lên bản khác.
3. `docs/implementation/v1-operational-e2e-critical-path.md`, `docs/implementation/v1-dual-audience-source-test-execution.md` và báo cáo mới nhất trong repo.
4. Các **approved** contract/ADR/spec và evidence hiện hữu cho slice đang làm, đặc biệt `v1-business-station-intake.delta.md`, `v1-intake-completeness-guards.delta.md` khi đụng Station; chỉ bản có chứng cứ technical/domain approval hợp lệ theo quyền thực tế mới cấp authority.
5. Source/API/schema/migrations/test hiện tại và Git status; kiểm chứng file thực có mặt trước khi đưa ra đường dẫn/claim. Tài liệu cũ không thay thế trạng thái code mới nhất.

**Quy tắc nhận task:** Co-Owner phụ trách domain gửi `MỤC 5 — NHIỆM VỤ HIỆN TẠI` hoặc một nhiệm vụ rõ ràng trong prompt. File này định nghĩa *trách nhiệm dài hạn*, không tự cho phép Codex thực thi mọi slice cùng lúc. Nếu chưa có task rõ ràng hoặc chưa xác minh được technical authority, làm **read-only audit/impact/contract proposal**, rồi báo blocker; không tự chạy migration hay phát triển domain chưa duyệt.

### I.2. Trạng thái khởi điểm được xác minh trong báo cáo 27/11/2026

- **S1 = PARTIAL, không Dual-Audience READY**: Personal HTTPS+PG18 Booking → Station (hai Parcel) PASS; Business Quote/Booking và Management V2 PASS nhưng **Business Acceptance E2E chưa PASS**.
- `BIA-TIMESTAMP-01`: immutable Booking `createdAt` trong `CarrierCargo` cần contract; `BIA-OWNER-01`: bảng `merchant.application_review_comment` có lỗi owner của LOGIN migrator cần forward-fix.
- `BIA-DB-GUARD-01`: đề xuất deferred cross-domain reads cho exact completeness; file gốc ghi **PROPOSED** và automatic review đã từ chối lần patch thiếu approval. **Không coi khuyến nghị trong chat là giấy phép đã phê duyệt.** Xác minh quyết định Co-Owner và technical review hiện hành và reviewer gate trước khi sửa.
- V48 có source/WIP; V1–V47 và checksum lịch sử được bảo toàn. Không giả định V48 đã/ chưa áp dụng tại mọi môi trường. **Kiểm tra registry, branch/PR, migration state trước bất kỳ forward migration mới.**
- S2–S5 chưa có evidence thực thi E2E hoàn chỉnh; S6–S7 có các thành phần rời rạc (UNPAID, Excel/CSV Draft, CSKH/Claims), chưa thể tuyên bố hoàn chỉnh.
- Menu2 Step B vẫn **`DEFERRED_POST_V1`**; không quay lại Lifecycle Execute/Owner Transfer khi chưa có quyết định Co-Owner mới.

Nếu source/evidence mới mâu thuẫn các điểm trên, báo khác biệt cho các Co-Owners liên quan kèm commit/hash/run ID, không lặng lẽ thay đổi trạng thái.


### I.3. SOURCE-FIRST INVENTORY — bắt buộc trước mọi code (D0, lặp lại khi nhận task mới)

**Không được triển khai dựa trên danh sách tính năng trong tài liệu này mà bỏ qua code đã tồn tại.** Báo cáo tiến độ ngày 27/11 là **baseline tham khảo, không phải trạng thái mới nhất của repository**. Codex phải đọc thực tế rồi chỉ làm **GAP có chứng cứ**. Không được đổi tên, sao chép hoặc xây lại engine đang tồn tại chỉ để hoàn thành Task ID.

**Trình tự `AUDIT → REUSE → GAP → CONTRACT → IMPLEMENT → PROVE`:**

1. **Read-only preflight:** đọc `AGENTS.md` (kể cả nested), hai North Star nếu có, Owner task mới nhất, critical-path, execution report mới nhất, approved deltas/ADRs/specs, functional coverage và evidence; ghi `git status --short`, `git diff --name-only`, `git log -1`, branch/worktree, staged index, migration list/registry, remote branches/PRs nếu có quyền xem. Không reset, clean, stash, commit, push hay sửa trạng thái để phục vụ audit.
2. **Source discovery:** tra bằng code search và các source files thực tế: class, controllers, ports, UI routes, schema, migrations, flags, security policy và tests. Đọc implementation thật, không chỉ tên `*Spec*`, TODO, README hoặc test stub. Phát hiện code đã có nhưng chưa nối HTTP/UI/worker; **ưu tiên wiring và tests thay vì tạo module mới**.
3. **Historical evidence verification:** đối chiếu exact Git revision, test names/results/run IDs, schema/checksum/fingerprint và dependencies thay đổi từ lúc PASS. PASS cũ được giữ là historical; nếu code liên quan đã đổi hoặc chưa từng test tích hợp thì ghi `STALE_EVIDENCE`/`NOT_INTEGRATED`, không ghi `CURRENT_PASS`.
4. **Gap register theo task ID:** ghi mỗi capability dưới một trạng thái: `REUSE_CURRENT_PASS`, `REUSE_NEEDS_REGRESSION`, `EXISTS_NEEDS_INTEGRATION`, `PARTIAL_GAP`, `MISSING`, `BLOCKED_APPROVAL`, `NOT_V1`. Kèm canonical source paths, behavior hiện hữu, bằng chứng, change nhỏ nhất, domain/source writer, approval, E2E dependency, estimated effort và test bắt buộc.
5. **Giới hạn patch:** chỉ chọn `PARTIAL_GAP`/`MISSING` có approved authority hoặc `EXISTS_NEEDS_INTEGRATION` trong phạm vi được phép. `REUSE_CURRENT_PASS` thì **không viết lại**, chỉ bổ sung integration/negative tests cần thiết. Code path/DB object không có trong source phải đánh dấu `PROPOSED`, không khẳng định đang hiện hữu.
6. **Chống đụng source:** trước mọi lần edit, đối chiếu owner+worktree+path-lock với hai teammate; nếu trùng file/port/migration, yêu cầu các Co-Owners có domain liên quan thống nhất đúng **một CODE_WRITER**. Khi hết quyền, chỉ mở request/handoff, không sửa source người khác.
7. **D0 output:** `ROLE_ID`, source-and-evidence inventory, `TASK_ID × STATUS × REUSE/GAP × PATH × OWNER × APPROVAL × TEST × DEPENDENCY × TARGET_DAY`, top blockers, và kế hoạch 24 giờ đầu. Report ngắn gọn nhưng đầy đủ, không làm giả test.

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

## III. 8 Slice và milestone toàn đội

| Slice | Đầu ra nghiệp vụ bắt buộc | Lead chính | Gate trước khi tuyên bố PASS | Mốc mục tiêu D0–D8 (ICT) |
|---|---|---|---|---|
| **S1** | Personal/Business Quote → Booking → Parcel/Label → **Drop-off** Station Acceptance + custody/tracking | Nam (Booking) + Hoàng (Intake) | `V1-S1-P` và `V1-S1-B`, DB security/atomicity & HTTPS/PG18 | **D2 — 29/11**; hiện chỉ có historical PARTIAL |
| **S2** | Canonical Inventory → versioned Sorting → exact Parcel-set Manifest Draft/Seal | **Hoàng** | Không double-count; no custody move on seal; strict site scope | **D3 — 30/11** |
| **S3** | Manifest Handover → Transfer → Hub Receiving/Discrepancy | **Hoàng** | Sender/receiver evidence, exactly one holder, rollback, Hub mismatch | **D4 — 01/12** |
| **S4** | DeliveryRun → Courier assignment/custody → Attempt/POD → Delivered | **Triết** | Genuine Courier + proof; deny fake delivery; per-parcel outcomes | **D4–D5 — 01–02/12** |
| **S5** | Customer Pickup Request → Courier Pickup → Station handover → shared S2–S4 | Nam (request) + Triết (execution) + Hoàng (station) | Personal + Business Pickup E2E branch and failure | **D5 — 02/12** |
| **S6** | Missed Pickup/Failed Delivery → Retry/Return + operational exceptions | Hoàng (network) + Triết (courier) + Nam (claim) | Full reverse custody/tracking & cancellation/no duplicate attempts | **D5–D6 — 02–03/12** |
| **S7** | Finance obligation & real supported settlement/reconciliation; Business XLSX/CSV Draft → Booking; Claims/CSKH | **Nam** | No fake payment/COD; import idempotency; issue-to-resolution evidence | **D2–D6 — 29/11–03/12** |
| **S8** | Four successful E2E scenarios + targeted failure/security/finance gates | **Nam+Hoàng+Triết** | Integrated SOURCE/TEST evidence, no blocker, signed gate | **D6–D8 — 03–05/12**, D7 final regression 04/12, ba Co-Owners cùng quyết định **trong ngày D8** |

**Chỉ một timeline có hiệu lực:** D0 **27/11/2026** đến D8 **05/12/2026**. Đây là **mốc mục tiêu có điều kiện**, không phải bằng chứng chức năng đã PASS, không phải quyền tự code hoặc cam kết hoàn thành. Handoff chi tiết và ngày mục tiêu tại **VI.1**; checkpoints tại **VII.2**; lịch theo tên tại **VII.A (Nam), VII.B (Hoàng), VII.C (Triết)**. Cả ba phải sử dụng cùng ngày mục tiêu và mốc nghiệm thu trong những mục đó, không tạo roadmap cạnh tranh.

**Hard dependencies, không buộc ngừng mọi công việc:** Chưa PASS S1 thì không claim S2 integrated; Hoàng vẫn có thể implement/test inventory domain độc lập theo contract được duyệt. S2 chưa có Manifest hợp lệ thì không claim S3; S3 chưa có Hub-received + physical custody evidence thì Triết không claim S4 E2E, nhưng được làm mobile/assignment độc lập. Pickup S5 phải hội tụ về Station Intake hiện hữu, không sinh engine thứ hai. S6 và Finance invariants phải được bổ sung đúng lúc phát sinh nghiệp vụ, không chờ đến mốc tổng hợp S6/S7. **Trễ handoff thì chuyển `WAITING_FOR_HANDOFF`, thực hiện task độc lập và báo Owner, tuyệt đối không tạo evidence giả.**

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

## V. Phạm vi độc quyền của V1-B — Carrier Network & Custody

**Mission:** Hoàng chịu trách nhiệm **physical custody và vận hành mạng lưới** từ Station Intake đến Inventory/Sorting/Manifest/Transfer/Hub Receiving, bao gồm handover Pickup từ Triết vào Station, discrepancy và reverse network. **Không** tạo Booking/giá/Org approval (Nam), không tạo Delivery Attempt/POD (Triết), không tự ghi Finance receipt.

**Nhóm source ưu tiên (kiểm chứng repo):** Carrier Operations `/ops` cho Station/Hub/network; Dispatch-owned `OperationalStationIntake`, site authority, `dispatch.pilot_intake_acceptance`, `dispatch.pilot_custody` và các projections/guards; Spec012/013 runtime mới chỉ theo contract. `orders.pilot_parcel`, `pilot_shipment`, `pilot_tracking_event` là **Orders facts**: dùng public ports/approved DB cross-read đúng delta, không tự sở hữu.

### V.1. Nhiệm vụ từng Slice

#### S1 — OWNER-NEUTRAL STATION ACCEPTANCE (B-S1-01 → B-S1-07)

- [ ] **B-S1-01 / audit:** Reuse original NWS PERSONAL Station Intake/OPS admission/exact scan/custody/receipt. So sánh current source với PASS evidence, không fork engine hoặc đánh đồng unit tests với Business E2E.
- [ ] **B-S1-02 / Business eligibility:** Sau đúng BIA-V1 approval, sử dụng Merchant `PilotCoverage` (A-owned) và Orders `CarrierCargo` (A-owned) để chọn BUSINESS canonical owner; Staff phải có genuine current Ops, `SITE_READ + STATION_INTAKE` đúng origin. Policy `BR13-BUSINESS-INTAKE-01` với ACTIVE Org/facility được phép; `UNPAID` phải qua Finance exact obligation reader, không tự allow từ Personal exception.
- [ ] **B-S1-03 / scan & acceptance:** Implement shared owner-neutral scans for exact Parcel set; 1–5 Parcels theo approved pilot; atomic ACCEPT, matching site/operator/cargo, exact copies custody/tracking/audit/receipt; duplicate accept/replay trả cùng kết quả, concurrent actors không sinh double custody.
- [ ] **B-S1-04 / OPS HTTP:** V1 Personal unchanged; V2 opt-in scoped endpoints theo BIA contract, không đổi response cho legacy clients, không expose Merchant PII cho OPS hoặc cho Customer public.
- [ ] **B-S1-05 / DB completeness:** Hợp tác Nam (Orders function owner) cho `BIA-DB-GUARD-01`: Hoàng chỉ là writer của **Dispatch-owned** `dispatch.require_complete_pilot_custody`; Orders-owned `orders.require_complete_pilot_tracking` là Nam/domain owner. Cross-private reads vào Orders/Dispatch audit/receipt **chỉ** sau exact approval + automatic security review, `SECURITY INVOKER`/owner/signature/ACL giữ nguyên. Không nới `SELECT`, không workaround review, reject DML forged events.
- [ ] **B-S1-06 / migration:** Hoàng kiểm chứng V48/ownership FKs/checks/historical preservation; phối hợp Nam sửa Merchant review-comment owner đúng domain; không edit V46; kiểm thử PostgreSQL18 cold start + populated upgrade + owners/ACL/checksum.
- [ ] **B-S1-07 / business HTTPS:** Cùng Nam chạy Business independent approval → Org activation → operator shipping config → Business Booking 2 Parcels → dedicated OPS login → 2 scans → atomic acceptance → Parcel custody/1 Shipment event/2 Parcel events/receipt/audit → Merchant AT_STATION. Negative: wrong site/org/facility/audience/operator, finance mismatch, stale createdAt, absent parcel, duplicate/race/revocation, forced late rollback.

**PASS khi:** `V1-S1-P` + `V1-S1-B` genuine HTTPS/PG18 PASS **và** DB completeness, ownership catalog, architecture gates PASS. Nam có customer reads. Nếu Business E2E chưa chạy hoặc guard còn BLOCKED → `PARTIAL`, không đổi status bằng JSON fixture.

#### S2 — INVENTORY, SORTING, MANIFEST (B-S2-01 → B-S2-06)

**Required prior:** Spec012 bounded API/schema/permission/state contract **được phê duyệt trước runtime code**. Tái sử dụng accepted custody + immutable Orders Parcel IDs; không tạo inventory source of truth thứ hai.

- [ ] **B-S2-01 / inventory:** Current physical holder/location projection từ accepted/movement/custody facts, scope theo Station/Hub và quyền staff; tách kiện available, allocated, in-transfer, exception; query theo actual location, no client-supplied override.
- [ ] **B-S2-02 / sort decisions:** Authenticated worker tạo/version sort decision từ route/service/address policy approved; invalidate stale decision khi upstream policy/version thực sự thay đổi, nhưng không rewrite historical Booking; re-sort phải có reason/audit.
- [ ] **B-S2-03 / manifest draft:** Chọn exact Parcel set đang tại site và đủ điều kiện, tuyến/destination và trạng thái hợp lệ; manifest ID, lifecycle `DRAFT → SEALED → HANDOVER_ELIGIBLE` theo approved spec, audit actor/time/version; không mix wrong org/site/route or foreign Parcel.
- [ ] **B-S2-04 / seal & concurrency:** No duplicate parcel allocation across open manifests, optimistic version/idempotency/replay; seal validate exact set, count, weight nếu nguồn verified; **Seal ≠ movement ≠ custody transfer**.
- [ ] **B-S2-05 / Ops UI:** Inventory search, scan/sort, manifest list/draft/seal và error/empty/loading states theo staff site RBAC; không hiển thị quyền sửa finance, không dùng fixture cho E2E claims.
- [ ] **B-S2-06 / PG18 gates:** Personal+Business 2 parcels cùng policy nhưng owners riêng; missing/duplicate/foreign/cross-site/mismatched route/revoked staff/stale version/races/rollback; Manifest remains correct even if scan partial/failure; evidence audit.

**PASS khi:** genuine accepted parcels → location inventory → correct sort decision → exact sealed Manifest; no double inclusion/phantom custody. Handoff `B2C-S4-DISPATCH-READY` để Triết chỉ nhận kiện đã ở điểm phát hợp lệ.

#### S3 — TRANSFER & HUB RECEIVING (B-S3-01 → B-S3-06)

**Required prior:** Spec013 authorized contract; S2 sealed Manifest; current holder valid. `Transfer` không được suy luận từ route config hay Merchant packing PDF.

- [ ] **B-S3-01:** Tạo Transfer/Manifest Handover với valid sender role, exact sealed parcels, destination Hub/site, explicit acceptance custody by transport actor theo approved policy.
- [ ] **B-S3-02:** Ghi `IN_TRANSIT` chỉ sau bằng chứng handover; không ghi trạng thái trên Booking snapshot; source event/holder thay đổi một lần/transaction.
- [ ] **B-S3-03:** Hub receiving worker scan thực tế; đối chiếu expected vs scanned: đủ/thừa/thiếu/sai kiện, partial arrival và discrepancy record; không âm thầm complete toàn bộ Manifest.
- [ ] **B-S3-04:** Confirm receiving theo authorized receiver; cập nhật custody current holder/location đúng từng parcel; reject wrong destination/site/actor, duplicate receive, stale manifest.
- [ ] **B-S3-05:** Destination sorting nếu cần, publish `READY_FOR_DISPATCH` event/port với IDs/holder/site/owner/version; Triết không được tiếp cận parcel nếu chưa nhận hợp lệ.
- [ ] **B-S3-06:** Negative rollback/replay/concurrent sender/receiver/stale handover; receipt/audit reconciliation, checksum/history preservation; H2/fixture không thay PG18.

**PASS khi:** mỗi parcel đi qua sender→transport→Hub receiver có đúng một holder hiện tại, audit/tracking, matched count hoặc discrepancy rõ. No teleport.

#### S4 — FINAL HUB RELEASE TO DISPATCH (B-S4-01 → B-S4-03)

- [ ] **B-S4-01:** Chốt readonly `DispatchEligibility` for Triết: current Hub/location, actual parcel set, route/attempt eligibility, current holder/custody version, Ops site authority.
- [ ] **B-S4-02:** Cung cấp handover command/receipt giữa Hub và Courier theo approved Spec014; Hoàng xử lý release inventory, **Triết** xử lý Courier acceptance/task; không viết Triết DeliveryRun.
- [ ] **B-S4-03:** Negative parcel already moved/held/returned, wrong Courier, duplicate handover, rollback, no cross-tenant data.

**PASS khi:** Triết có thể nhận đúng parcels đã ở Hub đích bằng event bàn giao hợp lệ; không còn kiện phantom trong inventory.

#### S5 — PICKUP TO STATION HANDOVER (B-S5-01 → B-S5-03)

- [ ] **B-S5-01:** Định nghĩa Station receiving từ Courier Pickup được Triết xác nhận, có custody trước khi vào Station; tránh ghi nhận như Customer tự Drop-off hay nhận hai lần cùng Parcel.
- [ ] **B-S5-02:** Kiểm tra Courier source evidence, exact Shipment/Parcel, site, prior holder, assignment validity; qua approved command bàn giao trách nhiệm về Station; trạng thái sau handover hội tụ về cùng inventory/sort flow S2.
- [ ] **B-S5-03:** Test Personal/Business Pickup: Courier picked scan → Station receiving → inventory; reject fake/drop-off bypass, wrong Station, missing pickup event, duplicate/replay/partial parcel, rollback.

**PASS khi:** pickup chain custody Sender→Courier→Station→Hub hợp lệ, không có source-of-truth thứ hai.

#### S6 — NETWORK EXCEPTIONS / REVERSE LOGISTICS (B-S6-01 → B-S6-04)

- [ ] **B-S6-01:** Xử lý Manifest discrepancy, missing/extra parcel, unsafe/hold inventory, quarantine với reason/evidence; không tự đánh dấu hàng mất/bồi thường.
- [ ] **B-S6-02:** Khi Triết trả hàng/failed attempt, có approved reverse manifest/transfer/site receive và current-holder transitions; không tạo đường tắt `RETURNED` từ một status-only API.
- [ ] **B-S6-03:** Prevent duplicate return or cross-route transfers; retry/return handoff must match exact owner/parcel/site/custody.
- [ ] **B-S6-04:** CSKH access limited to approved evidence reader; read event for claims, no physical exception from CSKH ticket text.

**PASS khi:** one physical discrepancy and one failed-delivery return traceable through Hub/Station and final owner-return evidence, all rollback/replay/negative gates PASS.

#### S7 — PROVIDE AUDITABLE FACTS, NOT MONEY (B-S7-01 → B-S7-02)

- [ ] **B-S7-01:** Public readonly event IDs (acceptance, handover, transfer, receiving, return) with organization-safe/Personal-safe metadata to Nam Finance, preserving historical invariants.
- [ ] **B-S7-02:** Prove event authenticity/custody chain and transaction reference; no Finance table DML, no fake collection/COD/paid status.

**PASS khi:** Nam reconciliation can link to actual operational event without direct private read; mismatched event/Shipment/owner denied.

#### S8 — NETWORK END-TO-END GATES (B-S8-01 → B-S8-04)

- [ ] **B-S8-01:** Run physical chain for `V1-E2E-P-DROP/B-DROP/P-PICK/B-PICK` with genuine acceptance, inventory, exact Manifest, transfer, Hub receiving and dispatch handover.
- [ ] **B-S8-02:** Inject wrong site/actor, two-actor races, duplicate parcel, source mismatch, manifest missing, partial Hub receive, reversal/return, late failure; validate transaction rollback and single current holder.
- [ ] **B-S8-03:** Provide PG18 catalog/ACL, migration checksums/forward upgrade and architecture boundary evidence with sanitized logs.
- [ ] **B-S8-04:** Hand off signed network readiness to Nam/Triết, specify exclusions/unsupported route exceptions.

**PASS khi:** 4 E2E show exactly-one-holder and no event forgery across Station/Hub/reverse; no S8 final approval without Nam/Triết signoffs.

### V.2. PRE-TASK DEPENDENCY MATRIX — Hoàng (V1-B)

**QUY TẮC:** Trước khi yêu cầu Codex triển khai **phần code phụ thuộc**, Hoàng phải lập `DEPENDENCY_NOTICE` cho Nam/Triết theo bảng, yêu cầu đúng mức CONTRACT/EVIDENCE và ghi task độc lập thay thế. Với S1 hiện tại, **Nam đang giữ temporary CODE_WRITER cho các change-set BIA-V1/V48/DB guards đang chạy**; Hoàng không được đụng phần shared đó khi chưa có `SOURCE_LOCK_RELEASED` và Owner reassign. Handoff không phải quyền tự chỉnh Orders/private data.

| Slice / task cần gate | Cần thông báo trước cho | Phải yêu cầu / nhận được gì | Gate trước code phụ thuộc hoặc E2E | Nếu chưa nhận: tiếp tục task độc lập |
|---|---|---|---|---|
| **S1 B-S1-02/03/04** (Business Intake/OPS V2) | **Nam V1-A** | `A2B-S1-BOOKED-CARGO`: canonical Orders/CarrierCargo createdAt, ownership, Org/facility, Merchant admission, Finance UNPAID, approved ports + tests | **CONTRACT + source writer availability** trước integration; **EVIDENCE** trước Business HTTPS PASS | B-S1-01 audit; B-S2-01/02 contract |
| **S1 B-S1-05/06** (deferred DB completeness/migration) | **Nam V1-A** (current change-set owner) | Exact signed/approved BIA-DB-GUARD review, Orders tracking function, migration catalog/ACL, V48 writer release; no historical checksum changes | **APPROVAL + SOURCE_LOCK_RELEASED** trước sửa Dispatch-owned guard/V48; test không bù thiếu approval | B-S2-03/04 spec, B-S1-06 read-only inventory |
| **S1 B-S1-07** (Business Station HTTPS E2E) | **Nam V1-A** | Genuine independent application approval, Org activation, explicit shipping setup, actual quote/Booking/2 Parcels, valid personal regression | **EVIDENCE** từ Nam + Hoàng engine PG18/HTTPS trước S1 PASS | B-S2-05 UI gap, B-S3-01/02 proposal |
| **S2 B-S2-01/02/03/04/06** (Inventory/Sorting/Manifest) | **Nam V1-A** (Orders canonical facts) | Approved Orders Shipment/Parcel reader + immutable route/service/owner/Parcel set; verify S1 accepted custody exists; Spec012 authority riêng | **CONTRACT + APPROVAL** trước source runtime; **EVIDENCE** S1 trước integration PASS | Spec012 proposal, negative test design, UI audit |
| **S3 B-S3-01…06** (Transfer/Hub) | **Không chờ Member khác cho dependency chính**; phụ thuộc **S2 nội bộ Hoàng** | Real sealed Manifest/current holder/location; Spec013 approved; nếu exposure cần Nam/Triết contract thì thông báo tương ứng | **INTERNAL EVIDENCE + APPROVAL**, không biến thành chờ Nam/Triết không cần thiết | Spec013 review, discrepancy test plan |
| **S4 B-S4-01** (Dispatch eligibility interface) | **Triết V1-C** | Triết xác nhận expected readiness/holder/version/route/error fields và DeliveryRun consumer contract | **CONTRACT coordination** trước đổi shared interface; Hoàng triển khai own reader theo authority | B-S3-03/04 Hub design |
| **S4 B-S4-02/03** (Hub→Courier physical handover) | **Triết V1-C** | Approved Courier assignment/acceptance/proof actor contract; no double custody writer; exact holder-transfer/ACK semantics | **CONTRACT** trước shared handover code; **EVIDENCE** Triết Courier acceptance trước S4 handoff PASS | B-S6-01 discrepancy, B-S7-01 evidence design |
| **S5 B-S5-01** (Station Pickup Receiving contract) | **Triết V1-C**; **Nam V1-A** for customer request context | Nam Pickup Request + Triết pickup-collection shape/actor/Parcel/custody and handover command; no substitute for Hoàng Station ACK | **CONTRACT** trước Hoàng receiving API; không chờ physical pickup để lập design | B-S2-05 UI / B-S6-01 contract |
| **S5 B-S5-02/03** (Pickup Station receiving E2E) | **Triết V1-C** | `C2B-S5-PICKUP-HANDOVER`: actual Courier scan, Sender→Courier custody/collection proof, correct site/Parcel IDs, genuine Hoàng Station ACK | **EVIDENCE** trước S5 Receiving PASS | B-S3-05/06 tests, B-S7-02 audit |
| **S6 B-S6-02/03** (return Hub/Station and reverse custody) | **Triết V1-C** | `C2B-S6-RETURN-HANDOVER`: approved failure/retry/return trigger and actual reverse handover, actor/site/Parcel, version | **CONTRACT** trước reverse API; **EVIDENCE** trước return E2E PASS | B-S6-01/04 network holds/proof reader |
| **S7 B-S7-01/02** (network evidence for Finance) | **Nam V1-A** (consumer) | Clarify canonical immutable reference/fee-trigger needs, scoped reader, finance non-DML boundary | **CONTRACT coordination** trước exposing new read APIs; no need Nam reconciliation runtime to build own reader | B-S8-02/03 negative matrix |
| **S8 B-S8-01/04** (four journeys/network signoff) | **Nam V1-A + Triết V1-C** | Nam valid Booking/Pickup Requests; Triết genuine pickup, Delivery/POD and handover evidence; same revision/PG18 E2E | **EVIDENCE + joint ACK** before S8 signoff | B-S8-02/03 own domain security tests |

**Outbound notices Hoàng phải chủ động gửi:** Nam về S1 upstream Orders facts/BIA-V48 writer release và S2 Orders reader; Triết về `B2C-S4-DISPATCH-READY`, pickup Station receiving, return-handover API. Không gửi lời nhắc trùng nếu có `HANDOFF_PACKET` hiện hành và đã ACK cùng revision.


### V.3. UI/API/TEST WORK PACKAGES — HOÀNG (V1-B)

Hoàng chịu trách nhiệm giao diện **Carrier Operations Station/Hub/Network**; **không** code Customer Portal của Nam hoặc Courier Mobile/DeliveryRun của Triết. Operations UI phải có route/menu/actor thật và state/error handling, không chỉ REST endpoints.

| Slice / Task | Hoàng phải hoàn thiện UI/actor workflow nào? | Backend + dependency | Real acceptance |
|---|---|---|---|
| S1 `B-S1-01/04/07` | OPS đúng site mở Intake, lookup/scan Parcel, xem exact set, match/deny, Accept một lần, hiển thị receipt + `AT_STATION`; Personal UI legacy giữ nguyên | Approved BIA/NWS, Orders/Finance refs từ Nam, current Ops admission | Ops Web→HTTPS→PG18 + customer tracking; duplicate/wrong site/owner/UNPAID guard |
| S2 `B-S2-01/06` | Operations Inventory list/search/filter theo Station/Hub; Parcel detail/current holder; sort decision form + route eligibility; Manifest create/edit/Seal/print/preview | Dispatch-owned canonical custody; Spec012 approval; Nam Parcel facts | Real scan/sort/manifest persisted; exact Parcel set; stale/held/not accepted/duplicate denied |
| S3 `B-S3-01/06` | OPS tạo handover/transfer từ sealed manifest, nhận receipt; Hub đích mở arrival queue, scan count, discrepancy thiếu/thừa/sai, confirm receive và destination sorting | Spec013; owned physical custody, manifest, current holder | Hub source UI→PG18 transfer/receive, compare list, no teleport, wrong site/partial receive tests |
| S4 `B-S4-01/03` | Operations Hub view `READY_FOR_DISPATCH`, release/physical handover scan và receipt cho Courier; **không** viết Dispatch DeliveryRun form | Triết courier assignment/handover contract, Hoàng current holder | Valid Parcel eligibility+handover event before Triết task acceptance; duplicate/release denied |
| S5 `B-S5-01/03` | Station nhận hàng do Courier Pickup bàn giao: scan, inspect/compare, ACK, Parcel/custody history | Triết actual pickup collection, Nam canonical Booking | Reuse same Station inventory; no second acceptance/custody engine or fake Drop-off |
| S6 `B-S6-01/04` | Operations discrepancy/hold queue, missing parcel detail, resolve/return receiving, authorized decisions + audit view | Triết reverse handover event, approved return/hold policies | Real failed/return physical custody, wrong actor and double return denied |
| S7 `B-S7-01/02` | Trong Ops history xuất event refs/scope để Nam Finance/CSKH xem qua approved reader; **không** xây Finance UI | Dispatch canonical event source; Nam read consumer | Scoped facts are truthful and immutable, no cross-tenant exposure |
| S8 `B-S8-01/04` | Automation/web browser walkthrough: Station→Inventory→Manifest→Transfer→Hub Receiving→Courier release for Personal/Business, Drop-off/Pickup | Nam Booking + Triết Pickup/Delivery handoff; same integrated revision | Real Ops Web+PG18 journey, security/rollback, signed network readiness |

**Không có ngoại lệ "Backend đủ":** nếu UI tại Station/Hub/Return chưa triển khai thì task runtime có thể `BACKEND_READY`, nhưng Slice liên quan **chưa** `SLICE_E2E_PASS`. Tận dụng Ops pages sẵn có; thay đổi shared Ops shell/nav phải có INTEGRATION LOCK.

## VI. Handoff bắt buộc của Hoàng

| Handoff ID | Đối tác | Contract/output | Gate |
|---|---|---|---|
| `A2B-S1-BOOKED-CARGO` | Nam → Hoàng | Canonical Orders Booking/Parcel/createdAt/owner/price policy | Nam/Orders-approved interface |
| `B2A-S1-ACCEPTED-EVENT` | Hoàng → Nam | Atomic acceptance/custody/tracking/receipt/audit IDs and current state | S1 PG18+HTTPS completeness PASS |
| `B2A-S2/S3-EVENTS` | Hoàng → Nam | Inventory/Manifest/Transfer/Hub event reader and version | S2/S3 genuine physical evidence |
| `B2C-S4-DISPATCH-READY` | Hoàng → Triết | Hub holder, exact Parcels, eligibility/version and custody handover | CONTRACT + real Hub receive EVIDENCE |
| `C2B-S5-PICKUP-HANDOVER` | Triết → Hoàng | Courier pickup proof/Parcel scan/custody/Station receipt | Genuine pickup execution PASS |
| `C2B-S6-RETURN-HANDOVER` | Triết → Hoàng | Failed delivery/return Parcel set/actor/physical transfer | Approved reverse policy + evidence |
| `B2A-S7-NETWORK-FACTS` | Hoàng → Nam | Immutable network custody/transfer/return refs | No finance DML |

**Hoàng không được:** sửa A-owned Orders/Finance/Merchant private sources, Triết Courier/DeliveryRun/Mobile, Admin Menu2 Step B; cấp extra SQL SELECT để làm guard pass, sửa SECURITY DEFINER hoặc suppress denied review; tạo fake manifest/transfer; giữ shipment `IN_TRANSIT`/`DELIVERED` theo đường tắt.


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


### VI.1. Daily Cross-Member Handoff Targets — sổ bàn giao duy nhất

**Các mốc dưới đây chỉ quy định ngày theo lịch Việt Nam, không ấn định giờ; một mốc dự kiến không mặc định là PASS.** Mỗi dòng thể hiện một deliverable được duyệt theo owner domain; nếu source/evidence đã có đúng phiên bản, **reuse** và ký nhận trước mốc, không build lại. Hai mức bắt buộc phân biệt: `CONTRACT_READY` (signature/authorization đã phê duyệt, chỉ cho tích hợp độc lập) và `EVIDENCE_READY` (genuine run + actual data/event chứng minh bàn giao thực tế). **CONTRACT_READY không đủ cho E2E PASS.**

| Ngày bàn giao mục tiêu | Handoff ID | Bên gửi → bên nhận | Gate / bằng chứng bắt buộc trước consumer ACK |
|---|---|---|---|
| **D0 27/11** | `TEAM-D0-BASELINE` | **Nam, Hoàng, Triết → shared team evidence register (3 Co-Owners)** | Ba source/evidence audits, gap registry, HEAD/worktree, role+writer lock matrix, review/decision queue; chưa coi đây là runtime PASS |
| **D1 28/11** | `A2B-S1-BOOKED-CARGO` | **Nam → Hoàng** | Approved Orders carrier contract, ownerType/Org/facility, immutable Booking/Parcel IDs, genuine createdAt, fee/UNPAID binding; test hoặc exact blocker |
| **D2 29/11** | `B2A-S1-ACCEPTED-EVENT` | **Hoàng → Nam** | BOTH Personal+Business valid Station acceptance, atomic custody/tracking/receipt/audit + PG18/HTTPS/negative/completeness gates; thiếu thì BLOCKED, không giả PASS |
| **D2 29/11** | `A2C-S5-PICKUP-REQUEST` — **CONTRACT** | **Nam → Triết** | Owner-scoped Pickup Request/Booking mode, admission, price/coverage, cancellation/idempotency; approved contract/version + targeted tests nếu có |
| **D3 30/11** | `B2C-S4-DISPATCH-READY` — **CONTRACT** | **Hoàng → Triết** | Approved readiness/eligibility DTO, current holder/version, exact Parcel set, handover command/error contract; **chưa** nhận là Hub physical PASS |
| **D3 30/11** | `A2C-S5-PICKUP-REQUEST` — **EVIDENCE** | **Nam → Triết** | Genuine authorized Pickup Request happy-path + wrong owner/cancel/duplicate tests; không dùng UI-only fixture làm booking thực |
| **D4 01/12** | `B2A-S2/S3-EVENTS` + `B2C-S4-DISPATCH-READY` — **EVIDENCE** | **Hoàng → Nam, Triết** | Real Station→Manifest→Transfer→Hub receive/holder, eligibility and event version; current PG18 tests + negative evidence; thiếu Hub gate → Triết chờ E2E |
| **D4 01/12** | `C2A-S4-POD-FACTS` | **Triết → Nam** | Genuine Courier delivery attempt/POD/Delivered proof với provenance + scoped read/PII denial; không gán Delivered từ UI |
| **D5 02/12** | `C2B-S5-PICKUP-HANDOVER` | **Triết → Hoàng** | Genuine assigned Courier pickup scan/physical custody, actor/site, exact Parcel set, proof/handover transaction; Hoàng phải ACK after real Station receive |
| **D5 02/12** | `B2A-S7-NETWORK-FACTS` + `C2A-S7-FINANCE-EVIDENCE` | **Hoàng, Triết → Nam** | Canonical transfer/custody/return and delivery/collection evidence refs, fee-relevant facts; **không** tự tạo PAID/COD/ledger |
| **D6 03/12** | `C2B-S6-RETURN-HANDOVER` | **Triết → Hoàng** | Failure/retry/return approved policy; correct actor, Parcel set and physical custody handover + negative tests |
| **D6 03/12** | `TEAM-S8-REHEARSAL` | **Nam + Hoàng + Triết → Nam** | Cả 4 E2E scenarios, S6/S7 security/finance/replay/rollback, source fingerprint; mọi unmet gate ghi FAIL/PARTIAL |
| **D7 04/12** | `TEAM-S8-REVALIDATION` | **Nam + Hoàng + Triết → shared team evidence register (3 Co-Owners)** | Chạy lại bốn hành trình Web/Mobile→HTTPS→PG18 trên integrated revision; khóa baseline review, triage P0/P1, chuẩn bị signed evidence; **chưa GO/NO-GO** |
| **D8 05/12** | `TEAM-S8-SIGNOFF` | **Nam + Hoàng + Triết → shared team evidence register (3 Co-Owners)** | Signed role-specific test/evidence matrix S1–S8, blockers, missing tests và source revisions sau D7 revalidation |
| **D8 05/12** | `TEAM-V1-GO-NOGO` | **Owner (Nam)** | GO chỉ nếu toàn bộ release gates PASS; nếu thiếu bất kỳ hard gate nào là **PARTIAL / NO-GO**, cùng recovery plan |

**Quy tắc bàn giao theo ngày:** khi nhận thấy mốc bất khả thi, gửi `RISK_NOTICE` ngay, không chờ hết ngày mục tiêu. Sau ngày mục tiêu, ghi `DEADLINE_MISSED` flag và escalate Co-Owners liên quan **ngay khi phát hiện**; không kéo mốc hoặc giả ACK. Nếu task đã được chứng minh PASS sớm, có thể bàn giao sớm với fingerprint; các mốc là *latest targets*, không cản trở tiến độ.

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

## VII. FAST-TRACK V1 — D0–D8 & quy trình GO/NO-GO

**Timebox:** bắt đầu kiểm kê **Thứ Sáu 27/11/2026 (D0)**; mục tiêu báo cáo nghiệm thu V1 **trong ngày Thứ Bảy 05/12/2026 (D8)**. Đây là **deadline mục tiêu rất tham vọng**, không bảo đảm có thể hoàn thiện 8 Slice từ baseline hiện tại. Đây là **single active execution timeline D0–D8**; mọi mốc lịch sử không còn hiệu lực cho đợt này. Không tự giảm phạm vi V1 hoặc chuyển tính năng bắt buộc thành tùy chọn. Nếu còn blocked/FAIL tại deadline, phải báo `PARTIAL/NO-GO`, không tuyên bố V1 PASS.

### VII.1. Quy tắc tăng tốc không đánh đổi chất lượng

- **Không rebuild** Identity/Booking/labels/NWS intake, Business management, Excel Draft importer, Finance obligation/CSKH nếu source+evidence xác minh có thể reuse. Thứ tự ưu tiên: `reuse → integrate → smallest missing behavior → test`.
- **Ba worktree/branch riêng**, không song song sửa cùng file, SQL migration, shared HTTP bootstrap, auth/security/config hoặc file mô tả chung. Các Co-Owners domain liên quan khóa shared paths/migration cho **một writer một thời điểm**; người khác triển khai phần độc lập trên interface đã được chốt.
- **D0 lập gap+contract review đồng thời**: Hoàng trình Spec012/013, Triết trình Spec014/015/029 và pickup/return, Nam trình Pickup booking/request và exact supported Finance method, chỉ cho phần thật sự thiếu sau audit. Gom quyết định cần đồng thuận Co-Owners theo đúng affected domains vào một checkpoint tổng hợp nhưng giữ từng domain đúng authority; không gộp tất cả thành giấy phép chung.
- **Hard dependencies:** Hoàng không Manifest movement trước valid acceptance; Triết không DeliveryRun từ manifest chưa Hub received/custody-handover; Pickup từ Triết chỉ vào Station khi Hoàng xác nhận; Nam không đánh `PAID` hoặc `DELIVERED` từ UI hay Booking. Real login/session, canonical Shipment/Parcel/custody/tracking, idempotency, auth/scope và rollback không được cắt bỏ.
- **Bằng chứng**: PG18/test backend thật, HTTPS Browser tích hợp và Courier app chạy với backend thật (thiết bị/emulator nếu có), catalog/ACL và negative tests liên quan. Unit/fixture/demo/UI screenshot **không** thay thế E2E. Đợt test dùng mạng tuyến mô phỏng SOURCE/TEST; không phải UAT/Production.
- **Triage hàng ngày:** đầu ngày trao đổi ngắn thống nhất blockers/lock/contracts, cuối ngày mỗi role gửi status + evidence + handoff; Owner xử lý decision queue trong ngày. Đổi timeline nếu critical path bị chặn; không giả làm đã approved bằng cách sửa nhãn tài liệu.

### VII.2. Checkpoints toàn đội — đồng bộ với handoff deadlines VI.1

**Ngày D0–D8 và ngày mục tiêu bàn giao của VI.1 là các mốc duy nhất có hiệu lực.** Checkpoint tổng quan dưới đây không tạo deadline khác hoặc thay thế `HANDOFF_PACKET`/consumer ACK.

| Ngày | Team gate (đều dựa trên source/evidence thật) | Quy tắc xử lý nếu không đạt |
|---|---|---|
| **D0 — 27/11** | 3 source audits; gap/task register; lock matrix; current S1 blocker status; các minimal review checkpoint cho phần thật sự thiếu | Không code unapproved; domain Co-Owners liên quan chốt writer/priority |
| **D1 — 28/11** | Ưu tiên đóng BIA-V1 Business intake + database security; đồng thời hoàn thành contract readiness cho S2–S7 | Nếu còn approval blocker, triển khai workstream độc lập; nêu critical-path risk |
| **D2 — 29/11** | Mục tiêu S1 dual-audience PASS; S2 Inventory/Sorting/Manifest và S4 foundation bắt đầu nếu authority hợp lệ | Nếu S1 chưa PASS, Hoàng không invent accepted custody; Nam/Triết tập trung phần độc lập |
| **D3 — 30/11** | Mục tiêu S2 PASS; khởi S3 Transfer/Hub, S4 DeliveryRun/Courier; Nam hoàn thiện Pickup Request/Excel path | Handoff chỉ theo test/contract đã duyệt, không dùng fixture làm physical event |
| **D4 — 01/12** | Mục tiêu S3 Hub receiving + S4 Drop-off→POD tích hợp; Nam Finance/Tracking consume actual events | Không ghi `DELIVERED` thiếu POD hoặc tồn kiện chuyển sai custody |
| **D5 — 02/12** | Mục tiêu S5 Personal/Business Pickup→Station→downstream; xử lý S6 failed/return chính, S7 payments/bulk/claims | Pickup không hội tụ về Station thật → S5 chưa PASS |
| **D6 — 03/12** | 4 happy-path E2E integration rehearsal; S6/S7 targeted negatives, replay/race, finance & claims; consolidate regressions | Missing mandatory flow/approved finance evidence = NO-GO candidate |
| **D7 — 04/12** | Tập trung khép các regression/integration blockers còn tồn, retest 4 journeys trên cùng integrated revision; cuối ngày `TEAM-S8-REVALIDATION`, chuẩn bị role evidence drafts | Không xem 04/12 là GO/NO-GO; chỉ thay đổi code theo approved authority, không thêm speculative scope |
| **D8 — 05/12** | Hoàng và Triết công bố `TEAM-S8-SIGNOFF` cho cả ba Co-Owners trước khi cả ba đánh giá; Nam xác minh Customer/Commercial, Hoàng xác minh Network, Triết xác minh Courier; cả ba quyết định `TEAM-V1-GO-NOGO` trong ngày 05/12 | Chỉ `V1_OPERATIONAL_E2E_ACCEPTED — SOURCE/TEST` nếu mọi hard gate PASS; ngược lại PARTIAL/NO-GO + blockers |

**Các checkpoint D2–D6 là *mục tiêu*, không phải trạng thái đã đạt hoặc phê duyệt kỹ thuật.** Nếu nguồn code thiếu nhiều hơn audit dự kiến, cả ba Co-Owners phải ghi nhận nguy cơ trễ deadline hoặc cùng quyết định thay đổi phạm vi; không được tự ý bỏ Pickup, Business, payment/claims theo đã thống nhất để hợp thức hóa nhãn V1.

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

**PROMPT khởi động mỗi Codex:** `Tôi giao ROLE_ID=V1-... đúng file này. Bắt đầu D0 bằng read-only source/evidence audit, lập reuse/gap register cho task IDs của mình và kế hoạch tới ngày mục tiêu 05/12/2026. KHÔNG build lại code đã có, không edit chưa đủ approval. Sau đó triển khai smallest approved critical-path task, bắt buộc tuân thủ V.2 + VI.0–VI.4 (DEPENDENCY_NOTICE trước code phụ thuộc, handoff deadline + ACK), report theo VII.4.`


### VII.5. TEAM EXECUTION STRATEGY — D0–D8, giảm chờ/giảm conflict

**Chiến thuật:** *contract-first + early handoff + vertical full-stack slice + single-writer + continuous integration*. Không làm hết backend độc lập rồi đến D6 mới bắt đầu Frontend hay integration. Mục tiêu thực dụng: làm **đường thao tác người dùng thật** sớm nhất có thể trên mỗi bounded workflow, test mỗi ngày; bảo toàn 8 Slice và bốn V1 E2E target, báo NO-GO nếu quá scope.

**Operating cadence (ICT, không tạo timeline mới):** đầu ngày kiểm tra critical blockers/approval/owner lock của ngày, handoff cần dùng trong 1–2 ngày tới; trong ngày đối chiếu các milestone VI.1 đến hạn; khi có source sẵn sàng thử integration/smoke với packet **đã đủ mức** (không đợi đủ toàn Slice); cuối ngày báo cáo tests + status + next independent task. Có blocker hard gate: notice/escalation ngay khi phát hiện theo VI.2. Người dùng Codex chuyển các notice qua kênh có thực; không giả lập thông báo tự động.

**WIP policy:** tối đa **1 task runtime đang edit + 1 task contract/audit độc lập** cho mỗi Codex tại cùng thời điểm. Khi task runtime blocked: đóng gói partial diff/evidence, ghi trạng thái `WAITING_FOR_HANDOFF/BLOCKED_APPROVAL`, chuyển sang một task độc lập trong V.1; không tự chuyển source writer. Ưu tiên finish nhỏ, test được, bàn giao sớm. Không biến 3 người thành 3 backend teams không có UI.

**Source/change locks:** Integration steward do cả ba chỉ định là **điều phối viên tích hợp (không phải người phê duyệt đơn phương)**, không mặc nhiên là writer mọi file. Trước patch: `LOCK_REQUEST(path/module, purpose, ROLE_ID, approval, planned consumer, expiry/handback)`. Shared source (`Workflow`, common config, migration registry, app shell, DTO clients, permission/ACL) chỉ có một CODE_WRITER tại một thời điểm. Nếu một integration phải sửa file người khác, tạo `CHANGE_REQUEST` → owner-domain review → hai/bên Co-Owners có liên quan thống nhất writer; người còn lại viết contract/consumer test. Worktrees/clones tách riêng giảm ghi đè **nhưng không tự giải quyết merge conflicts**. Branch integration/commit/push/merge vẫn cần quyền thực tế và quy định AGENTS/CI/branch protection; Co-Owner không được tự vượt kiểm soát.

**Daily evidence-first vertical gates:**

| Ngày (timeline VII.2 hiện hành) | Điểm giao sớm, nhóm nên kết nối ngay | Full-stack thử càng sớm càng tốt | Nếu chưa đủ gate |
|---|---|---|---|
| **D0 27/11** | Nam/Hoàng/Triết trao source inventory, CODE_WRITER locks, UI gap matrix, review/decision queue, BIA/V48 source lock | Khởi động UI/Backend test harness theo actor, giữ state historical/PARTIAL | Freeze shared writer; task audit/contract độc lập |
| **D1 28/11** | Nam→Hoàng BookedCargo; Nam↔Triết Pickup Request shape; Hoàng↔Triết Dispatch Eligibility/error shape | S1 Personal regression UI + Business customer/admin path, Triết mobile auth/build | Hoàng không tự sửa Orders; Triết chuẩn bị Courier auth/UI/test |
| **D2 29/11** | Hoàng→Nam Station proof; Nam→Triết Pickup contract; Spec012/014 approvals nếu thiếu | S1 Business OPS browser→PG18; S7 Bulk valid row→Booked UI | Nhận `BLOCKED_APPROVAL`; Hoàng prep Spec013, Triết Mobile tasks |
| **D3 30/11** | Hoàng→Triết readiness contract, Nam→Triết Pickup API evidence; share tested source ngay | S2 Operations Inventory/Sort/Manifest UI→PG18, Triết Dispatch UI foundation | Triết làm mobile task UI/contract tests, không giả Hub custody |
| **D4 01/12** | Hoàng Hub real events→Nam/Triết, Triết POD facts→Nam ngay khi có | S3 Hub receiving browser + S4 Dispatcher Web/Courier Mobile/POD first integrated Drop-off | Nếu S3 blocked, Triết chỉ feature-ready, Nam UI adapter tests |
| **D5 02/12** | Triết Pickup→Hoàng Station ACK, Hoàng/Triết actual finance-relevant facts→Nam | S5 Personal/Business Pickup→Station + S6 failure UI; S7 Finance/Claims web integration | Hoàng/Triết không viết phantom collection; Nam chỉ show UNPAID nếu chưa payment |
| **D6 03/12** | Combined source revision + packets + 4 actor journeys, fix gaps not new speculative scope | Four Web+Mobile+PG18 E2E rehearsals, negative/rollback and UI error states | Mark PARTIAL, prioritize critical gap and test evidence |
| **D7 04/12** | Final full-stack regression, triage chính xác blocker còn tồn; cuối ngày team revalidation packet | Repeat all four journeys on the same accessible integrated revision, draft Nam/Hoàng/Triết signoff | Không coi 04/12 là GO/NO-GO; chỉ thay đổi theo approved CODE_WRITER |
| **D8 05/12** | Nam/Hoàng/Triết cùng ký trên integrated revision; cả ba đồng thuận GO/NO-GO trong ngày | S8 GO only with every full-stack hard gate PASS; otherwise PARTIAL/NO-GO with recovery | Không xóa test failure, giảm scope hay tuyên bố Production Ready |

**No-silent-wait rule:** provider không thể bàn giao đúng hạn → phải gửi `BLOCKER_NOTICE` (exact field/owner/reason), consumer chuyển ngay sang `NEXT_INDEPENDENT_TASK` và Co-Owners của domain bị ảnh hưởng chọn một trong: re-sequence allowed task, assign temporary sole writer, approve minimal delta, hoặc declare schedule risk. **Không có quyền mở rộng contract mặc định chỉ vì deadline D8 (05/12).**

**Daily release dashboard:** Nam cập nhật một hàng cho mỗi Slice: `S# | UI_GATE | BACKEND_GATE | PG18_GATE | INTEGRATION_GATE | SECURITY_GATE | ROLE_EVIDENCE | BLOCKER | NEXT_OWNER | TARGET_DAY | GO/NO-GO`. Nếu UI đã có nhưng chưa Backend real: `UI_PROTOTYPE_READY`; nếu Backend có mà chưa UI: `BACKEND_READY`; nếu source có ở worktree riêng nhưng chưa ghép: `INTEGRATION_PENDING`. Không đánh dấu `PASS` chỉ vì tất cả task independent đã xong.

**Reality check:** deadline D8 (05/12) là aggressive checkpoint, không cam kết chắc chắn 8 Slice hoàn chỉnh. Nếu audit D0 cho thấy Spec012–015/029, Pickup hoặc Finance payment authority chưa sẵn sàng, cả ba Co-Owners phải được báo RED ngay; ưu tiên cả bốn hành trình và controls đã duyệt, không tự đánh tráo V1 bằng Drop-off-only hoặc Backend-only.

### VII.B — Lịch của Hoàng: hành động V1-B — Carrier Network & Custody

| Ngày | Ưu tiên thực thi trong authority | Output/handoff đo được |
|---|---|---|
| D0 (27/11) | Audit Dispatch Intake/NWS, Parcel ownership V48, guards, real scan/custody/tracking, Station/Hub inventory, sorting, manifest, transfer, receiving, DB migration state; rà true latest S1 evidence | `B-D0-GAP`, exact overlapping source/guard with Nam, Spec012/013 minimal contracts nếu thiếu, gửi Triết custody eligibility schema |
| D1 (28/11) | BIA-V1 Business Intake Ops V2 + atomic guards và BIA-DB-GUARD only if exact authority/review; lock/rollback/replay, wrong site/owner/Org | S1 Hoàng Business/Personal intake PG18 evidence + exact 2 Parcel audits/receipts; no private cross-read beyond approval |
| D2 (29/11) | Cùng Nam nghiệm thu S1 real HTTPS; triển khai S2 canonical Inventory read/movement eligibility, versioned Sorting, Manifest Draft/Seal theo Spec012 approved | S1 signoff hoặc blocker; S2 event/custody contracts, Hoàng Manifest draft tests |
| D3 (30/11) | S2 finish exact Parcel-set Manifest, per-site status, seal no movement; start S3 handover/transfer under approved Spec013 | S2 PG18+site/duplicate/partial tests, B2C readiness draft (không pretend delivered) |
| D4 (01/12) | S3 physical handover→transfer→Hub receiving/reconciliation, custody current-holder and tracking; Triết dùng read-only eligibility | S3 valid arrived Parcels and exactly-one holder; `B2C-S4-DISPATCH-READY` |
| D5 (02/12) | S5 Courier pickup handover to Station, same canonical acceptance into Inventory; S6 reverse receive/return routing with Triết | `C2B-S5-PICKUP-HANDOVER` exact Parcel/owner/actor/site tests; network failed/return handling |
| D6 (03/12) | Regression các luồng Hoàng, inventory/missing Parcel, partial Hub receive, duplicate manifest/transfer, concurrency/replay/rollback; 4 E2E rehearsal | S2/S3/S5/S6 evidence per four journey and Hoàng security/catalog signoff draft |
| D7 (04/12) | Tập trung regression S2/S3/S5/S6, Ops Web → HTTPS → PG18, Hub/custody, ACL/migration, ACK các handoff còn thiếu; phối hợp full team revalidation cuối ngày | Network evidence draft + updated source fingerprint và issue list cho Nam, chưa final sign-off |
| D8 (05/12 — trước khi ba Co-Owners chốt quyết định chung) | Chạy final targeted tests trên integrated revision, công bố `TEAM-S8-SIGNOFF` cho cả ba Co-Owners trước quyết định GO/NO-GO chung, nêu đúng hard gate PASS/PARTIAL | `B-S8` signed Network report + PG18/HTTPS/UI/security evidence hoặc exact missing blockers |

**Hoàng là source writer Station Intake và Station/Hub operations, Triết là writer DeliveryRun/Courier**, dù hai nhóm có thể cùng nằm trong `Dispatch` module. Shared config/migrations/HTTP cần Owner lock. Nếu nguồn hiện tại đã có Sorting/Manifest/Transfer, Hoàng phải reuse và sửa gap, **không tạo engine thứ hai**.
