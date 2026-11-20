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
public final class ProductionStaffComposition {
    private ProductionStaffComposition(){}
    public record Services(StaffAdmission admission,StaffRequestContext requests,ProductionStaffGuard guard,StaffInvitationDeliveryWorker delivery){}
    public static Services create(DataSource source,ProductionAccessTokens tokens,InvitationProtection protection,
        IdentityRateLimiter limiter,SmtpCustomerDelivery transport,String origin,String environment,Clock clock,BooleanSupplier available){
        var browser=new JdbcIdentityBrowserState(source,IdentitySessionStorage.STAFF);
        var sessionAdmission=new ProductionSessionAdmission(browser,limiter,false,"STAFF:");
        var sessions=IdentitySessions.staff(new JdbcIdentitySessionStore(source,IdentitySessionStorage.STAFF),new ProtectedSessionCredentials(),sessionAdmission,clock);
        var requestAdmission=new ProductionStaffRequestAdmission(browser,origin);
        CustomerAccessVerifier verifier=encoded->{var c=tokens.verify(encoded);return new CustomerAccessVerifier.Claim(c.sessionId(),c.accountId(),c.authVersion(),c.issuedAt(),c.expiresAt());};
        var admission=new JdbcStaffAdmission(source,verifier,requestAdmission,clock);
        var auth=new ProductionAuthenticationHttp(sessions,sessionAdmission,browser,limiter,tokens,origin,clock,ProductionAuthenticationHttp.Transport.STAFF);
        var activation=new JdbcStaffInvitationActivation(source,protection,clock);
        var outbox=new JdbcInvitationOutbox(source,protection,new JdbcInvitationOutbox.Policy(3,java.time.Duration.ofSeconds(30),java.time.Duration.ofSeconds(30)),clock,com.elogistic.backend.identity.internal.domain.ActionProof.Purpose.STAFF_INV);
        return new Services(admission,requestAdmission,new ProductionStaffGuard(tokens,requestAdmission,admission,auth,activation,browser,limiter,origin,environment,available),new StaffInvitationDeliveryWorker(outbox,transport,available));
    }
}
