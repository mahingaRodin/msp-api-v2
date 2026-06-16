package com.msp.services;

import java.util.Map;

public interface AwsSesService {
    void sendEmail(String to, String subject, String htmlBody);
    void sendTemplatedEmail(String to, String templateName, Map<String, String> templateData);
}
