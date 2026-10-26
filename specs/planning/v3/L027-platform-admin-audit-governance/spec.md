# L027 — platform-admin-audit-governance

PROPOSED / BLOCKED_FOR_IMPLEMENTATION. Primary accountable domain: identity; wave F4; actors R04,R05,R16,R17,R24,R27,R31,R32.

## Outcome và scope

Governance phân tách admin/IT/Auditor. Không cấp quyền từ role label; không ghi dữ liệu owner khác;5 DEFER chỉ denial. CORE wave sau vẫn trong target.

## Requirements theo source

[371 BC crosswalk](../../../../docs/reconciliation/v3/capability-crosswalk.csv) giữ từng guard/owner/status. [Legacy crosswalk](../../../../docs/reconciliation/v3/legacy-crosswalk.csv) bảo toàn FR/REQ/AC/task, không tự coi mapping là equivalence.

| Requirement | Source | Hành vi | Guard bắt buộc | Acceptance kind |
|---|---|---|---|---|
| REQ-V3-BC-031 | BC-031/R04/M01 | Truy cập admin bằng quyền nghiệp vụ | Không mặc nhiên có Identity quyền; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-036 | BC-036/R04/M18 | Tra cứu audit nghiệp vụ liên quan | Không xóa/sửa audit; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-043 | BC-043/R05/M18 | Xem sự kiện bảo mật Identity | Least privilege; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-124 | BC-124/R16/M18 | Xem audit vận hành của Station | Chỉ đọc; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-125 | BC-125/R17/M18 | Đọc audit events đúng phạm vi | Read only, không sửa/xóa; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-128 | BC-128/R17/M01 | Xem lịch sử phân quyền và thu hồi quyền | Không quản lý tài khoản; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-252 | BC-252/R24/M18 | Thao tác thay đổi assignment/chấp nhận tổn thất cần quyền riêng | Không mặc định có privilege đặc biệt; permission code DECISION_REQUIRED (no runtime allowlist expansion) | NEGATIVE/DENY |
| REQ-V3-BC-279 | BC-279/R27/M18 | Audit quyết định chuyển tuyến và ngoại lệ | Chỉ quyền được ủy quyền, không xóa log; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-306 | BC-306/R04/M01 | Phân công vai trò nghiệp vụ nhân sự theo ủy quyền | Không tự cấp admin bảo mật/Identity; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-308 | BC-308/R04/M16 | Xem báo cáo doanh thu, sản lượng, chất lượng dịch vụ | Theo tổ chức/vùng được cấp phép; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-313 | BC-313/R05/M18 | Giám sát thay đổi quyền và sự kiện Identity | Chỉ phạm vi bảo mật, append-only audit; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-314 | BC-314/R17/M18 | Xem lịch sử thay đổi quyền và quản trị | Read-only, scoped, immutable source; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-348 | BC-348/R31/M01 | Quản lý tenant và quyền quản trị nền tảng cấp cao | Privilege boundary, approval và session assurance; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-349 | BC-349/R31/M02 | Cấu hình phân vùng tổ chức ở cấp nền tảng | Không tự có quyền sở hữu Merchant; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-350 | BC-350/R31/M18 | Quản lý feature flags theo kiểm soát thay đổi | Audit, rollback và rollout scope; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-351 | BC-351/R31/M18 | Quản lý chính sách nền tảng được phân quyền | Policy versioning và phê duyệt; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-352 | BC-352/R31/M18 | Phê duyệt tác vụ quản trị đặc biệt theo quy trình | Dual approval khi policy yêu cầu; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-353 | BC-353/R31/M18 | Giám sát thao tác đặc quyền và break-glass | Reason, expiry, immutable audit; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-361 | BC-361/R32/M18 | Vận hành production có kiểm soát | Approval, least privilege, audit; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-362 | BC-362/R32/M01 | Không impersonate Merchant tùy ý | Session/account controls và evidence; permission code DECISION_REQUIRED (no runtime allowlist expansion) | NEGATIVE/DENY |

## Workflow và acceptance

Authorized change+reason/approval→audit; deny fake POD/COD/delete audit/impersonation.

Với từng requirement: Given actor/resource đúng scope và preconditions, When thực hiện operation nguồn, Then chỉ outcome đúng guard được commit với provenance. Với NEGATIVE/DENY: When gọi trực tiếp API, Then từ chối và không ghi effect. Given expired/revoked/wrong resource/state, When submit/replay/recovery, Then không leak data/receipt hay mutation. Given mất response sau commit, When recovery đúng contract, Then không lặp effect. Required test IDs tại CSV là planned, chưa có runtime receipt.

## Dependencies và readiness

Hard: L002,L003. Soft/consumer: L019,L026; soft không cho phép fake missing facts. [Roadmap](../../../../docs/implementation/roadmap-v3.md) phân biệt minimum contract slice và full acceptance.

Blockers: D-V3-02; D-V3-06; D-V3-07; DEC-21. Owner: Project Owner + Tech Lead; Security cho IAM/privacy; Finance cho money; Operations cho custody. Owner duyệt mapping/decisions, API/event/state/data owner và fixtures; không BLOCKING trong slice; positive/deny/concurrency/recovery đã thiết kế; canonical docs/codex/definition-of-ready.md.

## Delivery gate

BE+HTTP+DB+FE thật, PostgreSQL invariants/guards và browser/native evidence phù hợp, source+PR CI pass; BC/test/evidence cập nhật; Owner merge/acceptance riêng. [Design](design.md), [tasks](tasks.md), [shared convention](../../../../docs/reconciliation/v3/delivery-plan.md). Không promote IMPLEMENTED/VERIFIED do có file.
