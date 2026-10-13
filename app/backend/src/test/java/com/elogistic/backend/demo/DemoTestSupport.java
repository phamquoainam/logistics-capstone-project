package com.elogistic.backend.demo;
import com.jayway.jsonpath.JsonPath;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("demo")
abstract class DemoTestSupport {
 static final String PASSWORD=UUID.randomUUID().toString();
 static final String TOKEN_SECRET=UUID.randomUUID().toString()+UUID.randomUUID();
 @Autowired MockMvc mvc;
 @Autowired JdbcTemplate jdbc;
 @DynamicPropertySource static void config(DynamicPropertyRegistry registry){
  if ("postgres".equals(System.getProperty("demo.test.database"))) {
   String url=requiredEnv("PG_TEST_DB_URL");
   if(!url.matches("jdbc:postgresql://127\\.0\\.0\\.1:5433/elogistics_test_[a-z0-9_]+")) throw new IllegalStateException("Use an isolated foundation test database on port 5433");
   registry.add("spring.datasource.url",()->url);
   registry.add("spring.datasource.username",()->requiredEnv("PG_TEST_RUNTIME_USER"));
   registry.add("spring.datasource.password",()->requiredEnv("PG_TEST_RUNTIME_PASSWORD"));
   registry.add("spring.datasource.driver-class-name",()->"org.postgresql.Driver");
   registry.add("spring.flyway.url",()->url);
   registry.add("spring.flyway.user",()->requiredEnv("PG_TEST_MIGRATOR_USER"));
   registry.add("spring.flyway.password",()->requiredEnv("PG_TEST_MIGRATOR_PASSWORD"));
   registry.add("spring.flyway.init-sqls",()->"SET ROLE "+requiredEnv("PG_TEST_OWNER_ROLE"));
   registry.add("spring.flyway.locations",()->"classpath:db/migration");
   registry.add("spring.flyway.default-schema",()->"platform");
   registry.add("spring.flyway.schemas",()->"platform");
   registry.add("spring.flyway.create-schemas",()->"false");
   registry.add("spring.flyway.placeholders.runtimeRole",()->requiredEnv("PG_TEST_RUNTIME_USER"));
  } else {
   registry.add("spring.datasource.url",()->"jdbc:h2:mem:demo_tests;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1");
   registry.add("spring.datasource.username",()->"sa");registry.add("spring.datasource.password",()->"");
  }
  registry.add("demo.token-secret",()->TOKEN_SECRET);
  registry.add("demo.seed.enabled",()->"true");
  for(String name:new String[]{"admin","merchant-a","merchant-b","operator","disabled"}) registry.add("demo.seed."+name+"-password",()->PASSWORD);
 }
 static String requiredEnv(String name){String value=System.getenv(name);if(value==null||value.isBlank())throw new IllegalStateException("Missing "+name);return value;}
 String login(String username) throws Exception {
  String response=mvc.perform(post("/api/demo/v1/auth/login").contentType("application/json").content("{\"username\":\""+username+"\",\"password\":\""+PASSWORD+"\"}"))
   .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
  return JsonPath.read(response,"$.accessToken");
 }
}
