package com.ssafy.c204_be_judge.judge.command;

import com.ssafy.c204_be_judge.rabbitmq.message.JudgeMessage;
import lombok.Builder;

@Builder
public record JudgeCommand (
        Long problemId,
        String programmingLanguage,
        String sourceCode
) {

    public static JudgeCommand from(JudgeMessage message) {
        return JudgeCommand.builder()
                .problemId(message.getProblemId())
                .programmingLanguage(message.getProgrammingLanguage())
                .sourceCode(message.getSourceCode())
                .build();
    }

}
