package com.skideas.common.repository;

import org.springframework.data.domain.Pageable;

/**
 * Wrapper for pagination parameters with total record count holder.
 * Used to pass pagination info to repository layer and return total count.
 */
public class DynamicPageable {
    /**
     * Spring's Pageable interface with page, size, and sort info.
     */
    private final Pageable pageable;

    /**
     * Total number of records (set during query execution).
     */
    private Long totalRecords = 0L;

    public DynamicPageable(Pageable pageable) {
        this.pageable = pageable;
    }

    public Pageable getPageable() {
        return pageable;
    }

    public Long getTotalRecords() {
        return totalRecords;
    }

    public void setTotalRecords(Long totalRecords) {
        this.totalRecords = totalRecords;
    }
}
