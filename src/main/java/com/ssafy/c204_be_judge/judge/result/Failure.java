package com.ssafy.c204_be_judge.judge.result;

import lombok.Builder;

@Builder
public record Failure (
        String cause
) {
}
