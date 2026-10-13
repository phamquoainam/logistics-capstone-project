package com.elogistic.backend.identity.internal.web;
import com.elogistic.backend.identity.api.AccountView;
import com.elogistic.backend.identity.internal.application.LoginService;
import com.elogistic.backend.identity.internal.infrastructure.AccountRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;
import org.springframework.context.annotation.Profile;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
@RestController @Profile("demo") @RequestMapping("/api/demo/v1")
class AuthController {
 record LoginRequest(@NotBlank @Size(max=80) String username,@NotBlank @Size(max=72) String password) {}
 private final LoginService login;private final AccountRepository accounts;
 AuthController(LoginService login,AccountRepository accounts){this.login=login;this.accounts=accounts;}
 @PostMapping("/auth/login") LoginService.LoginResult login(@Valid @RequestBody LoginRequest r,HttpServletRequest http){return login.login(r.username(),r.password(),http.getRemoteAddr());}
 @GetMapping("/auth/me") AccountView me(@AuthenticationPrincipal AccountView user){return user;}
 @GetMapping("/admin/users") List<AccountView> users(){return accounts.list();}
}
