package com.elogistic.backend.demo;

import com.zaxxer.hikari.HikariDataSource;
import java.nio.file.Path;
import java.sql.*;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.availability.AvailabilityChangeEvent;
import org.springframework.boot.availability.ReadinessState;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.boot.health.contributor.Status;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@EnabledIfSystemProperty(named = "demo.test.database", matches = "postgres")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ActiveProfiles({"demo", "postgres"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@TestPropertySource(properties = {"spring.datasource.hikari.maximum-pool-size=1",
    "spring.datasource.hikari.minimum-idle=0"})
class PostgresHealthSafetyTests extends DemoTestSupport {
    @Autowired HikariDataSource dataSource;
    @Autowired ConfigurableApplicationContext context;
    @Autowired @Qualifier("dbHealthIndicator") HealthIndicator health;

    private int pid(Connection connection) throws SQLException {
        try (var statement = connection.createStatement(); var rows = statement.executeQuery("SELECT pg_backend_pid()")) {
            assertTrue(rows.next()); return rows.getInt(1);
        }
    }

    private void longRead(Connection connection) throws SQLException {
        assertEquals(0, connection.getNetworkTimeout(), "No global/readiness socket timeout may leak into runtime SQL");
        long start = System.nanoTime();
        try (var statement = connection.createStatement()) {
            statement.setQueryTimeout(8); // Test-only guard, not application timeout policy.
            try (var rows = statement.executeQuery("SELECT pg_sleep(4)")) { assertTrue(rows.next()); }
        }
        assertTrue(Duration.ofNanos(System.nanoTime() - start).toMillis() >= 3500);
        assertFalse(connection.isClosed()); assertTrue(connection.isValid(1));
        assertEquals(0, connection.getNetworkTimeout());
    }

    @Test void longRuntimeReadAndPhysicalReuseRemainSafeAfterSuccessfulProbe() throws Exception {
        int before;
        try (var connection = dataSource.getConnection()) { before = pid(connection); longRead(connection); }
        assertEquals(Status.UP, health.health().getStatus());
        assertEquals(0, dataSource.getHikariPoolMXBean().getActiveConnections());
        try (var connection = dataSource.getConnection()) {
            assertEquals(before, pid(connection), "Successful probe must reuse the same physical connection safely");
            longRead(connection);
        }
    }

    @Test void concurrentLongReadIsNotInterruptedByExhaustedPoolProbe() throws Exception {
        var borrowed = new CountDownLatch(1);
        try (var executor = Executors.newSingleThreadExecutor()) {
            Future<Void> query = executor.submit(() -> {
                try (var connection = dataSource.getConnection()) { borrowed.countDown(); longRead(connection); }
                return null;
            });
            assertTrue(borrowed.await(5, TimeUnit.SECONDS));
            long start = System.nanoTime();
            assertEquals(Status.DOWN, health.health().getStatus());
            long millis = Duration.ofNanos(System.nanoTime() - start).toMillis();
            assertTrue(millis >= 1500 && millis < 8000);
            query.get(12, TimeUnit.SECONDS); // Test wait only; not used to cancel JDBC.
        }
        assertEquals(Status.UP, health.health().getStatus());
        assertEquals(0, dataSource.getHikariPoolMXBean().getActiveConnections());
    }

    @Test void terminatedPhysicalConnectionIsDiscardedAndReplacementIsSafe() throws Exception {
        int previous;
        try (var connection = dataSource.getConnection()) { previous = pid(connection); }
        try (var observer = DriverManager.getConnection(requiredEnv("PG_TEST_DB_URL"),
                requiredEnv("PG_TEST_RUNTIME_USER"), requiredEnv("PG_TEST_RUNTIME_PASSWORD"));
                var statement = observer.prepareStatement("SELECT pg_terminate_backend(?)")) {
            statement.setInt(1, previous);
            try (var rows = statement.executeQuery()) { assertTrue(rows.next()); assertTrue(rows.getBoolean(1)); }
        }
        // Hikari may discard the broken idle connection before the indicator borrows it.
        var first = health.health().getStatus();
        assertTrue(first.equals(Status.DOWN) || first.equals(Status.UP));
        assertEquals(0, dataSource.getHikariPoolMXBean().getActiveConnections());
        try (var replacement = dataSource.getConnection()) {
            assertNotEquals(previous, pid(replacement)); assertEquals(0, replacement.getNetworkTimeout());
            assertTrue(replacement.isValid(1));
        }
        assertEquals(Status.UP, health.health().getStatus());
    }

    private String docker(String... args) throws Exception {
        String binary = "docker";
        if (System.getProperty("os.name").startsWith("Windows")) {
            binary = Path.of(System.getenv("LOCALAPPDATA"), "Programs", "DockerDesktop", "resources", "bin", "docker.exe").toString();
        }
        var command = new ArrayList<>(List.of(binary)); command.addAll(List.of(args));
        var process = new ProcessBuilder(command).redirectError(ProcessBuilder.Redirect.DISCARD).start();
        if (!process.waitFor(15, TimeUnit.SECONDS)) {
            process.destroyForcibly(); process.waitFor(5, TimeUnit.SECONDS);
            fail("Owned Docker diagnostic timed out");
        }
        assertEquals(0, process.exitValue(), "Owned PostgreSQL operation failed");
        return new String(process.getInputStream().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8).trim();
    }

    private String ownedContainer() throws Exception {
        String id = docker("ps", "-q", "--no-trunc", "--filter", "label=com.elogistic.d1-05.run", "--filter", "publish=5433");
        assertTrue(id.matches("[a-f0-9]{64}"), "Exactly one owned CI test container is required");
        String name = docker("inspect", "--format", "{{.Name}}", id);
        String labels = docker("inspect", "--format", "{{json .Config.Labels}}", id);
        var match = java.util.regex.Pattern.compile("\"com\\.elogistic\\.d1-05\\.run\":\"([a-f0-9]{12})\"").matcher(labels);
        assertTrue(match.find(), "Missing owned test label");
        String label = match.group(1);
        assertTrue(label.matches("[a-f0-9]{12}"));
        assertEquals("/elogistics-ci-postgres-" + label, name);
        assertTrue(requiredEnv("PG_TEST_DB_URL").endsWith("/elogistics_test_" + label));
        assertTrue(docker("inspect", "--format", "{{json .HostConfig.Tmpfs}}", id).contains("/var/lib/postgresql"));
        assertFalse(docker("inspect", "--format", "{{json .Mounts}}", id).contains("\"Type\":\"volume\""));
        return id;
    }

    @Test void stalledSocketProbeIsBoundedEvictsAndRecoversWithoutTimeoutLeak() throws Exception {
        AvailabilityChangeEvent.publish(context, ReadinessState.ACCEPTING_TRAFFIC);
        int previous;
        try (var connection = dataSource.getConnection()) { previous = pid(connection); }
        String id = ownedContainer();
        try {
            docker("pause", id);
            assertEquals("true", docker("inspect", "--format", "{{.State.Paused}}", id));
            for (int i = 0; i < 2; i++) {
                long start = System.nanoTime();
                mvc.perform(get("/actuator/health/readiness")).andExpect(status().isServiceUnavailable())
                    .andExpect(content().json("{\"status\":\"DOWN\"}", org.springframework.test.json.JsonCompareMode.STRICT));
                assertTrue(Duration.ofNanos(System.nanoTime() - start).toMillis() < 8000);
                assertEquals(0, dataSource.getHikariPoolMXBean().getActiveConnections());
                mvc.perform(get("/actuator/health/liveness")).andExpect(status().isOk());
            }
        } finally {
            assertEquals(id, ownedContainer()); docker("unpause", id);
        }
        for (int i = 0; i < 20 && !health.health().getStatus().equals(Status.UP); i++) Thread.sleep(200);
        assertEquals(Status.UP, health.health().getStatus());
        try (var replacement = dataSource.getConnection()) {
            assertNotEquals(previous, pid(replacement)); longRead(replacement);
        }
        assertEquals(0, dataSource.getHikariPoolMXBean().getActiveConnections());
    }

    @AfterAll void releaseOwnedPoolAndThreads() throws Exception {
        String prefix = dataSource.getPoolName();
        assertEquals(0, dataSource.getHikariPoolMXBean().getActiveConnections());
        dataSource.close();
        for (int i = 0; i < 50 && Thread.getAllStackTraces().keySet().stream()
                .anyMatch(t -> t.isAlive() && t.getName().startsWith(prefix)); i++) Thread.sleep(100);
        assertTrue(dataSource.isClosed());
        assertFalse(Thread.getAllStackTraces().keySet().stream()
            .anyMatch(t -> t.isAlive() && t.getName().startsWith(prefix)), "Test pool worker leaked");
    }
}
