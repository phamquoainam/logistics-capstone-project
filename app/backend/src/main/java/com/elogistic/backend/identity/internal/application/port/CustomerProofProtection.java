package com.elogistic.backend.identity.internal.application.port;

import com.elogistic.backend.identity.internal.domain.CustomerProof;
import java.time.Instant;
import java.util.Arrays;

/** Explicit customer keys; ciphertext/verifier only crosses persistence. No raw delivery proof result. */
public interface CustomerProofProtection {
    String activeVerifierKey();
    String activeEncryptionKey();
    Protected issue(CustomerProof proof,String destination);
    boolean matches(CustomerProof proof,String keyId,byte[] supplied,byte[] verifier,Instant now);
    byte[] fingerprint(String retainedKeyId,String operation,String... exactFields);
    String hashPassword(String password);
    /** Only a current owned delivery lease may call this; transport I/O is outside the DB transaction. */
    Opened open(CustomerProof proof,String destination,String verifierKeyId,String encryptionKeyId,byte[] nonce,byte[] ciphertext,Instant now);
    String openDestination(CustomerProof proof,String verifierKeyId,String encryptionKeyId,byte[] nonce,byte[] ciphertext,Instant now);

    final class Protected implements AutoCloseable {
        private final String verifierKey,encryptionKey;
        private final byte[] verifier,nonce,ciphertext,recipientNonce,recipientCiphertext;
        private boolean closed;
        public Protected(String verifierKey,String encryptionKey,byte[] verifier,byte[] nonce,byte[] ciphertext,byte[] recipientNonce,byte[] recipientCiphertext) {
            this.verifierKey=verifierKey;this.encryptionKey=encryptionKey;this.verifier=verifier.clone();this.nonce=nonce.clone();this.ciphertext=ciphertext.clone();
            this.recipientNonce=recipientNonce.clone();this.recipientCiphertext=recipientCiphertext.clone();
        }
        private void check(){if(closed)throw new IllegalStateException("CUSTOMER_PROOF_BUFFER_CLOSED");}
        public String verifierKey(){return verifierKey;}public String encryptionKey(){return encryptionKey;}
        public byte[] verifier(){check();return verifier.clone();}public byte[] nonce(){check();return nonce.clone();}public byte[] ciphertext(){check();return ciphertext.clone();}
        public byte[] recipientNonce(){check();return recipientNonce.clone();}public byte[] recipientCiphertext(){check();return recipientCiphertext.clone();}
        @Override public void close(){Arrays.fill(verifier,(byte)0);Arrays.fill(nonce,(byte)0);Arrays.fill(ciphertext,(byte)0);Arrays.fill(recipientNonce,(byte)0);Arrays.fill(recipientCiphertext,(byte)0);closed=true;}
        @Override public String toString(){return "ProtectedCustomerProof[redacted]";}
    }
    final class Opened implements AutoCloseable {
        private final byte[] token;private boolean closed;
        public Opened(byte[] token){this.token=token.clone();}
        public byte[] token(){if(closed)throw new IllegalStateException("CUSTOMER_PROOF_BUFFER_CLOSED");return token.clone();}
        @Override public void close(){Arrays.fill(token,(byte)0);closed=true;}
        @Override public String toString(){return "OpenedCustomerProof[redacted]";}
    }
}
