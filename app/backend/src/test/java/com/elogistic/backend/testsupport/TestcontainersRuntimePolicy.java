package com.elogistic.backend.testsupport;

import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.model.*;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import org.testcontainers.core.CreateContainerCmdModifier;
import org.testcontainers.utility.*;

/** Test-classpath SPI only; never included in the Spring Boot application artifact. */
public class TestcontainersRuntimePolicy extends ImageNameSubstitutor implements CreateContainerCmdModifier {
    public static final String POSTGRES = "postgres:18.6-bookworm@sha256:afc7e2d441324c0388fa80c3d24f733b4194a4eb7f47dd8ee2b08eb1a24a647c";
    public static final String RYUK = "testcontainers/ryuk:0.14.0@sha256:7c1a8a9a47c780ed0f983770a662f80deb115d95cce3e2daa3d12115b8cd28f0";
    public static final String LABEL = "com.elogistic.task-c.run";
    public static final String RUN = UUID.randomUUID().toString();
    private static final List<String> REJECTIONS = new CopyOnWriteArrayList<>();

    public static void requireSafeFlags(boolean disabledRyuk, boolean privileged, boolean disabledChecks, boolean reuse) {
        if (disabledRyuk || privileged || disabledChecks || reuse) throw new IllegalStateException("Unsafe Task C runtime settings");
    }

    public static void requireConfiguration() {
        if (!Boolean.getBoolean("task.c.enabled")) throw new IllegalStateException("Use the explicit testcontainers-postgres Maven profile");
        var c=TestcontainersConfiguration.getInstance();
        requireSafeFlags(Boolean.parseBoolean(System.getenv("TESTCONTAINERS_RYUK_DISABLED")),
            c.isRyukPrivileged(),c.isDisableChecks(),c.environmentSupportsReuse());
    }

    public static DockerImageName requireApprovedImage(DockerImageName image) {
        if (!Set.of(POSTGRES,RYUK).contains(image.asCanonicalNameString()))
            throw new IllegalArgumentException("Task C requires an exact approved immutable image");
        return image;
    }

    @Override public DockerImageName apply(DockerImageName original) {
        requireConfiguration();
        try { return requireApprovedImage(original); }
        catch (IllegalArgumentException e) { REJECTIONS.add("unapproved image attempt"); throw e; }
    }

    public static void requireNoRejectedAttempts() {
        if (!REJECTIONS.isEmpty()) throw new IllegalStateException("An unapproved helper was requested, even if upstream swallowed the error");
    }

    @Override protected String getDescription() { return "Task C immutable image allowlist"; }

    @Override public CreateContainerCmd modify(CreateContainerCmd command) {
        requireConfiguration();
        requireApprovedImage(DockerImageName.parse(command.getImage()));
        var host=command.getHostConfig();
        if (Boolean.TRUE.equals(host.getPrivileged()) || "host".equals(host.getNetworkMode()))
            throw new IllegalStateException("Privileged/host-network containers are forbidden");
        var bindings=new ArrayList<PortBinding>();
        for (ExposedPort port:command.getExposedPorts())
            bindings.add(new PortBinding(Ports.Binding.bindIpAndPort("127.0.0.1",0),port));
        host.withPortBindings(bindings);
        command.getLabels().put(LABEL,RUN);
        return command;
    }

    public static void requireOwned(String actualTaskRun, String actualSession, String expectedSession) {
        if (!RUN.equals(actualTaskRun) || !Objects.equals(expectedSession,actualSession) || expectedSession == null)
            throw new IllegalStateException("Refusing an operation on a foreign resource");
    }
}
