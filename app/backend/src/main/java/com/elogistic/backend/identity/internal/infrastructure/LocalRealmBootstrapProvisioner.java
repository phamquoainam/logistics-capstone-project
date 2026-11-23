package com.elogistic.backend.identity.internal.infrastructure;

import com.elogistic.backend.identity.internal.domain.ActionProof;
import com.elogistic.backend.identity.internal.domain.CustomerEmail;
import java.sql.*;
import java.time.Instant;
import java.util.*;
import javax.sql.DataSource;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** Independently authorized offline Realm operator. Never a runtime bean or Admin implication. */
public final class LocalRealmBootstrapProvisioner {
    public record OfflineRealmBootstrap(UUID requestId,UUID realmId,String displayName,String email,String reason){
        public OfflineRealmBootstrap{LocalAdminProvisioner.text(displayName,1,160);LocalAdminProvisioner.text(reason,10,500);}
        @Override public String toString(){return "OfflineRealmBootstrap[redacted]";}}
    public record OfflineRealmInvitationDisposition(UUID requestId,UUID realmId,String action,String reason){
        public OfflineRealmInvitationDisposition{LocalAdminProvisioner.text(reason,10,500);}
        @Override public String toString(){return "OfflineRealmInvitationDisposition[redacted]";}}
    public record OfflineRealmCommand(UUID requestId,UUID realmId,String reason){
        public OfflineRealmCommand{LocalAdminProvisioner.text(reason,10,500);}
        @Override public String toString(){return "OfflineRealmCommand[redacted]";}}
    public record OfflineRealmReceipt(UUID requestId,UUID realmId,UUID accountId,String committedOutcome,String currentState,boolean replayed){}
    private record Reservation(UUID account,UUID proof,UUID message,Instant created,Instant expires,boolean replayed,String state){}
    @FunctionalInterface private interface Work<T>{T run(Connection connection)throws SQLException;}
    private final DataSource operator;private final InvitationProtection protection;
    public LocalRealmBootstrapProvisioner(DataSource operator,InvitationProtection protection){this.operator=Objects.requireNonNull(operator);this.protection=Objects.requireNonNull(protection);}
    public OfflineRealmReceipt provision(OfflineRealmBootstrap input){
        if(input==null)throw rejected();new CustomerEmail(input.email());
        return transaction(c->{Reservation r;
            try(var q=c.prepareStatement("SELECT * FROM identity.provision_local_realm_bootstrap(?,?,?,?,?)")){
                q.setObject(1,input.requestId());q.setObject(2,input.realmId());q.setString(3,input.displayName());q.setString(4,input.email());q.setString(5,input.reason());
                try(var rows=q.executeQuery()){r=reservation(rows);}
            }
            if(!r.replayed())seal(c,input.requestId(),input.realmId(),input.email(),r);
            return new OfflineRealmReceipt(input.requestId(),input.realmId(),r.account(),"REALM_PENDING",r.state(),r.replayed());
        });
    }
    public OfflineRealmReceipt disposition(OfflineRealmInvitationDisposition input){
        if(input==null||!Set.of("RESEND","CANCEL").contains(input.action()))throw rejected();
        return transaction(c->{Reservation r;
            try(var q=c.prepareStatement("SELECT * FROM identity.disposition_local_realm_bootstrap_invitation(?,?,?,?)")){
                q.setObject(1,input.requestId());q.setObject(2,input.realmId());q.setString(3,input.action());q.setString(4,input.reason());try(var rows=q.executeQuery()){r=reservation(rows);}
            }
            if(!r.replayed()&&input.action().equals("RESEND")){
                String destination;
                try(var q=c.prepareStatement("SELECT identity.local_realm_bootstrap_invitation_destination(?)")){q.setObject(1,input.requestId());try(var rows=q.executeQuery()){if(!rows.next())throw rejected();destination=rows.getString(1);}}
                seal(c,input.requestId(),input.realmId(),destination,r);
            }
            return new OfflineRealmReceipt(input.requestId(),input.realmId(),r.account(),input.action().equals("RESEND")?"REALM_RESENT":"REALM_CANCELLED",r.state(),r.replayed());
        });
    }
    public OfflineRealmReceipt activate(OfflineRealmCommand input){return command(input,true);}
    public OfflineRealmReceipt disable(OfflineRealmCommand input){return command(input,false);}
    private OfflineRealmReceipt command(OfflineRealmCommand input,boolean activate){
        if(input==null)throw rejected();return transaction(c->{
            String sql=activate?"SELECT * FROM identity.activate_local_system_realm(?,?,?)":"SELECT * FROM identity.disable_local_system_realm(?,?,?)";
            try(var q=c.prepareStatement(sql)){
                q.setObject(1,input.requestId());q.setObject(2,input.realmId());q.setString(3,input.reason());try(var r=q.executeQuery()){
                    if(!r.next())throw rejected();var result=new OfflineRealmReceipt(input.requestId(),input.realmId(),null,activate?"REALM_ACTIVE":"REALM_INACTIVE",r.getString("current_state"),r.getBoolean("replayed"));if(r.next())throw rejected();return result;
                }
            }
        });
    }
    private void seal(Connection c,UUID request,UUID realm,String destination,Reservation r)throws SQLException{
        var proof=new ActionProof(r.proof(),r.account(),ActionProof.Purpose.INVITATION,r.created(),r.expires(),ActionProof.State.AVAILABLE,null);
        var selected=protection.context(proof,realm,destination);
        var context=new InvitationProtection.Context(r.message(),proof.id(),proof.accountId(),realm,destination,proof.createdAt(),proof.expiresAt(),selected.verifierKeyId(),selected.encryptionKeyId());
        try(var sealed=protection.issue(context);var q=c.prepareStatement("SELECT identity.seal_local_realm_bootstrap_invitation(?,?,?,?,?,?)")){
            byte[] hash=sealed.verifier(),nonce=sealed.nonce(),encrypted=sealed.ciphertext();
            try{q.setObject(1,request);q.setString(2,context.verifierKeyId());q.setString(3,context.encryptionKeyId());q.setBytes(4,hash);q.setBytes(5,nonce);q.setBytes(6,encrypted);q.execute();}
            finally{Arrays.fill(hash,(byte)0);Arrays.fill(nonce,(byte)0);Arrays.fill(encrypted,(byte)0);}
        }
    }
    private static Reservation reservation(ResultSet r)throws SQLException{
        if(!r.next())throw rejected();var expiry=r.getTimestamp("expires_at");var result=new Reservation(r.getObject("account_id",UUID.class),r.getObject("proof_id",UUID.class),r.getObject("message_id",UUID.class),r.getTimestamp("created_at").toInstant(),expiry==null?null:expiry.toInstant(),r.getBoolean("replayed"),r.getString("current_principal_state"));if(r.next())throw rejected();return result;
    }
    private <T>T transaction(Work<T> work){
        if(TransactionSynchronizationManager.isActualTransactionActive()||TransactionSynchronizationManager.isSynchronizationActive()||!TransactionSynchronizationManager.getResourceMap().isEmpty())throw rejected();
        try(var c=operator.getConnection()){
            if(!c.getAutoCommit())throw rejected();c.setAutoCommit(false);try{T result=work.run(c);c.commit();return result;}catch(SQLException|RuntimeException failure){c.rollback();throw rejected();}
        }catch(SQLException failure){throw rejected();}
    }
    private static IllegalStateException rejected(){return new IllegalStateException("REALM_OPERATOR_COMMAND_REJECTED");}
    @Override public String toString(){return "LocalRealmBootstrapProvisioner[redacted]";}
}
