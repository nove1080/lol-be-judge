package com.ssafy.c204_be_judge.judge.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

@Getter
@ToString
@RequiredArgsConstructor
public enum SandboxErrorCode {
    RE("Runtime Error"),
    SG("Signal"),
    TO("Timed Out"),
    XX("Internal Error");

    private final String description;
}
