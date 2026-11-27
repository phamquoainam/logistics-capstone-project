package com.elogistic.backend.dispatch.api;
import com.elogistic.backend.identity.api.staff.OperationalStaffAdmission;
import com.elogistic.backend.orders.api.PilotHandling;
import java.time.Instant;
import java.util.*;

/** Dedicated OPS admission into the existing Dispatch intake transaction. */
public interface OperationalStationIntake {
    record OperationalSite(UUID id,String code,String name,String type,String state,long version,List<String> capabilities,Instant effectiveFrom,Instant effectiveTo) {
        public OperationalSite { capabilities=List.copyOf(capabilities); }
    }
    List<OperationalSite> sites(OperationalStaffAdmission.Context context);
    void require(OperationalStaffAdmission.Context context,UUID siteId);
    StationIntake.Scan scan(OperationalStaffAdmission.Context context,UUID siteId,PilotHandling.Cargo cargo,String barcode,UUID key);
    List<StationIntake.Scan> scans(OperationalStaffAdmission.Context context,UUID siteId,UUID shipment);
    StationIntake.Acceptance accept(OperationalStaffAdmission.Context context,UUID siteId,PilotHandling.Cargo cargo,List<UUID> scanIds,long expectedVersion,String inspection,UUID key);
    StationIntake.Receipt receipt(OperationalStaffAdmission.Context context,UUID siteId,UUID key);
    default StationIntake.Scan scan(OperationalStaffAdmission.Context context,UUID siteId,PilotHandling.CarrierCargo cargo,String barcode,UUID key){throw new IllegalStateException("CARRIER_INTAKE_UNAVAILABLE");}
    default StationIntake.Acceptance accept(OperationalStaffAdmission.Context context,UUID siteId,PilotHandling.CarrierCargo cargo,List<UUID> scanIds,long expectedVersion,String inspection,UUID key){throw new IllegalStateException("CARRIER_INTAKE_UNAVAILABLE");}
}
