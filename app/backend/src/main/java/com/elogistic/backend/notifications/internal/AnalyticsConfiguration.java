package com.elogistic.backend.notifications.internal;
import com.elogistic.backend.notifications.api.PersonalAnalytics;
import com.elogistic.backend.notifications.internal.application.PersonalAnalyticsService;
import com.elogistic.backend.orders.api.AnalyticsShipments;
import com.elogistic.backend.finance.api.AnalyticsObligations;
import java.time.Clock;
import java.util.Set;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.*;
import org.springframework.core.env.*;
@Configuration @Profile("customer")
public class AnalyticsConfiguration {
    @Bean PersonalAnalytics personalAnalytics(AnalyticsShipments shipments,AnalyticsObligations obligations,@Qualifier("identityHttpClock")Clock clock,Environment environment){
        boolean enabled=Set.of("LOCAL","TEST").contains(environment.getProperty("shipping.pilot.environment","DISABLED"))&&"SANDBOX".equals(environment.getProperty("customer.rollout"))&&!environment.acceptsProfiles(Profiles.of("production","prod"));
        return new PersonalAnalyticsService(shipments,obligations,clock,enabled);
    }
}
