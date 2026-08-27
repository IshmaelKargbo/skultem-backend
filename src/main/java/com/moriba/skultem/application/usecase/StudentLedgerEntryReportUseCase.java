package com.moriba.skultem.application.usecase;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.ReportBuilderDTO;
import com.moriba.skultem.application.dto.StudentLedgerDTO;
import com.moriba.skultem.domain.repository.EnrollmentRepository;
import com.moriba.skultem.domain.repository.StudentLedgerEntryRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

/**
 * The filter-builder-backed counterpart to {@link ListStudentLedgerBySchoolUseCase} - same rows, same
 * per-row student/class resolution, but driven by the generic report engine ({@code entity: "ledger"})
 * so the Student Ledger page can use the same dynamic filter UI as Transactions instead of a fixed
 * academic-year-only scope. {@link ScopeReportToAcademicYearUseCase} still applies the year scope by
 * default (see its {@code ledger} entry) unless the request brings its own academicYearId filter.
 */
@Service
@Transactional
@RequiredArgsConstructor
public class StudentLedgerEntryReportUseCase {

    private final StudentLedgerEntryRepository repo;
    private final EnrollmentRepository enrollmentRepo;

    public Page<StudentLedgerDTO> execute(ReportBuilderDTO request, int page, int size) {
        Pageable pageable = createPageable(page, size);

        List<com.moriba.skultem.domain.vo.Filter> filters = request.filters();

        return repo.runReport(request.schoolId(), filters, pageable).map(entry -> {
            var enrollment = enrollmentRepo
                    .findByStudentAndAcademicYearAndSchoolId(entry.getStudentId(), entry.getAcademicYearId(),
                            request.schoolId())
                    .or(() -> enrollmentRepo.findTopByStudentAndSchoolIdOrderByCreatedAtDesc(entry.getStudentId(),
                            request.schoolId()))
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
        });
    }

    private Pageable createPageable(int page, int size) {
        if (size <= 0) {
            return Pageable.unpaged();
        }

        int pageNumber = Math.max(page - 1, 0);
        return PageRequest.of(pageNumber, size, Sort.by(Sort.Direction.DESC, "paidAt"));
    }
}
