package com.msp.consumers;

import tools.jackson.databind.ObjectMapper;
import com.msp.events.ConsentEventPayload;
import com.msp.events.RegistrationEventPayload;
import com.msp.services.AwsSesService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.*;

import java.util.List;

@Component
@ConditionalOnProperty(name = "aws.messaging.enabled", havingValue = "true")
@RequiredArgsConstructor
@Slf4j
public class SqsConsumer {

    private final SqsClient sqsClient;
    private final AwsSesService sesService;
    private final ObjectMapper objectMapper;

    @Value("${aws.sqs.notification-queue-url}")
    private String notificationQueueUrl;

    @Scheduled(fixedDelay = 1000) // Polls queue every 1 second
    public void consumeNotificationEvents() {
        try {
            ReceiveMessageRequest receiveRequest = ReceiveMessageRequest.builder()
                    .queueUrl(notificationQueueUrl)
                    .maxNumberOfMessages(10)
                    .waitTimeSeconds(5) // Long-polling
                    .build();

            ReceiveMessageResponse response = sqsClient.receiveMessage(receiveRequest);
            List<Message> messages = response.messages();

            for (Message msg : messages) {
                try {
                    processMessagePayload(msg.body());

                    sqsClient.deleteMessage(DeleteMessageRequest.builder()
                            .queueUrl(notificationQueueUrl)
                            .receiptHandle(msg.receiptHandle())
                            .build());
                } catch (Exception ex) {
                    log.error("Failed to process message {}: {}", msg.messageId(), ex.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("Error reading from SQS: {}", e.getMessage());
        }
    }

    private void processMessagePayload(String jsonBody) throws Exception {
        if (jsonBody.contains("\"eventType\"")) {
            RegistrationEventPayload payload = objectMapper.readValue(jsonBody, RegistrationEventPayload.class);
            handleRegistrationEmail(payload);
        } else if (jsonBody.contains("\"consentRequestId\"")) {
            ConsentEventPayload payload = objectMapper.readValue(jsonBody, ConsentEventPayload.class);
            handleConsentEmail(payload);
        } else {
            log.warn("Unknown event format received: {}", jsonBody);
        }
    }

    private void handleRegistrationEmail(RegistrationEventPayload payload) {
        String to = payload.getOwnerEmail();
        String subject;
        String htmlBody;

        switch (payload.getEventType()) {
            case "REGISTRATION_SUBMITTED" -> {
                subject = "Business Registration Received";
                htmlBody = String.format("<h2>Hello %s,</h2><p>Your registration for <b>%s</b> is received and pending review.</p>",
                        payload.getOwnerFirstName(), payload.getBusinessName());
            }
            case "REGISTRATION_REJECTED" -> {
                subject = "Business Registration Declined";
                htmlBody = String.format("<h2>Hello %s,</h2><p>We regret to inform you that your registration for <b>%s</b> was declined. Reason: %s</p>",
                        payload.getOwnerFirstName(), payload.getBusinessName(), payload.getContent());
            }
            case "TENANT_PROVISIONED" -> {
                subject = "Business Portal Approved!";
                htmlBody = String.format("<h2>Congratulations %s!</h2><p>Your business <b>%s</b> is now active.</p><p>Credentials: %s</p>",
                        payload.getOwnerFirstName(), payload.getBusinessName(), payload.getContent());
            }
            default -> {
                subject = "MSP Notification";
                htmlBody = "<p>" + payload.getContent() + "</p>";
            }
        }

        sesService.sendEmail(to, subject, htmlBody);
    }

    private void handleConsentEmail(ConsentEventPayload payload) {
        String subject = "Action Required: System Admin Data Access Consent Request";
        String htmlBody = String.format(
                "<h2>Consent Requested</h2><p>Admin <b>%s</b> is requesting access to your tenant data for <b>%d hours</b>.</p><p>Reason: %s</p>",
                payload.getAdminEmail(), payload.getDurationHours(), payload.getReason()
        );
        sesService.sendEmail(payload.getOwnerEmail(), subject, htmlBody);
    }
}
