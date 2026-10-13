package com.elogistic.backend.identity.internal.application;
import com.elogistic.backend.identity.api.AccountView;
import com.elogistic.backend.identity.internal.application.port.AccountQueries;
import com.elogistic.backend.platform.api.DemoException;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
@Service @Profile("demo")
public class LoginService {
 public record LoginResult(String accessToken,int expiresIn,AccountView user) {}
 private record Attempts(int count,Instant until) {}
 private final ConcurrentHashMap<String,Attempts> attempts=new ConcurrentHashMap<>();
 private final AccountQueries accounts;private final PasswordEncoder passwords;private final TokenService tokens;private final String dummyHash;
 public LoginService(AccountQueries accounts,PasswordEncoder passwords,TokenService tokens){this.accounts=accounts;this.passwords=passwords;this.tokens=tokens;dummyHash=passwords.encode(java.util.UUID.randomUUID().toString());}
 public synchronized LoginResult login(String username,String password,String remoteAddress) {
  Instant now=Instant.now();attempts.entrySet().removeIf(e->e.getValue().until().isBefore(now));String key=remoteAddress+":"+username;Attempts prior=attempts.get(key);
  if((prior!=null&&prior.count()>=5)||attempts.size()>=2000) throw new DemoException(HttpStatus.TOO_MANY_REQUESTS,"LOGIN_RATE_LIMIT","Quá nhiều lần đăng nhập. Thử lại sau 5 phút.");
  var credential=accounts.credential(username);
  boolean matches=password.getBytes(java.nio.charset.StandardCharsets.UTF_8).length<=72 && passwords.matches(password,credential.map(AccountQueries.Credential::passwordHash).orElse(dummyHash));
  if(!matches||credential.isEmpty()||!credential.get().account().enabled()) {
   attempts.put(key,new Attempts(prior==null?1:prior.count()+1,prior==null?now.plusSeconds(300):prior.until()));
   throw new DemoException(HttpStatus.UNAUTHORIZED,"INVALID_CREDENTIALS","Thông tin đăng nhập không hợp lệ.");
  }
  attempts.remove(key);AccountView account=credential.get().account();return new LoginResult(tokens.issue(account),900,account);
 }
}
