package com.elogistic.backend.identity.internal.application;
import com.elogistic.backend.identity.api.AccountView;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;
@Service @Profile("demo")
public class TokenService {
 private final JwtEncoder encoder;
 private final JwtDecoder decoder;
 public TokenService(@Value("${demo.token-secret}") String secret) {
  byte[] bytes=secret.getBytes(StandardCharsets.UTF_8);
  if(secret.isBlank()||bytes.length<32) throw new IllegalStateException("DEMO_TOKEN_SECRET must contain at least 32 bytes");
  encoder=new NimbusJwtEncoder(new ImmutableSecret<>(bytes));
  var d=NimbusJwtDecoder.withSecretKey(new SecretKeySpec(bytes,"HmacSHA256")).macAlgorithm(MacAlgorithm.HS256).build();
  d.setJwtValidator(JwtValidators.createDefaultWithIssuer("elogistics-demo"));decoder=d;
 }
 public JwtDecoder decoder(){return decoder;}
 public String issue(AccountView account) {
  Instant now=Instant.now();
  var claims=JwtClaimsSet.builder().issuer("elogistics-demo").subject(account.id().toString()).issuedAt(now).expiresAt(now.plusSeconds(900)).id(UUID.randomUUID().toString()).build();
  return encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(),claims)).getTokenValue();
 }
}
