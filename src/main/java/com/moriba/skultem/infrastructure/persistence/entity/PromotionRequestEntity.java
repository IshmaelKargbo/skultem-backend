package com.moriba.skultem.infrastructure.persistence.entity;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import com.moriba.skultem.domain.model.PromotionRequest;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "promotion_requests")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PromotionRequestEntity {
    @Id
    private String id;

    @Column(nullable = false)
    private String schoolId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "class_session_id", nullable = false)
    private ClassSessionEntity session;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "class_master_id", nullable = false)
    private ClassMasterEntity master;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "academic_year_id", nullable = false)
    private AcademicYearEntity academicYear;

    @OneToMany(mappedBy = "request", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @OrderBy("id ASC")
    @Builder.Default
    private List<PromotionRequestItemEntity> items = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PromotionRequest.Status status;

    @Column(columnDefinition = "TEXT")
    private String teacherNote;

    @Column(columnDefinition = "TEXT")
    private String returnReason;

    @Column(columnDefinition = "TEXT")
    private String approvalNote;

    private Instant executedAt;

    @Column(nullable = false)
    private int promotedCount;

    @Column(nullable = false)
    private int repeatedCount;

    private Instant createdAt;

    private Instant updatedAt;
}
