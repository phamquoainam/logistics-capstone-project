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

    private String intakeGuardAcl(Database db)throws Exception{
        try(var c=db.runtimeConnection();var q=c.createStatement();var r=q.executeQuery("SELECT string_agg(p.oid::text||':'||p.proowner::text||':'||p.prosecdef::text||':'||COALESCE(p.proacl::text,''),'|' ORDER BY p.oid) FROM pg_proc p WHERE p.oid IN ('dispatch.require_complete_pilot_custody()'::regprocedure,'orders.require_complete_pilot_tracking()'::regprocedure)")){assertTrue(r.next());return r.getString(1);}
    }
    @Test void nwsForwardUpgradePreservesV43RowsChecksumsAndPrivateGuardBoundary()throws Exception {
        Database db=PostgresTestcontainersSupport.database(POSTGRES);assertEquals(42,db.flyway("43").migrate().migrationsExecuted);account(db);profileFixture(db);addressFixture(db);
        var sums=Arrays.stream(db.flyway("43").info().applied()).map(m->m.getVersion()+":"+m.getChecksum()).toList();var accounts=snapshot(db,"identity.user_account");var profiles=snapshot(db,"merchant.customer_profile");var addresses=snapshot(db,"merchant.customer_address");String guardsBefore=intakeGuardAcl(db);
        assertEquals(6,db.flyway("latest").migrate().migrationsExecuted);assertTrue(db.flyway("latest").validateWithResult().validationSuccessful);assertEquals(0,db.flyway("latest").migrate().migrationsExecuted);
        assertEquals(sums,Arrays.stream(db.flyway("latest").info().applied()).limit(42).map(m->m.getVersion()+":"+m.getChecksum()).toList());assertEquals(accounts,snapshot(db,"identity.user_account"));assertEquals(profiles,snapshot(db,"merchant.customer_profile"));assertEquals(addresses,snapshot(db,"merchant.customer_address"));
        assertEquals(guardsBefore,intakeGuardAcl(db),"Guard signature/OID/owner/invoker/ACL must be preserved");
        try(var c=db.runtimeConnection();var q=c.createStatement()){
            for(String table:List.of("orders.pilot_shipment","orders.pilot_parcel","orders.pilot_tracking_event","orders.pilot_parcel_event","dispatch.pilot_intake_scan","dispatch.pilot_intake_acceptance","dispatch.pilot_custody","dispatch.pilot_intake_audit","dispatch.pilot_intake_receipt"))try(var r=q.executeQuery("SELECT has_table_privilege(current_user,'"+table+"','SELECT'),has_table_privilege(current_user,'"+table+"','UPDATE'),has_table_privilege(current_user,'"+table+"','DELETE')")){assertTrue(r.next());assertTrue(r.getBoolean(1));assertFalse(r.getBoolean(2));assertFalse(r.getBoolean(3));}
            try(var r=q.executeQuery("SELECT count(*) FROM pg_proc p JOIN pg_roles o ON o.oid=p.proowner WHERE p.oid IN ('dispatch.require_complete_pilot_custody()'::regprocedure,'orders.require_complete_pilot_tracking()'::regprocedure) AND NOT p.prosecdef AND NOT o.rolcanlogin AND NOT EXISTS(SELECT 1 FROM aclexplode(COALESCE(p.proacl,acldefault('f',p.proowner))) a WHERE a.grantee=0 AND a.privilege_type='EXECUTE')")){assertTrue(r.next());assertEquals(2,r.getInt(1));}
        }
        for(String role:List.of("dispatch_network_writer","merchant_coverage_reader","elogistics_local_operations_operator"))rejects(db,"42501","SET ROLE "+role);
        try(var c=db.migrationConnection();var q=c.createStatement()){
            q.execute("SET ROLE dispatch_network_writer");assertEquals("23514",assertThrows(SQLException.class,()->q.execute("INSERT INTO dispatch.carrier_site VALUES(gen_random_uuid(),'FORGED','STATION',clock_timestamp())")).getSQLState());
        }
    }

    @Test void adminOnboardingForwardMigrationsPreserveHistoryAndPrivatePrivileges() throws Exception {
        Database db=PostgresTestcontainersSupport.database(POSTGRES);
        assertEquals(37,db.flyway("38").migrate().migrationsExecuted);
        var before=Arrays.stream(db.flyway("38").info().applied()).map(m->m.getVersion()+":"+m.getChecksum()).toList();
        boolean baselineControlExecute,baselinePublicControlExecute;
        try(var c=db.runtimeConnection();var s=c.createStatement();var r=s.executeQuery("SELECT has_function_privilege(current_user,'pg_catalog.pg_control_system()','EXECUTE'),EXISTS(SELECT 1 FROM pg_proc p CROSS JOIN LATERAL aclexplode(COALESCE(p.proacl,acldefault('f',p.proowner))) a WHERE p.oid='pg_catalog.pg_control_system()'::regprocedure AND a.grantee=0 AND a.privilege_type='EXECUTE')")){
            assertTrue(r.next());baselineControlExecute=r.getBoolean(1);baselinePublicControlExecute=r.getBoolean(2);
        }
        assertEquals(2,db.flyway("40").migrate().migrationsExecuted);
        db.flyway("40").validate();
        assertEquals(before,Arrays.stream(db.flyway("40").info().applied()).limit(37).map(m->m.getVersion()+":"+m.getChecksum()).toList());
        assertEquals(0,db.flyway("40").migrate().migrationsExecuted);
        try(var c=db.runtimeConnection();var s=c.createStatement()) {
            for(String table:List.of("identity.local_admin_environment","identity.local_admin_principal","identity.local_admin_operator_receipt")) {
                try(var r=s.executeQuery("SELECT has_table_privilege(current_user,'"+table+"','SELECT,INSERT,UPDATE,DELETE,TRUNCATE')")){assertTrue(r.next());assertFalse(r.getBoolean(1));}
            }
            for(String role:List.of("elogistics_local_admin_operator","identity_admin_writer","elogistics_local_realm_operator")) {
                try(var r=s.executeQuery("SELECT pg_has_role(current_user,'"+role+"','SET')")){assertTrue(r.next());assertFalse(r.getBoolean(1));}
            }
            try(var r=s.executeQuery("SELECT identity.local_admin_environment_matches('TEST')")){assertTrue(r.next());assertFalse(r.getBoolean(1));}
            for(String function:List.of("bind_local_admin_environment(uuid,uuid,text,text)","provision_local_admin(uuid,uuid,text,text,text[],text)","configure_local_admin_permissions(uuid,uuid,bigint,text[],text)","revoke_local_admin(uuid,uuid,bigint,text)","provision_local_realm_bootstrap(uuid,uuid,text,text,text)","activate_local_system_realm(uuid,uuid,text)","disable_local_system_realm(uuid,uuid,text)")){
                try(var r=s.executeQuery("SELECT has_function_privilege(current_user,'identity."+function+"','EXECUTE'),EXISTS(SELECT 1 FROM pg_proc p CROSS JOIN LATERAL aclexplode(p.proacl) a WHERE p.oid='identity."+function+"'::regprocedure AND a.grantee=0)")){assertTrue(r.next());assertFalse(r.getBoolean(1));assertFalse(r.getBoolean(2));}
            }
            try(var r=s.executeQuery("SELECT bool_and(NOT prosecdef) FROM pg_proc WHERE oid=ANY(ARRAY['identity.guard_local_admin_account()'::regprocedure,'identity.guard_local_admin_grant()'::regprocedure,'identity.guard_local_admin_delivery()'::regprocedure,'identity.guard_local_admin_operator_receipt()'::regprocedure])")){assertTrue(r.next());assertTrue(r.getBoolean(1));}
            try(var r=s.executeQuery("SELECT count(*) FROM pg_proc p JOIN pg_namespace n ON n.oid=p.pronamespace CROSS JOIN LATERAL aclexplode(p.proacl) a WHERE n.nspname IN('identity','merchant') AND(p.proname LIKE '%local_admin%' OR p.proname LIKE '%local_realm_bootstrap%' OR p.proname LIKE '%local_system_realm%') AND a.grantee=0")){assertTrue(r.next());assertEquals(0,r.getLong(1));}
            try(var r=s.executeQuery("SELECT has_function_privilege(current_user,'pg_catalog.pg_control_system()','EXECUTE')")){assertTrue(r.next());assertFalse(r.getBoolean(1),"AON raw-control boundary; baseline V38 runtime="+baselineControlExecute+" PUBLIC="+baselinePublicControlExecute);}
            for(String role:List.of("elogistics_local_admin_operator","elogistics_local_realm_operator")){
                try(var r=s.executeQuery("SELECT has_function_privilege('"+role+"','pg_catalog.pg_control_system()','EXECUTE')")){assertTrue(r.next());assertFalse(r.getBoolean(1));}
            }
            for(String role:List.of("identity_admin_writer","identity_staff_writer",db.owner())){
                try(var r=s.executeQuery("SELECT has_function_privilege('"+role+"','pg_catalog.pg_control_system()','EXECUTE')")){assertTrue(r.next());assertTrue(r.getBoolean(1));}
            }
        }
    }

    @Test void adminOnboardingGuardsPreserveUnmarkedStaffWriterUpdates() throws Exception {
        Database db=PostgresTestcontainersSupport.database(POSTGRES);
        db.flyway("39").migrate();
        UUID target=UUID.randomUUID();
        try(var c=db.runtimeConnection();var s=c.prepareStatement("INSERT INTO identity.user_account(id,username,display_name,email,status,created_at,updated_at) VALUES (?,?,? ,?,'PENDING_ACTIVATION',CURRENT_TIMESTAMP,CURRENT_TIMESTAMP)")) {
            s.setObject(1,target);s.setString(2,"synthetic-"+target);s.setString(3,"Synthetic unmarked");s.setString(4,target+"@example.invalid");s.executeUpdate();
        }
        // The existing controlled Staff definer legitimately updates this shared table.
        // This unmarked row must not require extra Admin authority or private table access.
        try(var c=db.migrationConnection();var s=c.createStatement()) {
            s.execute("SET ROLE identity_staff_writer");
            try(var r=s.executeQuery("SELECT current_user,has_function_privilege(current_user,p.oid,'EXECUTE'),p.proacl::text,pg_get_userbyid(p.proowner),pg_has_role(current_user,'identity_admin_writer','MEMBER') FROM pg_proc p WHERE p.oid='identity.local_admin_registered(uuid)'::regprocedure")) {
                assertTrue(r.next());assertEquals("identity_staff_writer",r.getString(1));
                assertTrue(r.getBoolean(2),"Approved AON-ACL-01 predicate must be callable");
                assertEquals("identity_admin_writer",r.getString(4));assertFalse(r.getBoolean(5));
            }
            try(var r=s.executeQuery("SELECT has_function_privilege(current_user,'identity.local_admin_eligible(uuid,uuid)','EXECUTE'),has_function_privilege(current_user,'identity.local_admin_environment_matches(text)','EXECUTE'),has_table_privilege(current_user,'identity.local_admin_principal','SELECT'),EXISTS(SELECT 1 FROM pg_proc p CROSS JOIN LATERAL aclexplode(p.proacl) x WHERE p.oid='identity.local_admin_registered(uuid)'::regprocedure AND x.grantee=0)")) {
                assertTrue(r.next());for(int i=1;i<=4;i++)assertFalse(r.getBoolean(i),"No authority beyond boolean predicate, column "+i);
            }
            assertEquals(1,assertDoesNotThrow(()->s.executeUpdate("UPDATE identity.user_account SET updated_at=updated_at WHERE id='"+target+"'")));
        }
    }

    @Test void adminEnvironmentBindingIsPhysicalRequestBoundAndTerminal()throws Exception {
        try(var fixture=com.elogistic.backend.testsupport.IsolatedAdminTestDatabase.create(POSTGRES)) {
            var db=fixture.database();db.flyway("39").migrate();
            String operator="aon_operator_"+UUID.randomUUID().toString().replace("-","");String password=PostgresTestcontainersSupport.password();
            try(var c=DriverManager.getConnection(PostgresTestcontainersSupport.currentUrl(POSTGRES,"postgres"),POSTGRES.getUsername(),POSTGRES.getPassword());var s=c.createStatement()) {
                s.execute("CREATE ROLE "+operator+" LOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE PASSWORD '"+password+"'");
                s.execute("GRANT elogistics_local_admin_operator TO "+operator);s.execute("GRANT CONNECT ON DATABASE "+db.name()+" TO "+operator);
            }
            UUID request=UUID.randomUUID();String realm="10000000-0000-4000-8000-000000000001";
            String command="SELECT * FROM identity.bind_local_admin_environment('"+request+"','"+realm+"','TEST','Synthetic isolated test binding')";
            rejects(db,"42501",command);
            try(var c=DriverManager.getConnection(db.url(),operator,password);var s=c.createStatement()) {
                try(var r=s.executeQuery(command)){assertTrue(r.next());assertFalse(r.getBoolean("replayed"));assertEquals("ACTIVE",r.getString("current_state"));}
                try(var r=s.executeQuery(command)){assertTrue(r.next());assertTrue(r.getBoolean("replayed"));}
                assertEquals("23514",assertThrows(SQLException.class,()->s.execute(command.replace("test binding","changed binding"))).getSQLState());
                try(var r=s.executeQuery("SELECT * FROM identity.disable_local_admin_environment('"+UUID.randomUUID()+"','"+realm+"','Synthetic terminal disable')")){assertTrue(r.next());assertEquals("DISABLED",r.getString("current_state"));}
                assertEquals("42501",assertThrows(SQLException.class,()->s.execute(command)).getSQLState());
            }
            try(var c=db.runtimeConnection();var s=c.createStatement();var r=s.executeQuery("SELECT identity.local_admin_environment_matches('TEST')")){assertTrue(r.next());assertFalse(r.getBoolean(1));}
        }
    }

    @Test void adminProvisionReservationRequiresSealAndCurrentOperatorReplay()throws Exception {
        try(var fixture=com.elogistic.backend.testsupport.IsolatedAdminTestDatabase.create(POSTGRES)) {
            var db=fixture.database();db.flyway("39").migrate();
            UUID realm=UUID.fromString("10000000-0000-4000-8000-000000000001");
            // SQL reservation tests only. This historical fixture is NOT onboarding/UAT evidence.
            db.activateRealmForTest(realm);
            String operator="aon_operator_"+UUID.randomUUID().toString().replace("-","");String password=PostgresTestcontainersSupport.password();
            try(var c=DriverManager.getConnection(PostgresTestcontainersSupport.currentUrl(POSTGRES,"postgres"),POSTGRES.getUsername(),POSTGRES.getPassword());var s=c.createStatement()) {
                s.execute("CREATE ROLE "+operator+" LOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE PASSWORD '"+password+"'");s.execute("GRANT elogistics_local_admin_operator TO "+operator);s.execute("GRANT CONNECT ON DATABASE "+db.name()+" TO "+operator);
            }
            UUID request=UUID.randomUUID();
            String command="SELECT * FROM identity.provision_local_admin('"+request+"','"+realm+"','Synthetic Admin','admin-test@example.invalid',ARRAY['ADMIN_DASHBOARD_READ','ADMIN_SHIPMENT_READ'],'Synthetic source test provision')";
            try(var c=DriverManager.getConnection(db.url(),operator,password);var s=c.createStatement()) {
                s.execute("SELECT * FROM identity.bind_local_admin_environment('"+UUID.randomUUID()+"','"+realm+"','TEST','Synthetic isolated test binding')");
                c.setAutoCommit(false);s.execute(command);
                assertThrows(SQLException.class,c::commit,"Unsealed reservation must not commit");c.rollback();c.setAutoCommit(true);
                c.setAutoCommit(false);UUID account;
                try(var r=s.executeQuery(command)){assertTrue(r.next());account=r.getObject("account_id",UUID.class);assertFalse(r.getBoolean("replayed"));}
                s.execute("SELECT identity.seal_local_admin_invitation('"+request+"','v1','e1',decode(repeat('cd',32),'hex'),decode(repeat('ac',12),'hex'),decode(repeat('ad',59),'hex'))");
                c.commit();c.setAutoCommit(true);
                try(var r=s.executeQuery(command)){assertTrue(r.next());assertTrue(r.getBoolean("replayed"));assertEquals(account,r.getObject("account_id",UUID.class));}
                assertEquals("23514",assertThrows(SQLException.class,()->s.execute(command.replace("Synthetic Admin","Different Admin"))).getSQLState());
                assertEquals("42501",assertThrows(SQLException.class,()->s.execute("SELECT identity.local_admin_invitation_destination('"+request+"')")).getSQLState());
                assertEquals("22023",assertThrows(SQLException.class,()->s.execute(command.replace(request.toString(),UUID.randomUUID().toString()).replace("ADMIN_SHIPMENT_READ","IDENTITY_MANAGEMENT"))).getSQLState());
                String cancel="SELECT * FROM identity.disposition_local_admin_invitation('"+UUID.randomUUID()+"','"+account+"',1,'CANCEL','Synthetic cancel source test')";
                try(var r=s.executeQuery(cancel)){assertTrue(r.next());assertEquals("REVOKED",r.getString("current_principal_state"));}
                try(var r=s.executeQuery(cancel)){assertTrue(r.next());assertTrue(r.getBoolean("replayed"));}
                try(var r=s.executeQuery(command)){assertTrue(r.next());assertTrue(r.getBoolean("replayed"));assertEquals("REVOKED",r.getString("current_principal_state"));}
                try(var admin=DriverManager.getConnection(PostgresTestcontainersSupport.currentUrl(POSTGRES,"postgres"),POSTGRES.getUsername(),POSTGRES.getPassword());var q=admin.createStatement()){q.execute("REVOKE elogistics_local_admin_operator FROM "+operator);}
                assertEquals("42501",assertThrows(SQLException.class,()->s.execute(command)).getSQLState());
            }
        }
    }

    @Test void adminProtectedInvitationUsesCanonicalActivationAndTerminalRevocation()throws Exception {
        try(var fixture=com.elogistic.backend.testsupport.IsolatedAdminTestDatabase.create(POSTGRES)) {
            var db=fixture.database();db.flyway("39").migrate();UUID realm=UUID.fromString("10000000-0000-4000-8000-000000000001");
            db.activateRealmForTest(realm); // Component fixture only; AON-T-10 must prove genuine Root separately.
            String operator="aon_operator_"+UUID.randomUUID().toString().replace("-","");String operatorPassword=PostgresTestcontainersSupport.password();
            try(var c=DriverManager.getConnection(PostgresTestcontainersSupport.currentUrl(POSTGRES,"postgres"),POSTGRES.getUsername(),POSTGRES.getPassword());var s=c.createStatement()){
                s.execute("CREATE ROLE "+operator+" LOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE PASSWORD '"+operatorPassword+"'");s.execute("GRANT elogistics_local_admin_operator TO "+operator);s.execute("GRANT CONNECT ON DATABASE "+db.name()+" TO "+operator);
            }
            try(var c=DriverManager.getConnection(db.url(),operator,operatorPassword);var s=c.createStatement()){s.execute("SELECT * FROM identity.bind_local_admin_environment('"+UUID.randomUUID()+"','"+realm+"','TEST','Synthetic isolated test binding')");}
            byte[] verifier=new byte[32],encryptor=new byte[32];new java.security.SecureRandom().nextBytes(verifier);new java.security.SecureRandom().nextBytes(encryptor);
            var protection=new InvitationProtection(Map.of("v1",verifier),"v1",Map.of("e1",encryptor),"e1");Arrays.fill(verifier,(byte)0);Arrays.fill(encryptor,(byte)0);
            var source=new org.springframework.jdbc.datasource.DriverManagerDataSource(db.url(),db.runtime(),db.runtimePassword());
            var provisioner=new LocalAdminProvisioner(new org.springframework.jdbc.datasource.DriverManagerDataSource(db.url(),operator,operatorPassword),protection);
            var input=new LocalAdminProvisioner.OfflineAdminProvision(UUID.randomUUID(),realm,"Synthetic verified Admin","aon-crypto@example.invalid",Set.of("ADMIN_DASHBOARD_READ","ADMIN_SHIPMENT_READ"),"Synthetic canonical cryptography");
            var created=provisioner.provision(input);assertFalse(created.replayed());assertEquals("PENDING",created.currentState());
            assertTrue(provisioner.provision(input).replayed());UUID proof;byte[] token;
            try(var c=db.runtimeConnection();var q=c.prepareStatement("SELECT * FROM identity.invitation_outbox WHERE account_id=?")){
                q.setObject(1,created.accountId());try(var r=q.executeQuery()){
                    assertTrue(r.next());proof=r.getObject("proof_id",UUID.class);
                    var context=new InvitationProtection.Context(r.getObject("id",UUID.class),proof,created.accountId(),realm,r.getString("destination"),r.getTimestamp("created_at").toInstant(),r.getTimestamp("expires_at").toInstant(),r.getString("verifier_key_id"),r.getString("encryption_key_id"));
                    token=protection.open(context,r.getBytes("nonce"),r.getBytes("ciphertext"),java.time.Instant.now());
                }
            }
            try{
                String password=PostgresTestcontainersSupport.password();UUID key=UUID.randomUUID();var activation=new LocalAdminInvitationActivation(source,protection,java.time.Clock.systemUTC(),"TEST");
                byte[] wrong=token.clone();wrong[0]=(byte)(wrong[0]=='a'?'b':'a');
                try{assertThrows(com.elogistic.backend.identity.api.IdentityCommandFailure.class,()->activation.accept(proof,wrong,password,key,UUID.randomUUID()));}finally{Arrays.fill(wrong,(byte)0);}
                activation.accept(proof,token,password,key,UUID.randomUUID());activation.accept(proof,token,password,key,UUID.randomUUID());
                assertEquals("ACTIVE",provisioner.status(created.accountId()).currentState());assertEquals(2,provisioner.status(created.accountId()).currentVersion());
                rejects(db,"42501","UPDATE identity.user_account SET updated_at=updated_at WHERE id='"+created.accountId()+"'");
                rejects(db,"42501","DELETE FROM identity.account_grant WHERE account_id='"+created.accountId()+"'");
                var changed=new LocalAdminProvisioner.OfflineAdminPermissions(UUID.randomUUID(),created.accountId(),2,Set.of("ADMIN_DASHBOARD_READ"),"Synthetic remove shipment read");
                assertEquals(3,provisioner.configure(changed).currentVersion());assertTrue(provisioner.configure(changed).replayed());
                var revoked=new LocalAdminProvisioner.OfflineAdminRevoke(UUID.randomUUID(),created.accountId(),3,"Synthetic terminal Admin revoke");
                assertEquals("REVOKED",provisioner.revoke(revoked).currentState());assertTrue(provisioner.revoke(revoked).replayed());
                assertEquals("REVOKED",provisioner.provision(input).currentState());assertTrue(provisioner.status(created.accountId()).currentPermissions().isEmpty());
                assertThrows(com.elogistic.backend.identity.api.IdentityCommandFailure.class,()->activation.accept(proof,token,password,key,UUID.randomUUID()));
                try(var c=db.runtimeConnection();var q=c.prepareStatement("SELECT (SELECT count(*) FROM identity.audit_entry WHERE target_id=? AND event_type='ADMIN_INVITATION_ACCEPTED'),(SELECT count(*) FROM identity.command_receipt WHERE proof_id=?)")){
                    q.setObject(1,created.accountId());q.setObject(2,proof);try(var r=q.executeQuery()){assertTrue(r.next());assertEquals(1,r.getLong(1));assertEquals(1,r.getLong(2));}
                }
            }finally{Arrays.fill(token,(byte)0);}
        }
    }

    @Test void isolatedFirstRootRequiresGenuineProofAndCannotReactivateAfterDisable()throws Exception {
        try(var fixture=com.elogistic.backend.testsupport.IsolatedAdminTestDatabase.create(POSTGRES)){
            var db=fixture.database();db.flyway("40").migrate();UUID realm=UUID.fromString("10000000-0000-4000-8000-000000000001");
            String admin="aon_admin_"+UUID.randomUUID().toString().replace("-",""),realmOperator="aon_realm_"+UUID.randomUUID().toString().replace("-","");
            String adminPassword=PostgresTestcontainersSupport.password(),operatorPassword=PostgresTestcontainersSupport.password();
            try(var c=DriverManager.getConnection(PostgresTestcontainersSupport.currentUrl(POSTGRES,"postgres"),POSTGRES.getUsername(),POSTGRES.getPassword());var s=c.createStatement()){
                for(var item:List.of(new String[]{admin,adminPassword,"elogistics_local_admin_operator"},new String[]{realmOperator,operatorPassword,"elogistics_local_realm_operator"})){
                    s.execute("CREATE ROLE "+item[0]+" LOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE PASSWORD '"+item[1]+"'");s.execute("GRANT "+item[2]+" TO "+item[0]);s.execute("GRANT CONNECT ON DATABASE "+db.name()+" TO "+item[0]);
                }
            }
            try(var c=DriverManager.getConnection(db.url(),admin,adminPassword);var s=c.createStatement()){
                s.execute("SELECT * FROM identity.bind_local_admin_environment('"+UUID.randomUUID()+"','"+realm+"','TEST','Synthetic genuine Root test binding')");
                assertEquals("42501",assertThrows(SQLException.class,()->s.execute("SELECT * FROM identity.provision_local_realm_bootstrap('"+UUID.randomUUID()+"','"+realm+"','Synthetic Root','root-test@example.invalid','Synthetic denied Admin operator')")).getSQLState());
            }
            byte[] v=new byte[32],e=new byte[32];new java.security.SecureRandom().nextBytes(v);new java.security.SecureRandom().nextBytes(e);
            var protection=new InvitationProtection(Map.of("v1",v),"v1",Map.of("e1",e),"e1");Arrays.fill(v,(byte)0);Arrays.fill(e,(byte)0);
            var provisioner=new LocalRealmBootstrapProvisioner(new org.springframework.jdbc.datasource.DriverManagerDataSource(db.url(),realmOperator,operatorPassword),protection);
            var input=new LocalRealmBootstrapProvisioner.OfflineRealmBootstrap(UUID.randomUUID(),realm,"Synthetic Initial Root","root-real-proof@example.invalid","Synthetic genuine Root provisioning");
            var pending=provisioner.provision(input);assertEquals("PENDING",pending.currentState());assertTrue(provisioner.provision(input).replayed());
            var activationCommand=new LocalRealmBootstrapProvisioner.OfflineRealmCommand(UUID.randomUUID(),realm,"Synthetic activate after proof");
            assertThrows(IllegalStateException.class,()->provisioner.activate(activationCommand));
            UUID proof;byte[] token;
            var runtimeSource=new org.springframework.jdbc.datasource.DriverManagerDataSource(db.url(),db.runtime(),db.runtimePassword());
            var clock=java.time.Clock.systemUTC();var deliveryPolicy=new JdbcInvitationOutbox.Policy(3,java.time.Duration.ofSeconds(30),java.time.Duration.ofSeconds(2));
            assertEquals(0,new JdbcInvitationOutbox(runtimeSource,protection,deliveryPolicy,clock).purge());
            assertTrue(JdbcInvitationOutbox.admin(runtimeSource,protection,deliveryPolicy,clock,"TEST",false).claim().isEmpty());
            try(var inbox=new CustomerSmtpTestInbox(1,false,false,250,null)){
                var transport=new SmtpCustomerDelivery(new SmtpCustomerDelivery.Settings("127.0.0.1",inbox.port(),SmtpCustomerDelivery.Mode.TEST_LOOPBACK,null,null,"admin-sender@example.invalid",Set.of("example.invalid"),"https://127.0.0.1:5173",true),clock);
                var worker=new AdminInvitationDeliveryWorker(JdbcInvitationOutbox.admin(runtimeSource,protection,deliveryPolicy,clock,"TEST",true),transport,()->true);
                assertTrue(worker.runOne());assertFalse(worker.runOne());var received=inbox.messages();assertEquals(1,received.size());
                var mail=new jakarta.mail.internet.MimeMessage(jakarta.mail.Session.getInstance(new Properties()),new java.io.ByteArrayInputStream(received.getFirst()));
                String body=mail.getContent().toString();assertTrue(body.contains("root_"));
                var match=java.util.regex.Pattern.compile("/admin/activate#proofId=([a-f0-9-]{36})&token=([A-Za-z0-9_-]{43})").matcher(body);assertTrue(match.find());
                proof=UUID.fromString(match.group(1));token=match.group(2).getBytes(java.nio.charset.StandardCharsets.US_ASCII);
            }
            try{
                var runtime=new org.springframework.jdbc.datasource.DriverManagerDataSource(db.url(),db.runtime(),db.runtimePassword());String password=PostgresTestcontainersSupport.password();UUID key=UUID.randomUUID();
                assertThrows(com.elogistic.backend.identity.api.IdentityCommandFailure.class,()->new LocalAdminInvitationActivation(runtime,protection,java.time.Clock.systemUTC(),"TEST").accept(proof,token,password,key,UUID.randomUUID()));
                var accept=new LocalAdminInvitationActivation(runtime,protection,java.time.Clock.systemUTC(),"TEST",true);
                var browser=new JdbcIdentityBrowserState(runtime);var csrf=browser.issue(null,null);byte[] rateKey=new byte[32];new java.security.SecureRandom().nextBytes(rateKey);
                var limiter=new JdbcIdentityRateLimiter(runtime,rateKey);Arrays.fill(rateKey,(byte)0);
                var endpoint=new com.elogistic.backend.identity.internal.web.ProductionAdminInvitationHttp(accept,browser,limiter,"https://127.0.0.1:5173",()->true);
                var request=new org.springframework.mock.web.MockHttpServletRequest("POST","/api/v1/auth/admin-invitations/accept");
                request.setSecure(true);request.setRemoteAddr("127.0.0.1");request.setContentType("application/json");request.addHeader("Origin","https://127.0.0.1:5173");request.addHeader("X-CSRF-Token",csrf.token());request.addHeader("Idempotency-Key",key.toString());request.setCookies(new jakarta.servlet.http.Cookie("__Host-elogistics.csrf",csrf.cookie()));
                request.setContent(tools.jackson.databind.json.JsonMapper.builder().build().writeValueAsBytes(Map.of("proofId",proof,"token",new String(token,java.nio.charset.StandardCharsets.US_ASCII),"password",password)));
                var response=new org.springframework.mock.web.MockHttpServletResponse();UUID correlation=UUID.randomUUID();response.setHeader("X-Request-Id",correlation.toString());
                endpoint.handle(request,response,correlation);assertEquals(204,response.getStatus());assertTrue(response.getContentAsString().isEmpty());assertTrue(response.getHeaders("Set-Cookie").isEmpty());
                accept.accept(proof,token,password,key,UUID.randomUUID());
                try(var c=db.runtimeConnection();var s=c.createStatement();var r=s.executeQuery("SELECT status,activated_at,bootstrap_account_id FROM identity.realm WHERE id='"+realm+"'")){assertTrue(r.next());assertEquals("INACTIVE",r.getString(1));assertNull(r.getTimestamp(2));assertEquals(pending.accountId(),r.getObject(3,UUID.class));}
                assertEquals("ACTIVE",provisioner.activate(activationCommand).currentState());assertTrue(provisioner.activate(activationCommand).replayed());
                assertEquals("INACTIVE",provisioner.disable(new LocalRealmBootstrapProvisioner.OfflineRealmCommand(UUID.randomUUID(),realm,"Synthetic terminal realm disable")).currentState());
                var replay=provisioner.activate(activationCommand);assertTrue(replay.replayed());assertEquals("INACTIVE",replay.currentState());
                assertThrows(IllegalStateException.class,()->provisioner.activate(new LocalRealmBootstrapProvisioner.OfflineRealmCommand(UUID.randomUUID(),realm,"Synthetic reactivation denied")));
                try(var c=db.runtimeConnection();var s=c.createStatement();var r=s.executeQuery("SELECT (SELECT count(*) FROM identity.account_grant WHERE account_id='"+pending.accountId()+"' AND role_code='IDENTITY_MANAGEMENT'),identity.local_admin_registered('"+pending.accountId()+"')")){assertTrue(r.next());assertEquals(1,r.getLong(1));assertFalse(r.getBoolean(2));}
            }finally{Arrays.fill(token,(byte)0);}
        }
    }

    @Test void adminConcurrentIssueAndAcceptanceHaveOneCommittedEffect()throws Exception{
        UUID realm=UUID.randomUUID();
        try(var fixture=new AdminOnboardingPostgresFixture(POSTGRES,realm)){
            var db=fixture.database();var input=new LocalAdminProvisioner.OfflineAdminProvision(UUID.randomUUID(),realm,"Synthetic concurrent Admin","concurrent-admin@example.invalid",Set.of("ADMIN_DASHBOARD_READ"),"Synthetic concurrent issue test");
            try(var pool=java.util.concurrent.Executors.newFixedThreadPool(2)){
                var start=new java.util.concurrent.CyclicBarrier(2);
                java.util.concurrent.Callable<LocalAdminProvisioner.OfflineAdminReceipt> issue=()->{start.await(5,java.util.concurrent.TimeUnit.SECONDS);return fixture.admins().provision(input);};
                var a=pool.submit(issue);var b=pool.submit(issue);var first=a.get(15,java.util.concurrent.TimeUnit.SECONDS);var second=b.get(15,java.util.concurrent.TimeUnit.SECONDS);
                assertEquals(first.accountId(),second.accountId());assertNotEquals(first.replayed(),second.replayed());
                try(var pending=fixture.pendingToken(first.accountId())){
                    var runtime=new org.springframework.jdbc.datasource.DriverManagerDataSource(db.url(),db.runtime(),db.runtimePassword());
                    var activation=new LocalAdminInvitationActivation(runtime,fixture.protection(),java.time.Clock.systemUTC(),"TEST");String password=PostgresTestcontainersSupport.password();UUID key=UUID.randomUUID();
                    var ready=new java.util.concurrent.CyclicBarrier(2);
                    java.util.concurrent.Callable<Boolean> accept=()->{ready.await(5,java.util.concurrent.TimeUnit.SECONDS);byte[] token=pending.token();try{activation.accept(pending.proof(),token,password,key,UUID.randomUUID());return true;}finally{Arrays.fill(token,(byte)0);}};
                    var x=pool.submit(accept);var y=pool.submit(accept);assertTrue(x.get(20,java.util.concurrent.TimeUnit.SECONDS));assertTrue(y.get(20,java.util.concurrent.TimeUnit.SECONDS));
                    try(var c=db.runtimeConnection();var q=c.prepareStatement("SELECT (SELECT count(*) FROM identity.audit_entry WHERE target_id=? AND event_type='ADMIN_INVITATION_ACCEPTED'),(SELECT count(*) FROM identity.command_receipt WHERE proof_id=?),version,auth_version FROM identity.user_account WHERE id=?")){
                        q.setObject(1,first.accountId());q.setObject(2,pending.proof());q.setObject(3,first.accountId());try(var r=q.executeQuery()){assertTrue(r.next());assertEquals(1,r.getLong(1));assertEquals(1,r.getLong(2));assertEquals(2,r.getLong(3));assertEquals(2,r.getLong(4));}
                    }
                }
            }
        }
    }

    @Test void adminResendQuotaExpiryAndCancelNeverRenewOldProofs()throws Exception{
        UUID realm=UUID.randomUUID();
        try(var fixture=new AdminOnboardingPostgresFixture(POSTGRES,realm)){
            var db=fixture.database();var source=new org.springframework.jdbc.datasource.DriverManagerDataSource(db.url(),db.runtime(),db.runtimePassword());
            var input=new LocalAdminProvisioner.OfflineAdminProvision(UUID.randomUUID(),realm,"Synthetic resend Admin","resend-admin@example.invalid",Set.of("ADMIN_DASHBOARD_READ"),"Synthetic resend quota test");
            UUID account=fixture.admins().provision(input).accountId();
            try(var old=fixture.pendingToken(account)){
                LocalAdminProvisioner.OfflineAdminInvitationDisposition first=null;
                for(int version=1;version<=3;version++){
                    var resend=new LocalAdminProvisioner.OfflineAdminInvitationDisposition(UUID.randomUUID(),account,version,"RESEND","Synthetic bounded resend request");if(first==null)first=resend;
                    assertFalse(fixture.admins().disposition(resend).replayed());assertTrue(fixture.admins().disposition(resend).replayed());
                }
                assertThrows(IllegalStateException.class,()->fixture.admins().disposition(new LocalAdminProvisioner.OfflineAdminInvitationDisposition(UUID.randomUUID(),account,4,"RESEND","Synthetic fourth resend denied")));
                byte[] token=old.token();try{assertThrows(com.elogistic.backend.identity.api.IdentityCommandFailure.class,()->new LocalAdminInvitationActivation(source,fixture.protection(),java.time.Clock.systemUTC(),"TEST").accept(old.proof(),token,PostgresTestcontainersSupport.password(),UUID.randomUUID(),UUID.randomUUID()));}finally{Arrays.fill(token,(byte)0);}
                // Twenty committed resends/operator/hour; replay does not consume quota.
                for(int n=0;n<17;n++){
                    UUID other=fixture.admins().provision(new LocalAdminProvisioner.OfflineAdminProvision(UUID.randomUUID(),realm,"Synthetic quota Admin","quota-"+n+"@example.invalid",Set.of("ADMIN_DASHBOARD_READ"),"Synthetic operator quota target")).accountId();
                    fixture.admins().disposition(new LocalAdminProvisioner.OfflineAdminInvitationDisposition(UUID.randomUUID(),other,1,"RESEND","Synthetic operator bounded resend"));
                }
                UUID last=fixture.admins().provision(new LocalAdminProvisioner.OfflineAdminProvision(UUID.randomUUID(),realm,"Synthetic over quota","quota-last@example.invalid",Set.of("ADMIN_DASHBOARD_READ"),"Synthetic operator ceiling test")).accountId();
                assertThrows(IllegalStateException.class,()->fixture.admins().disposition(new LocalAdminProvisioner.OfflineAdminInvitationDisposition(UUID.randomUUID(),last,1,"RESEND","Synthetic twenty first denied")));
                assertTrue(fixture.admins().disposition(first).replayed());assertEquals(4,fixture.admins().status(account).currentVersion());
                try(var expired=fixture.pendingToken(account)){
                    byte[] supplied=expired.token();try{assertThrows(com.elogistic.backend.identity.api.IdentityCommandFailure.class,()->new LocalAdminInvitationActivation(source,fixture.protection(),java.time.Clock.offset(java.time.Clock.systemUTC(),java.time.Duration.ofHours(25)),"TEST").accept(expired.proof(),supplied,PostgresTestcontainersSupport.password(),UUID.randomUUID(),UUID.randomUUID()));}finally{Arrays.fill(supplied,(byte)0);}
                }
                var worker=JdbcInvitationOutbox.admin(source,fixture.protection(),new JdbcInvitationOutbox.Policy(3,java.time.Duration.ofSeconds(30),java.time.Duration.ofSeconds(2)),java.time.Clock.offset(java.time.Clock.systemUTC(),java.time.Duration.ofHours(25)),"TEST",false);
                assertTrue(worker.claim().isEmpty());
                assertEquals("REVOKED",fixture.admins().disposition(new LocalAdminProvisioner.OfflineAdminInvitationDisposition(UUID.randomUUID(),account,4,"CANCEL","Synthetic cancel after expiration")).currentState());
                assertEquals("REVOKED",fixture.admins().disposition(first).currentState());
                try(var c=db.runtimeConnection();var s=c.createStatement();var r=s.executeQuery("SELECT count(*) FROM identity.invitation_outbox WHERE account_id='"+account+"' AND(nonce IS NOT NULL OR ciphertext IS NOT NULL)")){assertTrue(r.next());assertEquals(0,r.getLong(1));}
            }
        }
    }

    @Test void rootResendAndCancelPreserveInitialBindingAndQuarantineRealm()throws Exception{
        UUID realm=UUID.randomUUID();
        try(var fixture=new AdminOnboardingPostgresFixture(POSTGRES,realm,false)){
            var first=new LocalRealmBootstrapProvisioner.OfflineRealmInvitationDisposition(UUID.randomUUID(),realm,"RESEND","Synthetic Root resend request");
            assertFalse(fixture.roots().disposition(first).replayed());assertTrue(fixture.roots().disposition(first).replayed());
            fixture.roots().disposition(new LocalRealmBootstrapProvisioner.OfflineRealmInvitationDisposition(UUID.randomUUID(),realm,"RESEND","Synthetic second Root resend"));
            fixture.roots().disposition(new LocalRealmBootstrapProvisioner.OfflineRealmInvitationDisposition(UUID.randomUUID(),realm,"RESEND","Synthetic third Root resend"));
            assertThrows(IllegalStateException.class,()->fixture.roots().disposition(new LocalRealmBootstrapProvisioner.OfflineRealmInvitationDisposition(UUID.randomUUID(),realm,"RESEND","Synthetic fourth Root denied")));
            var cancel=new LocalRealmBootstrapProvisioner.OfflineRealmInvitationDisposition(UUID.randomUUID(),realm,"CANCEL","Synthetic quarantine fresh realm");
            assertEquals("REVOKED",fixture.roots().disposition(cancel).currentState());assertTrue(fixture.roots().disposition(cancel).replayed());assertEquals("REVOKED",fixture.roots().disposition(first).currentState());
            assertThrows(IllegalStateException.class,()->fixture.roots().provision(new LocalRealmBootstrapProvisioner.OfflineRealmBootstrap(UUID.randomUUID(),realm,"Replacement denied","replacement@example.invalid","Synthetic replacement Root denied")));
            assertThrows(IllegalStateException.class,()->fixture.roots().activate(new LocalRealmBootstrapProvisioner.OfflineRealmCommand(UUID.randomUUID(),realm,"Synthetic cancelled activation denied")));
            try(var c=fixture.database().runtimeConnection();var s=c.createStatement();var r=s.executeQuery("SELECT status,bootstrap_account_id,activated_at FROM identity.realm WHERE id='"+realm+"'")){assertTrue(r.next());assertEquals("INACTIVE",r.getString(1));assertEquals(fixture.rootAccount(),r.getObject(2,UUID.class));assertNull(r.getTimestamp(3));}
        }
    }

    @Test void adminV40DdlFailurePreservesV39AndRetriesWithoutRepair()throws Exception{
        Database db=PostgresTestcontainersSupport.database(POSTGRES);db.flyway("39").migrate();
        var before=Arrays.stream(db.flyway("39").info().applied()).map(m->m.getVersion()+":"+m.getChecksum()).toList();String definition;
        try(var c=db.runtimeConnection();var s=c.createStatement();var r=s.executeQuery("SELECT pg_get_functiondef('identity.guard_local_admin_operator_receipt()'::regprocedure)")){assertTrue(r.next());definition=r.getString(1);}
        try(var c=DriverManager.getConnection(db.url(),POSTGRES.getUsername(),POSTGRES.getPassword());var s=c.createStatement()){
            s.execute("CREATE FUNCTION identity.synthetic_aon_ddl_failure() RETURNS event_trigger LANGUAGE plpgsql AS $$ BEGIN IF TG_TAG='CREATE FUNCTION' AND current_query() LIKE '%identity.accept_local_realm_bootstrap_invitation%' THEN RAISE EXCEPTION 'SYNTHETIC_AON_LATE_DDL_FAILURE';END IF;END $$;CREATE EVENT TRIGGER synthetic_aon_ddl_failure ON ddl_command_start EXECUTE FUNCTION identity.synthetic_aon_ddl_failure()");
            try{assertThrows(FlywayException.class,()->db.flyway("40").migrate());}finally{s.execute("DROP EVENT TRIGGER synthetic_aon_ddl_failure;DROP FUNCTION identity.synthetic_aon_ddl_failure()");}
        }
        assertEquals(before,Arrays.stream(db.flyway("39").info().applied()).map(m->m.getVersion()+":"+m.getChecksum()).toList());
        try(var c=db.runtimeConnection();var s=c.createStatement();var r=s.executeQuery("SELECT pg_get_functiondef('identity.guard_local_admin_operator_receipt()'::regprocedure),to_regprocedure('identity.accept_local_realm_bootstrap_invitation(uuid,uuid,bytea,text,uuid)') IS NULL")){assertTrue(r.next());assertEquals(definition,r.getString(1));assertTrue(r.getBoolean(2));}
        assertEquals(1,db.flyway("40").migrate().migrationsExecuted);db.flyway("40").validate();assertEquals(0,db.flyway("40").migrate().migrationsExecuted);
    }
    @Test void adminCollisionsAndRecreatedOperatorCannotTakeOverExistingIdentity()throws Exception{
        UUID realm=UUID.randomUUID();
        for(String invalid:List.of("\uD800", "name\u0000suffix", "x".repeat(161))){
            assertThrows(IllegalStateException.class,()->new LocalAdminProvisioner.OfflineAdminProvision(UUID.randomUUID(),realm,invalid,"synthetic@example.invalid",Set.of("ADMIN_DASHBOARD_READ"),"Synthetic validation reason"));
            assertThrows(IllegalStateException.class,()->new LocalRealmBootstrapProvisioner.OfflineRealmBootstrap(UUID.randomUUID(),realm,invalid,"synthetic@example.invalid","Synthetic validation reason"));
        }
        for(String invalid:List.of("\uDC00".repeat(10),"reason\u0000suffix","short","x".repeat(501))){
            assertThrows(IllegalStateException.class,()->new LocalAdminProvisioner.OfflineAdminInvitationDisposition(UUID.randomUUID(),realm,1,"RESEND",invalid));
            assertThrows(IllegalStateException.class,()->new LocalAdminProvisioner.OfflineAdminPermissions(UUID.randomUUID(),realm,1,Set.of("ADMIN_DASHBOARD_READ"),invalid));
            assertThrows(IllegalStateException.class,()->new LocalAdminProvisioner.OfflineAdminRevoke(UUID.randomUUID(),realm,1,invalid));
            assertThrows(IllegalStateException.class,()->new LocalRealmBootstrapProvisioner.OfflineRealmCommand(UUID.randomUUID(),realm,invalid));
            assertThrows(IllegalStateException.class,()->new LocalRealmBootstrapProvisioner.OfflineRealmInvitationDisposition(UUID.randomUUID(),realm,"RESEND",invalid));
        }
        String exactName="\uD83D\uDE80".repeat(160),exactReason="\u1EBF".repeat(500);
        var exact=new LocalAdminProvisioner.OfflineAdminProvision(UUID.randomUUID(),realm,exactName,"synthetic@example.invalid",Set.of("ADMIN_DASHBOARD_READ"),exactReason);
        assertEquals(exactName,exact.displayName());assertEquals(exactReason,exact.reason());
        assertEquals(exactName,new LocalRealmBootstrapProvisioner.OfflineRealmBootstrap(UUID.randomUUID(),realm,exactName,"synthetic@example.invalid",exactReason).displayName());
        try(var fixture=new AdminOnboardingPostgresFixture(POSTGRES,realm)){
            var db=fixture.database();String occupied="occupied-customer@example.invalid",enrollment="pending-enrollment@example.invalid";UUID existing=UUID.randomUUID();
            try(var c=db.runtimeConnection();var s=c.createStatement()){
                s.execute("INSERT INTO identity.user_account(id,username,display_name,email,status,created_at,updated_at) VALUES('"+existing+"','synthetic-occupied','Synthetic pending Customer','"+occupied+"','PENDING_ACTIVATION',CURRENT_TIMESTAMP,CURRENT_TIMESTAMP)");
                s.execute("INSERT INTO identity.customer_enrollment(id,intended_account_id,exact_email,display_name,protected_password,consent_version,created_at,expires_at,status,version) VALUES(gen_random_uuid(),gen_random_uuid(),'"+enrollment+"','Synthetic pending enrollment',convert_to("+HASH+",'UTF8'),'SYNTHETIC-TEST',CURRENT_TIMESTAMP,CURRENT_TIMESTAMP+INTERVAL '2 minutes','PENDING',1)");
            }
            for(String email:List.of(occupied,enrollment))assertThrows(IllegalStateException.class,()->fixture.admins().provision(new LocalAdminProvisioner.OfflineAdminProvision(UUID.randomUUID(),realm,"No conversion allowed",email,Set.of("ADMIN_DASHBOARD_READ"),"Synthetic collision must reject")));
            try(var c=db.runtimeConnection();var s=c.createStatement();var r=s.executeQuery("SELECT status,email_verified,password_hash IS NULL,identity.local_admin_registered(id) FROM identity.user_account WHERE id='"+existing+"'")){assertTrue(r.next());assertEquals("PENDING_ACTIVATION",r.getString(1));assertFalse(r.getBoolean(2));assertTrue(r.getBoolean(3));assertFalse(r.getBoolean(4));}
            var input=new LocalAdminProvisioner.OfflineAdminProvision(UUID.randomUUID(),realm,"Synthetic receipt owner","operator-replay@example.invalid",Set.of("ADMIN_DASHBOARD_READ"),"Synthetic exact operator binding");var result=fixture.admins().provision(input);
            String operator;try(var c=fixture.operatorConnection();var s=c.createStatement();var r=s.executeQuery("SELECT session_user")){assertTrue(r.next());operator=r.getString(1);}
            String password=PostgresTestcontainersSupport.password();
            try(var c=DriverManager.getConnection(PostgresTestcontainersSupport.currentUrl(POSTGRES,"postgres"),POSTGRES.getUsername(),POSTGRES.getPassword());var s=c.createStatement()){
                assertTrue(operator.matches("aon_fixture_[a-f0-9]{32}"));
                s.execute("REVOKE CONNECT ON DATABASE "+db.name()+" FROM "+operator);s.execute("REVOKE elogistics_local_admin_operator FROM "+operator);s.execute("DROP ROLE "+operator);
                s.execute("CREATE ROLE "+operator+" LOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE PASSWORD '"+password+"'");s.execute("GRANT elogistics_local_admin_operator TO "+operator);s.execute("GRANT CONNECT ON DATABASE "+db.name()+" TO "+operator);
            }
            var recreated=new LocalAdminProvisioner(new org.springframework.jdbc.datasource.DriverManagerDataSource(db.url(),operator,password),fixture.protection());
            assertThrows(IllegalStateException.class,()->recreated.provision(input));
            try(var c=db.runtimeConnection();var s=c.createStatement();var r=s.executeQuery("SELECT count(*) FROM identity.audit_entry WHERE target_id='"+result.accountId()+"' AND source='LOCAL_ADMIN_OPERATOR'")){assertTrue(r.next());assertEquals(1,r.getLong(1));}
        }
    }

    @Test void adminWorkerLeaseRecoveryAndCancellationRespectCurrentEligibility()throws Exception{
        UUID realm=UUID.randomUUID();
        try(var fixture=new AdminOnboardingPostgresFixture(POSTGRES,realm)){
            var db=fixture.database();var source=new org.springframework.jdbc.datasource.DriverManagerDataSource(db.url(),db.runtime(),db.runtimePassword());
            UUID account=fixture.admins().provision(new LocalAdminProvisioner.OfflineAdminProvision(UUID.randomUUID(),realm,"Synthetic recovery Admin","worker-recovery@example.invalid",Set.of("ADMIN_DASHBOARD_READ"),"Synthetic protected lease recovery")).accountId();
            var policy=new JdbcInvitationOutbox.Policy(3,java.time.Duration.ofSeconds(5),java.time.Duration.ofSeconds(1));var base=java.time.Clock.systemUTC();
            var first=JdbcInvitationOutbox.admin(source,fixture.protection(),policy,base,"TEST",false);var abandoned=first.claim().orElseThrow();
            var recovered=JdbcInvitationOutbox.admin(source,fixture.protection(),policy,java.time.Clock.offset(base,java.time.Duration.ofSeconds(6)),"TEST",false);var lease=recovered.claim().orElseThrow();
            assertEquals(abandoned.messageId(),lease.messageId());assertNotEquals(abandoned.leaseId(),lease.leaseId());assertThrows(IllegalStateException.class,()->first.open(abandoned));
            try(var delivery=recovered.open(lease)){assertTrue(delivery.loginUsername().startsWith("admin_"));}
            recovered.complete(lease,JdbcInvitationOutbox.Outcome.TRANSIENT_FAILURE);
            var clock=java.time.Clock.offset(base,java.time.Duration.ofSeconds(8));
            try(var inbox=new CustomerSmtpTestInbox(1,false,false,250,null)){
                var smtp=new SmtpCustomerDelivery(new SmtpCustomerDelivery.Settings("127.0.0.1",inbox.port(),SmtpCustomerDelivery.Mode.TEST_LOOPBACK,null,null,"admin-sender@example.invalid",Set.of("example.invalid"),"https://127.0.0.1:5173",true),clock);
                var worker=new AdminInvitationDeliveryWorker(JdbcInvitationOutbox.admin(source,fixture.protection(),policy,clock,"TEST",false),smtp,()->true);assertTrue(worker.runOne());assertFalse(worker.runOne());
                var mail=new jakarta.mail.internet.MimeMessage(jakarta.mail.Session.getInstance(new Properties()),new java.io.ByteArrayInputStream(inbox.messages().getFirst()));assertEquals("<"+lease.messageId()+"@example.invalid>",mail.getMessageID());
            }
            UUID cancelled=fixture.admins().provision(new LocalAdminProvisioner.OfflineAdminProvision(UUID.randomUUID(),realm,"Synthetic cancelled Admin","worker-cancel@example.invalid",Set.of("ADMIN_DASHBOARD_READ"),"Synthetic cancellation after claim")).accountId();
            var current=JdbcInvitationOutbox.admin(source,fixture.protection(),policy,java.time.Clock.systemUTC(),"TEST",false);var claim=current.claim().orElseThrow();
            fixture.admins().disposition(new LocalAdminProvisioner.OfflineAdminInvitationDisposition(UUID.randomUUID(),cancelled,1,"CANCEL","Synthetic cancel before worker open"));assertThrows(IllegalStateException.class,()->current.open(claim));
            try(var c=db.runtimeConnection();var s=c.createStatement();var r=s.executeQuery("SELECT count(*) FROM identity.invitation_outbox WHERE account_id IN('"+account+"','"+cancelled+"') AND(nonce IS NOT NULL OR ciphertext IS NOT NULL)")){assertTrue(r.next());assertEquals(0,r.getLong(1));}
        }
    }

    @Test void staffMigrationFailureRollsBackDefinitionAndHistoryWithoutRepair()throws Exception{
        Database db=PostgresTestcontainersSupport.database(POSTGRES);db.flyway("36").migrate();var history=Arrays.stream(db.flyway("36").info().applied()).map(m->m.getVersion()+":"+m.getChecksum()).toList();String definition;
        try(var c=db.runtimeConnection();var q=c.createStatement();var r=q.executeQuery("SELECT pg_get_functiondef('orders.configure_local_support_staff(uuid,text,text)'::regprocedure)")){assertTrue(r.next());definition=r.getString(1);}
        try(var admin=DriverManager.getConnection(PostgresTestcontainersSupport.currentUrl(POSTGRES,db.name()),POSTGRES.getUsername(),POSTGRES.getPassword());var q=admin.createStatement()){
            q.execute("CREATE FUNCTION identity.synthetic_cskh_ddl_failure() RETURNS event_trigger LANGUAGE plpgsql AS $$ BEGIN IF TG_TAG='CREATE FUNCTION' AND current_query() LIKE '%orders.configure_local_organization_support_staff%' THEN RAISE EXCEPTION 'SYNTHETIC_CSKH_LATE_DDL_FAILURE';END IF;END $$;CREATE EVENT TRIGGER synthetic_cskh_ddl_failure ON ddl_command_start EXECUTE FUNCTION identity.synthetic_cskh_ddl_failure()");
            try{assertThrows(FlywayException.class,()->db.flyway("37").migrate());}finally{q.execute("DROP EVENT TRIGGER synthetic_cskh_ddl_failure;DROP FUNCTION identity.synthetic_cskh_ddl_failure()");}
        }
        assertEquals(history,Arrays.stream(db.flyway("36").info().applied()).map(m->m.getVersion()+":"+m.getChecksum()).toList());
        try(var c=db.runtimeConnection();var q=c.createStatement()){
            try(var r=q.executeQuery("SELECT to_regclass('orders.support_grant_operator_receipt') IS NULL")){assertTrue(r.next());assertTrue(r.getBoolean(1));}
            try(var r=q.executeQuery("SELECT pg_get_functiondef('orders.configure_local_support_staff(uuid,text,text)'::regprocedure)")){assertTrue(r.next());assertEquals(definition,r.getString(1));}
        }
        assertEquals(1,db.flyway("37").migrate().migrationsExecuted);db.flyway("37").validate();assertEquals(0,db.flyway("37").migrate().migrationsExecuted);assertEquals(history,Arrays.stream(db.flyway("37").info().applied()).limit(35).map(m->m.getVersion()+":"+m.getChecksum()).toList());
    }

    @Test void claimIntakeV38StartsEmptyPreservesPriorChecksumsAndRejectsFinancialStates()throws Exception{
        Database db=PostgresTestcontainersSupport.database(POSTGRES);assertEquals(36,db.flyway("37").migrate().migrationsExecuted);
        var before=Arrays.stream(db.flyway("37").info().applied()).map(m->m.getVersion()+":"+m.getChecksum()).toList();
        assertEquals(1,db.flyway("38").migrate().migrationsExecuted);db.flyway("38").validate();
        assertEquals(before,Arrays.stream(db.flyway("38").info().applied()).limit(36).map(m->m.getVersion()+":"+m.getChecksum()).toList());
        assertEquals(0,db.flyway("38").migrate().migrationsExecuted);
        for(String table:List.of("orders.organization_claim_intake","orders.organization_claim_intake_event","orders.organization_claim_intake_receipt","orders.organization_claim_evidence"))assertEquals(0,managementSnapshot(db,table).size());
        try(var c=db.runtimeConnection();var s=c.createStatement();var r=s.executeQuery("SELECT pg_get_constraintdef(oid) FROM pg_constraint WHERE conrelid='orders.organization_claim_intake'::regclass AND contype='c'")){
            var definitions=new ArrayList<String>();while(r.next())definitions.add(r.getString(1));String checks=String.join(" ",definitions);
            assertTrue(checks.contains("CLOSED_WITHOUT_DECISION"));assertFalse(checks.contains("APPROVED"));assertFalse(checks.contains("PAID"));
        }
        rejects(db,"42501","DELETE FROM orders.organization_claim_intake");rejects(db,"42501","UPDATE orders.organization_claim_intake_event SET operation='APPROVE'");
        rejects(db,"42501","ALTER TABLE orders.organization_claim_intake ADD COLUMN paid BOOLEAN");
    }

    @Test void staffRoleBootstrapPreservesMigrationAndRuntimeLeastPrivilege() throws Exception {
        Database db = PostgresTestcontainersSupport.database(POSTGRES);
        assertEquals(34, db.flyway("35").migrate().migrationsExecuted);
        try (Connection c = db.migrationConnection(); Statement s = c.createStatement()) {
            for (String role : List.of("elogistics_local_staff_operator", "identity_staff_writer", "support_grant_writer")) {
                try (ResultSet r = s.executeQuery("SELECT rolcanlogin,rolsuper,rolcreatedb,rolcreaterole,rolreplication,rolbypassrls FROM pg_roles WHERE rolname='" + role + "'")) {
                    assertTrue(r.next(), "Required bounded Staff role must exist");
                    for (int i = 1; i <= 6; i++) assertFalse(r.getBoolean(i));
                    assertFalse(r.next());
                }
            }
            for (String role : List.of(db.owner(), db.migrator(), db.runtime())) {
                try (ResultSet r = s.executeQuery("SELECT rolsuper,rolcreaterole FROM pg_roles WHERE rolname='" + role + "'")) {
                    assertTrue(r.next()); assertFalse(r.getBoolean(1)); assertFalse(r.getBoolean(2));
                }
            }
            for (String writer : List.of("identity_staff_writer", "support_grant_writer")) {
                try (ResultSet r = s.executeQuery("SELECT pg_has_role('" + db.runtime() + "','" + writer + "','SET'),pg_has_role('elogistics_local_staff_operator','" + writer + "','SET'),pg_has_role('" + db.owner() + "','" + writer + "','SET'),pg_has_role('" + db.owner() + "','" + writer + "','USAGE')")) {
                    assertTrue(r.next()); assertFalse(r.getBoolean(1)); assertFalse(r.getBoolean(2));
                    assertTrue(r.getBoolean(3)); assertFalse(r.getBoolean(4));
                }
            }
            assertEquals("42501", assertThrows(SQLException.class, () -> s.execute("CREATE ROLE forbidden_staff_escalation NOLOGIN")).getSQLState());
        }
        rejects(db, "42501", "SET ROLE identity_staff_writer");
        rejects(db, "42501", "SET ROLE support_grant_writer");
    }

    private Database migrated() throws Exception {
        Database db = PostgresTestcontainersSupport.database(POSTGRES);
        assertEquals(48,db.flyway("latest").migrate().migrationsExecuted);
        return db;
    }

    @Test void staffV36StartsDisabledAndRejectsRawAuthorityWrites() throws Exception {
        Database db = PostgresTestcontainersSupport.database(POSTGRES);
        assertEquals(34, db.flyway("35").migrate().migrationsExecuted);
        var history = Arrays.stream(db.flyway("35").info().applied()).map(m -> m.getVersion() + ":" + m.getChecksum()).toList();
        assertEquals(1, db.flyway("36").migrate().migrationsExecuted);
        db.flyway("36").validate();
        assertEquals(history, Arrays.stream(db.flyway("36").info().applied()).limit(34).map(m -> m.getVersion() + ":" + m.getChecksum()).toList());
        assertEquals(0, db.flyway("36").migrate().migrationsExecuted);
        try (var c = db.runtimeConnection(); var s = c.createStatement(); var rows = s.executeQuery("SELECT identity.local_staff_environment_matches(),identity.support_staff_registered(gen_random_uuid()),identity.support_staff_eligible(gen_random_uuid())")) {
            assertTrue(rows.next()); for (int i = 1; i <= 3; i++) assertFalse(rows.getBoolean(i));
        }
        rejects(db, "42501", "INSERT INTO identity.local_staff_environment(singleton,environment) VALUES(TRUE,'DISABLED')");
        rejects(db, "42501", "UPDATE identity.local_staff_environment SET enabled=TRUE");
        rejects(db, "42501", "SELECT identity.bind_local_staff_environment(gen_random_uuid(),'TEST','Synthetic denial')");
        account(db);
        rejects(db, "42501", "INSERT INTO identity.local_staff_principal VALUES(" + ACCOUNT + ",'identity@example.invalid','SUPPORT','TEST','PENDING',1,gen_random_uuid(),clock_timestamp(),clock_timestamp())");
        assertEquals(0, managementSnapshot(db, "identity.local_staff_principal").size());
        for (String writer : List.of("identity_staff_writer", "support_grant_writer")) {
            try (var c = db.migrationConnection(); var s = c.createStatement(); var rows = s.executeQuery("SELECT has_schema_privilege('" + writer + "','identity','CREATE')")) {
                assertTrue(rows.next()); assertFalse(rows.getBoolean(1));
            }
        }
    }
    @Test void staffV36ProvisionRequiresSealAndReplaysWithoutNewAuthority() throws Exception {
        Database db = PostgresTestcontainersSupport.database(POSTGRES);
        db.flyway("36").migrate();
        String operator = "staff_operator_" + UUID.randomUUID().toString().replace("-", "");
        String password = PostgresTestcontainersSupport.password();
        try (var c = DriverManager.getConnection(PostgresTestcontainersSupport.currentUrl(POSTGRES,"postgres"), POSTGRES.getUsername(), POSTGRES.getPassword()); var s = c.createStatement()) {
            s.execute("CREATE ROLE " + operator + " LOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE PASSWORD '" + password + "'");
            s.execute("GRANT elogistics_local_staff_operator TO " + operator);
            s.execute("GRANT CONNECT ON DATABASE " + db.name() + " TO " + operator);
        }
        db.ownerExecute("SELECT identity.bind_local_staff_environment('" + UUID.randomUUID() + "','TEST','Synthetic isolated bootstrap')");
        UUID key = UUID.randomUUID();
        String provision = "SELECT * FROM identity.provision_local_support_staff('" + key + "','Synthetic Staff','staff@example.invalid','Synthetic isolated request')";
        try (var c = DriverManager.getConnection(db.url(), operator, password); var s = c.createStatement()) {
            c.setAutoCommit(false);
            try (var result = s.executeQuery(provision)) { assertTrue(result.next()); assertFalse(result.getBoolean("replayed")); }
            assertEquals("23514", assertThrows(SQLException.class, c::commit).getSQLState()); c.rollback();
        }
        assertEquals(0, managementSnapshot(db,"identity.local_staff_principal").size());
        assertEquals(0, staffPrivateReceiptCount(db));
        rejects(db,"42501","SELECT * FROM identity.local_staff_operator_receipt");
        UUID account;
        try (var c = DriverManager.getConnection(db.url(), operator, password); var s = c.createStatement()) {
            c.setAutoCommit(false);
            try (var result = s.executeQuery(provision)) {
                assertTrue(result.next()); account=result.getObject("account_id",UUID.class);
                assertEquals("PENDING",result.getString("current_principal_state")); assertFalse(result.getBoolean("replayed"));
            }
            s.execute("SELECT identity.seal_local_support_staff_invitation('" + key + "','v1','e1',decode(repeat('a1',32),'hex'),decode(repeat('a2',12),'hex'),decode(repeat('a3',59),'hex'))");
            c.commit();
            try (var result = s.executeQuery(provision)) { assertTrue(result.next()); assertTrue(result.getBoolean("replayed")); assertEquals(account,result.getObject("account_id",UUID.class)); }
            c.commit();
            assertEquals("23514",assertThrows(SQLException.class,()->s.execute(provision.replace("Synthetic Staff","Changed Staff"))).getSQLState()); c.rollback();
            assertEquals("42501",assertThrows(SQLException.class,()->s.execute("SET ROLE identity_staff_writer")).getSQLState()); c.rollback();
        }
        assertEquals(1, managementSnapshot(db,"identity.local_staff_principal").size());
        assertEquals(1, staffPrivateReceiptCount(db));
        try (var c=db.runtimeConnection();var s=c.createStatement();var r=s.executeQuery("SELECT status,password_hash,email_verified FROM identity.user_account WHERE id='"+account+"'")) {
            assertTrue(r.next());assertEquals("PENDING_ACTIVATION",r.getString(1));assertNull(r.getString(2));assertFalse(r.getBoolean(3));
        }
        rejects(db,"42501","UPDATE identity.user_account SET status='ACTIVE',password_hash="+HASH+",email_verified=TRUE,activated_at=clock_timestamp(),updated_at=clock_timestamp(),version=version+1 WHERE id='"+account+"'");
        assertEquals(0,managementSnapshot(db,"orders.support_staff_grant").size());
        try(var c=DriverManager.getConnection(db.url(),operator,password);var s=c.createStatement()) {
            s.execute("SELECT * FROM identity.disposition_local_support_staff_invitation('"+UUID.randomUUID()+"','"+account+"','CANCEL','Synthetic cancellation')");
        }
        try(var c=db.runtimeConnection();var s=c.createStatement();var r=s.executeQuery("SELECT a.status,p.state,p.version FROM identity.user_account a JOIN identity.local_staff_principal p ON p.account_id=a.id WHERE a.id='"+account+"'")) {
            assertTrue(r.next());assertEquals("DISABLED",r.getString(1));assertEquals("CANCELLED",r.getString(2));assertEquals(2,r.getLong(3));
        }
    }

    @Test void staffV37CanonicalReceiptsAreRevokeSafeAndNotOrganizationAuthority() throws Exception {
        Database db=PostgresTestcontainersSupport.database(POSTGRES);
        assertEquals(35,db.flyway("36").migrate().migrationsExecuted);
        var history=Arrays.stream(db.flyway("36").info().applied()).map(m->m.getVersion()+":"+m.getChecksum()).toList();
        assertEquals(1,db.flyway("37").migrate().migrationsExecuted);db.flyway("37").validate();
        assertEquals(history,Arrays.stream(db.flyway("37").info().applied()).limit(35).map(m->m.getVersion()+":"+m.getChecksum()).toList());
        String operator="staff_operator_"+UUID.randomUUID().toString().replace("-","");
        String password=PostgresTestcontainersSupport.password();
        try(var c=DriverManager.getConnection(PostgresTestcontainersSupport.currentUrl(POSTGRES,"postgres"),POSTGRES.getUsername(),POSTGRES.getPassword());var s=c.createStatement()){
            s.execute("CREATE ROLE "+operator+" LOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE PASSWORD '"+password+"'");
            s.execute("GRANT elogistics_local_staff_operator TO "+operator);s.execute("GRANT CONNECT ON DATABASE "+db.name()+" TO "+operator);
        }
        db.ownerExecute("SELECT identity.bind_local_staff_environment('"+UUID.randomUUID()+"','TEST','Synthetic isolated bootstrap')");
        UUID issue=UUID.randomUUID(),account,proof;
        try(var c=DriverManager.getConnection(db.url(),operator,password);var s=c.createStatement()){
            c.setAutoCommit(false);
            try(var r=s.executeQuery("SELECT * FROM identity.provision_local_support_staff('"+issue+"','Synthetic Staff','canonical@example.invalid','Synthetic isolated request')")){
                assertTrue(r.next());account=r.getObject("account_id",UUID.class);proof=r.getObject("proof_id",UUID.class);
            }
            s.execute("SELECT identity.seal_local_support_staff_invitation('"+issue+"','v1','e1',decode(repeat('b1',32),'hex'),decode(repeat('b2',12),'hex'),decode(repeat('b3',59),'hex'))");c.commit();
        }
        // SQL-boundary synthetic activation. HMAC/HTTP possession is a separate required gate.
        execute(db,"SELECT identity.accept_local_support_staff_invitation('"+proof+"','"+UUID.randomUUID()+"',decode(repeat('c1',32),'hex'),"+HASH+",'"+UUID.randomUUID()+"')");
        UUID grantKey=UUID.randomUUID(),grant;
        String command="SELECT * FROM orders.configure_local_support_staff('"+grantKey+"','"+account+"','SUPPORT_HANDLE','GRANT','Synthetic grant')";
        try(var c=DriverManager.getConnection(db.url(),operator,password);var s=c.createStatement()){
            try(var r=s.executeQuery(command)){assertTrue(r.next());grant=r.getObject("grant_id",UUID.class);assertEquals("GRANTED",r.getString("committed_outcome"));assertFalse(r.getBoolean("replayed"));assertTrue(r.getBoolean("effective_permission_active"));}
            try(var r=s.executeQuery(command)){assertTrue(r.next());assertTrue(r.getBoolean("replayed"));assertEquals(grant,r.getObject("grant_id",UUID.class));}
            assertEquals("23514",assertThrows(SQLException.class,()->s.execute(command.replace("Synthetic grant","Changed grant"))).getSQLState());
            try(var r=s.executeQuery("SELECT * FROM orders.configure_local_support_staff('"+UUID.randomUUID()+"','"+account+"','SUPPORT_HANDLE','GRANT','Synthetic no change')")){assertTrue(r.next());assertEquals("NO_CHANGE",r.getString("committed_outcome"));assertEquals(grant,r.getObject("grant_id",UUID.class));}
            try(var r=s.executeQuery("SELECT * FROM orders.configure_local_support_staff('"+UUID.randomUUID()+"','"+account+"','SUPPORT_HANDLE','REVOKE','Synthetic revoke')")){assertTrue(r.next());assertEquals("REVOKED",r.getString("committed_outcome"));}
            try(var r=s.executeQuery(command)){assertTrue(r.next());assertTrue(r.getBoolean("replayed"));assertEquals(grant,r.getObject("grant_id",UUID.class));assertEquals("REVOKED",r.getString("current_grant_state"));assertFalse(r.getBoolean("effective_permission_active"));}
            try(var r=s.executeQuery("SELECT * FROM orders.configure_local_support_staff('"+UUID.randomUUID()+"','"+account+"','SUPPORT_HANDLE','GRANT','Synthetic fresh grant')")){assertTrue(r.next());assertNotEquals(grant,r.getObject("grant_id",UUID.class));}
            assertEquals("42501",assertThrows(SQLException.class,()->s.execute("SELECT orders.configure_local_support_staff('"+account+"','SUPPORT_HANDLE','GRANT')")).getSQLState());
        }
        try(var c=db.runtimeConnection();var s=c.createStatement();var r=s.executeQuery("SELECT orders.organization_support_staff(gen_random_uuid(),'"+account+"','SUPPORT_HANDLE')")){
            assertTrue(r.next());assertFalse(r.getBoolean(1));
        }
        rejects(db,"42501","SELECT * FROM orders.support_grant_operator_receipt");
        rejects(db,"42501",command);
        try(var c=db.migrationConnection();var s=c.createStatement()){
            s.execute("SET ROLE "+db.owner());
            try(var r=s.executeQuery("SELECT (SELECT count(*) FROM orders.support_grant_operator_receipt),(SELECT count(*) FROM orders.support_business_audit WHERE operator_request_id IS NOT NULL)")){
                assertTrue(r.next());assertEquals(4,r.getLong(1));assertEquals(3,r.getLong(2));
            }
        }
        assertEquals(0,db.flyway("37").migrate().migrationsExecuted);
    }

    @Test void staffV36RealCryptoActivationAndSessionAudienceStayIsolated() throws Exception {
        Database db=PostgresTestcontainersSupport.database(POSTGRES);db.flyway("37").migrate();
        String operator="staff_operator_"+UUID.randomUUID().toString().replace("-","");String password=PostgresTestcontainersSupport.password();
        try(var c=DriverManager.getConnection(PostgresTestcontainersSupport.currentUrl(POSTGRES,"postgres"),POSTGRES.getUsername(),POSTGRES.getPassword());var s=c.createStatement()){
            s.execute("CREATE ROLE "+operator+" LOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE PASSWORD '"+password+"'");
            s.execute("GRANT elogistics_local_staff_operator TO "+operator);s.execute("GRANT CONNECT ON DATABASE "+db.name()+" TO "+operator);
        }
        db.ownerExecute("SELECT identity.bind_local_staff_environment('"+UUID.randomUUID()+"','TEST','Synthetic isolated bootstrap')");
        byte[] verifierKey=new byte[32],encryptionKey=new byte[32];new java.security.SecureRandom().nextBytes(verifierKey);new java.security.SecureRandom().nextBytes(encryptionKey);
        var protection=new InvitationProtection(Map.of("v1",verifierKey),"v1",Map.of("e1",encryptionKey),"e1");
        Arrays.fill(verifierKey,(byte)0);Arrays.fill(encryptionKey,(byte)0);
        var operatorSource=new org.springframework.jdbc.datasource.DriverManagerDataSource(db.url(),operator,password);
        var runtimeSource=new org.springframework.jdbc.datasource.DriverManagerDataSource(db.url(),db.runtime(),db.runtimePassword());
        var provisioner=new LocalSupportStaffProvisioner(operatorSource,protection);
        var command=new LocalSupportStaffProvisioner.OfflineStaffProvision(UUID.randomUUID(),"Synthetic Staff","verified-staff@example.invalid","Synthetic real cryptography");
        var receipt=provisioner.provision(command);assertFalse(receipt.replayed());assertEquals("PENDING",receipt.currentPrincipalState());
        var replay=provisioner.provision(command);assertTrue(replay.replayed());assertEquals(receipt.accountId(),replay.accountId());
        var clock=java.time.Clock.systemUTC();
        var policy=new JdbcInvitationOutbox.Policy(3,java.time.Duration.ofSeconds(30),java.time.Duration.ofSeconds(2));
        var systemWorker=new JdbcInvitationOutbox(runtimeSource,protection,policy,clock);
        assertEquals(0,systemWorker.purge());assertTrue(systemWorker.claim().isEmpty());
        var worker=new JdbcInvitationOutbox(runtimeSource,protection,policy,clock,com.elogistic.backend.identity.internal.domain.ActionProof.Purpose.STAFF_INV);
        byte[] token;
        try(var inbox=new CustomerSmtpTestInbox(1,false,false,250,null)){
            var transport=new SmtpCustomerDelivery(new SmtpCustomerDelivery.Settings("127.0.0.1",inbox.port(),SmtpCustomerDelivery.Mode.TEST_LOOPBACK,null,null,"staff-sender@example.invalid",Set.of("example.invalid"),"https://127.0.0.1:5173",true),clock);
            var deliveryWorker=new StaffInvitationDeliveryWorker(worker,transport);
            assertTrue(deliveryWorker.runOne());assertFalse(deliveryWorker.runOne());
            var messages=inbox.messages();assertEquals(1,messages.size());
            var message=new jakarta.mail.internet.MimeMessage(jakarta.mail.Session.getInstance(new java.util.Properties()),new java.io.ByteArrayInputStream(messages.getFirst()));
            String body=CustomerSmtpTestInbox.plainText(message);
            assertTrue(body.contains("/staff/activate#proofId="+receipt.proofId()+"&token="));assertFalse(body.contains("/verify-email"));
            var match=java.util.regex.Pattern.compile("&token=([A-Za-z0-9_-]{43})").matcher(body);assertTrue(match.find());
            token=match.group(1).getBytes(java.nio.charset.StandardCharsets.US_ASCII);
            assertEquals(0,inbox.authentications.get());
        }
        var activation=new JdbcStaffInvitationActivation(runtimeSource,protection,clock);UUID activationKey=UUID.randomUUID();
        String initialPassword="Synthetic initial password 7!";
        try{
            byte[] wrong=token.clone();wrong[0]=wrong[0]=='A'?(byte)'B':(byte)'A';
            var invalid=assertThrows(com.elogistic.backend.identity.api.IdentityCommandFailure.class,()->activation.accept(receipt.proofId(),wrong,initialPassword,activationKey,UUID.randomUUID()));
            assertEquals(com.elogistic.backend.identity.api.IdentityCommandFailure.Code.TOKEN_INVALID_OR_EXPIRED,invalid.code());Arrays.fill(wrong,(byte)0);
            activation.accept(receipt.proofId(),token,initialPassword,activationKey,UUID.randomUUID());
            activation.accept(receipt.proofId(),token,initialPassword,activationKey,UUID.randomUUID());
            assertThrows(com.elogistic.backend.identity.api.IdentityCommandFailure.class,()->activation.accept(receipt.proofId(),token,initialPassword,UUID.randomUUID(),UUID.randomUUID()));
        }finally{Arrays.fill(token,(byte)0);}
        var credentials=new ProtectedSessionCredentials();var staffStore=new JdbcIdentitySessionStore(runtimeSource,IdentitySessionStorage.STAFF);
        var sessions=com.elogistic.backend.identity.internal.application.IdentitySessions.staff(staffStore,credentials,(o,r,s,p)->{},clock);
        UUID session;byte[] refresh;
        try(var result=sessions.loginIdentity(new com.elogistic.backend.identity.api.IdentitySessionCommands.Login(command.email(),initialPassword,UUID.randomUUID()))){
            session=result.access().sessionId();refresh=result.refreshToken();assertEquals(receipt.accountId(),result.access().accountId());
        }
        byte[] digest=credentials.digest(refresh);
        try{
            var systemStore=new JdbcIdentitySessionStore(runtimeSource,IdentitySessionStorage.SYSTEM);
            systemStore.transaction(tx->{assertTrue(tx.findRefresh(digest).isEmpty());assertTrue(tx.accounts().sessionAccount(session).isEmpty());assertTrue(tx.accounts().lockSession(session).isEmpty());assertFalse(tx.revokeFamily(session,clock.instant()));return null;});
            var staffBrowser=new JdbcIdentityBrowserState(runtimeSource,IdentitySessionStorage.STAFF);
            var systemBrowser=new JdbcIdentityBrowserState(runtimeSource,IdentitySessionStorage.SYSTEM);
            var challenge=staffBrowser.rebind(null,session);staffBrowser.verify(challenge.cookie(),challenge.token(),session,false);
            assertThrows(com.elogistic.backend.identity.api.IdentityCommandFailure.class,()->systemBrowser.verify(challenge.cookie(),challenge.token(),session,false));
            try(var logout=new com.elogistic.backend.identity.api.IdentitySessionCommands.Token(refresh,UUID.randomUUID())){sessions.logoutIdentity(logout);}
        }finally{Arrays.fill(refresh,(byte)0);Arrays.fill(digest,(byte)0);}
        assertEquals(0,managementSnapshot(db,"identity.account_grant").size());
        assertEquals(0,managementSnapshot(db,"merchant.membership").size());
        // Real HTTP adapter -> CSRF/rate limiter -> shared Identity engine -> PostgreSQL.
        var generator=java.security.KeyPairGenerator.getInstance("RSA");generator.initialize(3072);var pair=generator.generateKeyPair();
        var tokens=new com.elogistic.backend.identity.internal.web.ProductionAccessTokens("https://staff.example.invalid","argopost-local-support-staff","test",(java.security.interfaces.RSAPrivateKey)pair.getPrivate(),Map.of("test",(java.security.interfaces.RSAPublicKey)pair.getPublic()),clock);
        byte[] limiterKey=new byte[32];new java.security.SecureRandom().nextBytes(limiterKey);
        var limiter=new JdbcIdentityRateLimiter(runtimeSource,limiterKey);Arrays.fill(limiterKey,(byte)0);
        var transport=new SmtpCustomerDelivery(new SmtpCustomerDelivery.Settings("127.0.0.1",12345,SmtpCustomerDelivery.Mode.TEST_LOOPBACK,null,null,"staff-sender@example.invalid",Set.of("example.invalid"),"https://127.0.0.1:5173",true),clock);
        var services=com.elogistic.backend.identity.internal.ProductionStaffComposition.create(runtimeSource,tokens,protection,limiter,transport,"https://127.0.0.1:5173","TEST",clock,()->true);
        var csrf=staffHttp(services.guard(),"GET","/api/v1/staff-auth/csrf",null,null,null,null,null);assertEquals(200,csrf.getStatus());
        var json=tools.jackson.databind.json.JsonMapper.builder().build();String challenge=json.readTree(csrf.getContentAsString()).get("csrfToken").asString();
        String csrfCookie=staffCookie(csrf,"__Host-argopost.staff.csrf");
        var login=staffHttp(services.guard(),"POST","/api/v1/staff-auth/login",json.writeValueAsString(Map.of("email",command.email(),"password",initialPassword)),csrfCookie,null,null,challenge);assertEquals(200,login.getStatus());
        String access=json.readTree(login.getContentAsString()).get("accessToken").asString();
        String httpRefresh=staffCookie(login,"__Host-argopost.staff.refresh");csrfCookie=staffCookie(login,"__Host-argopost.staff.csrf");
        var me=staffHttp(services.guard(),"GET","/api/v1/staff-auth/me",null,null,null,access,null);assertEquals(200,me.getStatus());
        assertEquals("STAFF",json.readTree(me.getContentAsString()).get("principalType").asString());assertFalse(me.getContentAsString().contains(command.email()));
        var refreshedCsrf=staffHttp(services.guard(),"GET","/api/v1/staff-auth/csrf",null,csrfCookie,httpRefresh,null,null);assertEquals(200,refreshedCsrf.getStatus());
        challenge=json.readTree(refreshedCsrf.getContentAsString()).get("csrfToken").asString();csrfCookie=staffCookie(refreshedCsrf,"__Host-argopost.staff.csrf");
        var refreshed=staffHttp(services.guard(),"POST","/api/v1/staff-auth/refresh",null,csrfCookie,httpRefresh,null,challenge);assertEquals(200,refreshed.getStatus());
        access=json.readTree(refreshed.getContentAsString()).get("accessToken").asString();httpRefresh=staffCookie(refreshed,"__Host-argopost.staff.refresh");
        var logout=staffHttp(services.guard(),"POST","/api/v1/staff-auth/logout",null,csrfCookie,httpRefresh,null,challenge);assertEquals(204,logout.getStatus());
        assertEquals(401,staffHttp(services.guard(),"GET","/api/v1/staff-auth/me",null,null,null,access,null).getStatus());
    }

    private org.springframework.mock.web.MockHttpServletResponse staffHttp(com.elogistic.backend.identity.internal.web.ProductionStaffGuard guard,String method,String path,String body,String csrf,String refresh,String bearer,String challenge)throws Exception{
        var request=new org.springframework.mock.web.MockHttpServletRequest(method,path);request.setSecure(true);request.setRemoteAddr("127.0.0.1");request.addHeader("Origin","https://127.0.0.1:5173");
        if(body!=null){request.setContentType("application/json");request.setContent(body.getBytes(java.nio.charset.StandardCharsets.UTF_8));}
        if(csrf!=null||refresh!=null){var cookies=new ArrayList<jakarta.servlet.http.Cookie>();if(csrf!=null)cookies.add(new jakarta.servlet.http.Cookie("__Host-argopost.staff.csrf",csrf));if(refresh!=null)cookies.add(new jakarta.servlet.http.Cookie("__Host-argopost.staff.refresh",refresh));request.setCookies(cookies.toArray(jakarta.servlet.http.Cookie[]::new));}
        if(bearer!=null)request.addHeader("Authorization","Bearer "+bearer);if(challenge!=null)request.addHeader("X-CSRF-Token",challenge);
        var response=new org.springframework.mock.web.MockHttpServletResponse();guard.doFilter(request,response,new org.springframework.mock.web.MockFilterChain());return response;
    }
    private String staffCookie(org.springframework.mock.web.MockHttpServletResponse response,String name){
        var value=response.getHeaders("Set-Cookie").stream().filter(v->v.startsWith(name+"=")).findFirst().orElseThrow();
        assertTrue(value.contains("; Secure; HttpOnly; SameSite=Strict"));return value.substring(name.length()+1,value.indexOf(';'));
    }

    private long staffPrivateReceiptCount(Database db) throws SQLException {
        try (var c=db.migrationConnection(); var s=c.createStatement()) {
            s.execute("SET ROLE " + db.owner());
            try (var r=s.executeQuery("SELECT count(*) FROM identity.local_staff_operator_receipt")) { assertTrue(r.next()); return r.getLong(1); }
        }
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
        assertEquals(List.of("1","2","3","4","5","6","7","8","9","10","11","12","13","14","15","16","17","18","19","20","21","22","23","24","25","26","27","28","29","30","31","32","34","35","36","37","38","39","40","41","42","43","44","45","46","47"), Arrays.stream(flyway.info().applied()).map(m -> m.getVersion().toString()).toList());
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
        assertEquals(44,db.flyway("latest").migrate().migrationsExecuted); db.flyway("latest").validate();
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
        assertEquals(43,db.flyway("latest").migrate().migrationsExecuted);db.flyway("latest").validate();
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
        assertEquals(42,db.flyway("latest").migrate().migrationsExecuted);db.flyway("latest").validate();assertEquals(0,db.flyway("latest").migrate().migrationsExecuted);
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
        assertEquals(38,db.flyway("latest").migrate().migrationsExecuted);db.flyway("latest").validate();assertEquals(0,db.flyway("latest").migrate().migrationsExecuted);
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
            var checksums=Arrays.stream(db.flyway("16").info().applied()).map(m->m.getChecksum()).toList();assertEquals(30,db.flyway("latest").migrate().migrationsExecuted);
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
        Database db=PostgresTestcontainersSupport.database(POSTGRES);assertEquals(11,db.flyway("11").migrate().migrationsExecuted);account(db);execute(db,session(SESSION));var rows=snapshot(db,"identity.user_account");var sessions=snapshot(db,"(SELECT id,account_id,auth_version,created_at,absolute_expires_at,last_activity_at,idle_expires_at,revoked_at FROM identity.auth_session)");var checksums=Arrays.stream(db.flyway("11").info().applied()).map(m->m.getChecksum()).toList();
        var source=new org.springframework.jdbc.datasource.DriverManagerDataSource(db.url(),db.runtime(),db.runtimePassword());assertEquals("CUSTOMER_POSTGRES18_SCHEMA_V17_REQUIRED",assertThrows(IllegalStateException.class,()->CustomerRuntimeRequirements.verify(source)).getMessage());
        assertEquals(35,db.flyway("latest").migrate().migrationsExecuted);db.flyway("latest").validate();assertEquals(0,db.flyway("latest").migrate().migrationsExecuted);assertEquals(checksums,Arrays.stream(db.flyway("latest").info().applied()).limit(11).map(m->m.getChecksum()).toList());assertEquals(rows,snapshot(db,"identity.user_account"));assertEquals(sessions,snapshot(db,"(SELECT id,account_id,auth_version,created_at,absolute_expires_at,last_activity_at,idle_expires_at,revoked_at FROM identity.auth_session)"));assertDoesNotThrow(()->CustomerRuntimeRequirements.verify(source));try(var c=db.runtimeConnection();var q=c.createStatement();var r=q.executeQuery("SELECT audience FROM identity.auth_session")){assertTrue(r.next());assertEquals("SYSTEM",r.getString(1));}
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
        assertEquals(33,db.flyway("latest").migrate().migrationsExecuted);db.flyway("latest").validate();assertEquals(0,db.flyway("latest").migrate().migrationsExecuted);assertEquals(checksums,Arrays.stream(db.flyway("latest").info().applied()).limit(13).map(m->m.getChecksum()).toList());assertEquals(profile,snapshot(db,"merchant.customer_profile"));assertDoesNotThrow(()->com.elogistic.backend.merchant.internal.infrastructure.JdbcMerchantApplicationStore.requireSchema(source));
        rejects(db,"42501","DELETE FROM merchant.application");rejects(db,"42501","UPDATE merchant.application_draft_version SET draft='{}'");rejects(db,"42501","DELETE FROM merchant.application_draft_version");rejects(db,"42501","ALTER TABLE merchant.application ADD COLUMN injected BOOLEAN");
        try(Connection c=db.runtimeConnection();Statement q=c.createStatement();ResultSet r=q.executeQuery("SELECT count(*) FROM pg_constraint c JOIN pg_class t ON t.oid=c.conrelid JOIN pg_namespace n ON n.oid=t.relnamespace JOIN pg_class f ON f.oid=c.confrelid JOIN pg_namespace fn ON fn.oid=f.relnamespace WHERE c.contype='f' AND n.nspname='merchant' AND fn.nspname<>'merchant'")){assertTrue(r.next());assertEquals(0,r.getInt(1));}
    }

    @Test void addressV18UpgradePreservesV17DataChecksumsAndRuntimeBoundary()throws Exception{
        Database db=PostgresTestcontainersSupport.database(POSTGRES);assertEquals(17,db.flyway("17").migrate().migrationsExecuted);account(db);profileFixture(db);var profile=snapshot(db,"merchant.customer_profile");var identity=snapshot(db,"identity.user_account");var checksums=Arrays.stream(db.flyway("17").info().applied()).map(m->m.getChecksum()).toList();
        var source=new org.springframework.jdbc.datasource.DriverManagerDataSource(db.url(),db.runtime(),db.runtimePassword());assertEquals("PERSONAL_ADDRESS_POSTGRES18_SCHEMA_V18_REQUIRED",assertThrows(IllegalStateException.class,()->com.elogistic.backend.merchant.internal.infrastructure.JdbcPersonalAddressStore.requireSchema(source)).getMessage());
        assertEquals(29,db.flyway("latest").migrate().migrationsExecuted);db.flyway("latest").validate();assertEquals(0,db.flyway("latest").migrate().migrationsExecuted);assertEquals(checksums,Arrays.stream(db.flyway("latest").info().applied()).limit(17).map(m->m.getChecksum()).toList());assertEquals(profile,snapshot(db,"merchant.customer_profile"));assertEquals(identity,snapshot(db,"identity.user_account"));assertDoesNotThrow(()->com.elogistic.backend.merchant.internal.infrastructure.JdbcPersonalAddressStore.requireSchema(source));
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
        var source=new org.springframework.jdbc.datasource.DriverManagerDataSource(db.url(),db.runtime(),db.runtimePassword());assertEquals("SHIPPING_DRAFT_POSTGRES18_V19_REQUIRED",assertThrows(IllegalStateException.class,()->com.elogistic.backend.orders.internal.infrastructure.JdbcShippingDraftStore.requireSchema(source)).getMessage());assertEquals(28,db.flyway("latest").migrate().migrationsExecuted);db.flyway("latest").validate();assertEquals(0,db.flyway("latest").migrate().migrationsExecuted);assertEquals(checksums,Arrays.stream(db.flyway("latest").info().applied()).limit(18).map(m->m.getChecksum()).toList());assertEquals(profile,snapshot(db,"merchant.customer_profile"));assertEquals(addresses,snapshot(db,"merchant.customer_address"));assertEquals(addressHistory,snapshot(db,"merchant.customer_address_version"));assertDoesNotThrow(()->com.elogistic.backend.orders.internal.infrastructure.JdbcShippingDraftStore.requireSchema(source));
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
    @Test void businessV26UpgradePreservesPopulatedPersonalBookingAndOwnerGuards()throws Exception{populatedV26Booking();}
    @Test void managementV27UpgradePreservesPopulatedV26WithoutGrantingAuthority()throws Exception{
        Database db=populatedV26Booking();
        var tables=List.of("identity.user_account","merchant.customer_profile","merchant.customer_address","orders.shipping_draft","orders.shipping_draft_version","pricing.pilot_quote","orders.pilot_shipment","orders.pilot_parcel","orders.pilot_booking_receipt","finance.pilot_shipping_obligation","merchant.organization","merchant.membership","merchant.member_grant","orders.organization_preparation","orders.organization_preparation_version");
        Map<String,List<String>> before=new LinkedHashMap<>();for(String table:tables)before.put(table,managementSnapshot(db,table));
        var checksums=Arrays.stream(db.flyway("26").info().applied()).map(m->m.getChecksum()).toList();assertEquals(1,db.flyway("27").migrate().migrationsExecuted);db.flyway("27").validate();
        assertEquals(checksums,Arrays.stream(db.flyway("27").info().applied()).limit(26).map(m->m.getChecksum()).toList());for(String table:tables)assertEquals(before.get(table),managementSnapshot(db,table));
        for(String table:List.of("organization_shipment_assignment","organization_shipment_assignment_event","organization_shipment_assignment_receipt"))assertTrue(managementSnapshot(db,"orders."+table).isEmpty());
        rejects(db,"42501","SELECT * FROM orders.organization_shipment_export_audit");rejects(db,"42501","SELECT merchant.configure_local_workspace(gen_random_uuid(),'GRANT',gen_random_uuid(),'Synthetic',"+ACCOUNT+",'OWNER',ARRAY['shipment.assign'],'INVALID_RUNTIME')");
        assertEquals(0,db.flyway("27").migrate().migrationsExecuted);
    }
    @Test void reportingV28UpgradePreservesPopulatedBookingOrganizationAndExplicitGrants()throws Exception{
        Database db=populatedV26Booking();assertEquals(1,db.flyway("27").migrate().migrationsExecuted);account(db);execute(db,"UPDATE identity.user_account SET status='ACTIVE',activated_at="+NOW+",email_verified=TRUE,password_hash="+HASH+" WHERE id="+ACCOUNT);execute(db,"INSERT INTO identity.customer_identifier VALUES("+OTHER+","+ACCOUNT+",'identity@example.invalid',"+NOW+",'ACTIVE',1)");
        String command="SELECT merchant.configure_local_workspace("+KEY+",'PROVISION',"+PROOF+",'Synthetic reports upgrade',"+ACCOUNT+",'OWNER',ARRAY['dashboard.read','shipment.draft.read','shipment.export'],'Synthetic V28 baseline')";db.ownerExecute("BEGIN;SET LOCAL argopost.operator_environment='TEST';"+command+";COMMIT");
        var tables=List.of("identity.user_account","identity.customer_identifier","merchant.organization","merchant.membership","merchant.member_grant","merchant.workspace_audit","merchant.workspace_command_receipt","orders.shipping_draft","pricing.pilot_quote","orders.pilot_shipment","orders.pilot_parcel","finance.pilot_shipping_obligation");var before=new LinkedHashMap<String,List<String>>();
        for(String table:tables)try(var c=db.migrationConnection();var q=c.createStatement()){q.execute("SET ROLE "+db.owner());try(var r=q.executeQuery("SELECT to_jsonb(t)::text FROM "+table+" t ORDER BY 1")){var rows=new ArrayList<String>();while(r.next())rows.add(r.getString(1));before.put(table,rows);}}
        var checksums=Arrays.stream(db.flyway("27").info().applied()).map(m->m.getChecksum()).toList();assertEquals(19,db.flyway("latest").migrate().migrationsExecuted);db.flyway("latest").validate();assertEquals(checksums,Arrays.stream(db.flyway("latest").info().applied()).limit(27).map(m->m.getChecksum()).toList());
        db.ownerExecute("BEGIN;SET LOCAL argopost.operator_environment='TEST';"+command+";COMMIT");for(String table:tables)try(var c=db.migrationConnection();var q=c.createStatement()){q.execute("SET ROLE "+db.owner());try(var r=q.executeQuery("SELECT to_jsonb(t)::text FROM "+table+" t ORDER BY 1")){var rows=new ArrayList<String>();while(r.next())rows.add(r.getString(1));assertEquals(before.get(table),rows);}}
        assertTrue(managementSnapshot(db,"orders.organization_report_export_audit").isEmpty());rejects(db,"42501","SELECT merchant.configure_local_workspace(gen_random_uuid(),'GRANT',"+PROOF+",NULL,"+ACCOUNT+",'OWNER',ARRAY['dashboard.read','shipment.draft.read','shipment.finance.read'],'Synthetic runtime denial')");assertEquals(0,db.flyway("latest").migrate().migrationsExecuted);
    }

    private List<String> managementSnapshot(Database db,String table)throws Exception{
        try(var c=db.runtimeConnection();var q=c.createStatement();var r=q.executeQuery("SELECT to_jsonb(t)::text FROM "+table+" t ORDER BY 1")){var rows=new ArrayList<String>();while(r.next())rows.add(r.getString(1));return rows;}
    }
    @Test void facilityV31UpgradePreservesLegacySitesContactsAndAllPriorChecksums()throws Exception{
        Database db=populatedV26Booking();assertEquals(4,db.flyway("30").migrate().migrationsExecuted);account(db);execute(db,"UPDATE identity.user_account SET status='ACTIVE',activated_at="+NOW+",email_verified=TRUE,password_hash="+HASH+" WHERE id="+ACCOUNT);execute(db,"INSERT INTO identity.customer_identifier VALUES("+OTHER+","+ACCOUNT+",'identity@example.invalid',"+NOW+",'ACTIVE',1)");
        db.ownerExecute("BEGIN;SET LOCAL argopost.operator_environment='TEST';SELECT merchant.configure_local_workspace("+KEY+",'PROVISION',"+PROOF+",'Synthetic legacy site owner',"+ACCOUNT+",'OWNER',ARRAY['dashboard.read'],'Synthetic legacy facility');INSERT INTO merchant.facility VALUES("+NEXT+","+PROOF+",'Legacy site','{\"name\":\"Synthetic contact\",\"phone\":\"+84901234567\",\"address\":{\"country\":\"VN\",\"administrativeArea\":\"Synthetic area\",\"locality\":\"Synthetic locality\",\"addressLine\":\"Synthetic street\"}}','ACTIVE',7,now(),now());COMMIT");
        var tables=List.of("identity.user_account","identity.customer_identifier","merchant.organization","merchant.membership","merchant.member_grant","merchant.workspace_audit","merchant.workspace_command_receipt","orders.shipping_draft","pricing.pilot_quote","orders.pilot_shipment","orders.pilot_parcel","finance.pilot_shipping_obligation");var before=new LinkedHashMap<String,List<String>>();
        for(String table:tables)try(var c=db.migrationConnection();var q=c.createStatement()){q.execute("SET ROLE "+db.owner());try(var r=q.executeQuery("SELECT to_jsonb(t)::text FROM "+table+" t ORDER BY 1")){var rows=new ArrayList<String>();while(r.next())rows.add(r.getString(1));before.put(table,rows);}}
        var checksums=Arrays.stream(db.flyway("30").info().applied()).map(m->m.getChecksum()).toList();assertEquals(1,db.flyway("31").migrate().migrationsExecuted);db.flyway("31").validate();assertEquals(checksums,Arrays.stream(db.flyway("31").info().applied()).limit(30).map(m->m.getChecksum()).toList());
        for(String table:tables)try(var c=db.migrationConnection();var q=c.createStatement()){q.execute("SET ROLE "+db.owner());try(var r=q.executeQuery("SELECT to_jsonb(t)::text FROM "+table+" t ORDER BY 1")){var rows=new ArrayList<String>();while(r.next())rows.add(r.getString(1));assertEquals(before.get(table),rows);}}
        try(var c=db.runtimeConnection();var q=c.createStatement();var r=q.executeQuery("SELECT f.version=7 AND f.code IS NULL AND f.kind IS NULL AND f.operating_hours IS NULL AND h.baseline AND h.snapshot=to_jsonb(f) FROM merchant.facility f JOIN merchant.facility_version h ON h.target_id=f.id AND h.resource_kind='SITE' WHERE f.id="+NEXT)){assertTrue(r.next());assertTrue(r.getBoolean(1));}
        assertTrue(managementSnapshot(db,"merchant.facility_audit").isEmpty());assertTrue(managementSnapshot(db,"merchant.facility_default").isEmpty());
        rejects(db,"42501","SELECT merchant.configure_local_facility_grants(gen_random_uuid(),'GRANT',"+PROOF+","+ACCOUNT+",NULL,ARRAY['facility.read'],'runtime denial')");rejects(db,"23514","UPDATE merchant.facility SET version=version+1 WHERE id="+NEXT);
        var priorTables=List.of("merchant.facility","merchant.facility_version","merchant.facility_default","merchant.facility_team","merchant.facility_responsibility","merchant.facility_audit","merchant.facility_command_receipt","merchant.member_grant");var priorRows=new LinkedHashMap<String,List<String>>();for(String table:priorTables)priorRows.put(table,managementSnapshot(db,table));var checksums31=Arrays.stream(db.flyway("31").info().applied()).map(m->m.getChecksum()).toList();assertEquals(1,db.flyway("32").migrate().migrationsExecuted);for(String table:priorTables)assertEquals(priorRows.get(table),managementSnapshot(db,table));assertEquals(checksums31,Arrays.stream(db.flyway("32").info().applied()).limit(31).map(m->m.getChecksum()).toList());assertTrue(managementSnapshot(db,"orders.merchant_parcel_preparation").isEmpty());assertTrue(managementSnapshot(db,"orders.merchant_handover_preparation").isEmpty());assertTrue(managementSnapshot(db,"orders.merchant_facility_work_event").isEmpty());

    }
    private Database populatedV26Booking()throws Exception{
        Database db=PostgresTestcontainersSupport.database(POSTGRES);assertEquals(25,db.flyway("25").migrate().migrationsExecuted);
        var mapper=tools.jackson.databind.json.JsonMapper.builder().build();java.time.Instant time=java.time.Instant.parse("2026-11-02T00:00:00Z");
        UUID owner=UUID.fromString(ACCOUNT.replace("'","")),draft=UUID.fromString(PROOF.replace("'","")),shipment=UUID.fromString(SESSION.replace("'","")),quote=UUID.fromString(BINDING.replace("'",""));
        var address=new com.elogistic.backend.orders.api.ShippingDrafts.Address("VN","Synthetic area","Synthetic locality","Synthetic street");var contact=new com.elogistic.backend.orders.api.ShippingDrafts.Contact("Synthetic contact","+84901234567",address);
        var parcel=new com.elogistic.backend.orders.api.ShippingDrafts.Parcel("Synthetic upgrade parcel",1,500,100,100,100,0);var input=new com.elogistic.backend.orders.api.ShippingDrafts.Input("SYNTHETIC_UPGRADE",null,contact,contact,List.of(parcel));
        var route=new com.elogistic.backend.pricing.api.PilotQuotes.Route("ARGO-SIM-COVERAGE-1","SIM-A1","SIM-A2","INTRA","SIM-DROP-A1");var facts=List.of(new com.elogistic.backend.pricing.api.PilotQuotes.Parcel(500,100,100,100));var lines=List.of(new com.elogistic.backend.pricing.api.PilotQuotes.Line(1,500,500,25000));
        var priced=new com.elogistic.backend.pricing.api.PilotQuotes.Quote(quote,owner,draft,1,"ARGO-ROAD-TARIFF-1",route,facts,lines,25000,"SIMULATION_NOT_TAX_INVOICE",time,time.plusSeconds(900),shipment);
        execute(db,"BEGIN;INSERT INTO orders.shipping_draft VALUES("+PROOF+",'PERSONAL',"+ACCOUNT+",'BOOKED',1,'2026-11-02T00:00:00Z','2026-11-02T00:00:00Z');INSERT INTO orders.shipping_draft_version VALUES("+PROOF+","+ACCOUNT+",1,'BOOKED','"+mapper.writeValueAsString(input)+"','2026-11-02T00:00:00Z','2026-11-02T00:00:00Z');INSERT INTO pricing.pilot_quote VALUES("+BINDING+","+ACCOUNT+","+PROOF+",1,'ARGO-ROAD-TARIFF-1','ARGO-SIM-COVERAGE-1','SIM-A1','SIM-A2','INTRA',TRUE,'SIM-DROP-A1','"+mapper.writeValueAsString(facts)+"','"+mapper.writeValueAsString(lines)+"',25000,'2026-11-02T00:00:00Z','2026-11-02 00:15:00Z',"+KEY+",'v1',"+digest(1)+","+SESSION+",'2026-11-02T00:00:00Z');INSERT INTO orders.pilot_shipment VALUES("+SESSION+","+ACCOUNT+",'PERSONAL','AP-SIM-"+shipment.toString().replace("-","")+"',"+PROOF+",1,'CONFIRMED','NOT_ACCEPTED','"+mapper.writeValueAsString(input)+"','"+mapper.writeValueAsString(priced)+"',"+BINDING+",25000,'2026-11-02T00:00:00Z');INSERT INTO orders.pilot_parcel VALUES("+OTHER_SESSION+","+SESSION+",1,'Synthetic upgrade parcel',1,500,100,100,100,0);INSERT INTO finance.pilot_shipping_obligation VALUES("+TOKEN+","+ACCOUNT+","+SESSION+","+BINDING+",25000,'VND','UNPAID','SENDER','CASH_ON_HANDOVER','BR13-LOCAL-PILOT-01',"+KEY+",'2026-11-02T00:00:00Z');INSERT INTO orders.pilot_booking_receipt VALUES("+ACCOUNT+","+KEY+","+PROOF+",1,"+BINDING+","+SESSION+",'2026-11-02T00:00:00Z');COMMIT");
        String projection="(to_jsonb(t)-'owner_type'-'organization_id'-'facility_id'-'creator_account_id'-'organization_preparation_id'-'organization_preparation_version'-'contract_id'-'contract_version')::text";List<String> tables=List.of("pricing.pilot_quote","orders.pilot_shipment","orders.pilot_parcel","orders.pilot_booking_receipt","finance.pilot_shipping_obligation");Map<String,List<String>> before=new LinkedHashMap<>();
        for(String table:tables)try(var c=db.runtimeConnection();var q=c.createStatement();var r=q.executeQuery("SELECT "+projection+" FROM "+table+" t ORDER BY 1")){var rows=new ArrayList<String>();while(r.next())rows.add(r.getString(1));before.put(table,rows);}
        var checksums=Arrays.stream(db.flyway("25").info().applied()).map(m->m.getChecksum()).toList();assertEquals(1,db.flyway("26").migrate().migrationsExecuted);db.flyway("26").validate();assertEquals(checksums,Arrays.stream(db.flyway("26").info().applied()).limit(25).map(m->m.getChecksum()).toList());
        for(String table:tables)try(var c=db.runtimeConnection();var q=c.createStatement();var r=q.executeQuery("SELECT "+projection+" FROM "+table+" t ORDER BY 1")){var rows=new ArrayList<String>();while(r.next())rows.add(r.getString(1));assertEquals(before.get(table),rows);}
        assertThrows(SQLException.class,()->db.ownerExecute("UPDATE orders.pilot_shipment SET owner_id=NULL"));assertThrows(SQLException.class,()->db.ownerExecute("UPDATE finance.pilot_shipping_obligation SET owner_id=NULL"));rejects(db,"42501","UPDATE pricing.pilot_quote SET total_vnd=1");rejects(db,"42501","DELETE FROM orders.pilot_shipment");rejects(db,"42501","INSERT INTO pricing.organization_pilot_contract VALUES(gen_random_uuid(),1,gen_random_uuid(),'ARGO-ROAD-TARIFF-1','ACTIVE',now(),NULL,'INVALID',gen_random_uuid(),current_user,now())");
        assertEquals(0,snapshot(db,"pricing.organization_pilot_contract").size());assertEquals(0,db.flyway("26").migrate().migrationsExecuted);
        return db;
    }
    @Test void stationV21UpgradePreservesV20SnapshotsAndSeedsNoStaffAuthority()throws Exception{
        Database db=PostgresTestcontainersSupport.database(POSTGRES);assertEquals(20,db.flyway("20").migrate().migrationsExecuted);account(db);profileFixture(db);addressFixture(db);var identity=snapshot(db,"identity.user_account");var address=snapshot(db,"merchant.customer_address");var tariff=snapshot(db,"(SELECT version AS id,* FROM pricing.pilot_tariff_version)");var checksums=Arrays.stream(db.flyway("20").info().applied()).map(m->m.getChecksum()).toList();assertEquals(26,db.flyway("latest").migrate().migrationsExecuted);db.flyway("latest").validate();assertEquals(identity,snapshot(db,"identity.user_account"));assertEquals(address,snapshot(db,"merchant.customer_address"));assertEquals(tariff,snapshot(db,"(SELECT version AS id,* FROM pricing.pilot_tariff_version)"));assertEquals(checksums,Arrays.stream(db.flyway("latest").info().applied()).limit(20).map(m->m.getChecksum()).toList());
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
        Map<String,List<String>> before=new LinkedHashMap<>();String stripped="(to_jsonb(t)-'result_snapshot'-'preparation_target'-'preparation_version'-'import_target'-'booking_target'-'booking_version')::text";
        var tables=List.of("orders.organization_preparation","orders.organization_preparation_version","orders.organization_preparation_audit","orders.organization_preparation_receipt");
        for(String table:tables)try(var c=db.runtimeConnection();var q=c.createStatement();var r=q.executeQuery("SELECT "+stripped+" FROM "+table+" t ORDER BY 1")){var rows=new ArrayList<String>();while(r.next())rows.add(r.getString(1));before.put(table,rows);}
        var checksums=Arrays.stream(db.flyway("24").info().applied()).map(m->m.getChecksum()).toList();assertEquals(22,db.flyway("latest").migrate().migrationsExecuted);db.flyway("latest").validate();assertEquals(checksums,Arrays.stream(db.flyway("latest").info().applied()).limit(24).map(m->m.getChecksum()).toList());
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
        var sums=Arrays.stream(db.flyway("23").info().applied()).map(m->m.getChecksum()).toList();assertEquals(1,db.flyway("24").migrate().migrationsExecuted);
        rejects(db,"42501","INSERT INTO merchant.facility VALUES(gen_random_uuid(),"+PROOF+",'Synthetic','{}','ACTIVE',1,now(),now())");
        assertEquals(22,db.flyway("latest").migrate().migrationsExecuted);db.flyway("latest").validate();
        assertEquals(sums,Arrays.stream(db.flyway("latest").info().applied()).limit(23).map(m->m.getChecksum()).toList());
        db.ownerExecute("BEGIN;SET LOCAL argopost.operator_environment='TEST';"+command+";COMMIT");
        for(String table:tables)try(var c=db.migrationConnection();var q=c.createStatement()){q.execute("SET ROLE "+db.owner());try(var r=q.executeQuery("SELECT "+(table.endsWith("member_grant")?"(to_jsonb(t)-'facility_id')":"to_jsonb(t)")+"::text FROM "+table+" t ORDER BY 1")){var rows=new ArrayList<String>();while(r.next())rows.add(r.getString(1));assertEquals(before.get(table),rows);}}
        // V31 accepted runtime CRUD; a valid row still cannot commit without its exact authority/history/audit/receipt.
        rejects(db,"23514","INSERT INTO merchant.facility(id,organization_id,name,contact,state,version,created_at,updated_at,code,kind) VALUES(gen_random_uuid(),"+PROOF+",'Synthetic','{\"name\":\"Synthetic contact\",\"phone\":\"+84901234567\",\"address\":{\"country\":\"VN\",\"administrativeArea\":\"Synthetic area\",\"locality\":\"Synthetic locality\",\"addressLine\":\"Synthetic street\"}}','ACTIVE',1,now(),now(),'NO-EVIDENCE','WAREHOUSE')");assertEquals(0,snapshot(db,"merchant.facility").size());
        rejects(db,"42501","SELECT merchant.configure_local_workspace(gen_random_uuid(),'GRANT',"+PROOF+",NULL,"+ACCOUNT+",'OWNER',ARRAY['dashboard.read','shipment.draft.read','shipment.draft.write'],'Synthetic denied runtime grant')");
        assertEquals(0,snapshot(db,"orders.organization_preparation").size());assertEquals(0,db.flyway("latest").migrate().migrationsExecuted);
    }
    @Test void businessV23UpgradePreservesV22AndCannotGrantRuntimeAuthority()throws Exception{
        Database db=PostgresTestcontainersSupport.database(POSTGRES);assertEquals(22,db.flyway("22").migrate().migrationsExecuted);account(db);profileFixture(db);addressFixture(db);
        var source=new org.springframework.jdbc.datasource.DriverManagerDataSource(db.url(),db.runtime(),db.runtimePassword());assertEquals("BUSINESS_WORKSPACE_POSTGRES18_SCHEMA_V23_REQUIRED",assertThrows(IllegalStateException.class,()->com.elogistic.backend.merchant.internal.infrastructure.JdbcBusinessWorkspaces.requireSchema(source)).getMessage());
        var identity=snapshot(db,"identity.user_account");var addresses=snapshot(db,"merchant.customer_address");var applications=snapshot(db,"merchant.application");var checksums=Arrays.stream(db.flyway("22").info().applied()).map(m->m.getChecksum()).toList();
        assertEquals(24,db.flyway("latest").migrate().migrationsExecuted);db.flyway("latest").validate();assertEquals(0,db.flyway("latest").migrate().migrationsExecuted);
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
        var checksums=Arrays.stream(db.flyway("21").info().applied()).map(m->m.getChecksum()).toList();assertEquals(25,db.flyway("latest").migrate().migrationsExecuted);db.flyway("latest").validate();assertEquals(0,db.flyway("latest").migrate().migrationsExecuted);
        assertEquals(checksums,Arrays.stream(db.flyway("latest").info().applied()).limit(21).map(m->m.getChecksum()).toList());assertEquals(identity,snapshot(db,"identity.user_account"));assertEquals(addresses,snapshot(db,"merchant.customer_address"));assertEquals(stations,snapshot(db,"(SELECT code AS id,* FROM dispatch.pilot_station)"));
        rejects(db,"42501","INSERT INTO orders.support_staff_grant VALUES(gen_random_uuid(),"+ACCOUNT+",'SUPPORT_TRIAGE','ACTIVE',1,clock_timestamp())");rejects(db,"42501","SELECT orders.configure_local_support_staff("+ACCOUNT+",'SUPPORT_TRIAGE','GRANT')");
        try(var c=db.runtimeConnection();var q=c.createStatement();var r=q.executeQuery("SELECT (SELECT count(*) FROM orders.support_staff_grant)+(SELECT count(*) FROM orders.personal_support_ticket)")){assertTrue(r.next());assertEquals(0,r.getInt(1));}
        assertThrows(SQLException.class,()->db.ownerExecute("SELECT orders.configure_local_support_staff("+ACCOUNT+",'SUPPORT_TRIAGE','GRANT')"));
        assertThrows(SQLException.class,()->db.ownerExecute("SET argopost.operator_environment='TEST';SELECT orders.configure_local_support_staff("+ACCOUNT+",'SUPPORT_TRIAGE','GRANT')"));
        // Explicit synthetic fixture activation only, after preserved V21 data/checksum assertions.
        db.ownerExecute("UPDATE identity.user_account SET status='ACTIVE',activated_at="+NOW+",email_verified=TRUE,password_hash="+HASH+" WHERE id="+ACCOUNT);
        assertThrows(SQLException.class,()->db.ownerExecute("SET argopost.operator_environment='TEST';SELECT orders.configure_local_support_staff("+ACCOUNT+",'SUPPORT_TRIAGE','GRANT')"));
        try(var c=db.runtimeConnection();var q=c.createStatement();var r=q.executeQuery("SELECT count(*) FROM orders.support_business_audit WHERE authority_grant_id IS NOT NULL AND operator_principal IS NOT NULL")){assertTrue(r.next());assertEquals(0,r.getInt(1));}

    }

    @Test void teamV34PreservesV32MembershipAndAddsNoAuthority()throws Exception{
        Database db=PostgresTestcontainersSupport.database(POSTGRES);assertEquals(32,db.flyway("32").migrate().migrationsExecuted);
        db.ownerExecute("BEGIN; INSERT INTO merchant.organization VALUES("+PROOF+",'Synthetic Team Org','ACTIVE','LOCAL_OPERATOR_PROVISIONED','TEST',"+NEXT+",1,"+NOW+","+NOW+"); INSERT INTO merchant.membership VALUES("+NEXT+","+PROOF+","+ACCOUNT+",'OWNER','ACTIVE',1,1,"+NOW+","+NOW+"); INSERT INTO merchant.member_grant VALUES(gen_random_uuid(),"+NEXT+","+PROOF+",'dashboard.read','ORGANIZATION',NULL); COMMIT");
        var tables=List.of("merchant.organization","merchant.membership","merchant.member_grant");var before=new HashMap<String,List<String>>();for(String table:tables)before.put(table,managementSnapshot(db,table));
        var checksums=Arrays.stream(db.flyway("32").info().applied()).map(m->m.getChecksum()).toList();assertEquals(1,db.flyway("34").migrate().migrationsExecuted);db.flyway("34").validate();
        assertEquals(checksums,Arrays.stream(db.flyway("34").info().applied()).limit(32).map(m->m.getChecksum()).toList());for(String table:tables)assertEquals(before.get(table),managementSnapshot(db,table));
        assertEquals(1,managementSnapshot(db,"merchant.team_policy").size());
        for(String table:List.of("team_invitation","team_invitation_grant","team_audit","team_command_receipt"))assertTrue(managementSnapshot(db,"merchant."+table).isEmpty());
        try(var c=db.runtimeConnection();var q=c.createStatement();var r=q.executeQuery("SELECT merchant.team_can_delegate("+PROOF+","+ACCOUNT+",'OPERATOR','dashboard.read',NULL),merchant.team_can_delegate("+PROOF+","+ACCOUNT+",'OWNER','dashboard.read',NULL),merchant.team_can_delegate("+PROOF+","+ACCOUNT+",'OPERATOR','team.invite',NULL)")){assertTrue(r.next());assertTrue(r.getBoolean(1));assertFalse(r.getBoolean(2));assertFalse(r.getBoolean(3));}
        rejects(db,"23514","UPDATE merchant.membership SET state='SUSPENDED',version=version+1,authority_version=authority_version+1 WHERE id="+NEXT);
        assertEquals("23514",assertThrows(SQLException.class,()->db.ownerExecute("UPDATE merchant.membership SET state='SUSPENDED',version=version+1,authority_version=authority_version+1 WHERE id="+NEXT)).getSQLState());
    }
    @Test void teamV34RejectsUnownedHistoryAndCrossOrganizationForeignKeys()throws Exception{
        Database db=migrated();
        rejects(db,"23503","INSERT INTO merchant.team_command_receipt VALUES(gen_random_uuid(),gen_random_uuid(),'ACCESS',gen_random_uuid(),decode(repeat('01',32),'hex'),gen_random_uuid(),NULL,'COMMITTED',1,CURRENT_TIMESTAMP)");
        assertEquals("23503",assertThrows(SQLException.class,()->db.ownerExecute("INSERT INTO merchant.team_policy VALUES(gen_random_uuid(),1,1,CURRENT_TIMESTAMP)")).getSQLState());
        assertEquals("23514",assertThrows(SQLException.class,()->db.ownerExecute("INSERT INTO merchant.team_policy VALUES(gen_random_uuid(),0,1,CURRENT_TIMESTAMP)")).getSQLState());
        assertEquals("23514",assertThrows(SQLException.class,()->db.ownerExecute("INSERT INTO merchant.team_invitation_grant VALUES(gen_random_uuid(),gen_random_uuid(),gen_random_uuid(),'team.read','FACILITY',NULL)")).getSQLState());
    }

    @Test void organizationSupportV35PreservesV34DataChecksumsAndTeamCeilings()throws Exception{
        Database db=PostgresTestcontainersSupport.database(POSTGRES);assertEquals(32,db.flyway("32").migrate().migrationsExecuted);
        db.ownerExecute("BEGIN;INSERT INTO merchant.organization VALUES("+PROOF+",'Synthetic Support Org','ACTIVE','LOCAL_OPERATOR_PROVISIONED','TEST',"+NEXT+",1,"+NOW+","+NOW+");INSERT INTO merchant.membership VALUES("+NEXT+","+PROOF+","+ACCOUNT+",'OWNER','ACTIVE',1,1,"+NOW+","+NOW+");INSERT INTO merchant.member_grant VALUES(gen_random_uuid(),"+NEXT+","+PROOF+",'dashboard.read','ORGANIZATION',NULL);COMMIT;");
        assertEquals(1,db.flyway("34").migrate().migrationsExecuted);
        var tables=List.of("merchant.organization","merchant.membership","merchant.member_grant","merchant.team_policy","merchant.team_invitation","merchant.team_audit","merchant.pilot_route","merchant.pilot_area","orders.personal_support_ticket","orders.support_staff_grant");var before=new HashMap<String,List<String>>();for(String table:tables)before.put(table,managementSnapshot(db,table));
        var checksums=Arrays.stream(db.flyway("34").info().applied()).map(m->m.getChecksum()).toList();assertEquals(1,db.flyway("35").migrate().migrationsExecuted);db.flyway("35").validate();assertEquals(0,db.flyway("35").migrate().migrationsExecuted);
        assertEquals(checksums,Arrays.stream(db.flyway("35").info().applied()).limit(33).map(m->m.getChecksum()).toList());for(String table:tables)assertEquals(before.get(table),managementSnapshot(db,table));
        for(String permission:List.of("support.read","support.write","support.assign","support.reports.read","support.notifications.manage")){
            try(var c=db.runtimeConnection();var s=c.createStatement();var r=s.executeQuery("SELECT '"+permission+"'=ANY(merchant.team_role_ceiling('OWNER')),merchant.team_can_delegate("+PROOF+","+ACCOUNT+",'OPERATOR','"+permission+"',NULL)")){assertTrue(r.next());assertFalse(r.getBoolean(1));assertFalse(r.getBoolean(2));}
        }
        assertEquals("42501",assertThrows(SQLException.class,()->execute(db,"SELECT * FROM orders.support_operator_audit")).getSQLState());
    }

}
