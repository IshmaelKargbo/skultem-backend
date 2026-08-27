package com.moriba.skultem.application.usecase;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.StudentLedgerDTO;
import com.moriba.skultem.application.dto.StudentLedgerPagedDTO;
import com.moriba.skultem.domain.model.StudentLedgerEntry;
import com.moriba.skultem.domain.repository.EnrollmentRepository;
import com.moriba.skultem.domain.repository.StudentLedgerEntryRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class ListStudentLedgerBySchoolUseCase {

    private final StudentLedgerEntryRepository repo;
    private final ResolveAcademicYearUseCase resolveAcademicYearUseCase;
    private final EnrollmentRepository enrollmentRepo;

    public StudentLedgerPagedDTO execute(String schoolId, String academicYearId, int page, int size) {

        var academicYear = resolveAcademicYearUseCase.execute(schoolId, academicYearId);

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "paidAt"));

        // Scoped to the resolved year, not every ledger entry the school has ever recorded - it was
        // pulling every entry unfiltered and then throwing if that entry's student didn't have an
        // enrollment in the *active* year specifically, which broke the whole page for any school
        // with more than one year of ledger history.
        Page<StudentLedgerEntry> ledgerPage =
                repo.findAllByAcademicYearAndSchool(academicYear.getId(), schoolId, pageable);

        var totals = new Object() {
            BigDecimal totalDebit = BigDecimal.ZERO;
            BigDecimal totalCredit = BigDecimal.ZERO;
        };

        List<StudentLedgerDTO> records = ledgerPage.getContent()
                .stream()
                .map(entry -> {
                    var enrollment = enrollmentRepo
                            .findByStudentAndAcademicYearAndSchoolId(entry.getStudentId(), academicYear.getId(),
                                    schoolId)
                            .or(() -> enrollmentRepo.findTopByStudentAndSchoolIdOrderByCreatedAtDesc(
                                    entry.getStudentId(), schoolId))
                            .orElse(null);

                    String studentName = enrollment != null ? enrollment.getStudent().getName() : "Unknown student";
                    String className = enrollment != null && enrollment.getClazz() != null
                            ? enrollment.getClazz().getName()
                            : "N/A";

                    BigDecimal debit = entry.getDebit();
                    BigDecimal credit = entry.getCredit();

                    totals.totalDebit = totals.totalDebit.add(debit);
                    totals.totalCredit = totals.totalCredit.add(credit);

                    return new StudentLedgerDTO(
                            entry.getDate(),
                            entry.getTransactionType().name(),
                            studentName,
                            className,
                            entry.getDescription(),
                            debit.compareTo(BigDecimal.ZERO) > 0 ? debit : null,
                            credit.compareTo(BigDecimal.ZERO) > 0 ? credit : null,
                            entry.getBalance()
                    );
                })
                .collect(Collectors.toList());

        BigDecimal latestBalance = ledgerPage.getContent().isEmpty()
                ? BigDecimal.ZERO
                : ledgerPage.getContent().get(0).getBalance();

        return new StudentLedgerPagedDTO(
                records,
                totals.totalDebit,
                totals.totalCredit,
                latestBalance,
                ledgerPage.getTotalElements(),
                ledgerPage.getTotalPages(),
                page,
                size
        );
    }
}