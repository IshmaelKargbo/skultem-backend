package com.moriba.skultem.application.usecase;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.ClassFeeSummaryRowDTO;
import com.moriba.skultem.application.dto.FeeTermSummaryDTO;
import com.moriba.skultem.application.dto.SchoolFeeRowDTO;
import com.moriba.skultem.domain.repository.ClassSessionRepository;
import com.moriba.skultem.domain.service.FeeCollectionCalculator;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

/**
 * The management-level term report: school-wide totals plus a class-level breakdown so the school
 * can see immediately where outstanding money is concentrated. Optionally narrowed to one class
 * session and/or fee category - narrowing to a class still returns a "classes" list, just with one
 * row in it, so the frontend doesn't need two different shapes.
 */
@Service
@Transactional
@RequiredArgsConstructor
public class GenerateFeeTermSummaryUseCase {

    private final LoadSchoolFeeRowsUseCase loadSchoolFeeRowsUseCase;
    private final ResolveAcademicYearUseCase resolveAcademicYearUseCase;
    private final ClassSessionRepository classSessionRepo;

    public FeeTermSummaryDTO execute(String schoolId, String academicYearId, String termId, String classSessionId,
            String feeCategoryId) {
        var academicYear = resolveAcademicYearUseCase.execute(schoolId, academicYearId);
        var rows = loadSchoolFeeRowsUseCase.execute(schoolId, academicYear.getId(), termId);

        String classSessionKey = null;
        if (classSessionId != null && !classSessionId.isBlank()) {
            var session = classSessionRepo.findByIdAndSchoolId(classSessionId, schoolId).orElse(null);
            if (session != null) {
                String streamId = session.getStream() != null ? session.getStream().getId() : "";
                classSessionKey = session.getClazz().getId() + "|" + session.getSection().getId() + "|" + streamId;
            } else {
                classSessionKey = "__none__"; // an id that matches nothing, rather than ignoring the filter
            }
        }
        final String keyFilter = classSessionKey;
        final String categoryFilter = (feeCategoryId != null && !feeCategoryId.isBlank()) ? feeCategoryId : null;

        var filtered = rows.stream()
                .filter(r -> keyFilter == null || keyFilter.equals(r.classSessionKey()))
                .filter(r -> categoryFilter == null || categoryFilter.equals(r.feeCategoryId()))
                .toList();

        var classes = buildClassRows(schoolId, academicYear.getId(), filtered);

        BigDecimal totalExpected = filtered.stream().map(SchoolFeeRowDTO::expected).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalCollected = filtered.stream().map(SchoolFeeRowDTO::paid).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalOutstanding = filtered.stream().map(SchoolFeeRowDTO::balance).reduce(BigDecimal.ZERO, BigDecimal::add);
        double collectionRate = FeeCollectionCalculator.rate(totalCollected, totalExpected);

        Map<String, List<SchoolFeeRowDTO>> byStudent = filtered.stream()
                .collect(Collectors.groupingBy(SchoolFeeRowDTO::studentId));
        int fullyPaid = 0, withBalance = 0;
        for (var studentRows : byStudent.values()) {
            BigDecimal balance = studentRows.stream().map(SchoolFeeRowDTO::balance).reduce(BigDecimal.ZERO, BigDecimal::add);
            if (balance.compareTo(BigDecimal.ZERO) <= 0) {
                fullyPaid++;
            } else {
                withBalance++;
            }
        }

        return new FeeTermSummaryDTO(totalExpected, totalCollected, totalOutstanding, collectionRate,
                byStudent.size(), fullyPaid, withBalance, classes);
    }

    // Groups by class SESSION (class + section + stream) - "SSS 1 Science" and "SSS 1 Art" are
    // separate rows even when they share the same class/section, matching the boundary the
    // attendance term/class summaries and the Classes list already respect.
    private List<ClassFeeSummaryRowDTO> buildClassRows(String schoolId, String academicYearId,
            List<SchoolFeeRowDTO> rows) {
        Map<String, List<SchoolFeeRowDTO>> byClass = rows.stream()
                .collect(Collectors.groupingBy(SchoolFeeRowDTO::classSessionKey));

        List<ClassFeeSummaryRowDTO> result = new ArrayList<>();
        for (var entry : byClass.entrySet()) {
            var classRows = entry.getValue();
            String[] parts = entry.getKey().split("\\|", -1);
            String clazzId = parts[0];
            String sectionId = parts.length > 1 ? parts[1] : "";
            String streamId = parts.length > 2 ? parts[2] : "";

            // Best-effort only - a session that can't be resolved (e.g. section/stream since
            // reassigned) still gets a row, just without a linkable id; the display name/totals
            // (from the rows themselves) never depend on this lookup succeeding.
            String classSessionId = (streamId.isEmpty()
                    ? classSessionRepo.findByClassIdAndAcademicYearIdAndSectionIdAndSchoolId(clazzId, academicYearId,
                            sectionId, schoolId)
                    : classSessionRepo.findByClassIdAndAcademicYearIdAndSectionIdAndStreamIdAndSchoolId(clazzId,
                            academicYearId, sectionId, streamId, schoolId))
                    .map(s -> s.getId())
                    .orElse(null);

            BigDecimal expected = classRows.stream().map(SchoolFeeRowDTO::expected).reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal collected = classRows.stream().map(SchoolFeeRowDTO::paid).reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal outstanding = classRows.stream().map(SchoolFeeRowDTO::balance).reduce(BigDecimal.ZERO, BigDecimal::add);
            double rate = FeeCollectionCalculator.rate(collected, expected);

            Map<String, List<SchoolFeeRowDTO>> byStudent = classRows.stream()
                    .collect(Collectors.groupingBy(SchoolFeeRowDTO::studentId));
            int fullyPaid = 0, withBalance = 0;
            for (var studentRows : byStudent.values()) {
                BigDecimal balance = studentRows.stream().map(SchoolFeeRowDTO::balance).reduce(BigDecimal.ZERO, BigDecimal::add);
                if (balance.compareTo(BigDecimal.ZERO) <= 0) {
                    fullyPaid++;
                } else {
                    withBalance++;
                }
            }

            result.add(new ClassFeeSummaryRowDTO(classSessionId, classRows.get(0).className(), null, null, expected,
                    collected, outstanding, rate, byStudent.size(), fullyPaid, withBalance));
        }

        result.sort((a, b) -> a.className().compareToIgnoreCase(b.className()));
        return result;
    }
}
