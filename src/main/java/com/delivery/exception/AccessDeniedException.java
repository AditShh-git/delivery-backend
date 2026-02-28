package com.delivery.exception;

public class AccessDeniedException extends ApiException {
    public AccessDeniedException(String message) {
        super(message);
    }
}
