package com.msp.impls;

import tools.jackson.databind.ObjectMapper;
import com.msp.services.AwsSqsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AwsSqsServiceImpl implements AwsSqsService {

    private final SqsClient sqsClient;
    private final ObjectMapper objectMapper;

    @Override
    public void publishEvent(String queueUrl, Object eventPayload) {
        try {
            String jsonBody = objectMapper.writeValueAsString(eventPayload);
            SendMessageRequest.Builder builder = SendMessageRequest.builder()
                    .queueUrl(queueUrl)
                    .messageBody(jsonBody);

            if (queueUrl.endsWith(".fifo")) {
                builder.messageGroupId("msp-pos-group")
                       .messageDeduplicationId(UUID.randomUUID().toString());
            }

            sqsClient.sendMessage(builder.build());
            log.debug("Published message to queue {}: {}", queueUrl, jsonBody);
        } catch (Exception e) {
            log.error("Failed to publish message to queue {}: {}", queueUrl, e.getMessage());
            throw new RuntimeException("SQS Send Failed", e);
        }
    }
}
