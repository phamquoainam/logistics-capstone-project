package com.elogistic.backend.identity.internal.web;

import com.elogistic.backend.identity.api.IdentityCommandFailure;
import com.elogistic.backend.identity.api.customer.CustomerRequestContext;
import com.elogistic.backend.identity.internal.application.port.*;
import jakarta.servlet.http.HttpServletRequest;
import java.util.*;

/** Same servlet request, same verified customer session, final live CSRF recheck on every mutation. */
public final class ProductionCustomerRequestAdmission implements CustomerRequestAdmission,CustomerRequestContext {
    private record State(Request request,UUID session,HttpServletRequest http){}
    private final ThreadLocal<State> current=new ThreadLocal<>();
    private final IdentityBrowserState browser;private final String origin;
    public ProductionCustomerRequestAdmission(IdentityBrowserState browser,String origin){
        this.browser=Objects.requireNonNull(browser);this.origin=requireOrigin(origin);
    }
    static String requireOrigin(String origin){
        try{var uri=java.net.URI.create(origin);if(!"https".equals(uri.getScheme()) || uri.getHost()==null || uri.getRawUserInfo()!=null
            || uri.getRawQuery()!=null || uri.getRawFragment()!=null || !(uri.getRawPath()==null || uri.getRawPath().isEmpty()))throw new IllegalArgumentException();return origin;}
        catch(Exception invalid){throw new IllegalArgumentException("CUSTOMER_ORIGIN_CONFIGURATION_INVALID");}
    }
    AutoCloseable enter(UUID request,String access,UUID session,HttpServletRequest http){
        if(request==null || access==null || session==null || http==null || current.get()!=null)throw invalid();
        current.set(new State(new Request(request,access),session,http));return current::remove;
    }
    private static IdentityCommandFailure invalid(){return new IdentityCommandFailure(IdentityCommandFailure.Code.SESSION_INVALID);}
    @Override public Request current(){State state=current.get();if(state==null)throw invalid();return state.request;}
    public IdentityRateLimiter.Reservation reservePassword(IdentityRateLimiter limiter,String exactEmail){
        State state=current.get();if(state==null || !state.http.getMethod().equals("POST")
            || !state.http.getRequestURI().substring(state.http.getContextPath().length()).equals("/api/v1/customer-auth/reauthentication"))throw invalid();
        authorize(state.request.requestId(),state.session);
        return limiter.reserve(List.of(new IdentityRateLimiter.Subject(IdentityRateLimiter.Policy.CUSTOMER_LOGIN_EMAIL,exactEmail),
            new IdentityRateLimiter.Subject(IdentityRateLimiter.Policy.CUSTOMER_LOGIN_IP,ProductionAuthenticationHttp.clientAddress(state.http))));
    }
    @Override public void authorize(UUID request,UUID session){
        State state=current.get();if(state==null || !state.request.requestId().equals(request) || !state.session.equals(session))throw invalid();
        var http=state.http;boolean mutation=!http.getMethod().equals("GET");
        var origins=Collections.list(http.getHeaders("Origin"));var bearers=Collections.list(http.getHeaders("Authorization"));
        if(!http.isSecure() || http.getParameter("access_token")!=null || origins.size()>1 || (mutation&&origins.size()!=1)
            || (!origins.isEmpty()&&!origin.equals(origins.getFirst())) || bearers.size()!=1
            || !bearers.getFirst().regionMatches(true,0,"Bearer ",0,7) || !bearers.getFirst().substring(7).equals(state.request.encodedAccess()))throw invalid();
        if(mutation)browser.verify(ProductionAuthenticationHttp.cookie(http,"__Host-elogistics.customer.csrf"),ProductionAuthenticationHttp.single(http,"X-CSRF-Token"),session,false);
    }
}
