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
  registry.add("spring.datasource.url",()->"jdbc:h2:mem:demo_tests;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1");
  registry.add("spring.datasource.username",()->"sa");registry.add("spring.datasource.password",()->"");
  registry.add("demo.token-secret",()->TOKEN_SECRET);
  registry.add("demo.seed.enabled",()->"true");
  for(String name:new String[]{"admin","merchant-a","merchant-b","operator","disabled"}) registry.add("demo.seed."+name+"-password",()->PASSWORD);
 }
 String login(String username) throws Exception {
  String response=mvc.perform(post("/api/demo/v1/auth/login").contentType("application/json").content("{\"username\":\""+username+"\",\"password\":\""+PASSWORD+"\"}"))
   .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
  return JsonPath.read(response,"$.accessToken");
 }
}
