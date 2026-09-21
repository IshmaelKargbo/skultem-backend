package com.moriba.skultem.application.usecase;

import java.math.BigDecimal;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.PlatformFeeReportDTO;
import com.moriba.skultem.domain.model.StudentLedgerEntry;
import com.moriba.skultem.domain.repository.StudentLedgerEntryRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

/**
 * A school's platform fee for one academic year: what its students were charged (expected), what
 * they've paid (collected), and what's still to collect. Worked out from the platform fee's own
 * ledger entries, so it matches the entries listed on the platform fee page exactly.
 */
@Service
@Transactional
@RequiredArgsConstructor
public class PlatformFeeReportUseCase {

    private final StudentLedgerEntryRepository repo;
    private final ResolveAcademicYearUseCase resolveAcademicYearUseCase;

    public PlatformFeeReportDTO execute(String schoolId, String academicYearId) {
        var academicYear = resolveAcademicYearUseCase.execute(schoolId, academicYearId);

        var entries = repo.findPlatformEntries(academicYear.getId(), schoolId, Pageable.unpaged()).getContent();

        BigDecimal expected = entries.stream()
                .map(StudentLedgerEntry::getDebit)
                .filter(d -> d != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal collected = entries.stream()
                .map(StudentLedgerEntry::getCredit)
                .filter(c -> c != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal outstanding = expected.subtract(collected).max(BigDecimal.ZERO);

        return new PlatformFeeReportDTO(expected.longValue(), collected.longValue(), outstanding.longValue());
    }
}
