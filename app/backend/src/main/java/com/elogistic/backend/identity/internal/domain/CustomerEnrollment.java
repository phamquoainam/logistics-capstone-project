package com.elogistic.backend.identity.internal.domain;

import java.time.Instant;
import java.util.UUID;

/** Pending enrollment is not a V3 pending account and grants no authority. */
public record CustomerEnrollment(UUID id,UUID intendedAccountId,CustomerEmail email,String displayName,String unverifiedPhone,
                                 String protectedPassword,String consentVersion,Instant createdAt,Instant expiresAt,
                                 Status status,long version) {
    public enum Status {PENDING,CONSUMED,EXPIRED,REVOKED}
    public CustomerEnrollment(UUID id,UUID intendedAccountId,CustomerEmail email,String displayName,String protectedPassword,
                              String consentVersion,Instant createdAt,Instant expiresAt,Status status,long version){
        this(id,intendedAccountId,email,displayName,null,protectedPassword,consentVersion,createdAt,expiresAt,status,version);
    }
    public CustomerEnrollment {
        if(id==null || intendedAccountId==null || email==null || displayName==null || displayName.isBlank()
            || displayName.codePointCount(0,displayName.length())>160 || consentVersion==null || consentVersion.isBlank()
            || (unverifiedPhone!=null && !unverifiedPhone.matches("\\+[1-9][0-9]{7,14}"))
            || consentVersion.codePointCount(0,consentVersion.length())>64 || createdAt==null || expiresAt==null
            || !expiresAt.isAfter(createdAt) || status==null || version<1
            || (status==Status.PENDING?(protectedPassword==null || !protectedPassword.matches("\\$2[aby]\\$12\\$[./A-Za-z0-9]{53}")):protectedPassword!=null))
            throw new DomainViolation(DomainViolation.Reason.INVALID_STATE);
    }
    public void requirePending(Instant now){if(status!=Status.PENDING || now==null || now.isBefore(createdAt) || !now.isBefore(expiresAt))throw CustomerProof.rejected();}
    public CustomerEnrollment consume(Instant now){requirePending(now);return new CustomerEnrollment(id,intendedAccountId,email,displayName,unverifiedPhone,null,consentVersion,createdAt,expiresAt,Status.CONSUMED,next());}
    public CustomerEnrollment resend(Instant now){requirePending(now);return new CustomerEnrollment(id,intendedAccountId,email,displayName,unverifiedPhone,protectedPassword,consentVersion,createdAt,now.plusSeconds(86400),Status.PENDING,next());}
    public CustomerEnrollment expire(Instant now){if(status!=Status.PENDING || now==null || now.isBefore(expiresAt))throw CustomerProof.rejected();return new CustomerEnrollment(id,intendedAccountId,email,displayName,unverifiedPhone,null,consentVersion,createdAt,expiresAt,Status.EXPIRED,next());}
    private long next(){if(version==Long.MAX_VALUE)throw new DomainViolation(DomainViolation.Reason.VERSION_EXHAUSTED);return version+1;}
    @Override public String toString(){return "CustomerEnrollment[redacted]";}
}
