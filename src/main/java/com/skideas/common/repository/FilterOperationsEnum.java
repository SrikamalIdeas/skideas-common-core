package com.skideas.common.repository;

import com.querydsl.core.types.Operator;
import com.querydsl.core.types.Ops;

/**
 * Enum of supported filter operations for dynamic query building.
 * Each operation maps to a QueryDSL operator for predicate construction.
 */
public enum FilterOperationsEnum {
    LIKE(Ops.LIKE, "like", "Pattern matching with %"),
    BETWEEN(Ops.BETWEEN, "between", "Range query (use ~ as separator)"),
    IN(Ops.IN, "in", "Multiple values (comma-separated)"),
    NOT_IN(Ops.NOT_IN, "not_in", "Exclude multiple values"),
    EQ(Ops.EQ, "eq", "Equals"),
    NE(Ops.NE, "ne", "Not equals"),
    GT(Ops.GT, "gt", "Greater than"),
    LT(Ops.LT, "lt", "Less than"),
    GTE(Ops.GOE, "gte", "Greater than or equal"),
    LTE(Ops.LOE, "lte", "Less than or equal");

    private final Operator operator;
    private final String code;
    private final String description;

    FilterOperationsEnum(Operator operator, String code, String description) {
        this.operator = operator;
        this.code = code;
        this.description = description;
    }

    public Operator getOperation() {
        return operator;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static FilterOperationsEnum fromCode(String code) {
        for (FilterOperationsEnum op : values()) {
            if (op.code.equalsIgnoreCase(code)) {
                return op;
            }
        }
        throw new IllegalArgumentException("Unknown filter operation: " + code);
    }
}
