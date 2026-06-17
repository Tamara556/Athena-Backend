package com.athena.common.storage;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

import java.net.URI;

@AutoConfiguration
@ConditionalOnClass(S3Client.class)
@ConditionalOnProperty(prefix = "athena.storage.s3", name = "enabled", havingValue = "true")
@EnableConfigurationProperties(StorageProperties.class)
public class ImageStorageAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public S3Client s3Client(StorageProperties properties) {
        return S3Client.builder()
                .endpointOverride(URI.create(properties.getEndpoint()))
                .region(Region.of(properties.getRegion()))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(properties.getAccessKey(), properties.getSecretKey())))
                .forcePathStyle(true)
                .build();
    }

    @Bean
    @ConditionalOnMissingBean
    public ImageStorage imageStorage(S3Client s3Client, StorageProperties properties) {
        return new S3ImageStorage(s3Client, properties);
    }
}
