package com.elogistic.backend.orders.internal.infrastructure;

import com.elogistic.backend.orders.api.*;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.oned.Code128Writer;
import java.io.*;
import java.awt.Color;
import java.util.*;
import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

/** Private in-memory100x150mm PDF; no HTML, network, file paths or mutable Address Book. */
public final class PilotLabelRenderer {
    private static final float WIDTH=100*72/25.4f,HEIGHT=150*72/25.4f,MARGIN=16;
    public static String barcode(UUID parcel){return "AP-P-"+Objects.requireNonNull(parcel).toString().replace("-","");}
    public static UUID parcelId(String code){
        if(code==null||!code.matches("AP-P-[a-f0-9]{32}"))throw new PilotHandlingFailure(PilotHandlingFailure.Code.VALIDATION_FAILED);
        String s=code.substring(5);return UUID.fromString(s.substring(0,8)+"-"+s.substring(8,12)+"-"+s.substring(12,16)+"-"+s.substring(16,20)+"-"+s.substring(20));
    }
    public byte[] render(PilotShipments.Shipment shipment){
        if(shipment.parcels().isEmpty()||shipment.parcels().size()>5||!"CONFIRMED".equals(shipment.bookingState()))throw new IllegalArgumentException("BOOKED_PARCELS_REQUIRED");
        try(var document=new PDDocument();var fontData=resource("NotoSans-Regular.ttf");var logoData=resource("argopost-logo-primary.png");var output=new ByteArrayOutputStream()){
            var font=PDType0Font.load(document,fontData,true);var logo=PDImageXObject.createFromByteArray(document,logoData.readAllBytes(),"ArgoPost");
            for(var parcel:shipment.parcels()){
                var page=new PDPage(new PDRectangle(WIDTH,HEIGHT));document.addPage(page);
                try(var stream=new PDPageContentStream(document,page)){
                    stream.setNonStrokingColor(new Color(255,111,0));stream.addRect(0,HEIGHT-24,WIDTH,24);stream.fill();
                    text(stream,font,"ARGOPOST - LOCAL/TEST SIMULATION",8,MARGIN,HEIGHT-16);
                    float logoWidth=126,logoHeight=Math.min(38,logoWidth*logo.getHeight()/logo.getWidth());stream.drawImage(logo,MARGIN,HEIGHT-72,logoWidth,logoHeight);
                    text(stream,font,"ROAD_STANDARD",8,169,HEIGHT-45);text(stream,font,"Kiện "+parcel.parcelNumber()+" / "+shipment.parcels().size(),10,169,HEIGHT-64);
                    text(stream,font,shipment.code(),7,MARGIN,HEIGHT-91);
                    text(stream,font,shipment.quote().route().originCode()+"  >  "+shipment.quote().route().destinationCode(),13,MARGIN,HEIGHT-112);
                    text(stream,font,"Drop-off: "+shipment.quote().route().dropOffCode(),8,MARGIN,HEIGHT-128);
                    drawBarcode(stream,barcode(parcel.id()),MARGIN,HEIGHT-188,WIDTH-2*MARGIN,43);
                    text(stream,font,barcode(parcel.id()),7,MARGIN,HEIGHT-201);
                    contact(stream,font,"NGƯỜI NHẬN",shipment.input().recipient(),HEIGHT-221,5);
                    contact(stream,font,"NGƯỜI GỬI",shipment.input().sender(),HEIGHT-313,3);
                    text(stream,font,"Khối lượng: "+parcel.facts().weightGrams()+" g",8,MARGIN,49);
                    text(stream,font,"UNPAID - CHƯA THU TIỀN / KHÔNG COD",8,MARGIN,31);
                    text(stream,font,"Nhãn mô phỏng; không phải biên nhận Carrier hoặc hóa đơn.",6.5f,MARGIN,16);
                }
            }
            document.getDocumentInformation().setTitle("ArgoPost - Local/Test Simulation Labels");document.save(output);return output.toByteArray();
        }catch(IOException failure){throw new IllegalStateException("PRIVATE_LABEL_RENDER_FAILED");}
    }
    private static InputStream resource(String name)throws IOException{var s=PilotLabelRenderer.class.getResourceAsStream("/labels/"+name);if(s==null)throw new IOException("LABEL_RESOURCE_MISSING");return s;}
    private static void text(PDPageContentStream s,PDType0Font font,String value,float size,float x,float y)throws IOException{
        StringBuilder safe=new StringBuilder();for(int cp:value.codePoints().toArray()){try{font.encode(new String(Character.toChars(cp)));safe.appendCodePoint(cp);}catch(IllegalArgumentException unavailable){safe.append('?');}}
        s.setNonStrokingColor(Color.BLACK);s.beginText();s.setFont(font,size);s.newLineAtOffset(x,y);s.showText(safe.toString());s.endText();
    }
    private static float wrapped(PDPageContentStream s,PDType0Font font,String value,float y,int maximum)throws IOException{
        String current="";int rows=0;for(String word:value.replaceAll("[\\p{Cntrl}\\s]+"," ").trim().split(" ")){
            String next=current.isEmpty()?word:current+" "+word;
            if(next.codePointCount(0,next.length())>47){text(s,font,current.isEmpty()?word.substring(0,Math.min(43,word.length()))+"…":current,8,MARGIN,y);y-=12;if(++rows>=maximum)return y;current=current.isEmpty()?"":word;}else current=next;
        }
        if(!current.isEmpty()&&rows<maximum){text(s,font,current,8,MARGIN,y);y-=12;}return y;
    }
    private static void contact(PDPageContentStream s,PDType0Font font,String heading,ShippingDrafts.Contact contact,float y,int lines)throws IOException{
        text(s,font,heading,7,MARGIN,y);y-=14;y=wrapped(s,font,contact.name()+" | "+contact.phone(),y,2);var a=contact.address();wrapped(s,font,a.addressLine()+", "+a.locality()+", "+a.administrativeArea(),y,lines);
    }
    private static void drawBarcode(PDPageContentStream s,String code,float x,float y,float width,float height)throws IOException{
        var bits=new Code128Writer().encode(code,BarcodeFormat.CODE_128,1,1,Map.of(EncodeHintType.MARGIN,16));float unit=width/bits.getWidth();s.setNonStrokingColor(Color.BLACK);for(int i=0;i<bits.getWidth();i++)if(bits.get(i,0))s.addRect(x+i*unit,y,unit,height);s.fill();
    }
}
