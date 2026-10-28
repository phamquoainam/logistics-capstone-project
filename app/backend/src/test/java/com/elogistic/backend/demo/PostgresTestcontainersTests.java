package com.elogistic.backend.demo;

import com.elogistic.backend.testsupport.PostgresTestcontainersSupport;
import com.elogistic.backend.testsupport.PostgresTestcontainersSupport.Database;
import java.sql.*;
import java.util.*;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.parallel.*;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import static org.junit.jupiter.api.Assertions.*;

@Tag("task-c")
@Testcontainers
@Execution(ExecutionMode.SAME_THREAD)
class PostgresTestcontainersTests {
    @Container static final PostgreSQLContainer POSTGRES=PostgresTestcontainersSupport.container();

    @AfterAll static void verifyFinalPolicy() throws Exception { PostgresTestcontainersSupport.verifyRuntime(POSTGRES); }

    @Test void actualPostgres18UsesPinnedIsolatedContainer() throws Exception {
        PostgresTestcontainersSupport.verifyRuntime(POSTGRES);
        try(Connection c=DriverManager.getConnection(PostgresTestcontainersSupport.currentUrl(POSTGRES,"postgres"),POSTGRES.getUsername(),POSTGRES.getPassword());Statement s=c.createStatement()) {
            assertEquals("PostgreSQL",c.getMetaData().getDatabaseProductName());assertEquals(18,c.getMetaData().getDatabaseMajorVersion());
            for(String setting:List.of("fsync","synchronous_commit")) try(ResultSet r=s.executeQuery("SHOW "+setting)) { assertTrue(r.next());assertEquals("on",r.getString(1)); }
        }
    }

    @Test void coldStartAppliesV1AndV2() throws Exception {
        Database db=PostgresTestcontainersSupport.database(POSTGRES);
        Flyway flyway=db.flyway("latest");assertEquals(0,flyway.info().applied().length);
        // Retain the report-policy method identity and assert the complete current stream.
        assertEquals(11,flyway.migrate().migrationsExecuted);assertTrue(flyway.validateWithResult().validationSuccessful);
        assertEquals(List.of("1","2","3","4","5","6","7","8","9","10","11"),Arrays.stream(flyway.info().applied()).map(m->m.getVersion().toString()).toList());
    }

    private List<String> checksums(Flyway flyway) { return Arrays.stream(flyway.info().applied()).map(m->m.getVersion()+":"+m.getChecksum()).toList(); }
    private void marker(Database db) throws SQLException {
        try(Connection c=db.migrationConnection();Statement s=c.createStatement()) {
            s.execute("SET ROLE "+db.owner());s.execute("CREATE TABLE platform.task_c_marker(id integer PRIMARY KEY,value text NOT NULL)");
            s.execute("INSERT INTO platform.task_c_marker VALUES (1,'preserve-task-c')");
        }
    }
    private void assertMarker(Database db) throws SQLException {
        try(Connection c=db.migrationConnection();Statement s=c.createStatement()) {
            s.execute("SET ROLE "+db.owner());try(ResultSet r=s.executeQuery("SELECT value FROM platform.task_c_marker WHERE id=1")) { assertTrue(r.next());assertEquals("preserve-task-c",r.getString(1)); }
        }
    }

    @Test void validateAndRerunPreserveDataAndChecksums() throws Exception {
        Database db=PostgresTestcontainersSupport.database(POSTGRES);Flyway flyway=db.flyway("latest");flyway.migrate();marker(db);
        var before=checksums(flyway);flyway.validate();assertEquals(0,flyway.migrate().migrationsExecuted);
        assertEquals(before,checksums(flyway));assertMarker(db);
    }

    @Test void upgradeV1ToV2PreservesExistingData() throws Exception {
        Database db=PostgresTestcontainersSupport.database(POSTGRES);assertEquals(1,db.flyway("1").migrate().migrationsExecuted);marker(db);
        Flyway latest=db.flyway("2");assertEquals(1,latest.migrate().migrationsExecuted);latest.validate();
        assertEquals(2,latest.info().applied().length);assertMarker(db);assertEquals(0,latest.migrate().migrationsExecuted);
    }

    @Test void schemasAndObjectsBelongToOwner() throws Exception {
        Database db=PostgresTestcontainersSupport.database(POSTGRES);db.flyway("latest").migrate();
        try(Connection c=db.migrationConnection();Statement s=c.createStatement()) {
            try(ResultSet r=s.executeQuery("SELECT session_user,current_user")) { assertTrue(r.next());assertEquals(db.migrator(),r.getString(1));assertEquals(db.migrator(),r.getString(2)); }
            s.execute("SET ROLE "+db.owner());
            try(ResultSet r=s.executeQuery("SELECT current_user")) { assertTrue(r.next());assertEquals(db.owner(),r.getString(1)); }
            try(ResultSet r=s.executeQuery("SELECT count(*) FROM pg_namespace WHERE nspowner=(SELECT oid FROM pg_roles WHERE rolname='"+db.owner()+"') AND nspname IN ('platform','identity','merchant','orders','pricing','warehouse','dispatch','finance','routing','notification')")) { assertTrue(r.next());assertEquals(10,r.getInt(1)); }
            // Five Demo + twelve accepted Identity + nine customer + four personal-profile tables + Flyway history.
            try(ResultSet r=s.executeQuery("SELECT count(*) FROM pg_tables WHERE tableowner='"+db.owner()+"' AND schemaname IN ('platform','identity','merchant','orders')")) { assertTrue(r.next());assertEquals(31,r.getInt(1)); }
            Set<String> expectedTables=Set.of("platform.flyway_schema_history", "identity.demo_account",
                "merchant.demo_merchant", "orders.demo_shipment", "orders.demo_event", "orders.demo_operation",
                "identity.user_account", "identity.account_grant", "identity.auth_session", "identity.refresh_token",
                "identity.action_proof", "identity.csrf_binding", "identity.command_receipt", "identity.audit_entry", "identity.realm", "identity.invitation_outbox", "identity.rate_limit_subject", "identity.rate_limit_attempt", "identity.customer_identifier", "identity.customer_auth_session", "identity.customer_refresh_token", "identity.customer_csrf_binding", "identity.customer_enrollment", "identity.customer_proof", "identity.customer_delivery", "identity.customer_command_receipt", "identity.customer_proof_receipt",
                "merchant.customer_profile", "merchant.customer_profile_version", "merchant.business_audit", "merchant.command_receipt");
            Map<String,String> actualTables=new TreeMap<>();
            try(ResultSet r=s.executeQuery("SELECT schemaname||'.'||tablename,tableowner FROM pg_tables WHERE schemaname IN ('platform','identity','merchant','orders')")) {
                while(r.next()) actualTables.put(r.getString(1),r.getString(2));
            }
            assertEquals(expectedTables,actualTables.keySet());
            assertTrue(actualTables.values().stream().allMatch(db.owner()::equals),"Every expected table must retain the authorized owner");
            Set<String> realmFunctions=new HashSet<>();
            try(ResultSet r=s.executeQuery("SELECT p.proname,pg_get_userbyid(p.proowner),p.prosecdef FROM pg_proc p JOIN pg_namespace n ON n.oid=p.pronamespace WHERE n.nspname='identity' AND p.proname IN ('bootstrap_system_realm_admin','activate_system_realm')")) {
                while(r.next()) { assertTrue(realmFunctions.add(r.getString(1)));assertEquals(db.owner(),r.getString(2));assertFalse(r.getBoolean(3)); }
            }
            assertEquals(Set.of("bootstrap_system_realm_admin","activate_system_realm"),realmFunctions);
        }
    }

    @Test void runtimeCannotDdlAssumeOwnerOrReadHistory() throws Exception {
        Database db=PostgresTestcontainersSupport.database(POSTGRES);db.flyway("latest").migrate();
        for(String statement:List.of("CREATE SCHEMA forbidden_runtime","CREATE TABLE orders.forbidden_runtime(id integer)","CREATE TABLE public.forbidden_runtime(id integer)","ALTER TABLE orders.demo_shipment ADD COLUMN forbidden integer","DROP TABLE orders.demo_event","TRUNCATE orders.demo_shipment","SET ROLE "+db.owner(),"SELECT * FROM platform.flyway_schema_history")) {
            try(Connection c=db.runtimeConnection();Statement s=c.createStatement()) { assertEquals(db.runtime(),c.getMetaData().getUserName());assertEquals("42501",assertThrows(SQLException.class,()->s.execute(statement)).getSQLState()); }
        }
        try(Connection c=db.runtimeConnection();Statement s=c.createStatement()) {
            try(ResultSet r=s.executeQuery("SELECT rolcanlogin FROM pg_roles WHERE rolname='"+db.owner()+"'")) { assertTrue(r.next());assertFalse(r.getBoolean(1)); }
            try(ResultSet r=s.executeQuery("SELECT bool_or(rolsuper OR rolcreatedb OR rolcreaterole) FROM pg_roles WHERE rolname IN ('"+db.runtime()+"','"+db.migrator()+"')")) { assertTrue(r.next());assertFalse(r.getBoolean(1)); }
        }
    }

    @Test void restartPreservesCommittedDataAndHistory() throws Exception {
        Database db=PostgresTestcontainersSupport.database(POSTGRES);Flyway flyway=db.flyway("latest");flyway.migrate();marker(db);var before=checksums(flyway);
        String id=POSTGRES.getContainerId();PostgresTestcontainersSupport.restart(POSTGRES);
        assertEquals(id,POSTGRES.getContainerId());assertMarker(db);assertEquals(before,checksums(db.flyway("latest")));
        db.flyway("latest").validate();assertEquals(0,db.flyway("latest").migrate().migrationsExecuted);
    }
}
