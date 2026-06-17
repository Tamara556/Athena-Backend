package com.athena.common.storage;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@NoArgsConstructor
@AllArgsConstructor
@ConfigurationProperties(prefix = "athena.storage.s3")
public class StorageProperties {

    private boolean enabled = false;
    private String endpoint = "http://localhost:4566";
    private String region = "us-east-1";
    private String accessKey = "test";
    private String secretKey = "test";
    private String bucket = "athena-user-images";
}
