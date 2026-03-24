package com.example.camunda.exception;

/**
 * Exception thrown when an account is not found in the system
 */
public class AccountNotFoundException extends BusinessException {

    public AccountNotFoundException(String message) {
        super(message);
    }

    public AccountNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}