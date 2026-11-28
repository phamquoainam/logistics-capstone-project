package com.elogistic.backend.identity.internal.infrastructure;

import com.elogistic.backend.identity.internal.domain.ActionProof;
import com.elogistic.backend.identity.internal.domain.CustomerEmail;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Arrays;
import java.util.Objects;
import java.util.UUID;
import javax.sql.DataSource;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** Offline composition only. Never register an operator DataSource as a runtime bean or HTTP service. */
public final class LocalSupportStaffProvisioner {
    public record OfflineStaffProvision(UUID requestId,String displayName,String email,String reason) {
        @Override public String toString(){return "OfflineStaffProvision[redacted]";}
    }
    public record OfflineStaffInvitationDisposition(UUID requestId,UUID accountId,String action,String reason) {
        @Override public String toString(){return "OfflineStaffInvitationDisposition[redacted]";}
    }
    public record OfflineStaffProvisionReceipt(UUID requestId,UUID accountId,UUID proofId,String committedOutcome,String currentPrincipalState,boolean replayed) {}
    private record Reservation(UUID account,UUID proof,UUID message,Instant created,Instant expires,boolean replayed,String state) {}
    private final DataSource operator;
    private final boolean operations;
    private final InvitationProtection protection;

    public LocalSupportStaffProvisioner(DataSource operator,InvitationProtection protection) {
        this(operator,protection,false);
    }
    private LocalSupportStaffProvisioner(DataSource operator,InvitationProtection protection,boolean operations) {
        this.operations=operations;this.operator=Objects.requireNonNull(operator);this.protection=Objects.requireNonNull(protection);
    }
    public static LocalSupportStaffProvisioner operations(DataSource source,InvitationProtection protection) { return new LocalSupportStaffProvisioner(source,protection,true); }
    private String function(String name) {return "identity."+name+(operations?"_operations_staff":"_support_staff");}
    public OfflineStaffProvisionReceipt provision(OfflineStaffProvision command) {
        if(command==null)throw rejected();new CustomerEmail(command.email());
        requireTransactionOwnership();
        try(Connection connection=operator.getConnection()) {
            if(!connection.getAutoCommit())throw rejected();connection.setAutoCommit(false);
            try {
                Reservation reservation;
                try(var statement=connection.prepareStatement("SELECT * FROM "+function("provision_local")+"(?,?,?,?)")) {
                    statement.setObject(1,command.requestId());statement.setString(2,command.displayName());
                    statement.setString(3,command.email());statement.setString(4,command.reason());
                    try(var rows=statement.executeQuery()){reservation=reservation(rows);}
                }
                if(!reservation.replayed())seal(connection,command.requestId(),reservation,command.email());
                connection.commit();
                return receipt(command.requestId(),reservation,"PENDING_VERIFICATION");
            }catch(SQLException|RuntimeException failure){connection.rollback();throw rejected();}
        }catch(SQLException failure){throw rejected();}
    }
    public OfflineStaffProvisionReceipt disposition(OfflineStaffInvitationDisposition command) {
        if(command==null || !java.util.Set.of("RESEND","CANCEL").contains(command.action()))throw rejected();
        requireTransactionOwnership();
        try(Connection connection=operator.getConnection()) {
            if(!connection.getAutoCommit())throw rejected();connection.setAutoCommit(false);
            try {
                Reservation reservation;
                try(var statement=connection.prepareStatement("SELECT * FROM "+function("disposition_local")+"_invitation(?,?,?,?)")) {
                    statement.setObject(1,command.requestId());statement.setObject(2,command.accountId());
                    statement.setString(3,command.action());statement.setString(4,command.reason());
                    try(var rows=statement.executeQuery()){reservation=reservation(rows);}
                }
                if(command.action().equals("RESEND")&&!reservation.replayed()) {
                    String destination;
                    try(var statement=connection.prepareStatement("SELECT "+function("local")+"_invitation_destination(?)")) {
                        statement.setObject(1,command.requestId());
                        try(var rows=statement.executeQuery()){if(!rows.next())throw rejected();destination=rows.getString(1);}
                    }
                    seal(connection,command.requestId(),reservation,destination);
                }
                connection.commit();
                return receipt(command.requestId(),reservation,command.action().equals("CANCEL")?"CANCELLED":"RESENT");
            }catch(SQLException|RuntimeException failure){connection.rollback();throw rejected();}
        }catch(SQLException failure){throw rejected();}
    }
    private void seal(Connection connection,UUID request,Reservation reservation,String destination) throws SQLException {
        var proof=new ActionProof(reservation.proof(),reservation.account(),ActionProof.Purpose.STAFF_INV,reservation.created(),reservation.expires(),ActionProof.State.AVAILABLE,null);
        var context=protection.staffContext(reservation.message(),proof,destination);
        try(var sealed=protection.issue(context);PreparedStatement statement=connection.prepareStatement("SELECT "+function("seal_local")+"_invitation(?,?,?,?,?,?)")) {
            byte[] verifier=sealed.verifier(),nonce=sealed.nonce(),ciphertext=sealed.ciphertext();
            try {
                statement.setObject(1,request);statement.setString(2,context.verifierKeyId());statement.setString(3,context.encryptionKeyId());
                statement.setBytes(4,verifier);statement.setBytes(5,nonce);statement.setBytes(6,ciphertext);statement.execute();
            }finally {Arrays.fill(verifier,(byte)0);Arrays.fill(nonce,(byte)0);Arrays.fill(ciphertext,(byte)0);}
        }
    }
    private static Reservation reservation(ResultSet rows) throws SQLException {
        if(!rows.next())throw rejected();
        var expiry=rows.getTimestamp("expires_at");
        var value=new Reservation(rows.getObject("account_id",UUID.class),rows.getObject("proof_id",UUID.class),rows.getObject("message_id",UUID.class),rows.getTimestamp("created_at").toInstant(),expiry==null?null:expiry.toInstant(),rows.getBoolean("replayed"),rows.getString("current_principal_state"));
        if(rows.next())throw rejected();return value;
    }
    private static OfflineStaffProvisionReceipt receipt(UUID request,Reservation reservation,String outcome) {
        return new OfflineStaffProvisionReceipt(request,reservation.account(),reservation.proof(),outcome,reservation.state(),reservation.replayed());
    }
    private static void requireTransactionOwnership() {
        if(TransactionSynchronizationManager.isActualTransactionActive() || TransactionSynchronizationManager.isSynchronizationActive() || !TransactionSynchronizationManager.getResourceMap().isEmpty())throw rejected();
    }
    private static IllegalStateException rejected(){return new IllegalStateException("STAFF_OPERATOR_COMMAND_REJECTED");}
    @Override public String toString(){return "LocalSupportStaffProvisioner[redacted]";}
}
