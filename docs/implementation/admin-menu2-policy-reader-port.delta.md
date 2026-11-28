# M2-L-POLICY-PORT-01 — exact Java binding for approved M2L-API-14

**OWNER APPROVED — 2026-11-26.** Isolated source/test only.
Parent manifest remains unchanged:
`6d072d9b649ab144c2fb9b8b780c8f077757387bcef58536a31618f9154251a2`.

M2L-API-14 already specifies GET
`/api/v1/admin/merchant-organizations/{organizationId}/governance-policies`
and `M2-CasePolicies`/`org.read`. However M2-PORT-03 omits its Java reader.
The exact proposed addition to `merchant.api.OrganizationGovernanceCases` is:

```java
CasePolicies policies(AdminReadContext actor, UUID organizationId);
```

Merchant verifies the genuine current Identity context, physical transaction,
current Organization `org.read` scope, Local/Test environment, publication state
and validity. Return only the already approved policy metadata DTO, no evidence,
private payload, credentials, actor override or raw receipt. No new HTTP API,
DTO, schema, grant, SQL signature or permission is proposed. Read is immutable;
an absent published policy yields a legitimate empty list, not a fake policy.
No automatic policy publication, real grant or deployment.

The Owner approved this exact method, requiring positive, wrong-scope, revoked
context and unavailable-policy tests to PASS **before endpoint registration**.
Binding source may be implemented; M2L-API-14 remains unregistered until that gate.
Case preparation/review uses its approved interfaces independently.

## Gate evidence

`.demo/cskh-focused-e4de5f4b31ea4d2283926939ce5abe4d`: 2/2 PASS,
unchanged source snapshot
`a91b9d181cf024f2d53d085380c3bba4be9ee44f7a8875505c85d14cda0da879`.
Actual isolated PG18 assertions cover published/absent policy, wrong Organization,
escaped callback and revoked scope. Only after this PASS, the approved API14
endpoint was registered on the preparation controller and existing GET routing
allowlist. No permission/grant/schema/SQL signature delta. HTTPS acceptance is a
separate final integration gate, not inferred from these assertions.
