package com.skideas.common.integration;

import com.skideas.common.exception.ValidationException;
import com.skideas.common.repository.DynamicPageable;
import com.skideas.common.repository.EntityMetaData;
import com.skideas.common.repository.FilterColumnType;
import com.skideas.common.repository.FilterDetails;
import com.skideas.common.repository.FilterOperationsEnum;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RepositorySupportTypesTest {

    @Test
    void filterDetails_validate_acceptsSupportedOperators() {
        assertThatNoException().isThrownBy(() -> new FilterDetails("name", FilterOperationsEnum.LIKE, "ali").validate());
        assertThatNoException().isThrownBy(() -> new FilterDetails("name", FilterOperationsEnum.IN, "alice,bob").validate());
        assertThatNoException().isThrownBy(() -> new FilterDetails("createdAt", FilterOperationsEnum.BETWEEN, "2024-01-01~2024-01-31").validate());
        assertThatNoException().isThrownBy(() -> new FilterDetails("age", FilterOperationsEnum.EQ, "20").validate());
    }

    @Test
    void filterDetails_validate_rejectsInvalidInputs() {
        assertThatThrownBy(() -> new FilterDetails("", FilterOperationsEnum.EQ, "x").validate())
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("field");
        assertThatThrownBy(() -> new FilterDetails("name", null, "x").validate())
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("name");
        assertThatThrownBy(() -> new FilterDetails("name", FilterOperationsEnum.EQ, " ").validate())
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("name");
        assertThatThrownBy(() -> new FilterDetails("createdAt", FilterOperationsEnum.BETWEEN, "2024-01-01").validate())
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("BETWEEN");
    }

    @Test
    void enums_and_metadata_exposeExpectedValues() {
        assertThat(FilterOperationsEnum.fromCode("lte")).isEqualTo(FilterOperationsEnum.LTE);
        assertThatThrownBy(() -> FilterOperationsEnum.fromCode("missing"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown filter operation");
        assertThat(FilterOperationsEnum.LIKE.getCode()).isEqualTo("like");
        assertThat(FilterOperationsEnum.LIKE.getDescription()).contains("Pattern matching");
        assertThat(FilterOperationsEnum.LIKE.getOperation()).isNotNull();
        assertThat(FilterColumnType.DATE_TIME.getDescription()).contains("yyyy-MM-dd HH:mm:ss");
        assertThat(com.skideas.common.repository.EmbedEnum.values()).isEmpty();

        EntityMetaData metaData = new EntityMetaData();
        metaData.setField("name");
        metaData.setColumnAliasNameByEntityName("sample.name");
        metaData.setEntityName("sample");
        metaData.setColumnType(FilterColumnType.STRING);
        assertThat(metaData.getField()).isEqualTo("name");
        assertThat(metaData.getColumnAliasNameByEntityName()).isEqualTo("sample.name");
        assertThat(metaData.getEntityName()).isEqualTo("sample");
        assertThat(metaData.getColumnType()).isEqualTo(FilterColumnType.STRING);
        assertThat(metaData.isFilterable()).isTrue();
        assertThat(metaData.toString()).contains("name").contains("sample");

        EntityMetaData incomplete = new EntityMetaData("name", null, "sample", FilterColumnType.STRING);
        assertThat(incomplete.isFilterable()).isFalse();
    }

    @Test
    void dynamicPageable_tracksPageRequestAndTotalRecords() {
        DynamicPageable pageable = new DynamicPageable(PageRequest.of(1, 5));

        assertThat(pageable.getPageable().getPageNumber()).isEqualTo(1);
        assertThat(pageable.getTotalRecords()).isZero();

        pageable.setTotalRecords(42L);
        assertThat(pageable.getTotalRecords()).isEqualTo(42L);
    }

    @Test
    void filterDetails_toString_includesKeyParts() {
        FilterDetails filterDetails = new FilterDetails();
        filterDetails.setField("active");
        filterDetails.setOperator(FilterOperationsEnum.EQ);
        filterDetails.setValue("true");

        assertThat(filterDetails.getField()).isEqualTo("active");
        assertThat(filterDetails.getOperator()).isEqualTo(FilterOperationsEnum.EQ);
        assertThat(filterDetails.getValue()).isEqualTo("true");
        assertThat(filterDetails.toString()).contains("active").contains("EQ").contains("true");
    }
}
