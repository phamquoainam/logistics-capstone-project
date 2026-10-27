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
        assertEquals(7, db.flyway("latest").migrate().migrationsExecuted);
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
        assertEquals(List.of("1","2","3","4","5","6","7"), Arrays.stream(flyway.info().applied()).map(m -> m.getVersion().toString()).toList());
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
        assertEquals(5, db.flyway("latest").migrate().migrationsExecuted); db.flyway("latest").validate();
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
        assertEquals(4,db.flyway("latest").migrate().migrationsExecuted);db.flyway("latest").validate();
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
        assertEquals(3,db.flyway("latest").migrate().migrationsExecuted);db.flyway("latest").validate();assertEquals(0,db.flyway("latest").migrate().migrationsExecuted);
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
        assertEquals(1,db.flyway("latest").migrate().migrationsExecuted);db.flyway("latest").validate();
        assertEquals(checksums,Arrays.stream(db.flyway("latest").info().applied()).limit(6).map(m->m.getChecksum()).toList());
        assertEquals(rows,snapshot(db,"identity.user_account"));assertEquals(0,db.flyway("latest").migrate().migrationsExecuted);
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
}
