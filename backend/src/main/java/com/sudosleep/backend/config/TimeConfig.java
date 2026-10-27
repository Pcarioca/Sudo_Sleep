package com.sudosleep.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class TimeConfig {

    /** Injected wherever "today" is needed, so tests can fix the date. */
    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }
}
