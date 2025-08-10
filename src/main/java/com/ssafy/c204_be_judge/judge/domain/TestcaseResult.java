package com.ssafy.c204_be_judge.judge.domain;

import lombok.Builder;

import java.util.Objects;

@Builder
public record TestcaseResult(
        Integer testcaseNumber,
        boolean isSolved, //문제를 맞추었는지 여부
        Double runningTime,
        Integer memoryUsage,
        Failure failure //테스트케이스 실행이 실패한 원인
) {
    public TestcaseResult {
        if (!isSolved) {
            Objects.requireNonNull(failure, "Failure must be provided when not solved");
        }
    }

    public static TestcaseResult success(Integer testcaseNumber, Double runningTime, Integer memoryUsage) {
        return TestcaseResult.builder()
                .testcaseNumber(testcaseNumber)
                .isSolved(true)
                .runningTime(runningTime)
                .memoryUsage(memoryUsage)
                .build();
    }

    /**
     * 테스트케이스를 맞추지 못한 경우에 대한 TestcaseResult를 생성합니다.
     * @param testcaseNumber 테스트케이스 번호
     * @param cause 실패 원인
     * @return TestcaseResult
     */
    public static TestcaseResult fail(Integer testcaseNumber, String cause) {
        return TestcaseResult.builder()
                .testcaseNumber(testcaseNumber)
                .isSolved(false)
                .failure(Failure.builder().cause(cause).build())
                .build();
    }

    public static TestcaseResult fail(Integer testcaseNumber) {
        return fail(testcaseNumber, "틀렸습니다.");
    }
}
