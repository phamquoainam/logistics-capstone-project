package com.elogistic.backend.orders.internal;
import com.elogistic.backend.orders.api.ShippingDrafts;
import com.elogistic.backend.orders.internal.application.ShippingDraftApplication;
import com.elogistic.backend.orders.internal.infrastructure.*;
import java.time.Clock;
import java.util.*;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.sql.init.dependency.DependsOnDatabaseInitialization;
import org.springframework.context.annotation.*;
import org.springframework.core.env.Environment;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.core.StreamReadFeature;

@Configuration @Profile("customer")
public class ShippingDraftConfiguration {
    @Bean @DependsOnDatabaseInitialization
    ShippingDrafts shippingDrafts(DataSource source,Environment env,@Qualifier("identityHttpClock")Clock clock){
        Map<String,byte[]> keys=new HashMap<>();try{var root=JsonMapper.builder().enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION).build().readTree(env.getRequiredProperty("merchant.customer-profile.receipt-keys"));if(root==null||!root.isObject()||root.isEmpty()||root.size()>16)throw new IllegalArgumentException();for(var e:root.properties()){if(!e.getValue().isString())throw new IllegalArgumentException();String value=e.getValue().asString();byte[] key=Base64.getDecoder().decode(value);if(!Base64.getEncoder().encodeToString(key).equals(value))throw new IllegalArgumentException();keys.put(e.getKey(),key);}var fingerprints=new ProtectedShippingDraftFingerprints(keys,env.getRequiredProperty("merchant.customer-profile.current-receipt-key"));JdbcShippingDraftStore.requireSchema(source);return new ShippingDraftApplication(new JdbcShippingDraftStore(source),fingerprints,clock);}catch(Exception invalid){throw new IllegalStateException("SHIPPING_DRAFT_CONFIGURATION_INVALID");}finally{keys.values().forEach(bytes->Arrays.fill(bytes,(byte)0));}
    }
}
