package com.moriba.skultem.application.usecase;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.AcademicOverviewDTO;
import com.moriba.skultem.application.dto.AcademicReportDTO;
import com.moriba.skultem.application.dto.ClassPerformanceDTO;
import com.moriba.skultem.application.dto.GradeCountDTO;
import com.moriba.skultem.application.dto.StudentAcademicPerformanceDTO;
import com.moriba.skultem.application.dto.StudentSubjectScoreDTO;
import com.moriba.skultem.application.dto.SubjectPerformanceDTO;
import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.domain.model.ClassSubjectAssessmentLifeCycle;
import com.moriba.skultem.domain.model.Enrollment;
import com.moriba.skultem.domain.model.School;
import com.moriba.skultem.domain.model.Term;
import com.moriba.skultem.domain.repository.AssessmentScoreRepository;
import com.moriba.skultem.domain.repository.ClassRepository;
import com.moriba.skultem.domain.repository.ClassSubjectAssessmentLifeCycleRepository;
import com.moriba.skultem.domain.repository.EnrollmentRepository;
import com.moriba.skultem.domain.repository.SchoolRepository;
import com.moriba.skultem.domain.repository.TermRepository;
import com.moriba.skultem.domain.service.AcademicPerformanceCalculator;
import com.moriba.skultem.domain.vo.Level;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

// Backs the Academic Report's Overview, Class Performance, Subject Performance and Student
// Performance sections in a single call. classId == null means "whole school" - the same
// aggregation runs either way, reading from one set of bulk queries grouped three different ways
// in Java (by student, by subject, by class), so none of the sections can disagree with each
// other. Only counts APPROVED/COMPLETED cycles - management sees released, reviewed academic
// data, per the feature's premise ("after assessments are approved, make the data available to
// school management"). This is deliberately stricter than ComputeClassAttentionUseCase's
// early-warning heuristic, which includes SUBMITTED/RETURNED on purpose so a class can be watched
// before formal review completes.
//
// Deliberately excludes LOCKED, even though AssessmentScore.isApproved() treats it as released
// data (COMPLETED/LOCKED can only be reached from APPROVED per the state machine, in theory). In
// practice LOCKED is also used as a bulk-provisioned placeholder for a term's not-yet-reached
// assessment positions (see the comment on AssessmentScoreJpaRepository#averageScoresByClassAndTerm
// for the same landmine) - those rows sit at score=0 and were never actually graded. Confirmed
// against live data: a freshly opened term had 0 real grades but its LOCKED placeholder cycles
// were still being counted as "completed", corrupting every average/pass-rate/completion number in
// this report. COMPLETED alone (the status set when a term is properly closed out after grading)
// doesn't have this ambiguity.
//
// Pass/fail is always evaluated against the STUDENT'S OWN CLASS pass mark (AssessmentTemplate.
// passMark, or 50 if the class has no template) - never one single global pass mark - since a
// whole-school report can span classes with different templates and there's no single "correct"
// pass mark to apply across all of them.
@Service
@Transactional
@RequiredArgsConstructor
public class GetClassAcademicPerformanceUseCase {

    public static final List<ClassSubjectAssessmentLifeCycle.Status> APPROVED_STATUSES = List.of(
            ClassSubjectAssessmentLifeCycle.Status.APPROVED,
            ClassSubjectAssessmentLifeCycle.Status.COMPLETED);

    private static final int DEFAULT_PASS_MARK = 50;

    private final ClassRepository classRepo;
    private final TermRepository termRepo;
    private final EnrollmentRepository enrollmentRepo;
    private final AssessmentScoreRepository scoreRepo;
    private final ClassSubjectAssessmentLifeCycleRepository cycleRepo;
    private final SchoolRepository schoolRepo;
    private final ResolveAcademicYearUseCase resolveAcademicYearUseCase;

    public AcademicReportDTO execute(String schoolId, String classId, String academicYearId, String termId,
            String subjectId, Level level, int page, int size) {
        var academicYear = resolveAcademicYearUseCase.execute(schoolId, academicYearId);
        var term = resolveTerm(schoolId, academicYear.getId(), termId);
        School school = schoolRepo.findById(schoolId).orElse(null);

        if (classId != null && !classId.isBlank()) {
            classRepo.findByIdAndSchool(classId, schoolId)
                    .orElseThrow(() -> new NotFoundException("Class not found"));
        }

        // classId -> passMark / level, built once from the whole school's (small) class list -
        // lets pass/fail be evaluated per-student against THEIR class's own configured pass mark,
        // even in whole-school mode where different classes may use different templates.
        Map<String, Integer> passMarkByClass = new HashMap<>();
        Map<String, Level> levelByClass = new HashMap<>();
        for (var clazz : classRepo.findBySchool(schoolId, Pageable.unpaged()).getContent()) {
            passMarkByClass.put(clazz.getId(),
                    clazz.getTemplate() != null ? clazz.getTemplate().getPassMark() : DEFAULT_PASS_MARK);
            levelByClass.put(clazz.getId(), clazz.getLevel());
        }

        List<Enrollment> roster = classId != null && !classId.isBlank()
                ? enrollmentRepo.findAllByClassAndAcademicAndSchoolId(classId, academicYear.getId(), schoolId,
                        Pageable.unpaged()).getContent()
                : enrollmentRepo.findAllByAcademicSchoolId(academicYear.getId(), schoolId);

        if (level != null) {
            roster = roster.stream().filter(e -> level.equals(levelByClass.get(e.getClazz().getId()))).toList();
        }

        Map<String, Integer> rosterTotalByClass = new HashMap<>();
        Map<String, String> classNameById = new HashMap<>();
        for (var enrollment : roster) {
            var clazz = enrollment.getClazz();
            rosterTotalByClass.merge(clazz.getId(), 1, Integer::sum);
            classNameById.put(clazz.getId(), clazz.getName());
        }
        int totalStudents = roster.size();

        var scoreRows = scoreRepo.studentSubjectAveragesForReport(schoolId, classId, term.getId(), subjectId,
                APPROVED_STATUSES);

        Map<String, StudentAccumulator> byStudent = new LinkedHashMap<>();
        Map<String, SubjectAccumulator> bySubject = new LinkedHashMap<>();

        for (Object[] row : scoreRows) {
            String enrollmentId = (String) row[0];
            String studentId = (String) row[1];
            String givenNames = (String) row[2];
            String familyName = (String) row[3];
            String rowClassId = (String) row[4];
            String rowClassName = (String) row[5];
            String subId = (String) row[6];
            String subName = (String) row[7];
            double avg = ((Number) row[8]).doubleValue();

            if (level != null && !level.equals(levelByClass.get(rowClassId))) {
                continue;
            }

            var student = byStudent.computeIfAbsent(enrollmentId,
                    k -> new StudentAccumulator(studentId, givenNames, familyName, rowClassId, rowClassName));
            student.subjects
                    .add(new StudentSubjectScoreDTO(subId, subName, AcademicPerformanceCalculator.round1Dp(avg)));

            var subject = bySubject.computeIfAbsent(subId, k -> new SubjectAccumulator(subName));
            subject.entries.add(new ScoredEntry(avg, rowClassId));
        }

        Map<String, long[]> completionByEnrollment = new HashMap<>();
        for (Object[] row : scoreRepo.assessmentCompletionByClassAndTerm(schoolId, classId, term.getId(),
                APPROVED_STATUSES)) {
            completionByEnrollment.put((String) row[0], new long[] {
                    ((Number) row[1]).longValue(),
                    ((Number) row[2]).longValue()
            });
        }

        List<StudentAcademicPerformanceDTO> allStudents = new ArrayList<>();
        Map<String, ClassAccumulator> byClass = new LinkedHashMap<>();

        for (var entry : byStudent.entrySet()) {
            String enrollmentId = entry.getKey();
            var acc = entry.getValue();
            double overallAverage = acc.subjects.stream()
                    .mapToDouble(StudentSubjectScoreDTO::averageScore)
                    .average()
                    .orElse(0);

            long[] completion = completionByEnrollment.getOrDefault(enrollmentId, new long[] { 0, 0 });
            long missing = Math.max(0, completion[0] - completion[1]);

            allStudents.add(new StudentAcademicPerformanceDTO(acc.studentId, enrollmentId, acc.givenNames,
                    acc.familyName, acc.subjects, AcademicPerformanceCalculator.round1Dp(overallAverage),
                    completion[1], missing));

            byClass.computeIfAbsent(acc.classId, k -> new ClassAccumulator(acc.className))
                    .studentAverages.add(overallAverage);
        }

        // Sort deterministically before paginating so the same page is returned consistently
        // across requests rather than depending on map iteration order.
        allStudents.sort((a, b) -> a.familyName().compareToIgnoreCase(b.familyName()));

        int studentsTotal = allStudents.size();
        int safeSize = size > 0 ? size : studentsTotal;
        int safePage = Math.max(page, 1);
        int fromIndex = Math.min((safePage - 1) * safeSize, studentsTotal);
        int toIndex = Math.min(fromIndex + safeSize, studentsTotal);
        List<StudentAcademicPerformanceDTO> pagedStudents = allStudents.subList(fromIndex, toIndex);

        List<SubjectPerformanceDTO> subjects = new ArrayList<>();
        for (var subjectEntry : bySubject.entrySet()) {
            subjects.add(buildSubjectPerformance(subjectEntry.getKey(), subjectEntry.getValue(), passMarkByClass,
                    school));
        }

        List<ClassPerformanceDTO> classPerformance = new ArrayList<>();
        if (classId == null || classId.isBlank()) {
            for (var classEntry : byClass.entrySet()) {
                String cId = classEntry.getKey();
                var acc = classEntry.getValue();
                int passMark = passMarkByClass.getOrDefault(cId, DEFAULT_PASS_MARK);
                int assessed = acc.studentAverages.size();
                int enrolled = rosterTotalByClass.getOrDefault(cId, assessed);
                double avg = acc.studentAverages.stream().mapToDouble(Double::doubleValue).average().orElse(0);
                long passCount = acc.studentAverages.stream()
                        .filter(a -> AcademicPerformanceCalculator.isPassed(a, passMark))
                        .count();

                classPerformance.add(new ClassPerformanceDTO(cId, acc.name, enrolled, assessed,
                        Math.max(0, enrolled - assessed), AcademicPerformanceCalculator.round1Dp(avg),
                        AcademicPerformanceCalculator.passRate(passCount, assessed)));
            }
            classPerformance.sort((a, b) -> a.className().compareToIgnoreCase(b.className()));
        }

        var completionRows = cycleRepo.completionReportRows(schoolId, term.getId(), classId, subjectId);
        List<Object[]> filteredCompletionRows = level == null ? completionRows
                : completionRows.stream()
                        .filter(row -> level.equals(levelByClass.get((String) row[0])))
                        .toList();
        int totalAssessments = filteredCompletionRows.size();
        int completedAssessments = (int) filteredCompletionRows.stream()
                .filter(row -> APPROVED_STATUSES.contains(row[7]))
                .count();

        int studentsAssessed = allStudents.size();

        double classAverage = allStudents.isEmpty() ? 0
                : allStudents.stream().mapToDouble(StudentAcademicPerformanceDTO::overallAverage).average()
                        .orElse(0);

        long passingStudents = 0;
        for (var entry : byStudent.entrySet()) {
            var acc = entry.getValue();
            double overallAverage = acc.subjects.stream()
                    .mapToDouble(StudentSubjectScoreDTO::averageScore)
                    .average()
                    .orElse(0);
            int passMark = passMarkByClass.getOrDefault(acc.classId, DEFAULT_PASS_MARK);
            if (AcademicPerformanceCalculator.isPassed(overallAverage, passMark)) {
                passingStudents++;
            }
        }
        int studentsNeedingSupport = studentsAssessed - (int) passingStudents;

        var overview = new AcademicOverviewDTO(totalStudents, studentsAssessed, totalAssessments,
                completedAssessments, AcademicPerformanceCalculator.round1Dp(classAverage),
                AcademicPerformanceCalculator.passRate(passingStudents, studentsAssessed), studentsNeedingSupport);

        return new AcademicReportDTO(overview, classPerformance, subjects, pagedStudents, safePage, safeSize,
                studentsTotal);
    }

    private SubjectPerformanceDTO buildSubjectPerformance(String subjectId, SubjectAccumulator acc,
            Map<String, Integer> passMarkByClass, School school) {
        List<Double> averages = acc.entries.stream().map(ScoredEntry::average).toList();
        double average = averages.stream().mapToDouble(Double::doubleValue).average().orElse(0);
        double median = median(averages);
        double highest = averages.stream().mapToDouble(Double::doubleValue).max().orElse(0);
        double lowest = averages.stream().mapToDouble(Double::doubleValue).min().orElse(0);

        long passCount = acc.entries.stream()
                .filter(e -> AcademicPerformanceCalculator.isPassed(e.average(),
                        passMarkByClass.getOrDefault(e.classId(), DEFAULT_PASS_MARK)))
                .count();
        long failCount = acc.entries.size() - passCount;

        Map<String, Long> gradeCounts = new LinkedHashMap<>();
        if (school != null && school.getGradingScale() != null) {
            for (var e : acc.entries) {
                String grade = school.resolveGrade((int) Math.round(e.average()));
                if (grade != null) {
                    gradeCounts.merge(grade, 1L, Long::sum);
                }
            }
        }
        List<GradeCountDTO> gradeDistribution = gradeCounts.entrySet().stream()
                .map(e -> new GradeCountDTO(e.getKey(), e.getValue()))
                .toList();

        return new SubjectPerformanceDTO(subjectId, acc.name,
                AcademicPerformanceCalculator.round1Dp(average),
                AcademicPerformanceCalculator.round1Dp(median),
                AcademicPerformanceCalculator.round1Dp(highest),
                AcademicPerformanceCalculator.round1Dp(lowest),
                AcademicPerformanceCalculator.passRate(passCount, acc.entries.size()),
                acc.entries.size(), passCount, failCount, gradeDistribution);
    }

    private double median(List<Double> values) {
        if (values.isEmpty()) {
            return 0;
        }
        List<Double> sorted = new ArrayList<>(values);
        Collections.sort(sorted);
        int n = sorted.size();
        if (n % 2 == 1) {
            return sorted.get(n / 2);
        }
        return (sorted.get(n / 2 - 1) + sorted.get(n / 2)) / 2.0;
    }

    private Term resolveTerm(String schoolId, String academicYearId, String termId) {
        if (termId != null && !termId.isBlank()) {
            return termRepo.findByIdAndAcademicYearIdAndSchoolId(termId, academicYearId, schoolId)
                    .orElseThrow(() -> new NotFoundException("Term not found"));
        }

        return termRepo.findActiveBySchoolAndAcademicYear(schoolId, academicYearId)
                .orElseThrow(() -> new NotFoundException("Active term not found"));
    }

    private static class StudentAccumulator {
        private final String studentId;
        private final String givenNames;
        private final String familyName;
        private final String classId;
        private final String className;
        private final List<StudentSubjectScoreDTO> subjects = new ArrayList<>();

        StudentAccumulator(String studentId, String givenNames, String familyName, String classId,
                String className) {
            this.studentId = studentId;
            this.givenNames = givenNames;
            this.familyName = familyName;
            this.classId = classId;
            this.className = className;
        }
    }

    private static class SubjectAccumulator {
        private final String name;
        private final List<ScoredEntry> entries = new ArrayList<>();

        SubjectAccumulator(String name) {
            this.name = name;
        }
    }

    private static class ClassAccumulator {
        private final String name;
        private final List<Double> studentAverages = new ArrayList<>();

        ClassAccumulator(String name) {
            this.name = name;
        }
    }

    private record ScoredEntry(double average, String classId) {
    }
}
