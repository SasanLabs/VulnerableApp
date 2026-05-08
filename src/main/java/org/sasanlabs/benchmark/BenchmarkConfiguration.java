package org.sasanlabs.benchmark;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class BenchmarkConfiguration {

    @Bean
    public RestTemplate benchmarkRestTemplate(RestTemplateBuilder builder) {
        return builder.build();
    }
}
