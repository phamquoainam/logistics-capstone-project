package com.elogistic.backend.orders.internal.infrastructure;

import com.elogistic.backend.orders.api.*;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.oned.Code128Writer;
import java.io.*;
import java.awt.Color;
import java.util.*;
import java.text.BreakIterator;
import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

/** Private in-memory100x150mm PDF; no HTML, network, file paths or mutable Address Book. */
public final class PilotLabelRenderer {
    private static final float WIDTH=100*72/25.4f,HEIGHT=150*72/25.4f,MARGIN=16;
    private static final float CONTACT_SIZE=9,LEADING=12,CONTACT_TOP=HEIGHT-232,CONTACT_BOTTOM=73;
    private record Row(String section,String value,boolean heading){}
    public static String barcode(UUID parcel){return "AP-P-"+Objects.requireNonNull(parcel).toString().replace("-","");}
    public static UUID parcelId(String code){
        if(code==null||!code.matches("AP-P-[a-f0-9]{32}"))throw new PilotHandlingFailure(PilotHandlingFailure.Code.VALIDATION_FAILED);
        String s=code.substring(5);return UUID.fromString(s.substring(0,8)+"-"+s.substring(8,12)+"-"+s.substring(12,16)+"-"+s.substring(16,20)+"-"+s.substring(20));
    }
    public byte[] render(PilotShipments.Shipment shipment){
        if(shipment.parcels().isEmpty()||shipment.parcels().size()>5||!"CONFIRMED".equals(shipment.bookingState()))throw new IllegalArgumentException("BOOKED_PARCELS_REQUIRED");
        try(var document=new PDDocument();var fontData=resource("NotoSans-Regular.ttf");var logoData=resource("argopost-logo-primary.png");var output=new ByteArrayOutputStream()){
            var font=PDType0Font.load(document,fontData,true);var logo=PDImageXObject.createFromByteArray(document,logoData.readAllBytes(),"ArgoPost");
            var pages=contactPages(font,shipment.input());
            for(var parcel:shipment.parcels()){
              for(int index=0;index<pages.size();index++){
                var page=new PDPage(new PDRectangle(WIDTH,HEIGHT));document.addPage(page);
                try(var stream=new PDPageContentStream(document,page)){
                    stream.setNonStrokingColor(new Color(255,111,0));stream.addRect(0,HEIGHT-28,WIDTH,28);stream.fill();
                    text(stream,font,"ARGOPOST - LOCAL/TEST SIMULATION",8,MARGIN,HEIGHT-22);
                    float logoWidth=126,logoHeight=Math.min(38,logoWidth*logo.getHeight()/logo.getWidth());stream.drawImage(logo,MARGIN,HEIGHT-72,logoWidth,logoHeight);
                    text(stream,font,"ROAD_STANDARD",8,169,HEIGHT-45);text(stream,font,"Kiện "+parcel.parcelNumber()+"/"+shipment.parcels().size()+" | "+(index+1)+"/"+pages.size(),8,169,HEIGHT-64);
                    text(stream,font,shipment.code(),7,MARGIN,HEIGHT-91);
                    text(stream,font,shipment.quote().route().originCode()+"  >  "+shipment.quote().route().destinationCode(),13,MARGIN,HEIGHT-112);
                    text(stream,font,"Drop-off: "+shipment.quote().route().dropOffCode(),8,MARGIN,HEIGHT-128);
                    drawBarcode(stream,barcode(parcel.id()),MARGIN,HEIGHT-188,WIDTH-2*MARGIN,43);
                    text(stream,font,barcode(parcel.id()),7,MARGIN,HEIGHT-201);
                    String note=index==0?(pages.size()==1?"Nhãn chính - đầy đủ thông tin":"Nhãn chính - kèm "+(pages.size()-1)+" trang tiếp nối"):
                        "Tiếp nối nhãn kiện "+parcel.parcelNumber()+" - trang "+(index+1)+"/"+pages.size();
                    text(stream,font,note,8,MARGIN,HEIGHT-214);
                    float y=CONTACT_TOP;
                    for(var row:pages.get(index)){text(stream,font,row.value(),row.heading()?8:CONTACT_SIZE,MARGIN,y);y-=LEADING;}
                    text(stream,font,"Khối lượng: "+parcel.facts().weightGrams()+" g",8,MARGIN,49);
                    text(stream,font,"UNPAID - CHƯA THU TIỀN / KHÔNG COD",8,MARGIN,31);
                    text(stream,font,"Nhãn mô phỏng; không phải biên nhận Carrier hoặc hóa đơn.",6.5f,MARGIN,16);
                }
              }
            }
            document.getDocumentInformation().setTitle("ArgoPost - Local/Test Simulation Labels");document.save(output);return output.toByteArray();
        }catch(IOException failure){throw new IllegalStateException("PRIVATE_LABEL_RENDER_FAILED");}
    }
    private static InputStream resource(String name)throws IOException{var s=PilotLabelRenderer.class.getResourceAsStream("/labels/"+name);if(s==null)throw new IOException("LABEL_RESOURCE_MISSING");return s;}
    private static void text(PDPageContentStream s,PDType0Font font,String value,float size,float x,float y)throws IOException{
        if(x<MARGIN||x+width(font,value,size)>WIDTH-MARGIN+0.1f||y<16||y>HEIGHT-16)throw new IOException("PRIVATE_LABEL_PRINTABLE_BOUNDS_REQUIRED");
        s.setNonStrokingColor(Color.BLACK);s.beginText();s.setFont(font,size);s.newLineAtOffset(x,y);s.showText(value);s.endText();
    }
    private static float width(PDType0Font font,String value,float size)throws IOException{
        try{return font.getStringWidth(value)*size/1000;}catch(IllegalArgumentException unsupported){throw new IOException("PRIVATE_LABEL_UNSUPPORTED_CHARACTER");}
    }
    /** Measure actual embedded-font advances; split unbroken tokens only at Unicode grapheme boundaries. */
    private static List<String> wrapped(PDType0Font font,String value)throws IOException{
        var boundaries=BreakIterator.getCharacterInstance(Locale.ROOT);boundaries.setText(value);
        List<String> clusters=new ArrayList<>();int previous=boundaries.first();
        for(int next=boundaries.next();next!=BreakIterator.DONE;previous=next,next=boundaries.next())clusters.add(value.substring(previous,next));
        List<String> lines=new ArrayList<>();int start=0;
        while(start<clusters.size()){
            StringBuilder line=new StringBuilder();int end=start,lastSpace=-1;
            while(end<clusters.size()&&width(font,line.toString()+clusters.get(end),CONTACT_SIZE)<=WIDTH-2*MARGIN){
                if(clusters.get(end).isBlank())lastSpace=end;line.append(clusters.get(end++));
            }
            if(end==start)throw new IOException("PRIVATE_LABEL_GLYPH_EXCEEDS_LINE");
            if(end<clusters.size()&&lastSpace>start)end=lastSpace;
            lines.add(String.join("",clusters.subList(start,end)).stripTrailing());start=end;
            while(start<clusters.size()&&clusters.get(start).isBlank())start++;
        }
        return lines;
    }
    private static void contact(List<Row> rows,PDType0Font font,String heading,ShippingDrafts.Contact contact)throws IOException{
        rows.add(new Row(heading,heading,true));var a=contact.address();
        for(String value:List.of(contact.name()+" | "+contact.phone(),a.addressLine()+", "+a.locality()+", "+a.administrativeArea()))
            for(String line:wrapped(font,value))rows.add(new Row(heading,line,false));
    }
    /** One primary label per Parcel plus explicitly bound continuation sheets; never truncate snapshot content. */
    private static List<List<Row>> contactPages(PDType0Font font,ShippingDrafts.Input input)throws IOException{
        List<Row> rows=new ArrayList<>();contact(rows,font,"NGƯỜI NHẬN",input.recipient());contact(rows,font,"NGƯỜI GỬI",input.sender());
        int capacity=(int)Math.floor((CONTACT_TOP-CONTACT_BOTTOM)/LEADING)+1;
        List<List<Row>> pages=new ArrayList<>();List<Row> current=new ArrayList<>();
        for(var row:rows){
            if(current.size()>=capacity||(row.heading()&&current.size()==capacity-1)){
                pages.add(List.copyOf(current));current=new ArrayList<>();
                if(!row.heading())current.add(new Row(row.section(),row.section()+" (tiếp)",true));
            }
            current.add(row);
        }
        if(!current.isEmpty())pages.add(List.copyOf(current));return List.copyOf(pages);
    }
    private static void drawBarcode(PDPageContentStream s,String code,float x,float y,float width,float height)throws IOException{
        var bits=new Code128Writer().encode(code,BarcodeFormat.CODE_128,1,1,Map.of(EncodeHintType.MARGIN,16));float unit=width/bits.getWidth();s.setNonStrokingColor(Color.BLACK);for(int i=0;i<bits.getWidth();i++)if(bits.get(i,0))s.addRect(x+i*unit,y,unit,height);s.fill();
    }
}
