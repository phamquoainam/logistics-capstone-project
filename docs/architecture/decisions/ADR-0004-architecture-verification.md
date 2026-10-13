# ADR-0004: Executable boundaries and minimal trusted actor

- **Status:** ACCEPTED for D1-04 by explicit user instructions, 2026-10-13.
- **Context:** One existing backend artifact; real Java modules identity, merchant,
  orders, platform. Six other business modules have no code. Demo-001 Orders used
  AccountView, although the catalog allowed only merchant/pricing collaboration.
- **Decision:** Use ArchUnit core 1.5.1 in test scope with ordinary JUnit Jupiter
  tests run by existing Maven Surefire. No additional JUnit engine or Spring
  Modulith dependency. Preserve Java 21 / Boot 4.1.1 / Maven 3.10.0. Sources:
  [official guide](https://www.archunit.org/userguide/html/000_Index.html),
  [official releases](https://github.com/TNG/ArchUnit/releases).
- **Rules:** Recognized api/internal packages, cross-module api access, API
  encapsulation, catalog collaborator direction, no module cycles, domain
  independence, application inward direction, web without persistence adapters,
  no controller transactions. Production imports exclude test output. Explicit
  coverage guards and shared-rule negative fixtures prevent vacuous verification.
- **Trusted actor:** Orders needs only id/enabled/role/merchantId. Introduce the
  read-only interface `identity.api.AuthenticatedActor`; existing AccountView
  implements it. ShipmentController receives that interface via
  `@AuthenticationPrincipal`; ShipmentService consumes the same minimal contract.
  Existing signed-token validation, live account lookup, disabled-account check
  and DB-derived authorities remain unchanged. No actor received from request
  fields; no identity import in Orders domain. Allow only that exact type and
  only at Orders HTTP/application boundaries. Do not allow the whole identity API.
- **Adapter direction:** Module-local AccountQueries and ShipmentStore ports
  invert existing repository coupling. Move technical seed runners to their own
  infrastructure packages. SQL, transaction caller, response record fields,
  HTTP routes, auth/RBAC and merchant ownership are preserved. Ports are internal,
  not new cross-module contracts.
- **Trade-off:** The minimal principal contract still couples boundary code to
  Identity's UUID/role vocabulary; it avoids username/display-name dependence and
  changes no wire contract. Application remains selectively Spring-aware for
  transactions/security/error mapping; this is not a fully framework-free model.
  ArchUnit checks bytecode dependencies, not dynamic SQL or runtime ownership.
- **Scope:** Missing modules are NOT YET IMPLEMENTED, not boundary-PASS. CI,
  independent reproduction, full security audit and native build are separate
  gates; successful local architecture tests do not complete feature 001.
- **Evidence:** [mapping/execution](../../implementation/d1-04-architecture-evidence.md).
