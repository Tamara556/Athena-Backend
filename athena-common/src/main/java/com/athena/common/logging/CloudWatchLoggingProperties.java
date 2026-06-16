package com.athena.common.logging;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "athena.logging.cloudwatch")
public class CloudWatchLoggingProperties {

    private boolean enabled = false;
    private String endpoint;
    private String region = "us-east-1";
    private String accessKey;
    private String secretKey;
    private String group;
    private String stream;

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public String getEndpoint() { return endpoint; }
    public void setEndpoint(String endpoint) { this.endpoint = endpoint; }
    public String getRegion() { return region; }
    public void setRegion(String region) { this.region = region; }
    public String getAccessKey() { return accessKey; }
    public void setAccessKey(String accessKey) { this.accessKey = accessKey; }
    public String getSecretKey() { return secretKey; }
    public void setSecretKey(String secretKey) { this.secretKey = secretKey; }
    public String getGroup() { return group; }
    public void setGroup(String group) { this.group = group; }
    public String getStream() { return stream; }
    public void setStream(String stream) { this.stream = stream; }
}
