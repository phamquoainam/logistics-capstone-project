# Menu 2 — Step B: exact technical approval checkpoint

> **Owner disposition — 2026-11-26: DEFERRED_POST_V1, NOT APPROVED FOR EXECUTION.**
> Operational shipment E2E supersedes this execution priority. Keep the original
> manifest bytes/hash and all review history below; this is neither REJECTED nor
> COMPLETED. Current task: [V1 critical path](v1-operational-e2e-critical-path.md).

**2026-11-26 · [PROPOSED] · SOURCE/TEST cô lập.**

Trạng thái: **STEP_B_TECHNICAL_REVIEW_PREPARED — BLOCKED_EXACT_TECHNICAL_APPROVAL**.
Đây không phải Step B SOURCE/TEST READY hoặc `MENU_2_FULL_SPEC_READY`.

## 1. Baseline và kết quả rà soát

Giữ nguyên M2-R01 approved subset, V46, M2-L01 preparation subset, V47,
Current Owner bridge, case/review/RFI, policy reader và audited disclosure.
Không chạy lại các scenario đã PASS; không sửa Backend/Frontend/migration trong
checkpoint này. Lịch sử failure và acceptance vẫn ở
[readiness](admin-menu12-readiness.md).

Parent manifest giữ nguyên SHA-256
`6d072d9b649ab144c2fb9b8b780c8f077757387bcef58536a31618f9154251a2`.
Business Policy V1 và DEC-O3 clarification đã được duyệt; không hỏi lại policy.

| Step B | Nguồn hiện hành / thiếu sót thực tế | Kết luận |
|---|---|---|
| Purpose-bound decision/execute | `JdbcAdminGovernanceReauthentication.java:18` chỉ có APPROVE/REJECT/SUSPEND/REINSTATE. `MerchantGovernanceDecisionHttp.java:31` cũng chỉ nhận bốn operation; target là application hoặc Organization, chưa phải governance case. | Reuse engine; cần hai purpose và resolver/authority binding chính xác. |
| Maker-checker/decision/execution | V47 chỉ cấp propose/review; case SQL guard chưa nhận decision/execution. | Không thể mở endpoint bằng UI hoặc dùng proof của Merchant Application. |
| Người quyết định đọc nội dung | M2-Case không có reviewVersion/impactDigest; disclosure đã duyệt chỉ maker/current reviewer. | Cần audited decision snapshot riêng, không tự mở rộng disclosure cũ. |
| Applied hold/release | DB09 chưa tồn tại. Payload case được mã hóa ở ứng dụng, SQL executor năm tham số tham chiếu không thể đọc loại hold/expiry/release target. | Cần typed non-PII projection trên case và proof-binding metadata trên event. |
| RESTRICTED capability enforcement | `JdbcBusinessWorkspaces` hiện ACTIVE-only; các API dùng helper chung, Team invitation accept và offline Pricing contract có đường riêng. | Cần gate theo action ở Backend và DB, không đổi nghĩa tất cả request thành history/read. |
| CLOSE | Chưa có implementations của `OrganizationOpenWorkFacts` / `OrganizationFinancialClearance`. UNPAID aggregate không phải exit clearance. | `BLOCKED_CANONICAL_SOURCE`; chưa có positive CLEAR acceptance. |

## 2. Một gói technical approval có giới hạn

Artifact chính xác:
[Step B manifest](admin-menu2-step-b-technical-manifest.proposed.json).
Đây là delta theo parent manifest, không phải một Lifecycle contract thứ hai.
Mọi amendment trong artifact vẫn `[PROPOSED]`.

SHA-256 của Step B manifest:
`b88e6d9df08c60d78d45afde9883dc424ed5ccb9aa5b12033f271d39e9cd1cd4`.

Đề nghị Owner duyệt **ba nhóm kỹ thuật**, chỉ SOURCE/TEST PostgreSQL18 cô lập:

1. **Identity binding — M2-B-IDENTITY-01.** Hai purpose mới trên engine hiện có;
   reuse private SQL predicate đã có ACL, không thêm Identity table/quyền. Hai
   Merchant Java methods resolve Case→Org/referenced subjects và server authority
   fingerprint. Không nhận actor/scope/authority do client tự khai.
2. **Merchant execution — M2-B-DISCLOSURE-01, M2-B-EFFECT-01,
   M2-B-STATE-01 và các parent items được liệt kê.** Bốn endpoint parent, một
   audited decision-disclosure endpoint mới; hai permission Organization-only;
   DB09; năm effect columns và ba proof-binding event columns; giữ nguyên exact
   SQL executor signature. Không chuyển Owner/member/grant/current-owner pointer.
3. **Operation-aware admission — M2-B-ADMISSION-01.** Một default-deny genuine
   Customer-context binding, hai Boolean SQL predicates với ACL chính xác,
   fixed-operation consumer gates và lock-order reconciliation. Phần shared
   Team/Identity/Orders/Pricing chỉ thực hiện sau khi có coordination về exact
   source revision; không được xem Owner approval là quyền ghi đè WIP nhóm khác.

Permission delta chỉ **15→17** capability codes của scoped Governance, thêm
`org.lifecycle.decide` và `org.lifecycle.execute`; không tự mở 22 permission của
toàn parent manifest, không auto-grant theo role, không real operator grant.

Các shared interfaces/SQL signatures, DTO/columns, ACL envelope, operation map,
consumer boundaries và negative gates được ghi trong JSON. Các khác biệt thực tế
phát hiện sau approval phải tách thành bounded delta; không tự phê duyệt.

## 3. Security và transaction requirements

- Gọi genuine Identity owner verification trước actor/scope. Merchant kiểm chứng
  đúng issued Scope, physical DataSource/transaction, callback lifetime và current
  grants; constructed/forwarded/escaped/suspended contexts đều deny.
- Decision proof và execution proof độc lập, consume cùng transaction với event,
  receipt và effects tương ứng. Ba actor maker/reviewer/decider khác nhau; executor
  có execute grant riêng, có thể là decider nhưng không là maker/reviewer.
- Receipt replay kiểm current admission/authorization và exact actor/Org/operation/
  key/digest trước khi trả kết quả đã commit. Không consume lại proof hoặc fail
  chỉ vì If-Match cũ của cùng request; payload khác phải reject.
- Decision evidence đã commit không bị viết lại. Execute phải kiểm current policy,
  authority và reviewed revision; không dùng proof decision cũ như fresh execute
  proof hoặc giả một lần reauthentication khác.
- Giữ lock order thống nhất với Business/Team/Booking; kiểm concurrency execution
  với quote/booking/delegation/contract change. Không sửa lock order một consumer
  rồi bỏ đường governance cũ theo thứ tự ngược lại.
- Runtime không được trực tiếp UPDATE/DELETE applied holds. Executor chỉ đổi state/
  version/hold đúng approved case, có event/receipt/proof và deferred completeness
  guards. Metadata projection không được biến thành nơi sửa private case độc lập.
- Legacy V47 case/event giữ nguyên bytes/data. Không SQL-decrypt/backfill hoặc tạo
  fake typed effect cho case cũ. Reviewed legacy case thiếu projection phải qua
  canonical RFI/resubmission/review trước decision.
- Không tự động ACTIVE trở lại hay tạo release event khi hết hạn. Không coi expiry
  proof của decision lịch sử là bằng chứng đã có fresh execution proof.

## 4. CLOSE và Step C chưa được mở

CLOSE cần **hai canonical clearance readers thật**, completeness từ Orders và
Finance, current-version/physical-transaction binding và cùng admission lock.
Thiếu provider thì deny `UNAVAILABLE`; có nguồn nói BLOCKED thì deny BLOCKED.
Không trả CLEAR từ danh sách rỗng giả, mock reader, số UNPAID hoặc receipt của
request khác. Việc chưa có positive CLEAR evidence không thể được tính PASS.

M2-PORT-05/06 và các dependency architecture/locking của chúng cần được owning
domains đối chiếu. Gói này không triển khai một Finance/Orders clearance engine
thay thế. DISPUTE/REVERIFY vẫn chờ purpose-specific retention/evidence policy.

Step C chỉ tiếp tục khi có exact Identity two-party intent/Team coordination,
atomic Current Owner/member/grants/invitations/history guards và các acceptance
tương ứng. Giữ outgoing Owner policy ACTIVE OPERATOR + chỉ dashboard.read; không
thực thi chuyển Owner trong Step B hoặc tự thay role model.

## 5. Git/migration preflight và thứ tự triển khai

Read-only GitHub kiểm 51 branches, không có open PR; complete trees của develop,
admin-dashboard, organization-management và spec003-full-onboarding không có
V48+. Local đang develop tại `d5a0c240841bdb35d9ef6064cddba696a5b2fa62`.
Đây không phải reservation của unpublished WIP. V48 Identity / V49 Merchant chỉ
là candidates, **chưa tạo file/chưa reserve**; phải kiểm tra lại registry và branch
liên quan trước implementation.

Sau exact approval và coordination: Identity binding → decision disclosure /
typed effects / DB guards → operation admission và holds → consolidated HTTPS +
PG18 scenario. CLOSE chỉ mở sau canonical provider acceptance. Không chuyển Menu3.

## 6. Verification ở checkpoint này

Chỉ chạy document/manifest/reference/preservation/ownership checks. Kết quả cụ thể
được ghi ở readiness sau khi chạy. M2-B-T-01–07 đều **NOT RUN — pending exact
technical approval**; không suy diễn security, PG18, Browser, CI hay full-suite PASS.
Không chạy lại R01/Preparation hoặc toàn bộ Backend vì runtime source không đổi.

Evidence inventory: `.demo/menu2-step-b-preflight.json`,
`.demo/menu2-step-b-github.json`; final document validation và fingerprints sẽ được
lưu riêng. Không DB connection/mutation, deploy, mail, grant, Git commit/push/merge.

Kết quả thực tế: **13/13 document checks PASS** trong
`.demo/menu2-step-b-document-validation.json`; `python
docs/reconciliation/v3/validate.py --implementation` exit0, ownership CONSISTENT;
`git diff --check` exit0 (cảnh báo LF/CRLF của WIP cũ giữ nguyên). Fingerprints
163 file baseline: chỉ readiness và traceability thay đổi có chủ đích; 161 file
còn lại, toàn bộ 46 migrations V1–V47, parent manifest, HEAD và index giữ nguyên.
Hai artifact Step B mới là documentation-only. Không có behavior test nào được
chạy hoặc được gán PASS trong lượt này.
