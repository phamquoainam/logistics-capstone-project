package com.elogistic.backend.orders.internal;
import com.elogistic.backend.orders.api.PilotShipments;
import com.elogistic.backend.orders.internal.application.PilotBookingApplication;
import com.elogistic.backend.orders.internal.infrastructure.*;
import java.time.Clock;
import java.util.Set;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.*;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
@Configuration @Profile("customer")
public class PilotBookingConfiguration {
    @Bean com.elogistic.backend.orders.api.PersonalShipmentManagement personalShipmentManagement(DataSource source,Environment env,@Qualifier("identityHttpClock")Clock clock){boolean enabled=Set.of("LOCAL","TEST").contains(env.getProperty("shipping.pilot.environment","DISABLED"))&&"SANDBOX".equals(env.getProperty("customer.rollout"))&&!env.acceptsProfiles(Profiles.of("production","prod"));return new JdbcPersonalShipmentManagement(source,new JdbcPilotShipmentStore(source),clock,enabled);}
    @Bean com.elogistic.backend.orders.api.PilotHandling pilotHandling(DataSource source,Environment env){String mode=env.getProperty("shipping.pilot.environment","DISABLED");boolean enabled=Set.of("LOCAL","TEST").contains(mode)&&"SANDBOX".equals(env.getProperty("customer.rollout"))&&!env.acceptsProfiles(Profiles.of("production","prod"));return new JdbcPilotHandling(source,enabled,mode);}
    @Bean PilotShipments pilotShipments(DataSource source,Environment env,@Qualifier("identityHttpClock")Clock clock){boolean enabled=Set.of("LOCAL","TEST").contains(env.getProperty("shipping.pilot.environment","DISABLED"))&&"SANDBOX".equals(env.getProperty("customer.rollout"))&&!env.acceptsProfiles(Profiles.of("production","prod"));return new PilotBookingApplication(new JdbcShippingDraftStore(source),new JdbcPilotShipmentStore(source),clock,enabled);}
}
