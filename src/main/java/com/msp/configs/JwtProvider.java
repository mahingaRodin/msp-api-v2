package com.msp.configs;

import com.msp.models.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

@Service
public class JwtProvider {
    static SecretKey key = Keys.hmacShaKeyFor(JwtConstant.JWT_SECRET.getBytes());

    /**
     * Legacy overload — used by AuthServiceImpl which doesn't have the User entity yet.
     * Does not embed tenantId or userId.
     */
    public String generateToken(Authentication authentication) {
        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
        String roles = populateAuthorities(authorities);

        return Jwts.builder()
                .issuedAt(new Date())
                .expiration(new Date(new Date().getTime() + 86400000))
                .claim("email", authentication.getName())
                .claim("authorities", roles)
                .signWith(key)
                .compact();
    }

    /**
     * Full token — embeds tenantId and userId for tenant-scoped users.
     * ROLE_SUPER_ADMIN users will have tenantId = null in the token.
     */
    public String generateToken(Authentication authentication, User user) {
        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
        String roles = populateAuthorities(authorities);

        return Jwts.builder()
                .issuedAt(new Date())
                .expiration(new Date(new Date().getTime() + 86400000))
                .claim("email",     user.getEmail())
                .claim("authorities", roles)
                .claim("userId",    user.getId() != null ? user.getId().toString() : null)
                .claim("tenantId",  user.getTenantId() != null ? user.getTenantId().toString() : null)
                .claim("role",      user.getRole() != null ? user.getRole().name() : null)
                .signWith(key)
                .compact();
    }

    public String getEmailFromToken(String jwt) {
        jwt = jwt.substring(7);
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(jwt)
                .getPayload();
        return String.valueOf(claims.get("email"));
    }

    private String populateAuthorities(Collection<? extends GrantedAuthority> authorities) {
        Set<String> auths = new HashSet<>();
        for (GrantedAuthority authority : authorities) {
            auths.add(authority.getAuthority());
        }
        return String.join(",", auths);
    }
}
