package com.skideas.common.repository;

/**
 * Enum of supported column data types for filter value conversion.
 * Used to determine how to parse and convert filter values from strings.
 */
public enum FilterColumnType {
    STRING("String"),
    INTEGER("Integer"),
    LONG("Long"),
    DOUBLE("Double"),
    FLOAT("Float"),
    DATE("Date (yyyy-MM-dd)"),
    DATE_TIME("DateTime (yyyy-MM-dd HH:mm:ss)"),
    BOOLEAN("Boolean");

    private final String description;

    FilterColumnType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
