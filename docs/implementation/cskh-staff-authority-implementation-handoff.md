# ArgoPost — CSKH-IMPLEMENT-01 Execution Handoff

**Purpose:** actionable, fail-closed source/test handoff for the **actual local repository**. This document is not a deployment authorization.  
**Original hash:** `370f965872c4bb3997cc868ebb6d89c945a88035eea484e07f3501c1e9b798cb`  
**ACL-aligned candidate hash:** `acf43f4ed06ac77bf3c405376adc2d7d9e0aa7db2598782b59eef35f4971ebb4`  
**Current gate:** `CHANGES_REQUESTED`; owner approval of the revised exact hash required before source modification.

## Strict boundaries

- Use **only** `D:\TLCN\e-logistic`; do not clone or create a worktree. First read all applicable `AGENTS.md` files.
- Never reset, discard, clean, repair Flyway, edit V1–V35, insert a V33 placeholder, or rewrite historical execution outcomes.
- Do not make speculative API, DTO, schema, or privilege changes outside the approved manifest. Any conflict is a review delta.
- Tests MUST use isolated PostgreSQL 18; never apply V36/V37 to `127.0.0.1:5433/elogistics_dev` during source/test.
- No Staff provision, no grants, no Organization binding, no login in demo, no feature enablement, no runtime restart, no commit/push/merge without independent authorization.

## Preflight inputs and stop criteria

| Gate | Evidence required | Stop when |
| --- | --- | --- |
| Repo & WIP | `git status --porcelain`, HEAD, checked branch, `origin/develop`, AGENTS instructions | dirty, unknown baseline or destructive operation needed |
| Original contracts | JSON SHA `370f965872c4bb3997cc868ebb6d89c945a88035eea484e07f3501c1e9b798cb`, revised candidate SHA `acf43f4ed06ac77bf3c405376adc2d7d9e0aa7db2598782b59eef35f4971ebb4` | adopted content SHA differs from owner-approved exact hash |
| Frozen migrations | V34 `07ebf9fd74aa31c178ab4e849761c4f3b513fdee7c8ad46269713661de6f0f4c`; V35 `33e7e8b532de598627c321a454fd293157fb6c3a365cede946a03c3267d4844b` | mismatch, current V36/V37 already exist or live Flyway conflicts |
| Approved Support | manifest `9484c415356e601ee8192e84a09af2e39f2f448d07244457de1ef79a09c3a940` | source or contract drift |
| DB authority | no public/writer role bypass, `session_user` and physical identity testable | operator or runtime can gain writer identity / privilege check fails |
| Data baseline | isolated PG18 V35 snapshot; live data unchanged | non-Staff ACTIVE canonical grant found, unexpected session/admission schema |

The included `.ps1` preflight script is *read-only* and checks repository-level evidence (not actual DB authority).

## TDD execution slices

| Slice | Primary IDs | Expected engineering action | Must pass before continuing |
| --- | --- | --- | --- |
| A — Identity DDL | DB-01/02/03/05/06/07/08, SQL-01..06/09/10, ACL-01/02/04, GUARD-01/02/03, MIG-01 | Add V36; disabled environment only; real Staff purpose no implicit grant; exact role ownership | PG18 V35→V36, rollback injection, ACL negative direct DML, legacy session fixtures |
| B — Staff identity service | API-01..06, DTO-01..09/11..13, PORT-01/02/04/05/06, CONFIG-01/02 | Controlled offline transaction + seal; proof v2 + protected outbox/SMTP; Staff admission, cookies, sessions and CSRF | T-03/04/05 and Customer/SYSTEM focused regressions |
| C — Canonical grant | DB-04/09/10, SQL-07/08, ACL-03/04/05, GUARD-04, MIG-02, REPLAY-01 | Add V37; request-bound receipt under exact authority; old signature deny-only; V35 binding eligibility forward amendment | T-01/02/06/07/08; no regrant after revoke, no Org bypass |
| D — Workflow/UI | HTTP-AMEND-01, PORT-03, UI-01 | Re-admit exactly six Organization Staff routes, preserve recipients and Merchant branch; distinct Staff desk client | T-05/07/09/10 plus browser/regression architecture |
| E — Gate closure | T-01..T-10 | Consolidate fresh PG18 + HTTP + Browser + security + architecture report | Results recorded individually; no planned-as-PASS or CI inference |

## Evidence report template for actual coding environment

```text
CHECKPOINT: CSKH_STAFF_AUTHORITY_SOURCE_TEST_READY | BLOCKED
MANIFEST_SHA256: <approved exact bytes>
BRANCH / HEAD: <actual>
WORKTREE_WIP_PRESERVED: yes/no
AGENTS_APPLICABLE_READ: <paths>
V34_V35_HASH_CHECK: PASS/FAIL
MIGRATION_STREAM_V36_V37: free/collision
ISOLATED_PG18_IDENTITY: <test database details, no secrets>
SQL_V36_V37_DIFF: <created files and SHA>
SECURITY_GATES_T01..T10: <each with PASS/FAIL and command>
REGRESSION_RESULTS: <command, count, outcome>
ACTUAL_SOURCE_CHANGED: yes/no
DEMO_DB_MIGRATED: no
STAFF_PROVISIONED: no
SUPPORT_GRANTS_CREATED: no
ORG_BINDING_CREATED: no
FEATURE_ACTIVATED: no
COMMIT_PUSH_MERGE: no
BLOCKERS_AND_EXACT_DELTAS: <details or none>
```

This packet intentionally does **not** represent source implementation or PostgreSQL-test success. It keeps the original frozen evidence intact and makes any source drift explicit before the Owner decides on the next approval.
