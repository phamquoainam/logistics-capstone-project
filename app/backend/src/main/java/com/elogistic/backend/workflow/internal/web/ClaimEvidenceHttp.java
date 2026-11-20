package com.elogistic.backend.workflow.internal.web;

import com.elogistic.backend.orders.api.*;
import com.elogistic.backend.orders.api.OrganizationClaimEvidence.*;
import com.elogistic.backend.platform.api.*;
import jakarta.servlet.http.*;
import java.io.*;
import java.security.MessageDigest;
import java.util.*;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Profile;
import org.springframework.http.*;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.bind.annotation.*;
import tools.jackson.core.StreamReadFeature;
import tools.jackson.databind.json.JsonMapper;
import static com.elogistic.backend.workflow.internal.web.CustomerProfileHttp.problem;

@RestController @Profile("customer")
public final class ClaimEvidenceHttp {
    private static final String BASE="/api/v1/merchant-organizations/{organizationId}/claim-intakes/{id}",STAFF="/api/v1/support-operations/organization-claim-intakes/{id}";
    private static final JsonMapper JSON=JsonMapper.builder().enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION).build();
    private final OrganizationSupportHttp admission;private final OrganizationClaimEvidence evidence;private final OrganizationClaimIntakes claims;private final PrivateEvidenceStore store;private final PrivateEvidenceScreening scanner;
    public ClaimEvidenceHttp(OrganizationSupportHttp admission,OrganizationClaimEvidence evidence,OrganizationClaimIntakes claims,@Qualifier("claimPrivateEvidenceStore")PrivateEvidenceStore store,@Qualifier("claimEvidenceScreening")PrivateEvidenceScreening scanner){this.admission=admission;this.evidence=evidence;this.claims=claims;this.store=store;this.scanner=scanner;}
    private <T>T target(String org,UUID intake,java.util.function.Function<OrganizationSupportTickets.Access,T> work){var refs=admission.execute(org,a->claims.references(a,intake));return admission.execute(org,refs,work);}
    @ModelAttribute public void privacy(HttpServletResponse r){r.setHeader("Cache-Control","no-store");r.setHeader("X-Content-Type-Options","nosniff");}
    private <T>T errors(java.util.function.Supplier<T> work){try{return work.get();}catch(OrganizationClaimIntakes.Failure f){throw problem(switch(f.getMessage()){case "VALIDATION_FAILED"->400;case "RESOURCE_NOT_FOUND"->404;case "PRECONDITION_STALE","IDEMPOTENCY_CONFLICT","CLAIM_STATE_CONFLICT","CLAIM_EVIDENCE_EXPIRED","CLAIM_EVIDENCE_LIMIT"->409;default->503;},f.getMessage());}catch(PrivateEvidenceScreening.Rejected|PrivateEvidenceStore.Rejected f){throw problem(422,"EVIDENCE_CONTENT_REJECTED");}catch(PrivateEvidenceScreening.Unavailable|PrivateEvidenceStore.Unavailable f){throw problem(503,"EVIDENCE_UNAVAILABLE");}}
    private static UUID id(String raw){try{UUID id=UUID.fromString(raw);if(!id.toString().equals(raw)||id.equals(new UUID(0,0)))throw new IllegalArgumentException();return id;}catch(RuntimeException invalid){throw problem(400,"VALIDATION_FAILED");}}
    private static String header(HttpServletRequest r,String key){var values=Collections.list(r.getHeaders(key));if(values.size()!=1)throw problem(key.equals("If-Match")?428:400,"VALIDATION_FAILED");return values.getFirst();}
    private static void query(HttpServletRequest r){if(!r.getParameterMap().isEmpty())throw problem(400,"VALIDATION_FAILED");}
    private static void external(){if(TransactionSynchronizationManager.isActualTransactionActive()||!TransactionSynchronizationManager.getResourceMap().isEmpty())throw new IllegalStateException("EVIDENCE_IO_TRANSACTION_FORBIDDEN");}
    @GetMapping(BASE+"/evidence") public List<Evidence> list(@PathVariable String organizationId,@PathVariable String id,HttpServletRequest r){query(r);return errors(()->admission.execute(organizationId,a->evidence.list(a,id(id))));}
    @GetMapping(STAFF+"/evidence") public List<Evidence> staffList(@PathVariable String id,HttpServletRequest r){query(r);return errors(()->admission.staff(Set.of(),a->evidence.staffList(a,id(id))));}
    @PostMapping(BASE+"/evidence-reservations") public ResponseEntity<Evidence> reserve(@PathVariable String organizationId,@PathVariable String id,HttpServletRequest r)throws IOException{
        query(r);if(r.getContentType()==null||!r.getContentType().split(";",2)[0].equalsIgnoreCase("application/json"))throw problem(400,"VALIDATION_FAILED");byte[] bytes=r.getInputStream().readNBytes(4097);Input input;
        try{var v=JSON.readTree(bytes);if(bytes.length>4096||v==null||!v.isObject()||!v.propertyNames().equals(Set.of("mime","bytes","sha256"))||!v.get("mime").isString()||!v.get("sha256").isString()||!v.get("bytes").isIntegralNumber()||!v.get("bytes").canConvertToLong())throw new IllegalArgumentException();input=new Input(v.get("mime").asString(),v.get("bytes").asLong(),v.get("sha256").asString());}catch(Exception invalid){throw problem(400,"VALIDATION_FAILED");}finally{Arrays.fill(bytes,(byte)0);}
        String raw=header(r,"If-Match");long version;try{if(!raw.matches("\"v[1-9][0-9]*\""))throw new IllegalArgumentException();version=Long.parseLong(raw.substring(2,raw.length()-1));}catch(RuntimeException invalid){throw problem(400,"VALIDATION_FAILED");}UUID key=id(header(r,"Idempotency-Key")),target=id(id);
        return ResponseEntity.status(201).body(errors(()->target(organizationId,target,a->evidence.reserve(a,target,input,version,key))));
    }
    @PutMapping(BASE+"/evidence/{evidenceId}") public Evidence upload(@PathVariable String organizationId,@PathVariable String id,@PathVariable String evidenceId,HttpServletRequest r)throws IOException{
        query(r);UUID target=id(id),file=id(evidenceId);Permit permit=errors(()->admission.execute(organizationId,a->evidence.uploadPermit(a,target,file)));
        if(!permit.evidence().mime().equals(r.getContentType())||r.getContentLengthLong()>PrivateEvidenceStore.MAX_BYTES)throw problem(422,"EVIDENCE_CONTENT_REJECTED");
        byte[] bytes=r.getInputStream().readNBytes((int)PrivateEvidenceStore.MAX_BYTES+1);
        try{return errors(()->{external();try{if(bytes.length!=permit.evidence().bytes()||!MessageDigest.isEqual(MessageDigest.getInstance("SHA-256").digest(bytes),HexFormat.of().parseHex(permit.evidence().sha256())))throw new PrivateEvidenceStore.Rejected();}catch(java.security.GeneralSecurityException unavailable){throw new PrivateEvidenceStore.Unavailable();}
            if(!permit.evidence().mime().equals(scanner.inspect(bytes,permit.evidence().mime())))throw new PrivateEvidenceScreening.Unavailable();var blob=store.put(permit.objectKey(),new ByteArrayInputStream(bytes));return target(organizationId,target,a->evidence.complete(a,permit,blob));});}finally{Arrays.fill(bytes,(byte)0);}
    }
    private ResponseEntity<byte[]> download(Permit permit,java.util.function.Supplier<Permit> recheck){return errors(()->{
        external();byte[] bytes;try(var content=store.read(new PrivateEvidenceStore.Blob(permit.objectKey(),permit.evidence().bytes(),permit.evidence().sha256()))){bytes=content.bytes();}
        try{Permit current=recheck.get();if(!current.equals(permit))throw new PrivateEvidenceStore.Unavailable();String ext=switch(permit.evidence().mime()){case "image/png"->"png";case "image/jpeg"->"jpg";default->"pdf";};return ResponseEntity.ok().contentType(MediaType.parseMediaType(permit.evidence().mime())).header("Content-Disposition","attachment; filename=\"argopost-evidence-"+permit.evidence().id()+"."+ext+"\"").body(bytes.clone());}finally{Arrays.fill(bytes,(byte)0);}
    });}
    @GetMapping(BASE+"/evidence/{evidenceId}") public ResponseEntity<byte[]> read(@PathVariable String organizationId,@PathVariable String id,@PathVariable String evidenceId,HttpServletRequest r){query(r);UUID target=id(id),file=id(evidenceId);java.util.function.Supplier<Permit> current=()->admission.execute(organizationId,a->evidence.readPermit(a,target,file));return errors(()->download(current.get(),current));}
    @GetMapping(STAFF+"/evidence/{evidenceId}") public ResponseEntity<byte[]> staffRead(@PathVariable String id,@PathVariable String evidenceId,HttpServletRequest r){query(r);UUID target=id(id),file=id(evidenceId);java.util.function.Supplier<Permit> current=()->admission.staff(Set.of(),a->evidence.staffReadPermit(a,target,file));return errors(()->download(current.get(),current));}
}
