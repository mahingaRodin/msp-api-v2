package com.msp.configs;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * Runs after JwtValidator. Extracts the tenantId claim from the validated JWT
 * and stores it in TenantContext (ThreadLocal) for the duration of the request.
 *
 * Always clears TenantContext in a finally block — never leaks between requests.
 * ROLE_SUPER_ADMIN users have tenantId = null in their JWT; that is valid.
 *
 * Registered as a @Component so Spring can inject it into SecurityConfig,
 * but excluded from auto-registration via FilterRegistrationBean to avoid double-execution.
 */
@Component
public class TenantContextFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain)
            throws ServletException, IOException {
        try {
            String header = request.getHeader(HttpHeaders.AUTHORIZATION);
            if (header != null && header.startsWith("Bearer ")) {
                String jwt = header.substring(7).trim();
                try {
                    SecretKey key = Keys.hmacShaKeyFor(
                            JwtConstant.JWT_SECRET.getBytes(StandardCharsets.UTF_8));

                    Claims claims = Jwts.parser()
                            .verifyWith(key)
                            .build()
                            .parseSignedClaims(jwt)
                            .getPayload();

                    Object tenantIdClaim = claims.get("tenantId");
                    if (tenantIdClaim != null && !"null".equals(tenantIdClaim.toString())) {
                        TenantContext.setTenantId(UUID.fromString(tenantIdClaim.toString()));
                    }
                    // SUPER_ADMIN has no tenantId — TenantContext stays null, which is correct
                } catch (Exception ignored) {
                    // JWT parsing errors are handled by JwtValidator; we just skip here
                }
            }
            chain.doFilter(request, response);
        } finally {
            // Always clear — prevents ThreadLocal leaks in thread-pool environments
            TenantContext.clear();
        }
    }
}
