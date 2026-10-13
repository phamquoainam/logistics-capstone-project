package com.elogistic.backend.merchant.internal.web;
import com.elogistic.backend.identity.api.AccountView;
import com.elogistic.backend.merchant.api.*;
import org.springframework.context.annotation.Profile;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
@RestController @Profile("demo")
class MerchantController {
 private final MerchantAccess merchants;
 MerchantController(MerchantAccess merchants){this.merchants=merchants;}
 @GetMapping("/api/demo/v1/merchant/me") MerchantView me(@AuthenticationPrincipal AccountView account){return merchants.require(account.merchantId());}
}
