package com.moriba.skultem.application.usecase;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.SchoolFeeRowDTO;
import com.moriba.skultem.application.dto.StudentFeeBalanceDTO;
import com.moriba.skultem.domain.repository.ClassSessionRepository;
import com.moriba.skultem.domain.service.FeeCollectionCalculator;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

/**
 * Every student's current school-fee position - one row per student (their fees for the scope
 * summed together), not one row per fee. Filterable by class, payment status, fee type and balance
 * range, sortable by balance/name/class. Status filtering and pagination happen in memory after
 * the rows are loaded, the same approach FeeReportUseCase already uses for its own status filter
 * (status isn't a database column, it's derived).
 */
@Service
@Transactional
@RequiredArgsConstructor
public class GetStudentFeeBalancesUseCase {

    public enum SortBy {
        BALANCE, NAME, CLASS
    }

    private final LoadSchoolFeeRowsUseCase loadSchoolFeeRowsUseCase;
    private final ResolveAcademicYearUseCase resolveAcademicYearUseCase;
    private final ClassSessionRepository classSessionRepo;

    public Page<StudentFeeBalanceDTO> execute(String schoolId, String academicYearId, String termId,
            String classSessionId, String status, String feeCategoryId, BigDecimal balanceMin, BigDecimal balanceMax,
            SortBy sortBy, boolean ascending, int page, int size) {

        var academicYear = resolveAcademicYearUseCase.execute(schoolId, academicYearId);
        var rows = loadSchoolFeeRowsUseCase.execute(schoolId, academicYear.getId(), termId);

        String classSessionKey = resolveClassSessionKey(schoolId, classSessionId);
        String categoryFilter = blankToNull(feeCategoryId);

        var filteredRows = rows.stream()
                .filter(r -> classSessionKey == null || classSessionKey.equals(r.classSessionKey()))
                .filter(r -> categoryFilter == null || categoryFilter.equals(r.feeCategoryId()))
                .toList();

        Map<String, List<SchoolFeeRowDTO>> byStudent = filteredRows.stream()
                .collect(Collectors.groupingBy(SchoolFeeRowDTO::studentId));

        List<StudentFeeBalanceDTO> all = new ArrayList<>();
        for (var entry : byStudent.entrySet()) {
            var studentRows = entry.getValue();
            var first = studentRows.get(0);
            BigDecimal expected = sum(studentRows, SchoolFeeRowDTO::expected);
            BigDecimal paid = sum(studentRows, SchoolFeeRowDTO::paid);
            BigDecimal balance = sum(studentRows, SchoolFeeRowDTO::balance);
            var studentStatus = FeeCollectionCalculator.resolveStatus(expected, paid);

            all.add(new StudentFeeBalanceDTO(entry.getKey(), first.admissionNumber(), first.studentName(),
                    first.className(), expected, paid, balance, studentStatus.name()));
        }

        String statusFilter = blankToNull(status);
        var statusFiltered = statusFilter == null
                ? all
                : all.stream().filter(s -> s.status().equalsIgnoreCase(statusFilter)).toList();

        var rangeFiltered = statusFiltered.stream()
                .filter(s -> balanceMin == null || s.balance().compareTo(balanceMin) >= 0)
                .filter(s -> balanceMax == null || s.balance().compareTo(balanceMax) <= 0)
                .toList();

        Comparator<StudentFeeBalanceDTO> comparator = switch (sortBy == null ? SortBy.NAME : sortBy) {
            case BALANCE -> Comparator.comparing(StudentFeeBalanceDTO::balance);
            case CLASS -> Comparator.comparing(s -> s.className() == null ? "" : s.className(),
                    String.CASE_INSENSITIVE_ORDER);
            case NAME -> Comparator.comparing(StudentFeeBalanceDTO::studentName, String.CASE_INSENSITIVE_ORDER);
        };
        if (!ascending) {
            comparator = comparator.reversed();
        }

        var sorted = rangeFiltered.stream().sorted(comparator).toList();

        int total = sorted.size();
        List<StudentFeeBalanceDTO> pageContent;
        Pageable pageable;
        if (size > 0) {
            int from = Math.min(page * size, total);
            int to = Math.min(from + size, total);
            pageContent = sorted.subList(from, to);
            pageable = PageRequest.of(page, size);
        } else {
            pageContent = sorted;
            pageable = Pageable.unpaged();
        }

        return new PageImpl<>(pageContent, pageable, total);
    }

    private String resolveClassSessionKey(String schoolId, String classSessionId) {
        if (classSessionId == null || classSessionId.isBlank()) {
            return null;
        }
        return classSessionRepo.findByIdAndSchoolId(classSessionId, schoolId)
                .map(s -> s.getClazz().getId() + "|" + s.getSection().getId()
                        + "|" + (s.getStream() != null ? s.getStream().getId() : ""))
                .orElse("__none__"); // matches nothing, rather than silently ignoring the filter
    }

    private static <T> BigDecimal sum(List<T> list, java.util.function.Function<T, BigDecimal> extractor) {
        return list.stream().map(extractor).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
