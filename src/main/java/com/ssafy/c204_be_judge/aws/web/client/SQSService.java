package com.ssafy.c204_be_judge.aws.web.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.ssafy.c204_be_judge.aws.config.AWSProperties;
import com.ssafy.c204_be_judge.aws.web.message.JudgeRequestMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerErrorException;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;

import java.util.NoSuchElementException;
import java.util.Optional;

@Component
@Slf4j
@RequiredArgsConstructor
public class SQSService {

    private final SqsClient sqsClient;
    private final AWSProperties awsProperties;
    private final ObjectMapper objectMapper;

    /**
     * 채점 요청을 메시지 큐에서 가져옵니다.
     * @return JudgeMessage 객체
     * @throws RuntimeException 채점 요청이 없을 경우
     */
    public JudgeRequestMessage receiveFromJudgeQueue() {
        String queueUrl = sqsClient.getQueueUrl(GetQueueUrlRequest.builder()
                        .queueName(awsProperties.getSqsQueueName())
                        .build())
                .queueUrl();

        Message message = receiveSingleMessage(queueUrl)
                .orElseThrow(() -> new NoSuchElementException("채점 요청이 없습니다."));

        try {
            return objectMapper.registerModule(new JavaTimeModule()).readValue(message.body(), JudgeRequestMessage.class);
        } catch (JsonProcessingException e) {
            throw new ServerErrorException("메시지를 JudgeMessage로 변환하는데 실패했습니다.", e);
        } finally {
            deleteMessage(queueUrl, message);
        }
    }

    private void deleteMessage(String queueUrl, Message message) {
        DeleteMessageRequest deleteMessageRequest = DeleteMessageRequest.builder()
                .queueUrl(queueUrl)
                .receiptHandle(message.receiptHandle())
                .build();

        sqsClient.deleteMessage(deleteMessageRequest);
    }

    private Optional<Message> receiveSingleMessage(String queueUrl) {
        ReceiveMessageRequest request = ReceiveMessageRequest.builder()
                .queueUrl(queueUrl)
                .maxNumberOfMessages(1)
                .waitTimeSeconds(20)
                .build();

        return sqsClient.receiveMessage(request)
                .messages()
                .stream()
                .findFirst();
    }
}
