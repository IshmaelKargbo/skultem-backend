package com.moriba.skultem.infrastructure.persistence.entity;

import com.moriba.skultem.domain.model.SchemeOfWork.State;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "scheme_of_works")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SchemeOfWorkEntity {
    @Id
    private String id;

    @Column(nullable = false)
    private String schoolId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "term_id", nullable = false)
    private TermEntity term;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_id", nullable = false)
    private SubjectEntity subject;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private ClassSessionEntity session;

    private long weeks;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private State state;

    private Instant createdAt;
    private Instant updatedAt;
}
