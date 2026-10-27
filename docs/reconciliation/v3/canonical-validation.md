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

## Limits

Static checks prove structural preservation, traceability presence, ownership constraints, link integrity and honest planning status; they do not prove production API/DDL/security/business correctness. All future BC runtime evidence stays NOT_STARTED/NOT_VERIFIED.003 DoR remains blocked by the explicit technical Identity/data/policy/privacy contracts. Full portal acceptance remains NOT_STARTED. Existing Foundation/Identity partial status and known findings remain unchanged.
