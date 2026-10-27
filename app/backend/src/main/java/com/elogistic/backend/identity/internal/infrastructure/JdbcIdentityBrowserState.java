package com.elogistic.backend.identity.internal.infrastructure;
import com.elogistic.backend.identity.internal.application.port.*;
import com.elogistic.backend.identity.api.IdentityCommandFailure;
import java.sql.*;
import java.security.*;
import java.util.*;
import java.nio.charset.StandardCharsets;
import javax.sql.DataSource;

/** Only hashes of fresh high-entropy secrets. No browser cookie establishes a principal. */
public final class JdbcIdentityBrowserState implements IdentityBrowserState {
    private final DataSource source;private final SecureRandom random=new SecureRandom();
    private final IdentitySessionStorage storage;
    public JdbcIdentityBrowserState(DataSource source){this(source,IdentitySessionStorage.SYSTEM);}
    public JdbcIdentityBrowserState(DataSource source,IdentitySessionStorage storage){this.source=Objects.requireNonNull(source);this.storage=Objects.requireNonNull(storage);}
    private static IdentityCommandFailure invalid(){return new IdentityCommandFailure(IdentityCommandFailure.Code.CSRF_REJECTED);}
    private static byte[] hash(String text){
        if(text==null||!text.matches("[A-Za-z0-9_-]{43}"))throw invalid();
        byte[] raw;try{raw=Base64.getUrlDecoder().decode(text);if(!Base64.getUrlEncoder().withoutPadding().encodeToString(raw).equals(text))throw invalid();return MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.US_ASCII));}catch(GeneralSecurityException|IllegalArgumentException e){throw invalid();}
    }
    private String secret(){byte[] b=new byte[32];random.nextBytes(b);try{return Base64.getUrlEncoder().withoutPadding().encodeToString(b);}finally{Arrays.fill(b,(byte)0);}}
    private static void budget(Connection c)throws SQLException{try(Statement s=c.createStatement()){s.execute("SET LOCAL lock_timeout='750ms'");s.execute("SET LOCAL statement_timeout='2500ms'");}}
    @Override public Challenge issue(String previous,String refresh){
        UUID session=null;
        if(refresh!=null){
            byte[] h;try{h=hash(refresh);}catch(IdentityCommandFailure invalid){h=null;}
            if(h!=null)try(Connection c=source.getConnection();PreparedStatement s=c.prepareStatement("SELECT session_id FROM "+storage.refresh+" WHERE token_hash=?")){
                s.setQueryTimeout(2);s.setBytes(1,h);try(ResultSet r=s.executeQuery()){if(r.next())session=r.getObject(1,UUID.class);}
            }catch(SQLException e){throw new IdentityRateLimiter.Unavailable();}
        }
        return rebind(previous,session);
    }
    @Override public Challenge rebind(String previous,UUID session){
        String cookie=secret(),challenge=secret();
        try(Connection c=source.getConnection()){
            c.setAutoCommit(false);try{budget(c);
                if(previous!=null){byte[] old;try{old=hash(previous);}catch(IdentityCommandFailure invalid){old=null;}
                    if(old!=null)try(PreparedStatement s=c.prepareStatement("UPDATE "+storage.csrf+" SET revoked_at=clock_timestamp() WHERE cookie_hash=? AND revoked_at IS NULL")){s.setBytes(1,old);s.executeUpdate();}}
                try(PreparedStatement s=c.prepareStatement("INSERT INTO "+storage.csrf+" VALUES (?,?,?,?,clock_timestamp(),clock_timestamp()+INTERVAL '15 minutes',NULL)")){
                    s.setObject(1,UUID.randomUUID());s.setBytes(2,hash(cookie));s.setBytes(3,hash(challenge));s.setObject(4,session);s.executeUpdate();}
                c.commit();return new Challenge(cookie,challenge);
            }catch(SQLException|RuntimeException e){c.rollback();throw e;}
        }catch(SQLException e){throw new IdentityRateLimiter.Unavailable();}
    }
    @Override public void verify(String cookie,String challenge,UUID session,boolean login){
        byte[] a=hash(cookie),b=hash(challenge);
        try(Connection c=source.getConnection();PreparedStatement s=c.prepareStatement("SELECT session_id FROM "+storage.csrf+" WHERE cookie_hash=? AND challenge_hash=? AND revoked_at IS NULL AND expires_at>clock_timestamp()")){
            s.setQueryTimeout(2);s.setBytes(1,a);s.setBytes(2,b);
            try(ResultSet r=s.executeQuery()){if(!r.next()||(!login&&!Objects.equals(session,r.getObject(1,UUID.class))))throw invalid();}
        }catch(SQLException e){throw new IdentityRateLimiter.Unavailable();}
    }
}
