package com.elogistic.backend.identity.internal.web;

import com.elogistic.backend.identity.api.IdentityCommandFailure;
import com.elogistic.backend.identity.api.customer.CustomerOwnerRecoveryConsents;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.*;
import tools.jackson.core.StreamReadFeature;
import tools.jackson.databind.json.JsonMapper;

/** Same-request customer admission is already entered by the actual production guard. */
public final class ProductionCustomerConsentHttp {
    private static final String PATH="/api/v1/customer-auth/owner-recovery-consents";
    private static final JsonMapper JSON=JsonMapper.builder().enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION).build();
    private final CustomerOwnerRecoveryConsents commands;
    public ProductionCustomerConsentHttp(CustomerOwnerRecoveryConsents commands){this.commands=Objects.requireNonNull(commands);}
    public boolean matches(HttpServletRequest request){return request.getMethod().equals("POST") && request.getRequestURI().substring(request.getContextPath().length()).equals(PATH);}
    private static IdentityCommandFailure invalid(){return new IdentityCommandFailure(IdentityCommandFailure.Code.VALIDATION_FAILED);}
    private static UUID uuid(String value){try{var id=UUID.fromString(value);if(!id.toString().equals(value))throw invalid();return id;}catch(IllegalArgumentException rejected){throw invalid();}}
    public void handle(HttpServletRequest request,HttpServletResponse response)throws IOException{
        try{
            var keys=Collections.list(request.getHeaders("Idempotency-Key"));if(keys.size()!=1)throw invalid();UUID key=uuid(keys.getFirst());
            if(request.getContentType()==null || !request.getContentType().split(";",2)[0].equalsIgnoreCase("application/json"))throw invalid();
            byte[] bytes=request.getInputStream().readNBytes(16385);CustomerOwnerRecoveryConsents.Command command;
            try{
                if(bytes.length>16384)throw invalid();var body=JSON.readTree(bytes);
                if(body==null || !body.isObject() || body.size()!=2 || body.get("organizationId")==null || !body.get("organizationId").isString()
                    || body.get("reauthenticationProofId")==null || !body.get("reauthenticationProofId").isString())throw invalid();
                command=new CustomerOwnerRecoveryConsents.Command(key,uuid(body.get("organizationId").asString()),uuid(body.get("reauthenticationProofId").asString()));
            }catch(IdentityCommandFailure rejected){throw rejected;}catch(Exception rejected){throw invalid();}finally{Arrays.fill(bytes,(byte)0);}
            var result=commands.create(command);response.setStatus(201);response.setHeader("Location",PATH+"/"+result.id());response.setContentType("application/json;charset=UTF-8");response.getWriter().write(JSON.writeValueAsString(result));
        }catch(IdentityCommandFailure rejected){
            int status=switch(rejected.code()){case SESSION_INVALID,INVALID_CREDENTIALS->401;case AUTHORITY_REQUIRED,CSRF_REJECTED->403;case IDEMPOTENCY_CONFLICT->409;case TOKEN_INVALID_OR_EXPIRED->422;default->400;};
            ProductionReadGuard.deny(response,status,rejected.code().name());
        }catch(Exception unavailable){ProductionReadGuard.deny(response,503,"SERVICE_UNAVAILABLE");}
    }
}
