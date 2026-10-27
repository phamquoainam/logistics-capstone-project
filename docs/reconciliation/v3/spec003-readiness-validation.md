# Spec003 controlled readiness — static validation receipt

Baseline: `5cdb3852707567169c804ac895740cb0a7ced841`. Branch: `docs/spec003-controlled-contract-readiness`. Owner source: “OWNER APPROVAL OF SPEC003 SIX DECISIONS & FINAL DOR CLOSURE”,2026-10-27. Policy approval is recorded; exact new API/DTO/permission/table names remain candidate. No production implementation or migration authorized/performed.

## Actual local checks

- `python -B -m unittest discover -s docs/reconciliation/v3 -p 'test_*.py'`:93 PASS, zero failures/errors. Previous mapping/portal/ownership controls retained; DATA-01 expectation explicitly updated to the latest Owner refinement, with independent self-profile tests. New tests cover direct source NFR/UC trace completeness and phone/grants/session-purpose confusion/invitation invalidation/linearizable revoke/activation/last-Owner/closure/privacy/retention/dependency/false-readiness mutations.
- `node docs/reconciliation/v3/validate_spec003.mjs`: PASS;75 operations,54 HTTP schemas+3 backend dependency schemas,60 positive sample checks,272 negative schema checks and8 contract mutation controls. Pinned existing Ajv8.20.0/formats3.0.1; no dependencies changed or installed. All57 schema definitions compile; examples remain synthetic/declarative.
- `python -B docs/reconciliation/v3/validate_spec003.py`: exit0/errors empty;13 CORE+mandatory DENY045 coverage,75 operations mapped,61 named behavioral scenarios, exact authority/state/dependency/privacy gates. Runtime evidence NOT_IMPLEMENTED.
- `node packages/api-client/node_modules/@redocly/cli/bin/cli.js lint specs/003-sender-merchant-onboarding/contracts/openapi.candidate.json --config contracts/identity-access/redocly.yaml --format=json`: Redocly2.64.0 exit0,0 errors/0 warnings/0 ignored. Existing license metadata exception only; no new rule suppression.
- Full V3 gate: `python -B docs/reconciliation/v3/validate.py`; retains371 BC/33 actors/18 modules/366 CORE+5 DEFER,30 canonical triples (28 new),1024 original legacy records+86 overlay IDs,112 A/B BC/110 CORE/23 supplemental/10 journeys, protected-source/link/history checks plus the new Spec003 gates. Actual final local result: exit0/errors empty, semantic ownership CONSISTENT/no unresolved policy owner, zero broken links/protected changes; staged scope checked before publication.
- `git diff --check` and staged equivalent are required before commit. No app/contracts/packages/infra/scripts/.github/accepted001002/architecture changes; source protections remain pinned to prior verified baseline.

## Real initial failures and corrections

Strict lint initially rejected three backend-only dependency DTOs as unused HTTP components. They were moved to dependency-schemas.json and validated separately, with no lint rule disabled. Initial full static gate reported two links to this not-yet-created receipt; creating the receipt and correcting the relative path from readiness.md closed those findings. A PowerShell stdout code-page error affected display of the lint failure summary only; the UTF-8 JSON report was intact and re-read with explicit UTF-8. No failure is described as PASS without its corrected real run.

## Limits and publication

These are docs/schema/policy/mutation checks, not Java/PostgreSQL/browser execution for Spec003. No service, migration, private storage, email provider, scanner or destructive purge was configured. All61 behavioral scenarios remain PLANNED_NOT_EXECUTED; all implementation tasks unchecked. SMTP/provider evidence, real screened upload, minimum004/005 subset and scoped closure clearances must be proven at their documented first-use/full-acceptance gates. Existing Foundation/Identity findings and partial status remain unchanged.

Required PR Architecture/H2/PostgreSQL18 CI will be recorded on the actual scoped PR/source+integration SHA after push, not inferred from PR31. This task stops for Owner review and never auto-merges the new PR. [DoR assessment](../../../specs/003-sender-merchant-onboarding/readiness.md) distinguishes approved business policy from pending technical acceptance/production authorization and full-spec acceptance.


## Subsequent Owner technical approval / focused final review

Authority: OWNER-S003-TECHNICAL-2026-10-27, supplied request attachment05b7c003-ed3c-4bab-85b1-3ace002e9c12. This supersedes the earlier pending-candidate/stop-for-review instructions above: focused review, minimal direct PR32 correction, conditional normal merge and clean root sync are now authorized. Production coding/migrations remain NOT_AUTHORIZED.

- Read original75 operations/54 HTTP+3 dependency schemas; [operation review](../../../specs/003-sender-merchant-onboarding/contracts/operation-review.csv) now covers77:69 REQUIRED,8 SUPPORTING,0 OUT_OF_SCOPE. Two necessary scoped Business Admin reads added; no unrelated operation retained/removed for count. Findings and controlled dispositions: [technical review](../../../specs/003-sender-merchant-onboarding/contracts/technical-review.md).
- `python -B -m unittest discover -s docs/reconciliation/v3 -p 'test*.py'`: PASS97 tests. Existing controls retained; added planning-source/no-runtime/no-production/complete-operation/current-reviewer-scope guards.
- `node docs/reconciliation/v3/validate_spec003.mjs`: PASS77 operations,55 HTTP+3 backend schemas;65 positive samples,292 negative schema checks,11 contract mutation controls. Initial conditional-schema declaration failed strict checks; corrected nested property definitions/forbidden fields, no rule suppression. Final compile and checks PASS.
- Existing Redocly2.64.0 lint/config: PASS0 errors,0 warnings,0 ignored; no new exception/dependency/tool version changes.
- `python -B docs/reconciliation/v3/validate.py`: PASS371 BC,33 actors,366 CORE+5 DEFER,30 canonical triples,1024 legacy records+86 overlay IDs,112 GroupAB BC/110 CORE,23 supplemental requirements/10 journeys; no broken links, protected changes or ownership conflicts.
- `git diff --check`: PASS. No production BE/FE, SQL, CI, accepted contracts or prior accepted test evidence modified. Architecture/H2/PG runtime suites are checked through exact updated PR integration and merge-SHA CI, not unnecessarily rerun locally for a docs-only patch.
- All61 behavioral scenarios still PLANNED_NOT_EXECUTED; all implementation tasks unchecked. Planning checkpoint SPEC003_READY_FOR_OWNER_IMPLEMENTATION_AUTHORIZATION; implementation NOT_AUTHORIZED; runtime NOT_IMPLEMENTED/NOT_ACCEPTED. First-use delivery/storage/scanner settings and minimum004/005 real dependencies remain mandatory at their respective integration/acceptance steps.

The earlier CI receipts below/above belong to their recorded revisions. They cannot prove the updated PR integration or future merge. Those exact run IDs/checkout SHAs/report totals and artifact digests are verified after push/merge and reported to the Owner; no fabricated PASS is embedded in advance.
