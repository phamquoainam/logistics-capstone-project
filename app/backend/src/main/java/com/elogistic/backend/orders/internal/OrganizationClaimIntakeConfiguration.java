package com.elogistic.backend.orders.internal;

import com.elogistic.backend.orders.api.*;
import com.elogistic.backend.orders.internal.infrastructure.JdbcOrganizationClaimIntakes;
import com.elogistic.backend.orders.internal.infrastructure.JdbcOrganizationClaimEvidence;
import com.elogistic.backend.orders.internal.infrastructure.BusinessPreparationProtection;
import java.time.Clock;
import java.util.*;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.*;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import tools.jackson.core.StreamReadFeature;
import tools.jackson.databind.json.JsonMapper;

@Configuration @Profile("customer")
public class OrganizationClaimIntakeConfiguration {
    @Bean OrganizationClaimEvidence organizationClaimEvidence(DataSource source,OrganizationClaimIntakes claims,BusinessPreparationProtection protection,Environment env,@Qualifier("identityHttpClock")Clock clock){
        boolean enabled=env.getProperty("support.claim-evidence.enabled",Boolean.class,false);
        if(enabled&&!env.getProperty("support.claim-intakes.enabled",Boolean.class,false))throw new IllegalStateException("CLAIM_EVIDENCE_REQUIRES_INTAKE");
        return new JdbcOrganizationClaimEvidence(source,claims,protection,clock,enabled);
    }
    @Bean OrganizationClaimIntakes organizationClaimIntakes(DataSource source,OrganizationSupportTickets tickets,Environment env,@Qualifier("identityHttpClock")Clock clock){
        boolean enabled=false;
        if(env.getProperty("support.claim-intakes.enabled",Boolean.class,false)){
            if(!OrganizationSupportConfiguration.enabled(env)||!env.getProperty("identity.staff.enabled",Boolean.class,false)
                ||!Set.of("LOCAL","TEST").contains(env.getProperty("identity.staff.environment","DISABLED")))throw new IllegalStateException("CLAIM_LOCAL_TEST_ISOLATION_REQUIRED");
            var jdbc=new JdbcTemplate(source);jdbc.setQueryTimeout(3);
            if(!Boolean.TRUE.equals(jdbc.queryForObject("SELECT identity.local_staff_environment_matches(?)",Boolean.class,env.getProperty("identity.staff.environment"))))throw new IllegalStateException("CLAIM_LOCAL_TEST_ISOLATION_REQUIRED");enabled=true;
        }
        var keys=new HashMap<String,byte[]>();try{
            var json=JsonMapper.builder().enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION).build().readTree(env.getRequiredProperty("merchant.customer-profile.receipt-keys"));
            if(json==null||!json.isObject()||json.isEmpty()||json.size()>16)throw new IllegalArgumentException();
            for(var e:json.properties()){if(!e.getValue().isString())throw new IllegalArgumentException();String encoded=e.getValue().asString();byte[] value=Base64.getDecoder().decode(encoded);if(!Base64.getEncoder().encodeToString(value).equals(encoded))throw new IllegalArgumentException();keys.put(e.getKey(),value);}
            return new JdbcOrganizationClaimIntakes(source,tickets,clock,enabled,env.getProperty("support.claim-evidence.enabled",Boolean.class,false),keys,env.getRequiredProperty("merchant.customer-profile.current-receipt-key"));
        }catch(Exception invalid){throw new IllegalStateException("CLAIM_CONFIGURATION_INVALID");}finally{keys.values().forEach(b->Arrays.fill(b,(byte)0));}
    }
}
