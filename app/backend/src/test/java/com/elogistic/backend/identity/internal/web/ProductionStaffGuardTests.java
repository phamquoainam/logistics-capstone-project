package com.elogistic.backend.identity.internal.web;

import com.elogistic.backend.identity.api.staff.*;
import com.elogistic.backend.identity.internal.application.port.*;
import com.elogistic.backend.identity.internal.infrastructure.JdbcStaffInvitationActivation;
import java.time.Clock;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ProductionStaffGuardTests {
    private static final String ORIGIN="https://127.0.0.1:5173";
    @Test void disabledBoundaryFailsClosedBeforeAuthenticationOrController()throws Exception {
        var chain=new MockFilterChain();var response=new MockHttpServletResponse();
        ProductionStaffGuard.disabled().doFilter(request("GET","/api/v1/staff-auth/csrf"),response,chain);
        assertEquals(503,response.getStatus());assertTrue(response.getContentAsString().contains("STAFF_UNAVAILABLE"));
        assertEquals("no-store",response.getHeader("Cache-Control"));assertNull(chain.getRequest());
    }
    @Test void invitationAcceptRequiresTlsOriginStaffCsrfExactBodyAndRequestKey()throws Exception {
        var browser=mock(IdentityBrowserState.class);var activation=mock(JdbcStaffInvitationActivation.class);
        var limiter=mock(IdentityRateLimiter.class);var admission=mock(StaffAdmission.class);
        var requests=new ProductionStaffRequestAdmission(browser,ORIGIN);
        var guard=new ProductionStaffGuard(mock(ProductionAccessTokens.class),requests,admission,
            mock(ProductionAuthenticationHttp.class),activation,browser,limiter,ORIGIN,"TEST",()->true);
        var request=request("POST","/api/v1/staff-auth/invitations/accept");
        UUID proof=UUID.randomUUID(),key=UUID.randomUUID();
        request.addHeader("Origin",ORIGIN);request.addHeader("X-CSRF-Token","test-challenge");
        request.addHeader("Idempotency-Key",key.toString());request.setContentType("application/json");
        request.setContent(("{\"proofId\":\""+proof+"\",\"token\":\""+"a".repeat(43)+"\",\"password\":\"SyntheticPassword!9\"}").getBytes(java.nio.charset.StandardCharsets.UTF_8));
        var response=new MockHttpServletResponse();guard.doFilter(request,response,new MockFilterChain());
        assertEquals(204,response.getStatus());verify(browser).verify(null,"test-challenge",null,true);
        verify(activation).accept(eq(proof),any(byte[].class),eq("SyntheticPassword!9"),eq(key),any(UUID.class));
        assertFalse(response.getContentAsString().contains("SyntheticPassword"));
        reset(activation);
        var insecure=request("POST","/api/v1/staff-auth/invitations/accept");insecure.setSecure(false);
        response=new MockHttpServletResponse();guard.doFilter(insecure,response,new MockFilterChain());
        assertEquals(403,response.getStatus());verifyNoInteractions(activation);
        var query=request("POST","/api/v1/staff-auth/invitations/accept");query.addParameter("token","secret");query.addHeader("Origin",ORIGIN);
        response=new MockHttpServletResponse();guard.doFilter(query,response,new MockFilterChain());
        assertEquals(400,response.getStatus());verifyNoInteractions(activation);
    }
    @Test void runtimeEnvironmentFailureDoesNotIssueCsrfOrReadTokens()throws Exception {
        var auth=mock(ProductionAuthenticationHttp.class);var tokens=mock(ProductionAccessTokens.class);
        var browser=mock(IdentityBrowserState.class);
        var guard=new ProductionStaffGuard(tokens,new ProductionStaffRequestAdmission(browser,ORIGIN),mock(StaffAdmission.class),auth,
            mock(JdbcStaffInvitationActivation.class),browser,mock(IdentityRateLimiter.class),ORIGIN,"TEST",()->false);
        var response=new MockHttpServletResponse();guard.doFilter(request("GET","/api/v1/staff-auth/csrf"),response,new MockFilterChain());
        assertEquals(503,response.getStatus());verifyNoInteractions(auth,tokens,browser);
    }
    private static MockHttpServletRequest request(String method,String path){var r=new MockHttpServletRequest(method,path);r.setSecure(true);r.setRemoteAddr("127.0.0.1");return r;}
}
