package com.msp.configs;

import com.msp.enums.EBusinessStatus;
import com.msp.enums.EStoreStatus;
import com.msp.enums.ESubscriptionTier;
import com.msp.models.Business;
import com.msp.repositories.BusinessRepository;
import com.msp.repositories.StoreRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class SubscriptionAccessFilter extends OncePerRequestFilter {

    private final BusinessRepository businessRepository;
    private final StoreRepository storeRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain)
            throws ServletException, IOException {
        UUID tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            chain.doFilter(request, response);
            return;
        }

        String path = request.getRequestURI();
        if (isAllowedWhenBlocked(path)) {
            chain.doFilter(request, response);
            return;
        }

        Business business = businessRepository.findByTenantId(tenantId).orElse(null);
        if (business == null) {
            chain.doFilter(request, response);
            return;
        }

        if (business.getSubscriptionTier() == ESubscriptionTier.FREE_TRIAL
                && business.getTrialEndsAt() != null
                && business.getTrialEndsAt().isBefore(LocalDateTime.now())
                && business.getStatus() == EBusinessStatus.ACTIVE) {
            business.setStatus(EBusinessStatus.SUSPENDED);
            businessRepository.save(business);
            storeRepository.findByTenantId(tenantId, PageRequest.of(0, 200))
                    .forEach(store -> {
                        if (store.getStatus() != EStoreStatus.BLOCKED) {
                            store.setStatus(EStoreStatus.BLOCKED);
                            storeRepository.save(store);
                        }
                    });
        }

        if (business.getStatus() == EBusinessStatus.SUSPENDED
                || business.getStatus() == EBusinessStatus.DEPROVISIONED) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write(
                    "{\"status\":403,\"error\":\"SUBSCRIPTION_BLOCKED\","
                            + "\"message\":\"Your trial or subscription has ended. Upgrade your plan to continue.\"}");
            return;
        }

        chain.doFilter(request, response);
    }

    private boolean isAllowedWhenBlocked(String path) {
        if (path == null) return true;
        return path.startsWith("/api/auth")
                || path.startsWith("/api/tenant/me")
                || path.startsWith("/api/profile")
                || path.startsWith("/actuator")
                || path.startsWith("/v3/api-docs")
                || path.startsWith("/swagger");
    }
}
