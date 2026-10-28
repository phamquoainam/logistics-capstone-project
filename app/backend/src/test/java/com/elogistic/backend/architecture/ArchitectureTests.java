package com.elogistic.backend.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import java.util.Set;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ArchitectureTests {
    private static final JavaClasses PRODUCTION = new ClassFileImporter()
        .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
        .importPackages(ArchitectureRules.ROOT);

    @Test void productionScopeIsRealAndExcludesNegativeFixtures() {
        Set<String> modules = PRODUCTION.stream().map(ArchitectureRules::packageModule)
            .filter(name -> !name.isEmpty()).collect(Collectors.toSet());
        assertEquals(Set.of("identity", "merchant", "orders", "platform", "workflow"), modules,
            "Update the evidence and implemented-module coverage when real modules are added");
        for (String module : modules) {
            assertTrue(PRODUCTION.stream().anyMatch(type -> ArchitectureRules.packageModule(type).equals(module)));
        }
        assertTrue(PRODUCTION.stream().noneMatch(type -> type.getSimpleName().contains("Negative")));
        assertTrue(PRODUCTION.stream().anyMatch(type -> type.getName().endsWith(".ShipmentService")));
        System.out.println("ARCH-SCOPE: " + PRODUCTION.size() + " production classes; modules=" + modules);
        for (String pattern : List.of("*.api..", "*.internal.application..", "*.internal.domain..", "*.internal.web..", "*.internal.infrastructure..")) {
            var scope = PRODUCTION.that(com.tngtech.archunit.core.domain.JavaClass.Predicates
                .resideInAPackage(ArchitectureRules.ROOT + "." + pattern));
            assertTrue(scope.size() > 0, "Empty architecture layer: " + pattern);
            System.out.println("ARCH-LAYER: " + pattern + "=" + scope.size());
        }
        assertTrue(PRODUCTION.stream().anyMatch(type -> type.isAnnotatedWith(org.springframework.web.bind.annotation.RestController.class)),
            "Controller rule must cover actual controllers");
    }

    @Test void packagesMatchApprovedModuleStructure() { ArchitectureRules.PACKAGE_SHAPE.check(PRODUCTION); }
    @Test void modulesOnlyUsePublicApis() { ArchitectureRules.MODULE_BOUNDARIES.check(PRODUCTION); }
    @Test void publicApisDoNotLeakInternals() { ArchitectureRules.API_ENCAPSULATION.check(PRODUCTION); }
    @Test void modulesFollowApprovedDependencyDirection() { ArchitectureRules.MODULE_DIRECTION.check(PRODUCTION); }
    @Test void domainIsIndependentOfOuterLayers() { ArchitectureRules.DOMAIN_INDEPENDENCE.check(PRODUCTION); }
    @Test void applicationDependsInward() { ArchitectureRules.APPLICATION_DIRECTION.check(PRODUCTION); }
    @Test void infrastructureDoesNotDependOnWeb() { ArchitectureRules.INFRASTRUCTURE_DIRECTION.check(PRODUCTION); }
    @Test void webDoesNotUsePersistenceAdapters() { ArchitectureRules.WEB_DIRECTION.check(PRODUCTION); }
    @Test void controllersDoNotOwnTransactions() { ArchitectureRules.CONTROLLER_TRANSACTIONS.check(PRODUCTION); }
    @Test void moduleGraphHasNoCycles() { ArchitectureRules.MODULE_CYCLES.check(PRODUCTION); }
}
