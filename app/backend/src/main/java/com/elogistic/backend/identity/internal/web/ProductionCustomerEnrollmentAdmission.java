package com.elogistic.backend.identity.internal.web;

import com.elogistic.backend.identity.api.IdentityCommandFailure;
import com.elogistic.backend.identity.internal.application.port.*;
import java.util.*;

/** Only the HTTPS customer adapter opens this request scope. Quota survives command rollback. */
public final class ProductionCustomerEnrollmentAdmission implements CustomerEnrollmentAdmission {
    private static final class State {
        final UUID request;final String cookie,challenge,address;Binding binding;
        State(UUID request,String cookie,String challenge,String address){this.request=request;this.cookie=cookie;this.challenge=challenge;this.address=address;}
    }
    private final ThreadLocal<State> current=new ThreadLocal<>();
    private final CustomerPreauthBrowserState browser;private final IdentityRateLimiter limiter;
    public ProductionCustomerEnrollmentAdmission(CustomerPreauthBrowserState browser,IdentityRateLimiter limiter){this.browser=Objects.requireNonNull(browser);this.limiter=Objects.requireNonNull(limiter);}
    AutoCloseable enter(UUID request,String cookie,String challenge,String address){
        if(request==null || address==null || current.get()!=null)throw invalid();current.set(new State(request,cookie,challenge,address));return current::remove;
    }
    private static IdentityCommandFailure invalid(){return new IdentityCommandFailure(IdentityCommandFailure.Code.CSRF_REJECTED);}
    @Override public Binding admit(Operation operation,UUID request,String exactSubject){
        State state=current.get();if(state==null || operation==null || !state.request.equals(request) || state.binding!=null)throw invalid();
        UUID id=browser.requirePreauthentication(state.cookie,state.challenge);
        var policy=switch(operation){case ENROLL->IdentityRateLimiter.Policy.CUSTOMER_ENROLL_EMAIL;case RESEND->IdentityRateLimiter.Policy.CUSTOMER_RESEND_EMAIL;case VERIFY->IdentityRateLimiter.Policy.CUSTOMER_VERIFY_PROOF;};
        var quotas=new ArrayList<IdentityRateLimiter.Subject>();quotas.add(new IdentityRateLimiter.Subject(policy,exactSubject));
        if(operation!=Operation.VERIFY)quotas.add(new IdentityRateLimiter.Subject(IdentityRateLimiter.Policy.CUSTOMER_ENROLL_IP,state.address));
        limiter.reserve(quotas);state.binding=new Binding(id);return state.binding;
    }
    @Override public void requireCurrent(Binding binding){
        State state=current.get();if(state==null || binding==null || state.binding!=binding
            || !binding.id().equals(browser.requirePreauthentication(state.cookie,state.challenge)))throw invalid();
    }
}
