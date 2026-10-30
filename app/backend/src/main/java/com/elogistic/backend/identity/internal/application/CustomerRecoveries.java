package com.elogistic.backend.identity.internal.application;

import com.elogistic.backend.identity.api.*;
import com.elogistic.backend.identity.api.customer.*;
import com.elogistic.backend.identity.internal.application.port.*;
import com.elogistic.backend.identity.internal.domain.*;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import static com.elogistic.backend.identity.api.IdentityCommandFailure.Code.*;

/** No email/phone account association. Exact derived binding and current security master are mandatory. */
public final class CustomerRecoveries implements CustomerRecoveryCommands {
    private final CustomerRecoveryStore store;private final CustomerProofProtection protection;private final CustomerEnrollmentAdmission admission;private final Clock clock;
    public CustomerRecoveries(CustomerRecoveryStore store,CustomerProofProtection protection,CustomerEnrollmentAdmission admission,Clock clock){this.store=Objects.requireNonNull(store);this.protection=Objects.requireNonNull(protection);this.admission=Objects.requireNonNull(admission);this.clock=Objects.requireNonNull(clock);}
    private Instant now(){return clock.instant().truncatedTo(ChronoUnit.MICROS);}
    private static IdentityCommandFailure fail(IdentityCommandFailure.Code code){return new IdentityCommandFailure(code);}
    private static void ids(UUID key,UUID request){if(key==null || request==null)throw fail(VALIDATION_FAILED);}
    @Override public CustomerEnrollmentCommands.Accepted request(Email c){
        if(c==null)throw fail(VALIDATION_FAILED);ids(c.key(),c.requestId());try{new CustomerEmail(c.email());}catch(DomainViolation invalid){throw fail(VALIDATION_FAILED);}
        var binding=Objects.requireNonNull(admission.admit(CustomerEnrollmentAdmission.Operation.RECOVERY_REQUEST,c.requestId(),c.email()));
        return store.transaction(tx->{
            tx.accounts().lockAdministration();admission.requireCurrent(binding);
            var previous=CustomerReceipts.match(tx.receipt(binding.id(),c.key()).orElse(null),protection,"requestCustomerRecovery",c.email());
            if(previous!=null){admission.requireCurrent(binding);return new CustomerEnrollmentCommands.Accepted(true,previous.requestId());}
            Instant time=now();var candidate=tx.accountForEmail(c.email());
            if(candidate.isPresent()){
                var account=tx.accounts().lockAccount(candidate.get()).orElseThrow(()->fail(TOKEN_INVALID_OR_EXPIRED));var destination=tx.currentEmail(account.id());
                if(account.recoveryEligible(destination.filter(c.email()::equals).isPresent()?IdentityAccount.EmailMatch.MATCH:IdentityAccount.EmailMatch.NO_MATCH)){
                    tx.revokePending(account.id(),time);
                    var proof=new CustomerProof(c.requestId(),account.id(),CustomerProof.Purpose.CUSTOMER_RECOVERY,time,time.plusSeconds(120),null,account.version().authVersion(),null,null,null);
                    try(var sealed=protection.issue(proof,destination.orElseThrow())){tx.issue(proof,destination.get(),sealed,time);}
                    tx.audit(account.id(),c.requestId(),"CUSTOMER_RECOVERY_QUEUED",time);
                }
            }
            tx.receipt(binding.id(),c.key(),CustomerReceipts.prepare(protection,c.requestId(),"requestCustomerRecovery",c.email()),time);admission.requireCurrent(binding);return new CustomerEnrollmentCommands.Accepted(true,c.requestId());
        });
    }
    @Override public Verified verify(Verification c){
        if(c==null || c.proofId()==null || c.proof()==null || !c.proof().matches("[0-9]{6}"))throw fail(VALIDATION_FAILED);ids(c.key(),c.requestId());
        var binding=Objects.requireNonNull(admission.admit(CustomerEnrollmentAdmission.Operation.RECOVERY_VERIFY,c.requestId(),c.proofId().toString()));
        byte[] supplied=c.proof().getBytes(StandardCharsets.US_ASCII);
        try{return store.transaction(tx->{
            tx.accounts().lockAdministration();admission.requireCurrent(binding);
            UUID subject=tx.proofSubject(c.proofId()).orElseThrow(()->fail(TOKEN_INVALID_OR_EXPIRED));
            var account=tx.accounts().lockAccount(subject).orElseThrow(()->fail(TOKEN_INVALID_OR_EXPIRED));
            var stored=tx.proof(c.proofId()).orElseThrow(()->fail(TOKEN_INVALID_OR_EXPIRED));var proof=stored.proof();Instant time=now();
            if(proof.purpose()!=CustomerProof.Purpose.CUSTOMER_RECOVERY || !proof.emailOtp() || !proof.subjectId().equals(subject)
                || !Objects.equals(proof.authVersion(),account.version().authVersion()) || !account.active()
                || account.activation()!=IdentityAccount.Activation.VERIFIED || tx.currentEmail(subject).isEmpty())throw fail(TOKEN_INVALID_OR_EXPIRED);
            byte[] verifier=stored.verifier(),grant=null;
            try{
                if(!protection.matches(proof,stored.verifierKey(),supplied,verifier,time))throw fail(TOKEN_INVALID_OR_EXPIRED);
                grant=protection.recoveryGrant(proof,stored.verifierKey(),verifier,time);
                if(!now().isBefore(proof.expiresAt()))throw fail(TOKEN_INVALID_OR_EXPIRED);admission.requireCurrent(binding);
                return new Verified(proof.id(),new String(grant,StandardCharsets.US_ASCII),proof.expiresAt());
            }finally{Arrays.fill(verifier,(byte)0);if(grant!=null)Arrays.fill(grant,(byte)0);}
        });}finally{Arrays.fill(supplied,(byte)0);}
    }
    @Override public void confirm(Confirmation c){
        if(c==null || c.proofId()==null || c.proof()==null || c.proof().isEmpty() || c.proof().length()>512)throw fail(VALIDATION_FAILED);ids(c.key(),c.requestId());
        try{PasswordPolicy.validateNew(c.newPassword());}catch(DomainViolation invalid){throw fail(VALIDATION_FAILED);}
        var binding=Objects.requireNonNull(admission.admit(CustomerEnrollmentAdmission.Operation.RECOVERY_CONFIRM,c.requestId(),c.proofId()+":reset"));byte[] supplied=c.proof().getBytes(StandardCharsets.UTF_8);
        try{store.transaction(tx->{
            tx.accounts().lockAdministration();admission.requireCurrent(binding);
            UUID subject=tx.proofSubject(c.proofId()).orElseThrow(()->fail(TOKEN_INVALID_OR_EXPIRED));var account=tx.accounts().lockAccount(subject).orElseThrow(()->fail(TOKEN_INVALID_OR_EXPIRED));tx.lockSessions(subject);
            var stored=tx.proof(c.proofId()).orElseThrow(()->fail(TOKEN_INVALID_OR_EXPIRED));var proof=stored.proof();
            if(proof.emailOtp() && c.proof().length()!=43)throw fail(TOKEN_INVALID_OR_EXPIRED);
            if(!proof.subjectId().equals(subject) || proof.purpose()!=CustomerProof.Purpose.CUSTOMER_RECOVERY || !account.active() || account.activation()!=IdentityAccount.Activation.VERIFIED || tx.currentEmail(subject).isEmpty())throw fail(TOKEN_INVALID_OR_EXPIRED);
            var previous=CustomerReceipts.match(tx.proofReceipt(c.proofId(),c.key()).orElse(null),protection,"confirmCustomerRecovery",c.proofId().toString(),c.proof(),c.newPassword());
            if(previous!=null){
                if(proof.consumedAt()==null || proof.authVersion()==null || proof.authVersion()==Long.MAX_VALUE || account.version().authVersion()!=proof.authVersion()+1)throw fail(TOKEN_INVALID_OR_EXPIRED);
                admission.requireCurrent(binding);return null;
            }
            Instant time=now();byte[] verifier=stored.verifier();
            try{if(!protection.matches(proof,stored.verifierKey(),supplied,verifier,time))throw fail(TOKEN_INVALID_OR_EXPIRED);}finally{Arrays.fill(verifier,(byte)0);}
            IdentityAccount.CustomerProofChange change;
            try{change=account.resetCustomerPassword(proof,c.newPassword(),account.version().version(),time);}catch(DomainViolation invalid){throw fail(TOKEN_INVALID_OR_EXPIRED);}
            tx.reset(account,change,protection.hashPassword(c.newPassword()),supplied,time);tx.audit(subject,c.requestId(),"CUSTOMER_RECOVERY_CONFIRMED",time);
            tx.proofReceipt(c.proofId(),c.key(),CustomerReceipts.prepare(protection,c.requestId(),"confirmCustomerRecovery",c.proofId().toString(),c.proof(),c.newPassword()),time);
            if(!now().isBefore(proof.expiresAt()))throw fail(TOKEN_INVALID_OR_EXPIRED);admission.requireCurrent(binding);return null;
        });}finally{Arrays.fill(supplied,(byte)0);}
    }
}
