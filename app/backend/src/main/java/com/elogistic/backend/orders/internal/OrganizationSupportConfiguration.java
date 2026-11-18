package com.elogistic.backend.orders.internal;

import com.elogistic.backend.orders.api.*;
import com.elogistic.backend.orders.internal.infrastructure.JdbcOrganizationSupportTickets;
import java.time.Clock;
import java.util.*;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.*;
import org.springframework.core.env.*;
import tools.jackson.core.StreamReadFeature;
import tools.jackson.databind.json.JsonMapper;

@Configuration @Profile("customer")
public class OrganizationSupportConfiguration {
    public static boolean enabled(Environment env){return env.getProperty("support.tickets.organization-enabled",Boolean.class,false)
        &&env.getProperty("support.tickets.enabled",Boolean.class,false)&&Set.of("LOCAL","TEST").contains(env.getProperty("support.tickets.environment","DISABLED"))
        &&"SANDBOX".equals(env.getProperty("customer.rollout"))&&!env.acceptsProfiles(Profiles.of("production","prod"));}
    @Bean OrganizationSupportTickets organizationSupportTickets(DataSource source,BusinessShipments shipments,Environment env,@Qualifier("identityHttpClock")Clock clock){
        // Reuse the existing protected receipt keyring. No new secret/provider or stored plaintext request body.
        Map<String,byte[]> keys=new HashMap<>();try{var root=JsonMapper.builder().enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION).build().readTree(env.getRequiredProperty("merchant.customer-profile.receipt-keys"));
            if(root==null||!root.isObject()||root.isEmpty()||root.size()>16)throw new IllegalArgumentException();
            for(var e:root.properties()){if(!e.getValue().isString())throw new IllegalArgumentException();String value=e.getValue().asString();byte[] key=Base64.getDecoder().decode(value);if(!Base64.getEncoder().encodeToString(key).equals(value))throw new IllegalArgumentException();keys.put(e.getKey(),key);}
            return new JdbcOrganizationSupportTickets(source,shipments,clock,enabled(env),keys,env.getRequiredProperty("merchant.customer-profile.current-receipt-key"));
        }catch(Exception invalid){throw new IllegalStateException("ORGANIZATION_SUPPORT_CONFIGURATION_INVALID");}finally{keys.values().forEach(b->Arrays.fill(b,(byte)0));}
    }
}
