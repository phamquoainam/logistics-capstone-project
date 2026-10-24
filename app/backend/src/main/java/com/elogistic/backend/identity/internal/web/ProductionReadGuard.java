package com.elogistic.backend.identity.internal.web;

import com.elogistic.backend.identity.api.IdentityCurrentSessionCommands;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/** Bearer-only production revision. No cookie/query/Demo principal authentication or forwarded-header trust. */
public final class ProductionReadGuard extends OncePerRequestFilter {
    private final ProductionAccessTokens tokens;
    private final ProductionAccessAdmission admission;
    private final IdentityCurrentSessionCommands sessions;
    private final String origin;
    private ProductionAuthenticationHttp authentication;
    public ProductionReadGuard browser(ProductionAuthenticationHttp authentication){this.authentication=Objects.requireNonNull(authentication);return this;}
    public ProductionReadGuard(ProductionAccessTokens tokens,ProductionAccessAdmission admission,IdentityCurrentSessionCommands sessions,String origin) {
        this.tokens=Objects.requireNonNull(tokens);this.admission=Objects.requireNonNull(admission);this.sessions=Objects.requireNonNull(sessions);
        try {var uri=java.net.URI.create(origin);if(!"https".equals(uri.getScheme()) || uri.getHost()==null || uri.getRawUserInfo()!=null
                || uri.getRawQuery()!=null || uri.getRawFragment()!=null || !(uri.getRawPath()==null || uri.getRawPath().isEmpty()))throw new IllegalArgumentException();}
        catch(Exception invalid){throw new IllegalArgumentException("IDENTITY_ORIGIN_CONFIGURATION_INVALID");}
        this.origin=origin;
    }
    @Override protected void doFilterInternal(HttpServletRequest request,HttpServletResponse response,FilterChain chain) throws ServletException,IOException {
        UUID requestId=UUID.randomUUID();response.setHeader("Cache-Control","no-store");response.setHeader("X-Request-Id",requestId.toString());
        response.setHeader("X-Content-Type-Options","nosniff");response.setHeader("Referrer-Policy","no-referrer");
        if(authentication!=null && authentication.matches(request)){authentication.handle(request,response);return;}
        try {
            if(!request.isSecure() || !allowedMethod(request)){deny(response,401,"SESSION_INVALID");return;}
            var origins=Collections.list(request.getHeaders("Origin"));
            if(origins.size()>1 || (!origins.isEmpty() && !origin.equals(origins.getFirst()))){deny(response,401,"SESSION_INVALID");return;}
            var headers=Collections.list(request.getHeaders("Authorization"));
            if(headers.size()!=1 || !headers.getFirst().regionMatches(true,0,"Bearer ",0,7) || request.getParameter("access_token")!=null){deny(response,401,"SESSION_INVALID");return;}
            var verified=tokens.verify(headers.getFirst().substring(7));
            try(var scope=admission.enter(requestId,verified)) {
                sessions.getIdentityMe(requestId); // current persisted authority before Spring/controller admission
                var context=SecurityContextHolder.createEmptyContext();
                context.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(verified,"",List.of()));SecurityContextHolder.setContext(context);
                chain.doFilter(request,response);
            } finally {SecurityContextHolder.clearContext();}
        } catch(com.elogistic.backend.identity.api.IdentityCommandFailure rejected) {if(!response.isCommitted())deny(response,401,"SESSION_INVALID");}
        catch(RuntimeException rejected){if(!response.isCommitted())deny(response,401,"SESSION_INVALID");}
        catch(Exception rejected){throw new ServletException("IDENTITY_REQUEST_FAILED");}
    }
    private static boolean allowedMethod(HttpServletRequest request) {
        if("GET".equals(request.getMethod()))return true;
        String path=request.getRequestURI().substring(request.getContextPath().length());
        return ("PUT".equals(request.getMethod()) && path.matches("/api/v1/admin/users/[0-9a-fA-F-]{36}/grants"))
            || ("POST".equals(request.getMethod()) && path.matches("/api/v1/admin/users/[0-9a-fA-F-]{36}/(disable|reactivate)"));
    }
    public static void deny(HttpServletResponse response,int status,String code) throws IOException {
        response.setStatus(status);response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"code\":\""+code+"\",\"message\":\"Request rejected.\",\"requestId\":\""+response.getHeader("X-Request-Id")+"\"}");
    }
}
