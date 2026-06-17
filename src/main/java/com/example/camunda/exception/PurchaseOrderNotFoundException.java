package com.example.camunda.exception;

public class PurchaseOrderNotFoundException extends BusinessException {

    public PurchaseOrderNotFoundException(String message) {
        super(message);
    }

    public PurchaseOrderNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
