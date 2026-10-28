package com.elogistic.backend.workflow.internal.web;

import com.elogistic.backend.identity.api.IdentityCommandFailure;
import com.elogistic.backend.merchant.api.OnboardingFailure;
import jakarta.servlet.http.HttpServletResponse;
import java.util.*;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestControllerAdvice(basePackages="com.elogistic.backend.workflow.internal.web") @Profile("customer")
public final class OnboardingHttpErrors {
    private ResponseEntity<Map<String,String>> rejected(HttpServletResponse response,int status,String code){
        String request=response.getHeader("X-Request-Id");if(request==null)request=UUID.randomUUID().toString();
        return ResponseEntity.status(status).header("Cache-Control","no-store").body(Map.of("code",code,"message","Request rejected.","requestId",request));
    }
    @ExceptionHandler(OnboardingFailure.class) public ResponseEntity<Map<String,String>> business(OnboardingFailure failure,HttpServletResponse response){
        int status=switch(failure.code()){case VALIDATION_FAILED->422;case IDEMPOTENCY_CONFLICT,BUSINESS_CONFLICT->409;case PRECONDITION_STALE->412;case RESOURCE_NOT_FOUND->404;};return rejected(response,status,failure.code().name());
    }
    @ExceptionHandler(CustomerProfileHttp.HttpProblem.class) public ResponseEntity<Map<String,String>> malformed(CustomerProfileHttp.HttpProblem failure,HttpServletResponse response){return rejected(response,failure.status,failure.code);}
    @ExceptionHandler(IdentityCommandFailure.class) public ResponseEntity<Map<String,String>> session(IdentityCommandFailure failure,HttpServletResponse response){return rejected(response,401,"SESSION_INVALID");}
    @ExceptionHandler(Exception.class) public ResponseEntity<Map<String,String>> unavailable(Exception failure,HttpServletResponse response){return rejected(response,503,"SERVICE_UNAVAILABLE");}
}
