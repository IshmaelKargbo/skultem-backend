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
@Table(name = "promotion_configs")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PromotionConfigEntity {
    @Id
    private String id;

    @Column(nullable = false, unique = true)
    private String schoolId;

    private Integer minPassMark;

    @Column(nullable = false)
    private int maxRepeatCount;

    @Column(nullable = false)
    private boolean requireApproval;

    @Column(nullable = false)
    private boolean requireRemarkForPromote;

    private Instant createdAt;

    private Instant updatedAt;
}
