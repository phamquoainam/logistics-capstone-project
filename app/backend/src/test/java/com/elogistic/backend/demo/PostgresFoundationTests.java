package com.elogistic.backend.demo;

import java.sql.*;
import java.util.*;
import java.util.concurrent.*;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import com.jayway.jsonpath.JsonPath;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@EnabledIfSystemProperty(named="demo.test.database", matches="postgres")
class PostgresFoundationTests extends DemoTestSupport {
 @Autowired Flyway flyway;
 @Autowired DataSource dataSource;
 @Autowired PlatformTransactionManager manager;

 @Test void actualPostgresAndSeparateMigrationPrincipal() throws Exception {
  try (Connection connection=dataSource.getConnection()) {
   assertEquals("PostgreSQL",connection.getMetaData().getDatabaseProductName());
   assertEquals(18,connection.getMetaData().getDatabaseMajorVersion());
   assertEquals(requiredEnv("PG_TEST_RUNTIME_USER"),connection.getMetaData().getUserName());
  }
  try(Connection connection=flyway.getConfiguration().getDataSource().getConnection(); Statement sql=connection.createStatement()) {
   ResultSet result=sql.executeQuery("SELECT session_user,current_user");assertTrue(result.next());
   assertEquals(requiredEnv("PG_TEST_MIGRATOR_USER"),result.getString(1));
   assertEquals(requiredEnv("PG_TEST_MIGRATOR_USER"),result.getString(2));
   result.close();
   // Flyway executes initSql when it opens its migration session, not on raw getConnection().
   assertEquals("SET ROLE "+requiredEnv("PG_TEST_OWNER_ROLE"),flyway.getConfiguration().getInitSql());
   sql.execute(flyway.getConfiguration().getInitSql());
   try(ResultSet owner=sql.executeQuery("SELECT current_user")) { assertTrue(owner.next());assertEquals(requiredEnv("PG_TEST_OWNER_ROLE"),owner.getString(1)); }
  }
 }

 @Test void nineSchemasAndTablesBelongToOwner() {
  List<String> schemas=jdbc.queryForList("SELECT nspname FROM pg_namespace WHERE nspname IN ('identity','merchant','orders','pricing','warehouse','dispatch','finance','routing','notification') ORDER BY nspname",String.class);
  assertEquals(List.of("dispatch","finance","identity","merchant","notification","orders","pricing","routing","warehouse"),schemas);
  assertEquals(10,jdbc.queryForObject("SELECT count(*) FROM pg_namespace n JOIN pg_roles r ON r.oid=n.nspowner WHERE r.rolname=? AND n.nspname IN ('platform','identity','merchant','orders','pricing','warehouse','dispatch','finance','routing','notification')",Integer.class,requiredEnv("PG_TEST_OWNER_ROLE")));
  // Five Demo + twelve accepted Identity + eight isolated customer tables + Flyway history.
  assertEquals(26,jdbc.queryForObject("SELECT count(*) FROM pg_tables WHERE tableowner=? AND schemaname IN ('platform','identity','merchant','orders')",Integer.class,requiredEnv("PG_TEST_OWNER_ROLE")));
 }

 @Test void runtimeCannotDdlAssumeOwnerOrReadHistory() throws Exception {
  for(String statement:List.of("CREATE SCHEMA forbidden_runtime", "CREATE TABLE orders.forbidden_runtime(id integer)", "CREATE TABLE public.forbidden_runtime(id integer)", "ALTER TABLE orders.demo_shipment ADD COLUMN forbidden integer", "DROP TABLE orders.demo_event", "TRUNCATE orders.demo_shipment", "SET ROLE "+requiredEnv("PG_TEST_OWNER_ROLE"), "SELECT * FROM platform.flyway_schema_history")) {
   try(Connection connection=dataSource.getConnection(); Statement sql=connection.createStatement()) {
    SQLException error=assertThrows(SQLException.class,()->sql.execute(statement)); assertEquals("42501",error.getSQLState());
   }
  }
  assertFalse(jdbc.queryForObject("SELECT rolcanlogin FROM pg_roles WHERE rolname=?",Boolean.class,requiredEnv("PG_TEST_OWNER_ROLE")));
  for(String role:List.of(requiredEnv("PG_TEST_RUNTIME_USER"),requiredEnv("PG_TEST_MIGRATOR_USER"))) {
   assertFalse(jdbc.queryForObject("SELECT rolsuper OR rolcreatedb OR rolcreaterole FROM pg_roles WHERE rolname=?",Boolean.class,role));
  }
 }

 @Test void eventsAndReceiptsCannotBeChangedByRuntime() throws Exception {
  for(String statement:List.of("UPDATE orders.demo_event SET reason='forged'", "DELETE FROM orders.demo_event", "UPDATE orders.demo_operation SET payload_hash='forged'", "DELETE FROM orders.demo_operation")) {
   try(Connection connection=dataSource.getConnection(); Statement sql=connection.createStatement()) {
    SQLException error=assertThrows(SQLException.class,()->sql.execute(statement));assertEquals("42501",error.getSQLState());
   }
  }
 }

 @Test void validateAndRerunPreserveDataAndChecksums() {
  long before=jdbc.queryForObject("SELECT COUNT(*) FROM identity.demo_account",Long.class);
  var applied=Arrays.stream(flyway.info().applied()).map(m->m.getVersion()+":"+m.getChecksum()).toList();
  assertEquals(List.of("1","2","3","4","5","6","7","8"),Arrays.stream(flyway.info().applied()).map(m->m.getVersion().toString()).toList());assertTrue(flyway.validateWithResult().validationSuccessful);
  assertEquals(0,flyway.migrate().migrationsExecuted);
  assertEquals(applied,Arrays.stream(flyway.info().applied()).map(m->m.getVersion()+":"+m.getChecksum()).toList());
  assertEquals(before,jdbc.queryForObject("SELECT COUNT(*) FROM identity.demo_account",Long.class));
 }

 @Test void versionOneToTwoUpgradePreservesExistingTechnicalData() throws Exception {
  String url=requiredEnv("PG_TEST_UPGRADE_DB_URL");
  assertTrue(url.matches("jdbc:postgresql://127\\.0\\.0\\.1:5433/elogistics_test_upgrade_[a-z0-9_]+"));
  var configuration=Flyway.configure().dataSource(url,requiredEnv("PG_TEST_MIGRATOR_USER"),requiredEnv("PG_TEST_MIGRATOR_PASSWORD"))
   .initSql("SET ROLE "+requiredEnv("PG_TEST_OWNER_ROLE")).defaultSchema("platform").schemas("platform").createSchemas(false)
   .locations("classpath:db/migration").placeholders(Map.of("runtimeRole",requiredEnv("PG_TEST_RUNTIME_USER"))).cleanDisabled(true).baselineOnMigrate(false);
  Flyway versionOne=configuration.target("1").load();assertEquals(1,versionOne.migrate().migrationsExecuted);
  try(Connection connection=versionOne.getConfiguration().getDataSource().getConnection();Statement sql=connection.createStatement()) {
   sql.execute("CREATE TABLE platform.upgrade_probe(id integer PRIMARY KEY, value text NOT NULL)");
   sql.execute("INSERT INTO platform.upgrade_probe VALUES (1,'preserve-upgrade')");
  }
  // Keep this historical V1 -> V2 regression bounded; IdentityMigrationTests covers V2 -> V3.
  Flyway latest=configuration.target("2").load();assertEquals(1,latest.migrate().migrationsExecuted);latest.validate();
  try(Connection connection=latest.getConfiguration().getDataSource().getConnection();Statement sql=connection.createStatement()) {
   ResultSet result=sql.executeQuery("SELECT value FROM platform.upgrade_probe WHERE id=1");assertTrue(result.next());assertEquals("preserve-upgrade",result.getString(1));
  }
  assertEquals(0,latest.migrate().migrationsExecuted);
 }

 @Test void failedForeignKeyRollsBackWholeTransaction() throws Exception {
  String token=login("demo.merchant.a"),reference="ROLLBACK-"+UUID.randomUUID();
  String body=new DemoShipmentTests().input(reference);
  String response=mvc.perform(post("/api/demo/v1/shipments").header("Authorization","Bearer "+token).header("Idempotency-Key",UUID.randomUUID().toString()).contentType("application/json").content(body)).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
  UUID id=UUID.fromString(JsonPath.read(response,"$.id"));
  assertThrows(DataIntegrityViolationException.class,()->new TransactionTemplate(manager).execute(tx->{
   jdbc.update("UPDATE orders.demo_shipment SET reference=? WHERE id=?","SHOULD-ROLLBACK",id);
   jdbc.update("INSERT INTO orders.demo_event VALUES (?,?,?,?,?,?)",UUID.randomUUID(),UUID.randomUUID(),"DRAFT_CREATED",UUID.randomUUID(),null,Timestamp.from(java.time.Instant.now()));
   return null;
  }));
  assertEquals(reference,jdbc.queryForObject("SELECT reference FROM orders.demo_shipment WHERE id=?",String.class,id));
  assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM orders.demo_event WHERE shipment_id=?",Integer.class,id));
 }

 @Test void concurrentCancelHasOneEventAndReceipt() throws Exception {
  String token=login("demo.merchant.a"),key=UUID.randomUUID().toString();
  String response=mvc.perform(post("/api/demo/v1/shipments").header("Authorization","Bearer "+token).header("Idempotency-Key",UUID.randomUUID().toString()).contentType("application/json").content(new DemoShipmentTests().input("CANCEL-RACE-"+UUID.randomUUID()))).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
  String id=JsonPath.read(response,"$.id");
  try(ExecutorService executor=Executors.newFixedThreadPool(2)) {
   CountDownLatch start=new CountDownLatch(1);
   Callable<String> request=()->{start.await();return mvc.perform(post("/api/demo/v1/shipments/"+id+"/cancel").header("Authorization","Bearer "+token).header("Idempotency-Key",key).contentType("application/json").content("{\"reason\":\"Concurrent cancellation\"}")).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("CANCELLED")).andReturn().getResponse().getContentAsString();};
   Future<String> a=executor.submit(request),b=executor.submit(request);start.countDown();assertEquals(a.get(15,TimeUnit.SECONDS),b.get(15,TimeUnit.SECONDS));
  }
  assertEquals(2,jdbc.queryForObject("SELECT count(*) FROM orders.demo_event WHERE shipment_id=?",Integer.class,UUID.fromString(id)));
  assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM orders.demo_operation WHERE shipment_id=? AND operation=?",Integer.class,UUID.fromString(id),"cancel:"+id));
 }
}
