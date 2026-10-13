package com.elogistic.backend.identity.internal.infrastructure;
import com.elogistic.backend.identity.api.*;
import com.elogistic.backend.identity.internal.application.port.AccountQueries;
import java.util.*;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.*;
import org.springframework.stereotype.Repository;
@Repository @Profile("demo")
public class AccountRepository implements AccountAccess, AccountQueries {
 private final JdbcTemplate jdbc;
 private static final RowMapper<AccountView> VIEW=(rs,row)->new AccountView(rs.getObject("id",UUID.class),rs.getString("username"),rs.getString("display_name"),rs.getString("role"),rs.getBoolean("enabled"),rs.getObject("merchant_id",UUID.class));
 public AccountRepository(JdbcTemplate jdbc) {this.jdbc=jdbc;}
 public Optional<AccountView> find(UUID id) {return jdbc.query("SELECT * FROM identity.demo_account WHERE id=?",VIEW,id).stream().findFirst();}
 public Optional<Credential> credential(String username) {return jdbc.query("SELECT * FROM identity.demo_account WHERE username=?",(rs,row)->new Credential(VIEW.mapRow(rs,row),rs.getString("password_hash")),username).stream().findFirst();}
 public List<AccountView> list() {return jdbc.query("SELECT * FROM identity.demo_account ORDER BY username",VIEW);}
 public void seed(UUID id,String username,String hash,String displayName,String role,boolean enabled,UUID merchantId) {
  if(credential(username).isEmpty()) jdbc.update("INSERT INTO identity.demo_account VALUES (?,?,?,?,?,?,?)",id,username,hash,displayName,role,enabled,merchantId);
 }
}
