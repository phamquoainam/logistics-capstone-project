package com.elogistic.backend.workflow.internal.web;

import com.elogistic.backend.identity.internal.ProductionIdentityHttpConfiguration;
import com.elogistic.backend.identity.internal.web.*;
import com.elogistic.backend.testsupport.PostgresTestcontainersSupport;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.*;
import java.security.interfaces.*;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.concurrent.TimeUnit;
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

/** Ephemeral pinned HTTPS, real Spring servlet and owned PG. No developer URL or API mock server. */
final class AdminBrowserRuntime {
    @TestConfiguration(proxyBeanMethods=false) @EnableAutoConfiguration
    @Import({ProductionIdentityHttpConfiguration.class,ProductionIdentityReads.class,ProductionIdentityErrors.class,
        AdminDashboardHttp.class,com.elogistic.backend.notifications.internal.AdminDashboardConfiguration.class,
        com.elogistic.backend.orders.internal.infrastructure.JdbcAdminShipmentFacts.class,
        com.elogistic.backend.finance.internal.infrastructure.JdbcAdminObligationFacts.class,
        com.elogistic.backend.merchant.internal.infrastructure.AdminApplicationSource.class,
        com.elogistic.backend.dispatch.internal.infrastructure.AdminNetworkSource.class})
    static class Host {
        @Bean WebMvcConfigurer adminAssets(){return new WebMvcConfigurer(){public void addViewControllers(ViewControllerRegistry routes){routes.addViewController("/admin").setViewName("forward:/index.html");routes.addViewController("/admin/login").setViewName("forward:/index.html");routes.addViewController("/admin/activate").setViewName("forward:/index.html");}};}
    }
    private static void finish(Process p)throws Exception{if(p!=null&&p.isAlive()){for(var child:p.toHandle().descendants().toList())child.destroyForcibly();p.destroyForcibly();assertTrue(p.waitFor(10,TimeUnit.SECONDS),"OWNED_ADMIN_CHILD_CLEANUP_FAILED");}}
    private static X509Certificate certificate(Path dir,String password)throws Exception{
        String exe=Path.of(System.getProperty("java.home"),"bin",System.getProperty("os.name").startsWith("Windows")?"keytool.exe":"keytool").toString();
        var command=new ProcessBuilder(exe,"-genkeypair","-alias","server","-keyalg","RSA","-keysize","3072","-validity","1","-dname","CN=localhost","-ext","SAN=DNS:localhost,IP:127.0.0.1","-ext","EKU=serverAuth","-storetype","PKCS12","-keystore",dir.resolve("server.p12").toString(),"-storepass:env","ADMIN_TEST_STORE_PASSWORD","-keypass:env","ADMIN_TEST_STORE_PASSWORD","-noprompt");
        command.environment().put("ADMIN_TEST_STORE_PASSWORD",password);command.redirectErrorStream(true).redirectOutput(dir.resolve("keytool-private.log").toFile());var process=command.start();
        try{assertTrue(process.waitFor(20,TimeUnit.SECONDS),"ADMIN_CERTIFICATE_TIMEOUT");assertEquals(0,process.exitValue(),"ADMIN_CERTIFICATE_FAILED");}finally{finish(process);}
        var keys=KeyStore.getInstance("PKCS12");try(var in=Files.newInputStream(dir.resolve("server.p12"))){keys.load(in,password.toCharArray());}
        var cert=(X509Certificate)keys.getCertificate("server");cert.checkValidity();Files.writeString(dir.resolve("server-cert.pem"),"-----BEGIN CERTIFICATE-----\n"+Base64.getMimeEncoder(64,new byte[]{'\n'}).encodeToString(cert.getEncoded())+"\n-----END CERTIFICATE-----\n",StandardCharsets.US_ASCII,StandardOpenOption.CREATE_NEW);return cert;
    }
    static void run(DataSource source,String username,String password,KeyPair pair,com.elogistic.backend.identity.internal.infrastructure.AdminOnboardingPostgresFixture onboarding)throws Exception{
        Path web=Path.of("../frontend/web").toAbsolutePath().normalize();assertTrue(Files.isRegularFile(web.resolve("dist-customer/index.html")),"CURRENT_CUSTOMER_BUILD_REQUIRED");
        Path parent=Path.of(Objects.requireNonNull(System.getenv("TASK_C_PRIVATE_DIRECTORY"))).toRealPath(),dir=Files.createDirectory(parent.resolve("admin-browser-"+UUID.randomUUID()));
        String storePassword=PostgresTestcontainersSupport.password();var cert=certificate(dir,storePassword);String pin=Base64.getEncoder().encodeToString(MessageDigest.getInstance("SHA-256").digest(cert.getPublicKey().getEncoded()));
        int port;try(var socket=new java.net.ServerSocket()){socket.bind(new InetSocketAddress("127.0.0.1",0));port=socket.getLocalPort();}String origin="https://127.0.0.1:"+port;
        var p=new HashMap<String,Object>();p.put("server.address","127.0.0.1");p.put("server.port",port);p.put("server.ssl.enabled",true);p.put("server.ssl.key-store",dir.resolve("server.p12").toUri().toString());p.put("server.ssl.key-store-type","PKCS12");p.put("server.ssl.key-store-password",storePassword);p.put("server.ssl.enabled-protocols","TLSv1.2,TLSv1.3");p.put("server.forward-headers-strategy","NONE");
        p.put("spring.autoconfigure.exclude","org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration,org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration,org.springframework.boot.data.redis.autoconfigure.DataRedisAutoConfiguration,org.springframework.boot.data.redis.autoconfigure.DataRedisReactiveAutoConfiguration,org.springframework.boot.data.redis.autoconfigure.DataRedisRepositoriesAutoConfiguration,org.springframework.boot.amqp.autoconfigure.RabbitAutoConfiguration,org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration");
        p.put("spring.flyway.enabled",false);p.put("spring.sql.init.mode","never");p.put("spring.main.banner-mode","off");p.put("logging.level.root","WARN");p.put("server.error.include-message","never");p.put("server.error.include-stacktrace","never");p.put("spring.web.resources.static-locations",web.resolve("dist-customer").toUri().toString());p.put("spring.web.resources.cache.cachecontrol.no-store",true);
        var jwk=new com.nimbusds.jose.jwk.RSAKey.Builder((RSAPublicKey)pair.getPublic()).privateKey((RSAPrivateKey)pair.getPrivate()).keyID("admin-test").algorithm(JWSAlgorithm.RS256).keyUse(KeyUse.SIGNATURE).build();
        p.put("identity.security.issuer","https://identity.example.invalid");p.put("identity.security.audience","system-browser");p.put("identity.security.current-kid","admin-test");p.put("identity.security.jwk-set",new JWKSet(jwk).toString(false));p.put("identity.security.origin",origin);p.put("identity.security.limiter-hmac",Base64.getEncoder().encodeToString(Base64.getUrlDecoder().decode(PostgresTestcontainersSupport.password())));p.put("identity.admin-dashboard.enabled",true);p.put("identity.admin-dashboard.environment","TEST");
        p.putAll(onboarding.runtimeSettings());p.put("identity.admin-onboarding.enabled",true);p.put("identity.admin-invitations.delivery-enabled",false);p.put("identity.local-realm-bootstrap.enabled",false);p.put("customer.rollout","SANDBOX");p.put("customer.security.origin",origin);
        try(var invitation=onboarding.pendingWithSmtp(com.elogistic.backend.identity.api.admin.AdminReadContext.CODES,origin)){
        String invitedUsername=new org.springframework.jdbc.core.JdbcTemplate(source).queryForObject("SELECT username FROM identity.user_account WHERE id=?",String.class,invitation.account());
        byte[] proofToken=invitation.token();
        try{Files.writeString(dir.resolve("fixture-login.json"),JsonMapper.builder().build().writeValueAsString(Map.of("username",username,"password",password,"activation",Map.of("proofId",invitation.proof(),"token",new String(proofToken,StandardCharsets.US_ASCII),"username",invitedUsername))),StandardCharsets.UTF_8,StandardOpenOption.CREATE_NEW);}
        finally{Arrays.fill(proofToken,(byte)0);}
        var env=new StandardServletEnvironment();env.getPropertySources().remove("systemEnvironment");env.getPropertySources().addFirst(new MapPropertySource("owned-admin-runtime",p));env.setActiveProfiles("identity","postgres");
        try(var host=new SpringApplicationBuilder(Host.class).environment(env).logStartupInfo(false).initializers(c->c.getBeanFactory().registerSingleton("dataSource",source)).run()){
            assertTrue(host.isActive(),"ADMIN_HTTPS_HOST_REQUIRED");
            var command=new ProcessBuilder("node",web.resolve("node_modules/@playwright/test/cli.js").toString(),"test","--config",web.resolve("playwright-admin-runtime.config.ts").toString()).directory(web.toFile()).redirectErrorStream(true).redirectOutput(dir.resolve("browser-private.log").toFile());
            if(System.getenv("NODE_OPTIONS")!=null&&!System.getenv("NODE_OPTIONS").isBlank())throw new IllegalStateException("UNTRUSTED_NODE_OPTIONS");
            var childEnv=command.environment();childEnv.keySet().removeIf(k->k.startsWith("PG_")||k.startsWith("IDENTITY_")||k.startsWith("CUSTOMER_")||k.startsWith("AWS_")||k.startsWith("SMTP_")||k.startsWith("GMAIL_")||Set.of("GITHUB_TOKEN","ACTIONS_RUNTIME_TOKEN","ACTIONS_ID_TOKEN_REQUEST_TOKEN").contains(k));
            childEnv.put("S003_BROWSER_ORIGIN",origin);childEnv.put("S003_BROWSER_CERT",dir.resolve("server-cert.pem").toString());childEnv.put("S003_BROWSER_SPKI",pin);childEnv.put("S003_BROWSER_PRIVATE_DIRECTORY",dir.toString());childEnv.put("NODE_EXTRA_CA_CERTS",dir.resolve("server-cert.pem").toString());childEnv.put("C04_SAFE_REPORT",dir.resolve("safe-browser-results.json").toString());
            var process=command.start();try{assertTrue(process.waitFor(180,TimeUnit.SECONDS),"ADMIN_BROWSER_TIMEOUT");assertEquals(0,process.exitValue(),"ADMIN_BROWSER_RUNTIME_FAILED");}finally{finish(process);}
            var result=JsonMapper.builder().build().readTree(Files.readString(dir.resolve("safe-browser-results.json")));assertEquals("passed",result.get("status").asString());assertEquals(5,result.get("passed").asInt());assertEquals(0,result.get("failed").asInt());
        }
        }
    }
}
