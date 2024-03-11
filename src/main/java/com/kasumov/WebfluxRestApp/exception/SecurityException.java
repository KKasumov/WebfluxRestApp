package com.kasumov.WebfluxRestApp.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@Getter
public class SecurityException extends RuntimeException {

    private final String errorCode;

    public SecurityException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public static class UnauthorizedException extends SecurityException {
        public UnauthorizedException(String message) {
            super(message, "UNAUTHORIZED");
        }
    }

    public static class AuthException extends SecurityException {
        public AuthException(String message, String errorCode) {
            super(message, errorCode);
        }
    }

    @Getter
    public static class ApiException extends RuntimeException {
        private final String errorCode;

        public ApiException(String message, String errorCode) {
            super(message);
            this.errorCode = errorCode;
        }
    }
}