# L020 — customer-support-tickets-sla

PROPOSED / BLOCKED_FOR_IMPLEMENTATION. Primary accountable domain: orders; wave F3; actors R01,R02,R03,R04,R10,R11,R14,R18,R19,R21,R24,R26,R29.

## Outcome và scope

Ticket có nguồn/scope/SLA. Không cấp quyền từ role label; không ghi dữ liệu owner khác;5 DEFER chỉ denial. CORE wave sau vẫn trong target.

## Requirements theo source

[371 BC crosswalk](../../../../docs/reconciliation/v3/capability-crosswalk.csv) giữ từng guard/owner/status. [Legacy crosswalk](../../../../docs/reconciliation/v3/legacy-crosswalk.csv) bảo toàn FR/REQ/AC/task, không tự coi mapping là equivalence.

| Requirement | Source | Hành vi | Guard bắt buộc | Acceptance kind |
|---|---|---|---|---|
| REQ-V3-BC-004 | BC-004/R01/M15 | Gửi yêu cầu tư vấn/hỗ trợ không đăng nhập | Rate limit và kiểm soát spam; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-016 | BC-016/R02/M15 | Gửi và theo dõi support ticket | Không tự quyết định bồi thường; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-030 | BC-030/R03/M15 | Tạo ticket hỗ trợ Logistics nếu được cấp quyền | Không duyệt bồi thường; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-038 | BC-038/R04/M15 | Phê duyệt escalation theo quyền | Separation of duties; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-083 | BC-083/R10/M15 | Phản ánh đơn Merchant qua Merchant Support | Merchant lập ticket với Logistics CSKH; không có Recipient Portal; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-089 | BC-089/R11/M15 | Mở tra soát COD thiếu/phí sai | Không tự duyệt hoàn tiền; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-105 | BC-105/R14/M15 | Nhận/tạo ticket từ Merchant hoặc người liên quan | Phân loại nguồn; xác minh quyền thông tin; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-107 | BC-107/R14/M15 | Phân loại, phân công và cập nhật SLA ticket | Lưu lịch sử thay đổi; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-108 | BC-108/R14/M10 | Gửi yêu cầu giao lại/đổi thông tin | Workflow phê duyệt; không tự sửa Shipment đã chốt; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-109 | BC-109/R14/M15 | Escalate vụ nghiêm trọng | Không tự phê duyệt bồi thường; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-110 | BC-110/R14/M15 | Đa kênh hotline/email/chat tích hợp | Không cần xây portal Recipient; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-145 | BC-145/R18/M15 | Mở và theo dõi yêu cầu hỗ trợ/tra soát mất, hỏng | Chỉ Shipment thuộc mình, có ticket/evidence; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-153 | BC-153/R14/M15 | Nhận và xử lý ticket trực tiếp từ Retail Sender | Xác minh Shipment thuộc Sender, tracking, timeline, escalation; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-154 | BC-154/R14/M15 | Tiếp nhận phản ánh Recipient cho đơn Retail Sender | Không giả định Merchant Support; xác minh tối thiểu; bảo vệ PII; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-155 | BC-155/R02/M15 | Tạo và theo dõi ticket từ phản ánh Recipient đối với đơn Merchant | Có thể ủy quyền Merchant Support khi Role được bổ sung; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-156 | BC-156/R03/M15 | Tiếp nhận/chuyển phản ánh Recipient và theo dõi ticket theo ủy quyền | Merchant scope, không tự quyết định bồi thường; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-173 | BC-173/R19/M15 | Trao đổi với hãng chuyển phát, mở/escalate ticket | Không tự duyệt bồi thường; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-191 | BC-191/R11/M15 | Mở tra soát COD thiếu, phí sai và theo dõi kết quả | Không tự phê duyệt khoản hoàn/bồi thường; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-195 | BC-195/R21/M15 | Tiếp nhận khiếu nại Recipient và tạo ticket liên kết Shipment | Xác minh đơn/nguồn phản ánh; chống ticket trùng; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-196 | BC-196/R21/M15 | Gửi ticket sang E-Logistics Customer Service và theo dõi timeline | Có phân công, SLA, audit và bằng chứng; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-199 | BC-199/R21/M15 | Cập nhật phản hồi Recipient và xác nhận hoàn tất phối hợp ticket | Không tự đóng ticket Logistics trái quy trình; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-249 | BC-249/R24/M15 | Hỗ trợ sự cố giao nhận và escalations | Ghi lý do, liên kết ticket/incident; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-266 | BC-266/R26/M15 | Tạo/điều phối báo cáo mất hoặc hỏng kiện | Liên kết kiện và vị trí cuối, ticket/claim; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-292 | BC-292/R14/M05 | Tra cứu Shipment theo ticket/phạm vi phân công | PII tối thiểu, reason và access audit; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-294 | BC-294/R14/M03 | Tiếp nhận yêu cầu đổi địa chỉ hoặc số liên lạc | Xác minh sở hữu; phê duyệt thay đổi theo state; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-296 | BC-296/R14/M16 | Giám sát deadline phản hồi và giải quyết ticket | SLA clock, escalation, audit; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-307 | BC-307/R04/M15 | Phê duyệt escalation nghiệp vụ theo hạn mức | Separation of duties, lý do và audit; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |
| REQ-V3-BC-334 | BC-334/R29/M15 | Phối hợp hỗ trợ Merchant lớn | Ticket ownership và escalation; permission code DECISION_REQUIRED (no runtime allowlist expansion) | POSITIVE+STATE_GUARD+AUDIT |

## Workflow và acceptance

Tiếp nhận→verify→phân công→phối hợp/escalate→phản hồi; không sửa POD/COD.

Với từng requirement: Given actor/resource đúng scope và preconditions, When thực hiện operation nguồn, Then chỉ outcome đúng guard được commit với provenance. Với NEGATIVE/DENY: When gọi trực tiếp API, Then từ chối và không ghi effect. Given expired/revoked/wrong resource/state, When submit/replay/recovery, Then không leak data/receipt hay mutation. Given mất response sau commit, When recovery đúng contract, Then không lặp effect. Required test IDs tại CSV là planned, chưa có runtime receipt.

## Dependencies và readiness

Hard: L002. Soft/consumer: L009,L015; soft không cho phép fake missing facts. [Roadmap](../../../../docs/implementation/roadmap-v3.md) phân biệt minimum contract slice và full acceptance.

Blockers: D-V3-05; D-V3-06; DEC-20. Owner: Project Owner + Tech Lead; Security cho IAM/privacy; Finance cho money; Operations cho custody. Owner duyệt mapping/decisions, API/event/state/data owner và fixtures; không BLOCKING trong slice; positive/deny/concurrency/recovery đã thiết kế; canonical docs/codex/definition-of-ready.md.

## Delivery gate

BE+HTTP+DB+FE thật, PostgreSQL invariants/guards và browser/native evidence phù hợp, source+PR CI pass; BC/test/evidence cập nhật; Owner merge/acceptance riêng. [Design](design.md), [tasks](tasks.md), [shared convention](../../../../docs/reconciliation/v3/delivery-plan.md). Không promote IMPLEMENTED/VERIFIED do có file.
