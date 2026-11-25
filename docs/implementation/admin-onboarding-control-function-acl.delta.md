# AON-ACL-02 — PostgreSQL control-function baseline ACL

Status: **OWNER APPROVED; APPLIED AND FOCUSED TESTED**. Source/test only.
Owner explicitly approved AON-ACL-02 for new isolated test databases.
Parent manifest SHA-256 remains
`025ae708cb97c01cf966a695402f730d5073364d7b0352298b7a27a644c9f0bc`.

## Observed conflict

AON-AMEND-07 states: “runtime/operator get boolean predicate only.”
On the pinned PostgreSQL18 isolated image, effective catalog EXECUTE on
`pg_catalog.pg_control_system()` is already granted through PUBLIC before V39.
V39/V40 do not introduce that PUBLIC grant, but the approved additional GRANT
alone cannot satisfy the stated boundary.

Evidence: `.demo/cskh-focused-03f4d8c71b624c1d9b4ba3537fd334b2`.
The mandatory test reports:
`baseline V38 runtime=true PUBLIC=true; expected false; actual true`.
The same run preserves sourceUnchanged=true. All new AON functions themselves
have no PUBLIC EXECUTE; provenance guards retain SECURITY INVOKER.

That historical run remains FAIL. Its assertion was not removed or weakened;
the approved correction and new PASS evidence are recorded below.

## Exact smallest proposed source patch

Add this statement to the separately privileged **owned isolated database**
prerequisite, before its existing explicit Admin grants:

```sql
REVOKE EXECUTE ON FUNCTION pg_catalog.pg_control_system() FROM PUBLIC;
GRANT EXECUTE ON FUNCTION pg_catalog.pg_control_system()
TO identity_admin_writer, <verified existing migration owner>;
```

The second statement already exists. Preserve the explicit existing
`identity_staff_writer` and migration-owner grants established by the Staff
prerequisite; do not revoke those ACL entries or change memberships.

Changed source locations:

1. `scripts/ci/admin_role_prerequisites.py::database(owner)`.
2. `app/backend/src/test/java/com/elogistic/backend/testsupport/AdminRoleTestBootstrap.java::database(...)`.
3. Their finite prerequisite/catalog tests: assert PUBLIC/runtime/Admin-operator/
   Realm-operator denial, approved writer/owner success and retained Staff behavior.

No new HTTP API, table, permission code, operator membership or definer rewrite.
No V1–V38 modification. No catalog-owner privilege is granted to Flyway/runtime.
The privileged setup owns this catalog ACL change; ordinary Flyway cannot
perform it. Re-running the prerequisite is idempotent.

## Boundary and impact

This tightens a built-in PostgreSQL function ACL, so it is explicitly treated as
a security-boundary delta rather than silently inferred from GRANT authority.
It is scoped to newly owned disposable PG18 test databases in this checkpoint.
No developer/shared/demo/Production database, role membership, credential,
certificate, existing Root or global cluster setting is changed.

Future real isolated deployment still needs exact target authorization and a
record of the original ACL. If another approved service needs the raw function,
stop and review that caller rather than broadening PUBLIC.

## Verification after approval

Re-run the failing catalog gate, Staff writer regression, physical binding,
genuine Root/Admin onboarding and affected prerequisite/report-policy tests.
Keep the historical FAIL evidence. Do not repeat unrelated suites.

The previously failing catalog gate now PASS with Staff/Admin/genuine Root
regression: `.demo/cskh-focused-89b3fca25a984b888c12530cd8a4c1be`,
5/5 PASS, sourceUnchanged=true. Python prerequisite tests4/4 PASS.
PUBLIC/runtime/both operator roles cannot call the raw catalog function;
Admin writer, retained Staff writer and migration owner can.
Final source/test readiness still depends on the remaining consolidated gates,
tracked in [execution](admin-onboarding-execution.md).
