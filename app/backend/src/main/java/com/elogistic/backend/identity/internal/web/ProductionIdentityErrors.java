package com.elogistic.backend.identity.internal.web;

import com.elogistic.backend.identity.api.IdentityCommandFailure;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.*;

@RestControllerAdvice(assignableTypes=ProductionIdentityReads.class) @Profile("identity")
public final class ProductionIdentityErrors {
    @ExceptionHandler(IdentityCommandFailure.class) public void command(IdentityCommandFailure error,HttpServletResponse response)throws IOException {
        int status=switch(error.code()){case SESSION_INVALID->401;case ACCOUNT_NOT_ACCESSIBLE->404;case AUTHORITY_REQUIRED,SELF_MANAGEMENT_FORBIDDEN->403;case PRECONDITION_STALE->412;case IDEMPOTENCY_CONFLICT,LAST_ADMIN_PROTECTED,INVALID_ACCOUNT_STATE->409;default->400;};
        ProductionReadGuard.deny(response,status,error.code().name());
    }
    // A persistence/unexpected runtime failure cannot establish a trusted response. Never expose its cause.
    @ExceptionHandler(RuntimeException.class) public void unavailable(HttpServletResponse response)throws IOException {ProductionReadGuard.deny(response,401,"SESSION_INVALID");}
    @ExceptionHandler(ProductionIdentityReads.PreconditionFailure.class) public void precondition(ProductionIdentityReads.PreconditionFailure error,HttpServletResponse response)throws IOException {ProductionReadGuard.deny(response,error.status,error.code);}
    @ExceptionHandler({org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class,org.springframework.web.bind.MissingServletRequestParameterException.class})
    public void invalid(HttpServletResponse response)throws IOException {ProductionReadGuard.deny(response,400,"VALIDATION_FAILED");}
}
