package com.skideas.common.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;

import java.util.List;
import java.util.Map;

/**
 * Generic base interface for all domain repositories in SrikamalIdeas projects.
 *
 * <p>Extends {@link JpaRepository} so that domain repositories only need to declare
 * {@code extends BaseRepository<Entity, Long>} — no separate {@code JpaRepository} extends
 * clause is required. The standard CRUD, paging, and sorting operations from {@code JpaRepository}
 * are inherited automatically.
 *
 * <p>{@link NoRepositoryBean} prevents Spring Data from instantiating this interface directly.
 * Only concrete domain repository sub-interfaces (e.g. {@code UserMemoryRepository}) are
 * registered as Spring beans.
 *
 * <p>Adds the dynamic QueryDSL filter/pagination contract ({@code getCollections}) on top.
 *
 * @param <D> domain class (entity type)
 * @param <I> ID type
 */
@NoRepositoryBean
public interface BaseRepository<D, I> extends JpaRepository<D, I> {

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
