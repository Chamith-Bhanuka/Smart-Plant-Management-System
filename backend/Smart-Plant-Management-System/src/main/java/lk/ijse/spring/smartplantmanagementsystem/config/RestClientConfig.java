package lk.ijse.spring.smartplantmanagementsystem.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

@Configuration
public class RestClientConfig {
    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder
                .connectTimeout(Duration.ofSeconds(5))    // connection must succeed quickly
                .readTimeout(Duration.ofSeconds(240))     // allow up to 2 minutes for a reply
                .build();
    }
}
