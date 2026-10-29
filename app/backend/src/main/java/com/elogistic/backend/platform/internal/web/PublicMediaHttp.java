package com.elogistic.backend.platform.internal.web;

import com.elogistic.backend.platform.api.PublicMediaStore;
import org.springframework.context.annotation.Profile;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

/** Fixed public asset inventory. There is deliberately no upload or private key route. */
@RestController @Profile("customer")
public final class PublicMediaHttp {
    private final PublicMediaStore store;
    public PublicMediaHttp(PublicMediaStore store){this.store=store;}
    @GetMapping("/api/v1/public-media/hero-network.svg")
    public ResponseEntity<byte[]> hero(){
        try{return ResponseEntity.ok().contentType(MediaType.parseMediaType("image/svg+xml"))
            .header(HttpHeaders.CACHE_CONTROL,"public, max-age=3600")
            .header("Content-Security-Policy","sandbox; default-src 'none'")
            .header("X-Content-Type-Options","nosniff")
            .body(store.read("hero-network.svg").bytes());}
        catch(RuntimeException unavailable){return ResponseEntity.status(503).cacheControl(CacheControl.noStore()).build();}
    }
}
