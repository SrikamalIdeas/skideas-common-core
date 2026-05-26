package com.skideas.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Minimal entity used only in integration tests to verify AuditableEntity behaviour.
 */
@Entity
@Table(name = "sample_item")
public class SampleItem extends AuditableEntity {

    @Id
    @Column(name = "id", length = 36)
    private String id;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    protected SampleItem() {}

    public SampleItem(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public String getId() { return id; }

    public String getName() { return name; }

    public void setName(String name) { this.name = name; }
}
