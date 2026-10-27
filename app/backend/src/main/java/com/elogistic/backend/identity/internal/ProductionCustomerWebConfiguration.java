package com.elogistic.backend.identity.internal;

import com.elogistic.backend.identity.internal.web.ProductionReadGuard;
import java.util.Set;
import org.springframework.context.annotation.*;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/** Actual customer Spring Security chain, separate from the accepted SYSTEM chain. */
@Configuration @Profile("customer")
public class ProductionCustomerWebConfiguration {
    public ProductionCustomerWebConfiguration(Environment environment){
        var profiles=Set.of(environment.getActiveProfiles());
        if(!profiles.containsAll(Set.of("postgres","identity")) || profiles.contains("demo") || profiles.contains("migration")
            || !"SANDBOX".equals(environment.getRequiredProperty("customer.rollout"))
            || !"NONE".equalsIgnoreCase(environment.getProperty("server.forward-headers-strategy","NONE")))throw new IllegalStateException("CUSTOMER_SANDBOX_PROFILE_ISOLATION_REQUIRED");
    }
    @Bean @Order(-20) SecurityFilterChain customerChain(HttpSecurity http,ProductionCustomerComposition.Services services)throws Exception{
        http.securityMatcher("/api/v1/customer-auth/**","/api/v1/customer/**","/api/v1/merchant-applications/**","/api/v1/merchant-organizations/**","/api/v1/merchant-invitations/**","/api/v1/business-review/**","/api/v1/onboarding-receipts/**")
            .csrf(c->c.disable()).httpBasic(c->c.disable()).formLogin(c->c.disable()).requestCache(c->c.disable()).sessionManagement(c->c.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(c->c.anyRequest().authenticated()).exceptionHandling(c->c.authenticationEntryPoint((q,r,e)->ProductionReadGuard.deny(r,401,"SESSION_INVALID"))
                .accessDeniedHandler((q,r,e)->ProductionReadGuard.deny(r,403,"AUTHORITY_REQUIRED")))
            .addFilterBefore(services.guard(),UsernamePasswordAuthenticationFilter.class);return http.build();
    }
}
