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
import org.apache.pdfbox.Loader;
import java.sql.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.parallel.*;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
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
        assertEquals(48, db.flyway("latest", SCOPE).migrate().migrationsExecuted);
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
    @Test void biaOwnershipSchemaBindsCarrierEventsToCanonicalOwners() throws Exception {
        assertEquals(3,scalar("SELECT count(*) FROM information_schema.columns WHERE table_schema='orders' AND table_name='pilot_tracking_event' AND column_name IN ('owner_type','organization_id','facility_id')"));
        assertEquals(1,scalar("SELECT count(*) FROM information_schema.columns WHERE table_schema='dispatch' AND table_name='pilot_intake_acceptance' AND column_name='owner_type'"));
        assertEquals(0,scalar("SELECT count(*) FROM orders.pilot_tracking_event"));
        assertEquals(0,scalar("SELECT count(*) FROM dispatch.pilot_intake_acceptance"));
        try(var c=db.runtimeConnection();var q=c.createStatement();var rows=q.executeQuery("SELECT has_table_privilege(current_user,'orders.pilot_tracking_event','UPDATE'),has_table_privilege(current_user,'dispatch.pilot_custody','DELETE')")) {
            assertTrue(rows.next());assertFalse(rows.getBoolean(1));assertFalse(rows.getBoolean(2));
        }
    }
    @Test void nwsV44DedicatedOperationsSchemaAndAudience() throws Exception {
        try(var c=db.runtimeConnection();var q=c.prepareStatement("SELECT to_regprocedure('identity.operations_staff_eligible(uuid)')" );var rows=q.executeQuery()) {
            assertTrue(rows.next());assertNotNull(rows.getString(1),"NWS-V1 requires dedicated Operations eligibility, not SUPPORT admission");
        }
        assertEquals(0,scalar("SELECT count(*) FROM dispatch.carrier_site"));
        assertEquals(0,scalar("SELECT count(*) FROM dispatch.operational_site_assignment"));
        try(var c=db.runtimeConnection();var q=c.createStatement();var rows=q.executeQuery("SELECT has_function_privilege(current_user,'pg_catalog.pg_control_system()','EXECUTE'),has_function_privilege('elogistics_local_operations_operator','pg_catalog.pg_control_system()','EXECUTE'),has_function_privilege('dispatch_network_writer','pg_catalog.pg_control_system()','EXECUTE')")) {
            assertTrue(rows.next());assertFalse(rows.getBoolean(1));assertFalse(rows.getBoolean(2));assertTrue(rows.getBoolean(3));
        }
    }
    @Test void nwsOperationsInvitationAndAudienceIsolation() throws Exception {
        ensureStaffOperator();var operator=nwsOperator();var provisioner=LocalSupportStaffProvisioner.operations(operator,staffProtection());
        var input=new LocalSupportStaffProvisioner.OfflineStaffProvision(UUID.randomUUID(),"Synthetic Operations Staff","operations@example.invalid","NWS isolated verification");
        var receipt=provisioner.provision(input);assertEquals(receipt.accountId(),provisioner.provision(input).accountId());
        var source=outboxDataSource();var worker=JdbcInvitationOutbox.operations(source,staffProtection(),outboxPolicy(),Clock.systemUTC());
        assertTrue(new JdbcInvitationOutbox(source,staffProtection(),outboxPolicy(),Clock.systemUTC(),ActionProof.Purpose.STAFF_INV).claim().isEmpty());
        var lease=worker.claim().orElseThrow();byte[] token;try(var delivery=worker.open(lease)){token=delivery.token().clone();}worker.complete(lease,JdbcInvitationOutbox.Outcome.SENT);
        try {
            assertThrows(IdentityCommandFailure.class,()->new JdbcStaffInvitationActivation(source,staffProtection(),Clock.systemUTC()).accept(receipt.proofId(),token,SESSION_PASSWORD,UUID.randomUUID(),UUID.randomUUID()));
            UUID acceptanceKey=UUID.randomUUID(),request=UUID.randomUUID();
            var activation=new JdbcStaffInvitationActivation(source,staffProtection(),Clock.systemUTC(),true);
            assertEquals(List.of("ACTIVATED","ACTIVATED"),adminRace(()->{activation.accept(receipt.proofId(),token,SESSION_PASSWORD,acceptanceKey,request);return "ACTIVATED";},()->{activation.accept(receipt.proofId(),token,SESSION_PASSWORD,acceptanceKey,request);return "ACTIVATED";}));
        } finally {Arrays.fill(token,(byte)0);}
        var jdbc=new org.springframework.jdbc.core.JdbcTemplate(source);
        assertEquals(Boolean.TRUE,jdbc.queryForObject("SELECT identity.operations_staff_eligible(?)",Boolean.class,receipt.accountId()));
        assertEquals(Boolean.FALSE,jdbc.queryForObject("SELECT identity.support_staff_eligible(?)",Boolean.class,receipt.accountId()));
        var sessions=com.elogistic.backend.identity.internal.application.IdentitySessions.staff(new JdbcIdentitySessionStore(source,IdentitySessionStorage.OPS),new ProtectedSessionCredentials(),(operation,request,session,present)->{},Clock.systemUTC());
        try(var login=sessions.loginIdentity(new com.elogistic.backend.identity.api.IdentitySessionCommands.Login(input.email(),SESSION_PASSWORD,UUID.randomUUID()))) {
            assertEquals(receipt.accountId(),login.access().accountId());
            assertEquals("OPS",jdbc.queryForObject("SELECT audience FROM identity.auth_session WHERE id=?",String.class,login.access().sessionId()));
            UUID site=nwsSite(operator,"SIM-DROP-A1","STATION","SIM-A1",UUID.randomUUID()),hub=nwsSite(operator,"SIM-HUB-A","HUB","SIM-A1",UUID.randomUUID());nwsLink(operator,site,hub);nwsAssignment(operator,receipt.accountId(),site,"SITE_READ",false);nwsAssignment(operator,receipt.accountId(),site,"STATION_INTAKE",false);
            var access=login.access();var admission=new JdbcOperationalStaffAdmission(source,t->new com.elogistic.backend.identity.internal.application.port.CustomerAccessVerifier.Claim(access.sessionId(),access.accountId(),access.authVersion(),access.issuedAt(),access.expiresAt()),(request,session)->{},Clock.systemUTC());
            var escaped=admission.execute("synthetic-verified-ops-claim",UUID.randomUUID(),c->{
                admission.requireCurrent(c);
                new com.elogistic.backend.dispatch.internal.infrastructure.JdbcStationIntake(source,Clock.systemUTC(),true,"TEST",admission).require(c,site);
                assertThrows(com.elogistic.backend.orders.api.PilotHandlingFailure.class,()->new com.elogistic.backend.dispatch.internal.infrastructure.JdbcStationIntake(source,Clock.offset(Clock.systemUTC(),Duration.ofHours(2)),true,"TEST",admission).require(c,site));
                assertThrows(IdentityCommandFailure.class,()->admission.requireCurrent(new com.elogistic.backend.identity.api.staff.OperationalStaffAdmission.Context(c.requestId(),c.sessionId(),c.actor())));
                var suspended=new org.springframework.transaction.support.TransactionTemplate(new org.springframework.jdbc.support.JdbcTransactionManager(source));suspended.setPropagationBehavior(org.springframework.transaction.TransactionDefinition.PROPAGATION_REQUIRES_NEW);
                assertThrows(IdentityCommandFailure.class,()->suspended.execute(status->{admission.requireCurrent(c);return null;}));admission.requireCurrent(c);
                var foreign=new org.springframework.jdbc.datasource.DriverManagerDataSource(db.url(),db.runtime(),db.runtimePassword());
                var wrongSource=new com.elogistic.backend.dispatch.internal.infrastructure.JdbcStationIntake(foreign,Clock.systemUTC(),true,"TEST",admission);
                assertThrows(com.elogistic.backend.orders.api.PilotHandlingFailure.class,()->wrongSource.require(c,UUID.randomUUID()));return c;});
            assertThrows(IdentityCommandFailure.class,()->admission.requireCurrent(escaped));
            assertEquals(0,scalar("SELECT count(*) FROM identity.account_grant WHERE account_id='"+receipt.accountId()+"'"));
            var expired=new JdbcOperationalStaffAdmission(source,t->new com.elogistic.backend.identity.internal.application.port.CustomerAccessVerifier.Claim(access.sessionId(),access.accountId(),access.authVersion(),access.issuedAt(),access.expiresAt()),(request,session)->{},Clock.offset(Clock.systemUTC(),Duration.ofMinutes(16)));
            assertThrows(IdentityCommandFailure.class,()->expired.execute("synthetic-verified-ops-claim",UUID.randomUUID(),c->c.actor()));
            jdbc.update("UPDATE identity.auth_session SET revoked_at=clock_timestamp() WHERE id=?",access.sessionId());
            assertThrows(IdentityCommandFailure.class,()->admission.execute("synthetic-verified-ops-claim",UUID.randomUUID(),c->c.actor()));
        }
    }
    private org.springframework.jdbc.datasource.DriverManagerDataSource nwsOperator() throws Exception {
        String name="nws_test_"+UUID.randomUUID().toString().replace("-",""),password=PostgresTestcontainersSupport.password();
        try(var c=DriverManager.getConnection(PostgresTestcontainersSupport.currentUrl(POSTGRES,"postgres"),POSTGRES.getUsername(),POSTGRES.getPassword());var q=c.createStatement()) {
            q.execute("CREATE ROLE "+name+" LOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE PASSWORD '"+password+"'");
            q.execute("GRANT elogistics_local_operations_operator TO "+name);q.execute("GRANT CONNECT ON DATABASE "+db.name()+" TO "+name);
        }
        return new org.springframework.jdbc.datasource.DriverManagerDataSource(db.url(),name,password);
    }
    private boolean nwsHttpEnabled;
    private boolean biaHttpEnabled;
    private UUID nwsStaff(org.springframework.jdbc.datasource.DriverManagerDataSource operator) throws Exception {
        var receipt=LocalSupportStaffProvisioner.operations(operator,staffProtection()).provision(new LocalSupportStaffProvisioner.OfflineStaffProvision(UUID.randomUUID(),"Synthetic Station Operator","operations@example.invalid","NWS isolated intake"));
        var source=outboxDataSource();var worker=JdbcInvitationOutbox.operations(source,staffProtection(),outboxPolicy(),Clock.systemUTC());var lease=worker.claim().orElseThrow();byte[] token;
        try(var d=worker.open(lease)){token=d.token().clone();}worker.complete(lease,JdbcInvitationOutbox.Outcome.SENT);
        try{new JdbcStaffInvitationActivation(source,staffProtection(),Clock.systemUTC(),true).accept(receipt.proofId(),token,SESSION_PASSWORD,UUID.randomUUID(),UUID.randomUUID());}finally{Arrays.fill(token,(byte)0);}return receipt.accountId();
    }
    private UUID nwsSite(javax.sql.DataSource operator,String code,String type,String area,UUID key) throws Exception {
        Map<String,Object> input=nwsSiteConfiguration(code,type,area);
        try(var c=operator.getConnection();var q=c.prepareStatement("SELECT * FROM dispatch.configure_local_carrier_site(?,NULL,0,?::jsonb,?)")){q.setObject(1,key);q.setString(2,new tools.jackson.databind.ObjectMapper().writeValueAsString(input));q.setString(3,"NWS synthetic site");try(var r=q.executeQuery()){assertTrue(r.next());return r.getObject(1,UUID.class);}}
    }
    private Map<String,Object> nwsSiteConfiguration(String code,String type,String area){
        Map<String,Object> input=new HashMap<>();input.put("code",code);input.put("type",type);input.put("name","Synthetic "+type);input.put("locationText","Isolated fixture location, not a real address");input.put("areaCode",area);input.put("latitude",null);input.put("longitude",null);input.put("timezone","Asia/Ho_Chi_Minh");input.put("operatingHours",Map.of());input.put("state","ACTIVE");input.put("capabilities",type.equals("STATION")?List.of("SITE_READ","STATION_INTAKE"):List.of("SITE_READ"));input.put("effectiveFrom",null);input.put("effectiveTo",null);input.put("environment","TEST");input.put("provenance","SIMULATION");
        return input;
    }
    private void nwsSiteState(javax.sql.DataSource operator,UUID site,long expected,String state)throws Exception {
        var config=nwsSiteConfiguration("SIM-DROP-A1","STATION","SIM-A1");config.put("state",state);
        try(var c=operator.getConnection();var q=c.prepareStatement("SELECT * FROM dispatch.configure_local_carrier_site(?,?,?,?,?)")){
            q.setObject(1,UUID.randomUUID());q.setObject(2,site);q.setLong(3,expected);q.setObject(4,new tools.jackson.databind.ObjectMapper().writeValueAsString(config),java.sql.Types.OTHER);q.setString(5,"NWS synthetic versioned state");try(var r=q.executeQuery()){assertTrue(r.next());assertEquals(expected+1,r.getLong(2));}
        }
    }
    private void nwsLink(javax.sql.DataSource operator,UUID station,UUID hub) throws Exception {
        Map<String,Object> v=new HashMap<>(Map.of("fromSiteId",station,"toSiteId",hub,"serviceCode","ROAD_STANDARD","datasetVersion","ARGO-SIM-COVERAGE-1","areaCode","SIM-A1","dropOffCode","SIM-DROP-A1"));v.put("effectiveFrom",null);v.put("effectiveTo",null);
        try(var c=operator.getConnection();var q=c.prepareStatement("SELECT * FROM dispatch.configure_local_site_link(?,NULL,0,?::jsonb,?)")){q.setObject(1,UUID.randomUUID());q.setString(2,new tools.jackson.databind.ObjectMapper().writeValueAsString(v));q.setString(3,"NWS synthetic topology");try(var r=q.executeQuery()){assertTrue(r.next());}}
    }
    private void nwsAssignment(javax.sql.DataSource operator,UUID actor,UUID site,String permission,boolean revoke) throws Exception {
        Instant from=Instant.now(),to=from.plusSeconds(3600);long version=0;
        if(revoke)try(var c=db.runtimeConnection();var q=c.prepareStatement("SELECT effective_from,effective_to,version FROM dispatch.operational_site_assignment WHERE account_id=? AND site_id=? AND permission=? AND state='ACTIVE'")){q.setObject(1,actor);q.setObject(2,site);q.setString(3,permission);try(var r=q.executeQuery()){assertTrue(r.next());from=r.getTimestamp(1).toInstant();to=r.getTimestamp(2).toInstant();version=r.getLong(3);}}
        try(var c=operator.getConnection();var q=c.prepareStatement("SELECT * FROM dispatch.configure_local_operational_assignment(?,?,?,?,?,?,?,?,?)")){q.setObject(1,UUID.randomUUID());q.setObject(2,actor);q.setObject(3,site);q.setString(4,permission);q.setString(5,revoke?"REVOKE":"GRANT");q.setTimestamp(6,Timestamp.from(from));q.setTimestamp(7,Timestamp.from(to));q.setLong(8,version);q.setString(9,"NWS synthetic assignment");try(var r=q.executeQuery()){assertTrue(r.next());}}
    }
    private ProfileBrowser nwsBrowser(org.springframework.test.web.servlet.MockMvc mvc)throws Exception {
        var csrf=mvc.perform(MockMvcRequestBuilders.get("/api/v1/ops-auth/csrf").secure(true).header("Origin","https://portal.example.invalid")).andReturn().getResponse();assertEquals(200,csrf.getStatus());
        var json=new tools.jackson.databind.ObjectMapper();String challenge=json.readTree(csrf.getContentAsString()).get("csrfToken").asString();
        var login=mvc.perform(MockMvcRequestBuilders.post("/api/v1/ops-auth/login").secure(true).header("Origin","https://portal.example.invalid").header("X-CSRF-Token",challenge).cookie(responseCookie(csrf,"__Host-argopost.ops.csrf")).contentType("application/json").content(json.writeValueAsString(Map.of("email","operations@example.invalid","password",SESSION_PASSWORD)))).andReturn().getResponse();assertEquals(200,login.getStatus(),this::safeOnboardingFailure);
        var cookie=responseCookie(login,"__Host-argopost.ops.csrf");var refresh=responseCookie(login,"__Host-argopost.ops.refresh");
        var rebound=mvc.perform(MockMvcRequestBuilders.get("/api/v1/ops-auth/csrf").secure(true).header("Origin","https://portal.example.invalid").cookie(cookie,refresh)).andReturn().getResponse();assertEquals(200,rebound.getStatus());return new ProfileBrowser(json.readTree(login.getContentAsString()).get("accessToken").asString(),new BrowserFlow(responseCookie(rebound,"__Host-argopost.ops.csrf"),refresh,json.readTree(rebound.getContentAsString()).get("csrfToken").asString()));
    }
    @Test void nwsNetworkOperatorReceiptAndRuntimeAclAreBound()throws Exception {
        ensureStaffOperator();var op=nwsOperator();UUID key=UUID.randomUUID();var race=adminRace(()->nwsSite(op,"SIM-DROP-A1","STATION","SIM-A1",key),()->nwsSite(op,"SIM-DROP-A1","STATION","SIM-A1",key));assertEquals(race.getFirst(),race.get(1));UUID site=(UUID)race.getFirst();assertEquals(site,nwsSite(op,"SIM-DROP-A1","STATION","SIM-A1",key));assertEquals(1,scalar("SELECT count(*) FROM dispatch.carrier_site"));
        try(var c=db.migrationConnection();var q=c.createStatement()){q.execute("SET ROLE dispatch_network_writer");try(var r=q.executeQuery("SELECT count(*) FROM dispatch.network_operator_receipt r JOIN dispatch.network_operator_audit a ON a.id=r.audit_id WHERE a.target_id=r.target_id AND a.request_id=r.request_id AND r.system_identifier=(SELECT system_identifier FROM pg_control_system()) AND r.database_name=current_database() AND r.database_oid=(SELECT oid FROM pg_database WHERE datname=current_database())")){assertTrue(r.next());assertEquals(1,r.getInt(1));}}
        assertThrows(SQLException.class,()->execute("UPDATE dispatch.carrier_site_version SET state='INACTIVE'"));assertThrows(SQLException.class,()->execute("INSERT INTO dispatch.carrier_site VALUES(gen_random_uuid(),'UNAUTHORIZED','STATION',clock_timestamp())"));
        assertThrows(SQLException.class,()->nwsSite(op,"SIM-DROP-A2","STATION","SIM-A2",key));assertEquals(1,scalar("SELECT count(*) FROM dispatch.carrier_site"));
        var source=outboxDataSource();UUID hub=nwsSite(op,"SIM-HUB-A","HUB","SIM-A1",UUID.randomUUID());nwsLink(op,site,hub);
        var reader=new com.elogistic.backend.dispatch.internal.infrastructure.JdbcCarrierNetworkReads(source,Clock.systemUTC());
        new org.springframework.transaction.support.TransactionTemplate(new org.springframework.jdbc.support.JdbcTransactionManager(source)).execute(status->{
            com.elogistic.backend.dispatch.api.CarrierNetworkReads.Access allowed=()->assertTrue(org.springframework.transaction.support.TransactionSynchronizationManager.hasResource(source));
            var page=reader.sites(allowed,new com.elogistic.backend.dispatch.api.CarrierNetworkReads.SiteQuery("ACTIVE","STATION","SIM-A1",0,20));assertEquals(1,page.total());assertEquals(site,page.items().getFirst().id());var detail=reader.site(allowed,site);assertEquals("SIMULATION",detail.provenance());assertTrue(detail.operatingHours().isEmpty());assertEquals(hub,detail.outgoing().getFirst().toSiteId());return null;
        });
        try(var c=DriverManager.getConnection(PostgresTestcontainersSupport.currentUrl(POSTGRES,"postgres"),POSTGRES.getUsername(),POSTGRES.getPassword());var q=c.createStatement()){q.execute("REVOKE elogistics_local_operations_operator FROM "+op.getUsername());}
        assertEquals("42501",assertThrows(SQLException.class,()->nwsSite(op,"SIM-DROP-A1","STATION","SIM-A1",key)).getSQLState());assertEquals(2,scalar("SELECT count(*) FROM dispatch.carrier_site"));

    }
    @Test void nwsOperationsOutboxFailureRetryAndCancellationArePurposeBound()throws Exception {
        ensureStaffOperator();var operator=nwsOperator();var provisioner=LocalSupportStaffProvisioner.operations(operator,staffProtection());var source=outboxDataSource();
        var receipt=provisioner.provision(new LocalSupportStaffProvisioner.OfflineStaffProvision(UUID.randomUUID(),"Synthetic Ops Retry","ops-retry@example.invalid","NWS transport failure recovery"));
        Instant base=Instant.now().plusSeconds(1);var initial=JdbcInvitationOutbox.operations(source,staffProtection(),outboxPolicy(),Clock.fixed(base,ZoneOffset.UTC));
        try(var smtp=new CustomerSmtpTestInbox(1,false,false,450,null)){
            assertTrue(new StaffInvitationDeliveryWorker(initial,smtp(smtp,base),()->true,true).runOne());
            assertEquals(1,scalar("SELECT count(*) FROM identity.invitation_outbox WHERE account_id='"+receipt.accountId()+"' AND status='PENDING' AND last_outcome='TRANSIENT_FAILURE'"));
        }
        var retry=JdbcInvitationOutbox.operations(source,staffProtection(),outboxPolicy(),Clock.fixed(base.plusSeconds(2),ZoneOffset.UTC));
        try(var smtp=new CustomerSmtpTestInbox(1,false,false,250,null)){
            assertTrue(new StaffInvitationDeliveryWorker(retry,smtp(smtp,base.plusSeconds(2)),()->true,true).runOne());var mail=new jakarta.mail.internet.MimeMessage(jakarta.mail.Session.getInstance(new Properties()),new java.io.ByteArrayInputStream(smtp.messages().getFirst()));String content=CustomerSmtpTestInbox.plainText(mail);assertTrue(content.contains("/ops/activate#proofId="));assertFalse(content.contains("/staff/activate"));assertEquals("ops-retry@example.invalid",mail.getAllRecipients()[0].toString());assertFalse(new StaffInvitationDeliveryWorker(retry,smtp(smtp,base.plusSeconds(2)),()->true,true).runOne());
        }
        assertEquals(1,scalar("SELECT count(*) FROM identity.invitation_outbox WHERE account_id='"+receipt.accountId()+"' AND status='SENT' AND attempts=2 AND nonce IS NULL AND ciphertext IS NULL"));assertEquals(1,scalar("SELECT count(*) FROM identity.local_staff_principal WHERE account_id='"+receipt.accountId()+"' AND state='PENDING'"));
        var pending=provisioner.provision(new LocalSupportStaffProvisioner.OfflineStaffProvision(UUID.randomUUID(),"Synthetic Ops Cancel","ops-cancel@example.invalid","NWS cancellation"));var outbox=JdbcInvitationOutbox.operations(source,staffProtection(),outboxPolicy(),Clock.systemUTC());var lease=outbox.claim().orElseThrow();provisioner.disposition(new LocalSupportStaffProvisioner.OfflineStaffInvitationDisposition(UUID.randomUUID(),pending.accountId(),"CANCEL","NWS cancel before dispatch"));assertThrows(IllegalStateException.class,()->outbox.open(lease));assertEquals(0,scalar("SELECT count(*) FROM dispatch.operational_site_assignment"));
    }
    @Test void nwsScopedIntakeReusesAtomicCustodyAndCurrentAuthority()throws Exception {
        customerFixture(true);ensureStaffOperator();var op=nwsOperator();UUID actor=nwsStaff(op),site=nwsSite(op,"SIM-DROP-A1","STATION","SIM-A1",UUID.randomUUID()),hub=nwsSite(op,"SIM-HUB-A","HUB","SIM-A1",UUID.randomUUID());nwsLink(op,site,hub);nwsAssignment(op,actor,site,"SITE_READ",false);nwsAssignment(op,actor,site,"STATION_INTAKE",false);
        nwsHttpEnabled=true;liveStaffClock=true;Instant current=Instant.now();
        try(var inbox=new CustomerSmtpTestInbox(0,false,false,250,null);var c=customerHttpContext(current,smtp(inbox,current),true)) {
            var mvc=httpMvc(c);var owner=profileBrowser(mvc);var staff=nwsBrowser(mvc);String shipment=intakeBooked(mvc,owner);var codes=intakeBarcodes(mvc,owner,shipment);String base="/api/v1/ops/sites/"+site;
            assertEquals(401,httpRead(mvc,base+"/shipments/"+shipment,owner.access()).getStatus());assertEquals(401,httpRead(mvc,"/api/v1/staff-auth/me",staff.access()).getStatus());assertEquals(403,httpRead(mvc,"/api/v1/ops/sites/"+hub+"/shipments/"+shipment,staff.access()).getStatus());
            List<String> scans=new ArrayList<>();for(String code:codes){var r=onboardingWrite(mvc,staff,"POST",base+"/scans",UUID.randomUUID(),null,Map.of("barcode",code));assertEquals(200,r.getStatus(),this::safeOnboardingFailure);scans.add(responseId(r,"id"));}
            UUID key=UUID.randomUUID();Map<String,Object> input=Map.of("scanIds",scans,"inspection","MATCHED");var accepted=onboardingWrite(mvc,staff,"POST",base+"/shipments/"+shipment+"/acceptance",key,"\"i0\"",input);assertEquals(200,accepted.getStatus(),this::safeOnboardingFailure);assertEquals(200,onboardingWrite(mvc,staff,"POST",base+"/shipments/"+shipment+"/acceptance",key,"\"i0\"",input).getStatus());
            assertEquals(1,scalar("SELECT count(*) FROM dispatch.pilot_intake_acceptance WHERE assignment_id IS NULL AND operational_assignment_id IS NOT NULL AND site_version_id IS NOT NULL"));assertEquals(2,scalar("SELECT count(*) FROM dispatch.pilot_custody"));assertEquals(2,scalar("SELECT count(*) FROM orders.pilot_parcel_event"));assertEquals(1,scalar("SELECT count(*) FROM orders.pilot_tracking_event"));assertEquals(1,scalar("SELECT count(*) FROM finance.pilot_shipping_obligation WHERE state='UNPAID'"));
            assertEquals(200,httpRead(mvc,"/api/v1/customer/shipments/"+shipment+"/handling",owner.access()).getStatus());
            nwsSiteState(op,site,1,"INACTIVE");assertEquals(403,httpRead(mvc,base+"/shipments/"+shipment,staff.access()).getStatus());nwsSiteState(op,site,2,"ACTIVE");assertEquals(200,httpRead(mvc,base+"/shipments/"+shipment,staff.access()).getStatus());
            String second=intakeBooked(mvc,owner);List<String> secondScans=new ArrayList<>();for(String code:intakeBarcodes(mvc,owner,second)){var r=onboardingWrite(mvc,staff,"POST",base+"/scans",UUID.randomUUID(),null,Map.of("barcode",code));assertEquals(200,r.getStatus());secondScans.add(responseId(r,"id"));}
            db.ownerExecute("CREATE FUNCTION dispatch.nws_intake_fault() RETURNS trigger LANGUAGE plpgsql AS $$ BEGIN RAISE EXCEPTION 'NWS_SYNTHETIC_LATE_FAILURE'; END $$");db.ownerExecute("CREATE TRIGGER nws_late_fault BEFORE INSERT ON dispatch.pilot_custody FOR EACH ROW EXECUTE FUNCTION dispatch.nws_intake_fault()");
            Map<String,Object> secondInput=Map.of("scanIds",secondScans,"inspection","MATCHED");assertEquals(503,onboardingWrite(mvc,staff,"POST",base+"/shipments/"+second+"/acceptance",UUID.randomUUID(),"\"i0\"",secondInput).getStatus());assertEquals(1,scalar("SELECT count(*) FROM dispatch.pilot_intake_acceptance"));assertEquals(2,scalar("SELECT count(*) FROM dispatch.pilot_custody"));assertEquals(2,scalar("SELECT count(*) FROM orders.pilot_parcel_event"));
            db.ownerExecute("DROP TRIGGER nws_late_fault ON dispatch.pilot_custody");db.ownerExecute("DROP FUNCTION dispatch.nws_intake_fault()");
            UUID concurrent=UUID.randomUUID();assertEquals(List.of(200,200),adminRace(()->onboardingWrite(mvc,staff,"POST",base+"/shipments/"+second+"/acceptance",concurrent,"\"i0\"",secondInput).getStatus(),()->onboardingWrite(mvc,staff,"POST",base+"/shipments/"+second+"/acceptance",concurrent,"\"i0\"",secondInput).getStatus()));assertEquals(2,scalar("SELECT count(*) FROM dispatch.pilot_intake_acceptance"));assertEquals(4,scalar("SELECT count(*) FROM dispatch.pilot_custody"));
            String third=intakeBooked(mvc,owner);List<String> thirdScans=new ArrayList<>();for(String code:intakeBarcodes(mvc,owner,third)){var r=onboardingWrite(mvc,staff,"POST",base+"/scans",UUID.randomUUID(),null,Map.of("barcode",code));assertEquals(200,r.getStatus());thirdScans.add(responseId(r,"id"));}
            var race=adminRace(()->onboardingWrite(mvc,staff,"POST",base+"/shipments/"+third+"/acceptance",UUID.randomUUID(),"\"i0\"",Map.of("scanIds",thirdScans,"inspection","MATCHED")).getStatus(),()->{nwsAssignment(op,actor,site,"STATION_INTAKE",true);return 204;});assertTrue(Set.of(200,403).contains(race.getFirst()));assertEquals(204,race.get(1));long count=Integer.valueOf(200).equals(race.getFirst())?3:2;assertEquals(count,scalar("SELECT count(*) FROM dispatch.pilot_intake_acceptance"));assertEquals(count*2,scalar("SELECT count(*) FROM dispatch.pilot_custody"));
            assertEquals(403,httpRead(mvc,base+"/shipments/"+shipment,staff.access()).getStatus());assertEquals(403,httpRead(mvc,base+"/receipts/"+key,staff.access()).getStatus());assertEquals(200,httpRead(mvc,"/api/v1/ops-auth/me",staff.access()).getStatus());
        }
    }
    @Test void nwsRealHttpsBrowserAndPostgres()throws Exception {
        customerFixture(true);ensureStaffOperator();var operator=nwsOperator();UUID site=nwsSite(operator,"SIM-DROP-A1","STATION","SIM-A1",UUID.randomUUID()),hub=nwsSite(operator,"SIM-HUB-A","HUB","SIM-A1",UUID.randomUUID());nwsLink(operator,site,hub);
        S003CustomerBrowserRuntime.runOperations(outboxDataSource(),operator,site);
    }
    // ADM-02–08 uses this existing owned lifecycle. No new suite alias or ownership/cleanup protocol change.
    private com.elogistic.backend.workflow.internal.web.AdminDashboardPostgresCases adminDashboardCases() throws Exception {
        return new com.elogistic.backend.workflow.internal.web.AdminDashboardPostgresCases(db,SCOPE);
    }
    @Test void nwsAdminCarrierDirectoryIsPermissionBound()throws Exception{try(var cases=adminDashboardCases()){cases.nwsCarrierDirectoryRequiresCurrentNetworkPermission();}}
    @Test void adminDashboardTwoConnectionPoolAndUnavailableDatabase()throws Exception{try(var cases=adminDashboardCases()){cases.twoConnectionPoolExhaustionFailsClosedAndRecovers();}}
    @Test void adminDashboardInvitationHttpSecurityAndRealLogin()throws Exception{try(var cases=adminDashboardCases()){cases.invitationHttpUsesSystemCsrfAndRealProofBeforeLogin();}}
    @Test void adminDashboardSixApisAndSystemAdmission() throws Exception {try(var cases=adminDashboardCases()){cases.sixApisHaveRealCurrentSystemAdmissionAndNoImplicitRootAuthority();}}
    @Test void adminDashboardCanonicalCohortAndPii() throws Exception {try(var cases=adminDashboardCases()){cases.canonicalBookingsParcelsAcceptanceFeesAndDrillthroughReconcileWithoutPii();}}
    @Test void adminDashboardGranularPermissionIsolation() throws Exception {try(var cases=adminDashboardCases()){cases.granularPermissionsDenyBeforeFetchingAndCannotLeakFinancialOrTechnicalData();}}
    @Test void adminDashboardDefaultOffAndRevocation() throws Exception {try(var cases=adminDashboardCases()){cases.defaultOffProductionCustomerMixedAndRevokedPermissionsFailClosed();}}
    @Test void adminDashboardSnapshotTamperReplayAndDrift() throws Exception {try(var cases=adminDashboardCases()){cases.queryTamperActorReplayAndLateCohortChangeDoNotReturnMismatchedCounts();}}
    @Test void adminDashboardReadOnlyAndScopeLifetime() throws Exception {try(var cases=adminDashboardCases()){cases.statelessReadContextCannotEscapeAndPostgresReadOnlyCannotWrite();}}
    @Test void adminDashboardRealHttpsBrowserAndPostgres() throws Exception {try(var cases=adminDashboardCases()){cases.realHttpsBrowserUsesSourceAndPostgresWithoutFixtureInterception();}}
    @Test void adminDashboardConcurrentAuthoritySnapshot() throws Exception {try(var cases=adminDashboardCases()){cases.concurrentAuthorityChangeCannotUseAPreLockSnapshot();}}
    @Test void adminDashboardPartialRevocationAndAudience() throws Exception {try(var cases=adminDashboardCases()){cases.partialRevocationInvalidatesSnapshotsAndOtherAudiencesAreRejected();}}
    @Test void adminDashboardBusinessAndPersonalCohort() throws Exception {try(var cases=adminDashboardCases()){cases.businessAndPersonalBookingsShareFactsWithoutMerchantMembershipSubstitution();}}
    @Test void adminGovernanceCommandAdmissionAndRollback()throws Exception{try(var cases=adminDashboardCases()){cases.governanceCommandAdmissionOwnsTransactionAndRollsBack();}}
    @Test void adminGovernanceCommandPurposeAndLifetime()throws Exception{try(var cases=adminDashboardCases()){cases.governanceCommandAdmissionRejectsWrongPurposeAndEscapedContexts();}}
    @Test void adminGovernanceCommandRevocationAndExpiry()throws Exception{try(var cases=adminDashboardCases()){cases.governanceCommandAdmissionRejectsRevocationAndExpiryAtCommit();}}
    @Test void adminGovernanceCommandConcurrentRevoke()throws Exception{try(var cases=adminDashboardCases()){cases.governanceCommandWaitingForRevocationCannotUseOldAuthority();}}
    @Test void adminGovernanceV41SchemaAndAcl()throws Exception{try(var cases=adminDashboardCases()){cases.governanceV41SchemaAndAclAreForwardOnly();}}
    @Test void adminGovernanceStepUpReplayAndConsume()throws Exception{try(var cases=adminDashboardCases()){cases.governanceStepUpUsesCredentialAndDurableReplay();}}
    @Test void adminGovernanceStepUpLimitsAndBinding()throws Exception{try(var cases=adminDashboardCases()){cases.governanceStepUpLimitsPersistAndProofBindingCannotChange();}}
    @Test void adminGovernanceDirectDmlDenials()throws Exception{try(var cases=adminDashboardCases()){cases.governanceProofDirectDmlCannotMutateIdentityOrOmitAudit();}}
    @Test void adminShipmentOversightSnapshotAndPrivacy()throws Exception{try(var cases=adminDashboardCases()){cases.maskedOversightUsesExactContributingSnapshotAndRealParcelEvents();}}
    @Test void businessContactSmtpClearingAndAtomicConsume()throws Exception{try(var cases=adminDashboardCases()){cases.businessContactSmtpClearingAndAtomicConsume();}}
    @Test void businessContactContextReplayAndLimits()throws Exception{try(var cases=adminDashboardCases()){cases.businessContactContextReplayAndLimits();}}
    @Test void businessContactWrongBindingExpiryAndTamper()throws Exception{try(var cases=adminDashboardCases()){cases.businessContactWrongBindingExpiryAndTamper();}}
    @Test void businessContactSuspendedTransactionAndRevocation()throws Exception{try(var cases=adminDashboardCases()){cases.businessContactSuspendedTransactionAndRevocation();}}
    @Test void adminGovernanceStepUpConstructedContextDenial()throws Exception{try(var cases=adminDashboardCases()){cases.governanceStepUpRejectsConstructedContext();}}
    @Test void businessContactConcurrentConfirmationAndReceiptBinding()throws Exception{try(var cases=adminDashboardCases()){cases.businessContactConcurrentConfirmationAndReceiptBinding();}}
    @Test void businessContactWrongSubjectAndSession()throws Exception{try(var cases=adminDashboardCases()){cases.businessContactWrongSubjectAndSession();}}
    @Test void businessContactActualCustomerHttpAndOwnership()throws Exception{try(var cases=adminDashboardCases()){cases.businessContactActualCustomerHttpAndOwnership();}}
    @Test void organizationLifecycleV47PreparationSchema()throws Exception{try(var cases=adminDashboardCases()){cases.organizationLifecycleV47PreparationSchema();}}
    @Test void organizationLifecycleV47PreparationHttps()throws Exception{try(var cases=adminDashboardCases()){cases.organizationLifecycleV47PreparationHttps();}}
    @Test void merchantR01SearchCommentsAndScope()throws Exception{try(var cases=adminDashboardCases()){cases.merchantR01SearchCommentsAndScope();}}
    @Test void merchantR01ComparisonAndHttps()throws Exception{try(var cases=adminDashboardCases()){cases.merchantR01ComparisonAndHttps();}}
    @Test void merchantR01ProfileAndMembers()throws Exception{try(var cases=adminDashboardCases()){cases.merchantR01ProfileAndMembers();}}
    @Test void merchantGovernanceCurrentScopeAndRevocation()throws Exception{try(var cases=adminDashboardCases()){cases.merchantGovernanceCurrentScopeAndRevocation();}}
    @Test void merchantWorkflowV42DraftCompatibilityAndDenial()throws Exception{try(var cases=adminDashboardCases()){cases.merchantWorkflowV42DraftCompatibilityAndDenial();}}
    @Test void merchantV42ScreenedEvidenceAndImmutableSubmission()throws Exception{try(var cases=adminDashboardCases()){cases.merchantV42ScreenedEvidenceAndImmutableSubmission();}}
    @Test void merchantV42HttpEvidenceAndSubmissionNegatives()throws Exception{try(var cases=adminDashboardCases()){cases.merchantV42HttpEvidenceAndSubmissionNegatives();}}
    @Test void merchantV42IndependentAssignmentReviewAndInformationRequest()throws Exception{try(var cases=adminDashboardCases()){cases.merchantV42IndependentAssignmentReviewAndInformationRequest();}}
    @Test void governanceIdentityOwnedContextsBindResourceAndLifetime()throws Exception{try(var cases=adminDashboardCases()){cases.governanceIdentityOwnedContextsBindResourceAndLifetime();}}
    @Test void submittedBusinessContactFactHistoryAndAuthority()throws Exception{try(var cases=adminDashboardCases()){cases.submittedBusinessContactFactHistoryAndAuthority();}}
    @Test void submittedBusinessContactFactSecurityVersion()throws Exception{try(var cases=adminDashboardCases()){cases.submittedBusinessContactFactSecurityVersion();}}
    @Test void merchantV42RealHttpsIndependentRejection()throws Exception{try(var cases=adminDashboardCases()){cases.merchantV42RealHttpsIndependentRejection();}}
    @Test void merchantV43AtomicApprovalAndBoundNotices()throws Exception{try(var cases=adminDashboardCases()){cases.merchantV43AtomicApprovalAndBoundNotices();}}
    @Test void merchantV43RealHttpsIndependentApproval()throws Exception{try(var cases=adminDashboardCases()){cases.merchantV43RealHttpsIndependentApproval();}}
    @Test void merchantV43LegacyKeyClearanceFailsClosed()throws Exception{try(var cases=adminDashboardCases()){cases.merchantV43LegacyKeyClearanceFailsClosed();}}
    @Test void merchantV43OppositeDecisionsCommitOnlyOneOutcome()throws Exception{try(var cases=adminDashboardCases()){cases.merchantV43OppositeDecisionsCommitOnlyOneOutcome();}}
    @Test void merchantV43ActivatedBusinessKeyCannotBeReused()throws Exception{try(var cases=adminDashboardCases()){cases.merchantV43ActivatedBusinessKeyCannotBeReused();}}
    @Test void merchantV43RejectionCommitsOneBoundNotice()throws Exception{try(var cases=adminDashboardCases()){cases.merchantV43RejectionCommitsOneBoundNotice();}}
    @Test void merchantV43ScopedAuditProjectsMetadataOnly()throws Exception{try(var cases=adminDashboardCases()){cases.merchantV43ScopedAuditProjectsMetadataOnly();}}
    @Test void merchantV43ApplicationFactsVerifyIdentityAndCanonicalCohort()throws Exception{try(var cases=adminDashboardCases()){cases.merchantV43ApplicationFactsVerifyIdentityAndCanonicalCohort();}}
    @Test void merchantV43DashboardV2CanonicalSnapshotAndCompatibility()throws Exception{try(var cases=adminDashboardCases()){cases.merchantV43DashboardV2CanonicalSnapshotAndCompatibility();}}
    @Test void adminCoreRealHttpsCustomerToIndependentActivation()throws Exception{try(var cases=adminDashboardCases()){cases.adminCoreRealHttpsCustomerToIndependentActivation();}}
    @Test void biaRealHttpsCustomerApplicationToBusinessStation()throws Exception{try(var cases=adminDashboardCases()){cases.biaRealHttpsCustomerApplicationToBusinessStation();}}
    @Test void merchantWorkAndOrganizationAuditCurrentAuthority()throws Exception{try(var cases=adminDashboardCases()){cases.merchantWorkAndOrganizationAuditCurrentAuthority();}}
    @Test void merchantV43SchemaAndWriterAcl()throws Exception{try(var cases=adminDashboardCases()){cases.merchantV43SchemaAndWriterAcl();}}
    @Test void merchantV42RealHttpsReviewerFlow()throws Exception{try(var cases=adminDashboardCases()){cases.merchantV42RealHttpsReviewerFlow();}}
    @Test void merchantV42ReassignmentRequiresFreshReview()throws Exception{try(var cases=adminDashboardCases()){cases.merchantV42ReassignmentRequiresFreshReview();}}
    @Test void merchantGovernanceRejectsConstructedIdentityContexts()throws Exception{try(var cases=adminDashboardCases()){cases.merchantGovernanceRejectsConstructedIdentityContexts();}}
    @Test void merchantGovernanceV42SchemaAndOperatorReceipts()throws Exception{try(var cases=adminDashboardCases()){cases.merchantGovernanceV42SchemaAndOperatorReceipts();}}
    @AfterEach void releaseCompletedOwnedDatabase() throws Exception {
        if (db != null) com.elogistic.backend.testsupport.OwnedTestDatabase.release(db);
    }
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
            var q=List.of(quota(p,p.name()));for(int i=0;i<p.limit;i++){var r=limiter().reserve(q);if(p.passwordAttempt())r.finishLogin(false);}
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
    private static final String ANALYTICS_PATH="/api/v1/customer/personal-analytics?from=2026-11-01&until=2026-11-03";
    private static final String SHIPPING_REPORTS_PATH=ANALYTICS_PATH.replace("personal-analytics","personal-shipping-reports");
    private String analyticsMetric(tools.jackson.databind.JsonNode report,String id){for(var metric:report.get("metrics"))if(id.equals(metric.get("id").asString()))return metric.get("value").asString();throw new AssertionError("ANALYTICS_METRIC_REQUIRED");}
    @Test void personalManagementFiltersPageAndKpisUseOwnedCommittedSource()throws Exception{
        customerFixture(true);try(var inbox=new CustomerSmtpTestInbox(0,false,false,250,null);var c=customerHttpContext(PILOT_NOW,smtp(inbox,PILOT_NOW),true)){
            var mvc=httpMvc(c);var owner=profileBrowser(mvc);var staff=addressOtherBrowser(mvc);intakeAssignment(mvc,staff,"SIM-DROP-A1");
            String first=null,expensive=null;
            for(int i=0;i<21;i++){
                var input=new HashMap<String,Object>(pilotDraftInput(i==0?"Mgmt%_literal":"Mgmt "+i));
                var recipient=new HashMap<String,Object>((Map<String,Object>)input.get("recipient"));recipient.put("name","Recipient "+i);input.put("recipient",recipient);
                String draft=responseId(onboardingWrite(mvc,owner,"POST",DRAFT_PATH,UUID.randomUUID(),null,input),"id");
                String quote=pilotQuoteId(pilotQuote(mvc,owner,draft,UUID.randomUUID(),"SIM-A1",i==20?"SIM-B1":"SIM-A2"));
                String shipment=pilotShipmentId(pilotBook(mvc,owner,draft,quote,UUID.randomUUID()));if(i==0)first=shipment;if(i==20)expensive=shipment;
            }
            var scans=intakeScans(mvc,staff,intakeBarcodes(mvc,owner,first));assertEquals(200,intakeAccept(mvc,staff,first,scans,UUID.randomUUID(),"\"i0\"").getStatus());
            String path="/api/v1/customer/shipments?view=management";var mapper=new tools.jackson.databind.ObjectMapper();
            var response=httpRead(mvc,path,owner.access());assertEquals(200,response.getStatus());assertTrue(response.getHeader("Cache-Control").contains("no-store"));var report=mapper.readTree(response.getContentAsString());
            assertEquals("PERSONAL_MANAGEMENT_1",report.get("schemaVersion").asString());assertEquals("21",report.get("total").asString());assertEquals("21",report.get("summary").get("totalShipments").asString());assertEquals("20",report.get("summary").get("waitingForHandover").asString());assertEquals("1",report.get("summary").get("atStation").asString());assertEquals(20,report.get("items").size());assertTrue(report.get("hasNext").asBoolean());
            Set<String> ids=new HashSet<>();for(var row:report.get("items"))ids.add(row.get("record").get("shipment").get("id").asString());
            var second=mapper.readTree(httpRead(mvc,path+"&page=1",owner.access()).getContentAsString());assertEquals(1,second.get("items").size());assertTrue(ids.add(second.get("items").get(0).get("record").get("shipment").get("id").asString()));assertEquals(21,ids.size());assertFalse(second.get("hasNext").asBoolean());
            var accepted=mapper.readTree(httpRead(mvc,path+"&status=AT_STATION",owner.access()).getContentAsString());assertEquals("1",accepted.get("total").asString());assertEquals("21",accepted.get("summary").get("totalShipments").asString());assertEquals("AT_STATION",accepted.get("items").get(0).get("status").asString());assertEquals(first,accepted.get("items").get(0).get("record").get("shipment").get("id").asString());assertEquals("NOT_ACCEPTED",accepted.get("items").get(0).get("record").get("shipment").get("carrierState").asString());
            for(String query:List.of("&recipient=Recipient%200&from=2026-11-01&until=2026-11-03&service=ROAD_STANDARD&origin=SIM-A1&destination=SIM-A2&obligation=UNPAID","&search=Mgmt%25_literal"))assertEquals("1",mapper.readTree(mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(java.net.URI.create(path+query)).secure(true).header("Authorization","Bearer "+owner.access())).andReturn().getResponse().getContentAsString()).get("total").asString(),query);
            assertEquals("0",mapper.readTree(httpRead(mvc,path+"&from=2026-10-01&until=2026-10-02",owner.access()).getContentAsString()).get("total").asString());
            var pricey=mapper.readTree(httpRead(mvc,path+"&sort=FEE_DESC",owner.access()).getContentAsString());assertEquals(expensive,pricey.get("items").get(0).get("record").get("shipment").get("id").asString());
            var foreign=mapper.readTree(httpRead(mvc,path,staff.access()).getContentAsString());assertEquals("0",foreign.get("total").asString());assertEquals(0,foreign.get("items").size());assertFalse(foreign.toString().contains(first));
            String phone=accepted.get("items").get(0).get("record").get("shipment").get("input").get("recipient").get("phone").asString();var phoneRead=mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/v1/customer/shipments").param("view","management").param("recipient",phone).secure(true).header("Authorization","Bearer "+owner.access())).andReturn().getResponse();assertEquals("21",mapper.readTree(phoneRead.getContentAsString()).get("total").asString());
            var legacy=mapper.readTree(httpRead(mvc,"/api/v1/customer/shipments",owner.access()).getContentAsString());assertEquals(Set.of("items","page","size","hasNext"),legacy.propertyNames());
            assertEquals(21,scalar("SELECT count(*) FROM finance.pilot_shipping_obligation WHERE state='UNPAID'"));assertEquals(42,scalar("SELECT count(*) FROM orders.pilot_parcel"));
        }
    }
    @Test void personalManagementRejectsSelectorsInvalidQueriesAndProduction()throws Exception{
        customerFixture(false);try(var inbox=new CustomerSmtpTestInbox(0,false,false,250,null);var c=customerHttpContext(PILOT_NOW,smtp(inbox,PILOT_NOW),true)){
            var mvc=httpMvc(c);var owner=profileBrowser(mvc);String path="/api/v1/customer/shipments?view=management";
            for(String extra:List.of("&ownerId="+B,"&organizationId="+B,"&context=BUSINESS","&recipient=A&recipient=B","&view=management","&page=501","&size=10","&sort=UNKNOWN","&status=DELIVERED","&obligation=PAID","&service=AIR","&origin=SIM-A2&destination=SIM-B1","&from=2026-11-01","&from=2026-11-02&until=2026-11-01","&from=2020-01-01&until=2026-11-01"))assertEquals(400,httpRead(mvc,path+extra,owner.access()).getStatus(),extra);
            assertEquals(400,httpRead(mvc,"/api/v1/customer/shipments?recipient=unreviewed",owner.access()).getStatus());
            execute("UPDATE identity.user_account SET auth_version=auth_version+1 WHERE id='"+A+"'");assertEquals(401,httpRead(mvc,path,owner.access()).getStatus());
        }
        try(var inbox=new CustomerSmtpTestInbox(0,false,false,250,null);var c=customerHttpContext(PILOT_NOW,smtp(inbox,PILOT_NOW),true,"PRODUCTION")){
            var mvc=httpMvc(c);var owner=profileBrowser(mvc);assertEquals(409,httpRead(mvc,"/api/v1/customer/shipments?view=management",owner.access()).getStatus());
        }
    }
    @Test void personalAnalyticsUsesRealPostgresOwnerCohortWithoutCollectedRevenue()throws Exception{
        customerFixture(true);try(var inbox=new CustomerSmtpTestInbox(0,false,false,250,null);var c=customerHttpContext(PILOT_NOW,smtp(inbox,PILOT_NOW),true)){
            var mvc=httpMvc(c);var owner=profileBrowser(mvc);var stranger=addressOtherBrowser(mvc);String shipment=intakeBooked(mvc,owner);
            var response=httpRead(mvc,ANALYTICS_PATH,owner.access());assertEquals(200,response.getStatus());assertTrue(response.getHeader("Cache-Control").contains("no-store"));var report=new tools.jackson.databind.ObjectMapper().readTree(response.getContentAsString());
            assertEquals("1",analyticsMetric(report,"BOOKED_SHIPMENTS"));assertEquals("2",analyticsMetric(report,"CREATED_PARCELS"));assertEquals("53000",analyticsMetric(report,"CONFIRMED_FEES"));assertEquals("53000",analyticsMetric(report,"UNPAID_OBLIGATIONS"));assertEquals("1",report.get("shipments").get("total").asString());assertEquals(shipment,report.get("shipments").get("items").get(0).get("id").asString());assertEquals(1,report.get("routes").size());assertEquals("0",analyticsMetric(report,"ACCEPTED_SHIPMENTS"));
            var extendedResponse=httpRead(mvc,SHIPPING_REPORTS_PATH,owner.access());assertEquals(200,extendedResponse.getStatus());assertTrue(extendedResponse.getHeader("Cache-Control").contains("no-store"));var ext=new tools.jackson.databind.ObjectMapper().readTree(extendedResponse.getContentAsString());assertEquals(Set.of("schemaVersion","report","acceptanceTiming","parcelWeights"),ext.propertyNames());assertEquals("PERSONAL_REPORTS_1",ext.get("schemaVersion").asString());assertEquals(report,ext.get("report"));assertEquals("1001",ext.get("parcelWeights").get("actualGrams").asString());assertEquals("1500",ext.get("parcelWeights").get("chargeableGrams").asString());assertEquals("0",ext.get("acceptanceTiming").get("sampleShipments").asString());assertTrue(ext.get("acceptanceTiming").get("minimumMillis").isNull());
            var isolated=new tools.jackson.databind.ObjectMapper().readTree(httpRead(mvc,SHIPPING_REPORTS_PATH,stranger.access()).getContentAsString());assertEquals("0",isolated.get("parcelWeights").get("chargeableGrams").asString());assertEquals("0",analyticsMetric(isolated.get("report"),"BOOKED_SHIPMENTS"));assertFalse(isolated.toString().contains(shipment));
            String json=response.getContentAsString();assertFalse(json.contains("example.invalid"));assertFalse(json.contains("sender"));assertFalse(json.contains("recipient"));assertFalse(json.contains("actorId"));assertFalse(json.contains("address"));
            var foreign=new tools.jackson.databind.ObjectMapper().readTree(httpRead(mvc,ANALYTICS_PATH,stranger.access()).getContentAsString());assertEquals("0",analyticsMetric(foreign,"BOOKED_SHIPMENTS"));assertEquals(0,foreign.get("shipments").get("items").size());assertFalse(foreign.toString().contains(shipment));assertEquals(1,scalar("SELECT count(*) FROM finance.pilot_shipping_obligation WHERE state='UNPAID'"));
        }
    }
    @Test void personalAnalyticsReadsCommittedTrackingAndReconcilesStatusDrill()throws Exception{
        customerFixture(true);try(var inbox=new CustomerSmtpTestInbox(0,false,false,250,null);var c=customerHttpContext(PILOT_NOW,smtp(inbox,PILOT_NOW),true)){
            var mvc=httpMvc(c);var owner=profileBrowser(mvc);var staff=addressOtherBrowser(mvc);intakeAssignment(mvc,staff,"SIM-DROP-A1");String shipment=intakeBooked(mvc,owner);var scans=intakeScans(mvc,staff,intakeBarcodes(mvc,owner,shipment));assertEquals(200,intakeAccept(mvc,staff,shipment,scans,UUID.randomUUID(),"\"i0\"").getStatus());
            var report=new tools.jackson.databind.ObjectMapper().readTree(httpRead(mvc,ANALYTICS_PATH+"&status=AT_STATION",owner.access()).getContentAsString());assertEquals("1",analyticsMetric(report,"ACCEPTED_SHIPMENTS"));assertEquals("0",analyticsMetric(report,"AWAITING_HANDOVER"));assertEquals("2",analyticsMetric(report,"CREATED_PARCELS"));assertEquals("1",report.get("shipments").get("total").asString());assertEquals(shipment,report.get("shipments").get("items").get(0).get("id").asString());assertEquals("AT_STATION",report.get("shipments").get("items").get(0).get("status").asString());
            var ext=new tools.jackson.databind.ObjectMapper().readTree(httpRead(mvc,SHIPPING_REPORTS_PATH+"&status=AT_STATION&page=1",owner.access()).getContentAsString());assertEquals("1",ext.get("acceptanceTiming").get("sampleShipments").asString());assertEquals("0",ext.get("acceptanceTiming").get("totalDurationMillis").asString());assertEquals(0,ext.get("report").get("shipments").get("items").size());assertEquals("1500",ext.get("parcelWeights").get("chargeableGrams").asString());
            var waiting=new tools.jackson.databind.ObjectMapper().readTree(httpRead(mvc,ANALYTICS_PATH+"&status=WAITING_FOR_HANDOVER",owner.access()).getContentAsString());assertEquals("0",analyticsMetric(waiting,"BOOKED_SHIPMENTS"));assertEquals(1,scalar("SELECT count(*) FROM orders.pilot_shipment WHERE carrier_state='NOT_ACCEPTED'"));assertEquals(1,scalar("SELECT count(*) FROM finance.pilot_shipping_obligation WHERE state='UNPAID'"));assertEquals(1,scalar("SELECT count(*) FROM orders.pilot_tracking_event"));
        }
    }
    @Test void personalAnalyticsRejectsOwnerBusinessSelectorsDuplicatesAndRevokedSession()throws Exception{
        customerFixture(false);try(var inbox=new CustomerSmtpTestInbox(0,false,false,250,null);var c=customerHttpContext(PILOT_NOW,smtp(inbox,PILOT_NOW),true)){
            var mvc=httpMvc(c);var owner=profileBrowser(mvc);
            for(String extra:List.of("&ownerId="+B,"&organizationId="+B,"&context=BUSINESS","&page=0&page=1","&status=DELIVERED","&origin=SIM-A2&destination=SIM-B1","&from=2026-11-02")){assertEquals(400,httpRead(mvc,SHIPPING_REPORTS_PATH+extra,owner.access()).getStatus());var rejected=httpRead(mvc,ANALYTICS_PATH+extra,owner.access());assertEquals(400,rejected.getStatus());var error=new tools.jackson.databind.ObjectMapper().readTree(rejected.getContentAsString());assertEquals(Set.of("code","message","requestId"),error.propertyNames());assertEquals("VALIDATION_FAILED",error.get("code").asString());assertEquals("Request rejected.",error.get("message").asString());UUID.fromString(error.get("requestId").asString());}
            assertEquals(0,scalar("SELECT count(*) FROM orders.pilot_shipment"));execute("UPDATE identity.user_account SET auth_version=auth_version+1 WHERE id='"+A+"'");assertEquals(401,httpRead(mvc,ANALYTICS_PATH,owner.access()).getStatus());assertEquals(401,httpRead(mvc,SHIPPING_REPORTS_PATH,owner.access()).getStatus());
        }
    }
    @Test void personalAnalyticsDisabledEnvironmentCannotExposePilotAggregates()throws Exception{
        customerFixture(false);try(var inbox=new CustomerSmtpTestInbox(0,false,false,250,null);var c=customerHttpContext(PILOT_NOW,smtp(inbox,PILOT_NOW),true,"PRODUCTION")){
            var mvc=httpMvc(c);var owner=profileBrowser(mvc);var response=httpRead(mvc,ANALYTICS_PATH,owner.access());assertEquals(503,response.getStatus());assertTrue(response.getContentAsString().contains("PILOT_DISABLED"));assertEquals(503,httpRead(mvc,SHIPPING_REPORTS_PATH,owner.access()).getStatus());assertEquals(0,scalar("SELECT count(*) FROM orders.pilot_shipment"));
        }
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
        return new com.elogistic.backend.identity.api.customer.CustomerEnrollmentCommands.Enrollment(email,ENROLLMENT_PASSWORD,"Synthetic Customer","S003-2026-10-27",key,UUID.randomUUID());
    }
    private com.elogistic.backend.identity.api.customer.CustomerEnrollmentCommands.Verification pendingEnrollmentProof()throws Exception {
        var source=outboxDataSource();var jdbc=new org.springframework.jdbc.core.JdbcTemplate(source);
        return jdbc.queryForObject("SELECT p.*,d.encryption_key_id,d.nonce,d.ciphertext,d.recipient_nonce,d.recipient_ciphertext FROM identity.customer_proof p JOIN identity.customer_delivery d ON d.proof_id=p.id WHERE p.purpose='CUSTOMER_ENROLLMENT' AND p.revoked_at IS NULL AND p.consumed_at IS NULL",(r,n)->{
            var p=new CustomerProof(r.getObject("id",UUID.class),r.getObject("subject_id",UUID.class),CustomerProof.Purpose.CUSTOMER_ENROLLMENT,r.getTimestamp("created_at").toInstant(),r.getTimestamp("expires_at").toInstant(),null,null,null,null,null);
            var crypto=enrollmentProtection();String destination=crypto.openDestination(p,r.getString("verifier_key_id"),r.getString("encryption_key_id"),r.getBytes("recipient_nonce"),r.getBytes("recipient_ciphertext"),p.createdAt());
            assertEquals(ENROLLMENT_EMAIL,destination);
            try(var opened=crypto.open(p,destination,r.getString("verifier_key_id"),r.getString("encryption_key_id"),r.getBytes("nonce"),r.getBytes("ciphertext"),p.createdAt())){
                byte[] token=opened.token();try{return new com.elogistic.backend.identity.api.customer.CustomerEnrollmentCommands.Verification(p.id(),new String(token,0,p.enrollmentOtp()?6:token.length,java.nio.charset.StandardCharsets.US_ASCII),UUID.randomUUID(),REQUEST);}
                finally{Arrays.fill(token,(byte)0);}
            }
        });
    }
    @Test void enrollmentQueuesOnlyProtectedMaterialWithoutAccountOrGrant()throws Exception {
        enrollmentBinding();var before=snapshot();var command=enrollCommand(KEY,ENROLLMENT_EMAIL);var result=enrollmentApp(NOW).enroll(command);
        assertTrue(result.accepted());assertEquals(command.requestId(),result.requestId());
        assertEquals(3,scalar("SELECT count(*) FROM identity.user_account"));assertEquals(0,scalar("SELECT count(*) FROM identity.customer_identifier"));
        assertEquals(1,scalar("SELECT count(*) FROM identity.customer_enrollment WHERE status='PENDING' AND octet_length(protected_password)=60"));
        assertEquals(1,scalar("SELECT count(*) FROM identity.customer_proof WHERE purpose='CUSTOMER_ENROLLMENT' AND expires_at=created_at+INTERVAL '2 minutes' AND octet_length(verifier)=32"));
        assertEquals(1,scalar("SELECT count(*) FROM identity.customer_delivery WHERE status='QUEUED' AND attempts=0 AND accepted_at IS NULL AND delivered_at IS NULL AND octet_length(ciphertext)=59 AND recipient_ciphertext IS NOT NULL"));
        assertEquals(before.get("account_grant"),snapshot().get("account_grant"));
        assertEquals(result.requestId(),pendingEnrollmentProof().proofId());
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
        for(var pair:List.of(Map.entry(NOW,wrong),Map.entry(NOW.minusSeconds(1),good),Map.entry(NOW.plusSeconds(120),good)))
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

    private static java.security.KeyPair customerHttpKeys;
    private static synchronized java.security.KeyPair customerHttpKeys()throws Exception{if(customerHttpKeys==null){var generator=java.security.KeyPairGenerator.getInstance("RSA");generator.initialize(3072);customerHttpKeys=generator.generateKeyPair();}return customerHttpKeys;}
    private com.elogistic.backend.identity.internal.web.ProductionAccessTokens customerTokens(Instant time)throws Exception{return customerTokens(Clock.fixed(time,ZoneOffset.UTC));}
    private com.elogistic.backend.identity.internal.web.ProductionAccessTokens customerTokens(Clock clock)throws Exception{
        return new com.elogistic.backend.identity.internal.web.ProductionAccessTokens("https://customer.example.invalid","customer-browser","customer-test",(java.security.interfaces.RSAPrivateKey)customerHttpKeys().getPrivate(),Map.of("customer-test",(java.security.interfaces.RSAPublicKey)customerHttpKeys().getPublic()),clock);
    }
    private SmtpCustomerDelivery smtp(CustomerSmtpTestInbox inbox,Instant time){
        return new SmtpCustomerDelivery(new SmtpCustomerDelivery.Settings("127.0.0.1",inbox.port(),SmtpCustomerDelivery.Mode.TEST_LOOPBACK,null,null,"no-reply@example.invalid",Set.of("example.invalid"),"https://portal.example.invalid",true),Clock.fixed(time,ZoneOffset.UTC));
    }
    private org.springframework.web.context.support.AnnotationConfigWebApplicationContext customerHttpContext(Instant time,com.elogistic.backend.identity.internal.application.port.CustomerDeliveryTransport transport)throws Exception{
        return customerHttpContext(time,transport,false);
    }
    private org.springframework.web.context.support.AnnotationConfigWebApplicationContext customerHttpContext(Instant time,com.elogistic.backend.identity.internal.application.port.CustomerDeliveryTransport transport,boolean profile)throws Exception{
        return customerHttpContext(time,transport,profile,"TEST");
    }
    private org.springframework.web.context.support.AnnotationConfigWebApplicationContext customerHttpContext(Instant time,com.elogistic.backend.identity.internal.application.port.CustomerDeliveryTransport transport,boolean profile,String pilotMode)throws Exception{
        return customerHttpContext(time,transport,profile,pilotMode,false);
    }
    private org.springframework.web.context.support.AnnotationConfigWebApplicationContext customerHttpContext(Instant time,com.elogistic.backend.identity.internal.application.port.CustomerDeliveryTransport transport,boolean profile,String pilotMode,boolean teamEnabled)throws Exception{
        return customerHttpContext(time,transport,profile,pilotMode,teamEnabled,false);
    }
    private org.springframework.web.context.support.AnnotationConfigWebApplicationContext customerHttpContext(Instant time,com.elogistic.backend.identity.internal.application.port.CustomerDeliveryTransport transport,boolean profile,String pilotMode,boolean teamEnabled,boolean organizationSupportEnabled)throws Exception{
        Clock fixtureClock=liveStaffClock?Clock.systemUTC():Clock.fixed(time,ZoneOffset.UTC);
        boolean productionProfile=pilotMode.equals("TEST_WITH_PRODUCTION_PROFILE");String configuredMode=productionProfile?"TEST":pilotMode;
        HttpFixtureConfiguration.source=outboxDataSource();var source=HttpFixtureConfiguration.source;
        var key=new com.nimbusds.jose.jwk.RSAKey.Builder((java.security.interfaces.RSAPublicKey)httpKeys().getPublic()).privateKey((java.security.interfaces.RSAPrivateKey)httpKeys().getPrivate()).keyID("http-test").keyUse(com.nimbusds.jose.jwk.KeyUse.SIGNATURE).algorithm(com.nimbusds.jose.JWSAlgorithm.RS256).build();
        var context=new org.springframework.web.context.support.AnnotationConfigWebApplicationContext();context.setServletContext(new org.springframework.mock.web.MockServletContext());context.getEnvironment().setActiveProfiles(productionProfile?new String[]{"identity","postgres","customer","production"}:new String[]{"identity","postgres","customer"});
        context.getEnvironment().getPropertySources().addFirst(new org.springframework.core.env.MapPropertySource("isolated-customer-http-fixture",Map.of("identity.security.issuer","https://identity.example.invalid","identity.security.audience","elogistics-browser","identity.security.current-kid","http-test","identity.security.jwk-set",new com.nimbusds.jose.jwk.JWKSet(key).toString(false),"identity.security.origin","https://portal.example.invalid","identity.security.limiter-hmac",Base64.getEncoder().encodeToString(limiterKey()),"customer.rollout","SANDBOX","shipping.pilot.environment",configuredMode)));
        if(biaHttpEnabled)context.getEnvironment().getPropertySources().addFirst(new org.springframework.core.env.MapPropertySource("isolated-bia-intake",Map.of("shipping.pilot.business-intake-enabled",true)));
        // Real production composition with deterministic test clock, test keys, same runtime DS and explicit manual worker tick.
        var services=com.elogistic.backend.identity.internal.ProductionCustomerComposition.create(source,customerTokens(fixtureClock),enrollmentProtection(),new JdbcIdentityRateLimiter(source,keyBytes(73)),transport,"https://portal.example.invalid",fixtureClock,()->(i,p,o,a,n,e)->context.getBean(com.elogistic.backend.merchant.api.customer.OrganizationTeam.class).deliveryCurrent(i,p,o,a,n,e));
        var staffTokens=new com.elogistic.backend.identity.internal.web.ProductionAccessTokens("https://customer.example.invalid","argopost-local-support-staff","http-test",(java.security.interfaces.RSAPrivateKey)httpKeys().getPrivate(),Map.of("http-test",(java.security.interfaces.RSAPublicKey)httpKeys().getPublic()),Clock.systemUTC());
        var staffTransport=transport instanceof SmtpCustomerDelivery actual?actual:new SmtpCustomerDelivery(new SmtpCustomerDelivery.Settings("127.0.0.1",12345,SmtpCustomerDelivery.Mode.TEST_LOOPBACK,null,null,"staff@example.invalid",Set.of("example.invalid"),"https://portal.example.invalid",true),Clock.systemUTC());
        var staffServices=com.elogistic.backend.identity.internal.ProductionStaffComposition.create(source,staffTokens,staffProtection(),new JdbcIdentityRateLimiter(source,keyBytes(73)),staffTransport,"https://portal.example.invalid","TEST",Clock.systemUTC(),()->Boolean.TRUE.equals(new org.springframework.jdbc.core.JdbcTemplate(source).queryForObject("SELECT identity.local_staff_environment_matches('TEST')",Boolean.class)));
        context.addBeanFactoryPostProcessor(factory->{factory.registerSingleton("staffServices",staffServices);factory.registerSingleton("staffAdmission",staffServices.admission());factory.registerSingleton("staffRequestContext",staffServices.requests());});
        context.register(com.elogistic.backend.identity.internal.ProductionStaffWebConfiguration.class);
        if(nwsHttpEnabled){
            var opsTokens=new com.elogistic.backend.identity.internal.web.ProductionAccessTokens("https://customer.example.invalid","argopost-local-operations-staff","http-test",(java.security.interfaces.RSAPrivateKey)httpKeys().getPrivate(),Map.of("http-test",(java.security.interfaces.RSAPublicKey)httpKeys().getPublic()),Clock.systemUTC());
            var operations=com.elogistic.backend.identity.internal.ProductionOperationsComposition.create(source,opsTokens,staffProtection(),new JdbcIdentityRateLimiter(source,keyBytes(73)),staffTransport,"https://portal.example.invalid","TEST",Clock.systemUTC(),()->Boolean.TRUE.equals(new org.springframework.jdbc.core.JdbcTemplate(source).queryForObject("SELECT identity.local_staff_environment_matches('TEST')",Boolean.class)));
            context.addBeanFactoryPostProcessor(factory->{
                factory.registerSingleton("operationsServices",operations);factory.registerSingleton("operationalStaffAdmission",operations.admission());factory.registerSingleton("operationsRequests",operations.requests());
                var primary=new org.springframework.beans.factory.support.RootBeanDefinition(com.elogistic.backend.identity.api.staff.StaffRequestContext.class);primary.setInstanceSupplier(staffServices::requests);primary.setPrimary(true);((org.springframework.beans.factory.support.BeanDefinitionRegistry)factory).registerBeanDefinition("staffRequestContext",primary);
            });
            context.register(com.elogistic.backend.identity.internal.ProductionOperationsWebConfiguration.class,com.elogistic.backend.workflow.internal.web.OperationalStationHttp.class);
        }

        context.addBeanFactoryPostProcessor(factory->{factory.registerSingleton("identityHttpClock",fixtureClock);factory.registerSingleton("customerServices",services);});
        context.register(HttpFixtureConfiguration.class,com.elogistic.backend.identity.internal.ProductionIdentityHttpConfiguration.class,com.elogistic.backend.identity.internal.ProductionCustomerWebConfiguration.class,com.elogistic.backend.identity.internal.web.ProductionIdentityReads.class,com.elogistic.backend.identity.internal.web.ProductionIdentityErrors.class);
        if(profile){
            context.getEnvironment().getPropertySources().addFirst(new org.springframework.core.env.MapPropertySource("isolated-profile-keys",Map.of("merchant.customer-profile.receipt-keys","{\"v1\":\""+Base64.getEncoder().encodeToString(keyBytes(76))+"\"}","merchant.customer-profile.current-receipt-key","v1","support.tickets.enabled","true","support.tickets.environment",configuredMode,"business.workspace.enabled","true","business.workspace.environment",configuredMode,"business.team.enabled",Boolean.toString(teamEnabled),"support.tickets.organization-enabled",Boolean.toString(organizationSupportEnabled))));
            context.addBeanFactoryPostProcessor(factory->{factory.registerSingleton("customerAdmission",services.customers());factory.registerSingleton("customerRequestContext",services.requests());factory.registerSingleton("customerMemberNames",new JdbcCustomerMemberNames(source,services.customers()));factory.registerSingleton("customerReauthentication",services.reauthentication());factory.registerSingleton("customerMembershipInvitations",services.memberships());});
            context.addBeanFactoryPostProcessor(factory->{factory.registerSingleton("claimPrivateEvidenceStore",claimStore!=null?claimStore:new com.elogistic.backend.platform.api.PrivateEvidenceStore(){public Blob put(java.io.InputStream in){throw new Unavailable();}public Content read(Blob b){throw new Unavailable();}});factory.registerSingleton("claimEvidenceScreening",claimScanner!=null?claimScanner:(com.elogistic.backend.platform.api.PrivateEvidenceScreening)(bytes,mime)->{throw new com.elogistic.backend.platform.api.PrivateEvidenceScreening.Unavailable();});});
            context.getEnvironment().getPropertySources().addFirst(new org.springframework.core.env.MapPropertySource("claim-intake-test-gate",Map.of("support.claim-intakes.enabled",Boolean.toString(claimIntakeEnabled),"support.claim-evidence.enabled",Boolean.toString(claimEvidenceEnabled),"identity.staff.enabled",Boolean.toString(claimIntakeEnabled),"identity.staff.environment","TEST")));
            context.register(com.elogistic.backend.workflow.internal.web.ClaimEvidenceHttp.class,com.elogistic.backend.orders.internal.OrganizationClaimIntakeConfiguration.class,com.elogistic.backend.workflow.internal.web.OrganizationClaimIntakeHttp.class,com.elogistic.backend.orders.internal.OrganizationSupportConfiguration.class,com.elogistic.backend.workflow.internal.web.OrganizationSupportHttp.class,com.elogistic.backend.merchant.internal.OrganizationTeamConfiguration.class,com.elogistic.backend.workflow.internal.web.OrganizationTeamHttp.class,com.elogistic.backend.workflow.internal.web.MerchantFacilityWorkHttp.class,com.elogistic.backend.workflow.internal.web.MerchantFacilitiesHttp.class,com.elogistic.backend.workflow.internal.web.BusinessReportingHttp.class,com.elogistic.backend.workflow.internal.web.OrganizationShipmentManagementHttp.class,com.elogistic.backend.workflow.internal.web.OrganizationContactsHttp.class,com.elogistic.backend.workflow.internal.web.OrganizationActionsHttp.class,com.elogistic.backend.workflow.internal.web.BusinessShippingHttp.class,com.elogistic.backend.orders.internal.BusinessPreparationConfiguration.class,com.elogistic.backend.workflow.internal.web.BusinessPreparationHttp.class,com.elogistic.backend.workflow.internal.web.BusinessImportHttp.class,com.elogistic.backend.merchant.internal.BusinessWorkspaceConfiguration.class,com.elogistic.backend.notifications.internal.BusinessDashboardConfiguration.class,com.elogistic.backend.workflow.internal.web.BusinessWorkspaceHttp.class,com.elogistic.backend.orders.internal.SupportTicketConfiguration.class,com.elogistic.backend.workflow.internal.web.SupportTicketHttp.class,com.elogistic.backend.orders.internal.AnalyticsSourceConfiguration.class,com.elogistic.backend.finance.internal.AnalyticsFinanceConfiguration.class,com.elogistic.backend.notifications.internal.AnalyticsConfiguration.class,com.elogistic.backend.workflow.internal.web.PersonalAnalyticsHttp.class,com.elogistic.backend.dispatch.internal.StationIntakeConfiguration.class,com.elogistic.backend.workflow.internal.web.StationIntakeHttp.class,com.elogistic.backend.pricing.internal.PilotPricingConfiguration.class,com.elogistic.backend.orders.internal.PilotBookingConfiguration.class,com.elogistic.backend.finance.internal.PilotFinanceConfiguration.class,com.elogistic.backend.workflow.internal.web.PilotShippingHttp.class,com.elogistic.backend.orders.internal.ShippingDraftConfiguration.class,com.elogistic.backend.workflow.internal.web.ShippingDraftHttp.class,com.elogistic.backend.merchant.internal.CustomerProfileConfiguration.class,com.elogistic.backend.workflow.internal.web.CustomerProfileHttp.class,com.elogistic.backend.workflow.internal.web.PersonalAddressHttp.class,com.elogistic.backend.workflow.internal.web.MerchantApplicationHttp.class,com.elogistic.backend.workflow.internal.web.OnboardingHttpErrors.class);
        }
        try{context.refresh();return context;}catch(Exception failure){context.close();throw failure;}
    }
    private BrowserFlow customerCsrf(org.springframework.test.web.servlet.MockMvc mvc,BrowserFlow old)throws Exception{
        var request=org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/v1/customer-auth/csrf").secure(true).header("Origin","https://portal.example.invalid");if(old!=null)request.cookie(old.csrf,old.refresh);
        var response=mvc.perform(request).andReturn().getResponse();assertEquals(200,response.getStatus());String challenge=new tools.jackson.databind.ObjectMapper().readTree(response.getContentAsString()).get("challenge").asString();
        return new BrowserFlow(responseCookie(response,"__Host-elogistics.customer.csrf"),old==null?new jakarta.servlet.http.Cookie("__Host-elogistics.customer.refresh",""):old.refresh,challenge);
    }
    private org.springframework.mock.web.MockHttpServletResponse customerPost(org.springframework.test.web.servlet.MockMvc mvc,String route,BrowserFlow flow,UUID key,Object body)throws Exception{
        var request=org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/v1/customer-auth/"+route).secure(true).header("Origin","https://portal.example.invalid").header("X-CSRF-Token",flow.challenge).cookie(flow.csrf);if(!flow.refresh.getValue().isEmpty())request.cookie(flow.refresh);if(key!=null)request.header("Idempotency-Key",key);if(body!=null)request.contentType("application/json").content(new tools.jackson.databind.ObjectMapper().writeValueAsBytes(body));return mvc.perform(request).andReturn().getResponse();
    }
    private Map<String,String> customerRegistration(){return Map.of("email",ENROLLMENT_EMAIL,"password",ENROLLMENT_PASSWORD,"displayName","Synthetic Customer","phone","0901-234-567","consentVersion","S003-2026-10-27");}
    private Map<String,String> receivedVerification(CustomerSmtpTestInbox inbox)throws Exception{
        var received=inbox.messages();assertEquals(1,received.size());var email=new jakarta.mail.internet.MimeMessage(jakarta.mail.Session.getInstance(new Properties()),new java.io.ByteArrayInputStream(received.getFirst()));
        String content=CustomerSmtpTestInbox.plainText(email);
        var code=java.util.regex.Pattern.compile("(?m)^([0-9]{6})\\r?$").matcher(content);
        assertTrue(code.find());assertFalse(content.contains("#proofId="));
        String locator=new org.springframework.jdbc.core.JdbcTemplate(outboxDataSource()).queryForObject("SELECT id::text FROM identity.customer_proof WHERE purpose='CUSTOMER_ENROLLMENT' AND consumed_at IS NULL AND revoked_at IS NULL",String.class);
        return Map.of("proofId",locator,"proof",code.group(1));
    }
    @Test void customerHttpEnrollmentMailVerificationLoginRefreshLogoutUseActualChains()throws Exception{
        try(var inbox=new CustomerSmtpTestInbox(1,false,false,250,null);var context=customerHttpContext(NOW,smtp(inbox,NOW))){
            var mvc=httpMvc(context);var pre=customerCsrf(mvc,null);var queued=customerPost(mvc,"enrollments",pre,KEY,customerRegistration());assertEquals(202,queued.getStatus());assertEquals(3,scalar("SELECT count(*) FROM identity.user_account"));
            assertEquals(1,scalar("SELECT count(*) FROM identity.customer_enrollment WHERE unverified_phone='+84901234567'"));
            var services=context.getBean(com.elogistic.backend.identity.internal.ProductionCustomerComposition.Services.class);assertTrue(services.delivery().runOne());
            assertEquals(1,scalar("SELECT count(*) FROM identity.customer_delivery WHERE status='ACCEPTED_BY_PROVIDER' AND accepted_at IS NOT NULL AND delivered_at IS NULL AND delivery_evidence IS NULL AND ciphertext IS NULL AND recipient_ciphertext IS NULL"));
            var proof=receivedVerification(inbox);assertEquals(204,customerPost(mvc,"verifications",pre,id(910),proof).getStatus());
            assertEquals(1,scalar("SELECT count(*) FROM identity.customer_phone_contact WHERE canonical_phone='+84901234567' AND verification_status='UNVERIFIED'"));
            assertEquals(1,scalar("SELECT count(*) FROM identity.customer_delivery WHERE status='DELIVERED' AND delivery_evidence IS NOT NULL"));
            var login=customerPost(mvc,"login",pre,null,Map.of("email",ENROLLMENT_EMAIL,"password",ENROLLMENT_PASSWORD));assertEquals(200,login.getStatus());
            var accessBody=new tools.jackson.databind.ObjectMapper().readTree(login.getContentAsString());assertEquals(3,accessBody.size());String access=accessBody.get("accessToken").asString();
            var me=httpRead(mvc,"/api/v1/customer-auth/me",access);assertEquals(200,me.getStatus());assertEquals(4,new tools.jackson.databind.ObjectMapper().readTree(me.getContentAsString()).size());
            assertEquals(401,httpRead(mvc,"/api/v1/auth/me",access).getStatus());assertEquals(401,httpRead(mvc,"/api/v1/admin/users",access).getStatus());
            var flow=customerCsrf(mvc,new BrowserFlow(responseCookie(login,"__Host-elogistics.customer.csrf"),responseCookie(login,"__Host-elogistics.customer.refresh"),"unused"));
            var refreshed=customerPost(mvc,"refresh",flow,null,null);assertEquals(200,refreshed.getStatus());
            var next=new BrowserFlow(responseCookie(refreshed,"__Host-elogistics.customer.csrf"),responseCookie(refreshed,"__Host-elogistics.customer.refresh"),flow.challenge);
            assertEquals(204,customerPost(mvc,"logout",next,null,null).getStatus());assertEquals(401,httpRead(mvc,"/api/v1/customer-auth/me",access).getStatus());
            assertEquals(0,scalar("SELECT count(*) FROM identity.account_grant g JOIN identity.user_account a ON a.id=g.account_id WHERE a.email='"+ENROLLMENT_EMAIL+"'"));assertEquals(3,scalar("SELECT count(*) FROM identity.auth_session"));
        }
    }
    @Test void customerHttpSystemTokenAndRevokedAccountCannotBecomeCustomerAuthority()throws Exception{
        customerFixture(true);try(var inbox=new CustomerSmtpTestInbox(0,false,false,250,null);var context=customerHttpContext(NOW,smtp(inbox,NOW))){
            var mvc=httpMvc(context);String system=httpToken(context,S,A,1,NOW,NOW.plusSeconds(900));assertEquals(401,httpRead(mvc,"/api/v1/customer-auth/me",system).getStatus());
            var login=customerPost(mvc,"login",customerCsrf(mvc,null),null,Map.of("email",CUSTOMER_EMAIL,"password",SESSION_PASSWORD));assertEquals(200,login.getStatus());String access=new tools.jackson.databind.ObjectMapper().readTree(login.getContentAsString()).get("accessToken").asString();
            assertEquals(200,httpRead(mvc,"/api/v1/customer-auth/me",access).getStatus());execute("UPDATE identity.user_account SET status='DISABLED',disabled_at=updated_at WHERE id='"+A+"'");assertEquals(401,httpRead(mvc,"/api/v1/customer-auth/me",access).getStatus());
        }
    }
    @Test void customerHttpRegistrationReplayAndDurableQuotaAreNotReleasedByRollback()throws Exception{
        try(var inbox=new CustomerSmtpTestInbox(0,false,false,250,null);var context=customerHttpContext(NOW,smtp(inbox,NOW))){
            var mvc=httpMvc(context);var pre=customerCsrf(mvc,null);for(int i=0;i<5;i++)assertEquals(202,customerPost(mvc,"enrollments",pre,KEY,customerRegistration()).getStatus());
            var exhausted=customerPost(mvc,"enrollments",pre,KEY,customerRegistration());assertEquals(429,exhausted.getStatus());assertNotNull(exhausted.getHeader("Retry-After"));assertEquals(1,scalar("SELECT count(*) FROM identity.customer_enrollment"));assertEquals(1,scalar("SELECT count(*) FROM identity.customer_delivery"));
            assertEquals(5,scalar("SELECT count(*) FROM identity.rate_limit_attempt WHERE policy='CUSTOMER_ENROLL_EMAIL' AND state='COUNTED'"));
        }
    }
    @Test void customerHttpQuotaStorageOutageDeniesBeforePendingAccountOrDelivery()throws Exception{
        try(var inbox=new CustomerSmtpTestInbox(0,false,false,250,null);var context=customerHttpContext(NOW,smtp(inbox,NOW))){
            var mvc=httpMvc(context);var pre=customerCsrf(mvc,null);failWrite("rate_limit_attempt","INSERT");var failed=customerPost(mvc,"enrollments",pre,KEY,customerRegistration());assertEquals(503,failed.getStatus());assertEquals(0,scalar("SELECT count(*) FROM identity.customer_enrollment"));assertEquals(0,scalar("SELECT count(*) FROM identity.customer_delivery"));assertFalse(failed.getContentAsString().contains("SYNTHETIC_WRITE_FAILURE"));
        }
    }
    @Test void deliveryLeaseFencingKeepsExpiredWorkerFromRecordingNewLeaseOutcome()throws Exception{
        enrollmentBinding();enrollmentApp(NOW).enroll(enrollCommand(KEY,ENROLLMENT_EMAIL));var store=new JdbcCustomerDeliveryStore(outboxDataSource());
        try(var first=store.claim(NOW).orElseThrow();var second=store.claim(NOW.plusSeconds(30)).orElseThrow()){
            var accepted=new com.elogistic.backend.identity.internal.application.port.CustomerDeliveryTransport.Outcome(com.elogistic.backend.identity.internal.application.port.CustomerDeliveryTransport.Kind.ACCEPTED_BY_PROVIDER,"TEST:"+first.delivery().id(),true);
            assertFalse(store.finish(first,accepted,NOW.plusSeconds(31)));assertTrue(store.finish(second,accepted,NOW.plusSeconds(31)));
            assertEquals(1,scalar("SELECT count(*) FROM identity.customer_delivery WHERE status='ACCEPTED_BY_PROVIDER' AND attempts=2 AND delivered_at IS NULL"));
        }
    }
    @Test void deliveryBoundedFailuresAndDeadlineScrubPendingSecretsWithoutDeletingHistory()throws Exception{
        enrollmentBinding();enrollmentApp(NOW).enroll(enrollCommand(KEY,ENROLLMENT_EMAIL));var store=new JdbcCustomerDeliveryStore(outboxDataSource());Instant time=NOW;
        for(int i=0;i<2;i++)try(var lease=store.claim(time).orElseThrow()){
            assertTrue(store.finish(lease,new com.elogistic.backend.identity.internal.application.port.CustomerDeliveryTransport.Outcome(com.elogistic.backend.identity.internal.application.port.CustomerDeliveryTransport.Kind.TRANSIENT_FAILURE,null,true),time.plusSeconds(1)));time=time.plusSeconds(1+(30L<<i));
        }
        assertEquals(1,scalar("SELECT count(*) FROM identity.customer_delivery WHERE status='RETRY_WAIT' AND attempts=2"));
        assertTrue(store.claim(NOW.plusSeconds(120)).isEmpty());
        assertEquals(1,scalar("SELECT count(*) FROM identity.customer_delivery WHERE status='EXPIRED' AND attempts=2 AND ciphertext IS NULL AND recipient_ciphertext IS NULL"));
        assertEquals(1,store.expire(NOW.plusSeconds(86400)));assertEquals(1,scalar("SELECT count(*) FROM identity.customer_enrollment WHERE status='EXPIRED' AND protected_password IS NULL"));assertEquals(1,scalar("SELECT count(*) FROM identity.customer_proof"));assertEquals(1,scalar("SELECT count(*) FROM identity.customer_command_receipt"));assertEquals(3,scalar("SELECT count(*) FROM identity.user_account"));
    }
    @Test void deliveryAuditFailureRollsBackLeaseAndRealSmtpRunsOutsidePhysicalTransaction()throws Exception{
        enrollmentBinding();enrollmentApp(NOW).enroll(enrollCommand(KEY,ENROLLMENT_EMAIL));failWrite("audit_entry","INSERT");var store=new JdbcCustomerDeliveryStore(outboxDataSource());assertThrows(RuntimeException.class,()->store.claim(NOW));assertEquals(1,scalar("SELECT count(*) FROM identity.customer_delivery WHERE status='QUEUED' AND attempts=0 AND lease_id IS NULL"));
        db.ownerExecute("DROP TRIGGER synthetic_fault ON identity.audit_entry");
        try(var inbox=new CustomerSmtpTestInbox(1,false,false,250,null)){
            var transport=smtp(inbox,NOW);var worker=new com.elogistic.backend.identity.internal.application.CustomerDeliveryWorker(store,enrollmentProtection(),message->{assertFalse(org.springframework.transaction.support.TransactionSynchronizationManager.isActualTransactionActive());assertTrue(org.springframework.transaction.support.TransactionSynchronizationManager.getResourceMap().isEmpty());return transport.send(message);},Clock.fixed(NOW,ZoneOffset.UTC));
            assertTrue(worker.runOne());assertEquals(1,inbox.messages().size());assertEquals(1,scalar("SELECT count(*) FROM identity.customer_delivery WHERE status='ACCEPTED_BY_PROVIDER'"));
        }
    }
    @Test void missingDeliveryKeyBlocksWithoutSendingOrClaimingProviderAcceptance()throws Exception{
        enrollmentBinding();enrollmentApp(NOW).enroll(enrollCommand(KEY,ENROLLMENT_EMAIL));
        try(var inbox=new CustomerSmtpTestInbox(0,false,false,250,null)){
            var other=new ProtectedCustomerProofs(Map.of("v2",keyBytes(74)),"v2",Map.of("e2",keyBytes(75)),"e2");
            var worker=new com.elogistic.backend.identity.internal.application.CustomerDeliveryWorker(new JdbcCustomerDeliveryStore(outboxDataSource()),other,smtp(inbox,NOW),Clock.fixed(NOW,ZoneOffset.UTC));
            assertTrue(worker.runOne());assertTrue(inbox.messages().isEmpty());assertEquals(1,scalar("SELECT count(*) FROM identity.customer_delivery WHERE status='BLOCKED' AND attempts=1 AND accepted_at IS NULL AND delivered_at IS NULL AND ciphertext IS NOT NULL"));
        }
    }
    @Test void customerPasswordQuotaReleasesOnlyItsOwnSuccessAndCannotConsumeSystemBudget()throws Exception{
        var limiter=new JdbcIdentityRateLimiter(outboxDataSource(),keyBytes(73));var customer=com.elogistic.backend.identity.internal.application.port.IdentityRateLimiter.Policy.CUSTOMER_LOGIN_EMAIL;
        var subject=List.of(quota(customer,"same-exact-identifier"));limiter.reserve(subject).finishLogin(true);
        for(int i=0;i<5;i++)limiter.reserve(subject).finishLogin(false);
        assertThrows(com.elogistic.backend.identity.internal.application.port.IdentityRateLimiter.Exhausted.class,()->limiter.reserve(subject));
        limiter.reserve(List.of(quota(com.elogistic.backend.identity.internal.application.port.IdentityRateLimiter.Policy.LOGIN_ACCOUNT,"same-exact-identifier"))).finishLogin(false);
        assertEquals(1,scalar("SELECT count(*) FROM identity.rate_limit_attempt WHERE policy='CUSTOMER_LOGIN_EMAIL' AND state='RELEASED'"));assertEquals(5,scalar("SELECT count(*) FROM identity.rate_limit_attempt WHERE policy='CUSTOMER_LOGIN_EMAIL' AND state='FAILED'"));assertEquals(1,scalar("SELECT count(*) FROM identity.rate_limit_attempt WHERE policy='LOGIN_ACCOUNT' AND state='FAILED'"));
    }
    @Test void customerPreauthCsrfMustBeUnboundCurrentAndNeverSystemPurpose()throws Exception{
        customerFixture(false);var source=outboxDataSource();var customer=new JdbcIdentityBrowserState(source,IdentitySessionStorage.CUSTOMER);var system=new JdbcIdentityBrowserState(source);
        var first=customer.rebind(null,null);assertNotNull(customer.requirePreauthentication(first.cookie(),first.token()));assertThrows(IdentityCommandFailure.class,()->system.requirePreauthentication(first.cookie(),first.token()));
        try(var login=customerLogin()){
            var bound=customer.rebind(first.cookie(),login.access().sessionId());assertThrows(IdentityCommandFailure.class,()->customer.requirePreauthentication(first.cookie(),first.token()));assertThrows(IdentityCommandFailure.class,()->customer.requirePreauthentication(bound.cookie(),bound.token()));
        }
    }
    @Test void enrollmentAuditRollbackCannotReleaseDurableCustomerQuota()throws Exception{
        try(var inbox=new CustomerSmtpTestInbox(0,false,false,250,null);var context=customerHttpContext(NOW,smtp(inbox,NOW))){
            var mvc=httpMvc(context);var pre=customerCsrf(mvc,null);failWrite("audit_entry","INSERT");assertEquals(503,customerPost(mvc,"enrollments",pre,KEY,customerRegistration()).getStatus());
            assertEquals(0,scalar("SELECT count(*) FROM identity.customer_enrollment"));assertEquals(0,scalar("SELECT count(*) FROM identity.customer_delivery"));assertEquals(0,scalar("SELECT count(*) FROM identity.customer_command_receipt"));assertEquals(1,scalar("SELECT count(*) FROM identity.rate_limit_attempt WHERE policy='CUSTOMER_ENROLL_EMAIL' AND state='COUNTED'"));
        }
    }
    private com.elogistic.backend.identity.api.customer.CustomerRecoveryCommands recoveryApp(Instant time){
        return new com.elogistic.backend.identity.internal.application.CustomerRecoveries(new JdbcCustomerRecoveryStore(outboxDataSource(),enrollmentProtection()),enrollmentProtection(),
            new com.elogistic.backend.identity.internal.application.port.CustomerEnrollmentAdmission(){public Binding admit(Operation operation,UUID request,String subject){return new Binding(ENROLLMENT_BINDING);}public void requireCurrent(Binding binding){assertEquals(ENROLLMENT_BINDING,binding.id());}},Clock.fixed(time,ZoneOffset.UTC));
    }
    /** Private negative-test fixture extraction, not SMTP delivery evidence. */
    private com.elogistic.backend.identity.api.customer.CustomerRecoveryCommands.Confirmation pendingRecoveryProof()throws Exception{
        var jdbc=new org.springframework.jdbc.core.JdbcTemplate(outboxDataSource());
        return jdbc.queryForObject("SELECT p.*,d.encryption_key_id,d.nonce,d.ciphertext,d.recipient_nonce,d.recipient_ciphertext FROM identity.customer_proof p JOIN identity.customer_delivery d ON d.proof_id=p.id WHERE p.purpose='CUSTOMER_RECOVERY' AND p.revoked_at IS NULL AND p.consumed_at IS NULL",(r,n)->{
            var p=new CustomerProof(r.getObject("id",UUID.class),r.getObject("subject_id",UUID.class),CustomerProof.Purpose.CUSTOMER_RECOVERY,r.getTimestamp("created_at").toInstant(),r.getTimestamp("expires_at").toInstant(),null,r.getLong("bound_auth_version"),null,null,null);
            var crypto=enrollmentProtection();String destination=crypto.openDestination(p,r.getString("verifier_key_id"),r.getString("encryption_key_id"),r.getBytes("recipient_nonce"),r.getBytes("recipient_ciphertext"),p.createdAt());assertEquals(CUSTOMER_EMAIL,destination);
            try(var opened=crypto.open(p,destination,r.getString("verifier_key_id"),r.getString("encryption_key_id"),r.getBytes("nonce"),r.getBytes("ciphertext"),p.createdAt())){
                byte[] token=p.emailOtp()?crypto.recoveryGrant(p,r.getString("verifier_key_id"),r.getBytes("verifier"),p.createdAt()):opened.token();try{return new com.elogistic.backend.identity.api.customer.CustomerRecoveryCommands.Confirmation(p.id(),new String(token,java.nio.charset.StandardCharsets.US_ASCII),"synthetic-recovered-password",UUID.randomUUID(),REQUEST);}finally{Arrays.fill(token,(byte)0);}
            }
        });
    }
    private com.elogistic.backend.identity.api.customer.CustomerRecoveryCommands.Email recoveryRequest(String email,UUID key){return new com.elogistic.backend.identity.api.customer.CustomerRecoveryCommands.Email(email,key,UUID.randomUUID());}
    @Test void recoveryUniformRequestsNeverUseUnverifiedDisabledStaleOrLegacyOnlyEmail()throws Exception{
        enrollmentBinding();customerFixture(true);long existingAccounts=scalar("SELECT count(*) FROM identity.user_account");var app=recoveryApp(NOW);
        assertTrue(app.request(recoveryRequest("unknown@example.invalid",KEY)).accepted());assertEquals(0,scalar("SELECT count(*) FROM identity.customer_delivery"));
        execute("UPDATE identity.user_account SET status='DISABLED',disabled_at=updated_at WHERE id='"+A+"'");assertTrue(app.request(recoveryRequest(CUSTOMER_EMAIL,id(920))).accepted());assertEquals(0,scalar("SELECT count(*) FROM identity.customer_delivery"));
        execute("UPDATE identity.user_account SET status='ACTIVE',disabled_at=NULL,email='changed@example.invalid' WHERE id='"+A+"'");assertTrue(app.request(recoveryRequest(CUSTOMER_EMAIL,id(921))).accepted());assertEquals(0,scalar("SELECT count(*) FROM identity.customer_delivery"));
        assertTrue(app.request(recoveryRequest("fixture@example.invalid",id(922))).accepted());assertEquals(0,scalar("SELECT count(*) FROM identity.customer_delivery"));
        execute("UPDATE identity.user_account SET status='PENDING_ACTIVATION',password_hash=NULL,email_verified=FALSE,activated_at=NULL,email='unverified@example.invalid' WHERE id='"+T+"'");
        execute("INSERT INTO identity.customer_identifier VALUES ('"+id(940)+"','"+T+"','unverified@example.invalid',TIMESTAMPTZ '2026-10-21 00:05:00Z','ACTIVE',1)");
        assertTrue(app.request(recoveryRequest("unverified@example.invalid",id(930))).accepted());assertEquals(0,scalar("SELECT count(*) FROM identity.customer_delivery"));
        execute("UPDATE identity.user_account SET email='"+CUSTOMER_EMAIL+"' WHERE id='"+A+"'");assertTrue(app.request(recoveryRequest(CUSTOMER_EMAIL,id(923))).accepted());assertEquals(1,scalar("SELECT count(*) FROM identity.customer_delivery"));assertEquals(existingAccounts,scalar("SELECT count(*) FROM identity.user_account"));
    }
    @Test void recoveryHttpActualMailResetsCredentialRevokesBothFamiliesAndPreservesGrants()throws Exception{
        customerFixture(true);var grants=snapshot().get("account_grant");
        try(var systemLogin=sessionLogin();var customerLogin=customerLogin();var inbox=new CustomerSmtpTestInbox(1,false,false,250,null);var context=customerHttpContext(NOW,smtp(inbox,NOW))){
            var mvc=httpMvc(context);var pre=customerCsrf(mvc,null);assertEquals(202,customerPost(mvc,"recovery",pre,KEY,Map.of("email",CUSTOMER_EMAIL)).getStatus());
            assertTrue(context.getBean(com.elogistic.backend.identity.internal.ProductionCustomerComposition.Services.class).delivery().runOne());
            var received=inbox.messages();assertEquals(1,received.size());var mail=new jakarta.mail.internet.MimeMessage(jakarta.mail.Session.getInstance(new Properties()),new java.io.ByteArrayInputStream(received.getFirst()));assertEquals(CUSTOMER_EMAIL,mail.getAllRecipients()[0].toString());
            var code=java.util.regex.Pattern.compile("(?m)^([0-9]{6})\\r?$").matcher(CustomerSmtpTestInbox.plainText(mail));assertTrue(code.find());
            String locator=new org.springframework.jdbc.core.JdbcTemplate(outboxDataSource()).queryForObject("SELECT id::text FROM identity.customer_proof WHERE purpose='CUSTOMER_RECOVERY' AND consumed_at IS NULL AND revoked_at IS NULL",String.class);
            assertEquals(422,customerPost(mvc,"recovery-confirm",pre,UUID.randomUUID(),Map.of("proofId",locator,"proof",code.group(1),"newPassword","synthetic-recovered-password")).getStatus());
            String wrong=code.group(1).equals("000000")?"999999":"000000";
            assertEquals(422,customerPost(mvc,"recovery-verifications",pre,UUID.randomUUID(),Map.of("proofId",locator,"proof",wrong)).getStatus());
            var verified=customerPost(mvc,"recovery-verifications",pre,UUID.randomUUID(),Map.of("proofId",locator,"proof",code.group(1)));assertEquals(200,verified.getStatus());
            var grant=new tools.jackson.databind.ObjectMapper().readTree(verified.getContentAsString());assertEquals(43,grant.get("proof").asString().length());assertEquals(NOW.plusSeconds(120),Instant.parse(grant.get("expiresAt").asString()));
            assertEquals(0,scalar("SELECT count(*) FROM identity.customer_proof WHERE purpose='CUSTOMER_RECOVERY' AND consumed_at IS NOT NULL"));
            var body=Map.of("proofId",locator,"proof",grant.get("proof").asString(),"newPassword","synthetic-recovered-password");var reset=customerPost(mvc,"recovery-confirm",pre,id(924),body);assertEquals(204,reset.getStatus());
            assertEquals(2,scalar("SELECT auth_version FROM identity.user_account WHERE id='"+A+"'"));assertEquals(0,scalar("SELECT count(*) FROM identity.auth_session WHERE account_id='"+A+"' AND revoked_at IS NULL"));assertEquals(0,scalar("SELECT count(*) FROM identity.customer_auth_session WHERE account_id='"+A+"' AND revoked_at IS NULL"));assertEquals(grants,snapshot().get("account_grant"));
            assertEquals(1,scalar("SELECT count(*) FROM identity.customer_delivery WHERE status='DELIVERED' AND delivery_evidence IS NOT NULL"));
            var rotated=customerCsrf(mvc,pre);assertEquals(204,customerPost(mvc,"recovery-confirm",rotated,id(924),body).getStatus());
            assertEquals(401,customerPost(mvc,"login",rotated,null,Map.of("email",CUSTOMER_EMAIL,"password",SESSION_PASSWORD)).getStatus());assertEquals(200,customerPost(mvc,"login",rotated,null,Map.of("email",CUSTOMER_EMAIL,"password","synthetic-recovered-password")).getStatus());
            assertEquals(401,httpRead(mvc,"/api/v1/customer-auth/me",customerTokens(NOW).issue(customerLogin.access())).getStatus());
            byte[] old=systemLogin.refreshToken();try(var token=sessionToken(old)){sessionDenied(SESSION_INVALID,()->sessions(NOW).refreshIdentity(token));}finally{Arrays.fill(old,(byte)0);}
            var abuse=customerCsrf(mvc,null);var unknown=Map.of("proofId",UUID.randomUUID().toString(),"proof","123456");
            for(int attempt=0;attempt<5;attempt++)assertEquals(422,customerPost(mvc,"recovery-verifications",abuse,UUID.randomUUID(),unknown).getStatus());
            var limited=customerPost(mvc,"recovery-verifications",abuse,UUID.randomUUID(),unknown);assertEquals(429,limited.getStatus());assertNotNull(limited.getHeader("Retry-After"));
        }
    }
    @Test void recoveryWrongExpiredAndReplayedProofCannotMutateCredentialOrSessions()throws Exception{
        enrollmentBinding();customerFixture(false);recoveryApp(NOW).request(recoveryRequest(CUSTOMER_EMAIL,KEY));var proof=pendingRecoveryProof();var before=snapshot();
        var wrong=new com.elogistic.backend.identity.api.customer.CustomerRecoveryCommands.Confirmation(proof.proofId(),"A".repeat(43),proof.newPassword(),proof.key(),REQUEST);
        for(var entry:List.of(Map.entry(NOW,wrong),Map.entry(NOW.plusSeconds(900),proof)))assertEquals(TOKEN_INVALID_OR_EXPIRED,assertThrows(IdentityCommandFailure.class,()->recoveryApp(entry.getKey()).confirm(entry.getValue())).code());
        assertEquals(before,snapshot());assertEquals(0,scalar("SELECT count(*) FROM identity.customer_proof_receipt"));
        recoveryApp(NOW.plusSeconds(1)).confirm(proof);var after=snapshot();recoveryApp(NOW.plusSeconds(2)).confirm(proof);assertEquals(after,snapshot());
        var replay=new com.elogistic.backend.identity.api.customer.CustomerRecoveryCommands.Confirmation(proof.proofId(),proof.proof(),proof.newPassword(),id(925),REQUEST);assertEquals(TOKEN_INVALID_OR_EXPIRED,assertThrows(IdentityCommandFailure.class,()->recoveryApp(NOW.plusSeconds(2)).confirm(replay)).code());
    }
    @Test void recoveryReplacementRevokesOnlyPreviousRecoveryAndNotPrivilegedInvitation()throws Exception{
        enrollmentBinding();customerFixture(true);execute("INSERT INTO identity.action_proof(id,account_id,purpose,token_hash,created_at,expires_at) VALUES ('"+id(926)+"','"+A+"','INVITATION',decode(repeat('26',32),'hex'),TIMESTAMPTZ '2026-10-21 00:05:00Z',TIMESTAMPTZ '2026-10-22 00:05:00Z')");
        recoveryApp(NOW).request(recoveryRequest(CUSTOMER_EMAIL,KEY));var old=pendingRecoveryProof();recoveryApp(NOW.plusSeconds(1)).request(recoveryRequest(CUSTOMER_EMAIL,id(927)));var next=pendingRecoveryProof();
        assertEquals(TOKEN_INVALID_OR_EXPIRED,assertThrows(IdentityCommandFailure.class,()->recoveryApp(NOW.plusSeconds(2)).confirm(old)).code());recoveryApp(NOW.plusSeconds(2)).confirm(next);
        assertEquals(0,scalar("SELECT count(*) FROM identity.action_proof WHERE purpose='INVITATION' AND revoked_at IS NOT NULL"));assertEquals(1,scalar("SELECT count(*) FROM identity.customer_proof WHERE purpose='CUSTOMER_RECOVERY' AND revoked_at IS NOT NULL"));
    }
    @Test void recoveryMandatoryAuditFailureRollsBackCredentialBothFamiliesAndProofReceipt()throws Exception{
        enrollmentBinding();customerFixture(true);try(var customer=customerLogin();var system=sessionLogin()){
            recoveryApp(NOW).request(recoveryRequest(CUSTOMER_EMAIL,KEY));var proof=pendingRecoveryProof();var before=snapshot();failWrite("audit_entry","INSERT");assertThrows(RuntimeException.class,()->recoveryApp(NOW.plusSeconds(1)).confirm(proof));assertEquals(before,snapshot());
            assertEquals(1,scalar("SELECT count(*) FROM identity.customer_auth_session WHERE revoked_at IS NULL"));assertEquals(0,scalar("SELECT count(*) FROM identity.customer_proof_receipt"));assertEquals(0,scalar("SELECT count(*) FROM identity.customer_proof WHERE consumed_at IS NOT NULL"));assertEquals(1,scalar("SELECT count(*) FROM identity.customer_delivery WHERE status='QUEUED' AND ciphertext IS NOT NULL"));
        }
    }
    @Test void concurrentRecoveryConfirmationHasOneSecurityVersionChangeAndOneReceipt()throws Exception{
        enrollmentBinding();customerFixture(true);recoveryApp(NOW).request(recoveryRequest(CUSTOMER_EMAIL,KEY));var proof=pendingRecoveryProof();
        var result=adminRace(()->{try{recoveryApp(NOW.plusSeconds(1)).confirm(proof);return "RESET";}catch(IdentityCommandFailure e){return e.code().name();}},()->{try{recoveryApp(NOW.plusSeconds(1)).confirm(new com.elogistic.backend.identity.api.customer.CustomerRecoveryCommands.Confirmation(proof.proofId(),proof.proof(),proof.newPassword(),id(928),REQUEST));return "RESET";}catch(IdentityCommandFailure e){return e.code().name();}});
        assertEquals(1,result.stream().filter("RESET"::equals).count());assertEquals(1,result.stream().filter(TOKEN_INVALID_OR_EXPIRED.name()::equals).count());assertEquals(2,scalar("SELECT auth_version FROM identity.user_account WHERE id='"+A+"'"));assertEquals(1,scalar("SELECT count(*) FROM identity.customer_proof_receipt"));
    }
    @Test void recoverySecurityVersionChangeOrDisableBeforeConsumptionAlwaysDenies()throws Exception{
        enrollmentBinding();customerFixture(true);recoveryApp(NOW).request(recoveryRequest(CUSTOMER_EMAIL,KEY));var proof=pendingRecoveryProof();execute("UPDATE identity.user_account SET version=version+1,auth_version=auth_version+1 WHERE id='"+A+"'");
        assertEquals(TOKEN_INVALID_OR_EXPIRED,assertThrows(IdentityCommandFailure.class,()->recoveryApp(NOW.plusSeconds(1)).confirm(proof)).code());
        execute("UPDATE identity.user_account SET status='DISABLED',disabled_at=updated_at WHERE id='"+A+"'");assertEquals(TOKEN_INVALID_OR_EXPIRED,assertThrows(IdentityCommandFailure.class,()->recoveryApp(NOW.plusSeconds(1)).confirm(proof)).code());assertEquals(0,scalar("SELECT count(*) FROM identity.customer_proof_receipt"));assertEquals(0,scalar("SELECT count(*) FROM identity.customer_proof WHERE consumed_at IS NOT NULL"));
    }
    @Test void enrollmentProofReceiptSurvivesCsrfRotationWithoutRepeatingAccountActivation()throws Exception{
        try(var inbox=new CustomerSmtpTestInbox(1,false,false,250,null);var context=customerHttpContext(NOW,smtp(inbox,NOW))){
            var mvc=httpMvc(context);var pre=customerCsrf(mvc,null);assertEquals(202,customerPost(mvc,"enrollments",pre,KEY,customerRegistration()).getStatus());assertTrue(context.getBean(com.elogistic.backend.identity.internal.ProductionCustomerComposition.Services.class).delivery().runOne());var proof=receivedVerification(inbox);
            assertEquals(204,customerPost(mvc,"verifications",pre,id(929),proof).getStatus());var after=snapshot();var next=customerCsrf(mvc,pre);assertEquals(204,customerPost(mvc,"verifications",next,id(929),proof).getStatus());assertEquals(after,snapshot());assertEquals(1,scalar("SELECT count(*) FROM identity.customer_proof_receipt"));
            assertEquals(409,customerPost(mvc,"verifications",next,id(929),Map.of("proofId",proof.get("proofId"),"proof","A".repeat(43))).getStatus());assertEquals(403,customerPost(mvc,"verifications",pre,id(929),proof).getStatus());assertEquals(4,scalar("SELECT count(*) FROM identity.user_account"));
        }
    }
    @Test void recoveryPersistenceRejectsConstructedDomainChangeWithoutActualHmacPossession()throws Exception{
        enrollmentBinding();customerFixture(true);var crypto=enrollmentProtection();recoveryApp(NOW).request(recoveryRequest(CUSTOMER_EMAIL,KEY));var proof=pendingRecoveryProof();var before=snapshot();var store=new JdbcCustomerRecoveryStore(outboxDataSource(),crypto);
        assertThrows(IllegalStateException.class,()->store.transaction(tx->{tx.accounts().lockAdministration();var account=tx.accounts().lockAccount(A).orElseThrow();tx.lockSessions(A);var stored=tx.proof(proof.proofId()).orElseThrow();var change=account.resetCustomerPassword(stored.proof(),proof.newPassword(),account.version().version(),NOW);tx.reset(account,change,crypto.hashPassword(proof.newPassword()),"A".repeat(43).getBytes(java.nio.charset.StandardCharsets.US_ASCII),NOW);return null;}));
        assertEquals(before,snapshot());assertEquals(0,scalar("SELECT count(*) FROM identity.customer_proof WHERE consumed_at IS NOT NULL"));assertEquals(0,scalar("SELECT count(*) FROM identity.customer_proof_receipt"));
    }

    private record ProfileBrowser(String access,BrowserFlow flow){}
    private ProfileBrowser profileBrowser(org.springframework.test.web.servlet.MockMvc mvc)throws Exception{
        var login=customerPost(mvc,"login",customerCsrf(mvc,null),null,Map.of("email",CUSTOMER_EMAIL,"password",SESSION_PASSWORD));assertEquals(200,login.getStatus());
        String token=new tools.jackson.databind.ObjectMapper().readTree(login.getContentAsString()).get("accessToken").asString();
        return new ProfileBrowser(token,customerCsrf(mvc,new BrowserFlow(responseCookie(login,"__Host-elogistics.customer.csrf"),responseCookie(login,"__Host-elogistics.customer.refresh"),"unused")));
    }
    private Map<String,String> profileInput(String display){return Map.of("displayName",display,"preferredLanguage","vi","businessContactEmail","business@example.invalid","businessContactPhone","synthetic contact");}
    private org.springframework.mock.web.MockHttpServletResponse profilePut(org.springframework.test.web.servlet.MockMvc mvc,ProfileBrowser browser,UUID key,String version,Object body)throws Exception{
        var request=org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put("/api/v1/customer/profile").secure(true).header("Origin","https://portal.example.invalid").header("Authorization","Bearer "+browser.access()).header("X-CSRF-Token",browser.flow().challenge).cookie(browser.flow().csrf,browser.flow().refresh).contentType("application/json");
        if(key!=null)request.header("Idempotency-Key",key);if(version!=null)request.header("If-Match",version);request.content(body instanceof String text?text:new tools.jackson.databind.ObjectMapper().writeValueAsString(body));return mvc.perform(request).andReturn().getResponse();
    }
    private Map<String,List<String>> profileSnapshot()throws Exception{
        var snapshot=new TreeMap<String,List<String>>();try(var c=db.runtimeConnection();var q=c.createStatement()){
            for(String table:List.of("customer_profile","customer_profile_version","business_audit","command_receipt"))try(var rows=q.executeQuery("SELECT row_to_json(t)::text FROM merchant."+table+" t ORDER BY row_to_json(t)::text")){var values=new ArrayList<String>();while(rows.next())values.add(rows.getString(1));snapshot.put(table,values);}
        }return snapshot;
    }
    @Test void profileHttpReadIsSelfScopedReadOnlyWithoutMembershipOrIdleExtension()throws Exception{
        customerFixture(false);try(var inbox=new CustomerSmtpTestInbox(0,false,false,250,null);var context=customerHttpContext(NOW,smtp(inbox,NOW),true)){
            var mvc=httpMvc(context);var browser=profileBrowser(mvc);var identity=secureSnapshot();var merchant=profileSnapshot();var response=httpRead(mvc,"/api/v1/customer/profile",browser.access());assertEquals(200,response.getStatus());assertEquals("\"v1\"",response.getHeader("ETag"));var json=new tools.jackson.databind.ObjectMapper().readTree(response.getContentAsString());assertEquals(5,json.size());assertEquals(A.toString(),json.get("accountId").asString());assertFalse(json.has("businessContactEmail"));assertEquals(identity,secureSnapshot());assertEquals(merchant,profileSnapshot());
        }
    }
    @Test void profileHttpUpdatePreservesIdentityAndReturnsOriginalVersionOnReceiptReplay()throws Exception{
        customerFixture(true);try(var inbox=new CustomerSmtpTestInbox(0,false,false,250,null);var context=customerHttpContext(NOW,smtp(inbox,NOW),true)){
            var mvc=httpMvc(context);var browser=profileBrowser(mvc);var identity=snapshot();var first=profilePut(mvc,browser,KEY,"\"v1\"",profileInput("First"));assertEquals(200,first.getStatus());assertEquals("\"v2\"",first.getHeader("ETag"));
            assertEquals(200,profilePut(mvc,browser,id(951),"\"v2\"",profileInput("Second")).getStatus());var saved=profileSnapshot();var replay=profilePut(mvc,browser,KEY,"\"v1\"",profileInput("First"));assertEquals(200,replay.getStatus());assertEquals(first.getContentAsString(),replay.getContentAsString());assertEquals(first.getHeader("ETag"),replay.getHeader("ETag"));assertEquals(saved,profileSnapshot());assertEquals(identity,snapshot());assertEquals(2,scalar("SELECT count(*) FROM merchant.business_audit"));assertEquals(2,scalar("SELECT count(*) FROM merchant.customer_profile_version"));
            assertEquals(409,profilePut(mvc,browser,KEY,"\"v1\"",profileInput("Changed")).getStatus());assertEquals(412,profilePut(mvc,browser,id(952),"\"v1\"",profileInput("Stale")).getStatus());assertEquals(saved,profileSnapshot());
        }
    }
    @Test void profileHttpRejectsActorAuthorityDuplicateFieldAndPreconditionInjection()throws Exception{
        customerFixture(false);try(var inbox=new CustomerSmtpTestInbox(0,false,false,250,null);var context=customerHttpContext(NOW,smtp(inbox,NOW),true)){
            var mvc=httpMvc(context);var browser=profileBrowser(mvc);var before=profileSnapshot();
            for(String field:List.of("accountId","organizationId","membershipId","emailVerified","status","roles","version")){var body=new HashMap<String,Object>(profileInput("Name"));body.put(field,A.toString());assertEquals(400,profilePut(mvc,browser,UUID.randomUUID(),"\"v1\"",body).getStatus());}
            assertEquals(400,profilePut(mvc,browser,KEY,"\"v1\"","{\"displayName\":\"First\",\"displayName\":\"Second\",\"preferredLanguage\":\"vi\"}").getStatus());assertEquals(400,profilePut(mvc,browser,KEY,"W/\"v1\"",profileInput("Name")).getStatus());assertEquals(428,profilePut(mvc,browser,KEY,null,profileInput("Name")).getStatus());assertEquals(400,profilePut(mvc,browser,null,"\"v1\"",profileInput("Name")).getStatus());assertEquals(422,profilePut(mvc,browser,KEY,"\"v1\"",profileInput(" ")).getStatus());assertEquals(before,profileSnapshot());
        }
    }
    @Test void profileAuditFailureRollsBackAggregateHistoryReceiptAndActivity()throws Exception{
        customerFixture(false);try(var inbox=new CustomerSmtpTestInbox(0,false,false,250,null);var context=customerHttpContext(NOW,smtp(inbox,NOW),true)){
            var mvc=httpMvc(context);var browser=profileBrowser(mvc);var identity=secureSnapshot();var before=profileSnapshot();db.ownerExecute("CREATE FUNCTION merchant.synthetic_profile_fault() RETURNS trigger LANGUAGE plpgsql AS $$ BEGIN RAISE EXCEPTION 'SYNTHETIC_PROFILE_AUDIT_FAILURE'; END $$");db.ownerExecute("CREATE TRIGGER synthetic_profile_fault BEFORE INSERT ON merchant.business_audit FOR EACH ROW EXECUTE FUNCTION merchant.synthetic_profile_fault()");
            var rejected=profilePut(mvc,browser,KEY,"\"v1\"",profileInput("Private"));assertEquals(503,rejected.getStatus());assertFalse(rejected.getContentAsString().contains("SYNTHETIC_PROFILE_AUDIT_FAILURE"));assertEquals(before,profileSnapshot());assertEquals(identity,secureSnapshot());
        }
    }
    @Test void concurrentProfileFirstUpdateHasOneCommittedVersionAndStaleLoser()throws Exception{
        customerFixture(false);try(var inbox=new CustomerSmtpTestInbox(0,false,false,250,null);var context=customerHttpContext(NOW,smtp(inbox,NOW),true)){
            var mvc=httpMvc(context);var browser=profileBrowser(mvc);var outcomes=adminRace(()->profilePut(mvc,browser,KEY,"\"v1\"",profileInput("Left")).getStatus(),()->profilePut(mvc,browser,id(953),"\"v1\"",profileInput("Right")).getStatus());assertEquals(1,outcomes.stream().filter(Integer.valueOf(200)::equals).count());assertEquals(1,outcomes.stream().filter(Integer.valueOf(412)::equals).count());assertEquals(1,scalar("SELECT count(*) FROM merchant.customer_profile"));assertEquals(1,scalar("SELECT count(*) FROM merchant.customer_profile_version"));assertEquals(1,scalar("SELECT count(*) FROM merchant.business_audit"));assertEquals(1,scalar("SELECT count(*) FROM merchant.command_receipt"));
        }
    }
    @Test void revokedCustomerAndSystemOnlyPrincipalCannotReadOrChangePersonalProfile()throws Exception{
        customerFixture(true);try(var inbox=new CustomerSmtpTestInbox(0,false,false,250,null);var context=customerHttpContext(NOW,smtp(inbox,NOW),true)){
            var mvc=httpMvc(context);var browser=profileBrowser(mvc);String system=httpToken(context,S,A,1,NOW,NOW.plusSeconds(900));assertEquals(401,httpRead(mvc,"/api/v1/customer/profile",system).getStatus());execute("UPDATE identity.customer_auth_session SET revoked_at=created_at WHERE account_id='"+A+"'");var before=profileSnapshot();assertEquals(401,httpRead(mvc,"/api/v1/customer/profile",browser.access()).getStatus());assertEquals(401,profilePut(mvc,browser,KEY,"\"v1\"",profileInput("Denied")).getStatus());assertEquals(before,profileSnapshot());
        }
    }

    @Test void profileReceiptLookupIsCurrentSelfScopedRedactedAndNeverExtendsIdle()throws Exception{
        customerFixture(true);try(var inbox=new CustomerSmtpTestInbox(0,false,false,250,null);var context=customerHttpContext(NOW,smtp(inbox,NOW),true)){
            var mvc=httpMvc(context);var browser=profileBrowser(mvc);String route="/api/v1/onboarding-receipts/updateCustomerProfile/"+KEY;assertEquals(404,httpRead(mvc,route,browser.access()).getStatus());assertEquals(200,profilePut(mvc,browser,KEY,"\"v1\"",profileInput("Private")).getStatus());var identity=secureSnapshot();var merchant=profileSnapshot();var response=httpRead(mvc,route,browser.access());assertEquals(200,response.getStatus());var json=new tools.jackson.databind.ObjectMapper().readTree(response.getContentAsString());assertEquals(4,json.size());assertEquals("updateCustomerProfile",json.get("operationId").asString());assertEquals(A.toString(),json.get("targetId").asString());assertEquals("COMMITTED",json.get("outcome").asString());assertEquals(2,json.get("version").asLong());assertFalse(response.getContentAsString().contains("Private"));assertFalse(response.getContentAsString().contains("business@"));assertEquals(identity,secureSnapshot());assertEquals(merchant,profileSnapshot());
            execute("UPDATE identity.user_account SET password_hash=(SELECT password_hash FROM identity.user_account WHERE id='"+A+"'),email='other-customer@example.invalid' WHERE id='"+T+"'");execute("INSERT INTO identity.customer_identifier VALUES ('"+id(954)+"','"+T+"','other-customer@example.invalid',TIMESTAMPTZ '2026-10-21 00:00:00Z','ACTIVE',1)");
            var other=customerPost(mvc,"login",customerCsrf(mvc,null),null,Map.of("email","other-customer@example.invalid","password",SESSION_PASSWORD));assertEquals(200,other.getStatus());String otherAccess=new tools.jackson.databind.ObjectMapper().readTree(other.getContentAsString()).get("accessToken").asString();assertEquals(404,httpRead(mvc,route,otherAccess).getStatus());assertEquals(404,httpRead(mvc,"/api/v1/onboarding-receipts/unknownOperation/"+KEY,browser.access()).getStatus());assertEquals(400,httpRead(mvc,"/api/v1/onboarding-receipts/updateCustomerProfile/not-a-uuid",browser.access()).getStatus());
            execute("UPDATE identity.customer_auth_session SET revoked_at=created_at WHERE account_id='"+A+"'");assertEquals(401,httpRead(mvc,route,browser.access()).getStatus());
        }
    }

    @Test void staffAuthorityBrowserUsesActualHttpsPostgresAndSmtp()throws Exception{
        ensureStaffOperator();long before=scalar("SELECT count(*) FROM identity.user_account");
        S003CustomerBrowserRuntime.runStaff(outboxDataSource(),staffOperatorSource);
        assertEquals(before+1,scalar("SELECT count(*) FROM identity.user_account"));
        assertEquals(1,scalar("SELECT count(*) FROM identity.local_staff_principal WHERE state='ACTIVE'"));
        assertEquals(0,scalar("SELECT count(*) FROM orders.support_staff_grant"));assertEquals(0,scalar("SELECT count(*) FROM orders.organization_support_staff_scope"));
        assertEquals(1,scalar("SELECT count(*) FROM identity.command_receipt WHERE operation='acceptStaffInvitation'"));
    }
    private Map<String,List<String>> systemSessionSnapshot()throws Exception{
        var result=new TreeMap<String,List<String>>();
        for(String table:List.of("auth_session","refresh_token"))try(var c=db.runtimeConnection();var q=c.createStatement();var r=q.executeQuery("SELECT row_to_json(t)::text FROM identity."+table+" t WHERE "+(table.equals("auth_session")?"t.audience='SYSTEM'":"EXISTS(SELECT 1 FROM identity.auth_session s WHERE s.id=t.session_id AND s.audience='SYSTEM')")+" ORDER BY row_to_json(t)::text")){var rows=new ArrayList<String>();while(r.next())rows.add(r.getString(1));result.put(table,rows);}
        return result;
    }
    @Test void customerEntryBrowserUsesActualHttpsPostgresAndSmtp()throws Exception{
        // Explicit controlled test-container Staff fixture only, never migration/local runtime bootstrap.
        sessionFixture();execute("UPDATE identity.user_account SET email='browser-staff@example.invalid',password_hash=(SELECT password_hash FROM identity.user_account WHERE id='"+A+"') WHERE id='"+B+"'");execute("INSERT INTO identity.customer_identifier VALUES ('"+UUID.randomUUID()+"','"+B+"','browser-staff@example.invalid',TIMESTAMPTZ '2026-10-21 00:00:00Z','ACTIVE',1)");db.ownerExecute("INSERT INTO dispatch.pilot_station_assignment(id,account_id,station_code,permission,state,effective_from,version) VALUES ('"+UUID.randomUUID()+"','"+B+"','SIM-DROP-A1','STATION_INTAKE','ACTIVE','2026-11-01T00:00:00Z',1)");
        supportGrant(staffFixtureId(),"SUPPORT_TRIAGE");supportGrant(staffFixtureId(),"SUPPORT_HANDLE");
        businessCommand(UUID.randomUUID(),"PROVISION",id(901),B,"OWNER","'dashboard.read','dashboard.finance.read','dashboard.staff.read','shipment.draft.read','shipment.draft.write','shipment.import','shipment.quote','shipment.book','shipment.label.read','shipment.export','shipment.export.pii','shipment.assign','shipment.reports.read','shipment.finance.read'");
        businessCommand(UUID.randomUUID(),"PROVISION",id(902),B,"OWNER","'dashboard.read'");
        businessCommand(UUID.randomUUID(),"GRANT",id(901),B,"OWNER","'dashboard.read','contacts.read','contacts.write','contacts.import','contacts.export','shipment.draft.read','shipment.actions.manage','shipment.actions.respond'");
        businessTestContract(id(901));facilityGrant(id(901),B,null,"'facility.read','facility.manage','defaults.manage','shipment.draft.read','shipment.prepare','shipment.handover.read','shipment.handover.manage'");
        businessCommand(UUID.randomUUID(),"GRANT",id(901),B,"OWNER","'dashboard.read','team.read','team.invite','team.access.manage','team.membership.manage','team.audit.read'");
        businessCommand(UUID.randomUUID(),"GRANT",id(901),B,"OWNER",ORGANIZATION_SUPPORT_RIGHTS);
        for(String permission:List.of("SUPPORT_TRIAGE","SUPPORT_HANDLE"))db.ownerExecute("BEGIN;SET LOCAL argopost.operator_environment='TEST';SELECT * FROM orders.configure_local_organization_support_staff('"+UUID.randomUUID()+"','BIND','"+id(901)+"','"+staffFixtureId()+"','"+permission+"','Synthetic browser support binding');COMMIT;");
        long accounts=scalar("SELECT count(*) FROM identity.user_account");var before=snapshot();before.putAll(systemSessionSnapshot());S003CustomerBrowserRuntime.run(outboxDataSource());
        assertEquals(1,scalar("SELECT count(*) FROM orders.organization_preparation WHERE state='ARCHIVED' AND version=3"));assertEquals(9,scalar("SELECT count(*) FROM orders.organization_preparation_audit"));assertEquals(7,scalar("SELECT count(*) FROM orders.organization_preparation_receipt"));assertEquals(1,scalar("SELECT count(*) FROM orders.organization_import_job WHERE state='COMPLETED' AND created_rows=1 AND invalid_rows=1"));
        assertEquals(1,scalar("SELECT count(*) FROM orders.organization_shipment_assignment"));assertEquals(1,scalar("SELECT count(*) FROM orders.organization_shipment_assignment_event"));assertEquals(1,businessScalar("SELECT count(*) FROM orders.organization_shipment_export_audit"));assertEquals(1,scalar("SELECT count(*) FROM orders.organization_report_export_audit"));
        assertEquals(2,scalar("SELECT count(*) FROM merchant.organization_contact"));assertEquals(1,scalar("SELECT count(*) FROM merchant.organization_contact WHERE version=3 AND state='ACTIVE'"));assertEquals(1,scalar("SELECT count(*) FROM merchant.organization_contact_default WHERE contact_id IS NULL AND version=2"));assertEquals(1,scalar("SELECT count(*) FROM merchant.organization_contact_import_row WHERE contact_id IS NOT NULL"));assertEquals(1,scalar("SELECT count(*) FROM orders.organization_action_task WHERE state='COMPLETED' AND version=5"));assertEquals(5,scalar("SELECT count(*) FROM orders.organization_action_event"));assertEquals(1,scalar("SELECT count(*) FROM notification.organization_action_notice WHERE read_at IS NOT NULL"));
        assertEquals(1,scalar("SELECT count(*) FROM orders.personal_support_ticket WHERE state='CLOSED' AND version=10"));assertEquals(10,scalar("SELECT count(*) FROM orders.personal_support_message"));assertEquals(10,scalar("SELECT count(*) FROM orders.support_command_receipt"));
        assertEquals(1,scalar("SELECT count(*) FROM orders.organization_support_ticket WHERE state='CLOSED' AND version=10"));assertEquals(10,scalar("SELECT count(*) FROM orders.organization_support_message"));assertEquals(1,scalar("SELECT count(*) FROM orders.organization_support_assignment WHERE version=1"));assertEquals(13,scalar("SELECT count(*) FROM orders.organization_support_receipt"));assertEquals(13,scalar("SELECT count(*) FROM orders.organization_support_audit"));assertEquals(1,scalar("SELECT count(*) FROM orders.organization_support_notice WHERE read_at IS NOT NULL"));
        assertEquals(1,scalar("SELECT count(*) FROM dispatch.pilot_intake_acceptance"));assertEquals(2,scalar("SELECT count(*) FROM dispatch.pilot_custody"));assertEquals(1,scalar("SELECT count(*) FROM orders.pilot_tracking_event"));assertEquals(2,scalar("SELECT count(*) FROM orders.pilot_parcel_event"));
        assertEquals(3,scalar("SELECT count(*) FROM orders.pilot_shipment WHERE booking_state='CONFIRMED' AND carrier_state='NOT_ACCEPTED'"));assertEquals(5,scalar("SELECT count(*) FROM orders.pilot_parcel"));assertEquals(1,scalar("SELECT count(*) FROM finance.pilot_shipping_obligation WHERE state='UNPAID' AND amount_vnd=53000"));assertEquals(3,scalar("SELECT count(*) FROM pricing.pilot_quote WHERE consumed_by IS NOT NULL"));assertEquals(1,scalar("SELECT count(*) FROM finance.pilot_shipping_obligation WHERE owner_type='BUSINESS' AND state='UNPAID' AND amount_vnd=25000"));assertEquals(2,scalar("SELECT count(*) FROM orders.organization_booking_receipt"));
        assertEquals(1,scalar("SELECT count(*) FROM merchant.facility WHERE code='BROWSER-WH-1' AND sender_contact IS NOT NULL AND return_contact IS NOT NULL AND operating_hours IS NOT NULL"));
        assertEquals(1,scalar("SELECT count(*) FROM orders.merchant_parcel_preparation WHERE state='READY_TO_HANDOVER' AND version=5"));assertEquals(2,scalar("SELECT count(*) FROM orders.merchant_preparation_check WHERE result='OK'"));assertEquals(1,scalar("SELECT count(*) FROM orders.merchant_handover_preparation WHERE state='READY' AND version=4"));assertEquals(2,scalar("SELECT count(*) FROM orders.merchant_handover_check WHERE result='PRESENT'"));assertEquals(9,scalar("SELECT count(*) FROM orders.merchant_facility_work_event"));assertEquals(9,scalar("SELECT count(*) FROM orders.merchant_facility_work_receipt"));
        // Create/edit, clear-default, default-shortcut, archive: five versions; next sender adds one.
        assertEquals(1,scalar("SELECT count(*) FROM merchant.customer_address WHERE archived_at IS NOT NULL AND NOT is_default AND version=5"));assertEquals(6,scalar("SELECT count(*) FROM merchant.customer_address_version"));assertEquals(6,scalar("SELECT count(*) FROM merchant.command_receipt WHERE target_type='CUSTOMER_ADDRESS'"));assertEquals(0,scalar("SELECT count(*) FROM merchant.customer_address WHERE is_default AND archived_at IS NULL"));
        assertEquals(accounts+1,scalar("SELECT count(*) FROM identity.user_account"));assertEquals(1,scalar("SELECT count(*) FROM identity.user_account WHERE email='"+S003CustomerBrowserRuntime.EMAIL+"' AND status='ACTIVE' AND email_verified AND auth_version=2 AND version=2"));
        assertEquals(1,scalar("SELECT count(*) FROM identity.customer_identifier WHERE exact_email='"+S003CustomerBrowserRuntime.EMAIL+"' AND status='ACTIVE'"));assertEquals(0,scalar("SELECT count(*) FROM identity.account_grant WHERE account_id IN (SELECT id FROM identity.user_account WHERE email='"+S003CustomerBrowserRuntime.EMAIL+"')"));
        assertEquals(1,scalar("SELECT count(*) FROM merchant.customer_profile WHERE version=2"));assertEquals(1,scalar("SELECT count(*) FROM merchant.customer_profile_version"));
        assertEquals(1,scalar("SELECT count(*) FROM merchant.business_audit WHERE target_type='CUSTOMER_PROFILE' AND operation='updateCustomerProfile'"));assertEquals(1,scalar("SELECT count(*) FROM merchant.command_receipt WHERE target_type='CUSTOMER_PROFILE' AND operation='updateCustomerProfile'"));
        assertEquals(1,scalar("SELECT count(*) FROM merchant.application WHERE state='DRAFT' AND version=2"));assertEquals(2,scalar("SELECT count(*) FROM merchant.application_draft_version"));
        for(String operation:List.of("createApplicationDraft","updateApplicationDraft")){
            assertEquals(1,scalar("SELECT count(*) FROM merchant.business_audit WHERE target_type='APPLICATION' AND operation='"+operation+"'"));
            assertEquals(1,scalar("SELECT count(*) FROM merchant.command_receipt WHERE target_type='APPLICATION' AND operation='"+operation+"'"));
        }
        assertEquals(2,scalar("SELECT count(*) FROM identity.customer_proof WHERE consumed_at IS NOT NULL AND purpose IN ('CUSTOMER_ENROLLMENT','CUSTOMER_RECOVERY')"));
        assertEquals(1,scalar("SELECT count(*) FROM identity.customer_proof WHERE consumed_at IS NOT NULL AND purpose='MERCHANT_MEMBERSHIP'"));
        assertEquals(3,scalar("SELECT count(*) FROM identity.customer_proof WHERE consumed_at IS NOT NULL AND purpose='REAUTHENTICATION'"));
        assertEquals(2,scalar("SELECT count(*) FROM identity.customer_delivery WHERE status='DELIVERED' AND delivery_evidence IS NOT NULL AND purpose IN ('CUSTOMER_ENROLLMENT','CUSTOMER_RECOVERY')"));
        assertEquals(1,scalar("SELECT count(*) FROM identity.customer_delivery WHERE status='DELIVERED' AND delivery_evidence IS NOT NULL AND purpose='MERCHANT_MEMBERSHIP'"));
        assertEquals(1,scalar("SELECT count(*) FROM merchant.team_invitation WHERE state='ACCEPTED'"));
        assertEquals(1,scalar("SELECT count(*) FROM merchant.membership WHERE organization_id='"+id(901)+"' AND role='OPERATOR' AND state='REVOKED'"));
        assertEquals(0,scalar("SELECT count(*) FROM identity.customer_auth_session WHERE revoked_at IS NULL"));
        var after=snapshot();after.putAll(systemSessionSnapshot());for(String table:List.of("auth_session","refresh_token","account_grant"))assertEquals(before.get(table),after.get(table));assertTrue(after.get("user_account").containsAll(before.get("user_account")));assertEquals(0,scalar("SELECT count(*) FROM identity.auth_session WHERE audience='STAFF' AND revoked_at IS NULL"));
    }
    private org.springframework.mock.web.MockHttpServletResponse reauthenticationPost(org.springframework.test.web.servlet.MockMvc mvc,ProfileBrowser browser,UUID key,Object body)throws Exception{
        var q=org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/v1/customer-auth/reauthentication").secure(true).header("Origin","https://portal.example.invalid").header("Authorization","Bearer "+browser.access()).header("X-CSRF-Token",browser.flow().challenge).cookie(browser.flow().csrf,browser.flow().refresh).contentType("application/json");
        if(key!=null)q.header("Idempotency-Key",key);q.content(body instanceof String text?text:new tools.jackson.databind.ObjectMapper().writeValueAsString(body));return mvc.perform(q).andReturn().getResponse();
    }
    @Test void reauthenticationHttpBindsCurrentSelfAndReplaysOnlyOriginalSafeOutcome()throws Exception{
        customerFixture(true);try(var inbox=new CustomerSmtpTestInbox(0,false,false,250,null);var context=customerHttpContext(NOW,smtp(inbox,NOW))){
            var mvc=httpMvc(context);var browser=profileBrowser(mvc);var before=snapshot();var body=Map.of("email",CUSTOMER_EMAIL,"password",SESSION_PASSWORD);var first=reauthenticationPost(mvc,browser,KEY,body);assertEquals(200,first.getStatus());var json=new tools.jackson.databind.ObjectMapper().readTree(first.getContentAsString());assertEquals(2,json.size());assertEquals(NOW.plusSeconds(900),Instant.parse(json.get("expiresAt").asString()));assertEquals("no-store",first.getHeader("Cache-Control"));assertFalse(first.getContentAsString().contains(SESSION_PASSWORD));
            assertEquals(1,scalar("SELECT count(*) FROM identity.customer_proof WHERE purpose='REAUTHENTICATION' AND subject_id='"+A+"' AND bound_auth_version=1 AND verifier IS NULL AND verifier_key_id IS NULL AND expires_at=created_at+INTERVAL '15 minutes'"));assertEquals(1,scalar("SELECT count(*) FROM identity.customer_security_receipt"));
            var retry=reauthenticationPost(mvc,browser,KEY,body);assertEquals(200,retry.getStatus());assertEquals(first.getContentAsString(),retry.getContentAsString());assertEquals(1,scalar("SELECT count(*) FROM identity.audit_entry WHERE event_type='CUSTOMER_REAUTHENTICATED'"));
            assertEquals(409,reauthenticationPost(mvc,browser,KEY,Map.of("email",CUSTOMER_EMAIL,"password","changed-password")).getStatus());assertEquals(400,reauthenticationPost(mvc,browser,null,body).getStatus());assertEquals(400,reauthenticationPost(mvc,browser,id(960),Map.of("email",CUSTOMER_EMAIL,"password",SESSION_PASSWORD,"accountId",T.toString())).getStatus());
            var system=new ProfileBrowser(httpToken(context,S,A,1,NOW,NOW.plusSeconds(900)),browser.flow());assertEquals(401,reauthenticationPost(mvc,system,id(963),body).getStatus());assertEquals(401,reauthenticationPost(mvc,browser,id(964),Map.of("email","other@example.invalid","password",SESSION_PASSWORD)).getStatus());
            for(String table:List.of("auth_session","refresh_token","account_grant","user_account"))assertEquals(before.get(table),snapshot().get(table));
            execute("UPDATE identity.customer_auth_session SET revoked_at=created_at WHERE account_id='"+A+"'");assertEquals(401,reauthenticationPost(mvc,browser,KEY,body).getStatus());assertEquals(1,scalar("SELECT count(*) FROM identity.customer_security_receipt"));
        }
    }
    @Test void reauthenticationDurableQuotaAndAuditFailureCannotBeBypassedByRollback()throws Exception{
        customerFixture(false);try(var inbox=new CustomerSmtpTestInbox(0,false,false,250,null);var context=customerHttpContext(NOW,smtp(inbox,NOW))){
            var mvc=httpMvc(context);var browser=profileBrowser(mvc);failWrite("audit_entry","INSERT");
            var response=reauthenticationPost(mvc,browser,KEY,Map.of("email",CUSTOMER_EMAIL,"password",SESSION_PASSWORD));assertEquals(503,response.getStatus());assertEquals(0,scalar("SELECT count(*) FROM identity.customer_proof"));assertEquals(0,scalar("SELECT count(*) FROM identity.customer_security_receipt"));
            assertEquals(1,scalar("SELECT count(*) FROM identity.rate_limit_attempt WHERE policy='CUSTOMER_LOGIN_EMAIL' AND state='PENDING'"));
            db.ownerExecute("DROP TRIGGER synthetic_fault ON identity.audit_entry");db.ownerExecute("DROP FUNCTION identity.synthetic_fault()");
            for(int i=0;i<4;i++)assertEquals(401,reauthenticationPost(mvc,browser,UUID.randomUUID(),Map.of("email",CUSTOMER_EMAIL,"password","incorrect-password")).getStatus());
            assertEquals(429,reauthenticationPost(mvc,browser,UUID.randomUUID(),Map.of("email",CUSTOMER_EMAIL,"password",SESSION_PASSWORD)).getStatus());assertEquals(0,scalar("SELECT count(*) FROM identity.customer_proof"));assertEquals(0,scalar("SELECT count(*) FROM identity.rate_limit_attempt WHERE policy='LOGIN_ACCOUNT'"));
        }
    }
    private com.elogistic.backend.identity.internal.application.CustomerReauthentications reauthenticationConsumer(com.elogistic.backend.identity.api.customer.CustomerAdmission admission,javax.sql.DataSource source,Clock clock){
        return new com.elogistic.backend.identity.internal.application.CustomerReauthentications(admission,()->{throw new IllegalStateException("NO_REQUEST_IN_CONSUMPTION_FIXTURE");},email->{throw new IllegalStateException("NO_PASSWORD_COMMAND_IN_CONSUMPTION_FIXTURE");},new JdbcCustomerReauthenticationStore(source),enrollmentProtection(),new ProtectedSessionCredentials(),clock);
    }
    @Test void reauthenticationConsumptionIsSessionBoundSingleUseAndRollsBackWithBusinessFailure()throws Exception{
        customerFixture(false);try(var login=customerLogin()){
            var source=outboxDataSource();var clock=Clock.fixed(NOW,ZoneOffset.UTC);var admission=customerAdmission(source,login.access(),clock);var store=new JdbcCustomerReauthenticationStore(source);var app=reauthenticationConsumer(admission,source,clock);var proof=CustomerProof.issue(id(961),A,CustomerProof.Purpose.REAUTHENTICATION,NOW,login.access().sessionId(),1L,null);
            admission.execute("claim",REQUEST,Set.of(),c->{store.replace(proof,NOW);return null;});
            assertThrows(IllegalStateException.class,()->admission.execute("claim",REQUEST,Set.of(),c->{app.consume(c,proof.id());throw new IllegalStateException("SYNTHETIC_BUSINESS_FAILURE");}));assertEquals(0,scalar("SELECT count(*) FROM identity.customer_proof WHERE consumed_at IS NOT NULL"));assertEquals(0,scalar("SELECT count(*) FROM identity.audit_entry WHERE event_type='CUSTOMER_REAUTHENTICATION_CONSUMED'"));
            try(var other=customerLogin()){var otherAdmission=customerAdmission(source,other.access(),clock);var otherApp=reauthenticationConsumer(otherAdmission,source,clock);sessionDenied(TOKEN_INVALID_OR_EXPIRED,()->otherAdmission.execute("claim",REQUEST,Set.of(),c->{otherApp.consume(c,proof.id());return null;}));}
            admission.execute("claim",REQUEST,Set.of(),c->{app.consume(c,proof.id());return null;});assertEquals(1,scalar("SELECT count(*) FROM identity.customer_proof WHERE consumed_at IS NOT NULL"));sessionDenied(TOKEN_INVALID_OR_EXPIRED,()->admission.execute("claim",REQUEST,Set.of(),c->{app.consume(c,proof.id());return null;}));
        }
    }
    @Test void reauthenticationFinalProofDeadlineRollsBackConsumptionAndAudit()throws Exception{
        customerFixture(false);try(var login=customerLogin()){
            var source=outboxDataSource();var time=new java.util.concurrent.atomic.AtomicReference<>(NOW.plusSeconds(100));Clock clock=new Clock(){public ZoneId getZone(){return ZoneOffset.UTC;}public Clock withZone(ZoneId z){return this;}public Instant instant(){return time.get();}};
            var access=new com.elogistic.backend.identity.api.IdentitySessionCommands.AccessGrant(login.access().sessionId(),A,1,NOW.plusSeconds(100),NOW.plusSeconds(1000));var admission=customerAdmission(source,access,clock);var store=new JdbcCustomerReauthenticationStore(source);var app=reauthenticationConsumer(admission,source,clock);var proof=CustomerProof.issue(id(962),A,CustomerProof.Purpose.REAUTHENTICATION,NOW,access.sessionId(),1L,null);
            admission.execute("claim",REQUEST,Set.of(),c->{store.replace(proof,NOW);return null;});sessionDenied(TOKEN_INVALID_OR_EXPIRED,()->admission.execute("claim",REQUEST,Set.of(),c->{app.consume(c,proof.id());time.set(NOW.plusSeconds(900));return null;}));assertEquals(0,scalar("SELECT count(*) FROM identity.customer_proof WHERE consumed_at IS NOT NULL"));assertEquals(0,scalar("SELECT count(*) FROM identity.audit_entry WHERE event_type='CUSTOMER_REAUTHENTICATION_CONSUMED'"));
        }
    }
    @Test void concurrentReauthenticationSameKeyCreatesOneProofReceiptAndAudit()throws Exception{
        customerFixture(false);try(var inbox=new CustomerSmtpTestInbox(0,false,false,250,null);var context=customerHttpContext(NOW,smtp(inbox,NOW))){
            var mvc=httpMvc(context);var browser=profileBrowser(mvc);var body=Map.of("email",CUSTOMER_EMAIL,"password",SESSION_PASSWORD);var result=adminRace(()->reauthenticationPost(mvc,browser,KEY,body).getContentAsString(),()->reauthenticationPost(mvc,browser,KEY,body).getContentAsString());assertEquals(result.getFirst(),result.getLast());assertTrue(String.valueOf(result.getFirst()).contains("proofId"));assertEquals(1,scalar("SELECT count(*) FROM identity.customer_proof"));assertEquals(1,scalar("SELECT count(*) FROM identity.customer_security_receipt"));assertEquals(1,scalar("SELECT count(*) FROM identity.audit_entry WHERE event_type='CUSTOMER_REAUTHENTICATED'"));
        }
    }

    private final ThreadLocal<Exception> onboardingFailure=new ThreadLocal<>();
    private String safeOnboardingFailure(){var failure=onboardingFailure.get();String result="";for(Throwable cause=failure;cause!=null;cause=cause.getCause()){
        result+=cause.getClass().getSimpleName()+ (cause instanceof SQLException sql?":"+sql.getSQLState():"")+" ";
        if(cause instanceof org.postgresql.util.PSQLException pg && pg.getServerErrorMessage()!=null){var message=pg.getServerErrorMessage().getMessage();var constraint=pg.getServerErrorMessage().getConstraint();
            if(message!=null&&message.matches("[A-Z_]{3,80}"))result+=message+" ";if(constraint!=null&&constraint.matches("[a-z_]{3,100}"))result+=constraint+" ";}
    }return result;}
    private org.springframework.mock.web.MockHttpServletResponse onboardingWrite(org.springframework.test.web.servlet.MockMvc mvc,ProfileBrowser browser,String method,String path,UUID key,String version,Object body)throws Exception{
        var q=org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request(org.springframework.http.HttpMethod.valueOf(method),path).secure(true).header("Origin","https://portal.example.invalid").header("Authorization","Bearer "+browser.access()).header("X-CSRF-Token",browser.flow().challenge).cookie(browser.flow().csrf,browser.flow().refresh).contentType("application/json");
        if(key!=null)q.header("Idempotency-Key",key);if(version!=null)q.header("If-Match",version);q.content(body instanceof String text?text:new tools.jackson.databind.ObjectMapper().writeValueAsString(body));var result=mvc.perform(q).andReturn();onboardingFailure.set(result.getResolvedException());return result.getResponse();
    }
    private String responseId(org.springframework.mock.web.MockHttpServletResponse response,String name)throws Exception{return new tools.jackson.databind.ObjectMapper().readTree(response.getContentAsString()).get(name).asString();}
    private String recentProof(org.springframework.test.web.servlet.MockMvc mvc,ProfileBrowser browser)throws Exception{var r=reauthenticationPost(mvc,browser,UUID.randomUUID(),Map.of("email",CUSTOMER_EMAIL,"password",SESSION_PASSWORD));assertEquals(200,r.getStatus());return responseId(r,"proofId");}
    @Test void ownerConsentHttpConsumesOnlyOwnRecentProofAndNeverGrantsBusinessAuthority()throws Exception{
        customerFixture(true);try(var inbox=new CustomerSmtpTestInbox(0,false,false,250,null);var context=customerHttpContext(NOW,smtp(inbox,NOW),true)){
            var mvc=httpMvc(context);var browser=profileBrowser(mvc);String source=recentProof(mvc,browser);var before=snapshot();var merchant=profileSnapshot();String path="/api/v1/customer-auth/owner-recovery-consents";var body=Map.of("organizationId",X.toString(),"reauthenticationProofId",source);
            var second=profileBrowser(mvc);assertEquals(422,onboardingWrite(mvc,second,"POST",path,UUID.randomUUID(),null,body).getStatus());
            var first=onboardingWrite(mvc,browser,"POST",path,KEY,null,body);assertEquals(201,first.getStatus());var json=new tools.jackson.databind.ObjectMapper().readTree(first.getContentAsString());assertEquals(3,json.size());assertEquals(X.toString(),json.get("organizationId").asString());assertEquals(NOW.plusSeconds(900),Instant.parse(json.get("expiresAt").asString()));assertEquals(path+"/"+json.get("id").asString(),first.getHeader("Location"));
            assertEquals(first.getContentAsString(),onboardingWrite(mvc,browser,"POST",path,KEY,null,body).getContentAsString());assertEquals(422,onboardingWrite(mvc,browser,"POST",path,id(970),null,body).getStatus());assertEquals(409,onboardingWrite(mvc,browser,"POST",path,KEY,null,Map.of("organizationId",Y.toString(),"reauthenticationProofId",source)).getStatus());assertEquals(400,onboardingWrite(mvc,browser,"POST",path,id(971),null,Map.of("organizationId",X.toString(),"reauthenticationProofId",source,"targetAccountId",T.toString())).getStatus());
            assertEquals(1,scalar("SELECT count(*) FROM identity.customer_proof WHERE purpose='OWNER_RECOVERY_CONSENT' AND organization_id='"+X+"' AND subject_id='"+A+"' AND verifier IS NULL AND bound_auth_version=1"));assertEquals(1,scalar("SELECT count(*) FROM identity.customer_proof WHERE purpose='REAUTHENTICATION' AND consumed_at IS NOT NULL"));assertEquals(1,scalar("SELECT count(*) FROM identity.customer_security_receipt"));assertEquals(1,scalar("SELECT count(*) FROM identity.customer_owner_consent_receipt"));assertEquals(1,scalar("SELECT count(*) FROM identity.audit_entry WHERE event_type='CUSTOMER_OWNER_RECOVERY_CONSENT_CREATED'"));assertEquals(merchant,profileSnapshot());assertEquals(0,scalar("SELECT count(*) FROM merchant.application"));
            var after=snapshot();for(String table:List.of("user_account","account_grant","auth_session","refresh_token"))assertEquals(before.get(table),after.get(table));
        }
    }
    @Test void ownerConsentLateAuditFailureRollsBackProofConsumptionAndReceipt()throws Exception{
        customerFixture(false);try(var inbox=new CustomerSmtpTestInbox(0,false,false,250,null);var context=customerHttpContext(NOW,smtp(inbox,NOW))){
            var mvc=httpMvc(context);var browser=profileBrowser(mvc);String source=recentProof(mvc,browser);var body=Map.of("organizationId",X.toString(),"reauthenticationProofId",source);
            db.ownerExecute("CREATE FUNCTION identity.synthetic_consent_fault() RETURNS trigger LANGUAGE plpgsql AS $$ BEGIN IF NEW.event_type='CUSTOMER_OWNER_RECOVERY_CONSENT_CREATED' THEN RAISE EXCEPTION 'SYNTHETIC_LATE_CONSENT_AUDIT'; END IF; RETURN NEW; END $$");db.ownerExecute("CREATE TRIGGER synthetic_consent_fault BEFORE INSERT ON identity.audit_entry FOR EACH ROW EXECUTE FUNCTION identity.synthetic_consent_fault()");
            var failed=onboardingWrite(mvc,browser,"POST","/api/v1/customer-auth/owner-recovery-consents",KEY,null,body);assertEquals(503,failed.getStatus());assertFalse(failed.getContentAsString().contains("SYNTHETIC_LATE"));assertEquals(0,scalar("SELECT count(*) FROM identity.customer_proof WHERE consumed_at IS NOT NULL OR purpose='OWNER_RECOVERY_CONSENT'"));assertEquals(1,scalar("SELECT count(*) FROM identity.customer_security_receipt"));assertEquals(0,scalar("SELECT count(*) FROM identity.customer_owner_consent_receipt"));assertEquals(0,scalar("SELECT count(*) FROM identity.audit_entry WHERE event_type='CUSTOMER_REAUTHENTICATION_CONSUMED'"));
            db.ownerExecute("DROP TRIGGER synthetic_consent_fault ON identity.audit_entry");db.ownerExecute("DROP FUNCTION identity.synthetic_consent_fault()");assertEquals(201,onboardingWrite(mvc,browser,"POST","/api/v1/customer-auth/owner-recovery-consents",KEY,null,body).getStatus());
        }
    }
    @Test void concurrentOwnerConsentCannotConsumeTheSameRecentProofTwice()throws Exception{
        customerFixture(false);try(var inbox=new CustomerSmtpTestInbox(0,false,false,250,null);var context=customerHttpContext(NOW,smtp(inbox,NOW))){
            var mvc=httpMvc(context);var browser=profileBrowser(mvc);var body=Map.of("organizationId",X.toString(),"reauthenticationProofId",recentProof(mvc,browser));var outcomes=adminRace(()->onboardingWrite(mvc,browser,"POST","/api/v1/customer-auth/owner-recovery-consents",KEY,null,body).getStatus(),()->onboardingWrite(mvc,browser,"POST","/api/v1/customer-auth/owner-recovery-consents",id(972),null,body).getStatus());assertEquals(1,outcomes.stream().filter(Integer.valueOf(201)::equals).count());assertEquals(1,outcomes.stream().filter(Integer.valueOf(422)::equals).count());assertEquals(1,scalar("SELECT count(*) FROM identity.customer_proof WHERE purpose='OWNER_RECOVERY_CONSENT'"));assertEquals(1,scalar("SELECT count(*) FROM identity.audit_entry WHERE event_type='CUSTOMER_REAUTHENTICATION_CONSUMED'"));
        }
    }
    @Test void applicationHttpCreatesPartialDraftAndReplaysExactVersionsWithScopedPaging()throws Exception{
        customerFixture(false);try(var inbox=new CustomerSmtpTestInbox(0,false,false,250,null);var context=customerHttpContext(NOW,smtp(inbox,NOW),true)){
            var mvc=httpMvc(context);var browser=profileBrowser(mvc);var before=snapshot();String path="/api/v1/merchant-applications";var first=onboardingWrite(mvc,browser,"POST",path,KEY,null,Map.of());assertEquals(201,first.getStatus());String id=responseId(first,"id");assertEquals("\"v1\"",first.getHeader("ETag"));assertEquals(path+"/"+id,first.getHeader("Location"));assertEquals(A.toString(),responseId(first,"applicantAccountId"));assertEquals("DRAFT",responseId(first,"state"));
            var second=onboardingWrite(mvc,browser,"PUT",path+"/"+id,id(973),"\"v1\"",Map.of("registeredName","First synthetic business"));assertEquals(200,second.getStatus());assertEquals("\"v2\"",second.getHeader("ETag"));assertEquals(200,onboardingWrite(mvc,browser,"PUT",path+"/"+id,id(974),"\"v2\"",Map.of("registeredName","Latest synthetic business")).getStatus());
            assertEquals(first.getContentAsString(),onboardingWrite(mvc,browser,"POST",path,KEY,null,Map.of()).getContentAsString());assertEquals(second.getContentAsString(),onboardingWrite(mvc,browser,"PUT",path+"/"+id,id(973),"\"v1\"",Map.of("registeredName","First synthetic business")).getContentAsString());
            var receipt=httpRead(mvc,"/api/v1/onboarding-receipts/createApplicationDraft/"+KEY,browser.access());assertEquals(200,receipt.getStatus());assertEquals(id,responseId(receipt,"targetId"));assertFalse(receipt.getContentAsString().contains("synthetic business"));
            var page=httpRead(mvc,path+"?page=0&size=1&search=Latest&state=DRAFT",browser.access());assertEquals(200,page.getStatus());var json=new tools.jackson.databind.ObjectMapper().readTree(page.getContentAsString());assertEquals(4,json.size());assertEquals(1,json.get("items").size());assertFalse(json.get("hasNext").asBoolean());assertEquals(3,scalar("SELECT count(*) FROM merchant.application_draft_version"));assertEquals(3,scalar("SELECT count(*) FROM merchant.business_audit WHERE target_type='APPLICATION'"));assertEquals(3,scalar("SELECT count(*) FROM merchant.command_receipt WHERE target_type='APPLICATION'"));assertEquals(0,scalar("SELECT count(*) FROM merchant.customer_profile"));
            var after=snapshot();for(String table:List.of("user_account","account_grant","auth_session","refresh_token"))assertEquals(before.get(table),after.get(table));
        }
    }
    @Test void applicationHttpDeniesForeignTargetsInjectionAndInvalidPreconditions()throws Exception{
        customerFixture(true);try(var inbox=new CustomerSmtpTestInbox(0,false,false,250,null);var context=customerHttpContext(NOW,smtp(inbox,NOW),true)){
            var mvc=httpMvc(context);var browser=profileBrowser(mvc);String path="/api/v1/merchant-applications";var first=onboardingWrite(mvc,browser,"POST",path,KEY,null,Map.of());assertEquals(201,first.getStatus());String own=responseId(first,"id");
            var foreignBrowser=addressOtherBrowser(mvc);var foreignCreated=onboardingWrite(mvc,foreignBrowser,"POST",path,UUID.randomUUID(),null,Map.of());assertEquals(201,foreignCreated.getStatus());UUID foreignId=UUID.fromString(responseId(foreignCreated,"id"));
            for(UUID target:List.of(foreignId,Y)){assertEquals(404,httpRead(mvc,path+"/"+target,browser.access()).getStatus());assertEquals(404,onboardingWrite(mvc,browser,"PUT",path+"/"+target,UUID.randomUUID(),"\"v999\"",Map.of()).getStatus());}
            for(String field:List.of("applicantAccountId","accountId","organizationId","owner","role","state","version"))assertEquals(400,onboardingWrite(mvc,browser,"POST",path,UUID.randomUUID(),null,Map.of(field,A.toString())).getStatus());
            assertEquals(400,onboardingWrite(mvc,browser,"POST",path,UUID.randomUUID(),null,"{\"registeredName\":\"First\",\"registeredName\":\"Second\"}").getStatus());assertEquals(400,onboardingWrite(mvc,browser,"POST",path,UUID.randomUUID(),null,"{\"registeredName\":null}").getStatus());assertEquals(400,onboardingWrite(mvc,browser,"POST",path,UUID.randomUUID(),null,Map.of("registeredName",17)).getStatus());assertEquals(422,onboardingWrite(mvc,browser,"POST",path,UUID.randomUUID(),null,Map.of("registeredName"," ")).getStatus());
            assertEquals(428,onboardingWrite(mvc,browser,"PUT",path+"/"+own,UUID.randomUUID(),null,Map.of()).getStatus());assertEquals(400,onboardingWrite(mvc,browser,"PUT",path+"/"+own,UUID.randomUUID(),"W/\"v1\"",Map.of()).getStatus());assertEquals(412,onboardingWrite(mvc,browser,"PUT",path+"/"+own,UUID.randomUUID(),"\"v2\"",Map.of()).getStatus());assertEquals(400,httpRead(mvc,path+"?page=-1",browser.access()).getStatus());
            var system=new ProfileBrowser(httpToken(context,S,A,1,NOW,NOW.plusSeconds(900)),browser.flow());assertEquals(401,onboardingWrite(mvc,system,"POST",path,UUID.randomUUID(),null,Map.of()).getStatus());assertEquals(2,scalar("SELECT count(*) FROM merchant.application"));assertEquals(2,scalar("SELECT count(*) FROM merchant.business_audit WHERE target_type='APPLICATION'"));
        }
    }
    @Test void applicationBusinessKeyAndConcurrentDraftUpdatesHaveOneWinner()throws Exception{
        customerFixture(false);try(var inbox=new CustomerSmtpTestInbox(0,false,false,250,null);var context=customerHttpContext(NOW,smtp(inbox,NOW),true)){
            var mvc=httpMvc(context);var browser=profileBrowser(mvc);String path="/api/v1/merchant-applications";var body=Map.of("entityType","COMPANY","jurisdiction"," VN ","legalIdentifier","123-45","registeredName","Synthetic business");var first=onboardingWrite(mvc,browser,"POST",path,KEY,null,body);assertEquals(201,first.getStatus());String target=responseId(first,"id");
            assertEquals(409,onboardingWrite(mvc,browser,"POST",path,id(975),null,Map.of("entityType","COMPANY","jurisdiction","VN","legalIdentifier","12345")).getStatus());assertEquals(409,onboardingWrite(mvc,browser,"POST",path,KEY,null,Map.of()).getStatus());
            var outcomes=adminRace(()->onboardingWrite(mvc,browser,"PUT",path+"/"+target,id(976),"\"v1\"",Map.of("registeredName","Left")).getStatus(),()->onboardingWrite(mvc,browser,"PUT",path+"/"+target,id(977),"\"v1\"",Map.of("registeredName","Right")).getStatus());assertEquals(1,outcomes.stream().filter(Integer.valueOf(200)::equals).count());assertEquals(1,outcomes.stream().filter(Integer.valueOf(412)::equals).count());assertEquals(1,scalar("SELECT count(*) FROM merchant.application"));assertEquals(2,scalar("SELECT count(*) FROM merchant.application_draft_version"));assertEquals(2,scalar("SELECT count(*) FROM merchant.business_audit WHERE target_type='APPLICATION'"));
        }
    }
    @Test void applicationAuditFailureRollsBackDraftHistoryReceiptAndActivity()throws Exception{
        customerFixture(false);try(var inbox=new CustomerSmtpTestInbox(0,false,false,250,null);var context=customerHttpContext(NOW,smtp(inbox,NOW),true)){
            var mvc=httpMvc(context);var browser=profileBrowser(mvc);var before=secureSnapshot();db.ownerExecute("CREATE FUNCTION merchant.synthetic_application_fault() RETURNS trigger LANGUAGE plpgsql AS $$ BEGIN RAISE EXCEPTION 'SYNTHETIC_APPLICATION_AUDIT'; END $$");db.ownerExecute("CREATE TRIGGER synthetic_application_fault BEFORE INSERT ON merchant.business_audit FOR EACH ROW EXECUTE FUNCTION merchant.synthetic_application_fault()");
            var response=onboardingWrite(mvc,browser,"POST","/api/v1/merchant-applications",KEY,null,Map.of("registeredName","Private synthetic name"));assertEquals(503,response.getStatus());assertFalse(response.getContentAsString().contains("SYNTHETIC_APPLICATION_AUDIT"));assertEquals(0,scalar("SELECT count(*) FROM merchant.application"));assertEquals(0,scalar("SELECT count(*) FROM merchant.application_draft_version"));assertEquals(0,scalar("SELECT count(*) FROM merchant.command_receipt WHERE target_type='APPLICATION'"));assertEquals(before,secureSnapshot());
        }
    }
    @Test void applicationConcurrentCreationAndRevokedReceiptCannotDuplicateOrRestoreAuthority()throws Exception{
        customerFixture(false);try(var inbox=new CustomerSmtpTestInbox(0,false,false,250,null);var context=customerHttpContext(NOW,smtp(inbox,NOW),true)){
            var mvc=httpMvc(context);var browser=profileBrowser(mvc);String path="/api/v1/merchant-applications";var body=Map.of("registeredName","Single draft");var responses=adminRace(()->onboardingWrite(mvc,browser,"POST",path,KEY,null,body).getContentAsString(),()->onboardingWrite(mvc,browser,"POST",path,KEY,null,body).getContentAsString());assertEquals(responses.getFirst(),responses.getLast());assertTrue(String.valueOf(responses.getFirst()).contains("DRAFT"));assertEquals(1,scalar("SELECT count(*) FROM merchant.application"));assertEquals(1,scalar("SELECT count(*) FROM merchant.command_receipt WHERE target_type='APPLICATION'"));
            execute("UPDATE identity.customer_auth_session SET revoked_at=created_at WHERE account_id='"+A+"'");assertEquals(401,httpRead(mvc,"/api/v1/onboarding-receipts/createApplicationDraft/"+KEY,browser.access()).getStatus());assertEquals(401,onboardingWrite(mvc,browser,"POST",path,KEY,null,body).getStatus());assertEquals(1,scalar("SELECT count(*) FROM merchant.application"));
        }
    }
    private static final String ADDRESS_PATH="/api/v1/customer/addresses";
    private Map<String,Object> addressInput(String name,String kind,boolean isDefault){return Map.of("kind",kind,"label",name,"contactName","Synthetic contact","contactPhone","0901234567","address",Map.of("country","VN","administrativeArea","Synthetic area","locality","Synthetic locality","addressLine","Synthetic street"),"isDefault",isDefault);}
    private void addressOtherIdentity()throws Exception{
        execute("UPDATE identity.user_account SET email='address-other@example.invalid',password_hash=(SELECT password_hash FROM identity.user_account WHERE id='"+A+"') WHERE id='"+B+"'");
        execute("INSERT INTO identity.customer_identifier VALUES ('"+UUID.randomUUID()+"','"+B+"','address-other@example.invalid',TIMESTAMPTZ '2026-10-21 00:00:00Z','ACTIVE',1)");
    }
    private ProfileBrowser addressOtherBrowser(org.springframework.test.web.servlet.MockMvc mvc)throws Exception{addressOtherIdentity();return addressOtherLogin(mvc);}
    private ProfileBrowser addressOtherLogin(org.springframework.test.web.servlet.MockMvc mvc)throws Exception{
        var login=customerPost(mvc,"login",customerCsrf(mvc,null),null,Map.of("email","address-other@example.invalid","password",SESSION_PASSWORD));assertEquals(200,login.getStatus());
        return new ProfileBrowser(responseId(login,"accessToken"),customerCsrf(mvc,new BrowserFlow(responseCookie(login,"__Host-elogistics.customer.csrf"),responseCookie(login,"__Host-elogistics.customer.refresh"),"unused")));
    }
    private Map<String,List<String>> addressSnapshot()throws Exception{
        var result=new TreeMap<String,List<String>>();try(var c=db.runtimeConnection();var q=c.createStatement()){
            for(String table:List.of("customer_address","customer_address_version","business_audit","command_receipt"))try(var rows=q.executeQuery("SELECT row_to_json(t)::text FROM merchant."+table+" t ORDER BY row_to_json(t)::text")){var values=new ArrayList<String>();while(rows.next())values.add(rows.getString(1));result.put(table,values);}
        }return result;
    }
    @Test void addressHttpCrudPersistsOnlyUnverifiedSelfContacts()throws Exception{
        customerFixture(false);try(var inbox=new CustomerSmtpTestInbox(0,false,false,250,null);var context=customerHttpContext(NOW,smtp(inbox,NOW),true)){
            var mvc=httpMvc(context);var browser=profileBrowser(mvc);var identity=snapshot();var first=onboardingWrite(mvc,browser,"POST",ADDRESS_PATH,KEY,null,addressInput("Home","SENDER",true));assertEquals(201,first.getStatus(),this::safeOnboardingFailure);String target=responseId(first,"id");assertEquals("UNVERIFIED",responseId(first,"verificationStatus"));assertEquals("+84901234567",responseId(first,"contactPhone"));assertEquals(A.toString(),responseId(first,"accountId"));assertEquals("\"v1\"",first.getHeader("ETag"));assertEquals(ADDRESS_PATH+"/"+target,first.getHeader("Location"));
            assertEquals(first.getContentAsString(),httpRead(mvc,ADDRESS_PATH+"/"+target,browser.access()).getContentAsString());assertTrue(httpRead(mvc,ADDRESS_PATH+"?search=0901234567",browser.access()).getContentAsString().contains("Home"));assertEquals(200,onboardingWrite(mvc,browser,"PUT",ADDRESS_PATH+"/"+target,id(1101),"\"v1\"",addressInput("New home","RECIPIENT",true)).getStatus());
            assertTrue(httpRead(mvc,ADDRESS_PATH+"?kind=RECIPIENT&search=New",browser.access()).getContentAsString().contains("New home"));
            assertEquals(204,onboardingWrite(mvc,browser,"POST",ADDRESS_PATH+"/"+target+"/archive",id(1102),"\"v2\"","").getStatus());assertEquals(404,httpRead(mvc,ADDRESS_PATH+"/"+target,browser.access()).getStatus());assertEquals(0,scalar("SELECT count(*) FROM merchant.customer_address WHERE is_default"));
            assertEquals(204,onboardingWrite(mvc,browser,"POST",ADDRESS_PATH+"/"+target+"/archive",id(1102),"\"v2\"","").getStatus());var receipt=httpRead(mvc,"/api/v1/customer/address-receipts/archivePersonalAddress/"+id(1102),browser.access());assertEquals(200,receipt.getStatus());assertEquals(target,responseId(receipt,"targetId"));assertFalse(receipt.getContentAsString().contains("Synthetic"));assertTrue(httpRead(mvc,ADDRESS_PATH,browser.access()).getContentAsString().contains("\"items\":[]"));
            assertEquals(identity,snapshot());assertEquals(3,scalar("SELECT count(*) FROM merchant.customer_address_version"));assertEquals(3,scalar("SELECT count(*) FROM merchant.command_receipt WHERE target_type='CUSTOMER_ADDRESS'"));assertEquals(0,scalar("SELECT count(*) FROM orders.demo_shipment"));
        }
    }
    @Test void addressHttpIsolatesForeignTargetsSearchAndReceipts()throws Exception{
        customerFixture(false);try(var inbox=new CustomerSmtpTestInbox(0,false,false,250,null);var context=customerHttpContext(NOW,smtp(inbox,NOW),true)){
            var mvc=httpMvc(context);var a=profileBrowser(mvc);var b=addressOtherBrowser(mvc);UUID otherKey=id(1103);var foreign=onboardingWrite(mvc,b,"POST",ADDRESS_PATH,otherKey,null,addressInput("Private other","SENDER",true));assertEquals(201,foreign.getStatus());String target=responseId(foreign,"id");
            for(UUID id:List.of(UUID.fromString(target),UUID.randomUUID())){assertEquals(404,httpRead(mvc,ADDRESS_PATH+"/"+id,a.access()).getStatus());assertEquals(404,onboardingWrite(mvc,a,"PUT",ADDRESS_PATH+"/"+id,UUID.randomUUID(),"\"v999\"",addressInput("Injected","SENDER",false)).getStatus());assertEquals(404,onboardingWrite(mvc,a,"POST",ADDRESS_PATH+"/"+id+"/archive",UUID.randomUUID(),"\"v999\"","").getStatus());}
            assertEquals(404,httpRead(mvc,"/api/v1/customer/address-receipts/createPersonalAddress/"+otherKey,a.access()).getStatus());assertTrue(httpRead(mvc,ADDRESS_PATH+"?search=0901234567",a.access()).getContentAsString().contains("\"items\":[]"));assertTrue(httpRead(mvc,ADDRESS_PATH+"?search=Private&page=0&size=20",a.access()).getContentAsString().contains("\"items\":[]"));
            var own=onboardingWrite(mvc,a,"POST",ADDRESS_PATH,otherKey,null,addressInput("My own","SENDER",true));assertEquals(201,own.getStatus());assertNotEquals(target,responseId(own,"id"));assertEquals(1,scalar("SELECT count(*) FROM merchant.customer_address WHERE account_id='"+A+"' AND is_default"));assertEquals(1,scalar("SELECT count(*) FROM merchant.customer_address WHERE account_id='"+B+"' AND is_default"));
        }
    }
    @Test void addressHttpRejectsInvalidSchemaPreconditionsAndCredentialInjection()throws Exception{
        customerFixture(false);try(var inbox=new CustomerSmtpTestInbox(0,false,false,250,null);var context=customerHttpContext(NOW,smtp(inbox,NOW),true)){
            var mvc=httpMvc(context);var browser=profileBrowser(mvc);var body=addressInput("Valid","SENDER",false);var created=onboardingWrite(mvc,browser,"POST",ADDRESS_PATH,KEY,null,body);String target=responseId(created,"id");
            for(String field:List.of("accountId","merchantId","role","verificationStatus","coordinates","version")){var injected=new HashMap<String,Object>(body);injected.put(field,A.toString());assertEquals(400,onboardingWrite(mvc,browser,"POST",ADDRESS_PATH,UUID.randomUUID(),null,injected).getStatus());}
            for(Object invalid:List.of(Map.of(),"{\"kind\":\"SENDER\",\"kind\":\"RECIPIENT\"}",Map.of("kind","SENDER","isDefault","true"))){assertEquals(400,onboardingWrite(mvc,browser,"POST",ADDRESS_PATH,UUID.randomUUID(),null,invalid).getStatus());}
            for(String phone:List.of("123","+1 901234567","0901234567\n")){var invalid=new HashMap<String,Object>(body);invalid.put("contactPhone",phone);assertEquals(400,onboardingWrite(mvc,browser,"POST",ADDRESS_PATH,UUID.randomUUID(),null,invalid).getStatus());}
            assertEquals(428,onboardingWrite(mvc,browser,"PUT",ADDRESS_PATH+"/"+target,UUID.randomUUID(),null,body).getStatus());assertEquals(400,onboardingWrite(mvc,browser,"PUT",ADDRESS_PATH+"/"+target,UUID.randomUUID(),"W/\"v1\"",body).getStatus());assertEquals(409,onboardingWrite(mvc,browser,"PUT",ADDRESS_PATH+"/"+target,UUID.randomUUID(),"\"v2\"",body).getStatus());assertEquals(400,onboardingWrite(mvc,browser,"POST",ADDRESS_PATH+"/"+target+"/archive",UUID.randomUUID(),"\"v1\"",Map.of()).getStatus());
            for(String query:List.of("?page=-1","?page=0&page=1","?size=100","?kind=OWNER","?accountId="+A))assertEquals(400,httpRead(mvc,ADDRESS_PATH+query,browser.access()).getStatus());
            var system=new ProfileBrowser(httpToken(context,S,A,1,NOW,NOW.plusSeconds(900)),browser.flow());assertEquals(401,httpRead(mvc,ADDRESS_PATH,system.access()).getStatus());assertEquals(401,onboardingWrite(mvc,system,"POST",ADDRESS_PATH,UUID.randomUUID(),null,body).getStatus());
            var insecure=org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(ADDRESS_PATH).secure(false).header("Authorization","Bearer "+browser.access());assertEquals(401,mvc.perform(insecure).andReturn().getResponse().getStatus());
            var noCsrf=org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(ADDRESS_PATH).secure(true).header("Authorization","Bearer "+browser.access()).header("Origin","https://portal.example.invalid").contentType("application/json").content(new tools.jackson.databind.ObjectMapper().writeValueAsString(body));assertEquals(401,mvc.perform(noCsrf).andReturn().getResponse().getStatus());assertEquals(1,scalar("SELECT count(*) FROM merchant.customer_address"));
        }
    }
    @Test void addressConcurrentDefaultsDisplaceAndInvalidateStaleForms()throws Exception{
        customerFixture(false);try(var inbox=new CustomerSmtpTestInbox(0,false,false,250,null);var context=customerHttpContext(NOW,smtp(inbox,NOW),true)){
            var mvc=httpMvc(context);var browser=profileBrowser(mvc);var results=adminRace(()->onboardingWrite(mvc,browser,"POST",ADDRESS_PATH,id(1104),null,addressInput("Left","SENDER",true)).getStatus(),()->onboardingWrite(mvc,browser,"POST",ADDRESS_PATH,id(1105),null,addressInput("Right","SENDER",true)).getStatus());assertEquals(List.of(201,201),results);
            assertEquals(1,scalar("SELECT count(*) FROM merchant.customer_address WHERE is_default AND kind='SENDER'"));assertEquals(3,scalar("SELECT count(*) FROM merchant.customer_address_version"));assertEquals(3,scalar("SELECT count(*) FROM merchant.business_audit WHERE target_type='CUSTOMER_ADDRESS'"));
            String old;try(var c=db.runtimeConnection();var q=c.createStatement();var r=q.executeQuery("SELECT id FROM merchant.customer_address WHERE NOT is_default")){assertTrue(r.next());old=r.getString(1);}
            assertEquals(409,onboardingWrite(mvc,browser,"PUT",ADDRESS_PATH+"/"+old,UUID.randomUUID(),"\"v1\"",addressInput("Stale","SENDER",true)).getStatus());assertEquals(201,onboardingWrite(mvc,browser,"POST",ADDRESS_PATH,UUID.randomUUID(),null,addressInput("Recipient","RECIPIENT",true)).getStatus());assertEquals(2,scalar("SELECT count(*) FROM merchant.customer_address WHERE is_default"));
        }
    }
    @Test void addressConcurrentUpdateAndArchiveHaveOneValidWinner()throws Exception{
        customerFixture(false);try(var inbox=new CustomerSmtpTestInbox(0,false,false,250,null);var context=customerHttpContext(NOW,smtp(inbox,NOW),true)){
            var mvc=httpMvc(context);var browser=profileBrowser(mvc);String target=responseId(onboardingWrite(mvc,browser,"POST",ADDRESS_PATH,KEY,null,addressInput("Original","SENDER",true)),"id");
            var results=adminRace(()->onboardingWrite(mvc,browser,"PUT",ADDRESS_PATH+"/"+target,id(1106),"\"v1\"",addressInput("Updated","SENDER",true)).getStatus(),()->onboardingWrite(mvc,browser,"POST",ADDRESS_PATH+"/"+target+"/archive",id(1107),"\"v1\"","").getStatus());
            assertEquals(1,results.stream().filter(value->Integer.valueOf(200).equals(value)||Integer.valueOf(204).equals(value)).count());assertEquals(1,results.stream().filter(value->Integer.valueOf(404).equals(value)||Integer.valueOf(409).equals(value)).count());assertEquals(2,scalar("SELECT count(*) FROM merchant.customer_address_version"));assertEquals(2,scalar("SELECT count(*) FROM merchant.command_receipt WHERE target_type='CUSTOMER_ADDRESS'"));assertEquals(0,scalar("SELECT count(*) FROM merchant.customer_address WHERE archived_at IS NOT NULL AND is_default"));
        }
    }
    @Test void addressIdempotencyReplaysOriginalSnapshotsAndRejectsChangedBindings()throws Exception{
        customerFixture(false);try(var inbox=new CustomerSmtpTestInbox(0,false,false,250,null);var context=customerHttpContext(NOW,smtp(inbox,NOW),true)){
            var mvc=httpMvc(context);var browser=profileBrowser(mvc);var body=addressInput("First","SENDER",false);var first=onboardingWrite(mvc,browser,"POST",ADDRESS_PATH,KEY,null,body);String target=responseId(first,"id");UUID update=id(1108);var second=onboardingWrite(mvc,browser,"PUT",ADDRESS_PATH+"/"+target,update,"\"v1\"",addressInput("Second","SENDER",false));assertEquals(200,second.getStatus());assertEquals(200,onboardingWrite(mvc,browser,"PUT",ADDRESS_PATH+"/"+target,id(1109),"\"v2\"",addressInput("Third","SENDER",false)).getStatus());var before=addressSnapshot();
            assertEquals(first.getContentAsString(),onboardingWrite(mvc,browser,"POST",ADDRESS_PATH,KEY,null,body).getContentAsString());assertEquals(second.getContentAsString(),onboardingWrite(mvc,browser,"PUT",ADDRESS_PATH+"/"+target,update,"\"v1\"",addressInput("Second","SENDER",false)).getContentAsString());assertEquals(before,addressSnapshot());
            assertEquals(409,onboardingWrite(mvc,browser,"POST",ADDRESS_PATH,KEY,null,addressInput("Changed","SENDER",false)).getStatus());assertEquals(409,onboardingWrite(mvc,browser,"PUT",ADDRESS_PATH+"/"+target,update,"\"v2\"",addressInput("Second","SENDER",false)).getStatus());
            String other=responseId(onboardingWrite(mvc,browser,"POST",ADDRESS_PATH,UUID.randomUUID(),null,body),"id");assertEquals(409,onboardingWrite(mvc,browser,"PUT",ADDRESS_PATH+"/"+other,update,"\"v1\"",addressInput("Second","SENDER",false)).getStatus());
        }
    }
    @Test void addressConcurrentSameKeyCreatesOnlyOneSourceHistoryAndReceipt()throws Exception{
        customerFixture(false);try(var inbox=new CustomerSmtpTestInbox(0,false,false,250,null);var context=customerHttpContext(NOW,smtp(inbox,NOW),true)){
            var mvc=httpMvc(context);var browser=profileBrowser(mvc);var body=addressInput("One","SENDER",true);var results=adminRace(()->onboardingWrite(mvc,browser,"POST",ADDRESS_PATH,KEY,null,body).getContentAsString(),()->onboardingWrite(mvc,browser,"POST",ADDRESS_PATH,KEY,null,body).getContentAsString());assertEquals(results.getFirst(),results.getLast());assertEquals(1,scalar("SELECT count(*) FROM merchant.customer_address"));assertEquals(1,scalar("SELECT count(*) FROM merchant.customer_address_version"));assertEquals(1,scalar("SELECT count(*) FROM merchant.business_audit WHERE target_type='CUSTOMER_ADDRESS'"));assertEquals(1,scalar("SELECT count(*) FROM merchant.command_receipt WHERE target_type='CUSTOMER_ADDRESS'"));
        }
    }
    @Test void addressAuditAndReceiptFailureRollBackDefaultReplacement()throws Exception{
        customerFixture(false);try(var inbox=new CustomerSmtpTestInbox(0,false,false,250,null);var context=customerHttpContext(NOW,smtp(inbox,NOW),true)){
            var mvc=httpMvc(context);var browser=profileBrowser(mvc);assertEquals(201,onboardingWrite(mvc,browser,"POST",ADDRESS_PATH,KEY,null,addressInput("Original default","SENDER",true)).getStatus());
            for(String table:List.of("business_audit","command_receipt")){
                var before=addressSnapshot();db.ownerExecute("CREATE FUNCTION merchant.synthetic_address_fault() RETURNS trigger LANGUAGE plpgsql AS $$ BEGIN RAISE EXCEPTION 'SYNTHETIC_ADDRESS_EVIDENCE_FAILURE'; END $$");db.ownerExecute("CREATE TRIGGER synthetic_address_fault BEFORE INSERT ON merchant."+table+" FOR EACH ROW EXECUTE FUNCTION merchant.synthetic_address_fault()");
                var result=onboardingWrite(mvc,browser,"POST",ADDRESS_PATH,UUID.randomUUID(),null,addressInput("Replacement","SENDER",true));assertEquals(503,result.getStatus());assertFalse(result.getContentAsString().contains("SYNTHETIC_ADDRESS"));assertEquals(before,addressSnapshot());db.ownerExecute("DROP TRIGGER synthetic_address_fault ON merchant."+table);db.ownerExecute("DROP FUNCTION merchant.synthetic_address_fault()");
            }assertEquals(1,scalar("SELECT count(*) FROM merchant.customer_address WHERE is_default"));
        }
    }
    @Test void addressRevocationPreventsReadReplayReceiptAndMutation()throws Exception{
        customerFixture(false);try(var inbox=new CustomerSmtpTestInbox(0,false,false,250,null);var context=customerHttpContext(NOW,smtp(inbox,NOW),true)){
            var mvc=httpMvc(context);var browser=profileBrowser(mvc);var body=addressInput("Own","SENDER",true);String target=responseId(onboardingWrite(mvc,browser,"POST",ADDRESS_PATH,KEY,null,body),"id");var before=addressSnapshot();execute("UPDATE identity.customer_auth_session SET revoked_at=created_at WHERE account_id='"+A+"'");
            for(String path:List.of(ADDRESS_PATH,ADDRESS_PATH+"/"+target,"/api/v1/customer/address-receipts/createPersonalAddress/"+KEY))assertEquals(401,httpRead(mvc,path,browser.access()).getStatus());assertEquals(401,onboardingWrite(mvc,browser,"POST",ADDRESS_PATH,KEY,null,body).getStatus());assertEquals(401,onboardingWrite(mvc,browser,"POST",ADDRESS_PATH+"/"+target+"/archive",UUID.randomUUID(),"\"v1\"","").getStatus());assertEquals(before,addressSnapshot());
        }
    }
    @Test void addressStoreAndConstructedContextCannotBypassOwnedAdmission()throws Exception{
        customerFixture(false);var store=new com.elogistic.backend.merchant.internal.infrastructure.JdbcPersonalAddressStore(outboxDataSource());assertThrows(IllegalStateException.class,()->store.find(A,X,false));assertThrows(IllegalStateException.class,()->store.list(A,new com.elogistic.backend.merchant.api.customer.PersonalAddresses.Query(0,20,null,null)));assertThrows(IllegalStateException.class,()->store.lockOwner(A));
        try(var inbox=new CustomerSmtpTestInbox(0,false,false,250,null);var context=customerHttpContext(NOW,smtp(inbox,NOW),true)){
            var fake=new com.elogistic.backend.identity.api.customer.CustomerAdmission.Context(REQUEST,S,new com.elogistic.backend.identity.api.customer.CustomerAdmission.Subject(A,"Fake","fake@example.invalid",1,true),Map.of());var api=context.getBean(com.elogistic.backend.merchant.api.customer.PersonalAddresses.class);assertThrows(RuntimeException.class,()->api.list(fake,new com.elogistic.backend.merchant.api.customer.PersonalAddresses.Query(0,20,null,null)));assertEquals(0,scalar("SELECT count(*) FROM merchant.customer_address"));
        }
    }


    private static final String DRAFT_PATH="/api/v1/customer/shipment-drafts";
    private Map<String,Object> shippingDraftInput(String reference){
        var location=Map.of("country","VN","administrativeArea","Synthetic Area","locality","Synthetic Locality","addressLine","Synthetic preparation only");
        var contact=Map.of("name","Synthetic Contact","phone","0901234567","address",location);
        return Map.of("reference",reference,"sender",contact,"recipient",contact,"parcels",List.of(Map.of("description","Synthetic Box","quantity",1,"weightGrams",501,"lengthMm",100,"widthMm",100,"heightMm",100,"declaredValueVnd",0)));
    }
    @Test void shippingDraftCrudPersistsPreparationWithoutBookingOrPayment()throws Exception{
        customerFixture(false);try(var inbox=new CustomerSmtpTestInbox(0,false,false,250,null);var context=customerHttpContext(NOW,smtp(inbox,NOW),true)){
            var mvc=httpMvc(context);var browser=profileBrowser(mvc);var first=onboardingWrite(mvc,browser,"POST",DRAFT_PATH,KEY,null,shippingDraftInput("Draft-A"));assertEquals(201,first.getStatus());String target=responseId(first,"id");assertEquals("\"v1\"",first.getHeader("ETag"));assertTrue(first.getContentAsString().contains("+84901234567"));
            var stored=httpRead(mvc,DRAFT_PATH+"/"+target,browser.access());assertEquals(200,stored.getStatus());assertTrue(stored.getContentAsString().contains("Draft-A"));
            assertEquals(200,onboardingWrite(mvc,browser,"PUT",DRAFT_PATH+"/"+target,id(1201),"\"v1\"",shippingDraftInput("Draft-B")).getStatus());
            assertEquals(2,scalar("SELECT count(*) FROM orders.shipping_draft_version"));assertEquals(2,scalar("SELECT count(*) FROM orders.shipping_draft_audit"));assertEquals(2,scalar("SELECT count(*) FROM orders.shipping_draft_receipt"));assertEquals(1,scalar("SELECT count(*) FROM orders.shipping_draft WHERE state='DRAFT' AND owner_type='PERSONAL'"));
            assertEquals(204,onboardingWrite(mvc,browser,"POST",DRAFT_PATH+"/"+target+"/archive",id(1202),"\"v2\"","").getStatus());assertEquals(404,httpRead(mvc,DRAFT_PATH+"/"+target,browser.access()).getStatus());assertFalse(httpRead(mvc,DRAFT_PATH,browser.access()).getContentAsString().contains(target));assertEquals(3,scalar("SELECT count(*) FROM orders.shipping_draft_version"));
        }
    }
    @Test void shippingDraftIsolatesForeignReadsWritesSearchAndReceipts()throws Exception{
        customerFixture(true);try(var inbox=new CustomerSmtpTestInbox(0,false,false,250,null);var context=customerHttpContext(NOW,smtp(inbox,NOW),true)){
            var mvc=httpMvc(context);var owner=profileBrowser(mvc);String target=responseId(onboardingWrite(mvc,owner,"POST",DRAFT_PATH,KEY,null,shippingDraftInput("ForeignSecret")),"id");var other=addressOtherBrowser(mvc);
            assertEquals(404,httpRead(mvc,DRAFT_PATH+"/"+target,other.access()).getStatus());assertFalse(httpRead(mvc,DRAFT_PATH+"?search=ForeignSecret",other.access()).getContentAsString().contains(target));assertEquals(404,httpRead(mvc,"/api/v1/customer/shipment-draft-receipts/createShippingDraft/"+KEY,other.access()).getStatus());
            assertEquals(404,onboardingWrite(mvc,other,"PUT",DRAFT_PATH+"/"+target,UUID.randomUUID(),"\"v1\"",shippingDraftInput("Attack")).getStatus());assertEquals(404,onboardingWrite(mvc,other,"POST",DRAFT_PATH+"/"+target+"/archive",UUID.randomUUID(),"\"v1\"","").getStatus());assertEquals(1,scalar("SELECT count(*) FROM orders.shipping_draft_version"));
        }
    }
    @Test void shippingDraftConcurrentSameKeyReplaysAndConflictingInputIsRejected()throws Exception{
        customerFixture(false);try(var inbox=new CustomerSmtpTestInbox(0,false,false,250,null);var context=customerHttpContext(NOW,smtp(inbox,NOW),true)){
            var mvc=httpMvc(context);var browser=profileBrowser(mvc);var input=shippingDraftInput("One");var results=adminRace(()->onboardingWrite(mvc,browser,"POST",DRAFT_PATH,KEY,null,input).getContentAsString(),()->onboardingWrite(mvc,browser,"POST",DRAFT_PATH,KEY,null,input).getContentAsString());assertEquals(results.getFirst(),results.getLast());assertEquals(1,scalar("SELECT count(*) FROM orders.shipping_draft"));assertEquals(1,scalar("SELECT count(*) FROM orders.shipping_draft_receipt"));assertEquals(409,onboardingWrite(mvc,browser,"POST",DRAFT_PATH,KEY,null,shippingDraftInput("Different")).getStatus());assertEquals(200,httpRead(mvc,"/api/v1/customer/shipment-draft-receipts/createShippingDraft/"+KEY,browser.access()).getStatus());
        }
    }
    @Test void shippingDraftConcurrentUpdateArchiveHaveOneWinner()throws Exception{
        customerFixture(false);try(var inbox=new CustomerSmtpTestInbox(0,false,false,250,null);var context=customerHttpContext(NOW,smtp(inbox,NOW),true)){
            var mvc=httpMvc(context);var browser=profileBrowser(mvc);String target=responseId(onboardingWrite(mvc,browser,"POST",DRAFT_PATH,KEY,null,shippingDraftInput("Race")),"id");var results=adminRace(()->onboardingWrite(mvc,browser,"PUT",DRAFT_PATH+"/"+target,id(1203),"\"v1\"",shippingDraftInput("Update")).getStatus(),()->onboardingWrite(mvc,browser,"POST",DRAFT_PATH+"/"+target+"/archive",id(1204),"\"v1\"","").getStatus());assertEquals(1,results.stream().filter(v->Integer.valueOf(200).equals(v)||Integer.valueOf(204).equals(v)).count());assertEquals(1,results.stream().filter(v->Integer.valueOf(404).equals(v)||Integer.valueOf(409).equals(v)).count());assertEquals(2,scalar("SELECT count(*) FROM orders.shipping_draft_version"));
        }
    }
    @Test void shippingDraftMandatoryAuditReceiptFailuresRollBackAllChanges()throws Exception{
        customerFixture(false);try(var inbox=new CustomerSmtpTestInbox(0,false,false,250,null);var context=customerHttpContext(NOW,smtp(inbox,NOW),true)){
            var mvc=httpMvc(context);var browser=profileBrowser(mvc);for(String table:List.of("shipping_draft_audit","shipping_draft_receipt")){
                db.ownerExecute("CREATE FUNCTION orders.synthetic_draft_fault() RETURNS trigger LANGUAGE plpgsql AS $$ BEGIN RAISE EXCEPTION 'SYNTHETIC_DRAFT_EVIDENCE'; END $$");db.ownerExecute("CREATE TRIGGER synthetic_draft_fault BEFORE INSERT ON orders."+table+" FOR EACH ROW EXECUTE FUNCTION orders.synthetic_draft_fault()");var result=onboardingWrite(mvc,browser,"POST",DRAFT_PATH,UUID.randomUUID(),null,shippingDraftInput("Rollback"));assertEquals(503,result.getStatus());assertFalse(result.getContentAsString().contains("SYNTHETIC_DRAFT"));assertEquals(0,scalar("SELECT count(*) FROM orders.shipping_draft"));assertEquals(0,scalar("SELECT count(*) FROM orders.shipping_draft_version"));db.ownerExecute("DROP TRIGGER synthetic_draft_fault ON orders."+table);db.ownerExecute("DROP FUNCTION orders.synthetic_draft_fault()");
            }
        }
    }
    @Test void shippingDraftRejectsInjectedAuthorityInvalidMeasuresAndStaleVersions()throws Exception{
        customerFixture(false);try(var inbox=new CustomerSmtpTestInbox(0,false,false,250,null);var context=customerHttpContext(NOW,smtp(inbox,NOW),true)){
            var mvc=httpMvc(context);var browser=profileBrowser(mvc);var bad=new HashMap<String,Object>(shippingDraftInput("Invalid"));for(String field:List.of("accountId","merchantId","role","state","price","paymentStatus","cod")){bad.put(field,"ATTACK");assertEquals(400,onboardingWrite(mvc,browser,"POST",DRAFT_PATH,UUID.randomUUID(),null,bad).getStatus());bad.remove(field);}
            bad.put("parcels",List.of(Map.of("description","Bad","quantity",1,"weightGrams",0,"lengthMm",1,"widthMm",1,"heightMm",1,"declaredValueVnd",0)));assertEquals(400,onboardingWrite(mvc,browser,"POST",DRAFT_PATH,UUID.randomUUID(),null,bad).getStatus());String target=responseId(onboardingWrite(mvc,browser,"POST",DRAFT_PATH,KEY,null,shippingDraftInput("Version")),"id");assertEquals(428,onboardingWrite(mvc,browser,"PUT",DRAFT_PATH+"/"+target,UUID.randomUUID(),null,shippingDraftInput("Missing version")).getStatus());assertEquals(409,onboardingWrite(mvc,browser,"PUT",DRAFT_PATH+"/"+target,UUID.randomUUID(),"\"v2\"",shippingDraftInput("Stale")).getStatus());assertEquals(1,scalar("SELECT count(*) FROM orders.shipping_draft_version"));
        }
    }
    @Test void shippingDraftRevokedSessionAndPolicyGateCannotBeBypassed()throws Exception{
        customerFixture(false);try(var inbox=new CustomerSmtpTestInbox(0,false,false,250,null);var context=customerHttpContext(NOW,smtp(inbox,NOW),true)){
            var mvc=httpMvc(context);var browser=profileBrowser(mvc);String target=responseId(onboardingWrite(mvc,browser,"POST",DRAFT_PATH,KEY,null,shippingDraftInput("Own")),"id");var available=httpRead(mvc,"/api/v1/customer/shipping-availability",browser.access());assertEquals(200,available.getStatus());assertTrue(available.getContentAsString().contains("\"bookingEnabled\":false"));assertTrue(available.getContentAsString().contains("\"quoteEnabled\":false"));
            for(String action:List.of("confirm","quote","paid","delivered"))assertEquals(404,onboardingWrite(mvc,browser,"POST",DRAFT_PATH+"/"+target+"/"+action,UUID.randomUUID(),"\"v1\"","").getStatus());execute("UPDATE identity.customer_auth_session SET revoked_at=created_at WHERE account_id='"+A+"'");assertEquals(401,httpRead(mvc,DRAFT_PATH,browser.access()).getStatus());assertEquals(401,httpRead(mvc,"/api/v1/customer/shipment-draft-receipts/createShippingDraft/"+KEY,browser.access()).getStatus());assertEquals(401,onboardingWrite(mvc,browser,"POST",DRAFT_PATH,KEY,null,shippingDraftInput("Own")).getStatus());
        }
    }
    @Test void shippingDraftDatabaseAndRuntimeCannotBypassSnapshotOwnerHistory()throws Exception{
        customerFixture(false);try(var inbox=new CustomerSmtpTestInbox(0,false,false,250,null);var context=customerHttpContext(NOW,smtp(inbox,NOW),true)){
            var mvc=httpMvc(context);var browser=profileBrowser(mvc);String target=responseId(onboardingWrite(mvc,browser,"POST",DRAFT_PATH,KEY,null,shippingDraftInput("Guard")),"id");
            assertThrows(SQLException.class,()->execute("UPDATE orders.shipping_draft SET owner_id='"+B+"',version=2 WHERE id='"+target+"'"));assertThrows(SQLException.class,()->execute("UPDATE orders.shipping_draft_version SET payload='{}'::jsonb WHERE id='"+target+"'"));assertThrows(SQLException.class,()->execute("DELETE FROM orders.shipping_draft WHERE id='"+target+"'"));assertEquals(0,scalar("SELECT count(*) FROM orders.shipping_draft_version WHERE orders.valid_shipping_preparation('{}'::jsonb)"));assertEquals(1,scalar("SELECT count(*) FROM orders.shipping_draft_version"));
            var store=new com.elogistic.backend.orders.internal.infrastructure.JdbcShippingDraftStore(outboxDataSource());assertThrows(IllegalStateException.class,()->store.find(A,UUID.fromString(target)));
        }
    }

    private static final Instant PILOT_NOW=Instant.parse("2026-11-02T00:00:00Z");
    private Map<String,Object> pilotDraftInput(String reference){var body=new HashMap<String,Object>(shippingDraftInput(reference));body.put("parcels",List.of(Map.of("description","Synthetic one","quantity",1,"weightGrams",501,"lengthMm",100,"widthMm",100,"heightMm",100,"declaredValueVnd",0),Map.of("description","Synthetic two","quantity",1,"weightGrams",500,"lengthMm",100,"widthMm",100,"heightMm",100,"declaredValueVnd",0)));return body;}
    private Map<String,Object> pilotRoute(String origin,String destination){return Map.of("originCode",origin,"destinationCode",destination,"ordinaryGoodsConfirmed",true);}
    private org.springframework.mock.web.MockHttpServletResponse pilotQuote(org.springframework.test.web.servlet.MockMvc mvc,ProfileBrowser b,String draft,UUID key,String origin,String destination)throws Exception{return onboardingWrite(mvc,b,"POST",DRAFT_PATH+"/"+draft+"/quotes",key,"\"v1\"",pilotRoute(origin,destination));}
    private String pilotQuoteId(org.springframework.mock.web.MockHttpServletResponse r)throws Exception{assertEquals(201,r.getStatus(),this::safeOnboardingFailure);return new tools.jackson.databind.ObjectMapper().readTree(r.getContentAsString()).get("quote").get("id").asString();}
    private String pilotShipmentId(org.springframework.mock.web.MockHttpServletResponse r)throws Exception{assertEquals(201,r.getStatus(),this::safeOnboardingFailure);return new tools.jackson.databind.ObjectMapper().readTree(r.getContentAsString()).get("shipment").get("id").asString();}
    private org.springframework.mock.web.MockHttpServletResponse pilotBook(org.springframework.test.web.servlet.MockMvc mvc,ProfileBrowser b,String draft,String quote,UUID key)throws Exception{return onboardingWrite(mvc,b,"POST",DRAFT_PATH+"/"+draft+"/bookings",key,"\"v1\"",Map.of("quoteId",quote));}
    @Test void pilotQuoteUsesApprovedRoutesExactFeesAndImmutableFifteenMinuteSnapshot()throws Exception{
        customerFixture(false);try(var inbox=new CustomerSmtpTestInbox(0,false,false,250,null);var c=customerHttpContext(PILOT_NOW,smtp(inbox,PILOT_NOW),true)){
            var mvc=httpMvc(c);var b=profileBrowser(mvc);var catalog=httpRead(mvc,"/api/v1/customer/shipping-catalog",b.access());assertEquals(200,catalog.getStatus());var data=new tools.jackson.databind.ObjectMapper().readTree(catalog.getContentAsString());assertEquals(3,data.get("coverage").get("areas").size());assertEquals(4,data.get("coverage").get("routes").size());assertEquals("SIMULATION",data.get("coverage").get("provenance").asString());
            String draft=responseId(onboardingWrite(mvc,b,"POST",DRAFT_PATH,KEY,null,pilotDraftInput("Quote")),"id");UUID qkey=id(1300);var first=pilotQuote(mvc,b,draft,qkey,"SIM-A1","SIM-A2");String quote=pilotQuoteId(first);var q=new tools.jackson.databind.ObjectMapper().readTree(first.getContentAsString()).get("quote");assertEquals(53000,q.get("totalVnd").asLong());assertEquals("ARGO-ROAD-TARIFF-1",q.get("tariffVersion").asString());assertEquals("SIMULATION_NOT_TAX_INVOICE",q.get("taxTreatment").asString());assertEquals(900,Duration.between(Instant.parse(q.get("createdAt").asString()),Instant.parse(q.get("expiresAt").asString())).toSeconds());assertEquals(first.getContentAsString(),pilotQuote(mvc,b,draft,qkey,"SIM-A1","SIM-A2").getContentAsString());assertEquals(409,pilotQuote(mvc,b,draft,qkey,"SIM-A1","SIM-B1").getStatus());assertEquals(1,scalar("SELECT count(*) FROM pricing.pilot_quote"));assertEquals(0,scalar("SELECT count(*) FROM finance.pilot_shipping_obligation"));assertTrue(httpRead(mvc,"/api/v1/customer/quote-receipts/"+qkey,b.access()).getContentAsString().contains(quote));
        }
    }
    @Test void pilotQuoteRejectsUnsupportedRoutesOversizeAndNonOrdinaryGoods()throws Exception{
        customerFixture(false);try(var inbox=new CustomerSmtpTestInbox(0,false,false,250,null);var c=customerHttpContext(PILOT_NOW,smtp(inbox,PILOT_NOW),true)){
            var mvc=httpMvc(c);var b=profileBrowser(mvc);String draft=responseId(onboardingWrite(mvc,b,"POST",DRAFT_PATH,KEY,null,pilotDraftInput("Limits")),"id");for(var route:List.of(new String[]{"SIM-A1","SIM-A2"},new String[]{"SIM-A2","SIM-A1"},new String[]{"SIM-A1","SIM-B1"},new String[]{"SIM-B1","SIM-A1"}))assertEquals(201,pilotQuote(mvc,b,draft,UUID.randomUUID(),route[0],route[1]).getStatus());
            for(var route:List.of(new String[]{"SIM-A2","SIM-B1"},new String[]{"SIM-A1","SIM-A1"},new String[]{"VN","SIM-A1"}))assertEquals(409,pilotQuote(mvc,b,draft,UUID.randomUUID(),route[0],route[1]).getStatus());assertEquals(409,onboardingWrite(mvc,b,"POST",DRAFT_PATH+"/"+draft+"/quotes",UUID.randomUUID(),"\"v1\"",Map.of("originCode","SIM-A1","destinationCode","SIM-A2","ordinaryGoodsConfirmed",false)).getStatus());
            for(var parcel:List.of(Map.of("description","Heavy","quantity",1,"weightGrams",20001,"lengthMm",100,"widthMm",100,"heightMm",100,"declaredValueVnd",0),Map.of("description","Large","quantity",1,"weightGrams",500,"lengthMm",1001,"widthMm",100,"heightMm",100,"declaredValueVnd",0),Map.of("description","Girth","quantity",1,"weightGrams",500,"lengthMm",1000,"widthMm",251,"heightMm",250,"declaredValueVnd",0))){var input=new HashMap<String,Object>(shippingDraftInput("Unsupported"));input.put("parcels",List.of(parcel));String target=responseId(onboardingWrite(mvc,b,"POST",DRAFT_PATH,UUID.randomUUID(),null,input),"id");assertEquals(409,pilotQuote(mvc,b,target,UUID.randomUUID(),"SIM-A1","SIM-A2").getStatus());}assertEquals(4,scalar("SELECT count(*) FROM pricing.pilot_quote"));
        }
    }
    @Test void pilotBookingPersistsShipmentParcelsAndUnpaidObligationAtomically()throws Exception{
        customerFixture(false);try(var inbox=new CustomerSmtpTestInbox(0,false,false,250,null);var c=customerHttpContext(PILOT_NOW,smtp(inbox,PILOT_NOW),true)){
            var mvc=httpMvc(c);var b=profileBrowser(mvc);String draft=responseId(onboardingWrite(mvc,b,"POST",DRAFT_PATH,KEY,null,pilotDraftInput("Booking")),"id");String quote=pilotQuoteId(pilotQuote(mvc,b,draft,id(1301),"SIM-A1","SIM-B1"));var result=pilotBook(mvc,b,draft,quote,id(1302));String shipment=pilotShipmentId(result);var body=new tools.jackson.databind.ObjectMapper().readTree(result.getContentAsString());assertEquals("CONFIRMED",body.get("shipment").get("bookingState").asString());assertEquals("NOT_ACCEPTED",body.get("shipment").get("carrierState").asString());assertEquals("UNPAID",body.get("obligation").get("state").asString());assertEquals(81000,body.get("obligation").get("amountVnd").asLong());assertEquals("SENDER",body.get("obligation").get("payer").asString());assertEquals("CASH_ON_HANDOVER",body.get("obligation").get("method").asString());assertTrue(body.get("simulation").asBoolean());assertEquals(2,body.get("shipment").get("parcels").size());assertEquals(1,scalar("SELECT count(*) FROM orders.pilot_shipment"));assertEquals(2,scalar("SELECT count(*) FROM orders.pilot_parcel"));assertEquals(1,scalar("SELECT count(*) FROM finance.pilot_shipping_obligation WHERE state='UNPAID'"));assertEquals(1,scalar("SELECT count(*) FROM orders.shipping_draft WHERE state='BOOKED' AND version=2"));assertEquals(1,scalar("SELECT count(*) FROM orders.shipping_draft_audit WHERE operation='confirmPilotBooking'"));assertEquals(404,httpRead(mvc,DRAFT_PATH+"/"+draft,b.access()).getStatus());assertTrue(httpRead(mvc,"/api/v1/customer/shipments/"+shipment,b.access()).getContentAsString().contains("Booking"));assertTrue(httpRead(mvc,"/api/v1/customer/shipments?status=WAITING_FOR_HANDOVER&search=Booking",b.access()).getContentAsString().contains(shipment));
        }
    }
    @Test void pilotBookingIsolatesForeignQuoteShipmentReceiptAndObligation()throws Exception{
        customerFixture(false);try(var inbox=new CustomerSmtpTestInbox(0,false,false,250,null);var c=customerHttpContext(PILOT_NOW,smtp(inbox,PILOT_NOW),true)){
            var mvc=httpMvc(c);var a=profileBrowser(mvc);var b=addressOtherBrowser(mvc);String draft=responseId(onboardingWrite(mvc,a,"POST",DRAFT_PATH,KEY,null,pilotDraftInput("Private A")),"id");String quote=pilotQuoteId(pilotQuote(mvc,a,draft,id(1303),"SIM-A1","SIM-A2"));assertEquals(404,httpRead(mvc,"/api/v1/customer/shipping-quotes/"+quote,b.access()).getStatus());assertEquals(404,httpRead(mvc,"/api/v1/customer/quote-receipts/"+id(1303),b.access()).getStatus());String other=responseId(onboardingWrite(mvc,b,"POST",DRAFT_PATH,UUID.randomUUID(),null,pilotDraftInput("Own B")),"id");assertEquals(404,pilotBook(mvc,b,other,quote,UUID.randomUUID()).getStatus());assertEquals(404,pilotBook(mvc,b,draft,quote,UUID.randomUUID()).getStatus());String shipment=pilotShipmentId(pilotBook(mvc,a,draft,quote,id(1304)));assertEquals(404,httpRead(mvc,"/api/v1/customer/shipments/"+shipment,b.access()).getStatus());assertEquals(404,httpRead(mvc,"/api/v1/customer/booking-receipts/"+id(1304),b.access()).getStatus());assertFalse(httpRead(mvc,"/api/v1/customer/shipments?search=Private",b.access()).getContentAsString().contains(shipment));assertEquals(1,scalar("SELECT count(*) FROM finance.pilot_shipping_obligation"));
        }
    }
    @Test void pilotBookingSameKeyConcurrentReplaysAndChangedBindingConflicts()throws Exception{
        customerFixture(false);try(var inbox=new CustomerSmtpTestInbox(0,false,false,250,null);var c=customerHttpContext(PILOT_NOW,smtp(inbox,PILOT_NOW),true)){
            var mvc=httpMvc(c);var b=profileBrowser(mvc);String draft=responseId(onboardingWrite(mvc,b,"POST",DRAFT_PATH,KEY,null,pilotDraftInput("Race")),"id");String quote=pilotQuoteId(pilotQuote(mvc,b,draft,id(1305),"SIM-A1","SIM-A2"));UUID key=id(1306);var results=adminRace(()->pilotBook(mvc,b,draft,quote,key).getContentAsString(),()->pilotBook(mvc,b,draft,quote,key).getContentAsString());assertEquals(results.getFirst(),results.getLast());assertEquals(1,scalar("SELECT count(*) FROM orders.pilot_shipment"));assertEquals(1,scalar("SELECT count(*) FROM orders.pilot_booking_receipt"));assertEquals(1,scalar("SELECT count(*) FROM finance.pilot_shipping_obligation"));assertEquals(409,pilotBook(mvc,b,draft,UUID.randomUUID().toString(),key).getStatus());assertTrue(httpRead(mvc,"/api/v1/customer/booking-receipts/"+key,b.access()).getContentAsString().contains("COMMITTED"));
        }
    }
    @Test void pilotBookingDifferentKeysOneDraftHasOnlyOneWinner()throws Exception{
        customerFixture(false);try(var inbox=new CustomerSmtpTestInbox(0,false,false,250,null);var c=customerHttpContext(PILOT_NOW,smtp(inbox,PILOT_NOW),true)){
            var mvc=httpMvc(c);var b=profileBrowser(mvc);String draft=responseId(onboardingWrite(mvc,b,"POST",DRAFT_PATH,KEY,null,pilotDraftInput("Two quotes")),"id");String q1=pilotQuoteId(pilotQuote(mvc,b,draft,id(1307),"SIM-A1","SIM-A2")),q2=pilotQuoteId(pilotQuote(mvc,b,draft,id(1308),"SIM-A1","SIM-A2"));var statuses=adminRace(()->pilotBook(mvc,b,draft,q1,id(1309)).getStatus(),()->pilotBook(mvc,b,draft,q2,id(1310)).getStatus());assertTrue(statuses.contains(201));assertTrue(statuses.contains(409));assertEquals(1,scalar("SELECT count(*) FROM orders.pilot_shipment"));assertEquals(1,scalar("SELECT count(*) FROM pricing.pilot_quote WHERE consumed_by IS NOT NULL"));assertEquals(1,scalar("SELECT count(*) FROM finance.pilot_shipping_obligation"));
        }
    }
    @Test void pilotExpiredQuoteCannotBookButSuccessfulBookingReplaySurvivesExpiry()throws Exception{
        customerFixture(false);String expiredDraft,expiredQuote,bookedDraft,bookedQuote,ship;
        try(var inbox=new CustomerSmtpTestInbox(0,false,false,250,null);var c=customerHttpContext(PILOT_NOW,smtp(inbox,PILOT_NOW),true)){
            var mvc=httpMvc(c);var b=profileBrowser(mvc);expiredDraft=responseId(onboardingWrite(mvc,b,"POST",DRAFT_PATH,KEY,null,pilotDraftInput("Expiry")),"id");expiredQuote=pilotQuoteId(pilotQuote(mvc,b,expiredDraft,id(1311),"SIM-A1","SIM-A2"));bookedDraft=responseId(onboardingWrite(mvc,b,"POST",DRAFT_PATH,id(1312),null,pilotDraftInput("Replay")),"id");bookedQuote=pilotQuoteId(pilotQuote(mvc,b,bookedDraft,id(1313),"SIM-A1","SIM-A2"));ship=pilotShipmentId(pilotBook(mvc,b,bookedDraft,bookedQuote,id(1314)));
        }Instant later=PILOT_NOW.plusSeconds(900);try(var inbox=new CustomerSmtpTestInbox(0,false,false,250,null);var c=customerHttpContext(later,smtp(inbox,later),true)){
            var mvc=httpMvc(c);var b=profileBrowser(mvc);assertEquals(409,pilotBook(mvc,b,expiredDraft,expiredQuote,id(1315)).getStatus());assertEquals(ship,pilotShipmentId(pilotBook(mvc,b,bookedDraft,bookedQuote,id(1314))));var replay=pilotQuote(mvc,b,expiredDraft,id(1311),"SIM-A1","SIM-A2");assertEquals(expiredQuote,pilotQuoteId(replay));var body=new tools.jackson.databind.ObjectMapper().readTree(replay.getContentAsString());assertFalse(body.get("usable").asBoolean());assertEquals(PILOT_NOW.plusSeconds(900),Instant.parse(body.get("quote").get("expiresAt").asString()));assertEquals(1,scalar("SELECT count(*) FROM orders.pilot_shipment"));
        }
    }
    @Test void pilotStaleDraftRequiresNewQuoteAndBookedSnapshotsStayImmutable()throws Exception{
        customerFixture(false);try(var inbox=new CustomerSmtpTestInbox(0,false,false,250,null);var c=customerHttpContext(PILOT_NOW,smtp(inbox,PILOT_NOW),true)){
            var mvc=httpMvc(c);var b=profileBrowser(mvc);String draft=responseId(onboardingWrite(mvc,b,"POST",DRAFT_PATH,KEY,null,pilotDraftInput("Before")),"id");String quote=pilotQuoteId(pilotQuote(mvc,b,draft,id(1316),"SIM-A1","SIM-A2"));assertEquals(200,onboardingWrite(mvc,b,"PUT",DRAFT_PATH+"/"+draft,id(1317),"\"v1\"",pilotDraftInput("After")).getStatus());assertEquals(409,pilotBook(mvc,b,draft,quote,id(1318)).getStatus());assertFalse(new tools.jackson.databind.ObjectMapper().readTree(httpRead(mvc,"/api/v1/customer/shipping-quotes/"+quote,b.access()).getContentAsString()).get("usable").asBoolean());var fresh=onboardingWrite(mvc,b,"POST",DRAFT_PATH+"/"+draft+"/quotes",id(1319),"\"v2\"",pilotRoute("SIM-A1","SIM-A2"));String q=pilotQuoteId(fresh);var result=onboardingWrite(mvc,b,"POST",DRAFT_PATH+"/"+draft+"/bookings",id(1320),"\"v2\"",Map.of("quoteId",q));String shipment=pilotShipmentId(result);var before=httpRead(mvc,"/api/v1/customer/shipments/"+shipment,b.access()).getContentAsString();assertEquals(201,onboardingWrite(mvc,b,"POST",ADDRESS_PATH,UUID.randomUUID(),null,addressInput("New unrelated address","SENDER",true)).getStatus());assertEquals(before,httpRead(mvc,"/api/v1/customer/shipments/"+shipment,b.access()).getContentAsString());assertEquals(404,onboardingWrite(mvc,b,"PUT",DRAFT_PATH+"/"+draft,UUID.randomUUID(),"\"v3\"",pilotDraftInput("Cannot overwrite")).getStatus());
        }
    }
    @Test void pilotFinancialAndEvidenceFailuresRollBackEveryBookingEffect()throws Exception{
        customerFixture(false);try(var inbox=new CustomerSmtpTestInbox(0,false,false,250,null);var c=customerHttpContext(PILOT_NOW,smtp(inbox,PILOT_NOW),true)){
            var mvc=httpMvc(c);var b=profileBrowser(mvc);String draft=responseId(onboardingWrite(mvc,b,"POST",DRAFT_PATH,KEY,null,pilotDraftInput("Rollback")),"id");String quote=pilotQuoteId(pilotQuote(mvc,b,draft,id(1321),"SIM-A1","SIM-A2"));for(String table:List.of("orders.pilot_parcel","orders.shipping_draft_audit","orders.pilot_booking_receipt","finance.pilot_shipping_obligation")){
                db.ownerExecute("CREATE FUNCTION orders.synthetic_pilot_fault() RETURNS trigger LANGUAGE plpgsql AS $$ BEGIN RAISE EXCEPTION 'SYNTHETIC_PILOT_FAILURE'; END $$");db.ownerExecute("CREATE TRIGGER synthetic_pilot_fault BEFORE INSERT ON "+table+" FOR EACH ROW EXECUTE FUNCTION orders.synthetic_pilot_fault()");var result=pilotBook(mvc,b,draft,quote,UUID.randomUUID());assertEquals(503,result.getStatus());assertFalse(result.getContentAsString().contains("SYNTHETIC_PILOT"));assertEquals(0,scalar("SELECT count(*) FROM orders.pilot_shipment"));assertEquals(0,scalar("SELECT count(*) FROM orders.pilot_parcel"));assertEquals(0,scalar("SELECT count(*) FROM finance.pilot_shipping_obligation"));assertEquals(0,scalar("SELECT count(*) FROM pricing.pilot_quote WHERE consumed_by IS NOT NULL"));assertEquals(1,scalar("SELECT count(*) FROM orders.shipping_draft WHERE state='DRAFT' AND version=1"));db.ownerExecute("DROP TRIGGER synthetic_pilot_fault ON "+table);db.ownerExecute("DROP FUNCTION orders.synthetic_pilot_fault()");
            }assertEquals(201,pilotBook(mvc,b,draft,quote,UUID.randomUUID()).getStatus());
        }
    }
    @Test void pilotEnvironmentAndMissingCollectionAuthorityCannotBeBypassed()throws Exception{
        customerFixture(false);for(String mode:List.of("DISABLED","PRODUCTION"))try(var inbox=new CustomerSmtpTestInbox(0,false,false,250,null);var c=customerHttpContext(PILOT_NOW,smtp(inbox,PILOT_NOW),true,mode)){
            var mvc=httpMvc(c);var b=profileBrowser(mvc);String draft=responseId(onboardingWrite(mvc,b,"POST",DRAFT_PATH,UUID.randomUUID(),null,pilotDraftInput("Mode")),"id");assertEquals(409,pilotQuote(mvc,b,draft,UUID.randomUUID(),"SIM-A1","SIM-A2").getStatus());assertEquals(409,pilotBook(mvc,b,draft,UUID.randomUUID().toString(),UUID.randomUUID()).getStatus());assertFalse(new tools.jackson.databind.ObjectMapper().readTree(httpRead(mvc,"/api/v1/customer/shipping-availability",b.access()).getContentAsString()).get("bookingEnabled").asBoolean());
        }try(var inbox=new CustomerSmtpTestInbox(0,false,false,250,null);var c=customerHttpContext(PILOT_NOW,smtp(inbox,PILOT_NOW),true)){
            var mvc=httpMvc(c);var b=profileBrowser(mvc);String draft=responseId(onboardingWrite(mvc,b,"POST",DRAFT_PATH,KEY,null,pilotDraftInput("No authority")),"id");String quote=pilotQuoteId(pilotQuote(mvc,b,draft,id(1322),"SIM-A1","SIM-A2"));var injected=Map.of("quoteId",quote,"paid",true);assertEquals(400,onboardingWrite(mvc,b,"POST",DRAFT_PATH+"/"+draft+"/bookings",UUID.randomUUID(),"\"v1\"",injected).getStatus());String shipment=pilotShipmentId(pilotBook(mvc,b,draft,quote,id(1323)));for(String action:List.of("paid","collect","accept","delivered","pickup"))assertEquals(404,onboardingWrite(mvc,b,"POST","/api/v1/customer/shipments/"+shipment+"/"+action,UUID.randomUUID(),null,"").getStatus());assertThrows(SQLException.class,()->execute("UPDATE finance.pilot_shipping_obligation SET state='PAID'"));assertThrows(SQLException.class,()->execute("UPDATE orders.pilot_shipment SET carrier_state='DELIVERED'"));assertThrows(SQLException.class,()->execute("UPDATE pricing.pilot_tariff_version SET intra_first_vnd=1"));assertEquals(1,scalar("SELECT count(*) FROM finance.pilot_shipping_obligation WHERE state='UNPAID'"));
        }
    }
    private static final String STATION_PATH="/api/v1/station-operations/stations/";
    private tools.jackson.databind.JsonNode intakeJson(org.springframework.mock.web.MockHttpServletResponse r)throws Exception{return new tools.jackson.databind.ObjectMapper().readTree(r.getContentAsString());}
    private String intakeBooked(org.springframework.test.web.servlet.MockMvc mvc,ProfileBrowser owner)throws Exception{String d=responseId(onboardingWrite(mvc,owner,"POST",DRAFT_PATH,UUID.randomUUID(),null,pilotDraftInput("Station")),"id");return pilotShipmentId(pilotBook(mvc,owner,d,pilotQuoteId(pilotQuote(mvc,owner,d,UUID.randomUUID(),"SIM-A1","SIM-A2")),UUID.randomUUID()));}
    private void intakeAssignment(org.springframework.test.web.servlet.MockMvc mvc,ProfileBrowser actor,String station)throws Exception{String account=intakeJson(httpRead(mvc,"/api/v1/customer-auth/me",actor.access())).get("accountId").asString();db.ownerExecute("INSERT INTO dispatch.pilot_station_assignment(id,account_id,station_code,permission,state,effective_from,version) VALUES ('"+UUID.randomUUID()+"','"+account+"','"+station+"','STATION_INTAKE','ACTIVE','2026-11-01T00:00:00Z',1)");}
    private List<String> intakeBarcodes(org.springframework.test.web.servlet.MockMvc mvc,ProfileBrowser owner,String shipment)throws Exception{var r=httpRead(mvc,"/api/v1/customer/shipments/"+shipment+"/handling",owner.access());assertEquals(200,r.getStatus(),this::safeOnboardingFailure);List<String> result=new ArrayList<>();for(var p:intakeJson(r).get("parcels"))result.add(p.get("barcode").asString());return result;}
    private List<String> intakeScans(org.springframework.test.web.servlet.MockMvc mvc,ProfileBrowser staff,List<String> codes)throws Exception{List<String> ids=new ArrayList<>();for(String code:codes){var r=onboardingWrite(mvc,staff,"POST",STATION_PATH+"SIM-DROP-A1/scans",UUID.randomUUID(),null,Map.of("barcode",code));assertEquals(200,r.getStatus(),this::safeOnboardingFailure);ids.add(intakeJson(r).get("id").asString());}return ids;}
    private org.springframework.mock.web.MockHttpServletResponse intakeAccept(org.springframework.test.web.servlet.MockMvc mvc,ProfileBrowser staff,String shipment,List<String> scans,UUID key,String etag)throws Exception{return onboardingWrite(mvc,staff,"POST",STATION_PATH+"SIM-DROP-A1/shipments/"+shipment+"/acceptance",key,etag,Map.of("scanIds",scans,"inspection","MATCHED"));}
    @Test void stationLabelsArePrivateStableMultiParcelPdfAndDraftsNeverGetLabels()throws Exception{
        customerFixture(true);try(var inbox=new CustomerSmtpTestInbox(0,false,false,250,null);var c=customerHttpContext(PILOT_NOW,smtp(inbox,PILOT_NOW),true)){
            var mvc=httpMvc(c);var owner=profileBrowser(mvc);var stranger=addressOtherBrowser(mvc);String shipment=intakeBooked(mvc,owner);var codes=intakeBarcodes(mvc,owner,shipment);assertEquals(2,new HashSet<>(codes).size());var label=httpRead(mvc,"/api/v1/customer/shipments/"+shipment+"/labels.pdf",owner.access());assertEquals(200,label.getStatus());assertEquals("application/pdf",label.getContentType());assertTrue(label.getHeader("Cache-Control").contains("no-store"));try(var pdf=org.apache.pdfbox.Loader.loadPDF(label.getContentAsByteArray())){assertEquals(2,pdf.getNumberOfPages());String text=new org.apache.pdfbox.text.PDFTextStripper().getText(pdf);assertTrue(text.contains(codes.getFirst()));assertTrue(text.contains("UNPAID"));}assertEquals(codes,intakeBarcodes(mvc,owner,shipment));assertEquals(404,httpRead(mvc,"/api/v1/customer/shipments/"+shipment+"/labels.pdf",stranger.access()).getStatus());String draft=responseId(onboardingWrite(mvc,owner,"POST",DRAFT_PATH,UUID.randomUUID(),null,pilotDraftInput("NotBooked")),"id");assertEquals(404,httpRead(mvc,"/api/v1/customer/shipments/"+draft+"/labels.pdf",owner.access()).getStatus());
        }
    }
    @Test void stationAuthorizedAllParcelAcceptancePersistsCustodyTrackingAndUnpaidAtomically()throws Exception{
        customerFixture(true);try(var inbox=new CustomerSmtpTestInbox(0,false,false,250,null);var c=customerHttpContext(PILOT_NOW,smtp(inbox,PILOT_NOW),true)){
            var mvc=httpMvc(c);var owner=profileBrowser(mvc);var staff=addressOtherBrowser(mvc);intakeAssignment(mvc,staff,"SIM-DROP-A1");String shipment=intakeBooked(mvc,owner);var codes=intakeBarcodes(mvc,owner,shipment);var scans=intakeScans(mvc,staff,codes);var r=intakeAccept(mvc,staff,shipment,scans,UUID.randomUUID(),"\"i0\"");assertEquals(200,r.getStatus(),this::safeOnboardingFailure);assertEquals("AT_STATION",intakeJson(r).get("cargo").get("handling").get("state").asString());assertEquals(1,scalar("SELECT count(*) FROM dispatch.pilot_intake_acceptance"));assertEquals(2,scalar("SELECT count(*) FROM dispatch.pilot_custody"));assertEquals(1,scalar("SELECT count(*) FROM orders.pilot_tracking_event"));assertEquals(2,scalar("SELECT count(*) FROM orders.pilot_parcel_event"));assertEquals(1,scalar("SELECT count(*) FROM finance.pilot_shipping_obligation WHERE state='UNPAID'"));assertEquals(1,scalar("SELECT count(*) FROM orders.pilot_shipment WHERE carrier_state='NOT_ACCEPTED'"));var personal=httpRead(mvc,"/api/v1/customer/shipments/"+shipment+"/handling",owner.access());assertEquals(200,personal.getStatus());assertTrue(personal.getContentAsString().contains("CARRIER_ACCEPTED_AT_STATION"));assertFalse(personal.getContentAsString().contains("actorId"));assertFalse(personal.getContentAsString().contains("IN_TRANSIT"));assertEquals(200,httpRead(mvc,STATION_PATH+"SIM-DROP-A1/shipments/"+shipment+"/labels.pdf",staff.access()).getStatus());
        }
    }
    @Test void stationCustomerMissingRevokedWrongStationAndUnknownBarcodeAreDenied()throws Exception{
        customerFixture(true);try(var inbox=new CustomerSmtpTestInbox(0,false,false,250,null);var c=customerHttpContext(PILOT_NOW,smtp(inbox,PILOT_NOW),true)){
            var mvc=httpMvc(c);var owner=profileBrowser(mvc);var staff=addressOtherBrowser(mvc);String shipment=intakeBooked(mvc,owner);var codes=intakeBarcodes(mvc,owner,shipment);assertEquals(403,httpRead(mvc,STATION_PATH.substring(0,STATION_PATH.length()-1),owner.access()).getStatus());assertEquals(403,onboardingWrite(mvc,owner,"POST",STATION_PATH+"SIM-DROP-A1/scans",UUID.randomUUID(),null,Map.of("barcode",codes.getFirst())).getStatus());intakeAssignment(mvc,staff,"SIM-DROP-A2");assertEquals(403,httpRead(mvc,STATION_PATH+"SIM-DROP-A1/parcels/"+codes.getFirst(),staff.access()).getStatus());assertEquals(404,httpRead(mvc,STATION_PATH+"SIM-DROP-A2/parcels/"+codes.getFirst(),staff.access()).getStatus());assertEquals(404,httpRead(mvc,STATION_PATH+"SIM-DROP-A2/shipments/"+shipment+"/labels.pdf",staff.access()).getStatus());assertEquals(400,httpRead(mvc,STATION_PATH+"SIM-DROP-A2/parcels/invalid",staff.access()).getStatus());assertEquals(404,httpRead(mvc,STATION_PATH+"SIM-DROP-A2/parcels/AP-P-00000000000040008000000000000000",staff.access()).getStatus());db.ownerExecute("UPDATE dispatch.pilot_station_assignment SET state='REVOKED',version=version+1");assertEquals(403,httpRead(mvc,STATION_PATH+"SIM-DROP-A2/parcels/"+codes.getFirst(),staff.access()).getStatus());assertEquals(0,scalar("SELECT count(*) FROM dispatch.pilot_intake_scan"));assertEquals(0,scalar("SELECT count(*) FROM orders.pilot_tracking_event"));
        }
    }
    @Test void stationIncompleteForeignDuplicateScansMismatchAndStaleVersionCannotAccept()throws Exception{
        customerFixture(true);try(var inbox=new CustomerSmtpTestInbox(0,false,false,250,null);var c=customerHttpContext(PILOT_NOW,smtp(inbox,PILOT_NOW),true)){
            var mvc=httpMvc(c);var owner=profileBrowser(mvc);var staff=addressOtherBrowser(mvc);intakeAssignment(mvc,staff,"SIM-DROP-A1");String shipment=intakeBooked(mvc,owner);var scans=intakeScans(mvc,staff,intakeBarcodes(mvc,owner,shipment));assertEquals(409,intakeAccept(mvc,staff,shipment,List.of(scans.getFirst()),UUID.randomUUID(),"\"i0\"").getStatus());assertEquals(400,intakeAccept(mvc,staff,shipment,List.of(scans.getFirst(),scans.getFirst()),UUID.randomUUID(),"\"i0\"").getStatus());assertEquals(404,intakeAccept(mvc,staff,shipment,List.of(scans.getFirst(),UUID.randomUUID().toString()),UUID.randomUUID(),"\"i0\"").getStatus());assertEquals(409,intakeAccept(mvc,staff,shipment,scans,UUID.randomUUID(),"\"i1\"").getStatus());assertEquals(428,intakeAccept(mvc,staff,shipment,scans,UUID.randomUUID(),null).getStatus());assertEquals(409,onboardingWrite(mvc,staff,"POST",STATION_PATH+"SIM-DROP-A1/shipments/"+shipment+"/acceptance",UUID.randomUUID(),"\"i0\"",Map.of("scanIds",scans,"inspection","MISMATCH")).getStatus());assertEquals(0,scalar("SELECT count(*) FROM dispatch.pilot_custody"));assertEquals(0,scalar("SELECT count(*) FROM orders.pilot_tracking_event"));
        }
    }
    @Test void stationSameKeyConcurrentAcceptanceReplaysAndDifferentBindingConflicts()throws Exception{
        customerFixture(true);try(var inbox=new CustomerSmtpTestInbox(0,false,false,250,null);var c=customerHttpContext(PILOT_NOW,smtp(inbox,PILOT_NOW),true)){
            var mvc=httpMvc(c);var owner=profileBrowser(mvc);var staff=addressOtherBrowser(mvc);intakeAssignment(mvc,staff,"SIM-DROP-A1");String shipment=intakeBooked(mvc,owner);var codes=intakeBarcodes(mvc,owner,shipment);UUID scanKey=UUID.randomUUID();var scan=onboardingWrite(mvc,staff,"POST",STATION_PATH+"SIM-DROP-A1/scans",scanKey,null,Map.of("barcode",codes.getFirst()));assertEquals(200,scan.getStatus());assertEquals(scan.getContentAsString(),onboardingWrite(mvc,staff,"POST",STATION_PATH+"SIM-DROP-A1/scans",scanKey,null,Map.of("barcode",codes.getFirst())).getContentAsString());assertEquals(409,onboardingWrite(mvc,staff,"POST",STATION_PATH+"SIM-DROP-A1/scans",scanKey,null,Map.of("barcode",codes.getLast())).getStatus());var scans=intakeScans(mvc,staff,codes);UUID key=UUID.randomUUID();var results=adminRace(()->intakeAccept(mvc,staff,shipment,scans,key,"\"i0\"").getStatus(),()->intakeAccept(mvc,staff,shipment,scans,key,"\"i0\"").getStatus());assertEquals(List.of(200,200),results);assertEquals(1,scalar("SELECT count(*) FROM dispatch.pilot_intake_acceptance"));assertEquals(1,scalar("SELECT count(*) FROM orders.pilot_tracking_event"));assertEquals(2,scalar("SELECT count(*) FROM dispatch.pilot_intake_scan"));assertEquals(409,intakeAccept(mvc,staff,shipment,List.of(scans.getFirst()),key,"\"i0\"").getStatus());assertEquals(200,httpRead(mvc,STATION_PATH+"SIM-DROP-A1/receipts/"+key,staff.access()).getStatus());assertEquals(403,httpRead(mvc,STATION_PATH+"SIM-DROP-A1/receipts/"+key,owner.access()).getStatus());
        }
    }
    @Test void stationCustodyAuditReceiptAndTrackingFaultsRollBackAcceptanceEntirely()throws Exception{
        customerFixture(true);try(var inbox=new CustomerSmtpTestInbox(0,false,false,250,null);var c=customerHttpContext(PILOT_NOW,smtp(inbox,PILOT_NOW),true)){
            var mvc=httpMvc(c);var owner=profileBrowser(mvc);var staff=addressOtherBrowser(mvc);intakeAssignment(mvc,staff,"SIM-DROP-A1");String shipment=intakeBooked(mvc,owner);var scans=intakeScans(mvc,staff,intakeBarcodes(mvc,owner,shipment));
            for(String table:List.of("dispatch.pilot_custody","dispatch.pilot_intake_audit","dispatch.pilot_intake_receipt","orders.pilot_tracking_event","orders.pilot_parcel_event")){
                String schema=table.split("\\.")[0];db.ownerExecute("CREATE FUNCTION "+schema+".synthetic_intake_fault() RETURNS trigger LANGUAGE plpgsql AS $$ BEGIN RAISE EXCEPTION 'SYNTHETIC_INTAKE_FAULT'; END $$");db.ownerExecute("CREATE TRIGGER synthetic_intake_fault BEFORE INSERT ON "+table+" FOR EACH ROW EXECUTE FUNCTION "+schema+".synthetic_intake_fault()");var r=intakeAccept(mvc,staff,shipment,scans,UUID.randomUUID(),"\"i0\"");assertEquals(503,r.getStatus());assertFalse(r.getContentAsString().contains("SYNTHETIC_INTAKE"));for(String empty:List.of("dispatch.pilot_intake_acceptance","dispatch.pilot_custody","orders.pilot_tracking_event","orders.pilot_parcel_event"))assertEquals(0,scalar("SELECT count(*) FROM "+empty));assertEquals(2,scalar("SELECT count(*) FROM dispatch.pilot_intake_scan"));assertEquals(1,scalar("SELECT count(*) FROM finance.pilot_shipping_obligation WHERE state='UNPAID'"));db.ownerExecute("DROP TRIGGER synthetic_intake_fault ON "+table);db.ownerExecute("DROP FUNCTION "+schema+".synthetic_intake_fault()");
            }
        }
    }

    @Test void stationTwoStaffRaceHasOneAcceptanceAndEveryParcelEventExactlyOnce()throws Exception{
        customerFixture(true);try(var inbox=new CustomerSmtpTestInbox(0,false,false,250,null);var c=customerHttpContext(PILOT_NOW,smtp(inbox,PILOT_NOW),true)){
            var mvc=httpMvc(c);var first=profileBrowser(mvc);var second=addressOtherBrowser(mvc);intakeAssignment(mvc,first,"SIM-DROP-A1");intakeAssignment(mvc,second,"SIM-DROP-A1");String shipment=intakeBooked(mvc,first);var codes=intakeBarcodes(mvc,first,shipment);var scansA=intakeScans(mvc,first,codes);var scansB=intakeScans(mvc,second,codes);var results=adminRace(()->intakeAccept(mvc,first,shipment,scansA,UUID.randomUUID(),"\"i0\"").getStatus(),()->intakeAccept(mvc,second,shipment,scansB,UUID.randomUUID(),"\"i0\"").getStatus());assertEquals(1,results.stream().filter(v->Integer.valueOf(200).equals(v)).count());assertEquals(1,results.stream().filter(v->Integer.valueOf(409).equals(v)).count());assertEquals(1,scalar("SELECT count(*) FROM dispatch.pilot_intake_acceptance"));assertEquals(2,scalar("SELECT count(*) FROM dispatch.pilot_custody"));assertEquals(2,scalar("SELECT count(*) FROM orders.pilot_parcel_event"));assertFalse(httpRead(mvc,"/api/v1/customer/shipments?status=WAITING_FOR_HANDOVER",first.access()).getContentAsString().contains(shipment));assertTrue(httpRead(mvc,"/api/v1/customer/shipments?status=AT_STATION",first.access()).getContentAsString().contains(shipment));
        }
    }
    @Test void stationForeignShipmentScansAndRevocationAfterLookupCannotBecomeCustody()throws Exception{
        customerFixture(true);try(var inbox=new CustomerSmtpTestInbox(0,false,false,250,null);var c=customerHttpContext(PILOT_NOW,smtp(inbox,PILOT_NOW),true)){
            var mvc=httpMvc(c);var owner=profileBrowser(mvc);var staff=addressOtherBrowser(mvc);intakeAssignment(mvc,staff,"SIM-DROP-A1");String shipment=intakeBooked(mvc,owner),other=intakeBooked(mvc,owner);var scans=intakeScans(mvc,staff,intakeBarcodes(mvc,owner,shipment));var foreign=intakeScans(mvc,staff,intakeBarcodes(mvc,owner,other));assertEquals(409,intakeAccept(mvc,staff,shipment,List.of(scans.getFirst(),foreign.getFirst()),UUID.randomUUID(),"\"i0\"").getStatus());assertEquals(200,httpRead(mvc,STATION_PATH+"SIM-DROP-A1/shipments/"+shipment,staff.access()).getStatus());db.ownerExecute("UPDATE dispatch.pilot_station_assignment SET state='REVOKED',version=version+1");assertEquals(403,intakeAccept(mvc,staff,shipment,scans,UUID.randomUUID(),"\"i0\"").getStatus());assertEquals(0,scalar("SELECT count(*) FROM dispatch.pilot_intake_acceptance"));assertEquals(0,scalar("SELECT count(*) FROM orders.pilot_tracking_event"));
        }
    }
    @Test void stationInactiveCapabilityInjectedAuthorityAndRuntimePrivilegeExpansionAreDenied()throws Exception{
        customerFixture(true);try(var inbox=new CustomerSmtpTestInbox(0,false,false,250,null);var c=customerHttpContext(PILOT_NOW,smtp(inbox,PILOT_NOW),true)){
            var mvc=httpMvc(c);var owner=profileBrowser(mvc);var staff=addressOtherBrowser(mvc);intakeAssignment(mvc,staff,"SIM-DROP-A1");String shipment=intakeBooked(mvc,owner);var codes=intakeBarcodes(mvc,owner,shipment);assertEquals(400,onboardingWrite(mvc,staff,"POST",STATION_PATH+"SIM-DROP-A1/scans",UUID.randomUUID(),null,Map.of("barcode",codes.getFirst(),"actorId",A,"role","STATION_STAFF")).getStatus());assertThrows(SQLException.class,()->execute("UPDATE dispatch.pilot_station_assignment SET state='ACTIVE',version=version+1"));assertThrows(SQLException.class,()->execute("DELETE FROM dispatch.pilot_custody"));assertThrows(SQLException.class,()->execute("UPDATE orders.pilot_tracking_event SET event_type='DELIVERED'"));assertThrows(SQLException.class,()->execute("UPDATE finance.pilot_shipping_obligation SET state='PAID'"));db.ownerExecute("UPDATE dispatch.pilot_station SET active=FALSE,version=version+1 WHERE code='SIM-DROP-A1'");assertEquals(403,httpRead(mvc,STATION_PATH+"SIM-DROP-A1/parcels/"+codes.getFirst(),staff.access()).getStatus());assertEquals(0,scalar("SELECT count(*) FROM dispatch.pilot_intake_scan"));
        }
    }
    @Test void stationDisabledProductionModesAndRevokedIdentityCannotPerformAcceptance()throws Exception{
        customerFixture(true);for(String mode:List.of("DISABLED","PRODUCTION"))try(var inbox=new CustomerSmtpTestInbox(0,false,false,250,null);var c=customerHttpContext(PILOT_NOW,smtp(inbox,PILOT_NOW),true,mode)){
            var mvc=httpMvc(c);var owner=profileBrowser(mvc);assertEquals(409,httpRead(mvc,STATION_PATH.substring(0,STATION_PATH.length()-1),owner.access()).getStatus());
        }
        try(var inbox=new CustomerSmtpTestInbox(0,false,false,250,null);var c=customerHttpContext(PILOT_NOW,smtp(inbox,PILOT_NOW),true)){
            var mvc=httpMvc(c);var owner=profileBrowser(mvc);var staff=addressOtherBrowser(mvc);intakeAssignment(mvc,staff,"SIM-DROP-A1");String shipment=intakeBooked(mvc,owner);var scans=intakeScans(mvc,staff,intakeBarcodes(mvc,owner,shipment));execute("UPDATE identity.user_account SET auth_version=auth_version+1 WHERE id='"+B+"'");assertEquals(401,intakeAccept(mvc,staff,shipment,scans,UUID.randomUUID(),"\"i0\"").getStatus());assertEquals(0,scalar("SELECT count(*) FROM dispatch.pilot_intake_acceptance"));assertEquals(1,scalar("SELECT count(*) FROM finance.pilot_shipping_obligation WHERE state='UNPAID'"));
        }
    }


    private String businessShippingBase(UUID org){return "/api/v1/merchant-organizations/"+org;}
    private Map<String,Object> organizationContactInput(String name){
        return Map.of("kind","RECIPIENT","contactName",name,"contactPhone","0901234567","address",Map.of("country","VN","administrativeArea","Synthetic Area","locality","Synthetic Locality","addressLine","Synthetic 12"));
    }
    private void organizationContactOwner(UUID org)throws Exception{businessShippingOwner(org);businessCommand(UUID.randomUUID(),"GRANT",org,A,"OWNER","'dashboard.read','contacts.read','contacts.write'");}
    @Test void organizationContactsLifecycleDefaultsReplayAndPrivacy()throws Exception{
        customerFixture(false);try(var inbox=new CustomerSmtpTestInbox(0,false,false,250,null);var c=customerHttpContext(PILOT_NOW,smtp(inbox,PILOT_NOW),true)){
            var mvc=httpMvc(c);var owner=profileBrowser(mvc);var other=addressOtherBrowser(mvc);UUID org=id(9011);organizationContactOwner(org);String base=businessShippingBase(org),path=base+"/contacts";UUID key=UUID.randomUUID();var input=organizationContactInput("Synthetic Contact");
            var created=onboardingWrite(mvc,owner,"POST",path,key,null,input);assertEquals(201,created.getStatus(),this::safeOnboardingFailure);String contact=intakeJson(created).get("id").asString();assertEquals("+84901234567",intakeJson(created).get("contactPhone").asString());assertEquals("UNVERIFIED",intakeJson(created).get("verificationStatus").asString());
            assertEquals(created.getContentAsString(),onboardingWrite(mvc,owner,"POST",path,key,null,input).getContentAsString());assertEquals(404,httpRead(mvc,path,other.access()).getStatus());assertEquals(404,httpRead(mvc,path+"/"+contact,other.access()).getStatus());
            String defaults=base+"/contact-defaults?kind=RECIPIENT";assertEquals(0,intakeJson(httpRead(mvc,defaults,owner.access())).get("version").asLong());assertEquals(200,onboardingWrite(mvc,owner,"PUT",defaults,UUID.randomUUID(),"\"d0\"",Map.of("contactId",contact)).getStatus());
            var archived=onboardingWrite(mvc,owner,"POST",path+"/"+contact+"/archive",UUID.randomUUID(),"\"v1\"","");assertEquals(200,archived.getStatus(),this::safeOnboardingFailure);assertEquals(0,intakeJson(httpRead(mvc,path,owner.access())).get("items").size());var cleared=intakeJson(httpRead(mvc,defaults,owner.access()));assertTrue(cleared.get("contactId").isNull());assertEquals(2,cleared.get("version").asLong());
            assertEquals(200,onboardingWrite(mvc,owner,"POST",path+"/"+contact+"/restore",UUID.randomUUID(),"\"v2\"","").getStatus());assertTrue(intakeJson(httpRead(mvc,defaults,owner.access())).get("contactId").isNull());assertEquals(3,scalar("SELECT count(*) FROM merchant.organization_contact_version"));assertEquals(0,scalar("SELECT count(*) FROM merchant.customer_address"));assertEquals(0,scalar("SELECT count(*) FROM orders.pilot_shipment"));
            var search=onboardingWrite(mvc,owner,"POST",path+"/search",UUID.randomUUID(),null,Map.of("page",0,"size",20,"search","Synthetic"));assertEquals(200,search.getStatus(),this::safeOnboardingFailure);assertEquals(1,intakeJson(search).get("items").size());assertEquals(400,httpRead(mvc,path+"?search=Synthetic",owner.access()).getStatus());
            var duplicate=onboardingWrite(mvc,owner,"POST",base+"/contact-duplicates",UUID.randomUUID(),null,Map.of("input",input));assertEquals(200,duplicate.getStatus());assertEquals(1,intakeJson(duplicate).get("items").size());assertEquals(200,httpRead(mvc,base+"/contact-receipts/CREATE/"+key,owner.access()).getStatus());
            businessCommand(UUID.randomUUID(),"REVOKE",org,A,"OWNER","'contacts.write','contacts.read'");assertEquals(404,httpRead(mvc,path+"/"+contact,owner.access()).getStatus());assertEquals(404,httpRead(mvc,base+"/contact-receipts/CREATE/"+key,owner.access()).getStatus());
        }
    }
    @Test void organizationContactsStaleImmutableReferenceAndEvidenceRollback()throws Exception{
        customerFixture(false);try(var inbox=new CustomerSmtpTestInbox(0,false,false,250,null);var c=customerHttpContext(PILOT_NOW,smtp(inbox,PILOT_NOW),true)){
            var mvc=httpMvc(c);var owner=profileBrowser(mvc);UUID org=id(9012);organizationContactOwner(org);String path=businessShippingBase(org)+"/contacts";var input=new HashMap<>(organizationContactInput("Synthetic Original"));input.put("reference","CONTACT-1");UUID key=UUID.randomUUID();var created=onboardingWrite(mvc,owner,"POST",path,key,null,input);assertEquals(201,created.getStatus(),this::safeOnboardingFailure);String id=intakeJson(created).get("id").asString();assertEquals(409,onboardingWrite(mvc,owner,"POST",path,UUID.randomUUID(),null,input).getStatus());
            var changed=new HashMap<>(input);changed.put("contactName","Synthetic Updated");assertEquals(200,onboardingWrite(mvc,owner,"PUT",path+"/"+id,UUID.randomUUID(),"\"v1\"",changed).getStatus());assertEquals(409,onboardingWrite(mvc,owner,"PUT",path+"/"+id,UUID.randomUUID(),"\"v1\"",input).getStatus());assertEquals(409,onboardingWrite(mvc,owner,"POST",path,key,null,changed).getStatus());var moved=new HashMap<>(changed);moved.put("kind","SENDER");assertEquals(409,onboardingWrite(mvc,owner,"PUT",path+"/"+id,UUID.randomUUID(),"\"v2\"",moved).getStatus());
            try(var connection=db.runtimeConnection();var statement=connection.createStatement()){assertThrows(java.sql.SQLException.class,()->statement.executeUpdate("UPDATE merchant.organization_contact SET contact_name='Unauthorized',version=version+1 WHERE id='"+id+"'"));assertThrows(java.sql.SQLException.class,()->statement.executeUpdate("DELETE FROM merchant.organization_contact_version"));}
            db.ownerExecute("REVOKE INSERT ON merchant.organization_contact_audit FROM "+db.runtime());assertEquals(503,onboardingWrite(mvc,owner,"POST",path,UUID.randomUUID(),null,organizationContactInput("Synthetic Rollback")).getStatus());assertEquals(1,scalar("SELECT count(*) FROM merchant.organization_contact"));db.ownerExecute("GRANT INSERT ON merchant.organization_contact_audit TO "+db.runtime());
        }
    }
    @Test void organizationContactsFacilityIsolationAndProductionDenial()throws Exception{
        customerFixture(false);UUID org=id(9013),site=id(9014);String contactId;
        try(var inbox=new CustomerSmtpTestInbox(0,false,false,250,null);var c=customerHttpContext(PILOT_NOW,smtp(inbox,PILOT_NOW),true)){
            var mvc=httpMvc(c);var owner=profileBrowser(mvc);var other=addressOtherBrowser(mvc);organizationContactOwner(org);String path=businessShippingBase(org)+"/contacts";contactId=intakeJson(onboardingWrite(mvc,owner,"POST",path,UUID.randomUUID(),null,organizationContactInput("Synthetic Org Contact"))).get("id").asString();
            String contact=new tools.jackson.databind.ObjectMapper().writeValueAsString(shippingDraftInput("Site").get("sender")).replace("0901234567","+84901234567");seedFacility(site,org,contact);businessCommand(UUID.randomUUID(),"GRANT",org,B,"OPERATOR","'dashboard.read'");seedScopedLegacyGrants(org,B,site,"'contacts.read','contacts.write'");
            assertEquals(404,httpRead(mvc,path+"/"+contactId,other.access()).getStatus());var local=new HashMap<>(organizationContactInput("Synthetic Site Contact"));local.put("facilityId",site);assertEquals(201,onboardingWrite(mvc,other,"POST",path,UUID.randomUUID(),null,local).getStatus());assertEquals(1,intakeJson(httpRead(mvc,path,other.access())).get("items").size());assertEquals(404,httpRead(mvc,businessShippingBase(org)+"/contact-defaults?kind=RECIPIENT",other.access()).getStatus());
        }
        try(var inbox=new CustomerSmtpTestInbox(0,false,false,250,null);var c=customerHttpContext(PILOT_NOW,smtp(inbox,PILOT_NOW),true,"PRODUCTION")){var mvc=httpMvc(c);var owner=profileBrowser(mvc);assertEquals(503,httpRead(mvc,businessShippingBase(org)+"/contacts",owner.access()).getStatus());}
    }

    @Test void organizationContactsConcurrentDefaultAndArchiveSerializeWithReceipts()throws Exception{
        customerFixture(false);try(var inbox=new CustomerSmtpTestInbox(0,false,false,250,null);var c=customerHttpContext(PILOT_NOW,smtp(inbox,PILOT_NOW),true)){
            var mvc=httpMvc(c);var owner=profileBrowser(mvc);UUID org=id(9015);organizationContactOwner(org);String base=businessShippingBase(org),path=base+"/contacts";String first=responseId(onboardingWrite(mvc,owner,"POST",path,UUID.randomUUID(),null,organizationContactInput("Synthetic First")),"id"),second=responseId(onboardingWrite(mvc,owner,"POST",path,UUID.randomUUID(),null,organizationContactInput("Synthetic Second")),"id");String defaults=base+"/contact-defaults?kind=RECIPIENT";
            var results=adminRace(()->onboardingWrite(mvc,owner,"PUT",defaults,UUID.randomUUID(),"\"d0\"",Map.of("contactId",first)).getStatus(),()->onboardingWrite(mvc,owner,"PUT",defaults,UUID.randomUUID(),"\"d0\"",Map.of("contactId",second)).getStatus());assertEquals(1,results.stream().filter(v->Integer.valueOf(200).equals(v)).count());assertEquals(1,results.stream().filter(v->Integer.valueOf(409).equals(v)).count());assertEquals(1,scalar("SELECT count(*) FROM merchant.organization_contact_default"));String chosen=intakeJson(httpRead(mvc,defaults,owner.access())).get("contactId").asString();UUID key=UUID.randomUUID();assertEquals(200,onboardingWrite(mvc,owner,"POST",path+"/"+chosen+"/archive",key,"\"v1\"","").getStatus());assertTrue(intakeJson(httpRead(mvc,defaults,owner.access())).get("contactId").isNull());assertEquals(200,onboardingWrite(mvc,owner,"POST",path+"/"+chosen+"/archive",key,"\"v1\"","").getStatus());assertEquals(2,intakeJson(httpRead(mvc,defaults,owner.access())).get("version").asLong());
        }
    }
    private String organizationContactCsv(String suffix){return "kind,label,reference,group,contact_name,contact_phone,country,administrative_area,locality,address_line\r\nRECIPIENT,,"+suffix+",,Synthetic Import,0901234567,VN,Synthetic Area,Synthetic Locality,Synthetic Street\r\n";}
    @Test void organizationContactsImportPreviewCommitCreatorIsolationAndExportAudit()throws Exception{
        customerFixture(false);try(var inbox=new CustomerSmtpTestInbox(0,false,false,250,null);var c=customerHttpContext(PILOT_NOW,smtp(inbox,PILOT_NOW),true)){
            var mvc=httpMvc(c);var owner=profileBrowser(mvc);var other=addressOtherBrowser(mvc);UUID org=id(9016);organizationContactOwner(org);businessCommand(UUID.randomUUID(),"GRANT",org,A,"OWNER","'dashboard.read','contacts.read','contacts.write','contacts.import','contacts.export'");businessCommand(UUID.randomUUID(),"GRANT",org,B,"OPERATOR","'dashboard.read','contacts.read','contacts.write','contacts.import'");String base=businessShippingBase(org),path=base+"/contact-imports";Map<String,Object> input=new HashMap<>();input.put("templateVersion","ORG_CONTACTS_CSV_1");input.put("facilityId",null);input.put("csv",organizationContactCsv("IMPORTED-1"));UUID previewKey=UUID.randomUUID();var preview=onboardingWrite(mvc,owner,"POST",path,previewKey,null,input);assertEquals(200,preview.getStatus(),this::safeOnboardingFailure);String job=responseId(preview,"id");assertEquals(0,scalar("SELECT count(*) FROM merchant.organization_contact"));assertEquals(1,intakeJson(preview).get("validRows").asInt());assertEquals(404,httpRead(mvc,path+"/"+job,other.access()).getStatus());
            UUID key=UUID.randomUUID();var committed=onboardingWrite(mvc,owner,"POST",path+"/"+job+"/commit",key,"\"v1\"",Map.of("rows",List.of(1)));assertEquals(200,committed.getStatus(),this::safeOnboardingFailure);assertEquals(1,intakeJson(committed).get("createdRows").asInt());assertEquals(committed.getContentAsString(),onboardingWrite(mvc,owner,"POST",path+"/"+job+"/commit",key,"\"v1\"",Map.of("rows",List.of(1))).getContentAsString());assertEquals(preview.getContentAsString(),onboardingWrite(mvc,owner,"POST",path,previewKey,null,input).getContentAsString());
            var exported=onboardingWrite(mvc,owner,"POST",base+"/contact-exports",null,null,Map.of());assertEquals(200,exported.getStatus(),this::safeOnboardingFailure);assertEquals(1,intakeJson(exported).get("rowCount").asInt());assertEquals("LOCAL_TEST_SIMULATION",intakeJson(exported).get("source").asString());assertEquals(1,businessScalar("SELECT count(*) FROM merchant.organization_contact_export_audit"));assertEquals(404,onboardingWrite(mvc,other,"POST",base+"/contact-exports",null,null,Map.of()).getStatus());
            db.ownerExecute("REVOKE INSERT ON merchant.organization_contact_export_audit FROM "+db.runtime());assertEquals(503,onboardingWrite(mvc,owner,"POST",base+"/contact-exports",null,null,Map.of()).getStatus());db.ownerExecute("GRANT INSERT ON merchant.organization_contact_export_audit TO "+db.runtime());assertEquals(0,scalar("SELECT count(*) FROM orders.pilot_shipment"));assertEquals(0,scalar("SELECT count(*) FROM merchant.customer_address"));
        }
    }
    @Test void organizationContactsImportErrorsAreExplicitAndChunkConflictRollsBack()throws Exception{
        customerFixture(false);try(var inbox=new CustomerSmtpTestInbox(0,false,false,250,null);var c=customerHttpContext(PILOT_NOW,smtp(inbox,PILOT_NOW),true)){
            var mvc=httpMvc(c);var owner=profileBrowser(mvc);UUID org=id(9017);organizationContactOwner(org);businessCommand(UUID.randomUUID(),"GRANT",org,A,"OWNER","'dashboard.read','contacts.read','contacts.write','contacts.import'");String base=businessShippingBase(org),path=base+"/contact-imports";Map<String,Object> input=new HashMap<>();input.put("templateVersion","ORG_CONTACTS_CSV_1");input.put("facilityId",null);input.put("csv",organizationContactCsv("SAFE-1")+"RECIPIENT,,SAFE-2,,=1+1,0901234567,VN,Area,Locality,Street\r\n");var preview=onboardingWrite(mvc,owner,"POST",path,UUID.randomUUID(),null,input);assertEquals(200,preview.getStatus(),this::safeOnboardingFailure);assertEquals(1,intakeJson(preview).get("invalidRows").asInt());String job=responseId(preview,"id");assertEquals(409,onboardingWrite(mvc,owner,"POST",path+"/"+job+"/commit",UUID.randomUUID(),"\"v1\"",Map.of("rows",List.of(1,2))).getStatus());assertEquals(0,scalar("SELECT count(*) FROM merchant.organization_contact"));
            input.put("csv",organizationContactCsv("CONFLICT-1")+"RECIPIENT,,CONFLICT-2,,Synthetic Conflict,0901234567,VN,Area,Locality,Street\r\n");var next=onboardingWrite(mvc,owner,"POST",path,UUID.randomUUID(),null,input);assertEquals(200,next.getStatus());String nextJob=responseId(next,"id");var existing=new HashMap<>(organizationContactInput("Existing Conflict"));existing.put("reference","CONFLICT-2");assertEquals(201,onboardingWrite(mvc,owner,"POST",base+"/contacts",UUID.randomUUID(),null,existing).getStatus());assertEquals(409,onboardingWrite(mvc,owner,"POST",path+"/"+nextJob+"/commit",UUID.randomUUID(),"\"v1\"",Map.of("rows",List.of(1,2))).getStatus());assertEquals(1,scalar("SELECT count(*) FROM merchant.organization_contact"));assertEquals(0,scalar("SELECT count(*) FROM merchant.organization_contact_import_row WHERE contact_id IS NOT NULL"));
        }
    }

    private void organizationActionOwner(UUID org)throws Exception{businessShippingOwner(org);businessCommand(UUID.randomUUID(),"GRANT",org,A,"OWNER","'dashboard.read','shipment.draft.read','shipment.actions.manage'");businessCommand(UUID.randomUUID(),"GRANT",org,B,"OPERATOR","'dashboard.read','shipment.draft.read','shipment.actions.respond'");}
    private String organizationActionCreate(org.springframework.test.web.servlet.MockMvc mvc,ProfileBrowser owner,UUID org,String draft)throws Exception{var created=onboardingWrite(mvc,owner,"POST",businessShippingBase(org)+"/action-tasks",UUID.randomUUID(),null,Map.of("targetKind","PREPARATION","targetId",draft,"type","PREPARATION_REVIEW","title","Synthetic preparation review","details","Check synthetic contents","priority","HIGH"));assertEquals(201,created.getStatus(),this::safeOnboardingFailure);return intakeJson(created).get("task").get("id").asString();}
    @Test void organizationActionsAssignedWorkerManagerReviewNoticesAndCarrierIsolation()throws Exception{
        customerFixture(false);try(var inbox=new CustomerSmtpTestInbox(0,false,false,250,null);var c=customerHttpContext(PILOT_NOW,smtp(inbox,PILOT_NOW),true)){
            var mvc=httpMvc(c);var owner=profileBrowser(mvc);var worker=addressOtherBrowser(mvc);UUID org=id(9111);organizationActionOwner(org);String draft=businessTestDraft(mvc,owner,org),base=businessShippingBase(org),task=organizationActionCreate(mvc,owner,org,draft),path=base+"/action-tasks/"+task;
            assertEquals(403,onboardingWrite(mvc,worker,"POST",path+"/transitions",UUID.randomUUID(),"\"v1\"",Map.of("action","START","message","Starting")).getStatus());UUID assignKey=UUID.randomUUID();var assigned=onboardingWrite(mvc,owner,"PUT",path+"/assignment",assignKey,"\"v1\"",Map.of("assigneeAccountId",B,"reason","Synthetic assignment"));assertEquals(200,assigned.getStatus(),this::safeOnboardingFailure);assertEquals(1,scalar("SELECT count(*) FROM notification.organization_action_notice"));assertEquals(assigned.getContentAsString(),onboardingWrite(mvc,owner,"PUT",path+"/assignment",assignKey,"\"v1\"",Map.of("assigneeAccountId",B,"reason","Synthetic assignment")).getContentAsString());assertEquals(1,scalar("SELECT count(*) FROM notification.organization_action_notice"));
            assertEquals(200,onboardingWrite(mvc,worker,"POST",path+"/transitions",UUID.randomUUID(),"\"v2\"",Map.of("action","START","message","Preparing synthetic contents")).getStatus());assertEquals(200,onboardingWrite(mvc,worker,"POST",path+"/transitions",UUID.randomUUID(),"\"v3\"",Map.of("action","REQUEST_REVIEW","message","Synthetic preparation complete; please review")).getStatus());assertEquals(403,onboardingWrite(mvc,worker,"POST",path+"/transitions",UUID.randomUUID(),"\"v4\"",Map.of("action","COMPLETE","message","Self approval denied")).getStatus());var completed=onboardingWrite(mvc,owner,"POST",path+"/transitions",UUID.randomUUID(),"\"v4\"",Map.of("action","COMPLETE","message","Internal review complete"));assertEquals(200,completed.getStatus(),this::safeOnboardingFailure);assertEquals("COMPLETED",intakeJson(completed).get("task").get("state").asString());assertEquals(5,intakeJson(completed).get("events").size());assertEquals("DRAFT",intakeJson(httpRead(mvc,preparationPath(org)+"/"+draft,owner.access())).get("state").asString());assertEquals(0,scalar("SELECT count(*) FROM orders.pilot_tracking_event"));assertEquals(0,scalar("SELECT count(*) FROM finance.pilot_shipping_obligation"));
            var notices=intakeJson(httpRead(mvc,base+"/action-notices",worker.access()));assertEquals(1,notices.get("items").size());String notice=notices.get("items").get(0).get("id").asString();assertEquals(404,onboardingWrite(mvc,owner,"POST",base+"/action-notices/"+notice+"/read",null,null,"").getStatus());assertEquals(200,onboardingWrite(mvc,worker,"POST",base+"/action-notices/"+notice+"/read",null,null,"").getStatus());assertEquals(200,onboardingWrite(mvc,worker,"POST",base+"/action-notices/"+notice+"/read",null,null,"").getStatus());assertEquals("1",intakeJson(httpRead(mvc,base+"/action-tasks?state=COMPLETED",owner.access())).get("total").asString());
        }
    }
    @Test void organizationActionsNoticeFailureAndConcurrencyAreAtomic()throws Exception{
        customerFixture(false);try(var inbox=new CustomerSmtpTestInbox(0,false,false,250,null);var c=customerHttpContext(PILOT_NOW,smtp(inbox,PILOT_NOW),true)){
            var mvc=httpMvc(c);var owner=profileBrowser(mvc);addressOtherBrowser(mvc);UUID org=id(9112);organizationActionOwner(org);String task=organizationActionCreate(mvc,owner,org,businessTestDraft(mvc,owner,org)),path=businessShippingBase(org)+"/action-tasks/"+task;db.ownerExecute("REVOKE INSERT ON notification.organization_action_notice FROM "+db.runtime());assertEquals(503,onboardingWrite(mvc,owner,"PUT",path+"/assignment",UUID.randomUUID(),"\"v1\"",Map.of("assigneeAccountId",B,"reason","Must be atomic")).getStatus());assertEquals(1,intakeJson(httpRead(mvc,path,owner.access())).get("task").get("version").asLong());assertEquals(1,scalar("SELECT count(*) FROM orders.organization_action_event"));db.ownerExecute("GRANT INSERT ON notification.organization_action_notice TO "+db.runtime());
            assertEquals(200,onboardingWrite(mvc,owner,"PUT",path+"/assignment",UUID.randomUUID(),"\"v1\"",Map.of("assigneeAccountId",B,"reason","Assigned atomically")).getStatus());var result=adminRace(()->onboardingWrite(mvc,owner,"POST",path+"/comments",UUID.randomUUID(),"\"v2\"",Map.of("message","Synthetic first")).getStatus(),()->onboardingWrite(mvc,owner,"POST",path+"/comments",UUID.randomUUID(),"\"v2\"",Map.of("message","Synthetic second")).getStatus());assertEquals(1,result.stream().filter(v->Integer.valueOf(200).equals(v)).count());assertEquals(1,result.stream().filter(v->Integer.valueOf(409).equals(v)).count());assertEquals(3,scalar("SELECT count(*) FROM orders.organization_action_event"));assertEquals(2,scalar("SELECT count(*) FROM notification.organization_action_notice"));assertThrows(SQLException.class,()->execute("UPDATE orders.organization_action_task SET state='COMPLETED',version=version+1,completed_at=updated_at"));assertThrows(SQLException.class,()->execute("DELETE FROM orders.organization_action_event"));
        }
    }
    @Test void organizationActionsPermissionRevocationAndProductionDenyMutations()throws Exception{
        customerFixture(false);UUID org=id(9113);String task;
        try(var inbox=new CustomerSmtpTestInbox(0,false,false,250,null);var c=customerHttpContext(PILOT_NOW,smtp(inbox,PILOT_NOW),true)){
            var mvc=httpMvc(c);var owner=profileBrowser(mvc);var worker=addressOtherBrowser(mvc);organizationActionOwner(org);String base=businessShippingBase(org);task=organizationActionCreate(mvc,owner,org,businessTestDraft(mvc,owner,org));String path=base+"/action-tasks/"+task;assertEquals(200,onboardingWrite(mvc,owner,"PUT",path+"/assignment",UUID.randomUUID(),"\"v1\"",Map.of("assigneeAccountId",B,"reason","Synthetic scope")).getStatus());businessCommand(UUID.randomUUID(),"REVOKE",org,B,"OPERATOR","'shipment.actions.respond'");assertEquals(403,onboardingWrite(mvc,worker,"POST",path+"/transitions",UUID.randomUUID(),"\"v2\"",Map.of("action","START","message","Denied after revocation")).getStatus());assertEquals(0,intakeJson(httpRead(mvc,path+"/assignment-candidates",owner.access())).get("items").size());assertEquals(400,onboardingWrite(mvc,owner,"POST",path+"/transitions",UUID.randomUUID(),"\"v2\"",Map.of("action","DELIVERED","message","Never a Carrier event")).getStatus());
            businessCommand(UUID.randomUUID(),"REVOKE",org,B,"OPERATOR","'shipment.draft.read'");assertEquals(404,httpRead(mvc,path,worker.access()).getStatus());assertEquals(404,httpRead(mvc,base+"/action-notices",worker.access()).getStatus());
        }
        try(var inbox=new CustomerSmtpTestInbox(0,false,false,250,null);var c=customerHttpContext(PILOT_NOW,smtp(inbox,PILOT_NOW),true,"TEST_WITH_PRODUCTION_PROFILE")){var mvc=httpMvc(c);var owner=profileBrowser(mvc);assertEquals(503,httpRead(mvc,businessShippingBase(org)+"/action-tasks",owner.access()).getStatus());}
    }

    private void businessReportOwner(UUID org)throws Exception{businessManagementOwner(org);businessCommand(UUID.randomUUID(),"GRANT",org,A,"OWNER","'dashboard.read','shipment.draft.read','shipment.reports.read','shipment.finance.read'");}
    @Test void businessReportsCaptureWholeOrganizationWithoutParcelOrPersonalDoubleCounting()throws Exception{
        customerFixture(false);try(var inbox=new CustomerSmtpTestInbox(0,false,false,250,null);var c=customerHttpContext(PILOT_NOW,smtp(inbox,PILOT_NOW),true)){
            var mvc=httpMvc(c);var owner=profileBrowser(mvc);var other=addressOtherBrowser(mvc);UUID org=id(8911);businessReportOwner(org);businessManagementBooking(mvc,owner,org);businessManagementBooking(mvc,owner,org);intakeBooked(mvc,owner);String base=businessShippingBase(org);
            var response=httpRead(mvc,base+"/operations-reports",owner.access());assertEquals(200,response.getStatus(),this::safeOnboardingFailure);assertEquals("no-store",response.getHeader("Cache-Control"));var report=intakeJson(response);assertEquals("ORGANIZATION_OPERATIONS_1",report.get("schemaVersion").asString());assertEquals("2",report.get("summary").get("shipments").asString());assertEquals("4",report.get("summary").get("parcels").asString());assertEquals("106000",report.get("summary").get("confirmedFeesVnd").asString());assertEquals("2",report.get("preparations").get("booked").asString());assertEquals("2",report.get("activity").get("created").asString());assertEquals("2",report.get("activity").get("booked").asString());
            var finance=httpRead(mvc,base+"/shipping-finance?page=1",owner.access());assertEquals(200,finance.getStatus(),this::safeOnboardingFailure);var f=intakeJson(finance);assertEquals("2",f.get("total").asString());assertEquals(0,f.get("items").size());assertEquals("106000",f.get("summary").get("unpaidFeesVnd").asString());assertEquals("0",f.get("summary").get("differenceVnd").asString());assertFalse(f.toString().contains("recipient"));assertEquals(404,httpRead(mvc,base+"/operations-reports",other.access()).getStatus());assertEquals(404,httpRead(mvc,base+"/shipping-finance",other.access()).getStatus());
            for(String interval:List.of("DAY","WEEK","MONTH","QUARTER","YEAR"))assertEquals(200,httpRead(mvc,base+"/operations-reports?interval="+interval,owner.access()).getStatus());for(String bad:List.of("?from=2026-02-30&until=2026-03-02","?interval=HOUR","?interval=DAY&interval=WEEK","?ownerId="+A,"?status=DELIVERED"))assertEquals(400,httpRead(mvc,base+"/operations-reports"+bad,owner.access()).getStatus());assertEquals(0,scalar("SELECT count(*) FROM orders.organization_report_export_audit"));
        }
    }
    @Test void businessReportsSeparateFinancialPermissionAndEmptyCohortAuthority()throws Exception{
        customerFixture(false);try(var inbox=new CustomerSmtpTestInbox(0,false,false,250,null);var c=customerHttpContext(PILOT_NOW,smtp(inbox,PILOT_NOW),true)){
            var mvc=httpMvc(c);var owner=profileBrowser(mvc);var other=addressOtherBrowser(mvc);UUID org=id(8912);businessManagementOwner(org);businessCommand(UUID.randomUUID(),"GRANT",org,A,"OWNER","'dashboard.read','shipment.draft.read','shipment.reports.read'");String base=businessShippingBase(org);
            var empty=httpRead(mvc,base+"/operations-reports",owner.access());assertEquals(200,empty.getStatus(),this::safeOnboardingFailure);assertEquals("0",intakeJson(empty).get("summary").get("shipments").asString());assertEquals("FORBIDDEN",intakeJson(empty).get("financialState").asString());assertTrue(intakeJson(empty).get("summary").get("confirmedFeesVnd").isNull());assertEquals(404,httpRead(mvc,base+"/shipping-finance",owner.access()).getStatus());businessManagementBooking(mvc,owner,org);
            var ops=intakeJson(httpRead(mvc,base+"/operations-reports",owner.access()));assertEquals("1",ops.get("summary").get("shipments").asString());assertTrue(ops.get("summary").get("unpaidFeesVnd").isNull());assertTrue(ops.get("trend").get(0).get("confirmedFeesVnd").isNull());
            businessCommand(UUID.randomUUID(),"GRANT",org,B,"OPERATOR","'dashboard.read','shipment.draft.read','shipment.finance.read'");assertEquals(200,httpRead(mvc,base+"/shipping-finance",other.access()).getStatus());assertEquals(404,httpRead(mvc,base+"/operations-reports",other.access()).getStatus());businessCommand(UUID.randomUUID(),"REVOKE",org,B,"OPERATOR","'shipment.finance.read'");assertEquals(404,httpRead(mvc,base+"/shipping-finance",other.access()).getStatus());
        }
    }
    @Test void businessReportsExportHistoryIsOwnScopedAppendOnlyAndAuditFailureDeniesOutput()throws Exception{
        customerFixture(false);try(var inbox=new CustomerSmtpTestInbox(0,false,false,250,null);var c=customerHttpContext(PILOT_NOW,smtp(inbox,PILOT_NOW),true)){
            var mvc=httpMvc(c);var owner=profileBrowser(mvc);var other=addressOtherBrowser(mvc);UUID org=id(8913);businessReportOwner(org);businessManagementBooking(mvc,owner,org);String base=businessShippingBase(org),path=base+"/shipping-report-exports?kind=OPERATIONS&section=trend&interval=MONTH";
            var first=httpRead(mvc,path,owner.access());assertEquals(200,first.getStatus(),this::safeOnboardingFailure);assertEquals("ORGANIZATION_REPORT_EXPORT_1",intakeJson(first).get("schemaVersion").asString());assertFalse(first.getContentAsString().contains("recipient"));assertTrue(intakeJson(first).get("rows").size()>0);assertEquals(200,httpRead(mvc,base+"/shipping-report-exports?kind=FINANCE&section=route",owner.access()).getStatus());assertEquals(2,scalar("SELECT count(*) FROM orders.organization_report_export_audit"));
            var history=httpRead(mvc,base+"/shipping-report-history",owner.access());assertEquals(200,history.getStatus(),this::safeOnboardingFailure);assertEquals(2,intakeJson(history).get("items").size());assertTrue(history.getContentAsString().contains("interval"));assertFalse(history.getContentAsString().contains("contributing_shipment_ids"));
            businessCommand(UUID.randomUUID(),"GRANT",org,B,"OPERATOR","'dashboard.read','shipment.draft.read','shipment.reports.read','shipment.export'");assertEquals(0,intakeJson(httpRead(mvc,base+"/shipping-report-history",other.access())).get("items").size());
            db.ownerExecute("REVOKE INSERT ON orders.organization_report_export_audit FROM \""+db.runtime()+"\"");try{assertEquals(503,httpRead(mvc,path,owner.access()).getStatus());assertEquals(2,scalar("SELECT count(*) FROM orders.organization_report_export_audit"));}finally{db.ownerExecute("GRANT INSERT ON orders.organization_report_export_audit TO \""+db.runtime()+"\"");}
            assertThrows(SQLException.class,()->execute("DELETE FROM orders.organization_report_export_audit"));assertThrows(SQLException.class,()->execute("UPDATE orders.organization_report_export_audit SET row_count=0"));assertEquals(400,httpRead(mvc,path+"&search=anything",owner.access()).getStatus());
            businessCommand(UUID.randomUUID(),"REVOKE",org,A,"OWNER","'shipment.finance.read'");assertEquals(1,intakeJson(httpRead(mvc,base+"/shipping-report-history",owner.access())).get("items").size());businessCommand(UUID.randomUUID(),"REVOKE",org,A,"OWNER","'shipment.export','shipment.export.pii'");assertEquals(404,httpRead(mvc,base+"/shipping-report-history",owner.access()).getStatus());assertEquals(404,httpRead(mvc,path,owner.access()).getStatus());
        }
    }
    @Test void businessReportsFacilityGrantsAndPreparationComparabilityRemainExact()throws Exception{
        customerFixture(false);try(var inbox=new CustomerSmtpTestInbox(0,false,false,250,null);var c=customerHttpContext(PILOT_NOW,smtp(inbox,PILOT_NOW),true)){
            var mvc=httpMvc(c);var owner=profileBrowser(mvc);var other=addressOtherBrowser(mvc);UUID org=id(8914),site=id(8915);businessReportOwner(org);businessManagementBooking(mvc,owner,org);String contact=new tools.jackson.databind.ObjectMapper().writeValueAsString(shippingDraftInput("Site").get("sender")).replace("0901234567","+84901234567");seedFacility(site,org,contact);
            var input=new HashMap<>(pilotDraftInput("Scoped report"));input.put("facilityId",site);String draft=responseId(onboardingWrite(mvc,owner,"POST",preparationPath(org),UUID.randomUUID(),null,input),"id");var q=businessTestQuote(mvc,owner,org,draft,UUID.randomUUID());assertEquals(201,q.getStatus(),this::safeOnboardingFailure);assertEquals(201,businessTestBook(mvc,owner,org,draft,intakeJson(q).get("quote").get("id").asString(),UUID.randomUUID()).getStatus());
            businessCommand(UUID.randomUUID(),"GRANT",org,B,"OPERATOR","'dashboard.read','shipment.draft.read'");seedScopedLegacyGrants(org,B,site,"'shipment.reports.read','shipment.finance.read','shipment.export'");String base=businessShippingBase(org);
            var ops=httpRead(mvc,base+"/operations-reports",other.access());assertEquals(200,ops.getStatus(),this::safeOnboardingFailure);assertEquals("1",intakeJson(ops).get("summary").get("shipments").asString());assertEquals("AVAILABLE",intakeJson(ops).get("financialState").asString());assertEquals("1",intakeJson(ops).get("preparations").get("total").asString());assertEquals("1",intakeJson(httpRead(mvc,base+"/shipping-finance",other.access())).get("total").asString());assertEquals(404,httpRead(mvc,base+"/operations-reports?facilityId="+UUID.randomUUID(),other.access()).getStatus());
            var route=intakeJson(httpRead(mvc,base+"/operations-reports?origin=SIM-A1&destination=SIM-A2",other.access()));assertEquals("NOT_COMPARABLE",route.get("preparations").get("state").asString());assertTrue(route.get("activity").get("created").isNull());
            businessCommand(UUID.randomUUID(),"GRANT",org,B,"OPERATOR","'dashboard.read','shipment.draft.read','shipment.reports.read'");assertEquals("FORBIDDEN",intakeJson(httpRead(mvc,base+"/operations-reports",other.access())).get("financialState").asString());assertEquals("AVAILABLE",intakeJson(httpRead(mvc,base+"/operations-reports?facilityId="+site,other.access())).get("financialState").asString());assertEquals("1",intakeJson(httpRead(mvc,base+"/shipping-finance",other.access())).get("total").asString());
        }
    }
    @Test void businessReportsProductionAndSuspensionNeverEnableFinanceOrCarrier()throws Exception{
        customerFixture(false);UUID org=id(8916);try(var inbox=new CustomerSmtpTestInbox(0,false,false,250,null);var c=customerHttpContext(PILOT_NOW,smtp(inbox,PILOT_NOW),true)){var mvc=httpMvc(c);var owner=profileBrowser(mvc);businessReportOwner(org);businessManagementBooking(mvc,owner,org);}
        for(String mode:List.of("DISABLED","PRODUCTION","TEST_WITH_PRODUCTION_PROFILE"))try(var inbox=new CustomerSmtpTestInbox(0,false,false,250,null);var c=customerHttpContext(PILOT_NOW,smtp(inbox,PILOT_NOW),true,mode)){var mvc=httpMvc(c);var owner=profileBrowser(mvc);for(String route:List.of("/operations-reports","/shipping-finance","/shipping-report-history","/shipping-report-exports?kind=FINANCE&section=route"))assertEquals(503,httpRead(mvc,businessShippingBase(org)+route,owner.access()).getStatus());}
        try(var inbox=new CustomerSmtpTestInbox(0,false,false,250,null);var c=customerHttpContext(PILOT_NOW,smtp(inbox,PILOT_NOW),true)){var mvc=httpMvc(c);var owner=profileBrowser(mvc);businessCommand(UUID.randomUUID(),"SUSPEND",org,A,"OWNER","");assertThrows(SQLException.class,()->execute("INSERT INTO orders.organization_report_export_audit VALUES(gen_random_uuid(),'"+org+"','"+A+"',gen_random_uuid(),'OPERATIONS','trend','{\"from\":\"2026-11-01\",\"until\":\"2026-11-03\",\"interval\":\"DAY\"}'::jsonb,decode(repeat('00',32),'hex'),ARRAY[]::uuid[],0,TIMESTAMPTZ '2026-11-02 00:00:00Z')"));for(String route:List.of("/operations-reports","/shipping-finance","/shipping-report-history","/shipping-report-exports?kind=OPERATIONS&section=trend"))assertEquals(404,httpRead(mvc,businessShippingBase(org)+route,owner.access()).getStatus());}
        assertEquals(0,scalar("SELECT count(*) FROM orders.organization_report_export_audit"));assertEquals(0,scalar("SELECT count(*) FROM orders.pilot_tracking_event"));assertEquals(1,scalar("SELECT count(*) FROM finance.pilot_shipping_obligation WHERE state='UNPAID'"));
    }

    private void businessManagementOwner(UUID org)throws Exception{businessShippingOwner(org);businessCommand(UUID.randomUUID(),"GRANT",org,A,"OWNER","'dashboard.read','shipment.draft.read','shipment.export','shipment.export.pii','shipment.assign'");businessTestContract(org);}
    private String businessManagementBooking(org.springframework.test.web.servlet.MockMvc mvc,ProfileBrowser browser,UUID org)throws Exception{String d=businessTestDraft(mvc,browser,org);var q=businessTestQuote(mvc,browser,org,d,UUID.randomUUID());assertEquals(201,q.getStatus(),this::safeOnboardingFailure);var b=businessTestBook(mvc,browser,org,d,intakeJson(q).get("quote").get("id").asString(),UUID.randomUUID());assertEquals(201,b.getStatus(),this::safeOnboardingFailure);return intakeJson(b).get("shipment").get("id").asString();}
    @Test void businessManagementCapturedCohortSearchAndLegacyContractRemainIsolated()throws Exception{
        customerFixture(false);try(var inbox=new CustomerSmtpTestInbox(0,false,false,250,null);var c=customerHttpContext(PILOT_NOW,smtp(inbox,PILOT_NOW),true)){
            var mvc=httpMvc(c);var owner=profileBrowser(mvc);var other=addressOtherBrowser(mvc);UUID org=id(8811);businessManagementOwner(org);String shipment=businessManagementBooking(mvc,owner,org),path=businessShippingBase(org)+"/shipments",query=path+"?view=management";
            var response=httpRead(mvc,query,owner.access());assertEquals(200,response.getStatus(),this::safeOnboardingFailure);var data=intakeJson(response);assertEquals("ORGANIZATION_MANAGEMENT_1",data.get("schemaVersion").asString());assertEquals("1",data.get("summary").get("bookedShipments").asString());assertEquals("1",data.get("summary").get("unassigned").asString());assertEquals("53000",data.get("summary").get("confirmedFeesVnd").asString());assertEquals(2,data.get("items").get(0).get("record").get("shipment").get("parcels").size());assertEquals(shipment,data.get("items").get(0).get("record").get("shipment").get("id").asString());
            assertFalse(intakeJson(httpRead(mvc,path,owner.access())).has("schemaVersion"));assertEquals(404,httpRead(mvc,query,other.access()).getStatus());assertEquals(404,httpRead(mvc,path+"/"+shipment,other.access()).getStatus());
            String parcel=data.get("items").get(0).get("record").get("shipment").get("parcels").get(0).get("id").asString();assertEquals("1",intakeJson(httpRead(mvc,query+"&search=AP-P-"+parcel.replace("-",""),owner.access())).get("total").asString());assertEquals("0",intakeJson(httpRead(mvc,query+"&search=%25",owner.access())).get("total").asString());
            var second=intakeJson(httpRead(mvc,query+"&page=1",owner.access()));assertEquals("1",second.get("total").asString());assertEquals(0,second.get("items").size());assertFalse(second.get("hasNext").asBoolean());
            var day=PILOT_NOW.atZone(ZoneId.of("Asia/Ho_Chi_Minh")).toLocalDate();assertEquals("1",intakeJson(httpRead(mvc,query+"&from="+day+"&until="+day.plusDays(1),owner.access())).get("total").asString());assertEquals("0",intakeJson(httpRead(mvc,query+"&from="+day.minusDays(1)+"&until="+day,owner.access())).get("total").asString());
            for(String invalid:List.of("&status=DELIVERED","&handover=PICKUP","&obligation=PAID","&actorAccountId="+A,"&search=a&search=b"))assertEquals(400,httpRead(mvc,query+invalid,owner.access()).getStatus());assertEquals(0,scalar("SELECT count(*) FROM orders.organization_shipment_assignment"));
        }
    }
    @Test void businessManagementAssignmentIsVersionedAuditedAndNeverCarrierTracking()throws Exception{
        customerFixture(false);try(var inbox=new CustomerSmtpTestInbox(0,false,false,250,null);var c=customerHttpContext(PILOT_NOW,smtp(inbox,PILOT_NOW),true)){
            var mvc=httpMvc(c);var owner=profileBrowser(mvc);UUID org=id(8812);businessManagementOwner(org);String shipment=businessManagementBooking(mvc,owner,org),base=businessShippingBase(org),path=base+"/shipments/"+shipment+"/assignment";UUID key=UUID.randomUUID();
            assertEquals(0,intakeJson(httpRead(mvc,path,owner.access())).get("version").asLong());assertEquals(0,scalar("SELECT count(*) FROM orders.organization_shipment_assignment"));
            var assigned=onboardingWrite(mvc,owner,"PUT",path,key,"\"a0\"",Map.of("assigneeAccountId",A,"reasonCode","INITIAL_ASSIGNMENT"));assertEquals(200,assigned.getStatus(),this::safeOnboardingFailure);assertEquals("\"a1\"",assigned.getHeader("ETag"));assertEquals(A.toString(),intakeJson(assigned).get("assigneeAccountId").asString());
            assertEquals(assigned.getContentAsString(),onboardingWrite(mvc,owner,"PUT",path,key,"\"a0\"",Map.of("assigneeAccountId",A,"reasonCode","INITIAL_ASSIGNMENT")).getContentAsString());assertEquals(1,scalar("SELECT count(*) FROM orders.organization_shipment_assignment_event"));assertEquals("0",intakeJson(httpRead(mvc,base+"/shipments?view=management",owner.access())).get("summary").get("unassigned").asString());
            var candidates=httpRead(mvc,base+"/shipments/"+shipment+"/assignment-candidates",owner.access());assertEquals(200,candidates.getStatus(),this::safeOnboardingFailure);assertEquals(A.toString(),intakeJson(candidates).get("items").get(0).get("accountId").asString());assertFalse(candidates.getContentAsString().contains("verifiedEmail"));
            Map<String,Object> clear=new HashMap<>();clear.put("assigneeAccountId",null);clear.put("reasonCode","UNASSIGNMENT");assertEquals(409,onboardingWrite(mvc,owner,"PUT",path,UUID.randomUUID(),"\"a0\"",clear).getStatus());assertEquals(200,onboardingWrite(mvc,owner,"PUT",path,UUID.randomUUID(),"\"a1\"",clear).getStatus());
            assertEquals(2,intakeJson(httpRead(mvc,path,owner.access())).get("version").asLong());assertEquals(assigned.getContentAsString(),onboardingWrite(mvc,owner,"PUT",path,key,"\"a0\"",Map.of("assigneeAccountId",A,"reasonCode","INITIAL_ASSIGNMENT")).getContentAsString());assertEquals(2,intakeJson(httpRead(mvc,base+"/shipments/"+shipment+"/internal-history",owner.access())).get("items").size());assertEquals(1,intakeJson(httpRead(mvc,base+"/shipment-assignment-receipts/"+key,owner.access())).get("version").asLong());
            assertEquals(0,scalar("SELECT count(*) FROM orders.pilot_tracking_event"));assertEquals(0,scalar("SELECT count(*) FROM dispatch.pilot_custody"));assertEquals(1,scalar("SELECT count(*) FROM finance.pilot_shipping_obligation WHERE state='UNPAID'"));assertEquals(1,scalar("SELECT count(*) FROM orders.pilot_shipment WHERE carrier_state='NOT_ACCEPTED'"));
            businessCommand(UUID.randomUUID(),"REVOKE",org,A,"OWNER","'shipment.assign'");assertEquals(404,httpRead(mvc,base+"/shipment-assignment-receipts/"+key,owner.access()).getStatus());assertEquals(404,httpRead(mvc,base+"/shipments/"+shipment+"/assignment-candidates",owner.access()).getStatus());
        }
    }
    @Test void businessManagementExportIsBoundedAuditedAndPiiPermissionIsIndependent()throws Exception{
        customerFixture(false);try(var inbox=new CustomerSmtpTestInbox(0,false,false,250,null);var c=customerHttpContext(PILOT_NOW,smtp(inbox,PILOT_NOW),true)){
            var mvc=httpMvc(c);var owner=profileBrowser(mvc);var stranger=addressOtherBrowser(mvc);UUID org=id(8813);businessManagementOwner(org);String shipment=businessManagementBooking(mvc,owner,org),path=businessShippingBase(org)+"/shipment-exports";
            var response=httpRead(mvc,path,owner.access());assertEquals(200,response.getStatus(),this::safeOnboardingFailure);assertEquals("no-store",response.getHeader("Cache-Control"));var result=intakeJson(response);assertEquals(1,result.get("rowCount").asInt());assertFalse(result.get("piiIncluded").asBoolean());assertEquals(15,result.get("columns").size());assertFalse(result.get("rows").toString().contains("+849"));
            var pii=httpRead(mvc,path+"?includePii=true&ids="+shipment,owner.access());assertEquals(200,pii.getStatus(),this::safeOnboardingFailure);assertEquals(18,intakeJson(pii).get("columns").size());assertTrue(intakeJson(pii).get("rows").get(0).get(16).asString().startsWith("+84"));assertEquals(2,businessScalar("SELECT count(*) FROM orders.organization_shipment_export_audit"));assertThrows(SQLException.class,()->execute("SELECT * FROM orders.organization_shipment_export_audit"));
            assertEquals(404,httpRead(mvc,path+"?ids="+shipment+","+UUID.randomUUID(),owner.access()).getStatus());assertEquals(404,httpRead(mvc,path,stranger.access()).getStatus());assertEquals(400,httpRead(mvc,path+"?page=0",owner.access()).getStatus());assertEquals(2,businessScalar("SELECT count(*) FROM orders.organization_shipment_export_audit"));
            db.ownerExecute("REVOKE INSERT ON orders.organization_shipment_export_audit FROM \""+db.runtime()+"\"");try{assertEquals(503,httpRead(mvc,path,owner.access()).getStatus());}finally{db.ownerExecute("GRANT INSERT ON orders.organization_shipment_export_audit TO \""+db.runtime()+"\"");}assertEquals(2,businessScalar("SELECT count(*) FROM orders.organization_shipment_export_audit"));
            businessCommand(UUID.randomUUID(),"REVOKE",org,A,"OWNER","'shipment.export.pii'");assertEquals(404,httpRead(mvc,path+"?includePii=true",owner.access()).getStatus());assertEquals(200,httpRead(mvc,path,owner.access()).getStatus());businessCommand(UUID.randomUUID(),"REVOKE",org,A,"OWNER","'shipment.export'");assertEquals(404,httpRead(mvc,path,owner.access()).getStatus());assertEquals(200,httpRead(mvc,businessShippingBase(org)+"/shipments?view=management",owner.access()).getStatus());
            assertThrows(SQLException.class,()->db.ownerExecute("DELETE FROM orders.organization_shipment_export_audit"));
        }
    }
    @Test void businessManagementAssignmentConcurrencyAndEvidenceFailuresRollback()throws Exception{
        customerFixture(false);try(var inbox=new CustomerSmtpTestInbox(0,false,false,250,null);var c=customerHttpContext(PILOT_NOW,smtp(inbox,PILOT_NOW),true)){
            var mvc=httpMvc(c);var owner=profileBrowser(mvc);UUID org=id(8814);businessManagementOwner(org);String shipment=businessManagementBooking(mvc,owner,org),path=businessShippingBase(org)+"/shipments/"+shipment+"/assignment";var body=Map.of("assigneeAccountId",A,"reasonCode","INITIAL_ASSIGNMENT");
            for(String table:List.of("orders.organization_shipment_assignment_event","orders.organization_shipment_assignment_receipt")){
                db.ownerExecute("REVOKE INSERT ON "+table+" FROM \""+db.runtime()+"\"");try{assertEquals(503,onboardingWrite(mvc,owner,"PUT",path,UUID.randomUUID(),"\"a0\"",body).getStatus());assertEquals(0,scalar("SELECT count(*) FROM orders.organization_shipment_assignment"));assertEquals(0,scalar("SELECT count(*) FROM orders.organization_shipment_assignment_event"));assertEquals(0,scalar("SELECT count(*) FROM orders.organization_shipment_assignment_receipt"));}finally{db.ownerExecute("GRANT INSERT ON "+table+" TO \""+db.runtime()+"\"");}
            }
            UUID key=UUID.randomUUID();assertEquals(List.of(200,200),adminRace(()->onboardingWrite(mvc,owner,"PUT",path,key,"\"a0\"",body).getStatus(),()->onboardingWrite(mvc,owner,"PUT",path,key,"\"a0\"",body).getStatus()));assertEquals(1,scalar("SELECT count(*) FROM orders.organization_shipment_assignment_event"));
            Map<String,Object> clear=new HashMap<>();clear.put("assigneeAccountId",null);clear.put("reasonCode","UNASSIGNMENT");assertEquals(List.of(200,409),adminRace(()->onboardingWrite(mvc,owner,"PUT",path,UUID.randomUUID(),"\"a1\"",clear).getStatus(),()->onboardingWrite(mvc,owner,"PUT",path,UUID.randomUUID(),"\"a1\"",clear).getStatus()).stream().map(Integer.class::cast).sorted().toList());assertEquals(2,scalar("SELECT count(*) FROM orders.organization_shipment_assignment_receipt"));
            assertThrows(SQLException.class,()->execute("UPDATE orders.organization_shipment_assignment SET assignee_account_id='"+A+"',version=version+1"));assertEquals(2,scalar("SELECT version FROM orders.organization_shipment_assignment"));assertThrows(SQLException.class,()->execute("DELETE FROM orders.organization_shipment_assignment_event"));assertThrows(SQLException.class,()->execute("UPDATE orders.organization_shipment_assignment_receipt SET fingerprint=decode(repeat('00',32),'hex')"));
            addressOtherBrowser(mvc);businessCommand(UUID.randomUUID(),"GRANT",org,B,"OPERATOR","'dashboard.read','shipment.draft.read'");
            var raced=adminRace(()->onboardingWrite(mvc,owner,"PUT",path,UUID.randomUUID(),"\"a2\"",Map.of("assigneeAccountId",B,"reasonCode","INITIAL_ASSIGNMENT")).getStatus(),()->{businessCommand(UUID.randomUUID(),"REVOKE",org,B,"OPERATOR","'shipment.draft.read'");return "REVOKED";});
            assertEquals("REVOKED",raced.get(1));assertTrue(Set.of(200,404).contains(raced.get(0)));assertEquals(raced.get(0).equals(200)?3:2,scalar("SELECT count(*) FROM orders.organization_shipment_assignment_event"));
            assertEquals(0,scalar("SELECT count(*) FROM orders.pilot_tracking_event"));assertEquals(1,scalar("SELECT count(*) FROM finance.pilot_shipping_obligation WHERE state='UNPAID'"));
        }
    }
    @Test void businessManagementFacilityScopeAndTargetRevocationNeverWidenAccess()throws Exception{
        customerFixture(false);try(var inbox=new CustomerSmtpTestInbox(0,false,false,250,null);var c=customerHttpContext(PILOT_NOW,smtp(inbox,PILOT_NOW),true)){
            var mvc=httpMvc(c);var owner=profileBrowser(mvc);var other=addressOtherBrowser(mvc);UUID org=id(8815),site=id(8816);businessManagementOwner(org);String noSite=businessManagementBooking(mvc,owner,org);
            String contact=new tools.jackson.databind.ObjectMapper().writeValueAsString(shippingDraftInput("Site").get("sender")).replace("0901234567","+84901234567");seedFacility(site,org,contact);
            var input=new HashMap<>(pilotDraftInput("Scoped management"));input.put("facilityId",site);String draft=responseId(onboardingWrite(mvc,owner,"POST",preparationPath(org),UUID.randomUUID(),null,input),"id");var quote=businessTestQuote(mvc,owner,org,draft,UUID.randomUUID());assertEquals(201,quote.getStatus(),this::safeOnboardingFailure);var booking=businessTestBook(mvc,owner,org,draft,intakeJson(quote).get("quote").get("id").asString(),UUID.randomUUID());assertEquals(201,booking.getStatus(),this::safeOnboardingFailure);String shipment=intakeJson(booking).get("shipment").get("id").asString(),base=businessShippingBase(org),path=base+"/shipments/"+shipment+"/assignment";
            businessCommand(UUID.randomUUID(),"GRANT",org,B,"OPERATOR","'dashboard.read'");seedScopedLegacyGrants(org,B,site,"'shipment.draft.read','shipment.assign','shipment.export','shipment.label.read'");
            var page=intakeJson(httpRead(mvc,base+"/shipments?view=management",other.access()));assertEquals("1",page.get("total").asString());assertEquals("53000",page.get("summary").get("confirmedFeesVnd").asString());assertEquals(shipment,page.get("items").get(0).get("record").get("shipment").get("id").asString());assertEquals(404,httpRead(mvc,base+"/shipments/"+noSite,other.access()).getStatus());assertEquals(404,httpRead(mvc,base+"/shipments/"+noSite+"/labels",other.access()).getStatus());assertEquals(404,httpRead(mvc,base+"/shipments/"+noSite+"/internal-history",other.access()).getStatus());assertEquals(404,httpRead(mvc,base+"/shipments?view=management&facilityId="+UUID.randomUUID(),other.access()).getStatus());assertEquals(1,intakeJson(httpRead(mvc,base+"/shipment-exports",other.access())).get("rowCount").asInt());assertEquals(404,httpRead(mvc,base+"/shipment-exports?ids="+shipment+","+noSite,other.access()).getStatus());
            UUID key=UUID.randomUUID();assertEquals(200,onboardingWrite(mvc,owner,"PUT",path,key,"\"a0\"",Map.of("assigneeAccountId",B,"reasonCode","INITIAL_ASSIGNMENT")).getStatus());assertEquals(404,httpRead(mvc,base+"/shipment-assignment-receipts/"+key,other.access()).getStatus());
            businessCommand(UUID.randomUUID(),"REVOKE",org,B,"OPERATOR","'shipment.assign','shipment.export','shipment.label.read','shipment.draft.read'");assertEquals(404,onboardingWrite(mvc,owner,"PUT",base+"/shipments/"+noSite+"/assignment",UUID.randomUUID(),"\"a0\"",Map.of("assigneeAccountId",B,"reasonCode","INITIAL_ASSIGNMENT")).getStatus());assertEquals(1,intakeJson(httpRead(mvc,base+"/shipments/"+shipment+"/assignment-candidates",owner.access())).get("items").size());assertEquals(404,httpRead(mvc,base+"/shipments?view=management",other.access()).getStatus());
        }
    }
    @Test void businessManagementImportSourcesLiteralSearchAndSortUseBookedSnapshot()throws Exception{
        customerFixture(false);try(var inbox=new CustomerSmtpTestInbox(0,false,false,250,null);var c=customerHttpContext(PILOT_NOW,smtp(inbox,PILOT_NOW),true)){
            var mvc=httpMvc(c);var owner=profileBrowser(mvc);UUID org=id(8817);businessManagementOwner(org);businessCommand(UUID.randomUUID(),"GRANT",org,A,"OWNER","'dashboard.read','shipment.draft.read','shipment.draft.write','shipment.import'");String base=businessShippingBase(org);String existing=businessManagementBooking(mvc,owner,org);
            String job=responseId(onboardingWrite(mvc,owner,"POST",base+"/shipment-imports",UUID.randomUUID(),null,Map.of("templateVersion","BUSINESS_CSV_V1","csv",businessCsv(1))),"id");assertEquals(200,onboardingWrite(mvc,owner,"POST",base+"/shipment-imports/"+job+"/commit",UUID.randomUUID(),"\"v1\"",Map.of("rows",List.of(1))).getStatus());
            String draft;try(var con=db.runtimeConnection();var s=con.createStatement();var r=s.executeQuery("SELECT preparation_id FROM orders.organization_import_row WHERE job_id='"+job+"'")){assertTrue(r.next());draft=r.getString(1);}var q=businessTestQuote(mvc,owner,org,draft,UUID.randomUUID());assertEquals(201,q.getStatus(),this::safeOnboardingFailure);var booked=businessTestBook(mvc,owner,org,draft,intakeJson(q).get("quote").get("id").asString(),UUID.randomUUID());assertEquals(201,booked.getStatus(),this::safeOnboardingFailure);String imported=intakeJson(booked).get("shipment").get("id").asString();
            var filtered=httpRead(mvc,base+"/shipments?view=management&batchId="+job,owner.access());assertEquals(200,filtered.getStatus(),this::safeOnboardingFailure);var row=intakeJson(filtered).get("items").get(0);assertEquals(imported,row.get("record").get("shipment").get("id").asString());assertEquals(job,row.get("source").get("importJobIds").get(0).asString());String channel=row.get("source").get("sourceChannel").asString();
            for(String suffix:List.of("&search=SOURCE-1","&creatorId="+A,"&assignee=NONE","&origin=SIM-A1&destination=SIM-A2","&search=0901234567"))assertEquals("1",intakeJson(httpRead(mvc,base+"/shipments?view=management&batchId="+job+suffix,owner.access())).get("total").asString(),suffix);
            var international=mvc.perform(MockMvcRequestBuilders.get(base+"/shipments").param("view","management").param("batchId",job).param("search","+84901234567").secure(true).header("Authorization","Bearer "+owner.access())).andReturn().getResponse();assertEquals("1",intakeJson(international).get("total").asString());
            if(!channel.isBlank())assertEquals("1",intakeJson(httpRead(mvc,base+"/shipments?view=management&sourceChannel="+channel+"&batchId="+job,owner.access())).get("total").asString());
            var ascending=intakeJson(httpRead(mvc,base+"/shipments?view=management&sort=FEE_ASC",owner.access()));assertEquals("2",ascending.get("total").asString());assertEquals(imported,ascending.get("items").get(0).get("record").get("shipment").get("id").asString());assertEquals(existing,intakeJson(httpRead(mvc,base+"/shipments?view=management&sort=FEE_DESC",owner.access())).get("items").get(0).get("record").get("shipment").get("id").asString());assertEquals(404,httpRead(mvc,base+"/shipments?view=management&batchId="+UUID.randomUUID(),owner.access()).getStatus());
        }
    }
    @Test void businessManagementDisabledAndProductionCannotReadExportOrAssign()throws Exception{
        customerFixture(false);UUID org=id(8818);String shipment;
        try(var inbox=new CustomerSmtpTestInbox(0,false,false,250,null);var c=customerHttpContext(PILOT_NOW,smtp(inbox,PILOT_NOW),true)){var mvc=httpMvc(c);var owner=profileBrowser(mvc);businessManagementOwner(org);shipment=businessManagementBooking(mvc,owner,org);}
        for(String mode:List.of("DISABLED","PRODUCTION","TEST_WITH_PRODUCTION_PROFILE"))try(var inbox=new CustomerSmtpTestInbox(0,false,false,250,null);var c=customerHttpContext(PILOT_NOW,smtp(inbox,PILOT_NOW),true,mode)){
            var mvc=httpMvc(c);var owner=profileBrowser(mvc);String base=businessShippingBase(org);for(String suffix:List.of("/shipments?view=management","/shipment-exports","/shipments/"+shipment+"/assignment","/shipments/"+shipment+"/internal-history","/shipments/"+shipment+"/assignment-candidates"))assertEquals(503,httpRead(mvc,base+suffix,owner.access()).getStatus());assertEquals(503,onboardingWrite(mvc,owner,"PUT",base+"/shipments/"+shipment+"/assignment",UUID.randomUUID(),"\"a0\"",Map.of("assigneeAccountId",A,"reasonCode","INITIAL_ASSIGNMENT")).getStatus());
        }assertEquals(0,scalar("SELECT count(*) FROM orders.organization_shipment_assignment"));assertEquals(0,businessScalar("SELECT count(*) FROM orders.organization_shipment_export_audit"));
    }

    @Test void businessManagementExportLimitNeverSilentlyTruncatesCohort()throws Exception{
        customerFixture(false);try(var inbox=new CustomerSmtpTestInbox(0,false,false,250,null);var c=customerHttpContext(PILOT_NOW,smtp(inbox,PILOT_NOW),true)){
            var mvc=httpMvc(c);var owner=profileBrowser(mvc);UUID org=id(8819);businessManagementOwner(org);String first=null;for(int i=0;i<201;i++){String shipment=businessManagementBooking(mvc,owner,org);if(first==null)first=shipment;}
            String base=businessShippingBase(org);var page=httpRead(mvc,base+"/shipments?view=management",owner.access());assertEquals(200,page.getStatus(),this::safeOnboardingFailure);var data=intakeJson(page);assertEquals("201",data.get("total").asString());assertEquals("10653000",data.get("summary").get("unpaidFeesVnd").asString());assertEquals(20,data.get("items").size());assertTrue(data.get("hasNext").asBoolean());
            var denied=httpRead(mvc,base+"/shipment-exports",owner.access());assertEquals(422,denied.getStatus());assertTrue(denied.getContentAsString().contains("EXPORT_LIMIT_EXCEEDED"));assertEquals(0,businessScalar("SELECT count(*) FROM orders.organization_shipment_export_audit"));
            var selected=httpRead(mvc,base+"/shipment-exports?ids="+first,owner.access());assertEquals(200,selected.getStatus(),this::safeOnboardingFailure);assertEquals(1,intakeJson(selected).get("rowCount").asInt());assertEquals(1,businessScalar("SELECT count(*) FROM orders.organization_shipment_export_audit"));assertEquals(201,scalar("SELECT count(*) FROM finance.pilot_shipping_obligation WHERE state='UNPAID'"));
        }
    }
    private void businessShippingOwner(UUID org)throws Exception{businessCommand(UUID.randomUUID(),"PROVISION",org,A,"OWNER","'dashboard.read','shipment.draft.read','shipment.draft.write','shipment.quote','shipment.book','shipment.label.read'");}
    private void businessTestContract(UUID org)throws Exception{db.ownerExecute("INSERT INTO pricing.organization_pilot_contract VALUES(gen_random_uuid(),1,'"+org+"','ARGO-ROAD-TARIFF-1','ACTIVE',TIMESTAMPTZ '2026-11-01 00:00:00Z',NULL,'SYNTHETIC_BSC06_TEST','"+UUID.randomUUID()+"',current_user,clock_timestamp())");}
    private String businessTestDraft(org.springframework.test.web.servlet.MockMvc mvc,ProfileBrowser owner,UUID org)throws Exception{var r=onboardingWrite(mvc,owner,"POST",preparationPath(org),UUID.randomUUID(),null,pilotDraftInput("Synthetic Business Booking"));assertEquals(201,r.getStatus(),this::safeOnboardingFailure);return responseId(r,"id");}
    private org.springframework.mock.web.MockHttpServletResponse businessTestQuote(org.springframework.test.web.servlet.MockMvc mvc,ProfileBrowser owner,UUID org,String draft,UUID key)throws Exception{return onboardingWrite(mvc,owner,"POST",preparationPath(org)+"/"+draft+"/quotes",key,"\"v1\"",Map.of("originCode","SIM-A1","destinationCode","SIM-A2","ordinaryGoodsConfirmed",true));}
    private org.springframework.mock.web.MockHttpServletResponse businessTestBook(org.springframework.test.web.servlet.MockMvc mvc,ProfileBrowser owner,UUID org,String draft,String quote,UUID key)throws Exception{return onboardingWrite(mvc,owner,"POST",preparationPath(org)+"/"+draft+"/bookings",key,"\"v1\"",Map.of("quoteId",quote));}
    @Test void businessQuoteUsesSeparateContractExactTariffAndScopedReceipts()throws Exception{
        customerFixture(true);try(var inbox=new CustomerSmtpTestInbox(0,false,false,250,null);var c=customerHttpContext(PILOT_NOW,smtp(inbox,PILOT_NOW),true)){
            var mvc=httpMvc(c);var owner=profileBrowser(mvc);UUID org=id(870);businessShippingOwner(org);String base=businessShippingBase(org),draft=businessTestDraft(mvc,owner,org);UUID key=UUID.randomUUID();
            assertFalse(intakeJson(httpRead(mvc,base+"/shipping-catalog",owner.access())).get("quoteEnabled").asBoolean());assertEquals(503,businessTestQuote(mvc,owner,org,draft,key).getStatus());businessTestContract(org);
            var first=businessTestQuote(mvc,owner,org,draft,key);assertEquals(201,first.getStatus(),this::safeOnboardingFailure);var q=intakeJson(first).get("quote");assertEquals(53000,q.get("totalVnd").asLong());assertFalse(q.has("accountId"));assertEquals(org.toString(),q.get("organizationId").asString());assertEquals(900,Duration.between(Instant.parse(q.get("createdAt").asString()),Instant.parse(q.get("expiresAt").asString())).toSeconds());assertTrue(intakeJson(first).get("usable").asBoolean());
            assertEquals(first.getContentAsString(),businessTestQuote(mvc,owner,org,draft,key).getContentAsString());assertTrue(httpRead(mvc,base+"/shipping-quote-receipts/"+key,owner.access()).getContentAsString().contains(q.get("id").asString()));
            assertEquals(409,onboardingWrite(mvc,owner,"POST",preparationPath(org)+"/"+draft+"/quotes",key,"\"v1\"",Map.of("originCode","SIM-A1","destinationCode","SIM-B1","ordinaryGoodsConfirmed",true)).getStatus());
            assertEquals(422,onboardingWrite(mvc,owner,"POST",preparationPath(org)+"/"+draft+"/quotes",UUID.randomUUID(),"\"v1\"",Map.of("originCode","SIM-A2","destinationCode","SIM-B1","ordinaryGoodsConfirmed",true)).getStatus());
            for(var route:List.of(new String[]{"SIM-A2","SIM-A1","53000"},new String[]{"SIM-A1","SIM-B1","81000"},new String[]{"SIM-B1","SIM-A1","81000"})){var priced=onboardingWrite(mvc,owner,"POST",preparationPath(org)+"/"+draft+"/quotes",UUID.randomUUID(),"\"v1\"",Map.of("originCode",route[0],"destinationCode",route[1],"ordinaryGoodsConfirmed",true));assertEquals(201,priced.getStatus(),this::safeOnboardingFailure);assertEquals(Long.parseLong(route[2]),intakeJson(priced).get("quote").get("totalVnd").asLong());}
            assertEquals(404,httpRead(mvc,"/api/v1/customer/shipping-quotes/"+q.get("id").asString(),owner.access()).getStatus());assertEquals(4,scalar("SELECT count(*) FROM pricing.pilot_quote WHERE owner_type='BUSINESS' AND owner_id IS NULL AND draft_id IS NULL"));assertEquals(0,scalar("SELECT count(*) FROM orders.pilot_shipment"));
        }
    }
    private String businessSnapshot(String table){if(!table.equals("orders.pilot_shipment"))throw new IllegalArgumentException();return new org.springframework.jdbc.core.JdbcTemplate(outboxDataSource()).queryForObject("SELECT md5(COALESCE(jsonb_agg(to_jsonb(s) ORDER BY id)::text,'')) FROM orders.pilot_shipment s",String.class);}
    private org.springframework.mock.web.MockHttpServletResponse biaWrite(org.springframework.test.web.servlet.MockMvc mvc,ProfileBrowser browser,String path,UUID key,String version,Object body)throws Exception{
        var q=MockMvcRequestBuilders.post(path).secure(true).header("Origin","https://portal.example.invalid").header("Authorization","Bearer "+browser.access()).header("X-CSRF-Token",browser.flow().challenge).cookie(browser.flow().csrf,browser.flow().refresh).contentType("application/json").accept("application/vnd.argopost.station-cargo.v2+json").header("Idempotency-Key",key);
        if(version!=null)q.header("If-Match",version);q.content(new tools.jackson.databind.ObjectMapper().writeValueAsString(body));var r=mvc.perform(q).andReturn();onboardingFailure.set(r.getResolvedException());return r.getResponse();
    }
    private org.springframework.mock.web.MockHttpServletResponse biaRead(org.springframework.test.web.servlet.MockMvc mvc,ProfileBrowser browser,String path)throws Exception{var r=mvc.perform(MockMvcRequestBuilders.get(path).secure(true).header("Authorization","Bearer "+browser.access()).accept("application/vnd.argopost.station-cargo.v2+json")).andReturn();onboardingFailure.set(r.getResolvedException());return r.getResponse();}
    private void biaPortsRejectForeignFacts(org.springframework.context.ApplicationContext c,String token,UUID site,UUID org,UUID shipment){
        var admission=c.getBean(com.elogistic.backend.identity.api.staff.OperationalStaffAdmission.class);var coverage=c.getBean(com.elogistic.backend.merchant.api.customer.PilotCoverage.class);var intake=c.getBean(com.elogistic.backend.dispatch.api.OperationalStationIntake.class);var handling=c.getBean(com.elogistic.backend.orders.api.PilotHandling.class);var finance=c.getBean(com.elogistic.backend.finance.api.BusinessObligations.class);
        UUID currentRequest=c.getBean("operationsRequests",com.elogistic.backend.identity.api.staff.StaffRequestContext.class).current().requestId();
        admission.execute(token,currentRequest,ctx->coverage.withIntakeOrganizationLock(ctx,()->{
            var access=new com.elogistic.backend.orders.api.PilotHandling.CarrierAccess(){public UUID actorId(){requireCurrent();return ctx.actor().accountId();}public UUID requestId(){requireCurrent();return ctx.requestId();}public String stationCode(){return "SIM-DROP-A1";}public void requireCurrent(){admission.requireCurrent(ctx);coverage.requireIntakeOrganization(ctx,org,null);intake.require(ctx,site);}};
            var cargo=handling.carrierAtStation(access,shipment);com.elogistic.backend.finance.api.PilotObligations.HandoverAccess guard=access::requireCurrent;
            finance.requireUnpaidHandover(guard,org,null,shipment,cargo.quoteId(),cargo.totalVnd());
            assertThrows(RuntimeException.class,()->finance.requireUnpaidHandover(guard,UUID.randomUUID(),null,shipment,cargo.quoteId(),cargo.totalVnd()));
            assertThrows(RuntimeException.class,()->finance.requireUnpaidHandover(guard,org,UUID.randomUUID(),shipment,cargo.quoteId(),cargo.totalVnd()));
            assertThrows(RuntimeException.class,()->finance.requireUnpaidHandover(guard,org,null,shipment,UUID.randomUUID(),cargo.totalVnd()));
            assertThrows(RuntimeException.class,()->finance.requireUnpaidHandover(guard,org,null,shipment,cargo.quoteId(),cargo.totalVnd()+1));
            assertThrows(RuntimeException.class,()->coverage.requireIntakeOrganization(ctx,org,UUID.randomUUID()));
            var forged=new com.elogistic.backend.orders.api.PilotHandling.CarrierCargo(cargo.shipmentId(),cargo.shipmentCode(),cargo.ownerType(),cargo.accountId(),cargo.organizationId(),cargo.facilityId(),cargo.quoteId(),cargo.totalVnd(),cargo.createdAt().plusSeconds(1),cargo.input(),cargo.route(),cargo.parcels(),cargo.handling());
            assertThrows(com.elogistic.backend.orders.api.PilotHandlingFailure.class,()->intake.scan(ctx,site,forged,cargo.handling().parcels().getFirst().barcode(),UUID.randomUUID()));
            var beforeBooking=new com.elogistic.backend.dispatch.internal.infrastructure.JdbcStationIntake(c.getBean(javax.sql.DataSource.class),Clock.fixed(cargo.createdAt().minusMillis(1),ZoneOffset.UTC),true,"TEST",admission,handling,true);
            assertEquals(com.elogistic.backend.orders.api.PilotHandlingFailure.Code.PARCEL_SET_INCOMPLETE,assertThrows(com.elogistic.backend.orders.api.PilotHandlingFailure.class,()->beforeBooking.scan(ctx,site,cargo,cargo.handling().parcels().getFirst().barcode(),UUID.randomUUID())).code());
            var earlyClock=new com.elogistic.backend.dispatch.internal.infrastructure.JdbcStationIntake(c.getBean(javax.sql.DataSource.class),Clock.fixed(cargo.createdAt().plusMillis(1),ZoneOffset.UTC),true,"TEST",admission,handling,true);
            var scanIds=intake.scans(ctx,site,shipment).stream().map(com.elogistic.backend.dispatch.api.StationIntake.Scan::id).toList();
            assertEquals(com.elogistic.backend.orders.api.PilotHandlingFailure.Code.PARCEL_SET_INCOMPLETE,assertThrows(com.elogistic.backend.orders.api.PilotHandlingFailure.class,()->earlyClock.accept(ctx,site,cargo,scanIds,0,"MATCHED",UUID.randomUUID())).code());
            return null;
        }));
    }
    /** Deliberately malformed direct DML only; every transaction must fail and roll back. */
    private void biaIncompleteDmlDenied(String shipment,String mode)throws Exception{
        UUID acceptance=UUID.randomUUID(),event=UUID.randomUUID(),request=UUID.randomUUID();String time=mode.equals("TIME")?"TIMESTAMPTZ '2000-01-01T00:00:00Z'":"TIMESTAMPTZ '"+Instant.now()+"'";
        try(var c=db.runtimeConnection();var q=c.createStatement()){
            c.setAutoCommit(false);
            try{
                q.executeUpdate("INSERT INTO dispatch.pilot_intake_acceptance SELECT '"+acceptance+"','"+shipment+"',"+(mode.equals("ACTOR")?"'"+UUID.randomUUID()+"'":"actor_id")+",assignment_id,"+(mode.equals("SITE")?"'SIM-DROP-B1'":"station_code")+",2,"+time+",'"+request+"',0,'MATCHED','BR13-BUSINESS-INTAKE-01',environment,provenance,operational_assignment_id,site_version_id,site_id,"+(mode.equals("OWNER")?"'PERSONAL'":"owner_type")+" FROM dispatch.pilot_intake_acceptance LIMIT 1");
                if(!mode.equals("CUSTODY"))q.executeUpdate("INSERT INTO dispatch.pilot_custody SELECT '"+acceptance+"',parcel_id,id,shipment_id,station_code,actor_id,"+time+" FROM dispatch.pilot_intake_scan WHERE shipment_id='"+shipment+"'");
                if(!mode.equals("TRACKING")){
                    q.executeUpdate("INSERT INTO orders.pilot_tracking_event(id,shipment_id,owner_type,owner_id,organization_id,facility_id,source_acceptance_id,event_type,station_code,actor_id,occurred_at,version,environment,provenance) SELECT '"+event+"',a.shipment_id,a.owner_type,NULL,"+(mode.equals("ORG")?"'"+UUID.randomUUID()+"'":"b.organization_id")+",b.facility_id,'"+(mode.equals("SOURCE")?UUID.randomUUID():acceptance)+"','CARRIER_ACCEPTED_AT_STATION',a.station_code,a.actor_id,"+time+",1,a.environment,'SIMULATION' FROM dispatch.pilot_intake_acceptance a JOIN orders.pilot_shipment b ON b.id=a.shipment_id WHERE a.id='"+acceptance+"'");
                    q.executeUpdate("INSERT INTO orders.pilot_parcel_event SELECT '"+event+"',shipment_id,"+(mode.equals("PARCEL")?"'"+UUID.randomUUID()+"'":"id")+",'CARRIER_ACCEPTED_AT_STATION',"+time+" FROM orders.pilot_parcel WHERE shipment_id='"+shipment+"'");
                }
                if(!mode.equals("AUDIT"))q.executeUpdate("INSERT INTO dispatch.pilot_intake_audit SELECT gen_random_uuid(),actor_id,station_code,'ACCEPT',id,shipment_id,request_id,occurred_at FROM dispatch.pilot_intake_acceptance WHERE id='"+acceptance+"'");
                if(!mode.equals("RECEIPT"))q.executeUpdate("INSERT INTO dispatch.pilot_intake_receipt SELECT actor_id,station_code,gen_random_uuid(),'ACCEPT',decode(repeat('01',32),'hex'),id,shipment_id,occurred_at FROM dispatch.pilot_intake_acceptance WHERE id='"+acceptance+"'");
                var failure=assertThrows(SQLException.class,c::commit,"Malformed "+mode+" must never commit");assertTrue(Set.of("23503","23514","42501","23505").contains(failure.getSQLState()));
            }catch(SQLException failure){assertTrue(Set.of("23503","23514","42501","23505").contains(failure.getSQLState()));}
            finally{c.rollback();}
        }
        assertEquals(1,scalar("SELECT count(*) FROM dispatch.pilot_intake_acceptance"));assertEquals(2,scalar("SELECT count(*) FROM dispatch.pilot_custody"));assertEquals(1,scalar("SELECT count(*) FROM orders.pilot_tracking_event"));
    }
    @Test void biaBusinessIntakeUsesCanonicalTimestampOwnershipAndAtomicPipeline()throws Exception{
        customerFixture(true);ensureStaffOperator();var op=nwsOperator();UUID actor=nwsStaff(op),site=nwsSite(op,"SIM-DROP-A1","STATION","SIM-A1",UUID.randomUUID()),hub=nwsSite(op,"SIM-HUB-A","HUB","SIM-A1",UUID.randomUUID());nwsLink(op,site,hub);nwsAssignment(op,actor,site,"SITE_READ",false);nwsAssignment(op,actor,site,"STATION_INTAKE",false);
        nwsHttpEnabled=true;biaHttpEnabled=true;liveStaffClock=true;Instant time=Instant.now();
        try(var inbox=new CustomerSmtpTestInbox(0,false,false,250,null);var c=customerHttpContext(time,smtp(inbox,time),true)){
            var mvc=httpMvc(c);var owner=profileBrowser(mvc);var staff=nwsBrowser(mvc);UUID org=id(878);businessShippingOwner(org);businessTestContract(org);String draft=businessTestDraft(mvc,owner,org);var qr=businessTestQuote(mvc,owner,org,draft,UUID.randomUUID());assertEquals(201,qr.getStatus(),this::safeOnboardingFailure);String quote=intakeJson(qr).get("quote").get("id").asString();var booked=businessTestBook(mvc,owner,org,draft,quote,UUID.randomUUID());assertEquals(201,booked.getStatus(),this::safeOnboardingFailure);var shipment=intakeJson(booked).get("shipment");String id=shipment.get("id").asString(),base="/api/v1/ops/sites/"+site;
            String snapshot=businessSnapshot("orders.pilot_shipment");var view=biaRead(mvc,staff,base+"/shipments/"+id);assertEquals(200,view.getStatus(),this::safeOnboardingFailure);var cargo=intakeJson(view).get("cargo");assertEquals("BUSINESS",cargo.get("ownerType").asString());assertEquals(org.toString(),cargo.get("organizationId").asString());assertEquals(shipment.get("createdAt").asString(),cargo.get("createdAt").asString());assertTrue(cargo.get("accountId").isNull());
            assertEquals(401,biaRead(mvc,owner,base+"/shipments/"+id).getStatus());assertEquals(403,biaRead(mvc,staff,"/api/v1/ops/sites/"+hub+"/shipments/"+id).getStatus());assertEquals(404,httpRead(mvc,base+"/shipments/"+id,staff.access()).getStatus());
            List<String> ids=new ArrayList<>();for(var parcel:shipment.get("parcels")){String barcode="AP-P-"+parcel.get("id").asString().replace("-","");UUID key=UUID.randomUUID();var scan=biaWrite(mvc,staff,base+"/scans",key,null,Map.of("barcode",barcode));assertEquals(200,scan.getStatus(),this::safeOnboardingFailure);assertEquals(scan.getContentAsString(),biaWrite(mvc,staff,base+"/scans",key,null,Map.of("barcode",barcode)).getContentAsString());ids.add(responseId(scan,"id"));}
            assertEquals(409,biaWrite(mvc,staff,base+"/shipments/"+id+"/acceptance",UUID.randomUUID(),"\"i0\"",Map.of("scanIds",ids.subList(0,1),"inspection","MATCHED")).getStatus());
            assertEquals(400,biaWrite(mvc,staff,base+"/shipments/"+id+"/acceptance",UUID.randomUUID(),"\"i0\"",Map.of("scanIds",List.of(ids.getFirst(),ids.getFirst()),"inspection","MATCHED")).getStatus());
            assertEquals(404,biaWrite(mvc,staff,base+"/shipments/"+id+"/acceptance",UUID.randomUUID(),"\"i0\"",Map.of("scanIds",List.of(ids.getFirst(),UUID.randomUUID().toString()),"inspection","MATCHED")).getStatus());
            assertEquals(412,biaWrite(mvc,staff,base+"/shipments/"+id+"/acceptance",UUID.randomUUID(),"\"i1\"",Map.of("scanIds",ids,"inspection","MATCHED")).getStatus());
            jakarta.servlet.Filter probe=(request,response,chain)->{biaPortsRejectForeignFacts(c,staff.access(),site,org,UUID.fromString(id));chain.doFilter(request,response);};
            var probeMvc=MockMvcBuilders.webAppContextSetup(c).addFilters(c.getBean("springSecurityFilterChain",jakarta.servlet.Filter.class),probe).build();
            assertEquals(200,biaRead(probeMvc,staff,base+"/shipments/"+id).getStatus(),this::safeOnboardingFailure);
            UUID key=UUID.randomUUID();var input=Map.of("scanIds",ids,"inspection","MATCHED");var acceptance=biaWrite(mvc,staff,base+"/shipments/"+id+"/acceptance",key,"\"i0\"",input);assertEquals(200,acceptance.getStatus(),this::safeOnboardingFailure);assertEquals("AT_STATION",intakeJson(acceptance).get("cargo").get("handling").get("state").asString());assertEquals(200,biaWrite(mvc,staff,base+"/shipments/"+id+"/acceptance",key,"\"i0\"",input).getStatus());
            assertEquals(1,scalar("SELECT count(*) FROM dispatch.pilot_intake_acceptance WHERE owner_type='BUSINESS' AND policy_version='BR13-BUSINESS-INTAKE-01'"));assertEquals(2,scalar("SELECT count(*) FROM dispatch.pilot_custody"));assertEquals(1,scalar("SELECT count(*) FROM orders.pilot_tracking_event WHERE owner_type='BUSINESS' AND owner_id IS NULL AND organization_id='"+org+"'"));assertEquals(2,scalar("SELECT count(*) FROM orders.pilot_parcel_event"));assertEquals(1,scalar("SELECT count(*) FROM finance.pilot_shipping_obligation WHERE state='UNPAID'"));assertEquals(snapshot,businessSnapshot("orders.pilot_shipment"));assertEquals("AT_STATION",intakeJson(httpRead(mvc,businessShippingBase(org)+"/shipments/"+id+"/handling",owner.access())).get("state").asString());
            String d2=businessTestDraft(mvc,owner,org);var q2=businessTestQuote(mvc,owner,org,d2,UUID.randomUUID());var b2=businessTestBook(mvc,owner,org,d2,intakeJson(q2).get("quote").get("id").asString(),UUID.randomUUID());assertEquals(201,b2.getStatus());String second=intakeJson(b2).get("shipment").get("id").asString();List<String> secondScans=new ArrayList<>();
            for(var p:intakeJson(b2).get("shipment").get("parcels")){var r=biaWrite(mvc,staff,base+"/scans",UUID.randomUUID(),null,Map.of("barcode","AP-P-"+p.get("id").asString().replace("-","")));assertEquals(200,r.getStatus());secondScans.add(responseId(r,"id"));}
            for(String malformed:List.of("CUSTODY","TRACKING","AUDIT","RECEIPT","TIME","SOURCE","OWNER","ORG","PARCEL","ACTOR","SITE"))biaIncompleteDmlDenied(second,malformed);
            var secondInput=Map.of("scanIds",secondScans,"inspection","MATCHED");String secondPath=base+"/shipments/"+second+"/acceptance";
            for(String table:List.of("dispatch.pilot_custody","dispatch.pilot_intake_audit","dispatch.pilot_intake_receipt","orders.pilot_tracking_event","orders.pilot_parcel_event")){
                db.ownerExecute("REVOKE INSERT ON "+table+" FROM \""+db.runtime()+"\"");try{assertEquals(503,biaWrite(mvc,staff,secondPath,UUID.randomUUID(),"\"i0\"",secondInput).getStatus());assertEquals(1,scalar("SELECT count(*) FROM dispatch.pilot_intake_acceptance"));assertEquals(2,scalar("SELECT count(*) FROM dispatch.pilot_custody"));assertEquals(1,scalar("SELECT count(*) FROM orders.pilot_tracking_event"));}finally{db.ownerExecute("GRANT INSERT ON "+table+" TO \""+db.runtime()+"\"");}
            }
            UUID concurrent=UUID.randomUUID();assertEquals(List.of(200,200),adminRace(()->biaWrite(mvc,staff,secondPath,concurrent,"\"i0\"",secondInput).getStatus(),()->biaWrite(mvc,staff,secondPath,concurrent,"\"i0\"",secondInput).getStatus()));assertEquals(2,scalar("SELECT count(*) FROM dispatch.pilot_intake_acceptance"));assertEquals(4,scalar("SELECT count(*) FROM dispatch.pilot_custody"));assertEquals(2,scalar("SELECT count(*) FROM finance.pilot_shipping_obligation WHERE state='UNPAID'"));
            String d3=businessTestDraft(mvc,owner,org);var q3=businessTestQuote(mvc,owner,org,d3,UUID.randomUUID());var b3=businessTestBook(mvc,owner,org,d3,intakeJson(q3).get("quote").get("id").asString(),UUID.randomUUID());assertEquals(201,b3.getStatus());String third=intakeJson(b3).get("shipment").get("id").asString();List<String> thirdScans=new ArrayList<>();
            for(var p:intakeJson(b3).get("shipment").get("parcels")){var r=biaWrite(mvc,staff,base+"/scans",UUID.randomUUID(),null,Map.of("barcode","AP-P-"+p.get("id").asString().replace("-","")));assertEquals(200,r.getStatus());thirdScans.add(responseId(r,"id"));}
            var race=adminRace(()->biaWrite(mvc,staff,base+"/shipments/"+third+"/acceptance",UUID.randomUUID(),"\"i0\"",Map.of("scanIds",thirdScans,"inspection","MATCHED")).getStatus(),()->{businessCommand(UUID.randomUUID(),"SUSPEND",org,A,"OWNER","'dashboard.read'");return 204;});assertTrue(Set.of(200,404).contains(race.getFirst()));assertEquals(204,race.get(1));long acceptedCount=Integer.valueOf(200).equals(race.getFirst())?3:2;assertEquals(acceptedCount,scalar("SELECT count(*) FROM dispatch.pilot_intake_acceptance"));assertEquals(acceptedCount*2,scalar("SELECT count(*) FROM dispatch.pilot_custody"));
            assertEquals(404,biaRead(mvc,staff,base+"/receipts/"+key).getStatus());assertEquals(404,biaWrite(mvc,staff,base+"/shipments/"+id+"/acceptance",key,"\"i0\"",input).getStatus());
            nwsAssignment(op,actor,site,"STATION_INTAKE",true);assertEquals(403,biaWrite(mvc,staff,base+"/shipments/"+id+"/acceptance",key,"\"i0\"",input).getStatus());
        }
    }
    @Test void biaBusinessHandlingRetainsOrganizationOwnershipAndUnpaid()throws Exception{
        customerFixture(true);biaHttpEnabled=true;
        try(var inbox=new CustomerSmtpTestInbox(0,false,false,250,null);var c=customerHttpContext(PILOT_NOW,smtp(inbox,PILOT_NOW),true)){
            var mvc=httpMvc(c);var owner=profileBrowser(mvc);var stranger=addressOtherBrowser(mvc);UUID org=id(885);
            businessShippingOwner(org);businessTestContract(org);String draft=businessTestDraft(mvc,owner,org);
            var priced=businessTestQuote(mvc,owner,org,draft,UUID.randomUUID());assertEquals(201,priced.getStatus(),this::safeOnboardingFailure);
            var booked=businessTestBook(mvc,owner,org,draft,intakeJson(priced).get("quote").get("id").asString(),UUID.randomUUID());
            assertEquals(201,booked.getStatus(),this::safeOnboardingFailure);String shipment=intakeJson(booked).get("shipment").get("id").asString();
            String path=businessShippingBase(org)+"/shipments/"+shipment+"/handling";
            var read=httpRead(mvc,path,owner.access());assertEquals(200,read.getStatus(),this::safeOnboardingFailure);
            assertEquals("WAITING_FOR_HANDOVER",intakeJson(read).get("state").asString());assertEquals(2,intakeJson(read).get("parcels").size());
            var management=mvc.perform(MockMvcRequestBuilders.get(businessShippingBase(org)+"/shipments?view=management")
                .secure(true).header("Authorization","Bearer "+owner.access()).header("Origin","https://portal.example.invalid")
                .accept("application/vnd.argopost.business-management.v2+json")).andReturn().getResponse();
            assertEquals(200,management.getStatus(),this::safeOnboardingFailure);
            assertEquals("ORGANIZATION_MANAGEMENT_2",intakeJson(management).get("schemaVersion").asString());
            assertEquals("WAITING_FOR_HANDOVER",intakeJson(management).get("items").get(0).get("handling").get("state").asString());
            assertEquals(404,httpRead(mvc,path,stranger.access()).getStatus());
            assertEquals(404,httpRead(mvc,"/api/v1/customer/shipments/"+shipment+"/handling",owner.access()).getStatus());
            assertEquals(1,scalar("SELECT count(*) FROM orders.pilot_shipment WHERE owner_type='BUSINESS' AND organization_id='"+org+"' AND owner_id IS NULL AND carrier_state='NOT_ACCEPTED'"));
            assertEquals(1,scalar("SELECT count(*) FROM finance.pilot_shipping_obligation WHERE owner_type='BUSINESS' AND organization_id='"+org+"' AND state='UNPAID'"));
            assertEquals(0,scalar("SELECT count(*) FROM dispatch.pilot_intake_acceptance"));
        }
    }
    @Test void businessBookingPersistsSharedParcelsUnpaidSnapshotAndPrivateLabels()throws Exception{
        customerFixture(true);try(var inbox=new CustomerSmtpTestInbox(0,false,false,250,null);var c=customerHttpContext(PILOT_NOW,smtp(inbox,PILOT_NOW),true)){
            var mvc=httpMvc(c);var owner=profileBrowser(mvc);var stranger=addressOtherBrowser(mvc);UUID org=id(871);businessShippingOwner(org);businessTestContract(org);String base=businessShippingBase(org),draft=businessTestDraft(mvc,owner,org);var qr=businessTestQuote(mvc,owner,org,draft,UUID.randomUUID());assertEquals(201,qr.getStatus(),this::safeOnboardingFailure);String quote=intakeJson(qr).get("quote").get("id").asString();UUID key=UUID.randomUUID();var booked=businessTestBook(mvc,owner,org,draft,quote,key);assertEquals(201,booked.getStatus(),this::safeOnboardingFailure);var s=intakeJson(booked).get("shipment");String id=s.get("id").asString();assertEquals("NOT_ACCEPTED",s.get("carrierState").asString());assertEquals(2,s.get("parcels").size());assertEquals("UNPAID",intakeJson(booked).get("obligation").get("state").asString());assertEquals(53000,intakeJson(booked).get("obligation").get("amountVnd").asLong());assertEquals(booked.getContentAsString(),businessTestBook(mvc,owner,org,draft,quote,key).getContentAsString());
            assertTrue(httpRead(mvc,base+"/shipments",owner.access()).getContentAsString().contains(id));assertTrue(httpRead(mvc,base+"/shipments?search=0901234567",owner.access()).getContentAsString().contains(id));assertTrue(httpRead(mvc,base+"/shipping-booking-receipts/"+key,owner.access()).getContentAsString().contains(id));assertEquals("BOOKED",intakeJson(httpRead(mvc,preparationPath(org)+"/"+draft,owner.access())).get("state").asString());
            assertEquals(404,httpRead(mvc,"/api/v1/customer/shipments/"+id,owner.access()).getStatus());assertFalse(httpRead(mvc,"/api/v1/customer/shipments",owner.access()).getContentAsString().contains(id));assertEquals(404,httpRead(mvc,base+"/shipments/"+id,stranger.access()).getStatus());
            var pdf=httpRead(mvc,base+"/shipments/"+id+"/labels",owner.access());assertEquals(200,pdf.getStatus());assertEquals("application/pdf",pdf.getContentType());assertTrue(pdf.getHeader("Cache-Control").contains("no-store"));try(var document=Loader.loadPDF(pdf.getContentAsByteArray())){assertEquals(2,document.getNumberOfPages());String text=new org.apache.pdfbox.text.PDFTextStripper().getText(document);assertTrue(text.contains("UNPAID"));assertTrue(text.contains("LOCAL/TEST SIMULATION"));}
            assertEquals(404,httpRead(mvc,base+"/shipments/"+id+"/labels",stranger.access()).getStatus());assertEquals(409,onboardingWrite(mvc,owner,"PUT",preparationPath(org)+"/"+draft,UUID.randomUUID(),"\"v2\"",pilotDraftInput("Changed")).getStatus());
            assertEquals(1,scalar("SELECT count(*) FROM orders.pilot_shipment WHERE owner_type='BUSINESS' AND owner_id IS NULL AND draft_id IS NULL"));assertEquals(1,scalar("SELECT count(*) FROM orders.organization_preparation_audit WHERE operation='BOOK'"));assertEquals(0,scalar("SELECT count(*) FROM orders.pilot_tracking_event"));assertEquals(0,scalar("SELECT count(*) FROM dispatch.pilot_intake_acceptance"));assertThrows(SQLException.class,()->execute("UPDATE finance.pilot_shipping_obligation SET state='PAID'"));
        }
    }
    @Test void businessBookingConcurrencyAndMandatoryEvidenceRollbackAreAtomic()throws Exception{
        customerFixture(true);try(var inbox=new CustomerSmtpTestInbox(0,false,false,250,null);var c=customerHttpContext(PILOT_NOW,smtp(inbox,PILOT_NOW),true)){
            var mvc=httpMvc(c);var owner=profileBrowser(mvc);UUID org=id(872);businessShippingOwner(org);businessTestContract(org);String draft=businessTestDraft(mvc,owner,org);var qr=businessTestQuote(mvc,owner,org,draft,UUID.randomUUID());assertEquals(201,qr.getStatus(),this::safeOnboardingFailure);String quote=intakeJson(qr).get("quote").get("id").asString();
            for(String table:List.of("orders.organization_booking_receipt","orders.organization_preparation_audit","orders.pilot_parcel","finance.pilot_shipping_obligation")){
                db.ownerExecute("REVOKE INSERT ON "+table+" FROM \""+db.runtime()+"\"");try{assertEquals(503,businessTestBook(mvc,owner,org,draft,quote,UUID.randomUUID()).getStatus());assertEquals(0,scalar("SELECT count(*) FROM orders.pilot_shipment"));assertEquals(0,scalar("SELECT count(*) FROM finance.pilot_shipping_obligation"));assertEquals(0,scalar("SELECT count(*) FROM pricing.pilot_quote WHERE consumed_by IS NOT NULL"));assertEquals(1,scalar("SELECT version FROM orders.organization_preparation"));}finally{db.ownerExecute("GRANT INSERT ON "+table+" TO \""+db.runtime()+"\"");}
            }
            UUID key=UUID.randomUUID();assertEquals(List.of(201,201),adminRace(()->businessTestBook(mvc,owner,org,draft,quote,key).getStatus(),()->businessTestBook(mvc,owner,org,draft,quote,key).getStatus()));assertEquals(1,scalar("SELECT count(*) FROM orders.pilot_shipment"));assertEquals(2,scalar("SELECT count(*) FROM orders.pilot_parcel"));assertEquals(1,scalar("SELECT count(*) FROM orders.organization_booking_receipt"));assertEquals(409,businessTestBook(mvc,owner,org,draft,quote,UUID.randomUUID()).getStatus());
        }
    }
    @Test void businessQuoteRevocationExpiryStaleAndProductionFailClosed()throws Exception{
        customerFixture(true);UUID org=id(873);String draft,quote;UUID key=UUID.randomUUID();
        try(var inbox=new CustomerSmtpTestInbox(0,false,false,250,null);var c=customerHttpContext(PILOT_NOW,smtp(inbox,PILOT_NOW),true)){
            var mvc=httpMvc(c);var owner=profileBrowser(mvc);businessShippingOwner(org);businessTestContract(org);draft=businessTestDraft(mvc,owner,org);var qr=businessTestQuote(mvc,owner,org,draft,key);assertEquals(201,qr.getStatus(),this::safeOnboardingFailure);quote=intakeJson(qr).get("quote").get("id").asString();
            assertEquals(400,onboardingWrite(mvc,owner,"POST",preparationPath(org)+"/"+draft+"/bookings",UUID.randomUUID(),"\"v1\"",Map.of("quoteId",quote,"amountVnd",1)).getStatus());
            db.ownerExecute("INSERT INTO pricing.organization_pilot_contract SELECT id,2,organization_id,tariff_version,'SUSPENDED',effective_from,effective_to,approval_ref,'"+UUID.randomUUID()+"',current_user,recorded_at FROM pricing.organization_pilot_contract WHERE organization_id='"+org+"'");assertFalse(intakeJson(httpRead(mvc,businessShippingBase(org)+"/shipping-quotes/"+quote,owner.access())).get("usable").asBoolean());assertEquals(409,businessTestBook(mvc,owner,org,draft,quote,UUID.randomUUID()).getStatus());
            businessCommand(UUID.randomUUID(),"REVOKE",org,A,"OWNER","'shipment.quote','shipment.book','shipment.label.read'");assertEquals(404,httpRead(mvc,businessShippingBase(org)+"/shipping-quote-receipts/"+key,owner.access()).getStatus());assertEquals(404,businessTestBook(mvc,owner,org,draft,quote,UUID.randomUUID()).getStatus());
        }
        for(String mode:List.of("DISABLED","PRODUCTION","TEST_WITH_PRODUCTION_PROFILE"))try(var inbox=new CustomerSmtpTestInbox(0,false,false,250,null);var c=customerHttpContext(PILOT_NOW,smtp(inbox,PILOT_NOW),true,mode)){var mvc=httpMvc(c);var owner=profileBrowser(mvc);assertEquals(503,httpRead(mvc,businessShippingBase(org)+"/shipping-catalog",owner.access()).getStatus());assertEquals(503,businessTestBook(mvc,owner,org,draft,quote,UUID.randomUUID()).getStatus());}
        assertEquals(0,scalar("SELECT count(*) FROM orders.pilot_shipment"));assertThrows(SQLException.class,()->execute("SELECT pricing.configure_local_business_contract('"+UUID.randomUUID()+"','"+org+"','ACTIVATE',2,'SYNTHETIC')"));
    }
    @Test void businessQuoteExpirationAndChangedPreparationCannotBeBooked()throws Exception{
        customerFixture(true);UUID org=id(874);String draft,quote;ProfileBrowser browser;
        try(var inbox=new CustomerSmtpTestInbox(0,false,false,250,null);var c=customerHttpContext(PILOT_NOW,smtp(inbox,PILOT_NOW),true)){
            var mvc=httpMvc(c);browser=profileBrowser(mvc);businessShippingOwner(org);businessTestContract(org);draft=businessTestDraft(mvc,browser,org);var q=businessTestQuote(mvc,browser,org,draft,UUID.randomUUID());assertEquals(201,q.getStatus(),this::safeOnboardingFailure);quote=intakeJson(q).get("quote").get("id").asString();
        }
        Instant expired=PILOT_NOW.plusSeconds(900);try(var inbox=new CustomerSmtpTestInbox(0,false,false,250,null);var c=customerHttpContext(expired,smtp(inbox,expired),true)){
            var mvc=httpMvc(c);var refreshed=customerPost(mvc,"refresh",browser.flow(),null,null);assertEquals(200,refreshed.getStatus());String token=intakeJson(refreshed).get("accessToken").asString();browser=new ProfileBrowser(token,new BrowserFlow(responseCookie(refreshed,"__Host-elogistics.customer.csrf"),responseCookie(refreshed,"__Host-elogistics.customer.refresh"),browser.flow().challenge));
            assertFalse(intakeJson(httpRead(mvc,businessShippingBase(org)+"/shipping-quotes/"+quote,browser.access())).get("usable").asBoolean());assertEquals(409,businessTestBook(mvc,browser,org,draft,quote,UUID.randomUUID()).getStatus());
            var q=businessTestQuote(mvc,browser,org,draft,UUID.randomUUID());assertEquals(201,q.getStatus(),this::safeOnboardingFailure);String fresh=intakeJson(q).get("quote").get("id").asString();assertEquals(200,onboardingWrite(mvc,browser,"PUT",preparationPath(org)+"/"+draft,UUID.randomUUID(),"\"v1\"",pilotDraftInput("Changed source facts")).getStatus());assertFalse(intakeJson(httpRead(mvc,businessShippingBase(org)+"/shipping-quotes/"+fresh,browser.access())).get("usable").asBoolean());assertEquals(409,businessTestBook(mvc,browser,org,draft,fresh,UUID.randomUUID()).getStatus());assertEquals(0,scalar("SELECT count(*) FROM orders.pilot_shipment"));
        }
    }
    @Test void businessConfirmedShippingHonorsFacilityScopeAndActorBoundReceipts()throws Exception{
        customerFixture(true);try(var inbox=new CustomerSmtpTestInbox(0,false,false,250,null);var c=customerHttpContext(PILOT_NOW,smtp(inbox,PILOT_NOW),true)){
            var mvc=httpMvc(c);var owner=profileBrowser(mvc);var member=addressOtherBrowser(mvc);UUID org=id(875),other=id(876),site=id(877),foreign=id(878);businessShippingOwner(org);preparationOwner(other);businessTestContract(org);
            String contact=new tools.jackson.databind.ObjectMapper().writeValueAsString(shippingDraftInput("Facility").get("sender")).replace("0901234567","+84901234567");seedFacility(site,org,contact);seedFacility(foreign,other,contact);
            businessCommand(UUID.randomUUID(),"GRANT",org,B,"OPERATOR","'dashboard.read'");seedScopedLegacyGrants(org,B,site,"'shipment.draft.read','shipment.quote','shipment.book','shipment.label.read'");
            var input=new HashMap<String,Object>(pilotDraftInput("Scoped"));input.put("facilityId",site);String draft=responseId(onboardingWrite(mvc,owner,"POST",preparationPath(org),UUID.randomUUID(),null,input),"id");String unscoped=businessTestDraft(mvc,owner,org);assertEquals(404,businessTestQuote(mvc,member,org,unscoped,UUID.randomUUID()).getStatus());
            UUID qkey=UUID.randomUUID();var q=businessTestQuote(mvc,member,org,draft,qkey);assertEquals(201,q.getStatus(),this::safeOnboardingFailure);String quote=intakeJson(q).get("quote").get("id").asString();UUID bkey=UUID.randomUUID();var booked=businessTestBook(mvc,member,org,draft,quote,bkey);assertEquals(201,booked.getStatus(),this::safeOnboardingFailure);String shipment=intakeJson(booked).get("shipment").get("id").asString();String base=businessShippingBase(org);assertTrue(httpRead(mvc,base+"/shipments",member.access()).getContentAsString().contains(shipment));assertEquals(200,httpRead(mvc,base+"/shipments/"+shipment+"/labels",member.access()).getStatus());
            assertEquals(404,httpRead(mvc,base+"/shipping-booking-receipts/"+bkey,owner.access()).getStatus());assertEquals(404,httpRead(mvc,base+"/shipping-quote-receipts/"+qkey,owner.access()).getStatus());assertEquals(404,httpRead(mvc,businessShippingBase(other)+"/shipments/"+shipment,owner.access()).getStatus());
            intakeAssignment(mvc,owner,"SIM-DROP-A1");String barcode="AP-P-"+intakeJson(booked).get("shipment").get("parcels").get(0).get("id").asString().replace("-","");assertEquals(404,onboardingWrite(mvc,owner,"POST",STATION_PATH+"SIM-DROP-A1/scans",UUID.randomUUID(),null,Map.of("barcode",barcode)).getStatus());
            businessCommand(UUID.randomUUID(),"REVOKE",org,B,"OPERATOR","");assertEquals(404,httpRead(mvc,base+"/shipments/"+shipment+"/labels",member.access()).getStatus());assertEquals(404,httpRead(mvc,base+"/shipping-booking-receipts/"+bkey,member.access()).getStatus());assertEquals(0,scalar("SELECT count(*) FROM dispatch.pilot_intake_acceptance"));
        }
    }
    @Test void businessBookingSerializesWithContractSuspension()throws Exception{
        customerFixture(true);try(var inbox=new CustomerSmtpTestInbox(0,false,false,250,null);var c=customerHttpContext(PILOT_NOW,smtp(inbox,PILOT_NOW),true)){
            var mvc=httpMvc(c);var owner=profileBrowser(mvc);UUID org=id(879);businessShippingOwner(org);businessTestContract(org);String draft=businessTestDraft(mvc,owner,org);var qr=businessTestQuote(mvc,owner,org,draft,UUID.randomUUID());assertEquals(201,qr.getStatus(),this::safeOnboardingFailure);String quote=intakeJson(qr).get("quote").get("id").asString();var pool=Executors.newSingleThreadExecutor();
            try(var connection=db.migrationConnection();var statement=connection.createStatement()){
                statement.execute("SET ROLE "+db.owner());connection.setAutoCommit(false);statement.execute("SET LOCAL argopost.operator_environment='TEST'");statement.execute("SELECT pricing.configure_local_business_contract('"+UUID.randomUUID()+"','"+org+"','SUSPEND',1,'SYNTHETIC_BSC06_SUSPENSION')");
                var entered=new CountDownLatch(1);var booking=pool.submit(()->{entered.countDown();return businessTestBook(mvc,owner,org,draft,quote,UUID.randomUUID()).getStatus();});assertTrue(entered.await(5,TimeUnit.SECONDS));assertThrows(TimeoutException.class,()->booking.get(200,TimeUnit.MILLISECONDS));connection.commit();assertEquals(409,booking.get(10,TimeUnit.SECONDS));
            }finally{pool.shutdownNow();}
            assertEquals(0,scalar("SELECT count(*) FROM orders.pilot_shipment"));assertEquals(0,scalar("SELECT count(*) FROM finance.pilot_shipping_obligation"));assertEquals(0,scalar("SELECT count(*) FROM pricing.pilot_quote WHERE consumed_by IS NOT NULL"));assertEquals(1,scalar("SELECT count(*) FROM pricing.organization_pilot_contract WHERE state='SUSPENDED' AND version=2"));
        }
    }
    private static final String BUSINESS_CONTEXTS="/api/v1/customer/business-contexts";
    private String businessPath(UUID org){return "/api/v1/merchant-organizations/"+org+"/dashboard";}
    private String businessSql(UUID request,String action,UUID org,UUID account,String role,String permissions){
        return "SELECT merchant.configure_local_workspace('"+request+"','"+action+"','"+org+"','Synthetic organization','"+account+"','"+role+"',ARRAY["+permissions+"]::text[],'Synthetic operator command')";
    }
    private void businessCommand(UUID request,String action,UUID org,UUID account,String role,String permissions)throws Exception{
        db.ownerExecute("BEGIN; SET LOCAL argopost.operator_environment='TEST'; "+businessSql(request,action,org,account,role,permissions)+"; COMMIT;");
    }
    private long businessScalar(String sql)throws Exception{try(var c=db.migrationConnection();var s=c.createStatement()){s.execute("SET ROLE "+db.owner());try(var r=s.executeQuery(sql)){assertTrue(r.next());return r.getLong(1);}}}
    @Test void businessWorkspaceContextsIsolateOrganizationAndNeverReusePersonalFacts()throws Exception{
        customerFixture(true);try(var inbox=new CustomerSmtpTestInbox(0,false,false,250,null);var c=customerHttpContext(PILOT_NOW,smtp(inbox,PILOT_NOW),true)){
            var mvc=httpMvc(c);var owner=profileBrowser(mvc);var other=addressOtherBrowser(mvc);
            assertEquals(0,intakeJson(httpRead(mvc,BUSINESS_CONTEXTS,owner.access())).get("items").size());
            UUID first=id(810),second=id(811);businessCommand(UUID.randomUUID(),"PROVISION",first,A,"OWNER","'dashboard.read','dashboard.finance.read','dashboard.staff.read'");businessCommand(UUID.randomUUID(),"PROVISION",second,B,"OWNER","'dashboard.read'");
            intakeBooked(mvc,owner);var contexts=httpRead(mvc,BUSINESS_CONTEXTS,owner.access());assertEquals(200,contexts.getStatus());assertEquals("no-store",contexts.getHeader("Cache-Control"));assertEquals(1,intakeJson(contexts).get("items").size());assertFalse(contexts.getContentAsString().contains(second.toString()));
            var result=httpRead(mvc,businessPath(first),owner.access());assertEquals(200,result.getStatus(),this::safeOnboardingFailure);assertEquals("no-store",result.getHeader("Cache-Control"));var report=intakeJson(result);assertEquals("BUSINESS_DASHBOARD_V1",report.get("schemaVersion").asString());assertEquals(21,report.get("metrics").size());
            for(var metric:report.get("metrics")){assertTrue(metric.get("value").isNull());assertEquals(0,metric.get("points").size());assertEquals("SOURCE_NOT_CONNECTED",metric.get("state").asString());}
            assertEquals(0,report.get("recentShipments").get("items").size());assertFalse(result.getContentAsString().contains("AP-SIM-"));assertEquals(404,httpRead(mvc,businessPath(second),owner.access()).getStatus());assertEquals(404,httpRead(mvc,businessPath(UUID.randomUUID()),owner.access()).getStatus());
            assertEquals(400,httpRead(mvc,BUSINESS_CONTEXTS+"?accountId="+B,owner.access()).getStatus());
            for(String query:List.of("?facilityId="+UUID.randomUUID(),"?from=2026-01-01&from=2026-01-02","?from=2024-01-01&to=2026-01-01","?from=2026-02-30","?interval=YEAR"))assertEquals(400,httpRead(mvc,businessPath(first)+query,owner.access()).getStatus());
            var readOnly=httpRead(mvc,businessPath(second),other.access());assertEquals(200,readOnly.getStatus());assertEquals(14,intakeJson(readOnly).get("metrics").size());assertFalse(readOnly.getContentAsString().contains("COD_EXPECTED"));assertFalse(readOnly.getContentAsString().contains("STAFF_BREAKDOWN"));
            assertEquals(1,scalar("SELECT count(*) FROM orders.pilot_shipment"));assertEquals(1,scalar("SELECT count(*) FROM finance.pilot_shipping_obligation WHERE state='UNPAID'"));
        }
    }
    @Test void businessWorkspaceOperatorReceiptsPrivilegesAndAuditRollback()throws Exception{
        customerFixture(true);UUID org=id(820),key=UUID.randomUUID();
        var pool=Executors.newFixedThreadPool(2);try{var a=pool.submit(()->{businessCommand(key,"PROVISION",org,A,"OWNER","'dashboard.read'");return true;});var b=pool.submit(()->{businessCommand(key,"PROVISION",org,A,"OWNER","'dashboard.read'");return true;});assertTrue(a.get(15,TimeUnit.SECONDS));assertTrue(b.get(15,TimeUnit.SECONDS));}finally{pool.shutdownNow();}
        assertEquals(1,businessScalar("SELECT count(*) FROM merchant.organization"));assertEquals(1,businessScalar("SELECT count(*) FROM merchant.workspace_audit"));assertEquals(1,businessScalar("SELECT count(*) FROM merchant.workspace_command_receipt"));
        assertThrows(SQLException.class,()->businessCommand(key,"PROVISION",org,A,"OWNER","'dashboard.read','dashboard.finance.read'"));
        assertThrows(SQLException.class,()->execute(businessSql(UUID.randomUUID(),"PROVISION",id(821),A,"OWNER","'dashboard.read'")));
        assertThrows(SQLException.class,()->execute("SELECT identity.lock_local_business_subject('"+A+"',TRUE)"));
        assertThrows(SQLException.class,()->execute("UPDATE merchant.membership SET state='REVOKED'"));assertThrows(SQLException.class,()->execute("DELETE FROM merchant.member_grant"));
        assertThrows(SQLException.class,()->businessCommand(UUID.randomUUID(),"REVOKE",org,A,"OWNER",""));assertEquals(1,businessScalar("SELECT count(*) FROM merchant.membership WHERE state='ACTIVE'"));
        assertThrows(SQLException.class,()->db.ownerExecute("INSERT INTO merchant.member_grant SELECT gen_random_uuid(),id,organization_id,'dashboard.finance.read','FACILITY' FROM merchant.membership"));
        db.ownerExecute("CREATE FUNCTION merchant.synthetic_workspace_audit_fault() RETURNS trigger LANGUAGE plpgsql AS $$ BEGIN RAISE EXCEPTION 'SYNTHETIC_AUDIT_FAILURE'; END $$; CREATE TRIGGER synthetic_workspace_audit_fault BEFORE INSERT ON merchant.workspace_audit FOR EACH ROW EXECUTE FUNCTION merchant.synthetic_workspace_audit_fault()");
        try{assertThrows(SQLException.class,()->businessCommand(UUID.randomUUID(),"PROVISION",id(822),B,"OWNER","'dashboard.read'"));assertEquals(1,businessScalar("SELECT count(*) FROM merchant.organization"));assertEquals(1,businessScalar("SELECT count(*) FROM merchant.membership"));}finally{db.ownerExecute("DROP TRIGGER synthetic_workspace_audit_fault ON merchant.workspace_audit; DROP FUNCTION merchant.synthetic_workspace_audit_fault()");}
        assertThrows(SQLException.class,()->db.ownerExecute("UPDATE merchant.workspace_audit SET reason='altered'"));
    }
    @Test void businessWorkspaceCurrentRevocationSuspensionAndOwnerEligibility()throws Exception{
        customerFixture(true);try(var inbox=new CustomerSmtpTestInbox(0,false,false,250,null);var c=customerHttpContext(PILOT_NOW,smtp(inbox,PILOT_NOW),true)){
            var mvc=httpMvc(c);var owner=profileBrowser(mvc);var member=addressOtherBrowser(mvc);UUID org=id(830);businessCommand(UUID.randomUUID(),"PROVISION",org,A,"OWNER","'dashboard.read'");businessCommand(UUID.randomUUID(),"GRANT",org,B,"MANAGER","'dashboard.read','dashboard.finance.read'");
            assertTrue(httpRead(mvc,businessPath(org),member.access()).getContentAsString().contains("CONFIRMED_FEES"));
            businessCommand(UUID.randomUUID(),"REVOKE",org,B,"MANAGER","'dashboard.finance.read'");var limited=httpRead(mvc,businessPath(org),member.access());assertEquals(200,limited.getStatus());assertFalse(limited.getContentAsString().contains("CONFIRMED_FEES"));assertFalse(limited.getContentAsString().contains("FEE_TREND"));
            execute("UPDATE identity.user_account SET status='DISABLED',disabled_at=TIMESTAMPTZ '2026-11-01 00:01:00Z',updated_at=TIMESTAMPTZ '2026-11-01 00:01:00Z' WHERE id='"+A+"'");assertEquals(404,httpRead(mvc,businessPath(org),member.access()).getStatus());assertEquals(401,httpRead(mvc,businessPath(org),owner.access()).getStatus());
            execute("UPDATE identity.user_account SET status='ACTIVE',disabled_at=NULL WHERE id='"+A+"'");
            businessCommand(UUID.randomUUID(),"REVOKE",org,B,"MANAGER","");assertEquals(404,httpRead(mvc,businessPath(org),member.access()).getStatus());assertThrows(SQLException.class,()->businessCommand(UUID.randomUUID(),"GRANT",org,B,"MANAGER","'dashboard.read'"));
            businessCommand(UUID.randomUUID(),"SUSPEND",org,A,"OWNER","");assertEquals(404,httpRead(mvc,businessPath(org),owner.access()).getStatus());assertEquals(0,intakeJson(httpRead(mvc,BUSINESS_CONTEXTS,owner.access())).get("items").size());
        }
    }
    @Test void businessWorkspaceReadSerializesWithCommittedRevocation()throws Exception{
        customerFixture(true);try(var inbox=new CustomerSmtpTestInbox(0,false,false,250,null);var c=customerHttpContext(PILOT_NOW,smtp(inbox,PILOT_NOW),true)){
            var mvc=httpMvc(c);profileBrowser(mvc);var member=addressOtherBrowser(mvc);UUID org=id(840);businessCommand(UUID.randomUUID(),"PROVISION",org,A,"OWNER","'dashboard.read'");businessCommand(UUID.randomUUID(),"GRANT",org,B,"OPERATOR","'dashboard.read'");
            var pool=Executors.newSingleThreadExecutor();try(var connection=db.migrationConnection();var statement=connection.createStatement()){
                statement.execute("SET ROLE "+db.owner());connection.setAutoCommit(false);statement.execute("SET LOCAL argopost.operator_environment='TEST'");statement.execute(businessSql(UUID.randomUUID(),"REVOKE",org,B,"OPERATOR",""));
                var entered=new CountDownLatch(1);var read=pool.submit(()->{entered.countDown();return httpRead(mvc,businessPath(org),member.access()).getStatus();});assertTrue(entered.await(5,TimeUnit.SECONDS));assertThrows(TimeoutException.class,()->read.get(200,TimeUnit.MILLISECONDS));connection.commit();assertEquals(404,read.get(10,TimeUnit.SECONDS));
            }finally{pool.shutdownNow();}
        }
    }
    @Test void businessWorkspaceDisabledAndProductionProfilesFailClosed()throws Exception{
        customerFixture(true);for(String mode:List.of("DISABLED","PRODUCTION","TEST_WITH_PRODUCTION_PROFILE"))try(var inbox=new CustomerSmtpTestInbox(0,false,false,250,null);var c=customerHttpContext(PILOT_NOW,smtp(inbox,PILOT_NOW),true,mode)){
            var mvc=httpMvc(c);var owner=profileBrowser(mvc);assertEquals(503,httpRead(mvc,BUSINESS_CONTEXTS,owner.access()).getStatus());assertEquals(503,httpRead(mvc,businessPath(id(850)),owner.access()).getStatus());assertEquals(401,mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(BUSINESS_CONTEXTS).secure(true)).andReturn().getResponse().getStatus());
        }
    }

    /** Synthetic fixtures use the same constrained snapshot/receipt/audit path after V31. */
    // Runtime fixture retains facility grants, version, receipt and audit guards; no schema-owner predicate ACL.
    private void seedFacility(UUID site,UUID org,String contact)throws Exception{
        facilityGrant(org,A,null,"'facility.read','facility.manage'");
        String code="SITE-"+site.toString().substring(24),op=UUID.randomUUID().toString();
        execute("BEGIN; INSERT INTO merchant.facility(id,organization_id,name,contact,state,version,created_at,updated_at,code,kind) VALUES ('"+site+"','"+org+"','Synthetic site','"+contact.replace("'","''")+"','ACTIVE',1,statement_timestamp(),statement_timestamp(),'"+code+"','WAREHOUSE'); INSERT INTO merchant.facility_version(resource_kind,target_id,organization_id,version,snapshot) SELECT 'SITE',id,organization_id,version,to_jsonb(f) FROM merchant.facility f WHERE id='"+site+"'; INSERT INTO merchant.facility_command_receipt SELECT organization_id,'"+A+"','CREATE','"+op+"','SITE',id,id,0,1,'synthetic',decode(repeat('01',32),'hex'),updated_at FROM merchant.facility WHERE id='"+site+"'; INSERT INTO merchant.facility_audit SELECT gen_random_uuid(),organization_id,'"+A+"','CREATE','"+op+"','SITE',id,0,1,gen_random_uuid(),updated_at FROM merchant.facility WHERE id='"+site+"'; COMMIT;");
    }
    private String facilityPath(UUID org){return "/api/v1/merchant-organizations/"+org+"/facilities";}
    private void facilityGrant(UUID org,UUID account,UUID site,String permissions)throws Exception{
        db.ownerExecute("BEGIN; SET LOCAL argopost.operator_environment='TEST'; SELECT merchant.configure_local_facility_grants('"+UUID.randomUUID()+"','GRANT','"+org+"','"+account+"',"+(site==null?"NULL":"'"+site+"'")+",ARRAY["+permissions+"]::text[],'Synthetic facility permission'); COMMIT;");
    }
    /** Legacy scoped fixtures use the trusted schema-owner lane, with authority
     * version + independent operator audit/receipt in one transaction. Never a
     * runtime shortcut or a localhost provisioning mechanism. */
    private void seedScopedLegacyGrants(UUID org,UUID account,UUID site,String permissions)throws Exception{
        UUID request=UUID.randomUUID();
        db.ownerExecute("BEGIN; SET LOCAL argopost.operator_environment='TEST'; UPDATE merchant.membership SET version=version+1,authority_version=authority_version+1,updated_at=clock_timestamp() WHERE organization_id='"+org+"' AND account_id='"+account+"'; INSERT INTO merchant.member_grant(id,membership_id,organization_id,permission,scope,facility_id) SELECT gen_random_uuid(),m.id,m.organization_id,p,'FACILITY','"+site+"' FROM merchant.membership m CROSS JOIN unnest(ARRAY["+permissions+"]::text[]) p WHERE m.organization_id='"+org+"' AND account_id='"+account+"'; INSERT INTO merchant.workspace_audit SELECT gen_random_uuid(),organization_id,id,session_user,'GRANT','"+request+"','Synthetic scoped legacy fixture',updated_at FROM merchant.membership WHERE organization_id='"+org+"' AND account_id='"+account+"'; INSERT INTO merchant.workspace_command_receipt SELECT '"+request+"',encode(sha256(convert_to('synthetic-scope-"+request+"','UTF8')),'hex'),organization_id,id,'GRANT',updated_at FROM merchant.membership WHERE organization_id='"+org+"' AND account_id='"+account+"'; COMMIT;");
    }
    private Map<String,Object> facilityInput(String code){var in=new HashMap<String,Object>();in.put("name","Synthetic site");in.put("code",code);in.put("kind","WAREHOUSE");in.put("contact",shippingDraftInput("Site").get("sender"));in.put("senderContact",null);in.put("returnContact",null);in.put("operatingHours",null);return in;}
    @Test void merchantFacilityCrudDefaultArchiveAndReplayAreScopedAndAtomic()throws Exception{
        customerFixture(true);try(var inbox=new CustomerSmtpTestInbox(0,false,false,250,null);var c=customerHttpContext(PILOT_NOW,smtp(inbox,PILOT_NOW),true)){
            var mvc=httpMvc(c);var owner=profileBrowser(mvc);UUID org=id(9101);preparationOwner(org);facilityGrant(org,A,null,"'facility.read','facility.manage','defaults.manage'");String path=facilityPath(org),defaults=businessShippingBase(org)+"/facility-default";UUID key=UUID.randomUUID();var input=facilityInput("wh-01");input.put("senderContact",input.get("contact"));input.put("returnContact",input.get("contact"));input.put("operatingHours",Map.of("timeZone","Asia/Ho_Chi_Minh","days",java.util.stream.Stream.of("MONDAY","TUESDAY","WEDNESDAY","THURSDAY","FRIDAY","SATURDAY","SUNDAY").map(day->Map.of("day",day,"intervals",day.equals("MONDAY")?List.of(Map.of("opensAt","08:00","closesAt","17:00")):List.of())).toList()));
            var created=onboardingWrite(mvc,owner,"POST",path,key,null,input);assertEquals(201,created.getStatus(),this::safeOnboardingFailure);String target=responseId(created,"id");assertEquals("WH-01",intakeJson(created).get("code").asString());assertEquals("+84901234567",intakeJson(created).get("contact").get("phone").asString());assertEquals("no-store",created.getHeader("Cache-Control"));assertEquals("\"v1\"",created.getHeader("ETag"));assertEquals(created.getContentAsString(),onboardingWrite(mvc,owner,"POST",path,key,null,input).getContentAsString());
            assertEquals(409,onboardingWrite(mvc,owner,"POST",path,UUID.randomUUID(),null,input).getStatus());input.put("name","Changed synthetic site");assertEquals(409,onboardingWrite(mvc,owner,"POST",path,key,null,input).getStatus());assertEquals(200,onboardingWrite(mvc,owner,"PUT",path+"/"+target,UUID.randomUUID(),"\"v1\"",input).getStatus());input.put("code","IMMUTABLE");assertEquals(409,onboardingWrite(mvc,owner,"PUT",path+"/"+target,UUID.randomUUID(),"\"v2\"",input).getStatus());
            assertEquals(200,onboardingWrite(mvc,owner,"PUT",defaults,UUID.randomUUID(),"\"d0\"",Map.of("facilityId",target)).getStatus());assertEquals(409,onboardingWrite(mvc,owner,"POST",path+"/"+target+"/archive",UUID.randomUUID(),"\"v2\"","").getStatus());
            var clear=new HashMap<String,Object>();clear.put("facilityId",null);assertEquals(200,onboardingWrite(mvc,owner,"PUT",defaults,UUID.randomUUID(),"\"d1\"",clear).getStatus());assertEquals(200,onboardingWrite(mvc,owner,"POST",path+"/"+target+"/archive",UUID.randomUUID(),"\"v2\"","").getStatus());assertEquals(0,intakeJson(httpRead(mvc,path,owner.access())).get("items").size());assertEquals(1,intakeJson(httpRead(mvc,path+"?state=ARCHIVED",owner.access())).get("items").size());
            assertEquals(200,httpRead(mvc,businessShippingBase(org)+"/facility-receipts/CREATE/"+key,owner.access()).getStatus());assertEquals(5,scalar("SELECT count(*) FROM merchant.facility_audit"));assertEquals(0,scalar("SELECT count(*) FROM orders.pilot_shipment"));assertEquals(0,scalar("SELECT count(*) FROM finance.pilot_shipping_obligation"));assertEquals(0,scalar("SELECT count(*) FROM orders.pilot_tracking_event"));
            assertThrows(SQLException.class,()->execute("DELETE FROM merchant.facility"));assertThrows(SQLException.class,()->execute("UPDATE merchant.facility_version SET snapshot='{}'"));
        }
    }
    @Test void merchantFacilityValidationConcurrencyAndEvidenceFailureDoNotLeakPartialWrites()throws Exception{
        customerFixture(true);try(var inbox=new CustomerSmtpTestInbox(0,false,false,250,null);var c=customerHttpContext(PILOT_NOW,smtp(inbox,PILOT_NOW),true)){
            var mvc=httpMvc(c);var owner=profileBrowser(mvc);UUID org=id(9102);preparationOwner(org);facilityGrant(org,A,null,"'facility.read','facility.manage','defaults.manage'");String path=facilityPath(org);var input=facilityInput("RACE-1");UUID key=UUID.randomUUID();assertEquals(List.of(201,201),adminRace(()->onboardingWrite(mvc,owner,"POST",path,key,null,input).getStatus(),()->onboardingWrite(mvc,owner,"POST",path,key,null,input).getStatus()));assertEquals(1,scalar("SELECT count(*) FROM merchant.facility"));String target=intakeJson(httpRead(mvc,path,owner.access())).get("items").get(0).get("id").asString();
            var race=adminRace(()->onboardingWrite(mvc,owner,"PUT",path+"/"+target,UUID.randomUUID(),"\"v1\"",input).getStatus(),()->onboardingWrite(mvc,owner,"POST",path+"/"+target+"/archive",UUID.randomUUID(),"\"v1\"","").getStatus());assertEquals(1,race.stream().filter(v->Integer.valueOf(200).equals(v)).count());assertEquals(1,race.stream().filter(v->Integer.valueOf(409).equals(v)).count());
            for(String table:List.of("facility_version","facility_audit","facility_command_receipt")){db.ownerExecute("REVOKE INSERT ON merchant."+table+" FROM \""+db.runtime()+"\"");try{assertEquals(503,onboardingWrite(mvc,owner,"POST",path,UUID.randomUUID(),null,facilityInput("FAULT-"+table)).getStatus());assertEquals(1,scalar("SELECT count(*) FROM merchant.facility"));}finally{db.ownerExecute("GRANT INSERT ON merchant."+table+" TO \""+db.runtime()+"\"");}}
            assertEquals(400,httpRead(mvc,path+"?search=private",owner.access()).getStatus());assertEquals(400,onboardingWrite(mvc,owner,"POST",path,UUID.randomUUID(),null,"{\"name\":\"a\",\"name\":\"b\"}").getStatus());var bad=facilityInput("BAD");bad.put("operatingHours",Map.of("timeZone","UTC","days",List.of()));assertEquals(400,onboardingWrite(mvc,owner,"POST",path,UUID.randomUUID(),null,bad).getStatus());assertEquals(428,onboardingWrite(mvc,owner,"PUT",path+"/"+target,UUID.randomUUID(),null,input).getStatus());
        }
    }
    @Test void merchantFacilityResponsibilitiesDoNotGrantAccessAndRevocationDeniesReplay()throws Exception{
        customerFixture(true);try(var inbox=new CustomerSmtpTestInbox(0,false,false,250,null);var c=customerHttpContext(PILOT_NOW,smtp(inbox,PILOT_NOW),true)){
            var mvc=httpMvc(c);var owner=profileBrowser(mvc);var worker=addressOtherBrowser(mvc);UUID org=id(9103);preparationOwner(org);businessCommand(UUID.randomUUID(),"GRANT",org,B,"OPERATOR","'dashboard.read'");facilityGrant(org,A,null,"'facility.read','facility.manage'");String path=facilityPath(org),target=responseId(onboardingWrite(mvc,owner,"POST",path,UUID.randomUUID(),null,facilityInput("TEAM-1")),"id"),team=path+"/"+target+"/responsibilities";UUID site=UUID.fromString(target);
            assertEquals(404,httpRead(mvc,path+"/"+target,worker.access()).getStatus());var items=Map.of("items",List.of(Map.of("accountId",B,"responsibility","PREPARATION")));assertEquals(409,onboardingWrite(mvc,owner,"PUT",team,UUID.randomUUID(),"\"v0\"",items).getStatus());facilityGrant(org,B,site,"'facility.read'");long grants=scalar("SELECT count(*) FROM merchant.member_grant");UUID key=UUID.randomUUID();assertEquals(200,onboardingWrite(mvc,owner,"PUT",team,key,"\"v0\"",items).getStatus());assertEquals(grants,scalar("SELECT count(*) FROM merchant.member_grant"));assertEquals(200,httpRead(mvc,path+"/"+target,worker.access()).getStatus());assertEquals(404,onboardingWrite(mvc,worker,"PUT",path+"/"+target,UUID.randomUUID(),"\"v1\"",facilityInput("TEAM-1")).getStatus());assertEquals(1,intakeJson(httpRead(mvc,path,worker.access())).get("items").size());assertEquals(404,onboardingWrite(mvc,worker,"POST",path,UUID.randomUUID(),null,facilityInput("DENIED")).getStatus());
            businessCommand(UUID.randomUUID(),"REVOKE",org,A,"OWNER","'facility.manage'");assertEquals(404,httpRead(mvc,businessShippingBase(org)+"/facility-receipts/RESPONSIBILITY/"+key,owner.access()).getStatus());assertEquals(404,onboardingWrite(mvc,owner,"PUT",team,key,"\"v0\"",items).getStatus());assertEquals(0,scalar("SELECT count(*) FROM orders.pilot_tracking_event"));
        }
        for(String mode:List.of("DISABLED","PRODUCTION","TEST_WITH_PRODUCTION_PROFILE"))try(var inbox=new CustomerSmtpTestInbox(0,false,false,250,null);var c=customerHttpContext(PILOT_NOW,smtp(inbox,PILOT_NOW),true,mode)){var mvc=httpMvc(c);var owner=profileBrowser(mvc);assertEquals(503,httpRead(mvc,facilityPath(id(9103)),owner.access()).getStatus());assertEquals(503,onboardingWrite(mvc,owner,"POST",facilityPath(id(9103)),UUID.randomUUID(),null,facilityInput("NO-PRODUCTION")).getStatus());}
    }
    private record FacilityWorkSource(UUID site,String shipment,List<String> parcels){}
    private FacilityWorkSource facilityWorkSource(org.springframework.test.web.servlet.MockMvc mvc,ProfileBrowser owner,UUID org,UUID existingSite)throws Exception{
        UUID site=existingSite==null?UUID.fromString(responseId(onboardingWrite(mvc,owner,"POST",facilityPath(org),UUID.randomUUID(),null,facilityInput("WH-"+UUID.randomUUID().toString().substring(0,8))),"id")):existingSite;
        var input=new HashMap<String,Object>(shippingDraftInput("Synthetic booked parcel source"));input.put("facilityId",site);var parcel=((List<?>)input.get("parcels")).getFirst();input.put("parcels",List.of(parcel,parcel));
        String draft=responseId(onboardingWrite(mvc,owner,"POST",preparationPath(org),UUID.randomUUID(),null,input),"id");var quoted=businessTestQuote(mvc,owner,org,draft,UUID.randomUUID());assertEquals(201,quoted.getStatus(),this::safeOnboardingFailure);var booked=businessTestBook(mvc,owner,org,draft,intakeJson(quoted).get("quote").get("id").asString(),UUID.randomUUID());assertEquals(201,booked.getStatus(),this::safeOnboardingFailure);var node=intakeJson(booked).get("shipment");List<String> parcels=new ArrayList<>();for(var p:node.get("parcels"))parcels.add(p.get("id").asString());return new FacilityWorkSource(site,node.get("id").asString(),parcels);
    }
    private void facilityWorkOwner(UUID org)throws Exception{businessShippingOwner(org);businessTestContract(org);facilityGrant(org,A,null,"'facility.read','facility.manage','shipment.draft.read','shipment.prepare','shipment.handover.read','shipment.handover.manage'");}
    @Test void merchantParcelPreparationAndHandoverRequireEveryRealParcelWithoutCarrierOrPaymentEffects()throws Exception{
        customerFixture(true);try(var inbox=new CustomerSmtpTestInbox(0,false,false,250,null);var c=customerHttpContext(PILOT_NOW,smtp(inbox,PILOT_NOW),true)){
            var mvc=httpMvc(c);var owner=profileBrowser(mvc);UUID org=id(9110);facilityWorkOwner(org);var source=facilityWorkSource(mvc,owner,org,null);String base=businessShippingBase(org),preps=base+"/shipment-preparation-work",batches=base+"/handover-preparations";
            var list=intakeJson(httpRead(mvc,facilityPath(org)+"/"+source.site()+"/preparation-work",owner.access()));assertTrue(list.get("items").get(0).get("preparation").isNull());UUID start=UUID.randomUUID();var created=onboardingWrite(mvc,owner,"POST",preps,start,null,Map.of("shipmentId",source.shipment()));assertEquals(200,created.getStatus(),this::safeOnboardingFailure);String work=responseId(created,"id"),path=preps+"/"+work;assertEquals(created.getContentAsString(),onboardingWrite(mvc,owner,"POST",preps,start,null,Map.of("shipmentId",source.shipment())).getContentAsString());
            assertEquals(409,onboardingWrite(mvc,owner,"POST",path+"/transitions",UUID.randomUUID(),"\"v1\"",Map.of("action","PACK")).getStatus());assertEquals(409,onboardingWrite(mvc,owner,"POST",path+"/checks",UUID.randomUUID(),"\"v1\"",Map.of("parcelId",id(99999),"condition","OK")).getStatus());
            assertEquals(200,onboardingWrite(mvc,owner,"POST",path+"/checks",UUID.randomUUID(),"\"v1\"",Map.of("parcelId",source.parcels().get(0),"condition","OK")).getStatus(),this::safeOnboardingFailure);
            assertEquals(200,onboardingWrite(mvc,owner,"POST",path+"/checks",UUID.randomUUID(),"\"v2\"",Map.of("parcelId",source.parcels().get(1),"condition","VERIFY","note","Synthetic discrepancy")).getStatus());assertEquals(409,onboardingWrite(mvc,owner,"POST",path+"/transitions",UUID.randomUUID(),"\"v3\"",Map.of("action","PACK")).getStatus());
            assertEquals(200,onboardingWrite(mvc,owner,"POST",path+"/checks",UUID.randomUUID(),"\"v3\"",Map.of("parcelId",source.parcels().get(1),"condition","OK")).getStatus());assertEquals(200,onboardingWrite(mvc,owner,"POST",path+"/transitions",UUID.randomUUID(),"\"v4\"",Map.of("action","PACK")).getStatus(),this::safeOnboardingFailure);assertEquals(200,onboardingWrite(mvc,owner,"POST",path+"/transitions",UUID.randomUUID(),"\"v5\"",Map.of("action","READY")).getStatus());
            var batch=onboardingWrite(mvc,owner,"POST",batches,UUID.randomUUID(),null,Map.of("facilityId",source.site(),"shipmentIds",List.of(source.shipment())));assertEquals(200,batch.getStatus(),this::safeOnboardingFailure);String target=responseId(batch,"id"),batchPath=batches+"/"+target;
            assertEquals(409,onboardingWrite(mvc,owner,"POST",batches,UUID.randomUUID(),null,Map.of("facilityId",source.site(),"shipmentIds",List.of(source.shipment()))).getStatus());
            assertEquals(200,onboardingWrite(mvc,owner,"POST",batchPath+"/checks",UUID.randomUUID(),"\"v1\"",Map.of("parcelId",source.parcels().get(0),"result","PRESENT")).getStatus());assertEquals(200,onboardingWrite(mvc,owner,"POST",batchPath+"/checks",UUID.randomUUID(),"\"v2\"",Map.of("parcelId",source.parcels().get(1),"result","MISSING","note","Synthetic missing parcel")).getStatus());assertEquals(409,onboardingWrite(mvc,owner,"POST",batchPath+"/transitions",UUID.randomUUID(),"\"v3\"",Map.of("action","READY")).getStatus());
            assertEquals(200,onboardingWrite(mvc,owner,"POST",batchPath+"/checks",UUID.randomUUID(),"\"v3\"",Map.of("parcelId",source.parcels().get(1),"result","PRESENT")).getStatus());assertEquals(200,onboardingWrite(mvc,owner,"POST",batchPath+"/transitions",UUID.randomUUID(),"\"v4\"",Map.of("action","READY")).getStatus(),this::safeOnboardingFailure);
            assertEquals(409,onboardingWrite(mvc,owner,"POST",path+"/transitions",UUID.randomUUID(),"\"v6\"",Map.of("action","REOPEN","reason","Synthetic recount")).getStatus());
            var pdf=httpRead(mvc,batchPath+"/document",owner.access());assertEquals(200,pdf.getStatus());assertEquals("no-store",pdf.getHeader("Cache-Control"));try(var document=Loader.loadPDF(pdf.getContentAsByteArray())){String text=new org.apache.pdfbox.text.PDFTextStripper().getText(document);assertTrue(text.contains("NOT_CARRIER_ACCEPTANCE"));for(String p:source.parcels())assertTrue(text.contains(com.elogistic.backend.orders.internal.infrastructure.PilotLabelRenderer.barcode(UUID.fromString(p))));assertFalse(text.contains("Synthetic sender"));}
            var shipment=intakeJson(httpRead(mvc,base+"/shipments/"+source.shipment(),owner.access()));assertEquals("NOT_ACCEPTED",shipment.get("shipment").get("carrierState").asString());assertEquals("UNPAID",shipment.get("obligation").get("state").asString());assertEquals(0,scalar("SELECT count(*) FROM orders.pilot_tracking_event"));
            UUID cancel=UUID.randomUUID();var cancelled=onboardingWrite(mvc,owner,"POST",batchPath+"/transitions",cancel,"\"v5\"",Map.of("action","CANCEL","reason","Synthetic list replacement"));assertEquals(200,cancelled.getStatus());assertEquals("CANCELLED",intakeJson(cancelled).get("state").asString());assertEquals(cancelled.getContentAsString(),onboardingWrite(mvc,owner,"POST",batchPath+"/transitions",cancel,"\"v5\"",Map.of("action","CANCEL","reason","Synthetic list replacement")).getContentAsString());assertEquals(0,scalar("SELECT count(*) FROM orders.merchant_handover_shipment WHERE active"));assertEquals(12,scalar("SELECT count(*) FROM orders.merchant_facility_work_event"));assertEquals(12,scalar("SELECT count(*) FROM orders.merchant_facility_work_receipt"));
        }
    }
    @Test void merchantHandoverConcurrencyFaultRollbackAndReadScopePreserveSourceSnapshots()throws Exception{
        customerFixture(true);try(var inbox=new CustomerSmtpTestInbox(0,false,false,250,null);var c=customerHttpContext(PILOT_NOW,smtp(inbox,PILOT_NOW),true)){
            var mvc=httpMvc(c);var owner=profileBrowser(mvc);var worker=addressOtherBrowser(mvc);UUID org=id(9111);facilityWorkOwner(org);var first=facilityWorkSource(mvc,owner,org,null);var second=facilityWorkSource(mvc,owner,org,first.site());String batches=businessShippingBase(org)+"/handover-preparations";var input=Map.of("facilityId",first.site(),"shipmentIds",List.of(first.shipment(),second.shipment()));var race=adminRace(()->onboardingWrite(mvc,owner,"POST",batches,UUID.randomUUID(),null,input).getStatus(),()->onboardingWrite(mvc,owner,"POST",batches,UUID.randomUUID(),null,input).getStatus());assertEquals(1,race.stream().filter(v->Integer.valueOf(200).equals(v)).count());assertEquals(1,race.stream().filter(v->Integer.valueOf(409).equals(v)).count());assertEquals(1,scalar("SELECT count(*) FROM orders.merchant_handover_preparation"));assertEquals(2,scalar("SELECT count(*) FROM orders.merchant_handover_shipment"));String target=intakeJson(httpRead(mvc,batches,owner.access())).get("items").get(0).get("id").asString(),path=batches+"/"+target;
            for(String table:List.of("merchant_facility_work_event","merchant_facility_work_receipt")){db.ownerExecute("REVOKE INSERT ON orders."+table+" FROM \""+db.runtime()+"\"");try{assertEquals(503,onboardingWrite(mvc,owner,"POST",path+"/checks",UUID.randomUUID(),"\"v1\"",Map.of("parcelId",first.parcels().get(0),"result","PRESENT")).getStatus());assertEquals(0,scalar("SELECT count(*) FROM orders.merchant_handover_check"));assertEquals(1,scalar("SELECT version FROM orders.merchant_handover_preparation"));}finally{db.ownerExecute("GRANT INSERT ON orders."+table+" TO \""+db.runtime()+"\"");}}
            businessCommand(UUID.randomUUID(),"GRANT",org,B,"OPERATOR","'dashboard.read','shipment.draft.read'");assertEquals(404,httpRead(mvc,path+"/document",worker.access()).getStatus());assertEquals(404,httpRead(mvc,batches,worker.access()).getStatus());facilityGrant(org,B,first.site(),"'shipment.handover.read'");assertEquals(200,httpRead(mvc,path+"/document",worker.access()).getStatus());assertEquals(403,onboardingWrite(mvc,worker,"POST",path+"/transitions",UUID.randomUUID(),"\"v1\"",Map.of("action","READY")).getStatus());
            assertThrows(SQLException.class,()->execute("UPDATE orders.merchant_handover_preparation SET state='READY',version=version+1"));assertThrows(SQLException.class,()->execute("UPDATE orders.merchant_facility_work_event SET snapshot='{}'"));assertEquals(0,scalar("SELECT count(*) FROM orders.pilot_tracking_event"));assertEquals(2,scalar("SELECT count(*) FROM finance.pilot_shipping_obligation WHERE state='UNPAID'"));
        }
    }
    @Test void merchantFacilityCurrentMemberCandidatesCrossSiteAndArchiveDependenciesAreExplicit()throws Exception{
        customerFixture(true);try(var inbox=new CustomerSmtpTestInbox(0,false,false,250,null);var c=customerHttpContext(PILOT_NOW,smtp(inbox,PILOT_NOW),true)){
            var mvc=httpMvc(c);var owner=profileBrowser(mvc);var worker=addressOtherBrowser(mvc);UUID org=id(9112),foreign=id(9113);preparationOwner(org);preparationOwner(foreign);businessCommand(UUID.randomUUID(),"GRANT",org,B,"OPERATOR","'dashboard.read'");facilityGrant(org,A,null,"'facility.read','facility.manage','defaults.manage'");String path=facilityPath(org),one=responseId(onboardingWrite(mvc,owner,"POST",path,UUID.randomUUID(),null,facilityInput("ONE")),"id"),two=responseId(onboardingWrite(mvc,owner,"POST",path,UUID.randomUUID(),null,facilityInput("TWO")),"id");facilityGrant(org,B,UUID.fromString(one),"'facility.read'");
            var members=httpRead(mvc,path+"/"+one+"/members",owner.access());assertEquals(200,members.getStatus());assertEquals(2,intakeJson(members).get("items").size());assertEquals(404,httpRead(mvc,path+"/"+two,worker.access()).getStatus());assertEquals(404,httpRead(mvc,facilityPath(foreign)+"/"+one,owner.access()).getStatus());assertEquals(1,intakeJson(httpRead(mvc,path,worker.access())).get("items").size());
            var in=new HashMap<String,Object>(shippingDraftInput("Site source"));in.put("facilityId",UUID.fromString(two));assertEquals(201,onboardingWrite(mvc,owner,"POST",preparationPath(org),UUID.randomUUID(),null,in).getStatus());assertEquals(409,onboardingWrite(mvc,owner,"POST",path+"/"+two+"/archive",UUID.randomUUID(),"\"v1\"","").getStatus());assertEquals(409,onboardingWrite(mvc,owner,"POST",path+"/"+one+"/archive",UUID.randomUUID(),"\"v1\"","").getStatus());
            var search=onboardingWrite(mvc,owner,"POST",path+"/search",UUID.randomUUID(),null,Map.of("page",0,"size",20,"search","%_"));assertEquals(200,search.getStatus());assertEquals(0,intakeJson(search).get("items").size());
            var race=adminRace(()->onboardingWrite(mvc,owner,"PUT",businessShippingBase(org)+"/facility-default",UUID.randomUUID(),"\"d0\"",Map.of("facilityId",one)).getStatus(),()->onboardingWrite(mvc,owner,"PUT",businessShippingBase(org)+"/facility-default",UUID.randomUUID(),"\"d0\"",Map.of("facilityId",two)).getStatus());assertEquals(1,race.stream().filter(v->Integer.valueOf(200).equals(v)).count());assertEquals(1,race.stream().filter(v->Integer.valueOf(409).equals(v)).count());assertEquals(1,scalar("SELECT count(*) FROM merchant.facility_default"));
            String third=responseId(onboardingWrite(mvc,owner,"POST",path,UUID.randomUUID(),null,facilityInput("THREE")),"id");var archiveRace=adminRace(()->onboardingWrite(mvc,owner,"PUT",businessShippingBase(org)+"/facility-default",UUID.randomUUID(),"\"d1\"",Map.of("facilityId",third)).getStatus(),()->onboardingWrite(mvc,owner,"POST",path+"/"+third+"/archive",UUID.randomUUID(),"\"v1\"","").getStatus());assertEquals(1,archiveRace.stream().filter(v->Integer.valueOf(200).equals(v)).count());assertTrue(archiveRace.stream().allMatch(v->Set.of(200,404,409).contains(v)));assertEquals(0,scalar("SELECT count(*) FROM merchant.facility_default d JOIN merchant.facility f ON f.id=d.facility_id WHERE f.state='ARCHIVED'"));
        }
    }
    @Test void merchantHandoverItemChangesAndReopenInvalidateChecksWhileProductionAndForeignParcelsAreDenied()throws Exception{
        customerFixture(true);try(var inbox=new CustomerSmtpTestInbox(0,false,false,250,null);var c=customerHttpContext(PILOT_NOW,smtp(inbox,PILOT_NOW),true)){
            var mvc=httpMvc(c);var owner=profileBrowser(mvc);UUID org=id(9114);facilityWorkOwner(org);var first=facilityWorkSource(mvc,owner,org,null);var second=facilityWorkSource(mvc,owner,org,first.site());var foreign=facilityWorkSource(mvc,owner,org,null);String base=businessShippingBase(org),batches=base+"/handover-preparations";var create=onboardingWrite(mvc,owner,"POST",batches,UUID.randomUUID(),null,Map.of("facilityId",first.site(),"shipmentIds",List.of(first.shipment())));assertEquals(200,create.getStatus(),this::safeOnboardingFailure);String path=batches+"/"+responseId(create,"id");
            assertEquals(409,onboardingWrite(mvc,owner,"POST",path+"/checks",UUID.randomUUID(),"\"v1\"",Map.of("parcelId",foreign.parcels().getFirst(),"result","PRESENT")).getStatus());assertEquals(200,onboardingWrite(mvc,owner,"POST",path+"/checks",UUID.randomUUID(),"\"v1\"",Map.of("parcelId",first.parcels().getFirst(),"result","VERIFY","note","Synthetic parcel discrepancy")).getStatus());assertEquals(409,onboardingWrite(mvc,owner,"PUT",path+"/items",UUID.randomUUID(),"\"v2\"",Map.of("shipmentIds",List.of(first.shipment(),foreign.shipment()))).getStatus());
            var edited=onboardingWrite(mvc,owner,"PUT",path+"/items",UUID.randomUUID(),"\"v2\"",Map.of("shipmentIds",List.of(first.shipment(),second.shipment())));assertEquals(200,edited.getStatus(),this::safeOnboardingFailure);assertEquals(0,intakeJson(edited).get("checks").size());assertEquals(2,intakeJson(edited).get("cycle").asInt());assertEquals(2,intakeJson(edited).get("shipments").size());
            var preps=base+"/shipment-preparation-work";String work=responseId(onboardingWrite(mvc,owner,"POST",preps,UUID.randomUUID(),null,Map.of("shipmentId",first.shipment())),"id"),prepPath=preps+"/"+work;int version=1;for(String parcel:first.parcels())assertEquals(200,onboardingWrite(mvc,owner,"POST",prepPath+"/checks",UUID.randomUUID(),"\"v"+(version++)+"\"",Map.of("parcelId",parcel,"condition","OK")).getStatus());assertEquals(200,onboardingWrite(mvc,owner,"POST",prepPath+"/transitions",UUID.randomUUID(),"\"v3\"",Map.of("action","PACK")).getStatus());var reopened=onboardingWrite(mvc,owner,"POST",prepPath+"/transitions",UUID.randomUUID(),"\"v4\"",Map.of("action","REOPEN","reason","Synthetic packing recheck"));assertEquals(200,reopened.getStatus(),this::safeOnboardingFailure);assertEquals(0,intakeJson(reopened).get("checks").size());assertEquals(2,intakeJson(reopened).get("cycle").asInt());
            assertEquals(401,mvc.perform(MockMvcRequestBuilders.get(path+"/document").secure(true)).andReturn().getResponse().getStatus());assertEquals(0,scalar("SELECT count(*) FROM orders.pilot_tracking_event"));assertEquals(3,scalar("SELECT count(*) FROM finance.pilot_shipping_obligation WHERE state='UNPAID'"));
        }
        for(String mode:List.of("DISABLED","PRODUCTION","TEST_WITH_PRODUCTION_PROFILE"))try(var inbox=new CustomerSmtpTestInbox(0,false,false,250,null);var c=customerHttpContext(PILOT_NOW,smtp(inbox,PILOT_NOW),true,mode)){var mvc=httpMvc(c);var owner=profileBrowser(mvc);String base=businessShippingBase(id(9114));assertEquals(503,httpRead(mvc,base+"/handover-preparations",owner.access()).getStatus());assertEquals(503,onboardingWrite(mvc,owner,"POST",base+"/shipment-preparation-work",UUID.randomUUID(),null,Map.of("shipmentId",id(9999))).getStatus());}
    }
    private String preparationPath(UUID org){return "/api/v1/merchant-organizations/"+org+"/shipment-preparations";}
    private void preparationOwner(UUID org)throws Exception{businessCommand(UUID.randomUUID(),"PROVISION",org,A,"OWNER","'dashboard.read','shipment.draft.read','shipment.draft.write'");}
    @Test void businessPreparationCrudReplayAndSourceBindingDoNotCreateShipment()throws Exception{
        customerFixture(true);try(var inbox=new CustomerSmtpTestInbox(0,false,false,250,null);var c=customerHttpContext(PILOT_NOW,smtp(inbox,PILOT_NOW),true)){
            var mvc=httpMvc(c);var owner=profileBrowser(mvc);UUID org=id(860);preparationOwner(org);String path=preparationPath(org);UUID key=UUID.randomUUID();
            var input=new HashMap<String,Object>(shippingDraftInput("Business draft"));input.put("sourceChannel","MANUAL");input.put("sourceOrderReference","DEMO-1");
            var created=onboardingWrite(mvc,owner,"POST",path,key,null,input);assertEquals(201,created.getStatus(),this::safeOnboardingFailure);String target=responseId(created,"id");assertEquals(org.toString(),intakeJson(created).get("organizationId").asString());assertEquals(A.toString(),intakeJson(created).get("creatorAccountId").asString());assertFalse(intakeJson(created).has("accountId"));assertEquals("\"v1\"",created.getHeader("ETag"));assertEquals("no-store",created.getHeader("Cache-Control"));
            assertEquals(created.getContentAsString(),onboardingWrite(mvc,owner,"POST",path,key,null,input).getContentAsString());
            input.put("reference","Changed");assertEquals(409,onboardingWrite(mvc,owner,"POST",path,key,null,input).getStatus());
            assertEquals(200,onboardingWrite(mvc,owner,"PUT",path+"/"+target,UUID.randomUUID(),"\"v1\"",input).getStatus());
            input.put("sourceOrderReference","DIFFERENT");assertEquals(409,onboardingWrite(mvc,owner,"PUT",path+"/"+target,UUID.randomUUID(),"\"v2\"",input).getStatus());input.put("sourceOrderReference","DEMO-1");
            assertEquals(409,onboardingWrite(mvc,owner,"POST",path,UUID.randomUUID(),null,input).getStatus());
            assertEquals(200,httpRead(mvc,path+"/"+target,owner.access()).getStatus());assertTrue(httpRead(mvc,path,owner.access()).getContentAsString().contains(target));
            assertFalse(httpRead(mvc,DRAFT_PATH,owner.access()).getContentAsString().contains(target));
            UUID archive=UUID.randomUUID();assertEquals(200,onboardingWrite(mvc,owner,"POST",path+"/"+target+"/archive",archive,"\"v2\"","").getStatus());assertEquals(200,onboardingWrite(mvc,owner,"POST",path+"/"+target+"/archive",archive,"\"v2\"","").getStatus());assertFalse(httpRead(mvc,path,owner.access()).getContentAsString().contains(target));
            assertEquals(200,httpRead(mvc,"/api/v1/merchant-organizations/"+org+"/shipping-preparation-receipts/CREATE/"+key,owner.access()).getStatus());
            assertEquals(3,scalar("SELECT count(*) FROM orders.organization_preparation_audit"));assertEquals(3,scalar("SELECT count(*) FROM orders.organization_preparation_receipt"));assertEquals(0,scalar("SELECT count(*) FROM orders.pilot_shipment"));assertEquals(0,scalar("SELECT count(*) FROM finance.pilot_shipping_obligation"));assertEquals(0,scalar("SELECT count(*) FROM orders.pilot_tracking_event"));
        }
    }
    @Test void businessPreparationOrganizationFacilityAndRevokedAuthorityStayIsolated()throws Exception{
        customerFixture(true);try(var inbox=new CustomerSmtpTestInbox(0,false,false,250,null);var c=customerHttpContext(PILOT_NOW,smtp(inbox,PILOT_NOW),true)){
            var mvc=httpMvc(c);var owner=profileBrowser(mvc);var member=addressOtherBrowser(mvc);UUID org=id(861),other=id(862),site=id(863),foreign=id(864);preparationOwner(org);preparationOwner(other);businessCommand(UUID.randomUUID(),"GRANT",org,B,"OPERATOR","'dashboard.read'");
            String facilityContact=new tools.jackson.databind.ObjectMapper().writeValueAsString(shippingDraftInput("Site").get("sender")).replace("0901234567","+84901234567");
            seedFacility(site,org,facilityContact);seedFacility(foreign,other,facilityContact);
            seedScopedLegacyGrants(org,B,site,"'shipment.draft.read','shipment.draft.write'");
            String path=preparationPath(org);var input=new HashMap<String,Object>(shippingDraftInput("Scoped"));input.put("facilityId",site);String target=responseId(onboardingWrite(mvc,member,"POST",path,UUID.randomUUID(),null,input),"id");
            assertEquals(200,httpRead(mvc,path+"/"+target,member.access()).getStatus());assertTrue(httpRead(mvc,path,member.access()).getContentAsString().contains(target));assertEquals(404,httpRead(mvc,preparationPath(other)+"/"+target,owner.access()).getStatus());
            input.remove("facilityId");assertEquals(404,onboardingWrite(mvc,member,"POST",path,UUID.randomUUID(),null,input).getStatus());input.put("facilityId",foreign);assertEquals(404,onboardingWrite(mvc,owner,"POST",path,UUID.randomUUID(),null,input).getStatus());
            String unscoped=responseId(onboardingWrite(mvc,owner,"POST",path,UUID.randomUUID(),null,shippingDraftInput("Org only")),"id");assertEquals(404,httpRead(mvc,path+"/"+unscoped,member.access()).getStatus());assertFalse(httpRead(mvc,path,member.access()).getContentAsString().contains(unscoped));
            businessCommand(UUID.randomUUID(),"REVOKE",org,B,"OPERATOR","");assertEquals(404,httpRead(mvc,path+"/"+target,member.access()).getStatus());assertEquals(404,onboardingWrite(mvc,member,"PUT",path+"/"+target,UUID.randomUUID(),"\"v1\"",input).getStatus());
            assertThrows(SQLException.class,()->execute("INSERT INTO merchant.member_grant(id,membership_id,organization_id,permission,scope) SELECT gen_random_uuid(),id,organization_id,'shipment.book','ORGANIZATION' FROM merchant.membership LIMIT 1"));
        }
    }
    @Test void businessPreparationConcurrencyValidationAndEvidenceRollback()throws Exception{
        customerFixture(true);try(var inbox=new CustomerSmtpTestInbox(0,false,false,250,null);var c=customerHttpContext(PILOT_NOW,smtp(inbox,PILOT_NOW),true)){
            var mvc=httpMvc(c);var owner=profileBrowser(mvc);UUID org=id(865);preparationOwner(org);String path=preparationPath(org);var input=shippingDraftInput("Concurrent");UUID key=UUID.randomUUID();
            assertEquals(List.of(201,201),adminRace(()->onboardingWrite(mvc,owner,"POST",path,key,null,input).getStatus(),()->onboardingWrite(mvc,owner,"POST",path,key,null,input).getStatus()));assertEquals(1,scalar("SELECT count(*) FROM orders.organization_preparation"));
            String target=intakeJson(httpRead(mvc,path,owner.access())).get("items").get(0).get("id").asString();var race=adminRace(()->onboardingWrite(mvc,owner,"PUT",path+"/"+target,UUID.randomUUID(),"\"v1\"",shippingDraftInput("Edit one")).getStatus(),()->onboardingWrite(mvc,owner,"POST",path+"/"+target+"/archive",UUID.randomUUID(),"\"v1\"","").getStatus());assertEquals(1,race.stream().filter(v->Integer.valueOf(200).equals(v)).count());assertEquals(1,race.stream().filter(v->Integer.valueOf(409).equals(v)).count());
            for(String table:List.of("organization_preparation_version","organization_preparation_audit","organization_preparation_receipt")){
                db.ownerExecute("REVOKE INSERT ON orders."+table+" FROM \""+db.runtime()+"\"");try{assertEquals(503,onboardingWrite(mvc,owner,"POST",path,UUID.randomUUID(),null,input).getStatus());assertEquals(1,scalar("SELECT count(*) FROM orders.organization_preparation"));}finally{db.ownerExecute("GRANT INSERT ON orders."+table+" TO \""+db.runtime()+"\"");}
            }
            var invalid=new HashMap<String,Object>(input);invalid.put("organizationId",org);assertEquals(400,onboardingWrite(mvc,owner,"POST",path,UUID.randomUUID(),null,invalid).getStatus());assertEquals(400,httpRead(mvc,path+"?actorId="+A,owner.access()).getStatus());assertEquals(428,onboardingWrite(mvc,owner,"PUT",path+"/"+target,UUID.randomUUID(),null,input).getStatus());
            assertThrows(SQLException.class,()->execute("DELETE FROM orders.organization_preparation"));assertThrows(SQLException.class,()->execute("UPDATE orders.organization_preparation_version SET snapshot='{}'"));assertThrows(SQLException.class,()->execute("UPDATE orders.organization_preparation SET version=version+1"));
        }
        for(String mode:List.of("DISABLED","PRODUCTION","TEST_WITH_PRODUCTION_PROFILE"))try(var inbox=new CustomerSmtpTestInbox(0,false,false,250,null);var c=customerHttpContext(PILOT_NOW,smtp(inbox,PILOT_NOW),true,mode)){
            var mvc=httpMvc(c);var owner=profileBrowser(mvc);assertEquals(503,httpRead(mvc,preparationPath(id(865)),owner.access()).getStatus());assertEquals(503,onboardingWrite(mvc,owner,"POST",preparationPath(id(865)),UUID.randomUUID(),null,shippingDraftInput("Disabled")).getStatus());assertEquals(503,httpRead(mvc,"/api/v1/merchant-organizations/"+id(865)+"/shipment-imports",owner.access()).getStatus());assertEquals(503,onboardingWrite(mvc,owner,"POST","/api/v1/merchant-organizations/"+id(865)+"/shipment-imports",UUID.randomUUID(),null,Map.of("templateVersion","BUSINESS_CSV_V1","csv",businessCsv(1))).getStatus());
        }
    }

    private String businessCsv(int rows){
        var headers=com.elogistic.backend.orders.internal.application.BusinessCsv.HEADERS;var csv=new StringBuilder(String.join(",",headers)+"\r\n");
        String parcels=new tools.jackson.databind.ObjectMapper().writeValueAsString(shippingDraftInput("CSV").get("parcels"));
        for(int i=1;i<=rows;i++){var fields=List.of("CSV","SOURCE-"+i,"Synthetic sender","0901234567","VN","Synthetic area","Synthetic locality","Synthetic street","Synthetic recipient","+84901234567","VN","Synthetic destination","Synthetic locality","Synthetic recipient street",parcels,"CSV-"+i,"");csv.append(fields.stream().map(v->"\""+v.replace("\"","\"\"")+"\"").collect(java.util.stream.Collectors.joining(","))).append("\r\n");}return csv.toString();
    }
    @Test void businessImportPreviewChunksReplayAndSourceConflictsPersistRealOutcomes()throws Exception{
        customerFixture(true);try(var inbox=new CustomerSmtpTestInbox(0,false,false,250,null);var c=customerHttpContext(PILOT_NOW,smtp(inbox,PILOT_NOW),true)){
            var mvc=httpMvc(c);var owner=profileBrowser(mvc);UUID org=id(866);businessCommand(UUID.randomUUID(),"PROVISION",org,A,"OWNER","'dashboard.read','shipment.draft.read','shipment.draft.write','shipment.import'");String path="/api/v1/merchant-organizations/"+org+"/shipment-imports";UUID previewKey=UUID.randomUUID();var input=Map.of("templateVersion","BUSINESS_CSV_V1","csv",businessCsv(22));
            var preview=onboardingWrite(mvc,owner,"POST",path,previewKey,null,input);assertEquals(201,preview.getStatus(),this::safeOnboardingFailure);String job=responseId(preview,"id");assertEquals(22,intakeJson(preview).get("validRows").asInt());assertEquals(20,intakeJson(preview).get("rows").size());assertEquals(0,scalar("SELECT count(*) FROM orders.organization_preparation"));assertEquals(preview.getContentAsString(),onboardingWrite(mvc,owner,"POST",path,previewKey,null,input).getContentAsString());
            var selected=java.util.stream.IntStream.rangeClosed(1,20).boxed().toList();UUID chunk=UUID.randomUUID();var first=onboardingWrite(mvc,owner,"POST",path+"/"+job+"/commit",chunk,"\"v1\"",Map.of("rows",selected));assertEquals(200,first.getStatus(),this::safeOnboardingFailure);assertEquals(20,intakeJson(first).get("createdRows").asInt());assertEquals("PARTIAL",intakeJson(first).get("state").asString());assertEquals(first.getContentAsString(),onboardingWrite(mvc,owner,"POST",path+"/"+job+"/commit",chunk,"\"v1\"",Map.of("rows",selected)).getContentAsString());
            assertEquals(409,onboardingWrite(mvc,owner,"POST",path+"/"+job+"/commit",UUID.randomUUID(),"\"v1\"",Map.of("rows",List.of(21,22))).getStatus());
            var completed=onboardingWrite(mvc,owner,"POST",path+"/"+job+"/commit",UUID.randomUUID(),"\"v2\"",Map.of("rows",List.of(21,22)));assertEquals(200,completed.getStatus(),this::safeOnboardingFailure);assertEquals("COMPLETED",intakeJson(completed).get("state").asString());assertEquals(22,intakeJson(completed).get("createdRows").asInt());assertEquals(2,intakeJson(httpRead(mvc,path+"/"+job+"?page=1",owner.access())).get("rows").size());
            String duplicate=responseId(onboardingWrite(mvc,owner,"POST",path,UUID.randomUUID(),null,Map.of("templateVersion","BUSINESS_CSV_V1","csv",businessCsv(1))),"id");var conflict=onboardingWrite(mvc,owner,"POST",path+"/"+duplicate+"/commit",UUID.randomUUID(),"\"v1\"",Map.of("rows",List.of(1)));assertEquals(200,conflict.getStatus());assertEquals(1,intakeJson(conflict).get("conflictRows").asInt());assertEquals(22,scalar("SELECT count(*) FROM orders.organization_preparation"));
            assertEquals(200,httpRead(mvc,"/api/v1/merchant-organizations/"+org+"/shipping-preparation-receipts/IMPORT_COMMIT/"+chunk,owner.access()).getStatus());
            assertEquals(0,scalar("SELECT count(*) FROM orders.pilot_shipment"));assertEquals(0,scalar("SELECT count(*) FROM finance.pilot_shipping_obligation"));
        }
    }
    @Test void businessImportParserIsolationRevocationAndFaultsCannotCreateUnconfirmedData()throws Exception{
        customerFixture(true);try(var inbox=new CustomerSmtpTestInbox(0,false,false,250,null);var c=customerHttpContext(PILOT_NOW,smtp(inbox,PILOT_NOW),true)){
            var mvc=httpMvc(c);var owner=profileBrowser(mvc);var other=addressOtherBrowser(mvc);UUID org=id(867);businessCommand(UUID.randomUUID(),"PROVISION",org,A,"OWNER","'dashboard.read','shipment.draft.read','shipment.draft.write','shipment.import'");String path="/api/v1/merchant-organizations/"+org+"/shipment-imports";
            for(String csv:List.of("bad,header\r\n1,2",businessCsv(101),businessCsv(1)+"\"unterminated",businessCsv(1).repeat(500))){assertEquals(400,onboardingWrite(mvc,owner,"POST",path,UUID.randomUUID(),null,Map.of("templateVersion","BUSINESS_CSV_V1","csv",csv)).getStatus());}
            String unsafe=businessCsv(1).replace("Synthetic sender","=HYPERLINK(1)");var preview=onboardingWrite(mvc,owner,"POST",path,UUID.randomUUID(),null,Map.of("templateVersion","BUSINESS_CSV_V1","csv",unsafe));assertEquals(201,preview.getStatus());assertEquals(1,intakeJson(preview).get("invalidRows").asInt());assertEquals(0,scalar("SELECT count(*) FROM orders.organization_preparation"));
            String duplicateRows=businessCsv(2).replace("SOURCE-2","SOURCE-1");var parsed=com.elogistic.backend.orders.internal.application.BusinessCsv.parse(null,duplicateRows);assertTrue(parsed.stream().allMatch(r->r.state().equals("INVALID")));assertEquals(100,com.elogistic.backend.orders.internal.application.BusinessCsv.parse(null,businessCsv(100)).size());
            String job=responseId(onboardingWrite(mvc,owner,"POST",path,UUID.randomUUID(),null,Map.of("templateVersion","BUSINESS_CSV_V1","csv",businessCsv(1))),"id");assertEquals(404,httpRead(mvc,path+"/"+job,other.access()).getStatus());assertEquals(404,onboardingWrite(mvc,other,"POST",path+"/"+job+"/commit",UUID.randomUUID(),"\"v1\"",Map.of("rows",List.of(1))).getStatus());
            db.ownerExecute("REVOKE INSERT ON orders.organization_preparation_audit FROM \""+db.runtime()+"\"");try{assertEquals(503,onboardingWrite(mvc,owner,"POST",path+"/"+job+"/commit",UUID.randomUUID(),"\"v1\"",Map.of("rows",List.of(1))).getStatus());assertEquals(0,scalar("SELECT count(*) FROM orders.organization_preparation"));assertEquals(1,scalar("SELECT version FROM orders.organization_import_job WHERE id='"+job+"'"));assertEquals(1,scalar("SELECT count(*) FROM orders.organization_import_row WHERE job_id='"+job+"' AND state='VALID'"));}finally{db.ownerExecute("GRANT INSERT ON orders.organization_preparation_audit TO \""+db.runtime()+"\"");}
            UUID key=UUID.randomUUID();assertEquals(List.of(200,200),adminRace(()->onboardingWrite(mvc,owner,"POST",path+"/"+job+"/commit",key,"\"v1\"",Map.of("rows",List.of(1))).getStatus(),()->onboardingWrite(mvc,owner,"POST",path+"/"+job+"/commit",key,"\"v1\"",Map.of("rows",List.of(1))).getStatus()));assertEquals(1,scalar("SELECT count(*) FROM orders.organization_preparation"));
            UUID site=id(868);String siteContact=new tools.jackson.databind.ObjectMapper().writeValueAsString(shippingDraftInput("Site").get("sender")).replace("0901234567","+84901234567");seedFacility(site,org,siteContact);
            UUID scopedKey=UUID.randomUUID();String scopedJob=responseId(onboardingWrite(mvc,owner,"POST",path,scopedKey,null,Map.of("templateVersion","BUSINESS_CSV_V1","facilityId",site,"csv",businessCsv(1))),"id");
            businessCommand(UUID.randomUUID(),"GRANT",org,B,"OPERATOR","'dashboard.read'");seedScopedLegacyGrants(org,B,site,"'shipment.draft.read','shipment.draft.write','shipment.import'");
            assertEquals(200,httpRead(mvc,path+"/"+scopedJob,other.access()).getStatus());assertEquals(404,httpRead(mvc,path+"/"+job,other.access()).getStatus());var restricted=httpRead(mvc,path,other.access());assertEquals(200,restricted.getStatus());assertFalse(restricted.getContentAsString().contains(job));assertTrue(restricted.getContentAsString().contains(scopedJob));assertEquals(404,httpRead(mvc,"/api/v1/merchant-organizations/"+org+"/shipping-preparation-receipts/IMPORT_PREVIEW/"+scopedKey,other.access()).getStatus());
            businessCommand(UUID.randomUUID(),"REVOKE",org,A,"OWNER","'shipment.import'");assertEquals(404,httpRead(mvc,path+"/"+job,owner.access()).getStatus());assertEquals(404,onboardingWrite(mvc,owner,"POST",path+"/"+job+"/commit",key,"\"v1\"",Map.of("rows",List.of(1))).getStatus());assertThrows(SQLException.class,()->execute("DELETE FROM orders.organization_import_row"));
        }
    }
    private static final String SUPPORT_PATH="/api/v1/customer/support-tickets", SUPPORT_STAFF="/api/v1/support-operations";
    private Map<String,Object> supportInput(){return Map.of("kind","GENERAL","category","ACCOUNT_HELP","subject","Synthetic support subject","message","Synthetic initial conversation");}
    private String supportId(org.springframework.mock.web.MockHttpServletResponse r)throws Exception{assertEquals(201,r.getStatus(),this::safeOnboardingFailure);return intakeJson(r).get("id").asString();}
    private boolean claimIntakeEnabled;
    private boolean claimEvidenceEnabled;
    private com.elogistic.backend.platform.api.PrivateEvidenceStore claimStore;
    private com.elogistic.backend.platform.api.PrivateEvidenceScreening claimScanner;
    private boolean liveStaffClock;
    private UUID dedicatedStaff;
    private org.springframework.jdbc.datasource.DriverManagerDataSource staffOperatorSource;
    private InvitationProtection staffProtection(){return new InvitationProtection(Map.of("v1",keyBytes(77)),"v1",Map.of("e1",keyBytes(78)),"e1");}
    private void ensureStaffOperator()throws Exception{
        if(staffOperatorSource!=null)return;
        String operator="staff_test_"+UUID.randomUUID().toString().replace("-","");String password=PostgresTestcontainersSupport.password();
        try(var c=DriverManager.getConnection(PostgresTestcontainersSupport.currentUrl(POSTGRES,"postgres"),POSTGRES.getUsername(),POSTGRES.getPassword());var q=c.createStatement()){
            q.execute("CREATE ROLE "+operator+" LOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE PASSWORD '"+password+"'");q.execute("GRANT elogistics_local_staff_operator TO "+operator);q.execute("GRANT CONNECT ON DATABASE "+db.name()+" TO "+operator);
        }
        db.ownerExecute("SELECT identity.bind_local_staff_environment('"+UUID.randomUUID()+"','TEST','Synthetic isolated Staff workflow')");
        staffOperatorSource=new org.springframework.jdbc.datasource.DriverManagerDataSource(db.url(),operator,password);
    }
    private UUID staffFixtureId()throws Exception{
        if(dedicatedStaff!=null)return dedicatedStaff;ensureStaffOperator();
        var receipt=new LocalSupportStaffProvisioner(staffOperatorSource,staffProtection()).provision(new LocalSupportStaffProvisioner.OfflineStaffProvision(UUID.randomUUID(),"Synthetic dedicated Staff","dedicated-staff@example.invalid","Synthetic isolated workflow"));
        dedicatedStaff=receipt.accountId();var worker=new JdbcInvitationOutbox(outboxDataSource(),staffProtection(),outboxPolicy(),Clock.systemUTC(),com.elogistic.backend.identity.internal.domain.ActionProof.Purpose.STAFF_INV);
        var lease=worker.claim().orElseThrow();byte[] token;try(var delivery=worker.open(lease)){token=delivery.token().clone();}worker.complete(lease,JdbcInvitationOutbox.Outcome.SENT);
        try{new JdbcStaffInvitationActivation(outboxDataSource(),staffProtection(),Clock.systemUTC()).accept(receipt.proofId(),token,SESSION_PASSWORD,UUID.randomUUID(),UUID.randomUUID());}finally{Arrays.fill(token,(byte)0);}
        return dedicatedStaff;
    }
    private void supportOperator(UUID target,String permission,String action)throws Exception{
        assertEquals(staffFixtureId(),target);try(var c=staffOperatorSource.getConnection();var q=c.prepareStatement("SELECT * FROM orders.configure_local_support_staff(?,?,?,?,?)")){
            q.setObject(1,UUID.randomUUID());q.setObject(2,target);q.setString(3,permission);q.setString(4,action);q.setString(5,"Synthetic focused support authority");try(var r=q.executeQuery()){assertTrue(r.next());}
        }
    }
    private ProfileBrowser staffFixtureBrowser(org.springframework.test.web.servlet.MockMvc mvc)throws Exception{
        staffFixtureId();var csrf=mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/v1/staff-auth/csrf").secure(true).header("Origin","https://portal.example.invalid")).andReturn().getResponse();assertEquals(200,csrf.getStatus());
        var json=new tools.jackson.databind.ObjectMapper();String challenge=json.readTree(csrf.getContentAsString()).get("csrfToken").asString();
        var login=mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/v1/staff-auth/login").secure(true).header("Origin","https://portal.example.invalid").header("X-CSRF-Token",challenge).cookie(responseCookie(csrf,"__Host-argopost.staff.csrf")).contentType("application/json").content(json.writeValueAsString(Map.of("email","dedicated-staff@example.invalid","password",SESSION_PASSWORD)))).andReturn().getResponse();assertEquals(200,login.getStatus());
        var cookie=responseCookie(login,"__Host-argopost.staff.csrf");var refresh=responseCookie(login,"__Host-argopost.staff.refresh");
        var rebound=mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/v1/staff-auth/csrf").secure(true).header("Origin","https://portal.example.invalid").cookie(cookie,refresh)).andReturn().getResponse();assertEquals(200,rebound.getStatus());
        return new ProfileBrowser(json.readTree(login.getContentAsString()).get("accessToken").asString(),new BrowserFlow(responseCookie(rebound,"__Host-argopost.staff.csrf"),refresh,json.readTree(rebound.getContentAsString()).get("csrfToken").asString()));
    }
    private void supportGrant(UUID account,String permission)throws Exception{supportOperator(account,permission,"GRANT");}
    private Map<String,Object> staffGrantCommand(UUID key,String permission,String action,String reason)throws Exception{
        try(var c=staffOperatorSource.getConnection();var q=c.prepareStatement("SELECT * FROM orders.configure_local_support_staff(?,?,?,?,?)")){
            q.setObject(1,key);q.setObject(2,dedicatedStaff);q.setString(3,permission);q.setString(4,action);q.setString(5,reason);
            try(var r=q.executeQuery()){assertTrue(r.next());var value=new HashMap<String,Object>();value.put("outcome",r.getString("committed_outcome"));value.put("state",r.getString("current_grant_state"));value.put("grant",r.getObject("grant_id"));value.put("replayed",r.getBoolean("replayed"));value.put("active",r.getBoolean("effective_permission_active"));return value;}
        }
    }
    @Test void staffCanonicalConcurrentReceiptLateFailureAndRevokedOperatorCannotBypassAuthority()throws Exception{
        staffFixtureId();UUID key=UUID.randomUUID();
        var raced=adminRace(()->staffGrantCommand(key,"SUPPORT_TRIAGE","GRANT","Synthetic same request"),()->staffGrantCommand(key,"SUPPORT_TRIAGE","GRANT","Synthetic same request"));
        assertEquals(1,raced.stream().filter(v->Boolean.FALSE.equals(((Map<?,?>)v).get("replayed"))).count());
        assertEquals(1,scalar("SELECT count(*) FROM orders.support_staff_grant"));assertEquals(1,businessScalar("SELECT count(*) FROM orders.support_grant_operator_receipt"));
        assertEquals(1,scalar("SELECT count(*) FROM orders.support_business_audit"));
        assertEquals("23514",assertThrows(SQLException.class,()->staffGrantCommand(key,"SUPPORT_TRIAGE","GRANT","Changed reason")).getSQLState());
        var absent=staffGrantCommand(UUID.randomUUID(),"SUPPORT_HANDLE","REVOKE","Synthetic absent revoke");assertEquals("ABSENT",absent.get("state"));assertEquals("NO_CHANGE",absent.get("outcome"));assertFalse((Boolean)absent.get("active"));
        db.ownerExecute("CREATE FUNCTION orders.synthetic_cskh_receipt_failure() RETURNS trigger LANGUAGE plpgsql AS $$ BEGIN RAISE EXCEPTION 'SYNTHETIC_RECEIPT_FAILURE';END $$;CREATE TRIGGER synthetic_cskh_receipt_failure BEFORE INSERT ON orders.support_grant_operator_receipt FOR EACH ROW EXECUTE FUNCTION orders.synthetic_cskh_receipt_failure()");
        try{assertThrows(SQLException.class,()->staffGrantCommand(UUID.randomUUID(),"SUPPORT_HANDLE","GRANT","Synthetic rollback"));assertEquals(1,scalar("SELECT count(*) FROM orders.support_staff_grant"));assertEquals(1,scalar("SELECT count(*) FROM orders.support_business_audit"));assertEquals(2,businessScalar("SELECT count(*) FROM orders.support_grant_operator_receipt"));}
        finally{db.ownerExecute("DROP TRIGGER synthetic_cskh_receipt_failure ON orders.support_grant_operator_receipt;DROP FUNCTION orders.synthetic_cskh_receipt_failure()");}
        supportOperator(dedicatedStaff,"SUPPORT_TRIAGE","REVOKE");var replay=staffGrantCommand(key,"SUPPORT_TRIAGE","GRANT","Synthetic same request");assertTrue((Boolean)replay.get("replayed"));assertFalse((Boolean)replay.get("active"));assertEquals("REVOKED",replay.get("state"));
        String operator=staffOperatorSource.getUsername();
        try(var c=DriverManager.getConnection(PostgresTestcontainersSupport.currentUrl(POSTGRES,"postgres"),POSTGRES.getUsername(),POSTGRES.getPassword());var q=c.createStatement()){q.execute("REVOKE elogistics_local_staff_operator FROM "+operator);}
        assertEquals("42501",assertThrows(SQLException.class,()->staffGrantCommand(key,"SUPPORT_TRIAGE","GRANT","Synthetic same request")).getSQLState());
        assertEquals(1,scalar("SELECT count(*) FROM orders.support_staff_grant"));assertEquals(0,scalar("SELECT count(*) FROM orders.support_staff_grant WHERE state='ACTIVE'"));
    }
    @Test void staffResendInvalidatesOldProofAndConcurrentActivationHasOneReceipt()throws Exception{
        long existingAccounts=scalar("SELECT count(*) FROM identity.user_account"),existingGrants=scalar("SELECT count(*) FROM identity.account_grant");ensureStaffOperator();var protection=staffProtection();var provisioner=new LocalSupportStaffProvisioner(staffOperatorSource,protection);
        var receipt=provisioner.provision(new LocalSupportStaffProvisioner.OfflineStaffProvision(UUID.randomUUID(),"Synthetic pending Staff","resend-staff@example.invalid","Synthetic resend"));
        var outbox=new JdbcInvitationOutbox(outboxDataSource(),protection,outboxPolicy(),Clock.systemUTC(),com.elogistic.backend.identity.internal.domain.ActionProof.Purpose.STAFF_INV);
        var oldLease=outbox.claim().orElseThrow();byte[] old;try(var d=outbox.open(oldLease)){old=d.token().clone();}
        var command=new LocalSupportStaffProvisioner.OfflineStaffInvitationDisposition(UUID.randomUUID(),receipt.accountId(),"RESEND","Synthetic resend");
        var resent=provisioner.disposition(command);assertFalse(resent.replayed());assertTrue(provisioner.disposition(command).replayed());assertNotEquals(receipt.proofId(),resent.proofId());
        assertThrows(IllegalStateException.class,()->outbox.open(oldLease));var activation=new JdbcStaffInvitationActivation(outboxDataSource(),protection,Clock.systemUTC());
        try{assertEquals(TOKEN_INVALID_OR_EXPIRED,assertThrows(com.elogistic.backend.identity.api.IdentityCommandFailure.class,()->activation.accept(receipt.proofId(),old,SESSION_PASSWORD,UUID.randomUUID(),UUID.randomUUID())).code());}finally{Arrays.fill(old,(byte)0);}
        var lease=outbox.claim().orElseThrow();byte[] token;try(var d=outbox.open(lease)){token=d.token().clone();}outbox.complete(lease,JdbcInvitationOutbox.Outcome.SENT);UUID key=UUID.randomUUID();
        try{assertEquals(List.of("ACCEPTED","ACCEPTED"),adminRace(()->{activation.accept(resent.proofId(),token,SESSION_PASSWORD,key,UUID.randomUUID());return "ACCEPTED";},()->{activation.accept(resent.proofId(),token,SESSION_PASSWORD,key,UUID.randomUUID());return "ACCEPTED";}));}finally{Arrays.fill(token,(byte)0);}
        assertEquals(1,scalar("SELECT count(*) FROM identity.command_receipt WHERE operation='acceptStaffInvitation'"));assertEquals(1,scalar("SELECT count(*) FROM identity.audit_entry WHERE event_type='STAFF_INVITATION_ACCEPTED'"));assertEquals(existingAccounts+1,scalar("SELECT count(*) FROM identity.user_account"));
        for(String sql:List.of("UPDATE identity.user_account SET status='DISABLED' WHERE id='"+receipt.accountId()+"'","DELETE FROM identity.local_staff_principal","UPDATE identity.action_proof SET purpose='INVITATION' WHERE purpose='STAFF_INV'"))assertThrows(SQLException.class,()->execute(sql));
        assertEquals(existingGrants,scalar("SELECT count(*) FROM identity.account_grant"));assertEquals(0,scalar("SELECT count(*) FROM identity.account_grant WHERE account_id='"+receipt.accountId()+"'"));assertEquals(0,scalar("SELECT count(*) FROM merchant.membership WHERE account_id='"+receipt.accountId()+"'"));
    }
    @Test void staffOutboxLeaseRecoveryExhaustionAndCancellationStayPurposeBound()throws Exception{
        ensureStaffOperator();var provisioner=new LocalSupportStaffProvisioner(staffOperatorSource,staffProtection());var source=outboxDataSource();var purpose=com.elogistic.backend.identity.internal.domain.ActionProof.Purpose.STAFF_INV;
        var first=provisioner.provision(new LocalSupportStaffProvisioner.OfflineStaffProvision(UUID.randomUUID(),"Synthetic retry Staff","retry-staff@example.invalid","Synthetic delivery recovery"));Instant base=Instant.now().plusSeconds(1);var initial=new JdbcInvitationOutbox(source,staffProtection(),outboxPolicy(),Clock.fixed(base,ZoneOffset.UTC),purpose);var lease=initial.claim().orElseThrow();
        assertTrue(new JdbcInvitationOutbox(source,staffProtection(),outboxPolicy(),Clock.fixed(base,ZoneOffset.UTC)).claim().isEmpty());
        var recovered=new JdbcInvitationOutbox(source,staffProtection(),outboxPolicy(),Clock.fixed(base.plusSeconds(6),ZoneOffset.UTC),purpose);var next=recovered.claim().orElseThrow();assertEquals(lease.messageId(),next.messageId());assertNotEquals(lease.leaseId(),next.leaseId());assertThrows(IllegalStateException.class,()->initial.open(lease));
        recovered.complete(next,JdbcInvitationOutbox.Outcome.TRANSIENT_FAILURE);assertTrue(recovered.claim().isEmpty());assertEquals(1,scalar("SELECT count(*) FROM identity.invitation_outbox WHERE account_id='"+first.accountId()+"' AND status='FAILED' AND attempts=2 AND nonce IS NULL AND ciphertext IS NULL"));
        var cancelled=provisioner.provision(new LocalSupportStaffProvisioner.OfflineStaffProvision(UUID.randomUUID(),"Synthetic cancel Staff","cancel-staff@example.invalid","Synthetic cancellation after claim"));var current=new JdbcInvitationOutbox(source,staffProtection(),outboxPolicy(),Clock.systemUTC(),purpose);var reserved=current.claim().orElseThrow();provisioner.disposition(new LocalSupportStaffProvisioner.OfflineStaffInvitationDisposition(UUID.randomUUID(),cancelled.accountId(),"CANCEL","Synthetic cancellation"));assertThrows(IllegalStateException.class,()->current.open(reserved));assertEquals(1,scalar("SELECT count(*) FROM identity.invitation_outbox WHERE account_id='"+cancelled.accountId()+"' AND nonce IS NULL AND ciphertext IS NULL"));
        var expired=provisioner.provision(new LocalSupportStaffProvisioner.OfflineStaffProvision(UUID.randomUUID(),"Synthetic expired Staff","expired-staff@example.invalid","Synthetic expiry"));assertTrue(new JdbcInvitationOutbox(source,staffProtection(),outboxPolicy(),Clock.offset(Clock.systemUTC(),Duration.ofHours(25)),purpose).claim().isEmpty());assertEquals(1,scalar("SELECT count(*) FROM identity.invitation_outbox WHERE account_id='"+expired.accountId()+"' AND status='EXPIRED' AND nonce IS NULL AND ciphertext IS NULL"));
        assertEquals(0,scalar("SELECT count(*) FROM identity.local_staff_principal WHERE state='ACTIVE'"));assertEquals(0,scalar("SELECT count(*) FROM orders.support_staff_grant"));
    }

    @Test void staffExpiredCancelledAndLateAuditFailureCannotActivateIdentity()throws Exception{
        ensureStaffOperator();var provisioner=new LocalSupportStaffProvisioner(staffOperatorSource,staffProtection());var source=outboxDataSource();
        for(int scenario=0;scenario<3;scenario++){
            var receipt=provisioner.provision(new LocalSupportStaffProvisioner.OfflineStaffProvision(UUID.randomUUID(),"Synthetic pending CSKH","pending-cskh-"+scenario+"@example.invalid","Synthetic negative activation"));
            var outbox=new JdbcInvitationOutbox(source,staffProtection(),outboxPolicy(),Clock.systemUTC(),com.elogistic.backend.identity.internal.domain.ActionProof.Purpose.STAFF_INV);var lease=outbox.claim().orElseThrow();byte[] token;try(var mail=outbox.open(lease)){token=mail.token().clone();}outbox.complete(lease,JdbcInvitationOutbox.Outcome.SENT);
            try{
                if(scenario==0){var expired=new JdbcStaffInvitationActivation(source,staffProtection(),Clock.offset(Clock.systemUTC(),Duration.ofHours(24).plusSeconds(1)));assertEquals(TOKEN_INVALID_OR_EXPIRED,assertThrows(IdentityCommandFailure.class,()->expired.accept(receipt.proofId(),token,SESSION_PASSWORD,UUID.randomUUID(),UUID.randomUUID())).code());}
                if(scenario==1){provisioner.disposition(new LocalSupportStaffProvisioner.OfflineStaffInvitationDisposition(UUID.randomUUID(),receipt.accountId(),"CANCEL","Synthetic cancellation"));assertEquals(TOKEN_INVALID_OR_EXPIRED,assertThrows(IdentityCommandFailure.class,()->new JdbcStaffInvitationActivation(source,staffProtection(),Clock.systemUTC()).accept(receipt.proofId(),token,SESSION_PASSWORD,UUID.randomUUID(),UUID.randomUUID())).code());}
                if(scenario==2){failWrite("audit_entry","INSERT");try{assertThrows(RuntimeException.class,()->new JdbcStaffInvitationActivation(source,staffProtection(),Clock.systemUTC()).accept(receipt.proofId(),token,SESSION_PASSWORD,UUID.randomUUID(),UUID.randomUUID()));}finally{db.ownerExecute("DROP TRIGGER synthetic_fault ON identity.audit_entry;DROP FUNCTION identity.synthetic_fault()");}}
                assertEquals(0,scalar("SELECT count(*) FROM identity.user_account WHERE id='"+receipt.accountId()+"' AND (email_verified OR status='ACTIVE' OR password_hash IS NOT NULL)"));assertEquals(0,scalar("SELECT count(*) FROM identity.action_proof WHERE id='"+receipt.proofId()+"' AND consumed_at IS NOT NULL"));assertEquals(0,scalar("SELECT count(*) FROM identity.command_receipt WHERE operation='acceptStaffInvitation' AND proof_id='"+receipt.proofId()+"'"));
            }finally{Arrays.fill(token,(byte)0);}
        }
        assertEquals(0,scalar("SELECT count(*) FROM orders.support_staff_grant"));assertEquals(0,scalar("SELECT count(*) FROM orders.organization_support_staff_scope"));
    }

    @Test void staffOfflineResendQuotaCountsCommittedRequestsAndPreservesExactReplay()throws Exception{
        ensureStaffOperator();var service=new LocalSupportStaffProvisioner(staffOperatorSource,staffProtection());
        var provision=service.provision(new LocalSupportStaffProvisioner.OfflineStaffProvision(UUID.randomUUID(),"Synthetic rate Staff","rate-staff@example.invalid","Synthetic rate test"));
        var input=new LocalSupportStaffProvisioner.OfflineStaffInvitationDisposition(UUID.randomUUID(),provision.accountId(),"RESEND","Synthetic rate request");
        for(int i=0;i<3;i++)service.disposition(i==2?input:new LocalSupportStaffProvisioner.OfflineStaffInvitationDisposition(UUID.randomUUID(),provision.accountId(),"RESEND","Synthetic rate request"));
        assertTrue(service.disposition(input).replayed());
        assertThrows(IllegalStateException.class,()->service.disposition(new LocalSupportStaffProvisioner.OfflineStaffInvitationDisposition(UUID.randomUUID(),provision.accountId(),"RESEND","Synthetic rate excess")));
        assertEquals(3,scalar("SELECT count(*) FROM identity.audit_entry WHERE event_type='STAFF_INVITATION_RESENT'"));assertEquals(4,scalar("SELECT count(*) FROM identity.action_proof WHERE purpose='STAFF_INV'"));assertEquals(1,scalar("SELECT count(*) FROM identity.invitation_outbox WHERE purpose='STAFF_INV' AND status='PENDING'"));
    }

    private org.springframework.mock.web.MockHttpServletResponse supportWrite(org.springframework.test.web.servlet.MockMvc mvc,ProfileBrowser browser,String path,int version,Object input)throws Exception{return onboardingWrite(mvc,browser,"POST",path,UUID.randomUUID(),"\"v"+version+"\"",input==null?"":input);}
    @Test void supportTicketConversationRequiresCurrentOwnerAndAssignedStaff()throws Exception{
        liveStaffClock=true;customerFixture(true);try(var inbox=new CustomerSmtpTestInbox(0,false,false,250,null);var c=customerHttpContext(PILOT_NOW,smtp(inbox,PILOT_NOW),true)){
            var mvc=httpMvc(c);var owner=profileBrowser(mvc);var staff=staffFixtureBrowser(mvc);String target=supportId(onboardingWrite(mvc,owner,"POST",SUPPORT_PATH,UUID.randomUUID(),null,supportInput()));
            assertEquals(401,httpRead(mvc,SUPPORT_PATH+"/"+target,staff.access()).getStatus());
            assertEquals(401,httpRead(mvc,SUPPORT_STAFF+"/queue",owner.access()).getStatus());assertEquals(403,httpRead(mvc,SUPPORT_STAFF+"/cases/"+target,staff.access()).getStatus());
            supportGrant(staffFixtureId(),"SUPPORT_TRIAGE");supportGrant(staffFixtureId(),"SUPPORT_HANDLE");assertTrue(httpRead(mvc,SUPPORT_STAFF+"/queue",staff.access()).getContentAsString().contains(target));
            var assignment=supportWrite(mvc,staff,SUPPORT_STAFF+"/cases/"+target+"/assignments",1,Map.of("assigneeAccountId",staffFixtureId()));assertEquals(200,assignment.getStatus(),this::safeOnboardingFailure);assertFalse(assignment.getContentAsString().contains("publicMessages"));
            assertEquals(200,supportWrite(mvc,staff,SUPPORT_STAFF+"/cases/"+target+"/transitions",2,Map.of("action","START","message","Started investigation")).getStatus());
            assertEquals(200,supportWrite(mvc,staff,SUPPORT_STAFF+"/cases/"+target+"/transitions",3,Map.of("action","REQUEST_INFORMATION","message","Please clarify the issue")).getStatus());
            var reply=supportWrite(mvc,owner,SUPPORT_PATH+"/"+target+"/replies",4,Map.of("message","Customer clarification"));assertEquals(200,reply.getStatus());assertEquals("IN_PROGRESS",intakeJson(reply).get("state").asString());
            assertEquals(200,supportWrite(mvc,staff,SUPPORT_STAFF+"/cases/"+target+"/transitions",5,Map.of("action","RESOLVE","message","Resolution supplied")).getStatus());
            var review=supportWrite(mvc,owner,SUPPORT_PATH+"/"+target+"/review-requests",6,Map.of("message","Please review the resolution"));assertEquals(200,review.getStatus());assertEquals("RESOLVED",intakeJson(review).get("state").asString());assertEquals(0,intakeJson(review).get("allowedActions").size());
            assertEquals(409,supportWrite(mvc,owner,SUPPORT_PATH+"/"+target+"/acknowledgements",7,null).getStatus());
            assertEquals(200,supportWrite(mvc,staff,SUPPORT_STAFF+"/cases/"+target+"/transitions",7,Map.of("action","REOPEN","message","Review accepted")).getStatus());
            assertEquals(200,supportWrite(mvc,staff,SUPPORT_STAFF+"/cases/"+target+"/transitions",8,Map.of("action","RESOLVE","message","Reviewed resolution")).getStatus());
            var closed=supportWrite(mvc,owner,SUPPORT_PATH+"/"+target+"/acknowledgements",9,null);assertEquals(200,closed.getStatus());assertEquals("CLOSED",intakeJson(closed).get("state").asString());
            assertEquals(10,intakeJson(closed).get("publicMessages").size());assertFalse(closed.getContentAsString().contains("assignee_id"));assertFalse(closed.getContentAsString().contains("operator_principal"));assertTrue(intakeJson(closed).get("responseDueAt").isNull());
            assertEquals(409,supportWrite(mvc,owner,SUPPORT_PATH+"/"+target+"/replies",10,Map.of("message","Cannot reopen closed case")).getStatus());
            assertEquals(10,scalar("SELECT count(*) FROM orders.support_command_receipt"));assertEquals(10,scalar("SELECT count(*) FROM orders.personal_support_message"));assertEquals(0,scalar("SELECT count(*) FROM orders.pilot_tracking_event"));assertEquals(0,scalar("SELECT count(*) FROM finance.pilot_shipping_obligation"));
        }
    }
    @Test void supportTicketSameKeyRaceStaleVersionQuotaAndReceiptAreAtomic()throws Exception{
        customerFixture(true);try(var inbox=new CustomerSmtpTestInbox(0,false,false,250,null);var c=customerHttpContext(PILOT_NOW,smtp(inbox,PILOT_NOW),true)){
            var mvc=httpMvc(c);var owner=profileBrowser(mvc);UUID key=UUID.randomUUID();var statuses=adminRace(()->onboardingWrite(mvc,owner,"POST",SUPPORT_PATH,key,null,supportInput()).getStatus(),()->onboardingWrite(mvc,owner,"POST",SUPPORT_PATH,key,null,supportInput()).getStatus());assertEquals(List.of(201,201),statuses);assertEquals(1,scalar("SELECT count(*) FROM orders.personal_support_ticket"));assertEquals(1,scalar("SELECT sum(count) FROM orders.support_request_window"));
            var receipt=httpRead(mvc,"/api/v1/customer/support-receipts/CREATE/"+key,owner.access());assertEquals(200,receipt.getStatus());String target=responseId(receipt,"targetId");var altered=new HashMap<>(supportInput());altered.put("message","Changed command");assertEquals(409,onboardingWrite(mvc,owner,"POST",SUPPORT_PATH,key,null,altered).getStatus());
            UUID replyKey=UUID.randomUUID();var results=adminRace(()->onboardingWrite(mvc,owner,"POST",SUPPORT_PATH+"/"+target+"/replies",replyKey,"\"v1\"",Map.of("message","Single reply")).getStatus(),()->onboardingWrite(mvc,owner,"POST",SUPPORT_PATH+"/"+target+"/replies",replyKey,"\"v1\"",Map.of("message","Single reply")).getStatus());assertEquals(List.of(200,200),results);assertEquals(2,scalar("SELECT count(*) FROM orders.personal_support_message"));
            assertEquals(409,supportWrite(mvc,owner,SUPPORT_PATH+"/"+target+"/replies",1,Map.of("message","Stale reply")).getStatus());
            for(int i=0;i<4;i++)supportId(onboardingWrite(mvc,owner,"POST",SUPPORT_PATH,UUID.randomUUID(),null,supportInput()));assertEquals(429,onboardingWrite(mvc,owner,"POST",SUPPORT_PATH,UUID.randomUUID(),null,supportInput()).getStatus());assertEquals(5,scalar("SELECT count(*) FROM orders.personal_support_ticket"));
            var staff=addressOtherBrowser(mvc);assertEquals(404,httpRead(mvc,"/api/v1/customer/support-receipts/CREATE/"+key,staff.access()).getStatus());
        }
    }
    @Test void supportTicketStaffRevocationAssignmentAndHttpBoundaryDeny()throws Exception{
        liveStaffClock=true;customerFixture(true);try(var inbox=new CustomerSmtpTestInbox(0,false,false,250,null);var c=customerHttpContext(PILOT_NOW,smtp(inbox,PILOT_NOW),true)){
            var mvc=httpMvc(c);var owner=profileBrowser(mvc);var staff=staffFixtureBrowser(mvc);String target=supportId(onboardingWrite(mvc,owner,"POST",SUPPORT_PATH,UUID.randomUUID(),null,supportInput()));supportGrant(staffFixtureId(),"SUPPORT_TRIAGE");supportGrant(staffFixtureId(),"SUPPORT_HANDLE");
            assertEquals(403,httpRead(mvc,SUPPORT_STAFF+"/cases/"+target,staff.access()).getStatus());
            UUID key=UUID.randomUUID();assertEquals(200,onboardingWrite(mvc,staff,"POST",SUPPORT_STAFF+"/cases/"+target+"/assignments",key,"\"v1\"",Map.of("assigneeAccountId",staffFixtureId())).getStatus());
            assertEquals(401,supportWrite(mvc,owner,SUPPORT_STAFF+"/cases/"+target+"/transitions",2,Map.of("action","START","message","Forged staff" )).getStatus());
            supportOperator(staffFixtureId(),"SUPPORT_HANDLE","REVOKE");assertEquals(403,httpRead(mvc,SUPPORT_STAFF+"/cases/"+target,staff.access()).getStatus());assertEquals(403,onboardingWrite(mvc,staff,"POST",SUPPORT_STAFF+"/cases/"+target+"/assignments",key,"\"v1\"",Map.of("assigneeAccountId",staffFixtureId())).getStatus());
            var bare=org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(SUPPORT_STAFF+"/cases/"+target+"/transitions").secure(true).header("Authorization","Bearer "+staff.access()).contentType("application/json").content("{\"action\":\"START\",\"message\":\"Missing CSRF\"}");assertEquals(401,mvc.perform(bare).andReturn().getResponse().getStatus());
            assertEquals(401,mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(SUPPORT_STAFF+"/queue").secure(true)).andReturn().getResponse().getStatus());
            var over=new HashMap<>(supportInput());over.put("ownerId",B);assertEquals(400,onboardingWrite(mvc,owner,"POST",SUPPORT_PATH,UUID.randomUUID(),null,over).getStatus());
            assertEquals(400,httpRead(mvc,SUPPORT_PATH+"?ownerId="+B,owner.access()).getStatus());assertEquals(428,onboardingWrite(mvc,owner,"POST",SUPPORT_PATH+"/"+target+"/replies",UUID.randomUUID(),null,Map.of("message","No version")).getStatus());assertEquals(2,scalar("SELECT count(*) FROM orders.personal_support_message"));
        }
    }
    @Test void supportTicketMandatoryEvidenceRollbackAndDbPrivileges()throws Exception{
        customerFixture(true);try(var inbox=new CustomerSmtpTestInbox(0,false,false,250,null);var c=customerHttpContext(PILOT_NOW,smtp(inbox,PILOT_NOW),true)){
            var mvc=httpMvc(c);var owner=profileBrowser(mvc);
            for(String table:List.of("orders.personal_support_message","orders.support_business_audit","orders.support_command_receipt")){
                db.ownerExecute("REVOKE INSERT ON "+table+" FROM \""+db.runtime()+"\"");try{assertEquals(503,onboardingWrite(mvc,owner,"POST",SUPPORT_PATH,UUID.randomUUID(),null,supportInput()).getStatus());assertEquals(0,scalar("SELECT count(*) FROM orders.personal_support_ticket"));assertEquals(0,scalar("SELECT count(*) FROM orders.support_request_window"));}finally{db.ownerExecute("GRANT INSERT ON "+table+" TO \""+db.runtime()+"\"");}
            }
            String target=supportId(onboardingWrite(mvc,owner,"POST",SUPPORT_PATH,UUID.randomUUID(),null,supportInput()));assertThrows(SQLException.class,()->execute("DELETE FROM orders.personal_support_ticket"));assertThrows(SQLException.class,()->execute("UPDATE orders.personal_support_message SET message='tampered'"));assertThrows(SQLException.class,()->execute("SELECT orders.configure_local_support_staff('"+A+"','SUPPORT_TRIAGE','GRANT')"));
            assertThrows(SQLException.class,()->execute("UPDATE orders.personal_support_ticket SET version=version+1 WHERE id='"+target+"'"));assertEquals(1,scalar("SELECT version FROM orders.personal_support_ticket WHERE id='"+target+"'"));
        }
    }
    @Test void supportTicketShipmentBindingAndDisabledEnvironmentFailClosed()throws Exception{
        customerFixture(true);try(var inbox=new CustomerSmtpTestInbox(0,false,false,250,null);var c=customerHttpContext(PILOT_NOW,smtp(inbox,PILOT_NOW),true)){
            var mvc=httpMvc(c);var owner=profileBrowser(mvc);var other=addressOtherBrowser(mvc);String shipment=intakeBooked(mvc,owner);var body=new HashMap<>(supportInput());body.put("kind","SHIPMENT");body.put("shipmentId",shipment);body.put("category","SHIPMENT_TRACE");assertEquals(404,onboardingWrite(mvc,other,"POST",SUPPORT_PATH,UUID.randomUUID(),null,body).getStatus());
            String target=supportId(onboardingWrite(mvc,owner,"POST",SUPPORT_PATH,UUID.randomUUID(),null,body));assertTrue(httpRead(mvc,SUPPORT_PATH+"?shipmentId="+shipment,owner.access()).getContentAsString().contains(target));assertEquals(1,scalar("SELECT count(*) FROM finance.pilot_shipping_obligation WHERE state='UNPAID'"));assertEquals(0,scalar("SELECT count(*) FROM orders.pilot_tracking_event"));
        }
        for(String mode:List.of("DISABLED","PRODUCTION","TEST_WITH_PRODUCTION_PROFILE"))try(var inbox=new CustomerSmtpTestInbox(0,false,false,250,null);var c=customerHttpContext(PILOT_NOW,smtp(inbox,PILOT_NOW),true,mode)){
            var mvc=httpMvc(c);var owner=profileBrowser(mvc);assertEquals(503,onboardingWrite(mvc,owner,"POST",SUPPORT_PATH,UUID.randomUUID(),null,supportInput()).getStatus());assertEquals(503,httpRead(mvc,SUPPORT_STAFF+"/queue",owner.access()).getStatus());
        }
    }

    @Test void teamOperatorGrantRemainsIndependentBoundedAuditedAndRuntimeDenied()throws Exception{
        customerFixture(true);UUID org=id(9071);businessShippingOwner(org);long auditBefore=businessScalar("SELECT count(*) FROM merchant.workspace_audit WHERE organization_id='"+org+"' AND operation='GRANT'");
        businessCommand(UUID.randomUUID(),"GRANT",org,A,"OWNER","'dashboard.read','team.read','team.invite','team.access.manage','team.membership.manage','team.audit.read'");
        assertEquals(5,scalar("SELECT count(*) FROM merchant.member_grant WHERE organization_id='"+org+"' AND permission LIKE 'team.%'"));
        assertEquals(auditBefore+1,businessScalar("SELECT count(*) FROM merchant.workspace_audit WHERE organization_id='"+org+"' AND operation='GRANT'"));
        try(var c=db.runtimeConnection();var q=c.createStatement()){
            assertEquals("42501",assertThrows(SQLException.class,()->q.execute(businessSql(UUID.randomUUID(),"GRANT",org,A,"OWNER","'dashboard.read','team.invite'"))).getSQLState());
        }
        var before=scalar("SELECT count(*) FROM merchant.member_grant");
        assertThrows(SQLException.class,()->businessCommand(UUID.randomUUID(),"GRANT",org,A,"OWNER","'dashboard.read','team.platform.admin'"));
        assertEquals(before,scalar("SELECT count(*) FROM merchant.member_grant"));
    }
    @Test void teamMembershipLifecycleReplaysConsumedProofAndPreservesOtherAccess()throws Exception{
        customerFixture(true);addressOtherIdentity();UUID org=id(9073),second=id(9074);businessShippingOwner(org);businessShippingOwner(second);
            businessCommand(UUID.randomUUID(),"GRANT",org,A,"OWNER","'dashboard.read','team.read','team.membership.manage'");
            businessCommand(UUID.randomUUID(),"GRANT",org,B,"OPERATOR","'dashboard.read','support.read','support.write'");businessCommand(UUID.randomUUID(),"GRANT",second,B,"OPERATOR","'dashboard.read'");
        // Operator timestamps use CURRENT_TIMESTAMP; business API clock must be
        // later than the already-provisioned fixture, never an older pilot date.
        Instant teamNow=Instant.now().plusSeconds(1).truncatedTo(java.time.temporal.ChronoUnit.MICROS);
        try(var inbox=new CustomerSmtpTestInbox(0,false,false,250,null);var c=customerHttpContext(teamNow,smtp(inbox,teamNow),true,"TEST",true)){
            var mvc=httpMvc(c);var owner=profileBrowser(mvc);var other=addressOtherLogin(mvc);
            var jdbc=new org.springframework.jdbc.core.JdbcTemplate(outboxDataSource());UUID target=jdbc.queryForObject("SELECT id FROM merchant.membership WHERE organization_id=? AND account_id=?",UUID.class,org,B);
            var proof=onboardingWrite(mvc,owner,"POST","/api/v1/customer-auth/reauthentication",UUID.randomUUID(),null,Map.of("email",CUSTOMER_EMAIL,"password",SESSION_PASSWORD));assertEquals(200,proof.getStatus(),this::safeOnboardingFailure);
            String proofId=intakeJson(proof).get("proofId").asString();var input=Map.of("reason","Synthetic offboarding check","reauthenticationProofId",proofId);UUID key=UUID.randomUUID();String base=businessShippingBase(org)+"/team",path=base+"/members/"+target;
            var response=onboardingWrite(mvc,owner,"POST",path+"/suspend",key,"\"v1\"",input);assertEquals(200,response.getStatus(),this::safeOnboardingFailure);assertEquals("SUSPENDED",intakeJson(response).get("state").asString());
            assertEquals(200,onboardingWrite(mvc,owner,"POST",path+"/suspend",key,"\"v1\"",input).getStatus());assertEquals(1,scalar("SELECT count(*) FROM merchant.team_audit WHERE target_membership_id='"+target+"'"));
            assertEquals(409,onboardingWrite(mvc,owner,"POST",path+"/suspend",key,"\"v1\"",Map.of("reason","Changed payload","reauthenticationProofId",proofId)).getStatus());
            assertEquals(422,onboardingWrite(mvc,owner,"POST",path+"/resume",UUID.randomUUID(),"\"v2\"",input).getStatus());
            assertEquals(200,httpRead(mvc,"/api/v1/customer-auth/me",other.access()).getStatus());String contexts=httpRead(mvc,BUSINESS_CONTEXTS,other.access()).getContentAsString();assertTrue(contexts.contains(second.toString()));assertFalse(contexts.contains(org.toString()));
            var resume=Map.of("reason","Synthetic resume","reauthenticationProofId",recentProof(mvc,owner));
            assertEquals(200,onboardingWrite(mvc,owner,"POST",path+"/resume",UUID.randomUUID(),"\"v2\"",resume).getStatus());
            var revoke=Map.of("reason","Synthetic permanent offboarding","reauthenticationProofId",recentProof(mvc,owner));UUID revokeKey=UUID.randomUUID();
            assertEquals(200,onboardingWrite(mvc,owner,"POST",path+"/revoke",revokeKey,"\"v3\"",revoke).getStatus());
            assertEquals(200,onboardingWrite(mvc,owner,"POST",path+"/revoke",revokeKey,"\"v3\"",revoke).getStatus());
            assertEquals(409,onboardingWrite(mvc,owner,"POST",path+"/resume",UUID.randomUUID(),"\"v4\"",revoke).getStatus());
            assertEquals(0,scalar("SELECT count(*) FROM merchant.member_grant WHERE membership_id='"+target+"'"));
            assertEquals(3,scalar("SELECT count(*) FROM merchant.team_audit WHERE target_membership_id='"+target+"'"));
            assertEquals(200,httpRead(mvc,"/api/v1/customer-auth/me",other.access()).getStatus());assertTrue(httpRead(mvc,BUSINESS_CONTEXTS,other.access()).getContentAsString().contains(second.toString()));
            UUID initial=jdbc.queryForObject("SELECT initial_owner_membership_id FROM merchant.organization WHERE id=?",UUID.class,org);
            assertEquals(409,onboardingWrite(mvc,owner,"POST",base+"/members/"+initial+"/suspend",UUID.randomUUID(),"\"v1\"",input).getStatus());
        }
    }
    private Map<String,Object> teamInvitationBody(){var grant=new HashMap<String,Object>();grant.put("permission","dashboard.read");grant.put("scope","ORGANIZATION");grant.put("facilityId",null);return Map.of("email","address-other@example.invalid","role","OPERATOR","grants",List.of(grant),"reason","Synthetic team invitation");}
    private String teamMailProof(byte[] raw)throws Exception{
        var mail=new jakarta.mail.internet.MimeMessage(jakarta.mail.Session.getInstance(new Properties()),new java.io.ByteArrayInputStream(raw));
        assertEquals("ArgoPost - Lời mời tham gia doanh nghiệp",mail.getSubject());String text=CustomerSmtpTestInbox.plainText(mail);
        var match=java.util.regex.Pattern.compile("#proof=([A-Za-z0-9_-]{43})").matcher(text);assertTrue(match.find());assertTrue(text.contains("72 giờ"));return match.group(1);
    }
    @Test void teamInvitationRealOutboxResendAndAcceptanceReplaysWithoutDuplicateMembership()throws Exception{
        customerFixture(true);try(var inbox=new CustomerSmtpTestInbox(2,false,false,250,null);var c=customerHttpContext(PILOT_NOW,smtp(inbox,PILOT_NOW),true,"TEST",true)){
            var mvc=httpMvc(c);var owner=profileBrowser(mvc);var recipient=addressOtherBrowser(mvc);UUID org=id(9075);businessShippingOwner(org);
            businessCommand(UUID.randomUUID(),"GRANT",org,A,"OWNER","'dashboard.read','team.invite','team.read'");String base=businessShippingBase(org)+"/team";
            UUID issueKey=UUID.randomUUID();var input=teamInvitationBody();var first=onboardingWrite(mvc,owner,"POST",base+"/invitations",issueKey,null,input);assertEquals(201,first.getStatus(),this::safeOnboardingFailure);
            String invitation=intakeJson(first).get("id").asString();assertFalse(first.getContentAsString().contains("address-other@example.invalid"));
            assertEquals(201,onboardingWrite(mvc,owner,"POST",base+"/invitations",issueKey,null,input).getStatus());assertEquals(1,scalar("SELECT count(*) FROM identity.customer_delivery WHERE purpose='MERCHANT_MEMBERSHIP'"));
            var worker=c.getBean(com.elogistic.backend.identity.internal.ProductionCustomerComposition.Services.class).delivery();assertTrue(worker.runOne());
            UUID resendKey=UUID.randomUUID();var reason=Map.of("reason","Synthetic resend");String resource=base+"/invitations/"+invitation;
            assertEquals(200,onboardingWrite(mvc,owner,"POST",resource+"/resend",resendKey,"\"v1\"",reason).getStatus());assertEquals(200,onboardingWrite(mvc,owner,"POST",resource+"/resend",resendKey,"\"v1\"",reason).getStatus());
            assertTrue(worker.runOne());var messages=inbox.messages();assertEquals(2,messages.size());String oldProof=teamMailProof(messages.get(0)),proof=teamMailProof(messages.get(1));
            String accept="/api/v1/customer/organization-invitations/"+invitation+"/accept";
            assertEquals(422,onboardingWrite(mvc,recipient,"POST",accept,UUID.randomUUID(),null,Map.of("proof",oldProof)).getStatus());
            assertEquals(422,onboardingWrite(mvc,owner,"POST",accept,UUID.randomUUID(),null,Map.of("proof",proof)).getStatus());
            UUID acceptKey=UUID.randomUUID();var outcomes=adminRace(()->onboardingWrite(mvc,recipient,"POST",accept,acceptKey,null,Map.of("proof",proof)).getStatus(),()->onboardingWrite(mvc,recipient,"POST",accept,acceptKey,null,Map.of("proof",proof)).getStatus());assertEquals(List.of(200,200),outcomes);
            var accepted=onboardingWrite(mvc,recipient,"POST",accept,acceptKey,null,Map.of("proof",proof));assertEquals(200,accepted.getStatus(),this::safeOnboardingFailure);
            assertEquals("ACCEPTED",intakeJson(accepted).get("outcome").asString());assertEquals(accepted.getContentAsString(),onboardingWrite(mvc,recipient,"POST",accept,acceptKey,null,Map.of("proof",proof)).getContentAsString());
            assertEquals(422,onboardingWrite(mvc,recipient,"POST",accept,UUID.randomUUID(),null,Map.of("proof",proof)).getStatus());
            assertEquals(1,scalar("SELECT count(*) FROM merchant.membership WHERE organization_id='"+org+"' AND account_id='"+B+"'"));
            assertEquals(1,scalar("SELECT count(*) FROM merchant.team_audit WHERE organization_id='"+org+"' AND operation='ACCEPT'"));
            assertEquals(0,scalar("SELECT count(*) FROM merchant.member_grant g JOIN merchant.membership m ON m.id=g.membership_id WHERE m.account_id='"+B+"' AND g.permission LIKE 'team.%'"));
            assertTrue(httpRead(mvc,BUSINESS_CONTEXTS,recipient.access()).getContentAsString().contains(org.toString()));
        }
    }
    @Test void teamOfflineAuthorityChangeInvalidatesInvitationAndProtectedDeliveryAtomically()throws Exception{
        customerFixture(true);try(var inbox=new CustomerSmtpTestInbox(0,false,false,250,null);var c=customerHttpContext(PILOT_NOW,smtp(inbox,PILOT_NOW),true,"TEST",true)){
            var mvc=httpMvc(c);var owner=profileBrowser(mvc);UUID org=id(9076);businessShippingOwner(org);businessCommand(UUID.randomUUID(),"GRANT",org,A,"OWNER","'dashboard.read','team.invite'");
            var response=onboardingWrite(mvc,owner,"POST",businessShippingBase(org)+"/team/invitations",UUID.randomUUID(),null,teamInvitationBody());assertEquals(201,response.getStatus(),this::safeOnboardingFailure);
            businessCommand(UUID.randomUUID(),"REVOKE",org,A,"OWNER","'team.invite'");
            assertEquals(1,scalar("SELECT count(*) FROM merchant.team_invitation WHERE organization_id='"+org+"' AND state='INVALIDATED'"));
            assertEquals(1,scalar("SELECT count(*) FROM identity.customer_proof WHERE purpose='MERCHANT_MEMBERSHIP' AND revoked_at IS NOT NULL"));
            assertEquals(1,scalar("SELECT count(*) FROM identity.customer_delivery WHERE purpose='MERCHANT_MEMBERSHIP' AND status='EXPIRED' AND ciphertext IS NULL AND recipient_ciphertext IS NULL"));
            assertFalse(c.getBean(com.elogistic.backend.identity.internal.ProductionCustomerComposition.Services.class).delivery().runOne());
        }
    }
    @Test void teamRuntimeDirectDmlCannotPromoteGrantForgeHistoryOrRemoveOwner()throws Exception{
        customerFixture(true);addressOtherIdentity();UUID org=id(9077);businessShippingOwner(org);
        businessCommand(UUID.randomUUID(),"GRANT",org,A,"OWNER","'dashboard.read','team.read','team.access.manage','team.membership.manage'");
        businessCommand(UUID.randomUUID(),"GRANT",org,B,"OPERATOR","'dashboard.read'");
        var jdbc=new org.springframework.jdbc.core.JdbcTemplate(outboxDataSource());UUID member=jdbc.queryForObject("SELECT id FROM merchant.membership WHERE organization_id=? AND account_id=?",UUID.class,org,B);
        for(String sql:List.of("UPDATE merchant.membership SET role='MANAGER',version=version+1,authority_version=authority_version+1,updated_at=clock_timestamp() WHERE id='"+member+"'",
          "DELETE FROM merchant.member_grant WHERE membership_id='"+member+"'",
          "INSERT INTO merchant.membership VALUES(gen_random_uuid(),'"+org+"','"+T+"','OPERATOR','ACTIVE',1,1,clock_timestamp(),clock_timestamp())",
          "INSERT INTO merchant.team_command_receipt VALUES('"+org+"','"+A+"','ACCESS',gen_random_uuid(),decode(repeat('01',32),'hex'),'"+member+"',NULL,'COMMITTED',2,clock_timestamp())")){
            assertEquals("23514",assertThrows(SQLException.class,()->execute(sql)).getSQLState());
        }
        for(String sql:List.of("UPDATE merchant.organization SET state='SUSPENDED' WHERE id='"+org+"'","UPDATE merchant.team_policy SET version=version+1 WHERE organization_id='"+org+"'",
          "DELETE FROM merchant.membership WHERE organization_id='"+org+"'","UPDATE merchant.membership SET account_id='"+T+"' WHERE id='"+member+"'",
          "INSERT INTO merchant.member_grant VALUES(gen_random_uuid(),'"+member+"','"+org+"','team.invite','ORGANIZATION',NULL)")){
            assertEquals("42501",assertThrows(SQLException.class,()->execute(sql)).getSQLState());
        }
        assertEquals(0,scalar("SELECT count(*) FROM merchant.team_command_receipt"));assertEquals(0,scalar("SELECT count(*) FROM merchant.team_audit"));
        assertEquals(1,scalar("SELECT count(*) FROM merchant.membership WHERE id='"+member+"' AND role='OPERATOR' AND state='ACTIVE' AND version=1"));
        assertEquals(1,scalar("SELECT count(*) FROM merchant.member_grant WHERE membership_id='"+member+"'"));
    }
    @Test void teamDeclineAndLateAuditFailurePreserveMembershipAndOutboxAtomicity()throws Exception{
        // The inbox accepts only after the rollback injection and fixture setup.
        customerFixture(true);try(var inbox=new CustomerSmtpTestInbox(1,false,false,250,null,30000);var c=customerHttpContext(PILOT_NOW,smtp(inbox,PILOT_NOW),true,"TEST",true)){
            var mvc=httpMvc(c);var owner=profileBrowser(mvc);var recipient=addressOtherBrowser(mvc);UUID org=id(9078);businessShippingOwner(org);businessCommand(UUID.randomUUID(),"GRANT",org,A,"OWNER","'dashboard.read','team.invite'");String base=businessShippingBase(org)+"/team/invitations";
            db.ownerExecute("CREATE FUNCTION merchant.synthetic_team_audit_fault() RETURNS trigger LANGUAGE plpgsql AS $$ BEGIN RAISE EXCEPTION 'SYNTHETIC_TEAM_AUDIT_FAULT'; END $$; CREATE TRIGGER synthetic_team_audit_fault BEFORE INSERT ON merchant.team_audit FOR EACH ROW EXECUTE FUNCTION merchant.synthetic_team_audit_fault()");
            try{assertEquals(503,onboardingWrite(mvc,owner,"POST",base,UUID.randomUUID(),null,teamInvitationBody()).getStatus());assertEquals(0,scalar("SELECT count(*) FROM merchant.team_invitation"));assertEquals(0,scalar("SELECT count(*) FROM identity.customer_proof WHERE purpose='MERCHANT_MEMBERSHIP'"));assertEquals(0,scalar("SELECT count(*) FROM identity.customer_delivery WHERE purpose='MERCHANT_MEMBERSHIP'"));}
            finally{db.ownerExecute("DROP TRIGGER synthetic_team_audit_fault ON merchant.team_audit; DROP FUNCTION merchant.synthetic_team_audit_fault()");}
            var response=onboardingWrite(mvc,owner,"POST",base,UUID.randomUUID(),null,teamInvitationBody());assertEquals(201,response.getStatus(),this::safeOnboardingFailure);String invitation=intakeJson(response).get("id").asString();
            assertTrue(c.getBean(com.elogistic.backend.identity.internal.ProductionCustomerComposition.Services.class).delivery().runOne());String proof=teamMailProof(inbox.messages().getFirst());UUID key=UUID.randomUUID();String decline="/api/v1/customer/organization-invitations/"+invitation+"/decline";
            var result=onboardingWrite(mvc,recipient,"POST",decline,key,null,Map.of("proof",proof));assertEquals(200,result.getStatus(),this::safeOnboardingFailure);assertTrue(intakeJson(result).get("membershipId").isNull());
            assertEquals(result.getContentAsString(),onboardingWrite(mvc,recipient,"POST",decline,key,null,Map.of("proof",proof)).getContentAsString());
            assertEquals(409,onboardingWrite(mvc,recipient,"POST",decline,key,null,Map.of("proof","A".repeat(43))).getStatus());
            assertEquals(422,onboardingWrite(mvc,recipient,"POST",decline.replace("/decline","/accept"),UUID.randomUUID(),null,Map.of("proof",proof)).getStatus());
            assertEquals(0,scalar("SELECT count(*) FROM merchant.membership WHERE organization_id='"+org+"' AND account_id='"+B+"'"));assertEquals(200,httpRead(mvc,"/api/v1/customer-auth/me",recipient.access()).getStatus());
        }
    }
    @Test void teamManagerDelegationCannotPromoteFinanceOrEscapeWarehouseAndProductionIsDenied()throws Exception{
        customerFixture(true);try(var inbox=new CustomerSmtpTestInbox(0,false,false,250,null);var c=customerHttpContext(PILOT_NOW,smtp(inbox,PILOT_NOW),true,"TEST",true)){
            var mvc=httpMvc(c);var owner=profileBrowser(mvc);var manager=addressOtherBrowser(mvc);UUID org=id(9079),foreign=id(9080),site=id(9081),outside=id(9082);businessShippingOwner(org);businessShippingOwner(foreign);
            seedFacility(site,org,new tools.jackson.databind.ObjectMapper().writeValueAsString(shippingDraftInput("Team site").get("sender")).replace("0901234567","+84901234567"));seedFacility(outside,org,new tools.jackson.databind.ObjectMapper().writeValueAsString(shippingDraftInput("Other site").get("sender")).replace("0901234567","+84901234567"));
            businessCommand(UUID.randomUUID(),"GRANT",org,B,"MANAGER","'dashboard.read','team.invite','team.read'");facilityGrant(org,B,site,"'facility.read','shipment.draft.read'");String path=businessShippingBase(org)+"/team/invitations";
            var body=new HashMap<>(teamInvitationBody());body.put("email","third@example.invalid");body.put("role","MANAGER");assertEquals(403,onboardingWrite(mvc,manager,"POST",path,UUID.randomUUID(),null,body).getStatus());body.put("role","OPERATOR");
            var base=new HashMap<String,Object>();base.put("permission","dashboard.read");base.put("scope","ORGANIZATION");base.put("facilityId",null);
            var finance=new HashMap<>(base);finance.put("permission","shipment.finance.read");finance.put("scope","FACILITY");finance.put("facilityId",site.toString());body.put("grants",List.of(base,Map.of("permission","shipment.draft.read","scope","FACILITY","facilityId",site.toString()),finance));assertEquals(403,onboardingWrite(mvc,manager,"POST",path,UUID.randomUUID(),null,body).getStatus());
            body.put("grants",List.of(base,Map.of("permission","shipment.draft.read","scope","FACILITY","facilityId",outside.toString())));assertEquals(403,onboardingWrite(mvc,manager,"POST",path,UUID.randomUUID(),null,body).getStatus());
            body.put("grants",List.of(base,Map.of("permission","shipment.draft.read","scope","FACILITY","facilityId",site.toString())));assertEquals(201,onboardingWrite(mvc,manager,"POST",path,UUID.randomUUID(),null,body).getStatus());
            assertEquals(404,onboardingWrite(mvc,manager,"POST",businessShippingBase(foreign)+"/team/invitations",UUID.randomUUID(),null,body).getStatus());
            assertEquals(1,scalar("SELECT count(*) FROM merchant.team_invitation"));
        }
        for(String mode:List.of("DISABLED","PRODUCTION","TEST_WITH_PRODUCTION_PROFILE"))try(var inbox=new CustomerSmtpTestInbox(0,false,false,250,null);var c=customerHttpContext(PILOT_NOW,smtp(inbox,PILOT_NOW),true,mode,true)){
            var mvc=httpMvc(c);var owner=profileBrowser(mvc);assertEquals(503,onboardingWrite(mvc,owner,"POST",businessShippingBase(id(9079))+"/team/invitations",UUID.randomUUID(),null,teamInvitationBody()).getStatus());
        }
    }
    @Test void teamExactInvitationExpiryAndPolicyChangeCannotLeaveUsableProtectedProof()throws Exception{
        customerFixture(true);UUID org=id(9083);String invitation,proof;String path=businessShippingBase(org)+"/team/invitations";
        try(var inbox=new CustomerSmtpTestInbox(1,false,false,250,null);var c=customerHttpContext(PILOT_NOW,smtp(inbox,PILOT_NOW),true,"TEST",true)){
            var mvc=httpMvc(c);var owner=profileBrowser(mvc);addressOtherBrowser(mvc);businessShippingOwner(org);businessCommand(UUID.randomUUID(),"GRANT",org,A,"OWNER","'dashboard.read','team.invite'");
            var response=onboardingWrite(mvc,owner,"POST",path,UUID.randomUUID(),null,teamInvitationBody());assertEquals(201,response.getStatus(),this::safeOnboardingFailure);invitation=intakeJson(response).get("id").asString();
            assertTrue(c.getBean(com.elogistic.backend.identity.internal.ProductionCustomerComposition.Services.class).delivery().runOne());proof=teamMailProof(inbox.messages().getFirst());
        }
        Instant expires=PILOT_NOW.plusSeconds(259200);
        try(var inbox=new CustomerSmtpTestInbox(0,false,false,250,null);var c=customerHttpContext(expires,smtp(inbox,expires),true,"TEST",true)){
            var mvc=httpMvc(c);var owner=profileBrowser(mvc);var recipient=addressOtherLogin(mvc);
            assertEquals(422,onboardingWrite(mvc,recipient,"POST","/api/v1/customer/organization-invitations/"+invitation+"/accept",UUID.randomUUID(),null,Map.of("proof",proof)).getStatus());
            var replacement=onboardingWrite(mvc,owner,"POST",path,UUID.randomUUID(),null,teamInvitationBody());assertEquals(201,replacement.getStatus(),this::safeOnboardingFailure);
            assertEquals(1,scalar("SELECT count(*) FROM merchant.team_invitation WHERE state='EXPIRED'"));
            db.ownerExecute("UPDATE merchant.team_policy SET version=version+1,updated_at=GREATEST(clock_timestamp(),updated_at) WHERE organization_id='"+org+"'");
            assertEquals(1,scalar("SELECT count(*) FROM merchant.team_invitation WHERE state='INVALIDATED'"));
            assertEquals(2,scalar("SELECT count(*) FROM identity.customer_proof WHERE purpose='MERCHANT_MEMBERSHIP' AND revoked_at IS NOT NULL"));
            assertEquals(2,scalar("SELECT count(*) FROM identity.customer_delivery WHERE purpose='MERCHANT_MEMBERSHIP' AND status='EXPIRED' AND ciphertext IS NULL"));
            assertEquals(0,scalar("SELECT count(*) FROM merchant.membership WHERE organization_id='"+org+"' AND account_id='"+B+"'"));
        }
    }
    @Test void teamDirectoryUsesCurrentOrganizationPermissionAndDoesNotExposeCredentials()throws Exception{
        customerFixture(true);try(var inbox=new CustomerSmtpTestInbox(0,false,false,250,null);var c=customerHttpContext(PILOT_NOW,smtp(inbox,PILOT_NOW),true,"TEST",true)){
            var mvc=httpMvc(c);var owner=profileBrowser(mvc);var other=addressOtherBrowser(mvc);UUID org=id(9072);businessShippingOwner(org);
            businessCommand(UUID.randomUUID(),"GRANT",org,A,"OWNER","'dashboard.read','team.read'");
            var query=new HashMap<String,Object>();query.put("search","");query.put("role",null);query.put("state",null);query.put("facilityId",null);query.put("page",0);query.put("size",20);
            String path=businessShippingBase(org)+"/team";var response=onboardingWrite(mvc,owner,"POST",path+"/members/search",UUID.randomUUID(),null,query);
            assertEquals(200,response.getStatus(),this::safeOnboardingFailure);var page=intakeJson(response);assertEquals(1,page.get("items").size());
            var first=page.get("items").get(0);assertEquals("OWNER",first.get("role").asString());assertFalse(first.has("accountId"));assertFalse(first.has("email"));assertFalse(first.has("phone"));
            String membership=first.get("membershipId").asString();assertEquals(200,httpRead(mvc,path+"/members/"+membership,owner.access()).getStatus());
            assertEquals(3,intakeJson(httpRead(mvc,path+"/role-templates",owner.access())).get("items").size());
            assertEquals(404,onboardingWrite(mvc,other,"POST",path+"/members/search",UUID.randomUUID(),null,query).getStatus());
            businessCommand(UUID.randomUUID(),"REVOKE",org,A,"OWNER","'team.read'");assertEquals(403,onboardingWrite(mvc,owner,"POST",path+"/members/search",UUID.randomUUID(),null,query).getStatus());
            assertEquals(200,httpRead(mvc,"/api/v1/customer-auth/me",owner.access()).getStatus());
        }
    }

    private static final String ORGANIZATION_SUPPORT_RIGHTS="'dashboard.read','support.read','support.write','support.assign','support.reports.read','support.notifications.manage'";
    private Map<String,Object> organizationSupportInput(){var input=new HashMap<>(supportInput());input.put("requestedPriority","HIGH");return input;}
    private void organizationSupportOwner(UUID org)throws Exception{businessShippingOwner(org);businessCommand(UUID.randomUUID(),"GRANT",org,A,"OWNER",ORGANIZATION_SUPPORT_RIGHTS);}
    private void organizationSupportStaff(UUID org)throws Exception{supportGrant(staffFixtureId(),"SUPPORT_TRIAGE");supportGrant(staffFixtureId(),"SUPPORT_HANDLE");for(String permission:List.of("SUPPORT_TRIAGE","SUPPORT_HANDLE"))db.ownerExecute("BEGIN; SET LOCAL argopost.operator_environment='TEST';SELECT * FROM orders.configure_local_organization_support_staff('"+UUID.randomUUID()+"','BIND','"+org+"','"+staffFixtureId()+"','"+permission+"','Synthetic support binding');COMMIT;");}
    private org.springframework.mock.web.MockHttpServletResponse organizationSupportWrite(org.springframework.test.web.servlet.MockMvc mvc,ProfileBrowser browser,String path,String prefix,long version,Object body,UUID key)throws Exception{return onboardingWrite(mvc,browser,prefix.equals("n")||prefix.equals("a")?"PUT":"POST",path,key,"\""+prefix+version+"\"",body);}
    @Test void organizationSupportConversationSeparatesMerchantAndCarrierAndPreservesHistory()throws Exception{
        liveStaffClock=true;customerFixture(true);UUID org=id(9202);organizationSupportOwner(org);
        try(var inbox=new CustomerSmtpTestInbox(0,false,false,250,null);var c=customerHttpContext(PILOT_NOW,smtp(inbox,PILOT_NOW),true,"TEST",false,true)){
            var mvc=httpMvc(c);var owner=profileBrowser(mvc);var staff=staffFixtureBrowser(mvc);organizationSupportStaff(org);String base=businessShippingBase(org)+"/support-tickets";
            String target=supportId(onboardingWrite(mvc,owner,"POST",base,UUID.randomUUID(),null,organizationSupportInput()));
            assertEquals(401,httpRead(mvc,SUPPORT_STAFF+"/organization-cases/"+target,owner.access()).getStatus());
            var queue=httpRead(mvc,SUPPORT_STAFF+"/organization-queue?organizationId="+org,staff.access());assertEquals(200,queue.getStatus());assertTrue(queue.getContentAsString().contains(target));assertFalse(queue.getContentAsString().contains("Synthetic initial conversation"));
            String path=SUPPORT_STAFF+"/organization-cases/"+target;
            assertEquals(200,supportWrite(mvc,staff,path+"/assignments",1,Map.of("assigneeAccountId",staffFixtureId())).getStatus());
            String report=businessShippingBase(org)+"/support-reports?from="+java.time.LocalDate.now(java.time.ZoneId.of("Asia/Ho_Chi_Minh"))+"&until="+java.time.LocalDate.now(java.time.ZoneId.of("Asia/Ho_Chi_Minh")).plusDays(1);
            var before=intakeJson(httpRead(mvc,report,owner.access()));assertEquals("0",before.get("firstResponse").get("sampleCount").asString());
            assertEquals(200,supportWrite(mvc,staff,path+"/transitions",2,Map.of("action","START","message","Investigation started")).getStatus());
            assertEquals(200,supportWrite(mvc,staff,path+"/replies",3,Map.of("message","Substantive support reply")).getStatus());
            assertEquals(200,supportWrite(mvc,staff,path+"/transitions",4,Map.of("action","REQUEST_INFORMATION","message","Clarify the request")).getStatus());
            assertEquals(200,supportWrite(mvc,owner,base+"/"+target+"/replies",5,Map.of("message","Merchant clarification")).getStatus());
            assertEquals(200,supportWrite(mvc,staff,path+"/transitions",6,Map.of("action","RESOLVE","message","Resolution supplied")).getStatus());
            assertEquals(200,supportWrite(mvc,owner,base+"/"+target+"/review-requests",7,Map.of("message","Review requested")).getStatus());
            assertEquals(409,supportWrite(mvc,owner,base+"/"+target+"/acknowledgements",8,Map.of()).getStatus());
            assertEquals(200,supportWrite(mvc,staff,path+"/transitions",8,Map.of("action","REOPEN","message","Review accepted")).getStatus());
            assertEquals(200,supportWrite(mvc,staff,path+"/transitions",9,Map.of("action","RESOLVE","message","Final resolution")).getStatus());
            var closed=supportWrite(mvc,owner,base+"/"+target+"/acknowledgements",10,Map.of());assertEquals(200,closed.getStatus());assertEquals("CLOSED",intakeJson(closed).get("state").asString());
            var after=intakeJson(httpRead(mvc,report,owner.access()));assertEquals("1",after.get("totalTickets").asString());assertEquals("1",after.get("firstResponse").get("sampleCount").asString());assertEquals("1",after.get("firstResolution").get("sampleCount").asString());assertEquals("UNPUBLISHED",after.get("slaState").asString());
            assertEquals(11,scalar("SELECT count(*) FROM orders.organization_support_message"));assertEquals(11,scalar("SELECT count(*) FROM orders.organization_support_receipt"));
            assertEquals(0,scalar("SELECT count(*) FROM orders.personal_support_ticket"));assertEquals(0,scalar("SELECT count(*) FROM orders.pilot_tracking_event"));assertEquals(0,scalar("SELECT count(*) FROM finance.pilot_shipping_obligation"));
        }
    }
    private String safeClaimFailure(){for(Throwable cause=onboardingFailure.get();cause!=null;cause=cause.getCause())if(cause instanceof org.postgresql.util.PSQLException pg&&"42703".equals(pg.getSQLState())){var e=pg.getServerErrorMessage();if(e!=null&&e.getMessage().matches("(record|column) [A-Za-z0-9_\\\". ]+ (has no field|does not exist)[A-Za-z0-9_\\\". ]*"))return e.getMessage();}return safeOnboardingFailure();}
    @Test void claimIntakeHttpPersistsBoundHistoryAndNeverDecidesCompensation()throws Exception{
        claimIntakeEnabled=true;liveStaffClock=true;customerFixture(true);staffFixtureId();UUID org=id(9701);organizationSupportOwner(org);businessTestContract(org);
        try(var inbox=new CustomerSmtpTestInbox(0,false,false,250,null);var c=customerHttpContext(PILOT_NOW,smtp(inbox,PILOT_NOW),true,"TEST",false,true)){
            var mvc=httpMvc(c);var owner=profileBrowser(mvc);var other=addressOtherBrowser(mvc);String shipment=businessManagementBooking(mvc,owner,org),root=businessShippingBase(org);
            var input=new HashMap<>(organizationSupportInput());input.put("kind","SHIPMENT");input.put("shipmentId",shipment);String ticket=supportId(onboardingWrite(mvc,owner,"POST",root+"/support-tickets",UUID.randomUUID(),null,input));
            String base=root+"/claim-intakes";UUID key=UUID.randomUUID();var body=Map.of("ticketId",ticket,"category","DAMAGE","narrative","Synthetic packaging damage, not a verified Carrier fact.");
            var created=onboardingWrite(mvc,owner,"POST",base,key,null,body);assertEquals(201,created.getStatus(),this::safeClaimFailure);var result=intakeJson(created).get("intake");String target=result.get("id").asString(),path=base+"/"+target;
            assertEquals("UNASSESSED",result.get("eligibility").asString());assertEquals("BLOCKED",result.get("compensation").asString());assertEquals(shipment,result.get("shipmentId").asString());
            assertEquals(201,onboardingWrite(mvc,owner,"POST",base,key,null,body).getStatus());assertEquals(1,scalar("SELECT count(*) FROM orders.organization_claim_intake_event"));
            assertEquals(409,onboardingWrite(mvc,owner,"POST",base,key,null,Map.of("ticketId",ticket,"category","LOSS","narrative","Changed request")).getStatus());
            assertEquals(409,onboardingWrite(mvc,owner,"POST",base,UUID.randomUUID(),null,body).getStatus());assertEquals(404,httpRead(mvc,path,other.access()).getStatus());
            var staff=staffFixtureBrowser(mvc);organizationSupportStaff(org);String staffPath=SUPPORT_STAFF+"/organization-claim-intakes/"+target;
            assertEquals(403,httpRead(mvc,staffPath,staff.access()).getStatus());assertEquals(200,supportWrite(mvc,staff,SUPPORT_STAFF+"/organization-cases/"+ticket+"/assignments",1,Map.of("assigneeAccountId",staffFixtureId())).getStatus());
            assertEquals(200,supportWrite(mvc,staff,staffPath+"/commands",1,Map.of("action","REQUEST_INFORMATION","message","Please supplement the record")).getStatus());
            UUID reply=UUID.randomUUID();var supplement=Map.of("narrative","Synthetic additional description");assertEquals(200,onboardingWrite(mvc,owner,"POST",path+"/supplements",reply,"\"v2\"",supplement).getStatus());
            assertEquals(200,onboardingWrite(mvc,owner,"POST",path+"/supplements",reply,"\"v2\"",supplement).getStatus());assertEquals(409,supportWrite(mvc,owner,path+"/supplements",2,supplement).getStatus());
            assertEquals(400,supportWrite(mvc,staff,staffPath+"/commands",3,Map.of("action","APPROVE","message","Forbidden financial decision")).getStatus());
            assertEquals(200,supportWrite(mvc,staff,staffPath+"/commands",3,Map.of("action","CLOSE_WITHOUT_DECISION","message","Document review closed without award")).getStatus());
            assertEquals(409,supportWrite(mvc,owner,path+"/supplements",4,supplement).getStatus());assertEquals(4,scalar("SELECT count(*) FROM orders.organization_claim_intake_event"));assertEquals(4,scalar("SELECT count(*) FROM orders.organization_claim_intake_receipt"));
            assertEquals(1,scalar("SELECT count(*) FROM finance.pilot_shipping_obligation WHERE state='UNPAID'"));assertEquals(0,scalar("SELECT count(*) FROM orders.organization_support_notice"));
            assertThrows(SQLException.class,()->execute("UPDATE orders.organization_claim_intake SET state='UNDER_REVIEW',version=version+1"));assertThrows(SQLException.class,()->execute("DELETE FROM orders.organization_claim_intake_receipt"));
        }
    }

    @Test void claimEvidenceRequiresPersistedReservationScreeningAndCurrentAuthority()throws Exception{
        claimEvidenceEnabled=true;claimIntakeEnabled=true;liveStaffClock=true;customerFixture(true);staffFixtureId();UUID org=id(9706);organizationSupportOwner(org);businessTestContract(org);
        try(var inbox=new CustomerSmtpTestInbox(0,false,false,250,null);var c=customerHttpContext(PILOT_NOW,smtp(inbox,PILOT_NOW),true,"TEST",false,true)){
            var mvc=httpMvc(c);var owner=profileBrowser(mvc);String root=businessShippingBase(org),shipment=businessManagementBooking(mvc,owner,org);var input=new HashMap<>(organizationSupportInput());input.put("kind","SHIPMENT");input.put("shipmentId",shipment);String ticket=supportId(onboardingWrite(mvc,owner,"POST",root+"/support-tickets",UUID.randomUUID(),null,input));var created=onboardingWrite(mvc,owner,"POST",root+"/claim-intakes",UUID.randomUUID(),null,Map.of("ticketId",ticket,"category","DAMAGE","narrative","Synthetic attachment case"));assertEquals(201,created.getStatus());String target=intakeJson(created).get("intake").get("id").asString(),path=root+"/claim-intakes/"+target;
            byte[] bytes="Synthetic source test attachment".getBytes(java.nio.charset.StandardCharsets.US_ASCII);String sha=java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(bytes));UUID key=UUID.randomUUID();var body=Map.of("mime","image/png","bytes",bytes.length,"sha256",sha);var reservation=onboardingWrite(mvc,owner,"POST",path+"/evidence-reservations",key,"\"v1\"",body);assertEquals(201,reservation.getStatus(),this::safeOnboardingFailure);String evidence=responseId(reservation,"id");assertEquals(201,onboardingWrite(mvc,owner,"POST",path+"/evidence-reservations",key,"\"v1\"",body).getStatus());assertEquals(1,scalar("SELECT count(*) FROM orders.organization_claim_evidence"));assertEquals(2,scalar("SELECT version FROM orders.organization_claim_intake"));
            assertEquals(404,httpRead(mvc,path+"/evidence/"+evidence,owner.access()).getStatus());assertEquals(409,onboardingWrite(mvc,owner,"POST",path+"/evidence-reservations",key,"\"v1\"",Map.of("mime","image/png","bytes",bytes.length+1,"sha256",sha)).getStatus());
            assertThrows(SQLException.class,()->execute("UPDATE orders.organization_claim_evidence SET state='CLEAN',attached_version=3,attached_at=clock_timestamp()"));assertEquals(0,scalar("SELECT count(*) FROM orders.organization_claim_evidence WHERE state='CLEAN'"));
            businessCommand(UUID.randomUUID(),"REVOKE",org,A,"OWNER","'support.read','support.write','support.assign','support.reports.read','support.notifications.manage'");assertEquals(404,httpRead(mvc,path+"/evidence",owner.access()).getStatus());assertEquals(0,scalar("SELECT count(*) FROM orders.pilot_tracking_event"));
        }
    }

    private org.springframework.mock.web.MockHttpServletResponse claimUpload(org.springframework.test.web.servlet.MockMvc mvc,ProfileBrowser browser,String path,byte[] bytes)throws Exception{
        var result=mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put(path).secure(true).header("Origin","https://portal.example.invalid").header("Authorization","Bearer "+browser.access()).header("X-CSRF-Token",browser.flow().challenge).cookie(browser.flow().csrf,browser.flow().refresh).contentType("image/png").content(bytes)).andReturn();onboardingFailure.set(result.getResolvedException());return result.getResponse();
    }
    @Test void claimEvidenceHttpScreensPersistsRetriesAndDeniesRevocationAfterExternalIo()throws Exception{
        claimEvidenceEnabled=true;claimIntakeEnabled=true;liveStaffClock=true;customerFixture(true);staffFixtureId();UUID org=id(9707);organizationSupportOwner(org);businessTestContract(org);
        var rootDirectory=java.nio.file.Files.createTempDirectory(java.nio.file.Path.of(System.getenv("TASK_C_PRIVATE_DIRECTORY")),"claim-private-");var disk=new com.elogistic.backend.platform.internal.infrastructure.FileSystemPrivateEvidenceStore(rootDirectory.resolve("quarantine"),List.of(rootDirectory.resolve("public")));var revoke=new java.util.concurrent.atomic.AtomicBoolean();
        claimStore=new com.elogistic.backend.platform.api.PrivateEvidenceStore(){public Blob put(java.io.InputStream in){throw new Unavailable();}public Blob put(UUID key,java.io.InputStream in){var blob=disk.put(key,in);if(revoke.getAndSet(false))try{businessCommand(UUID.randomUUID(),"REVOKE",org,A,"OWNER","'support.read','support.write','support.assign','support.reports.read','support.notifications.manage'");}catch(Exception failed){throw new Unavailable();}return blob;}public Content read(Blob blob){return disk.read(blob);}};
        try(var daemon=new com.elogistic.backend.testsupport.ClamAvTestDaemon();var inbox=new CustomerSmtpTestInbox(0,false,false,250,null)){
            claimScanner=new com.elogistic.backend.platform.internal.infrastructure.ClamAvEvidenceScreening("127.0.0.1",daemon.port(),2000);
            try(var c=customerHttpContext(PILOT_NOW,smtp(inbox,PILOT_NOW),true,"TEST",false,true)){
                var mvc=httpMvc(c);var owner=profileBrowser(mvc);var other=addressOtherBrowser(mvc);String base=businessShippingBase(org),shipment=businessManagementBooking(mvc,owner,org);var input=new HashMap<>(organizationSupportInput());input.put("kind","SHIPMENT");input.put("shipmentId",shipment);String ticket=supportId(onboardingWrite(mvc,owner,"POST",base+"/support-tickets",UUID.randomUUID(),null,input));var created=onboardingWrite(mvc,owner,"POST",base+"/claim-intakes",UUID.randomUUID(),null,Map.of("ticketId",ticket,"category","DAMAGE","narrative","Synthetic screened file case"));String target=intakeJson(created).get("intake").get("id").asString(),path=base+"/claim-intakes/"+target;
                var out=new java.io.ByteArrayOutputStream();javax.imageio.ImageIO.write(new java.awt.image.BufferedImage(2,2,java.awt.image.BufferedImage.TYPE_INT_RGB),"png",out);byte[] bytes=out.toByteArray();String sha=HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(bytes));var body=Map.of("mime","image/png","bytes",bytes.length,"sha256",sha);
                var reserved=onboardingWrite(mvc,owner,"POST",path+"/evidence-reservations",UUID.randomUUID(),"\"v1\"",body);assertEquals(201,reserved.getStatus(),this::safeOnboardingFailure);String first=path+"/evidence/"+responseId(reserved,"id");daemon.reply="stream: ERROR\0";assertEquals(503,claimUpload(mvc,owner,first,bytes).getStatus());assertEquals(0,scalar("SELECT count(*) FROM orders.organization_claim_evidence WHERE state='CLEAN'"));
                daemon.reply="stream: OK\0";assertEquals(200,claimUpload(mvc,owner,first,bytes).getStatus(),this::safeOnboardingFailure);assertEquals(200,claimUpload(mvc,owner,first,bytes).getStatus());assertEquals(3,scalar("SELECT version FROM orders.organization_claim_intake"));var read=httpRead(mvc,first,owner.access());assertEquals(200,read.getStatus());assertEquals("no-store",read.getHeader("Cache-Control"));assertEquals("nosniff",read.getHeader("X-Content-Type-Options"));assertTrue(read.getHeader("Content-Disposition").startsWith("attachment; filename=\"argopost-evidence-"));assertArrayEquals(bytes,read.getContentAsByteArray());assertEquals(404,httpRead(mvc,first,other.access()).getStatus());
                var second=onboardingWrite(mvc,owner,"POST",path+"/evidence-reservations",UUID.randomUUID(),"\"v3\"",body);assertEquals(201,second.getStatus());String next=path+"/evidence/"+responseId(second,"id");db.ownerExecute("CREATE FUNCTION orders.synthetic_claim_file_failure() RETURNS trigger LANGUAGE plpgsql AS $$ BEGIN RAISE EXCEPTION 'SYNTHETIC_FILE_FINALIZE_FAULT';END $$;CREATE TRIGGER synthetic_claim_file_failure BEFORE UPDATE ON orders.organization_claim_evidence FOR EACH ROW EXECUTE FUNCTION orders.synthetic_claim_file_failure()");
                try{assertEquals(503,claimUpload(mvc,owner,next,bytes).getStatus());assertEquals(4,scalar("SELECT version FROM orders.organization_claim_intake"));assertEquals(1,scalar("SELECT count(*) FROM orders.organization_claim_evidence WHERE state='CLEAN'"));assertEquals(404,httpRead(mvc,next,owner.access()).getStatus());}finally{db.ownerExecute("DROP TRIGGER synthetic_claim_file_failure ON orders.organization_claim_evidence;DROP FUNCTION orders.synthetic_claim_file_failure()");}
                assertEquals(200,claimUpload(mvc,owner,next,bytes).getStatus());assertEquals(5,scalar("SELECT version FROM orders.organization_claim_intake"));
                var third=onboardingWrite(mvc,owner,"POST",path+"/evidence-reservations",UUID.randomUUID(),"\"v5\"",body);assertEquals(201,third.getStatus());revoke.set(true);String denied=path+"/evidence/"+responseId(third,"id");assertEquals(404,claimUpload(mvc,owner,denied,bytes).getStatus());assertEquals(2,scalar("SELECT count(*) FROM orders.organization_claim_evidence WHERE state='CLEAN'"));assertEquals(6,scalar("SELECT version FROM orders.organization_claim_intake"));assertEquals(404,httpRead(mvc,denied,owner.access()).getStatus());assertEquals(0,scalar("SELECT count(*) FROM orders.pilot_tracking_event"));assertEquals(1,scalar("SELECT count(*) FROM finance.pilot_shipping_obligation WHERE state='UNPAID'"));
            }
        }
    }

    @Test void claimReportsAndNoticesUseActualScopedEventsAndTransactionalPreferences()throws Exception{
        claimIntakeEnabled=true;liveStaffClock=true;customerFixture(true);staffFixtureId();UUID org=id(9708);organizationSupportOwner(org);businessTestContract(org);
        try(var inbox=new CustomerSmtpTestInbox(0,false,false,250,null);var c=customerHttpContext(PILOT_NOW,smtp(inbox,PILOT_NOW),true,"TEST",false,true)){
            var mvc=httpMvc(c);var owner=profileBrowser(mvc);var other=addressOtherBrowser(mvc);var staff=staffFixtureBrowser(mvc);organizationSupportStaff(org);String root=businessShippingBase(org),shipment=businessManagementBooking(mvc,owner,org);var ticketInput=new HashMap<>(organizationSupportInput());ticketInput.put("kind","SHIPMENT");ticketInput.put("shipmentId",shipment);String ticket=supportId(onboardingWrite(mvc,owner,"POST",root+"/support-tickets",UUID.randomUUID(),null,ticketInput));var created=onboardingWrite(mvc,owner,"POST",root+"/claim-intakes",UUID.randomUUID(),null,Map.of("ticketId",ticket,"category","DAMAGE","narrative","Synthetic report case"));assertEquals(201,created.getStatus());String intake=intakeJson(created).get("intake").get("id").asString();
            assertEquals(200,organizationSupportWrite(mvc,owner,root+"/support-notification-preferences","n",0,Map.of("enabled",true,"eventTypes",List.of("NEEDS_INFORMATION","REVIEW")),UUID.randomUUID()).getStatus());assertEquals(200,supportWrite(mvc,staff,SUPPORT_STAFF+"/organization-cases/"+ticket+"/assignments",1,Map.of("assigneeAccountId",staffFixtureId())).getStatus());
            String path=SUPPORT_STAFF+"/organization-claim-intakes/"+intake+"/commands";UUID key=UUID.randomUUID();var command=Map.of("action","REQUEST_INFORMATION","message","Synthetic additional information request");assertEquals(200,onboardingWrite(mvc,staff,"POST",path,key,"\"v1\"",command).getStatus(),this::safeOnboardingFailure);assertEquals(200,onboardingWrite(mvc,staff,"POST",path,key,"\"v1\"",command).getStatus());assertEquals(1,scalar("SELECT count(*) FROM orders.organization_claim_notice"));
            var notices=intakeJson(httpRead(mvc,root+"/claim-intake-notices",owner.access()));assertEquals(1,notices.get("items").size());assertEquals(intake,notices.get("items").get(0).get("intakeId").asString());assertEquals(404,httpRead(mvc,root+"/claim-intake-notices",other.access()).getStatus());assertEquals(401,httpRead(mvc,root+"/claim-intake-notices",staff.access()).getStatus());
            var report=httpRead(mvc,root+"/claim-intake-reports",owner.access());assertEquals(200,report.getStatus(),this::safeOnboardingFailure);var stats=intakeJson(report);assertEquals("1",stats.get("total").asString());assertEquals("1",stats.get("firstStaffResponse").get("sampleCount").asString());assertEquals("0",stats.get("intakeClosure").get("sampleCount").asString());assertEquals("BLOCKED",stats.get("compensation").asString());assertEquals("UNPUBLISHED",stats.get("slaState").asString());
            UUID site=id(9709);String contact=new tools.jackson.databind.ObjectMapper().writeValueAsString(shippingDraftInput("Synthetic facility").get("sender")).replace("0901234567","+84901234567");seedFacility(site,org,contact);businessCommand(UUID.randomUUID(),"GRANT",org,B,"OPERATOR","'dashboard.read'");facilityGrant(org,B,site,"'support.read','support.reports.read'");assertEquals(0,intakeJson(httpRead(mvc,root+"/claim-intakes",other.access())).get("items").size());assertEquals("0",intakeJson(httpRead(mvc,root+"/claim-intake-reports",other.access())).get("total").asString());assertEquals(404,httpRead(mvc,root+"/claim-intakes/"+intake,other.access()).getStatus());
            db.ownerExecute("CREATE FUNCTION orders.synthetic_claim_notice_failure() RETURNS trigger LANGUAGE plpgsql AS $$ BEGIN RAISE EXCEPTION 'SYNTHETIC_CLAIM_NOTICE_FAULT';END $$;CREATE TRIGGER synthetic_claim_notice_failure BEFORE INSERT ON orders.organization_claim_notice FOR EACH ROW EXECUTE FUNCTION orders.synthetic_claim_notice_failure()");
            try{assertEquals(503,supportWrite(mvc,staff,path,2,Map.of("action","CLOSE_WITHOUT_DECISION","message","Must rollback together")).getStatus());assertEquals(2,scalar("SELECT version FROM orders.organization_claim_intake"));assertEquals(2,scalar("SELECT count(*) FROM orders.organization_claim_intake_event"));}finally{db.ownerExecute("DROP TRIGGER synthetic_claim_notice_failure ON orders.organization_claim_notice;DROP FUNCTION orders.synthetic_claim_notice_failure()");}
            assertEquals(200,supportWrite(mvc,staff,path,2,Map.of("action","CLOSE_WITHOUT_DECISION","message","Closed without compensation decision")).getStatus());assertEquals(2,scalar("SELECT count(*) FROM orders.organization_claim_notice"));var closed=intakeJson(httpRead(mvc,root+"/claim-intake-reports",owner.access()));assertEquals("1",closed.get("intakeClosure").get("sampleCount").asString());assertEquals("1",closed.get("byState").get("CLOSED_WITHOUT_DECISION").asString());assertThrows(SQLException.class,()->execute("DELETE FROM orders.organization_claim_notice"));assertEquals(0,scalar("SELECT count(*) FROM orders.pilot_tracking_event"));
        }
    }

    @Test void claimIntakeBrowserUsesRealMerchantStaffAndPostgres()throws Exception{
        claimIntakeEnabled=true;liveStaffClock=true;customerFixture(true);staffFixtureId();UUID org=id(9705);organizationSupportOwner(org);businessTestContract(org);organizationSupportStaff(org);
        try(var inbox=new CustomerSmtpTestInbox(0,false,false,250,null);var c=customerHttpContext(PILOT_NOW,smtp(inbox,PILOT_NOW),true,"TEST",false,true)){
            var mvc=httpMvc(c);var owner=profileBrowser(mvc);String shipment=businessManagementBooking(mvc,owner,org);var input=new HashMap<>(organizationSupportInput());input.put("kind","SHIPMENT");input.put("shipmentId",shipment);input.put("subject","Synthetic browser Claim ticket");supportId(onboardingWrite(mvc,owner,"POST",businessShippingBase(org)+"/support-tickets",UUID.randomUUID(),null,input));
        }
        S003CustomerBrowserRuntime.runClaims(outboxDataSource());assertEquals(1,scalar("SELECT count(*) FROM orders.organization_claim_evidence WHERE state='CLEAN'"));assertEquals(1,scalar("SELECT count(*) FROM orders.organization_claim_intake WHERE state='CLOSED_WITHOUT_DECISION' AND version=6"));assertEquals(6,scalar("SELECT count(*) FROM orders.organization_claim_intake_event"));assertEquals(6,scalar("SELECT count(*) FROM orders.organization_claim_intake_receipt"));assertEquals(1,scalar("SELECT count(*) FROM finance.pilot_shipping_obligation WHERE state='UNPAID'"));assertEquals(0,scalar("SELECT count(*) FROM orders.pilot_tracking_event"));
    }

    @Test void claimIntakeConcurrentReplayLateFailureAndCurrentMembershipAreEnforced()throws Exception{
        claimIntakeEnabled=true;liveStaffClock=true;customerFixture(true);staffFixtureId();UUID org=id(9702);organizationSupportOwner(org);businessTestContract(org);
        try(var inbox=new CustomerSmtpTestInbox(0,false,false,250,null);var c=customerHttpContext(PILOT_NOW,smtp(inbox,PILOT_NOW),true,"TEST",false,true)){
            var mvc=httpMvc(c);var owner=profileBrowser(mvc);String root=businessShippingBase(org),base=root+"/claim-intakes",shipment=businessManagementBooking(mvc,owner,org);var input=new HashMap<>(organizationSupportInput());input.put("kind","SHIPMENT");input.put("shipmentId",shipment);String ticket=supportId(onboardingWrite(mvc,owner,"POST",root+"/support-tickets",UUID.randomUUID(),null,input));var body=Map.of("ticketId",ticket,"category","LOSS","narrative","Synthetic investigation record");UUID key=UUID.randomUUID();
            db.ownerExecute("CREATE FUNCTION orders.synthetic_claim_failure() RETURNS trigger LANGUAGE plpgsql AS $$ BEGIN RAISE EXCEPTION 'SYNTHETIC_CLAIM_FAULT';END $$;CREATE TRIGGER synthetic_claim_failure BEFORE INSERT ON orders.organization_claim_intake_receipt FOR EACH ROW EXECUTE FUNCTION orders.synthetic_claim_failure()");
            try{assertEquals(503,onboardingWrite(mvc,owner,"POST",base,key,null,body).getStatus());assertEquals(0,scalar("SELECT count(*) FROM orders.organization_claim_intake"));assertEquals(0,scalar("SELECT count(*) FROM orders.organization_claim_intake_event"));}finally{db.ownerExecute("DROP TRIGGER synthetic_claim_failure ON orders.organization_claim_intake_receipt;DROP FUNCTION orders.synthetic_claim_failure()");}
            assertEquals(List.of(201,201),adminRace(()->onboardingWrite(mvc,owner,"POST",base,key,null,body).getStatus(),()->onboardingWrite(mvc,owner,"POST",base,key,null,body).getStatus()));
            var receipt=httpRead(mvc,root+"/claim-intake-receipts/CREATE/"+key,owner.access());assertEquals(200,receipt.getStatus());String target=responseId(receipt,"targetId"),path=base+"/"+target+"/supplements";var text=Map.of("narrative","Synthetic concurrent additional detail");
            assertEquals(List.of(200,409),adminRace(()->onboardingWrite(mvc,owner,"POST",path,UUID.randomUUID(),"\"v1\"",text).getStatus(),()->onboardingWrite(mvc,owner,"POST",path,UUID.randomUUID(),"\"v1\"",text).getStatus()).stream().map(Integer.class::cast).sorted().toList());
            assertEquals(2,scalar("SELECT count(*) FROM orders.organization_claim_intake_event"));assertEquals(2,scalar("SELECT count(*) FROM orders.organization_claim_intake_receipt"));
            assertThrows(SQLException.class,()->execute("UPDATE orders.organization_claim_intake SET state='PAID',version=version+1"));assertThrows(SQLException.class,()->execute("UPDATE orders.organization_claim_intake_event SET message='Forged history'"));
            businessCommand(UUID.randomUUID(),"REVOKE",org,A,"OWNER","'support.read','support.write','support.assign','support.reports.read','support.notifications.manage'");assertEquals(404,httpRead(mvc,base+"/"+target,owner.access()).getStatus());assertEquals(404,httpRead(mvc,root+"/claim-intake-receipts/CREATE/"+key,owner.access()).getStatus());assertEquals(404,onboardingWrite(mvc,owner,"POST",base,key,null,body).getStatus());
            assertEquals(0,scalar("SELECT count(*) FROM orders.pilot_tracking_event"));assertEquals(1,scalar("SELECT count(*) FROM finance.pilot_shipping_obligation WHERE state='UNPAID'"));
        }
    }
    @Test void claimIntakeRejectsGeneralAndForeignTicketsAndIsDisabledByDefault()throws Exception{
        claimIntakeEnabled=true;liveStaffClock=true;customerFixture(true);staffFixtureId();UUID org=id(9703),foreign=id(9704);organizationSupportOwner(org);organizationSupportOwner(foreign);
        try(var inbox=new CustomerSmtpTestInbox(0,false,false,250,null);var c=customerHttpContext(PILOT_NOW,smtp(inbox,PILOT_NOW),true,"TEST",false,true)){
            var mvc=httpMvc(c);var owner=profileBrowser(mvc);String root=businessShippingBase(org),ticket=supportId(onboardingWrite(mvc,owner,"POST",root+"/support-tickets",UUID.randomUUID(),null,organizationSupportInput()));var input=Map.of("ticketId",ticket,"category","DAMAGE","narrative","Not a Shipment ticket");assertEquals(400,onboardingWrite(mvc,owner,"POST",root+"/claim-intakes",UUID.randomUUID(),null,input).getStatus());
            assertEquals(404,onboardingWrite(mvc,owner,"POST",businessShippingBase(foreign)+"/claim-intakes",UUID.randomUUID(),null,input).getStatus());assertEquals(0,scalar("SELECT count(*) FROM orders.organization_claim_intake"));
        }
        claimIntakeEnabled=false;
        try(var inbox=new CustomerSmtpTestInbox(0,false,false,250,null);var c=customerHttpContext(PILOT_NOW,smtp(inbox,PILOT_NOW),true,"TEST",false,true)){var mvc=httpMvc(c);var owner=profileBrowser(mvc);assertEquals(503,httpRead(mvc,businessShippingBase(org)+"/claim-intakes",owner.access()).getStatus());}
    }

    @Test void organizationSupportReplayQuotaAndLateFailureAreAtomic()throws Exception{
        customerFixture(true);UUID org=id(9203);organizationSupportOwner(org);
        try(var inbox=new CustomerSmtpTestInbox(0,false,false,250,null);var c=customerHttpContext(PILOT_NOW,smtp(inbox,PILOT_NOW),true,"TEST",false,true)){
            var mvc=httpMvc(c);var owner=profileBrowser(mvc);String base=businessShippingBase(org)+"/support-tickets";UUID key=UUID.randomUUID();var input=organizationSupportInput();
            assertEquals(List.of(201,201),adminRace(()->onboardingWrite(mvc,owner,"POST",base,key,null,input).getStatus(),()->onboardingWrite(mvc,owner,"POST",base,key,null,input).getStatus()));
            var receipt=httpRead(mvc,businessShippingBase(org)+"/support-receipts/CREATE/"+key,owner.access());assertEquals(200,receipt.getStatus());String target=responseId(receipt,"targetId");
            var modified=new HashMap<>(input);modified.put("message","Different input");assertEquals(409,onboardingWrite(mvc,owner,"POST",base,key,null,modified).getStatus());
            UUID reply=UUID.randomUUID();String path=base+"/"+target+"/replies";assertEquals(200,onboardingWrite(mvc,owner,"POST",path,reply,"\"v1\"",Map.of("message","Lost response")).getStatus());assertEquals(200,onboardingWrite(mvc,owner,"POST",path,reply,"\"v1\"",Map.of("message","Lost response")).getStatus());assertEquals(409,supportWrite(mvc,owner,path,1,Map.of("message","Stale version")).getStatus());
            assertEquals(2,scalar("SELECT count(*) FROM orders.organization_support_message"));assertEquals(2,scalar("SELECT sum(count) FROM orders.organization_support_window"));
            db.ownerExecute("CREATE FUNCTION orders.synthetic_organization_support_failure() RETURNS trigger LANGUAGE plpgsql AS $$ BEGIN RAISE EXCEPTION 'SYNTHETIC_SUPPORT_FAULT';END $$;CREATE TRIGGER synthetic_organization_support_failure BEFORE INSERT ON orders.organization_support_audit FOR EACH ROW EXECUTE FUNCTION orders.synthetic_organization_support_failure()");
            try{assertEquals(503,onboardingWrite(mvc,owner,"POST",base,UUID.randomUUID(),null,input).getStatus());assertEquals(1,scalar("SELECT count(*) FROM orders.organization_support_ticket"));assertEquals(2,scalar("SELECT sum(count) FROM orders.organization_support_window"));}finally{db.ownerExecute("DROP TRIGGER synthetic_organization_support_failure ON orders.organization_support_audit;DROP FUNCTION orders.synthetic_organization_support_failure()");}
            for(int i=0;i<4;i++)supportId(onboardingWrite(mvc,owner,"POST",base,UUID.randomUUID(),null,input));assertEquals(429,onboardingWrite(mvc,owner,"POST",base,UUID.randomUUID(),null,input).getStatus());
            assertEquals(5,scalar("SELECT count(*) FROM orders.organization_support_ticket"));
        }
    }
    @Test void organizationSupportNotificationsPreferencesAndReadReplayUseCommittedEvents()throws Exception{
        liveStaffClock=true;customerFixture(true);UUID org=id(9204);organizationSupportOwner(org);
        try(var inbox=new CustomerSmtpTestInbox(0,false,false,250,null);var c=customerHttpContext(PILOT_NOW,smtp(inbox,PILOT_NOW),true,"TEST",false,true)){
            var mvc=httpMvc(c);var owner=profileBrowser(mvc);var staff=staffFixtureBrowser(mvc);organizationSupportStaff(org);String root=businessShippingBase(org),base=root+"/support-tickets";
            var virtual=httpRead(mvc,root+"/support-notification-preferences",owner.access());assertEquals("\"n0\"",virtual.getHeader("ETag"));assertEquals(0,scalar("SELECT count(*) FROM orders.organization_support_notification_preference"));
            UUID preferenceKey=UUID.randomUUID();var body=Map.of("enabled",true,"eventTypes",List.of("ASSIGNMENT","REPLY","NEEDS_INFORMATION","RESOLUTION","REVIEW"));
            assertEquals(200,organizationSupportWrite(mvc,owner,root+"/support-notification-preferences","n",0,body,preferenceKey).getStatus());
            assertEquals(200,organizationSupportWrite(mvc,owner,root+"/support-notification-preferences","n",0,body,preferenceKey).getStatus());assertEquals(1,scalar("SELECT version FROM orders.organization_support_notification_preference"));
            String target=supportId(onboardingWrite(mvc,owner,"POST",base,UUID.randomUUID(),null,organizationSupportInput()));String staffPath=SUPPORT_STAFF+"/organization-cases/"+target;
            UUID assignmentKey=UUID.randomUUID();assertEquals(200,onboardingWrite(mvc,staff,"POST",staffPath+"/assignments",assignmentKey,"\"v1\"",Map.of("assigneeAccountId",staffFixtureId())).getStatus());assertEquals(200,onboardingWrite(mvc,staff,"POST",staffPath+"/assignments",assignmentKey,"\"v1\"",Map.of("assigneeAccountId",staffFixtureId())).getStatus());
            assertEquals(1,scalar("SELECT count(*) FROM orders.organization_support_notice"));
            var notices=httpRead(mvc,root+"/support-notices",owner.access());assertEquals(200,notices.getStatus());String notice=intakeJson(notices).get("items").get(0).get("id").asString();UUID readKey=UUID.randomUUID();
            assertEquals(200,onboardingWrite(mvc,owner,"POST",root+"/support-notices/"+notice+"/read",readKey,"\"v1\"",Map.of()).getStatus());assertEquals(200,onboardingWrite(mvc,owner,"POST",root+"/support-notices/"+notice+"/read",readKey,"\"v1\"",Map.of()).getStatus());
            assertEquals(2,scalar("SELECT version FROM orders.organization_support_notice"));assertEquals(1,scalar("SELECT count(*) FROM orders.organization_support_receipt WHERE operation='READ_NOTICE'"));
            assertEquals(401,httpRead(mvc,root+"/support-notices",staff.access()).getStatus());
            assertEquals(200,supportWrite(mvc,staff,staffPath+"/transitions",2,Map.of("action","START","message","Started")).getStatus());assertEquals(2,scalar("SELECT count(*) FROM orders.organization_support_notice"));
            db.ownerExecute("CREATE FUNCTION orders.synthetic_support_notice_fault() RETURNS trigger LANGUAGE plpgsql AS $$ BEGIN RAISE EXCEPTION 'SYNTHETIC_NOTICE_FAULT';END $$;CREATE TRIGGER synthetic_support_notice_fault BEFORE INSERT ON orders.organization_support_notice FOR EACH ROW EXECUTE FUNCTION orders.synthetic_support_notice_fault()");
            try{assertEquals(503,supportWrite(mvc,staff,staffPath+"/replies",3,Map.of("message","Must rollback")).getStatus());assertEquals(3,scalar("SELECT version FROM orders.organization_support_ticket"));assertEquals(3,scalar("SELECT count(*) FROM orders.organization_support_message"));}finally{db.ownerExecute("DROP TRIGGER synthetic_support_notice_fault ON orders.organization_support_notice;DROP FUNCTION orders.synthetic_support_notice_fault()");}
            assertEquals(0,inbox.messages().size());
        }
    }
    @Test void organizationSupportFacilityScopeAssignmentAndRevocationFailClosed()throws Exception{
        customerFixture(true);UUID org=id(9205),foreign=id(9206),site=id(9207),outside=id(9208);organizationSupportOwner(org);businessShippingOwner(foreign);
        try(var inbox=new CustomerSmtpTestInbox(0,false,false,250,null);var c=customerHttpContext(PILOT_NOW,smtp(inbox,PILOT_NOW),true,"TEST",false,true)){
            var mvc=httpMvc(c);var owner=profileBrowser(mvc);var member=addressOtherBrowser(mvc);String siteContact=new tools.jackson.databind.ObjectMapper().writeValueAsString(shippingDraftInput("Support site").get("sender")).replace("0901234567","+84901234567");seedFacility(site,org,siteContact);seedFacility(outside,org,siteContact);businessCommand(UUID.randomUUID(),"GRANT",org,B,"OPERATOR","'dashboard.read'");facilityGrant(org,B,site,"'support.read','support.write','support.assign','support.reports.read','support.notifications.manage'");String base=businessShippingBase(org)+"/support-tickets";
            String broad=supportId(onboardingWrite(mvc,owner,"POST",base,UUID.randomUUID(),null,organizationSupportInput()));var scoped=new HashMap<>(organizationSupportInput());scoped.put("facilityId",site);String local=supportId(onboardingWrite(mvc,owner,"POST",base,UUID.randomUUID(),null,scoped));scoped.put("facilityId",outside);String other=supportId(onboardingWrite(mvc,owner,"POST",base,UUID.randomUUID(),null,scoped));
            var list=httpRead(mvc,base,member.access());assertEquals(200,list.getStatus());assertTrue(list.getContentAsString().contains(local));assertFalse(list.getContentAsString().contains(broad));assertFalse(list.getContentAsString().contains(other));
            assertEquals(404,httpRead(mvc,base+"/"+broad,member.access()).getStatus());assertEquals(404,httpRead(mvc,businessShippingBase(foreign)+"/support-tickets/"+local,member.access()).getStatus());
            String candidates=base+"/"+local+"/assignment-candidates";assertEquals(200,httpRead(mvc,candidates,member.access()).getStatus());
            String assignment=base+"/"+local+"/assignment";assertEquals("\"a0\"",httpRead(mvc,assignment,owner.access()).getHeader("ETag"));
            UUID key=UUID.randomUUID();assertEquals(200,organizationSupportWrite(mvc,owner,assignment,"a",0,Map.of("assigneeAccountId",B),key).getStatus());assertEquals(200,organizationSupportWrite(mvc,owner,assignment,"a",0,Map.of("assigneeAccountId",B),key).getStatus());
            assertEquals(1,scalar("SELECT count(*) FROM orders.organization_support_assignment_event"));assertEquals(1,scalar("SELECT version FROM orders.organization_support_ticket WHERE id='"+local+"'"));
            assertEquals(1,intakeJson(httpRead(mvc,base+"?assignee="+B,member.access())).get("items").size());
            var report=httpRead(mvc,businessShippingBase(org)+"/support-reports?from=2026-11-02&until=2026-11-03",member.access());assertEquals(200,report.getStatus());assertEquals("1",intakeJson(report).get("totalTickets").asString());
            db.ownerExecute("BEGIN;SET LOCAL argopost.operator_environment='TEST';SELECT merchant.configure_local_facility_grants('"+UUID.randomUUID()+"','REVOKE','"+org+"','"+B+"','"+site+"',ARRAY['support.write'],'Synthetic permission revoke');COMMIT;");
            assertEquals(409,organizationSupportWrite(mvc,owner,assignment,"a",0,Map.of("assigneeAccountId",A),UUID.randomUUID()).getStatus());
            assertEquals(404,supportWrite(mvc,member,base+"/"+local+"/replies",1,Map.of("message","Revoked write")).getStatus());
        }
    }
    @Test void organizationSupportTypedTargetsEventsAndStaffBindingsCannotBeForged()throws Exception{
        liveStaffClock=true;customerFixture(true);UUID org=id(9209);organizationSupportOwner(org);
        try(var inbox=new CustomerSmtpTestInbox(0,false,false,250,null);var c=customerHttpContext(PILOT_NOW,smtp(inbox,PILOT_NOW),true,"TEST",false,true)){
            var mvc=httpMvc(c);var owner=profileBrowser(mvc);var staff=staffFixtureBrowser(mvc);String base=businessShippingBase(org)+"/support-tickets";String target=supportId(onboardingWrite(mvc,owner,"POST",base,UUID.randomUUID(),null,organizationSupportInput()));
            assertThrows(SQLException.class,()->execute("UPDATE orders.organization_support_ticket SET version=version+1 WHERE id='"+target+"'"));
            assertThrows(SQLException.class,()->execute("UPDATE orders.organization_support_message SET message='tampered'"));
            assertThrows(SQLException.class,()->execute("SELECT * FROM orders.configure_local_organization_support_staff(gen_random_uuid(),'BIND','"+org+"','"+staffFixtureId()+"','SUPPORT_TRIAGE','forged')"));
            assertThrows(SQLException.class,()->execute("SELECT * FROM orders.support_operator_audit"));
            assertThrows(SQLException.class,()->execute("INSERT INTO orders.organization_support_receipt VALUES(gen_random_uuid(),'"+org+"','"+A+"','PREFERENCES',gen_random_uuid(),'PREFERENCES',NULL,'"+A+"',NULL,'"+A+"',0,1,'v1',decode(repeat('01',32),'hex'),clock_timestamp())"));
            organizationSupportStaff(org);String path=SUPPORT_STAFF+"/organization-cases/"+target;UUID staffKey=UUID.randomUUID();assertEquals(200,onboardingWrite(mvc,staff,"POST",path+"/assignments",staffKey,"\"v1\"",Map.of("assigneeAccountId",staffFixtureId())).getStatus());
            UUID second=id(9215);organizationSupportOwner(second);for(String permission:List.of("SUPPORT_TRIAGE","SUPPORT_HANDLE"))db.ownerExecute("BEGIN;SET LOCAL argopost.operator_environment='TEST';SELECT * FROM orders.configure_local_organization_support_staff('"+UUID.randomUUID()+"','BIND','"+second+"','"+staffFixtureId()+"','"+permission+"','Synthetic second binding');COMMIT;");
            String other=supportId(onboardingWrite(mvc,owner,"POST",businessShippingBase(second)+"/support-tickets",UUID.randomUUID(),null,organizationSupportInput()));
            assertEquals(409,onboardingWrite(mvc,staff,"POST",SUPPORT_STAFF+"/organization-cases/"+other+"/assignments",staffKey,"\"v1\"",Map.of("assigneeAccountId",staffFixtureId())).getStatus());
            assertEquals(target,intakeJson(httpRead(mvc,SUPPORT_STAFF+"/organization-receipts/CARRIER_ASSIGN/"+staffKey,staff.access())).get("targetId").asString());assertEquals(1,scalar("SELECT version FROM orders.organization_support_ticket WHERE id='"+other+"'"));
            UUID unbind=UUID.randomUUID();String sql="SELECT * FROM orders.configure_local_organization_support_staff('"+unbind+"','UNBIND','"+org+"','"+staffFixtureId()+"','SUPPORT_HANDLE','Synthetic revoke')";
            db.ownerExecute("BEGIN;SET LOCAL argopost.operator_environment='TEST';"+sql+";COMMIT;");db.ownerExecute("BEGIN;SET LOCAL argopost.operator_environment='TEST';"+sql+";COMMIT;");
            assertEquals(403,httpRead(mvc,path,staff.access()).getStatus());assertEquals(403,supportWrite(mvc,staff,path+"/transitions",2,Map.of("action","START","message","Revoked binding")).getStatus());
            assertEquals(5,businessScalar("SELECT count(*) FROM orders.support_operator_audit"));assertEquals(2,scalar("SELECT version FROM orders.organization_support_ticket WHERE id='"+target+"'"));
            assertEquals(0,scalar("SELECT count(*) FROM orders.organization_support_notice"));
        }
    }
    @Test void organizationSupportStrictHttpSearchAndProductionDenyWithoutSideEffects()throws Exception{
        customerFixture(true);UUID org=id(9210);organizationSupportOwner(org);
        try(var inbox=new CustomerSmtpTestInbox(0,false,false,250,null);var c=customerHttpContext(PILOT_NOW,smtp(inbox,PILOT_NOW),true,"TEST",false,true)){
            var mvc=httpMvc(c);var owner=profileBrowser(mvc);String base=businessShippingBase(org)+"/support-tickets";String target=supportId(onboardingWrite(mvc,owner,"POST",base,UUID.randomUUID(),null,organizationSupportInput()));
            assertEquals(400,httpRead(mvc,base+"?search=forbidden",owner.access()).getStatus());assertEquals(400,httpRead(mvc,base+"?page=0&page=1",owner.access()).getStatus());assertEquals(400,httpRead(mvc,base+"?size=21",owner.access()).getStatus());
            var read=onboardingWrite(mvc,owner,"POST",base+"/search",UUID.randomUUID(),null,Map.of("search","Synthetic support","page",0,"size",20));assertEquals(200,read.getStatus());assertTrue(read.getContentAsString().contains(target));assertEquals("no-store",read.getHeader("Cache-Control"));
            assertEquals(1,scalar("SELECT count(*) FROM orders.organization_support_receipt"));assertEquals(1,scalar("SELECT sum(count) FROM orders.organization_support_window"));
            var over=new HashMap<>(organizationSupportInput());over.put("organizationId",org);assertEquals(400,onboardingWrite(mvc,owner,"POST",base,UUID.randomUUID(),null,over).getStatus());
            assertEquals(400,onboardingWrite(mvc,owner,"POST",base,UUID.randomUUID(),null,"{\"kind\":\"GENERAL\",\"kind\":\"GENERAL\"}").getStatus());
            assertEquals(428,onboardingWrite(mvc,owner,"POST",base+"/"+target+"/replies",UUID.randomUUID(),null,Map.of("message","Missing ETag")).getStatus());
            assertEquals(400,supportWrite(mvc,owner,base+"/"+target+"/acknowledgements",1,Map.of("message","not allowed")).getStatus());
        }
        for(String mode:List.of("PRODUCTION","TEST_WITH_PRODUCTION_PROFILE"))try(var inbox=new CustomerSmtpTestInbox(0,false,false,250,null);var c=customerHttpContext(PILOT_NOW,smtp(inbox,PILOT_NOW),true,mode,false,true)){
            var mvc=httpMvc(c);var owner=profileBrowser(mvc);assertEquals(503,httpRead(mvc,businessShippingBase(org)+"/support-tickets",owner.access()).getStatus());
        }
        assertEquals(1,scalar("SELECT count(*) FROM orders.organization_support_ticket"));
    }
    @Test void organizationSupportShipmentBindingUsesCanonicalBusinessReadAndImmutableFacility()throws Exception{
        customerFixture(true);UUID org=id(9211),foreign=id(9212);organizationSupportOwner(org);businessShippingOwner(foreign);businessTestContract(org);
        try(var inbox=new CustomerSmtpTestInbox(0,false,false,250,null);var c=customerHttpContext(PILOT_NOW,smtp(inbox,PILOT_NOW),true,"TEST",false,true)){
            var mvc=httpMvc(c);var owner=profileBrowser(mvc);var draftInput=new HashMap<String,Object>(pilotDraftInput("Synthetic Business Booking"));draftInput.put("sourceChannel","MANUAL");draftInput.put("sourceOrderReference","SYNTHETIC-REF-9211");
            var draft=onboardingWrite(mvc,owner,"POST",preparationPath(org),UUID.randomUUID(),null,draftInput);assertEquals(201,draft.getStatus(),this::safeOnboardingFailure);String draftId=responseId(draft,"id");var quote=businessTestQuote(mvc,owner,org,draftId,UUID.randomUUID());assertEquals(201,quote.getStatus(),this::safeOnboardingFailure);var booking=businessTestBook(mvc,owner,org,draftId,intakeJson(quote).get("quote").get("id").asString(),UUID.randomUUID());assertEquals(201,booking.getStatus(),this::safeOnboardingFailure);
            String shipment=intakeJson(booking).get("shipment").get("id").asString(),base=businessShippingBase(org)+"/support-tickets";var input=new HashMap<>(organizationSupportInput());input.put("kind","SHIPMENT");input.put("shipmentId",shipment);input.put("category","SHIPMENT_TRACE");
            String target=supportId(onboardingWrite(mvc,owner,"POST",base,UUID.randomUUID(),null,input));assertEquals(shipment,intakeJson(httpRead(mvc,base+"/"+target,owner.access())).get("shipmentId").asString());
            var search=onboardingWrite(mvc,owner,"POST",base+"/search",null,null,Map.of("search","SYNTHETIC-REF-9211"));assertEquals(200,search.getStatus(),this::safeOnboardingFailure);assertEquals(target,intakeJson(search).get("items").get(0).get("id").asString());
            input.put("facilityId",id(9213));assertEquals(400,onboardingWrite(mvc,owner,"POST",base,UUID.randomUUID(),null,input).getStatus());input.remove("facilityId");
            businessCommand(UUID.randomUUID(),"GRANT",foreign,A,"OWNER",ORGANIZATION_SUPPORT_RIGHTS);assertEquals(404,onboardingWrite(mvc,owner,"POST",businessShippingBase(foreign)+"/support-tickets",UUID.randomUUID(),null,input).getStatus());
            assertThrows(SQLException.class,()->execute("UPDATE orders.organization_support_ticket SET facility_id='"+id(9213)+"',version=version+1 WHERE id='"+target+"'"));
            businessCommand(UUID.randomUUID(),"REVOKE",org,A,"OWNER","'shipment.draft.read','shipment.draft.write','shipment.quote','shipment.book','shipment.label.read'");
            assertEquals(404,onboardingWrite(mvc,owner,"POST",base,UUID.randomUUID(),null,input).getStatus());assertEquals(1,scalar("SELECT count(*) FROM orders.organization_support_ticket"));
            assertEquals(1,scalar("SELECT count(*) FROM finance.pilot_shipping_obligation WHERE state='UNPAID'"));assertEquals(0,scalar("SELECT count(*) FROM orders.pilot_tracking_event"));
        }
    }
    @Test void organizationSupportCandidatesFilterEligibilityBeforePaginationBeyondAdmissionLimit()throws Exception{
        customerFixture(true);UUID org=id(9214);organizationSupportOwner(org);
        // Test-owned synthetic Organization, no real recipients or demo database.
        for(int n=0;n<110;n++){
            UUID member=id(9300+n);String email="support-candidate-"+n+"@example.invalid";boolean disabled=n<25;
            execute("INSERT INTO identity.user_account(id,username,display_name,email,password_hash,status,email_verified,created_at,updated_at,activated_at,disabled_at) VALUES('"+member+"','support.candidate."+n+"','Synthetic member "+n+"','"+email+"',(SELECT password_hash FROM identity.user_account WHERE id='"+A+"'),'ACTIVE',TRUE,'2026-11-01','2026-11-01','2026-11-01',NULL)");
            execute("INSERT INTO identity.customer_identifier VALUES(gen_random_uuid(),'"+member+"','"+email+"','2026-11-01','ACTIVE',1)");
            businessCommand(UUID.randomUUID(),"GRANT",org,member,"OPERATOR","'dashboard.read','support.read','support.write'");
            if(disabled)execute("UPDATE identity.user_account SET status='DISABLED',disabled_at=updated_at WHERE id='"+member+"'");
        }
        try(var inbox=new CustomerSmtpTestInbox(0,false,false,250,null);var c=customerHttpContext(PILOT_NOW,smtp(inbox,PILOT_NOW),true,"TEST",false,true)){
            var mvc=httpMvc(c);var owner=profileBrowser(mvc);String base=businessShippingBase(org)+"/support-tickets";String target=supportId(onboardingWrite(mvc,owner,"POST",base,UUID.randomUUID(),null,organizationSupportInput()));
            var first=httpRead(mvc,base+"/"+target+"/assignment-candidates?page=0&size=20",owner.access());assertEquals(200,first.getStatus());assertEquals(20,intakeJson(first).get("items").size());assertTrue(intakeJson(first).get("hasNext").asBoolean());
            for(int n=0;n<25;n++)assertFalse(first.getContentAsString().contains(id(9300+n).toString()));
            var last=httpRead(mvc,base+"/"+target+"/assignment-candidates?page=4&size=20",owner.access());assertEquals(200,last.getStatus());assertEquals(6,intakeJson(last).get("items").size());assertFalse(intakeJson(last).get("hasNext").asBoolean());assertFalse(last.getContentAsString().contains("@example.invalid"));
            assertEquals(0,scalar("SELECT count(*) FROM orders.organization_support_assignment"));assertEquals(1,scalar("SELECT count(*) FROM orders.organization_support_receipt"));
        }
    }
    @Test void organizationSupportSchemaIsForwardOnlyAndDisabledByDefault()throws Exception{
        assertEquals(12,businessScalar("SELECT count(*) FROM information_schema.tables WHERE table_schema='orders' AND (table_name LIKE 'organization_support_%' OR table_name='support_operator_audit')"));
        assertEquals(0,scalar("SELECT count(*) FROM orders.organization_support_staff_scope"));
        customerFixture(true);UUID org=id(9201);businessShippingOwner(org);
        businessCommand(UUID.randomUUID(),"GRANT",org,A,"OWNER","'dashboard.read','support.read','support.write','support.assign','support.reports.read','support.notifications.manage'");
        try(var inbox=new CustomerSmtpTestInbox(0,false,false,250,null);var c=customerHttpContext(NOW,smtp(inbox,NOW),true)){
            var mvc=httpMvc(c);var owner=profileBrowser(mvc);
            assertEquals(503,httpRead(mvc,businessShippingBase(org)+"/support-tickets",owner.access()).getStatus());
        }
        assertEquals(0,scalar("SELECT count(*) FROM orders.organization_support_ticket"));
    }

}
