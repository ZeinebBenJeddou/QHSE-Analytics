package com.QHSEAnalytics.shared.exception;

public class KpiAlreadyExistsException extends RuntimeException {
    public KpiAlreadyExistsException(String message) {
        super(message);
    }
}
