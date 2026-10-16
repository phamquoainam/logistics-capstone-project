package com.elogistic.backend.identity.internal.infrastructure;
import com.elogistic.backend.identity.api.AccountAccess;
import com.elogistic.backend.identity.internal.application.TokenService;
import java.util.List;
import java.util.UUID;
import org.springframework.context.annotation.*;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.*;
import org.springframework.security.web.SecurityFilterChain;
@Configuration @Profile("demo")
class DemoSecurity {
 @Bean PasswordEncoder passwordEncoder(){return new BCryptPasswordEncoder(12);}
 private static void error(jakarta.servlet.http.HttpServletResponse response,int status,String code,String message) throws java.io.IOException {
  response.setStatus(status);response.setContentType("application/json;charset=UTF-8");response.getWriter().write("{\"code\":\""+code+"\",\"message\":\""+message+"\"}");
 }
 @Bean SecurityFilterChain demoChain(HttpSecurity http,TokenService tokens,AccountAccess accounts) throws Exception {
  http.csrf(csrf->csrf.disable()).sessionManagement(s->s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
   .authorizeHttpRequests(a->a.requestMatchers(HttpMethod.POST,"/api/demo/v1/auth/login").permitAll()
    .requestMatchers(HttpMethod.GET,"/api/demo/v1/health").permitAll()
    .requestMatchers(HttpMethod.GET,"/actuator/health/liveness","/actuator/health/readiness").permitAll()
    .requestMatchers(HttpMethod.GET,"/api/demo/v1/auth/me").authenticated()
    .requestMatchers(HttpMethod.GET,"/api/demo/v1/admin/**").hasRole("ADMIN")
    .requestMatchers(HttpMethod.GET,"/api/demo/v1/merchant/me","/api/demo/v1/shipments","/api/demo/v1/shipments/*").hasRole("MERCHANT")
    .requestMatchers(HttpMethod.POST,"/api/demo/v1/shipments","/api/demo/v1/shipments/*/cancel").hasRole("MERCHANT").anyRequest().denyAll())
   .exceptionHandling(e->e.authenticationEntryPoint((r,s,x)->error(s,401,"UNAUTHENTICATED","Vui lòng đăng nhập lại."))
    .accessDeniedHandler((r,s,x)->error(s,403,"FORBIDDEN","Bạn không có quyền thực hiện thao tác này.")))
   .oauth2ResourceServer(s->s.authenticationEntryPoint((r,p,x)->error(p,401,"INVALID_TOKEN","Phiên đăng nhập không hợp lệ hoặc đã hết hạn."))
    .jwt(j->j.decoder(tokens.decoder()).jwtAuthenticationConverter(token->{
     try {
      var account=accounts.find(UUID.fromString(token.getSubject())).filter(a->a.enabled()).orElseThrow(()->new IllegalArgumentException("Unavailable account"));
      return UsernamePasswordAuthenticationToken.authenticated(account,"",List.of(new SimpleGrantedAuthority("ROLE_"+account.role())));
     }catch(IllegalArgumentException x){throw new OAuth2AuthenticationException(new OAuth2Error("invalid_token"));}
    })));
  return http.build();
 }
}
