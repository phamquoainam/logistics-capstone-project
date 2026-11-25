package com.elogistic.backend.identity.internal.web;

import com.elogistic.backend.identity.api.IdentityCommandFailure;
import com.elogistic.backend.identity.internal.application.port.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.BooleanSupplier;
import tools.jackson.core.StreamReadFeature;
import tools.jackson.databind.json.JsonMapper;

/** One verification-only SYSTEM preauth endpoint. No public provisioning or automatic session. */
public final class ProductionAdminInvitationHttp {
    private static final JsonMapper JSON=JsonMapper.builder().enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION).build();
    private final LocalAdminInvitationActivation activation;private final IdentityBrowserState browser;private final IdentityRateLimiter limiter;private final String origin;private final BooleanSupplier available;
    private ProductionAdminInvitationHttp(){activation=null;browser=null;limiter=null;origin=null;available=()->false;}
    public static ProductionAdminInvitationHttp disabled(){return new ProductionAdminInvitationHttp();}
    public ProductionAdminInvitationHttp(LocalAdminInvitationActivation activation,IdentityBrowserState browser,IdentityRateLimiter limiter,String origin,BooleanSupplier available){this.activation=Objects.requireNonNull(activation);this.browser=Objects.requireNonNull(browser);this.limiter=Objects.requireNonNull(limiter);this.origin=ProductionCustomerRequestAdmission.requireOrigin(origin);this.available=Objects.requireNonNull(available);}
    public boolean matches(HttpServletRequest request){return request.getRequestURI().substring(request.getContextPath().length()).equals("/api/v1/auth/admin-invitations/accept");}
    public void handle(HttpServletRequest request,HttpServletResponse response,UUID requestId)throws IOException{
        byte[] body=null,token=null;
        try{
            if(!available.getAsBoolean()){deny(response,503,"SERVICE_UNAVAILABLE");return;}
            var origins=Collections.list(request.getHeaders("Origin"));
            if(!request.isSecure()||origins.size()!=1||!origin.equals(origins.getFirst())){deny(response,403,"ORIGIN_REJECTED");return;}
            if(!request.getMethod().equals("POST")||!request.getParameterMap().isEmpty()||request.getQueryString()!=null||request.getHeader("Authorization")!=null)throw validation();
            if(request.getCookies()!=null)for(var cookie:request.getCookies())if(cookie.getName().startsWith("__Host-argopost.staff.")||cookie.getName().startsWith("__Host-elogistics.customer."))throw csrf();
            browser.verify(ProductionAuthenticationHttp.cookie(request,"__Host-elogistics.csrf"),ProductionAuthenticationHttp.single(request,"X-CSRF-Token"),null,true);
            UUID key=uuid(ProductionAuthenticationHttp.single(request,"Idempotency-Key"));
            if(Collections.list(request.getHeaders("Content-Type")).size()!=1||request.getContentType()==null||!request.getContentType().split(";",2)[0].equalsIgnoreCase("application/json"))throw validation();
            body=request.getInputStream().readNBytes(16385);if(body.length>16384)throw validation();tools.jackson.databind.JsonNode input;
            try{input=JSON.readTree(body);}catch(RuntimeException malformed){throw validation();}
            if(input==null||!input.isObject()||!input.propertyNames().equals(Set.of("proofId","token","password"))||!input.get("proofId").isString()||!input.get("token").isString()||!input.get("password").isString())throw validation();
            UUID proof=uuid(input.get("proofId").asString());String encoded=input.get("token").asString();
            if(!encoded.matches("[A-Za-z0-9_-]{43}"))throw validation();token=encoded.getBytes(StandardCharsets.US_ASCII);
            byte[] decoded=Base64.getUrlDecoder().decode(token);try{if(decoded.length!=32||!Arrays.equals(token,Base64.getUrlEncoder().withoutPadding().encode(decoded)))throw validation();}finally{Arrays.fill(decoded,(byte)0);}
            limiter.reserve(List.of(new IdentityRateLimiter.Subject(IdentityRateLimiter.Policy.INVITATION_TOKEN,"ADMIN:"+proof),new IdentityRateLimiter.Subject(IdentityRateLimiter.Policy.RESEND_IP,"ADMIN:ACTIVATE:"+ProductionAuthenticationHttp.clientAddress(request))));
            activation.accept(proof,token,input.get("password").asString(),key,requestId);response.setStatus(204);
        }catch(IdentityRateLimiter.Exhausted exhausted){response.setHeader("Retry-After",Integer.toString(exhausted.retryAfter));deny(response,429,"RATE_LIMITED");}
        catch(IdentityCommandFailure rejected){String code=rejected.code().name();int status=switch(code){case "CSRF_REJECTED","ORIGIN_REJECTED"->403;case "IDEMPOTENCY_CONFLICT"->409;default->400;};deny(response,status,Set.of("CSRF_REJECTED","ORIGIN_REJECTED","IDEMPOTENCY_CONFLICT","VALIDATION_FAILED").contains(code)?code:"TOKEN_INVALID_OR_EXPIRED");}
        catch(IllegalArgumentException malformed){deny(response,400,"VALIDATION_FAILED");}
        catch(Exception unavailable){deny(response,503,"SERVICE_UNAVAILABLE");}
        finally{if(body!=null)Arrays.fill(body,(byte)0);if(token!=null)Arrays.fill(token,(byte)0);}
    }
    private static UUID uuid(String value){try{UUID id=UUID.fromString(value);if(!id.toString().equals(value)||id.equals(new UUID(0,0)))throw validation();return id;}catch(IllegalArgumentException malformed){throw validation();}}
    private static IdentityCommandFailure validation(){return new IdentityCommandFailure(IdentityCommandFailure.Code.VALIDATION_FAILED);}
    private static IdentityCommandFailure csrf(){return new IdentityCommandFailure(IdentityCommandFailure.Code.CSRF_REJECTED);}
    private static void deny(HttpServletResponse response,int status,String code)throws IOException{if(!response.isCommitted())ProductionReadGuard.deny(response,status,code);}
}
