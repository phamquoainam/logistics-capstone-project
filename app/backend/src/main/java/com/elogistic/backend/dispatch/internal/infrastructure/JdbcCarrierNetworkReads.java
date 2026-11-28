package com.elogistic.backend.dispatch.internal.infrastructure;
import com.elogistic.backend.dispatch.api.CarrierNetworkReads;
import java.time.*;
import java.sql.*;
import java.util.*;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import tools.jackson.databind.json.JsonMapper;

/** Canonical Dispatch registry projection with authorization before filtering/paging. */
@Component @Profile("identity")
public final class JdbcCarrierNetworkReads implements CarrierNetworkReads {
    private final DataSource source;private final JdbcTemplate jdbc;private final Clock clock;
    private static final JsonMapper JSON=JsonMapper.builder().build();
    private static final String CURRENT=" FROM dispatch.carrier_site s JOIN dispatch.carrier_site_version v ON v.site_id=s.id WHERE v.effective_from<=? AND v.version=(SELECT max(x.version) FROM dispatch.carrier_site_version x WHERE x.site_id=s.id AND x.effective_from<=?) AND (v.effective_to IS NULL OR v.effective_to>?)";
    public JdbcCarrierNetworkReads(DataSource source,@Qualifier("identityHttpClock")Clock clock){this.source=source;jdbc=new JdbcTemplate(source);this.clock=clock;}
    private void admitted(Access a){a.requireNetworkFacts();if(!TransactionSynchronizationManager.isActualTransactionActive()||!TransactionSynchronizationManager.hasResource(source))throw new IllegalStateException("NETWORK_READ_TRANSACTION_REQUIRED");}
    @Override public SitePage sites(Access a,SiteQuery q){
        admitted(a);if(q==null||q.page()<0||q.page()>100000||q.size()<1||q.size()>100||q.state()!=null&&!Set.of("ACTIVE","INACTIVE").contains(q.state())||q.type()!=null&&!Set.of("STATION","HUB").contains(q.type())||q.areaCode()!=null&&!q.areaCode().matches("[A-Z0-9-]{1,32}"))throw new IllegalArgumentException("VALIDATION_FAILED");
        Timestamp t=Timestamp.from(clock.instant());List<Object> args=new ArrayList<>(List.of(t,t,t));String where=CURRENT;
        if(q.state()!=null){where+=" AND v.state=?";args.add(q.state());}if(q.type()!=null){where+=" AND s.type=?";args.add(q.type());}if(q.areaCode()!=null){where+=" AND v.area_code=?";args.add(q.areaCode());}
        long total=Objects.requireNonNull(jdbc.queryForObject("SELECT count(*)"+where,Long.class,args.toArray()));
        args.add(q.size());args.add((long)q.page()*q.size());var rows=jdbc.query("SELECT s.id,s.code,s.type,v.*"+where+" ORDER BY s.code LIMIT ? OFFSET ?",this::row,args.toArray());
        a.requireNetworkFacts();return new SitePage(rows,total,q.page(),q.size());
    }
    @Override public SiteDetail site(Access a,UUID id){admitted(a);if(id==null)throw new IllegalArgumentException("VALIDATION_FAILED");Timestamp t=Timestamp.from(clock.instant());var rows=jdbc.query("SELECT s.id,s.code,s.type,v.*"+CURRENT+" AND s.id=?",this::row,t,t,t,id);a.requireNetworkFacts();return rows.stream().findFirst().orElseThrow(()->new NoSuchElementException("RESOURCE_NOT_FOUND"));}
    private SiteDetail row(ResultSet r,int index)throws SQLException{
        Map<String,List<String>> hours=new HashMap<>();var node=JSON.readTree(r.getString("operating_hours"));for(var field:node.properties()){List<String> values=new ArrayList<>();for(var n:field.getValue())values.add(n.asString());hours.put(field.getKey(),List.copyOf(values));}
        UUID id=r.getObject(1,UUID.class);Timestamp t=Timestamp.from(clock.instant());
        var links=jdbc.query("SELECT l.* FROM dispatch.carrier_site_link l WHERE l.from_site_id=? AND l.effective_from<=? AND (l.effective_to IS NULL OR l.effective_to>?) AND l.version=(SELECT max(x.version) FROM dispatch.carrier_site_link x WHERE x.link_id=l.link_id AND x.effective_from<=?) ORDER BY l.to_site_id,l.link_id",(x,n)->new SiteLink(x.getObject("link_id",UUID.class),x.getObject("to_site_id",UUID.class),x.getString("service_code"),x.getString("dataset_version"),x.getString("area_code"),x.getString("drop_off_code"),x.getTimestamp("effective_from").toInstant(),instant(x,"effective_to"),x.getLong("version")),id,t,t,t);
        return new SiteDetail(id,r.getString("code"),r.getString("name"),r.getString("type"),r.getString("state"),r.getLong("version"),r.getString("location_text"),r.getString("area_code"),r.getBigDecimal("latitude"),r.getBigDecimal("longitude"),r.getString("timezone"),hours,List.of((String[])r.getArray("capabilities").getArray()),r.getTimestamp("effective_from").toInstant(),instant(r,"effective_to"),r.getString("provenance"),links);
    }
    private static Instant instant(ResultSet r,String field)throws SQLException{var t=r.getTimestamp(field);return t==null?null:t.toInstant();}
}
