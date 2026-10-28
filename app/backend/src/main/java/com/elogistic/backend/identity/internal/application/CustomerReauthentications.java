package com.elogistic.backend.identity.internal.application;

import com.elogistic.backend.identity.api.IdentityCommandFailure;
import com.elogistic.backend.identity.api.customer.*;
import com.elogistic.backend.identity.internal.application.port.*;
import com.elogistic.backend.identity.internal.domain.*;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;

/** Password is checked against the locked current subject; input email cannot choose an actor. */
public final class CustomerReauthentications implements CustomerReauthentication {
    private static final String OP="requestCustomerReauthentication";
    private final CustomerAdmission admission;private final CustomerRequestContext requests;
    private final CustomerPasswordAttemptAdmission attempts;private final CustomerReauthenticationStore store;
    private final CustomerProofProtection protection;private final SessionCredentials credentials;private final Clock clock;
    public CustomerReauthentications(CustomerAdmission admission,CustomerRequestContext requests,CustomerPasswordAttemptAdmission attempts,
            CustomerReauthenticationStore store,CustomerProofProtection protection,SessionCredentials credentials,Clock clock){
        this.admission=Objects.requireNonNull(admission);this.requests=Objects.requireNonNull(requests);this.attempts=Objects.requireNonNull(attempts);
        this.store=Objects.requireNonNull(store);this.protection=Objects.requireNonNull(protection);this.credentials=Objects.requireNonNull(credentials);this.clock=Objects.requireNonNull(clock);
    }
    private static IdentityCommandFailure failure(IdentityCommandFailure.Code code){return new IdentityCommandFailure(code);}
    @Override public Result request(Command input){
        if(input==null || input.key()==null || input.password()==null || input.password().isEmpty()
            || input.password().codePointCount(0,input.password().length())>256)throw failure(IdentityCommandFailure.Code.VALIDATION_FAILED);
        try{new CustomerEmail(input.email());}catch(DomainViolation invalid){throw failure(IdentityCommandFailure.Code.VALIDATION_FAILED);}
        var request=requests.current();var reservation=attempts.reserve(input.email());
        Result result;
        try{
            result=admission.execute(request.encodedAccess(),request.requestId(),Set.of(),context->{
                admission.requireCurrent(context);var actor=context.actor();Instant now=clock.instant().truncatedTo(ChronoUnit.MICROS);
                var prior=store.receipt(actor.accountId(),input.key()).orElse(null);
                if(prior!=null){
                    CustomerReceipts.match(prior.binding(),protection,OP,actor.accountId().toString(),context.sessionId().toString(),Long.toString(actor.securityVersion()),input.email(),input.password());
                    if(!prior.sessionId().equals(context.sessionId()) || prior.authVersion()!=actor.securityVersion())throw failure(IdentityCommandFailure.Code.IDEMPOTENCY_CONFLICT);
                    // Return original safe outcome, even when the proof was subsequently consumed/revoked.
                    // Consumers independently require the current unconsumed proof; replay never renews it.
                    return new Result(prior.proof().id(),prior.proof().expiresAt());
                }
                // A mismatched destination uses the accepted dummy verifier, so rotating input emails
                // cannot test this account's password outside its exact-email budget.
                boolean password=credentials.verify(input.password(),input.email().equals(actor.verifiedEmail())?store.passwordVerifier(actor.accountId()):null);
                if(!password || !input.email().equals(actor.verifiedEmail()))throw failure(IdentityCommandFailure.Code.INVALID_CREDENTIALS);
                var proof=CustomerProof.issue(UUID.randomUUID(),actor.accountId(),CustomerProof.Purpose.REAUTHENTICATION,now,context.sessionId(),actor.securityVersion(),null);
                admission.requireDeadline(context,proof.expiresAt());
                var binding=CustomerReceipts.prepare(protection,context.requestId(),OP,actor.accountId().toString(),context.sessionId().toString(),Long.toString(actor.securityVersion()),input.email(),input.password());
                store.replace(proof,now);store.audit(actor.accountId(),context.requestId(),"CUSTOMER_REAUTHENTICATED",now);
                store.receipt(actor.accountId(),input.key(),new CustomerReauthenticationStore.Receipt(binding,context.sessionId(),actor.securityVersion(),proof),now);
                // Reauthentication does not extend the session. Only a later committed business command may do that.
                return new Result(proof.id(),proof.expiresAt());
            });
        }catch(IdentityCommandFailure rejected){if(rejected.code()==IdentityCommandFailure.Code.INVALID_CREDENTIALS)reservation.finishLogin(false);throw rejected;}
        reservation.finishLogin(true);return result;
    }
    @Override public void consume(CustomerAdmission.Context context,UUID proofId){
        admission.requireCurrent(context);if(proofId==null)throw failure(IdentityCommandFailure.Code.TOKEN_INVALID_OR_EXPIRED);
        var proof=store.proof(proofId).orElseThrow(()->failure(IdentityCommandFailure.Code.TOKEN_INVALID_OR_EXPIRED));
        try{
            var after=proof.consume(CustomerProof.Purpose.REAUTHENTICATION,context.actor().accountId(),context.sessionId(),context.actor().securityVersion(),null,clock.instant().truncatedTo(ChronoUnit.MICROS));
            admission.requireDeadline(context,proof.expiresAt());
            store.consume(proof,after);store.audit(context.actor().accountId(),context.requestId(),"CUSTOMER_REAUTHENTICATION_CONSUMED",after.consumedAt());
        }catch(DomainViolation invalid){throw failure(IdentityCommandFailure.Code.TOKEN_INVALID_OR_EXPIRED);}
    }
}
