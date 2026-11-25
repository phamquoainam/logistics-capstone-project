package com.elogistic.backend.identity.internal;

import com.elogistic.backend.identity.api.admin.*;
import com.elogistic.backend.identity.internal.application.port.IdentityRateLimiter;
import com.elogistic.backend.identity.internal.infrastructure.*;
import java.time.Clock;
import java.util.*;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.sql.init.dependency.DependsOnDatabaseInitialization;
import org.springframework.context.annotation.*;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.context.request.*;

/** Configured canonical engine only; no new credential, realm activation or default key. */
@Configuration @Profile("identity")
@ConditionalOnProperty(name="identity.admin-governance-step-up.enabled",havingValue="true")
public class ProductionGovernanceProofConfiguration {
    public ProductionGovernanceProofConfiguration(Environment environment){
        Set<String> profiles=new HashSet<>();for(String profile:environment.getActiveProfiles())profiles.add(profile.toLowerCase(Locale.ROOT));
        if(!profiles.containsAll(Set.of("identity","postgres"))||profiles.stream().anyMatch(Set.of("production","prod","demo","migration")::contains)||!environment.getProperty("identity.admin-dashboard.enabled",Boolean.class,false)||!Set.of("LOCAL","TEST").contains(environment.getProperty("identity.admin-dashboard.environment","DISABLED"))||!"SANDBOX".equals(environment.getProperty("customer.rollout"))||!"NONE".equalsIgnoreCase(environment.getProperty("server.forward-headers-strategy","NONE")))throw new IllegalStateException("GOVERNANCE_PROOF_LOCAL_TEST_REQUIRED");
    }
    @Bean @DependsOnDatabaseInitialization
    AdminGovernanceReauthentication adminGovernanceReauthentication(DataSource source,Environment environment,IdentityRateLimiter limiter,@Qualifier("identityHttpClock")Clock clock){
        Map<String,byte[]> verifier=Map.of(),encryption=Map.of();
        try{
            var jdbc=new JdbcTemplate(source);jdbc.setQueryTimeout(3);
            if(!Boolean.TRUE.equals(jdbc.queryForObject("SELECT identity.local_admin_environment_matches(?) AND to_regclass('identity.admin_governance_reauthentication') IS NOT NULL AND to_regclass('identity.governance_proof_receipt') IS NOT NULL",Boolean.class,environment.getRequiredProperty("identity.admin-dashboard.environment"))))throw new IllegalStateException();
            verifier=ProductionCustomerConfiguration.ring(environment.getRequiredProperty("customer.security.proof-verifier-keys"));encryption=ProductionCustomerConfiguration.ring(environment.getRequiredProperty("customer.security.delivery-encryption-keys"));
            var protection=new ProtectedCustomerProofs(verifier,environment.getRequiredProperty("customer.security.current-proof-key"),encryption,environment.getRequiredProperty("customer.security.current-encryption-key"));
            return new JdbcAdminGovernanceReauthentication(source,protection,new ProtectedSessionCredentials(),limiter,()->{var attributes=RequestContextHolder.getRequestAttributes();if(!(attributes instanceof ServletRequestAttributes request))throw new AdminFailure("SERVICE_UNAVAILABLE");return request.getRequest().getRemoteAddr();},clock,true);
        }catch(Exception invalid){throw new IllegalStateException("GOVERNANCE_PROOF_CONFIGURATION_INVALID");}
        finally{verifier.values().forEach(k->Arrays.fill(k,(byte)0));encryption.values().forEach(k->Arrays.fill(k,(byte)0));}
    }
}
