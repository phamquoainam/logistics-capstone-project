package com.elogistic.backend.notifications.internal.infrastructure;
import com.elogistic.backend.notifications.api.OrganizationActionNotices;
import com.elogistic.backend.notifications.api.OrganizationActionNotices.*;
import com.elogistic.backend.orders.api.BusinessPreparations;
import java.sql.*;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.Predicate;
import javax.sql.DataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** In-app metadata only; no emails, contact bodies or Carrier event payloads. */
public final class JdbcOrganizationActionNotices implements OrganizationActionNotices {
    private final DataSource source;private final JdbcTemplate jdbc;private final Clock clock;
    public JdbcOrganizationActionNotices(DataSource source,Clock clock){this.source=source;jdbc=new JdbcTemplate(source);this.clock=clock;}
    private UUID org(Access a){a.requireCurrent();if(!TransactionSynchronizationManager.isActualTransactionActive()||!TransactionSynchronizationManager.hasResource(source))throw new IllegalStateException("ACTION_NOTICE_TRANSACTION_REQUIRED");return a.organizationId();}
    private static Notice row(ResultSet r,int ignored)throws SQLException{Timestamp read=r.getTimestamp("read_at");return new Notice(r.getObject("id",UUID.class),r.getObject("organization_id",UUID.class),r.getObject("task_id",UUID.class),r.getObject("event_id",UUID.class),r.getString("type"),r.getTimestamp("created_at").toInstant(),read==null?null:read.toInstant());}
    @Override public void record(Access a,UUID task,UUID event,UUID recipient,UUID facility,String type,Instant occurredAt){jdbc.update("INSERT INTO notification.organization_action_notice VALUES(?,?,?,?,?,?,?,?,NULL)",UUID.randomUUID(),org(a),task,event,recipient,facility,type,Timestamp.from(occurredAt));a.requireCurrent();}
    @Override public Page list(Access a,int page,Predicate<UUID> readable){if(page<0)throw new BusinessPreparations.Failure("VALIDATION_FAILED");List<Object> args=new ArrayList<>(List.of(org(a),a.actorAccountId()));String where="organization_id=? AND recipient_account_id=?";if(!a.canReadAllTasks()){var sites=a.readableFacilities();if(sites.isEmpty())throw new BusinessPreparations.Failure("RESOURCE_NOT_FOUND");where+=" AND facility_id IN ("+String.join(",",Collections.nCopies(sites.size(),"?"))+")";args.addAll(sites);}args.add(Math.multiplyExact((long)page,20));var rows=jdbc.query("SELECT * FROM notification.organization_action_notice WHERE "+where+" ORDER BY created_at DESC,id ASC LIMIT 21 OFFSET ?",JdbcOrganizationActionNotices::row,args.toArray());List<Notice> visible=rows.stream().limit(20).filter(n->readable.test(n.taskId())).toList();a.requireCurrent();return new Page(visible,page,20,rows.size()>20);}
    @Override public Notice read(Access a,UUID id,Predicate<UUID> readable){var rows=jdbc.query("SELECT * FROM notification.organization_action_notice WHERE id=? AND organization_id=? AND recipient_account_id=?",JdbcOrganizationActionNotices::row,id,org(a),a.actorAccountId());if(rows.size()!=1||!readable.test(rows.getFirst().taskId()))throw new BusinessPreparations.Failure("RESOURCE_NOT_FOUND");var n=rows.getFirst();if(n.readAt()!=null)return n;Instant now=clock.instant().truncatedTo(ChronoUnit.MICROS);jdbc.update("UPDATE notification.organization_action_notice SET read_at=? WHERE id=? AND read_at IS NULL",Timestamp.from(now),id);a.requireCurrent();return new Notice(n.id(),n.organizationId(),n.taskId(),n.eventId(),n.type(),n.createdAt(),now);}
}
