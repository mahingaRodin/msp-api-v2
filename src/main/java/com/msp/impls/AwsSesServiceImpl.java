package com.msp.impls;

import com.msp.services.AwsSesService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.model.*;

import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AwsSesServiceImpl implements AwsSesService {

    private final SesClient sesClient;

    @Value("${aws.ses.from-address}")
    private String fromAddress;

    @Override
    public void sendEmail(String to, String subject, String htmlBody) {
        try {
            SendEmailRequest request = SendEmailRequest.builder()
                    .source(fromAddress)
                    .destination(Destination.builder().toAddresses(to).build())
                    .message(Message.builder()
                            .subject(Content.builder().data(subject).build())
                            .body(Body.builder().html(Content.builder().data(htmlBody).build()).build())
                            .build())
                    .build();

            sesClient.sendEmail(request);
            log.info("Email successfully sent via SES to: {}", to);
        } catch (SesException e) {
            log.error("Failed to send email via SES to {}: {}", to, e.awsErrorDetails().errorMessage());
            throw new RuntimeException("Email delivery failed", e);
        }
    }

    @Override
    public void sendTemplatedEmail(String to, String templateName, Map<String, String> templateData) {
        try {
            String templateDataJson = templateData.entrySet().stream()
                    .map(entry -> "\"" + entry.getKey() + "\":\"" + entry.getValue() + "\"")
                    .collect(Collectors.joining(",", "{", "}"));

            SendTemplatedEmailRequest request = SendTemplatedEmailRequest.builder()
                    .source(fromAddress)
                    .destination(Destination.builder().toAddresses(to).build())
                    .template(templateName)
                    .templateData(templateDataJson)
                    .build();

            sesClient.sendTemplatedEmail(request);
            log.info("Templated email '{}' successfully sent via SES to: {}", templateName, to);
        } catch (SesException e) {
            log.error("Failed to send templated email via SES to {}: {}", to, e.awsErrorDetails().errorMessage());
            sendFallbackEmail(to, templateName, templateData);
        }
    }

    private void sendFallbackEmail(String to, String templateName, Map<String, String> templateData) {
        log.warn("Falling back to raw HTML email rendering for template: {}", templateName);
        StringBuilder html = new StringBuilder("<h3>").append(templateName).append("</h3><ul>");
        templateData.forEach((key, val) -> html.append("<li><b>").append(key).append(":</b> ").append(val).append("</li>"));
        html.append("</ul>");
        sendEmail(to, "Notification: " + templateName, html.toString());
    }
}
