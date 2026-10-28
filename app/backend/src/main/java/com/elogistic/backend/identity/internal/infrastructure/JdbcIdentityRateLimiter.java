package com.elogistic.backend.identity.internal.infrastructure;
import com.elogistic.backend.identity.internal.application.port.IdentityRateLimiter;
import java.sql.*;
import java.time.*;
import java.util.*;
import java.nio.charset.StandardCharsets;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.sql.DataSource;

/** Separate physical transactions: authentication rollback can NEVER release durable quota. */
public final class JdbcIdentityRateLimiter implements IdentityRateLimiter {
    private final DataSource source;private final byte[] secret;
    public JdbcIdentityRateLimiter(DataSource source,byte[] secret){
        if(source==null||secret==null||secret.length!=32||Arrays.equals(secret,new byte[32]))throw new IllegalArgumentException("IDENTITY_LIMITER_CONFIGURATION_INVALID");
        this.source=source;this.secret=secret.clone();
    }
    private record Key(Policy policy,byte[] hash) {}
    public byte[] identifier(Subject s){
        if(s==null||s.policy()==null||s.value()==null||s.value().isEmpty()||s.value().length()>512)throw new IllegalArgumentException("LIMITER_SUBJECT_INVALID");
        try{Mac m=Mac.getInstance("HmacSHA256");m.init(new SecretKeySpec(secret,"HmacSHA256"));return m.doFinal(("identity-rate-v1\u0000"+s.policy().name()+"\u0000"+s.value()).getBytes(StandardCharsets.UTF_8));}
        catch(java.security.GeneralSecurityException e){throw new Unavailable();}
    }
    @FunctionalInterface private interface Work<T>{T run(Connection c)throws SQLException;}
    private <T>T tx(Work<T> work){
        for(int attempt=0;attempt<2;attempt++)try(Connection c=source.getConnection()){
            c.setAutoCommit(false);
            try(Statement s=c.createStatement()){s.execute("SET LOCAL lock_timeout='750ms'");s.execute("SET LOCAL statement_timeout='2500ms'");}
            try{T r=work.run(c);c.commit();return r;}catch(SQLException|RuntimeException e){try{c.rollback();}catch(SQLException ignored){}throw e;}
        }catch(SQLException e){if(attempt==0&&Set.of("40001","40P01").contains(e.getSQLState()))continue;throw new Unavailable();}
        throw new Unavailable();
    }
    private static Instant now(Connection c)throws SQLException{try(Statement s=c.createStatement();ResultSet r=s.executeQuery("SELECT clock_timestamp()")){r.next();return r.getTimestamp(1).toInstant();}}
    private static void lock(Connection c,Key k)throws SQLException{
        try(PreparedStatement s=c.prepareStatement("INSERT INTO identity.rate_limit_subject(policy,subject_hash) VALUES (?,?) ON CONFLICT DO NOTHING")){s.setString(1,k.policy.name());s.setBytes(2,k.hash);s.executeUpdate();}
        try(PreparedStatement s=c.prepareStatement("SELECT policy FROM identity.rate_limit_subject WHERE policy=? AND subject_hash=? FOR UPDATE")){s.setString(1,k.policy.name());s.setBytes(2,k.hash);try(ResultSet r=s.executeQuery()){if(!r.next())throw new Unavailable();}}
    }
    @Override public Reservation reserve(List<Subject> subjects){
        if(subjects==null||subjects.isEmpty()||subjects.size()>2)throw new IllegalArgumentException("LIMITER_SUBJECT_INVALID");
        var keys=subjects.stream().map(s->new Key(s.policy(),identifier(s))).sorted(Comparator.comparing((Key k)->k.policy.name()).thenComparing(k->HexFormat.of().formatHex(k.hash))).toList();
        if(keys.stream().map(k->k.policy.name()+HexFormat.of().formatHex(k.hash)).distinct().count()!=keys.size())throw new IllegalArgumentException("LIMITER_SUBJECT_INVALID");
        cleanup();
        UUID id=UUID.randomUUID();
        tx(c->{
            for(Key k:keys)lock(c,k);Instant time=now(c);int retry=0;
            for(Key k:keys){
                try(PreparedStatement s=c.prepareStatement("SELECT count(*),min(expires_at) FROM identity.rate_limit_attempt WHERE policy=? AND subject_hash=? AND expires_at>? AND state<>'RELEASED'")){
                    s.setString(1,k.policy.name());s.setBytes(2,k.hash);s.setTimestamp(3,Timestamp.from(time));
                    try(ResultSet r=s.executeQuery()){r.next();if(r.getLong(1)>=k.policy.limit)retry=Math.max(retry,(int)Math.ceil(Duration.between(time,r.getTimestamp(2).toInstant()).toMillis()/1000.0));}
                }
            }
            if(retry>0)throw new Exhausted(retry);
            for(Key k:keys){
                try(PreparedStatement s=c.prepareStatement("INSERT INTO identity.rate_limit_attempt VALUES (?,?,?,?,?,?,?)")){
                    s.setObject(1,id);s.setString(2,k.policy.name());s.setBytes(3,k.hash);s.setTimestamp(4,Timestamp.from(time));s.setTimestamp(5,Timestamp.from(time.plusSeconds(k.policy.seconds+(k.policy.passwordAttempt()?60:0))));s.setTimestamp(6,Timestamp.from(time.plusSeconds(60)));s.setString(7,k.policy.passwordAttempt()?"PENDING":"COUNTED");s.executeUpdate();
                }
                try(PreparedStatement s=c.prepareStatement("UPDATE identity.rate_limit_subject SET touched_at=? WHERE policy=? AND subject_hash=?")){s.setTimestamp(1,Timestamp.from(time));s.setString(2,k.policy.name());s.setBytes(3,k.hash);s.executeUpdate();}
            }return null;
        });
        return success->tx(c->{
            for(Key k:keys)lock(c,k);Instant time=now(c);
            for(Key k:keys)if(k.policy.passwordAttempt()){
                try(PreparedStatement s=c.prepareStatement("UPDATE identity.rate_limit_attempt SET state=?,expires_at=CASE WHEN ? THEN expires_at ELSE ? END WHERE reservation_id=? AND policy=? AND subject_hash=? AND state='PENDING' AND lease_expires_at>?")){
                    s.setString(1,success?"RELEASED":"FAILED");s.setBoolean(2,success);s.setTimestamp(3,Timestamp.from(time.plusSeconds(k.policy.seconds)));s.setObject(4,id);s.setString(5,k.policy.name());s.setBytes(6,k.hash);s.setTimestamp(7,Timestamp.from(time));if(s.executeUpdate()!=1)throw new Unavailable();
                }
            }return null;
        });
    }
    /** Expired reservations count conservatively until rolling expiry, never reopen an active window. */
    @Override public int cleanup(){return tx(c->{
        int removed;try(Statement s=c.createStatement()){removed=s.executeUpdate("DELETE FROM identity.rate_limit_attempt WHERE ctid IN (SELECT ctid FROM identity.rate_limit_attempt WHERE expires_at<=clock_timestamp() ORDER BY expires_at LIMIT 100 FOR UPDATE SKIP LOCKED)");
            s.executeUpdate("DELETE FROM identity.rate_limit_subject s WHERE s.ctid IN (SELECT q.ctid FROM identity.rate_limit_subject q WHERE touched_at<clock_timestamp()-INTERVAL '2 hours' AND NOT EXISTS (SELECT 1 FROM identity.rate_limit_attempt a WHERE a.policy=q.policy AND a.subject_hash=q.subject_hash) LIMIT 100 FOR UPDATE SKIP LOCKED)");}return removed;
    });}
    @Override public String toString(){return "JdbcIdentityRateLimiter[redacted]";}
}
