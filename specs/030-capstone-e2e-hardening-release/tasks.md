# Tasks 030 — capstone-e2e-hardening-release

NOT_STARTED; thứ tự là contract/fixture → DB → BE/HTTP → UI/channel → verification → full acceptance. Mỗi BC là gói end-to-end nội bộ, không thay phương pháp Spec-by-Spec. Không task nào DONE trong rebuild docs.

- [ ] T-L030-CONTRACT: chốt source dependencies, fixture/env/limits và expected outcomes.
- [ ] T-L030-BUILD: implement Regression/restore/health/load/CI evidence; reuse Research016 RQ1–4 + Browser J1–J9/native device; không business UI mới đúng source guards.
- [ ] T-L030-VERIFY: actual fault/restart/concurrency/browser/native/load/restore theo applicability, exact reports và preserved RQ IDs.

- [ ] T-LEGACY-L030: map từng obligation legacy ở spec sang implementation/negative/compatibility testcase, bao gồm phần consumer; không đóng full spec khi requirement legacy còn chưa được chứng minh hoặc disposition rõ.

## Closure toàn Spec

- [ ] T-L030-FULL: từng REQ có AC/design/task/code/API/DB/UI/test/receipt; lint/typecheck/build/test theo app liên quan, required Architecture/H2/PG18 CI exact revision, report/privacy/owned cleanup và backward compatibility giữ nguyên; no unresolved blocking findings.
- [ ] T-L030-ACCEPT: scoped PR integration PASS → Owner merge → regression trên merge SHA → Owner full-spec acceptance riêng. Chưa tick vì docs promotion.


- [ ] **T-AB-CONTINUITY** — REQ-AB-CONTINUITY: close approved operation/state/data/UX/config contract + fixtures; implement owner API/DB/HTTP and integrated Cross-product acceptance evidence; unit/PG/security/browser or verified channel assert «J-AB-01..10 with exact revisions real BE/PG/browser+native when applicable; no fake carrier event in portal for pass; RQ1–4 and restore/load gates retained»; TEST-AB-CONTINUITY with source+integration receipts. Dependencies L003;L004;L005;L006;L007;L008;L009;L010;L011;L012;L013;L014;L015;L016;L017;L018;L019;L020;L021;L022;L023;L028;L029; do not mark full spec when missing.
