package com.elogistic.backend.identity.internal.application;

import com.elogistic.backend.identity.internal.domain.*;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CustomerDeliveryTests {
    private static final Instant NOW=Instant.parse("2026-10-27T00:00:00Z");
    private CustomerDelivery queued(long seconds){return new CustomerDelivery(UUID.randomUUID(),UUID.randomUUID(),CustomerDelivery.Status.QUEUED,0,NOW,NOW.plusSeconds(seconds),NOW,null,null,null,1);}
    @Test void leaseIsBoundedAndCannotBeClaimedTwiceWhileLive(){
        var queued=queued(86400);var leased=queued.claim(UUID.randomUUID(),NOW);assertEquals(1,leased.attempts());assertEquals(NOW.plusSeconds(30),leased.leaseUntil());assertEquals(queued.id(),leased.id());
        assertFalse(leased.claimable(NOW.plusSeconds(29)));assertThrows(DomainViolation.class,()->leased.claim(UUID.randomUUID(),NOW.plusSeconds(29)));
    }
    @Test void exactLeaseDeadlineRejectsOldWorkerAndAllowsFencedReclaim(){
        var first=queued(86400).claim(UUID.randomUUID(),NOW);assertFalse(first.owns(first.leaseId(),first.leaseUntil()));
        var second=first.claim(UUID.randomUUID(),first.leaseUntil());assertNotEquals(first.leaseId(),second.leaseId());assertEquals(2,second.attempts());
        assertThrows(DomainViolation.class,()->second.finish(first.leaseId(),CustomerDelivery.Outcome.ACCEPTED_BY_PROVIDER,NOW.plusSeconds(31)));
    }
    @Test void leaseNeverOutlivesProofAndExactProofExpiryDeniesCompletion(){
        var lease=queued(10).claim(UUID.randomUUID(),NOW);assertEquals(lease.expiresAt(),lease.leaseUntil());
        assertThrows(DomainViolation.class,()->lease.finish(lease.leaseId(),CustomerDelivery.Outcome.ACCEPTED_BY_PROVIDER,lease.expiresAt()));assertFalse(lease.claimable(lease.expiresAt()));
    }
    @Test void providerAcceptanceIsTerminalWithoutBecomingDelivered(){
        var lease=queued(86400).claim(UUID.randomUUID(),NOW);var accepted=lease.finish(lease.leaseId(),CustomerDelivery.Outcome.ACCEPTED_BY_PROVIDER,NOW.plusSeconds(1));
        assertEquals(CustomerDelivery.Status.ACCEPTED_BY_PROVIDER,accepted.status());assertNull(accepted.leaseId());assertNotNull(accepted.finishedAt());assertFalse(accepted.claimable(NOW.plusSeconds(30)));
    }
    @Test void transientRetryBackoffStopsAtFiveAttempts(){
        var current=queued(86400);Instant time=NOW;
        for(int attempt=1;attempt<=5;attempt++){
            var lease=current.claim(UUID.randomUUID(),time);current=lease.finish(lease.leaseId(),CustomerDelivery.Outcome.TRANSIENT_FAILURE,time.plusSeconds(1));assertEquals(attempt,current.attempts());
            if(attempt<5){assertEquals(CustomerDelivery.Status.RETRY_WAIT,current.status());assertEquals(time.plusSeconds(1+(30L<<(attempt-1))),current.dueAt());time=current.dueAt();}
        }
        assertEquals(CustomerDelivery.Status.FAILED,current.status());assertFalse(current.claimable(time.plusSeconds(1000)));
    }
    @Test void retryCannotCrossProofExpiryOrBecomeAnUnboundedLease(){
        var lease=queued(20).claim(UUID.randomUUID(),NOW);assertEquals(CustomerDelivery.Status.EXPIRED,lease.finish(lease.leaseId(),CustomerDelivery.Outcome.TRANSIENT_FAILURE,NOW.plusSeconds(1)).status());
        assertEquals(CustomerDelivery.Status.EXPIRED,lease.finish(lease.leaseId(),CustomerDelivery.Outcome.BLOCKED,NOW.plusSeconds(1)).status());
    }
    @Test void permanentFailureAndKeyOutageRemainDifferentAuditableStates(){
        var lease=queued(86400).claim(UUID.randomUUID(),NOW);
        assertEquals(CustomerDelivery.Status.FAILED,lease.finish(lease.leaseId(),CustomerDelivery.Outcome.PERMANENT_FAILURE,NOW.plusSeconds(1)).status());
        var blocked=lease.finish(lease.leaseId(),CustomerDelivery.Outcome.BLOCKED,NOW.plusSeconds(1));assertEquals(CustomerDelivery.Status.BLOCKED,blocked.status());assertEquals(NOW.plusSeconds(61),blocked.dueAt());
    }
    @Test void fifthCrashedAttemptCannotBeReclaimedForever(){
        var current=queued(86400);Instant time=NOW;
        for(int i=0;i<5;i++){current=current.claim(UUID.randomUUID(),time);time=current.leaseUntil();}
        assertFalse(current.claimable(time));assertEquals(CustomerDelivery.Status.FAILED,current.exhaust(time).status());
    }
}
