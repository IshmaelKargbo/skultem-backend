package com.moriba.skultem.infrastructure.persistence.entity;

import java.time.Instant;

import com.moriba.skultem.domain.model.AssessmentApprovalRequest;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "assessment_approval_request")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssessmentApprovalRequestEntity {
    @Id
    private String id;

    @Column(nullable = false)
    private String schoolId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "class_master_id", nullable = false)
    private ClassMasterEntity master;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "class_subject_assessment_life_cycle_id", nullable = false)
    private ClassSubjectAssessmentLifeCycleEntity cycle;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "teacher_subject_id", nullable = false)
    private TeacherSubjectEntity teacherSubject;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "term_id", nullable = false)
    private TermEntity term;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AssessmentApprovalRequest.Status status;

    @Column(columnDefinition = "TEXT")
    private String teacherNote;

    @Column(columnDefinition = "TEXT")
    private String returnReason;

    @Column(columnDefinition = "TEXT")
    private String approvalNote;

    private Instant createdAt;

    private Instant updatedAt;
}
