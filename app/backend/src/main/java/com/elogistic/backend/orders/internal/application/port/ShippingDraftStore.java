package com.elogistic.backend.orders.internal.application.port;
import com.elogistic.backend.orders.api.ShippingDrafts.*;
import java.util.*;
public interface ShippingDraftStore {
    record StoredReceipt(UUID target,long originalVersion,long resultVersion,String keyId,byte[] hash){public StoredReceipt{hash=hash.clone();}@Override public byte[] hash(){return hash.clone();}@Override public String toString(){return "StoredReceipt[redacted]";}}
    void lockOwner(UUID owner);
    Optional<Draft> find(UUID owner,UUID id);
    Draft version(UUID owner,UUID id,long version);
    List<Draft> list(UUID owner,int page,String search);
    Optional<StoredReceipt> receipt(UUID owner,String operation,UUID key);
    void save(Draft before,Draft after,String operation,UUID request);
    void receipt(UUID owner,String operation,UUID key,StoredReceipt receipt,java.time.Instant now);
    String canonical(Input input);
}
