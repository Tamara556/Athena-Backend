package com.athena.common.logging;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.ThrowableProxyUtil;
import ch.qos.logback.core.UnsynchronizedAppenderBase;
import lombok.Setter;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cloudwatchlogs.CloudWatchLogsClient;
import software.amazon.awssdk.services.cloudwatchlogs.model.CreateLogGroupRequest;
import software.amazon.awssdk.services.cloudwatchlogs.model.CreateLogStreamRequest;
import software.amazon.awssdk.services.cloudwatchlogs.model.InputLogEvent;
import software.amazon.awssdk.services.cloudwatchlogs.model.PutLogEventsRequest;
import software.amazon.awssdk.services.cloudwatchlogs.model.ResourceAlreadyExistsException;
import software.amazon.awssdk.services.cloudwatchlogs.model.ResourceNotFoundException;

import java.net.URI;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class CloudWatchLogbackAppender extends UnsynchronizedAppenderBase<ILoggingEvent> {

    @Setter private String logGroup;
    @Setter private String logStream;
    @Setter private String region = "us-east-1";
    @Setter private String endpoint;
    @Setter private String accessKey;
    @Setter private String secretKey;

    private CloudWatchLogsClient client;
    private BlockingQueue<InputLogEvent> queue;
    private ScheduledExecutorService scheduler;
    private volatile boolean ready;

    @Override
    public void start() {
        try {
            var builder = CloudWatchLogsClient.builder()
                    .region(Region.of(region == null || region.isBlank() ? "us-east-1" : region));
            if (endpoint != null && !endpoint.isBlank()) {
                builder.endpointOverride(URI.create(endpoint));
            }
            if (accessKey != null && !accessKey.isBlank()) {
                builder.credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKey, secretKey == null ? "" : secretKey)));
            }
            client = builder.build();
            ensureGroupAndStream();

            queue = new LinkedBlockingQueue<>(10_000);
            scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "cloudwatch-logs");
                t.setDaemon(true);
                return t;
            });
            scheduler.scheduleWithFixedDelay(this::flush, 2, 2, TimeUnit.SECONDS);
            ready = true;
            super.start();
            addInfo("CloudWatch appender started (group=" + logGroup + " stream=" + logStream
                    + " endpoint=" + endpoint + ")");
        } catch (Exception ex) {
            addWarn("CloudWatch appender failed to start; logs will not ship to CloudWatch: " + ex.getMessage());
        }
    }

    @Override
    protected void append(ILoggingEvent event) {
        if (!ready) {
            return;
        }
        String logger = event.getLoggerName();
        if (logger != null && (logger.startsWith("software.amazon.awssdk")
                || logger.startsWith("io.netty")
                || logger.startsWith("org.apache.http")
                || logger.startsWith("org.apache.hc"))) {
            return;
        }
        try {
            queue.offer(InputLogEvent.builder()
                    .timestamp(event.getTimeStamp())
                    .message(render(event))
                    .build());
        } catch (Exception ignored) {
        }
    }

    private String render(ILoggingEvent e) {
        StringBuilder sb = new StringBuilder(128)
                .append(e.getLevel()).append(' ')
                .append('[').append(e.getThreadName()).append("] ")
                .append(e.getLoggerName()).append(" - ")
                .append(e.getFormattedMessage());
        if (e.getThrowableProxy() != null) {
            sb.append(System.lineSeparator()).append(ThrowableProxyUtil.asString(e.getThrowableProxy()));
        }
        return sb.toString();
    }

    private void flush() {
        if (queue == null || queue.isEmpty()) {
            return;
        }
        List<InputLogEvent> batch = new ArrayList<>(1000);
        queue.drainTo(batch, 1000);
        if (batch.isEmpty()) {
            return;
        }
        batch.sort(Comparator.comparingLong(InputLogEvent::timestamp)); // CloudWatch requires chronological order
        try {
            putEvents(batch);
        } catch (ResourceNotFoundException nf) {
            ensureGroupAndStream();
            try {
                putEvents(batch);
            } catch (Exception ignored) {
            }
        } catch (Exception ex) {
            addWarn("CloudWatch putLogEvents failed: " + ex.getMessage());
        }
    }

    private void putEvents(List<InputLogEvent> batch) {
        client.putLogEvents(PutLogEventsRequest.builder()
                .logGroupName(logGroup)
                .logStreamName(logStream)
                .logEvents(batch)
                .build());
    }

    private void ensureGroupAndStream() {
        try {
            client.createLogGroup(CreateLogGroupRequest.builder().logGroupName(logGroup).build());
        } catch (ResourceAlreadyExistsException ignored) {
        } catch (Exception ignored) {
        }
        try {
            client.createLogStream(CreateLogStreamRequest.builder()
                    .logGroupName(logGroup).logStreamName(logStream).build());
        } catch (ResourceAlreadyExistsException ignored) {
        } catch (Exception ignored) {
        }
    }

    @Override
    public void stop() {
        ready = false;
        try {
            if (scheduler != null) {
                scheduler.shutdown();
            }
            flush();
        } catch (Exception ignored) {
        }
        try {
            if (client != null) {
                client.close();
            }
        } catch (Exception ignored) {
        }
        super.stop();
    }
}
