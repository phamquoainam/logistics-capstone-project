package com.elogistic.backend.demo;

import com.elogistic.backend.identity.internal.application.TokenService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(OutputCaptureExtension.class)
class DemoSecurityEvidenceTests extends DemoTestSupport {
    @Test void credentialsAndBearerTokenDoNotAppearInApplicationLogs(CapturedOutput output) throws Exception {
        String token = login("demo.merchant.a");
        mvc.perform(get("/api/demo/v1/shipments").header("Authorization", "Bearer " + token))
            .andExpect(status().isOk());
        mvc.perform(post("/api/demo/v1/auth/login").contentType("application/json")
            .content("{\"username\":\"demo.merchant.a\",\"password\":\"invalid-log-probe\"}"))
            .andExpect(status().isUnauthorized());
        assertFalse(output.getAll().contains(PASSWORD), "Seed/login password leaked to application logs");
        assertFalse(output.getAll().contains(TOKEN_SECRET), "Signing secret leaked to application logs");
        assertFalse(output.getAll().contains(token), "Bearer token leaked to application logs");
        assertFalse(output.getAll().contains("invalid-log-probe"), "Rejected password leaked to application logs");
    }

    @Test void signingConfigurationFailsFastWhenMissingOrWeak() {
        var context = new ApplicationContextRunner()
            .withInitializer(application -> application.getEnvironment().setActiveProfiles("demo"))
            .withUserConfiguration(TokenService.class);
        context.run(result -> assertNotNull(result.getStartupFailure(), "Missing signing secret must prevent startup"));
        context.withPropertyValues("demo.token-secret=too-short").run(result -> {
            assertNotNull(result.getStartupFailure(), "Weak signing secret must prevent startup");
            Throwable root = result.getStartupFailure();
            while (root.getCause() != null) root = root.getCause();
            assertInstanceOf(IllegalStateException.class, root);
            assertEquals("DEMO_TOKEN_SECRET must contain at least 32 bytes", root.getMessage());
        });
        context.withPropertyValues("demo.token-secret=" + java.util.UUID.randomUUID()).run(result -> {
            assertNull(result.getStartupFailure());
            assertNotNull(result.getBean(TokenService.class), "The active demo profile must actually instantiate the service");
        });
    }
}
