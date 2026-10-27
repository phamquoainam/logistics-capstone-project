# Spec 030 — capstone-e2e-hardening-release

Alias lịch sử logical `L030`. CANONICAL_TARGET / NOT_STARTED / BLOCKED_FOR_IMPLEMENTATION. Owner accountable: `platform`; từng BC giữ DML owner riêng.

## Outcome và boundary

Cross-module E2E/release/RQ. Regression/restore/health/load/CI evidence; reuse Research016 RQ1–4.

Workflow: Reviewed SHA→journeys/negative/fault/load→restore→evidence→Owner release. Ngoài scope: Không cấp quyền từ role label; không ghi dữ liệu owner khác;5 DEFER chỉ denial. CORE wave sau vẫn trong target.; downstream thuộc owner register, không duplicate mutation.

Hard minimum contracts: L003, L004, L005, L006, L007, L008, L009, L010, L011, L012, L013, L014, L015, L016, L017, L018, L019, L020, L021, L022, L023, L024, L025, L026, L027, L028, L029. Consumer/soft: none. Hard là dependency chức năng tối thiểu đã chứng minh, không buộc cả spec upstream accepted. Soft không cho phép fake missing facts.

Blockers trước first use: DEC-24; unresolved release blockers. [Decision register](../../docs/reconciliation/v3/decisions.md).

## Requirements và acceptance từng BC

### REQ-V3-L030-ENABLER

Cross-module E2E/release/RQ. Given approved source contracts và reproducible fixtures; When Reviewed SHA→journeys/negative/fault/load→restore→evidence→Owner release; Then no source invariant violation, actual evidence và explicit failure/limitations; no fake runtime PASS.

## Nghĩa vụ legacy được tiếp nhận

Các ID cũ là aliases nguồn, không đổi nghĩa hoặc coi mapping bằng acceptance. Khi một yêu cầu phân bổ nhiều spec, primary mutation/data owner theo cross-module contract; consumer chứng minh phần mình.

| ID lịch sử | Nghĩa vụ giữ lại | Nguồn |
|---|---|---|
| REQ-016-01 | RQ1 test concurrent wallet/reservation/assignment với DB baseline vs optional Redis lock khi nhiều replica, đếm invariant violations. | [pinned history](https://github.com/phamquoainam/logistics-capstone-project/blob/eeb7833ab678240581c407ccd167d7e61ea873c6/specs/016-research-experiments/spec.md) |
| REQ-016-02 | RQ2 no cache/cold/hot, tariff change, cache outage; pricing accuracy luôn được đối chiếu golden cases. | [pinned history](https://github.com/phamquoainam/logistics-capstone-project/blob/eeb7833ab678240581c407ccd167d7e61ea873c6/specs/016-research-experiments/spec.md) |
| REQ-016-03 | RQ3 message retries/duplicate/reorder/restart so baseline vs outbox/inbox; đo completion/duplicate effects/recovery. | [pinned history](https://github.com/phamquoainam/logistics-capstone-project/blob/eeb7833ab678240581c407ccd167d7e61ea873c6/specs/016-research-experiments/spec.md) |
| REQ-016-04 | RQ4 baseline route order/nearest neighbor so solver candidate với cùng matrix/constraints; đo distance, estimated time, compute and valid routes. | [pinned history](https://github.com/phamquoainam/logistics-capstone-project/blob/eeb7833ab678240581c407ccd167d7e61ea873c6/specs/016-research-experiments/spec.md) |
| REQ-016-05 | Mỗi run lưu seed, source commit, image versions, parameter load, metrics raw, errors, limitations; không tự sinh thành tích giả. | [pinned history](https://github.com/phamquoainam/logistics-capstone-project/blob/eeb7833ab678240581c407ccd167d7e61ea873c6/specs/016-research-experiments/spec.md) |

## Quy tắc chung và evidence

Canonical target documentation; NOT_STARTED/BLOCKED_FOR_IMPLEMENTATION. Không acceptance do promotion. Exact route/DTO/error/permission/schema: DECISION_REQUIRED trong contracts sau review; các khái niệm dưới đây không phải DDL hay wire field được tự duyệt.

Authority = current account/session + explicit permission + personal/org/branch/assignment/resource scope + current state/evidence; server recheck cả list, pagination, submit, replay và async recovery. Deny trước khi tiết lộ existence/receipt; không body actor/role/owner authority. Machine principal tách human; Recipient external; Guest không tạo online Shipment.

Mutation hợp lệ: scoped key + subject/operation/target + canonical payload + original version; same binding một effect, changed binding conflict, current authority recheck; read không ghi business effect. Unique/check/FK/locking theo invariant đã review, rollback khi audit bắt buộc lỗi. Source+audit/receipt/outbox atomic; external I/O ngoài TX; consumer inbox/dedup. Không đổi strict refresh thành retry/idempotency grace.

UI có loading/empty/field validation/error/unauthorized/pending/conflict/unknown/revoked; giữ input an toàn, không success trước server. Search/filter/sort/paging scoped; keyboard/focus/labels/contrast/responsive; timezone/currency/unit nhất quán, private data mask. Feature chưa có không hiện nút chết/mock counter.

Unit/ArchUnit/contract/HTTP real filter chain/H2 regression/PG18 empty+upgrade+privilege+constraint+rollback+races/browser E2E; native device chỉ nơi thật sự có native flow. Exact SHA/env/command/exit/pass/fail/named skips/report/artifact digest; planned IDs không receipt. V1–V6 immutable; no repair/ORM update/security allowlist bypass. Owner merge và full acceptance riêng; TASK DONE != PR MERGED != SPEC ACCEPTED != PORTAL ACCEPTED.

## Full acceptance

Mọi assigned CORE/negative/legacy obligation và supplemental portal requirement phải có real BE/API/PG/UI-channel evidence; minimum dependencies verified; mandatory CI, traceability và Owner feature acceptance. [Portal milestone](../../docs/portal/group-ab-acceptance.md) là cross-Spec, không tự PASS khi spec này hoàn tất.


## REQ-AB-CONTINUITY

Owner target: L030; source BC-057;BC-063;BC-073;BC-092; latest Owner portal scope, no new BC ID. UI/channel: Cross-product acceptance evidence.

Required behavior: Customer requests connect carrier pickup→Station→sorting→linehaul→delivery or return→COD/Finance/support; preserve separate source facts. Exact routes/DTO/permission/schema CONFIG/DECISION_REQUIRED before code.

**AC-CONTINUITY:** Given valid current actor/resource/state; When thực hiện workflow nguồn; Then J-AB-01..10 with exact revisions real BE/PG/browser+native when applicable; no fake carrier event in portal for pass; RQ1–4 and restore/load gates retained. Negative: wrong owner/revoked/expired/illegal state → no unauthorized effect/private disclosure. Evidence: real source-owner HTTP/PG + browser/channel; TEST-AB-CONTINUITY PLANNED_NOT_EXECUTED. Dependencies: L003;L004;L005;L006;L007;L008;L009;L010;L011;L012;L013;L014;L015;L016;L017;L018;L019;L020;L021;L022;L023;L028;L029.
