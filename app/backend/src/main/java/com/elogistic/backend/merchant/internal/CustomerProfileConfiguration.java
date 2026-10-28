package com.elogistic.backend.merchant.internal;

import com.elogistic.backend.identity.api.customer.CustomerAdmission;
import com.elogistic.backend.merchant.api.customer.CustomerProfiles;
import com.elogistic.backend.merchant.api.customer.MerchantApplications;
import com.elogistic.backend.merchant.internal.application.MerchantApplicationService;
import com.elogistic.backend.merchant.internal.application.port.MerchantFingerprints;
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
    @Bean
    MerchantFingerprints merchantReceiptFingerprints(Environment environment){
        Map<String,byte[]> keys=new HashMap<>();
        try{
            var root=JSON.readTree(environment.getRequiredProperty("merchant.customer-profile.receipt-keys"));if(root==null || !root.isObject() || root.isEmpty() || root.size()>16)throw new IllegalArgumentException();
            for(var entry:root.properties()){
                if(!entry.getValue().isString())throw new IllegalArgumentException();String encoded=entry.getValue().asString();byte[] key=Base64.getDecoder().decode(encoded);
                if(!Base64.getEncoder().encodeToString(key).equals(encoded))throw new IllegalArgumentException();keys.put(entry.getKey(),key);
            }
            return new ProtectedMerchantFingerprints(keys,environment.getRequiredProperty("merchant.customer-profile.current-receipt-key"));
        }catch(Exception invalid){throw new IllegalStateException("CUSTOMER_PROFILE_CONFIGURATION_INVALID");}
        finally{keys.values().forEach(key->Arrays.fill(key,(byte)0));}
    }
    @Bean @DependsOnDatabaseInitialization
    CustomerProfiles customerProfiles(DataSource source,CustomerAdmission admission,MerchantFingerprints fingerprints,@Qualifier("identityHttpClock")Clock clock){
        JdbcCustomerProfileStore.requireSchema(source);return new CustomerProfileApplication(admission,new JdbcCustomerProfileStore(source),fingerprints,clock);
    }
    @Bean @DependsOnDatabaseInitialization
    MerchantApplications merchantApplications(DataSource source,CustomerAdmission admission,MerchantFingerprints fingerprints,@Qualifier("identityHttpClock")Clock clock){
        JdbcMerchantApplicationStore.requireSchema(source);return new MerchantApplicationService(admission,new JdbcMerchantApplicationStore(source),fingerprints,clock);
    }
}
