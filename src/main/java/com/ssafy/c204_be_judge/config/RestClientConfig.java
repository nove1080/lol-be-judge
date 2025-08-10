package com.ssafy.c204_be_judge.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Configuration
public class RestClientConfig {

    private static final int CONNECTION_TIMEOUT_SECONDS = 3;
    private static final int READ_TIMEOUT_SECONDS = 10;

    @Bean
    public RestClient restClient() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(CONNECTION_TIMEOUT_SECONDS));
        factory.setReadTimeout(Duration.ofSeconds(READ_TIMEOUT_SECONDS));

        return RestClient.builder()
                .requestFactory(factory)
                .build();
    }

}
