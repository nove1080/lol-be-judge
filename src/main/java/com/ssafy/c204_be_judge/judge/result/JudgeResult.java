package com.ssafy.c204_be_judge.judge.result;

import lombok.Builder;

import java.util.Objects;

@Builder
public record JudgeResult(
        Double runningTime,
        Failure failure
) {

    public JudgeResult {
        if (Objects.isNull(runningTime)) {
            Objects.requireNonNull(failure, "실행시간이 NULL 인 경우, failure 객체는 NULL 일 수 없습니다.");
        }
    }

}
