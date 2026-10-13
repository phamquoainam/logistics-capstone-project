package com.elogistic.backend.architecture;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import java.util.Map;
import java.util.Set;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RestController;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

/** Shared verbatim by production tests and isolated negative-fixture tests. */
final class ArchitectureRules {
    static final String ROOT = "com.elogistic.backend";
    private static final Map<String, String> MODULE_NAMES = Map.ofEntries(
        Map.entry("identity", "identity"), Map.entry("merchant", "merchant"),
        Map.entry("orders", "order"), Map.entry("order", "order"),
        Map.entry("pricing", "pricing"), Map.entry("wms", "warehouse"),
        Map.entry("warehouse", "warehouse"), Map.entry("finance", "finance"),
        Map.entry("dispatch", "dispatch"), Map.entry("routing", "routing"),
        Map.entry("notification", "notification"), Map.entry("notifications", "notification"),
        Map.entry("platform", "platform"), Map.entry("workflow", "workflow"));
    private static final Map<String, Set<String>> COLLABORATORS = Map.ofEntries(
        Map.entry("identity", Set.of()), Map.entry("merchant", Set.of("identity")),
        Map.entry("order", Set.of("merchant", "pricing")), Map.entry("pricing", Set.of()),
        Map.entry("warehouse", Set.of()), Map.entry("finance", Set.of()),
        Map.entry("dispatch", Set.of("order")), Map.entry("routing", Set.of("dispatch")),
        Map.entry("notification", Set.of("order", "warehouse", "finance", "dispatch", "routing", "identity")),
        Map.entry("platform", Set.of()),
        Map.entry("workflow", Set.of("identity", "merchant", "order", "pricing", "warehouse", "finance", "dispatch", "routing", "notification")));

    static String packageModule(JavaClass type) {
        String name = type.getPackageName();
        if (!name.startsWith(ROOT + ".")) return "";
        return name.substring(ROOT.length() + 1).split("\\.")[0];
    }

    private static String module(JavaClass type) {
        return MODULE_NAMES.getOrDefault(packageModule(type), packageModule(type));
    }

    private static boolean inLayer(JavaClass type, String layer) {
        String prefix = ROOT + "." + packageModule(type) + "." + layer;
        return type.getPackageName().equals(prefix) || type.getPackageName().startsWith(prefix + ".");
    }

    private static void reject(JavaClass source, JavaClass target, String reason, String description, ConditionEvents events) {
        events.add(SimpleConditionEvent.violated(source, reason + ": " + source.getName()
            + " -> " + target.getName() + "; " + description));
    }

    static final ArchRule PACKAGE_SHAPE = classes().should(new ArchCondition<>("use recognized modules and api/internal packages") {
        @Override public void check(JavaClass type, ConditionEvents events) {
            if (type.getName().equals(ROOT + ".BackendApplication")) return;
            String base = packageModule(type);
            if (!MODULE_NAMES.containsKey(base) || !(inLayer(type, "api") || inLayer(type, "internal"))) {
                events.add(SimpleConditionEvent.violated(type, "Unmapped production package: " + type.getName()));
            }
        }
    });

    static final ArchRule MODULE_BOUNDARIES = classes().should(new ArchCondition<>("access another module only through its api") {
        @Override public void check(JavaClass source, ConditionEvents events) {
            if (packageModule(source).isEmpty()) return; // The single checked composition root.
            source.getDirectDependenciesFromSelf().forEach(dependency -> {
                JavaClass target = dependency.getTargetClass();
                if (!packageModule(target).isEmpty() && !packageModule(source).equals(packageModule(target))
                        && !inLayer(target, "api")) {
                    reject(source, target, "Forbidden cross-module implementation dependency", dependency.getDescription(), events);
                }
            });
        }
    });

    static final ArchRule API_ENCAPSULATION = classes().that().resideInAPackage(ROOT + ".*.api..")
        .should(new ArchCondition<>("not expose or depend on internal implementation") {
            @Override public void check(JavaClass source, ConditionEvents events) {
                source.getDirectDependenciesFromSelf().forEach(dependency -> {
                    JavaClass target = dependency.getTargetClass();
                    if (inLayer(target, "internal")) {
                        reject(source, target, "Public API leaks internal type", dependency.getDescription(), events);
                    }
                });
            }
        });

    static final ArchRule MODULE_DIRECTION = classes().should(new ArchCondition<>("follow the approved module collaborator direction") {
        @Override public void check(JavaClass source, ConditionEvents events) {
            if (packageModule(source).isEmpty()) return;
            source.getDirectDependenciesFromSelf().forEach(dependency -> {
                JavaClass target = dependency.getTargetClass();
                String origin = module(source), destination = module(target);
                if (packageModule(target).isEmpty() || origin.equals(destination)) return;
                boolean technicalApi = !origin.equals("platform") && destination.equals("platform") && inLayer(target, "api");
                // ADR-0004: only this minimal security contract, only HTTP/application boundaries.
                boolean trustedActor = origin.equals("order")
                    && target.getName().equals(ROOT + ".identity.api.AuthenticatedActor")
                    && (inLayer(source, "internal.web") || inLayer(source, "internal.application"));
                if (!technicalApi && !trustedActor && !COLLABORATORS.getOrDefault(origin, Set.of()).contains(destination)) {
                    reject(source, target, "Unapproved module direction", dependency.getDescription(), events);
                }
            });
        }
    });

    static final ArchRule DOMAIN_INDEPENDENCE = classes().that().resideInAPackage(ROOT + ".*.internal.domain..")
        .should(new ArchCondition<>("remain independent of application, adapters and frameworks") {
            @Override public void check(JavaClass source, ConditionEvents events) {
                source.getDirectDependenciesFromSelf().forEach(dependency -> {
                    JavaClass target = dependency.getTargetClass();
                    String name = target.getName();
                    if (inLayer(target, "internal.application") || inLayer(target, "internal.infrastructure")
                            || inLayer(target, "internal.web") || name.startsWith("org.springframework.")
                            || name.startsWith("java.sql.") || name.startsWith("javax.sql.")
                            || name.startsWith("jakarta.persistence.")) {
                        reject(source, target, "Domain depends on outer layer", dependency.getDescription(), events);
                    }
                });
            }
        });

    static final ArchRule APPLICATION_DIRECTION = classes().that().resideInAPackage(ROOT + ".*.internal.application..")
        .should(new ArchCondition<>("depend inward on ports/domain, never web or persistence adapters") {
            @Override public void check(JavaClass source, ConditionEvents events) {
                source.getDirectDependenciesFromSelf().forEach(dependency -> {
                    JavaClass target = dependency.getTargetClass();
                    if (inLayer(target, "internal.web") || inLayer(target, "internal.infrastructure")
                            || target.getName().startsWith("org.springframework.jdbc.")
                            || target.getName().startsWith("java.sql.") || target.getName().startsWith("jakarta.persistence.")) {
                        reject(source, target, "Application depends on outer adapter", dependency.getDescription(), events);
                    }
                });
            }
        });

    static final ArchRule WEB_DIRECTION = classes().that().resideInAPackage(ROOT + ".*.internal.web..")
        .should(new ArchCondition<>("use application/domain/api, never persistence adapters") {
            @Override public void check(JavaClass source, ConditionEvents events) {
                source.getDirectDependenciesFromSelf().forEach(dependency -> {
                    JavaClass target = dependency.getTargetClass();
                    if (inLayer(target, "internal.infrastructure") || target.getName().startsWith("org.springframework.jdbc.")) {
                        reject(source, target, "Web depends on persistence adapter", dependency.getDescription(), events);
                    }
                });
            }
        });

    static final ArchRule CONTROLLER_TRANSACTIONS = classes().that().areAnnotatedWith(RestController.class)
        .should(new ArchCondition<>("leave transaction boundaries outside HTTP controllers") {
            @Override public void check(JavaClass type, ConditionEvents events) {
                if (type.isAnnotatedWith(Transactional.class)
                        || type.getMethods().stream().anyMatch(method -> method.isAnnotatedWith(Transactional.class))) {
                    events.add(SimpleConditionEvent.violated(type, "HTTP controller owns a transaction: " + type.getName()));
                }
            }
        });

    static final ArchRule INFRASTRUCTURE_DIRECTION = classes().that().resideInAPackage(ROOT + ".*.internal.infrastructure..")
        .should(new ArchCondition<>("implement inward contracts without depending on HTTP adapters") {
            @Override public void check(JavaClass source, ConditionEvents events) {
                source.getDirectDependenciesFromSelf().forEach(dependency -> {
                    JavaClass target = dependency.getTargetClass();
                    if (inLayer(target, "internal.web")) {
                        reject(source, target, "Infrastructure depends on HTTP adapter", dependency.getDescription(), events);
                    }
                });
            }
        });

    static final ArchRule MODULE_CYCLES = slices().matching(ROOT + ".(*)..").should().beFreeOfCycles();

    private ArchitectureRules() {}
}
