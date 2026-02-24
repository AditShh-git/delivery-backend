package com.delivery.exception;

public class EmailAlreadyExistsException extends ApiException {
    public EmailAlreadyExistsException(String email) {
        super("Email already registered: " + email);
    }
}
