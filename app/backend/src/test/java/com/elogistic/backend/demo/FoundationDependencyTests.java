package com.elogistic.backend.demo;

import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.amqp.health.RabbitHealthIndicator;
import org.springframework.boot.health.contributor.Status;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.boot.amqp.autoconfigure.RabbitProperties;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.amqp.core.AmqpAdmin;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import org.springframework.context.ApplicationContext;

/** Explicit real-infrastructure suite; standard H2/PG runs report named skips. */
@EnabledIfSystemProperty(named="demo.test.foundation", matches="true")
@ActiveProfiles({"demo", "postgres", "foundation"})
class FoundationDependencyTests extends DemoTestSupport {
    @Autowired RabbitTemplate rabbitTemplate;
    @Autowired RedisConnectionFactory redis;
    @Autowired RabbitProperties rabbit;
    @Autowired ApplicationContext context;
    @Autowired @Qualifier("rabbitHealthIndicator") HealthIndicator activeRabbit;

    static String docker(String... args) throws Exception {
        String executable = System.getProperty("os.name").startsWith("Windows")
            ? Path.of(System.getenv("LOCALAPPDATA"), "Programs/DockerDesktop/resources/bin/docker.exe").toString() : "docker";
        var command = new ArrayList<>(List.of(executable)); command.addAll(List.of(args));
        var process = new ProcessBuilder(command).redirectError(ProcessBuilder.Redirect.DISCARD).start();
        if (!process.waitFor(20, TimeUnit.SECONDS)) {
            process.destroyForcibly(); process.waitFor(5, TimeUnit.SECONDS);
            fail("Owned Docker operation timed out");
        }
        assertEquals(0, process.exitValue(), "Owned Docker operation failed");
        return new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
    }

    static String owned(String service) throws Exception {
        String id = requiredEnv("FOUNDATION_TEST_" + service.toUpperCase() + "_ID");
        assertTrue(id.matches("[a-f0-9]{64}"));
        assertEquals(id, docker("inspect", "--format", "{{.Id}}", id));
        JsonNode labels = new ObjectMapper().readTree(docker("inspect", "--format", "{{json .Config.Labels}}", id));
        assertEquals(requiredEnv("FOUNDATION_RESOURCE_ID"),
            labels.path("com.elogistic.foundation.owner").asText());
        assertEquals(service, labels.path("com.docker.compose.service").asText());
        return id;
    }

    @Test void cachedNativeRabbitProbeNeedsActiveCommunication() throws Exception {
        var nativeProbe = new RabbitHealthIndicator(rabbitTemplate);
        assertEquals(Status.UP, nativeProbe.health().getStatus());
        String id = owned("rabbitmq");
        try {
            docker("pause", id);
            long start = System.nanoTime();
            assertEquals(Status.UP, nativeProbe.health().getStatus(),
                "Characterization: cached server metadata does not prove a fresh broker exchange");
            assertTrue(Duration.ofNanos(System.nanoTime() - start).toMillis() < 2000);
            start = System.nanoTime();
            assertEquals(Status.DOWN, activeRabbit.health().getStatus());
            assertTrue(Duration.ofNanos(System.nanoTime() - start).toMillis() < 12000);
        } finally {
            assertEquals(id, owned("rabbitmq")); docker("unpause", id);
        }
    }

    @Test void authenticatedRedisRejectsWrongPasswordDefaultUserAndWrites() {
        try (var connection = redis.getConnection()) {
            assertEquals("PONG", connection.ping());
            assertNotNull(connection.serverCommands().info().getProperty("redis_version"));
            assertThrows(org.springframework.dao.DataAccessException.class,
                () -> connection.stringCommands().set("forbidden_fixture".getBytes(StandardCharsets.UTF_8), new byte[]{1}));
        }
        var client = ((org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory) redis).getClientConfiguration();
        assertEquals(Duration.ofSeconds(2), client.getCommandTimeout());
        assertEquals(Duration.ofSeconds(1), client.getClientOptions().orElseThrow().getSocketOptions().getConnectTimeout());
        for (String user : List.of("foundation_probe", "default")) {
            var uri = RedisURI.builder().withHost("127.0.0.1").withPort(6380)
                .withAuthentication(user, "synthetic-wrong-password").withTimeout(Duration.ofSeconds(2)).build();
            RedisClient wrongClient = RedisClient.create(uri);
            try { assertThrows(io.lettuce.core.RedisConnectionException.class, wrongClient::connect); }
            finally { wrongClient.shutdown(Duration.ZERO, Duration.ofSeconds(2)); }
        }
    }

    private com.rabbitmq.client.ConnectionFactory rabbitFactory(String password, String vhost) {
        var factory = new com.rabbitmq.client.ConnectionFactory();
        factory.setHost("127.0.0.1"); factory.setPort(5673);
        factory.setUsername(rabbit.getUsername()); factory.setPassword(password); factory.setVirtualHost(vhost);
        factory.setConnectionTimeout(2000); factory.setHandshakeTimeout(2000);
        factory.setChannelRpcTimeout(2000); factory.setShutdownTimeout(2000);
        factory.setAutomaticRecoveryEnabled(false);
        return factory;
    }

    @Test void authenticatedRabbitRejectsWrongCredentialsVhostAndResourceCreation() throws Exception {
        assertTrue(context.getBeansOfType(AmqpAdmin.class).isEmpty());
        try (var connection = rabbitFactory(rabbit.getPassword(), rabbit.getVirtualHost()).newConnection()) {
            var channel = connection.createChannel();
            channel.basicQos(1);
            assertThrows(java.io.IOException.class,
                () -> channel.queueDeclare("elogistics_forbidden_fixture", false, false, true, null));
            assertFalse(channel.isOpen(), "Broker must close channel on forbidden resource access");
        }
        assertThrows(com.rabbitmq.client.AuthenticationFailureException.class,
            () -> rabbitFactory("synthetic-wrong-password", rabbit.getVirtualHost()).newConnection());
        assertThrows(java.io.IOException.class, () -> rabbitFactory(rabbit.getPassword(), "/").newConnection());
    }

    @Test void activeProbeReleasesConnectionsAndLeavesApplicationPolicyUnchanged() throws Exception {
        var application = (org.springframework.amqp.rabbit.connection.CachingConnectionFactory) rabbitTemplate.getConnectionFactory();
        int originalRpc = application.getRabbitConnectionFactory().getChannelRpcTimeout();
        assertEquals(Status.UP, new RabbitHealthIndicator(rabbitTemplate).health().getStatus());
        long baseline = amqpWorkers();
        for (int i = 0; i < 20; i++) {
            assertEquals(Status.UP, activeRabbit.health().getStatus());
            assertTrue(activeRabbit.health().getDetails().isEmpty());
        }
        assertEquals(originalRpc, application.getRabbitConnectionFactory().getChannelRpcTimeout());
        for (int i = 0; i < 30 && amqpWorkers() > baseline; i++) Thread.sleep(100);
        assertTrue(amqpWorkers() <= baseline, "Health-only AMQP workers leaked");
    }

    private long amqpWorkers() {
        return Thread.getAllStackTraces().keySet().stream()
            .filter(t -> t.isAlive() && t.getName().startsWith("AMQP Connection")).count();
    }

    @Test void fullProfileProbesAreStatusOnlyAndManagementRemainsRestricted() throws Exception {
        for (String path : List.of("liveness", "readiness")) {
            mvc.perform(get("/actuator/health/" + path)).andExpect(status().isOk())
                .andExpect(content().json("{\"status\":\"UP\"}", org.springframework.test.json.JsonCompareMode.STRICT));
        }
        for (String path : List.of("/actuator", "/actuator/health", "/actuator/health/redis", "/actuator/health/rabbit"))
            mvc.perform(get(path)).andExpect(status().isUnauthorized());
    }
}
