package com.skideas.common.exception;

/**
 * Base exception for all skideas projects.
 * Every subclass MUST provide a machine-readable {@code code} in SCREAMING_SNAKE_CASE
 * so API error envelopes are consistent across all consumers.
 */
public abstract class SkideasException extends RuntimeException {

    private final String code;

    protected SkideasException(String message, String code) {
        super(message);
        this.code = code;
    }

    protected SkideasException(String message, String code, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
