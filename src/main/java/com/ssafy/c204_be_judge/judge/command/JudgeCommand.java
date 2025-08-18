package com.ssafy.c204_be_judge.judge.command;

import com.ssafy.c204_be_judge.aws.web.message.JudgeRequestMessage;
import lombok.Builder;

@Builder
public record JudgeCommand (
        Long playerId,
        Long problemId,
        String programmingLanguage,
        String sourceCode,
        Double timeLimit,
        Integer memoryLimit
) {

    public static JudgeCommand from(JudgeRequestMessage message) {
        return JudgeCommand.builder()
                .playerId(message.playerId())
                .problemId(message.problemId())
                .programmingLanguage(message.programmingLanguage())
                .sourceCode(message.sourceCode())
                .timeLimit(message.timeLimit())
                .memoryLimit(message.memoryLimit())
                .build();
    }

}
