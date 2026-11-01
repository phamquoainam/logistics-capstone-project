package com.elogistic.backend.pricing.internal.application.port;
import com.elogistic.backend.pricing.api.PilotQuotes.*;
import java.time.Instant;
import java.util.*;
public interface PilotQuoteStore {
    record Stored(Quote quote,String keyId,byte[] hash){public Stored{hash=hash.clone();}@Override public byte[] hash(){return hash.clone();}@Override public String toString(){return "StoredQuote[redacted]";}}
    void lockOwner(UUID owner);
    Rules rules(Instant now);
    Optional<Stored> byKey(UUID owner,UUID key);
    Optional<Quote> find(UUID owner,UUID id,boolean lock);
    void insert(Quote quote,UUID key,String keyId,byte[] hash);
    void consume(UUID owner,UUID id,UUID shipment,Instant now);
    String canonical(Request input);
    String activeKey();
    byte[] fingerprint(String keyId,UUID owner,String input);
}
