package com.elogistic.backend.identity.internal;

import com.elogistic.backend.identity.api.IdentityCommandFailure;
import com.elogistic.backend.identity.api.staff.*;
import com.elogistic.backend.identity.internal.infrastructure.*;
import com.elogistic.backend.identity.internal.web.*;
import java.time.Clock;
import java.util.*;
import java.util.function.Function;
import javax.sql.DataSource;
import org.springframework.boot.sql.init.dependency.DependsOnDatabaseInitialization;
import org.springframework.context.annotation.*;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/** Staff endpoints are deny-only by default; enabling requires both explicit config and physical DB binding. */
@Configuration @Profile("customer") @Import({ProductionStaffWebConfiguration.class,ProductionOperationsConfiguration.class})
public class ProductionStaffConfiguration {
    static boolean enabled(Environment environment){
        String raw=environment.getProperty("identity.staff.enabled","false");
        if(!Set.of("true","false").contains(raw))throw new IllegalStateException("STAFF_CONFIGURATION_INVALID");
        if(raw.equals("false"))return false;
        var profiles=Set.of(environment.getActiveProfiles());
        if(!profiles.containsAll(Set.of("customer","identity","postgres"))
            ||profiles.stream().anyMatch(p->Set.of("production","prod","demo","migration").contains(p.toLowerCase(Locale.ROOT)))
            ||!"SANDBOX".equals(environment.getProperty("customer.rollout"))
            ||!Set.of("LOCAL","TEST").contains(environment.getProperty("identity.staff.environment","DISABLED"))
            ||!"NONE".equalsIgnoreCase(environment.getProperty("server.forward-headers-strategy","NONE")))throw new IllegalStateException("STAFF_LOCAL_TEST_ISOLATION_REQUIRED");
        return true;
    }
    private static IdentityCommandFailure invalid(){return new IdentityCommandFailure(IdentityCommandFailure.Code.SESSION_INVALID);}
    private static ProductionStaffComposition.Services disabled(){
        var admission=new StaffAdmission(){
            public <T>T execute(String token,UUID request,Set<UUID> ids,Function<Context,T> work){throw invalid();}
            public void requireCurrent(Context c){throw invalid();}public void meaningfulAction(Context c){throw invalid();}
        };
        return new ProductionStaffComposition.Services(admission,()->{throw invalid();},ProductionStaffGuard.disabled(),null);
    }
    @Bean @DependsOnDatabaseInitialization
    ProductionStaffComposition.Services staffServices(DataSource source,Environment environment){
        if(!enabled(environment))return disabled();
        Map<String,byte[]> verifiers=Map.of(),encryptors=Map.of();byte[] limiter=null;
        try {
            CustomerRuntimeRequirements.verify(source);var jdbc=new JdbcTemplate(source);jdbc.setQueryTimeout(3);
            String current=environment.getRequiredProperty("identity.staff.environment");
            java.util.function.BooleanSupplier available=()->Boolean.TRUE.equals(jdbc.queryForObject("SELECT identity.local_staff_environment_matches(?)",Boolean.class,current));
            if(!available.getAsBoolean())throw new IllegalStateException();
            Clock clock=Clock.systemUTC();var tokens=ProductionCustomerConfiguration.staffTokens(environment,clock);
            verifiers=ProductionCustomerConfiguration.ring(environment.getRequiredProperty("customer.security.proof-verifier-keys"));
            encryptors=ProductionCustomerConfiguration.ring(environment.getRequiredProperty("customer.security.delivery-encryption-keys"));
            limiter=ProductionCustomerConfiguration.secret(environment.getRequiredProperty("customer.security.limiter-hmac"));
            for(byte[] key:verifiers.values())if(Arrays.equals(limiter,key))throw new IllegalArgumentException();
            for(byte[] key:encryptors.values())if(Arrays.equals(limiter,key))throw new IllegalArgumentException();
            var protection=new InvitationProtection(verifiers,environment.getRequiredProperty("customer.security.current-proof-key"),encryptors,environment.getRequiredProperty("customer.security.current-encryption-key"));
            return ProductionStaffComposition.create(source,tokens,protection,new JdbcIdentityRateLimiter(source,limiter),ProductionCustomerConfiguration.customerTransport(environment,clock),environment.getRequiredProperty("customer.security.origin"),current,clock,available);
        }catch(Exception unavailable){throw new IllegalStateException("STAFF_RUNTIME_CONFIGURATION_INVALID");}
        finally{verifiers.values().forEach(k->Arrays.fill(k,(byte)0));encryptors.values().forEach(k->Arrays.fill(k,(byte)0));if(limiter!=null)Arrays.fill(limiter,(byte)0);}
    }
    @Bean StaffAdmission staffAdmission(ProductionStaffComposition.Services services){return services.admission();}
    @Bean @Primary StaffRequestContext staffRequestContext(ProductionStaffComposition.Services services){return services.requests();}
    @Bean StaffDeliverySchedule staffDeliverySchedule(ProductionStaffComposition.Services services){return new StaffDeliverySchedule(services);}
    static final class StaffDeliverySchedule {
        private final ProductionStaffComposition.Services services;
        StaffDeliverySchedule(ProductionStaffComposition.Services services){this.services=services;}
        @org.springframework.scheduling.annotation.Scheduled(fixedDelay=1000) public void deliver(){
            if(services.delivery()==null)return;
            try{for(int batch=0;batch<10&&services.delivery().runOne();batch++){/* bounded transactions */}}
            catch(RuntimeException unavailable){org.slf4j.LoggerFactory.getLogger(StaffDeliverySchedule.class).warn("STAFF_DELIVERY_WORKER_UNAVAILABLE");}
        }
    }

}
