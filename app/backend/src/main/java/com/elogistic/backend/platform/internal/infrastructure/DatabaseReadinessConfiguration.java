package com.elogistic.backend.platform.internal.infrastructure;

import com.zaxxer.hikari.HikariDataSource;
import java.sql.Connection;
import java.sql.SQLException;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/** Actuator extension using the application pool and JDBC-native scoped validation. */
@Configuration(proxyBeanMethods = false)
@Profile("demo")
class DatabaseReadinessConfiguration {
    @Bean("dbHealthIndicator")
    HealthIndicator databaseReadiness(HikariDataSource dataSource) {
        return () -> check(dataSource);
    }

    private static Health check(HikariDataSource dataSource) {
        try (Connection connection = dataSource.getConnection()) {
            try {
                int originalTimeout = connection.getNetworkTimeout();
                // pgJDBC42.7.13 scopes its socket timeout to this call and restores it.
                // The connection is exclusively borrowed, never shared with business SQL.
                if (connection.isValid(1) && !connection.isClosed()
                        && connection.getNetworkTimeout() == originalTimeout) {
                    return Health.up().build();
                }
                dataSource.evictConnection(connection);
            } catch (SQLException | RuntimeException failure) {
                // isValid can return false after closing the physical connection.
                // Invalid or unrestored connections must not return to another request.
                dataSource.evictConnection(connection);
            }
        } catch (SQLException | RuntimeException failure) {
            // Includes bounded acquisition/close/eviction failure. No exception details.
        }
        return Health.down().build();
    }
}
