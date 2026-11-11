package com.elogistic.backend.notifications.internal;
import com.elogistic.backend.notifications.api.BusinessDashboard;
import com.elogistic.backend.notifications.internal.application.BusinessDashboardService;
import java.time.Clock;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.*;
@Configuration @Profile("customer")
public class BusinessDashboardConfiguration {
    @Bean BusinessDashboard businessDashboard(@Qualifier("identityHttpClock")Clock clock){return new BusinessDashboardService(clock);}
}
