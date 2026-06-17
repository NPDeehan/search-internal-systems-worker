package com.example.camunda.exception;

public class PackageNotFoundException extends BusinessException {

    public PackageNotFoundException(String message) {
        super(message);
    }

    public PackageNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
