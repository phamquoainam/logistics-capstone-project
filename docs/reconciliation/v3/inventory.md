# Repository và source inventory — 2026-10-26

Fetched origin/develop=`ff3b2963ee1b165c7ade412e8b37389da65ca28e`, merge [PR28](https://github.com/phamquoainam/logistics-capstone-project/pull/28), MERGED2026-10-24T18:20:43Z. [Post-merge CI40120270733](https://github.com/phamquoainam/logistics-capstone-project/actions/runs/40120270733) completed/success đúng SHA; PR checks40119118325 SUCCESS. Không biến historical CI thành candidate acceptance.

## Ba mandatory inputs đã đọc toàn văn từ đường dẫn tuyệt đối Owner

| Source | Raw SHA-256 |
|---|---|
| [E_Logistics_Business_Complete_GroupEF_Full.md](../../business-references/E_Logistics_Business_Complete_GroupEF_Full.md) | `7bdf80aab7ee77541c01e982f511c77e025acc125bf6a5f58d5b6a3b1aa75422` |
| [E_Logistics_Role_Module_Business_Capability_Matrix_GroupEF_Full.md](../../business-references/E_Logistics_Role_Module_Business_Capability_Matrix_GroupEF_Full.md) | `6cda0cef0a6654281c79ebecb4d03d89add6542f3e3721098b64f1f7fbb01a59` |
| [E_Logistics_Spec_Roadmap_V2_Proposed.md](../../business-references/E_Logistics_Spec_Roadmap_V2_Proposed.md) | `356a6421bddeb69869a8c5fbe1291b240e1854f1a0068b58c5df4e47c8d85a03` |

Raw bytes lần lượt98454/81221/36661. Business/Matrix giữ nguyên byte. V2.1 có3 dòng hard-break bằng hai spaces (3–5): [gzip](../../business-references/E_Logistics_Spec_Roadmap_V2_Proposed.md.gz) giữ raw nguyên byte; Markdown view chỉ thay bằng backslash hard-break tương đương để diff-check sạch. Validator so raw archive+view+gốc; không sửa nghĩa/lịch sử VIII.8.1–8.8.

MISSING_SOURCE: CLV04_BaoCao_TLCN_04.docx, business_logistoc.docx, E_Logistics_Roadmap_V2_1_BC_Ownership_Draft.csv, E_Logistics_Spec_Roadmap_V2_Original_Backup.md và assets/image*.jpeg không trong tracked baseline/ba input. Dùng report-derived FR/BR/NFR/state docs, không giả đã đọc DOCX/hình/backup.

[File inventory](repository-inventory.csv):555 tracked paths+Git blobs/categories. Root README.md/prd/ và contracts/events/ chưa tồn tại; entrypoints thật [README-SDD](../../README-SDD.md)/[PRD](../../prd.md)/[index](../../../specs/INDEX.md). Có16 folders001–016+demo+Foundation demo-contract-client slice. [Legacy crosswalk](legacy-crosswalk.csv):1024 ID/path records REQ/AC/task/ADR/policy/migration/operation, giữ IDs.

Backend app/backend/com.elogistic.backend với identity/merchant/orders/platform; sáu domain khác chưa implemented từ fixtures. Web app/frontend/web React/TS/React Router; native app/frontend/shipper-mobile Expo/RN; packages/api-client hiện Demo. App.tsx Demo/DRAFT không production Sender/Merchant.

| Baseline | Evidence | Disposition giữ nguyên |
|---|---|---|
| Foundation | [trace](../../quality/traceability.md), [AC01](../../implementation/ac-001-01-final-acceptance.md) | Feature001/D1-08/Day1 PARTIAL; AC01 PASS_WITH_DOCUMENTED_FINDINGS; AC04/05 NOT ACCEPTED; raw-cache/privacy OPEN |
| Identity | [tasks](../../../specs/002-identity-access/tasks.md), [trace](../../quality/feature-002-recovery-traceability.md), [auth](../../implementation/t-002-06-authentication-http-evidence.md) | T00201..04 bounded DONE; T-002-05/T-002-06 PARTIAL; Feature002/AC-002-01..05 NOT ACCEPTED; GRANT_MUTATION_FUNCTIONALITY_LIMITED |
| Demo | [spec](../../../specs/demo-001-auth-merchant-shipment/spec.md), [evidence](../../implementation/demo-001-evidence.md) | Historical/regression, không public registration/quote/pickup/POD/settlement |
| Future003–015 | spec/design/tasks và crosswalk | DRAFT/unaccepted, không promote BC/test/task từ planning |
| Research016 | [spec](../../../specs/016-research-experiments/spec.md) | PRESERVE RQ1–4, không Returns hay fake benchmark |
| Contract/migration/CI | [fingerprints](protection-baseline.json) | Hai OpenAPI/13ADR/V1–V6/limiter/workflow nguyên vẹn; global DEC25/events không tự ACCEPTED |

Gốc feature/002-identity-application-ci tại3d9281e; sáu Foundation hashes riêng. Gốc+năm Identity worktrees lưu HEAD/status, không pull/checkout/reset/stash/clean/merge. Đúng một worktree mới D:/TLCN/e-logistic-sdd-v3 branch docs/business-sdd-roadmap-v3 từ origin/develop. Snapshot458 files; dependency-order.md là authorized planning navigation exception, không runtime receipt. Không nhập Foundation pending vào PR.
