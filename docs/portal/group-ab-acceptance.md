# GROUP_A_B_PORTAL_FULL_ACCEPTANCE

PRODUCT_MILESTONE / NOT_STARTED_NOT_ACCEPTED. Đây là register cross-Spec, không Spec031 hoặc thay thế phát triển Spec-by-Spec. 112 BC của Group A/B gồm110 CORE+2 DEFER/deny BC006/085; thêm supplemental requirements từ Owner mà matrix cũ không phân biệt đầy đủ. C–F vẫn366 CORE toàn nền tảng, không xóa để ưu tiên portal.

## Gate tích lũy

1. G-AB-SCOPE: mỗi CORE trong [coverage](group-ab-coverage.csv) có một canonical owning requirement/design/task và acceptance; mọi supplemental requirement có owner; BC006/085 direct API deny. Canonical mapping không phải implementation acceptance.
2. G-AB-SPECS: tất cả owning specs của BC/supplement và minimum carrier dependencies có feature-level acceptance đủ scope; không task/PR-only closure. Nếu một spec ngoài A/B còn yêu cầu C–F, không tự đánh dấu full spec accepted vì subset portal xong.
3. G-AB-JOURNEYS: J-AB-01..10 real integrated source-owner APIs/PG/browser và verified external/native channels khi applicable; đủ positive, cross-person/org/resource/role deny, replay/concurrency/timeout/crash/revoke/rollback/recovery. Recipient không IAM; enterprise credential riêng.
4. G-AB-UX: navigation/role/capability matrix khớp backend/current scope/service eligibility, responsive/accessibility, clear loading/empty/error/conflict/pending/unknown, no dead buttons/fake counters/stale context leakage; real captured screenshots sau implementation, không synthetic benchmark thành evidence.
5. G-AB-DATA: shipment/Parcel/state/tracking/attempt/custody/finance sources phân biệt; immutable snapshots/ledger/closed periods, one effect; dashboard/exports tie to source definitions/as-of; privacy/minimization/retention and export formula injection controls.
6. G-AB-QUALITY: unit/architecture/H2/PG18/browser/security/migration/backward-compatibility/CI report/privacy/owned cleanup exact revision; relevant declared NFR load/recovery/restore/source–artifact provenance. NFR thresholds giữ docs/quality; không fabricate benchmark hoặc waive affected mandatory findings.
7. G-AB-OWNER: accepted contracts/decisions; no unresolved blocking business/security/ownership/dependency; integrated PRs và actual merge CI; Owner product-level acceptance record với evidence manifest và known limitations, no real money claim.

## Delivery roadmap

- Foundation/Identity protected baseline: accepted bounded controls reused, existing partial statuses/findings retained. Customer Identity amendment first; do not reopen unrelated foundation redesign.
- F1 customer foundation:003 personal→Merchant admission/membership;004 address/pickup/catalog;005 pricing/service;006 facts;018 minimum charge contract;007 booking;008 Parcel/label;009 tracking;020 public/support core;028 minimum notifications/source audit at first use.
- F2 physical:010 pickup→011 Station/walk-in/staff→012 sort→013 linehaul→014 delivery;017 minimum collection contract before015 POD→016 return. Customer portal integrates real carrier facts, not fake transitions.
- F3 finance/resolution: full017/018/019,020 tickets,021 claims;007/015 consume reviewed minimum money contracts earlier; full financial portal requires complete tie-out/dispute/settlement evidence.
- F4 enterprise/reporting/governance:022 machine/webhooks,023 bulk import (can deliver when007 contract ready),024 Partner,025 network/routing,026 risk,027 admin,028 full analytics/notifications; preserve C–F all assigned requirements.
- F5 029 native offline,030 full J-AB/J1–J9/RQ1–4/load/restore/release. Release/product acceptance separate from code delivery.

Hard dependency means demonstrated minimum contract; preparing a minimum core is internal work decomposition and never full-spec acceptance. Published catalog core may precede dependent feature; record exact incomplete upstream scope rather than declaring it accepted. No 30 mandatory approval prompts; approval remains material business/security/contracts and Owner merge/acceptance boundaries.
