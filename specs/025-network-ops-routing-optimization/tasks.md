# Tasks 025 — network-ops-routing-optimization

NOT_STARTED; thứ tự là contract/fixture → DB → BE/HTTP → UI/channel → verification → full acceptance. Mỗi BC là gói end-to-end nội bộ, không thay phương pháp Spec-by-Spec. Không task nào DONE trong rebuild docs.

- [ ] T-L025-CONTRACT: chốt source dependencies, fixture/env/limits và expected outcomes.
- [ ] T-L025-BUILD: implement Routing/capacity proposals; dispatch phê duyệt assignment/trip/custody + Network region/capacity/exception; Dispatcher review; Courier freshness đúng source guards.
- [ ] T-L025-VERIFY: actual fault/restart/concurrency/browser/native/load/restore theo applicability, exact reports và preserved RQ IDs.

- [ ] T-LEGACY-L025: map từng obligation legacy ở spec sang implementation/negative/compatibility testcase, bao gồm phần consumer; không đóng full spec khi requirement legacy còn chưa được chứng minh hoặc disposition rõ.

## Closure toàn Spec

- [ ] T-L025-FULL: từng REQ có AC/design/task/code/API/DB/UI/test/receipt; lint/typecheck/build/test theo app liên quan, required Architecture/H2/PG18 CI exact revision, report/privacy/owned cleanup và backward compatibility giữ nguyên; no unresolved blocking findings.
- [ ] T-L025-ACCEPT: scoped PR integration PASS → Owner merge → regression trên merge SHA → Owner full-spec acceptance riêng. Chưa tick vì docs promotion.
