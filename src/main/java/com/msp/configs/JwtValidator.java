package com.msp.configs;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class JwtValidator extends OncePerRequestFilter {

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();
        if (uri == null) {
            return false;
        }
        // Works with or without servlet context-path (/msp)
        return uri.contains("/api/auth")
                || uri.contains("/swagger-ui")
                || uri.contains("/v3/api-docs")
                || uri.contains("/actuator")
                || uri.contains("/webjars")
                || uri.endsWith("/favicon.ico");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        // Prefer Authorization: Bearer <token> (standard)
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (header == null || !header.startsWith("Bearer ")) {
            // No token -> continue (routes protected by Spring Security will still be
            // blocked)
            filterChain.doFilter(request, response);
            return;
        }

        String jwt = header.substring(7).trim();

        try {
            SecretKey key = Keys.hmacShaKeyFor(JwtConstant.JWT_SECRET.getBytes(StandardCharsets.UTF_8));

            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(jwt)
                    .getPayload();

            String email = String.valueOf(claims.get("email"));
            String authorities = String.valueOf(claims.get("authorities"));

            List<GrantedAuthority> auths = AuthorityUtils.commaSeparatedStringToAuthorityList(authorities);

            Authentication auth = new UsernamePasswordAuthenticationToken(email, null, auths);
            SecurityContextHolder.getContext().setAuthentication(auth);

            filterChain.doFilter(request, response);
        } catch (io.jsonwebtoken.JwtException | IllegalArgumentException e) {
            SecurityContextHolder.clearContext();
            writeUnauthorized(response, "Your session is invalid or has expired. Please sign in again.");
        } catch (Exception e) {
            throw e;
        }
    }

    private void writeUnauthorized(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        String json = "{\"timestamp\":\"" + java.time.Instant.now() + "\",\"status\":401,\"error\":\"Unauthorized\",\"message\":\""
                + message.replace("\"", "'") + "\"}";
        response.getWriter().write(json);
    }
}