package com.elogistic.backend.platform.internal.infrastructure;

import java.util.Arrays;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.boot.amqp.autoconfigure.RabbitProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.mock.env.MockEnvironment;
import static org.junit.jupiter.api.Assertions.*;

/** Real configuration bean lifecycle; no client auto-configurations or health probe calls. */
@ExtendWith(OutputCaptureExtension.class)
class FoundationContextEvidenceTests {
    private static final String[] KEYS = {"FOUNDATION_REDIS_USER", "FOUNDATION_REDIS_PASSWORD",
        "FOUNDATION_RABBIT_USER", "FOUNDATION_RABBIT_PASSWORD"};
    private static final String SYNTHETIC = "S2-foundation-synthetic-only-47!";

    private ApplicationContextRunner context(String[] profiles, String target, String value) {
        return new ApplicationContextRunner().withInitializer(application -> {
            var env = new MockEnvironment();
            env.setActiveProfiles(profiles);
            for (var key : KEYS) {
                if (!key.equals(target)) env.setProperty(key, SYNTHETIC);
                else if (value != null) env.setProperty(key, value);
            }
            application.setEnvironment(env);
        }).withBean(RabbitProperties.class, () -> {
            var properties = new RabbitProperties();
            properties.setHost("127.0.0.1"); properties.setPort(1);
            properties.setUsername("s2_synthetic"); properties.setPassword(SYNTHETIC);
            properties.setVirtualHost("/s2_never_connect");
            return properties;
        })
            .withUserConfiguration(FoundationConfiguration.class);
    }

    static Stream<Arguments> credentialCases() {
        return Arrays.stream(KEYS).flatMap(key -> Stream.of(
            Arguments.of(key + "-missing", key, null), Arguments.of(key + "-empty", key, ""),
            Arguments.of(key + "-whitespace", key, " \t ")));
    }

    @ParameterizedTest(name = "{0}") @MethodSource("credentialCases")
    void invalidCredentialPreventsContextRefresh(String label, String key, String value, CapturedOutput output) {
        context(new String[]{"demo", "postgres", "foundation"}, key, value).run(result -> {
            assertNotNull(result.getStartupFailure(), "Required foundation key must reject context refresh");
            Throwable error = result.getStartupFailure();
            while (error.getCause() != null) error = error.getCause();
            assertInstanceOf(IllegalStateException.class, error);
            assertEquals("Missing required foundation credential: " + key, error.getMessage());
            assertFalse(error.toString().contains(SYNTHETIC));
        });
        assertFalse(output.getAll().contains(SYNTHETIC), "Startup diagnostics must not echo other credentials");
    }

    static Stream<Arguments> incompleteProfiles() {
        return Stream.of(Arguments.of("foundation-only", new String[]{"foundation"}),
            Arguments.of("demo-foundation", new String[]{"demo", "foundation"}),
            Arguments.of("postgres-foundation", new String[]{"postgres", "foundation"}));
    }

    @ParameterizedTest(name = "{0}") @MethodSource("incompleteProfiles")
    void incompleteProfilesRejectContext(String label, String[] profiles) {
        context(profiles, null, null).run(result -> {
            assertNotNull(result.getStartupFailure());
            Throwable error = result.getStartupFailure();
            while (error.getCause() != null) error = error.getCause();
            assertEquals("Foundation requires demo,postgres,foundation profiles", error.getMessage());
        });
    }

    @Test void defaultProfileDoesNotCreateFoundationBeans() {
        context(new String[0], KEYS[0], null).run(result -> {
            assertNull(result.getStartupFailure());
            assertFalse(result.containsBean("foundationConfigurationValidation"));
            assertFalse(result.containsBean("rabbitHealthIndicator"));
        });
    }

    @Test void validFoundationConfigurationRefreshesWithoutClientConnections(CapturedOutput output) {
        context(new String[]{"demo", "postgres", "foundation"}, null, null).run(result -> {
            assertNull(result.getStartupFailure());
            assertTrue(result.containsBean("foundationConfigurationValidation"));
            assertTrue(result.containsBean("rabbitHealthIndicator"));
            assertFalse(result.containsBean("rabbitConnectionFactory"));
            assertFalse(result.containsBean("redisConnectionFactory"));
            assertFalse(result.containsBean("dataSource"));
            // Do not invoke health(): it intentionally performs network I/O.
        });
        assertFalse(output.getAll().contains(SYNTHETIC));
    }
}
