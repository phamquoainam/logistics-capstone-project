package com.elogistic.backend.identity.internal.infrastructure;

import com.elogistic.backend.testsupport.*;
import java.sql.*;
import java.time.Clock;
import java.util.*;
import javax.sql.DataSource;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.postgresql.PostgreSQLContainer;

/** Synthetic canonical onboarding fixture. No direct verified-account/READ grant seeds. */
public final class AdminOnboardingPostgresFixture implements AutoCloseable {
    private final IsolatedAdminTestDatabase owned;private final UUID realm;private final InvitationProtection protection;
    private final LocalAdminProvisioner admins;private final DriverManagerDataSource operator;
    private final DataSource runtime;
    private final Map<String,Object> runtimeSettings;
    private final LocalRealmBootstrapProvisioner roots;private final UUID rootAccount;
    public AdminOnboardingPostgresFixture(PostgreSQLContainer container,UUID realm)throws Exception{
        this(container,realm,true);
    }
    public AdminOnboardingPostgresFixture(PostgreSQLContainer container,UUID realm,boolean activateRoot)throws Exception{
        this.realm=realm;owned=IsolatedAdminTestDatabase.create(container);database().flyway("40",realm).migrate();
        operator=operator(container,"elogistics_local_admin_operator");var realmOperator=operator(container,"elogistics_local_realm_operator");
        try(var c=operator.getConnection();var q=c.prepareStatement("SELECT * FROM identity.bind_local_admin_environment(?,?,'TEST','Synthetic canonical fixture binding')")){q.setObject(1,UUID.randomUUID());q.setObject(2,realm);q.execute();}
        byte[] v=new byte[32],e=new byte[32];new java.security.SecureRandom().nextBytes(v);new java.security.SecureRandom().nextBytes(e);
        protection=new InvitationProtection(Map.of("v1",v),"v1",Map.of("e1",e),"e1");
        runtimeSettings=Map.of("customer.security.proof-verifier-keys","{\"v1\":\""+Base64.getEncoder().encodeToString(v)+"\"}","customer.security.delivery-encryption-keys","{\"e1\":\""+Base64.getEncoder().encodeToString(e)+"\"}","customer.security.current-proof-key","v1","customer.security.current-encryption-key","e1");
        Arrays.fill(v,(byte)0);Arrays.fill(e,(byte)0);
        runtime=new DriverManagerDataSource(database().url(),database().runtime(),database().runtimePassword());
        roots=new LocalRealmBootstrapProvisioner(realmOperator,protection);
        var root=roots.provision(new LocalRealmBootstrapProvisioner.OfflineRealmBootstrap(UUID.randomUUID(),realm,"Synthetic Initial Root","root-"+UUID.randomUUID()+"@example.invalid","Synthetic genuine fixture Root"));
        rootAccount=root.accountId();
        if(activateRoot){accept(root.accountId(),PostgresTestcontainersSupport.password(),true);roots.activate(new LocalRealmBootstrapProvisioner.OfflineRealmCommand(UUID.randomUUID(),realm,"Synthetic verified Root activation"));}
        admins=new LocalAdminProvisioner(operator,protection);
    }
    public PostgresTestcontainersSupport.Database database(){return owned.database();}
    public LocalAdminProvisioner admins(){return admins;}
    public LocalRealmBootstrapProvisioner roots(){return roots;}
    public UUID rootAccount(){return rootAccount;}
    public InvitationProtection protection(){return protection;}
    /** Synthetic in-memory keys for the owned HTTPS host only; never log/serialize this map. */
    public Map<String,Object> runtimeSettings(){return runtimeSettings;}
    public record PendingInvitation(UUID account,UUID proof,byte[] token)implements AutoCloseable{
        public PendingInvitation{token=token.clone();}
        @Override public byte[] token(){return token.clone();}
        @Override public void close(){Arrays.fill(token,(byte)0);}
        @Override public String toString(){return "PendingAdminInvitation[redacted]";}
    }
    public PendingInvitation pendingWithSmtp(Set<String> permissions,String origin)throws Exception{
        var result=admins.provision(new LocalAdminProvisioner.OfflineAdminProvision(UUID.randomUUID(),realm,"Synthetic invited Admin","invited-"+UUID.randomUUID()+"@example.invalid",permissions,"Synthetic owned HTTPS invitation"));
        var clock=Clock.systemUTC();
        try(var inbox=new CustomerSmtpTestInbox(1,false,false,250,null)){
            var transport=new SmtpCustomerDelivery(new SmtpCustomerDelivery.Settings("127.0.0.1",inbox.port(),SmtpCustomerDelivery.Mode.TEST_LOOPBACK,null,null,"admin-sender@example.invalid",Set.of("example.invalid"),origin,true),clock);
            var worker=new AdminInvitationDeliveryWorker(JdbcInvitationOutbox.admin(runtime,protection,new JdbcInvitationOutbox.Policy(3,java.time.Duration.ofSeconds(30),java.time.Duration.ofSeconds(2)),clock,"TEST",false),transport,()->true);
            if(!worker.runOne()||worker.runOne())throw new IllegalStateException("AON_FIXTURE_DELIVERY_FAILED");
            var received=inbox.messages();if(received.size()!=1)throw new IllegalStateException("AON_FIXTURE_MAIL_MISSING");
            var mail=new jakarta.mail.internet.MimeMessage(jakarta.mail.Session.getInstance(new Properties()),new java.io.ByteArrayInputStream(received.getFirst()));
            var match=java.util.regex.Pattern.compile("/admin/activate#proofId=([a-f0-9-]{36})&token=([A-Za-z0-9_-]{43})").matcher(CustomerSmtpTestInbox.plainText(mail));
            if(!match.find())throw new IllegalStateException("AON_FIXTURE_MAIL_INVALID");
            byte[] token=match.group(2).getBytes(java.nio.charset.StandardCharsets.US_ASCII);
            try{return new PendingInvitation(result.accountId(),UUID.fromString(match.group(1)),token);}finally{Arrays.fill(token,(byte)0);}
        }
    }
    private DriverManagerDataSource governanceOperator;
    public synchronized Connection governanceOperatorConnection()throws Exception{
        if(governanceOperator==null)governanceOperator=operator(database().container(),"merchant_governance_operator");
        return governanceOperator.getConnection();
    }
    public Connection operatorConnection()throws SQLException{return operator.getConnection();}
    private DriverManagerDataSource operator(PostgreSQLContainer pg,String role)throws Exception{
        String login="aon_fixture_"+UUID.randomUUID().toString().replace("-","");String password=PostgresTestcontainersSupport.password();
        try(var c=DriverManager.getConnection(PostgresTestcontainersSupport.currentUrl(pg,"postgres"),pg.getUsername(),pg.getPassword());var s=c.createStatement()){
            s.execute("CREATE ROLE "+login+" LOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE PASSWORD '"+password+"'");s.execute("GRANT "+role+" TO "+login);s.execute("GRANT CONNECT ON DATABASE "+database().name()+" TO "+login);
        }
        return new DriverManagerDataSource(database().url(),login,password);
    }
    public UUID admin(Set<String> permissions,String password)throws Exception{
        var result=admins.provision(new LocalAdminProvisioner.OfflineAdminProvision(UUID.randomUUID(),realm,"Synthetic Admin","admin-"+UUID.randomUUID()+"@example.invalid",permissions,"Synthetic canonical Admin fixture"));
        accept(result.accountId(),password,false);return result.accountId();
    }
    /** Cipher extraction for adversarial component tests only, not mail/UAT evidence. */
    public PendingInvitation pendingToken(UUID account)throws Exception{
        byte[] token;UUID proof;
        try(var c=runtime.getConnection();var q=c.prepareStatement("SELECT * FROM identity.invitation_outbox WHERE account_id=? AND status='PENDING'")){
            q.setObject(1,account);try(var r=q.executeQuery()){
                if(!r.next())throw new IllegalStateException("AON_FIXTURE_INVITATION_MISSING");proof=r.getObject("proof_id",UUID.class);
                var context=new InvitationProtection.Context(r.getObject("id",UUID.class),proof,account,realm,r.getString("destination"),r.getTimestamp("created_at").toInstant(),r.getTimestamp("expires_at").toInstant(),r.getString("verifier_key_id"),r.getString("encryption_key_id"));
                token=protection.open(context,r.getBytes("nonce"),r.getBytes("ciphertext"),java.time.Instant.now());
            }
        }
        try{return new PendingInvitation(account,proof,token);}finally{Arrays.fill(token,(byte)0);}
    }
    private void accept(UUID account,String password,boolean root)throws Exception{
        try(var pending=pendingToken(account)){
            byte[] token=pending.token();try{new LocalAdminInvitationActivation(runtime,protection,Clock.systemUTC(),"TEST",root).accept(pending.proof(),token,password,UUID.randomUUID(),UUID.randomUUID());}finally{Arrays.fill(token,(byte)0);}
        }
    }
    @Override public void close()throws Exception{owned.close();}
}
