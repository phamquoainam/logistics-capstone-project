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
import org.apache.pdfbox.text.TextPosition;
import com.elogistic.backend.orders.internal.domain.ShippingPreparation;
import java.text.Normalizer;
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
        Path root=Path.of("target","pilot-label-visual");Files.createDirectories(root);Path run=Files.createTempDirectory(root,"run-");
        for(var example:examples()) {
            var source=example.source();byte[] bytes=new PilotLabelRenderer().render(source);
            try(var pdf=Loader.loadPDF(bytes)){
                if(example.name().equals("standard"))assertEquals(2,pdf.getNumberOfPages());
                String text=new PDFTextStripper().getText(pdf);assertTrue(text.contains("LOCAL/TEST SIMULATION"));assertTrue(text.contains("UNPAID"));assertTrue(text.contains(source.code()));assertFalse(text.contains(source.accountId().toString()));
                var renderer=new PDFRenderer(pdf);Path folder=run.resolve(example.name());Files.createDirectories(folder);Files.write(folder.resolve("labels.pdf"),bytes);
                Map<String,StringBuilder> bodies=new LinkedHashMap<>();String last=null;Set<String> completed=new HashSet<>();
                for(int page=0;page<pdf.getNumberOfPages();page++){
                    assertEquals(100*72/25.4,pdf.getPage(page).getMediaBox().getWidth(),0.1);assertEquals(150*72/25.4,pdf.getPage(page).getMediaBox().getHeight(),0.1);
                    var image=renderer.renderImageWithDPI(page,300);ImageIO.write(image,"png",folder.resolve("page-"+(page+1)+".png").toFile());
                    var bitmap=new BinaryBitmap(new HybridBinarizer(new BufferedImageLuminanceSource(image)));String code=new MultiFormatReader().decode(bitmap,Map.of(DecodeHintType.POSSIBLE_FORMATS,List.of(BarcodeFormat.CODE_128),DecodeHintType.TRY_HARDER,true)).getText();
                    assertTrue(source.parcels().stream().anyMatch(p->PilotLabelRenderer.barcode(p.id()).equals(code)),example.name());
                    if(last!=null&&!last.equals(code)){completed.add(last);assertFalse(completed.contains(code),"Parcel continuation pages must remain contiguous");}last=code;
                    var inspection=new LayoutInspection();inspection.setStartPage(page+1);inspection.setEndPage(page+1);inspection.getText(pdf);inspection.verify(example.name()+" page "+(page+1));
                    bodies.computeIfAbsent(code,k->new StringBuilder()).append(inspection.body);
                    var pageText=new PDFTextStripper();pageText.setStartPage(page+1);pageText.setEndPage(page+1);String current=pageText.getText(pdf);assertTrue(current.contains(code));assertTrue(current.contains("UNPAID"));assertTrue(current.contains("Khối lượng: 500 g"));
                    if(page>0&&PilotLabelRenderer.barcode(source.parcels().getFirst().id()).equals(code))assertTrue(current.contains("Tiếp nối"));
                }
                assertEquals(source.parcels().size(),bodies.size());String expected=compact(contactText(source.input().recipient())+contactText(source.input().sender()));
                bodies.forEach((code,body)->assertEquals(expected,compact(body.toString()),example.name()+" full contact snapshot "+code));
            }
        }
        Files.writeString(root.resolve("latest.txt"),run.getFileName().toString());
    }
    private record Example(String name,PilotShipments.Shipment source){}
    private static String limit(String token,int length){return (token+" ").repeat(length).substring(0,length).stripTrailing();}
    private static ShippingDrafts.Contact contact(String name,String address,String locality,String region){return new ShippingDrafts.Contact(name,"+84987654321",new ShippingDrafts.Address("VN",region,locality,address));}
    private static PilotShipments.Shipment withContacts(ShippingDrafts.Contact sender,ShippingDrafts.Contact recipient){var s=shipment();var i=ShippingPreparation.normalize(new ShippingDrafts.Input(s.input().reference(),null,sender,recipient,s.input().parcels()));return new PilotShipments.Shipment(s.id(),s.accountId(),s.code(),s.draftId(),s.draftVersion(),s.bookingState(),s.carrierState(),i,s.quote(),s.totalVnd(),s.createdAt(),s.parcels());}
    private static List<Example> examples(){
        var standard=shipment();var shortContact=standard.input().sender();
        var longSender=contact(limit("Nguyễn Hoàng Người Gửi Mô Phỏng",160),limit("Địa chỉ gửi thử nghiệm nhiều từ",300),"Khu gửi mô phỏng","Vùng gửi mô phỏng");
        var longRecipient=contact(limit("Trần Ánh Người Nhận Mô Phỏng",160),limit("Địa chỉ nhận thử nghiệm nhiều từ",400),"Khu nhận mô phỏng","Vùng nhận mô phỏng");
        var maximum=contact("M".repeat(160),"W".repeat(500),"L".repeat(160),"R".repeat(160));
        assertEquals(500,maximum.address().addressLine().length());assertEquals(160,maximum.name().length());
        var unicode=contact("Nguyễn Thị Ánh Tuyết - Đặng Hữu Phước",limit("Đường Nguyễn Huệ, phường Thử Nghiệm",500),"Khu vực Đắk Lắk mô phỏng","Vùng Quảng Ngãi mô phỏng");
        var boundary=contact("Synthetic Sender Long Contact Name For Label Boundary Review Case Example","Synthetic address block one block two block three block four block five block six block seven block eight block nine block ten block eleven","Synthetic locality","Synthetic region");
        return List.of(new Example("standard",standard),new Example("long-sender",withContacts(longSender,shortContact)),new Example("long-recipient",withContacts(shortContact,longRecipient)),new Example("maximum-unbroken",withContacts(maximum,maximum)),new Example("vietnamese",withContacts(unicode,unicode)),new Example("boundary-reproduction",withContacts(boundary,shortContact)));
    }
    private static String contactText(ShippingDrafts.Contact c){return c.name()+" | "+c.phone()+c.address().addressLine()+", "+c.address().locality()+", "+c.address().administrativeArea();}
    private static String compact(String s){return Normalizer.normalize(s,Normalizer.Form.NFC).replaceAll("\\s+","");}
    private record Ink(float x,float top,float right,float bottom,float baseline){}
    private static final class LayoutInspection extends PDFTextStripper {
        final StringBuilder body=new StringBuilder();final List<Ink> ink=new ArrayList<>();
        LayoutInspection()throws java.io.IOException{}
        @Override protected void processTextPosition(TextPosition p){if(!p.getUnicode().isBlank()&&p.getWidthDirAdj()>0)ink.add(new Ink(p.getXDirAdj(),p.getYDirAdj()-p.getHeightDir(),p.getXDirAdj()+p.getWidthDirAdj(),p.getYDirAdj()+1.5f,p.getYDirAdj()));super.processTextPosition(p);}
        @Override protected void writeString(String s,List<TextPosition> positions){float y=positions.getFirst().getYDirAdj();if(y>=220&&y<=150*72/25.4-70&&!s.startsWith("NGƯỜI NHẬN")&&!s.startsWith("NGƯỜI GỬI"))body.append(s);}
        void verify(String label){float width=100*72/25.4f,height=150*72/25.4f;
            for(var a:ink){assertTrue(a.x()>=15.8&&a.right()<=width-15.8&&a.top()>=8&&a.bottom()<=height-12,label+" printable bounds");assertFalse(a.top()<188&&a.bottom()>145,label+" reserved barcode area");}
            for(int i=0;i<ink.size();i++)for(int j=i+1;j<ink.size();j++){var a=ink.get(i);var b=ink.get(j);assertFalse(a.x()<b.right()-0.1&&b.x()<a.right()-0.1&&a.top()<b.bottom()&&b.top()<a.bottom(),label+" overlapping text");}
        }
    }
    @Test void labelBarcodeParserRejectsNonCanonicalAndCredentialOrPathPayloads(){
        UUID id=UUID.fromString("12345678-abcd-4000-8000-0123456789ab");assertEquals(id,PilotLabelRenderer.parcelId(PilotLabelRenderer.barcode(id)));
        for(String invalid:List.of("../../private","Bearer secret","AP-P-"+id,"AP-P-"+id.toString().replace("-","").toUpperCase(Locale.ROOT),"AP-P-"+"0".repeat(33)))assertThrows(PilotHandlingFailure.class,()->PilotLabelRenderer.parcelId(invalid));
    }
}
