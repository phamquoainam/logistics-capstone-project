package com.elogistic.backend.identity.internal.infrastructure;

import com.elogistic.backend.identity.internal.*;
import com.elogistic.backend.merchant.internal.CustomerProfileConfiguration;
import com.elogistic.backend.platform.internal.infrastructure.PublicMediaConfiguration;
import com.elogistic.backend.platform.internal.web.PublicMediaHttp;
import com.elogistic.backend.workflow.internal.web.*;
import com.nimbusds.jose.*;
import com.nimbusds.jose.jwk.*;
import com.sun.net.httpserver.HttpServer;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.*;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPublicKey;
import java.security.interfaces.RSAPrivateKey;
import java.util.*;
import java.util.concurrent.*;
import javax.sql.DataSource;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.*;
import org.springframework.core.env.MapPropertySource;
import org.springframework.web.context.support.StandardServletEnvironment;
import org.springframework.web.servlet.*;
import org.springframework.web.servlet.config.annotation.*;
import tools.jackson.databind.json.JsonMapper;
import static org.junit.jupiter.api.Assertions.*;

/** Actual HTTPS production adapters + existing owned PG18 fixture + real SMTP inbox + built browser app.
 * No replacement API, extra container, persistent trust mutation, raw diagnostics or credentials in artifacts. */
final class S003CustomerBrowserRuntime {
    static final String EMAIL="browser-customer@example.invalid";
    private static final JsonMapper JSON=JsonMapper.builder().build();
    private static final List<String> CASES=List.of("retail guest registration uses the real HTTPS API", "verification consumes the actual SMTP message only after confirmation", "verified login has isolated customer cookies and no SYSTEM authority", "profile lost response resolves through the real committed receipt", "support and avatar settings reuse actual self profile without claiming unsupported workflows", "personal support ticket uses real staff assignment conversation review and closure", "business workspace uses real organization membership without reusing Personal facts", "personal address book persists CRUD and resolves a lost create through its real receipt", "personal shipping preparation persists address snapshots parcels and unknown-write recovery", "approved simulation quote booking persists shipment parcels and UNPAID obligation with receipt recovery", "station staff scans all parcel labels and atomically accepts custody with visible Personal tracking", "merchant draft uses the real HTTPS API and resolves one lost write", "logout and recovery request use the actual verified destination", "recovery confirmation changes the credential with the actual second message", "fresh login rejects the old credential and revoked browser loses its workspace", "unknown email and invalid proof do not fabricate account activation");

    @TestConfiguration(proxyBeanMethods=false) @EnableAutoConfiguration
    @Import({com.elogistic.backend.orders.internal.BusinessPreparationConfiguration.class,com.elogistic.backend.workflow.internal.web.BusinessPreparationHttp.class,com.elogistic.backend.workflow.internal.web.BusinessImportHttp.class,com.elogistic.backend.merchant.internal.BusinessWorkspaceConfiguration.class,com.elogistic.backend.notifications.internal.BusinessDashboardConfiguration.class,BusinessWorkspaceHttp.class,com.elogistic.backend.orders.internal.SupportTicketConfiguration.class,com.elogistic.backend.workflow.internal.web.SupportTicketHttp.class,com.elogistic.backend.orders.internal.AnalyticsSourceConfiguration.class,com.elogistic.backend.finance.internal.AnalyticsFinanceConfiguration.class,com.elogistic.backend.notifications.internal.AnalyticsConfiguration.class,com.elogistic.backend.workflow.internal.web.PersonalAnalyticsHttp.class,com.elogistic.backend.dispatch.internal.StationIntakeConfiguration.class,com.elogistic.backend.workflow.internal.web.StationIntakeHttp.class,ProductionIdentityHttpConfiguration.class,ProductionCustomerConfiguration.class,CustomerProfileConfiguration.class,com.elogistic.backend.pricing.internal.PilotPricingConfiguration.class,com.elogistic.backend.orders.internal.PilotBookingConfiguration.class,com.elogistic.backend.finance.internal.PilotFinanceConfiguration.class,com.elogistic.backend.workflow.internal.web.PilotShippingHttp.class,com.elogistic.backend.orders.internal.ShippingDraftConfiguration.class,com.elogistic.backend.workflow.internal.web.ShippingDraftHttp.class,CustomerProfileHttp.class,PersonalAddressHttp.class,MerchantApplicationHttp.class,OnboardingHttpErrors.class,PublicMediaConfiguration.class,PublicMediaHttp.class})
    static class Host {
        @Bean WebMvcConfigurer customerAssets(){return new WebMvcConfigurer(){
            public void addViewControllers(ViewControllerRegistry routes){for(String path:List.of("/","/services","/tracking","/quote","/coverage","/stations","/news","/faq","/about","/support","/privacy","/terms","/shipping-policy","/login","/register","/verify-email","/recover","/recover/confirm","/personal","/personal/overview","/personal/overview/shipments","/personal/overview/status","/personal/overview/services","/personal/overview/routes","/personal/overview/fees","/personal/overview/parcels","/personal/profile","/personal/addresses","/personal/create","/personal/shipments","/personal/shipments/{id}","/personal/drafts/{id}","/personal/lookup","/personal/labels","/personal/action-required","/personal/reports","/personal/billing","/personal/security","/personal/settings","/personal/support","/operations/station","/operations/support","/personal/applications","/personal/applications/{id}","/merchant","/merchant/{org}/overview","/merchant/{org}/create","/merchant/{org}/preparations","/merchant/{org}/imports"))routes.addViewController(path).setViewName("forward:/index.html");}
            public void addInterceptors(InterceptorRegistry interceptors){interceptors.addInterceptor(new HandlerInterceptor(){
                public boolean preHandle(jakarta.servlet.http.HttpServletRequest request,jakarta.servlet.http.HttpServletResponse response,Object handler){response.setHeader("Cache-Control","no-store");response.setHeader("Referrer-Policy","no-referrer");response.setHeader("X-Content-Type-Options","nosniff");return true;}
            });}
        };}
    }
    private static String randomKey(){byte[] bytes=new byte[32];new SecureRandom().nextBytes(bytes);try{return Base64.getEncoder().encodeToString(bytes);}finally{Arrays.fill(bytes,(byte)0);}}
    private static String jwk(String id)throws Exception{
        var generator=KeyPairGenerator.getInstance("RSA");generator.initialize(3072);var pair=generator.generateKeyPair();
        return new JWKSet(new RSAKey.Builder((RSAPublicKey)pair.getPublic()).privateKey((RSAPrivateKey)pair.getPrivate()).keyID(id).algorithm(JWSAlgorithm.RS256).keyUse(KeyUse.SIGNATURE).build()).toString(false);
    }
    private static int freeLoopbackPort()throws Exception{try(var socket=new ServerSocket()){socket.bind(new InetSocketAddress("127.0.0.1",0));return socket.getLocalPort();}}
    private static void finishOwned(Process process)throws Exception{
        if(process!=null && process.isAlive()){
            var children=process.toHandle().descendants().toList();for(int i=children.size()-1;i>=0;i--)children.get(i).destroyForcibly();process.destroyForcibly();assertTrue(process.waitFor(10,TimeUnit.SECONDS),"OWNED_BROWSER_PROCESS_CLEANUP_REQUIRED");
        }
    }
    private static X509Certificate certificate(Path directory,String password)throws Exception{
        Path store=directory.resolve("server.p12"),output=directory.resolve("keytool-private.log");
        String executable=Path.of(System.getProperty("java.home"),"bin",System.getProperty("os.name").startsWith("Windows")?"keytool.exe":"keytool").toString();
        var command=new ProcessBuilder(executable,"-genkeypair","-alias","server","-keyalg","RSA","-keysize","3072","-validity","1","-dname","CN=localhost","-ext","SAN=DNS:localhost,IP:127.0.0.1","-ext","EKU=serverAuth","-storetype","PKCS12","-keystore",store.toString(),"-storepass:env","S003_OWNED_KEYSTORE_PASSWORD","-keypass:env","S003_OWNED_KEYSTORE_PASSWORD","-noprompt");
        command.environment().put("S003_OWNED_KEYSTORE_PASSWORD",password);command.redirectErrorStream(true).redirectOutput(output.toFile());Process child=command.start();
        try{assertTrue(child.waitFor(20,TimeUnit.SECONDS),"OWNED_TEST_CERTIFICATE_TIMEOUT");assertEquals(0,child.exitValue(),"OWNED_TEST_CERTIFICATE_FAILED");}finally{finishOwned(child);}
        var keys=KeyStore.getInstance("PKCS12");try(var input=Files.newInputStream(store)){keys.load(input,password.toCharArray());}
        var certificate=(X509Certificate)keys.getCertificate("server");certificate.checkValidity();
        String pem="-----BEGIN CERTIFICATE-----\n"+Base64.getMimeEncoder(64,new byte[]{'\n'}).encodeToString(certificate.getEncoded())+"\n-----END CERTIFICATE-----\n";Files.writeString(directory.resolve("server-cert.pem"),pem,StandardCharsets.US_ASCII,StandardOpenOption.CREATE_NEW);return certificate;
    }
    private static Map<String,Object> properties(Path web,Path directory,String password,String origin,int port,int smtp)throws Exception{
        Map<String,Object> p=new HashMap<>();
        p.put("server.address","127.0.0.1");p.put("server.port",port);p.put("server.ssl.enabled",true);p.put("server.ssl.key-store",directory.resolve("server.p12").toUri().toString());p.put("server.ssl.key-store-type","PKCS12");p.put("server.ssl.key-store-password",password);p.put("server.ssl.enabled-protocols","TLSv1.2,TLSv1.3");p.put("server.forward-headers-strategy","NONE");
        p.put("spring.autoconfigure.exclude","org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration,org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration,org.springframework.boot.data.redis.autoconfigure.DataRedisAutoConfiguration,org.springframework.boot.data.redis.autoconfigure.DataRedisReactiveAutoConfiguration,org.springframework.boot.data.redis.autoconfigure.DataRedisRepositoriesAutoConfiguration,org.springframework.boot.amqp.autoconfigure.RabbitAutoConfiguration,org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration");
        p.put("spring.flyway.enabled",false);p.put("spring.sql.init.mode","never");p.put("spring.main.banner-mode","off");p.put("logging.level.root","WARN");p.put("spring.web.resources.static-locations",web.resolve("dist-customer").toUri().toString());p.put("spring.web.resources.cache.cachecontrol.no-store",true);p.put("server.error.include-message","never");p.put("server.error.include-stacktrace","never");p.put("public.media.mode","local");p.put("public.media.local-root",web.resolve("public/media").toString());
        p.put("identity.security.issuer","https://system-runtime.example.invalid");p.put("identity.security.audience","system-browser");p.put("identity.security.current-kid","system-test");p.put("identity.security.jwk-set",jwk("system-test"));p.put("identity.security.origin",origin);p.put("identity.security.limiter-hmac",randomKey());
        p.put("customer.rollout","SANDBOX");p.put("shipping.pilot.environment","TEST");p.put("support.tickets.environment","TEST");p.put("support.tickets.enabled",true);p.put("business.workspace.enabled",true);p.put("business.workspace.environment","TEST");p.put("customer.security.issuer","https://customer-runtime.example.invalid");p.put("customer.security.audience","customer-browser");p.put("customer.security.current-kid","customer-test");p.put("customer.security.jwk-set",jwk("customer-test"));p.put("customer.security.origin",origin);p.put("customer.security.limiter-hmac",randomKey());
        p.put("customer.security.proof-verifier-keys",JSON.writeValueAsString(Map.of("v1",randomKey())));p.put("customer.security.delivery-encryption-keys",JSON.writeValueAsString(Map.of("e1",randomKey())));p.put("customer.security.current-proof-key","v1");p.put("customer.security.current-encryption-key","e1");
        p.put("merchant.customer-profile.receipt-keys",JSON.writeValueAsString(Map.of("m1",randomKey())));p.put("merchant.customer-profile.current-receipt-key","m1");
        p.put("customer.smtp.mode","TEST_LOOPBACK");p.put("customer.smtp.host","127.0.0.1");p.put("customer.smtp.port",smtp);p.put("customer.smtp.sender","no-reply@example.invalid");p.put("customer.smtp.approved-sender-domains","example.invalid");return p;
    }
    private static ConfigurableApplicationContext host(DataSource source,Map<String,Object> settings){
        var environment=new StandardServletEnvironment();environment.getPropertySources().remove("systemEnvironment");environment.getPropertySources().addFirst(new MapPropertySource("owned-spec003-runtime",settings));environment.setActiveProfiles("identity","postgres","customer");
        return new SpringApplicationBuilder(Host.class).environment(environment).logStartupInfo(false).initializers(context->context.getBeanFactory().registerSingleton("dataSource",source)).run();
    }
    private static final class InboxControl implements AutoCloseable {
        private final HttpServer server;private final ExecutorService executor=Executors.newSingleThreadExecutor();private final String nonce=UUID.randomUUID().toString();
        InboxControl(CustomerSmtpTestInbox inbox,String origin)throws Exception{
            server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);server.setExecutor(executor);
            server.createContext("/mail/",exchange->{
                try{
                    if(!exchange.getRequestMethod().equals("GET") || !nonce.equals(exchange.getRequestHeaders().getFirst("X-Owned-Inbox-Nonce")) || !Set.of("/mail/0","/mail/1").contains(exchange.getRequestURI().toString())){exchange.sendResponseHeaders(404,-1);return;}
                    int index=Integer.parseInt(exchange.getRequestURI().getPath().substring(6));var message=inbox.message(index);if(message.isEmpty()){exchange.sendResponseHeaders(202,-1);return;}
                    byte[] bytes=message.get();String code=null;
                    try{
                        var mime=new jakarta.mail.internet.MimeMessage(jakarta.mail.Session.getInstance(new Properties()),new ByteArrayInputStream(bytes));
                        if(mime.getAllRecipients().length!=1 || !EMAIL.equals(mime.getAllRecipients()[0].toString()))throw new IllegalStateException();
                        String content=CustomerSmtpTestInbox.plainText(mime);
                        if(content.contains("#proofId="))throw new IllegalStateException();
                        var otp=java.util.regex.Pattern.compile("(?m)^([0-9]{6})\\r?$").matcher(content);if(!otp.find())throw new IllegalStateException();code=otp.group(1);
                    }finally{Arrays.fill(bytes,(byte)0);}
                    byte[] response=JSON.writeValueAsBytes(Map.of("code",code));exchange.getResponseHeaders().set("Content-Type","application/json");exchange.getResponseHeaders().set("Cache-Control","no-store");exchange.sendResponseHeaders(200,response.length);exchange.getResponseBody().write(response);Arrays.fill(response,(byte)0);
                }catch(Exception invalid){exchange.sendResponseHeaders(503,-1);}finally{exchange.close();}
            });server.start();
        }
        String origin(){return "http://127.0.0.1:"+server.getAddress().getPort();}
        public void close()throws Exception{server.stop(0);executor.shutdownNow();assertTrue(executor.awaitTermination(5,TimeUnit.SECONDS),"OWNED_INBOX_CONTROL_CLEANUP_REQUIRED");}
    }
    private static void node(Path web,Path directory,String origin,InboxControl inbox,String pin)throws Exception{
        if(System.getenv("NODE_OPTIONS")!=null && !System.getenv("NODE_OPTIONS").isBlank())throw new IllegalStateException("UNTRUSTED_NODE_OPTIONS");
        Process check=new ProcessBuilder("node","--version").redirectErrorStream(true).start();try{String version=new String(check.getInputStream().readNBytes(128),StandardCharsets.UTF_8).strip();assertTrue(check.waitFor(10,TimeUnit.SECONDS));assertEquals("v24.13.0",version,"PINNED_BROWSER_NODE_REQUIRED");}finally{finishOwned(check);}
        var command=new ProcessBuilder("node",web.resolve("node_modules/@playwright/test/cli.js").toString(),"test","--config",web.resolve("playwright-customer-runtime.config.ts").toString()).directory(web.toFile()).redirectErrorStream(true).redirectOutput(directory.resolve("browser-private.log").toFile());
        var env=command.environment();env.keySet().removeIf(k->k.startsWith("PG_") || k.startsWith("IDENTITY_") || k.startsWith("CUSTOMER_") || Set.of("GITHUB_TOKEN","ACTIONS_RUNTIME_TOKEN","ACTIONS_ID_TOKEN_REQUEST_TOKEN").contains(k));
        env.put("S003_BROWSER_ORIGIN",origin);env.put("S003_BROWSER_INBOX",inbox.origin());env.put("S003_BROWSER_INBOX_NONCE",inbox.nonce);env.put("S003_BROWSER_CERT",directory.resolve("server-cert.pem").toString());env.put("S003_BROWSER_SPKI",pin);env.put("S003_BROWSER_PRIVATE_DIRECTORY",directory.toString());env.put("NODE_EXTRA_CA_CERTS",directory.resolve("server-cert.pem").toString());env.put("C04_SAFE_REPORT",directory.resolve("safe-browser-results.json").toString());
        Process process=command.start();try{assertTrue(process.waitFor(180,TimeUnit.SECONDS),"OWNED_BROWSER_TIMEOUT");assertEquals(0,process.exitValue(),"CUSTOMER_BROWSER_RUNTIME_FAILED");}finally{finishOwned(process);}
    }
    /** Explicit allowlisted checkpoints, independent of source-line shifts; never publish private errors or values. */
    private static String stationFailurePhase(Path directory)throws IOException{
      try {
        Path file=directory.resolve("label-failure-stage.json");
        if(!Files.isRegularFile(file)||Files.isSymbolicLink(file)||Files.size(file)>65536)return "BROWSER";
        var marker=JSON.readTree(Files.readAllBytes(file));if(!marker.isObject()||!marker.propertyNames().equals(Set.of("stage"))||!marker.path("stage").isString())return "BROWSER";
        String stage=marker.path("stage").asString();
        return Set.of("LABEL_NAVIGATION","LABEL_REQUEST","LABEL_RESPONSE_WAIT","LABEL_MIME","LABEL_PRIVACY","LABEL_ACTIONS","LABEL_DOWNLOAD","LABEL_FILE_BINDING","LABEL_FILE_MAGIC").contains(stage)||stage.matches("LABEL_HTTP_[1-5][0-9]{2}")?stage:"BROWSER";
      } catch(IOException|RuntimeException malformed) {
        // Optional diagnostics must neither mask the original failed test nor expose malformed private JSON.
        return "BROWSER";
      }
    }
    /** Validate actual browser-saved bytes against the owned PostgreSQL source, not a DevTools payload copy. */
    private static void verifyDownloadedLabels(DataSource source,Path directory)throws Exception{
        record Parcel(UUID id,String shipmentCode){}
        var jdbc=new org.springframework.jdbc.core.JdbcTemplate(source);
        var parcels=jdbc.query("SELECT p.id,s.code FROM orders.pilot_parcel p JOIN orders.pilot_shipment s ON s.id=p.shipment_id JOIN identity.user_account a ON a.id=s.owner_id WHERE a.email=? ORDER BY p.parcel_number",(r,n)->new Parcel(r.getObject(1,UUID.class),r.getString(2)),EMAIL);
        assertEquals(2,parcels.size(),"OWNED_BROWSER_LABEL_SOURCE_REQUIRED");
        Set<String> expected=new HashSet<>();for(var parcel:parcels)expected.add("AP-P-"+parcel.id().toString().replace("-",""));
        for(int read=0;read<5;read++){
            Path file=directory.resolve("customer-label-"+read+".pdf");assertTrue(Files.isRegularFile(file,LinkOption.NOFOLLOW_LINKS)&&!Files.isSymbolicLink(file),"OWNED_BROWSER_DOWNLOAD_REQUIRED");assertTrue(Files.size(file)>5&&Files.size(file)<=12*1024*1024,"BOUNDED_BROWSER_DOWNLOAD_REQUIRED");
            try(var pdf=org.apache.pdfbox.Loader.loadPDF(file.toFile())){
                assertEquals(parcels.size(),pdf.getNumberOfPages(),"BROWSER_LABEL_PAGE_COUNT_REQUIRED");String text=new org.apache.pdfbox.text.PDFTextStripper().getText(pdf);assertTrue(text.contains("UNPAID")&&text.contains("LOCAL/TEST SIMULATION"),"BROWSER_LABEL_POLICY_REQUIRED");
                for(var parcel:parcels)assertTrue(text.contains(parcel.shipmentCode()),"BROWSER_LABEL_SHIPMENT_BINDING_REQUIRED");
                var renderer=new org.apache.pdfbox.rendering.PDFRenderer(pdf);Set<String> observed=new HashSet<>();
                for(int page=0;page<pdf.getNumberOfPages();page++){
                    var box=pdf.getPage(page).getMediaBox();assertEquals(100*72/25.4,box.getWidth(),0.1);assertEquals(150*72/25.4,box.getHeight(),0.1);
                    var image=renderer.renderImageWithDPI(page,300);var bitmap=new com.google.zxing.BinaryBitmap(new com.google.zxing.common.HybridBinarizer(new com.google.zxing.client.j2se.BufferedImageLuminanceSource(image)));
                    var code=new com.google.zxing.MultiFormatReader().decode(bitmap,Map.of(com.google.zxing.DecodeHintType.POSSIBLE_FORMATS,List.of(com.google.zxing.BarcodeFormat.CODE_128),com.google.zxing.DecodeHintType.TRY_HARDER,true)).getText();assertTrue(expected.contains(code)&&observed.add(code),"BROWSER_LABEL_PARCEL_BINDING_REQUIRED");
                }
                assertEquals(expected,observed,"BROWSER_LABEL_COMPLETE_PARCEL_SET_REQUIRED");
            }
        }
    }
    static void run(DataSource source)throws Exception{
        Path root=Path.of("").toAbsolutePath().normalize().getParent().getParent(),web=root.resolve("app/frontend/web");
        Path base=System.getenv("TASK_C_PRIVATE_DIRECTORY")==null?root.resolve(".demo/spec003-implementation/runtime-private"):Path.of(System.getenv("TASK_C_PRIVATE_DIRECTORY"));Files.createDirectories(base);Path directory=Files.createTempDirectory(base,"customer-browser-");
        Path publicDirectory=System.getenv("RUNNER_TEMP")==null?directory:Path.of(System.getenv("RUNNER_TEMP")).resolve("d1-05-reports");Files.createDirectories(publicDirectory);Path report=publicDirectory.resolve("customer-browser-results.json");
        String phase="PREFLIGHT",category="NONE";boolean passed=false;List<Map<String,Object>> cases=new ArrayList<>();Map<String,Object> settings=new HashMap<>();
        try{
            assertTrue(Files.isRegularFile(web.resolve("dist-customer/index.html")),"BUILT_CUSTOMER_ARTIFACT_REQUIRED");assertTrue(Files.isRegularFile(web.resolve("node_modules/@playwright/test/cli.js")),"INSTALLED_BROWSER_DRIVER_REQUIRED");
            phase="CERTIFICATE";String password=randomKey();var cert=certificate(directory,password);String pin=Base64.getEncoder().encodeToString(MessageDigest.getInstance("SHA-256").digest(cert.getPublicKey().getEncoded()));int port=freeLoopbackPort();String origin="https://127.0.0.1:"+port;
            try(var smtp=new CustomerSmtpTestInbox(2,false,false,250,null,120000);var control=new InboxControl(smtp,origin)){
                settings=properties(web,directory,password,origin,port,smtp.port());phase="BOOT";
                try(var context=host(source,settings)){
                    phase="BROWSER";node(web,directory,origin,control,pin);phase="LABEL_DOWNLOAD_PDF";verifyDownloadedLabels(source,directory);phase="RECEIPT";Path result=directory.resolve("safe-browser-results.json");assertTrue(Files.size(result)<=65536);var json=JSON.readTree(Files.readAllBytes(result));assertEquals("passed",json.get("status").asString());assertEquals(CASES.size(),json.get("passed").asInt());assertEquals(0,json.get("failed").asInt());assertEquals(CASES.size(),json.get("cases").size());
                    int i=0;for(var item:json.get("cases")){assertEquals(CASES.get(i++),item.get("name").asString());assertEquals("passed",item.get("status").asString());assertEquals(0,item.get("errors").asInt());cases.add(Map.of("name",item.get("name").asString(),"status","passed"));}
                    phase="CLEANUP";
                }
            }
            passed=true;
        }catch(Throwable failure){
            // Never propagate request/response, key material or private browser error objects into Surefire.
            category=Set.of("AssertionFailedError","BeanCreationException","UnsatisfiedDependencyException","IllegalStateException","IOException").contains(failure.getClass().getSimpleName())?failure.getClass().getSimpleName():"RUNTIME_FAILURE";
            try(var writer=new PrintWriter(Files.newBufferedWriter(directory.resolve("host-private.log")))){failure.printStackTrace(writer);}
            throw new AssertionError("CUSTOMER_BROWSER_RUNTIME_FAILED_SEE_SANITIZED_PHASE");
        }finally{
            settings.clear();Files.deleteIfExists(directory.resolve("server.p12"));
            if(!passed&&phase.equals("BROWSER"))phase=stationFailurePhase(directory);
            if(cases.isEmpty())try{
                Path safe=directory.resolve("safe-browser-results.json");
                if(Files.isRegularFile(safe) && Files.size(safe)<=65536){
                    var json=JSON.readTree(Files.readAllBytes(safe));
                    for(var item:json.get("cases")){
                        String name=item.get("name").asString(),status=item.get("status").asString();
                        if(cases.size()>CASES.size() || !CASES.contains(name) || !Set.of("passed","failed","timedOut","skipped","interrupted").contains(status))throw new IllegalStateException();
                        cases.add(Map.of("name",name,"status",status));
                    }
                }
            }catch(Exception malformed){cases.clear();}
            String revision=System.getenv("GITHUB_SHA");if(revision==null || !revision.matches("[a-f0-9]{40}"))revision="LOCAL_UNPUBLISHED";
            Files.writeString(report,JSON.writeValueAsString(Map.of("status",passed?"PASS":"FAIL","phase",phase,"category",category,"execution","REAL_HTTPS_POSTGRES18_SMTP_BROWSER","revision",revision,"cases",cases))+"\n",StandardOpenOption.CREATE,StandardOpenOption.TRUNCATE_EXISTING);
        }
    }
}
