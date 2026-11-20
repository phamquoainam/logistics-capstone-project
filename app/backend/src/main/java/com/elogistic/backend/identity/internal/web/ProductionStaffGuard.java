package com.elogistic.backend.identity.internal.web;

import com.elogistic.backend.identity.api.IdentityCommandFailure;
import com.elogistic.backend.identity.api.staff.*;
import com.elogistic.backend.identity.internal.application.port.*;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.BooleanSupplier;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.core.StreamReadFeature;
import tools.jackson.databind.json.JsonMapper;

/** Fixed Staff browser boundary. Disabled deployments never reach auth, DB mutation or controllers. */
public final class ProductionStaffGuard extends OncePerRequestFilter {
    private static final JsonMapper JSON=JsonMapper.builder().enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION).build();
    private final ProductionAccessTokens tokens;
    private final ProductionStaffRequestAdmission requests;
    private final StaffAdmission staff;
    private final ProductionAuthenticationHttp authentication;
    private final StaffInvitationActivation activation;
    private final IdentityBrowserState browser;
    private final IdentityRateLimiter limiter;
    private final String origin,environment;
    private final BooleanSupplier available;
    private ProductionStaffGuard(){tokens=null;requests=null;staff=null;authentication=null;activation=null;browser=null;limiter=null;origin=null;environment=null;available=()->false;}
    public static ProductionStaffGuard disabled(){return new ProductionStaffGuard();}
    public ProductionStaffGuard(ProductionAccessTokens tokens,ProductionStaffRequestAdmission requests,StaffAdmission staff,
        ProductionAuthenticationHttp authentication,StaffInvitationActivation activation,IdentityBrowserState browser,
        IdentityRateLimiter limiter,String origin,String environment,BooleanSupplier available){
        this.tokens=Objects.requireNonNull(tokens);this.requests=Objects.requireNonNull(requests);this.staff=Objects.requireNonNull(staff);
        this.authentication=Objects.requireNonNull(authentication);this.activation=Objects.requireNonNull(activation);
        this.browser=Objects.requireNonNull(browser);this.limiter=Objects.requireNonNull(limiter);
        this.origin=ProductionCustomerRequestAdmission.requireOrigin(origin);
        if(!Set.of("LOCAL","TEST").contains(environment))throw new IllegalArgumentException("STAFF_ENVIRONMENT_REQUIRED");
        this.environment=environment;this.available=Objects.requireNonNull(available);
    }
    @Override protected void doFilterInternal(HttpServletRequest request,HttpServletResponse response,FilterChain chain)throws ServletException,IOException {
        UUID requestId=UUID.randomUUID();response.setHeader("Cache-Control","no-store");response.setHeader("X-Request-Id",requestId.toString());
        response.setHeader("X-Content-Type-Options","nosniff");response.setHeader("Referrer-Policy","no-referrer");
        try {
            if(!available.getAsBoolean()){deny(response,503,"STAFF_UNAVAILABLE");return;}
            String path=request.getRequestURI().substring(request.getContextPath().length());
            if(authentication.matches(request)){authentication.handle(request,response);return;}
            if(request.getMethod().equals("POST")&&path.equals("/api/v1/staff-auth/invitations/accept")){
                accept(request,response,requestId);return;
            }
            var bearers=Collections.list(request.getHeaders("Authorization"));
            if(bearers.size()!=1 || !bearers.getFirst().regionMatches(true,0,"Bearer ",0,7))throw invalid();
            String encoded=bearers.getFirst().substring(7);var verified=tokens.verify(encoded);
            try(var ignored=requests.enter(requestId,encoded,verified.sessionId(),request)){
                requests.authorize(requestId,verified.sessionId());
                if(request.getMethod().equals("GET")&&path.equals("/api/v1/staff-auth/me")){
                    if(!request.getParameterMap().isEmpty()){deny(response,400,"VALIDATION_FAILED");return;}
                    var result=staff.execute(encoded,requestId,Set.of(),c->Map.of("accountId",c.actor().accountId(),"displayName",c.actor().displayName(),"principalType","STAFF","purpose","SUPPORT","environment",environment));
                    response.setStatus(200);response.setContentType("application/json;charset=UTF-8");response.getWriter().write(JSON.writeValueAsString(result));return;
                }
                if(!organizationOperation(request.getMethod(),path)&&!personalOperation(request.getMethod(),path)){deny(response,403,"AUTHORITY_REQUIRED");return;}
                staff.execute(encoded,requestId,Set.of(),StaffAdmission.Context::actor);
                var security=SecurityContextHolder.createEmptyContext();security.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(requests.current(),"",List.of()));SecurityContextHolder.setContext(security);
                try{chain.doFilter(request,response);}finally{SecurityContextHolder.clearContext();}
            }
        }catch(IdentityRateLimiter.Exhausted exhausted){response.setHeader("Retry-After",Integer.toString(exhausted.retryAfter));deny(response,429,"RATE_LIMITED");}
        catch(IdentityCommandFailure failure){
            String code=failure.code().name();int status=switch(code){case "CSRF_REJECTED","ORIGIN_REJECTED"->403;case "VALIDATION_FAILED"->400;case "IDEMPOTENCY_CONFLICT"->409;case "TOKEN_INVALID_OR_EXPIRED"->400;default->401;};
            deny(response,status,status==401?"SESSION_INVALID":code);
        }catch(Exception unavailable){deny(response,503,"STAFF_UNAVAILABLE");}
    }
    public static boolean organizationOperation(String method,String path){
        if(method.equals("GET"))return path.equals("/api/v1/support-operations/organization-queue")
            ||path.matches("/api/v1/support-operations/organization-cases/[0-9a-f-]{36}")
            ||path.matches("/api/v1/support-operations/organization-claim-intakes/[0-9a-f-]{36}")
            ||path.matches("/api/v1/support-operations/organization-claim-intakes/[0-9a-f-]{36}/evidence(?:/[0-9a-f-]{36})?")
            ||path.matches("/api/v1/support-operations/organization-cases/[0-9a-f-]{36}/claim-intakes")
            ||path.matches("/api/v1/support-operations/organization-receipts/[A-Z_]+/[0-9a-f-]{36}");
        return method.equals("POST")&&(path.matches("/api/v1/support-operations/organization-cases/[0-9a-f-]{36}/(assignments|replies|transitions)")||path.matches("/api/v1/support-operations/organization-claim-intakes/[0-9a-f-]{36}/commands"));
    }
    /** Compatibility for existing canonical Personal Support CSKH; Customer self routes are unchanged. */
    public static boolean personalOperation(String method,String path){
        if(method.equals("GET"))return path.equals("/api/v1/support-operations/queue")||path.matches("/api/v1/support-operations/cases/[0-9a-f-]{36}");
        return method.equals("POST")&&path.matches("/api/v1/support-operations/cases/[0-9a-f-]{36}/(assignments|transitions)");
    }
    private void accept(HttpServletRequest request,HttpServletResponse response,UUID requestId)throws IOException {
        var origins=Collections.list(request.getHeaders("Origin"));
        if(!request.isSecure()||origins.size()!=1||!origin.equals(origins.getFirst())){deny(response,403,"ORIGIN_REJECTED");return;}
        if(!request.getParameterMap().isEmpty()){deny(response,400,"VALIDATION_FAILED");return;}
        browser.verify(ProductionAuthenticationHttp.cookie(request,"__Host-argopost.staff.csrf"),ProductionAuthenticationHttp.single(request,"X-CSRF-Token"),null,true);
        if(request.getContentType()==null||!request.getContentType().split(";",2)[0].equalsIgnoreCase("application/json")){deny(response,400,"VALIDATION_FAILED");return;}
        byte[] bytes=request.getInputStream().readNBytes(16385),token=null;
        try {
            if(bytes.length>16384)throw validation();var input=JSON.readTree(bytes);
            if(input==null||!input.isObject()||!input.propertyNames().equals(Set.of("proofId","token","password"))
                ||!input.get("proofId").isString()||!input.get("token").isString()||!input.get("password").isString())throw validation();
            UUID proof=uuid(input.get("proofId").asString()),key=uuid(ProductionAuthenticationHttp.single(request,"Idempotency-Key"));
            String supplied=input.get("token").asString();if(!supplied.matches("[A-Za-z0-9_-]{43}"))throw validation();
            token=supplied.getBytes(StandardCharsets.US_ASCII);
            limiter.reserve(List.of(new IdentityRateLimiter.Subject(IdentityRateLimiter.Policy.INVITATION_TOKEN,"STAFF:"+proof),
                new IdentityRateLimiter.Subject(IdentityRateLimiter.Policy.RESEND_IP,"STAFF:ACTIVATE:"+ProductionAuthenticationHttp.clientAddress(request))));
            activation.accept(proof,token,input.get("password").asString(),key,requestId);response.setStatus(204);
        }catch(IdentityCommandFailure failure){throw failure;}
        catch(IllegalArgumentException invalid){throw validation();}
        finally{Arrays.fill(bytes,(byte)0);if(token!=null)Arrays.fill(token,(byte)0);}
    }
    private static UUID uuid(String value){try{UUID id=UUID.fromString(value);if(!id.toString().equals(value)||id.equals(new UUID(0,0)))throw validation();return id;}catch(IllegalArgumentException invalid){throw validation();}}
    private static IdentityCommandFailure validation(){return new IdentityCommandFailure(IdentityCommandFailure.Code.VALIDATION_FAILED);}
    private static IdentityCommandFailure invalid(){return new IdentityCommandFailure(IdentityCommandFailure.Code.SESSION_INVALID);}
    private static void deny(HttpServletResponse response,int status,String code)throws IOException{if(!response.isCommitted())ProductionReadGuard.deny(response,status,code);}
}
