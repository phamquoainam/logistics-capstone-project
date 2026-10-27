# Legacy retirement và canonical promotion — 2026-10-27

Owner explicitly authorized documentation retirement/promotion. Pinned baseline `eeb7833ab678240581c407ccd167d7e61ea873c6`; [exact45 retired legacy files](retirement.csv), [87 promoted/superseded planning files and IDs](planning-promotion-history.json), [1024 legacy ID/path records](legacy-crosswalk.csv). Git history giữ nội dung đầy đủ; không giữ duplicate active spec tree.

## Giữ nguyên

`specs/001-platform-foundation/` và `specs/002-identity-access/` giữ nguyên toàn bộ tracked bytes/accepted evidence/status;001 Demo contract-client slice vẫn giữ vì có accepted contracts/source evidence. Demo runtime Java/TS, migrations, tests, contracts, CI/security policies và historical evidence không bị xóa. Research016 không đổi nghĩa thành Returns016; RQ1–4 nghĩa vụ chuyển vào030 với old IDs/provenance.

## Verification trước retirement

Inventory đọc từng legacy spec/design/tasks;003–016 không có checked implementation task. Demo có accepted bounded execution nên **chỉ retire documentation entry**, không xóa source/contracts/tests/evidence. Mỗi deleted file được kiểm tra tracked Markdown-only, literal resolved path trong specs; SHA256 của working file và immutable Git history link trong retirement.csv. Không broad cleanup/reset/force deletion. Old planning triples được đọc và promote; first-business-slice packet superseded bằng delivery principles + cross-Spec product journeys, không tạo spec31.

| Retired directory | Canonical successor(s), scope relation only | Evidence |
|---|---|---|
| `specs/003-merchant-address/` | L003, L004, L006 | [pinned original](https://github.com/phamquoainam/logistics-capstone-project/blob/eeb7833ab678240581c407ccd167d7e61ea873c6/specs/003-merchant-address) |
| `specs/004-pricing-engine/` | L005, L018 | [pinned original](https://github.com/phamquoainam/logistics-capstone-project/blob/eeb7833ab678240581c407ccd167d7e61ea873c6/specs/004-pricing-engine) |
| `specs/005-shipment-lifecycle/` | L006, L007, L009, L015, L016 | [pinned original](https://github.com/phamquoainam/logistics-capstone-project/blob/eeb7833ab678240581c407ccd167d7e61ea873c6/specs/005-shipment-lifecycle) |
| `specs/006-wallet-payments/` | L007, L017, L018, L019 | [pinned original](https://github.com/phamquoainam/logistics-capstone-project/blob/eeb7833ab678240581c407ccd167d7e61ea873c6/specs/006-wallet-payments) |
| `specs/007-warehouse-wms/` | L008, L016 | [pinned original](https://github.com/phamquoainam/logistics-capstone-project/blob/eeb7833ab678240581c407ccd167d7e61ea873c6/specs/007-warehouse-wms) |
| `specs/008-dispatch-hub/` | L010, L011, L012, L013, L014, L025 | [pinned original](https://github.com/phamquoainam/logistics-capstone-project/blob/eeb7833ab678240581c407ccd167d7e61ea873c6/specs/008-dispatch-hub) |
| `specs/009-shipper-pod/` | L010, L014, L015, L016, L017, L029 | [pinned original](https://github.com/phamquoainam/logistics-capstone-project/blob/eeb7833ab678240581c407ccd167d7e61ea873c6/specs/009-shipper-pod) |
| `specs/010-routing-location/` | L004, L014, L025 | [pinned original](https://github.com/phamquoainam/logistics-capstone-project/blob/eeb7833ab678240581c407ccd167d7e61ea873c6/specs/010-routing-location) |
| `specs/011-cod-reconciliation/` | L017, L018, L019 | [pinned original](https://github.com/phamquoainam/logistics-capstone-project/blob/eeb7833ab678240581c407ccd167d7e61ea873c6/specs/011-cod-reconciliation) |
| `specs/012-notifications-dashboard/` | L009, L020, L028 | [pinned original](https://github.com/phamquoainam/logistics-capstone-project/blob/eeb7833ab678240581c407ccd167d7e61ea873c6/specs/012-notifications-dashboard) |
| `specs/013-admin-audit-configuration/` | L026, L027 | [pinned original](https://github.com/phamquoainam/logistics-capstone-project/blob/eeb7833ab678240581c407ccd167d7e61ea873c6/specs/013-admin-audit-configuration) |
| `specs/014-bulk-shipment-import/` | L023 | [pinned original](https://github.com/phamquoainam/logistics-capstone-project/blob/eeb7833ab678240581c407ccd167d7e61ea873c6/specs/014-bulk-shipment-import) |
| `specs/015-shipper-offline-sync/` | L029 | [pinned original](https://github.com/phamquoainam/logistics-capstone-project/blob/eeb7833ab678240581c407ccd167d7e61ea873c6/specs/015-shipper-offline-sync) |
| `specs/016-research-experiments/` | L030 | [pinned original](https://github.com/phamquoainam/logistics-capstone-project/blob/eeb7833ab678240581c407ccd167d7e61ea873c6/specs/016-research-experiments) |
| `specs/demo-001-auth-merchant-shipment/` | L001, L002, L007, L009 | [pinned original](https://github.com/phamquoainam/logistics-capstone-project/blob/eeb7833ab678240581c407ccd167d7e61ea873c6/specs/demo-001-auth-merchant-shipment) |

## Acceptance và alias rules

Retired REQ/AC/T/DEMO/FR/BR/NFR IDs không renumber hoặc xóa khỏi registry. Historical requirement statements được tiếp nhận vào successor spec và legacy task; acceptance equivalence không được suy từ mapping. Legacy003 REQ01 pickup primary command004 (003 chỉ authority/context); REQ02/03/05 address004; REQ04 snapshot006. Composite001/002 references giữ bounded historical scope, không nâng parent acceptance. Link lịch sử dùng pinned Git blob; active navigation chỉ canonical003–030. Historical planning task IDs vẫn tra được qua promotion-history, replacement tasks không có DONE tự động.

## Additional overlay IDs

Original1024 registry records remain unchanged in source statement/identity. [86 overlay IDs](retirement-id-supplement.csv) added by previous V3 documentation are preserved separately with exact pinned line; validator checks both. No historical IDs discarded to make migration pass.
