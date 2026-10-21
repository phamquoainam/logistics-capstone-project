package com.elogistic.backend.identity.internal.infrastructure;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import org.springframework.boot.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
@Component @Profile("demo") @ConditionalOnProperty(name="demo.seed.enabled",havingValue="true") @Order(2)
class DemoAccountSeed implements ApplicationRunner {
 private record SeedAccount(String username,String display,String role,boolean enabled,String merchant,String property) {}
 private static final List<SeedAccount> ACCOUNTS=List.of(
  new SeedAccount("demo.admin","Admin · Demo","ADMIN",true,null,"admin-password"),
  new SeedAccount("demo.merchant.a","Merchant A","MERCHANT",true,"00000000-0000-0000-0000-00000000000a","merchant-a-password"),
  new SeedAccount("demo.merchant.b","Merchant B","MERCHANT",true,"00000000-0000-0000-0000-00000000000b","merchant-b-password"),
  new SeedAccount("demo.operator","Warehouse · Demo","WAREHOUSE_OPERATOR",true,null,"operator-password"),
  new SeedAccount("demo.disabled","Disabled · Demo","SHIPPER",false,null,"disabled-password"));
 private final AccountRepository accounts;private final PasswordEncoder passwords;private final Environment env;
 DemoAccountSeed(AccountRepository accounts,PasswordEncoder passwords,Environment env){this.accounts=accounts;this.passwords=passwords;this.env=env;}
 /** Read-only check; an existing account still needs no seed password. */
 void validateConfiguration(){
  for(SeedAccount account:ACCOUNTS)if(accounts.credential(account.username()).isEmpty())requiredPassword(account.property());
 }
 @Transactional public void run(ApplicationArguments args){
  for(SeedAccount account:ACCOUNTS)seed(account.username(),account.display(),account.role(),account.enabled(),account.merchant(),account.property());
 }
 private void seed(String username,String display,String role,boolean enabled,String merchant,String property){
  // Recheck state: preflight does not lock accounts across startup phases.
  if(accounts.credential(username).isPresent())return;String password=requiredPassword(property);
  accounts.seed(UUID.randomUUID(),username,passwords.encode(password),display,role,enabled,merchant==null?null:UUID.fromString(merchant));
 }
 private String requiredPassword(String property){
  String password=env.getProperty("demo.seed."+property,"");
  if(password==null||password.isBlank()||password.length()<12||password.length()>72)throw new IllegalStateException("Configure all demo seed passwords (12–72 characters)");
  if(password.getBytes(StandardCharsets.UTF_8).length>72)throw new IllegalStateException("Configure all demo seed passwords (at most 72 UTF-8 bytes for BCrypt)");
  return password;
 }
}
