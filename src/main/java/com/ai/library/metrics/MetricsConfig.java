package com.ai.library.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MetricsConfig {

    @Bean
    public MetricsInterceptor metricsInterceptor(MeterRegistry registry) {
        return new MetricsInterceptor(registry);
    }

}
