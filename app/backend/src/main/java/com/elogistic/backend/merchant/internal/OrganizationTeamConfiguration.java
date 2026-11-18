package com.elogistic.backend.merchant.internal;
import com.elogistic.backend.merchant.api.customer.OrganizationTeam;
import com.elogistic.backend.merchant.internal.infrastructure.JdbcOrganizationTeam;
import javax.sql.DataSource;
import java.util.Set;
import org.springframework.context.annotation.*;
import org.springframework.core.env.*;
@Configuration @Profile("customer")
public class OrganizationTeamConfiguration {
    @Bean OrganizationTeam organizationTeam(DataSource source,Environment env,com.elogistic.backend.merchant.internal.application.port.MerchantFingerprints fingerprints,@org.springframework.beans.factory.annotation.Qualifier("identityHttpClock")java.time.Clock clock){
        boolean enabled=env.getProperty("business.team.enabled",Boolean.class,false)
          &&env.getProperty("business.workspace.enabled",Boolean.class,false)
          &&Set.of("LOCAL","TEST").contains(env.getProperty("business.workspace.environment","DISABLED"))
          &&"SANDBOX".equals(env.getProperty("customer.rollout"))&&!env.acceptsProfiles(Profiles.of("production","prod"));
        return new JdbcOrganizationTeam(source,enabled,fingerprints,clock);
    }
}
