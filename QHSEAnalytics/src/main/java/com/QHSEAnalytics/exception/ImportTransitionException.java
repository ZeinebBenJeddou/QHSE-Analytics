package com.QHSEAnalytics.exception;

public class ImportTransitionException extends RuntimeException {

    public ImportTransitionException(String message) {
        super(message);
    }

    public ImportTransitionException(String message, Throwable cause) {
        super(message, cause);
    }
}
