package com.elogistic.backend.identity.internal.application;

import com.elogistic.backend.identity.api.IdentityCommandFailure;
import com.elogistic.backend.identity.api.customer.*;
import com.elogistic.backend.identity.internal.application.port.*;
import com.elogistic.backend.identity.internal.domain.CustomerProof;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;

/** Org is an opaque purpose binding here. Merchant must independently admit real org/reviewers/target later. */
public final class CustomerOwnerConsents implements CustomerOwnerRecoveryConsents {
    private static final String OP="createOwnerRecoveryConsent";
    private final CustomerAdmission admission;private final CustomerRequestContext requests;private final CustomerReauthentication reauthentication;
    private final CustomerOwnerConsentStore store;private final CustomerProofProtection protection;private final Clock clock;
    public CustomerOwnerConsents(CustomerAdmission admission,CustomerRequestContext requests,CustomerReauthentication reauthentication,
            CustomerOwnerConsentStore store,CustomerProofProtection protection,Clock clock){
        this.admission=Objects.requireNonNull(admission);this.requests=Objects.requireNonNull(requests);this.reauthentication=Objects.requireNonNull(reauthentication);
        this.store=Objects.requireNonNull(store);this.protection=Objects.requireNonNull(protection);this.clock=Objects.requireNonNull(clock);
    }
    @Override public Result create(Command command){
        if(command==null || command.key()==null || command.organizationId()==null || command.reauthenticationProofId()==null)throw new IdentityCommandFailure(IdentityCommandFailure.Code.VALIDATION_FAILED);
        var request=requests.current();
        return admission.execute(request.encodedAccess(),request.requestId(),Set.of(),context->{
            admission.requireCurrent(context);var actor=context.actor();
            var previous=store.receipt(actor.accountId(),command.key()).orElse(null);
            String[] fields={actor.accountId().toString(),context.sessionId().toString(),Long.toString(actor.securityVersion()),command.organizationId().toString(),command.reauthenticationProofId().toString()};
            if(previous!=null){
                CustomerReceipts.match(previous.binding(),protection,OP,fields);var proof=previous.proof();
                if(proof.purpose()!=CustomerProof.Purpose.OWNER_RECOVERY_CONSENT || !proof.subjectId().equals(actor.accountId())
                    || !context.sessionId().equals(proof.sessionId()) || !Objects.equals(proof.authVersion(),actor.securityVersion())
                    || !command.organizationId().equals(proof.organizationId()) || !command.reauthenticationProofId().equals(previous.sourceReauthentication()))throw new IdentityCommandFailure(IdentityCommandFailure.Code.IDEMPOTENCY_CONFLICT);
                return new Result(proof.id(),proof.organizationId(),proof.expiresAt());
            }
            // Single-use consumption/audit joins this physical transaction; later failure restores the proof.
            reauthentication.consume(context,command.reauthenticationProofId());
            Instant now=clock.instant().truncatedTo(ChronoUnit.MICROS);
            var proof=CustomerProof.issue(UUID.randomUUID(),actor.accountId(),CustomerProof.Purpose.OWNER_RECOVERY_CONSENT,now,context.sessionId(),actor.securityVersion(),command.organizationId());
            admission.requireDeadline(context,proof.expiresAt());
            store.replace(proof,now);store.audit(actor.accountId(),context.requestId(),now);
            store.receipt(actor.accountId(),command.key(),new CustomerOwnerConsentStore.Receipt(CustomerReceipts.prepare(protection,context.requestId(),OP,fields),proof,command.reauthenticationProofId()),now);
            return new Result(proof.id(),proof.organizationId(),proof.expiresAt());
        });
    }
}
