package com.elogistic.backend.merchant.internal.infrastructure;
import com.elogistic.backend.merchant.api.*;
import com.elogistic.backend.platform.api.DemoException;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
@Repository @Profile("demo")
public class MerchantRepository implements MerchantAccess {
 private final JdbcTemplate jdbc;
 public MerchantRepository(JdbcTemplate jdbc){this.jdbc=jdbc;}
 public MerchantView require(UUID id){return jdbc.query("SELECT * FROM merchant.demo_merchant WHERE id=?",(rs,row)->new MerchantView(rs.getObject("id",UUID.class),rs.getString("display_name"),rs.getString("reference")),id).stream().findFirst().orElseThrow(()->new DemoException(HttpStatus.FORBIDDEN,"MERCHANT_UNAVAILABLE","Tổ chức merchant chưa được cấp quyền."));}
}
