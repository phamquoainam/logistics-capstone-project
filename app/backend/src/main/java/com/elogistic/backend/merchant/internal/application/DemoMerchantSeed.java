package com.elogistic.backend.merchant.internal.application;
import java.util.UUID;
import org.springframework.boot.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
@Component @Profile("demo") @ConditionalOnProperty(name="demo.seed.enabled",havingValue="true") @Order(1)
class DemoMerchantSeed implements ApplicationRunner {
 private final JdbcTemplate jdbc;
 DemoMerchantSeed(JdbcTemplate jdbc){this.jdbc=jdbc;}
 @Transactional public void run(ApplicationArguments args){seed("00000000-0000-0000-0000-00000000000a","Merchant A · Demo","DEMO-A");seed("00000000-0000-0000-0000-00000000000b","Merchant B · Demo","DEMO-B");}
 private void seed(String id,String name,String reference){UUID uuid=UUID.fromString(id);if(jdbc.queryForObject("SELECT COUNT(*) FROM merchant.demo_merchant WHERE id=?",Integer.class,uuid)==0)jdbc.update("INSERT INTO merchant.demo_merchant VALUES (?,?,?)",uuid,name,reference);}
}
