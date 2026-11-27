# Portable ArgoPost Local/Test baseline

This root Compose stack is separate from the legacy Windows demo launcher and
its database/JAR. Prerequisites: Git, Docker Engine/Desktop with Compose v2.20+
(including Linux containers), internet for initial image/build downloads, roughly
6GB Docker memory and10GB free Docker storage. No host Java/Node, AWS profile,
Gmail secret or private `.demo` artifact is required to start it.

## Start

After cloning the released revision (see PR/Owner release report), from its root:

```sh
docker compose up --build -d
docker compose ps
```

Optional `.env`: copy `.env.example`; only the three public Compose project/port
settings at the top affect this stack. Defaults: project `argopost-baseline`,
Web `https://localhost:5443`, Mailpit `http://localhost:8025`. Use exactly
localhost for browser origin/cookies; changing the port after initialization
fails closed rather than rotating credentials. Use a separate project for a
different isolated installation. PostgreSQL/backend/scanner ports are not
published. The mail UI is loopback-only and holds synthetic Local/Test mail.

First boot builds Web and the one Spring Boot artifact; generates unique keys,
private CA/certificates and DB credentials into named volumes; creates canonical
NOLOGIN prerequisites in a NEW PostgreSQL18 cluster; runs Flyway through V49;
binds LOCAL Staff/Admin physical environments; then starts the application.
The existing BRC-ACL-01 operator is separate from runtime/migrator, has only its
approved membership and performs canonical idempotent ENV_BIND. SYSTEM remains
INACTIVE. No Root/Admin/Staff/Customer, Merchant, permission or Shipment is seeded.

Current accepted pilot is ROAD_STANDARD, four SIM routes, Station Drop-off and
UNPAID. Booking is not acceptance, label is not custody, acceptance is not
transport/delivery/payment. This baseline does not activate Pickup, COD, Finance
collection, Spec012 or deferred Menu2 execution.

## Trust the generated local CA

The CA is unique to this installation. Export only its public certificate:

```sh
mkdir .local
docker compose cp web:/run/web/ca.crt .local/argopost-local-ca.crt
```

If `.local` already exists, keep it. Import this certificate using your OS/browser
trusted local certificate procedure, then open `https://localhost:5443/`.
Verify the exported fingerprint belongs to this exact local Compose project.
Never export the CA private key, database passwords, runtime properties or
invitation proofs into Git/chat. No trust-all TLS or HTTP authentication fallback.

The frontend terminates HTTPS and proxies `/api` to Backend HTTPS with certificate
verification and original Host/Origin. No X-Forwarded authority is accepted.
Mailpit and real ClamAV share Backend's network namespace so existing strict
127.0.0.1 transport/screening guards stay intact. Neither is a cloud adapter.
Merchant evidence uses private filesystem storage outside all public roots.
Do not disable the scanner to make an upload pass; unhealthy scanner means the
Merchant-evidence path is unavailable. Antivirus databases may require an initial
public signature update; no customer/cloud credential is used.

## Initialization and use

Create a synthetic personal account at `/register`, read its actual OTP in the
local mail UI, verify and log in at `/login`. Do not use real customer data.
No shared password exists in the repository. Contacts do not become accounts.

Business and internal identities retain their canonical controlled onboarding.
Merchant Application needs a published policy, independent reviewer/decider,
genuine contact/evidence, Organization activation and explicit approved shipping
grant/contract. Admin needs verified isolated Initial Root/SYSTEM workflow and
dedicated READ grants; Operations needs dedicated verified OPERATIONS purpose
and current exact-site assignments. A fresh `/admin/login` or `/ops/login` is
reachable but does not itself grant authority. BRC environment binding is not
approval to provision identities or activate SYSTEM. Use the existing approved
operator runbooks and appropriate exact target authorization, never INSERT/UPDATE
accounts/grants/custody to manufacture a demo. Actual isolated S1 test fixtures
exercise those workflows separately; they do not seed this persistent volume.

## Health, restart, and read-only verification

```sh
docker compose ps
docker compose stop
docker compose up -d
```

Ordinary restart reuses all named volumes, keys, role credentials and immutable
Flyway history. Jobs rerun validation and the same bound requests; no duplicate
account/grant/binding audit. Backend health checks actual runtime SQL connectivity
and HTTPS; Web health checks HTTPS through its verified Backend proxy. These are
container probes, not invented Admin platform-health metrics.

Optional developer verification (Python3, with Docker available):

```sh
python scripts/dev/verify-compose.py --project argopost-baseline
```

It reads the explicit target only and prints non-secret role/ACL, history and
health metadata. Record cold and restart results; migration scripts/checksums,
operator boundary, environment receipts and data must remain consistent. The
initial empty-account snapshot is distinct from later explicitly created test
accounts. No scripts repair roles, clean Flyway, truncate or reset volumes.

Optional synthetic Customer smoke (Node24, frontend npm dependencies and
Playwright Chromium, required only for the test): export public leaf certificate
`docker compose cp web:/run/web/web.crt .local/argopost-web.crt`, then run
`node infra/local/browser-smoke.mjs`. It pins that certificate's SPKI and uses real
local SMTP OTP. Each invocation creates one new example.invalid Customer without
privileged grants or deletion of account history. On Windows process-local
ARGOPOST_BROWSER_CHANNEL=msedge uses installed Edge; CI uses Chromium. Custom
ports use ARGOPOST_SMOKE_ORIGIN and ARGOPOST_SMOKE_MAIL. No secrets are output.

Rollback is target-only `docker compose stop`; retain volumes and investigate.
The old Windows `scripts/dev/local.cmd` remains unchanged for its existing demo
and must not be pointed at this database or updated with a WIP JAR.

## Resource and data preservation

Image/build/DB storage follows Docker Desktop's configured disk image location;
do not move it or prune unrelated resources. Container build caches stay in the
daemon. Host verification logs/Maven evidence should use a workspace directory
on a disk with capacity. No AWS credentials, home directory, Docker socket or
host database volume is mounted into an application container.

**Destructive reset is not an ordinary command.** No reset helper is enabled in
this release. If a developer separately consents to discarding one disposable
installation, first verify its project/container/volume labels, export and test
a backup, and list the exact local volumes to remove. Never copy a generic
`down -v`/prune command into routine startup or operate on another team's project.

## Courier/mobile

Mobile is an Expo57 host/device process, not an Android emulator inside Compose.
The existing host commands remain `npm ci`, `npm run typecheck`, `npm run lint`,
then `npm run start:go` from `app/frontend/mobile` where applicable. No Courier
admission/DeliveryRun/POD capability is created by containerizing Web. A physical
device needs separately reviewed network binding and CA trust; do not expose
this loopback stack to LAN or claim device validation without testing it.

## Release and handoff

Use the exact release SHA from the execution report/PR; do not assume dirty local
source equals develop. Hoàng and Triết must verify that SHA and their own clean
start before claiming three-machine parity. Spec012 stays PROPOSED/BLOCKED.
Temporary baseline integration writer is Nam; operational domain source handback
requires explicit path/version and independent consumer ACK. No team ACK is
implied by this document or by a single-host clean-clone test.
