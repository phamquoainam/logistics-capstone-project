package com.elogistic.backend.testsupport;

import java.nio.file.*;
import java.nio.file.attribute.PosixFilePermissions;
import java.security.SecureRandom;
import java.sql.*;
import java.time.Duration;
import java.util.*;
import org.flywaydb.core.Flyway;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;
import static org.junit.jupiter.api.Assertions.*;

/** Synthetic fixture; no Spring context, developer URL, fixed host port or shared volume. */
public final class PostgresTestcontainersSupport {
    private PostgresTestcontainersSupport() {}
    private static final SecureRandom RANDOM=new SecureRandom();
    private static final Path PRIVATE=privateDirectory();

    private static Path privateDirectory() {
        String configured=System.getenv("TASK_C_PRIVATE_DIRECTORY");
        if (configured == null || configured.isBlank()) throw new IllegalStateException("TASK_C_PRIVATE_DIRECTORY is required");
        Path directory=Path.of(configured).toAbsolutePath().normalize();
        if (!Files.isDirectory(directory) || Files.isSymbolicLink(directory)) throw new IllegalStateException("Provide an existing private test directory");
        try {
            if (Files.exists(directory.resolve("task-c-runtime.txt"),LinkOption.NOFOLLOW_LINKS))
                throw new IllegalStateException("Use a fresh private test directory for each run");
            Path credentials=Files.createFile(directory.resolve("task-c-secrets.txt"));
            if (Files.getFileStore(credentials).supportsFileAttributeView("posix"))
                Files.setPosixFilePermissions(credentials,PosixFilePermissions.fromString("rw-------"));
        } catch (Exception e) { throw new IllegalStateException("Cannot initialize fresh private credential evidence"); }
        return directory;
    }

    public static String password() {
        byte[] bytes=new byte[32];RANDOM.nextBytes(bytes);
        String value=Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        try {
            Path path=PRIVATE.resolve("task-c-secrets.txt");
            Files.writeString(path,value+"\n",StandardOpenOption.APPEND);
            if (Files.getFileStore(path).supportsFileAttributeView("posix"))
                Files.setPosixFilePermissions(path,PosixFilePermissions.fromString("rw-------"));
        } catch (Exception e) { throw new IllegalStateException("Cannot register synthetic test credentials for privacy audit"); }
        return value;
    }

    public static PostgreSQLContainer container() {
        TestcontainersRuntimePolicy.requireConfiguration();
        return new PostgreSQLContainer(DockerImageName.parse(TestcontainersRuntimePolicy.POSTGRES).asCompatibleSubstituteFor("postgres"))
            .withUsername("task_c_bootstrap").withPassword(password()).withDatabaseName("postgres")
            .withCommand("postgres","-c","fsync=on","-c","synchronous_commit=on")
            .withStartupTimeout(Duration.ofSeconds(120)).withReuse(false);
    }

    public record Database(String name,String owner,String migrator,String runtime,String migrationPassword,String runtimePassword,PostgreSQLContainer container) {
        public String url() { return currentUrl(container,name); }
        @Override public String toString() { return "Task C database fixture (credentials withheld)"; }
        public Connection runtimeConnection() throws SQLException { return DriverManager.getConnection(url(),runtime,runtimePassword); }
        public Connection migrationConnection() throws SQLException { return DriverManager.getConnection(url(),migrator,migrationPassword); }
        public Flyway flyway(String target) {
            return Flyway.configure().dataSource(url(),migrator,migrationPassword).initSql("SET ROLE "+owner)
                .defaultSchema("platform").schemas("platform").createSchemas(false).locations("classpath:db/migration")
                .placeholders(Map.of("runtimeRole",runtime)).cleanDisabled(true).baselineOnMigrate(false).target(target).load();
        }
    }

    public static Database database(PostgreSQLContainer pg) throws SQLException {
        verifyOwnership(pg);
        String suffix=UUID.randomUUID().toString().replace("-","");
        String name="task_c_"+suffix,owner="owner_"+suffix,migrator="migrator_"+suffix,runtime="runtime_"+suffix;
        String migrationPassword=password(),runtimePassword=password();
        try(Connection c=DriverManager.getConnection(currentUrl(pg,"postgres"),pg.getUsername(),pg.getPassword());Statement s=c.createStatement()) {
            s.execute("CREATE ROLE "+owner+" NOLOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE");
            s.execute("CREATE ROLE "+migrator+" LOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE PASSWORD '"+migrationPassword+"'");
            s.execute("CREATE ROLE "+runtime+" LOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE PASSWORD '"+runtimePassword+"'");
            s.execute("GRANT "+owner+" TO "+migrator);
            s.execute("CREATE DATABASE "+name+" OWNER "+owner);
            s.execute("REVOKE ALL ON DATABASE "+name+" FROM PUBLIC");
            s.execute("GRANT CONNECT ON DATABASE "+name+" TO "+migrator+","+runtime);
        }
        String url=currentUrl(pg,name);
        try(Connection c=DriverManager.getConnection(url,pg.getUsername(),pg.getPassword());Statement s=c.createStatement()) {
            s.execute("REVOKE ALL ON SCHEMA public FROM PUBLIC");
            s.execute("CREATE SCHEMA platform AUTHORIZATION "+owner);
        }
        return new Database(name,owner,migrator,runtime,migrationPassword,runtimePassword,pg);
    }

    public static String currentUrl(PostgreSQLContainer pg,String database) {
        // Docker may allocate a new random host port on restart. Never use a stale cached mapping.
        verifyOwnership(pg);
        var i=pg.getDockerClient().inspectContainerCmd(pg.getContainerId()).exec();
        var bindings=i.getNetworkSettings().getPorts().getBindings().get(com.github.dockerjava.api.model.ExposedPort.tcp(5432));
        assertNotNull(bindings);assertEquals(1,bindings.length);assertEquals("127.0.0.1",bindings[0].getHostIp());
        return "jdbc:postgresql://127.0.0.1:"+Integer.parseInt(bindings[0].getHostPortSpec())+"/"+database+"?connectTimeout=2";
    }

    public static void verifyOwnership(PostgreSQLContainer pg) {
        var i=pg.getDockerClient().inspectContainerCmd(pg.getContainerId()).exec();
        TestcontainersRuntimePolicy.requireOwned(i.getConfig().getLabels().get(TestcontainersRuntimePolicy.LABEL),
            i.getConfig().getLabels().get("org.testcontainers.sessionId"),DockerClientFactory.SESSION_ID);
    }

    public static void verifyRuntime(PostgreSQLContainer pg) throws Exception {
        verifyOwnership(pg);
        var client=pg.getDockerClient();
        var owned=client.listContainersCmd().withLabelFilter(Map.of(TestcontainersRuntimePolicy.LABEL,TestcontainersRuntimePolicy.RUN)).exec();
        assertEquals(2,owned.size(),"Exactly PostgreSQL and Ryuk are permitted");
        var proof=new ArrayList<String>();
        for(var container:owned) {
            var i=client.inspectContainerCmd(container.getId()).exec();
            String image=i.getConfig().getImage();TestcontainersRuntimePolicy.requireApprovedImage(DockerImageName.parse(image));
            assertFalse(Boolean.TRUE.equals(i.getHostConfig().getPrivileged()));
            assertFalse("host".equals(i.getHostConfig().getNetworkMode()));
            assertFalse(i.getNetworkSettings().getPorts().getBindings().isEmpty());
            for(var bindings:i.getNetworkSettings().getPorts().getBindings().values()) for(var binding:bindings) {
                assertEquals("127.0.0.1",binding.getHostIp());assertTrue(Integer.parseInt(binding.getHostPortSpec())>0);
            }
            var identity=client.inspectImageCmd(image).exec();
            assertEquals("linux",identity.getOs());assertEquals("amd64",identity.getArch());
            boolean digest=false;
            for(String entry:identity.getRepoDigests()) if(entry.endsWith(image.substring(image.indexOf('@')))) digest=true;
            assertTrue(digest,"Docker must confirm the approved manifest identity");
            if(image.equals(TestcontainersRuntimePolicy.RYUK)) {
                assertEquals("/testcontainers-ryuk-"+DockerClientFactory.SESSION_ID,i.getName());
                assertEquals(1,i.getMounts().size());
                var mount=i.getMounts().getFirst();assertEquals("/var/run/docker.sock",mount.getDestination().getPath());
                assertEquals("/var/run/docker.sock",mount.getSource().replaceFirst("^/+","/"));
            } else {
                assertEquals(1,i.getMounts().size());
                var mount=i.getMounts().getFirst();assertNotNull(mount.getName());
                var volume=client.inspectVolumeCmd(mount.getName()).exec();
                assertEquals(volume.getMountpoint(),mount.getSource());assertEquals("local",volume.getDriver());
                assertEquals("/var/lib/postgresql",mount.getDestination().getPath());
                proof.add("anonymousVolume="+mount.getName());
            }
            proof.add("image="+image+" id="+container.getId()+" privileged=false ports=loopback");
        }
        TestcontainersRuntimePolicy.requireNoRejectedAttempts();
        proof.add("session="+DockerClientFactory.SESSION_ID);
        proof.add("taskRun="+TestcontainersRuntimePolicy.RUN);
        Files.write(PRIVATE.resolve("task-c-runtime.txt"),proof);
    }

    public static void restart(PostgreSQLContainer pg) throws Exception {
        verifyOwnership(pg);
        pg.getDockerClient().restartContainerCmd(pg.getContainerId()).withTimeout(10).exec();
        long end=System.nanoTime()+Duration.ofSeconds(30).toNanos();
        while(System.nanoTime()<end) {
            try(Connection c=DriverManager.getConnection(currentUrl(pg,"postgres"),pg.getUsername(),pg.getPassword())) {
                if(c.isValid(1)) return;
            } catch(SQLException ignored) { /* Bounded readiness retry, not a skipped assertion. */ }
            Thread.sleep(100);
        }
        throw new IllegalStateException("Owned PostgreSQL did not recover after restart");
    }
}
