package com.skideas.common.repository;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.Expression;
import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.NumberPath;
import com.querydsl.jpa.JPQLQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.skideas.common.exception.ValidationException;
import jakarta.persistence.EntityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.support.SimpleJpaRepository;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import java.text.MessageFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Reusable QueryDSL-backed base repository for skideas projects.
 *
 * <p>Subclasses must provide four pieces of project-specific wiring:</p>
 * <ul>
 *   <li>{@link #getEntityMetaDataBySuppliedFilter()} maps API filter names to QueryDSL path aliases and column types.</li>
 *   <li>{@link #getSortParametersByDomainName()} maps exposed sort properties to QueryDSL path aliases.</li>
 *   <li>{@link #applyFiltersBasedOnSearch(JPQLQuery, Set)} adds any joins required for aliases referenced by filters.</li>
 *   <li>{@link #hasPKFieldsExistsInSearch(Map)} tells the concrete repository whether the search already contains primary-key style filters.</li>
 * </ul>
 *
 * <p>The base class supplies predicate building, pageable response mapping, paginated ID selection,
 * and sort translation. Concrete repositories are still responsible for assembling their entity fetch queries.</p>
 */
public abstract class BaseRepositoryImpl<D, I> extends SimpleJpaRepository<D, I> implements BaseRepository<D, I> {

    protected static final String DEFAULT_SORT_COLUMN = "createdAt";
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    protected final Logger logger = LoggerFactory.getLogger(getClass());
    protected final Class<D> domainClass;
    protected final JPAQueryFactory jpaQueryFactory;
    protected final Map<String, EntityMetaData> entityMetaDataBySuppliedFilter;
    protected final Map<String, String> sortParametersByDomainName;

    protected BaseRepositoryImpl(Class<D> domainClass, EntityManager entityManager, JPAQueryFactory jpaQueryFactory) {
        super(domainClass, entityManager);
        this.domainClass = domainClass;
        this.jpaQueryFactory = jpaQueryFactory;
        this.entityMetaDataBySuppliedFilter = Objects.requireNonNullElseGet(getEntityMetaDataBySuppliedFilter(), Collections::emptyMap);
        this.sortParametersByDomainName = Objects.requireNonNullElseGet(getSortParametersByDomainName(), Collections::emptyMap);
    }

    protected Predicate buildPredicate(Map<String, FilterDetails> filterDetailsBySearchField) {
        if (CollectionUtils.isEmpty(filterDetailsBySearchField)) {
            return null;
        }

        try {
            BooleanBuilder queryParamPredicate = new BooleanBuilder();
            for (Map.Entry<String, FilterDetails> entry : filterDetailsBySearchField.entrySet()) {
                String searchField = entry.getKey();
                FilterDetails filterDetails = entry.getValue();

                if (filterDetails == null) {
                    throw new ValidationException(searchField, "Filter details cannot be null");
                }
                if (!StringUtils.hasText(filterDetails.getField())) {
                    filterDetails.setField(searchField);
                }
                filterDetails.validate();

                EntityMetaData entityMetaData = entityMetaDataBySuppliedFilter.get(searchField);
                if (entityMetaData == null || !entityMetaData.isFilterable()) {
                    throw new ValidationException(searchField, "Filter field is not supported");
                }

                FilterOperationsEnum operation = filterDetails.getOperator();
                String value = filterDetails.getValue();
                validateFilterValue(searchField, value);
                if (operation == FilterOperationsEnum.LIKE) {
                    value = "%" + value.replace("*", "%") + "%";
                }

                Map.Entry<Expression<?>, Object> filterEntry;
                switch (operation) {
                    case BETWEEN:
                        filterEntry = getFiltersByColumnAliasNamesForBetween(entityMetaData, value);
                        queryParamPredicate.and(getPredicateForBetween(filterEntry));
                        break;
                    case IN:
                    case NOT_IN:
                        filterEntry = getFiltersByColumnAliasNames(entityMetaData, true, splitValues(searchField, value));
                        queryParamPredicate.and(getPredicate(operation, filterEntry));
                        break;
                    default:
                        filterEntry = getFiltersByColumnAliasNames(entityMetaData, value);
                        queryParamPredicate.and(getPredicate(operation, filterEntry));
                        break;
                }
            }
            return queryParamPredicate;
        } catch (ValidationException | IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            String errorMessage = "Unexpected error building predicate for " + domainClass.getSimpleName();
            logger.error(errorMessage, e);
            throw new IllegalStateException(errorMessage, e);
        }
    }

    private Map.Entry<Expression<?>, Object> getFiltersByColumnAliasNamesForBetween(EntityMetaData entityMetaData, String values) {
        if (!values.contains("~")) {
            throw new ValidationException(entityMetaData.getField(), "BETWEEN operator requires value in format 'value1~value2'");
        }

        String[] betweenValues = Arrays.stream(values.split("~", -1))
                .map(String::trim)
                .toArray(String[]::new);
        if (betweenValues.length != 2 || !StringUtils.hasText(betweenValues[0]) || !StringUtils.hasText(betweenValues[1])) {
            throw new ValidationException(entityMetaData.getField(), "BETWEEN operator requires exactly two non-empty values");
        }

        return switch (entityMetaData.getColumnType()) {
            case DATE -> Map.entry(
                    buildExpression(entityMetaData),
                    List.of(parseLocalDate(entityMetaData.getField(), betweenValues[0]), parseLocalDate(entityMetaData.getField(), betweenValues[1]))
            );
            case DATE_TIME -> Map.entry(
                    buildExpression(entityMetaData),
                    List.of(parseLocalDateTime(entityMetaData.getField(), betweenValues[0]), parseLocalDateTime(entityMetaData.getField(), betweenValues[1]))
            );
            default -> {
                String formattedMessage = MessageFormat.format(
                        "Filter type {0} data type is not supported with between operator",
                        entityMetaData.getColumnType()
                );
                logger.warn(formattedMessage);
                throw new IllegalArgumentException(formattedMessage);
            }
        };
    }

    private Map.Entry<Expression<?>, Object> getFiltersByColumnAliasNames(EntityMetaData entityMetaData, String... valuesArray) {
        return getFiltersByColumnAliasNames(entityMetaData, false, valuesArray);
    }

    private Map.Entry<Expression<?>, Object> getFiltersByColumnAliasNames(
            EntityMetaData entityMetaData,
            boolean forceCollection,
            String... valuesArray
    ) {
        if (valuesArray == null || valuesArray.length == 0) {
            throw new ValidationException(entityMetaData.getField(), "Filter value cannot be null or empty");
        }

        boolean isMultipleValues = forceCollection || valuesArray.length > 1;
        return switch (entityMetaData.getColumnType()) {
            case STRING -> Map.entry(
                    buildExpression(entityMetaData),
                    isMultipleValues ? Arrays.stream(valuesArray).map(String::trim).toList() : valuesArray[0].trim()
            );
            case INTEGER -> Map.entry(
                    buildExpression(entityMetaData),
                    isMultipleValues
                            ? Arrays.stream(valuesArray).map(value -> parseInteger(entityMetaData.getField(), value)).toList()
                            : parseInteger(entityMetaData.getField(), valuesArray[0])
            );
            case LONG -> Map.entry(
                    buildExpression(entityMetaData),
                    isMultipleValues
                            ? Arrays.stream(valuesArray).map(value -> parseLong(entityMetaData.getField(), value)).toList()
                            : parseLong(entityMetaData.getField(), valuesArray[0])
            );
            case DOUBLE -> Map.entry(
                    buildExpression(entityMetaData),
                    isMultipleValues
                            ? Arrays.stream(valuesArray).map(value -> parseDouble(entityMetaData.getField(), value)).toList()
                            : parseDouble(entityMetaData.getField(), valuesArray[0])
            );
            case FLOAT -> Map.entry(
                    buildExpression(entityMetaData),
                    isMultipleValues
                            ? Arrays.stream(valuesArray).map(value -> parseFloat(entityMetaData.getField(), value)).toList()
                            : parseFloat(entityMetaData.getField(), valuesArray[0])
            );
            case DATE -> Map.entry(
                    buildExpression(entityMetaData),
                    isMultipleValues
                            ? Arrays.stream(valuesArray).map(value -> parseLocalDate(entityMetaData.getField(), value)).toList()
                            : parseLocalDate(entityMetaData.getField(), valuesArray[0])
            );
            case DATE_TIME -> Map.entry(
                    buildExpression(entityMetaData),
                    isMultipleValues
                            ? Arrays.stream(valuesArray).map(value -> parseLocalDateTime(entityMetaData.getField(), value)).toList()
                            : parseLocalDateTime(entityMetaData.getField(), valuesArray[0])
            );
            case BOOLEAN -> Map.entry(
                    buildExpression(entityMetaData),
                    isMultipleValues
                            ? Arrays.stream(valuesArray).map(value -> parseBoolean(entityMetaData.getField(), value)).toList()
                            : parseBoolean(entityMetaData.getField(), valuesArray[0])
            );
        };
    }

    private Expression<?> buildExpression(EntityMetaData entityMetaData) {
        String alias = entityMetaData.getColumnAliasNameByEntityName();
        return switch (entityMetaData.getColumnType()) {
            case STRING -> Expressions.stringPath(alias);
            case INTEGER -> Expressions.numberPath(Integer.class, alias);
            case LONG -> Expressions.numberPath(Long.class, alias);
            case DOUBLE -> Expressions.numberPath(Double.class, alias);
            case FLOAT -> Expressions.numberPath(Float.class, alias);
            case DATE -> Expressions.datePath(LocalDate.class, alias);
            case DATE_TIME -> Expressions.dateTimePath(LocalDateTime.class, alias);
            case BOOLEAN -> Expressions.booleanPath(alias);
        };
    }

    private Predicate getPredicateForBetween(Map.Entry<Expression<?>, Object> filterByColumnAliasNames) {
        List<?> value = (List<?>) filterByColumnAliasNames.getValue();
        return Expressions.predicate(
                FilterOperationsEnum.BETWEEN.getOperation(),
                filterByColumnAliasNames.getKey(),
                Expressions.constant(value.get(0)),
                Expressions.constant(value.get(1))
        );
    }

    private Predicate getPredicate(FilterOperationsEnum operation, Map.Entry<Expression<?>, Object> filterByColumnAliasNames) {
        return Expressions.predicate(
                operation.getOperation(),
                filterByColumnAliasNames.getKey(),
                Expressions.constant(filterByColumnAliasNames.getValue())
        );
    }

    private void validateFilterValue(String field, String value) {
        if (!StringUtils.hasText(value)) {
            throw new ValidationException(field, "Filter value cannot be null or empty");
        }
    }

    private String[] splitValues(String field, String value) {
        String[] values = Arrays.stream(value.split(",", -1))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .toArray(String[]::new);
        if (values.length == 0) {
            throw new ValidationException(field, "Filter value cannot be null or empty");
        }
        return values;
    }

    private Integer parseInteger(String field, String value) {
        try {
            return Integer.valueOf(value.trim());
        } catch (NumberFormatException ex) {
            throw new ValidationException(field, "Invalid integer value: " + value);
        }
    }

    private Long parseLong(String field, String value) {
        try {
            return Long.valueOf(value.trim());
        } catch (NumberFormatException ex) {
            throw new ValidationException(field, "Invalid long value: " + value);
        }
    }

    private Double parseDouble(String field, String value) {
        try {
            return Double.valueOf(value.trim());
        } catch (NumberFormatException ex) {
            throw new ValidationException(field, "Invalid double value: " + value);
        }
    }

    private Float parseFloat(String field, String value) {
        try {
            return Float.valueOf(value.trim());
        } catch (NumberFormatException ex) {
            throw new ValidationException(field, "Invalid float value: " + value);
        }
    }

    private LocalDate parseLocalDate(String field, String value) {
        try {
            return LocalDate.parse(value.trim());
        } catch (DateTimeParseException ex) {
            throw new ValidationException(field, "Invalid date value: " + value);
        }
    }

    private LocalDateTime parseLocalDateTime(String field, String value) {
        try {
            return LocalDateTime.parse(value.trim(), DATE_TIME_FORMATTER);
        } catch (DateTimeParseException firstException) {
            try {
                return LocalDateTime.parse(value.trim());
            } catch (DateTimeParseException secondException) {
                throw new ValidationException(field, "Invalid date time value: " + value);
            }
        }
    }

    private Boolean parseBoolean(String field, String value) {
        String trimmedValue = value.trim();
        if (!"true".equalsIgnoreCase(trimmedValue) && !"false".equalsIgnoreCase(trimmedValue)) {
            throw new ValidationException(field, "Invalid boolean value: " + value);
        }
        return Boolean.parseBoolean(trimmedValue);
    }

    protected Page<D> mapPageableResponse(DynamicPageable pageable, List<D> domainList) {
        if (CollectionUtils.isEmpty(domainList)) {
            domainList = Collections.emptyList();
            pageable.setTotalRecords(0L);
        }

        Pageable requestedPageable = pageable.getPageable();
        return new PageImpl<>(
                domainList,
                PageRequest.of(requestedPageable.getPageNumber(), requestedPageable.getPageSize(), requestedPageable.getSort()),
                pageable.getTotalRecords()
        );
    }

    protected List<Long> getIds(
            Function<Expression<?>[], JPQLQuery<Tuple>> idQueryFunction,
            String idColumnName,
            Map<String, FilterDetails> filterDetailsBySearchField,
            DynamicPageable pageable
    ) {
        NumberPath<Long> idPath = Expressions.numberPath(Long.class, idColumnName);
        JPQLQuery<Tuple> idsQueryWithoutSortFields = getIdsQuery(idQueryFunction, filterDetailsBySearchField, idPath);
        long totalRecords = idsQueryWithoutSortFields.fetchCount();
        pageable.setTotalRecords(totalRecords);
        if (totalRecords <= 0) {
            return Collections.emptyList();
        }

        Pageable springPageable = pageable.getPageable();
        JPQLQuery<Tuple> idsQuery = getIdsQueryWithSortFieldsAndFilters(idQueryFunction, idPath, filterDetailsBySearchField, springPageable.getSort());
        if (springPageable.isPaged()) {
            idsQuery.offset(getPageOffset(springPageable));
            idsQuery.limit(getPageLimit(totalRecords, springPageable));
        }
        idsQuery.orderBy(updateSortWithDomainNames(springPageable.getSort()));
        return idsQuery.fetch().stream()
                .map(tuple -> tuple.get(idPath))
                .collect(Collectors.toList());
    }

    private JPQLQuery<Tuple> getIdsQuery(
            Function<Expression<?>[], JPQLQuery<Tuple>> idQueryFunction,
            Map<String, FilterDetails> filterDetailsBySearchField,
            Expression<?>... selectFields
    ) {
        JPQLQuery<Tuple> finalQuery = idQueryFunction.apply(selectFields).distinct();
        Set<String> uniqueDomainNames = CollectionUtils.isEmpty(filterDetailsBySearchField)
                ? Collections.emptySet()
                : filterDetailsBySearchField.keySet().stream()
                .map(entityMetaDataBySuppliedFilter::get)
                .filter(Objects::nonNull)
                .map(EntityMetaData::getEntityName)
                .filter(StringUtils::hasText)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        applyFiltersBasedOnSearch(finalQuery, uniqueDomainNames);
        Predicate predicate = buildPredicate(filterDetailsBySearchField);
        if (predicate != null) {
            finalQuery.where(predicate);
        }
        return finalQuery;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private JPQLQuery<Tuple> getIdsQueryWithSortFieldsAndFilters(
            Function<Expression<?>[], JPQLQuery<Tuple>> idQueryFunction,
            NumberPath<Long> idPath,
            Map<String, FilterDetails> filterDetailsBySearchField,
            Sort sort
    ) {
        List<Expression<?>> selectFields = new ArrayList<>();
        selectFields.add(idPath);
        if (sort == null || sort.isUnsorted()) {
            String defaultSortField = sortParametersByDomainName.getOrDefault(DEFAULT_SORT_COLUMN, DEFAULT_SORT_COLUMN);
            if (StringUtils.hasText(defaultSortField)) {
                selectFields.add(Expressions.comparablePath(Comparable.class, defaultSortField));
            }
        } else {
            for (Sort.Order order : sort) {
                String sortField = sortParametersByDomainName.getOrDefault(order.getProperty(), order.getProperty());
                if (StringUtils.hasText(sortField)) {
                    selectFields.add(Expressions.comparablePath(Comparable.class, sortField));
                }
            }
        }
        return getIdsQuery(idQueryFunction, filterDetailsBySearchField, selectFields.toArray(Expression[]::new));
    }

    private int getPageLimit(long maxCount, Pageable pageable) {
        return (!ObjectUtils.isEmpty(pageable) && pageable.isPaged())
                ? (int) Math.min(maxCount, pageable.getPageSize())
                : (int) maxCount;
    }

    private int getPageOffset(Pageable pageable) {
        return (!ObjectUtils.isEmpty(pageable) && pageable.isPaged()) ? (int) pageable.getOffset() : 0;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    protected OrderSpecifier<?>[] updateSortWithDomainNames(Sort sort) {
        Sort effectiveSort = (sort == null || sort.isUnsorted())
                ? Sort.by(DEFAULT_SORT_COLUMN).descending()
                : sort;

        List<OrderSpecifier<?>> orderSpecifiers = new ArrayList<>();
        for (Sort.Order order : effectiveSort) {
            String domainName = sortParametersByDomainName.getOrDefault(order.getProperty(), order.getProperty());
            if (StringUtils.hasText(domainName)) {
                orderSpecifiers.add(new OrderSpecifier(
                        order.getDirection() == Sort.Direction.ASC ? Order.ASC : Order.DESC,
                        Expressions.comparablePath(Comparable.class, domainName)
                ));
            }
        }
        return orderSpecifiers.toArray(OrderSpecifier[]::new);
    }

    protected abstract Map<String, EntityMetaData> getEntityMetaDataBySuppliedFilter();

    protected abstract void applyFiltersBasedOnSearch(JPQLQuery<?> query, Set<String> domainsFromFilters);

    protected abstract boolean hasPKFieldsExistsInSearch(Map<String, FilterDetails> filterDetailsBySearchField);

    protected abstract Map<String, String> getSortParametersByDomainName();

    @Override
    public Page<D> getCollections(Map<String, FilterDetails> filterDetailsBySearchField, DynamicPageable pageable) {
        return getCollections(filterDetailsBySearchField, pageable, Collections.emptyList());
    }

    @Override
    public Page<D> getCollections(
            Map<String, FilterDetails> filterDetailsBySearchField,
            DynamicPageable pageable,
            List<EmbedEnum> embeds
    ) {
        throw new UnsupportedOperationException(
                "Concrete repositories must implement getCollections(...) with their project-specific fetch query"
        );
    }
}
