package com.msp.services;

public interface AwsSnsService {
    void publishNotification(String topicArn, String subject, String message);
}
