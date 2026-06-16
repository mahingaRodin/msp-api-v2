package com.msp.configs;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sqs.SqsClient;

import java.net.URI;

@Configuration
public class AwsConfig {

    @Value("${aws.region:us-east-1}")
    private String region;

    @Value("${aws.localstack.enabled:false}")
    private boolean localstackEnabled;

    @Value("${aws.localstack.endpoint:http://localhost:4566}")
    private String localstackEndpoint;

    private AwsCredentialsProvider credentialsProvider() {
        if (localstackEnabled) {
            return StaticCredentialsProvider.create(AwsBasicCredentials.create("test", "test"));
        }
        return DefaultCredentialsProvider.create();
    }

    @Bean
    public SesClient sesClient() {
        var builder = SesClient.builder()
                .region(Region.of(region))
                .credentialsProvider(credentialsProvider());
        if (localstackEnabled) {
            builder.endpointOverride(URI.create(localstackEndpoint));
        }
        return builder.build();
    }

    @Bean
    public S3Client s3Client() {
        var builder = S3Client.builder()
                .region(Region.of(region))
                .credentialsProvider(credentialsProvider());
        if (localstackEnabled) {
            builder.endpointOverride(URI.create(localstackEndpoint))
                    .serviceConfiguration(S3Configuration.builder()
                            .pathStyleAccessEnabled(true)
                            .build());
        }
        return builder.build();
    }

    @Bean
    public S3Presigner s3Presigner() {
        var builder = S3Presigner.builder()
                .region(Region.of(region))
                .credentialsProvider(credentialsProvider());
        if (localstackEnabled) {
            builder.endpointOverride(URI.create(localstackEndpoint))
                    .serviceConfiguration(S3Configuration.builder()
                            .pathStyleAccessEnabled(true)
                            .build());
        }
        return builder.build();
    }

    @Bean
    public SqsClient sqsClient() {
        var builder = SqsClient.builder()
                .region(Region.of(region))
                .credentialsProvider(credentialsProvider());
        if (localstackEnabled) {
            builder.endpointOverride(URI.create(localstackEndpoint));
        }
        return builder.build();
    }

    @Bean
    public SnsClient snsClient() {
        var builder = SnsClient.builder()
                .region(Region.of(region))
                .credentialsProvider(credentialsProvider());
        if (localstackEnabled) {
            builder.endpointOverride(URI.create(localstackEndpoint));
        }
        return builder.build();
    }
}
