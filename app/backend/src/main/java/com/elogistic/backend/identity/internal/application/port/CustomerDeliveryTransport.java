package com.elogistic.backend.identity.internal.application.port;

import com.elogistic.backend.identity.internal.domain.*;
import java.util.*;

/** Actual transport result only. SMTP acceptance is not recipient delivery. */
public interface CustomerDeliveryTransport {
    Outcome send(Message message);
    enum Kind {ACCEPTED_BY_PROVIDER,TRANSIENT_FAILURE,PERMANENT_FAILURE,BLOCKED}
    record Outcome(Kind kind,String reference,boolean sandbox){
        public Outcome{if(kind==null || (kind==Kind.ACCEPTED_BY_PROVIDER)!=(reference!=null) || (reference!=null && !reference.matches("(?:TEST|SMTP):[0-9a-f-]{36}")))throw new IllegalArgumentException("CUSTOMER_DELIVERY_OUTCOME_INVALID");}
        @Override public String toString(){return "CustomerDeliveryOutcome[redacted]";}
    }
    final class Message implements AutoCloseable {
        private final UUID id;private final CustomerProof proof;private final String destination;private final byte[] token;private boolean closed;
        public Message(UUID id,CustomerProof proof,String destination,byte[] token){
            this.id=Objects.requireNonNull(id);this.proof=Objects.requireNonNull(proof);this.destination=new CustomerEmail(destination).value();
            if(!proof.purpose().possessionRequired() || token==null || token.length!=43)throw new IllegalArgumentException("CUSTOMER_DELIVERY_MESSAGE_INVALID");this.token=token.clone();
        }
        private void check(){if(closed)throw new IllegalStateException("CUSTOMER_DELIVERY_BUFFER_CLOSED");}
        public UUID id(){check();return id;}public CustomerProof proof(){check();return proof;}public String destination(){check();return destination;}public byte[] token(){check();return token.clone();}
        @Override public void close(){Arrays.fill(token,(byte)0);closed=true;}
        @Override public String toString(){return "CustomerDeliveryMessage[redacted]";}
    }
}
