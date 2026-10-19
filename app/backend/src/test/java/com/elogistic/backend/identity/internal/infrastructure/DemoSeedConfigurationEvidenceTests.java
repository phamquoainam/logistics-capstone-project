package com.elogistic.backend.identity.internal.infrastructure;

import com.elogistic.backend.identity.api.AccountView;
import com.elogistic.backend.identity.internal.application.port.AccountQueries.Credential;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.security.crypto.password.PasswordEncoder;
import static org.junit.jupiter.api.Assertions.*;

/** Runner/unit and conditional bean evidence with fake persistence; no JDBC resources.
 * Does not prove transactional rollback or full application runner/HTTP startup ordering.
 */
class DemoSeedConfigurationEvidenceTests {
    private static final String[] PASSWORD_KEYS = {"admin-password", "merchant-a-password", "merchant-b-password", "operator-password", "disabled-password"};
    private static final String[] USERS = {"demo.admin", "demo.merchant.a", "demo.merchant.b", "demo.operator", "demo.disabled"};

    static class MemoryAccounts extends AccountRepository {
        final List<String> mutations = new ArrayList<>();
        final java.util.Set<String> existing = new java.util.HashSet<>();
        MemoryAccounts() { super(null); }
        @Override public Optional<Credential> credential(String username) {
            if (!existing.contains(username)) return Optional.empty();
            return Optional.of(new Credential(new AccountView(UUID.randomUUID(), username, "Synthetic", "ADMIN", true, null), "synthetic-existing-hash"));
        }
        @Override public void seed(UUID id, String username, String hash, String display, String role, boolean enabled, UUID merchant) {
            mutations.add(username); existing.add(username);
        }
    }

    static class RecordingEncoder implements PasswordEncoder {
        int calls;
        @Override public String encode(CharSequence raw) { calls++; return "synthetic-test-hash"; }
        @Override public boolean matches(CharSequence raw, String encoded) { throw new AssertionError("Not used in seed control tests"); }
    }

    private static MockEnvironment configured() {
        var env = new MockEnvironment(); env.setActiveProfiles("demo");
        env.setProperty("demo.seed.enabled", "true");
        for (String key : PASSWORD_KEYS) env.setProperty("demo.seed." + key, "S2-synthetic-seed-password!");
        return env;
    }

    static Stream<Arguments> absentOrBlankCases() {
        var cases = new ArrayList<Arguments>();
        for (int index = 0; index < PASSWORD_KEYS.length; index++) {
            cases.add(Arguments.of(PASSWORD_KEYS[index] + "-missing", index, null));
            cases.add(Arguments.of(PASSWORD_KEYS[index] + "-empty", index, ""));
            cases.add(Arguments.of(PASSWORD_KEYS[index] + "-short-whitespace", index, " \t "));
        }
        return cases.stream();
    }

    @ParameterizedTest(name = "{0}") @MethodSource("absentOrBlankCases")
    void invalidAbsentAccountPasswordFailsBeforeThatAccountMutation(String label, int index, String value) {
        var env = configured();
        if (value == null) ((java.util.Map<?, ?>) env.getPropertySources().get("mockProperties").getSource()).remove("demo.seed." + PASSWORD_KEYS[index]);
        else env.setProperty("demo.seed." + PASSWORD_KEYS[index], value);
        var accounts = new MemoryAccounts(); var encoder = new RecordingEncoder();
        var runner = new DemoAccountSeed(accounts, encoder, env);
        var error = assertThrows(IllegalStateException.class, () -> runner.run(new DefaultApplicationArguments()));
        assertEquals("Configure all demo seed passwords (12–72 characters)", error.getMessage());
        assertFalse(error.toString().contains("S2-synthetic-seed-password!"));
        assertFalse(accounts.mutations.contains(USERS[index]), "Invalid account must not be seeded");
        assertEquals(index, accounts.mutations.size(), "Earlier fake calls are NOT committed DB writes or rollback proof");
        assertEquals(index, encoder.calls);
    }

    static Stream<Arguments> boundaryCases() {
        return Stream.of(Arguments.of("below-11", 11, false), Arguments.of("minimum-12", 12, true),
            Arguments.of("maximum-72", 72, true), Arguments.of("above-73", 73, false));
    }

    @ParameterizedTest(name = "{0}") @MethodSource("boundaryCases")
    void sourceCharacterBoundariesControlFirstAccountMutation(String label, int length, boolean accepted) {
        var env = configured(); env.setProperty("demo.seed.admin-password", "S2!" + "x".repeat(length - 3));
        var accounts = new MemoryAccounts(); var encoder = new RecordingEncoder();
        var runner = new DemoAccountSeed(accounts, encoder, env);
        if (accepted) {
            assertDoesNotThrow(() -> runner.run(new DefaultApplicationArguments()));
            assertEquals(List.of(USERS), accounts.mutations); assertEquals(5, encoder.calls);
        } else {
            assertThrows(IllegalStateException.class, () -> runner.run(new DefaultApplicationArguments()));
            assertTrue(accounts.mutations.isEmpty()); assertEquals(0, encoder.calls);
        }
    }

    @Test void multibyteSeedBoundaryUsesJavaCharactersNotTokenBytePolicy() {
        var env = configured(); env.setProperty("demo.seed.admin-password", "\u00e9".repeat(12));
        var accounts = new MemoryAccounts();
        assertDoesNotThrow(() -> new DemoAccountSeed(accounts, new RecordingEncoder(), env).run(new DefaultApplicationArguments()));
        assertEquals(5, accounts.mutations.size());
    }

    static Stream<Arguments> allExistingAccounts() {
        return java.util.stream.IntStream.range(0, USERS.length).mapToObj(index -> Arguments.of(PASSWORD_KEYS[index], index));
    }

    @ParameterizedTest(name = "{0}") @MethodSource("allExistingAccounts")
    void existingAccountDoesNotRequireItsMissingPassword(String label, int index) {
        var env = configured(); ((java.util.Map<?, ?>) env.getPropertySources().get("mockProperties").getSource()).remove("demo.seed." + PASSWORD_KEYS[index]);
        var accounts = new MemoryAccounts(); accounts.existing.add(USERS[index]);
        var encoder = new RecordingEncoder();
        assertDoesNotThrow(() -> new DemoAccountSeed(accounts, encoder, env).run(new DefaultApplicationArguments()));
        assertEquals(4, accounts.mutations.size()); assertEquals(4, encoder.calls);
        assertFalse(accounts.mutations.contains(USERS[index]));
    }

    private ApplicationContextRunner context(String enabled, MemoryAccounts accounts, RecordingEncoder encoder) {
        return new ApplicationContextRunner().withInitializer(application -> {
            var env = new MockEnvironment(); env.setActiveProfiles("demo");
            if (enabled != null) env.setProperty("demo.seed.enabled", enabled);
            application.setEnvironment(env);
            application.getBeanFactory().registerSingleton("s2Accounts", accounts);
            application.getBeanFactory().registerSingleton("s2Encoder", encoder);
        }).withUserConfiguration(DemoAccountSeed.class);
    }

    @Test void disabledOrAbsentSeedFlagDoesNotCreateRunnerOrMutateFakePersistence() {
        for (String enabled : new String[]{null, "false"}) {
            var accounts = new MemoryAccounts(); var encoder = new RecordingEncoder();
            context(enabled, accounts, encoder).run(result -> {
                assertNull(result.getStartupFailure()); assertTrue(result.getBeansOfType(DemoAccountSeed.class).isEmpty());
            });
            assertTrue(accounts.mutations.isEmpty()); assertEquals(0, encoder.calls);
        }
    }

    @Test void creatingEnabledRunnerBeanAloneDoesNotExecuteApplicationRunner() {
        var accounts = new MemoryAccounts(); var encoder = new RecordingEncoder();
        context("true", accounts, encoder).run(result -> {
            assertNull(result.getStartupFailure()); assertEquals(1, result.getBeansOfType(DemoAccountSeed.class).size());
            assertTrue(accounts.mutations.isEmpty()); assertEquals(0, encoder.calls);
            assertThrows(IllegalStateException.class, () -> result.getBean(DemoAccountSeed.class).run(new DefaultApplicationArguments()));
        });
    }

    /** Required blank rejection: retain red evidence until separately approved production remediation. */
    @ParameterizedTest(name = "{0}") @MethodSource("allExistingAccounts")
    void whitespaceOnlySeedPasswordMustBeRejected(String label, int index) {
        var env = configured(); env.setProperty("demo.seed." + PASSWORD_KEYS[index], " ".repeat(12));
        var accounts = new MemoryAccounts();
        assertThrows(IllegalStateException.class,
            () -> new DemoAccountSeed(accounts, new RecordingEncoder(), env).run(new DefaultApplicationArguments()),
            "SECURITY_DEFECT: whitespace-only password was accepted for an absent demo account");
    }

    @ParameterizedTest(name = "{0}") @MethodSource("allExistingAccounts")
    void blankPasswordValidationPrecedesEncodingAndMutationOfTheAbsentAccount(String label, int index) {
        var env = configured(); env.setProperty("demo.seed." + PASSWORD_KEYS[index], " ".repeat(12));
        var accounts = new MemoryAccounts();
        for (int other = 0; other < USERS.length; other++) if (other != index) accounts.existing.add(USERS[other]);
        var encoder = new RecordingEncoder();
        var error = assertThrows(IllegalStateException.class,
            () -> new DemoAccountSeed(accounts, encoder, env).run(new DefaultApplicationArguments()));
        assertEquals("Configure all demo seed passwords (12–72 characters)", error.getMessage());
        assertFalse(error.toString().contains(" ".repeat(12)));
        assertFalse(error.toString().contains("S2-synthetic-seed-password!"));
        assertEquals(0, encoder.calls, "Invalid absent account must fail before encoding");
        assertTrue(accounts.mutations.isEmpty(), "No account is created in this isolated absent-account branch");
    }

    @Test void validPaddedPasswordReachesEncoderUnchangedAndSeedingRemainsIdempotent() {
        String padded = "  S2-synthetic-seed-password!  ";
        var env = configured();
        for (String key : PASSWORD_KEYS) env.setProperty("demo.seed." + key, padded);
        var encodedInputs = new ArrayList<String>();
        PasswordEncoder encoder = new PasswordEncoder() {
            @Override public String encode(CharSequence raw) { encodedInputs.add(raw.toString()); return "synthetic-test-hash"; }
            @Override public boolean matches(CharSequence raw, String hash) { throw new AssertionError("Not used"); }
        };
        var accounts = new MemoryAccounts(); var runner = new DemoAccountSeed(accounts, encoder, env);
        runner.run(new DefaultApplicationArguments());
        assertEquals(List.of(USERS), accounts.mutations);
        assertEquals(java.util.Collections.nCopies(5, padded), encodedInputs);
        for (String key : PASSWORD_KEYS) ((java.util.Map<?, ?>) env.getPropertySources().get("mockProperties").getSource()).remove("demo.seed." + key);
        runner.run(new DefaultApplicationArguments());
        assertEquals(5, accounts.mutations.size()); assertEquals(5, encodedInputs.size());
    }
}
