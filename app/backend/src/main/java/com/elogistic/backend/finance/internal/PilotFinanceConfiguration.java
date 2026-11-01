package com.elogistic.backend.finance.internal;
import com.elogistic.backend.finance.api.PilotObligations;
import com.elogistic.backend.finance.internal.infrastructure.JdbcPilotObligations;
import java.time.Clock;
import java.util.Set;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.*;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
@Configuration @Profile("customer")
public class PilotFinanceConfiguration {
    @Bean PilotObligations pilotObligations(DataSource source,Environment env,@Qualifier("identityHttpClock")Clock clock){boolean enabled=Set.of("LOCAL","TEST").contains(env.getProperty("shipping.pilot.environment","DISABLED"))&&"SANDBOX".equals(env.getProperty("customer.rollout"))&&!env.acceptsProfiles(Profiles.of("production","prod"));return new JdbcPilotObligations(source,clock,enabled);}
}
