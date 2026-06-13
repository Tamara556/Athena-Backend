package com.athena.learning.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class LearningConfig {

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
