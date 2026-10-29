package com.moriba.skultem.infrastructure.persistence.entity;

import java.time.Instant;
import java.time.LocalDate;

import com.moriba.skultem.domain.model.LibraryLoan.BorrowerType;
import com.moriba.skultem.domain.model.LibraryLoan.Status;

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
@Table(name = "library_loans")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LibraryLoanEntity {
    @Id
    private String id;

    @Column(nullable = false)
    private String schoolId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "book_id", nullable = false)
    private LibraryBookEntity book;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BorrowerType borrowerType;

    private String studentId;
    private String teacherId;

    @Column(nullable = false)
    private String borrowerName;

    private String borrowerClass;

    @Column(nullable = false)
    private LocalDate issueDate;

    @Column(nullable = false)
    private LocalDate dueDate;

    private Instant returnedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status;

    private Instant notifiedAt;
    private Instant createdAt;
    private Instant updatedAt;
}
