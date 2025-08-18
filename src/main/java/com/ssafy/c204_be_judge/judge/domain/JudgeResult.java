package com.ssafy.c204_be_judge.judge.domain;

import com.ssafy.c204_be_judge.judge.command.JudgeCommand;
import jakarta.annotation.Nullable;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Slf4j
@Builder
public record JudgeResult(
        Long playerId,
        Long problemId,
        String programmingLanguage,
        Boolean isSolved,
        @Nullable Double maxRunningTime,
        @Nullable Integer maxMemoryUsage,
        @Nullable List<TestcaseResult> testcaseResults,
        @Nullable Failure failure
) {
    public JudgeResult {
        if (Objects.isNull(testcaseResults)) {
            Objects.requireNonNull(failure);
        }
    }

    /**
     * 채점에 성공한 경우에 대한 JudgeResult를 생성합니다.
     *
     * @param command         채점 명령
     * @param testcaseResults 테스트케이스 결과 리스트
     * @return JudgeResult
     */
    public static JudgeResult success(JudgeCommand command, List<TestcaseResult> testcaseResults) {
        boolean allSolved = testcaseResults.stream().allMatch(TestcaseResult::isSolved);

        Failure failure = null;
        double maxRunningTime = 0.0;
        int maxMemoryUsage = 0;

        if (allSolved) {
            for (TestcaseResult result : testcaseResults) {
                if (result.runningTime() != null && result.runningTime() > maxRunningTime) {
                    maxRunningTime = result.runningTime();
                }
                if (result.memoryUsage() != null && result.memoryUsage() > maxMemoryUsage) {
                    maxMemoryUsage = result.memoryUsage();
                }
            }
        } else {
            failure = Failure.builder()
                    .cause("틀렸습니다.")
                    .build();
        }

        return JudgeResult.builder()
                .playerId(command.playerId())
                .problemId(command.problemId())
                .programmingLanguage(command.programmingLanguage())
                .isSolved(allSolved)
                .maxRunningTime(maxRunningTime > 0 ? maxRunningTime : null)
                .maxMemoryUsage(maxMemoryUsage > 0 ? maxMemoryUsage : null)
                .testcaseResults(testcaseResults)
                .failure(failure)
                .build();
    }

    /**
     * 채점에 실패한 경우에 대한 JudgeResult를 생성합니다.
     *
     * @param command 채점 명령
     * @param cause   실패 원인
     * @return JudgeResult
     */
    public static JudgeResult fail(JudgeCommand command, String cause) {
        return JudgeResult.builder()
                .playerId(command.playerId())
                .problemId(command.problemId())
                .programmingLanguage(command.programmingLanguage())
                .isSolved(false)
                .failure(Failure.builder().cause(cause).build())
                .build();
    }
}
