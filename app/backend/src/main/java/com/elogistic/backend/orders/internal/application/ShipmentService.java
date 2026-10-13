package com.elogistic.backend.orders.internal.application;
import com.elogistic.backend.identity.api.AccountView;
import com.elogistic.backend.merchant.api.MerchantAccess;
import com.elogistic.backend.orders.internal.domain.*;
import com.elogistic.backend.orders.internal.infrastructure.ShipmentRepository;
import com.elogistic.backend.platform.api.DemoException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import org.springframework.context.annotation.Profile;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
@Service @Profile("demo")
public class ShipmentService {
 public record Detail(ShipmentView shipment,List<ShipmentEvent> events,String addressVerification) {}
 private final ShipmentRepository shipments;private final MerchantAccess merchants;private final TransactionTemplate transaction;
 public ShipmentService(ShipmentRepository shipments,MerchantAccess merchants,org.springframework.transaction.PlatformTransactionManager manager){this.shipments=shipments;this.merchants=merchants;transaction=new TransactionTemplate(manager);}
 private UUID owner(AccountView user){
  if(!user.enabled()||!"MERCHANT".equals(user.role())||user.merchantId()==null)throw new DemoException(HttpStatus.FORBIDDEN,"FORBIDDEN","Cần quyền merchant.");
  merchants.require(user.merchantId());return user.merchantId();
 }
 public List<ShipmentView> list(AccountView user){return shipments.list(owner(user));}
 public Detail detail(AccountView user,UUID id){UUID merchant=owner(user);return new Detail(require(merchant,id,false),shipments.events(id),"UNVERIFIED_DRAFT_INPUT");}
 public ShipmentView create(AccountView user,String key,DraftInput input){
  UUID merchant=owner(user);validKey(key);String hash=hash(input.reference(),input.pickupAddress(),input.recipientName(),input.recipientPhone(),input.deliveryStreet(),input.deliveryWard(),input.deliveryProvince(),input.parcelDescription(),input.weightKg().toPlainString());
  try{return transaction.execute(tx->{
   var receipt=shipments.receipt(merchant,"create",key);if(receipt.isPresent())return replay(merchant,receipt.get(),hash);
   UUID id=UUID.randomUUID();shipments.create(id,merchant,user.id(),input);shipments.receipt(merchant,"create",key,hash,id);return require(merchant,id,false);
  });}catch(DuplicateKeyException e){
   var receipt=shipments.receipt(merchant,"create",key);if(receipt.isPresent())return replay(merchant,receipt.get(),hash);
   throw new DemoException(HttpStatus.CONFLICT,"REFERENCE_EXISTS","Mã tham chiếu đã tồn tại trong tổ chức.");
  }
 }
 public ShipmentView cancel(AccountView user,UUID id,String key,String reason){
  UUID merchant=owner(user);validKey(key);String normalized=reason.trim(),operation="cancel:"+id,hash=hash(normalized);
  return transaction.execute(tx->{
   ShipmentView current=require(merchant,id,true);
   var receipt=shipments.receipt(merchant,operation,key);if(receipt.isPresent())return replay(merchant,receipt.get(),hash);
   if(!"DRAFT".equals(current.status()))throw new DemoException(HttpStatus.CONFLICT,"INVALID_TRANSITION","Chỉ có thể hủy vận đơn DRAFT.");
   shipments.cancel(merchant,id,user.id(),normalized);shipments.receipt(merchant,operation,key,hash,id);return require(merchant,id,false);
  });
 }
 private ShipmentView replay(UUID merchant,ShipmentRepository.Receipt receipt,String hash){
  if(!receipt.hash().equals(hash))throw new DemoException(HttpStatus.CONFLICT,"IDEMPOTENCY_CONFLICT","Mã thao tác đã được dùng với nội dung khác.");
  return require(merchant,receipt.shipmentId(),false);
 }
 private ShipmentView require(UUID merchant,UUID id,boolean lock){return shipments.find(merchant,id,lock).orElseThrow(()->new DemoException(HttpStatus.NOT_FOUND,"SHIPMENT_NOT_FOUND","Không tìm thấy vận đơn."));}
 private void validKey(String key){if(key==null||!key.matches("[A-Za-z0-9._:-]{1,100}"))throw new DemoException(HttpStatus.BAD_REQUEST,"INVALID_OPERATION_KEY","Idempotency-Key không hợp lệ.");}
 private String hash(String...values){try{MessageDigest digest=MessageDigest.getInstance("SHA-256");for(String value:values){byte[] bytes=value.getBytes(StandardCharsets.UTF_8);digest.update(ByteBuffer.allocate(4).putInt(bytes.length).array());digest.update(bytes);}return HexFormat.of().formatHex(digest.digest());}catch(java.security.NoSuchAlgorithmException e){throw new IllegalStateException(e);}}
 public ShipmentRepository.Overview overview(){return shipments.overview();}
 public List<ShipmentRepository.Summary> summaries(){return shipments.summaries();}
}
