package com.elogistic.backend.platform.internal.web;
import java.util.Map;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.*;
@RestController @Profile("demo")
class DemoHealth {@GetMapping("/api/demo/v1/health") Map<String,String> health(){return Map.of("status","UP","scope","local-demo");}}
