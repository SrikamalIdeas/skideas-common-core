package com.skideas.common.util;

/**
 * Input sanitization helpers. All methods are null-safe and return a safe value.
 * Does NOT do HTML escaping — use a dedicated library (e.g. OWASP Java HTML Sanitizer) for that.
 */
public final class StringSanitizer {

    private static final int DEFAULT_MAX_LENGTH = 1000;

    private StringSanitizer() {}

    /**
     * Trims whitespace, collapses internal runs of whitespace to a single space,
     * and strips non-printable control characters.
     *
     * @return sanitized string, or empty string if input is null/blank
     */
    public static String sanitize(String input) {
        return sanitize(input, DEFAULT_MAX_LENGTH);
    }

    /**
     * Same as {@link #sanitize(String)} but also truncates to {@code maxLength}.
     *
     * @throws IllegalArgumentException if maxLength ≤ 0
     */
    public static String sanitize(String input, int maxLength) {
        if (maxLength <= 0) throw new IllegalArgumentException("maxLength must be > 0");
        if (input == null || input.isBlank()) return "";
        String stripped = input.replaceAll("[\\p{Cntrl}&&[^\r\n\t]]", "")
                               .replaceAll("\\s+", " ")
                               .strip();
        return stripped.length() > maxLength ? stripped.substring(0, maxLength) : stripped;
    }

    /**
     * Returns true if the input is non-null and its sanitized form is non-empty.
     */
    public static boolean hasContent(String input) {
        return input != null && !sanitize(input).isEmpty();
    }

    /**
     * Masks all but the last {@code visibleChars} characters with '*'.
     * Useful for logging sensitive fields.
     *
     * @throws IllegalArgumentException if visibleChars is negative
     */
    public static String mask(String input, int visibleChars) {
        if (visibleChars < 0) throw new IllegalArgumentException("visibleChars must be >= 0");
        if (input == null || input.isEmpty()) return "";
        if (input.length() <= visibleChars) return "*".repeat(input.length());
        String suffix = input.substring(input.length() - visibleChars);
        return "*".repeat(input.length() - visibleChars) + suffix;
    }
}
