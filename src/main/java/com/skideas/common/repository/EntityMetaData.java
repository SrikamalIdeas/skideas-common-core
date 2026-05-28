package com.skideas.common.repository;

/**
 * POJO holding entity column metadata for filter mapping.
 * Maps user-facing field names to QueryDSL Q-class expressions.
 */
public class EntityMetaData {
    /**
     * User-facing field name (e.g. "context")
     */
    private String field;

    /**
     * QueryDSL Q-class path for this column (e.g. "qUserMemory.context")
     */
    private String columnAliasNameByEntityName;

    /**
     * Entity class name this column belongs to (e.g. "UserMemory")
     */
    private String entityName;

    /**
     * Data type of this column.
     */
    private FilterColumnType columnType;

    public EntityMetaData() {
    }

    public EntityMetaData(String field, String columnAliasNameByEntityName, String entityName, FilterColumnType columnType) {
        this.field = field;
        this.columnAliasNameByEntityName = columnAliasNameByEntityName;
        this.entityName = entityName;
        this.columnType = columnType;
    }

    public String getField() {
        return field;
    }

    public void setField(String field) {
        this.field = field;
    }

    public String getColumnAliasNameByEntityName() {
        return columnAliasNameByEntityName;
    }

    public void setColumnAliasNameByEntityName(String columnAliasNameByEntityName) {
        this.columnAliasNameByEntityName = columnAliasNameByEntityName;
    }

    public String getEntityName() {
        return entityName;
    }

    public void setEntityName(String entityName) {
        this.entityName = entityName;
    }

    public FilterColumnType getColumnType() {
        return columnType;
    }

    public void setColumnType(FilterColumnType columnType) {
        this.columnType = columnType;
    }

    /**
     * Check if this field is filterable.
     *
     * @return true if all required metadata is present
     */
    public boolean isFilterable() {
        return field != null && columnAliasNameByEntityName != null && columnType != null;
    }

    @Override
    public String toString() {
        return "EntityMetaData{" +
                "field='" + field + '\'' +
                ", columnType=" + columnType +
                ", entityName='" + entityName + '\'' +
                '}';
    }
}
