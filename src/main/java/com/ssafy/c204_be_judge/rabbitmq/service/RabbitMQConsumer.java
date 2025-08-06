package com.ssafy.c204_be_judge.rabbitmq.service;

import com.ssafy.c204_be_judge.rabbitmq.message.JudgeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class RabbitMQConsumer {

    @RabbitListener(queues = "${spring.rabbitmq.judge.queue-name}")
    public void sendMessage(JudgeMessage message) {
        log.info("Received message: {}", message);
    }

}
