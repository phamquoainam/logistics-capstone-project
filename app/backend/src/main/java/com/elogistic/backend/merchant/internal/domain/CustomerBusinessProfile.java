package com.elogistic.backend.merchant.internal.domain;

import java.time.Instant;
import java.util.*;

/** Business contact values are untrusted contact metadata, never authentication/verification authority. */
public record CustomerBusinessProfile(UUID id,UUID accountId,String displayName,String preferredLanguage,String businessContactEmail,
                                      String businessContactPhone,long version,Instant createdAt,Instant updatedAt){
    public CustomerBusinessProfile{
        if(id==null || accountId==null || displayName==null || displayName.codePoints().allMatch(c->Character.isWhitespace(c)||Character.isSpaceChar(c))
            || displayName.codePointCount(0,displayName.length())>160 || displayName.codePoints().anyMatch(Character::isISOControl)
            || !Set.of("vi","en").contains(preferredLanguage==null?"":preferredLanguage) || version<1 || createdAt==null || updatedAt==null || updatedAt.isBefore(createdAt))throw invalid();
        if(businessContactEmail!=null && (businessContactEmail.length()>254 || businessContactEmail.isEmpty()
            || !businessContactEmail.matches("[A-Za-z0-9.!#$%&'*+/=?^_`{|}~-]+@[A-Za-z0-9](?:[A-Za-z0-9-]*[A-Za-z0-9])?(?:\\.[A-Za-z0-9](?:[A-Za-z0-9-]*[A-Za-z0-9])?)*")
            || businessContactEmail.startsWith(".") || businessContactEmail.substring(0,businessContactEmail.indexOf('@')).endsWith(".") || businessContactEmail.substring(0,businessContactEmail.indexOf('@')).contains("..")))throw invalid();
        if(businessContactPhone!=null && (businessContactPhone.isBlank() || businessContactPhone.codePointCount(0,businessContactPhone.length())>32 || businessContactPhone.codePoints().anyMatch(Character::isISOControl)))throw invalid();
    }
    public CustomerBusinessProfile replace(String display,String language,String email,String phone,long expected,Instant now){
        if(expected!=version || version==Long.MAX_VALUE || now==null || now.isBefore(updatedAt))throw invalid();return new CustomerBusinessProfile(id,accountId,display,language,email,phone,version+1,createdAt,now);
    }
    private static IllegalArgumentException invalid(){return new IllegalArgumentException("CUSTOMER_PROFILE_INVALID");}
    @Override public String toString(){return "CustomerBusinessProfile[redacted]";}
}
