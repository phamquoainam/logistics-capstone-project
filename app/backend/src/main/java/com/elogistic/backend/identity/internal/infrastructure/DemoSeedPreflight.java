package com.elogistic.backend.identity.internal.infrastructure;

import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.sql.init.dependency.DependsOnDatabaseInitialization;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/** Validate after database initialization, before refresh starts the HTTP connector.
 * Performs no seed writes; the transactional ApplicationRunner remains the writer.
 */
@Component
@Profile("demo")
@ConditionalOnProperty(name = "demo.seed.enabled", havingValue = "true")
@DependsOnDatabaseInitialization
class DemoSeedPreflight implements SmartInitializingSingleton {
    private final DemoAccountSeed seed;

    DemoSeedPreflight(DemoAccountSeed seed) {
        this.seed = seed;
    }

    @Override
    public void afterSingletonsInstantiated() {
        seed.validateConfiguration();
    }
}
