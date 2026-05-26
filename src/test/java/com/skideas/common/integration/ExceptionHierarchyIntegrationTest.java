package com.skideas.common.integration;

import com.skideas.common.exception.ExternalServiceException;
import com.skideas.common.exception.ResourceNotFoundException;
import com.skideas.common.exception.SkideasException;
import com.skideas.common.exception.ValidationException;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
class ExceptionHierarchyIntegrationTest {

    @Test
    void resourceNotFoundException_hasCorrectCodeAndMessage() {
        var ex = new ResourceNotFoundException("ChatMessage", 42L);

        assertThat(ex).isInstanceOf(SkideasException.class);
        assertThat(ex.getCode()).isEqualTo("RESOURCE_NOT_FOUND");
        assertThat(ex.getMessage()).contains("ChatMessage").contains("42");
    }

    @Test
    void resourceNotFoundException_customMessage() {
        var ex = new ResourceNotFoundException("item not found");
        assertThat(ex.getCode()).isEqualTo("RESOURCE_NOT_FOUND");
        assertThat(ex.getMessage()).isEqualTo("item not found");
    }

    @Test
    void validationException_singleField() {
        var ex = new ValidationException("email", "must be a valid email address");

        assertThat(ex).isInstanceOf(SkideasException.class);
        assertThat(ex.getCode()).isEqualTo("VALIDATION_ERROR");
        assertThat(ex.getFieldErrors()).hasSize(1);
        assertThat(ex.getFieldErrors().get(0).field()).isEqualTo("email");
        assertThat(ex.getFieldErrors().get(0).reason()).isEqualTo("must be a valid email address");
    }

    @Test
    void validationException_multipleFields() {
        var errors = List.of(
                new ValidationException.FieldError("email", "must not be blank"),
                new ValidationException.FieldError("name", "must not be blank")
        );
        var ex = new ValidationException(errors);

        assertThat(ex.getCode()).isEqualTo("VALIDATION_ERROR");
        assertThat(ex.getFieldErrors()).hasSize(2);
        assertThat(ex.getMessage()).contains("2 field(s)");
    }

    @Test
    void externalServiceException_hasServiceNameInMessage() {
        var ex = new ExternalServiceException("OpenAI", "rate limit exceeded");

        assertThat(ex).isInstanceOf(SkideasException.class);
        assertThat(ex.getCode()).isEqualTo("EXTERNAL_SERVICE_ERROR");
        assertThat(ex.getService()).isEqualTo("OpenAI");
        assertThat(ex.getMessage()).contains("OpenAI").contains("rate limit exceeded");
    }

    @Test
    void externalServiceException_preservesCause() {
        var cause = new RuntimeException("connection timeout");
        var ex = new ExternalServiceException("LocalModel", "inference failed", cause);

        assertThat(ex.getCause()).isSameAs(cause);
        assertThat(ex.getService()).isEqualTo("LocalModel");
    }

    @Test
    void allExceptions_codeIsScreamingSnakeCase() {
        assertThat(new ResourceNotFoundException("X", 1).getCode()).matches("[A-Z][A-Z_]+");
        assertThat(new ValidationException("f", "r").getCode()).matches("[A-Z][A-Z_]+");
        assertThat(new ExternalServiceException("S", "m").getCode()).matches("[A-Z][A-Z_]+");
    }

    @Test
    void allExceptions_areSerializable() throws Exception {
        var ex = new ResourceNotFoundException("Order", 99L);
        var baos = new ByteArrayOutputStream();
        try (var oos = new ObjectOutputStream(baos)) {
            oos.writeObject(ex);
        }
        assertThat(baos.size()).isGreaterThan(0);
    }

    @Test
    void skideasException_throwsAndCatches_asRuntimeException() {
        assertThatThrownBy(() -> { throw new ResourceNotFoundException("User", "u-1"); })
                .isInstanceOf(RuntimeException.class)
                .isInstanceOf(SkideasException.class)
                .hasMessageContaining("User");
    }
}
