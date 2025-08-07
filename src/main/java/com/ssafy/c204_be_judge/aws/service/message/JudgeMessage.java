package com.ssafy.c204_be_judge.aws.service.message;

import lombok.Builder;

@Builder
public record JudgeMessage(
        Long playerId,
        Long problemId,
        String programmingLanguage,
        String sourceCode,
        Double timeLimit,
        Integer memoryLimit
) {

}