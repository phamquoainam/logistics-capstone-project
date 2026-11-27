package com.elogistic.backend.identity.internal;

import com.elogistic.backend.identity.api.staff.*;
import com.elogistic.backend.identity.internal.application.IdentitySessions;
import com.elogistic.backend.identity.internal.application.port.*;
import com.elogistic.backend.identity.internal.infrastructure.*;
import com.elogistic.backend.identity.internal.web.*;
import java.time.Clock;
import java.util.function.BooleanSupplier;
import javax.sql.DataSource;

/** One DataSource and existing Identity algorithms; no operator credential or provisioning port in runtime. */
public final class ProductionOperationsComposition {
    private ProductionOperationsComposition(){}
    public record Services(OperationalStaffAdmission admission,StaffRequestContext requests,ProductionOperationalGuard guard,StaffInvitationDeliveryWorker delivery){}
    public static Services create(DataSource source,ProductionAccessTokens tokens,InvitationProtection protection,
        IdentityRateLimiter limiter,SmtpCustomerDelivery transport,String origin,String environment,Clock clock,BooleanSupplier available){
        var browser=new JdbcIdentityBrowserState(source,IdentitySessionStorage.OPS);
        var sessionAdmission=new ProductionSessionAdmission(browser,limiter,false,"OPS:");
        var sessions=IdentitySessions.staff(new JdbcIdentitySessionStore(source,IdentitySessionStorage.OPS),new ProtectedSessionCredentials(),sessionAdmission,clock);
        var requestAdmission=new ProductionStaffRequestAdmission(browser,origin,true);
        CustomerAccessVerifier verifier=encoded->{var c=tokens.verify(encoded);return new CustomerAccessVerifier.Claim(c.sessionId(),c.accountId(),c.authVersion(),c.issuedAt(),c.expiresAt());};
        var admission=new JdbcOperationalStaffAdmission(source,verifier,requestAdmission,clock);
        var auth=new ProductionAuthenticationHttp(sessions,sessionAdmission,browser,limiter,tokens,origin,clock,ProductionAuthenticationHttp.Transport.OPS);
        var activation=new JdbcStaffInvitationActivation(source,protection,clock,true);
        var outbox=JdbcInvitationOutbox.operations(source,protection,new JdbcInvitationOutbox.Policy(3,java.time.Duration.ofSeconds(30),java.time.Duration.ofSeconds(30)),clock);
        return new Services(admission,requestAdmission,new ProductionOperationalGuard(tokens,requestAdmission,admission,auth,activation,browser,limiter,origin,environment,available),new StaffInvitationDeliveryWorker(outbox,transport,available,true));
    }
}
