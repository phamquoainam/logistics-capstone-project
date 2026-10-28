package com.elogistic.backend.merchant.internal.application;

import com.elogistic.backend.identity.api.customer.CustomerAdmission;
import com.elogistic.backend.merchant.api.*;
import com.elogistic.backend.merchant.api.customer.CustomerProfiles;
import com.elogistic.backend.merchant.internal.application.port.*;
import com.elogistic.backend.merchant.internal.domain.CustomerBusinessProfile;
import java.security.MessageDigest;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import static com.elogistic.backend.merchant.api.OnboardingFailure.Code.*;

public final class CustomerProfileApplication implements CustomerProfiles {
    private final CustomerAdmission admission;private final CustomerProfileStore store;private final MerchantFingerprints fingerprints;private final Clock clock;
    public CustomerProfileApplication(CustomerAdmission admission,CustomerProfileStore store,MerchantFingerprints fingerprints,Clock clock){this.admission=Objects.requireNonNull(admission);this.store=Objects.requireNonNull(store);this.fingerprints=Objects.requireNonNull(fingerprints);this.clock=Objects.requireNonNull(clock);}
    private Instant now(){return clock.instant().truncatedTo(ChronoUnit.MICROS);}
    private CustomerBusinessProfile virtual(CustomerAdmission.Context context){
        var actor=context.actor();Instant now=now();
        try{return new CustomerBusinessProfile(actor.accountId(),actor.accountId(),actor.displayName(),"vi",null,null,1,now,now);}
        catch(IllegalArgumentException legacyDisplay){return new CustomerBusinessProfile(actor.accountId(),actor.accountId(),"Khách hàng","vi",null,null,1,now,now);}
    }
    private static Profile view(CustomerBusinessProfile p){return new Profile(p.id(),p.accountId(),p.displayName(),p.preferredLanguage(),p.businessContactEmail(),p.businessContactPhone(),p.version());}
    private byte[] fingerprint(String key,Update command,UUID target){var input=command.input();return fingerprints.hash(key,"updateCustomerProfile",target,command.expectedVersion(),input.displayName(),input.preferredLanguage(),input.businessContactEmail(),input.businessContactPhone());}
    @Override public Profile get(CustomerAdmission.Context context){admission.requireCurrent(context);var profile=store.find(context.actor().accountId(),false).orElseGet(()->virtual(context));admission.requireCurrent(context);return view(profile);}
    @Override public Profile update(CustomerAdmission.Context context,Update command){
        admission.requireCurrent(context);if(command==null || command.input()==null || command.key()==null || command.expectedVersion()<1)throw new OnboardingFailure(VALIDATION_FAILED);
        UUID account=context.actor().accountId();store.lockControl();var existing=store.find(account,true);var before=existing.orElseGet(()->virtual(context));var receipt=store.receipt(account,before.id(),command.key());
        if(receipt.isPresent()){
            var old=receipt.get();byte[] hash=fingerprint(old.keyId(),command,before.id()),stored=old.fingerprint();
            try{if(old.originalVersion()!=command.expectedVersion() || !MessageDigest.isEqual(hash,stored))throw new OnboardingFailure(IDEMPOTENCY_CONFLICT);}
            finally{Arrays.fill(hash,(byte)0);Arrays.fill(stored,(byte)0);}
            var original=store.version(account,before.id(),old.resultVersion());admission.requireCurrent(context);return view(original);
        }
        if(before.version()!=command.expectedVersion())throw new OnboardingFailure(PRECONDITION_STALE);
        CustomerBusinessProfile after;var input=command.input();
        try{after=before.replace(input.displayName(),input.preferredLanguage(),input.businessContactEmail(),input.businessContactPhone(),command.expectedVersion(),now());}
        catch(IllegalArgumentException invalid){throw new OnboardingFailure(VALIDATION_FAILED);}
        store.save(before,after,existing.isEmpty());store.audit(account,after.id(),before.version(),after.version(),context.requestId(),after.updatedAt());
        byte[] hash=fingerprint(fingerprints.activeKey(),command,after.id());
        try{store.receipt(account,after.id(),command.key(),new CustomerProfileStore.Receipt(fingerprints.activeKey(),hash,before.version(),after.version()),after.updatedAt());}finally{Arrays.fill(hash,(byte)0);}
        admission.requireCurrent(context);admission.meaningfulAction(context);return view(after);
    }
}
