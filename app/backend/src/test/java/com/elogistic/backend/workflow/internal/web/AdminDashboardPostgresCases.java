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
        keys();REALM=realm;onboarding=new AdminOnboardingPostgresFixture(database.container(),realm);db=onboarding.database();assertTrue(db.flyway("latest",REALM).validateWithResult().validationSuccessful);
        source=new DriverManagerDataSource(db.url(),db.runtime(),db.runtimePassword());jdbc=new JdbcTemplate(source);admin=account(AdminReadContext.CODES);username=name(admin);compose(true,"TEST");
    }
    String name(UUID id){return jdbc.queryForObject("SELECT username FROM identity.user_account WHERE id=?",String.class,id);}
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
    AdminDashboardHttp controller(AdminAdmission admitted){return new AdminDashboardHttp(admitted,reports,new JdbcAdminShipmentFacts(source),new JdbcAdminObligationFacts(source),new AdminApplicationSource(),new AdminNetworkSource(),clock);}
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
        assertTrue(db.flyway("latest",REALM).validateWithResult().validationSuccessful);assertEquals(39,db.flyway("latest",REALM).info().applied().length);
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
