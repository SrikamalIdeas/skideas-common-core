package com.skideas.common.util;

import java.util.UUID;

/**
 * Generates typed, prefixed IDs for domain entities.
 * Format: {@code <prefix>_<uuid-no-dashes>}
 * Example: {@code usr_01ARZ3NDEKTSV4RRFFQ69G5FAV}
 */
public final class IdGenerator {

    private IdGenerator() {}

    /**
     * Generates a new ID with the given prefix.
     *
     * @param prefix short lowercase string identifying the entity type (e.g. "usr", "msg")
     * @return prefixed ID string
     * @throws IllegalArgumentException if prefix is null or blank
     */
    public static String generate(String prefix) {
        if (prefix == null || prefix.isBlank()) {
            throw new IllegalArgumentException("Prefix must not be null or blank");
        }
        return prefix.toLowerCase() + "_" + UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * Returns true if the given id starts with the expected prefix.
     */
    public static boolean hasPrefix(String id, String prefix) {
        if (id == null || prefix == null) return false;
        return id.startsWith(prefix.toLowerCase() + "_");
    }
}
