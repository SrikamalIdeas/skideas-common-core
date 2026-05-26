package com.skideas.common.exception;

/**
 * Thrown when a requested resource does not exist.
 * Maps to HTTP 404 in REST error handlers.
 */
public class ResourceNotFoundException extends SkideasException {

    private static final String CODE = "RESOURCE_NOT_FOUND";

    public ResourceNotFoundException(String resource, Object id) {
        super(String.format("%s not found with id: %s", resource, id), CODE);
    }

    public ResourceNotFoundException(String message) {
        super(message, CODE);
    }
}
