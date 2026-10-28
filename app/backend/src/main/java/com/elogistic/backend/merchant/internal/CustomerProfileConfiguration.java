package com.elogistic.backend.merchant.internal;

import com.elogistic.backend.identity.api.customer.CustomerAdmission;
import com.elogistic.backend.merchant.api.customer.CustomerProfiles;
import com.elogistic.backend.merchant.internal.application.CustomerProfileApplication;
import com.elogistic.backend.merchant.internal.infrastructure.*;
import java.time.Clock;
import java.util.*;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.sql.init.dependency.DependsOnDatabaseInitialization;
import org.springframework.context.annotation.*;
import org.springframework.core.env.Environment;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.core.StreamReadFeature;

/** Existing Merchant module hosts a separate personal aggregate, with no org or Identity SQL dependency. */
@Configuration @Profile("customer")
public class CustomerProfileConfiguration {
    private static final JsonMapper JSON=JsonMapper.builder().enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION).build();
    @Bean @DependsOnDatabaseInitialization
    CustomerProfiles customerProfiles(DataSource source,CustomerAdmission admission,@Qualifier("identityHttpClock")Clock clock,Environment environment){
        Map<String,byte[]> keys=new HashMap<>();
        try{
            JdbcCustomerProfileStore.requireSchema(source);
            var root=JSON.readTree(environment.getRequiredProperty("merchant.customer-profile.receipt-keys"));if(root==null || !root.isObject() || root.isEmpty() || root.size()>16)throw new IllegalArgumentException();
            for(var entry:root.properties()){
                if(!entry.getValue().isString())throw new IllegalArgumentException();String encoded=entry.getValue().asString();byte[] key=Base64.getDecoder().decode(encoded);
                if(!Base64.getEncoder().encodeToString(key).equals(encoded))throw new IllegalArgumentException();keys.put(entry.getKey(),key);
            }
            var fingerprints=new ProtectedMerchantFingerprints(keys,environment.getRequiredProperty("merchant.customer-profile.current-receipt-key"));
            return new CustomerProfileApplication(admission,new JdbcCustomerProfileStore(source),fingerprints,clock);
        }catch(Exception invalid){throw new IllegalStateException("CUSTOMER_PROFILE_CONFIGURATION_INVALID");}
        finally{keys.values().forEach(key->Arrays.fill(key,(byte)0));}
    }
}
