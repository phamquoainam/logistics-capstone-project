package com.elogistic.backend.identity.internal;

import com.elogistic.backend.identity.api.customer.*;
import com.elogistic.backend.identity.internal.application.*;
import com.elogistic.backend.identity.internal.application.port.*;
import com.elogistic.backend.identity.internal.infrastructure.*;
import com.elogistic.backend.identity.internal.web.*;
import java.time.Clock;
import java.util.Objects;
import javax.sql.DataSource;

/** One real runtime DataSource for customer session/admission/enrollment/delivery; no default authority ports. */
public final class ProductionCustomerComposition {
    private ProductionCustomerComposition(){}
    public record Services(CustomerAdmission customers,CustomerRequestContext requests,CustomerEnrollmentCommands enrollments,CustomerRecoveryCommands recoveries,CustomerReauthentication reauthentication,CustomerOwnerRecoveryConsents consents,
                           CustomerDeliveryWorker delivery,ProductionCustomerGuard guard){}
    public static Services create(DataSource source,ProductionAccessTokens tokens,CustomerProofProtection protection,
                                  IdentityRateLimiter limiter,CustomerDeliveryTransport transport,String origin,Clock clock){
        Objects.requireNonNull(source);Objects.requireNonNull(tokens);Objects.requireNonNull(protection);Objects.requireNonNull(limiter);Objects.requireNonNull(transport);Objects.requireNonNull(clock);
        var browser=new JdbcIdentityBrowserState(source,IdentitySessionStorage.CUSTOMER);
        var sessionAdmission=new ProductionSessionAdmission(browser,limiter,true);
        var sessions=IdentitySessions.customer(new JdbcIdentitySessionStore(source,IdentitySessionStorage.CUSTOMER),new ProtectedSessionCredentials(),sessionAdmission,clock);
        var enrollmentAdmission=new ProductionCustomerEnrollmentAdmission(browser,limiter);
        var enrollments=new CustomerEnrollments(new JdbcCustomerEnrollmentStore(source),protection,enrollmentAdmission,clock);
        var recoveries=new CustomerRecoveries(new JdbcCustomerRecoveryStore(source,protection),protection,enrollmentAdmission,clock);
        var requestAdmission=new ProductionCustomerRequestAdmission(browser,origin);
        CustomerAccessVerifier verifier=encoded->{var claim=tokens.verify(encoded);return new CustomerAccessVerifier.Claim(claim.sessionId(),claim.accountId(),claim.authVersion(),claim.issuedAt(),claim.expiresAt());};
        var customers=new CustomerAdmissions(new JdbcIdentitySessionStore(source,IdentitySessionStorage.CUSTOMER),verifier,requestAdmission,clock);
        var reauthentication=new CustomerReauthentications(customers,requestAdmission,email->requestAdmission.reservePassword(limiter,email),new JdbcCustomerReauthenticationStore(source),protection,new ProtectedSessionCredentials(),clock);
        var consents=new CustomerOwnerConsents(customers,requestAdmission,reauthentication,new JdbcCustomerOwnerConsentStore(source),protection,clock);
        var authentication=new ProductionAuthenticationHttp(sessions,sessionAdmission,browser,limiter,tokens,origin,clock,ProductionAuthenticationHttp.Transport.CUSTOMER);
        var enrollmentHttp=new ProductionCustomerEnrollmentHttp(enrollments,recoveries,enrollmentAdmission,origin);
        var worker=new CustomerDeliveryWorker(new JdbcCustomerDeliveryStore(source),protection,transport,clock);
        return new Services(customers,requestAdmission,enrollments,recoveries,reauthentication,consents,worker,new ProductionCustomerGuard(tokens,requestAdmission,customers,authentication,enrollmentHttp,new ProductionCustomerReauthenticationHttp(reauthentication),new ProductionCustomerConsentHttp(consents)));
    }
}
