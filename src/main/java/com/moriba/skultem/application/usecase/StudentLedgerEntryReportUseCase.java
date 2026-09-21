package com.moriba.skultem.application.usecase;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.ReportBuilderDTO;
import com.moriba.skultem.application.dto.StudentLedgerDTO;
import com.moriba.skultem.application.services.StudentLedgerRowMapper;
import com.moriba.skultem.domain.repository.StudentLedgerEntryRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

/**
 * The filter-builder-backed counterpart to {@link ListStudentLedgerBySchoolUseCase} - same rows, but
 * driven by the generic report engine ({@code entity: "ledger"}) so the Student Ledger page can use
 * the same dynamic filter UI as Transactions instead of a fixed academic-year-only scope.
 * {@link ScopeReportToAcademicYearUseCase} still applies the year scope by default (see its
 * {@code ledger} entry) unless the request brings its own academicYearId filter.
 * <p>
 * School fees only: the platform fee has its own page, so its entries are never in these rows.
 */
@Service
@Transactional
@RequiredArgsConstructor
public class StudentLedgerEntryReportUseCase {

    private final StudentLedgerEntryRepository repo;
    private final StudentLedgerRowMapper rowMapper;

    public Page<StudentLedgerDTO> execute(ReportBuilderDTO request, int page, int size) {
        Pageable pageable = createPageable(page, size);

        List<com.moriba.skultem.domain.vo.Filter> filters = request.filters();

        return repo.runSchoolReport(request.schoolId(), filters, pageable)
                .map(entry -> rowMapper.toDTO(entry, request.schoolId()));
    }

    private Pageable createPageable(int page, int size) {
        if (size <= 0) {
            return Pageable.unpaged();
        }

        int pageNumber = Math.max(page - 1, 0);
        return PageRequest.of(pageNumber, size, Sort.by(Sort.Direction.DESC, "paidAt"));
    }
}
