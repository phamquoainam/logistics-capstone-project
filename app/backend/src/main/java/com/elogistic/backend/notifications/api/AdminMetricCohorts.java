package com.elogistic.backend.notifications.api;

import com.elogistic.backend.identity.api.admin.AdminFailure;
import com.elogistic.backend.orders.api.AdminShipmentFacts;
import java.util.*;

/** One finite contributing-row definition shared by aggregate drill-through and masked oversight. */
public final class AdminMetricCohorts {
    private AdminMetricCohorts(){}
    public static List<AdminShipmentFacts.Row> select(List<AdminShipmentFacts.Row> rows,String metric){
        if(!Set.of("BOOKINGS","WAITING_HANDOVER","ACCEPTED","PARCELS","CONFIRMED_FEES","UNPAID_FEES").contains(Objects.toString(metric,"")))throw new AdminFailure("VALIDATION_FAILED");
        return rows.stream().filter(r->!metric.equals("WAITING_HANDOVER")&&!metric.equals("ACCEPTED")||r.status().equals(metric.equals("ACCEPTED")?"AT_STATION":"BOOKED"))
            .sorted(Comparator.comparing(AdminShipmentFacts.Row::bookedAt).thenComparing(AdminShipmentFacts.Row::id).reversed()).toList();
    }
}
