package com.example.camunda.exception;

/**
 * Base exception for business logic errors
 */
public abstract class BusinessException extends RuntimeException {
    
    public BusinessException(String message) {
        super(message);
    }
    
    public BusinessException(String message, Throwable cause) {
        super(message, cause);
    }
}
