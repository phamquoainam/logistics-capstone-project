package com.elogistic.backend.workflow.internal.web;
import com.elogistic.backend.orders.api.BusinessImports;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.*;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.core.StreamReadFeature;
import static com.elogistic.backend.workflow.internal.web.CustomerProfileHttp.problem;
import static com.elogistic.backend.workflow.internal.web.BusinessPreparationHttp.*;

@RestController @Profile("customer")
public final class BusinessImportHttp {
    private static final String BASE="/api/v1/merchant-organizations/{organizationId}", PATH=BASE+"/shipment-imports";
    private static final JsonMapper JSON=JsonMapper.builder().enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION).build();
    private final BusinessPreparationHttp admission;private final BusinessImports imports;
    public BusinessImportHttp(BusinessPreparationHttp admission,BusinessImports imports){this.admission=admission;this.imports=imports;}
    private static int page(HttpServletRequest r){if(!Set.of("page","size").containsAll(r.getParameterMap().keySet())||r.getParameterMap().values().stream().anyMatch(v->v.length!=1)||number(r.getParameter("size"),20)!=20)throw problem(400,"VALIDATION_FAILED");return number(r.getParameter("page"),0);}
    private static void noQuery(HttpServletRequest r){if(!r.getParameterMap().isEmpty())throw problem(400,"VALIDATION_FAILED");}
    @GetMapping(BASE+"/shipment-import-template") public ResponseEntity<String> template(@PathVariable String organizationId,HttpServletRequest r){if(r.getParameterMap().size()!=1||r.getParameterValues("format")==null||r.getParameterValues("format").length!=1||!"csv".equals(r.getParameter("format")))throw problem(400,"VALIDATION_FAILED");String csv=admission.execute(organizationId,imports::template);return ResponseEntity.ok().header("Cache-Control","no-store").header("X-Content-Type-Options","nosniff").header("Content-Disposition","attachment; filename=argopost-business-v1.csv").header("Content-Type","text/csv; charset=UTF-8").body(csv);}
    @GetMapping(PATH) public ResponseEntity<BusinessImports.Page> list(@PathVariable String organizationId,HttpServletRequest r){int page=page(r);return ResponseEntity.ok().header("Cache-Control","no-store").body(admission.execute(organizationId,a->imports.list(a,page)));}
    @GetMapping(PATH+"/{id}") public ResponseEntity<BusinessImports.Job> get(@PathVariable String organizationId,@PathVariable String id,HttpServletRequest r){int page=page(r);return response(admission.execute(organizationId,a->imports.get(a,uuid(id),page)));}
    @PostMapping(PATH) public ResponseEntity<BusinessImports.Job> preview(@PathVariable String organizationId,HttpServletRequest r)throws IOException{
        noQuery(r);UUID key=key(r);var body=body(r,1048576);shape(body,Set.of("templateVersion","csv"),Set.of("templateVersion","facilityId","csv"));if(!body.get("templateVersion").isString()||!body.get("csv").isString())throw problem(400,"VALIDATION_FAILED");UUID site=body.has("facilityId")&&!body.get("facilityId").isNull()?uuid(body.get("facilityId").asString()):null;
        var job=admission.execute(organizationId,a->imports.preview(a,body.get("templateVersion").asString(),site,body.get("csv").asString(),key));return ResponseEntity.status(201).header("Cache-Control","no-store").eTag("\"v"+job.version()+"\"").body(job);
    }
    @PostMapping(PATH+"/{id}/commit") public ResponseEntity<BusinessImports.Job> commit(@PathVariable String organizationId,@PathVariable String id,HttpServletRequest r)throws IOException{
        noQuery(r);UUID key=key(r),target=uuid(id);long expected=version(r);var body=body(r,8192);shape(body,Set.of("rows"),Set.of("rows"));var selected=body.get("rows");if(!selected.isArray()||selected.isEmpty()||selected.size()>20)throw problem(400,"VALIDATION_FAILED");List<Integer> rows=new ArrayList<>();for(var row:selected){if(!row.isIntegralNumber()||!row.canConvertToInt())throw problem(400,"VALIDATION_FAILED");rows.add(row.asInt());}return response(admission.execute(organizationId,a->imports.commit(a,target,rows,expected,key)));
    }
    private static ResponseEntity<BusinessImports.Job> response(BusinessImports.Job job){return ResponseEntity.ok().header("Cache-Control","no-store").eTag("\"v"+job.version()+"\"").body(job);}
    private static JsonNode body(HttpServletRequest r,int maximum)throws IOException{if(r.getContentType()==null||!r.getContentType().split(";",2)[0].equalsIgnoreCase("application/json"))throw problem(400,"VALIDATION_FAILED");byte[] bytes=r.getInputStream().readNBytes(maximum+1);try{if(bytes.length>maximum)throw problem(400,"VALIDATION_FAILED");return JSON.readTree(bytes);}catch(CustomerProfileHttp.HttpProblem rejected){throw rejected;}catch(RuntimeException invalid){throw problem(400,"VALIDATION_FAILED");}finally{Arrays.fill(bytes,(byte)0);}}
}
