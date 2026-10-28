package com.elogistic.backend.identity.internal.web;
import com.elogistic.backend.identity.internal.application.port.*;
import com.elogistic.backend.identity.api.IdentityCommandFailure;
import java.util.*;
/** Context can only be opened by the same package's HTTPS/Origin HTTP adapter. */
public final class ProductionSessionAdmission implements SessionAdmission {
    private record State(UUID request,String cookie,String challenge){}
    private final ThreadLocal<State> current=new ThreadLocal<>();
    private final IdentityBrowserState browser;private final IdentityRateLimiter limiter;
    private final boolean customer;
    public ProductionSessionAdmission(IdentityBrowserState browser,IdentityRateLimiter limiter){this(browser,limiter,false);}
    public ProductionSessionAdmission(IdentityBrowserState browser,IdentityRateLimiter limiter,boolean customer){this.browser=Objects.requireNonNull(browser);this.limiter=Objects.requireNonNull(limiter);this.customer=customer;}
    AutoCloseable enter(UUID request,String cookie,String challenge){
        if(request==null||current.get()!=null)throw invalid();current.set(new State(request,cookie,challenge));return ()->current.remove();
    }
    @Override public void authorize(Operation operation,UUID request,UUID session,boolean present){
        State s=current.get();if(s==null||!s.request.equals(request)||operation==null||present!=(session!=null))throw invalid();
        browser.verify(s.cookie,s.challenge,session,operation==Operation.LOGIN);
        if(operation==Operation.REFRESH)limiter.reserve(List.of(new IdentityRateLimiter.Subject(customer?IdentityRateLimiter.Policy.CUSTOMER_REFRESH_SESSION:IdentityRateLimiter.Policy.REFRESH_SESSION,session.toString())));
    }
    private static IdentityCommandFailure invalid(){return new IdentityCommandFailure(IdentityCommandFailure.Code.CSRF_REJECTED);}
}
