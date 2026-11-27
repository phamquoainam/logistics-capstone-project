package com.elogistic.backend.workflow.internal.web;

import com.elogistic.backend.testsupport.*;
import com.elogistic.backend.testsupport.PostgresTestcontainersSupport.Database;
import com.elogistic.backend.identity.api.*;
import com.elogistic.backend.identity.api.admin.*;
import com.elogistic.backend.identity.internal.application.*;
import com.elogistic.backend.identity.internal.infrastructure.*;
import com.elogistic.backend.identity.internal.web.*;
import com.elogistic.backend.orders.api.*;
import com.elogistic.backend.orders.internal.application.*;
import com.elogistic.backend.orders.internal.infrastructure.*;
import com.elogistic.backend.pricing.api.*;
import com.elogistic.backend.pricing.internal.application.PilotQuoteApplication;
import com.elogistic.backend.pricing.internal.infrastructure.JdbcPilotQuoteStore;
import com.elogistic.backend.finance.internal.infrastructure.*;
import com.elogistic.backend.dispatch.api.StationIntake;
import com.elogistic.backend.dispatch.internal.infrastructure.*;
import com.elogistic.backend.merchant.internal.infrastructure.AdminApplicationSource;
import com.elogistic.backend.notifications.api.AdminDashboardReads;
import com.elogistic.backend.notifications.internal.application.AdminDashboardService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import java.security.*;
import java.security.interfaces.*;
import java.sql.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import javax.sql.DataSource;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.parallel.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.support.JdbcTransactionManager;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.support.*;
import org.springframework.mock.web.MockHttpServletResponse;
import org.testcontainers.junit.jupiter.*;
import org.testcontainers.postgresql.PostgreSQLContainer;
import tools.jackson.databind.json.JsonMapper;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

/** Real isolated PG18 + accepted SYSTEM HTTP/security/domain implementations. Never demo provisioning. */
public final class AdminDashboardPostgresCases implements AutoCloseable {
    private final AdminOnboardingPostgresFixture onboarding;
    final UUID REALM;
    static final String ORIGIN="https://admin.example.invalid";
    static String password;
    static final JsonMapper JSON=JsonMapper.builder().build();
    static KeyPair pair;static String hash;
    Database db;DataSource source;JdbcTemplate jdbc;Clock clock=Clock.systemUTC();UUID admin;String username;
    AdminSessionPolicy policy;ProductionAccessAdmission requests;AdminAdmission admission;AdminDashboardService reports;
    ProductionAccessTokens tokens;IdentityCurrentSessionCommands sessions;ProductionAuthenticationHttp auth;MockMvc mvc;
    static synchronized void keys()throws Exception{if(pair!=null)return;var generator=KeyPairGenerator.getInstance("RSA");generator.initialize(3072);pair=generator.generateKeyPair();password=PostgresTestcontainersSupport.password();hash=new BCryptPasswordEncoder(12).encode(password);}
    public AdminDashboardPostgresCases(Database database,UUID realm)throws Exception{
        keys();REALM=realm;onboarding=new AdminOnboardingPostgresFixture(database.container(),realm);db=onboarding.database();assertTrue(db.flyway("40",REALM).validateWithResult().validationSuccessful);
        source=new DriverManagerDataSource(db.url(),db.runtime(),db.runtimePassword());jdbc=new JdbcTemplate(source);admin=account(AdminReadContext.CODES);username=name(admin);compose(true,"TEST");
    }
    String name(UUID id){return jdbc.queryForObject("SELECT username FROM identity.user_account WHERE id=?",String.class,id);}
    public void nwsCarrierDirectoryRequiresCurrentNetworkPermission()throws Exception {
        db.flyway("latest",REALM).migrate();assertTrue(db.flyway("latest",REALM).validateWithResult().validationSuccessful);
        var endpoint=MockMvcBuilders.standaloneSetup(new CarrierNetworkHttp(admission,new JdbcCarrierNetworkReads(source,clock)))
            .setControllerAdvice(new ProductionIdentityErrors()).addFilters(new ProductionReadGuard(tokens,requests,sessions,ORIGIN).browser(auth)).build();
        var allowed=login(admin);String path="/api/v1/admin/carrier-sites";
        var empty=endpoint.perform(get(path).secure(true).header("Authorization","Bearer "+allowed.access())).andReturn().getResponse();assertEquals(200,empty.getStatus());assertEquals(0,JSON.readTree(empty.getContentAsString()).get("total").asInt());
        assertEquals(400,endpoint.perform(get(path+"?type=WAREHOUSE").secure(true).header("Authorization","Bearer "+allowed.access())).andReturn().getResponse().getStatus());
        assertEquals(404,endpoint.perform(get(path+"/"+UUID.randomUUID()).secure(true).header("Authorization","Bearer "+allowed.access())).andReturn().getResponse().getStatus());
        var technical=login(account(Set.of(AdminReadContext.ENTRY,AdminReadContext.TECHNICAL)));
        assertEquals(403,endpoint.perform(get(path).secure(true).header("Authorization","Bearer "+technical.access())).andReturn().getResponse().getStatus());
        assertEquals(401,endpoint.perform(get(path).secure(true)).andReturn().getResponse().getStatus());
    }
    @Override public void close()throws Exception{onboarding.close();}
    UUID account(Set<String> permissions)throws Exception{
        if(permissions.contains(AdminReadContext.ENTRY)&&AdminReadContext.CODES.containsAll(permissions))return onboarding.admin(permissions,password);
        UUID id=UUID.randomUUID();try(var c=db.migrationConnection();var s=c.createStatement()){
            s.execute("SET ROLE "+db.owner());
            try(var q=c.prepareStatement("INSERT INTO identity.user_account(id,username,display_name,email,password_hash,status,email_verified,created_at,updated_at,activated_at) VALUES (?,?,?,? ,?,'ACTIVE',TRUE,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP)")){q.setObject(1,id);q.setString(2,"synthetic-"+id);q.setString(3,"Synthetic Admin");q.setString(4,"synthetic-"+id+"@example.invalid");q.setString(5,hash);q.executeUpdate();}
            for(String permission:permissions)try(var q=c.prepareStatement("INSERT INTO identity.account_grant(account_id,role_code,scope_type,scope_id,granted_at) VALUES (?,?,'IDENTITY',?,CURRENT_TIMESTAMP)")){q.setObject(1,id);q.setString(2,permission);q.setObject(3,REALM);q.executeUpdate();}
        }return id;
    }
    void compose(boolean enabled,String environment){
        policy=new AdminSessionPolicy(source,enabled,environment);requests=new ProductionAccessAdmission(new JdbcAccessSessionTiming(source));
        var store=new JdbcIdentitySessionStore(source,policy);sessions=new IdentityCurrentSessions(store,requests,clock);
        tokens=new ProductionAccessTokens("https://identity.example.invalid","system-browser","synthetic-system",(RSAPrivateKey)pair.getPrivate(),Map.of("synthetic-system",(RSAPublicKey)pair.getPublic()),clock);
        byte[] secret=new byte[32];Arrays.fill(secret,(byte)21);var limiter=new JdbcIdentityRateLimiter(source,secret);var browser=new JdbcIdentityBrowserState(source);
        var sessionAdmission=new ProductionSessionAdmission(browser,limiter);
        auth=new ProductionAuthenticationHttp(new IdentitySessions(store,new ProtectedSessionCredentials(),sessionAdmission,clock),sessionAdmission,browser,limiter,tokens,ORIGIN,clock);
        admission=new JdbcAdminAdmission(source,requests,requests::currentRequestId,policy,clock);reports=new AdminDashboardService(secret);
        var controller=controller(admission);
        mvc=MockMvcBuilders.standaloneSetup(controller,new ProductionIdentityReads(sessions,ProductionIdentityAdministrationComposition.create(source,requests,clock,Duration.ofHours(24)),requests,ProductionIdentityGrantComposition.create(source,clock,Duration.ofHours(24))))
            .setControllerAdvice(new ProductionIdentityErrors()).addFilters(new ProductionReadGuard(tokens,requests,sessions,ORIGIN).browser(auth)).build();
    }
    AdminDashboardHttp controller(AdminAdmission admitted){return new AdminDashboardHttp(admitted,reports,new JdbcAdminShipmentFacts(source),new JdbcAdminObligationFacts(source),new AdminApplicationSource(),new AdminNetworkSource(),new JdbcAdminOversightReads(source),clock);}
    record Browser(String access){@Override public String toString(){return "Synthetic browser[redacted]";}}
    Cookie cookie(MockHttpServletResponse response,String name){return response.getHeaders("Set-Cookie").stream().filter(v->v.startsWith(name+"=")).findFirst().map(v->new Cookie(name,v.substring(name.length()+1,v.indexOf(';')))).orElseThrow();}
    MockHttpServletResponse loginResult(UUID account)throws Exception{
        var csrf=mvc.perform(get("/api/v1/auth/csrf").secure(true)).andReturn().getResponse();assertEquals(200,csrf.getStatus());
        String challenge=JSON.readTree(csrf.getContentAsString()).get("csrfToken").asString();
        return mvc.perform(post("/api/v1/auth/login").secure(true).header("Origin",ORIGIN).header("X-CSRF-Token",challenge).cookie(cookie(csrf,"__Host-elogistics.csrf")).contentType("application/json").content(JSON.writeValueAsString(Map.of("username",name(account),"password",password)))).andReturn().getResponse();
    }
    Browser login(UUID account)throws Exception{var result=loginResult(account);assertEquals(200,result.getStatus(),"SYSTEM_LOGIN_EXPECTED");return new Browser(JSON.readTree(result.getContentAsString()).get("accessToken").asString());}
    MockHttpServletResponse read(Browser browser,String path)throws Exception{return mvc.perform(get(path).secure(true).header("Origin",ORIGIN).header("Authorization","Bearer "+browser.access())).andReturn().getResponse();}
    String query(){LocalDate today=clock.instant().atZone(ZoneId.of("Asia/Ho_Chi_Minh")).toLocalDate();return "from="+today.minusDays(29)+"&to="+today+"&grain=DAY";}
    String metric(MockHttpServletResponse response,String metric)throws Exception{var root=JSON.readTree(response.getContentAsString());for(var section:root.get("sections"))for(var m:section.get("metrics"))if(m.get("key").asString().equals(metric))return m.get("value").asString();throw new AssertionError("METRIC_MISSING");}
    String selector(MockHttpServletResponse response)throws Exception{return JSON.readTree(response.getContentAsString()).get("snapshotId").asString();}
    String listPath(String query,String snapshot,String metric){return "/api/v1/admin/dashboard/shipments?"+query+"&snapshotId="+snapshot+"&metric="+metric+"&page=0&size=20";}
    static byte[] key(){byte[] key=new byte[32];Arrays.fill(key,(byte)17);return key;}
    UUID customer()throws Exception{UUID id=account(Set.of());db.ownerExecute("INSERT INTO identity.customer_identifier VALUES('"+UUID.randomUUID()+"','"+id+"','"+name(id)+"@example.invalid',CURRENT_TIMESTAMP,'ACTIVE',1)");return id;}
    record PreparationAccess(UUID accountId,UUID requestId) implements ShippingDrafts.Access,PilotQuotes.Access,com.elogistic.backend.finance.api.PilotObligations.Access {
        public void requireCurrent(){assertTrue(TransactionSynchronizationManager.isActualTransactionActive());}public void meaningfulAction(){requireCurrent();}
    }
    PilotShipments.Shipment book(UUID customer,int count,Clock time){
        var transaction=new TransactionTemplate(new JdbcTransactionManager(source));
        return transaction.execute(status->{
            var access=new PreparationAccess(customer,UUID.randomUUID());var store=new JdbcShippingDraftStore(source);
            var drafts=new ShippingDraftApplication(store,new ProtectedShippingDraftFingerprints(Map.of("v1",key()),"v1"),time);
            var contact=new ShippingDrafts.Contact("Synthetic private recipient","0901234567",new ShippingDrafts.Address("VN","Synthetic area","Synthetic locality","Synthetic private address"));
            var parcels=new ArrayList<ShippingDrafts.Parcel>();for(int n=0;n<count;n++)parcels.add(new ShippingDrafts.Parcel("Synthetic parcel",1,500,100,100,100,0));
            var draft=drafts.create(access,new ShippingDrafts.Input("Synthetic reference",null,contact,contact,parcels),UUID.randomUUID());
            var quotes=new PilotQuoteApplication(new JdbcPilotQuoteStore(source,Map.of("v1",key()),"v1"),time,true);
            var route=new PilotQuotes.Route("ARGO-SIM-COVERAGE-1","SIM-A1","SIM-A2","INTRA","SIM-DROP-A1");
            var quote=quotes.create(access,new PilotQuotes.Request(draft.id(),draft.version(),route,parcels.stream().map(p->new PilotQuotes.Parcel(p.weightGrams(),p.lengthMm(),p.widthMm(),p.heightMm())).toList(),true),UUID.randomUUID());
            var service=new PilotBookingApplication(store,new JdbcPilotShipmentStore(source),time,true);service.requireBookable(access,draft.id(),draft.version());UUID shipment=UUID.randomUUID();
            var consumed=quotes.consume(access,quote.id(),draft.id(),draft.version(),shipment);var booked=service.confirm(access,draft.id(),draft.version(),quote.id(),UUID.randomUUID(),shipment,consumed);
            new JdbcPilotObligations(source,time,true).recordUnpaid(access,booked.id(),quote.id(),booked.totalVnd());return booked;
        });
    }
    void accept(PilotShipments.Shipment shipment)throws Exception{
        UUID staff=UUID.randomUUID();db.ownerExecute("SET argopost.operator_environment='TEST';SELECT dispatch.configure_pilot_station_staff('"+staff+"','SIM-DROP-A1','GRANT')");
        record Staff(UUID actorId,UUID requestId) implements StationIntake.Access,PilotHandling.CarrierAccess {public String stationCode(){return "SIM-DROP-A1";}public void requireCurrent(){assertTrue(TransactionSynchronizationManager.isActualTransactionActive());}public void meaningfulAction(){requireCurrent();}}
        new TransactionTemplate(new JdbcTransactionManager(source)).execute(status->{var access=new Staff(staff,UUID.randomUUID());var handling=new JdbcPilotHandling(source,true,"TEST");var stations=new JdbcStationIntake(source,clock,true,"TEST");var cargo=handling.atStation(access,shipment.id());var scans=cargo.handling().parcels().stream().map(p->stations.scan(access,"SIM-DROP-A1",cargo,p.barcode(),UUID.randomUUID()).id()).toList();var accepted=stations.accept(access,"SIM-DROP-A1",cargo,scans,0,"MATCHED",UUID.randomUUID());handling.accept(access,shipment.id(),0,accepted.id(),accepted.occurredAt());return null;});
    }
    private AdminCommandAdmission commandAdmission(UUID actor,Clock time)throws Exception{
        return commandAdmission(login(actor),time);
    }
    private AdminCommandAdmission commandAdmission(Browser browser,Clock time){
        var verified=tokens.verify(browser.access());
        Instant created=jdbc.queryForObject("SELECT created_at FROM identity.auth_session WHERE id=?",java.sql.Timestamp.class,verified.sessionId()).toInstant();
        var current=new com.elogistic.backend.identity.internal.application.port.CurrentSessionAdmission.Context(verified.sessionId(),verified.accountId(),verified.authVersion(),created,verified.expiresAt());
        return new JdbcAdminCommandAdmission(source,(operation,request)->current,policy,time);
    }
    public void governanceCommandAdmissionOwnsTransactionAndRollsBack()throws Exception{
        db.ownerExecute("CREATE TABLE public.synthetic_command_probe(id UUID PRIMARY KEY);GRANT USAGE ON SCHEMA public TO "+db.runtime()+";GRANT SELECT,INSERT ON public.synthetic_command_probe TO "+db.runtime());
        assertEquals(0,jdbc.queryForObject("SELECT count(*) FROM public.synthetic_command_probe",Integer.class));
        UUID applicant=customer(),reviewer=account(Set.of(AdminReadContext.ENTRY,AdminReadContext.MERCHANT));
        var commands=commandAdmission(admin,clock);var references=new AdminCommandAdmission.ReferencedSubjects(Set.of(applicant),Set.of(reviewer));UUID committed=UUID.randomUUID();
        assertEquals(committed,commands.execute(UUID.randomUUID(),references,context->{
            context.requireCurrent();assertEquals(admin,context.accountId());assertEquals(REALM,context.realmId());
            assertEquals(Connection.TRANSACTION_READ_COMMITTED,TransactionSynchronizationManager.getCurrentTransactionIsolationLevel());
            assertFalse(TransactionSynchronizationManager.isCurrentTransactionReadOnly());
            assertTrue(context.customer(applicant).eligible());assertNotNull(context.customer(applicant).verifiedEmail());assertTrue(context.admin(reviewer).eligible());
            assertThrows(AdminFailure.class,()->context.customer(reviewer));assertThrows(AdminFailure.class,()->context.admin(applicant));
            assertThrows(AdminFailure.class,()->commands.execute(UUID.randomUUID(),references,nested->null));
            var differentPhysicalTransaction=new TransactionTemplate(new JdbcTransactionManager(source));differentPhysicalTransaction.setPropagationBehavior(org.springframework.transaction.TransactionDefinition.PROPAGATION_REQUIRES_NEW);
            differentPhysicalTransaction.setIsolationLevel(org.springframework.transaction.TransactionDefinition.ISOLATION_READ_COMMITTED);
            assertThrows(AdminFailure.class,()->differentPhysicalTransaction.execute(status->{context.requireCurrent();return null;}));context.requireCurrent();
            jdbc.update("INSERT INTO public.synthetic_command_probe VALUES (?)",committed);return committed;
        }));
        assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM public.synthetic_command_probe",Integer.class));
        assertThrows(IllegalStateException.class,()->commands.execute(UUID.randomUUID(),references,context->{jdbc.update("INSERT INTO public.synthetic_command_probe VALUES (?)",UUID.randomUUID());throw new IllegalStateException("SYNTHETIC_ROLLBACK");}));
        assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM public.synthetic_command_probe",Integer.class));
        new TransactionTemplate(new JdbcTransactionManager(source)).execute(status->{assertThrows(AdminFailure.class,()->commands.execute(UUID.randomUUID(),references,context->null));return null;});
    }
    public void governanceCommandAdmissionRejectsWrongPurposeAndEscapedContexts()throws Exception{
        var references=new AdminCommandAdmission.ReferencedSubjects(Set.of(),Set.of());var commands=commandAdmission(admin,clock);
        AtomicReference<AdminCommandContext> captured=new AtomicReference<>();commands.execute(UUID.randomUUID(),references,context->{captured.set(context);return null;});
        assertThrows(AdminFailure.class,()->captured.get().requireCurrent());
        UUID technical=account(Set.of(AdminReadContext.ENTRY,AdminReadContext.TECHNICAL));
        assertEquals("ACCESS_DENIED",assertThrows(AdminFailure.class,()->commandAdmission(technical,clock).execute(UUID.randomUUID(),references,context->null)).code());
        UUID rootOnly=account(Set.of("IDENTITY_MANAGEMENT"));
        assertEquals("ACCESS_DENIED",assertThrows(AdminFailure.class,()->commandAdmission(rootOnly,clock).execute(UUID.randomUUID(),references,context->null)).code());
        assertThrows(AdminFailure.class,()->commands.execute(null,references,context->null));
        Set<UUID> tooMany=new HashSet<>();for(int n=0;n<101;n++)tooMany.add(UUID.randomUUID());
        assertThrows(AdminFailure.class,()->commands.execute(UUID.randomUUID(),new AdminCommandAdmission.ReferencedSubjects(tooMany,Set.of()),context->null));
        var validBrowser=login(admin);compose(true,"PRODUCTION");assertThrows(AdminFailure.class,()->commandAdmission(validBrowser,clock).execute(UUID.randomUUID(),references,context->null));
    }
    public void governanceCommandAdmissionRejectsRevocationAndExpiryAtCommit()throws Exception{
        var references=new AdminCommandAdmission.ReferencedSubjects(Set.of(),Set.of());
        var now=new AtomicReference<>(clock.instant());Clock moving=new Clock(){public ZoneId getZone(){return ZoneOffset.UTC;}public Clock withZone(ZoneId zone){return this;}public Instant instant(){return now.get();}};
        var commands=commandAdmission(admin,moving);
        now.set(clock.instant());var entered=new java.util.concurrent.atomic.AtomicBoolean();
        assertEquals("SESSION_INVALID",assertThrows(AdminFailure.class,()->commands.execute(UUID.randomUUID(),references,context->{entered.set(true);now.set(now.get().plus(Duration.ofMinutes(16)));return null;})).code());
        assertTrue(entered.get(),"Expiry must be detected after the business callback, not from a stale synthetic clock");
        var fresh=commandAdmission(admin,clock);
        // Callback-level revocation must not escape the final admission check or commit.
        assertThrows(AdminFailure.class,()->fresh.execute(UUID.randomUUID(),references,context->{jdbc.update("UPDATE identity.auth_session SET revoked_at=CURRENT_TIMESTAMP WHERE id=?",context.sessionId());return null;}));
        assertEquals(0,jdbc.queryForObject("SELECT count(*) FROM identity.auth_session WHERE account_id=? AND revoked_at IS NOT NULL",Integer.class,admin));
    }
    public void governanceCommandWaitingForRevocationCannotUseOldAuthority()throws Exception{
        var commands=commandAdmission(admin,clock);var entered=new java.util.concurrent.atomic.AtomicBoolean();
        try(var connection=onboarding.operatorConnection();var statement=connection.createStatement();var pool=Executors.newSingleThreadExecutor()){
            connection.setAutoCommit(false);statement.execute("SELECT pg_advisory_xact_lock(5279420865908921433)");
            statement.execute("SELECT * FROM identity.revoke_local_admin('"+UUID.randomUUID()+"','"+admin+"',2,'Synthetic command admission revoke race')");
            var pending=pool.submit(()->commands.execute(UUID.randomUUID(),new AdminCommandAdmission.ReferencedSubjects(Set.of(),Set.of()),context->{entered.set(true);return null;}));
            long deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(3);boolean waiting=false;
            while(System.nanoTime()<deadline){try(var rows=statement.executeQuery("SELECT EXISTS(SELECT 1 FROM pg_locks WHERE locktype='advisory' AND NOT granted AND database=(SELECT oid FROM pg_database WHERE datname=current_database()))")){waiting=rows.next()&&rows.getBoolean(1);}if(waiting)break;Thread.sleep(10);}
            try{assertTrue(waiting,"Command must reach the shared population lock before revoke commits");}finally{connection.commit();}
            var failure=assertThrows(ExecutionException.class,()->pending.get(5,TimeUnit.SECONDS));
            assertInstanceOf(AdminFailure.class,failure.getCause());assertEquals("ACCESS_DENIED",((AdminFailure)failure.getCause()).code());assertFalse(entered.get());
        }
    }
    public void governanceV41SchemaAndAclAreForwardOnly()throws Exception{
        var before=Arrays.stream(db.flyway("40",REALM).info().applied()).map(i->i.getChecksum()).toList();
        assertEquals(1,db.flyway("41",REALM).migrate().migrationsExecuted);db.flyway("41",REALM).validate();assertEquals(0,db.flyway("41",REALM).migrate().migrationsExecuted);
        assertEquals(before,Arrays.stream(db.flyway("41",REALM).info().applied()).limit(39).map(i->i.getChecksum()).toList());
        for(String table:List.of("admin_governance_reauthentication","business_contact_attestation","governance_proof_receipt")){
            String name="identity."+table;assertEquals(0,jdbc.queryForObject("SELECT count(*) FROM "+name,Integer.class));
            assertTrue(jdbc.queryForObject("SELECT has_table_privilege(current_user,?,'SELECT')",Boolean.class,name));
            assertFalse(jdbc.queryForObject("SELECT has_table_privilege(current_user,?,'DELETE') OR has_table_privilege(current_user,?,'TRUNCATE')",Boolean.class,name,name));
        }
        assertFalse(jdbc.queryForObject("SELECT has_table_privilege(current_user,'identity.governance_proof_receipt','UPDATE')",Boolean.class));
        assertFalse(jdbc.queryForObject("SELECT has_function_privilege(current_user,'identity.admin_governance_command_bound(uuid,uuid,uuid,text,text,uuid,bigint,bytea)','EXECUTE')",Boolean.class));
        assertTrue(jdbc.queryForObject("SELECT count(*)=2 AND bool_and(NOT rolcanlogin AND NOT rolsuper AND NOT rolcreatedb AND NOT rolcreaterole AND NOT rolreplication AND NOT rolbypassrls) FROM pg_roles WHERE rolname IN ('merchant_governance_operator','merchant_governance_writer')",Boolean.class));
        assertFalse(jdbc.queryForObject("SELECT pg_has_role(current_user,'merchant_governance_writer','MEMBER') OR pg_has_role(current_user,'merchant_governance_operator','MEMBER')",Boolean.class));
    }
    public void businessContactSmtpClearingAndAtomicConsume()throws Exception{
        db.flyway("41",REALM).migrate();new BusinessContactPostgresCases(source,customer(),password,clock).smtpClearingConfirmationAndAtomicConsume();
    }
    public void businessContactContextReplayAndLimits()throws Exception{
        db.flyway("41",REALM).migrate();new BusinessContactPostgresCases(source,customer(),password,clock).contextReplayResendAndLimitsFailClosed();
    }
    public void businessContactWrongBindingExpiryAndTamper()throws Exception{
        db.flyway("41",REALM).migrate();new BusinessContactPostgresCases(source,customer(),password,clock).wrongBindingExpiryAndTamperDeny();
    }
    public void businessContactSuspendedTransactionAndRevocation()throws Exception{
        db.flyway("41",REALM).migrate();new BusinessContactPostgresCases(source,customer(),password,clock).suspendedTransactionAndRevokedSessionDeny();
    }
    public void businessContactConcurrentConfirmationAndReceiptBinding()throws Exception{
        db.flyway("41",REALM).migrate();new BusinessContactPostgresCases(source,customer(),password,clock).concurrentConfirmationAndReceiptBindingDmlDeny();
    }
    public void businessContactWrongSubjectAndSession()throws Exception{
        db.flyway("41",REALM).migrate();new BusinessContactPostgresCases(source,customer(),password,clock).wrongSubjectAndSessionDeny(customer(),password);
    }
    public void businessContactActualCustomerHttpAndOwnership()throws Exception{
        db.flyway("41",REALM).migrate();new BusinessContactPostgresCases(source,customer(),password,clock).httpOwnedDraftAndRealProof(customer(),password);
    }
    public void merchantGovernanceV42SchemaAndOperatorReceipts()throws Exception{
        db.flyway("41",REALM).migrate();var prior=Arrays.stream(db.flyway("41",REALM).info().applied()).map(i->i.getChecksum()).toList();
        assertEquals(1,db.flyway("42",REALM).migrate().migrationsExecuted);db.flyway("42",REALM).validate();
        assertEquals(prior,Arrays.stream(db.flyway("42",REALM).info().applied()).limit(40).map(i->i.getChecksum()).toList());
        for(String table:List.of("application_policy","business_reviewer_scope","governance_event","governance_receipt","application_revision","application_evidence","application_evidence_reservation","application_revision_evidence","application_reviewer_assignment"))assertNotNull(jdbc.queryForObject("SELECT to_regclass(?)",String.class,"merchant."+table));
        assertFalse(jdbc.queryForObject("SELECT has_table_privilege(current_user,'merchant.application_policy','INSERT') OR has_table_privilege(current_user,'merchant.business_reviewer_scope','UPDATE')",Boolean.class));
        assertFalse(jdbc.queryForObject("SELECT has_function_privilege(current_user,'merchant.publish_local_application_policy(uuid,uuid,bigint,jsonb,text)','EXECUTE')",Boolean.class));
        new MerchantGovernancePostgresCases(db,source,onboarding,REALM,admin).operatorReceiptsAndCeilings();
    }
    public void merchantGovernanceCurrentScopeAndRevocation()throws Exception{
        db.flyway("42",REALM).migrate();var helper=new MerchantGovernancePostgresCases(db,source,onboarding,REALM,admin);
        UUID policyId=UUID.randomUUID();String body=MerchantGovernancePostgresCases.policy(clock.instant().minusSeconds(30).truncatedTo(java.time.temporal.ChronoUnit.MICROS));
        Instant expiry=clock.instant().plusSeconds(3600).truncatedTo(java.time.temporal.ChronoUnit.MICROS);
        var target=new com.elogistic.backend.merchant.api.MerchantGovernance.Target(com.elogistic.backend.merchant.api.MerchantGovernance.Kind.POLICY,policyId);
        var domain=new com.elogistic.backend.merchant.internal.infrastructure.JdbcMerchantGovernance(source,clock,readAdmission(admin),commandAdmission(admin,clock));
        var commands=commandAdmission(admin,clock);var refs=new AdminCommandAdmission.ReferencedSubjects(Set.of(),Set.of());
        try(var operator=onboarding.governanceOperatorConnection()){
            helper.publish(operator,UUID.randomUUID(),policyId,body);
            assertThrows(com.elogistic.backend.merchant.api.GovernanceFailure.class,()->commands.execute(UUID.randomUUID(),refs,c->domain.withScope(c,target,scope->scope.version())));
            helper.scope(operator,UUID.randomUUID(),policyId,"ISSUE",0,expiry,"application.read","application.review","application.evidence.read");
            AtomicReference<com.elogistic.backend.merchant.api.MerchantGovernance.GovernanceScope> captured=new AtomicReference<>();
            String version=commands.execute(UUID.randomUUID(),refs,c->domain.withScope(c,target,scope->{
                captured.set(scope);scope.require("application.review",target);
                assertThrows(com.elogistic.backend.merchant.api.GovernanceFailure.class,()->scope.require("application.decide",target));
                assertThrows(com.elogistic.backend.merchant.api.GovernanceFailure.class,()->scope.require("application.read",new com.elogistic.backend.merchant.api.MerchantGovernance.Target(target.kind(),UUID.randomUUID())));
                var isolated=new TransactionTemplate(new JdbcTransactionManager(source));isolated.setPropagationBehavior(org.springframework.transaction.TransactionDefinition.PROPAGATION_REQUIRES_NEW);
                assertThrows(AdminFailure.class,()->isolated.execute(s->{scope.require("application.read",target);return null;}));
                return scope.version();
            }));assertEquals(64,version.length());assertThrows(AdminFailure.class,()->captured.get().version());
            var expired=new com.elogistic.backend.merchant.internal.infrastructure.JdbcMerchantGovernance(source,Clock.fixed(expiry,ZoneOffset.UTC),readAdmission(admin),commandAdmission(admin,clock));
            assertThrows(com.elogistic.backend.merchant.api.GovernanceFailure.class,()->commands.execute(UUID.randomUUID(),refs,c->expired.withScope(c,target,scope->scope.version())));
            helper.scope(operator,UUID.randomUUID(),policyId,"REVOKE",1,expiry,"application.read","application.review","application.evidence.read");
            assertThrows(com.elogistic.backend.merchant.api.GovernanceFailure.class,()->commands.execute(UUID.randomUUID(),refs,c->domain.withScope(c,target,scope->scope.version())));
            helper.scope(operator,UUID.randomUUID(),policyId,"ISSUE",0,expiry,"application.read");
            String next=commands.execute(UUID.randomUUID(),refs,c->domain.withScope(c,target,scope->scope.version()));assertNotEquals(version,next);
            helper.publish(operator,UUID.randomUUID(),policyId,body.replace("PUBLISHED","RETIRED"));
            assertThrows(com.elogistic.backend.merchant.api.GovernanceFailure.class,()->commands.execute(UUID.randomUUID(),refs,c->domain.withScope(c,target,scope->scope.version())));
        }
    }
    public void merchantWorkflowV42DraftCompatibilityAndDenial()throws Exception{
        db.flyway("42",REALM).migrate();new BusinessContactPostgresCases(source,customer(),password,clock).workflowDraftCompatibilityAndDenial();
    }
    public void merchantV42ScreenedEvidenceAndImmutableSubmission()throws Exception{
        db.flyway("42",REALM).migrate();var helper=new MerchantGovernancePostgresCases(db,source,onboarding,REALM,admin);
        try(var operator=onboarding.governanceOperatorConnection()){helper.publish(operator,UUID.randomUUID(),UUID.randomUUID(),MerchantGovernancePostgresCases.policy(clock.instant().minusSeconds(30).truncatedTo(java.time.temporal.ChronoUnit.MICROS)));}
        new BusinessContactPostgresCases(source,customer(),password,clock).screenedEvidenceAndImmutableSubmission();
    }
    public void merchantV42HttpEvidenceAndSubmissionNegatives()throws Exception{
        db.flyway("42",REALM).migrate();var helper=new MerchantGovernancePostgresCases(db,source,onboarding,REALM,admin);
        try(var operator=onboarding.governanceOperatorConnection()){helper.publish(operator,UUID.randomUUID(),UUID.randomUUID(),MerchantGovernancePostgresCases.policy(clock.instant().minusSeconds(30).truncatedTo(java.time.temporal.ChronoUnit.MICROS)));}
        new BusinessContactPostgresCases(source,customer(),password,clock).httpEvidenceOwnershipCsrfAndStrictSubmission(customer(),password);
    }
    private AdminAdmission readAdmission(UUID actor)throws Exception{
        var browser=login(actor);var verified=tokens.verify(browser.access());Instant created=jdbc.queryForObject("SELECT created_at FROM identity.auth_session WHERE id=?",java.sql.Timestamp.class,verified.sessionId()).toInstant();
        var claim=new com.elogistic.backend.identity.internal.application.port.CurrentSessionAdmission.Context(verified.sessionId(),verified.accountId(),verified.authVersion(),created,verified.expiresAt());
        return new JdbcAdminAdmission(source,(op,request)->claim,UUID::randomUUID,policy,clock);
    }
    public void merchantV42IndependentAssignmentReviewAndInformationRequest()throws Exception{merchantDecisionFlow(false);}
    public void merchantV43AtomicApprovalAndBoundNotices()throws Exception{merchantDecisionFlow(true);}
    private static com.elogistic.backend.merchant.api.MerchantGovernance.ApplicationSummary applyDecision(com.elogistic.backend.merchant.api.MerchantGovernance governance,AdminCommandContext actor,UUID app,com.elogistic.backend.merchant.api.MerchantGovernance.DecisionInput input,long version,UUID key,boolean approval){return approval?governance.approve(actor,app,input,version,key):governance.reject(actor,app,input,version,key);}
    private void merchantDecisionFlow(boolean approval)throws Exception{
        final String operation=approval?"APPROVE":"REJECT", decisionPath=approval?"/approval":"/rejection";
        var governanceProofs=stepUps();db.flyway(approval?"43":"42",REALM).migrate();UUID policyId=UUID.randomUUID();var helper=new MerchantGovernancePostgresCases(db,source,onboarding,REALM,admin);
        try(var operator=onboarding.governanceOperatorConnection()){helper.publish(operator,UUID.randomUUID(),policyId,MerchantGovernancePostgresCases.policy(clock.instant().minusSeconds(30).truncatedTo(java.time.temporal.ChronoUnit.MICROS)));}
        UUID applicant=customer(),reviewer=account(Set.of(AdminReadContext.ENTRY,AdminReadContext.MERCHANT));
        var applicantWorkflow=new BusinessContactPostgresCases(source,applicant,password,clock);UUID app=applicantWorkflow.screenedEvidenceAndImmutableSubmission();
        Instant expiry=clock.instant().plusSeconds(3600).truncatedTo(java.time.temporal.ChronoUnit.MICROS);
        try(var operator=onboarding.governanceOperatorConnection()){
            helper.scope(operator,UUID.randomUUID(),policyId,"ISSUE",0,expiry,"application.read","application.assign","application.review","audit.read");
            new MerchantGovernancePostgresCases(db,source,onboarding,REALM,reviewer).scope(operator,UUID.randomUUID(),policyId,"ISSUE",0,expiry,"application.read","application.review","application.evidence.read");
        }
        byte[] encryption=new byte[32];Arrays.fill(encryption,(byte)72);var protectedPayloads=new com.elogistic.backend.merchant.internal.infrastructure.ProtectedGovernancePayloads(Map.of("e1",encryption),"e1");
        var evidenceHook=new java.util.concurrent.atomic.AtomicReference<Runnable>();
        var evidenceStore=new com.elogistic.backend.platform.api.PrivateEvidenceStore(){
            public Blob put(java.io.InputStream input){throw new Unavailable();}
            public Content read(Blob expected){assertFalse(org.springframework.transaction.support.TransactionSynchronizationManager.isActualTransactionActive(),"NO_DB_TRANSACTION_DURING_PRIVATE_IO");var hook=evidenceHook.getAndSet(null);if(hook!=null)hook.run();return applicantWorkflow.evidenceStore().read(expected);}
        };
        var mailCalls=new java.util.concurrent.atomic.AtomicInteger();var retryNext=new java.util.concurrent.atomic.AtomicBoolean();var forceTransient=new java.util.concurrent.atomic.AtomicBoolean();var mailIds=java.util.Collections.synchronizedList(new ArrayList<UUID>());
        com.elogistic.backend.platform.api.MerchantNoticeMail testMail=message->{assertFalse(org.springframework.transaction.support.TransactionSynchronizationManager.isActualTransactionActive(),"NO_SMTP_IN_BUSINESS_TRANSACTION");mailCalls.incrementAndGet();mailIds.add(message.id());return forceTransient.get()||retryNext.getAndSet(false)?new com.elogistic.backend.platform.api.MerchantNoticeMail.Outcome(com.elogistic.backend.platform.api.MerchantNoticeMail.Kind.TRANSIENT_FAILURE,null,true):new com.elogistic.backend.platform.api.MerchantNoticeMail.Outcome(com.elogistic.backend.platform.api.MerchantNoticeMail.Kind.ACCEPTED_BY_PROVIDER,"TEST:"+message.id(),true);};
        var noticeCommands=new JdbcAdminCommandAdmission(source,requests,policy,clock);
        var noticeWorker=new com.elogistic.backend.notifications.internal.infrastructure.JdbcMerchantApplicationNotices(source,applicantWorkflow.customerAdmission(),noticeCommands,new com.elogistic.backend.notifications.internal.infrastructure.ProtectedMerchantNoticeRecipient(Map.of("n1",encryption),"n1"),testMail,clock);
        var governance=new com.elogistic.backend.merchant.internal.infrastructure.JdbcMerchantApplicationReviews(source,new com.elogistic.backend.merchant.internal.infrastructure.ProtectedMerchantFingerprints(Map.of("v1",key()),"v1"),protectedPayloads,clock,admission,new JdbcAdminCommandAdmission(source,requests,policy,clock),applicantWorkflow.contactFacts(),governanceProofs,evidenceStore);
        var noticeEvents=new com.elogistic.backend.merchant.internal.infrastructure.JdbcMerchantApplicationNoticeEvents(source,applicantWorkflow.customerAdmission(),noticeCommands,governance);
        var noticeComposition=new com.elogistic.backend.workflow.internal.application.MerchantNoticeComposition(noticeEvents,noticeWorker);

        var overviewReads=readAdmission(admin);var ownContext=overviewReads.execute(governance::context);assertEquals(admin,ownContext.accountId());assertEquals(1,ownContext.assignments().size());assertEquals(policyId,ownContext.assignments().getFirst().policyId());
        var query=new com.elogistic.backend.merchant.api.MerchantGovernance.ApplicationQuery(clock.instant().atZone(ZoneId.of("Asia/Ho_Chi_Minh")).toLocalDate().minusDays(1),clock.instant().atZone(ZoneId.of("Asia/Ho_Chi_Minh")).toLocalDate(),"SUBMITTED",null,policyId,0,20);
        var list=overviewReads.execute(c->governance.applications(c,query));assertEquals(1,list.items().size());assertEquals(app,list.items().getFirst().id());assertFalse(list.hasNext());
        var revisions=overviewReads.execute(c->governance.revisions(c,app,0,20));assertEquals(1,revisions.items().size());assertNotNull(revisions.items().getFirst().contactVerifiedAt());assertEquals(2,revisions.items().getFirst().evidenceIds().size());
        var reads=readAdmission(admin);var references=reads.execute(c->governance.subjects(c,app,reviewer));assertEquals(Set.of(applicant),references.customers());assertTrue(references.admins().contains(reviewer));
        var commands=commandAdmission(admin,clock);UUID assignKey=UUID.randomUUID();var assignmentInput=new com.elogistic.backend.merchant.api.MerchantGovernance.ReviewerAssignmentInput(reviewer,"Synthetic independent assignment");
        var assigned=commands.execute(UUID.randomUUID(),references,c->governance.assign(c,app,assignmentInput,3,assignKey));assertEquals(4,assigned.version());assertEquals("SUBMITTED",assigned.state());assertEquals(reviewer,assigned.reviewerId());
        assertEquals(assigned,commands.execute(UUID.randomUUID(),references,c->governance.assign(c,app,assignmentInput,3,assignKey)));
        assertThrows(com.elogistic.backend.merchant.api.GovernanceFailure.class,()->commands.execute(UUID.randomUUID(),references,c->governance.reviewData(c,app,"MERCHANT_REVIEW")));
        var disclosureCommands=commandAdmission(reviewer,clock);var disclosed=disclosureCommands.execute(UUID.randomUUID(),references,c->governance.reviewData(c,app,"MERCHANT_REVIEW"));
        assertEquals(2,disclosed.evidence().size());assertEquals(1,disclosed.application().revision());assertNotNull(disclosed.contactVerifiedAt());assertEquals(2,disclosed.submission().evidenceIds().size());
        assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM merchant.governance_event WHERE application_id=? AND operation='READ_DISCLOSURE'",Integer.class,app));
        assertThrows(com.elogistic.backend.merchant.api.GovernanceFailure.class,()->disclosureCommands.execute(UUID.randomUUID(),references,c->governance.reviewData(c,app,"OTHER_PURPOSE")));
        var beginInput=new com.elogistic.backend.merchant.api.MerchantGovernance.ReviewInput("BEGIN",1,"1",null,"Synthetic review begin");
        assertThrows(com.elogistic.backend.merchant.api.GovernanceFailure.class,()->commands.execute(UUID.randomUUID(),references,c->governance.review(c,app,beginInput,4,UUID.randomUUID())));
        var reviewerReads=readAdmission(reviewer);var reviewerRefs=reviewerReads.execute(c->governance.subjects(c,app,null));var reviewerCommands=commandAdmission(reviewer,clock);UUID beginKey=UUID.randomUUID();
        var begun=reviewerCommands.execute(UUID.randomUUID(),reviewerRefs,c->governance.review(c,app,beginInput,4,beginKey));assertEquals(5,begun.version());assertEquals("UNDER_REVIEW",begun.state());
        var info=new com.elogistic.backend.merchant.api.MerchantGovernance.InformationRequest(1,List.of("registeredAddress","REGISTRATION"),"Synthetic private clarification");UUID infoKey=UUID.randomUUID();
        var requested=reviewerCommands.execute(UUID.randomUUID(),reviewerRefs,c->{var result=governance.requestInformation(c,app,info,5,infoKey);
            if(approval){
                var actual=c.customer(applicant);var constructed=new com.elogistic.backend.identity.api.customer.CustomerAdmission.Subject(actual.accountId(),actual.displayName(),actual.verifiedEmail(),actual.securityVersion(),actual.eligible());
                var event=noticeEvents.commandEvent(c,app,"REQUEST_INFORMATION",infoKey).orElseThrow();
                assertThrows(IllegalStateException.class,()->noticeWorker.enqueue(new com.elogistic.backend.notifications.api.MerchantApplicationNotices.Access(null,c),new com.elogistic.backend.notifications.api.MerchantApplicationNotices.ApplicationEvent(event.eventId(),app,applicant,event.template()),constructed));
                assertThrows(IllegalStateException.class,noticeWorker::runOne,"WORKER_MUST_REFUSE_BUSINESS_TRANSACTION");
                assertThrows(com.elogistic.backend.merchant.api.GovernanceFailure.class,()->noticeEvents.commandEvent(c,app,"WITHDRAW",infoKey));
                assertThrows(com.elogistic.backend.merchant.api.GovernanceFailure.class,()->noticeEvents.commandEvent(c,UUID.randomUUID(),"REQUEST_INFORMATION",infoKey));
                for(var wrong:List.of(new com.elogistic.backend.notifications.api.MerchantApplicationNotices.ApplicationEvent(UUID.randomUUID(),app,applicant,"REQUEST_INFORMATION"),new com.elogistic.backend.notifications.api.MerchantApplicationNotices.ApplicationEvent(event.eventId(),UUID.randomUUID(),applicant,"REQUEST_INFORMATION"),new com.elogistic.backend.notifications.api.MerchantApplicationNotices.ApplicationEvent(event.eventId(),app,applicant,"APPROVED"))){
                    var connection=org.springframework.jdbc.datasource.DataSourceUtils.getConnection(source);java.sql.Savepoint save;
                    try{save=connection.setSavepoint();try{assertThrows(org.springframework.dao.DataAccessException.class,()->noticeWorker.enqueue(new com.elogistic.backend.notifications.api.MerchantApplicationNotices.Access(null,c),wrong,actual));}finally{connection.rollback(save);connection.releaseSavepoint(save);}}catch(java.sql.SQLException failure){throw new AssertionError(failure);}
                }
                var fake=(AdminCommandContext)java.lang.reflect.Proxy.newProxyInstance(AdminCommandContext.class.getClassLoader(),new Class<?>[]{AdminCommandContext.class},(proxy,method,args)->method.invoke(c,args));
                assertThrows(AdminFailure.class,()->noticeEvents.commandEvent(fake,app,"REQUEST_INFORMATION",infoKey));
                noticeComposition.after(c,app,"REQUEST_INFORMATION",infoKey);noticeComposition.after(c,app,"REQUEST_INFORMATION",infoKey);
            }return result;});assertEquals(6,requested.version());assertEquals("NEEDS_INFORMATION",requested.state());
        assertEquals(requested,reviewerCommands.execute(UUID.randomUUID(),reviewerRefs,c->governance.requestInformation(c,app,info,5,infoKey)));
        assertEquals(begun,reviewerCommands.execute(UUID.randomUUID(),reviewerRefs,c->governance.review(c,app,beginInput,4,beginKey)),"REPLAY_RETURNS_ORIGINAL_VERSION_AND_STATE");
        assertEquals(assigned,commands.execute(UUID.randomUUID(),references,c->governance.assign(c,app,assignmentInput,3,assignKey)));
        assertEquals("NEEDS_INFORMATION",jdbc.queryForObject("SELECT state FROM merchant.application WHERE id=?",String.class,app));
        assertEquals(6,jdbc.queryForObject("SELECT count(*) FROM merchant.application_draft_version WHERE application_id=?",Integer.class,app));
        assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM merchant.application_revision WHERE application_id=?",Integer.class,app));
        assertEquals(3,jdbc.queryForObject("SELECT count(*) FROM merchant.governance_receipt WHERE actor_kind='SYSTEM' AND target_id=?",Integer.class,app));
        var event=jdbc.queryForMap("SELECT id,protected_payload FROM merchant.governance_event WHERE application_id=? AND operation='REQUEST_INFORMATION'",app);
        byte[] ciphertext=(byte[])event.get("protected_payload");assertFalse(new String(ciphertext,java.nio.charset.StandardCharsets.ISO_8859_1).contains(info.reason()));
        assertTrue(protectedPayloads.open((UUID)event.get("id"),app,1,"REQUEST_INFORMATION",ciphertext).contains(info.reason()));
        assertThrows(org.springframework.dao.DataAccessException.class,()->jdbc.update("UPDATE merchant.application_reviewer_assignment SET reviewer_account_id=? WHERE application_id=?",admin,app));
        var governanceMvc=MockMvcBuilders.standaloneSetup(new MerchantGovernanceHttp(admission,new JdbcAdminCommandAdmission(source,requests,policy,clock),governance,approval?noticeComposition:null),new MerchantGovernanceDecisionHttp(admission,new JdbcAdminCommandAdmission(source,requests,policy,clock),governance,governanceProofs,approval?noticeComposition:null),new MerchantOrganizationGovernanceHttp(admission,new JdbcAdminCommandAdmission(source,requests,policy,clock),governance)).setControllerAdvice(new ProductionIdentityErrors()).addFilters(new ProductionReadGuard(tokens,requests,sessions,ORIGIN).browser(auth)).build();
        var ownerBrowser=login(admin);var reviewerBrowser=login(reviewer);String route="/api/v1/admin/merchant-applications/"+app;
        UUID evidenceId=jdbc.queryForObject("SELECT evidence_id FROM merchant.application_revision_evidence WHERE application_id=? ORDER BY evidence_id LIMIT 1",UUID.class,app);String evidenceRoute=route+"/evidence/"+evidenceId+"/content?purpose=MERCHANT_REVIEW";
        var evidenceHttp=governanceMvc.perform(get(evidenceRoute).secure(true).header("Authorization","Bearer "+reviewerBrowser.access())).andReturn().getResponse();assertEquals(200,evidenceHttp.getStatus());assertEquals("image/png",evidenceHttp.getContentType());assertEquals("no-store",evidenceHttp.getHeader("Cache-Control"));assertTrue(evidenceHttp.getHeader("Content-Disposition").startsWith("attachment"));assertNotNull(javax.imageio.ImageIO.read(new java.io.ByteArrayInputStream(evidenceHttp.getContentAsByteArray())));
        assertEquals(403,governanceMvc.perform(get(evidenceRoute).secure(true).header("Authorization","Bearer "+ownerBrowser.access())).andReturn().getResponse().getStatus());
        assertEquals(400,governanceMvc.perform(get(evidenceRoute.replace("MERCHANT_REVIEW","OTHER")).secure(true).header("Authorization","Bearer "+reviewerBrowser.access())).andReturn().getResponse().getStatus());
        assertEquals(404,governanceMvc.perform(get(evidenceRoute.replace(evidenceId.toString(),UUID.randomUUID().toString())).secure(true).header("Authorization","Bearer "+reviewerBrowser.access())).andReturn().getResponse().getStatus());
        String date=clock.instant().atZone(ZoneId.of("Asia/Ho_Chi_Minh")).toLocalDate().toString();
        var listHttp=governanceMvc.perform(get("/api/v1/admin/merchant-applications?from="+date+"&to="+date).secure(true).header("Authorization","Bearer "+ownerBrowser.access())).andReturn().getResponse();assertEquals(200,listHttp.getStatus());assertEquals(1,JSON.readTree(listHttp.getContentAsString()).get("items").size());
        assertEquals(200,governanceMvc.perform(get("/api/v1/admin/merchant-governance/context").secure(true).header("Authorization","Bearer "+ownerBrowser.access())).andReturn().getResponse().getStatus());
        var candidatesHttp=governanceMvc.perform(get("/api/v1/admin/merchant-governance/reviewer-candidates?policyId="+policyId).secure(true).header("Authorization","Bearer "+ownerBrowser.access())).andReturn().getResponse();assertEquals(200,candidatesHttp.getStatus());var candidateItems=JSON.readTree(candidatesHttp.getContentAsString()).get("items");assertEquals(2,candidateItems.size());Set<String> candidateIds=new HashSet<>();for(var item:candidateItems)candidateIds.add(item.get("accountId").asString());assertEquals(Set.of(admin.toString(),reviewer.toString()),candidateIds);
        Set<String> pagedCandidateIds=new HashSet<>();for(int candidatePage=0;candidatePage<2;candidatePage++){var response=governanceMvc.perform(get("/api/v1/admin/merchant-governance/reviewer-candidates?policyId="+policyId+"&size=1&page="+candidatePage).secure(true).header("Authorization","Bearer "+ownerBrowser.access())).andReturn().getResponse();assertEquals(200,response.getStatus());var parsed=JSON.readTree(response.getContentAsString());assertEquals(1,parsed.get("items").size());assertEquals(candidatePage==0,parsed.get("hasNext").asBoolean());assertTrue(pagedCandidateIds.add(parsed.get("items").get(0).get("accountId").asString()));}assertEquals(candidateIds,pagedCandidateIds);
        assertEquals(403,governanceMvc.perform(get("/api/v1/admin/merchant-governance/reviewer-candidates?policyId="+policyId).secure(true).header("Authorization","Bearer "+reviewerBrowser.access())).andReturn().getResponse().getStatus());
        assertEquals(200,governanceMvc.perform(get(route+"/revisions").secure(true).header("Authorization","Bearer "+ownerBrowser.access())).andReturn().getResponse().getStatus());
        var safe=governanceMvc.perform(get(route).secure(true).header("Authorization","Bearer "+ownerBrowser.access())).andReturn().getResponse();assertEquals(200,safe.getStatus());assertEquals("no-store",safe.getHeader("Cache-Control"));assertFalse(safe.getContentAsString().contains("business-contact@example.invalid"));
        assertEquals(403,governanceMvc.perform(get(route+"/review-data?purpose=MERCHANT_REVIEW").secure(true).header("Authorization","Bearer "+ownerBrowser.access())).andReturn().getResponse().getStatus());
        var full=governanceMvc.perform(get(route+"/review-data?purpose=MERCHANT_REVIEW").secure(true).header("Authorization","Bearer "+reviewerBrowser.access())).andReturn().getResponse();assertEquals(200,full.getStatus());assertTrue(JSON.readTree(full.getContentAsString()).get("submission").isObject());
        String infoJson=JSON.writeValueAsString(Map.of("revision",1,"fieldCodes",List.of("registeredAddress"),"reason","Synthetic HTTP stale version"));
        assertEquals(428,governanceMvc.perform(post(route+"/request-information").secure(true).header("Origin",ORIGIN).header("Authorization","Bearer "+reviewerBrowser.access()).header("Idempotency-Key",UUID.randomUUID()).contentType("application/json").content(infoJson)).andReturn().getResponse().getStatus());
        assertEquals(412,governanceMvc.perform(post(route+"/request-information").secure(true).header("Origin",ORIGIN).header("Authorization","Bearer "+reviewerBrowser.access()).header("Idempotency-Key",UUID.randomUUID()).header("If-Match","\"v1\"").contentType("application/json").content(infoJson)).andReturn().getResponse().getStatus());
        assertEquals(401,governanceMvc.perform(post(route+"/request-information").secure(true).header("Authorization","Bearer "+reviewerBrowser.access()).header("Idempotency-Key",UUID.randomUUID()).header("If-Match","\"v6\"").contentType("application/json").content(infoJson)).andReturn().getResponse().getStatus());
        assertEquals(400,governanceMvc.perform(get(route+"?actorId="+admin).secure(true).header("Authorization","Bearer "+reviewerBrowser.access())).andReturn().getResponse().getStatus());
        applicantWorkflow.resubmitWithFreshContactFact(app);
        var revision2Begin=new com.elogistic.backend.merchant.api.MerchantGovernance.ReviewInput("BEGIN",2,"1",null,"Synthetic second review");
        var restarted=reviewerCommands.execute(UUID.randomUUID(),reviewerRefs,c->governance.review(c,app,revision2Begin,8,UUID.randomUUID()));assertEquals(9,restarted.version());
        var incomplete=new com.elogistic.backend.merchant.api.MerchantGovernance.ReviewInput("READY_FOR_DECISION",2,"1",new com.elogistic.backend.merchant.api.MerchantGovernance.Checklist(true,true,false,true,true),"Synthetic incomplete checklist");
        assertThrows(com.elogistic.backend.merchant.api.GovernanceFailure.class,()->reviewerCommands.execute(UUID.randomUUID(),reviewerRefs,c->governance.review(c,app,incomplete,9,UUID.randomUUID())));
        var complete=new com.elogistic.backend.merchant.api.MerchantGovernance.ReviewInput("READY_FOR_DECISION",2,"1",new com.elogistic.backend.merchant.api.MerchantGovernance.Checklist(true,true,true,true,true),"Synthetic complete checklist");UUID readyKey=UUID.randomUUID();
        com.elogistic.backend.merchant.api.MerchantGovernance.ApplicationSummary ready;
        try(var pool=java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor()){
            var barrier=new java.util.concurrent.CyclicBarrier(2);java.util.concurrent.Callable<com.elogistic.backend.merchant.api.MerchantGovernance.ApplicationSummary> command=()->{barrier.await(10,java.util.concurrent.TimeUnit.SECONDS);return reviewerCommands.execute(UUID.randomUUID(),reviewerRefs,c->governance.review(c,app,complete,9,readyKey));};
            var first=pool.submit(command);var second=pool.submit(command);ready=first.get(30,java.util.concurrent.TimeUnit.SECONDS);assertEquals(ready,second.get(30,java.util.concurrent.TimeUnit.SECONDS),"CONCURRENT_SAME_KEY_REVIEW_HAS_ONE_COMMITTED_OUTCOME");
        }
        assertEquals(10,ready.version());assertEquals("UNDER_REVIEW",ready.state());
        assertEquals(ready,reviewerCommands.execute(UUID.randomUUID(),reviewerRefs,c->governance.review(c,app,complete,9,readyKey)));
        assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM merchant.governance_event WHERE application_id=? AND operation='READY_FOR_DECISION' AND revision=2",Integer.class,app));
        var decisionReason="Synthetic independent rejection";
        var digestInput=new TreeMap<String,Object>();digestInput.put("operation",operation);digestInput.put("target",app.toString());digestInput.put("originalVersion",10L);digestInput.put("revision",2L);digestInput.put("policy","1");digestInput.put("reason",decisionReason);
        String commandHash=HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(JSON.writeValueAsString(digestInput).getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        UUID decider=account(Set.of(AdminReadContext.ENTRY,AdminReadContext.MERCHANT));
        try(var operator=onboarding.governanceOperatorConnection()){new MerchantGovernancePostgresCases(db,source,onboarding,REALM,decider).scope(operator,UUID.randomUUID(),policyId,"ISSUE",0,expiry,approval?new String[]{"application.read","application.decide","org.read","org.suspend","org.reinstate"}:new String[]{"application.read","application.decide"});}
        var deciderBrowser=login(decider);var deciderCommands=commandAdmission(deciderBrowser,clock);var deciderReads=readAdmission(decider);var decisionRefs=deciderReads.execute(c->governance.subjects(c,app,null));
        UUID stepKey=UUID.randomUUID();String stepBody=JSON.writeValueAsString(Map.of("operation",operation,"targetId",app,"expectedVersion",10,"commandDigest",commandHash,"password",password));
        assertEquals(403,governanceMvc.perform(post("/api/v1/admin/governance-reauthentications").secure(true).header("Origin",ORIGIN).header("Authorization","Bearer "+reviewerBrowser.access()).header("Idempotency-Key",UUID.randomUUID()).contentType("application/json").content(stepBody)).andReturn().getResponse().getStatus());
        assertEquals(401,governanceMvc.perform(post("/api/v1/admin/governance-reauthentications").secure(true).header("Authorization","Bearer "+deciderBrowser.access()).header("Idempotency-Key",stepKey).contentType("application/json").content(stepBody)).andReturn().getResponse().getStatus());
        var stepResponse=governanceMvc.perform(post("/api/v1/admin/governance-reauthentications").secure(true).header("Origin",ORIGIN).header("Authorization","Bearer "+deciderBrowser.access()).header("Idempotency-Key",stepKey).contentType("application/json").content(stepBody)).andReturn().getResponse();assertEquals(200,stepResponse.getStatus());assertEquals("no-store",stepResponse.getHeader("Cache-Control"));
        var stepNode=JSON.readTree(stepResponse.getContentAsString());assertEquals(Set.of("proofId","expiresAt"),stepNode.propertyNames());var stepup=new AdminGovernanceReauthentication.StepUpResult(UUID.fromString(stepNode.get("proofId").asString()),Instant.parse(stepNode.get("expiresAt").asString()));
        assertEquals(stepResponse.getContentAsString(),governanceMvc.perform(post("/api/v1/admin/governance-reauthentications").secure(true).header("Origin",ORIGIN).header("Authorization","Bearer "+deciderBrowser.access()).header("Idempotency-Key",stepKey).contentType("application/json").content(stepBody)).andReturn().getResponse().getContentAsString());

        var decision=new com.elogistic.backend.merchant.api.MerchantGovernance.DecisionInput(2,"1",stepup.proofId(),decisionReason);UUID decisionKey=UUID.randomUUID();
        var otherDeciderSession=commandAdmission(decider,clock);assertThrows(AdminFailure.class,()->otherDeciderSession.execute(UUID.randomUUID(),decisionRefs,c->applyDecision(governance,c,app,decision,10,UUID.randomUUID(),approval)),"PROOF_REMAINS_BOUND_TO_ISSUING_SESSION");
        assertThrows(org.springframework.dao.DataAccessException.class,()->jdbc.update("INSERT INTO merchant.governance_event(id,actor_kind,actor_id,realm_id,target_type,target_id,application_id,policy_id,operation,revision,prior_version,new_version,reason_code,request_id,occurred_at) VALUES (?,'SYSTEM',?,?,'APPLICATION',?,?,?,'REJECT',2,10,11,'INDEPENDENT_REJECTION',?,clock_timestamp())",UUID.randomUUID(),decider,REALM,app,app,policyId,UUID.randomUUID()));

        assertThrows(com.elogistic.backend.merchant.api.GovernanceFailure.class,()->reviewerCommands.execute(UUID.randomUUID(),reviewerRefs,c->applyDecision(governance,c,app,decision,10,UUID.randomUUID(),approval)));
        assertThrows(IllegalStateException.class,()->deciderCommands.execute(UUID.randomUUID(),decisionRefs,c->{try{applyDecision(governance,c,app,decision,10,decisionKey,approval);if(approval)noticeComposition.after(c,app,operation,decisionKey);}catch(org.springframework.dao.DataAccessException failure){throw new AssertionError("SYNTHETIC_CANONICAL_DECISION_SQL_FAILED",failure);}throw new IllegalStateException("SYNTHETIC_REJECTION_ROLLBACK");}));
        assertEquals("UNDER_REVIEW",jdbc.queryForObject("SELECT state FROM merchant.application WHERE id=?",String.class,app));
        assertEquals(0,jdbc.queryForObject("SELECT count(*) FROM identity.admin_governance_reauthentication WHERE id=? AND consumed_at IS NOT NULL",Integer.class,stepup.proofId()));
        String decisionBody=JSON.writeValueAsString(decision);
        assertEquals(428,governanceMvc.perform(post(route+decisionPath).secure(true).header("Origin",ORIGIN).header("Authorization","Bearer "+deciderBrowser.access()).header("Idempotency-Key",decisionKey).contentType("application/json").content(decisionBody)).andReturn().getResponse().getStatus());
        org.springframework.mock.web.MockHttpServletResponse rejectedHttp;
        try(var pool=java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor()){
            var barrier=new java.util.concurrent.CyclicBarrier(2);java.util.concurrent.Callable<org.springframework.mock.web.MockHttpServletResponse> command=()->{barrier.await(10,java.util.concurrent.TimeUnit.SECONDS);return governanceMvc.perform(post(route+decisionPath).secure(true).header("Origin",ORIGIN).header("Authorization","Bearer "+deciderBrowser.access()).header("Idempotency-Key",decisionKey).header("If-Match","\"v10\"").contentType("application/json").content(decisionBody)).andReturn().getResponse();};
            var first=pool.submit(command);var second=pool.submit(command);var firstHttp=first.get(30,java.util.concurrent.TimeUnit.SECONDS);var secondHttp=second.get(30,java.util.concurrent.TimeUnit.SECONDS);
            assertTrue(Set.of(200,503).contains(firstHttp.getStatus()));assertTrue(Set.of(200,503).contains(secondHttp.getStatus()));assertTrue(firstHttp.getStatus()==200||secondHttp.getStatus()==200,"AT_LEAST_ONE_CONCURRENT_REQUEST_MUST_COMMIT");
            // Read admission deliberately try-locks; a contending request may fail closed503.
            // After both attempts complete, exactly the same key/body/ETag must resolve200.
            rejectedHttp=firstHttp.getStatus()==200?firstHttp:secondHttp;
            if(firstHttp.getStatus()==503||secondHttp.getStatus()==503){var retried=governanceMvc.perform(post(route+decisionPath).secure(true).header("Origin",ORIGIN).header("Authorization","Bearer "+deciderBrowser.access()).header("Idempotency-Key",decisionKey).header("If-Match","\"v10\"").contentType("application/json").content(decisionBody)).andReturn().getResponse();assertEquals(200,retried.getStatus());assertEquals(rejectedHttp.getContentAsString(),retried.getContentAsString());}
            else assertEquals(firstHttp.getContentAsString(),secondHttp.getContentAsString());
        }
        assertEquals(200,rejectedHttp.getStatus());assertEquals("\"v11\"",rejectedHttp.getHeader("ETag"));
        var rejected=JSON.readValue(rejectedHttp.getContentAsString(),com.elogistic.backend.merchant.api.MerchantGovernance.ApplicationSummary.class);assertEquals(approval?"APPROVED":"REJECTED",rejected.state());assertEquals(11,rejected.version());
        assertEquals(rejectedHttp.getContentAsString(),governanceMvc.perform(post(route+decisionPath).secure(true).header("Origin",ORIGIN).header("Authorization","Bearer "+deciderBrowser.access()).header("Idempotency-Key",decisionKey).header("If-Match","\"v10\"").contentType("application/json").content(decisionBody)).andReturn().getResponse().getContentAsString());
        assertEquals(rejected,deciderCommands.execute(UUID.randomUUID(),decisionRefs,c->applyDecision(governance,c,app,decision,10,decisionKey,approval)),"SAME_KEY_REPLAY_PRECEDES_STALE_ETAG_AND_CONSUMED_PROOF");
        assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM merchant.governance_event WHERE application_id=? AND operation=?",Integer.class,app,operation));
        evidenceHook.set(()->{try(var operator=onboarding.governanceOperatorConnection()){new MerchantGovernancePostgresCases(db,source,onboarding,REALM,reviewer).scope(operator,UUID.randomUUID(),policyId,"REVOKE",1,expiry,"application.read","application.review","application.evidence.read");}catch(Exception failure){throw new IllegalStateException("SYNTHETIC_SCOPE_REVOCATION_FAILED",failure);}});
        var revokedDuringIo=governanceMvc.perform(get(evidenceRoute).secure(true).header("Authorization","Bearer "+reviewerBrowser.access())).andReturn().getResponse();assertEquals(404,revokedDuringIo.getStatus(),"REVOKED_DURING_PRIVATE_IO_MUST_NOT_RETURN_BYTES");assertTrue(revokedDuringIo.getContentType().startsWith("application/json"));assertNull(evidenceHook.get());

        assertThrows(com.elogistic.backend.merchant.api.GovernanceFailure.class,()->reviewerCommands.execute(UUID.randomUUID(),reviewerRefs,c->governance.requestInformation(c,app,info,5,infoKey)),"REVOKED_AUTHORITY_DENIES_RECEIPT_REPLAY");
        var audit=reads.execute(c->governance.events(c,app,0,2));assertEquals(2,audit.items().size());assertTrue(audit.hasNext());assertTrue(audit.items().stream().allMatch(e->e.targetId().equals(app)&&e.targetType().equals("APPLICATION")&&e.policyVersion().equals("1")));
        String auditJson=JSON.writeValueAsString(audit);assertFalse(auditJson.contains(info.reason()));assertFalse(auditJson.contains("protectedPayload"));assertFalse(auditJson.contains("submittedInput"));
        assertEquals(assigned.version(),reads.execute(c->governance.receipt(c,"ASSIGN_REVIEWER",assignKey)).version());
        assertThrows(com.elogistic.backend.merchant.api.GovernanceFailure.class,()->reads.execute(c->governance.receipt(c,operation,decisionKey)),"RECEIPT_MUST_BELONG_TO_CURRENT_ACTOR");
        assertThrows(com.elogistic.backend.merchant.api.GovernanceFailure.class,()->reviewerReads.execute(c->governance.receipt(c,"BEGIN_REVIEW",beginKey)),"REVOKED_DOMAIN_AUTHORITY_CANNOT_READ_OLD_RECEIPT");
        var auditHttp=governanceMvc.perform(get(route+"/events?page=0&size=2").secure(true).header("Authorization","Bearer "+ownerBrowser.access())).andReturn().getResponse();assertEquals(200,auditHttp.getStatus());assertEquals("no-store",auditHttp.getHeader("Cache-Control"));assertEquals(2,JSON.readTree(auditHttp.getContentAsString()).get("items").size());
        assertEquals(200,governanceMvc.perform(get("/api/v1/admin/merchant-governance/receipts/ASSIGN_REVIEWER/"+assignKey).secure(true).header("Authorization","Bearer "+ownerBrowser.access())).andReturn().getResponse().getStatus());
        assertEquals(404,governanceMvc.perform(get("/api/v1/admin/merchant-governance/receipts/"+operation+"/"+decisionKey).secure(true).header("Authorization","Bearer "+ownerBrowser.access())).andReturn().getResponse().getStatus());
        assertEquals(approval?1:0,jdbc.queryForObject("SELECT count(*) FROM merchant.organization",Integer.class));
        if(approval){
            assertNotNull(rejected.activation());assertEquals(app,rejected.activation().applicationId());
            UUID org=rejected.activation().organizationId();var organization=deciderReads.execute(c->governance.organization(c,org));assertEquals("ACTIVE",organization.state());assertEquals(1,organization.version());assertTrue(organization.allowedActions().contains("SUSPEND"));assertEquals(Map.of("OWNER",Map.of("ACTIVE",1L)),organization.membershipCounts());
            assertEquals(1,deciderReads.execute(c->governance.organizations(c,new com.elogistic.backend.merchant.api.MerchantGovernance.OrganizationQuery(null,policyId,0,20))).items().size());assertTrue(applicantWorkflow.businessWorkspaceVisible(org));
            assertEquals(200,governanceMvc.perform(get("/api/v1/admin/merchant-organizations").secure(true).header("Authorization","Bearer "+deciderBrowser.access())).andReturn().getResponse().getStatus());
            assertEquals(404,governanceMvc.perform(get("/api/v1/admin/merchant-organizations/"+org).secure(true).header("Authorization","Bearer "+reviewerBrowser.access())).andReturn().getResponse().getStatus());
            assertThrows(org.springframework.dao.DataAccessException.class,()->jdbc.update("UPDATE merchant.organization SET state='SUSPENDED',version=version+1,updated_at=clock_timestamp() WHERE id=?",org));
            var orgRefs=deciderReads.execute(c->governance.organizationSubjects(c,org));var orgTarget=new com.elogistic.backend.merchant.api.MerchantGovernance.Target(com.elogistic.backend.merchant.api.MerchantGovernance.Kind.ORGANIZATION,org);
            for(String action:List.of("SUSPEND","REINSTATE")){
                long version=action.equals("SUSPEND")?1:2;String reason="Synthetic organization "+action;var digestFields=new TreeMap<String,Object>();digestFields.put("operation",action);digestFields.put("originalVersion",version);digestFields.put("reason",reason);digestFields.put("target",org.toString());
                String digest=HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(JSON.writeValueAsString(digestFields).getBytes(java.nio.charset.StandardCharsets.UTF_8)));
                String authority=deciderCommands.execute(UUID.randomUUID(),orgRefs,c->governance.withScope(c,orgTarget,scope->scope.version()));
                String lifecycleStepBody=JSON.writeValueAsString(Map.of("operation",action,"targetId",org,"expectedVersion",version,"commandDigest",digest,"password",password));
                var proofHttp=governanceMvc.perform(post("/api/v1/admin/governance-reauthentications").secure(true).header("Origin",ORIGIN).header("Authorization","Bearer "+deciderBrowser.access()).header("Idempotency-Key",UUID.randomUUID()).contentType("application/json").content(lifecycleStepBody)).andReturn().getResponse();assertEquals(200,proofHttp.getStatus());
                var proof=JSON.readValue(proofHttp.getContentAsString(),AdminGovernanceReauthentication.StepUpResult.class);
                var input=new com.elogistic.backend.merchant.api.MerchantGovernance.LifecycleInput(reason,proof.proofId());UUID operationKey=UUID.randomUUID();
                String endpoint="/api/v1/admin/merchant-organizations/"+org+(action.equals("SUSPEND")?"/suspension":"/reinstatement");String inputBody=JSON.writeValueAsString(input);
                assertEquals(428,governanceMvc.perform(post(endpoint).secure(true).header("Origin",ORIGIN).header("Authorization","Bearer "+deciderBrowser.access()).header("Idempotency-Key",operationKey).contentType("application/json").content(inputBody)).andReturn().getResponse().getStatus());
                assertEquals(401,governanceMvc.perform(post(endpoint).secure(true).header("Authorization","Bearer "+deciderBrowser.access()).header("Idempotency-Key",operationKey).header("If-Match","\"v"+version+"\"").contentType("application/json").content(inputBody)).andReturn().getResponse().getStatus());
                var changedHttp=governanceMvc.perform(post(endpoint).secure(true).header("Origin",ORIGIN).header("Authorization","Bearer "+deciderBrowser.access()).header("Idempotency-Key",operationKey).header("If-Match","\"v"+version+"\"").contentType("application/json").content(inputBody)).andReturn().getResponse();assertEquals(200,changedHttp.getStatus());
                var changed=JSON.readValue(changedHttp.getContentAsString(),com.elogistic.backend.merchant.api.MerchantGovernance.OrganizationSummary.class);
                assertEquals(action.equals("SUSPEND")?"SUSPENDED":"ACTIVE",changed.state());assertEquals(version+1,changed.version());
                assertEquals(changed,deciderCommands.execute(UUID.randomUUID(),orgRefs,c->action.equals("SUSPEND")?governance.suspend(c,org,input,version,operationKey):governance.reinstate(c,org,input,version,operationKey)),"LIFECYCLE_REPLAY_PRECEDES_STALE_VERSION_AND_CONSUMED_PROOF");
                assertEquals(!action.equals("SUSPEND"),applicantWorkflow.businessWorkspaceVisible(org));
            }
assertEquals(applicant,jdbc.queryForObject("SELECT account_id FROM merchant.membership WHERE id=?",UUID.class,rejected.activation().ownerMembershipId()));
            assertEquals(List.of("dashboard.read"),jdbc.queryForList("SELECT permission FROM merchant.member_grant WHERE organization_id=?",String.class,rejected.activation().organizationId()));
            assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM merchant.business_key_registry",Integer.class));assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM merchant.application_activation",Integer.class));
            assertEquals(2,jdbc.queryForObject("SELECT count(*) FROM notification.merchant_application_notice",Integer.class));assertEquals(0,mailCalls.get());
            assertEquals(Boolean.TRUE,deciderCommands.execute(UUID.randomUUID(),decisionRefs,c->noticeEvents.commandEvent(c,app,"APPROVE",decisionKey).isEmpty()));
            assertThrows(com.elogistic.backend.merchant.api.GovernanceFailure.class,()->deciderCommands.execute(UUID.randomUUID(),decisionRefs,c->noticeEvents.commandEvent(c,app,"APPROVE",UUID.randomUUID())));
            applicantWorkflow.noticeWithdrawalAndReaderDenials(noticeEvents,noticeComposition);
            assertEquals(3,jdbc.queryForObject("SELECT count(*) FROM notification.merchant_application_notice",Integer.class));
            assertThrows(org.springframework.dao.DataAccessException.class,()->jdbc.update("INSERT INTO notification.merchant_application_notice VALUES (?,?,?,?,decode(repeat('01',64),'hex'),'n1','APPROVED','QUEUED',0,NULL,clock_timestamp(),NULL)",UUID.randomUUID(),rejected.activation().decisionEventId(),app,applicant));
            assertThrows(org.springframework.dao.DataAccessException.class,()->jdbc.update("UPDATE notification.merchant_application_notice SET protected_recipient=decode(repeat('01',64),'hex') WHERE application_id=?",app));
            retryNext.set(true);assertTrue(noticeWorker.runOne());assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM notification.merchant_application_notice WHERE state='RETRY_WAIT'",Integer.class));
            try(var workers=java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor()){var first=workers.submit(noticeWorker::runOne);var second=workers.submit(noticeWorker::runOne);assertTrue(first.get());assertTrue(second.get());}assertFalse(noticeWorker.runOne());
            var later=new com.elogistic.backend.notifications.internal.infrastructure.JdbcMerchantApplicationNotices(source,applicantWorkflow.customerAdmission(),noticeCommands,new com.elogistic.backend.notifications.internal.infrastructure.ProtectedMerchantNoticeRecipient(Map.of("n1",encryption),"n1"),testMail,Clock.offset(clock,Duration.ofMinutes(2)));
            assertTrue(later.runOne());assertFalse(later.runOne());assertEquals(4,mailCalls.get());assertEquals(mailIds.getFirst(),mailIds.getLast());
            assertEquals(3,jdbc.queryForObject("SELECT count(*) FROM notification.merchant_application_notice WHERE state='ACCEPTED_BY_PROVIDER'",Integer.class));
            applicantWorkflow.noticeWithdrawalAndReaderDenials(noticeEvents,noticeComposition);forceTransient.set(true);
            for(int attempt=0;attempt<5;attempt++){var retrying=new com.elogistic.backend.notifications.internal.infrastructure.JdbcMerchantApplicationNotices(source,applicantWorkflow.customerAdmission(),noticeCommands,new com.elogistic.backend.notifications.internal.infrastructure.ProtectedMerchantNoticeRecipient(Map.of("n1",encryption),"n1"),testMail,Clock.offset(clock,Duration.ofHours(attempt+1)));assertTrue(retrying.runOne());}
            assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM notification.merchant_application_notice WHERE state='FAILED' AND attempts=5",Integer.class));
            assertFalse(new com.elogistic.backend.notifications.internal.infrastructure.JdbcMerchantApplicationNotices(source,applicantWorkflow.customerAdmission(),noticeCommands,new com.elogistic.backend.notifications.internal.infrastructure.ProtectedMerchantNoticeRecipient(Map.of("n1",encryption),"n1"),testMail,Clock.offset(clock,Duration.ofDays(1))).runOne());
        }
    }
    private com.elogistic.backend.merchant.api.MerchantGovernanceViews views(ReadyMerchant ready){
        byte[] encryption=new byte[32];Arrays.fill(encryption,(byte)72);
        return new com.elogistic.backend.merchant.internal.infrastructure.JdbcMerchantGovernanceViews(source,(com.elogistic.backend.merchant.internal.infrastructure.JdbcMerchantApplicationReviews)ready.governance(),ready.commands(),new com.elogistic.backend.merchant.internal.infrastructure.ProtectedMerchantFingerprints(Map.of("v1",key()),"v1"),new com.elogistic.backend.merchant.internal.infrastructure.ProtectedGovernancePayloads(Map.of("e1",encryption),"e1"),clock);
    }
    private void viewScope(UUID account,String type,UUID target,String action,long expected,String... permissions)throws Exception{
        Instant expiry=action.equals("REVOKE")?jdbc.queryForObject("SELECT expires_at FROM merchant.business_reviewer_scope WHERE account_id=? AND realm_id=? AND coalesce(policy_id,organization_id)=? AND state='ACTIVE'",java.sql.Timestamp.class,account,REALM,target).toInstant():clock.instant().plusSeconds(3600).truncatedTo(java.time.temporal.ChronoUnit.MICROS);
        try(var op=onboarding.governanceOperatorConnection();var q=op.prepareStatement("SELECT merchant.configure_local_reviewer_scope(?,?,?,?,?,?,?,?,?,'Synthetic R01 isolated authority')")){
            q.setObject(1,UUID.randomUUID());q.setObject(2,account);q.setObject(3,REALM);q.setString(4,type);q.setObject(5,target);q.setArray(6,op.createArrayOf("text",permissions));q.setString(7,action);q.setLong(8,expected);q.setTimestamp(9,java.sql.Timestamp.from(expiry));q.execute();
        }
    }
    public void merchantR01SearchCommentsAndScope()throws Exception{
        var ready=readyMerchant(null,null);UUID app=ready.application();UUID policyId=jdbc.queryForObject("SELECT policy_id FROM merchant.application WHERE id=?",UUID.class,app);
        var second=readyMerchant(policyId,ready.refs().admins().iterator().next(),true);
        var prior=Arrays.stream(db.flyway("45",REALM).info().applied()).map(x->x.getChecksum()).toList();db.flyway("46",REALM).migrate();db.flyway("46",REALM).validate();
        assertEquals(prior,Arrays.stream(db.flyway("46",REALM).info().applied()).limit(prior.size()).map(x->x.getChecksum()).toList());
        viewScope(admin,"POLICY",policyId,"REVOKE",1,"application.read","application.assign","application.decide");
        String[] caps={"application.read","application.assign","application.decide","application.evidence.read","application.comment.write"};viewScope(admin,"POLICY",policyId,"ISSUE",0,caps);
        var views=views(ready);LocalDate date=clock.instant().atZone(ZoneId.of("Asia/Ho_Chi_Minh")).toLocalDate();
        var query=new com.elogistic.backend.merchant.api.MerchantGovernanceViews.ApplicationSearch(date,date,null,policyId,null,null,null,null,null,"MERCHANT_REVIEW",new com.elogistic.backend.merchant.api.MerchantGovernanceViews.PageQuery(1,null),null,null,null);
        var result=ready.commands().execute(UUID.randomUUID(),ready.refs(),c->views.searchApplications(c,query));assertEquals(2,result.matchedCount());assertEquals(1,result.items().size());assertNotNull(result.nextCursor());assertTrue(result.snapshotVersion().matches("[a-f0-9]{64}"));
        assertFalse(JSON.writeValueAsString(result).contains("business-contact@example.invalid"));
        ready.commands().execute(UUID.randomUUID(),ready.refs(),c->{
            var forwarding=(AdminCommandContext)java.lang.reflect.Proxy.newProxyInstance(AdminCommandContext.class.getClassLoader(),new Class<?>[]{AdminCommandContext.class},(proxy,method,args)->method.invoke(c,args));
            assertThrows(AdminFailure.class,()->views.searchApplications(forwarding,query));
            byte[] encryption=new byte[32];Arrays.fill(encryption,(byte)72);
            var substituted=new com.elogistic.backend.merchant.internal.infrastructure.JdbcMerchantGovernanceViews(new DriverManagerDataSource(db.url(),db.runtime(),db.runtimePassword()),(com.elogistic.backend.merchant.internal.infrastructure.JdbcMerchantApplicationReviews)ready.governance(),ready.commands(),new com.elogistic.backend.merchant.internal.infrastructure.ProtectedMerchantFingerprints(Map.of("v1",key()),"v1"),new com.elogistic.backend.merchant.internal.infrastructure.ProtectedGovernancePayloads(Map.of("e1",encryption),"e1"),clock);
            assertThrows(com.elogistic.backend.merchant.api.GovernanceFailure.class,()->substituted.searchApplications(c,query));return null;
        });
        var nextQuery=new com.elogistic.backend.merchant.api.MerchantGovernanceViews.ApplicationSearch(date,date,null,policyId,null,null,null,null,null,"MERCHANT_REVIEW",new com.elogistic.backend.merchant.api.MerchantGovernanceViews.PageQuery(1,result.nextCursor()),null,null,null);
        var next=ready.commands().execute(UUID.randomUUID(),ready.refs(),c->views.searchApplications(c,nextQuery));assertEquals(2,next.matchedCount());assertNull(next.nextCursor());assertEquals(Set.of(app,second.application()),Set.of(result.items().getFirst().id(),next.items().getFirst().id()));
        var exact=new com.elogistic.backend.merchant.api.MerchantGovernanceViews.ApplicationSearch(date,date,null,policyId,null,null,null,null,"SYNTHETIC-REG-1","MERCHANT_REVIEW",new com.elogistic.backend.merchant.api.MerchantGovernanceViews.PageQuery(1,null),null,null,null);
        var filtered=ready.commands().execute(UUID.randomUUID(),ready.refs(),c->views.searchApplications(c,exact));assertEquals(1,filtered.matchedCount());assertEquals(app,filtered.items().getFirst().id());assertNull(filtered.nextCursor());

        var input=new com.elogistic.backend.merchant.api.MerchantGovernanceViews.CommentInput(1,"Synthetic review note <script>not executable</script>");UUID key=UUID.randomUUID();
        var comment=ready.commands().execute(UUID.randomUUID(),ready.refs(),c->views.comment(c,app,input,6,key));
        assertEquals(comment,ready.commands().execute(UUID.randomUUID(),ready.refs(),c->views.comment(c,app,input,1,key)),"REPLAY_BEFORE_STALE_ETAG");
        assertEquals(com.elogistic.backend.merchant.api.GovernanceFailure.Code.IDEMPOTENCY_CONFLICT,assertThrows(com.elogistic.backend.merchant.api.GovernanceFailure.class,()->ready.commands().execute(UUID.randomUUID(),ready.refs(),c->views.comment(c,app,new com.elogistic.backend.merchant.api.MerchantGovernanceViews.CommentInput(1,"Different"),6,key))).code());
        assertEquals(6,jdbc.queryForObject("SELECT version FROM merchant.application WHERE id=?",Long.class,app));
        assertEquals(1,ready.commands().execute(UUID.randomUUID(),ready.refs(),c->views.comments(c,app,null)).items().size());
        // BIA-OWNER-01: preserve a real, guarded V46 comment across the forward repair.
        String commentsBefore=jdbc.queryForObject("SELECT md5(jsonb_agg(to_jsonb(c) ORDER BY id)::text) FROM merchant.application_review_comment c",String.class);
        var historical=Arrays.stream(db.flyway("46",REALM).info().applied()).map(x->x.getVersion()+":"+x.getChecksum()).toList();
        assertEquals(3,db.flyway("latest",REALM).migrate().migrationsExecuted);assertTrue(db.flyway("latest",REALM).validateWithResult().validationSuccessful);assertEquals(0,db.flyway("latest",REALM).migrate().migrationsExecuted);
        assertEquals(commentsBefore,jdbc.queryForObject("SELECT md5(jsonb_agg(to_jsonb(c) ORDER BY id)::text) FROM merchant.application_review_comment c",String.class));
        assertEquals(historical,Arrays.stream(db.flyway("latest",REALM).info().applied()).limit(historical.size()).map(x->x.getVersion()+":"+x.getChecksum()).toList());
        assertEquals(db.owner(),jdbc.queryForObject("SELECT pg_get_userbyid(relowner) FROM pg_class WHERE oid='merchant.application_review_comment'::regclass",String.class));
        assertTrue(Boolean.TRUE.equals(jdbc.queryForObject("SELECT has_table_privilege(current_user,'merchant.application_review_comment','SELECT') AND has_table_privilege(current_user,'merchant.application_review_comment','INSERT') AND NOT has_table_privilege(current_user,'merchant.application_review_comment','UPDATE') AND NOT has_table_privilege(current_user,'merchant.application_review_comment','DELETE')",Boolean.class)));
        long count=jdbc.queryForObject("SELECT count(*) FROM merchant.governance_event",Long.class);UUID rollback=UUID.randomUUID();
        assertThrows(IllegalStateException.class,()->ready.commands().execute(UUID.randomUUID(),ready.refs(),c->{views.comment(c,app,input,6,rollback);throw new IllegalStateException("Synthetic rollback");}));
        assertEquals(count,jdbc.queryForObject("SELECT count(*) FROM merchant.governance_event",Long.class));assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM merchant.application_review_comment",Integer.class));
        assertThrows(org.springframework.dao.DataAccessException.class,()->jdbc.update("UPDATE merchant.application_review_comment SET protected_text=decode(repeat('01',64),'hex')"));
        assertThrows(org.springframework.dao.DataAccessException.class,()->jdbc.update("DELETE FROM merchant.application_review_comment"));
        assertThrows(org.springframework.dao.DataAccessException.class,()->jdbc.update("INSERT INTO merchant.application_review_comment SELECT gen_random_uuid(),application_id,revision,actor_id,realm_id,protected_text,event_id,request_id,created_at,gen_random_uuid(),input_key_id,input_digest FROM merchant.application_review_comment"));
        assertThrows(org.springframework.dao.DataAccessException.class,()->jdbc.update("INSERT INTO merchant.application_review_comment SELECT gen_random_uuid(),application_id,revision,actor_id,realm_id,protected_text,gen_random_uuid(),request_id,created_at,gen_random_uuid(),input_key_id,input_digest FROM merchant.application_review_comment"));
        var checks=ready.commands().execute(UUID.randomUUID(),ready.refs(),c->views.checks(c,app));assertTrue(checks.checks().stream().anyMatch(x->x.code().equals("SUBMISSION_BINDING")&&x.result().equals("PASS")));
        try(var parallel=Executors.newVirtualThreadPerTaskExecutor()){UUID same=UUID.randomUUID();var barrier=new CyclicBarrier(2);Callable<UUID> work=()->{barrier.await(10,TimeUnit.SECONDS);return ready.commands().execute(UUID.randomUUID(),ready.refs(),c->views.comment(c,app,input,6,same)).id();};var a=parallel.submit(work);var b=parallel.submit(work);assertEquals(a.get(30,TimeUnit.SECONDS),b.get(30,TimeUnit.SECONDS));}
        assertEquals(2,jdbc.queryForObject("SELECT count(*) FROM merchant.application_review_comment",Integer.class));
        commandAdmission(ready.refs().admins().iterator().next(),clock).execute(UUID.randomUUID(),ready.refs(),c->ready.governance().requestInformation(c,app,new com.elogistic.backend.merchant.api.MerchantGovernance.InformationRequest(1,List.of("registeredAddress"),"Synthetic cursor source change"),6,UUID.randomUUID()));
        assertEquals(com.elogistic.backend.merchant.api.GovernanceFailure.Code.SNAPSHOT_EXPIRED,assertThrows(com.elogistic.backend.merchant.api.GovernanceFailure.class,()->ready.commands().execute(UUID.randomUUID(),ready.refs(),c->views.searchApplications(c,nextQuery))).code());
        var captured=new AtomicReference<AdminCommandContext>();ready.commands().execute(UUID.randomUUID(),ready.refs(),c->{captured.set(c);return null;});assertThrows(RuntimeException.class,()->views.checks(captured.get(),app));
        UUID foreign=account(Set.of(AdminReadContext.ENTRY,AdminReadContext.MERCHANT));assertThrows(com.elogistic.backend.merchant.api.GovernanceFailure.class,()->commandAdmission(foreign,clock).execute(UUID.randomUUID(),ready.refs(),c->views.searchApplications(c,query)));
        viewScope(admin,"POLICY",policyId,"REVOKE",1,caps);assertThrows(com.elogistic.backend.merchant.api.GovernanceFailure.class,()->ready.commands().execute(UUID.randomUUID(),ready.refs(),c->views.comment(c,app,input,6,key)));
    }
    public void merchantR01ProfileAndMembers()throws Exception{
        var ready=readyMerchant(null,null);var approved=decide(ready,"APPROVE",decision(ready,"APPROVE"),UUID.randomUUID());UUID organization=approved.activation().organizationId();db.flyway("46",REALM).migrate();
        viewScope(admin,"ORGANIZATION",organization,"ISSUE",0,"org.read","org.profile.read","org.members.read");var views=views(ready);var input=new com.elogistic.backend.merchant.api.MerchantGovernanceViews.DisclosureRequest("ORGANIZATION_OVERSIGHT",null);
        var profile=ready.commands().execute(UUID.randomUUID(),ready.refs(),c->views.profile(c,organization,input));assertEquals("AVAILABLE",profile.legal().get("availability"));assertEquals("UNAVAILABLE",profile.pricing().get("availability"));assertEquals("UNAVAILABLE",profile.holds().get("availability"));assertFalse(JSON.writeValueAsString(profile).contains("business-contact@example.invalid"));
        var members=ready.commands().execute(UUID.randomUUID(),ready.refs(),c->views.members(c,organization,input));assertEquals(1,members.items().size());assertTrue(members.items().getFirst().isInitialOwner());assertTrue(members.items().getFirst().isCurrentOwner());assertEquals(ready.applicant(),members.items().getFirst().accountId());assertEquals(0,members.pendingInvitationCount());assertFalse(JSON.writeValueAsString(members).contains("@"));
        var q=new com.elogistic.backend.merchant.api.MerchantGovernanceViews.OrganizationSearch("ACTIVE",null,null,null,null,null,null,null,"ORGANIZATION_OVERSIGHT",null,null);var search=ready.commands().execute(UUID.randomUUID(),ready.refs(),c->views.searchOrganizations(c,q));assertEquals(1,search.matchedCount());assertEquals(organization,search.items().getFirst().id());
        assertThrows(com.elogistic.backend.merchant.api.GovernanceFailure.class,()->ready.commands().execute(UUID.randomUUID(),ready.refs(),c->views.members(c,UUID.randomUUID(),input)));
        assertFalse(jdbc.queryForObject("SELECT has_function_privilege(current_user,'merchant.configure_local_reviewer_scope(uuid,uuid,uuid,text,uuid,text[],text,bigint,timestamptz,text)','EXECUTE')",Boolean.class));
    }
    public void merchantR01ComparisonAndHttps()throws Exception{
        var ready=readyMerchant(null,null);UUID app=ready.application(),reviewer=ready.refs().admins().iterator().next();UUID policyId=jdbc.queryForObject("SELECT policy_id FROM merchant.application WHERE id=?",UUID.class,app);db.flyway("46",REALM).migrate();
        viewScope(admin,"POLICY",policyId,"REVOKE",1,"application.read","application.assign","application.decide");viewScope(admin,"POLICY",policyId,"ISSUE",0,"application.read","application.assign","application.decide","application.evidence.read","application.comment.write");
        commandAdmission(reviewer,clock).execute(UUID.randomUUID(),ready.refs(),c->ready.governance().requestInformation(c,app,new com.elogistic.backend.merchant.api.MerchantGovernance.InformationRequest(1,List.of("registeredAddress"),"Synthetic R01 information"),6,UUID.randomUUID()));
        ready.workflow().resubmitWithFreshContactFact(app);var views=views(ready);
        var compare=new com.elogistic.backend.merchant.api.MerchantGovernanceViews.ComparisonRequest(1,2,"MERCHANT_REVIEW");var diff=ready.commands().execute(UUID.randomUUID(),ready.refs(),c->views.compare(c,app,compare));
        assertNotEquals(diff.fromDigest(),diff.toDigest());assertEquals(List.of("activationDisplayName"),diff.changes().stream().map(x->x.field()).toList());assertEquals(0,diff.evidenceAdded().size());assertEquals(0,diff.evidenceRemoved().size());assertNotNull(diff.contactVerifiedAt().from());assertNotNull(diff.contactVerifiedAt().to());
        assertThrows(com.elogistic.backend.merchant.api.GovernanceFailure.class,()->ready.commands().execute(UUID.randomUUID(),ready.refs(),c->views.compare(c,app,new com.elogistic.backend.merchant.api.MerchantGovernanceViews.ComparisonRequest(1,3,"MERCHANT_REVIEW"))));
        var review=commandAdmission(reviewer,clock);review.execute(UUID.randomUUID(),ready.refs(),c->ready.governance().review(c,app,new com.elogistic.backend.merchant.api.MerchantGovernance.ReviewInput("BEGIN",2,"1",null,"Synthetic R01 begin"),9,UUID.randomUUID()));review.execute(UUID.randomUUID(),ready.refs(),c->ready.governance().review(c,app,new com.elogistic.backend.merchant.api.MerchantGovernance.ReviewInput("READY_FOR_DECISION",2,"1",new com.elogistic.backend.merchant.api.MerchantGovernance.Checklist(true,true,true,true,true),"Synthetic R01 complete"),10,UUID.randomUUID()));
        String reason="Synthetic R01 approved",op="APPROVE";String digest=HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(JSON.writeValueAsString(new TreeMap<>(Map.of("operation",op,"target",app.toString(),"originalVersion",11L,"revision",2L,"policy","1","reason",reason))).getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        var target=new com.elogistic.backend.merchant.api.MerchantGovernance.Target(com.elogistic.backend.merchant.api.MerchantGovernance.Kind.APPLICATION,app);String authority=ready.commands().execute(UUID.randomUUID(),ready.refs(),c->ready.governance().withScope(c,target,x->x.version()));
        var proof=ready.commands().execute(UUID.randomUUID(),ready.refs(),c->ready.proofs().request(c,new AdminGovernanceReauthentication.AdminStepUpInput(op,app,11,digest,password),authority,UUID.randomUUID()));
        var approved=ready.commands().execute(UUID.randomUUID(),ready.refs(),c->ready.governance().approve(c,app,new com.elogistic.backend.merchant.api.MerchantGovernance.DecisionInput(2,"1",proof.proofId(),reason),11,UUID.randomUUID()));UUID org=approved.activation().organizationId();
        viewScope(admin,"ORGANIZATION",org,"ISSUE",0,"org.read","org.profile.read","org.members.read");
        AdminBrowserRuntime.runReviewViews(source,name(admin),password,pair,onboarding,ready.governance(),app,views);
        assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM merchant.application_review_comment WHERE application_id=?",Integer.class,app));assertEquals(12,jdbc.queryForObject("SELECT version FROM merchant.application WHERE id=?",Long.class,app));
    }
    public void organizationLifecycleV47PreparationSchema()throws Exception{organizationLifecycleV47PreparationSchema(false);}
    public void organizationLifecycleV47PreparationHttps()throws Exception{organizationLifecycleV47PreparationSchema(true);}
    private void organizationLifecycleV47PreparationSchema(boolean browser)throws Exception{
        var ready=readyMerchant(null,null);var approved=decide(ready,"APPROVE",decision(ready,"APPROVE"),UUID.randomUUID());UUID org=approved.activation().organizationId();
        var before=jdbc.queryForMap("SELECT * FROM merchant.organization WHERE id=?",org);var previous=Arrays.stream(db.flyway("46",REALM).info().applied()).map(v->v.getVersion()+":"+v.getChecksum()).toList();db.flyway("47",REALM).migrate();db.flyway("47",REALM).validate();
        assertEquals(previous,Arrays.stream(db.flyway("47",REALM).info().applied()).limit(previous.size()).map(v->v.getVersion()+":"+v.getChecksum()).toList());assertEquals(before,jdbc.queryForMap("SELECT * FROM merchant.organization WHERE id=?",org));
        var owner=jdbc.queryForMap("SELECT * FROM merchant.organization_current_owner WHERE organization_id=?",org);assertEquals(approved.activation().ownerMembershipId(),owner.get("membership_id"));assertEquals(1L,owner.get("version"));assertNull(owner.get("last_transfer_case_id"));
        assertThrows(org.springframework.dao.DataAccessException.class,()->jdbc.update("UPDATE merchant.organization_current_owner SET version=version+1 WHERE organization_id=?",org));
        viewScope(admin,"ORGANIZATION",org,"ISSUE",0,"org.read","org.lifecycle.propose");
        var policyReader=organizationCases(ready,admin);var policyReads=readAdmission(admin);
        assertTrue(policyReads.execute(c->policyReader.policies(c,org)).items().isEmpty());
        assertThrows(RuntimeException.class,()->policyReads.execute(c->policyReader.policies(c,UUID.randomUUID())));
        var escaped=new AtomicReference<AdminReadContext>();policyReads.execute(c->{escaped.set(c);return null;});assertThrows(AdminFailure.class,()->policyReader.policies(escaped.get(),org));
        String policy=com.elogistic.backend.merchant.internal.infrastructure.OrganizationCasePolicyTestFixture.ownerV1();UUID policyId=UUID.randomUUID(),key=UUID.randomUUID();
        try(var op=onboarding.governanceOperatorConnection();var q=op.prepareStatement("SELECT merchant.publish_local_organization_governance_policy(?,?,1,?::jsonb,'Synthetic Owner V1 policy')")){
            q.setObject(1,key);q.setObject(2,policyId);q.setString(3,policy);try(var r=q.executeQuery()){assertTrue(r.next());assertEquals(policyId,r.getObject(1,UUID.class));}q.execute();
            q.setString(3,policy.replace("604800","1"));assertThrows(java.sql.SQLException.class,q::execute);
        }
        assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM merchant.organization_governance_policy",Integer.class));assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM merchant.organization_governance_receipt",Integer.class));
        assertFalse(jdbc.queryForObject("SELECT has_function_privilege(current_user,'merchant.publish_local_organization_governance_policy(uuid,uuid,bigint,jsonb,text)','EXECUTE')",Boolean.class));
        assertFalse(jdbc.queryForObject("SELECT has_schema_privilege('merchant_governance_writer','merchant','CREATE')",Boolean.class));
        assertEquals(policyId,policyReads.execute(c->policyReader.policies(c,org)).items().getFirst().policyId());
        UUID other=account(Set.of(AdminReadContext.ENTRY,AdminReadContext.MERCHANT));assertThrows(java.sql.SQLException.class,()->viewScope(other,"ORGANIZATION",org,"ISSUE",0,"org.read","org.lifecycle.execute"));
        assertEquals("ACTIVE",jdbc.queryForObject("SELECT state FROM merchant.organization WHERE id=?",String.class,org));
        lifecyclePreparationFlow(ready,org,policyId,before,browser);
    }
    private com.elogistic.backend.merchant.api.OrganizationGovernanceCases organizationCases(ReadyMerchant ready,UUID actor)throws Exception{
        var reads=readAdmission(actor);var commands=commandAdmission(actor,clock);byte[] encryption=new byte[32];Arrays.fill(encryption,(byte)72);
        return new com.elogistic.backend.merchant.internal.infrastructure.JdbcOrganizationGovernanceCases(source,new com.elogistic.backend.merchant.internal.infrastructure.JdbcMerchantGovernance(source,clock,reads,commands),reads,commands,new com.elogistic.backend.merchant.internal.infrastructure.ProtectedMerchantFingerprints(Map.of("v1",key()),"v1"),new com.elogistic.backend.merchant.internal.infrastructure.ProtectedGovernancePayloads(Map.of("e1",encryption),"e1"),clock);
    }
    private void lifecyclePreparationFlow(ReadyMerchant ready,UUID org,UUID policyId,Map<String,Object> original,boolean browser)throws Exception{
        UUID reviewer=account(Set.of(AdminReadContext.ENTRY,AdminReadContext.MERCHANT));
        viewScope(reviewer,"ORGANIZATION",org,"ISSUE",0,"org.read","org.lifecycle.review","audit.read");
        byte[] encryption=new byte[32];Arrays.fill(encryption,(byte)72);
        var reads=readAdmission(admin);var maker=commandAdmission(admin,clock);var reviewCommands=commandAdmission(reviewer,clock);
        var scopes=new com.elogistic.backend.merchant.internal.infrastructure.JdbcMerchantGovernance(source,clock,reads,maker);
        var cases=new com.elogistic.backend.merchant.internal.infrastructure.JdbcOrganizationGovernanceCases(source,scopes,reads,maker,new com.elogistic.backend.merchant.internal.infrastructure.ProtectedMerchantFingerprints(Map.of("v1",key()),"v1"),new com.elogistic.backend.merchant.internal.infrastructure.ProtectedGovernancePayloads(Map.of("e1",encryption),"e1"),clock);
        var reviewerReads=readAdmission(reviewer);
        var reviewCases=new com.elogistic.backend.merchant.internal.infrastructure.JdbcOrganizationGovernanceCases(source,new com.elogistic.backend.merchant.internal.infrastructure.JdbcMerchantGovernance(source,clock,reviewerReads,reviewCommands),reviewerReads,reviewCommands,new com.elogistic.backend.merchant.internal.infrastructure.ProtectedMerchantFingerprints(Map.of("v1",key()),"v1"),new com.elogistic.backend.merchant.internal.infrastructure.ProtectedGovernancePayloads(Map.of("e1",encryption),"e1"),clock);
        var refs=new AdminCommandAdmission.ReferencedSubjects(Set.of(ready.applicant()),Set.of(admin,reviewer));
        var input=new com.elogistic.backend.merchant.api.OrganizationGovernanceCases.CaseInput("CLOSE","Synthetic closure preparation only",policyId,1,1,new com.elogistic.backend.merchant.api.OrganizationGovernanceCases.Closure());UUID createKey=UUID.randomUUID();
        var draft=maker.execute(UUID.randomUUID(),refs,c->cases.create(c,org,input,createKey));assertEquals("DRAFT",draft.state());
        assertEquals(draft,maker.execute(UUID.randomUUID(),refs,c->cases.create(c,org,input,createKey)));
        assertThrows(com.elogistic.backend.merchant.api.GovernanceFailure.class,()->maker.execute(UUID.randomUUID(),refs,c->cases.create(c,org,new com.elogistic.backend.merchant.api.OrganizationGovernanceCases.CaseInput("CLOSE","Different input",policyId,1,1,input.payload()),createKey)));
        var submit=new com.elogistic.backend.merchant.api.OrganizationGovernanceCases.ReasonAction(com.elogistic.backend.merchant.api.OrganizationGovernanceCases.ReasonTag.SUBMIT,new com.elogistic.backend.merchant.api.OrganizationGovernanceCases.ReasonInput("Synthetic submit"));UUID submitKey=UUID.randomUUID();
        var submitted=maker.execute(UUID.randomUUID(),refs,c->cases.command(c,org,draft.id(),submit,1,submitKey));assertEquals("SUBMITTED",submitted.state());assertEquals(submitted,maker.execute(UUID.randomUUID(),refs,c->cases.command(c,org,draft.id(),submit,1,submitKey)));
        assertThrows(com.elogistic.backend.merchant.api.GovernanceFailure.class,()->maker.execute(UUID.randomUUID(),refs,c->cases.command(c,org,draft.id(),new com.elogistic.backend.merchant.api.OrganizationGovernanceCases.AssignmentInput(admin,"Self reviewer"),2,UUID.randomUUID())));
        var assigned=maker.execute(UUID.randomUUID(),refs,c->cases.command(c,org,draft.id(),new com.elogistic.backend.merchant.api.OrganizationGovernanceCases.AssignmentInput(reviewer,"Synthetic assignment"),2,UUID.randomUUID()));assertEquals(reviewer,assigned.reviewerAccountId());
        reviewCommands.execute(UUID.randomUUID(),refs,c->reviewCases.command(c,org,draft.id(),new com.elogistic.backend.merchant.api.OrganizationGovernanceCases.CaseReviewInput("BEGIN",null,"Synthetic begin"),3,UUID.randomUUID()));
        reviewCommands.execute(UUID.randomUUID(),refs,c->reviewCases.command(c,org,draft.id(),new com.elogistic.backend.merchant.api.OrganizationGovernanceCases.InformationInput(List.of("reason"),"Synthetic information required"),4,UUID.randomUUID()));
        var information=maker.execute(UUID.randomUUID(),refs,c->cases.disclose(c,org,draft.id(),new com.elogistic.backend.merchant.api.OrganizationGovernanceCases.CaseDisclosureInput(5,"LIFECYCLE_REVIEW")));assertEquals(List.of("reason"),information.requestedFieldCodes());assertEquals("Synthetic information required",information.reason());
        maker.execute(UUID.randomUUID(),refs,c->cases.command(c,org,draft.id(),new com.elogistic.backend.merchant.api.OrganizationGovernanceCases.ResponseInput(input.payload(),List.of(),"Synthetic response",null),5,UUID.randomUUID()));
        reviewCommands.execute(UUID.randomUUID(),refs,c->reviewCases.command(c,org,draft.id(),new com.elogistic.backend.merchant.api.OrganizationGovernanceCases.CaseReviewInput("BEGIN",null,"Synthetic review revision two"),6,UUID.randomUUID()));
        var readyCase=reviewCommands.execute(UUID.randomUUID(),refs,c->reviewCases.command(c,org,draft.id(),new com.elogistic.backend.merchant.api.OrganizationGovernanceCases.CaseReviewInput("READY_FOR_DECISION",new com.elogistic.backend.merchant.api.OrganizationGovernanceCases.Checklist(true,true,true,true),"Synthetic complete"),7,UUID.randomUUID()));assertEquals("READY_FOR_DECISION",readyCase.state());assertTrue(readyCase.allowedActions().stream().noneMatch(a->Set.of("EXECUTE","APPROVE").contains(a)));
        assertEquals(2,jdbc.queryForObject("SELECT revision FROM merchant.organization_governance_case WHERE id=?",Integer.class,draft.id()));assertEquals(9,jdbc.queryForObject("SELECT count(*) FROM merchant.organization_governance_event WHERE case_id=?",Integer.class,draft.id()));assertEquals(8,jdbc.queryForObject("SELECT count(*) FROM merchant.organization_governance_receipt WHERE case_id=?",Integer.class,draft.id()));assertEquals(original,jdbc.queryForMap("SELECT * FROM merchant.organization WHERE id=?",org));
        assertThrows(org.springframework.dao.DataAccessException.class,()->jdbc.update("UPDATE merchant.organization_governance_case SET state='EXECUTED',version=version+1 WHERE id=?",draft.id()));
        var foreign=UUID.randomUUID();assertThrows(RuntimeException.class,()->reads.execute(c->cases.get(c,foreign,draft.id())));
        var constructed=(AdminReadContext)java.lang.reflect.Proxy.newProxyInstance(AdminReadContext.class.getClassLoader(),new Class[]{AdminReadContext.class},(p,m,a)->{throw new AssertionError("UNTRUSTED_CONTEXT_READ");});assertThrows(AdminFailure.class,()->cases.get(constructed,org,draft.id()));
        assertEquals(9,reviewerReads.execute(c->reviewCases.events(c,org,draft.id(),new com.elogistic.backend.merchant.api.MerchantGovernanceViews.PageQuery(20,null))).items().size());
        var disclosureInput=new com.elogistic.backend.merchant.api.OrganizationGovernanceCases.CaseDisclosureInput(8,"LIFECYCLE_REVIEW");
        var disclosed=maker.execute(UUID.randomUUID(),refs,c->cases.disclose(c,org,draft.id(),disclosureInput));assertEquals(8L,disclosed.version());assertEquals(2L,disclosed.revision());assertEquals(readyCase.lastEventId(),disclosed.sourceEventId());assertEquals(input.payload(),disclosed.casePayload());assertEquals("Synthetic complete",disclosed.reason());
        reviewCommands.execute(UUID.randomUUID(),refs,c->reviewCases.disclose(c,org,draft.id(),disclosureInput));
        assertFalse(JSON.writeValueAsString(disclosed).contains("protected_payload"));assertFalse(JSON.writeValueAsString(disclosed).contains("reauthenticationProofId"));assertFalse(disclosed.toString().contains("Synthetic"));
        assertEquals(3,jdbc.queryForObject("SELECT count(*) FROM merchant.organization_governance_event WHERE case_id=? AND operation='READ_CASE_DISCLOSURE'",Integer.class,draft.id()));assertEquals(8,jdbc.queryForObject("SELECT version FROM merchant.organization_governance_case WHERE id=?",Integer.class,draft.id()));assertEquals(8,jdbc.queryForObject("SELECT count(*) FROM merchant.organization_governance_receipt WHERE case_id=?",Integer.class,draft.id()));
        assertThrows(com.elogistic.backend.merchant.api.GovernanceFailure.class,()->maker.execute(UUID.randomUUID(),refs,c->cases.disclose(c,org,draft.id(),new com.elogistic.backend.merchant.api.OrganizationGovernanceCases.CaseDisclosureInput(7,"LIFECYCLE_REVIEW"))));
        assertThrows(RuntimeException.class,()->maker.execute(UUID.randomUUID(),refs,c->cases.disclose(c,UUID.randomUUID(),draft.id(),disclosureInput)));
        UUID unassigned=account(Set.of(AdminReadContext.ENTRY,AdminReadContext.MERCHANT));viewScope(unassigned,"ORGANIZATION",org,"ISSUE",0,"org.read","org.lifecycle.review");var otherCases=organizationCases(ready,unassigned);assertThrows(com.elogistic.backend.merchant.api.GovernanceFailure.class,()->commandAdmission(unassigned,clock).execute(UUID.randomUUID(),refs,c->otherCases.disclose(c,org,draft.id(),disclosureInput)));
        assertThrows(org.springframework.dao.DataAccessException.class,()->jdbc.update("INSERT INTO merchant.organization_governance_event SELECT ?,c.id,c.organization_id,'SYSTEM',?,?,NULL,'READ_CASE_DISCLOSURE',c.revision,c.version,c.version,c.policy_id,c.policy_version,'CASE_DISCLOSURE',c.protected_payload,?,clock_timestamp() FROM merchant.organization_governance_case c WHERE c.id=?",UUID.randomUUID(),unassigned,REALM,UUID.randomUUID(),draft.id()));
        var forgedCommand=(AdminCommandContext)java.lang.reflect.Proxy.newProxyInstance(AdminCommandContext.class.getClassLoader(),new Class[]{AdminCommandContext.class},(p,m,a)->{throw new AssertionError("FORGED_CONTEXT_READ");});assertThrows(AdminFailure.class,()->cases.disclose(forgedCommand,org,draft.id(),disclosureInput));
        assertThrows(IllegalStateException.class,()->maker.execute(UUID.randomUUID(),refs,c->{cases.disclose(c,org,draft.id(),disclosureInput);throw new IllegalStateException("SYNTHETIC_DISCLOSURE_ROLLBACK");}));assertEquals(3,jdbc.queryForObject("SELECT count(*) FROM merchant.organization_governance_event WHERE case_id=? AND operation='READ_CASE_DISCLOSURE'",Integer.class,draft.id()));
        int beforeRollback=jdbc.queryForObject("SELECT count(*) FROM merchant.organization_governance_case",Integer.class);
        var risk=new com.elogistic.backend.merchant.api.OrganizationGovernanceCases.CaseInput("RISK_HOLD","Synthetic preparation",policyId,1,1,new com.elogistic.backend.merchant.api.OrganizationGovernanceCases.RiskHold("BOOKING_HOLD",null));
        assertThrows(IllegalStateException.class,()->maker.execute(UUID.randomUUID(),refs,c->{cases.create(c,org,risk,UUID.randomUUID());throw new IllegalStateException("SYNTHETIC_ROLLBACK");}));assertEquals(beforeRollback,jdbc.queryForObject("SELECT count(*) FROM merchant.organization_governance_case",Integer.class));
        UUID concurrentKey=UUID.randomUUID();var barrier=new CyclicBarrier(2);var outcomes=new ConcurrentLinkedQueue<com.elogistic.backend.merchant.api.OrganizationGovernanceCases.Case>();
        try(var pool=Executors.newVirtualThreadPerTaskExecutor()){List<Future<?>> pending=new ArrayList<>();for(int n=0;n<2;n++)pending.add(pool.submit(()->{barrier.await(10,TimeUnit.SECONDS);outcomes.add(maker.execute(UUID.randomUUID(),refs,c->cases.create(c,org,risk,concurrentKey)));return null;}));for(var result:pending)result.get(30,TimeUnit.SECONDS);}
        assertEquals(2,outcomes.size());assertEquals(outcomes.peek(),outcomes.stream().skip(1).findFirst().orElseThrow());assertEquals(beforeRollback+1,jdbc.queryForObject("SELECT count(*) FROM merchant.organization_governance_case",Integer.class));
        var mvc=MockMvcBuilders.standaloneSetup(new OrganizationGovernanceCasesHttp(reads,maker,cases)).addFilters((jakarta.servlet.Filter)(request,response,chain)->{((HttpServletResponse)response).setHeader("X-Request-Id",UUID.randomUUID().toString());chain.doFilter(request,response);}).build();
        String base="/api/v1/admin/merchant-organizations/"+org+"/governance-cases";
        assertEquals(200,mvc.perform(get(base)).andReturn().getResponse().getStatus());assertEquals(200,mvc.perform(get(base+"/"+draft.id())).andReturn().getResponse().getStatus());
        assertEquals(400,mvc.perform(get(base+"?size=1&size=2")).andReturn().getResponse().getStatus());
        assertEquals(400,mvc.perform(post(base).contentType("application/json").header("Idempotency-Key",UUID.randomUUID()).content("{\"type\":\"CLOSE\",\"type\":\"RISK_HOLD\"}")).andReturn().getResponse().getStatus());
        assertEquals(428,mvc.perform(post(base+"/"+draft.id()+"/submission").contentType("application/json").header("Idempotency-Key",UUID.randomUUID()).content("{\"reason\":\"Synthetic\"}")).andReturn().getResponse().getStatus());
        assertEquals(404,mvc.perform(post(base+"/"+draft.id()+"/execution").contentType("application/json").content("{}")).andReturn().getResponse().getStatus());
        var replacementRefs=new AdminCommandAdmission.ReferencedSubjects(Set.of(ready.applicant()),Set.of(admin,reviewer,unassigned));
        reviewCommands.execute(UUID.randomUUID(),replacementRefs,c->reviewCases.command(c,org,draft.id(),new com.elogistic.backend.merchant.api.OrganizationGovernanceCases.InformationInput(List.of("reason"),"Synthetic second information request"),8,UUID.randomUUID()));
        maker.execute(UUID.randomUUID(),replacementRefs,c->cases.command(c,org,draft.id(),new com.elogistic.backend.merchant.api.OrganizationGovernanceCases.ResponseInput(input.payload(),List.of(),"Synthetic second response",null),9,UUID.randomUUID()));
        maker.execute(UUID.randomUUID(),replacementRefs,c->cases.command(c,org,draft.id(),new com.elogistic.backend.merchant.api.OrganizationGovernanceCases.AssignmentInput(unassigned,"Synthetic reassignment"),10,UUID.randomUUID()));
        var afterAssignment=new com.elogistic.backend.merchant.api.OrganizationGovernanceCases.CaseDisclosureInput(11,"LIFECYCLE_REVIEW");assertThrows(com.elogistic.backend.merchant.api.GovernanceFailure.class,()->reviewCommands.execute(UUID.randomUUID(),replacementRefs,c->reviewCases.disclose(c,org,draft.id(),afterAssignment)));
        commandAdmission(unassigned,clock).execute(UUID.randomUUID(),replacementRefs,c->otherCases.disclose(c,org,draft.id(),afterAssignment));
        viewScope(reviewer,"ORGANIZATION",org,"REVOKE",1,"org.read","org.lifecycle.review","audit.read");assertThrows(RuntimeException.class,()->reviewCommands.execute(UUID.randomUUID(),replacementRefs,c->reviewCases.command(c,org,draft.id(),new com.elogistic.backend.merchant.api.OrganizationGovernanceCases.InformationInput(List.of("reason"),"Revoked reviewer"),11,UUID.randomUUID())));
        assertEquals(11,jdbc.queryForObject("SELECT version FROM merchant.organization_governance_case WHERE id=?",Integer.class,draft.id()));
        assertThrows(RuntimeException.class,()->reviewerReads.execute(c->reviewCases.policies(c,org)));
        assertThrows(RuntimeException.class,()->reviewCommands.execute(UUID.randomUUID(),refs,c->reviewCases.disclose(c,org,draft.id(),disclosureInput)));
        if(browser)AdminBrowserRuntime.runCases(source,name(admin),password,pair,onboarding,ready.governance(),cases,org);
    }
    private record ReadyMerchant(com.elogistic.backend.merchant.api.MerchantGovernance governance,com.elogistic.backend.workflow.internal.application.MerchantNoticeComposition notices,AdminCommandAdmission commands,AdminGovernanceReauthentication proofs,AdminCommandAdmission.ReferencedSubjects refs,UUID application,UUID applicant,BusinessContactPostgresCases workflow){}
    private ReadyMerchant readyMerchant(UUID existingPolicy,UUID existingReviewer)throws Exception{return readyMerchant(existingPolicy,existingReviewer,false);}
    private ReadyMerchant readyMerchant(UUID existingPolicy,UUID existingReviewer,boolean distinctBusiness)throws Exception{
        var proofs=stepUps();db.flyway("43",REALM).migrate();UUID policyId=existingPolicy==null?UUID.randomUUID():existingPolicy,reviewer=existingReviewer==null?account(Set.of(AdminReadContext.ENTRY,AdminReadContext.MERCHANT)):existingReviewer;
        Instant expiry=clock.instant().plusSeconds(3600).truncatedTo(java.time.temporal.ChronoUnit.MICROS);
        if(existingPolicy==null)try(var op=onboarding.governanceOperatorConnection()){
            var helper=new MerchantGovernancePostgresCases(db,source,onboarding,REALM,admin);helper.publish(op,UUID.randomUUID(),policyId,MerchantGovernancePostgresCases.policy(clock.instant().minusSeconds(30).truncatedTo(java.time.temporal.ChronoUnit.MICROS)));helper.scope(op,UUID.randomUUID(),policyId,"ISSUE",0,expiry,"application.read","application.assign","application.decide");
            new MerchantGovernancePostgresCases(db,source,onboarding,REALM,reviewer).scope(op,UUID.randomUUID(),policyId,"ISSUE",0,expiry,"application.read","application.review","application.evidence.read");
        }
        UUID applicant=customer();var workflow=new BusinessContactPostgresCases(source,applicant,password,clock);UUID app=existingPolicy==null?workflow.screenedEvidenceAndImmutableSubmission():workflow.screenedEvidenceAndImmutableSubmissionPreservingOrganizations(distinctBusiness?"SYNTHETIC-"+UUID.randomUUID():"SYNTHETIC-REG-1");byte[] encryption=new byte[32];Arrays.fill(encryption,(byte)72);var commands=commandAdmission(admin,clock);
        var governance=new com.elogistic.backend.merchant.internal.infrastructure.JdbcMerchantApplicationReviews(source,new com.elogistic.backend.merchant.internal.infrastructure.ProtectedMerchantFingerprints(Map.of("v1",key()),"v1"),new com.elogistic.backend.merchant.internal.infrastructure.ProtectedGovernancePayloads(Map.of("e1",encryption),"e1"),clock,readAdmission(admin),commands,workflow.contactFacts(),proofs);
        var worker=new com.elogistic.backend.notifications.internal.infrastructure.JdbcMerchantApplicationNotices(source,workflow.customerAdmission(),commands,new com.elogistic.backend.notifications.internal.infrastructure.ProtectedMerchantNoticeRecipient(Map.of("n1",encryption),"n1"),message->{throw new AssertionError("NO_TRANSPORT_IN_TRANSACTION");},clock);
        var notices=new com.elogistic.backend.workflow.internal.application.MerchantNoticeComposition(new com.elogistic.backend.merchant.internal.infrastructure.JdbcMerchantApplicationNoticeEvents(source,workflow.customerAdmission(),commands,governance),worker);
        var refs=new AdminCommandAdmission.ReferencedSubjects(Set.of(applicant),Set.of(reviewer));commands.execute(UUID.randomUUID(),refs,c->governance.assign(c,app,new com.elogistic.backend.merchant.api.MerchantGovernance.ReviewerAssignmentInput(reviewer,"Synthetic assignment"),3,UUID.randomUUID()));
        var review=commandAdmission(reviewer,clock);review.execute(UUID.randomUUID(),refs,c->governance.review(c,app,new com.elogistic.backend.merchant.api.MerchantGovernance.ReviewInput("BEGIN",1,"1",null,"Synthetic begin"),4,UUID.randomUUID()));review.execute(UUID.randomUUID(),refs,c->governance.review(c,app,new com.elogistic.backend.merchant.api.MerchantGovernance.ReviewInput("READY_FOR_DECISION",1,"1",new com.elogistic.backend.merchant.api.MerchantGovernance.Checklist(true,true,true,true,true),"Synthetic complete"),5,UUID.randomUUID()));
        return new ReadyMerchant(governance,notices,commands,proofs,refs,app,applicant,workflow);
    }
    private com.elogistic.backend.merchant.api.MerchantGovernance.DecisionInput decision(ReadyMerchant ready,String operation)throws Exception{
        String reason="Synthetic "+operation;String digest=HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(JSON.writeValueAsString(new TreeMap<>(Map.of("operation",operation,"target",ready.application().toString(),"originalVersion",6L,"revision",1L,"policy","1","reason",reason))).getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        var target=new com.elogistic.backend.merchant.api.MerchantGovernance.Target(com.elogistic.backend.merchant.api.MerchantGovernance.Kind.APPLICATION,ready.application());
        String authority=ready.commands().execute(UUID.randomUUID(),ready.refs(),c->ready.governance().withScope(c,target,scope->scope.version()));
        var proof=ready.commands().execute(UUID.randomUUID(),ready.refs(),c->ready.proofs().request(c,new AdminGovernanceReauthentication.AdminStepUpInput(operation,ready.application(),6,digest,password),authority,UUID.randomUUID()));
        return new com.elogistic.backend.merchant.api.MerchantGovernance.DecisionInput(1,"1",proof.proofId(),reason);
    }
    private com.elogistic.backend.merchant.api.MerchantGovernance.ApplicationSummary decide(ReadyMerchant ready,String op,com.elogistic.backend.merchant.api.MerchantGovernance.DecisionInput input,UUID key){return ready.commands().execute(UUID.randomUUID(),ready.refs(),c->{var result=op.equals("APPROVE")?ready.governance().approve(c,ready.application(),input,6,key):ready.governance().reject(c,ready.application(),input,6,key);ready.notices().after(c,ready.application(),op,key);return result;});}
    public void merchantV43LegacyKeyClearanceFailsClosed()throws Exception{
        var ready=readyMerchant(null,null);UUID legacy=UUID.randomUUID();try(var c=db.migrationConnection();var st=c.createStatement()){
            st.execute("SET ROLE "+db.owner());st.execute("SELECT set_config('argopost.operator_environment','TEST',false)");
            try(var q=c.prepareStatement("SELECT merchant.configure_local_workspace(?,'PROVISION',?,?,?,'OWNER',ARRAY['dashboard.read'],?)")){q.setObject(1,UUID.randomUUID());q.setObject(2,legacy);q.setString(3,"Synthetic legacy organization");q.setObject(4,ready.applicant());q.setString(5,"Synthetic legacy conflict test");q.execute();}
        }
        var input=decision(ready,"APPROVE");var failed=assertThrows(com.elogistic.backend.merchant.api.GovernanceFailure.class,()->decide(ready,"APPROVE",input,UUID.randomUUID()));assertEquals(com.elogistic.backend.merchant.api.GovernanceFailure.Code.LEGACY_KEY_CLEARANCE_REQUIRED,failed.code());
        assertEquals("UNDER_REVIEW",jdbc.queryForObject("SELECT state FROM merchant.application WHERE id=?",String.class,ready.application()));assertEquals(0,jdbc.queryForObject("SELECT count(*) FROM merchant.application_activation",Integer.class));assertEquals(0,jdbc.queryForObject("SELECT count(*) FROM notification.merchant_application_notice",Integer.class));assertEquals("LOCAL_OPERATOR_PROVISIONED",jdbc.queryForObject("SELECT admission_origin FROM merchant.organization WHERE id=?",String.class,legacy));
        assertEquals(0,jdbc.queryForObject("SELECT count(*) FROM identity.admin_governance_reauthentication WHERE id=? AND consumed_at IS NOT NULL",Integer.class,input.reauthenticationProofId()));
    }
    public void merchantV43OppositeDecisionsCommitOnlyOneOutcome()throws Exception{
        var ready=readyMerchant(null,null);var approve=decision(ready,"APPROVE");var reject=decision(ready,"REJECT");UUID approveKey=UUID.randomUUID(),rejectKey=UUID.randomUUID();
        var outcomes=new java.util.concurrent.ConcurrentLinkedQueue<com.elogistic.backend.merchant.api.MerchantGovernance.ApplicationSummary>();var failures=new java.util.concurrent.ConcurrentLinkedQueue<com.elogistic.backend.merchant.api.GovernanceFailure.Code>();
        try(var pool=java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor()){
            var barrier=new java.util.concurrent.CyclicBarrier(2);List<java.util.concurrent.Future<?>> work=new ArrayList<>();for(String op:List.of("APPROVE","REJECT"))work.add(pool.submit(()->{try{barrier.await(10,java.util.concurrent.TimeUnit.SECONDS);outcomes.add(decide(ready,op,op.equals("APPROVE")?approve:reject,op.equals("APPROVE")?approveKey:rejectKey));}catch(com.elogistic.backend.merchant.api.GovernanceFailure expected){failures.add(expected.code());}catch(Exception failure){throw new AssertionError(failure);}}));for(var result:work)result.get(30,java.util.concurrent.TimeUnit.SECONDS);
        }
        assertEquals(1,outcomes.size());assertEquals(1,failures.size());assertTrue(Set.of(com.elogistic.backend.merchant.api.GovernanceFailure.Code.PRECONDITION_STALE,com.elogistic.backend.merchant.api.GovernanceFailure.Code.STATE_CONFLICT).contains(failures.peek()));
        var result=outcomes.peek();boolean approved=result.state().equals("APPROVED");assertEquals(result,decide(ready,approved?"APPROVE":"REJECT",approved?approve:reject,approved?approveKey:rejectKey));
        assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM merchant.governance_event WHERE application_id=? AND operation IN ('APPROVE','REJECT')",Integer.class,ready.application()));assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM notification.merchant_application_notice",Integer.class));assertEquals(approved?1:0,jdbc.queryForObject("SELECT count(*) FROM merchant.organization",Integer.class));
    }
    public void merchantV43ActivatedBusinessKeyCannotBeReused()throws Exception{
        var first=readyMerchant(null,null);var accepted=decide(first,"APPROVE",decision(first,"APPROVE"),UUID.randomUUID());UUID policy=jdbc.queryForObject("SELECT policy_id FROM merchant.application WHERE id=?",UUID.class,first.application());UUID reviewer=first.refs().admins().iterator().next();
        var second=readyMerchant(policy,reviewer);var input=decision(second,"APPROVE");var conflict=assertThrows(com.elogistic.backend.merchant.api.GovernanceFailure.class,()->decide(second,"APPROVE",input,UUID.randomUUID()));assertEquals(com.elogistic.backend.merchant.api.GovernanceFailure.Code.BUSINESS_CONFLICT,conflict.code());
        assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM merchant.organization",Integer.class));assertEquals(accepted.activation().applicationId(),jdbc.queryForObject("SELECT application_id FROM merchant.business_key_registry",UUID.class));assertEquals("UNDER_REVIEW",jdbc.queryForObject("SELECT state FROM merchant.application WHERE id=?",String.class,second.application()));
    }
    public void merchantV43RejectionCommitsOneBoundNotice()throws Exception{
        var ready=readyMerchant(null,null);var input=decision(ready,"REJECT");UUID key=UUID.randomUUID();
        var result=decide(ready,"REJECT",input,key);assertEquals("REJECTED",result.state());assertNull(result.activation());assertEquals(result,decide(ready,"REJECT",input,key));
        assertEquals(0,jdbc.queryForObject("SELECT count(*) FROM merchant.organization",Integer.class));assertEquals(0,jdbc.queryForObject("SELECT count(*) FROM merchant.application_activation",Integer.class));
        assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM notification.merchant_application_notice WHERE application_id=? AND template='REJECTED' AND state='QUEUED'",Integer.class,ready.application()));
        byte[] encryption=new byte[32];Arrays.fill(encryption,(byte)72);var sent=new java.util.concurrent.atomic.AtomicReference<com.elogistic.backend.platform.api.MerchantNoticeMail.Message>();
        var contacts=new BusinessContactPostgresCases(source,ready.applicant(),password,clock);
        var worker=new com.elogistic.backend.notifications.internal.infrastructure.JdbcMerchantApplicationNotices(source,contacts.customerAdmission(),ready.commands(),new com.elogistic.backend.notifications.internal.infrastructure.ProtectedMerchantNoticeRecipient(Map.of("n1",encryption),"n1"),message->{assertFalse(org.springframework.transaction.support.TransactionSynchronizationManager.isActualTransactionActive());assertTrue(sent.compareAndSet(null,message));return new com.elogistic.backend.platform.api.MerchantNoticeMail.Outcome(com.elogistic.backend.platform.api.MerchantNoticeMail.Kind.ACCEPTED_BY_PROVIDER,"TEST:"+message.id(),true);},clock);
        assertTrue(worker.runOne());assertFalse(worker.runOne());assertEquals(ready.application(),sent.get().applicationId());assertEquals(com.elogistic.backend.platform.api.MerchantNoticeMail.Template.REJECTED,sent.get().template());
        assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM notification.merchant_application_notice WHERE state='ACCEPTED_BY_PROVIDER'",Integer.class));
    }
    public void merchantV43ScopedAuditProjectsMetadataOnly()throws Exception{
        var ready=readyMerchant(null,null);decide(ready,"REJECT",decision(ready,"REJECT"),UUID.randomUUID());
        UUID policy=jdbc.queryForObject("SELECT policy_id FROM merchant.application WHERE id=?",UUID.class,ready.application());
        UUID auditor=account(Set.of(AdminReadContext.ENTRY));var read=readAdmission(auditor);Instant expiry=clock.instant().plusSeconds(3600).truncatedTo(java.time.temporal.ChronoUnit.MICROS);
        try(var op=onboarding.governanceOperatorConnection()){new MerchantGovernancePostgresCases(db,source,onboarding,REALM,auditor).scope(op,UUID.randomUUID(),policy,"ISSUE",0,expiry,"audit.read");}
        var query=new com.elogistic.backend.merchant.api.MerchantGovernance.AuditQuery("MERCHANT_APPLICATION",null,clock.instant().minusSeconds(60),clock.instant().plusSeconds(1),0,2);
        var page=read.execute(c->ready.governance().audit(c,query));assertEquals(2,page.items().size());assertTrue(page.hasNext());
        assertTrue(page.items().stream().allMatch(e->e.targetId().equals(ready.application())&&e.targetType().equals("APPLICATION")));
        var next=read.execute(c->ready.governance().audit(c,new com.elogistic.backend.merchant.api.MerchantGovernance.AuditQuery(query.domain(),null,query.from(),query.to(),1,2)));assertFalse(next.items().isEmpty());
        var ids=new HashSet<>(page.items().stream().map(com.elogistic.backend.merchant.api.MerchantGovernance.AuditItem::id).toList());assertTrue(next.items().stream().noneMatch(e->ids.contains(e.id())));
        var item=JSON.readTree(JSON.writeValueAsString(page.items().getFirst()));assertEquals(Set.of("id","actorId","operation","targetType","targetId","priorVersion","newVersion","revision","policyVersion","reasonCode","occurredAt","requestId"),item.propertyNames());
        assertThrows(com.elogistic.backend.identity.api.admin.AdminFailure.class,()->read.execute(c->ready.governance().application(c,ready.application())));
        var missing=assertThrows(com.elogistic.backend.merchant.api.GovernanceFailure.class,()->read.execute(c->ready.governance().audit(c,new com.elogistic.backend.merchant.api.MerchantGovernance.AuditQuery(query.domain(),UUID.randomUUID(),query.from(),query.to(),0,20))));assertEquals(com.elogistic.backend.merchant.api.GovernanceFailure.Code.RESOURCE_NOT_FOUND,missing.code());
        var escaped=new java.util.concurrent.atomic.AtomicReference<AdminReadContext>();read.execute(c->{escaped.set(c);return null;});assertThrows(AdminFailure.class,()->ready.governance().audit(escaped.get(),query));
        var http=MockMvcBuilders.standaloneSetup(new MerchantAuditHttp(admission,ready.governance())).addFilters(new ProductionReadGuard(tokens,requests,sessions,ORIGIN).browser(auth)).build();
        var browser=login(auditor);String path="/api/v1/admin/audit";
        var response=http.perform(get(path).secure(true).header("Origin",ORIGIN).header("Authorization","Bearer "+browser.access()).param("domain",query.domain()).param("from",query.from().toString()).param("to",query.to().toString()).param("size","2")).andReturn().getResponse();
        assertEquals(200,response.getStatus());assertEquals("no-store",response.getHeader("Cache-Control"));assertEquals(2,JSON.readTree(response.getContentAsString()).get("items").size());
        assertEquals(400,http.perform(get(path).secure(true).header("Authorization","Bearer "+browser.access()).param("domain",query.domain()).param("from",query.from().toString()).param("to",query.to().toString()).param("actorId",admin.toString())).andReturn().getResponse().getStatus());
        assertEquals(401,http.perform(get(path).secure(true)).andReturn().getResponse().getStatus());

        try(var op=onboarding.governanceOperatorConnection()){new MerchantGovernancePostgresCases(db,source,onboarding,REALM,auditor).scope(op,UUID.randomUUID(),policy,"REVOKE",1,expiry,"audit.read");}
        assertThrows(com.elogistic.backend.merchant.api.GovernanceFailure.class,()->read.execute(c->ready.governance().audit(c,query)));
    }
    public void merchantV43ApplicationFactsVerifyIdentityAndCanonicalCohort()throws Exception{
        var ready=readyMerchant(null,null);decide(ready,"REJECT",decision(ready,"REJECT"),UUID.randomUUID());
        var reader=new AdminApplicationSource(source,readAdmission(admin),ready.governance(),clock);
        var query=new com.elogistic.backend.merchant.api.AdminApplicationFacts.ApplicationCohort(clock.instant().minusSeconds(60),clock.instant().plusSeconds(1),null,null);
        java.util.function.Function<AdminReadContext,com.elogistic.backend.merchant.api.AdminApplicationFacts.Access> access=c->new com.elogistic.backend.merchant.api.AdminApplicationFacts.Access(){public void requireApplicationFacts(){c.require(AdminReadContext.MERCHANT);}public AdminReadContext identityContext(){return c;}};
        var read=readAdmission(admin);var captured=read.execute(c->reader.capture(access.apply(c),query,clock.instant()));
        assertEquals("AVAILABLE",captured.availability());assertEquals(1,captured.rows().size());var row=captured.rows().getFirst();assertEquals(ready.application(),row.id());assertEquals("REJECTED",row.state());assertNotNull(row.terminalAt());assertFalse(row.terminalAt().isBefore(row.submittedAt()));assertNotNull(row.lastSourceEventAt());
        assertEquals(1,read.execute(c->reader.page(access.apply(c),query,new com.elogistic.backend.merchant.api.AdminApplicationFacts.Selector("APPLICATIONS_REJECTED",clock.instant(),captured.resourceScopeVersion(),captured.fingerprint()),0,20)).items().size());
        assertEquals("SOURCE_NOT_IMPLEMENTED",read.execute(c->reader.availability(access.apply(c))));
        assertThrows(AdminFailure.class,()->read.execute(c->reader.capture(new com.elogistic.backend.merchant.api.AdminApplicationFacts.Access(){public void requireApplicationFacts(){}},query,clock.instant())));
        read.execute(c->{
            var forwarding=(AdminReadContext)java.lang.reflect.Proxy.newProxyInstance(AdminReadContext.class.getClassLoader(),new Class<?>[]{AdminReadContext.class},(proxy,method,args)->method.getName().equals("accountId")?UUID.randomUUID():method.invoke(c,args));
            assertThrows(AdminFailure.class,()->reader.capture(access.apply(forwarding),query,clock.instant()));
            var otherSource=new org.springframework.jdbc.datasource.DriverManagerDataSource(db.url(),db.runtime(),db.runtimePassword());
            assertThrows(AdminFailure.class,()->new AdminApplicationSource(otherSource,read,ready.governance(),clock).capture(access.apply(c),query,clock.instant()));
            var suspended=new org.springframework.transaction.support.TransactionTemplate(new org.springframework.jdbc.support.JdbcTransactionManager(source));suspended.setPropagationBehavior(org.springframework.transaction.TransactionDefinition.PROPAGATION_REQUIRES_NEW);
            suspended.execute(status->{assertThrows(AdminFailure.class,()->reader.capture(access.apply(c),query,clock.instant()));return null;});return null;
        });
        UUID unscoped=account(Set.of(AdminReadContext.ENTRY,AdminReadContext.MERCHANT));var absent=readAdmission(unscoped).execute(c->reader.capture(access.apply(c),query,clock.instant()));assertEquals("POLICY_NOT_CONFIGURED",absent.availability());assertTrue(absent.rows().isEmpty());

        var escaped=new java.util.concurrent.atomic.AtomicReference<AdminReadContext>();read.execute(c->{escaped.set(c);return null;});assertThrows(AdminFailure.class,()->reader.capture(access.apply(escaped.get()),query,clock.instant()));
        var changed=assertThrows(com.elogistic.backend.merchant.api.GovernanceFailure.class,()->read.execute(c->reader.page(access.apply(c),query,new com.elogistic.backend.merchant.api.AdminApplicationFacts.Selector("APPLICATIONS_REJECTED",clock.instant(),"0".repeat(64),captured.fingerprint()),0,20)));assertEquals(com.elogistic.backend.merchant.api.GovernanceFailure.Code.SNAPSHOT_EXPIRED,changed.code());
    }
    public void merchantV43DashboardV2CanonicalSnapshotAndCompatibility()throws Exception{
        var ready=readyMerchant(null,null);decide(ready,"REJECT",decision(ready,"REJECT"),UUID.randomUUID());UUID policyId=jdbc.queryForObject("SELECT policy_id FROM merchant.application WHERE id=?",UUID.class,ready.application());
        var pending=readyMerchant(policyId,ready.refs().admins().iterator().next());book(customer(),1,clock);
        var reader=new AdminApplicationSource(source,admission,ready.governance(),clock);
        var controller=new AdminDashboardHttp(admission,reports,new JdbcAdminShipmentFacts(source),new JdbcAdminObligationFacts(source),reader,new AdminNetworkSource(),new JdbcAdminOversightReads(source),clock,()->ready.governance());
        var http=MockMvcBuilders.standaloneSetup(controller).addFilters(new ProductionReadGuard(tokens,requests,sessions,ORIGIN).browser(auth)).build();var browser=login(admin);
        String media="application/vnd.argopost.admin-dashboard.v2+json",path="/api/v1/admin/dashboard",parameters=query();
        java.util.function.BiFunction<String,String,org.springframework.mock.web.MockHttpServletResponse> get=(url,accept)->{try{return http.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(url).secure(true).header("Origin",ORIGIN).header("Authorization","Bearer "+browser.access()).header("Accept",accept)).andReturn().getResponse();}catch(Exception failure){throw new AssertionError(failure);}};
        var v1=get.apply(path+"?"+parameters,"application/json");assertEquals(200,v1.getStatus());assertEquals(Set.of("query","generatedAt","snapshotId","sections"),JSON.readTree(v1.getContentAsString()).propertyNames());assertTrue(v1.getContentAsString().contains("SUBMITTED_APPLICATION_SOURCE_REQUIRED"));
        var result=get.apply(path+"?"+parameters,media);assertEquals(200,result.getStatus(),result.getContentAsString());assertTrue(result.getContentType().startsWith(media));assertEquals("Accept",result.getHeader("Vary"));var body=JSON.readTree(result.getContentAsString());assertEquals(Set.of("schemaVersion","query","generatedAt","snapshotId","sections","resourceScopeVersion","metricMetadata"),body.propertyNames());assertEquals("ADMIN_DASHBOARD_2",body.get("schemaVersion").asString());
        var sections=body.get("sections");tools.jackson.databind.JsonNode merchant=null;for(var section:sections)if(section.get("key").asString().equals("MERCHANT"))merchant=section;assertNotNull(merchant);assertEquals("AVAILABLE",merchant.get("availability").asString());Map<String,String> values=new HashMap<>();for(var metric:merchant.get("metrics"))values.put(metric.get("key").asString(),metric.get("value").isNull()?null:metric.get("value").asString());assertEquals("2",values.get("APPLICATIONS_SUBMITTED"));assertEquals("1",values.get("APPLICATIONS_UNDER_REVIEW"));assertEquals("1",values.get("APPLICATIONS_REJECTED"));
        for(var metadata:body.get("metricMetadata")){assertEquals("ARGO_ADMIN_METRICS_2",metadata.get("definitionVersion").asString());assertEquals("Asia/Ho_Chi_Minh",metadata.get("timeZone").asString());assertEquals("CURRENT_TRANSACTION",metadata.get("freshness").asString());if(metadata.get("metricKey").asString().equals("APPLICATION_PROCESSING_AVG_MS")){assertEquals("1",metadata.get("denominator").asString());assertEquals("1",metadata.get("sampleCount").asString());assertFalse(metadata.get("lastSourceEventAt").isNull());}}
        String snapshot=body.get("snapshotId").asString(),selector=parameters+"&snapshotId="+snapshot;
        var page=get.apply(path+"/merchant-applications?"+selector+"&metric=APPLICATIONS_REJECTED&page=0&size=20",media);assertEquals(200,page.getStatus(),page.getContentAsString());var rows=JSON.readTree(page.getContentAsString());assertEquals(1,rows.get("items").size());assertEquals(ready.application().toString(),rows.get("items").get(0).get("id").asString());assertEquals(Set.of("id","state","submittedAt","updatedAt"),rows.get("items").get(0).propertyNames());assertEquals(body.get("resourceScopeVersion"),rows.get("resourceScopeVersion"));
        var shipping=get.apply(path+"/shipments?"+selector+"&metric=BOOKINGS&page=0&size=20",media);assertEquals(200,shipping.getStatus());assertEquals(1,JSON.readTree(shipping.getContentAsString()).get("items").size());
        assertEquals(200,get.apply(path+"/context",media).getStatus());assertEquals(200,get.apply(path+"/platform",media).getStatus());assertEquals(422,get.apply(path+"/network?"+selector+"&metric=STATIONS",media).getStatus());
        var unknown=get.apply(path+"?"+parameters,"application/vnd.argopost.admin-dashboard.v3+json");assertEquals(406,unknown.getStatus());assertEquals("NOT_ACCEPTABLE",JSON.readTree(unknown.getContentAsString()).get("code").asString());assertEquals(400,get.apply(path+"?"+parameters+"&shipmentStatus=BOOKED&applicationState=REJECTED",media).getStatus());
        assertEquals(409,get.apply(path+"/merchant-applications?"+selector+"&metric=APPLICATIONS_REJECTED&applicationState=REJECTED",media).getStatus());
        var filtered=get.apply(path+"?"+parameters+"&applicationState=UNDER_REVIEW&policyId="+policyId,media);assertEquals(200,filtered.getStatus());String filteredSnapshot=JSON.readTree(filtered.getContentAsString()).get("snapshotId").asString();var filteredRows=get.apply(path+"/merchant-applications?"+parameters+"&applicationState=UNDER_REVIEW&policyId="+policyId+"&snapshotId="+filteredSnapshot+"&metric=APPLICATIONS_SUBMITTED",media);assertEquals(200,filteredRows.getStatus());assertEquals(pending.application().toString(),JSON.readTree(filteredRows.getContentAsString()).get("items").get(0).get("id").asString());
        Instant expiry=jdbc.queryForObject("SELECT expires_at FROM merchant.business_reviewer_scope WHERE account_id=? AND policy_id=? AND state='ACTIVE'",java.sql.Timestamp.class,admin,policyId).toInstant();try(var operator=onboarding.governanceOperatorConnection()){new MerchantGovernancePostgresCases(db,source,onboarding,REALM,admin).scope(operator,UUID.randomUUID(),policyId,"REVOKE",1,expiry,"application.read","application.assign","application.decide");}
        assertEquals(409,get.apply(path+"/merchant-applications?"+selector+"&metric=APPLICATIONS_REJECTED",media).getStatus());var revoked=get.apply(path+"?"+parameters,media);assertEquals(200,revoked.getStatus());var unavailable=JSON.readTree(revoked.getContentAsString()).get("sections").get(0);assertEquals("POLICY_NOT_CONFIGURED",unavailable.get("availability").asString());assertEquals(0,unavailable.get("metrics").size());
    }
    public void merchantWorkAndOrganizationAuditCurrentAuthority()throws Exception{
        var ready=readyMerchant(null,null);UUID policy=jdbc.queryForObject("SELECT policy_id FROM merchant.application WHERE id=?",UUID.class,ready.application()),reviewer=ready.refs().admins().iterator().next();
        var helper=new MerchantGovernancePostgresCases(db,source,onboarding,REALM,admin);Instant expiry=jdbc.queryForObject("SELECT expires_at FROM merchant.business_reviewer_scope WHERE account_id=? AND policy_id=? AND state='ACTIVE'",java.sql.Timestamp.class,admin,policy).toInstant();
        String[] original={"application.read","application.assign","application.decide"},expanded={"application.read","application.assign","application.decide","task.read","audit.read","org.read","org.suspend","org.reinstate"};
        try(var op=onboarding.governanceOperatorConnection()){helper.scope(op,UUID.randomUUID(),policy,"REVOKE",1,expiry,original);helper.scope(op,UUID.randomUUID(),policy,"ISSUE",0,expiry,expanded);}
        var query=new com.elogistic.backend.merchant.api.MerchantGovernance.WorkQuery(null,null,0,1);var first=ready.governance().work(UUID.randomUUID(),query);assertEquals(1,first.items().size());assertFalse(first.hasNext());var item=first.items().getFirst();assertEquals("MERCHANT_DECISION",item.kind());assertEquals(ready.application(),item.applicationId());assertNull(item.dueAt());assertEquals(List.of("REJECT","APPROVE"),item.allowedActions());assertEquals(item.id(),ready.governance().work(UUID.randomUUID(),query).items().getFirst().id());
        assertTrue(ready.governance().work(UUID.randomUUID(),new com.elogistic.backend.merchant.api.MerchantGovernance.WorkQuery("MERCHANT_REVIEW",policy,0,1)).items().isEmpty());assertTrue(ready.governance().work(UUID.randomUUID(),new com.elogistic.backend.merchant.api.MerchantGovernance.WorkQuery(null,policy,1,1)).items().isEmpty());
        assertThrows(com.elogistic.backend.merchant.api.GovernanceFailure.class,()->ready.governance().work(UUID.randomUUID(),new com.elogistic.backend.merchant.api.MerchantGovernance.WorkQuery(null,UUID.randomUUID(),0,20)));
        var mvc=MockMvcBuilders.standaloneSetup(new MerchantWorkHttp(admission,ready.governance())).addFilters(new ProductionReadGuard(tokens,requests,sessions,ORIGIN).browser(auth)).build();var browser=login(admin);
        var http=mvc.perform(get("/api/v1/admin/work-items").secure(true).header("Authorization","Bearer "+browser.access())).andReturn().getResponse();assertEquals(200,http.getStatus(),http.getContentAsString());assertEquals("no-store",http.getHeader("Cache-Control"));assertEquals(1,JSON.readTree(http.getContentAsString()).get("items").size());assertFalse(http.getContentAsString().contains("legalIdentifier"));
        assertEquals(400,mvc.perform(get("/api/v1/admin/work-items?kind=FAKE").secure(true).header("Authorization","Bearer "+browser.access())).andReturn().getResponse().getStatus());assertEquals(401,mvc.perform(get("/api/v1/admin/work-items").secure(true)).andReturn().getResponse().getStatus());
        // Identity revocation is resolved through the canonical owner before pagination, not by Merchant SQL.
        var reviewerHelper=new MerchantGovernancePostgresCases(db,source,onboarding,REALM,reviewer);Instant reviewerExpiry=jdbc.queryForObject("SELECT expires_at FROM merchant.business_reviewer_scope WHERE account_id=? AND policy_id=? AND state='ACTIVE'",java.sql.Timestamp.class,reviewer,policy).toInstant();
        try(var op=onboarding.governanceOperatorConnection()){reviewerHelper.scope(op,UUID.randomUUID(),policy,"REVOKE",1,reviewerExpiry,"application.read","application.review","application.evidence.read");}
        assertTrue(ready.governance().work(UUID.randomUUID(),query).items().isEmpty());
        // Reassign using canonical mutation after a fresh scope, preserving the original revision.
        try(var op=onboarding.governanceOperatorConnection()){reviewerHelper.scope(op,UUID.randomUUID(),policy,"ISSUE",0,reviewerExpiry,"application.read","application.review","application.evidence.read");}
        // Old assignment remains bound to revoked scope; it cannot become eligible by regrant alone.
        assertTrue(ready.governance().work(UUID.randomUUID(),query).items().isEmpty());
        // A second genuine application supplies Organization positive audit without weakening the revoked assignment.
        var active=readyMerchant(policy,reviewer,true);var approved=decide(active,"APPROVE",decision(active,"APPROVE"),UUID.randomUUID());UUID org=approved.activation().organizationId();
        for(String operation:List.of("SUSPEND","REINSTATE")){
            long version=operation.equals("SUSPEND")?1:2;String reason="Synthetic organization audit "+operation;var target=new com.elogistic.backend.merchant.api.MerchantGovernance.Target(com.elogistic.backend.merchant.api.MerchantGovernance.Kind.ORGANIZATION,org);
            String digest=HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(JSON.writeValueAsString(new TreeMap<>(Map.of("operation",operation,"target",org.toString(),"originalVersion",version,"reason",reason))).getBytes(java.nio.charset.StandardCharsets.UTF_8)));
            String authority=active.commands().execute(UUID.randomUUID(),active.refs(),c->active.governance().withScope(c,target,scope->scope.version()));var proof=active.commands().execute(UUID.randomUUID(),active.refs(),c->active.proofs().request(c,new AdminGovernanceReauthentication.AdminStepUpInput(operation,org,version,digest,password),authority,UUID.randomUUID()));
            active.commands().execute(UUID.randomUUID(),active.refs(),c->{var input=new com.elogistic.backend.merchant.api.MerchantGovernance.LifecycleInput(reason,proof.proofId());return operation.equals("SUSPEND")?active.governance().suspend(c,org,input,version,UUID.randomUUID()):active.governance().reinstate(c,org,input,version,UUID.randomUUID());});
        }
        var auditQuery=new com.elogistic.backend.merchant.api.MerchantGovernance.AuditQuery("MERCHANT_ORGANIZATION",org,clock.instant().minusSeconds(3600),clock.instant().plusSeconds(3600),0,1);var audit=readAdmission(admin).execute(c->active.governance().audit(c,auditQuery));assertEquals(1,audit.items().size());assertTrue(audit.hasNext());assertEquals(org,audit.items().getFirst().targetId());assertEquals(Set.of("SUSPEND","REINSTATE"),readAdmission(admin).execute(c->active.governance().audit(c,new com.elogistic.backend.merchant.api.MerchantGovernance.AuditQuery(auditQuery.domain(),org,auditQuery.from(),auditQuery.to(),0,20))).items().stream().map(com.elogistic.backend.merchant.api.MerchantGovernance.AuditItem::operation).collect(java.util.stream.Collectors.toSet()));
        UUID auditor=account(Set.of(AdminReadContext.ENTRY));try(var op=onboarding.governanceOperatorConnection();var sql=op.prepareStatement("SELECT merchant.configure_local_reviewer_scope(?,?,?,'ORGANIZATION',?,ARRAY['audit.read'],'ISSUE',0,?,'Synthetic organization-only audit')")){sql.setObject(1,UUID.randomUUID());sql.setObject(2,auditor);sql.setObject(3,REALM);sql.setObject(4,org);sql.setTimestamp(5,java.sql.Timestamp.from(expiry));sql.execute();}
        var reads=readAdmission(auditor);assertEquals(1,reads.execute(c->active.governance().audit(c,auditQuery)).items().size());assertThrows(AdminFailure.class,()->reads.execute(c->active.governance().organization(c,org)));assertThrows(com.elogistic.backend.merchant.api.GovernanceFailure.class,()->reads.execute(c->active.governance().audit(c,new com.elogistic.backend.merchant.api.MerchantGovernance.AuditQuery(auditQuery.domain(),UUID.randomUUID(),auditQuery.from(),auditQuery.to(),0,20))));
        assertTrue(ready.governance().work(UUID.randomUUID(),query).items().isEmpty());
    }
    public void adminCoreRealHttpsCustomerToIndependentActivation()throws Exception{
        adminCoreRealHttpsCustomerToIndependentActivation(false);
    }
    public void biaRealHttpsCustomerApplicationToBusinessStation()throws Exception{adminCoreRealHttpsCustomerToIndependentActivation(true);}
    private void adminCoreRealHttpsCustomerToIndependentActivation(boolean shipping)throws Exception{
        stepUps();db.flyway("43",REALM).migrate();UUID policy=UUID.randomUUID(),reviewer=account(Set.of(AdminReadContext.ENTRY,AdminReadContext.MERCHANT)),applicant=customer();Instant expiry=clock.instant().plusSeconds(3600).truncatedTo(java.time.temporal.ChronoUnit.MICROS);
        try(var op=onboarding.governanceOperatorConnection()){
            var helper=new MerchantGovernancePostgresCases(db,source,onboarding,REALM,admin);helper.publish(op,UUID.randomUUID(),policy,MerchantGovernancePostgresCases.policy(clock.instant().minusSeconds(30).truncatedTo(java.time.temporal.ChronoUnit.MICROS)));helper.scope(op,UUID.randomUUID(),policy,"ISSUE",0,expiry,"application.read","application.assign","application.decide","org.read","audit.read","task.read");new MerchantGovernancePostgresCases(db,source,onboarding,REALM,reviewer).scope(op,UUID.randomUUID(),policy,"ISSUE",0,expiry,"application.read","application.review","application.evidence.read","task.read","audit.read");
        }
        assertEquals(0,jdbc.queryForObject("SELECT count(*) FROM merchant.application",Integer.class));
        var identities=Map.<String,Object>of("customerEmail",name(applicant)+"@example.invalid","reviewer",name(reviewer),"decider",name(admin),"reviewerId",reviewer.toString());
        if(shipping){db.flyway("latest",REALM).migrate();AdminCoreBrowserRuntime.runBusiness(source,password,pair,onboarding,identities,db);assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM dispatch.pilot_intake_acceptance WHERE owner_type='BUSINESS'",Integer.class));assertEquals(2,jdbc.queryForObject("SELECT count(*) FROM dispatch.pilot_custody",Integer.class));assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM finance.pilot_shipping_obligation WHERE owner_type='BUSINESS' AND state='UNPAID'",Integer.class));}
        else AdminCoreBrowserRuntime.run(source,password,pair,onboarding,identities);
        assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM merchant.governance_event WHERE operation='READY_FOR_DECISION'",Integer.class));
        assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM merchant.application WHERE state='APPROVED'",Integer.class));assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM merchant.application_activation",Integer.class));assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM merchant.organization WHERE state='ACTIVE'",Integer.class));assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM merchant.membership WHERE account_id=? AND role='OWNER' AND state='ACTIVE'",Integer.class,applicant));assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM notification.merchant_application_notice WHERE template='APPROVED' AND state='ACCEPTED_BY_PROVIDER'",Integer.class));
    }
    public void merchantV43SchemaAndWriterAcl()throws Exception{
        stepUps();db.flyway("43",REALM).migrate();
        for(String table:List.of("merchant.business_key_registry","merchant.application_activation","notification.merchant_application_notice")){assertNotNull(jdbc.queryForObject("SELECT to_regclass(?)::text",String.class,table));assertEquals(0,jdbc.queryForObject("SELECT count(*) FROM "+table,Integer.class));}
        String activation="merchant.activate_approved_application(uuid,uuid,bigint,bigint,uuid,uuid)",origin="merchant.application_origin_admitted(uuid)",notice="merchant.application_notice_bound(uuid,uuid,uuid,text)";
        assertTrue(jdbc.queryForObject("SELECT has_function_privilege(current_user,?,'EXECUTE')",Boolean.class,activation));assertTrue(jdbc.queryForObject("SELECT has_function_privilege(current_user,?,'EXECUTE')",Boolean.class,origin));assertFalse(jdbc.queryForObject("SELECT has_function_privilege(current_user,?,'EXECUTE')",Boolean.class,notice));
        assertFalse(jdbc.queryForObject("SELECT pg_has_role(current_user,'merchant_governance_writer','MEMBER')",Boolean.class));
        assertEquals("merchant_governance_writer",jdbc.queryForObject("SELECT pg_get_userbyid(proowner) FROM pg_proc WHERE oid=?::regprocedure",String.class,activation));assertTrue(jdbc.queryForObject("SELECT prosecdef FROM pg_proc WHERE oid=?::regprocedure",Boolean.class,activation));
        assertThrows(org.springframework.dao.DataAccessException.class,()->jdbc.queryForObject("SELECT merchant.activate_approved_application(?,?,1,1,?,?)",UUID.class,UUID.randomUUID(),UUID.randomUUID(),UUID.randomUUID(),UUID.randomUUID()));
        assertThrows(org.springframework.dao.DataAccessException.class,()->jdbc.execute("SET ROLE merchant_governance_writer"));
        assertThrows(org.springframework.dao.DataAccessException.class,()->jdbc.execute("TRUNCATE merchant.application_activation"));
        assertThrows(org.springframework.dao.DataAccessException.class,()->jdbc.execute("TRUNCATE notification.merchant_application_notice"));
        for(String function:List.of("merchant.check_team_member_insert()","merchant.check_team_grant_mutation()","merchant.check_team_authority_change()")){assertNotNull(jdbc.queryForObject("SELECT pg_get_functiondef(?::regprocedure)",String.class,function));}
    }
    public void merchantV42RealHttpsReviewerFlow()throws Exception{
        var proofs=stepUps();db.flyway("42",REALM).migrate();UUID policyId=UUID.randomUUID();Instant expiry=clock.instant().plusSeconds(3600).truncatedTo(java.time.temporal.ChronoUnit.MICROS);
        var helper=new MerchantGovernancePostgresCases(db,source,onboarding,REALM,admin);
        try(var op=onboarding.governanceOperatorConnection()){helper.publish(op,UUID.randomUUID(),policyId,MerchantGovernancePostgresCases.policy(clock.instant().minusSeconds(30).truncatedTo(java.time.temporal.ChronoUnit.MICROS)));helper.scope(op,UUID.randomUUID(),policyId,"ISSUE",0,expiry,"application.read","application.assign","application.review","application.evidence.read");}
        UUID applicant=customer();var customerWorkflow=new BusinessContactPostgresCases(source,applicant,password,clock);UUID app=customerWorkflow.screenedEvidenceAndImmutableSubmission();byte[] encryption=new byte[32];Arrays.fill(encryption,(byte)72);
        var governance=new com.elogistic.backend.merchant.internal.infrastructure.JdbcMerchantApplicationReviews(source,new com.elogistic.backend.merchant.internal.infrastructure.ProtectedMerchantFingerprints(Map.of("v1",key()),"v1"),new com.elogistic.backend.merchant.internal.infrastructure.ProtectedGovernancePayloads(Map.of("e1",encryption),"e1"),clock,readAdmission(admin),commandAdmission(admin,clock),customerWorkflow.contactFacts(),proofs);
        commandAdmission(admin,clock).execute(UUID.randomUUID(),new AdminCommandAdmission.ReferencedSubjects(Set.of(applicant),Set.of(admin)),c->governance.assign(c,app,new com.elogistic.backend.merchant.api.MerchantGovernance.ReviewerAssignmentInput(admin,"Synthetic real TLS reviewer assignment"),3,UUID.randomUUID()));
        AdminBrowserRuntime.runGovernance(source,name(admin),password,pair,onboarding,governance,app);
        assertEquals("UNDER_REVIEW",jdbc.queryForObject("SELECT state FROM merchant.application WHERE id=?",String.class,app));
        assertEquals(6,jdbc.queryForObject("SELECT version FROM merchant.application WHERE id=?",Integer.class,app));
        assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM merchant.governance_event WHERE application_id=? AND operation='BEGIN_REVIEW'",Integer.class,app));
        assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM merchant.governance_event WHERE application_id=? AND operation='READY_FOR_DECISION'",Integer.class,app));
        assertEquals(0,jdbc.queryForObject("SELECT count(*) FROM merchant.organization",Integer.class));
    }
    public void merchantV42RealHttpsIndependentRejection()throws Exception{
        var proofs=stepUps();db.flyway("42",REALM).migrate();UUID policyId=UUID.randomUUID();Instant expiry=clock.instant().plusSeconds(3600).truncatedTo(java.time.temporal.ChronoUnit.MICROS);
        var helper=new MerchantGovernancePostgresCases(db,source,onboarding,REALM,admin);
        try(var op=onboarding.governanceOperatorConnection()){helper.publish(op,UUID.randomUUID(),policyId,MerchantGovernancePostgresCases.policy(clock.instant().minusSeconds(30).truncatedTo(java.time.temporal.ChronoUnit.MICROS)));helper.scope(op,UUID.randomUUID(),policyId,"ISSUE",0,expiry,"application.read","application.assign","application.decide");}
        UUID applicant=customer(),reviewer=account(Set.of(AdminReadContext.ENTRY,AdminReadContext.MERCHANT));
        try(var op=onboarding.governanceOperatorConnection()){new MerchantGovernancePostgresCases(db,source,onboarding,REALM,reviewer).scope(op,UUID.randomUUID(),policyId,"ISSUE",0,expiry,"application.read","application.review","application.evidence.read");}
        var workflow=new BusinessContactPostgresCases(source,applicant,password,clock);UUID app=workflow.screenedEvidenceAndImmutableSubmission();byte[] encryption=new byte[32];Arrays.fill(encryption,(byte)72);
        var governance=new com.elogistic.backend.merchant.internal.infrastructure.JdbcMerchantApplicationReviews(source,new com.elogistic.backend.merchant.internal.infrastructure.ProtectedMerchantFingerprints(Map.of("v1",key()),"v1"),new com.elogistic.backend.merchant.internal.infrastructure.ProtectedGovernancePayloads(Map.of("e1",encryption),"e1"),clock,readAdmission(admin),commandAdmission(admin,clock),workflow.contactFacts(),proofs);
        var refs=new AdminCommandAdmission.ReferencedSubjects(Set.of(applicant),Set.of(reviewer));
        commandAdmission(admin,clock).execute(UUID.randomUUID(),refs,c->governance.assign(c,app,new com.elogistic.backend.merchant.api.MerchantGovernance.ReviewerAssignmentInput(reviewer,"Synthetic independent HTTPS assignment"),3,UUID.randomUUID()));
        var review=commandAdmission(reviewer,clock);review.execute(UUID.randomUUID(),refs,c->governance.review(c,app,new com.elogistic.backend.merchant.api.MerchantGovernance.ReviewInput("BEGIN",1,"1",null,"Synthetic independent review"),4,UUID.randomUUID()));
        review.execute(UUID.randomUUID(),refs,c->governance.review(c,app,new com.elogistic.backend.merchant.api.MerchantGovernance.ReviewInput("READY_FOR_DECISION",1,"1",new com.elogistic.backend.merchant.api.MerchantGovernance.Checklist(true,true,true,true,true),"Synthetic verified checklist"),5,UUID.randomUUID()));
        AdminBrowserRuntime.runDecision(source,name(admin),password,pair,onboarding,governance,app,proofs);
        assertEquals("REJECTED",jdbc.queryForObject("SELECT state FROM merchant.application WHERE id=?",String.class,app));assertEquals(7,jdbc.queryForObject("SELECT version FROM merchant.application WHERE id=?",Integer.class,app));
        assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM merchant.governance_event WHERE application_id=? AND operation='REJECT' AND actor_id=?",Integer.class,app,admin));
        assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM identity.admin_governance_reauthentication WHERE target_id=? AND consumed_at IS NOT NULL",Integer.class,app));
        assertEquals(0,jdbc.queryForObject("SELECT count(*) FROM merchant.organization",Integer.class));
    }
    public void merchantV43RealHttpsIndependentApproval()throws Exception{
        var proofs=stepUps();db.flyway("43",REALM).migrate();UUID policyId=UUID.randomUUID();Instant expiry=clock.instant().plusSeconds(3600).truncatedTo(java.time.temporal.ChronoUnit.MICROS);
        var helper=new MerchantGovernancePostgresCases(db,source,onboarding,REALM,admin);
        try(var op=onboarding.governanceOperatorConnection()){helper.publish(op,UUID.randomUUID(),policyId,MerchantGovernancePostgresCases.policy(clock.instant().minusSeconds(30).truncatedTo(java.time.temporal.ChronoUnit.MICROS)));helper.scope(op,UUID.randomUUID(),policyId,"ISSUE",0,expiry,"application.read","application.assign","application.decide","org.read","org.suspend","org.reinstate");}
        UUID applicant=customer(),reviewer=account(Set.of(AdminReadContext.ENTRY,AdminReadContext.MERCHANT));
        try(var op=onboarding.governanceOperatorConnection()){new MerchantGovernancePostgresCases(db,source,onboarding,REALM,reviewer).scope(op,UUID.randomUUID(),policyId,"ISSUE",0,expiry,"application.read","application.review","application.evidence.read");}
        var workflow=new BusinessContactPostgresCases(source,applicant,password,clock);UUID app=workflow.screenedEvidenceAndImmutableSubmission();byte[] encryption=new byte[32];Arrays.fill(encryption,(byte)72);
        var governance=new com.elogistic.backend.merchant.internal.infrastructure.JdbcMerchantApplicationReviews(source,new com.elogistic.backend.merchant.internal.infrastructure.ProtectedMerchantFingerprints(Map.of("v1",key()),"v1"),new com.elogistic.backend.merchant.internal.infrastructure.ProtectedGovernancePayloads(Map.of("e1",encryption),"e1"),clock,readAdmission(admin),commandAdmission(admin,clock),workflow.contactFacts(),proofs);
        var refs=new AdminCommandAdmission.ReferencedSubjects(Set.of(applicant),Set.of(reviewer));
        commandAdmission(admin,clock).execute(UUID.randomUUID(),refs,c->governance.assign(c,app,new com.elogistic.backend.merchant.api.MerchantGovernance.ReviewerAssignmentInput(reviewer,"Synthetic independent HTTPS assignment"),3,UUID.randomUUID()));
        var review=commandAdmission(reviewer,clock);review.execute(UUID.randomUUID(),refs,c->governance.review(c,app,new com.elogistic.backend.merchant.api.MerchantGovernance.ReviewInput("BEGIN",1,"1",null,"Synthetic independent review"),4,UUID.randomUUID()));
        review.execute(UUID.randomUUID(),refs,c->governance.review(c,app,new com.elogistic.backend.merchant.api.MerchantGovernance.ReviewInput("READY_FOR_DECISION",1,"1",new com.elogistic.backend.merchant.api.MerchantGovernance.Checklist(true,true,true,true,true),"Synthetic verified checklist"),5,UUID.randomUUID()));
        var commandPort=commandAdmission(admin,clock);
        var recipientProtection=new com.elogistic.backend.notifications.internal.infrastructure.ProtectedMerchantNoticeRecipient(Map.of("n1",encryption),"n1");
        var notices=new com.elogistic.backend.notifications.internal.infrastructure.JdbcMerchantApplicationNotices(source,workflow.customerAdmission(),commandPort,recipientProtection,message->{throw new AssertionError("NO_SMTP_DURING_BROWSER_BUSINESS_COMMAND");},clock);
        var events=new com.elogistic.backend.merchant.internal.infrastructure.JdbcMerchantApplicationNoticeEvents(source,workflow.customerAdmission(),commandPort,governance);
        AdminBrowserRuntime.runApproval(source,name(admin),password,pair,onboarding,governance,app,proofs,new com.elogistic.backend.workflow.internal.application.MerchantNoticeComposition(events,notices));
        assertEquals("APPROVED",jdbc.queryForObject("SELECT state FROM merchant.application WHERE id=?",String.class,app));assertEquals(7,jdbc.queryForObject("SELECT version FROM merchant.application WHERE id=?",Integer.class,app));
        assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM merchant.governance_event WHERE application_id=? AND operation='APPROVE' AND actor_id=?",Integer.class,app,admin));
        assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM identity.admin_governance_reauthentication WHERE target_id=? AND consumed_at IS NOT NULL",Integer.class,app));
        assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM merchant.organization",Integer.class));
        assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM notification.merchant_application_notice WHERE state='QUEUED'",Integer.class));
        try(var inbox=new com.elogistic.backend.identity.internal.infrastructure.CustomerSmtpTestInbox(1,false,false,250,null)){
            var smtp=new com.elogistic.backend.identity.internal.infrastructure.SmtpCustomerDelivery(new com.elogistic.backend.identity.internal.infrastructure.SmtpCustomerDelivery.Settings("127.0.0.1",inbox.port(),com.elogistic.backend.identity.internal.infrastructure.SmtpCustomerDelivery.Mode.TEST_LOOPBACK,null,null,"notices@example.invalid",Set.of("example.invalid"),"https://portal.example.invalid",true),clock);
            var actualWorker=new com.elogistic.backend.notifications.internal.infrastructure.JdbcMerchantApplicationNotices(source,workflow.customerAdmission(),commandPort,recipientProtection,smtp,clock);
            assertTrue(actualWorker.runOne());assertFalse(actualWorker.runOne());var messages=inbox.messages();assertEquals(1,messages.size());
            var received=new jakarta.mail.internet.MimeMessage(jakarta.mail.Session.getInstance(new Properties()),new java.io.ByteArrayInputStream(messages.getFirst()));
            String content=com.elogistic.backend.identity.internal.infrastructure.CustomerSmtpTestInbox.plainText(received);assertTrue(content.contains("/personal/applications/"+app));assertFalse(content.contains("proof"));assertFalse(content.contains("token"));
            assertEquals("ACCEPTED_BY_PROVIDER",jdbc.queryForObject("SELECT state FROM notification.merchant_application_notice WHERE application_id=?",String.class,app));
        }

    }
    public void merchantV42ReassignmentRequiresFreshReview()throws Exception{
        var proofs=stepUps();db.flyway("42",REALM).migrate();UUID policyId=UUID.randomUUID();Instant expiry=clock.instant().plusSeconds(3600).truncatedTo(java.time.temporal.ChronoUnit.MICROS);
        var helper=new MerchantGovernancePostgresCases(db,source,onboarding,REALM,admin);
        try(var op=onboarding.governanceOperatorConnection()){helper.publish(op,UUID.randomUUID(),policyId,MerchantGovernancePostgresCases.policy(clock.instant().minusSeconds(30).truncatedTo(java.time.temporal.ChronoUnit.MICROS)));helper.scope(op,UUID.randomUUID(),policyId,"ISSUE",0,expiry,"application.read","application.assign");}
        UUID reviewer=account(Set.of(AdminReadContext.ENTRY,AdminReadContext.MERCHANT)),replacement=account(Set.of(AdminReadContext.ENTRY,AdminReadContext.MERCHANT)),applicant=customer();
        try(var op=onboarding.governanceOperatorConnection()){for(UUID id:List.of(reviewer,replacement))new MerchantGovernancePostgresCases(db,source,onboarding,REALM,id).scope(op,UUID.randomUUID(),policyId,"ISSUE",0,expiry,"application.read","application.review");}
        var customerWorkflow=new BusinessContactPostgresCases(source,applicant,password,clock);UUID app=customerWorkflow.screenedEvidenceAndImmutableSubmission();byte[] encryption=new byte[32];Arrays.fill(encryption,(byte)72);
        var governance=new com.elogistic.backend.merchant.internal.infrastructure.JdbcMerchantApplicationReviews(source,new com.elogistic.backend.merchant.internal.infrastructure.ProtectedMerchantFingerprints(Map.of("v1",key()),"v1"),new com.elogistic.backend.merchant.internal.infrastructure.ProtectedGovernancePayloads(Map.of("e1",encryption),"e1"),clock,readAdmission(admin),commandAdmission(admin,clock),customerWorkflow.contactFacts(),proofs);
        var refs=new AdminCommandAdmission.ReferencedSubjects(Set.of(applicant),Set.of(reviewer,replacement));var assigner=commandAdmission(admin,clock);var first=commandAdmission(reviewer,clock);var next=commandAdmission(replacement,clock);
        var begin=new com.elogistic.backend.merchant.api.MerchantGovernance.ReviewInput("BEGIN",1,"1",null,"Synthetic begin");var ready=new com.elogistic.backend.merchant.api.MerchantGovernance.ReviewInput("READY_FOR_DECISION",1,"1",new com.elogistic.backend.merchant.api.MerchantGovernance.Checklist(true,true,true,true,true),"Synthetic checklist");
        assigner.execute(UUID.randomUUID(),refs,c->governance.assign(c,app,new com.elogistic.backend.merchant.api.MerchantGovernance.ReviewerAssignmentInput(reviewer,"Synthetic first assignment"),3,UUID.randomUUID()));
        first.execute(UUID.randomUUID(),refs,c->governance.review(c,app,begin,4,UUID.randomUUID()));first.execute(UUID.randomUUID(),refs,c->governance.review(c,app,ready,5,UUID.randomUUID()));
        assertThrows(com.elogistic.backend.merchant.api.GovernanceFailure.class,()->first.execute(UUID.randomUUID(),refs,c->governance.review(c,app,ready,6,UUID.randomUUID())),"A second new key cannot repeatedly mark the same completed review");
        assigner.execute(UUID.randomUUID(),refs,c->governance.assign(c,app,new com.elogistic.backend.merchant.api.MerchantGovernance.ReviewerAssignmentInput(replacement,"Synthetic replacement"),6,UUID.randomUUID()));
        assertThrows(com.elogistic.backend.merchant.api.GovernanceFailure.class,()->next.execute(UUID.randomUUID(),refs,c->governance.review(c,app,ready,7,UUID.randomUUID())),"New reviewer cannot reuse a previous reviewer's BEGIN/checklist");
        var replacementView=readAdmission(replacement).execute(c->governance.application(c,app));assertTrue(replacementView.allowedActions().contains("BEGIN"));assertFalse(replacementView.allowedActions().contains("READY_FOR_DECISION"));
        next.execute(UUID.randomUUID(),refs,c->governance.review(c,app,begin,7,UUID.randomUUID()));
        assertThrows(com.elogistic.backend.merchant.api.GovernanceFailure.class,()->next.execute(UUID.randomUUID(),refs,c->governance.review(c,app,begin,8,UUID.randomUUID())));
        var done=next.execute(UUID.randomUUID(),refs,c->governance.review(c,app,ready,8,UUID.randomUUID()));assertEquals(9,done.version());assertEquals("UNDER_REVIEW",done.state());
        var view=readAdmission(replacement).execute(c->governance.application(c,app));assertFalse(view.allowedActions().contains("BEGIN"));assertFalse(view.allowedActions().contains("READY_FOR_DECISION"));assertTrue(view.allowedActions().contains("REQUEST_INFORMATION"));
    }
    public void merchantGovernanceRejectsConstructedIdentityContexts()throws Exception{
        db.flyway("42",REALM).migrate();UUID policyId=UUID.randomUUID(),otherAdmin=account(Set.of(AdminReadContext.ENTRY,AdminReadContext.MERCHANT));
        var helper=new MerchantGovernancePostgresCases(db,source,onboarding,REALM,otherAdmin);Instant expiry=clock.instant().plusSeconds(3600).truncatedTo(java.time.temporal.ChronoUnit.MICROS);
        try(var operator=onboarding.governanceOperatorConnection()){helper.publish(operator,UUID.randomUUID(),policyId,MerchantGovernancePostgresCases.policy(clock.instant().minusSeconds(30).truncatedTo(java.time.temporal.ChronoUnit.MICROS)));helper.scope(operator,UUID.randomUUID(),policyId,"ISSUE",0,expiry,"application.read");}
        var domain=new com.elogistic.backend.merchant.internal.infrastructure.JdbcMerchantGovernance(source,clock,readAdmission(admin),commandAdmission(admin,clock));var target=new com.elogistic.backend.merchant.api.MerchantGovernance.Target(com.elogistic.backend.merchant.api.MerchantGovernance.Kind.POLICY,policyId);
        var reads=readAdmission(admin);var commands=commandAdmission(admin,clock);var refs=new AdminCommandAdmission.ReferencedSubjects(Set.of(),Set.of(otherAdmin));
        assertAll(
            ()->assertThrows(AdminFailure.class,()->reads.execute(c->{
                var fake=(AdminReadContext)java.lang.reflect.Proxy.newProxyInstance(AdminReadContext.class.getClassLoader(),new Class<?>[]{AdminReadContext.class},(proxy,method,args)->method.getName().equals("accountId")?otherAdmin:method.invoke(c,args));
                return domain.withScope(fake,target,scope->{scope.require("application.read",target);return scope.version();});
            }),"A forwarding READ context with a substituted account is not Identity authority"),
            ()->assertThrows(AdminFailure.class,()->commands.execute(UUID.randomUUID(),refs,c->{
                var fake=(AdminCommandContext)java.lang.reflect.Proxy.newProxyInstance(AdminCommandContext.class.getClassLoader(),new Class<?>[]{AdminCommandContext.class},(proxy,method,args)->method.getName().equals("accountId")?otherAdmin:method.invoke(c,args));
                return domain.withScope(fake,target,scope->{scope.require("application.read",target);return scope.version();});
            }),"A forwarding COMMAND context with a substituted account is not Identity authority")
        );
    }
    public void governanceIdentityOwnedContextsBindResourceAndLifetime()throws Exception{
        var reads=readAdmission(admin);var commands=commandAdmission(admin,clock);
        var refs=new AdminCommandAdmission.ReferencedSubjects(Set.of(),Set.of());
        var foreignSource=new DriverManagerDataSource(db.url(),db.runtime(),db.runtimePassword());
        var foreignRead=new JdbcAdminAdmission(foreignSource,(o,r)->{throw new AssertionError("NO_NEW_ADMISSION");},UUID::randomUUID,policy,clock);
        var foreignWrite=new JdbcAdminCommandAdmission(foreignSource,(o,r)->{throw new AssertionError("NO_NEW_ADMISSION");},policy,clock);
        AtomicReference<AdminReadContext> escapedRead=new AtomicReference<>();AtomicReference<AdminCommandContext> escapedWrite=new AtomicReference<>();
        reads.execute(c->{
            reads.requireCurrent(c);escapedRead.set(c);
            assertThrows(AdminFailure.class,()->foreignRead.requireCurrent(c));
            var wrongDomain=new com.elogistic.backend.merchant.internal.infrastructure.JdbcMerchantGovernance(foreignSource,clock,reads,commands);assertThrows(com.elogistic.backend.merchant.api.GovernanceFailure.class,()->wrongDomain.context(c));

            var forwarded=(AdminReadContext)java.lang.reflect.Proxy.newProxyInstance(AdminReadContext.class.getClassLoader(),new Class<?>[]{AdminReadContext.class},(p,m,a)->m.invoke(c,a));
            assertThrows(AdminFailure.class,()->reads.requireCurrent(forwarded));
            AdminAdmission fallback=new AdminAdmission(){public <T>T execute(java.util.function.Function<AdminReadContext,T> work){throw new UnsupportedOperationException();}};
            assertThrows(AdminFailure.class,()->fallback.requireCurrent(c));
            var suspended=new TransactionTemplate(new JdbcTransactionManager(source));suspended.setPropagationBehavior(org.springframework.transaction.TransactionDefinition.PROPAGATION_REQUIRES_NEW);suspended.setReadOnly(true);suspended.setIsolationLevel(org.springframework.transaction.TransactionDefinition.ISOLATION_REPEATABLE_READ);
            suspended.execute(status->{assertThrows(AdminFailure.class,()->reads.requireCurrent(c));assertThrows(AdminFailure.class,c::requireCurrent);return null;});
            reads.requireCurrent(c);return null;
        });
        assertThrows(AdminFailure.class,()->reads.requireCurrent(escapedRead.get()));assertThrows(AdminFailure.class,escapedRead.get()::requireCurrent);
        commands.execute(UUID.randomUUID(),refs,c->{
            commands.requireCurrent(c);escapedWrite.set(c);assertThrows(AdminFailure.class,()->foreignWrite.requireCurrent(c));
            var forwarded=(AdminCommandContext)java.lang.reflect.Proxy.newProxyInstance(AdminCommandContext.class.getClassLoader(),new Class<?>[]{AdminCommandContext.class},(p,m,a)->m.invoke(c,a));
            assertThrows(AdminFailure.class,()->commands.requireCurrent(forwarded));
            var suspended=new TransactionTemplate(new JdbcTransactionManager(source));suspended.setPropagationBehavior(org.springframework.transaction.TransactionDefinition.PROPAGATION_REQUIRES_NEW);suspended.setIsolationLevel(org.springframework.transaction.TransactionDefinition.ISOLATION_READ_COMMITTED);
            suspended.execute(status->{assertThrows(AdminFailure.class,()->commands.requireCurrent(c));assertThrows(AdminFailure.class,c::requireCurrent);return null;});
            commands.requireCurrent(c);return null;
        });
        assertThrows(AdminFailure.class,()->commands.requireCurrent(escapedWrite.get()));
        reads.execute(c->{assertThrows(AdminFailure.class,()->reads.requireCurrent(escapedRead.get()));reads.requireCurrent(c);return null;});
        commands.execute(UUID.randomUUID(),refs,c->{assertThrows(AdminFailure.class,()->commands.requireCurrent(escapedWrite.get()));commands.requireCurrent(c);return null;});
    }
    public void submittedBusinessContactFactHistoryAndAuthority()throws Exception{
        db.flyway("41",REALM).migrate();new BusinessContactPostgresCases(source,customer(),password,clock).submittedFactsPreserveHistoryAndRejectWrongBindings(commandAdmission(admin,clock));
    }
    public void submittedBusinessContactFactSecurityVersion()throws Exception{
        db.flyway("41",REALM).migrate();new BusinessContactPostgresCases(source,customer(),password,clock).submittedFactSecurityVersionChangeDenies(commandAdmission(admin,clock));
    }
    private AdminGovernanceReauthentication stepUps(){
        db.flyway("41",REALM).migrate();byte[] verifier=key(),encryption=new byte[32];Arrays.fill(encryption,(byte)37);
        var protection=new ProtectedCustomerProofs(Map.of("v1",verifier),"v1",Map.of("e1",encryption),"e1");byte[] limiterKey=new byte[32];Arrays.fill(limiterKey,(byte)47);
        return new JdbcAdminGovernanceReauthentication(source,protection,new ProtectedSessionCredentials(),new JdbcIdentityRateLimiter(source,limiterKey),()->"127.0.0.1",clock,true);
    }
    public void governanceStepUpUsesCredentialAndDurableReplay()throws Exception{
        var proofs=stepUps();var commands=commandAdmission(admin,clock);var refs=new AdminCommandAdmission.ReferencedSubjects(Set.of(),Set.of());
        UUID target=UUID.randomUUID(),key=UUID.randomUUID();String authority="a".repeat(64),digest="b".repeat(64);
        var input=new AdminGovernanceReauthentication.AdminStepUpInput("APPROVE",target,1,digest,password);
        var result=commands.execute(UUID.randomUUID(),refs,c->proofs.request(c,input,authority,key));
        assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM identity.admin_governance_reauthentication",Integer.class));
        assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM identity.governance_proof_receipt",Integer.class));
        assertEquals(result,commands.execute(UUID.randomUUID(),refs,c->proofs.request(c,input,authority,key)));
        assertEquals("IDEMPOTENCY_CONFLICT",assertThrows(AdminFailure.class,()->commands.execute(UUID.randomUUID(),refs,c->proofs.request(c,new AdminGovernanceReauthentication.AdminStepUpInput("APPROVE",target,2,digest,password),authority,key))).code());
        commands.execute(UUID.randomUUID(),refs,c->{proofs.consume(c,result.proofId(),authority,"APPROVE",target,1,HexFormat.of().parseHex(digest));return null;});
        assertEquals(result,commands.execute(UUID.randomUUID(),refs,c->proofs.request(c,input,authority,key)),"Committed replay returns the original consumed proof, never a replacement");
        assertThrows(AdminFailure.class,()->commands.execute(UUID.randomUUID(),refs,c->{proofs.consume(c,result.proofId(),authority,"APPROVE",target,1,HexFormat.of().parseHex(digest));return null;}));
        assertEquals(2,jdbc.queryForObject("SELECT count(*) FROM identity.audit_entry WHERE event_type IN ('ADMIN_GOVERNANCE_REAUTHENTICATED','ADMIN_GOVERNANCE_PROOF_CONSUMED')",Integer.class));
        assertFalse(input.toString().contains(password));
    }
    public void governanceStepUpRejectsConstructedContext()throws Exception{
        var proofs=stepUps();var commands=commandAdmission(admin,clock);
        commands.execute(UUID.randomUUID(),new AdminCommandAdmission.ReferencedSubjects(Set.of(),Set.of()),context->{
            var constructed=(AdminCommandContext)java.lang.reflect.Proxy.newProxyInstance(AdminCommandContext.class.getClassLoader(),new Class<?>[]{AdminCommandContext.class},(p,m,a)->m.invoke(context,a));
            var failure=assertThrows(AdminFailure.class,()->proofs.request(constructed,new AdminGovernanceReauthentication.AdminStepUpInput("APPROVE",UUID.randomUUID(),1,"b".repeat(64),password),"a".repeat(64),UUID.randomUUID()));
            assertEquals("ACCESS_DENIED",failure.code());return null;
        });
        assertEquals(0,jdbc.queryForObject("SELECT count(*) FROM identity.admin_governance_reauthentication",Integer.class));
    }
    public void governanceStepUpLimitsPersistAndProofBindingCannotChange()throws Exception{
        var proofs=stepUps();var commands=commandAdmission(admin,clock);var refs=new AdminCommandAdmission.ReferencedSubjects(Set.of(),Set.of());
        UUID target=UUID.randomUUID();String authority="a".repeat(64),digest="b".repeat(64);
        var result=commands.execute(UUID.randomUUID(),refs,c->proofs.request(c,new AdminGovernanceReauthentication.AdminStepUpInput("REJECT",target,1,digest,password),authority,UUID.randomUUID()));
        assertThrows(AdminFailure.class,()->commands.execute(UUID.randomUUID(),refs,c->{proofs.consume(c,result.proofId(),authority,"APPROVE",target,1,HexFormat.of().parseHex(digest));return null;}));
        assertThrows(AdminFailure.class,()->commands.execute(UUID.randomUUID(),refs,c->{proofs.consume(c,result.proofId(),"c".repeat(64),"REJECT",target,1,HexFormat.of().parseHex(digest));return null;}));
        assertThrows(IllegalStateException.class,()->commands.execute(UUID.randomUUID(),refs,c->{proofs.consume(c,result.proofId(),authority,"REJECT",target,1,HexFormat.of().parseHex(digest));throw new IllegalStateException("SYNTHETIC_BUSINESS_ROLLBACK");}));
        assertEquals(0,jdbc.queryForObject("SELECT count(*) FROM identity.admin_governance_reauthentication WHERE consumed_at IS NOT NULL",Integer.class));
        for(int n=0;n<5;n++)assertEquals("SESSION_INVALID",assertThrows(AdminFailure.class,()->commands.execute(UUID.randomUUID(),refs,c->proofs.request(c,new AdminGovernanceReauthentication.AdminStepUpInput("REJECT",target,1,digest,"synthetic-wrong-password"),authority,UUID.randomUUID()))).code());
        assertThrows(com.elogistic.backend.identity.internal.application.port.IdentityRateLimiter.Exhausted.class,()->commands.execute(UUID.randomUUID(),refs,c->proofs.request(c,new AdminGovernanceReauthentication.AdminStepUpInput("REJECT",target,1,digest,password),authority,UUID.randomUUID())));
        assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM identity.admin_governance_reauthentication",Integer.class));
    }
    public void governanceProofDirectDmlCannotMutateIdentityOrOmitAudit()throws Exception{
        var proofs=stepUps();var commands=commandAdmission(admin,clock);var refs=new AdminCommandAdmission.ReferencedSubjects(Set.of(),Set.of());
        var result=commands.execute(UUID.randomUUID(),refs,c->proofs.request(c,new AdminGovernanceReauthentication.AdminStepUpInput("APPROVE",UUID.randomUUID(),1,"b".repeat(64),password),"a".repeat(64),UUID.randomUUID()));
        assertThrows(org.springframework.dao.DataAccessException.class,()->jdbc.update("UPDATE identity.admin_governance_reauthentication SET target_id=? WHERE id=?",UUID.randomUUID(),result.proofId()));
        assertThrows(org.springframework.dao.DataAccessException.class,()->jdbc.update("UPDATE identity.governance_proof_receipt SET target_id=?",UUID.randomUUID()));
        assertThrows(org.springframework.dao.DataAccessException.class,()->jdbc.update("DELETE FROM identity.admin_governance_reauthentication WHERE id=?",result.proofId()));
        assertThrows(org.springframework.dao.DataAccessException.class,()->jdbc.execute("TRUNCATE identity.governance_proof_receipt"));
        var transaction=new TransactionTemplate(new JdbcTransactionManager(source));
        var failure=assertThrows(RuntimeException.class,()->transaction.execute(status->{jdbc.update("UPDATE identity.admin_governance_reauthentication SET consumed_at=clock_timestamp(),consumed_request_id=? WHERE id=?",UUID.randomUUID(),result.proofId());return null;}),"Standalone consumption must fail its deferred immutable audit requirement");
        Throwable cause=failure;while(cause.getCause()!=null)cause=cause.getCause();assertInstanceOf(SQLException.class,cause);assertEquals("23514",((SQLException)cause).getSQLState());
        assertEquals(0,jdbc.queryForObject("SELECT count(*) FROM identity.admin_governance_reauthentication WHERE consumed_at IS NOT NULL",Integer.class));
        assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM identity.governance_proof_receipt",Integer.class));
    }
    public void maskedOversightUsesExactContributingSnapshotAndRealParcelEvents()throws Exception{
        UUID sender=customer();var waiting=book(sender,2,clock);var accepted=book(sender,1,clock);accept(accepted);var browser=login(admin);
        var dashboard=read(browser,"/api/v1/admin/dashboard?"+query());assertEquals(200,dashboard.getStatus());String snapshot=selector(dashboard);
        String path="/api/v1/admin/shipments/"+accepted.id()+"?"+query()+"&snapshotId="+snapshot+"&metric=ACCEPTED";
        var response=read(browser,path);assertEquals(200,response.getStatus());var json=JSON.readTree(response.getContentAsString());
        assertEquals(Set.of("shipment","parcels","tracking","acceptance","snapshotId"),json.propertyNames());
        assertEquals(snapshot,json.get("snapshotId").asString());assertEquals("AT_STATION",json.get("shipment").get("status").asString());
        assertEquals(1,json.get("parcels").size());assertEquals(1,json.get("tracking").size());assertEquals("SIM-DROP-A1",json.get("acceptance").get("stationCode").asString());
        assertEquals(PilotLabelRenderer.barcode(accepted.parcels().getFirst().id()),json.get("parcels").get(0).get("code").asString());
        for(String forbidden:List.of("Synthetic private",sender.toString(),"ownerId","recipient","sender","declaredValue","totalVnd","password","draftSnapshot","actorId"))assertFalse(response.getContentAsString().contains(forbidden),forbidden);
        assertEquals(404,read(browser,path.replace(accepted.id().toString(),waiting.id().toString())).getStatus(),"Waiting Shipment does not contribute to ACCEPTED");
        assertEquals(404,read(browser,path.replace(accepted.id().toString(),UUID.randomUUID().toString())).getStatus());
        assertEquals(400,read(browser,path+"&snapshotId="+snapshot).getStatus());
        UUID limited=account(Set.of(AdminReadContext.ENTRY,AdminReadContext.TECHNICAL));assertEquals(403,read(login(limited),path).getStatus());
        assertEquals(409,read(browser,path.replace("snapshotId="+snapshot,"snapshotId="+snapshot+"x")).getStatus());
        assertEquals(200,read(browser,"/api/v1/admin/dashboard/context").getStatus(),"Legacy context remains available");
    }
    public void sixApisHaveRealCurrentSystemAdmissionAndNoImplicitRootAuthority()throws Exception{
        var browser=login(admin);var context=read(browser,"/api/v1/admin/dashboard/context");assertEquals(200,context.getStatus());assertEquals(6,JSON.readTree(context.getContentAsString()).get("permissions").size());
        var report=read(browser,"/api/v1/admin/dashboard?"+query());assertEquals(200,report.getStatus());assertEquals("0",metric(report,"BOOKINGS"));assertEquals("no-store",report.getHeader("Cache-Control"));
        assertEquals(200,read(browser,listPath(query(),selector(report),"BOOKINGS")).getStatus());
        for(var suffix:Map.of("merchant-applications","MERCHANT_APPLICATIONS","network","STATIONS").entrySet())assertEquals(422,read(browser,"/api/v1/admin/dashboard/"+suffix.getKey()+"?"+query()+"&snapshotId="+selector(report)+"&metric="+suffix.getValue()).getStatus());
        assertEquals(200,read(browser,"/api/v1/admin/dashboard/platform").getStatus());
        UUID identity=account(Set.of("IDENTITY_MANAGEMENT"));assertEquals(403,read(login(identity),"/api/v1/admin/dashboard/context").getStatus());
        assertEquals(403,read(browser,"/api/v1/admin/users").getStatus(),"READ_ADMIN_HAS_NO_IDENTITY_ADMINISTRATION");
        assertEquals(401,mvc.perform(get("/api/v1/admin/dashboard/context").secure(true)).andReturn().getResponse().getStatus());
    }
    public void invitationHttpUsesSystemCsrfAndRealProofBeforeLogin()throws Exception{
        byte[] limiterKey=new byte[32];Arrays.fill(limiterKey,(byte)31);var limiter=new JdbcIdentityRateLimiter(source,limiterKey);Arrays.fill(limiterKey,(byte)0);
        var browserState=new JdbcIdentityBrowserState(source);
        var endpoint=new ProductionAdminInvitationHttp(new LocalAdminInvitationActivation(source,onboarding.protection(),clock,"TEST"),browserState,limiter,ORIGIN,()->true);
        var http=MockMvcBuilders.standaloneSetup(controller(admission)).addFilters(new ProductionReadGuard(tokens,requests,sessions,ORIGIN).browser(auth).invitations(endpoint)).build();
        try(var invitation=onboarding.pendingWithSmtp(Set.of(AdminReadContext.ENTRY,AdminReadContext.SHIPMENT),ORIGIN)){
            var csrf=http.perform(get("/api/v1/auth/csrf").secure(true)).andReturn().getResponse();assertEquals(200,csrf.getStatus());
            String challenge=JSON.readTree(csrf.getContentAsString()).get("csrfToken").asString();var cookie=cookie(csrf,"__Host-elogistics.csrf");UUID key=UUID.randomUUID();byte[] token=invitation.token();
            try{
                String body=JSON.writeValueAsString(Map.of("proofId",invitation.proof(),"token",new String(token,java.nio.charset.StandardCharsets.US_ASCII),"password",password));
                java.util.function.Supplier<org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder> request=()->post("/api/v1/auth/admin-invitations/accept").secure(true).header("Origin",ORIGIN).header("X-CSRF-Token",challenge).header("Idempotency-Key",key.toString()).cookie(cookie).contentType("application/json").content(body);
                assertEquals(403,http.perform(request.get().header("Origin","https://foreign.example.invalid")).andReturn().getResponse().getStatus());
                assertEquals(400,http.perform(request.get().queryParam("proofId",invitation.proof().toString())).andReturn().getResponse().getStatus());
                assertEquals(400,http.perform(request.get().content("{\"proofId\":\"invalid\",\"proofId\":\"invalid\"}")).andReturn().getResponse().getStatus());
                assertEquals(403,http.perform(request.get().cookie(new Cookie("__Host-argopost.staff.csrf","foreign"))).andReturn().getResponse().getStatus());
                var accepted=http.perform(request.get()).andReturn().getResponse();assertEquals(204,accepted.getStatus());assertTrue(accepted.getContentAsString().isEmpty());assertTrue(accepted.getHeaders("Set-Cookie").isEmpty());
                assertEquals(204,http.perform(request.get()).andReturn().getResponse().getStatus());
                var logged=login(invitation.account());assertEquals(200,read(logged,"/api/v1/admin/dashboard/context").getStatus());
                assertEquals(403,read(logged,"/api/v1/admin/dashboard/platform").getStatus());
                onboarding.admins().revoke(new LocalAdminProvisioner.OfflineAdminRevoke(UUID.randomUUID(),invitation.account(),2,"Synthetic HTTP revoke after login"));
                assertEquals(401,read(logged,"/api/v1/admin/dashboard/context").getStatus());
                assertEquals(400,http.perform(request.get()).andReturn().getResponse().getStatus());
            }finally{Arrays.fill(token,(byte)0);}
        }
    }
    public void twoConnectionPoolExhaustionFailsClosedAndRecovers()throws Exception{
        var browser=login(admin);var config=new com.zaxxer.hikari.HikariConfig();config.setJdbcUrl(db.url());config.setUsername(db.runtime());config.setPassword(db.runtimePassword());config.setMaximumPoolSize(2);config.setMinimumIdle(0);config.setConnectionTimeout(500);config.setValidationTimeout(500);config.setPoolName("owned-aon-read-pool");
        try(var pool=new com.zaxxer.hikari.HikariDataSource(config)){
            var admission=new JdbcAdminAdmission(pool,requests,requests::currentRequestId,new AdminSessionPolicy(pool,true,"TEST"),clock);
            var endpoint=MockMvcBuilders.standaloneSetup(controller(admission)).addFilters(new ProductionReadGuard(tokens,requests,sessions,ORIGIN)).build();
            java.util.function.Supplier<org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder> query=()->get("/api/v1/admin/dashboard/context").secure(true).header("Authorization","Bearer "+browser.access());
            assertEquals(200,endpoint.perform(query.get()).andReturn().getResponse().getStatus());
            try(var held=pool.getConnection()){
                assertTrue(held.isValid(1));long started=System.nanoTime();assertEquals(503,endpoint.perform(query.get()).andReturn().getResponse().getStatus());assertTrue(System.nanoTime()-started<java.time.Duration.ofSeconds(5).toNanos());assertTrue(held.isValid(1));
            }
            assertEquals(200,endpoint.perform(query.get()).andReturn().getResponse().getStatus());pool.close();
            assertEquals(503,endpoint.perform(query.get()).andReturn().getResponse().getStatus());
        }
    }
    public void canonicalBookingsParcelsAcceptanceFeesAndDrillthroughReconcileWithoutPii()throws Exception{
        var first=book(customer(),3,clock);book(customer(),2,clock);accept(first);var browser=login(admin);
        var report=read(browser,"/api/v1/admin/dashboard?"+query());assertEquals(200,report.getStatus());assertEquals("2",metric(report,"BOOKINGS"));assertEquals("5",metric(report,"PARCELS"));assertEquals("1",metric(report,"ACCEPTED"));assertEquals("1",metric(report,"WAITING_HANDOVER"));assertEquals("125000",metric(report,"CONFIRMED_FEES"));assertEquals("125000",metric(report,"UNPAID_FEES"));
        var list=read(browser,listPath(query(),selector(report),"ACCEPTED"));assertEquals(200,list.getStatus());var items=JSON.readTree(list.getContentAsString()).get("items");assertEquals(1,items.size());assertEquals(first.id().toString(),items.get(0).get("id").asString());assertEquals(3,items.get(0).get("parcelCount").asInt());
        for(String body:List.of(report.getContentAsString(),list.getContentAsString()))for(String forbidden:List.of("Synthetic private","0901234567","recipient","sender","email","password","\"PAID\""))assertFalse(body.contains(forbidden),"PII_OR_UNSUPPORTED_METRIC");
        for(var section:JSON.readTree(report.getContentAsString()).get("sections"))for(var m:section.get("metrics"))assertFalse(m.get("key").asString().contains("REVENUE"),"NO_REVENUE_METRIC_FROM_UNPAID");
        assertEquals(5,jdbc.queryForObject("SELECT count(*) FROM orders.pilot_parcel",Integer.class));assertEquals(2,jdbc.queryForObject("SELECT count(*) FROM finance.pilot_shipping_obligation WHERE state='UNPAID'",Integer.class));
    }
    public void granularPermissionsDenyBeforeFetchingAndCannotLeakFinancialOrTechnicalData()throws Exception{
        UUID shipping=account(Set.of(AdminReadContext.ENTRY,AdminReadContext.SHIPMENT));var browser=login(shipping);var report=read(browser,"/api/v1/admin/dashboard?"+query());assertEquals(200,report.getStatus());assertFalse(report.getContentAsString().contains("UNPAID_FEES"));assertFalse(report.getContentAsString().contains("NETWORK"));
        assertEquals(403,read(browser,"/api/v1/admin/dashboard/platform").getStatus());assertEquals(403,read(browser,listPath(query(),selector(report),"UNPAID_FEES")).getStatus());
        UUID finance=account(Set.of(AdminReadContext.ENTRY,AdminReadContext.FINANCE));var financial=read(login(finance),"/api/v1/admin/dashboard?"+query());assertEquals(200,financial.getStatus());assertFalse(financial.getContentAsString().contains("BOOKINGS"));assertTrue(financial.getContentAsString().contains("\"drilldown\":null"));
        assertEquals(403,read(login(finance),listPath(query(),selector(financial),"BOOKINGS")).getStatus());
        var noEntry=account(Set.of(AdminReadContext.FINANCE));assertEquals(401,loginResult(noEntry).getStatus());
    }
    public void defaultOffProductionCustomerMixedAndRevokedPermissionsFailClosed()throws Exception{
        compose(false,"TEST");assertEquals(401,loginResult(admin).getStatus());compose(true,"PRODUCTION");assertEquals(401,loginResult(admin).getStatus());compose(true,"TEST");
        UUID customer=account(Set.of());db.ownerExecute("INSERT INTO identity.customer_identifier VALUES('"+UUID.randomUUID()+"','"+customer+"','"+name(customer)+"@example.invalid',CURRENT_TIMESTAMP,'ACTIVE',1)");assertEquals(401,loginResult(customer).getStatus());
        UUID mixed=account(Set.of(AdminReadContext.ENTRY,"IDENTITY_MANAGEMENT"));assertEquals(401,loginResult(mixed).getStatus());UUID unknown=account(Set.of(AdminReadContext.ENTRY,"ADMIN_EVERYTHING"));assertEquals(401,loginResult(unknown).getStatus());
        var browser=login(admin);onboarding.admins().revoke(new LocalAdminProvisioner.OfflineAdminRevoke(UUID.randomUUID(),admin,onboarding.admins().status(admin).currentVersion(),"Synthetic revoke current Admin"));assertEquals(401,read(browser,"/api/v1/admin/dashboard/context").getStatus());
    }
    public void queryTamperActorReplayAndLateCohortChangeDoNotReturnMismatchedCounts()throws Exception{
        book(customer(),1,clock);var browser=login(admin);var report=read(browser,"/api/v1/admin/dashboard?"+query());String snapshot=selector(report);
        for(String suffix:List.of("&grain=DAY","&regionId=SIM-A1","&scopeId="+REALM,"&shipmentStatus=DELIVERED","&page=0"))assertEquals(400,read(browser,"/api/v1/admin/dashboard?"+query()+suffix).getStatus());
        assertEquals(409,read(browser,listPath(query(),snapshot+"x","BOOKINGS")).getStatus());UUID other=account(AdminReadContext.CODES);assertEquals(409,read(login(other),listPath(query(),snapshot,"BOOKINGS")).getStatus());
        // A real, committed late booking with a prior business timestamp changes the justified cohort.
        book(customer(),1,Clock.fixed(clock.instant().minusSeconds(5),ZoneOffset.UTC));assertEquals(409,read(browser,listPath(query(),snapshot,"BOOKINGS")).getStatus());
        var fresh=read(browser,"/api/v1/admin/dashboard?"+query());assertEquals("2",metric(fresh,"BOOKINGS"));assertEquals(200,read(browser,listPath(query(),selector(fresh),"BOOKINGS")).getStatus());
    }
    public void statelessReadContextCannotEscapeAndPostgresReadOnlyCannotWrite()throws Exception{
        var browser=login(admin);AtomicReference<AdminReadContext> leaked=new AtomicReference<>();
        AdminAdmission intercept=new AdminAdmission(){public <T>T execute(java.util.function.Function<AdminReadContext,T> work){return admission.execute(a->{leaked.set(a);assertTrue(TransactionSynchronizationManager.isCurrentTransactionReadOnly());assertEquals(Connection.TRANSACTION_REPEATABLE_READ,TransactionSynchronizationManager.getCurrentTransactionIsolationLevel());return work.apply(a);});}};
        var isolated=MockMvcBuilders.standaloneSetup(controller(intercept)).addFilters(new ProductionReadGuard(tokens,requests,sessions,ORIGIN)).build();
        assertEquals(200,isolated.perform(get("/api/v1/admin/dashboard/context").secure(true).header("Authorization","Bearer "+browser.access())).andReturn().getResponse().getStatus());assertThrows(AdminFailure.class,()->leaked.get().requireCurrent());
        AdminAdmission writeAttempt=new AdminAdmission(){public <T>T execute(java.util.function.Function<AdminReadContext,T> work){return admission.execute(a->{jdbc.update("DELETE FROM finance.pilot_shipping_obligation");return work.apply(a);});}};
        var denied=MockMvcBuilders.standaloneSetup(controller(writeAttempt)).addFilters(new ProductionReadGuard(tokens,requests,sessions,ORIGIN)).build();assertEquals(503,denied.perform(get("/api/v1/admin/dashboard/context").secure(true).header("Authorization","Bearer "+browser.access())).andReturn().getResponse().getStatus());
        assertTrue(db.flyway("40",REALM).validateWithResult().validationSuccessful);assertEquals(39,db.flyway("40",REALM).info().applied().length);
    }
    public void realHttpsBrowserUsesSourceAndPostgresWithoutFixtureInterception()throws Exception{
        var first=book(customer(),3,clock);book(customer(),2,clock);accept(first);
        AdminBrowserRuntime.run(source,name(admin),password,pair,onboarding);
        assertEquals(2,jdbc.queryForObject("SELECT count(*) FROM orders.pilot_shipment",Integer.class));
        assertEquals(2,jdbc.queryForObject("SELECT count(*) FROM finance.pilot_shipping_obligation WHERE state='UNPAID'",Integer.class));
        assertEquals(6,jdbc.queryForObject("SELECT count(*) FROM identity.account_grant WHERE account_id=?",Integer.class,admin));
    }
    public void concurrentAuthorityChangeCannotUseAPreLockSnapshot()throws Exception{
        var browser=login(admin);var reached=new CountDownLatch(1);var writerReady=new CountDownLatch(1);
        AdminAdmission gap=new AdminAdmission(){public <T>T execute(java.util.function.Function<AdminReadContext,T> work){reached.countDown();try{if(!writerReady.await(5,TimeUnit.SECONDS))throw new AssertionError("WRITER_NOT_READY");}catch(InterruptedException e){Thread.currentThread().interrupt();throw new AssertionError("INTERRUPTED");}return admission.execute(work);}};
        var endpoint=MockMvcBuilders.standaloneSetup(controller(gap)).addFilters(new ProductionReadGuard(tokens,requests,sessions,ORIGIN)).build();
        try(var pool=Executors.newSingleThreadExecutor();var connection=onboarding.operatorConnection();var statement=connection.createStatement()){
            Future<Integer> pending=pool.submit(()->endpoint.perform(get("/api/v1/admin/dashboard/context").secure(true).header("Authorization","Bearer "+browser.access())).andReturn().getResponse().getStatus());
            assertTrue(reached.await(5,TimeUnit.SECONDS));connection.setAutoCommit(false);statement.execute("SELECT pg_advisory_xact_lock(5279420865908921433)");statement.execute("SELECT * FROM identity.revoke_local_admin('"+UUID.randomUUID()+"','"+admin+"',2,'Synthetic concurrent Admin revoke')");writerReady.countDown();
            assertEquals(503,pending.get(5,TimeUnit.SECONDS),"BUSY_AUTHORITY_MUST_NOT_USE_AN_OLD_SNAPSHOT");connection.commit();
        }finally{writerReady.countDown();}
        assertEquals(401,read(browser,"/api/v1/admin/dashboard/context").getStatus());
    }
    public void partialRevocationInvalidatesSnapshotsAndOtherAudiencesAreRejected()throws Exception{
        var browser=login(admin);var report=read(browser,"/api/v1/admin/dashboard?"+query());String snapshot=selector(report);
        var foreign=new ProductionAccessTokens("https://identity.example.invalid","staff-browser","synthetic-system",(RSAPrivateKey)pair.getPrivate(),Map.of("synthetic-system",(RSAPublicKey)pair.getPublic()),clock);
        String staff=foreign.issue(new IdentitySessionCommands.AccessGrant(UUID.randomUUID(),admin,1,clock.instant(),clock.instant().plusSeconds(890)));
        assertEquals(401,read(new Browser(staff),"/api/v1/admin/dashboard/context").getStatus());
        assertEquals(401,mvc.perform(get("/api/v1/admin/dashboard/context").header("Authorization","Bearer "+browser.access())).andReturn().getResponse().getStatus());
        assertEquals(401,mvc.perform(get("/api/v1/admin/dashboard/context").secure(true).header("Origin","https://foreign.example.invalid").header("Authorization","Bearer "+browser.access())).andReturn().getResponse().getStatus());
        assertEquals(401,mvc.perform(get("/api/v1/admin/dashboard/context?access_token=forged").secure(true).header("Authorization","Bearer "+browser.access())).andReturn().getResponse().getStatus());
        var reduced=new HashSet<>(AdminReadContext.CODES);reduced.remove(AdminReadContext.FINANCE);onboarding.admins().configure(new LocalAdminProvisioner.OfflineAdminPermissions(UUID.randomUUID(),admin,2,reduced,"Synthetic remove Finance read"));
        assertEquals(401,read(browser,listPath(query(),snapshot,"UNPAID_FEES")).getStatus());browser=login(admin);
        assertEquals(403,read(browser,listPath(query(),snapshot,"UNPAID_FEES")).getStatus());assertEquals(409,read(browser,listPath(query(),snapshot,"BOOKINGS")).getStatus());
        var current=read(browser,"/api/v1/admin/dashboard?"+query());assertEquals(200,current.getStatus());assertFalse(current.getContentAsString().contains("UNPAID_FEES"));
    }
    public void businessAndPersonalBookingsShareFactsWithoutMerchantMembershipSubstitution()throws Exception{
        UUID customer=customer(),org=UUID.randomUUID();
        db.ownerExecute("SET argopost.operator_environment='TEST';SELECT merchant.configure_local_workspace('"+UUID.randomUUID()+"','PROVISION','"+org+"','Synthetic organization','"+customer+"','OWNER',ARRAY['dashboard.read','shipment.draft.read','shipment.draft.write','shipment.quote','shipment.book','shipment.finance.read'],'Synthetic isolated fixture');SELECT pricing.configure_local_business_contract('"+UUID.randomUUID()+"','"+org+"','ACTIVATE',0,'Synthetic isolated approved tariff')");
        var protection=new BusinessPreparationProtection(Map.of("v1",key()),"v1");
        var workspaces=new com.elogistic.backend.merchant.internal.infrastructure.JdbcBusinessWorkspaces(source,true,"TEST");
        var access=new com.elogistic.backend.merchant.api.customer.BusinessWorkspaces.Access(){public UUID accountId(){return customer;}public void requireCurrent(){assertTrue(TransactionSynchronizationManager.isActualTransactionActive());}public boolean eligibleAccount(UUID id){return id.equals(customer);}};
        UUID business=new TransactionTemplate(new JdbcTransactionManager(source)).execute(tx->workspaces.inOrganization(access,org,scope->{
            class BusinessAccess implements BusinessPreparations.Access,BusinessQuotes.Access,com.elogistic.backend.finance.api.BusinessObligations.Access {
                final UUID request=UUID.randomUUID();public UUID actorAccountId(){return customer;}public UUID requestId(){return request;}public UUID organizationId(){return org;}public com.elogistic.backend.merchant.api.customer.BusinessWorkspaces.Scope scope(){return scope;}public void requireCurrent(){scope.requireCurrent();}public void meaningfulAction(){requireCurrent();}public void requirePermission(String permission,UUID facility){requireCurrent();assertTrue(scope.permits(permission,facility),"CANONICAL_MERCHANT_PERMISSION_REQUIRED");}
            }
            var a=new BusinessAccess();var drafts=new JdbcBusinessPreparations(source,clock,protection);var contact=new ShippingDrafts.Contact("Synthetic Business Contact","0901234567",new ShippingDrafts.Address("VN","Synthetic Area","Synthetic Locality","Synthetic business address"));var parcels=List.of(new ShippingDrafts.Parcel("Synthetic business box",1,500,100,100,100,0));
            var draft=drafts.create(a,new BusinessPreparations.Input(null,"WEB","SYNTHETIC-ORDER","SYNTHETIC",null,contact,contact,parcels),UUID.randomUUID());
            var quotes=new com.elogistic.backend.pricing.internal.infrastructure.JdbcBusinessQuotes(source,new JdbcPilotQuoteStore(source,Map.of("v1",key()),"v1"),clock,true);
            var quote=quotes.create(a,new BusinessQuotes.Request(draft.id(),draft.version(),null,new PilotQuotes.Route("ARGO-SIM-COVERAGE-1","SIM-A1","SIM-A2","INTRA","SIM-DROP-A1"),List.of(new PilotQuotes.Parcel(500,100,100,100)),true),UUID.randomUUID());
            var shipping=new JdbcBusinessShipments(source,drafts,protection,clock,true);var ready=shipping.requireBookable(a,draft.id(),draft.version());UUID id=UUID.randomUUID();var consumed=quotes.consume(a,quote.id(),draft.id(),draft.version(),id);var shipment=shipping.confirm(a,ready,consumed,UUID.randomUUID(),id);
            new JdbcBusinessObligations(source,clock,true).recordUnpaid(a,null,id,quote.id(),shipment.totalVnd());return id;
        }));
        book(customer(),2,clock);var browser=login(admin);var report=read(browser,"/api/v1/admin/dashboard?"+query());assertEquals(200,report.getStatus());assertEquals("2",metric(report,"BOOKINGS"));assertEquals("3",metric(report,"PARCELS"));assertEquals("75000",metric(report,"UNPAID_FEES"));
        var list=read(browser,listPath(query(),selector(report),"BOOKINGS"));assertEquals(200,list.getStatus());var items=JSON.readTree(list.getContentAsString()).get("items");assertEquals(2,items.size());assertTrue(items.valueStream().anyMatch(row->row.get("id").asString().equals(business.toString())&&row.get("ownerKind").asString().equals("BUSINESS")));
        assertEquals(0,jdbc.queryForObject("SELECT count(*) FROM merchant.membership WHERE account_id=?",Integer.class,admin));assertEquals(401,loginResult(customer).getStatus(),"MERCHANT_OWNER_IS_NOT_CARRIER_ADMIN");
    }
}
