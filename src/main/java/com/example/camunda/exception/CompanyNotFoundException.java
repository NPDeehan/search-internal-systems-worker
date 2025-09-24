package com.example.camunda.exception;

/**
 * Exception thrown when an external company is not found in the system
 */
public class CompanyNotFoundException extends BusinessException {
    
    public CompanyNotFoundException(String message) {
        super(message);
    }
    
    public CompanyNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
