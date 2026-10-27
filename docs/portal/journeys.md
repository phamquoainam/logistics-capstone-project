# J-AB-01–10 — cross-Spec product journey register

[Machine-readable register](journeys.csv). Mỗi journey có roles, BC, source APIs, data owners và expected evidence; no extra canonical spec. NOT_STARTED; không actual runtime receipt.

| Journey | Specs | Kết quả và negative/failure gate |
|---|---|---|
| J-AB-01 Guest Public Services | L004;L005;L009;L020 | Public tracking/quote/coverage/Station/service/contact redacted; spam/enumeration/PII/COD/private history deny |
| J-AB-02 Customer Self-Registration | L002;L003 | One personal registration→verified activation→login→personal default; duplicate/recovery/abuse/no SYSTEM grant |
| J-AB-03 Merchant Admission | L002;L003 | Authenticated Sender→application→manual sandbox evidence→needs-info/resubmit→independent approval→atomic org/Owner→same personal+org access; Guest/self-review/duplicate/partial activation deny |
| J-AB-04 Retail Sender Shipping | L003;L004;L005;L006;L007;L008;L009;L010;L011;L014;L015;L016;L017;L018;L020;L021;L028 | Personal no Merchant→draft→quote→atomic confirm→pickup/dropoff→tracking→delivery/return→payment/support; real carrier handoff, failure/replay/rollback |
| J-AB-05 Merchant Shipping Operations | L003;L004;L005;L006;L007;L008;L009;L010;L011;L014;L016;L023 | Scoped staff→pickup→create/import preview/errors→draft/confirm→prepare Parcels→print→two-party handover→tracking/exceptions; pack != custody |
| J-AB-06 Merchant Staff Authorization | L002;L003 | Invite→recipient personal verify/login→accept→delegated access→revoke; expiry/rejection/replay/current rights/last Owner/multi-org cache tests |
| J-AB-07 Merchant Financial Visibility | L005;L017;L018;L019;L020;L028 | Charges+COD collection/cashier receipt/remittance→eligible reconciliation→statement/dispute→simulated settlement evidence; no inference DELIVERED=paid, closed period immutable |
| J-AB-08 Customer Service / Recipient | L009;L014;L015;L016;L017;L020;L021;L028 | Verified limited Recipient instructions/reschedule/refusal/POD/COD→Merchant Support or Retail carrier CS→claim workflow; no mandatory Recipient account/no final-fact patch |
| J-AB-09 Enterprise Integration | L002;L003;L005;L007;L008;L009;L022 | Machine scope/rotate/revoke→quote/create/label/status→signed webhook→bounded retry/reconciliation; tenant/secret isolation, one business effect |
| J-AB-10 Operational Continuity | L003;L004;L005;L006;L007;L008;L009;L010;L011;L012;L013;L014;L015;L016;L017;L018;L019;L020;L021;L024;L025;L026;L027;L028;L029;L030 | Customer request→pickup→Station→sort→linehaul→destination→Courier attempt/POD or return→Cashier/Finance/support; C–F current scopes, no fake events, faults/recovery/restore/load |

API paths/DTO là contract prerequisite, không suy từ screen label. DB owner trong CSV là source theo BC; workflow/source producers thêm owner cần thiết theo spec contract. Unit/HTTP/PG/browser evidence từng spec trước; cross-Spec composition kiểm ở milestone. Không bắt waiting full late analytics để làm audit/source event đầu tiên.
