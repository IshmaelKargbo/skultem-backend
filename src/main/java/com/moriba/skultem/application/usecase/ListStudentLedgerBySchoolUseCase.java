package com.moriba.skultem.application.usecase;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.StudentLedgerDTO;
import com.moriba.skultem.application.dto.StudentLedgerPagedDTO;
import com.moriba.skultem.application.services.StudentLedgerRowMapper;
import com.moriba.skultem.domain.model.StudentLedgerEntry;
import com.moriba.skultem.domain.repository.StudentLedgerEntryRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

/** One page of the school's own student ledger for a year - the platform fee is left out. */
@Service
@Transactional
@RequiredArgsConstructor
public class ListStudentLedgerBySchoolUseCase {

    private final StudentLedgerEntryRepository repo;
    private final ResolveAcademicYearUseCase resolveAcademicYearUseCase;
    private final StudentLedgerRowMapper rowMapper;

    /**
     * @param search      matches the student's name or admission number; ignored when blank
     * @param classId     only students enrolled in this class that year; ignored when blank
     * @param type        an entry type (FEE_ASSINMENT, PAYMENT, ...); ignored when blank
     * @param termId      only entries of this term; ignored when blank
     * @param oldestFirst sort by date ascending instead of the default newest-first
     */
    public StudentLedgerPagedDTO execute(String schoolId, String academicYearId, int page, int size, String search,
            String classId, String type, String termId, boolean oldestFirst) {

        var academicYear = resolveAcademicYearUseCase.execute(schoolId, academicYearId);

        var direction = oldestFirst ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, "paidAt"));

        StudentLedgerEntry.TransactionType entryType = null;
        if (type != null && !type.isBlank()) {
            try {
                entryType = StudentLedgerEntry.TransactionType.valueOf(type.trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Unknown ledger entry type: " + type);
            }
        }

        // Scoped to the resolved year, not every ledger entry the school has ever recorded - it was
        // pulling every entry unfiltered and then throwing if that entry's student didn't have an
        // enrollment in the *active* year specifically, which broke the whole page for any school
        // with more than one year of ledger history.
        Page<StudentLedgerEntry> ledgerPage = repo.searchSchoolEntries(academicYear.getId(), schoolId, search,
                classId, entryType, termId, pageable);

        BigDecimal totalDebit = BigDecimal.ZERO;
        BigDecimal totalCredit = BigDecimal.ZERO;
        for (var entry : ledgerPage.getContent()) {
            totalDebit = totalDebit.add(entry.getDebit());
            totalCredit = totalCredit.add(entry.getCredit());
        }

        List<StudentLedgerDTO> records = ledgerPage.getContent().stream()
                .map(entry -> rowMapper.toDTO(entry, schoolId))
                .toList();

        BigDecimal latestBalance = ledgerPage.getContent().isEmpty()
                ? BigDecimal.ZERO
                : ledgerPage.getContent().get(0).getBalance();

        return new StudentLedgerPagedDTO(
                records,
                totalDebit,
                totalCredit,
                latestBalance,
                ledgerPage.getTotalElements(),
                ledgerPage.getTotalPages(),
                page,
                size
        );
    }
}
