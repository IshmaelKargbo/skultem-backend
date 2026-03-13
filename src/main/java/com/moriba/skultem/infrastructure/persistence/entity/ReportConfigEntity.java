package com.moriba.skultem.infrastructure.persistence.entity;

import java.time.Instant;
import java.time.LocalDate;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "report_configs")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReportConfigEntity {
    @Id
    private String id;

    private String schoolId;
    private String name;
    private String type;
    private String format;
    private String classId;
    private String classSessionId;
    private String teacherSubjectId;
    private String termId;
    private LocalDate startDate;
    private LocalDate endDate;
    private Instant createdAt;
    private Instant updatedAt;
}
