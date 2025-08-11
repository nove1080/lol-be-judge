package com.ssafy.c204_be_judge.judge.service;

import com.ssafy.c204_be_judge.aws.web.client.SQSService;
import com.ssafy.c204_be_judge.aws.web.message.JudgeRequestMessage;
import com.ssafy.c204_be_judge.judge.command.JudgeCommand;
import com.ssafy.c204_be_judge.judge.domain.JudgeResult;
import com.ssafy.c204_be_judge.judge.domain.ProgrammingLanguage;
import com.ssafy.c204_be_judge.judge.worker.JudgeWorker;
import com.ssafy.c204_be_judge.validation.exception.JudgeServerException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.LocalDateTime;
import java.util.Map;

@Slf4j
@Service
@Profile("!test")
@RequiredArgsConstructor
public class JudgeService {

    private final SQSService sqsService;
    private final Map<String, JudgeWorker> judgeWorkerMap;
    private final RestClient restClient;

    @Value("${judge.api-server.base-url}")
    private String apiServerBaseUrl;
    @Value("${judge.api-server.result-path}")
    private String judgeResultApiPath;

    @Scheduled(fixedDelay = 2000)
    public JudgeResult startWorker() {
        JudgeRequestMessage message = sqsService.receiveFromJudgeQueue();
        JudgeResult result = doJudge(message);
        sendToAPIServer(message.memberId(), result);
        return result;
    }

    private JudgeResult doJudge(JudgeRequestMessage message) {
        JudgeResult result = null;

        Long start = System.currentTimeMillis();
        if (ProgrammingLanguage.isJava(message.programmingLanguage())) {
            result = judgeWorkerMap.get("judgeWorkerForJava").run(JudgeCommand.from(message));
        } else if (ProgrammingLanguage.isPython(message.programmingLanguage())) {
            result = judgeWorkerMap.get("judgeWorkerForPython").run(JudgeCommand.from(message));
        } else if (ProgrammingLanguage.isCpp(message.programmingLanguage())) {
            result = judgeWorkerMap.get("judgeWorkerForCpp").run(JudgeCommand.from(message));
        } else {
            result = JudgeResult.fail(JudgeCommand.from(message), "지원하지 않는 프로그래밍 언어입니다: " + message.programmingLanguage());
        }

        Long end = System.currentTimeMillis();
        log.debug("채점 소요 시간: {}ms | memberId: {} | problemId: {} | 시간: {}", end - start, message.memberId(), message.problemId(), LocalDateTime.now());

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