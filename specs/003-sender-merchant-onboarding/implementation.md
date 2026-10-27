# Spec003 production implementation — execution record

Authority: **OWNER_SPEC003_FULL_IMPLEMENTATION: GO**, Owner request “OWNER SPEC003 FULL PRODUCTION IMPLEMENTATION AUTHORIZATION”, attachment `69272a3b-e360-4030-a839-d79c04875906/Pasted text.txt`. The Owner authorizes **all T003-01..12**, necessary controlled Identity amendments, additive owner-specific migrations, BE/API/PG18/FE/browser/integrations/tests/CI and scoped implementation PRs. Routine implementation does not need repeated Owner approvals. Production PR merge, full-spec acceptance, public rollout and destructive purge retain their separate gates.

Verified starting baseline: develop `715a41139f9724cbd3f20526786a61f673393564`, clean root and one worktree. Normal feature branch `feature/spec003-full-onboarding` uses the same root; no new worktree, reset, stash or unrelated historical commits. Canonical inventory reconciled:77 operations (69 required/8 supporting),55 HTTP schemas,3 backend dependency schemas,13 CORE+DENY045 and61 planned behavioral scenarios.

## Current increment — T003-01 and Identity part of T003-02

Implemented source:

- Explicit SYSTEM/CUSTOMER session persistence selection; existing constructors preserve SYSTEM behavior. Customer storage resolves only a current exact-email index matching the existing verified ACTIVE account. No Merchant security master or public grants.
- Customer login identifier validation preserves exact spelling/case, accepts the reviewed254-character email bound independently of legacy80-character usernames, rejects phone/whitespace and never transforms security email.
- Public CustomerAdmission verifies signed customer-token facts through a mandatory inward verifier, requires live same-request admission evidence, owns its physical transaction and retains administration/account/session locks through final deadline/request recheck. Captured/constructed/cross-thread contexts have no authority. New successful business activity uses a second owned transaction after business commit; reads/receipts do not extend idle.
- Forward V7 adds customer_identifier and isolated customer session/refresh/CSRF relations with lifetime/lineage/uniqueness/FK/least-privilege controls. V1–V6 untouched. No migration has yet been executed by this increment's local unit tests.

Actual local evidence:

- Maven Wrapper/JDK21 focused run:12 customer-session +24 original session cases, **36 PASS/0 fail/error/skip**.
- Subsequent focused run including admission/architecture:12 customer-session +15 admission +24 original session +23 architecture, **74 PASS/0 fail/error/skip**. These are overlapping runs, not110 distinct tests.
- Added8 real-PG customer application cases and4 migration/upgrade/constraint/privilege cases to existing suites and their owned container lifecycle. **Authored, not executed yet.** Exact finite report inventory extended; previous tests/skips/privacy/cleanup controls retained.

T003-01/02 are **IN_PROGRESS**, not DONE. No customer HTTP enrollment/verification/UI availability or complete journey is claimed by this kernel increment. All61 behavioral scenarios remain PLANNED_NOT_EXECUTED pending complete runtime assertions; existing Foundation/Identity findings and acceptance statuses remain unchanged.

Next implementation work: execute current PG/required CI, then connect enrollment/proof/delivery and customer transport to the current application; build independent customer profile and Merchant journeys in the approved dependency order. Missing production credentials do not block independent work. Required real sandbox delivery/storage/screening, BC159 minimum owner contracts, browser E2E, compatibility/security and final full-spec acceptance remain mandatory.
