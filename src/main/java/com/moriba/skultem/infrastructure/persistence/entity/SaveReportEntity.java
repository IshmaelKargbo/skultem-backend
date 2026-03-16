package com.moriba.skultem.infrastructure.persistence.entity;

import java.time.Instant;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "save_report")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SaveReportEntity {
    @Id
    private String id;

    private String schoolId;
    private String name;
    private String entity;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "text")
    private String filters;

    private Instant createdAt;
    private Instant updatedAt;
}
