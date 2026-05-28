package com.skideas.common.integration;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "repository_test_entity")
public class RepositoryTestEntity {

    @Id
    private Long id;

    @Column(nullable = false)
    private String name;

    private Integer age;

    @Column(name = "long_score")
    private Long longScore;

    private Double rating;

    private Float weight;

    private LocalDate birthDate;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    private Boolean active;

    protected RepositoryTestEntity() {
    }

    RepositoryTestEntity(
            Long id,
            String name,
            Integer age,
            Long longScore,
            Double rating,
            Float weight,
            LocalDate birthDate,
            LocalDateTime createdAt,
            Boolean active
    ) {
        this.id = id;
        this.name = name;
        this.age = age;
        this.longScore = longScore;
        this.rating = rating;
        this.weight = weight;
        this.birthDate = birthDate;
        this.createdAt = createdAt;
        this.active = active;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Integer getAge() {
        return age;
    }

    public Long getLongScore() {
        return longScore;
    }

    public Double getRating() {
        return rating;
    }

    public Float getWeight() {
        return weight;
    }

    public LocalDate getBirthDate() {
        return birthDate;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public Boolean getActive() {
        return active;
    }
}
