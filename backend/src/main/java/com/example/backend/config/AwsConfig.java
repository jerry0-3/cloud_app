package com.example.backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cloudwatch.CloudWatchClient;
import software.amazon.awssdk.services.s3.S3Client;

@Configuration
public class AwsConfig {

    @Value("${app.aws-region}")
    private String awsRegion;

    @Value("${app.aws-enabled:false}")
    private boolean awsEnabled;

    @Bean
    public S3Client s3Client() {
        var builder = S3Client.builder().region(Region.of(awsRegion));
        if (!awsEnabled) {
            // Atrapa poświadczeń, aby kontener lokalny startował bez skonfigurowanego AWS CLI
            builder.credentialsProvider(StaticCredentialsProvider.create(
                    AwsBasicCredentials.create("local", "local")));
        }
        return builder.build();
    }

    @Bean
    public CloudWatchClient cloudWatchClient() {
        var builder = CloudWatchClient.builder().region(Region.of(awsRegion));
        if (!awsEnabled) {
            builder.credentialsProvider(StaticCredentialsProvider.create(
                    AwsBasicCredentials.create("local", "local")));
        }
        return builder.build();
    }
}