package com.elogistic.backend.identity.internal.infrastructure;

import java.util.List;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.aop.support.AopUtils;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.SmartLifecycle;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import static org.junit.jupiter.api.Assertions.*;

class DemoSeedPreflightTests {
    private static final List<String> KEYS = List.of("admin-password", "merchant-a-password",
        "merchant-b-password", "operator-password", "disabled-password");
    private static final List<String> USERS = List.of("demo.admin", "demo.merchant.a", "demo.merchant.b",
        "demo.operator", "demo.disabled");

    private static MockEnvironment configured() {
        var env = new MockEnvironment(); env.setActiveProfiles("demo");
        env.setProperty("demo.seed.enabled", "true");
        for (String key : KEYS) env.setProperty("demo.seed." + key, "  synthetic-R1-password  ");
        return env;
    }

    private static void remove(MockEnvironment env, String key) {
        ((Map<?, ?>) env.getPropertySources().get("mockProperties").getSource()).remove("demo.seed." + key);
    }

    static Stream<Arguments> invalidPasswords() {
        return KEYS.stream().flatMap(key -> Stream.of(
            Arguments.of(key + "-missing", key, null), Arguments.of(key + "-empty", key, ""),
            Arguments.of(key + "-blank", key, " ".repeat(12)), Arguments.of(key + "-short", key, "x".repeat(11)),
            Arguments.of(key + "-long", key, "x".repeat(73))));
    }

    @ParameterizedTest(name = "{0}") @MethodSource("invalidPasswords")
    void invalidMissingAccountFailsPreflightWithoutEncodingOrWriting(String label, String key, String value) {
        var env = configured();
        if (value == null) remove(env, key); else env.setProperty("demo.seed." + key, value);
        var accounts = new DemoSeedConfigurationEvidenceTests.MemoryAccounts();
        var encoder = new DemoSeedConfigurationEvidenceTests.RecordingEncoder();
        var preflight = new DemoSeedPreflight(new DemoAccountSeed(accounts, encoder, env));
        var error = assertThrows(IllegalStateException.class, preflight::afterSingletonsInstantiated);
        assertEquals("Configure all demo seed passwords (12–72 characters)", error.getMessage());
        assertTrue(accounts.mutations.isEmpty()); assertEquals(0, encoder.calls);
        assertFalse(error.toString().contains("synthetic-R1-password"));
    }

    @Test void existingAccountsNeedNoPasswordsAndMixedStateChecksOnlyMissingAccounts() {
        var env = configured();
        var accounts = new DemoSeedConfigurationEvidenceTests.MemoryAccounts();
        var encoder = new DemoSeedConfigurationEvidenceTests.RecordingEncoder();
        accounts.existing.addAll(USERS);
        for (String key : KEYS) remove(env, key);
        var preflight = new DemoSeedPreflight(new DemoAccountSeed(accounts, encoder, env));
        assertDoesNotThrow(preflight::afterSingletonsInstantiated);
        accounts.existing.remove(USERS.get(4));
        assertThrows(IllegalStateException.class, preflight::afterSingletonsInstantiated);
        env.setProperty("demo.seed.disabled-password", "x".repeat(12));
        assertDoesNotThrow(preflight::afterSingletonsInstantiated);
        assertTrue(accounts.mutations.isEmpty()); assertEquals(0, encoder.calls);
    }

    @Test void externalDeletionAfterPreflightStillRejectsMissingPasswordInsteadOfUnsafeCreation() {
        var env = configured();
        var accounts = new DemoSeedConfigurationEvidenceTests.MemoryAccounts();
        accounts.existing.addAll(USERS); for (String key : KEYS) remove(env, key);
        var encoder = new DemoSeedConfigurationEvidenceTests.RecordingEncoder();
        var seed = new DemoAccountSeed(accounts, encoder, env);
        new DemoSeedPreflight(seed).afterSingletonsInstantiated();
        accounts.existing.remove(USERS.get(0)); // Models the explicitly unclosed shared-DB race.
        assertThrows(IllegalStateException.class, () -> seed.run(new DefaultApplicationArguments()));
        assertTrue(accounts.mutations.isEmpty()); assertEquals(0, encoder.calls);
    }

    private ApplicationContextRunner context(MockEnvironment env, AtomicBoolean lifecycleStarted,
            DemoSeedConfigurationEvidenceTests.MemoryAccounts accounts) {
        return new ApplicationContextRunner().withInitializer(application -> {
            application.setEnvironment(env);
            application.getBeanFactory().registerSingleton("accounts", accounts);
            application.getBeanFactory().registerSingleton("encoder", new DemoSeedConfigurationEvidenceTests.RecordingEncoder());
        }).withUserConfiguration(DemoAccountSeed.class, DemoSeedPreflight.class)
          .withBean("servingLifecycleControl", SmartLifecycle.class, () -> new SmartLifecycle() {
              private boolean running;
              public void start() { running = true; lifecycleStarted.set(true); }
              public void stop() { running = false; }
              public boolean isRunning() { return running; }
          });
    }

    @Test void invalidPreflightAbortsRefreshBeforeSmartLifecycleCanStart() {
        var env = configured(); remove(env, KEYS.get(4));
        var started = new AtomicBoolean(); var accounts = new DemoSeedConfigurationEvidenceTests.MemoryAccounts();
        context(env, started, accounts).run(result -> {
            assertNotNull(result.getStartupFailure()); assertFalse(started.get());
            assertTrue(result.getStartupFailure().toString().contains("Configure all demo seed passwords"));
        });
        assertTrue(accounts.mutations.isEmpty());
    }

    @Test void validReadOnlyPreflightAllowsLifecycleThenRunnerCreatesAccounts() {
        var started = new AtomicBoolean(); var accounts = new DemoSeedConfigurationEvidenceTests.MemoryAccounts();
        context(configured(), started, accounts).run(result -> {
            assertNull(result.getStartupFailure()); assertTrue(started.get()); assertTrue(accounts.mutations.isEmpty());
            result.getBean(DemoAccountSeed.class).run(new DefaultApplicationArguments());
            assertEquals(USERS, accounts.mutations);
        });
    }

    @Test void disabledAbsentOrNonDemoSeedDoesNotCreatePreflightOrQueryAccounts() {
        for (String mode : List.of("disabled", "absent", "other-profile")) {
            var env = configured();
            if (mode.equals("disabled")) env.setProperty("demo.seed.enabled", "false");
            if (mode.equals("absent")) ((Map<?, ?>) env.getPropertySources().get("mockProperties").getSource()).remove("demo.seed.enabled");
            if (mode.equals("other-profile")) env.setActiveProfiles("other");
            var started = new AtomicBoolean();
            var accounts = new DemoSeedConfigurationEvidenceTests.MemoryAccounts() {
                @Override public java.util.Optional<com.elogistic.backend.identity.internal.application.port.AccountQueries.Credential> credential(String name) {
                    throw new AssertionError("Disabled preflight must not query persistence");
                }
            };
            context(env, started, accounts).run(result -> {
                assertNull(result.getStartupFailure()); assertTrue(started.get());
                assertTrue(result.getBeansOfType(DemoAccountSeed.class).isEmpty());
                assertTrue(result.getBeansOfType(DemoSeedPreflight.class).isEmpty());
            });
        }
    }

    @Configuration(proxyBeanMethods = false) @EnableTransactionManagement(proxyTargetClass = true)
    static class TransactionFixture {
        @Bean DataSource dataSource() {
            // Like the real demo, retain the migration session through a pool;
            // H2's constant-IN check must not retain a physically closed creation session.
            var pool = new HikariDataSource();
            pool.setJdbcUrl("jdbc:h2:mem:r1_" + UUID.randomUUID() + ";MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1");
            pool.setUsername("synthetic"); pool.setPassword("synthetic-fixture-only");
            pool.setMaximumPoolSize(2); pool.setMinimumIdle(1);
            return pool;
        }
        @Bean(initMethod = "migrate") Flyway flyway(DataSource dataSource) {
            return Flyway.configure().dataSource(dataSource).locations("classpath:db/demo-migration").load();
        }
        @Bean DataSourceTransactionManager transactionManager(DataSource dataSource) { return new DataSourceTransactionManager(dataSource); }
        @Bean PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(12); }
        @Bean FailingAccounts accountRepository(DataSource dataSource, Flyway flyway) { return new FailingAccounts(new JdbcTemplate(dataSource)); }
        @Bean DemoAccountSeed demoAccountSeed(FailingAccounts accounts, PasswordEncoder encoder, Environment env) {
            return new DemoAccountSeed(accounts, encoder, env);
        }
    }

    static class FailingAccounts extends AccountRepository {
        boolean failLast;
        FailingAccounts(JdbcTemplate jdbc) { super(jdbc); }
        @Override public void seed(UUID id, String username, String hash, String display, String role, boolean enabled, UUID merchant) {
            if (failLast && username.equals("demo.disabled")) throw new IllegalStateException("synthetic insert failure");
            super.seed(id, username, hash, display, role, enabled, merchant);
        }
    }

    @Test void actualH2ProxyRollbackBCryptAndExistingIdentityArePreserved() {
        var env = configured();
        try (var application = new AnnotationConfigApplicationContext()) {
            application.setEnvironment(env); application.register(TransactionFixture.class); application.refresh();
            var seed = application.getBean(DemoAccountSeed.class);
            assertTrue(AopUtils.isAopProxy(seed), "Runner writes must go through Spring's transaction proxy");
            var accounts = application.getBean(FailingAccounts.class);
            var jdbc = new JdbcTemplate(application.getBean(DataSource.class));
            seed.validateConfiguration(); assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM identity.demo_account", Integer.class));
            accounts.failLast = true;
            assertThrows(IllegalStateException.class, () -> seed.run(new DefaultApplicationArguments()));
            assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM identity.demo_account", Integer.class), "Earlier inserts must roll back");
            accounts.failLast = false; seed.run(new DefaultApplicationArguments());
            var before = jdbc.queryForList("SELECT * FROM identity.demo_account ORDER BY username");
            assertEquals(5, before.size());
            var encoder = application.getBean(PasswordEncoder.class);
            for (var row : before) {
                assertTrue(encoder.matches("  synthetic-R1-password  ", (String) row.get("password_hash")), "BCrypt must preserve raw padding");
            }
            for (String key : KEYS) remove(env, key);
            seed.validateConfiguration(); seed.run(new DefaultApplicationArguments());
            assertTrue(before.equals(jdbc.queryForList("SELECT * FROM identity.demo_account ORDER BY username")), "IDs, hashes, roles, enabled flags and merchant scopes must be unchanged");
        }
    }

    static Stream<Arguments> bcryptUtf8Boundaries() {
        return Stream.of(
            Arguments.of("A-e-36", "\u00e9".repeat(36), 36, 72, true, true),
            Arguments.of("B-e-37", "\u00e9".repeat(37), 37, 74, false, false),
            Arguments.of("C-euro-24", "\u20ac".repeat(24), 24, 72, true, true),
            Arguments.of("D-euro-25", "\u20ac".repeat(25), 25, 75, false, false),
            Arguments.of("E-ascii-72", "x".repeat(72), 72, 72, true, true),
            Arguments.of("F-ascii-73", "x".repeat(73), 73, 73, false, false),
            Arguments.of("G-mixed-72", "x".repeat(20) + "\u00e9".repeat(26), 46, 72, true, true),
            Arguments.of("H-mixed-73", "x".repeat(21) + "\u00e9".repeat(26), 47, 73, false, false),
            Arguments.of("I-padded", "  " + "x".repeat(12) + "  ", 16, 16, true, true),
            Arguments.of("supplementary-72", "\ud83d\ude42".repeat(18), 36, 72, true, true),
            Arguments.of("supplementary-76", "\ud83d\ude42".repeat(19), 38, 76, false, false),
            Arguments.of("malformed-java-replacement", "\ud800".repeat(12), 12, 12, true, true),
            Arguments.of("empty-encoder-only", "", 0, 0, true, false),
            Arguments.of("blank-encoder-only", " ".repeat(12), 12, 12, true, false),
            Arguments.of("short-encoder-only", "x".repeat(11), 11, 11, true, false));
    }

    @ParameterizedTest(name = "{0}") @MethodSource("bcryptUtf8Boundaries")
    void pinnedBCryptCharacterization(String label, String value, int characters, int bytes,
            boolean encoderAccepts, boolean applicationAccepts) {
        assertEquals(characters, value.length());
        assertEquals(bytes, value.getBytes(StandardCharsets.UTF_8).length);
        var encoder = new BCryptPasswordEncoder(12);
        if (encoderAccepts) {
            String hash = assertDoesNotThrow(() -> encoder.encode(value));
            assertTrue(hash.startsWith("$2a$12$"));
            if (label.equals("empty-encoder-only")) {
                assertFalse(encoder.matches(value, hash), "Pinned matches rejects empty; application policy already rejects it");
            } else assertTrue(encoder.matches(value, hash), "Original raw value must verify");
            if (label.equals("I-padded")) assertFalse(encoder.matches(value.strip(), hash));
        } else {
            var error = assertThrows(IllegalArgumentException.class, () -> encoder.encode(value));
            assertEquals("password cannot be more than 72 bytes", error.getMessage());
        }
    }

    @ParameterizedTest(name = "{0}") @MethodSource("bcryptUtf8Boundaries")
    void preflightKeepsCharacterRulesAndRejectsEncoderByteOverflow(String label, String value,
            int characters, int bytes, boolean encoderAccepts, boolean applicationAccepts) {
        var env = configured(); env.setProperty("demo.seed.admin-password", value);
        var accounts = new DemoSeedConfigurationEvidenceTests.MemoryAccounts();
        var encoder = new DemoSeedConfigurationEvidenceTests.RecordingEncoder();
        var preflight = new DemoSeedPreflight(new DemoAccountSeed(accounts, encoder, env));
        if (applicationAccepts) assertDoesNotThrow(preflight::afterSingletonsInstantiated);
        else assertThrows(IllegalStateException.class, preflight::afterSingletonsInstantiated);
        assertEquals(0, encoder.calls); assertTrue(accounts.mutations.isEmpty());
    }

    static Stream<String> seedPasswordKeys() { return KEYS.stream(); }

    @ParameterizedTest @MethodSource("seedPasswordKeys")
    void byteOverflowFailsBeforeEncodingForEveryMissingAccount(String key) {
        var env = configured(); env.setProperty("demo.seed." + key, "\u00e9".repeat(37));
        var accounts = new DemoSeedConfigurationEvidenceTests.MemoryAccounts();
        var encoder = new DemoSeedConfigurationEvidenceTests.RecordingEncoder();
        var seed = new DemoAccountSeed(accounts, encoder, env);
        var error = assertThrows(IllegalStateException.class,
            () -> new DemoSeedPreflight(seed).afterSingletonsInstantiated());
        assertEquals("Configure all demo seed passwords (at most 72 UTF-8 bytes for BCrypt)", error.getMessage());
        assertEquals(0, encoder.calls); assertTrue(accounts.mutations.isEmpty());
    }

    static Stream<Arguments> unusedExistingPasswords() {
        return Stream.of(Arguments.of("absent", (String) null), Arguments.of("empty", ""),
            Arguments.of("blank", " ".repeat(12)), Arguments.of("long-characters", "x".repeat(73)),
            Arguments.of("long-bytes", "\u00e9".repeat(37)));
    }

    @ParameterizedTest(name = "{0}") @MethodSource("unusedExistingPasswords")
    void existingAccountExemptionIgnoresUnusedInvalidPasswords(String label, String value) {
        var env = configured(); for (String key : KEYS) remove(env, key);
        if (value != null) for (String key : KEYS) env.setProperty("demo.seed." + key, value);
        var accounts = new DemoSeedConfigurationEvidenceTests.MemoryAccounts(); accounts.existing.addAll(USERS);
        var encoder = new DemoSeedConfigurationEvidenceTests.RecordingEncoder();
        var seed = new DemoAccountSeed(accounts, encoder, env);
        assertDoesNotThrow(() -> new DemoSeedPreflight(seed).afterSingletonsInstantiated());
        assertDoesNotThrow(() -> seed.run(new DefaultApplicationArguments()));
        assertEquals(0, encoder.calls); assertTrue(accounts.mutations.isEmpty());
    }

    @Test void nullRequiredValueHasSanitizedConfigurationRejection() {
        var env = new MockEnvironment() {
            @Override public String getProperty(String key, String defaultValue) { return null; }
        };
        var accounts = new DemoSeedConfigurationEvidenceTests.MemoryAccounts();
        var encoder = new DemoSeedConfigurationEvidenceTests.RecordingEncoder();
        var error = assertThrows(IllegalStateException.class,
            () -> new DemoSeedPreflight(new DemoAccountSeed(accounts, encoder, env)).afterSingletonsInstantiated());
        assertEquals("Configure all demo seed passwords (12–72 characters)", error.getMessage());
        assertTrue(accounts.mutations.isEmpty()); assertEquals(0, encoder.calls);
    }

    @Test void pinnedEncoderNullBehaviorDoesNotReplaceApplicationNonNullPolicy() {
        assertNull(new BCryptPasswordEncoder(12).encode(null));
    }

    @Test void malformedJavaUnicodeUsesSameUtf8ReplacementAsEncoder() {
        String raw = "\ud800".repeat(12);
        assertArrayEquals("?".repeat(12).getBytes(StandardCharsets.UTF_8), raw.getBytes(StandardCharsets.UTF_8));
        var encoder = new BCryptPasswordEncoder(12); String hash = encoder.encode(raw);
        assertTrue(encoder.matches(raw, hash));
        assertTrue(encoder.matches("?".repeat(12), hash));
        // Characterization only: do not introduce a new Unicode normalization policy.
    }
}
