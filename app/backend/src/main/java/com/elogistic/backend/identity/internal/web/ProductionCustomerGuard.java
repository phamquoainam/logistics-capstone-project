package com.elogistic.backend.identity.internal.web;

import com.elogistic.backend.identity.api.*;
import com.elogistic.backend.identity.api.customer.*;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.*;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import tools.jackson.databind.json.JsonMapper;

/** Customer filter boundary; no SYSTEM/Demo token, cookie-only principal or forwarded-header trust. */
public final class ProductionCustomerGuard extends OncePerRequestFilter {
    private static final JsonMapper JSON=JsonMapper.builder().build();
    private final ProductionAccessTokens tokens;private final ProductionCustomerRequestAdmission admission;
    private final CustomerAdmission customers;private final ProductionAuthenticationHttp authentication;private final ProductionCustomerEnrollmentHttp enrollments;private final ProductionCustomerReauthenticationHttp reauthentication;
    public ProductionCustomerGuard(ProductionAccessTokens tokens,ProductionCustomerRequestAdmission admission,CustomerAdmission customers,
                                   ProductionAuthenticationHttp authentication,ProductionCustomerEnrollmentHttp enrollments,ProductionCustomerReauthenticationHttp reauthentication){
        this.tokens=Objects.requireNonNull(tokens);this.admission=Objects.requireNonNull(admission);this.customers=Objects.requireNonNull(customers);
        this.authentication=Objects.requireNonNull(authentication);this.enrollments=Objects.requireNonNull(enrollments);this.reauthentication=Objects.requireNonNull(reauthentication);
    }
    @Override protected void doFilterInternal(HttpServletRequest request,HttpServletResponse response,FilterChain chain)throws ServletException,IOException {
        UUID requestId=UUID.randomUUID();response.setHeader("Cache-Control","no-store");response.setHeader("X-Request-Id",requestId.toString());
        response.setHeader("X-Content-Type-Options","nosniff");response.setHeader("Referrer-Policy","no-referrer");
        if(authentication.matches(request)){authentication.handle(request,response);return;}
        if(enrollments.matches(request)){enrollments.handle(request,response);return;}
        try{
            var values=Collections.list(request.getHeaders("Authorization"));
            if(values.size()!=1 || !values.getFirst().regionMatches(true,0,"Bearer ",0,7))throw invalid();
            String access=values.getFirst().substring(7);var verified=tokens.verify(access);
            try(var scope=admission.enter(requestId,access,verified.sessionId(),request)){
                admission.authorize(requestId,verified.sessionId());String path=request.getRequestURI().substring(request.getContextPath().length());
                if(reauthentication.matches(request)){reauthentication.handle(request,response);return;}
                if(request.getMethod().equals("GET") && path.equals("/api/v1/customer-auth/me")){
                    var result=customers.execute(access,requestId,Set.of(),context->{
                        var actor=context.actor();return Map.of("accountId",actor.accountId(),"displayName",actor.displayName(),"verifiedEmail",actor.verifiedEmail(),"securityVersion",actor.securityVersion());
                    });
                    response.setStatus(200);response.setContentType("application/json;charset=UTF-8");response.getWriter().write(JSON.writeValueAsString(result));return;
                }
                // Business controllers additionally call CustomerAdmission.execute with their target subjects.
                if(!path.startsWith("/api/v1/customer-auth/")){
                    customers.execute(access,requestId,Set.of(),CustomerAdmission.Context::actor);
                    var security=SecurityContextHolder.createEmptyContext();security.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(admission.current(),"",List.of()));SecurityContextHolder.setContext(security);
                    try{chain.doFilter(request,response);}finally{SecurityContextHolder.clearContext();}return;
                }
                ProductionReadGuard.deny(response,403,"AUTHORITY_REQUIRED");
            }
        }catch(IdentityCommandFailure rejected){if(!response.isCommitted())ProductionReadGuard.deny(response,401,"SESSION_INVALID");}
        catch(Exception unavailable){if(!response.isCommitted())ProductionReadGuard.deny(response,503,"SERVICE_UNAVAILABLE");}
    }
    private static IdentityCommandFailure invalid(){return new IdentityCommandFailure(IdentityCommandFailure.Code.SESSION_INVALID);}
}
