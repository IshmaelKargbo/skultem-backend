package com.moriba.skultem.application.usecase;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.OutstandingFeesReportDTO;
import com.moriba.skultem.application.dto.OutstandingFeesReportDTO.ClassOutstandingRowDTO;
import com.moriba.skultem.application.dto.OutstandingFeesReportDTO.FeeTypeOutstandingRowDTO;
import com.moriba.skultem.application.dto.SchoolFeeRowDTO;
import com.moriba.skultem.application.dto.StudentFeeBalanceDTO;
import com.moriba.skultem.domain.repository.ClassSessionRepository;
import com.moriba.skultem.domain.service.FeeCollectionCalculator;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

/** A focused view of money still owed: totals, where it's concentrated (by class, by fee type), and who owes the most. */
@Service
@Transactional
@RequiredArgsConstructor
public class GetOutstandingFeesReportUseCase {

    private static final int TOP_STUDENTS_LIMIT = 20;

    private final LoadSchoolFeeRowsUseCase loadSchoolFeeRowsUseCase;
    private final ResolveAcademicYearUseCase resolveAcademicYearUseCase;
    private final ClassSessionRepository classSessionRepo;

    public OutstandingFeesReportDTO execute(String schoolId, String academicYearId, String termId) {
        var academicYear = resolveAcademicYearUseCase.execute(schoolId, academicYearId);
        var rows = loadSchoolFeeRowsUseCase.execute(schoolId, academicYear.getId(), termId);

        Map<String, List<SchoolFeeRowDTO>> byStudent = rows.stream()
                .collect(Collectors.groupingBy(SchoolFeeRowDTO::studentId));

        List<StudentFeeBalanceDTO> studentBalances = new ArrayList<>();
        for (var entry : byStudent.entrySet()) {
            var studentRows = entry.getValue();
            var first = studentRows.get(0);
            BigDecimal expected = sum(studentRows, SchoolFeeRowDTO::expected);
            BigDecimal paid = sum(studentRows, SchoolFeeRowDTO::paid);
            BigDecimal balance = sum(studentRows, SchoolFeeRowDTO::balance);
            if (balance.compareTo(BigDecimal.ZERO) <= 0) {
                continue; // only students who actually owe something
            }
            var status = FeeCollectionCalculator.resolveStatus(expected, paid);
            studentBalances.add(new StudentFeeBalanceDTO(entry.getKey(), first.admissionNumber(), first.studentName(),
                    first.className(), expected, paid, balance, status.name()));
        }

        BigDecimal totalOutstanding = sum(studentBalances, StudentFeeBalanceDTO::balance);

        var topStudents = studentBalances.stream()
                .sorted(Comparator.comparing(StudentFeeBalanceDTO::balance).reversed())
                .limit(TOP_STUDENTS_LIMIT)
                .toList();

        var byClass = buildClassOutstanding(schoolId, academicYear.getId(), rows);
        var byFeeType = buildFeeTypeOutstanding(rows);

        return new OutstandingFeesReportDTO(totalOutstanding, studentBalances.size(), byClass, byFeeType,
                topStudents);
    }

    // Grouped by class SESSION (class + section + stream) - see GenerateFeeTermSummaryUseCase for
    // why. Only classes with an actual balance are returned.
    private List<ClassOutstandingRowDTO> buildClassOutstanding(String schoolId, String academicYearId,
            List<SchoolFeeRowDTO> rows) {
        Map<String, List<SchoolFeeRowDTO>> byClass = rows.stream()
                .collect(Collectors.groupingBy(SchoolFeeRowDTO::classSessionKey));

        List<ClassOutstandingRowDTO> result = new ArrayList<>();
        for (var entry : byClass.entrySet()) {
            var classRows = entry.getValue();
            BigDecimal outstanding = sum(classRows, SchoolFeeRowDTO::balance);
            if (outstanding.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }

            long studentsWithOutstanding = classRows.stream()
                    .collect(Collectors.groupingBy(SchoolFeeRowDTO::studentId))
                    .values().stream()
                    .filter(studentRows -> sum(studentRows, SchoolFeeRowDTO::balance).compareTo(BigDecimal.ZERO) > 0)
                    .count();

            String[] parts = entry.getKey().split("\\|", -1);
            String clazzId = parts[0];
            String sectionId = parts.length > 1 ? parts[1] : "";
            String streamId = parts.length > 2 ? parts[2] : "";

            String classSessionId = (streamId.isEmpty()
                    ? classSessionRepo.findByClassIdAndAcademicYearIdAndSectionIdAndSchoolId(clazzId, academicYearId,
                            sectionId, schoolId)
                    : classSessionRepo.findByClassIdAndAcademicYearIdAndSectionIdAndStreamIdAndSchoolId(clazzId,
                            academicYearId, sectionId, streamId, schoolId))
                    .map(s -> s.getId())
                    .orElse(null);

            result.add(new ClassOutstandingRowDTO(classSessionId, classRows.get(0).className(), outstanding,
                    (int) studentsWithOutstanding));
        }

        result.sort(Comparator.comparing(ClassOutstandingRowDTO::outstanding).reversed());
        return result;
    }

    private List<FeeTypeOutstandingRowDTO> buildFeeTypeOutstanding(List<SchoolFeeRowDTO> rows) {
        Map<String, List<SchoolFeeRowDTO>> byType = rows.stream()
                .collect(Collectors.groupingBy(SchoolFeeRowDTO::feeCategoryId));

        List<FeeTypeOutstandingRowDTO> result = new ArrayList<>();
        for (var entry : byType.entrySet()) {
            var typeRows = entry.getValue();
            BigDecimal outstanding = sum(typeRows, SchoolFeeRowDTO::balance);
            if (outstanding.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            long studentsWithOutstanding = typeRows.stream()
                    .filter(r -> r.balance().compareTo(BigDecimal.ZERO) > 0)
                    .map(SchoolFeeRowDTO::studentId)
                    .distinct()
                    .count();
            result.add(new FeeTypeOutstandingRowDTO(entry.getKey(), typeRows.get(0).feeCategoryName(), outstanding,
                    (int) studentsWithOutstanding));
        }

        result.sort(Comparator.comparing(FeeTypeOutstandingRowDTO::outstanding).reversed());
        return result;
    }

    private static <T> BigDecimal sum(List<T> list, Function<T, BigDecimal> extractor) {
        return list.stream().map(extractor).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
