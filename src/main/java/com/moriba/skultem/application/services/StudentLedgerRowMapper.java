package com.moriba.skultem.application.services;

import java.math.BigDecimal;

import org.springframework.stereotype.Component;

import com.moriba.skultem.application.dto.StudentLedgerDTO;
import com.moriba.skultem.domain.model.StudentLedgerEntry;
import com.moriba.skultem.domain.repository.EnrollmentRepository;

import lombok.RequiredArgsConstructor;

/**
 * Turns a ledger entry into the row the ledger tables show - resolving the student's name and class
 * from their enrollment for the entry's year, falling back to their latest one. Shared by the
 * student ledger and the platform fee page so both show the same row the same way.
 */
@Component
@RequiredArgsConstructor
public class StudentLedgerRowMapper {

    private final EnrollmentRepository enrollmentRepo;

    public StudentLedgerDTO toDTO(StudentLedgerEntry entry, String schoolId) {
        var enrollment = enrollmentRepo
                .findByStudentAndAcademicYearAndSchoolId(entry.getStudentId(), entry.getAcademicYearId(), schoolId)
                .or(() -> enrollmentRepo.findTopByStudentAndSchoolIdOrderByCreatedAtDesc(entry.getStudentId(),
                        schoolId))
                .orElse(null);

        String studentName = enrollment != null ? enrollment.getStudent().getName() : "Unknown student";
        String className = enrollment != null && enrollment.getClazz() != null ? enrollment.getClazz().getName()
                : "N/A";

        BigDecimal debit = entry.getDebit();
        BigDecimal credit = entry.getCredit();

        return new StudentLedgerDTO(
                entry.getDate(),
                entry.getTransactionType().name(),
                studentName,
                className,
                entry.getDescription(),
                debit.compareTo(BigDecimal.ZERO) > 0 ? debit : null,
                credit.compareTo(BigDecimal.ZERO) > 0 ? credit : null,
                entry.getBalance());
    }
}
