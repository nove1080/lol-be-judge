package com.ssafy.c204_be_judge.judge.service;

import com.ssafy.c204_be_judge.aws.web.client.SQSService;
import com.ssafy.c204_be_judge.aws.web.message.JudgeRequestMessage;
import com.ssafy.c204_be_judge.judge.command.JudgeCommand;
import com.ssafy.c204_be_judge.judge.domain.JudgeResult;
import com.ssafy.c204_be_judge.judge.worker.JudgeWorker;
import com.ssafy.c204_be_judge.validation.exception.JudgeServerException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Slf4j
@Service
public class JudgeService {

    private final SQSService sqsService;
    private final JudgeWorker judgeWorker;
    private final RestClient restClient;

    public JudgeService(@Qualifier("judgeWorkerV3") JudgeWorker judgeWorker, SQSService sqsService, RestClient restClient) {
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

        log.debug("채점 소요 시간: {}ms", end - start);
        sendToAPIServer(message.memberId(), result);
        return result;
    }

    private void sendToAPIServer(Long memberId, JudgeResult result) {
        restClient.post()
                .uri("http://localhost:8081/api/v1/problems/judge-result/" + memberId)
                .contentType(MediaType.APPLICATION_JSON)
                .body(result)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, (request, response) -> {
                    throw new JudgeServerException("채점 결과 전송에 실패했습니다. 코드: " + response.getStatusCode());
                })
                .toBodilessEntity();
    }

}