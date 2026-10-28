package com.elogistic.backend.merchant.internal.infrastructure;

import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import static org.junit.jupiter.api.Assertions.*;
class MerchantFingerprintTests {
    private static byte[] key(int value){byte[] k=new byte[32];Arrays.fill(k,(byte)value);return k;}
    @Test void exactFieldsNullAndOperationVersionTargetAreCryptographicallyBound(){
        var crypto=new ProtectedMerchantFingerprints(Map.of("k1",key(51)),"k1");UUID target=UUID.randomUUID();byte[] original=crypto.hash("k1","updateCustomerProfile",target,1,"a","bc",null);
        for(byte[] other:List.of(crypto.hash("k1","updateCustomerProfile",target,1,"ab","c",null),crypto.hash("k1","updateCustomerProfile",target,1,"a","bc",""),crypto.hash("k1","updateCustomerProfile",target,2,"a","bc",null),crypto.hash("k1","otherOperation",target,1,"a","bc",null),crypto.hash("k1","updateCustomerProfile",UUID.randomUUID(),1,"a","bc",null)))assertFalse(Arrays.equals(original,other));
    }
    @Test void retainedKeyRotationPreservesExactRetryAndClonesCallerMaterial(){
        byte[] old=key(51);var before=new ProtectedMerchantFingerprints(Map.of("k1",old),"k1");var rotated=new ProtectedMerchantFingerprints(Map.of("k1",old,"k2",key(52)),"k2");Arrays.fill(old,(byte)0);UUID target=UUID.randomUUID();assertArrayEquals(before.hash("k1","updateCustomerProfile",target,1,"exact"),rotated.hash("k1","updateCustomerProfile",target,1,"exact"));assertEquals("k2",rotated.activeKey());assertFalse(rotated.toString().contains("exact"));
    }
    @Test void missingWeakZeroAndUnknownReceiptKeysFailClosed(){
        assertThrows(IllegalArgumentException.class,()->new ProtectedMerchantFingerprints(Map.of(),"k1"));assertThrows(IllegalArgumentException.class,()->new ProtectedMerchantFingerprints(Map.of("k1",new byte[32]),"k1"));assertThrows(IllegalArgumentException.class,()->new ProtectedMerchantFingerprints(Map.of("k1",new byte[31]),"k1"));var crypto=new ProtectedMerchantFingerprints(Map.of("k1",key(51)),"k1");assertThrows(IllegalArgumentException.class,()->crypto.hash("missing","updateCustomerProfile",UUID.randomUUID(),1,"exact"));
    }
    @Test void profileStoreCannotOpenAnIndependentOrUnboundBusinessTransaction(){
        var source=new DriverManagerDataSource();var store=new JdbcCustomerProfileStore(source);assertThrows(IllegalStateException.class,store::lockControl);assertThrows(IllegalStateException.class,()->store.find(UUID.randomUUID(),false));assertEquals("CUSTOMER_PROFILE_POSTGRES18_SCHEMA_V11_REQUIRED",assertThrows(IllegalStateException.class,()->JdbcCustomerProfileStore.requireSchema(source)).getMessage());
    }
}
