package com.elogistic.backend.dispatch.api;
import com.elogistic.backend.orders.api.PilotHandling;
import java.time.Instant;
import java.util.*;
/** Dispatch owns Station assignments, scan facts, intake/custody and operational receipts. */
public interface StationIntake {
    interface Access {UUID actorId();UUID requestId();void requireCurrent();void meaningfulAction();}
    record Station(String code,String datasetVersion,long assignmentVersion,String permission,boolean acceptanceEnabled) {}
    record Scan(UUID id,UUID shipmentId,UUID parcelId,String barcode,String stationCode,Instant scannedAt) {}
    record Acceptance(UUID id,UUID shipmentId,String stationCode,Instant occurredAt,boolean newlyCommitted) {}
    record Receipt(String operation,UUID targetId,UUID shipmentId,String outcome) {}
    List<Station> stations(Access access);
    void require(Access access,String station);
    Scan scan(Access access,String station,PilotHandling.Cargo cargo,String barcode,UUID key);
    List<Scan> scans(Access access,String station,UUID shipment);
    Acceptance accept(Access access,String station,PilotHandling.Cargo cargo,List<UUID> scanIds,long expectedVersion,String inspection,UUID key);
    Receipt receipt(Access access,String station,UUID key);
}
