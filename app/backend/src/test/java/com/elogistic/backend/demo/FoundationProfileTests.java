package com.elogistic.backend.demo;

import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import static org.junit.jupiter.api.Assertions.*;

/** Runs in both existing H2 and real PostgreSQL regression modes. */
class FoundationProfileTests extends DemoTestSupport {
    @Autowired ApplicationContext context;

    @Test void existingDemoHasNoRedisOrRabbitClientsOrContributors() {
        assertTrue(context.getBeansOfType(RedisConnectionFactory.class).isEmpty());
        assertTrue(context.getBeansOfType(ConnectionFactory.class).isEmpty());
        assertFalse(context.containsBean("redisHealthContributor"));
        assertFalse(context.containsBean("rabbitHealthContributor"));
        assertFalse(context.containsBean("rabbitHealthIndicator"));
    }
}
