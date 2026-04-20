package com.QHSEAnalytics.exception;

public class UserAlreadyAdminException extends RuntimeException {
    public UserAlreadyAdminException(String message) {
        super(message);
    }
}