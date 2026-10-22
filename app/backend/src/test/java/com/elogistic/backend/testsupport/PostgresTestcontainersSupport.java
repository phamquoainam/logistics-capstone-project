package com.elogistic.backend.testsupport;

import java.nio.file.*;
import java.nio.file.attribute.PosixFilePermissions;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
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
    private static final Map<String,String> SUITES=Map.of(
        "com.elogistic.backend.demo.PostgresTestcontainersTests", "demo-postgres",
        "com.elogistic.backend.identity.internal.infrastructure.IdentityMigrationTests", "identity-migration",
        "com.elogistic.backend.identity.internal.infrastructure.IdentityGrantApplicationTests", "identity-grants");
    private static final String EXECUTION=executionIdentity();
    private static final Path PRIVATE=privateDirectory();

    private static String executionIdentity() {
        if ("true".equals(System.getenv("GITHUB_ACTIONS"))) {
            String run=System.getenv("GITHUB_RUN_ID"),attempt=System.getenv("GITHUB_RUN_ATTEMPT"),job=System.getenv("GITHUB_JOB");
            if (run == null || !run.matches("[1-9][0-9]*") || attempt == null || !attempt.matches("[1-9][0-9]*") ||
                job == null || !job.matches("[A-Za-z0-9_-]{1,80}")) throw new IllegalStateException("Invalid hosted execution identity");
            return "gha:"+run+":"+attempt+":"+job;
        }
        String local=System.getenv("TASK_C_EXECUTION_ID");
        if (local == null || !local.matches("[a-f0-9]{8}(-[a-f0-9]{4}){3}-[a-f0-9]{12}"))
            throw new IllegalStateException("Explicit local TASK_C_EXECUTION_ID is required");
        return "local:"+local;
    }

    private static List<String> envelope() {
        return List.of("format=task-c-lifecycle-v2", "execution="+EXECUTION,
            "taskRun="+TestcontainersRuntimePolicy.RUN, "session="+DockerClientFactory.SESSION_ID);
    }

    /** CREATE_NEW is exclusive: a truncated/failed publication remains blocking, never overwritten. */
    private static void writeNew(Path path,List<String> lines,boolean identicalAllowed) throws Exception {
        if (!Files.isDirectory(path.getParent(),LinkOption.NOFOLLOW_LINKS) || Files.isSymbolicLink(path.getParent()))
            throw new IllegalStateException("Lifecycle directory unavailable");
        byte[] bytes=(String.join("\n",lines)+"\n").getBytes(StandardCharsets.US_ASCII);
        if (identicalAllowed && Files.isRegularFile(path,LinkOption.NOFOLLOW_LINKS) && !Files.isSymbolicLink(path)) {
            if (!Arrays.equals(bytes,Files.readAllBytes(path))) throw new IllegalStateException("Lifecycle identity changed");
            return;
        }
        try(FileChannel channel=FileChannel.open(path,StandardOpenOption.CREATE_NEW,StandardOpenOption.WRITE)) {
            ByteBuffer buffer=ByteBuffer.wrap(bytes);while(buffer.hasRemaining()) channel.write(buffer);channel.force(true);
        }
    }

    private static Path privateDirectory() {
        String configured=System.getenv("TASK_C_PRIVATE_DIRECTORY");
        if (configured == null || configured.isBlank()) throw new IllegalStateException("TASK_C_PRIVATE_DIRECTORY is required");
        Path directory=Path.of(configured).toAbsolutePath().normalize();
        if (!Files.isDirectory(directory) || Files.isSymbolicLink(directory)) throw new IllegalStateException("Provide an existing private test directory");
        try {
            if (Files.exists(directory.resolve("task-c-runtime.txt"),LinkOption.NOFOLLOW_LINKS))
                throw new IllegalStateException("Use a fresh private test directory for each run");
            Path lifecycle=Files.createDirectory(directory.resolve("task-c-runtime.d"));
            if (Files.getFileStore(lifecycle).supportsFileAttributeView("posix"))
                Files.setPosixFilePermissions(lifecycle,PosixFilePermissions.fromString("rwx------"));
            var manifest=new ArrayList<>(envelope());
            for(String suite:new TreeSet<>(SUITES.keySet())) manifest.add("suite."+SUITES.get(suite)+"="+suite);
            writeNew(directory.resolve("task-c-runtime.txt"),manifest,false);
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
        // Existing suites need no API/assertion changes; bind the known <clinit> caller once.
        var callers=StackWalker.getInstance().walk(frames->frames.map(StackWalker.StackFrame::getClassName)
            .filter(SUITES::containsKey).distinct().toList());
        if (callers.size()!=1) throw new IllegalStateException("Unknown lifecycle suite");
        return new OwnedPostgres(callers.getFirst())
            .withUsername("task_c_bootstrap").withPassword(password()).withDatabaseName("postgres")
            .withCommand("postgres","-c","fsync=on","-c","synchronous_commit=on")
            .withStartupTimeout(Duration.ofSeconds(120)).withStartupAttempts(1).withReuse(false);
    }

    private static final class OwnedPostgres extends PostgreSQLContainer {
        private final String suite;
        private List<String> resources;

        OwnedPostgres(String suite) {
            super(DockerImageName.parse(TestcontainersRuntimePolicy.POSTGRES).asCompatibleSubstituteFor("postgres"));
            this.suite=suite;
            try { record("reserved",List.of(),false); }
            catch(Exception e) { throw new IllegalStateException("Cannot reserve suite lifecycle evidence"); }
        }

        private synchronized void record(String stage,List<String> facts,boolean identicalAllowed) throws Exception {
            var lines=new ArrayList<>(envelope());lines.add("suite="+suite);lines.add("stage="+stage);lines.addAll(facts);
            writeNew(PRIVATE.resolve("task-c-runtime.d").resolve(SUITES.get(suite)+"."+stage),lines,identicalAllowed);
        }

        private void failed(String reason) {
            try { record("failed",List.of("reason="+reason),true); }
            catch(Exception e) { throw new IllegalStateException("Lifecycle failure evidence unavailable"); }
        }

        @Override protected void containerIsCreated(String id) {
            super.containerIsCreated(id);
            try {
                if (!id.matches("[a-f0-9]{64}")) throw new IllegalStateException("Invalid container identity");
                // Observation is retained even if subsequent ownership inspection fails.
                // It never authorizes deletion: only validated resources below do that.
                record("observed",List.of("pgContainer="+id),false);
                if (!id.equals(getContainerId())) throw new IllegalStateException("Container identity mismatch");
                resources=resourceFacts(this,false);
                record("created",resources,false);
            } catch(Exception | AssertionError e) {
                failed("CREATION_UNVERIFIED");throw new IllegalStateException("Created resource provenance unavailable");
            }
        }

        @Override protected void containerIsStarted(com.github.dockerjava.api.command.InspectContainerResponse info) {
            super.containerIsStarted(info);
            try { verifyRuntime(this); }
            catch(Exception | AssertionError e) { failed("START_UNVERIFIED");throw new IllegalStateException("Runtime policy verification failed"); }
        }

        @Override public void start() {
            try { super.start(); }
            catch(RuntimeException | AssertionError e) { failed("START_FAILED");throw new IllegalStateException("Owned PostgreSQL startup failed; private evidence retained"); }
        }

        @Override public void stop() {
            try {
                if (resources==null) throw new IllegalStateException("Missing created resource identity");
                if (getContainerId()==null) {
                    requireAbsent(this,resources);record("stopped",resources,true);return;
                }
                // Reinspect labels, exact mount/volume, consumers and Ryuk before SDK deletion.
                if (!resources.equals(resourceFacts(this,false))) throw new IllegalStateException("Resource identity changed before stop");
                super.stop();
                requireAbsent(this,resources);
                record("stopped",resources,false);
            } catch(Exception | AssertionError e) {
                failed("STOP_UNVERIFIED");throw new IllegalStateException("Owned cleanup incomplete; no fallback deletion");
            }
        }
    }

    private static String fact(List<String> facts,String key) {
        return facts.stream().filter(s->s.startsWith(key+"=")).findFirst().orElseThrow().substring(key.length()+1);
    }

    private static void requireAbsent(PostgreSQLContainer pg,List<String> resources) {
        var client=pg.getDockerClient();String id=fact(resources,"pgContainer"),volume=fact(resources,"pgVolume");
        if (client.listContainersCmd().withShowAll(true).exec().stream().anyMatch(c->id.equals(c.getId())) ||
            client.listVolumesCmd().exec().getVolumes().stream().anyMatch(v->volume.equals(v.getName())))
            throw new IllegalStateException("Owned resources remain after stop");
    }

    public record Database(String name,String owner,String migrator,String runtime,String migrationPassword,String runtimePassword,PostgreSQLContainer container) {
        public String url() { return currentUrl(container,name); }
        @Override public String toString() { return "Task C database fixture (credentials withheld)"; }
        public Connection runtimeConnection() throws SQLException { return DriverManager.getConnection(url(),runtime,runtimePassword); }
        public Connection migrationConnection() throws SQLException { return DriverManager.getConnection(url(),migrator,migrationPassword); }
        public Flyway flyway(String target) {
            // Explicit synthetic registration for isolated test databases only; never production bootstrap.
            return flyway(target, UUID.fromString("10000000-0000-4000-8000-000000000001"));
        }
        public Flyway flyway(String target, UUID realm) {
            return Flyway.configure().dataSource(url(),migrator,migrationPassword).initSql("SET ROLE "+owner)
                .defaultSchema("platform").schemas("platform").createSchemas(false).locations("classpath:db/migration")
                .placeholders(Map.of("runtimeRole",runtime,"identitySystemRealmId",realm.toString()))
                .cleanDisabled(true).baselineOnMigrate(false).target(target).load();
        }
        /** Isolated synthetic fixture only: production activation uses the accepted invitation workflow. */
        public void ownerExecute(String sql) throws SQLException {
            try(Connection c=migrationConnection();Statement s=c.createStatement()) {
                s.execute("SET ROLE "+owner);s.execute(sql);
            }
        }
        public void completeInvitationForTest(UUID account) throws SQLException {
            ownerExecute("UPDATE identity.user_account SET status='ACTIVE',email_verified=TRUE,password_hash='$2b$12$"
                + ".".repeat(53)+"',activated_at=CURRENT_TIMESTAMP,updated_at=CURRENT_TIMESTAMP WHERE id='"+account+"'");
            ownerExecute("INSERT INTO identity.action_proof(id,account_id,purpose,token_hash,created_at,expires_at,consumed_at) VALUES ('"
                +UUID.randomUUID()+"','"+account+"','INVITATION',decode(repeat('ab',32),'hex'),CURRENT_TIMESTAMP,"
                +"CURRENT_TIMESTAMP+INTERVAL '24 hours',CURRENT_TIMESTAMP)");
        }
        public void activateRealmForTest(UUID realm) throws SQLException {
            UUID account=UUID.randomUUID();
            ownerExecute("SELECT identity.bootstrap_system_realm_admin('"+realm+"','"+account+"','realm.bootstrap.fixture',"
                +"'Synthetic Bootstrap','bootstrap@example.invalid','"+UUID.randomUUID()+"')");
            completeInvitationForTest(account);
            ownerExecute("SELECT identity.activate_system_realm('"+realm+"','"+account+"','"+UUID.randomUUID()+"')");
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
        if (!(pg instanceof OwnedPostgres owned)) throw new IllegalStateException("Unknown suite container");
        var facts=resourceFacts(pg,true);
        if (owned.resources==null || !owned.resources.equals(facts)) throw new IllegalStateException("Lifecycle resources changed");
        owned.record("verified",facts,true);
    }

    private static List<String> resourceFacts(PostgreSQLContainer pg,boolean running) throws Exception {
        verifyOwnership(pg);
        var client=pg.getDockerClient();
        var owned=client.listContainersCmd().withShowAll(true).withLabelFilter(Map.of(TestcontainersRuntimePolicy.LABEL,TestcontainersRuntimePolicy.RUN)).exec();
        assertEquals(2,owned.size(),"Exactly PostgreSQL and Ryuk are permitted");
        String pgId=null,volumeId=null,ryukId=null;
        for(var container:owned) {
            var i=client.inspectContainerCmd(container.getId()).exec();
            assertTrue(i.getId().matches("[a-f0-9]{64}"));
            assertEquals(TestcontainersRuntimePolicy.RUN,i.getConfig().getLabels().get(TestcontainersRuntimePolicy.LABEL));
            String image=i.getConfig().getImage();TestcontainersRuntimePolicy.requireApprovedImage(DockerImageName.parse(image));
            assertFalse(Boolean.TRUE.equals(i.getHostConfig().getPrivileged()));
            assertFalse("host".equals(i.getHostConfig().getNetworkMode()));
            if (running || image.equals(TestcontainersRuntimePolicy.RYUK)) {
                assertFalse(i.getNetworkSettings().getPorts().getBindings().isEmpty());
                for(var bindings:i.getNetworkSettings().getPorts().getBindings().values()) for(var binding:bindings) {
                    assertEquals("127.0.0.1",binding.getHostIp());assertTrue(Integer.parseInt(binding.getHostPortSpec())>0);
                }
            }
            var identity=client.inspectImageCmd(image).exec();
            assertEquals("linux",identity.getOs());assertEquals("amd64",identity.getArch());
            boolean digest=false;
            for(String entry:identity.getRepoDigests()) if(entry.endsWith(image.substring(image.indexOf('@')))) digest=true;
            assertTrue(digest,"Docker must confirm the approved manifest identity");
            if(image.equals(TestcontainersRuntimePolicy.RYUK)) {
                assertNull(ryukId);ryukId=i.getId();
                assertEquals("/testcontainers-ryuk-"+DockerClientFactory.SESSION_ID,i.getName());
                assertEquals(1,i.getMounts().size());
                var mount=i.getMounts().getFirst();assertEquals("/var/run/docker.sock",mount.getDestination().getPath());
                assertEquals("/var/run/docker.sock",mount.getSource().replaceFirst("^/+","/"));
            } else {
                assertNull(pgId);pgId=i.getId();assertEquals(pg.getContainerId(),pgId);
                assertEquals(DockerClientFactory.SESSION_ID,i.getConfig().getLabels().get("org.testcontainers.sessionId"));
                assertEquals(1,i.getMounts().size());
                var mount=i.getMounts().getFirst();assertNotNull(mount.getName());
                var volume=client.inspectVolumeCmd(mount.getName()).exec();
                assertEquals(volume.getMountpoint(),mount.getSource());assertEquals("local",volume.getDriver());
                assertEquals("/var/lib/postgresql",mount.getDestination().getPath());
                volumeId=mount.getName();assertTrue(volumeId.matches("[a-f0-9]{64}"));
                // Include stopped containers: a foreign consumer forbids SDK teardown.
                for(var consumer:client.listContainersCmd().withShowAll(true).exec()) {
                    var details=client.inspectContainerCmd(consumer.getId()).exec();
                    for(var attached:details.getMounts()) if(volumeId.equals(attached.getName())) assertEquals(pgId,consumer.getId());
                }
            }
        }
        TestcontainersRuntimePolicy.requireNoRejectedAttempts();
        assertNotNull(pgId);assertNotNull(volumeId);assertNotNull(ryukId);assertNotEquals(pgId,ryukId);
        return List.of("pgContainer="+pgId,"pgVolume="+volumeId,"volumeContainer="+pgId,
            "ryukContainer="+ryukId,"pgImage="+TestcontainersRuntimePolicy.POSTGRES,"ryukImage="+TestcontainersRuntimePolicy.RYUK,
            "pgLabelRun="+TestcontainersRuntimePolicy.RUN,"pgLabelSession="+DockerClientFactory.SESSION_ID,
            "ryukLabelRun="+TestcontainersRuntimePolicy.RUN,"ryukName=/testcontainers-ryuk-"+DockerClientFactory.SESSION_ID);
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
