# ADR-0019 — Dedicated Operations admission and Dispatch site foundation

Status: **ACCEPTED — Owner-approved NWS-V1, isolated SOURCE/TEST only** (2026-11-25).

Authority: NWS manifest SHA-256
`8adc4229ff00e728f322dd9341cb146a82e2dc16e1470dc077ec05eb55ac4ce5`
and separately approved NWS-ACL-02. No deployment authorization.

The ADR-0014 Customer-admitted Station pilot remains backward compatible. New
dedicated OPERATIONS Staff uses Identity-owned OPS admission, genuine invitation,
verification and the existing credential/session engine. It does not inherit
Customer, SUPPORT, SYSTEM/Admin, Finance or Courier privileges.

Dispatch owns site/version/topology and current SITE_READ/STATION_INTAKE scopes.
The architecture exception permits only its dependency on the exact public
`OperationalStaffAdmission` port/nested records for owner-verifiable context;
Identity internals remain inaccessible. Workflow composes the existing Orders,
Merchant, Finance and Dispatch ports in one physical transaction. Same intake,
custody and tracking engine, not a parallel authority or state source.

V44/V45 preserve historical migration bytes and legacy FK evidence. No runtime
registry/assignment write or operator membership. Admin directory READ uses
existing dashboard/network permissions and does not imply Ops authority. Private
Dispatch physical-stamp EXECUTE is limited to new owned isolated prerequisites.

Acceptance and limitations: see the [execution report](../../implementation/carrier-network-operational-staff-execution.md).
Sorting, Manifest, Transfer, Dispatch, Business intake, Pickup and collection remain
outside this decision. Admin commercial network KPIs do not treat SIM registry
records as official operational data.
