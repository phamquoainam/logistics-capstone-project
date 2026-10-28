package com.elogistic.backend.identity.internal.application.port;

import com.elogistic.backend.identity.internal.domain.*;
import java.time.Instant;
import java.util.*;

/** Each method owns and finishes a short physical transaction; it never invokes transport I/O. */
public interface CustomerDeliveryStore {
    int expire(Instant now);
    Optional<Lease> claim(Instant now);
    boolean finish(Lease lease,CustomerDeliveryTransport.Outcome outcome,Instant now);
    final class Lease implements AutoCloseable {
        private final CustomerDelivery delivery;private final CustomerProof proof;private final String verifierKey,encryptionKey,destination;
        private final byte[] nonce,ciphertext,recipientNonce,recipientCiphertext;private boolean closed;
        public Lease(CustomerDelivery delivery,CustomerProof proof,String verifierKey,String encryptionKey,String destination,byte[] nonce,byte[] ciphertext,byte[] recipientNonce,byte[] recipientCiphertext){
            this.delivery=Objects.requireNonNull(delivery);this.proof=Objects.requireNonNull(proof);this.verifierKey=verifierKey;this.encryptionKey=encryptionKey;this.destination=destination;
            this.nonce=nonce.clone();this.ciphertext=ciphertext.clone();this.recipientNonce=recipientNonce.clone();this.recipientCiphertext=recipientCiphertext.clone();
        }
        private void check(){if(closed)throw new IllegalStateException("CUSTOMER_DELIVERY_LEASE_CLOSED");}
        public CustomerDelivery delivery(){check();return delivery;}public CustomerProof proof(){check();return proof;}public String verifierKey(){check();return verifierKey;}public String encryptionKey(){check();return encryptionKey;}public String destination(){check();return destination;}
        public byte[] nonce(){check();return nonce.clone();}public byte[] ciphertext(){check();return ciphertext.clone();}public byte[] recipientNonce(){check();return recipientNonce.clone();}public byte[] recipientCiphertext(){check();return recipientCiphertext.clone();}
        @Override public void close(){Arrays.fill(nonce,(byte)0);Arrays.fill(ciphertext,(byte)0);Arrays.fill(recipientNonce,(byte)0);Arrays.fill(recipientCiphertext,(byte)0);closed=true;}
        @Override public String toString(){return "CustomerDeliveryLease[redacted]";}
    }
}
