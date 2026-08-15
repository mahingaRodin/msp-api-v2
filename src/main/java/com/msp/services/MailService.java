package com.msp.services;

public interface MailService {
    void send(String to, String subject, String htmlBody);
}
