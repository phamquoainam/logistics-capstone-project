package com.elogistic.backend.orders.internal.infrastructure;
import com.elogistic.backend.orders.api.*;
import com.elogistic.backend.orders.api.PilotShipments.*;
import com.elogistic.backend.orders.internal.application.port.PilotShipmentStore;
import com.elogistic.backend.pricing.api.PilotQuotes;
import java.sql.*;
import java.time.Instant;
import java.util.*;
import javax.sql.DataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import tools.jackson.databind.json.JsonMapper;
public final class JdbcPilotShipmentStore implements PilotShipmentStore {
    private static final JsonMapper JSON=JsonMapper.builder().build();private final DataSource source;private final JdbcTemplate jdbc;
    public JdbcPilotShipmentStore(DataSource source){this.source=source;jdbc=new JdbcTemplate(source);}
    private void joined(){if(!TransactionSynchronizationManager.isActualTransactionActive()||!TransactionSynchronizationManager.hasResource(source))throw new IllegalStateException("PILOT_SHIPMENT_ADMISSION_TRANSACTION_REQUIRED");}
    @Override public Optional<BookingReceipt> receipt(UUID owner,UUID key){joined();return jdbc.query("SELECT draft_id,draft_version,quote_id,shipment_id FROM orders.pilot_booking_receipt WHERE owner_id=? AND operation_key=?",(r,n)->new BookingReceipt(r.getObject(1,UUID.class),r.getLong(2),r.getObject(3,UUID.class),r.getObject(4,UUID.class)),owner,key).stream().findFirst();}
    private Shipment row(ResultSet r,int n)throws SQLException{try{UUID id=r.getObject("id",UUID.class);var parcels=jdbc.query("SELECT id,parcel_number,description,quantity,weight_grams,length_mm,width_mm,height_mm,declared_value_vnd FROM orders.pilot_parcel WHERE shipment_id=? ORDER BY parcel_number",(p,i)->new Parcel(p.getObject(1,UUID.class),p.getInt(2),new ShippingDrafts.Parcel(p.getString(3),p.getLong(4),p.getLong(5),p.getLong(6),p.getLong(7),p.getLong(8),p.getLong(9))),id);return new Shipment(id,r.getObject("owner_id",UUID.class),r.getString("code"),r.getObject("draft_id",UUID.class),r.getLong("draft_version"),r.getString("booking_state"),r.getString("carrier_state"),JSON.readValue(r.getString("draft_snapshot"),ShippingDrafts.Input.class),JSON.readValue(r.getString("quote_snapshot"),PilotQuotes.Quote.class),r.getLong("total_vnd"),r.getTimestamp("created_at").toInstant(),parcels);}catch(RuntimeException invalid){throw new IllegalStateException("PILOT_SHIPMENT_SNAPSHOT_REQUIRED");}}
    @Override public Shipment find(UUID owner,UUID id){joined();return jdbc.query("SELECT * FROM orders.pilot_shipment WHERE owner_id=? AND id=?",this::row,owner,id).stream().findFirst().orElseThrow(()->new PilotBookingFailure(PilotBookingFailure.Code.RESOURCE_NOT_FOUND));}
    @Override public List<Shipment> list(UUID owner,int page,String search){joined();String sql="SELECT * FROM orders.pilot_shipment WHERE owner_id=?";List<Object> args=new ArrayList<>();args.add(owner);if(search!=null){sql+=" AND lower(concat_ws(' ',code,draft_snapshot->>'reference',draft_snapshot#>>'{sender,name}',draft_snapshot#>>'{recipient,name}')) LIKE ? ESCAPE '!'";args.add("%"+search.toLowerCase(Locale.ROOT).replace("!","!!").replace("%","!%").replace("_","!_")+"%");}sql+=" ORDER BY created_at DESC,id ASC LIMIT 21 OFFSET ?";args.add(Math.multiplyExact((long)page,20));return jdbc.query(sql,this::row,args.toArray());}
    private static void one(int changed){if(changed!=1)throw new IllegalStateException("PILOT_BOOKING_ATOMIC_WRITE_REQUIRED");}
    @Override public void insert(Shipment s){joined();one(jdbc.update("INSERT INTO orders.pilot_shipment(id,owner_id,owner_type,code,draft_id,draft_version,booking_state,carrier_state,draft_snapshot,quote_snapshot,quote_id,total_vnd,created_at) VALUES (?,?,'PERSONAL',?,?,?,?,?,CAST(? AS jsonb),CAST(? AS jsonb),?,?,?)",s.id(),s.accountId(),s.code(),s.draftId(),s.draftVersion(),s.bookingState(),s.carrierState(),JSON.writeValueAsString(s.input()),JSON.writeValueAsString(s.quote()),s.quote().id(),s.totalVnd(),Timestamp.from(s.createdAt())));for(var p:s.parcels()){var f=p.facts();one(jdbc.update("INSERT INTO orders.pilot_parcel(id,shipment_id,parcel_number,description,quantity,weight_grams,length_mm,width_mm,height_mm,declared_value_vnd) VALUES (?,?,?,?,?,?,?,?,?,?)",p.id(),s.id(),p.parcelNumber(),f.description(),f.quantity(),f.weightGrams(),f.lengthMm(),f.widthMm(),f.heightMm(),f.declaredValueVnd()));}}
    @Override public void receipt(UUID owner,UUID key,BookingReceipt r,Instant now){joined();one(jdbc.update("INSERT INTO orders.pilot_booking_receipt(owner_id,operation_key,draft_id,draft_version,quote_id,shipment_id,created_at) VALUES (?,?,?,?,?,?,?)",owner,key,r.draft(),r.version(),r.quote(),r.shipment(),Timestamp.from(now)));}
}
