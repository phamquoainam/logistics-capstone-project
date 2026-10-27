# Canonical V3 — static validation evidence

Baseline: eeb7833ab678240581c407ccd167d7e61ea873c6; branch docs/canonical-v3-group-ab-rebuild. This document records docs/static checks, not feature/runtime acceptance. Exact published commit and PR/CI are reported separately; no evidence from a different source is reused as target acceptance.

## Actual local results — 2026-10-27

- `python -B docs/reconciliation/v3/validate.py`: exit0;371 BC,33 roles,18 modules,366 CORE+5 exact DEFER;30 canonical triples,1024 original legacy records+86 overlay IDs;45 retired legacy files+87 planning promotion records;112 A/B BC,110 CORE+2 denials;23 supplemental requirements,10 journeys; zero errors, zero broken links in changed Markdown, zero protected-source changes.
- `python -B -m unittest discover -s docs/reconciliation/v3 -p test_validate.py`:31 tests PASS, exit0; negative controls cover missing/duplicate BC, dropped guards/roles/modules/requirements, false implementation/acceptance, incorrect Station/pickup ownership, old account-choice flow, missing finance/supplement/journey/denial and wrong portal spec.
- Repository-wide Markdown scan: zero dangling retired-spec local links. Accepted001/002 and architecture docs required no modification to repair retired links.
- `git diff --check`: no whitespace findings; staged scope is checked again before commit.
- Protected diff against baseline: app/contracts/packages/infra/scripts/.github/specs001/specs002/docs architecture unchanged. No migrations/runtime/services/local BE or FE build launched in this documentation-only task. Required backend integration CI is separate after PR publication.

## Initial failure and correction

Initial static run identified86 IDs introduced by prior V3 overlays after the original1024-record registry; original records were retained. retirement-id-supplement.csv preserves exact pinned source statements/lines and validator verifies both sources. Missing canonical-validation link was created. Later stale register wording was corrected and a negative control added. These were documentation failures, not application test failures; final31 tests and complete static validator PASS.

## Bounded BC134 correction — 2026-10-27

The pre-correction documents failed the new semantic gate: BC134 mapped Identity security resources to Merchant, and its DB task instructed Merchant persistence. The correction separates authoritative Identity data from Spec003 workflow/UI orchestration. BC135 Retail Sender business profile remains explicitly unresolved DATA-01 (candidate Merchant, not accepted); this is not a readiness PASS.

- `python -B -m unittest discover -s docs/reconciliation/v3 -p test_validate.py`:50 tests PASS, exit0 (all31 existing controls retained +19 semantic ownership controls). Tests restore the original BC134 design and DB-task sections independently and reject each; correct Identity/Merchant/Dispatch ownership is accepted, contradictory/unknown/shared ownership and silently resolved DATA-01 are rejected.
- `python -B docs/reconciliation/v3/validate.py`: exit0/errors empty, semantic status `CONSISTENT_WITH_EXPLICIT_UNRESOLVED`, unresolved `retail_sender.business_profile:DATA-01`. All371 BC/28 new canonical triples/001–002/accepted-contract and legacy-preservation gates remain active.
- [Resource authority policy](resource-authority-policy.json) defines stable resource IDs/families and typed bindings in the governed BC134/135 design/task sections and portal DB-owner cells. `DECLARED` records resource authority, not implementation or contract approval; source candidate ownership in the flat crosswalk never closes DATA-01. Spec003 remains BLOCKED_FOR_IMPLEMENTATION.
- New integration CI must be verified after publishing this correction; run40460679174 belongs to the previous PR head and cannot prove this patch. Runtime/security sources, accepted contracts, SQL and CI policy are unchanged.
- Repository-wide scan of208 active Markdown files: zero links to retired legacy/planning paths. `git diff --check`: exit0, no whitespace findings.

## Limits (unchanged)

Static checks prove structural preservation, traceability presence, ownership constraints, link integrity and honest planning status; they do not prove production API/DDL/security/business correctness. All future BC runtime evidence stays NOT_STARTED/NOT_VERIFIED.003 DoR remains blocked by the explicit technical Identity/data/policy/privacy contracts. Full portal acceptance remains NOT_STARTED. Existing Foundation/Identity partial status and known findings remain unchanged.

## Later Owner DATA-01 refinement

The BC134 correction results above describe PR31. The subsequent [six-decision Owner record](../../decisions/spec003-six-decisions.md) accepts customer_profile as the independent Retail Sender business authority, with no Merchant org/member prerequisite. The resource policy now distinguishes logical authority from physical module candidate; earlier UNRESOLVED evidence is historical, not the current policy disposition. [Current scoped validation](spec003-readiness-validation.md) records this readiness PR; accepted source protections and prior negative controls remain active.
