package com.elogistic.backend.identity.internal.web;
import com.elogistic.backend.identity.api.*;
import com.elogistic.backend.identity.internal.application.port.*;
import com.elogistic.backend.identity.internal.domain.*;
import jakarta.servlet.http.*;
import java.io.*;
import java.time.*;
import java.util.*;
import java.nio.charset.StandardCharsets;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.core.StreamReadFeature;

/** Four exact accepted browser operations, isolated from Bearer-only administration. */
public final class ProductionAuthenticationHttp {
    private static final String CSRF="__Host-elogistics.csrf",REFRESH="__Host-elogistics.refresh";
    private static final JsonMapper JSON=JsonMapper.builder().enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION).build();
    private final IdentitySessionCommands sessions;private final ProductionSessionAdmission admission;
    private final IdentityBrowserState browser;private final IdentityRateLimiter limiter;private final ProductionAccessTokens tokens;
    private final String origin;private final Clock clock;
    public ProductionAuthenticationHttp(IdentitySessionCommands sessions,ProductionSessionAdmission admission,IdentityBrowserState browser,IdentityRateLimiter limiter,ProductionAccessTokens tokens,String origin,Clock clock){
        this.sessions=sessions;this.admission=admission;this.browser=browser;this.limiter=limiter;this.tokens=tokens;this.origin=origin;this.clock=clock;
    }
    public boolean matches(HttpServletRequest r){String p=r.getRequestURI().substring(r.getContextPath().length());return (r.getMethod().equals("GET")&&p.equals("/api/v1/auth/csrf"))||(r.getMethod().equals("POST")&&Set.of("/api/v1/auth/login","/api/v1/auth/refresh","/api/v1/auth/logout").contains(p));}
    public void handle(HttpServletRequest request,HttpServletResponse response)throws IOException {
        String path=request.getRequestURI();boolean csrf=path.endsWith("/csrf"),login=path.endsWith("/login"),logout=path.endsWith("/logout");
        try {
            var origins=Collections.list(request.getHeaders("Origin"));
            if(!request.isSecure()||origins.size()>1||(!csrf&&origins.size()!=1)||(!origins.isEmpty()&&!origin.equals(origins.getFirst()))){deny(response,403,"ORIGIN_REJECTED");return;}
            // Forwarded/X-Forwarded-* never override TLS or remote address; no proxy trust is configured.
            String cookie=cookie(request,CSRF),refresh=cookie(request,REFRESH);
            if(csrf){var c=browser.issue(cookie,refresh);setCookie(response,CSRF,c.cookie(),900);json(response,Map.of("csrfToken",c.token()));return;}
            UUID requestId=UUID.fromString(response.getHeader("X-Request-Id"));String challenge=single(request,"X-CSRF-Token");
            try(var ignored=admission.enter(requestId,cookie,challenge)){
                if(login){
                    admission.authorize(SessionAdmission.Operation.LOGIN,requestId,null,false);
                    var input=credentials(request);String username=input.get("username").asString(),password=input.get("password").asString();
                    try{new Username(username);PasswordPolicy.validateLogin(password);}catch(DomainViolation e){throw new IdentityCommandFailure(IdentityCommandFailure.Code.VALIDATION_FAILED);}
                    String ip=clientAddress(request);
                    var reservation=limiter.reserve(List.of(new IdentityRateLimiter.Subject(IdentityRateLimiter.Policy.LOGIN_ACCOUNT,username),new IdentityRateLimiter.Subject(IdentityRateLimiter.Policy.LOGIN_IP,ip)));
                    IdentitySessionCommands.Result result;
                    try{result=sessions.loginIdentity(new IdentitySessionCommands.Login(username,password,requestId));}
                    catch(IdentityCommandFailure rejected){if(rejected.code()==IdentityCommandFailure.Code.INVALID_CREDENTIALS)reservation.finishLogin(false);throw rejected;}
                    try(result){reservation.finishLogin(true);success(response,result,browser.rebind(cookie,result.access().sessionId()));}
                }else{
                    byte[] raw=refresh==null?null:refresh.getBytes(StandardCharsets.US_ASCII);
                    try(var command=new IdentitySessionCommands.Token(raw,requestId)){
                        if(logout){sessions.logoutIdentity(command);var c=browser.rebind(cookie,null);setCookie(response,REFRESH,"",0);setCookie(response,CSRF,c.cookie(),900);response.setStatus(204);}
                        else{try(var result=sessions.refreshIdentity(command)){success(response,result,null);setCookie(response,CSRF,cookie,900);}}
                    }finally{if(raw!=null)Arrays.fill(raw,(byte)0);}
                }
            }
        }catch(IdentityRateLimiter.Exhausted e){response.setHeader("Retry-After",Integer.toString(e.retryAfter));deny(response,429,"RATE_LIMITED");}
        catch(IdentityCommandFailure e){
            String code=e.code().name();int status;
            if(csrf){status=403;code="ORIGIN_REJECTED";}
            else if(Set.of("CSRF_REJECTED","ORIGIN_REJECTED").contains(code))status=403;
            else if(login&&code.equals("VALIDATION_FAILED"))status=400;
            else if(login){status=401;code="INVALID_CREDENTIALS";}
            else if(logout){status=403;code="CSRF_REJECTED";}
            else{status=401;if(!code.equals("REFRESH_REPLAY"))code="SESSION_INVALID";}
            deny(response,status,code);
        }catch(Exception e){deny(response,503,"SERVICE_UNAVAILABLE");}
    }
    private static tools.jackson.databind.JsonNode credentials(HttpServletRequest request)throws IOException{
        try{
            if(request.getContentType()==null||!request.getContentType().split(";",2)[0].equalsIgnoreCase("application/json"))throw new IllegalArgumentException();
            byte[] bytes=request.getInputStream().readNBytes(16385);if(bytes.length>16384)throw new IllegalArgumentException();var value=JSON.readTree(bytes);
            if(value==null||!value.isObject()||value.size()!=2||value.get("username")==null||!value.get("username").isString()||value.get("password")==null||!value.get("password").isString())throw new IllegalArgumentException();return value;
        }catch(Exception e){throw new IdentityCommandFailure(IdentityCommandFailure.Code.VALIDATION_FAILED);}
    }
    private void success(HttpServletResponse response,IdentitySessionCommands.Result result,IdentityBrowserState.Challenge csrf)throws IOException{
        // Construct signing/body BEFORE writing cookies. Never publish an incomplete credential pair.
        String access;try{access=tokens.issue(result.access());}catch(IdentityCommandFailure invalid){throw new IdentityRateLimiter.Unavailable();}long remaining=Duration.between(clock.instant(),result.absoluteExpiresAt()).getSeconds();if(remaining<=0)throw new IdentityRateLimiter.Unavailable();
        int seconds=(int)(result.access().expiresAt().getEpochSecond()-result.access().issuedAt().getEpochSecond());
        String body=JSON.writeValueAsString(Map.of("accessToken",access,"tokenType","Bearer","expiresIn",seconds,"sessionExpiresAt",result.absoluteExpiresAt().toString(),"user",result.user()));
        byte[] raw=result.refreshToken();try{setCookie(response,REFRESH,new String(raw,StandardCharsets.US_ASCII),Math.min(28800,remaining));}finally{Arrays.fill(raw,(byte)0);}
        if(csrf!=null)setCookie(response,CSRF,csrf.cookie(),900);
        response.setStatus(200);response.setContentType("application/json;charset=UTF-8");response.getWriter().write(body);
    }
    private static String clientAddress(HttpServletRequest request){
        String raw=request.getRemoteAddr();
        if(raw==null||raw.length()>128||!(raw.matches("[0-9]{1,3}(\\.[0-9]{1,3}){3}")||(raw.contains(":")&&raw.matches("[0-9a-fA-F:.%]+"))))throw new IdentityRateLimiter.Unavailable();
        try{return java.net.InetAddress.getByName(raw).getHostAddress();}catch(java.net.UnknownHostException invalid){throw new IdentityRateLimiter.Unavailable();}
    }
    private static String cookie(HttpServletRequest request,String name){String found=null;int count=0;Cookie[] cookies=request.getCookies();if(cookies!=null)for(Cookie c:cookies)if(c.getName().equals(name)){found=c.getValue();count++;}if(count>1)throw new IdentityCommandFailure(IdentityCommandFailure.Code.CSRF_REJECTED);return found;}
    private static String single(HttpServletRequest request,String name){var values=Collections.list(request.getHeaders(name));if(values.size()!=1)throw new IdentityCommandFailure(IdentityCommandFailure.Code.CSRF_REJECTED);return values.getFirst();}
    private static void setCookie(HttpServletResponse response,String name,String value,long age){response.addHeader("Set-Cookie",name+"="+value+"; Path=/; Secure; HttpOnly; SameSite=Strict; Max-Age="+age);}
    private static void deny(HttpServletResponse r,int s,String code)throws IOException{if(!r.isCommitted())ProductionReadGuard.deny(r,s,code);}
    private static void json(HttpServletResponse response,Object value)throws IOException{response.setStatus(200);response.setContentType("application/json;charset=UTF-8");response.getWriter().write(JSON.writeValueAsString(value));}
}
