# ADR-0005: Explicit Redis/RabbitMQ foundation and authenticated readiness

- Status: ACCEPTED for D1-08 Task B, explicit user authorization2026-10-17.
- Approval: Task B Approved Design B-01..B-09, following PR10/develop
  f20c8c642849ce79a9585ae8c70fe42faaab1c0e. No approval of business caching,
  queues/consumers/events/outbox/inbox, Testcontainers or CI expansion.
- Owner: platform infrastructure; maintenance/review owner: repository owner.

## Accepted configuration

Official unmodified linux/amd64 images, immutable multi-platform index pins:

```text
redis:8.4.7-trixie@sha256:43afe4a9a43080e64dfa29699eef903fc9d54991fe8b03d57237c05bed2a396c
rabbitmq:4.3.6@sha256:25736a5d515fe5dcf4c7eec2125b09a0e2043cc934ac0a7aca198c1058564620
```

Choose Redis AGPLv3; retain upstream license/notices and applicable source
obligations before conveying images. No server modifications or image
redistribution in this task. [License record](../../../infra/redis/NOTICE.md).

Additive standalone Compose project elogistics-foundation, generated container
names, user-defined bridge. It does not include/recreate PostgreSQL. Publish only
127.0.0.1:6380->6379 and127.0.0.1:5673->5672. No management/inter-node ports.
Redis /data tmpfs, save disabled, AOF off; not a correctness store. Rabbit new
elogistics_d1_rabbitmq_dev_data volume, stable foundation-rabbit hostname/node
identity. Existing resources/credentials are never reset or automatically rotated.

Named foundation_probe Redis ACL user; default disabled; only PING/INFO and
required HELLO/SELECT/CLIENT SETNAME/SETINFO. No business writes or +@all.
Rabbit dedicated non-admin user and /elogistics-foundation vhost; configure/write/
read resource patterns match nothing. Private definitions contain salted hashes;
core boot import needs no management plugin. No application queues/exchanges
or consumers. Container diagnostics alone are not proof of AMQP authentication.

Bootstrap settings/ACL/definitions/secrets remain ignored under .demo. POSIX
private permissions; Windows protected directory DACL permits current owner,
SYSTEM and Administrators, descendants inherit it. No machine/global ACL change.
This is local loopback development authentication, not production TLS approval.

## Application and probe boundaries

Boot4.1.1 manages Redis/Lettuce and AMQP/client versions. No explicit version
overrides. Existing default/demo/demo,postgres/migration targets exclude the new
client auto-configurations; foundation opts in only with demo,postgres,foundation.
Redis repositories/reactive auto-config remain excluded; RabbitAdmin dynamic
declaration and listeners disabled. Missing/blank credentials or incomplete
profile combination fail startup. Redis/Rabbit unavailable at runtime means
non-ready; it does not bypass failed Flyway startup.

Liveness=livenessState. Full readiness=readinessState,db,redis,rabbit. Exact GET
security and status-only output from Task A unchanged. Native Boot Redis
indicator performs authenticated INFO; Redis healthcheck uses authenticated PING.

Actual native Rabbit characterization proved cached indicator UP while broker
paused. The approved fallback uses one small Actuator HealthIndicator in platform
infrastructure: health-only Rabbit Java factory, new authenticated connection and
dedicated channel, synchronous basicQos RPC, then abort/close all resources.
It changes no application channel's prefetch/RPC policy, creates no queue/message,
has no background recovery and does not replace Spring-managed application clients.

Targets: Redis connect1s/command2s; probe-only Rabbit connect/handshake/RPC2s each,
shutdown2s, heartbeat10s. Existing JDBC scoped validation and pool settings are
unchanged. Measure readiness failure<12s and recovery<30s on the same backend;
these are verification targets, not a universal HTTP SLA. Later business users
must review their own operation timeouts/permissions; no business use approved here.

## Verification and limits

Real authenticated connections, denied credentials/vhosts/permissions, native
cached-Rabbit negative characterization vs active DOWN, exact HTTP/security,
individual pause/stop/recovery, Redis loss and Rabbit metadata persistence through
restart/recreate, workers/connections/privacy/owned cleanup, old profiles and
H2/PG/architecture/contract/wire regressions. [Evidence](../../implementation/d1-08-foundation-evidence.md).

The original CI matrix stays unchanged; explicit foundation tests have named skips
in its H2/PG modes and must all execute in a separate real-foundation local run.
The report guard rejects missing tests and rejects skips in foundation mode.
No remote full-infrastructure verification is inferred. Full cold-clone onboarding,
Testcontainers, literal red-then-fixed CI and broader security/CI gates remain open.
Rabbit4.3 community support ends2027-01-31; owner reviews before that date.
