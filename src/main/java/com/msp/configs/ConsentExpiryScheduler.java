package com.msp.configs;

import com.msp.enums.EConsentStatus;
import com.msp.models.AdminConsentRequest;
import com.msp.repositories.AdminConsentRequestRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Scheduled job that automatically expires AdminConsentRequest windows
 * whose expiresAt timestamp has passed.
 * Runs every 5 minutes.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ConsentExpiryScheduler {

    private final AdminConsentRequestRepository consentRepo;

    @Scheduled(fixedDelay = 300_000)   // every 5 minutes
    @Transactional
    public void expireStaleConsents() {
        LocalDateTime now = LocalDateTime.now();
        int page = 0;
        int expired = 0;

        Page<AdminConsentRequest> batch;
        do {
            // Find ACTIVE consents whose window has passed
            batch = consentRepo.findAll(PageRequest.of(page, 50));
            for (AdminConsentRequest req : batch.getContent()) {
                if (req.getStatus() == EConsentStatus.ACTIVE
                        && req.getExpiresAt() != null
                        && req.getExpiresAt().isBefore(now)) {
                    req.setStatus(EConsentStatus.EXPIRED);
                    consentRepo.save(req);
                    expired++;
                }
            }
            page++;
        } while (batch.hasNext());

        if (expired > 0) {
            log.info("ConsentExpiryScheduler: expired {} consent window(s)", expired);
        }
    }
}
