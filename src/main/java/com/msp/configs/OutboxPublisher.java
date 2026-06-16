package com.msp.configs;

import com.msp.enums.EOutboxStatus;
import com.msp.models.OutboxEvent;
import com.msp.repositories.OutboxEventRepository;
import com.msp.services.AwsSqsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@ConditionalOnProperty(name = "aws.messaging.enabled", havingValue = "true")
@RequiredArgsConstructor
@Slf4j
public class OutboxPublisher {

    private final OutboxEventRepository outboxRepo;
    private final AwsSqsService sqsService;

    @Scheduled(fixedDelay = 5000) // Polls every 5 seconds
    public void publishPendingEvents() {
        List<OutboxEvent> pendingEvents = outboxRepo.findByStatusOrderByCreatedAtAsc(
                EOutboxStatus.PENDING, PageRequest.of(0, 50));

        if (pendingEvents.isEmpty()) {
            return;
        }

        log.debug("Found {} pending outbox events to publish", pendingEvents.size());

        for (OutboxEvent event : pendingEvents) {
            try {
                sqsService.publishEvent(event.getQueueUrl(), event.getPayload());

                event.setStatus(EOutboxStatus.PROCESSED);
                event.setProcessedAt(LocalDateTime.now());
                outboxRepo.save(event);
            } catch (Exception e) {
                log.error("Failed to publish outbox event {}: {}", event.getId(), e.getMessage());
                event.setRetryCount(event.getRetryCount() + 1);
                event.setErrorMessage(e.getMessage());
                if (event.getRetryCount() >= 5) {
                    event.setStatus(EOutboxStatus.FAILED);
                }
                outboxRepo.save(event);
            }
        }
    }
}
