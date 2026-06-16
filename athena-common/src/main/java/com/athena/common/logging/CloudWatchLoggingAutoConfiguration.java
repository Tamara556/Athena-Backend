package com.athena.common.logging;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.core.env.Environment;

import java.net.InetAddress;

@AutoConfiguration
@ConditionalOnProperty(prefix = "athena.logging.cloudwatch", name = "enabled", havingValue = "true")
@EnableConfigurationProperties(CloudWatchLoggingProperties.class)
public class CloudWatchLoggingAutoConfiguration implements InitializingBean, DisposableBean {

    private static final org.slf4j.Logger log = LoggerFactory.getLogger(CloudWatchLoggingAutoConfiguration.class);

    private final CloudWatchLoggingProperties props;
    private final String appName;
    private CloudWatchLogbackAppender appender;

    public CloudWatchLoggingAutoConfiguration(CloudWatchLoggingProperties props, Environment env) {
        this.props = props;
        this.appName = env.getProperty("spring.application.name", "athena");
    }

    @Override
    public void afterPropertiesSet() {
        if (!(LoggerFactory.getILoggerFactory() instanceof LoggerContext context)) {
            log.warn("CloudWatch logging requested but the logging backend is not Logback; skipping.");
            return;
        }
        appender = new CloudWatchLogbackAppender();
        appender.setContext(context);
        appender.setName("CLOUDWATCH");
        appender.setRegion(props.getRegion());
        appender.setEndpoint(props.getEndpoint());
        appender.setAccessKey(props.getAccessKey());
        appender.setSecretKey(props.getSecretKey());
        appender.setLogGroup(props.getGroup() != null ? props.getGroup() : "/athena/" + appName);
        appender.setLogStream(props.getStream() != null ? props.getStream() : hostName());
        appender.start();

        Logger root = context.getLogger(Logger.ROOT_LOGGER_NAME);
        root.addAppender(appender);
        log.info("CloudWatch log shipping enabled for '{}'.", appName);
    }

    @Override
    public void destroy() {
        if (appender != null) {
            appender.stop();
        }
    }

    private static String hostName() {
        String env = System.getenv("HOSTNAME");
        if (env != null && !env.isBlank()) {
            return env;
        }
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (Exception e) {
            return "instance";
        }
    }
}
