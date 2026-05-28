package com.skideas.common.integration;

import com.querydsl.core.Tuple;
import com.querydsl.core.types.Expression;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.EntityPathBase;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.JPQLQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.skideas.common.exception.ValidationException;
import com.skideas.common.repository.BaseRepositoryImpl;
import com.skideas.common.repository.DynamicPageable;
import com.skideas.common.repository.EntityMetaData;
import com.skideas.common.repository.FilterColumnType;
import com.skideas.common.repository.FilterDetails;
import com.skideas.common.repository.FilterOperationsEnum;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;

import jakarta.persistence.EntityManager;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@ActiveProfiles("test")
class RepositoryInfrastructureIntegrationTest {

    private static final EntityPathBase<RepositoryTestEntity> ROOT =
            new EntityPathBase<>(RepositoryTestEntity.class, "repositoryTestEntity");

    @Autowired
    private TestEntityManager em;

    private RepositoryTestBaseImpl repository;
    private JPAQueryFactory queryFactory;

    @BeforeEach
    void setUp() {
        EntityManager entityManager = em.getEntityManager();
        queryFactory = new JPAQueryFactory(entityManager);
        repository = new RepositoryTestBaseImpl(entityManager, queryFactory);

        em.persist(new RepositoryTestEntity(1L, "Alice", 30, 200L, 4.9, 70.5f,
                LocalDate.of(1994, 5, 10), LocalDateTime.of(2024, 1, 5, 10, 0), true));
        em.persist(new RepositoryTestEntity(2L, "Bob", 20, 90L, 3.7, 55.0f,
                LocalDate.of(2001, 7, 15), LocalDateTime.of(2024, 1, 20, 9, 30), false));
        em.persist(new RepositoryTestEntity(3L, "Cara", 40, 150L, 4.2, 62.0f,
                LocalDate.of(1992, 3, 8), LocalDateTime.of(2024, 2, 10, 14, 45), true));
        em.flush();
        em.clear();
    }

    @Test
    void buildPredicate_returnsNullWhenNoFilters() {
        assertThat(repository.exposeBuildPredicate(Collections.emptyMap())).isNull();
    }

    @Test
    void buildPredicate_supportsStringNumericDateDateTimeAndBooleanFilters() {
        assertThat(idsFor(Map.of("name", new FilterDetails(null, FilterOperationsEnum.LIKE, "li"))))
                .containsExactly(1L);
        assertThat(idsFor(Map.of("age", new FilterDetails(null, FilterOperationsEnum.EQ, "20"))))
                .containsExactly(2L);
        assertThat(idsFor(Map.of("longScore", new FilterDetails(null, FilterOperationsEnum.GT, "100"))))
                .containsExactly(1L, 3L);
        assertThat(idsFor(Map.of("rating", new FilterDetails(null, FilterOperationsEnum.LT, "5.0"))))
                .containsExactly(1L, 2L, 3L);
        assertThat(idsFor(Map.of("weight", new FilterDetails(null, FilterOperationsEnum.GTE, "60.0"))))
                .containsExactly(1L, 3L);
        assertThat(idsFor(Map.of("birthDate", new FilterDetails(null, FilterOperationsEnum.BETWEEN, "1990-01-01~1996-12-31"))))
                .containsExactly(1L, 3L);
        assertThat(idsFor(Map.of("createdAt", new FilterDetails(null, FilterOperationsEnum.BETWEEN, "2024-01-01 00:00:00~2024-01-31 23:59:59"))))
                .containsExactly(1L, 2L);
        assertThat(idsFor(Map.of("active", new FilterDetails(null, FilterOperationsEnum.EQ, "true"))))
                .containsExactly(1L, 3L);
    }

    @Test
    void buildPredicate_supportsInNotInAndComparisonVariants() {
        assertThat(idsFor(Map.of("age", new FilterDetails(null, FilterOperationsEnum.IN, "20,30"))))
                .containsExactly(1L, 2L);
        assertThat(idsFor(Map.of("name", new FilterDetails(null, FilterOperationsEnum.NOT_IN, "Bob"))))
                .containsExactly(1L, 3L);
        assertThat(idsFor(Map.of("age", new FilterDetails(null, FilterOperationsEnum.NE, "20"))))
                .containsExactly(1L, 3L);
        assertThat(idsFor(Map.of("age", new FilterDetails(null, FilterOperationsEnum.LTE, "20"))))
                .containsExactly(2L);
    }

    @Test
    void buildPredicate_rejectsInvalidFilters() {
        assertThatThrownBy(() -> repository.exposeBuildPredicate(Map.of(
                "unknown", new FilterDetails(null, FilterOperationsEnum.EQ, "x")
        ))).isInstanceOf(ValidationException.class);

        assertThatThrownBy(() -> repository.exposeBuildPredicate(Map.of(
                "active", new FilterDetails(null, FilterOperationsEnum.EQ, "maybe")
        ))).isInstanceOf(ValidationException.class);

        assertThatThrownBy(() -> repository.exposeBuildPredicate(Map.of(
                "age", new FilterDetails(null, FilterOperationsEnum.BETWEEN, "1~2")
        ))).isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> repository.exposeBuildPredicate(Map.of(
                "rating", new FilterDetails(null, FilterOperationsEnum.EQ, "oops")
        ))).isInstanceOf(ValidationException.class);

        Map<String, FilterDetails> nullFilter = new HashMap<>();
        nullFilter.put("name", null);
        assertThatThrownBy(() -> repository.exposeBuildPredicate(nullFilter))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("name");

        repository.setMetadata("inactiveField", new EntityMetaData("inactiveField", null, "repositoryTestEntity", FilterColumnType.STRING));
        assertThatThrownBy(() -> repository.exposeBuildPredicate(Map.of(
                "inactiveField", new FilterDetails(null, FilterOperationsEnum.EQ, "x")
        ))).isInstanceOf(ValidationException.class);
    }

    @Test
    void getIds_appliesSortPaginationAndTracksDomains() {
        DynamicPageable pageable = new DynamicPageable(PageRequest.of(0, 2, Sort.by(Sort.Order.desc("name"))));

        List<Long> ids = repository.exposeGetIds(
                Map.of("active", new FilterDetails(null, FilterOperationsEnum.EQ, "true")),
                pageable
        );

        assertThat(ids).containsExactly(3L, 1L);
        assertThat(pageable.getTotalRecords()).isEqualTo(2L);
        assertThat(repository.getAppliedDomains()).containsExactly("repositoryTestEntity");
    }

    @Test
    void getIds_usesDefaultSortAndHandlesEmptyFilterMap() {
        DynamicPageable pageable = new DynamicPageable(PageRequest.of(0, 2, Sort.unsorted()));

        List<Long> ids = repository.exposeGetIds(Collections.emptyMap(), pageable);

        assertThat(ids).containsExactly(3L, 2L);
        assertThat(pageable.getTotalRecords()).isEqualTo(3L);
        assertThat(repository.getAppliedDomains()).isEmpty();
    }

    @Test
    void getIds_returnsEmptyWhenNothingMatches() {
        DynamicPageable pageable = new DynamicPageable(PageRequest.of(0, 5, Sort.by("name")));

        List<Long> ids = repository.exposeGetIds(
                Map.of("name", new FilterDetails(null, FilterOperationsEnum.EQ, "Nobody")),
                pageable
        );

        assertThat(ids).isEmpty();
        assertThat(pageable.getTotalRecords()).isZero();
    }

    @Test
    void mapPageableResponseAndSortTranslationWork() {
        DynamicPageable pageable = new DynamicPageable(PageRequest.of(1, 1, Sort.by("name")));
        pageable.setTotalRecords(3L);

        Page<RepositoryTestEntity> page = repository.exposeMapPageableResponse(
                pageable,
                List.of(new RepositoryTestEntity(2L, "Bob", 20, 90L, 3.7, 55.0f,
                        LocalDate.of(2001, 7, 15), LocalDateTime.of(2024, 1, 20, 9, 30), false))
        );
        assertThat(page.getTotalElements()).isEqualTo(3L);
        assertThat(page.getNumber()).isEqualTo(1);
        assertThat(page.getContent()).extracting(RepositoryTestEntity::getName).containsExactly("Bob");

        DynamicPageable emptyPageable = new DynamicPageable(PageRequest.of(0, 10));
        emptyPageable.setTotalRecords(99L);
        Page<RepositoryTestEntity> emptyPage = repository.exposeMapPageableResponse(emptyPageable, Collections.emptyList());
        assertThat(emptyPage.getTotalElements()).isZero();
        assertThat(emptyPage.getContent()).isEmpty();

        OrderSpecifier<?>[] explicitSort = repository.exposeUpdateSortWithDomainNames(Sort.by(Sort.Order.asc("name")));
        assertThat(explicitSort).hasSize(1);
        assertThat(explicitSort[0].toString()).contains("repositoryTestEntity.name");

        OrderSpecifier<?>[] defaultSort = repository.exposeUpdateSortWithDomainNames(Sort.unsorted());
        assertThat(defaultSort).hasSize(1);
        assertThat(defaultSort[0].toString()).contains("repositoryTestEntity.createdAt");
    }

    @Test
    void getCollections_defaultImplementationRequiresOverride() {
        DynamicPageable pageable = new DynamicPageable(Pageable.unpaged());

        assertThatThrownBy(() -> repository.getCollections(Collections.emptyMap(), pageable))
                .isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> repository.getCollections(Collections.emptyMap(), pageable, List.of()))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void privateHelpers_coverValidationAndFallbackBranches() {
        assertThatThrownBy(() -> invokePrivate("validateFilterValue", new Class<?>[]{String.class, String.class}, "name", " "))
                .isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> invokePrivate("splitValues", new Class<?>[]{String.class, String.class}, "name", " , "))
                .isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> invokePrivate(
                "getFiltersByColumnAliasNamesForBetween",
                new Class<?>[]{EntityMetaData.class, String.class},
                repository.meta("createdAt"), "2024-01-01"
        )).isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> invokePrivate(
                "getFiltersByColumnAliasNamesForBetween",
                new Class<?>[]{EntityMetaData.class, String.class},
                repository.meta("createdAt"), "2024-01-01~"
        )).isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> invokePrivate(
                "getFiltersByColumnAliasNames",
                new Class<?>[]{EntityMetaData.class, boolean.class, String[].class},
                repository.meta("name"), true, new String[]{}
        )).isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> invokePrivate("parseInteger", new Class<?>[]{String.class, String.class}, "age", "oops"))
                .isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> invokePrivate("parseLong", new Class<?>[]{String.class, String.class}, "longScore", "oops"))
                .isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> invokePrivate("parseDouble", new Class<?>[]{String.class, String.class}, "rating", "oops"))
                .isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> invokePrivate("parseFloat", new Class<?>[]{String.class, String.class}, "weight", "oops"))
                .isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> invokePrivate("parseLocalDate", new Class<?>[]{String.class, String.class}, "birthDate", "oops"))
                .isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> invokePrivate("parseLocalDateTime", new Class<?>[]{String.class, String.class}, "createdAt", "oops"))
                .isInstanceOf(ValidationException.class);

        LocalDateTime isoParsed = invokePrivate(
                "parseLocalDateTime",
                new Class<?>[]{String.class, String.class},
                "createdAt", "2024-03-01T10:15:30"
        );
        assertThat(isoParsed).isEqualTo(LocalDateTime.of(2024, 3, 1, 10, 15, 30));

        @SuppressWarnings("unchecked")
        Map.Entry<Expression<?>, Object> dateList = invokePrivate(
                "getFiltersByColumnAliasNames",
                new Class<?>[]{EntityMetaData.class, boolean.class, String[].class},
                repository.meta("birthDate"), true, new String[]{"2024-01-01", "2024-01-02"}
        );
        assertThat((List<?>) dateList.getValue()).hasSize(2);

        @SuppressWarnings("unchecked")
        Map.Entry<Expression<?>, Object> dateTimeList = invokePrivate(
                "getFiltersByColumnAliasNames",
                new Class<?>[]{EntityMetaData.class, boolean.class, String[].class},
                repository.meta("createdAt"), true, new String[]{"2024-01-01 00:00:00", "2024-01-02 00:00:00"}
        );
        assertThat((List<?>) dateTimeList.getValue()).hasSize(2);

        @SuppressWarnings("unchecked")
        Map.Entry<Expression<?>, Object> booleanList = invokePrivate(
                "getFiltersByColumnAliasNames",
                new Class<?>[]{EntityMetaData.class, boolean.class, String[].class},
                repository.meta("active"), true, new String[]{"true", "false"}
        );
        assertThat((List<Boolean>) booleanList.getValue()).containsExactly(Boolean.TRUE, Boolean.FALSE);

        Integer unpagedLimit = invokePrivate("getPageLimit", new Class<?>[]{long.class, Pageable.class}, 5L, Pageable.unpaged());
        Integer unpagedOffset = invokePrivate("getPageOffset", new Class<?>[]{Pageable.class}, Pageable.unpaged());
        assertThat(unpagedLimit).isEqualTo(5);
        assertThat(unpagedOffset).isZero();

        OrderSpecifier<?>[] nullSort = repository.exposeUpdateSortWithDomainNames(null);
        assertThat(nullSort).hasSize(1);
        assertThat(nullSort[0].toString()).contains("repositoryTestEntity.createdAt");
    }

    @SuppressWarnings("unchecked")
    private <T> T invokePrivate(String methodName, Class<?>[] parameterTypes, Object... args) {
        try {
            Method method = BaseRepositoryImpl.class.getDeclaredMethod(methodName, parameterTypes);
            method.setAccessible(true);
            return (T) method.invoke(repository, args);
        } catch (InvocationTargetException exception) {
            Throwable cause = exception.getCause();
            if (cause instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw new IllegalStateException(cause);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private List<Long> idsFor(Map<String, FilterDetails> filters) {
        Predicate predicate = repository.exposeBuildPredicate(filters);
        return queryFactory
                .select(Expressions.numberPath(Long.class, ROOT, "id"))
                .from(ROOT)
                .where(predicate)
                .orderBy(Expressions.numberPath(Long.class, ROOT, "id").asc())
                .fetch();
    }

    private static final class RepositoryTestBaseImpl extends BaseRepositoryImpl<RepositoryTestEntity, Long> {

        private Set<String> appliedDomains = Collections.emptySet();

        private RepositoryTestBaseImpl(EntityManager entityManager, JPAQueryFactory jpaQueryFactory) {
            super(RepositoryTestEntity.class, entityManager, jpaQueryFactory);
        }

        @Override
        protected Map<String, EntityMetaData> getEntityMetaDataBySuppliedFilter() {
            return new LinkedHashMap<>(Map.of(
                    "name", new EntityMetaData("name", "repositoryTestEntity.name", "repositoryTestEntity", FilterColumnType.STRING),
                    "age", new EntityMetaData("age", "repositoryTestEntity.age", "repositoryTestEntity", FilterColumnType.INTEGER),
                    "longScore", new EntityMetaData("longScore", "repositoryTestEntity.longScore", "repositoryTestEntity", FilterColumnType.LONG),
                    "rating", new EntityMetaData("rating", "repositoryTestEntity.rating", "repositoryTestEntity", FilterColumnType.DOUBLE),
                    "weight", new EntityMetaData("weight", "repositoryTestEntity.weight", "repositoryTestEntity", FilterColumnType.FLOAT),
                    "birthDate", new EntityMetaData("birthDate", "repositoryTestEntity.birthDate", "repositoryTestEntity", FilterColumnType.DATE),
                    "createdAt", new EntityMetaData("createdAt", "repositoryTestEntity.createdAt", "repositoryTestEntity", FilterColumnType.DATE_TIME),
                    "active", new EntityMetaData("active", "repositoryTestEntity.active", "repositoryTestEntity", FilterColumnType.BOOLEAN),
                    "id", new EntityMetaData("id", "repositoryTestEntity.id", "repositoryTestEntity", FilterColumnType.LONG)
            ));
        }

        @Override
        protected void applyFiltersBasedOnSearch(JPQLQuery<?> query, Set<String> domainsFromFilters) {
            this.appliedDomains = new LinkedHashSet<>(domainsFromFilters);
        }

        @Override
        protected boolean hasPKFieldsExistsInSearch(Map<String, FilterDetails> filterDetailsBySearchField) {
            return filterDetailsBySearchField.containsKey("id");
        }

        @Override
        protected Map<String, String> getSortParametersByDomainName() {
            return new LinkedHashMap<>(Map.of(
                    "name", "repositoryTestEntity.name",
                    "age", "repositoryTestEntity.age",
                    "createdAt", "repositoryTestEntity.createdAt",
                    "id", "repositoryTestEntity.id"
            ));
        }

        private Predicate exposeBuildPredicate(Map<String, FilterDetails> filterDetailsBySearchField) {
            return buildPredicate(filterDetailsBySearchField);
        }

        private Page<RepositoryTestEntity> exposeMapPageableResponse(DynamicPageable pageable, List<RepositoryTestEntity> domainList) {
            return mapPageableResponse(pageable, domainList);
        }

        private List<Long> exposeGetIds(Map<String, FilterDetails> filterDetailsBySearchField, DynamicPageable pageable) {
            return getIds(this::getIdsBaseQuery, "repositoryTestEntity.id", filterDetailsBySearchField, pageable);
        }

        private OrderSpecifier<?>[] exposeUpdateSortWithDomainNames(Sort sort) {
            return updateSortWithDomainNames(sort);
        }

        private Set<String> getAppliedDomains() {
            return appliedDomains;
        }

        private EntityMetaData meta(String key) {
            return entityMetaDataBySuppliedFilter.get(key);
        }

        private void setMetadata(String key, EntityMetaData metaData) {
            entityMetaDataBySuppliedFilter.put(key, metaData);
        }

        private JPQLQuery<Tuple> getIdsBaseQuery(Expression<?>[] selectFields) {
            return jpaQueryFactory.select(selectFields).from(ROOT);
        }
    }
}
