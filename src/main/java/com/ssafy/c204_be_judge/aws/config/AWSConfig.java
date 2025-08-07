package com.ssafy.c204_be_judge.aws.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;

@Configuration
@RequiredArgsConstructor
public class AWSConfig {

    private final AWSProperties awsProperties;

    @Bean
    public SqsClient sqsClient() {
        return SqsClient.builder()
                .credentialsProvider(awsProperties::getCredentials)
                .region(Region.of(awsProperties.getRegion()))
                .build();
    }
}
