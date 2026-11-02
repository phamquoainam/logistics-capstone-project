package com.elogistic.backend.orders.internal.infrastructure;
import com.elogistic.backend.orders.api.*;
import com.elogistic.backend.pricing.api.PilotQuotes;
import com.google.zxing.*;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import java.time.Instant;
import java.util.*;
import java.nio.file.*;
import javax.imageio.ImageIO;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PilotLabelTests {
    private static PilotShipments.Shipment shipment(){
        UUID id=UUID.fromString("00000000-0000-4000-8000-000000000001"),owner=UUID.randomUUID(),draft=UUID.randomUUID();
        var recipient=new ShippingDrafts.Contact("Nguyễn Người Nhận Mô Phỏng","+84901234567",new ShippingDrafts.Address("VN","Vùng mô phỏng","Khu thử nghiệm","Địa chỉ minh họa không có thật"));
        var facts=new ShippingDrafts.Parcel("Hàng mô phỏng",1,500,100,100,100,0);
        var input=new ShippingDrafts.Input("LABEL-TEST",null,recipient,recipient,List.of(facts,facts));Instant time=Instant.parse("2026-11-02T00:00:00Z");
        var q=new PilotQuotes.Quote(UUID.randomUUID(),owner,draft,1,"ARGO-ROAD-TARIFF-1",new PilotQuotes.Route("ARGO-SIM-COVERAGE-1","SIM-A1","SIM-A2","INTRA","SIM-DROP-A1"),List.of(new PilotQuotes.Parcel(500,100,100,100),new PilotQuotes.Parcel(500,100,100,100)),List.of(new PilotQuotes.Line(1,500,500,25000),new PilotQuotes.Line(2,500,500,25000)),50000,"SIMULATION_NOT_TAX_INVOICE",time,time.plusSeconds(900),id);
        return new PilotShipments.Shipment(id,owner,"AP-SIM-"+id.toString().replace("-",""),draft,1,"CONFIRMED","NOT_ACCEPTED",input,q,50000,time,List.of(new PilotShipments.Parcel(UUID.randomUUID(),1,facts),new PilotShipments.Parcel(UUID.randomUUID(),2,facts)));
    }
    @Test void bookedMultiParcelLabelsRenderVietnameseAndDecodeStableUniqueCode128()throws Exception{
        var source=shipment();byte[] bytes=new PilotLabelRenderer().render(source);
        try(var pdf=Loader.loadPDF(bytes)){
            assertEquals(2,pdf.getNumberOfPages());String text=new PDFTextStripper().getText(pdf);assertTrue(text.contains("LOCAL/TEST SIMULATION"));assertTrue(text.contains("UNPAID"));assertTrue(text.contains("Nguyễn"));assertTrue(text.contains(source.code()));assertFalse(text.contains(source.accountId().toString()));
            var renderer=new PDFRenderer(pdf);Set<String> decoded=new HashSet<>();Path folder=Path.of("target","pilot-label-visual");Files.createDirectories(folder);Files.write(folder.resolve("synthetic-labels.pdf"),bytes);
            for(int page=0;page<2;page++){
                assertEquals(100*72/25.4,pdf.getPage(page).getMediaBox().getWidth(),0.1);assertEquals(150*72/25.4,pdf.getPage(page).getMediaBox().getHeight(),0.1);
                var image=renderer.renderImageWithDPI(page,300);ImageIO.write(image,"png",folder.resolve("synthetic-label-"+page+".png").toFile());
                var bitmap=new BinaryBitmap(new HybridBinarizer(new BufferedImageLuminanceSource(image)));var result=new MultiFormatReader().decode(bitmap,Map.of(DecodeHintType.POSSIBLE_FORMATS,List.of(BarcodeFormat.CODE_128),DecodeHintType.TRY_HARDER,true));assertEquals(PilotLabelRenderer.barcode(source.parcels().get(page).id()),result.getText());assertTrue(decoded.add(result.getText()));
            }
        }
    }
    @Test void labelBarcodeParserRejectsNonCanonicalAndCredentialOrPathPayloads(){
        UUID id=UUID.fromString("12345678-abcd-4000-8000-0123456789ab");assertEquals(id,PilotLabelRenderer.parcelId(PilotLabelRenderer.barcode(id)));
        for(String invalid:List.of("../../private","Bearer secret","AP-P-"+id,"AP-P-"+id.toString().replace("-","").toUpperCase(Locale.ROOT),"AP-P-"+"0".repeat(33)))assertThrows(PilotHandlingFailure.class,()->PilotLabelRenderer.parcelId(invalid));
    }
}
