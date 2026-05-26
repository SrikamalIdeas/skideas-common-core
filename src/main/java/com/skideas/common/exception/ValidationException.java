package com.skideas.common.exception;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Thrown when input validation fails.
 * Maps to HTTP 422 in REST error handlers.
 * Supports single-field and multi-field error reporting.
 */
public class ValidationException extends SkideasException {

    private static final String CODE = "VALIDATION_ERROR";

    private final List<FieldError> fieldErrors;

    public ValidationException(String field, String reason) {
        super(String.format("Validation failed for field '%s': %s", field, reason), CODE);
        this.fieldErrors = List.of(new FieldError(field, reason));
    }

    public ValidationException(List<FieldError> fieldErrors) {
        super("Validation failed for " + fieldErrors.size() + " field(s)", CODE);
        this.fieldErrors = Collections.unmodifiableList(new ArrayList<>(fieldErrors));
    }

    public List<FieldError> getFieldErrors() {
        return fieldErrors;
    }

    public record FieldError(String field, String reason) {}
}
