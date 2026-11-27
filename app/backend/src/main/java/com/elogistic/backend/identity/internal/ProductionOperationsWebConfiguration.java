package com.elogistic.backend.identity.internal;
import com.elogistic.backend.identity.internal.web.ProductionReadGuard;
import org.springframework.context.annotation.*;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
/** Exact Staff matchers precede Customer, including compatibility for existing Personal CSKH. */
@Configuration @Profile("customer")
public class ProductionOperationsWebConfiguration {
    @Bean @Order(-31) SecurityFilterChain operationsChain(HttpSecurity http,ProductionOperationsComposition.Services services)throws Exception{
        http.securityMatcher("/api/v1/ops-auth/**","/api/v1/ops/**")
            .csrf(c->c.disable()).httpBasic(c->c.disable()).formLogin(c->c.disable()).requestCache(c->c.disable())
            .sessionManagement(c->c.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(c->c.anyRequest().authenticated())
            .exceptionHandling(c->c.authenticationEntryPoint((q,r,e)->ProductionReadGuard.deny(r,401,"SESSION_INVALID"))
                .accessDeniedHandler((q,r,e)->ProductionReadGuard.deny(r,403,"AUTHORITY_REQUIRED")))
            .addFilterBefore(services.guard(),UsernamePasswordAuthenticationFilter.class);return http.build();
    }
}
