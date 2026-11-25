package com.elogistic.backend.identity.internal;

import com.elogistic.backend.identity.internal.application.port.*;
import com.elogistic.backend.identity.internal.infrastructure.*;
import com.elogistic.backend.identity.internal.infrastructure.LocalAdminInvitationActivation;
import com.elogistic.backend.identity.internal.web.ProductionAdminInvitationHttp;
import java.time.*;
import java.util.*;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.sql.init.dependency.DependsOnDatabaseInitialization;
import org.springframework.context.annotation.*;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.*;

/** Default-off isolated onboarding runtime. Offline operator credentials are never injected here. */
@Configuration @Profile("identity") @EnableScheduling
public class ProductionAdminOnboardingConfiguration {
    record Services(ProductionAdminInvitationHttp http,AdminInvitationDeliveryWorker delivery){}
    static boolean flag(Environment e,String key){String value=e.getProperty(key,"false");if(!Set.of("true","false").contains(value))throw new IllegalStateException("ADMIN_ONBOARDING_CONFIGURATION_INVALID");return value.equals("true");}
    static boolean enabled(Environment e){
        boolean enabled=flag(e,"identity.admin-onboarding.enabled"),delivery=flag(e,"identity.admin-invitations.delivery-enabled"),bootstrap=flag(e,"identity.local-realm-bootstrap.enabled");
        if(!enabled){if(delivery||bootstrap)throw new IllegalStateException("ADMIN_ONBOARDING_REQUIRED");return false;}
        var profiles=new HashSet<String>();for(String p:e.getActiveProfiles())profiles.add(p.toLowerCase(Locale.ROOT));
        if(!profiles.containsAll(Set.of("identity","postgres"))||profiles.stream().anyMatch(Set.of("production","prod","demo","migration")::contains)||!"SANDBOX".equals(e.getProperty("customer.rollout"))||!Set.of("LOCAL","TEST").contains(e.getProperty("identity.admin-dashboard.environment","DISABLED"))||!"NONE".equalsIgnoreCase(e.getProperty("server.forward-headers-strategy","NONE")))throw new IllegalStateException("ADMIN_LOCAL_TEST_ISOLATION_REQUIRED");
        return true;
    }
    @Bean @DependsOnDatabaseInitialization
    Services adminOnboardingServices(DataSource source,Environment environment,IdentityBrowserState browser,IdentityRateLimiter limiter,@Qualifier("identityHttpClock")Clock clock){
        if(!enabled(environment))return new Services(ProductionAdminInvitationHttp.disabled(),null);
        Map<String,byte[]> verifiers=Map.of(),encryptors=Map.of();
        try{
            CustomerRuntimeRequirements.verify(source);var jdbc=new JdbcTemplate(source);jdbc.setQueryTimeout(3);
            String current=environment.getRequiredProperty("identity.admin-dashboard.environment");boolean bootstrap=flag(environment,"identity.local-realm-bootstrap.enabled");
            java.util.function.BooleanSupplier available=()->Boolean.TRUE.equals(jdbc.queryForObject("SELECT identity.local_admin_environment_matches(?)",Boolean.class,current));
            if(!available.getAsBoolean()||bootstrap&&!Boolean.TRUE.equals(jdbc.queryForObject("SELECT to_regprocedure('identity.accept_local_realm_bootstrap_invitation(uuid,uuid,bytea,text,uuid)') IS NOT NULL",Boolean.class)))throw new IllegalStateException();
            if(source instanceof com.zaxxer.hikari.HikariDataSource pool&&(pool.getMaximumPoolSize()<2||pool.getConnectionTimeout()>10000))throw new IllegalStateException();
            verifiers=ProductionCustomerConfiguration.ring(environment.getRequiredProperty("customer.security.proof-verifier-keys"));encryptors=ProductionCustomerConfiguration.ring(environment.getRequiredProperty("customer.security.delivery-encryption-keys"));
            byte[] limiterKey=ProductionCustomerConfiguration.secret(environment.getRequiredProperty("identity.security.limiter-hmac"));
            try{for(byte[] value:verifiers.values())if(Arrays.equals(value,limiterKey))throw new IllegalStateException();for(byte[] value:encryptors.values())if(Arrays.equals(value,limiterKey))throw new IllegalStateException();}
            finally{Arrays.fill(limiterKey,(byte)0);}
            var protection=new InvitationProtection(verifiers,environment.getRequiredProperty("customer.security.current-proof-key"),encryptors,environment.getRequiredProperty("customer.security.current-encryption-key"));
            var activation=new LocalAdminInvitationActivation(source,protection,clock,current,bootstrap);
            var http=new ProductionAdminInvitationHttp(activation,browser,limiter,environment.getRequiredProperty("identity.security.origin"),available);
            AdminInvitationDeliveryWorker worker=null;
            if(flag(environment,"identity.admin-invitations.delivery-enabled")){
                if(!environment.getRequiredProperty("identity.security.origin").equals(environment.getRequiredProperty("customer.security.origin")))throw new IllegalStateException();
                worker=new AdminInvitationDeliveryWorker(JdbcInvitationOutbox.admin(source,protection,new JdbcInvitationOutbox.Policy(3,Duration.ofSeconds(30),Duration.ofSeconds(30)),clock,current,bootstrap),ProductionCustomerConfiguration.customerTransport(environment,clock),available);
            }
            return new Services(http,worker);
        }catch(Exception unavailable){throw new IllegalStateException("ADMIN_ONBOARDING_RUNTIME_CONFIGURATION_INVALID");}
        finally{verifiers.values().forEach(k->Arrays.fill(k,(byte)0));encryptors.values().forEach(k->Arrays.fill(k,(byte)0));}
    }
    @Bean ProductionAdminInvitationHttp adminInvitationHttp(Services services){return services.http();}
    @Bean AdminDeliverySchedule adminDeliverySchedule(Services services){return new AdminDeliverySchedule(services);}
    static final class AdminDeliverySchedule {
        private final Services services;AdminDeliverySchedule(Services services){this.services=services;}
        @Scheduled(fixedDelay=1000)public void deliver(){
            if(services.delivery()==null)return;
            try{for(int n=0;n<10&&services.delivery().runOne();n++){/* Independent bounded transactions. */}}
            catch(RuntimeException unavailable){org.slf4j.LoggerFactory.getLogger(AdminDeliverySchedule.class).warn("ADMIN_DELIVERY_WORKER_UNAVAILABLE");}
        }
    }
}
