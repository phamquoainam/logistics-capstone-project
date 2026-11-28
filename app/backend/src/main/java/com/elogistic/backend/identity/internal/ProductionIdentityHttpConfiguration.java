package com.elogistic.backend.identity.internal;

import com.elogistic.backend.identity.api.*;
import com.elogistic.backend.identity.internal.infrastructure.*;
import com.elogistic.backend.identity.internal.web.*;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.*;
import java.security.interfaces.*;
import java.time.*;
import java.util.*;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.*;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/** Identity composition root: joins inward ports and both outer adapters without a layer cycle.
 * Explicit identity,postgres profile and externally supplied JWK configuration; never activates in Demo. */
@Configuration @Profile("identity") @Import(ProductionAdminOnboardingConfiguration.class)
public class ProductionIdentityHttpConfiguration {
    public ProductionIdentityHttpConfiguration(Environment environment) {
        var active=Set.of(environment.getActiveProfiles());
        if(!active.contains("postgres") || active.contains("demo") || active.contains("migration"))
            throw new IllegalStateException("IDENTITY_PROFILE_ISOLATION_REQUIRED");
    }
    @Bean("identityHttpClock") Clock identityHttpClock(){return Clock.systemUTC();}
    @Bean public ProductionAccessTokens productionAccessTokens(Environment environment,@Qualifier("identityHttpClock")Clock clock) {
        // JWK set must come from an external protected configtree/secret injection. No file generation,
        // developer credential lookup, default key, outbound JWK fetch or raw exception/logging.
        try {
            String issuer=environment.getRequiredProperty("identity.security.issuer"), audience=environment.getRequiredProperty("identity.security.audience"),
                kid=environment.getRequiredProperty("identity.security.current-kid");
            var set=JWKSet.parse(environment.getRequiredProperty("identity.security.jwk-set"));
            Map<String,RSAPublicKey> publicKeys=new HashMap<>();RSAPrivateKey privateKey=null;
            for(JWK key:set.getKeys()) {
                if(!(key instanceof com.nimbusds.jose.jwk.RSAKey rsa) || !JWSAlgorithm.RS256.equals(key.getAlgorithm())
                    || !KeyUse.SIGNATURE.equals(key.getKeyUse()) || publicKeys.containsKey(key.getKeyID()))throw new IllegalArgumentException();
                publicKeys.put(key.getKeyID(),rsa.toRSAPublicKey());
                if(key.getKeyID().equals(kid)){if(!rsa.isPrivate())throw new IllegalArgumentException();privateKey=rsa.toRSAPrivateKey();}
                else if(rsa.isPrivate())throw new IllegalArgumentException();
            }
            return new ProductionAccessTokens(issuer,audience,kid,privateKey,publicKeys,clock);
        } catch(Exception invalid){throw new IllegalStateException("IDENTITY_JWT_CONFIGURATION_INVALID");}
    }
    @Bean ProductionAccessAdmission productionAccessAdmission(DataSource runtime){return new ProductionAccessAdmission(new JdbcAccessSessionTiming(runtime));}
    @Bean AdminSessionPolicy adminSessionPolicy(DataSource runtime,Environment environment){boolean production=Arrays.stream(environment.getActiveProfiles()).anyMatch(p->Set.of("production","prod").contains(p.toLowerCase(Locale.ROOT)));return new AdminSessionPolicy(runtime,environment.getProperty("identity.admin-dashboard.enabled",Boolean.class,false)&&!production,environment.getProperty("identity.admin-dashboard.environment","DISABLED"));}
    @Bean com.elogistic.backend.identity.api.admin.AdminAdmission adminAdmission(DataSource runtime,ProductionAccessAdmission admission,AdminSessionPolicy policy,@Qualifier("identityHttpClock")Clock clock){return new JdbcAdminAdmission(runtime,admission,admission::currentRequestId,policy,clock);}
    @Bean com.elogistic.backend.identity.api.admin.AdminCommandAdmission adminCommandAdmission(DataSource runtime,ProductionAccessAdmission admission,AdminSessionPolicy policy,@Qualifier("identityHttpClock")Clock clock){return new JdbcAdminCommandAdmission(runtime,admission,policy,clock);}
    @Bean IdentityCurrentSessionCommands productionCurrentSession(DataSource runtime,ProductionAccessAdmission admission,AdminSessionPolicy policy,@Qualifier("identityHttpClock")Clock clock){return new com.elogistic.backend.identity.internal.application.IdentityCurrentSessions(new JdbcIdentitySessionStore(runtime,policy),admission,clock);}
    @Bean IdentityAdministrationCommands productionReadAdministration(DataSource runtime,ProductionAccessAdmission admission,@Qualifier("identityHttpClock")Clock clock){return ProductionIdentityAdministrationComposition.create(runtime,admission,clock,Duration.ofHours(24));}
    @Bean IdentityGrantCommands productionGrantCommands(DataSource runtime,@Qualifier("identityHttpClock")Clock clock){return ProductionIdentityGrantComposition.create(runtime,clock,Duration.ofHours(24));}
    @Bean com.elogistic.backend.identity.internal.application.port.IdentityRateLimiter identityRateLimiter(DataSource runtime,Environment environment){
        byte[] secret=null;
        try{String encoded=environment.getRequiredProperty("identity.security.limiter-hmac");secret=Base64.getDecoder().decode(encoded);if(!Base64.getEncoder().encodeToString(secret).equals(encoded))throw new IllegalArgumentException();return new JdbcIdentityRateLimiter(runtime,secret);}
        catch(Exception invalid){throw new IllegalStateException("IDENTITY_LIMITER_CONFIGURATION_INVALID");}
        finally{if(secret!=null)Arrays.fill(secret,(byte)0);}
    }
    @Bean com.elogistic.backend.identity.internal.application.port.IdentityBrowserState identityBrowserState(DataSource runtime){return new JdbcIdentityBrowserState(runtime);}
    @Bean ProductionSessionAdmission productionSessionAdmission(com.elogistic.backend.identity.internal.application.port.IdentityBrowserState browser,com.elogistic.backend.identity.internal.application.port.IdentityRateLimiter limiter){return new ProductionSessionAdmission(browser,limiter);}
    @Bean IdentitySessionCommands productionSessionCommands(DataSource runtime,ProductionSessionAdmission admission,AdminSessionPolicy policy,@Qualifier("identityHttpClock")Clock clock){return new com.elogistic.backend.identity.internal.application.IdentitySessions(new JdbcIdentitySessionStore(runtime,policy),new ProtectedSessionCredentials(),admission,clock);}
    @Bean ProductionAuthenticationHttp productionAuthenticationHttp(IdentitySessionCommands sessions,ProductionSessionAdmission admission,com.elogistic.backend.identity.internal.application.port.IdentityBrowserState browser,com.elogistic.backend.identity.internal.application.port.IdentityRateLimiter limiter,ProductionAccessTokens tokens,Environment environment,@Qualifier("identityHttpClock")Clock clock){return new ProductionAuthenticationHttp(sessions,admission,browser,limiter,tokens,environment.getRequiredProperty("identity.security.origin"),clock);}
    @Bean @Order(-10) SecurityFilterChain productionIdentityChain(HttpSecurity http,ProductionAccessTokens tokens,ProductionAccessAdmission admission,
            IdentityCurrentSessionCommands sessions,Environment environment,ProductionAuthenticationHttp authentication,ProductionAdminInvitationHttp invitations)throws Exception {
        var guard=new ProductionReadGuard(tokens,admission,sessions,environment.getRequiredProperty("identity.security.origin")).browser(authentication).invitations(invitations);
        http.securityMatcher("/api/v1/**").csrf(c->c.disable()).httpBasic(c->c.disable()).formLogin(c->c.disable())
            .requestCache(c->c.disable()).sessionManagement(c->c.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(c->c.requestMatchers(HttpMethod.GET,"/api/v1/auth/me","/api/v1/admin/users","/api/v1/admin/users/*","/api/v1/admin/roles","/api/v1/admin/scopes","/api/v1/admin/dashboard","/api/v1/admin/dashboard/context","/api/v1/admin/dashboard/shipments","/api/v1/admin/dashboard/merchant-applications","/api/v1/admin/dashboard/network","/api/v1/admin/carrier-sites","/api/v1/admin/carrier-sites/*","/api/v1/admin/dashboard/platform","/api/v1/admin/shipments/*","/api/v1/admin/merchant-applications/*","/api/v1/admin/merchant-applications/*/checks","/api/v1/admin/merchant-applications/*/review-data","/api/v1/admin/merchant-applications/*/revisions","/api/v1/admin/merchant-applications","/api/v1/admin/merchant-governance/context","/api/v1/admin/merchant-governance/reviewer-candidates","/api/v1/admin/merchant-organizations","/api/v1/admin/merchant-organizations/*","/api/v1/admin/merchant-organizations/*/governance-cases","/api/v1/admin/merchant-organizations/*/governance-cases/*","/api/v1/admin/merchant-organizations/*/governance-cases/*/events","/api/v1/admin/merchant-organizations/*/governance-policies","/api/v1/admin/merchant-applications/*/events","/api/v1/admin/merchant-applications/*/evidence/*/content","/api/v1/admin/merchant-governance/receipts/*/*","/api/v1/admin/audit","/api/v1/admin/work-items").authenticated().requestMatchers(HttpMethod.PUT,"/api/v1/admin/users/*/grants").authenticated().requestMatchers(HttpMethod.POST,"/api/v1/admin/users/*/disable","/api/v1/admin/users/*/reactivate","/api/v1/admin/merchant-applications/*/review-assignment","/api/v1/admin/merchant-applications/*/review","/api/v1/admin/merchant-applications/*/request-information","/api/v1/admin/merchant-applications/*/rejection","/api/v1/admin/merchant-applications/*/approval","/api/v1/admin/merchant-organizations/*/suspension","/api/v1/admin/merchant-organizations/*/reinstatement","/api/v1/admin/governance-reauthentications","/api/v1/admin/merchant-applications/search","/api/v1/admin/merchant-organizations/search","/api/v1/admin/merchant-applications/*/revision-comparisons","/api/v1/admin/merchant-applications/*/review-comments","/api/v1/admin/merchant-applications/*/comment-disclosures","/api/v1/admin/merchant-organizations/*/profile-disclosures","/api/v1/admin/merchant-organizations/*/membership-disclosures","/api/v1/admin/merchant-organizations/*/governance-cases","/api/v1/admin/merchant-organizations/*/governance-cases/*/assignment","/api/v1/admin/merchant-organizations/*/governance-cases/*/review","/api/v1/admin/merchant-organizations/*/governance-cases/*/request-information","/api/v1/admin/merchant-organizations/*/governance-cases/*/resubmission","/api/v1/admin/merchant-organizations/*/governance-cases/*/submission","/api/v1/admin/merchant-organizations/*/governance-cases/*/cancellation","/api/v1/admin/merchant-organizations/*/governance-cases/*/disclosures").authenticated().anyRequest().denyAll())
            .exceptionHandling(c->c.authenticationEntryPoint((q,r,e)->ProductionReadGuard.deny(r,401,"SESSION_INVALID"))
                .accessDeniedHandler((q,r,e)->ProductionReadGuard.deny(r,403,"AUTHORITY_REQUIRED")))
            .addFilterBefore(guard,UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
