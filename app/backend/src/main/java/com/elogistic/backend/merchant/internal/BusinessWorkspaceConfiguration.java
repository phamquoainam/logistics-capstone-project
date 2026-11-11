package com.elogistic.backend.merchant.internal;

import com.elogistic.backend.merchant.api.customer.BusinessWorkspaces;
import com.elogistic.backend.merchant.internal.infrastructure.JdbcBusinessWorkspaces;
import javax.sql.DataSource;
import java.util.Set;
import org.springframework.context.annotation.*;
import org.springframework.core.env.*;

@Configuration @Profile("customer")
public class BusinessWorkspaceConfiguration {
    @Bean BusinessWorkspaces businessWorkspaces(DataSource source,Environment env){
        String mode=env.getProperty("business.workspace.environment","DISABLED");
        boolean enabled=env.getProperty("business.workspace.enabled",Boolean.class,false)&&Set.of("LOCAL","TEST").contains(mode)
            &&"SANDBOX".equals(env.getProperty("customer.rollout"))&&!env.acceptsProfiles(Profiles.of("production","prod"));
        if(enabled)JdbcBusinessWorkspaces.requireSchema(source);
        return new JdbcBusinessWorkspaces(source,enabled,mode);
    }
}
