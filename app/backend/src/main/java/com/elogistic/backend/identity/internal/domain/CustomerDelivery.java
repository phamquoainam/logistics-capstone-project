package com.elogistic.backend.identity.internal.domain;

import java.time.Instant;
import java.util.*;

/** Bounded delivery lease/retry lifecycle. Provider acceptance never implies recipient delivery. */
public record CustomerDelivery(UUID id,UUID proofId,Status status,int attempts,Instant createdAt,Instant expiresAt,
                               Instant dueAt,UUID leaseId,Instant leaseUntil,Instant finishedAt,long version) {
    public enum Status {QUEUED,SENDING,RETRY_WAIT,ACCEPTED_BY_PROVIDER,DELIVERED,FAILED,EXPIRED,BLOCKED}
    public enum Outcome {ACCEPTED_BY_PROVIDER,TRANSIENT_FAILURE,PERMANENT_FAILURE,BLOCKED}
    public CustomerDelivery{
        if(id==null || proofId==null || status==null || attempts<0 || attempts>5 || createdAt==null || expiresAt==null || !expiresAt.isAfter(createdAt)
            || dueAt==null || dueAt.isBefore(createdAt) || dueAt.isAfter(expiresAt) || version<1
            || (status==Status.SENDING?(leaseId==null || leaseUntil==null || !leaseUntil.isAfter(createdAt) || leaseUntil.isAfter(expiresAt) || attempts<1):(leaseId!=null || leaseUntil!=null))
            || (terminal(status)?(finishedAt==null || finishedAt.isBefore(createdAt)):finishedAt!=null))throw invalid();
    }
    private static boolean terminal(Status status){return Set.of(Status.ACCEPTED_BY_PROVIDER,Status.DELIVERED,Status.FAILED,Status.EXPIRED).contains(status);}
    public boolean claimable(Instant now){return now!=null && !now.isBefore(createdAt) && now.isBefore(expiresAt) && !now.isBefore(dueAt)
        && attempts<5 && !terminal(status) && (status!=Status.SENDING || !now.isBefore(leaseUntil));}
    public CustomerDelivery claim(UUID lease,Instant now){
        if(lease==null || !claimable(now))throw invalid();return new CustomerDelivery(id,proofId,Status.SENDING,attempts+1,createdAt,expiresAt,dueAt,lease,min(now.plusSeconds(30),expiresAt),null,next());
    }
    public boolean owns(UUID lease,Instant now){return lease!=null && lease.equals(leaseId) && status==Status.SENDING && now!=null && !now.isBefore(createdAt) && now.isBefore(expiresAt) && now.isBefore(leaseUntil);}
    public CustomerDelivery finish(UUID lease,Outcome outcome,Instant now){
        if(!owns(lease,now) || outcome==null)throw invalid();
        if(outcome==Outcome.ACCEPTED_BY_PROVIDER)return terminal(Status.ACCEPTED_BY_PROVIDER,now);
        if(outcome==Outcome.PERMANENT_FAILURE || attempts>=5)return terminal(Status.FAILED,now);
        Instant due=now.plusSeconds(outcome==Outcome.BLOCKED?60:30L<<(attempts-1));
        if(!due.isBefore(expiresAt))return terminal(Status.EXPIRED,now);
        return new CustomerDelivery(id,proofId,outcome==Outcome.BLOCKED?Status.BLOCKED:Status.RETRY_WAIT,attempts,createdAt,expiresAt,due,null,null,null,next());
    }
    public CustomerDelivery invalidate(Instant now){if(now==null || now.isBefore(createdAt) || terminal(status))throw invalid();return terminal(Status.EXPIRED,now);}
    public CustomerDelivery exhaust(Instant now){if(attempts!=5 || terminal(status) || now==null || now.isBefore(createdAt) || (status==Status.SENDING && now.isBefore(leaseUntil)))throw invalid();return terminal(Status.FAILED,now);}
    private CustomerDelivery terminal(Status next,Instant now){return new CustomerDelivery(id,proofId,next,attempts,createdAt,expiresAt,dueAt,null,null,now,next());}
    private long next(){if(version==Long.MAX_VALUE)throw invalid();return version+1;}
    private static Instant min(Instant a,Instant b){return a.isBefore(b)?a:b;}
    private static DomainViolation invalid(){return new DomainViolation(DomainViolation.Reason.INVALID_STATE);}
    @Override public String toString(){return "CustomerDelivery[redacted]";}
}
