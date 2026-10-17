package com.elogistic.backend.platform.internal.infrastructure;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.util.Assert;
import org.springframework.boot.amqp.autoconfigure.RabbitProperties;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;

/** Explicit local foundation opt-in; missing credentials fail before serving traffic. */
@Configuration(proxyBeanMethods = false)
@Profile("foundation")
class FoundationConfiguration {
    @Bean InitializingBean foundationConfigurationValidation(Environment environment) {
        return () -> {
            Assert.state(environment.acceptsProfiles(Profiles.of("demo & postgres")),
                "Foundation requires demo,postgres,foundation profiles");
            for (String name : new String[]{"FOUNDATION_REDIS_USER", "FOUNDATION_REDIS_PASSWORD",
                    "FOUNDATION_RABBIT_USER", "FOUNDATION_RABBIT_PASSWORD"}) {
                String value = environment.getProperty(name);
                Assert.state(value != null && !value.isBlank(), "Missing required foundation credential: " + name);
            }
        };
    }

    /** Native indicator was proven UP on cached metadata while the real broker was paused.
     * This health-only factory never changes the Spring application connection's RPC policy.
     * A fresh authenticated connection and dedicated channel perform a real bounded RPC.
     */
    @Bean("rabbitHealthIndicator")
    HealthIndicator activeRabbitReadiness(RabbitProperties properties) {
        var probe = new com.rabbitmq.client.ConnectionFactory();
        probe.setHost(properties.getHost());
        probe.setPort(properties.getPort());
        probe.setUsername(properties.getUsername());
        probe.setPassword(properties.getPassword());
        probe.setVirtualHost(properties.getVirtualHost());
        probe.setConnectionTimeout(2000);
        probe.setHandshakeTimeout(2000);
        probe.setChannelRpcTimeout(2000);
        probe.setShutdownTimeout(2000);
        probe.setRequestedHeartbeat(10);
        probe.setAutomaticRecoveryEnabled(false);
        probe.setTopologyRecoveryEnabled(false);
        return () -> {
            com.rabbitmq.client.Connection connection = null;
            try {
                connection = probe.newConnection("elogistics-foundation-readiness");
                var channel = connection.createChannel();
                if (channel == null) return Health.down().build();
                channel.basicQos(1); // Synchronous AMQP reply; no queue/message/consumer.
                return Health.up().build();
            } catch (Exception unavailable) {
                return Health.down().build(); // Never attach exception/credential details.
            } finally {
                // Releases all channels and client workers, including failed RPC paths.
                if (connection != null) connection.abort(2000);
            }
        };
    }
}
