package com.project.ciaklog.exception;

public class LimitExceededException extends RuntimeException {
    public LimitExceededException(String message) {
        super(message);
    }
}