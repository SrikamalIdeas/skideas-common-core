package com.skideas.common.exception;

/**
 * Thrown when a downstream external service call fails.
 * Maps to HTTP 502 in REST error handlers.
 */
public class ExternalServiceException extends SkideasException {

    private static final String CODE = "EXTERNAL_SERVICE_ERROR";

    private final String service;

    public ExternalServiceException(String service, String message) {
        super(String.format("[%s] %s", service, message), CODE);
        this.service = service;
    }

    public ExternalServiceException(String service, String message, Throwable cause) {
        super(String.format("[%s] %s", service, message), CODE, cause);
        this.service = service;
    }

    public String getService() {
        return service;
    }
}
