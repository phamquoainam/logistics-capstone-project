package com.elogistic.backend.identity.internal.application;
import com.elogistic.backend.identity.internal.infrastructure.AccountRepository;
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
 private final AccountRepository accounts;private final PasswordEncoder passwords;private final Environment env;
 DemoAccountSeed(AccountRepository accounts,PasswordEncoder passwords,Environment env){this.accounts=accounts;this.passwords=passwords;this.env=env;}
 @Transactional public void run(ApplicationArguments args){
  seed("demo.admin","Admin · Demo","ADMIN",true,null,"admin-password");
  seed("demo.merchant.a","Merchant A","MERCHANT",true,"00000000-0000-0000-0000-00000000000a","merchant-a-password");
  seed("demo.merchant.b","Merchant B","MERCHANT",true,"00000000-0000-0000-0000-00000000000b","merchant-b-password");
  seed("demo.operator","Warehouse · Demo","WAREHOUSE_OPERATOR",true,null,"operator-password");
  seed("demo.disabled","Disabled · Demo","SHIPPER",false,null,"disabled-password");
 }
 private void seed(String username,String display,String role,boolean enabled,String merchant,String property){
  if(accounts.credential(username).isPresent())return;String password=env.getProperty("demo.seed."+property,"");
  if(password.length()<12||password.length()>72)throw new IllegalStateException("Configure all demo seed passwords (12–72 characters)");
  accounts.seed(UUID.randomUUID(),username,passwords.encode(password),display,role,enabled,merchant==null?null:UUID.fromString(merchant));
 }
}
