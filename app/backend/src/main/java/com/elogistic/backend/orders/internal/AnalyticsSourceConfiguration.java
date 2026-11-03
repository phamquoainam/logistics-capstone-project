package com.elogistic.backend.orders.internal;
import com.elogistic.backend.orders.api.AnalyticsShipments;
import com.elogistic.backend.orders.internal.infrastructure.JdbcAnalyticsShipments;
import javax.sql.DataSource;
import org.springframework.context.annotation.*;
@Configuration @Profile("customer")
public class AnalyticsSourceConfiguration {
    @Bean AnalyticsShipments analyticsShipments(DataSource source){return new JdbcAnalyticsShipments(source);}
}
