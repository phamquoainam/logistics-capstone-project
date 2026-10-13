package com.elogistic.backend.platform.internal;

import org.flywaydb.core.Flyway;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/** Close the one-shot job only after startup migrations and validation have succeeded. */
@Component
@Profile("migration")
class MigrationJob implements ApplicationRunner {
 private final Flyway flyway;
 private final ConfigurableApplicationContext context;
 MigrationJob(Flyway flyway, ConfigurableApplicationContext context) { this.flyway=flyway; this.context=context; }
 @Override public void run(ApplicationArguments args) { flyway.validate(); context.close(); }
}
