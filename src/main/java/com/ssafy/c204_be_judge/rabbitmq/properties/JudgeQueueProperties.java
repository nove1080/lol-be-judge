package com.ssafy.c204_be_judge.rabbitmq.properties;

import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.ConstructorBinding;

@Getter
@ConfigurationProperties(prefix = "spring.rabbitmq.judge")
public class JudgeQueueProperties {

    private final String exchangeName;
    private final String queueName;
    private final String routingKey;

    @ConstructorBinding
    public JudgeQueueProperties(String exchangeName, String queueName, String routingKey) {
        this.exchangeName = exchangeName;
        this.queueName = queueName;
        this.routingKey = routingKey;
    }
}
