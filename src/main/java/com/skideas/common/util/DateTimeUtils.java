package com.skideas.common.util;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Stateless date/time helpers. All operations use UTC.
 */
public final class DateTimeUtils {

    public static final DateTimeFormatter ISO_UTC =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'").withZone(ZoneOffset.UTC);

    private DateTimeUtils() {}

    /** Current instant in UTC. */
    public static Instant nowUtc() {
        return Instant.now();
    }

    /** Current date in UTC. */
    public static LocalDate todayUtc() {
        return LocalDate.now(ZoneOffset.UTC);
    }

    /**
     * Formats an Instant to ISO-8601 UTC string (seconds precision).
     *
     * @throws IllegalArgumentException if instant is null
     */
    public static String toIsoString(Instant instant) {
        if (instant == null) throw new IllegalArgumentException("Instant must not be null");
        return ISO_UTC.format(instant);
    }

    /**
     * Parses an ISO-8601 UTC string back to an Instant.
     *
     * @throws IllegalArgumentException if value is null/blank or unparseable
     */
    public static Instant fromIsoString(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("ISO string must not be null or blank");
        }
        try {
            return ZonedDateTime.parse(value, ISO_UTC).toInstant();
        } catch (Exception e) {
            throw new IllegalArgumentException("Unparseable ISO UTC string: " + value, e);
        }
    }

    /** Returns true if {@code candidate} is strictly before {@code reference}. */
    public static boolean isBefore(Instant candidate, Instant reference) {
        if (candidate == null || reference == null) return false;
        return candidate.isBefore(reference);
    }
}
