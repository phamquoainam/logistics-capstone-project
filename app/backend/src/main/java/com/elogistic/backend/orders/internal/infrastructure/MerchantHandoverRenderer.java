package com.elogistic.backend.orders.internal.infrastructure;

import com.elogistic.backend.orders.api.MerchantFacilityWork.Batch;
import java.awt.Color;
import java.io.*;
import java.util.*;
import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType0Font;

/** Private printable preparation list, expressly not a Carrier receipt or custody evidence. */
public final class MerchantHandoverRenderer {
    public byte[] render(Batch batch){
        List<String> rows=new ArrayList<>();
        for(var shipment:batch.shipments())for(var parcel:shipment.parcels()){
            String result=batch.checks().stream().filter(c->c.parcelId().equals(parcel.id())).map(c->c.result()).findFirst().orElse("UNCHECKED");
            rows.add(shipment.shipmentCode()+" | Kiện "+parcel.parcelNumber());
            rows.add(PilotLabelRenderer.barcode(parcel.id())+" | "+result);
        }
        try(var document=new PDDocument();var data=MerchantHandoverRenderer.class.getResourceAsStream("/labels/NotoSans-Regular.ttf");var output=new ByteArrayOutputStream()){
            if(data==null)throw new IOException();var font=PDType0Font.load(document,data,true);
            for(int offset=0;offset<rows.size();offset+=32){var page=new PDPage(PDRectangle.A4);document.addPage(page);try(var stream=new PDPageContentStream(document,page)){
                stream.setNonStrokingColor(new Color(11,45,91));line(stream,font,"ArgoPost · Danh sách chuẩn bị bàn giao",15,790);line(stream,font,"LOCAL/TEST SIMULATION · NOT_CARRIER_ACCEPTANCE",10,766);
                line(stream,font,"Không phải biên nhận Carrier. Không xác nhận đã nhận hàng hoặc thu tiền.",9,747);line(stream,font,"Danh sách: "+batch.id()+" | "+batch.state()+" | v"+batch.version(),9,723);line(stream,font,"Địa điểm Merchant: "+batch.facilityId(),9,705);line(stream,font,batch.shipments().size()+" vận đơn · "+(rows.size()/2)+" kiện · Trang "+(offset/32+1),9,687);
                float y=658;for(int i=offset;i<Math.min(offset+32,rows.size());i++){line(stream,font,rows.get(i),9,y);y-=17;}
                line(stream,font,"Chỉ đối chiếu kiện thật. Chênh lệch do Merchant khai báo, chưa phải kết luận Carrier.",8,50);
            }}document.save(output);return output.toByteArray();
        }catch(IOException failure){throw new IllegalStateException("MERCHANT_HANDOVER_DOCUMENT_UNAVAILABLE");}
    }
    private static void line(PDPageContentStream stream,PDType0Font font,String text,float size,float y)throws IOException{if(font.getStringWidth(text)/1000*size>PDRectangle.A4.getWidth()-84)throw new IOException("DOCUMENT_ROW_TOO_WIDE");stream.beginText();stream.setFont(font,size);stream.newLineAtOffset(42,y);stream.showText(text);stream.endText();}
}
