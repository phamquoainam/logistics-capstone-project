# Canonical V3 rebuild — documentation review packet

Owner authorization2026-10-27; baseline eeb7833. [Index](../../../specs/INDEX.md), [roadmap](../../implementation/roadmap-v3.md), [retirement](legacy-retirement.md), [account decision](../../decisions/customer-account-merchant-admission.md), [portal coverage](../../portal/group-ab-coverage.md), [benchmark](../../portal/screenshot-benchmark.md), [journeys](../../portal/journeys.md), [acceptance](../../portal/group-ab-acceptance.md).

371 BC/33 actors/18 modules preserved,366 CORE+5 DEFER deny.112 A/B BC=110 CORE+2 denials;23 supplemental Owner portal requirements;10 J-AB journeys. Canonical003–030 one tree,001/002 protected;45 legacy documents retired with exact Git history;87 planning documents promoted/superseded with IDs.1024 old legacy ID/path records retained. No code/SQL/accepted contract/CI-policy changes.

Public one personal account→verify/login→personal→Merchant application/manual sandbox review→controlled org/Owner activation. BC133 role/precondition corrected; no Guest application bypass/anonymous Shipment/Recipient IAM. Pickup004 sole owner, Station011 Dispatch, Sales relationship020/contract005/integration022; ownership-corrections.csv records exact deltas. C–F target retained.

All future runtime targets NOT_STARTED, no feature acceptance from docs/promotion.003 business design corrected but technical DoR BLOCKED as decision register; safe bounded Identity amendment prepared, accepted security untouched. Static docs validators/evidence separate from required PR backend integration CI. See validation.md and final PR report for actual executed revision/results; no invented PASS.
