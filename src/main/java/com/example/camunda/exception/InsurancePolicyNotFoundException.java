package com.example.camunda.exception;

public class InsurancePolicyNotFoundException extends BusinessException {

    public InsurancePolicyNotFoundException(String message) {
        super(message);
    }

    public InsurancePolicyNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
