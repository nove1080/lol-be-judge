package com.ssafy.c204_be_judge.validation.exception;

public class JudgeServerException extends RuntimeException {
    public JudgeServerException(String message) {
        super(message);
    }

    public JudgeServerException(String message, Throwable cause) {
        super(message, cause);
    }
}
