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
        assertEquals(8, db.flyway("latest", SCOPE).migrate().migrationsExecuted);
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


    // Activation tests reuse this suite's three-suite owned Testcontainers lifecycle unchanged.
    private static final String ACTIVATION_PASSWORD="Synthetic-only-activation-password";
    private record ActivationFixture(UUID account,UUID proof,byte[] token) implements AutoCloseable {
        @Override public void close(){Arrays.fill(token,(byte)0);}
        @Override public String toString(){return "ActivationFixture[redacted]";}
    }
    private ActivationFixture activationFixture(InvitationProtection protection) throws Exception {
        db.activateRealmForTest(SCOPE);
        UUID target=secureProvision(protection).provisionIdentityAccount(provisionCommand()).account().id();
        var worker=outbox(protection,NOW);var lease=worker.claim().orElseThrow();
        // Synthetic trusted-boundary input, not an assertion of external email delivery.
        try(var delivery=worker.open(lease);Connection c=db.runtimeConnection();PreparedStatement q=c.prepareStatement("SELECT id FROM identity.action_proof WHERE account_id=? AND purpose='INVITATION'")) {
            q.setObject(1,target);try(ResultSet r=q.executeQuery()){assertTrue(r.next());return new ActivationFixture(target,r.getObject(1,UUID.class),delivery.token().clone());}
        }
    }
    private com.elogistic.backend.identity.api.IdentityInvitationCommands activationApp(InvitationProtection protection,Instant now) {
        return ProductionIdentityActivationComposition.create(outboxDataSource(),protection,Clock.fixed(now,ZoneOffset.UTC),Duration.ofHours(24));
    }
    private void accept(ActivationFixture f,InvitationProtection p,UUID key,String password,Instant now) {
        try(var command=new com.elogistic.backend.identity.api.IdentityInvitationCommands.Command(f.proof(),f.token(),password,key,REQUEST)) {
            activationApp(p,now).acceptIdentityInvitation(command);
        }
    }
    private void activationRejected(IdentityCommandFailure.Code code,ActivationFixture f,InvitationProtection p,UUID key,String password,Instant now) throws Exception {
        var before=secureSnapshot();var error=assertThrows(IdentityCommandFailure.class,()->accept(f,p,key,password,now));
        assertEquals(code,error.code());assertEquals(code.name(),error.getMessage());assertEquals(before,secureSnapshot());
    }
    private void activationRollback(String table,String event) throws Exception {
        var p=outboxProtection();try(var f=activationFixture(p)) {
            failWrite(table,event);var before=secureSnapshot();assertThrows(org.springframework.dao.DataAccessException.class,()->accept(f,p,id(201),ACTIVATION_PASSWORD,NOW));assertEquals(before,secureSnapshot());
        }
    }
    @Test void activationCommitsVerifierProofVersionsAuditReceiptWithoutSession() throws Exception {
        var p=outboxProtection();try(var f=activationFixture(p)) {
            accept(f,p,id(201),ACTIVATION_PASSWORD,NOW);
            assertEquals(1,scalar("SELECT count(*) FROM identity.user_account WHERE id='"+f.account()+"' AND status='ACTIVE' AND email_verified AND activated_at IS NOT NULL AND version=2 AND auth_version=2"));
            try(Connection c=db.runtimeConnection();PreparedStatement q=c.prepareStatement("SELECT password_hash FROM identity.user_account WHERE id=?")) {
                q.setObject(1,f.account());try(ResultSet r=q.executeQuery()){assertTrue(r.next());assertTrue(new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder().matches(ACTIVATION_PASSWORD,r.getString(1)));assertFalse(r.getString(1).equals(ACTIVATION_PASSWORD));}
            }
            assertEquals(1,scalar("SELECT count(*) FROM identity.action_proof WHERE id='"+f.proof()+"' AND consumed_at IS NOT NULL"));
            assertEquals(1,scalar("SELECT count(*) FROM identity.audit_entry WHERE target_id='"+f.account()+"' AND event_type='IDENTITY_INVITATION_ACCEPTED'"));
            assertEquals(1,scalar("SELECT count(*) FROM identity.command_receipt WHERE proof_id='"+f.proof()+"' AND result_status=204 AND original_version=1 AND result_version=2 AND expires_at>=created_at+INTERVAL '24 hours'"));
            assertEquals(0,scalar("SELECT count(*) FROM identity.auth_session WHERE account_id='"+f.account()+"'"));
        }
    }
    @Test void activationExactReceiptReplayHasNoSecondEffect() throws Exception {
        var p=outboxProtection();try(var f=activationFixture(p)){accept(f,p,id(201),ACTIVATION_PASSWORD,NOW);var before=secureSnapshot();accept(f,p,id(201),ACTIVATION_PASSWORD,NOW.plusSeconds(1));assertEquals(before,secureSnapshot());}
    }
    @Test void activationChangedPasswordConflictsOnTheBoundReceipt() throws Exception {
        var p=outboxProtection();try(var f=activationFixture(p)){accept(f,p,id(201),ACTIVATION_PASSWORD,NOW);activationRejected(IDEMPOTENCY_CONFLICT,f,p,id(201),ACTIVATION_PASSWORD+"-changed",NOW);}
    }
    @Test void activationWrongTokenAndUnknownLocatorCannotAuthenticate() throws Exception {
        var p=outboxProtection();try(var f=activationFixture(p);var wrong=new ActivationFixture(f.account(),f.proof(),Base64.getUrlEncoder().withoutPadding().encode(outboxKey()));var unknown=new ActivationFixture(f.account(),UUID.randomUUID(),f.token().clone())) {
            activationRejected(TOKEN_INVALID_OR_EXPIRED,wrong,p,id(201),ACTIVATION_PASSWORD,NOW);activationRejected(TOKEN_INVALID_OR_EXPIRED,unknown,p,id(201),ACTIVATION_PASSWORD,NOW);
        }
    }
    @Test void activationExactExpiryAndFutureProofFailClosed() throws Exception {
        var p=outboxProtection();try(var f=activationFixture(p)){activationRejected(TOKEN_INVALID_OR_EXPIRED,f,p,id(201),ACTIVATION_PASSWORD,NOW.plusSeconds(86400));activationRejected(TOKEN_INVALID_OR_EXPIRED,f,p,id(201),ACTIVATION_PASSWORD,NOW.minusSeconds(1));}
    }
    @Test void activationConsumedAndRevokedProofCannotBeUsedAgain() throws Exception {
        var p=outboxProtection();try(var f=activationFixture(p)) {
            execute("UPDATE identity.action_proof SET revoked_at=created_at WHERE id='"+f.proof()+"'");activationRejected(TOKEN_INVALID_OR_EXPIRED,f,p,id(201),ACTIVATION_PASSWORD,NOW);
        }
        fixture();p=outboxProtection();try(var f=activationFixture(p)){accept(f,p,id(201),ACTIVATION_PASSWORD,NOW);activationRejected(TOKEN_INVALID_OR_EXPIRED,f,p,id(202),ACTIVATION_PASSWORD,NOW);}
    }
    @Test void activationWrongPurposeAndAccountBindingAreRejected() throws Exception {
        var p=outboxProtection();try(var f=activationFixture(p)) {
            UUID recovery=id(250);execute("INSERT INTO identity.action_proof(id,account_id,purpose,token_hash,created_at,expires_at,verifier_key_id) VALUES ('"+recovery+"','"+f.account()+"','RECOVERY',decode(repeat('23',32),'hex'),TIMESTAMPTZ '2026-10-21 00:05:00Z',TIMESTAMPTZ '2026-10-21 00:20:00Z','v1')");
            try(var wrong=new ActivationFixture(f.account(),recovery,f.token().clone())){activationRejected(TOKEN_INVALID_OR_EXPIRED,wrong,p,id(201),ACTIVATION_PASSWORD,NOW);}
            try(Connection c=db.runtimeConnection();Statement q=c.createStatement();ResultSet r=q.executeQuery("SELECT created_at,expires_at,token_hash FROM identity.action_proof WHERE id='"+f.proof()+"'")) {
                assertTrue(r.next());var wrong=new ActionProof(f.proof(),A,ActionProof.Purpose.INVITATION,r.getTimestamp(1).toInstant(),r.getTimestamp(2).toInstant(),ActionProof.State.AVAILABLE,null);
                assertFalse(p.matches(wrong,"v1",f.token(),r.getBytes(3),NOW));
            }
        }
    }
    @Test void activationDisabledPendingAccountIsNotReactivated() throws Exception {
        var p=outboxProtection();try(var f=activationFixture(p)){execute("UPDATE identity.user_account SET status='DISABLED',disabled_at=updated_at WHERE id='"+f.account()+"'");activationRejected(INVALID_ACCOUNT_STATE,f,p,id(201),ACTIVATION_PASSWORD,NOW);}
    }
    @Test void activationInactiveRealmAndRemovedGrantDeny() throws Exception {
        for(boolean grant:List.of(false,true)) {
            fixture();var p=outboxProtection();try(var f=activationFixture(p)) {
                if(grant)execute("DELETE FROM identity.account_grant WHERE account_id='"+f.account()+"'");else db.ownerExecute("UPDATE identity.realm SET status='INACTIVE',updated_at=CURRENT_TIMESTAMP");
                activationRejected(INVALID_ACCOUNT_STATE,f,p,id(201),ACTIVATION_PASSWORD,NOW);
            }
        }
    }
    @Test void activationPasswordBoundsPrecedeEveryPersistentWrite() throws Exception {
        var p=outboxProtection();try(var f=activationFixture(p)) {
            for(String password:List.of("x".repeat(14),"x".repeat(73),"\u20ac".repeat(25),"\ud800"+"x".repeat(20)))activationRejected(VALIDATION_FAILED,f,p,id(201),password,NOW);
            accept(f,p,id(201),"\u20ac".repeat(24),NOW);
            assertEquals(1,scalar("SELECT count(*) FROM identity.user_account WHERE id='"+f.account()+"' AND status='ACTIVE'"));
        }
    }
    @Test void activationPasswordWriteFailureRollsBackAllEffects() throws Exception {activationRollback("user_account","UPDATE");}
    @Test void activationAuditFailureRollsBackAllEffects() throws Exception {activationRollback("audit_entry","INSERT");}
    @Test void activationReceiptFailureRollsBackAllEffects() throws Exception {activationRollback("command_receipt","INSERT");}
    @Test void activationProofConsumptionFailureRollsBackPassword() throws Exception {activationRollback("action_proof","UPDATE");}
    @Test void activationPayloadClearFailureRollsBackConsumption() throws Exception {activationRollback("invitation_outbox","UPDATE");}
    private List<Object> activationRace(ActivationFixture f,InvitationProtection p,boolean sameKey) throws Exception {
        var pool=Executors.newFixedThreadPool(2);var ready=new CountDownLatch(2);var start=new CountDownLatch(1);
        try {
            var tasks=new ArrayList<Future<Object>>();
            for(UUID key:List.of(id(201),sameKey?id(201):id(202)))tasks.add(pool.submit(()->{
                ready.countDown();if(!start.await(5,TimeUnit.SECONDS))throw new AssertionError("Activation start timeout");
                try{accept(f,p,key,ACTIVATION_PASSWORD,NOW);return "COMMITTED";}catch(IdentityCommandFailure e){return e.code();}
            }));
            assertTrue(ready.await(5,TimeUnit.SECONDS));start.countDown();return List.of(tasks.get(0).get(20,TimeUnit.SECONDS),tasks.get(1).get(20,TimeUnit.SECONDS));
        }finally{start.countDown();pool.shutdownNow();assertTrue(pool.awaitTermination(5,TimeUnit.SECONDS));}
    }
    @Test void activationConcurrentDifferentKeysConsumeExactlyOnce() throws Exception {
        var p=outboxProtection();try(var f=activationFixture(p)) {
            var results=activationRace(f,p,false);assertEquals(1,results.stream().filter("COMMITTED"::equals).count());assertEquals(1,results.stream().filter(TOKEN_INVALID_OR_EXPIRED::equals).count());
            assertEquals(1,scalar("SELECT count(*) FROM identity.command_receipt WHERE proof_id='"+f.proof()+"'"));
        }
    }
    @Test void activationConcurrentSameKeyHasOneDurableEffect() throws Exception {
        var p=outboxProtection();try(var f=activationFixture(p)) {
            assertTrue(activationRace(f,p,true).stream().allMatch("COMMITTED"::equals));
            assertEquals(1,scalar("SELECT count(*) FROM identity.audit_entry WHERE target_id='"+f.account()+"' AND event_type='IDENTITY_INVITATION_ACCEPTED'"));
            assertEquals(2,scalar("SELECT version FROM identity.user_account WHERE id='"+f.account()+"'"));
        }
    }
    @Test void activationRetiredKeyRotationSupportsReplayAndMissingKeyDenies() throws Exception {
        byte[] v=outboxKey(),e=outboxKey();var p=new InvitationProtection(Map.of("v1",v),"v1",Map.of("e1",e),"e1");
        try(var f=activationFixture(p)) {
            var rotated=new InvitationProtection(Map.of("v1",v,"v2",outboxKey()),"v2",Map.of("e1",e,"e2",outboxKey()),"e2");
            accept(f,rotated,id(201),ACTIVATION_PASSWORD,NOW);accept(f,rotated,id(201),ACTIVATION_PASSWORD,NOW);
            var missing=new InvitationProtection(Map.of("v2",outboxKey()),"v2",Map.of("e2",outboxKey()),"e2");
            activationRejected(TOKEN_INVALID_OR_EXPIRED,f,missing,id(201),ACTIVATION_PASSWORD,NOW);
        }finally{Arrays.fill(v,(byte)0);Arrays.fill(e,(byte)0);}
    }
    @Test void activationOptimisticVersionMismatchRollsBackTheTransaction() throws Exception {
        var p=outboxProtection();try(var f=activationFixture(p)) {
            var source=outboxDataSource();var real=new ProtectedInvitationCredentials(p);
            var credentials=new com.elogistic.backend.identity.internal.application.port.InvitationCredentials() {
                public boolean matches(com.elogistic.backend.identity.internal.application.port.IdentityInvitationStore.StoredProof proof,byte[] token,Instant now){return real.matches(proof,token,now);}
                public byte[] fingerprint(com.elogistic.backend.identity.internal.application.port.IdentityInvitationStore.StoredProof proof,byte[] token,String password){return real.fingerprint(proof,token,password);}
                public String hashPassword(String password){new org.springframework.jdbc.core.JdbcTemplate(source).update("UPDATE identity.user_account SET version=version+1 WHERE id=?",f.account());return real.hashPassword(password);}
            };
            var app=new com.elogistic.backend.identity.internal.application.AcceptIdentityInvitation(new JdbcIdentityInvitationStore(source),credentials,Clock.fixed(NOW,ZoneOffset.UTC),Duration.ofHours(24));
            var before=secureSnapshot();try(var command=new com.elogistic.backend.identity.api.IdentityInvitationCommands.Command(f.proof(),f.token(),ACTIVATION_PASSWORD,id(201),REQUEST)) {
                assertEquals(INVALID_ACCOUNT_STATE,assertThrows(IdentityCommandFailure.class,()->app.acceptIdentityInvitation(command)).code());
            }
            assertEquals(before,secureSnapshot());
        }
    }
    @Test void activationRedactsSecretsRevokesOldSessionsAndDeniesStaleDelivery() throws Exception {
        var p=outboxProtection();try(var f=activationFixture(p)) {
            session(id(260),f.account());long sessions=scalar("SELECT count(*) FROM identity.auth_session");
            var auditBefore=scalar("SELECT count(*) FROM identity.audit_entry");var receiptBefore=scalar("SELECT count(*) FROM identity.command_receipt");
            accept(f,p,id(201),ACTIVATION_PASSWORD,NOW);
            assertEquals(sessions,scalar("SELECT count(*) FROM identity.auth_session"));assertEquals(1,scalar("SELECT count(*) FROM identity.auth_session WHERE account_id='"+f.account()+"' AND revoked_at IS NOT NULL"));
            assertEquals(auditBefore+1,scalar("SELECT count(*) FROM identity.audit_entry"));assertEquals(receiptBefore+1,scalar("SELECT count(*) FROM identity.command_receipt"));
            assertEquals(1,scalar("SELECT count(*) FROM identity.invitation_outbox WHERE proof_id='"+f.proof()+"' AND ciphertext IS NULL AND nonce IS NULL AND lease_id IS NULL"));
            assertTrue(outbox(p,NOW).claim().isEmpty());assertEquals(1,scalar("SELECT count(*) FROM identity.realm WHERE id='"+SCOPE+"' AND status='ACTIVE'"));
            String rows=secureSnapshot().toString();assertFalse(rows.contains(ACTIVATION_PASSWORD));assertFalse(rows.contains(new String(f.token(),java.nio.charset.StandardCharsets.US_ASCII)));
            try(var command=new com.elogistic.backend.identity.api.IdentityInvitationCommands.Command(f.proof(),f.token(),ACTIVATION_PASSWORD,id(201),REQUEST)){assertFalse(command.toString().contains(ACTIVATION_PASSWORD));assertFalse(command.toString().contains(new String(f.token(),java.nio.charset.StandardCharsets.US_ASCII)));}
        }
    }


    // Session tests share the existing isolated database/container and cleanup registry.
    private static final String SESSION_PASSWORD="synthetic-session-password";
    private void sessionFixture() throws Exception {
        db.activateRealmForTest(SCOPE);
        String hash=new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder(4).encode(SESSION_PASSWORD);
        try(Connection c=db.runtimeConnection();PreparedStatement q=c.prepareStatement("UPDATE identity.user_account SET password_hash=? WHERE id=?")) {q.setString(1,hash);q.setObject(2,A);assertEquals(1,q.executeUpdate());}
    }
    private com.elogistic.backend.identity.api.IdentitySessionCommands sessions(Instant now) {
        // Explicit test-only admission. No production transport/security assurance is inferred.
        return ProductionIdentitySessionComposition.create(outboxDataSource(),(operation,request,session,present)->{},Clock.fixed(now,ZoneOffset.UTC));
    }
    private com.elogistic.backend.identity.api.IdentitySessionCommands.Result sessionLogin() {
        return sessions(NOW).loginIdentity(new com.elogistic.backend.identity.api.IdentitySessionCommands.Login("fixture."+A,SESSION_PASSWORD,REQUEST));
    }
    private com.elogistic.backend.identity.api.IdentitySessionCommands.Token sessionToken(byte[] raw) {return new com.elogistic.backend.identity.api.IdentitySessionCommands.Token(raw,REQUEST);}
    private void sessionDenied(IdentityCommandFailure.Code code,Runnable action) throws Exception {
        var before=secureSnapshot();var e=assertThrows(IdentityCommandFailure.class,action::run);assertEquals(code,e.code());assertEquals(code.name(),e.getMessage());assertEquals(before,secureSnapshot());
    }
    @Test void sessionLoginPersistsOnlyDigestSessionAndAudit() throws Exception {
        sessionFixture();long count=scalar("SELECT count(*) FROM identity.auth_session");
        try(var r=sessionLogin()) {
            assertEquals(A,r.access().accountId());assertEquals(NOW.plusSeconds(900),r.access().expiresAt());
            assertEquals(count+1,scalar("SELECT count(*) FROM identity.auth_session"));
            assertEquals(1,scalar("SELECT count(*) FROM identity.refresh_token WHERE session_id='"+r.access().sessionId()+"' AND octet_length(token_hash)=32"));
            assertEquals(1,scalar("SELECT count(*) FROM identity.audit_entry WHERE event_type='IDENTITY_LOGIN' AND actor_id='"+A+"'"));
            assertEquals(1,scalar("SELECT version FROM identity.user_account WHERE id='"+A+"'"));
        }
    }
    @Test void sessionInvalidAndUnknownCredentialsHaveSameNoWriteOutcome() throws Exception {
        sessionFixture();var app=sessions(NOW);
        for(String username:List.of("fixture."+A,"not-a-real-account"))sessionDenied(INVALID_CREDENTIALS,()->app.loginIdentity(new com.elogistic.backend.identity.api.IdentitySessionCommands.Login(username,"wrong",REQUEST)));
    }
    @Test void sessionPendingAndDisabledAccountsCannotLogin() throws Exception {
        for(boolean pending:List.of(false,true)) {
            if(pending)fixture();sessionFixture();
            execute(pending?"UPDATE identity.user_account SET status='PENDING_ACTIVATION',password_hash=NULL,email_verified=FALSE,activated_at=NULL WHERE id='"+A+"'":"UPDATE identity.user_account SET status='DISABLED',disabled_at=updated_at WHERE id='"+A+"'");
            sessionDenied(INVALID_CREDENTIALS,()->sessions(NOW).loginIdentity(new com.elogistic.backend.identity.api.IdentitySessionCommands.Login("fixture."+A,SESSION_PASSWORD,REQUEST)));
        }
    }
    @Test void sessionInactiveRealmAndUnsupportedScopeFailClosed() throws Exception {
        sessionFixture();db.ownerExecute("UPDATE identity.realm SET status='INACTIVE',updated_at=CURRENT_TIMESTAMP");
        sessionDenied(INVALID_CREDENTIALS,()->sessionLogin());
        db.ownerExecute("UPDATE identity.realm SET status='ACTIVE',updated_at=CURRENT_TIMESTAMP");
        execute("DELETE FROM identity.account_grant WHERE account_id='"+A+"'");
        execute("INSERT INTO identity.account_grant(account_id,role_code,scope_type,scope_id,granted_at) VALUES ('"+A+"','OPERATOR','WAREHOUSE','"+X+"',CURRENT_TIMESTAMP)");
        sessionDenied(INVALID_CREDENTIALS,()->sessionLogin());
    }
    @Test void sessionRefreshRotatesPredecessorAndPreservesDeadlines() throws Exception {
        sessionFixture();try(var first=sessionLogin();var c=sessionToken(first.refreshToken());var next=sessions(NOW.plusSeconds(60)).refreshIdentity(c)) {
            assertEquals(first.access().sessionId(),next.access().sessionId());assertEquals(first.absoluteExpiresAt(),next.absoluteExpiresAt());
            assertFalse(Arrays.equals(first.refreshToken(),next.refreshToken()));
            assertEquals(2,scalar("SELECT count(*) FROM identity.refresh_token WHERE session_id='"+first.access().sessionId()+"'"));
            assertEquals(1,scalar("SELECT count(*) FROM identity.refresh_token WHERE session_id='"+first.access().sessionId()+"' AND predecessor_id IS NOT NULL AND consumed_at IS NULL"));
            assertEquals(1,scalar("SELECT count(*) FROM identity.auth_session WHERE id='"+first.access().sessionId()+"' AND last_activity_at=TIMESTAMPTZ '2026-10-21 00:05:00Z' AND idle_expires_at=TIMESTAMPTZ '2026-10-21 00:35:00Z'"));
        }
    }
    @Test void sessionReplayCommitsFamilyRevocationBeforeDenial() throws Exception {
        sessionFixture();try(var first=sessionLogin();var old=sessionToken(first.refreshToken());var next=sessions(NOW).refreshIdentity(old)) {
            assertEquals(REFRESH_REPLAY,assertThrows(IdentityCommandFailure.class,()->sessions(NOW).refreshIdentity(old)).code());
            assertEquals(1,scalar("SELECT count(*) FROM identity.auth_session WHERE id='"+first.access().sessionId()+"' AND revoked_at IS NOT NULL"));
            assertEquals(0,scalar("SELECT count(*) FROM identity.refresh_token WHERE session_id='"+first.access().sessionId()+"' AND revoked_at IS NULL"));
            try(var newest=sessionToken(next.refreshToken())) {sessionDenied(SESSION_INVALID,()->sessions(NOW).refreshIdentity(newest));}
        }
    }
    @Test void sessionConcurrentRefreshHasOneRotationThenReplayRevocation() throws Exception {
        sessionFixture();try(var first=sessionLogin()) {
            var pool=Executors.newFixedThreadPool(2);var ready=new CountDownLatch(2);var start=new CountDownLatch(1);
            try {
                List<Future<Object>> results=new ArrayList<>();
                for(int i=0;i<2;i++)results.add(pool.submit(()->{ready.countDown();assertTrue(start.await(5,TimeUnit.SECONDS));try(var c=sessionToken(first.refreshToken());var next=sessions(NOW).refreshIdentity(c)){return "ROTATED";}catch(IdentityCommandFailure e){return e.code();}}));
                assertTrue(ready.await(5,TimeUnit.SECONDS));start.countDown();var values=List.of(results.get(0).get(20,TimeUnit.SECONDS),results.get(1).get(20,TimeUnit.SECONDS));
                assertEquals(1,values.stream().filter("ROTATED"::equals).count());assertEquals(1,values.stream().filter(REFRESH_REPLAY::equals).count());
                assertEquals(2,scalar("SELECT count(*) FROM identity.refresh_token WHERE session_id='"+first.access().sessionId()+"'"));
                assertEquals(1,scalar("SELECT count(*) FROM identity.auth_session WHERE id='"+first.access().sessionId()+"' AND revoked_at IS NOT NULL"));
            } finally {start.countDown();pool.shutdownNow();assertTrue(pool.awaitTermination(5,TimeUnit.SECONDS));}
        }
    }
    @Test void sessionExactIdleAndAbsoluteExpiryRejectRefresh() throws Exception {
        sessionFixture();try(var first=sessionLogin();var c=sessionToken(first.refreshToken())) {
            sessionDenied(SESSION_INVALID,()->sessions(NOW.plusSeconds(1800)).refreshIdentity(c));
            sessionDenied(SESSION_INVALID,()->sessions(NOW.plusSeconds(28800)).refreshIdentity(c));
        }
    }
    @Test void sessionStaleAuthVersionAndDisabledAccountRejectRefresh() throws Exception {
        sessionFixture();try(var first=sessionLogin();var c=sessionToken(first.refreshToken())) {
            execute("UPDATE identity.user_account SET version=2,auth_version=2 WHERE id='"+A+"'");sessionDenied(SESSION_INVALID,()->sessions(NOW).refreshIdentity(c));
            execute("UPDATE identity.user_account SET auth_version=1,status='DISABLED',disabled_at=updated_at WHERE id='"+A+"'");sessionDenied(SESSION_INVALID,()->sessions(NOW).refreshIdentity(c));
        }
    }
    @Test void sessionRealmDeactivationRejectsRefreshWithoutMutation() throws Exception {
        sessionFixture();try(var first=sessionLogin();var c=sessionToken(first.refreshToken())) {
            db.ownerExecute("UPDATE identity.realm SET status='INACTIVE',updated_at=CURRENT_TIMESTAMP");sessionDenied(SESSION_INVALID,()->sessions(NOW).refreshIdentity(c));
        }
    }
    @Test void sessionLogoutPreservesOtherAccountsAndIsIdempotent() throws Exception {
        sessionFixture();try(var first=sessionLogin();var c=sessionToken(first.refreshToken())) {
            sessions(NOW).logoutIdentity(c);var before=secureSnapshot();sessions(NOW).logoutIdentity(c);assertEquals(before,secureSnapshot());
            assertEquals(1,scalar("SELECT count(*) FROM identity.auth_session WHERE id='"+AS+"' AND account_id='"+B+"' AND revoked_at IS NULL"));
            assertEquals(1,scalar("SELECT count(*) FROM identity.audit_entry WHERE event_type='IDENTITY_LOGOUT'"));
            sessionDenied(SESSION_INVALID,()->sessions(NOW).refreshIdentity(c));
        }
    }
    @Test void sessionExpiredLogoutAndMissingCookieDoNotRestoreSessions() throws Exception {
        sessionFixture();try(var first=sessionLogin();var c=sessionToken(first.refreshToken());var absent=sessionToken(null)) {
            sessions(NOW.plusSeconds(30000)).logoutIdentity(c);var before=secureSnapshot();sessions(NOW.plusSeconds(30000)).logoutIdentity(absent);assertEquals(before,secureSnapshot());
        }
    }
    @Test void sessionLoginMandatoryWriteFailuresRollBackEverything() throws Exception {
        for(String table:List.of("auth_session","refresh_token","audit_entry")) {
            if(!table.equals("auth_session"))fixture();sessionFixture();failWrite(table,"INSERT");var before=secureSnapshot();
            assertThrows(org.springframework.dao.DataAccessException.class,()->sessionLogin());assertEquals(before,secureSnapshot());
        }
    }
    @Test void sessionRefreshSuccessorFailureRollsBackConsumption() throws Exception {
        sessionFixture();try(var first=sessionLogin();var c=sessionToken(first.refreshToken())) {
            failWrite("refresh_token","INSERT");var before=secureSnapshot();assertThrows(org.springframework.dao.DataAccessException.class,()->sessions(NOW).refreshIdentity(c));assertEquals(before,secureSnapshot());
        }
    }
    @Test void sessionReplayAuditFailureRollsBackRevocation() throws Exception {
        sessionFixture();try(var first=sessionLogin();var c=sessionToken(first.refreshToken());var next=sessions(NOW).refreshIdentity(c)) {
            failWrite("audit_entry","INSERT");var before=secureSnapshot();assertThrows(org.springframework.dao.DataAccessException.class,()->sessions(NOW).refreshIdentity(c));assertEquals(before,secureSnapshot());
        }
    }
    @Test void sessionAdmissionMustBindTheAuthoritativeFamily() throws Exception {
        sessionFixture();try(var first=sessionLogin();var c=sessionToken(first.refreshToken())) {
            var app=ProductionIdentitySessionComposition.create(outboxDataSource(),(op,req,sid,present)->{assertEquals(first.access().sessionId(),sid);throw new IdentityCommandFailure(CSRF_REJECTED);},Clock.fixed(NOW,ZoneOffset.UTC));
            sessionDenied(CSRF_REJECTED,()->app.refreshIdentity(c));sessionDenied(CSRF_REJECTED,()->app.logoutIdentity(c));
        }
    }
    @Test void sessionPasswordsAndRawRefreshNeverEnterPersistedRows() throws Exception {
        sessionFixture();try(var first=sessionLogin();var c=sessionToken(first.refreshToken());var next=sessions(NOW).refreshIdentity(c)) {
            String rows=secureSnapshot().toString();assertFalse(rows.contains(SESSION_PASSWORD));
            byte[] a=first.refreshToken(),b=next.refreshToken();try {assertFalse(rows.contains(new String(a,java.nio.charset.StandardCharsets.US_ASCII)));assertFalse(rows.contains(new String(b,java.nio.charset.StandardCharsets.US_ASCII)));}finally {Arrays.fill(a,(byte)0);Arrays.fill(b,(byte)0);}
            assertEquals("IdentitySessionResult[redacted]",next.toString());assertEquals("IdentitySessionToken[redacted]",c.toString());
        }
    }
    @Test void sessionWaitingLoginObservesConcurrentRealmDeactivation() throws Exception {
        sessionFixture();var pool=Executors.newSingleThreadExecutor();
        try(Connection c=db.migrationConnection();Statement q=c.createStatement()) {
            c.setAutoCommit(false);q.execute("SET ROLE "+db.owner());
            // Use the exact same lock identity as JdbcIdentityGrantStore; assert the waiting admission result.
            q.execute("SELECT pg_advisory_xact_lock("+0x4944454e54495459L+")");
            q.execute("UPDATE identity.realm SET status='INACTIVE',updated_at=CURRENT_TIMESTAMP");
            var started=new CountDownLatch(1);var waiting=pool.submit(()->{started.countDown();try(var result=sessionLogin()){return "ADMITTED";}catch(IdentityCommandFailure e){return e.code();}});
            assertTrue(started.await(5,TimeUnit.SECONDS));assertThrows(TimeoutException.class,()->waiting.get(150,TimeUnit.MILLISECONDS));c.commit();assertEquals(INVALID_CREDENTIALS,waiting.get(15,TimeUnit.SECONDS));
        } finally {pool.shutdownNow();assertTrue(pool.awaitTermination(5,TimeUnit.SECONDS));}
    }

    private org.springframework.transaction.support.TransactionTemplate sessionCallerTransaction(javax.sql.DataSource source) {
        var outer=new org.springframework.transaction.support.TransactionTemplate(new org.springframework.jdbc.support.JdbcTransactionManager(source));
        outer.setIsolationLevel(org.springframework.transaction.TransactionDefinition.ISOLATION_READ_COMMITTED);
        return outer;
    }
    private void sessionOwnershipRejected(Runnable action) {
        var failure=assertThrows(IllegalStateException.class,action::run);
        assertEquals("SESSION_TRANSACTION_OWNERSHIP_REQUIRED",failure.getMessage());
    }
    @Test void sessionAmbientRefreshRejectedBeforeRotationAndCallerCommit() throws Exception {
        sessionFixture();var source=outboxDataSource();
        var app=ProductionIdentitySessionComposition.create(source,(op,request,sid,present)->{},Clock.fixed(NOW,ZoneOffset.UTC));
        try(var first=sessionLogin();var token=sessionToken(first.refreshToken())) {
            var before=secureSnapshot();
            sessionCallerTransaction(source).executeWithoutResult(status->{
                assertTrue(org.springframework.transaction.support.TransactionSynchronizationManager.hasResource(source));
                sessionOwnershipRejected(()->app.refreshIdentity(token));
                assertFalse(status.isRollbackOnly());
            });
            // Snapshot uses independent runtime connections after the caller's physical commit.
            assertEquals(before,secureSnapshot());
            try(var rotated=app.refreshIdentity(token)) {assertEquals(first.access().sessionId(),rotated.access().sessionId());}
        }
    }
    @Test void sessionAmbientReplayRejectedBeforeCallerRollback() throws Exception {
        sessionFixture();var source=outboxDataSource();
        var app=ProductionIdentitySessionComposition.create(source,(op,request,sid,present)->{},Clock.fixed(NOW,ZoneOffset.UTC));
        try(var first=sessionLogin();var old=sessionToken(first.refreshToken());var next=app.refreshIdentity(old)) {
            var before=secureSnapshot();
            sessionCallerTransaction(source).executeWithoutResult(status->{
                sessionOwnershipRejected(()->app.refreshIdentity(old));
                // The caller retains control: denial is ownership failure, never REFRESH_REPLAY.
                assertFalse(status.isRollbackOnly());status.setRollbackOnly();
            });
            assertEquals(before,secureSnapshot());
            try(var current=sessionToken(next.refreshToken());var rotated=app.refreshIdentity(current)) {
                assertEquals(first.access().sessionId(),rotated.access().sessionId());
            }
        }
    }
    @Test void sessionDirectReplayRevocationAndAuditAreIndependentlyCommitted() throws Exception {
        sessionFixture();
        try(var first=sessionLogin();var old=sessionToken(first.refreshToken());var next=sessions(NOW).refreshIdentity(old)) {
            assertEquals(REFRESH_REPLAY,assertThrows(IdentityCommandFailure.class,()->sessions(NOW).refreshIdentity(old)).code());
            // A new JDBC transaction, not a Spring thread-bound/caller connection.
            try(Connection c=db.runtimeConnection()) {
                c.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);c.setAutoCommit(false);
                try(PreparedStatement q=c.prepareStatement("SELECT (SELECT count(*) FROM identity.auth_session WHERE id=? AND revoked_at IS NOT NULL),(SELECT count(*) FROM identity.refresh_token WHERE session_id=? AND revoked_at IS NULL),(SELECT count(*) FROM identity.audit_entry WHERE target_id=? AND event_type='IDENTITY_REFRESH_REPLAY')")) {
                    q.setObject(1,first.access().sessionId());q.setObject(2,first.access().sessionId());q.setObject(3,A);
                    try(ResultSet row=q.executeQuery()) {assertTrue(row.next());assertEquals(1,row.getLong(1));assertEquals(0,row.getLong(2));assertEquals(1,row.getLong(3));}
                }
                c.commit();
            }
            try(var current=sessionToken(next.refreshToken())) {sessionDenied(SESSION_INVALID,()->sessions(NOW).refreshIdentity(current));}
        }
    }
    @Test void sessionAmbientLoginRejectedBeforeCallerCommit() throws Exception {
        sessionFixture();var source=outboxDataSource();
        var app=ProductionIdentitySessionComposition.create(source,(op,request,sid,present)->{},Clock.fixed(NOW,ZoneOffset.UTC));
        var before=secureSnapshot();
        sessionCallerTransaction(source).executeWithoutResult(status->{
            sessionOwnershipRejected(()->app.loginIdentity(new com.elogistic.backend.identity.api.IdentitySessionCommands.Login("fixture."+A,SESSION_PASSWORD,REQUEST)));
            assertFalse(status.isRollbackOnly());
        });
        assertEquals(before,secureSnapshot());
        try(var valid=app.loginIdentity(new com.elogistic.backend.identity.api.IdentitySessionCommands.Login("fixture."+A,SESSION_PASSWORD,REQUEST))) {assertEquals(A,valid.access().accountId());}
    }
    @Test void sessionAmbientLogoutRejectedBeforeCallerRollback() throws Exception {
        sessionFixture();var source=outboxDataSource();
        var app=ProductionIdentitySessionComposition.create(source,(op,request,sid,present)->{},Clock.fixed(NOW,ZoneOffset.UTC));
        try(var first=sessionLogin();var token=sessionToken(first.refreshToken())) {
            var before=secureSnapshot();
            sessionCallerTransaction(source).executeWithoutResult(status->{
                sessionOwnershipRejected(()->app.logoutIdentity(token));assertFalse(status.isRollbackOnly());status.setRollbackOnly();
            });
            assertEquals(before,secureSnapshot());
            app.logoutIdentity(token);app.logoutIdentity(token);
            assertEquals(1,scalar("SELECT count(*) FROM identity.auth_session WHERE id='"+first.access().sessionId()+"' AND revoked_at IS NOT NULL"));
            assertEquals(0,scalar("SELECT count(*) FROM identity.refresh_token WHERE session_id='"+first.access().sessionId()+"' AND revoked_at IS NULL"));
            assertEquals(1,scalar("SELECT count(*) FROM identity.audit_entry WHERE event_type='IDENTITY_LOGOUT' AND target_id='"+A+"'"));
        }
    }

    // Current-session/activity Application only; synthetic trusted admission, never a JWT/HTTP adapter.
    private com.elogistic.backend.identity.api.IdentityCurrentSessionCommands currentApp(UUID sid,UUID subject,long version,Instant time) {
        return ProductionIdentityCurrentSessionComposition.create(outboxDataSource(),(op,req)->
            new com.elogistic.backend.identity.internal.application.port.CurrentSessionAdmission.Context(sid,subject,version,time,time.plusSeconds(900)),Clock.fixed(time,ZoneOffset.UTC));
    }
    private void currentDeniedBoth(UUID sid,UUID subject,long version,Instant time) throws Exception {
        var app=currentApp(sid,subject,version,time);sessionDenied(SESSION_INVALID,()->app.getIdentityMe(REQUEST));sessionDenied(SESSION_INVALID,()->app.recordMeaningfulActivity(REQUEST));
    }
    private void currentActivityTimes(UUID sid,Instant last,Instant idle,Instant absolute) throws Exception {
        try(Connection c=db.runtimeConnection();PreparedStatement q=c.prepareStatement("SELECT last_activity_at,idle_expires_at,absolute_expires_at,auth_version,revoked_at FROM identity.auth_session WHERE id=?")) {
            q.setObject(1,sid);try(ResultSet row=q.executeQuery()) {assertTrue(row.next());assertEquals(last,row.getTimestamp(1).toInstant());assertEquals(idle,row.getTimestamp(2).toInstant());assertEquals(absolute,row.getTimestamp(3).toInstant());assertEquals(1,row.getLong(4));assertNull(row.getTimestamp(5));}
        }
    }
    @Test void currentSessionMeReturnsApprovedAccountWithoutExtendingIdle() throws Exception {
        sessionFixture();try(var first=sessionLogin()) {
            var before=secureSnapshot();var result=currentApp(first.access().sessionId(),A,1,NOW.plusSeconds(60)).getIdentityMe(REQUEST);
            assertEquals(A,result.id());assertEquals(List.of(ADMIN),result.grants());assertEquals(1,result.version());assertTrue(result.emailVerified());assertEquals("IdentityAccountResult[redacted]",result.toString());assertEquals(before,secureSnapshot());
        }
    }
    @Test void currentSessionActivityUpdatesOnlyOwnedIdleFields() throws Exception {
        sessionFixture();try(var first=sessionLogin()) {
            var before=secureSnapshot();var app=currentApp(first.access().sessionId(),A,1,NOW.plusSeconds(60));app.recordMeaningfulActivity(REQUEST);
            currentActivityTimes(first.access().sessionId(),NOW.plusSeconds(60),NOW.plusSeconds(1860),first.absoluteExpiresAt());
            var after=secureSnapshot();before.remove("auth_session");after.remove("auth_session");assertEquals(before,after);
            var once=secureSnapshot();app.recordMeaningfulActivity(REQUEST);assertEquals(once,secureSnapshot());
        }
    }
    @Test void currentSessionAccountAndRealmSubstitutionFailClosed() throws Exception {
        sessionFixture();try(var first=sessionLogin()) {
            currentDeniedBoth(first.access().sessionId(),B,1,NOW);currentDeniedBoth(AS,A,1,NOW);
            // A valid session for B returns only B, never caller-selected A or A's contacts.
            var own=currentApp(AS,B,1,NOW).getIdentityMe(REQUEST);assertEquals(B,own.id());assertNotEquals(A,own.id());
        }
    }
    @Test void currentSessionExpiredAccessCannotReadOrRecordActivity() throws Exception {
        sessionFixture();try(var first=sessionLogin()) {
            var app=ProductionIdentityCurrentSessionComposition.create(outboxDataSource(),(op,req)->new com.elogistic.backend.identity.internal.application.port.CurrentSessionAdmission.Context(first.access().sessionId(),A,1,NOW.minusSeconds(900),NOW),Clock.fixed(NOW,ZoneOffset.UTC));
            sessionDenied(SESSION_INVALID,()->app.getIdentityMe(REQUEST));sessionDenied(SESSION_INVALID,()->app.recordMeaningfulActivity(REQUEST));
        }
    }
    @Test void currentSessionStaleVersionAndDisabledAccountDenyWithoutMutation() throws Exception {
        sessionFixture();try(var first=sessionLogin()) {
            currentDeniedBoth(first.access().sessionId(),A,2,NOW);
            execute("UPDATE identity.user_account SET version=2,auth_version=2 WHERE id='"+A+"'");currentDeniedBoth(first.access().sessionId(),A,1,NOW);
            execute("UPDATE identity.user_account SET auth_version=1,status='DISABLED',disabled_at=updated_at WHERE id='"+A+"'");currentDeniedBoth(first.access().sessionId(),A,1,NOW);
        }
    }
    @Test void currentSessionPendingAccountCannotUseOldSession() throws Exception {
        sessionFixture();try(var first=sessionLogin()) {
            execute("UPDATE identity.user_account SET status='PENDING_ACTIVATION',password_hash=NULL,email_verified=FALSE,activated_at=NULL WHERE id='"+A+"'");currentDeniedBoth(first.access().sessionId(),A,1,NOW);
        }
    }
    @Test void currentSessionExactIdleAbsoluteAndRevocationDenyWithoutMutation() throws Exception {
        sessionFixture();try(var first=sessionLogin()) {
            currentDeniedBoth(first.access().sessionId(),A,1,NOW.plusSeconds(1800));currentDeniedBoth(first.access().sessionId(),A,1,NOW.plusSeconds(28800));
            execute("UPDATE identity.auth_session SET revoked_at=created_at WHERE id='"+first.access().sessionId()+"'");currentDeniedBoth(first.access().sessionId(),A,1,NOW);
        }
    }
    @Test void currentSessionInactiveRealmAndExternalGrantsFailClosed() throws Exception {
        sessionFixture();try(var first=sessionLogin()) {
            db.ownerExecute("UPDATE identity.realm SET status='INACTIVE',updated_at=CURRENT_TIMESTAMP");currentDeniedBoth(first.access().sessionId(),A,1,NOW);
            db.ownerExecute("UPDATE identity.realm SET status='ACTIVE',updated_at=CURRENT_TIMESTAMP");execute("DELETE FROM identity.account_grant WHERE account_id='"+A+"'");
            execute("INSERT INTO identity.account_grant(account_id,role_code,scope_type,scope_id,granted_at) VALUES ('"+A+"','OPERATOR','WAREHOUSE','"+X+"',CURRENT_TIMESTAMP)");currentDeniedBoth(first.access().sessionId(),A,1,NOW);
        }
    }
    @Test void currentSessionActivityNeverExtendsAbsoluteDeadline() throws Exception {
        sessionFixture();try(var first=sessionLogin()) {
            execute("UPDATE identity.auth_session SET last_activity_at=created_at+INTERVAL '7 hours 50 minutes',idle_expires_at=absolute_expires_at WHERE id='"+first.access().sessionId()+"'");
            Instant time=NOW.plusSeconds(28740);var app=ProductionIdentityCurrentSessionComposition.create(outboxDataSource(),(op,req)->new com.elogistic.backend.identity.internal.application.port.CurrentSessionAdmission.Context(first.access().sessionId(),A,1,time,first.absoluteExpiresAt()),Clock.fixed(time,ZoneOffset.UTC));
            var before=secureSnapshot();app.recordMeaningfulActivity(REQUEST);currentActivityTimes(first.access().sessionId(),time,first.absoluteExpiresAt(),first.absoluteExpiresAt());
            var after=secureSnapshot();before.remove("auth_session");after.remove("auth_session");assertEquals(before,after);
        }
    }
    @Test void currentSessionActivityWriteFailureRollsBackAllState() throws Exception {
        sessionFixture();try(var first=sessionLogin()) {
            failWrite("auth_session","UPDATE");var before=secureSnapshot();assertThrows(org.springframework.dao.DataAccessException.class,()->currentApp(first.access().sessionId(),A,1,NOW.plusSeconds(60)).recordMeaningfulActivity(REQUEST));assertEquals(before,secureSnapshot());
        }
    }
    @Test void currentSessionOwnedBoundaryRejectsAmbientReadAndActivity() throws Exception {
        sessionFixture();try(var first=sessionLogin()) {
            var source=outboxDataSource();var app=ProductionIdentityCurrentSessionComposition.create(source,(op,req)->new com.elogistic.backend.identity.internal.application.port.CurrentSessionAdmission.Context(first.access().sessionId(),A,1,NOW,NOW.plusSeconds(900)),Clock.fixed(NOW.plusSeconds(60),ZoneOffset.UTC));
            var before=secureSnapshot();sessionCallerTransaction(source).executeWithoutResult(status->{sessionOwnershipRejected(()->app.getIdentityMe(REQUEST));sessionOwnershipRejected(()->app.recordMeaningfulActivity(REQUEST));assertFalse(status.isRollbackOnly());status.setRollbackOnly();});assertEquals(before,secureSnapshot());
        }
    }
    @Test void currentSessionWaitingActivityObservesConcurrentRevocation() throws Exception {
        sessionFixture();try(var first=sessionLogin()) {
            var pool=Executors.newSingleThreadExecutor();try(Connection c=db.runtimeConnection();Statement q=c.createStatement()) {
                c.setAutoCommit(false);q.execute("SELECT pg_advisory_xact_lock("+0x4944454e54495459L+")");q.execute("UPDATE identity.auth_session SET revoked_at=created_at WHERE id='"+first.access().sessionId()+"'");
                var started=new CountDownLatch(1);var waiting=pool.submit(()->{started.countDown();try{currentApp(first.access().sessionId(),A,1,NOW.plusSeconds(60)).recordMeaningfulActivity(REQUEST);return "ADMITTED";}catch(IdentityCommandFailure e){return e.code();}});
                assertTrue(started.await(5,TimeUnit.SECONDS));assertThrows(TimeoutException.class,()->waiting.get(150,TimeUnit.MILLISECONDS));c.commit();assertEquals(SESSION_INVALID,waiting.get(15,TimeUnit.SECONDS));
                assertEquals(1,scalar("SELECT count(*) FROM identity.auth_session WHERE id='"+first.access().sessionId()+"' AND last_activity_at=created_at AND revoked_at IS NOT NULL"));
            } finally {pool.shutdownNow();assertTrue(pool.awaitTermination(5,TimeUnit.SECONDS));}
        }
    }
    @Test void currentSessionWaitingMeObservesConcurrentRealmDeactivation() throws Exception {
        sessionFixture();try(var first=sessionLogin()) {
            var pool=Executors.newSingleThreadExecutor();try(Connection c=db.migrationConnection();Statement q=c.createStatement()) {
                c.setAutoCommit(false);q.execute("SET ROLE "+db.owner());q.execute("SELECT pg_advisory_xact_lock("+0x4944454e54495459L+")");q.execute("UPDATE identity.realm SET status='INACTIVE',updated_at=CURRENT_TIMESTAMP");
                var started=new CountDownLatch(1);var waiting=pool.submit(()->{started.countDown();try{currentApp(first.access().sessionId(),A,1,NOW).getIdentityMe(REQUEST);return "ADMITTED";}catch(IdentityCommandFailure e){return e.code();}});
                assertTrue(started.await(5,TimeUnit.SECONDS));assertThrows(TimeoutException.class,()->waiting.get(150,TimeUnit.MILLISECONDS));c.commit();assertEquals(SESSION_INVALID,waiting.get(15,TimeUnit.SECONDS));
            } finally {pool.shutdownNow();assertTrue(pool.awaitTermination(5,TimeUnit.SECONDS));}
        }
    }
    @Test void currentSessionConcurrentActivityCannotRewindIdle() throws Exception {
        sessionFixture();try(var first=sessionLogin()) {
            var pool=Executors.newFixedThreadPool(2);var ready=new CountDownLatch(2);var start=new CountDownLatch(1);
            try {
                List<Future<Object>> results=new ArrayList<>();for(int elapsed:List.of(60,120))results.add(pool.submit(()->{ready.countDown();assertTrue(start.await(5,TimeUnit.SECONDS));try{currentApp(first.access().sessionId(),A,1,NOW.plusSeconds(elapsed)).recordMeaningfulActivity(REQUEST);return "UPDATED";}catch(IdentityCommandFailure e){return e.code();}}));
                assertTrue(ready.await(5,TimeUnit.SECONDS));start.countDown();Object older=results.get(0).get(20,TimeUnit.SECONDS),newer=results.get(1).get(20,TimeUnit.SECONDS);assertEquals("UPDATED",newer);assertTrue(older.equals("UPDATED") || older==SESSION_INVALID);
                currentActivityTimes(first.access().sessionId(),NOW.plusSeconds(120),NOW.plusSeconds(1920),first.absoluteExpiresAt());
            } finally {start.countDown();pool.shutdownNow();assertTrue(pool.awaitTermination(5,TimeUnit.SECONDS));}
        }
    }

    // Remaining Application operations: real runtime DML/crypto; no HTTP or external authority.
    private com.elogistic.backend.identity.api.IdentityAdministrationCommands administration(UUID sid,UUID actor,InvitationProtection protection) {
        return administration(sid,actor,protection,NOW);
    }
    private com.elogistic.backend.identity.api.IdentityAdministrationCommands administration(UUID sid,UUID actor,InvitationProtection protection,Instant time) {
        var source=outboxDataSource();var clock=Clock.fixed(time,ZoneOffset.UTC);
        com.elogistic.backend.identity.internal.application.port.CurrentSessionAdmission admission=(op,request)->new com.elogistic.backend.identity.internal.application.port.CurrentSessionAdmission.Context(sid,actor,1,time,time.plusSeconds(900));
        return protection==null?ProductionIdentityAdministrationComposition.create(source,admission,clock,Duration.ofHours(24)):
            ProductionIdentityAdministrationComposition.secure(source,admission,protection,outboxPolicy(),clock,Duration.ofHours(24));
    }
    private com.elogistic.backend.identity.api.IdentityAdministrationCommands.Command adminCommand(UUID target,long version,UUID key,String reason){return new com.elogistic.backend.identity.api.IdentityAdministrationCommands.Command(REQUEST,target,version,key,reason);}
    private com.elogistic.backend.identity.api.IdentityAdministrationCommands.Command adminCommand(){return adminCommand(B,1,KEY,"Synthetic administration");}
    private void adminDenied(IdentityCommandFailure.Code code,Runnable action) throws Exception {var before=secureSnapshot();assertEquals(code,assertThrows(IdentityCommandFailure.class,action::run).code());assertEquals(before,secureSnapshot());}
    private UUID adminBootstrapId() throws Exception {
        try(Connection c=db.runtimeConnection();Statement s=c.createStatement();ResultSet row=s.executeQuery("SELECT bootstrap_account_id FROM identity.realm WHERE id='"+SCOPE+"'")){assertTrue(row.next());return row.getObject(1,UUID.class);}
    }
    private List<Object> adminRace(Callable<Object> left,Callable<Object> right) throws Exception {
        var pool=Executors.newFixedThreadPool(2);var ready=new CountDownLatch(2);var go=new CountDownLatch(1);
        try {
            var futures=new ArrayList<Future<Object>>();for(var work:List.of(left,right))futures.add(pool.submit(()->{ready.countDown();assertTrue(go.await(5,TimeUnit.SECONDS));try{return work.call();}catch(IdentityCommandFailure e){return e.code();}}));
            assertTrue(ready.await(5,TimeUnit.SECONDS));go.countDown();return List.of(futures.get(0).get(20,TimeUnit.SECONDS),futures.get(1).get(20,TimeUnit.SECONDS));
        }finally{go.countDown();pool.shutdownNow();assertTrue(pool.awaitTermination(5,TimeUnit.SECONDS));}
    }
    @Test void adminReadFiltersWholeTargetAndDoesNotWriteActivity() throws Exception {
        sessionFixture();var app=administration(S,A,null);var before=secureSnapshot();assertEquals(B,app.getIdentityAccount(B,REQUEST).id());assertEquals(before,secureSnapshot());
        adminDenied(ACCOUNT_NOT_ACCESSIBLE,()->app.getIdentityAccount(T,REQUEST));adminDenied(ACCOUNT_NOT_ACCESSIBLE,()->app.getIdentityAccount(id(399),REQUEST));
    }
    @Test void adminCatalogsExposeOnlyCurrentRegisteredIdentityPair() throws Exception {
        sessionFixture();var app=administration(S,A,null);var before=secureSnapshot();assertEquals(List.of("IDENTITY_MANAGEMENT"),app.listIdentityRoles(REQUEST).items().stream().map(com.elogistic.backend.identity.api.IdentityAdministrationCommands.Role::code).toList());
        var scopes=app.listIdentityScopes(REQUEST).items();assertEquals(1,scopes.size());assertEquals(SCOPE,scopes.getFirst().id());assertEquals("IDENTITY",scopes.getFirst().type());assertEquals(before,secureSnapshot());
    }
    @Test void adminPaginationFiltersUnauthorizedRowsBeforeLimit() throws Exception {
        sessionFixture();UUID extra=id(310),mixed=id(311),unscoped=id(312);account(extra,"ACTIVE",List.of(ADMIN));account(mixed,"ACTIVE",List.of(ADMIN,GX));account(unscoped,"ACTIVE",List.of());
        var app=administration(S,A,null);var before=secureSnapshot();var seen=new HashSet<UUID>();String cursor=null;
        for(int page=0;page<4;page++) {
            var result=app.listIdentityAccounts(new com.elogistic.backend.identity.api.IdentityAdministrationCommands.Query(REQUEST,cursor,1));
            assertEquals(1,result.items().size());assertTrue(seen.add(result.items().getFirst().id()));cursor=result.nextCursor();if(cursor==null)break;
        }
        assertNull(cursor);assertEquals(Set.of(B,extra,adminBootstrapId()),seen);assertEquals(before,secureSnapshot());
    }
    @Test void adminCursorCannotBeTransferredBetweenActors() throws Exception {
        sessionFixture();account(id(313),"ACTIVE",List.of(ADMIN));var a=administration(S,A,null);var cursor=a.listIdentityAccounts(new com.elogistic.backend.identity.api.IdentityAdministrationCommands.Query(REQUEST,null,1)).nextCursor();assertNotNull(cursor);
        adminDenied(VALIDATION_FAILED,()->administration(AS,B,null).listIdentityAccounts(new com.elogistic.backend.identity.api.IdentityAdministrationCommands.Query(REQUEST,cursor,1)));
    }
    @Test void adminRevokedStaleAndExpiredSessionsCannotReadOrMutate() throws Exception {
        sessionFixture();var app=administration(S,A,null);execute("UPDATE identity.auth_session SET revoked_at=created_at WHERE id='"+S+"'");adminDenied(SESSION_INVALID,()->app.getIdentityAccount(B,REQUEST));adminDenied(SESSION_INVALID,()->app.disableIdentityAccount(adminCommand()));
        execute("UPDATE identity.auth_session SET revoked_at=NULL WHERE id='"+S+"'");execute("UPDATE identity.user_account SET version=2,auth_version=2 WHERE id='"+A+"'");adminDenied(SESSION_INVALID,()->app.listIdentityRoles(REQUEST));
        execute("UPDATE identity.user_account SET auth_version=1 WHERE id='"+A+"'");execute("UPDATE identity.auth_session SET idle_expires_at=TIMESTAMPTZ '2026-10-21 00:05:00Z' WHERE id='"+S+"'");adminDenied(SESSION_INVALID,()->app.reactivateIdentityAccount(adminCommand()));
    }
    @Test void adminInactiveRealmAndMixedActorRemainDefaultDeny() throws Exception {
        sessionFixture();var app=administration(S,A,null);db.ownerExecute("UPDATE identity.realm SET status='INACTIVE',updated_at=CURRENT_TIMESTAMP");adminDenied(ACCOUNT_NOT_ACCESSIBLE,()->app.getIdentityAccount(B,REQUEST));adminDenied(AUTHORITY_REQUIRED,()->app.listIdentityRoles(REQUEST));
        db.ownerExecute("UPDATE identity.realm SET status='ACTIVE',updated_at=CURRENT_TIMESTAMP");execute("INSERT INTO identity.account_grant(account_id,role_code,scope_type,scope_id,granted_at) VALUES ('"+A+"','OPERATOR','WAREHOUSE','"+X+"',CURRENT_TIMESTAMP)");adminDenied(AUTHORITY_REQUIRED,()->app.listIdentityScopes(REQUEST));
    }
    @Test void adminDisableAtomicallyRevokesTargetAndPreservesActor() throws Exception {
        sessionFixture();execute("INSERT INTO identity.refresh_token(id,session_id,token_hash,issued_at,expires_at) VALUES ('"+id(314)+"','"+AS+"',decode(repeat('03',32),'hex'),TIMESTAMPTZ '2026-10-21 00:00:00Z',TIMESTAMPTZ '2026-10-21 08:00:00Z')");
        var result=administration(S,A,null).disableIdentityAccount(adminCommand());assertEquals("DISABLED",result.account().status());assertEquals(2,result.appliedVersion());
        assertEquals(1,scalar("SELECT count(*) FROM identity.user_account WHERE id='"+B+"' AND version=2 AND auth_version=2 AND email_verified AND password_hash IS NOT NULL"));assertEquals(1,scalar("SELECT count(*) FROM identity.auth_session WHERE id='"+AS+"' AND revoked_at IS NOT NULL"));assertEquals(1,scalar("SELECT count(*) FROM identity.refresh_token WHERE id='"+id(314)+"' AND revoked_at IS NOT NULL"));assertEquals(1,scalar("SELECT count(*) FROM identity.auth_session WHERE id='"+S+"' AND revoked_at IS NULL"));
        assertEquals(1,scalar("SELECT count(*) FROM identity.audit_entry WHERE target_id='"+B+"' AND event_type='IDENTITY_ACCOUNT_DISABLED'"));
    }
    @Test void adminPendingDisablePreservesUnverifiedCredentialState() throws Exception {
        sessionFixture();var protection=outboxProtection();UUID target=secureProvision(protection).provisionIdentityAccount(provisionCommand()).account().id();administration(S,A,protection).disableIdentityAccount(adminCommand(target,1,KEY,"Synthetic disable"));
        assertEquals(1,scalar("SELECT count(*) FROM identity.user_account WHERE id='"+target+"' AND status='DISABLED' AND password_hash IS NULL AND NOT email_verified AND activated_at IS NULL AND version=2"));assertEquals(0,scalar("SELECT count(*) FROM identity.action_proof WHERE account_id='"+target+"' AND revoked_at IS NULL"));assertEquals(0,scalar("SELECT count(*) FROM identity.invitation_outbox WHERE account_id='"+target+"' AND ciphertext IS NOT NULL"));
    }
    @Test void adminReactivationNeverRestoresOldSessionsOrProofs() throws Exception {
        sessionFixture();var app=administration(S,A,null);app.disableIdentityAccount(adminCommand());var r=app.reactivateIdentityAccount(adminCommand(B,2,id(315),"Synthetic reactivate"));assertEquals("ACTIVE",r.account().status());assertEquals(3,r.account().version());assertEquals(1,scalar("SELECT count(*) FROM identity.user_account WHERE id='"+B+"' AND auth_version=3 AND email_verified AND password_hash IS NOT NULL AND disabled_at IS NULL"));assertEquals(1,scalar("SELECT count(*) FROM identity.auth_session WHERE id='"+AS+"' AND revoked_at IS NOT NULL"));
    }
    @Test void adminDisabledPendingCannotBeReactivated() throws Exception {
        sessionFixture();var protection=outboxProtection();UUID target=secureProvision(protection).provisionIdentityAccount(provisionCommand()).account().id();var app=administration(S,A,protection);app.disableIdentityAccount(adminCommand(target,1,KEY,"Synthetic disable"));adminDenied(INVALID_ACCOUNT_STATE,()->app.reactivateIdentityAccount(adminCommand(target,2,id(316),"Synthetic reactivate")));
    }
    @Test void adminSelfDisableDeniedWithoutAnyMutation() throws Exception {sessionFixture();adminDenied(SELF_MANAGEMENT_FORBIDDEN,()->administration(S,A,null).disableIdentityAccount(adminCommand(A,1,KEY,"Synthetic")));}
    @Test void adminCurrentAuthorityIsRequiredBeforeReceiptReplay() throws Exception {sessionFixture();var app=administration(S,A,null);app.disableIdentityAccount(adminCommand());execute("UPDATE identity.auth_session SET revoked_at=created_at WHERE id='"+S+"'");adminDenied(SESSION_INVALID,()->app.disableIdentityAccount(adminCommand()));}
    @Test void adminExactLifecycleReplayHasNoSecondEffect() throws Exception {sessionFixture();var app=administration(S,A,null);app.disableIdentityAccount(adminCommand());var before=secureSnapshot();var result=app.disableIdentityAccount(adminCommand());assertTrue(result.replayed());assertEquals(2,result.appliedVersion());assertEquals(before,secureSnapshot());}
    @Test void adminLifecycleChangedPayloadConflictsBeforeStaleVersion() throws Exception {
        sessionFixture();var app=administration(S,A,null);app.disableIdentityAccount(adminCommand());adminDenied(IDEMPOTENCY_CONFLICT,()->app.disableIdentityAccount(adminCommand(B,1,KEY,"Changed")));adminDenied(IDEMPOTENCY_CONFLICT,()->app.disableIdentityAccount(adminCommand(B,2,KEY,"Synthetic administration")));
    }
    @Test void adminOperationsUseSeparateReceiptNamespaces() throws Exception {sessionFixture();var app=administration(S,A,null);app.disableIdentityAccount(adminCommand());app.reactivateIdentityAccount(adminCommand(B,2,KEY,"Synthetic reactivate"));assertEquals(2,scalar("SELECT count(*) FROM identity.command_receipt WHERE actor_id='"+A+"' AND operation_key='"+KEY+"'"));}
    @Test void adminDisableAuditFailureRollsBackAllState() throws Exception {sessionFixture();failWrite("audit_entry","INSERT");var before=secureSnapshot();assertThrows(org.springframework.dao.DataAccessException.class,()->administration(S,A,null).disableIdentityAccount(adminCommand()));assertEquals(before,secureSnapshot());}
    @Test void adminDisableReceiptFailureRollsBackAllState() throws Exception {sessionFixture();failWrite("command_receipt","INSERT");var before=secureSnapshot();assertThrows(org.springframework.dao.DataAccessException.class,()->administration(S,A,null).disableIdentityAccount(adminCommand()));assertEquals(before,secureSnapshot());}
    @Test void adminDisableStateWriteFailureRollsBackAllState() throws Exception {sessionFixture();failWrite("user_account","UPDATE");var before=secureSnapshot();assertThrows(org.springframework.dao.DataAccessException.class,()->administration(S,A,null).disableIdentityAccount(adminCommand()));assertEquals(before,secureSnapshot());}
    @Test void adminDisableRevocationFailureRollsBackAllState() throws Exception {sessionFixture();failWrite("auth_session","UPDATE");var before=secureSnapshot();assertThrows(org.springframework.dao.DataAccessException.class,()->administration(S,A,null).disableIdentityAccount(adminCommand()));assertEquals(before,secureSnapshot());}
    @Test void adminAmbientCallerTransactionsCannotOwnResults() throws Exception {
        sessionFixture();var source=outboxDataSource();var app=ProductionIdentityAdministrationComposition.create(source,(op,request)->new com.elogistic.backend.identity.internal.application.port.CurrentSessionAdmission.Context(S,A,1,NOW,NOW.plusSeconds(900)),Clock.fixed(NOW,ZoneOffset.UTC),Duration.ofHours(24));
        var before=secureSnapshot();sessionCallerTransaction(source).executeWithoutResult(status->{sessionOwnershipRejected(()->app.getIdentityAccount(B,REQUEST));sessionOwnershipRejected(()->app.disableIdentityAccount(adminCommand()));assertFalse(status.isRollbackOnly());});assertEquals(before,secureSnapshot());
    }
    @Test void adminOppositeDisableConcurrencyKeepsOneActiveAdministrator() throws Exception {
        sessionFixture();Instant time;
        // Bootstrap helper uses database wall time; this test must share that clock domain.
        try(Connection c=db.runtimeConnection();Statement q=c.createStatement();ResultSet row=q.executeQuery("SELECT CURRENT_TIMESTAMP")){assertTrue(row.next());time=row.getTimestamp(1).toInstant();}
        try(Connection c=db.runtimeConnection();PreparedStatement q=c.prepareStatement("UPDATE identity.auth_session SET created_at=?,absolute_expires_at=?,last_activity_at=?,idle_expires_at=? WHERE id IN (?,?)")) {
            q.setTimestamp(1,Timestamp.from(time));q.setTimestamp(2,Timestamp.from(time.plusSeconds(28800)));q.setTimestamp(3,Timestamp.from(time));q.setTimestamp(4,Timestamp.from(time.plusSeconds(1800)));q.setObject(5,S);q.setObject(6,AS);assertEquals(2,q.executeUpdate());
        }
        var left=administration(S,A,null,time);var right=administration(AS,B,null,time);
        // Retire the third bootstrap fixture through the real authorized lifecycle first.
        left.disableIdentityAccount(adminCommand(adminBootstrapId(),1,id(320),"Synthetic bootstrap administrator handoff"));
        var result=adminRace(()->left.disableIdentityAccount(adminCommand()),()->right.disableIdentityAccount(adminCommand(A,1,KEY,"Synthetic opposite")));assertEquals(1,result.stream().filter(Result.class::isInstance).count());assertEquals(1,result.stream().filter(x->x==SESSION_INVALID).count());assertEquals(1,scalar("SELECT count(*) FROM identity.user_account a WHERE a.status='ACTIVE' AND EXISTS (SELECT 1 FROM identity.account_grant g WHERE g.account_id=a.id AND g.role_code='IDENTITY_MANAGEMENT')"));
    }
    @Test void adminConcurrentSameKeyHasOneLifecycleEffect() throws Exception {
        sessionFixture();var app=administration(S,A,null);var results=adminRace(()->app.disableIdentityAccount(adminCommand()),()->app.disableIdentityAccount(adminCommand()));assertEquals(2,results.stream().filter(Result.class::isInstance).count());assertEquals(1,results.stream().filter(x->x instanceof Result z && !z.replayed()).count());assertEquals(1,scalar("SELECT count(*) FROM identity.audit_entry WHERE target_id='"+B+"' AND event_type='IDENTITY_ACCOUNT_DISABLED'"));
    }
    @Test void adminWaitingMutationObservesConcurrentRealmDeactivation() throws Exception {
        sessionFixture();var pool=Executors.newSingleThreadExecutor();try(Connection c=db.migrationConnection();Statement q=c.createStatement()) {
            c.setAutoCommit(false);q.execute("SET ROLE "+db.owner());q.execute("SELECT pg_advisory_xact_lock("+0x4944454e54495459L+")");q.execute("UPDATE identity.realm SET status='INACTIVE',updated_at=CURRENT_TIMESTAMP");var ready=new CountDownLatch(1);var future=pool.submit(()->{ready.countDown();try{administration(S,A,null).disableIdentityAccount(adminCommand());return "ADMITTED";}catch(IdentityCommandFailure e){return e.code();}});assertTrue(ready.await(5,TimeUnit.SECONDS));assertThrows(TimeoutException.class,()->future.get(150,TimeUnit.MILLISECONDS));c.commit();assertEquals(ACCOUNT_NOT_ACCESSIBLE,future.get(15,TimeUnit.SECONDS));assertEquals(1,scalar("SELECT version FROM identity.user_account WHERE id='"+B+"'"));
        }finally{pool.shutdownNow();assertTrue(pool.awaitTermination(5,TimeUnit.SECONDS));}
    }
    @Test void adminResendQueuesProtectedReplacementAndKeepsPending() throws Exception {
        sessionFixture();var protection=outboxProtection();UUID target=secureProvision(protection).provisionIdentityAccount(provisionCommand()).account().id();administration(S,A,protection).resendIdentityInvitation(adminCommand(target,1,KEY,"Synthetic resend"));
        assertEquals(1,scalar("SELECT count(*) FROM identity.user_account WHERE id='"+target+"' AND status='PENDING_ACTIVATION' AND password_hash IS NULL AND NOT email_verified AND version=2 AND auth_version=2"));assertEquals(1,scalar("SELECT count(*) FROM identity.action_proof WHERE account_id='"+target+"' AND revoked_at IS NOT NULL"));assertEquals(1,scalar("SELECT count(*) FROM identity.action_proof WHERE account_id='"+target+"' AND revoked_at IS NULL AND consumed_at IS NULL"));assertEquals(1,scalar("SELECT count(*) FROM identity.invitation_outbox WHERE account_id='"+target+"' AND status='EXPIRED' AND ciphertext IS NULL"));
        var worker=outbox(protection,NOW);var lease=worker.claim().orElseThrow();try(var delivery=worker.open(lease)) {assertEquals(43,delivery.token().length);String token=new String(delivery.token(),java.nio.charset.StandardCharsets.US_ASCII);assertFalse(secureSnapshot().toString().contains(token));assertEquals("provision@example.invalid",delivery.destination());}
        assertEquals(0,scalar("SELECT count(*) FROM identity.auth_session WHERE account_id='"+target+"'"));
    }
    @Test void adminResendReplayAndConflictNeverRegenerateMaterial() throws Exception {
        sessionFixture();var protection=outboxProtection();UUID target=secureProvision(protection).provisionIdentityAccount(provisionCommand()).account().id();var app=administration(S,A,protection);var cmd=adminCommand(target,1,KEY,"Synthetic resend");app.resendIdentityInvitation(cmd);var before=secureSnapshot();app.resendIdentityInvitation(cmd);assertEquals(before,secureSnapshot());adminDenied(IDEMPOTENCY_CONFLICT,()->app.resendIdentityInvitation(adminCommand(target,1,KEY,"Changed")));assertEquals(2,scalar("SELECT count(*) FROM identity.action_proof WHERE account_id='"+target+"'"));
    }
    private void adminResendRollback(String table) throws Exception {
        sessionFixture();var protection=outboxProtection();UUID target=secureProvision(protection).provisionIdentityAccount(provisionCommand()).account().id();failWrite(table,"INSERT");var before=secureSnapshot();assertThrows(org.springframework.dao.DataAccessException.class,()->administration(S,A,protection).resendIdentityInvitation(adminCommand(target,1,KEY,"Synthetic resend")));assertEquals(before,secureSnapshot());
    }
    @Test void adminResendProofFailureRollsBackPriorRevocation() throws Exception {adminResendRollback("action_proof");}
    @Test void adminResendOutboxFailureRollsBackAllState() throws Exception {adminResendRollback("invitation_outbox");}
    @Test void adminResendAuditFailureRollsBackAllState() throws Exception {adminResendRollback("audit_entry");}
    @Test void adminResendReceiptFailureRollsBackAllState() throws Exception {adminResendRollback("command_receipt");}
    @Test void adminConcurrentResendKeyCreatesOneReplacement() throws Exception {
        sessionFixture();var protection=outboxProtection();UUID target=secureProvision(protection).provisionIdentityAccount(provisionCommand()).account().id();var app=administration(S,A,protection);var cmd=adminCommand(target,1,KEY,"Synthetic resend");var result=adminRace(()->{app.resendIdentityInvitation(cmd);return "DONE";},()->{app.resendIdentityInvitation(cmd);return "DONE";});assertEquals(List.of("DONE","DONE"),result);assertEquals(2,scalar("SELECT count(*) FROM identity.action_proof WHERE account_id='"+target+"'"));assertEquals(1,scalar("SELECT count(*) FROM identity.audit_entry WHERE target_id='"+target+"' AND event_type='IDENTITY_INVITATION_RESENT'"));
    }
    @Test void adminResendRejectsActiveAndDisabledPendingAccounts() throws Exception {
        sessionFixture();var protection=outboxProtection();var app=administration(S,A,protection);adminDenied(INVALID_ACCOUNT_STATE,()->app.resendIdentityInvitation(adminCommand()));UUID target=secureProvision(protection).provisionIdentityAccount(provisionCommand()).account().id();app.disableIdentityAccount(adminCommand(target,1,KEY,"Synthetic disable"));adminDenied(INVALID_ACCOUNT_STATE,()->app.resendIdentityInvitation(adminCommand(target,2,id(317),"Synthetic resend")));
    }
    @Test void adminMissingCryptoFailsBeforeProofOrAccountMutation() throws Exception {
        sessionFixture();UUID target=secureProvision(outboxProtection()).provisionIdentityAccount(provisionCommand()).account().id();var before=secureSnapshot();assertThrows(IllegalStateException.class,()->administration(S,A,null).resendIdentityInvitation(adminCommand(target,1,KEY,"Synthetic resend")));assertEquals(before,secureSnapshot());
    }
    @Test void adminUnsupportedTargetScopesRemainDeniedForAllCommands() throws Exception {
        sessionFixture();var app=administration(S,A,outboxProtection());adminDenied(ACCOUNT_NOT_ACCESSIBLE,()->app.disableIdentityAccount(adminCommand(T,1,KEY,"Synthetic")));adminDenied(ACCOUNT_NOT_ACCESSIBLE,()->app.reactivateIdentityAccount(adminCommand(T,1,KEY,"Synthetic")));adminDenied(ACCOUNT_NOT_ACCESSIBLE,()->app.resendIdentityInvitation(adminCommand(T,1,KEY,"Synthetic")));
    }
    @Test void adminConcurrentConflictingKeysNeverRepeatLifecycleEffect() throws Exception {
        sessionFixture();var app=administration(S,A,null);var result=adminRace(()->app.disableIdentityAccount(adminCommand()),()->app.disableIdentityAccount(adminCommand(B,1,KEY,"Changed reason")));assertEquals(1,result.stream().filter(Result.class::isInstance).count());assertEquals(1,result.stream().filter(x->x==IDEMPOTENCY_CONFLICT).count());assertEquals(2,scalar("SELECT version FROM identity.user_account WHERE id='"+B+"'"));
    }

    // Production HTTP read boundary. Same owned PostgreSQL lifecycle; no new container/volume.
    private static java.security.KeyPair httpKeys;
    private static synchronized java.security.KeyPair httpKeys() throws Exception {
        if(httpKeys==null){var generator=java.security.KeyPairGenerator.getInstance("RSA");generator.initialize(3072);httpKeys=generator.generateKeyPair();}return httpKeys;
    }
    @org.springframework.boot.test.context.TestConfiguration(proxyBeanMethods=false)
    @org.springframework.web.servlet.config.annotation.EnableWebMvc
    @org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
    static class HttpFixtureConfiguration {
        static javax.sql.DataSource source;
        @org.springframework.context.annotation.Bean javax.sql.DataSource dataSource(){return source;}
    }
    private org.springframework.web.context.support.AnnotationConfigWebApplicationContext httpContext(Instant time)throws Exception {
        HttpFixtureConfiguration.source=outboxDataSource();
        var key=new com.nimbusds.jose.jwk.RSAKey.Builder((java.security.interfaces.RSAPublicKey)httpKeys().getPublic())
            .privateKey((java.security.interfaces.RSAPrivateKey)httpKeys().getPrivate()).keyID("http-test")
            .keyUse(com.nimbusds.jose.jwk.KeyUse.SIGNATURE).algorithm(com.nimbusds.jose.JWSAlgorithm.RS256).build();
        var context=new org.springframework.web.context.support.AnnotationConfigWebApplicationContext();
        context.setServletContext(new org.springframework.mock.web.MockServletContext());context.getEnvironment().setActiveProfiles("identity","postgres");
        context.getEnvironment().getPropertySources().addFirst(new org.springframework.core.env.MapPropertySource("isolated-http-fixture",Map.of(
            "identity.security.issuer","https://identity.example.invalid","identity.security.audience","elogistics-browser",
            "identity.security.current-kid","http-test","identity.security.jwk-set",new com.nimbusds.jose.jwk.JWKSet(key).toString(false),
            "identity.security.origin","https://portal.example.invalid", "identity.security.limiter-hmac",Base64.getEncoder().encodeToString(limiterKey()))));
        context.addBeanFactoryPostProcessor(factory->factory.registerSingleton("identityHttpClock",Clock.fixed(time,ZoneOffset.UTC)));
        context.register(HttpFixtureConfiguration.class,com.elogistic.backend.identity.internal.ProductionIdentityHttpConfiguration.class,
            com.elogistic.backend.identity.internal.web.ProductionIdentityReads.class,com.elogistic.backend.identity.internal.web.ProductionIdentityErrors.class);
        try{context.refresh();return context;}catch(Exception failure){context.close();throw failure;}
    }
    private org.springframework.test.web.servlet.MockMvc httpMvc(org.springframework.web.context.support.AnnotationConfigWebApplicationContext context){
        return org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup(context)
            .addFilters(context.getBean("springSecurityFilterChain",jakarta.servlet.Filter.class)).build();
    }
    private String httpToken(org.springframework.web.context.support.AnnotationConfigWebApplicationContext context,UUID sid,UUID actor,long version,Instant issued,Instant expiry){
        return context.getBean(com.elogistic.backend.identity.internal.web.ProductionAccessTokens.class).issue(new com.elogistic.backend.identity.api.IdentitySessionCommands.AccessGrant(sid,actor,version,issued,expiry));
    }
    private org.springframework.mock.web.MockHttpServletResponse httpRead(org.springframework.test.web.servlet.MockMvc mvc,String path,String token)throws Exception{
        return mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(path).secure(true).header("Authorization","Bearer "+token)).andReturn().getResponse();
    }
    @Test void httpReadValidRs256UsesRealCurrentStateWithoutIdleWrites() throws Exception {
        sessionFixture();try(var context=httpContext(NOW)){var before=secureSnapshot();var r=httpRead(httpMvc(context),"/api/v1/auth/me",httpToken(context,S,A,1,NOW,NOW.plusSeconds(900)));
            assertEquals(200,r.getStatus());assertEquals("no-store",r.getHeader("Cache-Control"));assertEquals("\"v1\"",r.getHeader("ETag"));
            var body=new tools.jackson.databind.ObjectMapper().readTree(r.getContentAsString());assertEquals(A.toString(),body.get("id").asString());assertEquals(8,body.size());assertFalse(r.getContentAsString().contains("password"));assertFalse(r.getContentAsString().contains("accessToken"));assertEquals(before,secureSnapshot());}
    }
    @Test void httpReadIntegerJwtDatesResolveOnlyOwnedMicrosecondSession() throws Exception {
        sessionFixture();Instant time=NOW.plusNanos(123456000);try(var login=sessions(time).loginIdentity(new com.elogistic.backend.identity.api.IdentitySessionCommands.Login("fixture."+A,SESSION_PASSWORD,REQUEST));var context=httpContext(time)){
            var before=secureSnapshot();var token=context.getBean(com.elogistic.backend.identity.internal.web.ProductionAccessTokens.class).issue(login.access());
            assertEquals(200,httpRead(httpMvc(context),"/api/v1/auth/me",token).getStatus());assertEquals(before,secureSnapshot());}
    }
    @Test void httpReadAlreadySignedTokenRejectsRevocation() throws Exception {
        sessionFixture();try(var context=httpContext(NOW)){String token=httpToken(context,S,A,1,NOW,NOW.plusSeconds(900));execute("UPDATE identity.auth_session SET revoked_at=created_at WHERE id='"+S+"'");var before=secureSnapshot();assertEquals(401,httpRead(httpMvc(context),"/api/v1/auth/me",token).getStatus());assertEquals(before,secureSnapshot());}
    }
    @Test void httpReadAlreadySignedTokenRejectsChangedAuthVersion() throws Exception {
        sessionFixture();try(var context=httpContext(NOW)){String token=httpToken(context,S,A,1,NOW,NOW.plusSeconds(900));execute("UPDATE identity.user_account SET auth_version=auth_version+1 WHERE id='"+A+"'");var before=secureSnapshot();assertEquals(401,httpRead(httpMvc(context),"/api/v1/auth/me",token).getStatus());assertEquals(before,secureSnapshot());}
    }
    @Test void httpReadDisabledAndUnverifiedAccountsReject() throws Exception {
        sessionFixture();try(var context=httpContext(NOW)){String token=httpToken(context,S,A,1,NOW,NOW.plusSeconds(900));execute("UPDATE identity.user_account SET status='DISABLED',disabled_at=created_at WHERE id='"+A+"'");var before=secureSnapshot();assertEquals(401,httpRead(httpMvc(context),"/api/v1/auth/me",token).getStatus());assertEquals(before,secureSnapshot());execute("UPDATE identity.user_account SET status='PENDING_ACTIVATION',email_verified=FALSE,password_hash=NULL,activated_at=NULL,disabled_at=NULL WHERE id='"+A+"'");var pending=secureSnapshot();assertEquals(401,httpRead(httpMvc(context),"/api/v1/auth/me",token).getStatus());assertEquals(pending,secureSnapshot());}
    }
    @Test void httpReadInactiveRealmAndMixedActorReject() throws Exception {
        sessionFixture();try(var context=httpContext(NOW)){String token=httpToken(context,S,A,1,NOW,NOW.plusSeconds(900));
            execute("INSERT INTO identity.account_grant(account_id,role_code,scope_type,scope_id,granted_at) VALUES ('"+A+"','OPERATOR','WAREHOUSE','"+X+"',TIMESTAMPTZ '2026-10-21 00:00:00Z')");
            assertEquals(401,httpRead(httpMvc(context),"/api/v1/auth/me",token).getStatus());execute("DELETE FROM identity.account_grant WHERE account_id='"+A+"' AND scope_type='WAREHOUSE'");
            db.ownerExecute("UPDATE identity.realm SET status='INACTIVE',updated_at=CURRENT_TIMESTAMP");var before=secureSnapshot();assertEquals(401,httpRead(httpMvc(context),"/api/v1/auth/me",token).getStatus());assertEquals(before,secureSnapshot());}
    }
    @Test void httpReadSessionIdleAndAbsoluteExpiryRejectBeforeDisclosure() throws Exception {
        sessionFixture();try(var context=httpContext(NOW)){String token=httpToken(context,S,A,1,NOW,NOW.plusSeconds(900));execute("UPDATE identity.auth_session SET idle_expires_at=TIMESTAMPTZ '2026-10-21 00:05:00Z' WHERE id='"+S+"'");assertEquals(401,httpRead(httpMvc(context),"/api/v1/auth/me",token).getStatus());
            execute("UPDATE identity.auth_session SET absolute_expires_at=TIMESTAMPTZ '2026-10-21 00:05:00Z' WHERE id='"+S+"'");var before=secureSnapshot();assertEquals(401,httpRead(httpMvc(context),"/api/v1/auth/me",token).getStatus());assertEquals(before,secureSnapshot());}
    }
    @Test void httpReadForeignAndUnknownTargetsHaveSameMaskedContract() throws Exception {
        sessionFixture();try(var context=httpContext(NOW)){var mvc=httpMvc(context);String token=httpToken(context,S,A,1,NOW,NOW.plusSeconds(900));var before=secureSnapshot();var foreign=httpRead(mvc,"/api/v1/admin/users/"+T,token);var absent=httpRead(mvc,"/api/v1/admin/users/"+UUID.randomUUID(),token);assertEquals(404,foreign.getStatus());assertEquals(404,absent.getStatus());assertNull(foreign.getHeader("ETag"));assertNull(absent.getHeader("ETag"));var json=new tools.jackson.databind.ObjectMapper();assertEquals("ACCOUNT_NOT_ACCESSIBLE",json.readTree(foreign.getContentAsString()).get("code").asString());assertEquals("ACCOUNT_NOT_ACCESSIBLE",json.readTree(absent.getContentAsString()).get("code").asString());assertFalse(foreign.getContentAsString().contains("fixture@example.invalid"));assertEquals(before,secureSnapshot());}
    }
    @Test void httpReadListingsRolesAndScopesKeepWholeTargetIsolation() throws Exception {
        sessionFixture();try(var context=httpContext(NOW)){var mvc=httpMvc(context);String token=httpToken(context,S,A,1,NOW,NOW.plusSeconds(900));var before=secureSnapshot();var json=new tools.jackson.databind.ObjectMapper();var users=httpRead(mvc,"/api/v1/admin/users?limit=100",token);assertEquals(200,users.getStatus());var body=json.readTree(users.getContentAsString());assertEquals(2,body.size());assertFalse(users.getContentAsString().contains("fixture."+T));assertTrue(users.getContentAsString().contains("fixture."+B));var roles=httpRead(mvc,"/api/v1/admin/roles",token);assertEquals(200,roles.getStatus());assertEquals("IDENTITY_MANAGEMENT",json.readTree(roles.getContentAsString()).get("items").get(0).get("code").asString());var scopes=httpRead(mvc,"/api/v1/admin/scopes",token);assertEquals(200,scopes.getStatus());assertEquals(SCOPE.toString(),json.readTree(scopes.getContentAsString()).get("items").get(0).get("id").asString());assertEquals(before,secureSnapshot());}
    }
    @Test void httpReadWrongSessionOwnerFailsBeforeAuthoritativeView() throws Exception {
        sessionFixture();try(var context=httpContext(NOW)){String token=httpToken(context,S,B,1,NOW,NOW.plusSeconds(900));var before=secureSnapshot();assertEquals(401,httpRead(httpMvc(context),"/api/v1/auth/me",token).getStatus());assertEquals(before,secureSnapshot());}
    }
    @Test void httpReadFullChainRejectsPostAndAmbientTransaction() throws Exception {
        sessionFixture();try(var context=httpContext(NOW)){var mvc=httpMvc(context);String token=httpToken(context,S,A,1,NOW,NOW.plusSeconds(900));var before=secureSnapshot();assertEquals(401,mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/v1/admin/users").secure(true).header("Authorization","Bearer "+token)).andReturn().getResponse().getStatus());
            sessionCallerTransaction(outboxDataSource()).executeWithoutResult(status->{try{assertEquals(401,httpRead(mvc,"/api/v1/auth/me",token).getStatus());assertFalse(status.isRollbackOnly());}catch(Exception error){throw new IllegalStateException(error);}status.setRollbackOnly();});assertEquals(before,secureSnapshot());}
    }
    @Test void httpReadWaitingForLockObservesConcurrentRevocation() throws Exception {
        sessionFixture();try(var context=httpContext(NOW)){var mvc=httpMvc(context);String token=httpToken(context,S,A,1,NOW,NOW.plusSeconds(900));var pool=Executors.newSingleThreadExecutor();
            try(Connection c=db.runtimeConnection();Statement q=c.createStatement()){c.setAutoCommit(false);q.execute("SELECT pg_advisory_xact_lock("+0x4944454e54495459L+")");q.execute("UPDATE identity.auth_session SET revoked_at=created_at WHERE id='"+S+"'");var started=new CountDownLatch(1);var waiting=pool.submit(()->{started.countDown();return httpRead(mvc,"/api/v1/auth/me",token).getStatus();});assertTrue(started.await(5,TimeUnit.SECONDS));assertThrows(TimeoutException.class,()->waiting.get(150,TimeUnit.MILLISECONDS));c.commit();assertEquals(401,waiting.get(15,TimeUnit.SECONDS));}
            finally{pool.shutdownNow();assertTrue(pool.awaitTermination(5,TimeUnit.SECONDS));}}
    }


    private org.springframework.mock.web.MockHttpServletResponse httpMutation(org.springframework.test.web.servlet.MockMvc mvc,String verb,UUID target,String operation,String token,long version,UUID key,String body)throws Exception {
        var request="PUT".equals(verb)?org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put("/api/v1/admin/users/"+target+"/"+operation):org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/v1/admin/users/"+target+"/"+operation);
        return mvc.perform(request.secure(true).header("Authorization","Bearer "+token).header("If-Match","\"v"+version+"\"").header("Idempotency-Key",key).contentType("application/json").content(body)).andReturn().getResponse();
    }
    @Test void httpReadBearerDisableAndReplayKeepAtomicApplicationEffects()throws Exception {
        sessionFixture();try(var context=httpContext(NOW)){var mvc=httpMvc(context);String token=httpToken(context,S,A,1,NOW,NOW.plusSeconds(900));String body="{\"reason\":\"Synthetic HTTP disable\"}";assertEquals(200,httpMutation(mvc,"POST",B,"disable",token,1,KEY,body).getStatus());assertEquals(1,scalar("SELECT count(*) FROM identity.user_account WHERE id='"+B+"' AND status='DISABLED' AND version=2 AND auth_version=2"));assertEquals(1,scalar("SELECT count(*) FROM identity.auth_session WHERE id='"+AS+"' AND revoked_at IS NOT NULL"));assertEquals(1,scalar("SELECT count(*) FROM identity.auth_session WHERE id='"+S+"' AND last_activity_at=TIMESTAMPTZ '2026-10-21 00:05:00Z'"));var after=secureSnapshot();assertEquals(200,httpMutation(mvc,"POST",B,"disable",token,1,KEY,body).getStatus());assertEquals(after,secureSnapshot());}
    }
    @Test void httpReadBearerReactivationDoesNotRestoreTargetSessions()throws Exception {
        sessionFixture();administration(S,A,null).disableIdentityAccount(adminCommand());try(var context=httpContext(NOW)){assertEquals(200,httpMutation(httpMvc(context),"POST",B,"reactivate",httpToken(context,S,A,1,NOW,NOW.plusSeconds(900)),2,UUID.randomUUID(),"{\"reason\":\"Synthetic HTTP reactivation\"}").getStatus());assertEquals(1,scalar("SELECT count(*) FROM identity.user_account WHERE id='"+B+"' AND status='ACTIVE' AND version=3 AND auth_version=3"));assertEquals(1,scalar("SELECT count(*) FROM identity.auth_session WHERE id='"+AS+"' AND revoked_at IS NOT NULL"));}
    }
    @Test void httpReadGrantReplacementRequiresAuthoritativeWholeTarget()throws Exception {
        sessionFixture();String body="{\"reason\":\"Synthetic HTTP grants\",\"grants\":[{\"role\":\"IDENTITY_MANAGEMENT\",\"scopeType\":\"IDENTITY\",\"scopeId\":\""+SCOPE+"\"}]}";try(var context=httpContext(NOW)){var mvc=httpMvc(context);String token=httpToken(context,S,A,1,NOW,NOW.plusSeconds(900));assertEquals(200,httpMutation(mvc,"PUT",B,"grants",token,1,KEY,body).getStatus());assertEquals(1,scalar("SELECT count(*) FROM identity.user_account WHERE id='"+B+"' AND auth_version=2 AND version=2"));var after=secureSnapshot();assertEquals(404,httpMutation(mvc,"PUT",T,"grants",token,1,UUID.randomUUID(),body).getStatus());assertEquals(after,secureSnapshot());}
    }
    @Test void httpReadMutationFailuresNeverRecordActivityOrPartialCommit()throws Exception {
        sessionFixture();failWrite("audit_entry","INSERT");try(var context=httpContext(NOW)){var before=secureSnapshot();int status=httpMutation(httpMvc(context),"POST",B,"disable",httpToken(context,S,A,1,NOW,NOW.plusSeconds(900)),1,KEY,"{\"reason\":\"Synthetic failed mutation\"}").getStatus();assertEquals(401,status);assertEquals(before,secureSnapshot());}
    }
    @Test void httpReadMutationConflictAndStaleVersionHaveAcceptedCodes()throws Exception {
        sessionFixture();try(var context=httpContext(NOW)){var mvc=httpMvc(context);String token=httpToken(context,S,A,1,NOW,NOW.plusSeconds(900)),body="{\"reason\":\"Synthetic HTTP disable\"}";assertEquals(200,httpMutation(mvc,"POST",B,"disable",token,1,KEY,body).getStatus());var before=secureSnapshot();assertEquals(409,httpMutation(mvc,"POST",B,"disable",token,1,KEY,"{\"reason\":\"Changed reason\"}").getStatus());assertEquals(412,httpMutation(mvc,"POST",B,"reactivate",token,1,UUID.randomUUID(),body).getStatus());assertEquals(before,secureSnapshot());}
    }
    @Test void httpReadConcurrentSameKeyMutationHasOneEffect()throws Exception {
        sessionFixture();try(var context=httpContext(NOW)){var mvc=httpMvc(context);String token=httpToken(context,S,A,1,NOW,NOW.plusSeconds(900)),body="{\"reason\":\"Synthetic concurrent HTTP disable\"}";var statuses=adminRace(()->httpMutation(mvc,"POST",B,"disable",token,1,KEY,body).getStatus(),()->httpMutation(mvc,"POST",B,"disable",token,1,KEY,body).getStatus());assertEquals(List.of(200,200),statuses);assertEquals(1,scalar("SELECT count(*) FROM identity.audit_entry WHERE target_id='"+B+"' AND event_type='IDENTITY_ACCOUNT_DISABLED'"));}
    }

    private static byte[] limiterKey(){byte[] key=new byte[32];Arrays.fill(key,(byte)42);return key;}
    private JdbcIdentityRateLimiter limiter(){return new JdbcIdentityRateLimiter(outboxDataSource(),limiterKey());}
    private com.elogistic.backend.identity.internal.application.port.IdentityRateLimiter.Subject quota(com.elogistic.backend.identity.internal.application.port.IdentityRateLimiter.Policy p,String value){return new com.elogistic.backend.identity.internal.application.port.IdentityRateLimiter.Subject(p,value);}
    private List<com.elogistic.backend.identity.internal.application.port.IdentityRateLimiter.Subject> loginQuota(String account,String ip){return List.of(quota(com.elogistic.backend.identity.internal.application.port.IdentityRateLimiter.Policy.LOGIN_ACCOUNT,account),quota(com.elogistic.backend.identity.internal.application.port.IdentityRateLimiter.Policy.LOGIN_IP,ip));}
    @Test void authRateAllSixThresholdsAreEnforcedByPostgres()throws Exception{
        for(var p:com.elogistic.backend.identity.internal.application.port.IdentityRateLimiter.Policy.values()){
            var q=List.of(quota(p,p.name()));for(int i=0;i<p.limit;i++){var r=limiter().reserve(q);if(p==com.elogistic.backend.identity.internal.application.port.IdentityRateLimiter.Policy.LOGIN_ACCOUNT)r.finishLogin(false);}
            var e=assertThrows(com.elogistic.backend.identity.internal.application.port.IdentityRateLimiter.Exhausted.class,()->limiter().reserve(q));assertTrue(e.retryAfter>0&&e.retryAfter<=p.seconds);
        }
    }
    @Test void authRateSuccessfulLoginDoesNotConsumeFailedAccountBudget()throws Exception{for(int i=0;i<7;i++)limiter().reserve(loginQuota("same","same-ip")).finishLogin(true);assertEquals(7,scalar("SELECT count(*) FROM identity.rate_limit_attempt WHERE policy='LOGIN_IP' AND state='COUNTED'"));assertEquals(0,scalar("SELECT count(*) FROM identity.rate_limit_attempt WHERE policy='LOGIN_ACCOUNT' AND state<>'RELEASED'"));}
    @Test void authRateSuccessNeverErasesConcurrentFailedReservations()throws Exception{var a=limiter().reserve(loginQuota("same","ip"));var b=limiter().reserve(loginQuota("same","ip"));b.finishLogin(false);a.finishLogin(true);assertEquals(1,scalar("SELECT count(*) FROM identity.rate_limit_attempt WHERE policy='LOGIN_ACCOUNT' AND state='FAILED'"));}
    @Test void authRateMultiDimensionExhaustionIsAtomic()throws Exception{var p=com.elogistic.backend.identity.internal.application.port.IdentityRateLimiter.Policy.LOGIN_IP;for(int i=0;i<30;i++)limiter().reserve(List.of(quota(p,"full")));assertThrows(com.elogistic.backend.identity.internal.application.port.IdentityRateLimiter.Exhausted.class,()->limiter().reserve(loginQuota("new-account","full")));assertEquals(0,scalar("SELECT count(*) FROM identity.rate_limit_attempt WHERE policy='LOGIN_ACCOUNT'"));}
    @Test void authRateIndependentInstancesAndConcurrentReservationsCannotExceedFive()throws Exception{
        var pool=Executors.newFixedThreadPool(4);try{List<Future<Boolean>> jobs=new ArrayList<>();for(int i=0;i<12;i++)jobs.add(pool.submit(()->{try{limiter().reserve(loginQuota("raced","ip")).finishLogin(false);return true;}catch(com.elogistic.backend.identity.internal.application.port.IdentityRateLimiter.Exhausted e){return false;}}));int admitted=0;for(var job:jobs)if(job.get(20,TimeUnit.SECONDS))admitted++;assertEquals(5,admitted);assertEquals(5,scalar("SELECT count(*) FROM identity.rate_limit_attempt WHERE policy='LOGIN_ACCOUNT' AND state='FAILED'"));}finally{pool.shutdownNow();assertTrue(pool.awaitTermination(5,TimeUnit.SECONDS));}
    }
    @Test void authRateReversedDimensionOrderHasOneDeterministicLockOrder()throws Exception{var a=loginQuota("same","ip");var b=new ArrayList<>(a);Collections.reverse(b);var results=adminRace(()->{limiter().reserve(a).finishLogin(false);return 1;},()->{limiter().reserve(b).finishLogin(false);return 1;});assertEquals(List.of(1,1),results);assertEquals(4,scalar("SELECT count(*) FROM identity.rate_limit_attempt"));}
    @Test void authRateExpiredLeaseRemainsConservativelyCounted()throws Exception{for(int i=0;i<5;i++)limiter().reserve(loginQuota("same","ip"));execute("UPDATE identity.rate_limit_attempt SET created_at=statement_timestamp()-INTERVAL '61 seconds',lease_expires_at=statement_timestamp()-INTERVAL '1 second' WHERE policy='LOGIN_ACCOUNT'");assertThrows(com.elogistic.backend.identity.internal.application.port.IdentityRateLimiter.Exhausted.class,()->limiter().reserve(loginQuota("same","ip")));}
    @Test void authRateExpiredLeaseCannotReleaseQuotaOrIssueSuccess()throws Exception{var r=limiter().reserve(loginQuota("same","ip"));execute("UPDATE identity.rate_limit_attempt SET created_at=statement_timestamp()-INTERVAL '61 seconds',lease_expires_at=statement_timestamp()-INTERVAL '1 second' WHERE policy='LOGIN_ACCOUNT'");assertThrows(com.elogistic.backend.identity.internal.application.port.IdentityRateLimiter.Unavailable.class,()->r.finishLogin(true));assertEquals(1,scalar("SELECT count(*) FROM identity.rate_limit_attempt WHERE state='PENDING'"));}
    @Test void authRateRollingWindowExpiryRestoresAdmissionWithoutFixedBoundaryBurst()throws Exception{var q=loginQuota("same","ip");for(int i=0;i<5;i++)limiter().reserve(q).finishLogin(false);assertThrows(com.elogistic.backend.identity.internal.application.port.IdentityRateLimiter.Exhausted.class,()->limiter().reserve(q));execute("UPDATE identity.rate_limit_attempt SET created_at=statement_timestamp()-INTERVAL '901 seconds',lease_expires_at=statement_timestamp()-INTERVAL '841 seconds',expires_at=statement_timestamp()-INTERVAL '1 second'");limiter().reserve(q).finishLogin(false);assertEquals(2,scalar("SELECT count(*) FROM identity.rate_limit_attempt"));}
    @Test void authRateBusinessRollbackCannotUndoCommittedAccounting()throws Exception{try(Connection c=db.runtimeConnection();Statement s=c.createStatement()){c.setAutoCommit(false);s.execute("UPDATE identity.user_account SET display_name='Synthetic rollback' WHERE id='"+A+"'");limiter().reserve(loginQuota("same","ip")).finishLogin(false);c.rollback();}assertEquals(2,scalar("SELECT count(*) FROM identity.rate_limit_attempt"));assertEquals(0,scalar("SELECT count(*) FROM identity.user_account WHERE display_name='Synthetic rollback'"));}
    @Test void authRateRuntimeHasOnlyIntendedDmlAndNoOwnerDdl()throws Exception{limiter().reserve(loginQuota("same","ip")).finishLogin(false);for(String sql:List.of("ALTER TABLE identity.rate_limit_subject ADD COLUMN forbidden int","TRUNCATE identity.rate_limit_attempt","SET ROLE "+db.owner(),"SELECT * FROM platform.flyway_schema_history","DELETE FROM identity.audit_entry")){try(Connection c=db.runtimeConnection();Statement s=c.createStatement()){assertEquals("42501",assertThrows(SQLException.class,()->s.execute(sql)).getSQLState());}}}
    @Test void authRateStorageFailureNeverProducesReservation()throws Exception{var source=org.mockito.Mockito.mock(javax.sql.DataSource.class);org.mockito.Mockito.when(source.getConnection()).thenThrow(new SQLException("SYNTHETIC_UNAVAILABLE","08006"));assertThrows(com.elogistic.backend.identity.internal.application.port.IdentityRateLimiter.Unavailable.class,()->new JdbcIdentityRateLimiter(source,limiterKey()).reserve(loginQuota("same","ip")));assertEquals(0,scalar("SELECT count(*) FROM identity.rate_limit_attempt"));}
    @Test void authRateRetentionRemovesOnlyExpiredOwnedRows()throws Exception{limiter().reserve(loginQuota("expired","ip-old"));execute("UPDATE identity.rate_limit_attempt SET created_at=statement_timestamp()-INTERVAL '901 seconds',lease_expires_at=statement_timestamp()-INTERVAL '841 seconds',expires_at=statement_timestamp()-INTERVAL '1 second'");assertEquals(2,limiter().cleanup());limiter().reserve(loginQuota("live","ip-new"));assertEquals(0,limiter().cleanup());assertEquals(2,scalar("SELECT count(*) FROM identity.rate_limit_attempt"));assertEquals(3,scalar("SELECT count(*) FROM identity.user_account"));}
    @Test void authRateStoredIdentifiersAreHmacNotPlainSubjects()throws Exception{var q=loginQuota("private-subject","192.0.2.20");limiter().reserve(q);try(Connection c=db.runtimeConnection();PreparedStatement s=c.prepareStatement("SELECT subject_hash FROM identity.rate_limit_subject WHERE policy='LOGIN_ACCOUNT'");ResultSet r=s.executeQuery()){assertTrue(r.next());assertArrayEquals(limiter().identifier(q.getFirst()),r.getBytes(1));assertEquals(32,r.getBytes(1).length);}assertEquals(0,scalar("SELECT count(*) FROM information_schema.columns WHERE table_schema='identity' AND table_name LIKE 'rate_limit_%' AND column_name IN ('username','password','ip','token','secret')"));}
    @Test void authRateForwardV6PreservesEarlierMigrationChecksumsAndData()throws Exception{Database other=PostgresTestcontainersSupport.database(POSTGRES);assertEquals(5,other.flyway("5",SCOPE).migrate().migrationsExecuted);var before=Arrays.stream(other.flyway("5",SCOPE).info().applied()).map(m->m.getChecksum()).toList();assertEquals(1,other.flyway("6",SCOPE).migrate().migrationsExecuted);other.flyway("6",SCOPE).validate();assertEquals(0,other.flyway("6",SCOPE).migrate().migrationsExecuted);assertEquals(before,Arrays.stream(other.flyway("6",SCOPE).info().applied()).limit(5).map(m->m.getChecksum()).toList());}
    @Test void authBrowserBindingRejectsWrongFamilyRevocationAndExpiry()throws Exception{var browser=new JdbcIdentityBrowserState(outboxDataSource());var c=browser.rebind(null,S);browser.verify(c.cookie(),c.token(),S,false);assertThrows(IdentityCommandFailure.class,()->browser.verify(c.cookie(),c.token(),AS,false));assertThrows(IdentityCommandFailure.class,()->browser.verify(c.cookie(),"x".repeat(43),S,false));var replacement=browser.rebind(c.cookie(),null);assertThrows(IdentityCommandFailure.class,()->browser.verify(c.cookie(),c.token(),S,false));browser.verify(replacement.cookie(),replacement.token(),null,false);execute("UPDATE identity.csrf_binding SET expires_at=clock_timestamp()-INTERVAL '1 second',created_at=clock_timestamp()-INTERVAL '2 seconds'");assertThrows(IdentityCommandFailure.class,()->browser.verify(replacement.cookie(),replacement.token(),null,false));}
    @Test void authBrowserPreauthCannotBecomeSessionFamilyAuthority()throws Exception{var browser=new JdbcIdentityBrowserState(outboxDataSource());var c=browser.issue(null,null);browser.verify(c.cookie(),c.token(),null,true);assertThrows(IdentityCommandFailure.class,()->browser.verify(c.cookie(),c.token(),S,false));}
    private record BrowserFlow(jakarta.servlet.http.Cookie csrf,jakarta.servlet.http.Cookie refresh,String challenge){}
    private jakarta.servlet.http.Cookie responseCookie(org.springframework.mock.web.MockHttpServletResponse response,String name){for(String h:response.getHeaders("Set-Cookie"))if(h.startsWith(name+"="))return new jakarta.servlet.http.Cookie(name,h.substring(name.length()+1).split(";",2)[0]);throw new AssertionError("Expected cookie absent");}
    private BrowserFlow csrf(org.springframework.test.web.servlet.MockMvc mvc,BrowserFlow old)throws Exception{var request=org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/v1/auth/csrf").secure(true).header("Origin","https://portal.example.invalid");if(old!=null)request.cookie(old.csrf,old.refresh);var r=mvc.perform(request).andReturn().getResponse();assertEquals(200,r.getStatus());String challenge=new tools.jackson.databind.ObjectMapper().readTree(r.getContentAsString()).get("csrfToken").asString();return new BrowserFlow(responseCookie(r,"__Host-elogistics.csrf"),old==null?new jakarta.servlet.http.Cookie("__Host-elogistics.refresh",""):old.refresh,challenge);}
    private org.springframework.mock.web.MockHttpServletResponse authPost(org.springframework.test.web.servlet.MockMvc mvc,String route,BrowserFlow flow,String body)throws Exception{var request=org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/v1/auth/"+route).secure(true).header("Origin","https://portal.example.invalid").header("X-CSRF-Token",flow.challenge).cookie(flow.csrf);if(!flow.refresh.getValue().isEmpty())request.cookie(flow.refresh);if(body!=null)request.contentType("application/json").content(body);return mvc.perform(request).andReturn().getResponse();}
    private String loginBody(String password){return new tools.jackson.databind.ObjectMapper().writeValueAsString(Map.of("username","fixture."+A,"password",password));}
    private BrowserFlow loggedIn(org.springframework.test.web.servlet.MockMvc mvc)throws Exception{var pre=csrf(mvc,null);var r=authPost(mvc,"login",pre,loginBody(SESSION_PASSWORD));assertEquals(200,r.getStatus());return csrf(mvc,new BrowserFlow(responseCookie(r,"__Host-elogistics.csrf"),responseCookie(r,"__Host-elogistics.refresh"),"unused"));}
    @Test void authHttpLoginRefreshAndLogoutUseRealApplicationAndFilterChain()throws Exception{sessionFixture();try(var ctx=httpContext(NOW)){var mvc=httpMvc(ctx);var flow=loggedIn(mvc);long before=scalar("SELECT count(*) FROM identity.auth_session WHERE revoked_at IS NULL");var refresh=authPost(mvc,"refresh",flow,null);assertEquals(200,refresh.getStatus());String access=new tools.jackson.databind.ObjectMapper().readTree(refresh.getContentAsString()).get("accessToken").asString();assertEquals(200,httpRead(mvc,"/api/v1/auth/me",access).getStatus());var next=new BrowserFlow(responseCookie(refresh,"__Host-elogistics.csrf"),responseCookie(refresh,"__Host-elogistics.refresh"),flow.challenge);assertEquals(204,authPost(mvc,"logout",next,null).getStatus());assertEquals(before-1,scalar("SELECT count(*) FROM identity.auth_session WHERE revoked_at IS NULL"));assertEquals(401,httpRead(mvc,"/api/v1/auth/me",access).getStatus());}}
    @Test void authHttpRefreshReplayCommitsFamilyRevocation()throws Exception{sessionFixture();try(var ctx=httpContext(NOW)){var mvc=httpMvc(ctx);var flow=loggedIn(mvc);assertEquals(200,authPost(mvc,"refresh",flow,null).getStatus());var r=authPost(mvc,"refresh",flow,null);assertEquals(401,r.getStatus());assertTrue(r.getContentAsString().contains("REFRESH_REPLAY"));assertEquals(1,scalar("SELECT count(*) FROM identity.audit_entry WHERE event_type='IDENTITY_REFRESH_REPLAY'"));assertEquals(0,scalar("SELECT count(*) FROM identity.refresh_token WHERE account_id IS NOT NULL".replace("account_id IS NOT NULL","session_id IN (SELECT id FROM identity.auth_session WHERE revoked_at IS NOT NULL) AND revoked_at IS NULL")));}}
    @Test void authHttpRepeatedLogoutUsesFreshPreauthWithoutRestoringSessions()throws Exception{sessionFixture();try(var ctx=httpContext(NOW)){var mvc=httpMvc(ctx);var flow=loggedIn(mvc);assertEquals(204,authPost(mvc,"logout",flow,null).getStatus());long audit=scalar("SELECT count(*) FROM identity.audit_entry");assertEquals(204,authPost(mvc,"logout",csrf(mvc,null),null).getStatus());assertEquals(audit,scalar("SELECT count(*) FROM identity.audit_entry"));}}
    @Test void authHttpWrongPasswordExhaustsFiveFailuresWithGenericResponses()throws Exception{sessionFixture();try(var ctx=httpContext(NOW)){var mvc=httpMvc(ctx);var flow=csrf(mvc,null);for(int i=0;i<5;i++){var r=authPost(mvc,"login",flow,loginBody("wrong"));assertEquals(401,r.getStatus());assertTrue(r.getContentAsString().contains("INVALID_CREDENTIALS"));assertFalse(r.getContentAsString().contains("fixture."));}var r=authPost(mvc,"login",flow,loginBody(SESSION_PASSWORD));assertEquals(429,r.getStatus());assertNotNull(r.getHeader("Retry-After"));assertEquals(3,scalar("SELECT count(*) FROM identity.auth_session"));}}
    @Test void authHttpDisabledAccountAndInactiveRealmNeverIssueCredentials()throws Exception{sessionFixture();try(var ctx=httpContext(NOW)){var mvc=httpMvc(ctx);execute("UPDATE identity.user_account SET status='DISABLED',disabled_at=updated_at WHERE id='"+A+"'");var r=authPost(mvc,"login",csrf(mvc,null),loginBody(SESSION_PASSWORD));assertEquals(401,r.getStatus());assertTrue(r.getHeaders("Set-Cookie").isEmpty());execute("UPDATE identity.user_account SET status='ACTIVE',disabled_at=NULL WHERE id='"+A+"'");db.ownerExecute("UPDATE identity.realm SET status='INACTIVE',updated_at=clock_timestamp()");r=authPost(mvc,"login",csrf(mvc,null),loginBody(SESSION_PASSWORD));assertEquals(401,r.getStatus());assertEquals(3,scalar("SELECT count(*) FROM identity.auth_session"));}}
    @Test void authHttpCsrfCannotCrossFamiliesOrBypassOrigin()throws Exception{sessionFixture();try(var ctx=httpContext(NOW)){var mvc=httpMvc(ctx);var first=loggedIn(mvc);var second=loggedIn(mvc);var mixed=new BrowserFlow(first.csrf,second.refresh,first.challenge);long count=scalar("SELECT count(*) FROM identity.refresh_token");assertEquals(403,authPost(mvc,"refresh",mixed,null).getStatus());assertEquals(count,scalar("SELECT count(*) FROM identity.refresh_token"));var r=mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/v1/auth/logout").secure(true).header("Origin","https://evil.example.invalid").header("X-CSRF-Token",first.challenge).cookie(first.csrf,first.refresh)).andReturn().getResponse();assertEquals(403,r.getStatus());}}
    @Test void authHttpPollingAndRefreshNeverExtendSessionIdle()throws Exception{sessionFixture();try(var ctx=httpContext(NOW)){var mvc=httpMvc(ctx);var flow=loggedIn(mvc);long before=scalar("SELECT sum(EXTRACT(EPOCH FROM idle_expires_at)::bigint) FROM identity.auth_session");var r=authPost(mvc,"refresh",flow,null);String access=new tools.jackson.databind.ObjectMapper().readTree(r.getContentAsString()).get("accessToken").asString();assertEquals(200,httpRead(mvc,"/api/v1/auth/me",access).getStatus());assertEquals(before,scalar("SELECT sum(EXTRACT(EPOCH FROM idle_expires_at)::bigint) FROM identity.auth_session"));}}
    @Test void authHttpChangedAuthVersionRejectsRefreshAndAccess()throws Exception{sessionFixture();try(var ctx=httpContext(NOW)){var mvc=httpMvc(ctx);var flow=loggedIn(mvc);execute("UPDATE identity.user_account SET auth_version=auth_version+1 WHERE id='"+A+"'");assertEquals(401,authPost(mvc,"refresh",flow,null).getStatus());assertEquals(0,scalar("SELECT count(*) FROM identity.audit_entry WHERE event_type='IDENTITY_REFRESH'"));}}
    @Test void authHttpAuditFailureRollsBackLoginButKeepsLimiterReservation()throws Exception{sessionFixture();failWrite("audit_entry","INSERT");try(var ctx=httpContext(NOW)){var mvc=httpMvc(ctx);var r=authPost(mvc,"login",csrf(mvc,null),loginBody(SESSION_PASSWORD));assertEquals(503,r.getStatus());assertTrue(r.getHeaders("Set-Cookie").isEmpty());assertEquals(3,scalar("SELECT count(*) FROM identity.auth_session"));assertEquals(2,scalar("SELECT count(*) FROM identity.rate_limit_attempt"));}}


    @Test void authHttpConcurrentRefreshHasOneRotationAndDurableReplay()throws Exception{sessionFixture();try(var ctx=httpContext(NOW)){var mvc=httpMvc(ctx);var flow=loggedIn(mvc);var statuses=adminRace(()->authPost(mvc,"refresh",flow,null).getStatus(),()->authPost(mvc,"refresh",flow,null).getStatus());assertEquals(List.of(200,401),statuses.stream().sorted().toList());assertEquals(1,scalar("SELECT count(*) FROM identity.audit_entry WHERE event_type='IDENTITY_REFRESH_REPLAY'"));assertEquals(1,scalar("SELECT count(*) FROM identity.auth_session WHERE account_id='"+A+"' AND revoked_at IS NOT NULL"));}}
    @Test void authHttpLimiterFailureIs503BeforeSessionCreation()throws Exception{sessionFixture();failWrite("rate_limit_attempt","INSERT");try(var ctx=httpContext(NOW)){var mvc=httpMvc(ctx);var r=authPost(mvc,"login",csrf(mvc,null),loginBody(SESSION_PASSWORD));assertEquals(503,r.getStatus());assertTrue(r.getHeaders("Set-Cookie").isEmpty());assertTrue(r.getContentAsString().contains("SERVICE_UNAVAILABLE"));assertFalse(r.getContentAsString().contains("SYNTHETIC_WRITE_FAILURE"));assertEquals(3,scalar("SELECT count(*) FROM identity.auth_session"));}}
    @Test void authRateRowContentionTimesOutWithoutPermissiveFallback()throws Exception{var q=loginQuota("same","ip");limiter().reserve(q).finishLogin(true);try(Connection c=db.runtimeConnection();Statement s=c.createStatement()){c.setAutoCommit(false);s.execute("SELECT * FROM identity.rate_limit_subject FOR UPDATE");long start=System.nanoTime();assertThrows(com.elogistic.backend.identity.internal.application.port.IdentityRateLimiter.Unavailable.class,()->limiter().reserve(q));assertTrue(Duration.ofNanos(System.nanoTime()-start).compareTo(Duration.ofSeconds(5))<0);c.rollback();}limiter().reserve(q).finishLogin(true);assertEquals(2,scalar("SELECT count(*) FROM identity.rate_limit_attempt WHERE policy='LOGIN_IP'"));}
    @Test void authHttpRefreshEnforcesThirtyPerSessionWithoutIdleExtension()throws Exception{sessionFixture();try(var ctx=httpContext(NOW)){var mvc=httpMvc(ctx);var flow=loggedIn(mvc);for(int i=0;i<30;i++){var r=authPost(mvc,"refresh",flow,null);assertEquals(200,r.getStatus());flow=new BrowserFlow(responseCookie(r,"__Host-elogistics.csrf"),responseCookie(r,"__Host-elogistics.refresh"),flow.challenge);}var r=authPost(mvc,"refresh",flow,null);assertEquals(429,r.getStatus());assertNotNull(r.getHeader("Retry-After"));assertEquals(30,scalar("SELECT count(*) FROM identity.audit_entry WHERE event_type='IDENTITY_REFRESH'"));assertEquals(0,scalar("SELECT count(*) FROM identity.auth_session WHERE last_activity_at<>created_at"));}}


    @Test void authRateDatabaseConstraintsRejectBypassStatesAndInvalidDurations()throws Exception{
        limiter().reserve(loginQuota("synthetic","ip"));
        for(String sql:List.of("UPDATE identity.rate_limit_attempt SET state='BYPASS'","UPDATE identity.rate_limit_attempt SET expires_at=created_at","UPDATE identity.rate_limit_attempt SET lease_expires_at=created_at+INTERVAL '61 seconds'","INSERT INTO identity.rate_limit_subject(policy,subject_hash) VALUES ('LOGIN_IP',decode('01','hex'))"))
            try(Connection c=db.runtimeConnection();Statement s=c.createStatement()){assertEquals("23514",assertThrows(SQLException.class,()->s.execute(sql)).getSQLState());}
        assertEquals(2,scalar("SELECT count(*) FROM identity.rate_limit_attempt"));
    }

    // Spec003 customer-purpose persistence/admission. Test-only admission ports here do not prove HTTP/TLS.
    private static final String CUSTOMER_EMAIL="customer@example.invalid";
    private void customerFixture(boolean keepSystemGrant)throws Exception {
        sessionFixture();
        try(Connection c=db.runtimeConnection();PreparedStatement q=c.prepareStatement("UPDATE identity.user_account SET email=? WHERE id=?")) {
            q.setString(1,CUSTOMER_EMAIL);q.setObject(2,A);assertEquals(1,q.executeUpdate());
        }
        execute("INSERT INTO identity.customer_identifier VALUES ('"+id(901)+"','"+A+"','"+CUSTOMER_EMAIL+"',TIMESTAMPTZ '2026-10-21 00:00:00Z','ACTIVE',1)");
        if(!keepSystemGrant)execute("DELETE FROM identity.account_grant WHERE account_id='"+A+"'");
    }
    private com.elogistic.backend.identity.api.IdentitySessionCommands customerSessions(Instant time) {
        return com.elogistic.backend.identity.internal.application.IdentitySessions.customer(
            new JdbcIdentitySessionStore(outboxDataSource(),IdentitySessionStorage.CUSTOMER),new ProtectedSessionCredentials(),
            (operation,request,session,present)->{},Clock.fixed(time,ZoneOffset.UTC));
    }
    private com.elogistic.backend.identity.api.IdentitySessionCommands.Result customerLogin() {
        return customerSessions(NOW).loginIdentity(new com.elogistic.backend.identity.api.IdentitySessionCommands.Login(CUSTOMER_EMAIL,SESSION_PASSWORD,REQUEST));
    }
    @Test void customerLoginUsesIndependentVerifiedBindingWithoutSystemGrant()throws Exception {
        customerFixture(false);
        try(var result=customerLogin()) {
            assertEquals(A,result.access().accountId());assertTrue(result.user().grants().isEmpty());
            assertEquals(1,scalar("SELECT count(*) FROM identity.customer_auth_session"));
            assertEquals(1,scalar("SELECT count(*) FROM identity.customer_refresh_token WHERE octet_length(token_hash)=32"));
            assertEquals(3,scalar("SELECT count(*) FROM identity.auth_session"));
        }
        sessionDenied(INVALID_CREDENTIALS,()->sessions(NOW).loginIdentity(new com.elogistic.backend.identity.api.IdentitySessionCommands.Login("fixture."+A,SESSION_PASSWORD,REQUEST)));
    }
    @Test void customerExactEmailAndCurrentSecurityMasterPreventStaleIndexAdmission()throws Exception {
        customerFixture(false);
        sessionDenied(INVALID_CREDENTIALS,()->customerSessions(NOW).loginIdentity(new com.elogistic.backend.identity.api.IdentitySessionCommands.Login("Customer@example.invalid",SESSION_PASSWORD,REQUEST)));
        execute("UPDATE identity.user_account SET email='changed@example.invalid' WHERE id='"+A+"'");
        sessionDenied(INVALID_CREDENTIALS,()->customerLogin());assertEquals(0,scalar("SELECT count(*) FROM identity.customer_auth_session"));
    }
    @Test void dualPurposeAccountRefreshCannotCrossCustomerAndSystemTables()throws Exception {
        customerFixture(true);
        try(var customer=customerLogin();var system=sessionLogin()) {
            byte[] customerRaw=customer.refreshToken(),systemRaw=system.refreshToken();
            try(var customerCommand=sessionToken(customerRaw);var systemCommand=sessionToken(systemRaw)) {
                sessionDenied(SESSION_INVALID,()->sessions(NOW).refreshIdentity(customerCommand));
                sessionDenied(SESSION_INVALID,()->customerSessions(NOW).refreshIdentity(systemCommand));
            }finally {Arrays.fill(customerRaw,(byte)0);Arrays.fill(systemRaw,(byte)0);}
            assertEquals(1,scalar("SELECT count(*) FROM identity.customer_auth_session WHERE revoked_at IS NULL"));
            assertEquals(4,scalar("SELECT count(*) FROM identity.auth_session WHERE revoked_at IS NULL"));
        }
    }
    @Test void customerRefreshReplayCommitsOnlyCustomerFamilyRevocation()throws Exception {
        customerFixture(true);
        try(var login=customerLogin()) {
            byte[] raw=login.refreshToken();
            try(var first=sessionToken(raw);var replay=sessionToken(raw);var next=customerSessions(NOW.plusSeconds(1)).refreshIdentity(first)) {
                var systemBefore=scalar("SELECT count(*) FROM identity.auth_session WHERE revoked_at IS NULL");
                var denial=assertThrows(IdentityCommandFailure.class,()->customerSessions(NOW.plusSeconds(2)).refreshIdentity(replay));
                assertEquals(REFRESH_REPLAY,denial.code());
                assertEquals(REFRESH_REPLAY.name(),denial.getMessage());
                assertEquals(1,scalar("SELECT count(*) FROM identity.customer_auth_session WHERE revoked_at IS NOT NULL"));
                assertEquals(2,scalar("SELECT count(*) FROM identity.customer_refresh_token WHERE revoked_at IS NOT NULL"));
                assertEquals(0,scalar("SELECT count(*) FROM identity.auth_session WHERE revoked_at IS NOT NULL"));
                assertEquals(systemBefore,scalar("SELECT count(*) FROM identity.auth_session WHERE revoked_at IS NULL"));
                assertEquals(1,scalar("SELECT count(*) FROM identity.audit_entry WHERE event_type='CUSTOMER_REFRESH_REPLAY' AND source='IDENTITY_CUSTOMER' AND actor_id='"+A+"'"));
                try(var newest=sessionToken(next.refreshToken())) {
                    sessionDenied(SESSION_INVALID,()->customerSessions(NOW.plusSeconds(3)).refreshIdentity(newest));
                }
            }finally {Arrays.fill(raw,(byte)0);}
        }
    }
    @Test void identityVersionAndDisableInvalidateCustomerAdmissionImmediately()throws Exception {
        customerFixture(false);
        try(var login=customerLogin()) {
            byte[] raw=login.refreshToken();try(var command=sessionToken(raw)) {
                execute("UPDATE identity.user_account SET auth_version=auth_version+1,version=version+1 WHERE id='"+A+"'");
                sessionDenied(SESSION_INVALID,()->customerSessions(NOW.plusSeconds(1)).refreshIdentity(command));
            }finally {Arrays.fill(raw,(byte)0);}
        }
        try(var login=customerLogin()) {
            byte[] raw=login.refreshToken();try(var command=sessionToken(raw)) {
                execute("UPDATE identity.user_account SET status='DISABLED',disabled_at=updated_at WHERE id='"+A+"'");
                sessionDenied(SESSION_INVALID,()->customerSessions(NOW.plusSeconds(1)).refreshIdentity(command));
            }finally {Arrays.fill(raw,(byte)0);}
        }
    }
    @Test void customerCsrfHashesAndFamilyBindingCannotCrossSystemStore()throws Exception {
        customerFixture(true);
        var source=outboxDataSource();var customer=new JdbcIdentityBrowserState(source,IdentitySessionStorage.CUSTOMER);var system=new JdbcIdentityBrowserState(source);
        try(var login=customerLogin()) {
            var challenge=customer.rebind(null,login.access().sessionId());
            customer.verify(challenge.cookie(),challenge.token(),login.access().sessionId(),false);
            sessionDenied(CSRF_REJECTED,()->system.verify(challenge.cookie(),challenge.token(),login.access().sessionId(),false));
            var old=system.rebind(null,S);sessionDenied(CSRF_REJECTED,()->customer.verify(old.cookie(),old.token(),S,false));
            assertEquals(1,scalar("SELECT count(*) FROM identity.customer_csrf_binding WHERE octet_length(cookie_hash)=32 AND octet_length(challenge_hash)=32"));
        }
    }
    private com.elogistic.backend.identity.internal.application.CustomerAdmissions customerAdmission(
        javax.sql.DataSource source,com.elogistic.backend.identity.api.IdentitySessionCommands.AccessGrant access,Clock clock) {
        return new com.elogistic.backend.identity.internal.application.CustomerAdmissions(
            new JdbcIdentitySessionStore(source,IdentitySessionStorage.CUSTOMER),token->new com.elogistic.backend.identity.internal.application.port.CustomerAccessVerifier.Claim(
                access.sessionId(),access.accountId(),access.authVersion(),access.issuedAt(),access.expiresAt()),(request,session)->{},clock);
    }
    @Test void customerFinalDeadlineRollsBackStagedWritesInOwnedTransaction()throws Exception {
        customerFixture(false);try(var login=customerLogin()) {
            var time=new java.util.concurrent.atomic.AtomicReference<>(NOW);Clock clock=new Clock(){public ZoneId getZone(){return ZoneOffset.UTC;}public Clock withZone(ZoneId z){return this;}public Instant instant(){return time.get();}};
            var source=outboxDataSource();var app=customerAdmission(source,login.access(),clock);var jdbc=new org.springframework.jdbc.core.JdbcTemplate(source);
            sessionDenied(SESSION_INVALID,()->app.execute("synthetic-verified-claim",REQUEST,Set.of(),context->{
                jdbc.update("INSERT INTO identity.audit_entry(id,actor_id,target_id,event_type,source,reason,request_id,occurred_at) VALUES (?,?,?,'CUSTOMER_TEST_STAGED','SPEC003_TEST','Synthetic staged effect',?,?)",UUID.randomUUID(),A,A,REQUEST.toString(),Timestamp.from(NOW));
                time.set(NOW.plusSeconds(900));return null;
            }));
            assertEquals(0,scalar("SELECT count(*) FROM identity.audit_entry WHERE event_type='CUSTOMER_TEST_STAGED'"));
        }
    }
    @Test void customerMeaningfulActivityCommitsAfterBusinessAndContextCannotEscape()throws Exception {
        customerFixture(false);try(var login=customerLogin()) {
            var source=outboxDataSource();var app=customerAdmission(source,login.access(),Clock.fixed(NOW.plusSeconds(10),ZoneOffset.UTC));
            var captured=app.execute("synthetic-verified-claim",REQUEST,Set.of(),context->{app.requireCurrent(context);app.meaningfulAction(context);return context;});
            sessionDenied(SESSION_INVALID,()->app.requireCurrent(captured));
            assertEquals(1,scalar("SELECT count(*) FROM identity.customer_auth_session WHERE last_activity_at=TIMESTAMPTZ '2026-10-21 00:05:10Z'"));
            assertEquals(0,scalar("SELECT count(*) FROM identity.auth_session WHERE last_activity_at<>created_at"));
        }
    }

    // Enrollment application/PG evidence only. This fixture deliberately does not stand in for HTTP/TLS or SMTP.
    private static final UUID ENROLLMENT_BINDING=id(902);
    private static final String ENROLLMENT_EMAIL="new-customer@example.invalid",ENROLLMENT_PASSWORD="synthetic-enrollment-password";
    private ProtectedCustomerProofs enrollmentProtection(){return new ProtectedCustomerProofs(Map.of("v1",keyBytes(71)),"v1",Map.of("e1",keyBytes(72)),"e1");}
    private byte[] keyBytes(int value){byte[] bytes=new byte[32];Arrays.fill(bytes,(byte)value);return bytes;}
    private void enrollmentBinding()throws Exception {
        execute("INSERT INTO identity.customer_csrf_binding VALUES ('"+ENROLLMENT_BINDING+"',decode(repeat('71',32),'hex'),decode(repeat('72',32),'hex'),NULL,clock_timestamp(),clock_timestamp()+INTERVAL '15 minutes',NULL)");
    }
    private com.elogistic.backend.identity.api.customer.CustomerEnrollmentCommands enrollmentApp(Instant time){
        return new com.elogistic.backend.identity.internal.application.CustomerEnrollments(new JdbcCustomerEnrollmentStore(outboxDataSource()),enrollmentProtection(),
            new com.elogistic.backend.identity.internal.application.port.CustomerEnrollmentAdmission(){
                public Binding admit(Operation operation,UUID request,String exact){return new Binding(ENROLLMENT_BINDING);}
                public void requireCurrent(Binding binding){assertEquals(ENROLLMENT_BINDING,binding.id());}
            },Clock.fixed(time,ZoneOffset.UTC));
    }
    private com.elogistic.backend.identity.api.customer.CustomerEnrollmentCommands.Enrollment enrollCommand(UUID key,String email){
        return new com.elogistic.backend.identity.api.customer.CustomerEnrollmentCommands.Enrollment(email,ENROLLMENT_PASSWORD,"Synthetic Customer","S003-2026-10-27",key,REQUEST);
    }
    private com.elogistic.backend.identity.api.customer.CustomerEnrollmentCommands.Verification pendingEnrollmentProof()throws Exception {
        var source=outboxDataSource();var jdbc=new org.springframework.jdbc.core.JdbcTemplate(source);
        return jdbc.queryForObject("SELECT p.*,d.encryption_key_id,d.nonce,d.ciphertext,d.recipient_nonce,d.recipient_ciphertext FROM identity.customer_proof p JOIN identity.customer_delivery d ON d.proof_id=p.id WHERE p.purpose='CUSTOMER_ENROLLMENT' AND p.revoked_at IS NULL AND p.consumed_at IS NULL",(r,n)->{
            var p=new CustomerProof(r.getObject("id",UUID.class),r.getObject("subject_id",UUID.class),CustomerProof.Purpose.CUSTOMER_ENROLLMENT,r.getTimestamp("created_at").toInstant(),r.getTimestamp("expires_at").toInstant(),null,null,null,null,null);
            var crypto=enrollmentProtection();String destination=crypto.openDestination(p,r.getString("verifier_key_id"),r.getString("encryption_key_id"),r.getBytes("recipient_nonce"),r.getBytes("recipient_ciphertext"),p.createdAt());
            assertEquals(ENROLLMENT_EMAIL,destination);
            try(var opened=crypto.open(p,destination,r.getString("verifier_key_id"),r.getString("encryption_key_id"),r.getBytes("nonce"),r.getBytes("ciphertext"),p.createdAt())){
                byte[] token=opened.token();try{return new com.elogistic.backend.identity.api.customer.CustomerEnrollmentCommands.Verification(p.id(),new String(token,java.nio.charset.StandardCharsets.US_ASCII),UUID.randomUUID(),REQUEST);}
                finally{Arrays.fill(token,(byte)0);}
            }
        });
    }
    @Test void enrollmentQueuesOnlyProtectedMaterialWithoutAccountOrGrant()throws Exception {
        enrollmentBinding();var before=snapshot();var result=enrollmentApp(NOW).enroll(enrollCommand(KEY,ENROLLMENT_EMAIL));
        assertTrue(result.accepted());assertEquals(REQUEST,result.requestId());
        assertEquals(3,scalar("SELECT count(*) FROM identity.user_account"));assertEquals(0,scalar("SELECT count(*) FROM identity.customer_identifier"));
        assertEquals(1,scalar("SELECT count(*) FROM identity.customer_enrollment WHERE status='PENDING' AND octet_length(protected_password)=60"));
        assertEquals(1,scalar("SELECT count(*) FROM identity.customer_proof WHERE purpose='CUSTOMER_ENROLLMENT' AND expires_at=created_at+INTERVAL '24 hours' AND octet_length(verifier)=32"));
        assertEquals(1,scalar("SELECT count(*) FROM identity.customer_delivery WHERE status='QUEUED' AND attempts=0 AND accepted_at IS NULL AND delivered_at IS NULL AND octet_length(ciphertext)=59 AND recipient_ciphertext IS NOT NULL"));
        assertEquals(before.get("account_grant"),snapshot().get("account_grant"));
        assertNotNull(pendingEnrollmentProof());
    }
    @Test void enrollmentUniformReceiptAndChangedBindingPreventEnumerationAndDuplicates()throws Exception {
        enrollmentBinding();var app=enrollmentApp(NOW);var first=app.enroll(enrollCommand(KEY,ENROLLMENT_EMAIL));
        assertEquals(first,app.enroll(enrollCommand(KEY,ENROLLMENT_EMAIL)));
        assertTrue(app.enroll(enrollCommand(id(903),ENROLLMENT_EMAIL)).accepted());
        assertTrue(app.enroll(enrollCommand(id(904),"fixture@example.invalid")).accepted());
        assertEquals(IDEMPOTENCY_CONFLICT,assertThrows(IdentityCommandFailure.class,()->app.enroll(enrollCommand(KEY,"other@example.invalid"))).code());
        assertEquals(1,scalar("SELECT count(*) FROM identity.customer_enrollment"));assertEquals(1,scalar("SELECT count(*) FROM identity.customer_delivery"));
        assertEquals(3,scalar("SELECT count(*) FROM identity.customer_command_receipt"));
    }
    @Test void verificationAtomicallyCreatesVerifiedPersonalAccountWithoutSystemAuthority()throws Exception {
        enrollmentBinding();var app=enrollmentApp(NOW);app.enroll(enrollCommand(KEY,ENROLLMENT_EMAIL));var command=pendingEnrollmentProof();
        enrollmentApp(NOW.plusSeconds(1)).verify(command);
        assertEquals(1,scalar("SELECT count(*) FROM identity.customer_enrollment WHERE status='CONSUMED' AND protected_password IS NULL"));
        assertEquals(1,scalar("SELECT count(*) FROM identity.user_account WHERE email='"+ENROLLMENT_EMAIL+"' AND status='ACTIVE' AND email_verified AND password_hash IS NOT NULL"));
        assertEquals(1,scalar("SELECT count(*) FROM identity.customer_identifier WHERE exact_email='"+ENROLLMENT_EMAIL+"' AND status='ACTIVE'"));
        assertEquals(0,scalar("SELECT count(*) FROM identity.account_grant g JOIN identity.user_account a ON a.id=g.account_id WHERE a.email='"+ENROLLMENT_EMAIL+"'"));
        assertEquals(1,scalar("SELECT count(*) FROM identity.customer_delivery WHERE status='DELIVERED' AND delivery_evidence IS NOT NULL AND nonce IS NULL AND ciphertext IS NULL AND recipient_ciphertext IS NULL AND accepted_at IS NULL"));
        try(var login=customerSessions(NOW.plusSeconds(2)).loginIdentity(new com.elogistic.backend.identity.api.IdentitySessionCommands.Login(ENROLLMENT_EMAIL,ENROLLMENT_PASSWORD,REQUEST))){assertTrue(login.user().grants().isEmpty());}
        var after=snapshot();enrollmentApp(NOW.plusSeconds(3)).verify(command);assertEquals(after,snapshot());
        var replay=new com.elogistic.backend.identity.api.customer.CustomerEnrollmentCommands.Verification(command.proofId(),command.proof(),id(905),REQUEST);
        assertEquals(TOKEN_INVALID_OR_EXPIRED,assertThrows(IdentityCommandFailure.class,()->enrollmentApp(NOW.plusSeconds(4)).verify(replay)).code());
    }
    @Test void resendRevokesOldProofAndCiphertextBeforeCreatingFreshIntent()throws Exception {
        enrollmentBinding();enrollmentApp(NOW).enroll(enrollCommand(KEY,ENROLLMENT_EMAIL));var old=pendingEnrollmentProof();
        enrollmentApp(NOW.plusSeconds(60)).resend(new com.elogistic.backend.identity.api.customer.CustomerEnrollmentCommands.Email(ENROLLMENT_EMAIL,id(906),REQUEST));
        assertEquals(1,scalar("SELECT count(*) FROM identity.customer_proof WHERE revoked_at IS NOT NULL"));
        assertEquals(1,scalar("SELECT count(*) FROM identity.customer_delivery WHERE status='EXPIRED' AND ciphertext IS NULL AND recipient_ciphertext IS NULL"));
        assertEquals(TOKEN_INVALID_OR_EXPIRED,assertThrows(IdentityCommandFailure.class,()->enrollmentApp(NOW.plusSeconds(61)).verify(old)).code());
        enrollmentApp(NOW.plusSeconds(61)).verify(pendingEnrollmentProof());assertEquals(4,scalar("SELECT count(*) FROM identity.user_account"));
    }
    @Test void wrongFutureAndExpiredEnrollmentProofsCannotCreateAccount()throws Exception {
        enrollmentBinding();enrollmentApp(NOW).enroll(enrollCommand(KEY,ENROLLMENT_EMAIL));var good=pendingEnrollmentProof();
        var wrong=new com.elogistic.backend.identity.api.customer.CustomerEnrollmentCommands.Verification(good.proofId(),"A".repeat(43),id(907),REQUEST);
        for(var pair:List.of(Map.entry(NOW,wrong),Map.entry(NOW.minusSeconds(1),good),Map.entry(NOW.plusSeconds(86400),good)))
            assertEquals(TOKEN_INVALID_OR_EXPIRED,assertThrows(IdentityCommandFailure.class,()->enrollmentApp(pair.getKey()).verify(pair.getValue())).code());
        assertEquals(3,scalar("SELECT count(*) FROM identity.user_account"));assertEquals(0,scalar("SELECT count(*) FROM identity.customer_identifier"));
        assertEquals(0,scalar("SELECT count(*) FROM identity.customer_proof WHERE consumed_at IS NOT NULL"));
        enrollmentApp(NOW.plusSeconds(86400)).enroll(enrollCommand(id(908),ENROLLMENT_EMAIL));
        assertEquals(1,scalar("SELECT count(*) FROM identity.customer_enrollment WHERE status='EXPIRED' AND protected_password IS NULL"));
        enrollmentApp(NOW.plusSeconds(86401)).verify(pendingEnrollmentProof());assertEquals(4,scalar("SELECT count(*) FROM identity.user_account"));
    }
    @Test void verificationAuditFailureRollsBackAccountIndexProofAndReceipt()throws Exception {
        enrollmentBinding();enrollmentApp(NOW).enroll(enrollCommand(KEY,ENROLLMENT_EMAIL));var good=pendingEnrollmentProof();var before=snapshot();
        failWrite("audit_entry","INSERT");assertThrows(RuntimeException.class,()->enrollmentApp(NOW.plusSeconds(1)).verify(good));
        assertEquals(before,snapshot());assertEquals(0,scalar("SELECT count(*) FROM identity.customer_identifier"));
        assertEquals(0,scalar("SELECT count(*) FROM identity.customer_proof WHERE consumed_at IS NOT NULL"));
        assertEquals(1,scalar("SELECT count(*) FROM identity.customer_delivery WHERE status='QUEUED' AND ciphertext IS NOT NULL"));
        assertEquals(1,scalar("SELECT count(*) FROM identity.customer_command_receipt"));
    }
    @Test void concurrentVerificationHasOneAccountAndOneSingleUseOutcome()throws Exception {
        enrollmentBinding();enrollmentApp(NOW).enroll(enrollCommand(KEY,ENROLLMENT_EMAIL));var proof=pendingEnrollmentProof();
        var results=adminRace(()->{
            try{enrollmentApp(NOW.plusSeconds(1)).verify(proof);return "VERIFIED";}
            catch(IdentityCommandFailure failure){return failure.code().name();}
        },()->{
            try{enrollmentApp(NOW.plusSeconds(1)).verify(new com.elogistic.backend.identity.api.customer.CustomerEnrollmentCommands.Verification(proof.proofId(),proof.proof(),id(909),REQUEST));return "VERIFIED";}
            catch(IdentityCommandFailure failure){return failure.code().name();}
        });
        // Either caller can win; both tasks report an ordinary outcome instead of throwing into the executor.
        assertEquals(1,results.stream().filter("VERIFIED"::equals).count());
        assertEquals(1,results.stream().filter(TOKEN_INVALID_OR_EXPIRED.name()::equals).count());
        assertEquals(4,scalar("SELECT count(*) FROM identity.user_account"));assertEquals(1,scalar("SELECT count(*) FROM identity.customer_identifier"));
    }
    @Test void enrollmentStoreRejectsCallerOwnedTransactionAndResourceBindings()throws Exception {
        var source=outboxDataSource();var store=new JdbcCustomerEnrollmentStore(source);var manager=new org.springframework.jdbc.support.JdbcTransactionManager(source);
        new org.springframework.transaction.support.TransactionTemplate(manager).execute(status->{assertThrows(IllegalStateException.class,()->store.transaction(tx->null));return null;});
        org.springframework.transaction.support.TransactionSynchronizationManager.initSynchronization();try{assertThrows(IllegalStateException.class,()->store.transaction(tx->null));}
        finally{org.springframework.transaction.support.TransactionSynchronizationManager.clearSynchronization();}
        Object resource=new Object();org.springframework.transaction.support.TransactionSynchronizationManager.bindResource(source,resource);
        try{assertThrows(IllegalStateException.class,()->store.transaction(tx->null));assertSame(resource,org.springframework.transaction.support.TransactionSynchronizationManager.getResource(source));}
        finally{org.springframework.transaction.support.TransactionSynchronizationManager.unbindResource(source);}
    }
}
