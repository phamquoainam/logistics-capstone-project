package com.elogistic.backend.orders.internal.application;
import com.elogistic.backend.orders.api.*;
import com.elogistic.backend.orders.internal.domain.ShippingPreparation;
import java.nio.charset.StandardCharsets;
import java.util.*;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.core.StreamReadFeature;

/** RFC4180 structure, then field-level validation using the shared Shipping model. */
public final class BusinessCsv {
    private BusinessCsv(){}
    public static final List<String> HEADERS=List.of("source_channel","source_order_reference","sender_name","sender_phone","sender_country","sender_administrative_area","sender_locality","sender_address_line","recipient_name","recipient_phone","recipient_country","recipient_administrative_area","recipient_locality","recipient_address_line","parcels_json","reference","instructions");
    private static final JsonMapper JSON=JsonMapper.builder().enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION).build();
    private static BusinessPreparations.Failure invalid(){return new BusinessPreparations.Failure("VALIDATION_FAILED");}
    public static List<BusinessImports.Row> parse(UUID facility,String csv){
        if(csv==null||csv.isBlank()||csv.getBytes(StandardCharsets.UTF_8).length>262144)throw invalid();
        if(csv.startsWith("\uFEFF"))csv=csv.substring(1);
        var records=records(csv);if(records.size()<2||!records.getFirst().equals(HEADERS))throw invalid();
        List<BusinessImports.Row> result=new ArrayList<>();Map<String,Integer> sources=new HashMap<>();Set<String> duplicates=new HashSet<>();
        for(int i=1;i<records.size();i++){
            var cells=records.get(i);int number=i;List<BusinessImports.Error> errors=new ArrayList<>();BusinessPreparations.Input input=null;
            if(cells.size()!=HEADERS.size())errors.add(new BusinessImports.Error(number,"row","COLUMN_COUNT"));
            else{
                for(int n=0;n<cells.size();n++){
                    String value=cells.get(n).strip();boolean phone=n==3||n==9;
                    if(value.codePoints().anyMatch(Character::isISOControl))errors.add(new BusinessImports.Error(number,HEADERS.get(n),"CONTROL_CHARACTER"));
                    if(!phone&&!value.isEmpty()&&"=+-@".indexOf(value.charAt(0))>=0)errors.add(new BusinessImports.Error(number,HEADERS.get(n),"FORMULA_NOT_ALLOWED"));
                }
                String channel=cells.get(0).strip(),reference=cells.get(1).strip();
                if(channel.isEmpty()||reference.isEmpty()||channel.length()>40||reference.length()>80)errors.add(new BusinessImports.Error(number,"source_order_reference","SOURCE_KEY_REQUIRED"));
                String source=channel+"\u0000"+reference;if(sources.putIfAbsent(source,number)!=null)duplicates.add(source);
                try{
                    var parcelsNode=JSON.readTree(cells.get(14));if(!parcelsNode.isArray()||parcelsNode.isEmpty()||parcelsNode.size()>5)throw invalid();
                    var fields=Set.of("description","quantity","weightGrams","lengthMm","widthMm","heightMm","declaredValueVnd");
                    List<ShippingDrafts.Parcel> parcels=new ArrayList<>();
                    for(var p:parcelsNode){if(!p.isObject()||!p.propertyNames().equals(fields)||!p.get("description").isString())throw invalid();for(var f:fields)if(!f.equals("description")&&(!p.get(f).isIntegralNumber()||!p.get(f).canConvertToLong()))throw invalid();parcels.add(JSON.treeToValue(p,ShippingDrafts.Parcel.class));}
                    var normalized=ShippingPreparation.normalize(new ShippingDrafts.Input(optional(cells.get(15)),optional(cells.get(16)),contact(cells,2),contact(cells,8),parcels));
                    input=new BusinessPreparations.Input(facility,channel,reference,normalized.reference(),normalized.instructions(),normalized.sender(),normalized.recipient(),normalized.parcels());
                }catch(RuntimeException bad){errors.add(new BusinessImports.Error(number,"shipment","INVALID_SHIPPING_INPUT"));}
            }
            result.add(new BusinessImports.Row(number,errors.isEmpty()?"VALID":"INVALID",errors.isEmpty()?input:null,errors,null,1));
        }
        return result.stream().map(row->{if(row.input()!=null&&duplicates.contains(row.input().sourceChannel()+"\u0000"+row.input().sourceOrderReference()))return new BusinessImports.Row(row.rowNumber(),"INVALID",null,List.of(new BusinessImports.Error(row.rowNumber(),"source_order_reference","DUPLICATE_SOURCE_KEY")),null,1);return row;}).toList();
    }
    private static String optional(String v){return v.isBlank()?null:v;}
    private static ShippingDrafts.Contact contact(List<String> v,int start){return new ShippingDrafts.Contact(v.get(start),v.get(start+1),new ShippingDrafts.Address(v.get(start+2),v.get(start+3),v.get(start+4),v.get(start+5)));}
    private static List<List<String>> records(String csv){
        List<List<String>> rows=new ArrayList<>();List<String> row=new ArrayList<>();StringBuilder field=new StringBuilder();boolean quoted=false,closed=false,started=false;
        for(int i=0;i<csv.length();i++){
            char c=csv.charAt(i);if(quoted){if(c=='"'){if(i+1<csv.length()&&csv.charAt(i+1)=='"'){field.append('"');i++;}else{quoted=false;closed=true;}}else field.append(c);continue;}
            if(c=='"'){if(started||closed)throw invalid();quoted=true;started=true;continue;}
            if(c==','||c=='\r'||c=='\n'){
                row.add(field.toString());field.setLength(0);started=false;closed=false;
                if(c!=','){if(c=='\r'){if(i+1>=csv.length()||csv.charAt(++i)!='\n')throw invalid();}rows.add(List.copyOf(row));row.clear();if(rows.size()>101)throw invalid();}
            }else{if(closed)throw invalid();field.append(c);started=true;}
        }
        if(quoted)throw invalid();if(started||closed||!row.isEmpty()){row.add(field.toString());rows.add(List.copyOf(row));}
        if(rows.size()>101)throw invalid();return rows;
    }
}
