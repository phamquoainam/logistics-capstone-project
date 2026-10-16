package com.elogistic.backend.demo;

import com.zaxxer.hikari.HikariDataSource;
import java.sql.Connection;
import java.util.ArrayList;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.availability.AvailabilityChangeEvent;
import org.springframework.boot.availability.LivenessState;
import org.springframework.boot.availability.ReadinessState;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Uses the configured real H2/PostgreSQL DataSource; no mocked health indicator. */
class FoundationHealthTests extends DemoTestSupport {
    @Autowired ConfigurableApplicationContext context;
    @Autowired HikariDataSource dataSource;

    @Test void healthyProbesUseConfiguredDatabaseAndHideDetails() throws Exception {
        assertEquals(1, jdbc.queryForObject("SELECT 1", Integer.class));
        AvailabilityChangeEvent.publish(context, LivenessState.CORRECT);
        AvailabilityChangeEvent.publish(context, ReadinessState.ACCEPTING_TRAFFIC);
        for (String group : new String[]{"liveness", "readiness"}) {
            mvc.perform(get("/actuator/health/" + group)).andExpect(status().isOk())
                .andExpect(content().json("{\"status\":\"UP\"}", org.springframework.test.json.JsonCompareMode.STRICT));
        }
        assertEquals(2000, dataSource.getConnectionTimeout());
        assertEquals(1000, dataSource.getValidationTimeout());
        mvc.perform(get("/api/demo/v1/health")).andExpect(status().isOk())
            .andExpect(content().json("{\"status\":\"UP\",\"scope\":\"local-demo\"}", org.springframework.test.json.JsonCompareMode.STRICT));
    }

    @Test void refusingTrafficIs503WhileLivenessStaysUp() throws Exception {
        try {
            AvailabilityChangeEvent.publish(context, ReadinessState.REFUSING_TRAFFIC);
            mvc.perform(get("/actuator/health/readiness")).andExpect(status().isServiceUnavailable())
                .andExpect(content().json("{\"status\":\"OUT_OF_SERVICE\"}", org.springframework.test.json.JsonCompareMode.STRICT));
            mvc.perform(get("/actuator/health/liveness")).andExpect(status().isOk());
        } finally {
            AvailabilityChangeEvent.publish(context, ReadinessState.ACCEPTING_TRAFFIC);
        }
    }

    @Test void onlyExactAnonymousGetProbesAreAllowed() throws Exception {
        for (String path : new String[]{"/actuator", "/actuator/health", "/actuator/health/db",
                "/actuator/health/readiness/db", "/actuator/env", "/actuator/beans", "/actuator/metrics"}) {
            mvc.perform(get(path)).andExpect(status().isUnauthorized());
        }
        for (String method : new String[]{"POST", "PUT", "DELETE", "HEAD"}) {
            mvc.perform(MockMvcRequestBuilders.request(org.springframework.http.HttpMethod.valueOf(method),
                "/actuator/health/readiness")).andExpect(status().isUnauthorized());
        }
        String token = login("demo.admin");
        for (String path : new String[]{"/actuator/health", "/actuator/env", "/actuator/health/readiness/db"}) {
            mvc.perform(get(path).header("Authorization", "Bearer " + token)).andExpect(status().isForbidden());
        }
    }

    @Test void exhaustedRealPoolReturns503WithinAcquisitionBudgetAndRecovers() throws Exception {
        AvailabilityChangeEvent.publish(context, ReadinessState.ACCEPTING_TRAFFIC);
        var held = new ArrayList<Connection>();
        try {
            for (int i = 0; i < dataSource.getMaximumPoolSize(); i++) held.add(dataSource.getConnection());
            long start = System.nanoTime();
            mvc.perform(get("/actuator/health/readiness")).andExpect(status().isServiceUnavailable())
                .andExpect(content().json("{\"status\":\"DOWN\"}", org.springframework.test.json.JsonCompareMode.STRICT));
            long millis = (System.nanoTime() - start) / 1_000_000;
            assertTrue(millis >= 1500 && millis < 8000, "Pool timeout must be bounded; actual ms=" + millis);
            mvc.perform(get("/actuator/health/liveness")).andExpect(status().isOk());
        } finally {
            for (Connection connection : held) connection.close();
        }
        mvc.perform(get("/actuator/health/readiness")).andExpect(status().isOk());
    }
}
