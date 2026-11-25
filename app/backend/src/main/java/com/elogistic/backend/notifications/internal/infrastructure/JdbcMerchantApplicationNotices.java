package com.elogistic.backend.notifications.internal.infrastructure;

import com.elogistic.backend.identity.api.admin.AdminCommandAdmission;
import com.elogistic.backend.identity.api.customer.CustomerAdmission;
import com.elogistic.backend.notifications.api.MerchantApplicationNotices;
import com.elogistic.backend.platform.api.MerchantNoticeMail;
import java.time.*;
import java.util.*;
import javax.sql.DataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.*;

/** Notification-owned outbox. Merchant rows are verified only through its private SQL guard. */
public final class JdbcMerchantApplicationNotices implements MerchantApplicationNotices {
    private final DataSource source;private final JdbcTemplate jdbc;private final TransactionTemplate transactions;
    private final CustomerAdmission customers;private final AdminCommandAdmission admins;private final ProtectedMerchantNoticeRecipient protection;
    private final MerchantNoticeMail mail;private final Clock clock;
    public JdbcMerchantApplicationNotices(DataSource source,CustomerAdmission customers,AdminCommandAdmission admins,ProtectedMerchantNoticeRecipient protection,MerchantNoticeMail mail,Clock clock){
        this.source=Objects.requireNonNull(source);jdbc=new JdbcTemplate(source);transactions=new TransactionTemplate(new DataSourceTransactionManager(source));
        this.customers=Objects.requireNonNull(customers);this.admins=Objects.requireNonNull(admins);this.protection=Objects.requireNonNull(protection);this.mail=Objects.requireNonNull(mail);this.clock=Objects.requireNonNull(clock);
    }
    private static IllegalStateException denied(){return new IllegalStateException("MERCHANT_NOTICE_ADMISSION_DENIED");}
    private void admitted(Access access,CustomerAdmission.Subject recipient){
        if(access==null||recipient==null)throw denied();
        // Owner verification precedes reading any supplied actor or subject field.
        CustomerAdmission.Subject exact;
        if(access.customer()!=null){customers.requireCurrent(access.customer());exact=access.customer().actor().accountId().equals(recipient.accountId())?access.customer().actor():access.customer().subjects().get(recipient.accountId());}
        else{admins.requireCurrent(access.admin());exact=access.admin().customer(recipient.accountId());}
        if(exact!=recipient||!recipient.eligible()||recipient.verifiedEmail()==null||!TransactionSynchronizationManager.isActualTransactionActive()||!TransactionSynchronizationManager.hasResource(source))throw denied();
    }
    @Override public void enqueue(Access access,ApplicationEvent event,CustomerAdmission.Subject recipient){
        admitted(access,recipient);
        if(event==null||event.eventId()==null||event.applicationId()==null||!recipient.accountId().equals(event.applicantId()))throw denied();
        try{MerchantNoticeMail.Template.valueOf(event.template());}catch(IllegalArgumentException|NullPointerException invalid){throw denied();}
        UUID id=UUID.randomUUID();byte[] encrypted=protection.seal(id,event.eventId(),event.applicationId(),event.applicantId(),event.template(),recipient.verifiedEmail());
        try{
            int inserted=jdbc.update("INSERT INTO notification.merchant_application_notice VALUES (?,?,?,?,?,?,?,'QUEUED',0,NULL,?,NULL) ON CONFLICT(event_id,template) DO NOTHING",id,event.eventId(),event.applicationId(),event.applicantId(),encrypted,protection.activeKey(),event.template(),java.sql.Timestamp.from(clock.instant()));
            if(inserted==0){var row=jdbc.queryForMap("SELECT * FROM notification.merchant_application_notice WHERE event_id=? AND template=?",event.eventId(),event.template());
                if(!event.applicationId().equals(row.get("application_id"))||!event.applicantId().equals(row.get("applicant_id"))||!recipient.verifiedEmail().equals(protection.open((String)row.get("recipient_key_id"),(UUID)row.get("id"),event.eventId(),event.applicationId(),event.applicantId(),event.template(),(byte[])row.get("protected_recipient"))))throw denied();}
            admitted(access,recipient);
        }finally{Arrays.fill(encrypted,(byte)0);}
    }
    private record Pending(UUID id,UUID event,UUID app,UUID applicant,String template,String key,byte[] recipient,int attempt){@Override public String toString(){return "PendingMerchantNotice[redacted]";}}
    /** Explicit scheduler entry; rejects an enclosing transaction rather than sending before commit. */
    public boolean runOne(){
        if(TransactionSynchronizationManager.isActualTransactionActive())throw denied();
        Pending pending=transactions.execute(status->{
            Instant now=clock.instant();
            // Recover a crashed claim after its bounded lease. Stable ID supports provider deduplication,
            // but SMTP cannot promise exactly-once mailbox delivery after an unknown provider outcome.
            jdbc.update("UPDATE notification.merchant_application_notice SET state=CASE WHEN attempts>=5 THEN 'FAILED' ELSE 'RETRY_WAIT' END WHERE state='SENDING' AND next_attempt_at<=?",java.sql.Timestamp.from(now));
            var rows=jdbc.query("SELECT * FROM notification.merchant_application_notice WHERE state IN ('QUEUED','RETRY_WAIT') AND attempts<5 AND (next_attempt_at IS NULL OR next_attempt_at<=?) ORDER BY created_at,id LIMIT 1 FOR UPDATE SKIP LOCKED",(r,n)->new Pending(r.getObject("id",UUID.class),r.getObject("event_id",UUID.class),r.getObject("application_id",UUID.class),r.getObject("applicant_id",UUID.class),r.getString("template"),r.getString("recipient_key_id"),r.getBytes("protected_recipient"),r.getInt("attempts")+1),java.sql.Timestamp.from(now));
            if(rows.isEmpty())return null;var row=rows.getFirst();jdbc.update("UPDATE notification.merchant_application_notice SET state='SENDING',attempts=?,next_attempt_at=? WHERE id=?",row.attempt(),java.sql.Timestamp.from(now.plusSeconds(60)),row.id());return row;
        });
        if(pending==null)return false;MerchantNoticeMail.Outcome outcome;
        try{
            String destination=protection.open(pending.key(),pending.id(),pending.event(),pending.app(),pending.applicant(),pending.template(),pending.recipient());
            outcome=mail.send(new MerchantNoticeMail.Message(pending.id(),pending.app(),destination,MerchantNoticeMail.Template.valueOf(pending.template())));
            if(outcome==null||outcome.kind()==MerchantNoticeMail.Kind.ACCEPTED_BY_PROVIDER&&!outcome.reference().equals((outcome.sandbox()?"TEST:":"SMTP:")+pending.id()))throw denied();
        }catch(RuntimeException unavailable){outcome=new MerchantNoticeMail.Outcome(MerchantNoticeMail.Kind.BLOCKED,null,true);}
        finally{Arrays.fill(pending.recipient(),(byte)0);}
        var result=outcome;
        transactions.executeWithoutResult(status->{
            Instant now=clock.instant();String state=switch(result.kind()){case ACCEPTED_BY_PROVIDER->"ACCEPTED_BY_PROVIDER";case TRANSIENT_FAILURE->pending.attempt()<5?"RETRY_WAIT":"FAILED";case PERMANENT_FAILURE->"FAILED";case BLOCKED->"BLOCKED";};
            jdbc.update("UPDATE notification.merchant_application_notice SET state=?,next_attempt_at=?,accepted_at=? WHERE id=? AND state='SENDING' AND attempts=?",state,state.equals("RETRY_WAIT")?java.sql.Timestamp.from(now.plusSeconds(Math.min(3600,30L<<pending.attempt()))):null,state.equals("ACCEPTED_BY_PROVIDER")?java.sql.Timestamp.from(now):null,pending.id(),pending.attempt());
        });return true;
    }
    @Override public String toString(){return "MerchantApplicationNotices[redacted]";}
}
