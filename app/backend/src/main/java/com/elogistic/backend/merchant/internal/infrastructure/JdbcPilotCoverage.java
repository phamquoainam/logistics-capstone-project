package com.elogistic.backend.merchant.internal.infrastructure;
import com.elogistic.backend.identity.api.customer.CustomerAdmission;
import com.elogistic.backend.merchant.api.customer.*;
import java.time.Clock;
import javax.sql.DataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionSynchronizationManager;
public final class JdbcPilotCoverage implements PilotCoverage {
    private final com.elogistic.backend.identity.api.staff.OperationalStaffAdmission operations;private final CustomerAdmission admission;private final DataSource source;private final JdbcTemplate jdbc;private final Clock clock;private final String environment;
    public JdbcPilotCoverage(DataSource source,CustomerAdmission admission,Clock clock){this(source,admission,clock,null);}
    public JdbcPilotCoverage(DataSource source,CustomerAdmission admission,Clock clock,com.elogistic.backend.identity.api.staff.OperationalStaffAdmission operations){this(source,admission,clock,operations,"DISABLED");}
    public JdbcPilotCoverage(DataSource source,CustomerAdmission admission,Clock clock,com.elogistic.backend.identity.api.staff.OperationalStaffAdmission operations,String environment){this.operations=operations;this.source=source;this.admission=admission;this.clock=clock;this.environment=environment;jdbc=new JdbcTemplate(source);}
    private void current(CustomerAdmission.Context c){admission.requireCurrent(c);if(!TransactionSynchronizationManager.isActualTransactionActive()||!TransactionSynchronizationManager.hasResource(source))throw new IllegalStateException("PILOT_COVERAGE_ADMISSION_REQUIRED");}
    @Override public Catalog catalog(CustomerAdmission.Context c){current(c);return catalogRead(()->admission.requireCurrent(c));}
    @Override public Catalog catalog(com.elogistic.backend.identity.api.staff.OperationalStaffAdmission.Context c){if(operations==null)throw new IllegalStateException("OPS_CONTEXT_REQUIRED");operations.requireCurrent(c);if(!TransactionSynchronizationManager.isActualTransactionActive()||!TransactionSynchronizationManager.hasResource(source))throw new IllegalStateException("PILOT_COVERAGE_ADMISSION_REQUIRED");return catalogRead(()->operations.requireCurrent(c));}
    private void currentOps(com.elogistic.backend.identity.api.staff.OperationalStaffAdmission.Context c){
        if(operations==null||!java.util.Set.of("LOCAL","TEST").contains(environment))throw new IllegalStateException("OPS_CONTEXT_REQUIRED");operations.requireCurrent(c);
        if(!TransactionSynchronizationManager.isActualTransactionActive()||!TransactionSynchronizationManager.hasResource(source))throw new IllegalStateException("PILOT_COVERAGE_ADMISSION_REQUIRED");
    }
    @Override public <T>T withIntakeOrganizationLock(com.elogistic.backend.identity.api.staff.OperationalStaffAdmission.Context c,java.util.function.Supplier<T> work){
        currentOps(c);jdbc.execute("SELECT pg_advisory_xact_lock(730023,1)");T result=work.get();currentOps(c);return result;
    }
    @Override public void requireIntakeOrganization(com.elogistic.backend.identity.api.staff.OperationalStaffAdmission.Context c,java.util.UUID org,java.util.UUID facility){
        currentOps(c);if(org==null)throw new PersonalAddressFailure(PersonalAddressFailure.Code.RESOURCE_NOT_FOUND);
        Boolean allowed=jdbc.queryForObject("SELECT count(*)=1 FROM merchant.organization o JOIN merchant.membership m ON m.id=coalesce((SELECT membership_id FROM merchant.organization_current_owner WHERE organization_id=o.id),o.initial_owner_membership_id) AND m.organization_id=o.id WHERE o.id=? AND o.state='ACTIVE' AND o.environment=? AND (o.admission_origin='LOCAL_OPERATOR_PROVISIONED' OR merchant.application_origin_admitted(o.id)) AND m.role='OWNER' AND m.state='ACTIVE' AND (?::uuid IS NULL OR EXISTS(SELECT 1 FROM merchant.facility f WHERE f.id=? AND f.organization_id=o.id AND f.state='ACTIVE'))",Boolean.class,org,environment,facility,facility);
        if(!Boolean.TRUE.equals(allowed))throw new PersonalAddressFailure(PersonalAddressFailure.Code.RESOURCE_NOT_FOUND);currentOps(c);
    }
    private Catalog catalogRead(Runnable current){var datasets=jdbc.query("SELECT version,namespace,provenance,effective_from FROM merchant.pilot_coverage_dataset WHERE version='ARGO-SIM-COVERAGE-1' AND effective_from<=? AND (effective_to IS NULL OR effective_to>?)",(r,n)->new Catalog(r.getString(1),r.getString(2),r.getString(3),r.getTimestamp(4).toInstant(),java.util.List.of(),java.util.List.of()),java.sql.Timestamp.from(clock.instant()),java.sql.Timestamp.from(clock.instant()));if(datasets.size()!=1)throw new IllegalStateException("PILOT_COVERAGE_NOT_EFFECTIVE");var d=datasets.getFirst();
        var areas=jdbc.query("SELECT code,region,label,drop_off_code FROM merchant.pilot_area WHERE dataset_version=? ORDER BY code",(r,n)->new Area(r.getString(1),r.getString(2),r.getString(3),r.getString(4)),d.datasetVersion());var routes=jdbc.query("SELECT r.dataset_version,r.origin_code,r.destination_code,r.classification,a.drop_off_code FROM merchant.pilot_route r JOIN merchant.pilot_area a ON (a.dataset_version,a.code)=(r.dataset_version,r.origin_code) WHERE r.dataset_version=? AND r.service_code='ROAD_STANDARD' ORDER BY r.origin_code,r.destination_code",(r,n)->new Route(r.getString(1),r.getString(2),r.getString(3),r.getString(4),r.getString(5)),d.datasetVersion());current.run();return new Catalog(d.datasetVersion(),d.namespace(),d.provenance(),d.effectiveFrom(),areas,routes);
    }
    @Override public Route eligible(CustomerAdmission.Context c,String origin,String destination){if(origin==null||destination==null)throw new PersonalAddressFailure(PersonalAddressFailure.Code.VALIDATION_FAILED);return catalog(c).routes().stream().filter(r->r.originCode().equals(origin)&&r.destinationCode().equals(destination)).findFirst().orElseThrow(()->new PersonalAddressFailure(PersonalAddressFailure.Code.RESOURCE_NOT_FOUND));}
}
