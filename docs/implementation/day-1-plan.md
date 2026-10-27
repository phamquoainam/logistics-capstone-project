# Day 1 — Existing Codebase Assessment, Baseline Verification & SDD Implementation Plan

**2026-10-19 — AC-001-04 S2-R1 / CI inventory publication checkpoint.**
S1 assessment complete; two reviewed nonblank validation defects resolved with
exactly two production condition changes. Original eight RED assertions retained
and GREEN; original98 cases plus11 controls now109 PASS. CI explicit inventory
requires S2 in H2/PG only: expected architecture23/0 skips, H2157/17 skips,
PG182/5 skips. Local report/privacy controls37 PASS, S2109 PASS, ArchUnit23 PASS,
H2 clean verify157 PASS/17 expected skips and real H2 report guard PASS.
Hosted checks/artifacts/ruleset require actual publication review; no hosted PASS
inferred here. Full process/admission/PG configuration matrix remains UNVERIFIED;
real migration BLOCKED, legacy ACL/root owner and raw-cache FAIL/npm attribution
remain OPEN. AC-001-04/05 NOT ACCEPTED; Feature001/D1-08/Day1 PARTIAL.
[Sanitized evidence](ac-001-04-s2-evidence.md). Original REQ/AC unchanged.

**2026-10-19 — Protected store Phase 1: REVIEW_PASS_READY_FOR_PR_PREPARATION.**
Final independent review: PCS-01/02/03/04/RR-01 RESOLVED for the approved bounded
contract. Initial Ubuntu publication58 PASS/1 FAIL/13 skips stopped publication;
the test-only scoped owner fixture was repaired without changing production
logic/assertions. Focused GREEN1+adjacent4; full Windows72 PASS/0 skips and
Ubuntu59 PASS/13 expected Windows-only skips. Web28/lint/source typing/build
PASS are retained from the preceding publication execution, not rerun here.
Hosted backend verification remains separate; Linux protected mode unsupported.
Migration BLOCKED, legacy exposure/root owner action and raw-cache findings OPEN;
AC-001-04/05 NOT ACCEPTED, Feature001/D1-08/Day1 PARTIAL.
[Current evidence](ac-001-04-credential-store-evidence.md). Earlier checkpoints follow.

**2026-10-19 — PCS-RR-01 strict presence: PCS_RR01_PATCH_READY_FOR_REVIEW.**
Security decisions distinguish PRESENT/ABSENT/UNKNOWN; UNKNOWN query failure
cannot establish collision-free target, successful replace or reconciliation.
72 local security/native/launcher tests PASS/0 skips; guarded actual dry-run12
metadata rows/0 credential reads/0 destination writes, eligibility still BLOCKED.
Web28 PASS is historical, not rerun. PCS-01/02 resolved by prior re-review;
PCS-03/04/RR-01 corrections await final review. No PR/migration/acceptance claim.
[Evidence](ac-001-04-credential-store-evidence.md). AC-001-04/05 unaccepted;
Feature001/D1-08/Day1 PARTIAL. Earlier checkpoints remain below.

**2026-10-18 — Protected-store corrective checkpoint: CORRECTIVE_PATCH_READY_FOR_REVIEW.**
PCS-01..04 addressed in the bounded synthetic implementation: revision/identity
rollback, reserved control names, explicit dry-run eligibility and append-only
replace/reconciliation evidence.57 local Python/native/launcher tests PASS/0 skips;
28 Web unit tests and lint/src typecheck/build PASS. Actual guarded dry-run remains
BLOCKED for migration; legacy namespaces unsafe and root owner action required.
Power-loss durability, real cutover and Linux/hosted validation remain open.
[Evidence](ac-001-04-credential-store-evidence.md). Not READY_FOR_PR or acceptance;
AC-001-04/05 unaccepted; Feature001/D1-08/Day1 PARTIAL. Earlier results follow.

**2026-10-18 — AC-001-04 protected-store Phase 1: IMPLEMENTATION_READY_FOR_REVIEW.**
[Evidence](ac-001-04-credential-store-evidence.md),
[decision](../architecture/decisions/ADR-0008-protected-credential-store.md).
Protected KnownFolder helper, explicit legacy/protected selectors and test-only
Web configuration;36 Python/native/mock-launcher tests and28 Web unit tests PASS,
Web lint/src typecheck/build PASS. Actual dry-run5 active+7 historical metadata
rows only:0 credential-content reads/0 destination writes, root owner action.
No real store/migration/retirement, Docker/native execution or CI changes.
Legacy default and unresolved exposure remain; protected PG rebind is blocked.
AC-001-04/05 remain unaccepted; S1 pending, S2/S3/S4 not resumed;
Feature001/D1-08/Day1 PARTIAL. Original REQ/AC remain unchanged.

**Formal owner acceptance — 2026-10-18: AC-001-01 DONE / PASS_WITH_DOCUMENTED_FINDINGS.**
[Acceptance record and open findings](ac-001-01-final-acceptance.md): integrated
develop `242e3d31192fb493938b09b398b4cd1ddd29a83b`, PR16 and actual develop
run39067632758/attempt1 SUCCESS; new-clone SDK/web/mobile/native evidence reviewed.
Original25-gate reconciliation: **24 PASS / 0 FAIL / 1 UNVERIFIED**; broad raw-artifact
privacy assurance remains UNVERIFIED and the raw-cache scan FAIL is retained.
Historical libuv/Rabbit findings and native wrapper exit1 are not FIXED or erased.
AC-001-04/05 remain independently unaccepted; **Feature001/D1-08/Day1 PARTIAL**.
The older checkpoint entries below retain their historical outcomes; this decision
supersedes their AC-001-01 pending/blocked status only, without changing REQ/AC.

**2026-10-18 — RabbitMQ remediation LOCAL_DONE / READY_FOR_REVIEW; AC-001-01 BLOCKED.**
[Current report](ac-001-01-windows-remediation.md): authorized failed-session recovery
completed; owner-disabled Resource Saver, stable Engine/bridge. One fresh owned
session proved cold-start/non-root cookie999:999/0400, real AMQP auth/permission
denials, restart/recreate metadata persistence, PG/Redis isolation and controlled/
idempotent cleanup:7 runtime gates plus preflight PASS. Foreign4/5/4 unchanged;
no owned resources remain. Safety43/bootstrap7/guards25/checkout2 PASS this turn.
Earlier SDK41+44/web results retained; libuv cause OPEN/not reproduced, new APK
acceptance BLOCKED. Overall Windows remediation PARTIAL; full NEW-clone acceptance,
publication/hosted fixes CI and AC-001-05 outstanding. No commit/push/PR here;
Feature001/D1-08/Day1 PARTIAL. Earlier entries are historical snapshots.

**AC-001-01 isolated bootstrap — 2026-10-17: LOCAL_DONE / READY_FOR_REVIEW.**
[Decision](../architecture/decisions/ADR-0007-isolated-verification-bootstrap.md),
[local evidence](ac-001-01-isolated-bootstrap-evidence.md),
[runbook](../runbooks/clean-clone-verification.md). Additive verification-only
stack removes the developer-identity cold-clone blocker without changing developer
defaults. Controlled final9 gates, Vite proxy/auth, normal/idempotent owned cleanup
PASS; earlier default-bridge drift/manual recovery remains disclosed OPEN.
Focused safety40/bootstrap7/guards25, architecture23, H2 48/17, PG73/5 incl Task A/C,
Task B5/5+16 gates with disclosed image/cache adapters PASS. No existing developer
source/configuration, migration, dependency or CI behavior changed.
Official AC-001-01 UNVERIFIED until publish/review/merge/develop CI and NEW clone;
AC-001-05 separate. No commit/push/PR here. D1-08/Feature001/Day1 remain PARTIAL.

**D1-08 Task D — 2026-10-17: DONE for the approved isolated CI experiment; AC-001-03 PASS.**
[Evidence](d1-08-ci-red-green-evidence.md): Draft PR #13, actual hosted RED
39006911313 on 73dac74 (real Finance-to-Orders-internal dependency), followed by
corrective c52819f and hosted GREEN39007163856. Architecture23/0, H2 48/17,
PostgreSQL73/5, guards25 per job; exact method/status inventories verified.
Temporary fixture deleted; test/reporter restored to baseline. Final diff is
Task D documentation only. PR stays Draft/unmerged; develop is unchanged.
Task A/B/C and D1-07 remain scoped DONE per integrated owner-approved context.
Earlier headings below are historical snapshots, including old remote/red-CI
UNVERIFIED statements. D1-08/Feature001/Day1 remain PARTIAL: full clean-clone and
independent onboarding, configuration/security acceptance and coverage/risk
disposition remain open. No Task E/F implementation or acceptance change.

**D1-08 Task C —2026-10-17: LOCAL_DONE / READY_FOR_REVIEW for approved scope.**
[ADR-0006](../architecture/decisions/ADR-0006-testcontainers-verification.md),
[evidence/inventory](d1-08-testcontainers-evidence.md). Gate0 and real Windows
Docker canary PASS under approved PostgreSQL/Ryuk pins, non-privileged Ryuk,
socket-only helper, loopback random ports, checks on/no reuse/no Alpine.
Standalone JUnit/JDBC/Flyway suite7 + policy6 PASS; final PG73+5 named skips,
H2 unchanged48+17, architecture23, report/privacy/cleanup guards25, OpenAPI41,
wire27/108 responses/11 operations PASS. Existing harness/V1/V2/business behavior
and required checks unchanged. Task C remote CI UNVERIFIED; no publish authorized.
Task A/B integrated DONE and D1-07 DONE per approved context; older headings below
are snapshots. D1-08/Feature001/Day1 remain PARTIAL: full/independent onboarding,
literal red CI, broader CI and security/config gates remain open.

**D1-08 Task B — 2026-10-17: LOCAL_DONE / READY_FOR_REVIEW for approved scope.**
User approved B-01..B-09, exact Redis/Rabbit pins, Redis AGPLv3 and explicit full
profile; [decision](../architecture/decisions/ADR-0005-foundation-dependencies.md),
[Task B evidence](d1-08-foundation-evidence.md). Existing profiles, Task A JDBC,
V1/V2/11 API/OAS/SDK and CI workflow remain unchanged. Backend default profiles
exclude Redis/Rabbit clients; explicit foundation requires all three dependencies.
Original full001/Day1 acceptance remains PARTIAL. Earlier entries are historical.
Real foundation5/5,16 runtime/config/fault/persistence gates, Arch23, H2 48+17
named skips, PG60+5 named skips, guard17/bootstrap7/OAS41/wire27 and Task A HTTP
regressions PASS; owned cleanup/privacy PASS. A Windows Node/libuv pre-discovery
wire crash is disclosed; unchanged retry PASS, no toolchain fix claimed.
Task B remote CI/publication/integration remain UNVERIFIED; no commit/push/PR.

Task A integration DONE for approved scope: PR10 merged f20c8c6, actual develop
[push run38959330146](https://github.com/phamquoainam/logistics-capstone-project/actions/runs/38959330146)
PASS architecture23, H2 44/12 named skips, PG56/0; guards15 per job. This is
separate from Task B's new local results and its remote CI remains UNVERIFIED.

**D1-08 Task A timeout safety fix - 2026-10-17: local DONE / READY_FOR_PR.**
Removed the unsafe JDBC-wide socketTimeout3s. A small platform infrastructure
Actuator dbHealthIndicator uses the real runtime Hikari pool and JDBC isValid(1),
with native pgJDBC timeout restoration and explicit eviction of invalid or
unrestored connections. Real PG long reads before/after probe, concurrency,
physical reuse, broken/stalled connections, recovery and pool/thread cleanup PASS.
Architecture23; H2 44 PASS/12 exact PG-only skips; PostgreSQL56/no skips;
guards15, contract41 and actual HTTP/runtime gates recorded in
[Task A evidence](d1-08-health-readiness-evidence.md).
Remote verification pending authorized publication; no remote PASS inferred.
Feature001/Day1 PARTIAL; Task B DESIGN/PENDING APPROVAL. D1-07 post-merge
documentation closure remains separate; earlier dated checkpoints are historical.

**Current C-06 verification - 2026-10-17 /1f26854:** C-05 accepted/committed13
files; C-06 LOCAL_DONE. Package85/web24/mobile1 +actual compiler4/Hermes,
wire27/browser7 twice, architecture23/H2 40+8 named skips/PG48 no skips and guard14
PASS. Actual16-file generations/source raw bytes match; owned cleanup/privacy PASS.
[Final evidence/security/CI coverage](d1-07-c06-final-evidence.md).
D1-07 PARTIAL / READY_FOR_PR, remote/integration UNVERIFIED. Mobile audit58/35,
SDK dev4 high OPEN/runtime0, web0/0. C-06 uncommitted/unpublished;
Feature001/Day1 PARTIAL. Previous checkpoint notes are historical.

**Historical C-05 checkpoint — 2026-10-17 /6656753:** C-04 reviewed/committed23
files after safety PASS, no push. C-05 compile-only mobile SDK consumer DONE:
local dependency/public operations and types, no starter/native/business feature
change. Clean install/lint/typecheck/Jest1, real compiler/4 negative controls,
SDK-entry Hermes23 modules/14 SDK, unchanged starter Hermes576/zero SDK and SDK85
regression PASS. Mobile audits59 full/35 omit-dev FAIL/OPEN, SDK dev4 high OPEN.
[Evidence/inventory](d1-07-c05-mobile-evidence.md). C-06 combined/remote gates NOT
STARTED; D1-07/full001/Day1 PARTIAL. C-05 uncommitted/unpublished.
Previous checkpoint statements below retain their historical scope.

**Historical C-04 checkpoint — 2026-10-16 /a2609f9:** C-03 reviewed/committed
exact14 files after safety PASS, no push. C-04 existing web adoption DONE local:
all11 generated operations/types via public exports/local package; unchanged
UI/session/ownership/key behavior. Web clean install/lint/typecheck/build,
24 adapter/7 actual browser tests/no skips, contract/SDK85/wire27 and Maven40
with8 expected PG-only skips PASS. Cleanup/privacy PASS, SDK dev4 high OPEN/web0.
[Evidence/inventory/limits](d1-07-c04-web-evidence.md). C-05/mobile compile-only
and C-06 combined gates NOT STARTED; D1-07/full001/Day1 PARTIAL.
C-04 uncommitted/unpublished. Previous checkpoint notes are historical.

**Historical C-03 checkpoint — 2026-10-15:** exact43-file C-01/C-02 checkpoint
committed locally as b385244 after safety review and85-test PASS, per explicit
user authorization. No push/PR. C-03 actual HTTP/PG DONE: two final fresh owned
PostgreSQL18.6 tmpfs/backend runs,27/27 tests/no skips each;108 HTTP responses
each (34 success/74 negative), all11 operations, zero contract mismatches.
SQL confirms scoped ownership/replay/concurrency; cleanup/privacy PASS for all
attempts, including disclosed early environment/harness failures. Maven verify
40 PASS/eight named PG-only skips; C-01/C-02 regression85 PASS. Dev audit4 high
OPEN/runtime0. [Evidence and remaining limits](d1-07-c03-wire-evidence.md).
C-03 changes remain uncommitted; C-04–06 NOT STARTED; full001/Day1 PARTIAL.
Older NOT RUN/wire-unverified notes below retain their dated checkpoint scope.

**Historical C-01 final acceptance / C-02 checkpoint — 2026-10-15:** same branch/
HEAD20072b6, C-01 preserved and final technical review PASS. User's conditional
authorization opened C-02; generated Fetch SDK16 files/11 operations, strict
package build and NodeNext/Bundler declaration controls PASS. Clean install59
packages; C-0141 + SDK44 tests PASS/no skips; two-directory/source bytes match.
Dev audit FAIL4 high findings OPEN, runtime0; checked-object generation avoids
affected js-yaml loader with actual trap test. [Evidence](d1-07-c02-sdk-evidence.md).
C-01/C-02 local DONE; C-03–06 NOT STARTED; D1-07/full001/Day1 PARTIAL. No commit/
push/PR or business/source/workflow/migration/consumer change. Prior audit0 and
no-SDK notes below describe C-01's earlier graph/checkpoint only.

**Historical D1-07 / C-01 checkpoint — 2026-10-15:** PR#8 merged as20072b6;
existing clean feature/d1-07-demo-contract-client reused at matching develop/
origin/live remote HEAD. C-01 OpenAPI3.1 for11 existing operations and minimal
Redocly2.64.0/Ajv8.20.0/formats3.0.1 tooling implemented. Clean install, lint,
source inventory and41 tests PASS/no skips (37 negative fixtures); reported new
graph audit0 advisories. [Evidence/findings](d1-07-c01-contract-evidence.md).
C-01 local DONE, owner contract review pending before C-02. D1-07/slice PARTIAL;
C-02–06 NOT STARTED. Full001/Day1 remain PARTIAL; no original REQ/AC changed.
Earlier NOT STARTED/no-contract notes below are dated prior checkpoints.

**D1-06 checkpoint — 2026-10-15:** PR #7 merged as
`3186f01f363a378a5c89344fb828b24e282d8243`; develop/local/origin/live remote
matched and were clean before creating `feature/d1-06-slice-readiness`.
Selected exactly one slice: Demo-001 OpenAPI + generated Fetch SDK consumed by
the existing web journey; mobile compile-consumer only. [Gap matrix, candidates,
DEC-29 and DoR](d1-06-slice-readiness.md). The user formally approved the
refined DEC-29 scope/pins on2026-10-15; this supersedes the earlier review state.
**DEC-29 ACCEPTED; slice READY_FOR_IMPLEMENTATION; D1-06 DONE for documented
assessment/selection/DoR.** Source-drift and mandatory planning gates PASS.
Implementation tests remain NOT RUN; D1-07 NOT STARTED. This closure turn does
not install/generate/implement or commit/push/PR. Broad ADR-0003 remains PROPOSED.
Full001/Day1 remain PARTIAL; no original requirement/AC or completed scope changed.
The implementation/publication checkpoints below keep their historical dates.

**D1-02 hiện hành — 2026-10-15: DONE cho scope local toolchain và Android native starter feasibility đã duyệt.**
PR #6 merged `develop/2dcffd5`; user duyệt dùng lại nhánh sạch
`feature/d1-02-toolchain-closure`. Node24.13.0/npm11.6.2 pin và clean installs;
web lint/typecheck/build/5 API tests/3 browser regressions PASS; mobile
lint/typecheck/Jest starter/Android bundle PASS. APK debug package riêng
`com.elogistic.shipper.dev` build/install/render trên **Pixel_7/API37** PASS,
hai cold launches, Metro và no-app-fatal/ANR/JS-fatal trong observation windows.
SDK online patch advisory FAIL và dependency/upstream nonfatal findings vẫn
OPEN, không ghi là đã fix; không đổi Expo/RN/Java/Boot/Maven/DB hoặc giảm AC.
[D1-02 commands, hashes, findings và scope](d1-02-toolchain-evidence.md).
D1-01/02/03/04/05 DONE theo scope; D1-06–08 và toàn Feature001/Day1 vẫn mở.
Các trạng thái D1-02 PARTIAL/Pixel7Pro/chưa tooling phía dưới là lịch sử.
Không commit/push/PR trong execution này; HEAD giữ nguyên2dcffd5.

**Lịch sử closure sau merge D1-05 — 2026-10-14:** D1-05 **DONE** cho scope backend CI đã duyệt.
PR #5 merge vào `develop/f50c8972a3a4d0a9c877d6e7e99d2a918cd866fd`;
[push run 38512345091](https://github.com/phamquoainam/logistics-capstone-project/actions/runs/38512345091)
PASS: architecture 23/no skip, H2 40/tám PG-only skip, PG18 48/no skip;
guard 14 mỗi job, artifacts/digests và required checks enforced PASS.
[D1-05 evidence](d1-05-ci-evidence.md#post-merge-verification-and-official-closure--2026-10-14).
D1-03 **DONE cho PostgreSQL Phases A–E đã duyệt**; mobile tooling và full onboarding
thuộc D1-02/Feature 001, không giữ task PostgreSQL PARTIAL vì các gate khác.
[D1-03 reconciliation](d1-03-postgresql-evidence.md). D1-02, D1-06–08 và toàn
Feature 001/Day 1 còn mở; không giảm AC hoặc ngầm DEFERRED.
Nhánh tài liệu `feature/d1-05-post-merge-closure` tạo từ develop sạch; chưa commit/
push/PR/merge thay đổi mới. Checklist D/evidence hiện hành thay trạng thái audit
cũ bên dưới; các mốc cũ được giữ làm lịch sử.

**Lịch sử sau Demo-001 / D1-03 (2026-10-13):** baseline develop/08c0c34; branch `feature/d1-03-postgresql-foundation`. DEC-26 đã được duyệt; PostgreSQL/Flyway DB milestone PASS, H2 giữ nguyên; assessment khi đó D1-03 PARTIAL. [Evidence và reconciliation hiện hành](d1-03-postgresql-evidence.md), [runbook](../runbooks/postgresql-foundation.md). Các bảng baseline/gap dưới đây lưu lịch sử trước Demo-001; không dùng placeholder cũ để kết luận chưa có Flyway/dependencies.

**Cập nhật spike ngày 13/10/2026:** branch `docs/sdd-baseline-and-runbooks`, HEAD `495813dbd318142f30855451546d313190f11656`, working tree sạch trước khi sửa. Wrapper Windows đã sửa null-array; Maven 3.10.0/Java 21.0.10 chạy `verify` online và offline PASS, Surefire xác nhận `contextLoads` chạy thật. Chỉ backend toolchain slice đạt gate; foundation chưa DONE. Kết quả chi tiết nằm tại [runbook, mục L.4](../runbooks/local-development.md#l4-toolchain-spike-t-001-01--d1-02).

**Verification D1-02 tiếp theo, cùng ngày:** Node 24.13.0/npm 11.6.2, lock/dependencies, mobile typecheck, Metro và Android bundle PASS. Android render BLOCKED vì không có device/AVD; lint và development build chưa có tooling/config được duyệt. Spring Modulith 2.1.1 có metadata khớp Boot 4.1.1/Java 21 nhưng chưa integration test hoặc quyết định chọn tooling. D1-02 vẫn PARTIAL; [bằng chứng L.5](../runbooks/local-development.md#l5-d1-02-nodenpm-expoandroid-và-modulith-verification) thay trạng thái chưa kiểm tra ở mốc trước. Working tree đầu phiên có sẵn ba file từ spike; phiên này chỉ đổi hai Markdown, giữ nguyên Wrapper.

Baseline audit trước spike: branch `develop`, HEAD `7d0a7fa35d3c60c9fdfeb38fec4d588ff414fdee`, có tài liệu chưa commit. Metadata/lệnh FAIL của mốc này được giữ riêng tại runbook L.1–L.2.

**Android runtime & closure review, 13/10/2026:** môi trường đã thay đổi: AVD `Pixel_7_Pro`, `emulator-5554` online, Android 17/API 37, boot completed. Metro và kết nối trực tiếp từ emulator tới `/status` PASS; render vẫn BLOCKED vì chưa cài Expo Go (`host.exp.exponent`), lệnh mở client không resolve được Intent. Không cài client hoặc đổi hệ thống. [Bằng chứng và closure matrix L.6](../runbooks/local-development.md#l6-d1-02-android-runtime--closure-review) là trạng thái mới nhất; L.5 giữ lịch sử. ArchUnit-only, Expo Go smoke target, Node/npm pin và tách lint/test/native build vẫn PROPOSED; không có gate nào được DEFERRED hoặc miễn theo phê duyệt. D1-02 vẫn PARTIAL, chưa DONE.

**Expo Go/runtime smoke mới nhất, 13/10/2026:** người dùng đã cho phép cài client chính thức SDK 57 và smoke. Đầu phiên client 57.0.9/versionCode 444 đã có sẵn; APK pull từ emulator khớp SHA-256 release chính thức và signature verify PASS, không cần cài lại. Mở `exp://10.0.2.2:8081` thành công; screenshot/UI hierarchy xác nhận text starter, React chạy main bằng manifest SDK 57 và kết nối Metro còn ESTABLISHED sau gần 4 phút. Render/client/network PASS. Log review PARTIAL: có lỗi init của Expo Go và UWB-service crash thuộc emulator, chưa xác định root cause; không thấy client crash/ANR/JS fatal trong observation window. [L.7](../runbooks/local-development.md#l7-d1-02-expo-go-provenance--android-starter-smoke) thay trạng thái mới nhất, L.4–L.6 giữ lịch sử. D1-02 vẫn PARTIAL vì accepted tooling/pin, native build, lint/test/web và các phát hiện còn mở; chưa có DEFERRED hoặc scope change được duyệt.

## A. Mục tiêu và phạm vi

“Day 1” là phiên làm việc tiếp theo trên repository hiện hữu: kiểm kê code, xác minh baseline, xác định phần còn thiếu và chọn lát cắt triển khai theo SDD. Giữ backend Spring Boot và Expo app hiện có; chỉ bổ sung foundation khi có gap được chứng minh. Không cam kết hoàn thành P0 trong một ngày.

Phiên baseline ban đầu chỉ cập nhật hai Markdown. Phiên spike hiện tại được người dùng duyệt sửa thêm `app/backend/mvnw.cmd`, chạy backend baseline và cập nhật bằng chứng trong hai Markdown này. Giữ Java 21, Spring Boot 4.1.1, Maven distribution 3.10.0; không thêm dependencies, đổi global settings, triển khai business feature/migration/database, commit hoặc push. Foundation được duyệt triển khai theo milestone, ưu tiên backend build và PostgreSQL/Flyway trước Redis/RabbitMQ; acceptance criteria hiện có giữ nguyên.

Phiên environment verification kế tiếp chỉ cập nhật hai Markdown; không đổi source/dependencies/build config, không prebuild/cài SDK/tạo credentials. Expo lint đã tự thử setup dependency ngoài dự kiến; tiến trình được dừng và manifest khôi phục byte-for-byte theo snapshot, lockfile/dependencies không đổi. Lint vẫn BLOCKED, không nhận kết quả từ auto-setup.

Chuyển sang business code khi [Definition of Ready](../codex/definition-of-ready.md) đạt cho slice: dependency và hợp đồng công khai đã review, blocker liên quan đã chốt hoặc phần độc lập được tách rõ, fixture/test/gate xác định, baseline chạy được. Hoàn tất feature phải đạt [Definition of Done trong test strategy](../quality/test-strategy.md#definition-of-done-per-feature), không chỉ có controller hoặc UI.

## B. Baseline repository hiện tại

### B.1. Quy ước bằng chứng

| Trạng thái | Cách dùng trong tài liệu này |
| --- | --- |
| `IMPLEMENTED` | Có hiện thực và bằng chứng phù hợp với phạm vi nhỏ được nêu; không suy ra feature P0 hoàn thành. |
| `PARTIAL` | Có một phần hiện thực, chưa đủ yêu cầu. |
| `NOT_IMPLEMENTED` | Không tìm thấy hiện thực sau kiểm tra source/config, kể cả file ẩn ngoài dependency/build. |
| `UNVERIFIED` / `[UNVERIFIED]` | Có code/cấu hình hoặc lệnh được hỗ trợ nhưng hoạt động chưa được chứng minh. |
| `BLOCKED` / `[BLOCKING]` | Gate/business code chưa thể tiếp tục vì điều kiện/quyết định cụ thể còn thiếu. |

`[PROPOSED]` là phương án để review. Phiên bản trong manifest không tự thành quyết định `ACCEPTED`. Thư mục `.gitkeep`, dependency đã cài và `target/` có sẵn không phải bằng chứng runtime thành công.

### B.2. Ứng dụng, công cụ và hạ tầng

| Thành phần | Bằng chứng thực tế | Hiện trạng và kiểm chứng |
| --- | --- | --- |
| Backend | [pom.xml](../../app/backend/pom.xml), [BackendApplication.java](../../app/backend/src/main/java/com/elogistic/backend/BackendApplication.java) | `PARTIAL`: một artifact `com.elogistic:backend:0.0.1-SNAPSHOT`, Java 21, Spring Boot parent **4.1.1**; chỉ Web MVC và Web MVC test starters. Wrapper `verify`/context smoke PASS; HTTP server/runtime `UNVERIFIED`. |
| Build backend | [wrapper properties](../../app/backend/.mvn/wrapper/maven-wrapper.properties), [mvnw.cmd](../../app/backend/mvnw.cmd) | Wrapper **3.3.4**, distribution Maven **3.10.0** giữ nguyên. Null-safe cache target sửa tối thiểu; `--version`, cache thường có khoảng trắng và junction thật PASS. `verify` online rồi offline PASS với explicit user repository và quyền tạo cache lock. Xem L.4; không dùng Maven hệ thống để thay gate Wrapper. |
| Backend config | [application.properties](../../app/backend/src/main/resources/application.properties) | Chỉ `spring.application.name=backend`. Không profile local, datasource, security, port hoặc health/readiness tùy chỉnh. |
| Web độc lập | [app/frontend/web](../../app/frontend/web) | `NOT_IMPLEMENTED`: chỉ `.gitkeep`; không manifest, lockfile, entry point hoặc build tooling. |
| Mobile | [package.json](../../app/frontend/shipper-mobile/package.json), [App.tsx](../../app/frontend/shipper-mobile/App.tsx), [index.ts](../../app/frontend/shipper-mobile/index.ts), [app.json](../../app/frontend/shipper-mobile/app.json) | `PARTIAL`: Expo starter đăng ký `App`, màn hình mẫu; package/name/slug hiện là `web` dù nằm ở `shipper-mobile`. Chưa có luồng shipper. |
| Mobile stack | [package-lock.json](../../app/frontend/shipper-mobile/package-lock.json), [tsconfig.json](../../app/frontend/shipper-mobile/tsconfig.json) | npm lockfile v3; Expo **57.0.27**, React/React DOM **19.2.3**, RN **0.86.3**, TS **6.0.3**, `@types/react` **19.2.18**; installed direct dependencies khớp lock. Node **24.13.0**/npm **11.6.2** tương thích engines; project chưa pin engines/packageManager. Typecheck/Metro/bundle/starter render PASS tại L.7; logs PARTIAL, dev build UNVERIFIED. |
| Mobile scripts | `start`, `android`, `ios`, `web` trong manifest | Gọi `expo start` với tùy chọn tương ứng. Không script build/lint/typecheck/test, Expo Router dependency, native project hoặc EAS config. Expo web preview không thay web nghiệp vụ độc lập. |
| PostgreSQL/Flyway/seed | [db/migration](../../app/backend/src/main/resources/db/migration), [infra/postgres](../../infra/postgres) | `NOT_IMPLEMENTED`: chỉ `.gitkeep`; không SQL, JDBC driver, persistence/Flyway dependency, migration plugin, datasource hoặc seed script. Chưa migration strategy hoạt động. |
| Compose/cache/broker/storage | [infra](../../infra), POM và source | `NOT_IMPLEMENTED`: không Compose YAML, service/profile/image/healthcheck hay adapter Redis/RabbitMQ/POD. `.env.example` chưa nối vào ứng dụng. |
| Contracts/client | [package/api-client](../../package/api-client), `specs/openapi/`, `specs/events/` | Client chỉ `.gitkeep`, thư mục contracts cũ không có schema; `contracts/` chưa có. Không controller/API nghiệp vụ, schema hoặc generated client. |
| Backend tests | [BackendApplicationTests.java](../../app/backend/src/test/java/com/elogistic/backend/BackendApplicationTests.java) | Context-load **PASS**: Surefire 3.5.6 ghi 1 test, 0 failures/errors/skipped; report mới sau thời điểm chạy. Không assertion nghiệp vụ/DB integration/architecture test. |
| CI/quality/scripts | [.github/workflows](../../.github/workflows), [quality](../../quality), [scripts](../../scripts) | `NOT_IMPLEMENTED`: chỉ `.gitkeep`; chưa CI YAML, E2E/load/mobile harness, fixtures hoặc verify/seed/recovery script. |

### B.3. Nine domain modules: mục tiêu và package thực tế

Base package là `com.elogistic.backend`, khác package ví dụ `com.example.elogistics`. Source main chỉ có `BackendApplication.java`. **Cả chín domain đều `NOT_IMPLEMENTED` về nghiệp vụ**; các thư mục dưới chỉ `.gitkeep`, chưa được kiểm chứng boundary.

| Domain theo [Module Catalog](../architecture/module-catalog.md) | Package dưới `app/backend/src/main/java/com/elogistic/backend/` | Nhận định |
| --- | --- | --- |
| Identity | `identity/api`, `identity/internal` | Có bố trí thư mục; chưa authentication/RBAC/object scope. |
| Merchant | Không `merchant/` | Chưa profile, pickup point/address flow. |
| Order | `orders/api`, `orders/internal` | Tên số nhiều; chưa shipment/state/snapshot. |
| Pricing | Không `pricing/` | Chưa tariff/version/quote/cache. |
| Warehouse | `wms/api`, `wms/internal` | Tên khác catalog; chưa stock/reservation/fulfillment. |
| Finance | `finance/api`, `finance/internal` | Chưa ví/ledger/VNPay/COD. |
| Dispatch | `dispatch/api`, `dispatch/internal` | Chưa assignment/manifest/custody. |
| Routing | Không `routing/`; có `shipping/api`, `shipping/internal` | Không đủ code xác định `shipping` thuộc domain nào; không tự đồng nhất với Routing/Order. |
| Notification | `notifications/api`, `notifications/internal` | Tên số nhiều; chưa notification/outbox/inbox. |

`platform/` có thư mục kỹ thuật rỗng; `workflow/` chưa có. Chưa Spring Modulith, ArchUnit, `package-info.java` hoặc test phát hiện import `othermodule.internal.*`. Review mapping trước, không đổi tên hàng loạt theo blueprint.

### B.4. Nguồn và giới hạn audit

Đã đối chiếu [AGENTS.md](../../AGENTS.md), [PRD](../prd.md), [principles](../principles.md), toàn bộ [domain](../domain), [acceptance](../quality/acceptance.md), [test strategy](../quality/test-strategy.md), DoR, kiến trúc/ADR, [dependency order](dependency-order.md), [open questions](../decisions/open-questions.md), [failure recovery](../runbooks/failure-recovery.md) và spec/design/tasks liên quan. Bộ 16 feature vẫn ghi `DRAFT_SPEC/NOT_IMPLEMENTED`; feature 001 thực tế có một phần bootstrap, chưa đạt AC-001.

Không tìm thấy DOCX báo cáo trong repository; chưa đối chiếu trực tiếp bản gốc có thẩm quyền cao nhất. Các Markdown bắt buộc đều có; không file DoD riêng, DoD nằm trong test strategy. Chưa `README.md`, Compose executable, web manifest, contract schema hoặc seed executable; không dùng nội dung giả định thay bằng chứng.

## C. Gap analysis và mức độ ưu tiên

Mức độ phản ánh ảnh hưởng tới phát triển/nghiệm thu, không phải kết luận lỗi của nghiệp vụ đã chạy.

| ID | Mức | Gap và bằng chứng | Yêu cầu/gate ảnh hưởng | Hướng xử lý |
| --- | --- | --- | --- | --- |
| GAP-01 | Critical | Chưa persistence, migration, transaction/constraint, idempotency, ledger/reservation/workflow; POM chỉ Web MVC | BR-02/06–08/13–18; NFR-C01/C02; `005/006/007/011` | Chốt ownership/money/lock/migration contracts, test PostgreSQL thật. Chặn confirm/thu tiền/giữ tồn; không mock nghiệp vụ để vượt gate. |
| GAP-02 | Critical | Identity rỗng, không security dependency/config | FR-A01/A02, NFR-S01–05; `002` | Review DEC-03/04 và role/object scope trước API nghiệp vụ; frontend không thay authz server. |
| GAP-03 | High | Wrapper/verify và Node/mobile typecheck/Metro/bundle/starter render PASS; logs PARTIAL với init findings. Modulith 2.1.1 metadata phù hợp stack, chưa được chọn hoặc chạy trong repo | DEC-01/02; REQ-001-01/05 | Giữ versions; cần review log findings, dev-build, tooling/pin decision và clean build trên máy khác. Không suy ra library integration từ metadata hoặc native feature readiness từ starter. |
| GAP-04 | High | Không Compose/DB/health/seed; template env không consumer | AC-001-01/02/04/05, AC-06; NFR-O02/O03 | Chốt version/wiring cần cho slice, thêm hạ tầng có smoke/migration evidence. Không cài broker/cache chỉ để đủ sơ đồ. |
| GAP-05 | High | Mapping chưa đủ nine domains; architecture test chỉ `.gitkeep` | ADR-0001; REQ-001-03; NFR-M01 | Review mapping/public API; test phải fail khi tạo boundary violation. Giữ backend hiện có. |
| GAP-06 | High | Web chỉ thư mục; mobile starter thiếu native scan/POD/GPS/offline | AC-01/02; FR-S01–05; DEC-02/15/16/18/23 | Xác minh starter, native spike sau quyết định; tạo web khi task/baseline được duyệt. |
| GAP-07 | High | Không approved contracts/client, CI, integration/concurrency/E2E/fault tests | REQ-001-04; AC-04/07; DoR/DoD | Review DEC-25/27, ADR-0003; test cùng slice và lưu evidence thật. Context-load không chứng minh invariants. |
| GAP-08 | Medium | Blueprint dùng `apps/`, `packages/`; code dùng `app/`, `package/` | NFR-O03; kế hoạch/runbook cũ | Dùng đường dẫn thực trong hai file; review docs/spec khác ở task riêng, không ép chuyển thư mục. |
| GAP-09 | Medium | Không DOCX tại repo, runtime/evidence chưa có | Thứ tự thẩm quyền, AC-07; DoD | Cần bản báo cáo khi review nghiệp vụ; cập nhật traceability theo evidence trong phiên có phạm vi cho phép. |
| GAP-10 | Low | Mobile package/name/slug là `web`, routes rỗng chưa Router | Khả năng hiểu cấu trúc local | Ghi rõ runbook; đổi metadata/navigation khi có task, không viết lại starter để sửa tên. |

## D. Ưu tiên Day 1 và checklist thực thi

Checklist assessment này không thay checklist feature. D1-01/02/03/04/05/06 DONE theo scope có evidence; D1-07 là task tiếp theo chưa tick. Full Feature001 vẫn cần remaining contracts implementation, infrastructure, independent onboarding và full quality gates; không tick full feature từ các milestone độc lập.

- [x] **D1-01 — An toàn repository và kiểm kê baseline** — ưu tiên Critical; phụ thuộc: không. File: `AGENTS.md`, manifests, source/config, docs/specs. Gate: ghi branch/HEAD/dirty state, executable/placeholder mà không hủy thay đổi. Bằng chứng: `git status --short`, `git branch --show-current`, `git rev-parse HEAD`, `git ls-files`, `rg --files --hidden --no-ignore` với loại trừ dependency/build/Git; bảng B đã lập. Chỉ audit hoàn thành, không phải foundation.

- [x] **D1-02 — Xác minh môi trường/toolchain** — **DONE cho Phases B–F/local toolchain + Android native starter feasibility được duyệt**: exact Node24.13.0/npm11.6.2 pin/engine-strict; web/mobile clean installs, lint/typecheck/tests, web build và Android export PASS. Native APK build6m58s/install/signature/package riêng/render/Metro/no fatal trong hai cold launches Pixel_7/API37 PASS. Root causes/disposition của ::1 binding, Go init/headless/UIManager, native DevLauncher colorScheme/ReactHost soft exception và emulator UWB được ghi theo đúng process. Online latest-patch check FAIL advisory, audit/deprecation/upstream fixes OPEN; không nâng framework hoặc tuyên bố zero warnings/production security. [Evidence](d1-02-toolchain-evidence.md). Full other-developer onboarding/native business capabilities thuộc Feature001/task riêng, không được ngầm miễn.

- [x] **D1-03 — Xác minh build/test/migration** — **DONE cho PostgreSQL Phases A–E được duyệt**: DEC-26, infrastructure/profile/JDBC/Flyway V1/V2, ownership/no-DDL, cold-start/validate/rerun/upgrade/restart persistence, concurrency/idempotency/rollback và PostgreSQL-backed Demo-001 web/H2 regression PASS. Hosted develop/f50c897 verify PG18 48/no skip, H2 40/tám PG-only skip, architecture 23 PASS. Backend toolchain prerequisite đã PASS; mobile/pin/lint và full onboarding thuộc D1-02/Feature 001. [Scope/evidence](d1-03-postgresql-evidence.md). Không sửa AC hoặc miễn Testcontainers/full cold-start.

- [x] **D1-04 — Review kiến trúc/bảo mật** — **DONE cho scope local đã duyệt**: reviewer chấp thuận và merge vào develop/2c609c4 theo xác nhận người dùng, đã kiểm chứng ancestry/source. Mapping nine domains, 37 production classes thuộc identity/merchant/orders/platform; sáu business modules chưa implement không tính PASS. 23 architecture tests, 11 negative violations/one positive fixture; adapter direction và minimal trusted actor theo ADR-0004. Auth/object scope, log/fail-fast/bounded secret audit và H2/PG/browser regressions PASS. [Evidence gốc](d1-04-architecture-evidence.md), [merge/CI reconciliation](d1-05-ci-evidence.md). CI/full security/provider/native và full feature AC vẫn là gates riêng, không miễn hoặc hạ tiêu chí.

- [x] **D1-05 — CI Architecture Gate & Clean Runner Verification** — **DONE**: người dùng review/merge PR #5; merge f50c897, actual push develop run 38512345091/attempt 1 PASS. Architecture 23/23; H2 40 PASS/tám named PG-only skips; PG18 48/48, zero skips; guard 14 mỗi job. Artifacts digests/named inventory/privacy PASS. Ruleset 25049940 Active, target develop, ba required GitHub Actions checks/strict policy/no bypass, effective rules verified. [Official closure](d1-05-ci-evidence.md#post-merge-verification-and-official-closure--2026-10-14). Không tuyên bố deliberate-red workflow experiment hoặc sáu absent modules đã kiểm chứng; original Feature 001 AC còn nguyên.

- [x] **D1-06 — Chọn slice và kiểm tra DoR** — **DONE for assessment/selection/documented DoR,2026-10-15**: explicit user DEC-29 approval recorded; source-drift recheck PASS at3186f01. Exactly one slice READY_FOR_IMPLEMENTATION: existing11-operation Demo-001 OpenAPI + private generated Fetch SDK/web adoption, mobile compile-only. [Spec/design/tasks](../../specs/001-platform-foundation/slices/demo-contract-client/spec.md) contain accepted scope/pins, ownership/API/security, SC/AC mapping, fixtures, drift/determinism/rollback, tasks/file scope/DoD. No new source/dependencies/generated SDK or full001 DONE; D1-07 tests NOT RUN.

- [ ] **D1-07 — Test-first và gate slice** - **PARTIAL / READY_FOR_PR**: C-01-06 local deliverables verified; package85/web24/mobile1, wire27/browser7 twice, architecture23/H2 40+8 named skips/PG48+0/guard14 PASS. [C-06 evidence](d1-07-c06-final-evidence.md). Audits OPEN; actual remote required checks/integration pending. No publish or official integrated DONE claim.

- [ ] **D1-08 — Chốt kết quả/evidence/blocker** - **PARTIAL**: D1-01-06 scoped DONE; D1-07 local verification ready for publication/review. Remote PR/CI/integration, full001 REQ/AC, infrastructure/readiness/independent runbook reproduction and final Day1 review remain open.

## E. Chiến lược triển khai feature

Theo [dependency order](dependency-order.md), [feature register](../../specs/INDEX.md) và audit, việc tiếp theo là **review T-001-01, spike tháo blocker của foundation hiện có**, rồi bổ sung enabler còn thiếu. Không mặc định hoàn thành/xây lại `001/002` theo tên thư mục.

| Bước | Slice sau khi đủ điều kiện | Dependency/quyết định | Bất biến và test |
| --- | --- | --- | --- |
| 1 | [001 foundation](../../specs/001-platform-foundation/spec.md): kiểm chứng/bổ sung build, DB migration, boundary, health/runtime | [Design](../../specs/001-platform-foundation/design.md), [tasks](../../specs/001-platform-foundation/tasks.md); DEC-01/02/25/26, mapping packages | Một artifact, runtime không DDL, migration không phá dữ liệu, boundary test thật; giữ backend/mobile hiện có. |
| 2 | [002 Identity](../../specs/002-identity-access/spec.md): phiên/quyền đối tượng trong slice review | Foundation cần thiết; [design](../../specs/002-identity-access/design.md), [tasks](../../specs/002-identity-access/tasks.md); DEC-03/04 | Unauthorized/expired/revoked/cross-scope bị từ chối; Admin không mặc nhiên Finance. |
| 3 | [003 Merchant/address](https://github.com/phamquoainam/logistics-capstone-project/blob/eeb7833ab678240581c407ccd167d7e61ea873c6/specs/003-merchant-address/spec.md) → [004 Pricing](https://github.com/phamquoainam/logistics-capstone-project/blob/eeb7833ab678240581c407ccd167d7e61ea873c6/specs/004-pricing-engine/spec.md) | Identity; DEC-05/06/07/08; dữ liệu địa chỉ/vùng/giá chuẩn | BR-19, BR-10–12: địa chỉ gốc/version, quote expiry/input/breakdown/version, golden rounding; cache không thay DB. |
| 4 | [005 DRAFT](https://github.com/phamquoainam/logistics-capstone-project/blob/eeb7833ab678240581c407ccd167d7e61ea873c6/specs/005-shipment-lifecycle/spec.md) phần độc lập sau contract; [006 Finance](https://github.com/phamquoainam/logistics-capstone-project/blob/eeb7833ab678240581c407ccd167d7e61ea873c6/specs/006-wallet-payments/spec.md), [007 WMS](https://github.com/phamquoainam/logistics-capstone-project/blob/eeb7833ab678240581c407ccd167d7e61ea873c6/specs/007-warehouse-wms/spec.md) chuẩn bị confirm | DEC-09/10/11/12/13/28; chưa confirm khi Finance/WMS cần thiết chưa ready | Owner trusted, replay/state guard; exact money, ledger append-only, tồn khả dụng không âm, reservation conservation. |
| 5 | `005` confirm/cancel xuyên module | Pricing + Finance + Warehouse public API; workflow cùng transaction manager, lock order review | Quote + debit + hold nếu WMS + shipment/snapshot/idempotency cùng local DB transaction; thiếu tiền/tồn rollback; retry/concurrent không trừ/hold hai lần; hủy phần chưa dùng. |
| 6 | [008 Dispatch/Hub](https://github.com/phamquoainam/logistics-capstone-project/blob/eeb7833ab678240581c407ccd167d7e61ea873c6/specs/008-dispatch-hub/spec.md) → [009 Shipper/POD](https://github.com/phamquoainam/logistics-capstone-project/blob/eeb7833ab678240581c407ccd167d7e61ea873c6/specs/009-shipper-pod/spec.md) → [010 Routing/GPS](https://github.com/phamquoainam/logistics-capstone-project/blob/eeb7833ab678240581c407ccd167d7e61ea873c6/specs/010-routing-location/spec.md) → [011 COD](https://github.com/phamquoainam/logistics-capstone-project/blob/eeb7833ab678240581c407ccd167d7e61ea873c6/specs/011-cod-reconciliation/spec.md) | Order/Finance/WMS; Routing cần Dispatch; DEC-14–19, native/provider decisions | Một assignment active/chặng; WMS outbound ≠ hub inbound; delivery/POD/COD nhất quán; GPS/route không sửa assignment; COD đủ điều kiện, kỳ CLOSED bất biến. |
| 7 | `012` notification/dashboard, `013` admin, `014` import, `015` offline; chuẩn bị `016` evidence | Prerequisites từng spec; DEC-20–24/27, core contracts | Sau commit + dedupe/inbox; scope dashboard/recovery; import chỉ draft; offline giữ operation ID/revalidate; RQ dataset cố định; P1 sau P0. |

Không triển khai API/DTO/table `[PROPOSED]` như contract đã duyệt. Không tính số dư từ body, thay DB constraint bằng Redis lock/queue/frontend, hoặc gọi provider mạng trong transaction tiền/tồn. Draft/starter chạy được chưa phải AC-02 end-to-end.

## F. Verification gates

Các lệnh hiện có nằm trong [runbook](../runbooks/local-development.md); lệnh được hỗ trợ khác kết quả đã chạy. Gate chưa có công cụ/test báo `NOT_IMPLEMENTED`, không đặt lệnh giả.

| Gate | Bắt đầu khi | Hoàn tất khi | Hiện trạng |
| --- | --- | --- | --- |
| Ready/contract | Spec → design → tasks review; blocker chốt; contract/fixture rõ | REQ/AC/test/evidence truy vết, contract đúng schema approved | `BLOCKED`: DRAFT specs, schema/client chưa có. |
| Build | JDK/Wrapper/dependencies truy cập được | Backend `.\mvnw.cmd verify`; mobile `node .\node_modules\typescript\bin\tsc --noEmit --incremental false`; web theo scripts thật sau manifest | Wrapper verify PASS tại L.4; mobile typecheck và Android JS bundle PASS tại L.5, không phải APK build; web chưa có. |
| Unit/context/integration | Test tồn tại; DB thử riêng khi persistence | Exit 0/report thật, assertions theo BR/transaction/constraint | Context-load PASS (1 test); domain/DB tests chưa có. |
| Architecture | Mapping/boundary policy review | Test bắt internal import/cycle/cross-owner write, PASS khi code hợp lệ | Chưa test/dependency; rà starter không chứng minh nine domains. |
| Migration | Flyway/driver/config/SQL/DB/version/credentials chốt | Empty/upgrade/re-run an toàn; Hibernate validate nếu ORM dùng; runtime không DDL | `NOT_IMPLEMENTED`, không CLI/plugin command hiện hữu. |
| API/authz | Approved schema/auth decision/account-scope fixtures | Contract/unauthorized/cross-tenant/revocation tests PASS, không lộ dữ liệu | Chưa có; `/api/v1`, `/actuator/health` vẫn đề xuất. |
| Business invariants | Owner/transaction/lock/unique/check/idempotency thiết kế | Happy/negative/duplicate/concurrent/timeout/restart/rollback PASS, không vi phạm BR trong kịch bản công bố | `NOT_IMPLEMENTED`, không mock tiền/tồn để thay gate. |
| Native/E2E/CI | Device/browser target công bố, harness có | UI đúng server state, permission/offline/restart thật, CI/evidence | Expo Go starter render/client-to-Metro PASS tại L.7; logs PARTIAL/init findings. Dev-build/permissions/offline UNVERIFIED; chưa harness/workflow, không thay P0 feature E2E. |

## G. Rủi ro, blocker và quyết định

| Loại | Nội dung | Điều kiện tiếp tục |
| --- | --- | --- |
| Chấp thuận theo báo cáo | [ADR-0001](../architecture/decisions/ADR-0001-modular-monolith.md): một backend, Package by Domain, local ACID, một PostgreSQL với ownership | Giữ định hướng; boundary/atomicity tests chứng minh. |
| Chấp thuận một phần | [ADR-0002](../architecture/decisions/ADR-0002-database-ownership.md): Flyway/ownership theo báo cáo, migration details PROPOSED | Review DEC-26, DB version/stream/order và runtime/migration accounts trước DDL. |
| Đề xuất chưa duyệt | [ADR-0003](../architecture/decisions/ADR-0003-contract-and-idempotency.md), DEC-25/27/28 | Review API/error/idempotency/event/workflow; không chọn wire format/provider bằng phỏng đoán. |
| `[BLOCKING]` gần nhất | DEC-01/02: wrapper/build/native baseline; DEC-03/04: auth; package mapping chưa rõ | Owner/reviewer và evidence trong [decision register](../decisions/open-questions.md); source version không tự đóng decision. |
| `[BLOCKING]` theo domain | DEC-05–07/09–13/15–17/19/22–24, CONFIG chưa được duyệt | Chốt theo slice, provider theo decision; không bắt mọi provider chốt trước spike độc lập. |
| Giới hạn môi trường | Client Expo Go SDK 57 đã có, APK chính thức và starter render/network PASS. Expo Go init exceptions và emulator UWB-service crash được ghi riêng, root cause UNVERIFIED | Không sửa client/emulator/system/source để che log. Cần review findings và approval tooling/dev-build/pin/milestone; Docker chưa trên PATH. |
| Giới hạn nguồn | Chưa DOCX tại repo; docs mới và `.gitkeep` bị xóa sẵn | Đối chiếu báo cáo khi review nghiệp vụ; giữ toàn bộ thay đổi, không checkout/reset để làm sạch. |

## H. Đầu ra kỳ vọng và điều kiện tiếp tục

Baseline và spike cung cấp version/path/evidence, gap, task/dependency/gate và runbook PowerShell có PASS/FAIL/NOT_RUN. Không tự tick feature DONE hoặc sửa ADR/traceability ngoài phạm vi ba file đã duyệt.

Backend toolchain spike của **T-001-01 / D1-02** đã thực hiện; bằng chứng REQ/AC nằm tại runbook L.4. Không sửa feature tasks, ADR hoặc [traceability](../quality/traceability.md) vì ngoài ba file được phép. T-001-01 và feature 001 chưa DONE.

**D1-02 hiện hành:** local tooling/native starter feasibility DONE. Follow-up SDK patch/dependency advisories và upstream library/emulator findings được theo dõi riêng, không tự áp major/downgrade. D1-06 đã chốt DoR theo DEC-29 ACCEPTED; implementation kế tiếp là D1-07 slice001-CONTRACT, chưa bắt đầu trong lượt closure. Không triển khai lại auth/DRAFT Demo-001.

**D1-03 hiện hành:** PostgreSQL Phases A–E DONE; build/DB/architecture/hosted CI PASS. Review DEC-26/chưa migration ở các mốc cũ là lịch sử. D1-02 và full onboarding/native/contract gates vẫn giữ trong đúng task; không tự chuyển business slice sang READY.

<a id="i-d1-02-closure-plan-sau-d1-05--proposed"></a>
## I. D1-02 Closure execution — 2026-10-15

Kế hoạch PROPOSED trước đây đã được user duyệt triển khai Phases B–F và có
bằng chứng thực thi trong [D1-02 evidence](d1-02-toolchain-evidence.md).

| Phase | Mandatory execution gates | Assessment |
|---|---|---|
| B | Baseline-compatible exact Node/npm pins, engines/packageManager, effective strict engines | PASS |
| C | Web/mobile lockfile clean install, real lint/typecheck/tests, web build and Android bundle | PASS:5 web API,3 browser,1 mobile lifecycle test |
| D | Pixel7 AVD/SDK/JDK, native APK compile/signature/install/separate package/render/Metro/no app fatal during observations | PASS:304 Gradle tasks; two cold launches, distinct from Expo Go |
| E | Findings attributed and impact/reproducibility recorded | PASS for starter disposition; upstream fixes/security review remain OPEN, not claimed fixed |
| F | Evidence/traceability and scoped status reconciliation | DONE local; no commit/push/PR |

Online Expo latest-patch diagnostic remains **FAIL advisory** against deliberately
retained57.0.27/57.0.16, not a failed engine/peer/compile/runtime test. Offline
validation explicitly warns about reliability and is not substituted for online
PASS. No mandatory requirement for latest patch/zero dependency advisories was
invented or removed. Full AC-001-01/05 onboarding and native feature/production
security verification remain OPEN under Feature001.

**Next priority: D1-07 publication and remote verification**, only after separate
user authorization. Review/commit the valid C-06 allowlist, normal push and one PR
to develop, inspect three actual HEAD checks/artifacts/conflicts/effective rules.
C-01-06 local verified; advisory findings OPEN. Do not merge or mark full001/Day1
DONE. No additional feature slice or dependency upgrade started.
