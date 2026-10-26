# Cross-module contracts và nguồn chân lý — PROPOSED

[Accepted architecture](../../architecture/module-catalog.md), [data ownership](../../architecture/data-ownership.md) giữ authority. Đây là amendment proposal, không thêm table/service/schema.

| Contract | Accountable/collaborating owners | First enforcement | Source/consumers/tests |
|---|---|---|---|
| XC-IDENTITY-SCOPE | identity; merchant business membership qua authority API; dispatch assignment | L003 trước CRUD, L022 trước machine credential | Account/session/Realm/grant khác membership. Current scope/resource/state; deny missing provider/cross-tenant UUID/revoked member/self-elevation/full-target replacement. No Recipient login/Guest create; D01/02 |
| XC-SHIPMENT-CUSTODY | orders Shipment/Parcel/state; dispatch assignment/custody; warehouse SKU | L006 identity/guard; L010 pickup; L015 delivery | State/tracking/attempt/POD riêng physical custody và finance money custody. Test foreign/duplicate scan/two-hub/reassign race/missing handover/POD; D04/05 |
| XC-PRICE-MONEY | finance money; pricing quote/rate; orders accepted snapshot | L005 policy; L018-core trước L007 fee; L017-core trước L015 COD | Một ledger; payment≠COD≠receipt≠reconciliation≠payout. Test double charge/refund/short receipt/closed-period edit/late event/duplicate payout/sums/currency/rounding; BR13/17/18; D04 |
| XC-EVENT-AUDIT | Source domain event/evidence; platform technical envelope; notification delivery/inbox | L003 first mutation/public first lookup, không đợi L027/028 | Source+audit/outbox atomic; reviewed version/correlation/reason/provenance/time; consumer dedup/state guard/redaction/retention. Test missing audit rollback/forged/replay/reorder/stale projection/revoked recipient; D06 |

Identity không nhận Merchant làm Realm, Merchant không password store. AuthenticatedActor hiện4 fields Demo; production context cần accepted extension/contract riêng, không body actor.

orders Shipment/Parcel/snapshot/tracking/attempt/POD/return; dispatch Station/Hub/trip/assignment/custody/manifest; warehouse SKU/reservation/movement. L008 fulfillment dẫn warehouse nhưng Parcel/label vẫn orders; L024 không own org/ledger. pricing quote khác finance adjustment; orders snapshot không tính lại từ live tariff. finance không import orders.internal; workflow truyền approved facts qua APIs cùng transaction.

Ticket/Claim/Risk dưới orders là D05 proposal trước DDL, không generic status patch. Notification/KPI/audit views chỉ read projections, không source state/ledger. Producer own audit; Auditor scoped read-only. Không compile-time cycle; workflow không own business data. Event không thay DB invariant. Wire fields/envelope cũ PROPOSED trừ accepted Identity/Demo; schema chỉ tại contracts/ sau review.
