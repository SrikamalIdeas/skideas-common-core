package com.skideas.common.repository;

import org.springframework.data.domain.Page;

import java.util.List;
import java.util.Map;

/**
 * Generic interface for domain-specific repositories.
 * Defines contract for dynamic query building, filtering, and pagination.
 *
 * @param <D> domain class (entity type)
 * @param <I> ID type
 */
public interface BaseRepository<D, I> {

    /**
     * Get paginated collection of domain objects with dynamic filters.
     *
     * @param filterDetailsBySearchField map of field name to filter details
     * @param pageable pagination parameters and total record holder
     * @return page of results
     */
    Page<D> getCollections(
            Map<String, FilterDetails> filterDetailsBySearchField,
            DynamicPageable pageable
    );

    /**
     * Get paginated collection with filters and relation eager-loading hints.
     *
     * @param filterDetailsBySearchField map of field name to filter details
     * @param pageable pagination parameters and total record holder
     * @param embeds relation names to eager-load
     * @return page of results
     */
    Page<D> getCollections(
            Map<String, FilterDetails> filterDetailsBySearchField,
            DynamicPageable pageable,
            List<EmbedEnum> embeds
    );
}
