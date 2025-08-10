package com.ssafy.c204_be_judge.judge.domain;

import lombok.Builder;

@Builder
public record Failure (
        String cause
) {
}
