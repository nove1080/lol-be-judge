package com.ssafy.c204_be_judge.rabbitmq.properties;

import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.ConstructorBinding;

@Getter
@ConfigurationProperties(prefix = "spring.rabbitmq")
public class RabbitMQProperties {

    private final String host;
    private final Integer port;
    private final String username;
    private final String password;

    @ConstructorBinding
    public RabbitMQProperties(String host, Integer port, String username, String password) {
        this.host = host;
        this.port = port;
        this.username = username;
        this.password = password;
    }
}
