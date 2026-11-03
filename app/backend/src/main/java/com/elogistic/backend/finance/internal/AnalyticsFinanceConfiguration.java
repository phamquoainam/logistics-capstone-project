package com.elogistic.backend.finance.internal;
import com.elogistic.backend.finance.api.AnalyticsObligations;
import com.elogistic.backend.finance.internal.infrastructure.JdbcAnalyticsObligations;
import javax.sql.DataSource;
import org.springframework.context.annotation.*;
@Configuration @Profile("customer")
public class AnalyticsFinanceConfiguration {
    @Bean AnalyticsObligations analyticsObligations(DataSource source){return new JdbcAnalyticsObligations(source);}
}
