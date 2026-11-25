package com.elogistic.backend.merchant.internal;

import com.elogistic.backend.identity.api.admin.*;
import com.elogistic.backend.identity.api.customer.CustomerBusinessContacts;
import com.elogistic.backend.merchant.api.MerchantGovernance;
import com.elogistic.backend.merchant.internal.application.port.MerchantFingerprints;
import com.elogistic.backend.merchant.internal.infrastructure.*;
import com.elogistic.backend.platform.api.PrivateEvidenceStore;
import java.time.Clock;
import java.util.*;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.sql.init.dependency.DependsOnDatabaseInitialization;
import org.springframework.context.annotation.*;
import org.springframework.core.env.Environment;

/** Public ports and retained protection only. Flag enablement is not permission issuance. */
@Configuration @Profile("identity")
@ConditionalOnProperty(name="merchant.admin-governance.enabled",havingValue="true")
public class MerchantGovernanceConfiguration {
    public MerchantGovernanceConfiguration(Environment environment){requireLocal(environment);}
    static void requireLocal(Environment environment){
        requireApplicantLocal(environment);if(!environment.getProperty("identity.admin-dashboard.enabled",Boolean.class,false)||!environment.getProperty("identity.admin-governance-step-up.enabled",Boolean.class,false))throw new IllegalStateException("MERCHANT_GOVERNANCE_LOCAL_TEST_DEPENDENCIES_REQUIRED");
    }
    static void requireApplicantLocal(Environment environment){
        Set<String> profiles=new HashSet<>();for(String profile:environment.getActiveProfiles())profiles.add(profile.toLowerCase(Locale.ROOT));
        if(!profiles.containsAll(Set.of("identity","customer","postgres"))||profiles.stream().anyMatch(Set.of("production","prod","demo","migration")::contains)||!"SANDBOX".equals(environment.getProperty("customer.rollout"))||!Set.of("LOCAL","TEST").contains(environment.getProperty("identity.admin-dashboard.environment","DISABLED"))||!environment.getProperty("identity.business-contact.enabled",Boolean.class,false))throw new IllegalStateException("MERCHANT_GOVERNANCE_LOCAL_TEST_DEPENDENCIES_REQUIRED");
    }
    static ProtectedGovernancePayloads payloads(Environment environment){
        Map<String,byte[]> keys=new HashMap<>();
        try{
            var json=tools.jackson.databind.json.JsonMapper.builder().enable(tools.jackson.core.StreamReadFeature.STRICT_DUPLICATE_DETECTION).build();var root=json.readTree(environment.getRequiredProperty("customer.security.delivery-encryption-keys"));
            if(root==null||!root.isObject()||root.isEmpty()||root.size()>16)throw new IllegalArgumentException();
            for(var field:root.properties()){if(!field.getValue().isString())throw new IllegalArgumentException();String encoded=field.getValue().asString();byte[] key=Base64.getDecoder().decode(encoded);if(!Base64.getEncoder().encodeToString(key).equals(encoded))throw new IllegalArgumentException();keys.put(field.getKey(),key);}
            return new ProtectedGovernancePayloads(keys,environment.getRequiredProperty("customer.security.current-encryption-key"));
        }catch(Exception invalid){throw new IllegalStateException("MERCHANT_GOVERNANCE_PROTECTION_INVALID");}
        finally{keys.values().forEach(k->Arrays.fill(k,(byte)0));}
    }
    @Bean @DependsOnDatabaseInitialization
    MerchantGovernance merchantGovernance(DataSource source,MerchantFingerprints fingerprints,@Qualifier("identityHttpClock")Clock clock,AdminAdmission reads,AdminCommandAdmission commands,CustomerBusinessContacts contacts,AdminGovernanceReauthentication proofs,@Qualifier("claimPrivateEvidenceStore")PrivateEvidenceStore store,Environment environment){return new JdbcMerchantApplicationReviews(source,fingerprints,payloads(environment),clock,reads,commands,contacts,proofs,store);}
    @Bean com.elogistic.backend.merchant.api.MerchantApplicationNoticeEvents merchantApplicationNoticeEvents(DataSource source,com.elogistic.backend.identity.api.customer.CustomerAdmission customers,AdminCommandAdmission admins,MerchantGovernance governance){return new JdbcMerchantApplicationNoticeEvents(source,customers,admins,governance);}

}
