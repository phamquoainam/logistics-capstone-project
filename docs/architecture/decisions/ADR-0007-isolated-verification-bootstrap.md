# ADR-0007: Session-owned clean-clone verification bootstrap

- Status: ACCEPTED for the explicitly owner-approved local bootstrap implementation, 2026-10-17.
- Baseline: develop `af0c02096f2560335e187108ea06c2d472bc1842`.
- Owner: platform/testing; review/maintenance: repository owner.
- Acceptance boundary: local implementation is not official AC-001-01 completion.

## Decision

Use an additive standalone `infra/verification/compose.yaml`, not an override of
developer Compose files. The latter carry explicit developer container/volume/DB
identities; merely setting a different project is insufficient. Existing developer
launchers, defaults, migrations, application behavior, API/SDK and CI are unchanged.

The small Python CLI has init/dry-run/up/status/check/serve/down capabilities.
Synthetic credentials and settings are private/ignored, immutable on repeated
invocation. One 32-hex session determines project, database, role identities,
PostgreSQL/Rabbit volumes and network. No fixed container_name; service names are
Compose-generated under that project. Docker publishes only loopback/random host
ports, re-inspected after start/restart. Java listens on loopback port0; temporary
Vite configuration imports the existing config and replaces only verification
proxy/host/port at runtime. No new provider, dependency or business module.

Approved immutable images are derived from existing Compose and cross-checked
against the existing foundation pin register. Canonical repository@digest uses
the same bytes when local caches have no tag; this is not a pin/version change.
No automatic image pull or upgrade; missing images stop startup pending the
documented exact-pin pull. Existing Redis/Rabbit configs are mounted read-only.
Redis default user is disabled, probe ACL minimal and data ephemeral; Rabbit
probe is non-admin in its dedicated vhost with denied configure/read/write and
metadata in a new persistent volume. No management UI or business messaging.

Verification-specific SQL duplicates only the minimal role/database provisioning
invariant: owner NOLOGIN, separate restricted migrator/runtime, public access
revoked and platform schema owned correctly. Test coverage checks these invariants
against the existing strategy. Database/role identifiers use validated generated
names and psql identifier quoting; credentials enter through secret files/env.
Flyway V1/V2 remain the sole unchanged application migrations.

## Safety and trade-offs

Reject inherited connection/Compose/Docker/JVM overrides. Compose uses explicit
files, empty env-file and controlled working directory/environment; Java loads
classpath config only. Runtime URLs derive exclusively from inspected owned
bindings. Only the existing local daemon endpoint is allowed, no Docker TCP/socket
mount into containers. Docker daemon access itself remains high trust.

Resolved Compose output, current resource names/labels/IDs/images/mounts/ports,
network attachments and volume consumers must match before use/removal. Partial
startup records owned resources; mismatch stops without speculative cleanup.
Cleanup removes only captured IDs/names and never prunes, downs a project or
removes developer resources. Baseline inventory drift blocks automatic deletion.
Docker Desktop default-bridge identity drift remains an OPEN operational finding;
explicit independently reviewed recovery is distinct from an automatic PASS.

Costs: a small separate Compose/provisioning path must stay consistent with
approved source configuration. Tests plus effective-config/runtime checks enforce
the relationship. This is verification tooling, not a general orchestration
framework or a replacement for developer/CI infrastructure.

## Evidence and closure

[Implementation evidence](../../implementation/ac-001-01-isolated-bootstrap-evidence.md)
and [runbook](../../runbooks/clean-clone-verification.md). Official AC-001-01 requires
review/commit/publish/CI/owner merge, actual develop CI and an entirely new clean
clone with the complete SDK/web/mobile/infrastructure/backend acceptance sequence.
The old blocked clone is preserved. AC-001-05 remains separate; Feature001,
D1-08 and Day1 remain PARTIAL. Original REQ/AC text is unchanged.
