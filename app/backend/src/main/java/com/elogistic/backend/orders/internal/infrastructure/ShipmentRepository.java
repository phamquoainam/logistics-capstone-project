package com.elogistic.backend.orders.internal.infrastructure;
import com.elogistic.backend.orders.internal.domain.*;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.*;
import org.springframework.stereotype.Repository;
@Repository @Profile("demo")
public class ShipmentRepository {
 private final JdbcTemplate jdbc;
 private static final RowMapper<ShipmentView> VIEW=(r,n)->new ShipmentView(r.getObject("id",UUID.class),r.getObject("merchant_id",UUID.class),r.getString("reference"),r.getString("pickup_address"),r.getString("recipient_name"),r.getString("recipient_phone"),r.getString("delivery_street"),r.getString("delivery_ward"),r.getString("delivery_province"),r.getString("parcel_description"),r.getBigDecimal("weight_kg"),r.getString("status"),r.getObject("created_by",UUID.class),r.getTimestamp("created_at").toInstant(),r.getTimestamp("updated_at").toInstant());
 public record Receipt(String hash,UUID shipmentId) {}
 public record Summary(UUID id,UUID merchantId,String reference,String status,Instant createdAt) {}
 public record Overview(long total,long draft,long cancelled) {}
 public ShipmentRepository(JdbcTemplate jdbc){this.jdbc=jdbc;}
 public List<ShipmentView> list(UUID merchant){return jdbc.query("SELECT * FROM orders.demo_shipment WHERE merchant_id=? ORDER BY created_at DESC LIMIT 200",VIEW,merchant);}
 public Optional<ShipmentView> find(UUID merchant,UUID id,boolean lock){return jdbc.query("SELECT * FROM orders.demo_shipment WHERE merchant_id=? AND id=?"+(lock?" FOR UPDATE":""),VIEW,merchant,id).stream().findFirst();}
 public List<ShipmentEvent> events(UUID shipment){return jdbc.query("SELECT * FROM orders.demo_event WHERE shipment_id=? ORDER BY occurred_at,id",(r,n)->new ShipmentEvent(r.getObject("id",UUID.class),r.getString("event_type"),r.getObject("actor_id",UUID.class),r.getString("reason"),r.getTimestamp("occurred_at").toInstant()),shipment);}
 public Optional<Receipt> receipt(UUID merchant,String operation,String key){return jdbc.query("SELECT payload_hash,shipment_id FROM orders.demo_operation WHERE merchant_id=? AND operation=? AND operation_key=?",(r,n)->new Receipt(r.getString(1),r.getObject(2,UUID.class)),merchant,operation,key).stream().findFirst();}
 public void receipt(UUID merchant,String operation,String key,String hash,UUID shipment){jdbc.update("INSERT INTO orders.demo_operation VALUES (?,?,?,?,?)",merchant,operation,key,hash,shipment);}
 public void create(UUID id,UUID merchant,UUID actor,DraftInput d){
  Timestamp now=Timestamp.from(Instant.now());
  jdbc.update("INSERT INTO orders.demo_shipment VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",id,merchant,d.reference(),d.pickupAddress(),d.recipientName(),d.recipientPhone(),d.deliveryStreet(),d.deliveryWard(),d.deliveryProvince(),d.parcelDescription(),d.weightKg(),"DRAFT",actor,now,now);
  event(id,"DRAFT_CREATED",actor,null);
 }
 public void cancel(UUID merchant,UUID shipment,UUID actor,String reason){
  jdbc.update("UPDATE orders.demo_shipment SET status='CANCELLED',updated_at=? WHERE merchant_id=? AND id=? AND status='DRAFT'",Timestamp.from(Instant.now()),merchant,shipment);
  event(shipment,"DRAFT_CANCELLED",actor,reason);
 }
 private void event(UUID shipment,String type,UUID actor,String reason){jdbc.update("INSERT INTO orders.demo_event VALUES (?,?,?,?,?,?)",UUID.randomUUID(),shipment,type,actor,reason,Timestamp.from(Instant.now()));}
 public List<Summary> summaries(){return jdbc.query("SELECT id,merchant_id,reference,status,created_at FROM orders.demo_shipment ORDER BY created_at DESC LIMIT 200",(r,n)->new Summary(r.getObject(1,UUID.class),r.getObject(2,UUID.class),r.getString(3),r.getString(4),r.getTimestamp(5).toInstant()));}
 public Overview overview(){return new Overview(count(null),count("DRAFT"),count("CANCELLED"));}
 private long count(String status){return status==null?jdbc.queryForObject("SELECT COUNT(*) FROM orders.demo_shipment",Long.class):jdbc.queryForObject("SELECT COUNT(*) FROM orders.demo_shipment WHERE status=?",Long.class,status);}
}
