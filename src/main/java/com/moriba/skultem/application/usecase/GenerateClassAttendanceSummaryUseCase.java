package com.moriba.skultem.application.usecase;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.ClassAttendanceSummaryDTO;
import com.moriba.skultem.application.dto.ClassAttendanceSummaryRowDTO;
import com.moriba.skultem.application.services.AttendanceRulesResolver;
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
    private final AttendanceRulesResolver attendanceRulesResolver;
    private final ResolveTermSchoolDaysUseCase resolveTermSchoolDaysUseCase;
    private final com.moriba.skultem.domain.repository.EnrollmentRepository enrollmentRepo;

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

        // Every student is measured against the school days so far; a day with no mark counts as absent.
        long schoolDays = resolveTermSchoolDaysUseCase.between(schoolId, academicYear.getId(), startDate, endDate)
                .size();

        var rows = withUnmarked(attendanceRepo.attendanceCountsBySchoolAndDateRange(schoolId, academicYear.getId(),
                startDate, endDate), academicYear.getId(), schoolId);

        // A section-limited caller compares only the classes of their own section(s).
        var scope = sectionScopeService.effective();
        java.util.Set<String> visibleClasses = scope.wholeSchool() ? null
                : classRepo.findBySchool(schoolId, scope.levels(), Pageable.unpaged()).getContent().stream()
                        .map(Clazz::getId).collect(java.util.stream.Collectors.toSet());

        // Each class is judged against its own section's minimum (else the school's).
        double schoolThreshold = school.getAttendanceThreshold();
        var rulesByLevel = attendanceRulesResolver.forAllLevels(school);
        Map<String, Double> thresholdByClass = new java.util.HashMap<>();
        for (var c : classRepo.findBySchool(schoolId, Pageable.unpaged()).getContent()) {
            var r = rulesByLevel.get(c.getLevel());
            thresholdByClass.put(c.getId(), r != null ? r.threshold() : schoolThreshold);
        }

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
            long presentOrLate = Math.min(((Number) row[9]).longValue(), schoolDays);
            long late = Math.min(((Number) row[10]).longValue(), presentOrLate);

            String key = classId + "|" + sectionId + "|" + streamId;
            var acc = groups.computeIfAbsent(key,
                    k -> new ClassAccumulator(classId, sectionId, streamId, buildLabel(className, sectionName, streamName)));

            long presentOnly = presentOrLate - late;
            long absent = schoolDays - presentOrLate;

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
            double threshold = thresholdByClass.getOrDefault(acc.classId, schoolThreshold);
            boolean belowThreshold = AttendanceRateCalculator.isBelowThreshold(percentage, threshold);

            classRows.add(new ClassAttendanceSummaryRowDTO(acc.classId, acc.sectionId, acc.streamId, acc.label,
                    acc.totalStudents, acc.totalBoys, acc.totalGirls, acc.present, acc.absent, acc.late,
                    acc.presentBoys, acc.presentGirls, percentage, belowThreshold, threshold));

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
                classesBelowThreshold, schoolBoys, schoolGirls, schoolPresentBoys, schoolPresentGirls, (int) schoolDays);
    }

    // The query's rows plus an all-zero row for every active enrollment it has nothing for, so a student (or a
    // whole class) never marked in the period still shows, missing every school day. Kept in the query's own
    // order: class display order, then section, then stream.
    private java.util.List<Object[]> withUnmarked(java.util.List<Object[]> rows, String academicYearId, String schoolId) {
        var seen = new java.util.HashSet<String>();
        rows.forEach(r -> seen.add((String) r[6]));
        var order = new java.util.HashMap<String, Integer>();
        var all = new ArrayList<Object[]>(rows);
        boolean added = false;
        for (var e : enrollmentRepo.findAllByAcademicSchoolId(academicYearId, schoolId)) {
            if (e.getClazz() != null) order.put(e.getClazz().getId(), e.getClazz().getDisplayOrder());
            if (e.getStatus() != com.moriba.skultem.domain.model.Enrollment.Status.ACTIVE || e.getClazz() == null
                    || e.getSection() == null || e.getStudent() == null || !seen.add(e.getId())) {
                continue;
            }
            added = true;
            all.add(new Object[] { e.getClazz().getId(), e.getClazz().getName(), e.getSection().getId(),
                    e.getSection().getName(), e.getStream() == null ? null : e.getStream().getId(),
                    e.getStream() == null ? null : e.getStream().getName(), e.getId(), e.getStudent().getId(),
                    e.getStudent().getGender(), 0L, 0L, 0L });
        }
        if (!added) return rows;
        all.sort(java.util.Comparator
                .comparingInt((Object[] r) -> order.getOrDefault((String) r[0], Integer.MAX_VALUE))
                .thenComparing(r -> r[3] == null ? "" : (String) r[3])
                .thenComparing(r -> r[5] == null ? "" : (String) r[5]));
        return all;
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
