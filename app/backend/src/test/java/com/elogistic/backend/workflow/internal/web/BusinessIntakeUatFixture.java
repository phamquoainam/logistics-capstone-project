package com.elogistic.backend.workflow.internal.web;

import com.elogistic.backend.identity.internal.infrastructure.*;
import com.elogistic.backend.testsupport.PostgresTestcontainersSupport.Database;
import java.sql.*;
import java.time.Instant;
import java.util.*;
import javax.sql.DataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import tools.jackson.databind.json.JsonMapper;

/** Owned HTTPS fixture control; canonical offline setup only, never business fact DML. */
final class BusinessIntakeUatFixture {
    private final Database db;private final DataSource operator;private final InvitationProtection protection;
    private UUID actor,site;
    BusinessIntakeUatFixture(Database db,DataSource operator,Map<String,Object> settings)throws Exception{
        this.db=db;this.operator=operator;var json=JsonMapper.builder().build();
        byte[] v=Base64.getDecoder().decode(json.readTree((String)settings.get("customer.security.proof-verifier-keys")).get("v1").asString());
        byte[] e=Base64.getDecoder().decode(json.readTree((String)settings.get("customer.security.delivery-encryption-keys")).get("e1").asString());
        try{protection=new InvitationProtection(Map.of("v1",v),"v1",Map.of("e1",e),"e1");}finally{Arrays.fill(v,(byte)0);Arrays.fill(e,(byte)0);}
        db.ownerExecute("SELECT identity.bind_local_staff_environment('"+UUID.randomUUID()+"','TEST','BIA owned HTTPS binding')");
        site=site("SIM-DROP-A1","STATION");UUID hub=site("SIM-HUB-A","HUB");
        Map<String,Object> link=new HashMap<>(Map.of("fromSiteId",site,"toSiteId",hub,"serviceCode","ROAD_STANDARD","datasetVersion","ARGO-SIM-COVERAGE-1","areaCode","SIM-A1","dropOffCode","SIM-DROP-A1"));link.put("effectiveFrom",null);link.put("effectiveTo",null);
        try(var c=operator.getConnection();var q=c.prepareStatement("SELECT * FROM dispatch.configure_local_site_link(?,NULL,0,?::jsonb,?)")){q.setObject(1,UUID.randomUUID());q.setString(2,json.writeValueAsString(link));q.setString(3,"BIA owned HTTPS topology");q.execute();}
    }
    private UUID site(String code,String type)throws Exception{
        var input=new HashMap<String,Object>();input.put("code",code);input.put("type",type);input.put("name","Synthetic "+type);input.put("locationText","Synthetic isolated location");input.put("areaCode","SIM-A1");input.put("latitude",null);input.put("longitude",null);input.put("timezone","Asia/Ho_Chi_Minh");input.put("operatingHours",Map.of());input.put("state","ACTIVE");input.put("capabilities",type.equals("STATION")?List.of("SITE_READ","STATION_INTAKE"):List.of("SITE_READ"));input.put("effectiveFrom",null);input.put("effectiveTo",null);input.put("environment","TEST");input.put("provenance","SIMULATION");
        try(var c=operator.getConnection();var q=c.prepareStatement("SELECT * FROM dispatch.configure_local_carrier_site(?,NULL,0,?::jsonb,?)")){q.setObject(1,UUID.randomUUID());q.setString(2,JsonMapper.builder().build().writeValueAsString(input));q.setString(3,"BIA owned HTTPS site");try(var r=q.executeQuery()){if(!r.next())throw new IllegalStateException("BIA_SITE_REQUIRED");return r.getObject(1,UUID.class);}}
    }
    synchronized Map<String,Object> shippingSetup(UUID application)throws Exception{
        UUID org,owner;
        try(var c=db.runtimeConnection();var q=c.prepareStatement("SELECT x.organization_id,x.applicant_id FROM merchant.application_activation x JOIN merchant.application a ON a.id=x.application_id AND a.state='APPROVED' JOIN merchant.organization o ON o.id=x.organization_id AND o.state='ACTIVE' AND o.admission_origin='APPLICATION_APPROVED' WHERE x.application_id=?")){q.setObject(1,application);try(var r=q.executeQuery()){if(!r.next())throw new IllegalStateException("BIA_REAL_ACTIVATION_REQUIRED");org=r.getObject(1,UUID.class);owner=r.getObject(2,UUID.class);if(r.next())throw new IllegalStateException("BIA_EXACT_ORG_REQUIRED");}}
        // Explicit trusted operator procedure under existing canonical owner; no runtime grants.
        try(var c=db.migrationConnection();var s=c.createStatement()){
            c.setAutoCommit(false);s.execute("SET LOCAL ROLE "+db.owner());s.execute("SET LOCAL argopost.operator_environment='TEST'");
            try(var q=c.prepareStatement("SELECT merchant.configure_local_workspace(?, 'GRANT', ?, 'Synthetic Browser Merchant', ?, 'OWNER', ?::text[], 'BIA explicit isolated shipping setup')")){q.setObject(1,UUID.randomUUID());q.setObject(2,org);q.setObject(3,owner);q.setArray(4,c.createArrayOf("text",new String[]{"dashboard.read","shipment.draft.read","shipment.draft.write","shipment.quote","shipment.book","shipment.label.read"}));q.execute();}
            try(var q=c.prepareStatement("SELECT pricing.configure_local_business_contract(?,?,'ACTIVATE',0,'BIA explicit isolated tariff approval')")){q.setObject(1,UUID.randomUUID());q.setObject(2,org);q.execute();}c.commit();
        }
        actor=LocalSupportStaffProvisioner.operations(operator,protection).provision(new LocalSupportStaffProvisioner.OfflineStaffProvision(UUID.randomUUID(),"Synthetic BIA Operations","bia-operations@example.invalid","BIA genuine isolated invitation")).accountId();
        return Map.of("organizationId",org,"siteId",site);
    }
    synchronized void assignVerifiedOperations()throws Exception{
        try(var c=db.runtimeConnection();var q=c.prepareStatement("SELECT identity.operations_staff_eligible(?)")){q.setObject(1,actor);try(var r=q.executeQuery()){if(!r.next()||!r.getBoolean(1))throw new IllegalStateException("BIA_VERIFICATION_REQUIRED");}}
        for(String permission:List.of("SITE_READ","STATION_INTAKE"))try(var c=operator.getConnection();var q=c.prepareStatement("SELECT * FROM dispatch.configure_local_operational_assignment(?,?,?,?,?,?,?,?,?)")){Instant now=Instant.now();q.setObject(1,UUID.randomUUID());q.setObject(2,actor);q.setObject(3,site);q.setString(4,permission);q.setString(5,"GRANT");q.setTimestamp(6,Timestamp.from(now));q.setTimestamp(7,Timestamp.from(now.plusSeconds(3600)));q.setLong(8,0);q.setString(9,"BIA explicit isolated site assignment");q.execute();}
    }
}
