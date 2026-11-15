package com.elogistic.backend.orders.internal.infrastructure;

import com.elogistic.backend.orders.api.*;
import com.elogistic.backend.orders.api.BusinessShipmentManagement.*;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.sql.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.Consumer;
import javax.sql.DataSource;
import org.springframework.jdbc.core.*;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import tools.jackson.databind.json.JsonMapper;

/** One scoped SQL cursor captures immutable fee bindings, source and internal assignment. */
public final class JdbcBusinessShipmentManagement implements BusinessShipmentManagement, BusinessReportSources {
    private static final JsonMapper JSON=JsonMapper.builder().build();
    private static final DateTimeFormatter VIETNAM=DateTimeFormatter.ofPattern("dd/MM/uuuu HH:mm:ss").withZone(ZoneId.of("Asia/Ho_Chi_Minh"));
    private final DataSource source;private final JdbcTemplate jdbc;private final BusinessShipments shipments;private final BusinessPreparationProtection protection;private final Clock clock;private final boolean enabled;
    public JdbcBusinessShipmentManagement(DataSource source,BusinessShipments shipments,BusinessPreparationProtection protection,Clock clock,boolean enabled){this.source=source;this.jdbc=new JdbcTemplate(source);this.shipments=shipments;this.protection=protection;this.clock=clock;this.enabled=enabled;}
    private static BusinessPreparations.Failure fail(String code){return new BusinessPreparations.Failure(code);}
    private UUID org(Access a){a.requireCurrent();if(!enabled)throw fail("BUSINESS_DISABLED");if(!TransactionSynchronizationManager.isActualTransactionActive()||!TransactionSynchronizationManager.hasResource(source))throw new IllegalStateException("ORGANIZATION_MANAGEMENT_ADMISSION_REQUIRED");return a.scope().context().organizationId();}
    private void permit(Access a,String permission,UUID site){org(a);if(!a.scope().permits("shipment.draft.read",site)||!a.scope().permits(permission,site))throw fail("RESOURCE_NOT_FOUND");}
    private void any(Access a,String permission){org(a);if(!a.scope().permits(permission,null)&&a.scope().facilities(permission).isEmpty())throw fail("RESOURCE_NOT_FOUND");}
    private record Captured(UUID id,UUID quote,UUID site,long amount,Source source,Assignment assignment,BusinessReportSources.Row report){}
    private record Capture(Instant at,BigInteger total,BigInteger unassigned,BigInteger fees,List<Captured> rows){}
    private Captured captured(ResultSet r,UUID org,boolean reporting)throws SQLException{
        if(!"NOT_ACCEPTED".equals(r.getString("carrier_state")))throw new IllegalStateException("BUSINESS_LIFECYCLE_SOURCE_UNAVAILABLE");
        UUID id=r.getObject("id",UUID.class),site=r.getObject("facility_id",UUID.class);List<UUID> batches=new ArrayList<>();for(var v:JSON.readTree(r.getString("batches")))batches.add(UUID.fromString(v.asString()));
        BusinessReportSources.Row report=null;
        if(reporting){var quote=JSON.readValue(r.getString("report_quote"),com.elogistic.backend.pricing.api.BusinessQuotes.Quote.class);
            if(!quote.id().equals(r.getObject("quote_id",UUID.class))||!quote.organizationId().equals(org)||!Objects.equals(quote.facilityId(),site)||!Objects.equals(quote.consumedBy(),id)||quote.totalVnd()!=r.getLong("total_vnd"))throw new IllegalStateException("REPORT_QUOTE_BINDING_REQUIRED");
            report=new BusinessReportSources.Row(id,r.getString("code"),r.getTimestamp("created_at").toInstant(),site,r.getObject("creator_account_id",UUID.class),r.getString("channel"),r.getString("external_ref"),r.getObject("assignee_account_id",UUID.class),new BusinessReportSources.Quote(quote.id(),quote.contractId(),quote.contractVersion(),quote.tariffVersion(),new BusinessReportSources.Route(quote.route().originCode(),quote.route().destinationCode()),quote.parcels().stream().map(p->new BusinessReportSources.Parcel(p.weightGrams())).toList(),quote.feeLines().stream().map(l->new BusinessReportSources.Line(l.parcelNumber(),l.actualGrams(),l.chargeableGrams(),l.feeVnd())).toList(),quote.totalVnd()));
        }
        var at=r.getTimestamp("assignment_at");return new Captured(id,r.getObject("quote_id",UUID.class),site,r.getLong("total_vnd"),new Source(r.getString("channel"),r.getString("external_ref"),site,batches),new Assignment(id,org,r.getObject("assignee_account_id",UUID.class),r.getLong("assignment_version"),at==null?null:at.toInstant()),report);
    }
    private Capture capture(Access a,BusinessManagementFilters f,boolean export,Consumer<List<FeeBinding>> validate){return capture(a,f,export,validate,null,false);}
    private void scope(Access a,String permission,String alias,StringBuilder where,List<Object> args){
        if(!a.scope().permits(permission,null)){var sites=a.scope().facilities(permission);if(sites.isEmpty())throw fail("RESOURCE_NOT_FOUND");where.append(" AND "+alias+".facility_id IN ("+String.join(",",Collections.nCopies(sites.size(),"?"))+")");args.addAll(sites);}
    }
    private Capture capture(Access a,BusinessManagementFilters f,boolean export,Consumer<List<FeeBinding>> validate,String reportPermission,boolean reportExport){
        UUID org=org(a);if(validate==null)throw new IllegalStateException("FINANCE_CAPTURE_VALIDATION_REQUIRED");
        if(f.facility()!=null&&(!a.scope().facilityExists(f.facility())||!a.scope().permits("shipment.draft.read",f.facility())))throw fail("RESOURCE_NOT_FOUND");
        if(f.creator()!=null&&!a.scope().memberExists(f.creator()))throw fail("RESOURCE_NOT_FOUND");
        if(f.assignee()!=null&&!f.assignee().equals("NONE")&&!a.scope().memberExists(UUID.fromString(f.assignee())))throw fail("RESOURCE_NOT_FOUND");
        if(f.batch()!=null){var sites=jdbc.query("SELECT facility_id FROM orders.organization_import_job WHERE id=? AND organization_id=?",(r,n)->Optional.ofNullable(r.getObject(1,UUID.class)),f.batch(),org);if(sites.size()!=1||!a.scope().permits("shipment.draft.read",sites.getFirst().orElse(null))||reportPermission!=null&&!a.scope().permits(reportPermission,sites.getFirst().orElse(null)))throw fail("RESOURCE_NOT_FOUND");}
        if(reportPermission!=null){any(a,reportPermission);if(f.facility()!=null)permit(a,reportPermission,f.facility());}
        List<Object> args=new ArrayList<>();args.add(org);args.add(Timestamp.from(f.until()));StringBuilder where=new StringBuilder("s.owner_type='BUSINESS' AND s.booking_state='CONFIRMED' AND s.organization_id=? AND s.created_at<?");
        if(!a.scope().permits("shipment.draft.read",null)){var sites=a.scope().facilities("shipment.draft.read");if(sites.isEmpty())throw fail("RESOURCE_NOT_FOUND");where.append(" AND s.facility_id IN ("+String.join(",",Collections.nCopies(sites.size(),"?"))+")");args.addAll(sites);}
        if(reportPermission!=null)scope(a,reportPermission,"s",where,args);
        if(f.from()!=null){where.append(" AND s.created_at>=?");args.add(Timestamp.from(f.from()));}
        if(f.facility()!=null){where.append(" AND s.facility_id=?");args.add(f.facility());}
        if(f.creator()!=null){where.append(" AND s.creator_account_id=?");args.add(f.creator());}
        if(f.assignee()!=null){if(f.assignee().equals("NONE"))where.append(" AND a.assignee_account_id IS NULL");else{where.append(" AND a.assignee_account_id=?");args.add(UUID.fromString(f.assignee()));}}
        if(f.origin()!=null){where.append(" AND s.quote_snapshot#>>'{route,originCode}'=? AND s.quote_snapshot#>>'{route,destinationCode}'=?");args.add(f.origin());args.add(f.destination());}
        if(f.sourceChannel()!=null){where.append(" AND v.snapshot->>'sourceChannel'=?");args.add(f.sourceChannel());}
        if(f.batch()!=null){where.append(" AND EXISTS(SELECT 1 FROM orders.organization_import_row i WHERE i.organization_id=s.organization_id AND i.preparation_id=s.organization_preparation_id AND i.state='CREATED' AND i.job_id=?)");args.add(f.batch());}
        if(!f.selected().isEmpty()){where.append(" AND s.id IN ("+String.join(",",Collections.nCopies(f.selected().size(),"?"))+")");args.addAll(f.selected());}
        if(f.search()!=null){where.append(" AND lower(concat_ws(' ',s.code,s.draft_snapshot->>'reference',v.snapshot->>'sourceOrderReference',s.draft_snapshot#>>'{recipient,name}',s.draft_snapshot#>>'{recipient,phone}',CASE WHEN s.draft_snapshot#>>'{recipient,phone}' LIKE '+84%' THEN '0'||substring(s.draft_snapshot#>>'{recipient,phone}' FROM 4) END,(SELECT string_agg('AP-P-'||replace(p.id::text,'-','')||' '||p.id::text,' ') FROM orders.pilot_parcel p WHERE p.shipment_id=s.id))) LIKE ? ESCAPE '!'");args.add(BusinessManagementFilters.like(f.search()));}
        String sql="SELECT "+(reportPermission==null?"":"s.code,s.created_at,s.creator_account_id,s.quote_snapshot::text report_quote,")+"s.id,s.quote_id,s.facility_id,s.total_vnd,s.carrier_state,v.snapshot->>'sourceChannel' channel,v.snapshot->>'sourceOrderReference' external_ref,a.assignee_account_id,coalesce(a.version,0) assignment_version,a.updated_at assignment_at,coalesce((SELECT jsonb_agg(b.job_id ORDER BY b.job_id)::text FROM (SELECT DISTINCT i.job_id FROM orders.organization_import_row i WHERE i.organization_id=s.organization_id AND i.preparation_id=s.organization_preparation_id AND i.state='CREATED') b),'[]') batches FROM orders.pilot_shipment s JOIN orders.organization_preparation_version v ON v.id=s.organization_preparation_id AND v.organization_id=s.organization_id AND v.version=s.organization_preparation_version LEFT JOIN orders.organization_shipment_assignment a ON a.shipment_id=s.id AND a.organization_id=s.organization_id WHERE "+where+" ORDER BY "+f.order();
        Instant now=clock.instant().truncatedTo(ChronoUnit.MICROS);
        var result=jdbc.query(c->{var statement=c.prepareStatement(sql);statement.setFetchSize(256);new ArgumentPreparedStatementSetter(args.toArray()).setValues(statement);return statement;},(ResultSetExtractor<Capture>)r->{
            BigInteger total=BigInteger.ZERO,unassigned=BigInteger.ZERO,fees=BigInteger.ZERO,offset=BigInteger.valueOf((long)f.page()*20);List<Captured> rows=new ArrayList<>();List<FeeBinding> chunk=new ArrayList<>();
            while(r.next()){
                var row=captured(r,org,reportPermission!=null);if(reportPermission!=null){if(rows.size()==10000)throw fail("REPORT_LIMIT_EXCEEDED");if(reportExport)permit(a,"shipment.export",row.site());rows.add(row);}else if(export){permit(a,"shipment.export",row.site());if(f.includePii())permit(a,"shipment.export.pii",row.site());if(rows.size()==200)throw fail("EXPORT_LIMIT_EXCEEDED");rows.add(row);}else if(total.compareTo(offset)>=0&&total.compareTo(offset.add(BigInteger.valueOf(21)))<0)rows.add(row);
                total=total.add(BigInteger.ONE);if(row.assignment().assigneeAccountId()==null)unassigned=unassigned.add(BigInteger.ONE);fees=fees.add(BigInteger.valueOf(row.amount()));
                chunk.add(new FeeBinding(row.id(),row.quote(),row.site(),row.amount()));if(chunk.size()==256){validate.accept(List.copyOf(chunk));a.requireCurrent();chunk.clear();}
            }
            if(!chunk.isEmpty())validate.accept(List.copyOf(chunk));if(!f.selected().isEmpty()&&!total.equals(BigInteger.valueOf(f.selected().size())))throw fail("RESOURCE_NOT_FOUND");
            return new Capture(now,total,unassigned,fees,List.copyOf(rows));
        });a.requireCurrent();if(result==null)throw new IllegalStateException("BUSINESS_COHORT_CAPTURE_REQUIRED");return result;
    }
    private Item item(Access a,Captured row){
        var actions=new ArrayList<String>();if(a.scope().permits("shipment.label.read",row.site()))actions.add("READ_LABEL");if(a.scope().permits("shipment.export",row.site())){actions.add("EXPORT");if(a.scope().permits("shipment.export.pii",row.site()))actions.add("EXPORT_PII");}
        if(a.scope().permits("shipment.assign",row.site()))actions.add("ASSIGN");return new Item(shipments.get(a,row.id()),row.source(),row.assignment(),actions);
    }
    @Override public Page read(Access a,Map<String,String> parameters,Consumer<List<FeeBinding>> validate){org(a);var f=BusinessManagementFilters.parse(parameters,clock.instant(),false);var c=capture(a,f,false,validate);return new Page(c.at(),new Summary(c.total().toString(),c.total().toString(),c.unassigned().toString(),c.fees().toString(),c.fees().toString()),c.total().toString(),f.page(),c.rows().size()>20,c.rows().stream().limit(20).map(row->item(a,row)).toList());}
    private static String text(Object value){return value==null?"":value.toString();}
    @Override public Export export(Access a,Map<String,String> parameters,Consumer<List<FeeBinding>> validate){
        UUID org=org(a);any(a,"shipment.export");var f=BusinessManagementFilters.parse(parameters,clock.instant(),true);if(f.includePii())any(a,"shipment.export.pii");var c=capture(a,f,true,validate);
        var columns=new ArrayList<>(List.of("shipmentCode","bookingTimeVietnam","reference","sourceChannel","sourceOrderReference","service","origin","destination","parcelCount","totalVnd","paymentState","creatorAccountId","facilityId","importJobIds","assigneeAccountId"));
        if(f.includePii())columns.addAll(List.of("recipientName","recipientPhone","recipientAddress"));List<List<String>> rows=new ArrayList<>();
        for(var captured:c.rows()){
            var s=shipments.get(a,captured.id());var row=new ArrayList<>(List.of(s.code(),VIETNAM.format(s.createdAt()),text(s.input().reference()),text(captured.source().sourceChannel()),text(captured.source().sourceOrderReference()),"ROAD_STANDARD",s.quote().route().originCode(),s.quote().route().destinationCode(),String.valueOf(s.parcels().size()),String.valueOf(s.totalVnd()),"UNPAID",s.creatorAccountId().toString(),text(s.facilityId()),String.join(";",captured.source().importJobIds().stream().map(UUID::toString).toList()),text(captured.assignment().assigneeAccountId())));
            if(f.includePii()){var contact=s.input().recipient();var address=contact.address();row.addAll(List.of(contact.name(),contact.phone(),String.join(", ",address.addressLine(),address.locality(),address.administrativeArea(),address.country())));}rows.add(List.copyOf(row));
        }
        UUID id=UUID.randomUUID();byte[] digest;try{digest=MessageDigest.getInstance("SHA-256").digest(JSON.writeValueAsBytes(new TreeMap<>(parameters)));}catch(java.security.GeneralSecurityException unavailable){throw new IllegalStateException("EXPORT_FILTER_DIGEST_REQUIRED");}
        try{Integer n=jdbc.execute((ConnectionCallback<Integer>)connection->{var array=connection.createArrayOf("uuid",c.rows().stream().map(Captured::id).toArray(UUID[]::new));try(var statement=connection.prepareStatement("INSERT INTO orders.organization_shipment_export_audit VALUES(?,?,?,?,?,?,?,?,?)")){statement.setObject(1,id);statement.setObject(2,org);statement.setObject(3,a.actorAccountId());statement.setObject(4,a.requestId());statement.setTimestamp(5,Timestamp.from(c.at()));statement.setBytes(6,digest);statement.setArray(7,array);statement.setInt(8,rows.size());statement.setBoolean(9,f.includePii());return statement.executeUpdate();}finally{array.free();}});if(n==null||n!=1)throw new IllegalStateException("EXPORT_ACCESS_AUDIT_REQUIRED");}finally{Arrays.fill(digest,(byte)0);}a.requireCurrent();return new Export(id,org,c.at(),f.includePii(),rows.size(),columns,rows);
    }
    private Assignment current(Access a,UUID shipment,boolean lock){UUID org=org(a);var rows=jdbc.query("SELECT assignee_account_id,version,updated_at FROM orders.organization_shipment_assignment WHERE organization_id=? AND shipment_id=?"+(lock?" FOR UPDATE":""),(r,n)->new Assignment(shipment,org,r.getObject(1,UUID.class),r.getLong(2),r.getTimestamp(3).toInstant()),org,shipment);return rows.isEmpty()?new Assignment(shipment,org,null,0,null):rows.getFirst();}
    @Override public Assignment assignment(Access a,UUID shipment){shipments.get(a,shipment);return current(a,shipment,false);}
    private record Prior(UUID shipment,UUID site,long expected,long version,String kid,byte[] hash){}
    private Optional<Prior> prior(Access a,UUID key){return jdbc.query("SELECT shipment_id,facility_id,expected_version,result_version,fingerprint_key_id,fingerprint FROM orders.organization_shipment_assignment_receipt WHERE organization_id=? AND actor_account_id=? AND operation_key=?",(r,n)->new Prior(r.getObject(1,UUID.class),r.getObject(2,UUID.class),r.getLong(3),r.getLong(4),r.getString(5),r.getBytes(6)),org(a),a.actorAccountId(),key).stream().findFirst();}
    private byte[] hash(Access a,String kid,UUID site,UUID shipment,long version,UUID assignee,String reason){return protection.hash(kid,"ORGANIZATION_ASSIGNMENT_V1",org(a),a.actorAccountId(),site,shipment,version,assignee,reason);}
    private void one(int n){if(n!=1)throw new IllegalStateException("ORGANIZATION_ASSIGNMENT_ATOMIC_WRITE_REQUIRED");}
    @Override public Assignment assign(Access a,UUID shipment,UUID assignee,String reason,long expected,UUID key){
        var s=shipments.get(a,shipment);permit(a,"shipment.assign",s.facilityId());if(key==null||expected<0||reason==null||!Set.of("INITIAL_ASSIGNMENT","REASSIGNMENT","UNASSIGNMENT").contains(reason))throw fail("VALIDATION_FAILED");
        var replay=prior(a,key);if(replay.isPresent()){var p=replay.get();byte[] digest=hash(a,p.kid(),s.facilityId(),shipment,expected,assignee,reason);try{if(!p.shipment().equals(shipment)||p.expected()!=expected||!Objects.equals(p.site(),s.facilityId())||!MessageDigest.isEqual(digest,p.hash()))throw fail("IDEMPOTENCY_CONFLICT");}finally{Arrays.fill(digest,(byte)0);Arrays.fill(p.hash(),(byte)0);}return jdbc.queryForObject("SELECT assignee_account_id,occurred_at FROM orders.organization_shipment_assignment_event WHERE shipment_id=? AND new_version=? AND organization_id=?",(r,n)->new Assignment(shipment,org(a),r.getObject(1,UUID.class),p.version(),r.getTimestamp(2).toInstant()),shipment,p.version(),org(a));}
        a.scope().requireActiveFacility(s.facilityId());if(assignee!=null&&(!a.eligibleAccount(assignee)||!a.scope().eligibleAssignee(assignee,s.facilityId())))throw fail("RESOURCE_NOT_FOUND");var before=current(a,shipment,true);
        if(before.version()!=expected||before.version()==Long.MAX_VALUE)throw fail("PRECONDITION_STALE");if(Objects.equals(before.assigneeAccountId(),assignee))throw fail("ASSIGNMENT_NO_CHANGE");
        String correct=assignee==null?"UNASSIGNMENT":before.assigneeAccountId()==null?"INITIAL_ASSIGNMENT":"REASSIGNMENT";if(!correct.equals(reason))throw fail("VALIDATION_FAILED");
        long next=expected+1;Instant at=clock.instant().truncatedTo(ChronoUnit.MICROS);if(before.updatedAt()!=null&&at.isBefore(before.updatedAt()))throw fail("PRECONDITION_STALE");
        if(expected==0)one(jdbc.update("INSERT INTO orders.organization_shipment_assignment VALUES(?,?,?,?,?,?)",shipment,org(a),s.facilityId(),assignee,next,Timestamp.from(at)));
        else one(jdbc.update("UPDATE orders.organization_shipment_assignment SET assignee_account_id=?,version=?,updated_at=? WHERE shipment_id=? AND organization_id=? AND version=?",assignee,next,Timestamp.from(at),shipment,org(a),expected));
        String operation=assignee==null?"UNASSIGNED":before.assigneeAccountId()==null?"ASSIGNED":"REASSIGNED";
        one(jdbc.update("INSERT INTO orders.organization_shipment_assignment_event VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?)",UUID.randomUUID(),org(a),shipment,s.facilityId(),operation,a.actorAccountId(),before.assigneeAccountId(),assignee,expected,next,reason,a.requestId(),key,Timestamp.from(at)));
        byte[] digest=hash(a,protection.active(),s.facilityId(),shipment,expected,assignee,reason);try{one(jdbc.update("INSERT INTO orders.organization_shipment_assignment_receipt VALUES(?,?,?,?,?,?,?,?,?,?)",org(a),a.actorAccountId(),key,shipment,s.facilityId(),expected,next,protection.active(),digest,Timestamp.from(at)));}finally{Arrays.fill(digest,(byte)0);}
        a.requireCurrent();a.meaningfulAction();return new Assignment(shipment,org(a),assignee,next,at);
    }
    @Override public Receipt receipt(Access a,UUID key){var p=prior(a,key).orElseThrow(()->fail("RESOURCE_NOT_FOUND"));try{shipments.get(a,p.shipment());permit(a,"shipment.assign",p.site());return new Receipt(p.shipment(),p.version(),"COMMITTED");}finally{Arrays.fill(p.hash(),(byte)0);}}
    @Override public BusinessShipmentManagement.HistoryPage history(Access a,UUID shipment,int page){shipments.get(a,shipment);if(page<0)throw fail("VALIDATION_FAILED");var rows=jdbc.query("SELECT id,operation,actor_account_id,prior_assignee_account_id,assignee_account_id,prior_version,new_version,occurred_at FROM orders.organization_shipment_assignment_event WHERE organization_id=? AND shipment_id=? ORDER BY new_version DESC LIMIT 21 OFFSET ?",(r,n)->new History(r.getObject(1,UUID.class),r.getString(2),r.getObject(3,UUID.class),r.getObject(4,UUID.class),r.getObject(5,UUID.class),r.getLong(6),r.getLong(7),r.getTimestamp(8).toInstant()),org(a),shipment,(long)page*20);a.requireCurrent();return new BusinessShipmentManagement.HistoryPage(rows.stream().limit(20).toList(),page,rows.size()>20);}
    private Set<UUID> reportSites(Access a,String permission){
        if(a.scope().permits("shipment.draft.read",null)&&a.scope().permits(permission,null))return null;
        Set<UUID> sites=new HashSet<>();sites.addAll(a.scope().facilities("shipment.draft.read"));sites.addAll(a.scope().facilities(permission));
        sites.removeIf(site->!a.scope().permits("shipment.draft.read",site)||!a.scope().permits(permission,site));return sites;
    }
    private void reportScope(Access a,String permission,UUID facility,boolean exporting){
        any(a,permission);if(facility!=null){permit(a,permission,facility);if(exporting)permit(a,"shipment.export",facility);return;}
        var sites=reportSites(a,permission);if(sites!=null&&sites.isEmpty())throw fail("RESOURCE_NOT_FOUND");
        if(exporting){any(a,"shipment.export");if(sites!=null&&sites.stream().noneMatch(site->a.scope().permits("shipment.export",site)))throw fail("RESOURCE_NOT_FOUND");}
    }
    @Override public BusinessReportSources.Cohort capture(Access a,Map<String,String> parameters,String kind,boolean exporting,Consumer<List<FeeBinding>> validate){
        org(a);if(!Set.of("OPERATIONS","FINANCE").contains(kind))throw fail("VALIDATION_FAILED");
        String permission=kind.equals("FINANCE")?"shipment.finance.read":"shipment.reports.read";
        var f=BusinessManagementFilters.parse(reportQuery(parameters),clock.instant(),false);reportScope(a,permission,f.facility(),exporting);
        var result=capture(a,f,false,validate,permission,exporting);var rows=result.rows().stream().map(Captured::report).toList();
        var sites=reportSites(a,permission);boolean financial=kind.equals("FINANCE")||(f.facility()!=null?a.scope().permits("shipment.finance.read",f.facility()):sites==null?a.scope().permits("shipment.finance.read",null):!sites.isEmpty()&&sites.stream().allMatch(site->a.scope().permits("shipment.finance.read",site)));
        financial=financial&&rows.stream().allMatch(row->a.scope().permits("shipment.finance.read",row.facilityId()));
        boolean comparable=kind.equals("OPERATIONS")&&Collections.disjoint(parameters.keySet(),Set.of("origin","destination","service","status","assignee","obligation","handover","search"));
        var unavailable=new BusinessReportSources.Preparation("NOT_COMPARABLE","BOOKING_FILTER_NOT_APPLICABLE",null,null,null,null);
        var noActivity=new BusinessReportSources.Activity("NOT_COMPARABLE","BOOKING_FILTER_NOT_APPLICABLE",null,null,List.of());
        return new BusinessReportSources.Cohort(result.at(),financial,rows,comparable?preparationReport(a,f):unavailable,comparable?activityReport(a,f):noActivity);
    }
    private String preparationWhere(Access a,BusinessManagementFilters f,String time,List<Object> args){
        args.add(org(a));args.add(Timestamp.from(f.until()));StringBuilder where=new StringBuilder("d.organization_id=? AND d."+time+"<?");
        scope(a,"shipment.draft.read","d",where,args);scope(a,"shipment.reports.read","d",where,args);
        if(f.from()!=null){where.append(" AND d."+time+">=?");args.add(Timestamp.from(f.from()));}
        if(f.facility()!=null){where.append(" AND d.facility_id=?");args.add(f.facility());}
        if(f.creator()!=null){where.append(" AND d.creator_account_id=?");args.add(f.creator());}
        if(f.sourceChannel()!=null){where.append(" AND d.source_channel=?");args.add(f.sourceChannel());}
        if(f.batch()!=null){where.append(" AND EXISTS(SELECT 1 FROM orders.organization_import_row i WHERE i.organization_id=d.organization_id AND i.preparation_id=d.id AND i.job_id=? AND i.state='CREATED')");args.add(f.batch());}return where.toString();
    }
    private BusinessReportSources.Preparation preparationReport(Access a,BusinessManagementFilters f){
        var args=new ArrayList<Object>();String where=preparationWhere(a,f,"created_at",args);
        return jdbc.queryForObject("SELECT count(*)::text,count(*) FILTER(WHERE state='DRAFT')::text,count(*) FILTER(WHERE state='BOOKED')::text,count(*) FILTER(WHERE state='ARCHIVED')::text FROM orders.organization_preparation d WHERE "+where,(r,n)->new BusinessReportSources.Preparation("AVAILABLE",null,r.getString(1),r.getString(2),r.getString(3),r.getString(4)),args.toArray());
    }
    private BusinessReportSources.Activity activityReport(Access a,BusinessManagementFilters f){
        var args=new ArrayList<Object>();String where=preparationWhere(a,f,"occurred_at",args);
        String event="(SELECT e.organization_id,e.target_id id,e.actor_account_id,e.operation,e.occurred_at,(v.snapshot->>'facilityId')::uuid facility_id,(v.snapshot->>'creatorAccountId')::uuid creator_account_id,v.snapshot->>'sourceChannel' source_channel FROM orders.organization_preparation_audit e JOIN orders.organization_preparation_version v ON v.id=e.target_id AND v.organization_id=e.organization_id AND v.version=e.new_version WHERE e.operation IN ('CREATE','BOOK')) d";
        var rows=jdbc.query("SELECT actor_account_id,count(*) FILTER(WHERE operation='CREATE')::text,count(*) FILTER(WHERE operation='BOOK')::text FROM "+event+" WHERE "+where+" GROUP BY actor_account_id ORDER BY actor_account_id LIMIT 1001",(r,n)->new BusinessReportSources.Actor(r.getObject(1,UUID.class),r.getString(2),r.getString(3)),args.toArray());
        if(rows.size()>1000)throw fail("REPORT_LIMIT_EXCEEDED");BigInteger created=BigInteger.ZERO,booked=BigInteger.ZERO;for(var row:rows){created=created.add(new BigInteger(row.created()));booked=booked.add(new BigInteger(row.booked()));}
        return new BusinessReportSources.Activity("AVAILABLE",null,created.toString(),booked.toString(),rows);
    }
    private static Map<String,String> reportQuery(Map<String,String> query){var result=new HashMap<>(query);result.remove("interval");result.put("view","management");return result;}
    @Override public UUID auditExport(Access a,String kind,String section,Map<String,String> query,List<UUID> ids,int rowCount,Instant at){
        UUID organization=org(a);if(!Set.of("OPERATIONS","FINANCE").contains(kind)||!Set.of("trend","facility","creator","channel","service","route").contains(section)||ids.size()>10000||new HashSet<>(ids).size()!=ids.size()||rowCount<0||rowCount>1000)throw fail("VALIDATION_FAILED");
        var f=BusinessManagementFilters.parse(reportQuery(query),clock.instant(),false);reportScope(a,kind.equals("FINANCE")?"shipment.finance.read":"shipment.reports.read",f.facility(),true);
        UUID id=UUID.randomUUID();byte[] bytes=JSON.writeValueAsBytes(new TreeMap<>(query)),digest;try{digest=MessageDigest.getInstance("SHA-256").digest(bytes);}catch(java.security.GeneralSecurityException bad){throw new IllegalStateException("REPORT_DIGEST_REQUIRED");}
        try{one(Objects.requireNonNull(jdbc.execute((ConnectionCallback<Integer>)c->{var array=c.createArrayOf("uuid",ids.toArray(UUID[]::new));try(var p=c.prepareStatement("INSERT INTO orders.organization_report_export_audit VALUES(?,?,?,?,?,?,?::jsonb,?,?,?,?)")){p.setObject(1,id);p.setObject(2,organization);p.setObject(3,a.actorAccountId());p.setObject(4,a.requestId());p.setString(5,kind);p.setString(6,section);p.setString(7,new String(bytes,java.nio.charset.StandardCharsets.UTF_8));p.setBytes(8,digest);p.setArray(9,array);p.setInt(10,rowCount);p.setTimestamp(11,Timestamp.from(at));return p.executeUpdate();}finally{array.free();}})));}finally{Arrays.fill(bytes,(byte)0);Arrays.fill(digest,(byte)0);}a.requireCurrent();return id;
    }
    private boolean historicalReportScope(Access a,String kind,List<UUID> ids,Map<String,String> query){
        try{String permission=kind.equals("FINANCE")?"shipment.finance.read":"shipment.reports.read";var f=BusinessManagementFilters.parse(reportQuery(query),clock.instant(),false);reportScope(a,permission,f.facility(),true);
            for(UUID id:ids){var sites=jdbc.query("SELECT facility_id FROM orders.pilot_shipment WHERE id=? AND organization_id=? AND owner_type='BUSINESS'",(r,n)->Optional.ofNullable(r.getObject(1,UUID.class)),id,org(a));if(sites.size()!=1)return false;UUID site=sites.getFirst().orElse(null);permit(a,permission,site);permit(a,"shipment.export",site);}return true;
        }catch(BusinessPreparations.Failure rejected){if("RESOURCE_NOT_FOUND".equals(rejected.getMessage()))return false;throw rejected;}
    }
    @Override public BusinessReportSources.HistoryPage history(Access a,int page){
        UUID organization=org(a);any(a,"shipment.export");if(page<0)throw fail("VALIDATION_FAILED");
        if(!a.scope().permits("shipment.reports.read",null)&&a.scope().facilities("shipment.reports.read").isEmpty()&&!a.scope().permits("shipment.finance.read",null)&&a.scope().facilities("shipment.finance.read").isEmpty())throw fail("RESOURCE_NOT_FOUND");
        record StoredHistory(BusinessReportSources.ExportHistory value,List<UUID> ids){}
        var candidates=jdbc.query("SELECT id,kind,section,canonical_query::text,contributing_shipment_ids,row_count,occurred_at FROM orders.organization_report_export_audit WHERE organization_id=? AND actor_account_id=? ORDER BY occurred_at DESC,id DESC LIMIT 21 OFFSET ?",(r,n)->{
            var query=new HashMap<String,String>();JSON.readTree(r.getString(4)).properties().forEach(e->query.put(e.getKey(),e.getValue().asString()));var array=r.getArray(5);List<UUID> ids;try{ids=Arrays.stream((Object[])array.getArray()).map(v->UUID.fromString(v.toString())).toList();}finally{array.free();}
            return new StoredHistory(new BusinessReportSources.ExportHistory(r.getObject(1,UUID.class),r.getString(2),r.getString(3),query,r.getInt(6),r.getTimestamp(7).toInstant()),ids);
        },organization,a.actorAccountId(),(long)page*20);
        var visible=candidates.stream().limit(20).filter(row->historicalReportScope(a,row.value().kind(),row.ids(),row.value().query())).map(StoredHistory::value).toList();a.requireCurrent();return new BusinessReportSources.HistoryPage(visible,page,candidates.size()>20);
    }

}
