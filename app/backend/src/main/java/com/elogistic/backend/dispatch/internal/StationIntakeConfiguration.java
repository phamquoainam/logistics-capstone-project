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
    @Bean com.elogistic.backend.dispatch.api.OperationalStationIntake operationalStationIntake(StationIntake intake){return (com.elogistic.backend.dispatch.api.OperationalStationIntake)intake;}
    @Bean @Primary StationIntake stationIntake(DataSource source,Environment env,@Qualifier("identityHttpClock")Clock clock,org.springframework.beans.factory.ObjectProvider<com.elogistic.backend.identity.api.staff.OperationalStaffAdmission> operations,com.elogistic.backend.orders.api.PilotHandling handling){String mode=env.getProperty("shipping.pilot.environment","DISABLED");boolean enabled=Set.of("LOCAL","TEST").contains(mode)&&"SANDBOX".equals(env.getProperty("customer.rollout"))&&!env.acceptsProfiles(Profiles.of("production","prod"));return new JdbcStationIntake(source,clock,enabled,mode,operations.getIfAvailable(),handling,enabled&&Boolean.parseBoolean(env.getProperty("shipping.pilot.business-intake-enabled","false")));}
}
