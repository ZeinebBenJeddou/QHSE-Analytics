package com.QHSEAnalytics.exception;

public class ProviderUnavailableException extends RuntimeException {
    public ProviderUnavailableException(String provider, String reason) {
        super("Provider '" + provider + "' unavailable: " + reason);
    }
}