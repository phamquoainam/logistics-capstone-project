package com.elogistic.backend.workflow.internal.web;

import com.elogistic.backend.identity.internal.*;
import com.elogistic.backend.identity.internal.web.*;
import com.elogistic.backend.identity.internal.infrastructure.*;
import com.elogistic.backend.testsupport.*;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.*;
import java.security.interfaces.*;
import java.util.*;
import java.util.concurrent.*;
import javax.sql.DataSource;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.*;
import org.springframework.core.env.*;
import org.springframework.web.context.support.StandardServletEnvironment;
import org.springframework.web.servlet.config.annotation.*;
import tools.jackson.databind.json.JsonMapper;
import static org.junit.jupiter.api.Assertions.*;

/** One actual Customer-to-independent-Admin browser acceptance. Owned TLS/PG/SMTP/scanner only. */
final class AdminCoreBrowserRuntime {
    @TestConfiguration(proxyBeanMethods=false) @EnableAutoConfiguration
    @Import({ProductionIdentityHttpConfiguration.class,ProductionIdentityReads.class,ProductionIdentityErrors.class,
      ProductionCustomerConfiguration.class,ProductionGovernanceProofConfiguration.class,
      com.elogistic.backend.merchant.internal.CustomerProfileConfiguration.class,
      com.elogistic.backend.merchant.internal.MerchantGovernanceConfiguration.class,
      com.elogistic.backend.merchant.internal.BusinessWorkspaceConfiguration.class,
      com.elogistic.backend.platform.internal.infrastructure.PrivateEvidenceConfiguration.class,
      com.elogistic.backend.notifications.internal.MerchantApplicationNoticeConfiguration.class,
      com.elogistic.backend.workflow.internal.MerchantNoticeConfiguration.class,
      com.elogistic.backend.notifications.internal.AdminDashboardConfiguration.class,
      com.elogistic.backend.notifications.internal.BusinessDashboardConfiguration.class,
      com.elogistic.backend.orders.internal.infrastructure.JdbcAdminShipmentFacts.class,
      com.elogistic.backend.finance.internal.infrastructure.JdbcAdminObligationFacts.class,
      com.elogistic.backend.merchant.internal.infrastructure.AdminApplicationSource.class,
      com.elogistic.backend.dispatch.internal.infrastructure.AdminNetworkSource.class,
      com.elogistic.backend.orders.internal.infrastructure.JdbcAdminOversightReads.class,
      CustomerProfileHttp.class,PersonalAddressHttp.class,MerchantApplicationHttp.class,OnboardingHttpErrors.class,
      CustomerBusinessContactHttp.class,MerchantApplicationWorkflowHttp.class,BusinessWorkspaceHttp.class,
      AdminDashboardHttp.class,MerchantGovernanceHttp.class,MerchantGovernanceDecisionHttp.class,
      MerchantOrganizationGovernanceHttp.class,MerchantAuditHttp.class,MerchantWorkHttp.class})
    static class Host {
      @Bean WebMvcConfigurer assets(){return new WebMvcConfigurer(){public void addViewControllers(ViewControllerRegistry routes){for(String path:List.of("/ops","/ops/login","/ops/activate","/ops/station","/merchant/{org}/create","/merchant/{org}/shipments","/merchant/{org}/shipments/{id}","/","/login","/personal","/personal/profile","/personal/applications","/personal/applications/{id}","/admin","/admin/login","/admin/account","/admin/audit","/admin/merchants/applications","/admin/merchants/organizations"))routes.addViewController(path).setViewName("forward:/index.html");}};}
    }
    @TestConfiguration(proxyBeanMethods=false) @Import({Host.class,ProductionStaffConfiguration.class,
      com.elogistic.backend.pricing.internal.PilotPricingConfiguration.class,com.elogistic.backend.orders.internal.PilotBookingConfiguration.class,
      com.elogistic.backend.finance.internal.PilotFinanceConfiguration.class,com.elogistic.backend.orders.internal.ShippingDraftConfiguration.class,
      com.elogistic.backend.orders.internal.BusinessPreparationConfiguration.class,com.elogistic.backend.dispatch.internal.StationIntakeConfiguration.class,
      BusinessPreparationHttp.class,BusinessShippingHttp.class,OrganizationShipmentManagementHttp.class,OperationalStationHttp.class})
    static class ShippingHost {}
    private static String secret(){byte[] b=new byte[32];new SecureRandom().nextBytes(b);return Base64.getEncoder().encodeToString(b);}
    private static String customerJwk()throws Exception{var g=KeyPairGenerator.getInstance("RSA");g.initialize(3072);var pair=g.generateKeyPair();return new com.nimbusds.jose.jwk.JWKSet(new com.nimbusds.jose.jwk.RSAKey.Builder((RSAPublicKey)pair.getPublic()).privateKey((RSAPrivateKey)pair.getPrivate()).keyID("customer-test").algorithm(com.nimbusds.jose.JWSAlgorithm.RS256).keyUse(com.nimbusds.jose.jwk.KeyUse.SIGNATURE).build()).toString(false);}
    /** Bounded SQLSTATE-only diagnostics: no SQL text, arguments, actor or private payload. */
    private static <T>T observe(T target,Class<T> contract,List<String> faults){
        Object proxy=java.lang.reflect.Proxy.newProxyInstance(contract.getClassLoader(),new Class<?>[]{contract},(object,method,args)->{
            if(method.getDeclaringClass()==Object.class)return switch(method.getName()){case "hashCode"->System.identityHashCode(object);case "equals"->object==args[0];default->"OwnedTestSqlResource[redacted]";};
            try{Object result=method.invoke(target,args);if(result instanceof java.sql.Connection c)return observe(c,java.sql.Connection.class,faults);if(result instanceof java.sql.CallableStatement c)return observe(c,java.sql.CallableStatement.class,faults);if(result instanceof java.sql.PreparedStatement p)return observe(p,java.sql.PreparedStatement.class,faults);if(result instanceof java.sql.Statement s)return observe(s,java.sql.Statement.class,faults);return result;}
            catch(java.lang.reflect.InvocationTargetException failure){Throwable cause=failure.getCause();if(cause instanceof java.sql.SQLException sql){var guard=java.util.regex.Pattern.compile("(?m)^ERROR: ([A-Z][A-Z0-9_]{3,100})$").matcher(Objects.toString(sql.getMessage(),""));String state=Objects.toString(sql.getSQLState(),"UNKNOWN");if(!state.matches("[A-Z0-9]{5}"))state="UNKNOWN";if(faults.size()<30)faults.add(state+":"+(guard.find()?guard.group(1):"SQL_FAILURE"));}throw cause;}
        });return contract.cast(proxy);
    }
    static void run(DataSource source,String password,KeyPair pair,AdminOnboardingPostgresFixture onboarding,Map<String,Object> identities)throws Exception{run(source,password,pair,onboarding,identities,null);}
    static void runBusiness(DataSource source,String password,KeyPair pair,AdminOnboardingPostgresFixture onboarding,Map<String,Object> identities,PostgresTestcontainersSupport.Database db)throws Exception{run(source,password,pair,onboarding,identities,db);}
    private static void run(DataSource source,String password,KeyPair pair,AdminOnboardingPostgresFixture onboarding,Map<String,Object> identities,PostgresTestcontainersSupport.Database db)throws Exception{
        Path web=Path.of("../frontend/web").toAbsolutePath().normalize(),parent=Path.of(Objects.requireNonNull(System.getenv("TASK_C_PRIVATE_DIRECTORY"))).toRealPath(),dir=Files.createDirectory(parent.resolve("admin-core-browser-"+UUID.randomUUID()));
        String storePassword=PostgresTestcontainersSupport.password();var cert=AdminBrowserRuntime.certificate(dir,storePassword);String pin=Base64.getEncoder().encodeToString(MessageDigest.getInstance("SHA-256").digest(cert.getPublicKey().getEncoded()));
        int port;try(var socket=new java.net.ServerSocket()){socket.bind(new InetSocketAddress("127.0.0.1",0));port=socket.getLocalPort();}String origin="https://127.0.0.1:"+port;
        // Business adds one genuine Operations invitation after contact + approval.
        try(var smtp=new CustomerSmtpTestInbox(db==null?2:3,false,false,250,null,120000);var scanner=new ClamAvTestDaemon()){
        var p=new HashMap<String,Object>();p.put("server.address","127.0.0.1");p.put("server.port",port);p.put("server.ssl.enabled",true);p.put("server.ssl.key-store",dir.resolve("server.p12").toUri().toString());p.put("server.ssl.key-store-type","PKCS12");p.put("server.ssl.key-store-password",storePassword);p.put("server.ssl.enabled-protocols","TLSv1.2,TLSv1.3");p.put("server.forward-headers-strategy","NONE");
        p.put("spring.autoconfigure.exclude","org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration,org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration,org.springframework.boot.data.redis.autoconfigure.DataRedisAutoConfiguration,org.springframework.boot.data.redis.autoconfigure.DataRedisReactiveAutoConfiguration,org.springframework.boot.data.redis.autoconfigure.DataRedisRepositoriesAutoConfiguration,org.springframework.boot.amqp.autoconfigure.RabbitAutoConfiguration,org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration");
        p.put("spring.flyway.enabled",false);p.put("spring.sql.init.mode","never");p.put("spring.main.banner-mode","off");p.put("logging.level.root","WARN");p.put("server.error.include-message","never");p.put("server.error.include-stacktrace","never");p.put("spring.web.resources.static-locations",web.resolve("dist-customer").toUri().toString());p.put("spring.web.resources.cache.cachecontrol.no-store",true);
        var jwk=new com.nimbusds.jose.jwk.RSAKey.Builder((RSAPublicKey)pair.getPublic()).privateKey((RSAPrivateKey)pair.getPrivate()).keyID("admin-test").algorithm(JWSAlgorithm.RS256).keyUse(KeyUse.SIGNATURE).build();
        p.put("identity.security.issuer","https://identity.example.invalid");p.put("identity.security.audience","system-browser");p.put("identity.security.current-kid","admin-test");p.put("identity.security.jwk-set",new JWKSet(jwk).toString(false));p.put("identity.security.origin",origin);p.put("identity.security.limiter-hmac",Base64.getEncoder().encodeToString(Base64.getUrlDecoder().decode(PostgresTestcontainersSupport.password())));p.put("identity.admin-dashboard.enabled",true);p.put("identity.admin-dashboard.environment","TEST");

        p.put("merchant.admin-governance.enabled",true);p.put("merchant.application-workflow.enabled",true);p.put("identity.business-contact.enabled",true);p.put("identity.admin-governance-step-up.enabled",true);
        p.putAll(onboarding.runtimeSettings());p.put("identity.admin-onboarding.enabled",true);p.put("identity.admin-invitations.delivery-enabled",false);p.put("identity.local-realm-bootstrap.enabled",false);
        p.put("customer.rollout","SANDBOX");p.put("customer.security.issuer","https://customer-core.example.invalid");p.put("customer.security.audience","customer-browser");p.put("customer.security.current-kid","customer-test");p.put("customer.security.jwk-set",customerJwk());p.put("customer.security.origin",origin);p.put("customer.security.limiter-hmac",secret());
        p.put("customer.security.proof-verifier-keys",JsonMapper.builder().build().writeValueAsString(Map.of("v1",secret())));p.put("customer.security.delivery-encryption-keys",JsonMapper.builder().build().writeValueAsString(Map.of("e1",secret())));p.put("customer.security.current-proof-key","v1");p.put("customer.security.current-encryption-key","e1");p.put("merchant.customer-profile.receipt-keys",JsonMapper.builder().build().writeValueAsString(Map.of("m1",secret())));p.put("merchant.customer-profile.current-receipt-key","m1");
        p.put("business.workspace.enabled",true);p.put("business.workspace.environment","TEST");p.put("shipping.pilot.environment","TEST");
        p.put("public.media.local-root",web.resolve("public/media").toString());p.put("support.claim-evidence.mode","filesystem");p.put("support.claim-evidence.private-root",dir.resolve("private-evidence").toString());p.put("support.claim-evidence.public-roots",web.resolve("public").toString()+";"+web.resolve("dist-customer"));p.put("support.claim-evidence.clamav-host","127.0.0.1");p.put("support.claim-evidence.clamav-port",scanner.port());
        p.put("customer.smtp.mode","TEST_LOOPBACK");p.put("customer.smtp.host","127.0.0.1");p.put("customer.smtp.port",smtp.port());p.put("customer.smtp.sender","no-reply@example.invalid");p.put("customer.smtp.approved-sender-domains","example.invalid");
        if(db!=null){p.put("shipping.pilot.business-intake-enabled",true);p.put("identity.operations.enabled",true);p.put("identity.operations.environment","TEST");p.put("identity.staff.enabled",true);p.put("identity.staff.environment","TEST");}
        var business=db==null?null:new BusinessIntakeUatFixture(db,onboarding.operationsTestOperator(),p);
        var fixture=new HashMap<String,Object>(identities);fixture.put("businessIntake",business!=null);fixture.put("password",password);String nonce=UUID.randomUUID().toString();fixture.put("inboxNonce",nonce);
        var inbox=com.sun.net.httpserver.HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);var executor=Executors.newSingleThreadExecutor();inbox.setExecutor(executor);
        if(business!=null){
            inbox.createContext("/shipping-setup",exchange->{try{
                if(!"POST".equals(exchange.getRequestMethod())||!nonce.equals(exchange.getRequestHeaders().getFirst("X-Test-Nonce"))){exchange.sendResponseHeaders(403,-1);return;}
                var application=UUID.fromString(exchange.getRequestURI().getQuery());byte[] data=JsonMapper.builder().build().writeValueAsBytes(business.shippingSetup(application));
                exchange.getResponseHeaders().set("Content-Type","application/json");exchange.sendResponseHeaders(200,data.length);exchange.getResponseBody().write(data);
            }catch(Exception failure){exchange.sendResponseHeaders(503,-1);}finally{exchange.close();}});
            inbox.createContext("/ops-authority",exchange->{try{
                if(!"POST".equals(exchange.getRequestMethod())||!nonce.equals(exchange.getRequestHeaders().getFirst("X-Test-Nonce"))){exchange.sendResponseHeaders(403,-1);return;}
                business.assignVerifiedOperations();exchange.sendResponseHeaders(204,-1);
            }catch(Exception failure){exchange.sendResponseHeaders(503,-1);}finally{exchange.close();}});
            inbox.createContext("/ops-invitation",exchange->{try{
                if(!"GET".equals(exchange.getRequestMethod())||!nonce.equals(exchange.getRequestHeaders().getFirst("X-Test-Nonce"))){exchange.sendResponseHeaders(403,-1);return;}
                for(byte[] mail:smtp.received()){
                    var text=CustomerSmtpTestInbox.plainText(new jakarta.mail.internet.MimeMessage(jakarta.mail.Session.getInstance(new Properties()),new java.io.ByteArrayInputStream(mail)));
                    var match=java.util.regex.Pattern.compile(java.util.regex.Pattern.quote(origin)+"/ops/activate#proofId=[a-f0-9-]{36}&token=[A-Za-z0-9_-]{43}").matcher(text);
                    if(match.find()){byte[] data=JsonMapper.builder().build().writeValueAsBytes(Map.of("link",match.group()));exchange.getResponseHeaders().set("Content-Type","application/json");exchange.sendResponseHeaders(200,data.length);exchange.getResponseBody().write(data);return;}
                }exchange.sendResponseHeaders(404,-1);
            }catch(Exception failure){exchange.sendResponseHeaders(503,-1);}finally{exchange.close();}});
        }
        var heldAdmission=new java.util.concurrent.atomic.AtomicReference<java.sql.Connection>();
        inbox.createContext("/admission-hold",exchange->{try{
            if(!"POST".equals(exchange.getRequestMethod())||!nonce.equals(exchange.getRequestHeaders().getFirst("X-Test-Nonce"))){exchange.sendResponseHeaders(403,-1);return;}
            if("hold".equals(exchange.getRequestURI().getQuery())){
                if(heldAdmission.get()!=null){exchange.sendResponseHeaders(409,-1);return;}
                var connection=source.getConnection();boolean held=false;try(var statement=connection.prepareStatement("SELECT pg_try_advisory_lock(?)")){statement.setLong(1,0x4944454e54495459L);try(var rows=statement.executeQuery()){held=rows.next()&&rows.getBoolean(1);}}
                if(!held){connection.close();exchange.sendResponseHeaders(409,-1);return;}heldAdmission.set(connection);
            }else if("release".equals(exchange.getRequestURI().getQuery())){
                var connection=heldAdmission.getAndSet(null);if(connection==null){exchange.sendResponseHeaders(409,-1);return;}try(connection;var statement=connection.prepareStatement("SELECT pg_advisory_unlock(?)")){statement.setLong(1,0x4944454e54495459L);try(var rows=statement.executeQuery()){assertTrue(rows.next()&&rows.getBoolean(1));}}
            }else{exchange.sendResponseHeaders(400,-1);return;}
            exchange.sendResponseHeaders(204,-1);
        }catch(Exception failure){exchange.sendResponseHeaders(503,-1);}finally{exchange.close();}});
        inbox.createContext("/contact",exchange->{try{
            if(!"GET".equals(exchange.getRequestMethod())||!nonce.equals(exchange.getRequestHeaders().getFirst("X-Test-Nonce"))){exchange.sendResponseHeaders(403,-1);return;}
            var received=smtp.received();if(received.isEmpty()){exchange.sendResponseHeaders(404,-1);return;}
            String content=CustomerSmtpTestInbox.plainText(new jakarta.mail.internet.MimeMessage(jakarta.mail.Session.getInstance(new Properties()),new java.io.ByteArrayInputStream(received.getFirst())));
            var match=java.util.regex.Pattern.compile(java.util.regex.Pattern.quote(origin)+"/personal/profile#applicationId=[a-f0-9-]{36}&proofId=[a-f0-9-]{36}&proof=[A-Za-z0-9_-]{43}").matcher(content);if(!match.find())throw new IllegalStateException();
            byte[] result=JsonMapper.builder().build().writeValueAsBytes(Map.of("link",match.group(),"count",received.size()));exchange.getResponseHeaders().set("Content-Type","application/json");exchange.sendResponseHeaders(200,result.length);exchange.getResponseBody().write(result);Arrays.fill(result,(byte)0);
        }catch(Exception failure){exchange.sendResponseHeaders(503,-1);}finally{exchange.close();}});inbox.start();fixture.put("inboxOrigin","http://127.0.0.1:"+inbox.getAddress().getPort());
        Files.writeString(dir.resolve("fixture-login.json"),JsonMapper.builder().build().writeValueAsString(fixture),StandardCharsets.UTF_8,StandardOpenOption.CREATE_NEW);
        var sqlFaults=new java.util.concurrent.CopyOnWriteArrayList<String>();var observed=observe(source,DataSource.class,sqlFaults);
        var env=new StandardServletEnvironment();env.getPropertySources().remove("systemEnvironment");env.getPropertySources().addFirst(new MapPropertySource("owned-admin-core",p));env.setActiveProfiles("identity","customer","postgres");
        try(var host=new SpringApplicationBuilder(db==null?Host.class:ShippingHost.class).environment(env).logStartupInfo(false).initializers(c->c.getBeanFactory().registerSingleton("dataSource",observed)).run()){
            var command=new ProcessBuilder("node",web.resolve("node_modules/@playwright/test/cli.js").toString(),"test","--config",web.resolve("playwright-admin-core-runtime.config.ts").toString()).directory(web.toFile()).redirectErrorStream(true).redirectOutput(dir.resolve("browser-private.log").toFile());
            if(System.getenv("NODE_OPTIONS")!=null&&!System.getenv("NODE_OPTIONS").isBlank())throw new IllegalStateException("UNTRUSTED_NODE_OPTIONS");
            var child=command.environment();child.keySet().removeIf(k->k.startsWith("PG_")||k.startsWith("IDENTITY_")||k.startsWith("CUSTOMER_")||k.startsWith("AWS_")||k.startsWith("SMTP_")||k.startsWith("GMAIL_")||Set.of("GITHUB_TOKEN","ACTIONS_RUNTIME_TOKEN","ACTIONS_ID_TOKEN_REQUEST_TOKEN").contains(k));
            child.put("S003_BROWSER_ORIGIN",origin);child.put("S003_BROWSER_CERT",dir.resolve("server-cert.pem").toString());child.put("S003_BROWSER_SPKI",pin);child.put("S003_BROWSER_PRIVATE_DIRECTORY",dir.toString());child.put("NODE_EXTRA_CA_CERTS",dir.resolve("server-cert.pem").toString());child.put("C04_SAFE_REPORT",dir.resolve("safe-browser-results.json").toString());
            var process=command.start();try{assertTrue(process.waitFor(180,TimeUnit.SECONDS),()->"ADMIN_CORE_BROWSER_TIMEOUT BIA_SAFE_STAGE_"+safeStage(dir));assertEquals(0,process.exitValue(),()->"ADMIN_CORE_BROWSER_RUNTIME_FAILED BIA_SAFE_STAGE_"+safeStage(dir));}finally{if(process.isAlive()){process.toHandle().descendants().forEach(ProcessHandle::destroyForcibly);process.destroyForcibly();}}
            var result=JsonMapper.builder().build().readTree(Files.readString(dir.resolve("safe-browser-results.json")));assertEquals("passed",result.get("status").asString());assertEquals(1,result.get("passed").asInt());assertEquals(0,result.get("failed").asInt());
            if(db!=null)try(var pdf=org.apache.pdfbox.Loader.loadPDF(dir.resolve("bia-labels.pdf").toFile())){
                assertEquals(2,pdf.getNumberOfPages(),"BIA_SAFE_CHECK_LABEL_PAGES");String text=new org.apache.pdfbox.text.PDFTextStripper().getText(pdf);assertTrue(text.contains("UNPAID"),"BIA_SAFE_CHECK_LABEL_UNPAID");assertTrue(text.contains("LOCAL/TEST SIMULATION"),"BIA_SAFE_CHECK_LABEL_SIMULATION");
                var expected=new HashSet<>(new org.springframework.jdbc.core.JdbcTemplate(source).query("SELECT p.id FROM orders.pilot_parcel p JOIN orders.pilot_shipment s ON s.id=p.shipment_id WHERE s.owner_type='BUSINESS'",(r,n)->"AP-P-"+r.getObject(1,UUID.class).toString().replace("-","")));var decoded=new HashSet<String>();var renderer=new org.apache.pdfbox.rendering.PDFRenderer(pdf);
                for(int i=0;i<2;i++){var bitmap=new com.google.zxing.BinaryBitmap(new com.google.zxing.common.HybridBinarizer(new com.google.zxing.client.j2se.BufferedImageLuminanceSource(renderer.renderImageWithDPI(i,300))));decoded.add(new com.google.zxing.MultiFormatReader().decode(bitmap,Map.of(com.google.zxing.DecodeHintType.POSSIBLE_FORMATS,List.of(com.google.zxing.BarcodeFormat.CODE_128),com.google.zxing.DecodeHintType.TRY_HARDER,true)).getText());}assertEquals(expected,decoded,"BIA_SAFE_CHECK_LABEL_BARCODES");
            }
            assertEquals(db==null?2:3,smtp.messages().size(),"BIA_SAFE_CHECK_SMTP_MESSAGES");assertTrue(scanner.requests.get()>=2,"BIA_SAFE_CHECK_SCANNER_REQUESTS");
        }finally{var held=heldAdmission.getAndSet(null);if(held!=null)held.close();Files.writeString(dir.resolve("safe-sql-faults.json"),JsonMapper.builder().build().writeValueAsString(sqlFaults),StandardCharsets.UTF_8);inbox.stop(0);executor.shutdownNow();}
        }
    }
    private static String safeStage(Path directory){
        try{
            var path=directory.resolve("safe-core-stage.json");if(Files.isSymbolicLink(path)||Files.size(path)>256)return "UNKNOWN";
            String stage=JsonMapper.builder().build().readTree(Files.readString(path,StandardCharsets.UTF_8)).path("stage").asString();
            return Set.of("CUSTOMER_LOGIN","APPLICATION_DRAFT","CONTACT_VERIFICATION","EVIDENCE","SUBMISSION","ASSIGNMENT","REVIEW","APPROVAL","DASHBOARD","AUDIT_AND_NOTICES","EXPLICIT_SHIPPING_SETUP","BUSINESS_QUOTE_BOOKING","GENUINE_OPS_VERIFICATION","BUSINESS_SCAN_ACCEPTANCE","BUSINESS_AT_STATION_VERIFIED").contains(stage)?stage:"UNKNOWN";
        }catch(Exception unavailable){return "UNKNOWN";}
    }
}
