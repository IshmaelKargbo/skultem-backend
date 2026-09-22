package com.moriba.skultem.application.dto;

import java.math.BigDecimal;

import com.moriba.skultem.domain.service.FeeCollectionCalculator.Status;

/**
 * One school-fee charge (a StudentFee row), net of any discount and matched against what's been
 * paid - the shared building block every Fees Reporting use case aggregates differently (by
 * student, by class session, by fee category, or all together). Produced by
 * LoadSchoolFeeRowsUseCase; the platform fee is never represented here (see that use case).
 */
public record SchoolFeeRowDTO(
        String studentId,
        String studentName,
        String admissionNumber,
        // clazzId|sectionId|streamId - stable per class-session grouping key. A class session is
        // class + section + stream, e.g. "SSS 1 Science" and "SSS 1 Art" have different keys even
        // when they share the same class/section. A use case that needs the actual ClassSession id
        // (e.g. to link a class-level row to /classes/{id}) resolves it once per distinct key via
        // ClassSessionRepository, rather than every row carrying a redundant lookup.
        String classSessionKey,
        String className,
        String feeCategoryId,
        String feeCategoryName,
        String termId,
        String termName,
        BigDecimal expected,
        BigDecimal paid,
        BigDecimal balance,
        Status status) {
}
