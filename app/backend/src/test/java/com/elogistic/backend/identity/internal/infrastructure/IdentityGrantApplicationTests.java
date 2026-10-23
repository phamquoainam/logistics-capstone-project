package com.elogistic.backend.identity.internal.infrastructure;

import com.elogistic.backend.identity.api.IdentityCommandFailure;
import com.elogistic.backend.identity.api.IdentityGrantCommands;
import com.elogistic.backend.identity.api.IdentityProvisionCommands;
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
import java.util.concurrent.atomic.AtomicInteger;
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
        assertEquals(5, db.flyway("latest", SCOPE).migrate().migrationsExecuted);
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
                    "INSERT INTO identity.account_grant(account_id,role_code,scope_type,scope_id,granted_at) VALUES (?,?,?,?,TIMESTAMPTZ '2026-10-21 00:00:00Z')")) {
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

    private IdentityGrantCommands production(GrantAuthority provider) {
        return ProductionIdentityGrantComposition.create(
            new DriverManagerDataSource(db.url(), db.runtime(), db.runtimePassword()),
            provider, Clock.fixed(NOW, ZoneOffset.UTC), Duration.ofHours(24));
    }
    private void productionDenied(IdentityGrantCommands app, IdentityCommandFailure.Code code, Command input) throws Exception {
        var before = snapshot();
        var failure = assertThrows(IdentityCommandFailure.class, () -> app.replaceIdentityGrants(input));
        assertEquals(code, failure.code()); assertEquals(code.name(), failure.getMessage());
        assertNull(failure.getCause()); assertEquals(before, snapshot());
    }
    private GrantAuthority countingDeniedProvider(AtomicInteger calls) {
        return (actor, target, requested) -> { calls.incrementAndGet(); return null; };
    }
    private void concurrentActorChange(String sql, IdentityCommandFailure.Code expected) throws Exception {
        AtomicInteger calls = new AtomicInteger(); var app = production(countingDeniedProvider(calls));
        ExecutorService pool = Executors.newSingleThreadExecutor(); CountDownLatch entered = new CountDownLatch(1);
        Future<IdentityCommandFailure.Code> waiting = null;
        try (Connection writer = db.runtimeConnection(); Statement s = writer.createStatement()) {
            writer.setAutoCommit(false);
            try {
                // Same already-approved Identity lock protocol, held by a real competing transaction.
                s.execute("SELECT pg_advisory_xact_lock(" + 0x4944454e54495459L + ")");
                s.executeUpdate(sql);
                waiting = pool.submit(() -> {
                    entered.countDown();
                    try { app.replaceIdentityGrants(command()); throw new AssertionError("Mutation must be denied"); }
                    catch (IdentityCommandFailure denied) { return denied.code(); }
                });
                assertTrue(entered.await(2, TimeUnit.SECONDS));
                long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(2);
                boolean blocked = false;
                while (System.nanoTime() < deadline) {
                    if (scalar("SELECT count(*) FROM pg_locks WHERE locktype='advisory' AND NOT granted AND database=(SELECT oid FROM pg_database WHERE datname=current_database())") > 0) {
                        blocked = true; break;
                    }
                    Thread.sleep(10);
                }
                assertTrue(blocked, "Competing command must wait for the Identity transaction lock");
                assertEquals(0, calls.get()); writer.commit();
            } finally { writer.rollback(); }
            var afterAuthorityChange = snapshot();
            assertEquals(expected, waiting.get(10, TimeUnit.SECONDS));
            assertEquals(0, calls.get()); assertEquals(afterAuthorityChange, snapshot());
            assertEquals(1, scalar("SELECT version FROM identity.user_account WHERE id='" + T + "'"));
            assertEquals(0, scalar("SELECT count(*) FROM identity.audit_entry"));
            assertEquals(0, scalar("SELECT count(*) FROM identity.command_receipt"));
        } finally {
            if (waiting != null && !waiting.isDone()) waiting.cancel(true);
            pool.shutdownNow(); assertTrue(pool.awaitTermination(5, TimeUnit.SECONDS));
        }
    }

    @Test void productionDefaultCompositionDeniesWithoutResourceContracts() throws Exception {
        var app = ProductionIdentityGrantComposition.create(
            new DriverManagerDataSource(db.url(), db.runtime(), db.runtimePassword()),
            Clock.fixed(NOW, ZoneOffset.UTC), Duration.ofHours(24));
        productionDenied(app, ACCOUNT_NOT_ACCESSIBLE, command());
    }
    @Test void productionProviderFailureIsSanitizedBeforeReceiptDisclosure() throws Exception {
        service.replaceIdentityGrants(command()); // Existing synthetic transaction fixture, not production owner proof.
        var app = production((actor, target, requested) -> { throw new IllegalStateException("synthetic-private-owner-diagnostic"); });
        productionDenied(app, ACCOUNT_NOT_ACCESSIBLE, command());
    }
    @Test void productionIncompleteOwnerContextCannotRemoveHiddenGrants() throws Exception {
        service.replaceIdentityGrants(command());
        var app = production((actor, target, requested) -> new ManagementAuthority(actor.id(), actor.version().authVersion(),
            target.id(), target.version().version(), Set.of(grant(GX)), Set.of(grant(GX))));
        productionDenied(app, ACCOUNT_NOT_ACCESSIBLE, command(S, T, List.of(GX), 2, id(70), command().reason()));
    }
    @Test void productionMissingProviderRejectsEvenAnEmptyTargetGrantSet() throws Exception {
        execute("DELETE FROM identity.account_grant WHERE account_id='" + T + "'");
        productionDenied(production(null), ACCOUNT_NOT_ACCESSIBLE, command());
    }
    @Test void productionDisabledActorIsRejectedBeforeAuthorityLookup() throws Exception {
        AtomicInteger calls = new AtomicInteger();
        execute("UPDATE identity.user_account SET status='DISABLED',disabled_at=TIMESTAMPTZ '2026-10-21 00:01:00Z' WHERE id='" + A + "'");
        productionDenied(production(countingDeniedProvider(calls)), SESSION_INVALID, command());
        assertEquals(0, calls.get());
    }
    @Test void productionRevokedOrExpiredSessionIsRejectedBeforeAuthorityLookup() throws Exception {
        AtomicInteger calls = new AtomicInteger(); var app = production(countingDeniedProvider(calls));
        execute("UPDATE identity.auth_session SET revoked_at=TIMESTAMPTZ '2026-10-21 00:01:00Z' WHERE id='" + S + "'");
        productionDenied(app, SESSION_INVALID, command());
        execute("UPDATE identity.auth_session SET revoked_at=NULL,idle_expires_at=TIMESTAMPTZ '2026-10-21 00:05:00Z' WHERE id='" + S + "'");
        productionDenied(app, SESSION_INVALID, command()); assertEquals(0, calls.get());
    }
    @Test void productionStaleAuthorizationVersionIsRejectedBeforeAuthorityLookup() throws Exception {
        AtomicInteger calls = new AtomicInteger();
        execute("UPDATE identity.user_account SET auth_version=2 WHERE id='" + A + "'");
        productionDenied(production(countingDeniedProvider(calls)), SESSION_INVALID, command()); assertEquals(0, calls.get());
    }
    @Test void productionSnapshotSubstitutionAndUnassignableScopeAreRejected() throws Exception {
        List<GrantAuthority> invalid = List.of(
            (actor, target, requested) -> new ManagementAuthority(B, 1, target.id(), target.version().version(), Set.of(grant(GX), grant(GY)), Set.of(grant(GX), grant(GY))),
            (actor, target, requested) -> new ManagementAuthority(actor.id(), 2, target.id(), target.version().version(), Set.of(grant(GX), grant(GY)), Set.of(grant(GX), grant(GY))),
            (actor, target, requested) -> new ManagementAuthority(actor.id(), 1, B, target.version().version(), Set.of(grant(GX), grant(GY)), Set.of(grant(GX), grant(GY))),
            (actor, target, requested) -> new ManagementAuthority(actor.id(), 1, target.id(), 2, Set.of(grant(GX), grant(GY)), Set.of(grant(GX), grant(GY))),
            (actor, target, requested) -> new ManagementAuthority(actor.id(), 1, target.id(), 1, Set.of(grant(GX), grant(GY)), Set.of(grant(GX))));
        for (GrantAuthority provider : invalid) productionDenied(production(provider), ACCOUNT_NOT_ACCESSIBLE, command());
    }
    @Test void productionSelfManagementPreservesSpecificDomainDenial() throws Exception {
        productionDenied(production(authority), SELF_MANAGEMENT_FORBIDDEN,
            command(S, A, List.of(ADMIN), 1, KEY, command().reason()));
    }
    @Test void productionConcurrentAuthorizationVersionChangeIsObservedAfterLock() throws Exception {
        concurrentActorChange("UPDATE identity.user_account SET auth_version=2 WHERE id='" + A + "'", SESSION_INVALID);
    }
    @Test void productionConcurrentManagementGrantLossIsObservedAfterLock() throws Exception {
        concurrentActorChange("DELETE FROM identity.account_grant WHERE account_id='" + A + "'", ACCOUNT_NOT_ACCESSIBLE);
    }

    @Test void productionEmptyTargetIsDeniedBeforeAnAllowingDelegate() throws Exception {
        execute("DELETE FROM identity.account_grant WHERE account_id='" + T + "'");
        AtomicInteger calls = new AtomicInteger();
        var app = production((actor, target, requested) -> {
            calls.incrementAndGet();
            return new ManagementAuthority(actor.id(), actor.version().authVersion(),
                target.id(), target.version().version(), requested, requested);
        });
        productionDenied(app, ACCOUNT_NOT_ACCESSIBLE, command());
        assertEquals(0, calls.get());
    }

    private IdentityGrantCommands realmProduction() {
        return ProductionIdentityGrantComposition.create(new DriverManagerDataSource(db.url(),db.runtime(),db.runtimePassword()),
            Clock.fixed(NOW,ZoneOffset.UTC),Duration.ofHours(24));
    }
    private Command realmCommand() { return command(S,T,List.of(ADMIN),1,KEY,"Synthetic Realm management"); }
    private void realmTarget() throws Exception {
        execute("DELETE FROM identity.account_grant WHERE account_id='"+T+"'");
        execute("INSERT INTO identity.account_grant(account_id,role_code,scope_type,scope_id,granted_at) VALUES ('"+T+"','IDENTITY_MANAGEMENT','IDENTITY','"+SCOPE+"',CURRENT_TIMESTAMP)");
    }
    @Test void realmProductionPerformsExactSameRealmReplacementAndAuthorizedReplay() throws Exception {
        realmTarget();db.activateRealmForTest(SCOPE);var app=realmProduction();
        var first=app.replaceIdentityGrants(realmCommand());assertFalse(first.replayed());assertEquals(2,first.appliedVersion());
        var before=snapshot();var replay=app.replaceIdentityGrants(realmCommand());assertTrue(replay.replayed());
        assertEquals(2,replay.appliedVersion());assertEquals(before,snapshot());
        assertEquals(1,scalar("SELECT count(*) FROM identity.audit_entry WHERE event_type='IDENTITY_GRANTS_REPLACED'"));
        assertEquals(0,scalar("SELECT count(*) FROM identity.auth_session WHERE account_id='"+T+"' AND revoked_at IS NULL"));
        assertEquals(0,scalar("SELECT count(*) FROM identity.refresh_token WHERE session_id='"+TS+"' AND revoked_at IS NULL"));
    }
    @Test void realmProductionRejectsInactiveAndMissingRegistryWithoutDisclosure() throws Exception {
        realmTarget();productionDenied(realmProduction(),ACCOUNT_NOT_ACCESSIBLE,realmCommand());
        db.ownerExecute("ALTER TABLE identity.realm RENAME TO synthetic_unavailable_realm");
        productionDenied(realmProduction(),ACCOUNT_NOT_ACCESSIBLE,realmCommand());
    }
    @Test void realmProductionRejectsExternalOrHiddenCurrentGrants() throws Exception {
        db.activateRealmForTest(SCOPE);productionDenied(realmProduction(),ACCOUNT_NOT_ACCESSIBLE,realmCommand());
        realmTarget();productionDenied(realmProduction(),ACCOUNT_NOT_ACCESSIBLE,command());
        execute("INSERT INTO identity.account_grant(account_id,role_code,scope_type,scope_id,granted_at) VALUES ('"+T+"','OPERATOR','WAREHOUSE','"+X+"',CURRENT_TIMESTAMP)");
        productionDenied(realmProduction(),ACCOUNT_NOT_ACCESSIBLE,realmCommand());
    }
    @Test void realmProductionPreservesSessionSelfAndVersionGuards() throws Exception {
        realmTarget();db.activateRealmForTest(SCOPE);var app=realmProduction();
        productionDenied(app,SELF_MANAGEMENT_FORBIDDEN,command(S,A,List.of(ADMIN),1,KEY,"Synthetic self denial"));
        productionDenied(app,PRECONDITION_STALE,command(S,T,List.of(ADMIN),2,KEY,"Synthetic stale denial"));
        execute("UPDATE identity.user_account SET auth_version=2 WHERE id='"+A+"'");
        productionDenied(app,SESSION_INVALID,realmCommand());
    }
    @Test void realmProductionRollsBackOnAuditFailure() throws Exception {
        realmTarget();db.activateRealmForTest(SCOPE);var before=snapshot();failWrite("audit_entry","INSERT");
        assertThrows(org.springframework.dao.DataAccessException.class,()->realmProduction().replaceIdentityGrants(realmCommand()));
        assertEquals(before,snapshot());
    }
    @Test void realmProductionRequiresSameTransactionAndAdministrationLock() throws Exception {
        realmTarget();db.activateRealmForTest(SCOPE);
        var ds=new DriverManagerDataSource(db.url(),db.runtime(),db.runtimePassword());
        var provider=new JdbcIdentityRealmGrantAuthority(ds);
        assertNull(provider.resolve(null,null,null)); // No transaction or JDBC query allowed.
        new org.springframework.transaction.support.TransactionTemplate(new org.springframework.jdbc.support.JdbcTransactionManager(ds))
            .execute(status->{ assertNull(provider.resolve(null,null,null));return null; }); // No administration lock.
    }
    @Test void realmDeactivationPrecedingAdmissionDeniesWaitingProductionCommand() throws Exception {
        realmTarget();db.activateRealmForTest(SCOPE);var app=realmProduction();
        ExecutorService pool=Executors.newSingleThreadExecutor();Future<IdentityCommandFailure.Code> waiting=null;
        try(Connection c=db.migrationConnection();Statement s=c.createStatement()) {
            s.execute("SET ROLE "+db.owner());c.setAutoCommit(false);
            s.execute("SELECT pg_advisory_xact_lock(5279420865908921433)");
            s.execute("UPDATE identity.realm SET status='INACTIVE',updated_at=CURRENT_TIMESTAMP");
            waiting=pool.submit(()->{try {app.replaceIdentityGrants(realmCommand());throw new AssertionError("Must deny after Realm deactivation");}
                catch(IdentityCommandFailure e){return e.code();}});
            boolean blocked=false;long deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(3);
            while(System.nanoTime()<deadline) {
                if(scalar("SELECT count(*) FROM pg_locks WHERE locktype='advisory' AND NOT granted AND database=(SELECT oid FROM pg_database WHERE datname=current_database())")>0){blocked=true;break;}
                Thread.sleep(10);
            }
            assertTrue(blocked);var before=snapshot();c.commit();
            assertEquals(ACCOUNT_NOT_ACCESSIBLE,waiting.get(10,TimeUnit.SECONDS));assertEquals(before,snapshot());
        } finally {
            if(waiting!=null && !waiting.isDone())waiting.cancel(true);
            pool.shutdownNow();assertTrue(pool.awaitTermination(5,TimeUnit.SECONDS));
        }
    }

    // Provisioning uses the REAL JDBC authority. Only the pending delivery adapter is synthetic:
    // its durable test table contains NO token/ciphertext and is NOT a production outbox schema.
    private void provisionFixture(boolean activate) throws Exception {
        db.ownerExecute("CREATE TABLE identity.synthetic_invitation_intent(proof_id UUID PRIMARY KEY REFERENCES identity.action_proof(id),account_id UUID NOT NULL REFERENCES identity.user_account(id),marker TEXT NOT NULL CHECK(marker='SYNTHETIC_NO_TOKEN')); GRANT SELECT,INSERT ON identity.synthetic_invitation_intent TO "+db.runtime());
        if(activate) db.activateRealmForTest(SCOPE);
    }
    private IdentityProvisionCommands.Command provisionCommand(String username,List<GrantData> grants,UUID key) {
        return new IdentityProvisionCommands.Command(S,username,"Synthetic Provision","provision@example.invalid",grants,key,REQUEST);
    }
    private IdentityProvisionCommands.Command provisionCommand() {return provisionCommand("fixture.provisioned",List.of(ADMIN),KEY);}
    private IdentityProvisionCommands provisionApp(AtomicInteger closed,boolean invalidProof) {
        return provisionApp(closed,invalidProof ? 1 : 0);
    }
    private IdentityProvisionCommands provisionApp(AtomicInteger closed,int invitationDefect) {
        var ds=new DriverManagerDataSource(db.url(),db.runtime(),db.runtimePassword());
        com.elogistic.backend.identity.internal.application.port.InvitationIssuer issuer=(pending,destination,now)->new com.elogistic.backend.identity.internal.application.port.InvitationIssuer.Prepared() {
            private final ActionProof proof=ActionProof.invitation(UUID.randomUUID(),pending,now);
            // Synthetic test-only digest, NOT evidence of production token generation/protection.
            private final byte[] hash=syntheticDigest();
            private byte[] syntheticDigest() {
                try {return java.security.MessageDigest.getInstance("SHA-256").digest(UUID.randomUUID().toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));}
                catch(java.security.NoSuchAlgorithmException e){throw new AssertionError(e);}
            }
            public ActionProof proof(){
                return switch(invitationDefect) {
                    case 2 -> new ActionProof(proof.id(),pending.id(),ActionProof.Purpose.RECOVERY,now,now.plus(Duration.ofMinutes(15)),ActionProof.State.AVAILABLE,null);
                    case 3 -> proof.consume(ActionProof.Purpose.INVITATION,pending.id(),now);
                    case 4 -> new ActionProof(proof.id(),pending.id(),ActionProof.Purpose.INVITATION,now,now.plus(Duration.ofHours(23)),ActionProof.State.AVAILABLE,null);
                    case 5 -> new ActionProof(proof.id(),id(99),ActionProof.Purpose.INVITATION,now,now.plus(Duration.ofHours(24)),ActionProof.State.AVAILABLE,null);
                    default -> proof;
                };
            }
            public byte[] tokenHash(){return invitationDefect==1 ? new byte[31] : hash.clone();}
            public void appendDeliveryIntent(){
                assertTrue(org.springframework.transaction.support.TransactionSynchronizationManager.hasResource(ds));
                new org.springframework.jdbc.core.JdbcTemplate(ds).update("INSERT INTO identity.synthetic_invitation_intent(proof_id,account_id,marker) VALUES (?,?,'SYNTHETIC_NO_TOKEN')",proof.id(),pending.id());
            }
            public void close(){Arrays.fill(hash,(byte)0);closed.incrementAndGet();}
            @Override public String toString(){return "Synthetic invitation fixture[redacted]";}
        };
        return ProductionIdentityProvisionComposition.create(ds,issuer,Clock.fixed(NOW,ZoneOffset.UTC),Duration.ofHours(24));
    }
    private Map<String,List<String>> provisionSnapshot() throws Exception {
        var rows=new TreeMap<>(snapshot());
        for(String table:List.of("action_proof","synthetic_invitation_intent")) {
            try(Connection c=db.runtimeConnection();Statement s=c.createStatement();ResultSet r=s.executeQuery("SELECT row_to_json(t)::text FROM identity."+table+" t ORDER BY row_to_json(t)::text")) {
                var values=new ArrayList<String>();while(r.next())values.add(r.getString(1));rows.put(table,values);
            }
        }
        return rows;
    }
    private void provisionDenied(IdentityProvisionCommands app,IdentityCommandFailure.Code code,IdentityProvisionCommands.Command command) throws Exception {
        var before=provisionSnapshot();var error=assertThrows(IdentityCommandFailure.class,()->app.provisionIdentityAccount(command));
        assertEquals(code,error.code());assertEquals(code.name(),error.getMessage());assertNull(error.getCause());assertEquals(before,provisionSnapshot());
    }
    private List<Object> provisionRace(IdentityProvisionCommands app,IdentityProvisionCommands.Command left,IdentityProvisionCommands.Command right) throws Exception {
        var pool=Executors.newFixedThreadPool(2);var ready=new CountDownLatch(2);var go=new CountDownLatch(1);
        try {
            var futures=new ArrayList<Future<Object>>();
            for(var input:List.of(left,right)) futures.add(pool.submit(()->{
                ready.countDown();if(!go.await(5,TimeUnit.SECONDS))throw new AssertionError("Start gate timed out");
                try{return app.provisionIdentityAccount(input);}catch(IdentityCommandFailure denied){return denied.code();}
            }));
            assertTrue(ready.await(5,TimeUnit.SECONDS));go.countDown();return List.of(futures.get(0).get(20,TimeUnit.SECONDS),futures.get(1).get(20,TimeUnit.SECONDS));
        } finally {go.countDown();pool.shutdownNow();assertTrue(pool.awaitTermination(5,TimeUnit.SECONDS));}
    }
    @Test void provisionPersistsPendingInitialPairProofAuditReceiptAndTestIntent() throws Exception {
        provisionFixture(true);var closed=new AtomicInteger();var result=provisionApp(closed,false).provisionIdentityAccount(provisionCommand());
        assertFalse(result.replayed());assertEquals(1,result.appliedVersion());assertEquals("PENDING_ACTIVATION",result.account().status());assertFalse(result.account().emailVerified());assertEquals(List.of(ADMIN),result.account().grants());
        UUID target=result.account().id();assertNotEquals(A,target);assertNotEquals(T,target);
        assertEquals(1,scalar("SELECT count(*) FROM identity.user_account WHERE id='"+target+"' AND version=1 AND auth_version=1 AND password_hash IS NULL AND activated_at IS NULL AND NOT email_verified"));
        assertEquals(1,scalar("SELECT count(*) FROM identity.account_grant WHERE account_id='"+target+"' AND identity_realm_id='"+SCOPE+"'"));
        assertEquals(1,scalar("SELECT count(*) FROM identity.action_proof WHERE account_id='"+target+"' AND purpose='INVITATION' AND expires_at=created_at+INTERVAL '24 hours' AND consumed_at IS NULL AND revoked_at IS NULL AND octet_length(token_hash)=32"));
        assertEquals(1,scalar("SELECT count(*) FROM identity.audit_entry WHERE actor_id='"+A+"' AND target_id='"+target+"' AND event_type='IDENTITY_ACCOUNT_PROVISIONED'"));
        assertEquals(1,scalar("SELECT count(*) FROM identity.command_receipt WHERE operation='provisionIdentityAccount' AND target_id='"+target+"' AND result_status=201 AND original_version IS NULL AND result_version=1 AND expires_at>=created_at+INTERVAL '24 hours'"));
        assertEquals(1,scalar("SELECT count(*) FROM identity.synthetic_invitation_intent WHERE account_id='"+target+"'"));assertEquals(1,closed.get());
    }
    @Test void provisionExactReplayDoesNotRepeatAccountProofAuditOrDelivery() throws Exception {
        provisionFixture(true);var closed=new AtomicInteger();var app=provisionApp(closed,false);var first=app.provisionIdentityAccount(provisionCommand());var before=provisionSnapshot();
        var replay=app.provisionIdentityAccount(provisionCommand());assertTrue(replay.replayed());assertEquals(first.account(),replay.account());assertEquals(before,provisionSnapshot());assertEquals(1,closed.get());
    }
    @Test void provisionConflictingPayloadKeyDoesNotDiscloseOrRepeatEffects() throws Exception {
        provisionFixture(true);var app=provisionApp(new AtomicInteger(),false);app.provisionIdentityAccount(provisionCommand());
        var c=provisionCommand();provisionDenied(app,IDEMPOTENCY_CONFLICT,new IdentityProvisionCommands.Command(S,c.username(),"Changed Contact",c.email(),c.grants(),KEY,REQUEST));
    }
    @Test void provisionDuplicateUsernameCannotTakeOverExistingAccount() throws Exception {
        provisionFixture(true);var app=provisionApp(new AtomicInteger(),false);app.provisionIdentityAccount(provisionCommand());
        provisionDenied(app,USERNAME_CONFLICT,provisionCommand("fixture.provisioned",List.of(ADMIN),id(90)));
    }
    @Test void provisionUnauthenticatedAndUnknownSessionsMakeNoWrites() throws Exception {
        provisionFixture(true);var app=provisionApp(new AtomicInteger(),false);provisionDenied(app,SESSION_INVALID,null);
        var c=provisionCommand();provisionDenied(app,SESSION_INVALID,new IdentityProvisionCommands.Command(id(99),c.username(),c.displayName(),c.email(),c.grants(),KEY,REQUEST));
    }
    @Test void provisionRevokedSessionCannotUseCurrentAdministrativeGrant() throws Exception {
        provisionFixture(true);execute("UPDATE identity.auth_session SET revoked_at=TIMESTAMPTZ '2026-10-21 00:01:00Z' WHERE id='"+S+"'");
        provisionDenied(provisionApp(new AtomicInteger(),false),SESSION_INVALID,provisionCommand());
    }
    @Test void provisionExpiredSessionFailsBeforeContactLookup() throws Exception {
        provisionFixture(true);execute("UPDATE identity.auth_session SET idle_expires_at=TIMESTAMPTZ '2026-10-21 00:05:00Z' WHERE id='"+S+"'");
        provisionDenied(provisionApp(new AtomicInteger(),false),SESSION_INVALID,provisionCommand());
    }
    @Test void provisionDisabledActorAndStaleAuthVersionAreRejected() throws Exception {
        provisionFixture(true);var app=provisionApp(new AtomicInteger(),false);
        execute("UPDATE identity.user_account SET auth_version=2 WHERE id='"+A+"'");provisionDenied(app,SESSION_INVALID,provisionCommand());
        execute("UPDATE identity.user_account SET auth_version=1,status='DISABLED',disabled_at=TIMESTAMPTZ '2026-10-21 00:01:00Z' WHERE id='"+A+"'");provisionDenied(app,SESSION_INVALID,provisionCommand());
    }
    @Test void provisionInactiveUnknownAndCrossRealmAuthorityFailClosed() throws Exception {
        provisionFixture(false);var app=provisionApp(new AtomicInteger(),false);provisionDenied(app,AUTHORITY_REQUIRED,provisionCommand());
        db.activateRealmForTest(SCOPE);provisionDenied(app,AUTHORITY_REQUIRED,provisionCommand("fixture.provisioned",List.of(new GrantData("IDENTITY_MANAGEMENT","IDENTITY",X)),KEY));
        db.ownerExecute("ALTER TABLE identity.realm RENAME TO synthetic_unavailable_realm");provisionDenied(app,AUTHORITY_REQUIRED,provisionCommand());
    }
    @Test void provisionExternalMixedAndOrdinaryRoleRequestsNeverAssignGrants() throws Exception {
        provisionFixture(true);var app=provisionApp(new AtomicInteger(),false);
        for(var grants:List.of(List.of(GX),List.of(ADMIN,GX),List.of(new GrantData("FINANCE","IDENTITY",SCOPE))))
            provisionDenied(app,AUTHORITY_REQUIRED,provisionCommand("fixture.provisioned",grants,KEY));
        execute("DELETE FROM identity.account_grant WHERE account_id='"+A+"'");provisionDenied(app,AUTHORITY_REQUIRED,provisionCommand());
    }
    @Test void provisionMissingDurableInvitationAdapterFailsBeforeWrites() throws Exception {
        provisionFixture(true);var app=ProductionIdentityProvisionComposition.create(new DriverManagerDataSource(db.url(),db.runtime(),db.runtimePassword()),Clock.fixed(NOW,ZoneOffset.UTC),Duration.ofHours(24));
        var before=provisionSnapshot();var error=assertThrows(IllegalStateException.class,()->app.provisionIdentityAccount(provisionCommand()));
        assertEquals("PROVISIONING_INVITATION_ADAPTER_REQUIRED",error.getMessage());assertEquals(before,provisionSnapshot());
    }
    @Test void provisionInvalidPreparedProofClosesMaterialWithoutPartialAccount() throws Exception {
        provisionFixture(true);var before=provisionSnapshot();
        for(int defect:List.of(1,2,3,4,5)) {
            var closed=new AtomicInteger();
            assertThrows(IllegalStateException.class,()->provisionApp(closed,defect).provisionIdentityAccount(provisionCommand()));
            assertEquals(before,provisionSnapshot());assertEquals(1,closed.get());
        }
    }
    @Test void provisionGrantProofDeliveryAuditAndReceiptFaultsRollBackAtomically() throws Exception {
        for(String table:List.of("account_grant","action_proof","synthetic_invitation_intent","audit_entry","command_receipt")) {
            fixture();provisionFixture(true);failWrite(table,"INSERT");var before=provisionSnapshot();var closed=new AtomicInteger();
            assertThrows(org.springframework.dao.DataAccessException.class,()->provisionApp(closed,false).provisionIdentityAccount(provisionCommand()));
            assertEquals(before,provisionSnapshot(),table);assertEquals(1,closed.get());
        }
    }
    @Test void provisionConcurrentSameKeyHasOneAccountProofAndTestIntent() throws Exception {
        provisionFixture(true);var closed=new AtomicInteger();var results=provisionRace(provisionApp(closed,false),provisionCommand(),provisionCommand());
        assertTrue(results.stream().allMatch(Result.class::isInstance));
        assertEquals(1,results.stream().map(Result.class::cast).filter(Result::replayed).count());assertEquals(1,closed.get());
        assertEquals(1,scalar("SELECT count(*) FROM identity.user_account WHERE username='fixture.provisioned'"));assertEquals(1,scalar("SELECT count(*) FROM identity.synthetic_invitation_intent"));
        assertEquals(1,scalar("SELECT count(*) FROM identity.command_receipt WHERE operation='provisionIdentityAccount'"));
    }
    @Test void provisionConcurrentUsernameConflictHasNoLoserEffects() throws Exception {
        provisionFixture(true);var closed=new AtomicInteger();var results=provisionRace(provisionApp(closed,false),provisionCommand(),provisionCommand("fixture.provisioned",List.of(ADMIN),id(90)));
        assertEquals(1,results.stream().filter(Result.class::isInstance).count());assertEquals(1,results.stream().filter(USERNAME_CONFLICT::equals).count());assertEquals(1,closed.get());
        assertEquals(1,scalar("SELECT count(*) FROM identity.synthetic_invitation_intent"));assertEquals(1,scalar("SELECT count(*) FROM identity.command_receipt WHERE operation='provisionIdentityAccount'"));
    }
    @Test void provisionReplayRechecksStoredTargetGrantUnionAndCurrentSession() throws Exception {
        provisionFixture(true);var app=provisionApp(new AtomicInteger(),false);UUID target=app.provisionIdentityAccount(provisionCommand()).account().id();
        execute("INSERT INTO identity.account_grant(account_id,role_code,scope_type,scope_id,granted_at) VALUES ('"+target+"','OPERATOR','WAREHOUSE','"+X+"',CURRENT_TIMESTAMP)");
        provisionDenied(app,AUTHORITY_REQUIRED,provisionCommand());execute("UPDATE identity.user_account SET auth_version=2 WHERE id='"+A+"'");provisionDenied(app,SESSION_INVALID,provisionCommand());
    }
    @Test void provisionCannotActivateRealmAccountOrIssueSessions() throws Exception {
        provisionFixture(true);String before;
        try(Connection c=db.runtimeConnection();Statement s=c.createStatement();ResultSet r=s.executeQuery("SELECT row_to_json(t)::text FROM identity.realm t")){assertTrue(r.next());before=r.getString(1);}
        UUID target=provisionApp(new AtomicInteger(),false).provisionIdentityAccount(provisionCommand()).account().id();
        try(Connection c=db.runtimeConnection();Statement s=c.createStatement();ResultSet r=s.executeQuery("SELECT row_to_json(t)::text FROM identity.realm t")){assertTrue(r.next());assertEquals(before,r.getString(1));}
        assertEquals(0,scalar("SELECT count(*) FROM identity.auth_session WHERE account_id='"+target+"'"));
        assertEquals(1,scalar("SELECT count(*) FROM identity.user_account WHERE id='"+target+"' AND status='PENDING_ACTIVATION' AND NOT email_verified AND password_hash IS NULL"));
    }
    /** Expected state includes the writer's deliberate uncommitted authority change, while
     * the administration lock still prevents provisioning effects from contaminating the oracle. */
    private Map<String,List<String>> provisionAdmissionSnapshot(Connection connection) throws SQLException {
        var tables=new TreeSet<>(TABLES);
        tables.addAll(List.of("action_proof","synthetic_invitation_intent","invitation_outbox","realm"));
        var rows=new TreeMap<String,List<String>>();
        for(String table:tables) {
            try(Statement statement=connection.createStatement();ResultSet result=statement.executeQuery(
                    "SELECT row_to_json(t)::text FROM identity."+table+" t ORDER BY row_to_json(t)::text")) {
                var values=new ArrayList<String>();while(result.next())values.add(result.getString(1));rows.put(table,values);
            }
        }
        return rows;
    }
    @Test void provisionWaitsForConcurrentActorAndRealmStateBeforeAdmission() throws Exception {
        for(boolean deactivateRealm:List.of(false,true)) {
            fixture();provisionFixture(true);var app=provisionApp(new AtomicInteger(),false);var pool=Executors.newSingleThreadExecutor();Future<IdentityCommandFailure.Code> waiting=null;
            try(Connection c=db.migrationConnection();Statement s=c.createStatement()) {
                s.execute("SET ROLE "+db.owner());c.setAutoCommit(false);s.execute("SELECT pg_advisory_xact_lock(5279420865908921433)");
                s.execute(deactivateRealm ? "UPDATE identity.realm SET status='INACTIVE',updated_at=CURRENT_TIMESTAMP" : "UPDATE identity.user_account SET auth_version=2 WHERE id='"+A+"'");
                waiting=pool.submit(()->{try {app.provisionIdentityAccount(provisionCommand());throw new AssertionError("Must deny after current-state change");}catch(IdentityCommandFailure denied){return denied.code();}});
                boolean blocked=false;long deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(3);
                while(System.nanoTime()<deadline) {if(scalar("SELECT count(*) FROM pg_locks WHERE locktype='advisory' AND NOT granted AND database=(SELECT oid FROM pg_database WHERE datname=current_database())")>0){blocked=true;break;}Thread.sleep(10);}
                assertTrue(blocked);assertFalse(waiting.isDone());
                var expected=provisionAdmissionSnapshot(c); // Read our own intentional actor/Realm delta, not stale committed data.
                c.commit();
                assertEquals(deactivateRealm ? AUTHORITY_REQUIRED : SESSION_INVALID,waiting.get(10,TimeUnit.SECONDS));
                try(Connection observed=db.runtimeConnection()) {assertEquals(expected,provisionAdmissionSnapshot(observed));}
            } finally {if(waiting!=null && !waiting.isDone())waiting.cancel(true);pool.shutdownNow();assertTrue(pool.awaitTermination(5,TimeUnit.SECONDS));}
        }
    }
    // Real secure issuer/outbox tests: no synthetic delivery table/issuer substitutes here.
    private static byte[] outboxKey(){byte[] value=new byte[32];new java.security.SecureRandom().nextBytes(value);return value;}
    private static InvitationProtection outboxProtection(){return new InvitationProtection(Map.of("v1",outboxKey()),"v1",Map.of("e1",outboxKey()),"e1");}
    private DriverManagerDataSource outboxDataSource(){return new DriverManagerDataSource(db.url(),db.runtime(),db.runtimePassword());}
    private static JdbcInvitationOutbox.Policy outboxPolicy(){return new JdbcInvitationOutbox.Policy(2,Duration.ofSeconds(5),Duration.ofSeconds(1));}
    private JdbcInvitationOutbox outbox(InvitationProtection protection,Instant time){return new JdbcInvitationOutbox(outboxDataSource(),protection,outboxPolicy(),Clock.fixed(time,ZoneOffset.UTC));}
    private IdentityProvisionCommands secureProvision(InvitationProtection protection){return ProductionIdentityProvisionComposition.secure(outboxDataSource(),protection,outboxPolicy(),Clock.fixed(NOW,ZoneOffset.UTC),Duration.ofHours(24));}
    private Map<String,List<String>> secureSnapshot() throws Exception {
        var rows=new TreeMap<>(snapshot());
        for(String table:List.of("action_proof","invitation_outbox")) {
            try(Connection c=db.runtimeConnection();Statement s=c.createStatement();ResultSet r=s.executeQuery("SELECT row_to_json(t)::text FROM identity."+table+" t ORDER BY id")) {
                var values=new ArrayList<String>();while(r.next())values.add(r.getString(1));rows.put(table,values);
            }
        }
        return rows;
    }
    private void assertPending(UUID account) throws Exception {
        assertEquals(1,scalar("SELECT count(*) FROM identity.user_account WHERE id='"+account+"' AND status='PENDING_ACTIVATION' AND NOT email_verified AND password_hash IS NULL AND activated_at IS NULL AND version=1 AND auth_version=1"));
        assertEquals(0,scalar("SELECT count(*) FROM identity.auth_session WHERE account_id='"+account+"'"));
        assertEquals(1,scalar("SELECT count(*) FROM identity.action_proof WHERE account_id='"+account+"' AND consumed_at IS NULL AND revoked_at IS NULL"));
    }
    @Test void secureProvisionQueuesEncryptedIntentWithMatchingHmac() throws Exception {
        db.activateRealmForTest(SCOPE);var protection=outboxProtection();UUID target=secureProvision(protection).provisionIdentityAccount(provisionCommand()).account().id();
        var worker=outbox(protection,NOW);var lease=worker.claim().orElseThrow();
        try(var delivery=worker.open(lease);Connection c=db.runtimeConnection();PreparedStatement s=c.prepareStatement("SELECT p.id,p.created_at,p.expires_at,p.token_hash,p.verifier_key_id,o.nonce,o.ciphertext FROM identity.action_proof p JOIN identity.invitation_outbox o ON o.proof_id=p.id WHERE p.account_id=?")) {
            s.setObject(1,target);try(ResultSet r=s.executeQuery()) {
                assertTrue(r.next());var proof=new ActionProof(r.getObject(1,UUID.class),target,ActionProof.Purpose.INVITATION,r.getTimestamp(2).toInstant(),r.getTimestamp(3).toInstant(),ActionProof.State.AVAILABLE,null);
                assertTrue(protection.matches(proof,r.getString(5),delivery.token(),r.getBytes(4),NOW));
                assertEquals(12,r.getBytes(6).length);assertEquals(59,r.getBytes(7).length);assertEquals(43,delivery.token().length);
                assertFalse(Arrays.equals(delivery.token(),r.getBytes(4)));assertFalse(Arrays.equals(delivery.token(),r.getBytes(7)));
                // Exact raw material must not occur in ANY persisted row/audit/receipt snapshot.
                String marker=new String(delivery.token(),java.nio.charset.StandardCharsets.US_ASCII);
                assertFalse(secureSnapshot().toString().contains(marker));assertTrue(delivery.destination().equals("provision@example.invalid"));
                assertFalse(delivery.toString().contains(marker));assertFalse(delivery.toString().contains(delivery.destination()));
            }
        }
        assertPending(target);assertEquals(1,scalar("SELECT count(*) FROM identity.account_grant WHERE account_id='"+target+"'"));
        assertEquals(1,scalar("SELECT count(*) FROM identity.audit_entry WHERE target_id='"+target+"' AND event_type='IDENTITY_ACCOUNT_PROVISIONED'"));
        assertEquals(1,scalar("SELECT count(*) FROM identity.command_receipt WHERE target_id='"+target+"' AND operation='provisionIdentityAccount'"));
    }
    @Test void secureProvisionReplayAndConflictingKeyNeverRegenerateOutbox() throws Exception {
        db.activateRealmForTest(SCOPE);var app=secureProvision(outboxProtection());var result=app.provisionIdentityAccount(provisionCommand());var before=secureSnapshot();
        assertTrue(app.provisionIdentityAccount(provisionCommand()).replayed());assertEquals(before,secureSnapshot());
        var error=assertThrows(IdentityCommandFailure.class,()->app.provisionIdentityAccount(provisionCommand("different.fixture",List.of(ADMIN),KEY)));
        assertEquals(IDEMPOTENCY_CONFLICT,error.code());assertEquals(before,secureSnapshot());assertPending(result.account().id());
    }
    @Test void secureProvisionEveryMandatoryPersistenceFailureRollsBack() throws Exception {
        for(String table:List.of("account_grant","action_proof","invitation_outbox","audit_entry","command_receipt")) {
            fixture();db.activateRealmForTest(SCOPE);var app=secureProvision(outboxProtection());var before=secureSnapshot();failWrite(table,"INSERT");
            assertThrows(org.springframework.dao.DataAccessException.class,()->app.provisionIdentityAccount(provisionCommand()));assertEquals(before,secureSnapshot());
        }
    }
    @Test void secureProvisionConcurrentKeyAndUsernameHaveOneDurableEffect() throws Exception {
        for(boolean sameKey:List.of(true,false)) {
            fixture();db.activateRealmForTest(SCOPE);var app=secureProvision(outboxProtection());var result=provisionRace(app,provisionCommand(),sameKey?provisionCommand():provisionCommand("fixture.provisioned",List.of(ADMIN),id(90)));
            assertEquals(sameKey?2:1,result.stream().filter(IdentityGrantCommands.Result.class::isInstance).count());
            assertEquals(sameKey?0:1,result.stream().filter(USERNAME_CONFLICT::equals).count());
            assertEquals(1,scalar("SELECT count(*) FROM identity.invitation_outbox"));assertEquals(1,scalar("SELECT count(*) FROM identity.command_receipt WHERE operation='provisionIdentityAccount'"));
            assertEquals(1,scalar("SELECT count(*) FROM identity.user_account WHERE username='fixture.provisioned'"));
        }
    }
    @Test void secureOutboxClaimLeaseRecoveryRejectsStaleOwner() throws Exception {
        db.activateRealmForTest(SCOPE);var p=outboxProtection();secureProvision(p).provisionIdentityAccount(provisionCommand());var worker=outbox(p,NOW);var first=worker.claim().orElseThrow();
        assertTrue(worker.claim().isEmpty());var later=outbox(p,NOW.plusSeconds(6));var second=later.claim().orElseThrow();
        assertEquals(first.messageId(),second.messageId());assertNotEquals(first.leaseId(),second.leaseId());
        assertThrows(IllegalStateException.class,()->later.open(first));assertThrows(IllegalStateException.class,()->later.complete(first,JdbcInvitationOutbox.Outcome.SENT));
        try(var delivered=later.open(second)){assertEquals(43,delivered.token().length);}
    }
    @Test void secureOutboxConcurrentClaimsHaveExactlyOneLease() throws Exception {
        db.activateRealmForTest(SCOPE);var p=outboxProtection();secureProvision(p).provisionIdentityAccount(provisionCommand());var pool=Executors.newFixedThreadPool(2);
        var ready=new CountDownLatch(2);var go=new CountDownLatch(1);
        try {
            Callable<Optional<JdbcInvitationOutbox.Lease>> claim=()->{ready.countDown();if(!go.await(5,TimeUnit.SECONDS))throw new AssertionError("Claim gate timed out");return outbox(p,NOW).claim();};
            var first=pool.submit(claim);var second=pool.submit(claim);assertTrue(ready.await(5,TimeUnit.SECONDS));go.countDown();
            assertEquals(1,List.of(first.get(20,TimeUnit.SECONDS),second.get(20,TimeUnit.SECONDS)).stream().filter(Optional::isPresent).count());
            assertEquals(1,scalar("SELECT attempts FROM identity.invitation_outbox"));
        }finally {go.countDown();pool.shutdownNow();assertTrue(pool.awaitTermination(5,TimeUnit.SECONDS));}
    }
    @Test void secureOutboxSentClearsPayloadWithoutConsumingOrActivating() throws Exception {
        db.activateRealmForTest(SCOPE);var p=outboxProtection();UUID target=secureProvision(p).provisionIdentityAccount(provisionCommand()).account().id();var worker=outbox(p,NOW);var lease=worker.claim().orElseThrow();byte[] material;
        try(var delivery=worker.open(lease)){material=delivery.token();assertEquals(43,material.length);}
        assertTrue(Arrays.equals(new byte[43],material));worker.complete(lease,JdbcInvitationOutbox.Outcome.SENT);
        assertEquals(1,scalar("SELECT count(*) FROM identity.invitation_outbox WHERE status='SENT' AND nonce IS NULL AND ciphertext IS NULL AND lease_id IS NULL"));
        assertThrows(IllegalStateException.class,()->worker.open(lease));assertThrows(IllegalStateException.class,()->worker.complete(lease,JdbcInvitationOutbox.Outcome.SENT));assertPending(target);
    }
    @Test void secureOutboxTransientRetriesAreBoundedAndExhaustionPurges() throws Exception {
        db.activateRealmForTest(SCOPE);var p=outboxProtection();secureProvision(p).provisionIdentityAccount(provisionCommand());var worker=outbox(p,NOW);
        worker.complete(worker.claim().orElseThrow(),JdbcInvitationOutbox.Outcome.TRANSIENT_FAILURE);assertTrue(worker.claim().isEmpty());
        var later=outbox(p,NOW.plusSeconds(1));later.complete(later.claim().orElseThrow(),JdbcInvitationOutbox.Outcome.TRANSIENT_FAILURE);
        assertTrue(later.claim().isEmpty());assertEquals(1,scalar("SELECT count(*) FROM identity.invitation_outbox WHERE status='FAILED' AND attempts=2 AND ciphertext IS NULL AND nonce IS NULL"));
    }
    @Test void secureOutboxPermanentFailurePurgesWithoutBusinessMutation() throws Exception {
        db.activateRealmForTest(SCOPE);var p=outboxProtection();UUID target=secureProvision(p).provisionIdentityAccount(provisionCommand()).account().id();var worker=outbox(p,NOW);
        worker.complete(worker.claim().orElseThrow(),JdbcInvitationOutbox.Outcome.PERMANENT_FAILURE);
        assertEquals(1,scalar("SELECT count(*) FROM identity.invitation_outbox WHERE status='FAILED' AND ciphertext IS NULL AND nonce IS NULL AND last_outcome='PERMANENT_FAILURE'"));assertPending(target);
    }
    @Test void secureOutboxExpiryRevocationAndConsumptionPurgeOnlyPayloads() throws Exception {
        for(String mode:List.of("expiry","revoked_at","consumed_at")) {
            fixture();db.activateRealmForTest(SCOPE);var p=outboxProtection();UUID target=secureProvision(p).provisionIdentityAccount(provisionCommand()).account().id();
            if(!mode.equals("expiry"))execute("UPDATE identity.action_proof SET "+mode+"=created_at");
            var worker=outbox(p,mode.equals("expiry")?NOW.plusSeconds(86400):NOW);assertEquals(1,worker.purge());assertEquals(0,worker.purge());assertTrue(worker.claim().isEmpty());
            assertEquals(1,scalar("SELECT count(*) FROM identity.invitation_outbox WHERE status='EXPIRED' AND nonce IS NULL AND ciphertext IS NULL"));
            assertEquals(1,scalar("SELECT count(*) FROM identity.user_account WHERE id='"+target+"' AND status='PENDING_ACTIVATION'"));
            assertEquals(1,scalar("SELECT count(*) FROM identity.command_receipt WHERE target_id='"+target+"'"));
        }
    }
    @Test void secureOutboxCurrentRealmAndGrantStateGateDelivery() throws Exception {
        for(boolean revokeGrant:List.of(false,true)) {
            fixture();db.activateRealmForTest(SCOPE);var p=outboxProtection();UUID target=secureProvision(p).provisionIdentityAccount(provisionCommand()).account().id();var worker=outbox(p,NOW);var lease=worker.claim().orElseThrow();
            if(revokeGrant)execute("DELETE FROM identity.account_grant WHERE account_id='"+target+"'");
            else db.ownerExecute("UPDATE identity.realm SET status='INACTIVE',updated_at=CURRENT_TIMESTAMP");
            assertThrows(IllegalStateException.class,()->worker.open(lease));assertEquals(1,worker.purge());assertTrue(worker.claim().isEmpty());
        }
    }
    @Test void secureOutboxTamperingCannotProducePlaintextDelivery() throws Exception {
        for(boolean contact:List.of(false,true)) {
            fixture();db.activateRealmForTest(SCOPE);var p=outboxProtection();UUID target=secureProvision(p).provisionIdentityAccount(provisionCommand()).account().id();var worker=outbox(p,NOW);var lease=worker.claim().orElseThrow();
            if(contact){execute("UPDATE identity.user_account SET email='changed@example.invalid' WHERE id='"+target+"'");execute("UPDATE identity.invitation_outbox SET destination='changed@example.invalid'");}
            else execute("UPDATE identity.invitation_outbox SET ciphertext=decode(repeat('00',59),'hex')");
            var error=assertThrows(IllegalStateException.class,()->worker.open(lease));assertEquals("INVITATION_PROTECTION_REJECTED",error.getMessage());assertNull(error.getCause());
        }
    }
    @Test void secureOutboxRotationReadsPersistedOldKeyIdentity() throws Exception {
        db.activateRealmForTest(SCOPE);byte[] v=outboxKey(),e=outboxKey();var p=new InvitationProtection(Map.of("v1",v),"v1",Map.of("e1",e),"e1");
        secureProvision(p).provisionIdentityAccount(provisionCommand());var lease=outbox(p,NOW).claim().orElseThrow();
        var rotated=new InvitationProtection(Map.of("v1",v,"v2",outboxKey()),"v2",Map.of("e1",e,"e2",outboxKey()),"e2");
        try(var delivery=outbox(rotated,NOW).open(lease)){assertEquals(43,delivery.token().length);}
        var unavailable=new InvitationProtection(Map.of("v2",outboxKey()),"v2",Map.of("e2",outboxKey()),"e2");
        assertThrows(IllegalStateException.class,()->outbox(unavailable,NOW).open(lease));InvitationProtection.wipe(v);InvitationProtection.wipe(e);
    }
    @Test void secureOutboxMissingKeysOrRuntimeInsertPrivilegeCannotCommit() throws Exception {
        db.activateRealmForTest(SCOPE);var before=secureSnapshot();
        var missing=assertThrows(IllegalStateException.class,()->ProductionIdentityProvisionComposition.secure(outboxDataSource(),null,outboxPolicy(),Clock.fixed(NOW,ZoneOffset.UTC),Duration.ofHours(24)));
        assertEquals("PROVISIONING_INVITATION_KEYS_REQUIRED",missing.getMessage());
        assertEquals(before,secureSnapshot());var app=secureProvision(outboxProtection());db.ownerExecute("REVOKE INSERT ON identity.invitation_outbox FROM "+db.runtime());
        assertThrows(org.springframework.dao.DataAccessException.class,()->app.provisionIdentityAccount(provisionCommand()));assertEquals(before,secureSnapshot());
    }
    @Test void secureOutboxRejectsIntentOutsideTheProvisionTransaction() throws Exception {
        var p=outboxProtection();var issuer=new SecureInvitationIssuer(p,outbox(p,NOW));
        var pending=new IdentityAccount(id(99),new Username("fixture.pending"),IdentityAccount.Status.PENDING_ACTIVATION,IdentityAccount.Activation.UNVERIFIED,new AccountVersion(1,1),Set.of(grant(ADMIN)));
        try(var prepared=issuer.prepare(pending,"fixture@example.invalid",NOW)) {
            var error=assertThrows(IllegalStateException.class,prepared::appendDeliveryIntent);assertEquals("INVITATION_SAME_TRANSACTION_REQUIRED",error.getMessage());
        }
        assertEquals(0,scalar("SELECT count(*) FROM identity.invitation_outbox"));
    }

}
