package com.elogistic.backend.identity.internal;

import com.elogistic.backend.identity.api.customer.*;
import com.elogistic.backend.identity.internal.infrastructure.*;
import com.elogistic.backend.identity.internal.web.ProductionAccessTokens;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.*;
import java.security.interfaces.RSAPublicKey;
import java.security.interfaces.RSAPrivateKey;
import java.time.Clock;
import java.util.*;
import javax.sql.DataSource;
import org.slf4j.LoggerFactory;
import org.springframework.boot.sql.init.dependency.DependsOnDatabaseInitialization;
import org.springframework.context.annotation.*;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.*;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.core.StreamReadFeature;

/** Explicit customer+identity+postgres sandbox deployment. No public rollout or generated/default secrets. */
@Configuration @Profile("customer") @EnableScheduling @Import(ProductionCustomerWebConfiguration.class)
public class ProductionCustomerConfiguration {
    private static final JsonMapper JSON=JsonMapper.builder().enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION).build();
    public ProductionCustomerConfiguration(Environment environment){
        var profiles=Set.of(environment.getActiveProfiles());
        if(!profiles.containsAll(Set.of("postgres","identity")) || profiles.contains("demo") || profiles.contains("migration")
            || !"SANDBOX".equals(environment.getRequiredProperty("customer.rollout"))
            || !"NONE".equalsIgnoreCase(environment.getProperty("server.forward-headers-strategy","NONE")))throw new IllegalStateException("CUSTOMER_SANDBOX_PROFILE_ISOLATION_REQUIRED");
    }
    private static byte[] secret(String value){
        byte[] decoded=Base64.getDecoder().decode(value);if(!Base64.getEncoder().encodeToString(decoded).equals(value) || decoded.length!=32 || Arrays.equals(decoded,new byte[32]))throw new IllegalArgumentException();return decoded;
    }
    private static Map<String,byte[]> ring(String value){
        var root=JSON.readTree(value);if(root==null || !root.isObject() || root.isEmpty() || root.size()>16)throw new IllegalArgumentException();
        Map<String,byte[]> ring=new HashMap<>();for(var entry:root.properties()){if(!entry.getValue().isString())throw new IllegalArgumentException();ring.put(entry.getKey(),secret(entry.getValue().asString()));}return ring;
    }
    static ProductionAccessTokens customerTokens(Environment environment,Clock clock){
        try{
            String issuer=environment.getRequiredProperty("customer.security.issuer"),audience=environment.getRequiredProperty("customer.security.audience"),kid=environment.getRequiredProperty("customer.security.current-kid");
            if(issuer.equals(environment.getRequiredProperty("identity.security.issuer")) || audience.equals(environment.getRequiredProperty("identity.security.audience")))throw new IllegalArgumentException();
            var system=JWKSet.parse(environment.getRequiredProperty("identity.security.jwk-set"));var customer=JWKSet.parse(environment.getRequiredProperty("customer.security.jwk-set"));
            if(system.getKeys().isEmpty())throw new IllegalArgumentException();
            Map<String,RSAPublicKey> publicKeys=new HashMap<>();RSAPrivateKey signer=null;
            for(JWK key:customer.getKeys()){
                if(!(key instanceof RSAKey rsa) || !JWSAlgorithm.RS256.equals(key.getAlgorithm()) || !KeyUse.SIGNATURE.equals(key.getKeyUse()) || publicKeys.containsKey(key.getKeyID()))throw new IllegalArgumentException();
                var publicKey=rsa.toRSAPublicKey();
                for(JWK old:system.getKeys())if(!(old instanceof RSAKey oldRsa) || Arrays.equals(publicKey.getEncoded(),oldRsa.toRSAPublicKey().getEncoded()))throw new IllegalArgumentException();
                publicKeys.put(key.getKeyID(),publicKey);
                if(kid.equals(key.getKeyID())){if(!rsa.isPrivate())throw new IllegalArgumentException();signer=rsa.toRSAPrivateKey();}else if(rsa.isPrivate())throw new IllegalArgumentException();
            }
            return new ProductionAccessTokens(issuer,audience,kid,signer,publicKeys,clock);
        }catch(Exception invalid){throw new IllegalStateException("CUSTOMER_JWT_PURPOSE_CONFIGURATION_INVALID");}
    }
    @Bean @DependsOnDatabaseInitialization
    ProductionCustomerComposition.Services customerServices(DataSource source,Environment environment){
        Map<String,byte[]> verifiers=Map.of(),encryptors=Map.of();byte[] limiter=null,systemLimiter=null;
        try{
            CustomerRuntimeRequirements.verify(source);Clock clock=Clock.systemUTC();var tokens=customerTokens(environment,clock);
            verifiers=ring(environment.getRequiredProperty("customer.security.proof-verifier-keys"));encryptors=ring(environment.getRequiredProperty("customer.security.delivery-encryption-keys"));
            limiter=secret(environment.getRequiredProperty("customer.security.limiter-hmac"));systemLimiter=secret(environment.getRequiredProperty("identity.security.limiter-hmac"));
            if(Arrays.equals(limiter,systemLimiter))throw new IllegalArgumentException();
            for(byte[] key:verifiers.values())if(Arrays.equals(limiter,key))throw new IllegalArgumentException();for(byte[] key:encryptors.values())if(Arrays.equals(limiter,key))throw new IllegalArgumentException();
            var protection=new ProtectedCustomerProofs(verifiers,environment.getRequiredProperty("customer.security.current-proof-key"),encryptors,environment.getRequiredProperty("customer.security.current-encryption-key"));
            var mode=SmtpCustomerDelivery.Mode.valueOf(environment.getRequiredProperty("customer.smtp.mode"));boolean loopback=mode==SmtpCustomerDelivery.Mode.TEST_LOOPBACK;
            var domains=new HashSet<>(Arrays.asList(environment.getRequiredProperty("customer.smtp.approved-sender-domains").split(",",-1)));
            var smtp=new SmtpCustomerDelivery(new SmtpCustomerDelivery.Settings(environment.getRequiredProperty("customer.smtp.host"),environment.getRequiredProperty("customer.smtp.port",Integer.class),mode,
                loopback?null:environment.getRequiredProperty("customer.smtp.username"),loopback?null:environment.getRequiredProperty("customer.smtp.password"),environment.getRequiredProperty("customer.smtp.sender"),domains,environment.getRequiredProperty("customer.security.origin"),true),clock);
            return ProductionCustomerComposition.create(source,tokens,protection,new JdbcIdentityRateLimiter(source,limiter),smtp,environment.getRequiredProperty("customer.security.origin"),clock);
        }catch(Exception invalid){throw new IllegalStateException("CUSTOMER_RUNTIME_CONFIGURATION_INVALID");}
        finally{verifiers.values().forEach(key->Arrays.fill(key,(byte)0));encryptors.values().forEach(key->Arrays.fill(key,(byte)0));if(limiter!=null)Arrays.fill(limiter,(byte)0);if(systemLimiter!=null)Arrays.fill(systemLimiter,(byte)0);}
    }
    @Bean CustomerAdmission customerAdmission(ProductionCustomerComposition.Services services){return services.customers();}
    @Bean CustomerRequestContext customerRequestContext(ProductionCustomerComposition.Services services){return services.requests();}
    @Bean CustomerReauthentication customerReauthentication(ProductionCustomerComposition.Services services){return services.reauthentication();}
    @Bean CustomerOwnerRecoveryConsents customerOwnerRecoveryConsents(ProductionCustomerComposition.Services services){return services.consents();}
    @Bean CustomerDeliverySchedule customerDeliverySchedule(ProductionCustomerComposition.Services services){return new CustomerDeliverySchedule(services);}
    static final class CustomerDeliverySchedule {
        private final ProductionCustomerComposition.Services services;
        CustomerDeliverySchedule(ProductionCustomerComposition.Services services){this.services=services;}
        @Scheduled(fixedDelay=1000) public void deliver(){
            try{for(int batch=0;batch<10 && services.delivery().runOne();batch++){/* Each work item owns bounded transactions. */}}
            catch(RuntimeException unavailable){LoggerFactory.getLogger(CustomerDeliverySchedule.class).warn("CUSTOMER_DELIVERY_WORKER_UNAVAILABLE");}
        }
    }
}
