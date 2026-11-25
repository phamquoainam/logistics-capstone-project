package com.elogistic.backend.identity.internal.infrastructure;

import com.elogistic.backend.identity.internal.domain.ActionProof;
import com.elogistic.backend.identity.internal.domain.CustomerEmail;
import java.sql.*;
import java.time.Instant;
import java.util.*;
import javax.sql.DataSource;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** Trusted offline composition only. No runtime bean, public provisioning or credential input. */
public final class LocalAdminProvisioner {
    public record OfflineAdminProvision(UUID requestId,UUID realmId,String displayName,String email,Set<String> permissions,String reason) {
        public OfflineAdminProvision{text(displayName,1,160);text(reason,10,500);permissions=Set.copyOf(permissions);}
        @Override public String toString(){return "OfflineAdminProvision[redacted]";}
    }
    public record OfflineAdminInvitationDisposition(UUID requestId,UUID accountId,long expectedVersion,String action,String reason) {
        public OfflineAdminInvitationDisposition{text(reason,10,500);}
        @Override public String toString(){return "OfflineAdminInvitationDisposition[redacted]";}
    }
    public record OfflineAdminPermissions(UUID requestId,UUID accountId,long expectedVersion,Set<String> permissions,String reason) {
        public OfflineAdminPermissions{text(reason,10,500);permissions=Set.copyOf(permissions);}
        @Override public String toString(){return "OfflineAdminPermissions[redacted]";}
    }
    public record OfflineAdminRevoke(UUID requestId,UUID accountId,long expectedVersion,String reason) {
        public OfflineAdminRevoke{text(reason,10,500);}
        @Override public String toString(){return "OfflineAdminRevoke[redacted]";}
    }
    public record OfflineAdminReceipt(UUID requestId,UUID accountId,String committedOutcome,Long committedVersion,Long currentVersion,String currentState,boolean replayed) {}
    public record OfflineAdminStatus(UUID accountId,UUID realmId,String currentState,long currentVersion,Set<String> currentPermissions) {
        public OfflineAdminStatus{currentPermissions=Set.copyOf(currentPermissions);}
    }
    private record Reservation(UUID account,UUID proof,UUID message,Instant created,Instant expires,boolean replayed,String state) {}
    @FunctionalInterface private interface Work<T>{T run(Connection connection)throws SQLException;}
    private final DataSource operator;private final InvitationProtection protection;
    public LocalAdminProvisioner(DataSource operator,InvitationProtection protection){this.operator=Objects.requireNonNull(operator);this.protection=Objects.requireNonNull(protection);}
    public OfflineAdminReceipt provision(OfflineAdminProvision input){
        if(input==null)throw rejected();new CustomerEmail(input.email());
        return transaction(c->{
            Reservation r;
            try(var q=c.prepareStatement("SELECT * FROM identity.provision_local_admin(?,?,?,?,?,?)")){
                q.setObject(1,input.requestId());q.setObject(2,input.realmId());q.setString(3,input.displayName());q.setString(4,input.email());
                q.setArray(5,c.createArrayOf("text",input.permissions().toArray(String[]::new)));q.setString(6,input.reason());
                try(var rows=q.executeQuery()){r=reservation(rows);}
            }
            if(!r.replayed())seal(c,input.requestId(),input.realmId(),r,input.email());
            var current=status(c,r.account());return new OfflineAdminReceipt(input.requestId(),r.account(),"PENDING_VERIFICATION",1L,current.currentVersion(),current.currentState(),r.replayed());
        });
    }
    public OfflineAdminReceipt disposition(OfflineAdminInvitationDisposition input){
        if(input==null||!Set.of("RESEND","CANCEL").contains(input.action()))throw rejected();
        return transaction(c->{
            Reservation r;
            try(var q=c.prepareStatement("SELECT * FROM identity.disposition_local_admin_invitation(?,?,?,?,?)")){
                q.setObject(1,input.requestId());q.setObject(2,input.accountId());q.setLong(3,input.expectedVersion());q.setString(4,input.action());q.setString(5,input.reason());
                try(var rows=q.executeQuery()){r=reservation(rows);}
            }
            var current=status(c,r.account());
            if(!r.replayed()&&input.action().equals("RESEND")){
                String destination;
                try(var q=c.prepareStatement("SELECT identity.local_admin_invitation_destination(?)")){q.setObject(1,input.requestId());try(var rows=q.executeQuery()){if(!rows.next())throw rejected();destination=rows.getString(1);}}
                seal(c,input.requestId(),current.realmId(),r,destination);
            }
            return new OfflineAdminReceipt(input.requestId(),r.account(),input.action().equals("CANCEL")?"CANCELLED":"RESENT",input.expectedVersion()+1,current.currentVersion(),current.currentState(),r.replayed());
        });
    }
    public OfflineAdminReceipt configure(OfflineAdminPermissions input){
        if(input==null)throw rejected();return transaction(c->{
            try(var q=c.prepareStatement("SELECT * FROM identity.configure_local_admin_permissions(?,?,?,?,?)")){
                q.setObject(1,input.requestId());q.setObject(2,input.accountId());q.setLong(3,input.expectedVersion());q.setArray(4,c.createArrayOf("text",input.permissions().toArray(String[]::new)));q.setString(5,input.reason());
                try(var r=q.executeQuery()){return receipt(r,"PERMISSIONS_UPDATED");}
            }
        });
    }
    public OfflineAdminReceipt revoke(OfflineAdminRevoke input){
        if(input==null)throw rejected();return transaction(c->{
            try(var q=c.prepareStatement("SELECT * FROM identity.revoke_local_admin(?,?,?,?)")){
                q.setObject(1,input.requestId());q.setObject(2,input.accountId());q.setLong(3,input.expectedVersion());q.setString(4,input.reason());
                try(var r=q.executeQuery()){return receipt(r,"REVOKED");}
            }
        });
    }
    public OfflineAdminStatus status(UUID account){return transaction(c->status(c,account));}
    private static OfflineAdminStatus status(Connection c,UUID account)throws SQLException{
        try(var q=c.prepareStatement("SELECT * FROM identity.local_admin_operator_status(?)")){
            q.setObject(1,account);try(var r=q.executeQuery()){
                if(!r.next())throw rejected();var permissions=new TreeSet<String>();var a=r.getArray("current_permissions");
                try{Collections.addAll(permissions,(String[])a.getArray());}finally{a.free();}
                var result=new OfflineAdminStatus(r.getObject("account_id",UUID.class),r.getObject("realm_id",UUID.class),r.getString("current_state"),r.getLong("current_version"),permissions);
                if(r.next())throw rejected();return result;
            }
        }
    }
    private void seal(Connection c,UUID request,UUID realm,Reservation r,String destination)throws SQLException{
        var proof=new ActionProof(r.proof(),r.account(),ActionProof.Purpose.INVITATION,r.created(),r.expires(),ActionProof.State.AVAILABLE,null);
        var selected=protection.context(proof,realm,destination);
        var context=new InvitationProtection.Context(r.message(),proof.id(),proof.accountId(),realm,destination,proof.createdAt(),proof.expiresAt(),selected.verifierKeyId(),selected.encryptionKeyId());
        try(var sealed=protection.issue(context);var q=c.prepareStatement("SELECT identity.seal_local_admin_invitation(?,?,?,?,?,?)")){
            byte[] hash=sealed.verifier(),nonce=sealed.nonce(),encrypted=sealed.ciphertext();
            try{q.setObject(1,request);q.setString(2,context.verifierKeyId());q.setString(3,context.encryptionKeyId());q.setBytes(4,hash);q.setBytes(5,nonce);q.setBytes(6,encrypted);q.execute();}
            finally{Arrays.fill(hash,(byte)0);Arrays.fill(nonce,(byte)0);Arrays.fill(encrypted,(byte)0);}
        }
    }
    private static Reservation reservation(ResultSet r)throws SQLException{
        if(!r.next())throw rejected();var expiry=r.getTimestamp("expires_at");
        var result=new Reservation(r.getObject("account_id",UUID.class),r.getObject("proof_id",UUID.class),r.getObject("message_id",UUID.class),r.getTimestamp("created_at").toInstant(),expiry==null?null:expiry.toInstant(),r.getBoolean("replayed"),r.getString("current_principal_state"));
        if(r.next())throw rejected();return result;
    }
    private static OfflineAdminReceipt receipt(ResultSet r,String outcome)throws SQLException{
        if(!r.next())throw rejected();var result=new OfflineAdminReceipt(r.getObject("request_id",UUID.class),r.getObject("account_id",UUID.class),outcome,r.getLong("committed_version"),r.getLong("current_version"),r.getString("current_state"),r.getBoolean("replayed"));if(r.next())throw rejected();return result;
    }
    private <T>T transaction(Work<T> work){
        if(TransactionSynchronizationManager.isActualTransactionActive()||TransactionSynchronizationManager.isSynchronizationActive()||!TransactionSynchronizationManager.getResourceMap().isEmpty())throw rejected();
        try(var c=operator.getConnection()){
            if(!c.getAutoCommit())throw rejected();c.setAutoCommit(false);
            try{T result=work.run(c);c.commit();return result;}catch(SQLException|RuntimeException failure){c.rollback();throw rejected();}
        }catch(SQLException failure){throw rejected();}
    }
    static void text(String value,int min,int max){
        if(value==null||value.indexOf(0)>=0||!java.nio.charset.StandardCharsets.UTF_8.newEncoder().canEncode(value))throw rejected();
        int length=value.codePointCount(0,value.length());if(length<min||length>max)throw rejected();
    }
    private static IllegalStateException rejected(){return new IllegalStateException("ADMIN_OPERATOR_COMMAND_REJECTED");}
    @Override public String toString(){return "LocalAdminProvisioner[redacted]";}
}
