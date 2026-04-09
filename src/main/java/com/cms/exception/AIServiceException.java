package com.cms.exception;

import lombok.Getter;

@Getter
public class AIServiceException extends RuntimeException {
    private final String errorCode;

    public AIServiceException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }
}