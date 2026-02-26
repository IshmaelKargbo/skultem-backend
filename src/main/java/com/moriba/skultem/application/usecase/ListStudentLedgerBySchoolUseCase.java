package com.moriba.skultem.application.usecase;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.StudentLedgerDTO;
import com.moriba.skultem.application.dto.StudentLedgerPagedDTO;
import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.domain.model.StudentLedgerEntry;
import com.moriba.skultem.domain.repository.AcademicYearRepository;
import com.moriba.skultem.domain.repository.EnrollmentRepository;
import com.moriba.skultem.domain.repository.StudentLedgerEntryRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class ListStudentLedgerBySchoolUseCase {

    private final StudentLedgerEntryRepository repo;
    private final AcademicYearRepository academicYearRepo;
    private final EnrollmentRepository enrollmentRepo;

    public StudentLedgerPagedDTO execute(String schoolId, int page, int size) {

        Pageable pageable = PageRequest.of(page, size);

        Page<StudentLedgerEntry> ledgerPage =
                repo.findAllBySchoolIdOrderByPaidAtDesc(schoolId, pageable);

        var academicYear = academicYearRepo.findActiveBySchool(schoolId)
                .orElseThrow(() -> new NotFoundException("no academic year found"));

        var totals = new Object() {
            BigDecimal totalDebit = BigDecimal.ZERO;
            BigDecimal totalCredit = BigDecimal.ZERO;
        };

        List<StudentLedgerDTO> records = ledgerPage.getContent()
                .stream()
                .map(entry -> {
                    var enrollment = enrollmentRepo
                            .findByStudentAndAcademicYearAndSchoolId(
                                    entry.getStudentId(),
                                    academicYear.getId(),
                                    schoolId
                            )
                            .orElseThrow(() -> new NotFoundException("no enrollment found"));

                    BigDecimal debit = entry.getDebit();
                    BigDecimal credit = entry.getCredit();

                    totals.totalDebit = totals.totalDebit.add(debit);
                    totals.totalCredit = totals.totalCredit.add(credit);

                    return new StudentLedgerDTO(
                            entry.getDate(),
                            entry.getTransactionType().name(),
                            enrollment.getStudent().getName(),
                            enrollment.getClazz().getName(),
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