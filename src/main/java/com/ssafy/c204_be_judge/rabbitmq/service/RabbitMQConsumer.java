package com.ssafy.c204_be_judge.rabbitmq.service;

import com.ssafy.c204_be_judge.rabbitmq.message.JudgeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RabbitMQConsumer {

    @RabbitListener(queues = "${spring.rabbitmq.judge.queue-name}")
    public void sendMessage(JudgeMessage message) {
    }

}
