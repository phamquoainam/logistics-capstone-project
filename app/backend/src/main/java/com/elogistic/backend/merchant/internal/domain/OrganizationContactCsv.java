package com.elogistic.backend.merchant.internal.domain;
import com.elogistic.backend.merchant.api.customer.OrganizationContacts.*;
import com.elogistic.backend.merchant.api.customer.OrganizationContactTransfers.Row;
import com.elogistic.backend.merchant.api.customer.PersonalAddresses.Location;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Strict bounded RFC4180 contact format, with no executable spreadsheet content. */
public final class OrganizationContactCsv {
    private OrganizationContactCsv(){}
    public static final List<String> HEADERS=List.of("kind","label","reference","group","contact_name","contact_phone","country","administrative_area","locality","address_line");
    private static Failure invalid(){return new Failure("VALIDATION_FAILED");}
    private static String optional(String v){return v.isBlank()?null:v;}
    public static List<Row> parse(UUID facility,String csv){
        if(csv==null||csv.isBlank()||csv.getBytes(StandardCharsets.UTF_8).length>262144)throw invalid();
        if(csv.startsWith("\uFEFF"))csv=csv.substring(1);var records=records(csv);if(records.size()<2||!records.getFirst().equals(HEADERS))throw invalid();
        List<Row> rows=new ArrayList<>();Set<String> refs=new HashSet<>(),duplicateRefs=new HashSet<>();
        for(int i=1;i<records.size();i++){var cells=records.get(i);List<String> errors=new ArrayList<>();Input in=null;
            if(cells.size()!=10)errors.add("COLUMN_COUNT");else{
                for(int k=0;k<10;k++){String v=cells.get(k).strip();if(v.codePoints().anyMatch(Character::isISOControl))errors.add("CONTROL_CHARACTER");if(!v.isEmpty()&&"=+-@".indexOf(v.charAt(0))>=0&&!(k==5&&v.matches("\\+84[35789][0-9]{8}")))errors.add("FORMULA_NOT_ALLOWED");}
                try{in=OrganizationContactInput.normalize(new Input(cells.get(0),facility,optional(cells.get(1)),optional(cells.get(2)),optional(cells.get(3)),cells.get(4),cells.get(5),new Location(cells.get(6),cells.get(7),cells.get(8),cells.get(9))));}catch(RuntimeException bad){errors.add("INVALID_CONTACT");}
                if(in!=null&&in.reference()!=null){String ref=in.kind()+":"+in.reference();if(!refs.add(ref))duplicateRefs.add(ref);}
            }rows.add(new Row(i,errors.isEmpty()?"VALID":"INVALID",errors.isEmpty()?in:null,List.copyOf(new LinkedHashSet<>(errors)),null));
        }
        return rows.stream().map(row->row.input()!=null&&row.input().reference()!=null&&duplicateRefs.contains(row.input().kind()+":"+row.input().reference())?new Row(row.rowNumber(),"INVALID",null,List.of("DUPLICATE_REFERENCE"),null):row).toList();
    }
    private static List<List<String>> records(String csv){
        List<List<String>> rows=new ArrayList<>();List<String> row=new ArrayList<>();StringBuilder cell=new StringBuilder();boolean quoted=false,closed=false,started=false;
        for(int i=0;i<csv.length();i++){char c=csv.charAt(i);if(quoted){if(c=='"'){if(i+1<csv.length()&&csv.charAt(i+1)=='"'){cell.append('"');i++;}else{quoted=false;closed=true;}}else cell.append(c);continue;}
            if(c=='"'){if(started||closed)throw invalid();quoted=true;started=true;continue;}
            if(c==','||c=='\r'||c=='\n'){row.add(cell.toString());cell.setLength(0);started=false;closed=false;if(c!=','){if(c=='\r'&&(i+1>=csv.length()||csv.charAt(++i)!='\n'))throw invalid();rows.add(List.copyOf(row));row.clear();if(rows.size()>101)throw invalid();}}
            else{if(closed)throw invalid();cell.append(c);started=true;}
        }if(quoted)throw invalid();if(started||closed||!row.isEmpty()){row.add(cell.toString());rows.add(List.copyOf(row));}if(rows.size()>101)throw invalid();return rows;
    }
}
