package com.elogistic.backend.platform.internal.infrastructure;

import com.elogistic.backend.platform.api.PublicMediaStore;
import java.nio.file.Path;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.core.annotation.Order;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

@Configuration @Profile("customer")
public class PublicMediaConfiguration {
    @Bean PublicMediaStore publicMediaStore(@Value("${public.media.mode:local}")String mode,
        @Value("${public.media.local-root:}")String localRoot,
        @Value("${public.media.s3-region:}")String region,
        @Value("${public.media.s3-bucket:}")String bucket){
        if("local".equals(mode) && !localRoot.isBlank())return new LocalPublicMediaStore(Path.of(localRoot));
        if("s3".equals(mode) && !region.isBlank() && !bucket.isBlank())return new S3PublicMediaStore(S3Client.builder().region(Region.of(region)).build(),bucket);
        throw new IllegalStateException("PUBLIC_MEDIA_CONFIGURATION_INVALID");
    }
    @Bean @Order(-30) SecurityFilterChain publicMediaChain(HttpSecurity http)throws Exception{
        http.securityMatcher("/api/v1/public-media/**").csrf(c->c.disable()).httpBasic(c->c.disable())
            .formLogin(c->c.disable()).sessionManagement(s->s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(a->a.requestMatchers(HttpMethod.GET,"/api/v1/public-media/hero-network.svg").permitAll().anyRequest().denyAll());
        return http.build();
    }
}
