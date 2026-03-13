package com.moriba.skultem.infrastructure.persistence.entity;

import java.time.Instant;

import com.moriba.skultem.domain.model.Clazz.Status;
import com.moriba.skultem.domain.vo.Level;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "classes")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClassEntity {
    @Id
    private String id;

    @Column(nullable = false)
    private String schoolId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, name = "level_order")
    private int levelOrder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assessment_template_id")
    private AssessmentTemplateEntity template;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "next_class")
    private ClassEntity nextClass;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Level level;

    private boolean terminal;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status;

    private Instant createdAt;

    private Instant updatedAt;
}
