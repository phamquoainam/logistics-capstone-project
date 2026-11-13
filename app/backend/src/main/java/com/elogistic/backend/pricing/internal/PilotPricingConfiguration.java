package com.elogistic.backend.pricing.internal;
import com.elogistic.backend.pricing.api.PilotQuotes;
import com.elogistic.backend.pricing.api.BusinessQuotes;
import com.elogistic.backend.pricing.internal.infrastructure.JdbcBusinessQuotes;
import com.elogistic.backend.pricing.internal.application.PilotQuoteApplication;
import com.elogistic.backend.pricing.internal.infrastructure.JdbcPilotQuoteStore;
import java.time.Clock;
import java.util.*;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.sql.init.dependency.DependsOnDatabaseInitialization;
import org.springframework.context.annotation.*;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.core.StreamReadFeature;
@Configuration @Profile("customer")
public class PilotPricingConfiguration {
    @Bean @DependsOnDatabaseInitialization JdbcPilotQuoteStore pilotQuoteStore(DataSource source,Environment env){Map<String,byte[]> keys=new HashMap<>();try{var root=JsonMapper.builder().enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION).build().readTree(env.getRequiredProperty("merchant.customer-profile.receipt-keys"));if(!root.isObject()||root.isEmpty()||root.size()>16)throw new IllegalArgumentException();for(var e:root.properties()){if(!e.getValue().isString())throw new IllegalArgumentException();String encoded=e.getValue().asString();byte[] key=Base64.getDecoder().decode(encoded);if(!Base64.getEncoder().encodeToString(key).equals(encoded))throw new IllegalArgumentException();keys.put(e.getKey(),key);}return new JdbcPilotQuoteStore(source,keys,env.getRequiredProperty("merchant.customer-profile.current-receipt-key"));}catch(Exception invalid){throw new IllegalStateException("PILOT_PRICING_CONFIGURATION_INVALID");}finally{keys.values().forEach(k->Arrays.fill(k,(byte)0));}}
    private static boolean enabled(Environment env){return Set.of("LOCAL","TEST").contains(env.getProperty("shipping.pilot.environment","DISABLED"))&&"SANDBOX".equals(env.getProperty("customer.rollout"))&&!env.acceptsProfiles(Profiles.of("production","prod"));}
    @Bean PilotQuotes pilotQuotes(JdbcPilotQuoteStore store,Environment env,@Qualifier("identityHttpClock")Clock clock){return new PilotQuoteApplication(store,clock,enabled(env));}
    @Bean BusinessQuotes businessQuotes(DataSource source,JdbcPilotQuoteStore store,Environment env,@Qualifier("identityHttpClock")Clock clock){return new JdbcBusinessQuotes(source,store,clock,enabled(env));}
}
