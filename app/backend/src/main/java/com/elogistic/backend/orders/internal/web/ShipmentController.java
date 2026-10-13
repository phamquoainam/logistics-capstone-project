package com.elogistic.backend.orders.internal.web;
import com.elogistic.backend.identity.api.AccountView;
import com.elogistic.backend.orders.internal.application.ShipmentService;
import com.elogistic.backend.orders.internal.domain.*;
import com.elogistic.backend.orders.internal.infrastructure.ShipmentRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.net.URI;
import java.util.*;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
@RestController @Profile("demo") @RequestMapping("/api/demo/v1")
class ShipmentController {
 record CancelInput(@NotBlank @Size(max=300) String reason) {}
 private final ShipmentService service;
 ShipmentController(ShipmentService service){this.service=service;}
 @GetMapping("/shipments") List<ShipmentView> list(@AuthenticationPrincipal AccountView user){return service.list(user);}
 @PostMapping("/shipments") ResponseEntity<ShipmentView> create(@AuthenticationPrincipal AccountView user,@RequestHeader("Idempotency-Key") String key,@Valid @RequestBody DraftInput input){ShipmentView s=service.create(user,key,input);return ResponseEntity.created(URI.create("/api/demo/v1/shipments/"+s.id())).body(s);}
 @GetMapping("/shipments/{id}") ShipmentService.Detail detail(@AuthenticationPrincipal AccountView user,@PathVariable UUID id){return service.detail(user,id);}
 @PostMapping("/shipments/{id}/cancel") ShipmentView cancel(@AuthenticationPrincipal AccountView user,@PathVariable UUID id,@RequestHeader("Idempotency-Key") String key,@Valid @RequestBody CancelInput input){return service.cancel(user,id,key,input.reason());}
 @GetMapping("/admin/overview") ShipmentRepository.Overview overview(){return service.overview();}
 @GetMapping("/admin/shipments") List<ShipmentRepository.Summary> summaries(){return service.summaries();}
}
