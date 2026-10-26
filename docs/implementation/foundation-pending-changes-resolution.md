# Foundation pending changes — reconciliation 2026-10-27

**Phạm vi:** T-001-10 / traceability và disposition của nội dung chưa commit;
REQ-001-05, AC-001-04 subset và AC-001-05 evidence boundary.
Đây là đề xuất docs-only cho Owner review. Không phải nghiệm thu AC-001-04/05,
không đánh dấu Feature001/D1-08/Day1 DONE, không phê duyệt xóa file gốc.

## Baseline và bảo toàn

Đã chạy Git preflight đọc và `git fetch origin`. Develop thực tế là
`84e72aca482831dae27239fde1fa65c30cfcb196` ([PR #29](https://github.com/phamquoainam/logistics-capstone-project/pull/29)).
Repo gốc `D:/TLCN/e-logistic` vẫn ở `feature/002-identity-application-ci`,
HEAD `3d9281eaae59bfdd63138ea0c4a1c45f498f1e80`: 0 commit riêng / 29 commit
phía develop. Index rỗng; 1 file tracked sửa (+372 dòng, không xóa) và 8 file
untracked. Không có thay đổi staged. Không suy inventory chỉ có sáu file.

Nội dung đầy đủ của 9 đường dẫn, binary unstaged/staged patches, status,
HEAD và worktree inventory đã được lưu ngoài repo tại thư mục tạm
`elogistics-foundation-pending-d2w3d8ja` dưới TEMP của phiên kiểm kê.
`pending-content.zip` SHA-256:
`514e950a681b060c8720e7a9bb0205d0b8c5f446069eaf8388611c1619bae03d`.
Manifest có SHA-256 từng file; zip chứa bản gốc, không phải bản normalize.
Đây là checkpoint cục bộ, chưa là archive bền vững do Owner quản lý.
Mọi nội dung gốc vẫn giữ tại chỗ; không đọc lại private raw-cache/credential.

| Đường dẫn gốc | Bytes | SHA-256 |
|---|---:|---|
| `docs/quality/traceability.md` | 96276 | `0e992a189ff22e0876d3f7616890b15cf23188abf0e6723022696e6b3bc89eb9` |
| `docs/business-references/E_Logistics_Business_Complete_GroupEF_Full.md` | 98454 | `7bdf80aab7ee77541c01e982f511c77e025acc125bf6a5f58d5b6a3b1aa75422` |
| `docs/business-references/E_Logistics_Role_Module_Business_Capability_Matrix_GroupEF_Full.md` | 81221 | `6cda0cef0a6654281c79ebecb4d03d89add6542f3e3721098b64f1f7fbb01a59` |
| `docs/business-references/E_Logistics_Spec_Roadmap_V2_Proposed.md` | 36661 | `356a6421bddeb69869a8c5fbe1291b240e1854f1a0068b58c5df4e47c8d85a03` |
| `docs/implementation/ac-001-04-s3a-r1-evidence.md` | 209755 | `668733d05374d4216eb5cbce16b6f16d11aaa1069a1c7a7a8f89863daa410658` |
| `infra/verification/fail_fast.py` | 32692 | `ec349558c7aa197663bc5a6ea88e139069a5d4359f164faa2d065ba24c095524` |
| `infra/verification/maven_h2.py` | 133728 | `42198a9be515196b3e3acce57967335ccb8414a98859b633c744345cb0e349eb` |
| `infra/verification/test_fail_fast.py` | 14366 | `c80d775d8f72351744e30cf49f0ce7660bf875f5fe7c71eee278b1965b80d0dc` |
| `infra/verification/test_maven_h2.py` | 223757 | `85a5bc2c0189f9b7b11a1fed98b80bd11561ef5f34f9149748a7005766c03dd3` |

## Quyết định từng file

Phân loại dưới đây áp dụng cho việc tích hợp vào develop hiện tại. OBSOLETE
nghĩa là không đưa công cụ thí nghiệm cũ vào integration này; không đồng nghĩa
finding đã FIXED, runtime đã PASS hoặc được phép xóa bản gốc.

| File (theo thư mục) | Mục đích / task / AC | Phân loại và xử lý đề xuất |
|---|---|---|
| `docs/quality/traceability.md` | 372 dòng lịch sử S3-A/observer/config; T-001-10, REQ-001-05 / AC-001-04 subset | **ADAPT**: thêm checkpoint ngắn vào bản develop hiện tại, liên kết báo cáo này; không chép đè trace Identity/V3 và không diễn giải trạng thái CI cũ là hiện tại. Nguyên bản giữ trong archive. |
| `docs/implementation/ac-001-04-s3a-r1-evidence.md` | Nhật ký S3-A RED/GREEN và nhiều lần sửa observer, S3-B/S4/AC04/05 chưa hoàn tất | **ADAPT**: đối chiếu các kết luận tích hợp bằng báo cáo này; giữ nguyên toàn bộ nhật ký trong archive và repo gốc. Không công bố 3.000+ dòng lịch sử như bằng chứng của revision hiện tại. |
| `infra/verification/fail_fast.py` | Runner thí nghiệm demo/H2/HTTP/BCrypt cho TEST-001-S3A-SEED-01, AC04 subset | **OBSOLETE** cho integration hiện tại: ràng buộc provenance `e8dd52c`, JDK/path/session Windows cũ; không được CI gọi. Fix production và 74 test đã tích hợp. Giữ source để tái kiểm chứng lịch sử, không đổi SHA thành current để tạo PASS mới. |
| `infra/verification/test_fail_fast.py` | 24 control của runner cũ, gồm Job/socket/ACL/privacy | **OBSOLETE** cùng runner; 24/24 PASS hiện tại chỉ chứng minh các control đã chạy, không thay thế fail-fast runtime/current onboarding. Giữ cùng source trong archive. |
| `infra/verification/maven_h2.py` | Observer/containment/report publication của thí nghiệm Maven Windows, TEST-001-S3A-OBS-01 / AC04 subset | **OBSOLETE** cho integration hiện tại: report contract 248/231/17 đã cũ, CLI H2 cố ý chặn; Direct/Wrapper acceptance vẫn OPEN. Reuse vận hành **bị chặn**, không gỡ guard hay mở rộng allowlist trong nhiệm vụ này. |
| `infra/verification/test_maven_h2.py` | 310 source/mock controls cho observer/parser/receipt | **OBSOLETE** cùng harness; 310/310 PASS không chứng minh H2/Wrapper/native runtime hoàn thành. Fixture 248 test không đại diện cho policy 500 test hiện tại; giữ lịch sử, không thêm vào CI như bằng chứng acceptance. |
| `docs/business-references/E_Logistics_Business_Complete_GroupEF_Full.md` | Nguồn Business V3, không phải fix Foundation | **ALREADY_INTEGRATED** qua PR29; từng byte khớp develop. |
| `docs/business-references/E_Logistics_Role_Module_Business_Capability_Matrix_GroupEF_Full.md` | Nguồn Matrix V3, không phải fix Foundation | **ALREADY_INTEGRATED** qua PR29; từng byte khớp develop. |
| `docs/business-references/E_Logistics_Spec_Roadmap_V2_Proposed.md` | Nguồn Roadmap V2 cho reconciliation V3 | **ALREADY_INTEGRATED** qua PR29: bản `.md.gz` giải nén khớp từng byte nguồn; bản đọc `.md` chỉ normalize ba hard-break ở dòng 3–5. Không ghi đè bản đã tích hợp. |

## Bằng chứng production và xung đột với harness lịch sử

`DemoSeedPreflight.java` và `DemoSeedPreflightTests.java` ở root khớp từng byte
với develop. Commit `c59a3f022e28e6ecd28a24f9999d4b983fba5f90` đã đưa hai file
vào lịch sử develop qua [PR #20](https://github.com/phamquoainam/logistics-capstone-project/pull/20),
merge `8e5e1b571fa2ff3587b7e4bc63b0df8dd2cddf47`.
Preflight vẫn là `SmartInitializingSingleton`, profile demo/seed opt-in và
`@DependsOnDatabaseInitialization`; gọi validation, không chuyển seed writes
khỏi transactional runner. Policy CI hiện tại yêu cầu chính xác 74 invocation
preflight trong cả H2 và PostgreSQL; không copy lại Java hay policy cũ.

Ba sanitized artifact của develop run40371523817 có tập identity khớp tuyệt đối
`expected_suites(mode)` hiện tại, không trùng và không thiếu:

| Job | Discovered | PASS | Skip | Failure / Error | 74 preflight |
|---|---:|---:|---:|---:|---|
| Architecture gate | 23 | 23 | 0 | 0 / 0 | Không thuộc job này |
| Backend default / H2 | 500 | 483 | 17 | 0 / 0 | 74 PASS, 0 skip |
| Backend PostgreSQL 18 | 763 | 758 | 5 | 0 / 0 | 74 PASS, 0 skip |

[Actual develop push CI](https://github.com/phamquoainam/logistics-capstone-project/actions/runs/40371523817),
HEAD chính xác `84e72ac`. Artifact chỉ có `summary.json` và `tests.xml` đã làm sạch.
SHA-256 summary: Architecture `0d2b9972f1d8464736228c4ac0cf110ed42a3a0591ac90d4af060fdbe6a3e3dd`;
H2 `26517206b90e99c122a043a37855c83a727076efc2dcd8d19cc70ee3a898d0e2`;
PG `6ddf63ecf864c235975320ae537452b939d50a77af665930963e709007810c40`.

Một compatibility probe không chạy JVM đã đưa artifact H2 hiện tại cho
`guard_reports` cũ, chỉ mock ranh giới `run_owned` trả exit0. Kết quả thực tế:
`INVALID_PUBLIC_SUMMARY`; không có `guard-complete.json`. `verify_h2(None,None)`
vẫn từ chối `LEGACY_H2_ENTRY_BLOCKED_REQUIRES_STRICT_R1` trước mọi launch.
Đây là bằng chứng không tương thích, không phải build backend FAIL và không phải
actual report guard execution. Không sửa constant, test fixture hay bypass guard.
Tracked develop chỉ tham chiếu hai harness trong các bảng bảo toàn lịch sử;
không có import/command gọi chúng trong application, workflow hay launcher hiện hành.

## Security, compatibility và giới hạn nghiệm thu

Diff integration chỉ có báo cáo này và checkpoint traceability. Không thay
Backend/Frontend, CI, security policy, accepted contract, SQL/Flyway migration,
JDK/Maven/provider hay credential store. Không merge commit nhánh lịch sử.
Không cần migration mới/repair; kiểm thử PostgreSQL hiện hành đã chạy các suite
migration/upgrade/ownership/Testcontainers dưới cùng policy, không suy thêm
backward-compatibility guarantee ngoài các case được thực thi.

Giữ nguyên các finding lịch sử: 35 telemetry policy violations, raw-cache/privacy
và legacy ACL/store migration chưa đóng; nguồn payload/creator còn thiếu bằng chứng;
S3AR1-07 LOW/open; observer/Wrapper acceptance OPEN; S3-B/S4 và AC-001-04/05
chưa được nghiệm thu độc lập. PASS của CI application không hồi tố sửa các FAIL đó.
Không truy cập/migrate/xóa credential hoặc raw evidence để xử lý file debt.
Muốn dùng lại observer cũ phải có diff riêng trên baseline hiện tại cùng review
và runtime gates phù hợp; không phải prerequisite phát minh cho mọi business slice.
Business delivery chỉ bị chặn bởi requirement/security dependency thực sự của slice.

## Validation của lần reconciliation

- Root: `python -B -m unittest discover -s infra/verification -p test_fail_fast.py`:
  **24 PASS**, 0 skip; có một synthetic Python child/Windows Job/socket control,
  không chạy JVM/Maven/application.
- Root: `python -B -m unittest discover -s infra/verification -p test_maven_h2.py`:
  **310 PASS**, 0 skip; process-free audit hook, OS boundary mocks.
- Integration worktree: `python -B -m unittest discover -s scripts/ci -p test_verify_reports.py`:
  **128 PASS**, 0 skip; current report-policy/privacy/negative controls.
- Integration worktree: `python -B -m unittest discover -s infra/verification -p test_manage.py`:
  lần đầu **20 PASS / 23 setup ERROR** vì chưa có `.demo`; tạo directory fixture
  bị ignore, không sửa source/assertion, chạy lại **43 PASS**, 0 skip.
- Không chạy lại local backend H2/PG/architecture: production/CI tree không đổi,
  đã có actual develop CI đúng HEAD nêu trên. PR mới vẫn phải qua đủ ba required
  checks; không tái sử dụng kết quả develop làm check của PR.
- Không chạy FE/mobile gates vì không có thay đổi FE/mobile/toolchain/contract.
- `python -B docs/reconciliation/v3/validate.py`: **PASS**, 0 errors;
  371 capability, 30 logical spec, 1.024 legacy records, 674 links, 458 protected
  file và 6 worktree được bảo toàn. Không sửa validator/baseline để đạt PASS.
- `python -B -m unittest discover -s docs/reconciliation/v3 -p test_validate.py`:
  **19 PASS**, 0 skip. Root HEAD/status/index và cả 9 SHA-256 khớp manifest sau kiểm tra.
  CI integration của PR được ghi tại trang PR sau push, không gán PASS trước khi chạy.

## Worktree và kế hoạch hợp nhất repository

Không tạo worktree mới. Tái sử dụng worktree SDD đã sạch sau PR29:
`D:/TLCN/e-logistic-sdd-v3`, tạo nhánh
`docs/foundation-pending-changes-resolution` trực tiếp từ `origin/develop` mới nhất.
Không checkout hoặc update working files tại root; không sửa năm worktree Identity.

| Worktree | Branch trước disposition | Commit riêng so với develop | Status đọc thực tế |
|---|---|---:|---|
| e-logistic | feature/002-identity-application-ci | 0 (behind29) | 1 modified +8 untracked |
| e-logistic-identity-activation | feature/002-identity-activation | 0 | Clean |
| e-logistic-identity-current-session | feature/002-identity-current-session-activity | 0 | Clean |
| e-logistic-identity-http-security | feature/002-identity-authentication-http | 0 | Clean |
| e-logistic-identity-remaining-application | feature/002-identity-remaining-application | 0 | Clean |
| e-logistic-identity-session | feature/002-identity-session-application | 0 | Clean |
| e-logistic-sdd-v3 | docs/foundation-pending-changes-resolution | Nhánh PR đang xử lý | Clean trước thay đổi docs |

Sau integration thành công **và Owner disposition rõ ràng**, đề xuất chuỗi sau;
không thực hiện cleanup trong lần này:

1. Owner lưu archive đầy đủ ngoài TEMP vào nơi bền vững; xác minh ZIP SHA-256,
   giải nén thử và đối chiếu đủ 9 hashes. Giữ staged/unstaged patch cùng manifest.
2. Đọc lại toàn bộ status/hash/HEAD trước cleanup; nếu khác checkpoint thì dừng
   và lưu delta mới. Owner cho phép chính xác việc restore file trace tracked
   và gỡ 8 untracked paths đã archive; không xóa thư mục rộng, không clean/reset/stash.
   Các tài liệu business đã có trên develop nên không mang bản raw untracked đè lên.
3. Xác minh `git worktree list`, nhánh develop không bị worktree khác giữ;
   root không có commit riêng và index/worktree sạch. Khi được Owner cho phép,
   chuyển root về develop và cập nhật fast-forward-only tới origin/develop đã fetch;
   không merge nhánh lịch sử, không force checkout, không dùng pull trong root.
4. Kiểm tra từng worktree hoàn tất: tracked/untracked/ignored evidence, process
   đang dùng, branch tip đã reachable từ develop và archive cần giữ. Clean Git
   status không chứng minh ignored private evidence không còn giá trị. Chỉ sau
   Owner chấp thuận mới remove từng worktree cụ thể theo cách chuẩn, không force;
   chưa xóa branch lịch sử hay private credential/evidence.
5. Sau PR này merged, worktree SDD cũng là ứng viên retire khi sạch/không còn active
   branch. Giữ một primary repo và chỉ worktree của công việc đang chạy. Không có
   hai worktree hiện giữ cùng branch theo inventory đã đọc.

## Owner decision gate

Đề nghị review một PR docs-only chứa hai file, không merge tự động. PR approval
không tự cấp quyền xóa dữ liệu gốc; cleanup cần disposition cụ thể sau integration.
Các phân loại OBSOLETE/ADAPT là đề xuất review, không tự đóng security findings.
