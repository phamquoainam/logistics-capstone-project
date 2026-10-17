# ADR-0006: Isolated Testcontainers PostgreSQL/Flyway verification

- Status: ACCEPTED for D1-08 Task C, explicit user implementation approval2026-10-17.
- Owner: platform/testing; review and maintenance: repository owner.
- Baseline: develop/fe5b440af8f541289a7defc5c2ae39036bfd9b11.
- Scope: additive test suite and existing PostgreSQL CI job. No business behavior,
  migration changes, Redis/RabbitMQ Testcontainers, new required checks or ruleset changes.

## Accepted identities and permissions

Boot4.1.1 manages Testcontainers2.0.5. Add only test-scoped
`org.testcontainers:testcontainers-junit-jupiter` and
`org.testcontainers:testcontainers-postgresql`, without version overrides.
Use `org.testcontainers.postgresql.PostgreSQLContainer`; the tag+digest requires
an explicit `asCompatibleSubstituteFor("postgres")` hint in this version. The
hint does not change the approved bytes or relax the image allowlist.

```text
postgres:18.6-bookworm@sha256:afc7e2d441324c0388fa80c3d24f733b4194a4eb7f47dd8ee2b08eb1a24a647c
testcontainers/ryuk:0.14.0@sha256:7c1a8a9a47c780ed0f983770a662f80deb115d95cce3e2daa3d12115b8cd28f0
```

Ryuk stays enabled and explicitly non-privileged; no automatic privileged
fallback, Docker TCP exposure, disabled startup checks or container reuse.
Only Ryuk mounts the existing daemon socket. This remains a high-trust operation:
non-privileged mode and cleanup labels are not Docker authorization boundaries.

Test-only image substitution and container-command SPIs enforce exact immutable
identities, task labels and random loopback-only published ports. Higher-priority
configuration must pass the effective-policy check. Unknown helper attempts are
recorded and fail verification even if upstream catches the initial exception.
Alpine3.17 is not approved and was not requested/executed by the successful canary.

## Startup documentation reconciliation

The generic [configuration documentation](https://java.testcontainers.org/features/configuration/)
describes a tiny image as always required. The published2.0.5 source instead has
`DockerClientFactory.client()` checking the Docker version, and a lazy mount
check invoked when `ContainerDef` has host binds. Ryuk adds its socket via a later
command modifier. This suite has no application host binds. Actual canary and
suite runs operated with startup checks enabled and only the two approved images.
Do not generalize this result to other modules/versions or disable checks to
avoid a helper. A future helper requirement blocks execution pending approval.

## Fixture and lifecycle

JUnit owns one class-scoped, non-reused container; no Spring context or
`@ServiceConnection`. Each database fixture has independent synthetic owner
(NOLOGIN), migrator and runtime roles. Flyway uses the migrator with SET ROLE
owner; runtime assertions never use bootstrap credentials. V1/V2 remain immutable.

Use a newly allocated anonymous PG18 volume at `/var/lib/postgresql`, fsync and
synchronous_commit on, closed JDBC resources before restart/teardown. Re-read
owned-container port bindings after restart because Docker can allocate a new
random port. Normal teardown removes the container and its anonymous volume;
Ryuk handles session cleanup on JVM disconnect. Ryuk identifies itself by its
session-derived name and task label, not the database's session cleanup label.

Default/H2 excludes the task-c test tag before execution; the explicit Maven
profile enables it in the existing PostgreSQL job. Exact report inventory rejects
missing/skipped Task C cases. Private credential and read-only cleanup audits fail
the job on incomplete evidence, leaks or remaining owned resources.

## Limits

Windows Docker Desktop runtime verified; hosted Linux execution of Task C remains
UNVERIFIED until publication. No deliberate JVM-kill experiment or vulnerability
certification is claimed. Full Feature001/Day1 acceptance remains PARTIAL.
[Evidence](../../implementation/d1-08-testcontainers-evidence.md).
