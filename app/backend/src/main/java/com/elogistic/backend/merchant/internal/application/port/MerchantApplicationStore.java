package com.elogistic.backend.merchant.internal.application.port;

import com.elogistic.backend.merchant.api.customer.MerchantApplications.*;
import com.elogistic.backend.merchant.internal.domain.MerchantApplicationDraft;
import java.time.Instant;
import java.util.*;

public interface MerchantApplicationStore {
    void lockControl();
    void lockKeys(Collection<String> keys);
    Optional<MerchantApplicationDraft> find(UUID actor,UUID target,boolean lock);
    List<MerchantApplicationDraft> list(UUID actor,Query query);
    Optional<Receipt> receipt(UUID actor,String operation,UUID key);
    MerchantApplicationDraft version(UUID actor,UUID target,long version);
    void save(MerchantApplicationDraft before,MerchantApplicationDraft after);
    void audit(UUID actor,String operation,UUID target,long before,long after,UUID request,Instant now);
    void receipt(UUID actor,String operation,UUID key,Receipt receipt,Instant now);
    String canonical(Input input);
    record Receipt(UUID target,String keyId,byte[] fingerprint,long originalVersion,long resultVersion) {
        public Receipt{fingerprint=fingerprint.clone();}
        @Override public byte[] fingerprint(){return fingerprint.clone();}
        @Override public String toString(){return "ApplicationReceipt[redacted]";}
    }
}
