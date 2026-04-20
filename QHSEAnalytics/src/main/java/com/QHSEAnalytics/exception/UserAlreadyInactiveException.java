package com.QHSEAnalytics.exception;

public class UserAlreadyInactiveException extends RuntimeException {
    public UserAlreadyInactiveException(String message) {
        super(message);
    }
}