package com.athena.progress.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class ProgressConfig {

    /**
     * Injecting a {@link Clock} keeps streak/date logic deterministic and testable.
     */
    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
