package com.elogistic.backend.merchant.api.customer;

import com.elogistic.backend.identity.api.customer.CustomerAdmission;
import java.util.UUID;

/** Independent personal business profile. The locked current Identity context establishes self scope. */
public interface CustomerProfiles {
    Profile get(CustomerAdmission.Context context);
    Profile update(CustomerAdmission.Context context,Update command);
    record Input(String displayName,String preferredLanguage,String businessContactEmail,String businessContactPhone){
        @Override public String toString(){return "CustomerProfileInput[redacted]";}
    }
    record Update(Input input,long expectedVersion,UUID key){@Override public String toString(){return "CustomerProfileUpdate[redacted]";}}
    record Profile(UUID id,UUID accountId,String displayName,String preferredLanguage,String businessContactEmail,String businessContactPhone,long version){
        @Override public String toString(){return "CustomerProfile[redacted]";}
    }
}
