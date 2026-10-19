package com.elogistic.backend.platform.internal.infrastructure;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.core.io.support.ResourcePropertySource;
import org.springframework.mock.env.MockEnvironment;
import static org.junit.jupiter.api.Assertions.*;

/** Production property-file resolution evidence, NOT datasource startup or DB authentication proof.
 * No datasource, Flyway instance, JDBC connection or test-defined production validator is created.
 */
class PostgresConfigurationEvidenceTests {
    private static Map<String, String> synthetic() {
        var values = new LinkedHashMap<String, String>();
        values.put("DEMO_DB_URL", "jdbc:postgresql://127.0.0.1:1/s2_never_connect_demo");
        values.put("DEMO_DB_USER", "s2_demo"); values.put("DEMO_DB_PASSWORD", "S2-demo-synthetic-only!");
        values.put("PG_DB_URL", "jdbc:postgresql://127.0.0.1:1/s2_never_connect_pg");
        values.put("PG_RUNTIME_USER", "s2_runtime"); values.put("PG_RUNTIME_PASSWORD", "S2-runtime-synthetic-only!");
        values.put("PG_MIGRATOR_USER", "s2_migrator"); values.put("PG_MIGRATOR_PASSWORD", "S2-migrator-synthetic-only!");
        values.put("PG_OWNER_ROLE", "s2_owner");
        return values;
    }

    private MockEnvironment environment(String profiles, Map<String, String> variables) throws IOException {
        var env = new MockEnvironment();
        env.getPropertySources().addLast(new ResourcePropertySource("classpath:application.properties"));
        for (String profile : profiles.split(",")) {
            env.getPropertySources().addAfter("mockProperties",
                new ResourcePropertySource("classpath:application-" + profile + ".properties"));
        }
        variables.forEach(env::setProperty);
        return env;
    }

    static Stream<Arguments> requiredBindings() {
        return Stream.of(
            Arguments.of("demo-url", "demo", "DEMO_DB_URL", "spring.datasource.url"),
            Arguments.of("demo-user", "demo", "DEMO_DB_USER", "spring.datasource.username"),
            Arguments.of("demo-password", "demo", "DEMO_DB_PASSWORD", "spring.datasource.password"),
            Arguments.of("pg-url", "demo,postgres", "PG_DB_URL", "spring.datasource.url"),
            Arguments.of("pg-runtime-user", "demo,postgres", "PG_RUNTIME_USER", "spring.datasource.username"),
            Arguments.of("pg-runtime-password", "demo,postgres", "PG_RUNTIME_PASSWORD", "spring.datasource.password"),
            Arguments.of("pg-migrator-user", "demo,postgres", "PG_MIGRATOR_USER", "spring.flyway.user"),
            Arguments.of("pg-migrator-password", "demo,postgres", "PG_MIGRATOR_PASSWORD", "spring.flyway.password"),
            Arguments.of("pg-owner", "demo,postgres", "PG_OWNER_ROLE", "spring.flyway.init-sqls"),
            Arguments.of("migration-url", "migration", "PG_DB_URL", "spring.flyway.url"),
            Arguments.of("migration-user", "migration", "PG_MIGRATOR_USER", "spring.datasource.username"),
            Arguments.of("migration-password", "migration", "PG_MIGRATOR_PASSWORD", "spring.datasource.password"),
            Arguments.of("migration-owner", "migration", "PG_OWNER_ROLE", "spring.flyway.init-sqls"),
            Arguments.of("migration-grant-target", "migration", "PG_RUNTIME_USER", "spring.flyway.placeholders.runtimeRole"));
    }

    @ParameterizedTest(name = "{0}") @MethodSource("requiredBindings")
    void missingPlaceholderFailsExplicitResolutionWithoutNetwork(String label, String profiles, String variable, String property) throws Exception {
        var values = synthetic(); values.remove(variable);
        var env = environment(profiles, values);
        var error = assertThrows(IllegalArgumentException.class, () -> env.getRequiredProperty(property));
        assertTrue(error.getMessage().contains(variable), "Resolution failure must identify missing variable");
        for (String value : values.values()) assertFalse(error.toString().contains(value), "Resolution cause must not echo other values");
    }

    @ParameterizedTest(name = "{0}") @MethodSource("requiredBindings")
    void blankValuesAreNotRejectedAtPropertyResolutionBoundary(String label, String profiles, String variable, String property) throws Exception {
        for (String blank : new String[]{"", " \t "}) {
            var values = synthetic(); values.put(variable, blank);
            var env = environment(profiles, values);
            String result = env.getRequiredProperty(property);
            assertEquals(property.equals("spring.flyway.init-sqls") ? "SET ROLE " + blank : blank, result);
        }
        // Characterization only: absence of property validation is NOT proof that a real DB starts.
    }

    @Test void validPostgresPropertiesKeepRuntimeAndMigratorResponsibilitiesDistinct() throws Exception {
        var values = synthetic(); var env = environment("demo,postgres", values);
        assertEquals(values.get("PG_DB_URL"), env.getRequiredProperty("spring.datasource.url"));
        assertEquals(values.get("PG_DB_URL"), env.getRequiredProperty("spring.flyway.url"));
        assertEquals(values.get("PG_RUNTIME_USER"), env.getRequiredProperty("spring.datasource.username"));
        assertEquals(values.get("PG_RUNTIME_PASSWORD"), env.getRequiredProperty("spring.datasource.password"));
        assertEquals(values.get("PG_MIGRATOR_USER"), env.getRequiredProperty("spring.flyway.user"));
        assertEquals(values.get("PG_MIGRATOR_PASSWORD"), env.getRequiredProperty("spring.flyway.password"));
        assertEquals("SET ROLE s2_owner", env.getRequiredProperty("spring.flyway.init-sqls"));
        assertEquals("s2_runtime", env.getRequiredProperty("spring.flyway.placeholders.runtimeRole"));
        assertEquals("true", env.getRequiredProperty("spring.flyway.enabled"));
        assertEquals("classpath:db/migration", env.getRequiredProperty("spring.flyway.locations"));
    }

    @Test void migrationOnlyNeedsGrantTargetButNotRuntimePasswordOrToken() throws Exception {
        var values = synthetic(); values.remove("PG_RUNTIME_PASSWORD");
        var env = environment("migration", values);
        assertEquals("none", env.getRequiredProperty("spring.main.web-application-type"));
        assertEquals(values.get("PG_MIGRATOR_USER"), env.getRequiredProperty("spring.datasource.username"));
        assertEquals(values.get("PG_MIGRATOR_PASSWORD"), env.getRequiredProperty("spring.datasource.password"));
        assertEquals("s2_runtime", env.getRequiredProperty("spring.flyway.placeholders.runtimeRole"));
        assertNull(env.getProperty("demo.token-secret"));
        assertNull(env.getProperty("demo.seed.enabled"));
    }

    @Test void runtimeOnlyResolvesRuntimePropertiesWithoutUnusedMigrationVariables() throws Exception {
        var values = synthetic();
        for (String name : new String[]{"PG_MIGRATOR_USER", "PG_MIGRATOR_PASSWORD", "PG_OWNER_ROLE"}) values.remove(name);
        values.put("PG_MIGRATION_ENABLED", "false"); values.put("DEMO_SEED_ENABLED", "false");
        var env = environment("demo,postgres", values);
        assertEquals(Boolean.FALSE, env.getRequiredProperty("spring.flyway.enabled", Boolean.class));
        assertEquals(Boolean.FALSE, env.getRequiredProperty("demo.seed.enabled", Boolean.class));
        assertEquals(values.get("PG_RUNTIME_USER"), env.getRequiredProperty("spring.datasource.username"));
        assertEquals(values.get("PG_RUNTIME_PASSWORD"), env.getRequiredProperty("spring.datasource.password"));
        // Unused migration placeholders still fail IF resolved. This does not prove auto-config binding order.
        assertThrows(IllegalArgumentException.class, () -> env.getRequiredProperty("spring.flyway.user"));
    }

    @Test void invalidMigrationBooleanHasConfigurationConversionFailure() throws Exception {
        var values = synthetic(); values.put("PG_MIGRATION_ENABLED", "not-a-boolean");
        var env = environment("demo,postgres", values);
        var error = assertThrows(org.springframework.core.convert.ConversionFailedException.class,
            () -> env.getRequiredProperty("spring.flyway.enabled", Boolean.class));
        assertInstanceOf(IllegalArgumentException.class, error.getCause());
    }

    @Test void jdbcUrlSyntaxCheckDoesNotOpenConnection() {
        assertNull(org.postgresql.Driver.parseURL("not-a-jdbc-url", new Properties()));
        assertNotNull(org.postgresql.Driver.parseURL(synthetic().get("PG_DB_URL"), new Properties()));
    }

    @Test void unrelatedDemoModeDoesNotRequirePostgresOrFoundationVariables() throws Exception {
        var values = synthetic(); values.keySet().removeIf(key -> key.startsWith("PG_"));
        var env = environment("demo", values);
        assertEquals(values.get("DEMO_DB_URL"), env.getRequiredProperty("spring.datasource.url"));
        assertEquals("classpath:db/demo-migration", env.getRequiredProperty("spring.flyway.locations"));
        assertEquals(Boolean.FALSE, env.getRequiredProperty("demo.seed.enabled", Boolean.class));
    }
}
