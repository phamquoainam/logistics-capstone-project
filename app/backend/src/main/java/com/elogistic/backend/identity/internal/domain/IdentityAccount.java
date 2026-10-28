package com.elogistic.backend.identity.internal.domain;

import java.time.Instant;
import java.util.Collection;
import java.util.Set;
import java.util.UUID;
import static com.elogistic.backend.identity.internal.domain.DomainValues.require;
import static com.elogistic.backend.identity.internal.domain.DomainViolation.Reason.*;

/** Immutable authoritative aggregate state, to be loaded/persisted by later application work. */
public record IdentityAccount(UUID id, Username username, Status status, Activation activation,
                              AccountVersion version, Set<Grant> grants) {
    public enum Status { PENDING_ACTIVATION, ACTIVE, DISABLED }
    /** VERIFIED means both stored verified email and an activated credential, matching V3. */
    public enum Activation { UNVERIFIED, VERIFIED }

    public IdentityAccount {
        require(id != null && username != null && status != null && activation != null && version != null,
            INVALID_STATE);
        require(status != Status.PENDING_ACTIVATION || activation == Activation.UNVERIFIED, INVALID_STATE);
        require(status != Status.ACTIVE || activation == Activation.VERIFIED, INVALID_STATE);
        grants = DomainValues.unique(grants);
    }

    public static IdentityAccount provision(UUID id, Username username, Collection<Grant> grants,
                                             IdentityAccount actor, ManagementAuthority authority) {
        IdentityAccount pending = new IdentityAccount(id, username, Status.PENDING_ACTIVATION,
            Activation.UNVERIFIED, new AccountVersion(1, 1), Set.of());
        Set<Grant> proposed = DomainValues.unique(grants);
        require(authority != null, AUTHORITY_REQUIRED);
        authority.requireManagement(actor, pending, proposed);
        return new IdentityAccount(id, username, pending.status, pending.activation, pending.version, proposed);
    }

    public boolean active() { return status == Status.ACTIVE; }
    public boolean managementAdmin() { return active() && grants.stream().anyMatch(Grant::isIdentityManagement); }

    /** Required role AND resource scope, supplied by the trusted owner/use case, never any-match. */
    public boolean hasGrant(Grant required) { return active() && required != null && grants.contains(required); }

    public IdentityAccount replaceGrants(IdentityAccount actor, ManagementAuthority authority,
                                         Collection<Grant> replacement, long expected, long activeAdmins) {
        Set<Grant> proposed = DomainValues.unique(replacement);
        authorize(actor, authority, proposed);
        version.requireExpected(expected);
        protectLastAdmin(proposed.stream().anyMatch(Grant::isIdentityManagement), activeAdmins);
        return new IdentityAccount(id, username, status, activation, version.next(), proposed);
    }

    public IdentityAccount disable(IdentityAccount actor, ManagementAuthority authority,
                                    long expected, long activeAdmins) {
        authorize(actor, authority, Set.of());
        version.requireExpected(expected);
        require(status != Status.DISABLED, INVALID_STATE);
        protectLastAdmin(false, activeAdmins);
        return new IdentityAccount(id, username, Status.DISABLED, activation, version.next(), grants);
    }

    public IdentityAccount reactivate(IdentityAccount actor, ManagementAuthority authority, long expected) {
        authorize(actor, authority, Set.of());
        version.requireExpected(expected);
        require(status == Status.DISABLED && activation == Activation.VERIFIED, INVALID_STATE);
        return new IdentityAccount(id, username, Status.ACTIVE, activation, version.next(), grants);
    }

    /** Application must hash the validated password and commit account + consumed proof atomically. */
    public ProofChange activate(ActionProof invitation, String password, long expected, Instant now) {
        require(invitation != null, PROOF_INVALID);
        ActionProof consumed = invitation.consume(ActionProof.Purpose.INVITATION, id, now);
        version.requireExpected(expected);
        require(status == Status.PENDING_ACTIVATION, INVALID_STATE);
        PasswordPolicy.validateNew(password);
        return new ProofChange(new IdentityAccount(id, username, Status.ACTIVE, Activation.VERIFIED,
            version.next(), grants), consumed);
    }

    public ProofChange resetPassword(ActionProof recovery, String password, long expected, Instant now) {
        require(recovery != null, PROOF_INVALID);
        ActionProof consumed = recovery.consume(ActionProof.Purpose.RECOVERY, id, now);
        version.requireExpected(expected);
        require(active() && activation == Activation.VERIFIED, INVALID_STATE);
        PasswordPolicy.validateNew(password);
        return new ProofChange(new IdentityAccount(id, username, status, activation, version.next(), grants), consumed);
    }

    public CustomerProofChange resetCustomerPassword(CustomerProof recovery,String password,long expected,Instant now){
        require(recovery!=null,PROOF_INVALID);
        var consumed=recovery.consume(CustomerProof.Purpose.CUSTOMER_RECOVERY,id,null,version.authVersion(),null,now);
        version.requireExpected(expected);require(active() && activation==Activation.VERIFIED,INVALID_STATE);PasswordPolicy.validateNew(password);
        return new CustomerProofChange(new IdentityAccount(id,username,status,activation,version.next(),grants),consumed);
    }
    public record CustomerProofChange(IdentityAccount account,CustomerProof consumedProof){
        public CustomerProofChange{require(account!=null && consumedProof!=null && account.id().equals(consumedProof.subjectId()) && consumedProof.purpose()==CustomerProof.Purpose.CUSTOMER_RECOVERY && consumedProof.consumedAt()!=null,INVALID_STATE);}
        @Override public String toString(){return "CustomerProofChange[redacted]";}
    }

    /** Matching is a trusted backend fact against the STORED verified destination, not a client authority. */
    public boolean recoveryEligible(EmailMatch storedVerifiedEmailMatch) {
        return active() && activation == Activation.VERIFIED && storedVerifiedEmailMatch == EmailMatch.MATCH;
    }
    public enum EmailMatch { MATCH, NO_MATCH }

    public record ProofChange(IdentityAccount account, ActionProof consumedProof) {
        public ProofChange {
            require(account != null && consumedProof != null && consumedProof.accountId().equals(account.id())
                && consumedProof.state() == ActionProof.State.CONSUMED, INVALID_STATE);
        }
        public boolean requiresSessionInvalidation() { return true; }
    }

    private void authorize(IdentityAccount actor, ManagementAuthority authority, Set<Grant> proposed) {
        require(authority != null, AUTHORITY_REQUIRED);
        authority.requireManagement(actor, this, proposed);
    }

    /** Count is a trusted, transactionally protected population input, not a concurrency proof. */
    private void protectLastAdmin(boolean remainsAdmin, long activeAdmins) {
        require(activeAdmins >= 0 && (!managementAdmin() || activeAdmins >= 1), INVALID_STATE);
        require(!managementAdmin() || remainsAdmin || activeAdmins > 1, LAST_ACTIVE_ADMIN);
    }

    @Override public String toString() { return "IdentityAccount[redacted]"; }
}
