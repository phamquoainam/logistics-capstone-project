package com.elogistic.backend.identity.internal.web;

import com.elogistic.backend.identity.api.IdentityCommandFailure;
import com.elogistic.backend.identity.api.customer.CustomerReauthentication;
import com.elogistic.backend.identity.internal.application.port.IdentityRateLimiter;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.*;
import tools.jackson.core.StreamReadFeature;
import tools.jackson.databind.json.JsonMapper;

/** Invoked only inside the customer guard's same-request admission scope. */
public final class ProductionCustomerReauthenticationHttp {
    private static final JsonMapper JSON=JsonMapper.builder().enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION).build();
    private final CustomerReauthentication commands;
    public ProductionCustomerReauthenticationHttp(CustomerReauthentication commands){this.commands=Objects.requireNonNull(commands);}
    public boolean matches(HttpServletRequest request){return request.getMethod().equals("POST") && request.getRequestURI().substring(request.getContextPath().length()).equals("/api/v1/customer-auth/reauthentication");}
    private static IdentityCommandFailure invalid(){return new IdentityCommandFailure(IdentityCommandFailure.Code.VALIDATION_FAILED);}
    public void handle(HttpServletRequest request,HttpServletResponse response)throws IOException {
        try{
            UUID key;var keys=Collections.list(request.getHeaders("Idempotency-Key"));
            try{if(keys.size()!=1)throw invalid();key=UUID.fromString(keys.getFirst());if(!key.toString().equals(keys.getFirst()))throw invalid();}catch(IllegalArgumentException rejected){throw invalid();}
            if(request.getContentType()==null || !request.getContentType().split(";",2)[0].equalsIgnoreCase("application/json"))throw invalid();
            byte[] bytes=request.getInputStream().readNBytes(16385);CustomerReauthentication.Command command;
            try{
                if(bytes.length>16384)throw invalid();var body=JSON.readTree(bytes);
                if(body==null || !body.isObject() || body.size()!=2 || body.get("email")==null || !body.get("email").isString()
                    || body.get("password")==null || !body.get("password").isString())throw invalid();
                command=new CustomerReauthentication.Command(key,body.get("email").asString(),body.get("password").asString());
            }catch(IdentityCommandFailure rejected){throw rejected;}catch(Exception rejected){throw invalid();}finally{Arrays.fill(bytes,(byte)0);}
            var result=commands.request(command);response.setStatus(200);response.setContentType("application/json;charset=UTF-8");response.getWriter().write(JSON.writeValueAsString(result));
        }catch(IdentityRateLimiter.Exhausted exhausted){response.setHeader("Retry-After",Integer.toString(exhausted.retryAfter));ProductionReadGuard.deny(response,429,"RATE_LIMITED");}
        catch(IdentityCommandFailure rejected){
            int status=switch(rejected.code()){case INVALID_CREDENTIALS,SESSION_INVALID->401;case CSRF_REJECTED,AUTHORITY_REQUIRED->403;case IDEMPOTENCY_CONFLICT->409;case TOKEN_INVALID_OR_EXPIRED->422;default->400;};
            ProductionReadGuard.deny(response,status,rejected.code().name());
        }catch(Exception unavailable){ProductionReadGuard.deny(response,503,"SERVICE_UNAVAILABLE");}
    }
}
