package com.elogistic.backend.dispatch.internal;
import com.elogistic.backend.dispatch.api.StationIntake;
import com.elogistic.backend.dispatch.internal.infrastructure.JdbcStationIntake;
import java.time.Clock;
import java.util.Set;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.*;
import org.springframework.core.env.*;
@Configuration @Profile("customer")
public class StationIntakeConfiguration {
    @Bean StationIntake stationIntake(DataSource source,Environment env,@Qualifier("identityHttpClock")Clock clock){String mode=env.getProperty("shipping.pilot.environment","DISABLED");boolean enabled=Set.of("LOCAL","TEST").contains(mode)&&"SANDBOX".equals(env.getProperty("customer.rollout"))&&!env.acceptsProfiles(Profiles.of("production","prod"));return new JdbcStationIntake(source,clock,enabled,mode);}
}
