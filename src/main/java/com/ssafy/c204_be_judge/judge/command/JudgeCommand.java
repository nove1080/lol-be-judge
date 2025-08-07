package com.ssafy.c204_be_judge.judge.command;

import com.ssafy.c204_be_judge.aws.service.message.JudgeMessage;
import lombok.Builder;

@Builder
public record JudgeCommand (
        Long problemId,
        String programmingLanguage,
        String sourceCode,
        Double timeLimit,
        Integer memoryLimit
) {

    public static JudgeCommand from(JudgeMessage message) {
        return JudgeCommand.builder()
                .problemId(message.problemId())
                .programmingLanguage(message.programmingLanguage())
                .sourceCode(message.sourceCode())
                .timeLimit(message.timeLimit())
                .memoryLimit(message.memoryLimit())
                .build();
    }

}
