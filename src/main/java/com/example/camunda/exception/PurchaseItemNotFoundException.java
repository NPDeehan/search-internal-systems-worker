package com.example.camunda.exception;

public class PurchaseItemNotFoundException extends BusinessException {

    public PurchaseItemNotFoundException(String message) {
        super(message);
    }

    public PurchaseItemNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
