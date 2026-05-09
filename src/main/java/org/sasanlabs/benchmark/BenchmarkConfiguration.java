package org.sasanlabs.benchmark;

import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class BenchmarkConfiguration {

    @Bean
    public RestTemplate benchmarkRestTemplate(
            RestTemplateBuilder builder,
            @Value("${benchmark.dast.ground-truth.connect-timeout-ms:5000}") long connectMs,
            @Value("${benchmark.dast.ground-truth.read-timeout-ms:10000}") long readMs) {
        return builder.setConnectTimeout(Duration.ofMillis(connectMs))
                .setReadTimeout(Duration.ofMillis(readMs))
                .build();
    }
}
