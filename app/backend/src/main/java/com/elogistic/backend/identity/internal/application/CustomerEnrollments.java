package com.elogistic.backend.identity.internal.application;

import com.elogistic.backend.identity.api.IdentityCommandFailure;
import com.elogistic.backend.identity.api.customer.CustomerEnrollmentCommands;
import com.elogistic.backend.identity.internal.application.port.*;
import com.elogistic.backend.identity.internal.domain.*;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import static com.elogistic.backend.identity.api.IdentityCommandFailure.Code.*;
import static com.elogistic.backend.identity.internal.application.port.CustomerEnrollmentAdmission.Operation.*;

/** Uniform preauth commands, single-use activation and atomic protected delivery intent. */
public final class CustomerEnrollments implements CustomerEnrollmentCommands {
    private final CustomerEnrollmentStore store;
    private final CustomerProofProtection protection;
    private final CustomerEnrollmentAdmission admission;
    private final Clock clock;
    public CustomerEnrollments(CustomerEnrollmentStore store,CustomerProofProtection protection,CustomerEnrollmentAdmission admission,Clock clock){
        this.store=Objects.requireNonNull(store);this.protection=Objects.requireNonNull(protection);this.admission=Objects.requireNonNull(admission);this.clock=Objects.requireNonNull(clock);
    }
    private Instant now(){return clock.instant().truncatedTo(ChronoUnit.MICROS);}
    private static IdentityCommandFailure fail(IdentityCommandFailure.Code code){return new IdentityCommandFailure(code);}
    private static void ids(UUID key,UUID request){if(key==null || request==null)throw fail(VALIDATION_FAILED);}
    private static CustomerEmail email(String value){try{return new CustomerEmail(value);}catch(DomainViolation invalid){throw fail(VALIDATION_FAILED);}}
    private static void text(String value,int max){if(value==null || value.isBlank() || value.codePointCount(0,value.length())>max)throw fail(VALIDATION_FAILED);}
    private static String phone(String value){
        if(value==null)return null; // Reviewed legacy enrollment remains accepted for backward compatibility.
        String canonical=value.replaceAll("[\\s()-]","");
        if(canonical.matches("0[35789][0-9]{8}"))canonical="+84"+canonical.substring(1);
        else if(canonical.matches("84[35789][0-9]{8}"))canonical="+"+canonical;
        if(!canonical.matches("\\+[1-9][0-9]{7,14}"))throw fail(VALIDATION_FAILED);
        return canonical;
    }
    private static String[] enrollmentFingerprint(Enrollment c,String canonicalPhone){
        return canonicalPhone==null?new String[]{c.email(),c.password(),c.displayName(),c.consentVersion()}
            :new String[]{c.email(),c.password(),c.displayName(),canonicalPhone,c.consentVersion()};
    }
    private CustomerEnrollmentStore.Receipt previous(CustomerEnrollmentStore.Transaction tx,CustomerEnrollmentAdmission.Binding binding,UUID key,String operation,String... fields){
        return CustomerReceipts.match(tx.receipt(binding.id(),key).orElse(null),protection,operation,fields);
    }
    private void receipt(CustomerEnrollmentStore.Transaction tx,CustomerEnrollmentAdmission.Binding binding,UUID key,UUID request,String operation,Instant time,String... fields){
        tx.receipt(binding.id(),key,CustomerReceipts.prepare(protection,request,operation,fields),time);
    }
    private void issue(CustomerEnrollmentStore.Transaction tx,CustomerEnrollment e,Instant time,UUID proofId){
        var proof=CustomerProof.issue(proofId,e.intendedAccountId(),CustomerProof.Purpose.CUSTOMER_ENROLLMENT,time,null,null,null);
        try(var sealed=protection.issue(proof,e.email().value())){tx.issue(e,proof,sealed,time);}
    }
    @Override public Accepted enroll(Enrollment c){
        if(c==null)throw fail(VALIDATION_FAILED);ids(c.key(),c.requestId());var email=email(c.email());text(c.displayName(),160);text(c.consentVersion(),64);String phone=phone(c.phone());
        try{PasswordPolicy.validateNew(c.password());}catch(DomainViolation invalid){throw fail(VALIDATION_FAILED);}
        var binding=Objects.requireNonNull(admission.admit(ENROLL,c.requestId(),email.value()));
        // Same accepted password work for known/unknown addresses; no credential-derived identifier or email takeover.
        String material=protection.hashPassword(c.password());
        return store.transaction(tx->{
            tx.accounts().lockAdministration();admission.requireCurrent(binding);
            var previous=previous(tx,binding,c.key(),"enrollCustomer",enrollmentFingerprint(c,phone));
            if(previous!=null){admission.requireCurrent(binding);return new Accepted(true,previous.requestId());}
            Instant time=now();
            var pending=tx.pending(email.value());
            if(pending.isPresent() && !time.isBefore(pending.get().expiresAt())){
                var expired=pending.get();tx.revokePending(expired.id(),time);tx.update(expired,expired.expire(time));
                tx.audit(expired.intendedAccountId(),c.requestId(),"CUSTOMER_ENROLLMENT_EXPIRED",time);pending=Optional.empty();
            }
            if(!tx.emailOccupied(email.value()) && pending.isEmpty()){
                var enrollment=new CustomerEnrollment(UUID.randomUUID(),UUID.randomUUID(),email,c.displayName(),phone,material,c.consentVersion(),time,time.plusSeconds(86400),CustomerEnrollment.Status.PENDING,1);
                tx.insert(enrollment);issue(tx,enrollment,time,c.requestId());tx.audit(enrollment.intendedAccountId(),c.requestId(),"CUSTOMER_ENROLLMENT_QUEUED",time);
            }
            receipt(tx,binding,c.key(),c.requestId(),"enrollCustomer",time,enrollmentFingerprint(c,phone));
            admission.requireCurrent(binding);return new Accepted(true,c.requestId());
        });
    }
    @Override public Accepted resend(Email c){
        if(c==null)throw fail(VALIDATION_FAILED);ids(c.key(),c.requestId());var email=email(c.email());
        var binding=Objects.requireNonNull(admission.admit(RESEND,c.requestId(),email.value()));
        return store.transaction(tx->{
            tx.accounts().lockAdministration();admission.requireCurrent(binding);
            var previous=previous(tx,binding,c.key(),"resendCustomerVerification",c.email());
            if(previous!=null){admission.requireCurrent(binding);return new Accepted(true,previous.requestId());}
            Instant time=now();var pending=tx.pending(email.value());
            if(!tx.emailOccupied(email.value()) && pending.isPresent() && time.isBefore(pending.get().expiresAt())){
                var before=pending.get();var after=before.resend(time);tx.revokePending(before.id(),time);tx.update(before,after);issue(tx,after,time,c.requestId());
                tx.audit(after.intendedAccountId(),c.requestId(),"CUSTOMER_VERIFICATION_RESENT",time);
            }
            receipt(tx,binding,c.key(),c.requestId(),"resendCustomerVerification",time,c.email());admission.requireCurrent(binding);return new Accepted(true,c.requestId());
        });
    }
    @Override public void verify(Verification c){
        if(c==null || c.proofId()==null || c.proof()==null || c.proof().isEmpty() || c.proof().length()>512)throw fail(VALIDATION_FAILED);ids(c.key(),c.requestId());
        var binding=Objects.requireNonNull(admission.admit(VERIFY,c.requestId(),c.proofId().toString()));
        byte[] supplied=c.proof().getBytes(StandardCharsets.UTF_8);
        try{store.transaction(tx->{
            tx.accounts().lockAdministration();admission.requireCurrent(binding);
            // Possession is required even for a same-key replay; no receipt lookup by email/locator alone.
            var stored=tx.proof(c.proofId()).orElseThrow(()->fail(TOKEN_INVALID_OR_EXPIRED));var proof=stored.proof();
            if(proof.purpose()!=CustomerProof.Purpose.CUSTOMER_ENROLLMENT)throw fail(TOKEN_INVALID_OR_EXPIRED);
            var previous=CustomerReceipts.match(tx.proofReceipt(c.proofId(),c.key()).orElse(null),protection,"verifyCustomerEmail",c.proofId().toString(),c.proof());
            // Retain exact same-binding replay of V8/V9 receipts; new confirmations are proof-scoped.
            if(previous==null)previous=previous(tx,binding,c.key(),"verifyCustomerEmail",c.proofId().toString(),c.proof());
            Instant time=now();
            if(previous!=null){admission.requireCurrent(binding);return null;}
            byte[] verifier=stored.verifier();
            try{if(proof.purpose()!=CustomerProof.Purpose.CUSTOMER_ENROLLMENT || !protection.matches(proof,stored.verifierKey(),supplied,verifier,time))throw fail(TOKEN_INVALID_OR_EXPIRED);}
            finally{Arrays.fill(verifier,(byte)0);}
            var e=tx.enrollment(stored.enrollmentId()).orElseThrow(()->fail(TOKEN_INVALID_OR_EXPIRED));
            try{e.requirePending(time);proof.consume(proof.purpose(),e.intendedAccountId(),null,null,null,time);}catch(DomainViolation invalid){throw fail(TOKEN_INVALID_OR_EXPIRED);}
            if(tx.emailOccupied(e.email().value()))throw fail(TOKEN_INVALID_OR_EXPIRED);
            tx.activate(e,proof,time);tx.update(e,e.consume(time));tx.audit(e.intendedAccountId(),c.requestId(),"CUSTOMER_EMAIL_VERIFIED",time);
            tx.proofReceipt(c.proofId(),c.key(),CustomerReceipts.prepare(protection,c.requestId(),"verifyCustomerEmail",c.proofId().toString(),c.proof()),time);
            // Wall-clock expiry is re-evaluated after staged effects, immediately before owned commit.
            if(!now().isBefore(proof.expiresAt()) || !now().isBefore(e.expiresAt()))throw fail(TOKEN_INVALID_OR_EXPIRED);
            admission.requireCurrent(binding);return null;
        });}finally{Arrays.fill(supplied,(byte)0);}
    }
}
