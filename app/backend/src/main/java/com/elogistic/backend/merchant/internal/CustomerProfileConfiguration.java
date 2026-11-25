package com.elogistic.backend.merchant.internal;

import com.elogistic.backend.identity.api.customer.CustomerAdmission;
import com.elogistic.backend.merchant.api.customer.CustomerProfiles;
import com.elogistic.backend.merchant.api.customer.MerchantApplications;
import com.elogistic.backend.merchant.api.customer.PersonalAddresses;
import com.elogistic.backend.merchant.internal.application.PersonalAddressApplication;
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
    @Bean com.elogistic.backend.merchant.api.customer.MerchantFacilities merchantFacilities(DataSource source,MerchantFingerprints fingerprints,@Qualifier("identityHttpClock")Clock clock,Environment env){boolean enabled=Set.of("LOCAL","TEST").contains(env.getProperty("shipping.pilot.environment","DISABLED"))&&"SANDBOX".equals(env.getProperty("customer.rollout"))&&!env.acceptsProfiles(org.springframework.core.env.Profiles.of("production","prod"));return new JdbcMerchantFacilities(source,fingerprints,clock,enabled);}
    @Bean JdbcOrganizationContacts organizationContacts(DataSource source,MerchantFingerprints fingerprints,@Qualifier("identityHttpClock")Clock clock,Environment env){boolean enabled=Set.of("LOCAL","TEST").contains(env.getProperty("shipping.pilot.environment","DISABLED"))&&"SANDBOX".equals(env.getProperty("customer.rollout"))&&!env.acceptsProfiles(org.springframework.core.env.Profiles.of("production","prod"));return new JdbcOrganizationContacts(source,fingerprints,clock,enabled);}
    @Bean com.elogistic.backend.merchant.api.customer.OrganizationContactTransfers organizationContactTransfers(DataSource source,JdbcOrganizationContacts contacts,MerchantFingerprints fingerprints,@Qualifier("identityHttpClock")Clock clock){return new JdbcOrganizationContactTransfers(source,contacts,fingerprints,clock);}
    @Bean com.elogistic.backend.merchant.api.customer.PilotCoverage pilotCoverage(DataSource source,CustomerAdmission admission,@Qualifier("identityHttpClock")Clock clock){return new JdbcPilotCoverage(source,admission,clock);}
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
    MerchantApplications merchantApplications(DataSource source,CustomerAdmission admission,MerchantFingerprints fingerprints,@Qualifier("identityHttpClock")Clock clock,org.springframework.beans.factory.ObjectProvider<com.elogistic.backend.identity.api.customer.CustomerBusinessContacts> contacts,@Qualifier("claimPrivateEvidenceStore")org.springframework.beans.factory.ObjectProvider<com.elogistic.backend.platform.api.PrivateEvidenceStore> store,@Qualifier("claimEvidenceScreening")org.springframework.beans.factory.ObjectProvider<com.elogistic.backend.platform.api.PrivateEvidenceScreening> scanner,Environment environment){
        JdbcMerchantApplicationStore.requireSchema(source);
        if(!environment.getProperty("merchant.application-workflow.enabled",Boolean.class,false))return new MerchantApplicationService(admission,new JdbcMerchantApplicationStore(source),fingerprints,clock);
        MerchantGovernanceConfiguration.requireApplicantLocal(environment);var jdbc=new org.springframework.jdbc.core.JdbcTemplate(source);jdbc.setQueryTimeout(3);
        if(!Boolean.TRUE.equals(jdbc.queryForObject("SELECT to_regclass('merchant.application_policy') IS NOT NULL AND to_regclass('merchant.application_revision') IS NOT NULL",Boolean.class)))throw new IllegalStateException("MERCHANT_APPLICATION_V42_SCHEMA_REQUIRED");
        return new JdbcMerchantApplicationWorkflow(source,admission,contacts.getObject(),fingerprints,store.getObject(),scanner.getObject(),MerchantGovernanceConfiguration.payloads(environment),clock,true,environment.getRequiredProperty("identity.admin-dashboard.environment"));
    }
    @Bean @DependsOnDatabaseInitialization
    PersonalAddresses personalAddresses(DataSource source,CustomerAdmission admission,MerchantFingerprints fingerprints,@Qualifier("identityHttpClock")Clock clock){
        JdbcPersonalAddressStore.requireSchema(source);return new PersonalAddressApplication(admission,new JdbcPersonalAddressStore(source),fingerprints,clock);
    }
}
