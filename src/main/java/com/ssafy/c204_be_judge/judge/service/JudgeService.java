package com.ssafy.c204_be_judge.judge.service;

import com.ssafy.c204_be_judge.aws.web.client.SQSService;
import com.ssafy.c204_be_judge.aws.web.message.JudgeRequestMessage;
import com.ssafy.c204_be_judge.judge.command.JudgeCommand;
import com.ssafy.c204_be_judge.judge.domain.JudgeResult;
import com.ssafy.c204_be_judge.judge.worker.JudgeWorker;
import com.ssafy.c204_be_judge.validation.exception.JudgeServerException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.LocalDateTime;

@Slf4j
@Service
public class JudgeService {

    private final SQSService sqsService;
    private final JudgeWorker judgeWorker;
    private final RestClient restClient;

    @Value("${judge.api-server.base-url}")
    private String apiServerBaseUrl;
    @Value("${judge.api-server.result-path}")
    private String judgeResultApiPath;

    public JudgeService(@Qualifier("judgeWorkerForUbuntu") JudgeWorker judgeWorker, SQSService sqsService, RestClient restClient) {
        this.judgeWorker = judgeWorker;
        this.sqsService = sqsService;
        this.restClient = restClient;
    }

    @Scheduled(fixedDelay = 2000)
    public JudgeResult startWorker() {
        JudgeRequestMessage message = sqsService.receiveFromJudgeQueue();
        Long start = System.currentTimeMillis();
        JudgeResult result = judgeWorker.run(JudgeCommand.from(message));
        Long end = System.currentTimeMillis();

        log.info("채점 소요 시간: {}ms | memberId: {} | problemId: {} | 시간: {}", end - start, message.memberId(), message.problemId(), LocalDateTime.now());
        sendToAPIServer(message.memberId(), result);
        return result;
    }

    private void sendToAPIServer(Long memberId, JudgeResult result) {
        restClient.post()
                .uri(apiServerBaseUrl + judgeResultApiPath + memberId)
                .contentType(MediaType.APPLICATION_JSON)
                .body(result)
                .retrieve()
                .onStatus(HttpStatusCode::isError, (request, response) -> {
                    throw new JudgeServerException("채점 결과 전송에 실패했습니다. 코드: " + response.getStatusCode());
                })
                .toBodilessEntity();
    }

}