package com.elogistic.backend.demo;
import com.jayway.jsonpath.JsonPath;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
class DemoShipmentTests extends DemoTestSupport {
 String input(String reference){return "{\"reference\":\""+reference+"\",\"pickupAddress\":\"Demo pickup\",\"recipientName\":\"Synthetic recipient\",\"recipientPhone\":\"0000000000\",\"deliveryStreet\":\"Demo street\",\"deliveryWard\":\"Demo ward\",\"deliveryProvince\":\"Demo province\",\"parcelDescription\":\"Demo parcel\",\"weightKg\":1.25}";}
 String create(String token,String key,String body) throws Exception {return mvc.perform(post("/api/demo/v1/shipments").header("Authorization","Bearer "+token).header("Idempotency-Key",key).contentType("application/json").content(body)).andExpect(status().isCreated()).andExpect(jsonPath("$.status").value("DRAFT")).andReturn().getResponse().getContentAsString();}
 @Test void ownDraftAndCrossMerchantIsolation() throws Exception {
  String a=login("demo.merchant.a"),b=login("demo.merchant.b");String id=JsonPath.read(create(a,UUID.randomUUID().toString(),input("OWN-"+UUID.randomUUID())),"$.id");
  mvc.perform(get("/api/demo/v1/merchant/me").header("Authorization","Bearer "+a)).andExpect(status().isOk()).andExpect(jsonPath("$.reference").value("DEMO-A"));
  mvc.perform(get("/api/demo/v1/shipments/"+id).header("Authorization","Bearer "+a)).andExpect(status().isOk()).andExpect(jsonPath("$.shipment.status").value("DRAFT")).andExpect(jsonPath("$.events.length()").value(1));
  mvc.perform(get("/api/demo/v1/shipments/"+id).header("Authorization","Bearer "+b)).andExpect(status().isNotFound());
  mvc.perform(post("/api/demo/v1/shipments/"+id+"/cancel").header("Authorization","Bearer "+b).header("Idempotency-Key",UUID.randomUUID().toString()).contentType("application/json").content("{\"reason\":\"Demo\"}")).andExpect(status().isNotFound());
  String list=mvc.perform(get("/api/demo/v1/shipments").header("Authorization","Bearer "+b)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();assertFalse(list.contains(id));
 }
 @Test void createReplayConflictAndReferenceUniqueness() throws Exception {
  String a=login("demo.merchant.a"),key=UUID.randomUUID().toString(),body=input("REPLAY-"+UUID.randomUUID());
  String first=JsonPath.read(create(a,key,body),"$.id"),second=JsonPath.read(create(a,key,body),"$.id");assertEquals(first,second);
  mvc.perform(post("/api/demo/v1/shipments").header("Authorization","Bearer "+a).header("Idempotency-Key",key).contentType("application/json").content(body.replace("1.25","2.5"))).andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("IDEMPOTENCY_CONFLICT"));
  mvc.perform(post("/api/demo/v1/shipments").header("Authorization","Bearer "+a).header("Idempotency-Key",UUID.randomUUID().toString()).contentType("application/json").content(body)).andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("REFERENCE_EXISTS"));
  assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM orders.demo_event WHERE shipment_id=?",Integer.class,UUID.fromString(first)));
 }
 @Test void validationOwnerAndStatusInjectionRejected() throws Exception {
  String token=login("demo.merchant.a"),body=input("INVALID-"+UUID.randomUUID());long before=jdbc.queryForObject("SELECT COUNT(*) FROM orders.demo_shipment",Long.class);
  for(String invalid:new String[]{body.replace("1.25","-1"),body.replace("1.25","1.2345"),body.replace("Demo ward",""),body.replace("}",",\"ownerId\":\"forged\"}"),body.replace("}",",\"status\":\"CONFIRMED\"}")})
   mvc.perform(post("/api/demo/v1/shipments").header("Authorization","Bearer "+token).header("Idempotency-Key",UUID.randomUUID().toString()).contentType("application/json").content(invalid)).andExpect(status().isBadRequest());
  mvc.perform(post("/api/demo/v1/shipments").header("Authorization","Bearer "+token).contentType("application/json").content(body)).andExpect(status().isBadRequest());
  assertEquals(before,jdbc.queryForObject("SELECT COUNT(*) FROM orders.demo_shipment",Long.class));
 }
 @Test void cancelGuardReplayAndImmutableEvents() throws Exception {
  String token=login("demo.merchant.a"),id=JsonPath.read(create(token,UUID.randomUUID().toString(),input("CANCEL-"+UUID.randomUUID())),"$.id"),key=UUID.randomUUID().toString();
  String path="/api/demo/v1/shipments/"+id+"/cancel";
  for(int i=0;i<2;i++)mvc.perform(post(path).header("Authorization","Bearer "+token).header("Idempotency-Key",key).contentType("application/json").content("{\"reason\":\"Cancelled demo draft\"}")).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("CANCELLED"));
  mvc.perform(post(path).header("Authorization","Bearer "+token).header("Idempotency-Key",key).contentType("application/json").content("{\"reason\":\"Different\"}")).andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("IDEMPOTENCY_CONFLICT"));
  mvc.perform(post(path).header("Authorization","Bearer "+token).header("Idempotency-Key",UUID.randomUUID().toString()).contentType("application/json").content("{\"reason\":\"Again\"}")).andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("INVALID_TRANSITION"));
  mvc.perform(patch("/api/demo/v1/shipments/"+id).header("Authorization","Bearer "+token).contentType("application/json").content("{\"status\":\"DELIVERED\"}")).andExpect(status().isForbidden());
  assertEquals(2,jdbc.queryForObject("SELECT COUNT(*) FROM orders.demo_event WHERE shipment_id=?",Integer.class,UUID.fromString(id)));
 }
 @Test void concurrentCreateHasOneEffect() throws Exception {
  String token=login("demo.merchant.a"),key=UUID.randomUUID().toString(),body=input("CONCURRENT-"+UUID.randomUUID());
  try(ExecutorService executor=Executors.newFixedThreadPool(2)) {
   CountDownLatch start=new CountDownLatch(1);
   Callable<String> request=()->{start.await();return JsonPath.read(create(token,key,body),"$.id");};
   Future<String> a=executor.submit(request),b=executor.submit(request);start.countDown();String id=a.get(15,TimeUnit.SECONDS);assertEquals(id,b.get(15,TimeUnit.SECONDS));
   assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM orders.demo_event WHERE shipment_id=?",Integer.class,UUID.fromString(id)));
  }
 }
 @Test void databaseConstraintAndAdminSummary() throws Exception {
  String a=login("demo.merchant.a"),admin=login("demo.admin"),id=JsonPath.read(create(a,UUID.randomUUID().toString(),input("CONSTRAINT-"+UUID.randomUUID())),"$.id");
  assertThrows(org.springframework.dao.DataIntegrityViolationException.class,()->jdbc.update("UPDATE orders.demo_shipment SET weight_kg=-1 WHERE id=?",UUID.fromString(id)));
  assertThrows(org.springframework.dao.DataIntegrityViolationException.class,()->jdbc.update("UPDATE orders.demo_shipment SET status='CONFIRMED' WHERE id=?",UUID.fromString(id)));
  mvc.perform(get("/api/demo/v1/admin/overview").header("Authorization","Bearer "+admin)).andExpect(status().isOk()).andExpect(jsonPath("$.total").isNumber());
  mvc.perform(get("/api/demo/v1/admin/shipments").header("Authorization","Bearer "+admin)).andExpect(status().isOk()).andExpect(jsonPath("$[0].recipientPhone").doesNotExist());
 }
}
