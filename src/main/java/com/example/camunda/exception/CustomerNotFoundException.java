package com.example.camunda.exception;

/**
 * Exception thrown when a customer is not found in the system
 */
public class CustomerNotFoundException extends BusinessException {
    
    public CustomerNotFoundException(String message) {
        super(message);
    }
    
    public CustomerNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
