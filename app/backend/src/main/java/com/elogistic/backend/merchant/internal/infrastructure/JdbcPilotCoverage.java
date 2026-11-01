package com.elogistic.backend.merchant.internal.infrastructure;
import com.elogistic.backend.identity.api.customer.CustomerAdmission;
import com.elogistic.backend.merchant.api.customer.*;
import java.time.Clock;
import javax.sql.DataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionSynchronizationManager;
public final class JdbcPilotCoverage implements PilotCoverage {
    private final CustomerAdmission admission;private final DataSource source;private final JdbcTemplate jdbc;private final Clock clock;
    public JdbcPilotCoverage(DataSource source,CustomerAdmission admission,Clock clock){this.source=source;this.admission=admission;this.clock=clock;jdbc=new JdbcTemplate(source);}
    private void current(CustomerAdmission.Context c){admission.requireCurrent(c);if(!TransactionSynchronizationManager.isActualTransactionActive()||!TransactionSynchronizationManager.hasResource(source))throw new IllegalStateException("PILOT_COVERAGE_ADMISSION_REQUIRED");}
    @Override public Catalog catalog(CustomerAdmission.Context c){current(c);var datasets=jdbc.query("SELECT version,namespace,provenance,effective_from FROM merchant.pilot_coverage_dataset WHERE version='ARGO-SIM-COVERAGE-1' AND effective_from<=? AND (effective_to IS NULL OR effective_to>?)",(r,n)->new Catalog(r.getString(1),r.getString(2),r.getString(3),r.getTimestamp(4).toInstant(),java.util.List.of(),java.util.List.of()),java.sql.Timestamp.from(clock.instant()),java.sql.Timestamp.from(clock.instant()));if(datasets.size()!=1)throw new IllegalStateException("PILOT_COVERAGE_NOT_EFFECTIVE");var d=datasets.getFirst();
        var areas=jdbc.query("SELECT code,region,label,drop_off_code FROM merchant.pilot_area WHERE dataset_version=? ORDER BY code",(r,n)->new Area(r.getString(1),r.getString(2),r.getString(3),r.getString(4)),d.datasetVersion());var routes=jdbc.query("SELECT r.dataset_version,r.origin_code,r.destination_code,r.classification,a.drop_off_code FROM merchant.pilot_route r JOIN merchant.pilot_area a ON (a.dataset_version,a.code)=(r.dataset_version,r.origin_code) WHERE r.dataset_version=? AND r.service_code='ROAD_STANDARD' ORDER BY r.origin_code,r.destination_code",(r,n)->new Route(r.getString(1),r.getString(2),r.getString(3),r.getString(4),r.getString(5)),d.datasetVersion());admission.requireCurrent(c);return new Catalog(d.datasetVersion(),d.namespace(),d.provenance(),d.effectiveFrom(),areas,routes);
    }
    @Override public Route eligible(CustomerAdmission.Context c,String origin,String destination){if(origin==null||destination==null)throw new PersonalAddressFailure(PersonalAddressFailure.Code.VALIDATION_FAILED);return catalog(c).routes().stream().filter(r->r.originCode().equals(origin)&&r.destinationCode().equals(destination)).findFirst().orElseThrow(()->new PersonalAddressFailure(PersonalAddressFailure.Code.RESOURCE_NOT_FOUND));}
}
