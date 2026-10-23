package com.elogistic.backend.identity.internal.application;

import com.elogistic.backend.identity.api.*;
import com.elogistic.backend.identity.api.IdentityGrantCommands.Result;
import com.elogistic.backend.identity.internal.application.port.*;
import com.elogistic.backend.identity.internal.domain.*;
import java.time.*;
import java.util.*;
import static com.elogistic.backend.identity.api.IdentityCommandFailure.Code.*;

/** Invitation provisioning only; no session issue, password setup, HTTP or privileged bootstrap. */
public final class ProvisionIdentityAccount implements IdentityProvisionCommands {
    private final IdentityProvisionStore store;
    private final ProvisionAuthority authority;
    private final InvitationIssuer invitations;
    private final Clock clock;
    private final Duration retention;
    public ProvisionIdentityAccount(IdentityProvisionStore store,ProvisionAuthority authority,
                                    InvitationIssuer invitations,Clock clock,Duration retention) {
        this.store=Objects.requireNonNull(store);this.authority=authority;this.invitations=invitations;
        this.clock=Objects.requireNonNull(clock);
        if(retention==null || retention.compareTo(Duration.ofHours(24))<0)
            throw new IllegalArgumentException("Receipt retention must cover invitation lifetime");
        this.retention=retention;
    }
    @Override public Result provisionIdentityAccount(Command command) {
        if(command==null || command.sessionId()==null) throw failure(SESSION_INVALID);
        return store.transaction(tx->{
            var admission=tx.admission();admission.lockAdministration();
            UUID actorId=admission.sessionAccount(command.sessionId()).orElseThrow(()->failure(SESSION_INVALID));
            var actor=admission.lockAccount(actorId).orElseThrow(()->failure(SESSION_INVALID));
            var session=admission.lockSession(command.sessionId()).orElseThrow(()->failure(SESSION_INVALID));
            admit(session,actor);
            if(!actor.managementAdmin()) throw failure(AUTHORITY_REQUIRED);
            var input=ProvisionInput.from(command);
            // Internal lookup only; neither receipt existence nor contents are returned before authorization.
            var receipt=tx.receipt(actor.id(),command.operationKey());
            var target=receipt.isPresent() ? admission.lockAccount(receipt.get().targetId()).orElseThrow(()->failure(AUTHORITY_REQUIRED))
                : new IdentityAccount(UUID.randomUUID(),input.username(),IdentityAccount.Status.PENDING_ACTIVATION,
                    IdentityAccount.Activation.UNVERIFIED,new AccountVersion(1,1),Set.of());
            ManagementAuthority scope=null;
            if(authority!=null) {
                try {scope=receipt.isPresent() ? authority.forReplay(actor,target,input.grants()) : authority.forNewAccount(actor,target,input.grants());}
                catch(RuntimeException unavailable) {scope=null;}
            }
            if(scope==null) throw failure(AUTHORITY_REQUIRED);
            try {scope.requireManagement(actor,target,input.grants());}
            catch(DomainViolation denied) {throw failure(AUTHORITY_REQUIRED);}
            admit(session,actor);
            String fingerprint=input.fingerprint();
            if(receipt.isPresent()) {
                if(!receipt.get().fingerprint().equals(fingerprint)) throw failure(IDEMPOTENCY_CONFLICT);
                return new Result(admission.accountView(target.id()),receipt.get().resultVersion(),true);
            }
            if(tx.usernameExists(input.username())) throw failure(USERNAME_CONFLICT);
            // Explicit secure adapter required BEFORE creating any account/grant/proof.
            if(invitations==null) throw new IllegalStateException("PROVISIONING_INVITATION_ADAPTER_REQUIRED");
            IdentityAccount pending;
            try {pending=IdentityAccount.provision(target.id(),input.username(),input.grants(),actor,scope);}
            catch(DomainViolation denied) {throw failure(AUTHORITY_REQUIRED);}
            // PostgreSQL TIMESTAMPTZ stores microseconds: bind the exact persisted timestamp in HMAC/AAD.
            var now=clock.instant().truncatedTo(java.time.temporal.ChronoUnit.MICROS);
            try(var issued=invitations.prepare(pending,input.email(),now)) {
                if(issued==null) throw new IllegalStateException("Invalid prepared invitation");
                var proof=issued.proof();byte[] hash=issued.tokenHash();
                if(proof==null || !proof.accountId().equals(pending.id()) || proof.purpose()!=ActionProof.Purpose.INVITATION
                        || proof.state()!=ActionProof.State.AVAILABLE || !proof.createdAt().equals(now)
                        || !proof.expiresAt().equals(now.plus(Duration.ofHours(24))) || hash==null || hash.length!=32)
                    throw new IllegalStateException("Invalid prepared invitation");
                admit(session,actor);
                try {tx.insertPending(pending,input,now);tx.appendProof(proof,hash,issued.verifierKeyId());}
                finally {Arrays.fill(hash,(byte)0);}
                issued.appendDeliveryIntent(); // same transaction only, never network delivery
                admit(session,actor);
                tx.appendAudit(actor.id(),pending.id(),command.requestId(),now);
                tx.appendReceipt(actor.id(),command.operationKey(),new IdentityProvisionStore.Receipt(pending.id(),1,fingerprint),now,now.plus(retention));
                return new Result(admission.accountView(pending.id()),1,false);
            }
        });
    }
    private void admit(IdentitySession session,IdentityAccount actor) {
        try {session.admit(actor,clock.instant());}catch(DomainViolation denied){throw failure(SESSION_INVALID);}
    }
    private static IdentityCommandFailure failure(IdentityCommandFailure.Code code) {return new IdentityCommandFailure(code);}
}
