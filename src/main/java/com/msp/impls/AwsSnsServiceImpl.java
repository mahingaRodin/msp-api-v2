package com.msp.impls;

import com.msp.services.AwsSnsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.PublishRequest;

@Service
@RequiredArgsConstructor
@Slf4j
public class AwsSnsServiceImpl implements AwsSnsService {

    private final SnsClient snsClient;

    @Override
    public void publishNotification(String topicArn, String subject, String message) {
        try {
            PublishRequest request = PublishRequest.builder()
                    .topicArn(topicArn)
                    .subject(subject)
                    .message(message)
                    .build();

            snsClient.publish(request);
            log.info("Published SNS notification to topic: {}", topicArn);
        } catch (Exception e) {
            log.error("Failed to publish SNS to topic {}: {}", topicArn, e.getMessage());
            throw new RuntimeException("SNS Publish Failed", e);
        }
    }
}
