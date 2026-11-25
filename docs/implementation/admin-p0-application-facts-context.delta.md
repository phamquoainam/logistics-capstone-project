# P0-FACTS-CTX-01 ? owner-verifiable application facts access

Status: **OWNER APPROVED on2026-11-24; implementation pending focused gates**.
Owner approved P0-FACTS-CTX-01 for isolated SOURCE/TEST; no deployment or real grants.
Scope: isolated SOURCE/TEST only. Parent manifest remains byte-identical:
`90c795bd7e773b6a58d0d220056cfbc5e9a5ee95b38ae2493d5b9f75a372291f`.

## Exact integration gap

P0-PORT-05 approves AdminApplicationFacts.capture(Access,ApplicationCohort,Instant),
page(Access,ApplicationCohort,Selector,int,int) and existing availability(Access).
The existing Access interface exposes only void requireApplicationFacts(). That
caller-implemented check cannot prove an issued Identity context or identify the
current actor/realm/domain scopes to the Merchant owner. Constructed no-op Access
objects must never authorize a new canonical Merchant cohort reader. This is a
missing integration contract, not evidence of a deployed HTTP exploit.

## Exact proposed addition

```java
// Existing merchant.api.AdminApplicationFacts.Access only
default AdminReadContext identityContext() {
    throw new AdminFailure("ACCESS_DENIED");
}
```

No change to Identity interfaces or existing P0-PORT-05 method signatures.
Workflow's existing private Access adapter returns the genuine context it receives
inside AdminAdmission.execute. Merchant injects the existing AdminAdmission and
calls its approved requireCurrent(context) BEFORE actor/realm/permission access.
A caller-supplied context.requireCurrent() or requireApplicationFacts() alone is
never accepted. Merchant verifies same physical DataSource, current read-only
repeatable-read callback and current persisted application.read assignments.
The context is server-local authority; it is never JSON/HTTP input or output.

Old callers retain fail-closed behavior and the unchanged v1 unavailable source.
The new v2 source is not connected until the exact owner verification is installed
and tested. No registry/private Identity import, reflection or privileged SQL.

No new HTTP path, wire DTO field, schema/migration, role, permission, grant,
operator authority, runtime feature flag, provider or real recipient. No production,
demo/UAT deployment, real grants/mail, commit/push/merge.

## Required gates

- Genuine issued same-source context succeeds with exact current scope.
- Default/no-op Access without a context denies.
- Constructed, actor-substituted and forwarding contexts deny even when forwarding
  a genuine requireCurrent implementation.
- Escaped callback, transaction suspension, cross-source and revoked/expired
  Identity/domain scope deny.
- Missing domain scope is unavailable, never all records or a fabricated0.
- Resubmission counted once; first-submission cohort, terminal durations and
  metadata/drill-through use the same authorized canonical population.
- Existing v1 exact wire shape and original constructed READ/COMMAND gates pass.
