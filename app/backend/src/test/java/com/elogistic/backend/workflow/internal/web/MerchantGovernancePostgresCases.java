package com.elogistic.backend.workflow.internal.web;

import com.elogistic.backend.identity.internal.infrastructure.AdminOnboardingPostgresFixture;
import com.elogistic.backend.testsupport.PostgresTestcontainersSupport.Database;
import java.sql.*;
import java.time.*;
import java.util.*;
import javax.sql.DataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import static org.junit.jupiter.api.Assertions.*;

/** Owned PG18 only; canonical functions are invoked with a separate test operator principal. */
final class MerchantGovernancePostgresCases {
    private final Database db;private final JdbcTemplate jdbc;private final AdminOnboardingPostgresFixture fixture;private final UUID realm,admin;
    MerchantGovernancePostgresCases(Database db,DataSource source,AdminOnboardingPostgresFixture fixture,UUID realm,UUID admin){this.db=db;jdbc=new JdbcTemplate(source);this.fixture=fixture;this.realm=realm;this.admin=admin;}
    static String policy(Instant from){return "{\"code\":\"SYNTHETIC_MANUAL_SANDBOX\",\"environment\":\"TEST\",\"entityTypes\":[\"COMPANY\",\"HOUSEHOLD_BUSINESS\"],\"jurisdictionCodes\":[\"SYNTHETIC-J1\"],\"consentVersion\":\"SYNTHETIC-CONSENT-1\",\"consentStatements\":{\"accuracy\":\"Synthetic accuracy statement\",\"representation\":\"Synthetic representation statement\"},\"consentText\":\"Synthetic isolated test consent; no commercial or legal activation.\",\"requiredEvidence\":[\"REGISTRATION\",\"REPRESENTATIVE_AUTHORIZATION\"],\"reviewMode\":\"TWO_PERSON\",\"state\":\"PUBLISHED\",\"validFrom\":\""+from+"\",\"validTo\":null}";}
    UUID publish(Connection c,UUID key,UUID policy,String body)throws Exception{try(var q=c.prepareStatement("SELECT merchant.publish_local_application_policy(?,?,1,?::jsonb,'Synthetic operator publication')")){q.setObject(1,key);q.setObject(2,policy);q.setString(3,body);try(var r=q.executeQuery()){assertTrue(r.next());return r.getObject(1,UUID.class);}}}
    UUID scope(Connection c,UUID request,UUID policy,String action,long expected,Instant expiry,String...permissions)throws Exception{try(var q=c.prepareStatement("SELECT merchant.configure_local_reviewer_scope(?,?,?,'POLICY',?,?,?, ?,?,'Synthetic independent reviewer scope')")){q.setObject(1,request);q.setObject(2,admin);q.setObject(3,realm);q.setObject(4,policy);q.setArray(5,c.createArrayOf("text",permissions));q.setString(6,action);q.setLong(7,expected);q.setTimestamp(8,Timestamp.from(expiry));try(var r=q.executeQuery()){assertTrue(r.next());return r.getObject(1,UUID.class);}}}
    void operatorReceiptsAndCeilings()throws Exception{
        UUID id=UUID.randomUUID(),key=UUID.randomUUID();String body=policy(Instant.now().minusSeconds(30).truncatedTo(java.time.temporal.ChronoUnit.MICROS));
        try(var c=fixture.governanceOperatorConnection()){
            var json=new tools.jackson.databind.ObjectMapper();
            for(String invalid:List.of("null","{}","{\"accuracy\":null,\"representation\":\"Synthetic\"}","{\"accuracy\":\"Synthetic\",\"representation\":\"Synthetic\",\"extra\":true}",json.writeValueAsString(Map.of("accuracy"," ","representation","Synthetic")),json.writeValueAsString(Map.of("accuracy","x".repeat(1001),"representation","Synthetic")),json.writeValueAsString(Map.of("accuracy","Synthetic\ncontrol","representation","Synthetic")),json.writeValueAsString(Map.of("accuracy","e\u0301","representation","Synthetic")))){
                var input=json.readTree(body).deepCopy();((tools.jackson.databind.node.ObjectNode)input).set("consentStatements",json.readTree(invalid));
                assertThrows(SQLException.class,()->publish(c,UUID.randomUUID(),UUID.randomUUID(),json.writeValueAsString(input)));
                assertEquals(0,jdbc.queryForObject("SELECT count(*) FROM merchant.application_policy",Integer.class));assertEquals(0,jdbc.queryForObject("SELECT count(*) FROM merchant.governance_receipt",Integer.class));
            }
            assertEquals(id,publish(c,key,id,body));try(var statement=c.createStatement()){statement.execute("SET TIME ZONE 'Pacific/Auckland'");}
            assertEquals(id,publish(c,key,id,body));
            assertThrows(SQLException.class,()->publish(c,key,id,body.replace("Synthetic accuracy statement","Changed synthetic wording")));
            assertThrows(SQLException.class,()->publish(c,key,UUID.randomUUID(),body));
            assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM merchant.application_policy",Integer.class));
            assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM merchant.governance_receipt WHERE actor_kind='OPERATOR' AND input_key_id='SQL_SHA256_1'",Integer.class));
            assertThrows(SQLException.class,()->publish(c,UUID.randomUUID(),UUID.randomUUID(),body));
            UUID request=UUID.randomUUID();Instant expires=Instant.now().plusSeconds(3600).truncatedTo(java.time.temporal.ChronoUnit.MICROS);
            var assignment=scope(c,request,id,"ISSUE",0,expires,"application.read","application.review");
            assertEquals(assignment,scope(c,request,id,"ISSUE",0,expires,"application.review","application.read"));
            assertThrows(SQLException.class,()->scope(c,request,id,"ISSUE",0,expires,"application.decide"));
            assertThrows(SQLException.class,()->scope(c,UUID.randomUUID(),id,"ISSUE",0,expires,"application.read","application.read"));
            assertThrows(SQLException.class,()->scope(c,UUID.randomUUID(),id,"ISSUE",0,expires,"IDENTITY_MANAGEMENT"));
            assertEquals(assignment,scope(c,UUID.randomUUID(),id,"REVOKE",1,expires,"application.read","application.review"));
            assertEquals(assignment,scope(c,request,id,"ISSUE",0,expires,"application.read","application.review"));
            assertEquals("REVOKED",jdbc.queryForObject("SELECT state FROM merchant.business_reviewer_scope WHERE id=?",String.class,assignment));
            assertEquals(3,jdbc.queryForObject("SELECT count(*) FROM merchant.governance_event",Integer.class));
            assertEquals(3,jdbc.queryForObject("SELECT count(*) FROM merchant.governance_receipt",Integer.class));
        }
        assertThrows(org.springframework.dao.DataAccessException.class,()->jdbc.update("UPDATE merchant.business_reviewer_scope SET state='ACTIVE'"));
        assertThrows(org.springframework.dao.DataAccessException.class,()->jdbc.update("UPDATE merchant.application_policy SET consent_version='tampered'"));
        assertThrows(org.springframework.dao.DataAccessException.class,()->jdbc.execute("DELETE FROM merchant.governance_receipt"));
        assertThrows(org.springframework.dao.DataAccessException.class,()->jdbc.execute("TRUNCATE merchant.governance_event CASCADE"));
    }
}
