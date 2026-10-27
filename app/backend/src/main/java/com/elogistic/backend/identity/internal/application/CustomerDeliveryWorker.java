package com.elogistic.backend.identity.internal.application;

import com.elogistic.backend.identity.internal.application.port.*;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;

/** Decrypt/send only after the lease transaction commits. Stale workers cannot finalize a newer lease. */
public final class CustomerDeliveryWorker {
    private final CustomerDeliveryStore store;private final CustomerProofProtection protection;private final CustomerDeliveryTransport transport;private final Clock clock;
    public CustomerDeliveryWorker(CustomerDeliveryStore store,CustomerProofProtection protection,CustomerDeliveryTransport transport,Clock clock){
        this.store=Objects.requireNonNull(store);this.protection=Objects.requireNonNull(protection);this.transport=Objects.requireNonNull(transport);this.clock=Objects.requireNonNull(clock);
    }
    private Instant now(){return clock.instant().truncatedTo(ChronoUnit.MICROS);}
    public boolean runOne(){
        store.expire(now());var candidate=store.claim(now());if(candidate.isEmpty())return false;
        try(var lease=candidate.get()){
            CustomerDeliveryTransport.Outcome outcome;
            byte[] nonce=lease.nonce(),ciphertext=lease.ciphertext(),recipientNonce=lease.recipientNonce(),recipientCiphertext=lease.recipientCiphertext();
            try{
                String recipient=protection.openDestination(lease.proof(),lease.verifierKey(),lease.encryptionKey(),recipientNonce,recipientCiphertext,now());
                if(!recipient.equals(lease.destination()))throw new IllegalStateException("CUSTOMER_DELIVERY_RECIPIENT_REJECTED");
                try(var opened=protection.open(lease.proof(),recipient,lease.verifierKey(),lease.encryptionKey(),nonce,ciphertext,now())){
                    byte[] token=opened.token();try(var message=new CustomerDeliveryTransport.Message(lease.delivery().id(),lease.proof(),recipient,token)){
                        outcome=Objects.requireNonNull(transport.send(message));
                    }finally{Arrays.fill(token,(byte)0);}
                }
            }catch(RuntimeException unavailable){outcome=new CustomerDeliveryTransport.Outcome(CustomerDeliveryTransport.Kind.BLOCKED,null,true);}
            finally{Arrays.fill(nonce,(byte)0);Arrays.fill(ciphertext,(byte)0);Arrays.fill(recipientNonce,(byte)0);Arrays.fill(recipientCiphertext,(byte)0);}
            store.finish(lease,outcome,now());return true;
        }
    }
}
