package com.elogistic.backend.orders.internal.infrastructure;

import com.elogistic.backend.orders.api.*;
import com.elogistic.backend.orders.api.MerchantFacilityWork.*;
import java.time.Instant;
import java.util.*;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MerchantHandoverRendererTests {
    Batch fixture(int shipments){var rows=new ArrayList<BatchShipment>();for(int i=0;i<shipments;i++){UUID s=UUID.randomUUID();var parcels=new ArrayList<PilotShipments.Parcel>();for(int j=1;j<=5;j++)parcels.add(new PilotShipments.Parcel(UUID.randomUUID(),j,new ShippingDrafts.Parcel("Synthetic goods",1,500,100,100,100,0)));rows.add(new BatchShipment(s,"AP-SIM-"+s.toString().replace("-",""),parcels));}return new Batch(UUID.randomUUID(),UUID.randomUUID(),UUID.randomUUID(),"DRAFT",1,1,Instant.EPOCH,Instant.EPOCH,rows,List.of(),List.of(),List.of());}
    @Test void printablePreparationListPreservesEveryActualIdentifierAcrossBoundedPages()throws Exception{var b=fixture(20);try(var pdf=Loader.loadPDF(new MerchantHandoverRenderer().render(b))){assertEquals(7,pdf.getNumberOfPages());String text=new PDFTextStripper().getText(pdf);for(var s:b.shipments()){assertTrue(text.contains(s.shipmentCode()));for(var p:s.parcels())assertTrue(text.contains(PilotLabelRenderer.barcode(p.id())));}for(var page:pdf.getPages())assertEquals(595.27563f,page.getMediaBox().getWidth(),.01f);}}
    @Test void everyPageRetainsSimulationAndNotCarrierAcceptanceDesignation()throws Exception{var b=fixture(8);try(var pdf=Loader.loadPDF(new MerchantHandoverRenderer().render(b))){var text=new PDFTextStripper();for(int i=1;i<=pdf.getNumberOfPages();i++){text.setStartPage(i);text.setEndPage(i);String page=text.getText(pdf);assertTrue(page.contains("LOCAL/TEST SIMULATION"));assertTrue(page.contains("NOT_CARRIER_ACCEPTANCE"));assertTrue(page.contains("Không phải biên nhận Carrier"));assertFalse(page.contains("Synthetic goods"));}}}
}
