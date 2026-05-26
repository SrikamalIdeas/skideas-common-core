package com.skideas.common.integration;

import com.skideas.common.util.IdGenerator;
import com.skideas.common.util.DateTimeUtils;
import com.skideas.common.util.StringSanitizer;
import com.skideas.common.util.TraceIdProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class UtilityClassesIntegrationTest {

    @Autowired
    private TraceIdProvider traceIdProvider;

    // ── IdGenerator ──────────────────────────────────────────────────────────

    @Test
    void idGenerator_producesCorrectFormat() {
        String id = IdGenerator.generate("usr");
        assertThat(id).startsWith("usr_");
        assertThat(id).hasSize(4 + 32); // "usr_" + 32 hex chars
    }

    @Test
    void idGenerator_idsAreUnique() {
        Set<String> ids = new HashSet<>();
        for (int i = 0; i < 500; i++) ids.add(IdGenerator.generate("msg"));
        assertThat(ids).hasSize(500);
    }

    @Test
    void idGenerator_hasPrefix_works() {
        String id = IdGenerator.generate("ord");
        assertThat(IdGenerator.hasPrefix(id, "ord")).isTrue();
        assertThat(IdGenerator.hasPrefix(id, "usr")).isFalse();
        assertThat(IdGenerator.hasPrefix(null, "ord")).isFalse();
    }

    @Test
    void idGenerator_blankPrefix_throwsIllegalArgument() {
        assertThatThrownBy(() -> IdGenerator.generate("  "))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ── DateTimeUtils ─────────────────────────────────────────────────────────

    @Test
    void dateTimeUtils_roundTrip_isoString() {
        Instant now = DateTimeUtils.nowUtc().truncatedTo(java.time.temporal.ChronoUnit.SECONDS);
        String iso = DateTimeUtils.toIsoString(now);
        Instant parsed = DateTimeUtils.fromIsoString(iso);
        assertThat(parsed).isEqualTo(now);
    }

    @Test
    void dateTimeUtils_todayUtc_isNotNull() {
        assertThat(DateTimeUtils.todayUtc()).isNotNull();
    }

    @Test
    void dateTimeUtils_isBefore_works() {
        Instant past = Instant.parse("2020-01-01T00:00:00Z");
        Instant future = Instant.parse("2099-01-01T00:00:00Z");
        assertThat(DateTimeUtils.isBefore(past, future)).isTrue();
        assertThat(DateTimeUtils.isBefore(future, past)).isFalse();
        assertThat(DateTimeUtils.isBefore(null, future)).isFalse();
    }

    @Test
    void dateTimeUtils_nullInput_throwsIllegalArgument() {
        assertThatThrownBy(() -> DateTimeUtils.toIsoString(null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> DateTimeUtils.fromIsoString(null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> DateTimeUtils.fromIsoString("not-a-date"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ── StringSanitizer ───────────────────────────────────────────────────────

    @Test
    void stringSanitizer_trimsAndCollapsesWhitespace() {
        assertThat(StringSanitizer.sanitize("  hello   world  ")).isEqualTo("hello world");
    }

    @Test
    void stringSanitizer_nullOrBlank_returnsEmpty() {
        assertThat(StringSanitizer.sanitize(null)).isEmpty();
        assertThat(StringSanitizer.sanitize("   ")).isEmpty();
    }

    @Test
    void stringSanitizer_truncatesToMaxLength() {
        String long100 = "a".repeat(200);
        assertThat(StringSanitizer.sanitize(long100, 50)).hasSize(50);
    }

    @Test
    void stringSanitizer_invalidMaxLength_throwsIllegalArgument() {
        assertThatThrownBy(() -> StringSanitizer.sanitize("hello", 0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void stringSanitizer_hasContent_works() {
        assertThat(StringSanitizer.hasContent("hello")).isTrue();
        assertThat(StringSanitizer.hasContent("   ")).isFalse();
        assertThat(StringSanitizer.hasContent(null)).isFalse();
    }

    @Test
    void stringSanitizer_mask_works() {
        assertThat(StringSanitizer.mask("secret123", 3)).isEqualTo("******123");
        assertThat(StringSanitizer.mask("abc", 10)).isEqualTo("***");
        assertThat(StringSanitizer.mask(null, 3)).isEmpty();
        assertThat(StringSanitizer.mask("hi", 0)).isEqualTo("**");
    }

    @Test
    void stringSanitizer_negativeVisibleChars_throwsIllegalArgument() {
        assertThatThrownBy(() -> StringSanitizer.mask("hello", -1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ── TraceIdProvider ───────────────────────────────────────────────────────

    @Test
    void traceIdProvider_init_setsAndReturnsUuid() {
        String id = traceIdProvider.init();
        assertThat(id).isNotBlank();
        assertThat(traceIdProvider.get()).isEqualTo(id);
        traceIdProvider.clear();
    }

    @Test
    void traceIdProvider_initWithSuppliedId_propagatesIt() {
        String supplied = "upstream-request-abc123";
        traceIdProvider.init(supplied);
        assertThat(traceIdProvider.get()).isEqualTo(supplied);
        traceIdProvider.clear();
    }

    @Test
    void traceIdProvider_afterClear_returnsNoTrace() {
        traceIdProvider.init();
        traceIdProvider.clear();
        assertThat(traceIdProvider.get()).isEqualTo("no-trace");
    }

    @Test
    void traceIdProvider_blankId_throwsIllegalArgument() {
        assertThatThrownBy(() -> traceIdProvider.init("  "))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
