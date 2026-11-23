package com.elogistic.backend.notifications.internal;

import com.elogistic.backend.notifications.api.AdminDashboardReads;
import com.elogistic.backend.notifications.internal.application.AdminDashboardService;
import java.util.*;
import org.springframework.context.annotation.*;
import org.springframework.core.env.Environment;

@Configuration @Profile("identity")
public class AdminDashboardConfiguration {
    @Bean AdminDashboardReads adminDashboardReads(Environment environment){
        byte[] secret=null;
        try{secret=Base64.getDecoder().decode(environment.getRequiredProperty("identity.security.limiter-hmac"));return new AdminDashboardService(secret);}
        catch(Exception bad){throw new IllegalStateException("ADMIN_SNAPSHOT_CONFIGURATION_INVALID");}
        finally{if(secret!=null)Arrays.fill(secret,(byte)0);}
    }
}
