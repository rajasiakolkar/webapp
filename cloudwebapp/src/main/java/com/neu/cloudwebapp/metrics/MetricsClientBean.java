package com.neu.cloudwebapp.metrics;

import com.timgroup.statsd.NoOpStatsDClient;
import com.timgroup.statsd.NonBlockingStatsDClient;
import com.timgroup.statsd.StatsDClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MetricsClientBean {

    @Value("${publish.metrics:true}")
    private boolean publishMetrics;

    @Value("${metrics.server.hostname:localhost}")
    private String metricsServerHost;

    @Value("${metrics.server.port:8125}")
    private int metricsServerPort;

    @Value("csye6225")
    private String prefix;

    @Bean
    public StatsDClient statsDClient() {
        if (publishMetrics) {
            return new NonBlockingStatsDClient(prefix, metricsServerHost, metricsServerPort);
        }
        return new NoOpStatsDClient();
    }

}
