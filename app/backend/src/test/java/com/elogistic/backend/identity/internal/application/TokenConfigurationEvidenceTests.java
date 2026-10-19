package com.elogistic.backend.identity.internal.application;

import java.nio.charset.StandardCharsets;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.core.io.support.ResourcePropertySource;
import org.springframework.mock.env.MockEnvironment;
import static org.junit.jupiter.api.Assertions.*;

/** S2 bean-context evidence only: no application scan, web server, DB or broker. */
@ExtendWith(OutputCaptureExtension.class)
class TokenConfigurationEvidenceTests {
    private ApplicationContextRunner context(String value) {
        return new ApplicationContextRunner().withInitializer(application -> {
            var env = new MockEnvironment(); // Never consult process credentials.
            env.setActiveProfiles("demo");
            try {
                env.getPropertySources().addLast(new ResourcePropertySource("classpath:application-demo.properties"));
            } catch (java.io.IOException error) {
                throw new IllegalStateException("Production demo properties must be readable", error);
            }
            if (value != null) env.setProperty("DEMO_TOKEN_SECRET", value);
            application.setEnvironment(env);
        }).withBean(org.springframework.context.support.PropertySourcesPlaceholderConfigurer.class,
            org.springframework.context.support.PropertySourcesPlaceholderConfigurer::new)
            .withUserConfiguration(TokenService.class);
    }

    private static Throwable root(Throwable error) {
        while (error.getCause() != null) error = error.getCause();
        return error;
    }

    @Test void missingEnvironmentPlaceholderRejectsContext(CapturedOutput output) {
        context(null).run(result -> {
            assertNotNull(result.getStartupFailure(), "Missing token configuration must reject context");
            var error = root(result.getStartupFailure());
            assertInstanceOf(IllegalArgumentException.class, error);
            assertTrue(error.getMessage().contains("DEMO_TOKEN_SECRET"));
        });
        assertFalse(output.getAll().contains("BEGIN PRIVATE KEY"));
    }

    static Stream<Arguments> shortValues() {
        return Stream.of(
            Arguments.of("empty", ""), Arguments.of("one-space", " "),
            Arguments.of("tabs-newline", "\t\r\n"),
            Arguments.of("ascii-31-bytes", "S2-synthetic-" + "a".repeat(17) + "!"),
            Arguments.of("unicode-31-bytes", "\u00e9".repeat(15) + "x"));
    }

    @ParameterizedTest(name = "{0}") @MethodSource("shortValues")
    void shortOrBlankSecretRejectsContextWithoutEcho(String label, String secret, CapturedOutput output) {
        assertTrue(secret.getBytes(StandardCharsets.UTF_8).length < 32, "Fixture must cover short byte branch");
        context(secret).run(result -> {
            assertNotNull(result.getStartupFailure(), "Short token configuration must reject context");
            var error = root(result.getStartupFailure());
            assertInstanceOf(IllegalStateException.class, error);
            assertEquals("DEMO_TOKEN_SECRET must contain at least 32 bytes", error.getMessage());
            if (!secret.isBlank()) assertFalse(error.toString().contains(secret), "Cause must not echo secret");
        });
        if (!secret.isBlank()) assertFalse(output.getAll().contains(secret), "Startup log must not echo secret");
    }

    static Stream<Arguments> validValues() {
        return Stream.of(
            Arguments.of("ascii-exact-32", "S2-synthetic-" + "a".repeat(18) + "!", 32),
            Arguments.of("ascii-above-33", "S2-synthetic-" + "a".repeat(19) + "!", 33),
            Arguments.of("unicode-exact-32", "\u00e9".repeat(16), 32),
            Arguments.of("unicode-above-33", "\u00e9".repeat(16) + "x", 33),
            Arguments.of("nontrivial-positive", "S2-only-Synthetic.Signing.Material_47!", 38),
            Arguments.of("padding-is-not-forbidden-by-current-policy", "  " + "S2-synthetic-" + "a".repeat(18) + "!" + "  ", 36));
    }

    @ParameterizedTest(name = "{0}") @MethodSource("validValues")
    void supportedByteBoundariesInstantiateService(String label, String secret, int bytes, CapturedOutput output) {
        assertEquals(bytes, secret.getBytes(StandardCharsets.UTF_8).length, "Synthetic byte-boundary fixture");
        context(secret).run(result -> {
            assertNull(result.getStartupFailure());
            assertNotNull(result.getBean(TokenService.class).decoder());
        });
        assertFalse(output.getAll().contains(secret), "Context log must not echo secret");
    }

    static Stream<Arguments> longBlankValues() {
        return Stream.of(Arguments.of("ascii-32-spaces", " ".repeat(32)),
            Arguments.of("ascii-33-spaces", " ".repeat(33)),
            Arguments.of("unicode-33-byte-blank", "\u2003".repeat(11)));
    }

    /** Deliberately red until separately approved production remediation; never suppress. */
    @ParameterizedTest(name = "{0}") @MethodSource("longBlankValues")
    void whitespaceOnlySecretMustRejectContext(String label, String secret) {
        assertTrue(secret.isBlank());
        assertTrue(secret.getBytes(StandardCharsets.UTF_8).length >= 32);
        context(secret).run(result -> assertNotNull(result.getStartupFailure(),
            "SECURITY_DEFECT: whitespace-only signing secret was accepted by the bean context"));
    }

    @ParameterizedTest(name = "{0}") @MethodSource("longBlankValues")
    void blankSecretFailsForValidationCauseWithoutValueDisclosure(String label, String secret, CapturedOutput output) {
        context(secret).run(result -> {
            assertNotNull(result.getStartupFailure());
            var error = root(result.getStartupFailure());
            assertInstanceOf(IllegalStateException.class, error);
            assertEquals("DEMO_TOKEN_SECRET must contain at least 32 bytes", error.getMessage());
            assertFalse(error.toString().contains(secret), "Validation error must not reproduce the input");
        });
        assertFalse(output.getAll().contains(secret), "Startup logs must not reproduce the input");
    }

    @Test void directNullConstructorInputRetainsExistingRejection() {
        assertThrows(NullPointerException.class, () -> new TokenService(null));
    }

    @Test void paddedSecretSignsAndVerifiesWithoutNormalization(CapturedOutput output) {
        String padded = "  S2-synthetic-signing-material-47!  ";
        var service = new TokenService(padded);
        var account = new com.elogistic.backend.identity.api.AccountView(java.util.UUID.randomUUID(),
            "s2.synthetic", "Synthetic", "MERCHANT", true, java.util.UUID.randomUUID());
        String encoded = service.issue(account);
        var decoded = service.decoder().decode(encoded);
        assertEquals(account.id().toString(), decoded.getSubject());
        assertEquals("elogistics-demo", decoded.getClaimAsString("iss"));
        assertEquals("HS256", decoded.getHeaders().get("alg"));
        assertEquals(900, java.time.Duration.between(decoded.getIssuedAt(), decoded.getExpiresAt()).toSeconds());
        assertNotNull(decoded.getId());
        assertThrows(org.springframework.security.oauth2.jwt.JwtException.class,
            () -> new TokenService(padded.trim()).decoder().decode(encoded),
            "The distinct trimmed key must not verify a token signed with the raw padded key");
        assertFalse(output.getAll().contains(padded));
        assertFalse(output.getAll().contains(encoded));
    }
}
