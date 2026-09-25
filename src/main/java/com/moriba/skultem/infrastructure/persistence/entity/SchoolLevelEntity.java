package com.moriba.skultem.infrastructure.persistence.entity;

import java.time.Instant;

import com.moriba.skultem.domain.vo.Level;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "school_levels")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SchoolLevelEntity {
    @Id
    private String id;

    @Column(nullable = false)
    private String schoolId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Level level;

    private String managementSectionId;

    private Instant createdAt;

    private Instant updatedAt;
}
