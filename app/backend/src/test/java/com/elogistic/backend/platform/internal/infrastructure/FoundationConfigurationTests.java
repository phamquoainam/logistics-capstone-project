package com.elogistic.backend.platform.internal.infrastructure;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import static org.junit.jupiter.api.Assertions.*;

class FoundationConfigurationTests {
    private static final String[] NAMES = {"FOUNDATION_REDIS_USER", "FOUNDATION_REDIS_PASSWORD",
        "FOUNDATION_RABBIT_USER", "FOUNDATION_RABBIT_PASSWORD"};

    private MockEnvironment configured() {
        var env = new MockEnvironment();
        env.setActiveProfiles("demo", "postgres", "foundation");
        for (String name : NAMES) env.setProperty(name, "synthetic-configuration-test");
        return env;
    }

    @Test void rejectsIncompleteProfileCombination() {
        var env = configured(); env.setActiveProfiles("demo", "foundation");
        assertThrows(IllegalStateException.class,
            () -> new FoundationConfiguration().foundationConfigurationValidation(env).afterPropertiesSet());
    }

    @Test void rejectsEachMissingOrBlankCredentialWithoutExposingValues() {
        for (String name : NAMES) {
            for (boolean missing : new boolean[]{true, false}) {
                var env = new MockEnvironment(); env.setActiveProfiles("demo", "postgres", "foundation");
                for (String key : NAMES) if (!key.equals(name)) env.setProperty(key, "synthetic-configuration-test");
                if (!missing) env.setProperty(name, " ");
                var error = assertThrows(IllegalStateException.class,
                    () -> new FoundationConfiguration().foundationConfigurationValidation(env).afterPropertiesSet());
                assertTrue(error.getMessage().contains(name));
                assertFalse(error.getMessage().contains("synthetic-configuration-test"));
            }
        }
    }

    @Test void acceptsExplicitProfilesWithRequiredCredentials() {
        assertDoesNotThrow(() -> new FoundationConfiguration().foundationConfigurationValidation(configured()).afterPropertiesSet());
    }
}
