package com.elogistic.backend.identity.internal.application;

import com.elogistic.backend.identity.api.*;
import com.elogistic.backend.identity.internal.application.port.*;
import com.elogistic.backend.identity.internal.domain.*;
import java.security.MessageDigest;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import static com.elogistic.backend.identity.api.IdentityCommandFailure.Code.*;

/** Accepted acceptIdentityInvitation application semantics only; no HTTP/session/bootstrap side effects. */
public final class AcceptIdentityInvitation implements IdentityInvitationCommands {
    private final IdentityInvitationStore store;
    private final InvitationCredentials credentials;
    private final Clock clock;
    private final Duration retention;
    public AcceptIdentityInvitation(IdentityInvitationStore store,InvitationCredentials credentials,Clock clock,Duration retention) {
        this.store=Objects.requireNonNull(store);this.credentials=Objects.requireNonNull(credentials);this.clock=Objects.requireNonNull(clock);
        if(retention==null || retention.compareTo(Duration.ofHours(24))<0)throw new IllegalArgumentException("INVITATION_RECEIPT_RETENTION_INVALID");
        this.retention=retention;
    }
    @Override public void acceptIdentityInvitation(Command command) {
        if(command==null || command.proofId()==null || command.operationKey()==null || command.requestId()==null)throw fail(VALIDATION_FAILED);
        try {PasswordPolicy.validateNew(command.password());}
        catch(DomainViolation invalid){throw fail(VALIDATION_FAILED);}
        byte[] token=command.token();
        try {
            store.transaction(tx->{
                var admission=tx.admission();admission.lockAdministration();
                var stored=tx.lockProof(command.proofId()).orElseThrow(()->fail(TOKEN_INVALID_OR_EXPIRED));
                var proof=stored.proof();
                var account=admission.lockAccount(proof.accountId()).orElseThrow(()->fail(TOKEN_INVALID_OR_EXPIRED));
                Instant now=clock.instant().truncatedTo(ChronoUnit.MICROS);
                // Locator, existing outbox, consumed status or receipt alone can NEVER authenticate a request.
                if(!credentials.matches(stored,token,now))throw fail(TOKEN_INVALID_OR_EXPIRED);
                if(!tx.eligibleRealm(account.id(),proof.id()))throw fail(INVALID_ACCOUNT_STATE);
                byte[] fingerprint=credentials.fingerprint(stored,token,command.password());
                try {
                    var receipt=tx.receipt(proof.id(),command.operationKey());
                    if(receipt.isPresent()) {
                        var r=receipt.get();
                        byte[] retained=r.fingerprint();
                        try {
                            if(!r.targetId().equals(account.id()) || !MessageDigest.isEqual(fingerprint,retained))throw fail(IDEMPOTENCY_CONFLICT);
                        } finally {Arrays.fill(retained,(byte)0);}
                        // An exact receipt replay is a no-op, not a second activation or renewed proof.
                        if(!now.isBefore(r.expiresAt()) || proof.state()!=ActionProof.State.CONSUMED || !account.active()
                            || account.activation()!=IdentityAccount.Activation.VERIFIED || account.version().version()!=r.version())throw fail(INVALID_ACCOUNT_STATE);
                        return null;
                    }
                    if(proof.state()!=ActionProof.State.AVAILABLE)throw fail(TOKEN_INVALID_OR_EXPIRED);
                    if(account.status()!=IdentityAccount.Status.PENDING_ACTIVATION || account.activation()!=IdentityAccount.Activation.UNVERIFIED)throw fail(INVALID_ACCOUNT_STATE);
                    IdentityAccount.ProofChange change;
                    try {change=account.activate(proof,command.password(),account.version().version(),now);}
                    catch(DomainViolation invalid){throw fail(INVALID_ACCOUNT_STATE);}
                    String verifier=credentials.hashPassword(command.password());
                    tx.activate(account,change,verifier,now);
                    admission.revokeSessions(account.id(),now);
                    tx.clearDelivery(proof.id(),now);
                    tx.appendAudit(account.id(),command.requestId(),now);
                    tx.appendReceipt(proof.id(),command.operationKey(),new IdentityInvitationStore.Receipt(account.id(),change.account().version().version(),fingerprint,now.plus(retention)),account.version().version(),now);
                    return null;
                } finally {Arrays.fill(fingerprint,(byte)0);}
            });
        } finally {if(token!=null)Arrays.fill(token,(byte)0);}
    }
    private static IdentityCommandFailure fail(IdentityCommandFailure.Code code){return new IdentityCommandFailure(code);}
}
