package com.elogistic.backend.identity.internal.infrastructure;

import com.elogistic.backend.identity.api.IdentityCommandFailure;
import com.elogistic.backend.identity.api.IdentityGrantCommands.*;
import com.elogistic.backend.identity.internal.application.ReplaceIdentityGrants;
import com.elogistic.backend.identity.internal.application.port.GrantAuthority;
import com.elogistic.backend.identity.internal.domain.*;
import com.elogistic.backend.testsupport.PostgresTestcontainersSupport;
import com.elogistic.backend.testsupport.PostgresTestcontainersSupport.Database;
import java.sql.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.parallel.*;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import static com.elogistic.backend.identity.api.IdentityCommandFailure.Code.*;
import static org.junit.jupiter.api.Assertions.*;

/** Real V3/runtime-role transactions, no Spring/developer URL or H2 replacement. */
@Tag("task-c") @Testcontainers @Execution(ExecutionMode.SAME_THREAD)
class IdentityGrantApplicationTests {
    @Container static final PostgreSQLContainer POSTGRES = PostgresTestcontainersSupport.container();
    private static final Instant NOW = Instant.parse("2026-10-21T00:05:00Z");
    private static final UUID A = id(1), B = id(2), T = id(3), S = id(4), TS = id(5),
        AS = id(6), SCOPE = id(7), X = id(8), Y = id(9), KEY = id(10), REQUEST = id(11);
    private static final GrantData ADMIN = new GrantData("IDENTITY_MANAGEMENT", "IDENTITY", SCOPE);
    private static final GrantData GX = new GrantData("OPERATOR", "WAREHOUSE", X);
    private static final GrantData GY = new GrantData("OPERATOR", "WAREHOUSE", Y);
    private static final List<String> TABLES = List.of("user_account", "account_grant", "auth_session", "refresh_token", "audit_entry", "command_receipt");
    private Database db;
    private ReplaceIdentityGrants service;
    private GrantAuthority authority;

    @BeforeEach void fixture() throws Exception {
        db = PostgresTestcontainersSupport.database(POSTGRES);
        assertEquals(3, db.flyway("latest").migrate().migrationsExecuted);
        account(A, "ACTIVE", List.of(ADMIN)); account(B, "ACTIVE", List.of(ADMIN)); account(T, "ACTIVE", List.of(GX));
        session(S, A); session(AS, B); session(TS, T);
        execute("INSERT INTO identity.refresh_token(id,session_id,token_hash,issued_at,expires_at) VALUES ('" + id(20) + "','" + TS + "',decode(repeat('01',32),'hex'),TIMESTAMPTZ '2026-10-21 00:00:00Z',TIMESTAMPTZ '2026-10-21 08:00:00Z')");
        // Synthetic in-process owner fixture only. Never registered as a production provider.
        authority = (actor, target, proposed) -> {
            Set<Grant> known = Set.of(grant(ADMIN), grant(GX), grant(GY));
            return new ManagementAuthority(actor.id(), actor.version().authVersion(), target.id(),
                target.version().version(), known, known);
        };
        service = useCase(authority);
    }
    @AfterAll static void verifyPolicy() throws Exception { PostgresTestcontainersSupport.verifyRuntime(POSTGRES); }
    private static UUID id(int n) { return UUID.fromString(String.format("10000000-0000-4000-8000-%012d", n)); }
    private static Grant grant(GrantData g) { return new Grant(new RoleCode(g.role()), new ResourceScope(ResourceScope.Type.valueOf(g.scopeType()), g.scopeId())); }
    private ReplaceIdentityGrants useCase(GrantAuthority provider) {
        var dataSource = new DriverManagerDataSource(db.url(), db.runtime(), db.runtimePassword());
        return new ReplaceIdentityGrants(new JdbcIdentityGrantStore(dataSource), provider, Clock.fixed(NOW, ZoneOffset.UTC), Duration.ofHours(24));
    }
    private Command command() { return command(S, T, List.of(GX, GY), 1, KEY, "Synthetic scope replacement"); }
    private Command command(UUID session, UUID target, List<GrantData> grants, long version, UUID key, String reason) {
        return new Command(session, target, grants, version, key, reason, REQUEST);
    }
    private void account(UUID id, String state, List<GrantData> grants) throws Exception {
        try (Connection c = db.runtimeConnection(); PreparedStatement s = c.prepareStatement(
                "INSERT INTO identity.user_account(id,username,display_name,email,password_hash,status,email_verified,created_at,updated_at,activated_at,disabled_at) " +
                "VALUES (?,?,?,? ,?, ?,TRUE,TIMESTAMPTZ '2026-10-21 00:00:00Z',TIMESTAMPTZ '2026-10-21 00:00:00Z',TIMESTAMPTZ '2026-10-21 00:00:00Z',?)")) {
            s.setObject(1, id); s.setString(2, "fixture." + id); s.setString(3, "Synthetic Account");
            s.setString(4, "fixture@example.invalid"); s.setString(5, "$2b$12$" + ".".repeat(53)); s.setString(6, state);
            s.setTimestamp(7, "DISABLED".equals(state) ? Timestamp.from(NOW.minusSeconds(60)) : null); s.executeUpdate();
        }
        for (GrantData g : grants) {
            try (Connection c = db.runtimeConnection(); PreparedStatement s = c.prepareStatement(
                    "INSERT INTO identity.account_grant VALUES (?,?,?,?,TIMESTAMPTZ '2026-10-21 00:00:00Z')")) {
                s.setObject(1, id); s.setString(2, g.role()); s.setString(3, g.scopeType()); s.setObject(4, g.scopeId()); s.executeUpdate();
            }
        }
    }
    private void session(UUID id, UUID account) throws Exception {
        execute("INSERT INTO identity.auth_session VALUES ('" + id + "','" + account + "',1,TIMESTAMPTZ '2026-10-21 00:00:00Z',TIMESTAMPTZ '2026-10-21 08:00:00Z',TIMESTAMPTZ '2026-10-21 00:00:00Z',TIMESTAMPTZ '2026-10-21 00:30:00Z',NULL)");
    }
    private void execute(String sql) throws Exception {
        try (Connection c = db.runtimeConnection(); Statement s = c.createStatement()) { s.execute(sql); }
    }
    private long scalar(String sql) throws Exception {
        try (Connection c = db.runtimeConnection(); Statement s = c.createStatement(); ResultSet r = s.executeQuery(sql)) { assertTrue(r.next()); return r.getLong(1); }
    }
    private Map<String, List<String>> snapshot() throws Exception {
        Map<String, List<String>> rows = new TreeMap<>();
        for (String table : TABLES) {
            try (Connection c = db.runtimeConnection(); Statement s = c.createStatement(); ResultSet r = s.executeQuery(
                    "SELECT row_to_json(t)::text FROM identity." + table + " t ORDER BY row_to_json(t)::text")) {
                List<String> values = new ArrayList<>(); while (r.next()) values.add(r.getString(1)); rows.put(table, values);
            }
        }
        return rows;
    }
    private void rejected(IdentityCommandFailure.Code code, Command command) throws Exception {
        var before = snapshot(); var error = assertThrows(IdentityCommandFailure.class, () -> service.replaceIdentityGrants(command));
        assertEquals(code, error.code()); assertEquals(code.name(), error.getMessage()); assertEquals(before, snapshot());
    }
    private void failWrite(String table, String event) throws Exception {
        // Test-only DDL in this fresh synthetic DB, never the runtime role/developer database.
        try (Connection c = db.migrationConnection(); Statement s = c.createStatement()) {
            s.execute("SET ROLE " + db.owner());
            s.execute("CREATE FUNCTION identity.synthetic_fault() RETURNS trigger LANGUAGE plpgsql AS $$ BEGIN RAISE EXCEPTION 'SYNTHETIC_WRITE_FAILURE'; END $$");
            s.execute("CREATE TRIGGER synthetic_fault BEFORE " + event + " ON identity." + table + " FOR EACH ROW EXECUTE FUNCTION identity.synthetic_fault()");
        }
    }
    private void rollback(String table, String event) throws Exception {
        failWrite(table, event); var before = snapshot();
        assertThrows(org.springframework.dao.DataAccessException.class, () -> service.replaceIdentityGrants(command()));
        assertEquals(before, snapshot());
    }
    private List<Object> race(Command left, Command right) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(2); CountDownLatch ready = new CountDownLatch(2), go = new CountDownLatch(1);
        try {
            List<Future<Object>> futures = new ArrayList<>();
            for (Command input : List.of(left, right)) futures.add(pool.submit(() -> {
                ready.countDown(); if (!go.await(5, TimeUnit.SECONDS)) throw new AssertionError("Start gate timed out");
                try { return service.replaceIdentityGrants(input); } catch (IdentityCommandFailure e) { return e.code(); }
            }));
            assertTrue(ready.await(5, TimeUnit.SECONDS)); go.countDown();
            return List.of(futures.get(0).get(20, TimeUnit.SECONDS), futures.get(1).get(20, TimeUnit.SECONDS));
        } finally { go.countDown(); pool.shutdownNow(); assertTrue(pool.awaitTermination(5, TimeUnit.SECONDS)); }
    }

    @Test void authorizedReplacementPersistsMultipleScopesVersionsAuditAndReceipt() throws Exception {
        Result result = service.replaceIdentityGrants(command(S, T, List.of(ADMIN, GX, GY), 1, KEY, command().reason()));
        assertFalse(result.replayed()); assertEquals(2, result.appliedVersion()); assertEquals(2, result.account().version());
        assertEquals(Set.of(ADMIN, GX, GY), Set.copyOf(result.account().grants()));
        assertEquals(2, scalar("SELECT version FROM identity.user_account WHERE id='" + T + "'"));
        assertEquals(2, scalar("SELECT auth_version FROM identity.user_account WHERE id='" + T + "'"));
        assertEquals(3, scalar("SELECT count(*) FROM identity.account_grant WHERE account_id='" + T + "'"));
        assertEquals(1, scalar("SELECT count(*) FROM identity.audit_entry WHERE actor_id='" + A + "' AND target_id='" + T + "' AND event_type='IDENTITY_GRANTS_REPLACED' AND request_id='" + REQUEST + "'"));
        assertEquals(1, scalar("SELECT count(*) FROM identity.command_receipt WHERE actor_id='" + A + "' AND target_id='" + T + "' AND original_version=1 AND result_version=2 AND result_status=200 AND octet_length(request_fingerprint)=32"));
    }
    @Test void allTargetSessionsAndRefreshTokensAreRevokedWithoutTouchingActor() throws Exception {
        session(id(30), T); service.replaceIdentityGrants(command());
        assertEquals(2, scalar("SELECT count(*) FROM identity.auth_session WHERE account_id='" + T + "' AND revoked_at IS NOT NULL"));
        assertEquals(1, scalar("SELECT count(*) FROM identity.refresh_token WHERE revoked_at IS NOT NULL"));
        assertEquals(0, scalar("SELECT count(*) FROM identity.auth_session WHERE account_id='" + A + "' AND revoked_at IS NOT NULL"));
    }
    @Test void authorizedReplayPrecedesStaleVersionAndDoesNotRepeatEffects() throws Exception {
        service.replaceIdentityGrants(command()); var after = snapshot();
        Result result = service.replaceIdentityGrants(command());
        assertTrue(result.replayed()); assertEquals(2, result.appliedVersion()); assertEquals(after, snapshot());
    }
    @Test void reorderedGrantSetHasSameCanonicalReceiptFingerprint() throws Exception {
        service.replaceIdentityGrants(command()); var before = snapshot();
        assertTrue(service.replaceIdentityGrants(command(S, T, List.of(GY, GX), 1, KEY, command().reason())).replayed());
        assertEquals(before, snapshot());
    }
    @Test void sameKeyChangedPayloadConflictsWithoutWrites() throws Exception {
        service.replaceIdentityGrants(command()); rejected(IDEMPOTENCY_CONFLICT, command(S, T, List.of(GX), 1, KEY, command().reason()));
    }
    @Test void sameKeyChangedTargetConflictsWithoutWrites() throws Exception {
        service.replaceIdentityGrants(command()); rejected(IDEMPOTENCY_CONFLICT, command(S, B, List.of(ADMIN), 1, KEY, command().reason()));
    }
    @Test void sameKeyChangedExpectedVersionConflictsBeforeStaleCheck() throws Exception {
        service.replaceIdentityGrants(command()); rejected(IDEMPOTENCY_CONFLICT, command(S, T, command().grants(), 2, KEY, command().reason()));
    }
    @Test void sameKeyChangedReasonConflictsWithoutWrites() throws Exception {
        service.replaceIdentityGrants(command()); rejected(IDEMPOTENCY_CONFLICT, command(S, T, command().grants(), 1, KEY, "Other synthetic reason"));
    }
    @Test void revokedActorSessionCannotReplayReceipt() throws Exception {
        service.replaceIdentityGrants(command()); execute("UPDATE identity.auth_session SET revoked_at=TIMESTAMPTZ '2026-10-21 00:05:00Z' WHERE id='" + S + "'");
        rejected(SESSION_INVALID, command());
    }
    @Test void lostManagementAuthorityCannotReplayReceipt() throws Exception {
        service.replaceIdentityGrants(command()); execute("DELETE FROM identity.account_grant WHERE account_id='" + A + "'");
        rejected(ACCOUNT_NOT_ACCESSIBLE, command());
    }
    @Test void missingOwnerProviderDefaultsDenyEvenForEmptyTarget() throws Exception {
        execute("DELETE FROM identity.account_grant WHERE account_id='" + T + "'"); service = useCase(null);
        rejected(ACCOUNT_NOT_ACCESSIBLE, command());
    }
    @Test void partialAuthorityCannotRemoveHiddenCurrentGrant() throws Exception {
        service.replaceIdentityGrants(command());
        service = useCase((actor, target, input) -> new ManagementAuthority(actor.id(), actor.version().authVersion(), target.id(), target.version().version(), Set.of(grant(GX)), Set.of(grant(GX))));
        rejected(ACCOUNT_NOT_ACCESSIBLE, command(S, T, List.of(GX), 2, id(31), command().reason()));
    }
    @Test void requestedGrantOutsideAssignableScopeIsDenied() throws Exception {
        service = useCase((actor, target, input) -> new ManagementAuthority(actor.id(), actor.version().authVersion(), target.id(), target.version().version(), Set.of(grant(GX), grant(GY)), Set.of(grant(GX))));
        rejected(ACCOUNT_NOT_ACCESSIBLE, command());
    }
    @Test void nonManagementActorCannotUsePermissiveOwnerFixture() throws Exception {
        rejected(ACCOUNT_NOT_ACCESSIBLE, command(TS, B, List.of(ADMIN), 1, KEY, command().reason()));
    }
    @Test void selfGrantManagementIsForbidden() throws Exception {
        rejected(SELF_MANAGEMENT_FORBIDDEN, command(S, A, List.of(ADMIN), 1, KEY, command().reason()));
    }
    @Test void lastAdministratorCannotRemoveItsOwnManagementGrant() throws Exception {
        execute("DELETE FROM identity.account_grant WHERE account_id='" + B + "'");
        rejected(SELF_MANAGEMENT_FORBIDDEN, command(S, A, List.of(GX), 1, KEY, command().reason()));
        assertEquals(1, scalar("SELECT count(DISTINCT account_id) FROM identity.account_grant WHERE role_code='IDENTITY_MANAGEMENT'"));
    }
    @Test void staleNonReplayVersionIsRejectedWithoutWrites() throws Exception {
        rejected(PRECONDITION_STALE, command(S, T, List.of(GX), 2, KEY, command().reason()));
    }
    @Test void duplicateGrantsAreRejectedWithoutWrites() throws Exception {
        rejected(VALIDATION_FAILED, command(S, T, List.of(GX, GX), 1, KEY, command().reason()));
    }
    @Test void emptyReplacementIsRejectedByAcceptedContractBoundary() throws Exception {
        rejected(VALIDATION_FAILED, command(S, T, List.of(), 1, KEY, command().reason()));
    }
    @Test void malformedScopeIsRejectedWithoutWrites() throws Exception {
        rejected(VALIDATION_FAILED, command(S, T, List.of(new GrantData("OPERATOR", "GLOBAL", X)), 1, KEY, command().reason()));
    }
    @Test void unknownResourceScopeIsNotGrantedImplicitly() throws Exception {
        rejected(ACCOUNT_NOT_ACCESSIBLE, command(S, T, List.of(new GrantData("OPERATOR", "WAREHOUSE", id(90))), 1, KEY, command().reason()));
    }
    @Test void missingAndUnauthorizedTargetsHaveSameSafeClassification() throws Exception {
        rejected(ACCOUNT_NOT_ACCESSIBLE, command(S, id(99), List.of(GX), 1, KEY, command().reason()));
        service = useCase(null); rejected(ACCOUNT_NOT_ACCESSIBLE, command());
    }
    @Test void disabledActorCannotSubmitCommand() throws Exception {
        execute("UPDATE identity.user_account SET status='DISABLED',disabled_at=TIMESTAMPTZ '2026-10-21 00:01:00Z' WHERE id='" + A + "'");
        rejected(SESSION_INVALID, command());
    }
    @Test void expiredSessionAndChangedAuthVersionAreRejected() throws Exception {
        rejected(SESSION_INVALID, command(id(98), T, List.of(GX), 1, KEY, command().reason()));
        execute("UPDATE identity.auth_session SET idle_expires_at=TIMESTAMPTZ '2026-10-21 00:05:00Z' WHERE id='" + S + "'");
        rejected(SESSION_INVALID, command());
        execute("UPDATE identity.auth_session SET idle_expires_at=TIMESTAMPTZ '2026-10-21 00:30:00Z',auth_version=2 WHERE id='" + S + "'");
        rejected(SESSION_INVALID, command());
    }
    @Test void invalidCommandMetadataDoesNotMutateState() throws Exception {
        rejected(VALIDATION_FAILED, command(S, T, List.of(GX), 1, KEY, ""));
        rejected(VALIDATION_FAILED, command(S, T, List.of(GX), 1, null, command().reason()));
        rejected(VALIDATION_FAILED, command(S, T, List.of(GX), 1, KEY, "\uD800"));
    }
    @Test void disabledTargetStaysDisabledAndDoesNotRegainSessions() throws Exception {
        execute("UPDATE identity.user_account SET status='DISABLED',disabled_at=TIMESTAMPTZ '2026-10-21 00:01:00Z' WHERE id='" + T + "'");
        assertEquals("DISABLED", service.replaceIdentityGrants(command()).account().status());
        assertEquals(1, scalar("SELECT count(*) FROM identity.auth_session WHERE account_id='" + T + "' AND revoked_at IS NOT NULL"));
    }
    @Test void auditWriteFailureRollsBackAllSecurityEffects() throws Exception { rollback("audit_entry", "INSERT"); }
    @Test void receiptWriteFailureRollsBackAuditAndAllSecurityEffects() throws Exception { rollback("command_receipt", "INSERT"); }
    @Test void refreshRevocationFailureRollsBackSessionAndGrantWrites() throws Exception { rollback("refresh_token", "UPDATE"); }
    @Test void concurrentVersionsProduceOneMutationAndOneStaleResult() throws Exception {
        var results = race(command(), command(S, T, List.of(GX), 1, id(50), command().reason()));
        assertEquals(1, results.stream().filter(Result.class::isInstance).count()); assertTrue(results.contains(PRECONDITION_STALE));
        assertEquals(2, scalar("SELECT version FROM identity.user_account WHERE id='" + T + "'"));
        assertEquals(1, scalar("SELECT count(*) FROM identity.audit_entry")); assertEquals(1, scalar("SELECT count(*) FROM identity.command_receipt"));
    }
    @Test void concurrentSameKeyCommandsCreateOneReceiptAndOneAudit() throws Exception {
        var results = race(command(), command()); assertTrue(results.stream().allMatch(Result.class::isInstance));
        assertEquals(1, results.stream().map(Result.class::cast).filter(Result::replayed).count());
        assertEquals(1, scalar("SELECT count(*) FROM identity.audit_entry")); assertEquals(1, scalar("SELECT count(*) FROM identity.command_receipt"));
    }
    @Test void concurrentOppositeAdminRemovalCannotRemoveLastAdministrator() throws Exception {
        var results = race(command(S, B, List.of(GX), 1, KEY, command().reason()), command(AS, A, List.of(GX), 1, id(51), command().reason()));
        assertEquals(1, results.stream().filter(Result.class::isInstance).count()); assertTrue(results.contains(SESSION_INVALID));
        assertEquals(1, scalar("SELECT count(DISTINCT a.id) FROM identity.user_account a JOIN identity.account_grant g ON g.account_id=a.id WHERE a.status='ACTIVE' AND g.role_code='IDENTITY_MANAGEMENT' AND g.scope_type='IDENTITY'"));
    }
    @Test void resultContainsOnlyAcceptedAccountFieldsAndRedactsToString() throws Exception {
        var result = service.replaceIdentityGrants(command());
        assertEquals(Set.of("id", "username", "displayName", "email", "status", "grants", "version", "emailVerified"),
            Arrays.stream(result.account().getClass().getRecordComponents()).map(java.lang.reflect.RecordComponent::getName).collect(java.util.stream.Collectors.toSet()));
        assertFalse(result.account().toString().contains("@")); assertFalse(command().toString().contains(command().reason()));
    }
}
