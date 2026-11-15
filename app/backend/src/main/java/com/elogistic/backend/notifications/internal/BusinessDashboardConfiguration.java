package com.elogistic.backend.notifications.internal;
import com.elogistic.backend.notifications.api.BusinessDashboard;
import com.elogistic.backend.notifications.internal.application.BusinessDashboardService;
import java.time.Clock;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.*;
@Configuration @Profile("customer")
public class BusinessDashboardConfiguration {
    @Bean com.elogistic.backend.notifications.api.OrganizationActionNotices organizationActionNotices(javax.sql.DataSource source,@org.springframework.beans.factory.annotation.Qualifier("identityHttpClock")java.time.Clock clock){return new com.elogistic.backend.notifications.internal.infrastructure.JdbcOrganizationActionNotices(source,clock);}

    @Bean com.elogistic.backend.notifications.api.BusinessReporting businessReporting(){return new com.elogistic.backend.notifications.internal.application.BusinessReportingService();}
    @Bean BusinessDashboard businessDashboard(@Qualifier("identityHttpClock")Clock clock){return new BusinessDashboardService(clock);}
}
