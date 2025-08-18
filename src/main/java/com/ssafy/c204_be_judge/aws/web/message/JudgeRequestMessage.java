package com.ssafy.c204_be_judge.aws.web.message;

import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record JudgeRequestMessage(
        Long playerId,
        Long memberId,
        Long problemId,
        String programmingLanguage,
        String sourceCode,
        Double timeLimit,
        Integer memoryLimit,
        LocalDateTime submittedAt
) {

}