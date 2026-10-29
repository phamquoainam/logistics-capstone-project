package com.elogistic.backend.identity.internal.web;

import com.elogistic.backend.identity.api.*;
import com.elogistic.backend.identity.api.customer.CustomerEnrollmentCommands;
import com.elogistic.backend.identity.api.customer.CustomerRecoveryCommands;
import com.elogistic.backend.identity.internal.application.port.IdentityRateLimiter;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.*;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.core.StreamReadFeature;

/** Exact reviewed public enrollment commands. Response is written only after owned DB commit. */
public final class ProductionCustomerEnrollmentHttp {
    private static final JsonMapper JSON=JsonMapper.builder().enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION).build();
    private static final String PREFIX="/api/v1/customer-auth/";
    private final CustomerEnrollmentCommands commands;private final CustomerRecoveryCommands recoveries;private final ProductionCustomerEnrollmentAdmission admission;private final String origin;
    public ProductionCustomerEnrollmentHttp(CustomerEnrollmentCommands commands,CustomerRecoveryCommands recoveries,ProductionCustomerEnrollmentAdmission admission,String origin){
        this.commands=Objects.requireNonNull(commands);this.recoveries=Objects.requireNonNull(recoveries);this.admission=Objects.requireNonNull(admission);this.origin=ProductionCustomerRequestAdmission.requireOrigin(origin);
    }
    public boolean matches(HttpServletRequest request){String path=request.getRequestURI().substring(request.getContextPath().length());return request.getMethod().equals("POST") && Set.of(PREFIX+"enrollments",PREFIX+"verification-resend",PREFIX+"verifications",PREFIX+"recovery",PREFIX+"recovery-confirm").contains(path);}
    private static IdentityCommandFailure invalid(){return new IdentityCommandFailure(IdentityCommandFailure.Code.VALIDATION_FAILED);}
    private static JsonNode body(HttpServletRequest request,Set<String> fields)throws IOException {return body(request,fields,Set.of());}
    private static JsonNode body(HttpServletRequest request,Set<String> fields,Set<String> optional)throws IOException {
        try{
            if(request.getContentType()==null || !request.getContentType().split(";",2)[0].equalsIgnoreCase("application/json"))throw invalid();
            byte[] bytes=request.getInputStream().readNBytes(16385);try{
                if(bytes.length>16384)throw invalid();JsonNode value=JSON.readTree(bytes);
                if(value==null || !value.isObject() || value.size()<fields.size() || value.size()>fields.size()+optional.size())throw invalid();
                for(String field:fields)if(value.get(field)==null || !value.get(field).isString())throw invalid();
                for(var entry:value.properties())if(!fields.contains(entry.getKey()) && !optional.contains(entry.getKey()) || !entry.getValue().isString())throw invalid();
                return value;
            }finally{Arrays.fill(bytes,(byte)0);}
        }catch(IdentityCommandFailure rejected){throw rejected;}catch(Exception rejected){throw invalid();}
    }
    private static UUID key(HttpServletRequest request){
        var values=Collections.list(request.getHeaders("Idempotency-Key"));
        try{if(values.size()!=1)throw invalid();UUID id=UUID.fromString(values.getFirst());if(!id.toString().equals(values.getFirst()))throw invalid();return id;}
        catch(IllegalArgumentException invalid){throw invalid();}
    }
    public void handle(HttpServletRequest request,HttpServletResponse response)throws IOException {
        try{
            var origins=Collections.list(request.getHeaders("Origin"));
            if(!request.isSecure() || origins.size()!=1 || !origin.equals(origins.getFirst())){ProductionReadGuard.deny(response,403,"ORIGIN_REJECTED");return;}
            UUID requestId=UUID.fromString(response.getHeader("X-Request-Id")),key=key(request);
            String cookie=ProductionAuthenticationHttp.cookie(request,"__Host-elogistics.customer.csrf"),challenge=ProductionAuthenticationHttp.single(request,"X-CSRF-Token");
            try(var scope=admission.enter(requestId,cookie,challenge,ProductionAuthenticationHttp.clientAddress(request))){
                String path=request.getRequestURI();CustomerEnrollmentCommands.Accepted result;
                if(path.endsWith("/enrollments")){
                    var input=body(request,Set.of("email","password","displayName","consentVersion"),Set.of("phone"));
                    result=commands.enroll(new CustomerEnrollmentCommands.Enrollment(input.get("email").asString(),input.get("password").asString(),input.get("displayName").asString(),input.has("phone")?input.get("phone").asString():null,input.get("consentVersion").asString(),key,requestId));
                }else if(path.endsWith("/verification-resend")){
                    var input=body(request,Set.of("email"));result=commands.resend(new CustomerEnrollmentCommands.Email(input.get("email").asString(),key,requestId));
                }else if(path.endsWith("/recovery")){
                    var input=body(request,Set.of("email"));result=recoveries.request(new CustomerRecoveryCommands.Email(input.get("email").asString(),key,requestId));
                }else{
                    boolean recovery=path.endsWith("/recovery-confirm");var input=body(request,recovery?Set.of("proofId","proof","newPassword"):Set.of("proofId","proof"));UUID proof;
                    try{proof=UUID.fromString(input.get("proofId").asString());if(!proof.toString().equals(input.get("proofId").asString()))throw invalid();}
                    catch(IllegalArgumentException invalid){throw invalid();}
                    if(recovery){
                        recoveries.confirm(new CustomerRecoveryCommands.Confirmation(proof,input.get("proof").asString(),input.get("newPassword").asString(),key,requestId));
                        response.addHeader("Set-Cookie","__Host-elogistics.customer.refresh=; Path=/; Secure; HttpOnly; SameSite=Strict; Max-Age=0");
                    }else commands.verify(new CustomerEnrollmentCommands.Verification(proof,input.get("proof").asString(),key,requestId));response.setStatus(204);return;
                }
                response.setStatus(202);response.setContentType("application/json;charset=UTF-8");response.getWriter().write(JSON.writeValueAsString(result));
            }
        }catch(IdentityRateLimiter.Exhausted exhausted){response.setHeader("Retry-After",Integer.toString(exhausted.retryAfter));ProductionReadGuard.deny(response,429,"RATE_LIMITED");}
        catch(IdentityCommandFailure rejected){
            int status=switch(rejected.code()){case CSRF_REJECTED->403;case IDEMPOTENCY_CONFLICT->409;case TOKEN_INVALID_OR_EXPIRED->422;default->400;};
            ProductionReadGuard.deny(response,status,rejected.code().name());
        }catch(Exception unavailable){ProductionReadGuard.deny(response,503,"SERVICE_UNAVAILABLE");}
    }
}
