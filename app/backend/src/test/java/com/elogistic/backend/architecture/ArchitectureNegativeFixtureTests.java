package com.elogistic.backend.architecture;

import com.elogistic.backend.finance.api.*;
import com.elogistic.backend.finance.internal.application.*;
import com.elogistic.backend.finance.internal.domain.NegativeDomainAdapter;
import com.elogistic.backend.finance.internal.infrastructure.NegativeAdapter;
import com.elogistic.backend.finance.internal.infrastructure.NegativeInfrastructureWeb;
import com.elogistic.backend.finance.internal.web.*;
import com.elogistic.backend.identity.internal.application.NegativeModuleDirection;
import com.elogistic.backend.pricing.api.NegativePricingCycle;
import com.elogistic.backend.orders.internal.application.NegativeBroadIdentityContract;
import com.elogistic.backend.orders.internal.application.AllowedTrustedActorFixture;
import com.elogistic.backend.orders.internal.domain.NegativeDomainActor;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ArchitectureNegativeFixtureTests {
    private void detects(ArchRule rule, String expected, Class<?>... fixtureTypes) {
        var classes = new ClassFileImporter().importClasses(fixtureTypes);
        var result = rule.evaluate(classes);
        assertTrue(result.hasViolation(), "The production rule must detect this deliberate violation");
        String details = result.getFailureReport().toString();
        assertTrue(details.contains(expected), details);
        assertThrows(AssertionError.class, () -> rule.check(classes));
        System.out.println("ARCH-NEGATIVE: " + details);
    }

    @Test void financeCannotUseOrderInternalRepository() {
        detects(ArchitectureRules.MODULE_BOUNDARIES, "com.elogistic.backend.orders.internal.infrastructure.ShipmentRepository",
            NegativeInternalDependency.class);
    }
    @Test void identityCannotCallMerchantEvenThroughPublicApi() {
        detects(ArchitectureRules.MODULE_DIRECTION, "Unapproved module direction", NegativeModuleDirection.class);
    }
    @Test void publicApiCannotLeakOrderDomain() {
        detects(ArchitectureRules.API_ENCAPSULATION, "Public API leaks internal type", NegativeApiLeak.class);
    }
    @Test void moduleCycleIsDetected() {
        detects(ArchitectureRules.MODULE_CYCLES, "Cycle detected", NegativeFinanceCycle.class, NegativePricingCycle.class);
    }
    @Test void domainCannotUseJdbc() {
        detects(ArchitectureRules.DOMAIN_INDEPENDENCE, "Domain depends on outer layer", NegativeDomainAdapter.class);
    }
    @Test void applicationCannotDependOnItsAdapter() {
        detects(ArchitectureRules.APPLICATION_DIRECTION, "Application depends on outer adapter",
            NegativeApplicationAdapter.class, NegativeAdapter.class);
    }
    @Test void webCannotCallRepositoryAdapter() {
        detects(ArchitectureRules.WEB_DIRECTION, "Web depends on persistence adapter", NegativeWebAdapter.class, NegativeAdapter.class);
    }
    @Test void infrastructureCannotDependOnHttpAdapter() {
        detects(ArchitectureRules.INFRASTRUCTURE_DIRECTION, "Infrastructure depends on HTTP adapter",
            NegativeInfrastructureWeb.class, NegativeWebAdapter.class);
    }
    @Test void controllerCannotDeclareTransaction() {
        detects(ArchitectureRules.CONTROLLER_TRANSACTIONS, "HTTP controller owns a transaction", NegativeTransactionalController.class);
    }
    @Test void ordersCannotUseBroadAccountView() {
        detects(ArchitectureRules.MODULE_DIRECTION, "identity.api.AccountView", NegativeBroadIdentityContract.class);
    }
    @Test void ordersDomainCannotUseIdentityActor() {
        detects(ArchitectureRules.MODULE_DIRECTION, "identity.api.AuthenticatedActor", NegativeDomainActor.class);
    }
    @Test void minimalTrustedActorIsAllowedAtApplicationBoundary() {
        var classes = new ClassFileImporter().importClasses(AllowedTrustedActorFixture.class);
        ArchitectureRules.MODULE_DIRECTION.check(classes);
        ArchitectureRules.MODULE_BOUNDARIES.check(classes);
    }
}
