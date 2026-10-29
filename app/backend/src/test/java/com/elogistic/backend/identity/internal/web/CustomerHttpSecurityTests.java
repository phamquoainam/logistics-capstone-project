package com.elogistic.backend.identity.internal.web;

import com.elogistic.backend.identity.api.*;
import com.elogistic.backend.identity.api.customer.*;
import com.elogistic.backend.identity.internal.application.port.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.interfaces.*;
import java.time.*;
import java.util.*;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.*;
import org.springframework.mock.web.*;
import tools.jackson.databind.json.JsonMapper;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Isolated HTTP/admission negative controls. Real filter-chain/PG/SMTP evidence is separate. */
class CustomerHttpSecurityTests {
    private static final String ORIGIN="https://portal.example.invalid",EMAIL="fixture@example.invalid",C="c".repeat(43),T="t".repeat(43),R="r".repeat(43);
    private static final UUID ACCOUNT=UUID.randomUUID(),SESSION=UUID.randomUUID(),REQUEST=UUID.randomUUID(),BINDING=UUID.randomUUID(),KEY=UUID.randomUUID();
    private static final Instant NOW=Instant.parse("2026-10-27T00:00:00Z");
    private static KeyPair rsa;
    private final JsonMapper json=JsonMapper.builder().build();
    private IdentityBrowserState browser;private CustomerPreauthBrowserState preauth;private IdentityRateLimiter limiter;
    private IdentitySessionCommands sessions;private CustomerEnrollmentCommands commands;private CustomerRecoveryCommands recoveries;private ProductionCustomerEnrollmentAdmission enrollmentAdmission;
    private ProductionSessionAdmission sessionAdmission;private ProductionAuthenticationHttp authentication;private ProductionCustomerEnrollmentHttp enrollment;
    @BeforeAll static void keys()throws Exception{var generator=KeyPairGenerator.getInstance("RSA");generator.initialize(3072);rsa=generator.generateKeyPair();}
    @BeforeEach void setup(){
        browser=mock(IdentityBrowserState.class);preauth=mock(CustomerPreauthBrowserState.class);limiter=mock(IdentityRateLimiter.class);
        sessions=mock(IdentitySessionCommands.class);commands=mock(CustomerEnrollmentCommands.class);recoveries=mock(CustomerRecoveryCommands.class);
        when(browser.issue(any(),any())).thenReturn(new IdentityBrowserState.Challenge(C,T));when(browser.rebind(any(),any())).thenReturn(new IdentityBrowserState.Challenge(C,T));
        when(preauth.requirePreauthentication(C,T)).thenReturn(BINDING);when(limiter.reserve(any())).thenReturn(success->{});
        sessionAdmission=new ProductionSessionAdmission(browser,limiter,true);enrollmentAdmission=new ProductionCustomerEnrollmentAdmission(preauth,limiter);
        var tokens=new ProductionAccessTokens("https://customer.example.invalid","customer-browser","test",(RSAPrivateKey)rsa.getPrivate(),Map.of("test",(RSAPublicKey)rsa.getPublic()),Clock.fixed(NOW,ZoneOffset.UTC));
        authentication=new ProductionAuthenticationHttp(sessions,sessionAdmission,browser,limiter,tokens,ORIGIN,Clock.fixed(NOW,ZoneOffset.UTC),ProductionAuthenticationHttp.Transport.CUSTOMER);
        enrollment=new ProductionCustomerEnrollmentHttp(commands,recoveries,enrollmentAdmission,ORIGIN);
        when(sessions.loginIdentity(any())).thenAnswer(call->sessionResult());
        when(sessions.refreshIdentity(any())).thenAnswer(call->{sessionAdmission.authorize(SessionAdmission.Operation.REFRESH,REQUEST,SESSION,true);return sessionResult();});
        when(commands.enroll(any())).thenAnswer(call->{var command=(CustomerEnrollmentCommands.Enrollment)call.getArgument(0);var binding=enrollmentAdmission.admit(CustomerEnrollmentAdmission.Operation.ENROLL,command.requestId(),command.email());enrollmentAdmission.requireCurrent(binding);return new CustomerEnrollmentCommands.Accepted(true,command.requestId());});
        when(recoveries.request(any())).thenAnswer(call->{var command=(CustomerRecoveryCommands.Email)call.getArgument(0);var binding=enrollmentAdmission.admit(CustomerEnrollmentAdmission.Operation.RECOVERY_REQUEST,command.requestId(),command.email());enrollmentAdmission.requireCurrent(binding);return new CustomerEnrollmentCommands.Accepted(true,command.requestId());});
        doAnswer(call->{var command=(CustomerRecoveryCommands.Confirmation)call.getArgument(0);var binding=enrollmentAdmission.admit(CustomerEnrollmentAdmission.Operation.RECOVERY_CONFIRM,command.requestId(),command.proofId().toString());enrollmentAdmission.requireCurrent(binding);return null;}).when(recoveries).confirm(any());
    }
    private IdentitySessionCommands.Result sessionResult(){return new IdentitySessionCommands.Result(new IdentitySessionCommands.AccessGrant(SESSION,ACCOUNT,1,NOW,NOW.plusSeconds(900)),new IdentityGrantCommands.Account(ACCOUNT,"synthetic","Synthetic",EMAIL,"ACTIVE",List.of(),1,true),NOW.plusSeconds(28800),R.getBytes(StandardCharsets.US_ASCII));}
    private MockHttpServletRequest request(String route){
        var request=new MockHttpServletRequest(route.equals("csrf")?"GET":"POST","/api/v1/customer-auth/"+route);request.setSecure(true);request.setRemoteAddr("192.0.2.10");
        request.addHeader("Origin",ORIGIN);request.addHeader("X-CSRF-Token",T);request.addHeader("Idempotency-Key",KEY.toString());request.setContentType("application/json");
        request.setCookies(new Cookie("__Host-elogistics.customer.csrf",C),new Cookie("__Host-elogistics.customer.refresh",R));return request;
    }
    private MockHttpServletResponse run(MockHttpServletRequest request)throws Exception{
        var response=new MockHttpServletResponse();response.setHeader("X-Request-Id",REQUEST.toString());response.setHeader("Cache-Control","no-store");
        if(authentication.matches(request))authentication.handle(request,response);else enrollment.handle(request,response);return response;
    }
    private MockHttpServletRequest registration()throws Exception{var request=request("enrollments");request.setContent(json.writeValueAsBytes(Map.of("email",EMAIL,"password","synthetic-customer-password","displayName","Synthetic Customer","phone","+84901234567","consentVersion","S003-2026-10-27")));return request;}
    private void rejected(MockHttpServletResponse response,int status,String code)throws Exception{assertEquals(status,response.getStatus());assertEquals(code,json.readTree(response.getContentAsString()).get("code").asString());assertTrue(response.getHeaders("Set-Cookie").isEmpty());assertFalse(response.getContentAsString().contains(EMAIL));assertFalse(response.getContentAsString().contains("synthetic-customer-password"));}
    @Test void customerCsrfHasExactChallengeAndOnlyCustomerCookie()throws Exception{
        var response=run(request("csrf"));assertEquals(200,response.getStatus());var body=json.readTree(response.getContentAsString());assertEquals(1,body.size());assertEquals(T,body.get("challenge").asString());
        assertTrue(response.getHeader("Set-Cookie").startsWith("__Host-elogistics.customer.csrf="));assertFalse(authentication.matches(new MockHttpServletRequest("GET","/api/v1/auth/csrf")));
    }
    @Test void emailLoginUsesExactCustomerBodyCookiesAndIndependentQuotas()throws Exception{
        var request=request("login");request.setContent(json.writeValueAsBytes(Map.of("email",EMAIL,"password","legacy")));var response=run(request);
        assertEquals(200,response.getStatus());var body=json.readTree(response.getContentAsString());assertEquals(3,body.size());assertEquals(ACCOUNT.toString(),body.get("accountId").asString());assertEquals(NOW.plusSeconds(900).toString(),body.get("expiresAt").asString());assertNotNull(body.get("accessToken"));
        for(String header:response.getHeaders("Set-Cookie")){var cookie=MockCookie.parse(header);assertTrue(cookie.getName().startsWith("__Host-elogistics.customer."));assertTrue(cookie.isHttpOnly());assertTrue(cookie.getSecure());assertEquals("Strict",cookie.getAttribute("SameSite"));assertNull(cookie.getDomain());assertEquals("/",cookie.getPath());}
        verify(limiter).reserve(List.of(new IdentityRateLimiter.Subject(IdentityRateLimiter.Policy.CUSTOMER_LOGIN_EMAIL,EMAIL),new IdentityRateLimiter.Subject(IdentityRateLimiter.Policy.CUSTOMER_LOGIN_IP,"192.0.2.10")));
    }
    @Test void legacyUsernameBodyAndSystemCookiesDoNotBecomeCustomerCredentials()throws Exception{
        var request=request("login");request.setContent(json.writeValueAsBytes(Map.of("username","synthetic","password","legacy")));rejected(run(request),400,"VALIDATION_FAILED");verifyNoInteractions(sessions);
        request=registration();request.setCookies(new Cookie("__Host-elogistics.csrf",C));when(preauth.requirePreauthentication(null,T)).thenThrow(new IdentityCommandFailure(IdentityCommandFailure.Code.CSRF_REJECTED));rejected(run(request),403,"CSRF_REJECTED");
    }
    @Test void refreshUsesCustomerSessionQuotaAndRetainsAcceptedRotationProtocol()throws Exception{
        var response=run(request("refresh"));assertEquals(200,response.getStatus());verify(limiter).reserve(List.of(new IdentityRateLimiter.Subject(IdentityRateLimiter.Policy.CUSTOMER_REFRESH_SESSION,SESSION.toString())));verify(browser,never()).rebind(any(),any());
    }
    @Test void enrollmentUsesServerCsrfBindingAndIgnoresForwardedClientAddress()throws Exception{
        var request=registration();request.addHeader("X-Forwarded-For","198.51.100.10");var response=run(request);assertEquals(202,response.getStatus());assertTrue(json.readTree(response.getContentAsString()).get("accepted").asBoolean());
        var captured=org.mockito.ArgumentCaptor.forClass(CustomerEnrollmentCommands.Enrollment.class);verify(commands).enroll(captured.capture());assertEquals("+84901234567",captured.getValue().phone());
        verify(limiter).reserve(List.of(new IdentityRateLimiter.Subject(IdentityRateLimiter.Policy.CUSTOMER_ENROLL_EMAIL,EMAIL),new IdentityRateLimiter.Subject(IdentityRateLimiter.Policy.CUSTOMER_ENROLL_IP,"192.0.2.10")));
        verify(preauth,atLeast(2)).requirePreauthentication(C,T);
    }
    @Test void legacyEnrollmentBodyRemainsAcceptedWithoutInventingVerifiedPhone()throws Exception{
        var request=request("enrollments");request.setContent(json.writeValueAsBytes(Map.of("email",EMAIL,"password","synthetic-customer-password","displayName","Synthetic Customer","consentVersion","S003-2026-10-27")));
        assertEquals(202,run(request).getStatus());var captured=org.mockito.ArgumentCaptor.forClass(CustomerEnrollmentCommands.Enrollment.class);verify(commands).enroll(captured.capture());assertNull(captured.getValue().phone());
    }
    @Test void insecureMissingDuplicateAndForeignOriginsDenyBeforeApplication()throws Exception{
        var insecure=registration();insecure.setSecure(false);insecure.addHeader("X-Forwarded-Proto","https");rejected(run(insecure),403,"ORIGIN_REJECTED");
        var missing=registration();missing.removeHeader("Origin");rejected(run(missing),403,"ORIGIN_REJECTED");
        var duplicate=registration();duplicate.addHeader("Origin",ORIGIN);rejected(run(duplicate),403,"ORIGIN_REJECTED");
        var foreign=registration();foreign.removeHeader("Origin");foreign.addHeader("Origin","https://other.example.invalid");rejected(run(foreign),403,"ORIGIN_REJECTED");verifyNoInteractions(commands,limiter);
    }
    @Test void missingDuplicateCsrfAndCookiesCannotChooseAlternativeBinding()throws Exception{
        var missing=registration();missing.removeHeader("X-CSRF-Token");rejected(run(missing),403,"CSRF_REJECTED");
        var duplicate=registration();duplicate.addHeader("X-CSRF-Token",T);rejected(run(duplicate),403,"CSRF_REJECTED");
        duplicate=registration();duplicate.setCookies(new Cookie("__Host-elogistics.customer.csrf",C),new Cookie("__Host-elogistics.customer.csrf",T));rejected(run(duplicate),403,"CSRF_REJECTED");verifyNoInteractions(commands);
    }
    @Test void malformedUnknownDuplicateAndOversizedBodiesNeverReachEnrollment()throws Exception{
        for(String body:List.of("{","{\"email\":\"a\",\"email\":\"b\",\"password\":\"x\",\"displayName\":\"x\",\"consentVersion\":\"x\"}","{\"email\":\"a\",\"password\":\"x\",\"displayName\":\"x\",\"consentVersion\":\"x\",\"role\":\"Owner\"}")){
            var request=registration();request.setContent(body.getBytes(StandardCharsets.UTF_8));rejected(run(request),400,"VALIDATION_FAILED");
        }
        var large=registration();large.setContent(new byte[16385]);rejected(run(large),400,"VALIDATION_FAILED");verifyNoInteractions(commands);
    }
    @Test void missingAndMalformedIdempotencyKeysDoNotCreateEffect()throws Exception{
        for(String key:List.of("", "invalid","AAAAAAAA-AAAA-4AAA-8AAA-AAAAAAAAAAAA")){var request=registration();request.removeHeader("Idempotency-Key");if(!key.isEmpty())request.addHeader("Idempotency-Key",key);rejected(run(request),400,"VALIDATION_FAILED");}verifyNoInteractions(commands);
    }
    @Test void quotaOutageAndExhaustionDoNotPublishAcceptance()throws Exception{
        when(limiter.reserve(any())).thenThrow(new IdentityRateLimiter.Unavailable());rejected(run(registration()),503,"SERVICE_UNAVAILABLE");
        doThrow(new IdentityRateLimiter.Exhausted(37)).when(limiter).reserve(any());var response=run(registration());rejected(response,429,"RATE_LIMITED");assertEquals("37",response.getHeader("Retry-After"));
    }
    @Test void preauthScopeCannotBeForgedCapturedReenteredOrUsedOnAnotherThread()throws Exception{
        assertThrows(IdentityCommandFailure.class,()->enrollmentAdmission.admit(CustomerEnrollmentAdmission.Operation.ENROLL,REQUEST,EMAIL));
        CustomerEnrollmentAdmission.Binding captured;
        try(var scope=enrollmentAdmission.enter(REQUEST,C,T,"192.0.2.10")){
            captured=enrollmentAdmission.admit(CustomerEnrollmentAdmission.Operation.ENROLL,REQUEST,EMAIL);
            assertThrows(IdentityCommandFailure.class,()->enrollmentAdmission.requireCurrent(new CustomerEnrollmentAdmission.Binding(BINDING)));
            assertThrows(IdentityCommandFailure.class,()->enrollmentAdmission.enter(REQUEST,C,T,"192.0.2.10"));
            var failure=new java.util.concurrent.atomic.AtomicReference<Throwable>();Thread thread=new Thread(()->{try{enrollmentAdmission.requireCurrent(captured);}catch(Throwable rejected){failure.set(rejected);}});thread.start();thread.join(3000);assertInstanceOf(IdentityCommandFailure.class,failure.get());
        }
        assertThrows(IdentityCommandFailure.class,()->enrollmentAdmission.requireCurrent(captured));
    }
    @Test void finalPreauthRecheckRejectsRevokedOrSubstitutedCsrfBinding()throws Exception{
        try(var scope=enrollmentAdmission.enter(REQUEST,C,T,"192.0.2.10")){
            var binding=enrollmentAdmission.admit(CustomerEnrollmentAdmission.Operation.VERIFY,REQUEST,KEY.toString());
            when(preauth.requirePreauthentication(C,T)).thenReturn(UUID.randomUUID());assertThrows(IdentityCommandFailure.class,()->enrollmentAdmission.requireCurrent(binding));
        }
    }
    @Test void protectedRequestRequiresLiveSameRequestSessionAndMutationCsrf()throws Exception{
        var admission=new ProductionCustomerRequestAdmission(browser,ORIGIN);var request=registration();request.addHeader("Authorization","Bearer synthetic-access");
        try(var scope=admission.enter(REQUEST,"synthetic-access",SESSION,request)){
            admission.authorize(REQUEST,SESSION);verify(browser).verify(C,T,SESSION,false);
            assertThrows(IdentityCommandFailure.class,()->admission.authorize(UUID.randomUUID(),SESSION));assertThrows(IdentityCommandFailure.class,()->admission.authorize(REQUEST,UUID.randomUUID()));
            doThrow(new IdentityCommandFailure(IdentityCommandFailure.Code.CSRF_REJECTED)).when(browser).verify(C,T,SESSION,false);assertThrows(IdentityCommandFailure.class,()->admission.authorize(REQUEST,SESSION));
            assertEquals("CustomerRequest[redacted]",admission.current().toString());
        }
        assertThrows(IdentityCommandFailure.class,admission::current);assertThrows(IdentityCommandFailure.class,()->admission.authorize(REQUEST,SESSION));
    }
    @Test void customerPoliciesHaveExactLimitsAndDistinctLoginReservationSemantics(){
        var policies=Arrays.stream(IdentityRateLimiter.Policy.values()).filter(p->p.name().startsWith("CUSTOMER_")).toList();
        assertEquals(List.of(5,30,30,5,20,3,5,3,20,5,3),policies.stream().map(p->p.limit).toList());
        assertEquals(List.of(900,900,300,3600,3600,3600,900,3600,3600,900,3600),policies.stream().map(p->p.seconds).toList());
        assertEquals(Set.of(IdentityRateLimiter.Policy.LOGIN_ACCOUNT,IdentityRateLimiter.Policy.CUSTOMER_LOGIN_EMAIL),new HashSet<>(Arrays.stream(IdentityRateLimiter.Policy.values()).filter(IdentityRateLimiter.Policy::passwordAttempt).toList()));
    }
    @Test void customerOriginConfigurationHasNoHttpWildcardOrPathFallback(){
        for(String invalid:List.of("http://portal.example.invalid","https://portal.example.invalid/","https://portal.example.invalid/path","https://user@portal.example.invalid","null","*"))
            assertThrows(IllegalArgumentException.class,()->new ProductionCustomerRequestAdmission(browser,invalid));
    }
    @Test void recoveryHttpBindsOnlyEmailAndDedicatedRequestQuotas()throws Exception{
        var request=request("recovery");request.setContent(json.writeValueAsBytes(Map.of("email",EMAIL)));var response=run(request);assertEquals(202,response.getStatus());assertEquals(2,json.readTree(response.getContentAsString()).size());
        verify(limiter).reserve(List.of(new IdentityRateLimiter.Subject(IdentityRateLimiter.Policy.CUSTOMER_RECOVERY_EMAIL,EMAIL),new IdentityRateLimiter.Subject(IdentityRateLimiter.Policy.CUSTOMER_RECOVERY_IP,"192.0.2.10")));verifyNoInteractions(commands);
    }
    @Test void recoveryConfirmationUsesExactInputAndClearsRefreshOnlyAfterCommit()throws Exception{
        var request=request("recovery-confirm");request.setContent(json.writeValueAsBytes(Map.of("proofId",KEY.toString(),"proof",C,"newPassword","synthetic-new-password")));var response=run(request);assertEquals(204,response.getStatus());assertEquals("",response.getContentAsString());
        assertEquals(1,response.getHeaders("Set-Cookie").size());var cookie=MockCookie.parse(response.getHeader("Set-Cookie"));assertEquals("__Host-elogistics.customer.refresh",cookie.getName());assertEquals(0,cookie.getMaxAge());assertTrue(cookie.getSecure());assertTrue(cookie.isHttpOnly());
        verify(limiter).reserve(List.of(new IdentityRateLimiter.Subject(IdentityRateLimiter.Policy.CUSTOMER_RECOVERY_PROOF,KEY.toString())));
        var captor=org.mockito.ArgumentCaptor.forClass(CustomerRecoveryCommands.Confirmation.class);verify(recoveries).confirm(captor.capture());assertEquals("CustomerRecoveryConfirmation[redacted]",captor.getValue().toString());
    }
    @Test void recoveryRejectsActorDestinationAndDuplicateProofFieldInjection()throws Exception{
        var request=request("recovery");request.setContent(json.writeValueAsBytes(Map.of("email",EMAIL,"destination","other@example.invalid","accountId",ACCOUNT.toString())));rejected(run(request),400,"VALIDATION_FAILED");
        request=request("recovery-confirm");request.setContent(("{\"proofId\":\""+KEY+"\",\"proof\":\"a\",\"proof\":\"b\",\"newPassword\":\"synthetic-new-password\"}").getBytes(StandardCharsets.UTF_8));rejected(run(request),400,"VALIDATION_FAILED");verifyNoInteractions(recoveries);
    }
    @Test void recoveryFailureNeverClearsCookiesOrFabricatesCredentialChange()throws Exception{
        var request=request("recovery-confirm");request.setContent(json.writeValueAsBytes(Map.of("proofId",KEY.toString(),"proof",C,"newPassword","synthetic-new-password")));
        doThrow(new IdentityCommandFailure(IdentityCommandFailure.Code.TOKEN_INVALID_OR_EXPIRED)).when(recoveries).confirm(any());rejected(run(request),422,"TOKEN_INVALID_OR_EXPIRED");
        doThrow(new IllegalStateException("PRIVATE_TEST_FAILURE")).when(recoveries).confirm(any());var retry=request("recovery-confirm");retry.setContent(json.writeValueAsBytes(Map.of("proofId",KEY.toString(),"proof",C,"newPassword","synthetic-new-password")));rejected(run(retry),503,"SERVICE_UNAVAILABLE");
    }
}
