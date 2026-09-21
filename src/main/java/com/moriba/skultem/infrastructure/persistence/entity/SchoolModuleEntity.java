package com.moriba.skultem.infrastructure.persistence.entity;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "school_modules")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SchoolModuleEntity {
    @Id
    private String id;

    @Column(nullable = false)
    private String schoolId;

    @Column(name = "module_key", nullable = false)
    private String moduleKey;

    @Column(nullable = false)
    private boolean enabled;

    private String installedBy;
    private Instant installedAt;
    private Instant updatedAt;
}
