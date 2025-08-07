package com.ssafy.c204_be_judge.aws.config;

import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.ConstructorBinding;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;

@Getter
@ConfigurationProperties(prefix = "aws")
public class AWSProperties {

    private final String accessKey;
    private final String secretKey;
    private final String region;
    private final String s3BucketName;
    private final String sqsQueueName;

    @ConstructorBinding
    public AWSProperties(String accessKey, String secretKey, String region, String s3BucketName, String sqsQueueName) {
        this.accessKey = accessKey;
        this.secretKey = secretKey;
        this.region = region;
        this.s3BucketName = s3BucketName;
        this.sqsQueueName = sqsQueueName;
    }

    public AwsBasicCredentials getCredentials() {
        return AwsBasicCredentials.create(getAccessKey(), getSecretKey());
    }
}
