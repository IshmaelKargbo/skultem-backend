package com.moriba.skultem.infrastructure.persistence.entity;

import java.time.Instant;
import java.time.LocalDate;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.moriba.skultem.domain.model.Student.EnrollmentType;
import com.moriba.skultem.domain.model.Student.Status;
import com.moriba.skultem.domain.vo.Gender;

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
@Table(name = "students")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentEntity {
    @Id
    private String id;

    @Column(nullable = false)
    private String schoolId;

    @Column(nullable = false)
    private String admissionNumber;

    private LocalDate admissionDate;

    private String givenNames;

    @Column(nullable = false)
    private String familyName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EnrollmentType enrollmentType;
    
    private String previousSchool;
    
    private String nationality;
    
    private String religion;
    
    private String photo;
    
    private String city;
    
    private String street;
    
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String family;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "house_id")
    private HouseEntity house;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id", nullable = false)
    private ParentEntity parent;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private ClassSessionEntity session;

    @Column(name = "last_class")
    private String lastClass;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Gender gender;

    private LocalDate dateOfBirth;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status;

    private Instant createdAt;
    private Instant updatedAt;
}
