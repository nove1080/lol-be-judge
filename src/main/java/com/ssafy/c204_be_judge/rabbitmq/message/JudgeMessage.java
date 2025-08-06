package com.ssafy.c204_be_judge.rabbitmq.message;

import lombok.*;

@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JudgeMessage {

    private Long playerId;
    private Long problemId;
    private String programmingLanguage;
    private String sourceCode;
    private Double timeLimit;
    private Integer memoryLimit;

}
