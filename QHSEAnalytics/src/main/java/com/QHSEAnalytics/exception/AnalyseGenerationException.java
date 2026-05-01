package com.QHSEAnalytics.exception;

public class AnalyseGenerationException extends RuntimeException {
    public AnalyseGenerationException(String message) {
        super(message);
    }

    public AnalyseGenerationException(String message, Throwable cause) {
        super(message, cause);
    }
}