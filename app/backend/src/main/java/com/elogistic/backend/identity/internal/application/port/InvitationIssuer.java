package com.elogistic.backend.identity.internal.application.port;

import com.elogistic.backend.identity.internal.domain.ActionProof;
import com.elogistic.backend.identity.internal.domain.IdentityAccount;
import java.time.Instant;

/** Trusted infrastructure boundary, NOT an email sender or a client-provided proof.
 * A reviewed adapter must generate secure opaque material and append a durable protected
 * delivery intent using the SAME runtime DataSource/transaction. No external I/O in either
 * method. Raw secrets never leave the adapter, appear in toString, or enter audit/receipts.
 * Absence must stop all provisioning writes. Production keys are externally supplied. */
public interface InvitationIssuer {
    Prepared prepare(IdentityAccount pending, String destination, Instant now);

    interface Prepared extends AutoCloseable {
        ActionProof proof();
        byte[] tokenHash();
        /** NULL only for isolated legacy/synthetic adapters, never the secure V5 issuer. */
        default String verifierKeyId() {return null;}
        void appendDeliveryIntent();
        /** Destroy transient secret material before transaction completion, including failure. */
        @Override void close();
    }
}
