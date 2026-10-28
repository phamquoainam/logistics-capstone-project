package com.elogistic.backend.identity.internal.application;

import com.elogistic.backend.identity.api.IdentityCommandFailure;
import com.elogistic.backend.identity.internal.application.port.*;
import java.security.MessageDigest;
import java.util.*;

/** Keyed exact-input binding shared by preauth requests and possession-scoped confirmations. */
final class CustomerReceipts {
    private CustomerReceipts(){}
    static CustomerEnrollmentStore.Receipt match(CustomerEnrollmentStore.Receipt previous,CustomerProofProtection protection,String operation,String... fields){
        if(previous==null)return null;byte[] fingerprint=protection.fingerprint(previous.keyId(),operation,fields),stored=previous.fingerprint();
        try{if(!previous.operation().equals(operation) || !MessageDigest.isEqual(fingerprint,stored))throw new IdentityCommandFailure(IdentityCommandFailure.Code.IDEMPOTENCY_CONFLICT);return previous;}
        finally{Arrays.fill(fingerprint,(byte)0);Arrays.fill(stored,(byte)0);}
    }
    static CustomerEnrollmentStore.Receipt prepare(CustomerProofProtection protection,UUID request,String operation,String... fields){
        byte[] fingerprint=protection.fingerprint(protection.activeVerifierKey(),operation,fields);
        try{return new CustomerEnrollmentStore.Receipt(operation,protection.activeVerifierKey(),fingerprint,request);}finally{Arrays.fill(fingerprint,(byte)0);}
    }
}
