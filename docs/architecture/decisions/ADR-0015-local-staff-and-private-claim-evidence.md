# ADR-0015 - Local/Test Staff admission and private Claim evidence

Status: Accepted for source/test by explicit Owner standing authorization,
2026-11-20. Deployment/real provisioning/grants/bindings remain separately gated.

## Context

SYSTEM administration stays INACTIVE. A Customer label or Merchant Owner role
cannot provide CSKH identity. Existing canonical Support grants were state-idempotent
and lacked request/operator receipts. Claim document evidence needs private storage
without exposing a Merchant internal adapter across domain boundaries.

## Decision

Reuse Identity credentials, protected invitations, sessions, refresh, CSRF and
SMTP delivery with a distinct Staff purpose/audience. Dedicated Staff remains
pending until genuine invitation possession activation; it receives no default
business or administration grant. Orders retains canonical SUPPORT_TRIAGE/HANDLE
storage and exact current Organization binding/Ticket assignment admission.
V36/V37 add request-bound provenance and receipts, physical Local/Test database
binding and deny-only legacy operator. Trusted isolated test bootstrap creates only
three NOLOGIN prerequisites outside Flyway; runtime/operator cannot assume writers.

Promote the existing byte-only private evidence port and hardened filesystem engine
to Platform; retain a Merchant compatibility alias. Orders owns V38 Claim document
intake, evidence metadata, immutable history, receipts and notices. Workflow composes
public ports under current Identity/Merchant/Support authorization. No new service,
authentication engine, grant engine or public media path is introduced.

Reserve immutable server object identity/content before external I/O. Screen bounded
PNG/JPEG/passive PDF via existing ClamAV policy, then store privately using filesystem
or existing S3 provider. Reauthorize and atomically finalize metadata/event/receipt.
Uncertain finalization leaves inaccessible private bytes for exact retry; no unsupported
business deletion compensation is invented. No scanner/storage network call holds
an application database transaction.

## Consequences and evidence

V1-V35 remain unchanged; forward V36-V38 are tested only on isolated PG18.
[Staff evidence](../../implementation/cskh-staff-authority-execution.md) and
[Claims evidence](../../implementation/organization-claims-intake-execution.md)
record the required security, migration, HTTP/Browser and failure/retry tests.
Compensation policy/eligibility/windows/SoD, SLA promises and financial execution
remain BLOCKED. Controlled S3/ClamAV transports are not live provider acceptance.
No source/test result authorizes demo activation or real identity/grant creation.
