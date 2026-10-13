package com.elogistic.backend.demo;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
class DemoAuthenticationTests extends DemoTestSupport {
 @Test void loginMeAndHashedPassword() throws Exception {
  String token=login("demo.merchant.a");
  mvc.perform(get("/api/demo/v1/auth/me").header("Authorization","Bearer "+token)).andExpect(status().isOk()).andExpect(jsonPath("$.role").value("MERCHANT"));
  String hash=jdbc.queryForObject("SELECT password_hash FROM identity.demo_account WHERE username='demo.merchant.a'",String.class);
  assertNotEquals(PASSWORD,hash);assertTrue(new BCryptPasswordEncoder().matches(PASSWORD,hash));
 }
 @Test void wrongPasswordAndDisabledLogin() throws Exception {
  for(String username:new String[]{"demo.admin","demo.disabled"}) {
   String password=username.equals("demo.disabled")?PASSWORD:"invalid";
   mvc.perform(post("/api/demo/v1/auth/login").contentType("application/json").content("{\"username\":\""+username+"\",\"password\":\""+password+"\"}"))
    .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
  }
 }
 @Test void anonymousUnauthorizedAndWrongRoleForbidden() throws Exception {
  mvc.perform(get("/api/demo/v1/auth/me")).andExpect(status().isUnauthorized());
  String merchant=login("demo.merchant.a"),admin=login("demo.admin"),operator=login("demo.operator");
  mvc.perform(get("/api/demo/v1/admin/users").header("Authorization","Bearer "+merchant)).andExpect(status().isForbidden());
  mvc.perform(get("/api/demo/v1/admin/users").header("Authorization","Bearer "+admin)).andExpect(status().isOk()).andExpect(jsonPath("$[0].passwordHash").doesNotExist());
  mvc.perform(get("/api/demo/v1/shipments").header("Authorization","Bearer "+operator)).andExpect(status().isForbidden());
  mvc.perform(get("/api/demo/v1/finance").header("Authorization","Bearer "+admin)).andExpect(status().isForbidden());
 }
 @Test void accountStatusAndRoleRecheckedAfterIssue() throws Exception {
  String token=login("demo.operator");
  try {
   jdbc.update("UPDATE identity.demo_account SET enabled=false WHERE username='demo.operator'");
   mvc.perform(get("/api/demo/v1/auth/me").header("Authorization","Bearer "+token)).andExpect(status().isUnauthorized());
   jdbc.update("UPDATE identity.demo_account SET enabled=true,role='ADMIN' WHERE username='demo.operator'");
   mvc.perform(get("/api/demo/v1/admin/users").header("Authorization","Bearer "+token)).andExpect(status().isOk());
  }finally{jdbc.update("UPDATE identity.demo_account SET enabled=true,role='WAREHOUSE_OPERATOR' WHERE username='demo.operator'");}
 }
 @Test void tamperedTokenAndClientRoleInjectionRejected() throws Exception {
  mvc.perform(get("/api/demo/v1/auth/me").header("Authorization","Bearer invalid.token.value")).andExpect(status().isUnauthorized());
  mvc.perform(post("/api/demo/v1/auth/login").contentType("application/json").content("{\"username\":\"demo.admin\",\"password\":\""+PASSWORD+"\",\"role\":\"ADMIN\"}"))
   .andExpect(status().isBadRequest());
 }
 @Test void rateLimitFailedLogins() throws Exception {
  String body="{\"username\":\"nonexistent-rate-limit-user\",\"password\":\"wrong\"}";
  for(int i=0;i<5;i++)mvc.perform(post("/api/demo/v1/auth/login").contentType("application/json").content(body)).andExpect(status().isUnauthorized());
  mvc.perform(post("/api/demo/v1/auth/login").contentType("application/json").content(body)).andExpect(status().isTooManyRequests());
 }
 @Test void signedExpiredTokenRejected() throws Exception {
  String id=jdbc.queryForObject("SELECT id FROM identity.demo_account WHERE username='demo.admin'",java.util.UUID.class).toString();
  var encoder=new org.springframework.security.oauth2.jwt.NimbusJwtEncoder(new com.nimbusds.jose.jwk.source.ImmutableSecret<>(TOKEN_SECRET.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
  var now=java.time.Instant.now();
  var claims=org.springframework.security.oauth2.jwt.JwtClaimsSet.builder().issuer("elogistics-demo").subject(id).issuedAt(now.minusSeconds(300)).expiresAt(now.minusSeconds(120)).build();
  String token=encoder.encode(org.springframework.security.oauth2.jwt.JwtEncoderParameters.from(org.springframework.security.oauth2.jwt.JwsHeader.with(org.springframework.security.oauth2.jose.jws.MacAlgorithm.HS256).build(),claims)).getTokenValue();
  mvc.perform(get("/api/demo/v1/auth/me").header("Authorization","Bearer "+token)).andExpect(status().isUnauthorized());
 }
}
