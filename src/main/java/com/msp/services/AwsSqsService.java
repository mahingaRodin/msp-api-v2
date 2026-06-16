package com.msp.services;

public interface AwsSqsService {
    void publishEvent(String queueUrl, Object eventPayload);
}
