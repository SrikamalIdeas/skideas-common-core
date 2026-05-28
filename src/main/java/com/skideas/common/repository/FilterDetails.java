package com.skideas.common.repository;

import com.skideas.common.exception.ValidationException;
import org.springframework.util.StringUtils;

/**
 * POJO holding filter details: field name, operation, and value.
 * Supports validation of operation + value combinations.
 */
public class FilterDetails {
    private String field;
    private FilterOperationsEnum operator;
    private String value;

    public FilterDetails() {
    }

    public FilterDetails(String field, FilterOperationsEnum operator, String value) {
        this.field = field;
        this.operator = operator;
        this.value = value;
    }

    public String getField() {
        return field;
    }

    public void setField(String field) {
        this.field = field;
    }

    public FilterOperationsEnum getOperator() {
        return operator;
    }

    public void setOperator(FilterOperationsEnum operator) {
        this.operator = operator;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    /**
     * Validate this filter details object.
     * Ensures operation + value combination is valid.
     *
     * @throws ValidationException if validation fails
     */
    public void validate() {
        if (!StringUtils.hasText(field)) {
            throw new ValidationException("field", "Filter field cannot be null or empty");
        }

        if (operator == null) {
            throw new ValidationException(field, "Filter operator cannot be null");
        }

        if (!StringUtils.hasText(value)) {
            throw new ValidationException(field, "Filter value cannot be null or empty");
        }

        switch (operator) {
            case BETWEEN:
                if (!value.contains("~")) {
                    throw new ValidationException(field, "BETWEEN operator requires value in format 'value1~value2'");
                }
                break;
            case IN:
            case NOT_IN:
            case LIKE:
            case EQ:
            case NE:
            case GT:
            case LT:
            case GTE:
            case LTE:
                break;
            default:
                break;
        }
    }

    @Override
    public String toString() {
        return "FilterDetails{" +
                "field='" + field + '\'' +
                ", operator=" + operator +
                ", value='" + value + '\'' +
                '}';
    }
}
