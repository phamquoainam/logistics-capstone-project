# Demo-001 OpenAPI contract

[demo-001.yaml](demo-001.yaml) records exactly the eleven existing
`/api/demo/v1` HTTP operations under accepted DEC-29. C-01 final technical
acceptance PASS; the user's conditional authorization opened C-02, now verified
locally. [C-02 evidence](../../docs/implementation/d1-07-c02-sdk-evidence.md). This is
not a full production `/api/v1` design, a new response envelope or a runtime
backend change. Implicit framework HEAD/OPTIONS are not added business APIs.

Run from `packages/api-client` with Node24.13.0 / npm11.6.2:

```powershell
npm.cmd ci --no-audit --no-fund
npm.cmd run verify:c01
```

The server prefix is `/api/demo/v1`; each path is relative to that prefix.
Explicit operation security, `x-roles`, `x-ownership` and `x-idempotency` document
existing server enforcement. They do not authorize a caller. Protected requests
use the verified JWT and current database account; owner/actor identity is never
accepted from request bodies. Health is the existing constant local-demo status,
not database/cache/broker readiness.

Component examples and application-error examples are synthetic schema fixtures,
not live response captures or credentials. `accessToken`'s example deliberately
is not a signed token. Actual backend HTTP conformance remains C-03, including
Java normalization, UTF-16 character limits, password UTF-8 byte limits, Jackson
coercion, errors and idempotent replay. Request schemas describe canonical typed
values: DraftInput strings are trimmed before validation and its BigDecimal is
normalized; CancelInput is validated before its reason is trimmed. A raw padded
reference can be accepted after backend trimming even though it fails the
canonical reference pattern. No client-side input validator is installed here.

OAS3.1 nullable fields use JSON Schema types including `null`; no OAS3.0
`nullable` keyword. Only AccountView.merchantId and ShipmentEvent.reason are
nullable in current output records. Application/security errors remain flat
`{code,message}`; `default` deliberately makes no flat-JSON promise for generic
framework/proxy/unhandled failures. Operation-specific statuses/error components
distinguish public invalid-token failures, protected missing credentials, role
denial and unavailable merchant scope.

Source inventory scans production RestControllers and freezes their explicit
eleven method/path pairs. The same checker verifies the reviewed DTO field sets,
constraints, security matchers, schemas, statuses, headers and error examples.
It is a bounded source-drift guard, not a Java compiler or proof of runtime
serialization. Unsupported controller mapping syntax fails closed and needs
review. Negative fixtures use the same checker/Redocly/Ajv as positive fixtures.

Redocly uses `recommended-strict`. Only `info-license` is disabled: the repository
has no approved project license, and a license must not be invented to satisfy
publishing metadata. Structural/reference/operation/security/unused-component
checks stay enabled, and any reported error, warning or ignored problem fails.

See [C-01 evidence](../../docs/implementation/d1-07-c01-contract-evidence.md),
[slice design](../../specs/001-platform-foundation/slices/demo-contract-client/design.md)
and [tooling](../../packages/api-client/README.md). After C-01 owner review this
file is the bounded demo schema source; SDK output is derived from it.
