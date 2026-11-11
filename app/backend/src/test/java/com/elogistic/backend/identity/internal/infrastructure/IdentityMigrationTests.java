package com.elogistic.backend.identity.internal.infrastructure;

import com.elogistic.backend.testsupport.PostgresTestcontainersSupport;
import com.elogistic.backend.testsupport.PostgresTestcontainersSupport.Database;
import java.sql.*;
import java.util.*;
import org.flywaydb.core.api.FlywayException;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.parallel.*;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import static org.junit.jupiter.api.Assertions.*;

/** T-002-03 only. Explicit Task-C profile; no Spring/developer datasource fallback. */
@Tag("task-c")
@Testcontainers
@Execution(ExecutionMode.SAME_THREAD)
class IdentityMigrationTests {
    @Container static final PostgreSQLContainer POSTGRES = PostgresTestcontainersSupport.container();
    private static final String ACCOUNT = "'10000000-0000-4000-8000-000000000001'";
    private static final String OTHER = "'10000000-0000-4000-8000-000000000002'";
    private static final String SESSION = "'20000000-0000-4000-8000-000000000001'";
    private static final String OTHER_SESSION = "'20000000-0000-4000-8000-000000000002'";
    private static final String TOKEN = "'30000000-0000-4000-8000-000000000001'";
    private static final String NEXT = "'30000000-0000-4000-8000-000000000002'";
    private static final String PROOF = "'40000000-0000-4000-8000-000000000001'";
    private static final String BINDING = "'50000000-0000-4000-8000-000000000001'";
    private static final String KEY = "'60000000-0000-4000-8000-000000000001'";
    private static final String NOW = "TIMESTAMPTZ '2026-10-21 00:00:00+00'";
    // Syntactically BCrypt, synthetic/non-authenticating fixture, never a real credential.
    private static final String HASH = "'$2b$12$" + ".".repeat(53) + "'";
    private static final List<String> TABLES = List.of("user_account", "account_grant", "auth_session",
        "refresh_token", "action_proof", "csrf_binding", "command_receipt", "audit_entry");

    @AfterAll static void verifyPolicy() throws Exception { PostgresTestcontainersSupport.verifyRuntime(POSTGRES); }

    private Database migrated() throws Exception {
        Database db = PostgresTestcontainersSupport.database(POSTGRES);
        assertEquals(25, db.flyway("latest").migrate().migrationsExecuted);
        return db;
    }
    private static String digest(int value) { return "decode('" + String.format("%064x", value) + "','hex')"; }
    private void execute(Database db, String sql) throws SQLException {
        try (Connection c = db.runtimeConnection(); Statement s = c.createStatement()) { s.execute(sql); }
    }
    private void rejects(Database db, String state, String sql) throws SQLException {
        try (Connection c = db.runtimeConnection(); Statement s = c.createStatement()) {
            assertEquals(state, assertThrows(SQLException.class, () -> s.execute(sql)).getSQLState());
        }
    }
    private String account(String id, String username) {
        return "INSERT INTO identity.user_account(id,username,display_name,email,status,created_at,updated_at) VALUES (" +
            id + "," + username + ",'Synthetic Identity','identity@example.invalid','PENDING_ACTIVATION'," + NOW + "," + NOW + ")";
    }
    private void account(Database db) throws SQLException { execute(db, account(ACCOUNT, "'fixture.identity'")); }
    private String session(String id) {
        return "INSERT INTO identity.auth_session VALUES (" + id + "," + ACCOUNT + ",1," + NOW + "," + NOW +
            "+INTERVAL '8 hours'," + NOW + "," + NOW + "+INTERVAL '30 minutes',NULL)";
    }
    private String token(String id, String family, int hash, String predecessor) {
        return "INSERT INTO identity.refresh_token(id,session_id,token_hash,predecessor_id,issued_at,expires_at) VALUES (" +
            id + "," + family + "," + digest(hash) + "," + predecessor + "," + NOW + "," + NOW + "+INTERVAL '8 hours')";
    }
    private String proof(String id, String purpose, int hash, String lifetime) {
        return "INSERT INTO identity.action_proof(id,account_id,purpose,token_hash,created_at,expires_at) VALUES (" +
            id + "," + ACCOUNT + ",'" + purpose + "'," + digest(hash) + "," + NOW + "," + NOW + "+INTERVAL '" + lifetime + "')";
    }
    private String csrf() {
        return "INSERT INTO identity.csrf_binding(id,cookie_hash,challenge_hash,created_at,expires_at) VALUES (" +
            BINDING + "," + digest(100) + "," + digest(101) + "," + NOW + "," + NOW + "+INTERVAL '15 minutes')";
    }
    private List<String> snapshot(Database db, String table) throws SQLException {
        try (Connection c = db.runtimeConnection(); Statement s = c.createStatement();
             ResultSet r = s.executeQuery("SELECT row_to_json(t)::text FROM " + table + " t ORDER BY id")) {
            List<String> result = new ArrayList<>(); while (r.next()) result.add(r.getString(1)); return result;
        }
    }

    @Test void coldStartValidateAndRerunPreserveHistoryAndIdentityData() throws Exception {
        Database db = migrated(); account(db);
        var flyway = db.flyway("latest"); flyway.validate();
        var before = Arrays.stream(flyway.info().applied()).map(m -> m.getVersion() + ":" + m.getChecksum()).toList();
        assertEquals(List.of("1","2","3","4","5","6","7","8","9","10","11","12","13","14","15","16","17","18","19","20","21","22","23","24","25"), Arrays.stream(flyway.info().applied()).map(m -> m.getVersion().toString()).toList());
        var rows = snapshot(db, "identity.user_account");
        assertEquals(0, flyway.migrate().migrationsExecuted);
        assertEquals(before, Arrays.stream(flyway.info().applied()).map(m -> m.getVersion() + ":" + m.getChecksum()).toList());
        assertEquals(rows, snapshot(db, "identity.user_account"));
    }

    @Test void upgradeV2PreservesAllDemoTablesWithoutImportingAccounts() throws Exception {
        Database db = PostgresTestcontainersSupport.database(POSTGRES);
        assertEquals(2, db.flyway("2").migrate().migrationsExecuted);
        execute(db, "INSERT INTO merchant.demo_merchant VALUES (" + ACCOUNT + ",'Synthetic Merchant','fixture-merchant')");
        execute(db, "INSERT INTO identity.demo_account VALUES (" + ACCOUNT + ",'fixture.demo'," + HASH + ",'Synthetic Demo','MERCHANT',TRUE," + ACCOUNT + ")");
        execute(db, "INSERT INTO orders.demo_shipment VALUES (" + ACCOUNT + "," + ACCOUNT + ",'fixture-draft','synthetic pickup'," +
            "'Synthetic Recipient','0000000000','synthetic street','synthetic ward','synthetic province','synthetic parcel',1,'DRAFT'," + ACCOUNT + "," + NOW + "," + NOW + ")");
        execute(db, "INSERT INTO orders.demo_event VALUES (" + ACCOUNT + "," + ACCOUNT + ",'DRAFT_CREATED'," + ACCOUNT + ",NULL," + NOW + ")");
        execute(db, "INSERT INTO orders.demo_operation VALUES (" + ACCOUNT + ",'create','fixture-key','synthetic-fingerprint'," + ACCOUNT + ")");
        var tables = List.of("merchant.demo_merchant", "identity.demo_account", "orders.demo_shipment", "orders.demo_event");
        Map<String,List<String>> before = new LinkedHashMap<>(); for (String table : tables) before.put(table, snapshot(db, table));
        var oldChecksums = Arrays.stream(db.flyway("2").info().applied()).map(m -> m.getChecksum()).toList();
        assertEquals(23, db.flyway("latest").migrate().migrationsExecuted); db.flyway("latest").validate();
        for (String table : tables) assertEquals(before.get(table), snapshot(db, table));
        assertEquals(oldChecksums, Arrays.stream(db.flyway("latest").info().applied()).limit(2).map(m -> m.getChecksum()).toList());
        try (Connection c = db.runtimeConnection(); Statement s = c.createStatement()) {
            try (ResultSet r = s.executeQuery("SELECT count(*) FROM orders.demo_operation WHERE payload_hash='synthetic-fingerprint'")) { assertTrue(r.next()); assertEquals(1, r.getInt(1)); }
            try (ResultSet r = s.executeQuery("SELECT count(*) FROM identity.user_account")) { assertTrue(r.next()); assertEquals(0, r.getInt(1)); }
        }
    }

    @Test void ownerOwnsEightTablesIndexesAndOnlyIdentityForeignKeys() throws Exception {
        Database db = migrated();
        try (Connection c = db.migrationConnection(); Statement s = c.createStatement()) {
            s.execute("SET ROLE " + db.owner());
            for (String table : TABLES) {
                try (ResultSet r = s.executeQuery("SELECT tableowner FROM pg_tables WHERE schemaname='identity' AND tablename='" + table + "'")) { assertTrue(r.next()); assertEquals(db.owner(), r.getString(1)); }
                try (ResultSet r = s.executeQuery("SELECT count(*) FROM pg_indexes WHERE schemaname='identity' AND tablename='" + table + "'")) { assertTrue(r.next()); assertTrue(r.getInt(1) > 0); }
            }
            try (ResultSet r = s.executeQuery("SELECT count(*) FROM pg_constraint c JOIN pg_class t ON c.conrelid=t.oid JOIN pg_namespace n ON t.relnamespace=n.oid JOIN pg_class p ON c.confrelid=p.oid JOIN pg_namespace pn ON p.relnamespace=pn.oid WHERE c.contype='f' AND n.nspname='identity' AND pn.nspname<>'identity'")) { assertTrue(r.next()); assertEquals(0, r.getInt(1)); }
            for (String index : List.of("idx_identity_grant_scope", "idx_identity_session_account", "uq_identity_refresh_live", "uq_identity_proof_live", "idx_identity_receipt_expiry", "idx_identity_audit_target_time")) {
                try (ResultSet r = s.executeQuery("SELECT count(*) FROM pg_indexes WHERE schemaname='identity' AND indexname='" + index + "'")) { assertTrue(r.next()); assertEquals(1, r.getInt(1)); }
            }
        }
    }

    @Test void usernamesRemainExactCaseUnicodeAndRejectDuplicatesBlankAndNull() throws Exception {
        Database db = migrated(); account(db);
        rejects(db, "23505", account(OTHER, "'fixture.identity'"));
        execute(db, account(OTHER, "'Fixture.identity'"));
        execute(db, account(TOKEN, "' fixture.identity '"));
        execute(db, account(NEXT, "chr(233)"));
        execute(db, account(PROOF, "'e'||chr(769)"));
        for (String username : List.of("''", "'  '", "chr(160)||chr(12288)||chr(65279)", "repeat('x',81)"))
            rejects(db, username.contains("81") ? "22001" : "23514", account(SESSION, username));
        rejects(db, "23502", account(SESSION, "NULL"));
    }

    @Test void accountLifecycleRequiresVerifiedActivatedCredentialAndPositiveVersions() throws Exception {
        Database db = migrated(); account(db);
        rejects(db, "23514", "UPDATE identity.user_account SET status='ACTIVE'");
        rejects(db, "23514", "UPDATE identity.user_account SET status='DISABLED'");
        rejects(db, "23514", "UPDATE identity.user_account SET version=0");
        rejects(db, "23514", "UPDATE identity.user_account SET auth_version=0");
        execute(db, "UPDATE identity.user_account SET status='ACTIVE',password_hash=" + HASH + ",activated_at=" + NOW + ",email_verified=TRUE");
        rejects(db, "23514", "UPDATE identity.user_account SET password_hash='plaintext-not-allowed'");
        execute(db, "UPDATE identity.user_account SET status='DISABLED',disabled_at=" + NOW);
        execute(db, "UPDATE identity.user_account SET status='ACTIVE',disabled_at=NULL,version=version+1");
    }

    @Test void grantsAllowMultipleScopesButRejectDuplicatesInvalidScopesAndMissingAccounts() throws Exception {
        Database db = migrated(); account(db);
        String grant = "INSERT INTO identity.account_grant(account_id,role_code,scope_type,scope_id,granted_at) VALUES (" + ACCOUNT + ",'IDENTITY_MANAGEMENT','IDENTITY'," + ACCOUNT + "," + NOW + ")";
        execute(db, grant); rejects(db, "23505", grant);
        execute(db, grant.replace("'IDENTITY_MANAGEMENT','IDENTITY'", "'MERCHANT','MERCHANT'").replace("," + ACCOUNT + "," + NOW, "," + OTHER + "," + NOW));
        rejects(db, "23514", grant.replace("'IDENTITY',", "'UNKNOWN',"));
        rejects(db, "23503", grant.replace("VALUES (" + ACCOUNT, "VALUES (" + OTHER));
        rejects(db, "23502", grant.replace("'IDENTITY_MANAGEMENT'", "NULL"));
    }

    @Test void sessionsRejectInvalidDeadlinesMissingAccountsAndNullExpiry() throws Exception {
        Database db = migrated(); account(db); execute(db, session(SESSION));
        for (String update : List.of("absolute_expires_at=" + NOW + "+INTERVAL '9 hours'", "idle_expires_at=" + NOW + "+INTERVAL '31 minutes'", "last_activity_at=" + NOW + "-INTERVAL '1 second'", "revoked_at=" + NOW + "-INTERVAL '1 second'"))
            rejects(db, "23514", "UPDATE identity.auth_session SET " + update);
        rejects(db, "23502", "UPDATE identity.auth_session SET absolute_expires_at=NULL");
        rejects(db, "23503", session(OTHER_SESSION).replace("," + ACCOUNT + ",1,", "," + OTHER + ",1,"));
    }

    @Test void refreshHashesMustBeUniqueCorrectSizeAndHaveExistingSession() throws Exception {
        Database db = migrated(); account(db); execute(db, session(SESSION)); execute(db, session(OTHER_SESSION));
        execute(db, token(TOKEN, SESSION, 1, "NULL"));
        rejects(db, "23505", token(NEXT, OTHER_SESSION, 1, "NULL"));
        rejects(db, "23514", token(NEXT, OTHER_SESSION, 2, "NULL").replace(digest(2), "decode('01','hex')"));
        rejects(db, "23503", token(NEXT, OTHER, 2, "NULL"));
        rejects(db, "23514", "UPDATE identity.refresh_token SET expires_at=issued_at");
    }

    @Test void refreshLineageCannotCrossFamiliesForkOrHaveTwoLiveMembers() throws Exception {
        Database db = migrated(); account(db); execute(db, session(SESSION)); execute(db, session(OTHER_SESSION));
        execute(db, token(TOKEN, SESSION, 1, "NULL"));
        rejects(db, "23505", token(NEXT, SESSION, 2, TOKEN));
        execute(db, "UPDATE identity.refresh_token SET consumed_at=" + NOW + " WHERE id=" + TOKEN);
        rejects(db, "23503", token(NEXT, OTHER_SESSION, 2, TOKEN));
        execute(db, token(NEXT, SESSION, 2, TOKEN));
        execute(db, "UPDATE identity.refresh_token SET consumed_at=" + NOW + " WHERE id=" + NEXT);
        rejects(db, "23505", token(PROOF, SESSION, 3, TOKEN));
        rejects(db, "23505", token(PROOF, SESSION, 3, "NULL"));
        rejects(db, "23514", token(PROOF, SESSION, 3, PROOF));
    }

    @Test void proofPurposeLifetimeHashAndSingleOutstandingProofAreEnforced() throws Exception {
        Database db = migrated(); account(db);
        execute(db, proof(PROOF, "INVITATION", 10, "24 hours"));
        rejects(db, "23505", proof(NEXT, "INVITATION", 11, "24 hours"));
        rejects(db, "23514", proof(NEXT, "RECOVERY", 11, "16 minutes"));
        rejects(db, "23514", proof(NEXT, "OTHER", 11, "15 minutes"));
        rejects(db, "23505", proof(NEXT, "RECOVERY", 10, "15 minutes"));
        execute(db, proof(NEXT, "RECOVERY", 11, "15 minutes"));
        rejects(db, "23514", "UPDATE identity.action_proof SET consumed_at=expires_at+INTERVAL '1 second'");
        execute(db, "UPDATE identity.action_proof SET consumed_at=" + NOW);
        execute(db, proof(TOKEN, "INVITATION", 12, "24 hours"));
    }

    @Test void csrfSupportsPreauthAndSessionBindingButRejectsBadHashesAndReferences() throws Exception {
        Database db = migrated(); account(db); execute(db, session(SESSION)); execute(db, csrf());
        execute(db, "UPDATE identity.csrf_binding SET session_id=" + SESSION);
        rejects(db, "23503", "UPDATE identity.csrf_binding SET session_id=" + OTHER_SESSION);
        rejects(db, "23514", "UPDATE identity.csrf_binding SET cookie_hash=decode('01','hex')");
        rejects(db, "23514", "UPDATE identity.csrf_binding SET expires_at=created_at");
    }

    @Test void receiptsBindActorProofOrPreauthWithoutExistenceDisclosureOrDuplicateKey() throws Exception {
        Database db = migrated(); execute(db, csrf());
        String receipt = "INSERT INTO identity.command_receipt(binding_kind,preauth_binding_id,operation,operation_key,request_fingerprint,result_status,created_at,expires_at) VALUES ('PREAUTH'," +
            BINDING + ",'requestIdentityRecovery'," + KEY + "," + digest(80) + ",202," + NOW + "," + NOW + "+INTERVAL '24 hours')";
        execute(db, receipt); rejects(db, "23505", receipt);
        rejects(db, "23514", receipt.replace("'requestIdentityRecovery'", "'disableIdentityAccount'"));
        rejects(db, "23503", receipt.replace(BINDING, OTHER));
        account(db); execute(db, proof(PROOF, "RECOVERY", 2, "15 minutes"));
        execute(db, "INSERT INTO identity.command_receipt(binding_kind,proof_id,operation,operation_key,request_fingerprint,target_id,result_status,created_at,expires_at) VALUES ('PROOF'," + PROOF + ",'confirmIdentityRecovery'," + KEY + "," + digest(81) + "," + ACCOUNT + ",204," + NOW + "," + NOW + "+INTERVAL '24 hours')");
        execute(db, "INSERT INTO identity.command_receipt(binding_kind,actor_id,operation,operation_key,request_fingerprint,target_id,result_status,created_at,expires_at) VALUES ('ACTOR'," + ACCOUNT + ",'disableIdentityAccount'," + KEY + "," + digest(82) + "," + ACCOUNT + ",200," + NOW + "," + NOW + "+INTERVAL '24 hours')");
    }

    @Test void runtimeCanAppendAuditButCannotDdlDeleteAccountsOrMutateAuditAndReceipts() throws Exception {
        Database db = migrated(); account(db);
        execute(db, "INSERT INTO identity.audit_entry VALUES (" + KEY + "," + ACCOUNT + "," + ACCOUNT + ",'ACCOUNT_PROVISIONED','IDENTITY','synthetic reason','fixture-request'," + NOW + ")");
        for (String sql : List.of("UPDATE identity.audit_entry SET reason='changed'", "DELETE FROM identity.audit_entry", "TRUNCATE identity.audit_entry", "UPDATE identity.command_receipt SET result_status=204", "DELETE FROM identity.command_receipt", "DELETE FROM identity.user_account", "CREATE TABLE identity.forbidden(id INTEGER)", "ALTER TABLE identity.user_account ADD COLUMN forbidden TEXT", "SET ROLE " + db.owner()))
            rejects(db, "42501", sql);
        rejects(db, "23503", "INSERT INTO identity.audit_entry VALUES (" + OTHER + "," + OTHER + "," + ACCOUNT + ",'ACCOUNT_PROVISIONED','IDENTITY',NULL,'fixture-request'," + NOW + ")");
    }

    @Test void realmRegistryStartsInactiveAndRuntimeCannotRegisterOrActivate() throws Exception {
        Database db=migrated();
        try(Connection c=db.runtimeConnection();Statement s=c.createStatement();ResultSet r=s.executeQuery("SELECT tableowner FROM pg_tables WHERE schemaname='identity' AND tablename='realm'")) {
            assertTrue(r.next());assertEquals(db.owner(),r.getString(1));
        }
        try(Connection c=db.runtimeConnection();Statement s=c.createStatement();ResultSet r=s.executeQuery("SELECT code,status,bootstrap_account_id,activated_at FROM identity.realm")) {
            assertTrue(r.next());assertEquals("SYSTEM",r.getString(1));assertEquals("INACTIVE",r.getString(2));
            assertNull(r.getObject(3));assertNull(r.getObject(4));assertFalse(r.next());
        }
        for(String sql:List.of("UPDATE identity.realm SET status='ACTIVE'", "DELETE FROM identity.realm", "TRUNCATE identity.realm",
                "INSERT INTO identity.realm SELECT * FROM identity.realm",
                "SELECT identity.activate_system_realm("+ACCOUNT+","+ACCOUNT+","+KEY+")",
                "SELECT identity.bootstrap_system_realm_admin("+ACCOUNT+","+OTHER+",'fixture.new','Synthetic','new@example.invalid',"+KEY+")"))
            rejects(db,"42501",sql);
    }
    @Test void realmForeignKeyRejectsUnknownIdentityButPreservesExternalScope() throws Exception {
        Database db=migrated();account(db);
        String prefix="INSERT INTO identity.account_grant(account_id,role_code,scope_type,scope_id,granted_at) VALUES ("+ACCOUNT+",'IDENTITY_MANAGEMENT',";
        rejects(db,"23503",prefix+"'IDENTITY',"+OTHER+","+NOW+")");
        execute(db,prefix+"'IDENTITY',"+ACCOUNT+","+NOW+")");
        execute(db,prefix+"'MERCHANT',"+OTHER+","+NOW+")");
        // Generated-column validation precedes permission rejection for this statement on PG18.
        // Prove the runtime privilege boundary independently, then test generated-column integrity.
        try(Connection c=db.runtimeConnection();Statement s=c.createStatement();ResultSet r=s.executeQuery("SELECT has_column_privilege(current_user,'identity.account_grant','identity_realm_id','UPDATE')")) {
            assertTrue(r.next());assertFalse(r.getBoolean(1));
        }
        rejects(db,"42501","UPDATE identity.account_grant SET granted_at=granted_at");
        rejects(db,"428C9","UPDATE identity.account_grant SET identity_realm_id="+OTHER);
        SQLException generated=assertThrows(SQLException.class,()->db.ownerExecute("UPDATE identity.account_grant SET identity_realm_id="+OTHER));
        assertEquals("428C9",generated.getSQLState());
        try(Connection c=db.runtimeConnection();Statement s=c.createStatement();ResultSet r=s.executeQuery("SELECT identity_realm_id FROM identity.account_grant WHERE scope_type='MERCHANT'")) {
            assertTrue(r.next());assertNull(r.getObject(1));
        }
        assertThrows(SQLException.class,()->db.ownerExecute("DELETE FROM identity.realm"));
    }
    @Test void realmUpgradePreservesV3DataAndChecksums() throws Exception {
        Database db=PostgresTestcontainersSupport.database(POSTGRES);db.flyway("3").migrate();account(db);
        execute(db,"INSERT INTO identity.account_grant(account_id,role_code,scope_type,scope_id,granted_at) VALUES ("+ACCOUNT+",'IDENTITY_MANAGEMENT','IDENTITY',"+ACCOUNT+","+NOW+")");
        var before=snapshot(db,"identity.user_account");
        var checksums=Arrays.stream(db.flyway("3").info().applied()).map(m->m.getChecksum()).toList();
        assertEquals(22,db.flyway("latest").migrate().migrationsExecuted);db.flyway("latest").validate();
        assertEquals(0,db.flyway("latest").migrate().migrationsExecuted);
        assertEquals(before,snapshot(db,"identity.user_account"));
        assertEquals(checksums,Arrays.stream(db.flyway("latest").info().applied()).limit(3).map(m->m.getChecksum()).toList());
        try(Connection c=db.runtimeConnection();Statement s=c.createStatement();ResultSet r=s.executeQuery("SELECT scope_id,identity_realm_id,granted_at FROM identity.account_grant")) {
            assertTrue(r.next());assertEquals(r.getObject(1),r.getObject(2));assertEquals(Timestamp.from(java.time.Instant.parse("2026-10-21T00:00:00Z")),r.getTimestamp(3));
        }
    }
    @Test void unmatchedLegacyIdentityScopeStopsRealmUpgradeAtomically() throws Exception {
        Database db=PostgresTestcontainersSupport.database(POSTGRES);db.flyway("3").migrate();account(db);
        execute(db,"INSERT INTO identity.account_grant VALUES ("+ACCOUNT+",'IDENTITY_MANAGEMENT','IDENTITY',"+OTHER+","+NOW+")");
        var before=snapshot(db,"identity.user_account");
        assertThrows(FlywayException.class,()->db.flyway("latest").migrate());
        assertEquals(before,snapshot(db,"identity.user_account"));
        assertEquals(List.of("1","2","3"),Arrays.stream(db.flyway("3").info().applied()).map(m->m.getVersion().toString()).toList());
        try(Connection c=db.runtimeConnection();Statement s=c.createStatement();ResultSet r=s.executeQuery("SELECT to_regclass('identity.realm'),scope_id FROM identity.account_grant")) {
            assertTrue(r.next());assertNull(r.getObject(1));assertEquals(UUID.fromString(OTHER.replace("'","")),r.getObject(2,UUID.class));
        }
    }
    private void bootstrap(Database db) throws SQLException {
        db.ownerExecute("SELECT identity.bootstrap_system_realm_admin("+ACCOUNT+","+OTHER+",'fixture.bootstrap','Synthetic Bootstrap','bootstrap@example.invalid',"+KEY+")");
    }
    @Test void bootstrapCreatesPendingOnlyAndDuplicateCannotTakeOverAnAccount() throws Exception {
        Database db=migrated();bootstrap(db);
        try(Connection c=db.runtimeConnection();Statement s=c.createStatement();ResultSet r=s.executeQuery("SELECT status,email_verified,password_hash FROM identity.user_account")) {
            assertTrue(r.next());assertEquals("PENDING_ACTIVATION",r.getString(1));assertFalse(r.getBoolean(2));assertNull(r.getString(3));assertFalse(r.next());
        }
        var before=snapshot(db,"identity.user_account");assertThrows(SQLException.class,()->bootstrap(db));assertEquals(before,snapshot(db,"identity.user_account"));
        Database existing=migrated();execute(existing,account(OTHER,"'existing.identity'"));
        var old=snapshot(existing,"identity.user_account");assertThrows(SQLException.class,()->bootstrap(existing));assertEquals(old,snapshot(existing,"identity.user_account"));
    }
    @Test void realmActivationRequiresVerifiedInvitationAndCannotBeReplayedOrReactivated() throws Exception {
        Database db=migrated();bootstrap(db);
        String activate="SELECT identity.activate_system_realm("+ACCOUNT+","+OTHER+","+BINDING+")";
        assertThrows(SQLException.class,()->db.ownerExecute(activate));
        db.completeInvitationForTest(UUID.fromString(OTHER.replace("'","")));db.ownerExecute(activate);
        assertThrows(SQLException.class,()->db.ownerExecute(activate));
        db.ownerExecute("SELECT pg_advisory_xact_lock(5279420865908921433); UPDATE identity.realm SET status='INACTIVE',updated_at=CURRENT_TIMESTAMP");
        assertThrows(SQLException.class,()->db.ownerExecute(activate));
        try(Connection c=db.runtimeConnection();Statement s=c.createStatement();ResultSet r=s.executeQuery("SELECT count(*) FROM identity.audit_entry WHERE source='OWNER_BOOTSTRAP'")) {
            assertTrue(r.next());assertEquals(2,r.getInt(1));
        }
    }
    @Test void bootstrapAuditFailureRollsBackPendingAccountGrantAndRealmBinding() throws Exception {
        Database db=migrated();
        execute(db,"INSERT INTO identity.audit_entry(id,event_type,source,request_id,occurred_at) VALUES ("+KEY+",'SYNTHETIC','FIXTURE','fixture',CURRENT_TIMESTAMP)");
        assertThrows(SQLException.class,()->bootstrap(db));
        try(Connection c=db.runtimeConnection();Statement s=c.createStatement();ResultSet r=s.executeQuery("SELECT bootstrap_account_id,(SELECT count(*) FROM identity.user_account),(SELECT count(*) FROM identity.account_grant) FROM identity.realm")) {
            assertTrue(r.next());assertNull(r.getObject(1));assertEquals(0,r.getInt(2));assertEquals(0,r.getInt(3));
        }
    }
    @Test void inactiveRealmIsNotEnoughToCreateActiveAdministrativeAuthority() throws Exception {
        Database db=migrated();bootstrap(db);
        assertThrows(SQLException.class,()->db.ownerExecute("SELECT identity.activate_system_realm("+ACCOUNT+","+ACCOUNT+","+BINDING+")"));
        try(Connection c=db.runtimeConnection();Statement s=c.createStatement();ResultSet r=s.executeQuery("SELECT status,activated_at FROM identity.realm")) {
            assertTrue(r.next());assertEquals("INACTIVE",r.getString(1));assertNull(r.getObject(2));
        }
    }

    @Test void invitationV5UpgradePreservesLegacyProofsDataAndChecksums() throws Exception {
        Database db=PostgresTestcontainersSupport.database(POSTGRES);assertEquals(4,db.flyway("4").migrate().migrationsExecuted);account(db);
        execute(db,proof(PROOF,"INVITATION",501,"24 hours"));var accounts=snapshot(db,"identity.user_account");
        var checksums=Arrays.stream(db.flyway("4").info().applied()).map(m->m.getChecksum()).toList();
        assertEquals(21,db.flyway("latest").migrate().migrationsExecuted);db.flyway("latest").validate();assertEquals(0,db.flyway("latest").migrate().migrationsExecuted);
        assertEquals(checksums,Arrays.stream(db.flyway("latest").info().applied()).limit(4).map(m->m.getChecksum()).toList());assertEquals(accounts,snapshot(db,"identity.user_account"));
        try(Connection c=db.runtimeConnection();Statement s=c.createStatement();ResultSet r=s.executeQuery("SELECT account_id,purpose,token_hash,expires_at-created_at=INTERVAL '24 hours',verifier_key_id,consumed_at,revoked_at FROM identity.action_proof WHERE id="+PROOF)) {
            assertTrue(r.next());assertEquals(UUID.fromString(ACCOUNT.replace("'","")),r.getObject(1,UUID.class));assertEquals("INVITATION",r.getString(2));
            assertEquals(501,new java.math.BigInteger(1,r.getBytes(3)).intValueExact());assertTrue(r.getBoolean(4));assertNull(r.getString(5));assertNull(r.getObject(6));assertNull(r.getObject(7));assertFalse(r.next());
        }
        assertTrue(snapshot(db,"identity.invitation_outbox").isEmpty());
    }
    @Test void invitationOutboxEnforcesProofAssociationUniquenessAndProtectedShape() throws Exception {
        Database db=migrated();account(db);execute(db,account(OTHER,"'fixture.other'"));execute(db,proof(PROOF,"INVITATION",502,"24 hours"));
        execute(db,"UPDATE identity.action_proof SET verifier_key_id='v1' WHERE id="+PROOF);
        String insert="INSERT INTO identity.invitation_outbox(id,proof_id,account_id,realm_id,purpose,destination,format_version,verifier_key_id,encryption_key_id,created_at,expires_at,status,next_attempt_at,nonce,ciphertext) VALUES ("+KEY+","+PROOF+","+ACCOUNT+","+ACCOUNT+",'INVITATION','fixture@example.invalid',1,'v1','e1',"+NOW+","+NOW+"+INTERVAL '24 hours','PENDING',"+NOW+",decode(repeat('00',12),'hex'),decode(repeat('00',59),'hex'))";
        rejects(db,"23503",insert.replace(PROOF+","+ACCOUNT+","+ACCOUNT,PROOF+","+OTHER+","+ACCOUNT));
        rejects(db,"23503",insert.replace("'v1'","'unknown'"));
        rejects(db,"23514",insert.replace("repeat('00',12)","repeat('00',11)"));
        rejects(db,"23514",insert.replace("repeat('00',59)","repeat('00',43)"));
        rejects(db,"23514",insert.replace("'PENDING'","'SENT'"));
        rejects(db,"23514",insert.replace("'INVITATION'","'RECOVERY'"));
        execute(db,insert);rejects(db,"23505",insert.replace(KEY,BINDING));
        rejects(db,"23514","UPDATE identity.invitation_outbox SET status='LEASED'");
        rejects(db,"23514","UPDATE identity.invitation_outbox SET attempts=11");
        rejects(db,"23514","UPDATE identity.invitation_outbox SET verifier_key_id='bad/key'");
    }
    @Test void invitationOutboxOwnershipAndRuntimePrivilegesRemainNarrow() throws Exception {
        Database db=migrated();
        try(Connection c=db.runtimeConnection();Statement s=c.createStatement()) {
            try(ResultSet r=s.executeQuery("SELECT tableowner FROM pg_tables WHERE schemaname='identity' AND tablename='invitation_outbox'")){assertTrue(r.next());assertEquals(db.owner(),r.getString(1));}
            for(String privilege:List.of("SELECT","INSERT","UPDATE"))try(ResultSet r=s.executeQuery("SELECT has_table_privilege(current_user,'identity.invitation_outbox','"+privilege+"')")){assertTrue(r.next());assertTrue(r.getBoolean(1));}
            for(String privilege:List.of("DELETE","TRUNCATE","REFERENCES","TRIGGER"))try(ResultSet r=s.executeQuery("SELECT has_table_privilege(current_user,'identity.invitation_outbox','"+privilege+"')")){assertTrue(r.next());assertFalse(r.getBoolean(1));}
            try(ResultSet r=s.executeQuery("SELECT count(*) FROM information_schema.columns WHERE table_schema='identity' AND table_name='invitation_outbox' AND column_name IN ('raw_token','token','invitation_link','payload')")){assertTrue(r.next());assertEquals(0,r.getInt(1));}
        }
        rejects(db,"42501","DELETE FROM identity.invitation_outbox");rejects(db,"42501","TRUNCATE identity.invitation_outbox");
        rejects(db,"42501","ALTER TABLE identity.invitation_outbox ADD COLUMN raw_token TEXT");
    }
    @Test void preexistingSchemaDriftFailsWithoutBaselineOrRepair() throws Exception {
        Database db = PostgresTestcontainersSupport.database(POSTGRES); db.flyway("2").migrate();
        try (Connection c = db.migrationConnection(); Statement s = c.createStatement()) {
            s.execute("SET ROLE " + db.owner()); s.execute("CREATE TABLE identity.user_account(drift_marker INTEGER PRIMARY KEY)");
        }
        assertThrows(FlywayException.class, () -> db.flyway("latest").migrate());
        assertEquals(List.of("1","2"), Arrays.stream(db.flyway("2").info().applied()).map(m -> m.getVersion().toString()).toList());
        try (Connection c = db.migrationConnection(); Statement s = c.createStatement(); ResultSet r = s.executeQuery("SELECT count(*) FROM information_schema.columns WHERE table_schema='identity' AND table_name='user_account' AND column_name='drift_marker'")) { assertTrue(r.next()); assertEquals(1, r.getInt(1)); }
    }

    @Test void customerV7UpgradePreservesV6DataChecksumsAndCreatesNoBindings()throws Exception {
        Database db=PostgresTestcontainersSupport.database(POSTGRES);assertEquals(6,db.flyway("6").migrate().migrationsExecuted);account(db);
        var checksums=Arrays.stream(db.flyway("6").info().applied()).map(m->m.getChecksum()).toList();var rows=snapshot(db,"identity.user_account");
        assertEquals(1,db.flyway("7").migrate().migrationsExecuted);db.flyway("7").validate();
        assertEquals(checksums,Arrays.stream(db.flyway("7").info().applied()).limit(6).map(m->m.getChecksum()).toList());
        assertEquals(rows,snapshot(db,"identity.user_account"));assertEquals(0,db.flyway("7").migrate().migrationsExecuted);
        try(Connection c=db.runtimeConnection();Statement q=c.createStatement();ResultSet r=q.executeQuery("SELECT count(*) FROM identity.customer_identifier")){assertTrue(r.next());assertEquals(0,r.getInt(1));}
    }
    @Test void customerIdentifierHasExactUniqueActiveAccountBinding()throws Exception {
        Database db=migrated();account(db);execute(db,account(OTHER,"'other.identity'"));
        execute(db,"INSERT INTO identity.customer_identifier VALUES ("+PROOF+","+ACCOUNT+",'Person@example.invalid',"+NOW+",'ACTIVE',1)");
        rejects(db,"23505","INSERT INTO identity.customer_identifier VALUES ("+NEXT+","+OTHER+",'Person@example.invalid',"+NOW+",'ACTIVE',1)");
        rejects(db,"23505","INSERT INTO identity.customer_identifier VALUES ("+NEXT+","+ACCOUNT+",'other@example.invalid',"+NOW+",'ACTIVE',1)");
        execute(db,"INSERT INTO identity.customer_identifier VALUES ("+NEXT+","+OTHER+",'person@example.invalid',"+NOW+",'ACTIVE',1)");
        rejects(db,"23503","INSERT INTO identity.customer_identifier VALUES ("+TOKEN+","+BINDING+",'missing@example.invalid',"+NOW+",'ACTIVE',1)");
    }
    @Test void customerSessionsRefreshAndCsrfEnforceOwnPurposeForeignKeys()throws Exception {
        Database db=migrated();account(db);execute(db,session(SESSION));
        execute(db,session(OTHER_SESSION).replace("identity.auth_session","identity.customer_auth_session"));
        execute(db,token(TOKEN,OTHER_SESSION,41,"NULL").replace("identity.refresh_token","identity.customer_refresh_token"));
        rejects(db,"23503",token(NEXT,OTHER_SESSION,42,"NULL"));
        rejects(db,"23503",token(NEXT,SESSION,42,"NULL").replace("identity.refresh_token","identity.customer_refresh_token"));
        rejects(db,"23503","INSERT INTO identity.customer_csrf_binding VALUES ("+BINDING+","+digest(43)+","+digest(44)+","+SESSION+","+NOW+","+NOW+"+INTERVAL '15 minutes',NULL)");
        rejects(db,"23514",session(PROOF).replace("identity.auth_session","identity.customer_auth_session").replace("INTERVAL '8 hours'","INTERVAL '9 hours'"));
    }
    @Test void customerTablesRemainOwnerOwnedWithoutRuntimeDdlOrAuditEscalation()throws Exception {
        Database db=migrated();
        for(String table:List.of("customer_identifier","customer_auth_session","customer_refresh_token","customer_csrf_binding")) {
            rejects(db,"42501","ALTER TABLE identity."+table+" ADD COLUMN injected BOOLEAN");rejects(db,"42501","TRUNCATE identity."+table);rejects(db,"42501","DELETE FROM identity."+table);
            try(Connection c=db.migrationConnection();Statement q=c.createStatement();ResultSet r=q.executeQuery("SELECT tableowner FROM pg_tables WHERE schemaname='identity' AND tablename='"+table+"'")){assertTrue(r.next());assertEquals(db.owner(),r.getString(1));}
        }
        rejects(db,"42501","UPDATE identity.audit_entry SET reason='injected'");rejects(db,"42501","DELETE FROM identity.user_account");
    }
    @Test void enrollmentV8UpgradePreservesPopulatedV7AndEarlierChecksums()throws Exception {
        Database db=PostgresTestcontainersSupport.database(POSTGRES);assertEquals(7,db.flyway("7").migrate().migrationsExecuted);account(db);execute(db,session(SESSION));
        execute(db,session(OTHER_SESSION).replace("identity.auth_session","identity.customer_auth_session"));
        var rows=snapshot(db,"identity.user_account");var checksums=Arrays.stream(db.flyway("7").info().applied()).map(m->m.getChecksum()).toList();
        assertEquals(1,db.flyway("8").migrate().migrationsExecuted);db.flyway("8").validate();assertEquals(0,db.flyway("8").migrate().migrationsExecuted);
        assertEquals(checksums,Arrays.stream(db.flyway("8").info().applied()).limit(7).map(m->m.getChecksum()).toList());assertEquals(rows,snapshot(db,"identity.user_account"));
        // The older SYSTEM store still reads/writes its accepted account/session relations.
        execute(db,"UPDATE identity.auth_session SET revoked_at="+NOW+" WHERE id="+SESSION);
        execute(db,token(TOKEN,SESSION,55,"NULL"));
    }
    private String enrollmentSql(){return "INSERT INTO identity.customer_enrollment VALUES ("+PROOF+","+NEXT+",'new@example.invalid','Synthetic',convert_to("+HASH+",'UTF8'),'S003-2026-10-27',"+NOW+","+NOW+"+INTERVAL '24 hours','PENDING',1)";}
    private String customerProofSql(){return "INSERT INTO identity.customer_proof(id,subject_id,enrollment_id,purpose,verifier,verifier_key_id,created_at,expires_at,policy_version) VALUES ("+TOKEN+","+NEXT+","+PROOF+",'CUSTOMER_ENROLLMENT',"+digest(56)+",'v1',"+NOW+","+NOW+"+INTERVAL '24 hours','S003-2026-10-27')";}
    @Test void enrollmentAndProofConstraintsDenyPlaintextWrongSubjectAndDeadline()throws Exception {
        Database db=migrated();
        rejects(db,"23514",enrollmentSql().replace("convert_to("+HASH+",'UTF8')","convert_to('plaintext-password','UTF8')"));
        execute(db,enrollmentSql());
        rejects(db,"23514",customerProofSql().replace("INTERVAL '24 hours'","INTERVAL '25 hours'"));
        rejects(db,"23503",customerProofSql().replace(TOKEN+","+NEXT+","+PROOF,TOKEN+","+ACCOUNT+","+PROOF));
        execute(db,customerProofSql());
        rejects(db,"23514","UPDATE identity.customer_enrollment SET status='CONSUMED' WHERE id="+PROOF);
        rejects(db,"23514","UPDATE identity.customer_proof SET consumed_at=expires_at");
        rejects(db,"23505",customerProofSql().replace(TOKEN,KEY));
    }
    @Test void enrollmentPersistenceHasLeastPrivilegeAndImmutableReceipts()throws Exception {
        Database db=migrated();
        for(String table:List.of("customer_enrollment","customer_phone_contact","customer_proof","customer_delivery","customer_command_receipt")){
            rejects(db,"42501","ALTER TABLE identity."+table+" ADD COLUMN injected BOOLEAN");rejects(db,"42501","TRUNCATE identity."+table);rejects(db,"42501","DELETE FROM identity."+table);
            try(Connection c=db.migrationConnection();Statement s=c.createStatement();ResultSet r=s.executeQuery("SELECT tableowner FROM pg_tables WHERE schemaname='identity' AND tablename='"+table+"'")){assertTrue(r.next());assertEquals(db.owner(),r.getString(1));}
        }
        rejects(db,"42501","UPDATE identity.customer_command_receipt SET request_id='00000000-0000-0000-0000-000000000000'");
        rejects(db,"42501","UPDATE identity.customer_phone_contact SET verification_status='UNVERIFIED'");
    }
    @Test void customerQuotaV9PreservesV8ChecksumsAndExistingSystemReservations()throws Exception {
        Database db=PostgresTestcontainersSupport.database(POSTGRES);assertEquals(8,db.flyway("8").migrate().migrationsExecuted);
        execute(db,"INSERT INTO identity.rate_limit_subject(policy,subject_hash) VALUES ('LOGIN_IP',"+digest(57)+")");
        execute(db,"INSERT INTO identity.rate_limit_attempt VALUES ("+KEY+",'LOGIN_IP',"+digest(57)+","+NOW+","+NOW+"+INTERVAL '15 minutes',"+NOW+"+INTERVAL '60 seconds','COUNTED')");
        var rows=rateAttemptSnapshot(db);var checksums=Arrays.stream(db.flyway("8").info().applied()).map(m->m.getChecksum()).toList();
        var source=new org.springframework.jdbc.datasource.DriverManagerDataSource(db.url(),db.runtime(),db.runtimePassword());assertThrows(IllegalStateException.class,()->CustomerRuntimeRequirements.verify(source));
        assertEquals(17,db.flyway("latest").migrate().migrationsExecuted);db.flyway("latest").validate();assertEquals(0,db.flyway("latest").migrate().migrationsExecuted);
        assertEquals(checksums,Arrays.stream(db.flyway("latest").info().applied()).limit(8).map(m->m.getChecksum()).toList());assertEquals(rows,rateAttemptSnapshot(db));assertDoesNotThrow(()->CustomerRuntimeRequirements.verify(source));
    }
    private List<String> rateAttemptSnapshot(Database db)throws SQLException{
        try(Connection c=db.runtimeConnection();Statement s=c.createStatement();ResultSet rows=s.executeQuery("SELECT row_to_json(t)::text FROM identity.rate_limit_attempt t ORDER BY reservation_id,policy,subject_hash")){
            List<String> result=new ArrayList<>();while(rows.next())result.add(rows.getString(1));return result;
        }
    }
    @Test void customerQuotaConstraintsPreserveCountedNonPasswordAndRejectUnknownPolicies()throws Exception {
        Database db=migrated();execute(db,"INSERT INTO identity.rate_limit_subject(policy,subject_hash) VALUES ('CUSTOMER_ENROLL_EMAIL',"+digest(58)+"),('CUSTOMER_LOGIN_EMAIL',"+digest(59)+")");
        rejects(db,"23514","INSERT INTO identity.rate_limit_subject(policy,subject_hash) VALUES ('CUSTOMER_PHONE',"+digest(60)+")");
        rejects(db,"23514","INSERT INTO identity.rate_limit_attempt VALUES ("+KEY+",'CUSTOMER_ENROLL_EMAIL',"+digest(58)+","+NOW+","+NOW+"+INTERVAL '1 hour',"+NOW+"+INTERVAL '60 seconds','PENDING')");
        execute(db,"INSERT INTO identity.rate_limit_attempt VALUES ("+KEY+",'CUSTOMER_LOGIN_EMAIL',"+digest(59)+","+NOW+","+NOW+"+INTERVAL '16 minutes',"+NOW+"+INTERVAL '60 seconds','PENDING')");
        rejects(db,"23514","UPDATE identity.rate_limit_attempt SET state='BYPASS'");rejects(db,"23514","UPDATE identity.rate_limit_attempt SET expires_at=created_at+INTERVAL '2 hours'");
    }
    @Test void recoveryV10UpgradePreservesV9AndRevokesOnlyUnsupportedUnboundRecovery()throws Exception{
        Database db=PostgresTestcontainersSupport.database(POSTGRES);assertEquals(9,db.flyway("9").migrate().migrationsExecuted);account(db);execute(db,proof(PROOF,"INVITATION",63,"24 hours"));
        // Synthetic V9 schema fixture; the previous application did not expose recovery issuance.
        execute(db,"INSERT INTO identity.customer_proof(id,subject_id,account_id,purpose,verifier,verifier_key_id,created_at,expires_at,policy_version) VALUES ("+TOKEN+","+ACCOUNT+","+ACCOUNT+",'CUSTOMER_RECOVERY',"+digest(62)+",'v1',"+NOW+","+NOW+"+INTERVAL '15 minutes','S003-2026-10-27')");
        execute(db,"INSERT INTO identity.customer_delivery(id,proof_id,subject_id,purpose,verifier_key_id,encryption_key_id,template_version,configuration_version,created_at,expires_at,status,due_at,nonce,ciphertext,recipient_nonce,recipient_ciphertext,version) VALUES ("+OTHER_SESSION+","+TOKEN+","+ACCOUNT+",'CUSTOMER_RECOVERY','v1','e1','S003-EMAIL-1','S003-SMTP-1',"+NOW+","+NOW+"+INTERVAL '15 minutes','QUEUED',"+NOW+",decode(repeat('01',12),'hex'),decode(repeat('02',59),'hex'),decode(repeat('03',12),'hex'),decode(repeat('04',32),'hex'),1)");
        var accounts=snapshot(db,"identity.user_account");var invitations=snapshot(db,"identity.action_proof");var checksums=Arrays.stream(db.flyway("9").info().applied()).map(m->m.getChecksum()).toList();
        assertEquals(1,db.flyway("10").migrate().migrationsExecuted);db.flyway("10").validate();assertEquals(0,db.flyway("10").migrate().migrationsExecuted);
        assertEquals(checksums,Arrays.stream(db.flyway("10").info().applied()).limit(9).map(m->m.getChecksum()).toList());assertEquals(accounts,snapshot(db,"identity.user_account"));assertEquals(invitations,snapshot(db,"identity.action_proof"));
        try(Connection c=db.runtimeConnection();Statement s=c.createStatement();ResultSet r=s.executeQuery("SELECT p.revoked_at,p.bound_auth_version,p.verifier,d.status,d.ciphertext,d.recipient_ciphertext FROM identity.customer_proof p JOIN identity.customer_delivery d ON d.proof_id=p.id")){
            assertTrue(r.next());assertNotNull(r.getTimestamp(1));assertNull(r.getObject(2));assertEquals(62,new java.math.BigInteger(1,r.getBytes(3)).intValueExact());assertEquals("EXPIRED",r.getString(4));assertNull(r.getBytes(5));assertNull(r.getBytes(6));assertFalse(r.next());
        }
    }
    @Test void recoveryProofDatabaseRequiresSecurityVersionWithoutSession(){
        assertDoesNotThrow(()->{
            Database db=PostgresTestcontainersSupport.database(POSTGRES);assertEquals(16,db.flyway("16").migrate().migrationsExecuted);account(db);
            var source=new org.springframework.jdbc.datasource.DriverManagerDataSource(db.url(),db.runtime(),db.runtimePassword());
            assertEquals("CUSTOMER_POSTGRES18_SCHEMA_V17_REQUIRED",assertThrows(IllegalStateException.class,()->CustomerRuntimeRequirements.verify(source)).getMessage());
            var checksums=Arrays.stream(db.flyway("16").info().applied()).map(m->m.getChecksum()).toList();assertEquals(9,db.flyway("latest").migrate().migrationsExecuted);
            assertEquals(checksums,Arrays.stream(db.flyway("latest").info().applied()).limit(16).map(m->m.getChecksum()).toList());CustomerRuntimeRequirements.verify(source);
            execute(db,session(SESSION).replace("identity.auth_session","identity.customer_auth_session"));
            String prefix="INSERT INTO identity.customer_proof(id,subject_id,account_id,purpose,verifier,verifier_key_id,created_at,expires_at,bound_auth_version,bound_session_id,policy_version) VALUES ("+TOKEN+","+ACCOUNT+","+ACCOUNT+",'CUSTOMER_RECOVERY',"+digest(64)+",'v1',"+NOW+","+NOW+"+INTERVAL '15 minutes',";
            rejects(db,"23514",prefix+"NULL,NULL,'S003-2026-10-27')");rejects(db,"23514",prefix+"0,NULL,'S003-2026-10-27')");rejects(db,"23514",prefix+"1,"+SESSION+",'S003-2026-10-27')");execute(db,prefix+"1,NULL,'S003-2026-10-27')");
            String otp=prefix.replace(TOKEN,NEXT).replace("15 minutes","2 minutes")+"1,NULL,'S003-OTP-2026-10-30')";
            rejects(db,"23514",otp.replace("2 minutes","3 minutes"));rejects(db,"23505",otp);
            execute(db,"UPDATE identity.customer_proof SET revoked_at=created_at WHERE id="+TOKEN);execute(db,otp);
        });
    }
    @Test void proofReceiptDatabaseEnforcesPurposeRetentionAndNarrowPrivileges()throws Exception{
        Database db=migrated();execute(db,enrollmentSql());execute(db,customerProofSql());
        String receipt="INSERT INTO identity.customer_proof_receipt VALUES ("+TOKEN+",'CUSTOMER_ENROLLMENT',"+KEY+",'verifyCustomerEmail','v1',"+digest(65)+","+BINDING+","+NOW+","+NOW+"+INTERVAL '7 days')";
        rejects(db,"23514",receipt.replace("'verifyCustomerEmail'","'confirmCustomerRecovery'"));rejects(db,"23503",receipt.replace("'CUSTOMER_ENROLLMENT'","'CUSTOMER_RECOVERY'").replace("'verifyCustomerEmail'","'confirmCustomerRecovery'"));rejects(db,"23514",receipt.replace("INTERVAL '7 days'","INTERVAL '6 days'"));
        execute(db,receipt);rejects(db,"23505",receipt);rejects(db,"42501","UPDATE identity.customer_proof_receipt SET request_id="+KEY);rejects(db,"42501","DELETE FROM identity.customer_proof_receipt");rejects(db,"42501","TRUNCATE identity.customer_proof_receipt");rejects(db,"42501","ALTER TABLE identity.customer_proof_receipt ADD COLUMN injected BOOLEAN");
    }

    private String profileSql(){return "INSERT INTO merchant.customer_profile VALUES ("+PROOF+","+ACCOUNT+",'Business display','vi','business@example.invalid',NULL,2,"+NOW+","+NOW+")";}
    private String profileHistorySql(){return "INSERT INTO merchant.customer_profile_version VALUES ("+PROOF+",2,"+ACCOUNT+",'Business display','vi','business@example.invalid',NULL,"+NOW+","+NOW+")";}
    private void profileFixture(Database db)throws SQLException{
        try(Connection c=db.runtimeConnection();Statement q=c.createStatement()){c.setAutoCommit(false);q.execute(profileSql());q.execute(profileHistorySql());c.commit();}
    }
    @Test void profileV11UpgradePreservesV10IdentityDataChecksumsAndOldSystemWrites()throws Exception{
        Database db=PostgresTestcontainersSupport.database(POSTGRES);assertEquals(10,db.flyway("10").migrate().migrationsExecuted);account(db);execute(db,session(SESSION));var rows=snapshot(db,"identity.user_account");var checksums=Arrays.stream(db.flyway("10").info().applied()).map(m->m.getChecksum()).toList();var source=new org.springframework.jdbc.datasource.DriverManagerDataSource(db.url(),db.runtime(),db.runtimePassword());
        assertEquals("CUSTOMER_PROFILE_POSTGRES18_SCHEMA_V11_REQUIRED",assertThrows(IllegalStateException.class,()->com.elogistic.backend.merchant.internal.infrastructure.JdbcCustomerProfileStore.requireSchema(source)).getMessage());assertEquals(1,db.flyway("11").migrate().migrationsExecuted);db.flyway("11").validate();assertEquals(0,db.flyway("11").migrate().migrationsExecuted);assertEquals(checksums,Arrays.stream(db.flyway("11").info().applied()).limit(10).map(m->m.getChecksum()).toList());assertEquals(rows,snapshot(db,"identity.user_account"));assertDoesNotThrow(()->com.elogistic.backend.merchant.internal.infrastructure.JdbcCustomerProfileStore.requireSchema(source));
        execute(db,"UPDATE identity.auth_session SET revoked_at="+NOW+" WHERE id="+SESSION);execute(db,token(TOKEN,SESSION,75,"NULL"));assertTrue(snapshot(db,"merchant.customer_profile").isEmpty());
    }
    @Test void profileDatabaseRequiresHistoryAndPreservesImmutableAccountVersionBinding()throws Exception{
        Database db=migrated();rejects(db,"23503",profileSql());profileFixture(db);
        rejects(db,"23505",profileSql().replace(PROOF,NEXT));rejects(db,"23514","UPDATE merchant.customer_profile SET account_id="+OTHER+",version=3");rejects(db,"23514","UPDATE merchant.customer_profile SET display_name='Unsafe'");rejects(db,"23514","UPDATE merchant.customer_profile SET id="+NEXT+",version=3");rejects(db,"23503","UPDATE merchant.customer_profile SET version=3,display_name='Missing history'");
        try(Connection c=db.runtimeConnection();Statement q=c.createStatement();ResultSet r=q.executeQuery("SELECT count(*) FROM pg_constraint c JOIN pg_class t ON t.oid=c.conrelid JOIN pg_namespace n ON n.oid=t.relnamespace JOIN pg_class f ON f.oid=c.confrelid JOIN pg_namespace fn ON fn.oid=f.relnamespace WHERE c.contype='f' AND n.nspname='merchant' AND fn.nspname<>'merchant'")){assertTrue(r.next());assertEquals(0,r.getInt(1));}
    }
    @Test void profileRuntimeCannotRewriteHistoryAuditReceiptOrDeleteEvidence()throws Exception{
        Database db=migrated();profileFixture(db);
        for(String table:List.of("customer_profile_version","business_audit","command_receipt")){rejects(db,"42501","UPDATE merchant."+table+" SET "+(table.equals("customer_profile_version")?"display_name='Injected'":table.equals("business_audit")?"policy_version='Injected'":"outcome='COMMITTED'"));rejects(db,"42501","DELETE FROM merchant."+table);rejects(db,"42501","TRUNCATE merchant."+table);rejects(db,"42501","ALTER TABLE merchant."+table+" ADD COLUMN injected BOOLEAN");}
        rejects(db,"42501","DELETE FROM merchant.customer_profile");rejects(db,"42501","CREATE TABLE merchant.injected(id UUID)");
        try(Connection c=db.migrationConnection();Statement q=c.createStatement();ResultSet r=q.executeQuery("SELECT count(*) FROM pg_tables WHERE schemaname='merchant' AND tablename IN ('customer_profile','customer_profile_version','business_audit','command_receipt') AND tableowner='"+db.owner()+"'")){assertTrue(r.next());assertEquals(4,r.getInt(1));}
    }
    @Test void profileReceiptRequiresExactSubjectVersionRetentionAndContainsNoRawContact()throws Exception{
        Database db=migrated();profileFixture(db);String receipt="INSERT INTO merchant.command_receipt(subject_account_id,target_type,target_id,operation,operation_key,fingerprint_key_id,canonical_request_hash,original_version,result_version,outcome,created_at,expires_at) VALUES ("+ACCOUNT+",'CUSTOMER_PROFILE',"+PROOF+",'updateCustomerProfile',"+KEY+",'k1',"+digest(76)+",1,2,'COMMITTED',"+NOW+","+NOW+"+INTERVAL '7 days')";
        rejects(db,"23514",receipt.replace("INTERVAL '7 days'","INTERVAL '6 days'"));rejects(db,"23503",receipt.replace("VALUES ("+ACCOUNT+",","VALUES ("+OTHER+","));rejects(db,"23503",receipt.replace(",1,2,'COMMITTED'",",2,3,'COMMITTED'"));execute(db,receipt);rejects(db,"23505",receipt);
        try(Connection c=db.runtimeConnection();Statement q=c.createStatement();ResultSet r=q.executeQuery("SELECT count(*) FROM information_schema.columns WHERE table_schema='merchant' AND table_name IN ('command_receipt','business_audit') AND column_name ~ '(email|phone|proof|password|token|payload)'")){assertTrue(r.next());assertEquals(0,r.getInt(1));}
    }
    @Test void reauthenticationV12UpgradePreservesV11ChecksumsAccountsAndSystemSessions()throws Exception{
        Database db=PostgresTestcontainersSupport.database(POSTGRES);assertEquals(11,db.flyway("11").migrate().migrationsExecuted);account(db);execute(db,session(SESSION));var rows=snapshot(db,"identity.user_account");var sessions=snapshot(db,"identity.auth_session");var checksums=Arrays.stream(db.flyway("11").info().applied()).map(m->m.getChecksum()).toList();
        var source=new org.springframework.jdbc.datasource.DriverManagerDataSource(db.url(),db.runtime(),db.runtimePassword());assertEquals("CUSTOMER_POSTGRES18_SCHEMA_V17_REQUIRED",assertThrows(IllegalStateException.class,()->CustomerRuntimeRequirements.verify(source)).getMessage());
        assertEquals(14,db.flyway("latest").migrate().migrationsExecuted);db.flyway("latest").validate();assertEquals(0,db.flyway("latest").migrate().migrationsExecuted);assertEquals(checksums,Arrays.stream(db.flyway("latest").info().applied()).limit(11).map(m->m.getChecksum()).toList());assertEquals(rows,snapshot(db,"identity.user_account"));assertEquals(sessions,snapshot(db,"identity.auth_session"));assertDoesNotThrow(()->CustomerRuntimeRequirements.verify(source));
        execute(db,"UPDATE identity.auth_session SET revoked_at="+NOW+" WHERE id="+SESSION);execute(db,token(TOKEN,SESSION,77,"NULL"));
    }
    @Test void reauthenticationReceiptRequiresExactProofSessionVersionRetentionAndImmutablePrivileges()throws Exception{
        Database db=migrated();account(db);execute(db,session(SESSION).replace("identity.auth_session","identity.customer_auth_session"));
        String proof="INSERT INTO identity.customer_proof(id,subject_id,account_id,purpose,created_at,expires_at,bound_session_id,bound_auth_version,policy_version) VALUES ("+PROOF+","+ACCOUNT+","+ACCOUNT+",'REAUTHENTICATION',"+NOW+","+NOW+"+INTERVAL '15 minutes',"+SESSION+",1,'S003-2026-10-27')";execute(db,proof);
        String receipt="INSERT INTO identity.customer_security_receipt(account_id,operation_key,operation,bound_session_id,bound_auth_version,proof_id,proof_purpose,fingerprint_key_id,request_fingerprint,request_id,created_at,expires_at) VALUES ("+ACCOUNT+","+KEY+",'requestCustomerReauthentication',"+SESSION+",1,"+PROOF+",'REAUTHENTICATION','v1',"+digest(78)+","+BINDING+","+NOW+","+NOW+"+INTERVAL '7 days')";
        rejects(db,"23514",receipt.replace("INTERVAL '7 days'","INTERVAL '6 days'"));rejects(db,"23503",receipt.replace(",1,"+PROOF,",2,"+PROOF));rejects(db,"23514",receipt.replace("'REAUTHENTICATION'","'BUSINESS_CONTACT'"));execute(db,receipt);rejects(db,"23505",receipt);
        for(String sql:List.of("UPDATE identity.customer_security_receipt SET request_id="+KEY,"DELETE FROM identity.customer_security_receipt","TRUNCATE identity.customer_security_receipt","ALTER TABLE identity.customer_security_receipt ADD COLUMN injected BOOLEAN"))rejects(db,"42501",sql);
        try(Connection c=db.runtimeConnection();Statement q=c.createStatement();ResultSet r=q.executeQuery("SELECT count(*) FROM information_schema.columns WHERE table_schema='identity' AND table_name='customer_security_receipt' AND column_name ~ '(email|phone|password|token|payload)'")){assertTrue(r.next());assertEquals(0,r.getInt(1));}
    }
    @Test void ownerConsentV13UpgradePreservesV12ReceiptsAndNarrowPrivileges()throws Exception{
        Database db=PostgresTestcontainersSupport.database(POSTGRES);assertEquals(12,db.flyway("12").migrate().migrationsExecuted);account(db);execute(db,session(SESSION).replace("identity.auth_session","identity.customer_auth_session"));
        execute(db,"INSERT INTO identity.customer_proof(id,subject_id,account_id,purpose,created_at,expires_at,bound_session_id,bound_auth_version,policy_version) VALUES ("+PROOF+","+ACCOUNT+","+ACCOUNT+",'REAUTHENTICATION',"+NOW+","+NOW+"+INTERVAL '15 minutes',"+SESSION+",1,'S003-2026-10-27')");
        String receipt="INSERT INTO identity.customer_security_receipt VALUES ("+ACCOUNT+","+KEY+",'requestCustomerReauthentication',"+SESSION+",1,"+PROOF+",'REAUTHENTICATION','v1',"+digest(78)+","+BINDING+","+NOW+","+NOW+"+INTERVAL '7 days')";
        execute(db,receipt);var checksums=Arrays.stream(db.flyway("12").info().applied()).map(m->m.getChecksum()).toList();
        assertEquals(1,db.flyway("13").migrate().migrationsExecuted);db.flyway("13").validate();assertEquals(0,db.flyway("13").migrate().migrationsExecuted);assertEquals(checksums,Arrays.stream(db.flyway("13").info().applied()).limit(12).map(m->m.getChecksum()).toList());
        execute(db,receipt.replace(KEY,NEXT));
        try(Connection c=db.runtimeConnection();Statement q=c.createStatement();ResultSet r=q.executeQuery("SELECT count(*) FROM identity.customer_security_receipt")){assertTrue(r.next());assertEquals(2,r.getInt(1));}
        try(Connection c=db.runtimeConnection();Statement q=c.createStatement();ResultSet r=q.executeQuery("SELECT count(*) FROM identity.customer_owner_consent_receipt")){assertTrue(r.next());assertEquals(0,r.getInt(1));}
        rejects(db,"42501","UPDATE identity.customer_security_receipt SET request_id="+KEY);rejects(db,"42501","DELETE FROM identity.customer_security_receipt");rejects(db,"42501","UPDATE identity.customer_owner_consent_receipt SET request_id="+KEY);rejects(db,"42501","DELETE FROM identity.customer_owner_consent_receipt");
    }
    @Test void applicationV14UpgradePreservesV13ProfileAndRestrictsDraftHistory()throws Exception{
        Database db=PostgresTestcontainersSupport.database(POSTGRES);assertEquals(13,db.flyway("13").migrate().migrationsExecuted);account(db);profileFixture(db);var profile=snapshot(db,"merchant.customer_profile");var checksums=Arrays.stream(db.flyway("13").info().applied()).map(m->m.getChecksum()).toList();
        var source=new org.springframework.jdbc.datasource.DriverManagerDataSource(db.url(),db.runtime(),db.runtimePassword());assertEquals("MERCHANT_APPLICATION_POSTGRES18_SCHEMA_V14_REQUIRED",assertThrows(IllegalStateException.class,()->com.elogistic.backend.merchant.internal.infrastructure.JdbcMerchantApplicationStore.requireSchema(source)).getMessage());
        assertEquals(12,db.flyway("latest").migrate().migrationsExecuted);db.flyway("latest").validate();assertEquals(0,db.flyway("latest").migrate().migrationsExecuted);assertEquals(checksums,Arrays.stream(db.flyway("latest").info().applied()).limit(13).map(m->m.getChecksum()).toList());assertEquals(profile,snapshot(db,"merchant.customer_profile"));assertDoesNotThrow(()->com.elogistic.backend.merchant.internal.infrastructure.JdbcMerchantApplicationStore.requireSchema(source));
        rejects(db,"42501","DELETE FROM merchant.application");rejects(db,"42501","UPDATE merchant.application_draft_version SET draft='{}'");rejects(db,"42501","DELETE FROM merchant.application_draft_version");rejects(db,"42501","ALTER TABLE merchant.application ADD COLUMN injected BOOLEAN");
        try(Connection c=db.runtimeConnection();Statement q=c.createStatement();ResultSet r=q.executeQuery("SELECT count(*) FROM pg_constraint c JOIN pg_class t ON t.oid=c.conrelid JOIN pg_namespace n ON n.oid=t.relnamespace JOIN pg_class f ON f.oid=c.confrelid JOIN pg_namespace fn ON fn.oid=f.relnamespace WHERE c.contype='f' AND n.nspname='merchant' AND fn.nspname<>'merchant'")){assertTrue(r.next());assertEquals(0,r.getInt(1));}
    }

    @Test void addressV18UpgradePreservesV17DataChecksumsAndRuntimeBoundary()throws Exception{
        Database db=PostgresTestcontainersSupport.database(POSTGRES);assertEquals(17,db.flyway("17").migrate().migrationsExecuted);account(db);profileFixture(db);var profile=snapshot(db,"merchant.customer_profile");var identity=snapshot(db,"identity.user_account");var checksums=Arrays.stream(db.flyway("17").info().applied()).map(m->m.getChecksum()).toList();
        var source=new org.springframework.jdbc.datasource.DriverManagerDataSource(db.url(),db.runtime(),db.runtimePassword());assertEquals("PERSONAL_ADDRESS_POSTGRES18_SCHEMA_V18_REQUIRED",assertThrows(IllegalStateException.class,()->com.elogistic.backend.merchant.internal.infrastructure.JdbcPersonalAddressStore.requireSchema(source)).getMessage());
        assertEquals(8,db.flyway("latest").migrate().migrationsExecuted);db.flyway("latest").validate();assertEquals(0,db.flyway("latest").migrate().migrationsExecuted);assertEquals(checksums,Arrays.stream(db.flyway("latest").info().applied()).limit(17).map(m->m.getChecksum()).toList());assertEquals(profile,snapshot(db,"merchant.customer_profile"));assertEquals(identity,snapshot(db,"identity.user_account"));assertDoesNotThrow(()->com.elogistic.backend.merchant.internal.infrastructure.JdbcPersonalAddressStore.requireSchema(source));
        for(String sql:List.of("DELETE FROM merchant.customer_address","UPDATE merchant.customer_address_version SET label='injected'","DELETE FROM merchant.customer_address_version","ALTER TABLE merchant.customer_address ADD COLUMN injected BOOLEAN"))rejects(db,"42501",sql);
    }
    private String addressRow(String id,String account,boolean isDefault){return "INSERT INTO merchant.customer_address(id,account_id,kind,contact_name,contact_phone,country,administrative_area,locality,address_line,is_default,version,created_at,updated_at) VALUES ("+id+","+account+",'SENDER','Synthetic contact','+84901234567','VN','Area','Locality','Street',"+isDefault+",1,"+NOW+","+NOW+")";}
    private void addressFixture(Database db)throws Exception{execute(db,"BEGIN;"+addressRow(PROOF,ACCOUNT,true)+"; INSERT INTO merchant.customer_address_version SELECT * FROM merchant.customer_address; COMMIT;");}
    @Test void addressDatabaseDefaultHistoryAndImmutableOwnerCannotBeBypassed()throws Exception{
        Database db=migrated();addressFixture(db);rejects(db,"23505",addressRow(BINDING,ACCOUNT,true));rejects(db,"23503",addressRow(BINDING,ACCOUNT,false));
        for(String sql:List.of("UPDATE merchant.customer_address SET account_id="+OTHER+",version=2","UPDATE merchant.customer_address SET version=3","UPDATE merchant.customer_address SET contact_phone='0901234567',version=2","UPDATE merchant.customer_address SET is_default=true,archived_at=updated_at,version=2"))rejects(db,"23514",sql);
        rejects(db,"23503","UPDATE merchant.customer_address SET label='Without history',version=2");assertEquals(1,snapshot(db,"merchant.customer_address").size());assertEquals(1,snapshot(db,"merchant.customer_address_version").size());
    }
    @Test void addressReceiptForeignKeysBindExactSubjectVersionAndNoPlainContact()throws Exception{
        Database db=migrated();addressFixture(db);
        String receipt="INSERT INTO merchant.command_receipt(subject_account_id,target_type,target_id,operation,operation_key,fingerprint_key_id,canonical_request_hash,original_version,result_version,outcome,created_at,expires_at) VALUES (%s,'CUSTOMER_ADDRESS',"+PROOF+",'createPersonalAddress',"+KEY+",'v1',decode(repeat('01',32),'hex'),0,1,'COMMITTED',"+NOW+","+NOW+"+INTERVAL '7 days')";
        rejects(db,"23503",receipt.formatted(OTHER));execute(db,receipt.formatted(ACCOUNT));rejects(db,"23505",receipt.formatted(ACCOUNT));
        for(String sql:List.of("UPDATE merchant.command_receipt SET target_id="+BINDING,"DELETE FROM merchant.command_receipt","UPDATE merchant.business_audit SET policy_version='changed'"))rejects(db,"42501",sql);
        try(var c=db.runtimeConnection();var s=c.createStatement();var r=s.executeQuery("SELECT row_to_json(t)::text FROM merchant.command_receipt t WHERE target_type='CUSTOMER_ADDRESS'")){assertTrue(r.next());assertFalse(r.getString(1).contains("Synthetic contact"));assertFalse(r.getString(1).contains("901234567"));}
    }


    @Test void shippingDraftV19UpgradePreservesV18DataAndNarrowPrivileges()throws Exception{
        Database db=PostgresTestcontainersSupport.database(POSTGRES);assertEquals(18,db.flyway("18").migrate().migrationsExecuted);account(db);profileFixture(db);addressFixture(db);var profile=snapshot(db,"merchant.customer_profile");var addresses=snapshot(db,"merchant.customer_address");var addressHistory=snapshot(db,"merchant.customer_address_version");var checksums=Arrays.stream(db.flyway("18").info().applied()).map(m->m.getChecksum()).toList();
        var source=new org.springframework.jdbc.datasource.DriverManagerDataSource(db.url(),db.runtime(),db.runtimePassword());assertEquals("SHIPPING_DRAFT_POSTGRES18_V19_REQUIRED",assertThrows(IllegalStateException.class,()->com.elogistic.backend.orders.internal.infrastructure.JdbcShippingDraftStore.requireSchema(source)).getMessage());assertEquals(7,db.flyway("latest").migrate().migrationsExecuted);db.flyway("latest").validate();assertEquals(0,db.flyway("latest").migrate().migrationsExecuted);assertEquals(checksums,Arrays.stream(db.flyway("latest").info().applied()).limit(18).map(m->m.getChecksum()).toList());assertEquals(profile,snapshot(db,"merchant.customer_profile"));assertEquals(addresses,snapshot(db,"merchant.customer_address"));assertEquals(addressHistory,snapshot(db,"merchant.customer_address_version"));assertDoesNotThrow(()->com.elogistic.backend.orders.internal.infrastructure.JdbcShippingDraftStore.requireSchema(source));
        for(String sql:List.of("DELETE FROM orders.shipping_draft","UPDATE orders.shipping_draft_version SET payload='{}'","DELETE FROM orders.shipping_draft_audit","UPDATE orders.shipping_draft_receipt SET original_version=2","ALTER TABLE orders.shipping_draft ADD COLUMN injected BOOLEAN"))rejects(db,"42501",sql);
    }

    @Test void pilotV20UpgradePreservesV19DataAndSeedsExactlyApprovedSimulation()throws Exception{
        Database db=PostgresTestcontainersSupport.database(POSTGRES);assertEquals(19,db.flyway("19").migrate().migrationsExecuted);account(db);profileFixture(db);addressFixture(db);var identity=snapshot(db,"identity.user_account");var address=snapshot(db,"merchant.customer_address");var history=snapshot(db,"merchant.customer_address_version");var checksums=Arrays.stream(db.flyway("19").info().applied()).map(m->m.getChecksum()).toList();assertEquals(1,db.flyway("20").migrate().migrationsExecuted);db.flyway("20").validate();assertEquals(0,db.flyway("20").migrate().migrationsExecuted);assertEquals(checksums,Arrays.stream(db.flyway("20").info().applied()).limit(19).map(m->m.getChecksum()).toList());assertEquals(identity,snapshot(db,"identity.user_account"));assertEquals(address,snapshot(db,"merchant.customer_address"));assertEquals(history,snapshot(db,"merchant.customer_address_version"));
        try(Connection c=db.runtimeConnection();Statement q=c.createStatement();ResultSet r=q.executeQuery("SELECT origin_code||'>'||destination_code||':'||classification FROM merchant.pilot_route ORDER BY origin_code,destination_code")){var routes=new ArrayList<String>();while(r.next())routes.add(r.getString(1));assertEquals(List.of("SIM-A1>SIM-A2:INTRA","SIM-A1>SIM-B1:INTER","SIM-A2>SIM-A1:INTRA","SIM-B1>SIM-A1:INTER"),routes);}
        rejects(db,"42501","INSERT INTO merchant.pilot_route VALUES ('ARGO-SIM-COVERAGE-1','SIM-A2','SIM-B1','ROAD_STANDARD','INTER')");rejects(db,"42501","UPDATE pricing.pilot_tariff_version SET intra_first_vnd=1");rejects(db,"42501","UPDATE finance.pilot_shipping_obligation SET state='PAID'");rejects(db,"42501","DELETE FROM orders.pilot_shipment");rejects(db,"42501","UPDATE orders.pilot_parcel SET weight_grams=1");rejects(db,"42501","ALTER TABLE pricing.pilot_quote ADD COLUMN injected BOOLEAN");
        assertThrows(SQLException.class,()->db.ownerExecute("UPDATE pricing.pilot_tariff_version SET intra_first_vnd=1"));assertThrows(SQLException.class,()->db.ownerExecute("UPDATE merchant.pilot_route SET classification='INTER'"));
    }
    @Test void pilotV20DatabaseRejectsForgedPricesAndPaidObligations()throws Exception{
        Database db=migrated();rejects(db,"23514","INSERT INTO finance.pilot_shipping_obligation VALUES ("+PROOF+","+ACCOUNT+","+SESSION+","+BINDING+",25000,'VND','PAID','SENDER','CASH_ON_HANDOVER','BR13-LOCAL-PILOT-01',"+KEY+","+NOW+")");
        try(Connection c=db.runtimeConnection();Statement q=c.createStatement();ResultSet r=q.executeQuery("SELECT pricing.valid_pilot_quote_breakdown('[{\"weightGrams\":500,\"lengthMm\":100,\"widthMm\":100,\"heightMm\":100}]'::jsonb,'[{\"parcelNumber\":1,\"actualGrams\":500,\"chargeableGrams\":500,\"feeVnd\":1}]'::jsonb,1,'ARGO-ROAD-TARIFF-1','INTRA')")){assertTrue(r.next());assertFalse(r.getBoolean(1));}
        rejects(db,"42501","UPDATE pricing.pilot_quote SET total_vnd=1");rejects(db,"42501","DELETE FROM pricing.pilot_quote");rejects(db,"42501","UPDATE orders.pilot_booking_receipt SET draft_version=2");
    }
    @Test void stationV21UpgradePreservesV20SnapshotsAndSeedsNoStaffAuthority()throws Exception{
        Database db=PostgresTestcontainersSupport.database(POSTGRES);assertEquals(20,db.flyway("20").migrate().migrationsExecuted);account(db);profileFixture(db);addressFixture(db);var identity=snapshot(db,"identity.user_account");var address=snapshot(db,"merchant.customer_address");var tariff=snapshot(db,"(SELECT version AS id,* FROM pricing.pilot_tariff_version)");var checksums=Arrays.stream(db.flyway("20").info().applied()).map(m->m.getChecksum()).toList();assertEquals(5,db.flyway("latest").migrate().migrationsExecuted);db.flyway("latest").validate();assertEquals(identity,snapshot(db,"identity.user_account"));assertEquals(address,snapshot(db,"merchant.customer_address"));assertEquals(tariff,snapshot(db,"(SELECT version AS id,* FROM pricing.pilot_tariff_version)"));assertEquals(checksums,Arrays.stream(db.flyway("latest").info().applied()).limit(20).map(m->m.getChecksum()).toList());
        try(Connection c=db.runtimeConnection();Statement q=c.createStatement();ResultSet r=q.executeQuery("SELECT (SELECT count(*) FROM dispatch.pilot_station),(SELECT count(*) FROM dispatch.pilot_station_assignment),(SELECT count(*) FROM dispatch.pilot_station WHERE NOT pickup_enabled AND active AND drop_off_enabled AND provenance='SIMULATION')")){assertTrue(r.next());assertEquals(3,r.getInt(1));assertEquals(0,r.getInt(2));assertEquals(3,r.getInt(3));}
        rejects(db,"42501","UPDATE dispatch.pilot_station SET active=FALSE,version=version+1");rejects(db,"42501","INSERT INTO dispatch.pilot_station_assignment VALUES ('10000000-0000-4000-8000-000000000099',"+ACCOUNT+",'SIM-DROP-A1','STATION_INTAKE','ACTIVE','2026-11-01',NULL,1)");rejects(db,"42501","UPDATE orders.pilot_tracking_event SET event_type='DELIVERED'");rejects(db,"42501","DELETE FROM dispatch.pilot_intake_acceptance");
    }
    @Test void stationV21DatabaseRejectsUnapprovedNetworkAndMutableAuthorityIdentity()throws Exception{
        Database db=migrated();assertThrows(SQLException.class,()->db.ownerExecute("INSERT INTO dispatch.pilot_station VALUES ('REAL-FAKE','ARGO-SIM-COVERAGE-1',TRUE,TRUE,FALSE,'SIMULATION',1)"));assertThrows(SQLException.class,()->db.ownerExecute("UPDATE dispatch.pilot_station SET pickup_enabled=TRUE,version=version+1 WHERE code='SIM-DROP-A1'"));assertThrows(SQLException.class,()->db.ownerExecute("UPDATE dispatch.pilot_station SET active=FALSE WHERE code='SIM-DROP-A1'"));
        db.ownerExecute("INSERT INTO dispatch.pilot_station_assignment(id,account_id,station_code,permission,state,effective_from,version) VALUES ('10000000-0000-4000-8000-000000000099',"+ACCOUNT+",'SIM-DROP-A1','STATION_INTAKE','ACTIVE','2026-11-01',1)");assertThrows(SQLException.class,()->db.ownerExecute("UPDATE dispatch.pilot_station_assignment SET account_id="+OTHER+",version=version+1"));
    }

    @Test void stationV21OwnerProvisioningIsExplicitAuditedAndNotRuntimeCallable()throws Exception{
        Database db=migrated();account(db);rejects(db,"42501","SELECT dispatch.configure_pilot_station_staff("+ACCOUNT+",'SIM-DROP-A1','GRANT')");assertThrows(SQLException.class,()->db.ownerExecute("SELECT dispatch.configure_pilot_station_staff("+ACCOUNT+",'SIM-DROP-A1','GRANT')"));
        db.ownerExecute("DO $$ BEGIN PERFORM set_config('argopost.operator_environment','TEST',TRUE); PERFORM dispatch.configure_pilot_station_staff("+ACCOUNT+",'SIM-DROP-A1','GRANT'); PERFORM dispatch.configure_pilot_station_staff("+ACCOUNT+",'SIM-DROP-A1','GRANT'); IF (SELECT count(*) FROM dispatch.pilot_station_assignment_audit)<>1 THEN RAISE EXCEPTION 'OPERATOR_REPLAY_DUPLICATED'; END IF; PERFORM dispatch.configure_pilot_station_staff("+ACCOUNT+",'SIM-DROP-A1','REVOKE'); IF (SELECT count(*) FROM dispatch.pilot_station_assignment_audit)<>2 THEN RAISE EXCEPTION 'OPERATOR_AUDIT_REQUIRED'; END IF; END $$");
        try(Connection c=db.runtimeConnection();Statement q=c.createStatement();ResultSet r=q.executeQuery("SELECT count(*) FROM dispatch.pilot_station_assignment WHERE state='REVOKED' AND version=2")){assertTrue(r.next());assertEquals(1,r.getInt(1));}rejects(db,"42501","SELECT * FROM dispatch.pilot_station_assignment_audit");
    }


    @Test void businessV25UpgradePreservesPopulatedPreparationHistoryAndReceipts()throws Exception{
        Database db=PostgresTestcontainersSupport.database(POSTGRES);assertEquals(24,db.flyway("24").migrate().migrationsExecuted);account(db);
        execute(db,"UPDATE identity.user_account SET status='ACTIVE',activated_at="+NOW+",email_verified=TRUE,password_hash="+HASH+" WHERE id="+ACCOUNT);
        execute(db,"INSERT INTO identity.customer_identifier VALUES("+OTHER+","+ACCOUNT+",'identity@example.invalid',"+NOW+",'ACTIVE',1)");
        db.ownerExecute("BEGIN;SET LOCAL argopost.operator_environment='TEST';SELECT merchant.configure_local_workspace("+KEY+",'PROVISION',"+PROOF+",'Synthetic upgrade',"+ACCOUNT+",'OWNER',ARRAY['dashboard.read','shipment.draft.read','shipment.draft.write'],'Synthetic V24 baseline');COMMIT");
        var address=new com.elogistic.backend.orders.api.ShippingDrafts.Address("VN","Synthetic area","Synthetic locality","Synthetic street");var contact=new com.elogistic.backend.orders.api.ShippingDrafts.Contact("Synthetic contact","+84901234567",address);
        var input=new com.elogistic.backend.orders.api.ShippingDrafts.Input(null,null,contact,contact,List.of(new com.elogistic.backend.orders.api.ShippingDrafts.Parcel("Synthetic parcel",1,500,100,100,100,0)));
        var draft=new com.elogistic.backend.orders.api.BusinessPreparations.Draft(UUID.fromString(SESSION.replace("'","")),UUID.fromString(PROOF.replace("'","")),null,UUID.fromString(ACCOUNT.replace("'","")),"DRAFT","DRAFT",1,input,null,null,java.time.Instant.parse("2026-10-21T00:00:00Z"),java.time.Instant.parse("2026-10-21T00:00:00Z"),List.of());
        var json=tools.jackson.databind.json.JsonMapper.builder().build();
        execute(db,"BEGIN;INSERT INTO orders.organization_preparation VALUES("+SESSION+","+PROOF+",NULL,"+ACCOUNT+",'DRAFT','DRAFT',1,NULL,NULL,'"+json.writeValueAsString(input)+"',"+NOW+","+NOW+");INSERT INTO orders.organization_preparation_version VALUES("+SESSION+","+PROOF+",1,'"+json.writeValueAsString(draft)+"',"+NOW+");INSERT INTO orders.organization_preparation_audit VALUES("+OTHER_SESSION+","+PROOF+","+ACCOUNT+","+SESSION+",'CREATE',0,1,NULL,'DRAFT',"+BINDING+","+NOW+");INSERT INTO orders.organization_preparation_receipt VALUES("+PROOF+",'CREATE',"+TOKEN+","+ACCOUNT+",NULL,"+SESSION+",0,1,'v1',"+digest(1)+","+NOW+");COMMIT");
        Map<String,List<String>> before=new LinkedHashMap<>();String stripped="(to_jsonb(t)-'result_snapshot'-'preparation_target'-'preparation_version'-'import_target')::text";
        var tables=List.of("orders.organization_preparation","orders.organization_preparation_version","orders.organization_preparation_audit","orders.organization_preparation_receipt");
        for(String table:tables)try(var c=db.runtimeConnection();var q=c.createStatement();var r=q.executeQuery("SELECT "+stripped+" FROM "+table+" t ORDER BY 1")){var rows=new ArrayList<String>();while(r.next())rows.add(r.getString(1));before.put(table,rows);}
        var checksums=Arrays.stream(db.flyway("24").info().applied()).map(m->m.getChecksum()).toList();assertEquals(1,db.flyway("latest").migrate().migrationsExecuted);db.flyway("latest").validate();assertEquals(checksums,Arrays.stream(db.flyway("latest").info().applied()).limit(24).map(m->m.getChecksum()).toList());
        for(String table:tables)try(var c=db.runtimeConnection();var q=c.createStatement();var r=q.executeQuery("SELECT "+stripped+" FROM "+table+" t ORDER BY 1")){var rows=new ArrayList<String>();while(r.next())rows.add(r.getString(1));assertEquals(before.get(table),rows);}
        assertEquals(0,snapshot(db,"orders.organization_import_job").size());rejects(db,"42501","DELETE FROM orders.organization_import_job");assertEquals(0,db.flyway("latest").migrate().migrationsExecuted);
    }
    @Test void businessV24UpgradePreservesPopulatedOrganizationAndOperatorReceipt()throws Exception{
        Database db=PostgresTestcontainersSupport.database(POSTGRES);assertEquals(23,db.flyway("23").migrate().migrationsExecuted);account(db);
        execute(db,"UPDATE identity.user_account SET status='ACTIVE',activated_at="+NOW+",email_verified=TRUE,password_hash="+HASH+" WHERE id="+ACCOUNT);
        execute(db,"INSERT INTO identity.customer_identifier VALUES("+OTHER+","+ACCOUNT+",'identity@example.invalid',"+NOW+",'ACTIVE',1)");
        String command="SELECT merchant.configure_local_workspace("+KEY+",'PROVISION',"+PROOF+",'Synthetic upgrade organization',"+ACCOUNT+",'OWNER',ARRAY['dashboard.read'],'Synthetic legacy receipt')";
        db.ownerExecute("BEGIN;SET LOCAL argopost.operator_environment='TEST';"+command+";COMMIT");
        List<String> tables=List.of("merchant.organization","merchant.membership","merchant.member_grant","merchant.workspace_audit","merchant.workspace_command_receipt");Map<String,List<String>> before=new LinkedHashMap<>();
        for(String table:tables)try(var c=db.migrationConnection();var q=c.createStatement()){q.execute("SET ROLE "+db.owner());try(var r=q.executeQuery("SELECT to_jsonb(t)::text FROM "+table+" t ORDER BY 1")){var rows=new ArrayList<String>();while(r.next())rows.add(r.getString(1));before.put(table,rows);}}
        var sums=Arrays.stream(db.flyway("23").info().applied()).map(m->m.getChecksum()).toList();assertEquals(2,db.flyway("latest").migrate().migrationsExecuted);db.flyway("latest").validate();
        assertEquals(sums,Arrays.stream(db.flyway("latest").info().applied()).limit(23).map(m->m.getChecksum()).toList());
        db.ownerExecute("BEGIN;SET LOCAL argopost.operator_environment='TEST';"+command+";COMMIT");
        for(String table:tables)try(var c=db.migrationConnection();var q=c.createStatement()){q.execute("SET ROLE "+db.owner());try(var r=q.executeQuery("SELECT "+(table.endsWith("member_grant")?"(to_jsonb(t)-'facility_id')":"to_jsonb(t)")+"::text FROM "+table+" t ORDER BY 1")){var rows=new ArrayList<String>();while(r.next())rows.add(r.getString(1));assertEquals(before.get(table),rows);}}
        rejects(db,"42501","INSERT INTO merchant.facility VALUES(gen_random_uuid(),"+PROOF+",'Synthetic','{}','ACTIVE',1,now(),now())");
        rejects(db,"42501","SELECT merchant.configure_local_workspace(gen_random_uuid(),'GRANT',"+PROOF+",NULL,"+ACCOUNT+",'OWNER',ARRAY['dashboard.read','shipment.draft.read','shipment.draft.write'],'Synthetic denied runtime grant')");
        assertEquals(0,snapshot(db,"orders.organization_preparation").size());assertEquals(0,db.flyway("latest").migrate().migrationsExecuted);
    }
    @Test void businessV23UpgradePreservesV22AndCannotGrantRuntimeAuthority()throws Exception{
        Database db=PostgresTestcontainersSupport.database(POSTGRES);assertEquals(22,db.flyway("22").migrate().migrationsExecuted);account(db);profileFixture(db);addressFixture(db);
        var source=new org.springframework.jdbc.datasource.DriverManagerDataSource(db.url(),db.runtime(),db.runtimePassword());assertEquals("BUSINESS_WORKSPACE_POSTGRES18_SCHEMA_V23_REQUIRED",assertThrows(IllegalStateException.class,()->com.elogistic.backend.merchant.internal.infrastructure.JdbcBusinessWorkspaces.requireSchema(source)).getMessage());
        var identity=snapshot(db,"identity.user_account");var addresses=snapshot(db,"merchant.customer_address");var applications=snapshot(db,"merchant.application");var checksums=Arrays.stream(db.flyway("22").info().applied()).map(m->m.getChecksum()).toList();
        assertEquals(3,db.flyway("latest").migrate().migrationsExecuted);db.flyway("latest").validate();assertEquals(0,db.flyway("latest").migrate().migrationsExecuted);
        assertDoesNotThrow(()->com.elogistic.backend.merchant.internal.infrastructure.JdbcBusinessWorkspaces.requireSchema(source));
        assertEquals(checksums,Arrays.stream(db.flyway("latest").info().applied()).limit(22).map(m->m.getChecksum()).toList());assertEquals(identity,snapshot(db,"identity.user_account"));assertEquals(addresses,snapshot(db,"merchant.customer_address"));assertEquals(applications,snapshot(db,"merchant.application"));
        for(String table:List.of("organization","membership","member_grant"))try(var c=db.runtimeConnection();var s=c.createStatement();var r=s.executeQuery("SELECT count(*) FROM merchant."+table)){assertTrue(r.next());assertEquals(0,r.getInt(1));}
        rejects(db,"42501","INSERT INTO merchant.organization VALUES(gen_random_uuid(),'Synthetic','ACTIVE','LOCAL_OPERATOR_PROVISIONED','TEST',gen_random_uuid(),1,now(),now())");
        rejects(db,"42501","SELECT identity.lock_local_business_subject("+ACCOUNT+",TRUE)");
        rejects(db,"42501","SELECT merchant.configure_local_workspace(gen_random_uuid(),'PROVISION',gen_random_uuid(),'Synthetic',"+ACCOUNT+",'OWNER',ARRAY['dashboard.read'],'Synthetic reason')");
        assertThrows(SQLException.class,()->db.ownerExecute("SET argopost.operator_environment='TEST';SELECT merchant.configure_local_workspace(gen_random_uuid(),'PROVISION',gen_random_uuid(),'Synthetic',"+ACCOUNT+",'OWNER',ARRAY['dashboard.read'],'Synthetic reason')"));
    }
    @Test void supportV22UpgradePreservesV21DataAndRequiresOperatorAuthority()throws Exception{
        Database db=PostgresTestcontainersSupport.database(POSTGRES);assertEquals(21,db.flyway("21").migrate().migrationsExecuted);account(db);profileFixture(db);addressFixture(db);
        var identity=snapshot(db,"identity.user_account");var addresses=snapshot(db,"merchant.customer_address");var stations=snapshot(db,"(SELECT code AS id,* FROM dispatch.pilot_station)");
        var checksums=Arrays.stream(db.flyway("21").info().applied()).map(m->m.getChecksum()).toList();assertEquals(4,db.flyway("latest").migrate().migrationsExecuted);db.flyway("latest").validate();assertEquals(0,db.flyway("latest").migrate().migrationsExecuted);
        assertEquals(checksums,Arrays.stream(db.flyway("latest").info().applied()).limit(21).map(m->m.getChecksum()).toList());assertEquals(identity,snapshot(db,"identity.user_account"));assertEquals(addresses,snapshot(db,"merchant.customer_address"));assertEquals(stations,snapshot(db,"(SELECT code AS id,* FROM dispatch.pilot_station)"));
        rejects(db,"42501","INSERT INTO orders.support_staff_grant VALUES(gen_random_uuid(),"+ACCOUNT+",'SUPPORT_TRIAGE','ACTIVE',1,clock_timestamp())");rejects(db,"42501","SELECT orders.configure_local_support_staff("+ACCOUNT+",'SUPPORT_TRIAGE','GRANT')");
        try(var c=db.runtimeConnection();var q=c.createStatement();var r=q.executeQuery("SELECT (SELECT count(*) FROM orders.support_staff_grant)+(SELECT count(*) FROM orders.personal_support_ticket)")){assertTrue(r.next());assertEquals(0,r.getInt(1));}
        assertThrows(SQLException.class,()->db.ownerExecute("SELECT orders.configure_local_support_staff("+ACCOUNT+",'SUPPORT_TRIAGE','GRANT')"));
        assertThrows(SQLException.class,()->db.ownerExecute("SET argopost.operator_environment='TEST';SELECT orders.configure_local_support_staff("+ACCOUNT+",'SUPPORT_TRIAGE','GRANT')"));
        // Explicit synthetic fixture activation only, after preserved V21 data/checksum assertions.
        db.ownerExecute("UPDATE identity.user_account SET status='ACTIVE',activated_at="+NOW+",email_verified=TRUE,password_hash="+HASH+" WHERE id="+ACCOUNT);
        db.ownerExecute("SET argopost.operator_environment='TEST';SELECT orders.configure_local_support_staff("+ACCOUNT+",'SUPPORT_TRIAGE','GRANT');SELECT orders.configure_local_support_staff("+ACCOUNT+",'SUPPORT_TRIAGE','REVOKE')");
        try(var c=db.runtimeConnection();var q=c.createStatement();var r=q.executeQuery("SELECT count(*) FROM orders.support_business_audit WHERE authority_grant_id IS NOT NULL AND operator_principal IS NOT NULL")){assertTrue(r.next());assertEquals(2,r.getInt(1));}
    }

}
