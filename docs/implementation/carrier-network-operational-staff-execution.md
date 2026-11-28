# NWS-V1 — Carrier Network & Operational Staff execution

Date: 2026-11-25. **NWS-V1 FOUNDATION SOURCE/TEST READY.**
This is bounded foundation acceptance, not Full ADM-05/ADM-12, Carrier Operations
E2E, Local UAT or Production readiness. No live deployment or Git publication.

## Authority, preflight and preservation

- Read AGENTS, entire Project North Star, DoR, approved NWS contract/manifest and
  retained Station evidence before implementation. First missing task was the
  dedicated Operations/site foundation; existing Quote/Booking/intake was reused.
- Branch `develop`, unchanged HEAD `d5a0c240841bdb35d9ef6064cddba696a5b2fa62`.
  Prior staged AGENTS/North Star and Menu1 WIP were preserved, not reset/stashed.
- Parent manifest SHA-256 remains
  `8adc4229ff00e728f322dd9341cb146a82e2dc16e1470dc077ec05eb55ac4ce5`.
  Its historical PROPOSED text/bytes are retained. Owner approval and NWS-ACL-02
  are composed in the separate execution manifest, not rewritten into the parent.
- Preflight `.demo/nws-v1-preflight.json` captured 42 historical migration files:
  V1–V43, with V33 historically absent. All 42 final byte hashes still match.
  V44/V45 were unoccupied; only new forward migrations were created.
- All PostgreSQL mutations used the existing owned Testcontainers fixture and
  fresh isolated databases. No demo, Admin-UAT, shared or Production connection;
  no real account, grants, mailbox, credentials or certificate changes.

## Implementation

| Owner | Source and behavior |
|---|---|
| Identity | V44; `OperationalStaffAdmission`, `JdbcOperationalStaffAdmission`, `ProductionOperations*`, fixed Ops guard. Dedicated OPERATIONS purpose/OPS audience, actual invitation/verification, same credential/session/protected outbox engines; separate cookies, JWT audience and limiter partition. No account grants or assignments by default. |
| Dispatch | V45; six registry/version/link/assignment/audit/receipt tables; exact offline request-bound functions and private guards. Actual physical stamp, current effective site/capability/scope, terminal revoke, immutable history. No public/runtime registry or operator writes. |
| Workflow | `OperationalStationHttp` composes genuine Identity context, existing PilotCoverage, Finance UNPAID and the same StationIntake/Orders transaction. Seven exact Ops intake endpoints; no parallel custody engine. |
| Admin read | `CarrierNetworkReads`, `JdbcCarrierNetworkReads`, `CarrierNetworkHttp`, `CarrierSites.tsx`; actual filtered/paged registry/details/topology, gated by existing dashboard + network READ. No write/IAM authority. Official network KPI remains unavailable. |
| Browser | `/ops/login`, `/ops/activate`, `/ops/station`; shared InternalShell and StationOperations, private PDF loader, unknown-write receipt recovery, actionable empty/denied states. Admin nine parent menus remain intact. |
| Test prerequisites | `StaffRoleTestBootstrap` and matching CI prerequisite generator; exact approved NOLOGIN roles/ownership transfer. NWS-ACL-02 only adds private Dispatch EXECUTE; existing AON PUBLIC revoke is not expanded. |

V45 adds an XOR legacy/Operations FK branch to existing scans/acceptances; old
evidence is not rewritten. Ops evidence binds actor + assignment + site/version.
Current Identity locks precede Dispatch/site and Shipment locks; checks run through
commit. Constructed/escaped/suspended/foreign-source contexts fail closed.

Acceptance still requires PERSONAL Local/Test, ROAD_STANDARD, one of the four
approved SIM routes, matching origin SIM-DROP, every Parcel and MATCHED inspection.
It creates one acceptance, all custody/Shipment/Parcel events, audit and receipt
atomically; UNPAID stays UNPAID. Scan, label or topology does not imply acceptance,
Pickup, collection, In Transit or Delivered. Business intake remains denied by the
existing Orders PERSONAL-only reader and the unchanged payment exception.

New source migration SHA-256 (not deployed checksums):

- V44: `02c12c6e4c4ecd0af016377f4eedaa0621501e2e8468b62742064a8ad3cd209c`.
- V45: `07b4d0b4d01a81d870879dafaf4c50e7406b534e2a349b9156ea436db414139d`.

## Required gate evidence

All runs used `scripts/ci/run-cskh-focused.ps1 -Tests '<focused selectors>' -Offline`.
Private directories below retain XML, Maven logs, source fingerprints and lifecycle
records. No private credential/proof payload is copied into this report.

| Gate | Observed result |
|---|---|
| NWS-T-01 | PASS: V43→V45 upgrade exactly two migrations, unchanged old account/profile/address rows and 42 checksums, validate/re-run, exact table ownership/inventory, NOLOGIN/ACL/direct-DML/private-writer negatives. |
| NWS-T-02 | PASS: actual protected Ops invitation; exact issue replay and concurrent same-key activation; wrong Support purpose rejected; SMTP 450→retry→250 uses one protected message, fixed `/ops/activate`, cancellation invalidates lease, ciphertext purged, pending account never auto-activated/granted. Provider acceptance is not live email delivery. |
| NWS-T-03 | PASS: issued object identity, foreign DataSource, constructed/escaped callback, transaction suspension, token expiry and session revocation; separate OPS/Customer/SUPPORT transport/admission. |
| NWS-T-04 | PASS: actual site/version/link projection, current SITE_READ + STATION_INTAKE, wrong Hub/site denial, explicit assignment expiry, inactive site denial and new effective ACTIVE revision, current revoked scope denies even receipt reads; revoked operator cannot replay. |
| NWS-T-05 | PASS: all-Parcels intake; injected late custody failure rolls back; same-key concurrent accept has one effect; revoke/accept race serializes to permitted commit or denial; legacy positive and rollback cases pass; UNPAID unchanged. |
| NWS-T-06 | PASS: one genuine HTTPS server + PG18 + loopback SMTP + built React browser journey. UI verifies Ops invitation, independent test operator assigns actual site, Customer logs in/quotes/books two Parcels, Ops scans both and accepts, private PDF loads, Customer sees real Station event/UNPAID, wrong audiences deny, logout revokes old token. TLS hostname/CA/SPKI checks enabled, no API fulfilment or trust-all TLS. |

Consolidated core run `cskh-focused-85576b6743d54da2b6051316e9c9fd0b`:
**7/7 PASS, zero failures/errors/skips**, source unchanged, fingerprint
`4d33d460bd487857452d37ffe21d6e74056d12e4cd5aed72352004a35164aabc`.
Cases: `nwsOperationsInvitationAndAudienceIsolation`,
`nwsScopedIntakeReusesAtomicCustodyAndCurrentAuthority`,
`nwsRealHttpsBrowserAndPostgres`,
`nwsForwardUpgradePreservesV43RowsChecksumsAndPrivateGuardBoundary`,
`schemasAndObjectsBelongToOwner`, and two existing Station positive/late-fault cases.
All three owned suite containers have stopped records and no lifecycle failure.

Final complementary run `cskh-focused-ff5a881614ec44d5aff536eb60cce773`:
**5/5 PASS**, source unchanged, fingerprint
`186f0dc28ee4dfed3df04aba800673e7ce3918fa63b96fd4c3b2370f401bd9b4`.
Cases: `nwsV44DedicatedOperationsSchemaAndAudience`,
`nwsNetworkOperatorReceiptAndRuntimeAclAreBound`,
`nwsOperationsOutboxFailureRetryAndCancellationArePurposeBound`,
`nwsAdminCarrierDirectoryIsPermissionBound`,
`staffExpiredCancelledAndLateAuditFailureCannotActivateIdentity`.

Shared-engine regression run `cskh-focused-cbbab2b01d4f46a3a0f163f7f8dcd087`:
**3/3 PASS**, same final fingerprint/source unchanged: existing CSKH real HTTP
conversation/current permissions plus real STARTTLS certificate/authentication
and missing-STARTTLS denial. Owned cleanup verified.

Architecture/isolation: 23 architecture/negative-fixture tests plus three
Staff/Ops configuration tests PASS in run `cskh-focused-b85f157889e54360b9cb2e5ae4d9068d`.
That historical run was **27 PASS / 1 FAIL overall**, not a green suite: its browser
failure was fixed and superseded by the core run. These unchanged architecture/
configuration dependencies were not redundantly rerun. Existing 13/13 Station
CI evidence remains retained; only two impacted legacy cases were rerun.

## Frontend, visual and repository verification

- `npm.cmd run typecheck`: PASS.
- Targeted `npx.cmd eslint ... --max-warnings 0`: PASS for changed Ops/Admin/
  shared frontend source and new tests/config.
- `npm.cmd run build:customer`: PASS; repeated by the UI test harness. Existing
  bundle-size warning remains, not suppressed or treated as a functional failure.
- `playwright-unit.config.ts` selected Operations/Staff proof/session, Station
  client and internal navigation: **16/16 PASS**.
- Selected `playwright-customer-ui.config.ts`: Ops empty-state + five existing
  Station UI/private-label tests **6/6 PASS**; affected Admin portal-selector
  test **1/1 PASS** after expecting only the dedicated Ops CSRF/refresh attempts.
  Intercepted UI fixtures are presentation evidence, not PostgreSQL acceptance.
- Real desktop and 390px mobile screenshots inspected: no horizontal overflow,
  clear Station/Parcel state and UNPAID; private PDF remains scoped. Synthetic data
  only; screenshots retained in the private HTTPS runtime artifact.
- `python -m unittest discover -s scripts/ci -p test_staff_role_prerequisites.py`:
  **1/1 PASS**. No Docker lifecycle/cleanup protocol changes.
- `python docs/reconciliation/v3/validate.py --implementation`: PASS, errors empty,
  semantic ownership CONSISTENT, no unresolved ownership. Explicit source inventory
  added; existing historical documentation warnings not hidden.
- Historical migration hashes and approved manifest unchanged; secret-pattern
  scan of new source found no AWS key/private-key material. No real PII added.
- No full Backend suite or GitHub CI run/publication claimed.

## Retained failures and corrections

Earlier red/failed logs remain private. They include missing foundation (TDD),
V44 SQL syntax/function ownership, V45 predicate grant owner, stale inventory,
Ops limiter prefix, a test compile error, duplicate test helper, and Docker Engine
unavailable before container creation. These were not bypassed or hidden.
Final full-stack debugging also found:

1. New Admin directory errors needed controller-local HTTP error mapping.
2. Cross-audience browser assertion required enabling the existing SUPPORT guard
   in the isolated fixture, rather than expecting 401 from a disabled 503 path.
3. Tracking assertion now checks the actual canonical Station-event wording.
4. `RESET ROLE` returned to migration login, giving six new tables the wrong
   owner. V44/V45 now restore the catalog-derived canonical schema owner after
   private-writer blocks; no new membership/privilege was granted. The exact
   ownership test was retained and now passes.

Failed runs include `e4717ea3ab7944b8a8a18c93ebaaa046`,
`cf74f8efd5ed4283bdd339dbd3037a10`, `046486c180cb433fb9225e0964ce35a3`,
`5a0df106f2724477a84fe0681df25596`, `b85f157889e54360b9cb2e5ae4d9068d`,
and `20d1195addb84802a6abb5cfc847b9b7`. Later PASS records above supersede
only their affected gates; they do not relabel historical failures as PASS.

## Next dependency and deployment boundary

No blocker remains for the approved NWS foundation SOURCE/TEST scope.
Deployment and real Operations operator/site/Staff selection remain unapproved.
Admin Staff IAM editor, commercial network registry, site deactivation/custody
transition policy and general Ops lifecycle are not completed by this foundation.

Next smallest E2E unblocker: **spec012 Parcel Inventory → Sorting → Manifest**.
Reuse real AT_STATION custody as inventory source, then approve exact read/command
contracts for destination/sort decisions and immutable Manifest Parcel sets,
sealing/version, discrepancies, scope, idempotency and rollback. Do not derive
movement from a configured topology link. Receiver-confirmed Transfer (spec013)
and Dispatch (spec014) follow separate contracts. No implementation of these
downstream capabilities was started.
