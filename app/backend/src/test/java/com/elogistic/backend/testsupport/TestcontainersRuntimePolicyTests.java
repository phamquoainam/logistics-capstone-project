package com.elogistic.backend.testsupport;

import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.model.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.testcontainers.utility.DockerImageName;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@Tag("task-c")
class TestcontainersRuntimePolicyTests {
    @Test void pinsApprovedImagesAndRejectsUnknownImages() {
        for(String image:List.of(TestcontainersRuntimePolicy.POSTGRES,TestcontainersRuntimePolicy.RYUK))
            assertEquals(image,TestcontainersRuntimePolicy.requireApprovedImage(DockerImageName.parse(image)).asCanonicalNameString());
        for(String image:List.of("postgres:18.6-bookworm","testcontainers/ryuk:0.14.0","alpine:3.17","postgres@sha256:"+"0".repeat(64)))
            assertThrows(IllegalArgumentException.class,()->TestcontainersRuntimePolicy.requireApprovedImage(DockerImageName.parse(image)));
    }

    @Test void rejectsDisabledRyukPrivilegedChecksAndReuse() {
        assertDoesNotThrow(()->TestcontainersRuntimePolicy.requireSafeFlags(false,false,false,false));
        assertThrows(IllegalStateException.class,()->TestcontainersRuntimePolicy.requireSafeFlags(true,false,false,false));
        assertThrows(IllegalStateException.class,()->TestcontainersRuntimePolicy.requireSafeFlags(false,true,false,false));
        assertThrows(IllegalStateException.class,()->TestcontainersRuntimePolicy.requireSafeFlags(false,false,true,false));
        assertThrows(IllegalStateException.class,()->TestcontainersRuntimePolicy.requireSafeFlags(false,false,false,true));
    }

    private CreateContainerCmd command(HostConfig host) {
        var command=mock(CreateContainerCmd.class);when(command.getImage()).thenReturn(TestcontainersRuntimePolicy.POSTGRES);
        when(command.getHostConfig()).thenReturn(host);when(command.getExposedPorts()).thenReturn(new ExposedPort[]{ExposedPort.tcp(5432)});
        when(command.getLabels()).thenReturn(new HashMap<>());return command;
    }

    @Test void rejectsPrivilegedAndHostNetworkCommands() {
        assertThrows(IllegalStateException.class,()->new TestcontainersRuntimePolicy().modify(command(HostConfig.newHostConfig().withPrivileged(true))));
        assertThrows(IllegalStateException.class,()->new TestcontainersRuntimePolicy().modify(command(HostConfig.newHostConfig().withNetworkMode("host"))));
    }

    @Test void bindsPublishedPortsToLoopbackWithRandomHostPorts() {
        var host=HostConfig.newHostConfig().withPortBindings(new PortBinding(Ports.Binding.bindPort(5433),ExposedPort.tcp(5432)));
        var command=command(host);assertSame(command,new TestcontainersRuntimePolicy().modify(command));
        var binding=host.getPortBindings().getBindings().get(ExposedPort.tcp(5432));
        assertEquals(1,binding.length);assertEquals("127.0.0.1",binding[0].getHostIp());assertEquals("0",binding[0].getHostPortSpec());
        assertEquals(TestcontainersRuntimePolicy.RUN,command.getLabels().get(TestcontainersRuntimePolicy.LABEL));
    }

    @Test void rejectsOperationsOnForeignResources() {
        assertDoesNotThrow(()->TestcontainersRuntimePolicy.requireOwned(TestcontainersRuntimePolicy.RUN,"session","session"));
        assertThrows(IllegalStateException.class,()->TestcontainersRuntimePolicy.requireOwned("foreign","session","session"));
        assertThrows(IllegalStateException.class,()->TestcontainersRuntimePolicy.requireOwned(TestcontainersRuntimePolicy.RUN,"foreign","session"));
        assertThrows(IllegalStateException.class,()->TestcontainersRuntimePolicy.requireOwned(TestcontainersRuntimePolicy.RUN,null,null));
    }

    @Test void policyDoesNotAddSocketMountsOrPrivilegeToDatabaseCommand() {
        var host=HostConfig.newHostConfig();new TestcontainersRuntimePolicy().modify(command(host));
        assertFalse(Boolean.TRUE.equals(host.getPrivileged()));assertTrue(host.getBinds()==null || host.getBinds().length==0);
    }
}
