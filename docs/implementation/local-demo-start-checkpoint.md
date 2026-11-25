# Local demo start checkpoint — 2026-11-24

Owner paused P0 implementation and requested a working `scripts/dev/local.cmd start`.

## Startup behavior

- `start` uses the previously verified Backend artifact pinned by SHA-256 in
  `scripts/dev/local-backend-artifact.json`; its ignored local copy is preserved at
  `.demo/local-runtime/backend-v40.jar`.
- No Maven build, Flyway migration, seed, repair or container/volume recreation is
  performed by `start`. The explicit `migrate` action remains separate and is not
  authorized for the current P0 WIP on demo.
- Existing demo container, Compose ownership, persistent volume, loopback port,
  database name, successful V35 baseline and absence of failed migrations are
  checked. A mismatch stops startup rather than changing schema.
- Stale process records are archived. Matching live owned processes cause a stop
  with instructions; PID reuse never authorizes termination.
- Existing certificates, DPAPI-protected Gmail settings, AWS profile and runtime
  secrets are reused without display or rotation. Backend receives no migrator
  credential and has Flyway explicitly disabled.

## Observed verification

- `powershell.exe -NoProfile -ExecutionPolicy Bypass -File scripts/dev/test-start-local.ps1`:
  PASS. Synthetic artifact integrity/tamper, database mismatch/failed migration,
  stale record preservation, live-owner denial and start/migration separation.
- `scripts/dev/local.cmd start`: PASS, `LOCAL_FULL_STACK_HTTP_READY`.
- `scripts/dev/local.cmd status`: PASS, owned Backend/Frontend and TLS-validated
  HTTP 200 probes.
- Read-only post-start history: `elogistics_dev|35|0` (database, latest migration,
  failed migration count). No P0 migrations applied.
- Browser certificate trust PASS; configured Gmail detected. No new live-mail
  delivery test, authenticated Browser UAT, full suite or GitHub CI claim.

## Owner commands

```powershell
.\scripts\dev\local.cmd status
.\scripts\dev\local.cmd stop
.\scripts\dev\local.cmd start
```

Website: https://127.0.0.1:5173/; login: https://127.0.0.1:5173/login.
`stop` terminates only verified owned app processes; PostgreSQL data is preserved.

This preview combines the current Frontend with the previously deployed Backend.
P0 V41–V43 functionality is not deployed or activated on demo. The separate Admin
UAT target was not started or changed. All P0 WIP remains preserved; implementation
is paused. No commit/push/merge.
