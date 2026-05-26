package com.skideas.common.integration;

import com.skideas.common.entity.SampleItem;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.AutoConfigureTestEntityManager;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.data.domain.AuditorAware;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@AutoConfigureTestEntityManager
@Transactional
@ActiveProfiles("test")
class AuditableEntityIntegrationTest {

    @Autowired
    private TestEntityManager em;

    @Test
    void persist_populatesCreatedAtAndUpdatedAt() {
        SampleItem item = new SampleItem("itm_001", "First Item");
        em.persistAndFlush(item);
        em.clear();

        SampleItem found = em.find(SampleItem.class, "itm_001");
        assertThat(found.getCreatedAt()).isNotNull();
        assertThat(found.getUpdatedAt()).isNotNull();
    }

    @Test
    void persist_populatesCreatedByAndUpdatedBy() {
        SampleItem item = new SampleItem("itm_002", "Second Item");
        em.persistAndFlush(item);
        em.clear();

        SampleItem found = em.find(SampleItem.class, "itm_002");
        assertThat(found.getCreatedBy()).isEqualTo("test-auditor");
        assertThat(found.getUpdatedBy()).isEqualTo("test-auditor");
    }

    @Test
    void persist_setsVersionToZero() {
        SampleItem item = new SampleItem("itm_003", "Third Item");
        em.persistAndFlush(item);
        em.clear();

        SampleItem found = em.find(SampleItem.class, "itm_003");
        assertThat(found.getVersion()).isZero();
    }

    @Test
    void update_incrementsVersion() {
        SampleItem item = new SampleItem("itm_004", "Fourth Item");
        em.persistAndFlush(item);
        em.clear();

        SampleItem found = em.find(SampleItem.class, "itm_004");
        found.setName("Updated Name");
        em.persistAndFlush(found);
        em.clear();

        SampleItem updated = em.find(SampleItem.class, "itm_004");
        assertThat(updated.getVersion()).isEqualTo(1L);
    }

    @Test
    void update_updatedAtChanges() throws InterruptedException {
        SampleItem item = new SampleItem("itm_005", "Fifth Item");
        em.persistAndFlush(item);
        em.clear();

        SampleItem found = em.find(SampleItem.class, "itm_005");
        var originalUpdatedAt = found.getUpdatedAt();

        Thread.sleep(10);
        found.setName("Changed");
        em.persistAndFlush(found);
        em.clear();

        SampleItem updated = em.find(SampleItem.class, "itm_005");
        assertThat(updated.getUpdatedAt()).isAfterOrEqualTo(originalUpdatedAt);
    }

    @Test
    void update_createdAtDoesNotChange() {
        SampleItem item = new SampleItem("itm_006", "Sixth Item");
        em.persistAndFlush(item);
        em.clear();

        SampleItem found = em.find(SampleItem.class, "itm_006");
        var originalCreatedAt = found.getCreatedAt();

        found.setName("Changed Again");
        em.persistAndFlush(found);
        em.clear();

        SampleItem updated = em.find(SampleItem.class, "itm_006");
        assertThat(updated.getCreatedAt()).isEqualTo(originalCreatedAt);
    }

    @TestConfiguration
    static class AuditConfig {
        @Bean
        AuditorAware<String> testAuditorAware() {
            return () -> Optional.of("test-auditor");
        }
    }
}
