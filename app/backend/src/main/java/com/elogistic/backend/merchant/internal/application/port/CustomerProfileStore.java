package com.elogistic.backend.merchant.internal.application.port;

import com.elogistic.backend.merchant.internal.domain.CustomerBusinessProfile;
import java.time.Instant;
import java.util.*;

/** Joins the exact Identity-owned physical transaction; never opens/commits its own transaction. */
public interface CustomerProfileStore {
    record Receipt(String keyId,byte[] fingerprint,long originalVersion,long resultVersion){
        public Receipt{fingerprint=fingerprint.clone();}@Override public byte[] fingerprint(){return fingerprint.clone();}
        @Override public String toString(){return "MerchantReceipt[redacted]";}
    }
    void lockControl();
    Optional<CustomerBusinessProfile> find(UUID account,boolean lock);
    Optional<Receipt> receipt(UUID account,UUID target,UUID key);
    CustomerBusinessProfile version(UUID account,UUID profile,long version);
    void save(CustomerBusinessProfile before,CustomerBusinessProfile after,boolean insert);
    void audit(UUID account,UUID target,long before,long after,UUID request,Instant now);
    void receipt(UUID account,UUID target,UUID key,Receipt receipt,Instant now);
}
