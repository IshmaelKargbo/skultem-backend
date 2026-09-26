package com.moriba.skultem.application.usecase;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.ClassAttendanceSummaryDTO;
import com.moriba.skultem.application.dto.ClassAttendanceSummaryRowDTO;
import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.services.SectionScopeService;
import com.moriba.skultem.domain.model.Clazz;
import com.moriba.skultem.domain.repository.AttendanceRepository;
import com.moriba.skultem.domain.repository.ClassRepository;
import com.moriba.skultem.domain.repository.SchoolRepository;
import com.moriba.skultem.domain.repository.TermRepository;
import com.moriba.skultem.domain.service.AttendanceRateCalculator;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

// One row per class session (class + section + stream) across the WHOLE school for one date
// range - a school-wide companion to Term Summary's single-class view, so an admin can compare
// classes side by side instead of picking one at a time. termId null/blank means "All Terms": the
// whole academic year's date range, not just one term's. A single query
// (attendanceCountsBySchoolAndDateRange) returns every student's counts tagged with their class
// session, and this use case groups them in memory rather than issuing one query per class -
// avoids N+1 for a school with many classes.
@Service
@Transactional
@RequiredArgsConstructor
public class GenerateClassAttendanceSummaryUseCase {

    private final SchoolRepository schoolRepo;
    private final TermRepository termRepo;
    private final AttendanceRepository attendanceRepo;
    private final ResolveAcademicYearUseCase resolveAcademicYearUseCase;
    private final ClassRepository classRepo;
    private final SectionScopeService sectionScopeService;

    public ClassAttendanceSummaryDTO execute(String schoolId, String academicYearId, String termId) {
        var school = schoolRepo.findById(schoolId).orElseThrow(() -> new NotFoundException("School not found"));
        var academicYear = resolveAcademicYearUseCase.execute(schoolId, academicYearId);

        String termLabel = null;
        var startDate = academicYear.getStartDate();
        var endDate = academicYear.getEndDate();

        if (termId != null && !termId.isBlank()) {
            var term = termRepo.findByIdAndSchoolId(termId, schoolId)
                    .orElseThrow(() -> new NotFoundException("Term not found"));
            termLabel = term.getName();
            startDate = term.getStartDate();
            endDate = term.getEndDate();
        }

        var rows = attendanceRepo.attendanceCountsBySchoolAndDateRange(schoolId, academicYear.getId(), startDate,
                endDate);

        // A section-limited caller compares only the classes of their own section(s).
        var scope = sectionScopeService.effective();
        java.util.Set<String> visibleClasses = scope.wholeSchool() ? null
                : classRepo.findBySchool(schoolId, scope.levels(), Pageable.unpaged()).getContent().stream()
                        .map(Clazz::getId).collect(java.util.stream.Collectors.toSet());

        double threshold = school.getAttendanceThreshold();

        // Group per-student rows into one accumulator per class session, preserving the query's
        // own ORDER BY (class display order, section, stream) via LinkedHashMap insertion order.
        Map<String, ClassAccumulator> groups = new LinkedHashMap<>();
        for (Object[] row : rows) {
            String classId = (String) row[0];
            if (visibleClasses != null && !visibleClasses.contains(classId)) {
                continue;
            }
            String className = (String) row[1];
            String sectionId = (String) row[2];
            String sectionName = (String) row[3];
            String streamId = (String) row[4];
            String streamName = (String) row[5];
            String gender = row[8] != null ? row[8].toString() : null;
            long presentOrLate = ((Number) row[9]).longValue();
            long late = ((Number) row[10]).longValue();
            long totalRecorded = ((Number) row[11]).longValue();

            String key = classId + "|" + sectionId + "|" + streamId;
            var acc = groups.computeIfAbsent(key,
                    k -> new ClassAccumulator(classId, sectionId, streamId, buildLabel(className, sectionName, streamName)));

            long presentOnly = presentOrLate - late;
            long absent = totalRecorded - presentOrLate;

            acc.totalStudents++;
            acc.present += presentOnly;
            acc.absent += absent;
            acc.late += late;
            if ("MALE".equals(gender)) {
                acc.totalBoys++;
                acc.presentBoys += presentOrLate;
            } else if ("FEMALE".equals(gender)) {
                acc.totalGirls++;
                acc.presentGirls += presentOrLate;
            }
        }

        var classRows = new ArrayList<ClassAttendanceSummaryRowDTO>();
        long schoolPresent = 0;
        long schoolTotal = 0;
        long schoolBoys = 0;
        long schoolGirls = 0;
        long schoolPresentBoys = 0;
        long schoolPresentGirls = 0;

        for (var acc : groups.values()) {
            long totalRecorded = acc.present + acc.absent + acc.late;
            Double percentage = totalRecorded > 0
                    ? AttendanceRateCalculator.rate(acc.present + acc.late, totalRecorded)
                    : null;
            boolean belowThreshold = AttendanceRateCalculator.isBelowThreshold(percentage, threshold);

            classRows.add(new ClassAttendanceSummaryRowDTO(acc.classId, acc.sectionId, acc.streamId, acc.label,
                    acc.totalStudents, acc.totalBoys, acc.totalGirls, acc.present, acc.absent, acc.late,
                    acc.presentBoys, acc.presentGirls, percentage, belowThreshold));

            schoolPresent += acc.present + acc.late;
            schoolTotal += totalRecorded;
            schoolBoys += acc.totalBoys;
            schoolGirls += acc.totalGirls;
            schoolPresentBoys += acc.presentBoys;
            schoolPresentGirls += acc.presentGirls;
        }

        double schoolAverage = AttendanceRateCalculator.rate(schoolPresent, schoolTotal);
        int classesBelowThreshold = (int) classRows.stream().filter(ClassAttendanceSummaryRowDTO::belowThreshold)
                .count();
        long totalStudents = classRows.stream().mapToLong(ClassAttendanceSummaryRowDTO::totalStudents).sum();

        return new ClassAttendanceSummaryDTO(classRows, termLabel, classRows.size(), totalStudents, schoolAverage,
                classesBelowThreshold, schoolBoys, schoolGirls, schoolPresentBoys, schoolPresentGirls);
    }

    private String buildLabel(String className, String sectionName, String streamName) {
        var builder = new StringBuilder(className);
        if (sectionName != null && !sectionName.isBlank()) {
            builder.append(" ").append(sectionName);
        }
        if (streamName != null && !streamName.isBlank()) {
            builder.append(" - ").append(streamName);
        }
        return builder.toString();
    }

    private static final class ClassAccumulator {
        final String classId;
        final String sectionId;
        final String streamId;
        final String label;
        long totalStudents;
        long totalBoys;
        long totalGirls;
        long present;
        long absent;
        long late;
        long presentBoys;
        long presentGirls;

        ClassAccumulator(String classId, String sectionId, String streamId, String label) {
            this.classId = classId;
            this.sectionId = sectionId;
            this.streamId = streamId;
            this.label = label;
        }
    }
}
