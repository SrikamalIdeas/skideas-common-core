package com.skideas.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Version;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;

/**
 * Base class for all auditable JPA entities.
 * <p>
 * Provides: {@code createdAt}, {@code updatedAt}, {@code createdBy}, {@code updatedBy},
 * and an optimistic-locking {@code version} column.
 * <p>
 * Consumer projects must enable JPA auditing:
 * <pre>{@code @EnableJpaAuditing}</pre>
 * and supply an {@code AuditorAware<String>} bean to populate the actor fields.
 */
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class AuditableEntity {

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @CreatedBy
    @Column(name = "created_by", updatable = false, length = 100)
    private String createdBy;

    @LastModifiedBy
    @Column(name = "updated_by", length = 100)
    private String updatedBy;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    // ── Getters ───────────────────────────────────────────────────────────────

    public Instant getCreatedAt() { return createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }

    public String getCreatedBy() { return createdBy; }

    public String getUpdatedBy() { return updatedBy; }

    public Long getVersion() { return version; }

    // ── Package-visible setters for testing ───────────────────────────────────

    void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
