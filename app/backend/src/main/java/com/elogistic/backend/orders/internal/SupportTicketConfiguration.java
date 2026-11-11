package com.elogistic.backend.orders.internal;
import com.elogistic.backend.orders.api.*;
import com.elogistic.backend.orders.internal.infrastructure.JdbcSupportTickets;
import java.time.Clock;
import java.util.*;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.*;
import org.springframework.core.env.*;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.core.StreamReadFeature;
@Configuration @Profile("customer")
public class SupportTicketConfiguration {
    @Bean SupportTickets supportTickets(DataSource source,PilotShipments shipments,Environment env,@Qualifier("identityHttpClock")Clock clock){
        boolean enabled=env.getProperty("support.tickets.enabled",Boolean.class,false)&&Set.of("LOCAL","TEST").contains(env.getProperty("support.tickets.environment","DISABLED"))&&"SANDBOX".equals(env.getProperty("customer.rollout"))&&!env.acceptsProfiles(Profiles.of("production","prod"));
        Map<String,byte[]> keys=new HashMap<>();try{var root=JsonMapper.builder().enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION).build().readTree(env.getRequiredProperty("merchant.customer-profile.receipt-keys"));if(root==null||!root.isObject()||root.isEmpty()||root.size()>16)throw new IllegalArgumentException();for(var e:root.properties()){if(!e.getValue().isString())throw new IllegalArgumentException();String value=e.getValue().asString();byte[] key=Base64.getDecoder().decode(value);if(!Base64.getEncoder().encodeToString(key).equals(value))throw new IllegalArgumentException();keys.put(e.getKey(),key);}return new JdbcSupportTickets(source,shipments,clock,enabled,keys,env.getRequiredProperty("merchant.customer-profile.current-receipt-key"));}catch(Exception invalid){throw new IllegalStateException("SUPPORT_TICKET_CONFIGURATION_INVALID");}finally{keys.values().forEach(bytes->Arrays.fill(bytes,(byte)0));}
    }
}
