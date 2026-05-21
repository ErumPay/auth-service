package com.erumpay.auth_service.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class AuthException extends RuntimeException {

    private final HttpStatus status;
    private final Object extra;

    public AuthException(HttpStatus status, String message) {
        super(message);
        this.status = status;
        this.extra = null;
    }

    public AuthException(HttpStatus status, String message, Object extra) {
        super(message);
        this.status = status;
        this.extra = extra;
    }
}
