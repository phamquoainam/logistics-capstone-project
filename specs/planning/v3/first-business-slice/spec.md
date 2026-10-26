# First business slice — khởi tạo khách gửi và sổ địa chỉ

PROPOSED/BLOCKED_FOR_IMPLEMENTATION. Supplemental packet L003+L004, không spec31/canonical003 replacement. T-003-01 chưa DONE. Không account-enabled slice hoàn toàn unblocked: enrollment D01, authority D02, catalog D03 và data owner D05 chưa approved. Public catalog read-only là nhánh chuẩn bị độc lập IAM nhưng còn dataset/publication/privacy/abuse contract, không complete Sender/Merchant wave.

Outcome: Guest chọn đăng ký cá nhân hoặc Merchant; Sender verified login/profile/sổ địa chỉ riêng; Merchant Owner org/membership/delegation và pickup point, Operator/Manager đúng scope. Reload thấy dữ liệu; Merchant B/Sender B biết UUID vẫn deny; public catalog/coverage/điểm phục vụ không lộ private data.

BC subset: BC-001/005/007/008/009/010/020/021/022/132/133/134/135/136/137/157/158/166/168, REQ-V3-BC-xxx ở L003/L004; composite BC008/157/166 chưa complete trước đủ mọi operation. Denials BC006/045/085 tại server. Guest quote/tracking/contact còn CORE L005/L009/L020; Sender booking/payment/returns/support theo later dependencies, không loại target.

Ngoài packet: Shipment creation/confirm, money/stock/custody/POD, Recipient account/Guest booking/machine credential/Partner operations; không add enum roles từ matrix.

| Planned AC | Given/When | Then/proof |
|---|---|---|
| AC-V3-FIRST-01 | Guest chọn cá nhân/Merchant và verified enrollment theo approved contract | Đúng context; không staff/admin escalation, không ép Sender membership; proof replay/enumeration controlled |
| AC-V3-FIRST-02 | Sender A/B login, A lưu/sửa confirmed address rồi reload | Chỉ A owned data; B UUID/URL/API deny; raw/version/provenance đúng, không fake geocode |
| AC-V3-FIRST-03 | Owner A cấp delegated Operator/Manager và tạo pickup/reload | Org/branch/current membership đúng; B và Identity-only admin không business permission deny |
| AC-V3-FIRST-04 | Revoke membership/account giữa form→submit hoặc replay | Current backend authority deny, no receipt/data leak; UI re-auth/permission changed, no success giả |
| AC-V3-FIRST-05 | Cùng key/payload replay, key khác payload, concurrent stale updates | Một effect/audit; payload/version conflict theo approved contract; PG constraints chống lost update |
| AC-V3-FIRST-06 | Catalog đổi/archive point, geocoder ambiguity/timeout, invalid coordinate | Chưa confirmed khi thiếu proof, input retained/manual-approved; historical refs giữ version |
| AC-V3-FIRST-07 | Guest/Recipient gọi Shipment create/private APIs, role/owner injection | No business effect/account path; public redaction/rate limit và server denial |

Demo sau implementation: fresh migrated PG/fixtures, Guest hai paths; Sender A address; Owner A delegate và Operator point; reload/persistence; direct UUID từ B deny; revoke trước submit; failure/replay receipts. Không Demo principal/synthetic UI thay authority.

DoR: Owner D01/02/03/05, public D06 subset, namespace D08, accepted API/data/model/fixture/error/state/idempotency. DoD: [delivery](../../../../docs/reconciliation/v3/delivery-plan.md), AC trên bằng real API/PG/browser/source+PR CI, no parent001/002 auto accepted.
